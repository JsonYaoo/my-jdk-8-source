/*
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

/*
 * Written by Doug Lea and Josh Bloch with assistance from members of JCP
 * JSR-166 Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util;

/**
 * 20210609
 * A. {@link SortedMap} 扩展了导航方法，返回给定搜索目标的最接近匹配项。
 *    方法{@code lowerEntry}、{@code floorEntry}、{@code ceilingEntry} 和{@code HigherEntry}
 *    分别返回与小于、小于或等于、大于或等于键关联的{@code Map.Entry} 对象 , 并且大于给定的键，如果没有这样的键，则返回 {@code null}。
 *    类似地，方法{@code lowerKey}、{@code floorKey}、{@code purgeKey} 和{@code HigherKey} 仅返回关联的键。 所有这些方法都是为定位而不是遍历条目而设计的。
 * B. {@code NavigableMap} 可以按升序或降序键顺序访问和遍历。 {@code DescingMap} 方法返回Map的视图，其中所有关系和方向方法的含义都颠倒了。
 *    升序操作和视图的性能可能比降序操作更快。 方法 {@code subMap}、{@code headMap} 和 {@code tailMap} 与类似名称的 {@code SortedMap} 方法不同，
 *    它接受描述上下限是包含还是不包含的附加参数。 任何 {@code NavigableMap} 的子图都必须实现 {@code NavigableMap} 接口。
 * C. 此接口还定义了方法 {@code firstEntry}、{@code pollFirstEntry}、{@code lastEntry} 和 {@code pollLastEntry}，
 *    它们返回与/或删除最小和最大映射（如果存在），否则返回 {@code 空值}。
 * D. 条目返回方法的实现应返回 {@code Map.Entry} 对，表示它们生成时的映射快照，因此通常不支持可选的 {@code Entry.setValue} 方法。
 *    但是请注意，可以使用 {@code put} 方法更改关联映射中的映射。
 * E. 方法{@link #subMap(Object, Object) subMap(K, K)}、{@link #headMap(Object) headMap(K)} 和 {@link #tailMap(Object) tailMap(K)}
 *    被指定为返回 {@code SortedMap}以允许兼容地改造 {@code SortedMap} 的现有实现以实现 {@code NavigableMap}，但鼓励此接口的扩展和实现覆盖这些方法以返回
 *    {@code NavigableMap}。 类似地，可以覆盖 {@link #keySet()} 以返回 {@code NavigableSet}。
 * F. {@docRoot}/../technotes/guides/collections/index.html
 */

/**
 * A.
 * A {@link SortedMap} extended with navigation methods returning the
 * closest matches for given search targets. Methods
 * {@code lowerEntry}, {@code floorEntry}, {@code ceilingEntry},
 * and {@code higherEntry} return {@code Map.Entry} objects
 * associated with keys respectively less than, less than or equal,
 * greater than or equal, and greater than a given key, returning
 * {@code null} if there is no such key.  Similarly, methods
 * {@code lowerKey}, {@code floorKey}, {@code ceilingKey}, and
 * {@code higherKey} return only the associated keys. All of these
 * methods are designed for locating, not traversing entries.
 *
 * B.
 * <p>A {@code NavigableMap} may be accessed and traversed in either
 * ascending or descending key order.  The {@code descendingMap}
 * method returns a view of the map with the senses of all relational
 * and directional methods inverted. The performance of ascending
 * operations and views is likely to be faster than that of descending
 * ones.  Methods {@code subMap}, {@code headMap},
 * and {@code tailMap} differ from the like-named {@code
 * SortedMap} methods in accepting additional arguments describing
 * whether lower and upper bounds are inclusive versus exclusive.
 * Submaps of any {@code NavigableMap} must implement the {@code
 * NavigableMap} interface.
 *
 * C.
 * <p>This interface additionally defines methods {@code firstEntry},
 * {@code pollFirstEntry}, {@code lastEntry}, and
 * {@code pollLastEntry} that return and/or remove the least and
 * greatest mappings, if any exist, else returning {@code null}.
 *
 * D.
 * <p>Implementations of entry-returning methods are expected to
 * return {@code Map.Entry} pairs representing snapshots of mappings
 * at the time they were produced, and thus generally do <em>not</em>
 * support the optional {@code Entry.setValue} method. Note however
 * that it is possible to change mappings in the associated map using
 * method {@code put}.
 *
 * E.
 * <p>Methods
 * {@link #subMap(Object, Object) subMap(K, K)},
 * {@link #headMap(Object) headMap(K)}, and
 * {@link #tailMap(Object) tailMap(K)}
 * are specified to return {@code SortedMap} to allow existing
 * implementations of {@code SortedMap} to be compatibly retrofitted to
 * implement {@code NavigableMap}, but extensions and implementations
 * of this interface are encouraged to override these methods to return
 * {@code NavigableMap}.  Similarly,
 * {@link #keySet()} can be overriden to return {@code NavigableSet}.
 *
 * F.
 * <p>This interface is a member of the
 * <a href="{@docRoot}/../technotes/guides/collections/index.html">
 * Java Collections Framework</a>.
 *
 * @author Doug Lea
 * @author Josh Bloch
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 * @since 1.6
 */
