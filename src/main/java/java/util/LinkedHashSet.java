/*
 * Copyright (c) 2000, 2013, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package java.util;

/**
 * 20210520
 * A. Set接口的哈希表和链表实现，具有可预测的迭代顺序。 此实现与HashSet的不同之处在于，它维护在其所有条目中运行的双向链接列表。 此链表定义了迭代顺序，
 *    即将元素插入到集合中的顺序（插入顺序）。 请注意，如果将元素重新插入到集合中，则插入顺序不会受到影响。
 *    （如果在调用之前s.contains（e）将返回true的情况下调用s.add（e），则将元素e重新插入到set s中。）
 * B. 此实现使客户免于{@link HashSet}提供的未指定的，通常混乱的排序，而不会增加与{@link TreeSet}相关的成本。 无论原始集的实现如何，
 *    都可以使用它来产生与原始集具有相同顺序的集合副本:
 *     void foo(Set s) {
 *         Set copy = new LinkedHashSet(s);
 *         ...
 *     }
 *    如果模块对输入进行设置，复制并随后返回结果（其顺序由复制的顺序确定），则此技术特别有用。 （客户通常希望按退货的顺序将其退回。）
 * C. 此类提供所有可选的Set操作，并允许空元素。 与HashSet一样，它为基本操作（add，contains和remove），假设哈希函数在各个存储桶之间正确分散了元素。
 *    由于维护链接列表会增加开销，因此性能可能会略低于HashSet，但有一个例外：在LinkedHashSet上进行迭代需要的时间与集合的大小成正比，而无论其容量如何。
 *    在HashSet上进行迭代可能会更昂贵，需要的时间与其容量成正比。
 * D. 链接的哈希集具有两个影响其性能的参数：初始容量和负载因子。它们的定义与HashSet一样。 但是请注意，对于此类而言，为初始容量选择过高的值的惩罚并不那么严重。
 *    与HashSet相比，因为此类的迭代时间不受影响按容量。
 * E. 请注意，此实现未同步。 如果多个线程同时访问链接的哈希集，并且至少有一个线程修改了该哈希集，则必须在外部对其进行同步。
 *    通常，通过在自然封装了该集合的某个对象上进行同步来完成此操作。
 * F. 如果不存在这样的对象，则应使用{@link Collections＃synchronizedSet Collections.synchronizedSet}方法来“包装”该集合。
 *    最好在创建时完成此操作，以防止意外地异步访问集合：
 *      Set s = Collections.synchronizedSet(new LinkedHashSet(...));
 * G. 此类的迭代器方法返回的迭代器为
 *    失败快速：如果在创建迭代器后的任何时间修改了集合，则除了通过迭代器自己的remove方法之外，该迭代器将以任何其他方式进行修改，
 *    该迭代器将抛出{@link ConcurrentModificationException}。 因此，面对并发修改，迭代器会快速干净地失败，而不会在未来的不确定时间内冒任意，不确定的行为的风险。
 * H. 请注意，迭代器的快速失败行为无法得到保证，因为通常来说，在存在不同步的并发修改的情况下，不可能做出任何严格的保证。
 *    快速失败的迭代器会尽最大努力抛出ConcurrentModificationException，因此，编写依赖于此异常的程序的正确性是错误的：迭代器的快速失败行为应仅用于检测错误。
 * I. {@docRoot}/../technotes/guides/collections/index.html
 */

