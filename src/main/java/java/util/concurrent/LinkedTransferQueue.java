/*
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util.concurrent;

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;

/**
 * 20210530
 * A. 基于链接节点的无界 {@link TransferQueue}。 该队列根据任何给定的生产者对元素 FIFO（先进先出）进行排序。 队列的头部是某个生产者在队列中停留时间最长的元素。
 *    队列的尾部是某个生产者在队列中停留时间最短的那个元素。
 * B. 请注意，与大多数集合不同，{@code size} 方法不是恒定时间操作。 由于这些队列的异步性质，确定当前元素数量需要遍历元素，因此如果在遍历期间修改此集合，
 *    则可能会报告不准确的结果。 此外，批量操作 {@code addAll}、{@code removeAll}、{@code retainAll}、{@code containsAll}、{@code equals} 和
 *    {@code toArray} 不能保证以原子方式执行。 例如，与 {@code addAll} 操作同时运行的迭代器可能只查看一些添加的元素。
 * C. 此类及其迭代器实现了 {@link Collection} 和 {@link Iterator} 接口的所有可选方法。
 * D. 内存一致性影响：与其他并发集合一样，在将对象放入 {@code LinkedTransferQueue} 之前的线程中的操作 <a href="package-summary.html#MemoryVisibility">
 *    happen-before</a> 之后的操作 从另一个线程中的 {@code LinkedTransferQueue} 访问或删除该元素。
 * E. {@docRoot}/../technotes/guides/collections/index.html
 */
/**
 * A.
 * An unbounded {@link TransferQueue} based on linked nodes.
 * This queue orders elements FIFO (first-in-first-out) with respect
 * to any given producer.  The <em>head</em> of the queue is that
 * element that has been on the queue the longest time for some
 * producer.  The <em>tail</em> of the queue is that element that has
 * been on the queue the shortest time for some producer.
 *
 * B.
 * <p>Beware that, unlike in most collections, the {@code size} method
 * is <em>NOT</em> a constant-time operation. Because of the
 * asynchronous nature of these queues, determining the current number
 * of elements requires a traversal of the elements, and so may report
 * inaccurate results if this collection is modified during traversal.
 * Additionally, the bulk operations {@code addAll},
 * {@code removeAll}, {@code retainAll}, {@code containsAll},
 * {@code equals}, and {@code toArray} are <em>not</em> guaranteed
 * to be performed atomically. For example, an iterator operating
 * concurrently with an {@code addAll} operation might view only some
 * of the added elements.
 *
 * C.
 * <p>This class and its iterator implement all of the
 * <em>optional</em> methods of the {@link Collection} and {@link
 * Iterator} interfaces.
 *
 * D.
 * <p>Memory consistency effects: As with other concurrent
 * collections, actions in a thread prior to placing an object into a
 * {@code LinkedTransferQueue}
 * <a href="package-summary.html#MemoryVisibility"><i>happen-before</i></a>
 * actions subsequent to the access or removal of that element from
 * the {@code LinkedTransferQueue} in another thread.
 *
 * E.
 * <p>This class is a member of the
 * <a href="{@docRoot}/../technotes/guides/collections/index.html">
 * Java Collections Framework</a>.
 *
 * @since 1.7
 * @author Doug Lea
 * @param <E> the type of elements held in this collection
 */
public class LinkedTransferQueue<E> extends AbstractQueue<E> implements TransferQueue<E>, java.io.Serializable {
    private static final long serialVersionUID = -3223113410248163686L;