public interface NavigableMap<K,V> extends SortedMap<K,V> {

    /**
     * 20210609
     * 返回与严格小于给定键的最大键关联的键值映射，如果没有这样的键，则返回 {@code null}。
     */
    /**
     * Returns a key-value mapping associated with the greatest key
     * strictly less than the given key, or {@code null} if there is
     * no such key.
     *
     * @param key the key
     * @return an entry with the greatest key less than {@code key},
     *         or {@code null} if there is no such key
     * @throws ClassCastException if the specified key cannot be compared
     *         with the keys currently in the map
     * @throws NullPointerException if the specified key is null
     *         and this map does not permit null keys
     */
    Map.Entry<K,V> lowerEntry(K key);

    /**
     * 20210609
     * 返回严格小于给定键的最大键，如果没有这样的键，则返回 {@code null}。
     */
    /**
     * Returns the greatest key strictly less than the given key, or
     * {@code null} if there is no such key.
     *
     * @param key the key
     * @return the greatest key less than {@code key},
     *         or {@code null} if there is no such key
     * @throws ClassCastException if the specified key cannot be compared
     *         with the keys currently in the map
     * @throws NullPointerException if the specified key is null
     *         and this map does not permit null keys
     */
    K lowerKey(K key);

    /**
     * 20210609
     * 返回与小于或等于给定键的最大键关联的键值映射，如果没有这样的键，则返回 {@code null}。
     */
    /**
     * Returns a key-value mapping associated with the greatest key
     * less than or equal to the given key, or {@code null} if there
     * is no such key.
     *
     * @param key the key
     * @return an entry with the greatest key less than or equal to
     *         {@code key}, or {@code null} if there is no such key
     * @throws ClassCastException if the specified key cannot be compared
     *         with the keys currently in the map
     * @throws NullPointerException if the specified key is null
     *         and this map does not permit null keys
     */
    Map.Entry<K,V> floorEntry(K key);

    /**
     * 20210609
     * 返回小于或等于给定键的最大键，如果没有这样的键，则返回 {@code null}。
     */
    /**
     * Returns the greatest key less than or equal to the given key,
     * or {@code null} if there is no such key.
     *
     * @param key the key
     * @return the greatest key less than or equal to {@code key},
     *         or {@code null} if there is no such key
     * @throws ClassCastException if the specified key cannot be compared
     *         with the keys currently in the map
     * @throws NullPointerException if the specified key is null
     *         and this map does not permit null keys
     */
    K floorKey(K key);

    /**
     * 20210609
     * 返回与大于或等于给定键的最小键关联的键值映射，如果没有这样的键，则返回 {@code null}。
     */
    /**
     * Returns a key-value mapping associated with the least key
     * greater than or equal to the given key, or {@code null} if
     * there is no such key.
     *
     * @param key the key
     * @return an entry with the least key greater than or equal to
     *         {@code key}, or {@code null} if there is no such key
     * @throws ClassCastException if the specified key cannot be compared
     *         with the keys currently in the map
     * @throws NullPointerException if the specified key is null
     *         and this map does not permit null keys
     */
    Map.Entry<K,V> ceilingEntry(K key);

    /**
     * 20210609
     * 返回大于或等于给定键的最小键，如果没有这样的键，则返回 {@code null}。
     */
    /**
     * Returns the least key greater than or equal to the given key,
     * or {@code null} if there is no such key.
     *
     * @param key the key
     * @return the least key greater than or equal to {@code key},
     *         or {@code null} if there is no such key
     * @throws ClassCastException if the specified key cannot be compared
     *         with the keys currently in the map
     * @throws NullPointerException if the specified key is null
     *         and this map does not permit null keys
     */
    K ceilingKey(K key);

