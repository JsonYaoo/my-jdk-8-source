/*
 * Copyright (c) 1997, 2014, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package java.util;

import java.io.Serializable;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * 20210609
 * A. 基于红黑树的 {@link NavigableMap} 实现。 Map根据其键的 {@linkplain Comparable natural ordering} 进行排序，或者根据Map创建时提供的 {@link Comparator}
 *    进行排序，具体取决于使用的构造函数。
 * B. 此实现为 {@code containsKey}、{@code get}、{@code put} 和 {@code remove} 操作提供有保证的 log(n) 时间成本。
 *    算法是对 Cormen、Leiserson 和 Rivest 的算法导论中的算法的改编。
 * C. 请注意，树形映射维护的排序，与任何排序映射一样，无论是否提供显式比较器，如果此排序映射要正确实现 {@code Map} 接口，则必须与 {@code equals} 一致。
 *    （请参阅 {@code Comparable} 或 {@code Comparator} 以获取与 equals 一致的精确定义。）之所以如此，是因为 {@code Map} 接口是根据 {@code equals} 操作定义的，
 *    但是排序的 map 使用其 {@code compareTo}（或 {@code compare}）方法执行所有键比较，因此从排序映射的角度来看，此方法认为相等的两个键是相等的。
 *    排序映射的行为是明确定义的，即使它的排序与 {@code equals} 不一致； 它只是不遵守 {@code Map} 接口的一般约定。
 * D. 请注意，此实现不是同步的。 如果多个线程同时访问一个映射，并且至少有一个线程在结构上修改了映射，则必须在外部进行同步。
 *   （结构修改是添加或删除一个或多个映射的任何操作；仅更改与现有键关联的值不是结构修改。）这通常是通过同步一些自然封装映射的对象来完成的。
 * E. 如果不存在此类对象，则应使用 {@link Collections#synchronizedSortedMap Collections.synchronizedSortedMap} 方法“包装”Map。 这最好在创建时完成，
 *    以防止对Map的意外不同步访问：
 *     SortedMap m = Collections.synchronizedSortedMap(new TreeMap(...));
 * F. 由该类的所有“集合视图方法”返回的集合的 {@code iterator} 方法返回的迭代器是快速失败的：如果在创建迭代器后的任何时间对地图进行结构修改，除了通过迭代器自己的
 *    {@code remove} 方法，迭代器将抛出一个 {@link ConcurrentModificationException}。 因此，面对并发修改，迭代器快速而干净地失败，而不是在未来不确定的时间
 *    冒着任意、非确定性行为的风险。
 * G. 请注意，无法保证迭代器的快速失败行为，因为一般而言，在存在非同步并发修改的情况下不可能做出任何硬保证。 快速失败的迭代器会在尽力而为的基础上抛出
 *    {@code ConcurrentModificationException}。因此，编写一个依赖此异常来确保其正确性的程序是错误的：迭代器的快速失败行为应该仅用于检测错误.
 * H. NavigableMap接口的方法及其视图返回的所有 {@code Map.Entry} 对都表示映射生成时的快照。 它们不支持 {@code Entry.setValue} 方法。
 *    （但请注意，可以使用 {@code put} 更改关联Map中的映射。）
 * I. {@docRoot}/../technotes/guides/collections/index.html
 */

/**
 * A.
 * A Red-Black tree based {@link NavigableMap} implementation.
 * The map is sorted according to the {@linkplain Comparable natural
 * ordering} of its keys, or by a {@link Comparator} provided at map
 * creation time, depending on which constructor is used.
 *
 * B.
 * <p>This implementation provides guaranteed log(n) time cost for the
 * {@code containsKey}, {@code get}, {@code put} and {@code remove}
 * operations.  Algorithms are adaptations of those in Cormen, Leiserson, and
 * Rivest's <em>Introduction to Algorithms</em>.
 *
 * C.
 * <p>Note that the ordering maintained by a tree map, like any sorted map, and
 * whether or not an explicit comparator is provided, must be <em>consistent
 * with {@code equals}</em> if this sorted map is to correctly implement the
 * {@code Map} interface.  (See {@code Comparable} or {@code Comparator} for a
 * precise definition of <em>consistent with equals</em>.)  This is so because
 * the {@code Map} interface is defined in terms of the {@code equals}
 * operation, but a sorted map performs all key comparisons using its {@code
 * compareTo} (or {@code compare}) method, so two keys that are deemed equal by
 * this method are, from the standpoint of the sorted map, equal.  The behavior
 * of a sorted map <em>is</em> well-defined even if its ordering is
 * inconsistent with {@code equals}; it just fails to obey the general contract
 * of the {@code Map} interface.
 *
 * D.
 * <p><strong>Note that this implementation is not synchronized.</strong>
 * If multiple threads access a map concurrently, and at least one of the
 * threads modifies the map structurally, it <em>must</em> be synchronized
 * externally.  (A structural modification is any operation that adds or
 * deletes one or more mappings; merely changing the value associated
 * with an existing key is not a structural modification.)  This is
 * typically accomplished by synchronizing on some object that naturally
 * encapsulates the map.
 *
 * E.
 * If no such object exists, the map should be "wrapped" using the
 * {@link Collections#synchronizedSortedMap Collections.synchronizedSortedMap}
 * method.  This is best done at creation time, to prevent accidental
 * unsynchronized access to the map: <pre>
 *   SortedMap m = Collections.synchronizedSortedMap(new TreeMap(...));</pre>
 *
 * F.
 * <p>The iterators returned by the {@code iterator} method of the collections
 * returned by all of this class's "collection view methods" are
 * <em>fail-fast</em>: if the map is structurally modified at any time after
 * the iterator is created, in any way except through the iterator's own
 * {@code remove} method, the iterator will throw a {@link
 * ConcurrentModificationException}.  Thus, in the face of concurrent
 * modification, the iterator fails quickly and cleanly, rather than risking
 * arbitrary, non-deterministic behavior at an undetermined time in the future.
 *
 * G.
 * <p>Note that the fail-fast behavior of an iterator cannot be guaranteed
 * as it is, generally speaking, impossible to make any hard guarantees in the
 * presence of unsynchronized concurrent modification.  Fail-fast iterators
 * throw {@code ConcurrentModificationException} on a best-effort basis.
 * Therefore, it would be wrong to write a program that depended on this
 * exception for its correctness:   <em>the fail-fast behavior of iterators
 * should be used only to detect bugs.</em>
 *
 * H.
 * <p>All {@code Map.Entry} pairs returned by methods in this class
 * and its views represent snapshots of mappings at the time they were
 * produced. They do <strong>not</strong> support the {@code Entry.setValue}
 * method. (Note however that it is possible to change mappings in the
 * associated map using {@code put}.)
 *
 * I.
 * <p>This class is a member of the
 * <a href="{@docRoot}/../technotes/guides/collections/index.html">
 * Java Collections Framework</a>.
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 *
 * @author  Josh Bloch and Doug Lea
 * @see Map
 * @see HashMap
 * @see Hashtable
 * @see Comparable
 * @see Comparator
 * @see Collection
 * @since 1.2
 */

public class TreeMap<K,V> extends AbstractMap<K,V> implements NavigableMap<K,V>, Cloneable, java.io.Serializable
{
    /**
     * 20210610
     * 用于维护此树图中顺序的比较器，如果使用其键的自然顺序，则为 null。
     */
    /**
     * The comparator used to maintain order in this tree map, or
     * null if it uses the natural ordering of its keys.
     *
     * @serial
     */
    private final Comparator<? super K> comparator;

    private transient Entry<K,V> root;

    /**
     * The number of entries in the tree
     */
    private transient int size = 0;// 树中的条目数

    /**
     * The number of structural modifications to the tree.
     */
    private transient int modCount = 0;// 对树的结构修改次数。

    /**
     * 20210610
     * 使用其键的自然顺序构造一个新的空树映射。 插入到Map中的所有键都必须实现 {@link Comparable} 接口。 此外，所有这些键必须相互比较：{@code k1.compareTo(k2)}
     * 不得为映射中的任何键 {@code k1} 和 {@code k2} 抛出 {@code ClassCastException}。 如果用户尝试将违反此约束的键放入映射中
     * （例如，用户尝试将字符串键放入键为整数的映射中），则 {@code put(Object key, Object value)} 调用 将抛出一个 {@code ClassCastException}。
     */
    /**
     * Constructs a new, empty tree map, using the natural ordering of its
     * keys.  All keys inserted into the map must implement the {@link
     * Comparable} interface.  Furthermore, all such keys must be
     * <em>mutually comparable</em>: {@code k1.compareTo(k2)} must not throw
     * a {@code ClassCastException} for any keys {@code k1} and
     * {@code k2} in the map.  If the user attempts to put a key into the
     * map that violates this constraint (for example, the user attempts to
     * put a string key into a map whose keys are integers), the
     * {@code put(Object key, Object value)} call will throw a
     * {@code ClassCastException}.
     */
    public TreeMap() {
        comparator = null;
    }

    /**
     * 20210610
     * 构造一个新的空树图，根据给定的顺序排序比较器。 插入到映射中的所有键必须由给定的比较器相互比较：{@code comparer.compare(k1, k2)}
     * 不得为任何键 {@code k1} 和 {@code k2} 抛出 {@code ClassCastException} 在Map中。 如果用户尝试将违反此约束的键放入Map，
     * 则 {@code put(Object key, Object value)} 调用将抛出 {@code ClassCastException}。
     */
    /**
     * Constructs a new, empty tree map, ordered according to the given
     * comparator.  All keys inserted into the map must be <em>mutually
     * comparable</em> by the given comparator: {@code comparator.compare(k1,
     * k2)} must not throw a {@code ClassCastException} for any keys
     * {@code k1} and {@code k2} in the map.  If the user attempts to put
     * a key into the map that violates this constraint, the {@code put(Object
     * key, Object value)} call will throw a
     * {@code ClassCastException}.
     *
     * @param comparator the comparator that will be used to order this map.
     *        If {@code null}, the {@linkplain Comparable natural
     *        ordering} of the keys will be used.
     */
    public TreeMap(Comparator<? super K> comparator) {
        this.comparator = comparator;
    }

    /**
     * 20210610
     * 构造一个包含与给定映射相同的映射的新树映射，根据其键的自然顺序进行排序。 插入新地图的所有键都必须实现 {@link Comparable} 接口。
     * 此外，所有这些键必须相互比较：{@code k1.compareTo(k2)} 不得为映射中的任何键 {@code k1} 和 {@code k2} 抛出 {@code ClassCastException}。
     * 此方法在 n*log(n) 时间内运行。
     */
    /**
     * Constructs a new tree map containing the same mappings as the given
     * map, ordered according to the <em>natural ordering</em> of its keys.
     * All keys inserted into the new map must implement the {@link
     * Comparable} interface.  Furthermore, all such keys must be
     * <em>mutually comparable</em>: {@code k1.compareTo(k2)} must not throw
     * a {@code ClassCastException} for any keys {@code k1} and
     * {@code k2} in the map.  This method runs in n*log(n) time.
     *
     * @param  m the map whose mappings are to be placed in this map
     * @throws ClassCastException if the keys in m are not {@link Comparable},
     *         or are not mutually comparable
     * @throws NullPointerException if the specified map is null
     */
    public TreeMap(Map<? extends K, ? extends V> m) {
        comparator = null;
        putAll(m);
    }

    /**
     * 20210610
     * 构造一个包含相同映射并使用与指定排序映射相同的排序的新树映射。 此方法在线性时间O(n)内运行。
     */
    /**
     * Constructs a new tree map containing the same mappings and
     * using the same ordering as the specified sorted map.  This
     * method runs in linear time.
     *
     * @param  m the sorted map whose mappings are to be placed in this map,
     *         and whose comparator is to be used to sort this map
     * @throws NullPointerException if the specified map is null
     */
    // 根据传入的迭代器it或者流str, 复制元素构造红黑树(只有最底层的结点才为红结点), 更新所有元素为传入集合的元素, O(n)
    public TreeMap(SortedMap<K, ? extends V> m) {
        comparator = m.comparator();
        try {
            // 根据传入的迭代器it或者流str, 复制元素构造红黑树(只有最底层的结点才为红结点), 更新所有元素为传入集合的元素, 红黑树结点数目计算O(n)
            buildFromSorted(m.size(), m.entrySet().iterator(), null, null);
        } catch (java.io.IOException cannotHappen) {
        } catch (ClassNotFoundException cannotHappen) {
        }
    }


    // Query Operations

    /**
     * Returns the number of key-value mappings in this map.
     *
     * @return the number of key-value mappings in this map
     */
    public int size() {
        return size;
    }

    /**
     * Returns {@code true} if this map contains a mapping for the specified
     * key.
     *
     * @param key key whose presence in this map is to be tested
     * @return {@code true} if this map contains a mapping for the
     *         specified key
     * @throws ClassCastException if the specified key cannot be compared
     *         with the keys currently in the map
     * @throws NullPointerException if the specified key is null
     *         and this map uses natural ordering, or its comparator
     *         does not permit null keys
     */
    public boolean containsKey(Object key) {
        return getEntry(key) != null;
    }

    /**
     * Returns {@code true} if this map maps one or more keys to the
     * specified value.  More formally, returns {@code true} if and only if
     * this map contains at least one mapping to a value {@code v} such
     * that {@code (value==null ? v==null : value.equals(v))}.  This
     * operation will probably require time linear in the map size for
     * most implementations.
     *
     * @param value value whose presence in this map is to be tested
     * @return {@code true} if a mapping to {@code value} exists;
     *         {@code false} otherwise
     * @since 1.2
     */
    public boolean containsValue(Object value) {
        for (Entry<K,V> e = getFirstEntry(); e != null; e = successor(e))
            if (valEquals(value, e.value))
                return true;
        return false;
    }

    /**
     * 20210613
     * A. 返回指定键映射到的值，如果此映射不包含键的映射，则返回 {@code null}。
     * B. 更正式地说，如果此映射包含从键 {@code k} 到值 {@code v} 的映射，使得 {@code key} 根据映射的排序等于 {@code k}，则此方法返回 {@code v};
     *    否则返回 {@code null}。 （最多可以有一个这样的映射。）
     * C. {@code null} 的返回值不一定表示映射不包含键的映射； Map也有可能将键显式映射到 {@code null}。 {@link #containsKey containsKey} 操作可用于区分这两种情况。
     */
    /**
     * A.
     * Returns the value to which the specified key is mapped,
     * or {@code null} if this map contains no mapping for the key.
     *
     * B.
     * <p>More formally, if this map contains a mapping from a key
     * {@code k} to a value {@code v} such that {@code key} compares
     * equal to {@code k} according to the map's ordering, then this
     * method returns {@code v}; otherwise it returns {@code null}.
     * (There can be at most one such mapping.)
     *
     * C.
     * <p>A return value of {@code null} does not <em>necessarily</em>
     * indicate that the map contains no mapping for the key; it's also
     * possible that the map explicitly maps the key to {@code null}.
     * The {@link #containsKey containsKey} operation may be used to
     * distinguish these two cases.
     *
     * @throws ClassCastException if the specified key cannot be compared
     *         with the keys currently in the map
     * @throws NullPointerException if the specified key is null
     *         and this map uses natural ordering, or its comparator
     *         does not permit null keys
     */
    public V get(Object key) {
        Entry<K,V> p = getEntry(key);
        return (p==null ? null : p.value);
    }

    public Comparator<? super K> comparator() {
        return comparator;
    }

