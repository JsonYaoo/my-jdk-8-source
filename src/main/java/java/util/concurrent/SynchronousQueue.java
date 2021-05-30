/*
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

/*
 * Written by Doug Lea, Bill Scherer, and Michael Scott with
 * assistance from members of JCP JSR-166 Expert Group and released to
 * the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util.concurrent;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;
import java.util.*;
import java.util.Spliterator;
import java.util.Spliterators;

/**
 * 20210524
 * A. {@linkplain BlockingQueue}阻塞队列，其中每个插入操作必须等待另一个线程进行相应的删除操作，反之亦然。 同步队列没有任何内部容量，甚至没有一个容量。
 *    您无法在同步队列中{@code peek}，因为仅当您尝试删除它时，该元素才存在。 您不能插入元素（使用任何方法），除非另一个线程试图将其删除；
 *    您无法进行迭代，因为没有要迭代的内容。 队列的头部是第一个排队的插入线程试图添加到队列中的元素； 如果没有这样的排队线程，则没有元素可用于删除，
 *    并且{@code poll（）}将返回{@code null}。 出于其他{@code Collection}方法（例如{@code contains}）的目的，{@code SynchronousQueue}用作空集合。
 *    此队列不允许{@code null}元素。
 * B. 同步队列类似于CSP和Ada中使用的集合通道。 它们非常适合切换设计，在该设计中，在一个线程中运行的对象必须与在另一个线程中运行的对象同步，
 *    以便向其传递一些信息，事件或任务。
 * C. 此类支持可选的公平性策略，用于订购正在等待的生产者和使用者线程。 默认情况下，不保证此排序。 但是，使用公平性设置为{@code true}构造的队列将按FIFO顺序授予线程访问权限。
 * D. 此类及其迭代器实现{@link Collection}和{@link Iterator}接口的所有可选方法。
 * E. {@docRoot}/../technotes/guides/collections/index.html
 */

/**
 * A.
 * A {@linkplain BlockingQueue blocking queue} in which each insert
 * operation must wait for a corresponding remove operation by another
 * thread, and vice versa.  A synchronous queue does not have any
 * internal capacity, not even a capacity of one.  You cannot
 * {@code peek} at a synchronous queue because an element is only
 * present when you try to remove it; you cannot insert an element
 * (using any method) unless another thread is trying to remove it;
 * you cannot iterate as there is nothing to iterate.  The
 * <em>head</em> of the queue is the element that the first queued
 * inserting thread is trying to add to the queue; if there is no such
 * queued thread then no element is available for removal and
 * {@code poll()} will return {@code null}.  For purposes of other
 * {@code Collection} methods (for example {@code contains}), a
 * {@code SynchronousQueue} acts as an empty collection.  This queue
 * does not permit {@code null} elements.
 *
 * B.
 * <p>Synchronous queues are similar to rendezvous channels used in
 * CSP and Ada. They are well suited for handoff designs, in which an
 * object running in one thread must sync up with an object running
 * in another thread in order to hand it some information, event, or
 * task.
 *
 * C.
 * <p>This class supports an optional fairness policy for ordering
 * waiting producer and consumer threads.  By default, this ordering
 * is not guaranteed. However, a queue constructed with fairness set
 * to {@code true} grants threads access in FIFO order.
 *
 * D.
 * <p>This class and its iterator implement all of the
 * <em>optional</em> methods of the {@link Collection} and {@link
 * Iterator} interfaces.
 *
 * E.
 * <p>This class is a member of the
 * <a href="{@docRoot}/../technotes/guides/collections/index.html">
 * Java Collections Framework</a>.
 *
 * @since 1.5
 * @author Doug Lea and Bill Scherer and Michael Scott
 * @param <E> the type of elements held in this collection
 */
public class SynchronousQueue<E> extends AbstractQueue<E> implements BlockingQueue<E>, java.io.Serializable {
    private static final long serialVersionUID = -3223113410248163686L;

