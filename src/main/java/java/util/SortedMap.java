/*
 * Copyright (c) 1998, 2011, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package java.util;

/**
 * 20210609
 * A. 一个{@link Map}进一步提供其键的总排序。Map根据其键的{@linkplain Comparable natural ordering}进行排序，或者通常由在SortedMap创建时提供的
 *    {@link Comparator} 进行排序。 当迭代已排序映射的集合视图（由 {@code entrySet}、{@code keySet} 和 {@code values} 方法返回）时，会反映此顺序。
 *    提供了几个额外的操作来利用排序。 （这个接口是 {@link SortedSet} 的Map模拟。）
 * B. 插入排序映射的所有键都必须实现{@code Comparable}接口（或被指定的比较器接受）。 此外，所有这些键必须相互比较：
 *    {@code k1.compareTo(k2)}（或 {@code comparer.compare(k1, k2)}）不得为任何键抛出 {@code ClassCastException} {@code k1} 和 {@code k2} 在排序映射中。
 *    尝试违反此限制将导致违规方法或构造函数调用抛出 {@code ClassCastException}。
 * C. 请注意，如果排序映射要正确实现 {@code Map} 接口，则排序映射维护的排序（无论是否提供显式比较器）必须与 equals 一致。
 *    （请参阅 {@code Comparable} 接口或 {@code Comparator} 接口以获取与 equals 一致的精确定义。）这是因为 {@code Map} 接口是根据 {@code equals} 操作定义的，
 *    但是排序映射使用其 {@code compareTo}（或 {@code compare}）方法执行所有键比较，因此从排序映射的角度来看，此方法认为相等的两个键是相等的。
 *    TreeMap的行为是明确定义的，即使它的排序与 equals 不一致； 它只是不遵守 {@code Map} 接口的一般约定。
 * D. 所有通用排序映射实现类都应该提供四个“标准”构造函数。 尽管无法通过接口指定必需的构造函数，但不可能强制执行此建议。 所有排序映射实现的预期“标准”构造函数是：
 *    1) 一个空（无参数）构造函数，它创建一个空的排序映射，根据其键的自然顺序排序。
 *    2) 一个带有{@code Comparator} 类型参数的构造函数，它创建一个空的排序映射，根据指定的比较器排序。
 *    3) 具有 {@code Map} 类型的单个参数的构造函数，它创建一个新映射，其键值映射与其参数相同，根据键的自然顺序排序。
 *    4) 具有 {@code SortedMap} 类型的单个参数的构造函数，它创建一个新的排序映射，其键值映射和排序与输入排序映射相同。
 * E. 注意：几种方法返回具有受限键范围的子图。 这些范围是半开的，也就是说，它们包括它们的低端点，但不包括它们的高端点（如果适用）。 如果您需要一个封闭的范围（包括两个端点），
 *    并且键类型允许计算给定键的后继，只需请求从 {@code lowEndpoint} 到 {@code successor(highEndpoint)} 的子范围。 例如，假设 {@code m} 是一个键为字符串的映射。
 *    以下习语获取包含 {@code m} 中所有键值映射的视图，其键在 {@code low} 和 {@code high} 之间，包括：
 *    SortedMap<String, V> sub = m.subMap(low, high+"\0");
 * F. 可以使用类似的技术来生成开放范围（既不包含端点）。 以下习语获得包含 {@code m} 中所有键值映射的视图，其键在 {@code low} 和 {@code high} 之间，不包括：
 *    SortedMap<String, V> sub = m.subMap(low+"\0", high);
 * G. {@docRoot}/../technotes/guides/collections/index.html
 */