    /**
     * 20210530
     * 松弛的双队列概述:
     * A. 由 Scherer 和 Scott (http://www.cs.rice.edu/~wns1/papers/2004-DISC-DDS.pdf) 引入的双队列是（链接的）队列，其中节点可以表示数据或请求。
     *    当一个线程试图将一个数据节点入队，但遇到一个请求节点时，它反而“匹配”并删除它； 反之亦然用于排队请求。 阻塞双队列安排将不匹配请求排队的线程阻塞，
     *    直到其他线程提供匹配。 双同步队列（参见 Scherer、Lea 和 Scott http://www.cs.rochester.edu/u/scott/papers/2009_Scherer_CACM_SSQ.pdf）
     *    另外安排将不匹配数据排队的线程也阻塞。 根据呼叫者的指示，双传输队列支持所有这些模式。
     * B. 可以使用 Michael & Scott (M&S) 无锁队列算法 (http://www.cs.rochester.edu/u/scott/papers/1996_PODC_queues.pdf) 的变体来实现 FIFO 双队列。
     *    它维护两个指针字段，“head”，指向一个（匹配的）节点，该节点又指向第一个实际（不匹配的）队列节点（如果为空，则为 null）；
     *    和指向队列中最后一个节点的“tail”（如果为空，则再次为 null）。 例如，这是一个可能的队列，包含四个数据元素：
     *      head                tail
     *      |                   |
     *      v                   v
     *      M -> U -> U -> U -> U
     * C. 众所周知，M&S 队列算法在维护（通过 CAS）这些头指针和尾指针时容易受到可扩展性和开销限制。 这导致了竞争减少变体的发展，
     *    例如消除数组（参见 Moir 等人 http://portal.acm.org/citation.cfm?id=1074013）和乐观反向指针
     *    （参见 Ladan-Mozes & Shavit http ://people.csail.mit.edu/edya/publications/OptimisticFIFOQueue-journal.pdf）。
     *    然而，当需要双重性时，双重队列的性质为改进 M&S 风格的实现提供了一种更简单的策略。
     * D. 在双队列中，每个节点必须原子地保持其匹配状态。 虽然还有其他可能的变体，但我们在这里将其实现为：对于数据模式节点，匹配需要在匹配时将“项目”字段
     *    从非空数据值转换为空，反之亦然，对于请求节点，CASing from 空为数据值。 （请注意，这种队列的线性化特性很容易验证——元素通过链接可用，而通过匹配不可用。）
     *    与普通 M&S 队列相比，双队列的这种特性需要每个 enq/ 一个额外的成功原子操作 deq 对。 但它也支持队列维护机制的低成本变体。
     *    （这个想法的一个变体甚至适用于支持删除内部元素的非双队列，例如 j.u.c.ConcurrentLinkedQueue。）
     * E. 一旦一个节点被匹配，它的匹配状态就再也不会改变了。 因此，我们可以安排它们的链表包含零个或多个匹配节点的前缀，后跟零个或多个不匹配节点的后缀。
     *   （请注意，我们允许前缀和后缀都为零长度，这反过来意味着我们不使用虚拟头。）如果我们不关心时间或空间效率，我们可以通过以下方式正确执行入队和出队操作:
     *    从一个指针到初始节点遍历； 对匹配中的第一个不匹配节点的项进行CAS处理，对追加中的尾随节点的下一个字段进行CAS处理。 （在最初为空时加上一些特殊外壳）。
     *    虽然这本身就是一个糟糕的想法，但它的好处是不需要对头/尾字段进行任何原子更新。
     * F. 我们在此介绍一种介于从不更新和始终更新队列（头和尾）指针之间的方法。 这提供了有时需要额外的遍历步骤来定位第一个和/或最后一个不匹配的节点与减少的开销
     *    和较少更新队列指针的争用之间的权衡。 例如，队列的可能快照是：
     *      head           tail
     *      |              |
     *      v              v
     *      M -> M -> U -> U -> U -> U
     * G. 此“松弛”的最佳值（“头”的值与第一个未匹配节点之间的目标最大距离，“尾”的值类似）是一个经验问题。 我们发现使用 1-3 范围内的非常小的常数在一系列平台上效果最好。
     *    较大的值会增加缓存未命中的成本和长遍历链的风险，而较小的值会增加 CAS 争用和开销。
     * H. 松弛的双队列不同于普通的M＆S双队列，因为在匹配，附加甚至遍历节点时有时仅更新头或尾指针。 以保持有针对性的松弛。 “有时”的概念可以通过多种方式实施。
     *    最简单的方法是使用在每个遍历步骤中递增的每个操作计数器，并在计数超过阈值时尝试（通过 CAS）更新关联的队列指针。
     *    另一个需要更多开销的方法是使用随机数生成器以每个遍历步骤的给定概率进行更新。
     * I. 在沿着这些路线的任何策略中，由于 CAS 更新字段可能会失败，因此实际松弛可能会超过目标松弛。 但是，可以随时重试它们以保持目标。 即使使用非常小的 slack 值，
     *    这种方法也适用于双队列，因为它允许所有操作直到匹配或附加项目（因此可能允许另一个线程的进度）都是只读的，因此不会进一步引入 争执。
     *    如下所述，我们通过仅在这些点之后执行松弛维护重试来实现这一点。
     * J. 作为此类技术的陪伴，可以在不增加头指针更新争用的情况下进一步减少遍历开销：线程有时可能会将当前“头”节点的“下一个”链接路径捷径化，使其更接近当前已知的
     *    第一个不匹配节点，并且尾巴类似。 同样，这可以通过使用阈值或随机化来触发。
     * K. 这些想法必须进一步扩展，以避免由从旧的被遗忘的头节点开始的节点的顺序“下一个”链接引起的无限量的回收成本高昂的垃圾：正如
     *    Boehm (http://portal.acm. org/citation.cfm?doid=503272.503282) 如果 GC 延迟注意到任何任意旧节点已成为垃圾，则所有较新的死节点也将无法回收。
     *    （在非 GC 环境中也会出现类似的问题。）为了在我们的实现中解决这个问题，在 CASing 推进头指针时，我们将前一个头的“下一个”链接设置为仅指向它自己；
     *    从而限制了连接死列表的长度。（我们也采取了类似的措施来清除其他 Node 字段中可能保留的垃圾值。）但是，这样做会增加遍历的复杂性：如果有任何“next”指针链接到自身，
     *    则表明当前线程已经落后头部更新，因此遍历必须从“头部”继续进行。试图从“tail”开始寻找当前尾部的遍历也可能遇到自链接，在这种情况下，它们也会在“head”处继续。
     * L. 在基于 slack 的方案中，甚至不使用 CAS 进行更新是很诱人的（类似于 Ladan-Mozes 和 Shavit）。 但是，在上述链接忘记机制下，这不能用于头更新，
     *    因为更新可能会将头留在分离的节点上。 虽然尾更新可以直接写入，但它们会增加长回溯的风险，从而增加长垃圾链，这可能比考虑到执行 CAS 与写入的成本差异较小时
     *    值得考虑的成本高得多。 在每个操作上触发（特别是考虑到写入和 CAS 同样需要额外的 GC 簿记（“写入障碍”），有时由于争用而比写入本身成本更高）。
     * 实施概述:
     * A. 我们使用基于阈值的方法进行更新，松弛阈值为 2——也就是说，当当前指针看起来离第一个/最后一个节点有两步或更多步时，我们更新 head/tail。
     *    松弛值是硬连接的：大于 1 的路径自然通过检查遍历指针的相等性来实现，除非列表只有一个元素，在这种情况下，我们将松弛阈值保持为 1。
     *    避免跨方法调用跟踪显式计数会稍微简化已经很混乱的实现。 如果有一个低质量、便宜的每线程可用，使用随机化可能会更好，
     *    但即使是 ThreadLocalRandom 对于这些目的来说也太重了。
     * B. 使用如此小的松弛阈值，除了取消/移除的情况（见下文）外，不值得用路径短路（即取消拼接内部节点）来增加它。
     * C. 在任何节点入队之前，我们允许头字段和尾字段都为空； 在第一次追加时初始化。 这简化了一些其他逻辑，并提供了更有效的显式控制路径，
     *    而不是让 JVM 在它们为 null 时插入隐式 NullPointerException。 虽然目前尚未完全实施，但我们也保留了在空时将这些字段重新归零的可能性
     *    （安排起来很复杂，几乎没有什么好处。）
     * D. 所有入队/出队操作都由单一方法“xfer”处理，其参数指示是否充当某种形式的提供、放置、轮询、获取或传输（每个都可能带有超时）。
     *    使用单一方法的相对复杂性超过了为每种情况使用单独方法的代码量和维护问题。
     * E. 操作最多包括三个阶段。 第一个在 xfer 方法中实现，第二个在 tryAppend 中实现，第三个在 awaitMatch 方法中实现:
     *      1. 尝试匹配现有节点:
     *          从头开始，跳过已经匹配的节点，直到找到一个相反模式的不匹配节点，如果存在，在这种情况下匹配它并返回，如果有必要，也将头更新到匹配节点的后面
     *          （或节点本身，如果列表有 没有其他不匹配的节点）。 如果 CAS 未命中，则循环重试向前推进两步，直到成功或 slack 最多为 2。 通过要求每次尝试前进两个
     *          （如果适用），我们确保松弛不会无限制地增长。 遍历还检查初始头部现在是否不在列表中，在这种情况下，它们从新头部开始。
     *          如果没有找到候选并且调用是不定时的轮询/提供，（参数“现在”是现在）返回。
     *      2. 尝试追加一个新节点（方法 tryAppend）:
     *          从当前尾指针开始，找到实际的最后一个节点并尝试追加一个新节点（或者如果 head 为空，则建立第一个节点）。 仅当其前驱节点已经匹配或处于相同模式时，
     *          才可以追加节点。 如果我们检测到其他情况，则必须在遍历过程中附加了一个具有相反模式的新节点，因此我们必须在阶段 1 重新开始。
     *          遍历和更新步骤在其他方面与阶段 1 类似：重试 CAS 未命中并检查是否过时。 特别是，如果遇到自链接，那么我们可以通过在当前头继续遍历来安全地跳转到列表上的节点。
     *          成功追加时，如果调用是 ASYNC，则返回。
     *      3. 等待匹配或取消（方法 awaitMatch）:
     *          等待另一个线程匹配节点；如果当前线程被中断或等待超时，则取消。在多处理器上，我们使用队列前旋转：如果一个节点似乎是队列中第一个不匹配的节点，
     *          它会在阻塞之前旋转一点。在任何一种情况下，在阻塞之前，它都会尝试断开当前“头”和第一个不匹配节点之间的任何节点。队列前旋转极大地提高了竞争激烈的队列的性能。
     *          只要它相对简短和“安静”，旋转不会对争用较少的队列的性能产生太大影响。在旋转期间，线程检查其中断状态并生成线程局部随机数，以决定偶尔执行Thread.yield。
     *          虽然 yield 的规格不明确，但我们认为它可能有助于限制旋转对繁忙系统的影响，但不会造成伤害。
     *          我们还使用较小 (1/2) 的自旋用于不知道在前但其前驱尚未阻塞的节点——这些“链式”自旋避免了队列前规则的伪影，否则会导致交替的节点自旋与阻塞.
     *          此外，与它们的前辈相比，代表相变（从数据到请求节点，反之亦然）的前端线程接收额外的链式自旋，反映了在相变期间通常需要更长的路径来解除阻塞线程。
     * 取消链接移除的内部节点:
     * A. 除了通过上述自链接最小化垃圾保留之外，我们还取消链接已删除的内部节点。这些可能是由于超时或中断的等待，或调用 remove(x) 或 Iterator.remove 引起的。
     *    通常，给定一个节点，该节点曾经被认为是某个要删除的节点 s 的前驱，如果它仍然指向 s，我们可以通过 CASing 其前驱的下一个字段来取消拼接 s
     *    （否则 s 必须已经被删除了）已删除或现在不在列表中）。但是有两种情况我们不能保证这样使节点s不可达：（1）如果s是list的尾节点（即next为null），
     *    那么它就被固定为appends的目标节点，所以只能在附加其他节点后才能删除。 (2) 给定匹配的前驱节点（包括被取消的情况），我们不一定取消链接 s：前驱可能已经解开，
     *    在这种情况下，某些先前可达的节点可能仍然指向 s。 （有关进一步的解释，请参阅 Herlihy & Shavit “多处理器编程的艺术”第 9 章）。
     *    尽管在这两种情况下，如果 s 或其前任位于（或可以成为）列表的头部或脱离列表的头部，我们都可以排除采取进一步行动的必要性。
     * B. 如果不考虑这些，则有可能无限数量的假定已删除的节点仍然可访问。 导致这种堆积的情况并不常见，但在实践中可能会发生； 例如，当一系列短时间的轮询调用反复超时，
     *    但由于队列前面的不定时调用而从未从列表中退出时。
     * C. 当这些情况出现时，我们不是总是遍历整个列表以找到要取消链接的实际前任（无论如何这对情况 (1) 没有帮助），而是记录对可能的取消拼接失败的保守估计
     *   （在“sweepVotes”中）。 当估计值超过阈值（“SWEEP_THRESHOLD”）时，我们触发全面扫描，该阈值指示在扫描之前可以容忍的估计删除失败的最大数量，
     *    取消链接在初始删除时未取消链接的已取消节点。 我们通过线程达到阈值（而不是后台线程或通过将工作分散到其他线程）来执行扫描，因为在发生删除的主要上下文中，
     *    调用者已经超时、取消或执行潜在的 O(n) 操作 （例如，remove(x)），它们都不是时间紧迫的，足以保证替代方案会强加给其他线程的开销。
     * D. 因为sweepVotes 估计是保守的，并且因为节点在脱离队列头部时“自然地”断开链接，并且因为即使在sweep 进行时我们也允许累积投票，所以这些节点通常比估计的少得多。
     *    阈值的选择平衡了浪费精力和争用的可能性，而不是提供在静态队列中保留内部节点的最坏情况限制。 下面定义的值是根据经验选择的，以在各种超时情况下平衡这些值。
     * E. 请注意，在扫描期间我们无法自链接未链接的内部节点。 然而，当某个后继者最终脱离列表的头部并自链接时，相关的垃圾链就会终止。
     */
    /*
     * *** Overview of Dual Queues with Slack ***
     *
     * A.
     * Dual Queues, introduced by Scherer and Scott
     * (http://www.cs.rice.edu/~wns1/papers/2004-DISC-DDS.pdf) are
     * (linked) queues in which nodes may represent either data or
     * requests.  When a thread tries to enqueue a data node, but
     * encounters a request node, it instead "matches" and removes it;
     * and vice versa for enqueuing requests. Blocking Dual Queues
     * arrange that threads enqueuing unmatched requests block until
     * other threads provide the match. Dual Synchronous Queues (see
     * Scherer, Lea, & Scott
     * http://www.cs.rochester.edu/u/scott/papers/2009_Scherer_CACM_SSQ.pdf)
     * additionally arrange that threads enqueuing unmatched data also
     * block.  Dual Transfer Queues support all of these modes, as
     * dictated by callers.
     *
     * B.
     * A FIFO dual queue may be implemented using a variation of the
     * Michael & Scott (M&S) lock-free queue algorithm
     * (http://www.cs.rochester.edu/u/scott/papers/1996_PODC_queues.pdf).
     * It maintains two pointer fields, "head", pointing to a
     * (matched) node that in turn points to the first actual
     * (unmatched) queue node (or null if empty); and "tail" that
     * points to the last node on the queue (or again null if
     * empty). For example, here is a possible queue with four data
     * elements:
     *
     *  head                tail
     *    |                   |
     *    v                   v
     *    M -> U -> U -> U -> U
     *
     * C.
     * The M&S queue algorithm is known to be prone to scalability and
     * overhead limitations when maintaining (via CAS) these head and
     * tail pointers. This has led to the development of
     * contention-reducing variants such as elimination arrays (see
     * Moir et al http://portal.acm.org/citation.cfm?id=1074013) and
     * optimistic back pointers (see Ladan-Mozes & Shavit
     * http://people.csail.mit.edu/edya/publications/OptimisticFIFOQueue-journal.pdf).
     * However, the nature of dual queues enables a simpler tactic for
     * improving M&S-style implementations when dual-ness is needed.
     *
     * D.
     * In a dual queue, each node must atomically maintain its match
     * status. While there are other possible variants, we implement
     * this here as: for a data-mode node, matching entails CASing an
     * "item" field from a non-null data value to null upon match, and
     * vice-versa for request nodes, CASing from null to a data
     * value. (Note that the linearization properties of this style of
     * queue are easy to verify -- elements are made available by
     * linking, and unavailable by matching.) Compared to plain M&S
     * queues, this property of dual queues requires one additional
     * successful atomic operation per enq/deq pair. But it also
     * enables lower cost variants of queue maintenance mechanics. (A
     * variation of this idea applies even for non-dual queues that
     * support deletion of interior elements, such as
     * j.u.c.ConcurrentLinkedQueue.)
     *
     * E.
     * Once a node is matched, its match status can never again
     * change.  We may thus arrange that the linked list of them
     * contain a prefix of zero or more matched nodes, followed by a
     * suffix of zero or more unmatched nodes. (Note that we allow
     * both the prefix and suffix to be zero length, which in turn
     * means that we do not use a dummy header.)  If we were not
     * concerned with either time or space efficiency, we could
     * correctly perform enqueue and dequeue operations by traversing
     * from a pointer to the initial node; CASing the item of the
     * first unmatched node on match and CASing the next field of the
     * trailing node on appends. (Plus some special-casing when
     * initially empty).  While this would be a terrible idea in
     * itself, it does have the benefit of not requiring ANY atomic
     * updates on head/tail fields.
     *
     * F.
     * We introduce here an approach that lies between the extremes of
     * never versus always updating queue (head and tail) pointers.
     * This offers a tradeoff between sometimes requiring extra
     * traversal steps to locate the first and/or last unmatched
     * nodes, versus the reduced overhead and contention of fewer
     * updates to queue pointers. For example, a possible snapshot of
     * a queue is:
     *
     *  head           tail
     *    |              |
     *    v              v
     *    M -> M -> U -> U -> U -> U
     *
     * G.
     * The best value for this "slack" (the targeted maximum distance
     * between the value of "head" and the first unmatched node, and
     * similarly for "tail") is an empirical matter. We have found
     * that using very small constants in the range of 1-3 work best
     * over a range of platforms. Larger values introduce increasing
     * costs of cache misses and risks of long traversal chains, while
     * smaller values increase CAS contention and overhead.
     *
     * H.
     * Dual queues with slack differ from plain M&S dual queues by
     * virtue of only sometimes updating head or tail pointers when
     * matching, appending, or even traversing nodes; in order to
     * maintain a targeted slack.  The idea of "sometimes" may be
     * operationalized in several ways. The simplest is to use a
     * per-operation counter incremented on each traversal step, and
     * to try (via CAS) to update the associated queue pointer
     * whenever the count exceeds a threshold. Another, that requires
     * more overhead, is to use random number generators to update
     * with a given probability per traversal step.
     *
     * I.
     * In any strategy along these lines, because CASes updating
     * fields may fail, the actual slack may exceed targeted
     * slack. However, they may be retried at any time to maintain
     * targets.  Even when using very small slack values, this
     * approach works well for dual queues because it allows all
     * operations up to the point of matching or appending an item
     * (hence potentially allowing progress by another thread) to be
     * read-only, thus not introducing any further contention. As
     * described below, we implement this by performing slack
     * maintenance retries only after these points.
     *
     * J.
     * As an accompaniment to such techniques, traversal overhead can
     * be further reduced without increasing contention of head
     * pointer updates: Threads may sometimes shortcut the "next" link
     * path from the current "head" node to be closer to the currently
     * known first unmatched node, and similarly for tail. Again, this
     * may be triggered with using thresholds or randomization.
     *
     * K.
     * These ideas must be further extended to avoid unbounded amounts
     * of costly-to-reclaim garbage caused by the sequential "next"
     * links of nodes starting at old forgotten head nodes: As first
     * described in detail by Boehm
     * (http://portal.acm.org/citation.cfm?doid=503272.503282) if a GC
     * delays noticing that any arbitrarily old node has become
     * garbage, all newer dead nodes will also be unreclaimed.
     * (Similar issues arise in non-GC environments.)  To cope with
     * this in our implementation, upon CASing to advance the head
     * pointer, we set the "next" link of the previous head to point
     * only to itself; thus limiting the length of connected dead lists.
     * (We also take similar care to wipe out possibly garbage
     * retaining values held in other Node fields.)  However, doing so
     * adds some further complexity to traversal: If any "next"
     * pointer links to itself, it indicates that the current thread
     * has lagged behind a head-update, and so the traversal must
     * continue from the "head".  Traversals trying to find the
     * current tail starting from "tail" may also encounter
     * self-links, in which case they also continue at "head".
     *
     * L.
     * It is tempting in slack-based scheme to not even use CAS for
     * updates (similarly to Ladan-Mozes & Shavit). However, this
     * cannot be done for head updates under the above link-forgetting
     * mechanics because an update may leave head at a detached node.
     * And while direct writes are possible for tail updates, they
     * increase the risk of long retraversals, and hence long garbage
     * chains, which can be much more costly than is worthwhile
     * considering that the cost difference of performing a CAS vs
     * write is smaller when they are not triggered on each operation
     * (especially considering that writes and CASes equally require
     * additional GC bookkeeping ("write barriers") that are sometimes
     * more costly than the writes themselves because of contention).
     *
     *
     * *** Overview of implementation ***
     *
     * A.
     * We use a threshold-based approach to updates, with a slack
     * threshold of two -- that is, we update head/tail when the
     * current pointer appears to be two or more steps away from the
     * first/last node. The slack value is hard-wired: a path greater
     * than one is naturally implemented by checking equality of
     * traversal pointers except when the list has only one element,
     * in which case we keep slack threshold at one. Avoiding tracking
     * explicit counts across method calls slightly simplifies an
     * already-messy implementation. Using randomization would
     * probably work better if there were a low-quality dirt-cheap
     * per-thread one available, but even ThreadLocalRandom is too
     * heavy for these purposes.
     *
     * B.
     * With such a small slack threshold value, it is not worthwhile
     * to augment this with path short-circuiting (i.e., unsplicing
     * interior nodes) except in the case of cancellation/removal (see
     * below).
     *
     * C.
     * We allow both the head and tail fields to be null before any
     * nodes are enqueued; initializing upon first append.  This
     * simplifies some other logic, as well as providing more
     * efficient explicit control paths instead of letting JVMs insert
     * implicit NullPointerExceptions when they are null.  While not
     * currently fully implemented, we also leave open the possibility
     * of re-nulling these fields when empty (which is complicated to
     * arrange, for little benefit.)
     *
     * D.
     * All enqueue/dequeue operations are handled by the single method
     * "xfer" with parameters indicating whether to act as some form
     * of offer, put, poll, take, or transfer (each possibly with
     * timeout). The relative complexity of using one monolithic
     * method outweighs the code bulk and maintenance problems of
     * using separate methods for each case.
     *
     * E.
     * Operation consists of up to three phases. The first is
     * implemented within method xfer, the second in tryAppend, and
     * the third in method awaitMatch.
     *
     * 1. Try to match an existing node
     *
     *    Starting at head, skip already-matched nodes until finding
     *    an unmatched node of opposite mode, if one exists, in which
     *    case matching it and returning, also if necessary updating
     *    head to one past the matched node (or the node itself if the
     *    list has no other unmatched nodes). If the CAS misses, then
     *    a loop retries advancing head by two steps until either
     *    success or the slack is at most two. By requiring that each
     *    attempt advances head by two (if applicable), we ensure that
     *    the slack does not grow without bound. Traversals also check
     *    if the initial head is now off-list, in which case they
     *    start at the new head.
     *
     *    If no candidates are found and the call was untimed
     *    poll/offer, (argument "how" is NOW) return.
     *
     * 2. Try to append a new node (method tryAppend)
     *
     *    Starting at current tail pointer, find the actual last node
     *    and try to append a new node (or if head was null, establish
     *    the first node). Nodes can be appended only if their
     *    predecessors are either already matched or are of the same
     *    mode. If we detect otherwise, then a new node with opposite
     *    mode must have been appended during traversal, so we must
     *    restart at phase 1. The traversal and update steps are
     *    otherwise similar to phase 1: Retrying upon CAS misses and
     *    checking for staleness.  In particular, if a self-link is
     *    encountered, then we can safely jump to a node on the list
     *    by continuing the traversal at current head.
     *
     *    On successful append, if the call was ASYNC, return.
     *
     * 3. Await match or cancellation (method awaitMatch)
     *
     *    Wait for another thread to match node; instead cancelling if
     *    the current thread was interrupted or the wait timed out. On
     *    multiprocessors, we use front-of-queue spinning: If a node
     *    appears to be the first unmatched node in the queue, it
     *    spins a bit before blocking. In either case, before blocking
     *    it tries to unsplice any nodes between the current "head"
     *    and the first unmatched node.
     *
     *    Front-of-queue spinning vastly improves performance of
     *    heavily contended queues. And so long as it is relatively
     *    brief and "quiet", spinning does not much impact performance
     *    of less-contended queues.  During spins threads check their
     *    interrupt status and generate a thread-local random number
     *    to decide to occasionally perform a Thread.yield. While
     *    yield has underdefined specs, we assume that it might help,
     *    and will not hurt, in limiting impact of spinning on busy
     *    systems.  We also use smaller (1/2) spins for nodes that are
     *    not known to be front but whose predecessors have not
     *    blocked -- these "chained" spins avoid artifacts of
     *    front-of-queue rules which otherwise lead to alternating
     *    nodes spinning vs blocking. Further, front threads that
     *    represent phase changes (from data to request node or vice
     *    versa) compared to their predecessors receive additional
     *    chained spins, reflecting longer paths typically required to
     *    unblock threads during phase changes.
     *
     *
     * ** Unlinking removed interior nodes **
     *
     * A.
     * In addition to minimizing garbage retention via self-linking
     * described above, we also unlink removed interior nodes. These
     * may arise due to timed out or interrupted waits, or calls to
     * remove(x) or Iterator.remove.  Normally, given a node that was
     * at one time known to be the predecessor of some node s that is
     * to be removed, we can unsplice s by CASing the next field of
     * its predecessor if it still points to s (otherwise s must
     * already have been removed or is now offlist). But there are two
     * situations in which we cannot guarantee to make node s
     * unreachable in this way: (1) If s is the trailing node of list
     * (i.e., with null next), then it is pinned as the target node
     * for appends, so can only be removed later after other nodes are
     * appended. (2) We cannot necessarily unlink s given a
     * predecessor node that is matched (including the case of being
     * cancelled): the predecessor may already be unspliced, in which
     * case some previous reachable node may still point to s.
     * (For further explanation see Herlihy & Shavit "The Art of
     * Multiprocessor Programming" chapter 9).  Although, in both
     * cases, we can rule out the need for further action if either s
     * or its predecessor are (or can be made to be) at, or fall off
     * from, the head of list.
     *
     * B.
     * Without taking these into account, it would be possible for an
     * unbounded number of supposedly removed nodes to remain
     * reachable.  Situations leading to such buildup are uncommon but
     * can occur in practice; for example when a series of short timed
     * calls to poll repeatedly time out but never otherwise fall off
     * the list because of an untimed call to take at the front of the
     * queue.
     *
     * C.
     * When these cases arise, rather than always retraversing the
     * entire list to find an actual predecessor to unlink (which
     * won't help for case (1) anyway), we record a conservative
     * estimate of possible unsplice failures (in "sweepVotes").
     * We trigger a full sweep when the estimate exceeds a threshold
     * ("SWEEP_THRESHOLD") indicating the maximum number of estimated
     * removal failures to tolerate before sweeping through, unlinking
     * cancelled nodes that were not unlinked upon initial removal.
     * We perform sweeps by the thread hitting threshold (rather than
     * background threads or by spreading work to other threads)
     * because in the main contexts in which removal occurs, the
     * caller is already timed-out, cancelled, or performing a
     * potentially O(n) operation (e.g. remove(x)), none of which are
     * time-critical enough to warrant the overhead that alternatives
     * would impose on other threads.
     *
     * D.
     * Because the sweepVotes estimate is conservative, and because
     * nodes become unlinked "naturally" as they fall off the head of
     * the queue, and because we allow votes to accumulate even while
     * sweeps are in progress, there are typically significantly fewer
     * such nodes than estimated.  Choice of a threshold value
     * balances the likelihood of wasted effort and contention, versus
     * providing a worst-case bound on retention of interior nodes in
     * quiescent queues. The value defined below was chosen
     * empirically to balance these under various timeout scenarios.
     *
     * E.
     * Note that we cannot self-link unlinked interior nodes during
     * sweeps. However, the associated garbage chains terminate when
     * some successor ultimately falls off the head of the list and is
     * self-linked.
     */