    /**
     * 20210524
     * A. 此类实现了W. N. Scherer III和M. L. Scott所著的“不带条件同步的并发对象的无阻塞”中描述的双栈和双队列算法的扩展。 第十八届年度大会关于分布式计算，
     *    2004年10月（另请参见http://www.cs.rochester.edu/u/scott/synchronization/pseudocode/duals.html）。
     *    （Lifo）堆栈用于非公平模式，（Fifo）队列用于公平模式。 两者的性能通常相似。 Fifo通常在竞争下支持更高的吞吐量，但是Lifo在常见应用程序中保持更高的线程局部性。
     * B. 双队列（和类似的堆栈）是在任何给定时间保存“数据”（由put操作提供的项，或“请求”）的插槽，表示代入操作，或者为空。
     *    对“实现”的调用（即，从保存数据的队列中请求商品的调用，反之亦然）使互补节点出队。 这些队列最有趣的功能是，任何操作都可以弄清楚队列所处的模式，
     *    并且无需锁就可以采取相应的措施。
     * C. 队列和堆栈都扩展了抽象类Transferer，它们定义了执行放置或取出操作的单个方法传输。 将它们统一为一个方法，因为在双重数据结构中，放置和取出操作是对称的，
     *    因此几乎所有代码都可以合并。 最终的传输方法长远来看，但比分解成几乎重复的部分要容易得多。
     * D. 队列和堆栈数据结构在概念上有很多相似之处，但具体细节很少。 为简单起见，它们保持不同，以便以后可以分别发展。
     * E. 此处的算法与上述论文中的版本不同，在于扩展了它们以用于同步队列以及处理取消。 主要区别包括：
     *      1. 原始算法使用带位标记的指针，但此处的算法使用节点中的模式位，从而导致了许多进一步的调整。
     *      2. SynchronousQueues必须阻塞等待实现的线程。
     *      3. 支持通过超时和中断进行取消，包括从列表中清除已取消的节点/线程，以避免垃圾保留和内存耗尽。
     * F. 阻塞主要使用LockSupport暂存/取消暂存来完成，除了看起来像是首先要实现的下一个节点外，首先一点是自旋（仅在多处理器上）。 在非常繁忙的同步队列上，
     *    自旋可以极大地提高吞吐量。 在不那么忙碌的自旋上，自旋的量很小，不足以引起注意。
     * G. 在队列和堆栈中以不同的方式进行清理。 对于队列，我们几乎总是可以在取消节点后的O（1）时间（立即重试以进行一致性检查）中立即删除该节点。
     *    但是，如果可能将其固定为当前尾部，则必须等待直到随后进行一些取消。 对于堆栈，我们需要潜在的O（n）遍历，以确保可以删除节点，但这可以与其他访问堆栈的线程同时运行。
     * H. 尽管垃圾回收会处理大多数会使非阻塞算法复杂化的节点回收问题，但还是要小心“忘记”对数据，其他节点和可能被阻塞线程长期保留的线程的引用。
     *    如果设置为null会与主要算法发生冲突，则可以通过将节点的链接更改为现在指向节点本身来实现。 对于Stack节点，这不会发生太多（因为阻塞的线程不会挂在旧的头部指针上），
     *    但是必须积极地忘记Queue节点中的引用，以防止自到达以来任何节点都曾引用的所有内容都可以访问。
     */
    /*
     * A.
     * This class implements extensions of the dual stack and dual
     * queue algorithms described in "Nonblocking Concurrent Objects
     * with Condition Synchronization", by W. N. Scherer III and
     * M. L. Scott.  18th Annual Conf. on Distributed Computing,
     * Oct. 2004 (see also
     * http://www.cs.rochester.edu/u/scott/synchronization/pseudocode/duals.html).
     * The (Lifo) stack is used for non-fair mode, and the (Fifo)
     * queue for fair mode. The performance of the two is generally
     * similar. Fifo usually supports higher throughput under
     * contention but Lifo maintains higher thread locality in common
     * applications.
     *
     * B.
     * A dual queue (and similarly stack) is one that at any given
     * time either holds "data" -- items provided by put operations,
     * or "requests" -- slots representing take operations, or is
     * empty. A call to "fulfill" (i.e., a call requesting an item
     * from a queue holding data or vice versa) dequeues a
     * complementary node.  The most interesting feature of these
     * queues is that any operation can figure out which mode the
     * queue is in, and act accordingly without needing locks.
     *
     * C.
     * Both the queue and stack extend abstract class Transferer
     * defining the single method transfer that does a put or a
     * take. These are unified into a single method because in dual
     * data structures, the put and take operations are symmetrical,
     * so nearly all code can be combined. The resulting transfer
     * methods are on the long side, but are easier to follow than
     * they would be if broken up into nearly-duplicated parts.
     *
     * D.
     * The queue and stack data structures share many conceptual
     * similarities but very few concrete details. For simplicity,
     * they are kept distinct so that they can later evolve
     * separately.
     *
     * E.
     * The algorithms here differ from the versions in the above paper
     * in extending them for use in synchronous queues, as well as
     * dealing with cancellation. The main differences include:
     *
     *  1. The original algorithms used bit-marked pointers, but
     *     the ones here use mode bits in nodes, leading to a number
     *     of further adaptations.
     *  2. SynchronousQueues must block threads waiting to become
     *     fulfilled.
     *  3. Support for cancellation via timeout and interrupts,
     *     including cleaning out cancelled nodes/threads
     *     from lists to avoid garbage retention and memory depletion.
     *
     * F.
     * Blocking is mainly accomplished using LockSupport park/unpark,
     * except that nodes that appear to be the next ones to become
     * fulfilled first spin a bit (on multiprocessors only). On very
     * busy synchronous queues, spinning can dramatically improve
     * throughput. And on less busy ones, the amount of spinning is
     * small enough not to be noticeable.
     *
     * G.
     * Cleaning is done in different ways in queues vs stacks.  For
     * queues, we can almost always remove a node immediately in O(1)
     * time (modulo retries for consistency checks) when it is
     * cancelled. But if it may be pinned as the current tail, it must
     * wait until some subsequent cancellation. For stacks, we need a
     * potentially O(n) traversal to be sure that we can remove the
     * node, but this can run concurrently with other threads
     * accessing the stack.
     *
     * H.
     * While garbage collection takes care of most node reclamation
     * issues that otherwise complicate nonblocking algorithms, care
     * is taken to "forget" references to data, other nodes, and
     * threads that might be held on to long-term by blocked
     * threads. In cases where setting to null would otherwise
     * conflict with main algorithms, this is done by changing a
     * node's link to now point to the node itself. This doesn't arise
     * much for Stack nodes (because blocked threads do not hang on to
     * old head pointers), but references in Queue nodes must be
     * aggressively forgotten to avoid reachability of everything any
     * node has ever referred to since arrival.
     */

    /**
     * 20210524
     * 双重堆栈和队列的共享内部API。
     */
    /**
     * Shared internal API for dual stacks and queues.
     */
    abstract static class Transferer<E> {
        /**
         * Performs a put or take.
         *
         * @param e if non-null, the item to be handed to a consumer;
         *          if null, requests that transfer return an item
         *          offered by producer.
         * @param timed if this operation should timeout
         * @param nanos the timeout, in nanoseconds
         *
         * // 如果为非空，则提供或接收的项目； 如果为null，则操作由于超时或中断而失败-调用者可以通过检查Thread.interrupted来区分发生哪种操作。
         * @return if non-null, the item provided or received; if null,
         *         the operation failed due to timeout or interrupt --
         *         the caller can distinguish which of these occurred
         *         by checking Thread.interrupted.
         */
        abstract E transfer(E e, boolean timed, long nanos);
    }

    /** 用于自旋控制的CPU数量 */
    /** The number of CPUs, for spin control */
    static final int NCPUS = Runtime.getRuntime().availableProcessors();

    /**
     * 20210524
     * 在阻塞等待时间之前自旋次数。 该值是根据经验得出的-在各种处理器和OS上都可以很好地工作。 根据经验，最佳值似乎不会随CPU数量（超过2个）而变化，因此只是一个常数。
     */
    /**
     * The number of times to spin before blocking in timed waits.
     * The value is empirically derived -- it works well across a
     * variety of processors and OSes. Empirically, the best value
     * seems not to vary with number of CPUs (beyond 2) so is just
     * a constant.
     */
    static final int maxTimedSpins = (NCPUS < 2) ? 0 : 32;

    /**
     * 20210524
     * 在阻塞未定时的等待之前自旋次数。 此值大于定时值，因为非定时等待旋转得更快，因为它们不需要检查每次旋转的时间。
     */
    /**
     * The number of times to spin before blocking in untimed waits.
     * This is greater than timed value because untimed waits spin
     * faster since they don't need to check times on each spin.
     */
    static final int maxUntimedSpins = maxTimedSpins * 16;

    /**
     * 20210524
     * 相对于使用定时停驻而言，旋转得更快的纳秒数。 粗略的估计就足够了。
     */
    /**
     * The number of nanoseconds for which it is faster to spin
     * rather than to use timed park. A rough estimate suffices.
     */
    static final long spinForTimeoutThreshold = 1000L;

    /** 双重结构之栈结构 */
    /** Dual stack */
    static final class TransferStack<E> extends Transferer<E> {

        /**
         * 20210524
         * 这扩展了Scherer-Scott双堆栈算法，除其他方式外，它通过使用“覆盖”节点而不是位标记的指针而有所不同：
         * 实现操作将推送标记节点（在模式下将FULFILLING位置1）以保留一个点以匹配等待 节点。
         */
        /*
         * This extends Scherer-Scott dual stack algorithm, differing,
         * among other ways, by using "covering" nodes rather than
         * bit-marked pointers: Fulfilling operations push on marker
         * nodes (with FULFILLING bit set in mode) to reserve a spot
         * to match a waiting node.
         */

        /** 20210524 SNode的模式，在节点字段中一起或运算 */
        /* Modes for SNodes, ORed together in node fields */
        /** Node represents an unfulfilled consumer */
        static final int REQUEST    = 0;// 节点代表未实现的消费者
        /** Node represents an unfulfilled producer */
        static final int DATA       = 1;// 节点代表未完成的生产者
        /** Node is fulfilling another unfulfilled DATA or REQUEST */
        static final int FULFILLING = 2;// 节点正在执行另一个未完成的数据或请求

        /** 20210524 如果m设置了满足位，则返回true。*/
        /** Returns true if m has fulfilling bit set. */
        static boolean isFulfilling(int m) { return (m & FULFILLING) != 0; }