    /**
     * @throws NoSuchElementException {@inheritDoc}
     */
    public K firstKey() {
        return key(getFirstEntry());
    }

    /**
     * @throws NoSuchElementException {@inheritDoc}
     */
    public K lastKey() {
        return key(getLastEntry());
    }

    /**
     * 20210612
     * 将所有映射从指定映射复制到此映射。 这些映射替换了此映射对当前在指定映射中的任何键所具有的任何映射。
     */
    /**
     * Copies all of the mappings from the specified map to this map.
     * These mappings replace any mappings that this map had for any
     * of the keys currently in the specified map.
     *
     * @param  map mappings to be stored in this map
     * @throws ClassCastException if the class of a key or value in
     *         the specified map prevents it from being stored in this map
     * @throws NullPointerException if the specified map is null or
     *         the specified map contains a null key and this map does not
     *         permit null keys
     */
    // 如果复制集合为SortedMap类型, 且比较器与当前的比较器不等, 则构建新树, O(n); 如果复制集合不为SortedMap类型, 或者比较器与当前的比较器相等, 则增量添加元素, O(n * logn)
    public void putAll(Map<? extends K, ? extends V> map) {
        int mapSize = map.size();

        // 如果复制集合为SortedMap类型, 且比较器与当前的比较器不等, 则构建新树, O(n)
        if (size==0 && mapSize!=0 && map instanceof SortedMap) {
            Comparator<?> c = ((SortedMap<?,?>)map).comparator();
            if (c == comparator || (c != null && c.equals(comparator))) {
                ++modCount;
                try {
                    // 根据传入的迭代器it或者流str, 复制元素构造红黑树(只有最底层的结点才为红结点), 更新所有元素为传入集合的元素, 红黑树结点数目计算O(n)
                    buildFromSorted(mapSize, map.entrySet().iterator(),
                                    null, null);
                } catch (java.io.IOException cannotHappen) {
                } catch (ClassNotFoundException cannotHappen) {
                }
                return;
            }
        }

        // 如果复制集合不为SortedMap类型, 或者比较器与当前的比较器相等, 则增量添加元素, 复制集合遍历 + 二叉搜索树添加: O(n * logn)
        super.putAll(map);
    }

    /**
     * 20210612
     * 返回此映射的给定键的条目，如果映射不包含键的条目，则返回 {@code null}。
     */
    /**
     * Returns this map's entry for the given key, or {@code null} if the map
     * does not contain an entry for the key.
     *
     * @return this map's entry for the given key, or {@code null} if the map
     *         does not contain an entry for the key
     * @throws ClassCastException if the specified key cannot be compared
     *         with the keys currently in the map
     * @throws NullPointerException if the specified key is null
     *         and this map uses natural ordering, or its comparator
     *         does not permit null keys
     */
    final Entry<K,V> getEntry(Object key) {
        // Offload comparator-based version for sake of performance // 为了性能而卸载基于比较器的版本
        if (comparator != null)
            // 使用比较器根据key值对进行二叉搜索查找, O(logn)
            return getEntryUsingComparator(key);

        if (key == null)
            throw new NullPointerException();

        // key作为Comparable接口实现, 根据key值对进行二叉搜索查找, O(logn)
        @SuppressWarnings("unchecked")
            Comparable<? super K> k = (Comparable<? super K>) key;
        Entry<K,V> p = root;
        while (p != null) {
            int cmp = k.compareTo(p.key);
            if (cmp < 0)
                p = p.left;
            else if (cmp > 0)
                p = p.right;
            else
                return p;
        }
        return null;
    }

    /**
     * 20210613
     * 使用比较器的 getEntry 版本。 从 getEntry 分离以获得性能。 （这对于大多数方法来说是不值得的，它们对比较器性能的依赖性较小，但在这里是值得的。）
     */
    /**
     * Version of getEntry using comparator. Split off from getEntry
     * for performance. (This is not worth doing for most methods,
     * that are less dependent on comparator performance, but is
     * worthwhile here.)
     */
    // 使用比较器根据key值对进行二叉搜索查找, O(logn)
    final Entry<K,V> getEntryUsingComparator(Object key) {
        @SuppressWarnings("unchecked")
            K k = (K) key;
        Comparator<? super K> cpr = comparator;
        if (cpr != null) {
            Entry<K,V> p = root;
            while (p != null) {
                int cmp = cpr.compare(k, p.key);
                if (cmp < 0)
                    p = p.left;
                else if (cmp > 0)
                    p = p.right;
                else
                    return p;
            }
        }
        return null;
    }

    /**
     * 20210613
     * 获取指定键对应的条目； 如果不存在这样的条目，则返回大于指定键的最小键的条目； 如果不存在这样的条目（即树中最大的键小于指定的键），则返回 {@code null}。
     */
    /**
     * Gets the entry corresponding to the specified key; if no such entry
     * exists, returns the entry for the least key greater than the specified
     * key; if no such entry exists (i.e., the greatest key in the Tree is less
     * than the specified key), returns {@code null}.
     */
    final Entry<K,V> getCeilingEntry(K key) {
        // 指定键值key, 当前键值p, 比较结果cmp(为key - p的结果, 大于0说明key比p大, 小于0说明key比p小)
        Entry<K,V> p = root;
        while (p != null) {
            // 如果没有指定比较器, 则使用键(已实现Comparable)来比较, 否则使用比较器比较
            int cmp = compare(key, p.key);

            // 如果key < p, 则查找左子树, 如果达到左子树的叶子结点还没找到, 则返回最小的p
            if (cmp < 0) {
                if (p.left != null)
                    p = p.left;
                else
                    return p;
            }
            // 如果key > p, 且p存在右孩子, 则继续查找右子树, 如果不存在右孩子, 说明碰到比key小的p了, 需要向上找p的父结点
            else if (cmp > 0) {
                // 如果p存在右孩子, 说明存在比p大一点的键, 而由于key > p, 所以还需要找到再大一点的p, 则遍历右子树
                if (p.right != null) {
                    p = p.right;
                }
                // 如果p不存在右孩子, 说明没有比p大一点的键了, 而由于key > p, 所以还需要找到再大一点的p, 则返回p的后继(一定比key大)
                else {
                    Entry<K,V> parent = p.parent;
                    Entry<K,V> ch = p;
                    while (parent != null && ch == parent.right) {
                        ch = parent;
                        parent = parent.parent;
                    }
                    return parent;
                }
            }
            // 如果key = p, 返回p
            else
                return p;
        }

        // 找不到则返回null
        return null;
    }

    /**
     * 20210613
     * 获取指定键对应的条目； 如果不存在这样的条目，则返回小于指定键的最大键的条目； 如果不存在这样的条目，则返回 {@code null}。
     */
    /**
     * Gets the entry corresponding to the specified key; if no such entry
     * exists, returns the entry for the greatest key less than the specified
     * key; if no such entry exists, returns {@code null}.
     */
    final Entry<K,V> getFloorEntry(K key) {
        // 指定键值key, 当前键值p, 比较结果cmp(为key - p的结果, 大于0说明key比p大, 小于0说明key比p小)
        Entry<K,V> p = root;
        while (p != null) {
            // 如果没有指定比较器, 则使用键(已实现Comparable)来比较, 否则使用比较器比较
            int cmp = compare(key, p.key);

            // 如果key > p, 则查找右子树, 如果达到右子树的叶子结点还没找到, 则返回最大的p
            if (cmp > 0) {
                if (p.right != null)
                    p = p.right;
                else
                    return p;
            }
            // 如果key < p, 且p存在左孩子, 则继续查找左子树, 如果不存在左孩子, 说明碰到比key大的p了, 需要向上找p的父结点
            else if (cmp < 0) {
                // 如果p存在左孩子, 说明存在比p小一点的键, 而由于key < p, 所以还需要找到再小一点的p, 则遍历左子树
                if (p.left != null) {
                    p = p.left;
                }
                // 如果p不存在左孩子, 说明没有比p小一点的键了, 而由于key < p, 所以还需要找到再小一点的p, 则返回p的前驱(一定比key小)
                else {
                    Entry<K,V> parent = p.parent;
                    Entry<K,V> ch = p;
                    while (parent != null && ch == parent.left) {
                        ch = parent;
                        parent = parent.parent;
                    }
                    return parent;
                }
            }
            // 如果key = p, 返回p
            else
                return p;

        }

        // 找不到则返回null
        return null;
    }

    /**
     * 20210613
     * 获取大于指定键的最小键的条目； 如果不存在这样的条目，则返回大于指定键的最小键的条目； 如果不存在这样的条目，则返回 {@code null}。
     */
    /**
     * Gets the entry for the least key greater than the specified
     * key; if no such entry exists, returns the entry for the least
     * key greater than the specified key; if no such entry exists
     * returns {@code null}.
     */
    final Entry<K,V> getHigherEntry(K key) {
        // 指定键值key, 当前键值p, 比较结果cmp(为key - p的结果, 大于0说明key比p大, 小于0说明key比p小)
        Entry<K,V> p = root;
        while (p != null) {
            // 如果没有指定比较器, 则使用键(已实现Comparable)来比较, 否则使用比较器比较
            int cmp = compare(key, p.key);

            // 如果key < p, 则查找左子树, 如果达到左子树的叶子结点还没找到, 则返回最小的p
            if (cmp < 0) {
                if (p.left != null)
                    p = p.left;
                else
                    return p;
            }
            // 如果key >= p, 且p存在右孩子, 则继续查找右子树, 如果不存在右孩子, 说明碰到比key小的p了, 需要向上找p的父结点
            else {
                // 如果p存在右孩子, 说明存在比p大一点的键, 而由于key > p, 所以还需要找到再大一点的p, 则遍历右子树
                if (p.right != null) {
                    p = p.right;
                }
                // 如果p不存在右孩子, 说明没有比p大一点的键了, 而由于key > p, 所以还需要找到再大一点的p, 则返回p的后继(一定比key大)
                else {
                    Entry<K,V> parent = p.parent;
                    Entry<K,V> ch = p;
                    while (parent != null && ch == parent.right) {
                        ch = parent;
                        parent = parent.parent;
                    }
                    return parent;
                }
            }
        }

        // 找不到则返回null
        return null;
    }

    /**
     * 20210613
     * 返回小于指定键的最大键的条目； 如果不存在这样的条目（即树中的最小键大于指定的键），则返回 {@code null}。
     */
    /**
     * Returns the entry for the greatest key less than the specified key; if
     * no such entry exists (i.e., the least key in the Tree is greater than
     * the specified key), returns {@code null}.
     */
    final Entry<K,V> getLowerEntry(K key) {
        // 指定键值key, 当前键值p, 比较结果cmp(为key - p的结果, 大于0说明key比p大, 小于0说明key比p小)
        Entry<K,V> p = root;
        while (p != null) {
            // 如果没有指定比较器, 则使用键(已实现Comparable)来比较, 否则使用比较器比较
            int cmp = compare(key, p.key);

            // 如果key > p, 则查找右子树, 如果达到右子树的叶子结点还没找到, 则返回最大的p
            if (cmp > 0) {
                if (p.right != null)
                    p = p.right;
                else
                    return p;
            }
            // 如果key <= p, 且p存在左孩子, 则继续查找左子树, 如果不存在左孩子, 说明碰到比key大的p了, 需要向上找p的父结点
            else {
                // 如果p存在左孩子, 说明存在比p小一点的键, 而由于key < p, 所以还需要找到再小一点的p, 则遍历左子树
                if (p.left != null) {
                    p = p.left;
                }
                // 如果p不存在左孩子, 说明没有比p小一点的键了, 而由于key < p, 所以还需要找到再小一点的p, 则返回p的前驱(一定比key小)
                else {
                    Entry<K,V> parent = p.parent;
                    Entry<K,V> ch = p;
                    while (parent != null && ch == parent.left) {
                        ch = parent;
                        parent = parent.parent;
                    }
                    return parent;
                }
            }
        }

        // 找不到则返回null
        return null;
    }

    /**
     * 20210610
     * 将指定值与此映射中的指定键相关联。 如果映射先前包含键的映射，则旧值将被替换。
     */
    /**
     * Associates the specified value with the specified key in this map.
     * If the map previously contained a mapping for the key, the old
     * value is replaced.
     *
     * @param key key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     *
     * @return the previous value associated with {@code key}, or
     *         {@code null} if there was no mapping for {@code key}.
     *         (A {@code null} return can also indicate that the map
     *         previously associated {@code null} with {@code key}.)
     * @throws ClassCastException if the specified key cannot be compared
     *         with the keys currently in the map
     * @throws NullPointerException if the specified key is null
     *         and this map uses natural ordering, or its comparator
     *         does not permit null keys
     */
    public V put(K key, V value) {
        Entry<K,V> t = root;

        // 如果root结点为null, 则构建root结点, 更新实际大小, 插入成功返回null
        if (t == null) {
            // 如果没有指定比较器, 则使用键(已实现Comparable)来比较, 否则使用比较器比较
            compare(key, key); // type (and possibly null) check // comparator类型（可能为空）检查

            root = new Entry<>(key, value, null);
            size = 1;
            modCount++;
            return null;
        }

        // 比较结果cmp, 父结点parent, 比较器cpr, 临时结点t
        int cmp;
        Entry<K,V> parent;
        // split comparator and comparable paths  // 拆分比较器和可比较路径
        Comparator<? super K> cpr = comparator;

        // 如果指定了比较器cpr, 则使用cpr比较, cmp小于0则遍历左子树并更新parent指针, cmp大于0则遍历右子树并更新parent指针, cmp等于0则替换结点的值并返回
        if (cpr != null) {
            do {
                parent = t;
                cmp = cpr.compare(key, t.key);
                if (cmp < 0)
                    t = t.left;
                else if (cmp > 0)
                    t = t.right;
                else
                    return t.setValue(value);
            } while (t != null);
        }
        // 如果没有指定比较器cpr, 则使用键(已实现Comparable)来比较比较, cmp小于0则遍历左子树并更新parent指针, cmp大于0则遍历右子树并更新parent指针, cmp等于0则替换结点的值并返回
        else {
            if (key == null)
                throw new NullPointerException();
            @SuppressWarnings("unchecked")
                Comparable<? super K> k = (Comparable<? super K>) key;
            do {
                parent = t;
                cmp = k.compareTo(t.key);
                if (cmp < 0)
                    t = t.left;
                else if (cmp > 0)
                    t = t.right;
                else
                    return t.setValue(value);
            } while (t != null);
        }

        // 构建新结点, cmp小于0则设置/替换parent的左孩子, 否则说明大于0, 则设置/替换parent的右孩子
        Entry<K,V> e = new Entry<>(key, value, parent);
        if (cmp < 0)
            parent.left = e;
        else
            parent.right = e;

        // 插入结点后平衡红黑树, 更新实际大小, 插入成功返回null
        fixAfterInsertion(e);
        size++;
        modCount++;
        return null;
    }

    /**
     * 20210612
     * 如果存在，则从此 TreeMap 中删除此键的映射。
     */
    /**
     * Removes the mapping for this key from this TreeMap if present.
     *
     * @param  key key for which mapping should be removed
     * @return the previous value associated with {@code key}, or
     *         {@code null} if there was no mapping for {@code key}.
     *         (A {@code null} return can also indicate that the map
     *         previously associated {@code null} with {@code key}.)
     * @throws ClassCastException if the specified key cannot be compared
     *         with the keys currently in the map
     * @throws NullPointerException if the specified key is null
     *         and this map uses natural ordering, or its comparator
     *         does not permit null keys
     */
    public V remove(Object key) {
        Entry<K,V> p = getEntry(key);
        if (p == null)
            return null;

        V oldValue = p.value;
        deleteEntry(p);
        return oldValue;
    }