    /** 如果在多处理器上为真 */
    /** True if on multiprocessor */
    private static final boolean MP = Runtime.getRuntime().availableProcessors() > 1;

    /**
     * 20210530
     * 当某个节点显然是队列中的第一个服务员时，在阻塞之前在多处理器上旋转的次数（随机散布着对Thread.yield的调用）。 见上文解释。 必须是二的幂。
     * 该值是凭经验推导出来的——它在各种处理器、CPU 数量和操作系统上都能很好地工作。
     */
    /**
     * The number of times to spin (with randomly interspersed calls
     * to Thread.yield) on multiprocessor before blocking when a node
     * is apparently the first waiter in the queue.  See above for
     * explanation. Must be a power of two. The value is empirically
     * derived -- it works pretty well across a variety of processors,
     * numbers of CPUs, and OSes.
     */
    private static final int FRONT_SPINS   = 1 << 7;

    /**
     * 20210530
     * 当一个节点前面是另一个明显正在旋转的节点时，在阻塞之前旋转的次数。 还用作相位变化时 FRONT_SPINS 的增量，以及旋转期间屈服的基本平均频率。 必须是二的幂。
     */
    /**
     * The number of times to spin before blocking when a node is
     * preceded by another node that is apparently spinning.  Also
     * serves as an increment to FRONT_SPINS on phase changes, and as
     * base average frequency for yielding during spins. Must be a
     * power of two.
     */
    private static final int CHAINED_SPINS = FRONT_SPINS >>> 1;