        /** 20210524 TransferStacks的节点类。 */
        /** Node class for TransferStacks. */
        static final class SNode {
            volatile SNode next;        // next node in stack   // 堆栈中的下一个节点
            volatile SNode match;       // the node matched to this // 与此节点匹配的节点
            volatile Thread waiter;     // to control park/unpark   // 控制park/unpark
            Object item;                // data; or null for REQUESTs // 数据; 或对于REQUEST为null
            int mode;

            /**
             * 20210524
             * 注意：item和mode字段不需要是易失性的，因为它们总是在其他易失性/原子操作之前写入和在其后读取。
             */
            // Note: item and mode fields don't need to be volatile
            // since they are always written before, and read after,
            // other volatile/atomic operations.
            SNode(Object item) {
                this.item = item;
            }

            // CAS下一个结点为val
            boolean casNext(SNode cmp, SNode val) {
                return cmp == next && UNSAFE.compareAndSwapObject(this, nextOffset, cmp, val);
            }

            /**
             * 20210524
             * 尝试将节点s与此节点匹配，如果是，则唤醒线程。 Fulfiller呼叫tryMatch来识别其服务员。 服务员封锁，直到他们被匹配。
             */
            /**
             * Tries to match node s to this node, if so, waking up thread.
             * Fulfillers call tryMatch to identify their waiters.
             * Waiters block until they have been matched.
             *
             * @param s the node to match
             * @return true if successfully matched to s
             */
            // 结点配对
            boolean tryMatch(SNode s) {
                // 18. 如果当前结点为null, 则还需要CAS设置为传入的结点为匹配结点 + 唤醒put()阻塞的线程
                // 见(9. 如果自旋结束(阻塞512次或者头结点更新了), 判断当前结点有没有设置阻塞线程, eg: put(): null => 设置waiter为当前线程)
                if (match == null && UNSAFE.compareAndSwapObject(this, matchOffset, null, s)) {
                    Thread w = waiter;
                    if (w != null) {    // waiters need at most one unpark
                        waiter = null;
                        LockSupport.unpark(w);
                    }
                    return true;
                }
                return match == s;
            }

            /**
             * 20210524
             * 尝试通过将节点与其自身匹配来取消等待。
             */
            /**
             * Tries to cancel a wait by matching node to itself.
             */
            void tryCancel() {
                UNSAFE.compareAndSwapObject(this, matchOffset, null, this);
            }

            // 如果当前头结点的配对结点等于头结点自身, 则认为可以取消头结点了(见6. 如果当前线程被中断, 则将设置当前配对结点为当前结点)
            boolean isCancelled() {
                return match == this;
            }

            // Unsafe mechanics
            private static final sun.misc.Unsafe UNSAFE;
            private static final long matchOffset;
            private static final long nextOffset;

            static {
                try {
                    UNSAFE = sun.misc.Unsafe.getUnsafe();
                    Class<?> k = SNode.class;
                    matchOffset = UNSAFE.objectFieldOffset
                        (k.getDeclaredField("match"));
                    nextOffset = UNSAFE.objectFieldOffset
                        (k.getDeclaredField("next"));
                } catch (Exception e) {
                    throw new Error(e);
                }
            }
        }

        /** The head (top) of the stack */
        volatile SNode head;// 堆栈的头（顶部）

        boolean casHead(SNode h, SNode nh) {
            return h == head &&
                UNSAFE.compareAndSwapObject(this, headOffset, h, nh);
        }

        /**
         * 20210524
         * 创建或重置节点的字段。 仅从传输中调用，在这种情况下将延迟创建要推入堆栈的节点，并在可能的情况下将其重用，以帮助减少读取和头的CASes之间的间隔，
         * 并避免当CASes推入节点由于争用而失败时产生的大量垃圾。
         */
        /**
         * Creates or resets fields of a node. Called only from transfer
         * where the node to push on stack is lazily created and
         * reused when possible to help reduce intervals between reads
         * and CASes of head and to avoid surges of garbage when CASes
         * to push nodes fail due to contention.
         */
        static SNode snode(SNode s, Object e, SNode next, int mode) {
            if (s == null) s = new SNode(e);
            s.mode = mode;
            s.next = next;
            return s;
        }