    /**
     * Removes all of the mappings from this map.
     * The map will be empty after this call returns.
     */
    public void clear() {
        modCount++;
        size = 0;
        root = null;
    }

    /**
     * Returns a shallow copy of this {@code TreeMap} instance. (The keys and
     * values themselves are not cloned.)
     *
     * @return a shallow copy of this map
     */
    public Object clone() {
        TreeMap<?,?> clone;
        try {
            clone = (TreeMap<?,?>) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }

        // Put clone into "virgin" state (except for comparator)
        clone.root = null;
        clone.size = 0;
        clone.modCount = 0;
        clone.entrySet = null;
        clone.navigableKeySet = null;
        clone.descendingMap = null;

        // Initialize clone with our mappings
        try {
            clone.buildFromSorted(size, entrySet().iterator(), null, null);
        } catch (java.io.IOException cannotHappen) {
        } catch (ClassNotFoundException cannotHappen) {
        }

        return clone;
    }

    // NavigableMap API methods

    /**
     * @since 1.6
     */
    public Map.Entry<K,V> firstEntry() {
        return exportEntry(getFirstEntry());
    }

    /**
     * @since 1.6
     */
    public Map.Entry<K,V> lastEntry() {
        return exportEntry(getLastEntry());
    }

    /**
     * @since 1.6
     */
    public Map.Entry<K,V> pollFirstEntry() {
        Entry<K,V> p = getFirstEntry();
        Map.Entry<K,V> result = exportEntry(p);
        if (p != null)
            deleteEntry(p);
        return result;
    }

    /**
     * @since 1.6
     */
    public Map.Entry<K,V> pollLastEntry() {
        Entry<K,V> p = getLastEntry();
        Map.Entry<K,V> result = exportEntry(p);
        if (p != null)
            deleteEntry(p);
        return result;
    }

    /**
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException if the specified key is null
     *         and this map uses natural ordering, or its comparator
     *         does not permit null keys
     * @since 1.6
     */
    public Map.Entry<K,V> lowerEntry(K key) {
        return exportEntry(getLowerEntry(key));
    }

    /**
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException if the specified key is null
     *         and this map uses natural ordering, or its comparator
     *         does not permit null keys
     * @since 1.6
     */
    public K lowerKey(K key) {
        return keyOrNull(getLowerEntry(key));
    }

    /**
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException if the specified key is null
     *         and this map uses natural ordering, or its comparator
     *         does not permit null keys
     * @since 1.6
     */
    public Map.Entry<K,V> floorEntry(K key) {
        return exportEntry(getFloorEntry(key));
    }

    /**
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException if the specified key is null
     *         and this map uses natural ordering, or its comparator
     *         does not permit null keys
     * @since 1.6
     */
    public K floorKey(K key) {
        return keyOrNull(getFloorEntry(key));
    }

    /**
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException if the specified key is null
     *         and this map uses natural ordering, or its comparator
     *         does not permit null keys
     * @since 1.6
     */
    public Map.Entry<K,V> ceilingEntry(K key) {
        return exportEntry(getCeilingEntry(key));
    }

    /**
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException if the specified key is null
     *         and this map uses natural ordering, or its comparator
     *         does not permit null keys
     * @since 1.6
     */
    public K ceilingKey(K key) {
        return keyOrNull(getCeilingEntry(key));
    }

    /**
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException if the specified key is null
     *         and this map uses natural ordering, or its comparator
     *         does not permit null keys
     * @since 1.6
     */
    public Map.Entry<K,V> higherEntry(K key) {
        return exportEntry(getHigherEntry(key));
    }

    /**
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException if the specified key is null
     *         and this map uses natural ordering, or its comparator
     *         does not permit null keys
     * @since 1.6
     */
    public K higherKey(K key) {
        return keyOrNull(getHigherEntry(key));
    }

    // Views

    /**
     * Fields initialized to contain an instance of the entry set view
     * the first time this view is requested.  Views are stateless, so
     * there's no reason to create more than one.
     */
    private transient EntrySet entrySet;
    private transient KeySet<K> navigableKeySet;
    private transient NavigableMap<K,V> descendingMap;

    /**
     * Returns a {@link Set} view of the keys contained in this map.
     *
     * <p>The set's iterator returns the keys in ascending order.
     * The set's spliterator is
     * <em><a href="Spliterator.html#binding">late-binding</a></em>,
     * <em>fail-fast</em>, and additionally reports {@link Spliterator#SORTED}
     * and {@link Spliterator#ORDERED} with an encounter order that is ascending
     * key order.  The spliterator's comparator (see
     * {@link java.util.Spliterator#getComparator()}) is {@code null} if
     * the tree map's comparator (see {@link #comparator()}) is {@code null}.
     * Otherwise, the spliterator's comparator is the same as or imposes the
     * same total ordering as the tree map's comparator.
     *
     * <p>The set is backed by the map, so changes to the map are
     * reflected in the set, and vice-versa.  If the map is modified
     * while an iteration over the set is in progress (except through
     * the iterator's own {@code remove} operation), the results of
     * the iteration are undefined.  The set supports element removal,
     * which removes the corresponding mapping from the map, via the
     * {@code Iterator.remove}, {@code Set.remove},
     * {@code removeAll}, {@code retainAll}, and {@code clear}
     * operations.  It does not support the {@code add} or {@code addAll}
     * operations.
     */
    public Set<K> keySet() {
        return navigableKeySet();
    }

    /**
     * @since 1.6
     */
    public NavigableSet<K> navigableKeySet() {
        KeySet<K> nks = navigableKeySet;
        return (nks != null) ? nks : (navigableKeySet = new KeySet<>(this));
    }

    /**
     * @since 1.6
     */
    public NavigableSet<K> descendingKeySet() {
        return descendingMap().navigableKeySet();
    }

    /**
     * Returns a {@link Collection} view of the values contained in this map.
     *
     * <p>The collection's iterator returns the values in ascending order
     * of the corresponding keys. The collection's spliterator is
     * <em><a href="Spliterator.html#binding">late-binding</a></em>,
     * <em>fail-fast</em>, and additionally reports {@link Spliterator#ORDERED}
     * with an encounter order that is ascending order of the corresponding
     * keys.
     *
     * <p>The collection is backed by the map, so changes to the map are
     * reflected in the collection, and vice-versa.  If the map is
     * modified while an iteration over the collection is in progress
     * (except through the iterator's own {@code remove} operation),
     * the results of the iteration are undefined.  The collection
     * supports element removal, which removes the corresponding
     * mapping from the map, via the {@code Iterator.remove},
     * {@code Collection.remove}, {@code removeAll},
     * {@code retainAll} and {@code clear} operations.  It does not
     * support the {@code add} or {@code addAll} operations.
     */
    public Collection<V> values() {
        Collection<V> vs = values;
        return (vs != null) ? vs : (values = new Values());
    }

    /**
     * Returns a {@link Set} view of the mappings contained in this map.
     *
     * <p>The set's iterator returns the entries in ascending key order. The
     * sets's spliterator is
     * <em><a href="Spliterator.html#binding">late-binding</a></em>,
     * <em>fail-fast</em>, and additionally reports {@link Spliterator#SORTED} and
     * {@link Spliterator#ORDERED} with an encounter order that is ascending key
     * order.
     *
     * <p>The set is backed by the map, so changes to the map are
     * reflected in the set, and vice-versa.  If the map is modified
     * while an iteration over the set is in progress (except through
     * the iterator's own {@code remove} operation, or through the
     * {@code setValue} operation on a map entry returned by the
     * iterator) the results of the iteration are undefined.  The set
     * supports element removal, which removes the corresponding
     * mapping from the map, via the {@code Iterator.remove},
     * {@code Set.remove}, {@code removeAll}, {@code retainAll} and
     * {@code clear} operations.  It does not support the
     * {@code add} or {@code addAll} operations.
     */
    public Set<Map.Entry<K,V>> entrySet() {
        EntrySet es = entrySet;
        return (es != null) ? es : (entrySet = new EntrySet());
    }

    /**
     * @since 1.6
     */
    public NavigableMap<K, V> descendingMap() {
        NavigableMap<K, V> km = descendingMap;
        return (km != null) ? km :
            (descendingMap = new DescendingSubMap<>(this,
                                                    true, null, true,
                                                    true, null, true));
    }

    /**
     * @throws ClassCastException       {@inheritDoc}
     * @throws NullPointerException if {@code fromKey} or {@code toKey} is
     *         null and this map uses natural ordering, or its comparator
     *         does not permit null keys
     * @throws IllegalArgumentException {@inheritDoc}
     * @since 1.6
     */
    public NavigableMap<K,V> subMap(K fromKey, boolean fromInclusive,
                                    K toKey,   boolean toInclusive) {
        return new AscendingSubMap<>(this,
                                     false, fromKey, fromInclusive,
                                     false, toKey,   toInclusive);
    }

    /**
     * @throws ClassCastException       {@inheritDoc}
     * @throws NullPointerException if {@code toKey} is null
     *         and this map uses natural ordering, or its comparator
     *         does not permit null keys
     * @throws IllegalArgumentException {@inheritDoc}
     * @since 1.6
     */
    public NavigableMap<K,V> headMap(K toKey, boolean inclusive) {
        return new AscendingSubMap<>(this,
                                     true,  null,  true,
                                     false, toKey, inclusive);
    }

    /**
     * @throws ClassCastException       {@inheritDoc}
     * @throws NullPointerException if {@code fromKey} is null
     *         and this map uses natural ordering, or its comparator
     *         does not permit null keys
     * @throws IllegalArgumentException {@inheritDoc}
     * @since 1.6
     */
    public NavigableMap<K,V> tailMap(K fromKey, boolean inclusive) {
        return new AscendingSubMap<>(this,
                                     false, fromKey, inclusive,
                                     true,  null,    true);
    }

    /**
     * @throws ClassCastException       {@inheritDoc}
     * @throws NullPointerException if {@code fromKey} or {@code toKey} is
     *         null and this map uses natural ordering, or its comparator
     *         does not permit null keys
     * @throws IllegalArgumentException {@inheritDoc}
     */
    public SortedMap<K,V> subMap(K fromKey, K toKey) {
        return subMap(fromKey, true, toKey, false);
    }

    /**
     * @throws ClassCastException       {@inheritDoc}
     * @throws NullPointerException if {@code toKey} is null
     *         and this map uses natural ordering, or its comparator
     *         does not permit null keys
     * @throws IllegalArgumentException {@inheritDoc}
     */
    public SortedMap<K,V> headMap(K toKey) {
        return headMap(toKey, false);
    }

    /**
     * @throws ClassCastException       {@inheritDoc}
     * @throws NullPointerException if {@code fromKey} is null
     *         and this map uses natural ordering, or its comparator
     *         does not permit null keys
     * @throws IllegalArgumentException {@inheritDoc}
     */
    public SortedMap<K,V> tailMap(K fromKey) {
        return tailMap(fromKey, true);
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        Entry<K,V> p = getEntry(key);
        if (p!=null && Objects.equals(oldValue, p.value)) {
            p.value = newValue;
            return true;
        }
        return false;
    }

    @Override
    public V replace(K key, V value) {
        Entry<K,V> p = getEntry(key);
        if (p!=null) {
            V oldValue = p.value;
            p.value = value;
            return oldValue;
        }
        return null;
    }

    @Override
    public void forEach(BiConsumer<? super K, ? super V> action) {
        Objects.requireNonNull(action);
        int expectedModCount = modCount;
        for (Entry<K, V> e = getFirstEntry(); e != null; e = successor(e)) {
            action.accept(e.key, e.value);

            if (expectedModCount != modCount) {
                throw new ConcurrentModificationException();
            }
        }
    }

    @Override
    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        Objects.requireNonNull(function);
        int expectedModCount = modCount;

