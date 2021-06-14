/*
 * Copyright (c) 1997, 2003, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package java.lang.ref;

/**
 * 20210614
 * A. 软引用对象，由垃圾收集器根据内存需求自行决定清除。 软引用最常用于实现内存敏感的缓存。
 * B. 假设垃圾收集器在某个时间点确定一个对象是软可达的。 那时，它可以选择以原子方式清除对该对象的所有软引用以及对任何其他软可达对象的所有软引用，
 *    通过强引用链可以从中访问该对象。 同时或稍后它会将那些注册到引用队列的新清除的软引用加入队列。
 * C. 在虚拟机抛出 OutOfMemoryError 之前，保证已清除对软可访问对象的所有软引用。 否则，对清除软引用的时间或清除对不同对象的一组此类引用的顺序没有限制。
 *    然而，鼓励虚拟机实现偏向于清除最近创建或最近使用的软引用。
 * D. 此类的直接实例可用于实现简单的缓存；这个类或派生的子类也可以用在更大的数据结构中来实现更复杂的缓存。只要软引用的所指对象是强可达的，即实际在使用中，软引用就不会被清除。
 *    因此，一个复杂的缓存可以，例如，通过保持对这些条目的强引用来防止其最近使用的条目被丢弃，剩下的条目由垃圾收集器自行决定丢弃。
 */

/**
 * A.
 * Soft reference objects, which are cleared at the discretion of the garbage
 * collector in response to memory demand.  Soft references are most often used
 * to implement memory-sensitive caches.
 *
 * B.
 * <p> Suppose that the garbage collector determines at a certain point in time
 * that an object is <a href="package-summary.html#reachability">softly
 * reachable</a>.  At that time it may choose to clear atomically all soft
 * references to that object and all soft references to any other
 * softly-reachable objects from which that object is reachable through a chain
 * of strong references.  At the same time or at some later time it will
 * enqueue those newly-cleared soft references that are registered with
 * reference queues.
 *
 * C.
 * <p> All soft references to softly-reachable objects are guaranteed to have
 * been cleared before the virtual machine throws an
 * <code>OutOfMemoryError</code>.  Otherwise no constraints are placed upon the
 * time at which a soft reference will be cleared or the order in which a set
 * of such references to different objects will be cleared.  Virtual machine
 * implementations are, however, encouraged to bias against clearing
 * recently-created or recently-used soft references.
 *
 * D.
 * <p> Direct instances of this class may be used to implement simple caches;
 * this class or derived subclasses may also be used in larger data structures
 * to implement more sophisticated caches.  As long as the referent of a soft
 * reference is strongly reachable, that is, is actually in use, the soft
 * reference will not be cleared.  Thus a sophisticated cache can, for example,
 * prevent its most recently used entries from being discarded by keeping
 * strong referents to those entries, leaving the remaining entries to be
 * discarded at the discretion of the garbage collector.
 *
 * @author   Mark Reinhold
 * @since    1.2
 */

public class SoftReference<T> extends Reference<T> {

    /**
     * 20210614
     * 时间戳时钟，由垃圾收集器更新
     */
    /**
     * Timestamp clock, updated by the garbage collector
     */
    static private long clock;

    /**
     * 20210614
     * 每次调用 get 方法都会更新时间戳。 VM 可以在选择要清除的软引用时使用此字段，但这不是必需的。
     */
    /**
     * Timestamp updated by each invocation of the get method.  The VM may use
     * this field when selecting soft references to be cleared, but it is not
     * required to do so.
     */
    private long timestamp;

    /**
     * 20210614
     * 创建引用给定对象的新软引用。 新引用未在任何队列中注册。
     */
    /**
     * Creates a new soft reference that refers to the given object.  The new
     * reference is not registered with any queue.
     *
     * @param referent object the new soft reference will refer to
     */
    // 只指定业务对象的构造方法, 此时不指定引用队列
    public SoftReference(T referent) {
        super(referent);
        this.timestamp = clock;
    }

    /**
     * 20210614
     * 创建一个新的软引用，它引用给定的对象并注册到给定的队列中。
     */
    /**
     * Creates a new soft reference that refers to the given object and is
     * registered with the given queue.
     *
     * @param referent object the new soft reference will refer to
     * @param q the queue with which the reference is to be registered,
     *          or <tt>null</tt> if registration is not required
     *
     */
    // 指定业务对象以及指定引用对象的构造方法
    public SoftReference(T referent, ReferenceQueue<? super T> q) {
        super(referent, q);
        this.timestamp = clock;
    }

    /**
     * 20210614
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
    // (非JVM调用)返回软ref引用的实例对象(即data实际业务对象)
    public T get() {
        T o = super.get();
        if (o != null && this.timestamp != clock)
            this.timestamp = clock;
        return o;
    }

}
