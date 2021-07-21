/*
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util.concurrent;

import java.io.ObjectStreamField;
import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Spliterator;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.DoubleBinaryOperator;
import java.util.function.Function;
import java.util.function.IntBinaryOperator;
import java.util.function.LongBinaryOperator;
import java.util.function.ToDoubleBiFunction;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntBiFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongBiFunction;
import java.util.function.ToLongFunction;
import java.util.stream.Stream;

/**
 * 20210618
 * A. 一个哈希表，支持检索的完全并发和更新的高预期并发。 此类遵循与{@link java.util.Hashtable}相同的功能规范，并包含与{@code Hashtable}的每个方法对应的方法版本。
 *    但是，即使所有操作都是线程安全的，检索操作不需要也不支持锁，以防止所有访问的方式锁定整个表。在依赖其线程安全而不关注其同步细节的程序时，
 *    可完全视此类操作等同于{@code Hashtable}。
 * B. 检索操作（包括{@code get}）一般不会阻塞，可能与更新操作（包括{@code put} 和{@code remove}）同时执行，反映的是最近完成更新操作的结果。
 *   （更正式地说，给定键的更新操作与报告更新值的该键的任何（非null）检索具有先发生关系。）对于诸如 {@code putAll} 和 {@code clear} 之类的聚合操作，
 *    并发检索可能只反映出插入或删除某些条目。类似地，迭代器、拆分器和枚举返回反映哈希表在迭代器/枚举创建时或创建后的某个时刻的状态的元素。
 *    它们不会抛出 {@link java.util.ConcurrentModificationException ConcurrentModificationException}。但是，迭代器被设计为一次仅由一个线程使用。
 *    请记住，包括 {@code size}、{@code isEmpty} 和 {@code containsValue} 在内的聚合状态方法的结果通常仅在Map未在其他线程中进行并发更新时才有用。
 *    否则，这些方法的结果反映的瞬态状态可能足以用于监测或估计目的，但不适用于程序控制。
 * C. 当有太多冲突（即具有不同哈希码但落入同一个槽模表大小的键）时，该表动态扩容，预期平均效果是每个映射保持大约两倍容量、（对应于 0.75 负载）调整大小的因子阈值。
 *    随着映射的添加和删除，这个平均值可能会有很大差异，但总的来说，这保持了哈希表的普遍接受的时间/空间权衡。然而，调整这个或任何其他类型的哈希表的大小可能是一个相对较慢的操作。
 *    如果可能，最好将大小估计作为可选的 {@code initialCapacity} 构造函数参数提供。额外的可选 {@code loadFactor} 构造函数参数通过指定用于计算为给定数量元素分配的空间量的表密度，
 *    提供了一种自定义初始表容量的进一步方法。此外，为了与此类的先前版本兼容，构造函数可以选择指定预期的 {@code concurrencyLevel} 作为内部大小调整的附加提示。
 *    请注意，使用许多具有完全相同 {@code hashCode()} 的键是降低任何哈希表性能的可靠方法。为了改善影响，当键是 {@link Comparable} 时，
 *    此类可以使用键之间的比较顺序来帮助打破联系。
 * D. ConcurrentHashMap的{@link Set}视图可以创建（使用{@link #newKeySet()}或{@link #newKeySet(int)}），或查看（使用{@link #keySet(Object)}键很重要，
 *    并且映射的值（可能暂时）不使用或都采用相同的映射值。
 * E. 通过使用 {@link java.util.concurrent.atomic.LongAdder}值并通过{@link #computeIfAbsent computeIfAbsent} 初始化，
 *    ConcurrentHashMap可以用作可扩展的频率图（直方图或多重集的形式）。例如，要将计数添加 {@code ConcurrentHashMap<String,LongAdder> freqs}，
 *    您可以使用{@code freqs.computeIfAbsent(k -> new LongAdder()).increment();}
 * F. 此类及其视图和迭代器实现了{@link Map}和{@link Iterator} 接口的所有可选方法。
 * G. 与{@link Hashtable} 类似但与 {@link HashMap} 不同的是，此类不允许将 {@code null} 用作键或值。
 * H. ConcurrentHashMaps支持一组顺序和并行的批量操作，与大多数 {@link Stream} 方法不同，这些操作旨在安全且通常明智地应用，即使是其他线程并发更新的映射；
 *    例如，在计算共享注册表中值的快照摘要时。 共有三种操作，每种有四种形式，接受带有键、值、条目和（键，值）参数和/或返回值的函数。
 *    因为ConcurrentHashMap的元素没有以任何特定的方式排序，并且可能在不同的并行执行中以不同的顺序进行处理，所提供函数的正确性不应该依赖于任何排序，
 *    也不依赖于任何其他对象或值，这些对象或值可能会暂时改变计算正在进行中； 除了forEach操作，理想情况下应该是无副作用的。
 *    {@link java.util.Map.Entry}对象上的批量操作不支持方法{@code setValue}:
 *    a. forEach：对每个元素执行给定的操作。 变体形式在执行操作之前对每个元素应用给定的转换。
 *    b. search：返回在每个元素上应用给定函数的第一个可用非null结果； 找到结果时跳过进一步搜索。
 *    c. reduce：累加每个元素。提供的归约函数不能依赖于排序（更正式地说，它应该是结合的和可交换的）。有五种变体：
 *          1) 普通减价, （因为没有相应的返回类型，所以 (key, value) 函数参数没有这种方法的形式。）
 *          2) 累积应用于每个元素的给定函数的结果的映射归约。
 *          3) 使用给定的基值减少到双精度、长整数和整数的标量。
 * I. 这些批量操作接受一个{@code parallelismThreshold} 参数。如果估计当前Map大小小于给定阈值，则方法按顺序进行。使用{@code Long.MAX_VALUE}值会抑制所有并行性。
 *    使用 {@code 1} 值通过划分为足够多的子任务以充分利用用于所有并行计算的{@link ForkJoinPool#commonPool()}来实现最大并行度。通常，您最初会选择这些极值之一，
 *    然后测量使用在开销与吞吐量之间进行权衡的中间值的性能。
 * J. 批量操作的并发属性遵循ConcurrentHashMap的并发属性：从{@code get(key)}和相关访问方法返回的任何非null结果, 都与相关的插入或更新具有先发生关系。
 *    任何批量操作的结果都反映了这些每个元素关系的组成（但对于整个映射而言，不一定是原子的，除非以某种方式知道它是静止的）。相反，由于映射中的键和值永远不会为null，
 *    因此null作为当前缺少任何结果的可靠原子指示符。为了保持这个属性，null 作为所有非标量归约操作的隐式基础。对于double、long和int版本，基础应该是与任何其他值组合时
 *    返回该其他值的基础（更正式地说，它应该是归约的标识元素）。最常见的还原具有这些特性；例如，计算以0为基础的总和或以MAX_VALUE为基础的最小值。
 * K. 作为参数提供的搜索和转换函数应该类似地返回null以指示缺少任何结果（在这种情况下不使用它）。在映射归约的情况下，这也使转换能够充当过滤器，如果元素不应该被组合，
 *    则返回 null（或者，在原始特化的情况下，身份基础）。在将它们用于搜索或归约操作之前，您可以通过在此“null"(意味着现在没有任何东西)规则下自己组合它们来创建复合转换和过滤。
 * L. 接受和/或返回Entry参数的方法维护键值关联。例如，当找到最大值的键时，它们可能很有用。请注意，可以使用{@code new AbstractMap.SimpleEntry(k,v)}提供“普通”条目参数。
 * M. 批量操作可能会突然完成，从而引发在所提供函数的应用程序中遇到的异常。 请记住，在处理此类异常时，其他并发执行的函数也可能抛出异常，或者如果第一个异常没有发生就会抛出异常。
 * N. 与顺序形式相比，并行的加速是常见的，但不能保证。如果并行化计算的基础工作比计算本身更昂贵，则涉及小映射上的简短函数的并行操作可能比顺序形式执行得更慢。
 *    类似地，如果所有处理器都忙于执行不相关的任务，则并行化可能不会带来太多实际的并行性。
 * O. 所有任务方法的所有参数都必须为非null。
 * P. {@docRoot}/../technotes/guides/collections/index.html
 */

/**
 * A.
 * A hash table supporting full concurrency of retrievals and
 * high expected concurrency for updates. This class obeys the
 * same functional specification as {@link java.util.Hashtable}, and
 * includes versions of methods corresponding to each method of
 * {@code Hashtable}. However, even though all operations are
 * thread-safe, retrieval operations do <em>not</em> entail locking,
 * and there is <em>not</em> any support for locking the entire table
 * in a way that prevents all access.  This class is fully
 * interoperable with {@code Hashtable} in programs that rely on its
 * thread safety but not on its synchronization details.
 *
 * B.
 * <p>Retrieval operations (including {@code get}) generally do not
 * block, so may overlap with update operations (including {@code put}
 * and {@code remove}). Retrievals reflect the results of the most
 * recently <em>completed</em> update operations holding upon their
 * onset. (More formally, an update operation for a given key bears a
 * <em>happens-before</em> relation with any (non-null) retrieval for
 * that key reporting the updated value.)  For aggregate operations
 * such as {@code putAll} and {@code clear}, concurrent retrievals may
 * reflect insertion or removal of only some entries.  Similarly,
 * Iterators, Spliterators and Enumerations return elements reflecting the
 * state of the hash table at some point at or since the creation of the
 * iterator/enumeration.  They do <em>not</em> throw {@link
 * java.util.ConcurrentModificationException ConcurrentModificationException}.
 * However, iterators are designed to be used by only one thread at a time.
 * Bear in mind that the results of aggregate status methods including
 * {@code size}, {@code isEmpty}, and {@code containsValue} are typically
 * useful only when a map is not undergoing concurrent updates in other threads.
 * Otherwise the results of these methods reflect transient states
 * that may be adequate for monitoring or estimation purposes, but not
 * for program control.
 *
 * C.
 * <p>The table is dynamically expanded when there are too many
 * collisions (i.e., keys that have distinct hash codes but fall into
 * the same slot modulo the table size), with the expected average
 * effect of maintaining roughly two bins per mapping (corresponding
 * to a 0.75 load factor threshold for resizing). There may be much
 * variance around this average as mappings are added and removed, but
 * overall, this maintains a commonly accepted time/space tradeoff for
 * hash tables.  However, resizing this or any other kind of hash
 * table may be a relatively slow operation. When possible, it is a
 * good idea to provide a size estimate as an optional {@code
 * initialCapacity} constructor argument. An additional optional
 * {@code loadFactor} constructor argument provides a further means of
 * customizing initial table capacity by specifying the table density
 * to be used in calculating the amount of space to allocate for the
 * given number of elements.  Also, for compatibility with previous
 * versions of this class, constructors may optionally specify an
 * expected {@code concurrencyLevel} as an additional hint for
 * internal sizing.  Note that using many keys with exactly the same
 * {@code hashCode()} is a sure way to slow down performance of any
 * hash table. To ameliorate impact, when keys are {@link Comparable},
 * this class may use comparison order among keys to help break ties.
 *
 * D.
 * <p>A {@link Set} projection of a ConcurrentHashMap may be created
 * (using {@link #newKeySet()} or {@link #newKeySet(int)}), or viewed
 * (using {@link #keySet(Object)} when only keys are of interest, and the
 * mapped values are (perhaps transiently) not used or all take the
 * same mapping value.
 *
 * E.
 * <p>A ConcurrentHashMap can be used as scalable frequency map (a
 * form of histogram or multiset) by using {@link
 * java.util.concurrent.atomic.LongAdder} values and initializing via
 * {@link #computeIfAbsent computeIfAbsent}. For example, to add a count
 * to a {@code ConcurrentHashMap<String,LongAdder> freqs}, you can use
 * {@code freqs.computeIfAbsent(k -> new LongAdder()).increment();}
 *
 * F.
 * <p>This class and its views and iterators implement all of the
 * <em>optional</em> methods of the {@link Map} and {@link Iterator}
 * interfaces.
 *
 * G.
 * <p>Like {@link Hashtable} but unlike {@link HashMap}, this class
 * does <em>not</em> allow {@code null} to be used as a key or value.
 *
 * H.
 * <p>ConcurrentHashMaps support a set of sequential and parallel bulk
 * operations that, unlike most {@link Stream} methods, are designed
 * to be safely, and often sensibly, applied even with maps that are
 * being concurrently updated by other threads; for example, when
 * computing a snapshot summary of the values in a shared registry.
 * There are three kinds of operation, each with four forms, accepting
 * functions with Keys, Values, Entries, and (Key, Value) arguments
 * and/or return values. Because the elements of a ConcurrentHashMap
 * are not ordered in any particular way, and may be processed in
 * different orders in different parallel executions, the correctness
 * of supplied functions should not depend on any ordering, or on any
 * other objects or values that may transiently change while
 * computation is in progress; and except for forEach actions, should
 * ideally be side-effect-free. Bulk operations on {@link java.util.Map.Entry}
 * objects do not support method {@code setValue}.
 *
 * <ul>
 * <li> forEach: Perform a given action on each element.
 * A variant form applies a given transformation on each element
 * before performing the action.</li>
 *
 * <li> search: Return the first available non-null result of
 * applying a given function on each element; skipping further
 * search when a result is found.</li>
 *
 * <li> reduce: Accumulate each element.  The supplied reduction
 * function cannot rely on ordering (more formally, it should be
 * both associative and commutative).  There are five variants:
 *
 * <ul>
 *
 * <li> Plain reductions. (There is not a form of this method for
 * (key, value) function arguments since there is no corresponding
 * return type.)</li>
 *
 * <li> Mapped reductions that accumulate the results of a given
 * function applied to each element.</li>
 *
 * <li> Reductions to scalar doubles, longs, and ints, using a
 * given basis value.</li>
 *
 * </ul>
 * </li>
 * </ul>
 *
 * I.
 * <p>These bulk operations accept a {@code parallelismThreshold}
 * argument. Methods proceed sequentially if the current map size is
 * estimated to be less than the given threshold. Using a value of
 * {@code Long.MAX_VALUE} suppresses all parallelism.  Using a value
 * of {@code 1} results in maximal parallelism by partitioning into
 * enough subtasks to fully utilize the {@link
 * ForkJoinPool#commonPool()} that is used for all parallel
 * computations. Normally, you would initially choose one of these
 * extreme values, and then measure performance of using in-between
 * values that trade off overhead versus throughput.
 *
 * J.
 * <p>The concurrency properties of bulk operations follow
 * from those of ConcurrentHashMap: Any non-null result returned
 * from {@code get(key)} and related access methods bears a
 * happens-before relation with the associated insertion or
 * update.  The result of any bulk operation reflects the
 * composition of these per-element relations (but is not
 * necessarily atomic with respect to the map as a whole unless it
 * is somehow known to be quiescent).  Conversely, because keys
 * and values in the map are never null, null serves as a reliable
 * atomic indicator of the current lack of any result.  To
 * maintain this property, null serves as an implicit basis for
 * all non-scalar reduction operations. For the double, long, and
 * int versions, the basis should be one that, when combined with
 * any other value, returns that other value (more formally, it
 * should be the identity element for the reduction). Most common
 * reductions have these properties; for example, computing a sum
 * with basis 0 or a minimum with basis MAX_VALUE.
 *
 * K.
 * <p>Search and transformation functions provided as arguments
 * should similarly return null to indicate the lack of any result
 * (in which case it is not used). In the case of mapped
 * reductions, this also enables transformations to serve as
 * filters, returning null (or, in the case of primitive
 * specializations, the identity basis) if the element should not
 * be combined. You can create compound transformations and
 * filterings by composing them yourself under this "null means
 * there is nothing there now" rule before using them in search or
 * reduce operations.
 *
 * L.
 * <p>Methods accepting and/or returning Entry arguments maintain
 * key-value associations. They may be useful for example when
 * finding the key for the greatest value. Note that "plain" Entry
 * arguments can be supplied using {@code new
 * AbstractMap.SimpleEntry(k,v)}.
 *
 * M.
 * <p>Bulk operations may complete abruptly, throwing an
 * exception encountered in the application of a supplied
 * function. Bear in mind when handling such exceptions that other
 * concurrently executing functions could also have thrown
 * exceptions, or would have done so if the first exception had
 * not occurred.
 *
 * N.
 * <p>Speedups for parallel compared to sequential forms are common
 * but not guaranteed.  Parallel operations involving brief functions
 * on small maps may execute more slowly than sequential forms if the
 * underlying work to parallelize the computation is more expensive
 * than the computation itself.  Similarly, parallelization may not
 * lead to much actual parallelism if all processors are busy
 * performing unrelated tasks.
 *
 * O.
 * <p>All arguments to all task methods must be non-null.
 *
 * P.
 * <p>This class is a member of the
 * <a href="{@docRoot}/../technotes/guides/collections/index.html">
 * Java Collections Framework</a>.
 *
 * @since 1.5
 * @author Doug Lea
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 */
public class ConcurrentHashMap<K,V> extends AbstractMap<K,V> implements ConcurrentMap<K,V>, Serializable {

    private static final long serialVersionUID = 7249069246763182397L;

    /**
     * 20210618
     * 概述:
     * A. 这个哈希表的主要设计目标是保持并发可读性（通常是方法get()，还有迭代器和相关方法），同时最小化更新争用。
     *    次要目标是保持与java.util.HashMap相同或更好的空间消耗，并支持许多线程对空表的高初始插入率。
     * B. 该映射通常用作分箱（分桶）哈希表。每个键值映射都保存在一个节点中。大多数节点是具有散列、键、值和下一个字段的基本Node类的实例。
     *    但是，存在各种子类：TreeNode排列在平衡树中，而不是列表中。TreeBins持有TreeNode集合的根。ForwardingNodes在调整大小期间放置在bin的头部。
     *    ReservationNodes用作占位符，同时在computeIfAbsent和相关方法中建立值。类型TreeBin、ForwardingNode和ReservationNode不保存普通的用户键、值或散列，
     *    并且在搜索等过程中很容易区分，因为它们具有负散列字段和null键和值字段。 （这些特殊节点要么不常见，要么是暂时的，所以携带一些未使用的字段的影响是微不足道的。）
     * C. 该表在第一次插入时被延迟初始化为2的幂大小。表中的每个bin通常包含一个节点列表（大多数情况下，该列表只有零个或一个节点）。表访问需要易失性/原子读取、写入
     *    和CAS。因为没有其他方法可以在不添加更多间接方法的情况下进行安排，所以我们使用内部函数 (sun.misc.Unsafe) 操作。
     * D. 我们使用节点哈希字段的顶部（符号）位进行控制——由于寻址限制，它无论如何都可用。具有负散列字段的节点在map方法中被特殊处理或忽略。
     * E. 将第一个节点插入（通过put或其变体）到空bin中，只需将其CASing到bin中即可。这是迄今为止大多数键/哈希分布下放置操作的最常见情况。
     *    其他更新操作（插入、删除和替换）需要锁定。我们不想浪费将不同的锁对象与每个 bin 相关联所需的空间，而是使用 bin 链表本身的第一个节点作为锁。
     *    对这些锁的锁定支持依赖于内置的“同步”监视器。
     * F. 但是，使用链表的第一个节点作为锁本身是不够的：当一个节点被锁定时，任何更新都必须首先验证它在锁定后仍然是第一个节点，如果不是，则重试。
     *    因为新节点总是附加到链表中，一旦一个节点首先出现在一个bin中，它就会保持在第一个位置，直到被删除或 bin 失效（调整大小时）。
     * G. per-bin锁的主要缺点是受同一锁保护的bin列表中其他节点上的其他更新操作可能会停止，例如当用户equals()或映射函数需要很长时间时。然而，统计上，在随机哈希码下，
     *    这不是一个普遍的问题。理想情况下，考虑到0.75的大小调整阈值，bin中节点的频率遵循泊松分布，参数平均约为0.5，尽管由于调整粒度。
     *    忽略方差，列表大小k的预期出现次数为 (exp(-0.5) * pow(0.5, k) / factorial(k))。 第一个值是：
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
     * H. 在随机散列下，两个线程访问不同元素的锁争用概率大约为 1 / (8 * #elements)。
     * I. 实践中遇到的实际散列码分布有时会明显偏离均匀随机性。这包括 N > (1<<30) 的情况，因此某些键必须发生冲突。类似的愚蠢或敌对用途，
     *    其中多个密钥被设计为具有相同的哈希码或仅在屏蔽高位上不同的哈希码。因此，当bin中的节点数量超过阈值时，我们使用了一种辅助策略。
     *    这些TreeBins使用平衡树来保存节点（红黑树的一种特殊形式），将搜索时间限制为O(log N)。 TreeBin中的每个搜索步骤至少比常规列表慢两倍，
     *    但鉴于N不能超过(1<<64)（在地址用完之前），这将搜索步骤、锁定保持时间等限制为合理常量（每个操作最坏的情况下检查大约 100 个节点），
     *    只要键是可比较的（这很常见——字符串、长整数等）。TreeBin 节点（TreeNodes）也和普通节点一样维护着同样的“next”遍历指针，
     *    因此可以在迭代器中以同样的方式进行遍历。
     * J. 当占用率超过百分比阈值（名义上为 0.75，但见下文）时，将调整表格大小。在启动线程分配和设置替换数组之后，任何注意到垃圾箱过满的线程都可以帮助调整大小。
     *    然而，这些其他线程可能会继续进行插入等操作，而不是停顿。TreeBins的使用使我们免受在调整大小进行时过度填充的最坏情况影响。
     *    通过将bins一个个地转移到下一个bin来调整大小。但是，线程在这样做之前要求传输小块索引（通过字段 transferIndex），从而减少争用。
     *    字段sizeCtl中的生成戳可确保调整大小不会重叠。因为我们使用的是二次幂扩展，所以每个 bin 中的元素必须保持相同的索引，或者以二次幂的偏移量移动。
     *    我们通过捕获可以重用旧节点的情况来消除不必要的节点创建，因为它们的下一个字段不会改变。平均而言，当一个bin移动时，其中只有大约六分之一需要克隆。
     *    一旦它们不再被可能处于并发遍历表中的任何读取器线程引用，它们替换的节点将是可垃圾回收的。传输时，旧表 bin 仅包含一个特殊的转发节点（具有哈希字段“MOVED”），
     *    该节点包含下一个表作为其键。遇到转发节点时，访问和更新操作将使用新表重新启动。
     * K. 每个bin传输都需要其bin锁，这可能会在调整大小时停止等待锁。但是，因为其他线程可以加入并帮助调整大小而不是争用锁，所以随着调整大小的进行，
     *    平均聚合等待时间变得更短。传输操作还必须确保旧表和新表中所有可访问的bin均可用于任何遍历。这部分是通过从最后一个bin(table.length - 1)到第一个bin来安排的。
     *    在看到转发节点时，遍历（参见 Traverser 类）安排移动到新表，而无需重新访问节点。为了确保即使在无序移动时也不会跳过中间节点，
     *    在遍历期间第一次遇到转发节点时会创建一个堆栈（参见类 TableStack），以在以后处理当前表时保持其位置。这些保存/恢复机制的需求相对较少，
     *    但是当遇到一个转发节点时，通常会有更多。所以Traversers使用简单的缓存方案来避免创建这么多新的TableStack节点。 （感谢 Peter Levart 在这里建议使用堆栈。）
     * L. 遍历方案也适用于bin范围的部分遍历（通过备用 Traverser 构造函数）以支持分区聚合操作。此外，如果转发到空表，只读操作就会放弃，
     *    这提供了对关闭式清除的支持，目前也没有实现。
     * M. 延迟表初始化在第一次使用之前最大限度地减少了占用空间，并且还避免了在第一次操作来自putAll、带有映射参数的构造函数或反序列化时调整大小。
     *    这些情况试图覆盖初始容量设置，但在竞争情况下无害地无法生效。
     * N. 元素计数是使用LongAdder的特殊化来维护的。为了访问导致创建多个CounterCell的隐式争用感知，我们需要合并一个专门化，而不仅仅是使用 LongAdder。
     *    计数器机制避免了更新争用，但如果在并发访问期间读取过于频繁，可能会遇到缓存抖动。 为了避免频繁读取，只有在添加到已经包含两个或更多节点的bin时，
     *    才尝试在争用情况下调整大小。在均匀散列分布下，这种情况发生在阈值的概率约为13%，这意味着只有大约八分之一的put检查阈值（并且在调整大小后，
     *    这样做的可能性要小得多）。
     * O. TreeBins使用一种特殊形式的比较来进行搜索和相关操作（这是我们不能使用现有集合（例如 TreeMap）的主要原因）。TreeBins 包含 Comparable 元素，
     *    但可能包含其他元素，以及对于同一个T具有Comparable但不一定Comparable的元素，因此我们不能在它们之间调用compareTo。为了处理这个问题，树主要按哈希值排序，
     *    然后按Comparable.compareTo排序（如果适用）。在查找节点时，如果元素不可比较或比较为0，则在绑定散列值的情况下，可能需要搜索左右孩子。
     *    （这对应于如果所有元素都是不可比较的并且绑定了散列，则需要进行完整的列表搜索。）在插入时，为了在重新平衡之间保持总排序（或尽可能接近），我们比较类和
     *    identityHashCodes作为决胜局。红黑平衡代码由 pre-jdk-collections (http://gee.cs.oswego.edu/dl/classes/collections/RBCell.java) 更新而来，
     *    依次基于 Cormen、Leiserson 和 Rivest “Introduction to算法”（CLR）。
     * P. TreeBins还需要一个额外的锁定机制。尽管即使在更新期间读者也始终可以进行列表遍历，但树遍历却不是，这主要是因为树旋转可能会改变根节点和/或其链接。
     *    TreeBins包含一个简单的读写锁机制，寄生在主bin同步策略上：与插入或移除相关的结构调整已经被bin锁定（因此不会与其他写入器冲突），
     *    但必须等待正在进行的读取器完成。由于只能有一个这样的服务员，我们使用一个简单的方案，使用单个“服务员”字段来阻止写入者。但是，读者永远不需要阻止。
     *    如果根锁被持有，它们会沿着缓慢的遍历路径（通过下一个指针）继续，直到锁可用或列表耗尽，以先到者为准。 这些情况并不快，但可以最大限度地提高预期总吞吐量。
     * Q. 保持与此类以前版本的API和序列化兼容性会引入一些奇怪的问题。主要是：我们保留未触及但未使用的构造函数参数，引用concurrencyLevel。
     *    我们接受一个loadFactor构造函数参数，但仅将其应用于初始表容量（这是我们可以保证遵守它的唯一时间。）我们还声明了一个未使用的“Segment”类，
     *    该类仅在序列化时以最小形式实例化。
     * R. 另外，仅仅为了兼容这个类的以前版本，它扩展了 AbstractMap，即使它的所有方法都被覆盖了，所以它只是无用的包袱。
     * S. 该文件的组织方式使阅读时比其他方式更容易理解：首先是主要的静态声明和实用程序，然后是字段，然后是主要的公共方法（将多个公共方法的一些因素分解为内部方法），
     *    然后调整大小 方法、树、遍历器和批量操作。
     */
    /*
     * Overview:
     *
     * A.
     * The primary design goal of this hash table is to maintain
     * concurrent readability (typically method get(), but also
     * iterators and related methods) while minimizing update
     * contention. Secondary goals are to keep space consumption about
     * the same or better than java.util.HashMap, and to support high
     * initial insertion rates on an empty table by many threads.
     *
     * B.
     * This map usually acts as a binned (bucketed) hash table.  Each
     * key-value mapping is held in a Node.  Most nodes are instances
     * of the basic Node class with hash, key, value, and next
     * fields. However, various subclasses exist: TreeNodes are
     * arranged in balanced trees, not lists.  TreeBins hold the roots
     * of sets of TreeNodes. ForwardingNodes are placed at the heads
     * of bins during resizing. ReservationNodes are used as
     * placeholders while establishing values in computeIfAbsent and
     * related methods.  The types TreeBin, ForwardingNode, and
     * ReservationNode do not hold normal user keys, values, or
     * hashes, and are readily distinguishable during search etc
     * because they have negative hash fields and null key and value
     * fields. (These special nodes are either uncommon or transient,
     * so the impact of carrying around some unused fields is
     * insignificant.)
     *
     * C.
     * The table is lazily initialized to a power-of-two size upon the
     * first insertion.  Each bin in the table normally contains a
     * list of Nodes (most often, the list has only zero or one Node).
     * Table accesses require volatile/atomic reads, writes, and
     * CASes.  Because there is no other way to arrange this without
     * adding further indirections, we use intrinsics
     * (sun.misc.Unsafe) operations.
     *
     * D.
     * We use the top (sign) bit of Node hash fields for control
     * purposes -- it is available anyway because of addressing
     * constraints.  Nodes with negative hash fields are specially
     * handled or ignored in map methods.
     *
     * E.
     * Insertion (via put or its variants) of the first node in an
     * empty bin is performed by just CASing it to the bin.  This is
     * by far the most common case for put operations under most
     * key/hash distributions.  Other update operations (insert,
     * delete, and replace) require locks.  We do not want to waste
     * the space required to associate a distinct lock object with
     * each bin, so instead use the first node of a bin list itself as
     * a lock. Locking support for these locks relies on builtin
     * "synchronized" monitors.
     *
     * F.
     * Using the first node of a list as a lock does not by itself
     * suffice though: When a node is locked, any update must first
     * validate that it is still the first node after locking it, and
     * retry if not. Because new nodes are always appended to lists,
     * once a node is first in a bin, it remains first until deleted
     * or the bin becomes invalidated (upon resizing).
     *
     * G.
     * The main disadvantage of per-bin locks is that other update
     * operations on other nodes in a bin list protected by the same
     * lock can stall, for example when user equals() or mapping
     * functions take a long time.  However, statistically, under
     * random hash codes, this is not a common problem.  Ideally, the
     * frequency of nodes in bins follows a Poisson distribution
     * (http://en.wikipedia.org/wiki/Poisson_distribution) with a
     * parameter of about 0.5 on average, given the resizing threshold
     * of 0.75, although with a large variance because of resizing
     * granularity. Ignoring variance, the expected occurrences of
     * list size k are (exp(-0.5) * pow(0.5, k) / factorial(k)). The
     * first values are:
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
     * H.
     * Lock contention probability for two threads accessing distinct
     * elements is roughly 1 / (8 * #elements) under random hashes.
     *
     * I.
     * Actual hash code distributions encountered in practice
     * sometimes deviate significantly from uniform randomness.  This
     * includes the case when N > (1<<30), so some keys MUST collide.
     * Similarly for dumb or hostile usages in which multiple keys are
     * designed to have identical hash codes or ones that differs only
     * in masked-out high bits. So we use a secondary strategy that
     * applies when the number of nodes in a bin exceeds a
     * threshold. These TreeBins use a balanced tree to hold nodes (a
     * specialized form of red-black trees), bounding search time to
     * O(log N).  Each search step in a TreeBin is at least twice as
     * slow as in a regular list, but given that N cannot exceed
     * (1<<64) (before running out of addresses) this bounds search
     * steps, lock hold times, etc, to reasonable constants (roughly
     * 100 nodes inspected per operation worst case) so long as keys
     * are Comparable (which is very common -- String, Long, etc).
     * TreeBin nodes (TreeNodes) also maintain the same "next"
     * traversal pointers as regular nodes, so can be traversed in
     * iterators in the same way.
     *
     * J.
     * The table is resized when occupancy exceeds a percentage
     * threshold (nominally, 0.75, but see below).  Any thread
     * noticing an overfull bin may assist in resizing after the
     * initiating thread allocates and sets up the replacement array.
     * However, rather than stalling, these other threads may proceed
     * with insertions etc.  The use of TreeBins shields us from the
     * worst case effects of overfilling while resizes are in
     * progress.  Resizing proceeds by transferring bins, one by one,
     * from the table to the next table. However, threads claim small
     * blocks of indices to transfer (via field transferIndex) before
     * doing so, reducing contention.  A generation stamp in field
     * sizeCtl ensures that resizings do not overlap. Because we are
     * using power-of-two expansion, the elements from each bin must
     * either stay at same index, or move with a power of two
     * offset. We eliminate unnecessary node creation by catching
     * cases where old nodes can be reused because their next fields
     * won't change.  On average, only about one-sixth of them need
     * cloning when a table doubles. The nodes they replace will be
     * garbage collectable as soon as they are no longer referenced by
     * any reader thread that may be in the midst of concurrently
     * traversing table.  Upon transfer, the old table bin contains
     * only a special forwarding node (with hash field "MOVED") that
     * contains the next table as its key. On encountering a
     * forwarding node, access and update operations restart, using
     * the new table.
     *
     * K.
     * Each bin transfer requires its bin lock, which can stall
     * waiting for locks while resizing. However, because other
     * threads can join in and help resize rather than contend for
     * locks, average aggregate waits become shorter as resizing
     * progresses.  The transfer operation must also ensure that all
     * accessible bins in both the old and new table are usable by any
     * traversal.  This is arranged in part by proceeding from the
     * last bin (table.length - 1) up towards the first.  Upon seeing
     * a forwarding node, traversals (see class Traverser) arrange to
     * move to the new table without revisiting nodes.  To ensure that
     * no intervening nodes are skipped even when moved out of order,
     * a stack (see class TableStack) is created on first encounter of
     * a forwarding node during a traversal, to maintain its place if
     * later processing the current table. The need for these
     * save/restore mechanics is relatively rare, but when one
     * forwarding node is encountered, typically many more will be.
     * So Traversers use a simple caching scheme to avoid creating so
     * many new TableStack nodes. (Thanks to Peter Levart for
     * suggesting use of a stack here.)
     *
     * L.
     * The traversal scheme also applies to partial traversals of
     * ranges of bins (via an alternate Traverser constructor)
     * to support partitioned aggregate operations.  Also, read-only
     * operations give up if ever forwarded to a null table, which
     * provides support for shutdown-style clearing, which is also not
     * currently implemented.
     *
     * M.
     * Lazy table initialization minimizes footprint until first use,
     * and also avoids resizings when the first operation is from a
     * putAll, constructor with map argument, or deserialization.
     * These cases attempt to override the initial capacity settings,
     * but harmlessly fail to take effect in cases of races.
     *
     * N.
     * The element count is maintained using a specialization of
     * LongAdder. We need to incorporate a specialization rather than
     * just use a LongAdder in order to access implicit
     * contention-sensing that leads to creation of multiple
     * CounterCells.  The counter mechanics avoid contention on
     * updates but can encounter cache thrashing if read too
     * frequently during concurrent access. To avoid reading so often,
     * resizing under contention is attempted only upon adding to a
     * bin already holding two or more nodes. Under uniform hash
     * distributions, the probability of this occurring at threshold
     * is around 13%, meaning that only about 1 in 8 puts check
     * threshold (and after resizing, many fewer do so).
     *
     * O.
     * TreeBins use a special form of comparison for search and
     * related operations (which is the main reason we cannot use
     * existing collections such as TreeMaps). TreeBins contain
     * Comparable elements, but may contain others, as well as
     * elements that are Comparable but not necessarily Comparable for
     * the same T, so we cannot invoke compareTo among them. To handle
     * this, the tree is ordered primarily by hash value, then by
     * Comparable.compareTo order if applicable.  On lookup at a node,
     * if elements are not comparable or compare as 0 then both left
     * and right children may need to be searched in the case of tied
     * hash values. (This corresponds to the full list search that
     * would be necessary if all elements were non-Comparable and had
     * tied hashes.) On insertion, to keep a total ordering (or as
     * close as is required here) across rebalancings, we compare
     * classes and identityHashCodes as tie-breakers. The red-black
     * balancing code is updated from pre-jdk-collections
     * (http://gee.cs.oswego.edu/dl/classes/collections/RBCell.java)
     * based in turn on Cormen, Leiserson, and Rivest "Introduction to
     * Algorithms" (CLR).
     *
     * P.
     * TreeBins also require an additional locking mechanism.  While
     * list traversal is always possible by readers even during
     * updates, tree traversal is not, mainly because of tree-rotations
     * that may change the root node and/or its linkages.  TreeBins
     * include a simple read-write lock mechanism parasitic on the
     * main bin-synchronization strategy: Structural adjustments
     * associated with an insertion or removal are already bin-locked
     * (and so cannot conflict with other writers) but must wait for
     * ongoing readers to finish. Since there can be only one such
     * waiter, we use a simple scheme using a single "waiter" field to
     * block writers.  However, readers need never block.  If the root
     * lock is held, they proceed along the slow traversal path (via
     * next-pointers) until the lock becomes available or the list is
     * exhausted, whichever comes first. These cases are not fast, but
     * maximize aggregate expected throughput.
     *
     * Q.
     * Maintaining API and serialization compatibility with previous
     * versions of this class introduces several oddities. Mainly: We
     * leave untouched but unused constructor arguments refering to
     * concurrencyLevel. We accept a loadFactor constructor argument,
     * but apply it only to initial table capacity (which is the only
     * time that we can guarantee to honor it.) We also declare an
     * unused "Segment" class that is instantiated in minimal form
     * only when serializing.
     *
     * R.
     * Also, solely for compatibility with previous versions of this
     * class, it extends AbstractMap, even though all of its methods
     * are overridden, so it is just useless baggage.
     *
     * S.
     * This file is organized to make things a little easier to follow
     * while reading than they might otherwise: First the main static
     * declarations and utilities, then fields, then main public
     * methods (with a few factorings of multiple public methods into
     * internal ones), then sizing methods, trees, traversers, and
     * bulk operations.
     */

    /* ---------------- Constants -------------- */

    /**
     * 20210619
     * 最大可能的表容量。该值必须恰好为1<<30以保持在Java数组分配和两个表大小的幂的索引范围内，并且进一步需要，因为32位哈希字段的前两位用于控制目的。
     */
    /**
     * The largest possible table capacity.  This value must be
     * exactly 1<<30 to stay within Java array allocation and indexing
     * bounds for power of two table sizes, and is further required
     * because the top two bits of 32bit hash fields are used for
     * control purposes.
     */
    private static final int MAXIMUM_CAPACITY = 1 << 30;

    /**
     * The default initial table capacity.  Must be a power of 2
     * (i.e., at least 1) and at most MAXIMUM_CAPACITY.
     */
    // 默认的初始表容量。 必须是 2 的幂（即至少为 1）且最多为 MAXIMUM_CAPACITY。
    private static final int DEFAULT_CAPACITY = 16;