    /**
     * 20210609
     * 返回与严格大于给定键的最小键关联的键值映射，如果没有这样的键，则返回 {@code null}。
     */
    /**
     * Returns a key-value mapping associated with the least key
     * strictly greater than the given key, or {@code null} if there
     * is no such key.
     *
     * @param key the key
     * @return an entry with the least key greater than {@code key},
     *         or {@code null} if there is no such key
     * @throws ClassCastException if the specified key cannot be compared
     *         with the keys currently in the map
     * @throws NullPointerException if the specified key is null
     *         and this map does not permit null keys
     */
    Map.Entry<K,V> higherEntry(K key);

    /**
     * 20210609
     * 返回严格大于给定键的最小键，如果没有这样的键，则返回 {@code null}。
     */
    /**
     * Returns the least key strictly greater than the given key, or
     * {@code null} if there is no such key.
     *
     * @param key the key
     * @return the least key greater than {@code key},
     *         or {@code null} if there is no such key
     * @throws ClassCastException if the specified key cannot be compared
     *         with the keys currently in the map
     * @throws NullPointerException if the specified key is null
     *         and this map does not permit null keys
     */
    K higherKey(K key);

    /**
     * 20210609
     * 返回与此映射中最小键关联的键值映射，如果映射为空，则返回 {@code null}。
     */
    /**
     * Returns a key-value mapping associated with the least
     * key in this map, or {@code null} if the map is empty.
     *
     * @return an entry with the least key,
     *         or {@code null} if this map is empty
     */
    Map.Entry<K,V> firstEntry();

    /**
     * 20210609
     * 返回与此映射中最大键关联的键值映射，如果映射为空，则返回 {@code null}。
     */
    /**
     * Returns a key-value mapping associated with the greatest
     * key in this map, or {@code null} if the map is empty.
     *
     * @return an entry with the greatest key,
     *         or {@code null} if this map is empty
     */
    Map.Entry<K,V> lastEntry();

    /**
     * 20210609
     * 删除并返回与此映射中最小键关联的键值映射，如果映射为空，则返回 {@code null}。
     */
    /**
     * Removes and returns a key-value mapping associated with
     * the least key in this map, or {@code null} if the map is empty.
     *
     * @return the removed first entry of this map,
     *         or {@code null} if this map is empty
     */
    Map.Entry<K,V> pollFirstEntry();

    /**
     * 20210609
     * 删除并返回与此映射中最大键关联的键值映射，如果映射为空，则返回 {@code null}。
     */
    /**
     * Removes and returns a key-value mapping associated with
     * the greatest key in this map, or {@code null} if the map is empty.
     *
     * @return the removed last entry of this map,
     *         or {@code null} if this map is empty
     */
    Map.Entry<K,V> pollLastEntry();

    /**
     * 20210609
     * A. 返回此映射中包含的映射的逆序视图。 降序映射由该映射支持，因此对映射的更改反映在降序映射中，反之亦然。 如果在对任一映射的集合视图进行迭代时修改任一映射
     *   （通过迭代器自己的 {@code remove} 操作除外），则迭代结果未定义。
     * B. 返回的映射具有等效于 {@link Collections#reverseOrder(Comparator) Collections.reverseOrder}(comparator()) 的排序。
     *    表达式 {@code m.descendingMap().descendingMap()} 返回 {@code m} 的视图，本质上等同于 {@code m}。
     */
    /**
     * A.
     * Returns a reverse order view of the mappings contained in this map.
     * The descending map is backed by this map, so changes to the map are
     * reflected in the descending map, and vice-versa.  If either map is
     * modified while an iteration over a collection view of either map
     * is in progress (except through the iterator's own {@code remove}
     * operation), the results of the iteration are undefined.
     *
     * B.
     * <p>The returned map has an ordering equivalent to
     * <tt>{@link Collections#reverseOrder(Comparator) Collections.reverseOrder}(comparator())</tt>.
     * The expression {@code m.descendingMap().descendingMap()} returns a
     * view of {@code m} essentially equivalent to {@code m}.
     *
     * @return a reverse order view of this map
     */
    NavigableMap<K,V> descendingMap();

