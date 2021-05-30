/*
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

/*
 * Written by Doug Lea and Martin Buchholz with assistance from members of
 * JCP JSR-166 Expert Group and released to the public domain, as explained
 * at http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util.concurrent;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;

/**
 * 20210529
 * A. 基于链接节点的无界并发双端队列 {@linkplain Deque deque}。 并发插入、删除和访问操作跨多个线程安全地执行。 当许多线程将共享对公共集合的访问时，
 *    {@code ConcurrentLinkedDeque} 是合适的选择。 与大多数其他并发集合实现一样，此类不允许使用{@code null}元素。
 * B. 迭代器和拆分器弱一致的。
 * C. 请注意，与大多数集合不同，{@code size} 方法不是恒定时间操作。 由于这些双端队列的异步性质，确定当前元素的数量需要遍历元素，因此如果在遍历期间修改此集合，
 *    则可能会报告不准确的结果。 此外，批量操作 {@code addAll}、{@code removeAll}、{@code retainAll}、{@code containsAll}、{@code equals} 和
 *    {@code toArray} 不能保证以原子方式执行。 例如，与 {@code addAll} 操作同时运行的迭代器可能只查看一些添加的元素。
 * D. 此类及其迭代器实现了 {@link Deque} 和 {@link Iterator} 接口的所有可选方法。
 * E. 内存一致性效果：与其他并发集合一样，在将对象放入 {@code ConcurrentLinkedDeque} 之前的线程中的操作
 *    <a href="package-summary.html#MemoryVisibility">happen-before</a> 之后的操作 在另一个线程中从{@code ConcurrentLinkedDeque}访问或删除该元素。
 * F. {@docRoot}/../technotes/guides/collections/index.html
 */

/**
 * A.
 * An unbounded concurrent {@linkplain Deque deque} based on linked nodes.
 * Concurrent insertion, removal, and access operations execute safely
 * across multiple threads.
 * A {@code ConcurrentLinkedDeque} is an appropriate choice when
 * many threads will share access to a common collection.
 * Like most other concurrent collection implementations, this class
 * does not permit the use of {@code null} elements.
 *
 * B.
 * <p>Iterators and spliterators are
 * <a href="package-summary.html#Weakly"><i>weakly consistent</i></a>.
 *
 * C.
 * <p>Beware that, unlike in most collections, the {@code size} method
 * is <em>NOT</em> a constant-time operation. Because of the
 * asynchronous nature of these deques, determining the current number
 * of elements requires a traversal of the elements, and so may report
 * inaccurate results if this collection is modified during traversal.
 * Additionally, the bulk operations {@code addAll},
 * {@code removeAll}, {@code retainAll}, {@code containsAll},
 * {@code equals}, and {@code toArray} are <em>not</em> guaranteed
 * to be performed atomically. For example, an iterator operating
 * concurrently with an {@code addAll} operation might view only some
 * of the added elements.
 *
 * D.
 * <p>This class and its iterator implement all of the <em>optional</em>
 * methods of the {@link Deque} and {@link Iterator} interfaces.
 *
 * E.
 * <p>Memory consistency effects: As with other concurrent collections,
 * actions in a thread prior to placing an object into a
 * {@code ConcurrentLinkedDeque}
 * <a href="package-summary.html#MemoryVisibility"><i>happen-before</i></a>
 * actions subsequent to the access or removal of that element from
 * the {@code ConcurrentLinkedDeque} in another thread.
 *
 * F.
 * <p>This class is a member of the
 * <a href="{@docRoot}/../technotes/guides/collections/index.html">
 * Java Collections Framework</a>.
 *
 * @since 1.7
 * @author Doug Lea
 * @author Martin Buchholz
 * @param <E> the type of elements held in this collection
 */
public class ConcurrentLinkedDeque<E> extends AbstractCollection<E> implements Deque<E>, java.io.Serializable {

