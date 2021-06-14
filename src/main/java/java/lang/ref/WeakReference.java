/*
 * Copyright (c) 1997, 2003, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package java.lang.ref;

/**
 * 20210614
 * A. 弱引用对象，不会阻止它们的引用对象被终结、终结和回收。 弱引用最常用于实现规范化映射。
 * B. 假设垃圾收集器在某个时间点确定某个对象是弱可达的。 那时它将原子地清除对该对象的所有弱引用以及对任何其他弱可达对象的所有弱引用，通过强引用和软引用链可以从中访问该对象。
 *    同时它将声明所有以前弱可达的对象是可终结的。 同时或稍后它会将那些新清除的已注册到引用队列的弱引用加入队列。
 */

/**
 * A.
 * Weak reference objects, which do not prevent their referents from being
 * made finalizable, finalized, and then reclaimed.  Weak references are most
 * often used to implement canonicalizing mappings.
 *
 * B.
 * <p> Suppose that the garbage collector determines at a certain point in time
 * that an object is <a href="package-summary.html#reachability">weakly
 * reachable</a>.  At that time it will atomically clear all weak references to
 * that object and all weak references to any other weakly-reachable objects
 * from which that object is reachable through a chain of strong and soft
 * references.  At the same time it will declare all of the formerly
 * weakly-reachable objects to be finalizable.  At the same time or at some
 * later time it will enqueue those newly-cleared weak references that are
 * registered with reference queues.
 *
 * @author   Mark Reinhold
 * @since    1.2
 */

public class WeakReference<T> extends Reference<T> {

    /**
     * 20210614
     * 创建一个引用给定对象的新弱引用。 新引用未在任何队列中注册。
     */
    /**
     * Creates a new weak reference that refers to the given object.  The new
     * reference is not registered with any queue.
     *
     * @param referent object the new weak reference will refer to
     */
    // 只指定业务对象的构造方法, 此时不指定引用队列
    public WeakReference(T referent) {
        super(referent);
    }

    /**
     * 20210614
     * 创建一个新的弱引用，它引用给定的对象并注册到给定的队列中。
     */
    /**
     * Creates a new weak reference that refers to the given object and is
     * registered with the given queue.
     *
     * @param referent object the new weak reference will refer to
     * @param q the queue with which the reference is to be registered,
     *          or <tt>null</tt> if registration is not required
     */
    // 指定业务对象以及指定引用对象的构造方法
    public WeakReference(T referent, ReferenceQueue<? super T> q) {
        super(referent, q);
    }

}