        /**
         * 20210524
         * 放入或取出物品。
         */
        /**
         * Puts or takes an item.
         */
        @SuppressWarnings("unchecked")
        E transfer(E e, boolean timed, long nanos) {

            /**
             * 20210524
             * 基本算法是循环尝试以下三个动作之一：
             * 1. 如果显然是空的或已经包含相同模式的节点，请尝试将节点压入堆栈并等待匹配，然后将其返回；如果取消，则返回null。
             * 2. 如果显然包含互补模式的节点，请尝试将满足条件的节点推入堆栈，与相应的等待节点匹配，从堆栈中弹出两者，然后返回匹配的项。 由于其他线程正在执行操作3，
             *    因此实际上可能不需要匹配或取消链接。
             * 3. 如果堆栈顶部已经包含另一个充实的节点，请通过执行其匹配和/或弹出操作来对其进行帮助，然后继续。 帮助代码与实现代码基本相同，不同之处在于它不返回该项目。
             */
            /*
             * Basic algorithm is to loop trying one of three actions:
             *
             * 1. If apparently empty or already containing nodes of same
             *    mode, try to push node on stack and wait for a match,
             *    returning it, or null if cancelled.
             *
             * 2. If apparently containing node of complementary mode,
             *    try to push a fulfilling node on to stack, match
             *    with corresponding waiting node, pop both from
             *    stack, and return matched item. The matching or
             *    unlinking might not actually be necessary because of
             *    other threads performing action 3:
             *
             * 3. If top of stack already holds another fulfilling node,
             *    help it out by doing its match and/or pop
             *    operations, and then continue. The code for helping
             *    is essentially the same as for fulfilling, except
             *    that it doesn't return the item.
             */

            SNode s = null; // constructed/reused as needed
            int mode = (e == null) ? REQUEST : DATA;

            for (;;) {
                SNode h = head;

                // 33. eg: 再offer(E), 这时的mode为1, h为take()的s, h.mode=0
                // 24. eg: 先take(), 这时的mode为0, h为null
                // 11. eg: poll(): 这时的mode为0, h为put()的s, h.mode = 1
                // 0. 一开始头结点为空
                if (h == null || h.mode == mode) {  // empty or same-mode
                    // 1. put()与take()为false, 其他为true, 且offer(E)与poll()的超时时间为0
                    if (timed && nanos <= 0) {      // can't wait
                        if (h != null && h.isCancelled())
                            casHead(h, h.next);     // pop cancelled node
                        else
                            return null;
                    }
                    // 25. eg: take(): 构造结点 s=snode[item=e, next=null, mode=0] => 再CAS移动头结点
                    // 2. eg: put(): 构造结点 s=snode[item=e, next=null, mode=1] => 再CAS移动头结点
                    else if (casHead(h, s = snode(s, e, h, mode))) {
                        // 26.CAS移动头结点成功, 则自旋 + 设置s结点的阻塞线程 + LockSupport.park阻塞当前线程
                        // 3.CAS移动头结点成功, 则自旋 + 设置s结点的阻塞线程 + LockSupport.park阻塞当前线程
                        SNode m = awaitFulfill(s, timed, nanos);

                        // 41. 与唤醒当前线程的take()线程同步进行: m为take()时的匹配结点, eg: m != s
                        // 21. 与唤醒当前线程的poll()线程同步进行: m为poll()时的匹配结点, eg: m != s
                        if (m == s) {               // wait was cancelled
                            clean(s);
                            return null;
                        }
                        // 42. 与唤醒当前线程的take()线程同步进行: eg: 此时head为null
                        // 22. 与唤醒当前线程的poll()线程同步进行: eg: 此时head为null
                        if ((h = head) != null && h.next == s)
                            casHead(h, s.next);     // help s's fulfiller

                        // 43. 与唤醒当前线程的take()线程同步进行: eg: take(): mode=0, 返回offer(E)时的元素
                        // 23. 与唤醒当前线程的poll()线程同步进行: eg: put(): mode=1, 返回put()时的元素
                        return (E) ((mode == REQUEST) ? m.item : s.item);
                    }
                }
                // 34. 判断是否为配对模式, eg: offer(E): false
                // 12. 判断是否为配对模式, eg: poll(): false
                else if (!isFulfilling(h.mode)) { // try to fulfill
                    // 13. 如果当前头结点的配对结点等于头结点自身, 则认为可以取消头结点了(见6. 如果当前线程被中断, 则将设置当前配对结点为当前结点)
                    // eg: poll(): false
                    if (h.isCancelled())            // already cancelled
                        casHead(h, h.next);         // pop and retry
                    // 35. 再次判断当前模式是否为配对模式, 构造结点 s=snode[item=e, next=take()的结点, mode=0] => 再CAS移动头结点(相当于压栈)
                    // 14. 再次判断当前模式是否为配对模式, 构造结点 s=snode[item=e, next=原来的头结点, mode=0] => 再CAS移动头结点(相当于压栈)
                    else if (casHead(h, s=snode(s, e, h, FULFILLING|mode))) {
                        // 36. 压栈成功后, 自旋
                        // 15. 压栈成功后, 自旋
                        for (;;) { // loop until matched or waiters disappear
                            // 37. 获取原来的头结点, eg: offer(E): s
                            // 16. 获取原来的头结点, eg: poll(): s
                            SNode m = s.next;       // m is s's match
                            if (m == null) {        // all waiters are gone
                                casHead(s, null);   // pop fulfill node
                                s = null;           // use new node next time
                                break;              // restart main loop
                            }

                            // 38. 获取原来头结点的下一个结点, eg: offer(E): null, 将原来结点配对当前结点 + 唤醒take()阻塞的线程
                            // 17. 获取原来头结点的下一个结点, eg: poll(): null, 将原来结点配对当前结点 + 唤醒put()阻塞的线程
                            SNode mn = m.next;
                            if (m.tryMatch(s)) {
                                // 39. 与唤醒的take()线程同时进行: 配对成功, eg: 交换null作为新的头结点
                                // 19. 与唤醒的put()线程同时进行: 配对成功, eg: 交换null作为新的头结点
                                casHead(s, mn);     // pop both s and m
                                // 40. 与唤醒的take()线程同时进行: offer(E)添加成功, 返回当前offer(E)的元素。
                                // 20. 与唤醒的put()线程同时进行: poll()删除成功, 返回put()时的元素
                                return (E) ((mode == REQUEST) ? m.item : s.item);
                            } else                  // lost match
                            // 19. 如果没匹配上, 则继续找下一个结点匹配
                                s.casNext(m, mn);   // help unlink
                        }
                    }
                } else {                            // help a fulfiller
                    SNode m = h.next;               // m is h's match
                    if (m == null)                  // waiter is gone
                        casHead(h, null);           // pop fulfilling node
                    else {
                        SNode mn = m.next;
                        if (m.tryMatch(h))          // help match
                            casHead(h, mn);         // pop both h and m
                        else                        // lost match
                            h.casNext(m, mn);       // help unlink
                    }
                }
            }
        }

        /**
         * 20210524
         * 自旋/阻塞，直到节点s通过执行操作被匹配。
         */
        /**
         * Spins/blocks until node s is matched by a fulfill operation.
         *
         * @param s the waiting node
         * @param timed true if timed wait
         * @param nanos timeout value
         * @return matched node, or s if cancelled
         */
        SNode awaitFulfill(SNode s, boolean timed, long nanos) {

            /**
             * 20210524
             * A. 当节点/线程将要阻塞时，它会设置其阻塞线程字段，然后在实际park之前至少再检查一次状态，从而涵盖了竞争与实现者的关系，并注意到阻塞线程为非空，因此应将其唤醒。
             * B. 当由出现在堆栈顶部的调用点处的节点调用时，对park的调用之前会进行自旋，以避免在生产者和消费者及时到达时阻塞。 这可能足以只在多处理器上发生。
             * C. 从主循环返回的检查顺序反映了这样一个事实，即中断的优先级高于正常的返回，而正常的返回优先于超时。 （因此，在超时时，在放弃之前要进行最后一次匹配检查。）
             *    除了来自非定时SynchronousQueue的调用。{poll / offer}不会检查中断，根本不等待，因此被困在传输方法中 而不是调用awaitFulfill。
             */
            /*
             * A.
             * When a node/thread is about to block, it sets its waiter
             * field and then rechecks state at least one more time
             * before actually parking, thus covering race vs
             * fulfiller noticing that waiter is non-null so should be
             * woken.
             *
             * B.
             * When invoked by nodes that appear at the point of call
             * to be at the head of the stack, calls to park are
             * preceded by spins to avoid blocking when producers and
             * consumers are arriving very close in time.  This can
             * happen enough to bother only on multiprocessors.
             *
             * C.
             * The order of checks for returning out of main loop
             * reflects fact that interrupts have precedence over
             * normal returns, which have precedence over
             * timeouts. (So, on timeout, one last check for match is
             * done before giving up.) Except that calls from untimed
             * SynchronousQueue.{poll/offer} don't check interrupts
             * and don't wait at all, so are trapped in transfer
             * method rather than calling awaitFulfill.
             */
            // 27. eg: take(): time=false
            // 4. eg: put(): timed=false
            final long deadline = timed ? System.nanoTime() + nanos : 0L;
            Thread w = Thread.currentThread();

            // 28. 是否应该自旋: 当前结点为头结点 | 头结点为null | 当前模式为配对模式时 => true, 自旋次数为32 * 16 = 512
            // 5. 是否应该自旋: 当前结点为头结点 | 头结点为null | 当前模式为配对模式时 => true, 自旋次数为32 * 16 = 512
            int spins = (shouldSpin(s) ? (timed ? maxTimedSpins : maxUntimedSpins) : 0);

            // 39. 与唤醒当前线程的offer(E)线程同步进行: 继续自旋
            // 19. 与唤醒当前线程的poll()线程同步进行: 继续自旋
            for (;;) {
                // 6. 如果当前线程被中断, 则将设置当前配对结点为当前结点, eg: put() => false
                if (w.isInterrupted())
                    s.tryCancel();

                // 40. 与唤醒当前线程的offer(E)线程同步进行: 见(38. 如果当前结点为null, 则还需要CAS设置为传入的结点为匹配结点 + 唤醒take()阻塞的线程),
                //     eg: 此时m=take()时的结点, 并返回m, 结束自旋
                // 29. eg: take(), 配对结点为null
                // 20. 与唤醒当前线程的poll()线程同步进行: 见(18. 如果当前结点为null, 则还需要CAS设置为传入的结点为匹配结点 + 唤醒put()阻塞的线程),
                //     eg: 此时m=poll()时的结点, 并返回m, 结束自旋
                // 7. 获取配对的结点, eg: put() => null
                SNode m = s.match;
                if (m != null)
                    return m;
                if (timed) {
                    nanos = deadline - System.nanoTime();
                    if (nanos <= 0L) {
                        s.tryCancel();
                        continue;
                    }
                }
                // 30. 重新计算自旋次数, eg: put() => 512 - 1 = 511...
                // 8. 重新计算自旋次数, eg: put() => 512 - 1 = 511...
                if (spins > 0)
                    spins = shouldSpin(s) ? (spins-1) : 0;
                // 31. 如果自旋结束(阻塞512次或者头结点更新了), 判断当前结点有没有设置阻塞线程, eg: take(): null => 设置waiter为当前线程
                // 9. 如果自旋结束(阻塞512次或者头结点更新了), 判断当前结点有没有设置阻塞线程, eg: put(): null => 设置waiter为当前线程
                else if (s.waiter == null)
                    s.waiter = w; // establish waiter so can park next iter
                // 32. 如果自旋结束了, 且也设置了阻塞线程, eg: take(): false => 调用LockSupport.part进行阻塞
                // 10. 如果自旋结束了, 且也设置了阻塞线程, eg: put(): false => 调用LockSupport.part进行阻塞
                else if (!timed)
                    LockSupport.park(this);
                else if (nanos > spinForTimeoutThreshold)
                    LockSupport.parkNanos(this, nanos);
            }
        }

