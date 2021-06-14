/*
 * Copyright (c) 1997, 2013, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package java.lang.ref;

import sun.misc.Cleaner;
import sun.misc.JavaLangRefAccess;
import sun.misc.SharedSecrets;

/**
 * 20210613
 * 引用对象的抽象基类。 此类定义了所有引用对象的通用操作。 由于引用对象是与垃圾收集器密切配合实现的，因此该类可能无法直接子类化。
 */

/**
 * Abstract base class for reference objects.  This class defines the
 * operations common to all reference objects.  Because reference objects are
 * implemented in close cooperation with the garbage collector, this class may
 * not be subclassed directly.
 *
 * @author   Mark Reinhold
 * @since    1.2
 */

public abstract class Reference<T> {

    /**
     * 20210613
     * A. 引用实例处于四种可能的内部状态之一：
     *    1) Active活动状态：受到垃圾收集器的特殊处理。 在收集器检测到所指对象的可达性已更改为适当状态后的一段时间，它会将实例的状态更改为Pending或Inactive，
     *       具体取决于实例在创建时是否注册了引用队列实例。 在前一种情况下，它还会将ref实例添加到待定引用列表中。 新创建的ref实例处于活动状态。
     *    2) Pending挂起状态：Pending引用列表的一个元素，等待引用处理程序线程入队。未注册引用队列的ref实例永远不会处于这种状态。
     *    3) Enqueued入队状态：在检测到适当的可达性更改后，垃圾收集器会将注册的引用对象附加到该队列中。 当一个ref实例从它的ReferenceQueue中被移除时，
     *       它就会变成Inactive。未注册引用队列的ref实例永远不会处于这种状态。
     *    4) Inactive不活动状态：无事可做。 一旦ref实例变为不活动状态，其状态将永远不会再改变。
     * B. 队列状态解释如下(this指的是引用实例)：
     *    1) Active活动状态时：ref.queue可能为ReferenceQueue实例或者为ReferenceQueue.NULL(说明没有指定ref实例引用队列实例), 此时ref.next为null。
     *    2) Pending挂起状态时: ref.queue可能为ReferenceQueue实例, 此时ref.next为this。
     *    3) Enqueued入队状态时: ref.queue为ReferenceQueue.ENQUEUED, 此时ref.next为队列中的后续元素或者为this(作为队尾元素时)。
     *    4) Inactive不活动状态时: ref.queue为ReferenceQueue.NULL(说明没有指定ref实例引用队列实例), 此时ref.next为this.
     * C. 使用这种方案，收集器只需要检查引用队列中的元素, 以确定引用实例是否还需要特殊处理：
     *    1) 如果引用队列不存在元素，则该实例处于活动状态；
     *    2) 如果引用队列存在元素，则收集器应该正常处理该ref实例。
     * D. 为确保并发收集器可以发现活动的引用对象ref，不干扰可能将要入队的ref对象的线程，收集器应通过discovered字段链接已发现的对象。
     *    此外, discovered字段还用于链接Pending列表中的引用对象ref。
     */
    /*
     * A.
     * A Reference instance is in one of four possible internal states:
     *
     * 1)
     *     Active: Subject to special treatment by the garbage collector.  Some
     *     time after the collector detects that the reachability of the
     *     referent has changed to the appropriate state, it changes the
     *     instance's state to either Pending or Inactive, depending upon
     *     whether or not the instance was registered with a queue when it was
     *     created.  In the former case it also adds the instance to the
     *     pending-Reference list.  Newly-created instances are Active.
     * 2)
     *     Pending: An element of the pending-Reference list, waiting to be
     *     enqueued by the Reference-handler thread.  Unregistered instances
     *     are never in this state.
     * 3)
     *     Enqueued: An element of the queue with which the instance was
     *     registered when it was created.  When an instance is removed from
     *     its ReferenceQueue, it is made Inactive.  Unregistered instances are
     *     never in this state.
     * 4)
     *     Inactive: Nothing more to do.  Once an instance becomes Inactive its
     *     state will never change again.
     *
     * B.
     * The state is encoded in the queue and next fields as follows:
     *
     * 1)
     *     Active: queue = ReferenceQueue with which instance is registered, or
     *     ReferenceQueue.NULL if it was not registered with a queue; next =
     *     null.
     * 2)
     *     Pending: queue = ReferenceQueue with which instance is registered;
     *     next = this
     * 3)
     *     Enqueued: queue = ReferenceQueue.ENQUEUED; next = Following instance
     *     in queue, or this if at end of list.
     * 4)
     *     Inactive: queue = ReferenceQueue.NULL; next = this.
     *
     * C.
     * With this scheme the collector need only examine the next field in order
     * to determine whether a Reference instance requires special treatment: If
     * the next field is null then the instance is active; if it is non-null,
     * then the collector should treat the instance normally.
     *
     * D.
     * To ensure that a concurrent collector can discover active Reference
     * objects without interfering with application threads that may apply
     * the enqueue() method to those objects, collectors should link
     * discovered objects through the discovered field. The discovered
     * field is also used for linking Reference objects in the pending list.
     */

