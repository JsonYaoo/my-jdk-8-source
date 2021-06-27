/*
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util.concurrent;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * 20210618
 * A. 提供线程安全和原子性保证的 {@link java.util.Map}。
 * B. 内存一致性影响：与其他并发集合一样，实现了同步的插入，在将对象作为键或值放入 {@code ConcurrentMap} 之前线程中的操作发生在从
 *    {@code ConcurrentMap} 访问或删除该对象之后的操作之前在另一个线程中。
 * C. {@docRoot}/../technotes/guides/collections/index.html
 */

/**
 * A.
 * A {@link java.util.Map} providing thread safety and atomicity
 * guarantees.
 *
 * B.
 * <p>Memory consistency effects: As with other concurrent
 * collections, actions in a thread prior to placing an object into a
 * {@code ConcurrentMap} as a key or value
 * <a href="package-summary.html#MemoryVisibility"><i>happen-before</i></a>
 * actions subsequent to the access or removal of that object from
 * the {@code ConcurrentMap} in another thread.
 *
 * C.
 * <p>This interface is a member of the
 * <a href="{@docRoot}/../technotes/guides/collections/index.html">
 * Java Collections Framework</a>.
 *
 * @since 1.5
 * @author Doug Lea
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 */
public interface ConcurrentMap<K, V> extends Map<K, V> {

    /**
     * 20210618
     * 此实现假定ConcurrentMap不能包含null值，并且{@code get()}明确返回null意味着该键不存在。支持null值的实现必须覆盖这个默认实现。
     */
    /**
     * {@inheritDoc}
     *
     * @implNote This implementation assumes that the ConcurrentMap cannot
     * contain null values and {@code get()} returning null unambiguously means
     * the key is absent. Implementations which support null values
     * <strong>must</strong> override this default implementation.
     *
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @since 1.8
     */
    @Override
    default V getOrDefault(Object key, V defaultValue) {
        V v;
        return ((v = get(key)) != null) ? v : defaultValue;
    }

    /**
     * 20210618
     * A. 对于此 {@code map}，默认实现等效于：
     *      for ((Map.Entry<K, V> entry : map.entrySet()) {
     *          action.accept(entry.getKey(), entry.getValue());
     *      }
     * B. 默认实现假定{@code getKey()}或{@code getValue()} 抛出的 {@code IllegalStateException} 表示该条目已被删除且无法处理。后续条目的操作将继续。
     */
   /**
     * {@inheritDoc}
     *
     * A.
     * @implSpec The default implementation is equivalent to, for this
     * {@code map}:
     * <pre> {@code
     * for ((Map.Entry<K, V> entry : map.entrySet())
     *     action.accept(entry.getKey(), entry.getValue());
     * }</pre>
     *
     * B.
     * @implNote The default implementation assumes that
     * {@code IllegalStateException} thrown by {@code getKey()} or
     * {@code getValue()} indicates that the entry has been removed and cannot
     * be processed. Operation continues for subsequent entries.
     *
     * @throws NullPointerException {@inheritDoc}
     * @since 1.8
     */
    @Override
    default void forEach(BiConsumer<? super K, ? super V> action) {
        Objects.requireNonNull(action);
        for (Map.Entry<K, V> entry : entrySet()) {
            K k;
            V v;
            try {
                k = entry.getKey();
                v = entry.getValue();
            } catch(IllegalStateException ise) {
                // this usually means the entry is no longer in the map.
                continue;
            }
            action.accept(k, v);
        }
    }