        /**
         * 20210524
         * 如果节点s在头或有一个活跃的履行者，则返回true。
         */
        /**
         * Returns true if node s is at head or there is an active
         * fulfiller.
         */
        // 是否应该自旋: 当前结点为头结点 | 头结点为null | 当前模式为配对模式时
        boolean shouldSpin(SNode s) {
            SNode h = head;
            return (h == s || h == null || isFulfilling(h.mode));
        }


        /**
         * 20210524
         * 从堆栈取消链接。
         */
        /**
         * Unlinks s from the stack.
         */
        void clean(SNode s) {
            s.item = null;   // forget item
            s.waiter = null; // forget thread

            /**
             * 20210524
             * 最糟糕的是，我们可能需要遍历整个堆栈以取消s的链接。 如果有多个并发的clean调用，则如果另一个线程已将其删除，则可能看不到。
             * 但是，当我们看到任何已知跟随s的节点时，我们可以停止。 除非也将其取消，否则我们将使用s.next，在这种情况下，我们将尝试过去一个节点。 我们不做任何进一步的检查，因为我们不想为了找到标记而进行双重遍历。
             */
            /*
             * At worst we may need to traverse entire stack to unlink
             * s. If there are multiple concurrent calls to clean, we
             * might not see s if another thread has already removed
             * it. But we can stop when we see any node known to
             * follow s. We use s.next unless it too is cancelled, in
             * which case we try the node one past. We don't check any
             * further because we don't want to doubly traverse just to
             * find sentinel.
             */

            SNode past = s.next;
            if (past != null && past.isCancelled())
                past = past.next;

            // Absorb cancelled nodes at head
            SNode p;
            while ((p = head) != null && p != past && p.isCancelled())
                casHead(p, p.next);

            // Unsplice embedded nodes
            while (p != null && p != past) {
                SNode n = p.next;
                if (n != null && n.isCancelled())
                    p.casNext(n, n.next);
                else
                    p = n;
            }
        }

        // Unsafe mechanics
        private static final sun.misc.Unsafe UNSAFE;
        private static final long headOffset;
        static {
            try {
                UNSAFE = sun.misc.Unsafe.getUnsafe();
                Class<?> k = TransferStack.class;
                headOffset = UNSAFE.objectFieldOffset
                    (k.getDeclaredField("head"));
            } catch (Exception e) {
                throw new Error(e);
            }
        }
    }

    /** 双重结构之队列结构 */
    /** Dual Queue */
    static final class TransferQueue<E> extends Transferer<E> {
        /**
         * 20210524
         * 这扩展了Scherer-Scott双队列算法，其不同之处在于，通过使用节点内的模式而不是标记的指针来实现。 该算法比堆栈的算法更简单，因为实现者不需要显式节点，
         * 并且匹配是通过CAS的QNode.item字段从非null到null（用于放置）或反之亦然（用于获取）来完成的。
         */
        /*
         * This extends Scherer-Scott dual queue algorithm, differing,
         * among other ways, by using modes within nodes rather than
         * marked pointers. The algorithm is a little simpler than
         * that for stacks because fulfillers do not need explicit
         * nodes, and matching is done by CAS'ing QNode.item field
         * from non-null to null (for put) or vice versa (for take).
         */

        /** Node class for TransferQueue. */
        static final class QNode {
            volatile QNode next;          // next node in queue
            volatile Object item;         // CAS'ed to or from null
            volatile Thread waiter;       // to control park/unpark
            final boolean isData;

            QNode(Object item, boolean isData) {
                this.item = item;
                this.isData = isData;
            }

            boolean casNext(QNode cmp, QNode val) {
                return next == cmp &&
                    UNSAFE.compareAndSwapObject(this, nextOffset, cmp, val);
            }

            boolean casItem(Object cmp, Object val) {
                return item == cmp &&
                    UNSAFE.compareAndSwapObject(this, itemOffset, cmp, val);
            }

            /**
             * Tries to cancel by CAS'ing ref to this as item.
             */
            void tryCancel(Object cmp) {
                UNSAFE.compareAndSwapObject(this, itemOffset, cmp, this);
            }

            boolean isCancelled() {
                return item == this;
            }

            /**
             * 20210524
             * 如果已知此节点不在队列中，则返回true，因为该节点的下一个指针由于AdvanceHead操作而被遗忘。
             */
            /**
             * Returns true if this node is known to be off the queue
             * because its next pointer has been forgotten due to
             * an advanceHead operation.
             */
            boolean isOffList() {
                return next == this;
            }

            // Unsafe mechanics
            private static final sun.misc.Unsafe UNSAFE;
            private static final long itemOffset;
            private static final long nextOffset;

            static {
                try {
                    UNSAFE = sun.misc.Unsafe.getUnsafe();
                    Class<?> k = QNode.class;
                    itemOffset = UNSAFE.objectFieldOffset
                        (k.getDeclaredField("item"));
                    nextOffset = UNSAFE.objectFieldOffset
                        (k.getDeclaredField("next"));
                } catch (Exception e) {
                    throw new Error(e);
                }
            }
        }

        /** Head of queue */
        transient volatile QNode head;
        /** Tail of queue */
        transient volatile QNode tail;

        /**
         * 20210524
         * 对已取消节点的引用，该节点可能尚未取消与队列的链接，因为它是被取消时最后插入的节点。
         */
        /**
         * Reference to a cancelled node that might not yet have been
         * unlinked from queue because it was the last inserted node
         * when it was cancelled.
         */
        transient volatile QNode cleanMe;

        TransferQueue() {
            QNode h = new QNode(null, false); // initialize to dummy node.
            head = h;
            tail = h;
        }

