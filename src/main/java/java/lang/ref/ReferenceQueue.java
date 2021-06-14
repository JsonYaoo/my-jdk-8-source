/*
 * Copyright (c) 1997, 2013, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package java.lang.ref;

/**
 * 20210613
 * 引用队列，在检测到适当的可达性更改后，垃圾收集器会将注册的引用对象附加到该队列中。
 */

/**
 * Reference queues, to which registered reference objects are appended by the
 * garbage collector after the appropriate reachability changes are detected.
 *
 * @author   Mark Reinhold
 * @since    1.2
 */

public class ReferenceQueue<T> {

    /**
     * Constructs a new reference-object queue.
     */
    public ReferenceQueue() { }

    private static class Null<S> extends ReferenceQueue<S> {
        boolean enqueue(Reference<? extends S> r) {
            return false;
        }
    }

    static ReferenceQueue<Object> NULL = new Null<>();// NULL, 表示该ref实例在构造时不指定队列实例
    static ReferenceQueue<Object> ENQUEUED = new Null<>();// ENQUEUED, 表示该ref实例已经入队了

    static private class Lock { };
    private Lock lock = new Lock();
    private volatile Reference<? extends T> head = null;
    private long queueLength = 0;

    // ref实例入队
    boolean enqueue(Reference<? extends T> r) { /* Called only by Reference class */
        synchronized (lock) {
            // 检查自从获得锁定以来，这个引用还没有被排队（甚至被删除）
            // Check that since getting the lock this reference hasn't already been
            // enqueued (and even then removed)

            // 如果ref实例没指定引用队列 或者 ref实例已经入队, 则无需入队, 直接返回false即可
            ReferenceQueue<?> queue = r.queue;
            if ((queue == NULL) || (queue == ENQUEUED)) {
                return false;
            }

            // 否则说明ref队列适合入队, 则更新queue状态为ENQUEUED, 更新r.next、head为自身
            assert queue == this;
            r.queue = ENQUEUED;
            r.next = (head == null) ? r : head;
            head = r;
            queueLength++;
            if (r instanceof FinalReference) {
                sun.misc.VM.addFinalRefCount(1);
            }

            // 唤醒阻塞的remove(long timeout)所在的线程
            lock.notifyAll();
            return true;
        }
    }

    @SuppressWarnings("unchecked")
    // 出队ref实例
    private Reference<? extends T> reallyPoll() {       /* Must hold lock */
        Reference<? extends T> r = head;
        if (r != null) {
            // 更新head指针, 指向null或者下一个元素
            head = (r.next == r) ?
                null :
                r.next; // Unchecked due to the next field having a raw type in Reference // 由于参考中的下一个字段具有原始类型，因此未选中

            // 更新ref实例的queue为NULL, next为r本身
            r.queue = NULL;
            r.next = r;
            queueLength--;
            if (r instanceof FinalReference) {
                sun.misc.VM.addFinalRefCount(-1);
            }
            return r;
        }
        return null;
    }

    /**
     * 20210614
     * 轮询此队列以查看参考对象是否可用。 如果一个可用而没有进一步的延迟，则将其从队列中删除并返回。 否则此方法立即返回 null。
     */
    /**
     * Polls this queue to see if a reference object is available.  If one is
     * available without further delay then it is removed from the queue and
     * returned.  Otherwise this method immediately returns <tt>null</tt>.
     *
     * @return  A reference object, if one was immediately available,
     *          otherwise <code>null</code>
     */
    // 加锁, 出队ref实例
    public Reference<? extends T> poll() {
        if (head == null)
            return null;
        synchronized (lock) {
            return reallyPoll();
        }
    }

    /**
     * 20210614
     * A. 移除此队列中的下一个引用对象，阻塞直到其中一个变为可用或给定的超时期限到期。
     * B. 此方法不提供实时保证：它通过调用 {@link Object#wait(long)} 方法来安排超时。
     */
    /**
     * A.
     * Removes the next reference object in this queue, blocking until either
     * one becomes available or the given timeout period expires.
     *
     * B.
     * <p> This method does not offer real-time guarantees: It schedules the
     * timeout as if by invoking the {@link Object#wait(long)} method.
     *
     * @param  timeout  If positive, block for up to <code>timeout</code>
     *                  milliseconds while waiting for a reference to be
     *                  added to this queue.  If zero, block indefinitely.
     *
     * @return  A reference object, if one was available within the specified
     *          timeout period, otherwise <code>null</code>
     *
     * @throws  IllegalArgumentException
     *          If the value of the timeout argument is negative
     *
     * @throws  InterruptedException
     *          If the timeout wait is interrupted
     */
    // 加锁, 出队ref实例, 如果返回的ref实例为null, 则阻塞等待timeout时间, 0表示无限阻塞
    public Reference<? extends T> remove(long timeout)
        throws IllegalArgumentException, InterruptedException
    {
        if (timeout < 0) {
            throw new IllegalArgumentException("Negative timeout value");
        }
        synchronized (lock) {
            Reference<? extends T> r = reallyPoll();
            if (r != null) return r;
            long start = (timeout == 0) ? 0 : System.nanoTime();
            for (;;) {
                lock.wait(timeout);
                r = reallyPoll();
                if (r != null) return r;
                if (timeout != 0) {
                    long end = System.nanoTime();
                    timeout -= (end - start) / 1000_000;
                    if (timeout <= 0) return null;
                    start = end;
                }
            }
        }
    }

    /**
     * 20210614
     * 移除此队列中的下一个引用对象，阻塞直到一个可用。
     */
    /**
     * Removes the next reference object in this queue, blocking until one
     * becomes available.
     *
     * @return A reference object, blocking until one becomes available
     * @throws  InterruptedException  If the wait is interrupted
     */
    // 加锁, 出队ref实例, 如果返回的ref实例为null, 则无限阻塞等待
    public Reference<? extends T> remove() throws InterruptedException {
        return remove(0);
    }

}