    /**
     * 20210529
     * A. 这是支持内部删除但不支持内部插入的并发无锁双端队列的实现，这是支持整个双端队列接口所需的。
     * B. 我们扩展了为 ConcurrentLinkedQueue 和 LinkedTransferQueue 开发的技术（请参阅这些类的内部文档）。 理解 ConcurrentLinkedQueue 的实现是理解这个类的实现的前提。
     * C. 数据结构是一个对称的双链“GC-robust”节点链表。 我们使用两种技术最小化易失性写入的数量：使用单个 CAS 推进多跳，以及混合相同内存位置的易失性和非易失性写入。
     * D. 一个节点包含预期的 E（“item”）以及指向前驱（“prev”）和后继（“next”）节点的链接：
     *      class Node<E> {
     *          volatile Node<E> prev, next; volatile E item;
     *      }
     * E. 如果节点 p 包含非空项目（p.item != null），则它被认为是“活动的”。 当一个项目被 CASed 为 null 时，该项目从集合中以原子逻辑的方式被删除。
     * F. 在任何时候，都有一个带有空 prev 引用的“第一个”节点，它终止从活动节点开始的任何 prev 引用链。 类似地，恰好有一个“最后”节点终止从活动节点开始的任何下一引用链。
     *    “第一个”和“最后一个”节点可能存在也可能不存在。 “第一个”和“最后一个”节点总是相互可达的。
     * G. 通过将第一个或最后一个节点中的空prev或next引用CAS到包含该元素的新鲜节点上，原子地添加了一个新元素。 元素的节点在这一点上原子地变为“活动”。
     * H. 如果节点是活动节点，或者是第一个或最后一个节点，则该节点被认为是“活动的”。 活动节点不能取消链接。
     * I. “自链接”是指同一节点的下一个或上一个引用： p.prev == p 或 p.next == p 自链接用于节点取消链接过程。 活动节点永远不会有自链接。
     * J. 当且仅当以下情况，节点p才是活动的：
     *      p.item != null ||
     *      (p.prev == null && p.next != p) ||
     *      (p.next == null && p.prev != p)
     * K. deque 对象有两个节点引用，“head”和“tail”。 头和尾只是双端队列的第一个和最后一个节点的近似值。 始终可以通过从 head 跟随 prev 指针来找到第一个节点；
     *    尾巴也是如此。 但是，head 和tail 可以指代已取消链接的已删除节点，因此可能无法从任何活动节点访问。
     * L. 节点删除分为3个阶段： “逻辑删除”、“取消链接”和“gc 取消链接”:
     *      1. 通过CASing item为null的“逻辑删除”从集合中原子地删除元素，并使包含节点有资格取消链接。
     *      2. “unlinking”使已删除的节点无法从活动节点访问，因此最终可以被 GC 回收。 未链接的节点可以从迭代器无限期地保持可达。
     *      3. “gc-unlinking”通过使已删除节点无法访问活动节点来进一步取消链接，从而使 GC 更容易回收未来已删除的节点。 这一步使数据结构“gc-robust”，
     *          正如 Boehm 首先详细描述的（http://portal.acm.org/citation.cfm?doid=503272.503282）。
     * M. GC 未链接的节点可以从迭代器无限期地保持可达，但与未链接的节点不同，从头或尾永远无法到达。
     * N. 使数据结构具有 GC 健壮性将消除使用保守 GC 的无限内存保留的风险，并可能通过分代 GC 提高性能。
     * O. 当节点在任一端出队时，例如通过 poll()，我们想打破从节点到活动节点的任何引用。我们进一步开发了在其他并发集合类中非常有效的自链接的使用。
     *    这个想法是用特殊值替换 prev 和 next 指针，这些值被解释为意味着在一端不在列表中。这些是近似值，但足以保留我们在遍历中想要的属性，
     *    例如我们保证遍历永远不会访问同一个元素两次，但我们不保证用完元素的遍历在该端入队后是否能够看到更多元素。安全地执行 gc-unlinking 特别棘手，
     *    因为任何节点都可以无限期地使用（例如通过迭代器）。我们必须确保 head/tail 指向的节点永远不会被 gc-unlinked，
     *    因为 head/tail 需要被其他没有 gc-unlinked 的节点“回到正轨”。 gc-unlinking 解释了大部分的实现复杂性。
     * P. 由于 unlinking 和 gc-unlinking 都不是正确性所必需的，因此有许多关于这些操作的频率（热切性）的实现选择。 由于 volatile 读取可能比 CAS 便宜得多，
     *    因此通过一次断开多个相邻节点的链接来节省 CAS 可能是一种胜利。 gc-unlinking 可以很少执行但仍然有效，因为最重要的是删除节点的长链偶尔会被破坏。
     * Q. 我们使用的实际表示形式是p.next == p表示是否需要转到第一个节点（依次通过跟随来自head的prev指针到达），
     *    p.next == null && p.prev == p表示迭代结束，p 是（静态最终）虚拟节点 NEXT_TERMINATOR，而不是最后一个活动节点。
     *    遇到这样的 TERMINATOR 就结束迭代对于只读遍历来说已经足够好了，所以这样的遍历可以使用 p.next == null 作为终止条件。
     *    当我们需要找到最后一个（活动）节点时，为了将一个新节点入队，我们需要检查我们是否已经到达了一个 TERMINATOR 节点； 如果是，则从尾部重新开始遍历。
     * R. 该实现是完全方向对称的，除了大多数遍历列表的公共方法遵循下一个指针（“向前”方向）。
     * S. 我们认为（没有充分的证据）所有单元素双端队列操作（例如addFirst，peekLast，pollLast）都是可线性化的（请参见Herlihy和Shavit的书）, 即O(n)。
     *    然而，已知某些操作组合不可线性化。 特别是，当 addFirst(A) 与 pollFirst() 移除 B 竞争时，即使从未执行过内部移除，
     *    观察者也有可能在元素上迭代观察 A B C 并随后观察 A C。 尽管如此，迭代器的行为还是合理的，提供了“弱一致性”的保证。
     * T. 根据经验，微基准测试表明，这个类相对于ConcurrentLinkedQueue增加了大约40%的开销，这感觉就像我们希望的那样好。
     */
    /*
     * A.
     * This is an implementation of a concurrent lock-free deque
     * supporting interior removes but not interior insertions, as
     * required to support the entire Deque interface.
     *
     * B.
     * We extend the techniques developed for ConcurrentLinkedQueue and
     * LinkedTransferQueue (see the internal docs for those classes).
     * Understanding the ConcurrentLinkedQueue implementation is a
     * prerequisite for understanding the implementation of this class.
     *
     * C.
     * The data structure is a symmetrical doubly-linked "GC-robust"
     * linked list of nodes.  We minimize the number of volatile writes
     * using two techniques: advancing multiple hops with a single CAS
     * and mixing volatile and non-volatile writes of the same memory
     * locations.
     *
     * D.
     * A node contains the expected E ("item") and links to predecessor
     * ("prev") and successor ("next") nodes:
     *
     * class Node<E> { volatile Node<E> prev, next; volatile E item; }
     *
     * E.
     * A node p is considered "live" if it contains a non-null item
     * (p.item != null).  When an item is CASed to null, the item is
     * atomically logically deleted from the collection.
     *
     * F.
     * At any time, there is precisely one "first" node with a null
     * prev reference that terminates any chain of prev references
     * starting at a live node.  Similarly there is precisely one
     * "last" node terminating any chain of next references starting at
     * a live node.  The "first" and "last" nodes may or may not be live.
     * The "first" and "last" nodes are always mutually reachable.
     *
     * G.
     * A new element is added atomically by CASing the null prev or
     * next reference in the first or last node to a fresh node
     * containing the element.  The element's node atomically becomes
     * "live" at that point.
     *
     * H.
     * A node is considered "active" if it is a live node, or the
     * first or last node.  Active nodes cannot be unlinked.
     *
     * I.
     * A "self-link" is a next or prev reference that is the same node:
     *   p.prev == p  or  p.next == p
     * Self-links are used in the node unlinking process.  Active nodes
     * never have self-links.
     *
     * J.
     * A node p is active if and only if:
     *
     * p.item != null ||
     * (p.prev == null && p.next != p) ||
     * (p.next == null && p.prev != p)
     *
     * K.
     * The deque object has two node references, "head" and "tail".
     * The head and tail are only approximations to the first and last
     * nodes of the deque.  The first node can always be found by
     * following prev pointers from head; likewise for tail.  However,
     * it is permissible for head and tail to be referring to deleted
     * nodes that have been unlinked and so may not be reachable from
     * any live node.
     *
     * L.
     * There are 3 stages of node deletion;
     * "logical deletion", "unlinking", and "gc-unlinking".
     *
     * 1. "logical deletion" by CASing item to null atomically removes
     * the element from the collection, and makes the containing node
     * eligible for unlinking.
     *
     * 2. "unlinking" makes a deleted node unreachable from active
     * nodes, and thus eventually reclaimable by GC.  Unlinked nodes
     * may remain reachable indefinitely from an iterator.
     *
     * Physical node unlinking is merely an optimization (albeit a
     * critical one), and so can be performed at our convenience.  At
     * any time, the set of live nodes maintained by prev and next
     * links are identical, that is, the live nodes found via next
     * links from the first node is equal to the elements found via
     * prev links from the last node.  However, this is not true for
     * nodes that have already been logically deleted - such nodes may
     * be reachable in one direction only.
     *
     * 3. "gc-unlinking" takes unlinking further by making active
     * nodes unreachable from deleted nodes, making it easier for the
     * GC to reclaim future deleted nodes.  This step makes the data
     * structure "gc-robust", as first described in detail by Boehm
     * (http://portal.acm.org/citation.cfm?doid=503272.503282).
     *
     * M.
     * GC-unlinked nodes may remain reachable indefinitely from an
     * iterator, but unlike unlinked nodes, are never reachable from
     * head or tail.
     *
     * N.
     * Making the data structure GC-robust will eliminate the risk of
     * unbounded memory retention with conservative GCs and is likely
     * to improve performance with generational GCs.
     *
     * O.
     * When a node is dequeued at either end, e.g. via poll(), we would
     * like to break any references from the node to active nodes.  We
     * develop further the use of self-links that was very effective in
     * other concurrent collection classes.  The idea is to replace
     * prev and next pointers with special values that are interpreted
     * to mean off-the-list-at-one-end.  These are approximations, but
     * good enough to preserve the properties we want in our
     * traversals, e.g. we guarantee that a traversal will never visit
     * the same element twice, but we don't guarantee whether a
     * traversal that runs out of elements will be able to see more
     * elements later after enqueues at that end.  Doing gc-unlinking
     * safely is particularly tricky, since any node can be in use
     * indefinitely (for example by an iterator).  We must ensure that
     * the nodes pointed at by head/tail never get gc-unlinked, since
     * head/tail are needed to get "back on track" by other nodes that
     * are gc-unlinked.  gc-unlinking accounts for much of the
     * implementation complexity.
     *
     * P.
     * Since neither unlinking nor gc-unlinking are necessary for
     * correctness, there are many implementation choices regarding
     * frequency (eagerness) of these operations.  Since volatile
     * reads are likely to be much cheaper than CASes, saving CASes by
     * unlinking multiple adjacent nodes at a time may be a win.
     * gc-unlinking can be performed rarely and still be effective,
     * since it is most important that long chains of deleted nodes
     * are occasionally broken.
     *
     * Q.
     * The actual representation we use is that p.next == p means to
     * goto the first node (which in turn is reached by following prev
     * pointers from head), and p.next == null && p.prev == p means
     * that the iteration is at an end and that p is a (static final)
     * dummy node, NEXT_TERMINATOR, and not the last active node.
     * Finishing the iteration when encountering such a TERMINATOR is
     * good enough for read-only traversals, so such traversals can use
     * p.next == null as the termination condition.  When we need to
     * find the last (active) node, for enqueueing a new node, we need
     * to check whether we have reached a TERMINATOR node; if so,
     * restart traversal from tail.
     *
     * R.
     * The implementation is completely directionally symmetrical,
     * except that most public methods that iterate through the list
     * follow next pointers ("forward" direction).
     *
     * S.
     * We believe (without full proof) that all single-element deque
     * operations (e.g., addFirst, peekLast, pollLast) are linearizable
     * (see Herlihy and Shavit's book).  However, some combinations of
     * operations are known not to be linearizable.  In particular,
     * when an addFirst(A) is racing with pollFirst() removing B, it is
     * possible for an observer iterating over the elements to observe
     * A B C and subsequently observe A C, even though no interior
     * removes are ever performed.  Nevertheless, iterators behave
     * reasonably, providing the "weakly consistent" guarantees.
     *
     * T.
     * Empirically, microbenchmarks suggest that this class adds about
     * 40% overhead relative to ConcurrentLinkedQueue, which feels as
     * good as we can hope for.
     */

    private static final long serialVersionUID = 876323262645176354L;

    /**
     * 20210529
     * A. 可以在O（1）时间内到达列表上的第一个节点（即具有p.prev == null和＆p.next！= p的唯一节点p）的节点。
     * B. 不变性：
     *      1) 第一个节点总是 O(1) 可从头通过 prev 链接到达。
     *      2) 所有活动节点都可以通过 succ() 从第一个节点到达。
     *      3) head != null
     *      4) (tmp = head).next != tmp || tmp != head  => 即head的next结点永远不会指向自己
     *      5) head 永远不会被 gc-unlinked（但可能会被取消链接）。
     * C. 可变性：
     *      1) head.item 可能是也可能不是 null。
     *      2) head 可能无法从第一个或最后一个节点或从 tail 访问。
     */
    /**
     * A.
     * A node from which the first node on list (that is, the unique node p
     * with p.prev == null && p.next != p) can be reached in O(1) time.
     *
     * B.
     * Invariants:
     * - the first node is always O(1) reachable from head via prev links
     * - all live nodes are reachable from the first node via succ()
     * - head != null
     * - (tmp = head).next != tmp || tmp != head
     * - head is never gc-unlinked (but may be unlinked)
     *
     * C.
     * Non-invariants:
     * - head.item may or may not be null
     * - head may not be reachable from the first or last node, or from tail
     */
    private transient volatile Node<E> head;

    /**
     * 20210529
     * A. 可以在 O(1) 时间内到达列表中最后一个节点（即具有 p.next == null && p.prev != p 的唯一节点 p）的节点。
     * B. 不变性：
     *      1) 最后一个节点总是可以通过next链接从尾部到达 O(1)。
     *      2) 所有活动节点都可以通过 pred() 从最后一个节点到达。
     *      3) tail != null
     *      4) tail 永远不会被 gc 取消链接（但可能会取消链接）。
     * C. 可变性：
     *      1) tail.item 可能是也可能不是 null。
     *      2) tail 可能无法从第一个或最后一个节点或从 head 到达。
     */
    /**
     * A.
     * A node from which the last node on list (that is, the unique node p
     * with p.next == null && p.prev != p) can be reached in O(1) time.
     *
     * B.
     * Invariants:
     * - the last node is always O(1) reachable from tail via next links
     * - all live nodes are reachable from the last node via pred()
     * - tail != null
     * - tail is never gc-unlinked (but may be unlinked)
     *
     * C.
     * Non-invariants:
     * - tail.item may or may not be null
     * - tail may not be reachable from the first or last node, or from head
     */
    private transient volatile Node<E> tail;

    // PREV_TERMINATOR, prev终止结点: 队头结点p出队时, 会把p.next=p, p.prev=PREV_TERMINATOR
    // NEXT_TERMINATOR, next终止结点: 队尾结点p出队时, 会把p.prev=p, p.next=NEXT_TERMINATOR
    private static final Node<Object> PREV_TERMINATOR, NEXT_TERMINATOR;

    @SuppressWarnings("unchecked")
    Node<E> prevTerminator() {
        return (Node<E>) PREV_TERMINATOR;
    }