        /**
         * 20210524
         * 尝试以cas nh作为新负责人； 如果成功，请断开旧头的下一个节点的链接以避免垃圾保留。
         */
        /**
         * Tries to cas nh as new head; if successful, unlink
         * old head's next node to avoid garbage retention.
         */
        void advanceHead(QNode h, QNode nh) {
            if (h == head &&
                UNSAFE.compareAndSwapObject(this, headOffset, h, nh))
                h.next = h; // forget old next
        }

        /**
         * Tries to cas nt as new tail.
         */
        void advanceTail(QNode t, QNode nt) {
            if (tail == t)
                UNSAFE.compareAndSwapObject(this, tailOffset, t, nt);
        }

        /**
         * Tries to CAS cleanMe slot.
         */
        boolean casCleanMe(QNode cmp, QNode val) {
            return cleanMe == cmp &&
                UNSAFE.compareAndSwapObject(this, cleanMeOffset, cmp, val);
        }

        /**
         * Puts or takes an item.
         */
        @SuppressWarnings("unchecked")
        E transfer(E e, boolean timed, long nanos) {

            /**
             * 20210524
             *
             * A.
             * 基本算法是循环尝试执行以下两个操作之一：
             * 1. 如果队列明显为空或持有相同模式的节点，请尝试将节点添加到等待队列中，等待被实现（或取消）并返回匹配项。
             * 2. 如果队列显然包含等待项，并且此调用是互补模式，请尝试通过CAS将等待节点的项字段放入队列并使其出队，然后返回匹配项来实现。
             *
             * B. 在每种情况下，一路检查并尝试帮助其他停滞/缓慢的线程推进头和尾。
             * C. 循环以空检查开始，以防止看到未初始化的头或尾值。 这在当前的SynchronousQueue中永远不会发生，但是如果调用者持有对传输者的非易失性/最终引用，
             *    则可能会发生这种情况。 无论如何，这里的检查是因为将空检查放在循环的顶部，通常比隐式散布检查要快。
             */
            /*
             * A.
             * Basic algorithm is to loop trying to take either of
             * two actions:
             *
             * 1. If queue apparently empty or holding same-mode nodes,
             *    try to add node to queue of waiters, wait to be
             *    fulfilled (or cancelled) and return matching item.
             *
             * 2. If queue apparently contains waiting items, and this
             *    call is of complementary mode, try to fulfill by CAS'ing
             *    item field of waiting node and dequeuing it, and then
             *    returning matching item.
             *
             * B.
             * In each case, along the way, check for and try to help
             * advance head and tail on behalf of other stalled/slow
             * threads.
             *
             * C.
             * The loop starts off with a null check guarding against
             * seeing uninitialized head or tail values. This never
             * happens in current SynchronousQueue, but could if
             * callers held non-volatile/final ref to the
             * transferer. The check is here anyway because it places
             * null checks at top of loop, which is usually faster
             * than having them implicitly interspersed.
             */
            // 35. 最后再offer(E), isData=true
            // 23. 再反过来, 先take(), isData=false
            // 13. poll(), isData=false
            // 0. put(E), isData=true
            QNode s = null; // constructed/reused as needed
            boolean isData = (e != null);

            for (;;) {
                // 36. offer(E), 此时head=上次put的结点, tail=take时的结点
                // 24. 这时head和tail都为上次put的结点, isData=true
                // 14. poll(), 此时head=初始的head, tail=put时的结点
                // 1. head和tail在构造函数就创建了, head=tail=new node(null, false)
                QNode t = tail;
                QNode h = head;
                if (t == null || h == null)         // saw uninitialized value
                    continue;                       // spin

                // 37. t.isData为take的isData即false, 不等于现在的true
                // 25. 这里的h也等于t
                // 15. t.isData为put的isData即true, 不等于现在的false
                // 2. 这里h等于t
                if (h == t || t.isData == isData) { // empty or same-mode
                    // 26. tn也为null
                    // 3. tn为null
                    QNode tn = t.next;
                    if (t != tail)                  // inconsistent read
                        continue;
                    if (tn != null) {               // lagging tail
                        advanceTail(t, tn);
                        continue;
                    }
                    if (timed && nanos <= 0)        // can't wait
                        return null;
                    // 27. s为null, 构造s结点[null, false]
                    // 4. s为null, 构造s结点[e, true]
                    if (s == null)
                        s = new QNode(e, isData);
                    // 28. s入队, 入队成功则CAS社会队尾为s结点, 设置s为新的队尾
                    // 5. s入队, 入队成功则CAS社会队尾为s结点, 设置s为新的队尾
                    if (!t.casNext(null, s))        // failed to link in
                        continue;
                    advanceTail(t, s);              // swing tail and wait

                    // 43. 与offer(E)线程同时进行: item被offer(E)线程赋值了, x为offer的元素e
                    // 34. 自旋 + 设置waiter阻塞线程 + LockSupport.park阻塞take线程
                    // 20. 与poll()线程同时进行: item被poll()线程置空了, x为null
                    // 12. 自旋 + 设置waiter阻塞线程 + LockSupport.park阻塞put线程
                    Object x = awaitFulfill(s, e, timed, nanos);

                    // 44. x为e, s为take的结点
                    // 21. x为null, s为put的结点
                    if (x == s) {                   // wait was cancelled
                        clean(t, s);
                        return null;
                    }

                    // 45. s为新的头和尾结点, x为e, 则返回x, 即为offer(E)时的元素
                    // 22. s为新的头和尾结点, x为null, 则返回put时的元素
                    if (!s.isOffList()) {           // not already unlinked
                        advanceHead(t, s);          // unlink if head
                        if (x != null)              // and forget fields
                            s.item = s;
                        s.waiter = null;
                    }
                    return (x != null) ? (E)x : e;

                }
                else {                            // complementary-mode
                    // 38. t等于tail, m为t, h为head
                    // 16. t等于tail, m为t, h为head
                    QNode m = h.next;               // node to fulfill
                    if (t != tail || m == null || h != head)
                        continue;                   // inconsistent read

                    // 39. x为take时的元素, isData=true, x为空, x不为take的结点即还没取消put的线程, 则尝试CAS交换x为当前offer(E)的e
                    // 17. x为put时的元素, isData=false, x不为空, x不为put的结点即还没取消put的线程, 则尝试CAS交换x为null
                    Object x = m.item;
                    if (isData == (x != null) ||    // m already fulfilled
                        x == m ||                   // m cancelled
                        !m.casItem(x, e)) {         // lost CAS
                        advanceHead(h, m);          // dequeue and retry
                        continue;
                    }

                    // 40. 设置了take时的元素为当前元素e后, 继续交换take的结点为头结点(这时与原头结点差不多, 但isData为false)
                    // 18. 置空put时的元素后, 继续交换put的结点为头结点(这时与原头结点差不多, 但isData为true)
                    advanceHead(h, m);              // successfully fulfilled
                    // 41. 唤醒take()线程
                    // 19. 唤醒put(E)线程
                    LockSupport.unpark(m.waiter);
                    // 42. 与take()线程同时进行: x为null, 这时返回当前元素e, 结束offer(E)方法
                    // 20. 与put(E)线程同时进行: x不为null, 为put时的元素, 这时返回x, 结束poll()方法
                    return (x != null) ? (E)x : e;
                }
            }
        }