    /**
     * 20210530
     * 在清除队列以取消链接在初始删除时未取消链接的已取消节点之前，可容忍的最大估计删除失败次数 (sweepVotes)。 见上文解释。 该值必须至少为2，
     * 以避免在删除尾随节点时进行无用的扫描。
     */
    /**
     * The maximum number of estimated removal failures (sweepVotes)
     * to tolerate before sweeping through the queue unlinking
     * cancelled nodes that were not unlinked upon initial
     * removal. See above for explanation. The value must be at least
     * two to avoid useless sweeps when removing trailing nodes.
     */
    static final int SWEEP_THRESHOLD = 32;

    /**
     * 20210530
     * 队列节点。 对物品使用 Object 而不是 E，以便在使用后忘记它们。 严重依赖于不安全机制来最大程度地减少不必要的排序约束：
     * 在其他访问或CAS的本质上排序的写入使用简单的宽松形式。
     */
    /**
     * Queue nodes. Uses Object, not E, for items to allow forgetting
     * them after use.  Relies heavily on Unsafe mechanics to minimize
     * unnecessary ordering constraints: Writes that are intrinsically
     * ordered wrt other accesses or CASes use simple relaxed forms.
     */
    static final class Node {
        final boolean isData;   // false if this is a request node  // 如果这是请求节点，则为 false
        volatile Object item;   // initially non-null if isData; CASed to match // 如果isData，则最初为非null；否则为false。 CASed 匹配
        volatile Node next;
        volatile Thread waiter; // null until waiting   // 空直到等待

        // CAS methods for fields
        final boolean casNext(Node cmp, Node val) {
            return UNSAFE.compareAndSwapObject(this, nextOffset, cmp, val);
        }

        final boolean casItem(Object cmp, Object val) {
            // assert cmp == null || cmp.getClass() != Node.class;
            return UNSAFE.compareAndSwapObject(this, itemOffset, cmp, val);
        }

        /**
         * Constructs a new node.  Uses relaxed write because item can
         * only be seen after publication via casNext.
         */
        Node(Object item, boolean isData) {
            UNSAFE.putObject(this, itemOffset, item); // relaxed write
            this.isData = isData;
        }

        /**
         * Links node to itself to avoid garbage retention.  Called
         * only after CASing head field, so uses relaxed write.
         */
        final void forgetNext() {
            UNSAFE.putObject(this, nextOffset, this);
        }

        /**
         * Sets item to self and waiter to null, to avoid garbage
         * retention after matching or cancelling. Uses relaxed writes
         * because order is already constrained in the only calling
         * contexts: item is forgotten only after volatile/atomic
         * mechanics that extract items.  Similarly, clearing waiter
         * follows either CAS or return from park (if ever parked;
         * else we don't care).
         */
        final void forgetContents() {
            UNSAFE.putObject(this, itemOffset, this);
            UNSAFE.putObject(this, waiterOffset, null);
        }