    /**
     * 20210609
     * 返回此Map中包含的键的 {@link NavigableSet} 视图。 集合的迭代器按升序返回键。 该集合由Map支持，因此对Map的更改会反映在该集合中，反之亦然。
     * 如果在对集合进行迭代时修改了Map（除非通过迭代器自己的 {@code remove} 操作），迭代的结果是不确定的。 该集合支持元素移除，通过{@code Iterator.remove}、
     * {@code Set.remove}、{@code removeAll}、{@code retainAll} 和{@code clear 从Map中移除相应的映射 } 操作。
     * 它不支持 {@code add} 或 {@code addAll} 操作。
     */
    /**
     * Returns a {@link NavigableSet} view of the keys contained in this map.
     * The set's iterator returns the keys in ascending order.
     * The set is backed by the map, so changes to the map are reflected in
     * the set, and vice-versa.  If the map is modified while an iteration
     * over the set is in progress (except through the iterator's own {@code
     * remove} operation), the results of the iteration are undefined.  The
     * set supports element removal, which removes the corresponding mapping
     * from the map, via the {@code Iterator.remove}, {@code Set.remove},
     * {@code removeAll}, {@code retainAll}, and {@code clear} operations.
     * It does not support the {@code add} or {@code addAll} operations.
     *
     * @return a navigable set view of the keys in this map
     */
    NavigableSet<K> navigableKeySet();

    /**
     * 20210609
     * 返回此映射中包含的键的逆序 {@link NavigableSet} 视图。 集合的迭代器按降序返回键。 该集合由Map支持，因此对Map的更改会反映在该集合中，反之亦然。
     * 如果在对集合进行迭代时修改了Map（除非通过迭代器自己的 {@code remove} 操作），迭代的结果是不确定的。
     * 该集合支持元素移除，通过{@code Iterator.remove}、{@code Set.remove}、{@code removeAll}、{@code retainAll} 和{@code clear 从Map中移除相应的映射 } 操作。
     * 它不支持 {@code add} 或 {@code addAll} 操作。
     */
    /**
     * Returns a reverse order {@link NavigableSet} view of the keys contained in this map.
     * The set's iterator returns the keys in descending order.
     * The set is backed by the map, so changes to the map are reflected in
     * the set, and vice-versa.  If the map is modified while an iteration
     * over the set is in progress (except through the iterator's own {@code
     * remove} operation), the results of the iteration are undefined.  The
     * set supports element removal, which removes the corresponding mapping
     * from the map, via the {@code Iterator.remove}, {@code Set.remove},
     * {@code removeAll}, {@code retainAll}, and {@code clear} operations.
     * It does not support the {@code add} or {@code addAll} operations.
     *
     * @return a reverse order navigable set view of the keys in this map
     */
    NavigableSet<K> descendingKeySet();

    /**
     * 20210609
     * A. 返回此Map部分的视图，其键范围从 {@code fromKey} 到 {@code toKey}。 如果 {@code fromKey} 和 {@code toKey} 相等，则返回的映射为空，除非
     *    {@code fromInclusive} 和 {@code toInclusive} 都为真。 返回的Map由此Map支持，因此返回的Map中的更改会反映在此Map中，反之亦然。
     *    返回的Map支持此Map支持的所有可选Map操作。
     * B. 返回的映射将抛出 {@code IllegalArgumentException} 尝试插入其范围之外的键，或构造其端点位于其范围之外的子映射。
     */
    /**
     * A.
     * Returns a view of the portion of this map whose keys range from
     * {@code fromKey} to {@code toKey}.  If {@code fromKey} and
     * {@code toKey} are equal, the returned map is empty unless
     * {@code fromInclusive} and {@code toInclusive} are both true.  The
     * returned map is backed by this map, so changes in the returned map are
     * reflected in this map, and vice-versa.  The returned map supports all
     * optional map operations that this map supports.
     *
     * B.
     * <p>The returned map will throw an {@code IllegalArgumentException}
     * on an attempt to insert a key outside of its range, or to construct a
     * submap either of whose endpoints lie outside its range.
     *
     * 返回映射中键的低端点
     * @param fromKey low endpoint of the keys in the returned map
     *
     * {@code true} 如果低端点要包含在返回的视图中
     * @param fromInclusive {@code true} if the low endpoint
     *        is to be included in the returned view
     *
     * 返回映射中键的高端点
     * @param toKey high endpoint of the keys in the returned map
     *
     * {@code true} 如果要在返回的视图中包含高端
     * @param toInclusive {@code true} if the high endpoint
     *        is to be included in the returned view
     * @return a view of the portion of this map whose keys range from
     *         {@code fromKey} to {@code toKey}
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
    NavigableMap<K,V> subMap(K fromKey, boolean fromInclusive,
                             K toKey,   boolean toInclusive);

    /**
     * 20210609
     * A. 返回此映射部分的视图，其键小于（或等于，如果 {@code inclusive} 为真）{@code toKey}。 返回的Map由此Map支持，因此返回的Map中的更改会反映在此Map中，反之亦然。
     *    返回的Map支持此Map支持的所有可选Map操作。
     * B. 返回的映射将在尝试插入其范围之外的键时抛出 {@code IllegalArgumentException}。
     */
    /**
     * A.
     * Returns a view of the portion of this map whose keys are less than (or
     * equal to, if {@code inclusive} is true) {@code toKey}.  The returned
     * map is backed by this map, so changes in the returned map are reflected
     * in this map, and vice-versa.  The returned map supports all optional
     * map operations that this map supports.
     *
     * B.
     * <p>The returned map will throw an {@code IllegalArgumentException}
     * on an attempt to insert a key outside its range.
     *
     * @param toKey high endpoint of the keys in the returned map
     * @param inclusive {@code true} if the high endpoint
     *        is to be included in the returned view
     * @return a view of the portion of this map whose keys are less than
     *         (or equal to, if {@code inclusive} is true) {@code toKey}
     * @throws ClassCastException if {@code toKey} is not compatible
     *         with this map's comparator (or, if the map has no comparator,
     *         if {@code toKey} does not implement {@link Comparable}).
     *         Implementations may, but are not required to, throw this
     *         exception if {@code toKey} cannot be compared to keys
     *         currently in the map.
     * @throws NullPointerException if {@code toKey} is null
     *         and this map does not permit null keys
     * @throws IllegalArgumentException if this map itself has a
     *         restricted range, and {@code toKey} lies outside the
     *         bounds of the range
     */
    NavigableMap<K,V> headMap(K toKey, boolean inclusive);

