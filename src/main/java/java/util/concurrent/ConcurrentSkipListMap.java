/*
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util.concurrent;
import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.Spliterator;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * 20210624
 * A. 可扩展的并发{@link ConcurrentNavigableMap} 实现。 Map根据其键的{@linkplain Comparable natural ordering} 进行排序，或者根据Map创建时提供的
 *    {@link Comparator} 进行排序，具体取决于使用的构造函数。
 * B. 此类实现了SkipLists的并发变体，为 {@code containsKey}、{@code get}、{@code put} 和 {@code remove} 操作及其变体提供预期的平均 log(n) 时间成本。
 *    插入、移除、更新和访问操作由多个线程安全地并发执行。
 * C. 迭代器和拆分器是弱一致的。
 * D. 升序键有序视图及其迭代器比降序更快。
 * E. 此类中的方法及其视图返回的所有 {@code Map.Entry} 对都表示映射生成时的快照。 它们不支持 {@code Entry.setValue} 方法。
 *   （但请注意，可以使用 {@code put}、{@code putIfAbsent} 或 {@code replace} 更改关联映射中的映射，具体取决于您需要的效果。）
 * F. 请注意，与大多数集合不同，{@code size} 方法不是恒定时间操作。 由于这些映射的异步性质，确定当前元素数量需要遍历元素，因此如果在遍历期间修改此集合，
 *    则可能会报告不准确的结果。 此外，批量操作 {@code putAll}、{@code equals}、{@code toArray}、{@code containsValue} 和 {@code clear} 不能保证以原子方式执行。
 *    例如，与 {@code putAll} 操作并发运行的迭代器可能仅查看一些添加的元素。
 * G. 此类及其视图和迭代器实现了 {@link Map} 和 {@link Iterator} 接口的所有可选方法。 与大多数其他并发集合一样，此类不允许使用 {@code null} 键或值，
 *    因为无法可靠地区分某些 null 返回值与元素的缺失。
 * H. 此类是 Java 集合框架的成员。
 */
/**
 * A.
 * A scalable concurrent {@link ConcurrentNavigableMap} implementation.
 * The map is sorted according to the {@linkplain Comparable natural
 * ordering} of its keys, or by a {@link Comparator} provided at map
 * creation time, depending on which constructor is used.
 *
 * B.
 * <p>This class implements a concurrent variant of <a
 * href="http://en.wikipedia.org/wiki/Skip_list" target="_top">SkipLists</a>
 * providing expected average <i>log(n)</i> time cost for the
 * {@code containsKey}, {@code get}, {@code put} and
 * {@code remove} operations and their variants.  Insertion, removal,
 * update, and access operations safely execute concurrently by
 * multiple threads.
 *
 * C.
 * <p>Iterators and spliterators are
 * <a href="package-summary.html#Weakly"><i>weakly consistent</i></a>.
 *
 * D.
 * <p>Ascending key ordered views and their iterators are faster than
 * descending ones.
 *
 * E.
 * <p>All {@code Map.Entry} pairs returned by methods in this class
 * and its views represent snapshots of mappings at the time they were
 * produced. They do <em>not</em> support the {@code Entry.setValue}
 * method. (Note however that it is possible to change mappings in the
 * associated map using {@code put}, {@code putIfAbsent}, or
 * {@code replace}, depending on exactly which effect you need.)
 *
 * F.
 * <p>Beware that, unlike in most collections, the {@code size}
 * method is <em>not</em> a constant-time operation. Because of the
 * asynchronous nature of these maps, determining the current number
 * of elements requires a traversal of the elements, and so may report
 * inaccurate results if this collection is modified during traversal.
 * Additionally, the bulk operations {@code putAll}, {@code equals},
 * {@code toArray}, {@code containsValue}, and {@code clear} are
 * <em>not</em> guaranteed to be performed atomically. For example, an
 * iterator operating concurrently with a {@code putAll} operation
 * might view only some of the added elements.
 *
 * G.
 * <p>This class and its views and iterators implement all of the
 * <em>optional</em> methods of the {@link Map} and {@link Iterator}
 * interfaces. Like most other concurrent collections, this class does
 * <em>not</em> permit the use of {@code null} keys or values because some
 * null return values cannot be reliably distinguished from the absence of
 * elements.
 *
 * H.
 * <p>This class is a member of the
 * <a href="{@docRoot}/../technotes/guides/collections/index.html">
 * Java Collections Framework</a>.
 *
 * @author Doug Lea
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 * @since 1.6
 */
public class ConcurrentSkipListMap<K,V> extends AbstractMap<K,V> implements ConcurrentNavigableMap<K,V>, Cloneable, Serializable {

    /**
     * 20210624
     * A. 此类实现了一个树状二维链接跳跃表，其中索引级别在与保存数据的基本节点不同的节点中表示。 采用这种方法而不是通常的基于数组的结构有两个原因：
     *      1）基于数组的实现似乎会遇到更多的复杂性和开销
     *      2）我们可以对频繁遍历的索引列表使用比用于基列表更便宜的算法列表。
     *    这是具有2个索引级别的可能列表的一些基础知识的图片：
     * Head nodes          Index nodes
     * +-+    right        +-+                      +-+
     * |2|---------------->| |--------------------->| |->null
     * +-+                 +-+                      +-+
     *  | down              |                        |
     *  v                   v                        v
     * +-+            +-+  +-+       +-+            +-+       +-+
     * |1|----------->| |->| |------>| |----------->| |------>| |->null
     * +-+            +-+  +-+       +-+            +-+       +-+
     *  v              |    |         |              |         |
     * Nodes  next     v    v         v              v         v
     * +-+  +-+  +-+  +-+  +-+  +-+  +-+  +-+  +-+  +-+  +-+  +-+
     * | |->|A|->|B|->|C|->|D|->|E|->|F|->|G|->|H|->|I|->|J|->|K|->null
     * +-+  +-+  +-+  +-+  +-+  +-+  +-+  +-+  +-+  +-+  +-+  +-+
     * B. 基本列表使用HM链接有序集算法的变体。 参见 Tim Harris，“非阻塞链表的实用实现”http://www.cl.cam.ac.uk/~tlh20/publications.html 和 Maged Michael“
     *    高性能动态无锁哈希表和列表- 基于集”http://www.research.ibm.com/people/m/michael/pubs.htm。 这些列表中的基本思想是在删除时标记已删除节点的“下一个”指针
     *    以避免与并发插入发生冲突，并在遍历时跟踪三元组（前驱、节点、后继）以检测何时以及如何取消链接这些删除的节点。
     * C. 节点不使用标记位来标记列表删除（使用 AtomicMarkedReference 可能会很慢且占用大量空间），而是使用直接 CAS'able next 指针。
     *    在删除时，它们不是标记指针，而是拼接另一个节点，该节点可以被认为是代表标记的指针（通过使用其他不可能的字段值来表示这一点）。
     *    使用普通节点的行为大致类似于标记指针的“装箱”实现，但仅在删除节点时才使用新节点，而不是针对每个链接。 这需要更少的空间并支持更快的遍历。
     *    即使 JVM 更好地支持标记引用，使用这种技术的遍历可能仍然更快，因为任何搜索只需要比其他方式所需的多一个节点（检查尾随标记）而不是取消屏蔽标记位或每次读取时的任何内容。
     * D. 这种方法保留了 HM 算法中所需的基本属性，即更改已删除节点的下一个指针，以便它的任何其他 CAS 都将失败，但通过将指针更改为指向不同节点而不是通过标记来实现该想法.
     *    虽然可以通过定义没有键/值字段的标记节点来进一步压缩空间，但额外的类型测试开销是不值得的。 删除标记在遍历过程中很少遇到，通常会很快被垃圾收集。
     *    （请注意，此技术在没有垃圾收集的系统中效果不佳。）
     * E. 除了使用删除标记之外，列表还使用值字段的空值来指示删除，其风格类似于典型的延迟删除方案。 如果一个节点的值为空，那么它被认为是逻辑删除并被忽略，
     *    即使它仍然可以访问。 这保持了对并发替换与删除操作的适当控制——如果删除通过空字段击败它，则尝试替换必须失败，并且删除必须返回该字段中保存的最后一个非空值。
     *    （注意：Null，而不是一些特殊的标记，用于此处的值字段，因为它恰好符合 Map API 的要求，即如果没有映射，则方法 get 返回 null，
     *    这允许节点即使在删除时也保持并发可读. 在这里使用任何其他标记值充其量只是混乱。）
     * F. 这是删除具有前驱b和后继f的节点n的事件序列，最初：
     *        +------+       +------+      +------+
     *   ...  |   b  |------>|   n  |----->|   f  | ...
     *        +------+       +------+      +------+
     *    1. CAS n 的值字段从非null到null。 从现在开始，没有遇到节点的公共操作会认为此映射存在。 但是，其他正在进行的插入和删除操作可能仍会修改 n 的下一个指针。
     *    2. CAS n 的next 指针指向一个新的标记节点。 从这一点开始，没有其他节点可以附加到 n。 这避免了基于 CAS 的链表中的删除错误。
     *        +------+       +------+      +------+       +------+
     *   ...  |   b  |------>|   n  |----->|marker|------>|   f  | ...
     *        +------+       +------+      +------+       +------+
     *    3. CAS b 在 n 及其标记上的下一个指针。 从这点开始，没有新的遍历会遇到 n，最终可以被 GCed。
     *        +------+                                    +------+
     *   ...  |   b  |----------------------------------->|   f  | ...
     *        +------+                                    +------+
     * G. 步骤 1 的失败会导致简单的重试，因为与另一个操作的竞争失败。 步骤 2-3 可能会失败，因为其他一些线程在遍历具有null值的节点期间注意到并通过标记和或取消链接来提供帮助。
     *    这种帮助确保没有线程会卡住等待删除线程的进度。 标记节点的使用使帮助代码稍微复杂化，因为遍历必须跟踪最多四个节点（b，n，marker，f）的一致读取，
     *    而不仅仅是（b，n，f），尽管标记的下一个字段是 不可变的，一旦下一个字段被 CAS 指向一个标记，它就永远不会再改变，所以这需要更少的关注。
     * H. 跳跃表为该方案添加索引，以便基本级别的遍历从靠近被发现、插入或删除的位置开始——通常基本级别的遍历只遍历几个节点。 这不会改变基本算法，
     *    除了需要确保基遍历从未（结构上）删除的前辈（此处为 b）开始，否则在处理删除后重试。
     * I. 索引级别保持为具有可变下一个字段的列表，使用 CAS 链接和取消链接。 在索引列表操作中允许竞争，这可能（很少）无法链接到新的索引节点或删除一个。
     *   （我们当然不能对数据节点这样做。）但是，即使发生这种情况，索引列表仍保持排序，因此可以正确地用作索引。 这可能会影响性能，但由于跳过列表无论如何都是概率性的，
     *    因此最终结果是在争用情况下，有效的“p”值可能低于其标称值。 并且比赛窗口保持得足够小，以至于在实践中这些失败很少见，即使在很多竞争下也是如此。
     * J. 由于索引，重试（对于基本列表和索引列表）相对便宜的事实允许对重试逻辑进行一些小的简化。 在大多数“帮助”CAS 之后执行遍历重新启动。 这并不总是绝对必要的，
     *    但隐式退避往往有助于减少其他下游失败的 CAS，足以超过重启成本。 这会使最坏的情况恶化，但似乎甚至可以改善竞争激烈的情况。
     * K. 与大多数跳跃表实现不同，这里的索引插入和删除需要在基本级别操作之后进行单独的遍历，以添加或删除索引节点。 这增加了单线程开销，但通过缩小干扰窗口提高了竞争多线程的性能，
     *    并允许删除以确保所有索引节点在从公共删除操作返回时都将无法访问，从而避免不必要的垃圾保留。 这在这里比在其他一些数据结构中更重要，
     *    因为我们不能将引用用户键的节点字段清空，因为它们可能仍被其他正在进行的遍历读取。
     * L. 索引使用跳跃表参数，在使用比通常更稀疏的索引时保持良好的搜索性能：硬连线参数 k=1，p=0.5（参见方法 doPut）意味着大约四分之一的节点具有索引。
     *    在那些这样做的人中，一半有一个级别，四分之一有两个，依此类推（参见 Pugh 的跳过列表食谱，第 3.4 节）。 Map的预期总空间需求略小于java.util.TreeMap的当前实现。
     * M. 更改索引的级别（即树状结构的高度）也使用 CAS。 头部索引的初始水平/高度为 1。 创建高度大于当前级别的索引会通过 CAS 对新的最顶部头部添加一个级别到头部索引。
     *    为了在大量删除后保持良好的性能，如果最顶层看起来是空的，删除方法会试探性地尝试降低高度。 这可能会遇到可能（但很少见）减少和“丢失”级别的竞争，
     *    就像它即将包含一个索引（然后永远不会遇到）。 这不会造成结构性损害，而且在实践中似乎是比允许水平无限制增长更好的选择。
     * N. 所有这些的代码比您想要的要冗长。 大多数操作需要定位元素（或插入元素的位置）。 无法很好地分解出执行此操作的代码，
     *    因为后续使用需要无法一次全部返回的前驱和/或后继和/或值字段的快照，至少在没有创建另一个对象来保存它们的情况下不会 -- 创建这么小的对象对于基本的内部搜索操作来
     *    说是一个特别糟糕的主意，因为它会增加 GC 开销。 （这是我希望 Java 有宏的少数几次之一。）相反，一些遍历代码在插入和删除操作中交错。
     *    处理所有重试条件的控制逻辑有时是曲折的。 大多数搜索分为两部分。 findPredecessor() 仅搜索索引节点，返回键的基本级前驱。
     *    findNode() 完成了基本级别的搜索。 即使有这种分解，也有相当数量的近乎重复的代码来处理变体。
     * O. 为了在不跨线程干扰的情况下生成随机值，我们使用 JDK 内线程本地随机支持（通过“辅助种子”，以避免干扰用户级 ThreadLocalRandom。）
     * P. 此类的先前版本在使用比较器与 Comparables 时将不可比较的键与其比较器一起包装，以模拟 Comparables。 但是，现在 JVM 似乎可以更好地处理将比较器
     *    与可比较选择注入搜索循环中。 静态方法 cpr(comarator, x, y) 用于所有比较，只要比较器参数设置在循环之外（因此有时作为参数传递给内部方法），
     *    它就可以很好地工作，以避免字段重新读取。
     * Q. 有关与该算法共享至少几个特征的算法的解释，请参阅 Mikhail Fomitchev 的论文 (http://www.cs.yorku.ca/~mikhail/)、
     *    Keir Fraser 的论文 (http://www.cl.cam .ac.uk/users/kaf24/) 和 Hakan Sundell 的论文 (http://www.cs.chalmers.se/~phs/)。
     * R. 考虑到树状索引节点的使用，您可能想知道为什么不使用某种搜索树来代替，这将支持更快的搜索操作。 原因是搜索树没有已知的高效无锁插入和删除算法。
     *    索引节点的“向下”链接的不变性（与真实树中可变的“左”字段相反）使得仅使用 CAS 操作就可以处理。
     * S. 局部变量的符号指南:
     *      节点：b、n、f 表示前驱、节点、后继
     *      索引：q、r、d 表示索引节点，右、下。
     *      t 表示另一个索引节点
     *      Head: h
     *      Levels: j
     *      Keys: k, key
     *      Values: v, value
     *      Comparisons: c
     */
    /*
     * A.
     * This class implements a tree-like two-dimensionally linked skip
     * list in which the index levels are represented in separate
     * nodes from the base nodes holding data.  There are two reasons
     * for taking this approach instead of the usual array-based
     * structure: 1) Array based implementations seem to encounter
     * more complexity and overhead 2) We can use cheaper algorithms
     * for the heavily-traversed index lists than can be used for the
     * base lists.  Here's a picture of some of the basics for a
     * possible list with 2 levels of index:
     *
     * Head nodes          Index nodes
     * +-+    right        +-+                      +-+
     * |2|---------------->| |--------------------->| |->null
     * +-+                 +-+                      +-+
     *  | down              |                        |
     *  v                   v                        v
     * +-+            +-+  +-+       +-+            +-+       +-+
     * |1|----------->| |->| |------>| |----------->| |------>| |->null
     * +-+            +-+  +-+       +-+            +-+       +-+
     *  v              |    |         |              |         |
     * Nodes  next     v    v         v              v         v
     * +-+  +-+  +-+  +-+  +-+  +-+  +-+  +-+  +-+  +-+  +-+  +-+
     * | |->|A|->|B|->|C|->|D|->|E|->|F|->|G|->|H|->|I|->|J|->|K|->null
     * +-+  +-+  +-+  +-+  +-+  +-+  +-+  +-+  +-+  +-+  +-+  +-+
     *
     * B.
     * The base lists use a variant of the HM linked ordered set
     * algorithm. See Tim Harris, "A pragmatic implementation of
     * non-blocking linked lists"
     * http://www.cl.cam.ac.uk/~tlh20/publications.html and Maged
     * Michael "High Performance Dynamic Lock-Free Hash Tables and
     * List-Based Sets"
     * http://www.research.ibm.com/people/m/michael/pubs.htm.  The
     * basic idea in these lists is to mark the "next" pointers of
     * deleted nodes when deleting to avoid conflicts with concurrent
     * insertions, and when traversing to keep track of triples
     * (predecessor, node, successor) in order to detect when and how
     * to unlink these deleted nodes.
     *
     * C.
     * Rather than using mark-bits to mark list deletions (which can
     * be slow and space-intensive using AtomicMarkedReference), nodes
     * use direct CAS'able next pointers.  On deletion, instead of
     * marking a pointer, they splice in another node that can be
     * thought of as standing for a marked pointer (indicating this by
     * using otherwise impossible field values).  Using plain nodes
     * acts roughly like "boxed" implementations of marked pointers,
     * but uses new nodes only when nodes are deleted, not for every
     * link.  This requires less space and supports faster
     * traversal. Even if marked references were better supported by
     * JVMs, traversal using this technique might still be faster
     * because any search need only read ahead one more node than
     * otherwise required (to check for trailing marker) rather than
     * unmasking mark bits or whatever on each read.
     *
     * D.
     * This approach maintains the essential property needed in the HM
     * algorithm of changing the next-pointer of a deleted node so
     * that any other CAS of it will fail, but implements the idea by
     * changing the pointer to point to a different node, not by
     * marking it.  While it would be possible to further squeeze
     * space by defining marker nodes not to have key/value fields, it
     * isn't worth the extra type-testing overhead.  The deletion
     * markers are rarely encountered during traversal and are
     * normally quickly garbage collected. (Note that this technique
     * would not work well in systems without garbage collection.)
     *
     * E.
     * In addition to using deletion markers, the lists also use
     * nullness of value fields to indicate deletion, in a style
     * similar to typical lazy-deletion schemes.  If a node's value is
     * null, then it is considered logically deleted and ignored even
     * though it is still reachable. This maintains proper control of
     * concurrent replace vs delete operations -- an attempted replace
     * must fail if a delete beat it by nulling field, and a delete
     * must return the last non-null value held in the field. (Note:
     * Null, rather than some special marker, is used for value fields
     * here because it just so happens to mesh with the Map API
     * requirement that method get returns null if there is no
     * mapping, which allows nodes to remain concurrently readable
     * even when deleted. Using any other marker value here would be
     * messy at best.)
     *
     * F.
     * Here's the sequence of events for a deletion of node n with
     * predecessor b and successor f, initially:
     *
     *        +------+       +------+      +------+
     *   ...  |   b  |------>|   n  |----->|   f  | ...
     *        +------+       +------+      +------+
     *
     * 1. CAS n's value field from non-null to null.
     *    From this point on, no public operations encountering
     *    the node consider this mapping to exist. However, other
     *    ongoing insertions and deletions might still modify
     *    n's next pointer.
     *
     * 2. CAS n's next pointer to point to a new marker node.
     *    From this point on, no other nodes can be appended to n.
     *    which avoids deletion errors in CAS-based linked lists.
     *
     *        +------+       +------+      +------+       +------+
     *   ...  |   b  |------>|   n  |----->|marker|------>|   f  | ...
     *        +------+       +------+      +------+       +------+
     *
     * 3. CAS b's next pointer over both n and its marker.
     *    From this point on, no new traversals will encounter n,
     *    and it can eventually be GCed.
     *        +------+                                    +------+
     *   ...  |   b  |----------------------------------->|   f  | ...
     *        +------+                                    +------+
     *
     * G.
     * A failure at step 1 leads to simple retry due to a lost race
     * with another operation. Steps 2-3 can fail because some other
     * thread noticed during a traversal a node with null value and
     * helped out by marking and/or unlinking.  This helping-out
     * ensures that no thread can become stuck waiting for progress of
     * the deleting thread.  The use of marker nodes slightly
     * complicates helping-out code because traversals must track
     * consistent reads of up to four nodes (b, n, marker, f), not
     * just (b, n, f), although the next field of a marker is
     * immutable, and once a next field is CAS'ed to point to a
     * marker, it never again changes, so this requires less care.
     *
     * H.
     * Skip lists add indexing to this scheme, so that the base-level
     * traversals start close to the locations being found, inserted
     * or deleted -- usually base level traversals only traverse a few
     * nodes. This doesn't change the basic algorithm except for the
     * need to make sure base traversals start at predecessors (here,
     * b) that are not (structurally) deleted, otherwise retrying
     * after processing the deletion.
     *
     * I.
     * Index levels are maintained as lists with volatile next fields,
     * using CAS to link and unlink.  Races are allowed in index-list
     * operations that can (rarely) fail to link in a new index node
     * or delete one. (We can't do this of course for data nodes.)
     * However, even when this happens, the index lists remain sorted,
     * so correctly serve as indices.  This can impact performance,
     * but since skip lists are probabilistic anyway, the net result
     * is that under contention, the effective "p" value may be lower
     * than its nominal value. And race windows are kept small enough
     * that in practice these failures are rare, even under a lot of
     * contention.
     *
     * J.
     * The fact that retries (for both base and index lists) are
     * relatively cheap due to indexing allows some minor
     * simplifications of retry logic. Traversal restarts are
     * performed after most "helping-out" CASes. This isn't always
     * strictly necessary, but the implicit backoffs tend to help
     * reduce other downstream failed CAS's enough to outweigh restart
     * cost.  This worsens the worst case, but seems to improve even
     * highly contended cases.
     *
     * K.
     * Unlike most skip-list implementations, index insertion and
     * deletion here require a separate traversal pass occurring after
     * the base-level action, to add or remove index nodes.  This adds
     * to single-threaded overhead, but improves contended
     * multithreaded performance by narrowing interference windows,
     * and allows deletion to ensure that all index nodes will be made
     * unreachable upon return from a public remove operation, thus
     * avoiding unwanted garbage retention. This is more important
     * here than in some other data structures because we cannot null
     * out node fields referencing user keys since they might still be
     * read by other ongoing traversals.
     *
     * L.
     * Indexing uses skip list parameters that maintain good search
     * performance while using sparser-than-usual indices: The
     * hardwired parameters k=1, p=0.5 (see method doPut) mean
     * that about one-quarter of the nodes have indices. Of those that
     * do, half have one level, a quarter have two, and so on (see
     * Pugh's Skip List Cookbook, sec 3.4).  The expected total space
     * requirement for a map is slightly less than for the current
     * implementation of java.util.TreeMap.
     *
     * M.
     * Changing the level of the index (i.e, the height of the
     * tree-like structure) also uses CAS. The head index has initial
     * level/height of one. Creation of an index with height greater
     * than the current level adds a level to the head index by
     * CAS'ing on a new top-most head. To maintain good performance
     * after a lot of removals, deletion methods heuristically try to
     * reduce the height if the topmost levels appear to be empty.
     * This may encounter races in which it possible (but rare) to
     * reduce and "lose" a level just as it is about to contain an
     * index (that will then never be encountered). This does no
     * structural harm, and in practice appears to be a better option
     * than allowing unrestrained growth of levels.
     *
     * N.
     * The code for all this is more verbose than you'd like. Most
     * operations entail locating an element (or position to insert an
     * element). The code to do this can't be nicely factored out
     * because subsequent uses require a snapshot of predecessor
     * and/or successor and/or value fields which can't be returned
     * all at once, at least not without creating yet another object
     * to hold them -- creating such little objects is an especially
     * bad idea for basic internal search operations because it adds
     * to GC overhead.  (This is one of the few times I've wished Java
     * had macros.) Instead, some traversal code is interleaved within
     * insertion and removal operations.  The control logic to handle
     * all the retry conditions is sometimes twisty. Most search is
     * broken into 2 parts. findPredecessor() searches index nodes
     * only, returning a base-level predecessor of the key. findNode()
     * finishes out the base-level search. Even with this factoring,
     * there is a fair amount of near-duplication of code to handle
     * variants.
     *
     * O.
     * To produce random values without interference across threads,
     * we use within-JDK thread local random support (via the
     * "secondary seed", to avoid interference with user-level
     * ThreadLocalRandom.)
     *
     * P.
     * A previous version of this class wrapped non-comparable keys
     * with their comparators to emulate Comparables when using
     * comparators vs Comparables.  However, JVMs now appear to better
     * handle infusing comparator-vs-comparable choice into search
     * loops. Static method cpr(comparator, x, y) is used for all
     * comparisons, which works well as long as the comparator
     * argument is set up outside of loops (thus sometimes passed as
     * an argument to internal methods) to avoid field re-reads.
     *
     * Q.
     * For explanation of algorithms sharing at least a couple of
     * features with this one, see Mikhail Fomitchev's thesis
     * (http://www.cs.yorku.ca/~mikhail/), Keir Fraser's thesis
     * (http://www.cl.cam.ac.uk/users/kaf24/), and Hakan Sundell's
     * thesis (http://www.cs.chalmers.se/~phs/).
     *
     * R.
     * Given the use of tree-like index nodes, you might wonder why
     * this doesn't use some kind of search tree instead, which would
     * support somewhat faster search operations. The reason is that
     * there are no known efficient lock-free insertion and deletion
     * algorithms for search trees. The immutability of the "down"
     * links of index nodes (as opposed to mutable "left" fields in
     * true trees) makes this tractable using only CAS operations.
     *
     * S.
     * Notation guide for local variables
     * Node:         b, n, f    for  predecessor, node, successor
     * Index:        q, r, d    for index node, right, down.
     *               t          for another index node
     * Head:         h
     * Levels:       j
     * Keys:         k, key
     * Values:       v, value
     * Comparisons:  c
     */