        /**
         * 20210530
         * 如果此节点已匹配，则返回 true，包括由于取消而人工匹配的情况。
         */
        /**
         * Returns true if this node has been matched, including the
         * case of artificial matches due to cancellation.
         */
        final boolean isMatched() {
            Object x = item;
            return (x == this) || ((x == null) == isData);
        }

        /**
         * Returns true if this is an unmatched request node.
         */
        final boolean isUnmatchedRequest() {
            return !isData && item == null;
        }

        /**
         * 20210530
         * 如果具有给定模式的节点无法附加到此节点，则返回 true，因为此节点不匹配且具有相反的数据模式。
         */
        /**
         * Returns true if a node with the given mode cannot be
         * appended to this node because this node is unmatched and
         * has opposite data mode.
         */
        final boolean cannotPrecede(boolean haveData) {
            boolean d = isData;
            Object x;
            return d != haveData && (x = item) != this && (x != null) == d;
        }

        /**
         * Tries to artificially match a data node -- used by remove.
         */
        final boolean tryMatchData() {
            // assert isData;
            Object x = item;
            if (x != null && x != this && casItem(x, null)) {
                LockSupport.unpark(waiter);
                return true;
            }
            return false;
        }

        private static final long serialVersionUID = -3375979862319811754L;

        // Unsafe mechanics
        private static final sun.misc.Unsafe UNSAFE;
        private static final long itemOffset;
        private static final long nextOffset;
        private static final long waiterOffset;
        static {
            try {
                UNSAFE = sun.misc.Unsafe.getUnsafe();
                Class<?> k = Node.class;
                itemOffset = UNSAFE.objectFieldOffset
                    (k.getDeclaredField("item"));
                nextOffset = UNSAFE.objectFieldOffset
                    (k.getDeclaredField("next"));
                waiterOffset = UNSAFE.objectFieldOffset
                    (k.getDeclaredField("waiter"));
            } catch (Exception e) {
                throw new Error(e);
            }
        }
    }

    /** 队列的首部； 空直到第一次入队 */
    /** head of the queue; null until first enqueue */
    transient volatile Node head;

    /** 队列尾部； 空直到第一次追加 */
    /** tail of the queue; null until first append */
    private transient volatile Node tail;

    /** 取消拼接移除节点的明显失败次数 */
    /** The number of apparent failures to unsplice removed nodes */
    private transient volatile int sweepVotes;

    // CAS methods for fields
    private boolean casTail(Node cmp, Node val) {
        return UNSAFE.compareAndSwapObject(this, tailOffset, cmp, val);
    }

    private boolean casHead(Node cmp, Node val) {
        return UNSAFE.compareAndSwapObject(this, headOffset, cmp, val);
    }

    private boolean casSweepVotes(int cmp, int val) {
        return UNSAFE.compareAndSwapInt(this, sweepVotesOffset, cmp, val);
    }

    /**
     * 20210530
     * xfer 方法中“how”参数的可能值。
     */
    /*
     * Possible values for "how" argument in xfer method.
     */
    private static final int NOW   = 0; // for untimed poll, tryTransfer
    private static final int ASYNC = 1; // for offer, put, add
    private static final int SYNC  = 2; // for transfer, take
    private static final int TIMED = 3; // for timed poll, tryTransfer

    @SuppressWarnings("unchecked")
    static <E> E cast(Object item) {
        // assert item == null || item.getClass() != Node.class;
        return (E) item;
    }

    /**
     * 20210530
     * 实现所有排队方法。 见上文解释。
     */
    /**
     * Implements all queuing methods. See above for explanation.
     *
     * @param e the item or null for take
     * @param haveData true if this is a put, else a take
     * @param how NOW, ASYNC, SYNC, or TIMED
     * @param nanos timeout in nanosecs, used only if mode is TIMED
     * @return an item if matched, else e
     * @throws NullPointerException if haveData mode but e is null
     */
    private E xfer(E e, boolean haveData, int how, long nanos) {

        // 35. eg: poll() * 2: null, false, NOW, 0
        // 26. eg: poll(): null, false, NOW, 0
        // 9. eg: offer(E): e, true, ASYNC, 0
        // 0. eg: offer(E): e, true, ASYNC, 0
        if (haveData && (e == null))
            throw new NullPointerException();
        Node s = null;                        // the node to append, if needed

        retry:
        for (;;) {                            // restart on append race
            // 40. eg: poll() * 2: 继续自旋后, h=p=head=offer(E) * 2的s, 不为null
            // 36. eg: poll() * 2: h=p=head=offer(E)的s(这时s的item已经被上一个poll线程置null了), 不为null
            // 27. eg: poll(): h=p=head=offer(E)的s, 不为null
            // 10. eg: offer(E) * 2: h=p=head=offer(E)的s, 不为null
            // 1. eg: offer(E): h=p=head == null
            for (Node h = head, p = h; p != null;) { // find & match first node

                // 41. eg: poll() * 2: isData = true, item=offer(E) * 2的s.item
                // 37. eg: poll() * 2: isData = true, item=offer(E)的s.item, 但已经被上一个poll线程置null了, 所以item=null
                // 28. eg: poll(): isData = true, item=offer(E)的s.item
                // 11. eg: offer(E) * 2: isData=true, item=offer(E)的s.item
                boolean isData = p.isData;
                Object item = p.item;

                // 42. eg: poll() * 2: item=offer(E) * 2的s.item, p=head.next, item确实不为p, item!=null => true, 符合isData
                // 38. eg: poll() * 2: item=null, p=head, item确实不为p, item!=null => false, 不符合isData
                // 29. eg: poll(): item=offer(E)的s.item, p=head, item确实不为p, item!=null => true, 符合isData
                // 12. eg: offer(E) * 2: item确实不等于p, item不为null, 确认符合isData=true
                if (item != p && (item != null) == isData) { // unmatched

                    // 43. eg: poll() * 2: isData=true, haveData=false => false
                    // 30. eg: poll(): isData=true, haveData=false => false
                    // 13. eg: offer(E) * 2: isData == haveData == true, 所以退出循环, 表示不能匹配
                    if (isData == haveData)   // can't match
                        break;

                    // 44. eg: poll() * 2: CAS设置head.next的item为null
                    // 31. eg: poll(): CAS设置head的item为null
                    if (p.casItem(item, e)) { // match

                        // 45. eg: poll() * 2: q=p=head.next, h=head => q确实不等于h, 说明该线程需要移动head指针
                        // 32. eg: poll(): q=p=head, h=head => false
                        for (Node q = p; q != h;) {
                            Node n = q.next;  // update by 2 unless singleton   // 除非单例，否则更新 2
                            // 46. eg: poll() * 2: h=head=offer(E)的s, n=null, CAS设置head为q=p=head.next
                            if (head == h && casHead(h, n == null ? q : n)) {
                                // 46, eg: poll() * 2: CAS设置head为q=p=head.next成功, 则自链接旧的head, 退出循环即可
                                h.forgetNext();
                                break;
                            }                 // advance and retry  // 前进并重试
                            // 46. eg: poll() * 2: CAS设置head为q=p=head.next失败, 说明head指着已经被更新了,
                            //     则更新最新的head指针, 判断head.next是否已经匹配, 如果匹配了, 则继续向下寻找, 如果没有匹配则说明可以匹配, 退出循环即可
                            if ((h = head)   == null ||
                                (q = h.next) == null || !q.isMatched())
                                break;        // unless slack < 2
                        }

                        // 47, eg: poll() * 2: 退出循环说明head指针已经移动完成, 这时唤醒p的线程, 由于p为offer(E) * 2的s, 是异步非阻塞的添加, 没有线程 => 什么也不做
                        // 33. eg: poll(): 唤醒p的线程, 由于p为offer(E)的s, 是异步非阻塞的添加, 没有线程 => 什么也不做
                        LockSupport.unpark(p.waiter);

                        // 48, eg: poll() * 2: 返回配对的那个item
                        // 34. eg: poll(): 返回配对的那个item => 移动指针交由下一个线程来处理
                        return LinkedTransferQueue.<E>cast(item);
                    }
                }

                // 39. eg: poll() * 2: item不匹配isData, 说明被上一个线程置null了, 这时需要p右移一个结点, 继续自旋
                // 30. eg: poll(): CAS设置head的item为null, 如果设置失败, 说明被别的线程匹配了, 这时需要p右移一个结点, 继续自旋
                Node n = p.next;
                p = (p != n) ? n : (h = head); // Use head if p offlist
            }

            // 14. eg: offer(E) * 2: SYNC != NOW
            // 2. eg: offer(E): SYNC != NOW
            if (how != NOW) {                 // No matches available
                // 15. eg: offer(E) * 2: s为null => s[e, true]
                // 3. eg: offer(E): s为null => s[e, true]
                if (s == null)
                    s = new Node(e, haveData);

                // 16. eg: offer(E) * 2: CAS设置s为head.next, 成功则返回head => head != null, how == ASYNC
                // 4. eg: offer(E): CAS设置s为head指针, 成功则返回s => pred不为null, how == ASYNC
                Node pred = tryAppend(s, haveData);
                if (pred == null)
                    continue retry;           // lost race vs opposite mode
                if (how != ASYNC)
                    return awaitMatch(s, pred, e, (how == TIMED), nanos);
            }

            // 25. eg: offer(E) * 2: 返回添加的元素
            // 8. eg: offer(E): 返回添加的元素
            return e; // not waiting
        }
    }