    /**
     * 20210609
     * A. 返回此Map部分的视图，其键大于（或等于，如果 {@code inclusive} 为真）{@code fromKey}。 返回的Map由此Map支持，
     *    因此返回的Map中的更改会反映在此Map中，反之亦然。 返回的Map支持此Map支持的所有可选Map操作。
     * B. 返回的映射将在尝试插入其范围之外的键时抛出 {@code IllegalArgumentException}。
     */
    /**
     * A.
     * Returns a view of the portion of this map whose keys are greater than (or
     * equal to, if {@code inclusive} is true) {@code fromKey}.  The returned
     * map is backed by this map, so changes in the returned map are reflected
     * in this map, and vice-versa.  The returned map supports all optional
     * map operations that this map supports.
     *
     * B.
     * <p>The returned map will throw an {@code IllegalArgumentException}
     * on an attempt to insert a key outside its range.
     *
     * @param fromKey low endpoint of the keys in the returned map
     * @param inclusive {@code true} if the low endpoint
     *        is to be included in the returned view
     * @return a view of the portion of this map whose keys are greater than
     *         (or equal to, if {@code inclusive} is true) {@code fromKey}
     * @throws ClassCastException if {@code fromKey} is not compatible
     *         with this map's comparator (or, if the map has no comparator,
     *         if {@code fromKey} does not implement {@link Comparable}).
     *         Implementations may, but are not required to, throw this
     *         exception if {@code fromKey} cannot be compared to keys
     *         currently in the map.
     * @throws NullPointerException if {@code fromKey} is null
     *         and this map does not permit null keys
     * @throws IllegalArgumentException if this map itself has a
     *         restricted range, and {@code fromKey} lies outside the
     *         bounds of the range
     */
    NavigableMap<K,V> tailMap(K fromKey, boolean inclusive);

    /**
     * 20210609
     * 相当于 {@code subMap(fromKey, true, toKey, false)}。
     */
    /**
     * {@inheritDoc}
     *
     * <p>Equivalent to {@code subMap(fromKey, true, toKey, false)}.
     *
     * @throws ClassCastException       {@inheritDoc}
     * @throws NullPointerException     {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    SortedMap<K,V> subMap(K fromKey, K toKey);

    /**
     * 20210609
     * 相当于 {@code headMap(toKey, false)}。
     */
    /**
     * {@inheritDoc}
     *
     * <p>Equivalent to {@code headMap(toKey, false)}.
     *
     * @throws ClassCastException       {@inheritDoc}
     * @throws NullPointerException     {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    SortedMap<K,V> headMap(K toKey);

    /**
     * 20210609
     * 相当于 {@code tailMap(fromKey, true)}。
     */
    /**
     * {@inheritDoc}
     *
     * <p>Equivalent to {@code tailMap(fromKey, true)}.
     *
     * @throws ClassCastException       {@inheritDoc}
     * @throws NullPointerException     {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    SortedMap<K,V> tailMap(K fromKey);
}