    // 软/弱/虚 引用的实例对象(即data实际业务对象)
    private T referent;         /* Treated specially by GC */ // GC特殊处理

    // 引用队列: 用于存放JVM标记/清除掉的引用对象ref
    volatile ReferenceQueue<? super T> queue;

    /* When active:   NULL
     *     pending:   this
     *    Enqueued:   next reference in queue (or this if last)
     *    Inactive:   this
     */
    // 引用对象ref链表(如果ref属于队尾元素, 则next为this)
    @SuppressWarnings("rawtypes")
    Reference next;

    /* When active:
     *          // 由GC维护的已发现引用列表中的下一个元素（或者如果是最后一个）
     *          next element in a discovered reference list maintained by GC (or this if last)
     *      pending:
     *          // pending列表中的下一个元素（如果最后一个则为 null）
     *          next element in the pending list (or null if last)
     *      otherwise:
     *          NULL
     */
    // JVM维护的字段, 使用discovered字段来更新pending列表, 即该引用实例ref通过discovered带出下一个pending元素
    transient private Reference<T> discovered;  /* used by VM */

    /**
     * 20210613
     * 用于与垃圾收集器同步的对象。 收集器必须在每个收集周期开始时获取此锁。 因此，持有此锁的任何代码都必须尽快完成、不分配新对象并避免调用用户代码，这一点至关重要。
     */
    /* Object used to synchronize with the garbage collector.  The collector
     * must acquire this lock at the beginning of each collection cycle.  It is
     * therefore critical that any code holding this lock complete as quickly
     * as possible, allocate no new objects, and avoid calling user code.
     */
    static private class Lock { }
    private static Lock lock = new Lock();

    /**
     * 20210613
     * 待入队的引用实例列表。收集器将引用添加到此列表中，而引用处理程序线程将删除它们。 此列表受上述锁定对象保护。 该列表使用discovered字段来链接其元素。
     */
    /* List of References waiting to be enqueued.  The collector adds
     * References to this list, while the Reference-handler thread removes
     * them.  This list is protected by the above lock object. The
     * list uses the discovered field to link its elements.
     */
    // 待入队的引用实例列表, 在检测到适当的可达性更改后，垃圾收集器会将注册的引用对象ref附加到引用队列queue中, JVM使用discovered字段来更新pending列表(即该引用实例ref通过discovered带出下一个pending元素)
    private static Reference<Object> pending = null;

    /**
     * 20210613
     * 用于入队Pending状态的高优先级线程
     */
    /* High-priority thread to enqueue pending References
     */
    private static class ReferenceHandler extends Thread {

        private static void ensureClassInitialized(Class<?> clazz) {
            try {
                Class.forName(clazz.getName(), true, clazz.getClassLoader());
            } catch (ClassNotFoundException e) {
                throw (Error) new NoClassDefFoundError(e.getMessage()).initCause(e);
            }
        }

        static {
            /**
             * 20210613
             * 预加载和初始化 InterruptedException 和 Cleaner 类，这样如果在加载/初始化它们时出现内存不足，我们就不会在运行循环中遇到麻烦。
             */
            // pre-load and initialize InterruptedException and Cleaner classes
            // so that we don't get into trouble later in the run loop if there's
            // memory shortage while loading/initializing them lazily.
            ensureClassInitialized(InterruptedException.class);
            ensureClassInitialized(Cleaner.class);
        }

        ReferenceHandler(ThreadGroup g, String name) {
            super(g, name);
        }

        public void run() {
            while (true) {
                tryHandlePending(true);
            }
        }
    }