    /**
     * 20210530
     * 尝试将节点 s 附加为尾部。
     */
    /**
     * Tries to append node s as tail.
     *
     * @param s the node to append
     * @param haveData true if appending in data mode
     * @return null on failure due to losing race with append in
     * different mode, else s's predecessor, or s itself if no
     * predecessor
     */
    private Node tryAppend(Node s, boolean haveData) {
        // 17. eg: offer(E) * 2: p=tail == null
        // 5. eg: offer(E): p=tail == null
        for (Node t = tail, p = t;;) {        // move p to last node and append // 将 p 移动到最后一个节点并追加
            Node n, u;                        // temps for reads of next & tail // 读取 next 和 tail 的温度

            // 18. eg: offer(E) * 2: p为null, 但head不为null => p=head
            // 6. eg: offer(E): p=heda == null
            if (p == null && (p = head) == null) {
                // 7. eg: offer(E): CAS设置s为head指针, 成功则返回s
                if (casHead(null, s))
                    return s;                 // initialize
            }

            // 19. eg: offer(E) * 2: head的isData = havaData = true, 返回false, 说明可以处理, 不用返回false
            else if (p.cannotPrecede(haveData))
                return null;                  // lost race vs opposite mode // 输了比赛 vs 对立模式

            // 20. eg: offer(E) * 2: head.next == null, n=null
            else if ((n = p.next) != null)    // not last; keep traversing  // 不持久； 继续穿越
                p =
                    p != t && t != (u = tail) ?
                    (t = u) :
                    (p != n)?
                        n :
                        null;

            // 21. eg: offer(E) * 2: p=head, CAS设置head的next为s
            else if (!p.casNext(null, s))
                p = p.next;                   // re-read on CAS failure // 重读CAS失败

            else {
                // 22. eg: offer(E) * 2: p.next设置成功, 则判断p=head, t=null => p确定不等于t
                if (p != t) {                 // update if slack now >= 2   // 如果现在松弛 >= 2 则更新
                    // 23. eg: offer(E) * 2: tail=t=null, CAS设置t为s => t=tail != null => 退出循环
                    while ((tail != t || !casTail(t, s)) &&
                           (t = tail)   != null &&
                           (s = t.next) != null && // advance and retry
                           (s = s.next) != null && s != t);
                }

                // 24. eg: offer(E) * 2: 返回p=head
                return p;
            }
        }
    }

    /**
     * Spins/yields/blocks until node s is matched or caller gives up.
     *
     * @param s the waiting node
     * @param pred the predecessor of s, or s itself if it has no
     * predecessor, or null if unknown (the null case does not occur
     * in any current calls but may in possible future extensions)
     * @param e the comparison value for checking match
     * @param timed if true, wait only until timeout elapses
     * @param nanos timeout in nanosecs, used only if timed is true
     * @return matched item, or e if unmatched on interrupt or timeout
     */
    private E awaitMatch(Node s, Node pred, E e, boolean timed, long nanos) {
        final long deadline = timed ? System.nanoTime() + nanos : 0L;
        Thread w = Thread.currentThread();
        int spins = -1; // initialized after first item and cancel checks
        ThreadLocalRandom randomYields = null; // bound if needed

        for (;;) {
            Object item = s.item;
            if (item != e) {                  // matched
                // assert item != s;
                s.forgetContents();           // avoid garbage
                return LinkedTransferQueue.<E>cast(item);
            }
            if ((w.isInterrupted() || (timed && nanos <= 0)) &&
                    s.casItem(e, s)) {        // cancel
                unsplice(pred, s);
                return e;
            }

            if (spins < 0) {                  // establish spins at/near front
                if ((spins = spinsFor(pred, s.isData)) > 0)
                    randomYields = ThreadLocalRandom.current();
            }
            else if (spins > 0) {             // spin
                --spins;
                if (randomYields.nextInt(CHAINED_SPINS) == 0)
                    Thread.yield();           // occasionally yield
            }
            else if (s.waiter == null) {
                s.waiter = w;                 // request unpark then recheck
            }
            else if (timed) {
                nanos = deadline - System.nanoTime();
                if (nanos > 0L)
                    LockSupport.parkNanos(this, nanos);
            }
            else {
                LockSupport.park(this);
            }
        }
    }

    /**
     * Returns spin/yield value for a node with given predecessor and
     * data mode. See above for explanation.
     */
    private static int spinsFor(Node pred, boolean haveData) {
        if (MP && pred != null) {
            if (pred.isData != haveData)      // phase change
                return FRONT_SPINS + CHAINED_SPINS;
            if (pred.isMatched())             // probably at front
                return FRONT_SPINS;
            if (pred.waiter == null)          // pred apparently spinning
                return CHAINED_SPINS;
        }
        return 0;
    }

    /* -------------- Traversal methods -------------- */

    /**
     * 20210530
     * 返回 p 的后继节点，或者如果 p.next 已经链接到 self 则返回头节点，这只有在使用现在不在列表中的陈旧指针遍历时才会为真。
     */
    /**
     * Returns the successor of p, or the head node if p.next has been
     * linked to self, which will only be true if traversing with a
     * stale pointer that is now off the list.
     */
    final Node succ(Node p) {
        Node next = p.next;
        return (p == next) ? head : next;
    }

    /**
     * Returns the first unmatched node of the given mode, or null if
     * none.  Used by methods isEmpty, hasWaitingConsumer.
     */
    private Node firstOfMode(boolean isData) {
        for (Node p = head; p != null; p = succ(p)) {
            if (!p.isMatched())
                return (p.isData == isData) ? p : null;
        }
        return null;
    }

    /**
     * Version of firstOfMode used by Spliterator. Callers must
     * recheck if the returned node's item field is null or
     * self-linked before using.
     */
    final Node firstDataNode() {
        for (Node p = head; p != null;) {
            Object item = p.item;
            if (p.isData) {
                if (item != null && item != p)
                    return p;
            }
            else if (item == null)
                break;
            if (p == (p = p.next))
                p = head;
        }
        return null;
    }

    /**
     * 20210530
     * 使用 isData 返回第一个不匹配节点中的项目； 如果没有，则为 null。 由 peek 使用。
     */
    /**
     * Returns the item in the first unmatched node with isData; or
     * null if none.  Used by peek.
     */
    private E firstDataItem() {
        for (Node p = head; p != null; p = succ(p)) {
            Object item = p.item;
            if (p.isData) {
                if (item != null && item != p)
                    return LinkedTransferQueue.<E>cast(item);
            }
            else if (item == null)
                return null;
        }
        return null;
    }

    /**
     * Traverses and counts unmatched nodes of the given mode.
     * Used by methods size and getWaitingConsumerCount.
     */
    private int countOfMode(boolean data) {
        int count = 0;
        for (Node p = head; p != null; ) {
            if (!p.isMatched()) {
                if (p.isData != data)
                    return 0;
                if (++count == Integer.MAX_VALUE) // saturated
                    break;
            }
            Node n = p.next;
            if (n != p)
                p = n;
            else {
                count = 0;
                p = head;
            }
        }
        return count;
    }

    final class Itr implements Iterator<E> {
        private Node nextNode;   // next node to return item for
        private E nextItem;      // the corresponding item
        private Node lastRet;    // last returned node, to support remove
        private Node lastPred;   // predecessor to unlink lastRet

        /**
         * Moves to next node after prev, or first node if prev null.
         */
        private void advance(Node prev) {
            /*
             * To track and avoid buildup of deleted nodes in the face
             * of calls to both Queue.remove and Itr.remove, we must
             * include variants of unsplice and sweep upon each
             * advance: Upon Itr.remove, we may need to catch up links
             * from lastPred, and upon other removes, we might need to
             * skip ahead from stale nodes and unsplice deleted ones
             * found while advancing.
             */

            Node r, b; // reset lastPred upon possible deletion of lastRet
            if ((r = lastRet) != null && !r.isMatched())
                lastPred = r;    // next lastPred is old lastRet
            else if ((b = lastPred) == null || b.isMatched())
                lastPred = null; // at start of list
            else {
                Node s, n;       // help with removal of lastPred.next
                while ((s = b.next) != null &&
                       s != b && s.isMatched() &&
                       (n = s.next) != null && n != s)
                    b.casNext(s, n);
            }

            this.lastRet = prev;

            for (Node p = prev, s, n;;) {
                s = (p == null) ? head : p.next;
                if (s == null)
                    break;
                else if (s == p) {
                    p = null;
                    continue;
                }
                Object item = s.item;
                if (s.isData) {
                    if (item != null && item != s) {
                        nextItem = LinkedTransferQueue.<E>cast(item);
                        nextNode = s;
                        return;
                    }
                }
                else if (item == null)
                    break;
                // assert s.isMatched();
                if (p == null)
                    p = s;
                else if ((n = s.next) == null)
                    break;
                else if (s == n)
                    p = null;
                else
                    p.casNext(s, n);
            }
            nextNode = null;
            nextItem = null;
        }

        Itr() {
            advance(null);
        }

        public final boolean hasNext() {
            return nextNode != null;
        }

