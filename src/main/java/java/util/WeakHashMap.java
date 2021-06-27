/*
 * Copyright (c) 1998, 2013, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package java.util;

import java.lang.ref.WeakReference;
import java.lang.ref.ReferenceQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * 20210613
 * A. Map接口基于弱键的哈希表实现。WeakHashMap中的条目将在其键不再正常使用时自动删除。更准确地说，给定键的映射的存在不会阻止该键被垃圾收集器丢弃，即使其可终结、终结，
 *    然后被回收。 当一个键被丢弃时，它的条目被有效地从映射中删除，所以这个类的行为与其他 Map 实现有些不同。
 * B. 支持null值和null键。 该类具有与HashMap类相似的性能特征，具有相同的初始容量和负载因子的效率参数。
 * C. 像大多数集合类一样，这个类不是同步的。 可以使用 {@link Collections#synchronizedMap Collections.synchronizedMap} 方法构造同步的 WeakHashMap。
 * D. 此类主要用于其equals方法使用 == 运算符测试对象身份的关键对象。 一旦这样的键被丢弃，它就永远不会被重新创建，所以以后不可能在 WeakHashMap 中查找该键，
 *    并且会惊讶于它的条目已被删除。 此类将与关键对象完美配合，这些对象的 equals 方法不基于对象标识，例如 String 实例。 然而，对于这种可重新创建的键对象，
 *    自动删除键已被丢弃的 WeakHashMap 条目可能会令人困惑。
 * E. WeakHashMap类的行为部分取决于垃圾收集器的操作，因此一些熟悉的（虽然不是必需的）Map 不变量不适用于此类。 因为垃圾收集器可能随时丢弃键，
 *    所以WeakHashMap可能表现得好像一个未知线程正在默默地删除条目。特别是，即使您在WeakHashMap实例上进行同步并且不调用它的任何mutator方法，随着时间的推移，
 *    size方法可能返回较小的值，isEmpty方法返回false然后返回true，containsKey方法返回对于给定的键为真，后来为假，对于get方法返回给定键的值但后来返回null，
 *    对于put方法返回null和remove方法为以前出现在映射，以及对键集、值集合和条目集的连续检查，以产生连续较小数量的元素。
 * F. WeakHashMap中的每个键对象都被间接存储为弱引用的所指对象。因此，只有在垃圾收集器清除了Map内外的弱引用后，才会自动删除键。
 * G. 实现说明：WeakHashMap中的值对象由普通的强引用持有。因此应该注意确保值对象不会直接或间接地强烈引用它们自己的键，因为这将防止键被丢弃。
 *    请注意，值对象可以通过WeakHashMap本身间接引用其键；也就是说，一个值对象可能会强烈引用某个其他的键对象，其关联的值对象又会强烈引用第一个值对象的键。
 *    如果映射中的值不依赖于对它们进行强引用的映射，则解决此问题的一种方法是在插入之前将值本身包装在WeakReferences中，如：
 *    m.put(key, new WeakReference(value))，然后在每次获取时展开。
 * H. 由该类的所有“集合视图方法”返回的集合的迭代器方法返回的迭代器是快速失败的：如果在迭代器创建后的任何时间以任何方式修改映射结构，除了通过迭代器自己的删除方法，
 *    迭代器将抛出一个 {@link ConcurrentModificationException}。 因此，面对并发修改，迭代器快速而干净地失败，而不是在未来不确定的时间冒着任意、非确定性行为的风险。
 * I. 请注意，无法保证迭代器的快速失败行为，因为一般而言，在存在非同步并发修改的情况下不可能做出任何硬保证。 快速失败的迭代器会在尽力而为的基础上抛出
 *    ConcurrentModificationException。因此，编写依赖此异常来确保其正确性的程序是错误的：迭代器的快速失败行为应该仅用于检测错误。
 * J. {@docRoot}/../technotes/guides/collections/index.html
 */
/**
 * A.
 * Hash table based implementation of the <tt>Map</tt> interface, with
 * <em>weak keys</em>.
 * An entry in a <tt>WeakHashMap</tt> will automatically be removed when
 * its key is no longer in ordinary use.  More precisely, the presence of a
 * mapping for a given key will not prevent the key from being discarded by the
 * garbage collector, that is, made finalizable, finalized, and then reclaimed.
 * When a key has been discarded its entry is effectively removed from the map,
 * so this class behaves somewhat differently from other <tt>Map</tt>
 * implementations.
 *
 * B.
 * <p> Both null values and the null key are supported. This class has
 * performance characteristics similar to those of the <tt>HashMap</tt>
 * class, and has the same efficiency parameters of <em>initial capacity</em>
 * and <em>load factor</em>.
 *
 * C.
 * <p> Like most collection classes, this class is not synchronized.
 * A synchronized <tt>WeakHashMap</tt> may be constructed using the
 * {@link Collections#synchronizedMap Collections.synchronizedMap}
 * method.
 *
 * D.
 * <p> This class is intended primarily for use with key objects whose
 * <tt>equals</tt> methods test for object identity using the
 * <tt>==</tt> operator.  Once such a key is discarded it can never be
 * recreated, so it is impossible to do a lookup of that key in a
 * <tt>WeakHashMap</tt> at some later time and be surprised that its entry
 * has been removed.  This class will work perfectly well with key objects
 * whose <tt>equals</tt> methods are not based upon object identity, such
 * as <tt>String</tt> instances.  With such recreatable key objects,
 * however, the automatic removal of <tt>WeakHashMap</tt> entries whose
 * keys have been discarded may prove to be confusing.
 *
 * E.
 * <p> The behavior of the <tt>WeakHashMap</tt> class depends in part upon
 * the actions of the garbage collector, so several familiar (though not
 * required) <tt>Map</tt> invariants do not hold for this class.  Because
 * the garbage collector may discard keys at any time, a
 * <tt>WeakHashMap</tt> may behave as though an unknown thread is silently
 * removing entries.  In particular, even if you synchronize on a
 * <tt>WeakHashMap</tt> instance and invoke none of its mutator methods, it
 * is possible for the <tt>size</tt> method to return smaller values over
 * time, for the <tt>isEmpty</tt> method to return <tt>false</tt> and
 * then <tt>true</tt>, for the <tt>containsKey</tt> method to return
 * <tt>true</tt> and later <tt>false</tt> for a given key, for the
 * <tt>get</tt> method to return a value for a given key but later return
 * <tt>null</tt>, for the <tt>put</tt> method to return
 * <tt>null</tt> and the <tt>remove</tt> method to return
 * <tt>false</tt> for a key that previously appeared to be in the map, and
 * for successive examinations of the key set, the value collection, and
 * the entry set to yield successively smaller numbers of elements.
 *
 * F.
 * <p> Each key object in a <tt>WeakHashMap</tt> is stored indirectly as
 * the referent of a weak reference.  Therefore a key will automatically be
 * removed only after the weak references to it, both inside and outside of the
 * map, have been cleared by the garbage collector.
 *
 * G.
 * <p> <strong>Implementation note:</strong> The value objects in a
 * <tt>WeakHashMap</tt> are held by ordinary strong references.  Thus care
 * should be taken to ensure that value objects do not strongly refer to their
 * own keys, either directly or indirectly, since that will prevent the keys
 * from being discarded.  Note that a value object may refer indirectly to its
 * key via the <tt>WeakHashMap</tt> itself; that is, a value object may
 * strongly refer to some other key object whose associated value object, in
 * turn, strongly refers to the key of the first value object.  If the values
 * in the map do not rely on the map holding strong references to them, one way
 * to deal with this is to wrap values themselves within
 * <tt>WeakReferences</tt> before
 * inserting, as in: <tt>m.put(key, new WeakReference(value))</tt>,
 * and then unwrapping upon each <tt>get</tt>.
 *
 * H.
 * <p>The iterators returned by the <tt>iterator</tt> method of the collections
 * returned by all of this class's "collection view methods" are
 * <i>fail-fast</i>: if the map is structurally modified at any time after the
 * iterator is created, in any way except through the iterator's own
 * <tt>remove</tt> method, the iterator will throw a {@link
 * ConcurrentModificationException}.  Thus, in the face of concurrent
 * modification, the iterator fails quickly and cleanly, rather than risking
 * arbitrary, non-deterministic behavior at an undetermined time in the future.
 *
 * I.
 * <p>Note that the fail-fast behavior of an iterator cannot be guaranteed
 * as it is, generally speaking, impossible to make any hard guarantees in the
 * presence of unsynchronized concurrent modification.  Fail-fast iterators
 * throw <tt>ConcurrentModificationException</tt> on a best-effort basis.
 * Therefore, it would be wrong to write a program that depended on this
 * exception for its correctness:  <i>the fail-fast behavior of iterators
 * should be used only to detect bugs.</i>
 *
 * J.
 * <p>This class is a member of the
 * <a href="{@docRoot}/../technotes/guides/collections/index.html">
 * Java Collections Framework</a>.
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 *
 * @author      Doug Lea
 * @author      Josh Bloch
 * @author      Mark Reinhold
 * @since       1.2
 * @see         java.util.HashMap
 * @see         java.lang.ref.WeakReference
 */