    /**
     * The largest possible (non-power of two) array size.
     * Needed by toArray and related methods.
     */
    // 可能的最大（非 2 的幂）数组大小。 toArray 和相关方法需要。
    static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    /**
     * The default concurrency level for this table. Unused but
     * defined for compatibility with previous versions of this class.
     */
    // 此表的默认并发级别。 未使用但定义为与此类的先前版本兼容。
    private static final int DEFAULT_CONCURRENCY_LEVEL = 16;

    /**
     * 20210619
     * 此表的负载因子。在构造函数中覆盖此值仅影响初始表容量。通常不使用实际的浮点值——使用 {@code n - (n >>> 2)} 之类的表达式来表示相关的调整大小阈值会更简单。
     */
    /**
     * The load factor for this table. Overrides of this value in
     * constructors affect only the initial table capacity.  The
     * actual floating point value isn't normally used -- it is
     * simpler to use expressions such as {@code n - (n >>> 2)} for
     * the associated resizing threshold.
     */
    private static final float LOAD_FACTOR = 0.75f;

    /**
     * 20210619
     * 使用树而不是列表的bin计数阈值。将元素添加到至少具有这么多节点的bin时，bin会转换为树。该值必须大于 2，并且应至少为 8，
     * 以与树移除中关于在收缩时转换回普通bin的假设相匹配。
     */
    /**
     * The bin count threshold for using a tree rather than list for a
     * bin.  Bins are converted to trees when adding an element to a
     * bin with at least this many nodes. The value must be greater
     * than 2, and should be at least 8 to mesh with assumptions in
     * tree removal about conversion back to plain bins upon
     * shrinkage.
     */
    static final int TREEIFY_THRESHOLD = 8;

    /**
     * The bin count threshold for untreeifying a (split) bin during a
     * resize operation. Should be less than TREEIFY_THRESHOLD, and at
     * most 6 to mesh with shrinkage detection under removal.
     */
    // 在调整大小操作期间取消（拆分）bin 的 bin 计数阈值。 应小于 TREEIFY_THRESHOLD，最多为 6 以在移除下进行收缩检测。
    static final int UNTREEIFY_THRESHOLD = 6;

    /**
     * 20210619
     * 可以将 bin 树化的最小散列表容量。（否则，如果 bin 中的节点过多，则调整表的大小。）该值应至少为 4 * TREEIFY_THRESHOLD，以避免调整大小和树化阈值之间发生冲突。
     */
    /**
     * The smallest table capacity for which bins may be treeified.
     * (Otherwise the table is resized if too many nodes in a bin.)
     * The value should be at least 4 * TREEIFY_THRESHOLD to avoid
     * conflicts between resizing and treeification thresholds.
     */
    static final int MIN_TREEIFY_CAPACITY = 64;

    /**
     * 20210619
     * 每个转移步骤的最小重新组合次数。 范围被细分以允许多个调整器线程。 此值用作下限，以避免调整器遇到过多的内存争用。 该值应至少为 DEFAULT_CAPACITY。
     */
    /**
     * Minimum number of rebinnings per transfer step. Ranges are
     * subdivided to allow multiple resizer threads.  This value
     * serves as a lower bound to avoid resizers encountering
     * excessive memory contention.  The value should be at least
     * DEFAULT_CAPACITY.
     */
    private static final int MIN_TRANSFER_STRIDE = 16;

    /**
     * The number of bits used for generation stamp in sizeCtl.
     * Must be at least 6 for 32bit arrays.
     */
    // sizeCtl中用于生成标记的位数。 对于 32 位数组，必须至少为 6。
    private static int RESIZE_STAMP_BITS = 16;

    /**
     * The maximum number of threads that can help resize.
     * Must fit in 32 - RESIZE_STAMP_BITS bits.
     */
    // 可以帮助调整大小的最大线程数。 必须适合 32 - RESIZE_STAMP_BITS 位。
    private static final int MAX_RESIZERS = (1 << (32 - RESIZE_STAMP_BITS)) - 1;

    /**
     * The bit shift for recording size stamp in sizeCtl.
     */
    // sizeCtl 中记录大小标记的位移位。
    private static final int RESIZE_STAMP_SHIFT = 32 - RESIZE_STAMP_BITS;

    /**
     * 20210619
     * 节点哈希字段的编码。 见上文解释。
     */
    /*
     * Encodings for Node hash fields. See above for explanation.
     */
    static final int MOVED     = -1; // hash for forwarding nodes // 转发节点的哈希
    static final int TREEBIN   = -2; // hash for roots of trees // 树根的散列
    static final int RESERVED  = -3; // hash for transient reservations // 临时保留的哈希
    static final int HASH_BITS = 0x7fffffff; // usable bits of normal node hash // 正常节点哈希的可用位

    /** Number of CPUS, to place bounds on some sizings */
    // CPUS 数量，用于限制某些尺寸
    static final int NCPU = Runtime.getRuntime().availableProcessors();

    /** For serialization compatibility. */
    // 为了序列化兼容性
    private static final ObjectStreamField[] serialPersistentFields = {
        new ObjectStreamField("segments", Segment[].class),
        new ObjectStreamField("segmentMask", Integer.TYPE),
        new ObjectStreamField("segmentShift", Integer.TYPE)
    };

    /* ---------------- Nodes -------------- */

    /**
     * 20210619
     * 键值输入。此类永远不会作为用户可变的Map.Entry（即一个支持 setValue；参见下面的 MapEntry）导出，但可用于批量任务中使用的只读遍历。
     * 具有负散列字段的Node子类是特殊的，包含空键和值（但从不导出）。否则，键和值永远不会为null。
     */
    /**
     * Key-value entry.  This class is never exported out as a
     * user-mutable Map.Entry (i.e., one supporting setValue; see
     * MapEntry below), but can be used for read-only traversals used
     * in bulk tasks.  Subclasses of Node with a negative hash field
     * are special, and contain null keys and values (but are never
     * exported).  Otherwise, keys and vals are never null.
     */
    static class Node<K,V> implements Map.Entry<K,V> {
        final int hash;
        final K key;
        volatile V val;
        volatile Node<K,V> next;

        Node(int hash, K key, V val, Node<K,V> next) {
            this.hash = hash;
            this.key = key;
            this.val = val;
            this.next = next;
        }

        public final K getKey()       { return key; }
        public final V getValue()     { return val; }
        public final int hashCode()   { return key.hashCode() ^ val.hashCode(); }
        public final String toString(){ return key + "=" + val; }
        public final V setValue(V value) {
            throw new UnsupportedOperationException();
        }

        public final boolean equals(Object o) {
            Object k, v, u; Map.Entry<?,?> e;
            return ((o instanceof Map.Entry) &&
                    (k = (e = (Map.Entry<?,?>)o).getKey()) != null &&
                    (v = e.getValue()) != null &&
                    (k == key || k.equals(key)) &&
                    (v == (u = val) || v.equals(u)));
        }

        /**
         * Virtualized support for map.get(); overridden in subclasses.
         */
        // 对 map.get() 的虚拟化支持； 在子类中被覆盖。
        Node<K,V> find(int h, Object k) {
            Node<K,V> e = this;
            if (k != null) {
                do {
                    K ek;
                    if (e.hash == h &&
                        ((ek = e.key) == k || (ek != null && k.equals(ek))))
                        return e;
                } while ((e = e.next) != null);
            }
            return null;
        }
    }

    /* ---------------- Static utilities -------------- */
    // 静态实用程序

    /**
     * 20210619
     * 将散列的较高位传播 (XOR) 到较低位，并强制将最高位设为0。由于该表使用二次幂掩码，因此仅在当前掩码上方位不同的散列集将始终冲突。
     * （众所周知的例子是在小表中保存连续整数的浮点键集。）所以我们应用了一种向下传播高位影响的变换。 位扩展的速度、效用和质量之间存在权衡。
     * 因为许多常见的哈希集已经合理分布（因此不会从传播中受益），并且因为我们使用树来处理 bin 中的大量冲突，所以我们只是以最便宜的方式对
     * 一些移位的位进行异或以减少系统损失， 以及合并最高位的影响，否则由于表边界而永远不会在索引计算中使用。
     */
    /**
     * Spreads (XORs) higher bits of hash to lower and also forces top
     * bit to 0. Because the table uses power-of-two masking, sets of
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
    // 将散列的较高位传播 (XOR) 到较低位，并强制将最高位设为0
    static final int spread(int h) {
        return (h ^ (h >>> 16)) & HASH_BITS;
    }

    /**
     * Returns a power of two table size for the given desired capacity.
     * See Hackers Delight, sec 3.2
     */
    // 为给定的所需容量返回表大小的 2 次幂。 参见 Hackers Delight，第 3.2 节
    private static final int tableSizeFor(int c) {
        int n = c - 1;
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
    }

