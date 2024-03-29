/*
 * Copyright (c) 1997, 2013, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package java.util;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 20201119
 * A. Map接口的基于哈希表的实现。 此实现提供所有可选的映射操作，并允许空值和空键。（HashMap 类大致等同于 Hashtable，只是它是非同步的并且允许空值。）
 *    该类不保证映射的顺序； 特别是，它不保证顺序会随着时间的推移保持不变。
 * B. 此实现为基本操作（get 和 put）提供恒定时间性能O(1)，假设散列函数在存储桶中正确分散元素。 集合视图上的迭代所需的时间与HashMap实例的“容量”（存储桶数）
 *    及其大小（键-值映射数）成正比。 因此，如果迭代性能很重要，则不要将初始容量设置得太高（或负载因子太低），这一点非常重要。
 * C. HashMap 的实例有两个影响其性能的参数：初始容量和负载因子。 容量是哈希表中存储桶的数量，初始容量只是创建哈希表时的容量。 负载因子是在自动增加散列表容量之前
 *    允许散列表获得的满度的度量。 当哈希表中的条目数超过负载因子和当前容量的乘积时，重新哈希表（即重建内部数据结构），使哈希表具有大约两倍的桶数。
 * D. 作为一般规则，默认负载因子 (0.75) 在时间和空间成本之间提供了很好的权衡。 较高的值会减少空间开销，但会增加查找成本（反映在 HashMap 类的大多数操作中，
 *    包括 get 和 put）。 在设置其初始容量时，应考虑映射中的预期条目数及其负载因子，以尽量减少重新哈希操作的次数。 如果初始容量大于最大条目数除以负载因子，
 *    则不会发生重新哈希操作。
 * E. 如果要在一个 HashMap 实例中存储许多映射，则创建具有足够大容量的映射将允许更有效地存储映射，而不是让它根据需要执行自动重新散列以增加表。 请注意，使用具有相同
 *    {@code hashCode()} 的多个键是降低任何哈希表性能的可靠方法。 为了改善影响，当键是 {@link Comparable} 时，此类可以使用键之间的比较顺序来帮助打破联系。
 * F. 请注意，此实现不是同步的。 如果多个线程并发访问一个散列映射，并且至少有一个线程在结构上修改了映射，则必须在外部进行同步。
 *   （结构修改是添加或删除一个或多个映射的任何操作；仅更改与实例已包含的键关联的值不是结构修改。）这通常是通过同步一些自然封装映射的对象来完成的。
 * G. 如果不存在此类对象，则应使用 {@link Collections#synchronizedMap Collections.synchronizedMap} 方法“包装”Map。 最好在创建时完成此操作，
 *    以防止意外不同步地访问Map：
 *      Map m = Collections.synchronizedMap(new HashMap(...));
 * H. 由此类的所有“集合视图方法”返回的迭代器都是快速失败的：如果在创建迭代器后的任何时间对结构进行结构修改，则除了通过迭代器自己的remove方法之外，
 *    该迭代器都将抛出{ @link ConcurrentModificationException}。 因此，面对并发修改，迭代器快速而干净地失败，而不是在未来不确定的时间冒着任意、非确定性行为的风险。
 * I. 请注意，无法保证迭代器的快速失败行为，因为一般而言，在存在非同步并发修改的情况下不可能做出任何硬保证。 快速失败的迭代器会尽最大努力抛出
 *    ConcurrentModificationException。 因此，编写一个依赖于这个异常来保证其正确性的程序是错误的：迭代器的快速失败行为应该只用于检测错误。
 * J. {@docRoot}/../technotes/guides/collections/index.html
 */
/**
 * A.
 * Hash table based implementation of the <tt>Map</tt> interface.  This
 * implementation provides all of the optional map operations, and permits
 * <tt>null</tt> values and the <tt>null</tt> key.  (The <tt>HashMap</tt>
 * class is roughly equivalent to <tt>Hashtable</tt>, except that it is
 * unsynchronized and permits nulls.)  This class makes no guarantees as to
 * the order of the map; in particular, it does not guarantee that the order
 * will remain constant over time.
 *
 * B.
 * <p>This implementation provides constant-time performance for the basic
 * operations (<tt>get</tt> and <tt>put</tt>), assuming the hash function
 * disperses the elements properly among the buckets.  Iteration over
 * collection views requires time proportional to the "capacity" of the
 * <tt>HashMap</tt> instance (the number of buckets) plus its size (the number
 * of key-value mappings).  Thus, it's very important not to set the initial
 * capacity too high (or the load factor too low) if iteration performance is
 * important.
 *
 * C.
 * <p>An instance of <tt>HashMap</tt> has two parameters that affect its
 * performance: <i>initial capacity</i> and <i>load factor</i>.  The
 * <i>capacity</i> is the number of buckets in the hash table, and the initial
 * capacity is simply the capacity at the time the hash table is created.  The
 * <i>load factor</i> is a measure of how full the hash table is allowed to
 * get before its capacity is automatically increased.  When the number of
 * entries in the hash table exceeds the product of the load factor and the
 * current capacity, the hash table is <i>rehashed</i> (that is, internal data
 * structures are rebuilt) so that the hash table has approximately twice the
 * number of buckets.
 *
 * D.
 * <p>As a general rule, the default load factor (.75) offers a good
 * tradeoff between time and space costs.  Higher values decrease the
 * space overhead but increase the lookup cost (reflected in most of
 * the operations of the <tt>HashMap</tt> class, including
 * <tt>get</tt> and <tt>put</tt>).  The expected number of entries in
 * the map and its load factor should be taken into account when
 * setting its initial capacity, so as to minimize the number of
 * rehash operations.  If the initial capacity is greater than the
 * maximum number of entries divided by the load factor, no rehash
 * operations will ever occur.
 *
 * E.
 * <p>If many mappings are to be stored in a <tt>HashMap</tt>
 * instance, creating it with a sufficiently large capacity will allow
 * the mappings to be stored more efficiently than letting it perform
 * automatic rehashing as needed to grow the table.  Note that using
 * many keys with the same {@code hashCode()} is a sure way to slow
 * down performance of any hash table. To ameliorate impact, when keys
 * are {@link Comparable}, this class may use comparison order among
 * keys to help break ties.
 *
 * F.
 * <p><strong>Note that this implementation is not synchronized.</strong>
 * If multiple threads access a hash map concurrently, and at least one of
 * the threads modifies the map structurally, it <i>must</i> be
 * synchronized externally.  (A structural modification is any operation
 * that adds or deletes one or more mappings; merely changing the value
 * associated with a key that an instance already contains is not a
 * structural modification.)  This is typically accomplished by
 * synchronizing on some object that naturally encapsulates the map.
 *
 * G.
 * If no such object exists, the map should be "wrapped" using the
 * {@link Collections#synchronizedMap Collections.synchronizedMap}
 * method.  This is best done at creation time, to prevent accidental
 * unsynchronized access to the map:<pre>
 *   Map m = Collections.synchronizedMap(new HashMap(...));</pre>
 *
 * H.
 * <p>The iterators returned by all of this class's "collection view methods"
 * are <i>fail-fast</i>: if the map is structurally modified at any time after
 * the iterator is created, in any way except through the iterator's own
 * <tt>remove</tt> method, the iterator will throw a
 * {@link ConcurrentModificationException}.  Thus, in the face of concurrent
 * modification, the iterator fails quickly and cleanly, rather than risking
 * arbitrary, non-deterministic behavior at an undetermined time in the
 * future.
 *
 * I.
 * <p>Note that the fail-fast behavior of an iterator cannot be guaranteed
 * as it is, generally speaking, impossible to make any hard guarantees in the
 * presence of unsynchronized concurrent modification.  Fail-fast iterators
 * throw <tt>ConcurrentModificationException</tt> on a best-effort basis.
 * Therefore, it would be wrong to write a program that depended on this
 * exception for its correctness: <i>the fail-fast behavior of iterators
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
 * @author  Doug Lea
 * @author  Josh Bloch
 * @author  Arthur van Hoff
 * @author  Neal Gafter
 * @see     Object#hashCode()
 * @see     Collection
 * @see     Map
 * @see     TreeMap
 * @see     Hashtable
 * @since   1.2
 */
// 20201119 继承AbstractMap, 实现Map、Cloneable、Serializable接口, 表示可克隆、可序列化
public class HashMap<K,V> extends AbstractMap<K,V> implements Map<K,V>, Cloneable, Serializable {

    private static final long serialVersionUID = 362498820763181265L;

    /**
     * 20201119
     * 实施说明:
     * A. 该映射通常用作分箱（分桶）哈希表，但是当分箱变得太大时，它们会被转换为 TreeNode 的分箱，每个分箱的结构类似于 java.util.TreeMap 中的分箱。
     *    大多数方法尝试使用普通的bin(箱)，但是在适用时转换到TreeNode方法（只需通过检查节点的instance）。 TreeNodes 的 bins 可以像任何其他 bins 一样被遍历和使用，
     *    但另外支持在人口过多时更快的查找。 然而，由于绝大多数正常使用的 bin 并没有过度填充，因此在 table 方法的过程中检查树 bin 的存在可能会延迟。
     * B. Tree bins（即元素都是 TreeNode 的箱）主要按 hashCode 排序，但在 tie 的情况下，如果两个元素是相同的“C 类实现 Comparable<C>”，则输入它们的
     *    compareTo 方法排序。 （我们通过反射保守地检查泛型类型以验证这一点——请参阅方法 compareClassFor）。 当键具有不同的哈希值或可排序时，
     *    树箱增加的复杂性值得在最坏的情况下提供O（log n）操作，因此，在偶然或恶意使用（其中hashCode（）方法返回的值很差）的情况下，性能会正常降低。
     *    分布式的，以及其中许多键共享一个hashCode的键，只要它们也是可比较的。 （如果这些都不适用，与不采取预防措施相比，我们可能会在时间和空间上浪费大约两倍。
     *    但唯一已知的情况源于糟糕的用户编程实践，这些实践已经很慢，这几乎没有什么区别。）
     * C. 因为 TreeNode 的大小大约是常规节点的两倍，所以我们仅在 bin 包含足够多的节点以保证使用时才使用它们（请参阅 TREEIFY_THRESHOLD）。
     *    当它们变得太小（由于移除或调整大小）时，它们会被转换回普通的bin。 在使用分布良好的用户哈希码的情况下，很少使用树箱。 理想情况下，在随机 hashCodes 下，
     *    bin 中节点的频率遵循泊松分布 (http://en.wikipedia.org/wiki/Poisson_distribution)，对于 0.75 的默认调整大小阈值，平均参数约为 0.5，
     *    尽管由于调整大小粒度而导致的大差异。 忽略方差，列表大小 k 的预期出现次数为 (exp(-0.5) * pow(0.5, k) / factorial(k))。 第一个值是：
     *      0:    0.60653066
     *      1:    0.30326533
     *      2:    0.07581633
     *      3:    0.01263606
     *      4:    0.00157952
     *      5:    0.00015795
     *      6:    0.00001316
     *      7:    0.00000094
     *      8:    0.00000006
     *    更多：不到千万分之一
     * D. 树箱的根通常是它的第一个节点。 然而，有时（目前仅在 Iterator.remove 上），根可能在别处，但可以通过父链接（方法 TreeNode.root()）恢复。
     * E. 所有适用的内部方法都接受一个哈希码作为参数（通常由公共方法提供），允许它们相互调用而无需重新计算用户哈希码。 大多数内部方法也接受一个“tab”参数，
     *    它通常是当前表，但在调整大小或转换时可能是新的或旧的。
     * F. 当 bin 列表被树化、拆分或未树化时，我们将它们保持在相同的相对访问/遍历顺序（即字段 Node.next）中，以更好地保留局部性，并稍微简化调用
     *    iterator.remove 的拆分和遍历的处理。 在插入时使用比较器时，为了在重新平衡之间保持总排序（或尽可能接近此处的要求），我们将类和
     *    identityHashCodes 作为决胜局进行比较。
     * G. 由于子类 LinkedHashMap 的存在，普通模式与树模式之间的使用和转换变得复杂。 请参阅下文，了解定义为在插入、删除和访问时调用的钩子方法，
     *    这些方法允许 LinkedHashMap 内部以其他方式保持独立于这些机制。 （这还要求将Map实例传递给一些可能创建新节点的实用程序方法。）
     * H. 类似于并发编程的基于 SSA 的编码风格有助于避免在所有扭曲指针操作中出现混叠错误。
     */
    /*
     * Implementation notes.
     *
     * A.
     * This map usually acts as a binned (bucketed) hash table, but
     * when bins get too large, they are transformed into bins of
     * TreeNodes, each structured similarly to those in
     * java.util.TreeMap. Most methods try to use normal bins, but
     * relay to TreeNode methods when applicable (simply by checking
     * instanceof a node).  Bins of TreeNodes may be traversed and
     * used like any others, but additionally support faster lookup
     * when overpopulated. However, since the vast majority of bins in
     * normal use are not overpopulated, checking for existence of
     * tree bins may be delayed in the course of table methods.
     *
     * B.
     * Tree bins (i.e., bins whose elements are all TreeNodes) are
     * ordered primarily by hashCode, but in the case of ties, if two
     * elements are of the same "class C implements Comparable<C>",
     * type then their compareTo method is used for ordering. (We
     * conservatively check generic types via reflection to validate
     * this -- see method comparableClassFor).  The added complexity
     * of tree bins is worthwhile in providing worst-case O(log n)
     * operations when keys either have distinct hashes or are
     * orderable, Thus, performance degrades gracefully under
     * accidental or malicious usages in which hashCode() methods
     * return values that are poorly distributed, as well as those in
     * which many keys share a hashCode, so long as they are also
     * Comparable. (If neither of these apply, we may waste about a
     * factor of two in time and space compared to taking no
     * precautions. But the only known cases stem from poor user
     * programming practices that are already so slow that this makes
     * little difference.)
     *
     * C.
     * Because TreeNodes are about twice the size of regular nodes, we
     * use them only when bins contain enough nodes to warrant use
     * (see TREEIFY_THRESHOLD). And when they become too small (due to
     * removal or resizing) they are converted back to plain bins.  In
     * usages with well-distributed user hashCodes, tree bins are
     * rarely used.  Ideally, under random hashCodes, the frequency of
     * nodes in bins follows a Poisson distribution
     * (http://en.wikipedia.org/wiki/Poisson_distribution) with a
     * parameter of about 0.5 on average for the default resizing
     * threshold of 0.75, although with a large variance because of
     * resizing granularity. Ignoring variance, the expected
     * occurrences of list size k are (exp(-0.5) * pow(0.5, k) /
     * factorial(k)). The first values are:
     *
     * 0:    0.60653066
     * 1:    0.30326533
     * 2:    0.07581633
     * 3:    0.01263606
     * 4:    0.00157952
     * 5:    0.00015795
     * 6:    0.00001316
     * 7:    0.00000094
     * 8:    0.00000006
     * more: less than 1 in ten million
     *
     * D.
     * The root of a tree bin is normally its first node.  However,
     * sometimes (currently only upon Iterator.remove), the root might
     * be elsewhere, but can be recovered following parent links
     * (method TreeNode.root()).
     *
     * E.
     * All applicable internal methods accept a hash code as an
     * argument (as normally supplied from a public method), allowing
     * them to call each other without recomputing user hashCodes.
     * Most internal methods also accept a "tab" argument, that is
     * normally the current table, but may be a new or old one when
     * resizing or converting.
     *
     * F.
     * When bin lists are treeified, split, or untreeified, we keep
     * them in the same relative access/traversal order (i.e., field
     * Node.next) to better preserve locality, and to slightly
     * simplify handling of splits and traversals that invoke
     * iterator.remove. When using comparators on insertion, to keep a
     * total ordering (or as close as is required here) across
     * rebalancings, we compare classes and identityHashCodes as
     * tie-breakers.
     *
     * G.
     * The use and transitions among plain vs tree modes is
     * complicated by the existence of subclass LinkedHashMap. See
     * below for hook methods defined to be invoked upon insertion,
     * removal and access that allow LinkedHashMap internals to
     * otherwise remain independent of these mechanics. (This also
     * requires that a map instance be passed to some utility methods
     * that may create new nodes.)
     *
     * H.
     * The concurrent-programming-like SSA-based coding style helps
     * avoid aliasing errors amid all of the twisty pointer operations.
     */

    /**
     * The default initial capacity - MUST be a power of two.
     */
    // 20201119 默认初始容量-必须是2的幂 => 默认容量2^4 = 16
    static final int DEFAULT_INITIAL_CAPACITY = 1 << 4; // aka 16

    /**
     * The maximum capacity, used if a higher value is implicitly specified
     * by either of the constructors with arguments.
     * MUST be a power of two <= 1<<30.
     */
    // 最大容量，在两个带参数的构造函数隐式指定更高值时使用。 必须是 2 的幂 <= 1<<30。
    static final int MAXIMUM_CAPACITY = 1 << 30;

    /**
     * The load factor used when none specified in constructor.
     */
    // 20201119 构造函数中未指定时使用的负载因子 => 加载因子0.75
    static final float DEFAULT_LOAD_FACTOR = 0.75f;

    /**
     * 20210601
     * 使用树而不是列表的 bin 计数阈值。将元素添加到至少具有这么多节点的 bin 时，bin 会转换为树。
     * 该值必须大于 2 且至少应为 8，以与树移除中关于在收缩时转换回普通 bin 的假设相匹配。
     */
    /**
     * The bin count threshold for using a tree rather than list for a
     * bin.  Bins are converted to trees when adding an element to a
     * bin with at least this many nodes. The value must be greater
     * than 2 and should be at least 8 to mesh with assumptions in
     * tree removal about conversion back to plain bins upon
     * shrinkage.
     */
    // 20201119 使用树（而不是列表）的存储箱计数阈值。将元素添加到至少具有这么多节点的bin时，存储单元将转换为树。
    // 20201119 该值必须大于2，且至少应为8，以符合树木移除中关于在收缩时转换回普通垃圾箱的假设。
    // 20201119 转换红黑树的哈希桶阈值=8
    static final int TREEIFY_THRESHOLD = 8;