public class WeakHashMap<K,V> extends AbstractMap<K,V> implements Map<K,V> {

    /**
     * The default initial capacity -- MUST be a power of two.
     */
    // 默认初始容量 -- 必须是 2 的幂。
    private static final int DEFAULT_INITIAL_CAPACITY = 16;

    /**
     * The maximum capacity, used if a higher value is implicitly specified
     * by either of the constructors with arguments.
     * MUST be a power of two <= 1<<30.
     */
    // 最大容量，在两个带参数的构造函数隐式指定更高值时使用。 必须是 2 的幂 <= 1<<30。
    private static final int MAXIMUM_CAPACITY = 1 << 30;

    /**
     * The load factor used when none specified in constructor.
     */
    // 在构造函数中未指定时使用的负载因子。
    private static final float DEFAULT_LOAD_FACTOR = 0.75f;

    /**
     * The table, resized as necessary. Length MUST Always be a power of two.
     */
    // table，根据需要调整大小。 长度必须始终是 2 的幂。
    Entry<K,V>[] table;

    /**
     * The number of key-value mappings contained in this weak hash map.
     */
    // 此弱哈希映射中包含的键值映射的数量。
    private int size;

    /**
     * The next size value at which to resize (capacity * load factor).
     */
    // 要调整大小的下一个大小值（容量 * 负载因子）。
    private int threshold;

    /**
     * The load factor for the hash table.
     */
    // 哈希表的负载因子。
    private final float loadFactor;

    /**
     * Reference queue for cleared WeakEntries
     */
    // 已清除的 WeakEntries 的引用队列
    private final ReferenceQueue<Object> queue = new ReferenceQueue<>();

    /**
     * 20210614
     * 此 WeakHashMap 在结构上被修改的次数。 结构修改是那些改变映射中映射数量或以其他方式修改其内部结构（例如，重新哈希）的修改。
     * 此字段用于使Map的集合视图上的迭代器快速失败。
     */
    /**
     * The number of times this WeakHashMap has been structurally modified.
     * Structural modifications are those that change the number of
     * mappings in the map or otherwise modify its internal structure
     * (e.g., rehash).  This field is used to make iterators on
     * Collection-views of the map fail-fast.
     *
     * @see ConcurrentModificationException
     */
    int modCount;

    @SuppressWarnings("unchecked")
    // 构造指定容量的散列表数组
    private Entry<K,V>[] newTable(int n) {
        return (Entry<K,V>[]) new Entry<?,?>[n];
    }

    /**
     * 20210614
     * 使用给定的初始容量和给定的负载因子构造一个新的空 WeakHashMap。
     */
    /**
     * Constructs a new, empty <tt>WeakHashMap</tt> with the given initial
     * capacity and the given load factor.
     *
     * @param  initialCapacity The initial capacity of the <tt>WeakHashMap</tt>
     * @param  loadFactor      The load factor of the <tt>WeakHashMap</tt>
     * @throws IllegalArgumentException if the initial capacity is negative,
     *         or if the load factor is nonpositive.
     */
    public WeakHashMap(int initialCapacity, float loadFactor) {
        if (initialCapacity < 0)
            throw new IllegalArgumentException("Illegal Initial Capacity: "+
                                               initialCapacity);
        if (initialCapacity > MAXIMUM_CAPACITY)
            initialCapacity = MAXIMUM_CAPACITY;

        if (loadFactor <= 0 || Float.isNaN(loadFactor))
            throw new IllegalArgumentException("Illegal Load factor: "+
                                               loadFactor);

        // 扩容到接近指定初始容量的2^n
        int capacity = 1;
        while (capacity < initialCapacity)
            capacity <<= 1;

        // 构造指定容量的散列表数组
        table = newTable(capacity);
        this.loadFactor = loadFactor;

        // 计算阈值
        threshold = (int)(capacity * loadFactor);
    }