/**
 * A.
 * A {@link Map} that further provides a <em>total ordering</em> on its keys.
 * The map is ordered according to the {@linkplain Comparable natural
 * ordering} of its keys, or by a {@link Comparator} typically
 * provided at sorted map creation time.  This order is reflected when
 * iterating over the sorted map's collection views (returned by the
 * {@code entrySet}, {@code keySet} and {@code values} methods).
 * Several additional operations are provided to take advantage of the
 * ordering.  (This interface is the map analogue of {@link SortedSet}.)
 *
 * B.
 * <p>All keys inserted into a sorted map must implement the {@code Comparable}
 * interface (or be accepted by the specified comparator).  Furthermore, all
 * such keys must be <em>mutually comparable</em>: {@code k1.compareTo(k2)} (or
 * {@code comparator.compare(k1, k2)}) must not throw a
 * {@code ClassCastException} for any keys {@code k1} and {@code k2} in
 * the sorted map.  Attempts to violate this restriction will cause the
 * offending method or constructor invocation to throw a
 * {@code ClassCastException}.
 *
 * C.
 * <p>Note that the ordering maintained by a sorted map (whether or not an
 * explicit comparator is provided) must be <em>consistent with equals</em> if
 * the sorted map is to correctly implement the {@code Map} interface.  (See
 * the {@code Comparable} interface or {@code Comparator} interface for a
 * precise definition of <em>consistent with equals</em>.)  This is so because
 * the {@code Map} interface is defined in terms of the {@code equals}
 * operation, but a sorted map performs all key comparisons using its
 * {@code compareTo} (or {@code compare}) method, so two keys that are
 * deemed equal by this method are, from the standpoint of the sorted map,
 * equal.  The behavior of a tree map <em>is</em> well-defined even if its
 * ordering is inconsistent with equals; it just fails to obey the general
 * contract of the {@code Map} interface.
 *
 * D.
 * <p>All general-purpose sorted map implementation classes should provide four
 * "standard" constructors. It is not possible to enforce this recommendation
 * though as required constructors cannot be specified by interfaces. The
 * expected "standard" constructors for all sorted map implementations are:
 * <ol>
 *   <li>A void (no arguments) constructor, which creates an empty sorted map
 *   sorted according to the natural ordering of its keys.</li>
 *   <li>A constructor with a single argument of type {@code Comparator}, which
 *   creates an empty sorted map sorted according to the specified comparator.</li>
 *   <li>A constructor with a single argument of type {@code Map}, which creates
 *   a new map with the same key-value mappings as its argument, sorted
 *   according to the keys' natural ordering.</li>
 *   <li>A constructor with a single argument of type {@code SortedMap}, which
 *   creates a new sorted map with the same key-value mappings and the same
 *   ordering as the input sorted map.</li>
 * </ol>
 *
 * E.
 * <p><strong>Note</strong>: several methods return submaps with restricted key
 * ranges. Such ranges are <em>half-open</em>, that is, they include their low
 * endpoint but not their high endpoint (where applicable).  If you need a
 * <em>closed range</em> (which includes both endpoints), and the key type
 * allows for calculation of the successor of a given key, merely request
 * the subrange from {@code lowEndpoint} to
 * {@code successor(highEndpoint)}.  For example, suppose that {@code m}
 * is a map whose keys are strings.  The following idiom obtains a view
 * containing all of the key-value mappings in {@code m} whose keys are
 * between {@code low} and {@code high}, inclusive:<pre>
 *   SortedMap&lt;String, V&gt; sub = m.subMap(low, high+"\0");</pre>
 *
 * F.
 * A similar technique can be used to generate an <em>open range</em>
 * (which contains neither endpoint).  The following idiom obtains a
 * view containing all of the key-value mappings in {@code m} whose keys
 * are between {@code low} and {@code high}, exclusive:<pre>
 *   SortedMap&lt;String, V&gt; sub = m.subMap(low+"\0", high);</pre>
 *
 * G.
 * <p>This interface is a member of the
 * <a href="{@docRoot}/../technotes/guides/collections/index.html">
 * Java Collections Framework</a>.
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 *
 * @author  Josh Bloch
 * @see Map
 * @see TreeMap
 * @see SortedSet
 * @see Comparator
 * @see Comparable
 * @see Collection
 * @see ClassCastException
 * @since 1.2
 */

public interface SortedMap<K,V> extends Map<K,V> {

    /**
     * 20210609
     * 返回用于对此映射中的键进行排序的比较器，如果此映射使用其键的 {@linkplain Comparable 自然排序}，则返回 {@code null}。
     */
    /**
     * Returns the comparator used to order the keys in this map, or
     * {@code null} if this map uses the {@linkplain Comparable
     * natural ordering} of its keys.
     *
     * @return the comparator used to order the keys in this map,
     *         or {@code null} if this map uses the natural ordering
     *         of its keys
     */
    Comparator<? super K> comparator();