    private static final long serialVersionUID = -8627078645895051609L;

    /**
     * Special value used to identify base-level header
     */
    // 用于标识基本级别标头的特殊值
    private static final Object BASE_HEADER = new Object();

    /**
     * The topmost head index of the skiplist.
     */
    // 跳过列表的最顶层索引。
    private transient volatile HeadIndex<K,V> head;

    /**
     * The comparator used to maintain order in this map, or null if
     * using natural ordering.  (Non-private to simplify access in
     * nested classes.)
     * @serial
     */
    // 用于维护此映射中的顺序的比较器，如果使用自然排序，则为 null。 （非私有以简化嵌套类中的访问。）
    final Comparator<? super K> comparator;

    /** Lazily initialized key set */
    // 延迟初始化的key set
    private transient KeySet<K> keySet;

    /** Lazily initialized entry set */
    // 延迟初始化的entry set
    private transient EntrySet<K,V> entrySet;

    /** Lazily initialized values collection */
    // 延迟初始化的value set
    private transient Values<V> values;

    /** Lazily initialized descending key set */
    // 延迟初始化的逆序的key set
    private transient ConcurrentNavigableMap<K,V> descendingMap;

    /**
     * 20210624
     * 初始化或重置状态。 构造函数、clone、clear、readObject 需要。 和 ConcurrentSkipListSet.clone。 （注意比较器必须单独初始化。）
     */
    /**
     * Initializes or resets state. Needed by constructors, clone,
     * clear, readObject. and ConcurrentSkipListSet.clone.
     * (Note that comparator must be separately initialized.)
     */
    private void initialize() {
        keySet = null;
        entrySet = null;
        values = null;
        descendingMap = null;

        // 创建level为1的索引结点, 指向新创建的BASE_HEADER数据头结点
        // HeadIndex: { node: [Node: {key: null, value: BASE_HEADER, next: null}], down: null, right: null, level: 1 }
        head = new HeadIndex<K,V>(
                new Node<K,V>(null, BASE_HEADER, null),
                null, null, 1
        );
    }

    /**
     * compareAndSet head node
     */
    private boolean casHead(HeadIndex<K,V> cmp, HeadIndex<K,V> val) {
        return UNSAFE.compareAndSwapObject(this, headOffset, cmp, val);
    }

    /* ---------------- Nodes -------------- */

    /**
     * 20210624
     * 节点保存键和值，并按排序顺序单独链接，可能带有一些中间标记节点。该列表由一个可作为head.node访问的虚拟节点领导。 value字段仅声明为Object，
     * 因为它为标记和标题节点采用特殊的非V值。
     */
    /**
     * Nodes hold keys and values, and are singly linked in sorted
     * order, possibly with some intervening marker nodes. The list is
     * headed by a dummy node accessible as head.node. The value field
     * is declared only as Object because it takes special non-V
     * values for marker and header nodes.
     */
    static final class Node<K,V> {
        final K key;
        volatile Object value;
        volatile Node<K,V> next;

        /**
         * Creates a new regular node.
         */
        Node(K key, Object value, Node<K,V> next) {
            this.key = key;
            this.value = value;
            this.next = next;
        }

        /**
         * 20210624
         * 创建一个新的标记节点。标记的区别在于其值字段指向自身。标记节点也有null键，这一事实在一些地方被利用，但这并不能将标记与基础级头节点 (head.node) 区分开来，
         * 后者也有一个null键。
         */
        /**
         * Creates a new marker node. A marker is distinguished by
         * having its value field point to itself.  Marker nodes also
         * have null keys, a fact that is exploited in a few places,
         * but this doesn't distinguish markers from the base-level
         * header node (head.node), which also has a null key.
         */
        Node(Node<K,V> next) {
            this.key = null;
            this.value = this;
            this.next = next;
        }

        /**
         * compareAndSet value field
         */
        boolean casValue(Object cmp, Object val) {
            return UNSAFE.compareAndSwapObject(this, valueOffset, cmp, val);
        }

        /**
         * compareAndSet next field
         */
        boolean casNext(Node<K,V> cmp, Node<K,V> val) {
            return UNSAFE.compareAndSwapObject(this, nextOffset, cmp, val);
        }

        /**
         * 20210624
         * 如果此节点是标记，则返回 true。在任何当前检查标记的代码中实际上并未调用此方法，因为调用者已经读取了value字段并且需要使用该读取（这里不是另一个完成的），
         * 因此直接测试value是否指向 node。
         */
        /**
         * Returns true if this node is a marker. This method isn't
         * actually called in any current code checking for markers
         * because callers will have already read value field and need
         * to use that read (not another done here) and so directly
         * test if value points to node.
         *
         * @return true if this node is a marker node
         */
        boolean isMarker() {
            return value == this;
        }

        /**
         * Returns true if this node is the header of base-level list.
         * @return true if this node is header node
         */
        boolean isBaseHeader() {
            return value == BASE_HEADER;
        }

        /**
         * Tries to append a deletion marker to this node.
         * @param f the assumed current successor of this node
         * @return true if successful
         */
        // 尝试将删除标记附加到此节点: next指针指向自身
        boolean appendMarker(Node<K,V> f) {
            return casNext(f, new Node<K,V>(f));
        }

        /**
         * 20210624
         * 通过附加标记或取消与前任的链接来帮助删除。当值字段为null时，在遍历期间调用此方法。
         */
        /**
         * Helps out a deletion by appending marker or unlinking from
         * predecessor. This is called during traversals when value
         * field seen to be null.
         * @param b predecessor 前驱
         * @param f successor 后继
         */
        // 通过标记后继结点辅助删除实例结点, 在实例结点value为null时调用
        void helpDelete(Node<K,V> b, Node<K,V> f) {

            /**
             * 20210624
             * 重新检查链接，然后在每次调用时只执行一个帮助阶段，可以将帮助线程之间的CAS干扰降至最低。
             */
            /*
             * Rechecking links and then doing only one of the
             * help-out stages per call tends to minimize CAS
             * interference among helping threads.
             */
            // 前驱b, 后继f, 当前要删除的结点this, 如果f还为后继, b仍为前驱, 说明this还没被其他线程删除
            if (f == next && this == b.next) {
                // 如果f为null(bug?), 或者f不为null且f值不为自身, 说明f仍未标记, 则构造新的后继结点, 另原本n的后继f为标记结点
                if (f == null || f.value != f) // not already marked 尚未标记
                    casNext(f, new Node<K,V>(f));

                // 如果f不为null, 且f值为自身, 则链接n的前驱与f.next(新构建的f结点, 其next还是指向正常的node结点), 此时完成n与旧f的脱钩
                else
                    b.casNext(this, f.next);

                // 而f为null, 且n也为null的情况, 可视它们为null的链尾, 不需要处理
            }
        }

        /**
         * 20210624
         * 如果此节点包含有效的键值对，则返回值，否则返回 null。
         */
        /**
         * Returns value if this node contains a valid key-value pair,
         * else null.
         * @return this node's value if it isn't a marker or header or
         * is deleted, else null
         */
        V getValidValue() {
            Object v = value;
            if (v == this || v == BASE_HEADER)
                return null;
            @SuppressWarnings("unchecked") V vv = (V)v;
            return vv;
        }

        /**
         * 20210624
         * 如果此节点持有有效值，则创建并返回一个新的 SimpleImmutableEntry 持有当前映射，否则为 null。
         * => 维护不可变键和值的条目。 此类不支持方法 setValue。 此类在返回键值映射的线程安全快照的方法中可能很方便。
         */
        /**
         * Creates and returns a new SimpleImmutableEntry holding current
         * mapping if this node holds a valid value, else null.
         * @return new entry or null
         */
        AbstractMap.SimpleImmutableEntry<K,V> createSnapshot() {
            Object v = value;
            if (v == null || v == this || v == BASE_HEADER)
                return null;
            @SuppressWarnings("unchecked") V vv = (V)v;
            return new AbstractMap.SimpleImmutableEntry<K,V>(key, vv);
        }

        // UNSAFE mechanics

        private static final sun.misc.Unsafe UNSAFE;
        private static final long valueOffset;// value
        private static final long nextOffset;// next

        static {
            try {
                UNSAFE = sun.misc.Unsafe.getUnsafe();
                Class<?> k = Node.class;
                valueOffset = UNSAFE.objectFieldOffset
                    (k.getDeclaredField("value"));
                nextOffset = UNSAFE.objectFieldOffset
                    (k.getDeclaredField("next"));
            } catch (Exception e) {
                throw new Error(e);
            }
        }
    }

    /* ---------------- Indexing -------------- */

    /**
     * 20210624
     * 索引节点表示跳过列表的级别。请注意，即使节点和索引都具有前向字段，但它们具有不同的类型并以不同的方式处理，通过将字段放置在共享抽象类中无法很好地捕获它们。
     */
    /**
     * Index nodes represent the levels of the skip list.  Note that
     * even though both Nodes and Indexes have forward-pointing
     * fields, they have different types and are handled in different
     * ways, that can't nicely be captured by placing field in a
     * shared abstract class.
     */
    static class Index<K,V> {
        final Node<K,V> node;
        final Index<K,V> down;
        volatile Index<K,V> right;

        /**
         * Creates index node with given values.
         */
        // 创建具有给定值的索引节点。
        Index(Node<K,V> node, Index<K,V> down, Index<K,V> right) {
            this.node = node;
            this.down = down;
            this.right = right;
        }

        /**
         * compareAndSet right field
         */
        // compareAndSet 右字段
        final boolean casRight(Index<K,V> cmp, Index<K,V> val) {
            return UNSAFE.compareAndSwapObject(this, rightOffset, cmp, val);
        }

        /**
         * Returns true if the node this indexes has been deleted.
         * @return true if indexed node is known to be deleted
         */
        // 如果此索引的节点已被删除，则返回 true。
        final boolean indexesDeletedNode() {
            return node.value == null;
        }

        /**
         * 20210624
         * 尝试将CAS newSucc作为继任者。为了最大限度地减少可能会丢失此索引节点的取消链接的竞争，如果已知被索引的节点已被删除，则它不会尝试链接。
         */
        /**
         * Tries to CAS newSucc as successor.  To minimize races with
         * unlink that may lose this index node, if the node being
         * indexed is known to be deleted, it doesn't try to link in.
         * @param succ the expected current successor
         * @param newSucc the new successor
         * @return true if successful
         */
        // CAS更新实例结点后继succ为newSucc
        final boolean link(Index<K,V> succ, Index<K,V> newSucc) {
            Node<K,V> n = node;
            newSucc.right = succ;
            return n.value != null && casRight(succ, newSucc);
        }

        /**
         * 20210624
         * 尝试向 CAS 正确的字段跳过明显的后继成功。 如果已知此节点已被删除，则失败（强制调用者进行回溯）。
         */
        /**
         * Tries to CAS right field to skip over apparent successor
         * succ.  Fails (forcing a retraversal by caller) if this node
         * is known to be deleted.
         * @param succ the expected current successor
         * @return true if successful
         */
        // CAS解除后继succ的链接
        final boolean unlink(Index<K,V> succ) {
            return node.value != null && casRight(succ, succ.right);
        }

        // Unsafe mechanics
        private static final sun.misc.Unsafe UNSAFE;
        private static final long rightOffset;// right
        static {
            try {
                UNSAFE = sun.misc.Unsafe.getUnsafe();
                Class<?> k = Index.class;
                rightOffset = UNSAFE.objectFieldOffset
                    (k.getDeclaredField("right"));
            } catch (Exception e) {
                throw new Error(e);
            }
        }
    }

    /* ---------------- Head nodes -------------- */

    /**
     * 20210624
     * 指向每个级别的节点会跟踪它们的级别。
     */
    /**
     * Nodes heading each level keep track of their level.
     */
    static final class HeadIndex<K,V> extends Index<K,V> {
        final int level;
        HeadIndex(Node<K,V> node, Index<K,V> down, Index<K,V> right, int level) {
            super(node, down, right);
            this.level = level;
        }
    }

    /* ---------------- Comparison utilities -------------- */

    /**
     * 20210624
     * 如果为 null，则使用比较器或自然排序进行比较。 仅由已执行所需类型检查的方法调用。
     */
    /**
     * Compares using comparator or natural ordering if null.
     * Called only by methods that have performed required type checks.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    static final int cpr(Comparator c, Object x, Object y) {
        return (c != null) ? c.compare(x, y) : ((Comparable)x).compareTo(y);
    }

    /* ---------------- Traversal -------------- */

    /**
     * 20210626
     * 返回键严格小于给定键的基本级节点，如果没有这样的节点，则返回BASE_HEADER。 还将索引取消链接到沿途发现的已删除节点。 调用者依赖于清除已删除节点的索引的这种副作用。
     */
    /**
     * Returns a base-level node with key strictly less than given key,
     * or the base-level header if there is no such node.  Also
     * unlinks indexes to deleted nodes found along the way.  Callers
     * rely on this side-effect of clearing indices to deleted nodes.
     * @param key the key
     * @return a predecessor of key
     */
    // 查找比刚好key小一点的数据结点(key前驱), 如果找不到则会返回BASE_HEADER, 查找同时还会清除value为null的数据结点的索引结点
    private Node<K,V> findPredecessor(Object key, Comparator<? super K> cmp) {
        if (key == null)
            throw new NullPointerException(); // don't postpone errors

        // 开始自旋, 键key, 比较器cmp
        for (;;) {
            // 从head指针开始遍历, 当前遍历结点q, q的后继r, q的下结点d, 右结点r对应的数据结点n, n键k
            for (Index<K,V> q = head, r = q.right, d;;) {
                // 如果q存在后继r, 分两种情况处理, value值为null的会被清除, 否则比较键值继续往后遍历同一层索引
                if (r != null) {
                    Node<K,V> n = r.node;
                    K k = n.key;

                    // 如果n结点value为null, 说明n结点是删除的, 则CAS解除后继r的链接
                    if (n.value == null) {
                        if (!q.unlink(r))
                            // 如果CAS更新失败, 说明q或者r已被其他线程改变, 则进入下一轮自旋, 重新获取head指针, 重新遍历
                            break;           // restart

                        // 如果CAS更新成功, 则获取最新r指针为q最新的后继, 继续下一轮遍历
                        r = q.right;         // reread r
                        continue;
                    }

                    // n结点value不为null, 则使用比较器或者自然排序比较key与r.node的键, 如果比较结果大于0, 说明r.node键k小了(用于定位r的位置, 保证r.key刚好小于key), 则继续往前遍历, 直到r.node键刚好等于key或者大于key
                    if (cpr(cmp, key, k) > 0) {
                        q = r;
                        r = r.right;
                        continue;
                    }
                }

                // 经过上面的判断, 因为r.key刚好等于或者大于key, 此时q肯定为比key小的结点, 如果q下结点为null, 说明最后一层索引层, 此时返回q的数据结点即可
                if ((d = q.down) == null)
                    return q.node;

                // 经过上面的判断, 因为r.key刚好等于或者大于key, 此时q肯定为比key小的结点, 如果q下结点不为null, 说明还没达到最后一层索引层, 此时索引往下走一层
                q = d;
                r = d.right;
            }
        }
    }