        /**
         * 20210524
         * 自旋/阻塞，直到满足节点s为止。
         */
        /**
         * Spins/blocks until node s is fulfilled.
         *
         * @param s the waiting node
         * @param e the comparison value for checking match
         * @param timed true if timed wait
         * @param nanos timeout value
         * @return matched item, or s if cancelled
         */
        Object awaitFulfill(QNode s, E e, boolean timed, long nanos) {
            // 29. take(): 插入的s结点, 当前插入的元素, false, 0 => deadline = 0
            // 6. put(E): 插入的s结点, 当前插入的元素, false, 0 => deadline = 0
            /* Same idea as TransferStack.awaitFulfill */
            final long deadline = timed ? System.nanoTime() + nanos : 0L;
            Thread w = Thread.currentThread();

            // 30. take(): 此时head.next确实等于s, timed=false => 自旋32*16=512次
            // 7. put(E): 此时head.next确实等于s, timed=false => 自旋32*16=512次
            int spins = ((head.next == s) ? (timed ? maxTimedSpins : maxUntimedSpins) : 0);
            for (;;) {
                if (w.isInterrupted())
                    // 如果线程被中断的话, 则设置item为结点
                    s.tryCancel(e);

                // 42. 与offer(E)线程同时进行: item被offer(E)线程赋值了, x不为null了, 代表与offer(E)配对了, 这时返回offer的元素e
                // 31. take(): 这时x确实为s的元素, 即没有被中断也没有offer(E)进行配对
                // 20. 与poll()线程同时进行: item被poll()线程置空了, x不为e了, 代表与poll()配对了, 这时返回null
                // 8. put(E): 这时x确实为s的元素, 即没有被中断也没有poll()进行配对
                Object x = s.item;
                if (x != e)
                    return x;
                if (timed) {
                    nanos = deadline - System.nanoTime();
                    if (nanos <= 0L) {
                        s.tryCancel(e);
                        continue;
                    }
                }
                // 32. 更新自旋次数, 继续自旋
                // 9. 更新自旋次数, 继续自旋
                if (spins > 0)
                    --spins;
                // 33. 自旋结束后, 设置waiter阻塞线程为当前take()线程
                // 10. 自旋结束后, 设置waiter阻塞线程为当前put(E)线程
                else if (s.waiter == null)
                    s.waiter = w;
                // 33. 如果自旋结束, 也设置了waiter线程, timed又为false, 则调用LockSupport.park阻塞当前线程
                // 11. 如果自旋结束, 也设置了waiter线程, timed又为false, 则调用LockSupport.park阻塞当前线程
                else if (!timed)
                    LockSupport.park(this);
                else if (nanos > spinForTimeoutThreshold)
                    LockSupport.parkNanos(this, nanos);
            }
        }

        /**
         * 20210524
         * 删除具有原始前任pred的已取消节点s。
         */
        /**
         * Gets rid of cancelled node s with original predecessor pred.
         */
        void clean(QNode pred, QNode s) {
            s.waiter = null; // forget thread

            /**
             * 20210524
             * 在任何给定时间，列表中的一个节点都无法删除-最后插入的节点。 为了解决这个问题，如果我们不能删除s，我们将其前身保存为“ cleanMe”，首先删除之前保存的版本。
             * 节点s或先前保存的节点中的至少一个始终可以被删除，因此该操作始终终止。
             */
            /*
             * At any given time, exactly one node on list cannot be
             * deleted -- the last inserted node. To accommodate this,
             * if we cannot delete s, we save its predecessor as
             * "cleanMe", deleting the previously saved version
             * first. At least one of node s or the node previously
             * saved can always be deleted, so this always terminates.
             */
            while (pred.next == s) { // Return early if already unlinked
                QNode h = head;
                QNode hn = h.next;   // Absorb cancelled first node as head
                if (hn != null && hn.isCancelled()) {
                    advanceHead(h, hn);
                    continue;
                }
                QNode t = tail;      // Ensure consistent read for tail
                if (t == h)
                    return;
                QNode tn = t.next;
                if (t != tail)
                    continue;
                if (tn != null) {
                    advanceTail(t, tn);
                    continue;
                }
                if (s != t) {        // If not tail, try to unsplice
                    QNode sn = s.next;
                    if (sn == s || pred.casNext(s, sn))
                        return;
                }
                QNode dp = cleanMe;
                if (dp != null) {    // Try unlinking previous cancelled node
                    QNode d = dp.next;
                    QNode dn;
                    if (d == null ||               // d is gone or
                        d == dp ||                 // d is off list or
                        !d.isCancelled() ||        // d not cancelled or
                        (d != t &&                 // d not tail and
                         (dn = d.next) != null &&  //   has successor
                         dn != d &&                //   that is on list
                         dp.casNext(d, dn)))       // d unspliced
                        casCleanMe(dp, null);
                    if (dp == pred)
                        return;      // s is already saved node
                } else if (casCleanMe(null, pred))
                    return;          // Postpone cleaning s
            }
        }

        private static final sun.misc.Unsafe UNSAFE;
        private static final long headOffset;
        private static final long tailOffset;
        private static final long cleanMeOffset;
        static {
            try {
                UNSAFE = sun.misc.Unsafe.getUnsafe();
                Class<?> k = TransferQueue.class;
                headOffset = UNSAFE.objectFieldOffset
                    (k.getDeclaredField("head"));
                tailOffset = UNSAFE.objectFieldOffset
                    (k.getDeclaredField("tail"));
                cleanMeOffset = UNSAFE.objectFieldOffset
                    (k.getDeclaredField("cleanMe"));
            } catch (Exception e) {
                throw new Error(e);
            }
        }
    }

    /**
     * 20210524
     * 转移者。仅在构造函数中设置，但在不使序列化进一步复杂的情况下不能将其声明为final。 由于每个公用方法最多只能访问一次，
     * 因此在这里使用volatile而不是final不会造成明显的性能损失
     */
    /**
     * The transferer. Set only in constructor, but cannot be declared
     * as final without further complicating serialization.  Since
     * this is accessed only at most once per public method, there
     * isn't a noticeable performance penalty for using volatile
     * instead of final here.
     */
    private transient volatile Transferer<E> transferer;

    /**
     * 20210524
     * 创建具有不公平访问策略的{@code SynchronousQueue}。
     */
    /**
     * Creates a {@code SynchronousQueue} with nonfair access policy.
     */
    public SynchronousQueue() {
        this(false);
    }

    /**
     * Creates a {@code SynchronousQueue} with the specified fairness policy.
     *
     * @param fair if true, waiting threads contend in FIFO order for
     *        access; otherwise the order is unspecified.
     */
    public SynchronousQueue(boolean fair) {
        transferer = fair ? new TransferQueue<E>() : new TransferStack<E>();
    }

    /**
     * Adds the specified element to this queue, waiting if necessary for
     * another thread to receive it.
     *
     * @throws InterruptedException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     */
    public void put(E e) throws InterruptedException {
        if (e == null) throw new NullPointerException();
        if (transferer.transfer(e, false, 0) == null) {
            Thread.interrupted();
            throw new InterruptedException();
        }
    }