    /**
     * 20210601
     * 在调整大小操作期间取消（拆分）bin 的 bin 计数阈值。 应小于 TREEIFY_THRESHOLD，最多为 6 以在移除下进行收缩检测。
     */
    /**
     * The bin count threshold for untreeifying a (split) bin during a
     * resize operation. Should be less than TREEIFY_THRESHOLD, and at
     * most 6 to mesh with shrinkage detection under removal.
     */
    // 20201119 在调整大小操作期间取消查询（拆分）存储箱的箱计数阈值。应小于TREEIFY_THRESHOLD，且最多为6，以便在去除时检测到收缩。
    // 20201119 取消红黑树的哈希桶阈值=6
    static final int UNTREEIFY_THRESHOLD = 6;

    /**
     * 20210601
     * 可以将 bin 树化的最小表容量。 （否则，如果 bin 中的节点过多，则调整表的大小。）应至少为 4 * TREEIFY_THRESHOLD，以避免调整大小和树化阈值之间发生冲突。
     */
    /**
     * The smallest table capacity for which bins may be treeified.
     * (Otherwise the table is resized if too many nodes in a bin.)
     * Should be at least 4 * TREEIFY_THRESHOLD to avoid conflicts
     * between resizing and treeification thresholds.
     */
    // 20201119 可对箱子进行树型化的最小表容量。（否则，如果一个bin中的节点太多，则会调整表的大小。）
    // 20201119 应至少为4*TREEIFY_THRESHOLD，以避免调整大小和树调整阈值之间的冲突。
    // 20201119 每个红黑树桶中表的最小容量=64, 至少为4*TREEIFY_THRESHOLD
    static final int MIN_TREEIFY_CAPACITY = 64;

    /**
     * Basic hash bin node, used for most entries.  (See below for
     * TreeNode subclass, and in LinkedHashMap for its Entry subclass.)
     */
    // 20201119 基本哈希bin节点，用于大多数条目。（TreeNode子类见下文，Entry子类见LinkedHashMap。）
    static class Node<K,V> implements Map.Entry<K,V> {
        final int hash;// 20201119 HashMap的hash值
        final K key;// 20201119 key值
        V value;// 20201119 value值
        Node<K,V> next;// 20201119 下一个结点

        // 20201119 结点构造器
        Node(int hash, K key, V value, Node<K,V> next) {
            this.hash = hash;
            this.key = key;
            this.value = value;// 20201119 存储的元素
            this.next = next;
        }

        public final K getKey()        { return key; }
        public final V getValue()      { return value; }

        // 20201119 entry的toString = "key = value"
        public final String toString() { return key + "=" + value; }

        // 20201119 entry的hashCode = 键的hashCode ^ 值的hashCode
        public final int hashCode() {
            return Objects.hashCode(key) ^ Objects.hashCode(value);
        }

        // 20201119 设置value值
        public final V setValue(V newValue) {
            V oldValue = value;
            value = newValue;
            return oldValue;
        }

        // 20201119 比较entry
        public final boolean equals(Object o) {
            // 20201119 如果对象等于本对象, 则返回true
            if (o == this)
                return true;

            // 20201119 如果对象为Map.Entry类型
            if (o instanceof Map.Entry) {
                // 20201119 则比较key和value, 只有都是同一个key和同一个value时才返回true
                Map.Entry<?,?> e = (Map.Entry<?,?>)o;
                if (Objects.equals(key, e.getKey()) &&
                    Objects.equals(value, e.getValue()))
                    return true;
            }

            // 20201119 否则返回false
            return false;
        }
    }

    /* ---------------- Static utilities -------------- */

    /**
     * 20201119
     * 计算 key.hashCode() 并将散列的较高位（异或）传播到较低位。 由于该表使用二次幂掩码，因此仅在当前掩码之上位变化的散列集将始终发生冲突。
     * （众所周知的例子是在小表中保存连续整数的浮点键集。）所以我们应用了一种向下传播高位影响的变换。 位扩展的速度、效用和质量之间存在权衡。
     * 因为许多常见的散列集已经合理分布（因此不会从传播中受益），并且因为我们使用树来处理 bin 中的大量冲突，所以我们只是以最便宜的方式对一些移位的位
     * 进行异或以减少系统损失， 以及合并最高位的影响，否则由于表边界而永远不会在索引计算中使用。
     */
    /**
     * Computes key.hashCode() and spreads (XORs) higher bits of hash
     * to lower.  Because the table uses power-of-two masking, sets of
     * hashes that vary only in bits above the current mask will
     * always collide. (Among known examples are sets of Float keys
     * holding consecutive whole numbers in small tables.)  So we
     * apply a transform that spreads the impact of higher bits
     * downward. There is a tradeoff between speed, utility, and
     * quality of bit-spreading. Because many common sets of hashes
     * are already reasonably distributed (so don't benefit from
     * spreading), and because we use trees to handle large sets of
     * collisions in bins, we just XOR some shifted bits in the
     * cheapest possible way to reduce systematic lossage, as well as
     * to incorporate impact of the highest bits that would otherwise
     * never be used in index calculations because of table bounds.
     */
    // 20201119 根据key获取hash
    static final int hash(Object key) {
        /**
         * 20201119 hash = key的hashCode ^ key的hashCode>>>16
         * 这是一种 速度、效用和质量 折衷的解决方案:
         *      A. 使用简单的位移运算, 保证运算速度; 使用高位异或, 保证减少低位冲突的可能性, 保证存储速度
         *      B. hashCode右移16位异或, 使得高位能被利用起来, 保证效用性
         *      C. 使用高位异或, 保证减少低位冲突的可能性, 保证散列表的质量
         */
        int h;
        return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
    }

    /**
     * Returns x's Class if it is of the form "class C implements
     * Comparable<C>", else null.
     */
    // 20201119 如果x的类的形式为“类C implements Comparable<C>”，则返回x的类，否则返回null。
    static Class<?> comparableClassFor(Object x) {
        // 20201119 如果X对象实现了Comparable接口
        if (x instanceof Comparable) {
            // 20201119 初始化参数
            Class<?> c;
            Type[] ts, as;
            Type t;
            ParameterizedType p;

            // 20201119 如果该对象是String类型
            if ((c = x.getClass()) == String.class) // bypass checks
                return c;// 则直接返回String类型, 因为Stirng实现了Compare接口

            // 20201119 否则获取它的父类接口, 如果不为空
            if ((ts = c.getGenericInterfaces()) != null) {
                // 20201119 则遍历获取每个接口的参数类型, 如果参数类型为Comparable类型, 则返回该类型
                for (int i = 0; i < ts.length; ++i) {
                    if (((t = ts[i]) instanceof ParameterizedType) &&
                        ((p = (ParameterizedType)t).getRawType() ==
                         Comparable.class) &&
                        (as = p.getActualTypeArguments()) != null &&
                        as.length == 1 && as[0] == c) // type arg is c
                        return c;
                }
            }
        }

        // 20201119 否则返回null, 代表该对象没有实现Compare接口
        return null;
    }

    /**
     * Returns k.compareTo(x) if x matches kc (k's screened comparable
     * class), else 0.
     */
    // 20201119 如果x与kc（k的筛选可比类）匹配，则返回k.compareTo（x），否则返回0。
    @SuppressWarnings({"rawtypes","unchecked"}) // for cast to Comparable
    // 20201119 返回Object k 和 Object x的比较结果
    static int compareComparables(Class<?> kc, Object k, Object x) {
        return (x == null || x.getClass() != kc ? 0 :
                ((Comparable)k).compareTo(x));
    }

    /**
     * Returns a power of two size for the given target capacity.
     */
    // 20201119 返回给定目标容量的2的幂次 => 根据容量获取实际容量2^n
    static final int tableSizeFor(int cap) {
        // 20201119 eg: cap = 0100 0011 1010 1001
        // 20201119 eg: n = 0100 0011 1010 1000
        int n = cap - 1;

        // 20201119 eg: 0100 0011 1010 1000
        //           |= 0010 0001 1101 0100 => 0110 0011 1111 1100
        n |= n >>> 1;

        // 2020119 eg:  0110 0011 1111 1100
        //           |= 0001 1000 1111 1111 => 0111 1011 1111 1111
        n |= n >>> 2;

        // 2020119 eg : 0111 1011 1111 1111
        //           |= 0000 0111 1011 1111 => 0111 1111 1111 1111
        n |= n >>> 4;

        // 2020119 eg : 0111 1111 1111 1111
        //           |= 0000 0000 0111 1111 => 0111 1111 1111 1111
        n |= n >>> 8;

        // 2020119 eg : 0111 1111 1111 1111
        //           |= 0000 0000 0000 0000 => 0111 1111 1111 1111
        n |= n >>> 16;
        // 20201119 eg: n => 0111 1111 1111 1111,
        // 实际容量再加1    => 1000 0000 0000 0000,
        // 因此右移这么位的目的是: 为了取得容纳CAP最高位且保证是2的幂次, 是保证求出的最高的那个1 在32位指定容量中最高位的那个1之前

        // 返回实际容量 => 大于最大容量则取最大容量2^30, 否则取n+1
        return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
    }

    /* ---------------- Fields -------------- */

    /**
     * The table, initialized on first use, and resized as
     * necessary. When allocated, length is always a power of two.
     * (We also tolerate length zero in some operations to allow
     * bootstrapping mechanics that are currently not needed.)
     */
    // 20201119 散列表, 长度总是2^n
    transient Node<K,V>[] table;

    /**
     * Holds cached entrySet(). Note that AbstractMap fields are used
     * for keySet() and values().
     */
    // 20201119 保存缓存的entrySet（）。请注意，AbstractMap字段用于keySet（）和values（）。
    transient Set<Map.Entry<K,V>> entrySet;

    /**
     * The number of key-value mappings contained in this map.
     */
    // 20201119 此映射中包含的键值映射数 => 即实际元素个数
    transient int size;

    /**
     * The number of times this HashMap has been structurally modified
     * Structural modifications are those that change the number of mappings in
     * the HashMap or otherwise modify its internal structure (e.g.,
     * rehash).  This field is used to make iterators on Collection-views of
     * the HashMap fail-fast.  (See ConcurrentModificationException).
     */
    // 20201119 结构修改次数
    transient int modCount;

    /**
     * The next size value at which to resize (capacity * load factor).
     *
     * @serial
     */
    // (The javadoc description is true upon serialization.
    // Additionally, if the table array has not been allocated, this
    // field holds the initial array capacity, or zero signifying
    // DEFAULT_INITIAL_CAPACITY.)
    // 20201119 要调整大小的下一个大小值（容量*负载系数） => 阈值(下一个目标容量的意思) = 容量 * 加载因子, 主要用于判断该次是否需要扩容
    int threshold;

    /**
     * The load factor for the hash table.
     *
     * @serial
     */
    // 20201119 散列表的加载因子
    final float loadFactor;

    /* ---------------- Public operations -------------- */

    /**
     * Constructs an empty <tt>HashMap</tt> with the specified initial
     * capacity and load factor.
     *
     * @param  initialCapacity the initial capacity
     * @param  loadFactor      the load factor
     * @throws IllegalArgumentException if the initial capacity is negative
     *         or the load factor is nonpositive
     */
    // 20201119 使用指定的初始容量和负载因子构造一个空的<tt>HashMap</tt>。
    public HashMap(int initialCapacity, float loadFactor) {
        // 20201119 如果指定容量小于0, 则抛出非法参数异常
        if (initialCapacity < 0)
            throw new IllegalArgumentException("Illegal initial capacity: " +
                                               initialCapacity);

        // 20201119 如果指定容量 > 最大容量2^30
        if (initialCapacity > MAXIMUM_CAPACITY)
            // 20201119 则等于最大容量2^30
            initialCapacity = MAXIMUM_CAPACITY;

        // 20201119 如果指定的加载因子小于0, 或者不是float类型, 则抛出非法参数异常
        if (loadFactor <= 0 || Float.isNaN(loadFactor))
            throw new IllegalArgumentException("Illegal load factor: " +
                                               loadFactor);

        // 20201119 赋值加载因子
        this.loadFactor = loadFactor;

        // 20201119 根据指定容量, 获取下一个目标容量, 赋值给阈值, 作为下次是否需要扩容的标准
        this.threshold = tableSizeFor(initialCapacity);
    }