    /**
     * 20210613
     * 如果有，请尝试处理待处理的 {@link Reference}。 返回 {@code true} 作为提示，当目前没有更多待处理的 {@link Reference} 并且程序可以做一些其他有用的工作时，
     * 可能还有另一个 {@link Reference} 未决或 {@code false} 而不是循环。
     */
    /**
     * Try handle pending {@link Reference} if there is one.<p>
     * Return {@code true} as a hint that there might be another
     * {@link Reference} pending or {@code false} when there are no more pending
     * {@link Reference}s at the moment and the program can do some other
     * useful work instead of looping.
     *
     * // 如果 {@code true} 且没有待处理的 {@link Reference}，则等待 VM 通知或中断； 如果 {@code false}，则在没有待处理的 {@link Reference} 时立即返回。
     * @param waitForNotify if {@code true} and there was no pending
     *                      {@link Reference}, wait until notified from VM
     *                      or interrupted; if {@code false}, return immediately
     *                      when there is no pending {@link Reference}.
     *
     * // {@code true} 如果有一个 {@link Reference} 待处理并且它被处理，或者我们等待通知并得到它或者线程在被通知之前被中断； {@code false} 否则。
     * @return {@code true} if there was a {@link Reference} pending and it
     *         was processed, or we waited for notification and either got it
     *         or thread was interrupted before being notified;
     *         {@code false} otherwise.
     */
    // 尝试处理Pending列表的引用实例ref, pending成功/无需pending被唤醒成功/OOM让步CPU分片/当前线程被中断都返回true, 只有无需pending且无需阻塞才返回false
    static boolean tryHandlePending(boolean waitForNotify) {
        Reference<Object> r;
        Cleaner c;
        try {
            synchronized (lock) {
                // 如果pending列表不为null, 说明有要pending的元素, 则使用discovered字段来更新pending列表, 即该引用实例ref通过discovered带出下一个pending元素
                if (pending != null) {
                    r = pending;
                    // 'instanceof' 有时可能会抛出 OutOfMemoryError，所以在从 'pending' 链中取消链接 'r' 之前这样做......
                    // 'instanceof' might throw OutOfMemoryError sometimes
                    // so do this before un-linking 'r' from the 'pending' chain...
                    c = r instanceof Cleaner ? (Cleaner) r : null;

                    // unlink 'r' from 'pending' chain
                    pending = r.discovered;
                    r.discovered = null;
                }
                // 如果pending列表为null, 说明没有要pending的元素, 如果需要阻塞, 则线程让出锁, 进入阻塞状态, 直到被唤醒后返回true
                else {
                    // 对锁的等待可能会导致 OutOfMemoryError，因为它可能会尝试分配异常对象。
                    // The waiting on the lock may cause an OutOfMemoryError
                    // because it may try to allocate exception objects.
                    if (waitForNotify) {
                        lock.wait();// 线程让出锁, 进入阻塞状态
                    }
                    // retry if waited
                    return waitForNotify;
                }
            }
        }
        // 如果抛出OOM异常, 则让出CPU分片给其他线程, 让GC能回收一些空间, 此时也返回true
        catch (OutOfMemoryError x) {
            // 给其他线程 CPU 时间，以便他们希望删除一些实时引用并且 GC 回收一些空间。 如果上面的“r instanceof Cleaner”持续抛出 OOME 一段时间，
            // 还要防止 CPU 密集型旋转......
            // Give other threads CPU time so they hopefully drop some live references
            // and GC reclaims some space.
            // Also prevent CPU intensive spinning in case 'r instanceof Cleaner' above
            // persistently throws OOME for some time...
            Thread.yield();

            // retry
            return true;
        }
        // 如果该处理pending元素的守护线程被中断, 也返回true
        catch (InterruptedException x) {
            // retry
            return true;
        }

        // 如果该引用实例ref本身为Clean类型, 则调用clean方法, 返回true
        // Fast path for cleaners
        if (c != null) {
            c.clean();
            return true;
        }

        // 如果discovered字段已更新pending列表, 且该引用实例构造时还指定引用队列实例, 则将ref入队, 返回true
        ReferenceQueue<? super Object> q = r.queue;
        if (q != ReferenceQueue.NULL) q.enqueue(r);
        return true;
    }