    /**
     * 20210626
     * A. 如果没有，则返回持有 key 或 null 的节点，清除沿途看到的任何已删除节点。 从 findPredecessor 返回的前驱开始，在基本级别重复遍历查找键，
     *    处理遇到的基本级别删除。 一些调用者依赖于清除已删除节点的这种副作用。
     * B. 重新启动发生在以节点 n 为中心的遍历步骤，如果：
     *   (1) 在读取 n 的 next 字段后，n不再被假定为前驱b的当前后继，这意味着我们没有一致的3节点快照，因此无法取消遇到任何后续删除的节点。
     *   (2) n的值字段为空，表示n被删除，在这种情况下，我们在重试之前帮助正在进行的结构删除。即使在某些情况下这种取消链接不需要重新启动，它们也不会在这里整理，
     *       因为这样做通常不会超过重新启动的成本。
     *   (3) n是一个标记或n的前驱值字段为null，表明（除其他可能性外）findPredecessor 返回了一个已删除的节点。我们无法解除节点的链接，因为我们不知道它的前任，
     *       所以依靠另一个调用 findPredecessor 来注意并返回一些较早的前任，它会这样做。仅在循环开始时才严格需要此检查（并且根本不需要 b.value 检查），
     *       但每次迭代都会执行此检查，以帮助避免无法更改链接的调用者与其他线程的争用，所以无论如何都会重试。
     * C. doPut、doRemove 和 findNear 中的遍历循环都包含相同的三种检查。 特殊版本出现在 findFirst 和 findLast 及其变体中。 他们不能轻易共享代码，
     *    因为每个人都使用按执行顺序发生在本地人中的字段的读取。
     */
    /**
     * A.
     * Returns node holding key or null if no such, clearing out any
     * deleted nodes seen along the way.  Repeatedly traverses at
     * base-level looking for key starting at predecessor returned
     * from findPredecessor, processing base-level deletions as
     * encountered. Some callers rely on this side-effect of clearing
     * deleted nodes.
     *
     * B.
     * Restarts occur, at traversal step centered on node n, if:
     *
     *   (1) After reading n's next field, n is no longer assumed
     *       predecessor b's current successor, which means that
     *       we don't have a consistent 3-node snapshot and so cannot
     *       unlink any subsequent deleted nodes encountered.
     *
     *   (2) n's value field is null, indicating n is deleted, in
     *       which case we help out an ongoing structural deletion
     *       before retrying.  Even though there are cases where such
     *       unlinking doesn't require restart, they aren't sorted out
     *       here because doing so would not usually outweigh cost of
     *       restarting.
     *
     *   (3) n is a marker or n's predecessor's value field is null,
     *       indicating (among other possibilities) that
     *       findPredecessor returned a deleted node. We can't unlink
     *       the node because we don't know its predecessor, so rely
     *       on another call to findPredecessor to notice and return
     *       some earlier predecessor, which it will do. This check is
     *       only strictly needed at beginning of loop, (and the
     *       b.value check isn't strictly needed at all) but is done
     *       each iteration to help avoid contention with other
     *       threads by callers that will fail to be able to change
     *       links, and so will retry anyway.
     *
     * C.
     * The traversal loops in doPut, doRemove, and findNear all
     * include the same three kinds of checks. And specialized
     * versions appear in findFirst, and findLast and their
     * variants. They can't easily share code because each uses the
     * reads of fields held in locals occurring in the orders they
     * were performed.
     *
     * @param key the key
     * @return node holding key, or null if no such
     */
    // 返回持有key的结点(如果没有则返回null), 同时清除遍历key沿途看到的任何已删除节点, 包括数据结点和索引结点
    private Node<K,V> findNode(Object key) {
        if (key == null)
            throw new NullPointerException(); // don't postpone errors
        Comparator<? super K> cmp = comparator;

        // 开始自旋, 要添加的结点z, 比较器comparator, 键key, 值value, key前驱b, b后继n, n后继f, n的值v, 比较结果c
        outer: for (;;) {
            // 查找比刚好key小一点的数据结点(key前驱), 如果找不到则会返回BASE_HEADER, 查找同时还会清除value为null的数据结点的索引结点
            for (Node<K,V> b = findPredecessor(key, cmp), n = b.next;;) {
                Object v; int c;

                // 如果key要所在位置为null, 说明没找到key对应的结点, 此时结束自旋, 返回null
                if (n == null)
                    break outer;

                // 如果n不为b后继了, 则说明b的后继被其他线程更改了, 此时重新自旋, 以获取新的b
                Node<K,V> f = n.next;
                if (n != b.next)                // inconsistent read
                    break;

                // 如果后继n值value为null, 说明n已经被删除了, 则通过标记后继结点辅助删除实例结点, 在实例结点value为null时调用, 删除后重新自旋, 以获取新的b
                if ((v = n.value) == null) {    // n is deleted
                    n.helpDelete(b, f);
                    break;
                }

                // 如果key前驱b值为null, 或者n为标记结点, 说明b是要被删除的, 此时重新自旋, 以获取新的b
                if (b.value == null || v == n)  // b is deleted
                    break;

                // 如果b和n都不是已删除的结点, 则比较key与n.key, 如果key == n.key, 说明n就是要找结点结点, 此时返回n结点
                if ((c = cpr(cmp, key, n.key)) == 0)
                    return n;

                // 如果c小于0, 即key < n.key, 说明b遍历到了链尾也没找到key对应的结点, 此时结束自旋, 返回null、
                if (c < 0)
                    break outer;

                // 如果c大于0, 即key > n.key, 说明key对应的结点可能还在b后面, 则继续遍历b链表
                b = n;
                n = f;
            }
        }
        return null;
    }

    /**
     * 20210627
     * 获取键的值。 与findNode几乎相同，但返回找到的值（以避免在重新读取期间重试）
     */
    /**
     * Gets value for key. Almost the same as findNode, but returns
     * the found value (to avoid retries during re-reads)
     *
     * @param key the key
     * @return the value, or null if absent
     */
    // get方法核心逻辑, 与findNode方法几乎相同, 但返回的是结点的值
    private V doGet(Object key) {
        if (key == null)
            throw new NullPointerException();
        Comparator<? super K> cmp = comparator;

        // 开始自旋, 比较器comparator, 键key, 值value, key前驱b, b后继n, n后继f, n的值v, 比较结果c
        outer: for (;;) {
            // 查找比刚好key小一点的数据结点(key前驱), 如果找不到则会返回BASE_HEADER, 查找同时还会清除value为null的数据结点的索引结点
            for (Node<K,V> b = findPredecessor(key, cmp), n = b.next;;) {
                Object v; int c;

                // 如果key要所在位置为null, 说明没找到key对应的结点, 此时结束自旋, 返回null
                if (n == null)
                    break outer;

                // 如果n不为b后继了, 则说明b的后继被其他线程更改了, 此时重新自旋, 以获取新的b
                Node<K,V> f = n.next;
                if (n != b.next)                // inconsistent read
                    break;

                // 如果后继n值value为null, 说明n已经被删除了, 则通过标记后继结点辅助删除实例结点, 在实例结点value为null时调用, 删除后重新自旋, 以获取新的b
                if ((v = n.value) == null) {    // n is deleted
                    n.helpDelete(b, f);
                    break;
                }

                // 如果key前驱b值为null, 或者n为标记结点, 说明b是要被删除的, 此时重新自旋, 以获取新的b
                if (b.value == null || v == n)  // b is deleted
                    break;

                // 如果b和n都不是已删除的结点, 则比较key与n.key, 如果key == n.key, 说明n就是要找结点结点, 此时返回n结点的值
                if ((c = cpr(cmp, key, n.key)) == 0) {
                    @SuppressWarnings("unchecked") V vv = (V)v;
                    return vv;
                }

                // 如果c小于0, 即key < n.key, 说明b遍历到了链尾也没找到key对应的结点, 此时结束自旋, 返回null
                if (c < 0)
                    break outer;

                // 如果c大于0, 即key > n.key, 说明key对应的结点可能还在b后面, 则继续遍历b链表
                b = n;
                n = f;
            }
        }
        return null;
    }

    /* ---------------- Insertion -------------- */

    /**
     * 20210626
     * 主要插入方法。 如果不存在则添加元素，如果存在则替换值且 onlyIfAbsent 为 false。
     */
    /**
     * Main insertion method.  Adds element if not present, or
     * replaces value if present and onlyIfAbsent is false.
     *
     * @param key the key
     * @param value the value that must be associated with key
     * @param onlyIfAbsent if should not insert if already present
     * @return the old value, or null if newly inserted
     */
    // put方法核心逻辑, onlyIfAbsent为true代表只允许不存在时插入(即存在时不允许插入或者替换), 此时返回n值; 否则替换n值, 返回value值
    private V doPut(K key, V value, boolean onlyIfAbsent) {
        Node<K,V> z;             // added node
        if (key == null)
            throw new NullPointerException();
        Comparator<? super K> cmp = comparator;

        // 开始自旋, 要添加的结点z, 比较器comparator, 键key, 值value, key前驱b, b后继n, n后继f, n的值v, 比较结果c
        outer: for (;;) {
            // 查找比刚好key小一点的数据结点(key前驱), 如果找不到则会返回BASE_HEADER, 查找同时还会清除value为null的数据结点的索引结点
            for (Node<K,V> b = findPredecessor(key, cmp), n = b.next;;) {
                // 如果key要插入位置存在后继
                if (n != null) {
                    Object v; int c;
                    Node<K,V> f = n.next;

                    // 如果n不为b后继了, 则说明b的后继被其他线程更改了, 此时重新自旋, 以获取新的b
                    if (n != b.next)               // inconsistent read 读取不一致
                        break;

                    // 如果后继n值value为null, 说明n已经被删除了, 则通过标记后继结点辅助删除实例结点, 在实例结点value为null时调用, 删除后重新自旋, 以获取新的b
                    if ((v = n.value) == null) {   // n is deleted
                        n.helpDelete(b, f);
                        break;
                    }

                    // 如果key前驱b值为null, 或者n为标记结点, 说明b是要被删除的, 此时重新自旋, 以获取新的b
                    if (b.value == null || v == n) // b is deleted
                        break;

                    // 如果b和n都不是已删除的结点, 则比较key与n.key, 如果key > n.key, 说明key的位置应该在n的后面, 也就是跳表追加了更接近key的结点, 则继续遍历
                    if ((c = cpr(cmp, key, n.key)) > 0) {
                        b = n;
                        n = f;
                        continue;
                    }

                    // 如果c为0, 说明key == n.key, 说明n就是key对应的结点
                    if (c == 0) {
                        // 如果只允许不存在时插入(即存在时不允许插入或者替换), 则返回n的值, 否则替换n值, 返回value值
                        if (onlyIfAbsent || n.casValue(v, value)) {
                            @SuppressWarnings("unchecked") V vv = (V)v;
                            return vv;
                        }

                        // 如果允许存在时插入或者替换, 但CAS更新n值失败了, 说明被其他线程替换掉了, 此时重新自旋, 以获取新的b
                        break; // restart if lost race to replace value 如果丢失了替换值的竞赛，则重新启动
                    }

                    // 如果c小于0, 即key < n.key, 且key != b.key, 说明b确实是key的前驱, 则继续往下走, 随机生成并维护索引结点
                    // else c < 0; fall through
                }

                // 到这里, key已经找到了插入的位置, 则构造node结点z, key -> n
                z = new Node<K,V>(key, value, n);

                // CAS链接b与key, b -> key, 如果CAS失败, 说明key插入失败, 则重新自旋, 以获取新的b
                if (!b.casNext(n, z))
                    break;         // restart if lost race to append to b 如果失去了追加到 b 的竞争，则重新启动
                // CAS链接b与key, b -> key, 如果CAS成功, 说明key插入成功, 则退出自旋
                break outer;
            }
        }

        // 到这里, key对应的数据结点z已创建并维护完毕, 此时需要随机生成level并维护索引结点
        int rnd = ThreadLocalRandom.nextSecondarySeed();

        // 0x8000_0001: 最高位为1, 最低位为1 => 排除了负数和奇数, 也就是rnd肯定为正偶数
        if ((rnd & 0x80000001) == 0) { // test highest and lowest bits 测试最高位和最低位
            int level = 1, max;

            // level为rnd连续1的个数, 因此增加层级的概率是1/2 * 1/2 = 1/4(本层级为1占1/2, +1层级为1占1/2)
            while (((rnd >>>= 1) & 1) != 0)
                ++level;

            // 索引结点idx, 顶层索引头结点h
            Index<K,V> idx = null;
            HeadIndex<K,V> h = head;

            // 如果随机生成的level小于等于h的层级, 说明不用更新顶层索引头结点h的指针, 则依次建立1~level级的索引结点, 但还没维护与前驱索引结点的链接
            if (level <= (max = h.level)) {
                for (int i = 1; i <= level; ++i)
                    idx = new Index<K,V>(z, idx, null);
            }

            // 如果随机生成的level大于h的层级, 说明需要更新顶层索引头结点head的指针, 则更新head指针(只维护head指针与z对应的idx结点关系, 因为这层只需要维护这个关系即可)
            else { // try to grow by one level 尝试成长一级
                level = max + 1; // hold in array and later pick the one to use 保持阵列，然后选择一个使用

                // 创建level+1索引结点数组, size-1可以表示索引结点的级高(0表示数据结点), 如果i小于size, 表示i层还没超出以前的层级, 则需要把i层idx结点右移一位
                @SuppressWarnings("unchecked")
                Index<K,V>[] idxs = (Index<K,V>[])new Index<?,?>[level+1];

                // 依次建立1~level级的索引结点, 并存进idxs数组(但还没维护与前驱索引结点的链接), idxs[1~level]分别表示i层z对应的索引结点
                for (int i = 1; i <= level; ++i)
                    idxs[i] = idx = new Index<K,V>(z, idx, null);

                // 开始自旋, 再次检查顶层索引头结点h是否被其他线程改变了
                for (;;) {
                    h = head;
                    int oldLevel = h.level;

                    // 如果level不在大于h.level, 说明h确实被其他线程改变了, 则结束自旋, 此后直接维护前驱索引结点即可, 无需更新head指针了
                    if (level <= oldLevel) // lost race to add level 输掉比赛以增加级别
                        break;

                    // 如果level确实还大于h.level, 说明h确实没被其他线程更改过, 则更新h指针为level层的HeadIndex, 这里只是维护好head指针(方便后面利用head进行每个HeadIndex与每层z对应的idx结点的指针维护)
                    HeadIndex<K,V> newh = h;
                    Node<K,V> oldbase = h.node;
                    for (int j = oldLevel+1; j <= level; ++j)
                        newh = new HeadIndex<K,V>(oldbase, newh, idxs[j], j);

                    // 如果CAS更新head指针成功, 则更新h指针指向最新的HeadIndex, level为以前h的level, idx为以前h的level层z对应的索引结点
                    if (casHead(h, newh)) {
                        h = newh;
                        idx = idxs[level = oldLevel];
                        break;
                    }
                    // 如果CAS更新h指针失败, 则h还是为以前那个HeadIndex, level为max+1, idx为max+1层z对应的索引结点, 此时重新自旋, 获取最新的head指针
                }
            }

            // find insertion points and splice in 找到插入点并拼接
            // 开始自旋, insertionLevel为以前h的level, j为新h的level, q为新的h, r为q的后继, t为以前h的level层z对应的索引结点
            splice: for (int insertionLevel = level;;) {
                int j = h.level;

                // 从h开始遍历, 维护每个t的索引结点前驱
                for (Index<K,V> q = h, r = q.right, t = idx;;) {
                    // 如果q为null, 或者t为null, 说明到了level 0索引结点(不存在), 代表维护结束, 结束自旋
                    if (q == null || t == null)
                        break splice;

                    // q不为null, 且t不为null, 说明索引结点q和t都是正常的, 如果r不为null, 说明还没找到原本最右的索引结点
                    if (r != null) {
                        Node<K,V> n = r.node;

                        // compare before deletion check avoids needing recheck 在删除检查之前进行比较避免需要重新检查
                        // 使用比较器或者自然排序比较key和后继的数据结点n的键, 比较结果c
                        int c = cpr(cmp, key, n.key);

                        // 如果n结点value为null, 说明n结点是删除的, 则CAS解除后继r的链接
                        if (n.value == null) {
                            // 如果CAS更新失败, 说明q或者r已被其他线程改变, 则进入下一轮自旋, 从头开始遍历
                            if (!q.unlink(r))
                                break;

                            // 如果CAS更新成功, 则获取最新r指针为q最新的后继, 接着遍历即可
                            r = q.right;
                            continue;
                        }

                        // n结点value不为null, 如果比较结果大于0, 说明r.node键k小了(用于定位r的位置, 保证r.key刚好小于key), 则继续往前遍历, 直到r.node键刚好等于key或者大于key
                        if (c > 0) {
                            q = r;
                            r = r.right;
                            continue;
                        }
                    }

                    // 经过上面的判断, 因为r.key刚好等于或者大于key, 此时q肯定为比key小的结点
                    // 如果j减小到了以前h的level, 说明j层适合维护t的前驱, 则CAS更新实例结点q最右结点r为t
                    if (j == insertionLevel) {
                        // 如果CAS成功, 则q链表上, 最右索引结点为t
                        if (!q.link(r, t))
                            // 如果CAS更新失败, 说明r已经被更改了, 则重新自旋, 重新从head指针位置开始找过来
                            break; // restart

                        // 如果t的数据结点已经删除, 则清除遍历key沿途看到的任何已删除节点, 包括数据结点和索引结点, 代表无需再维护索引结点了, 结束自旋返回null, 表明插入完成(实际上是被删除了懒得插了)
                        if (t.node.value == null) {
                            findNode(key);
                            break splice;
                        }
                        // t的数据结点没有被删除, 说明该层z对应的idx结点前驱已经维护完毕, 则insertionLevel-1, 继续维护下一层z对应的idx结点前驱, 此时如果insertionLevel-1为0, 说明所有层的idx结点前驱都维护完毕了, 所以结束自旋返回null即可, 代表插入完毕
                        if (--insertionLevel == 0)
                            break splice;
                    }

                    // 如果j大于以前h的level, 说明j层不是要维护的索引层, 则z对应的idx结点向下走一层
                    if (--j >= insertionLevel && j < level)
                        t = t.down;

                    // 到这里, 前一层z对应的idx结点前驱已经维护完毕了, 重新初始化HeadIndex q和Index r, q往下走一层, r为q的后继
                    q = q.down;
                    r = q.right;
                }
            }
        }

        // 插入成功, 返回null
        return null;
    }

    /* ---------------- Deletion -------------- */