        public final E next() {
            Node p = nextNode;
            if (p == null) throw new NoSuchElementException();
            E e = nextItem;
            advance(p);
            return e;
        }

        public final void remove() {
            final Node lastRet = this.lastRet;
            if (lastRet == null)
                throw new IllegalStateException();
            this.lastRet = null;
            if (lastRet.tryMatchData())
                unsplice(lastPred, lastRet);
        }
    }

    /** A customized variant of Spliterators.IteratorSpliterator */
    static final class LTQSpliterator<E> implements Spliterator<E> {
        static final int MAX_BATCH = 1 << 25;  // max batch array size;
        final LinkedTransferQueue<E> queue;
        Node current;    // current node; null until initialized
        int batch;          // batch size for splits
        boolean exhausted;  // true when no more nodes
        LTQSpliterator(LinkedTransferQueue<E> queue) {
            this.queue = queue;
        }

        public Spliterator<E> trySplit() {
            Node p;
            final LinkedTransferQueue<E> q = this.queue;
            int b = batch;
            int n = (b <= 0) ? 1 : (b >= MAX_BATCH) ? MAX_BATCH : b + 1;
            if (!exhausted &&
                ((p = current) != null || (p = q.firstDataNode()) != null) &&
                p.next != null) {
                Object[] a = new Object[n];
                int i = 0;
                do {
                    Object e = p.item;
                    if (e != p && (a[i] = e) != null)
                        ++i;
                    if (p == (p = p.next))
                        p = q.firstDataNode();
                } while (p != null && i < n && p.isData);
                if ((current = p) == null)
                    exhausted = true;
                if (i > 0) {
                    batch = i;
                    return Spliterators.spliterator
                        (a, 0, i, Spliterator.ORDERED | Spliterator.NONNULL |
                         Spliterator.CONCURRENT);
                }
            }
            return null;
        }

        @SuppressWarnings("unchecked")
        public void forEachRemaining(Consumer<? super E> action) {
            Node p;
            if (action == null) throw new NullPointerException();
            final LinkedTransferQueue<E> q = this.queue;
            if (!exhausted &&
                ((p = current) != null || (p = q.firstDataNode()) != null)) {
                exhausted = true;
                do {
                    Object e = p.item;
                    if (e != null && e != p)
                        action.accept((E)e);
                    if (p == (p = p.next))
                        p = q.firstDataNode();
                } while (p != null && p.isData);
            }
        }

        @SuppressWarnings("unchecked")
        public boolean tryAdvance(Consumer<? super E> action) {
            Node p;
            if (action == null) throw new NullPointerException();
            final LinkedTransferQueue<E> q = this.queue;
            if (!exhausted &&
                ((p = current) != null || (p = q.firstDataNode()) != null)) {
                Object e;
                do {
                    if ((e = p.item) == p)
                        e = null;
                    if (p == (p = p.next))
                        p = q.firstDataNode();
                } while (e == null && p != null && p.isData);
                if ((current = p) == null)
                    exhausted = true;
                if (e != null) {
                    action.accept((E)e);
                    return true;
                }
            }
            return false;
        }

        public long estimateSize() { return Long.MAX_VALUE; }

        public int characteristics() {
            return Spliterator.ORDERED | Spliterator.NONNULL |
                Spliterator.CONCURRENT;
        }
    }

    /**
     * Returns a {@link Spliterator} over the elements in this queue.
     *
     * <p>The returned spliterator is
     * <a href="package-summary.html#Weakly"><i>weakly consistent</i></a>.
     *
     * <p>The {@code Spliterator} reports {@link Spliterator#CONCURRENT},
     * {@link Spliterator#ORDERED}, and {@link Spliterator#NONNULL}.
     *
     * @implNote
     * The {@code Spliterator} implements {@code trySplit} to permit limited
     * parallelism.
     *
     * @return a {@code Spliterator} over the elements in this queue
     * @since 1.8
     */
    public Spliterator<E> spliterator() {
        return new LTQSpliterator<E>(this);
    }

    /* -------------- Removal methods -------------- */

    /**
     * Unsplices (now or later) the given deleted/cancelled node with
     * the given predecessor.
     *
     * @param pred a node that was at one time known to be the
     * predecessor of s, or null or s itself if s is/was at head
     * @param s the node to be unspliced
     */
    final void unsplice(Node pred, Node s) {
        s.forgetContents(); // forget unneeded fields
        /*
         * See above for rationale. Briefly: if pred still points to
         * s, try to unlink s.  If s cannot be unlinked, because it is
         * trailing node or pred might be unlinked, and neither pred
         * nor s are head or offlist, add to sweepVotes, and if enough
         * votes have accumulated, sweep.
         */
        if (pred != null && pred != s && pred.next == s) {
            Node n = s.next;
            if (n == null ||
                (n != s && pred.casNext(s, n) && pred.isMatched())) {
                for (;;) {               // check if at, or could be, head
                    Node h = head;
                    if (h == pred || h == s || h == null)
                        return;          // at head or list empty
                    if (!h.isMatched())
                        break;
                    Node hn = h.next;
                    if (hn == null)
                        return;          // now empty
                    if (hn != h && casHead(h, hn))
                        h.forgetNext();  // advance head
                }
                if (pred.next != pred && s.next != s) { // recheck if offlist
                    for (;;) {           // sweep now if enough votes
                        int v = sweepVotes;
                        if (v < SWEEP_THRESHOLD) {
                            if (casSweepVotes(v, v + 1))
                                break;
                        }
                        else if (casSweepVotes(v, 0)) {
                            sweep();
                            break;
                        }
                    }
                }
            }
        }
    }

    /**
     * Unlinks matched (typically cancelled) nodes encountered in a
     * traversal from head.
     */
    private void sweep() {
        for (Node p = head, s, n; p != null && (s = p.next) != null; ) {
            if (!s.isMatched())
                // Unmatched nodes are never self-linked
                p = s;
            else if ((n = s.next) == null) // trailing node is pinned
                break;
            else if (s == n)    // stale
                // No need to also check for p == s, since that implies s == n
                p = head;
            else
                p.casNext(s, n);
        }
    }

    /**
     * Main implementation of remove(Object)
     */
    private boolean findAndRemove(Object e) {
        if (e != null) {
            for (Node pred = null, p = head; p != null; ) {
                Object item = p.item;
                if (p.isData) {
                    if (item != null && item != p && e.equals(item) &&
                        p.tryMatchData()) {
                        unsplice(pred, p);
                        return true;
                    }
                }
                else if (item == null)
                    break;
                pred = p;
                if ((p = p.next) == pred) { // stale
                    pred = null;
                    p = head;
                }
            }
        }
        return false;
    }

    /**
     * Creates an initially empty {@code LinkedTransferQueue}.
     */
    public LinkedTransferQueue() {
    }

    /**
     * Creates a {@code LinkedTransferQueue}
     * initially containing the elements of the given collection,
     * added in traversal order of the collection's iterator.
     *
     * @param c the collection of elements to initially contain
     * @throws NullPointerException if the specified collection or any
     *         of its elements are null
     */
    public LinkedTransferQueue(Collection<? extends E> c) {
        this();
        addAll(c);
    }

    /**
     * 20210530
     * 在此队列的尾部插入指定的元素。 由于队列是无界的，这个方法永远不会阻塞。
     */
    /**
     * Inserts the specified element at the tail of this queue.
     * As the queue is unbounded, this method will never block.
     *
     * @throws NullPointerException if the specified element is null
     */
    public void put(E e) {
        xfer(e, true, ASYNC, 0);
    }

    /**
     * 20210530
     * 在此队列的尾部插入指定的元素。 由于队列是无界的，此方法永远不会阻塞或返回 {@code false}。
     */
    /**
     * Inserts the specified element at the tail of this queue.
     * As the queue is unbounded, this method will never block or
     * return {@code false}.
     *
     * @return {@code true} (as specified by
     *  {@link java.util.concurrent.BlockingQueue#offer(Object,long,TimeUnit)
     *  BlockingQueue.offer})
     * @throws NullPointerException if the specified element is null
     */
    public boolean offer(E e, long timeout, TimeUnit unit) {
        xfer(e, true, ASYNC, 0);
        return true;
    }

    /**
     * 20210530
     * 在此队列的尾部插入指定的元素。 由于队列是无界的，这个方法永远不会返回 {@code false}。
     */
    /**
     * Inserts the specified element at the tail of this queue.
     * As the queue is unbounded, this method will never return {@code false}.
     *
     * @return {@code true} (as specified by {@link Queue#offer})
     * @throws NullPointerException if the specified element is null
     */
    public boolean offer(E e) {
        xfer(e, true, ASYNC, 0);
        return true;
    }

    /**
     * 20210530
     * 在此队列的尾部插入指定的元素。 由于队列是无界的，此方法永远不会抛出 {@link IllegalStateException} 或返回 {@code false}。
     */
    /**
     * Inserts the specified element at the tail of this queue.
     * As the queue is unbounded, this method will never throw
     * {@link IllegalStateException} or return {@code false}.
     *
     * @return {@code true} (as specified by {@link Collection#add})
     * @throws NullPointerException if the specified element is null
     */
    public boolean add(E e) {
        xfer(e, true, ASYNC, 0);
        return true;
    }