    /**
     * 20210614
     * 使用给定的初始容量和默认加载因子 (0.75) 构造一个新的空 WeakHashMap。
     */
    /**
     * Constructs a new, empty <tt>WeakHashMap</tt> with the given initial
     * capacity and the default load factor (0.75).
     *
     * @param  initialCapacity The initial capacity of the <tt>WeakHashMap</tt>
     * @throws IllegalArgumentException if the initial capacity is negative
     */
    public WeakHashMap(int initialCapacity) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR);
    }

    /**
     * 20210614
     * 使用默认初始容量 (16) 和负载因子 (0.75) 构造一个新的空 WeakHashMap。
     */
    /**
     * Constructs a new, empty <tt>WeakHashMap</tt> with the default initial
     * capacity (16) and load factor (0.75).
     */
    public WeakHashMap() {
        this(DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR);
    }

    /**
     * 20210614
     * 构造一个与指定映射具有相同映射关系的新WeakHashMap。WeakHashMap 是使用默认负载因子 (0.75) 和足以容纳指定映射中的初始容量创建。
     */
    /**
     * Constructs a new <tt>WeakHashMap</tt> with the same mappings as the
     * specified map.  The <tt>WeakHashMap</tt> is created with the default
     * load factor (0.75) and an initial capacity sufficient to hold the
     * mappings in the specified map.
     *
     * @param   m the map whose mappings are to be placed in this map
     * @throws  NullPointerException if the specified map is null
     * @since   1.3
     */
    public WeakHashMap(Map<? extends K, ? extends V> m) {
        // 取计算得到的容量((复制集合的实际大小 / 默认负载因子0.75) + 1) 和 默认容量16之间的最大值作为初始容量, 取默认负载因子0.75
        this(Math.max((int) (m.size() / DEFAULT_LOAD_FACTOR) + 1,
                DEFAULT_INITIAL_CAPACITY),
             DEFAULT_LOAD_FACTOR);

        // 遍历指定复制集合所有的条目, 并添加到WeakHashMap的散列表中: 添加前如果复制集合的大小大于当前阈值, 则会先对散列表进行扩容,
        // 同时删除每个桶链上已被清除弱引用的Entry条目, 注意的是扩容后不一定会使用新容量的散列表(只有当删除的条目减少到大于阈值一半时才会)
        putAll(m);
    }

    // internal utilities

    /**
     * Value representing null keys inside tables.
     */
    // 表示table内空键的值。
    private static final Object NULL_KEY = new Object();

    /**
     * Use NULL_KEY for key if it is null.
     */
    // 如果键为null，则使用 NULL_KEY 作为键。
    private static Object maskNull(Object key) {
        return (key == null) ? NULL_KEY : key;
    }

    /**
     * Returns internal representation of null key back to caller as null.
     */
    // 将null键的内部表示作为null返回给调用者。
    static Object unmaskNull(Object key) {
        return (key == NULL_KEY) ? null : key;
    }

    /**
     * Checks for equality of non-null reference x and possibly-null y.  By
     * default uses Object.equals.
     */
    // 使用==和Object.equals比较Key对象是否相等。                       s。
    private static boolean eq(Object x, Object y) {
        return x == y || x.equals(y);
    }

    /**
     * 20210616
     * 检索对象散列代码并将补充散列函数应用于结果散列，以防止质量差的散列函数。这很关键，因为HashMap使用长度为2的幂的哈希表，否则会遇到低位没有不同的hashCode的冲突。
     */
    /**
     * Retrieve object hash code and applies a supplemental hash function to the
     * result hash, which defends against poor quality hash functions.  This is
     * critical because HashMap uses power-of-two length hash tables, that
     * otherwise encounter collisions for hashCodes that do not differ
     * in lower bits.
     */
    // HashCode扰动函数: 尽量做到任何一位的变化都能对最终得到的结果产生影响, 降低哈希冲突的概率
    final int hash(Object k) {
        int h = k.hashCode();

        // 此函数可确保在每个位位置仅相差常数倍的hashCode具有有限数量的冲突（在默认加载因子下约为8）。
        // This function ensures that hashCodes that differ only by
        // constant multiples at each bit position have a bounded
        // number of collisions (approximately 8 at default load factor).

        // 为了把高位的特征和低位的特征组合起来，降低哈希冲突的概率，也就是说，尽量做到任何一位的变化都能对最终得到的结果产生影响
        h ^= (h >>> 20) ^ (h >>> 12);
        return h ^ (h >>> 7) ^ (h >>> 4);
    }

    /**
     * Returns index for hash code h.
     */
    // 返回哈希值h在散列表数组中的索引
    private static int indexFor(int h, int length) {
        return h & (length-1);
    }

    /**
     * Expunges stale entries from the table.
     */
    // 从表中删除桶链上已被清除弱引用的Entry条目: table[i] -> Entry --> Key, 由于Key只被Entry"弱引用", 当Key被清除后, 通过WeakReference#get()获取真实Key强引用为null,
    // 相当于Entry.key==null了(虽然WeakHashMap的Entry没有key属性), 此时如果不删除Entry.value以及让Entry脱钩table[i],
    // 则会存在Value对象内存泄露问题(因为key被置null了, 在外界看来Value应该是被清空了的, 但实际上Value却还在), 因此需要从引用队列获取条目Entry e进而删除该条目
    private void expungeStaleEntries() {
        for (Object x; (x = queue.poll()) != null; ) {
            synchronized (queue) {
                @SuppressWarnings("unchecked")
                Entry<K,V> e = (Entry<K,V>) x;

                // 重新取模条目e.hash与散列表长度, 计算条目e在散列表数组中的索引, 来获取e所在的桶链表prev
                int i = indexFor(e.hash, table.length);
                Entry<K,V> prev = table[i];
                Entry<K,V> p = prev;

                // 遍历table[i]桶链, p为当前遍历的结点, prev作为p的前驱
                while (p != null) {
                    Entry<K,V> next = p.next;

                    // 如果当前结点p为在引用队列获取到的条目, 说明遍历找到了e条目
                    if (p == e) {
                        // 如果条目e是链头结点, 则next成为新的链头
                        if (prev == e)
                            table[i] = next;
                        // 如果条目e不是链头结点, 则链接p的前驱prev与next结点
                        else
                            prev.next = next;

                        // Must not null out e.next;
                        // stale entries may be in use by a HashIterator

                        // 链接e的前驱和后继完毕, 则清空e的value, 更新实际大小, 但不得将e.next置为空, 因为HashIterator可能正在使用陈旧的条目(桶链上已被清除弱引用的Entry条目)
                        e.value = null; // Help GC
                        size--;

                        // 退出循环, 此时Entry e已经没有了原来table[i]桶链的结点引用了, 也就是条目e被删除了, 很快该Entry就会被回收掉(但是e#next还在, 但是不影响e的回收)
                        break;
                    }

                    // 如果当前结点p不为在引用队列获取到的条目, 说明还没找到了e条目, 则更新prev和p指针, 继续往后找
                    prev = p;
                    p = next;
                }
            }
        }
    }

    /**
     * Returns the table after first expunging stale entries.
     */
    // 从表中删除桶链上陈旧条目（已被清除弱引用的Entry条目）后，返回散列表数组
    private Entry<K,V>[] getTable() {
        // 从表中删除桶链上已被清除弱引用的Entry条目: table[i] -> Entry --> Key, 由于Key只被Entry"弱引用", 当Key被清除后, 通过WeakReference#get()获取真实Key强引用为null,
        // 相当于Entry.key==null了(虽然WeakHashMap的Entry没有key属性), 此时如果不删除Entry.value以及让Entry脱钩table[i],
        // 则会存在Value对象内存泄露问题(因为key被置null了, 在外界看来Value应该是被清空了的, 但实际上Value却还在), 因此需要从引用队列获取条目Entry e进而删除该条目
        expungeStaleEntries();
        return table;
    }

    /**
     * Returns the number of key-value mappings in this map.
     * This result is a snapshot, and may not reflect unprocessed
     * entries that will be removed before next attempted access
     * because they are no longer referenced.
     */
    public int size() {
        if (size == 0)
            return 0;
        expungeStaleEntries();
        return size;
    }

    /**
     * Returns <tt>true</tt> if this map contains no key-value mappings.
     * This result is a snapshot, and may not reflect unprocessed
     * entries that will be removed before next attempted access
     * because they are no longer referenced.
     */
    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * 20210616
     * A. 返回指定键映射到的值，如果此映射不包含键的映射，则返回 {@code null}。
     * B. 更正式地说，如果此映射包含从键 {@code k} 到值 {@code v} 的映射，使得 {@code (key==null ? k==null : key.equals(k))}， 然后这个方法返回 {@code v};
     *    否则返回 {@code null}。 （最多可以有一个这样的映射。）
     * C. {@code null} 的返回值不一定表示映射不包含键的映射； 映射也可能将键显式映射到 {@code null}。{@link #containsKey containsKey} 操作可用于区分这两种情况。
     */
    /**
     * A.
     * Returns the value to which the specified key is mapped,
     * or {@code null} if this map contains no mapping for the key.
     *
     * B.
     * <p>More formally, if this map contains a mapping from a key
     * {@code k} to a value {@code v} such that {@code (key==null ? k==null :
     * key.equals(k))}, then this method returns {@code v}; otherwise
     * it returns {@code null}.  (There can be at most one such mapping.)
     *
     * C.
     * <p>A return value of {@code null} does not <i>necessarily</i>
     * indicate that the map contains no mapping for the key; it's also
     * possible that the map explicitly maps the key to {@code null}.
     * The {@link #containsKey containsKey} operation may be used to
     * distinguish these two cases.
     *
     * @see #put(Object, Object)
     */
    // 获取指定键映射到的值，如果此映射不包含键的映射，则返回 {@code null}。
    public V get(Object key) {
        // 如果键为null，则使用 NULL_KEY 作为键。
        Object k = maskNull(key);

        // HashCode扰动函数: 尽量做到任何一位的变化都能对最终得到的结果产生影响, 降低哈希冲突的概率
        int h = hash(k);

        // 从表中删除桶链上陈旧条目（已被清除弱引用的Entry条目）后，返回散列表数组
        Entry<K,V>[] tab = getTable();

        // 返回哈希值h在散列表数组中的索引
        int index = indexFor(h, tab.length);

        // 计算出的索引对应散列表数组中的桶e
        Entry<K,V> e = tab[index];

        // 遍历e桶链表, e为当前链表遍历的结点, 如果找到hash相等, 且Key对象也相等的结点, 说明e结点就是要找的结点, 此时返回e的value值
        while (e != null) {
            if (e.hash == h && eq(k, e.get()))
                return e.value;
            e = e.next;
        }

        // 如果最后都找不到要删除的结点, 则返回null
        return null;
    }

    /**
     * Returns <tt>true</tt> if this map contains a mapping for the
     * specified key.
     *
     * @param  key   The key whose presence in this map is to be tested
     * @return <tt>true</tt> if there is a mapping for <tt>key</tt>;
     *         <tt>false</tt> otherwise
     */
    public boolean containsKey(Object key) {
        return getEntry(key) != null;
    }

    /**
     * Returns the entry associated with the specified key in this map.
     * Returns null if the map contains no mapping for this key.
     */
    Entry<K,V> getEntry(Object key) {
        Object k = maskNull(key);
        int h = hash(k);
        Entry<K,V>[] tab = getTable();
        int index = indexFor(h, tab.length);
        Entry<K,V> e = tab[index];
        while (e != null && !(e.hash == h && eq(k, e.get())))
            e = e.next;
        return e;
    }

    /**
     * 20210616
     * 将指定值与此映射中的指定键相关联。如果映射先前包含此键的映射，则替换旧值。
     */
    /**
     * Associates the specified value with the specified key in this map.
     * If the map previously contained a mapping for this key, the old
     * value is replaced.
     *
     * // 与指定值相关联的键。
     * @param key key with which the specified value is to be associated.
     *
     * // 要与指定键关联的值。
     * @param value value to be associated with the specified key.
     *
     * // 与 key 关联的先前值，如果没有 key 的映射，则为 null。 （null返回也可以表明映射先前将null与键相关联。）
     * @return the previous value associated with <tt>key</tt>, or
     *         <tt>null</tt> if there was no mapping for <tt>key</tt>.
     *         (A <tt>null</tt> return can also indicate that the map
     *         previously associated <tt>null</tt> with <tt>key</tt>.)
     */
    // 将指定值和指定键相关联，形成key-value键值对，如果包含相同的键，则会替换旧值。
    public V put(K key, V value) {
        // 如果键为null，则使用NULL_KEY作为键。
        Object k = maskNull(key);

        // HashCode扰动函数: 尽量做到任何一位的变化都能对最终得到的结果产生影响, 降低哈希冲突的概率
        int h = hash(k);

        // 从表中删除桶链上陈旧条目（已被清除弱引用的Entry条目）后，返回散列表数组
        Entry<K,V>[] tab = getTable();

        // 返回哈希值h在散列表数组中的索引
        int i = indexFor(h, tab.length);

        // 计算出的索引对应的桶e, 遍历e链表, 通过Entry#get()获取Key对象的强引用, 使用==和Object.equals比较Key对象是否相等
        for (Entry<K,V> e = tab[i]; e != null; e = e.next) {
            // 如果Entry.hash相等, 且Key对象相等, 则说明存在相同键的Entry, 此时需要替换条目, 即替换旧值, 返回旧值
            if (h == e.hash && eq(k, e.get())) {
                V oldValue = e.value;
                if (value != oldValue)
                    e.value = value;
                return oldValue;
            }
        }

        modCount++;

        // 如果不存在相同键的Entry, 说明需要添加Entry, 此时构造传入Key、Value对象的Entry结点, 并插入到tab[i]桶的桶头中(HashMap JDK7是头插, JDK8是尾插), 原来的桶头e成为next结点
        Entry<K,V> e = tab[i];
        tab[i] = new Entry<>(k, value, queue, h, e);

        // 添加后, 如果实际大小大于阈值, 则指定容量(2倍实际大小)进行散列表扩容: 从表中删除桶链上已被清除弱引用的Entry条目后, 如果散列表实际大小减小到阈值一半后, 则继续使用旧散列表, 否则使用新散列表并重新计算阈值
        if (++size >= threshold)
            resize(tab.length * 2);
        return null;
    }

    /**
     * 20210615
     * A. 将此映射的内容重新散列到具有更大容量的新数组中。 当此映射中的键数达到其阈值时，将自动调用此方法。
     * B. 如果当前容量为 MAXIMUM_CAPACITY，则此方法不会调整Map大小，而是将阈值设置为 Integer.MAX_VALUE。 这具有防止将来调用的效果。
     */
    /**
     * A.
     * Rehashes the contents of this map into a new array with a
     * larger capacity.  This method is called automatically when the
     * number of keys in this map reaches its threshold.
     *
     * B.
     * If current capacity is MAXIMUM_CAPACITY, this method does not
     * resize the map, but sets threshold to Integer.MAX_VALUE.
     * This has the effect of preventing future calls.
     *
     * @param newCapacity the new capacity, MUST be a power of two;
     *        must be greater than current capacity unless current
     *        capacity is MAXIMUM_CAPACITY (in which case value
     *        is irrelevant).
     */
    // 指定容量进行散列表扩容: 从表中删除桶链上已被清除弱引用的Entry条目后, 如果散列表实际大小减小到阈值一半后, 则继续使用旧散列表, 否则使用新散列表并重新计算阈值
    void resize(int newCapacity) {
        // 从表中删除桶链上已被清除弱引用的Entry条目后, 返回散列表数组
        Entry<K,V>[] oldTable = getTable();

        // 如果容量达到极限, 则计算最大阈值
        int oldCapacity = oldTable.length;
        if (oldCapacity == MAXIMUM_CAPACITY) {
            threshold = Integer.MAX_VALUE;
            return;
        }

        // 构造指定容量的新散列表数组
        Entry<K,V>[] newTable = newTable(newCapacity);

        // 把所有条目从oldTable数组移动到newTable数组(e.hash重新对数组取模), 同时清除每个桶链上已被清除弱引用的Entry条目
        transfer(oldTable, newTable);

        // 使用新容量的散列表
        table = newTable;

        /**
         * 20210615
         * 如果忽略null元素和处理引用队列导致大量收缩，则恢复旧表。 这应该很少见，但避免了垃圾填充表的无限扩展。
         */
        /*
         * If ignoring null elements and processing ref queue caused massive
         * shrinkage, then restore old table.  This should be rare, but avoids
         * unbounded expansion of garbage-filled tables.
         */
        // 如果清除陈旧条目后的实际大小 大于等于 当前阈值的一半, 说明散列表中至少有一半阈值的真实元素,
        // 此时确认是要使用新容量的散列表了, 则需要准备下一个阈值, 根据指定的容量计算阈值
        if (size >= threshold / 2) {
            threshold = (int)(newCapacity * loadFactor);
        }
        // 如果清除陈旧条目后的实际大小 小于 当前阈值的一半, 说明散列表中一半阈值的真实元素都没有, 即之前清除过一批弱引用的Entry条目,
        // 也就是以前的散列表还是可以装得下的(因为没超过一般的阈值), 此时还是继续使用旧的散列表, 把原本的条目移动回旧的散列表
        else {
            // 从表中删除桶链上已被清除弱引用的Entry条目: table[i] -> Entry --> Key, 由于Key只被Entry"弱引用", 当Key被清除后, 通过WeakReference#get()获取真实Key强引用为null,
            // 相当于Entry.key==null了(虽然WeakHashMap的Entry没有key属性), 此时如果不删除Entry.value以及让Entry脱钩table[i],
            // 则会存在Value对象内存泄露问题(因为key被置null了, 在外界看来Value应该是被清空了的, 但实际上Value却还在), 因此需要从引用队列获取条目Entry e进而删除该条目
            expungeStaleEntries();

            // 把所有条目从newTable数组还原到oldTable数组(e.hash重新对数组取模), 同时清除每个桶链上已被清除弱引用的Entry条目
            transfer(newTable, oldTable);

            // 继续使用旧的散列表
            table = oldTable;
        }
    }

    /** Transfers all entries from src to dest tables */
    // 将所有条目从src数组传输到dest数组(hash重新对数组取模), 同时清除每个桶链上已被清除弱引用的Entry条目
    private void transfer(Entry<K,V>[] src, Entry<K,V>[] dest) {
        // 遍历老散列表数组, 当前遍历的条目e
        for (int j = 0; j < src.length; ++j) {
            Entry<K,V> e = src[j];

            // 清空e的原散列表上的桶
            src[j] = null;

            // 遍历e桶上的链表, 当前遍历结点e
            while (e != null) {
                Entry<K,V> next = e.next;

                // 如果e与Key之间的"弱引用"已经被清除, 也即Key对象被清除了, 说明e已经无效了, 则删除链表上的Entry e结点
                Object key = e.get();
                if (key == null) {
                    e.next = null;  // Help GC
                    e.value = null; //  "   "
                    size--;
                }
                // 如果e与Key之间的"弱引用"还没被清除, 也即Key对象还没被清除, 说明e还是有效的, 则计算e.hash在新散列表数组中的索引, 进行头插法
                else {
                    int i = indexFor(e.hash, dest.length);
                    e.next = dest[i];// 头插, 后继链接到原来的链头(HashMap JDK7是头插, JDK8是尾插)
                    dest[i] = e;// 头插(HashMap JDK7是头插, JDK8是尾插)
                }

                // 继续遍历当前链表
                e = next;
            }
        }
    }

    /**
     * 20210614
     * 将所有映射从指定映射复制到此映射。 这些映射将替换此映射对当前在指定映射中的任何键的任何映射。
     */
    /**
     * Copies all of the mappings from the specified map to this map.
     * These mappings will replace any mappings that this map had for any
     * of the keys currently in the specified map.
     *
     * @param m mappings to be stored in this map.
     * @throws  NullPointerException if the specified map is null.
     */
    // 遍历指定复制集合所有的条目, 并添加到WeakHashMap的散列表中: 添加前如果复制集合的大小大于当前阈值, 则会先对散列表进行扩容,
    // 同时删除每个桶链上已被清除弱引用的Entry条目, 注意的是扩容后不一定会使用新容量的散列表(只有当删除的条目减少到大于阈值一半时才会)
    public void putAll(Map<? extends K, ? extends V> m) {
        /**
         * 20210614
         * 如果要添加的映射数大于或等于阈值，则扩展映射。这是保守的；显而易见的条件是(m.size() + size) >= threshold，但如果要添加的键与此映射中已有的键重叠，
         * 则此条件可能导致映射具有两倍的适当容量。通过使用保守的计算，我们最多可以进行一次额外的调整。
         */
        /*
         * Expand the map if the map if the number of mappings to be added
         * is greater than or equal to threshold.  This is conservative; the
         * obvious condition is (m.size() + size) >= threshold, but this
         * condition could result in a map with twice the appropriate capacity,
         * if the keys to be added overlap with the keys already in this map.
         * By using the conservative calculation, we subject ourself
         * to at most one extra resize.
         */
        // 如果复制集合的大小大于当前阈值, 则先计算新的容量(最接近targetCapacity的2^n), 而targetCapacity=复制集合实际大小 * 负载因子
        int numKeysToBeAdded = m.size();
        if (numKeysToBeAdded == 0) return;
        if (numKeysToBeAdded > threshold) {
            int targetCapacity = (int)(numKeysToBeAdded / loadFactor + 1);
            if (targetCapacity > MAXIMUM_CAPACITY)
                targetCapacity = MAXIMUM_CAPACITY;

            // 扩容到接近当前容量的2^n
            int newCapacity = table.length;
            while (newCapacity < targetCapacity)
                newCapacity <<= 1;

            // 如果新容量确实变大了, 则指定新容量(最接近targetCapacity的2^n)进行散列表扩容: 从表中删除桶链上已被清除弱引用的Entry条目后,
            // 如果散列表实际大小减小到阈值一半后, 则继续使用旧散列表, 否则使用新散列表并重新计算阈值
            if (newCapacity > table.length)
                resize(newCapacity);
        }

        // 遍历指定复制集合所有的条目, 并添加到WeakHashMap的散列表中
        for (Map.Entry<? extends K, ? extends V> e : m.entrySet())
            put(e.getKey(), e.getValue());
    }

    /**
     * 20210616
     * A. 如果存在，则从此弱哈希映射中删除键的映射。更正式地，如果此映射包含从键 k 到值 v 的映射，
     *    使得 (key==null ? k==null : key.equals(k))，则删除该映射。（Map最多可以包含一个这样的映射。）
     * B. 返回此映射先前与键关联的值，如果映射不包含键的映射，则返回 null。 返回值 null 不一定表示映射不包含键的映射； 映射也可能将键显式映射到空值。
     * C. 一旦调用返回，映射将不包含指定键的映射。
     */
    /**
     * A.
     * Removes the mapping for a key from this weak hash map if it is present.
     * More formally, if this map contains a mapping from key <tt>k</tt> to
     * value <tt>v</tt> such that <code>(key==null ?  k==null :
     * key.equals(k))</code>, that mapping is removed.  (The map can contain
     * at most one such mapping.)
     *
     * B.
     * <p>Returns the value to which this map previously associated the key,
     * or <tt>null</tt> if the map contained no mapping for the key.  A
     * return value of <tt>null</tt> does not <i>necessarily</i> indicate
     * that the map contained no mapping for the key; it's also possible
     * that the map explicitly mapped the key to <tt>null</tt>.
     *
     * C.
     * <p>The map will not contain a mapping for the specified key once the
     * call returns.
     *
     * @param key key whose mapping is to be removed from the map
     * @return the previous value associated with <tt>key</tt>, or
     *         <tt>null</tt> if there was no mapping for <tt>key</tt>
     */
    // 如果key存在，则从此映射中删除该key对应的映射，忽略value值。
    public V remove(Object key) {
        // 如果键为null，则使用 NULL_KEY 作为键。
        Object k = maskNull(key);

        // HashCode扰动函数: 尽量做到任何一位的变化都能对最终得到的结果产生影响, 降低哈希冲突的概率
        int h = hash(k);

        // 从表中删除桶链上陈旧条目（已被清除弱引用的Entry条目）后，返回散列表数组
        Entry<K,V>[] tab = getTable();

        // 返回哈希值h在散列表数组中的索引
        int i = indexFor(h, tab.length);

        // 计算出的索引对应散列表数组中的桶e
        Entry<K,V> prev = tab[i];
        Entry<K,V> e = prev;

        // 遍历e桶链表, e为当前链表遍历的结点, prev为e的前驱, next为e的后继
        while (e != null) {
            Entry<K,V> next = e.next;

            // 如果找到hash相等, 且Key对象也相等的结点, 说明e结点就是要删除的结点, 则脱钩e结点
            if (h == e.hash && eq(k, e.get())) {
                modCount++;
                size--;

                // 如果e为桶头结点, 则脱离桶头结点
                if (prev == e)
                    tab[i] = next;
                // 如果e不为桶头结点, 则脱钩e结点
                else
                    prev.next = next;

                // 返回e结点的value值
                return e.value;
            }

            // 如果还没找到要删除的结点, 则交换prev、e指针, 继续遍历链表
            prev = e;
            e = next;
        }

        // 如果最后都找不到要删除的结点, 则返回null
        return null;
    }

    /** Special version of remove needed by Entry set */
    boolean removeMapping(Object o) {
        if (!(o instanceof Map.Entry))
            return false;
        Entry<K,V>[] tab = getTable();
        Map.Entry<?,?> entry = (Map.Entry<?,?>)o;
        Object k = maskNull(entry.getKey());
        int h = hash(k);
        int i = indexFor(h, tab.length);
        Entry<K,V> prev = tab[i];
        Entry<K,V> e = prev;

        while (e != null) {
            Entry<K,V> next = e.next;
            if (h == e.hash && e.equals(entry)) {
                modCount++;
                size--;
                if (prev == e)
                    tab[i] = next;
                else
                    prev.next = next;
                return true;
            }
            prev = e;
            e = next;
        }

        return false;
    }

    /**
     * Removes all of the mappings from this map.
     * The map will be empty after this call returns.
     */
    public void clear() {
        // clear out ref queue. We don't need to expunge entries
        // since table is getting cleared.
        while (queue.poll() != null)
            ;

        modCount++;
        Arrays.fill(table, null);
        size = 0;

        // Allocation of array may have caused GC, which may have caused
        // additional entries to go stale.  Removing these entries from the
        // reference queue will make them eligible for reclamation.
        while (queue.poll() != null)
            ;
    }

    /**
     * Returns <tt>true</tt> if this map maps one or more keys to the
     * specified value.
     *
     * @param value value whose presence in this map is to be tested
     * @return <tt>true</tt> if this map maps one or more keys to the
     *         specified value
     */
    public boolean containsValue(Object value) {
        if (value==null)
            return containsNullValue();

        Entry<K,V>[] tab = getTable();
        for (int i = tab.length; i-- > 0;)
            for (Entry<K,V> e = tab[i]; e != null; e = e.next)
                if (value.equals(e.value))
                    return true;
        return false;
    }

    /**
     * Special-case code for containsValue with null argument
     */
    private boolean containsNullValue() {
        Entry<K,V>[] tab = getTable();
        for (int i = tab.length; i-- > 0;)
            for (Entry<K,V> e = tab[i]; e != null; e = e.next)
                if (e.value==null)
                    return true;
        return false;
    }

    /**
     * 20210614
     * 这个哈希表中的条目扩展了 WeakReference，使用它的主要 ref 字段作为键。
     */
    /**
     * The entries in this hash table extend WeakReference, using its main ref
     * field as the key.
     */
    // Entry就是一个WeakReference, 没有Entry.key, 实际上是通过Entry --> Key弱引用Key对象, 通过WeakReference#get()来获取真实Key对象的强引用
    private static class Entry<K,V> extends WeakReference<Object> implements Map.Entry<K,V> {
        V value;
        final int hash;
        Entry<K,V> next;

        /**
         * Creates new entry.
         */
        Entry(Object key, V value,
              ReferenceQueue<Object> queue,
              int hash, Entry<K,V> next) {
            super(key, queue);// Entry"弱引用“Key对象(table[i] -> Entry --> Key)
            this.value = value;
            this.hash  = hash;
            this.next  = next;
        }

        @SuppressWarnings("unchecked")
        public K getKey() {
            return (K) WeakHashMap.unmaskNull(get());
        }

        public V getValue() {
            return value;
        }

        public V setValue(V newValue) {
            V oldValue = value;
            value = newValue;
            return oldValue;
        }

        public boolean equals(Object o) {
            if (!(o instanceof Map.Entry))
                return false;
            Map.Entry<?,?> e = (Map.Entry<?,?>)o;
            K k1 = getKey();
            Object k2 = e.getKey();
            if (k1 == k2 || (k1 != null && k1.equals(k2))) {
                V v1 = getValue();
                Object v2 = e.getValue();
                if (v1 == v2 || (v1 != null && v1.equals(v2)))
                    return true;
            }
            return false;
        }

        public int hashCode() {
            K k = getKey();
            V v = getValue();
            return Objects.hashCode(k) ^ Objects.hashCode(v);
        }

        public String toString() {
            return getKey() + "=" + getValue();
        }
    }

    private abstract class HashIterator<T> implements Iterator<T> {
        private int index;
        private Entry<K,V> entry;
        private Entry<K,V> lastReturned;
        private int expectedModCount = modCount;

        /**
         * Strong reference needed to avoid disappearance of key
         * between hasNext and next
         */
        private Object nextKey;// 需要强引用以避免 hasNext 和 next 之间的键消失

        /**
         * Strong reference needed to avoid disappearance of key
         * between nextEntry() and any use of the entry
         */
        private Object currentKey;// 需要强引用以避免 nextEntry() 和条目的任何使用之间的键消失

        HashIterator() {
            index = isEmpty() ? 0 : table.length;
        }

        public boolean hasNext() {
            Entry<K,V>[] t = table;

            while (nextKey == null) {
                Entry<K,V> e = entry;
                int i = index;
                while (e == null && i > 0)
                    e = t[--i];
                entry = e;
                index = i;
                if (e == null) {
                    currentKey = null;
                    return false;
                }
                nextKey = e.get(); // hold on to key in strong ref
                if (nextKey == null)
                    entry = entry.next;
            }
            return true;
        }

        // 跨不同类型迭代器的 next() 的公共部分
        /** The common parts of next() across different types of iterators */
        protected Entry<K,V> nextEntry() {
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
            if (nextKey == null && !hasNext())
                throw new NoSuchElementException();

            lastReturned = entry;
            entry = entry.next;
            currentKey = nextKey;
            nextKey = null;
            return lastReturned;
        }

        public void remove() {
            if (lastReturned == null)
                throw new IllegalStateException();
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();

            WeakHashMap.this.remove(currentKey);
            expectedModCount = modCount;
            lastReturned = null;
            currentKey = null;
        }

    }

    private class ValueIterator extends HashIterator<V> {
        public V next() {
            return nextEntry().value;
        }
    }

    private class KeyIterator extends HashIterator<K> {
        public K next() {
            return nextEntry().getKey();
        }
    }

    private class EntryIterator extends HashIterator<Map.Entry<K,V>> {
        public Map.Entry<K,V> next() {
            return nextEntry();
        }
    }

    // Views

    private transient Set<Map.Entry<K,V>> entrySet;

    /**
     * Returns a {@link Set} view of the keys contained in this map.
     * The set is backed by the map, so changes to the map are
     * reflected in the set, and vice-versa.  If the map is modified
     * while an iteration over the set is in progress (except through
     * the iterator's own <tt>remove</tt> operation), the results of
     * the iteration are undefined.  The set supports element removal,
     * which removes the corresponding mapping from the map, via the
     * <tt>Iterator.remove</tt>, <tt>Set.remove</tt>,
     * <tt>removeAll</tt>, <tt>retainAll</tt>, and <tt>clear</tt>
     * operations.  It does not support the <tt>add</tt> or <tt>addAll</tt>
     * operations.
     */
    public Set<K> keySet() {
        Set<K> ks = keySet;
        return (ks != null ? ks : (keySet = new KeySet()));
    }

    private class KeySet extends AbstractSet<K> {
        public Iterator<K> iterator() {
            return new KeyIterator();
        }

        public int size() {
            return WeakHashMap.this.size();
        }

        public boolean contains(Object o) {
            return containsKey(o);
        }

        public boolean remove(Object o) {
            if (containsKey(o)) {
                WeakHashMap.this.remove(o);
                return true;
            }
            else
                return false;
        }

        public void clear() {
            WeakHashMap.this.clear();
        }

        public Spliterator<K> spliterator() {
            return new KeySpliterator<>(WeakHashMap.this, 0, -1, 0, 0);
        }
    }

    /**
     * Returns a {@link Collection} view of the values contained in this map.
     * The collection is backed by the map, so changes to the map are
     * reflected in the collection, and vice-versa.  If the map is
     * modified while an iteration over the collection is in progress
     * (except through the iterator's own <tt>remove</tt> operation),
     * the results of the iteration are undefined.  The collection
     * supports element removal, which removes the corresponding
     * mapping from the map, via the <tt>Iterator.remove</tt>,
     * <tt>Collection.remove</tt>, <tt>removeAll</tt>,
     * <tt>retainAll</tt> and <tt>clear</tt> operations.  It does not
     * support the <tt>add</tt> or <tt>addAll</tt> operations.
     */
    public Collection<V> values() {
        Collection<V> vs = values;
        return (vs != null) ? vs : (values = new Values());
    }

    private class Values extends AbstractCollection<V> {
        public Iterator<V> iterator() {
            return new ValueIterator();
        }

        public int size() {
            return WeakHashMap.this.size();
        }

        public boolean contains(Object o) {
            return containsValue(o);
        }

        public void clear() {
            WeakHashMap.this.clear();
        }

        public Spliterator<V> spliterator() {
            return new ValueSpliterator<>(WeakHashMap.this, 0, -1, 0, 0);
        }
    }

    /**
     * Returns a {@link Set} view of the mappings contained in this map.
     * The set is backed by the map, so changes to the map are
     * reflected in the set, and vice-versa.  If the map is modified
     * while an iteration over the set is in progress (except through
     * the iterator's own <tt>remove</tt> operation, or through the
     * <tt>setValue</tt> operation on a map entry returned by the
     * iterator) the results of the iteration are undefined.  The set
     * supports element removal, which removes the corresponding
     * mapping from the map, via the <tt>Iterator.remove</tt>,
     * <tt>Set.remove</tt>, <tt>removeAll</tt>, <tt>retainAll</tt> and
     * <tt>clear</tt> operations.  It does not support the
     * <tt>add</tt> or <tt>addAll</tt> operations.
     */
    public Set<Map.Entry<K,V>> entrySet() {
        Set<Map.Entry<K,V>> es = entrySet;
        return es != null ? es : (entrySet = new EntrySet());
    }

    private class EntrySet extends AbstractSet<Map.Entry<K,V>> {
        public Iterator<Map.Entry<K,V>> iterator() {
            return new EntryIterator();
        }

        public boolean contains(Object o) {
            if (!(o instanceof Map.Entry))
                return false;
            Map.Entry<?,?> e = (Map.Entry<?,?>)o;
            Entry<K,V> candidate = getEntry(e.getKey());
            return candidate != null && candidate.equals(e);
        }

        public boolean remove(Object o) {
            return removeMapping(o);
        }

        public int size() {
            return WeakHashMap.this.size();
        }

        public void clear() {
            WeakHashMap.this.clear();
        }

        private List<Map.Entry<K,V>> deepCopy() {
            List<Map.Entry<K,V>> list = new ArrayList<>(size());
            for (Map.Entry<K,V> e : this)
                list.add(new AbstractMap.SimpleEntry<>(e));
            return list;
        }

        public Object[] toArray() {
            return deepCopy().toArray();
        }

        public <T> T[] toArray(T[] a) {
            return deepCopy().toArray(a);
        }

        public Spliterator<Map.Entry<K,V>> spliterator() {
            return new EntrySpliterator<>(WeakHashMap.this, 0, -1, 0, 0);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void forEach(BiConsumer<? super K, ? super V> action) {
        Objects.requireNonNull(action);
        int expectedModCount = modCount;

        Entry<K, V>[] tab = getTable();
        for (Entry<K, V> entry : tab) {
            while (entry != null) {
                Object key = entry.get();
                if (key != null) {
                    action.accept((K)WeakHashMap.unmaskNull(key), entry.value);
                }
                entry = entry.next;

                if (expectedModCount != modCount) {
                    throw new ConcurrentModificationException();
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        Objects.requireNonNull(function);
        int expectedModCount = modCount;

        Entry<K, V>[] tab = getTable();;
        for (Entry<K, V> entry : tab) {
            while (entry != null) {
                Object key = entry.get();
                if (key != null) {
                    entry.value = function.apply((K)WeakHashMap.unmaskNull(key), entry.value);
                }
                entry = entry.next;

                if (expectedModCount != modCount) {
                    throw new ConcurrentModificationException();
                }
            }
        }
    }

    /**
     * Similar form as other hash Spliterators, but skips dead
     * elements.
     */
    static class WeakHashMapSpliterator<K,V> {
        final WeakHashMap<K,V> map;
        WeakHashMap.Entry<K,V> current; // current node
        int index;             // current index, modified on advance/split
        int fence;             // -1 until first use; then one past last index
        int est;               // size estimate
        int expectedModCount;  // for comodification checks

        WeakHashMapSpliterator(WeakHashMap<K,V> m, int origin,
                               int fence, int est,
                               int expectedModCount) {
            this.map = m;
            this.index = origin;
            this.fence = fence;
            this.est = est;
            this.expectedModCount = expectedModCount;
        }

        final int getFence() { // initialize fence and size on first use
            int hi;
            if ((hi = fence) < 0) {
                WeakHashMap<K,V> m = map;
                est = m.size();
                expectedModCount = m.modCount;
                hi = fence = m.table.length;
            }
            return hi;
        }

        public final long estimateSize() {
            getFence(); // force init
            return (long) est;
        }
    }

    static final class KeySpliterator<K,V>
        extends WeakHashMapSpliterator<K,V>
        implements Spliterator<K> {
        KeySpliterator(WeakHashMap<K,V> m, int origin, int fence, int est,
                       int expectedModCount) {
            super(m, origin, fence, est, expectedModCount);
        }

        public KeySpliterator<K,V> trySplit() {
            int hi = getFence(), lo = index, mid = (lo + hi) >>> 1;
            return (lo >= mid) ? null :
                new KeySpliterator<K,V>(map, lo, index = mid, est >>>= 1,
                                        expectedModCount);
        }

        public void forEachRemaining(Consumer<? super K> action) {
            int i, hi, mc;
            if (action == null)
                throw new NullPointerException();
            WeakHashMap<K,V> m = map;
            WeakHashMap.Entry<K,V>[] tab = m.table;
            if ((hi = fence) < 0) {
                mc = expectedModCount = m.modCount;
                hi = fence = tab.length;
            }
            else
                mc = expectedModCount;
            if (tab.length >= hi && (i = index) >= 0 &&
                (i < (index = hi) || current != null)) {
                WeakHashMap.Entry<K,V> p = current;
                current = null; // exhaust
                do {
                    if (p == null)
                        p = tab[i++];
                    else {
                        Object x = p.get();
                        p = p.next;
                        if (x != null) {
                            @SuppressWarnings("unchecked") K k =
                                (K) WeakHashMap.unmaskNull(x);
                            action.accept(k);
                        }
                    }
                } while (p != null || i < hi);
            }
            if (m.modCount != mc)
                throw new ConcurrentModificationException();
        }

        public boolean tryAdvance(Consumer<? super K> action) {
            int hi;
            if (action == null)
                throw new NullPointerException();
            WeakHashMap.Entry<K,V>[] tab = map.table;
            if (tab.length >= (hi = getFence()) && index >= 0) {
                while (current != null || index < hi) {
                    if (current == null)
                        current = tab[index++];
                    else {
                        Object x = current.get();
                        current = current.next;
                        if (x != null) {
                            @SuppressWarnings("unchecked") K k =
                                (K) WeakHashMap.unmaskNull(x);
                            action.accept(k);
                            if (map.modCount != expectedModCount)
                                throw new ConcurrentModificationException();
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        public int characteristics() {
            return Spliterator.DISTINCT;
        }
    }

    static final class ValueSpliterator<K,V>
        extends WeakHashMapSpliterator<K,V>
        implements Spliterator<V> {
        ValueSpliterator(WeakHashMap<K,V> m, int origin, int fence, int est,
                         int expectedModCount) {
            super(m, origin, fence, est, expectedModCount);
        }

        public ValueSpliterator<K,V> trySplit() {
            int hi = getFence(), lo = index, mid = (lo + hi) >>> 1;
            return (lo >= mid) ? null :
                new ValueSpliterator<K,V>(map, lo, index = mid, est >>>= 1,
                                          expectedModCount);
        }

        public void forEachRemaining(Consumer<? super V> action) {
            int i, hi, mc;
            if (action == null)
                throw new NullPointerException();
            WeakHashMap<K,V> m = map;
            WeakHashMap.Entry<K,V>[] tab = m.table;
            if ((hi = fence) < 0) {
                mc = expectedModCount = m.modCount;
                hi = fence = tab.length;
            }
            else
                mc = expectedModCount;
            if (tab.length >= hi && (i = index) >= 0 &&
                (i < (index = hi) || current != null)) {
                WeakHashMap.Entry<K,V> p = current;
                current = null; // exhaust
                do {
                    if (p == null)
                        p = tab[i++];
                    else {
                        Object x = p.get();
                        V v = p.value;
                        p = p.next;
                        if (x != null)
                            action.accept(v);
                    }
                } while (p != null || i < hi);
            }
            if (m.modCount != mc)
                throw new ConcurrentModificationException();
        }

        public boolean tryAdvance(Consumer<? super V> action) {
            int hi;
            if (action == null)
                throw new NullPointerException();
            WeakHashMap.Entry<K,V>[] tab = map.table;
            if (tab.length >= (hi = getFence()) && index >= 0) {
                while (current != null || index < hi) {
                    if (current == null)
                        current = tab[index++];
                    else {
                        Object x = current.get();
                        V v = current.value;
                        current = current.next;
                        if (x != null) {
                            action.accept(v);
                            if (map.modCount != expectedModCount)
                                throw new ConcurrentModificationException();
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        public int characteristics() {
            return 0;
        }
    }

    static final class EntrySpliterator<K,V>
        extends WeakHashMapSpliterator<K,V>
        implements Spliterator<Map.Entry<K,V>> {
        EntrySpliterator(WeakHashMap<K,V> m, int origin, int fence, int est,
                       int expectedModCount) {
            super(m, origin, fence, est, expectedModCount);
        }

        public EntrySpliterator<K,V> trySplit() {
            int hi = getFence(), lo = index, mid = (lo + hi) >>> 1;
            return (lo >= mid) ? null :
                new EntrySpliterator<K,V>(map, lo, index = mid, est >>>= 1,
                                          expectedModCount);
        }


        public void forEachRemaining(Consumer<? super Map.Entry<K, V>> action) {
            int i, hi, mc;
            if (action == null)
                throw new NullPointerException();
            WeakHashMap<K,V> m = map;
            WeakHashMap.Entry<K,V>[] tab = m.table;
            if ((hi = fence) < 0) {
                mc = expectedModCount = m.modCount;
                hi = fence = tab.length;
            }
            else
                mc = expectedModCount;
            if (tab.length >= hi && (i = index) >= 0 &&
                (i < (index = hi) || current != null)) {
                WeakHashMap.Entry<K,V> p = current;
                current = null; // exhaust
                do {
                    if (p == null)
                        p = tab[i++];
                    else {
                        Object x = p.get();
                        V v = p.value;
                        p = p.next;
                        if (x != null) {
                            @SuppressWarnings("unchecked") K k =
                                (K) WeakHashMap.unmaskNull(x);
                            action.accept
                                (new AbstractMap.SimpleImmutableEntry<K,V>(k, v));
                        }
                    }
                } while (p != null || i < hi);
            }
            if (m.modCount != mc)
                throw new ConcurrentModificationException();
        }

        public boolean tryAdvance(Consumer<? super Map.Entry<K,V>> action) {
            int hi;
            if (action == null)
                throw new NullPointerException();
            WeakHashMap.Entry<K,V>[] tab = map.table;
            if (tab.length >= (hi = getFence()) && index >= 0) {
                while (current != null || index < hi) {
                    if (current == null)
                        current = tab[index++];
                    else {
                        Object x = current.get();
                        V v = current.value;
                        current = current.next;
                        if (x != null) {
                            @SuppressWarnings("unchecked") K k =
                                (K) WeakHashMap.unmaskNull(x);
                            action.accept
                                (new AbstractMap.SimpleImmutableEntry<K,V>(k, v));
                            if (map.modCount != expectedModCount)
                                throw new ConcurrentModificationException();
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        public int characteristics() {
            return Spliterator.DISTINCT;
        }
    }

}