/**
 * A.
 * <p>Hash table and linked list implementation of the <tt>Set</tt> interface,
 * with predictable iteration order.  This implementation differs from
 * <tt>HashSet</tt> in that it maintains a doubly-linked list running through
 * all of its entries.  This linked list defines the iteration ordering,
 * which is the order in which elements were inserted into the set
 * (<i>insertion-order</i>).  Note that insertion order is <i>not</i> affected
 * if an element is <i>re-inserted</i> into the set.  (An element <tt>e</tt>
 * is reinserted into a set <tt>s</tt> if <tt>s.add(e)</tt> is invoked when
 * <tt>s.contains(e)</tt> would return <tt>true</tt> immediately prior to
 * the invocation.)
 *
 * B.
 * <p>This implementation spares its clients from the unspecified, generally
 * chaotic ordering provided by {@link HashSet}, without incurring the
 * increased cost associated with {@link TreeSet}.  It can be used to
 * produce a copy of a set that has the same order as the original, regardless
 * of the original set's implementation:
 * <pre>
 *     void foo(Set s) {
 *         Set copy = new LinkedHashSet(s);
 *         ...
 *     }
 * </pre>
 * This technique is particularly useful if a module takes a set on input,
 * copies it, and later returns results whose order is determined by that of
 * the copy.  (Clients generally appreciate having things returned in the same
 * order they were presented.)
 *
 * C.
 * <p>This class provides all of the optional <tt>Set</tt> operations, and
 * permits null elements.  Like <tt>HashSet</tt>, it provides constant-time
 * performance for the basic operations (<tt>add</tt>, <tt>contains</tt> and
 * <tt>remove</tt>), assuming the hash function disperses elements
 * properly among the buckets.  Performance is likely to be just slightly
 * below that of <tt>HashSet</tt>, due to the added expense of maintaining the
 * linked list, with one exception: Iteration over a <tt>LinkedHashSet</tt>
 * requires time proportional to the <i>size</i> of the set, regardless of
 * its capacity.  Iteration over a <tt>HashSet</tt> is likely to be more
 * expensive, requiring time proportional to its <i>capacity</i>.
 *
 * D.
 * <p>A linked hash set has two parameters that affect its performance:
 * <i>initial capacity</i> and <i>load factor</i>.  They are defined precisely
 * as for <tt>HashSet</tt>.  Note, however, that the penalty for choosing an
 * excessively high value for initial capacity is less severe for this class
 * than for <tt>HashSet</tt>, as iteration times for this class are unaffected
 * by capacity.
 *
 * E.
 * <p><strong>Note that this implementation is not synchronized.</strong>
 * If multiple threads access a linked hash set concurrently, and at least
 * one of the threads modifies the set, it <em>must</em> be synchronized
 * externally.  This is typically accomplished by synchronizing on some
 * object that naturally encapsulates the set.
 *
 * F.
 * If no such object exists, the set should be "wrapped" using the
 * {@link Collections#synchronizedSet Collections.synchronizedSet}
 * method.  This is best done at creation time, to prevent accidental
 * unsynchronized access to the set: <pre>
 *   Set s = Collections.synchronizedSet(new LinkedHashSet(...));</pre>
 *
 * G.
 * <p>The iterators returned by this class's <tt>iterator</tt> method are
 * <em>fail-fast</em>: if the set is modified at any time after the iterator
 * is created, in any way except through the iterator's own <tt>remove</tt>
 * method, the iterator will throw a {@link ConcurrentModificationException}.
 * Thus, in the face of concurrent modification, the iterator fails quickly
 * and cleanly, rather than risking arbitrary, non-deterministic behavior at
 * an undetermined time in the future.
 *
 * H.
 * <p>Note that the fail-fast behavior of an iterator cannot be guaranteed
 * as it is, generally speaking, impossible to make any hard guarantees in the
 * presence of unsynchronized concurrent modification.  Fail-fast iterators
 * throw <tt>ConcurrentModificationException</tt> on a best-effort basis.
 * Therefore, it would be wrong to write a program that depended on this
 * exception for its correctness:   <i>the fail-fast behavior of iterators
 * should be used only to detect bugs.</i>
 *
 * I.
 * <p>This class is a member of the
 * <a href="{@docRoot}/../technotes/guides/collections/index.html">
 * Java Collections Framework</a>.
 *
 * @param <E> the type of elements maintained by this set
 *
 * @author  Josh Bloch
 * @see     Object#hashCode()
 * @see     Collection
 * @see     Set
 * @see     HashSet
 * @see     TreeSet
 * @see     Hashtable
 * @since   1.4
 */

public class LinkedHashSet<E> extends HashSet<E> implements Set<E>, Cloneable, java.io.Serializable {

    private static final long serialVersionUID = -2851667679971038690L;

    /**
     * Constructs a new, empty linked hash set with the specified initial
     * capacity and load factor.
     *
     * @param      initialCapacity the initial capacity of the linked hash set
     * @param      loadFactor      the load factor of the linked hash set
     * @throws     IllegalArgumentException  if the initial capacity is less
     *               than zero, or if the load factor is nonpositive
     */
    public LinkedHashSet(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor, true);
    }

    /**
     * Constructs a new, empty linked hash set with the specified initial
     * capacity and the default load factor (0.75).
     *
     * @param   initialCapacity   the initial capacity of the LinkedHashSet
     * @throws  IllegalArgumentException if the initial capacity is less
     *              than zero
     */
    public LinkedHashSet(int initialCapacity) {
        super(initialCapacity, .75f, true);
    }

    /**
     * Constructs a new, empty linked hash set with the default initial
     * capacity (16) and load factor (0.75).
     */
    public LinkedHashSet() {
        super(16, .75f, true);
    }

    /**
     * Constructs a new linked hash set with the same elements as the
     * specified collection.  The linked hash set is created with an initial
     * capacity sufficient to hold the elements in the specified collection
     * and the default load factor (0.75).
     *
     * @param c  the collection whose elements are to be placed into
     *           this set
     * @throws NullPointerException if the specified collection is null
     */
    public LinkedHashSet(Collection<? extends E> c) {
        super(Math.max(2*c.size(), 11), .75f, true);
        addAll(c);
    }

    /**
     * Creates a <em><a href="Spliterator.html#binding">late-binding</a></em>
     * and <em>fail-fast</em> {@code Spliterator} over the elements in this set.
     *
     * <p>The {@code Spliterator} reports {@link Spliterator#SIZED},
     * {@link Spliterator#DISTINCT}, and {@code ORDERED}.  Implementations
     * should document the reporting of additional characteristic values.
     *
     * @implNote
     * The implementation creates a
     * <em><a href="Spliterator.html#binding">late-binding</a></em> spliterator
     * from the set's {@code Iterator}.  The spliterator inherits the
     * <em>fail-fast</em> properties of the set's iterator.
     * The created {@code Spliterator} additionally reports
     * {@link Spliterator#SUBSIZED}.
     *
     * @return a {@code Spliterator} over the elements in this set
     * @since 1.8
     */
    @Override
    public Spliterator<E> spliterator() {
        return Spliterators.spliterator(this, Spliterator.DISTINCT | Spliterator.ORDERED);
    }
}