    /**
     * 20210618
     * A. 如果指定的键尚未与值关联，则将其与给定值关联。 这相当于:
     *      if (!map.containsKey(key))
     *          return map.put(key, value);
     *      else
     *          return map.get(key);
     *      }
     * B. 除了动作是原子地执行的。
     * C. 此实现有意重新抽象了{@code Map}中提供的不适当的默认值。
     */
    /**
     * A.
     * If the specified key is not already associated
     * with a value, associate it with the given value.
     * This is equivalent to
     *  <pre> {@code
     * if (!map.containsKey(key))
     *   return map.put(key, value);
     * else
     *   return map.get(key);
     * }</pre>
     *
     * B.
     * except that the action is performed atomically.
     *
     * C.
     * @implNote This implementation intentionally re-abstracts the
     * inappropriate default provided in {@code Map}.
     *
     * @param key key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     *
     * // 与指定键关联的前一个值，如果键没有映射，则为 {@code null}。（如果实现支持 null 值，{@code null} 返回也可以指示映射先前将 {@code null} 与键关联。）
     * @return the previous value associated with the specified key, or
     *         {@code null} if there was no mapping for the key.
     *         (A {@code null} return can also indicate that the map
     *         previously associated {@code null} with the key,
     *         if the implementation supports null values.)
     * @throws UnsupportedOperationException if the {@code put} operation
     *         is not supported by this map
     * @throws ClassCastException if the class of the specified key or value
     *         prevents it from being stored in this map
     * @throws NullPointerException if the specified key or value is null,
     *         and this map does not permit null keys or values
     * @throws IllegalArgumentException if some property of the specified key
     *         or value prevents it from being stored in this map
     */
     V putIfAbsent(K key, V value);

    /**
     * 20210618
     * A. 仅当当前映射到给定值时才删除键的条目。 这相当于:
     *      if (map.containsKey(key) && Objects.equals(map.get(key), value)) {
     *          map.remove(key);
     *          return true;
     *      } else
     *          return false;
     *      }
     * B. 除了动作是原子地执行的。
     * C. 此实现有意重新抽象了 {@code Map} 中提供的不适当的默认值。
     */
    /**
     * A.
     * Removes the entry for a key only if currently mapped to a given value.
     * This is equivalent to
     *  <pre> {@code
     * if (map.containsKey(key) && Objects.equals(map.get(key), value)) {
     *   map.remove(key);
     *   return true;
     * } else
     *   return false;
     * }</pre>
     *
     * B.
     * except that the action is performed atomically.
     *
     * C.
     * @implNote This implementation intentionally re-abstracts the
     * inappropriate default provided in {@code Map}.
     *
     * @param key key with which the specified value is associated
     * @param value value expected to be associated with the specified key
     * @return {@code true} if the value was removed
     * @throws UnsupportedOperationException if the {@code remove} operation
     *         is not supported by this map
     * @throws ClassCastException if the key or value is of an inappropriate
     *         type for this map
     *         (<a href="../Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException if the specified key or value is null,
     *         and this map does not permit null keys or values
     *         (<a href="../Collection.html#optional-restrictions">optional</a>)
     */
    boolean remove(Object key, Object value);

    /**
     * 20210618
     * A. 仅当当前映射到给定值时才替换键的条目。 这相当于
     *      if (map.containsKey(key) && Objects.equals(map.get(key), oldValue)) {
     *          map.put(key, newValue);
     *          return true;
     *      } else
     *          return false;
     *      }
     * B. 除了动作是原子地执行的。
     * C. 此实现有意重新抽象了 {@code Map} 中提供的不适当的默认值。
     */
    /**
     * A.
     * Replaces the entry for a key only if currently mapped to a given value.
     * This is equivalent to
     *  <pre> {@code
     * if (map.containsKey(key) && Objects.equals(map.get(key), oldValue)) {
     *   map.put(key, newValue);
     *   return true;
     * } else
     *   return false;
     * }</pre>
     *
     * B.
     * except that the action is performed atomically.
     *
     * C.
     * @implNote This implementation intentionally re-abstracts the
     * inappropriate default provided in {@code Map}.
     *
     * @param key key with which the specified value is associated
     * @param oldValue value expected to be associated with the specified key
     * @param newValue value to be associated with the specified key
     * @return {@code true} if the value was replaced
     * @throws UnsupportedOperationException if the {@code put} operation
     *         is not supported by this map
     * @throws ClassCastException if the class of a specified key or value
     *         prevents it from being stored in this map
     * @throws NullPointerException if a specified key or value is null,
     *         and this map does not permit null keys or values
     * @throws IllegalArgumentException if some property of a specified key
     *         or value prevents it from being stored in this map
     */
    boolean replace(K key, V oldValue, V newValue);