    /**
     * Returns x's Class if it is of the form "class C implements
     * Comparable<C>", else null.
     */
    // 如果 x 的类的形式为“类 C 实现 Comparable<C>”，则返回 x 的类，否则返回 null。
    static Class<?> comparableClassFor(Object x) {
        if (x instanceof Comparable) {
            Class<?> c; Type[] ts, as; Type t; ParameterizedType p;
            if ((c = x.getClass()) == String.class) // bypass checks
                return c;
            if ((ts = c.getGenericInterfaces()) != null) {
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
        return null;
    }

    /**
     * Returns k.compareTo(x) if x matches kc (k's screened comparable
     * class), else 0.
     */
    // 如果 x 匹配 kc（k 的筛选可比类），则返回 k.compareTo(x)，否则返回 0。
    @SuppressWarnings({"rawtypes","unchecked"}) // for cast to Comparable
    static int compareComparables(Class<?> kc, Object k, Object x) {
        return (x == null || x.getClass() != kc ? 0 :
                ((Comparable)k).compareTo(x));
    }

    /* ---------------- Table element access -------------- */
    // 表元素访问

    /**
     * 20210619
     * 可变访问方法用于表元素以及调整大小时正在进行的下一个表的元素。 调用者必须对 tab 参数的所有使用进行空检查。
     * 所有调用者还偏执地预检查选项卡的长度不为零（或等效检查），从而确保采用散列值形式并带有 (length - 1) 的任何索引参数都是有效索引。
     * 请注意，为了纠正用户的任意并发错误，这些检查必须对局部变量进行操作，这说明了下面一些看起来很奇怪的内联分配。 请注意，对 setTabAt 的调用总是发生在锁定区域内，
     * 因此原则上只需要发布顺序，而不是完全 volatile 语义，但目前被编码为 volatile 写入是保守的。
     */
    /*
     * Volatile access methods are used for table elements as well as
     * elements of in-progress next table while resizing.  All uses of
     * the tab arguments must be null checked by callers.  All callers
     * also paranoically precheck that tab's length is not zero (or an
     * equivalent check), thus ensuring that any index argument taking
     * the form of a hash value anded with (length - 1) is a valid
     * index.  Note that, to be correct wrt arbitrary concurrency
     * errors by users, these checks must operate on local variables,
     * which accounts for some odd-looking inline assignments below.
     * Note that calls to setTabAt always occur within locked regions,
     * and so in principle require only release ordering, not
     * full volatile semantics, but are currently coded as volatile
     * writes to be conservative.
     */

    // volatile方式获取散列表tab中i位置的Node节点
    @SuppressWarnings("unchecked")
    static final <K,V> Node<K,V> tabAt(Node<K,V>[] tab, int i) {
        return (Node<K,V>)U.getObjectVolatile(tab, ((long)i << ASHIFT) + ABASE);
    }

    // CAS方式更新散列表tab中i位置的Node节点为v(原值必须为c才更新)
    static final <K,V> boolean casTabAt(Node<K,V>[] tab, int i,
                                        Node<K,V> c, Node<K,V> v) {
        return U.compareAndSwapObject(tab, ((long)i << ASHIFT) + ABASE, c, v);
    }

    // volatile方式添加v到散列表tab中i位置
    static final <K,V> void setTabAt(Node<K,V>[] tab, int i, Node<K,V> v) {
        U.putObjectVolatile(tab, ((long)i << ASHIFT) + ABASE, v);
    }

    /* ---------------- Fields -------------- */

    /**
     * The array of bins. Lazily initialized upon first insertion.
     * Size is always a power of two. Accessed directly by iterators.
     */
    // bin数组。第一次插入时延迟初始化。大小始终是2的幂。由迭代器直接访问。
    transient volatile Node<K,V>[] table;

    /**
     * The next table to use; non-null only while resizing.
     */
    // 下一个要使用的表；仅在调整大小时非null。
    private transient volatile Node<K,V>[] nextTable;

    /**
     * Base counter value, used mainly when there is no contention,
     * but also as a fallback during table initialization
     * races. Updated via CAS.
     */
    // 基本计数器值，主要在没有争用时使用，但也可作为表初始化竞争期间的后备。 通过 CAS 更新。
    private transient volatile long baseCount;

    /**
     * 20210619
     * 表初始化和调整大小控制。如果为负，则表正在初始化或调整大小：-1 表示初始化，否则 -（1 + 活动调整大小线程的数量）。
     * 否则，当 table 为 null 时，保存创建时使用的初始表大小，或默认为0。初始化后，保存下一个要调整表格大小的元素计数值。
     */
    /**
     * Table initialization and resizing control.  When negative, the
     * table is being initialized or resized: -1 for initialization,
     * else -(1 + the number of active resizing threads).  Otherwise,
     * when table is null, holds the initial table size to use upon
     * creation, or 0 for default. After initialization, holds the
     * next element count value upon which to resize the table.
     */
    // 并发阈值：通常等于0.75 * 容量, 但在构造函数中等于初始容量, 散列表正在初始化时为-1
    private transient volatile int sizeCtl;

    /**
     * The next table index (plus one) to split while resizing.
     */
    // 调整大小时要拆分的下一个表索引（加一）。
    private transient volatile int transferIndex;

    /**
     * Spinlock (locked via CAS) used when resizing and/or creating CounterCells.
     */
    // 调整大小或创建 CounterCell 时使用的自旋锁（通过 CAS 锁定）。
    private transient volatile int cellsBusy;

    /**
     * Table of counter cells. When non-null, size is a power of 2.
     */
    // 计数器单元格表。 当非null时，大小是 2 的幂。
    private transient volatile CounterCell[] counterCells;

    // views 视图
    private transient KeySetView<K,V> keySet;
    private transient ValuesView<K,V> values;
    private transient EntrySetView<K,V> entrySet;


    /* ---------------- Public operations -------------- */

    /**
     * Creates a new, empty map with the default initial table size (16).
     */
    // 使用默认的初始表大小 (16) 创建一个新的空映射。
    public ConcurrentHashMap() {
    }

    /**
     * Creates a new, empty map with an initial table size
     * accommodating the specified number of elements without the need
     * to dynamically resize.
     *
     * @param initialCapacity The implementation performs internal
     * sizing to accommodate this many elements.
     * @throws IllegalArgumentException if the initial capacity of
     * elements is negative
     */
    // 创建一个新的空Map，其初始表大小可容纳指定数量的元素，而无需动态调整大小。
    public ConcurrentHashMap(int initialCapacity) {
        if (initialCapacity < 0)
            throw new IllegalArgumentException();
        int cap = ((initialCapacity >= (MAXIMUM_CAPACITY >>> 1)) ?
                   MAXIMUM_CAPACITY :
                   tableSizeFor(initialCapacity + (initialCapacity >>> 1) + 1));
        this.sizeCtl = cap;
    }

    /**
     * Creates a new map with the same mappings as the given map.
     *
     * @param m the map
     */
    // 创建一个与给定Map具有相同映射的新Map。
    public ConcurrentHashMap(Map<? extends K, ? extends V> m) {
        this.sizeCtl = DEFAULT_CAPACITY;
        putAll(m);
    }

    /**
     * Creates a new, empty map with an initial table size based on
     * the given number of elements ({@code initialCapacity}) and
     * initial table density ({@code loadFactor}).
     *
     * @param initialCapacity the initial capacity. The implementation
     * performs internal sizing to accommodate this many elements,
     * given the specified load factor.
     * @param loadFactor the load factor (table density) for
     * establishing the initial table size
     * @throws IllegalArgumentException if the initial capacity of
     * elements is negative or the load factor is nonpositive
     *
     * @since 1.6
     */
    // 根据给定的元素数量 ({@code initialCapacity}) 和初始表密度 ({@code loadFactor}) 创建一个具有初始表大小的新空映射。
    public ConcurrentHashMap(int initialCapacity, float loadFactor) {
        this(initialCapacity, loadFactor, 1);
    }

    /**
     * Creates a new, empty map with an initial table size based on
     * the given number of elements ({@code initialCapacity}), table
     * density ({@code loadFactor}), and number of concurrently
     * updating threads ({@code concurrencyLevel}).
     *
     * // 初始容量。 给定指定的负载因子，实现会执行内部大小调整以容纳这么多元素。
     * @param initialCapacity the initial capacity. The implementation
     * performs internal sizing to accommodate this many elements,
     * given the specified load factor.
     *
     * // 用于建立初始表大小的加载因子（表密度）
     * @param loadFactor the load factor (table density) for
     * establishing the initial table size
     *
     * // 估计的并发更新线程数。 实现可以使用这个值作为调整大小的提示。
     * @param concurrencyLevel the estimated number of concurrently
     * updating threads. The implementation may use this value as
     * a sizing hint.
     *
     * // 如果初始容量为负或负载因子或 concurrencyLevel 为负数
     * @throws IllegalArgumentException if the initial capacity is
     * negative or the load factor or concurrencyLevel are
     * nonpositive
     */
    // 根据给定的元素数 ({@code initialCapacity})、表密度 ({@code loadFactor}) 和并发更新线程数 ({@code concurrencyLevel}) 创建一个具有初始表大小的新空映射。
    public ConcurrentHashMap(int initialCapacity,
                             float loadFactor, int concurrencyLevel) {
        if (!(loadFactor > 0.0f) || initialCapacity < 0 || concurrencyLevel <= 0)
            throw new IllegalArgumentException();
        // 使用至少与估计线程一样多的 bin
        if (initialCapacity < concurrencyLevel)   // Use at least as many bins
            initialCapacity = concurrencyLevel;   // as estimated threads
        long size = (long)(1.0 + (long)initialCapacity / loadFactor);
        int cap = (size >= (long)MAXIMUM_CAPACITY) ?
            MAXIMUM_CAPACITY : tableSizeFor((int)size);
        this.sizeCtl = cap;
    }

    // Original (since JDK1.2) Map methods

    /**
     * {@inheritDoc}
     */
    public int size() {
        long n = sumCount();
        return ((n < 0L) ? 0 :
                (n > (long)Integer.MAX_VALUE) ? Integer.MAX_VALUE :
                (int)n);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isEmpty() {
        return sumCount() <= 0L; // ignore transient negative values
    }

    /**
     * 20210623
     * A. 返回指定键映射到的值，如果此映射不包含键的映射，则返回 {@code null}。
     * B. 更正式地说，如果此映射包含从键 {@code k} 到值 {@code v} 的映射，使得 {@code key.equals(k)}，则此方法返回 {@code v}； 否则返回 {@code null}。
     *   （最多可以有一个这样的映射。）
     */
    /**
     * A.
     * Returns the value to which the specified key is mapped,
     * or {@code null} if this map contains no mapping for the key.
     *
     * B.
     * <p>More formally, if this map contains a mapping from a key
     * {@code k} to a value {@code v} such that {@code key.equals(k)},
     * then this method returns {@code v}; otherwise it returns
     * {@code null}.  (There can be at most one such mapping.)
     *
     * @throws NullPointerException if the specified key is null
     */
    // 获取指定键映射到的值，如果此映射不包含键的映射，则返回 {@code null}: 需要判断桶头结点e是否为转发结点、红黑树结点、computeIfAbsent临时结点
    public V get(Object key) {
        Node<K,V>[] tab; Node<K,V> e, p; int n, eh; K ek;

        // 将散列的较高位传播 (XOR) 到较低位，并强制将最高位设为0
        int h = spread(key.hashCode());

        // 当前散列表tab, Key的hash值h, tab容量n, h取模n后的桶e, e的hash值eh, e的键ek
        if ((tab = table) != null && (n = tab.length) > 0 && (e = tabAt(tab, (n - 1) & h)) != null) {

            // 如果e的hash值相等, 且Key相等或者Key equals, 说明桶头结点e就是要找到的结点, 此时返回e的value
            if ((eh = e.hash) == h) {
                if ((ek = e.key) == key || (ek != null && key.equals(ek)))
                    return e.val;
            }

            // 如果e的hash小于0, 说明e可能为转发结点、红黑树结点、computeIfAbsent临时结点
            else if (eh < 0)
                // 1. 如果e为转发结点, 则以转发结点方式根据hash以及key对象查找结点: hash相等, 且Key相等或者equals
                // 2. 如果e为红黑树结点, 则根据hash值和key值，从根结点开始查找红黑树结点 => 同HashMap#getTreeNode（int，Object）
                // 3. 如果e为computeIfAbsent临时结点, 则返回null
                return (p = e.find(h, key)) != null ? p.val : null;

            // 如果桶头没找e结点, 则继续遍历e链表, 找到hash值相等, 且Key相等或者equals结点并返回
            while ((e = e.next) != null) {
                if (e.hash == h && ((ek = e.key) == key || (ek != null && key.equals(ek))))
                    return e.val;
            }
        }

        // 如果实在找不到, 则返回null
        return null;
    }

    /**
     * Tests if the specified object is a key in this table.
     *
     * @param  key possible key
     * @return {@code true} if and only if the specified object
     *         is a key in this table, as determined by the
     *         {@code equals} method; {@code false} otherwise
     * @throws NullPointerException if the specified key is null
     */
    public boolean containsKey(Object key) {
        return get(key) != null;
    }

    /**
     * Returns {@code true} if this map maps one or more keys to the
     * specified value. Note: This method may require a full traversal
     * of the map, and is much slower than method {@code containsKey}.
     *
     * @param value value whose presence in this map is to be tested
     * @return {@code true} if this map maps one or more keys to the
     *         specified value
     * @throws NullPointerException if the specified value is null
     */
    public boolean containsValue(Object value) {
        if (value == null)
            throw new NullPointerException();
        Node<K,V>[] t;
        if ((t = table) != null) {
            Traverser<K,V> it = new Traverser<K,V>(t, t.length, 0, t.length);
            for (Node<K,V> p; (p = it.advance()) != null; ) {
                V v;
                if ((v = p.val) == value || (v != null && value.equals(v)))
                    return true;
            }
        }
        return false;
    }

    /**
     * 20210619
     * A. 将指定的键映射到此表中的指定值。 键和值都不能为null。
     * B. 可以通过使用与原始键相同的键调用 {@code get} 方法来检索该值。
     */
    /**
     * A.
     * Maps the specified key to the specified value in this table.
     * Neither the key nor the value can be null.
     *
     * B.
     * <p>The value can be retrieved by calling the {@code get} method
     * with a key that is equal to the original key.
     *
     * @param key key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with {@code key}, or
     *         {@code null} if there was no mapping for {@code key}
     * @throws NullPointerException if the specified key or value is null
     */
    public V put(K key, V value) {
        return putVal(key, value, false);
    }

    // put 和 putIfAbsent 的实现
    /** Implementation for put and putIfAbsent */
    // 将指定值和指定键相关联，形成key-value键值对，如果包含相同的键且onlyIfAbsent为false，则会替换旧值。
    final V putVal(K key, V value, boolean onlyIfAbsent) {
        if (key == null || value == null) throw new NullPointerException();

        // 将散列的较高位传播 (XOR) 到较低位，并强制将最高位设为0
        int hash = spread(key.hashCode());

        // 开始自旋, 散列表tab, 桶头结点f, 当前散列表容量n, Key的hash值索引i, 桶头结点f的hash值fh, f桶链上结点数量binCount
        int binCount = 0;
        for (Node<K,V>[] tab = table;;) {
            Node<K,V> f; int n, i, fh;

            // 如果tab为null或者tab容量n为0, 说明散列表还没被创建, 则创建散列表
            if (tab == null || (n = tab.length) == 0)
                // 使用并发阈值初始化散列表
                tab = initTable();

            // 如果tab已被初始化, 且桶头结点f为null, 则CAS方式更新散列表tab中i位置的Node节点为v, 设置成功则结束自旋, 否则继续下轮自旋
            else if ((f = tabAt(tab, i = (n - 1) & hash)) == null) {
                if (casTabAt(tab, i, null, new Node<K,V>(hash, key, value, null)))
                    break;                   // no lock when adding to empty bin 添加到空箱时没有锁定
            }

            // 如果tab已被初始化, 且桶头结点f不为null, 但f为转发结点, 说明当前散列表已经是旧的散列表了, 则当前线程协助转移tab元素到新的散列表中
            else if ((fh = f.hash) == MOVED)
                // 如果旧tab正在被其他线程扩容转移中, 则当前线程加入一起转移结点, 协助完成后返回新tab, 则基于新tab进行下一轮自旋(会把结点插入到新tab中)
                tab = helpTransfer(tab, f);

            // 如果tab已被初始化, 且桶头结点f不为null, 且f也不为转发结点, 说明f为正常结点, 可以在桶链上追加元素
            else {
                V oldVal = null;

                // 此时对桶头结点f进行加锁, 只有抢到锁的线程才能进行追加结点
                synchronized (f) {
                    // 再次检查当前桶头结点是否还为f, 如果是才继续追加, 否则说明桶头结点已被其他线程更新了, 则释放f锁, 继续自旋
                    if (tabAt(tab, i) == f) {
                        // 如果f的hash值fh大于等于0, 说明f为普通Node结点, 则更新binCount为1(1个Node), 然后按普通链表方式添加结点
                        if (fh >= 0) {
                            binCount = 1;
                            for (Node<K,V> e = f;; ++binCount) {
                                K ek;

                                // 如果已经存在该元素, 如果只允许在不存在时才添加则什么也不做, 否则替换旧值, 结束遍历
                                if (e.hash == hash && ((ek = e.key) == key || (ek != null && key.equals(ek)))) {
                                    oldVal = e.val;
                                    if (!onlyIfAbsent)
                                        e.val = value;
                                    break;
                                }

                                // 如果不存在该元素, 则直接添加到链尾, 结束遍历
                                Node<K,V> pred = e;
                                if ((e = e.next) == null) {
                                    pred.next = new Node<K,V>(hash, key, value, null);
                                    break;
                                }
                            }
                        }

                        // 如果f的hash值fh小于0, 且为TreeBin类型, 说明f链为红黑树, 则更新binCount为2(1个TreeBin + 1个TreeNode), 然后按照红黑树结点的添加方法添加该结点
                        else if (f instanceof TreeBin) {
                            Node<K,V> p;
                            binCount = 2;
                            // 红黑树结点的添加方法（插入成功则返回null，插入失败则返回已经存在的结点）=> 类似HashMap#putTreeVal（HashMap，Node，int，K，V）, 但这里还需要竞争TreeBin桶头结点的写锁
                            if ((p = ((TreeBin<K,V>)f).putTreeVal(hash, key, value)) != null) {
                                // 如果插入失败, 说明已经存在该元素, 此时p.val为旧值, 如果只允许在不存在时才添加则什么也不做, 否则替换旧值
                                oldVal = p.val;
                                if (!onlyIfAbsent)
                                    p.val = value;
                            }
                        }
                    }
                }

                // 释放桶头结点f的锁, 如果binCount不为0, 说明结点已经添加成功
                if (binCount != 0) {

                    // 如果f链上结点个数是否大于红黑树化阈值8, 则需要红黑树化f链
                    if (binCount >= TREEIFY_THRESHOLD)
                        treeifyBin(tab, i);

                    // 否则说明还不需要红黑树化, 如果存在旧值, 则说明发生的不是结点添加, 而是结点值替换, 此时只需要返回旧值即可, 无需更新实际大小和扩容
                    if (oldVal != null)
                        return oldVal;

                    // 否则说明为结点添加, 则结束自旋, 开始更新实际大小和扩容
                    break;
                }
            }
        }

        // 到这里说明发生了结点添加, 则baseCount或者CounterCell叠加x, 叠加后检查是否需要扩容, 如果需要则启动扩容并转移结点; 如果判断到其他线程正在扩容转移结点, 则当前线程进行协助扩容转移结点
        addCount(1L, binCount);

        // 结点添加成功, 返回null作为标志位
        return null;
    }

    /**
     * 20210623
     * 将所有映射从指定映射复制到此映射。 这些映射替换了此映射对当前在指定映射中的任何键所具有的任何映射。
     */
    /**
     * Copies all of the mappings from the specified map to this one.
     * These mappings replace any mappings that this map had for any of the
     * keys currently in the specified map.
     *
     * @param m mappings to be stored in this map
     */
    public void putAll(Map<? extends K, ? extends V> m) {
        // 初始化或者扩容当前散列表tab容量, 到最接近m.size的2次幂次容量, 可能会初始化tab、无需扩容tab、协助转移tab结点、扩容tab并转移结点
        tryPresize(m.size());

        // 遍历复制集合m, 将指定值和指定键相关联, 形成key-value键值对, 如果包含相同的键会替换旧值
        for (Map.Entry<? extends K, ? extends V> e : m.entrySet())
            putVal(e.getKey(), e.getValue(), false);
    }

    /**
     * 20210623
     * 从此映射中删除键（及其相应的值）。 如果键不在Map中，则此方法不执行任何操作。
     */
    /**
     * Removes the key (and its corresponding value) from this map.
     * This method does nothing if the key is not in the map.
     *
     * @param  key the key that needs to be removed
     * @return the previous value associated with {@code key}, or
     *         {@code null} if there was no mapping for {@code key}
     * @throws NullPointerException if the specified key is null
     */
    // 根据key删除结点, 删除成功会返回旧值, key可以为null
    public V remove(Object key) {
        // 根据key和cv删除结点, 删除成功会返回旧值
        return replaceNode(key, null, null);
    }

    /**
     * 20210623
     * 四个公共删除/替换方法的实现： 用v替换节点值，条件是cv匹配（如果非null）。如果结果值为null，则删除。
     */
    /**
     * Implementation for the four public remove/replace methods:
     * Replaces node value with v, conditional upon match of cv if
     * non-null.  If resulting value is null, delete.
     */
    // 根据key和cv删除/替换结点核心逻辑, 如果指定了value说明为替换模式, 此时会替换目标结点的value值为指定的value并返回null, 否则删除成功会返回旧值
    final V replaceNode(Object key, V value, Object cv) {
        // 将散列的较高位传播 (XOR) 到较低位，并强制将最高位设为0
        int hash = spread(key.hashCode());

        // 开始自旋, 当前散列表tab, tab容量n, hash值索引i, hash值索引对应的桶头结点f, f的hash值fh
        for (Node<K,V>[] tab = table;;) {
            Node<K,V> f; int n, i, fh;

            // 如果tab不存在, 或者n为0, 或者f为null, 则直接结束自旋, 返回null
            if (tab == null || (n = tab.length) == 0 || (f = tabAt(tab, i = (n - 1) & hash)) == null)
                break;

            // 如果tab已被初始化, 且桶头结点f不为null, 但f为转发结点, 说明当前散列表已经是旧的散列表了, 则当前线程协助转移tab元素到新的散列表中
            else if ((fh = f.hash) == MOVED)
                // 如果旧tab正在被其他线程扩容转移中, 则当前线程加入一起转移结点, 协助完成后返回新tab, 则基于新tab进行下一轮自旋(会新tab中删除对应的结点)
                tab = helpTransfer(tab, f);

            // 如果tab已被初始化, 且桶头结点f不为null, 且f也不为转发结点, 说明f为正常结点, 可以在tab上删除结点
            else {
                V oldVal = null;
                boolean validated = false;// 是否有真正做了结点删除, 有的话为true, 代表需要更新散列表实际大小并返回旧值

                // 此时对桶头结点f进行加锁, 只有抢到锁的线程才能进行删除结点
                synchronized (f) {
                    // 再次检查当前桶头结点是否还为f, 如果是才继续删除, 否则说明桶头结点已被其他线程更新了, 则释放f锁, 继续自旋
                    if (tabAt(tab, i) == f) {
                        // 如果f的hash值fh大于等于0, 说明f为普通Node结点, 则普通链表方式删除结点
                        if (fh >= 0) {
                            validated = true;

                            // 以链表方式遍历f链表, 当前遍历结点e, e键ek, 根据e重新构造的结点p
                            for (Node<K,V> e = f, pred = null;;) {
                                K ek;

                                // 如果找到e的hash值相等, 且e键相等的或者e键equals的结点, 则继续比较e值与cv是否相等或者e值e与cv是否quals
                                if (e.hash == hash && ((ek = e.key) == key || (ek != null && key.equals(ek)))) {
                                    V ev = e.val;

                                    // 如果k和cv都相等或者equals, 则说明找到了对应的结点
                                    if (cv == null || cv == ev || (ev != null && cv.equals(ev))) {
                                        oldVal = ev;

                                        // 如果指定了不为null的value, 则替换e结点的value
                                        if (value != null)
                                            e.val = value;
                                        // 如果没指定value, 且e结点还有前驱, 则链接e的前驱和后继, 脱钩e结点
                                        else if (pred != null)
                                            pred.next = e.next;
                                        // 如果没指定value, 但e结点没有了前驱, 说明e为头结点, 则volatile方式添加后继为头结点的链表到散列表tab的i桶中
                                        else
                                            setTabAt(tab, i, e.next);
                                    }

                                    // 结束链表遍历, 返回null, 再去更新散列表实际大小并返回旧值
                                    break;
                                }
                                pred = e;

                                // 如果连k匹配的结点都找不到, 则结束链表遍历, 返回null, 再去更新散列表实际大小并返回旧值
                                if ((e = e.next) == null)
                                    break;
                            }
                        }

                        // 如果f的hash值fh小于0, 且为TreeBin类型, 说明f链为红黑树, 则按照红黑树结点方式删除结点
                        else if (f instanceof TreeBin) {
                            validated = true;
                            TreeBin<K,V> t = (TreeBin<K,V>)f;
                            TreeNode<K,V> r, p;

                            // 根据hash值和key值, 从根结点r开始查找红黑树结点p, 如果找到继续匹配cv, 否则释放f锁, 再去散列表实际大小并返回旧值
                            if ((r = t.root) != null && (p = r.findTreeNode(hash, key, null)) != null) {
                                V pv = p.val;

                                // 如果k和cv都相等或者equals, 则说明找到了对应的结点
                                if (cv == null || cv == pv || (pv != null && cv.equals(pv))) {
                                    oldVal = pv;

                                    // 如果指定了不为null的value, 则替换e结点的value, 再去散列表实际大小并返回旧值
                                    if (value != null)
                                        p.val = value;

                                    // 如果没指定value, 则调用红黑树结点的删除方法, 返回false, 说明正常插入结点到红黑树了, 不需要拆除当前红黑树退化成普通链表
                                    else if (t.removeTreeNode(p))
                                        // 如果返回true, 则普通化t.first链表, 并volatile方式添加到散列表tab中i位置, 再去散列表实际大小并返回旧值
                                        setTabAt(tab, i, untreeify(t.first));
                                }
                            }
                        }
                    }
                }

                // 为true说明真正做了结点删除, 需要更新散列表实际大小并返回旧值
                if (validated) {
                    if (oldVal != null) {
                        // 如果没指定value, 说明不是替换模式, 则返回旧值
                        if (value == null)
                            // baseCount或者CounterCell叠加x, 删除操作时这里无需协助扩容
                            addCount(-1L, -1);
                        return oldVal;
                    }
                    break;
                }
            }
        }

        // 没有做到结点删除, 返回null即可(比如替换操作或者没找要删除的结点时)
        return null;
    }

    /**
     * Removes all of the mappings from this map.
     */
    public void clear() {
        long delta = 0L; // negative number of deletions
        int i = 0;
        Node<K,V>[] tab = table;
        while (tab != null && i < tab.length) {
            int fh;
            Node<K,V> f = tabAt(tab, i);
            if (f == null)
                ++i;
            else if ((fh = f.hash) == MOVED) {
                tab = helpTransfer(tab, f);
                i = 0; // restart
            }
            else {
                synchronized (f) {
                    if (tabAt(tab, i) == f) {
                        Node<K,V> p = (fh >= 0 ? f :
                                       (f instanceof TreeBin) ?
                                       ((TreeBin<K,V>)f).first : null);
                        while (p != null) {
                            --delta;
                            p = p.next;
                        }
                        setTabAt(tab, i++, null);
                    }
                }
            }
        }
        if (delta != 0L)
            addCount(delta, -1);
    }

    /**
     * Returns a {@link Set} view of the keys contained in this map.
     * The set is backed by the map, so changes to the map are
     * reflected in the set, and vice-versa. The set supports element
     * removal, which removes the corresponding mapping from this map,
     * via the {@code Iterator.remove}, {@code Set.remove},
     * {@code removeAll}, {@code retainAll}, and {@code clear}
     * operations.  It does not support the {@code add} or
     * {@code addAll} operations.
     *
     * <p>The view's iterators and spliterators are
     * <a href="package-summary.html#Weakly"><i>weakly consistent</i></a>.
     *
     * <p>The view's {@code spliterator} reports {@link Spliterator#CONCURRENT},
     * {@link Spliterator#DISTINCT}, and {@link Spliterator#NONNULL}.
     *
     * @return the set view
     */
    public KeySetView<K,V> keySet() {
        KeySetView<K,V> ks;
        return (ks = keySet) != null ? ks : (keySet = new KeySetView<K,V>(this, null));
    }

    /**
     * Returns a {@link Collection} view of the values contained in this map.
     * The collection is backed by the map, so changes to the map are
     * reflected in the collection, and vice-versa.  The collection
     * supports element removal, which removes the corresponding
     * mapping from this map, via the {@code Iterator.remove},
     * {@code Collection.remove}, {@code removeAll},
     * {@code retainAll}, and {@code clear} operations.  It does not
     * support the {@code add} or {@code addAll} operations.
     *
     * <p>The view's iterators and spliterators are
     * <a href="package-summary.html#Weakly"><i>weakly consistent</i></a>.
     *
     * <p>The view's {@code spliterator} reports {@link Spliterator#CONCURRENT}
     * and {@link Spliterator#NONNULL}.
     *
     * @return the collection view
     */
    public Collection<V> values() {
        ValuesView<K,V> vs;
        return (vs = values) != null ? vs : (values = new ValuesView<K,V>(this));
    }

    /**
     * Returns a {@link Set} view of the mappings contained in this map.
     * The set is backed by the map, so changes to the map are
     * reflected in the set, and vice-versa.  The set supports element
     * removal, which removes the corresponding mapping from the map,
     * via the {@code Iterator.remove}, {@code Set.remove},
     * {@code removeAll}, {@code retainAll}, and {@code clear}
     * operations.
     *
     * <p>The view's iterators and spliterators are
     * <a href="package-summary.html#Weakly"><i>weakly consistent</i></a>.
     *
     * <p>The view's {@code spliterator} reports {@link Spliterator#CONCURRENT},
     * {@link Spliterator#DISTINCT}, and {@link Spliterator#NONNULL}.
     *
     * @return the set view
     */
    public Set<Map.Entry<K,V>> entrySet() {
        EntrySetView<K,V> es;
        return (es = entrySet) != null ? es : (entrySet = new EntrySetView<K,V>(this));
    }

    /**
     * Returns the hash code value for this {@link Map}, i.e.,
     * the sum of, for each key-value pair in the map,
     * {@code key.hashCode() ^ value.hashCode()}.
     *
     * @return the hash code value for this map
     */
    public int hashCode() {
        int h = 0;
        Node<K,V>[] t;
        if ((t = table) != null) {
            Traverser<K,V> it = new Traverser<K,V>(t, t.length, 0, t.length);
            for (Node<K,V> p; (p = it.advance()) != null; )
                h += p.key.hashCode() ^ p.val.hashCode();
        }
        return h;
    }

    /**
     * Returns a string representation of this map.  The string
     * representation consists of a list of key-value mappings (in no
     * particular order) enclosed in braces ("{@code {}}").  Adjacent
     * mappings are separated by the characters {@code ", "} (comma
     * and space).  Each key-value mapping is rendered as the key
     * followed by an equals sign ("{@code =}") followed by the
     * associated value.
     *
     * @return a string representation of this map
     */
    public String toString() {
        Node<K,V>[] t;
        int f = (t = table) == null ? 0 : t.length;
        Traverser<K,V> it = new Traverser<K,V>(t, f, 0, f);
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        Node<K,V> p;
        if ((p = it.advance()) != null) {
            for (;;) {
                K k = p.key;
                V v = p.val;
                sb.append(k == this ? "(this Map)" : k);
                sb.append('=');
                sb.append(v == this ? "(this Map)" : v);
                if ((p = it.advance()) == null)
                    break;
                sb.append(',').append(' ');
            }
        }
        return sb.append('}').toString();
    }

    /**
     * Compares the specified object with this map for equality.
     * Returns {@code true} if the given object is a map with the same
     * mappings as this map.  This operation may return misleading
     * results if either map is concurrently modified during execution
     * of this method.
     *
     * @param o object to be compared for equality with this map
     * @return {@code true} if the specified object is equal to this map
     */
    public boolean equals(Object o) {
        if (o != this) {
            if (!(o instanceof Map))
                return false;
            Map<?,?> m = (Map<?,?>) o;
            Node<K,V>[] t;
            int f = (t = table) == null ? 0 : t.length;
            Traverser<K,V> it = new Traverser<K,V>(t, f, 0, f);
            for (Node<K,V> p; (p = it.advance()) != null; ) {
                V val = p.val;
                Object v = m.get(p.key);
                if (v == null || (v != val && !v.equals(val)))
                    return false;
            }
            for (Map.Entry<?,?> e : m.entrySet()) {
                Object mk, mv, v;
                if ((mk = e.getKey()) == null ||
                    (mv = e.getValue()) == null ||
                    (v = get(mk)) == null ||
                    (mv != v && !mv.equals(v)))
                    return false;
            }
        }
        return true;
    }

    /**
     * Stripped-down version of helper class used in previous version,
     * declared for the sake of serialization compatibility
     */
    static class Segment<K,V> extends ReentrantLock implements Serializable {
        private static final long serialVersionUID = 2249069246763182397L;
        final float loadFactor;
        Segment(float lf) { this.loadFactor = lf; }
    }

    /**
     * Saves the state of the {@code ConcurrentHashMap} instance to a
     * stream (i.e., serializes it).
     * @param s the stream
     * @throws java.io.IOException if an I/O error occurs
     * @serialData
     * the key (Object) and value (Object)
     * for each key-value mapping, followed by a null pair.
     * The key-value mappings are emitted in no particular order.
     */
    private void writeObject(java.io.ObjectOutputStream s)
        throws java.io.IOException {
        // For serialization compatibility
        // Emulate segment calculation from previous version of this class
        int sshift = 0;
        int ssize = 1;
        while (ssize < DEFAULT_CONCURRENCY_LEVEL) {
            ++sshift;
            ssize <<= 1;
        }
        int segmentShift = 32 - sshift;
        int segmentMask = ssize - 1;
        @SuppressWarnings("unchecked")
        Segment<K,V>[] segments = (Segment<K,V>[])
            new Segment<?,?>[DEFAULT_CONCURRENCY_LEVEL];
        for (int i = 0; i < segments.length; ++i)
            segments[i] = new Segment<K,V>(LOAD_FACTOR);
        s.putFields().put("segments", segments);
        s.putFields().put("segmentShift", segmentShift);
        s.putFields().put("segmentMask", segmentMask);
        s.writeFields();

        Node<K,V>[] t;
        if ((t = table) != null) {
            Traverser<K,V> it = new Traverser<K,V>(t, t.length, 0, t.length);
            for (Node<K,V> p; (p = it.advance()) != null; ) {
                s.writeObject(p.key);
                s.writeObject(p.val);
            }
        }
        s.writeObject(null);
        s.writeObject(null);
        segments = null; // throw away
    }

    /**
     * Reconstitutes the instance from a stream (that is, deserializes it).
     * @param s the stream
     * @throws ClassNotFoundException if the class of a serialized object
     *         could not be found
     * @throws java.io.IOException if an I/O error occurs
     */
    private void readObject(java.io.ObjectInputStream s)
        throws java.io.IOException, ClassNotFoundException {
        /*
         * To improve performance in typical cases, we create nodes
         * while reading, then place in table once size is known.
         * However, we must also validate uniqueness and deal with
         * overpopulated bins while doing so, which requires
         * specialized versions of putVal mechanics.
         */
        sizeCtl = -1; // force exclusion for table construction
        s.defaultReadObject();
        long size = 0L;
        Node<K,V> p = null;
        for (;;) {
            @SuppressWarnings("unchecked")
            K k = (K) s.readObject();
            @SuppressWarnings("unchecked")
            V v = (V) s.readObject();
            if (k != null && v != null) {
                p = new Node<K,V>(spread(k.hashCode()), k, v, p);
                ++size;
            }
            else
                break;
        }
        if (size == 0L)
            sizeCtl = 0;
        else {
            int n;
            if (size >= (long)(MAXIMUM_CAPACITY >>> 1))
                n = MAXIMUM_CAPACITY;
            else {
                int sz = (int)size;
                n = tableSizeFor(sz + (sz >>> 1) + 1);
            }
            @SuppressWarnings("unchecked")
            Node<K,V>[] tab = (Node<K,V>[])new Node<?,?>[n];
            int mask = n - 1;
            long added = 0L;
            while (p != null) {
                boolean insertAtFront;
                Node<K,V> next = p.next, first;
                int h = p.hash, j = h & mask;
                if ((first = tabAt(tab, j)) == null)
                    insertAtFront = true;
                else {
                    K k = p.key;
                    if (first.hash < 0) {
                        TreeBin<K,V> t = (TreeBin<K,V>)first;
                        if (t.putTreeVal(h, k, p.val) == null)
                            ++added;
                        insertAtFront = false;
                    }
                    else {
                        int binCount = 0;
                        insertAtFront = true;
                        Node<K,V> q; K qk;
                        for (q = first; q != null; q = q.next) {
                            if (q.hash == h &&
                                ((qk = q.key) == k ||
                                 (qk != null && k.equals(qk)))) {
                                insertAtFront = false;
                                break;
                            }
                            ++binCount;
                        }
                        if (insertAtFront && binCount >= TREEIFY_THRESHOLD) {
                            insertAtFront = false;
                            ++added;
                            p.next = first;
                            TreeNode<K,V> hd = null, tl = null;
                            for (q = p; q != null; q = q.next) {
                                TreeNode<K,V> t = new TreeNode<K,V>
                                    (q.hash, q.key, q.val, null, null);
                                if ((t.prev = tl) == null)
                                    hd = t;
                                else
                                    tl.next = t;
                                tl = t;
                            }
                            setTabAt(tab, j, new TreeBin<K,V>(hd));
                        }
                    }
                }
                if (insertAtFront) {
                    ++added;
                    p.next = first;
                    setTabAt(tab, j, p);
                }
                p = next;
            }
            table = tab;
            sizeCtl = n - (n >>> 2);
            baseCount = added;
        }
    }

    // ConcurrentMap methods

    /**
     * {@inheritDoc}
     *
     * @return the previous value associated with the specified key,
     *         or {@code null} if there was no mapping for the key
     * @throws NullPointerException if the specified key or value is null
     */
    public V putIfAbsent(K key, V value) {
        return putVal(key, value, true);
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if the specified key is null
     */
    // 根据key和value删除结点, 删除成功会返回旧值, key不可以为null
    public boolean remove(Object key, Object value) {
        // 根据key和cv删除结点, 删除成功会返回旧值
        if (key == null) throw new NullPointerException();
        return value != null && replaceNode(key, null, value) != null;
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException if any of the arguments are null
     */
    public boolean replace(K key, V oldValue, V newValue) {
        if (key == null || oldValue == null || newValue == null)
            throw new NullPointerException();
        return replaceNode(key, newValue, oldValue) != null;
    }

    /**
     * {@inheritDoc}
     *
     * @return the previous value associated with the specified key,
     *         or {@code null} if there was no mapping for the key
     * @throws NullPointerException if the specified key or value is null
     */
    public V replace(K key, V value) {
        if (key == null || value == null)
            throw new NullPointerException();
        return replaceNode(key, value, null);
    }

    // Overrides of JDK8+ Map extension method defaults

    /**
     * Returns the value to which the specified key is mapped, or the
     * given default value if this map contains no mapping for the
     * key.
     *
     * @param key the key whose associated value is to be returned
     * @param defaultValue the value to return if this map contains
     * no mapping for the given key
     * @return the mapping for the key, if present; else the default value
     * @throws NullPointerException if the specified key is null
     */
    // 带默认值的获取, 如果获取不到key的元素, 则返回默认值，底层依赖于get（Object）方法: 需要判断桶头结点e是否为转发结点、红黑树结点、computeIfAbsent临时结点
    public V getOrDefault(Object key, V defaultValue) {
        V v;
        return (v = get(key)) == null ? defaultValue : v;
    }

    public void forEach(BiConsumer<? super K, ? super V> action) {
        if (action == null) throw new NullPointerException();
        Node<K,V>[] t;
        if ((t = table) != null) {
            Traverser<K,V> it = new Traverser<K,V>(t, t.length, 0, t.length);
            for (Node<K,V> p; (p = it.advance()) != null; ) {
                action.accept(p.key, p.val);
            }
        }
    }

    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        if (function == null) throw new NullPointerException();
        Node<K,V>[] t;
        if ((t = table) != null) {
            Traverser<K,V> it = new Traverser<K,V>(t, t.length, 0, t.length);
            for (Node<K,V> p; (p = it.advance()) != null; ) {
                V oldValue = p.val;
                for (K key = p.key;;) {
                    V newValue = function.apply(key, oldValue);
                    if (newValue == null)
                        throw new NullPointerException();
                    if (replaceNode(key, newValue, oldValue) != null ||
                        (oldValue = get(key)) == null)
                        break;
                }
            }
        }
    }

    /**
     * If the specified key is not already associated with a value,
     * attempts to compute its value using the given mapping function
     * and enters it into this map unless {@code null}.  The entire
     * method invocation is performed atomically, so the function is
     * applied at most once per key.  Some attempted update operations
     * on this map by other threads may be blocked while computation
     * is in progress, so the computation should be short and simple,
     * and must not attempt to update any other mappings of this map.
     *
     * @param key key with which the specified value is to be associated
     * @param mappingFunction the function to compute a value
     * @return the current (existing or computed) value associated with
     *         the specified key, or null if the computed value is null
     * @throws NullPointerException if the specified key or mappingFunction
     *         is null
     * @throws IllegalStateException if the computation detectably
     *         attempts a recursive update to this map that would
     *         otherwise never complete
     * @throws RuntimeException or Error if the mappingFunction does so,
     *         in which case the mapping is left unestablished
     */
    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        if (key == null || mappingFunction == null)
            throw new NullPointerException();
        int h = spread(key.hashCode());
        V val = null;
        int binCount = 0;
        for (Node<K,V>[] tab = table;;) {
            Node<K,V> f; int n, i, fh;
            if (tab == null || (n = tab.length) == 0)
                tab = initTable();
            else if ((f = tabAt(tab, i = (n - 1) & h)) == null) {
                Node<K,V> r = new ReservationNode<K,V>();
                synchronized (r) {
                    if (casTabAt(tab, i, null, r)) {
                        binCount = 1;
                        Node<K,V> node = null;
                        try {
                            if ((val = mappingFunction.apply(key)) != null)
                                node = new Node<K,V>(h, key, val, null);
                        } finally {
                            setTabAt(tab, i, node);
                        }
                    }
                }
                if (binCount != 0)
                    break;
            }
            else if ((fh = f.hash) == MOVED)
                tab = helpTransfer(tab, f);
            else {
                boolean added = false;
                synchronized (f) {
                    if (tabAt(tab, i) == f) {
                        if (fh >= 0) {
                            binCount = 1;
                            for (Node<K,V> e = f;; ++binCount) {
                                K ek; V ev;
                                if (e.hash == h &&
                                    ((ek = e.key) == key ||
                                     (ek != null && key.equals(ek)))) {
                                    val = e.val;
                                    break;
                                }
                                Node<K,V> pred = e;
                                if ((e = e.next) == null) {
                                    if ((val = mappingFunction.apply(key)) != null) {
                                        added = true;
                                        pred.next = new Node<K,V>(h, key, val, null);
                                    }
                                    break;
                                }
                            }
                        }
                        else if (f instanceof TreeBin) {
                            binCount = 2;
                            TreeBin<K,V> t = (TreeBin<K,V>)f;
                            TreeNode<K,V> r, p;
                            if ((r = t.root) != null &&
                                (p = r.findTreeNode(h, key, null)) != null)
                                val = p.val;
                            else if ((val = mappingFunction.apply(key)) != null) {
                                added = true;
                                t.putTreeVal(h, key, val);
                            }
                        }
                    }
                }
                if (binCount != 0) {
                    if (binCount >= TREEIFY_THRESHOLD)
                        treeifyBin(tab, i);
                    if (!added)
                        return val;
                    break;
                }
            }
        }
        if (val != null)
            addCount(1L, binCount);
        return val;
    }

    /**
     * If the value for the specified key is present, attempts to
     * compute a new mapping given the key and its current mapped
     * value.  The entire method invocation is performed atomically.
     * Some attempted update operations on this map by other threads
     * may be blocked while computation is in progress, so the
     * computation should be short and simple, and must not attempt to
     * update any other mappings of this map.
     *
     * @param key key with which a value may be associated
     * @param remappingFunction the function to compute a value
     * @return the new value associated with the specified key, or null if none
     * @throws NullPointerException if the specified key or remappingFunction
     *         is null
     * @throws IllegalStateException if the computation detectably
     *         attempts a recursive update to this map that would
     *         otherwise never complete
     * @throws RuntimeException or Error if the remappingFunction does so,
     *         in which case the mapping is unchanged
     */
    public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        if (key == null || remappingFunction == null)
            throw new NullPointerException();
        int h = spread(key.hashCode());
        V val = null;
        int delta = 0;
        int binCount = 0;
        for (Node<K,V>[] tab = table;;) {
            Node<K,V> f; int n, i, fh;
            if (tab == null || (n = tab.length) == 0)
                tab = initTable();
            else if ((f = tabAt(tab, i = (n - 1) & h)) == null)
                break;
            else if ((fh = f.hash) == MOVED)
                tab = helpTransfer(tab, f);
            else {
                synchronized (f) {
                    if (tabAt(tab, i) == f) {
                        if (fh >= 0) {
                            binCount = 1;
                            for (Node<K,V> e = f, pred = null;; ++binCount) {
                                K ek;
                                if (e.hash == h &&
                                    ((ek = e.key) == key ||
                                     (ek != null && key.equals(ek)))) {
                                    val = remappingFunction.apply(key, e.val);
                                    if (val != null)
                                        e.val = val;
                                    else {
                                        delta = -1;
                                        Node<K,V> en = e.next;
                                        if (pred != null)
                                            pred.next = en;
                                        else
                                            setTabAt(tab, i, en);
                                    }
                                    break;
                                }
                                pred = e;
                                if ((e = e.next) == null)
                                    break;
                            }
                        }
                        else if (f instanceof TreeBin) {
                            binCount = 2;
                            TreeBin<K,V> t = (TreeBin<K,V>)f;
                            TreeNode<K,V> r, p;
                            if ((r = t.root) != null &&
                                (p = r.findTreeNode(h, key, null)) != null) {
                                val = remappingFunction.apply(key, p.val);
                                if (val != null)
                                    p.val = val;
                                else {
                                    delta = -1;
                                    if (t.removeTreeNode(p))
                                        setTabAt(tab, i, untreeify(t.first));
                                }
                            }
                        }
                    }
                }
                if (binCount != 0)
                    break;
            }
        }
        if (delta != 0)
            addCount((long)delta, binCount);
        return val;
    }

    /**
     * Attempts to compute a mapping for the specified key and its
     * current mapped value (or {@code null} if there is no current
     * mapping). The entire method invocation is performed atomically.
     * Some attempted update operations on this map by other threads
     * may be blocked while computation is in progress, so the
     * computation should be short and simple, and must not attempt to
     * update any other mappings of this Map.
     *
     * @param key key with which the specified value is to be associated
     * @param remappingFunction the function to compute a value
     * @return the new value associated with the specified key, or null if none
     * @throws NullPointerException if the specified key or remappingFunction
     *         is null
     * @throws IllegalStateException if the computation detectably
     *         attempts a recursive update to this map that would
     *         otherwise never complete
     * @throws RuntimeException or Error if the remappingFunction does so,
     *         in which case the mapping is unchanged
     */
    public V compute(K key,
                     BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        if (key == null || remappingFunction == null)
            throw new NullPointerException();
        int h = spread(key.hashCode());
        V val = null;
        int delta = 0;
        int binCount = 0;
        for (Node<K,V>[] tab = table;;) {
            Node<K,V> f; int n, i, fh;
            if (tab == null || (n = tab.length) == 0)
                tab = initTable();
            else if ((f = tabAt(tab, i = (n - 1) & h)) == null) {
                Node<K,V> r = new ReservationNode<K,V>();
                synchronized (r) {
                    if (casTabAt(tab, i, null, r)) {
                        binCount = 1;
                        Node<K,V> node = null;
                        try {
                            if ((val = remappingFunction.apply(key, null)) != null) {
                                delta = 1;
                                node = new Node<K,V>(h, key, val, null);
                            }
                        } finally {
                            setTabAt(tab, i, node);
                        }
                    }
                }
                if (binCount != 0)
                    break;
            }
            else if ((fh = f.hash) == MOVED)
                tab = helpTransfer(tab, f);
            else {
                synchronized (f) {
                    if (tabAt(tab, i) == f) {
                        if (fh >= 0) {
                            binCount = 1;
                            for (Node<K,V> e = f, pred = null;; ++binCount) {
                                K ek;
                                if (e.hash == h &&
                                    ((ek = e.key) == key ||
                                     (ek != null && key.equals(ek)))) {
                                    val = remappingFunction.apply(key, e.val);
                                    if (val != null)
                                        e.val = val;
                                    else {
                                        delta = -1;
                                        Node<K,V> en = e.next;
                                        if (pred != null)
                                            pred.next = en;
                                        else
                                            setTabAt(tab, i, en);
                                    }
                                    break;
                                }
                                pred = e;
                                if ((e = e.next) == null) {
                                    val = remappingFunction.apply(key, null);
                                    if (val != null) {
                                        delta = 1;
                                        pred.next =
                                            new Node<K,V>(h, key, val, null);
                                    }
                                    break;
                                }
                            }
                        }
                        else if (f instanceof TreeBin) {
                            binCount = 1;
                            TreeBin<K,V> t = (TreeBin<K,V>)f;
                            TreeNode<K,V> r, p;
                            if ((r = t.root) != null)
                                p = r.findTreeNode(h, key, null);
                            else
                                p = null;
                            V pv = (p == null) ? null : p.val;
                            val = remappingFunction.apply(key, pv);
                            if (val != null) {
                                if (p != null)
                                    p.val = val;
                                else {
                                    delta = 1;
                                    t.putTreeVal(h, key, val);
                                }
                            }
                            else if (p != null) {
                                delta = -1;
                                if (t.removeTreeNode(p))
                                    setTabAt(tab, i, untreeify(t.first));
                            }
                        }
                    }
                }
                if (binCount != 0) {
                    if (binCount >= TREEIFY_THRESHOLD)
                        treeifyBin(tab, i);
                    break;
                }
            }
        }
        if (delta != 0)
            addCount((long)delta, binCount);
        return val;
    }

    /**
     * If the specified key is not already associated with a
     * (non-null) value, associates it with the given value.
     * Otherwise, replaces the value with the results of the given
     * remapping function, or removes if {@code null}. The entire
     * method invocation is performed atomically.  Some attempted
     * update operations on this map by other threads may be blocked
     * while computation is in progress, so the computation should be
     * short and simple, and must not attempt to update any other
     * mappings of this Map.
     *
     * @param key key with which the specified value is to be associated
     * @param value the value to use if absent
     * @param remappingFunction the function to recompute a value if present
     * @return the new value associated with the specified key, or null if none
     * @throws NullPointerException if the specified key or the
     *         remappingFunction is null
     * @throws RuntimeException or Error if the remappingFunction does so,
     *         in which case the mapping is unchanged
     */
    public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        if (key == null || value == null || remappingFunction == null)
            throw new NullPointerException();
        int h = spread(key.hashCode());
        V val = null;
        int delta = 0;
        int binCount = 0;
        for (Node<K,V>[] tab = table;;) {
            Node<K,V> f; int n, i, fh;
            if (tab == null || (n = tab.length) == 0)
                tab = initTable();
            else if ((f = tabAt(tab, i = (n - 1) & h)) == null) {
                if (casTabAt(tab, i, null, new Node<K,V>(h, key, value, null))) {
                    delta = 1;
                    val = value;
                    break;
                }
            }
            else if ((fh = f.hash) == MOVED)
                tab = helpTransfer(tab, f);
            else {
                synchronized (f) {
                    if (tabAt(tab, i) == f) {
                        if (fh >= 0) {
                            binCount = 1;
                            for (Node<K,V> e = f, pred = null;; ++binCount) {
                                K ek;
                                if (e.hash == h &&
                                    ((ek = e.key) == key ||
                                     (ek != null && key.equals(ek)))) {
                                    val = remappingFunction.apply(e.val, value);
                                    if (val != null)
                                        e.val = val;
                                    else {
                                        delta = -1;
                                        Node<K,V> en = e.next;
                                        if (pred != null)
                                            pred.next = en;
                                        else
                                            setTabAt(tab, i, en);
                                    }
                                    break;
                                }
                                pred = e;
                                if ((e = e.next) == null) {
                                    delta = 1;
                                    val = value;
                                    pred.next =
                                        new Node<K,V>(h, key, val, null);
                                    break;
                                }
                            }
                        }
                        else if (f instanceof TreeBin) {
                            binCount = 2;
                            TreeBin<K,V> t = (TreeBin<K,V>)f;
                            TreeNode<K,V> r = t.root;
                            TreeNode<K,V> p = (r == null) ? null :
                                r.findTreeNode(h, key, null);
                            val = (p == null) ? value :
                                remappingFunction.apply(p.val, value);
                            if (val != null) {
                                if (p != null)
                                    p.val = val;
                                else {
                                    delta = 1;
                                    t.putTreeVal(h, key, val);
                                }
                            }
                            else if (p != null) {
                                delta = -1;
                                if (t.removeTreeNode(p))
                                    setTabAt(tab, i, untreeify(t.first));
                            }
                        }
                    }
                }
                if (binCount != 0) {
                    if (binCount >= TREEIFY_THRESHOLD)
                        treeifyBin(tab, i);
                    break;
                }
            }
        }
        if (delta != 0)
            addCount((long)delta, binCount);
        return val;
    }

    // Hashtable legacy methods

    /**
     * Legacy method testing if some key maps into the specified value
     * in this table.  This method is identical in functionality to
     * {@link #containsValue(Object)}, and exists solely to ensure
     * full compatibility with class {@link java.util.Hashtable},
     * which supported this method prior to introduction of the
     * Java Collections framework.
     *
     * @param  value a value to search for
     * @return {@code true} if and only if some key maps to the
     *         {@code value} argument in this table as
     *         determined by the {@code equals} method;
     *         {@code false} otherwise
     * @throws NullPointerException if the specified value is null
     */
    public boolean contains(Object value) {
        return containsValue(value);
    }

    /**
     * Returns an enumeration of the keys in this table.
     *
     * @return an enumeration of the keys in this table
     * @see #keySet()
     */
    public Enumeration<K> keys() {
        Node<K,V>[] t;
        int f = (t = table) == null ? 0 : t.length;
        return new KeyIterator<K,V>(t, f, 0, f, this);
    }

    /**
     * Returns an enumeration of the values in this table.
     *
     * @return an enumeration of the values in this table
     * @see #values()
     */
    public Enumeration<V> elements() {
        Node<K,V>[] t;
        int f = (t = table) == null ? 0 : t.length;
        return new ValueIterator<K,V>(t, f, 0, f, this);
    }

    // ConcurrentHashMap-only methods

    /**
     * Returns the number of mappings. This method should be used
     * instead of {@link #size} because a ConcurrentHashMap may
     * contain more mappings than can be represented as an int. The
     * value returned is an estimate; the actual count may differ if
     * there are concurrent insertions or removals.
     *
     * @return the number of mappings
     * @since 1.8
     */
    public long mappingCount() {
        long n = sumCount();
        return (n < 0L) ? 0L : n; // ignore transient negative values
    }

    /**
     * Creates a new {@link Set} backed by a ConcurrentHashMap
     * from the given type to {@code Boolean.TRUE}.
     *
     * @param <K> the element type of the returned set
     * @return the new set
     * @since 1.8
     */
    public static <K> KeySetView<K,Boolean> newKeySet() {
        return new KeySetView<K,Boolean>
            (new ConcurrentHashMap<K,Boolean>(), Boolean.TRUE);
    }

    /**
     * Creates a new {@link Set} backed by a ConcurrentHashMap
     * from the given type to {@code Boolean.TRUE}.
     *
     * @param initialCapacity The implementation performs internal
     * sizing to accommodate this many elements.
     * @param <K> the element type of the returned set
     * @return the new set
     * @throws IllegalArgumentException if the initial capacity of
     * elements is negative
     * @since 1.8
     */
    public static <K> KeySetView<K,Boolean> newKeySet(int initialCapacity) {
        return new KeySetView<K,Boolean>
            (new ConcurrentHashMap<K,Boolean>(initialCapacity), Boolean.TRUE);
    }

    /**
     * Returns a {@link Set} view of the keys in this map, using the
     * given common mapped value for any additions (i.e., {@link
     * Collection#add} and {@link Collection#addAll(Collection)}).
     * This is of course only appropriate if it is acceptable to use
     * the same value for all additions from this view.
     *
     * @param mappedValue the mapped value to use for any additions
     * @return the set view
     * @throws NullPointerException if the mappedValue is null
     */
    public KeySetView<K,V> keySet(V mappedValue) {
        if (mappedValue == null)
            throw new NullPointerException();
        return new KeySetView<K,V>(this, mappedValue);
    }

    /* ---------------- Special Nodes -------------- */

    /**
     * A node inserted at head of bins during transfer operations.
     */
    // 在传输操作期间插入到bin头部的节点。
    static final class ForwardingNode<K,V> extends Node<K,V> {

        final Node<K,V>[] nextTable;

        // 创建转发结点, hash为MOVED(-1), 没有键和值, nextTab指向新散列表
        ForwardingNode(Node<K,V>[] tab) {
            super(MOVED, null, null, null);
            this.nextTable = tab;
        }

        // 以转发结点方式根据hash以及key对象查找结点: hash相等, 且Key相等或者equals
        Node<K,V> find(int h, Object k) {
            // loop to avoid arbitrarily deep recursion on forwarding nodes 循环以避免在转发节点上任意深度递归
            outer: for (Node<K,V>[] tab = nextTable;;) {
                Node<K,V> e; int n;

                // 键k, 键hash值h, 新散列表tab, tab容量n, h取模n对应的结点e, 如果任意为null, 则返回null, 代表以转发结点方式没找到对应的桶头结点
                if (k == null || tab == null || (n = tab.length) == 0 || (e = tabAt(tab, (n - 1) & h)) == null)
                    return null;

                // 如果找到对应的桶头结点, 则开始自旋, e的hash值eh, e的键ek
                for (;;) {
                    int eh; K ek;

                    // 如果e的hash值相等, e的Key相等或者e的Key equals, 说明在新tab中的e链表上找到了对应结点, 此时返回e结点即可
                    if ((eh = e.hash) == h && ((ek = e.key) == k || (ek != null && k.equals(ek))))
                        return e;

                    // 如果e的hash值还小于0, 说明e可能为转发结点、红黑树结点、computeIfAbsent临时结点
                    if (eh < 0) {
                        // 如果e仍为转发结点, 说明新表tab又被扩容转移了, 此时更新tab指针, 继续自旋
                        if (e instanceof ForwardingNode) {
                            tab = ((ForwardingNode<K,V>)e).nextTable;
                            continue outer;
                        }
                        // 如果e为红黑树结点, 则按根据hash值和key值，从根结点开始查找红黑树结点; 如果e为computeIfAbsent临时结点, 则返回null
                        else
                            return e.find(h, k);
                    }

                    // 桶头没找到e结点, 则继续遍历e链表, 如果找到链尾还没找到对应的结点, 则返回null
                    if ((e = e.next) == null)
                        return null;
                }
            }
        }
    }

    /**
     * A place-holder node used in computeIfAbsent and compute
     */
    // 在 computeIfAbsent 和计算中使用的占位符节点
    static final class ReservationNode<K,V> extends Node<K,V> {
        ReservationNode() {
            super(RESERVED, null, null, null);
        }

        Node<K,V> find(int h, Object k) {
            return null;
        }
    }

    /* ---------------- Table Initialization and Resizing -------------- */
    // 表初始化和调整大小

    /**
     * 20210620
     * 返回用于调整大小为 n 的表的标记位。 向左移动 RESIZE_STAMP_SHIFT 时必须为负。
     */
    /**
     * Returns the stamp bits for resizing a table of size n.
     * Must be negative when shifted left by RESIZE_STAMP_SHIFT.
     */
    // 获取容量n的扩容标记位, 用于更新并发阈值为: 高16为扩容标记, 第16位为并发扩容线程数(从2开始, 步长+1), 结果肯定为负数
    static final int resizeStamp(int n) {
        // n的二进制补码最高位前的为零的位数 | 1 << 15
        // eg: n = 10: 0000 0000 0000 0000 0000 0000 0000 1010
        // => 0000 0000 0000 0000, 0000 0000 0001 1100 | 0000 0000 0000 0000, 1000 0000 0000 0000
        // => 0000 0000 0000 0000, 1000 0000 0001 1100
        return Integer.numberOfLeadingZeros(n) | (1 << (RESIZE_STAMP_BITS - 1));
    }

    /**
     * Initializes table, using the size recorded in sizeCtl.
     */
    // 使用并发阈值初始化散列表
    private final Node<K,V>[] initTable() {

        // 开始自旋, 散列表tab, 当前时刻阈值sc, 如果该轮自旋检测到tab已经有容量了, 说明tab被其他线程初始化了, 则当前线程结束自旋, 返回tab
        Node<K,V>[] tab; int sc;
        while ((tab = table) == null || tab.length == 0) {

            // 如果sc小于0, 说明tab正在被其他线程初始化, 则当前线程让步时间片给CPU, 自己进入阻塞状态, 等待下一轮自旋
            if ((sc = sizeCtl) < 0)
                Thread.yield(); // lost initialization race; just spin // 失去初始化竞赛； 只是旋转

            // 如果sc不小于0, 说明tab没在初始化, 则CAS设置sizeCtl为-1(表示tab正在初始化)
            else if (U.compareAndSwapInt(this, SIZECTL, sc, -1)) {
                try {
                    // CAS成功, 再次检查tab是否还没被创建, 如果确实没创建, 则创建sc长或者默认容量16的散列表, 且更新并发阈值为0.75 * n, 结束自旋返回tab
                    if ((tab = table) == null || tab.length == 0) {
                        int n = (sc > 0) ? sc : DEFAULT_CAPACITY;
                        @SuppressWarnings("unchecked")
                        Node<K,V>[] nt = (Node<K,V>[])new Node<?,?>[n];
                        table = tab = nt;
                        sc = n - (n >>> 2);
                    }
                } finally {
                    sizeCtl = sc;
                }
                break;
            }
        }

        return tab;
    }

    /**
     * 20210620
     * 添加计数，如果表太小且尚未调整大小，则启动传输。 如果已经调整大小，则在工作可用时帮助执行转移。
     * 传输后重新检查占用情况，以查看是否已经需要再次调整大小，因为调整大小是滞后添加的。
     */
    /**
     * Adds to count, and if table is too small and not already
     * resizing, initiates transfer. If already resizing, helps
     * perform transfer if work is available.  Rechecks occupancy
     * after a transfer to see if another resize is already needed
     * because resizings are lagging additions.
     *
     * @param x the count to add
     *
     * // 如果 <0，不检查调整大小，如果 <= 1 只检查是否无竞争
     * @param check if <0, don't check resize, if <= 1 only check if uncontended
     */
    // baseCount或者CounterCell叠加x, 叠加后检查是否需要扩容, 如果需要则启动扩容并转移结点; 如果判断到其他线程正在扩容转移结点, 则当前线程进行协助扩容转移结点
    private final void addCount(long x, int check) {
        // 要添加的数量x, 分布计数填充单元格数组as, ConcurrentHashMap实例字段上的元素个数b, 叠加b后的元素个数s
        CounterCell[] as; long b, s;

        // 如果CAS更新baseCount为s失败, 说明当前线程竞争b失败, 此时需要竞争as单元格a, 从而叠加x
        if ((as = counterCells) != null || !U.compareAndSwapLong(this, BASECOUNT, b = baseCount, s = b + x)) {
            CounterCell a; long v; int m;
            boolean uncontended = true;

            // 如果as还没被初始化, 或者对as掩码m取模后获得的a单元格没被初始化, 或者a有被初始化但CAS叠加x值到a单元格失败, 则在as上并发叠加x
            if (as == null || (m = as.length - 1) < 0 || (a = as[ThreadLocalRandom.getProbe() & m]) == null || !(uncontended = U.compareAndSwapLong(a, CELLVALUE, v = a.value, v + x))) {
                // 在baseCount或者counterCells上叠加或者存放x: 单元格为空 -> 竞争初始化as -> 竞争初始化as失败则叠加到baseCount -> || 单元格不为空 -> 竞争叠加到单元格a -> 竞争叠加a失败则重新哈希再次确认叠加, 还失败则扩容as
                fullAddCount(x, uncontended);
                return;
            }

            // 如果CAS叠加x到a单元格成功, 且只需要检查有无竞争, 则直接可以返回无需扩容了, 这种情况发生在删除、替换结点中, 因为操作后肯定不需要扩容
            if (check <= 1)
                return;

            // 如果需要扩容, 则s等于叠加ConcurrentHashMap实例字段上的元素个数baseCount, 与计数填充单元格数组CounterCell[]的所有数值
            s = sumCount();
        }

        // 到这里, 说明CAS更新baseCount为s成功, 或者竞争as成功了, 即x已经被成功叠加了, 此时需要进行扩容判断和扩容
        if (check >= 0) {
            // 旧散列表tab, 新散列表nt, tab容量n, 并发阈值sc
            Node<K,V>[] tab, nt; int n, sc;

            // 如果叠加后的s大于sc, 且旧散列表容量n还没到最大容量, 说明需要扩容
            while (s >= (long)(sc = sizeCtl) && (tab = table) != null && (n = tab.length) < MAXIMUM_CAPACITY) {
                // 获取容量n的扩容标记位rs, 用于更新并发阈值为: 高16为扩容标记, 第16位为并发扩容线程数(从2开始, 步长+1), 结果肯定为负数
                int rs = resizeStamp(n);

                // 如果并发阈值sc小于0, 说明散列表正在被其他线程扩容, 则当前线程加入一起转移结点
                if (sc < 0) {
                    // 如果sc高16位扩容标记已经改变(说明不是本次扩容了), 或者并发阈值为非法状态(并发扩容线程数从+2开始), 或者并发线程数超过了最大值2^16-1, 或者nextTable不存在, 或者transferIndex到头了, 代表非法条件或者终止条件, 则退出自旋
                    if ((sc >>> RESIZE_STAMP_SHIFT) != rs || sc == rs + 1 || sc == rs + MAX_RESIZERS || (nt = nextTable) == null || transferIndex <= 0)
                        break;

                    // 如果sc合法, 且当前处于非终止条件, 则CAS更新sc+1, 也就是低16位并发扩容线程数+1(从2开始, 步长+1)
                    if (U.compareAndSwapInt(this, SIZECTL, sc, sc + 1))
                        // 如果CAS更新成功, 说明当前线程争抢到协助转移结点的机会, 则转移旧散列表tab中的结点到新散列表nextTab中
                        transfer(tab, nt);
                    // 如果CAS更新失败, 说明同一时刻协助转移结点的机会被其他线程抢占了, 则当前线程继续自旋, 争抢协助机会
                }
                // 如果散列表没有在被其他线程扩容, 说明当前线程为扩容的第一个线程, 则CAS更新并发阈值为1000 0000 0001 1100, 0000, 0000, 0000, 0010(eg: n=16)
                else if (U.compareAndSwapInt(this, SIZECTL, sc, (rs << RESIZE_STAMP_SHIFT) + 2))
                    // 如果CAS更新成功, 说明当前线程争抢到扩容转移结点的机会, 则转移旧散列表tab中的结点到新散列表nextTab中, 如果nextTab还没创建则先扩容(创建nextTab)
                    transfer(tab, null);
                // 如果CAS更新失败, 说明同一时刻扩容转移结点的机会被其他线程抢占了, 则当前线程继续自旋, 争抢扩容转移机会

                // 每轮自旋都重新计算s, s等于叠加ConcurrentHashMap实例字段上的元素个数baseCount, 与计数填充单元格数组CounterCell[]的所有数值, 准备下一次自旋, 看当前nt是否还需要扩容
                s = sumCount();
            }
        }
    }

    /**
     * 20210622
     * 如果正在调整大小，则有助于传输。
     */
    /**
     * Helps transfer if a resize is in progress.
     */
    // 如果旧tab正在被其他线程扩容转移中, 则当前线程加入一起转移结点, 协助完成后返回新tab
    final Node<K,V>[] helpTransfer(Node<K,V>[] tab, Node<K,V> f) {
        // 旧散列表tab, i桶头结点f, 新散列表nextTab, 并发阈值sc
        Node<K,V>[] nextTab; int sc;

        // 如果在tab碰到ForwardingNode结点, 且该结点指向的新散列表不为null, 说明确实有线程正在扩容转移tab上的结点(理论上都是成立), 则当前线程也加入结点转移工作中
        if (tab != null && (f instanceof ForwardingNode) && (nextTab = ((ForwardingNode<K,V>)f).nextTable) != null) {
            // 获取容量n的扩容标记位, 用于更新并发阈值为: 高16为扩容标记, 第16位为并发扩容线程数(从2开始, 步长+1), 结果肯定为负数
            int rs = resizeStamp(tab.length);

            // 如果nextTable指针没变, 且table指针没变, 并发阈值sc小于0, 说明tab正在被其他线程扩容转移中, 则当前线程加入一起转移结点
            while (nextTab == nextTable && table == tab && (sc = sizeCtl) < 0) {
                // 如果sc高16位扩容标记已经改变(说明不是本次扩容了), 或者并发阈值为非法状态(并发扩容线程数从+2开始), 或者并发线程数超过了最大值2^16-1, 或者transferIndex到头了, 代表非法条件或者终止条件, 则退出自旋
                if ((sc >>> RESIZE_STAMP_SHIFT) != rs || sc == rs + 1 || sc == rs + MAX_RESIZERS || transferIndex <= 0)
                    break;

                // 如果sc合法, 且当前处于非终止条件, 则CAS更新sc+1, 也就是低16位并发扩容线程数+1(从2开始, 步长+1)
                if (U.compareAndSwapInt(this, SIZECTL, sc, sc + 1)) {
                    // 如果CAS更新成功, 说明当前线程争抢到协助转移结点的机会, 则转移旧散列表tab中的结点到新散列表nextTab中
                    transfer(tab, nextTab);

                    // 当前线程转移完毕, 则结束自旋
                    break;
                }
                // 如果CAS更新失败, 说明同一时刻协助转移结点的机会被其他线程抢占了, 则当前线程继续自旋, 争抢协助机会
            }

            // 自旋结束, 返回新散列表nextTab
            return nextTab;
        }

        // 一般来说永远不会走到这里
        return table;
    }

    /**
     * 20210623
     * 尝试预先调整表格以容纳给定数量的元素。
     */
    /**
     * Tries to presize table to accommodate the given number of elements.
     *
     * @param size number of elements (doesn't need to be perfectly accurate)
     */
    // 初始化或者扩容当前散列表tab容量, 到最接近指定容量的2次幂次容量, 可能会初始化tab、无需扩容tab、协助转移tab结点、扩容tab并转移结点
    private final void tryPresize(int size) {
        // 获取最接近指定容量的2次幂次容量c
        int c = (size >= (MAXIMUM_CAPACITY >>> 1)) ? MAXIMUM_CAPACITY : tableSizeFor(size + (size >>> 1) + 1);

        // 开始自旋, 如果并发阈值sc大于等于0, 说明散列表没有在做扩容或者初始化, 当前散列表tab, tab容量n
        int sc;
        while ((sc = sizeCtl) >= 0) {
            Node<K,V>[] tab = table; int n;

            // 如果散列表还没初始化, 则判断并发阈值sc与新容量c, 取两者的大者来创建散列表tab的长度
            if (tab == null || (n = tab.length) == 0) {
                n = (sc > c) ? sc : c;

                // 如果CAS成功更新sc为-1, 说明当前线程争抢到创建散列表tab的机会
                if (U.compareAndSwapInt(this, SIZECTL, sc, -1)) {
                    try {
                        // CAS成功则再次检查, 如果table指针没有被改变, 则创建n长的散列表为table, 且更新并发阈值为0.75 * n
                        if (table == tab) {
                            @SuppressWarnings("unchecked")
                            Node<K,V>[] nt = (Node<K,V>[])new Node<?,?>[n];
                            table = nt;
                            sc = n - (n >>> 2);
                        }
                    } finally {
                        sizeCtl = sc;
                    }
                }
            }

            // 如果散列表已存在, 且c小于等于sc, 说明并不需要扩容这么快, 或者容量n已经超出了最大容量, 则直接返回即可, 无需扩容
            else if (c <= sc || n >= MAXIMUM_CAPACITY)
                break;

            // 如果散列表已存在, 且c大于sc, 且n还没达到最大容量,
            else if (tab == table) {
                // 获取容量n的扩容标记位, 用于更新并发阈值为: 高16为扩容标记, 第16位为并发扩容线程数(从2开始, 步长+1), 结果肯定为负数
                int rs = resizeStamp(n);

                // 如果并发阈值sc小于0, 说明散列表正在被其他线程扩容, 则当前线程加入一起转移结点
                if (sc < 0) {
                    Node<K,V>[] nt;

                    // 如果sc高16位扩容标记已经改变(说明不是本次扩容了), 或者并发阈值为非法状态(并发扩容线程数从+2开始), 或者并发线程数超过了最大值2^16-1, 或者nextTable不存在, 或者transferIndex到头了, 代表非法条件或者终止条件, 则退出自旋
                    if ((sc >>> RESIZE_STAMP_SHIFT) != rs || sc == rs + 1 || sc == rs + MAX_RESIZERS || (nt = nextTable) == null || transferIndex <= 0)
                        break;

                    // 如果sc合法, 且当前处于非终止条件, 则CAS更新sc+1, 也就是低16位并发扩容线程数+1(从2开始, 步长+1)
                    if (U.compareAndSwapInt(this, SIZECTL, sc, sc + 1))
                        // 如果CAS更新成功, 说明当前线程争抢到协助转移结点的机会, 则转移旧散列表tab中的结点到新散列表nextTab中
                        transfer(tab, nt);
                    // 如果CAS更新失败, 说明同一时刻协助转移结点的机会被其他线程抢占了, 则当前线程继续自旋, 争抢协助机会
                }

                // 如果散列表没有在被其他线程扩容, 说明当前线程为扩容的第一个线程, 则CAS更新并发阈值为1000 0000 0001 1100, 0000, 0000, 0000, 0010(eg: n=16)
                else if (U.compareAndSwapInt(this, SIZECTL, sc, (rs << RESIZE_STAMP_SHIFT) + 2))
                    // 如果CAS更新成功, 说明当前线程争抢到扩容转移结点的机会, 则转移旧散列表tab中的结点到新散列表nextTab中, 如果nextTab还没创建则先扩容(创建nextTab)
                    transfer(tab, null);
                // 如果CAS更新失败, 说明同一时刻扩容转移结点的机会被其他线程抢占了, 则当前线程继续自旋, 争抢扩容转移机会
            }
        }
    }

    /**
     * 20210620
     * 将每个bin中的节点移动和/或复制到新表。见上文解释。
     */
    /**
     * Moves and/or copies the nodes in each bin to new table. See
     * above for explanation.
     */
    // 转移旧散列表tab中的结点到新散列表nextTab中, 如果nextTab还没创建则先扩容(创建nextTab)
    private final void transfer(Node<K,V>[] tab, Node<K,V>[] nextTab) {
        // 旧散列表tab, 新散列表nextTab, tab容量n, 步长stride
        int n = tab.length, stride;

        // 步长计算公式(多核) = n/8 / cores, 步长计算公式(单核) = 1, 如果步长小于16, 则默认为16
        if ((stride = (NCPU > 1) ? (n >>> 3) / NCPU : n) < MIN_TRANSFER_STRIDE)
            stride = MIN_TRANSFER_STRIDE; // subdivide range 细分范围

        // 如果nextTab还没被创建, 说明当前线程为扩容发起的线程(由于经过了CAS才进入的, 所以nextTab为null的只有一个线程), 则创建2倍容量的散列表数组, 更新为nextTable开始扩容, 且更新转移索引为n(即从n开始转移)
        if (nextTab == null) {            // initiating 发起
            try {
                @SuppressWarnings("unchecked")
                Node<K,V>[] nt = (Node<K,V>[])new Node<?,?>[n << 1];
                nextTab = nt;
            } catch (Throwable ex) {      // try to cope with OOME 尝试应对OOM Error
                sizeCtl = Integer.MAX_VALUE;
                return;
            }
            nextTable = nextTab;
            transferIndex = n;
        }

        // 到这里nextTab已经被创建, nextTab容量nextn, 创建转发结点fwd(hash为MOVED(-1), 没有键和值, nextTab指向新散列表, 下面转移完结点都会复用这个转发结点) => put时会判断, 判断到则会协助转移结点
        int nextn = nextTab.length;
        ForwardingNode<K,V> fwd = new ForwardingNode<K,V>(nextTab);

        // 当前线程是否还需要前进划分转移区间advance, 当成线程转移工作是否已经完成finishing
        boolean advance = true;
        boolean finishing = false; // to ensure sweep before committing nextTab 在提交 nextTab 之前确保扫描

        // 当前线程开始转移结点, 当前索引i, 左边界bound, i桶的头结点f, f的hash值fh
        for (int i = 0, bound = 0;;) {
            Node<K,V> f; int fh;

            // 如果当前线程还需要前进划分转移区间, 则继续前进, 当前转移索引nextIndex, 下一个转移边界nextBound
            while (advance) {
                int nextIndex, nextBound;

                // 索引i前移1, 如果超过了左边界bound, 或者转移已经完成了, 则更新advance为false, 代表不需要前进划分转移区间了
                if (--i >= bound || finishing)
                    advance = false;
                // 如果i还没超出左边界bound, 且nextIndex小于等于0了, 说明所有转移区间都被其他线程划分完毕了, 则更新advance为false, 代表不需要前进划分转移区间了
                else if ((nextIndex = transferIndex) <= 0) {
                    i = -1;
                    advance = false;
                }
                // 如果i还没超出左边界bound, 且nextIndex也还没小于等于0, 说明当前线程存在转移区间, 则transferIndex前移一个步长, 并使用CAS更新, 新的左边界nextBound
                else if (U.compareAndSwapInt(this, TRANSFERINDEX, nextIndex, nextBound = (nextIndex > stride ? nextIndex - stride : 0))) {
                    // 如果CAS成功, 说明当前线程抢到转移区间, 则更新左边界bound为nextBound, 更新i为nextIndex-1, 更新advance为false, 代表当前转移区间已经确定(为bound~i), 不需要前进划分转移区间了
                    bound = nextBound;
                    i = nextIndex - 1;
                    advance = false;
                }

                // 如果CAS失败, 说明当前线程没抢到转移区间, 则需要重新advance自旋: 前移i、判断transferIndex是否合法、CAS更新transferIndex...
            }

            // 到这里, 说明当前线程的转移区间(bound~i)已经确定好了
            // 如果i小于0, 或者i大于tab容量n, 或者i+n新的索引大于nextn, 说明i转移起来不合法了(i"越界"了), 也就是当前转移工作已经完成了
            if (i < 0 || i >= n || i + n >= nextn) {
                int sc;

                // 如果当前线程转移工作已经完成了, 且通过了最后一次的校验(遍历完旧表tab), 则发起最后的扩容提交, 正式更新table为新散列表nextTab, 清空nextTable, 更新并发阈值sizeCtl为(0.75 * 新表容量), 结束扩容转移操作并返回
                if (finishing) {
                    nextTable = null;
                    table = nextTab;
                    sizeCtl = (n << 1) - (n >>> 1);
                    return;
                }

                // 如果当前线程转移工作已经完成了, 则CAS更新并发阈值sizeCtl-1, 代表扩容/转移并发线程数-1, 如果CAS成功, 说明当前线程抢到了提交工作的名额
                if (U.compareAndSwapInt(this, SIZECTL, sc = sizeCtl, sc - 1)) {
                    // 如果容量n的扩容标记位rs不一致, 说明sc低位还有别的线程在工作, 此时直接返回即可, 把nextTab的赋值操作留给最后一个提交线程来实现
                    if ((sc - 2) != resizeStamp(n) << RESIZE_STAMP_SHIFT)
                        return;

                    // 如果容量n的扩容标记位rs一致, 说明当前线程为最后一个提交工作的线程, 此时置finishing=advance为true, 进行提交前最后的校验
                    finishing = advance = true;
                    i = n; // recheck before commit 提交前重新检查
                }

                // 如果CAS失败, 说明当前线程没抢到了提交工作的名额, 需要自旋等待下一轮名额的竞争
            }

            // i没有"越界", 当前线程适合转移结点, 则volatile方式获取散列表tab中i位置的Node节点, 如果为null, 则CAS标记为ForwardingNode结点, 代表已经转移过了(其他线程看到会跳过的)
            else if ((f = tabAt(tab, i)) == null)
                // 如果CAS标记成功, 说明当前线程成功转移了i结点, 则置advance为true, 当前线程可以继续前进划分区间(为了保持stride不长的区间); 如果CAS失败, 则保持i位置不动进行下一轮自旋(一定是进入MOVED的判断分支)
                advance = casTabAt(tab, i, null, fwd);

            // 如果当前线程转移i结点碰到ForwardingNode结点, 说明该结点已经被其他线程转移了, 则置advance为true, 当前线程可以继续前进划分区间(为了保持stride不长的区间)
            else if ((fh = f.hash) == MOVED)
                advance = true; // already processed 已经处理

            // 如果当前线程转移i结点碰到非ForwardingNode结点, 说明该结点是个有效的业务结点, 则对结点加锁老老实实地转移, 如果获取不到f锁, 则阻塞等待其他线程释放f锁, 保证再判断一次f结点是否转移完毕
            else {
                synchronized (f) {
                    // 拿到f锁后, 判断如果f结点确实没有改变, 说明f结点适合转移; 如果不是即说明f结点被改变了(也就是被f转移了), 则释放f锁, 保持i位置不动进行下一轮自旋(一定是进入MOVED的判断分支)
                    if (tabAt(tab, i) == f) {
                        // 新散列表nextTab[0, n-1]低区间链表ln, nextTab[n-1, 2n-1]高区间链表hn
                        Node<K,V> ln, hn;

                        // 如果f的hash值fh(在上面赋值了)大于等于0, 说明f为普通结点, 则以普通链表方式转移结点
                        if (fh >= 0) {
                            // 旧散列表tab容量n, 通过fh & n计算f结点位标记runBit, lastRun对应该位标记的结点
                            int runBit = fh & n;
                            Node<K,V> lastRun = f;

                            // f后继p, 遍历f链表, 位标记runBit取最后结点的位标记(如果不同的话), lastRun对应该位标记的结点
                            for (Node<K,V> p = f.next; p != null; p = p.next) {
                                int b = p.hash & n;
                                if (b != runBit) {
                                    runBit = b;
                                    lastRun = p;
                                }
                            }

                            // 如果最后位标记runBit为0, 说明lastRun结点的hash值"不变", 即lastRun结点转移后需要在新散列表nextTab[0, n-1]低区间链表ln内
                            if (runBit == 0) {
                                ln = lastRun;
                                hn = null;
                            }
                            // 如果最后位标记runBit不为0, 说明lastRun结点的hash值"变了", 即lastRun结点转移后需要在新散列表nextTab[n-1, 2n-1]高区间链表hn
                            else {
                                hn = lastRun;
                                ln = null;
                            }

                            // 再次遍历f链表, 切割f~lastRun之间的结点为ln链表, lastRun到剩余结点为hn链表
                            for (Node<K,V> p = f; p != lastRun; p = p.next) {
                                int ph = p.hash; K pk = p.key; V pv = p.val;
                                if ((ph & n) == 0)
                                    ln = new Node<K,V>(ph, pk, pv, ln);
                                else
                                    hn = new Node<K,V>(ph, pk, pv, hn);
                            }

                            // 划分ln和hn链表后, volatile方式添加ln链表到新散列表nextTab的i桶中
                            setTabAt(nextTab, i, ln);

                            // volatile方式添加hn链表到新散列表nextTab的i+n桶中
                            setTabAt(nextTab, i + n, hn);

                            // volatile方式添加ForwardingNode结点到旧散列表tab中i位置, 代表i桶结点已经转移完毕
                            setTabAt(tab, i, fwd);

                            // 置advance为true, 当前线程可以继续前进划分区间(为了保持stride不长的区间)
                            advance = true;
                        }

                        // 如果fh小于0, 且f为TreeBin类型, 说明f链为红黑树, f.root为红黑树的根结点, 则以"红黑树方式"转移结点
                        else if (f instanceof TreeBin) {
                            // f结点备份指针t, 低区间链表lo, 高区间链表hi, lo尾指针loTail, hi尾指针hiTail, lo链表容量lc, hi链表容量hc
                            TreeBin<K,V> t = (TreeBin<K,V>)f;
                            TreeNode<K,V> lo = null, loTail = null;
                            TreeNode<K,V> hi = null, hiTail = null;
                            int lc = 0, hc = 0;

                            // 以链表方式遍历t链表, 当前遍历结点e, e的hash值h, 根据e重新构造的结点p
                            for (Node<K,V> e = t.first; e != null; e = e.next) {
                                int h = e.hash;
                                TreeNode<K,V> p = new TreeNode<K,V>(h, e.key, e.val, null, null);

                                // 通过h & n计算结点位标记, 如果为0, 说明结点的hash值"不变", 即结点转移后需要在新散列表nextTab[0, n-1]低区间链表lo内, 则维护lo链表结点的前驱和后继
                                if ((h & n) == 0) {
                                    if ((p.prev = loTail) == null)
                                        lo = p;
                                    else
                                        loTail.next = p;
                                    loTail = p;
                                    ++lc;
                                }
                                // 通过h & n计算结点位标记, 如果不为0, 说明结点的hash值"变了", 即结点转移后需要在新散列表nextTab[n-1, 2n-1]高区间链表hi内, 则维护hi链表结点的前驱和后继
                                else {
                                    if ((p.prev = hiTail) == null)
                                        hi = p;
                                    else
                                        hiTail.next = p;
                                    hiTail = p;
                                    ++hc;
                                }
                            }

                            // lo链表维护完毕后, 如果lc小于6, 则普通化lo链表; 否则lc大于等于6, 如果hi链表存在, 说明有必要红黑树化lo链表, 如果hi链表不存在, 说明红黑树保持不变为t即可
                            ln = (lc <= UNTREEIFY_THRESHOLD) ? untreeify(lo) : (hc != 0) ? new TreeBin<K,V>(lo) : t;

                            // hi链表维护完毕后, 如果hc小于6, 则普通化hi链表; 否则hc大于等于6, 如果lo链表存在, 说明有必要红黑树化hi链表, 如果lo链表不存在, 说明红黑树保持不变为t即可
                            hn = (hc <= UNTREEIFY_THRESHOLD) ? untreeify(hi) : (lc != 0) ? new TreeBin<K,V>(hi) : t;

                            // 红黑树化/普通化完毕后, volatile方式添加ln链表到新散列表nextTab的i桶中
                            setTabAt(nextTab, i, ln);

                            // volatile方式添加hn链表到新散列表nextTab的i+n桶中
                            setTabAt(nextTab, i + n, hn);

                            // volatile方式添加ForwardingNode结点到旧散列表tab中i位置, 代表i桶结点已经转移完毕
                            setTabAt(tab, i, fwd);

                            // 置advance为true, 当前线程可以继续前进划分区间(为了保持stride不长的区间)
                            advance = true;
                        }
                    }
                }
            }
        }
    }

    /* ---------------- Counter support -------------- */

    /**
     * A padded cell for distributing counts.  Adapted from LongAdder
     * and Striped64.  See their internal docs for explanation.
     */
    // 用于分布计数的填充单元格。 改编自 LongAdder 和 Striped64。 有关解释，请参阅他们的内部文档。
    @sun.misc.Contended
    static final class CounterCell {
        volatile long value;
        CounterCell(long x) { value = x; }
    }

    // 分布计数核心逻辑: 叠加ConcurrentHashMap实例字段上的元素个数baseCount, 与计数填充单元格数组CounterCell[]的所有数值
    final long sumCount() {
        CounterCell[] as = counterCells; CounterCell a;
        long sum = baseCount;
        if (as != null) {
            for (int i = 0; i < as.length; ++i) {
                if ((a = as[i]) != null)
                    sum += a.value;
            }
        }
        return sum;
    }

    // See LongAdder version for explanation 有关解释，请参阅 LongAdder 版本
    // 在baseCount或者counterCells上叠加或者存放x: 单元格为空 -> 竞争初始化as -> 竞争初始化as失败则叠加到baseCount -> || 单元格不为空 -> 竞争叠加到单元格a -> 竞争叠加a失败则重新哈希再次确认叠加, 还失败则扩容as
    private final void fullAddCount(long x, boolean wasUncontended) {

        // 获取当前线程哈希值h(同一个线程的h相同)
        int h;
        if ((h = ThreadLocalRandom.getProbe()) == 0) {
            ThreadLocalRandom.localInit();      // force initialization 强制初始化
            h = ThreadLocalRandom.getProbe();
            wasUncontended = true;
        }

        // 开始自旋, 是否已冲突collide(fasle代表已冲突), 分布计算单元格数组as, 当前计算单元格a, as长度n, 单元格a的值或者baseCount的值v
        boolean collide = false;                // True if last slot nonempty 如果最后一个插槽非空，则为真
        for (;;) {
            CounterCell[] as; CounterCell a; int n; long v;

            // 如果分布计算单元格数组as已经创建, 且长度n大于0, 说明as有效, 则需要争抢counterCells数组来叠加x
            if ((as = counterCells) != null && (n = as.length) > 0) {

                // 如果h取模n-1得到的单元格a为null, 说明当前位置可以直接存放x, 如果这里h变化了, a可能会变化, 相当于随机得到新的索引位置去判断
                if ((a = as[(n - 1) & h]) == null) {

                    // 如果cellsBusy标记为0, 说明as数组空闲, 可以操作as, 则构造值为x的CounterCell单元格r
                    if (cellsBusy == 0) {            // Try to attach new Cell 尝试附加新单元格
                        CounterCell r = new CounterCell(x); // Optimistic create 乐观创造

                        // 再次检查as数组是否空闲, 如果空闲则CAS更新cellsBusy标记位为1
                        if (cellsBusy == 0 && U.compareAndSwapInt(this, CELLSBUSY, 0, 1)) {

                            // 如果CAS成功, 说明当前线程竞争到操作as的机会, counterCells数组rs, rs长度m, h取模m-1索引j, 单元格a是否创建成功created
                            boolean created = false;
                            try {               // Recheck under lock 在锁定状态下重新检查
                                // 再次检查rs不为空, 长度m大于0, 且j位置单元格为空, 说明a单元格没有被其他线程抢占, 则当前线程直接把新建的单元格r添加到数组as中
                                CounterCell[] rs; int m, j;
                                if ((rs = counterCells) != null && (m = rs.length) > 0 && rs[j = (m - 1) & h] == null) {
                                    rs[j] = r;
                                    created = true;
                                }
                            } finally {
                                // as操作完成后, 则更新cellsBusy为0, 代表counterCells数组已经空闲了
                                cellsBusy = 0;
                            }

                            // 如果a单元格被当前线程成功创建, 则结束自旋并返回, 代表x已经存放成功了
                            if (created)
                                break;

                            // 如果a单元格没有被当前线程创建, 则当前线程还需要继续自旋
                            continue;           // Slot is now non-empty 插槽现在非空
                        }
                    }

                    // 如果cellsBusy标记为1, 说明as数组在忙, 不可以操作as, 则标记a单元格已冲突, 获取新的h和as再自旋去竞争
                    collide = false;
                }

                // 如果h取模n-1得到的单元格a不为null, 说明a单元格需要竞争, 如果wasUncontended为false, 说明当前线程调用方法前已经CAS竞争a单元格失败过了, 则标记wasUncontended为true, 获取新的h和as再自旋去竞争(应该是为了缓一缓, 减少竞争)
                else if (!wasUncontended)       // CAS already known to fail CAS 已知失败
                    wasUncontended = true;      // Continue after rehash 重刷后继续

                // 如果h取模n-1得到的单元格a不为null, 说明a单元格需要竞争, 如果当前线程已经置true了wasUncontended, 说明已经至少重刷了一遍, 则CAS叠加x到单元格a中
                else if (U.compareAndSwapLong(a, CELLVALUE, v = a.value, v + x))
                    // 如果CAS成功, 说明当前线程争抢叠加x到了a单元格, 此时可以结束自旋并返回了
                    break;

                // CAS叠加x到a单元格失败, 如果as有变化或者长度n超出CPU核心数, 则更新collide为已冲突, 获取新的h和as再自旋去竞争
                else if (counterCells != as || n >= NCPU)
                    // 对于长度n超出CPU核心数, 说明as已经达到最长了, collide只能一直为false, 一直获取新的h再自旋去竞争, 此时是永远不会走collide = true分支的
                    // 而对于as有变化的情况, 说明当前as已经落后了, 不能再叠加x了, 必须重新自旋获取新的as来叠加, 获取新的as后如果还冲突, 是会走collide = true分支的
                    collide = false;            // At max size or stale 最大尺寸或过时

                // 如果collide为已冲突且长度还没达到最大, 则更新collide为true, 代表即将无冲突了, 扩容前先让当前线程再去获取h, 确认一下as是否有空格子, 或者是否能够竞争格子成功, 或者as是否有发生变化
                else if (!collide)
                    collide = true;

                // 当前线程最后一次已经确认完毕, 说明当前as大概率没有空格子(碰到两次格子都不为空), 且as大概率冲突很严重(竞争格子失败至少2次 或者 数组繁忙时至少竞争3次 或者as变化时至少竞争3次)
                // 如果cellsBusy为0, 说明as数组空闲, 可以被操作, 则CAS更新cellsBusy为1, 代表as在忙
                else if (cellsBusy == 0 && U.compareAndSwapInt(this, CELLSBUSY, 0, 1)) {
                    try {
                        // 再次检查as是否为counterCells, 如果as不变, 则对as数组扩容2倍, 并转移旧的as数据到新的数组, 转移完成后更新as为新的数组, 完成as扩容
                        if (counterCells == as) {// Expand table unless stale 展开表格，除非过时
                            CounterCell[] rs = new CounterCell[n << 1];
                            for (int i = 0; i < n; ++i)
                                rs[i] = as[i];
                            counterCells = rs;
                        }
                    } finally {
                        // as操作完毕后, 更新cellsBusy为0, 说明as空闲了, 可以被操作了
                        cellsBusy = 0;
                    }

                    // 重新置collide为已冲突, 让当前线程继续去竞争, 当识别到as冲突严重且可以扩容时, 则再次扩容as
                    collide = false;

                    // 以当前h, 并获取新的as再自旋去竞争
                    continue;                   // Retry with expanded table 使用扩展表重试
                }

                // 如果竞争a冲突, 或者方法调用前CAS单元格a失败, 则重新获取当前线程哈希值h(更新同一个线程的h, 与之前的h不一样了)
                h = ThreadLocalRandom.advanceProbe(h);
            }

            // 如果as还没创建, 或者长度n为0了, 说明as无效需要创建, 且cellsBusy标志位为0, 说明as空闲, 即数组没有其他线程在使用, 则CAS更新cellsBusy标志为1, 代表as在忙
            else if (cellsBusy == 0 && counterCells == as && U.compareAndSwapInt(this, CELLSBUSY, 0, 1)) {
                // 如果CAS成功, 说明当前线程竞争到操作as的机会
                boolean init = false;
                try {                           // Initialize table // 初始化表
                    // 重新检查as指针, 如果仍然指向counterCells, 说明还没被更新, 则构造长度为2的CounterCell[], h取模n-1得到单元格并存放往里面x, 此后init为true, 代表as已经被初始化了
                    if (counterCells == as) {
                        CounterCell[] rs = new CounterCell[2];
                        rs[h & 1] = new CounterCell(x);
                        counterCells = rs;
                        init = true;
                    }
                } finally {
                    // as操作完成后, 则更新cellsBusy为0, 代表counterCells数组已经空闲了
                    cellsBusy = 0;
                }

                // 如果as被当前线程初始化成功, 则结束自旋并返回, 代表x已经存放成功了; 如果as指针已经改变, 说明as并没有被当前线程初始化, 此时当前线程继续自旋
                if (init)
                    break;
            }

            // 如果as为空, 且在初始化as时如果CAS更新cellsBusy标志为1失败, 说明初始化as操作被别的线程抢占了, 则当前线程CAS更新baseCount叠加x, 即叠加到baseCount
            else if (U.compareAndSwapLong(this, BASECOUNT, v = baseCount, v + x))
                // 如果CAS成功, 此时结束并返回, 代表x已经叠加成功了; 如果CAS失败, 则还需继续自旋找机会叠加x
                break;                          // Fall back on using base 重新使用基础
        }
    }

    /* ---------------- Conversion from/to TreeBins -------------- */

    /**
     * 20210623
     * 在给定索引处替换 bin 中的所有链接节点，除非表太小，在这种情况下改为调整大小。
     */
    /**
     * Replaces all linked nodes in bin at given index unless table is
     * too small, in which case resizes instead.
     */
    // 红黑树化index对应桶中的普通链表
    private final void treeifyBin(Node<K,V>[] tab, int index) {
        Node<K,V> b; int n, sc;

        // 当前散列表tab, 待红黑树化的桶索引index, 桶结点b, tab容量n, 并发阈值sc
        if (tab != null) {
            // 如果容量n小于最小树化容量64, 则停止红黑树化链表, 转而初始化或者扩容当前散列表tab容量到n的2倍, 可能会初始化tab、无需扩容tab、协助转移tab结点、扩容tab并转移结点
            if ((n = tab.length) < MIN_TREEIFY_CAPACITY)
                tryPresize(n << 1);

            // 如果容量n满足最小树化容量64, 说明可以进行红黑树化b桶链表, 则对b结点加锁进行红黑树化
            else if ((b = tabAt(tab, index)) != null && b.hash >= 0) {
                synchronized (b) {
                    // 再次检查, 如果b结点还是为index桶的头结点, 说明b没有变化, 则对b结点加锁进行红黑树化
                    if (tabAt(tab, index) == b) {
                        TreeNode<K,V> hd = null, tl = null;

                        // 以链表方式遍历b链表, 当前遍历结点e, 根据e重新构造的结点p, 维护hd链表结点的前驱和后继
                        for (Node<K,V> e = b; e != null; e = e.next) {
                            TreeNode<K,V> p = new TreeNode<K,V>(e.hash, e.key, e.val, null, null);
                            if ((p.prev = tl) == null)
                                hd = p;
                            else
                                tl.next = p;
                            tl = p;
                        }

                        // volatile方式添加hd链表到散列表tab的index桶中
                        setTabAt(tab, index, new TreeBin<K,V>(hd));
                    }
                }
            }
        }
    }

    /**
     * 20210623
     * 返回非TreeNode上的列表，替换给定列表中的列表。
     */
    /**
     * Returns a list on non-TreeNodes replacing those in given list.
     */
    // 普通化红黑树结点链表
    static <K,V> Node<K,V> untreeify(Node<K,V> b) {
        Node<K,V> hd = null, tl = null;

        // 以链表方式遍历b链表, 当前遍历结点q, 根据q重新构造的结点p, 删除hd链表结点的前驱, 只维护hd链表结点的后继
        for (Node<K,V> q = b; q != null; q = q.next) {
            Node<K,V> p = new Node<K,V>(q.hash, q.key, q.val, null);
            if (tl == null)
                hd = p;
            else
                tl.next = p;
            tl = p;
        }

        // 返回普通结点hd链表
        return hd;
    }

    /* ---------------- TreeNodes -------------- */

    /**
     * Nodes for use in TreeBins
     */
    // 用于 TreeBins 的节点
    static final class TreeNode<K,V> extends Node<K,V> {
        TreeNode<K,V> parent;  // red-black tree links  红黑树链接
        TreeNode<K,V> left;
        TreeNode<K,V> right;
        TreeNode<K,V> prev;    // needed to unlink next upon deletion 删除后需要取消链接
        boolean red;

        TreeNode(int hash, K key, V val, Node<K,V> next,
                 TreeNode<K,V> parent) {
            super(hash, key, val, next);
            this.parent = parent;
        }

        // 根据hash值和key值，从根结点开始查找红黑树结点 => 同HashMap#getTreeNode（int，Object）
        Node<K,V> find(int h, Object k) {
            return findTreeNode(h, k, null);
        }

        /**
         * 20210620
         * 返回从给定根开始的给定键的 TreeNode（如果未找到，则返回 null）。
         */
        /**
         * Returns the TreeNode (or null if not found) for the given key
         * starting at given root.
         */
        // 根据hash值、key值和比较对象kc，从根结点开始查找查找红黑树结点 => 同HashMap#find（int，Object，Class）
        final TreeNode<K,V> findTreeNode(int h, Object k, Class<?> kc) {
            if (k != null) {
                TreeNode<K,V> p = this;
                do  {
                    // 实例结点p，p的hash值ph，比较结果dir，p的key值pk，p的左孩子pl，p的右孩子pr，要查找的结点q（null）
                    int ph, dir; K pk; TreeNode<K,V> q;
                    TreeNode<K,V> pl = p.left, pr = p.right;

                    // 如果ph大于h，说明要查找的结点在左子树
                    if ((ph = p.hash) > h)
                        p = pl;

                    // 如果ph小于h，说明要查找的结点在右子树
                    else if (ph < h)
                        p = pr;

                    // 如果ph等于h，且pk相等或者equals，说明p就是要找的结点
                    else if ((pk = p.key) == k || (pk != null && k.equals(pk)))
                        return p;

                    // 如果ph等于h，但pk不相等且不equals，说明哈希冲突了，需要往后继续遍历；如果pl为null，说明还没有左孩子，则往右子树继续遍历
                    else if (pl == null)
                        p = pr;

                    // 如果ph等于h，但pk不相等且不equals，说明哈希冲突了，需要往后继续遍历；如果pl不为null但pr为null，说明还没有右孩子，则往左子树继续遍历
                    else if (pr == null)
                        p = pl;

                    // 如果ph等于h，但pk不相等且不equals，说明哈希冲突了，需要往后继续遍历，但pl和pr都不为null，此时就需要判断x是否还实现了Comparable接口，如果是则继续比较是否相等，dir取其比较结果；如果dir<0，则p取左子树，否则取右子树
                    else if ((kc != null ||
                              (kc = comparableClassFor(k)) != null) &&
                             (dir = compareComparables(kc, k, pk)) != 0)
                        p = (dir < 0) ? pl : pr;

                    // 如果ph等于h，但pk不相等且不equals，说明哈希冲突了，需要往后继续遍历，但pl和pr都不为null，且比较对象也为null，则递归查找右孩子，如果找到则返回
                    else if ((q = pr.findTreeNode(h, k, kc)) != null)
                        return q;

                    // 如果ph等于h，但pk不相等且不equals，说明哈希冲突了，需要往后继续遍历，但pl和pr都不为null，且比较对象也为null，则递归查找右孩子，如果没找到则p设置为左子树，继续循环查找
                    else
                        p = pl;
                } while (p != null);
            }

            // 如果确实找不到则返回null
            return null;
        }
    }

    /* ---------------- TreeBins -------------- */

    /**
     * 20210620
     * 在 bin 的头部使用的 TreeNodes。 TreeBins 不保存用户键或值，而是指向 TreeNode 列表及其根。
     * 他们还维护一个寄生读写锁，迫使写者（持有 bin 锁）在树重组操作之前等待读者（没有）完成。
     */
    /**
     * TreeNodes used at the heads of bins. TreeBins do not hold user
     * keys or values, but instead point to list of TreeNodes and
     * their root. They also maintain a parasitic read-write lock
     * forcing writers (who hold bin lock) to wait for readers (who do
     * not) to complete before tree restructuring operations.
     */
    static final class TreeBin<K,V> extends Node<K,V> {
        TreeNode<K,V> root;// 红黑树根结点
        volatile TreeNode<K,V> first;// 链表形式的头结点(等于红黑树根结点)
        volatile Thread waiter;// 等待写锁的线程
        volatile int lockState;// 读写锁状态
        // values for lockState
        static final int WRITER = 1; // set while holding write lock 持有写锁时设置
        static final int WAITER = 2; // set when waiting for write lock 等待写锁时设置
        static final int READER = 4; // increment value for setting read lock 设置读锁的增量值

        /**
         * 20210620
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
        // 当 hashCode 相等且不可比较时，用于对插入进行排序的打破平局实用程序
        static int tieBreakOrder(Object a, Object b) {
            int d;
            if (a == null || b == null ||
                (d = a.getClass().getName().
                 compareTo(b.getClass().getName())) == 0)
                d = (System.identityHashCode(a) <= System.identityHashCode(b) ?
                     -1 : 1);
            return d;
        }

        /**
         * 20210620
         * 创建带有以 b 为首的初始节点集的 bin。
         */
        /**
         * Creates bin with initial set of nodes headed by b.
         */
        // 根据b链表构建TreeBin结点 => 类似于HashMap#treeify(Node)
        TreeBin(TreeNode<K,V> b) {
            // 创建null键、null值、next指针也为null、hash值为TREEBIN(-2)的Node结点
            super(TREEBIN, null, null, null);

            // b链表头结点设置为first结点
            this.first = b;

            // First结点b, 当前遍历结点x, x后继next, 根结点r, 直到遍历到b链尾
            TreeNode<K,V> r = null;
            for (TreeNode<K,V> x = b, next; x != null; x = next) {
                next = (TreeNode<K,V>)x.next;

                // 置空x的左右孩子
                x.left = x.right = null;

                // 如果r为null, 说明根结点还没设置, 则设置x为根结点, 更新r指针指向x
                if (r == null) {
                    x.parent = null;
                    x.red = false;
                    r = x;
                }

                // 如果r不为null, 说明根结点已经设置了, 此时x不再为根结点, 则获取x的键k, x的hash值h, kc为x的Class对象
                else {
                    K k = x.key;
                    int h = x.hash;
                    Class<?> kc = null;

                    // 从根结点r开始遍历, 当前遍历结点p, 比较结果dir, p的hash值ph, p的键pk
                    for (TreeNode<K,V> p = r;;) {
                        int dir, ph;
                        K pk = p.key;

                        // 如果ph大于x的hash值h, 说明x应该往左子树方向插入, 则dir为-1
                        if ((ph = p.hash) > h)
                            dir = -1;

                        // 如果ph小于x的hash值h, 说明x应该往右子树方向插入, 则dir为1
                        else if (ph < h)
                            dir = 1;

                        // 如果ph等于x的hash值h, 则还需要比较键的Class, 此时dir为其比较结果
                        else if ((kc == null &&
                                  (kc = comparableClassFor(k)) == null) ||
                                 (dir = compareComparables(kc, k, pk)) == 0)
                            // 当 hashCode 相等且不可比较时，用于对插入进行排序的打破平局实用程序
                            dir = tieBreakOrder(k, pk);

                        // 到这里, x与p的比较结果dir已经知道了, 如果dir小于等于0, 则x作为p左孩子, 否则x作为p右孩子, 并插入结点后平衡红黑树
                        TreeNode<K,V> xp = p;
                        if ((p = (dir <= 0) ? p.left : p.right) == null) {
                            x.parent = xp;
                            if (dir <= 0)
                                xp.left = x;
                            else
                                xp.right = x;
                            // 插入结点后平衡红黑树
                            r = balanceInsertion(r, x);
                            break;
                        }
                    }
                }
            }

            // b链表遍历完毕, 说明r结点上的红黑树已经构建完毕, 则设置r为TreeBin桶的root指针
            this.root = r;

            // 递归检查指定结点是否为红黑树
            assert checkInvariants(root);
        }

        /**
         * 20210620
         * 获取用于树重组的写锁。
         */
        /**
         * Acquires write lock for tree restructuring.
         */
        // CAS方式设置lockState为1(持有写锁), 如果CAS设置成功, 说明当前线程获取到写锁, 此时直接返回即可; 如果CAS设置失败, 说明当前线程获取不到写锁, 此时需要继续争抢写锁
        private final void lockRoot() {
            // CAS方式设置lockState为1(持有写锁时设置), 如果CAS设置成功, 说明当前线程获取到写锁, 此时直接返回即可
            if (!U.compareAndSwapInt(this, LOCKSTATE, 0, WRITER))
                // 如果CAS设置失败, 说明当前线程获取不到写锁, 此时需要继续争抢写锁
                contendedLock(); // offload to separate method 卸载到分离方法
        }

        /**
         * 20210620
         * 为树重组释放写锁。
         */
        /**
         * Releases write lock for tree restructuring.
         */
        // 重置锁状态, 释放读锁/写锁
        private final void unlockRoot() {
            lockState = 0;
        }

        /**
         * 20210620
         * 可能阻塞等待根锁定。
         */
        /**
         * Possibly blocks awaiting root lock.
         */
        /**
         * 争抢写锁，争抢不到写锁的会进入阻塞状态，直到所有调用TreeBin#find（int，Object）的线程调用完毕后唤醒，从而重新争抢写锁。
         * a. 如果当前红黑树不存在写或者读线程, 则当前线程去竞争写锁, 如果竞争成功则返回;
         * b. 如果当前红黑树写锁或者读锁正在被持有, 且不存在等待写锁的线程，则当前线程去竞争成为等待锁的线程, 竞争成功则成为等待锁的线程;
         * c. 如果当前线程成为等待锁的线程, 但竞争读/写锁失败, 则进入阻塞状态;
         * d. 而那些争抢不到写锁, 也进入不了阻塞状态成为等待写锁的线程, 会一直自旋等待锁状态变更;
         */
        private final void contendedLock() {
            boolean waiting = false;

            // 开始自旋
            for (int s;;) {
                // 如果锁状态非101, 说明当前红黑树不存在写或者读线程, 此时允许当前线程去竞争锁
                if (((s = lockState) & ~WAITER) == 0) {
                    // CAS设置lockState为1(持有写锁), 如果CAS成功, 说明当前线程成功获取到写锁, 此时置空等待线程waiter, 然后返回即可
                    if (U.compareAndSwapInt(this, LOCKSTATE, s, WRITER)) {
                        if (waiting)
                            waiter = null;
                        return;
                    }
                }

                // 如果锁状态非010, 说明当前红黑树不存在等待锁的线程, 此时允许当前线程成功为等待锁的线程
                else if ((s & WAITER) == 0) {
                    // CAS设置lockState为011或者110(等待读锁或者等待写锁的状态), 如果CAS成功说明当前线程成功成为等待锁的线程, 此时设置waiter为当前线程
                    if (U.compareAndSwapInt(this, LOCKSTATE, s, s | WAITER)) {
                        waiting = true;
                        waiter = Thread.currentThread();
                    }
                }

                // 如果当前线程成为等待写锁的线程, 但竞争写锁失败, 则进入阻塞状态
                else if (waiting)
                    LockSupport.park(this);
            }
        }

        /**
         * 20210620
         * 如果没有，则返回匹配的节点或 null。 尝试使用来自根的树比较进行搜索，但在锁不可用时继续线性搜索。
         */
        /**
         * Returns matching node or null if none. Tries to search
         * using tree comparisons from root, but continues linear
         * search when lock not available.
         */
        // 根据hash值和key值，从根结点开始查找红黑树结点: 如果当前红黑树存在写线程或者等待写锁线程, 则以链表的方式去遍历出红黑树结点并返回; 如果当前红黑树没有写线程或者等待写锁线程, 则叠加读锁状态后以红黑树方式去遍历红黑树结点并返回
        final Node<K,V> find(int h, Object k) {
            if (k != null) {
                // 从根结点开始遍历, 当前遍历结点e, 锁状态s, e键ek,
                for (Node<K,V> e = first; e != null; ) {
                    int s; K ek;

                    // 如果锁状态为011, 说明当前红黑树存在写线程或者等待写锁线程, 为了减少锁竞争以便写操作尽快完成, 则以遍历链表的方式去遍历出红黑树结点并返回
                    if (((s = lockState) & (WAITER|WRITER)) != 0) {
                        if (e.hash == h &&
                            ((ek = e.key) == k || (ek != null && k.equals(ek))))
                            return e;
                        e = e.next;
                    }

                    // 否则, 说明当前红黑树没有写线程或者等待写锁线程, 则CAS叠加lockState读锁状态(每个读线程叠加一次), 然后再以红黑树方式去遍历红黑树结点并返回
                    else if (U.compareAndSwapInt(this, LOCKSTATE, s, s + READER)) {
                        TreeNode<K,V> r, p;
                        try {
                            // 根据hash值、key值和比较对象kc，从根结点开始查找查找红黑树结点
                            p = ((r = root) == null ? null :
                                 r.findTreeNode(h, k, null));
                        } finally {
                            // 最后释放当前读锁状态, 如果释放读锁前的状态为读状态, 或者为等待写锁状态且存在等待写锁线程, 说明当前线程为最后一个读线程, 需要唤醒因调用contendedLock（）争抢不到写锁而进入阻塞等待的线程，使其重新争抢写锁
                            Thread w;
                            if (U.getAndAddInt(this, LOCKSTATE, -READER) == (READER|WAITER) && (w = waiter) != null)
                                LockSupport.unpark(w);
                        }
                        return p;
                    }
                }
            }

            // 如果确实找不到则返回null
            return null;
        }

        /**
         * Finds or adds a node.
         * @return null if added
         */
        // 红黑树结点的添加方法（插入成功则返回null，插入失败则返回已经存在的结点）=> 类似HashMap#putTreeVal（HashMap，Node，int，K，V）, 但这里还需要竞争TreeBin桶头结点的写锁
        final TreeNode<K,V> putTreeVal(int h, K k, V v) {
            // 比较键的Class对象kc，是否已经找过了searched，根节点root
            Class<?> kc = null;
            boolean searched = false;

            // 从根结点root开始查找，比较结果dir， 比较结点hash值ph，比较结点key值pk，当前遍历结点p
            for (TreeNode<K,V> p = root;;) {
                int dir, ph; K pk;

                // 如果p为null且p不为叶子结点的孩子结点, 说明红黑树根结点还没被初始化, 则把k和v当做根结点插入
                if (p == null) {
                    first = root = new TreeNode<K,V>(h, k, v, null, null);
                    break;
                }

                // 如果ph大于h，说明插入结点应该在p的左边，dir为-1
                else if ((ph = p.hash) > h)
                    dir = -1;

                // 如果ph小于h，说明插入结点应该在p的右边，dir为1
                else if (ph < h)
                    dir = 1;

                // 如果ph等于h，且pk为插入结点的key值，或者equals，则说明p就是要插入的位置，此时返回p结点
                else if ((pk = p.key) == k || (pk != null && k.equals(pk)))
                    return p;

                // 如果ph等于h，且pk不为插入结点的key值，也不equals，则判断x是否还实现了Comparable接口，如果是则继续比较是否相等
                else if ((kc == null &&
                          (kc = comparableClassFor(k)) == null) ||
                         (dir = compareComparables(kc, k, pk)) == 0) {
                    // 如果还没找过，则递归查找左右子树, 直到找到指定hash和key匹配的结点q返回即可
                    if (!searched) {
                        TreeNode<K,V> q, ch;
                        searched = true;
                        if (((ch = p.left) != null &&
                             (q = ch.findTreeNode(h, k, kc)) != null) ||
                            ((ch = p.right) != null &&
                             (q = ch.findTreeNode(h, k, kc)) != null))
                            return q;
                    }

                    // 如果已经递归找过了，当hashCode相等且不可比较时, 用于打破比较平局的情况 => 比较a和b两个对象的ClassName相等以及原始hashCode
                    dir = tieBreakOrder(k, pk);
                }

                // 到这里，比较结果dir已经确定，如果dir<=0, 则从p的左子树开始找，否则从p的右子树开始找，p结点备份为xp, p继续作为左孩子或者右孩子，链表头结点f，如果p为null，说明到了叶子结点，则构建TreeNode结点，next指向链表头结点f
                TreeNode<K,V> xp = p;
                if ((p = (dir <= 0) ? p.left : p.right) == null) {
                    TreeNode<K,V> x, f = first;
                    first = x = new TreeNode<K,V>(h, k, v, f, xp);

                    // 如果f不为null, 则关联x与f的关系
                    if (f != null)
                        f.prev = x;

                    // 如果dir<=0，说明x应该在xp的左边，则设置x为xp的左孩子
                    if (dir <= 0)
                        xp.left = x;
                    // 如果dir>0，说明x应该在xp的右边，则设置x为xp的右孩子
                    else
                        xp.right = x;

                    // 如果父结点不为红结点, 则插入x结点并标记为红结点即可退出遍历
                    if (!xp.red)
                        x.red = true;
                    // 如果父结点为红结点, 此时如果插入会出现红红情况, 不符合红黑树性质, 因此需要对root结点加锁平衡, 平衡后释放锁结束遍历
                    else {
                        // CAS方式设置lockState为1(持有写锁时设置), 如果CAS设置成功, 说明当前线程获取到写锁, 此时直接返回即可; 如果CAS设置失败, 说明当前线程获取不到写锁, 此时需要继续争抢写锁
                        lockRoot();
                        try {
                            // 如果当前线程成功争抢到写锁后, 则进行插入后平衡红黑树
                            root = balanceInsertion(root, x);
                        } finally {
                            // 重置锁状态, 释放读锁/写锁
                            unlockRoot();
                        }
                    }
                    break;
                }
            }

            // 递归检查指定结点是否为红黑树
            assert checkInvariants(root);

            // 插入成功则返回null
            return null;
        }

        /**
         * 20210620
         * 删除给定节点，该节点必须在此调用之前存在。 这比典型的红黑删除代码更混乱，
         * 因为我们无法将内部节点的内容与叶后继交换，该后继由可独立于锁访问的“next”指针固定。 因此，我们交换树链接。
         */
        /**
         * Removes the given node, that must be present before this
         * call.  This is messier than typical red-black deletion code
         * because we cannot swap the contents of an interior node
         * with a leaf successor that is pinned by "next" pointers
         * that are accessible independently of lock. So instead we
         * swap the tree linkages.
         *
         * @return true if now too small, so should be untreeified
         */
        /**
         * removeTreeNode（HashMap，Node，boolean）：
         *
         * - 替代结点：红黑树是一种自平衡的二叉搜索树，而二叉搜索树删除，本质上是找前驱或者后继结点来替代删除（这里是replacement替代p然后删除p）。
         * - A. 如果要删除的结点是叶子结点：则直接删除即可（肯定为黑色）。
         * - B. 如果要删除的结点只有一个孩子结点：则使用孩子结点进行替代，然后删除"替代结点"。
         * - C. 如果要删除的结点有两个孩子结点：则需要找到前驱或者后继进行替代，然后删除"替代结点"。
         *   - C.1. 如果替代结点没有孩子结点：此时所在的结点为2-3-4树的2结点，则直接要"替代结点"即可。
         *   - C.2. 如果替代结点有孩子结点且孩子结点为替代方向：此时所在的结点为2-3-4树的3结点或者4结点，则继续使用孩子结点进行替代，然后“替代结点”即可（二次替代）。
         * - 无论是哪种情况，红黑树结点的删除方法，都要调用平衡红黑树的方法，在删除结点前/后平衡红黑树。
         */
        // 红黑树结点的删除方法 => 类似HashMap#removeTreeNode（HashMap，Node，boolean）, 但这里还需要竞争TreeBin桶头结点的写锁
        final boolean removeTreeNode(TreeNode<K,V> p) {
            // p的后继next, p的前驱pred, 根结点r, 根结点的左孩子rl
            TreeNode<K,V> next = (TreeNode<K,V>)p.next;
            TreeNode<K,V> pred = p.prev;  // unlink traversal pointers 取消链接遍历指针
            TreeNode<K,V> r, rl;

            // 如果pred为null, 说明p为桶头结点，则更新后继succ作为新的桶头
            if (pred == null)
                first = next;
            // 如果pred不为null，说明p不为桶头结点，则链接前驱和后继，跳过p结点
            else
                pred.next = next;
            if (next != null)
                next.prev = pred;

            // 链接pred、succ后，如果桶头结点为null，说明桶内已经没有数据了，所以不用再处理了，直接返回true, 说明可以拆除当前红黑树，将其退化成普通链表
            if (first == null) {
                root = null;
                return true;
            }

            // 如果root为null或者没有孩子时，说明红黑树结点太少了，则返回true, 说明可以拆除当前红黑树，将其退化成普通链表
            if ((r = root) == null || r.right == null || // too small
                (rl = r.left) == null || rl.left == null)
                return true;

            // 到这里, 说明真的是要添加p到红黑树中, 则CAS方式设置lockState为1(持有写锁时设置), 如果CAS设置成功, 说明当前线程获取到写锁, 此时直接返回即可; 如果CAS设置失败, 说明当前线程获取不到写锁, 此时需要继续争抢写锁
            lockRoot();
            try {
                // p左孩子pl，p右孩子pr，替代结点replacement（null）
                TreeNode<K,V> replacement;
                TreeNode<K,V> pl = p.left;
                TreeNode<K,V> pr = p.right;

                // 如果p有两个孩子结点，则一路遍历右孩子pr的左孩子sl，直到sl为叶子结点，即查找p的后继s，并交换p和s的颜色
                if (pl != null && pr != null) {
                    TreeNode<K,V> s = pr, sl;
                    while ((sl = s.left) != null) // find successor
                        s = sl;
                    boolean c = s.red; s.red = p.red; p.red = c; // swap colors

                    // 后继s的右孩子sr, p的父结点pp
                    TreeNode<K,V> sr = s.right;
                    TreeNode<K,V> pp = p.parent;

                    // 如果后继s为p的右孩子，说明s是p的直接后继，直接反转两者的父子关系即可，即s作为父亲，p作为右孩子，使得p交换到了后继s的位置
                    if (s == pr) { // p was s's direct parent
                        p.parent = s;
                        s.right = p;
                    }
                    // 如果后继s不是p的右孩子，说明s是p的间接后继，则维护s、p、sp、pr的指针关系，使得p交换到了后继s的位置
                    else {
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

                    // 交换了p与后继s的位置后，继续更新s、p、sr、sl、pl、pp的关系
                    p.left = null;
                    if ((p.right = sr) != null)
                        sr.parent = p;
                    if ((s.left = pl) != null)
                        pl.parent = s;
                    if ((s.parent = pp) == null)
                        r = s;
                    else if (p == pp.left)
                        pp.left = s;
                    else
                        pp.right = s;

                    // 到这里，p与s交换了位置，并完成了所有父结点、孩子节点的指针关系维护。如果sr不为null，说明s有右孩子（这里由于原s是通过遍历原p的右孩子的所有左孩子找到的，所以原s肯定是没有左孩子的，如果判断没有右孩子，则可以说明原s肯定是没有孩子的），则设置右孩子为替代结点(相当于情况C.2)
                    if (sr != null)
                        replacement = sr;
                    // 否则，如果原s没有右孩子，则交换后的p就为替代结点(相当于情况C.1)
                    else
                        replacement = p;
                }

                // 否则，如果原s没有右孩子，则交换后的p就为替代结点(相当于情况C.1)
                else if (pl != null)
                    replacement = pl;

                // 如果实例结点p只有右孩子时，则替代结点为右孩子pr（相当于情况B）
                else if (pr != null)
                    replacement = pr;

                // 如果实例结点p只有右孩子时，则替代结点为右孩子pr（相当于情况B）
                else
                    replacement = p;

                // 到这一步，原p的替代结点replacement已经找到了。如果replacement不为p结点，说明为2-3-4树3结点或者4结点的红结点，即情况B和C.2，为了让replacement代替p结点，则链接pp与replacement并清空p所有的链接，此后p就没有被任何结点引用等待GC回收，相当于p脱离了红黑树
                if (replacement != p) {
                    TreeNode<K,V> pp = replacement.parent = p.parent;
                    if (pp == null)
                        r = replacement;
                    else if (p == pp.left)
                        pp.left = replacement;
                    else
                        pp.right = replacement;
                    p.left = p.right = p.parent = null;
                }

                // 删除结点前/后平衡红黑树，如果x所在结点为2-3-4树的2结点，则平衡后再删除x，否则在平衡前对应的结点就已经删除了（此时x作为该结点的替代结点而保留下来），r为根结点
                root = (p.red) ? r : balanceDeletion(r, replacement);

                // 如果替代节点replacement为p结点，说明要删除的是2-3-4树的2结点（相当于情况A和C.1），则脱离p链接
                if (p == replacement) {  // detach pointers
                    TreeNode<K,V> pp;
                    if ((pp = p.parent) != null) {
                        if (p == pp.left)
                            pp.left = null;
                        else if (p == pp.right)
                            pp.right = null;
                        p.parent = null;
                    }
                }
            } finally {
                // 重置锁状态, 释放读锁/写锁
                unlockRoot();
            }

            // 递归检查指定结点是否为红黑树
            assert checkInvariants(root);

            // 返回false, 说明正常插入结点到红黑树了, 不需要拆除当前红黑树退化成普通链表
            return false;
        }

        /* ------------------------------------------------------------ */
        // Red-black tree methods, all adapted from CLR

        // 左旋p结点，左右左右右 => 同HashMap#rotateLeft(TreeNode, TreeNode)
        static <K,V> TreeNode<K,V> rotateLeft(TreeNode<K,V> root,
                                              TreeNode<K,V> p) {
            TreeNode<K,V> r, pp, rl;

            // 如果p的右节点r不为null，则有必要进行左旋，否则直接返回root即可
            if (p != null && (r = p.right) != null) {
                // 如果p有右结点r，且r还有左结点rl, 则旋转后成为p的右结点，并连接上rl与p的关系
                if ((rl = p.right = r.left) != null)
                    rl.parent = p;

                // 如果p有父结点，则关联p的父结点pp与p的右结点r的关系，且如果pp为null，说明需要更新r为根结点，以及变为黑色
                if ((pp = r.parent = p.parent) == null)
                    (root = r).red = false;

                // 如果pp不为null，说明r不需要成为根结点，则关联pp与r的关系
                else if (pp.left == p)
                    pp.left = r;
                else
                    pp.right = r;

                // 关联p结点与r结点的关系
                r.left = p;
                p.parent = r;
            }

            return root;
        }

        // 右旋p结点，右左右左左 => 同HashMap#rotateRight(TreeNode, TreeNode)
        static <K,V> TreeNode<K,V> rotateRight(TreeNode<K,V> root,
                                               TreeNode<K,V> p) {
            TreeNode<K,V> l, pp, lr;

            // 如果p的左节点l不为null，则有必要进行右旋，否则直接返回root即可
            if (p != null && (l = p.left) != null) {
                // 如果p有左结点l，且l还有右结点lr, 则旋转后成为p的左结点，并连接上rl与p的关系
                if ((lr = p.left = l.right) != null)
                    lr.parent = p;

                // 如果p有父结点，则关联p的父结点pp与p的左结点r的关系，且如果pp为null，说明需要更新l为根结点，以及变为黑色
                if ((pp = l.parent = p.parent) == null)
                    (root = l).red = false;

                // 如果pp不为null，说明l不需要成为根结点，则关联pp与l的关系
                else if (pp.right == p)
                    pp.right = l;
                else
                    pp.left = l;

                // 如果pp不为null，说明l不需要成为根结点，则关联pp与l的关系
                l.right = p;
                p.parent = l;
            }

            return root;
        }

        /**
         * balanceInsertion（TreeNode，TreeNode）：对应2-3-4树的情况：
         *
         * - a. 空结点新增：成为一个2结点，插入前树为null，插入后x需要变黑色，作为根结点。
         * - b. 合并到2结点中：成为一个3结点，插入前2结点为黑色，插入后无论是（上黑下左红 |  上黑下右红）, 都符合3结点要求，因此无需调整。
         * - c. 合并到3结点中：成为一个4结点，插入前为3结点（上黑下左红 |  上黑下右红），插入后成为4结点黑红红的情况，根据x插入位置不同分为6种情况：
         *   - c.2.1.  左三(中左左*) ，黑红红，不符合红黑树定义 => 需要调整，则中1右旋，中1变红，左1变黑。
         *   - c.2.2. 中左右*(其实就相当于左三，因为对父结点进行左旋，即得到左三) ，黑红红，不符合红黑树定义 => 需要调整，则左1左旋（得到左三），中1右旋，中1变红，新左变黑。
         *   - c.2.3. 右三(中 右右*) ，黑红红，不符合红黑树定义 => 需要调整，则中1左旋，中1变红，右1变黑。
         *   - c.2.4. 中 右左*(其实就相当于右三，因为对父结点进行右旋，即得到右三) 黑红红，不符合红黑树定义 => 需要调整，则右1右旋（得到右三），中1左旋，中1变红，新右变黑。
         *   - c.2.5. 中左 右*，黑红 红，符合红黑树定义 => 无需调整。
         *   - c.2.6. 中左* 右，黑红 红，符合红黑树定义 => 无需调整。
         * - d. 合并到4结点中：成为一个裂变状态（变色后相当于升元了），插入前为4结点（黑红红），插入后4结点颜色反转，爷结点成为新的x结点，准备下一轮的向上调整，根据x插入的位置不同分为4种情况：
         *   - d.2.1. 中左左* 右(黑红红 红)，不符合红黑树定义 => 需要调整，则中变红，左1变黑，左2保持为红， 右1变黑，中看作为“插入结点”，继续向上调整。
         *   - d.2.2. 中左右* 右(黑红红 红)，不符合红黑树定义 => 需要调整，则中变红，左1变黑，右1保持为红，右2变黑，中看作为“插入结点”，继续向上调整。
         *   - d.2.3. 中左 右左*(黑红 红红)，不符合红黑树定义 => 需要调整，则中变红，左1变黑，左2保持为红，右1变黑，中看作为“插入结点”，继续向上调整。
         *   - d.2.4. 中左 右右*(黑红 红红)，不符合红黑树定义 => 需要调整，则中变红，左1变黑，右1变黑，右2保持为红，中看作为“插入结点”，继续向上调整。
         */
        // 插入后平衡红黑树，到了这步红黑树结点的二叉树指针关系已经确定好了的, 只需要进行调整和变色就可以了 => 同HashMap#balanceInsertion(TreeNode, TreeNode)
        static <K,V> TreeNode<K,V> balanceInsertion(TreeNode<K,V> root,
                                                    TreeNode<K,V> x) {
            x.red = true;// 标记插入结点x为红结点

            // 从x结点向上遍历, 发现需要调整则进行调整
            for (TreeNode<K,V> xp, xpp, xppl, xppr;;) {
                // x结点父结点xp, 如果为null, 说明x为根结点，相当于情况a，则设置当前结点为黑色(性质2)
                if ((xp = x.parent) == null) {
                    x.red = false;
                    return x;
                }

                // 如果x结点不是根结点, 且父结点xp为黑色或者没有爷结点xpp时，相当于情况b，即合并到2结点中，所以无需调整, 直接返回根结点即可。
                else if (!xp.red || (xpp = xp.parent) == null)
                    return root;

                // 如果x结点不是根结点，且父节点xp为红色，且有爷结点，且父结点为爷结点的左孩子时，相当于插入到3、4结点中，即情况c.2.1 & c.2.2 & d.2.1 & d.2.2
                if (xp == (xppl = xpp.left)) {// 这里的xppl是为了获取当父结点为爷结点右孩子时的叔结点
                    // 如果xpp右孩子叔结点不为null, 且为红结点时，相当于情况d.2.1 和 d.2.2，即合并到4结点，因此x结点需要插入到了父结点xp的孩子结点中，此时标记叔结点为黑色, 父节点为黑色, 爷结点为红色（4结点颜色反转），且设置爷结点为新的x结点, 准备进行下一轮的向上调整
                    if ((xppr = xpp.right) != null && xppr.red) {
                        xppr.red = false;
                        xp.red = false;
                        xpp.red = true;
                        x = xpp;
                    }

                    // 如果x没有叔结点，或者叔结点为黑色（不会出现，因为黑色不平衡），相当于插入到3结点中
                    else {
                        // 如果x结点为父节点xp的右孩子，即父结点为爷结点的左孩子且为红结点, x结点插入到父结点的右孩子时(相当于c.2.2 中左右*, 合并到3结点)
                        if (x == xp.right) {
                            // 此时将父结点xp左旋即可得到c.2.1左三的情况（相当于x结点与父结点交换了位置），x赋值为了xp
                            root = rotateLeft(root, x = xp);

                            // 左旋后更新指针：原来的x结点作为xp, 如果为null(不会出现), 则xpp设置为null, 否则xpp设置为原来的x结点的爷结点，x为原来x结点的父结点
                            xpp = (xp = x.parent) == null ? null : xp.parent;

                            // 这步if其实做的是把c.2.2的情况转换为c.2.1左三的情况
                        }

                        // 到这里，无论是哪种情况，如果xp不为null那肯定都是红色, 且x肯定为父亲的左孩子(相当于c.2.1, 左三, 合并到3结点) => 父变黑, 爷变红, 爷进行右旋, 变成4结点
                        if (xp != null) {
                            xp.red = false;
                            if (xpp != null) {
                                xpp.red = true;
                                root = rotateRight(root, xpp);
                            }
                        }
                    }
                }

                // 如果x结点不是根结点，且父节点xp为红色，且有爷结点，且父结点为爷结点的右孩子时，相当于插入到3、4结点中，即情况c.2.3 & c.2.4 & d.2.3 & d.2.4
                else {
                    // 如果xpp做孩子叔结点不为null, 且为红结点时，相当于情况d.2.3 和 d.2.4，即合并到4结点，因此x结点需要插入到了父结点xp的孩子结点中，此时标记叔结点为黑色, 父节点为黑色, 爷结点为红色（4结点颜色反转），且设置爷结点为新的x结点, 准备进行下一轮的向上调整
                    if (xppl != null && xppl.red) {
                        xppl.red = false;
                        xp.red = false;
                        xpp.red = true;
                        x = xpp;
                    }

                    // 如果x没有叔结点，或者叔结点为黑色（不会出现，因为黑色不平衡），相当于插入到3结点中
                    else {
                        // 如果x结点为父节点xp的左孩子，即父结点为爷结点的右孩子且为红结点, x结点插入到父结点的左孩子时(相当于c.2.4 中右左*, 合并到3结点)
                        if (x == xp.left) {
                            // 此时将父结点xp右旋即可得到c.2.3右三的情况（相当于x结点与父结点交换了位置），x赋值为了xp
                            root = rotateRight(root, x = xp);

                            // 右旋后更新指针：原来的x结点作为xp, 如果为null(不会出现), 则xpp设置为null, 否则xpp设置为原来的x结点的爷结点，x为原来x结点的父结点
                            xpp = (xp = x.parent) == null ? null : xp.parent;

                            // 这步if其实做的是把c.2.4的情况转换为c.2.3右三的情况
                        }

                        // 到这里，无论是哪种情况，如果xp不为null那肯定都是红色, 且x肯定为父亲的右孩子(相当于c.2.3, 右三, 合并到3结点) => 父变黑, 爷变红, 爷进行左旋, 变成4结点
                        if (xp != null) {
                            xp.red = false;
                            if (xpp != null) {
                                xpp.red = true;
                                root = rotateLeft(root, xpp);
                            }
                        }
                    }
                }
            }
        }

        /**
         * balanceDeletion（TreeNode，TreeNode）：删除结点前/后平衡红黑树，如果x所在结点为2-3-4树的2结点，则平衡后再删除，如果x所在结点为3结点或者4结点，在平衡前对应的结点就已经删除了，此时x作为该结点的替代结点而保留下来：
         *
         * - x自己搞得定：
         *   - 自己搞得定的意思就是，可以在自己结点内部处理完毕（对应2-3-4树结构），不影响其他树的结构。
         *   - a.1. x为3结点或者4结点的红结点：直接置黑返回x结点即可调整完毕（因为x是作为替代结点而保留下来的），然后交由上层方法删除x结点。
         * - x自己搞不定，兄弟搞得定：
         *   - 自己搞不定的意思就是，自身结点为黑结点，如果直接删除会导致父结点所在的树黑色不平衡。
         *   - 兄弟搞得定的意思就是，兄弟结点存在多余的子结点（即兄弟结点为3结点或者4结点），此时，x的父结点就可以借出结点下来合并到x结点中，兄弟结点再借出结点合并到父结点中，这样x就可以顺利删除了，同时2-3-4树的结构还保持不变。
         *   - 但是，前提是x的兄弟结点是真正的兄弟结点，即为黑色的结点，如果为红色的结点，说明其只是父结点（3结点）的红结点，此时需要对父结点进行旋转，以保证x有真正的兄弟结点。
         *   - b.1. 兄弟结点为3结点，但无右（左）：在x在左子树一方时，x的兄弟结点xpr为右子树，如果xpr无右孩子在对父结点左旋时，会导致xpr为null，导致2-3-4树的结构不正确，因此，b.1是一个临时情况，需要对xpr进行右旋，转换为b.2有右进一步处理。x为右子树一方时则相反。
         *   - b.2. 兄弟结点为3结点，但有右（左）：在x在左子树一方时，x的兄弟结点xpr为右子树，如果xpr有右，则可以顺利地对父结点xp进行左旋。左旋后，在2-3-4树结构看来，xp作为xpr的左孩子（相当于父结点借出去一个结点，合并到x结点中），xpr作为xp的父亲（相当于兄弟结点借出去一个结点，合并到父结点中），xpr借出去的结点颜色为xp借出去的结点颜色，xp借出去的结点颜色一定要为黑色（相当于3结点），xpr剩余结点一定要为黑色（相当于叶子结点），返回x结点即可调整完毕，交由上层方法删除x结点。x为右子树一方时则相反。
         *   - b.3. 兄弟结点为4结点，肯定有右：在x在左子树一方时，x的兄弟结点xpr为右子树，如果xpr有右，则可以顺利地对父结点xp进行左旋。左旋后，在2-3-4树结构看来，xp作为xpr的左孩子（相当于父结点借出去一个结点，合并到x结点中），xpr作为xp的父亲（相当于兄弟结点借出去一个结点，合并到父结点中，而且还多借出左孩子合并到x结点中），xpr合并到父结点的颜色为xp借出去的结点颜色（而借出去的左孩子本来为红色所以不用变），xp借出去的结点颜色一定要为黑色（相当于4结点），xpr剩余结点一定要为黑色（相当于叶子结点），返回x结点即可调整完毕，交由上层方法删除x结点。x为右子树一方时则相反。
         *   - 在b.3中对于兄弟结点为4结点时，兄弟结点可以借出1个结点（需要旋转两次）或者2个结点（只需要旋转一次），在JDK中无论是HashMap还是TreeMap，都选择借出2个结点，因为可以减少花销。
         * - x自己搞不定，而且兄弟也搞不定：
         *   - 自己搞不定的意思就是，自身结点为黑结点，如果直接删除会导致父结点所在的树黑色不平衡。
         *   - 兄弟也搞不定的意思就是，兄弟结点也为黑结点，没有多余的子结点，如果直接删除x，则导致叔结点所在路径多了一个黑色结点，造成黑色不平衡。
         *   - c.1. 兄弟结点为2结点：此时，为了让x能够顺利删除，兄弟结点需要置红（自损），这样x在删除后，x父结点所在树还是黑色平衡的。但是，如果x父结点为黑色，x爷结点所在树则不黑色平衡了（因为父结点这边少了一个黑色结点），所以父结点的叔结点要也要被置红。因此需要一路向上自损，直到碰到任意一个终止条件即可结束：
         *     - 自损的终止条件1（向上碰到根结点）：经过一路置红叔结点（置红叔结点是没问题的，因为出现该情况是叶子结点为3结点黑黑黑的时候，此时如果叔结点没有孩子结点即为黑色，而对于更上层的叔结点来说，貌似不会出现叔为黑红红这种情况），直到循环到根结点时（因为上面已经没有父节点了），则代表自损完毕，此时整棵树都是黑色平衡的了（都减少了一个黑色结点）。
         *     - 自损的终止条件2（向上碰到红结点）：如果碰到红色结点时，只需要把该结点置黑，则不需要在置红叔结点了，此时相当于在父结点这边子树补回了一个黑色结点，而不影响叔结点那边子树的黑色结点数目，因此整棵树还是黑色平衡的。
         */
        // 删除结点前/后平衡红黑树，如果x所在结点为2-3-4树的2结点，则平衡后再删除，如果x所在结点为3结点或者4结点，在平衡前对应的结点就已经删除了，此时x作为该结点的替代结点而保留下来 => 同HashMap#balanceDeletion(TreeNode, TreeNode)
        static <K,V> TreeNode<K,V> balanceDeletion(TreeNode<K,V> root,
                                                   TreeNode<K,V> x) {
            // 传入的根结点root，当前结点x，x的父结点xp，xp的左孩子xpl，xp的右孩子xpr
            for (TreeNode<K,V> xp, xpl, xpr;;)  {
                // 如果x为null，或者为指定的根结点root，则无需调整，直接返回root即可
                if (x == null || x == root)
                    return root;

                // 如果x不为null，也不为root，且父节点xp为null，说明为自损的终止条件1（向上碰到根结点），此时x为根结点，此时置黑x结点即可（此时达到整棵树的黑色平衡，因为上一轮已经对叔结点进行置红了），终止循环向上调整，返回x结点作为根结点
                else if ((xp = x.parent) == null) {
                    x.red = false;
                    return x;
                }

                // 如果x不为null，也不为root，且父结点xp也不为null，且x为红结点，说明为自损的终止条件2（向上碰到红结点），此时置黑x结点即可（此时达到整棵树的黑色平衡，因为上一轮已经对叔结点进行置红了），终止循环向上调整，返回x结点作为根结点。还有一种情况就是，x自己搞的定（3、4结点的红结点时），置黑直接返回即可。
                else if (x.red) {
                    x.red = false;
                    return root;
                }

                // 如果x不为null，也不为根结点，且父结点也不为null，且x为黑色结点(删除2结点时，此时自己搞不定，需要兄弟和父亲帮忙)，且x为xp的左孩子（这里的xpl也等于x为右孩子时的叔结点），则按左的方式调整
                else if ((xpl = xp.left) == x) {
                    // 叔结点xpr为xp的右孩子，如果为红结点时，说明xpr不是x真正的兄弟结点（xpr只是为xp的红结点，此时xp为3结点)，则置黑叔结点xpr，置红父结点xp，对xp进行左旋，左旋后xpr成为xp的父结点，原xpr的左结点成为xp的右结点（这时x与x真正的兄弟结点才一一对应）
                    if ((xpr = xp.right) != null && xpr.red) {
                        xpr.red = false;
                        xp.red = true;
                        root = rotateLeft(root, xp);
                        xpr = (xp = x.parent) == null ? null : xp.right;
                    }

                    // 如果xpr为null，说明x没有叔结点，也就是兄弟没有得借，因此当x删除后，xp所在树会少一个黑结点，为了保证黑色平衡，所以把xp赋值给x，继续循环向上调整，直到碰到红结点或者根结点
                    if (xpr == null)
                        x = xp;// 可以看作xpr为红色（也相当于null），需要向上置红调整

                    // 如果xpr不为null，说明x有真正的兄弟xpr，则继续判断xpr的左孩子sl，右孩子sr，看xpr是否有得借
                    else {
                        TreeNode<K,V> sl = xpr.left, sr = xpr.right;

                        // 如果xpr没有孩子结点，则说明xpr没有得借，或者xpr的孩子结点都为黑色，说明xpr为叶子结点，也没有得借，相当于情况c.1，因此当x删除后，xp所在树会少一个黑结点，为了保证黑色平衡，所以把xp赋值给x，继续循环向上调整，直到碰到红结点或者根结点
                        if ((sr == null || !sr.red) &&
                            (sl == null || !sl.red)) {
                            xpr.red = true;
                            x = xp;
                        }

                        // 如果兄弟结点xpr有得借，则继续判断兄弟结点到底是3结点还是4结点
                        else {
                            // 如果xpr没有右孩子，为3结点时，对应情况b.1无右，此时对xpr进行右旋成b.2有右的情况，右旋后左孩子成为新的xpr
                            if (sr == null || !sr.red) {
                                if (sl != null)
                                    sl.red = false;
                                xpr.red = true;
                                root = rotateRight(root, xpr);
                                xpr = (xp = x.parent) == null ?
                                    null : xp.right;
                            }

                            // 如果xpr不为null，说明此时对应情况b.2（xpr为3结点有右）和b.3（xpr为4结点），即xpr为3、4结点的情况，此时需要对xp进行左旋，左旋后xpr成为xp的父结点，sr置黑，xp置黑，并设置x为root结点，代表调整完毕，退出循环
                            if (xpr != null) {
                                xpr.red = (xp == null) ? false : xp.red;
                                if ((sr = xpr.right) != null)
                                    sr.red = false;
                            }
                            if (xp != null) {
                                xp.red = false;
                                root = rotateLeft(root, xp);
                            }
                            x = root;// 更新x为root结点, 退出循环, 结束调整
                        }
                    }
                }

                // 同理右边情况，反向操作，如果x不为null，也不为根结点，且父结点也不为null，且x为黑色结点(删除2结点时，此时自己搞不定，需要兄弟和父亲帮忙)，且x为xp的右孩子，xpl为叔结点（上面已赋值），则按右的方式调整
                else { // symmetric
                    // 叔结点xpl为xp的左孩子，如果为红结点时，说明xpl不是x真正的兄弟结点（xpl只是为xp的红结点，此时xp为3结点)，则置黑叔结点xpl，置红父结点xp，对xp进行右旋，右旋后xpl成为xp的父结点，原xpl的右结点成为xp的左结点（这时x与x真正的兄弟结点才一一对应）
                    if (xpl != null && xpl.red) {
                        xpl.red = false;
                        xp.red = true;
                        root = rotateRight(root, xp);
                        xpl = (xp = x.parent) == null ? null : xp.left;
                    }

                    // 如果xpl为null，说明x没有叔结点，也就是兄弟没有得借，因此当x删除后，xp所在树会少一个黑结点，为了保证黑色平衡，所以把xp赋值给x，继续循环向上调整，直到碰到红结点或者根结点
                    if (xpl == null)
                        x = xp;// 可以看作xpl为红色（也相当于null），需要向上置红调整

                    // 如果xpl不为null，说明x有真正的兄弟xpl，则继续判断xpl的左孩子sl，右孩子sr，看xpl是否有得借
                    else {
                        TreeNode<K,V> sl = xpl.left, sr = xpl.right;

                        // 如果xpl没有孩子结点，则说明xpl没有得借，或者xpl的孩子结点都为黑色，说明xpl为叶子结点，也没有得借，相当于情况c.1，因此当x删除后，xp所在树会少一个黑结点，为了保证黑色平衡，所以把xp赋值给x，继续循环向上调整，直到碰到红结点或者根结点
                        if ((sl == null || !sl.red) &&
                            (sr == null || !sr.red)) {
                            xpl.red = true;
                            x = xp;
                        }

                        // 如果兄弟结点xpl有得借，则继续判断兄弟结点到底是3结点还是4结点
                        else {
                            // 如果xpl没有左孩子，为3结点时，对应情况b.1无左，此时对xpl进行左旋成b.2有左的情况，左旋后右孩子成为新的xpl
                            if (sl == null || !sl.red) {
                                if (sr != null)
                                    sr.red = false;
                                xpl.red = true;
                                root = rotateLeft(root, xpl);
                                xpl = (xp = x.parent) == null ?
                                    null : xp.left;
                            }

                            // 如果xpl不为null，说明此时对应情况b.2（xpl为3结点有左）和b.3（xpl为4结点），即xpl为3、4结点的情况，此时需要对xp进行右旋，右旋后xpl成为xp的父结点，sl置黑，xp置黑，并设置x为root结点，代表调整完毕，退出循环
                            if (xpl != null) {
                                xpl.red = (xp == null) ? false : xp.red;
                                if ((sl = xpl.left) != null)
                                    sl.red = false;
                            }
                            if (xp != null) {
                                xp.red = false;
                                root = rotateRight(root, xp);
                            }
                            x = root;// 更新x为root结点, 退出循环, 结束调整
                        }
                    }
                }
            }
        }

        /**
         * Recursive invariant check
         */
        // 递归检查指定结点是否为红黑树 => 同HashMap#checkInvariants(TreeNode)
        static <K,V> boolean checkInvariants(TreeNode<K,V> t) {
            // 待检查结点t，父结点tp, 左孩子tl, 右孩子tr, 前驱tb, 后继tn
            TreeNode<K,V> tp = t.parent, tl = t.left, tr = t.right,
                tb = t.prev, tn = (TreeNode<K,V>)t.next;

            // 待检查结点t，父结点tp, 左孩子tl, 右孩子tr, 前驱tb, 后继tn
            if (tb != null && tb.next != t)
                return false;

            // 如果后继tn不为null，但后继的prev结点不为t结点, 则返回false
            if (tn != null && tn.prev != t)
                return false;

            // 如果父结点tp不为null，但t又不是tp的左右孩子, 则返回false
            if (tp != null && t != tp.left && t != tp.right)
                return false;

            // 如果父结点tp不为null，但t又不是tp的左右孩子, 则返回false
            if (tl != null && (tl.parent != t || tl.hash > t.hash))
                return false;

            // 如果右孩子tr不为null, 且右孩子的父亲不是t或者小于父亲的hash值，则返回false（右孩子的hash值必须大于根结点的hash值）
            if (tr != null && (tr.parent != t || tr.hash < t.hash))
                return false;

            // 如果t是红节点, 且左右孩子都是红结点, 则返回false
            if (t.red && tl != null && tl.red && tr != null && tr.red)
                // bug？如果结点为红结点, 那么左右孩子结点肯定为黑结点! 节点红, 左孩子红, 右孩子黑, 也算红 黑树？估计是不会出现这种情况的，因为不符合2-3-4树结点的特点
                return false;

            // 如果左孩子tl不为叶子结点，则继续校验左子树
            if (tl != null && !checkInvariants(tl))
                return false;

            // 如果右孩子tr不为叶子结点，则继续校验右子树
            if (tr != null && !checkInvariants(tr))
                return false;

            // 如果右孩子tr不为叶子结点，则继续校验右子树
            return true;
        }

        private static final sun.misc.Unsafe U;
        private static final long LOCKSTATE;// lockState
        static {
            try {
                U = sun.misc.Unsafe.getUnsafe();
                Class<?> k = TreeBin.class;
                LOCKSTATE = U.objectFieldOffset
                    (k.getDeclaredField("lockState"));
            } catch (Exception e) {
                throw new Error(e);
            }
        }
    }

    /* ----------------Table Traversal -------------- */
    // 表遍历

    /**
     * 20210623
     * 记录表、其长度和遍历器的当前遍历索引，遍历器必须在处理当前表之前处理转发表的区域。
     */
    /**
     * Records the table, its length, and current traversal index for a
     * traverser that must process a region of a forwarded table before
     * proceeding with current table.
     */
    static final class TableStack<K,V> {
        int length;
        int index;
        Node<K,V>[] tab;
        TableStack<K,V> next;
    }

    /**
     * 20210623
     * A. 封装了containsValue等方法的遍历； 也用作其他迭代器和拆分器的基类。
     * B. 方法提前访问每个在迭代器构建时可到达的仍然有效的节点。 它可能会错过一些在访问 bin 后添加到 bin 中的内容，这是可以保证一致性的。
     *    面对可能的持续调整大小，维护此属性需要相当数量的簿记状态，而这些状态在不稳定的访问中难以优化。 即便如此，遍历仍保持合理的吞吐量。
     * C. 通常，迭代会逐个bin地遍历列表。 但是，如果表已调整大小，则所有后续步骤都必须遍历当前索引处以及 (index + baseSize) 处的 bin；
     *    等等以进一步调整大小。 为了偏执地应对跨线程迭代器用户的潜在共享，如果表读取的边界检查失败，则迭代终止。
     */
    /**
     * A.
     * Encapsulates traversal for methods such as containsValue; also
     * serves as a base class for other iterators and spliterators.
     *
     * B.
     * Method advance visits once each still-valid node that was
     * reachable upon iterator construction. It might miss some that
     * were added to a bin after the bin was visited, which is OK wrt
     * consistency guarantees. Maintaining this property in the face
     * of possible ongoing resizes requires a fair amount of
     * bookkeeping state that is difficult to optimize away amidst
     * volatile accesses.  Even so, traversal maintains reasonable
     * throughput.
     *
     * C.
     * Normally, iteration proceeds bin-by-bin traversing lists.
     * However, if the table has been resized, then all future steps
     * must traverse both the bin at the current index as well as at
     * (index + baseSize); and so on for further resizings. To
     * paranoically cope with potential sharing by users of iterators
     * across threads, iteration terminates if a bounds checks fails
     * for a table read.
     */
    static class Traverser<K,V> {
        Node<K,V>[] tab;        // current table; updated if resized 当前表；如果调整大小则更新
        Node<K,V> next;         // the next entry to use 下一个要使用的条目
        TableStack<K,V> stack, spare; // to save/restore on ForwardingNodes 在ForwardingNodes上保存/恢复
        int index;              // index of bin to use next 下一个要使用的 bin 索引
        int baseIndex;          // current index of initial table 初始表的当前索引
        int baseLimit;          // index bound for initial table 初始表的索引边界
        final int baseSize;     // initial table size 初始表大小

        Traverser(Node<K,V>[] tab, int size, int index, int limit) {
            this.tab = tab;
            this.baseSize = size;
            this.baseIndex = this.index = index;
            this.baseLimit = limit;
            this.next = null;
        }

        /**
         * Advances if possible, returning next valid node, or null if none.
         */
        // 如果可能，则前进，返回下一个有效节点，如果没有，则返回 null。
        final Node<K,V> advance() {
            Node<K,V> e;
            if ((e = next) != null)
                e = e.next;

            for (;;) {
                Node<K,V>[] t; int i, n;  // must use locals in checks

                if (e != null)
                    return next = e;

                if (baseIndex >= baseLimit || (t = tab) == null || (n = t.length) <= (i = index) || i < 0)
                    return next = null;

                if ((e = tabAt(t, i)) != null && e.hash < 0) {
                    if (e instanceof ForwardingNode) {
                        tab = ((ForwardingNode<K,V>)e).nextTable;
                        e = null;
                        pushState(t, i, n);
                        continue;
                    }
                    else if (e instanceof TreeBin)
                        e = ((TreeBin<K,V>)e).first;
                    else
                        e = null;
                }
                if (stack != null)
                    recoverState(n);
                else if ((index = i + baseSize) >= n)
                    index = ++baseIndex; // visit upper slots if present 如果存在，请访问上面的插槽
            }
        }

        /**
         * Saves traversal state upon encountering a forwarding node.
         */
        // 遇到转发节点时保存遍历状态。
        private void pushState(Node<K,V>[] t, int i, int n) {
            TableStack<K,V> s = spare;  // reuse if possible
            if (s != null)
                spare = s.next;
            else
                s = new TableStack<K,V>();
            s.tab = t;
            s.length = n;
            s.index = i;
            s.next = stack;
            stack = s;
        }

        /**
         * Possibly pops traversal state.
         *
         * @param n length of current table
         */
        // 可能会弹出遍历状态。
        private void recoverState(int n) {
            TableStack<K,V> s; int len;
            while ((s = stack) != null && (index += (len = s.length)) >= n) {
                n = len;
                index = s.index;
                tab = s.tab;
                s.tab = null;
                TableStack<K,V> next = s.next;
                s.next = spare; // save for reuse
                stack = next;
                spare = s;
            }
            if (s == null && (index += baseSize) >= n)
                index = ++baseIndex;
        }
    }

    /**
     * Base of key, value, and entry Iterators. Adds fields to
     * Traverser to support iterator.remove.
     */
    // 键、值和条目迭代器的基础。 向 Traverser 添加字段以支持 iterator.remove。
    static class BaseIterator<K,V> extends Traverser<K,V> {
        final ConcurrentHashMap<K,V> map;
        Node<K,V> lastReturned;
        BaseIterator(Node<K,V>[] tab, int size, int index, int limit,
                    ConcurrentHashMap<K,V> map) {
            super(tab, size, index, limit);
            this.map = map;
            advance();
        }

        public final boolean hasNext() { return next != null; }
        public final boolean hasMoreElements() { return next != null; }

        public final void remove() {
            Node<K,V> p;
            if ((p = lastReturned) == null)
                throw new IllegalStateException();
            lastReturned = null;
            map.replaceNode(p.key, null, null);
        }
    }

    static final class KeyIterator<K,V> extends BaseIterator<K,V> implements Iterator<K>, Enumeration<K> {
        KeyIterator(Node<K,V>[] tab, int index, int size, int limit,
                    ConcurrentHashMap<K,V> map) {
            super(tab, index, size, limit, map);
        }

        public final K next() {
            Node<K,V> p;
            if ((p = next) == null)
                throw new NoSuchElementException();
            K k = p.key;
            lastReturned = p;
            advance();
            return k;
        }

        public final K nextElement() { return next(); }
    }

    static final class ValueIterator<K,V> extends BaseIterator<K,V> implements Iterator<V>, Enumeration<V> {
        ValueIterator(Node<K,V>[] tab, int index, int size, int limit,
                      ConcurrentHashMap<K,V> map) {
            super(tab, index, size, limit, map);
        }

        public final V next() {
            Node<K,V> p;
            if ((p = next) == null)
                throw new NoSuchElementException();
            V v = p.val;
            lastReturned = p;
            advance();
            return v;
        }

        public final V nextElement() { return next(); }
    }

    static final class EntryIterator<K,V> extends BaseIterator<K,V> implements Iterator<Map.Entry<K,V>> {
        EntryIterator(Node<K,V>[] tab, int index, int size, int limit, ConcurrentHashMap<K,V> map) {
            super(tab, index, size, limit, map);
        }

        public final Map.Entry<K,V> next() {
            Node<K,V> p;
            if ((p = next) == null)
                throw new NoSuchElementException();
            K k = p.key;
            V v = p.val;
            lastReturned = p;
            advance();
            return new MapEntry<K,V>(k, v, map);
        }
    }

    /**
     * Exported Entry for EntryIterator
     */
    static final class MapEntry<K,V> implements Map.Entry<K,V> {
        final K key; // non-null
        V val;       // non-null
        final ConcurrentHashMap<K,V> map;
        MapEntry(K key, V val, ConcurrentHashMap<K,V> map) {
            this.key = key;
            this.val = val;
            this.map = map;
        }
        public K getKey()        { return key; }
        public V getValue()      { return val; }
        public int hashCode()    { return key.hashCode() ^ val.hashCode(); }
        public String toString() { return key + "=" + val; }

        public boolean equals(Object o) {
            Object k, v; Map.Entry<?,?> e;
            return ((o instanceof Map.Entry) &&
                    (k = (e = (Map.Entry<?,?>)o).getKey()) != null &&
                    (v = e.getValue()) != null &&
                    (k == key || k.equals(key)) &&
                    (v == val || v.equals(val)));
        }

        /**
         * Sets our entry's value and writes through to the map. The
         * value to return is somewhat arbitrary here. Since we do not
         * necessarily track asynchronous changes, the most recent
         * "previous" value could be different from what we return (or
         * could even have been removed, in which case the put will
         * re-establish). We do not and cannot guarantee more.
         */
        public V setValue(V value) {
            if (value == null) throw new NullPointerException();
            V v = val;
            val = value;
            map.put(key, value);
            return v;
        }
    }

    static final class KeySpliterator<K,V> extends Traverser<K,V>
        implements Spliterator<K> {
        long est;               // size estimate
        KeySpliterator(Node<K,V>[] tab, int size, int index, int limit,
                       long est) {
            super(tab, size, index, limit);
            this.est = est;
        }

        public Spliterator<K> trySplit() {
            int i, f, h;
            return (h = ((i = baseIndex) + (f = baseLimit)) >>> 1) <= i ? null :
                new KeySpliterator<K,V>(tab, baseSize, baseLimit = h,
                                        f, est >>>= 1);
        }

        public void forEachRemaining(Consumer<? super K> action) {
            if (action == null) throw new NullPointerException();
            for (Node<K,V> p; (p = advance()) != null;)
                action.accept(p.key);
        }

        public boolean tryAdvance(Consumer<? super K> action) {
            if (action == null) throw new NullPointerException();
            Node<K,V> p;
            if ((p = advance()) == null)
                return false;
            action.accept(p.key);
            return true;
        }

        public long estimateSize() { return est; }

        public int characteristics() {
            return Spliterator.DISTINCT | Spliterator.CONCURRENT |
                Spliterator.NONNULL;
        }
    }

    static final class ValueSpliterator<K,V> extends Traverser<K,V>
        implements Spliterator<V> {
        long est;               // size estimate
        ValueSpliterator(Node<K,V>[] tab, int size, int index, int limit,
                         long est) {
            super(tab, size, index, limit);
            this.est = est;
        }

        public Spliterator<V> trySplit() {
            int i, f, h;
            return (h = ((i = baseIndex) + (f = baseLimit)) >>> 1) <= i ? null :
                new ValueSpliterator<K,V>(tab, baseSize, baseLimit = h,
                                          f, est >>>= 1);
        }

        public void forEachRemaining(Consumer<? super V> action) {
            if (action == null) throw new NullPointerException();
            for (Node<K,V> p; (p = advance()) != null;)
                action.accept(p.val);
        }

        public boolean tryAdvance(Consumer<? super V> action) {
            if (action == null) throw new NullPointerException();
            Node<K,V> p;
            if ((p = advance()) == null)
                return false;
            action.accept(p.val);
            return true;
        }

        public long estimateSize() { return est; }

        public int characteristics() {
            return Spliterator.CONCURRENT | Spliterator.NONNULL;
        }
    }

    static final class EntrySpliterator<K,V> extends Traverser<K,V>
        implements Spliterator<Map.Entry<K,V>> {
        final ConcurrentHashMap<K,V> map; // To export MapEntry
        long est;               // size estimate
        EntrySpliterator(Node<K,V>[] tab, int size, int index, int limit,
                         long est, ConcurrentHashMap<K,V> map) {
            super(tab, size, index, limit);
            this.map = map;
            this.est = est;
        }

        public Spliterator<Map.Entry<K,V>> trySplit() {
            int i, f, h;
            return (h = ((i = baseIndex) + (f = baseLimit)) >>> 1) <= i ? null :
                new EntrySpliterator<K,V>(tab, baseSize, baseLimit = h,
                                          f, est >>>= 1, map);
        }

        public void forEachRemaining(Consumer<? super Map.Entry<K,V>> action) {
            if (action == null) throw new NullPointerException();
            for (Node<K,V> p; (p = advance()) != null; )
                action.accept(new MapEntry<K,V>(p.key, p.val, map));
        }

        public boolean tryAdvance(Consumer<? super Map.Entry<K,V>> action) {
            if (action == null) throw new NullPointerException();
            Node<K,V> p;
            if ((p = advance()) == null)
                return false;
            action.accept(new MapEntry<K,V>(p.key, p.val, map));
            return true;
        }

        public long estimateSize() { return est; }

        public int characteristics() {
            return Spliterator.DISTINCT | Spliterator.CONCURRENT |
                Spliterator.NONNULL;
        }
    }

    // Parallel bulk operations

    /**
     * Computes initial batch value for bulk tasks. The returned value
     * is approximately exp2 of the number of times (minus one) to
     * split task by two before executing leaf action. This value is
     * faster to compute and more convenient to use as a guide to
     * splitting than is the depth, since it is used while dividing by
     * two anyway.
     */
    final int batchFor(long b) {
        long n;
        if (b == Long.MAX_VALUE || (n = sumCount()) <= 1L || n < b)
            return 0;
        int sp = ForkJoinPool.getCommonPoolParallelism() << 2; // slack of 4
        return (b <= 0L || (n /= b) >= sp) ? sp : (int)n;
    }

    /**
     * Performs the given action for each (key, value).
     *
     * @param parallelismThreshold the (estimated) number of elements
     * needed for this operation to be executed in parallel
     * @param action the action
     * @since 1.8
     */
    public void forEach(long parallelismThreshold,
                        BiConsumer<? super K,? super V> action) {
        if (action == null) throw new NullPointerException();
        new ForEachMappingTask<K,V>
            (null, batchFor(parallelismThreshold), 0, 0, table,
             action).invoke();
    }

    /**
     * Performs the given action for each non-null transformation
     * of each (key, value).
     *
     * @param parallelismThreshold the (estimated) number of elements
     * needed for this operation to be executed in parallel
     * @param transformer a function returning the transformation
     * for an element, or null if there is no transformation (in
     * which case the action is not applied)
     * @param action the action
     * @param <U> the return type of the transformer
     * @since 1.8
     */
    public <U> void forEach(long parallelismThreshold,
                            BiFunction<? super K, ? super V, ? extends U> transformer,
                            Consumer<? super U> action) {
        if (transformer == null || action == null)
            throw new NullPointerException();
        new ForEachTransformedMappingTask<K,V,U>
            (null, batchFor(parallelismThreshold), 0, 0, table,
             transformer, action).invoke();
    }

    /**
     * Returns a non-null result from applying the given search
     * function on each (key, value), or null if none.  Upon
     * success, further element processing is suppressed and the
     * results of any other parallel invocations of the search
     * function are ignored.
     *
     * @param parallelismThreshold the (estimated) number of elements
     * needed for this operation to be executed in parallel
     * @param searchFunction a function returning a non-null
     * result on success, else null
     * @param <U> the return type of the search function
     * @return a non-null result from applying the given search
     * function on each (key, value), or null if none
     * @since 1.8
     */
    public <U> U search(long parallelismThreshold,
                        BiFunction<? super K, ? super V, ? extends U> searchFunction) {
        if (searchFunction == null) throw new NullPointerException();
        return new SearchMappingsTask<K,V,U>
            (null, batchFor(parallelismThreshold), 0, 0, table,
             searchFunction, new AtomicReference<U>()).invoke();
    }

    /**
     * Returns the result of accumulating the given transformation
     * of all (key, value) pairs using the given reducer to
     * combine values, or null if none.
     *
     * @param parallelismThreshold the (estimated) number of elements
     * needed for this operation to be executed in parallel
     * @param transformer a function returning the transformation
     * for an element, or null if there is no transformation (in
     * which case it is not combined)
     * @param reducer a commutative associative combining function
     * @param <U> the return type of the transformer
     * @return the result of accumulating the given transformation
     * of all (key, value) pairs
     * @since 1.8
     */
    public <U> U reduce(long parallelismThreshold,
                        BiFunction<? super K, ? super V, ? extends U> transformer,
                        BiFunction<? super U, ? super U, ? extends U> reducer) {
        if (transformer == null || reducer == null)
            throw new NullPointerException();
        return new MapReduceMappingsTask<K,V,U>
            (null, batchFor(parallelismThreshold), 0, 0, table,
             null, transformer, reducer).invoke();
    }

    /**
     * Returns the result of accumulating the given transformation
     * of all (key, value) pairs using the given reducer to
     * combine values, and the given basis as an identity value.
     *
     * @param parallelismThreshold the (estimated) number of elements
     * needed for this operation to be executed in parallel
     * @param transformer a function returning the transformation
     * for an element
     * @param basis the identity (initial default value) for the reduction
     * @param reducer a commutative associative combining function
     * @return the result of accumulating the given transformation
     * of all (key, value) pairs
     * @since 1.8
     */
    public double reduceToDouble(long parallelismThreshold,
                                 ToDoubleBiFunction<? super K, ? super V> transformer,
                                 double basis,
                                 DoubleBinaryOperator reducer) {
        if (transformer == null || reducer == null)
            throw new NullPointerException();
        return new MapReduceMappingsToDoubleTask<K,V>
            (null, batchFor(parallelismThreshold), 0, 0, table,
             null, transformer, basis, reducer).invoke();
    }

    /**
     * Returns the result of accumulating the given transformation
     * of all (key, value) pairs using the given reducer to
     * combine values, and the given basis as an identity value.
     *
     * @param parallelismThreshold the (estimated) number of elements
     * needed for this operation to be executed in parallel
     * @param transformer a function returning the transformation
     * for an element
     * @param basis the identity (initial default value) for the reduction
     * @param reducer a commutative associative combining function
     * @return the result of accumulating the given transformation
     * of all (key, value) pairs
     * @since 1.8
     */
    public long reduceToLong(long parallelismThreshold,
                             ToLongBiFunction<? super K, ? super V> transformer,
                             long basis,
                             LongBinaryOperator reducer) {
        if (transformer == null || reducer == null)
            throw new NullPointerException();
        return new MapReduceMappingsToLongTask<K,V>
            (null, batchFor(parallelismThreshold), 0, 0, table,
             null, transformer, basis, reducer).invoke();
    }

    /**
     * Returns the result of accumulating the given transformation
     * of all (key, value) pairs using the given reducer to
     * combine values, and the given basis as an identity value.
     *
     * @param parallelismThreshold the (estimated) number of elements
     * needed for this operation to be executed in parallel
     * @param transformer a function returning the transformation
     * for an element
     * @param basis the identity (initial default value) for the reduction
     * @param reducer a commutative associative combining function
     * @return the result of accumulating the given transformation
     * of all (key, value) pairs
     * @since 1.8
     */
    public int reduceToInt(long parallelismThreshold,
                           ToIntBiFunction<? super K, ? super V> transformer,
                           int basis,
                           IntBinaryOperator reducer) {
        if (transformer == null || reducer == null)
            throw new NullPointerException();
        return new MapReduceMappingsToIntTask<K,V>
            (null, batchFor(parallelismThreshold), 0, 0, table,
             null, transformer, basis, reducer).invoke();
    }

    /**
     * Performs the given action for each key.
     *
     * @param parallelismThreshold the (estimated) number of elements
     * needed for this operation to be executed in parallel
     * @param action the action
     * @since 1.8
     */
    public void forEachKey(long parallelismThreshold,
                           Consumer<? super K> action) {
        if (action == null) throw new NullPointerException();
        new ForEachKeyTask<K,V>
            (null, batchFor(parallelismThreshold), 0, 0, table,
             action).invoke();
    }

    /**
     * Performs the given action for each non-null transformation
     * of each key.
     *
     * @param parallelismThreshold the (estimated) number of elements
     * needed for this operation to be executed in parallel
     * @param transformer a function returning the transformation
     * for an element, or null if there is no transformation (in
     * which case the action is not applied)
     * @param action the action
     * @param <U> the return type of the transformer
     * @since 1.8
     */
    public <U> void forEachKey(long parallelismThreshold,
                               Function<? super K, ? extends U> transformer,
                               Consumer<? super U> action) {
        if (transformer == null || action == null)
            throw new NullPointerException();
        new ForEachTransformedKeyTask<K,V,U>
            (null, batchFor(parallelismThreshold), 0, 0, table,
             transformer, action).invoke();
    }

    /**
     * Returns a non-null result from applying the given search
     * function on each key, or null if none. Upon success,
     * further element processing is suppressed and the results of
     * any other parallel invocations of the search function are
     * ignored.
     *
     * @param parallelismThreshold the (estimated) number of elements
     * needed for this operation to be executed in parallel
     * @param searchFunction a function returning a non-null
     * result on success, else null
     * @param <U> the return type of the search function
     * @return a non-null result from applying the given search
     * function on each key, or null if none
     * @since 1.8
     */
    public <U> U searchKeys(long parallelismThreshold,
                            Function<? super K, ? extends U> searchFunction) {
        if (searchFunction == null) throw new NullPointerException();
        return new SearchKeysTask<K,V,U>
            (null, batchFor(parallelismThreshold), 0, 0, table,
             searchFunction, new AtomicReference<U>()).invoke();
    }

    /**
     * Returns the result of accumulating all keys using the given
     * reducer to combine values, or null if none.
     *
     * @param parallelismThreshold the (estimated) number of elements
     * needed for this operation to be executed in parallel
     * @param reducer a commutative associative combining function
     * @return the result of accumulating all keys using the given
     * reducer to combine values, or null if none
     * @since 1.8
     */
    public K reduceKeys(long parallelismThreshold,
                        BiFunction<? super K, ? super K, ? extends K> reducer) {
        if (reducer == null) throw new NullPointerException();
        return new ReduceKeysTask<K,V>
            (null, batchFor(parallelismThreshold), 0, 0, table,
             null, reducer).invoke();
    }

    /**
     * Returns the result of accumulating the given transformation
     * of all keys using the given reducer to combine values, or
     * null if none.
     *
     * @param parallelismThreshold the (estimated) number of elements
     * needed for this operation to be executed in parallel
     * @param transformer a function returning the transformation
     * for an element, or null if there is no transformation (in
     * which case it is not combined)
     * @param reducer a commutative associative combining function
     * @param <U> the return type of the transformer
     * @return the result of accumulating the given transformation
     * of all keys
     * @since 1.8
     */
    public <U> U reduceKeys(long parallelismThreshold,
                            Function<? super K, ? extends U> transformer,
         BiFunction<? super U, ? super U, ? extends U> reducer) {
        if (transformer == null || reducer == null)
            throw new NullPointerException();
        return new MapReduceKeysTask<K,V,U>
            (null, batchFor(parallelismThreshold), 0, 0, table,
             null, transformer, reducer).invoke();
    }

    /**
     * Returns the result of accumulating the given transformation
     * of all keys using the given reducer to combine values, and
     * the given basis as an identity value.
     *
     * @param parallelismThreshold the (estimated) number of elements
     * needed for this operation to be executed in parallel
     * @param transformer a function returning the transformation
     * for an element
     * @param basis the identity (initial default value) for the reduction
     * @param reducer a commutative associative combining function
     * @return the result of accumulating the given transformation
     * of all keys
     * @since 1.8
     */
    public double reduceKeysToDouble(long parallelismThreshold,
                                     ToDoubleFunction<? super K> transformer,
                                     double basis,
                                     DoubleBinaryOperator reducer) {
        if (transformer == null || reducer == null)
            throw new NullPointerException();
        return new MapReduceKeysToDoubleTask<K,V>
            (null, batchFor(parallelismThreshold), 0, 0, table,
             null, transformer, basis, reducer).invoke();
    }

    /**
     * Returns the result of accumulating the given transformation
     * of all keys using the given reducer to combine values, and
     * the given basis as an identity value.
     *
     * @param parallelismThreshold the (estimated) number of elements
     * needed for this operation to be executed in parallel
     * @param transformer a function returning the transformation
     * for an element
     * @param basis the identity (initial default value) for the reduction
     * @param reducer a commutative associative combining function
     * @return the result of accumulating the given transformation
     * of all keys
     * @since 1.8
     */
    public long reduceKeysToLong(long parallelismThreshold,
                                 ToLongFunction<? super K> transformer,
                                 long basis,
                                 LongBinaryOperator reducer) {
        if (transformer == null || reducer == null)
            throw new NullPointerException();
        return new MapReduceKeysToLongTask<K,V>
            (null, batchFor(parallelismThreshold), 0, 0, table,
             null, transformer, basis, reducer).invoke();
    }

    /**
     * Returns the result of accumulating the given transformation
     * of all keys using the given reducer to combine values, and
     * the given basis as an identity value.
     *
     * @param parallelismThreshold the (estimated) number of elements
     * needed for this operation to be executed in parallel
     * @param transformer a function returning the transformation
     * for an element
     * @param basis the identity (initial default value) for the reduction
     * @param reducer a commutative associative combining function
     * @return the result of accumulating the given transformation
     * of all keys
     * @since 1.8
     */
    public int reduceKeysToInt(long parallelismThreshold,
                               ToIntFunction<? super K> transformer,
                               int basis,
                               IntBinaryOperator reducer) {
        if (transformer == null || reducer == null)
            throw new NullPointerException();
        return new MapReduceKeysToIntTask<K,V>
            (null, batchFor(parallelismThreshold), 0, 0, table,
             null, transformer, basis, reducer).invoke();
    }

    /**
     * Performs the given action for each value.
     *
     * @param parallelismThreshold the (estimated) number of elements
     * needed for this operation to be executed in parallel
     * @param action the action
     * @since 1.8
     */
    public void forEachValue(long parallelismThreshold,
                             Consumer<? super V> action) {
        if (action == null)
            throw new NullPointerException();
        new ForEachValueTask<K,V>
            (null, batchFor(parallelismThreshold), 0, 0, table,
             action).invoke();
    }

    /**
     * Performs the given action for each non-null transformation
     * of each value.
     *
     * @param parallelismThreshold the (estimated) number of elements
     * needed for this operation to be executed in parallel
     * @param transformer a function returning the transformation
     * for an element, or null if there is no transformation (in
     * which case the action is not applied)
     * @param action the action
     * @param <U> the return type of the transformer
     * @since 1.8
     */
    public <U> void forEachValue(long parallelismThreshold,
                                 Function<? super V, ? extends U> transformer,
                                 Consumer<? super U> action) {
        if (transformer == null || action == null)
            throw new NullPointerException();
        new ForEachTransformedValueTask<K,V,U>
            (null, batchFor(parallelismThreshold), 0, 0, table,
             transformer, action).invoke();
    }

    /**
     * Returns a non-null result from applying the given search
     * function on each value, or null if none.  Upon success,
     * further element processing is suppressed and the results of
     * any other parallel invocations of the search function are
     * ignored.
     *
     * @param parallelismThreshold the (estimated) number of elements
     * needed for this operation to be executed in parallel
     * @param searchFunction a function returning a non-null
     * result on success, else null
     * @param <U> the return type of the search function
     * @return a non-null result from applying the given search
     * function on each value, or null if none
     * @since 1.8
     */
    public <U> U searchValues(long parallelismThreshold,
                              Function<? super V, ? extends U> searchFunction) {
        if (searchFunction == null) throw new NullPointerException();
        return new SearchValuesTask<K,V,U>
            (null, batchFor(parallelismThreshold), 0, 0, table,
             searchFunction, new AtomicReference<U>()).invoke();
    }

    /**
     * Returns the result of accumulating all values using the
     * given reducer to combine values, or null if none.
     *
     * @param parallelismThreshold the (estimated) number of elements
     * needed for this operation to be executed in parallel
     * @param reducer a commutative associative combining function
     * @return the result of accumulating all values
     * @since 1.8
     */
    public V reduceValues(long parallelismThreshold,
                          BiFunction<? super V, ? super V, ? extends V> reducer) {
        if (reducer == null) throw new NullPointerException();
        return new ReduceValuesTask<K,V>
            (null, batchFor(parallelismThreshold), 0, 0, table,
             null, reducer).invoke();
    }

    /**
     * Returns the result of accumulating the given transformation
     * of all values using the given reducer to combine values, or
     * null if none.
     *
     * @param parallelismThreshold the (estimated) number of elements
     * needed for this operation to be executed in parallel
     * @param transformer a function returning the transformation
     * for an element, or null if there is no transformation (in
     * which case it is not combined)
     * @param reducer a commutative associative combining function
     * @param <U> the return type of the transformer
     * @return the result of accumulating the given transformation
     * of all values
     * @since 1.8
     */
    public <U> U reduceValues(long parallelismThreshold,
                              Function<? super V, ? extends U> transformer,
                              BiFunction<? super U, ? super U, ? extends U> reducer) {
        if (transformer == null || reducer == null)
            throw new NullPointerException();
        return new MapReduceValuesTask<K,V,U>
            (null, batchFor(parallelismThreshold), 0, 0, table,
             null, transformer, reducer).invoke();
    }

    /**
     * Returns the result of accumulating the given transformation
     * of all values using the given reducer to combine values,
     * and the given basis as an identity value.
     *
     * @param parallelismThreshold the (estimated) number of elements
     * needed for this operation to be executed in parallel
     * @param transformer a function returning the transformation
     * for an element
     * @param basis the identity (initial default value) for the reduction
     * @param reducer a commutative associative combining function
     * @return the result of accumulating the given transformation
     * of all values
     * @since 1.8
     */
    public double reduceValuesToDouble(long parallelismThreshold,
                                       ToDoubleFunction<? super V> transformer,
                                       double basis,
                                       DoubleBinaryOperator reducer) {
        if (transformer == null || reducer == null)
            throw new NullPointerException();
        return new MapReduceValuesToDoubleTask<K,V>
            (null, batchFor(parallelismThreshold), 0, 0, table,
             null, transformer, basis, reducer).invoke();
    }

    /**
     * Returns the result of accumulating the given transformation
     * of all values using the given reducer to combine values,
     * and the given basis as an identity value.
     *
     * @param parallelismThreshold the (estimated) number of elements
     * needed for this operation to be executed in parallel
     * @param transformer a function returning the transformation
     * for an element
     * @param basis the identity (initial default value) for the reduction
     * @param reducer a commutative associative combining function
     * @return the result of accumulating the given transformation
     * of all values
     * @since 1.8
     */
    public long reduceValuesToLong(long parallelismThreshold,
                                   ToLongFunction<? super V> transformer,
                                   long basis,
                                   LongBinaryOperator reducer) {
        if (transformer == null || reducer == null)
            throw new NullPointerException();
        return new MapReduceValuesToLongTask<K,V>
            (null, batchFor(parallelismThreshold), 0, 0, table,
             null, transformer, basis, reducer).invoke();
    }

    /**
     * Returns the result of accumulating the given transformation
     * of all values using the given reducer to combine values,
     * and the given basis as an identity value.
     *
     * @param parallelismThreshold the (estimated) number of elements
     * needed for this operation to be executed in parallel
     * @param transformer a function returning the transformation
     * for an element
     * @param basis the identity (initial default value) for the reduction
     * @param reducer a commutative associative combining function
     * @return the result of accumulating the given transformation
     * of all values
     * @since 1.8
     */
    public int reduceValuesToInt(long parallelismThreshold,
                                 ToIntFunction<? super V> transformer,
                                 int basis,
                                 IntBinaryOperator reducer) {
        if (transformer == null || reducer == null)
            throw new NullPointerException();
        return new MapReduceValuesToIntTask<K,V>
            (null, batchFor(parallelismThreshold), 0, 0, table,
             null, transformer, basis, reducer).invoke();
    }

    /**
     * Performs the given action for each entry.
     *
     * @param parallelismThreshold the (estimated) number of elements
     * needed for this operation to be executed in parallel
     * @param action the action
     * @since 1.8
     */
    public void forEachEntry(long parallelismThreshold,
                             Consumer<? super Map.Entry<K,V>> action) {
        if (action == null) throw new NullPointerException();
        new ForEachEntryTask<K,V>(null, batchFor(parallelismThreshold), 0, 0, table,
                                  action).invoke();
    }

    /**
     * Performs the given action for each non-null transformation
     * of each entry.
     *
     * @param parallelismThreshold the (estimated) number of elements
     * needed for this operation to be executed in parallel
     * @param transformer a function returning the transformation
     * for an element, or null if there is no transformation (in
     * which case the action is not applied)
     * @param action the action
     * @param <U> the return type of the transformer
     * @since 1.8
     */
    public <U> void forEachEntry(long parallelismThreshold,
                                 Function<Map.Entry<K,V>, ? extends U> transformer,
                                 Consumer<? super U> action) {
        if (transformer == null || action == null)
            throw new NullPointerException();
        new ForEachTransformedEntryTask<K,V,U>
            (null, batchFor(parallelismThreshold), 0, 0, table,
             transformer, action).invoke();
    }

    /**
     * Returns a non-null result from applying the given search
     * function on each entry, or null if none.  Upon success,
     * further element processing is suppressed and the results of
     * any other parallel invocations of the search function are
     * ignored.
     *
     * @param parallelismThreshold the (estimated) number of elements
     * needed for this operation to be executed in parallel
     * @param searchFunction a function returning a non-null
     * result on success, else null
     * @param <U> the return type of the search function
     * @return a non-null result from applying the given search
     * function on each entry, or null if none
     * @since 1.8
     */
    public <U> U searchEntries(long parallelismThreshold,
                               Function<Map.Entry<K,V>, ? extends U> searchFunction) {
        if (searchFunction == null) throw new NullPointerException();
        return new SearchEntriesTask<K,V,U>
            (null, batchFor(parallelismThreshold), 0, 0, table,
             searchFunction, new AtomicReference<U>()).invoke();
    }

    /**
     * Returns the result of accumulating all entries using the
     * given reducer to combine values, or null if none.
     *
     * @param parallelismThreshold the (estimated) number of elements
     * needed for this operation to be executed in parallel
     * @param reducer a commutative associative combining function
     * @return the result of accumulating all entries
     * @since 1.8
     */
    public Map.Entry<K,V> reduceEntries(long parallelismThreshold,
                                        BiFunction<Map.Entry<K,V>, Map.Entry<K,V>, ? extends Map.Entry<K,V>> reducer) {
        if (reducer == null) throw new NullPointerException();
        return new ReduceEntriesTask<K,V>
            (null, batchFor(parallelismThreshold), 0, 0, table,
             null, reducer).invoke();
    }

    /**
     * Returns the result of accumulating the given transformation
     * of all entries using the given reducer to combine values,
     * or null if none.
     *
     * @param parallelismThreshold the (estimated) number of elements
     * needed for this operation to be executed in parallel
     * @param transformer a function returning the transformation
     * for an element, or null if there is no transformation (in
     * which case it is not combined)
     * @param reducer a commutative associative combining function
     * @param <U> the return type of the transformer
     * @return the result of accumulating the given transformation
     * of all entries
     * @since 1.8
     */
    public <U> U reduceEntries(long parallelismThreshold,
                               Function<Map.Entry<K,V>, ? extends U> transformer,
                               BiFunction<? super U, ? super U, ? extends U> reducer) {
        if (transformer == null || reducer == null)
            throw new NullPointerException();
        return new MapReduceEntriesTask<K,V,U>
            (null, batchFor(parallelismThreshold), 0, 0, table,
             null, transformer, reducer).invoke();
    }

    /**
     * Returns the result of accumulating the given transformation
     * of all entries using the given reducer to combine values,
     * and the given basis as an identity value.
     *
     * @param parallelismThreshold the (estimated) number of elements
     * needed for this operation to be executed in parallel
     * @param transformer a function returning the transformation
     * for an element
     * @param basis the identity (initial default value) for the reduction
     * @param reducer a commutative associative combining function
     * @return the result of accumulating the given transformation
     * of all entries
     * @since 1.8
     */
    public double reduceEntriesToDouble(long parallelismThreshold,
                                        ToDoubleFunction<Map.Entry<K,V>> transformer,
                                        double basis,
                                        DoubleBinaryOperator reducer) {
        if (transformer == null || reducer == null)
            throw new NullPointerException();
        return new MapReduceEntriesToDoubleTask<K,V>
            (null, batchFor(parallelismThreshold), 0, 0, table,
             null, transformer, basis, reducer).invoke();
    }

    /**
     * Returns the result of accumulating the given transformation
     * of all entries using the given reducer to combine values,
     * and the given basis as an identity value.
     *
     * @param parallelismThreshold the (estimated) number of elements
     * needed for this operation to be executed in parallel
     * @param transformer a function returning the transformation
     * for an element
     * @param basis the identity (initial default value) for the reduction
     * @param reducer a commutative associative combining function
     * @return the result of accumulating the given transformation
     * of all entries
     * @since 1.8
     */
    public long reduceEntriesToLong(long parallelismThreshold,
                                    ToLongFunction<Map.Entry<K,V>> transformer,
                                    long basis,
                                    LongBinaryOperator reducer) {
        if (transformer == null || reducer == null)
            throw new NullPointerException();
        return new MapReduceEntriesToLongTask<K,V>
            (null, batchFor(parallelismThreshold), 0, 0, table,
             null, transformer, basis, reducer).invoke();
    }

    /**
     * Returns the result of accumulating the given transformation
     * of all entries using the given reducer to combine values,
     * and the given basis as an identity value.
     *
     * @param parallelismThreshold the (estimated) number of elements
     * needed for this operation to be executed in parallel
     * @param transformer a function returning the transformation
     * for an element
     * @param basis the identity (initial default value) for the reduction
     * @param reducer a commutative associative combining function
     * @return the result of accumulating the given transformation
     * of all entries
     * @since 1.8
     */
    public int reduceEntriesToInt(long parallelismThreshold,
                                  ToIntFunction<Map.Entry<K,V>> transformer,
                                  int basis,
                                  IntBinaryOperator reducer) {
        if (transformer == null || reducer == null)
            throw new NullPointerException();
        return new MapReduceEntriesToIntTask<K,V>
            (null, batchFor(parallelismThreshold), 0, 0, table,
             null, transformer, basis, reducer).invoke();
    }


    /* ----------------Views -------------- */

    /**
     * Base class for views.
     */
    abstract static class CollectionView<K,V,E>
        implements Collection<E>, java.io.Serializable {
        private static final long serialVersionUID = 7249069246763182397L;
        final ConcurrentHashMap<K,V> map;
        CollectionView(ConcurrentHashMap<K,V> map)  { this.map = map; }

        /**
         * Returns the map backing this view.
         *
         * @return the map backing this view
         */
        public ConcurrentHashMap<K,V> getMap() { return map; }

        /**
         * Removes all of the elements from this view, by removing all
         * the mappings from the map backing this view.
         */
        public final void clear()      { map.clear(); }
        public final int size()        { return map.size(); }
        public final boolean isEmpty() { return map.isEmpty(); }

        // implementations below rely on concrete classes supplying these
        // abstract methods
        /**
         * Returns an iterator over the elements in this collection.
         *
         * <p>The returned iterator is
         * <a href="package-summary.html#Weakly"><i>weakly consistent</i></a>.
         *
         * @return an iterator over the elements in this collection
         */
        public abstract Iterator<E> iterator();
        public abstract boolean contains(Object o);
        public abstract boolean remove(Object o);

        private static final String oomeMsg = "Required array size too large";

        public final Object[] toArray() {
            long sz = map.mappingCount();
            if (sz > MAX_ARRAY_SIZE)
                throw new OutOfMemoryError(oomeMsg);
            int n = (int)sz;
            Object[] r = new Object[n];
            int i = 0;
            for (E e : this) {
                if (i == n) {
                    if (n >= MAX_ARRAY_SIZE)
                        throw new OutOfMemoryError(oomeMsg);
                    if (n >= MAX_ARRAY_SIZE - (MAX_ARRAY_SIZE >>> 1) - 1)
                        n = MAX_ARRAY_SIZE;
                    else
                        n += (n >>> 1) + 1;
                    r = Arrays.copyOf(r, n);
                }
                r[i++] = e;
            }
            return (i == n) ? r : Arrays.copyOf(r, i);
        }

        @SuppressWarnings("unchecked")
        public final <T> T[] toArray(T[] a) {
            long sz = map.mappingCount();
            if (sz > MAX_ARRAY_SIZE)
                throw new OutOfMemoryError(oomeMsg);
            int m = (int)sz;
            T[] r = (a.length >= m) ? a :
                (T[])java.lang.reflect.Array
                .newInstance(a.getClass().getComponentType(), m);
            int n = r.length;
            int i = 0;
            for (E e : this) {
                if (i == n) {
                    if (n >= MAX_ARRAY_SIZE)
                        throw new OutOfMemoryError(oomeMsg);
                    if (n >= MAX_ARRAY_SIZE - (MAX_ARRAY_SIZE >>> 1) - 1)
                        n = MAX_ARRAY_SIZE;
                    else
                        n += (n >>> 1) + 1;
                    r = Arrays.copyOf(r, n);
                }
                r[i++] = (T)e;
            }
            if (a == r && i < n) {
                r[i] = null; // null-terminate
                return r;
            }
            return (i == n) ? r : Arrays.copyOf(r, i);
        }

        /**
         * Returns a string representation of this collection.
         * The string representation consists of the string representations
         * of the collection's elements in the order they are returned by
         * its iterator, enclosed in square brackets ({@code "[]"}).
         * Adjacent elements are separated by the characters {@code ", "}
         * (comma and space).  Elements are converted to strings as by
         * {@link String#valueOf(Object)}.
         *
         * @return a string representation of this collection
         */
        public final String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append('[');
            Iterator<E> it = iterator();
            if (it.hasNext()) {
                for (;;) {
                    Object e = it.next();
                    sb.append(e == this ? "(this Collection)" : e);
                    if (!it.hasNext())
                        break;
                    sb.append(',').append(' ');
                }
            }
            return sb.append(']').toString();
        }

        public final boolean containsAll(Collection<?> c) {
            if (c != this) {
                for (Object e : c) {
                    if (e == null || !contains(e))
                        return false;
                }
            }
            return true;
        }

        public final boolean removeAll(Collection<?> c) {
            if (c == null) throw new NullPointerException();
            boolean modified = false;
            for (Iterator<E> it = iterator(); it.hasNext();) {
                if (c.contains(it.next())) {
                    it.remove();
                    modified = true;
                }
            }
            return modified;
        }

        public final boolean retainAll(Collection<?> c) {
            if (c == null) throw new NullPointerException();
            boolean modified = false;
            for (Iterator<E> it = iterator(); it.hasNext();) {
                if (!c.contains(it.next())) {
                    it.remove();
                    modified = true;
                }
            }
            return modified;
        }

    }

    /**
     * A view of a ConcurrentHashMap as a {@link Set} of keys, in
     * which additions may optionally be enabled by mapping to a
     * common value.  This class cannot be directly instantiated.
     * See {@link #keySet() keySet()},
     * {@link #keySet(Object) keySet(V)},
     * {@link #newKeySet() newKeySet()},
     * {@link #newKeySet(int) newKeySet(int)}.
     *
     * @since 1.8
     */
    public static class KeySetView<K,V> extends CollectionView<K,V,K>
        implements Set<K>, java.io.Serializable {
        private static final long serialVersionUID = 7249069246763182397L;
        private final V value;
        KeySetView(ConcurrentHashMap<K,V> map, V value) {  // non-public
            super(map);
            this.value = value;
        }

        /**
         * Returns the default mapped value for additions,
         * or {@code null} if additions are not supported.
         *
         * @return the default mapped value for additions, or {@code null}
         * if not supported
         */
        public V getMappedValue() { return value; }

        /**
         * {@inheritDoc}
         * @throws NullPointerException if the specified key is null
         */
        public boolean contains(Object o) { return map.containsKey(o); }

        /**
         * Removes the key from this map view, by removing the key (and its
         * corresponding value) from the backing map.  This method does
         * nothing if the key is not in the map.
         *
         * @param  o the key to be removed from the backing map
         * @return {@code true} if the backing map contained the specified key
         * @throws NullPointerException if the specified key is null
         */
        public boolean remove(Object o) { return map.remove(o) != null; }

        /**
         * @return an iterator over the keys of the backing map
         */
        public Iterator<K> iterator() {
            Node<K,V>[] t;
            ConcurrentHashMap<K,V> m = map;
            int f = (t = m.table) == null ? 0 : t.length;
            return new KeyIterator<K,V>(t, f, 0, f, m);
        }

        /**
         * Adds the specified key to this set view by mapping the key to
         * the default mapped value in the backing map, if defined.
         *
         * @param e key to be added
         * @return {@code true} if this set changed as a result of the call
         * @throws NullPointerException if the specified key is null
         * @throws UnsupportedOperationException if no default mapped value
         * for additions was provided
         */
        public boolean add(K e) {
            V v;
            if ((v = value) == null)
                throw new UnsupportedOperationException();
            return map.putVal(e, v, true) == null;
        }

        /**
         * Adds all of the elements in the specified collection to this set,
         * as if by calling {@link #add} on each one.
         *
         * @param c the elements to be inserted into this set
         * @return {@code true} if this set changed as a result of the call
         * @throws NullPointerException if the collection or any of its
         * elements are {@code null}
         * @throws UnsupportedOperationException if no default mapped value
         * for additions was provided
         */
        public boolean addAll(Collection<? extends K> c) {
            boolean added = false;
            V v;
            if ((v = value) == null)
                throw new UnsupportedOperationException();
            for (K e : c) {
                if (map.putVal(e, v, true) == null)
                    added = true;
            }
            return added;
        }

        public int hashCode() {
            int h = 0;
            for (K e : this)
                h += e.hashCode();
            return h;
        }

        public boolean equals(Object o) {
            Set<?> c;
            return ((o instanceof Set) &&
                    ((c = (Set<?>)o) == this ||
                     (containsAll(c) && c.containsAll(this))));
        }

        public Spliterator<K> spliterator() {
            Node<K,V>[] t;
            ConcurrentHashMap<K,V> m = map;
            long n = m.sumCount();
            int f = (t = m.table) == null ? 0 : t.length;
            return new KeySpliterator<K,V>(t, f, 0, f, n < 0L ? 0L : n);
        }

        public void forEach(Consumer<? super K> action) {
            if (action == null) throw new NullPointerException();
            Node<K,V>[] t;
            if ((t = map.table) != null) {
                Traverser<K,V> it = new Traverser<K,V>(t, t.length, 0, t.length);
                for (Node<K,V> p; (p = it.advance()) != null; )
                    action.accept(p.key);
            }
        }
    }

    /**
     * A view of a ConcurrentHashMap as a {@link Collection} of
     * values, in which additions are disabled. This class cannot be
     * directly instantiated. See {@link #values()}.
     */
    static final class ValuesView<K,V> extends CollectionView<K,V,V>
        implements Collection<V>, java.io.Serializable {
        private static final long serialVersionUID = 2249069246763182397L;
        ValuesView(ConcurrentHashMap<K,V> map) { super(map); }
        public final boolean contains(Object o) {
            return map.containsValue(o);
        }

        public final boolean remove(Object o) {
            if (o != null) {
                for (Iterator<V> it = iterator(); it.hasNext();) {
                    if (o.equals(it.next())) {
                        it.remove();
                        return true;
                    }
                }
            }
            return false;
        }

        public final Iterator<V> iterator() {
            ConcurrentHashMap<K,V> m = map;
            Node<K,V>[] t;
            int f = (t = m.table) == null ? 0 : t.length;
            return new ValueIterator<K,V>(t, f, 0, f, m);
        }

        public final boolean add(V e) {
            throw new UnsupportedOperationException();
        }
        public final boolean addAll(Collection<? extends V> c) {
            throw new UnsupportedOperationException();
        }

        public Spliterator<V> spliterator() {
            Node<K,V>[] t;
            ConcurrentHashMap<K,V> m = map;
            long n = m.sumCount();
            int f = (t = m.table) == null ? 0 : t.length;
            return new ValueSpliterator<K,V>(t, f, 0, f, n < 0L ? 0L : n);
        }

        public void forEach(Consumer<? super V> action) {
            if (action == null) throw new NullPointerException();
            Node<K,V>[] t;
            if ((t = map.table) != null) {
                Traverser<K,V> it = new Traverser<K,V>(t, t.length, 0, t.length);
                for (Node<K,V> p; (p = it.advance()) != null; )
                    action.accept(p.val);
            }
        }
    }

    /**
     * A view of a ConcurrentHashMap as a {@link Set} of (key, value)
     * entries.  This class cannot be directly instantiated. See
     * {@link #entrySet()}.
     */
    static final class EntrySetView<K,V> extends CollectionView<K,V,Map.Entry<K,V>>
        implements Set<Map.Entry<K,V>>, java.io.Serializable {
        private static final long serialVersionUID = 2249069246763182397L;
        EntrySetView(ConcurrentHashMap<K,V> map) { super(map); }

        public boolean contains(Object o) {
            Object k, v, r; Map.Entry<?,?> e;
            return ((o instanceof Map.Entry) &&
                    (k = (e = (Map.Entry<?,?>)o).getKey()) != null &&
                    (r = map.get(k)) != null &&
                    (v = e.getValue()) != null &&
                    (v == r || v.equals(r)));
        }

        public boolean remove(Object o) {
            Object k, v; Map.Entry<?,?> e;
            return ((o instanceof Map.Entry) &&
                    (k = (e = (Map.Entry<?,?>)o).getKey()) != null &&
                    (v = e.getValue()) != null &&
                    map.remove(k, v));
        }

        /**
         * @return an iterator over the entries of the backing map
         */
        public Iterator<Map.Entry<K,V>> iterator() {
            ConcurrentHashMap<K,V> m = map;
            Node<K,V>[] t;
            int f = (t = m.table) == null ? 0 : t.length;
            return new EntryIterator<K,V>(t, f, 0, f, m);
        }

        public boolean add(Entry<K,V> e) {
            return map.putVal(e.getKey(), e.getValue(), false) == null;
        }

        public boolean addAll(Collection<? extends Entry<K,V>> c) {
            boolean added = false;
            for (Entry<K,V> e : c) {
                if (add(e))
                    added = true;
            }
            return added;
        }

        public final int hashCode() {
            int h = 0;
            Node<K,V>[] t;
            if ((t = map.table) != null) {
                Traverser<K,V> it = new Traverser<K,V>(t, t.length, 0, t.length);
                for (Node<K,V> p; (p = it.advance()) != null; ) {
                    h += p.hashCode();
                }
            }
            return h;
        }

        public final boolean equals(Object o) {
            Set<?> c;
            return ((o instanceof Set) &&
                    ((c = (Set<?>)o) == this ||
                     (containsAll(c) && c.containsAll(this))));
        }

        public Spliterator<Map.Entry<K,V>> spliterator() {
            Node<K,V>[] t;
            ConcurrentHashMap<K,V> m = map;
            long n = m.sumCount();
            int f = (t = m.table) == null ? 0 : t.length;
            return new EntrySpliterator<K,V>(t, f, 0, f, n < 0L ? 0L : n, m);
        }

        public void forEach(Consumer<? super Map.Entry<K,V>> action) {
            if (action == null) throw new NullPointerException();
            Node<K,V>[] t;
            if ((t = map.table) != null) {
                Traverser<K,V> it = new Traverser<K,V>(t, t.length, 0, t.length);
                for (Node<K,V> p; (p = it.advance()) != null; )
                    action.accept(new MapEntry<K,V>(p.key, p.val, map));
            }
        }

    }

    // -------------------------------------------------------

    /**
     * Base class for bulk tasks. Repeats some fields and code from
     * class Traverser, because we need to subclass CountedCompleter.
     */
    // 批量任务的基类。 重复 Traverser 类中的一些字段和代码，因为我们需要继承 CountedCompleter。
    @SuppressWarnings("serial")
    abstract static class BulkTask<K,V,R> extends CountedCompleter<R> {
        Node<K,V>[] tab;        // same as Traverser
        Node<K,V> next;
        TableStack<K,V> stack, spare;
        int index;
        int baseIndex;
        int baseLimit;
        final int baseSize;
        int batch;              // split control

        BulkTask(BulkTask<K,V,?> par, int b, int i, int f, Node<K,V>[] t) {
            super(par);
            this.batch = b;
            this.index = this.baseIndex = i;
            if ((this.tab = t) == null)
                this.baseSize = this.baseLimit = 0;
            else if (par == null)
                this.baseSize = this.baseLimit = t.length;
            else {
                this.baseLimit = f;
                this.baseSize = par.baseSize;
            }
        }

        /**
         * Same as Traverser version
         */
        final Node<K,V> advance() {
            Node<K,V> e;
            if ((e = next) != null)
                e = e.next;
            for (;;) {
                Node<K,V>[] t; int i, n;
                if (e != null)
                    return next = e;
                if (baseIndex >= baseLimit || (t = tab) == null ||
                    (n = t.length) <= (i = index) || i < 0)
                    return next = null;
                if ((e = tabAt(t, i)) != null && e.hash < 0) {
                    if (e instanceof ForwardingNode) {
                        tab = ((ForwardingNode<K,V>)e).nextTable;
                        e = null;
                        pushState(t, i, n);
                        continue;
                    }
                    else if (e instanceof TreeBin)
                        e = ((TreeBin<K,V>)e).first;
                    else
                        e = null;
                }
                if (stack != null)
                    recoverState(n);
                else if ((index = i + baseSize) >= n)
                    index = ++baseIndex;
            }
        }

        private void pushState(Node<K,V>[] t, int i, int n) {
            TableStack<K,V> s = spare;
            if (s != null)
                spare = s.next;
            else
                s = new TableStack<K,V>();
            s.tab = t;
            s.length = n;
            s.index = i;
            s.next = stack;
            stack = s;
        }

        private void recoverState(int n) {
            TableStack<K,V> s; int len;
            while ((s = stack) != null && (index += (len = s.length)) >= n) {
                n = len;
                index = s.index;
                tab = s.tab;
                s.tab = null;
                TableStack<K,V> next = s.next;
                s.next = spare; // save for reuse
                stack = next;
                spare = s;
            }
            if (s == null && (index += baseSize) >= n)
                index = ++baseIndex;
        }
    }

    /**
     * 20210627
     * 任务类。 以常规但丑陋的格式/风格编码，以简化检查每个变体与其他变体以正确方式不同的方式。 存在空筛选是因为编译器无法判断我们已经对任务参数进行了空检查，
     * 因此我们强制使用最简单的提升绕过来帮助避免复杂的陷阱。
     */
    /*
     * Task classes. Coded in a regular but ugly format/style to
     * simplify checks that each variant differs in the right way from
     * others. The null screenings exist because compilers cannot tell
     * that we've already null-checked task arguments, so we force
     * simplest hoisted bypass to help avoid convoluted traps.
     */
    @SuppressWarnings("serial")
    static final class ForEachKeyTask<K,V>
        extends BulkTask<K,V,Void> {
        final Consumer<? super K> action;
        ForEachKeyTask
            (BulkTask<K,V,?> p, int b, int i, int f, Node<K,V>[] t,
             Consumer<? super K> action) {
            super(p, b, i, f, t);
            this.action = action;
        }
        public final void compute() {
            final Consumer<? super K> action;
            if ((action = this.action) != null) {
                for (int i = baseIndex, f, h; batch > 0 &&
                         (h = ((f = baseLimit) + i) >>> 1) > i;) {
                    addToPendingCount(1);
                    new ForEachKeyTask<K,V>
                        (this, batch >>>= 1, baseLimit = h, f, tab,
                         action).fork();
                }
                for (Node<K,V> p; (p = advance()) != null;)
                    action.accept(p.key);
                propagateCompletion();
            }
        }
    }

    @SuppressWarnings("serial")
    static final class ForEachValueTask<K,V>
        extends BulkTask<K,V,Void> {
        final Consumer<? super V> action;
        ForEachValueTask
            (BulkTask<K,V,?> p, int b, int i, int f, Node<K,V>[] t,
             Consumer<? super V> action) {
            super(p, b, i, f, t);
            this.action = action;
        }
        public final void compute() {
            final Consumer<? super V> action;
            if ((action = this.action) != null) {
                for (int i = baseIndex, f, h; batch > 0 &&
                         (h = ((f = baseLimit) + i) >>> 1) > i;) {
                    addToPendingCount(1);
                    new ForEachValueTask<K,V>
                        (this, batch >>>= 1, baseLimit = h, f, tab,
                         action).fork();
                }
                for (Node<K,V> p; (p = advance()) != null;)
                    action.accept(p.val);
                propagateCompletion();
            }
        }
    }

    @SuppressWarnings("serial")
    static final class ForEachEntryTask<K,V>
        extends BulkTask<K,V,Void> {
        final Consumer<? super Entry<K,V>> action;
        ForEachEntryTask
            (BulkTask<K,V,?> p, int b, int i, int f, Node<K,V>[] t,
             Consumer<? super Entry<K,V>> action) {
            super(p, b, i, f, t);
            this.action = action;
        }
        public final void compute() {
            final Consumer<? super Entry<K,V>> action;
            if ((action = this.action) != null) {
                for (int i = baseIndex, f, h; batch > 0 &&
                         (h = ((f = baseLimit) + i) >>> 1) > i;) {
                    addToPendingCount(1);
                    new ForEachEntryTask<K,V>
                        (this, batch >>>= 1, baseLimit = h, f, tab,
                         action).fork();
                }
                for (Node<K,V> p; (p = advance()) != null; )
                    action.accept(p);
                propagateCompletion();
            }
        }
    }

    @SuppressWarnings("serial")
    static final class ForEachMappingTask<K,V>
        extends BulkTask<K,V,Void> {
        final BiConsumer<? super K, ? super V> action;
        ForEachMappingTask
            (BulkTask<K,V,?> p, int b, int i, int f, Node<K,V>[] t,
             BiConsumer<? super K,? super V> action) {
            super(p, b, i, f, t);
            this.action = action;
        }
        public final void compute() {
            final BiConsumer<? super K, ? super V> action;
            if ((action = this.action) != null) {
                for (int i = baseIndex, f, h; batch > 0 &&
                         (h = ((f = baseLimit) + i) >>> 1) > i;) {
                    addToPendingCount(1);
                    new ForEachMappingTask<K,V>
                        (this, batch >>>= 1, baseLimit = h, f, tab,
                         action).fork();
                }
                for (Node<K,V> p; (p = advance()) != null; )
                    action.accept(p.key, p.val);
                propagateCompletion();
            }
        }
    }

    @SuppressWarnings("serial")
    static final class ForEachTransformedKeyTask<K,V,U>
        extends BulkTask<K,V,Void> {
        final Function<? super K, ? extends U> transformer;
        final Consumer<? super U> action;
        ForEachTransformedKeyTask
            (BulkTask<K,V,?> p, int b, int i, int f, Node<K,V>[] t,
             Function<? super K, ? extends U> transformer, Consumer<? super U> action) {
            super(p, b, i, f, t);
            this.transformer = transformer; this.action = action;
        }
        public final void compute() {
            final Function<? super K, ? extends U> transformer;
            final Consumer<? super U> action;
            if ((transformer = this.transformer) != null &&
                (action = this.action) != null) {
                for (int i = baseIndex, f, h; batch > 0 &&
                         (h = ((f = baseLimit) + i) >>> 1) > i;) {
                    addToPendingCount(1);
                    new ForEachTransformedKeyTask<K,V,U>
                        (this, batch >>>= 1, baseLimit = h, f, tab,
                         transformer, action).fork();
                }
                for (Node<K,V> p; (p = advance()) != null; ) {
                    U u;
                    if ((u = transformer.apply(p.key)) != null)
                        action.accept(u);
                }
                propagateCompletion();
            }
        }
    }

    @SuppressWarnings("serial")
    static final class ForEachTransformedValueTask<K,V,U>
        extends BulkTask<K,V,Void> {
        final Function<? super V, ? extends U> transformer;
        final Consumer<? super U> action;
        ForEachTransformedValueTask
            (BulkTask<K,V,?> p, int b, int i, int f, Node<K,V>[] t,
             Function<? super V, ? extends U> transformer, Consumer<? super U> action) {
            super(p, b, i, f, t);
            this.transformer = transformer; this.action = action;
        }
        public final void compute() {
            final Function<? super V, ? extends U> transformer;
            final Consumer<? super U> action;
            if ((transformer = this.transformer) != null &&
                (action = this.action) != null) {
                for (int i = baseIndex, f, h; batch > 0 &&
                         (h = ((f = baseLimit) + i) >>> 1) > i;) {
                    addToPendingCount(1);
                    new ForEachTransformedValueTask<K,V,U>
                        (this, batch >>>= 1, baseLimit = h, f, tab,
                         transformer, action).fork();
                }
                for (Node<K,V> p; (p = advance()) != null; ) {
                    U u;
                    if ((u = transformer.apply(p.val)) != null)
                        action.accept(u);
                }
                propagateCompletion();
            }
        }
    }

    @SuppressWarnings("serial")
    static final class ForEachTransformedEntryTask<K,V,U>
        extends BulkTask<K,V,Void> {
        final Function<Map.Entry<K,V>, ? extends U> transformer;
        final Consumer<? super U> action;
        ForEachTransformedEntryTask
            (BulkTask<K,V,?> p, int b, int i, int f, Node<K,V>[] t,
             Function<Map.Entry<K,V>, ? extends U> transformer, Consumer<? super U> action) {
            super(p, b, i, f, t);
            this.transformer = transformer; this.action = action;
        }
        public final void compute() {
            final Function<Map.Entry<K,V>, ? extends U> transformer;
            final Consumer<? super U> action;
            if ((transformer = this.transformer) != null &&
                (action = this.action) != null) {
                for (int i = baseIndex, f, h; batch > 0 &&
                         (h = ((f = baseLimit) + i) >>> 1) > i;) {
                    addToPendingCount(1);
                    new ForEachTransformedEntryTask<K,V,U>
                        (this, batch >>>= 1, baseLimit = h, f, tab,
                         transformer, action).fork();
                }
                for (Node<K,V> p; (p = advance()) != null; ) {
                    U u;
                    if ((u = transformer.apply(p)) != null)
                        action.accept(u);
                }
                propagateCompletion();
            }
        }
    }

    @SuppressWarnings("serial")
    static final class ForEachTransformedMappingTask<K,V,U>
        extends BulkTask<K,V,Void> {
        final BiFunction<? super K, ? super V, ? extends U> transformer;
        final Consumer<? super U> action;
        ForEachTransformedMappingTask
            (BulkTask<K,V,?> p, int b, int i, int f, Node<K,V>[] t,
             BiFunction<? super K, ? super V, ? extends U> transformer,
             Consumer<? super U> action) {
            super(p, b, i, f, t);
            this.transformer = transformer; this.action = action;
        }
        public final void compute() {
            final BiFunction<? super K, ? super V, ? extends U> transformer;
            final Consumer<? super U> action;
            if ((transformer = this.transformer) != null &&
                (action = this.action) != null) {
                for (int i = baseIndex, f, h; batch > 0 &&
                         (h = ((f = baseLimit) + i) >>> 1) > i;) {
                    addToPendingCount(1);
                    new ForEachTransformedMappingTask<K,V,U>
                        (this, batch >>>= 1, baseLimit = h, f, tab,
                         transformer, action).fork();
                }
                for (Node<K,V> p; (p = advance()) != null; ) {
                    U u;
                    if ((u = transformer.apply(p.key, p.val)) != null)
                        action.accept(u);
                }
                propagateCompletion();
            }
        }
    }

    @SuppressWarnings("serial")
    static final class SearchKeysTask<K,V,U>
        extends BulkTask<K,V,U> {
        final Function<? super K, ? extends U> searchFunction;
        final AtomicReference<U> result;
        SearchKeysTask
            (BulkTask<K,V,?> p, int b, int i, int f, Node<K,V>[] t,
             Function<? super K, ? extends U> searchFunction,
             AtomicReference<U> result) {
            super(p, b, i, f, t);
            this.searchFunction = searchFunction; this.result = result;
        }
        public final U getRawResult() { return result.get(); }
        public final void compute() {
            final Function<? super K, ? extends U> searchFunction;
            final AtomicReference<U> result;
            if ((searchFunction = this.searchFunction) != null &&
                (result = this.result) != null) {
                for (int i = baseIndex, f, h; batch > 0 &&
                         (h = ((f = baseLimit) + i) >>> 1) > i;) {
                    if (result.get() != null)
                        return;
                    addToPendingCount(1);
                    new SearchKeysTask<K,V,U>
                        (this, batch >>>= 1, baseLimit = h, f, tab,
                         searchFunction, result).fork();
                }
                while (result.get() == null) {
                    U u;
                    Node<K,V> p;
                    if ((p = advance()) == null) {
                        propagateCompletion();
                        break;
                    }
                    if ((u = searchFunction.apply(p.key)) != null) {
                        if (result.compareAndSet(null, u))
                            quietlyCompleteRoot();
                        break;
                    }
                }
            }
        }
    }

    @SuppressWarnings("serial")
    static final class SearchValuesTask<K,V,U>
        extends BulkTask<K,V,U> {
        final Function<? super V, ? extends U> searchFunction;
        final AtomicReference<U> result;
        SearchValuesTask
            (BulkTask<K,V,?> p, int b, int i, int f, Node<K,V>[] t,
             Function<? super V, ? extends U> searchFunction,
             AtomicReference<U> result) {
            super(p, b, i, f, t);
            this.searchFunction = searchFunction; this.result = result;
        }
        public final U getRawResult() { return result.get(); }
        public final void compute() {
            final Function<? super V, ? extends U> searchFunction;
            final AtomicReference<U> result;
            if ((searchFunction = this.searchFunction) != null &&
                (result = this.result) != null) {
                for (int i = baseIndex, f, h; batch > 0 &&
                         (h = ((f = baseLimit) + i) >>> 1) > i;) {
                    if (result.get() != null)
                        return;
                    addToPendingCount(1);
                    new SearchValuesTask<K,V,U>
                        (this, batch >>>= 1, baseLimit = h, f, tab,
                         searchFunction, result).fork();
                }
                while (result.get() == null) {
                    U u;
                    Node<K,V> p;
                    if ((p = advance()) == null) {
                        propagateCompletion();
                        break;
                    }
                    if ((u = searchFunction.apply(p.val)) != null) {
                        if (result.compareAndSet(null, u))
                            quietlyCompleteRoot();
                        break;
                    }
                }
            }
        }
    }

    @SuppressWarnings("serial")
    static final class SearchEntriesTask<K,V,U>
        extends BulkTask<K,V,U> {
        final Function<Entry<K,V>, ? extends U> searchFunction;
        final AtomicReference<U> result;
        SearchEntriesTask
            (BulkTask<K,V,?> p, int b, int i, int f, Node<K,V>[] t,
             Function<Entry<K,V>, ? extends U> searchFunction,
             AtomicReference<U> result) {
            super(p, b, i, f, t);
            this.searchFunction = searchFunction; this.result = result;
        }
        public final U getRawResult() { return result.get(); }
        public final void compute() {
            final Function<Entry<K,V>, ? extends U> searchFunction;
            final AtomicReference<U> result;
            if ((searchFunction = this.searchFunction) != null &&
                (result = this.result) != null) {
                for (int i = baseIndex, f, h; batch > 0 &&
                         (h = ((f = baseLimit) + i) >>> 1) > i;) {
                    if (result.get() != null)
                        return;
                    addToPendingCount(1);
                    new SearchEntriesTask<K,V,U>
                        (this, batch >>>= 1, baseLimit = h, f, tab,
                         searchFunction, result).fork();
                }
                while (result.get() == null) {
                    U u;
                    Node<K,V> p;
                    if ((p = advance()) == null) {
                        propagateCompletion();
                        break;
                    }
                    if ((u = searchFunction.apply(p)) != null) {
                        if (result.compareAndSet(null, u))
                            quietlyCompleteRoot();
                        return;
                    }
                }
            }
        }
    }

    @SuppressWarnings("serial")
    static final class SearchMappingsTask<K,V,U>
        extends BulkTask<K,V,U> {
        final BiFunction<? super K, ? super V, ? extends U> searchFunction;
        final AtomicReference<U> result;
        SearchMappingsTask
            (BulkTask<K,V,?> p, int b, int i, int f, Node<K,V>[] t,
             BiFunction<? super K, ? super V, ? extends U> searchFunction,
             AtomicReference<U> result) {
            super(p, b, i, f, t);
            this.searchFunction = searchFunction; this.result = result;
        }
        public final U getRawResult() { return result.get(); }
        public final void compute() {
            final BiFunction<? super K, ? super V, ? extends U> searchFunction;
            final AtomicReference<U> result;
            if ((searchFunction = this.searchFunction) != null &&
                (result = this.result) != null) {
                for (int i = baseIndex, f, h; batch > 0 &&
                         (h = ((f = baseLimit) + i) >>> 1) > i;) {
                    if (result.get() != null)
                        return;
                    addToPendingCount(1);
                    new SearchMappingsTask<K,V,U>
                        (this, batch >>>= 1, baseLimit = h, f, tab,
                         searchFunction, result).fork();
                }
                while (result.get() == null) {
                    U u;
                    Node<K,V> p;
                    if ((p = advance()) == null) {
                        propagateCompletion();
                        break;
                    }
                    if ((u = searchFunction.apply(p.key, p.val)) != null) {
                        if (result.compareAndSet(null, u))
                            quietlyCompleteRoot();
                        break;
                    }
                }
            }
        }
    }

    @SuppressWarnings("serial")
    static final class ReduceKeysTask<K,V>
        extends BulkTask<K,V,K> {
        final BiFunction<? super K, ? super K, ? extends K> reducer;
        K result;
        ReduceKeysTask<K,V> rights, nextRight;
        ReduceKeysTask
            (BulkTask<K,V,?> p, int b, int i, int f, Node<K,V>[] t,
             ReduceKeysTask<K,V> nextRight,
             BiFunction<? super K, ? super K, ? extends K> reducer) {
            super(p, b, i, f, t); this.nextRight = nextRight;
            this.reducer = reducer;
        }
        public final K getRawResult() { return result; }
        public final void compute() {
            final BiFunction<? super K, ? super K, ? extends K> reducer;
            if ((reducer = this.reducer) != null) {
                for (int i = baseIndex, f, h; batch > 0 &&
                         (h = ((f = baseLimit) + i) >>> 1) > i;) {
                    addToPendingCount(1);
                    (rights = new ReduceKeysTask<K,V>
                     (this, batch >>>= 1, baseLimit = h, f, tab,
                      rights, reducer)).fork();
                }
                K r = null;
                for (Node<K,V> p; (p = advance()) != null; ) {
                    K u = p.key;
                    r = (r == null) ? u : u == null ? r : reducer.apply(r, u);
                }
                result = r;
                CountedCompleter<?> c;
                for (c = firstComplete(); c != null; c = c.nextComplete()) {
                    @SuppressWarnings("unchecked")
                    ReduceKeysTask<K,V>
                        t = (ReduceKeysTask<K,V>)c,
                        s = t.rights;
                    while (s != null) {
                        K tr, sr;
                        if ((sr = s.result) != null)
                            t.result = (((tr = t.result) == null) ? sr :
                                        reducer.apply(tr, sr));
                        s = t.rights = s.nextRight;
                    }
                }
            }
        }
    }

    @SuppressWarnings("serial")
    static final class ReduceValuesTask<K,V>
        extends BulkTask<K,V,V> {
        final BiFunction<? super V, ? super V, ? extends V> reducer;
        V result;
        ReduceValuesTask<K,V> rights, nextRight;
        ReduceValuesTask
            (BulkTask<K,V,?> p, int b, int i, int f, Node<K,V>[] t,
             ReduceValuesTask<K,V> nextRight,
             BiFunction<? super V, ? super V, ? extends V> reducer) {
            super(p, b, i, f, t); this.nextRight = nextRight;
            this.reducer = reducer;
        }
        public final V getRawResult() { return result; }
        public final void compute() {
            final BiFunction<? super V, ? super V, ? extends V> reducer;
            if ((reducer = this.reducer) != null) {
                for (int i = baseIndex, f, h; batch > 0 &&
                         (h = ((f = baseLimit) + i) >>> 1) > i;) {
                    addToPendingCount(1);
                    (rights = new ReduceValuesTask<K,V>
                     (this, batch >>>= 1, baseLimit = h, f, tab,
                      rights, reducer)).fork();
                }
                V r = null;
                for (Node<K,V> p; (p = advance()) != null; ) {
                    V v = p.val;
                    r = (r == null) ? v : reducer.apply(r, v);
                }
                result = r;
                CountedCompleter<?> c;
                for (c = firstComplete(); c != null; c = c.nextComplete()) {
                    @SuppressWarnings("unchecked")
                    ReduceValuesTask<K,V>
                        t = (ReduceValuesTask<K,V>)c,
                        s = t.rights;
                    while (s != null) {
                        V tr, sr;
                        if ((sr = s.result) != null)
                            t.result = (((tr = t.result) == null) ? sr :
                                        reducer.apply(tr, sr));
                        s = t.rights = s.nextRight;
                    }
                }
            }
        }
    }

    @SuppressWarnings("serial")
    static final class ReduceEntriesTask<K,V>
        extends BulkTask<K,V,Map.Entry<K,V>> {
        final BiFunction<Map.Entry<K,V>, Map.Entry<K,V>, ? extends Map.Entry<K,V>> reducer;
        Map.Entry<K,V> result;
        ReduceEntriesTask<K,V> rights, nextRight;
        ReduceEntriesTask
            (BulkTask<K,V,?> p, int b, int i, int f, Node<K,V>[] t,
             ReduceEntriesTask<K,V> nextRight,
             BiFunction<Entry<K,V>, Map.Entry<K,V>, ? extends Map.Entry<K,V>> reducer) {
            super(p, b, i, f, t); this.nextRight = nextRight;
            this.reducer = reducer;
        }
        public final Map.Entry<K,V> getRawResult() { return result; }
        public final void compute() {
            final BiFunction<Map.Entry<K,V>, Map.Entry<K,V>, ? extends Map.Entry<K,V>> reducer;
            if ((reducer = this.reducer) != null) {
                for (int i = baseIndex, f, h; batch > 0 &&
                         (h = ((f = baseLimit) + i) >>> 1) > i;) {
                    addToPendingCount(1);
                    (rights = new ReduceEntriesTask<K,V>
                     (this, batch >>>= 1, baseLimit = h, f, tab,
                      rights, reducer)).fork();
                }
                Map.Entry<K,V> r = null;
                for (Node<K,V> p; (p = advance()) != null; )
                    r = (r == null) ? p : reducer.apply(r, p);
                result = r;
                CountedCompleter<?> c;
                for (c = firstComplete(); c != null; c = c.nextComplete()) {
                    @SuppressWarnings("unchecked")
                    ReduceEntriesTask<K,V>
                        t = (ReduceEntriesTask<K,V>)c,
                        s = t.rights;
                    while (s != null) {
                        Map.Entry<K,V> tr, sr;
                        if ((sr = s.result) != null)
                            t.result = (((tr = t.result) == null) ? sr :
                                        reducer.apply(tr, sr));
                        s = t.rights = s.nextRight;
                    }
                }
            }
        }
    }

    @SuppressWarnings("serial")
    static final class MapReduceKeysTask<K,V,U>
        extends BulkTask<K,V,U> {
        final Function<? super K, ? extends U> transformer;
        final BiFunction<? super U, ? super U, ? extends U> reducer;
        U result;
        MapReduceKeysTask<K,V,U> rights, nextRight;
        MapReduceKeysTask
            (BulkTask<K,V,?> p, int b, int i, int f, Node<K,V>[] t,
             MapReduceKeysTask<K,V,U> nextRight,
             Function<? super K, ? extends U> transformer,
             BiFunction<? super U, ? super U, ? extends U> reducer) {
            super(p, b, i, f, t); this.nextRight = nextRight;
            this.transformer = transformer;
            this.reducer = reducer;
        }
        public final U getRawResult() { return result; }
        public final void compute() {
            final Function<? super K, ? extends U> transformer;
            final BiFunction<? super U, ? super U, ? extends U> reducer;
            if ((transformer = this.transformer) != null &&
                (reducer = this.reducer) != null) {
                for (int i = baseIndex, f, h; batch > 0 &&
                         (h = ((f = baseLimit) + i) >>> 1) > i;) {
                    addToPendingCount(1);
                    (rights = new MapReduceKeysTask<K,V,U>
                     (this, batch >>>= 1, baseLimit = h, f, tab,
                      rights, transformer, reducer)).fork();
                }
                U r = null;
                for (Node<K,V> p; (p = advance()) != null; ) {
                    U u;
                    if ((u = transformer.apply(p.key)) != null)
                        r = (r == null) ? u : reducer.apply(r, u);
                }
                result = r;
                CountedCompleter<?> c;
                for (c = firstComplete(); c != null; c = c.nextComplete()) {
                    @SuppressWarnings("unchecked")
                    MapReduceKeysTask<K,V,U>
                        t = (MapReduceKeysTask<K,V,U>)c,
                        s = t.rights;
                    while (s != null) {
                        U tr, sr;
                        if ((sr = s.result) != null)
                            t.result = (((tr = t.result) == null) ? sr :
                                        reducer.apply(tr, sr));
                        s = t.rights = s.nextRight;
                    }
                }
            }
        }
    }

    @SuppressWarnings("serial")
    static final class MapReduceValuesTask<K,V,U>
        extends BulkTask<K,V,U> {
        final Function<? super V, ? extends U> transformer;
        final BiFunction<? super U, ? super U, ? extends U> reducer;
        U result;
        MapReduceValuesTask<K,V,U> rights, nextRight;
        MapReduceValuesTask
            (BulkTask<K,V,?> p, int b, int i, int f, Node<K,V>[] t,
             MapReduceValuesTask<K,V,U> nextRight,
             Function<? super V, ? extends U> transformer,
             BiFunction<? super U, ? super U, ? extends U> reducer) {
            super(p, b, i, f, t); this.nextRight = nextRight;
            this.transformer = transformer;
            this.reducer = reducer;
        }
        public final U getRawResult() { return result; }
        public final void compute() {
            final Function<? super V, ? extends U> transformer;
            final BiFunction<? super U, ? super U, ? extends U> reducer;
            if ((transformer = this.transformer) != null &&
                (reducer = this.reducer) != null) {
                for (int i = baseIndex, f, h; batch > 0 &&
                         (h = ((f = baseLimit) + i) >>> 1) > i;) {
                    addToPendingCount(1);
                    (rights = new MapReduceValuesTask<K,V,U>
                     (this, batch >>>= 1, baseLimit = h, f, tab,
                      rights, transformer, reducer)).fork();
                }
                U r = null;
                for (Node<K,V> p; (p = advance()) != null; ) {
                    U u;
                    if ((u = transformer.apply(p.val)) != null)
                        r = (r == null) ? u : reducer.apply(r, u);
                }
                result = r;
                CountedCompleter<?> c;
                for (c = firstComplete(); c != null; c = c.nextComplete()) {
                    @SuppressWarnings("unchecked")
                    MapReduceValuesTask<K,V,U>
                        t = (MapReduceValuesTask<K,V,U>)c,
                        s = t.rights;
                    while (s != null) {
                        U tr, sr;
                        if ((sr = s.result) != null)
                            t.result = (((tr = t.result) == null) ? sr :
                                        reducer.apply(tr, sr));
                        s = t.rights = s.nextRight;
                    }
                }
            }
        }
    }

    @SuppressWarnings("serial")
    static final class MapReduceEntriesTask<K,V,U>
        extends BulkTask<K,V,U> {
        final Function<Map.Entry<K,V>, ? extends U> transformer;
        final BiFunction<? super U, ? super U, ? extends U> reducer;
        U result;
        MapReduceEntriesTask<K,V,U> rights, nextRight;
        MapReduceEntriesTask
            (BulkTask<K,V,?> p, int b, int i, int f, Node<K,V>[] t,
             MapReduceEntriesTask<K,V,U> nextRight,
             Function<Map.Entry<K,V>, ? extends U> transformer,
             BiFunction<? super U, ? super U, ? extends U> reducer) {
            super(p, b, i, f, t); this.nextRight = nextRight;
            this.transformer = transformer;
            this.reducer = reducer;
        }
        public final U getRawResult() { return result; }
        public final void compute() {
            final Function<Map.Entry<K,V>, ? extends U> transformer;
            final BiFunction<? super U, ? super U, ? extends U> reducer;
            if ((transformer = this.transformer) != null &&
                (reducer = this.reducer) != null) {
                for (int i = baseIndex, f, h; batch > 0 &&
                         (h = ((f = baseLimit) + i) >>> 1) > i;) {
                    addToPendingCount(1);
                    (rights = new MapReduceEntriesTask<K,V,U>
                     (this, batch >>>= 1, baseLimit = h, f, tab,
                      rights, transformer, reducer)).fork();
                }
                U r = null;
                for (Node<K,V> p; (p = advance()) != null; ) {
                    U u;
                    if ((u = transformer.apply(p)) != null)
                        r = (r == null) ? u : reducer.apply(r, u);
                }
                result = r;
                CountedCompleter<?> c;
                for (c = firstComplete(); c != null; c = c.nextComplete()) {
                    @SuppressWarnings("unchecked")
                    MapReduceEntriesTask<K,V,U>
                        t = (MapReduceEntriesTask<K,V,U>)c,
                        s = t.rights;
                    while (s != null) {
                        U tr, sr;
                        if ((sr = s.result) != null)
                            t.result = (((tr = t.result) == null) ? sr :
                                        reducer.apply(tr, sr));
                        s = t.rights = s.nextRight;
                    }
                }
            }
        }
    }

    @SuppressWarnings("serial")
    static final class MapReduceMappingsTask<K,V,U>
        extends BulkTask<K,V,U> {
        final BiFunction<? super K, ? super V, ? extends U> transformer;
        final BiFunction<? super U, ? super U, ? extends U> reducer;
        U result;
        MapReduceMappingsTask<K,V,U> rights, nextRight;
        MapReduceMappingsTask
            (BulkTask<K,V,?> p, int b, int i, int f, Node<K,V>[] t,
             MapReduceMappingsTask<K,V,U> nextRight,
             BiFunction<? super K, ? super V, ? extends U> transformer,
             BiFunction<? super U, ? super U, ? extends U> reducer) {
            super(p, b, i, f, t); this.nextRight = nextRight;
            this.transformer = transformer;
            this.reducer = reducer;
        }
        public final U getRawResult() { return result; }
        public final void compute() {
            final BiFunction<? super K, ? super V, ? extends U> transformer;
            final BiFunction<? super U, ? super U, ? extends U> reducer;
            if ((transformer = this.transformer) != null &&
                (reducer = this.reducer) != null) {
                for (int i = baseIndex, f, h; batch > 0 &&
                         (h = ((f = baseLimit) + i) >>> 1) > i;) {
                    addToPendingCount(1);
                    (rights = new MapReduceMappingsTask<K,V,U>
                     (this, batch >>>= 1, baseLimit = h, f, tab,
                      rights, transformer, reducer)).fork();
                }
                U r = null;
                for (Node<K,V> p; (p = advance()) != null; ) {
                    U u;
                    if ((u = transformer.apply(p.key, p.val)) != null)
                        r = (r == null) ? u : reducer.apply(r, u);
                }
                result = r;
                CountedCompleter<?> c;
                for (c = firstComplete(); c != null; c = c.nextComplete()) {
                    @SuppressWarnings("unchecked")
                    MapReduceMappingsTask<K,V,U>
                        t = (MapReduceMappingsTask<K,V,U>)c,
                        s = t.rights;
                    while (s != null) {
                        U tr, sr;
                        if ((sr = s.result) != null)
                            t.result = (((tr = t.result) == null) ? sr :
                                        reducer.apply(tr, sr));
                        s = t.rights = s.nextRight;
                    }
                }
            }
        }
    }

    @SuppressWarnings("serial")
    static final class MapReduceKeysToDoubleTask<K,V>
        extends BulkTask<K,V,Double> {
        final ToDoubleFunction<? super K> transformer;
        final DoubleBinaryOperator reducer;
        final double basis;
        double result;
        MapReduceKeysToDoubleTask<K,V> rights, nextRight;
        MapReduceKeysToDoubleTask
            (BulkTask<K,V,?> p, int b, int i, int f, Node<K,V>[] t,
             MapReduceKeysToDoubleTask<K,V> nextRight,
             ToDoubleFunction<? super K> transformer,
             double basis,
             DoubleBinaryOperator reducer) {
            super(p, b, i, f, t); this.nextRight = nextRight;
            this.transformer = transformer;
            this.basis = basis; this.reducer = reducer;
        }
        public final Double getRawResult() { return result; }
        public final void compute() {
            final ToDoubleFunction<? super K> transformer;
            final DoubleBinaryOperator reducer;
            if ((transformer = this.transformer) != null &&
                (reducer = this.reducer) != null) {
                double r = this.basis;
                for (int i = baseIndex, f, h; batch > 0 &&
                         (h = ((f = baseLimit) + i) >>> 1) > i;) {
                    addToPendingCount(1);
                    (rights = new MapReduceKeysToDoubleTask<K,V>
                     (this, batch >>>= 1, baseLimit = h, f, tab,
                      rights, transformer, r, reducer)).fork();
                }
                for (Node<K,V> p; (p = advance()) != null; )
                    r = reducer.applyAsDouble(r, transformer.applyAsDouble(p.key));
                result = r;
                CountedCompleter<?> c;
                for (c = firstComplete(); c != null; c = c.nextComplete()) {
                    @SuppressWarnings("unchecked")
                    MapReduceKeysToDoubleTask<K,V>
                        t = (MapReduceKeysToDoubleTask<K,V>)c,
                        s = t.rights;
                    while (s != null) {
                        t.result = reducer.applyAsDouble(t.result, s.result);
                        s = t.rights = s.nextRight;
                    }
                }
            }
        }
    }

    @SuppressWarnings("serial")
    static final class MapReduceValuesToDoubleTask<K,V>
        extends BulkTask<K,V,Double> {
        final ToDoubleFunction<? super V> transformer;
        final DoubleBinaryOperator reducer;
        final double basis;
        double result;
        MapReduceValuesToDoubleTask<K,V> rights, nextRight;
        MapReduceValuesToDoubleTask
            (BulkTask<K,V,?> p, int b, int i, int f, Node<K,V>[] t,
             MapReduceValuesToDoubleTask<K,V> nextRight,
             ToDoubleFunction<? super V> transformer,
             double basis,
             DoubleBinaryOperator reducer) {
            super(p, b, i, f, t); this.nextRight = nextRight;
            this.transformer = transformer;
            this.basis = basis; this.reducer = reducer;
        }
        public final Double getRawResult() { return result; }
        public final void compute() {
            final ToDoubleFunction<? super V> transformer;
            final DoubleBinaryOperator reducer;
            if ((transformer = this.transformer) != null &&
                (reducer = this.reducer) != null) {
                double r = this.basis;
                for (int i = baseIndex, f, h; batch > 0 &&
                         (h = ((f = baseLimit) + i) >>> 1) > i;) {
                    addToPendingCount(1);
                    (rights = new MapReduceValuesToDoubleTask<K,V>
                     (this, batch >>>= 1, baseLimit = h, f, tab,
                      rights, transformer, r, reducer)).fork();
                }
                for (Node<K,V> p; (p = advance()) != null; )
                    r = reducer.applyAsDouble(r, transformer.applyAsDouble(p.val));
                result = r;
                CountedCompleter<?> c;
                for (c = firstComplete(); c != null; c = c.nextComplete()) {
                    @SuppressWarnings("unchecked")
                    MapReduceValuesToDoubleTask<K,V>
                        t = (MapReduceValuesToDoubleTask<K,V>)c,
                        s = t.rights;
                    while (s != null) {
                        t.result = reducer.applyAsDouble(t.result, s.result);
                        s = t.rights = s.nextRight;
                    }
                }
            }
        }
    }

    @SuppressWarnings("serial")
    static final class MapReduceEntriesToDoubleTask<K,V>
        extends BulkTask<K,V,Double> {
        final ToDoubleFunction<Map.Entry<K,V>> transformer;
        final DoubleBinaryOperator reducer;
        final double basis;
        double result;
        MapReduceEntriesToDoubleTask<K,V> rights, nextRight;
        MapReduceEntriesToDoubleTask
            (BulkTask<K,V,?> p, int b, int i, int f, Node<K,V>[] t,
             MapReduceEntriesToDoubleTask<K,V> nextRight,
             ToDoubleFunction<Map.Entry<K,V>> transformer,
             double basis,
             DoubleBinaryOperator reducer) {
            super(p, b, i, f, t); this.nextRight = nextRight;
            this.transformer = transformer;
            this.basis = basis; this.reducer = reducer;
        }
        public final Double getRawResult() { return result; }
        public final void compute() {
            final ToDoubleFunction<Map.Entry<K,V>> transformer;
            final DoubleBinaryOperator reducer;
            if ((transformer = this.transformer) != null &&
                (reducer = this.reducer) != null) {
                double r = this.basis;
                for (int i = baseIndex, f, h; batch > 0 &&
                         (h = ((f = baseLimit) + i) >>> 1) > i;) {
                    addToPendingCount(1);
                    (rights = new MapReduceEntriesToDoubleTask<K,V>
                     (this, batch >>>= 1, baseLimit = h, f, tab,
                      rights, transformer, r, reducer)).fork();
                }
                for (Node<K,V> p; (p = advance()) != null; )
                    r = reducer.applyAsDouble(r, transformer.applyAsDouble(p));
                result = r;
                CountedCompleter<?> c;
                for (c = firstComplete(); c != null; c = c.nextComplete()) {
                    @SuppressWarnings("unchecked")
                    MapReduceEntriesToDoubleTask<K,V>
                        t = (MapReduceEntriesToDoubleTask<K,V>)c,
                        s = t.rights;
                    while (s != null) {
                        t.result = reducer.applyAsDouble(t.result, s.result);
                        s = t.rights = s.nextRight;
                    }
                }
            }
        }
    }

    @SuppressWarnings("serial")
    static final class MapReduceMappingsToDoubleTask<K,V>
        extends BulkTask<K,V,Double> {
        final ToDoubleBiFunction<? super K, ? super V> transformer;
        final DoubleBinaryOperator reducer;
        final double basis;
        double result;
        MapReduceMappingsToDoubleTask<K,V> rights, nextRight;
        MapReduceMappingsToDoubleTask
            (BulkTask<K,V,?> p, int b, int i, int f, Node<K,V>[] t,
             MapReduceMappingsToDoubleTask<K,V> nextRight,
             ToDoubleBiFunction<? super K, ? super V> transformer,
             double basis,
             DoubleBinaryOperator reducer) {
            super(p, b, i, f, t); this.nextRight = nextRight;
            this.transformer = transformer;
            this.basis = basis; this.reducer = reducer;
        }
        public final Double getRawResult() { return result; }
        public final void compute() {
            final ToDoubleBiFunction<? super K, ? super V> transformer;
            final DoubleBinaryOperator reducer;
            if ((transformer = this.transformer) != null &&
                (reducer = this.reducer) != null) {
                double r = this.basis;
                for (int i = baseIndex, f, h; batch > 0 &&
                         (h = ((f = baseLimit) + i) >>> 1) > i;) {
                    addToPendingCount(1);
                    (rights = new MapReduceMappingsToDoubleTask<K,V>
                     (this, batch >>>= 1, baseLimit = h, f, tab,
                      rights, transformer, r, reducer)).fork();
                }
                for (Node<K,V> p; (p = advance()) != null; )
                    r = reducer.applyAsDouble(r, transformer.applyAsDouble(p.key, p.val));
                result = r;
                CountedCompleter<?> c;
                for (c = firstComplete(); c != null; c = c.nextComplete()) {
                    @SuppressWarnings("unchecked")
                    MapReduceMappingsToDoubleTask<K,V>
                        t = (MapReduceMappingsToDoubleTask<K,V>)c,
                        s = t.rights;
                    while (s != null) {
                        t.result = reducer.applyAsDouble(t.result, s.result);
                        s = t.rights = s.nextRight;
                    }
                }
            }
        }
    }

    @SuppressWarnings("serial")
    static final class MapReduceKeysToLongTask<K,V>
        extends BulkTask<K,V,Long> {
        final ToLongFunction<? super K> transformer;
        final LongBinaryOperator reducer;
        final long basis;
        long result;
        MapReduceKeysToLongTask<K,V> rights, nextRight;
        MapReduceKeysToLongTask
            (BulkTask<K,V,?> p, int b, int i, int f, Node<K,V>[] t,
             MapReduceKeysToLongTask<K,V> nextRight,
             ToLongFunction<? super K> transformer,
             long basis,
             LongBinaryOperator reducer) {
            super(p, b, i, f, t); this.nextRight = nextRight;
            this.transformer = transformer;
            this.basis = basis; this.reducer = reducer;
        }
        public final Long getRawResult() { return result; }
        public final void compute() {
            final ToLongFunction<? super K> transformer;
            final LongBinaryOperator reducer;
            if ((transformer = this.transformer) != null &&
                (reducer = this.reducer) != null) {
                long r = this.basis;
                for (int i = baseIndex, f, h; batch > 0 &&
                         (h = ((f = baseLimit) + i) >>> 1) > i;) {
                    addToPendingCount(1);
                    (rights = new MapReduceKeysToLongTask<K,V>
                     (this, batch >>>= 1, baseLimit = h, f, tab,
                      rights, transformer, r, reducer)).fork();
                }
                for (Node<K,V> p; (p = advance()) != null; )
                    r = reducer.applyAsLong(r, transformer.applyAsLong(p.key));
                result = r;
                CountedCompleter<?> c;
                for (c = firstComplete(); c != null; c = c.nextComplete()) {
                    @SuppressWarnings("unchecked")
                    MapReduceKeysToLongTask<K,V>
                        t = (MapReduceKeysToLongTask<K,V>)c,
                        s = t.rights;
                    while (s != null) {
                        t.result = reducer.applyAsLong(t.result, s.result);
                        s = t.rights = s.nextRight;
                    }
                }
            }
        }
    }

    @SuppressWarnings("serial")
    static final class MapReduceValuesToLongTask<K,V>
        extends BulkTask<K,V,Long> {
        final ToLongFunction<? super V> transformer;
        final LongBinaryOperator reducer;
        final long basis;
        long result;
        MapReduceValuesToLongTask<K,V> rights, nextRight;
        MapReduceValuesToLongTask
            (BulkTask<K,V,?> p, int b, int i, int f, Node<K,V>[] t,
             MapReduceValuesToLongTask<K,V> nextRight,
             ToLongFunction<? super V> transformer,
             long basis,
             LongBinaryOperator reducer) {
            super(p, b, i, f, t); this.nextRight = nextRight;
            this.transformer = transformer;
            this.basis = basis; this.reducer = reducer;
        }
        public final Long getRawResult() { return result; }
        public final void compute() {
            final ToLongFunction<? super V> transformer;
            final LongBinaryOperator reducer;
            if ((transformer = this.transformer) != null &&
                (reducer = this.reducer) != null) {
                long r = this.basis;
                for (int i = baseIndex, f, h; batch > 0 &&
                         (h = ((f = baseLimit) + i) >>> 1) > i;) {
                    addToPendingCount(1);
                    (rights = new MapReduceValuesToLongTask<K,V>
                     (this, batch >>>= 1, baseLimit = h, f, tab,
                      rights, transformer, r, reducer)).fork();
                }
                for (Node<K,V> p; (p = advance()) != null; )
                    r = reducer.applyAsLong(r, transformer.applyAsLong(p.val));
                result = r;
                CountedCompleter<?> c;
                for (c = firstComplete(); c != null; c = c.nextComplete()) {
                    @SuppressWarnings("unchecked")
                    MapReduceValuesToLongTask<K,V>
                        t = (MapReduceValuesToLongTask<K,V>)c,
                        s = t.rights;
                    while (s != null) {
                        t.result = reducer.applyAsLong(t.result, s.result);
                        s = t.rights = s.nextRight;
                    }
                }
            }
        }
    }

    @SuppressWarnings("serial")
    static final class MapReduceEntriesToLongTask<K,V>
        extends BulkTask<K,V,Long> {
        final ToLongFunction<Map.Entry<K,V>> transformer;
        final LongBinaryOperator reducer;
        final long basis;
        long result;
        MapReduceEntriesToLongTask<K,V> rights, nextRight;
        MapReduceEntriesToLongTask
            (BulkTask<K,V,?> p, int b, int i, int f, Node<K,V>[] t,
             MapReduceEntriesToLongTask<K,V> nextRight,
             ToLongFunction<Map.Entry<K,V>> transformer,
             long basis,
             LongBinaryOperator reducer) {
            super(p, b, i, f, t); this.nextRight = nextRight;
            this.transformer = transformer;
            this.basis = basis; this.reducer = reducer;
        }
        public final Long getRawResult() { return result; }
        public final void compute() {
            final ToLongFunction<Map.Entry<K,V>> transformer;
            final LongBinaryOperator reducer;
            if ((transformer = this.transformer) != null &&
                (reducer = this.reducer) != null) {
                long r = this.basis;
                for (int i = baseIndex, f, h; batch > 0 &&
                         (h = ((f = baseLimit) + i) >>> 1) > i;) {
                    addToPendingCount(1);
                    (rights = new MapReduceEntriesToLongTask<K,V>
                     (this, batch >>>= 1, baseLimit = h, f, tab,
                      rights, transformer, r, reducer)).fork();
                }
                for (Node<K,V> p; (p = advance()) != null; )
                    r = reducer.applyAsLong(r, transformer.applyAsLong(p));
                result = r;
                CountedCompleter<?> c;
                for (c = firstComplete(); c != null; c = c.nextComplete()) {
                    @SuppressWarnings("unchecked")
                    MapReduceEntriesToLongTask<K,V>
                        t = (MapReduceEntriesToLongTask<K,V>)c,
                        s = t.rights;
                    while (s != null) {
                        t.result = reducer.applyAsLong(t.result, s.result);
                        s = t.rights = s.nextRight;
                    }
                }
            }
        }
    }

    @SuppressWarnings("serial")
    static final class MapReduceMappingsToLongTask<K,V>
        extends BulkTask<K,V,Long> {
        final ToLongBiFunction<? super K, ? super V> transformer;
        final LongBinaryOperator reducer;
        final long basis;
        long result;
        MapReduceMappingsToLongTask<K,V> rights, nextRight;
        MapReduceMappingsToLongTask
            (BulkTask<K,V,?> p, int b, int i, int f, Node<K,V>[] t,
             MapReduceMappingsToLongTask<K,V> nextRight,
             ToLongBiFunction<? super K, ? super V> transformer,
             long basis,
             LongBinaryOperator reducer) {
            super(p, b, i, f, t); this.nextRight = nextRight;
            this.transformer = transformer;
            this.basis = basis; this.reducer = reducer;
        }
        public final Long getRawResult() { return result; }
        public final void compute() {
            final ToLongBiFunction<? super K, ? super V> transformer;
            final LongBinaryOperator reducer;
            if ((transformer = this.transformer) != null &&
                (reducer = this.reducer) != null) {
                long r = this.basis;
                for (int i = baseIndex, f, h; batch > 0 &&
                         (h = ((f = baseLimit) + i) >>> 1) > i;) {
                    addToPendingCount(1);
                    (rights = new MapReduceMappingsToLongTask<K,V>
                     (this, batch >>>= 1, baseLimit = h, f, tab,
                      rights, transformer, r, reducer)).fork();
                }
                for (Node<K,V> p; (p = advance()) != null; )
                    r = reducer.applyAsLong(r, transformer.applyAsLong(p.key, p.val));
                result = r;
                CountedCompleter<?> c;
                for (c = firstComplete(); c != null; c = c.nextComplete()) {
                    @SuppressWarnings("unchecked")
                    MapReduceMappingsToLongTask<K,V>
                        t = (MapReduceMappingsToLongTask<K,V>)c,
                        s = t.rights;
                    while (s != null) {
                        t.result = reducer.applyAsLong(t.result, s.result);
                        s = t.rights = s.nextRight;
                    }
                }
            }
        }
    }

    @SuppressWarnings("serial")
    static final class MapReduceKeysToIntTask<K,V>
        extends BulkTask<K,V,Integer> {
        final ToIntFunction<? super K> transformer;
        final IntBinaryOperator reducer;
        final int basis;
        int result;
        MapReduceKeysToIntTask<K,V> rights, nextRight;
        MapReduceKeysToIntTask
            (BulkTask<K,V,?> p, int b, int i, int f, Node<K,V>[] t,
             MapReduceKeysToIntTask<K,V> nextRight,
             ToIntFunction<? super K> transformer,
             int basis,
             IntBinaryOperator reducer) {
            super(p, b, i, f, t); this.nextRight = nextRight;
            this.transformer = transformer;
            this.basis = basis; this.reducer = reducer;
        }
        public final Integer getRawResult() { return result; }
        public final void compute() {
            final ToIntFunction<? super K> transformer;
            final IntBinaryOperator reducer;
            if ((transformer = this.transformer) != null &&
                (reducer = this.reducer) != null) {
                int r = this.basis;
                for (int i = baseIndex, f, h; batch > 0 &&
                         (h = ((f = baseLimit) + i) >>> 1) > i;) {
                    addToPendingCount(1);
                    (rights = new MapReduceKeysToIntTask<K,V>
                     (this, batch >>>= 1, baseLimit = h, f, tab,
                      rights, transformer, r, reducer)).fork();
                }
                for (Node<K,V> p; (p = advance()) != null; )
                    r = reducer.applyAsInt(r, transformer.applyAsInt(p.key));
                result = r;
                CountedCompleter<?> c;
                for (c = firstComplete(); c != null; c = c.nextComplete()) {
                    @SuppressWarnings("unchecked")
                    MapReduceKeysToIntTask<K,V>
                        t = (MapReduceKeysToIntTask<K,V>)c,
                        s = t.rights;
                    while (s != null) {
                        t.result = reducer.applyAsInt(t.result, s.result);
                        s = t.rights = s.nextRight;
                    }
                }
            }
        }
    }

    @SuppressWarnings("serial")
    static final class MapReduceValuesToIntTask<K,V>
        extends BulkTask<K,V,Integer> {
        final ToIntFunction<? super V> transformer;
        final IntBinaryOperator reducer;
        final int basis;
        int result;
        MapReduceValuesToIntTask<K,V> rights, nextRight;
        MapReduceValuesToIntTask
            (BulkTask<K,V,?> p, int b, int i, int f, Node<K,V>[] t,
             MapReduceValuesToIntTask<K,V> nextRight,
             ToIntFunction<? super V> transformer,
             int basis,
             IntBinaryOperator reducer) {
            super(p, b, i, f, t); this.nextRight = nextRight;
            this.transformer = transformer;
            this.basis = basis; this.reducer = reducer;
        }
        public final Integer getRawResult() { return result; }
        public final void compute() {
            final ToIntFunction<? super V> transformer;
            final IntBinaryOperator reducer;
            if ((transformer = this.transformer) != null &&
                (reducer = this.reducer) != null) {
                int r = this.basis;
                for (int i = baseIndex, f, h; batch > 0 &&
                         (h = ((f = baseLimit) + i) >>> 1) > i;) {
                    addToPendingCount(1);
                    (rights = new MapReduceValuesToIntTask<K,V>
                     (this, batch >>>= 1, baseLimit = h, f, tab,
                      rights, transformer, r, reducer)).fork();
                }
                for (Node<K,V> p; (p = advance()) != null; )
                    r = reducer.applyAsInt(r, transformer.applyAsInt(p.val));
                result = r;
                CountedCompleter<?> c;
                for (c = firstComplete(); c != null; c = c.nextComplete()) {
                    @SuppressWarnings("unchecked")
                    MapReduceValuesToIntTask<K,V>
                        t = (MapReduceValuesToIntTask<K,V>)c,
                        s = t.rights;
                    while (s != null) {
                        t.result = reducer.applyAsInt(t.result, s.result);
                        s = t.rights = s.nextRight;
                    }
                }
            }
        }
    }

    @SuppressWarnings("serial")
    static final class MapReduceEntriesToIntTask<K,V>
        extends BulkTask<K,V,Integer> {
        final ToIntFunction<Map.Entry<K,V>> transformer;
        final IntBinaryOperator reducer;
        final int basis;
        int result;
        MapReduceEntriesToIntTask<K,V> rights, nextRight;
        MapReduceEntriesToIntTask
            (BulkTask<K,V,?> p, int b, int i, int f, Node<K,V>[] t,
             MapReduceEntriesToIntTask<K,V> nextRight,
             ToIntFunction<Map.Entry<K,V>> transformer,
             int basis,
             IntBinaryOperator reducer) {
            super(p, b, i, f, t); this.nextRight = nextRight;
            this.transformer = transformer;
            this.basis = basis; this.reducer = reducer;
        }
        public final Integer getRawResult() { return result; }
        public final void compute() {
            final ToIntFunction<Map.Entry<K,V>> transformer;
            final IntBinaryOperator reducer;
            if ((transformer = this.transformer) != null &&
                (reducer = this.reducer) != null) {
                int r = this.basis;
                for (int i = baseIndex, f, h; batch > 0 &&
                         (h = ((f = baseLimit) + i) >>> 1) > i;) {
                    addToPendingCount(1);
                    (rights = new MapReduceEntriesToIntTask<K,V>
                     (this, batch >>>= 1, baseLimit = h, f, tab,
                      rights, transformer, r, reducer)).fork();
                }
                for (Node<K,V> p; (p = advance()) != null; )
                    r = reducer.applyAsInt(r, transformer.applyAsInt(p));
                result = r;
                CountedCompleter<?> c;
                for (c = firstComplete(); c != null; c = c.nextComplete()) {
                    @SuppressWarnings("unchecked")
                    MapReduceEntriesToIntTask<K,V>
                        t = (MapReduceEntriesToIntTask<K,V>)c,
                        s = t.rights;
                    while (s != null) {
                        t.result = reducer.applyAsInt(t.result, s.result);
                        s = t.rights = s.nextRight;
                    }
                }
            }
        }
    }

    @SuppressWarnings("serial")
    static final class MapReduceMappingsToIntTask<K,V>
        extends BulkTask<K,V,Integer> {
        final ToIntBiFunction<? super K, ? super V> transformer;
        final IntBinaryOperator reducer;
        final int basis;
        int result;
        MapReduceMappingsToIntTask<K,V> rights, nextRight;
        MapReduceMappingsToIntTask
            (BulkTask<K,V,?> p, int b, int i, int f, Node<K,V>[] t,
             MapReduceMappingsToIntTask<K,V> nextRight,
             ToIntBiFunction<? super K, ? super V> transformer,
             int basis,
             IntBinaryOperator reducer) {
            super(p, b, i, f, t); this.nextRight = nextRight;
            this.transformer = transformer;
            this.basis = basis; this.reducer = reducer;
        }
        public final Integer getRawResult() { return result; }
        public final void compute() {
            final ToIntBiFunction<? super K, ? super V> transformer;
            final IntBinaryOperator reducer;
            if ((transformer = this.transformer) != null &&
                (reducer = this.reducer) != null) {
                int r = this.basis;
                for (int i = baseIndex, f, h; batch > 0 &&
                         (h = ((f = baseLimit) + i) >>> 1) > i;) {
                    addToPendingCount(1);
                    (rights = new MapReduceMappingsToIntTask<K,V>
                     (this, batch >>>= 1, baseLimit = h, f, tab,
                      rights, transformer, r, reducer)).fork();
                }
                for (Node<K,V> p; (p = advance()) != null; )
                    r = reducer.applyAsInt(r, transformer.applyAsInt(p.key, p.val));
                result = r;
                CountedCompleter<?> c;
                for (c = firstComplete(); c != null; c = c.nextComplete()) {
                    @SuppressWarnings("unchecked")
                    MapReduceMappingsToIntTask<K,V>
                        t = (MapReduceMappingsToIntTask<K,V>)c,
                        s = t.rights;
                    while (s != null) {
                        t.result = reducer.applyAsInt(t.result, s.result);
                        s = t.rights = s.nextRight;
                    }
                }
            }
        }
    }

    // Unsafe mechanics
    // Unsafe机制
    private static final sun.misc.Unsafe U;
    private static final long SIZECTL;// sizeCtl
    private static final long TRANSFERINDEX;// transferIndex
    private static final long BASECOUNT;// baseCount
    private static final long CELLSBUSY;// cellsBusy
    private static final long CELLVALUE;// value
    private static final long ABASE;// Node[].class
    private static final int ASHIFT;// 31 - Integer.numberOfLeadingZeros(U.arrayIndexScale(Node[].class));

    static {
        try {
            U = sun.misc.Unsafe.getUnsafe();
            Class<?> k = ConcurrentHashMap.class;
            SIZECTL = U.objectFieldOffset
                (k.getDeclaredField("sizeCtl"));
            TRANSFERINDEX = U.objectFieldOffset
                (k.getDeclaredField("transferIndex"));
            BASECOUNT = U.objectFieldOffset
                (k.getDeclaredField("baseCount"));
            CELLSBUSY = U.objectFieldOffset
                (k.getDeclaredField("cellsBusy"));
            Class<?> ck = CounterCell.class;
            CELLVALUE = U.objectFieldOffset
                (ck.getDeclaredField("value"));
            Class<?> ak = Node[].class;
            ABASE = U.arrayBaseOffset(ak);
            int scale = U.arrayIndexScale(ak);
            if ((scale & (scale - 1)) != 0)
                throw new Error("data type scale not a power of two");
            ASHIFT = 31 - Integer.numberOfLeadingZeros(scale);
        } catch (Exception e) {
            throw new Error(e);
        }
    }
}