    @SuppressWarnings("unchecked")
    Node<E> nextTerminator() {
        return (Node<E>) NEXT_TERMINATOR;
    }

    static final class Node<E> {
        volatile Node<E> prev;
        volatile E item;
        volatile Node<E> next;

        Node() {  // default constructor for NEXT_TERMINATOR, PREV_TERMINATOR
        }

        /**
         * Constructs a new node.  Uses relaxed write because item can
         * only be seen after publication via casNext or casPrev.
         */
        Node(E item) {
            UNSAFE.putObject(this, itemOffset, item);
        }

        boolean casItem(E cmp, E val) {
            return UNSAFE.compareAndSwapObject(this, itemOffset, cmp, val);
        }

        void lazySetNext(Node<E> val) {
            UNSAFE.putOrderedObject(this, nextOffset, val);
        }

        boolean casNext(Node<E> cmp, Node<E> val) {
            return UNSAFE.compareAndSwapObject(this, nextOffset, cmp, val);
        }

        void lazySetPrev(Node<E> val) {
            UNSAFE.putOrderedObject(this, prevOffset, val);
        }

        boolean casPrev(Node<E> cmp, Node<E> val) {
            return UNSAFE.compareAndSwapObject(this, prevOffset, cmp, val);
        }

        // Unsafe mechanics

        private static final sun.misc.Unsafe UNSAFE;
        private static final long prevOffset;
        private static final long itemOffset;
        private static final long nextOffset;

        static {
            try {
                UNSAFE = sun.misc.Unsafe.getUnsafe();
                Class<?> k = Node.class;
                prevOffset = UNSAFE.objectFieldOffset
                    (k.getDeclaredField("prev"));
                itemOffset = UNSAFE.objectFieldOffset
                    (k.getDeclaredField("item"));
                nextOffset = UNSAFE.objectFieldOffset
                    (k.getDeclaredField("next"));
            } catch (Exception e) {
                throw new Error(e);
            }
        }
    }

    /**
     * Links e as first element.
     */
    private void linkFirst(E e) {
        checkNotNull(e);
        final Node<E> newNode = new Node<E>(e);

        restartFromHead:
        for (;;)
            // 1. h = p = head, 为head指针
            for (Node<E> h = head, p = h, q;;) {
                // 2. 如果p的前驱不为null, 且p的前驱的前驱也不为null, 相当于p、q都左移一个结点,
                //    这里巧妙的地方在于, 如果p前驱为null了就不会再找前驱的前驱了, 而如果前驱不为null还会去更新p为p的前驱, 从而保证了p一定视为第一个结点, 而q永远只代表p的前驱
                if ((q = p.prev) != null &&
                    (q = (p = q).prev) != null)
                    // Check for head updates every other hop.  // 每隔一跳检查头部更新。
                    // If p == q, we are sure to follow head instead.   // 如果 p == q，我们肯定会跟随 head。

                    // 3. 如果此时还不为空, 说明前面还有结点, 则判断h是否为最新的head指针, 如果不是, 则说明head指针被更新过了, 设置p为最新的head指针继续自旋;
                    //    如果是, 则说明head指针还没更新, 说明了别的线程插入了两结点, 但最后的线程还没更新head指针, 此时设置p为q即p的前驱, 继续自旋, 等待插入或者继续往前找
                    p = (h != (h = head)) ? h : q;

                // 2. 如果p的前驱为null, 且p的后继为它本身, 说明p结点已经被删除了, 需要重新获取head指针, 此时重新进入自旋
                else if (p.next == p) // PREV_TERMINATOR
                    continue restartFromHead;

                // 2. 如果p的前驱为null, 且p还没有被删除, 说明p是第一个结点
                else {
                    // 3. 此时把当前结点CAS插入到p前面
                    // p is first node
                    newNode.lazySetNext(p); // CAS piggyback

                    // 4. 然后CAS设置p的前驱(写死为null)为当前结点
                    if (p.casPrev(null, newNode)) {
                        // 成功的 CAS 是 e 成为这个双端队列的一个元素，以及 newNode 成为“活”的线性化点。
                        // Successful CAS is the linearization point
                        // for e to become an element of this deque,
                        // and for newNode to become "live".

                        // 5. 如果CAS设置前驱成功, 如果p!=h, 说明当前线程需要移动head指针, 此时移动head指针为当前结点
                        if (p != h) // hop two nodes at a time  // 一次跳两个节点
                            casHead(h, newNode);  // Failure is OK. // 失败是可以的。

                        // 5. 如果CAS设置前驱成功, 则直接返回, 移动head指针交由下一个线程完成
                        return;
                    }

                    // 失去了到另一个线程的 CAST 竞赛； 重读上一篇
                    // Lost CAS race to another thread; re-read prev
                }
            }
    }

    /**
     * Links e as last element.
     */
    private void linkLast(E e) {
        checkNotNull(e);
        final Node<E> newNode = new Node<E>(e);

        restartFromTail:
        for (;;)
            // 1. h = p = head, 为tail指针
            for (Node<E> t = tail, p = t, q;;) {
                // 2. 如果p的后继不为null, 且p的后继的后继也不为null, 相当于p和q都右移了一个结点
                //    这里巧妙的地方在于, 如果p后继为null了就不会再找后继的后继了, 而如果后继不为null还会去更新p为p的后继, 从而保证了p一定视为最后结点, 而q永远只代表p的后继
                if ((q = p.next) != null &&
                    (q = (p = q).next) != null)
                    // Check for tail updates every other hop.  // 每隔一跳检查尾部更新。
                    // If p == q, we are sure to follow tail instead.   // 如果 p == q，我们肯定会跟随tail。

                    // 3. 如果此时还不为空, 说明后面还有结点, 再判断t是否为最新的tail指针, 如果不是, 则更新t为最新的tail指针, 继续自旋
                    //    如果是, 则说明tail指针还没更新, 说明有两个线程插入了结点, 但一个线程还没更新tail指针, 此时设置p为p的后继, 等待插入或者继续往后找
                    p = (t != (t = tail)) ? t : q;

                // 2. 如果p的后继为null, 且p的前驱为它本身, 说明p被删除了, 此时需要获取最新的tail指针, 所以重新进入自旋
                else if (p.prev == p) // NEXT_TERMINATOR
                    continue restartFromTail;

                // 2. 如果p的后继为null, 且p还没有被删除, 说明p是最后一个结点
                else {
                    // 3. 此时把当前结点CAS插入到p后面
                    // p is last node
                    newNode.lazySetPrev(p); // CAS piggyback

                    // 4. 然后CAS设置p的后继(写死为null)为当前结点
                    if (p.casNext(null, newNode)) {
                        // Successful CAS is the linearization point
                        // for e to become an element of this deque,
                        // and for newNode to become "live".

                        // 5. 如果CAS设置后继成功, 如果p!=h, 说明当前线程需要移动tail指针, 此时移动tail指针为当前结点
                        if (p != t) // hop two nodes at a time
                            casTail(t, newNode);  // Failure is OK.

                        // 5. 如果CAS设置后继成功, 则直接返回, 移动tail指针交由下一个线程完成
                        return;
                    }
                    // Lost CAS race to another thread; re-read next
                }
            }
    }

    private static final int HOPS = 2;