    static {
        // tg为root线程组
        ThreadGroup tg = Thread.currentThread().getThreadGroup();
        for (ThreadGroup tgn = tg; tgn != null; tg = tgn, tgn = tg.getParent());
        Thread handler = new ReferenceHandler(tg, "Reference Handler");

        /**
         * 20210613
         * 如果存在大于 MAX_PRIORITY 的特殊系统优先级，则将在此处使用
         */
        /* If there were a special system-only priority greater than
         * MAX_PRIORITY, it would be used here
         */
        handler.setPriority(Thread.MAX_PRIORITY);
        handler.setDaemon(true);// 守护线程, 守护用户线程, 当JVM所有用户线程全部结束后, 守护线程也会随着JVM退出。守护线程标志必须在线程启动前设置为true。
        handler.start();

        // 在 SharedSecrets 中提供访问权限
        // provide access in SharedSecrets
        SharedSecrets.setJavaLangRefAccess(new JavaLangRefAccess() {
            @Override
            public boolean tryHandlePendingReference() {
                // 该守护线程用于: 尝试处理Pending列表的引用实例ref, pending成功/OOM让步CPU分片/当前线程被中断都返回true, 无需pending且无需阻塞, 此时才返回false
                return tryHandlePending(false);
            }
        });
    }

    // 引用访问器和设置器
    /* -- Referent accessor and setters -- */

    /**
     * 20210613
     * 返回此引用对象的引用对象。 如果此引用对象已被程序或垃圾收集器清除，则此方法返回 null。
     */
    /**
     * Returns this reference object's referent.  If this reference object has
     * been cleared, either by the program or by the garbage collector, then
     * this method returns <code>null</code>.
     *
     * @return   The object to which this reference refers, or
     *           <code>null</code> if this reference object has been cleared
     */
    // (非JVM调用)返回软/弱/虚 ref引用的实例对象(即data实际业务对象)
    public T get() {
        return this.referent;
    }

    /**
     * 20210613
     * A. 清除此参考对象。 调用此方法不会导致此对象入队。
     * B. 此方法仅由 Java 代码调用； 当垃圾收集器清除引用时，它会直接清除引用，而不调用此方法。
     */
    /**
     * A.
     * Clears this reference object.  Invoking this method will not cause this
     * object to be enqueued.
     *
     * B.
     * <p> This method is invoked only by Java code; when the garbage collector
     * clears references it does so directly, without invoking this method.
     */
    // (非JVM调用)清空软/弱/虚 ref引用的实例对象(即data实际业务对象)
    public void clear() {
        this.referent = null;
    }


    /* -- Queue operations -- */

    /**
     * 20210613
     * 显示此引用对象是否已被程序或垃圾收集器排入队列。 如果这个引用对象在创建的时候没有注册到队列中，那么这个方法会一直返回false。
     */
    /**
     * Tells whether or not this reference object has been enqueued, either by
     * the program or by the garbage collector.  If this reference object was
     * not registered with a queue when it was created, then this method will
     * always return <code>false</code>.
     *
     * @return   <code>true</code> if and only if this reference object has
     *           been enqueued
     */
    // 判断ref实例是否已经入队成功
    public boolean isEnqueued() {
        return (this.queue == ReferenceQueue.ENQUEUED);
    }

    /**
     * 20210613
     * A. 将此引用对象添加到它注册的队列中（如果有）。
     * B. 此方法仅由 Java 代码调用； 当垃圾收集器将引用排入队列时，它会直接这样做，而不调用此方法。
     */
    /**
     * A.
     * Adds this reference object to the queue with which it is registered,
     * if any.
     *
     * B.
     * <p> This method is invoked only by Java code; when the garbage collector
     * enqueues references it does so directly, without invoking this method.
     *
     * // 如果此引用对象已成功入队，则为 true； 如果它已经入队或在创建时未向队列注册，则为 false
     * @return   <code>true</code> if this reference object was successfully
     *           enqueued; <code>false</code> if it was already enqueued or if
     *           it was not registered with a queue when it was created
     */
    // ref实例入队: 在检测到适当的可达性更改后，垃圾收集器会将注册的引用对象ref附加到引用队列中。
    public boolean enqueue() {
        return this.queue.enqueue(this);
    }


    /* -- Constructors -- */
    // 只指定业务对象的构造方法, 此时不指定引用队列
    Reference(T referent) {
        this(referent, null);
    }

    // 指定业务对象以及指定引用对象的构造方法
    Reference(T referent, ReferenceQueue<? super T> queue) {
        this.referent = referent;
        this.queue = (queue == null) ? ReferenceQueue.NULL : queue;
    }

}