    /**
     * 20210627
     * A. 主要删除方法。 定位节点，空值，附加删除标记，取消前任链接，删除关联的索引节点，并可能降低头索引级别。
     * B. 只需调用 findPredecessor 即可清除索引节点。 它将索引取消链接到沿键路径找到的已删除节点，这将包括该节点的索引。 这是无条件完成的。
     *    我们无法事先检查是否有索引节点，因为在初始搜索过程中可能尚未为此节点插入部分或全部索引，并且我们希望确保没有垃圾保留，因此必须调用确定。
     */
    /**
     * A.
     * Main deletion method. Locates node, nulls value, appends a
     * deletion marker, unlinks predecessor, removes associated index
     * nodes, and possibly reduces head index level.
     *
     * B.
     * Index nodes are cleared out simply by calling findPredecessor.
     * which unlinks indexes to deleted nodes found along path to key,
     * which will include the indexes to this node.  This is done
     * unconditionally. We can't check beforehand whether there are
     * index nodes because it might be the case that some or all
     * indexes hadn't been inserted yet for this node during initial
     * search for it, and we'd like to ensure lack of garbage
     * retention, so must call to be sure.
     *
     * @param key the key
     * @param value if non-null, the value that must be 如果非null，则必须与键关联的值
     * associated with key
     * @return the node, or null if not found 节点，如果未找到，则为 null
     */
    // remove方法核心逻辑, 如果value不为null, 则必须key和value都匹配才删除该结点, 否则key匹配即可删除
    final V doRemove(Object key, Object value) {
        if (key == null)
            throw new NullPointerException();
        Comparator<? super K> cmp = comparator;

        // 开始自旋, 比较器comparator, 键key, 值value, key前驱b, b后继n, n值value, key比较结果c, n后继f
        outer: for (;;) {
            // 查找比刚好key小一点的数据结点(key前驱), 如果找不到则会返回BASE_HEADER, 查找同时还会清除value为null的数据结点的索引结点
            for (Node<K,V> b = findPredecessor(key, cmp), n = b.next;;) {
                Object v; int c;
                if (n == null)
                    break outer;
                Node<K,V> f = n.next;

                // 如果n不为b后继了, 则说明b的后继被其他线程更改了, 此时重新自旋, 以获取新的b
                if (n != b.next)                    // inconsistent read
                    break;

                // 如果后继n值value为null, 说明n已经被删除了, 则通过标记后继结点辅助删除实例结点, 在实例结点value为null时调用, 删除后重新自旋, 以获取新的b
                if ((v = n.value) == null) {        // n is deleted
                    n.helpDelete(b, f);
                    break;
                }

                // 如果key前驱b值为null, 或者n为标记结点, 说明b是要被删除的, 此时重新自旋, 以获取新的b
                if (b.value == null || v == n)      // b is deleted
                    break;

                // 如果b和n都不是已删除的结点, 则比较key与n.key, 如果key < n.key, 说明key不存在(有可能真的不存在、也有可能被之前存在但被其他线程删除了), 则直接结束自旋返回null即可
                if ((c = cpr(cmp, key, n.key)) < 0)
                    break outer;

                // 如果key > n.key, 说明key的位置应该在n的后面, 则继续遍历
                if (c > 0) {
                    b = n;
                    n = f;
                    continue;
                }

                // 如果key == n.key, 说明n就是key要找的结点, 如果指定了value, 但value不匹配, 则直接结束自旋返回null即可
                if (value != null && !value.equals(v))
                    break outer;

                // 如果key == n.key, 且不指定value或者value匹配了, 则CAS更新n.value为null, 代表删除结点n
                if (!n.casValue(v, null))
                    // 如果CAS失败, 说明v.value已经被其他线程更新掉了, 即删除失败, 则重新自旋
                    break;

                // 如果CAS删除n结点成功, 则再CAS更新f(即n.next)的next指针指向f自身, 代表使f为标记结点, 如果失败了, 则再CAS更新b.next为f结点
                if (!n.appendMarker(f) || !b.casNext(n, f))
                    // 如果CAS更新f后继失败, 且再CAS更新b后继失败, 则清除遍历key沿途看到的任何已删除节点, 包括数据结点和索引结点
                    findNode(key);                  // retry via findNode 通过 findNode 重试

                //  如果CAS更新f后继成功, 或者f后继更新失败但b后继更新成功
                else {
                    // 清除value为null的数据结点的索引结点
                    findPredecessor(key, cmp);      // clean index

                    // 如果head没有索引结点了, 则尝试减少索引层级
                    if (head.right == null)
                        // 如果head、head.down、head.down.down3层都没有索引结点了(即只剩下HeadIndex时), 则删除head层, 使head指向下一层; 但如果删除后复检时发现原head又多了索引结点, 则又会恢复原状, 取消删除
                        tryReduceLevel();
                }

                // 删除结点并清除索引结点后, 返回n.value的深复制vv
                @SuppressWarnings("unchecked") V vv = (V)v;
                return vv;
            }
        }
        return null;
    }

    /**
     * 20210627
     * A. 如果没有节点，可能会降低头部水平。 这种方法可能（很少）出错，在这种情况下，即使级别将包含索引节点，它们也会消失。 这会影响性能，而不是正确性。
     *    为了尽量减少错误并减少滞后，只有当最上面的三个级别看起来是空的时候，级别才减一。 此外，如果移除的级别在 CAS 之后看起来非null，
     *    我们会尝试在任何人注意到我们的错误之前快速将其更改回来！ （这个技巧很有效，因为这个方法实际上永远不会出错，除非当前线程在第一次 CAS 之前立即停顿，
     *    在这种情况下，它不太可能在之后立即再次停顿，所以会恢复。）
     * B. 我们忍受了这一切，而不仅仅是让关卡增长，否则，即使是经过大量插入和删除的小Map也会有很多关卡，这会比偶尔出现的不必要的减少更能减慢访问速度。
     */
    /**
     * A.
     * Possibly reduce head level if it has no nodes.  This method can
     * (rarely) make mistakes, in which case levels can disappear even
     * though they are about to contain index nodes. This impacts
     * performance, not correctness.  To minimize mistakes as well as
     * to reduce hysteresis, the level is reduced by one only if the
     * topmost three levels look empty. Also, if the removed level
     * looks non-empty after CAS, we try to change it back quick
     * before anyone notices our mistake! (This trick works pretty
     * well because this method will practically never make mistakes
     * unless current thread stalls immediately before first CAS, in
     * which case it is very unlikely to stall again immediately
     * afterwards, so will recover.)
     *
     * B.
     * We put up with all this rather than just let levels grow
     * because otherwise, even a small map that has undergone a large
     * number of insertions and removals will have a lot of levels,
     * slowing down access more than would an occasional unwanted
     * reduction.
     */
    // 尝试减少索引层级, 如果head、head.down、head.down.down3层都没有索引结点了(即只剩下HeadIndex时), 则删除head层, 使head指向下一层; 但如果删除后复检时发现原head又多了索引结点, 则又会恢复原状, 取消删除
    private void tryReduceLevel() {
        // head指针h, h下结点d, d下结点e
        HeadIndex<K,V> h = head;
        HeadIndex<K,V> d;
        HeadIndex<K,V> e;

        // 如果h的层级大于3, 且d、e不为null, 且h、d、e层只有HeadIndex结点(即没有索引结点时), 则CAS更新h到h.down
        if (h.level > 3 &&
            (d = (HeadIndex<K,V>)h.down) != null &&
            (e = (HeadIndex<K,V>)d.down) != null &&
            e.right == null &&
            d.right == null &&
            h.right == null &&
            casHead(h, d) && // try to set
            h.right != null) // recheck

            // CAS更新h为d成功后, 再次检查h层是否还有索引结点, 如果确实还有, 那再把h改为原来的h(即取消删除), 否则直接返回
            casHead(d, h);   // try to backout
    }

    /* ---------------- Finding and removing first element -------------- */

    /**
     * Specialized variant of findNode to get first valid node.
     * @return first node or null if empty
     */
    // findNode的特殊变体, 用于获取第一个有效节点, 如果返回之前刚好n被置null了, 则通过标记后继结点辅助删除实例结点
    final Node<K,V> findFirst() {
        // 从head开始遍历node, 直到第一个不为null的后继n, 并返回n.value
        for (Node<K,V> b, n;;) {
            if ((n = (b = head.node).next) == null)
                return null;
            if (n.value != null)
                return n;
            // 如果返回之前刚好n被置null了, 则通过标记后继结点辅助删除实例结点, 在实例结点value为null时调用
            n.helpDelete(b, n.next);
        }
    }

    /**
     * Removes first entry; returns its snapshot.
     *
     * @return null if empty, else snapshot of first entry
     */
    // 删除第一个条目的数据结点以及与之对应的每层的索引结点, 并返回其键和值的快照
    private Map.Entry<K,V> doRemoveFirstEntry() {
        for (Node<K,V> b, n;;) {
            // 从head开始遍历node, 找到第一个不为null的后继n, 如果找不到则返回null
            if ((n = (b = head.node).next) == null)
                return null;

            // 如果n不为b后继了, 则说明b的后继被其他线程更改了, 此时重新自旋, 以获取新的b
            Node<K,V> f = n.next;
            if (n != b.next)
                continue;

            // 如果后继n值value为null, 说明n已经被删除了, 则通过标记后继结点辅助删除实例结点, 在实例结点value为null时调用, 删除后重新自旋, 以获取新的b
            Object v = n.value;
            if (v == null) {
                n.helpDelete(b, f);
                continue;
            }

            // 如果后继n没有发生改变, 则CAS更新n.value为null, 即删除n结点
            if (!n.casValue(v, null))
                // 如果CAS失败, 说明v.value已经被其他线程更新掉了, 即删除失败, 则重新自旋
                continue;

            // 如果CAS删除n结点成功, 则再CAS更新f(即n.next)的next指针指向f自身, 代表使f为标记结点, 如果失败了, 则再CAS更新b.next为f结点
            if (!n.appendMarker(f) || !b.casNext(n, f))
                // 通过标记后继结点辅助删除实例结点, 在实例结点value为null时调用
                findFirst(); // retry

            // 清除数据结点后, 继续清除与删除每层第一个条目关联的索引节点
            clearIndexToFirst();

            // 数据结点和索引结点都删除成功后, 则返回n键、n值的快照
            @SuppressWarnings("unchecked") V vv = (V)v;
            return new AbstractMap.SimpleImmutableEntry<K,V>(n.key, vv);
        }
    }

    /**
     * Clears out index nodes associated with deleted first entry.
     */
    // 清除与删除每层第一个条目关联的索引节点
    private void clearIndexToFirst() {
        // 开始自旋
        for (;;) {
            // 从head开始遍历索引层, head指针q, q的后继r
            for (Index<K,V> q = head;;) {
                Index<K,V> r = q.right;
                // 如果r不为null, 且r对应的数据结点为null, 说明找到了q层第一个需要删除的的索引结点, 则CAS解除后继r的链接
                if (r != null && r.indexesDeletedNode() && !q.unlink(r))
                    // 如果r无效, 或者r数据结点有效, 或者CAS解除r链接失败, 则重新自旋
                    break;

                // CAS解除r链接成功, 如果q下结点为null, 说明清除到了最后一层, 即每层的第一个需要删除的索引已经清理完毕了
                if ((q = q.down) == null) {
                    // 再判断是否需要减少索引层级, 如果head没有索引结点了, 则尝试减少索引层级
                    if (head.right == null)
                        // 如果head、head.down、head.down.down3层都没有索引结点了(即只剩下HeadIndex时), 则删除head层, 使head指向下一层; 但如果删除后复检时发现原head又多了索引结点, 则又会恢复原状, 取消删除
                        tryReduceLevel();

                    // 如果不需要减少索引层级, 或者减少成功后, 则返回即可
                    return;
                }
            }
        }
    }

    /**
     * Removes last entry; returns its snapshot.
     * Specialized variant of doRemove.
     *
     * @return null if empty, else snapshot of last entry
     */
    // 删除最后一个条目的数据结点以及与之对应的每层的索引结点, 并返回其键和值的快照
    private Map.Entry<K,V> doRemoveLastEntry() {
        // 开始自旋
        for (;;) {
            // 获取最后一层最后一个索引的数据结点
            Node<K,V> b = findPredecessorOfLast();

            // b后继n, 如果n为null了, 说明数据结点遍历到尾了, 则继续自旋; 如果b为BASE_HEADER, 则返回null, 表示没有任何数据结点, 删除失败
            Node<K,V> n = b.next;
            if (n == null) {
                if (b.isBaseHeader())               // empty
                    return null;
                else
                    continue; // all b's successors are deleted; retry // b的所有后继都被删除； 重试
            }

            // 如果b还有后继, 则继续遍历
            for (;;) {
                // 如果n不为b后继了, 则说明b的后继被其他线程更改了, 此时重新自旋, 以获取新的b
                Node<K,V> f = n.next;
                if (n != b.next)                    // inconsistent read
                    break;

                // 如果后继n值value为null, 说明n已经被删除了, 则通过标记后继结点辅助删除实例结点, 在实例结点value为null时调用, 删除后结束遍历, 重新获取b, 重新自旋
                Object v = n.value;
                if (v == null) {                    // n is deleted
                    n.helpDelete(b, f);
                    break;
                }

                // 如果key前驱b值为null, 或者n为标记结点, 说明b是要被删除的, 此时结束遍历, 重新获取b, 重新自旋
                if (b.value == null || v == n)      // b is deleted
                    break;

                // 如果b和n都是正常的结点, 且f不为null, 说明n还有后继, 则继续遍历数据结点链表
                if (f != null) {
                    b = n;
                    n = f;
                    continue;
                }

                // 如果b和n都是正常的结点, 但f为null, 说明n为最后一个结点了, 则CAS更新v值为null
                if (!n.casValue(v, null))
                    // 如果CAS失败, 则结束遍历, 重新获取b, 重新自旋
                    break;

                // 如果CAS删除n结点成功, 则再CAS更新f(即n.next)的next指针指向f自身, 代表使f为标记结点, 如果失败了, 则再CAS更新b.next为f结点
                K key = n.key;
                if (!n.appendMarker(f) || !b.casNext(n, f))
                    // 如果CAS更新f后继失败, 且再CAS更新b后继失败, 则清除遍历key沿途看到的任何已删除节点, 包括数据结点和索引结点
                    findNode(key);                  // retry via findNode

                //  如果CAS更新f后继成功, 或者f后继更新失败但b后继更新成功
                else {                              // clean index
                    // 清除value为null的数据结点的索引结点
                    findPredecessor(key, comparator);

                    // 如果head没有索引结点了, 则尝试减少索引层级
                    if (head.right == null)
                        // 如果head、head.down、head.down.down3层都没有索引结点了(即只剩下HeadIndex时), 则删除head层, 使head指向下一层; 但如果删除后复检时发现原head又多了索引结点, 则又会恢复原状, 取消删除
                        tryReduceLevel();
                }

                // 数据结点和索引结点都删除成功后, 则返回n键、n值的快照
                @SuppressWarnings("unchecked") V vv = (V)v;
                return new AbstractMap.SimpleImmutableEntry<K,V>(key, vv);
            }
        }
    }

    /* ---------------- Finding and removing last element -------------- */

    /**
     * Specialized version of find to get last valid node.
     *
     * @return last node or null if empty
     */
    // 获取最后一个有效节点: 先走到最后一个索引结点, 再从该索引的数据结点遍历到最后一个数据结点，对比findPredecessor，该方法不需要比较key大小
    final Node<K,V> findLast() {

        /**
         * 20210627
         * findPredecessor不能用于遍历索引级别，因为它不使用比较。所以两个层次的遍历被折叠在一起。
         */
        /*
         * findPredecessor can't be used to traverse index level
         * because this doesn't use comparisons.  So traversals of
         * both levels are folded together.
         */
        Index<K,V> q = head;

        // 开始自旋, head指针q, q下结点d, q后继r
        for (;;) {
            Index<K,V> d, r;

            // 如果q存在后继r, 说明还没到最右一个索引结点, 则继续遍历q层索引
            if ((r = q.right) != null) {
                // 如果r已被删除, 则返回CAS解除后继succ的链接, 更新q为head指针, 继续自旋
                if (r.indexesDeletedNode()) {
                    q.unlink(r);
                    q = head; // restart
                }
                // 如果r没被删除, 则继续遍历q层索引结点
                else
                    q = r;
            }
            // 如果q不存在后继r, 说明q层索引遍历到尾了, 则q往下走一层, 继续自旋
            else if ((d = q.down) != null) {
                q = d;
            }
            // 如果q层索引到尾了, 且q为最后一层索引, 说明找到了最后一个索引结点
            else {
                // 从最后一个索引的数据结点开始遍历, q的数据结点b, b后继n, n后继f
                for (Node<K,V> b = q.node, n = b.next;;) {
                    // 如果n为null了, 说明数据结点遍历到尾了, 则返回b; 如果b为BASE_HEADER, 则返回null, 表示没有任何数据结点
                    if (n == null)
                        return b.isBaseHeader() ? null : b;

                    // 如果n不为b后继了, 则说明b的后继被其他线程更改了, 此时重新自旋, 以获取新的b
                    Node<K,V> f = n.next;            // inconsistent read
                    if (n != b.next)
                        break;

                    // 如果后继n值value为null, 说明n已经被删除了, 则通过标记后继结点辅助删除实例结点, 在实例结点value为null时调用, 删除后结束遍历, 重新获取b, 重新自旋
                    Object v = n.value;
                    if (v == null) {                 // n is deleted
                        n.helpDelete(b, f);
                        break;
                    }

                    // 如果key前驱b值为null, 或者n为标记结点, 说明b是要被删除的, 此时结束遍历, 重新获取b, 重新自旋
                    if (b.value == null || v == n)      // b is deleted
                        break;

                    // 如果b和n都是正常的结点, 继续遍历数据结点链表
                    b = n;
                    n = f;
                }

                // 重新获取b, 重新自旋
                q = head; // restart
            }
        }
    }

    /**
     * 20210627
     * findPredecessor的特殊变体，用于获取最后一个有效节点的前驱。删除最后一个条目时需要。返回节点的所有后继节点可能在返回时都已被删除，在这种情况下，可以重试此方法。
     */
    /**
     * Specialized variant of findPredecessor to get predecessor of last
     * valid node.  Needed when removing the last entry.  It is possible
     * that all successors of returned node will have been deleted upon
     * return, in which case this method can be retried.
     *
     * @return likely predecessor of last node
     */
    // 获取最后一层最后一个索引的数据结点
    private Node<K,V> findPredecessorOfLast() {
        // 开始自旋, head指针q, q下结点d, q后继r
        for (;;) {
            // 从head指针开始遍历
            for (Index<K,V> q = head;;) {
                Index<K,V> d, r;

                // 如果q存在后继r, 说明还没到最右一个索引结点, 则继续遍历q层索引
                if ((r = q.right) != null) {
                    // 如果r已被删除, 则返回CAS解除后继succ的链接, 更新q为head指针, 退出遍历, 重新获取q, 重新自旋
                    if (r.indexesDeletedNode()) {
                        q.unlink(r);
                        break;    // must restart
                    }

                    // proceed as far across as possible without overshooting 在不超调的情况下尽可能远地进行
                    // 如果r没被删除, 且r的数据结点不为最后一个时, 则继续遍历q层索引结点
                    if (r.node.next != null) {
                        q = r;
                        continue;
                    }
                }

                // 如果q不存在后继r, 说明q层索引遍历到尾了, 则q往下走一层, 继续自旋
                if ((d = q.down) != null)
                    q = d;

                // 如果q层索引到尾了, 且q为最后一层索引, 说明找到了最后一个索引结点, 则返回该索引的数据结点
                else
                    return q.node;
            }
        }
    }

    /* ---------------- Relational operations -------------- */
    // 关系操作

    // Control values OR'ed as arguments to findNear
    // 控制值或作为 findNear 的参数
    private static final int EQ = 1;
    private static final int LT = 2;
    private static final int GT = 0; // Actually checked as !LT 实际上检查为 !LT