    /**
     * Unlinks non-null node x.
     */
    // 解除指定活动结点的链接
    void unlink(Node<E> x) {
        // assert x != null;
        // assert x.item == null;
        // assert x != PREV_TERMINATOR;
        // assert x != NEXT_TERMINATOR;

        final Node<E> prev = x.prev;
        final Node<E> next = x.next;

        // 1. 如果当前结点的前驱为null, 说明x为第一个活动结点, 则unlink x, 且x是已经被标记为逻辑删除的结点(logical deletion),
        //    只有在第一次并发同端删除才会进入, 且first=head并不会被gc-unlink, 但会被unlink
        if (prev == null) {
            unlinkFirst(x, next);
        }

        // 2. 如果当期结点的后继为null, 说明x为最后一个活动结点, 则unlink X, 且X是已经被标记为逻辑删除的结点(logical deletion),
        //    只有在第一次并发同端删除才会进入, 且last=tail并不会被gc-unlink, 但会被unlink
        else if (next == null) {
            unlinkLast(x, prev);
        }

        // 3. 如果当前结点的前驱和后继都不为null, 说明x既不是第一个也不是最后一个活动结点, 非第一次同端删除都属于内部结点的删除, 用于删除中间标记了逻辑删除但为gc-unlink的o结点们
        else {
            /**
             * 20210529
             * A. 取消链接内部节点。
             * B. 这是常见的情况，因为在同一端的一系列轮询将是“内部”删除，除了第一个，因为端节点无法断开链接。
             * C. 在任何时候，所有活动节点都可以通过遵循 next 或 prev 指针序列相互访问。
             * D. 我们的策略是找到 x 唯一的活动前身和后继者。 尝试修复它们的链接，使它们相互指向，使 x 无法从活动节点访问。
             *    如果成功，并且如果 x 没有活动的前驱/后继，我们另外尝试 gc-unlink，通过重新检查前驱和后继的状态未改变并确保 x 无法从尾部/头部到达，
             *    使活动节点无法从 x 到达 , 在将 x 的上一个/下一个链接设置为它们的逻辑近似替换之前，self/TERMINATOR。
             */
            // A.
            // Unlink interior node.
            //
            // B.
            // This is the common case, since a series of polls at the
            // same end will be "interior" removes, except perhaps for
            // the first one, since end nodes cannot be unlinked.
            //
            // C.
            // At any time, all active nodes are mutually reachable by
            // following a sequence of either next or prev pointers.
            //
            // D.
            // Our strategy is to find the unique active predecessor
            // and successor of x.  Try to fix up their links so that
            // they point to each other, leaving x unreachable from
            // active nodes.  If successful, and if x has no live
            // predecessor/successor, we additionally try to gc-unlink,
            // leaving active nodes unreachable from x, by rechecking
            // that the status of predecessor and successor are
            // unchanged and ensuring that x is not reachable from
            // tail/head, before setting x's prev/next links to their
            // logical approximate replacements, self/TERMINATOR.

            // 4. activePred为x前第一个活动的前驱, activeSucc为x后第一个活动的后继, isFist代表x前是否存在待删除的first结点, isLast代表x后是否存在待删除的last结点
            //    x是已经被标记为逻辑删除的结点(logical deletion)
            Node<E> activePred, activeSucc;
            boolean isFirst, isLast;
            int hops = 1;

            // Find active predecessor

            // 查找x前第一个活动的前驱
            for (Node<E> p = prev; ; ++hops) {
                // 5. 如果x的前驱p的item不为null, 则说明p为x前第一个活动的前驱, 此时x前不存在待删除的first结点, 所以isFirst为ifasle
                if (p.item != null) {
                    activePred = p;
                    isFirst = false;
                    break;
                }

                // 5. 如果x的前驱p的item为null, 说明p是一个活动的结点, 但也是一个待删除的结点, 则再获取p的前驱q
                Node<E> q = p.prev;

                // 6. 如果q为null, 说明p为第一个活动的结点
                if (q == null) {
                    // 7. 但p.next=p, 说明p为PREV_TERMINATOR, 因为找不到一个x的活动的前驱结点, 说明还没经过第一个同端删除, 这时直接返回即可, 把unlink x看作为第一个的同端删除
                    if (p.next == p)
                        return;

                    // 7. 如果p不是PREV_TERMINATOR, 则说明找到了第一个活动结点也是待删除的结点, 此时设置activePred为p, 所以isFirst为true, 退出循环
                    activePred = p;
                    isFirst = true;
                    break;
                }

                // 6. 如果q不为null, 说明q前还有结点, 但p==q, 即p=p.prev, 说明p为NEXT_TERMINATOR, 其实不可能发生
                else if (p == q)
                    return;

                // 6. 如果q不为null, 说明q前还有结点, 也不为NEXT_TERMINATOR, 此时p往前移动一个结点, hops记录经过的结点数, 此后就是2了
                else
                    p = q;
            }

            // Find active successor

            // 查找x前第一个活动的后继
            for (Node<E> p = next; ; ++hops) {
                // 8. 如果x的后继p的item不为null, 则说明p为x前第一个活动的后继, 此时x前不存在待删除的last结点, 所以isLast为ifasle
                if (p.item != null) {
                    activeSucc = p;
                    isLast = false;
                    break;
                }

                // 8. 如果x的后继p的item为null, 说明p并不是一个活动的结点, 则再获取p的后继q
                Node<E> q = p.next;

                // 10. 如果q为null, 说明p为最后一个活动结点
                if (q == null) {
                    // 11. 但p.prev=p, 说明p为NEXT_TERMINATOR, 因为找不到一个x的活动的后继结点, 说明还没经过第一个同端删除, 这时直接返回即可, 把unlink x看作为第一个的同端删除
                    if (p.prev == p)
                        return;

                    // 11. 如果p为活动结点, 则说明找到了最后一个活动结点也是待删除的结点, 此时设置activeSucc为p, 所以isLast为true, 退出循环
                    activeSucc = p;
                    isLast = true;
                    break;
                }

                // 10. 如果q不为null, 说明q后还有结点, 但p==q, 即p=p.next, 说明p为PREV_TERMINATOR, 其实不可能发生
                else if (p == q)
                    return;

                // 10. 如果q不为null, 说明q后还有结点, 也不为PREV_TERMINATOR, 此时p往后移动一个结点, hops记录经过的结点数, 此后就是3了
                else
                    p = q;
            }

            // 12. 如果中间跳过的结点数小于2, 则退出, 如果x前不存在待删除结点, 则也会退出
            // TODO: better HOP heuristics
            if (hops < HOPS
                // 总是挤出内部删除的节点
                // always squeeze out interior deleted nodes
                && (isFirst | isLast))
                return;

            // 挤出activePred和activeSucc之间删除的节点，包括x。
            // Squeeze out deleted nodes between activePred and
            // activeSucc, including x.

            // 13. activePred往后跳过已经逻辑删除的后继结点, 使得activePred的next一定是个活动结点
            skipDeletedSuccessors(activePred);

            // 14. activeSucc往前跳过已经逻辑删除的前驱结点, 使得activeSucc的prev一定是个活动结点
            skipDeletedPredecessors(activeSucc);

            // Try to gc-unlink, if possible

            // 15. 如果x前存在待删除结点, 且activePred和activeSucc保持链接,
            //     如果存在待删除的first结点, 则判断activePred是否为第一个结点, 否则判断activePred是否为存活结点
            //     如果存在待删除的last结点, 则判断activeSucc是否为最后一个结点, 否则判断activeSucc是否为存活结点
            //     说明x确实在activePred与activeSucc的中间, 且activePred和activeSucc都是合法的
            if ((isFirst | isLast) &&

                // Recheck expected state of predecessor and successor
                (activePred.next == activeSucc) &&
                (activeSucc.prev == activePred) &&
                (isFirst ? activePred.prev == null : activePred.item != null) &&
                (isLast  ? activeSucc.next == null : activeSucc.item != null)) {

                // 17. 通过当前head指针, 去向前找到第一个活动结点, 并设置为最新的head指针, 这里保证了head一定是activePred或者activePred前面, 从而确保x在中间
                updateHead(); // Ensure x is not reachable from head

                // 18. 通过当前tail指针, 去向后找到最后一个活动结点, 并设置为最新的tail指针, 这里保证了head一定是activeSucc或者activeSucc后面, 从而确保x在中间
                updateTail(); // Ensure x is not reachable from tail

                // Finally, actually gc-unlink

                // 19. gc-unlink: 如果存在待删除的first结点, 则x.prev链接PREV_TERMINATOR, 否则自链接自身结点x
                x.lazySetPrev(isFirst ? prevTerminator() : x);

                // 20. gc-unlink: 如果存在待删除的last结点, 则x.next链接NEXT_TERMINATOR, 否则自链接自身结点x
                x.lazySetNext(isLast  ? nextTerminator() : x);

                // first和last结点会不清理, 或者留到下次清理
            }
        }
    }

    /**
     * 20210529
     * unlink第一个非null结点, 且该结点是已经被标记为逻辑删除的结点(logical deletion), 只有在第一次并发同端删除才会进入, 且first=head并不会被gc-unlink, 但会被unlink
     */
    /**
     * Unlinks non-null first node.
     */
    private void unlinkFirst(Node<E> first, Node<E> next) {
        // assert first != null;
        // assert next != null;
        // assert first.item == null;

        // 1. first为当前要解除链接的结点, p为解除结点的next结点
        for (Node<E> o = null, p = next, q;;) {

            // 2. 如果p的item不为null, 说明p为活动结点, 或者p的next结点为null, 说明p为尾结点了
            if (p.item != null || (q = p.next) == null) {

                // 3. 此时p为活动结点或者为尾结点, 如果o不为null, 说明p经过了移动, 且p的前驱不是它自己, 说明新的p还没有被gc-unlink, 即p是first后的第一个活动结点,
                //    此时设置待删除的结点first的next为p, 保证只能从first结点才能访问活动结点,
                //    => 如果下面还将p的prev设置为first, 那么无论是从head还是tail, 都无法访问first与p之间的待删除结点了, 这就是unlink的第一步
                if (o != null && p.prev != p && first.casNext(next, p)) {

                    // 4. 如果first.next设置成功了, 则p往前跳过已经逻辑删除的前驱结点, 使得p的prev为first结点， 这步属于unlink的第二步
                    //    => unlink后, 中间的逻辑删除结点o就被first和p圈起来了, first的next为p, p的prev为frist, 尽管o还可以访问first和p,
                    //       但由于o没有了引用, GC最后都会发现它们
                    skipDeletedPredecessors(p);

                    // 5. 如果first的前驱为null, 说明first为第一个结点, 且p的next为null或者p的item不为null, 说明p是个活动结点
                    //    且p的前驱为first, 说明p与first的链接还是正常的
                    if (first.prev == null &&
                        (p.next == null || p.item != null) &&
                        p.prev == first) {

                        // 6. 通过当前head指针, 去向前找到第一个活动结点, 并设置为最新的head指针, 这里保证了head一定是first或者first前面, 从而确保o在中间
                        updateHead(); // Ensure o is not reachable from head    // 确保 o 无法从头部到达, o为上一个线程留下的

                        // 7. 通过当前tail指针, 去向后找到最后一个活动结点, 并设置为最新的tail指针, 这里保证了tail一定是p或者p后面, 从而确保o在中间
                        updateTail(); // Ensure o is not reachable from tail    // 确保 o 无法从尾部到达, o为上一个线程留下的

                        // Finally, actually gc-unlink
                        // 8. gc-unlink: 自链接中间的o结点, 设置o的前驱为PREV_TERMINATOR, 即将o进行最后一步删除
                        o.lazySetNext(o);
                        o.lazySetPrev(prevTerminator());
                    }
                }

                // 3. 如果p为活动结点或者为尾结点, 如果o为null, 说明first到p之间不存在逻辑删除的结点, 直接返回即可,
                //    由于first标记为了逻辑删除, 下一个线程再poll时, 会把该first结点gc-unlink(自链接)掉的
                return;
            }

            // 2. 如果p为非活动结点、也不为尾结点, 且p=q, 即p=p.next, 说明p已经被gc-unlink了, 则直接返回,
            //    由于first标记为了逻辑删除, 下一个线程再poll时, 会把该first结点gc-unlink(自链接)掉的
            else if (p == q)
                return;

            // 3. 如果p为非活动结点、也不为尾结点、也还没有被删除, 说明first到p之间存在逻辑删除的结点, O只会有一个
            else {
                o = p;
                p = q;
            }
        }
    }