    /**
     * Inserts the specified element into this queue, waiting if necessary
     * up to the specified wait time for another thread to receive it.
     *
     * @return {@code true} if successful, or {@code false} if the
     *         specified waiting time elapses before a consumer appears
     * @throws InterruptedException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     */
    public boolean offer(E e, long timeout, TimeUnit unit)
        throws InterruptedException {
        if (e == null) throw new NullPointerException();
        if (transferer.transfer(e, true, unit.toNanos(timeout)) != null)
            return true;
        if (!Thread.interrupted())
            return false;
        throw new InterruptedException();
    }

    /**
     * Inserts the specified element into this queue, if another thread is
     * waiting to receive it.
     *
     * @param e the element to add
     * @return {@code true} if the element was added to this queue, else
     *         {@code false}
     * @throws NullPointerException if the specified element is null
     */
    public boolean offer(E e) {
        if (e == null) throw new NullPointerException();
        return transferer.transfer(e, true, 0) != null;
    }

    /**
     * Retrieves and removes the head of this queue, waiting if necessary
     * for another thread to insert it.
     *
     * @return the head of this queue
     * @throws InterruptedException {@inheritDoc}
     */
    public E take() throws InterruptedException {
        E e = transferer.transfer(null, false, 0);
        if (e != null)
            return e;
        Thread.interrupted();
        throw new InterruptedException();
    }

    /**
     * Retrieves and removes the head of this queue, waiting
     * if necessary up to the specified wait time, for another thread
     * to insert it.
     *
     * @return the head of this queue, or {@code null} if the
     *         specified waiting time elapses before an element is present
     * @throws InterruptedException {@inheritDoc}
     */
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        E e = transferer.transfer(null, true, unit.toNanos(timeout));
        if (e != null || !Thread.interrupted())
            return e;
        throw new InterruptedException();
    }

    /**
     * Retrieves and removes the head of this queue, if another thread
     * is currently making an element available.
     *
     * @return the head of this queue, or {@code null} if no
     *         element is available
     */
    public E poll() {
        return transferer.transfer(null, true, 0);
    }

    /**
     * Always returns {@code true}.
     * A {@code SynchronousQueue} has no internal capacity.
     *
     * @return {@code true}
     */
    public boolean isEmpty() {
        return true;
    }

    /**
     * Always returns zero.
     * A {@code SynchronousQueue} has no internal capacity.
     *
     * @return zero
     */
    public int size() {
        return 0;
    }

    /**
     * Always returns zero.
     * A {@code SynchronousQueue} has no internal capacity.
     *
     * @return zero
     */
    public int remainingCapacity() {
        return 0;
    }

    /**
     * Does nothing.
     * A {@code SynchronousQueue} has no internal capacity.
     */
    public void clear() {
    }

    /**
     * Always returns {@code false}.
     * A {@code SynchronousQueue} has no internal capacity.
     *
     * @param o the element
     * @return {@code false}
     */
    public boolean contains(Object o) {
        return false;
    }

    /**
     * Always returns {@code false}.
     * A {@code SynchronousQueue} has no internal capacity.
     *
     * @param o the element to remove
     * @return {@code false}
     */
    public boolean remove(Object o) {
        return false;
    }

    /**
     * Returns {@code false} unless the given collection is empty.
     * A {@code SynchronousQueue} has no internal capacity.
     *
     * @param c the collection
     * @return {@code false} unless given collection is empty
     */
    public boolean containsAll(Collection<?> c) {
        return c.isEmpty();
    }

    /**
     * Always returns {@code false}.
     * A {@code SynchronousQueue} has no internal capacity.
     *
     * @param c the collection
     * @return {@code false}
     */
    public boolean removeAll(Collection<?> c) {
        return false;
    }

    /**
     * Always returns {@code false}.
     * A {@code SynchronousQueue} has no internal capacity.
     *
     * @param c the collection
     * @return {@code false}
     */
    public boolean retainAll(Collection<?> c) {
        return false;
    }

    /**
     * Always returns {@code null}.
     * A {@code SynchronousQueue} does not return elements
     * unless actively waited on.
     *
     * @return {@code null}
     */
    public E peek() {
        return null;
    }

    /**
     * Returns an empty iterator in which {@code hasNext} always returns
     * {@code false}.
     *
     * @return an empty iterator
     */
    public Iterator<E> iterator() {
        return Collections.emptyIterator();
    }

    /**
     * Returns an empty spliterator in which calls to
     * {@link java.util.Spliterator#trySplit()} always return {@code null}.
     *
     * @return an empty spliterator
     * @since 1.8
     */
    public Spliterator<E> spliterator() {
        return Spliterators.emptySpliterator();
    }

    /**
     * Returns a zero-length array.
     * @return a zero-length array
     */
    public Object[] toArray() {
        return new Object[0];
    }

    /**
     * Sets the zeroeth element of the specified array to {@code null}
     * (if the array has non-zero length) and returns it.
     *
     * @param a the array
     * @return the specified array
     * @throws NullPointerException if the specified array is null
     */
    public <T> T[] toArray(T[] a) {
        if (a.length > 0)
            a[0] = null;
        return a;
    }

    /**
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws ClassCastException            {@inheritDoc}
     * @throws NullPointerException          {@inheritDoc}
     * @throws IllegalArgumentException      {@inheritDoc}
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
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws ClassCastException            {@inheritDoc}
     * @throws NullPointerException          {@inheritDoc}
     * @throws IllegalArgumentException      {@inheritDoc}
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

    /*
     * To cope with serialization strategy in the 1.5 version of
     * SynchronousQueue, we declare some unused classes and fields
     * that exist solely to enable serializability across versions.
     * These fields are never used, so are initialized only if this
     * object is ever serialized or deserialized.
     */

    @SuppressWarnings("serial")
    static class WaitQueue implements java.io.Serializable { }
    static class LifoWaitQueue extends WaitQueue {
        private static final long serialVersionUID = -3633113410248163686L;
    }
    static class FifoWaitQueue extends WaitQueue {
        private static final long serialVersionUID = -3623113410248163686L;
    }
    private ReentrantLock qlock;
    private WaitQueue waitingProducers;
    private WaitQueue waitingConsumers;

    /**
     * Saves this queue to a stream (that is, serializes it).
     * @param s the stream
     * @throws java.io.IOException if an I/O error occurs
     */
    private void writeObject(java.io.ObjectOutputStream s)
        throws java.io.IOException {
        boolean fair = transferer instanceof TransferQueue;
        if (fair) {
            qlock = new ReentrantLock(true);
            waitingProducers = new FifoWaitQueue();
            waitingConsumers = new FifoWaitQueue();
        }
        else {
            qlock = new ReentrantLock();
            waitingProducers = new LifoWaitQueue();
            waitingConsumers = new LifoWaitQueue();
        }
        s.defaultWriteObject();
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
        if (waitingProducers instanceof FifoWaitQueue)
            transferer = new TransferQueue<E>();
        else
            transferer = new TransferStack<E>();
    }

    // Unsafe mechanics
    static long objectFieldOffset(sun.misc.Unsafe UNSAFE,
                                  String field, Class<?> klazz) {
        try {
            return UNSAFE.objectFieldOffset(klazz.getDeclaredField(field));
        } catch (NoSuchFieldException e) {
            // Convert Exception to corresponding Error
            NoSuchFieldError error = new NoSuchFieldError(field);
            error.initCause(e);
            throw error;
        }
    }

}