    /**
     * 20210530
     * A. 如果可能，立即将元素传输给等待的使用者。
     * B. 更准确地说，如果存在已经等待接收它的消费者（在 {@link #take} 或定时 {@link #poll(long,TimeUnit) poll}），则立即传输指定的元素，
     *    否则返回 {@code false} 而不 元素入队。
     */
    /**
     * A.
     * Transfers the element to a waiting consumer immediately, if possible.
     *
     * B.
     * <p>More precisely, transfers the specified element immediately
     * if there exists a consumer already waiting to receive it (in
     * {@link #take} or timed {@link #poll(long,TimeUnit) poll}),
     * otherwise returning {@code false} without enqueuing the element.
     *
     * @throws NullPointerException if the specified element is null
     */
    public boolean tryTransfer(E e) {
        return xfer(e, true, NOW, 0) == null;
    }

    /**
     * 20210530
     * A. 将元素传输给消费者，必要时等待。
     * B. 更确切地说，如果存在已经在等待接收它的使用者（在{@link #take}或定时{@link #poll（long，TimeUnit）poll}中），
     *    则立即传输指定的元素，否则将指定的元素插入尾部 并等待该元素被消费者接收。
     */
    /**
     * A.
     * Transfers the element to a consumer, waiting if necessary to do so.
     *
     * B.
     * <p>More precisely, transfers the specified element immediately
     * if there exists a consumer already waiting to receive it (in
     * {@link #take} or timed {@link #poll(long,TimeUnit) poll}),
     * else inserts the specified element at the tail of this queue
     * and waits until the element is received by a consumer.
     *
     * @throws NullPointerException if the specified element is null
     */
    public void transfer(E e) throws InterruptedException {
        if (xfer(e, true, SYNC, 0) != null) {
            Thread.interrupted(); // failure possible only due to interrupt
            throw new InterruptedException();
        }
    }

    /**
     * 20210530
     * A. 如果有可能，在超时之前将元素转移给使用者。
     * B. 更准确地说，如果存在已经等待接收它的消费者（在{@link #take} 或定时 {@link #poll(long,TimeUnit) poll}），则立即传输指定的元素，否则在尾部插入指定的元素
     *    并等待该元素被消费者接收到，如果指定的等待时间过去后元素可以被传输，则返回 {@code false}。
     */
    /**
     * A.
     * Transfers the element to a consumer if it is possible to do so
     * before the timeout elapses.
     *
     * B.
     * <p>More precisely, transfers the specified element immediately
     * if there exists a consumer already waiting to receive it (in
     * {@link #take} or timed {@link #poll(long,TimeUnit) poll}),
     * else inserts the specified element at the tail of this queue
     * and waits until the element is received by a consumer,
     * returning {@code false} if the specified wait time elapses
     * before the element can be transferred.
     *
     * @throws NullPointerException if the specified element is null
     */
    public boolean tryTransfer(E e, long timeout, TimeUnit unit)
        throws InterruptedException {
        if (xfer(e, true, TIMED, unit.toNanos(timeout)) == null)
            return true;
        if (!Thread.interrupted())
            return false;
        throw new InterruptedException();
    }

    public E take() throws InterruptedException {
        E e = xfer(null, false, SYNC, 0);
        if (e != null)
            return e;
        Thread.interrupted();
        throw new InterruptedException();
    }

    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        E e = xfer(null, false, TIMED, unit.toNanos(timeout));
        if (e != null || !Thread.interrupted())
            return e;
        throw new InterruptedException();
    }

    public E poll() {
        return xfer(null, false, NOW, 0);
    }

    /**
     * @throws NullPointerException     {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    public int drainTo(Collection<? super E> c) {
        if (c == null)
            throw new NullPointerException();
        if (c == this)
            throw new IllegalArgumentException();
        int n = 0;
        for (E e; (e = poll()) != null;) {
            c.add(e);
            ++n;
        }
        return n;
    }

    /**
     * @throws NullPointerException     {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    public int drainTo(Collection<? super E> c, int maxElements) {
        if (c == null)
            throw new NullPointerException();
        if (c == this)
            throw new IllegalArgumentException();
        int n = 0;
        for (E e; n < maxElements && (e = poll()) != null;) {
            c.add(e);
            ++n;
        }
        return n;
    }

    /**
     * 20210530
     * A. 以适当的顺序返回此队列中元素的迭代器。 元素将按从第一个（头）到最后一个（尾）的顺序返回。
     * B. 返回的迭代器弱一致。
     */
    /**
     * A.
     * Returns an iterator over the elements in this queue in proper sequence.
     * The elements will be returned in order from first (head) to last (tail).
     *
     * B.
     * <p>The returned iterator is
     * <a href="package-summary.html#Weakly"><i>weakly consistent</i></a>.
     *
     * @return an iterator over the elements in this queue in proper sequence
     */
    public Iterator<E> iterator() {
        return new Itr();
    }

    public E peek() {
        return firstDataItem();
    }

    /**
     * Returns {@code true} if this queue contains no elements.
     *
     * @return {@code true} if this queue contains no elements
     */
    public boolean isEmpty() {
        for (Node p = head; p != null; p = succ(p)) {
            if (!p.isMatched())
                return !p.isData;
        }
        return true;
    }

    public boolean hasWaitingConsumer() {
        return firstOfMode(false) != null;
    }

    /**
     * Returns the number of elements in this queue.  If this queue
     * contains more than {@code Integer.MAX_VALUE} elements, returns
     * {@code Integer.MAX_VALUE}.
     *
     * <p>Beware that, unlike in most collections, this method is
     * <em>NOT</em> a constant-time operation. Because of the
     * asynchronous nature of these queues, determining the current
     * number of elements requires an O(n) traversal.
     *
     * @return the number of elements in this queue
     */
    public int size() {
        return countOfMode(true);
    }

    public int getWaitingConsumerCount() {
        return countOfMode(false);
    }

    /**
     * 20210530
     * 从此队列中移除指定元素的单个实例（如果存在）。 更正式地，如果此队列包含一个或多个此类元素，则删除元素 {@code e} 使得 {@code o.equals(e)}。
     * 如果此队列包含指定的元素（或等效地，如果此队列因调用而更改），则返回 {@code true}。
     */
    /**
     * Removes a single instance of the specified element from this queue,
     * if it is present.  More formally, removes an element {@code e} such
     * that {@code o.equals(e)}, if this queue contains one or more such
     * elements.
     * Returns {@code true} if this queue contained the specified element
     * (or equivalently, if this queue changed as a result of the call).
     *
     * @param o element to be removed from this queue, if present
     * @return {@code true} if this queue changed as a result of the call
     */
    public boolean remove(Object o) {
        return findAndRemove(o);
    }

    /**
     * Returns {@code true} if this queue contains the specified element.
     * More formally, returns {@code true} if and only if this queue contains
     * at least one element {@code e} such that {@code o.equals(e)}.
     *
     * @param o object to be checked for containment in this queue
     * @return {@code true} if this queue contains the specified element
     */
    public boolean contains(Object o) {
        if (o == null) return false;
        for (Node p = head; p != null; p = succ(p)) {
            Object item = p.item;
            if (p.isData) {
                if (item != null && item != p && o.equals(item))
                    return true;
            }
            else if (item == null)
                break;
        }
        return false;
    }

    /**
     * Always returns {@code Integer.MAX_VALUE} because a
     * {@code LinkedTransferQueue} is not capacity constrained.
     *
     * @return {@code Integer.MAX_VALUE} (as specified by
     *         {@link java.util.concurrent.BlockingQueue#remainingCapacity()
     *         BlockingQueue.remainingCapacity})
     */
    public int remainingCapacity() {
        return Integer.MAX_VALUE;
    }

    /**
     * Saves this queue to a stream (that is, serializes it).
     *
     * @param s the stream
     * @throws java.io.IOException if an I/O error occurs
     * @serialData All of the elements (each an {@code E}) in
     * the proper order, followed by a null
     */
    private void writeObject(java.io.ObjectOutputStream s)
        throws java.io.IOException {
        s.defaultWriteObject();
        for (E e : this)
            s.writeObject(e);
        // Use trailing null as sentinel
        s.writeObject(null);
    }

    /**
     * Reconstitutes this queue from a stream (that is, deserializes it).
     * @param s the stream
     * @throws ClassNotFoundException if the class of a serialized object
     *         could not be found
     * @throws java.io.IOException if an I/O error occurs
     */
    private void readObject(java.io.ObjectInputStream s)
        throws java.io.IOException, ClassNotFoundException {
        s.defaultReadObject();
        for (;;) {
            @SuppressWarnings("unchecked")
            E item = (E) s.readObject();
            if (item == null)
                break;
            else
                offer(item);
        }
    }

    // Unsafe mechanics

    private static final sun.misc.Unsafe UNSAFE;
    private static final long headOffset;
    private static final long tailOffset;
    private static final long sweepVotesOffset;
    static {
        try {
            UNSAFE = sun.misc.Unsafe.getUnsafe();
            Class<?> k = LinkedTransferQueue.class;
            headOffset = UNSAFE.objectFieldOffset
                (k.getDeclaredField("head"));
            tailOffset = UNSAFE.objectFieldOffset
                (k.getDeclaredField("tail"));
            sweepVotesOffset = UNSAFE.objectFieldOffset
                (k.getDeclaredField("sweepVotes"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }
}