        for (Entry<K, V> e = getFirstEntry(); e != null; e = successor(e)) {
            e.value = function.apply(e.key, e.value);

            if (expectedModCount != modCount) {
                throw new ConcurrentModificationException();
            }
        }
    }

    // View class support

    class Values extends AbstractCollection<V> {
        public Iterator<V> iterator() {
            return new ValueIterator(getFirstEntry());
        }

        public int size() {
            return TreeMap.this.size();
        }

        public boolean contains(Object o) {
            return TreeMap.this.containsValue(o);
        }

        public boolean remove(Object o) {
            for (Entry<K,V> e = getFirstEntry(); e != null; e = successor(e)) {
                if (valEquals(e.getValue(), o)) {
                    deleteEntry(e);
                    return true;
                }
            }
            return false;
        }

        public void clear() {
            TreeMap.this.clear();
        }

        public Spliterator<V> spliterator() {
            return new ValueSpliterator<K,V>(TreeMap.this, null, null, 0, -1, 0);
        }
    }

    class EntrySet extends AbstractSet<Map.Entry<K,V>> {
        public Iterator<Map.Entry<K,V>> iterator() {
            return new EntryIterator(getFirstEntry());
        }

        public boolean contains(Object o) {
            if (!(o instanceof Map.Entry))
                return false;
            Map.Entry<?,?> entry = (Map.Entry<?,?>) o;
            Object value = entry.getValue();
            Entry<K,V> p = getEntry(entry.getKey());
            return p != null && valEquals(p.getValue(), value);
        }

        public boolean remove(Object o) {
            if (!(o instanceof Map.Entry))
                return false;
            Map.Entry<?,?> entry = (Map.Entry<?,?>) o;
            Object value = entry.getValue();
            Entry<K,V> p = getEntry(entry.getKey());
            if (p != null && valEquals(p.getValue(), value)) {
                deleteEntry(p);
                return true;
            }
            return false;
        }

        public int size() {
            return TreeMap.this.size();
        }

        public void clear() {
            TreeMap.this.clear();
        }

        public Spliterator<Map.Entry<K,V>> spliterator() {
            return new EntrySpliterator<K,V>(TreeMap.this, null, null, 0, -1, 0);
        }
    }

    /*
     * Unlike Values and EntrySet, the KeySet class is static,
     * delegating to a NavigableMap to allow use by SubMaps, which
     * outweighs the ugliness of needing type-tests for the following
     * Iterator methods that are defined appropriately in main versus
     * submap classes.
     */

    Iterator<K> keyIterator() {
        return new KeyIterator(getFirstEntry());
    }

    Iterator<K> descendingKeyIterator() {
        return new DescendingKeyIterator(getLastEntry());
    }

    static final class KeySet<E> extends AbstractSet<E> implements NavigableSet<E> {
        private final NavigableMap<E, ?> m;
        KeySet(NavigableMap<E,?> map) { m = map; }

        public Iterator<E> iterator() {
            if (m instanceof TreeMap)
                return ((TreeMap<E,?>)m).keyIterator();
            else
                return ((TreeMap.NavigableSubMap<E,?>)m).keyIterator();
        }

        public Iterator<E> descendingIterator() {
            if (m instanceof TreeMap)
                return ((TreeMap<E,?>)m).descendingKeyIterator();
            else
                return ((TreeMap.NavigableSubMap<E,?>)m).descendingKeyIterator();
        }

        public int size() { return m.size(); }
        public boolean isEmpty() { return m.isEmpty(); }
        public boolean contains(Object o) { return m.containsKey(o); }
        public void clear() { m.clear(); }
        public E lower(E e) { return m.lowerKey(e); }
        public E floor(E e) { return m.floorKey(e); }
        public E ceiling(E e) { return m.ceilingKey(e); }
        public E higher(E e) { return m.higherKey(e); }
        public E first() { return m.firstKey(); }
        public E last() { return m.lastKey(); }
        public Comparator<? super E> comparator() { return m.comparator(); }
        public E pollFirst() {
            Map.Entry<E,?> e = m.pollFirstEntry();
            return (e == null) ? null : e.getKey();
        }
        public E pollLast() {
            Map.Entry<E,?> e = m.pollLastEntry();
            return (e == null) ? null : e.getKey();
        }
        public boolean remove(Object o) {
            int oldSize = size();
            m.remove(o);
            return size() != oldSize;
        }
        public NavigableSet<E> subSet(E fromElement, boolean fromInclusive,
                                      E toElement,   boolean toInclusive) {
            return new KeySet<>(m.subMap(fromElement, fromInclusive,
                                          toElement,   toInclusive));
        }
        public NavigableSet<E> headSet(E toElement, boolean inclusive) {
            return new KeySet<>(m.headMap(toElement, inclusive));
        }
        public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
            return new KeySet<>(m.tailMap(fromElement, inclusive));
        }
        public SortedSet<E> subSet(E fromElement, E toElement) {
            return subSet(fromElement, true, toElement, false);
        }
        public SortedSet<E> headSet(E toElement) {
            return headSet(toElement, false);
        }
        public SortedSet<E> tailSet(E fromElement) {
            return tailSet(fromElement, true);
        }
        public NavigableSet<E> descendingSet() {
            return new KeySet<>(m.descendingMap());
        }

        public Spliterator<E> spliterator() {
            return keySpliteratorFor(m);
        }
    }

    /**
     * 20210613
     * TreeMap 迭代器的基类
     */
    /**
     * Base class for TreeMap Iterators
     */
    abstract class PrivateEntryIterator<T> implements Iterator<T> {
        Entry<K,V> next;
        Entry<K,V> lastReturned;
        int expectedModCount;

        PrivateEntryIterator(Entry<K,V> first) {
            expectedModCount = modCount;
            lastReturned = null;
            next = first;
        }

        public final boolean hasNext() {
            return next != null;
        }

        final Entry<K,V> nextEntry() {
            Entry<K,V> e = next;
            if (e == null)
                throw new NoSuchElementException();
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
            next = successor(e);
            lastReturned = e;
            return e;
        }

        final Entry<K,V> prevEntry() {
            Entry<K,V> e = next;
            if (e == null)
                throw new NoSuchElementException();
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
            next = predecessor(e);
            lastReturned = e;
            return e;
        }

        public void remove() {
            if (lastReturned == null)
                throw new IllegalStateException();
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
            // deleted entries are replaced by their successors
            if (lastReturned.left != null && lastReturned.right != null)
                next = lastReturned;
            deleteEntry(lastReturned);
            expectedModCount = modCount;
            lastReturned = null;
        }
    }

    final class EntryIterator extends PrivateEntryIterator<Map.Entry<K,V>> {
        EntryIterator(Entry<K,V> first) {
            super(first);
        }
        public Map.Entry<K,V> next() {
            return nextEntry();
        }
    }

    final class ValueIterator extends PrivateEntryIterator<V> {
        ValueIterator(Entry<K,V> first) {
            super(first);
        }
        public V next() {
            return nextEntry().value;
        }
    }

    final class KeyIterator extends PrivateEntryIterator<K> {
        KeyIterator(Entry<K,V> first) {
            super(first);
        }
        public K next() {
            return nextEntry().key;
        }
    }

    final class DescendingKeyIterator extends PrivateEntryIterator<K> {
        DescendingKeyIterator(Entry<K,V> first) {
            super(first);
        }
        public K next() {
            return prevEntry().key;
        }
        public void remove() {
            if (lastReturned == null)
                throw new IllegalStateException();
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
            deleteEntry(lastReturned);
            lastReturned = null;
            expectedModCount = modCount;
        }
    }

    // Little utilities
    /**
     * 20210612
     * 使用此TreeMap的正确比较方法比较两个键。
     */
    /**
     * Compares two keys using the correct comparison method for this TreeMap.
     */
    // 如果没有指定比较器, 则使用键(已实现Comparable)来比较, 否则使用比较器比较
    @SuppressWarnings("unchecked")
    final int compare(Object k1, Object k2) {
        return comparator==null ? ((Comparable<? super K>)k1).compareTo((K)k2)
            : comparator.compare((K)k1, (K)k2);
    }

    /**
     * Test two values for equality.  Differs from o1.equals(o2) only in
     * that it copes with {@code null} o1 properly.
     */
    static final boolean valEquals(Object o1, Object o2) {
        return (o1==null ? o2==null : o1.equals(o2));
    }

    /**
     * Return SimpleImmutableEntry for entry, or null if null
     */
    // 返回 SimpleImmutableEntry 作为条目，如果为 null，则返回 null
    static <K,V> Map.Entry<K,V> exportEntry(TreeMap.Entry<K,V> e) {
        return (e == null) ? null :
            new AbstractMap.SimpleImmutableEntry<>(e);
    }

    /**
     * Return key for entry, or null if null
     */
    // 返回输入键，如果为空则为空
    static <K,V> K keyOrNull(TreeMap.Entry<K,V> e) {
        return (e == null) ? null : e.key;
    }

    /**
     * Returns the key corresponding to the specified Entry.
     * @throws NoSuchElementException if the Entry is null
     */
    static <K> K key(Entry<K,?> e) {
        if (e==null)
            throw new NoSuchElementException();
        return e.key;
    }


    // SubMaps

    /**
     * Dummy value serving as unmatchable fence key for unbounded
     * SubMapIterators
     */
    private static final Object UNBOUNDED = new Object();

    /**
     * @serial include
     */
    abstract static class NavigableSubMap<K,V> extends AbstractMap<K,V> implements NavigableMap<K,V>, java.io.Serializable {
        private static final long serialVersionUID = -2102997345730753016L;
        /**
         * The backing map.
         */
        final TreeMap<K,V> m;

        /**
         * 20210613
         * 端点表示为三元组 (fromStart, lo, loInclusive) 和 (toEnd, hi, hiInclusive)。 如果 fromStart 为真，则下限（绝对）是支持映射的开始，其他值将被忽略。
         * 否则，如果 loInclusive 为真，则 lo 是包含边界，否则 lo 是互斥边界。 上界也是如此。
         */
        /**
         * Endpoints are represented as triples (fromStart, lo,
         * loInclusive) and (toEnd, hi, hiInclusive). If fromStart is
         * true, then the low (absolute) bound is the start of the
         * backing map, and the other values are ignored. Otherwise,
         * if loInclusive is true, lo is the inclusive bound, else lo
         * is the exclusive bound. Similarly for the upper bound.
         */
        final K lo, hi;
        final boolean fromStart, toEnd;
        final boolean loInclusive, hiInclusive;

        NavigableSubMap(TreeMap<K,V> m,
                        boolean fromStart, K lo, boolean loInclusive,
                        boolean toEnd,     K hi, boolean hiInclusive) {
            if (!fromStart && !toEnd) {
                if (m.compare(lo, hi) > 0)
                    throw new IllegalArgumentException("fromKey > toKey");
            } else {
                if (!fromStart) // type check
                    m.compare(lo, lo);
                if (!toEnd)
                    m.compare(hi, hi);
            }

            this.m = m;
            this.fromStart = fromStart;
            this.lo = lo;
            this.loInclusive = loInclusive;
            this.toEnd = toEnd;
            this.hi = hi;
            this.hiInclusive = hiInclusive;
        }

        // internal utilities
        // 如果绝对从头开始, 则返回false, 否则如果超过lo(包含时还要判断loInclusive)范围, 则返回false
        final boolean tooLow(Object key) {
            if (!fromStart) {
                int c = m.compare(key, lo);
                if (c < 0 || (c == 0 && !loInclusive))
                    return true;
            }
            return false;
        }

        // 如果绝对从尾开始, 则返回false, 否则如果超过hi(包含时还要判断hiInclusive)范围, 则返回false
        final boolean tooHigh(Object key) {
            if (!toEnd) {
                int c = m.compare(key, hi);
                if (c > 0 || (c == 0 && !hiInclusive))
                    return true;
            }
            return false;
        }

        final boolean inRange(Object key) {
            return !tooLow(key) && !tooHigh(key);
        }

        final boolean inClosedRange(Object key) {
            return (fromStart || m.compare(key, lo) >= 0)
                && (toEnd || m.compare(hi, key) >= 0);
        }

        final boolean inRange(Object key, boolean inclusive) {
            return inclusive ? inRange(key) : inClosedRange(key);
        }

        /**
         * 20210613
         * 关系操作的绝对版本。 子类使用同名的“子”版本映射到这些，这些版本反转降序映射的意义
         */
        /*
         * Absolute versions of relation operations.
         * Subclasses map to these using like-named "sub"
         * versions that invert senses for descending maps
         */
        // 获取最小键 或者 大于等于lo的最小键Entry 或者 大于lo的最小键Entry
        final TreeMap.Entry<K,V> absLowest() {
            // 如果绝对从头开始, 则返回最小键Entry, 否则如果可以包含边界, 则返回大于等于lo的最小键Entry, 如果不可以包含边界, 则返回大于lo的最小键Entry
            TreeMap.Entry<K,V> e =
                (fromStart ?  m.getFirstEntry() :
                 (loInclusive ? m.getCeilingEntry(lo) :
                                m.getHigherEntry(lo)));

            // 如果e > hi, 则返回null, 否则正常返回
            return (e == null || tooHigh(e.key)) ? null : e;
        }

        // 获取最大键 或者 小于等于hi的最大键Entry 或者 小于hi的最大键Entry
        final TreeMap.Entry<K,V> absHighest() {
            // 如果绝对从尾开始, 则返回最大键Entry, 否则如果可以包含边界, 则返回小于等于hi的最大键Entry, 如果不可以包含边界, 则返回小于hi的最大键Entry
            TreeMap.Entry<K,V> e =
                (toEnd ?  m.getLastEntry() :
                 (hiInclusive ?  m.getFloorEntry(hi) :
                                 m.getLowerEntry(hi)));

            // 如果e < lo, 则返回null, 否则正常返回
            return (e == null || tooLow(e.key)) ? null : e;
        }

        // 获取最小键 或者 大于等于key的最小键Entry 或者 大于key的最小键Entry
        final TreeMap.Entry<K,V> absCeiling(K key) {
            // 如果key <= lo, 则返回获取最小键 或者 大于等于lo的最小键Entry 或者 大于lo的最小键Entry
            if (tooLow(key))
                return absLowest();

            // 如果key > lo, 否则返回大于或者等于Key的最小键的Entry
            TreeMap.Entry<K,V> e = m.getCeilingEntry(key);

            // 如果e > hi, 则返回null, 否则正常返回
            return (e == null || tooHigh(e.key)) ? null : e;
        }

        // 获取最小键 或者 大于key的最小键Entry
        final TreeMap.Entry<K,V> absHigher(K key) {
            // 如果key <= lo, 则返回获取最小键 或者 大于等于lo的最小键Entry 或者 大于lo的最小键Entry
            if (tooLow(key))
                return absLowest();

            // 如果key > lo, 否则返回大于Key的最小键Entry
            TreeMap.Entry<K,V> e = m.getHigherEntry(key);

            // 如果e > hi, 则返回null, 否则正常返回
            return (e == null || tooHigh(e.key)) ? null : e;
        }

        // 获取最大键 或者 小于等于key的最大键Entry 或者 小于key的最大键Entry
        final TreeMap.Entry<K,V> absFloor(K key) {
            // 如果key >= lo, 则返回获取最大键 或者 小于等于hi的最大键Entry 或者 小于hi的最大键Entry
            if (tooHigh(key))
                return absHighest();

            // 如果key > lo, 否则返回小于或者等于Key的最大键的Entry
            TreeMap.Entry<K,V> e = m.getFloorEntry(key);

            // 如果e < lo, 则返回null, 否则正常返回
            return (e == null || tooLow(e.key)) ? null : e;
        }

        // 获取最大键 或者 小于key的最大键Entry
        final TreeMap.Entry<K,V> absLower(K key) {
            // 如果key >= lo, 则返回获取最大键 或者 小于等于hi的最大键Entry 或者 小于hi的最大键Entry
            if (tooHigh(key))
                return absHighest();

            // 如果key > lo, 否则返回小于key的最大键的Entry
            TreeMap.Entry<K,V> e = m.getLowerEntry(key);

            // 如果e < lo, 则返回null, 否则正常返回
            return (e == null || tooLow(e.key)) ? null : e;
        }

        // 返回上升遍历的绝对高围栏
        /** Returns the absolute high fence for ascending traversal */
        // 获取最高围栏: 如果绝对从尾开始, 则返回null; 否则如果包含边界, 则返回大于hi的最小键, 如果不包含边界, 则返回大于等于hi的最小键
        final TreeMap.Entry<K,V> absHighFence() {
            return (toEnd ? null : (hiInclusive ?
                                    m.getHigherEntry(hi) :
                                    m.getCeilingEntry(hi)));
        }

        // 返回下降遍历的绝对低围栏
        /** Return the absolute low fence for descending traversal  */
        // 获取最低围栏: 如果绝对从头开始, 则返回null; 否则如果包含边界, 则返回小于lo的最大键, 如果不包含边界, 则返回小于等于lo的最大键
        final TreeMap.Entry<K,V> absLowFence() {
            return (fromStart ? null : (loInclusive ?
                                        m.getLowerEntry(lo) :
                                        m.getFloorEntry(lo)));
        }

        // Abstract methods defined in ascending vs descending classes
        // These relay to the appropriate absolute versions

        abstract TreeMap.Entry<K,V> subLowest();
        abstract TreeMap.Entry<K,V> subHighest();
        abstract TreeMap.Entry<K,V> subCeiling(K key);
        abstract TreeMap.Entry<K,V> subHigher(K key);
        abstract TreeMap.Entry<K,V> subFloor(K key);
        abstract TreeMap.Entry<K,V> subLower(K key);

        /** Returns ascending iterator from the perspective of this submap */
        abstract Iterator<K> keyIterator();

        abstract Spliterator<K> keySpliterator();

        /** Returns descending iterator from the perspective of this submap */
        abstract Iterator<K> descendingKeyIterator();

        // public methods

        public boolean isEmpty() {
            return (fromStart && toEnd) ? m.isEmpty() : entrySet().isEmpty();
        }

        public int size() {
            return (fromStart && toEnd) ? m.size() : entrySet().size();
        }

        public final boolean containsKey(Object key) {
            return inRange(key) && m.containsKey(key);
        }

        public final V put(K key, V value) {
            if (!inRange(key))
                throw new IllegalArgumentException("key out of range");
            return m.put(key, value);
        }

        public final V get(Object key) {
            return !inRange(key) ? null :  m.get(key);
        }

        public final V remove(Object key) {
            return !inRange(key) ? null : m.remove(key);
        }

        public final Map.Entry<K,V> ceilingEntry(K key) {
            return exportEntry(subCeiling(key));
        }

        public final K ceilingKey(K key) {
            return keyOrNull(subCeiling(key));
        }

        public final Map.Entry<K,V> higherEntry(K key) {
            return exportEntry(subHigher(key));
        }

        public final K higherKey(K key) {
            return keyOrNull(subHigher(key));
        }

        public final Map.Entry<K,V> floorEntry(K key) {
            return exportEntry(subFloor(key));
        }

        public final K floorKey(K key) {
            return keyOrNull(subFloor(key));
        }

        public final Map.Entry<K,V> lowerEntry(K key) {
            return exportEntry(subLower(key));
        }

        public final K lowerKey(K key) {
            return keyOrNull(subLower(key));
        }

        public final K firstKey() {
            return key(subLowest());
        }

        public final K lastKey() {
            return key(subHighest());
        }

        public final Map.Entry<K,V> firstEntry() {
            return exportEntry(subLowest());
        }

        public final Map.Entry<K,V> lastEntry() {
            return exportEntry(subHighest());
        }

        public final Map.Entry<K,V> pollFirstEntry() {
            TreeMap.Entry<K,V> e = subLowest();
            Map.Entry<K,V> result = exportEntry(e);
            if (e != null)
                m.deleteEntry(e);
            return result;
        }

        public final Map.Entry<K,V> pollLastEntry() {
            TreeMap.Entry<K,V> e = subHighest();
            Map.Entry<K,V> result = exportEntry(e);
            if (e != null)
                m.deleteEntry(e);
            return result;
        }

        // Views
        transient NavigableMap<K,V> descendingMapView;
        transient EntrySetView entrySetView;
        transient KeySet<K> navigableKeySetView;

        public final NavigableSet<K> navigableKeySet() {
            KeySet<K> nksv = navigableKeySetView;
            return (nksv != null) ? nksv :
                (navigableKeySetView = new TreeMap.KeySet<>(this));
        }

        public final Set<K> keySet() {
            return navigableKeySet();
        }

        public NavigableSet<K> descendingKeySet() {
            return descendingMap().navigableKeySet();
        }

        public final SortedMap<K,V> subMap(K fromKey, K toKey) {
            return subMap(fromKey, true, toKey, false);
        }

        public final SortedMap<K,V> headMap(K toKey) {
            return headMap(toKey, false);
        }

        public final SortedMap<K,V> tailMap(K fromKey) {
            return tailMap(fromKey, true);
        }

        // View classes

        abstract class EntrySetView extends AbstractSet<Map.Entry<K,V>> {
            private transient int size = -1, sizeModCount;

            public int size() {
                if (fromStart && toEnd)
                    return m.size();
                if (size == -1 || sizeModCount != m.modCount) {
                    sizeModCount = m.modCount;
                    size = 0;
                    Iterator<?> i = iterator();
                    while (i.hasNext()) {
                        size++;
                        i.next();
                    }
                }
                return size;
            }

            public boolean isEmpty() {
                TreeMap.Entry<K,V> n = absLowest();
                return n == null || tooHigh(n.key);
            }

            public boolean contains(Object o) {
                if (!(o instanceof Map.Entry))
                    return false;
                Map.Entry<?,?> entry = (Map.Entry<?,?>) o;
                Object key = entry.getKey();
                if (!inRange(key))
                    return false;
                TreeMap.Entry<?,?> node = m.getEntry(key);
                return node != null &&
                    valEquals(node.getValue(), entry.getValue());
            }

            public boolean remove(Object o) {
                if (!(o instanceof Map.Entry))
                    return false;
                Map.Entry<?,?> entry = (Map.Entry<?,?>) o;
                Object key = entry.getKey();
                if (!inRange(key))
                    return false;
                TreeMap.Entry<K,V> node = m.getEntry(key);
                if (node!=null && valEquals(node.getValue(),
                                            entry.getValue())) {
                    m.deleteEntry(node);
                    return true;
                }
                return false;
            }
        }

        /**
         * Iterators for SubMaps
         */
        abstract class SubMapIterator<T> implements Iterator<T> {
            TreeMap.Entry<K,V> lastReturned;
            TreeMap.Entry<K,V> next;
            final Object fenceKey;
            int expectedModCount;

            SubMapIterator(TreeMap.Entry<K,V> first,
                           TreeMap.Entry<K,V> fence) {
                expectedModCount = m.modCount;
                lastReturned = null;
                next = first;
                fenceKey = fence == null ? UNBOUNDED : fence.key;
            }

            public final boolean hasNext() {
                return next != null && next.key != fenceKey;
            }

            final TreeMap.Entry<K,V> nextEntry() {
                TreeMap.Entry<K,V> e = next;
                if (e == null || e.key == fenceKey)
                    throw new NoSuchElementException();
                if (m.modCount != expectedModCount)
                    throw new ConcurrentModificationException();
                next = successor(e);
                lastReturned = e;
                return e;
            }

            final TreeMap.Entry<K,V> prevEntry() {
                TreeMap.Entry<K,V> e = next;
                if (e == null || e.key == fenceKey)
                    throw new NoSuchElementException();
                if (m.modCount != expectedModCount)
                    throw new ConcurrentModificationException();
                next = predecessor(e);
                lastReturned = e;
                return e;
            }

            final void removeAscending() {
                if (lastReturned == null)
                    throw new IllegalStateException();
                if (m.modCount != expectedModCount)
                    throw new ConcurrentModificationException();
                // deleted entries are replaced by their successors
                if (lastReturned.left != null && lastReturned.right != null)
                    next = lastReturned;
                m.deleteEntry(lastReturned);
                lastReturned = null;
                expectedModCount = m.modCount;
            }

            final void removeDescending() {
                if (lastReturned == null)
                    throw new IllegalStateException();
                if (m.modCount != expectedModCount)
                    throw new ConcurrentModificationException();
                m.deleteEntry(lastReturned);
                lastReturned = null;
                expectedModCount = m.modCount;
            }

        }

        final class SubMapEntryIterator extends SubMapIterator<Map.Entry<K,V>> {
            SubMapEntryIterator(TreeMap.Entry<K,V> first,
                                TreeMap.Entry<K,V> fence) {
                super(first, fence);
            }
            public Map.Entry<K,V> next() {
                return nextEntry();
            }
            public void remove() {
                removeAscending();
            }
        }

        final class DescendingSubMapEntryIterator extends SubMapIterator<Map.Entry<K,V>> {
            DescendingSubMapEntryIterator(TreeMap.Entry<K,V> last,
                                          TreeMap.Entry<K,V> fence) {
                super(last, fence);
            }

            public Map.Entry<K,V> next() {
                return prevEntry();
            }
            public void remove() {
                removeDescending();
            }
        }

        // Implement minimal Spliterator as KeySpliterator backup
        final class SubMapKeyIterator extends SubMapIterator<K> implements Spliterator<K> {
            SubMapKeyIterator(TreeMap.Entry<K,V> first,
                              TreeMap.Entry<K,V> fence) {
                super(first, fence);
            }
            public K next() {
                return nextEntry().key;
            }
            public void remove() {
                removeAscending();
            }
            public Spliterator<K> trySplit() {
                return null;
            }
            public void forEachRemaining(Consumer<? super K> action) {
                while (hasNext())
                    action.accept(next());
            }
            public boolean tryAdvance(Consumer<? super K> action) {
                if (hasNext()) {
                    action.accept(next());
                    return true;
                }
                return false;
            }
            public long estimateSize() {
                return Long.MAX_VALUE;
            }
            public int characteristics() {
                return Spliterator.DISTINCT | Spliterator.ORDERED |
                    Spliterator.SORTED;
            }
            public final Comparator<? super K>  getComparator() {
                return NavigableSubMap.this.comparator();
            }
        }

        final class DescendingSubMapKeyIterator extends SubMapIterator<K> implements Spliterator<K> {
            DescendingSubMapKeyIterator(TreeMap.Entry<K,V> last,
                                        TreeMap.Entry<K,V> fence) {
                super(last, fence);
            }
            public K next() {
                return prevEntry().key;
            }
            public void remove() {
                removeDescending();
            }
            public Spliterator<K> trySplit() {
                return null;
            }
            public void forEachRemaining(Consumer<? super K> action) {
                while (hasNext())
                    action.accept(next());
            }
            public boolean tryAdvance(Consumer<? super K> action) {
                if (hasNext()) {
                    action.accept(next());
                    return true;
                }
                return false;
            }
            public long estimateSize() {
                return Long.MAX_VALUE;
            }
            public int characteristics() {
                return Spliterator.DISTINCT | Spliterator.ORDERED;
            }
        }
    }

    /**
     * @serial include
     */
    static final class AscendingSubMap<K,V> extends NavigableSubMap<K,V> {
        private static final long serialVersionUID = 912986545866124060L;

        AscendingSubMap(TreeMap<K,V> m,
                        boolean fromStart, K lo, boolean loInclusive,
                        boolean toEnd,     K hi, boolean hiInclusive) {
            super(m, fromStart, lo, loInclusive, toEnd, hi, hiInclusive);
        }

        public Comparator<? super K> comparator() {
            return m.comparator();
        }

        public NavigableMap<K,V> subMap(K fromKey, boolean fromInclusive,
                                        K toKey,   boolean toInclusive) {
            if (!inRange(fromKey, fromInclusive))
                throw new IllegalArgumentException("fromKey out of range");
            if (!inRange(toKey, toInclusive))
                throw new IllegalArgumentException("toKey out of range");
            return new AscendingSubMap<>(m,
                                         false, fromKey, fromInclusive,
                                         false, toKey,   toInclusive);
        }

        public NavigableMap<K,V> headMap(K toKey, boolean inclusive) {
            if (!inRange(toKey, inclusive))
                throw new IllegalArgumentException("toKey out of range");
            return new AscendingSubMap<>(m,
                                         fromStart, lo,    loInclusive,
                                         false,     toKey, inclusive);
        }

        public NavigableMap<K,V> tailMap(K fromKey, boolean inclusive) {
            if (!inRange(fromKey, inclusive))
                throw new IllegalArgumentException("fromKey out of range");
            return new AscendingSubMap<>(m,
                                         false, fromKey, inclusive,
                                         toEnd, hi,      hiInclusive);
        }

        public NavigableMap<K,V> descendingMap() {
            NavigableMap<K,V> mv = descendingMapView;
            return (mv != null) ? mv :
                (descendingMapView =
                 new DescendingSubMap<>(m,
                                        fromStart, lo, loInclusive,
                                        toEnd,     hi, hiInclusive));
        }

        Iterator<K> keyIterator() {
            return new SubMapKeyIterator(absLowest(), absHighFence());
        }

        Spliterator<K> keySpliterator() {
            return new SubMapKeyIterator(absLowest(), absHighFence());
        }

        Iterator<K> descendingKeyIterator() {
            return new DescendingSubMapKeyIterator(absHighest(), absLowFence());
        }

        final class AscendingEntrySetView extends EntrySetView {
            public Iterator<Map.Entry<K,V>> iterator() {
                return new SubMapEntryIterator(absLowest(), absHighFence());
            }
        }

        public Set<Map.Entry<K,V>> entrySet() {
            EntrySetView es = entrySetView;
            return (es != null) ? es : (entrySetView = new AscendingEntrySetView());
        }

        TreeMap.Entry<K,V> subLowest()       { return absLowest(); }
        TreeMap.Entry<K,V> subHighest()      { return absHighest(); }
        TreeMap.Entry<K,V> subCeiling(K key) { return absCeiling(key); }
        TreeMap.Entry<K,V> subHigher(K key)  { return absHigher(key); }
        TreeMap.Entry<K,V> subFloor(K key)   { return absFloor(key); }
        TreeMap.Entry<K,V> subLower(K key)   { return absLower(key); }
    }

    /**
     * @serial include
     */
    static final class DescendingSubMap<K,V>  extends NavigableSubMap<K,V> {
        private static final long serialVersionUID = 912986545866120460L;
        DescendingSubMap(TreeMap<K,V> m,
                        boolean fromStart, K lo, boolean loInclusive,
                        boolean toEnd,     K hi, boolean hiInclusive) {
            super(m, fromStart, lo, loInclusive, toEnd, hi, hiInclusive);
        }

        private final Comparator<? super K> reverseComparator =
            Collections.reverseOrder(m.comparator);

        public Comparator<? super K> comparator() {
            return reverseComparator;
        }

        public NavigableMap<K,V> subMap(K fromKey, boolean fromInclusive,
                                        K toKey,   boolean toInclusive) {
            if (!inRange(fromKey, fromInclusive))
                throw new IllegalArgumentException("fromKey out of range");
            if (!inRange(toKey, toInclusive))
                throw new IllegalArgumentException("toKey out of range");
            return new DescendingSubMap<>(m,
                                          false, toKey,   toInclusive,
                                          false, fromKey, fromInclusive);
        }

        public NavigableMap<K,V> headMap(K toKey, boolean inclusive) {
            if (!inRange(toKey, inclusive))
                throw new IllegalArgumentException("toKey out of range");
            return new DescendingSubMap<>(m,
                                          false, toKey, inclusive,
                                          toEnd, hi,    hiInclusive);
        }

        public NavigableMap<K,V> tailMap(K fromKey, boolean inclusive) {
            if (!inRange(fromKey, inclusive))
                throw new IllegalArgumentException("fromKey out of range");
            return new DescendingSubMap<>(m,
                                          fromStart, lo, loInclusive,
                                          false, fromKey, inclusive);
        }

        public NavigableMap<K,V> descendingMap() {
            NavigableMap<K,V> mv = descendingMapView;
            return (mv != null) ? mv :
                (descendingMapView =
                 new AscendingSubMap<>(m,
                                       fromStart, lo, loInclusive,
                                       toEnd,     hi, hiInclusive));
        }

        Iterator<K> keyIterator() {
            return new DescendingSubMapKeyIterator(absHighest(), absLowFence());
        }

        Spliterator<K> keySpliterator() {
            return new DescendingSubMapKeyIterator(absHighest(), absLowFence());
        }

        Iterator<K> descendingKeyIterator() {
            return new SubMapKeyIterator(absLowest(), absHighFence());
        }

        final class DescendingEntrySetView extends EntrySetView {
            public Iterator<Map.Entry<K,V>> iterator() {
                return new DescendingSubMapEntryIterator(absHighest(), absLowFence());
            }
        }

        public Set<Map.Entry<K,V>> entrySet() {
            EntrySetView es = entrySetView;
            return (es != null) ? es : (entrySetView = new DescendingEntrySetView());
        }

        TreeMap.Entry<K,V> subLowest()       { return absHighest(); }
        TreeMap.Entry<K,V> subHighest()      { return absLowest(); }
        TreeMap.Entry<K,V> subCeiling(K key) { return absFloor(key); }
        TreeMap.Entry<K,V> subHigher(K key)  { return absLower(key); }
        TreeMap.Entry<K,V> subFloor(K key)   { return absCeiling(key); }
        TreeMap.Entry<K,V> subLower(K key)   { return absHigher(key); }
    }

    /**
     * This class exists solely for the sake of serialization
     * compatibility with previous releases of TreeMap that did not
     * support NavigableMap.  It translates an old-version SubMap into
     * a new-version AscendingSubMap. This class is never otherwise
     * used.
     *
     * @serial include
     */
    private class SubMap extends AbstractMap<K,V>
        implements SortedMap<K,V>, java.io.Serializable {
        private static final long serialVersionUID = -6520786458950516097L;
        private boolean fromStart = false, toEnd = false;
        private K fromKey, toKey;
        private Object readResolve() {
            return new AscendingSubMap<>(TreeMap.this,
                                         fromStart, fromKey, true,
                                         toEnd, toKey, false);
        }
        public Set<Map.Entry<K,V>> entrySet() { throw new InternalError(); }
        public K lastKey() { throw new InternalError(); }
        public K firstKey() { throw new InternalError(); }
        public SortedMap<K,V> subMap(K fromKey, K toKey) { throw new InternalError(); }
        public SortedMap<K,V> headMap(K toKey) { throw new InternalError(); }
        public SortedMap<K,V> tailMap(K fromKey) { throw new InternalError(); }
        public Comparator<? super K> comparator() { throw new InternalError(); }
    }


    // Red-black mechanics

    private static final boolean RED   = false;
    private static final boolean BLACK = true;

    /**
     * 202106010
     * 树中的节点。 双倍作为将键值对传递回用户的一种方式（参见 Map.Entry）。
     */
    /**
     * Node in the Tree.  Doubles as a means to pass key-value pairs back to
     * user (see Map.Entry).
     */
    static final class Entry<K,V> implements Map.Entry<K,V> {
        K key;
        V value;
        Entry<K,V> left;
        Entry<K,V> right;
        Entry<K,V> parent;
        boolean color = BLACK;

        /**
         * 20210610
         * 使用给定的键、值和父级以及 {@code null} 子链接和黑色创建一个新单元格。
         */
        /**
         * Make a new cell with given key, value, and parent, and with
         * {@code null} child links, and BLACK color.
         */
        Entry(K key, V value, Entry<K,V> parent) {
            this.key = key;
            this.value = value;
            this.parent = parent;
        }

        /**
         * Returns the key.
         *
         * @return the key
         */
        public K getKey() {
            return key;
        }

        /**
         * Returns the value associated with the key.
         *
         * @return the value associated with the key
         */
        public V getValue() {
            return value;
        }

        /**
         * 20210610
         * 用给定的值替换当前与键关联的值。
         */
        /**
         * Replaces the value currently associated with the key with the given
         * value.
         *
         * @return the value associated with the key before this method was
         *         called
         */
        public V setValue(V value) {
            V oldValue = this.value;
            this.value = value;
            return oldValue;
        }

        public boolean equals(Object o) {
            if (!(o instanceof Map.Entry))
                return false;
            Map.Entry<?,?> e = (Map.Entry<?,?>)o;

            return valEquals(key,e.getKey()) && valEquals(value,e.getValue());
        }

        public int hashCode() {
            int keyHash = (key==null ? 0 : key.hashCode());
            int valueHash = (value==null ? 0 : value.hashCode());
            return keyHash ^ valueHash;
        }

        public String toString() {
            return key + "=" + value;
        }
    }

    /**
     * 20210613
     * 返回 TreeMap 中的第一个 Entry（根据 TreeMap 的键排序函数）。 如果 TreeMap 为空，则返回 null。
     */
    /**
     * Returns the first Entry in the TreeMap (according to the TreeMap's
     * key-sort function).  Returns null if the TreeMap is empty.
     */
    final Entry<K,V> getFirstEntry() {
        Entry<K,V> p = root;
        if (p != null)
            while (p.left != null)
                p = p.left;
        return p;
    }

    /**
     * 20210613
     * 返回 TreeMap 中的最后一个 Entry（根据 TreeMap 的键排序函数）。 如果 TreeMap 为空，则返回 null。
     */
    /**
     * Returns the last Entry in the TreeMap (according to the TreeMap's
     * key-sort function).  Returns null if the TreeMap is empty.
     */
    final Entry<K,V> getLastEntry() {
        Entry<K,V> p = root;
        if (p != null)
            while (p.right != null)
                p = p.right;
        return p;
    }

    /**
     * 20210612
     * 返回指定条目的后继者，如果没有，则返回 null。
     */
    /**
     * Returns the successor of the specified Entry, or null if no such.
     */
    static <K,V> TreeMap.Entry<K,V> successor(Entry<K,V> t) {
        // 如果t为null, 则后继也为null
        if (t == null)
            return null;

        // 如果t有右孩子, 则后继为右孩子或者为右孩子最左的左孩子(顺方向)
        else if (t.right != null) {
            Entry<K,V> p = t.right;
            while (p.left != null)
                p = p.left;
            return p;
        }
        // 如果t没有右孩子, 则后继为该分叉尽头结点的父结点(逆方向)
        else {
            Entry<K,V> p = t.parent;
            Entry<K,V> ch = t;
            while (p != null && ch == p.right) {
                ch = p;
                p = p.parent;
            }
            return p;
        }
    }

    /**
     * Returns the predecessor of the specified Entry, or null if no such.
     */
    static <K,V> Entry<K,V> predecessor(Entry<K,V> t) {
        if (t == null)
            return null;
        else if (t.left != null) {
            Entry<K,V> p = t.left;
            while (p.right != null)
                p = p.right;
            return p;
        } else {
            Entry<K,V> p = t.parent;
            Entry<K,V> ch = t;
            while (p != null && ch == p.left) {
                ch = p;
                p = p.parent;
            }
            return p;
        }
    }

    /**
     * Balancing operations.
     *
     * Implementations of rebalancings during insertion and deletion are
     * slightly different than the CLR version.  Rather than using dummy
     * nilnodes, we use a set of accessors that deal properly with null.  They
     * are used to avoid messiness surrounding nullness checks in the main
     * algorithms.
     */

    private static <K,V> boolean colorOf(Entry<K,V> p) {
        return (p == null ? BLACK : p.color);
    }

    private static <K,V> Entry<K,V> parentOf(Entry<K,V> p) {
        return (p == null ? null: p.parent);
    }

    private static <K,V> void setColor(Entry<K,V> p, boolean c) {
        if (p != null)
            p.color = c;
    }

    private static <K,V> Entry<K,V> leftOf(Entry<K,V> p) {
        return (p == null) ? null: p.left;
    }

    private static <K,V> Entry<K,V> rightOf(Entry<K,V> p) {
        return (p == null) ? null: p.right;
    }

    /** From CLR */
    // 左边的高度比右边的矮, 通过左旋可以增加左边高度
    private void rotateLeft(Entry<K,V> p) {
        // 旋转结点p, p的右孩子r
        if (p != null) {
            Entry<K,V> r = p.right;

            // r的左孩子脱钩, 成为p的右孩子
            p.right = r.left;
            if (r.left != null)
                r.left.parent = p;

            // r结点作为p的父结点
            r.parent = p.parent;
            if (p.parent == null)
                root = r;
            else if (p.parent.left == p)
                p.parent.left = r;
            else
                p.parent.right = r;
            r.left = p;
            p.parent = r;
        }
    }

    /** From CLR */
    // 右边的高度比左边的矮, 通过右旋可以增加右边高度
    private void rotateRight(Entry<K,V> p) {
        // 旋转结点p, p的左孩子l
        if (p != null) {
            Entry<K,V> l = p.left;

            // l的右孩子脱钩, 成为p的左孩子
            p.left = l.right;
            if (l.right != null)
                l.right.parent = p;

            // l结点作为p的父结点
            l.parent = p.parent;
            if (p.parent == null)
                root = l;
            else if (p.parent.right == p)
                p.parent.right = l;
            else p.parent.left = l;
            l.right = p;
            p.parent = l;
        }
    }

    /** From CLR */
    // 插入结点后平衡红黑树
    private void fixAfterInsertion(Entry<K,V> x) {
        // 标记插入结点x为红结点
        x.color = RED;

        // 如果x不为root, 且x的父结点parent为红结点时才需要调整
        // x作为调整的逻辑终止条件分析: 1) 如果x为根结点, 则置黑root(即x)相当于把一个4结点成为了3个2结点); 2) 如果x父结点为黑, 此时x相当于插入到了一个2结点无需再作调整
        while (x != null && x != root && x.parent.color == RED) {
            // x的父结点为爷结点的左孩子时, x的叔结点y
            if (parentOf(x) == leftOf(parentOf(parentOf(x)))) {
                Entry<K,V> y = rightOf(parentOf(parentOf(x)));

                // x的叔结点为红结点, 说明为合并到4结点中: 成为一个裂变状态（变色后相当于升元了）, 插入前为4结点（黑红红）,
                // 插入后4结点颜色反转，爷结点成为新的x结点，准备下一轮的向上调整，根据x插入的位置不同分为: 中左左* 右、中左右* 右
                if (colorOf(y) == RED) {
                    setColor(parentOf(x), BLACK);// x的父结点置黑
                    setColor(y, BLACK);// x的叔结点置黑
                    setColor(parentOf(parentOf(x)), RED);// x的爷结点置红
                    x = parentOf(parentOf(x));// 爷结点成为新的x结点(视为新插入的红结点), 准备下一轮的向上调整, 直到终止条件1或2的发生
                }
                // x的叔结点不为红结点, 说明叔结点不存在 或者 叔结点为黑色(不存在这情况)
                // 而x的叔结点不存在, 说明为合并到3结点中: 成为一个4结点, 插入前为3结点（上黑下左红）,
                // 插入后成为4结点黑红红的情况，根据x插入位置不同分为: 左三、中左右*
                else {
                    // 如果x为父结点的右孩子时, 说明为 中左右* 情况, 此时需要先对父结点左旋成 左三 的情况
                    if (x == rightOf(parentOf(x))) {
                        x = parentOf(x);
                        rotateLeft(x);
                    }
                    // 来到这里, 说明肯定为黑红红 左三 的情况, 为了使其成为3结点, 需要对爷结点置红, 父结点置黑, 然后爷结点右旋, 让父结点成为中
                    setColor(parentOf(x), BLACK);// 父结点置黑
                    setColor(parentOf(parentOf(x)), RED);// 爷结点置红
                    rotateRight(parentOf(parentOf(x)));// 爷结点右旋, 让父结点成为中
                }
            }
            // x的父结点为爷结点的右孩子时, 叔结点y
            else {
                Entry<K,V> y = leftOf(parentOf(parentOf(x)));

                // x的叔结点为红结点, 说明为合并到4结点中: 成为一个裂变状态（变色后相当于升元了）, 插入前为4结点（黑红红）,
                // 插入后4结点颜色反转，爷结点成为新的x结点，准备下一轮的向上调整，根据x插入的位置不同分为: 中左 右左*、中左 右右*
                if (colorOf(y) == RED) {
                    setColor(parentOf(x), BLACK);// x的父结点置黑
                    setColor(y, BLACK);// x的叔结点置黑
                    setColor(parentOf(parentOf(x)), RED);// x的爷结点置红
                    x = parentOf(parentOf(x));// 爷结点成为新的x结点(视为新插入的红结点), 准备下一轮的向上调整, 直到终止条件1或2的发生
                }
                // x的叔结点不为红结点, 说明叔结点不存在 或者 叔结点为黑色(不存在这情况)
                // 而x的叔结点不存在, 说明为合并到3结点中: 成为一个4结点, 插入前为3结点（上黑下右红）,
                // 插入后成为4结点黑红红的情况，根据x插入位置不同分为: 右三、中右左*
                else {
                    // 如果x为父结点的左孩子时, 说明为 中右左* 情况, 此时需要先对父结点右旋成 右三 的情况
                    if (x == leftOf(parentOf(x))) {
                        x = parentOf(x);
                        rotateRight(x);
                    }
                    // 来到这里, 说明肯定为黑红红 右三 的情况, 为了使其成为3结点, 需要对爷结点置红, 父结点置黑, 然后爷结点左旋, 让父结点成为中
                    setColor(parentOf(x), BLACK);// 父结点置黑
                    setColor(parentOf(parentOf(x)), RED);// 爷结点置红
                    rotateLeft(parentOf(parentOf(x)));// 爷结点右旋, 让父结点成为中
                }
            }
        }

        // 如果x为root, 或者x的父结点为黑结点时, 不需要调整
        // a. 空结点新增: x成为一个2结点, 作为根结点x变为黑色
        // b. 合并到2结点: x与parent成为一个3结点, 插入前2结点为黑色, 插入符合3结点条件(上黑下左红 | 上黑下右红), 因此直接返回即可
        // c. 合并到3结点中(此时x的父结点为上黑): 成为一个4结点, 插入前为3结点（上黑下红）, 插入后成为4结点黑红红的情况，根据x插入位置不同分为: 中左* 右、中左 右*
        root.color = BLACK;
    }

    /**
     * 20210612
     * 删除节点 p，然后重新平衡树。
     */
    /**
     * Delete node p, and then rebalance the tree.
     */
    private void deleteEntry(Entry<K,V> p) {
        modCount++;
        size--;

        // 如果严格为内部，则将后继元素复制到 p，然后使 p 指向后继。
        // If strictly internal, copy successor's element to p and then make p
        // point to successor.

        // 如果p同时存在左右孩子, 则寻找后继s, 交换s和p, 此后p指向后继s结点
        if (p.left != null && p.right != null) {
            Entry<K,V> s = successor(p);
            p.key = s.key;
            p.value = s.value;
            p = s;
        } // p has 2 children

        // Start fixup at replacement node, if it exists. // 在替换节点处开始修复（如果存在）。
        // 如果p只有一个孩子, 或者没有孩子, 或者交换到了后继位置, 则选择替代结点: 如果此时p存在左孩子, 则替代结点为左孩子, 否则为右孩子
        Entry<K,V> replacement = (p.left != null ? p.left : p.right);

        // 如果替代结点不为null, 说明p至少有一个孩子, 即p为非叶子结点为3结点或者4结点, 则p的父结点链接替代结点, 父结点脱钩p
        if (replacement != null) {
            // Link replacement to parent  // 链接替换到父级
            replacement.parent = p.parent;
            if (p.parent == null)
                root = replacement;
            else if (p == p.parent.left)
                p.parent.left  = replacement;
            else
                p.parent.right = replacement;

            // Null out links so they are OK to use by fixAfterDeletion. // 清除链接，以便 fixAfterDeletion 可以使用它们。
            // p脱钩父结点与孩子结点
            p.left = p.right = p.parent = null;

            // 这里p为3结点或者4结点, 如果p为黑结点, 则需要删除结点后平衡红黑树
            // Fix replacement
            if (p.color == BLACK)
                fixAfterDeletion(replacement);
        }
        // 如果替代结点为null, 说明p没有孩子, 但p的父结点为null, 说明p为根结点, 直接置空根结点即可
        else if (p.parent == null) { // return if we are the only node. // 如果我们是唯一的节点，则返回。
            root = null;
        }
        // 如果替代结点为null, 说明p没有孩子, 且p也不是根结点, 说明p为叶子结点
        else { //  No children. Use self as phantom replacement and unlink. // 没有小孩。 使用 self 作为幻像替换和取消链接。
            // 这里p为2结点, 如果p为黑结点, 则需要删除结点前平衡红黑树
            if (p.color == BLACK)
                fixAfterDeletion(p);

            // 红黑树平衡之后, 父结点脱钩p, p脱钩父结点
            if (p.parent != null) {
                if (p == p.parent.left)
                    p.parent.left = null;
                else if (p == p.parent.right)
                    p.parent.right = null;
                p.parent = null;
            }
        }
    }

    /** From CLR */
    // 删除结点前/后平衡红黑树
    private void fixAfterDeletion(Entry<K,V> x) {
        // 如果x不为root, 且x为黑结点时才需要调整
        while (x != root && colorOf(x) == BLACK) {
            // x为父结点的左孩子时, 兄弟结点sib
            if (x == leftOf(parentOf(x))) {
                Entry<K,V> sib = rightOf(parentOf(x));

                // 如果兄弟结点为红色, 说明不是真正的兄弟结点(只是父结点的红结点), 需要对父结点进行左旋, 左旋x出现了真正的兄弟结点, 原父结点位置被假的兄弟结点占据
                if (colorOf(sib) == RED) {
                    setColor(sib, BLACK);// 置黑假的兄弟结点(因为将要成为x的爷结点)
                    setColor(parentOf(x), RED);// 置红x的父结点(设置为原兄弟结点的红色, 因为将要把原父结点3结点样式从中右改成中左)
                    rotateLeft(parentOf(x));// 左旋父结点(将要把原父结点3结点样式从中右改成中左)
                    sib = rightOf(parentOf(x));// sib更新为x真正的兄弟结点
                }
                // sib为x真正的兄弟结点, 如果兄弟结点没有可用的结点(2结点c.1时), 则 x设置为父结点, 继续向上自损, 置红叔结点, 直到根结点或者红结点
                if (colorOf(leftOf(sib))  == BLACK &&
                    colorOf(rightOf(sib)) == BLACK) {
                    setColor(sib, RED);// 自损, 置红叔结点
                    x = parentOf(x);// x设置为父结点, 继续向上自损, 直到根结点或者红结点
                }
                // sib为x真正的兄弟结点, 如果兄弟结点有可用的孩子结点(3结点b.1、b.2或者4结点b.3时)
                else {
                    // 如果叔结点不存在右结点, 说明为3结点无右b.1的情况, 为一个临时情况, 需要对叔结点进行右旋, 右转后成为有右b.2情况
                    if (colorOf(rightOf(sib)) == BLACK) {
                        setColor(leftOf(sib), BLACK);// 置黑叔结点的左孩子(因为无右那肯定有左, 此时置黑为了成为父结点)
                        setColor(sib, RED);// 置红叔结点(为了成为有右的右结点)
                        rotateRight(sib);// 右旋叔结点
                        sib = rightOf(parentOf(x));// sib更新为最新的叔结点指针
                    }
                    // 到这里肯定有右即3结点b.2和4结点b.3的情况, 此时借出两个结点(最多), 则置叔为父结点颜色, 父结点置黑, 叔右结点置黑, 左旋父结点,
                    // 让原父结点成为x和叔左结点的父结点, 叔结点站上原父结点的位置, 叔右结点保持为右孩子(2结点), 黑黑黑
                    setColor(sib, colorOf(parentOf(x)));// 置叔为父结点颜色(因为将要站上父结点的位置)
                    setColor(parentOf(x), BLACK);// 父结点置黑(因为左旋后借出去的父结点要成为3结点或者4结点的黑结点)
                    setColor(rightOf(sib), BLACK);// 叔右结点置黑(因为原父结点置黑了, 叔右结点为黑(2结点), 才能保持黑黑黑的结构)
                    rotateLeft(parentOf(x));// 左旋父结点
                    x = root;// 设置x为根结点, 退出循环, 停止红黑树调整
                }
            }
            // x为父结点的右孩子时, 兄弟结点sib
            else { // symmetric // 对称的
                Entry<K,V> sib = leftOf(parentOf(x));

                // 如果兄弟结点为红色, 说明不是真正的兄弟结点(只是父结点的红结点), 需要对父结点进行右旋, 右旋x出现了真正的兄弟结点, 原父结点位置被假的兄弟结点占据
                if (colorOf(sib) == RED) {
                    setColor(sib, BLACK);// 置黑假的兄弟结点(因为将要成为x的爷结点)
                    setColor(parentOf(x), RED);// 置红x的父结点(设置为原兄弟结点的红色, 因为将要把原父结点3结点样式从中左改成中右)
                    rotateRight(parentOf(x));// 左旋父结点(将要把原父结点3结点样式从中左改成中右)
                    sib = leftOf(parentOf(x));// sib更新为x真正的兄弟结点
                }
                // sib为x真正的兄弟结点, 如果兄弟结点没有可用的结点(2结点c.1时), 则 x设置为父结点, 继续向上自损, 置红叔结点, 直到根结点或者红结点
                if (colorOf(rightOf(sib)) == BLACK &&
                    colorOf(leftOf(sib)) == BLACK) {
                    setColor(sib, RED);// 自损, 置红叔结点
                    x = parentOf(x);// x设置为父结点, 继续向上自损, 直到根结点或者红结点
                }
                // sib为x真正的兄弟结点, 如果兄弟结点有可用的孩子结点(3结点b.1、b.2或者4结点b.3时)
                else {
                    // 如果叔结点不存在左结点, 说明为3结点无左b.1的情况, 为一个临时情况, 需要对叔结点进行左旋, 左转后成为有左b.2情况
                    if (colorOf(leftOf(sib)) == BLACK) {
                        setColor(rightOf(sib), BLACK);// 置黑叔结点的左孩子(因为无左那肯定有右, 此时置黑为了成为父结点)
                        setColor(sib, RED);// 置红叔结点(为了成为有左的左结点)
                        rotateLeft(sib);// 左旋叔结点
                        sib = leftOf(parentOf(x));// sib更新为最新的叔结点指针
                    }
                    // 到这里肯定有左即3结点b.2和4结点b.3的情况, 此时借出两个结点(最多), 则置叔为父结点颜色, 父结点置黑, 叔右结点置黑, 右旋父结点,
                    // 让原父结点成为x和叔右结点的父结点, 叔结点站上原父结点的位置, 叔左结点保持为左孩子(2结点), 黑黑黑
                    setColor(sib, colorOf(parentOf(x)));// 置叔为父结点颜色(因为将要站上父结点的位置)
                    setColor(parentOf(x), BLACK);// 父结点置黑(因为右旋后借出去的父结点要成为3结点或者4结点的黑结点)
                    setColor(leftOf(sib), BLACK);// 叔左结点置黑(因为原父结点置黑了, 叔左结点为黑(2结点), 才能保持黑黑黑的结构)
                    rotateRight(parentOf(x));// 右旋父结点
                    x = root;// 设置x为根结点, 退出循环, 停止红黑树调整
                }
            }
        }

        // a. 自损终止条件打成: 碰到根结点或者红结点, 此时置黑x结点, 完成自损
        // b. 非自损调整完毕后, x被手动设置成了root结点, 表示需要结束调整, 此时对root结点置黑没有任何作用(因为root本身就是黑的), 只是为了重用代码而已
        setColor(x, BLACK);
    }

    private static final long serialVersionUID = 919286545866124006L;

    /**
     * Save the state of the {@code TreeMap} instance to a stream (i.e.,
     * serialize it).
     *
     * @serialData The <em>size</em> of the TreeMap (the number of key-value
     *             mappings) is emitted (int), followed by the key (Object)
     *             and value (Object) for each key-value mapping represented
     *             by the TreeMap. The key-value mappings are emitted in
     *             key-order (as determined by the TreeMap's Comparator,
     *             or by the keys' natural ordering if the TreeMap has no
     *             Comparator).
     */
    private void writeObject(java.io.ObjectOutputStream s)
        throws java.io.IOException {
        // Write out the Comparator and any hidden stuff
        s.defaultWriteObject();

        // Write out size (number of Mappings)
        s.writeInt(size);

        // Write out keys and values (alternating)
        for (Iterator<Map.Entry<K,V>> i = entrySet().iterator(); i.hasNext(); ) {
            Map.Entry<K,V> e = i.next();
            s.writeObject(e.getKey());
            s.writeObject(e.getValue());
        }
    }

    /**
     * Reconstitute the {@code TreeMap} instance from a stream (i.e.,
     * deserialize it).
     */
    private void readObject(final java.io.ObjectInputStream s)
        throws java.io.IOException, ClassNotFoundException {
        // Read in the Comparator and any hidden stuff
        s.defaultReadObject();

        // Read in size
        int size = s.readInt();

        buildFromSorted(size, null, s, null);
    }

    /** Intended to be called only from TreeSet.readObject */
    void readTreeSet(int size, java.io.ObjectInputStream s, V defaultVal)
        throws java.io.IOException, ClassNotFoundException {
        buildFromSorted(size, null, s, defaultVal);
    }

    /** Intended to be called only from TreeSet.addAll */
    void addAllForTreeSet(SortedSet<? extends K> set, V defaultVal) {
        try {
            buildFromSorted(set.size(), set.iterator(), null, defaultVal);
        } catch (java.io.IOException cannotHappen) {
        } catch (ClassNotFoundException cannotHappen) {
        }
    }

    /**
     * 20210612
     * A. 基于排序数据的线性时间O(n)树构建算法。 可以接受来自迭代器或流的键和/或值。 这会导致参数过多，但似乎比替代方案更好。 此方法接受的四种格式是：
     *      1) An iterator of Map.Entries.  (it != null, defaultVal == null).
     *      2) An iterator of keys.         (it != null, defaultVal != null).
     *      3) A stream of alternating serialized keys and values. (it == null, defaultVal == null).
     *      4) A stream of serialized keys. (it == null, defaultVal != null).
     * B. 假设在调用此方法之前已经设置了 TreeMap 的比较器。
     */
    /**
     * A.
     * Linear time tree building algorithm from sorted data.  Can accept keys
     * and/or values from iterator or stream. This leads to too many
     * parameters, but seems better than alternatives.  The four formats
     * that this method accepts are:
     *
     *    1) An iterator of Map.Entries.  (it != null, defaultVal == null).
     *    2) An iterator of keys.         (it != null, defaultVal != null).
     *    3) A stream of alternating serialized keys and values.
     *                                   (it == null, defaultVal == null).
     *    4) A stream of serialized keys. (it == null, defaultVal != null).
     *
     * B.
     * It is assumed that the comparator of the TreeMap is already set prior
     * to calling this method.
     *
     * // 要从迭代器或流中读取的键（或键值对）的数量
     * @param size the number of keys (or key-value pairs) to be read from
     *        the iterator or stream
     *
     * // 如果非空，则根据从此迭代器读取的条目或键创建新条目。
     * @param it If non-null, new entries are created from entries
     *        or keys read from this iterator.
     *
     * // 如果非空，则从以序列化形式从此流中读取的键和可能的值创建新条目。恰好其中之一和 str 应为非空。
     * @param str If non-null, new entries are created from keys and
     *        possibly values read from this stream in serialized form.
     *        Exactly one of it and str should be non-null.
     *
     * // 如果非空，则此默认值用于映射中的每个值。 如果为 null，则从迭代器或流中读取每个值，如上所述。
     * @param defaultVal if non-null, this default value is used for
     *        each value in the map.  If null, each value is read from
     *        iterator or stream, as described above.
     * @throws java.io.IOException propagated from stream reads. This cannot
     *         occur if str is null.
     * @throws ClassNotFoundException propagated from readObject.
     *         This cannot occur if str is null.
     */
    // 根据传入的迭代器it或者流str, 复制元素构造红黑树(只有最底层的结点才为红结点), 更新所有元素为传入集合的元素, 红黑树结点数目计算O(n)
    private void buildFromSorted(int size, Iterator<?> it,
                                 java.io.ObjectInputStream str,
                                 V defaultVal)
        throws  java.io.IOException, ClassNotFoundException {
        this.size = size;

        // 根据传入的迭代器it或者流str, 复制元素构造红黑树(只有最底层的结点才为红结点), 红黑树结点数目计算O(n)
        root = buildFromSorted(0, 0, size-1, computeRedLevel(size),
                               it, str, defaultVal);
    }

    /**
     * 20210612
     * 递归“辅助方法”，它完成前一个方法的实际工作。 相同命名的参数具有相同的定义。 下面记录了其他参数。 假设在调用此方法之前已经设置了 TreeMap 的比较器和大小字段。
     * （它忽略两个字段。）
     */
    /**
     * Recursive "helper method" that does the real work of the
     * previous method.  Identically named parameters have
     * identical definitions.  Additional parameters are documented below.
     * It is assumed that the comparator and size fields of the TreeMap are
     * already set prior to calling this method.  (It ignores both fields.)
     *
     * // 树的当前级别。 初始调用应为 0。
     * @param level the current level of tree. Initial call should be 0.
     *
     * // 此子树的第一个元素索引。 初始值应为 0。
     * @param lo the first element index of this subtree. Initial should be 0.
     *
     * // 此子树的最后一个元素索引。 初始应该是大小 1。
     * @param hi the last element index of this subtree.  Initial should be
     *        size-1.
     *
     * // 节点应为红色的级别。 对于这种大小的树，必须等于computeRedLevel。
     * @param redLevel the level at which nodes should be red.
     *        Must be equal to computeRedLevel for tree of this size.
     */
    @SuppressWarnings("unchecked")
    // 根据传入的迭代器it或者流str的复制元素构造红黑树(只有最底层的结点才为红结点)
    private final Entry<K,V> buildFromSorted(int level, int lo, int hi,
                                             int redLevel,
                                             Iterator<?> it,
                                             java.io.ObjectInputStream str,
                                             V defaultVal)
        throws  java.io.IOException, ClassNotFoundException {

        /**
         * 20210612
         * A. 策略：根是最中间的元素。 为了得到它，我们必须首先递归地构造整个左子树，以便获取它的所有元素。 然后我们可以继续处理右子树。
         * B. lo 和 hi 参数是从当前子树的迭代器或流中提取的最小和最大索引。 它们实际上没有被索引，我们只是按顺序进行，确保按相应的顺序提取项目。
         */
        /*
         * A.
         * Strategy: The root is the middlemost element. To get to it, we
         * have to first recursively construct the entire left subtree,
         * so as to grab all of its elements. We can then proceed with right
         * subtree.
         *
         * B.
         * The lo and hi arguments are the minimum and maximum
         * indices to pull out of the iterator or stream for current subtree.
         * They are not actually indexed, we just proceed sequentially,
         * ensuring that items are extracted in corresponding order.
         */

        // lo当前子树的最小索引, hi最大索引, mid中间索引, left左子树根结点，right右子树根结点
        if (hi < lo) return null;
        int mid = (lo + hi) >>> 1;
        Entry<K,V> left  = null;
        if (lo < mid)
            left = buildFromSorted(level+1, lo, mid - 1, redLevel,
                                   it, str, defaultVal);

        // 取原序列的中间元素作为根结点, 中间之前的元素作为左子树, 中间之后的元素作为右子树
        // extract key and/or value from iterator or stream // 从迭代器或流中提取键和/或值
        // 如果指定的是迭代器, 则从迭代器中获取数据
        K key;
        V value;
        if (it != null) {
            // 如果没指定默认值, 则key设置为迭代器元素的key, value设置为迭代器元素的value
            if (defaultVal==null) {
                Map.Entry<?,?> entry = (Map.Entry<?,?>)it.next();
                key = (K)entry.getKey();
                value = (V)entry.getValue();
            }
            // 如果指定了默认值, 则key设置为迭代器元素的key, value设置为默认值
            else {
                key = (K)it.next();
                value = defaultVal;
            }
        }
        // 如果指定的是流, 则从流中获取数据
        else { // use stream
            // 如果没指定默认值, 则key和value设置为按顺序读取流中的key和value, 否则key设置为流中的key, value设置为默认值
            key = (K) str.readObject();
            value = (defaultVal != null ? defaultVal : (V) str.readObject());
        }

        // 构造每棵子树的根结点
        Entry<K,V> middle =  new Entry<>(key, value, null);

        // color nodes in non-full bottommost level red // 非完全最底层红色的颜色节点
        // 最底层的结点设置红色
        if (level == redLevel)
            middle.color = RED;

        // 设置左子树
        if (left != null) {
            middle.left = left;
            left.parent = middle;
        }

        // 设置右子树
        if (mid < hi) {
            Entry<K,V> right = buildFromSorted(level+1, mid+1, hi, redLevel,
                                               it, str, defaultVal);
            middle.right = right;
            right.parent = middle;
        }

        // 返回当前子树的根结点
        return middle;
    }

    /**
     * 20210612
     * 找到向下分配所有黑色节点的深度。 这是由 buildTree 生成的完整二叉树的最后一个“完整”级别。 其余节点为红色。 （这使得一组“漂亮”的颜色分配与未来的插入有关。）
     * 这个深度数是通过找到到达第零节点所需的分裂数来计算的。 （答案是 ~lg(N)，但无论如何必须通过相同的快速 O(lg(N)) 循环计算。）
     */
    /**
     * Find the level down to which to assign all nodes BLACK.  This is the
     * last `full' level of the complete binary tree produced by
     * buildTree. The remaining nodes are colored RED. (This makes a `nice'
     * set of color assignments wrt future insertions.) This level number is
     * computed by finding the number of splits needed to reach the zeroeth
     * node.  (The answer is ~lg(N), but in any case must be computed by same
     * quick O(lg(N)) loop.)
     */
    // 计算传入结点高度中红黑树需要的红结点的数目
    private static int computeRedLevel(int sz) {
        // 折半从最后一个结点开始计算, 直到根结点
        int level = 0;
        for (int m = sz - 1; m >= 0; m = m / 2 - 1)
            level++;
        return level;
    }

    /**
     * Currently, we support Spliterator-based versions only for the
     * full map, in either plain of descending form, otherwise relying
     * on defaults because size estimation for submaps would dominate
     * costs. The type tests needed to check these for key views are
     * not very nice but avoid disrupting existing class
     * structures. Callers must use plain default spliterators if this
     * returns null.
     */
    static <K> Spliterator<K> keySpliteratorFor(NavigableMap<K,?> m) {
        if (m instanceof TreeMap) {
            @SuppressWarnings("unchecked") TreeMap<K,Object> t =
                (TreeMap<K,Object>) m;
            return t.keySpliterator();
        }
        if (m instanceof DescendingSubMap) {
            @SuppressWarnings("unchecked") DescendingSubMap<K,?> dm =
                (DescendingSubMap<K,?>) m;
            TreeMap<K,?> tm = dm.m;
            if (dm == tm.descendingMap) {
                @SuppressWarnings("unchecked") TreeMap<K,Object> t =
                    (TreeMap<K,Object>) tm;
                return t.descendingKeySpliterator();
            }
        }
        @SuppressWarnings("unchecked") NavigableSubMap<K,?> sm =
            (NavigableSubMap<K,?>) m;
        return sm.keySpliterator();
    }

    final Spliterator<K> keySpliterator() {
        return new KeySpliterator<K,V>(this, null, null, 0, -1, 0);
    }

    final Spliterator<K> descendingKeySpliterator() {
        return new DescendingKeySpliterator<K,V>(this, null, null, 0, -2, 0);
    }

    /**
     * Base class for spliterators.  Iteration starts at a given
     * origin and continues up to but not including a given fence (or
     * null for end).  At top-level, for ascending cases, the first
     * split uses the root as left-fence/right-origin. From there,
     * right-hand splits replace the current fence with its left
     * child, also serving as origin for the split-off spliterator.
     * Left-hands are symmetric. Descending versions place the origin
     * at the end and invert ascending split rules.  This base class
     * is non-commital about directionality, or whether the top-level
     * spliterator covers the whole tree. This means that the actual
     * split mechanics are located in subclasses. Some of the subclass
     * trySplit methods are identical (except for return types), but
     * not nicely factorable.
     *
     * Currently, subclass versions exist only for the full map
     * (including descending keys via its descendingMap).  Others are
     * possible but currently not worthwhile because submaps require
     * O(n) computations to determine size, which substantially limits
     * potential speed-ups of using custom Spliterators versus default
     * mechanics.
     *
     * To boostrap initialization, external constructors use
     * negative size estimates: -1 for ascend, -2 for descend.
     */
    static class TreeMapSpliterator<K,V> {
        final TreeMap<K,V> tree;
        TreeMap.Entry<K,V> current; // traverser; initially first node in range
        TreeMap.Entry<K,V> fence;   // one past last, or null
        int side;                   // 0: top, -1: is a left split, +1: right
        int est;                    // size estimate (exact only for top-level)
        int expectedModCount;       // for CME checks

        TreeMapSpliterator(TreeMap<K,V> tree,
                           TreeMap.Entry<K,V> origin, TreeMap.Entry<K,V> fence,
                           int side, int est, int expectedModCount) {
            this.tree = tree;
            this.current = origin;
            this.fence = fence;
            this.side = side;
            this.est = est;
            this.expectedModCount = expectedModCount;
        }

        final int getEstimate() { // force initialization
            int s; TreeMap<K,V> t;
            if ((s = est) < 0) {
                if ((t = tree) != null) {
                    current = (s == -1) ? t.getFirstEntry() : t.getLastEntry();
                    s = est = t.size;
                    expectedModCount = t.modCount;
                }
                else
                    s = est = 0;
            }
            return s;
        }

        public final long estimateSize() {
            return (long)getEstimate();
        }
    }

    static final class KeySpliterator<K,V>
        extends TreeMapSpliterator<K,V>
        implements Spliterator<K> {
        KeySpliterator(TreeMap<K,V> tree,
                       TreeMap.Entry<K,V> origin, TreeMap.Entry<K,V> fence,
                       int side, int est, int expectedModCount) {
            super(tree, origin, fence, side, est, expectedModCount);
        }

        public KeySpliterator<K,V> trySplit() {
            if (est < 0)
                getEstimate(); // force initialization
            int d = side;
            TreeMap.Entry<K,V> e = current, f = fence,
                s = ((e == null || e == f) ? null :      // empty
                     (d == 0)              ? tree.root : // was top
                     (d >  0)              ? e.right :   // was right
                     (d <  0 && f != null) ? f.left :    // was left
                     null);
            if (s != null && s != e && s != f &&
                tree.compare(e.key, s.key) < 0) {        // e not already past s
                side = 1;
                return new KeySpliterator<>
                    (tree, e, current = s, -1, est >>>= 1, expectedModCount);
            }
            return null;
        }

        public void forEachRemaining(Consumer<? super K> action) {
            if (action == null)
                throw new NullPointerException();
            if (est < 0)
                getEstimate(); // force initialization
            TreeMap.Entry<K,V> f = fence, e, p, pl;
            if ((e = current) != null && e != f) {
                current = f; // exhaust
                do {
                    action.accept(e.key);
                    if ((p = e.right) != null) {
                        while ((pl = p.left) != null)
                            p = pl;
                    }
                    else {
                        while ((p = e.parent) != null && e == p.right)
                            e = p;
                    }
                } while ((e = p) != null && e != f);
                if (tree.modCount != expectedModCount)
                    throw new ConcurrentModificationException();
            }
        }

        public boolean tryAdvance(Consumer<? super K> action) {
            TreeMap.Entry<K,V> e;
            if (action == null)
                throw new NullPointerException();
            if (est < 0)
                getEstimate(); // force initialization
            if ((e = current) == null || e == fence)
                return false;
            current = successor(e);
            action.accept(e.key);
            if (tree.modCount != expectedModCount)
                throw new ConcurrentModificationException();
            return true;
        }

        public int characteristics() {
            return (side == 0 ? Spliterator.SIZED : 0) |
                Spliterator.DISTINCT | Spliterator.SORTED | Spliterator.ORDERED;
        }

        public final Comparator<? super K>  getComparator() {
            return tree.comparator;
        }

    }

    static final class DescendingKeySpliterator<K,V>
        extends TreeMapSpliterator<K,V>
        implements Spliterator<K> {
        DescendingKeySpliterator(TreeMap<K,V> tree,
                                 TreeMap.Entry<K,V> origin, TreeMap.Entry<K,V> fence,
                                 int side, int est, int expectedModCount) {
            super(tree, origin, fence, side, est, expectedModCount);
        }

        public DescendingKeySpliterator<K,V> trySplit() {
            if (est < 0)
                getEstimate(); // force initialization
            int d = side;
            TreeMap.Entry<K,V> e = current, f = fence,
                    s = ((e == null || e == f) ? null :      // empty
                         (d == 0)              ? tree.root : // was top
                         (d <  0)              ? e.left :    // was left
                         (d >  0 && f != null) ? f.right :   // was right
                         null);
            if (s != null && s != e && s != f &&
                tree.compare(e.key, s.key) > 0) {       // e not already past s
                side = 1;
                return new DescendingKeySpliterator<>
                        (tree, e, current = s, -1, est >>>= 1, expectedModCount);
            }
            return null;
        }

        public void forEachRemaining(Consumer<? super K> action) {
            if (action == null)
                throw new NullPointerException();
            if (est < 0)
                getEstimate(); // force initialization
            TreeMap.Entry<K,V> f = fence, e, p, pr;
            if ((e = current) != null && e != f) {
                current = f; // exhaust
                do {
                    action.accept(e.key);
                    if ((p = e.left) != null) {
                        while ((pr = p.right) != null)
                            p = pr;
                    }
                    else {
                        while ((p = e.parent) != null && e == p.left)
                            e = p;
                    }
                } while ((e = p) != null && e != f);
                if (tree.modCount != expectedModCount)
                    throw new ConcurrentModificationException();
            }
        }

        public boolean tryAdvance(Consumer<? super K> action) {
            TreeMap.Entry<K,V> e;
            if (action == null)
                throw new NullPointerException();
            if (est < 0)
                getEstimate(); // force initialization
            if ((e = current) == null || e == fence)
                return false;
            current = predecessor(e);
            action.accept(e.key);
            if (tree.modCount != expectedModCount)
                throw new ConcurrentModificationException();
            return true;
        }

        public int characteristics() {
            return (side == 0 ? Spliterator.SIZED : 0) |
                Spliterator.DISTINCT | Spliterator.ORDERED;
        }
    }

    static final class ValueSpliterator<K,V>
            extends TreeMapSpliterator<K,V>
            implements Spliterator<V> {
        ValueSpliterator(TreeMap<K,V> tree,
                         TreeMap.Entry<K,V> origin, TreeMap.Entry<K,V> fence,
                         int side, int est, int expectedModCount) {
            super(tree, origin, fence, side, est, expectedModCount);
        }

        public ValueSpliterator<K,V> trySplit() {
            if (est < 0)
                getEstimate(); // force initialization
            int d = side;
            TreeMap.Entry<K,V> e = current, f = fence,
                    s = ((e == null || e == f) ? null :      // empty
                         (d == 0)              ? tree.root : // was top
                         (d >  0)              ? e.right :   // was right
                         (d <  0 && f != null) ? f.left :    // was left
                         null);
            if (s != null && s != e && s != f &&
                tree.compare(e.key, s.key) < 0) {        // e not already past s
                side = 1;
                return new ValueSpliterator<>
                        (tree, e, current = s, -1, est >>>= 1, expectedModCount);
            }
            return null;
        }

        public void forEachRemaining(Consumer<? super V> action) {
            if (action == null)
                throw new NullPointerException();
            if (est < 0)
                getEstimate(); // force initialization
            TreeMap.Entry<K,V> f = fence, e, p, pl;
            if ((e = current) != null && e != f) {
                current = f; // exhaust
                do {
                    action.accept(e.value);
                    if ((p = e.right) != null) {
                        while ((pl = p.left) != null)
                            p = pl;
                    }
                    else {
                        while ((p = e.parent) != null && e == p.right)
                            e = p;
                    }
                } while ((e = p) != null && e != f);
                if (tree.modCount != expectedModCount)
                    throw new ConcurrentModificationException();
            }
        }

        public boolean tryAdvance(Consumer<? super V> action) {
            TreeMap.Entry<K,V> e;
            if (action == null)
                throw new NullPointerException();
            if (est < 0)
                getEstimate(); // force initialization
            if ((e = current) == null || e == fence)
                return false;
            current = successor(e);
            action.accept(e.value);
            if (tree.modCount != expectedModCount)
                throw new ConcurrentModificationException();
            return true;
        }

        public int characteristics() {
            return (side == 0 ? Spliterator.SIZED : 0) | Spliterator.ORDERED;
        }
    }

    static final class EntrySpliterator<K,V>
        extends TreeMapSpliterator<K,V>
        implements Spliterator<Map.Entry<K,V>> {
        EntrySpliterator(TreeMap<K,V> tree,
                         TreeMap.Entry<K,V> origin, TreeMap.Entry<K,V> fence,
                         int side, int est, int expectedModCount) {
            super(tree, origin, fence, side, est, expectedModCount);
        }

        public EntrySpliterator<K,V> trySplit() {
            if (est < 0)
                getEstimate(); // force initialization
            int d = side;
            TreeMap.Entry<K,V> e = current, f = fence,
                    s = ((e == null || e == f) ? null :      // empty
                         (d == 0)              ? tree.root : // was top
                         (d >  0)              ? e.right :   // was right
                         (d <  0 && f != null) ? f.left :    // was left
                         null);
            if (s != null && s != e && s != f &&
                tree.compare(e.key, s.key) < 0) {        // e not already past s
                side = 1;
                return new EntrySpliterator<>
                        (tree, e, current = s, -1, est >>>= 1, expectedModCount);
            }
            return null;
        }

        public void forEachRemaining(Consumer<? super Map.Entry<K, V>> action) {
            if (action == null)
                throw new NullPointerException();
            if (est < 0)
                getEstimate(); // force initialization
            TreeMap.Entry<K,V> f = fence, e, p, pl;
            if ((e = current) != null && e != f) {
                current = f; // exhaust
                do {
                    action.accept(e);
                    if ((p = e.right) != null) {
                        while ((pl = p.left) != null)
                            p = pl;
                    }
                    else {
                        while ((p = e.parent) != null && e == p.right)
                            e = p;
                    }
                } while ((e = p) != null && e != f);
                if (tree.modCount != expectedModCount)
                    throw new ConcurrentModificationException();
            }
        }

        public boolean tryAdvance(Consumer<? super Map.Entry<K,V>> action) {
            TreeMap.Entry<K,V> e;
            if (action == null)
                throw new NullPointerException();
            if (est < 0)
                getEstimate(); // force initialization
            if ((e = current) == null || e == fence)
                return false;
            current = successor(e);
            action.accept(e);
            if (tree.modCount != expectedModCount)
                throw new ConcurrentModificationException();
            return true;
        }

        public int characteristics() {
            return (side == 0 ? Spliterator.SIZED : 0) |
                    Spliterator.DISTINCT | Spliterator.SORTED | Spliterator.ORDERED;
        }

        @Override
        public Comparator<Map.Entry<K, V>> getComparator() {
            // Adapt or create a key-based comparator
            if (tree.comparator != null) {
                return Map.Entry.comparingByKey(tree.comparator);
            }
            else {
                return (Comparator<Map.Entry<K, V>> & Serializable) (e1, e2) -> {
                    @SuppressWarnings("unchecked")
                    Comparable<? super K> k1 = (Comparable<? super K>) e1.getKey();
                    return k1.compareTo(e2.getKey());
                };
            }
        }
    }
}