    /**
     * 20210529
     * unlink最后一个非null结点, 且该结点是已经被标记为逻辑删除的结点(logical deletion), 只有在第一次并发同端删除才会进入, 且last=tail并不会被gc-unlink, 但会被unlink
     */
    /**
     * Unlinks non-null last node.
     */
    private void unlinkLast(Node<E> last, Node<E> prev) {
        // assert last != null;
        // assert prev != null;
        // assert last.item == null;

        // 1. last为当前要解除链接的结点, p为解除结点的prev结点
        for (Node<E> o = null, p = prev, q;;) {

            // 2. 如果p的item不为null, 说明p为活动结点, 或者p的prev结点为null, 说明p为头结点了
            if (p.item != null || (q = p.prev) == null) {

                // 3. 此时p为活动结点或者为头结点, 如果o不为null, 说明p经过了移动, 且p的后继不是它自己, 说明新的p还没有被gc-unlink, 即p是last前的第一个活动结点,
                //    此时设置待删除的结点last的prev为p, 保证只能从last结点才能访问活动结点,
                //    => 如果下面还将p的next设置为last, 那么无论是从head还是tail, 都无法访问last与p之间的待删除结点了, 这就是unlink的第一步
                if (o != null && p.next != p && last.casPrev(prev, p)) {

                    // 4. 如果last.prev设置成功了, 则p往后跳过已经逻辑删除的后继结点, 使得p的next为last结点， 这步属于unlink的第二步
                    //    => unlink后, 中间的逻辑删除结点o就被last和p圈起来了, last的prev为p, p的next为last, 尽管o还可以访问last和p,
                    //       但由于o没有了引用, GC最后都会发现它们
                    skipDeletedSuccessors(p);

                    // 5. 如果last的后继为null, 说明last为最后一个结点, 且p的prev为null或者p的item不为null, 说明p是个活动结点
                    //    且p的后继为last, 说明p与last的链接还是正常的
                    if (last.next == null &&
                        (p.prev == null || p.item != null) &&
                        p.next == last) {

                        // 6. 通过当前head指针, 去向前找到第一个活动结点, 并设置为最新的head指针, 这里保证了head一定是p或者p前面, 从而确保o在中间
                        updateHead(); // Ensure o is not reachable from head    // 确保 o 无法从头部到达, o为上一个线程留下的

                        // 7. 通过当前tail指针, 去向后找到最后一个活动结点, 并设置为最新的tail指针, 这里保证了tail一定是last或者last后面, 从而确保o在中间
                        updateTail(); // Ensure o is not reachable from tail    // 确保 o 无法从尾部到达, o为上一个线程留下的

                        // Finally, actually gc-unlink
                        // 8. gc-unlink: 自链接中间的o结点, 设置o的后继为NEXT_TERMINATOR, 即将o进行最后一步删除
                        o.lazySetPrev(o);
                        o.lazySetNext(nextTerminator());
                    }
                }

                // 3. 如果p为活动结点或者为头结点, 如果o为null, 说明last到p之间不存在逻辑删除的结点, 直接返回即可,
                //    由于last标记为了逻辑删除, 下一个线程再poll时, 会把该last结点gc-unlink(自链接)掉的
                return;
            }

            // 2. 如果p为非活动结点、也不为头结点, 且p=q, 即p=p.prev, 说明p已经被gc-unlink了, 则直接返回,
            //    由于last标记为了逻辑删除, 下一个线程再poll时, 会把该last结点gc-unlink(自链接)掉的
            else if (p == q)
                return;

            // 3. 如果p为非活动结点、也不为头结点、也还没有被删除, 说明last到p之间存在逻辑删除的结点, O只会有一个
            else {
                o = p;
                p = q;
            }
        }
    }

    /**
     * 20210529
     * 保证在调用此方法之前取消链接的任何节点在返回后都将无法从 head 访问。 不保证消除 slack，只有 head 会指向在此方法运行时处于活动状态的节点。
     */
    /**
     * Guarantees that any node which was unlinked before a call to
     * this method will be unreachable from head after it returns.
     * Does not guarantee to eliminate slack, only that head will
     * point to a node that was active while this method was running.
     */
    // 通过当前head指针, 去向前找到第一个活动结点, 并设置为最新的head指针
    private final void updateHead() {
        // 要么 head 已经指向一个活动节点，要么我们一直尝试将其 cas 到第一个节点，直到它指向。
        // Either head already points to an active node, or we keep
        // trying to cas it to the first node until it does.
        Node<E> h, p, q;
        restartFromHead:

        // 1. h为head指针, 如果item为null, 说明当前head不是活动的结点, 需要移动head指针, p为head的前驱, 如果前驱不为null, 说明head前面还有活动的结点
        while ((h = head).item == null && (p = h.prev) != null) {
            for (;;) {
                // 2. p、q前移一个结点, 永远保证p为第一个活动结点, q为p的前驱
                if ((q = p.prev) == null ||
                    (q = (p = q).prev) == null) {
                    // It is possible that p is PREV_TERMINATOR,
                    // but if so, the CAS is guaranteed to fail.

                    // 3. 如果p的前驱为null, 说明p为第一个活动节点了, 此时CAS设置h为p, 设置成功说明h没移动过, p确实是头结点, 则返回
                    if (casHead(h, p))
                        return;

                    // 3. CAS设置失败, 说明p有前移过, 即有插入过结点, h发生过移动, 此时重新进入restartFromHead自旋, 获取最新的head指针
                    else
                        continue restartFromHead;
                }

                // 2. 如果p的前驱不为null, 且h不为最新的head结点, 此时重新进入restartFromHead自旋, 获取最新的head指针
                else if (h != head)
                    continue restartFromHead;

                // 2. 如果p的前驱不为null, 且h为最新的head结点, 则p继续前移, 继续自旋, 找出第一个活动结点
                else
                    p = q;
            }
        }
    }

    /**
     * 20210529
     * 保证在调用此方法之前取消链接的任何节点在返回后都将无法从尾部访问。 不保证消除松弛，只有尾部将指向在此方法运行时处于活动状态的节点。
     */
    /**
     * Guarantees that any node which was unlinked before a call to
     * this method will be unreachable from tail after it returns.
     * Does not guarantee to eliminate slack, only that tail will
     * point to a node that was active while this method was running.
     */
    // 通过当前tail指针, 去向后找到最后一个活动结点, 并设置为最新的tail指针
    private final void updateTail() {
        // 要么 tail 已经指向一个活动节点，要么我们一直尝试将其 cas 到最后一个节点，直到它指向。
        // Either tail already points to an active node, or we keep
        // trying to cas it to the last node until it does.
        Node<E> t, p, q;
        restartFromTail:

        // 1. t为tail指针, 如果item为null, 说明当前tail不是活动的结点, 需要移动tail指针, p为tail的后继, 如果后继不为null, 说明tail后面还有活动的结点
        while ((t = tail).item == null && (p = t.next) != null) {
            for (;;) {
                // 2. p、q后移一个结点, 永远保证p为最后一个活动结点, q为p的后继
                if ((q = p.next) == null ||
                    (q = (p = q).next) == null) {
                    // It is possible that p is NEXT_TERMINATOR,
                    // but if so, the CAS is guaranteed to fail.

                    // 3. 如果p的后继为null, 说明p为最后一个活动节点了, 此时CAS设置t为p, 设置成功说明t没移动过, p确实是最后一个结点, 则返回
                    if (casTail(t, p))
                        return;

                    // 3. CAS设置失败, 说明p有后移过, 即有插入过结点, t发生过移动, 此时重新进入restartFromHead自旋, 获取最新的head指针
                    else
                        continue restartFromTail;
                }

                // 2. 如果p的后继不为null, 且t不为最新的tail结点, 此时重新进入restartFromTail自旋, 获取最新的tail指针
                else if (t != tail)
                    continue restartFromTail;

                // 2. 如果p的后继不为null, 且t为最新的tail结点, 则p继续后移, 继续自旋, 找出最后一个活动结点
                else
                    p = q;
            }
        }
    }

    // x往前跳过已经逻辑删除的前驱结点, 使得x的prev一定是个活动结点
    private void skipDeletedPredecessors(Node<E> x) {
        whileActive:
        do {
            // x为有效的结点, prev、p为x的前驱
            Node<E> prev = x.prev;
            // assert prev != null;
            // assert x != NEXT_TERMINATOR;
            // assert x != PREV_TERMINATOR;
            Node<E> p = prev;

            findActive:
            for (;;) {
                // 2. 如果p的item不为null, 说明前驱p是活动结点, 此时退出findActive自旋, 因为找到了活动的前驱结点p
                if (p.item != null)
                    break findActive;

                // 3. 如果p的item是null, 说明前驱p是非活动结点, 此时需要再判断p的前驱
                Node<E> q = p.prev;

                // 4. 此时如果p的前驱为null, 说明p已经是第一个结点了
                if (q == null) {
                    // 5. 如果p的前驱为null, 但p.next=p, 说明p并不是第一个结点, 只是为PREV_TERMINATOR, 则需要继续whileActive自旋, 重新获取x的前驱
                    if (p.next == p)
                        continue whileActive;

                    // 5. 否则说明p就是要找的有效x前驱结点
                    break findActive;
                }

                // 4. 如果p的前驱不为null, 且p=q, 即p=p.prev, 说明p为NEXT_TERMINATOR, 则继续whileActive自旋, 重新获取x的有效前驱
                else if (p == q)
                    continue whileActive;

                // 4. 如果p的前驱不为null, 且p还没被gc-link, 则把p向前移动一个结点, 继续findActive自旋, 直到找出有效的x前驱结点
                else
                    p = q;
            }

            // 3. 退出了findActive自旋来到这里, 说明x的有效前驱结点已经找到了, 如果p还是x之前的前驱, 说明p没有变过; 如果p向前移动过了, 则设置x的前驱为最新的p
            // found active CAS target
            if (prev == p || x.casPrev(prev, p))
                return;

        } while (x.item != null || x.next == null);// 如果x的item不为null, 说明x为有效的结点, 或者x的next不为null, 说明x为尾结点, 即x为合法链表里的结点
    }