    /**
     * 20210618
     * A. 仅当当前映射到某个值时才替换键的条目。 这相当于
     *      if (map.containsKey(key)) {
     *          return map.put(key, value);
     *      } else
     *          return null;
     *      }
     * B. 除了动作是原子地执行的。
     * C. 此实现有意重新抽象了{@code Map}中提供的不适当的默认值。
     */
    /**
     * A.
     * Replaces the entry for a key only if currently mapped to some value.
     * This is equivalent to
     *  <pre> {@code
     * if (map.containsKey(key)) {
     *   return map.put(key, value);
     * } else
     *   return null;
     * }</pre>
     *
     * B.
     * except that the action is performed atomically.
     *
     * C.
     * @implNote This implementation intentionally re-abstracts the
     * inappropriate default provided in {@code Map}.
     *
     * @param key key with which the specified value is associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with the specified key, or
     *         {@code null} if there was no mapping for the key.
     *         (A {@code null} return can also indicate that the map
     *         previously associated {@code null} with the key,
     *         if the implementation supports null values.)
     * @throws UnsupportedOperationException if the {@code put} operation
     *         is not supported by this map
     * @throws ClassCastException if the class of the specified key or value
     *         prevents it from being stored in this map
     * @throws NullPointerException if the specified key or value is null,
     *         and this map does not permit null keys or values
     * @throws IllegalArgumentException if some property of the specified key
     *         or value prevents it from being stored in this map
     */
    V replace(K key, V value);