    /**
     * 20210609
     * A. 返回此Map部分的视图，其键范围从 {@code fromKey}（含）到 {@code toKey}（不含）。 （如果 {@code fromKey} 和 {@code toKey} 相等，则返回的映射为空。）
     *    返回的映射由此映射支持，因此返回映射的更改会反映在此映射中，反之亦然。 返回的Map支持此Map支持的所有可选Map操作。
     * B. 返回的Map试图在其范围之外插入一个键, 将抛出一个{@code IllegalArgumentException}。
     */
    /**
     * A.
     * Returns a view of the portion of this map whose keys range from
     * {@code fromKey}, inclusive, to {@code toKey}, exclusive.  (If
     * {@code fromKey} and {@code toKey} are equal, the returned map
     * is empty.)  The returned map is backed by this map, so changes
     * in the returned map are reflected in this map, and vice-versa.
     * The returned map supports all optional map operations that this
     * map supports.
     *
     * B.
     * <p>The returned map will throw an {@code IllegalArgumentException}
     * on an attempt to insert a key outside its range.
     *
     * @param fromKey low endpoint (inclusive) of the keys in the returned map
     * @param toKey high endpoint (exclusive) of the keys in the returned map
     * @return a view of the portion of this map whose keys range from
     *         {@code fromKey}, inclusive, to {@code toKey}, exclusive
     * @throws ClassCastException if {@code fromKey} and {@code toKey}
     *         cannot be compared to one another using this map's comparator
     *         (or, if the map has no comparator, using natural ordering).
     *         Implementations may, but are not required to, throw this
     *         exception if {@code fromKey} or {@code toKey}
     *         cannot be compared to keys currently in the map.
     * @throws NullPointerException if {@code fromKey} or {@code toKey}
     *         is null and this map does not permit null keys
     * @throws IllegalArgumentException if {@code fromKey} is greater than
     *         {@code toKey}; or if this map itself has a restricted
     *         range, and {@code fromKey} or {@code toKey} lies
     *         outside the bounds of the range
     */
    SortedMap<K,V> subMap(K fromKey, K toKey);

    /**
     * 20210609
     * A. 返回此Map部分的视图，其键严格小于{@code toKey}。 返回的Map由此Map支持，因此返回的Map中的更改会反映在此Map中，反之亦然。 返回支持此Map支持的所有可选Map操作。
     * B. 返回的映射将在尝试插入其范围之外的键时抛出 {@code IllegalArgumentException}。
     */
    /**
     * A.
     * Returns a view of the portion of this map whose keys are
     * strictly less than {@code toKey}.  The returned map is backed
     * by this map, so changes in the returned map are reflected in
     * this map, and vice-versa.  The returned map supports all
     * optional map operations that this map supports.
     *
     * B.
     * <p>The returned map will throw an {@code IllegalArgumentException}
     * on an attempt to insert a key outside its range.
     *
     * @param toKey high endpoint (exclusive) of the keys in the returned map
     * @return a view of the portion of this map whose keys are strictly
     *         less than {@code toKey}
     * @throws ClassCastException if {@code toKey} is not compatible
     *         with this map's comparator (or, if the map has no comparator,
     *         if {@code toKey} does not implement {@link Comparable}).
     *         Implementations may, but are not required to, throw this
     *         exception if {@code toKey} cannot be compared to keys
     *         currently in the map.
     * @throws NullPointerException if {@code toKey} is null and
     *         this map does not permit null keys
     * @throws IllegalArgumentException if this map itself has a
     *         restricted range, and {@code toKey} lies outside the
     *         bounds of the range
     */
    SortedMap<K,V> headMap(K toKey);

    /**
     * 20210609
     * A. 返回此Map部分的视图，其键大于或等于 {@code fromKey}。 返回的Map由此Map支持，因此返回的Map中的更改会反映在此Map中，反之亦然。
     *    返回的Map支持此Map支持的所有可选Map操作。
     * B. 返回的映射将在尝试插入其范围之外的键时抛出 {@code IllegalArgumentException}。
     */
    /**
     * A.
     * Returns a view of the portion of this map whose keys are
     * greater than or equal to {@code fromKey}.  The returned map is
     * backed by this map, so changes in the returned map are
     * reflected in this map, and vice-versa.  The returned map
     * supports all optional map operations that this map supports.
     *
     * B.
     * <p>The returned map will throw an {@code IllegalArgumentException}
     * on an attempt to insert a key outside its range.
     *
     * @param fromKey low endpoint (inclusive) of the keys in the returned map
     * @return a view of the portion of this map whose keys are greater
     *         than or equal to {@code fromKey}
     * @throws ClassCastException if {@code fromKey} is not compatible
     *         with this map's comparator (or, if the map has no comparator,
     *         if {@code fromKey} does not implement {@link Comparable}).
     *         Implementations may, but are not required to, throw this
     *         exception if {@code fromKey} cannot be compared to keys
     *         currently in the map.
     * @throws NullPointerException if {@code fromKey} is null and
     *         this map does not permit null keys
     * @throws IllegalArgumentException if this map itself has a
     *         restricted range, and {@code fromKey} lies outside the
     *         bounds of the range
     */
    SortedMap<K,V> tailMap(K fromKey);