    // x往后跳过已经逻辑删除的后继结点, 使得x的next一定是个活动结点
    private void skipDeletedSuccessors(Node<E> x) {
        whileActive:
        do {
            // x为有效的结点, next、p为x的后继
            Node<E> next = x.next;
            // assert next != null;
            // assert x != NEXT_TERMINATOR;
            // assert x != PREV_TERMINATOR;
            Node<E> p = next;
            findActive:
            for (;;) {
                // 2. 如果p的item不为null, 说明后继p是活动结点, 此时退出findActive自旋, 因为找到了活动的后继结点p
                if (p.item != null)
                    break findActive;

                // 3. 如果p的item是null, 说明后继p是非活动结点, 此时需要再判断p的后继
                Node<E> q = p.next;

                // 4. 此时如果p的后继为null, 说明p已经是最后一个结点了
                if (q == null) {
                    // 5. 如果p的后继为null, 但p.prev=p, 说明p并不是最后一个结点, 只是为NEXT_TERMINATOR, 则需要继续whileActive自旋, 重新获取x的后继
                    if (p.prev == p)
                        continue whileActive;

                    // 5. 否则说明p就是要找的有效x后继结点
                    break findActive;
                }

                // 4. 如果p的后继不为null, 且p=q, 即p=p.next, 说明p为PREV_TERMINATOR, 则继续whileActive自旋, 重新获取x的有效后继
                else if (p == q)
                    continue whileActive;

                // 4. 如果p的后继不为null, 且p还没被gc-link, 则把p向前移动一个结点, 继续findActive自旋, 直到找出有效的x后继结点
                else
                    p = q;
            }

            // 3. 退出了findActive自旋来到这里, 说明x的有效后继结点已经找到了, 如果p还是x之前的后继, 说明p没有变过; 如果p向前移动过了, 则设置x的后继为最新的p
            // found active CAS target
            if (next == p || x.casNext(next, p))
                return;

        } while (x.item != null || x.prev == null);// 如果x的item不为null, 说明x为有效的结点, 或者x的prev不为null, 说明x为头结点, 即x为合法链表里的结点
    }

    /**
     * 20210529
     * 返回 p 的后继节点，或者如果 p.next 已链接到 self ，则返回第一个节点，只有在使用现在不在列表中的陈旧指针遍历时才会为真。
     */
    /**
     * Returns the successor of p, or the first node if p.next has been
     * linked to self, which will only be true if traversing with a
     * stale pointer that is now off the list.
     */
    // 返回p的后继结点, 如果碰到自链接, 则返回第一个不能null的结点, 从头再来
    final Node<E> succ(Node<E> p) {
        // TODO: should we skip deleted nodes here?
        Node<E> q = p.next;
        return (p == q) ? first() : q;
    }

    /**
     * 20210529
     * 返回 p 的前导，或者如果 p.prev 已链接到 self 则返回最后一个节点，只有在使用现在不在列表中的陈旧指针遍历时才会为真。
     */
    /**
     * Returns the predecessor of p, or the last node if p.prev has been
     * linked to self, which will only be true if traversing with a
     * stale pointer that is now off the list.
     */
    // 返回p的前驱结点, 如果碰到自链接, 则返回最后一个不为null的结点, 从尾再来
    final Node<E> pred(Node<E> p) {
        Node<E> q = p.prev;
        return (p == q) ? last() : q;
    }

    /**
     * 20210529
     * 返回第一个节点，即唯一节点 p，其中：
     *  p.prev == null && p.next != p
     * 返回的节点可能会或可能不会被逻辑删除。 保证 head 设置为返回的节点。
     */
    /**
     * Returns the first node, the unique node p for which:
     *     p.prev == null && p.next != p
     * The returned node may or may not be logically deleted.
     * Guarantees that head is set to the returned node.
     */
    // 获取第一个不为null的结点, 不会返回PREV_TERMINATOR
    Node<E> first() {
        restartFromHead:
        for (;;)
            // 1. h、p为head指针
            for (Node<E> h = head, p = h, q;;) {
                // 2. p、q前移一个结点, 同时保证p永远是第一个不为null的结点, q为p的前驱
                if ((q = p.prev) != null &&
                    (q = (p = q).prev) != null)
                    // Check for head updates every other hop.
                    // If p == q, we are sure to follow head instead.

                    // 3. 如果h不为最新的head指针, 则更新p为最新的head指针;
                    //    如果为最新的head指针, 说明有两个线程插入了结点, 且最后一个线程还没有更新head指针, 此时更新p为p的前驱, 继续自旋, 继续往前找或者等待更新p为最新的h
                    p = (h != (h = head)) ? h : q;
                // 2. 如果p的前驱为null, 且p==h, 说明此时p就是头结点了, 此时直接返回即可; 如果p!=h, 说明此时p已经移动到前面了, 为第一个不为null的结点了,
                //    即别的线程插入了新的结点, 此时CAS设置h为p, 如果设置成功了说明h为有效结点, 此时返回p即可; 如果失败, 说明h已经移动过了, 不能CAS更新h指针
                else if (p == h
                        // p 可能是 PREV_TERMINATOR，但如果是这样，CAS 肯定会失败。
                         // It is possible that p is PREV_TERMINATOR,
                         // but if so, the CAS is guaranteed to fail.
                         || casHead(h, p))
                    return p;
                // 2. 如果p的前驱为null, 且p和h已经移动过了, 此时需要重新进入自旋, 获取最新的head指针
                else
                    continue restartFromHead;
            }
    }

    /**
     * 20210529
     * 返回最后一个节点，其唯一节点 p：
     *  p.next == null && p.prev != p
     * 返回的节点可能会或可能不会被逻辑删除。 保证将尾部设置为返回的节点。
     */
    /**
     * Returns the last node, the unique node p for which:
     *     p.next == null && p.prev != p
     * The returned node may or may not be logically deleted.
     * Guarantees that tail is set to the returned node.
     */
    // 返回最后一个不能null的结点
    Node<E> last() {
        restartFromTail:
        for (;;)
            // 1. t、p为tail指针
            for (Node<E> t = tail, p = t, q;;) {

                // 2. p、q后移一个结点, 同时保证p永远是最后一个不为null的结点, q为p的后继
                if ((q = p.next) != null &&
                    (q = (p = q).next) != null)
                    // Check for tail updates every other hop.
                    // If p == q, we are sure to follow tail instead.

                    // 3. 如果t不为最新的tail指针, 则更新p为最新的tail指针;
                    //    如果为最新的tail指针, 说明有两个线程插入了结点, 且最后一个线程还没有更新tail指针, 此时更新p为p的后继, 继续自旋, 继续往后找或者等待更新p为最新的t
                    p = (t != (t = tail)) ? t : q;

                // 2. 如果p的后继为null, 且p==t, 说明此时p就是尾结点了, 此时直接返回即可; 如果p!=h, 说明此时p已经移动到后面了, 为最后一个不为null的结点了,
                //    即别的线程插入了新的结点, 此时CAS设置t为p, 如果设置成功了说明t为有效结点, 此时返回p即可; 如果失败, 说明t已经移动过了, 不能CAS更新t指针
                else if (p == t
                         // It is possible that p is NEXT_TERMINATOR,
                         // but if so, the CAS is guaranteed to fail.
                         || casTail(t, p))
                    return p;

                // 2. 如果p的后继为null, 且p和t已经移动过了, 此时需要重新进入自旋, 获取最新的tail指针
                else
                    continue restartFromTail;
            }
    }

    // Minor convenience utilities

    /**
     * Throws NullPointerException if argument is null.
     *
     * @param v the element
     */
    private static void checkNotNull(Object v) {
        if (v == null)
            throw new NullPointerException();
    }

    /**
     * Returns element unless it is null, in which case throws
     * NoSuchElementException.
     *
     * @param v the element
     * @return the element
     */
    private E screenNullResult(E v) {
        if (v == null)
            throw new NoSuchElementException();
        return v;
    }

    /**
     * Creates an array list and fills it with elements of this list.
     * Used by toArray.
     *
     * @return the array list
     */
    private ArrayList<E> toArrayList() {
        ArrayList<E> list = new ArrayList<E>();
        for (Node<E> p = first(); p != null; p = succ(p)) {
            E item = p.item;
            if (item != null)
                list.add(item);
        }
        return list;
    }

    /**
     * Constructs an empty deque.
     */
    public ConcurrentLinkedDeque() {
        head = tail = new Node<E>(null);
    }

    /**
     * 20210529
     * 构造一个最初包含给定集合元素的双端队列，按集合迭代器的遍历顺序添加。
     */
    /**
     * Constructs a deque initially containing the elements of
     * the given collection, added in traversal order of the
     * collection's iterator.
     *
     * @param c the collection of elements to initially contain
     * @throws NullPointerException if the specified collection or any
     *         of its elements are null
     */
    public ConcurrentLinkedDeque(Collection<? extends E> c) {
        // Copy c into a private chain of Nodes
        Node<E> h = null, t = null;
        for (E e : c) {
            checkNotNull(e);
            Node<E> newNode = new Node<E>(e);
            if (h == null)
                h = t = newNode;
            else {
                t.lazySetNext(newNode);
                newNode.lazySetPrev(t);
                t = newNode;
            }
        }
        initHeadTail(h, t);
    }

    /**
     * Initializes head and tail, ensuring invariants hold.
     */
    private void initHeadTail(Node<E> h, Node<E> t) {
        if (h == t) {
            if (h == null)
                h = t = new Node<E>(null);
            else {
                // Avoid edge case of a single Node with non-null item.
                Node<E> newNode = new Node<E>(null);
                t.lazySetNext(newNode);
                newNode.lazySetPrev(t);
                t = newNode;
            }
        }
        head = h;
        tail = t;
    }