    /**
     * 20210618
     * A. 对于此 {@code map}，默认实现等效于：
     *      for ((Map.Entry<K, V> entry : map.entrySet())
     *          do {
     *              K k = entry.getKey();
     *              V v = entry.getValue();
     *          } while(!replace(k, v, function.apply(k, v)));
     *      }
     * B. 当多个线程尝试更新时，默认实现可能会重试这些步骤，包括可能为给定的键重复调用该函数。
     * C. 此实现假定ConcurrentMap不能包含 null 值，并且 {@code get()} 明确返回 null 意味着该键不存在。支持null值的实现必须覆盖这个默认实现。
     */
    /**
     * {@inheritDoc}
     *
     * A.
     * @implSpec
     * <p>The default implementation is equivalent to, for this {@code map}:
     * <pre> {@code
     * for ((Map.Entry<K, V> entry : map.entrySet())
     *     do {
     *        K k = entry.getKey();
     *        V v = entry.getValue();
     *     } while(!replace(k, v, function.apply(k, v)));
     * }</pre>
     *
     * B.
     * The default implementation may retry these steps when multiple
     * threads attempt updates including potentially calling the function
     * repeatedly for a given key.
     *
     * C.
     * <p>This implementation assumes that the ConcurrentMap cannot contain null
     * values and {@code get()} returning null unambiguously means the key is
     * absent. Implementations which support null values <strong>must</strong>
     * override this default implementation.
     *
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @throws ClassCastException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @since 1.8
     */
    @Override
    default void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        Objects.requireNonNull(function);
        forEach((k,v) -> {
            while(!replace(k, v, function.apply(k, v))) {
                // v changed or k is gone
                if ( (v = get(k)) == null) {
                    // k is no longer in the map.
                    break;
                }
            }
        });
    }

    /**
     * 20210618
     * A. 默认实现相当于此 {@code map} 的以下步骤，然后返回当前值或 {@code null}（如果现在不存在）：
     *      if (map.get(key) == null) {
     *          V newValue = mappingFunction.apply(key);
     *          if (newValue != null)
     *              return map.putIfAbsent(key, newValue);
     *          }
     *      }
     * B. 当多个线程尝试更新（包括可能多次调用映射函数）时，默认实现可能会重试这些步骤。
     * C. 此实现假定 ConcurrentMap 不能包含 null 值，并且 {@code get()} 明确返回 null 意味着该键不存在。 支持null值的实现必须覆盖这个默认实现。
     */
    /**
     * {@inheritDoc}
     *
     * A.
     * @implSpec
     * The default implementation is equivalent to the following steps for this
     * {@code map}, then returning the current value or {@code null} if now
     * absent:
     *
     * <pre> {@code
     * if (map.get(key) == null) {
     *     V newValue = mappingFunction.apply(key);
     *     if (newValue != null)
     *         return map.putIfAbsent(key, newValue);
     * }
     * }</pre>
     *
     * B.
     * The default implementation may retry these steps when multiple
     * threads attempt updates including potentially calling the mapping
     * function multiple times.
     *
     * C.
     * <p>This implementation assumes that the ConcurrentMap cannot contain null
     * values and {@code get()} returning null unambiguously means the key is
     * absent. Implementations which support null values <strong>must</strong>
     * override this default implementation.
     *
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @since 1.8
     */
    @Override
    default V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        Objects.requireNonNull(mappingFunction);
        V v, newValue;
        return ((v = get(key)) == null &&
                (newValue = mappingFunction.apply(key)) != null &&
                (v = putIfAbsent(key, newValue)) == null) ? newValue : v;
    }

    /**
     * 20210618
     * A. 默认实现相当于为此 {@code map} 执行以下步骤，然后返回当前值或 {@code null}（如果现在不存在） ：
     *      if (map.get(key) != null) {
     *          V oldValue = map.get(key);
     *          V newValue = remappingFunction.apply(key, oldValue);
     *          if (newValue != null)
     *              map.replace(key, oldValue, newValue);
     *          else
     *              map.remove(key, oldValue);
     *          }
     *      }
     * B. 当多个线程尝试更新（包括可能多次调用重新映射函数）时，默认实现可能会重试这些步骤。
     * C. 此实现假定 ConcurrentMap 不能包含 null 值，并且 {@code get()} 明确返回 null 意味着该键不存在。 支持null值的实现必须覆盖这个默认实现。
     */
    /**
     * {@inheritDoc}
     *
     * A.
     * @implSpec
     * The default implementation is equivalent to performing the following
     * steps for this {@code map}, then returning the current value or
     * {@code null} if now absent. :
     *
     * <pre> {@code
     * if (map.get(key) != null) {
     *     V oldValue = map.get(key);
     *     V newValue = remappingFunction.apply(key, oldValue);
     *     if (newValue != null)
     *         map.replace(key, oldValue, newValue);
     *     else
     *         map.remove(key, oldValue);
     * }
     * }</pre>
     *
     * B.
     * The default implementation may retry these steps when multiple threads
     * attempt updates including potentially calling the remapping function
     * multiple times.
     *
     * C.
     * <p>This implementation assumes that the ConcurrentMap cannot contain null
     * values and {@code get()} returning null unambiguously means the key is
     * absent. Implementations which support null values <strong>must</strong>
     * override this default implementation.
     *
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @since 1.8
     */
    @Override
    default V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        Objects.requireNonNull(remappingFunction);
        V oldValue;
        while((oldValue = get(key)) != null) {
            V newValue = remappingFunction.apply(key, oldValue);
            if (newValue != null) {
                if (replace(key, oldValue, newValue))
                    return newValue;
            } else if (remove(key, oldValue))
               return null;
        }
        return oldValue;
    }

    /**
     * 20210618
     * A. 默认实现相当于为此 {@code map} 执行以下步骤，然后返回当前值或 {@code null}（如果不存在）：
     *      V oldValue = map.get(key);
     *      V newValue = remappingFunction.apply(key, oldValue);
     *      if (oldValue != null ) {
     *          if (newValue != null)
     *              map.replace(key, oldValue, newValue);
     *          else
     *              map.remove(key, oldValue);
     *      } else {
     *          if (newValue != null)
     *              map.putIfAbsent(key, newValue);
     *          else
     *              return null;
     *          }
     *      }
     * B. 当多个线程尝试更新（包括可能多次调用重新映射函数）时，默认实现可能会重试这些步骤。
     * C. 此实现假定 ConcurrentMap 不能包含 null 值，并且 {@code get()} 明确返回 null 意味着该键不存在。 支持null值的实现必须覆盖这个默认实现。
     */
    /**
     * {@inheritDoc}
     *
     * A.
     * @implSpec
     * The default implementation is equivalent to performing the following
     * steps for this {@code map}, then returning the current value or
     * {@code null} if absent:
     *
     * <pre> {@code
     * V oldValue = map.get(key);
     * V newValue = remappingFunction.apply(key, oldValue);
     * if (oldValue != null ) {
     *    if (newValue != null)
     *       map.replace(key, oldValue, newValue);
     *    else
     *       map.remove(key, oldValue);
     * } else {
     *    if (newValue != null)
     *       map.putIfAbsent(key, newValue);
     *    else
     *       return null;
     * }
     * }</pre>
     *
     * B.
     * The default implementation may retry these steps when multiple
     * threads attempt updates including potentially calling the remapping
     * function multiple times.
     *
     * C.
     * <p>This implementation assumes that the ConcurrentMap cannot contain null
     * values and {@code get()} returning null unambiguously means the key is
     * absent. Implementations which support null values <strong>must</strong>
     * override this default implementation.
     *
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @since 1.8
     */
    @Override
    default V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        Objects.requireNonNull(remappingFunction);
        V oldValue = get(key);
        for(;;) {
            V newValue = remappingFunction.apply(key, oldValue);
            if (newValue == null) {
                // delete mapping
                if (oldValue != null || containsKey(key)) {
                    // something to remove
                    if (remove(key, oldValue)) {
                        // removed the old value as expected
                        return null;
                    }

                    // some other value replaced old value. try again.
                    oldValue = get(key);
                } else {
                    // nothing to do. Leave things as they were.
                    return null;
                }
            } else {
                // add or replace old mapping
                if (oldValue != null) {
                    // replace
                    if (replace(key, oldValue, newValue)) {
                        // replaced as expected.
                        return newValue;
                    }

                    // some other value replaced old value. try again.
                    oldValue = get(key);
                } else {
                    // add (replace if oldValue was null)
                    if ((oldValue = putIfAbsent(key, newValue)) == null) {
                        // replaced
                        return newValue;
                    }

                    // some other value replaced old value. try again.
                }
            }
        }
    }

    /**
     * 20210618
     * A. 默认实现相当于为此 {@code map} 执行以下步骤，然后返回当前值或 {@code null}（如果不存在）：
     *      V oldValue = map.get(key);
     *      V newValue = (oldValue == null) ? value :
     *                    remappingFunction.apply(oldValue, value);
     *      if (newValue == null)
     *          map.remove(key);
     *      else
     *          map.put(key, newValue);
     * B. 当多个线程尝试更新（包括可能多次调用重新映射函数）时，默认实现可能会重试这些步骤。
     * C. 此实现假定 ConcurrentMap 不能包含 null 值，并且 {@code get()} 明确返回 null 意味着该键不存在。 支持null值的实现必须覆盖这个默认实现。
     */
    /**
     * {@inheritDoc}
     *
     * A.
     * @implSpec
     * The default implementation is equivalent to performing the following
     * steps for this {@code map}, then returning the current value or
     * {@code null} if absent:
     *
     * <pre> {@code
     * V oldValue = map.get(key);
     * V newValue = (oldValue == null) ? value :
     *              remappingFunction.apply(oldValue, value);
     * if (newValue == null)
     *     map.remove(key);
     * else
     *     map.put(key, newValue);
     * }</pre>
     *
     * B.
     * <p>The default implementation may retry these steps when multiple
     * threads attempt updates including potentially calling the remapping
     * function multiple times.
     *
     * C.
     * <p>This implementation assumes that the ConcurrentMap cannot contain null
     * values and {@code get()} returning null unambiguously means the key is
     * absent. Implementations which support null values <strong>must</strong>
     * override this default implementation.
     *
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @since 1.8
     */
    @Override
    default V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        Objects.requireNonNull(remappingFunction);
        Objects.requireNonNull(value);
        V oldValue = get(key);
        for (;;) {
            if (oldValue != null) {
                V newValue = remappingFunction.apply(oldValue, value);
                if (newValue != null) {
                    if (replace(key, oldValue, newValue))
                        return newValue;
                } else if (remove(key, oldValue)) {
                    return null;
                }
                oldValue = get(key);
            } else {
                if ((oldValue = putIfAbsent(key, value)) == null) {
                    return value;
                }
            }
        }
    }
}