    /**
     * Utility for ceiling, floor, lower, higher methods.
     *
     * @param key the key
     * @param rel the relation -- OR'ed combination of EQ, LT, GT 关系 -- EQ、LT、GT 的 OR 组合
     * @return nearest node fitting relation, or null if no such 最近节点拟合关系，如果没有，则为 null
     */
    // 返回指定key和拟合关系的最近节点(通过LT|EQ或者GT|EQ 来控制 小于等于或者大于等于), 如果没有则为 null, 用于lower、floor、ceiling、higher方法
    final Node<K,V> findNear(K key, int rel, Comparator<? super K> cmp) {
        if (key == null)
            throw new NullPointerException();

        // 开始自旋, 比较器comparator, 键key, key前驱b, b后继n, n后继f, n的值v, 比较结果c
        for (;;) {
            for (Node<K,V> b = findPredecessor(key, cmp), n = b.next;;) {
                Object v;

                // 如果key要所在位置为null, 说明没找到key对应的结点, 此时如果rel不为LT || rel为LT/LT|EQ, 但b为BASE_HEADER, 则返回null, 代表找不到后继或者前驱结点; 否则如果rel为LT/LT|EQ且b不为BASE_HEADER, 则返回key前驱b结点
                if (n == null)
                    return ((rel & LT) == 0 || b.isBaseHeader()) ? null : b;

                // 如果n不为b后继了, 则说明b的后继被其他线程更改了, 此时重新自旋, 以获取新的b
                Node<K,V> f = n.next;
                if (n != b.next)                  // inconsistent read
                    break;

                // 如果后继n值value为null, 说明n已经被删除了, 则通过标记后继结点辅助删除实例结点, 在实例结点value为null时调用, 删除后重新自旋, 以获取新的b
                if ((v = n.value) == null) {      // n is deleted
                    n.helpDelete(b, f);
                    break;
                }

                // 如果key前驱b值为null, 或者n为标记结点, 说明b是要被删除的, 此时重新自旋, 以获取新的b
                if (b.value == null || v == n)      // b is deleted
                    break;

                // 如果b和n都不是已删除的结点, 则比较key与n.key
                int c = cpr(cmp, key, n.key);

                // 1. 如果key == n.key, 说明n就是要找结点结点, 如果此时rel为LT|EQ(小于等于)或者GT|EQ(大于等于)时, 则返回n
                // 2. 如果key < n.key, 说明b遍历到了链尾也没找到key对应的结点, 如果此时rel为GT(大于)或者GT|EQ(大于等于)时, 则返回n
                if ((c == 0 && (rel & EQ) != 0) || (c < 0 && (rel & LT) == 0))
                    return n;

                // 3. 如果key < n.key, 如果此时rel为LT(小于)或者LT|EQ(小于等于)时
                if ( c <= 0 && (rel & LT) != 0)
                    // 如果为BASE_HEADER, 则返回null, 代表找不到后继或者前驱结点; 否则返回key前驱b结点
                    return b.isBaseHeader() ? null : b;

                // 如果c大于0, 即key > n.key, 说明key对应的结点可能还在b后面, 则继续遍历b链表
                b = n;
                n = f;
            }
        }
    }

    /**
     * 20210627
     * 为 findNear 的结果返回 SimpleImmutableEntry。
     */
    /**
     * Returns SimpleImmutableEntry for results of findNear.
     * @param key the key
     * @param rel the relation -- OR'ed combination of EQ, LT, GT
     * @return Entry fitting relation, or null if no such
     */
    // 为findNear的结果封装成SimpleImmutableEntry对象, 生成时的映射快照, 不支持setValue方法
    final AbstractMap.SimpleImmutableEntry<K,V> getNear(K key, int rel) {
        Comparator<? super K> cmp = comparator;
        for (;;) {
            Node<K,V> n = findNear(key, rel, cmp);
            if (n == null)
                return null;
            AbstractMap.SimpleImmutableEntry<K,V> e = n.createSnapshot();
            if (e != null)
                return e;
        }
    }

    /* ---------------- Constructors -------------- */

    /**
     * 20210624
     * 构造一个新的空映射，根据键的 {@linkplain Comparable natural ordering} 进行排序。
     */
    /**
     * Constructs a new, empty map, sorted according to the
     * {@linkplain Comparable natural ordering} of the keys.
     */
    public ConcurrentSkipListMap() {
        this.comparator = null;
        initialize();
    }

    /**
     * 20210624
     * 构造一个新的空映射，根据指定的比较器进行排序。
     */
    /**
     * Constructs a new, empty map, sorted according to the specified
     * comparator.
     *
     * @param comparator the comparator that will be used to order this map.
     *        If {@code null}, the {@linkplain Comparable natural
     *        ordering} of the keys will be used.
     */
    public ConcurrentSkipListMap(Comparator<? super K> comparator) {
        this.comparator = comparator;
        initialize();
    }

    /**
     * 20210624
     * 构造一个包含与给定映射相同的映射的新映射，根据键的 {@linkplain Comparable 自然排序} 排序。
     */
    /**
     * Constructs a new map containing the same mappings as the given map,
     * sorted according to the {@linkplain Comparable natural ordering} of
     * the keys.
     *
     * @param  m the map whose mappings are to be placed in this map
     * @throws ClassCastException if the keys in {@code m} are not
     *         {@link Comparable}, or are not mutually comparable
     * @throws NullPointerException if the specified map or any of its keys
     *         or values are null
     */
    public ConcurrentSkipListMap(Map<? extends K, ? extends V> m) {
        this.comparator = null;
        initialize();
        putAll(m);
    }

    /**
     * 20210624
     * 构造一个包含相同映射并使用与指定排序映射相同的排序的新映射。
     */
    /**
     * Constructs a new map containing the same mappings and using the
     * same ordering as the specified sorted map.
     *
     * @param m the sorted map whose mappings are to be placed in this
     *        map, and whose comparator is to be used to sort this map
     * @throws NullPointerException if the specified sorted map or any of
     *         its keys or values are null
     */
    public ConcurrentSkipListMap(SortedMap<K, ? extends V> m) {
        this.comparator = m.comparator();
        initialize();
        buildFromSorted(m);
    }

    /**
     * Returns a shallow copy of this {@code ConcurrentSkipListMap}
     * instance. (The keys and values themselves are not cloned.)
     *
     * @return a shallow copy of this map
     */
    public ConcurrentSkipListMap<K,V> clone() {
        try {
            @SuppressWarnings("unchecked")
            ConcurrentSkipListMap<K,V> clone =
                (ConcurrentSkipListMap<K,V>) super.clone();
            clone.initialize();
            clone.buildFromSorted(this);
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new InternalError();
        }
    }

    /**
     * 20210624
     * 简化批量插入以从给定排序映射的元素进行初始化。仅从构造函数或克隆方法调用。
     */
    /**
     * Streamlined bulk insertion to initialize from elements of
     * given sorted map.  Call only from constructor or clone
     * method.
     */
    // 根据顺序Map重新建立跳跃表, head指针设置为最高层的索引头结点 => level 1: 50%, level 2: 25%, level 3: 12.5%, 索引层级越高, 出现的概率越低
    private void buildFromSorted(SortedMap<K, ? extends V> map) {
        if (map == null)
            throw new NullPointerException();

        // 构造函数&clone()调用时, h为 HeadIndex: { node: { Node: {key: null, value: BASE_HEADER, next: null} }, down: null, right: null, level: 1 }
        HeadIndex<K,V> h = head;

        // basepred表示最右边的数据结点, basepred为 Node: {key: null, value: BASE_HEADER, next: null}
        Node<K,V> basepred = h.node;

        // Track the current rightmost node at each level. Uses an
        // ArrayList to avoid committing to initial or maximum level.
        // 在每个级别跟踪当前最右边的节点。 使用 ArrayList 避免提交到初始或最大级别。
        // preds第i桶存放第i级的最右边的索引结点(i为0时为null)
        ArrayList<Index<K,V>> preds = new ArrayList<Index<K,V>>();

        // initialize
        // 为每层的最右索引结点占空位
        // eg: h.level=1时, preds[0: null, 1: null]
        for (int i = 0; i <= h.level; ++i)
            preds.add(null);

        // 设置preds数组元素, 设置每层最右的索引结点(i、level一一对应), 此时只有1个结点, 所以为HeadIndex结点
        // eg: h.level=1时, preds[0: null, 1: h]
        Index<K,V> q = h;
        for (int i = h.level; i > 0; --i) {
            preds.set(i, q);
            q = q.down;
        }

        // 使用Node的有序迭代器遍历, 迭代器it, 当前迭代结点e, 伪随机数rnd, 确定到的层级j
        Iterator<? extends Map.Entry<? extends K, ? extends V>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<? extends K, ? extends V> e = it.next();
            int rnd = ThreadLocalRandom.current().nextInt();
            int j = 0;

            // 0x8000_0001: 最高位为1, 最低位为1 => 排除了负数和奇数, 也就是rnd肯定为正偶数
            if ((rnd & 0x80000001) == 0) {
                do {
                    // j为rnd连续1的个数, 因此增加层级的概率是1/2 * 1/2 = 1/4(本层级为1占1/2, +1层级为1占1/2)
                    ++j;
                } while (((rnd >>>= 1) & 1) != 0);

                // 当j大于当前层级, 则层级+1赋值给j => level 1: 50%, level 2: 25%, level 3: 12.5% => 索引层级越高, 出现的概率越低
                if (j > h.level) j = h.level + 1;
            }

            // e键k, e值v, 构造数据结点z, 由于map是有序的, 则按顺序建立数据结点
            // z为Node: {key: e.k, value: e.v, null},
            K k = e.getKey();
            V v = e.getValue();
            if (k == null || v == null)
                throw new NullPointerException();
            Node<K,V> z = new Node<K,V>(k, v, null);

            // 链接最右数据结点basepred与z, 此时basepred为 Node: {key: null, value: BASE_HEADER, next: { Node: {key: e.k, value: e.v, null} }}
            basepred.next = z;

            // z成为新的最右结点, 此时basepred为 Node: {key: e.k, value: e.v, null}
            basepred = z;

            // 如果确定到的层级j大于0, 说明j有效, 则从低到高建立索引结点
            if (j > 0) {
                Index<K,V> idx = null;

                // i、j表示从下到上的层级(j为0时表示数据结点), i从1开始表示从第1级的索引结点开始处理
                for (int i = 1; i <= j; ++i) {
                    // 基于数据结点z, i=1时, idx表示新建的索引结点, i=2时, idx表示i=1的索引结点
                    // eg: i=1时, idx1为 Index: node: { Node: {key: e.k, value: e.v, null} }, down: null, right: null }
                    // eg: i=2时, idx2为 Index: node: { Node: {key: e.k, value: e.v, null} }, down: idx1, right: null }
                    idx = new Index<K,V>(z, idx, null);

                    // 如果层级比h的层级还高, 说明需要建立更高层的HeadIndex结点
                    if (i > h.level)
                        // eg: h=1, i=2时, h为 HeadIndex: { node: { Node: {key: null, value: BASE_HEADER, next: null} }, down: {h}, right: idx2, level: 2 }
                        h = new HeadIndex<K,V>(h.node, h, idx, i);
                    // 如果i<=h, 则保持h不变
                    // eg: h=1, i=1时, h为 HeadIndex: { node: { Node: {key: null, value: BASE_HEADER, next: null} }, down: null, right: null, level: 1 }

                    // preds#size-1可以表示索引结点的级高(0表示数据结点), 如果i小于size, 表示i层还没超出以前的层级, 则需要把i层idx结点右移一位
                    // eg: h.level=1时, preds[0: null, 1: h], 此时i确实小于size
                    if (i < preds.size()) {
                        // eg: i=1时, preds[1].right => h.right = idx1, 相当于i层结点right指针追加idx
                        preds.get(i).right = idx;

                        // 再设置preds[1] = idx1, 相当于i层的结点右移一个结点
                        preds.set(i, idx);
                    }
                    // 如果i等于size(永远不会大于), 表示i层超出了1层最高层, 则往preds继续追加一个索引结点, 待后面遍历时补上
                    // eg: i=2时, 则preds[0: null, 1: h, 2: idx2]
                    else
                        preds.add(idx);
                }
            }
        }