    /**
     * Constructs an empty <tt>HashMap</tt> with the specified initial
     * capacity and the default load factor (0.75).
     *
     * @param  initialCapacity the initial capacity.
     * @throws IllegalArgumentException if the initial capacity is negative.
     */
    // 20201119 使用指定的初始容量和默认的加载因子（0.75）构造一个空的<tt>HashMap</tt>。
    public HashMap(int initialCapacity) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR);
    }

    /**
     * Constructs an empty <tt>HashMap</tt> with the default initial capacity
     * (16) and the default load factor (0.75).
     */
    // 20201119 使用默认的初始容量（16）和默认的加载因子（0.75）构造一个空的<tt>HashMap</tt>。
    public HashMap() {
        this.loadFactor = DEFAULT_LOAD_FACTOR; // all other fields defaulted
    }

    /**
     * Constructs a new <tt>HashMap</tt> with the same mappings as the
     * specified <tt>Map</tt>.  The <tt>HashMap</tt> is created with
     * default load factor (0.75) and an initial capacity sufficient to
     * hold the mappings in the specified <tt>Map</tt>.
     *
     * @param   m the map whose mappings are to be placed in this map
     * @throws  NullPointerException if the specified map is null
     */
    // 20201119 构造一个新的<tt>HashMap</tt>，其映射与指定的映射相同。HashMap</tt>是使用默认的负载因子（0.75）创建的，初始容量足以容纳指定的<tt>映射</tt>中的映射。
    public HashMap(Map<? extends K, ? extends V> m) {
        // 20201119 使用默认的加载因子
        this.loadFactor = DEFAULT_LOAD_FACTOR;

        // 20201119 添加m的所有key-value对, 处于表创建模式
        putMapEntries(m, false);
    }

    /**
     * Implements Map.putAll and Map constructor
     *
     * @param m the map
     * @param evict false when initially constructing this map, else // 20201119 最初构造此映射时为false，否则为true（中继到方法afterNodeInsertion）。
     * true (relayed to method afterNodeInsertion).
     */
    // 20201119 Map.putAll()方法的实现 => 添加指定Map的所有键值对
    final void putMapEntries(Map<? extends K, ? extends V> m, boolean evict) {
        // 20201119 获取目标Map的实际元素个数
        int s = m.size();

        // 20201119 如果元素个数大于0
        if (s > 0) {
            // 20201119 如果当前散列表为空
            if (table == null) { // pre-size
                // 20201119 使用目标Map的实际长度和本Map的阈值计算实际容量t
                float ft = ((float)s / loadFactor) + 1.0F;
                int t = ((ft < (float)MAXIMUM_CAPACITY) ?
                         (int)ft : MAXIMUM_CAPACITY);
                if (t > threshold)
                    // 20201119 如果t大于阈值, 则需要根据t重新计算阈值(下一个容量)
                    threshold = tableSizeFor(t);
            }

            // 20201119 如果散列表不为空 且目标Map实际长度大于当前阈值
            else if (s > threshold)
                resize();// 20201119 初始化 | 重置散列表

            // 20201119 遍历map中的所有entry
            for (Map.Entry<? extends K, ? extends V> e : m.entrySet()) {
                K key = e.getKey();
                V value = e.getValue();

                // 20201120 添加key-value对, 可以替换旧值, evict代表是否可以处于表创建模式
                putVal(hash(key), key, value, false, evict);
            }
        }
    }

    /**
     * Returns the number of key-value mappings in this map.
     *
     * @return the number of key-value mappings in this map
     */
    public int size() {
        return size;
    }

    /**
     * Returns <tt>true</tt> if this map contains no key-value mappings.
     *
     * @return <tt>true</tt> if this map contains no key-value mappings
     */
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * 20210605
     * A. 返回指定键映射到的值，如果此映射不包含键的映射，则返回 {@code null}。
     * B. 更正式地说，如果此映射包含从键 {@code k} 到值 {@code v} 的映射，使得 {@code (key==null ? k==null : key.equals(k))}，
     *    然后这个方法返回 {@code v}; 否则返回 {@code null}。 （最多可以有一个这样的映射。）
     * C. {@code null} 的返回值不一定表示映射不包含键的映射； Map也有可能将键显式映射到 {@code null}。 {@link #containsKey containsKey}
     *    操作可用于区分这两种情况。
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
    // 返回指定键映射到的值，如果此映射不包含键的映射，则返回 {@code null}。
    public V get(Object key) {
        Node<K,V> e;
        return (e = getNode(hash(key), key)) == null ? null : e.value;
    }

    /**
     * Implements Map.get and related methods
     *
     * @param hash hash for key
     * @param key the key
     * @return the node, or null if none
     */
    // 20201119 实现Map.get和相关方法 => 根据key和key的hash获取结点
    final Node<K,V> getNode(int hash, Object key) {
        // 20201119 初始化散列表, 桶头指针, 当前容量, 键值
        Node<K,V>[] tab; Node<K,V> first, e; int n; K k;

        // 20201119 赋值散列表tab, 当前容量n, 获取新hash桶的结点first
        if ((tab = table) != null && (n = tab.length) > 0 && (first = tab[(n - 1) & hash]) != null) {
            // 20201119 如果first结点hash相等, key值也相等, key对象任意相等
            if (first.hash == hash && // always check first node
                ((k = first.key) == key || (key != null && key.equals(k))))
                return first;// 20201119 则代表找到结点, 返回first结点
            if ((e = first.next) != null) {
                // 20201119 否则继续遍历链表
                if (first instanceof TreeNode)
                    // 20201119 如果链表属于红黑树结点, 则使用红黑树结点获取方法获取
                    return ((TreeNode<K,V>)first).getTreeNode(hash, key);
                do {
                    // 20201119 否则属于普通结点
                    if (e.hash == hash &&
                        ((k = e.key) == key || (key != null && key.equals(k))))
                        return e;// 20201119 如果hash、key值、key对象任意相等, 则返回当前结点
                } while ((e = e.next) != null);
            }
        }
        return null;
    }

    /**
     * Returns <tt>true</tt> if this map contains a mapping for the
     * specified key.
     *
     * @param   key   The key whose presence in this map is to be tested
     * @return <tt>true</tt> if this map contains a mapping for the specified
     * key.
     */
    // 20201120 如果此映射包含指定键的映射，则返回<tt>true</tt>。
    public boolean containsKey(Object key) {
        return getNode(hash(key), key) != null;
    }

    /**
     * 20210605
     * 将指定值与此映射中的指定键相关联。 如果映射先前包含键的映射，则旧值将被替换。
     */
    /**
     * Associates the specified value with the specified key in this map.
     * If the map previously contained a mapping for the key, the old
     * value is replaced.
     *
     * @param key key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with <tt>key</tt>, or
     *         <tt>null</tt> if there was no mapping for <tt>key</tt>.
     *         (A <tt>null</tt> return can also indicate that the map
     *         previously associated <tt>null</tt> with <tt>key</tt>.)
     */
    public V put(K key, V value) {
        return putVal(hash(key), key, value, false, true);
    }

    /**
     * Implements Map.put and related methods
     *
     * @param hash hash for key       // 20201120 key的hash
     * @param key the key             // 20201120 key值
     * @param value the value to put    // 20201120 value值
     * @param onlyIfAbsent if true, don't change existing value     // 20201120 true时不能替换旧值
     * @param evict if false, the table is in creation mode.        // 20201120 如果为false，则表处于创建模式。
     * @return previous value, or null if none
     */
    // 20201120 Map.put的方法实现, 添加key-value对
    final V putVal(int hash, K key, V value, boolean onlyIfAbsent, boolean evict) {
        Node<K,V>[] tab; Node<K,V> p; int n, i;

        // 20201120 如果当前散列表tab为空, 或者容量n为0
        if ((tab = table) == null || (n = tab.length) == 0)
            // 20201120 则初始化散列表
            n = (tab = resize()).length;

        // 20201120 根据新的容量n以及旧的hash计算新桶头结点p
        if ((p = tab[i = (n - 1) & hash]) == null)
            // 20201120 如果新桶为空, 则创建普通结点, 然后作为新桶的头结点
            tab[i] = newNode(hash, key, value, null);

        // 20201120 否则, 如果新桶不为空
        else {
            Node<K,V> e; K k;

            // 20201120 如果新桶头结点p的hash、key相等, 则设置p为当前结点e
            if (p.hash == hash && ((k = p.key) == key || (key != null && key.equals(k))))
                e = p;

            // 20201120 如果p为红黑树结点
            else if (p instanceof TreeNode)
                // 20201120 则使用红黑树的结点添加方法
                e = ((TreeNode<K,V>)p).putTreeVal(this, tab, hash, key, value);
            else {
                // 20201120 否则为普通结点, 则遍历散列表
                for (int binCount = 0; ; ++binCount) {
                    // 20201120 如果下一个结点e不为空
                    if ((e = p.next) == null) {
                        // 20201120 则创建hash桶的普通结点
                        p.next = newNode(hash, key, value, null);// 尾插

                        // 20201120 如果桶链表长度达到了红黑树转换的阈值8时
                        if (binCount >= TREEIFY_THRESHOLD - 1) // -1 for 1st // 20201120 -1是因为当前比较的binCount是索引不是个数
                            // 20201120 红黑树化当前桶
                            treeifyBin(tab, hash);
                        break;
                    }

                    // 20201120 如下一个结点e的hash与hash桶的hash相等 且 key也相等
                    if (e.hash == hash && ((k = e.key) == key || (key != null && key.equals(k))))
                        break;// 20201120 则跳出循环, 代表找到了对应的结点

                    // 20201120 继续遍历新桶的链表
                    p = e;
                }
            }

            // 20201120 当前结点e不为空
            if (e != null) { // existing mapping for key // 20201120 键的现有映射
                // 20201120 则获取旧值
                V oldValue = e.value;

                // 20201120 如果可以替换旧值, 或者旧值为null
                if (!onlyIfAbsent || oldValue == null)
                    // 20201120 则填充新值
                    e.value = value;

                // 20201120 触发LinkedHashMap结点替换后的回调访问函数
                afterNodeAccess(e);

                // 20201120 返回旧值
                return oldValue;
            }
        }

        // 20201120 如果不是结点替换, 而是插入到新桶中, 则更新结构修改次数
        ++modCount;

        // 20201120 如果插入后实际元素个数大于阈值(下一个目标的容量)时, 则进行重置散列表
        if (++size > threshold)
            resize();

        // 20201120 触发LinkedHashMap结点插入后的回调访问函数
        afterNodeInsertion(evict);
        return null;
    }

    /**
     * 20201119
     * 初始化或加倍表大小。 如果为空，则根据字段阈值中持有的初始容量目标进行分配。 否则，因为我们使用的是 2 的幂扩展，所以每个 bin 中的元素必须保持相同的索引，
     * 或者在新表中以 2 的幂的偏移量移动。
     */
    /**
     * Initializes or doubles table size.  If null, allocates in
     * accord with initial capacity target held in field threshold.
     * Otherwise, because we are using power-of-two expansion, the
     * elements from each bin must either stay at same index, or move
     * with a power of two offset in the new table.
     *
     * @return the table
     */
    // 20201119 初始化 | 重置散列表
    final Node<K,V>[] resize() {
        // 20201119 备份当前散列表
        Node<K,V>[] oldTab = table;

        // 20201119 获取当前散列表长度 => 旧的容量
        int oldCap = (oldTab == null) ? 0 : oldTab.length;

        // 20201119 获取当前Map的阈值(下一个目标容量)
        int oldThr = threshold;

        // 20201119 初始化新容量和新阈值
        int newCap, newThr = 0;

        // 20201119 如果旧的容量>0 => 开始计算新的容量和新的阈值
        if (oldCap > 0) {
            // 20201119 如果旧的容量还大于最大容量2^30
            if (oldCap >= MAXIMUM_CAPACITY) {
                // 20201119 则设置最大阈值0x7fffffff
                threshold = Integer.MAX_VALUE;

                // 20201119 返回最大容量
                return oldTab;
            }

            // 20201119 否则, 如果将旧容量*2, 这时小于最大容量2^30 & >= 初始容量16
            else if ((newCap = oldCap << 1) < MAXIMUM_CAPACITY &&
                     oldCap >= DEFAULT_INITIAL_CAPACITY)
                // 20201119 则新阈值 = 旧阈值 * 2, 也就是新的一个目标容量 = 旧的下一个目标容量 * 2
                newThr = oldThr << 1; // double threshold
        }
        // 20201110 如果旧的容量<=0, 但旧的阈值>0
        else if (oldThr > 0) // initial capacity was placed in threshold // 20201119 初始容量处于阈值
            newCap = oldThr;// 20201119 则新容量 = 旧的阈值
        else {               // zero initial threshold signifies using defaults // 20201119 零初始阈值表示使用默认值
            newCap = DEFAULT_INITIAL_CAPACITY;// 20201119 否则容量<=0, 阈值<=0, 则新容量=默认容量16
            newThr = (int)(DEFAULT_LOAD_FACTOR * DEFAULT_INITIAL_CAPACITY);// 20201119 新阈值 = 0.75 * 16 = 12
        }

        // 20201119 如果新阈值为0
        if (newThr == 0) {
            // 20201119 则新阈值 = 新容量 * 当前的加载因子
            float ft = (float)newCap * loadFactor;
            newThr = (newCap < MAXIMUM_CAPACITY && ft < (float)MAXIMUM_CAPACITY ?
                      (int)ft : Integer.MAX_VALUE);
        }

        // 20201119 更新当前阈值为新阈值
        threshold = newThr;

        // 20201119 使用新容量创建散列表(Node数组, 注意不是链表, 看成对象就行了)
        @SuppressWarnings({"rawtypes","unchecked"})
            Node<K,V>[] newTab = (Node<K,V>[])new Node[newCap];

        // 20201119 赋值新数组给当前散列表
        table = newTab;

        // 20201119 如果旧的散列表不为空 => 开始转移数据
        if (oldTab != null) {
            // 20201119 则遍历旧的散列表
            for (int j = 0; j < oldCap; ++j) {
                // 20201119 初始化空的Node对象
                Node<K,V> e;

                // 20201119 如果当前桶不为null, 同时备份桶内容到e对象中
                if ((e = oldTab[j]) != null) {
                    // 20201119 则清空该桶
                    oldTab[j] = null;

                    // 20201119 如果e(即原来的桶)不存在下一个结点, 即原来的桶中的元素中只有一个元素
                    if (e.next == null)
                        // 20201120 e.hash & (newCap - 1) 相当于 e.hash % newCap, 其中后者最后需要转换为前者进行计算, 所以前者效率更高
                        // 20201120 扩容前选择桶的位置 oldIndex: e.hash & (oldCap - 1)
                        // 20201120 扩容后选择桶的位置 newIndex: e.hash & (newCap - 1)
                        // 20201120 经过例子计算, 由于oldCap、newCap都是2^n, 且newCap比oldCap高1位, 那么oldCap - 1 与 newCap - 1两者实际就是差了最高位的1
                        // 20201120 这时与原来的hash相与只会有两种结果:
                        // 20201120 1、原来的hash对应newCap-1的最高位为0, 那么扩容后桶的位置newIndex还是不变
                        // 20201120 2、原来的hash对应newCap-1的最高位为1, 那么扩容后痛的位置newIndex比oldIndex值差了高位的1, 即newIndex = oldIndex + 2^n, 其中2^n正好是oldCap, 即newIndex = oldIndex + oldCap
                        // 20201120 =>  这样能够在尽可能保证桶index的不变性, 减少结点的移动, 提高resize性能
                        newTab[e.hash & (newCap - 1)] = e;// 20201119 e赋值到新的散列表中

                    // 20201119 如果当前桶属于红黑树结点
                    else if (e instanceof TreeNode)
                        // 20201119 则根据新容量拆分当前桶链表, 会有两种结果 newIndex = oldIndex; newIndex = oldIndex + oldCap
                        ((TreeNode<K,V>)e).split(this, newTab, j, oldCap);
                    else { // preserve order
                        // 20201119 如果不属于红黑树结点
                        Node<K,V> loHead = null, loTail = null;// 20201120 loHead链表存放newIndex = oldIndex的结点
                        Node<K,V> hiHead = null, hiTail = null;// 20201120 hiHead链表存放newInedx = oldIndex + oldCap的结点
                        Node<K,V> next;// 20201120 只有next结点, 没有prev结点, prev红黑树结点才有

                        do {
                            // 20201119 遍历分割当前桶链表, 根据newIndex是否发生变化, 决定存放结点到lo链表还是hi链表
                            next = e.next;

                            // 20201120 (e.hash & oldCap) == 0, 说明hash在oldCap最高位为0, 即扩容后newIndex = oldIndex, 结点存放到lo链表中
                            if ((e.hash & oldCap) == 0) {
                                if (loTail == null)
                                    loHead = e;// 20201119 初始化lo链表头指针
                                else
                                    loTail.next = e;// 20201119 追加结点到lo链表

                                // 20201120 lo链表移动到一个结点
                                loTail = e;
                            }

                            // 20201120 (e.hash & oldCap) != 0, 说明hash在oldCap最高位为1, 即扩容后newIndex = oldIndex + oldCap, 结点存放到hi链表中
                            else {
                                if (hiTail == null)
                                    hiHead = e;// 20201119 初始化hi链表
                                else
                                    hiTail.next = e;// 20201119 追加结点到hi链表

                                // 20201120
                                hiTail = e;
                            }
                        } while ((e = next) != null);

                        // 20201119 如果lo链表不为空, 则设置lo链表头指针到当前桶中
                        if (loTail != null) {
                            loTail.next = null;
                            newTab[j] = loHead;// 20201120 因为newIndex = oldIndex
                        }

                        // 20201119 如果hi链表不为空, 则设置hi链表头指针到桶偏移+旧容量的新桶中
                        if (hiTail != null) {
                            hiTail.next = null;
                            newTab[j + oldCap] = hiHead;// 20201120 因为newIndex = oldIndex + oldCap
                        }
                    }
                }
            }
        }

        // 20201119 返回新的散列表
        return newTab;
    }

    /**
     * 20210605
     * 除非表太小，否则替换给定哈希索引处 bin 中的所有链接节点，在这种情况下改为调整大小。
     */
    /**
     * Replaces all linked nodes in bin at index for given hash unless
     * table is too small, in which case resizes instead.
     */
    // 红黑树化hash对应桶中的普通链表
    final void treeifyBin(Node<K,V>[] tab, int hash) {
        int n, index; Node<K,V> e;

        // 20201120 如果散列表为空, 或者散列表容量n小于红黑树化列表阈值64时
        if (tab == null || (n = tab.length) < MIN_TREEIFY_CAPACITY)
            resize();// 20201120 则重置散列表

        // 20201120 如果散列表满足红黑树化条件, 且新桶e不为空
        else if ((e = tab[index = (n - 1) & hash]) != null) {
            TreeNode<K,V> hd = null, tl = null;

            // 20201120 遍历当前桶链表
            do {
                // 20201120 替换新桶e结点为红黑树结点
                TreeNode<K,V> p = replacementTreeNode(e, null);

                // 20201120 tl指针还没被初始化
                if (tl == null)
                    hd = p;// 20201120 则备份新桶头指针p
                else {
                    // 20201120 否则追加链表结点到hd链表中
                    p.prev = tl;
                    tl.next = p;
                }

                // 20201120 tl结点移动到下一个
                tl = p;
            } while ((e = e.next) != null);

            // 20201120 新链表hd设置为新桶的头结点, 如果不为空
            if ((tab[index] = hd) != null)
                // 20201120 则将红黑树化散列表 -> 实例调用时遍历当前桶的链表
                hd.treeify(tab);
        }
    }

    /**
     * Copies all of the mappings from the specified map to this map.
     * These mappings will replace any mappings that this map had for
     * any of the keys currently in the specified map.
     *
     * @param m mappings to be stored in this map
     * @throws NullPointerException if the specified map is null
     */
    // 20201120 将指定映射中的所有映射复制到此映射。这些映射将替换此映射对指定映射中当前任何键的任何映射。
    public void putAll(Map<? extends K, ? extends V> m) {
        // 20201120 添加m的所有key-value对, 可以替换旧值, 不处于表创建模式, 是添加模式
        putMapEntries(m, true);
    }

    /**
     * Removes the mapping for the specified key from this map if present.
     *
     * @param  key key whose mapping is to be removed from the map
     * @return the previous value associated with <tt>key</tt>, or
     *         <tt>null</tt> if there was no mapping for <tt>key</tt>.
     *         (A <tt>null</tt> return can also indicate that the map
     *         previously associated <tt>null</tt> with <tt>key</tt>.)
     */
    // 如果存在，则从此映射中删除指定键的映射。
    public V remove(Object key) {
        Node<K,V> e;

        // 20201121 false，false：代表值不相等也会删除，删除时不要移动根结点
        return (e = removeNode(hash(key), key, null, false, true)) == null ?
            null : e.value;
    }

    /**
     * Implements Map.remove and related methods
     *
     * @param hash hash for key // key的hash值
     * @param key the key // key值
     * @param value the value to match if matchValue, else ignored  // 如果matchValue为true则匹配的value值，否则忽略value值
     * @param matchValue if true only remove if value is equal // 20201119 如果为true，则仅在值相等时才删除
     * @param movable if false do not move other nodes while removing // 20201119 如果为false，则在删除时不移动其他节点
     * @return the node, or null if none
     */
    // 20201119 根据key的hash、key值、value值删除结点
    final Node<K,V> removeNode(int hash, Object key, Object value, boolean matchValue, boolean movable) {
        Node<K,V>[] tab; Node<K,V> p; int n, index;

        // 20201119 当前散列表tab、容量n、新桶p
        if ((tab = table) != null && (n = tab.length) > 0 && (p = tab[index = (n - 1) & hash]) != null) {
            Node<K,V> node = null, e; K k; V v;

            // 20201119 如果新桶头结点hash相等、key值相等、key对象相等
            if (p.hash == hash && ((k = p.key) == key || (key != null && key.equals(k))))
                node = p;// 20201119 则标记当前结点为待删除结点node
            else if ((e = p.next) != null) {
                // 20201119 否则继续遍历该链表
                if (p instanceof TreeNode)
                    // 20201119 如果属于红黑树结点, 则使用红黑树方法获取结点
                    node = ((TreeNode<K,V>)p).getTreeNode(hash, key);
                else {
                    // 20201119 否则为普通结点
                    do {
                        // 20201119 如果结点hash相等、key值相等、key对象相等
                        if (e.hash == hash && ((k = e.key) == key || (key != null && key.equals(k)))) {
                            node = e;// 20201119 则标记当前结点为待删除结点node
                            break;
                        }
                        p = e;
                    } while ((e = e.next) != null);
                }
            }

            // 20201119 如果存在待删除结点node, 且该结点的值相等, 值对象也相等
            if (node != null && (!matchValue || (v = node.value) == value || (value != null && value.equals(v)))) {
                // 20201119 如果该结点又属于红黑树结点
                if (node instanceof TreeNode)
                    // 20201119 则使用红黑树方法删除该结点
                    ((TreeNode<K,V>)node).removeTreeNode(this, tab, movable);
                else if (node == p)// 20201119 否则如果该结点为该桶的头结点
                    // 20201119 删除该结点, 并重新设置该桶的头节点
                    tab[index] = node.next;
                else
                    // 20201119 否则清空该结点与下一个结点的关系, 链接该结点与下一个结点
                    p.next = node.next;

                // 20201119 结构修改次数+1
                ++modCount;

                // 20201119 实际元素个数-1
                --size;

                // 20201119 LinkedHashMap回调函数 => 空实现
                afterNodeRemoval(node);

                // 20201119 返回当前结点
                return node;
            }
        }

        // 20201119 找不到则返回null
        return null;
    }

    /**
     * Removes all of the mappings from this map.
     * The map will be empty after this call returns.
     */
    // 20201119 从该map中删除所有映射。 此调用返回后，map将为空。
    public void clear() {
        Node<K,V>[] tab;

        // 20201119 结构修改次数+1
        modCount++;

        // 20201119 遍历散列表, 将散列表每个桶都清空
        if ((tab = table) != null && size > 0) {
            size = 0;
            for (int i = 0; i < tab.length; ++i)
                tab[i] = null;
        }
    }

    /**
     * Returns <tt>true</tt> if this map maps one or more keys to the
     * specified value.
     *
     * @param value value whose presence in this map is to be tested
     * @return <tt>true</tt> if this map maps one or more keys to the
     *         specified value
     */
    // 20201119 如果此映射将一个或多个键映射到指定值，则返回<tt>true</tt>。
    public boolean containsValue(Object value) {
        Node<K,V>[] tab; V v;

        // 20201120 散列表tab, 如果实际元素个数>0
        if ((tab = table) != null && size > 0) {
            // 20201120 则遍历散列表
            for (int i = 0; i < tab.length; ++i) {
                // 20201120 遍历每个桶的链表
                for (Node<K,V> e = tab[i]; e != null; e = e.next) {
                    // 20201120 如果找到值相等的结点, 在返回true
                    if ((v = e.value) == value || (value != null && value.equals(v)))
                        return true;
                }
            }
        }

        // 202011120 否则返回false
        return false;
    }

    /**
     * 20201120
     * 返回此映射中包含的键的{@link Set}视图。
     * 集合由映射支持，因此对映射的更改将反映在集合中，反之亦然。
     * 如果在对集合进行迭代时修改映射（除了通过迭代器自己的<tt>remove</tt>操作），则迭代的结果是未定义的。
     * 集合支持元素移除，元素移除通过<tt>迭代器.remove</tt>，<tt>设置。删除</tt>，<tt>移除所有</tt>，<tt>返回</tt>，和<tt>清除</tt>操作。
     * 它不支持<tt>add</tt>或<tt>addAll</tt>操作。
     */
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
     *
     * @return a set view of the keys contained in this map
     */
    // 20201120 返回map的键的集合
    public Set<K> keySet() {
        Set<K> ks;
        return (ks = keySet) == null ? (keySet = new KeySet()) : ks;
    }

    // 20201120 map键的集合, 继承了AbstractSet
    final class KeySet extends AbstractSet<K> {
        public final int size()                 { return size; }
        public final void clear()               { HashMap.this.clear(); }
        public final Iterator<K> iterator()     { return new KeyIterator(); }
        public final boolean contains(Object o) { return containsKey(o); }
        public final boolean remove(Object key) {
            return removeNode(hash(key), key, null, false, true) != null;
        }
        public final Spliterator<K> spliterator() {
            return new KeySpliterator<>(HashMap.this, 0, -1, 0, 0);
        }

        // 20201120 根据指定规则迭代
        public final void forEach(Consumer<? super K> action) {
            Node<K,V>[] tab;
            if (action == null)
                throw new NullPointerException();

            // 20201120 迭代散列表、每个桶的结点
            if (size > 0 && (tab = table) != null) {
                int mc = modCount;
                for (int i = 0; i < tab.length; ++i) {
                    for (Node<K,V> e = tab[i]; e != null; e = e.next)
                        action.accept(e.key);
                }
                if (modCount != mc)
                    throw new ConcurrentModificationException();
            }
        }
    }

    /**
     * 20201120
     * 返回此映射中包含的值的{@link Collection}视图。
     * 集合由映射支持，因此对映射的更改将反映在集合中，反之亦然。
     * 如果在对集合进行迭代时修改了映射（除了通过迭代器自己的<tt>remove</tt>操作），则迭代的结果是未定义的。
     * 集合支持元素移除，元素移除通过<tt>迭代器.remove</tt>，<tt>集合.删除</tt>，<tt>移除所有</tt>，<tt>返回</tt>和<tt>清除</tt>操作。
     * 它不支持<tt>add</tt>或<tt>addAll</tt>操作。
     */
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
     *
     * @return a view of the values contained in this map
     */
    // 20201120 返回map的值的集合
    public Collection<V> values() {
        Collection<V> vs;
        return (vs = values) == null ? (values = new Values()) : vs;
    }

    // 20201120 map的值的结合, 继承了AbstractCollection
    final class Values extends AbstractCollection<V> {
        public final int size()                 { return size; }
        public final void clear()               { HashMap.this.clear(); }
        public final Iterator<V> iterator()     { return new ValueIterator(); }
        public final boolean contains(Object o) { return containsValue(o); }
        public final Spliterator<V> spliterator() {
            return new ValueSpliterator<>(HashMap.this, 0, -1, 0, 0);
        }
        public final void forEach(Consumer<? super V> action) {
            Node<K,V>[] tab;
            if (action == null)
                throw new NullPointerException();
            if (size > 0 && (tab = table) != null) {
                int mc = modCount;
                for (int i = 0; i < tab.length; ++i) {
                    for (Node<K,V> e = tab[i]; e != null; e = e.next)
                        action.accept(e.value);
                }
                if (modCount != mc)
                    throw new ConcurrentModificationException();
            }
        }
    }

    /**
     * 20201119
     * 返回此map中包含的映射的{@link Set}视图。该map集受map支持，因此对map的更改会反映在该map集中，反之亦然。
     * 如果在对集合进行迭代时修改了map（通过迭代器自己的<tt> remove </ tt>操作 或 通过迭代器返回的映射项上的<tt> setValue </ tt>操作除外） ）的迭代结果不确定。
     * 该集合支持元素删除，该元素通过<tt> Iterator.remove </ tt，<tt> Set.remove </ tt>，<tt> removeAll </ tt>，<tt>从map中删除相应的映射。
     * keepAll </ tt>和<tt> clear </ tt>操作。 它不支持<tt> add </ tt>或<tt> addAll </ tt>操作。
     */
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
     *
     * @return a set view of the mappings contained in this map
     */
    // 20201119 返回map所有的entry集合
    public Set<Map.Entry<K,V>> entrySet() {
        Set<Map.Entry<K,V>> es;

        // 20201119 构造并返回entrySet
        return (es = entrySet) == null ? (entrySet = new EntrySet()) : es;
    }

    // 20201119 用于keySet（）和values（）的Set集合, 继承AbstractSet
    final class EntrySet extends AbstractSet<Map.Entry<K,V>> {
        // 20201119 获取实际元素个数
        public final int size()                 { return size; }

        // 20201119 清空map => 得到一个空的散列表
        public final void clear()               { HashMap.this.clear(); }

        // 20201119 获取EntryIterator
        public final Iterator<Map.Entry<K,V>> iterator() {
            return new EntryIterator();
        }

        // 20201119 判断是否包含指定对象
        public final boolean contains(Object o) {
            // 20201119 如果该对象不属于Map.Entry类型, 则返回false
            if (!(o instanceof Map.Entry))
                return false;

            // 20201119 否则根据key的hash和key值获取对应的结点
            Map.Entry<?,?> e = (Map.Entry<?,?>) o;
            Object key = e.getKey();
            Node<K,V> candidate = getNode(hash(key), key);

            // 20201119 判断两结点是否相等
            return candidate != null && candidate.equals(e);
        }

        // 20201119 移除指定对象
        public final boolean remove(Object o) {
            // 20201119 类型校验
            if (o instanceof Map.Entry) {
                // 20201119 如果属于Map.Entry类型
                Map.Entry<?,?> e = (Map.Entry<?,?>) o;
                Object key = e.getKey();
                Object value = e.getValue();

                // 20201119 则根据key的hash、key值、value值删除, 只有在值相等时才删除, 且删除时需要移动结点
                return removeNode(hash(key), key, value, true, true) != null;
            }

            // 20201119 否则返回false
            return false;
        }
        public final Spliterator<Map.Entry<K,V>> spliterator() {
            return new EntrySpliterator<>(HashMap.this, 0, -1, 0, 0);
        }
        public final void forEach(Consumer<? super Map.Entry<K,V>> action) {
            Node<K,V>[] tab;
            if (action == null)
                throw new NullPointerException();
            if (size > 0 && (tab = table) != null) {
                int mc = modCount;
                for (int i = 0; i < tab.length; ++i) {
                    for (Node<K,V> e = tab[i]; e != null; e = e.next)
                        action.accept(e);
                }
                if (modCount != mc)
                    throw new ConcurrentModificationException();
            }
        }
    }

    // Overrides of JDK8 Map extension methods
    // 20201120 带默认值的获取, 如果获取不到key的元素, 则返回默认值
    @Override
    public V getOrDefault(Object key, V defaultValue) {
        Node<K,V> e;
        return (e = getNode(hash(key), key)) == null ? defaultValue : e.value;
    }

    // 20201120 处于创建模式下的key-value添加, 不能替换旧值
    @Override
    public V putIfAbsent(K key, V value) {
        return putVal(hash(key), key, value, true, true);
    }

    // 删除key和value都equals的键值对
    @Override
    public boolean remove(Object key, Object value) {
        return removeNode(hash(key), key, value, true, true) != null;
    }

    // 20201120 key-value相等时, 替换结点的值
    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        Node<K,V> e; V v;

        // 20201120 如果key相等、值相等
        if ((e = getNode(hash(key), key)) != null && ((v = e.value) == oldValue || (v != null && v.equals(oldValue)))) {
            // 20201120 则旧值替换成新值
            e.value = newValue;

            // 20201120 LinkedHashMap回调函数
            afterNodeAccess(e);

            // 20201120 替换成功返回true
            return true;
        }

        // 20201120 替换失败返回false
        return false;
    }

    // 20201120 key相等时, 新值替换掉旧值, 返回旧值
    @Override
    public V replace(K key, V value) {
        Node<K,V> e;

        // 20201120 如果key值相等, 则新值替换旧值, 返回旧值
        if ((e = getNode(hash(key), key)) != null) {
            V oldValue = e.value;
            e.value = value;
            afterNodeAccess(e);
            return oldValue;
        }
        return null;
    }

    // 20201120 通过Function计算key, 得出新值然后替换key的旧值
    @Override
    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        if (mappingFunction == null)
            throw new NullPointerException();
        int hash = hash(key);
        Node<K,V>[] tab; Node<K,V> first; int n, i;
        int binCount = 0;
        TreeNode<K,V> t = null;
        Node<K,V> old = null;
        if (size > threshold || (tab = table) == null ||
            (n = tab.length) == 0)
            n = (tab = resize()).length;
        if ((first = tab[i = (n - 1) & hash]) != null) {
            if (first instanceof TreeNode)
                old = (t = (TreeNode<K,V>)first).getTreeNode(hash, key);
            else {
                Node<K,V> e = first; K k;
                do {
                    if (e.hash == hash &&
                        ((k = e.key) == key || (key != null && key.equals(k)))) {
                        old = e;
                        break;
                    }
                    ++binCount;
                } while ((e = e.next) != null);
            }
            V oldValue;
            if (old != null && (oldValue = old.value) != null) {
                afterNodeAccess(old);
                return oldValue;
            }
        }
        V v = mappingFunction.apply(key);
        if (v == null) {
            return null;
        } else if (old != null) {
            old.value = v;
            afterNodeAccess(old);
            return v;
        }
        else if (t != null)
            t.putTreeVal(this, tab, hash, key, v);
        else {
            tab[i] = newNode(hash, key, v, first);
            if (binCount >= TREEIFY_THRESHOLD - 1)
                treeifyBin(tab, hash);
        }
        ++modCount;
        ++size;
        afterNodeInsertion(true);
        return v;
    }

    // 20201120 通过Function计算key和旧值, 得出新值然后替换key的旧值
    public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        if (remappingFunction == null)
            throw new NullPointerException();
        Node<K,V> e; V oldValue;
        int hash = hash(key);
        if ((e = getNode(hash, key)) != null &&
            (oldValue = e.value) != null) {
            V v = remappingFunction.apply(key, oldValue);
            if (v != null) {
                e.value = v;
                afterNodeAccess(e);
                return v;
            }
            else
                removeNode(hash, key, null, false, true);
        }
        return null;
    }

    // 20201120 通过Function计算key和旧值, 得出新值然后生成新的结点
    @Override
    public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        if (remappingFunction == null)
            throw new NullPointerException();
        int hash = hash(key);
        Node<K,V>[] tab; Node<K,V> first; int n, i;
        int binCount = 0;
        TreeNode<K,V> t = null;
        Node<K,V> old = null;
        if (size > threshold || (tab = table) == null ||
            (n = tab.length) == 0)
            n = (tab = resize()).length;
        if ((first = tab[i = (n - 1) & hash]) != null) {
            if (first instanceof TreeNode)
                old = (t = (TreeNode<K,V>)first).getTreeNode(hash, key);
            else {
                Node<K,V> e = first; K k;
                do {
                    if (e.hash == hash &&
                        ((k = e.key) == key || (key != null && key.equals(k)))) {
                        old = e;
                        break;
                    }
                    ++binCount;
                } while ((e = e.next) != null);
            }
        }
        V oldValue = (old == null) ? null : old.value;
        V v = remappingFunction.apply(key, oldValue);
        if (old != null) {
            if (v != null) {
                old.value = v;
                afterNodeAccess(old);
            }
            else
                removeNode(hash, key, null, false, true);
        }
        else if (v != null) {
            if (t != null)
                t.putTreeVal(this, tab, hash, key, v);
            else {
                tab[i] = newNode(hash, key, v, first);
                if (binCount >= TREEIFY_THRESHOLD - 1)
                    treeifyBin(tab, hash);
            }
            ++modCount;
            ++size;
            afterNodeInsertion(true);
        }
        return v;
    }

    // 20201120 通过Function计算旧值和value, 得出新值然后生成新的结点
    @Override
    public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        if (value == null)
            throw new NullPointerException();
        if (remappingFunction == null)
            throw new NullPointerException();
        int hash = hash(key);
        Node<K,V>[] tab; Node<K,V> first; int n, i;
        int binCount = 0;
        TreeNode<K,V> t = null;
        Node<K,V> old = null;
        if (size > threshold || (tab = table) == null ||
            (n = tab.length) == 0)
            n = (tab = resize()).length;
        if ((first = tab[i = (n - 1) & hash]) != null) {
            if (first instanceof TreeNode)
                old = (t = (TreeNode<K,V>)first).getTreeNode(hash, key);
            else {
                Node<K,V> e = first; K k;
                do {
                    if (e.hash == hash &&
                        ((k = e.key) == key || (key != null && key.equals(k)))) {
                        old = e;
                        break;
                    }
                    ++binCount;
                } while ((e = e.next) != null);
            }
        }
        if (old != null) {
            V v;
            if (old.value != null)
                v = remappingFunction.apply(old.value, value);
            else
                v = value;
            if (v != null) {
                old.value = v;
                afterNodeAccess(old);
            }
            else
                removeNode(hash, key, null, false, true);
            return v;
        }
        if (value != null) {
            if (t != null)
                t.putTreeVal(this, tab, hash, key, value);
            else {
                tab[i] = newNode(hash, key, value, first);
                if (binCount >= TREEIFY_THRESHOLD - 1)
                    treeifyBin(tab, hash);
            }
            ++modCount;
            ++size;
            afterNodeInsertion(true);
        }
        return value;
    }

    // 202011120 使用key和value进行比较得到的迭代结果
    @Override
    public void forEach(BiConsumer<? super K, ? super V> action) {
        Node<K,V>[] tab;
        if (action == null)
            throw new NullPointerException();
        if (size > 0 && (tab = table) != null) {
            int mc = modCount;
            for (int i = 0; i < tab.length; ++i) {
                for (Node<K,V> e = tab[i]; e != null; e = e.next)
                    action.accept(e.key, e.value);
            }
            if (modCount != mc)
                throw new ConcurrentModificationException();
        }
    }

    // 20201120 通过key和value的比较进行替换旧值
    @Override
    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        Node<K,V>[] tab;
        if (function == null)
            throw new NullPointerException();
        if (size > 0 && (tab = table) != null) {
            int mc = modCount;
            for (int i = 0; i < tab.length; ++i) {
                for (Node<K,V> e = tab[i]; e != null; e = e.next) {
                    e.value = function.apply(e.key, e.value);
                }
            }
            if (modCount != mc)
                throw new ConcurrentModificationException();
        }
    }

    /* ------------------------------------------------------------ */
    // Cloning and serialization

    /**
     * Returns a shallow copy of this <tt>HashMap</tt> instance: the keys and
     * values themselves are not cloned.
     *
     * @return a shallow copy of this map
     */
    // 200211120 返回这个<tt>HashMap</tt>实例的一个浅拷贝：键和值本身不被克隆。
    @SuppressWarnings("unchecked")
    @Override
    public Object clone() {
        HashMap<K,V> result;
        try {
            result = (HashMap<K,V>)super.clone();
        } catch (CloneNotSupportedException e) {
            // this shouldn't happen, since we are Cloneable
            throw new InternalError(e);
        }
        result.reinitialize();
        result.putMapEntries(this, false);
        return result;
    }

    // These methods are also used when serializing HashSets // 20201120 在序列化HashSets也使用这些方法
    // 20201120 获取加载因子
    final float loadFactor() {
        return loadFactor;
    }

    // 20201120 获取散列表容量 => 如果散列表不为空, 则返回散列表当前的长度, 否则判断阈值是否大于0, 如果大于0则返回阈值, 否则返回默认容量16
    final int capacity() {
        return (table != null) ? table.length : (threshold > 0) ? threshold : DEFAULT_INITIAL_CAPACITY;
    }

    /**
     * Save the state of the <tt>HashMap</tt> instance to a stream (i.e.,
     * serialize it).
     *
     * @serialData The <i>capacity</i> of the HashMap (the length of the
     *             bucket array) is emitted (int), followed by the
     *             <i>size</i> (an int, the number of key-value
     *             mappings), followed by the key (Object) and value (Object)
     *             for each key-value mapping.  The key-value mappings are
     *             emitted in no particular order.
     */
    // 202011120 序列化时调用的写方法
    private void writeObject(java.io.ObjectOutputStream s)
        throws IOException {
        int buckets = capacity();
        // Write out the threshold, loadfactor, and any hidden stuff
        s.defaultWriteObject();
        s.writeInt(buckets);
        s.writeInt(size);
        internalWriteEntries(s);
    }

    /**
     * Reconstitute the {@code HashMap} instance from a stream (i.e.,
     * deserialize it).
     */
    // 20201120 反序列化调用的读方法
    private void readObject(java.io.ObjectInputStream s)
        throws IOException, ClassNotFoundException {
        // Read in the threshold (ignored), loadfactor, and any hidden stuff
        s.defaultReadObject();
        reinitialize();
        if (loadFactor <= 0 || Float.isNaN(loadFactor))
            throw new InvalidObjectException("Illegal load factor: " +
                                             loadFactor);
        s.readInt();                // Read and ignore number of buckets
        int mappings = s.readInt(); // Read number of mappings (size)
        if (mappings < 0)
            throw new InvalidObjectException("Illegal mappings count: " +
                                             mappings);
        else if (mappings > 0) { // (if zero, use defaults)
            // Size the table using given load factor only if within
            // range of 0.25...4.0
            float lf = Math.min(Math.max(0.25f, loadFactor), 4.0f);
            float fc = (float)mappings / lf + 1.0f;
            int cap = ((fc < DEFAULT_INITIAL_CAPACITY) ?
                       DEFAULT_INITIAL_CAPACITY :
                       (fc >= MAXIMUM_CAPACITY) ?
                       MAXIMUM_CAPACITY :
                       tableSizeFor((int)fc));
            float ft = (float)cap * lf;
            threshold = ((cap < MAXIMUM_CAPACITY && ft < MAXIMUM_CAPACITY) ?
                         (int)ft : Integer.MAX_VALUE);
            @SuppressWarnings({"rawtypes","unchecked"})
                Node<K,V>[] tab = (Node<K,V>[])new Node[cap];
            table = tab;

            // Read the keys and values, and put the mappings in the HashMap
            for (int i = 0; i < mappings; i++) {
                @SuppressWarnings("unchecked")
                    K key = (K) s.readObject();
                @SuppressWarnings("unchecked")
                    V value = (V) s.readObject();
                putVal(hash(key), key, value, false, false);
            }
        }
    }

    /* ------------------------------------------------------------ */
    // iterators
    // HashMap迭代器
    abstract class HashIterator {
        Node<K,V> next;        // next entry to return
        Node<K,V> current;     // current entry
        int expectedModCount;  // for fast-fail
        int index;             // current slot

        HashIterator() {
            expectedModCount = modCount;
            Node<K,V>[] t = table;
            current = next = null;
            index = 0;
            if (t != null && size > 0) { // advance to first entry
                do {} while (index < t.length && (next = t[index++]) == null);
            }
        }

        public final boolean hasNext() {
            return next != null;
        }

        // 20201119 返回下一个结点
        final Node<K,V> nextNode() {
            Node<K,V>[] t;
            Node<K,V> e = next;
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
            if (e == null)
                throw new NoSuchElementException();
            if ((next = (current = e).next) == null && (t = table) != null) {
                do {} while (index < t.length && (next = t[index++]) == null);
            }
            return e;
        }

        public final void remove() {
            Node<K,V> p = current;
            if (p == null)
                throw new IllegalStateException();
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
            current = null;
            K key = p.key;
            removeNode(hash(key), key, null, false, false);
            expectedModCount = modCount;
        }
    }

    // 20201120 key的迭代器
    final class KeyIterator extends HashIterator implements Iterator<K> {
        public final K next() { return nextNode().key; }
    }

    // 20201120 value的迭代器
    final class ValueIterator extends HashIterator implements Iterator<V> {
        public final V next() { return nextNode().value; }
    }

    // 20201119 继承HashIterator
    final class EntryIterator extends HashIterator
        implements Iterator<Map.Entry<K,V>> {

        // 20201119  迭代器迭代方法 => 返回下一个结点
        public final Map.Entry<K,V> next() { return nextNode(); }
    }

    /* ------------------------------------------------------------ */
    // spliterators

    static class HashMapSpliterator<K,V> {
        final HashMap<K,V> map;
        Node<K,V> current;          // current node
        int index;                  // current index, modified on advance/split
        int fence;                  // one past last index
        int est;                    // size estimate
        int expectedModCount;       // for comodification checks

        HashMapSpliterator(HashMap<K,V> m, int origin,
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
                HashMap<K,V> m = map;
                est = m.size;
                expectedModCount = m.modCount;
                Node<K,V>[] tab = m.table;
                hi = fence = (tab == null) ? 0 : tab.length;
            }
            return hi;
        }

        public final long estimateSize() {
            getFence(); // force init
            return (long) est;
        }
    }

    static final class KeySpliterator<K,V>
        extends HashMapSpliterator<K,V>
        implements Spliterator<K> {
        KeySpliterator(HashMap<K,V> m, int origin, int fence, int est,
                       int expectedModCount) {
            super(m, origin, fence, est, expectedModCount);
        }

        public KeySpliterator<K,V> trySplit() {
            int hi = getFence(), lo = index, mid = (lo + hi) >>> 1;
            return (lo >= mid || current != null) ? null :
                new KeySpliterator<>(map, lo, index = mid, est >>>= 1,
                                        expectedModCount);
        }

        public void forEachRemaining(Consumer<? super K> action) {
            int i, hi, mc;
            if (action == null)
                throw new NullPointerException();
            HashMap<K,V> m = map;
            Node<K,V>[] tab = m.table;
            if ((hi = fence) < 0) {
                mc = expectedModCount = m.modCount;
                hi = fence = (tab == null) ? 0 : tab.length;
            }
            else
                mc = expectedModCount;
            if (tab != null && tab.length >= hi &&
                (i = index) >= 0 && (i < (index = hi) || current != null)) {
                Node<K,V> p = current;
                current = null;
                do {
                    if (p == null)
                        p = tab[i++];
                    else {
                        action.accept(p.key);
                        p = p.next;
                    }
                } while (p != null || i < hi);
                if (m.modCount != mc)
                    throw new ConcurrentModificationException();
            }
        }

        public boolean tryAdvance(Consumer<? super K> action) {
            int hi;
            if (action == null)
                throw new NullPointerException();
            Node<K,V>[] tab = map.table;
            if (tab != null && tab.length >= (hi = getFence()) && index >= 0) {
                while (current != null || index < hi) {
                    if (current == null)
                        current = tab[index++];
                    else {
                        K k = current.key;
                        current = current.next;
                        action.accept(k);
                        if (map.modCount != expectedModCount)
                            throw new ConcurrentModificationException();
                        return true;
                    }
                }
            }
            return false;
        }

        public int characteristics() {
            return (fence < 0 || est == map.size ? Spliterator.SIZED : 0) |
                Spliterator.DISTINCT;
        }
    }

    static final class ValueSpliterator<K,V>
        extends HashMapSpliterator<K,V>
        implements Spliterator<V> {
        ValueSpliterator(HashMap<K,V> m, int origin, int fence, int est,
                         int expectedModCount) {
            super(m, origin, fence, est, expectedModCount);
        }

        public ValueSpliterator<K,V> trySplit() {
            int hi = getFence(), lo = index, mid = (lo + hi) >>> 1;
            return (lo >= mid || current != null) ? null :
                new ValueSpliterator<>(map, lo, index = mid, est >>>= 1,
                                          expectedModCount);
        }

        public void forEachRemaining(Consumer<? super V> action) {
            int i, hi, mc;
            if (action == null)
                throw new NullPointerException();
            HashMap<K,V> m = map;
            Node<K,V>[] tab = m.table;
            if ((hi = fence) < 0) {
                mc = expectedModCount = m.modCount;
                hi = fence = (tab == null) ? 0 : tab.length;
            }
            else
                mc = expectedModCount;
            if (tab != null && tab.length >= hi &&
                (i = index) >= 0 && (i < (index = hi) || current != null)) {
                Node<K,V> p = current;
                current = null;
                do {
                    if (p == null)
                        p = tab[i++];
                    else {
                        action.accept(p.value);
                        p = p.next;
                    }
                } while (p != null || i < hi);
                if (m.modCount != mc)
                    throw new ConcurrentModificationException();
            }
        }

        public boolean tryAdvance(Consumer<? super V> action) {
            int hi;
            if (action == null)
                throw new NullPointerException();
            Node<K,V>[] tab = map.table;
            if (tab != null && tab.length >= (hi = getFence()) && index >= 0) {
                while (current != null || index < hi) {
                    if (current == null)
                        current = tab[index++];
                    else {
                        V v = current.value;
                        current = current.next;
                        action.accept(v);
                        if (map.modCount != expectedModCount)
                            throw new ConcurrentModificationException();
                        return true;
                    }
                }
            }
            return false;
        }

        public int characteristics() {
            return (fence < 0 || est == map.size ? Spliterator.SIZED : 0);
        }
    }

    static final class EntrySpliterator<K,V>
        extends HashMapSpliterator<K,V>
        implements Spliterator<Map.Entry<K,V>> {
        EntrySpliterator(HashMap<K,V> m, int origin, int fence, int est,
                         int expectedModCount) {
            super(m, origin, fence, est, expectedModCount);
        }

        public EntrySpliterator<K,V> trySplit() {
            int hi = getFence(), lo = index, mid = (lo + hi) >>> 1;
            return (lo >= mid || current != null) ? null :
                new EntrySpliterator<>(map, lo, index = mid, est >>>= 1,
                                          expectedModCount);
        }

        public void forEachRemaining(Consumer<? super Map.Entry<K,V>> action) {
            int i, hi, mc;
            if (action == null)
                throw new NullPointerException();
            HashMap<K,V> m = map;
            Node<K,V>[] tab = m.table;
            if ((hi = fence) < 0) {
                mc = expectedModCount = m.modCount;
                hi = fence = (tab == null) ? 0 : tab.length;
            }
            else
                mc = expectedModCount;
            if (tab != null && tab.length >= hi &&
                (i = index) >= 0 && (i < (index = hi) || current != null)) {
                Node<K,V> p = current;
                current = null;
                do {
                    if (p == null)
                        p = tab[i++];
                    else {
                        action.accept(p);
                        p = p.next;
                    }
                } while (p != null || i < hi);
                if (m.modCount != mc)
                    throw new ConcurrentModificationException();
            }
        }

        public boolean tryAdvance(Consumer<? super Map.Entry<K,V>> action) {
            int hi;
            if (action == null)
                throw new NullPointerException();
            Node<K,V>[] tab = map.table;
            if (tab != null && tab.length >= (hi = getFence()) && index >= 0) {
                while (current != null || index < hi) {
                    if (current == null)
                        current = tab[index++];
                    else {
                        Node<K,V> e = current;
                        current = current.next;
                        action.accept(e);
                        if (map.modCount != expectedModCount)
                            throw new ConcurrentModificationException();
                        return true;
                    }
                }
            }
            return false;
        }

        public int characteristics() {
            return (fence < 0 || est == map.size ? Spliterator.SIZED : 0) |
                Spliterator.DISTINCT;
        }
    }

    /* ------------------------------------------------------------ */
    // LinkedHashMap support


    /**
     * 20201120
     * 以下包保护的方法设计为被 LinkedHashMap 覆盖，但不能被任何其他子类覆盖。 几乎所有其他内部方法也是包保护的，但被声明为 final，因此可以被
     * LinkedHashMap、视图类和 HashSet 使用。
     */
    /*
     * The following package-protected methods are designed to be
     * overridden by LinkedHashMap, but not by any other subclass.
     * Nearly all other internal methods are also package-protected
     * but are declared final, so can be used by LinkedHashMap, view
     * classes, and HashSet.
     */
    // 创建一个常规（非树）节点
    // Create a regular (non-tree) node
    Node<K,V> newNode(int hash, K key, V value, Node<K,V> next) {
        return new Node<>(hash, key, value, next);
    }

    // For conversion from TreeNodes to plain nodes
    // 用于从 TreeNodes 到普通节点的转换
    Node<K,V> replacementNode(Node<K,V> p, Node<K,V> next) {
        // 20201119 使用当前结点p, 下一结点next, 构造普通结点
        return new Node<>(p.hash, p.key, p.value, next);
    }

    // Create a tree bin node
    // 创建红黑树结点
    TreeNode<K,V> newTreeNode(int hash, K key, V value, Node<K,V> next) {
        return new TreeNode<>(hash, key, value, next);
    }

    // For treeifyBin
    // 创建新红黑树结点-用于替换结点
    TreeNode<K,V> replacementTreeNode(Node<K,V> p, Node<K,V> next) {
        return new TreeNode<>(p.hash, p.key, p.value, next);
    }

    /**
     * 20210608
     * 重置为初始默认状态。 由 clone 和 readObject 调用。
     */
    /**
     * Reset to initial default state.  Called by clone and readObject.
     */
    void reinitialize() {
        table = null;
        entrySet = null;
        keySet = null;
        values = null;
        modCount = 0;
        threshold = 0;
        size = 0;
    }

    // Callbacks to allow LinkedHashMap post-actions
    // 回调以允许 LinkedHashMap 后操作
    void afterNodeAccess(Node<K,V> p) { }
    void afterNodeInsertion(boolean evict) { }
    void afterNodeRemoval(Node<K,V> p) { }

    // Called only from writeObject, to ensure compatible ordering.
    // 仅从 writeObject 调用，以确保兼容排序。
    void internalWriteEntries(java.io.ObjectOutputStream s) throws IOException {
        Node<K,V>[] tab;
        if (size > 0 && (tab = table) != null) {
            for (int i = 0; i < tab.length; ++i) {
                for (Node<K,V> e = tab[i]; e != null; e = e.next) {
                    s.writeObject(e.key);
                    s.writeObject(e.value);
                }
            }
        }
    }

    /* ------------------------------------------------------------ */
    // Tree bins

    /**
     * Entry for Tree bins. Extends LinkedHashMap.Entry (which in turn
     * extends Node) so can be used as extension of either regular or
     * linked node.
     */
    // 20201119 HashMap子类-红黑树结点, 双向链表。延伸LinkedHashMap.Entry（反过来扩展节点）所以可以用作常规节点或链接节点的扩展。
    static final class TreeNode<K,V> extends LinkedHashMap.Entry<K,V> {
        TreeNode<K,V> parent;  // red-black tree links // 20201119 父结点
        TreeNode<K,V> left;// 20201119 左孩子结点
        TreeNode<K,V> right;// 20201119 右孩子结点
        TreeNode<K,V> prev;    // needed to unlink next upon deletion // 20201119 表前指针-删除后需要取消链接
        boolean red;// 20201119 是否为红结点

        // 20201119 调用Map的entry构造方法, 构造红黑树结点
        TreeNode(int hash, K key, V val, Node<K,V> next) {
            super(hash, key, val, next);
        }

        /**
         * Returns root of tree containing this node.
         */
        // 20201119 返回包含此节点的树的根。
        final TreeNode<K,V> root() {
            // 20201119 从当前结点向上遍历, 找到根结点(没有父结点的结点)则返回
            for (TreeNode<K,V> r = this, p;;) {
                if ((p = r.parent) == null)
                    return r;
                r = p;
            }
        }

        /**
         * Ensures that the given root is the first node of its bin.
         */
        // 20201119 确保给定的根是其 bin 的第一个节点。 => 移动指定的root结点到散列表桶中
        static <K,V> void moveRootToFront(Node<K,V>[] tab, TreeNode<K,V> root) {
            int n;

            // 20201120 如果根结点不为空, 散列表不为空
            if (root != null && tab != null && (n = tab.length) > 0) {
                // 20201119 获取root结点的桶索引 = (容量 - 1) & hash = hash % (容量 - 1)
                int index = (n - 1) & root.hash;

                // 20201119 获取index处的桶first
                TreeNode<K,V> first = (TreeNode<K,V>)tab[index];

                // 20201119 如果桶的头节点不为根结点, 则需要进行根结点移动
                if (root != first) {
                    Node<K,V> rn;

                    // 20201119 设置根结点为桶的头结点
                    tab[index] = root;

                    // 20201119 获取该桶的表前指针rp
                    TreeNode<K,V> rp = root.prev;

                    // 20201119 如果该桶存在表后指针rn
                    if ((rn = root.next) != null)
                        // 20201119 则链接前指针和后指针
                        ((TreeNode<K,V>)rn).prev = rp;

                    // 20201119 如果该桶的前指针存在
                    if (rp != null)
                        // 20201119 则链接后指针和前指针
                        rp.next = rn;

                    // 20201119 如果原来桶头结点不为空
                    if (first != null)
                        // 20201119 则设置根结点为原来桶头结点的前指针
                        first.prev = root;

                    // 20201119 设置原来桶头结点为根结点的后指针, 相当于first作为下一个桶
                    root.next = first;

                    // 20201119 桶头结点的前指针置为空
                    root.prev = null;
                }

                // 20201119 递归进行红黑树结点检查根结点
                assert checkInvariants(root);
            }
        }

        /**
         * 20210605
         * 使用给定的散列和键查找从根 p 开始的节点。 kc 参数在第一次使用比较键时缓存可比较ClassFor(key)。
         */
        /**
         * Finds the node starting at root p with the given hash and key.
         * The kc argument caches comparableClassFor(key) upon first use
         * comparing keys.
         */
        // 根据hash值和key，从根结点开始查找
        final TreeNode<K,V> find(int h, Object k, Class<?> kc) {
            // 20201119 初始化结点P结点
            TreeNode<K,V> p = this;// 20201119 this代表调用find()的实例结点

            // 20201119 开始循环
            do {
                // 20201119 初始化p结点的hash ph, 比较结果dir, p结点的key pk
                int ph, dir; K pk;

                // 20201119 初始化p的左孩子结点pl, 右孩子结点pr, 查找结果结点q
                TreeNode<K,V> pl = p.left, pr = p.right, q;

                // 20201119 如果哈希ph 大于 h
                if ((ph = p.hash) > h)
                    p = pl;// 20201119 则比较结果在左子树
                else if (ph < h)
                    p = pr;// 20201119 否则在右子树
                else if ((pk = p.key) == k || (k != null && k.equals(pk)))// 20201119 否则如果哈希相等, 且key值也相等
                    return p;// 20201119 则代表就是这个结点, 直接返回p
                else if (pl == null)// 20201119 如果哈希相等, 但key值不相等, 且左孩子为叶子结点
                    p = pr;// 20201119 则比较结果在右子树
                else if (pr == null)// 20201119 如果哈希相等, 但key值不相等, 且右孩子为叶子结点
                    p = pl;// 20201119 则比较结果在左子树
                else if ((kc != null ||
                          (kc = comparableClassFor(k)) != null) &&
                         (dir = compareComparables(kc, k, pk)) != 0)// 20201119 如果都有左右孩子且有比较器, 则使用比较器比较key值 k和pk
                    p = (dir < 0) ? pl : pr;// 20201119 如果k比较小, 则比较结果在左子树, 否则在右子树
                else if ((q = pr.find(h, k, kc)) != null)// 20201119 如果使用比较器比较key值的结果相等
                    return q;// 20201119 递归完成返回查找结果q
                else
                    p = pl;// 20201119 如果q结果为空, 则比较结果在左子树
            } while (p != null);// 20201119 继续循环查找p子树

            return null;
        }

        /**
         * Calls find for root node.
         */
        // 20201119 调用 find 根节点。
        final TreeNode<K,V> getTreeNode(int h, Object k) {
            // 20201119 如果父节点为空, 则该结点就是根结点, 如果父节点不为空, 则使用hash和key值去遍历查找根结点
            return ((parent != null) ? root() : this).find(h, k, null);
        }

        /**
         * 20201119
         * 当 hashCode 相等且不可比较时，用于对插入进行排序的打破平局实用程序。 我们不需要总顺序，只需要一致的插入规则来保持重新平衡之间的等效性。
         * 超出必要的打破平局会稍微简化测试。
         */
        /**
         * Tie-breaking utility for ordering insertions when equal
         * hashCodes and non-comparable. We don't require a total
         * order, just a consistent insertion rule to maintain
         * equivalence across rebalancings. Tie-breaking further than
         * necessary simplifies testing a bit.
         */
        // 当hashCode相等且不可比较时, 用于打破比较平局的情况 => 比较a和b两个对象的ClassName相等以及原始hashCode
        static int tieBreakOrder(Object a, Object b) {
            int d;
            if (a == null || b == null || (d = a.getClass().getName().compareTo(b.getClass().getName())) == 0)
                d = (System.identityHashCode(a) <= System.identityHashCode(b) ? -1 : 1);
            return d;
        }

        /**
         * 20210605
         * 形成从此节点链接的节点的树。
         */
        /**
         * Forms tree of the nodes linked from this node.
         * @return root of tree
         */
        // 20201119 红黑树化实例结点链表
        final void treeify(Node<K,V>[] tab) {
            // 20201119 初始化红黑树根结点
            TreeNode<K,V> root = null;

            // 20201119 从当前结点开始遍历链表
            for (TreeNode<K,V> x = this, next; x != null; x = next) {
                next = (TreeNode<K,V>)x.next;

                // 20201119 初始化左右孩子结点
                x.left = x.right = null;

                // 20201119 如果树为空
                if (root == null) {
                    // 20201119 则设置当前结点为根结点(黑色)
                    x.parent = null;
                    x.red = false;
                    root = x;
                }
                else {
                    // 20201119 否则设置k为x的键, h为x的hash
                    K k = x.key;
                    int h = x.hash;

                    // 20201119 初始化键值Class对象数组比较参数
                    Class<?> kc = null;

                    // 20201119 遍历根结点
                    for (TreeNode<K,V> p = root;;) {
                        // 20201119 初始化比较结果dir, p的hash ph
                        int dir, ph;
                        K pk = p.key;

                        // 20201119 如果p的hash大于当前结点的hash
                        if ((ph = p.hash) > h)
                            dir = -1;// 20201119 则比较结果为-1
                        else if (ph < h)
                            dir = 1;// 20201119 否则比较结果为1
                        else if ((kc == null &&
                                  (kc = comparableClassFor(k)) == null) ||
                                 (dir = compareComparables(kc, k, pk)) == 0)
                            // 20201119 如果比较相等, 则进行hashCode比较
                            dir = tieBreakOrder(k, pk);

                        // 20201119 备份根节点开始指针
                        TreeNode<K,V> xp = p;

                        // 20201119 如果比较结果<=0, 则下次遍历取左孩子结点, 否则取右孩子结点
                        if ((p = (dir <= 0) ? p.left : p.right) == null) {
                            x.parent = xp;// 20201120 设置当前结点x的父节点为当前比较结点xp
                            if (dir <= 0)// 20201119 如果比较结果<=0
                                xp.left = x;// 20201119 则设置当前比较结点的左孩子为当前结点x
                            else
                                xp.right = x;// 20201119 则设置当前比较结点的右孩子为当前结点x

                            // 20201119 插入后平衡红黑树
                            root = balanceInsertion(root, x);
                            break;
                        }
                    }
                }
            }

            // 20201119 移动根结点到散列表桶中的第一个元素
            moveRootToFront(tab, root);
        }

        /**
         * 20210605
         * 返回替换从该节点链接的非 TreeNode 的列表。
         */
        /**
         * Returns a list of non-TreeNodes replacing those linked from
         * this node.
         */
        // 普通化红黑树结点链表
        final Node<K,V> untreeify(HashMap<K,V> map) {
            // 20201119 初始化hd、tl普通结点
            Node<K,V> hd = null, tl = null;

            // 20201119 q结点位当前结点, 遍历当前链表
            for (Node<K,V> q = this; q != null; q = q.next) {
                // 20201119 构造普通结点
                Node<K,V> p = map.replacementNode(q, null);

                // 20201119 如果tl链表为空
                if (tl == null)
                    hd = p;// 20201119 则初始化hd链表 => 作为tl结点的头指针
                else
                    tl.next = p;// 20201119 否则最加到tl链表中
                tl = p;// 20201119 初始化tl链表
            }

            // 20201119 返回hd链表, 即返回普通链表的头指针
            return hd;
        }

        /**
         * Tree version of putVal.
         */
        // 20201120 红黑树的结点添加方法 => 插入新结点会返回null, 插入失败则返回失败结点
        final TreeNode<K,V> putTreeVal(HashMap<K,V> map, Node<K,V>[] tab, int h, K k, V v) {
            // 202011120 比较器Class类kc, 搜索结果searched, 根节点root
            Class<?> kc = null;
            boolean searched = false;
            TreeNode<K,V> root = (parent != null) ? root() : this;// 20201120 遍历查找根结点

            // 20201120 从根结点开始查找
            for (TreeNode<K,V> p = root;;) {
                int dir, ph; K pk;

                // 20201120 当前结点p, 当前节点的hash ph
                if ((ph = p.hash) > h)
                    // 20201120 如果ph大于目标hash, 则比较结果dir为-1
                    dir = -1;

                // 20201120 如果小于hash, 则dir为1
                else if (ph < h)
                    dir = 1;

                // 20201120 如果等于hash, 且key值也相等
                else if ((pk = p.key) == k || (k != null && k.equals(pk)))
                    // 20201119 则返回当前结点, 不用添加, 因为hash相等, 说明value和key都相等
                    return p;

                // 20201120 如果hash相等且key值不等, 但用比较器比较key值相等时
                else if ((kc == null && (kc = comparableClassFor(k)) == null) ||(dir = compareComparables(kc, k, pk)) == 0) {
                    // 20201120 如果还没找到
                    if (!searched) {
                        // 20201120 则递归查找左右子树, 直到找到指定hash和key匹配的结点q
                        TreeNode<K,V> q, ch;
                        searched = true;
                        if (((ch = p.left) != null && (q = ch.find(h, k, kc)) != null) || ((ch = p.right) != null && (q = ch.find(h, k, kc)) != null))
                            return q;
                    }

                    // 20201120 比较传入的键值和当前结点的键值, 比较结果dir
                    dir = tieBreakOrder(k, pk);
                }

                // 20201120 找不到, 则根据dir进行插入新结点, 如果比较结果dir <=, 则从左子树开始找, 如果>0, 则从右子树开始找
                TreeNode<K,V> xp = p;
                if ((p = (dir <= 0) ? p.left : p.right) == null) {
                    // 20201120 当前结点xp, 结点的后指针xpn, 新红黑树结点x
                    Node<K,V> xpn = xp.next;
                    TreeNode<K,V> x = map.newTreeNode(h, k, v, xpn);

                    // 20201120 如果比较结果<=0
                    if (dir <= 0)
                        // 20201120 则设置红黑树结点x为当前节点xp的左孩子
                        xp.left = x;
                    else
                        // 20201120 否则设置红黑树结点x为当前结点xp的右孩子
                        xp.right = x;

                    // 20201120 设置当前结点xp的后指针为x
                    xp.next = x;

                    // 20201120 关联xp结点与x的关系
                    x.parent = x.prev = xp;

                    // 20201120 如果以前的后指针xpn不为空
                    if (xpn != null)
                        // 20201120 则设定红黑树结点x为xpn的前指针
                        ((TreeNode<K,V>)xpn).prev = x;

                    // 20201120 插入x后平衡红黑树, 然后指定root结点到桶头
                    moveRootToFront(tab, balanceInsertion(root, x));

                    // 20201120 插入新结点时返回null
                    return null;
                }
            }
        }

        /**
         * 20201119
         * 删除给定节点，该节点必须在此调用之前存在。这比典型的红黑删除代码更混乱，因为我们无法将内部节点的内容与由“next”指针固定的叶后继节点交换遍历。
         * 因此，我们交换树链接。 如果当前树的节点似乎太少，则将 bin 转换回普通 bin。 （测试会在 2 到 6 个节点之间触发，具体取决于树结构）。
         */
        /**
         * Removes the given node, that must be present before this call.
         * This is messier than typical red-black deletion code because we
         * cannot swap the contents of an interior node with a leaf
         * successor that is pinned by "next" pointers that are accessible
         * independently during traversal. So instead we swap the tree
         * linkages. If the current tree appears to have too few nodes,
         * the bin is converted back to a plain bin. (The test triggers
         * somewhere between 2 and 6 nodes, depending on tree structure).
         */
        // 20201119 红黑树结点的删除方法
        final void removeTreeNode(HashMap<K,V> map, Node<K,V>[] tab, boolean movable) {
            int n;
            if (tab == null || (n = tab.length) == 0)
                return;

            // 20201119 当前结点的新hash
            int index = (n - 1) & hash;

            // 20201119 桶头结点first, 根结点root, 左孩子结点rl, 前结点pred, 后结点succ
            TreeNode<K,V> first = (TreeNode<K,V>)tab[index], root = first, rl;
            TreeNode<K,V> succ = (TreeNode<K,V>)next, pred = prev;

            // 20201121 如果前结点pred为空, 则说明当前结点为桶头结点, 这时由于是删除该结点, 所以可以把first & succ结点作为桶头结点
            if (pred == null)
                tab[index] = first = succ;
            else
                // 20201121 否则说明当前结点不为桶头结点, 是树内结点, 这时需要链接pred、succ结点
                pred.next = succ;

            if (succ != null)
                succ.prev = pred;// 20201119 则链接pred、succ结点

            // 20201121 如果链接pred、succ结点后, 桶头结点为空, 说明桶内已经没有数据了, 所以不用再处理, 直接返回
            if (first == null)
                return;

            // 20201121 如果桶内还有数据, 且如果根结点的父节点不为空
            if (root.parent != null)
                root = root.root();// 20021119 则设置遍历红黑树找到新的root结点作为根结点

            // 20201121 如果根节点为null 或者 根节点只有1个结点时, 说明红黑树的结点太少了
            if (root == null || root.right == null || (rl = root.left) == null || rl.left == null) {
                // 20201121 则时需要当前红黑树进行拆除退化成链表
                tab[index] = first.untreeify(map);  // too small
                return;
            }

            /**
             * 20201122
             * 删除红黑树结点:(不考虑调整) => 替代的意思是把替代结点和原删除结点的内容互换, 相当于原删除结点交换到了替代结点位置, 替代结点交换到了原删除结点的位置
             *      A. 删除的结点是叶子结点 => 则直接删除即可
             *      B. 删除的结点只有一个孩子结点 => 则使用孩子结点进行替代(也就是查找前驱 | 后继结点作为替代结点), 删除孩子结点
             *      C. 删除的结点有两个孩子结点 => 则查找删除结点的前驱 | 后继结点作为替代结点, 删除替代结点即可, 但这时候会有2种情况:
             *          C.1. 替代结点没有子结点 => 则直接删除替代结点即可
             *          C.2. 替代结点只有1个子结点 => 则使用孩子结点进行替代(也就是查找前驱 | 后继结点作为替代结点), 删除孩子结点, 也就是2次替代
             */
            TreeNode<K,V> p = this, pl = left, pr = right, replacement;

            // 20201122 如果删除结点有两个孩子结点
            if (pl != null && pr != null) {
                TreeNode<K,V> s = pr, sl;

                // 20021122 则一路遍历当前结点的右孩子pr的左孩子sl, 直到sl为叶子结点 => 查找删除结点p的后继结点s
                while ((sl = s.left) != null) // find successor
                    s = sl;

                // 20201122 交换后继结点s和删除结点p的颜色
                boolean c = s.red; s.red = p.red; p.red = c; // swap colors

                // 20201122 获取后继结点s的右孩子sr, 删除结点p的父结点pp
                TreeNode<K,V> sr = s.right;
                TreeNode<K,V> pp = p.parent;

                // 20201122 如果后继结点s直接为删除结点p的右孩子, 则更新p.parent, s.right, 这时s已经放到了p的位置, p已经放到了s的位置(但指针关系还没更新完)
                if (s == pr) { // p was s's direct parent
                    p.parent = s;
                    s.right = p;
                } else {// 20201122 如果后继结点s不是删除结点p的孩子, 因为s是从pr开始找的(如果s不直接为pr那么说明s肯定不是p的孩子)
                    // 20201122 => 则更新p.parent, sp.left | sp.right, s.right, pr.parent
                    TreeNode<K,V> sp = s.parent;

                    if ((p.parent = sp) != null) {
                        if (s == sp.left)
                            sp.left = p;
                        else
                            sp.right = p;
                    }

                    if ((s.right = pr) != null)
                        pr.parent = s;
                }

                // 20201122 清空p的左结点关系, 这时s已经放到了p的位置, p已经放到了s的位置, 已更新p.parent, s.right
                p.left = null;// 20201122 更新p.left

                // 20201122 更新p.right, sr.parent
                if ((p.right = sr) != null)
                    sr.parent = p;

                // 20201122 更新s.left, pl.parent
                if ((s.left = pl) != null)
                    pl.parent = s;

                // 20201122 更新s.parent, pp.left | pp.right
                if ((s.parent = pp) == null)
                    root = s;
                else if (p == pp.left)
                    pp.left = s;
                else
                    pp.right = s;

                // 20201122 到这步更新完的有: 可见s和p对其他结点的指针关系已经更新, 现在s在p以前的位置, p在s以前的位置, 颜色也已经更换完 => 所以现在s就是原删除结点, p就是后继结点
                // 20201122 p: p.parent、pr.parent、p.left、p.right、pl.parent、pp.left | pp.right
                // 20201122 s: s.right、sp.left | sp.right、sr.parent、s.left、s.parent

                // 20201122 设置替代结点replacement
                if (sr != null)// 20201122 这里由于原后继结点s是通过遍历原删除结点p的右孩子所有的左孩子找到的, 所以原s肯定是没有左孩子的, 如果再判断没有右孩子, 则可以说明原s肯定是没有孩子的
                    replacement = sr;// 20201122 如果原后继结点有右孩子, 则该右孩子sr为替代结点replacement(相当于情况C.2)
                else
                    replacement = p;// 20201122 如果原后继结点没有右孩子, 则该后继结点p为替代结点replacement(相当于情况C.1)
            }

            // 20201122 如果删除结点p只有一个孩子, 当左孩子pl不为空时
            else if (pl != null)
                replacement = pl;// 20201122 则替换结点为该左孩子pl

            // 20201122 当删除结点p的右孩子pr不为空时
            else if (pr != null)
                replacement = pr;// 20201122 则替换结点为该右孩子pr

            // 20201122 删除结点为叶子结点, 这时删除结点本身就是替代结点
            else
                replacement = p;// 20201121 设置删除结点p为替换结点replacement

            // 20201122 到这一步原删除结点真正的替代结点已经找到, 为replacement
            // 20201122 如果替换结点replacement不为p结点, 说明删除对应的是2-3-4树的3结点, 即情况C.2和B, 下面开始脱离p结点, 使用replacement顶替p结点
            if (replacement != p) {
                // 20201122 则设置当前p结点的父节点pp为替代结点replacement的父结点
                TreeNode<K,V> pp = replacement.parent = p.parent;

                // 20201122 如果父节点pp为空
                if (pp == null)
                    // 20201122 则更新根结点为替换结点replacement
                    root = replacement;

                // 20201122 如果不为空, 且当前p结点为父节点pp的左孩子
                else if (p == pp.left)
                    // 20201122 则更新父节点pp的左孩子为替换结点replacement
                    pp.left = replacement;
                else
                    // 20201122 否则更新父节点pp的右孩子为替换结点replacement
                    pp.right = replacement;

                // 20201122 清空当前删除结点p => 通知GC回收
                p.left = p.right = p.parent = null;

                // 20201122 到这一步, 情况C.2和B的替代结点replacement已经替换到了当前p结点的位置, 相当于完成了p的删除操作
                // 20201122 也就是相当于删除了2次替代的替代结点(但不是这里的replacement, 而是这里的p, 因为这里的算法进行了优化, 并没有发生2次替代的值和指针的互换, 而是直接删除了p然后replacement占据p的位置)
            }

            /**
             * 20201122
             * 删除红黑树结点:(不考虑调整) => 替代的意思是把替代结点和原删除结点的内容互换, 相当于原删除结点交换到了替代结点位置, 替代结点交换到了原删除结点的位置
             *      A. 删除的结点是叶子结点 => 则直接删除即可
             *      B. 删除的结点只有一个孩子结点 => 则使用孩子结点进行替代(也就是查找前驱 | 后继结点作为替代结点), 删除孩子结点
             *      C. 删除的结点有两个孩子结点 => 则查找删除结点的前驱 | 后继结点作为替代结点, 删除替代结点即可, 但这时候会有2种情况:
             *          C.1. 替代结点没有子结点 => 则直接删除替代结点即可
             *          C.2. 替代结点只有1个子结点 => 则使用孩子结点进行替代(也就是查找前驱 | 后继结点作为替代结点), 删除孩子结点, 也就是2次替代
             */
            /**
             * 20201122 到这一步存在6种可能(考虑调整):
             *      a. pl != null && pr != null => 即删除结点p那位置的那个结点pi, 有两个孩子结点:
             *          => 这时需要找前驱 | 后继结点作为替代结点, HashMap用的后继结点s, 找到后又分为两种情况:
             *              a.1. 后继节点s有右孩子sr(肯定没有左孩子), 则replacement为sr(sr替代后继结点s, 而s又是替代pi, 属于2次替代), 对应情况C.2 => 对应c., 需要进行调整
             *              a.2. 后继结点s没有右孩子, 说明s没有孩子, 则replacement为s, 对应情况C.1 => 对应d., 则需要进行调整
             *      b. pl != null && pr == null => pi只有左孩子:
             *          => 这种情况 replacement为pl, 对应情况B => 需要对替代结点进行变色即可, 需要进行调整
             *      c. pl == null && pr != null => pi只有右孩子:
             *          => 这种情况 replacement为pr, 对应情况B => 需要对替代结点进行变色即可, 需要进行调整
             *      d. pi一个孩子都没:
             *          => 这种情况 replacement为p, 对应情况A => 如果替代结点是红色, 则直接删除即可; 如果替代结点是黑色, 则需要进行调整
             */
            // 20201122 删除后调整红黑树, 对替代结点replacement进行平衡, 每种情况都有调整的必要
            TreeNode<K,V> r = p.red ? root : balanceDeletion(root, replacement);

            // 20201122 平衡后, 如果替代节点replacement为当前结点 => 说明删除对应的是2-3-4树的2结点(而4结点其实对应的是删除红色且没有子结点的结点),
            // 20201122 即情况A, 下面开始脱离p结点也即是replacement结点
            if (replacement == p) {  // detach
                // 20201122 获取结点p的父节点pp
                TreeNode<K,V> pp = p.parent;

                // 20201122 清空结点p与父节点pp的关系
                p.parent = null;

                // 20201122 如果父节点pp不为空
                if (pp != null) {
                    // 20201122 且如果结点p是父节点pp的左孩子
                    if (p == pp.left)
                        // 20201122 则清空父节点pp左孩子
                        pp.left = null;

                        // 20201122 且如果结点p是父节点pp的右孩子
                    else if (p == pp.right)
                        // 20201119 则清空父节点pp右孩子
                        pp.right = null;
                }

                // 20201122 到这里情况A的删除结点已经脱离成功
            }

            // 20201122 至此情况的删除结点都已经处理完毕, 如果允许移动
            if (movable)
                // 20201122 则移动红黑树的根结点为桶的头结点
                moveRootToFront(tab, r);
        }

        /**
         * 20210605
         * 将树箱中的节点拆分为下树箱和上树箱，或者如果现在太小则取消树化。 仅从调整大小调用； 见上面关于分割位和索引的讨论。
         */
        /**
         * Splits nodes in a tree bin into lower and upper tree bins,
         * or untreeifies if now too small. Called only from resize;
         * see above discussion about split bits and indices.
         *
         * @param map the map // 20201119 当前map对象
         * @param tab the table for recording bin heads // 20201119 分割后存放的新散列表
         * @param index the index of the table being split  // 20201119 要分割的旧散列表桶的位置
         * @param bit the bit of hash to split on       // 20201119 旧的散列表容量
         */
        // 20201120 根据新容量拆分当前桶链表, 会有两种结果newIndex = oldIndex; newIndex = oldIndex + oldCap, HashMap#resize()方法中调用
        final void split(HashMap<K,V> map, Node<K,V>[] tab, int index, int bit) {
            // 20201119 初始化红黑树结点
            TreeNode<K,V> b = this;// 20201119 this指引用该方法的对象, 因为这是他本身的方法, 所以才叫this

            // Relink into lo and hi lists, preserving order
            // 20201119 重新链接到lo和hi列表，保留顺序
            TreeNode<K,V> loHead = null, loTail = null;// 20201119 lo列表
            TreeNode<K,V> hiHead = null, hiTail = null;// 20201119 hi列表

            // 20201119 初始化lo、hi链表容量
            int lc = 0, hc = 0;

            // 20201119 遍历当前结点链表, 初始化lo链表和hi链表
            for (TreeNode<K,V> e = b, next; e != null; e = next) {
                // 20201119 下一结点
                next = (TreeNode<K,V>)e.next;

                // 20201119 置空下一结点
                e.next = null;

                // 20201119 (e.hash & oldCap) == 0, 说明newIndex = oldIndex, 则把结点放到lo链表
                if ((e.hash & bit) == 0) {
                    // 20201119 loTail赋值给前一结点, 如果loTail为空
                    if ((e.prev = loTail) == null)
                        loHead = e;// 20201119 说明lo链表为空, 则初始化loHead
                    else
                        // 20201119 lo链表非空, 则添加e结点到lo链表末尾
                        loTail.next = e;

                    // 20201119 更新lo链表尾结点
                    loTail = e;

                    // 20201119 lo链表容量+1
                    ++lc;
                }
                // 20201119 (e.hash & oldCap) == 1, 说明newIndex = oldIndex + oldCap, 则把结点放到hi链表
                else {
                    // 20201119 hiTail赋值给前一结点, 如果hiTail为空
                    if ((e.prev = hiTail) == null)
                        hiHead = e;// 20201119 说明hi链表为空, 则初始化hiHead
                    else
                        // 20201119 hi链表非空, 添加e结点到hi链表末尾
                        hiTail.next = e;

                    // 20201119 更新hi链表尾结点
                    hiTail = e;

                    // 20201119 hi链表容量+1
                    ++hc;
                }
            }

            // 20201119 如果lo链表不为空
            if (loHead != null) {
                // 20201119 检查lo容量是否小于红黑树拆除阈值
                if (lc <= UNTREEIFY_THRESHOLD)
                    // 20201119 如果li链表阈值<=拆除链表6, 则拆除红黑树
                    tab[index] = loHead.untreeify(map);// 20201119 拆除后返回当前桶的普通链表
                else {
                    // 20201119 否则不需要拆除红黑树, 则该桶头指针位lo链表的头指针
                    tab[index] = loHead;

                    // 20201119 如果hi链表也不为空, 说明原桶链表进行了拆除, 这时还需要重新进行红黑树化lo链表
                    if (hiHead != null) // (else is already treeified)
                        // 20201119 则将lo链表红黑树化
                        loHead.treeify(tab);

                    // 20201120 如果hi链表为空, 说明原桶链表也就是现在全部的lo链表已经是红黑树了, 所以无需再进行红黑树化
                }
            }

            // 20201119 如果hi链表不为空
            if (hiHead != null) {
                // 20201119 检查hi容量是否小于红黑树拆除阈值
                if (hc <= UNTREEIFY_THRESHOLD)
                    // 20201119 拆除红黑树, 存放的桶位置偏移旧容量的大小即2^n
                    tab[index + bit] = hiHead.untreeify(map);// 20201119 拆除后返回当前桶的普通链表
                else {
                    // 20201119 否则不需要拆除红黑树, 则该桶头指针为hi链表的头指针
                    tab[index + bit] = hiHead;

                    // 20201119 如果lo链表也不为空, 说明原桶链表进行了拆除, 这时还需要重新进行红黑树化hi链表
                    if (loHead != null)
                        // 20201119 则将hi链表红黑树化
                        hiHead.treeify(tab);

                    // 20201120 如果lo链表为空, 说明原桶链表也就是现在全部的hi链表已经是红黑树了, 所以无需再进行红黑树化
                }
            }
        }

        /* ------------------------------------------------------------ */
        // Red-black tree methods, all adapted from CLR
        // 20201121 左旋p结点 => 左右左右:
        // 20201121 以p结点为旋转结点进行左旋, p点的右结点r成为p的父结点, r的左结点rl, 成为p的右结点
        static <K,V> TreeNode<K,V> rotateLeft(TreeNode<K,V> root, TreeNode<K,V> p) {
            TreeNode<K,V> r, pp, rl;

            // 20201121 获取p的右节点r, 如果不为空
            if (p != null && (r = p.right) != null) {
                // 20201121 r的左结点rl, 成为p的右结点
                if ((rl = p.right = r.left) != null)
                    // 20201121 如果rl不为空, 则关联rl与p的关系
                    rl.parent = p;

                // 20201121 关联p的父结点与p的右结点的关系
                if ((pp = r.parent = p.parent) == null)
                    // 20201121 如果r结点成为父结点后为根结点, 那么需要变为黑色
                    (root = r).red = false;

                // 20201121 如果r结点没有成为根结点, 关联p的父结点与p的右结点的关系
                else if (pp.left == p)
                    pp.left = r;
                else
                    pp.right = r;

                // 20201121 关联p结点与r结点的关系
                r.left = p;
                p.parent = r;
            }

            // 20201121 返回根结点
            return root;
        }

        // 20201121 右旋p结点 => 右左右左
        // 20201121 以p结点为旋转结点进行右旋, p点的左结点l成为p的父结点, l的右结点lr, 成为p的左结点
        static <K,V> TreeNode<K,V> rotateRight(TreeNode<K,V> root, TreeNode<K,V> p) {
            TreeNode<K,V> l, pp, lr;

            // 20201121 获取p的左结点l, 如果不为空
            if (p != null && (l = p.left) != null) {
                // 20201121 l的右结点lr, 成为p的左结点
                if ((lr = p.left = l.right) != null)
                    // 20201121 如果lr不为空, 则关联lr与p的关系
                    lr.parent = p;

                // 20201121 关联p的父结点与l结点的关系
                if ((pp = l.parent = p.parent) == null)
                    // 20201121 如果l结点成为了根结点, 那么需要将l结点变为黑色
                    (root = l).red = false;

                // 20201121 如果l结点没有成为根结点, 那么关联p的父结点与l结点的关系
                else if (pp.right == p)
                    pp.right = l;
                else
                    pp.left = l;

                // 20201121 关联p与l的关系
                l.right = p;
                p.parent = l;
            }

            // 2020 返回根结点
            return root;
        }

        // 20201119 插入后平衡红黑树 => 到了这步, 红黑树结点的二叉树指针关系已经确定好了的, 只需要进行调整和变色就可以了
        /**
         * 20201121
         * 插入红结点e情况(对应2-3-4树):
         *      a. 空结点新增
         *          a.1. 插入前树中没有结点
         *          a.1. 直接插入e
         *          a.2. 然后变为黑色, 符合2结点要求 => 变色
         *      b. 合并到2结点 => 成为一个3结点
         *          b.1. 插入前是一个2结点 黑
         *          b.2. 插入后, 变为( 上黑下左红 |  上黑下右红 ), 都符合3结点要求 => 无需调整
         *      c. 合并到3结点 => 成为一个4结点
         *          c.1. 插入前是一个3结点( 上黑下左红 |  上黑下右红 )
         *          c.2. 插入后, 一共有6种情况:
         *              c.2.1. 左三(中左左*) 黑红红, 不符合红黑树定义 => 需要调整, 中1 右旋, 中1变红, 左1变黑
         *              c.2.2. 中左右*(其实就相当于左三, 因为对父结点进行左旋即得到左三) 黑红红, 不符合红黑树定义 => 需要调整, 左1左旋, 中1 右旋, 中1变红 新左变黑
         *              c.2.3. 右三(中 右右*) 黑 红红, 不符合红黑树定义 => 需要调整, 中1 左旋, 中1变红 右1变黑
         *              c.2.4. 中 右左*(其实就相当于右三, 因为对父结点进行右旋即得到右三) 黑 红红, 不符合红黑树定义 => 需要调整, 右1右旋, 中1 左旋, 中1变红 新右变黑
         *              c.2.5. 中左 右* 黑红 红, 符合红黑树定义 => 无需调整
         *              c.2.6. 中左* 右 黑红 红, 符合红黑树定义 => 无需调整
         *      d. 合并到4结点 => 成为一个裂变状态
         *         d.1. 插入前是一个4结点(中左 右 黑红 红)
         *         d.2. 插入后, 一共有4种情况:(由于是红黑树, 本质就是二叉树, 所以4结点合并变色即可, 无需考虑向上升元的情况)
         *              d.2.1. 中左左* 右(黑红红 红), 不符合红黑树定义 => 需要调整, 中变红, 左1变黑 左2为红, 右1变黑
         *              d.2.2. 中左右* 右(黑红红 红), 不符合红黑树定义 => 需要调整, 中变红, 左1变黑 右1为红, 右2变黑
         *              d.2.3. 中左 左*右(黑红 红红), 不符合红黑树定义 => 需要调整, 中变红, 左1变黑, 左2为红 右1变黑
         *              d.2.4. 中左 左右*(黑红 红红), 不符合红黑树定义 => 需要调整, 中变红, 左1变黑, 右1变黑 右2为红
         */
        static <K,V> TreeNode<K,V> balanceInsertion(TreeNode<K,V> root, TreeNode<K,V> x) {
            // 20201119 标记当前插入结点为红结点
            x.red = true;

            // 20201119 从当前结点向上遍历, 发现需要调整则进行调整
            for (TreeNode<K,V> xp, xpp, xppl, xppr;;) {
                // 20201119 当前结点父结点xp, 如果为空
                if ((xp = x.parent) == null) {
                    // 20201119 则设置当前结点为黑色 => 根结点
                    x.red = false;
                    return x;
                }

                // 20201119 如果当前结点不是根结点, 如果父结点为黑色 或者 没有爷结点时(相当于情况b.2., 合并到2结点), 则无需调整, 直接返回
                else if (!xp.red || (xpp = xp.parent) == null)
                    return root;// 20201119 则直接返回根结点

                // 20201121 父结点为爷结点的左孩子时, 包含c.2.1. & c.2.2. & d.2.1. & d.2.2.
                // 20201119 否则父结点是红结点, 且存在爷结点, 如果父结点为爷结点的左孩子
                if (xp == (xppl = xpp.left)) {// 20201121 这里的xppl是为了获取当父结点为爷结点右孩子时的叔结点
                    // 20201119 且如果右孩子叔结点不为空, 且为红结点时(相当于情况d.2.1 和 d.2.2, 合并到4结点), 即新结点插入到了爷结点的左结点的孩子结点中
                    if ((xppr = xpp.right) != null && xppr.red) {
                        // 20201119 则标记叔结点为黑色, 父节点为黑色, 爷结点为红色
                        xppr.red = false;
                        xp.red = false;
                        xpp.red = true;

                        // 20201119 爷结点为当前新结点, 进行下一轮的向上调整(是为了保险起见, 如果不循环时不做这步也可以)
                        x = xpp;
                    }
                    else {
                        // 20201121 否则爷结点没有叔结点 或者 有叔结点但为黑色(不会出现, 因为黑色不平衡), 如果当前结点为父节点的右孩子,
                        // 20201121 即父结点为爷结点的左孩子且为红结点, 当前结点插入到父结点的右孩子时(相当于c.2.2. 中左右*, 合并到3结点), 这时将父结点左旋即可得到c.2.1 左三的情况
                        if (x == xp.right) {
                            // 2020119 则对父节点左旋, 且父节点作为新的当前结点 => 相当于当前结点与父结点交换了位置
                            root = rotateLeft(root, x = xp);

                            // 20201119 原来的当前结点作为新的父结点, 如果为空(不会出现), 则新的爷结点为空, 否则新的爷结点为原结点的父节点
                            xpp = (xp = x.parent) == null ? null : xp.parent;

                            // 20201121 这步if起始做的是把c.2.2的情况转换为c.2.1 左三的情况
                        }

                        // 20201119 如果父结点红色, 且不为空, 且当前插入的结点为父亲的左孩子(相当于c.2.1, 左三, 合并到3结点) => 父亲变黑, 爷变红, 爷进行左旋, 变成4结点
                        if (xp != null) {
                            // 20201119 则设置父节点为黑节点
                            xp.red = false;

                            // 20201119 如果爷结点不为空
                            if (xpp != null) {
                                // 20201119 则设置爷结点为红结点
                                xpp.red = true;

                                // 20201119 对爷结点进行右旋
                                root = rotateRight(root, xpp);
                            }
                        }
                    }
                }

                // 20201121 父结点为爷结点的右孩子时, 包含c.2.3. & c.2.4. & d.2.3. & d.2.4.
                // 20201119 同理, 父结点是红结点, 如果父结点为爷结点的右孩子, 进行反向操作
                else {
                    // 20201121 如果叔结点存在且为红结点时(相当于d.2.3 | d.2.4情况) => 这时需要将父叔置黑, 爷置红
                    if (xppl != null && xppl.red) {
                        // 20201121 叔叔结点置为黑结点
                        xppl.red = false;

                        // 20201121 父结点置为黑结点
                        xp.red = false;

                        // 20201121 爷结点置为红结点
                        xpp.red = true;

                        // 20201119 爷结点为当前新结点, 进行下一轮的向上调整(是为了保险起见, 如果不循环时不做这步也可以)
                        x = xpp;
                    }

                    // 20201121 父结点为爷结点的右孩子且为红结点, 且没有叔叔结点或者叔叔结点为黑结点时
                    else {
                        // 20201121 如果当前结点插入到父结点的左孩子时(相当于c.2.4情况) => 将父结点右旋即可得到c.2.3 右三的情况
                        if (x == xp.left) {
                            // 20201121 父结点右旋
                            root = rotateRight(root, x = xp);

                            // 20201121 原来的当前结点作为新的父结点, 如果为空(不会出现), 则新的爷结点为空, 否则新的爷结点为原结点的父节点
                            xpp = (xp = x.parent) == null ? null : xp.parent;

                            // 20201121 这步if起始做的是把c.2.4的情况转换为c.2.3 右三的情况
                        }

                        // 20201121 如果当前结点插入到父结点的右孩子时(相当于c.2.3情况, 右三) => 则需要父结点变黑, 爷结点变红, 爷结点左旋, 变成4结点
                        if (xp != null) {
                            // 20201121 父结点置黑
                            xp.red = false;
                            if (xpp != null) {
                                // 20201121 爷结点置红
                                xpp.red = true;

                                // 20201121 爷结点左旋
                                root = rotateLeft(root, xpp);
                            }
                        }
                    }
                }
            }
        }

        // 20201122 删除前调整红黑树, 对x结点进行调整
        /**
         * 20201122
         * 删除红黑树结点思路:(先不考虑调整) => 替代的意思是把替代结点和原删除结点的内容互换, 相当于原删除结点交换到了替代结点位置, 替代结点交换到了原删除结点的位置
         *      A. 删除的结点是叶子结点 => 则直接删除即可
         *      B. 删除的结点只有一个孩子结点 => 则删除该结点，然后使用孩子结点进行替代(也就是查找前驱 | 后继结点进行替代)
         *      C. 删除的结点有两个孩子结点 => 则查找删除结点的前驱 | 后继结点作为替代结点, 删除替代结点即可, 但这时候会有2种情况:
         *          C.1. 替代结点没有子结点 => 则直接删除替代结点即可
         *          C.2. 替代结点只有1个子结点 => 则删除该结点，然后使用孩子结点进行替代(也就是查找前驱 | 后继结点进行替代)
         */
        /**
         * 20201122 到这一步存在5种可能(考虑调整):
         *      a. pl != null && pr != null => 即删除结点p那位置的那个结点pi, 有两个孩子结点:
         *          => 这时需要找前驱 | 后继结点作为替代结点, HashMap用的后继结点s, 找到后又分为两种情况:
         *              a.1. 后继节点s有右孩子sr(肯定没有左孩子), 则replacement设置为sr, 删除原sr, 对应情况C.2 => 对应c., 删除了黑结点, 需要进行调整
         *              a.2. 后继结点s没有右孩子, 说明s没有孩子, 则replacement为s, 删除s, 对应情况C.1 => 对应d., 无需调整
         *      b. pl != null && pr == null => pi只有左孩子:
         *          => 这种情况 replacement为pl, 对应情况B => 设置replacement为pi, 删除pi, 删除了黑结点, 需要进行调整 => 对替代结点进行变色即可
         *      c. pl == null && pr != null => pi只有右孩子:
         *          => 这种情况 replacement为pr, 对应情况B => 设置replacement为pi, 删除pi, 删除了黑结点, 需要进行调整 => 对替代结点进行变色即可
         *      d. pi一个孩子都没:
         *          => 这种情况 replacement为p, 对应情况A => 如果结点是红色, 则直接删除即可; 如果结点是黑色, 则需要进行调整再删除
         */
        /**
         * 20201122
         * 调整的3种情况:
         *      a. 自己能搞定的自己搞定, 不用跟兄弟和父亲借:
         *          a.1 如果删除的结点对应于2-3-4树的3结点或4结点:
         *              a.1.1. C.1、=> 直接删除, 无需调整
         *              a.1.2. C.2、=> 直接删除, 由于删除的是黑结点，需要变色, 需要调整
         *              a.1.3. B:
         *                  a.1.3.1. 如果删除的是红结点 => 直接删除接口, 无需调整
         *                  a.1.3.2. 如果删除的是黑结点 => 删除后用红结点替代, 然后变黑即可, 由于删除的是黑结点, 需要调整
         *              a.1.4. 红结点的A => 直接删除, 无需调整
         *      b. 搞不定的找兄弟和父亲帮忙(即删除的是黑色的2结点, 相当于黑结点的情况A):
         *          => 前提是找到真正得兄弟结点:
         *              b.0.1. 如果兄弟结点为红色, 说明不是真正的兄弟结点(因为这是为3结点的父结点的红结点), 因为兄弟结点无论是2、3、4结点, 都是黑结点在上的
         *              b.0.2. 这时, 需要对父结点进行左旋, 把本来属于父结点的红结点作为新的父结点, 这样出现在兄弟结点位置的结点才是真正的兄弟结点(黑色)
         *          b.1. 兄弟有得借(即兄弟结点为3结点或4结点):
         *              b.1.1. 兄弟结点为3结点, 即只有一个子结点时:
         *                  b.1.1.1. 兄弟结点的孩子结点借不到: 即父结点旋转后不会断开兄弟结点与孩子结点的关系 => 对父结点旋转, 使得兄弟结点成为新的父结点
         *                  b.1.1.2. 兄弟结点的孩子结点借得到: 即父结点旋转后会断开兄弟结点与孩子结点的关系 => 先对兄弟结点进行旋转, 转成b.1.1.1.情况, 然后对父结点旋转, 使得兄弟结点成为新的父结点
         *                  => 这里孩子结点借不借得到的意思是: 借给父结点做孩子, 其中最后一步对父结点旋转的操作可以跟b.1.2. 的处理逻辑公用(因为都是只旋转一次)
         *              b.1.2. 兄弟结点为4结点, 即有两个子结点时(子结点肯定是红色) => 这里调整方案有2个:
         *                  b.1.2.1. 兄弟结点只借出去1个 => 需要先对兄弟结点旋转, 转出最左的子结点, 然后对父结点旋转, 把该子结点转到父结点位置, 父结点成为兄弟结点的孩子结点, 即需要进行两次旋转; 变色;
         *                  b.1.2.2. 兄弟结点借出去2个 => 需要对父结点旋转, 把兄弟结点转到父结点位置, 兄弟结点的一个子结点转到父结点的孩子处, 父结点成为兄弟的孩子, 即只需1次旋转; 变色;
         *          b.2. 兄弟没得借, 那就有难同当(即兄弟结点为2结点, 一定是黑结点):
         *              b.2.1. 删除该黑色的2结点后, 此时该结点原所在分支比兄弟结点所在分支的黑色结点数量少1 => 需要自损兄弟结点所在分支的一个黑色结点
         *                          b.2.1.1. 将兄弟结点变红, 此时父结点所在的子树黑色平衡
         *                          b.2.1.2. 但父结点以上等祖结点的子树黑色不平衡, 此时需要进行向上递归处理
         *                          b.2.1.3. 直到新父结点为根结点(向上没有别的子树) 或者 新父结点为红结点(因为这时另外一个子树已经变色, 所以只需要将当前新父结点变为黑色, 整棵树即可达到黑色平衡), 则停止递归
         */
        static <K,V> TreeNode<K,V> balanceDeletion(TreeNode<K,V> root, TreeNode<K,V> x) {
            // 20201122 从当前结点x结点开始循环, 不是遍历
            for (TreeNode<K,V> xp, xpl, xpr;;)  {
                // 20201122 如果当前结点x为空, 或者为根结点
                if (x == null || x == root)
                    return root;// 20201122 则无需调整, 直接返回原本的红黑树

                // 20201122 如果当前结点x的父节点xp为空 => 为自损递归终止条件, 则说明此时x结点为根结点, 向上再无其他子树, 此时置黑该结点可以达到黑色平衡, 终止循环递归
                else if ((xp = x.parent) == null) {// 20201123 同时也是删除3结点红结点的终止条件
                    // 20201122 则设置当前结点x为黑结点
                    x.red = false;

                    // 20201122 返回当前结点x作为根节点
                    return x;
                }

                // 20201122 如果当前结点x为红结点 => 为自损递归终止条件, 此时置黑该结点可以达到黑色平衡, 终止循环递归
                else if (x.red) {// 20201122 还有一种情况就是, 删除的结点没有孩子结点且为红结点, 则该结点调整后会被直接删除, 因此这里置黑也没关系
                    // 20201122 则设置当前结点x为黑结点
                    x.red = false;

                    // 20201122 返回根结点
                    return root;
                }

                // 20201122 当前结点x为黑结点(删除2结点 或者 3结点的黑结点时), 如果当前结点x属于父节点的左孩子, 则按左的方式调整 => 这里的xpl也等于x为右孩子时的叔结点
                else if ((xpl = xp.left) == x) {
                    // 20201122 如果叔结点xpr为父节点的右孩子, 且为红结点时(不是真正的兄弟结点, 只是为3结点父结点的红结点)
                    if ((xpr = xp.right) != null && xpr.red) {
                        // 20201122 设置该结点(父结点的红结点)为黑结点
                        xpr.red = false;

                        // 20201122 父节点为红结点
                        xp.red = true;

                        // 20201122 对父结点进行左旋
                        root = rotateLeft(root, xp);

                        // 20201122 更新真正的兄弟结点(黑色)
                        xpr = (xp = x.parent) == null ? null : xp.right;
                    }

                    // 20201122 如果没有真正的兄弟结点, 说明为情况a.1.3.1. 删除的是3结点的红结点
                    if (xpr == null)
                        x = xp;// 20201122 则设置父亲结点为新的当前结点, 使得replacement==p

                    // 20201122 如果真正的兄弟结点存在(肯定为黑结点)
                    else {
                        // 20201122 获取真正的兄弟结点的左孩子sl, 右孩子sr
                        TreeNode<K,V> sl = xpr.left, sr = xpr.right;

                        // 20201122 如果兄弟结点没有孩子结点 或者 孩子结点都为黑色(不能变色, 没用), 说明兄弟没得借(对应b.2. 兄弟没得借)
                        if ((sr == null || !sr.red) && (sl == null || !sl.red)) {
                            // 2020119 则将兄弟结点设置为红结点 => 兄弟结点所在分支黑结点-1, 开始自损
                            xpr.red = true;

                            // 20201122 设置父节点为新的当前结点, 开始向上自损, 向上递归
                            x = xp;
                        }

                        // 20201122 到这里, x黑, xp红, xpr黑
                        // 20201122 兄弟结点任意一个子结点为红色, 说明兄弟结点有得借(对应方案b.1.2.2. 兄弟结点借出去2个)
                        else {
                            // 20201122 如果兄弟结点只有左孩子 或者右孩子为黑结点时, 说明属于孩子借得到的情况, 对应情况b.1.1.2. => 这时先对兄弟结点进行右旋成b.1.1.1. 的情况
                            if (sr == null || !sr.red) {
                                // 20201122 如果兄弟结点的左孩子可以借
                                if (sl != null)
                                    // 20201122 则设置兄弟结点的左孩子为黑结点 => 目的是为了旋转后出现红红的情况
                                    sl.red = false;

                                // 20201122 设置兄弟结点为红结点 => 目的是保持这次右旋的平衡
                                xpr.red = true;

                                // 20201122 对兄弟结点进行右旋, 旋出最左的结点
                                root = rotateRight(root, xpr);

                                // 20201122 更新兄弟结点
                                xpr = (xp = x.parent) == null ? null : xp.right;
                            }

                            // 20201122 到这步, x黑, xp红, xpr黑, 对应情况b.1.1.1. 和 b.1.2.2. 即兄弟结点为3、4结点的情况
                            // 20201122 如果新的兄弟结点不为null
                            if (xpr != null) {
                                // 20201122 如果父节点也为空(不会发生), 置兄弟结点为黑结点, 否则设置与父节点相同的颜色 => 准备左旋, xpr将会坐上xp的位置
                                xpr.red = (xp == null) ? false : xp.red;

                                // 20201122 如果兄弟结点的右孩子不为空
                                if ((sr = xpr.right) != null)
                                    // 20201122 则设置兄弟结点的右孩子为黑结点 => 防止左旋后出现的红红情况
                                    sr.red = false;
                            }

                            // 20201122 如果当前结点的父节点不为空
                            if (xp != null) {
                                // 20201122 则设置父节点为黑结点 => 防止左旋后出现的红红情况
                                xp.red = false;

                                // 20201122 对父节点进行左旋 => xpr将坐上xp的位置
                                root = rotateLeft(root, xp);
                            }

                            // 20201122 调整完毕, 设置当前结点为根结点, 退出循环
                            x = root;
                        }
                    }
                }

                // 20201122 同理, 如果当前结点x属于父节点的右孩子时, 则进行反向操作
                else { // symmetric
                    // 20201122 xpl在上面已经获取到了, 兄弟结点为红结点, 说明该兄弟结点不是真正的兄弟结点(因为这时为3结点的父结点的红结点)
                    if (xpl != null && xpl.red) {
                        // 20201122 设置假兄弟结点(为3结点的父结点的红结点)为黑色 => 为了保持旋转后父结点3结点的性质
                        xpl.red = false;

                        // 20201122 设置父结点(为3结点的父结点的黑结点)为红色 => 为了保持旋转后父结点3结点的性质
                        xp.red = true;

                        // 20201122 对父结点进行右旋, 把为3结点的父结点的红结点旋上父结点位置, 旋出真正的兄弟结点
                        root = rotateRight(root, xp);

                        // 20201122 更新真正的兄弟结点
                        xpl = (xp = x.parent) == null ? null : xp.left;
                    }

                    // 20201122 如果真正的兄弟结点为空, 说明为情况a.1.3.1. 删除的是3结点的红结点
                    if (xpl == null)
                        x = xp;// 20201122 则设置父亲结点为新的当前结点, 使得replacement==p

                    // 20201122 如果真正的兄弟结点存在(肯定为黑结点)
                    else {
                        // 20201122 获取兄弟结点的左孩子sl, 右孩子sr
                        TreeNode<K,V> sl = xpl.left, sr = xpl.right;

                        // 20201122 如果兄弟结点没有孩子结点 或者孩子结点都为黑色(不能变色, 没用), 说明没得借(对应b.2. 兄弟没得借)
                        if ((sl == null || !sl.red) && (sr == null || !sr.red)) {
                            // 2020119 则将兄弟结点设置为红结点 => 兄弟结点所在分支黑结点-1, 开始自损
                            xpl.red = true;

                            // 20201122 设置父节点为新的当前结点, 开始向上自损, 向上递归
                            x = xp;
                        }

                        // 20201122 到这里, x黑, xp红, xpl黑
                        // 20201122 兄弟结点任意一个子结点为红色, 说明兄弟结点有得借(对应方案b.1.2.2. 兄弟结点借出去2个)
                        else {
                            // 20201122 如果兄弟结点只有右孩子 或者左孩子为黑结点时, 说明属于孩子借得到的情况, 对应情况b.1.1.2. => 这时先对兄弟结点进行左旋成b.1.1.1. 的情况
                            if (sl == null || !sl.red) {
                                if (sr != null)
                                    // 20201122 则设置兄弟结点的右孩子为黑结点 => 目的是为了旋转后出现红红的情况
                                    sr.red = false;

                                // 20201122 设置兄弟结点为红结点 => 目的是保持这次右旋的平衡
                                xpl.red = true;

                                // 20201122 对兄弟结点进行左旋, 旋出最右的结点
                                root = rotateLeft(root, xpl);

                                // 20201122 更新兄弟结点
                                xpl = (xp = x.parent) == null ? null : xp.left;
                            }

                            // 20201122 到这步, x黑, xp红, xpl黑, 对应情况b.1.1.1. 和 b.1.2.2. 即兄弟结点为3、4结点的情况
                            // 20201122 如果新的兄弟结点不为null
                            if (xpl != null) {
                                // 20201122 如果父节点也为空(不会发生), 置兄弟结点为黑结点, 否则设置与父节点相同的颜色 => 准备右旋, xpl将会坐上xp的位置
                                xpl.red = (xp == null) ? false : xp.red;

                                // 20201122 如果兄弟结点的左孩子不为空
                                if ((sl = xpl.left) != null)
                                    // 20201122 则设置兄弟结点的左孩子为黑结点 => 防止右旋后出现的红红情况
                                    sl.red = false;
                            }

                            // 20201122 如果当前结点的父节点不为空
                            if (xp != null) {
                                // 20201122 则设置父节点为黑结点 => 防止右旋后出现的红红情况
                                xp.red = false;

                                // 20201122 对父节点进行右旋 => xpl将坐上xp的位置
                                root = rotateRight(root, xp);
                            }

                            // 20201122 调整完毕, 设置当前结点为根结点, 退出循环
                            x = root;
                        }
                    }
                }
            }
        }

        /**
         * Recursive invariant check
         */
        // 20201119 递归不变检查 => 红黑树结点检查
        /**
         * 红黑树性质:
         *      a. 结点是红色或黑色
         *      b. 根结点必须为黑色
         *      c. 所有叶子都是黑色
         *      d. 每个红结点的两个子结点都是黑色 => 即从每个叶子到根的所有路径上不能有两个连续的红色结点
         *      e. 从任一结点到其每个叶子的所有路径都包含相同数目的黑色结点(黑色平衡)
         */
        static <K,V> boolean checkInvariants(TreeNode<K,V> t) {
            // 20201119 初始化父节点tp, 左孩子结点tl, 右孩子结点tr, 桶的前指针tb, 桶的后指针tn
            TreeNode<K,V> tp = t.parent, tl = t.left, tr = t.right,
                tb = t.prev, tn = (TreeNode<K,V>)t.next;

            // 20201119 如果前结点不为空, 但它的后结点不为t结点, 则返回false
            if (tb != null && tb.next != t)
                return false;

            // 20201119 如果后结点不为空, 但它的前结点不为t结点, 则返回false
            if (tn != null && tn.prev != t)
                return false;

            // 20201119 如果父结点不为空, 但t又不是他的左右孩子, 则返回false
            if (tp != null && t != tp.left && t != tp.right)
                return false;

            // 20201119 如果左孩子结点不为空, 且左孩子的父亲不是t 或者 大于父亲的hash, 则返回false => 左孩子的hash必须小于根结点的hash
            if (tl != null && (tl.parent != t || tl.hash > t.hash))
                return false;

            // 20201119 如果右孩子结点不为空, 且右孩子的父亲不是t 或者 小于父亲的hash, 则返回false => 右孩子的hash必须大于根结点的hash
            if (tr != null && (tr.parent != t || tr.hash < t.hash))
                return false;

            // 20201119 如果t结点是红节点, 且左右孩子都是红结点, 则返回false
            if (t.red && tl != null && tl.red && tr != null && tr.red)// 20201119 节点红, 左孩子红, 右孩子黑, 也算红黑树?
                return false;// 20201119 如果结点为红结点, 那么左右孩子结点肯定为黑结点

            // 20201119 如果左孩子不为叶子结点, 则继续校验左子树
            if (tl != null && !checkInvariants(tl))
                return false;

            // 20201119 如果右孩子不为叶子结点, 则继续校验右子树
            if (tr != null && !checkInvariants(tr))
                return false;

            // 20201119 否则证明的确是一颗红黑树
            return true;
        }
    }

}