    /**
     * Inserts the specified element at the front of this deque.
     * As the deque is unbounded, this method will never throw
     * {@link IllegalStateException}.
     *
     * @throws NullPointerException if the specified element is null
     */
    public void addFirst(E e) {
        linkFirst(e);
    }

    /**
     * Inserts the specified element at the end of this deque.
     * As the deque is unbounded, this method will never throw
     * {@link IllegalStateException}.
     *
     * <p>This method is equivalent to {@link #add}.
     *
     * @throws NullPointerException if the specified element is null
     */
    public void addLast(E e) {
        linkLast(e);
    }

    /**
     * Inserts the specified element at the front of this deque.
     * As the deque is unbounded, this method will never return {@code false}.
     *
     * @return {@code true} (as specified by {@link Deque#offerFirst})
     * @throws NullPointerException if the specified element is null
     */
    public boolean offerFirst(E e) {
        linkFirst(e);
        return true;
    }

    /**
     * Inserts the specified element at the end of this deque.
     * As the deque is unbounded, this method will never return {@code false}.
     *
     * <p>This method is equivalent to {@link #add}.
     *
     * @return {@code true} (as specified by {@link Deque#offerLast})
     * @throws NullPointerException if the specified element is null
     */
    public boolean offerLast(E e) {
        linkLast(e);
        return true;
    }

    public E peekFirst() {
        for (Node<E> p = first(); p != null; p = succ(p)) {
            E item = p.item;
            if (item != null)
                return item;
        }
        return null;
    }

    public E peekLast() {
        for (Node<E> p = last(); p != null; p = pred(p)) {
            E item = p.item;
            if (item != null)
                return item;
        }
        return null;
    }

    /**
     * @throws NoSuchElementException {@inheritDoc}
     */
    public E getFirst() {
        return screenNullResult(peekFirst());
    }

    /**
     * @throws NoSuchElementException {@inheritDoc}
     */
    public E getLast() {
        return screenNullResult(peekLast());
    }

    public E pollFirst() {
        for (Node<E> p = first(); p != null; p = succ(p)) {
            E item = p.item;
            if (item != null && p.casItem(item, null)) {
                unlink(p);
                return item;
            }
        }
        return null;
    }

    public E pollLast() {
        for (Node<E> p = last(); p != null; p = pred(p)) {
            E item = p.item;
            if (item != null && p.casItem(item, null)) {
                unlink(p);
                return item;
            }
        }
        return null;
    }

    /**
     * @throws NoSuchElementException {@inheritDoc}
     */
    public E removeFirst() {
        return screenNullResult(pollFirst());
    }

    /**
     * @throws NoSuchElementException {@inheritDoc}
     */
    public E removeLast() {
        return screenNullResult(pollLast());
    }

    // *** Queue and stack methods ***

    /**
     * 20210529
     * 在此双端队列的尾部插入指定的元素。 由于双端队列不受限制，因此此方法将永远不会返回{@code false}。
     */
    /**
     * Inserts the specified element at the tail of this deque.
     * As the deque is unbounded, this method will never return {@code false}.
     *
     * @return {@code true} (as specified by {@link Queue#offer})
     * @throws NullPointerException if the specified element is null
     */
    public boolean offer(E e) {
        return offerLast(e);
    }

    /**
     * 20210529
     * 在此双端队列的尾部插入指定的元素。
     * 由于双端队列不受限制，因此此方法将永远不会抛出{@link IllegalStateException}或返回{@code false}。
     */
    /**
     * Inserts the specified element at the tail of this deque.
     * As the deque is unbounded, this method will never throw
     * {@link IllegalStateException} or return {@code false}.
     *
     * @return {@code true} (as specified by {@link Collection#add})
     * @throws NullPointerException if the specified element is null
     */
    public boolean add(E e) {
        return offerLast(e);
    }

    public E poll()           { return pollFirst(); }
    public E peek()           { return peekFirst(); }

    /**
     * @throws NoSuchElementException {@inheritDoc}
     */
    public E remove()         { return removeFirst(); }

    /**
     * @throws NoSuchElementException {@inheritDoc}
     */
    public E pop()            { return removeFirst(); }

    /**
     * @throws NoSuchElementException {@inheritDoc}
     */
    public E element()        { return getFirst(); }

    /**
     * @throws NullPointerException {@inheritDoc}
     */
    public void push(E e)     { addFirst(e); }

    /**
     * Removes the first element {@code e} such that
     * {@code o.equals(e)}, if such an element exists in this deque.
     * If the deque does not contain the element, it is unchanged.
     *
     * @param o element to be removed from this deque, if present
     * @return {@code true} if the deque contained the specified element
     * @throws NullPointerException if the specified element is null
     */
    public boolean removeFirstOccurrence(Object o) {
        checkNotNull(o);
        for (Node<E> p = first(); p != null; p = succ(p)) {
            E item = p.item;
            if (item != null && o.equals(item) && p.casItem(item, null)) {
                unlink(p);
                return true;
            }
        }
        return false;
    }

    /**
     * Removes the last element {@code e} such that
     * {@code o.equals(e)}, if such an element exists in this deque.
     * If the deque does not contain the element, it is unchanged.
     *
     * @param o element to be removed from this deque, if present
     * @return {@code true} if the deque contained the specified element
     * @throws NullPointerException if the specified element is null
     */
    public boolean removeLastOccurrence(Object o) {
        checkNotNull(o);
        for (Node<E> p = last(); p != null; p = pred(p)) {
            E item = p.item;
            if (item != null && o.equals(item) && p.casItem(item, null)) {
                unlink(p);
                return true;
            }
        }
        return false;
    }

    /**
     * Returns {@code true} if this deque contains at least one
     * element {@code e} such that {@code o.equals(e)}.
     *
     * @param o element whose presence in this deque is to be tested
     * @return {@code true} if this deque contains the specified element
     */
    public boolean contains(Object o) {
        if (o == null) return false;
        for (Node<E> p = first(); p != null; p = succ(p)) {
            E item = p.item;
            if (item != null && o.equals(item))
                return true;
        }
        return false;
    }

    /**
     * Returns {@code true} if this collection contains no elements.
     *
     * @return {@code true} if this collection contains no elements
     */
    public boolean isEmpty() {
        return peekFirst() == null;
    }

    /**
     * Returns the number of elements in this deque.  If this deque
     * contains more than {@code Integer.MAX_VALUE} elements, it
     * returns {@code Integer.MAX_VALUE}.
     *
     * <p>Beware that, unlike in most collections, this method is
     * <em>NOT</em> a constant-time operation. Because of the
     * asynchronous nature of these deques, determining the current
     * number of elements requires traversing them all to count them.
     * Additionally, it is possible for the size to change during
     * execution of this method, in which case the returned result
     * will be inaccurate. Thus, this method is typically not very
     * useful in concurrent applications.
     *
     * @return the number of elements in this deque
     */
    public int size() {
        int count = 0;
        for (Node<E> p = first(); p != null; p = succ(p))
            if (p.item != null)
                // Collection.size() spec says to max out
                if (++count == Integer.MAX_VALUE)
                    break;
        return count;
    }

    /**
     * Removes the first element {@code e} such that
     * {@code o.equals(e)}, if such an element exists in this deque.
     * If the deque does not contain the element, it is unchanged.
     *
     * @param o element to be removed from this deque, if present
     * @return {@code true} if the deque contained the specified element
     * @throws NullPointerException if the specified element is null
     */
    public boolean remove(Object o) {
        return removeFirstOccurrence(o);
    }

    /**
     * Appends all of the elements in the specified collection to the end of
     * this deque, in the order that they are returned by the specified
     * collection's iterator.  Attempts to {@code addAll} of a deque to
     * itself result in {@code IllegalArgumentException}.
     *
     * @param c the elements to be inserted into this deque
     * @return {@code true} if this deque changed as a result of the call
     * @throws NullPointerException if the specified collection or any
     *         of its elements are null
     * @throws IllegalArgumentException if the collection is this deque
     */
    public boolean addAll(Collection<? extends E> c) {
        if (c == this)
            // As historically specified in AbstractQueue#addAll
            throw new IllegalArgumentException();

        // Copy c into a private chain of Nodes
        Node<E> beginningOfTheEnd = null, last = null;
        for (E e : c) {
            checkNotNull(e);
            Node<E> newNode = new Node<E>(e);
            if (beginningOfTheEnd == null)
                beginningOfTheEnd = last = newNode;
            else {
                last.lazySetNext(newNode);
                newNode.lazySetPrev(last);
                last = newNode;
            }
        }
        if (beginningOfTheEnd == null)
            return false;

        // Atomically append the chain at the tail of this collection
        restartFromTail:
        for (;;)
            for (Node<E> t = tail, p = t, q;;) {
                if ((q = p.next) != null &&
                    (q = (p = q).next) != null)
                    // Check for tail updates every other hop.
                    // If p == q, we are sure to follow tail instead.
                    p = (t != (t = tail)) ? t : q;
                else if (p.prev == p) // NEXT_TERMINATOR
                    continue restartFromTail;
                else {
                    // p is last node
                    beginningOfTheEnd.lazySetPrev(p); // CAS piggyback
                    if (p.casNext(null, beginningOfTheEnd)) {
                        // Successful CAS is the linearization point
                        // for all elements to be added to this deque.
                        if (!casTail(t, last)) {
                            // Try a little harder to update tail,
                            // since we may be adding many elements.
                            t = tail;
                            if (last.next == null)
                                casTail(t, last);
                        }
                        return true;
                    }
                    // Lost CAS race to another thread; re-read next
                }
            }
    }

    /**
     * Removes all of the elements from this deque.
     */
    public void clear() {
        while (pollFirst() != null)
            ;
    }

    /**
     * Returns an array containing all of the elements in this deque, in
     * proper sequence (from first to last element).
     *
     * <p>The returned array will be "safe" in that no references to it are
     * maintained by this deque.  (In other words, this method must allocate
     * a new array).  The caller is thus free to modify the returned array.
     *
     * <p>This method acts as bridge between array-based and collection-based
     * APIs.
     *
     * @return an array containing all of the elements in this deque
     */
    public Object[] toArray() {
        return toArrayList().toArray();
    }