        // head指针设置为最高层的索引头结点
        head = h;
    }

    /* ---------------- Serialization -------------- */

    /**
     * Saves this map to a stream (that is, serializes it).
     *
     * @param s the stream
     * @throws java.io.IOException if an I/O error occurs
     * @serialData The key (Object) and value (Object) for each
     * key-value mapping represented by the map, followed by
     * {@code null}. The key-value mappings are emitted in key-order
     * (as determined by the Comparator, or by the keys' natural
     * ordering if no Comparator).
     */
    private void writeObject(java.io.ObjectOutputStream s)
        throws java.io.IOException {
        // Write out the Comparator and any hidden stuff
        s.defaultWriteObject();

        // Write out keys and values (alternating)
        for (Node<K,V> n = findFirst(); n != null; n = n.next) {
            V v = n.getValidValue();
            if (v != null) {
                s.writeObject(n.key);
                s.writeObject(v);
            }
        }
        s.writeObject(null);
    }

    /**
     * Reconstitutes this map from a stream (that is, deserializes it).
     * @param s the stream
     * @throws ClassNotFoundException if the class of a serialized object
     *         could not be found
     * @throws java.io.IOException if an I/O error occurs
     */
    @SuppressWarnings("unchecked")
    private void readObject(final java.io.ObjectInputStream s)
        throws java.io.IOException, ClassNotFoundException {
        // Read in the Comparator and any hidden stuff
        s.defaultReadObject();
        // Reset transients
        initialize();

        /*
         * This is nearly identical to buildFromSorted, but is
         * distinct because readObject calls can't be nicely adapted
         * as the kind of iterator needed by buildFromSorted. (They
         * can be, but doing so requires type cheats and/or creation
         * of adaptor classes.) It is simpler to just adapt the code.
         */

        HeadIndex<K,V> h = head;
        Node<K,V> basepred = h.node;
        ArrayList<Index<K,V>> preds = new ArrayList<Index<K,V>>();
        for (int i = 0; i <= h.level; ++i)
            preds.add(null);
        Index<K,V> q = h;
        for (int i = h.level; i > 0; --i) {
            preds.set(i, q);
            q = q.down;
        }

        for (;;) {
            Object k = s.readObject();
            if (k == null)
                break;
            Object v = s.readObject();
            if (v == null)
                throw new NullPointerException();
            K key = (K) k;
            V val = (V) v;
            int rnd = ThreadLocalRandom.current().nextInt();
            int j = 0;
            if ((rnd & 0x80000001) == 0) {
                do {
                    ++j;
                } while (((rnd >>>= 1) & 1) != 0);
                if (j > h.level) j = h.level + 1;
            }
            Node<K,V> z = new Node<K,V>(key, val, null);
            basepred.next = z;
            basepred = z;
            if (j > 0) {
                Index<K,V> idx = null;
                for (int i = 1; i <= j; ++i) {
                    idx = new Index<K,V>(z, idx, null);
                    if (i > h.level)
                        h = new HeadIndex<K,V>(h.node, h, idx, i);

                    if (i < preds.size()) {
                        preds.get(i).right = idx;
                        preds.set(i, idx);
                    } else
                        preds.add(idx);
                }
            }
        }
        head = h;
    }

    /* ------ Map API methods ------ */

    /**
     * Returns {@code true} if this map contains a mapping for the specified
     * key.
     *
     * @param key key whose presence in this map is to be tested
     * @return {@code true} if this map contains a mapping for the specified key
     * @throws ClassCastException if the specified key cannot be compared
     *         with the keys currently in the map
     * @throws NullPointerException if the specified key is null
     */
    public boolean containsKey(Object key) {
        return doGet(key) != null;
    }

    /**
     * Returns the value to which the specified key is mapped,
     * or {@code null} if this map contains no mapping for the key.
     *
     * <p>More formally, if this map contains a mapping from a key
     * {@code k} to a value {@code v} such that {@code key} compares
     * equal to {@code k} according to the map's ordering, then this
     * method returns {@code v}; otherwise it returns {@code null}.
     * (There can be at most one such mapping.)
     *
     * @throws ClassCastException if the specified key cannot be compared
     *         with the keys currently in the map
     * @throws NullPointerException if the specified key is null
     */
    public V get(Object key) {
        return doGet(key);
    }

    /**
     * Returns the value to which the specified key is mapped,
     * or the given defaultValue if this map contains no mapping for the key.
     *
     * @param key the key
     * @param defaultValue the value to return if this map contains
     * no mapping for the given key
     * @return the mapping for the key, if present; else the defaultValue
     * @throws NullPointerException if the specified key is null
     * @since 1.8
     */
    public V getOrDefault(Object key, V defaultValue) {
        V v;
        return (v = doGet(key)) == null ? defaultValue : v;
    }

    /**
     * 20210626
     * 将指定值与此映射中的指定键相关联。 如果映射先前包含键的映射，则旧值将被替换。
     */
    /**
     * Associates the specified value with the specified key in this map.
     * If the map previously contained a mapping for the key, the old
     * value is replaced.
     *
     * @param key key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with the specified key, or
     *         {@code null} if there was no mapping for the key
     * @throws ClassCastException if the specified key cannot be compared
     *         with the keys currently in the map
     * @throws NullPointerException if the specified key or value is null
     */
    public V put(K key, V value) {
        if (value == null)
            throw new NullPointerException();
        return doPut(key, value, false);
    }

    /**
     * Removes the mapping for the specified key from this map if present.
     *
     * @param  key key for which mapping should be removed
     * @return the previous value associated with the specified key, or
     *         {@code null} if there was no mapping for the key
     * @throws ClassCastException if the specified key cannot be compared
     *         with the keys currently in the map
     * @throws NullPointerException if the specified key is null
     */
    public V remove(Object key) {
        return doRemove(key, null);
    }

    /**
     * Returns {@code true} if this map maps one or more keys to the
     * specified value.  This operation requires time linear in the
     * map size. Additionally, it is possible for the map to change
     * during execution of this method, in which case the returned
     * result may be inaccurate.
     *
     * @param value value whose presence in this map is to be tested
     * @return {@code true} if a mapping to {@code value} exists;
     *         {@code false} otherwise
     * @throws NullPointerException if the specified value is null
     */
    public boolean containsValue(Object value) {
        if (value == null)
            throw new NullPointerException();
        for (Node<K,V> n = findFirst(); n != null; n = n.next) {
            V v = n.getValidValue();
            if (v != null && value.equals(v))
                return true;
        }
        return false;
    }

    /**
     * Returns the number of key-value mappings in this map.  If this map
     * contains more than {@code Integer.MAX_VALUE} elements, it
     * returns {@code Integer.MAX_VALUE}.
     *
     * <p>Beware that, unlike in most collections, this method is
     * <em>NOT</em> a constant-time operation. Because of the
     * asynchronous nature of these maps, determining the current
     * number of elements requires traversing them all to count them.
     * Additionally, it is possible for the size to change during
     * execution of this method, in which case the returned result
     * will be inaccurate. Thus, this method is typically not very
     * useful in concurrent applications.
     *
     * @return the number of elements in this map
     */
    public int size() {
        long count = 0;
        for (Node<K,V> n = findFirst(); n != null; n = n.next) {
            if (n.getValidValue() != null)
                ++count;
        }
        return (count >= Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int) count;
    }

    /**
     * Returns {@code true} if this map contains no key-value mappings.
     * @return {@code true} if this map contains no key-value mappings
     */
    public boolean isEmpty() {
        return findFirst() == null;
    }

    /**
     * Removes all of the mappings from this map.
     */
    public void clear() {
        initialize();
    }

    /**
     * If the specified key is not already associated with a value,
     * attempts to compute its value using the given mapping function
     * and enters it into this map unless {@code null}.  The function
     * is <em>NOT</em> guaranteed to be applied once atomically only
     * if the value is not present.
     *
     * @param key key with which the specified value is to be associated
     * @param mappingFunction the function to compute a value
     * @return the current (existing or computed) value associated with
     *         the specified key, or null if the computed value is null
     * @throws NullPointerException if the specified key is null
     *         or the mappingFunction is null
     * @since 1.8
     */
    public V computeIfAbsent(K key,
                             Function<? super K, ? extends V> mappingFunction) {
        if (key == null || mappingFunction == null)
            throw new NullPointerException();
        V v, p, r;
        if ((v = doGet(key)) == null &&
            (r = mappingFunction.apply(key)) != null)
            v = (p = doPut(key, r, true)) == null ? r : p;
        return v;
    }

    /**
     * If the value for the specified key is present, attempts to
     * compute a new mapping given the key and its current mapped
     * value. The function is <em>NOT</em> guaranteed to be applied
     * once atomically.
     *
     * @param key key with which a value may be associated
     * @param remappingFunction the function to compute a value
     * @return the new value associated with the specified key, or null if none
     * @throws NullPointerException if the specified key is null
     *         or the remappingFunction is null
     * @since 1.8
     */
    public V computeIfPresent(K key,
                              BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        if (key == null || remappingFunction == null)
            throw new NullPointerException();
        Node<K,V> n; Object v;
        while ((n = findNode(key)) != null) {
            if ((v = n.value) != null) {
                @SuppressWarnings("unchecked") V vv = (V) v;
                V r = remappingFunction.apply(key, vv);
                if (r != null) {
                    if (n.casValue(vv, r))
                        return r;
                }
                else if (doRemove(key, vv) != null)
                    break;
            }
        }
        return null;
    }

    /**
     * Attempts to compute a mapping for the specified key and its
     * current mapped value (or {@code null} if there is no current
     * mapping). The function is <em>NOT</em> guaranteed to be applied
     * once atomically.
     *
     * @param key key with which the specified value is to be associated
     * @param remappingFunction the function to compute a value
     * @return the new value associated with the specified key, or null if none
     * @throws NullPointerException if the specified key is null
     *         or the remappingFunction is null
     * @since 1.8
     */
    public V compute(K key,
                     BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        if (key == null || remappingFunction == null)
            throw new NullPointerException();
        for (;;) {
            Node<K,V> n; Object v; V r;
            if ((n = findNode(key)) == null) {
                if ((r = remappingFunction.apply(key, null)) == null)
                    break;
                if (doPut(key, r, true) == null)
                    return r;
            }
            else if ((v = n.value) != null) {
                @SuppressWarnings("unchecked") V vv = (V) v;
                if ((r = remappingFunction.apply(key, vv)) != null) {
                    if (n.casValue(vv, r))
                        return r;
                }
                else if (doRemove(key, vv) != null)
                    break;
            }
        }
        return null;
    }

    /**
     * If the specified key is not already associated with a value,
     * associates it with the given value.  Otherwise, replaces the
     * value with the results of the given remapping function, or
     * removes if {@code null}. The function is <em>NOT</em>
     * guaranteed to be applied once atomically.
     *
     * @param key key with which the specified value is to be associated
     * @param value the value to use if absent
     * @param remappingFunction the function to recompute a value if present
     * @return the new value associated with the specified key, or null if none
     * @throws NullPointerException if the specified key or value is null
     *         or the remappingFunction is null
     * @since 1.8
     */
    public V merge(K key, V value,
                   BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        if (key == null || value == null || remappingFunction == null)
            throw new NullPointerException();
        for (;;) {
            Node<K,V> n; Object v; V r;
            if ((n = findNode(key)) == null) {
                if (doPut(key, value, true) == null)
                    return value;
            }
            else if ((v = n.value) != null) {
                @SuppressWarnings("unchecked") V vv = (V) v;
                if ((r = remappingFunction.apply(vv, value)) != null) {
                    if (n.casValue(vv, r))
                        return r;
                }
                else if (doRemove(key, vv) != null)
                    return null;
            }
        }
    }

    /* ---------------- View methods -------------- */

    /*
     * Note: Lazy initialization works for views because view classes
     * are stateless/immutable so it doesn't matter wrt correctness if
     * more than one is created (which will only rarely happen).  Even
     * so, the following idiom conservatively ensures that the method
     * returns the one it created if it does so, not one created by
     * another racing thread.
     */

    /**
     * Returns a {@link NavigableSet} view of the keys contained in this map.
     *
     * <p>The set's iterator returns the keys in ascending order.
     * The set's spliterator additionally reports {@link Spliterator#CONCURRENT},
     * {@link Spliterator#NONNULL}, {@link Spliterator#SORTED} and
     * {@link Spliterator#ORDERED}, with an encounter order that is ascending
     * key order.  The spliterator's comparator (see
     * {@link java.util.Spliterator#getComparator()}) is {@code null} if
     * the map's comparator (see {@link #comparator()}) is {@code null}.
     * Otherwise, the spliterator's comparator is the same as or imposes the
     * same total ordering as the map's comparator.
     *
     * <p>The set is backed by the map, so changes to the map are
     * reflected in the set, and vice-versa.  The set supports element
     * removal, which removes the corresponding mapping from the map,
     * via the {@code Iterator.remove}, {@code Set.remove},
     * {@code removeAll}, {@code retainAll}, and {@code clear}
     * operations.  It does not support the {@code add} or {@code addAll}
     * operations.
     *
     * <p>The view's iterators and spliterators are
     * <a href="package-summary.html#Weakly"><i>weakly consistent</i></a>.
     *
     * <p>This method is equivalent to method {@code navigableKeySet}.
     *
     * @return a navigable set view of the keys in this map
     */
    public NavigableSet<K> keySet() {
        KeySet<K> ks = keySet;
        return (ks != null) ? ks : (keySet = new KeySet<K>(this));
    }

    public NavigableSet<K> navigableKeySet() {
        KeySet<K> ks = keySet;
        return (ks != null) ? ks : (keySet = new KeySet<K>(this));
    }

    /**
     * Returns a {@link Collection} view of the values contained in this map.
     * <p>The collection's iterator returns the values in ascending order
     * of the corresponding keys. The collections's spliterator additionally
     * reports {@link Spliterator#CONCURRENT}, {@link Spliterator#NONNULL} and
     * {@link Spliterator#ORDERED}, with an encounter order that is ascending
     * order of the corresponding keys.
     *
     * <p>The collection is backed by the map, so changes to the map are
     * reflected in the collection, and vice-versa.  The collection
     * supports element removal, which removes the corresponding
     * mapping from the map, via the {@code Iterator.remove},
     * {@code Collection.remove}, {@code removeAll},
     * {@code retainAll} and {@code clear} operations.  It does not
     * support the {@code add} or {@code addAll} operations.
     *
     * <p>The view's iterators and spliterators are
     * <a href="package-summary.html#Weakly"><i>weakly consistent</i></a>.
     */
    public Collection<V> values() {
        Values<V> vs = values;
        return (vs != null) ? vs : (values = new Values<V>(this));
    }

    /**
     * Returns a {@link Set} view of the mappings contained in this map.
     *
     * <p>The set's iterator returns the entries in ascending key order.  The
     * set's spliterator additionally reports {@link Spliterator#CONCURRENT},
     * {@link Spliterator#NONNULL}, {@link Spliterator#SORTED} and
     * {@link Spliterator#ORDERED}, with an encounter order that is ascending
     * key order.
     *
     * <p>The set is backed by the map, so changes to the map are
     * reflected in the set, and vice-versa.  The set supports element
     * removal, which removes the corresponding mapping from the map,
     * via the {@code Iterator.remove}, {@code Set.remove},
     * {@code removeAll}, {@code retainAll} and {@code clear}
     * operations.  It does not support the {@code add} or
     * {@code addAll} operations.
     *
     * <p>The view's iterators and spliterators are
     * <a href="package-summary.html#Weakly"><i>weakly consistent</i></a>.
     *
     * <p>The {@code Map.Entry} elements traversed by the {@code iterator}
     * or {@code spliterator} do <em>not</em> support the {@code setValue}
     * operation.
     *
     * @return a set view of the mappings contained in this map,
     *         sorted in ascending key order
     */
    public Set<Map.Entry<K,V>> entrySet() {
        EntrySet<K,V> es = entrySet;
        return (es != null) ? es : (entrySet = new EntrySet<K,V>(this));
    }

    public ConcurrentNavigableMap<K,V> descendingMap() {
        ConcurrentNavigableMap<K,V> dm = descendingMap;
        return (dm != null) ? dm :
                // 本Map作为底层Map, 没有下限键, 不包含lo, 没有上限键, 不包含hi, 反向
                (descendingMap = new SubMap<K,V>(this, null, false, null, false, true));
    }

    public NavigableSet<K> descendingKeySet() {
        return descendingMap().navigableKeySet();
    }

    /* ---------------- AbstractMap Overrides -------------- */

    /**
     * Compares the specified object with this map for equality.
     * Returns {@code true} if the given object is also a map and the
     * two maps represent the same mappings.  More formally, two maps
     * {@code m1} and {@code m2} represent the same mappings if
     * {@code m1.entrySet().equals(m2.entrySet())}.  This
     * operation may return misleading results if either map is
     * concurrently modified during execution of this method.
     *
     * @param o object to be compared for equality with this map
     * @return {@code true} if the specified object is equal to this map
     */
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof Map))
            return false;
        Map<?,?> m = (Map<?,?>) o;
        try {
            for (Map.Entry<K,V> e : this.entrySet())
                if (! e.getValue().equals(m.get(e.getKey())))
                    return false;
            for (Map.Entry<?,?> e : m.entrySet()) {
                Object k = e.getKey();
                Object v = e.getValue();
                if (k == null || v == null || !v.equals(get(k)))
                    return false;
            }
            return true;
        } catch (ClassCastException unused) {
            return false;
        } catch (NullPointerException unused) {
            return false;
        }
    }

    /* ------ ConcurrentMap API methods ------ */

    /**
     * {@inheritDoc}
     *
     * @return the previous value associated with the specified key,
     *         or {@code null} if there was no mapping for the key
     * @throws ClassCastException if the specified key cannot be compared
     *         with the keys currently in the map
     * @throws NullPointerException if the specified key or value is null
     */
    public V putIfAbsent(K key, V value) {
        if (value == null)
            throw new NullPointerException();
        return doPut(key, value, true);
    }

    /**
     * {@inheritDoc}
     *
     * @throws ClassCastException if the specified key cannot be compared
     *         with the keys currently in the map
     * @throws NullPointerException if the specified key is null
     */
    public boolean remove(Object key, Object value) {
        if (key == null)
            throw new NullPointerException();
        return value != null && doRemove(key, value) != null;
    }

    /**
     * {@inheritDoc}
     *
     * @throws ClassCastException if the specified key cannot be compared
     *         with the keys currently in the map
     * @throws NullPointerException if any of the arguments are null
     */
    public boolean replace(K key, V oldValue, V newValue) {
        if (key == null || oldValue == null || newValue == null)
            throw new NullPointerException();
        for (;;) {
            Node<K,V> n; Object v;
            if ((n = findNode(key)) == null)
                return false;
            if ((v = n.value) != null) {
                if (!oldValue.equals(v))
                    return false;
                if (n.casValue(v, newValue))
                    return true;
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return the previous value associated with the specified key,
     *         or {@code null} if there was no mapping for the key
     * @throws ClassCastException if the specified key cannot be compared
     *         with the keys currently in the map
     * @throws NullPointerException if the specified key or value is null
     */
    public V replace(K key, V value) {
        if (key == null || value == null)
            throw new NullPointerException();
        for (;;) {
            Node<K,V> n; Object v;
            if ((n = findNode(key)) == null)
                return null;
            if ((v = n.value) != null && n.casValue(v, value)) {
                @SuppressWarnings("unchecked") V vv = (V)v;
                return vv;
            }
        }
    }

    /* ------ SortedMap API methods ------ */

    public Comparator<? super K> comparator() {
        return comparator;
    }

    /**
     * @throws NoSuchElementException {@inheritDoc}
     */
    public K firstKey() {
        // findNode的特殊变体, 用于获取第一个有效节点
        Node<K,V> n = findFirst();
        if (n == null)
            throw new NoSuchElementException();
        return n.key;
    }

    /**
     * @throws NoSuchElementException {@inheritDoc}
     */
    public K lastKey() {
        // 获取最后一个有效节点: 先走到最后一个索引结点, 再从该索引的数据结点遍历到最后一个数据结点，对比findPredecessor，该方法不需要比较key大小
        Node<K,V> n = findLast();
        if (n == null)
            throw new NoSuchElementException();
        return n.key;
    }

    /**
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException if {@code fromKey} or {@code toKey} is null
     * @throws IllegalArgumentException {@inheritDoc}
     */
    public ConcurrentNavigableMap<K,V> subMap(K fromKey,
                                              boolean fromInclusive,
                                              K toKey,
                                              boolean toInclusive) {
        if (fromKey == null || toKey == null)
            throw new NullPointerException();
        return new SubMap<K,V>
            (this, fromKey, fromInclusive, toKey, toInclusive, false);
    }

    /**
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException if {@code toKey} is null
     * @throws IllegalArgumentException {@inheritDoc}
     */
    public ConcurrentNavigableMap<K,V> headMap(K toKey,
                                               boolean inclusive) {
        if (toKey == null)
            throw new NullPointerException();
        return new SubMap<K,V>
            (this, null, false, toKey, inclusive, false);
    }

    /**
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException if {@code fromKey} is null
     * @throws IllegalArgumentException {@inheritDoc}
     */
    public ConcurrentNavigableMap<K,V> tailMap(K fromKey,
                                               boolean inclusive) {
        if (fromKey == null)
            throw new NullPointerException();
        return new SubMap<K,V>
            (this, fromKey, inclusive, null, false, false);
    }

    /**
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException if {@code fromKey} or {@code toKey} is null
     * @throws IllegalArgumentException {@inheritDoc}
     */
    public ConcurrentNavigableMap<K,V> subMap(K fromKey, K toKey) {
        return subMap(fromKey, true, toKey, false);
    }

    /**
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException if {@code toKey} is null
     * @throws IllegalArgumentException {@inheritDoc}
     */
    public ConcurrentNavigableMap<K,V> headMap(K toKey) {
        return headMap(toKey, false);
    }

    /**
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException if {@code fromKey} is null
     * @throws IllegalArgumentException {@inheritDoc}
     */
    public ConcurrentNavigableMap<K,V> tailMap(K fromKey) {
        return tailMap(fromKey, true);
    }

    /* ---------------- Relational operations -------------- */

    /**
     * Returns a key-value mapping associated with the greatest key
     * strictly less than the given key, or {@code null} if there is
     * no such key. The returned entry does <em>not</em> support the
     * {@code Entry.setValue} method.
     *
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException if the specified key is null
     */
    public Map.Entry<K,V> lowerEntry(K key) {
        return getNear(key, LT);
    }

    /**
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException if the specified key is null
     */
    public K lowerKey(K key) {
        Node<K,V> n = findNear(key, LT, comparator);
        return (n == null) ? null : n.key;
    }

    /**
     * Returns a key-value mapping associated with the greatest key
     * less than or equal to the given key, or {@code null} if there
     * is no such key. The returned entry does <em>not</em> support
     * the {@code Entry.setValue} method.
     *
     * @param key the key
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException if the specified key is null
     */
    public Map.Entry<K,V> floorEntry(K key) {
        return getNear(key, LT|EQ);
    }

    /**
     * @param key the key
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException if the specified key is null
     */
    public K floorKey(K key) {
        Node<K,V> n = findNear(key, LT|EQ, comparator);
        return (n == null) ? null : n.key;
    }

    /**
     * Returns a key-value mapping associated with the least key
     * greater than or equal to the given key, or {@code null} if
     * there is no such entry. The returned entry does <em>not</em>
     * support the {@code Entry.setValue} method.
     *
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException if the specified key is null
     */
    public Map.Entry<K,V> ceilingEntry(K key) {
        return getNear(key, GT|EQ);
    }

    /**
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException if the specified key is null
     */
    public K ceilingKey(K key) {
        Node<K,V> n = findNear(key, GT|EQ, comparator);
        return (n == null) ? null : n.key;
    }

    /**
     * Returns a key-value mapping associated with the least key
     * strictly greater than the given key, or {@code null} if there
     * is no such key. The returned entry does <em>not</em> support
     * the {@code Entry.setValue} method.
     *
     * @param key the key
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException if the specified key is null
     */
    public Map.Entry<K,V> higherEntry(K key) {
        return getNear(key, GT);
    }

    /**
     * @param key the key
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException if the specified key is null
     */
    public K higherKey(K key) {
        Node<K,V> n = findNear(key, GT, comparator);
        return (n == null) ? null : n.key;
    }

    /**
     * Returns a key-value mapping associated with the least
     * key in this map, or {@code null} if the map is empty.
     * The returned entry does <em>not</em> support
     * the {@code Entry.setValue} method.
     */
    public Map.Entry<K,V> firstEntry() {
        for (;;) {
            // findNode的特殊变体, 用于获取第一个有效节点, 如果返回之前刚好n被置null了, 则通过标记后继结点辅助删除实例结点
            Node<K,V> n = findFirst();
            if (n == null)
                return null;
            AbstractMap.SimpleImmutableEntry<K,V> e = n.createSnapshot();
            if (e != null)
                return e;
        }
    }

    /**
     * Returns a key-value mapping associated with the greatest
     * key in this map, or {@code null} if the map is empty.
     * The returned entry does <em>not</em> support
     * the {@code Entry.setValue} method.
     */
    public Map.Entry<K,V> lastEntry() {
        for (;;) {
            // 获取最后一个有效节点: 先走到最后一个索引结点, 再从该索引的数据结点遍历到最后一个数据结点，对比findPredecessor，该方法不需要比较key大小
            Node<K,V> n = findLast();
            if (n == null)
                return null;
            AbstractMap.SimpleImmutableEntry<K,V> e = n.createSnapshot();
            if (e != null)
                return e;
        }
    }

    /**
     * Removes and returns a key-value mapping associated with
     * the least key in this map, or {@code null} if the map is empty.
     * The returned entry does <em>not</em> support
     * the {@code Entry.setValue} method.
     */
    // 删除并返回与此映射中最小键关联的键值映射，如果映射为空，则返回 {@code null}。 返回的条目不支持 {@code Entry.setValue} 方法。
    public Map.Entry<K,V> pollFirstEntry() {
        // 删除第一个条目的数据结点以及与之对应的每层的索引结点, 并返回其键和值的快照
        return doRemoveFirstEntry();
    }

    /**
     * Removes and returns a key-value mapping associated with
     * the greatest key in this map, or {@code null} if the map is empty.
     * The returned entry does <em>not</em> support
     * the {@code Entry.setValue} method.
     */
    public Map.Entry<K,V> pollLastEntry() {
        // 删除最后一个条目的数据结点以及与之对应的每层的索引结点, 并返回其键和值的快照
        return doRemoveLastEntry();
    }


    /* ---------------- Iterators -------------- */

    /**
     * Base of iterator classes:
     */
    // 迭代器基类
    abstract class Iter<T> implements Iterator<T> {
        /** the last node returned by next() */
        // next() 返回的最后一个节点
        Node<K,V> lastReturned;

        /** the next node to return from next(); */
        // 从 next() 返回的下一个节点；
        Node<K,V> next;

        /** Cache of next value field to maintain weak consistency */
        // 缓存下一个值字段以保持弱一致性
        V nextValue;

        // 初始化整个范围的升序迭代器。
        /** Initializes ascending iterator for entire range. */
        Iter() {
            while ((next = findFirst()) != null) {
                Object x = next.value;
                if (x != null && x != next) {
                    @SuppressWarnings("unchecked") V vv = (V)x;
                    nextValue = vv;
                    break;
                }
            }
        }

        public final boolean hasNext() {
            return next != null;
        }

        /** Advances next to higher entry. */
        final void advance() {
            if (next == null)
                throw new NoSuchElementException();
            lastReturned = next;
            while ((next = next.next) != null) {
                Object x = next.value;
                if (x != null && x != next) {
                    @SuppressWarnings("unchecked") V vv = (V)x;
                    nextValue = vv;
                    break;
                }
            }
        }

        public void remove() {
            Node<K,V> l = lastReturned;
            if (l == null)
                throw new IllegalStateException();
            // It would not be worth all of the overhead to directly
            // unlink from here. Using remove is fast enough.
            ConcurrentSkipListMap.this.remove(l.key);
            lastReturned = null;
        }

    }

    final class ValueIterator extends Iter<V> {
        public V next() {
            V v = nextValue;
            advance();
            return v;
        }
    }

    final class KeyIterator extends Iter<K> {
        public K next() {
            Node<K,V> n = next;
            advance();
            return n.key;
        }
    }

    final class EntryIterator extends Iter<Map.Entry<K,V>> {
        public Map.Entry<K,V> next() {
            Node<K,V> n = next;
            V v = nextValue;
            advance();
            return new AbstractMap.SimpleImmutableEntry<K,V>(n.key, v);
        }
    }

    // Factory methods for iterators needed by ConcurrentSkipListSet etc

    Iterator<K> keyIterator() {
        return new KeyIterator();
    }

    Iterator<V> valueIterator() {
        return new ValueIterator();
    }

    Iterator<Map.Entry<K,V>> entryIterator() {
        return new EntryIterator();
    }

    /* ---------------- View Classes -------------- */

    /*
     * View classes are static, delegating to a ConcurrentNavigableMap
     * to allow use by SubMaps, which outweighs the ugliness of
     * needing type-tests for Iterator methods.
     */

    static final <E> List<E> toList(Collection<E> c) {
        // Using size() here would be a pessimization.
        ArrayList<E> list = new ArrayList<E>();
        for (E e : c)
            list.add(e);
        return list;
    }

    static final class KeySet<E>
            extends AbstractSet<E> implements NavigableSet<E> {
        final ConcurrentNavigableMap<E,?> m;
        KeySet(ConcurrentNavigableMap<E,?> map) { m = map; }
        public int size() { return m.size(); }
        public boolean isEmpty() { return m.isEmpty(); }
        public boolean contains(Object o) { return m.containsKey(o); }
        public boolean remove(Object o) { return m.remove(o) != null; }
        public void clear() { m.clear(); }
        public E lower(E e) { return m.lowerKey(e); }
        public E floor(E e) { return m.floorKey(e); }
        public E ceiling(E e) { return m.ceilingKey(e); }
        public E higher(E e) { return m.higherKey(e); }
        public Comparator<? super E> comparator() { return m.comparator(); }
        public E first() { return m.firstKey(); }
        public E last() { return m.lastKey(); }
        public E pollFirst() {
            Map.Entry<E,?> e = m.pollFirstEntry();
            return (e == null) ? null : e.getKey();
        }
        public E pollLast() {
            Map.Entry<E,?> e = m.pollLastEntry();
            return (e == null) ? null : e.getKey();
        }
        @SuppressWarnings("unchecked")
        public Iterator<E> iterator() {
            if (m instanceof ConcurrentSkipListMap)
                return ((ConcurrentSkipListMap<E,Object>)m).keyIterator();
            else
                return ((ConcurrentSkipListMap.SubMap<E,Object>)m).keyIterator();
        }
        public boolean equals(Object o) {
            if (o == this)
                return true;
            if (!(o instanceof Set))
                return false;
            Collection<?> c = (Collection<?>) o;
            try {
                return containsAll(c) && c.containsAll(this);
            } catch (ClassCastException unused) {
                return false;
            } catch (NullPointerException unused) {
                return false;
            }
        }
        public Object[] toArray()     { return toList(this).toArray();  }
        public <T> T[] toArray(T[] a) { return toList(this).toArray(a); }
        public Iterator<E> descendingIterator() {
            return descendingSet().iterator();
        }
        public NavigableSet<E> subSet(E fromElement,
                                      boolean fromInclusive,
                                      E toElement,
                                      boolean toInclusive) {
            return new KeySet<E>(m.subMap(fromElement, fromInclusive,
                                          toElement,   toInclusive));
        }
        public NavigableSet<E> headSet(E toElement, boolean inclusive) {
            return new KeySet<E>(m.headMap(toElement, inclusive));
        }
        public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
            return new KeySet<E>(m.tailMap(fromElement, inclusive));
        }
        public NavigableSet<E> subSet(E fromElement, E toElement) {
            return subSet(fromElement, true, toElement, false);
        }
        public NavigableSet<E> headSet(E toElement) {
            return headSet(toElement, false);
        }
        public NavigableSet<E> tailSet(E fromElement) {
            return tailSet(fromElement, true);
        }
        public NavigableSet<E> descendingSet() {
            return new KeySet<E>(m.descendingMap());
        }
        @SuppressWarnings("unchecked")
        public Spliterator<E> spliterator() {
            if (m instanceof ConcurrentSkipListMap)
                return ((ConcurrentSkipListMap<E,?>)m).keySpliterator();
            else
                return (Spliterator<E>)((SubMap<E,?>)m).keyIterator();
        }
    }

    static final class Values<E> extends AbstractCollection<E> {
        final ConcurrentNavigableMap<?, E> m;
        Values(ConcurrentNavigableMap<?, E> map) {
            m = map;
        }
        @SuppressWarnings("unchecked")
        public Iterator<E> iterator() {
            if (m instanceof ConcurrentSkipListMap)
                return ((ConcurrentSkipListMap<?,E>)m).valueIterator();
            else
                return ((SubMap<?,E>)m).valueIterator();
        }
        public boolean isEmpty() {
            return m.isEmpty();
        }
        public int size() {
            return m.size();
        }
        public boolean contains(Object o) {
            return m.containsValue(o);
        }
        public void clear() {
            m.clear();
        }
        public Object[] toArray()     { return toList(this).toArray();  }
        public <T> T[] toArray(T[] a) { return toList(this).toArray(a); }
        @SuppressWarnings("unchecked")
        public Spliterator<E> spliterator() {
            if (m instanceof ConcurrentSkipListMap)
                return ((ConcurrentSkipListMap<?,E>)m).valueSpliterator();
            else
                return (Spliterator<E>)((SubMap<?,E>)m).valueIterator();
        }
    }

    static final class EntrySet<K1,V1> extends AbstractSet<Map.Entry<K1,V1>> {
        final ConcurrentNavigableMap<K1, V1> m;
        EntrySet(ConcurrentNavigableMap<K1, V1> map) {
            m = map;
        }
        @SuppressWarnings("unchecked")
        public Iterator<Map.Entry<K1,V1>> iterator() {
            if (m instanceof ConcurrentSkipListMap)
                return ((ConcurrentSkipListMap<K1,V1>)m).entryIterator();
            else
                return ((SubMap<K1,V1>)m).entryIterator();
        }

        public boolean contains(Object o) {
            if (!(o instanceof Map.Entry))
                return false;
            Map.Entry<?,?> e = (Map.Entry<?,?>)o;
            V1 v = m.get(e.getKey());
            return v != null && v.equals(e.getValue());
        }
        public boolean remove(Object o) {
            if (!(o instanceof Map.Entry))
                return false;
            Map.Entry<?,?> e = (Map.Entry<?,?>)o;
            return m.remove(e.getKey(),
                            e.getValue());
        }
        public boolean isEmpty() {
            return m.isEmpty();
        }
        public int size() {
            return m.size();
        }
        public void clear() {
            m.clear();
        }
        public boolean equals(Object o) {
            if (o == this)
                return true;
            if (!(o instanceof Set))
                return false;
            Collection<?> c = (Collection<?>) o;
            try {
                return containsAll(c) && c.containsAll(this);
            } catch (ClassCastException unused) {
                return false;
            } catch (NullPointerException unused) {
                return false;
            }
        }
        public Object[] toArray()     { return toList(this).toArray();  }
        public <T> T[] toArray(T[] a) { return toList(this).toArray(a); }
        @SuppressWarnings("unchecked")
        public Spliterator<Map.Entry<K1,V1>> spliterator() {
            if (m instanceof ConcurrentSkipListMap)
                return ((ConcurrentSkipListMap<K1,V1>)m).entrySpliterator();
            else
                return (Spliterator<Map.Entry<K1,V1>>)
                    ((SubMap<K1,V1>)m).entryIterator();
        }
    }

    /**
     * 20210627
     * {@link ConcurrentSkipListMap} 子Map操作返回的子Map表示其底层Map的映射子范围。 此类的实例支持其底层映射的所有方法，不同之处在于其范围外的映射将被忽略，
     * 并且尝试添加其范围外的映射会导致 {@link IllegalArgumentException}。 此类的实例仅使用其底层映射的 {@code subMap}、{@code headMap} 和 {@code tailMap} 方法构造。
     */
    /**
     * Submaps returned by {@link ConcurrentSkipListMap} submap operations
     * represent a subrange of mappings of their underlying
     * maps. Instances of this class support all methods of their
     * underlying maps, differing in that mappings outside their range are
     * ignored, and attempts to add mappings outside their ranges result
     * in {@link IllegalArgumentException}.  Instances of this class are
     * constructed only using the {@code subMap}, {@code headMap}, and
     * {@code tailMap} methods of their underlying maps.
     *
     * @serial include
     */
    static final class SubMap<K,V> extends AbstractMap<K,V> implements ConcurrentNavigableMap<K,V>, Cloneable, Serializable {
        private static final long serialVersionUID = -7647078645895051609L;

        /** Underlying map */
        // 底层Map
        private final ConcurrentSkipListMap<K,V> m;
        /** lower bound key, or null if from start */
        // 下限键，如果从头开始，则为 null
        private final K lo;
        /** upper bound key, or null if to end */
        // 上限键，如果结束则为空
        private final K hi;
        /** inclusion flag for lo */
        // lo 的包含标志
        private final boolean loInclusive;
        /** inclusion flag for hi */
        // hi 的包含标志
        private final boolean hiInclusive;
        /** direction */
        // 方向
        private final boolean isDescending;

        // Lazily initialized view holders
        // 延迟初始化的视图持有者
        private transient KeySet<K> keySetView;
        private transient Set<Map.Entry<K,V>> entrySetView;
        private transient Collection<V> valuesView;

        /**
         * Creates a new submap, initializing all fields.
         */
        // 创建一个新的子图，初始化所有字段。
        SubMap(ConcurrentSkipListMap<K,V> map,
               K fromKey, boolean fromInclusive,
               K toKey, boolean toInclusive,
               boolean isDescending) {
            Comparator<? super K> cmp = map.comparator;
            if (fromKey != null && toKey != null &&
                cpr(cmp, fromKey, toKey) > 0)
                throw new IllegalArgumentException("inconsistent range");
            this.m = map;
            this.lo = fromKey;
            this.hi = toKey;
            this.loInclusive = fromInclusive;
            this.hiInclusive = toInclusive;
            this.isDescending = isDescending;
        }

        /* ----------------  Utilities -------------- */

        boolean tooLow(Object key, Comparator<? super K> cmp) {
            int c;
            return (lo != null && ((c = cpr(cmp, key, lo)) < 0 ||
                                   (c == 0 && !loInclusive)));
        }

        boolean tooHigh(Object key, Comparator<? super K> cmp) {
            int c;
            return (hi != null && ((c = cpr(cmp, key, hi)) > 0 ||
                                   (c == 0 && !hiInclusive)));
        }

        boolean inBounds(Object key, Comparator<? super K> cmp) {
            return !tooLow(key, cmp) && !tooHigh(key, cmp);
        }

        void checkKeyBounds(K key, Comparator<? super K> cmp) {
            if (key == null)
                throw new NullPointerException();
            if (!inBounds(key, cmp))
                throw new IllegalArgumentException("key out of range");
        }

        /**
         * Returns true if node key is less than upper bound of range.
         */
        // 如果节点键小于范围上限，则返回 true。
        boolean isBeforeEnd(ConcurrentSkipListMap.Node<K,V> n,
                            Comparator<? super K> cmp) {
            if (n == null)
                return false;
            if (hi == null)
                return true;
            K k = n.key;
            if (k == null) // pass by markers and headers
                return true;
            int c = cpr(cmp, k, hi);
            if (c > 0 || (c == 0 && !hiInclusive))
                return false;
            return true;
        }

        /**
         * Returns lowest node. This node might not be in range, so
         * most usages need to check bounds.
         */
        // 返回最低节点。 此节点可能不在范围内，因此大多数用法需要检查边界。
        ConcurrentSkipListMap.Node<K,V> loNode(Comparator<? super K> cmp) {
            if (lo == null)
                return m.findFirst();
            else if (loInclusive)
                return m.findNear(lo, GT|EQ, cmp);
            else
                return m.findNear(lo, GT, cmp);
        }

        /**
         * Returns highest node. This node might not be in range, so
         * most usages need to check bounds.
         */
        // 返回最高节点。 此节点可能不在范围内，因此大多数用法需要检查边界。
        ConcurrentSkipListMap.Node<K,V> hiNode(Comparator<? super K> cmp) {
            if (hi == null)
                return m.findLast();
            else if (hiInclusive)
                return m.findNear(hi, LT|EQ, cmp);
            else
                return m.findNear(hi, LT, cmp);
        }

        /**
         * Returns lowest absolute key (ignoring directonality).
         */
        // 返回最低绝对键（忽略方向性）
        K lowestKey() {
            Comparator<? super K> cmp = m.comparator;
            ConcurrentSkipListMap.Node<K,V> n = loNode(cmp);
            if (isBeforeEnd(n, cmp))
                return n.key;
            else
                throw new NoSuchElementException();
        }

        /**
         * Returns highest absolute key (ignoring directonality).
         */
        // 返回最高绝对键（忽略方向性）。
        K highestKey() {
            Comparator<? super K> cmp = m.comparator;
            ConcurrentSkipListMap.Node<K,V> n = hiNode(cmp);
            if (n != null) {
                K last = n.key;
                if (inBounds(last, cmp))
                    return last;
            }
            throw new NoSuchElementException();
        }

        Map.Entry<K,V> lowestEntry() {
            Comparator<? super K> cmp = m.comparator;
            for (;;) {
                ConcurrentSkipListMap.Node<K,V> n = loNode(cmp);
                if (!isBeforeEnd(n, cmp))
                    return null;
                Map.Entry<K,V> e = n.createSnapshot();
                if (e != null)
                    return e;
            }
        }

        Map.Entry<K,V> highestEntry() {
            Comparator<? super K> cmp = m.comparator;
            for (;;) {
                ConcurrentSkipListMap.Node<K,V> n = hiNode(cmp);
                if (n == null || !inBounds(n.key, cmp))
                    return null;
                Map.Entry<K,V> e = n.createSnapshot();
                if (e != null)
                    return e;
            }
        }

        Map.Entry<K,V> removeLowest() {
            Comparator<? super K> cmp = m.comparator;
            for (;;) {
                Node<K,V> n = loNode(cmp);
                if (n == null)
                    return null;
                K k = n.key;
                if (!inBounds(k, cmp))
                    return null;
                V v = m.doRemove(k, null);
                if (v != null)
                    return new AbstractMap.SimpleImmutableEntry<K,V>(k, v);
            }
        }

        Map.Entry<K,V> removeHighest() {
            Comparator<? super K> cmp = m.comparator;
            for (;;) {
                Node<K,V> n = hiNode(cmp);
                if (n == null)
                    return null;
                K k = n.key;
                if (!inBounds(k, cmp))
                    return null;
                V v = m.doRemove(k, null);
                if (v != null)
                    return new AbstractMap.SimpleImmutableEntry<K,V>(k, v);
            }
        }

        /**
         * Submap version of ConcurrentSkipListMap.getNearEntry
         */
        // ConcurrentSkipListMap.getNearEntry 的子Map版本
        Map.Entry<K,V> getNearEntry(K key, int rel) {
            Comparator<? super K> cmp = m.comparator;
            if (isDescending) { // adjust relation for direction
                if ((rel & LT) == 0)
                    rel |= LT;
                else
                    rel &= ~LT;
            }
            if (tooLow(key, cmp))
                return ((rel & LT) != 0) ? null : lowestEntry();
            if (tooHigh(key, cmp))
                return ((rel & LT) != 0) ? highestEntry() : null;
            for (;;) {
                Node<K,V> n = m.findNear(key, rel, cmp);
                if (n == null || !inBounds(n.key, cmp))
                    return null;
                K k = n.key;
                V v = n.getValidValue();
                if (v != null)
                    return new AbstractMap.SimpleImmutableEntry<K,V>(k, v);
            }
        }

        // Almost the same as getNearEntry, except for keys
        // 与 getNearEntry 几乎相同，除了键
        K getNearKey(K key, int rel) {
            Comparator<? super K> cmp = m.comparator;
            if (isDescending) { // adjust relation for direction
                if ((rel & LT) == 0)
                    rel |= LT;
                else
                    rel &= ~LT;
            }
            if (tooLow(key, cmp)) {
                if ((rel & LT) == 0) {
                    ConcurrentSkipListMap.Node<K,V> n = loNode(cmp);
                    if (isBeforeEnd(n, cmp))
                        return n.key;
                }
                return null;
            }
            if (tooHigh(key, cmp)) {
                if ((rel & LT) != 0) {
                    ConcurrentSkipListMap.Node<K,V> n = hiNode(cmp);
                    if (n != null) {
                        K last = n.key;
                        if (inBounds(last, cmp))
                            return last;
                    }
                }
                return null;
            }
            for (;;) {
                Node<K,V> n = m.findNear(key, rel, cmp);
                if (n == null || !inBounds(n.key, cmp))
                    return null;
                K k = n.key;
                V v = n.getValidValue();
                if (v != null)
                    return k;
            }
        }

        /* ----------------  Map API methods -------------- */

        public boolean containsKey(Object key) {
            if (key == null) throw new NullPointerException();
            return inBounds(key, m.comparator) && m.containsKey(key);
        }

        public V get(Object key) {
            if (key == null) throw new NullPointerException();
            return (!inBounds(key, m.comparator)) ? null : m.get(key);
        }

        public V put(K key, V value) {
            checkKeyBounds(key, m.comparator);
            return m.put(key, value);
        }

        public V remove(Object key) {
            return (!inBounds(key, m.comparator)) ? null : m.remove(key);
        }

        public int size() {
            Comparator<? super K> cmp = m.comparator;
            long count = 0;
            for (ConcurrentSkipListMap.Node<K,V> n = loNode(cmp);
                 isBeforeEnd(n, cmp);
                 n = n.next) {
                if (n.getValidValue() != null)
                    ++count;
            }
            return count >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int)count;
        }

        public boolean isEmpty() {
            Comparator<? super K> cmp = m.comparator;
            return !isBeforeEnd(loNode(cmp), cmp);
        }

        public boolean containsValue(Object value) {
            if (value == null)
                throw new NullPointerException();
            Comparator<? super K> cmp = m.comparator;
            for (ConcurrentSkipListMap.Node<K,V> n = loNode(cmp);
                 isBeforeEnd(n, cmp);
                 n = n.next) {
                V v = n.getValidValue();
                if (v != null && value.equals(v))
                    return true;
            }
            return false;
        }

        public void clear() {
            Comparator<? super K> cmp = m.comparator;
            for (ConcurrentSkipListMap.Node<K,V> n = loNode(cmp);
                 isBeforeEnd(n, cmp);
                 n = n.next) {
                if (n.getValidValue() != null)
                    m.remove(n.key);
            }
        }

        /* ----------------  ConcurrentMap API methods -------------- */

        public V putIfAbsent(K key, V value) {
            checkKeyBounds(key, m.comparator);
            return m.putIfAbsent(key, value);
        }

        public boolean remove(Object key, Object value) {
            return inBounds(key, m.comparator) && m.remove(key, value);
        }

        public boolean replace(K key, V oldValue, V newValue) {
            checkKeyBounds(key, m.comparator);
            return m.replace(key, oldValue, newValue);
        }

        public V replace(K key, V value) {
            checkKeyBounds(key, m.comparator);
            return m.replace(key, value);
        }

        /* ----------------  SortedMap API methods -------------- */

        public Comparator<? super K> comparator() {
            Comparator<? super K> cmp = m.comparator();
            if (isDescending)
                return Collections.reverseOrder(cmp);
            else
                return cmp;
        }

        /**
         * Utility to create submaps, where given bounds override
         * unbounded(null) ones and/or are checked against bounded ones.
         */
        // 创建子图的实用程序，其中给定的边界覆盖无界（空）的和/或根据有界的进行检查。
        SubMap<K,V> newSubMap(K fromKey, boolean fromInclusive,
                              K toKey, boolean toInclusive) {
            Comparator<? super K> cmp = m.comparator;
            if (isDescending) { // flip senses
                K tk = fromKey;
                fromKey = toKey;
                toKey = tk;
                boolean ti = fromInclusive;
                fromInclusive = toInclusive;
                toInclusive = ti;
            }
            if (lo != null) {
                if (fromKey == null) {
                    fromKey = lo;
                    fromInclusive = loInclusive;
                }
                else {
                    int c = cpr(cmp, fromKey, lo);
                    if (c < 0 || (c == 0 && !loInclusive && fromInclusive))
                        throw new IllegalArgumentException("key out of range");
                }
            }
            if (hi != null) {
                if (toKey == null) {
                    toKey = hi;
                    toInclusive = hiInclusive;
                }
                else {
                    int c = cpr(cmp, toKey, hi);
                    if (c > 0 || (c == 0 && !hiInclusive && toInclusive))
                        throw new IllegalArgumentException("key out of range");
                }
            }
            return new SubMap<K,V>(m, fromKey, fromInclusive,
                                   toKey, toInclusive, isDescending);
        }

        public SubMap<K,V> subMap(K fromKey, boolean fromInclusive,
                                  K toKey, boolean toInclusive) {
            if (fromKey == null || toKey == null)
                throw new NullPointerException();
            return newSubMap(fromKey, fromInclusive, toKey, toInclusive);
        }

        public SubMap<K,V> headMap(K toKey, boolean inclusive) {
            if (toKey == null)
                throw new NullPointerException();
            return newSubMap(null, false, toKey, inclusive);
        }

        public SubMap<K,V> tailMap(K fromKey, boolean inclusive) {
            if (fromKey == null)
                throw new NullPointerException();
            return newSubMap(fromKey, inclusive, null, false);
        }

        public SubMap<K,V> subMap(K fromKey, K toKey) {
            return subMap(fromKey, true, toKey, false);
        }

        public SubMap<K,V> headMap(K toKey) {
            return headMap(toKey, false);
        }

        public SubMap<K,V> tailMap(K fromKey) {
            return tailMap(fromKey, true);
        }

        public SubMap<K,V> descendingMap() {
            return new SubMap<K,V>(m, lo, loInclusive,
                                   hi, hiInclusive, !isDescending);
        }

        /* ----------------  Relational methods -------------- */

        public Map.Entry<K,V> ceilingEntry(K key) {
            return getNearEntry(key, GT|EQ);
        }

        public K ceilingKey(K key) {
            return getNearKey(key, GT|EQ);
        }

        public Map.Entry<K,V> lowerEntry(K key) {
            return getNearEntry(key, LT);
        }

        public K lowerKey(K key) {
            return getNearKey(key, LT);
        }

        public Map.Entry<K,V> floorEntry(K key) {
            return getNearEntry(key, LT|EQ);
        }

        public K floorKey(K key) {
            return getNearKey(key, LT|EQ);
        }

        public Map.Entry<K,V> higherEntry(K key) {
            return getNearEntry(key, GT);
        }

        public K higherKey(K key) {
            return getNearKey(key, GT);
        }

        public K firstKey() {
            return isDescending ? highestKey() : lowestKey();
        }

        public K lastKey() {
            return isDescending ? lowestKey() : highestKey();
        }

        public Map.Entry<K,V> firstEntry() {
            return isDescending ? highestEntry() : lowestEntry();
        }

        public Map.Entry<K,V> lastEntry() {
            return isDescending ? lowestEntry() : highestEntry();
        }

        public Map.Entry<K,V> pollFirstEntry() {
            return isDescending ? removeHighest() : removeLowest();
        }

        public Map.Entry<K,V> pollLastEntry() {
            return isDescending ? removeLowest() : removeHighest();
        }

        /* ---------------- Submap Views -------------- */

        public NavigableSet<K> keySet() {
            KeySet<K> ks = keySetView;
            return (ks != null) ? ks : (keySetView = new KeySet<K>(this));
        }

        public NavigableSet<K> navigableKeySet() {
            KeySet<K> ks = keySetView;
            return (ks != null) ? ks : (keySetView = new KeySet<K>(this));
        }

        public Collection<V> values() {
            Collection<V> vs = valuesView;
            return (vs != null) ? vs : (valuesView = new Values<V>(this));
        }

        public Set<Map.Entry<K,V>> entrySet() {
            Set<Map.Entry<K,V>> es = entrySetView;
            return (es != null) ? es : (entrySetView = new EntrySet<K,V>(this));
        }

        public NavigableSet<K> descendingKeySet() {
            return descendingMap().navigableKeySet();
        }

        Iterator<K> keyIterator() {
            return new SubMapKeyIterator();
        }

        Iterator<V> valueIterator() {
            return new SubMapValueIterator();
        }

        Iterator<Map.Entry<K,V>> entryIterator() {
            return new SubMapEntryIterator();
        }

        /**
         * Variant of main Iter class to traverse through submaps.
         * Also serves as back-up Spliterator for views
         */
        // 用于遍历子图的主 Iter 类的变体。 也可作为视图的备用 Spliterator
        abstract class SubMapIter<T> implements Iterator<T>, Spliterator<T> {
            /** the last node returned by next() */
            // next() 返回的最后一个节点
            Node<K,V> lastReturned;
            /** the next node to return from next(); */
            // 从 next() 返回的下一个节点；
            Node<K,V> next;
            /** Cache of next value field to maintain weak consistency */
            // 缓存下一个值字段以保持弱一致性
            V nextValue;

            SubMapIter() {
                Comparator<? super K> cmp = m.comparator;
                for (;;) {
                    next = isDescending ? hiNode(cmp) : loNode(cmp);
                    if (next == null)
                        break;
                    Object x = next.value;
                    if (x != null && x != next) {
                        if (! inBounds(next.key, cmp))
                            next = null;
                        else {
                            @SuppressWarnings("unchecked") V vv = (V)x;
                            nextValue = vv;
                        }
                        break;
                    }
                }
            }

            public final boolean hasNext() {
                return next != null;
            }

            final void advance() {
                if (next == null)
                    throw new NoSuchElementException();
                lastReturned = next;
                if (isDescending)
                    descend();
                else
                    ascend();
            }

            private void ascend() {
                Comparator<? super K> cmp = m.comparator;
                for (;;) {
                    next = next.next;
                    if (next == null)
                        break;
                    Object x = next.value;
                    if (x != null && x != next) {
                        if (tooHigh(next.key, cmp))
                            next = null;
                        else {
                            @SuppressWarnings("unchecked") V vv = (V)x;
                            nextValue = vv;
                        }
                        break;
                    }
                }
            }

            private void descend() {
                Comparator<? super K> cmp = m.comparator;
                for (;;) {
                    next = m.findNear(lastReturned.key, LT, cmp);
                    if (next == null)
                        break;
                    Object x = next.value;
                    if (x != null && x != next) {
                        if (tooLow(next.key, cmp))
                            next = null;
                        else {
                            @SuppressWarnings("unchecked") V vv = (V)x;
                            nextValue = vv;
                        }
                        break;
                    }
                }
            }

            public void remove() {
                Node<K,V> l = lastReturned;
                if (l == null)
                    throw new IllegalStateException();
                m.remove(l.key);
                lastReturned = null;
            }

            public Spliterator<T> trySplit() {
                return null;
            }

            public boolean tryAdvance(Consumer<? super T> action) {
                if (hasNext()) {
                    action.accept(next());
                    return true;
                }
                return false;
            }

            public void forEachRemaining(Consumer<? super T> action) {
                while (hasNext())
                    action.accept(next());
            }

            public long estimateSize() {
                return Long.MAX_VALUE;
            }

        }

        final class SubMapValueIterator extends SubMapIter<V> {
            public V next() {
                V v = nextValue;
                advance();
                return v;
            }
            public int characteristics() {
                return 0;
            }
        }

        final class SubMapKeyIterator extends SubMapIter<K> {
            public K next() {
                Node<K,V> n = next;
                advance();
                return n.key;
            }
            public int characteristics() {
                return Spliterator.DISTINCT | Spliterator.ORDERED |
                    Spliterator.SORTED;
            }
            public final Comparator<? super K> getComparator() {
                return SubMap.this.comparator();
            }
        }

        final class SubMapEntryIterator extends SubMapIter<Map.Entry<K,V>> {
            public Map.Entry<K,V> next() {
                Node<K,V> n = next;
                V v = nextValue;
                advance();
                return new AbstractMap.SimpleImmutableEntry<K,V>(n.key, v);
            }
            public int characteristics() {
                return Spliterator.DISTINCT;
            }
        }
    }

    // default Map method overrides

    public void forEach(BiConsumer<? super K, ? super V> action) {
        if (action == null) throw new NullPointerException();
        V v;
        for (Node<K,V> n = findFirst(); n != null; n = n.next) {
            if ((v = n.getValidValue()) != null)
                action.accept(n.key, v);
        }
    }

    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        if (function == null) throw new NullPointerException();
        V v;
        for (Node<K,V> n = findFirst(); n != null; n = n.next) {
            while ((v = n.getValidValue()) != null) {
                V r = function.apply(n.key, v);
                if (r == null) throw new NullPointerException();
                if (n.casValue(v, r))
                    break;
            }
        }
    }

    /**
     * Base class providing common structure for Spliterators.
     * (Although not all that much common functionality; as usual for
     * view classes, details annoyingly vary in key, value, and entry
     * subclasses in ways that are not worth abstracting out for
     * internal classes.)
     *
     * The basic split strategy is to recursively descend from top
     * level, row by row, descending to next row when either split
     * off, or the end of row is encountered. Control of the number of
     * splits relies on some statistical estimation: The expected
     * remaining number of elements of a skip list when advancing
     * either across or down decreases by about 25%. To make this
     * observation useful, we need to know initial size, which we
     * don't. But we can just use Integer.MAX_VALUE so that we
     * don't prematurely zero out while splitting.
     */
    abstract static class CSLMSpliterator<K,V> {
        final Comparator<? super K> comparator;
        final K fence;     // exclusive upper bound for keys, or null if to end
        Index<K,V> row;    // the level to split out
        Node<K,V> current; // current traversal node; initialize at origin
        int est;           // pseudo-size estimate
        CSLMSpliterator(Comparator<? super K> comparator, Index<K,V> row,
                        Node<K,V> origin, K fence, int est) {
            this.comparator = comparator; this.row = row;
            this.current = origin; this.fence = fence; this.est = est;
        }

        public final long estimateSize() { return (long)est; }
    }

    static final class KeySpliterator<K,V> extends CSLMSpliterator<K,V>
        implements Spliterator<K> {
        KeySpliterator(Comparator<? super K> comparator, Index<K,V> row,
                       Node<K,V> origin, K fence, int est) {
            super(comparator, row, origin, fence, est);
        }

        public Spliterator<K> trySplit() {
            Node<K,V> e; K ek;
            Comparator<? super K> cmp = comparator;
            K f = fence;
            if ((e = current) != null && (ek = e.key) != null) {
                for (Index<K,V> q = row; q != null; q = row = q.down) {
                    Index<K,V> s; Node<K,V> b, n; K sk;
                    if ((s = q.right) != null && (b = s.node) != null &&
                        (n = b.next) != null && n.value != null &&
                        (sk = n.key) != null && cpr(cmp, sk, ek) > 0 &&
                        (f == null || cpr(cmp, sk, f) < 0)) {
                        current = n;
                        Index<K,V> r = q.down;
                        row = (s.right != null) ? s : s.down;
                        est -= est >>> 2;
                        return new KeySpliterator<K,V>(cmp, r, e, sk, est);
                    }
                }
            }
            return null;
        }

        public void forEachRemaining(Consumer<? super K> action) {
            if (action == null) throw new NullPointerException();
            Comparator<? super K> cmp = comparator;
            K f = fence;
            Node<K,V> e = current;
            current = null;
            for (; e != null; e = e.next) {
                K k; Object v;
                if ((k = e.key) != null && f != null && cpr(cmp, f, k) <= 0)
                    break;
                if ((v = e.value) != null && v != e)
                    action.accept(k);
            }
        }

        public boolean tryAdvance(Consumer<? super K> action) {
            if (action == null) throw new NullPointerException();
            Comparator<? super K> cmp = comparator;
            K f = fence;
            Node<K,V> e = current;
            for (; e != null; e = e.next) {
                K k; Object v;
                if ((k = e.key) != null && f != null && cpr(cmp, f, k) <= 0) {
                    e = null;
                    break;
                }
                if ((v = e.value) != null && v != e) {
                    current = e.next;
                    action.accept(k);
                    return true;
                }
            }
            current = e;
            return false;
        }

        public int characteristics() {
            return Spliterator.DISTINCT | Spliterator.SORTED |
                Spliterator.ORDERED | Spliterator.CONCURRENT |
                Spliterator.NONNULL;
        }

        public final Comparator<? super K> getComparator() {
            return comparator;
        }
    }
    // factory method for KeySpliterator
    final KeySpliterator<K,V> keySpliterator() {
        Comparator<? super K> cmp = comparator;
        for (;;) { // ensure h corresponds to origin p
            HeadIndex<K,V> h; Node<K,V> p;
            Node<K,V> b = (h = head).node;
            if ((p = b.next) == null || p.value != null)
                return new KeySpliterator<K,V>(cmp, h, p, null, (p == null) ?
                                               0 : Integer.MAX_VALUE);
            p.helpDelete(b, p.next);
        }
    }

    static final class ValueSpliterator<K,V> extends CSLMSpliterator<K,V>
        implements Spliterator<V> {
        ValueSpliterator(Comparator<? super K> comparator, Index<K,V> row,
                       Node<K,V> origin, K fence, int est) {
            super(comparator, row, origin, fence, est);
        }

        public Spliterator<V> trySplit() {
            Node<K,V> e; K ek;
            Comparator<? super K> cmp = comparator;
            K f = fence;
            if ((e = current) != null && (ek = e.key) != null) {
                for (Index<K,V> q = row; q != null; q = row = q.down) {
                    Index<K,V> s; Node<K,V> b, n; K sk;
                    if ((s = q.right) != null && (b = s.node) != null &&
                        (n = b.next) != null && n.value != null &&
                        (sk = n.key) != null && cpr(cmp, sk, ek) > 0 &&
                        (f == null || cpr(cmp, sk, f) < 0)) {
                        current = n;
                        Index<K,V> r = q.down;
                        row = (s.right != null) ? s : s.down;
                        est -= est >>> 2;
                        return new ValueSpliterator<K,V>(cmp, r, e, sk, est);
                    }
                }
            }
            return null;
        }

        public void forEachRemaining(Consumer<? super V> action) {
            if (action == null) throw new NullPointerException();
            Comparator<? super K> cmp = comparator;
            K f = fence;
            Node<K,V> e = current;
            current = null;
            for (; e != null; e = e.next) {
                K k; Object v;
                if ((k = e.key) != null && f != null && cpr(cmp, f, k) <= 0)
                    break;
                if ((v = e.value) != null && v != e) {
                    @SuppressWarnings("unchecked") V vv = (V)v;
                    action.accept(vv);
                }
            }
        }

        public boolean tryAdvance(Consumer<? super V> action) {
            if (action == null) throw new NullPointerException();
            Comparator<? super K> cmp = comparator;
            K f = fence;
            Node<K,V> e = current;
            for (; e != null; e = e.next) {
                K k; Object v;
                if ((k = e.key) != null && f != null && cpr(cmp, f, k) <= 0) {
                    e = null;
                    break;
                }
                if ((v = e.value) != null && v != e) {
                    current = e.next;
                    @SuppressWarnings("unchecked") V vv = (V)v;
                    action.accept(vv);
                    return true;
                }
            }
            current = e;
            return false;
        }

        public int characteristics() {
            return Spliterator.CONCURRENT | Spliterator.ORDERED |
                Spliterator.NONNULL;
        }
    }

    // Almost the same as keySpliterator()
    final ValueSpliterator<K,V> valueSpliterator() {
        Comparator<? super K> cmp = comparator;
        for (;;) {
            HeadIndex<K,V> h; Node<K,V> p;
            Node<K,V> b = (h = head).node;
            if ((p = b.next) == null || p.value != null)
                return new ValueSpliterator<K,V>(cmp, h, p, null, (p == null) ?
                                                 0 : Integer.MAX_VALUE);
            p.helpDelete(b, p.next);
        }
    }

    static final class EntrySpliterator<K,V> extends CSLMSpliterator<K,V>
        implements Spliterator<Map.Entry<K,V>> {
        EntrySpliterator(Comparator<? super K> comparator, Index<K,V> row,
                         Node<K,V> origin, K fence, int est) {
            super(comparator, row, origin, fence, est);
        }

        public Spliterator<Map.Entry<K,V>> trySplit() {
            Node<K,V> e; K ek;
            Comparator<? super K> cmp = comparator;
            K f = fence;
            if ((e = current) != null && (ek = e.key) != null) {
                for (Index<K,V> q = row; q != null; q = row = q.down) {
                    Index<K,V> s; Node<K,V> b, n; K sk;
                    if ((s = q.right) != null && (b = s.node) != null &&
                        (n = b.next) != null && n.value != null &&
                        (sk = n.key) != null && cpr(cmp, sk, ek) > 0 &&
                        (f == null || cpr(cmp, sk, f) < 0)) {
                        current = n;
                        Index<K,V> r = q.down;
                        row = (s.right != null) ? s : s.down;
                        est -= est >>> 2;
                        return new EntrySpliterator<K,V>(cmp, r, e, sk, est);
                    }
                }
            }
            return null;
        }

        public void forEachRemaining(Consumer<? super Map.Entry<K,V>> action) {
            if (action == null) throw new NullPointerException();
            Comparator<? super K> cmp = comparator;
            K f = fence;
            Node<K,V> e = current;
            current = null;
            for (; e != null; e = e.next) {
                K k; Object v;
                if ((k = e.key) != null && f != null && cpr(cmp, f, k) <= 0)
                    break;
                if ((v = e.value) != null && v != e) {
                    @SuppressWarnings("unchecked") V vv = (V)v;
                    action.accept
                        (new AbstractMap.SimpleImmutableEntry<K,V>(k, vv));
                }
            }
        }

        public boolean tryAdvance(Consumer<? super Map.Entry<K,V>> action) {
            if (action == null) throw new NullPointerException();
            Comparator<? super K> cmp = comparator;
            K f = fence;
            Node<K,V> e = current;
            for (; e != null; e = e.next) {
                K k; Object v;
                if ((k = e.key) != null && f != null && cpr(cmp, f, k) <= 0) {
                    e = null;
                    break;
                }
                if ((v = e.value) != null && v != e) {
                    current = e.next;
                    @SuppressWarnings("unchecked") V vv = (V)v;
                    action.accept
                        (new AbstractMap.SimpleImmutableEntry<K,V>(k, vv));
                    return true;
                }
            }
            current = e;
            return false;
        }

        public int characteristics() {
            return Spliterator.DISTINCT | Spliterator.SORTED |
                Spliterator.ORDERED | Spliterator.CONCURRENT |
                Spliterator.NONNULL;
        }

        public final Comparator<Map.Entry<K,V>> getComparator() {
            // Adapt or create a key-based comparator
            if (comparator != null) {
                return Map.Entry.comparingByKey(comparator);
            }
            else {
                return (Comparator<Map.Entry<K,V>> & Serializable) (e1, e2) -> {
                    @SuppressWarnings("unchecked")
                    Comparable<? super K> k1 = (Comparable<? super K>) e1.getKey();
                    return k1.compareTo(e2.getKey());
                };
            }
        }
    }

    // Almost the same as keySpliterator()
    final EntrySpliterator<K,V> entrySpliterator() {
        Comparator<? super K> cmp = comparator;
        for (;;) { // almost same as key version
            HeadIndex<K,V> h; Node<K,V> p;
            Node<K,V> b = (h = head).node;
            if ((p = b.next) == null || p.value != null)
                return new EntrySpliterator<K,V>(cmp, h, p, null, (p == null) ?
                                                 0 : Integer.MAX_VALUE);
            p.helpDelete(b, p.next);
        }
    }

    // Unsafe mechanics
    private static final sun.misc.Unsafe UNSAFE;
    private static final long headOffset;// head
    private static final long SECONDARY;// Thread#threadLocalRandomSecondarySeed
    static {
        try {
            UNSAFE = sun.misc.Unsafe.getUnsafe();
            Class<?> k = ConcurrentSkipListMap.class;
            headOffset = UNSAFE.objectFieldOffset
                (k.getDeclaredField("head"));
            Class<?> tk = Thread.class;
            SECONDARY = UNSAFE.objectFieldOffset
                (tk.getDeclaredField("threadLocalRandomSecondarySeed"));

        } catch (Exception e) {
            throw new Error(e);
        }
    }
}