    /**
     * 20210609
     * 返回当前在此映射中的第一个（最低）键。
     */
    /**
     * Returns the first (lowest) key currently in this map.
     *
     * @return the first (lowest) key currently in this map
     * @throws NoSuchElementException if this map is empty
     */
    K firstKey();

    /**
     * 20210609
     * 返回此映射中当前的最后一个（最高）键。
     */
    /**
     * Returns the last (highest) key currently in this map.
     *
     * @return the last (highest) key currently in this map
     * @throws NoSuchElementException if this map is empty
     */
    K lastKey();

    /**
     * 20210609
     * 返回此映射中包含的键的 {@link Set} 视图。 集合的迭代器按升序返回键。 该集合由Map支持，因此对Map的更改会反映在该集合中，反之亦然。
     * 如果在对集合进行迭代时修改了Map（除非通过迭代器自己的 {@code remove} 操作），迭代的结果是不确定的。
     * 该集合支持元素移除，通过{@code Iterator.remove}、{@code Set.remove}、{@code removeAll}、{@code retainAll} 和{@code clear 从Map中移除相应的映射 } 操作。
     * 它不支持 {@code add} 或 {@code addAll} 操作。
     */
    /**
     * Returns a {@link Set} view of the keys contained in this map.
     * The set's iterator returns the keys in ascending order.
     * The set is backed by the map, so changes to the map are
     * reflected in the set, and vice-versa.  If the map is modified
     * while an iteration over the set is in progress (except through
     * the iterator's own {@code remove} operation), the results of
     * the iteration are undefined.  The set supports element removal,
     * which removes the corresponding mapping from the map, via the
     * {@code Iterator.remove}, {@code Set.remove},
     * {@code removeAll}, {@code retainAll}, and {@code clear}
     * operations.  It does not support the {@code add} or {@code addAll}
     * operations.
     *
     * @return a set view of the keys contained in this map, sorted in
     *         ascending order
     */
    Set<K> keySet();

    /**
     * 20210609
     * 返回此Map中包含的值的 {@link Collection} 视图。 集合的迭代器按相应键的升序返回值。 集合由地图支持，因此对Map的更改会反映在集合中，反之亦然。
     * 如果在对集合进行迭代时修改了映射（通过迭代器自己的 {@code remove} 操作除外），则迭代的结果是不确定的。 集合支持元素移除，
     * 通过 {@code Iterator.remove}、{@code Collection.remove}、{@code removeAll}、{@code retainAll} 和 {@code clear} 从Map中移除对应的映射 操作。
     * 它不支持 {@code add} 或 {@code addAll} 操作。
     */
    /**
     * Returns a {@link Collection} view of the values contained in this map.
     * The collection's iterator returns the values in ascending order
     * of the corresponding keys.
     * The collection is backed by the map, so changes to the map are
     * reflected in the collection, and vice-versa.  If the map is
     * modified while an iteration over the collection is in progress
     * (except through the iterator's own {@code remove} operation),
     * the results of the iteration are undefined.  The collection
     * supports element removal, which removes the corresponding
     * mapping from the map, via the {@code Iterator.remove},
     * {@code Collection.remove}, {@code removeAll},
     * {@code retainAll} and {@code clear} operations.  It does not
     * support the {@code add} or {@code addAll} operations.
     *
     * @return a collection view of the values contained in this map,
     *         sorted in ascending key order
     */
    Collection<V> values();

    /**
     * 20210609
     * 返回此Map中包含的映射的 {@link Set} 视图。 集合的迭代器以键升序返回条目。 该集合由Map支持，因此对Map的更改会反映在该集合中，反之亦然。
     * 如果在对集合进行迭代时修改了映射（除了通过迭代器自己的 {@code remove} 操作，或通过迭代器返回的映射条目上的 {@code setValue} 操作）迭代的结果 未定义。
     * 该集合支持元素移除，即通过 {@code Iterator.remove}、{@code Set.remove}、{@code removeAll}、{@code retainAll} 和 {@code clear} 从Map中移除相应的映射操作。
     * 它不支持 {@code add} 或 {@code addAll} 操作。
     */
    /**
     * Returns a {@link Set} view of the mappings contained in this map.
     * The set's iterator returns the entries in ascending key order.
     * The set is backed by the map, so changes to the map are
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
     *
     * @return a set view of the mappings contained in this map,
     *         sorted in ascending key order
     */
    Set<Map.Entry<K, V>> entrySet();
}