    /**
     * Returns an array containing all of the elements in this deque,
     * in proper sequence (from first to last element); the runtime
     * type of the returned array is that of the specified array.  If
     * the deque fits in the specified array, it is returned therein.
     * Otherwise, a new array is allocated with the runtime type of
     * the specified array and the size of this deque.
     *
     * <p>If this deque fits in the specified array with room to spare
     * (i.e., the array has more elements than this deque), the element in
     * the array immediately following the end of the deque is set to
     * {@code null}.
     *
     * <p>Like the {@link #toArray()} method, this method acts as
     * bridge between array-based and collection-based APIs.  Further,
     * this method allows precise control over the runtime type of the
     * output array, and may, under certain circumstances, be used to
     * save allocation costs.
     *
     * <p>Suppose {@code x} is a deque known to contain only strings.
     * The following code can be used to dump the deque into a newly
     * allocated array of {@code String}:
     *
     *  <pre> {@code String[] y = x.toArray(new String[0]);}</pre>
     *
     * Note that {@code toArray(new Object[0])} is identical in function to
     * {@code toArray()}.
     *
     * @param a the array into which the elements of the deque are to
     *          be stored, if it is big enough; otherwise, a new array of the
     *          same runtime type is allocated for this purpose
     * @return an array containing all of the elements in this deque
     * @throws ArrayStoreException if the runtime type of the specified array
     *         is not a supertype of the runtime type of every element in
     *         this deque
     * @throws NullPointerException if the specified array is null
     */
    public <T> T[] toArray(T[] a) {
        return toArrayList().toArray(a);
    }

    /**
     * 20210529
     * A. 以适当的顺序返回此双端队列中元素的迭代器。 元素将按从第一个（头）到最后一个（尾）的顺序返回。
     * B. 返回的迭代器弱一致。
     */
    /**
     * A.
     * Returns an iterator over the elements in this deque in proper sequence.
     * The elements will be returned in order from first (head) to last (tail).
     *
     * B.
     * <p>The returned iterator is
     * <a href="package-summary.html#Weakly"><i>weakly consistent</i></a>.
     *
     * @return an iterator over the elements in this deque in proper sequence
     */
    public Iterator<E> iterator() {
        return new Itr();
    }

    /**
     * Returns an iterator over the elements in this deque in reverse
     * sequential order.  The elements will be returned in order from
     * last (tail) to first (head).
     *
     * <p>The returned iterator is
     * <a href="package-summary.html#Weakly"><i>weakly consistent</i></a>.
     *
     * @return an iterator over the elements in this deque in reverse order
     */
    public Iterator<E> descendingIterator() {
        return new DescendingItr();
    }

    private abstract class AbstractItr implements Iterator<E> {
        /**
         * Next node to return item for.
         */
        private Node<E> nextNode;

        /**
         * nextItem holds on to item fields because once we claim
         * that an element exists in hasNext(), we must return it in
         * the following next() call even if it was in the process of
         * being removed when hasNext() was called.
         */
        private E nextItem;

        /**
         * Node returned by most recent call to next. Needed by remove.
         * Reset to null if this element is deleted by a call to remove.
         */
        private Node<E> lastRet;

        abstract Node<E> startNode();
        abstract Node<E> nextNode(Node<E> p);

        AbstractItr() {
            advance();
        }

        /**
         * Sets nextNode and nextItem to next valid node, or to null
         * if no such.
         */
        private void advance() {
            lastRet = nextNode;

            Node<E> p = (nextNode == null) ? startNode() : nextNode(nextNode);
            for (;; p = nextNode(p)) {
                if (p == null) {
                    // p might be active end or TERMINATOR node; both are OK
                    nextNode = null;
                    nextItem = null;
                    break;
                }
                E item = p.item;
                if (item != null) {
                    nextNode = p;
                    nextItem = item;
                    break;
                }
            }
        }

        public boolean hasNext() {
            return nextItem != null;
        }

        public E next() {
            E item = nextItem;
            if (item == null) throw new NoSuchElementException();
            advance();
            return item;
        }

        public void remove() {
            Node<E> l = lastRet;
            if (l == null) throw new IllegalStateException();
            l.item = null;
            unlink(l);
            lastRet = null;
        }
    }

    /** Forward iterator */
    private class Itr extends AbstractItr {
        Node<E> startNode() { return first(); }
        Node<E> nextNode(Node<E> p) { return succ(p); }
    }

    /** Descending iterator */
    private class DescendingItr extends AbstractItr {
        Node<E> startNode() { return last(); }
        Node<E> nextNode(Node<E> p) { return pred(p); }
    }

    /** A customized variant of Spliterators.IteratorSpliterator */
    static final class CLDSpliterator<E> implements Spliterator<E> {
        static final int MAX_BATCH = 1 << 25;  // max batch array size;
        final ConcurrentLinkedDeque<E> queue;
        Node<E> current;    // current node; null until initialized
        int batch;          // batch size for splits
        boolean exhausted;  // true when no more nodes
        CLDSpliterator(ConcurrentLinkedDeque<E> queue) {
            this.queue = queue;
        }

        public Spliterator<E> trySplit() {
            Node<E> p;
            final ConcurrentLinkedDeque<E> q = this.queue;
            int b = batch;
            int n = (b <= 0) ? 1 : (b >= MAX_BATCH) ? MAX_BATCH : b + 1;
            if (!exhausted &&
                ((p = current) != null || (p = q.first()) != null)) {
                if (p.item == null && p == (p = p.next))
                    current = p = q.first();
                if (p != null && p.next != null) {
                    Object[] a = new Object[n];
                    int i = 0;
                    do {
                        if ((a[i] = p.item) != null)
                            ++i;
                        if (p == (p = p.next))
                            p = q.first();
                    } while (p != null && i < n);
                    if ((current = p) == null)
                        exhausted = true;
                    if (i > 0) {
                        batch = i;
                        return Spliterators.spliterator
                            (a, 0, i, Spliterator.ORDERED | Spliterator.NONNULL |
                             Spliterator.CONCURRENT);
                    }
                }
            }
            return null;
        }

        public void forEachRemaining(Consumer<? super E> action) {
            Node<E> p;
            if (action == null) throw new NullPointerException();
            final ConcurrentLinkedDeque<E> q = this.queue;
            if (!exhausted &&
                ((p = current) != null || (p = q.first()) != null)) {
                exhausted = true;
                do {
                    E e = p.item;
                    if (p == (p = p.next))
                        p = q.first();
                    if (e != null)
                        action.accept(e);
                } while (p != null);
            }
        }

        public boolean tryAdvance(Consumer<? super E> action) {
            Node<E> p;
            if (action == null) throw new NullPointerException();
            final ConcurrentLinkedDeque<E> q = this.queue;
            if (!exhausted &&
                ((p = current) != null || (p = q.first()) != null)) {
                E e;
                do {
                    e = p.item;
                    if (p == (p = p.next))
                        p = q.first();
                } while (e == null && p != null);
                if ((current = p) == null)
                    exhausted = true;
                if (e != null) {
                    action.accept(e);
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
     * Returns a {@link Spliterator} over the elements in this deque.
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
     * @return a {@code Spliterator} over the elements in this deque
     * @since 1.8
     */
    public Spliterator<E> spliterator() {
        return new CLDSpliterator<E>(this);
    }

    /**
     * Saves this deque to a stream (that is, serializes it).
     *
     * @param s the stream
     * @throws java.io.IOException if an I/O error occurs
     * @serialData All of the elements (each an {@code E}) in
     * the proper order, followed by a null
     */
    private void writeObject(java.io.ObjectOutputStream s)
        throws java.io.IOException {

        // Write out any hidden stuff
        s.defaultWriteObject();

        // Write out all elements in the proper order.
        for (Node<E> p = first(); p != null; p = succ(p)) {
            E item = p.item;
            if (item != null)
                s.writeObject(item);
        }

        // Use trailing null as sentinel
        s.writeObject(null);
    }

    /**
     * Reconstitutes this deque from a stream (that is, deserializes it).
     * @param s the stream
     * @throws ClassNotFoundException if the class of a serialized object
     *         could not be found
     * @throws java.io.IOException if an I/O error occurs
     */
    private void readObject(java.io.ObjectInputStream s)
        throws java.io.IOException, ClassNotFoundException {
        s.defaultReadObject();

        // Read in elements until trailing null sentinel found
        Node<E> h = null, t = null;
        Object item;
        while ((item = s.readObject()) != null) {
            @SuppressWarnings("unchecked")
            Node<E> newNode = new Node<E>((E) item);
            if (h == null)
                h = t = newNode;
            else {
                t.lazySetNext(newNode);
                newNode.lazySetPrev(t);
                t = newNode;
            }
        }
        initHeadTail(h, t);
    }

    private boolean casHead(Node<E> cmp, Node<E> val) {
        return UNSAFE.compareAndSwapObject(this, headOffset, cmp, val);
    }

    private boolean casTail(Node<E> cmp, Node<E> val) {
        return UNSAFE.compareAndSwapObject(this, tailOffset, cmp, val);
    }

    // Unsafe mechanics

    private static final sun.misc.Unsafe UNSAFE;
    private static final long headOffset;
    private static final long tailOffset;
    static {
        PREV_TERMINATOR = new Node<Object>();
        PREV_TERMINATOR.next = PREV_TERMINATOR;
        NEXT_TERMINATOR = new Node<Object>();
        NEXT_TERMINATOR.prev = NEXT_TERMINATOR;
        try {
            UNSAFE = sun.misc.Unsafe.getUnsafe();
            Class<?> k = ConcurrentLinkedDeque.class;
            headOffset = UNSAFE.objectFieldOffset
                (k.getDeclaredField("head"));
            tailOffset = UNSAFE.objectFieldOffset
                (k.getDeclaredField("tail"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }
}
