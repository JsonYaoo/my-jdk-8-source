/*
 * Copyright (c) 1997, 2003, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package java.lang.ref;

/**
 * 20210614
 * A. 虚引用对象，在收集器确定它们的引用对象可能会被回收后, 会其上的虚引用对象加入引用队列进行排队。
 *    虚引用最常用于以比Java终结机制更灵活的方式调度预检清理操作, 比如用于管理堆外内存的释放。
 * B. 如果垃圾收集器在某个时间点确定虚引用的所指对象是虚可达的，那么在那个时候或稍后的某个时间它将将该引用加入队列。
 * C. 为了保证一个可回收的对象保持如此，虚引用的所指对象可能不会被检索：虚引用的get方法总是返回null。
 * D. 与软引用和弱引用不同，虚引用在入队时不会被垃圾收集器自动清除。可通过虚引用访问的对象将保持不变，直到所有此类引用都被清除或自身变得无法访问为止。
 */

/**
 * A.
 * Phantom reference objects, which are enqueued after the collector
 * determines that their referents may otherwise be reclaimed.  Phantom
 * references are most often used for scheduling pre-mortem cleanup actions in
 * a more flexible way than is possible with the Java finalization mechanism.
 *
 * B.
 * <p> If the garbage collector determines at a certain point in time that the
 * referent of a phantom reference is <a
 * href="package-summary.html#reachability">phantom reachable</a>, then at that
 * time or at some later time it will enqueue the reference.
 *
 * C.
 * <p> In order to ensure that a reclaimable object remains so, the referent of
 * a phantom reference may not be retrieved: The <code>get</code> method of a
 * phantom reference always returns <code>null</code>.
 *
 * D.
 * <p> Unlike soft and weak references, phantom references are not
 * automatically cleared by the garbage collector as they are enqueued.  An
 * object that is reachable via phantom references will remain so until all
 * such references are cleared or themselves become unreachable.
 *
 * @author   Mark Reinhold
 * @since    1.2
 */

public class PhantomReference<T> extends Reference<T> {

    /**
     * 20210614
     * 返回此引用对象的引用对象。 由于虚引用的所指对象始终不可访问，因此此方法始终返回 null。
     */
    /**
     * Returns this reference object's referent.  Because the referent of a
     * phantom reference is always inaccessible, this method always returns
     * <code>null</code>.
     *
     * @return  <code>null</code>
     */
    public T get() {
        return null;
    }

    /**
     * 20210614
     * A. 创建一个新的虚引用，该引用引用给定的对象并注册到给定的队列中。
     * B. 可以使用空队列创建虚引用，但这样的引用完全没有用：它的get方法将始终返回空值，并且由于它没有队列，因此永远不会入队。
     */
    /**
     * A.
     * Creates a new phantom reference that refers to the given object and
     * is registered with the given queue.
     *
     * B.
     * <p> It is possible to create a phantom reference with a <tt>null</tt>
     * queue, but such a reference is completely useless: Its <tt>get</tt>
     * method will always return null and, since it does not have a queue, it
     * will never be enqueued.
     *
     * @param referent the object the new phantom reference will refer to
     * @param q the queue with which the reference is to be registered,
     *          or <tt>null</tt> if registration is not required
     */
    public PhantomReference(T referent, ReferenceQueue<? super T> q) {
        super(referent, q);
    }

}
