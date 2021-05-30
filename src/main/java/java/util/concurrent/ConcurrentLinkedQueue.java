/*
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

/*
 * Written by Doug Lea and Martin Buchholz with assistance from members of
 * JCP JSR-166 Expert Group and released to the public domain, as explained
 * at http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util.concurrent;

import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;

/**
 * 20210526
 * A. 基于链接节点的无界线程安全队列。 此队列对元素FIFO（先进先出）进行排序。 队列的开头是该元素在队列中停留时间最长的元素。 队列的尾部是最短时间出现在队列中的元素。
 *    新元素插入到队列的尾部，并且队列检索操作在队列的开头获取元素。 当许多线程将共享对一个公共集合的访问权限时，{@code ConcurrentLinkedQueue}是一个适当的选择。
 *    与大多数其他并发集合实现一样，此类不允许使用{@code null}元素。
 * B. 此实现采用一种有效的非阻塞算法，该算法基于简单，快速且实用的非阻塞和自适应的阻塞并发队列算法。
 *    <a href="http://www.cs.rochester.edu/u/michael/PODC96.html">Maged M. Michael和Michael L.Scott</a>。
 * C. 迭代器是弱一致性的，返回的元素在创建迭代器时或创建迭代器后的某个时刻反映队列的状态。 它们不会抛出{@link java.util.ConcurrentModificationException}，
 *    并且可能与其他操作并发进行。 自创建迭代器以来，队列中包含的元素将仅返回一次。
 * D. 注意，与大多数集合不同，{@code size}方法不是恒定时间操作。 由于这些队列的异步性质，确定当前元素数需要对元素进行遍历，因此，如果在遍历期间修改了此集合，
 *    则可能会报告不准确的结果。 此外，不能保证批量操作{@code addAll}，{@code removeAll}，{@code keepAll}，{@code containsAll}，
 *    {@code equals}和{@code toArray}是原子执行的。 例如，与{@code addAll}操作同时运行的迭代器可能只查看一些添加的元素。
 * E. 此类及其迭代器实现{@link Queue}和{@link Iterator}接口的所有可选方法。
 * F. 内存一致性影响：与其他并发集合一样，在将对象放入{@code ConcurrentLinkedQueue} <a href="package-summary.html#MemoryVisibility">发生之前</a>之后的线程中，
 *    线程中的操作如下： 在另一个线程中从{@code ConcurrentLinkedQueue}访问或删除该元素。
 * G. {@docRoot}/../technotes/guides/collections/index.html
 */

/**
 * A.
 * An unbounded thread-safe {@linkplain Queue queue} based on linked nodes.
 * This queue orders elements FIFO (first-in-first-out).
 * The <em>head</em> of the queue is that element that has been on the
 * queue the longest time.
 * The <em>tail</em> of the queue is that element that has been on the
 * queue the shortest time. New elements
 * are inserted at the tail of the queue, and the queue retrieval
 * operations obtain elements at the head of the queue.
 * A {@code ConcurrentLinkedQueue} is an appropriate choice when
 * many threads will share access to a common collection.
 * Like most other concurrent collection implementations, this class
 * does not permit the use of {@code null} elements.
 *
 * B.
 * <p>This implementation employs an efficient <em>non-blocking</em>
 * algorithm based on one described in <a
 * href="http://www.cs.rochester.edu/u/michael/PODC96.html"> Simple,
 * Fast, and Practical Non-Blocking and Blocking Concurrent Queue
 * Algorithms</a> by Maged M. Michael and Michael L. Scott.
 *
 * C.
 * <p>Iterators are <i>weakly consistent</i>, returning elements
 * reflecting the state of the queue at some point at or since the
 * creation of the iterator.  They do <em>not</em> throw {@link
 * java.util.ConcurrentModificationException}, and may proceed concurrently
 * with other operations.  Elements contained in the queue since the creation
 * of the iterator will be returned exactly once.
 *
 * D.
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
 * E.
 * <p>This class and its iterator implement all of the <em>optional</em>
 * methods of the {@link Queue} and {@link Iterator} interfaces.
 *
 * F.
 * <p>Memory consistency effects: As with other concurrent
 * collections, actions in a thread prior to placing an object into a
 * {@code ConcurrentLinkedQueue}
 * <a href="package-summary.html#MemoryVisibility"><i>happen-before</i></a>
 * actions subsequent to the access or removal of that element from
 * the {@code ConcurrentLinkedQueue} in another thread.
 *
 * G.
 * <p>This class is a member of the
 * <a href="{@docRoot}/../technotes/guides/collections/index.html">
 * Java Collections Framework</a>.
 *
 * @since 1.5
 * @author Doug Lea
 * @param <E> the type of elements held in this collection
 */
public class ConcurrentLinkedQueue<E> extends AbstractQueue<E> implements Queue<E>, java.io.Serializable {
    private static final long serialVersionUID = 196745693267521676L;

    /**
     * 20210527
     * A. 这是对Michael＆Scott算法的修改，适用于垃圾回收环境，并支持内部节点删除（以支持remove（Object））。 要进行解释，请阅读本文。
     * B. 请注意，与该程序包中的大多数非阻塞算法一样，此实现依赖于以下事实：在垃圾回收系统中，由于回收节点而不会出现ABA问题，因此无需使用“计数指针”或相关技术。
     *    在非GC设置中使用的版本中可以看到。
     * C. 基本不变式是：
     *    1) 恰好有一个（最后一个）节点，其下一个引用为空，入队时将进行CAS附加。 可以从尾部的O（1）时间到达最后一个节点，
     *       但是尾部仅仅是一种优化-它也总是可以从头部的O（N）时间到达。
     *    2) 队列中包含的元素是节点中从头可到达的非空项目。 将Node的项目引用CAS原子化为null会将其从队列中删除。 即使在并行修改导致头部前进的情况下，
     *       头部所有元素的可达性也必须保持正确。 由于创建了Iterator或只是丢失了其时间片的poll（），出队的节点可能会无限期地保持使用状态。
     * D. 上面的内容似乎暗示着所有节点都可以从先前的出队节点通过GC到达。 这将导致两个问题：
     *    1) 允许恶意的Iterator导致无限的内存保留。
     *    2) 如果某个节点在使用中处于使用期，则会导致旧节点与新节点的跨代链接，这会导致一代代GC难以处理，从而主要导致重复的集合。
     * E. 头和尾都允许滞后。 实际上，每次都无法更新它们是一个重大的优化（较少的CASes）。 与LinkedTransferQueue一样（请参阅该类的内部文档），我们使用2的松弛阈值。
     *    也就是说，当当前指针似乎与第一个/最后一个节点相距两步或更多步时，我们将更新头/尾。
     * F. 由于头和尾同时并独立地更新，所以尾可能会滞后于头（为什么不这样）？
     * G. 将Node的项目引用CAS原子化为null会从队列中删除该元素。 迭代器跳过具有空项目的节点。 此类的先前实现在poll（）和remove（Object）之间存在竞争，
     *    其中相同的元素似乎可以通过两次并发操作成功删除。 方法remove（Object）也懒惰地取消链接已删除的节点，但这仅仅是一种优化。
     * H. 在构造Node时（在将其放入队列之前），我们避免使用Unsafe.putObject而不是常规写入来为项目进行易失性写入。 这使得入队成本成为“一个半”的案例。
     * I. 头部和尾部都可能指向也可能不指向带有非空项目的节点。 如果队列为空，则所有项目当然必须为空。 创建后，头和尾都引用具有空项目的虚拟节点。
     *    头部和尾部都仅使用CAS进行更新，因此它们永远不会回归，尽管这只是一种优化。
     */
    /*
     * A.
     * This is a modification of the Michael & Scott algorithm,
     * adapted for a garbage-collected environment, with support for
     * interior node deletion (to support remove(Object)).  For
     * explanation, read the paper.
     *
     * B.
     * Note that like most non-blocking algorithms in this package,
     * this implementation relies on the fact that in garbage
     * collected systems, there is no possibility of ABA problems due
     * to recycled nodes, so there is no need to use "counted
     * pointers" or related techniques seen in versions used in
     * non-GC'ed settings.
     *
     * C.
     * The fundamental invariants are:
     * - There is exactly one (last) Node with a null next reference,
     *   which is CASed when enqueueing.  This last Node can be
     *   reached in O(1) time from tail, but tail is merely an
     *   optimization - it can always be reached in O(N) time from
     *   head as well.
     * - The elements contained in the queue are the non-null items in
     *   Nodes that are reachable from head.  CASing the item
     *   reference of a Node to null atomically removes it from the
     *   queue.  Reachability of all elements from head must remain
     *   true even in the case of concurrent modifications that cause
     *   head to advance.  A dequeued Node may remain in use
     *   indefinitely due to creation of an Iterator or simply a
     *   poll() that has lost its time slice.
     *
     * D.
     * The above might appear to imply that all Nodes are GC-reachable
     * from a predecessor dequeued Node.  That would cause two problems:
     * - allow a rogue Iterator to cause unbounded memory retention
     * - cause cross-generational linking of old Nodes to new Nodes if
     *   a Node was tenured while live, which generational GCs have a
     *   hard time dealing with, causing repeated major collections.
     * However, only non-deleted Nodes need to be reachable from
     * dequeued Nodes, and reachability does not necessarily have to
     * be of the kind understood by the GC.  We use the trick of
     * linking a Node that has just been dequeued to itself.  Such a
     * self-link implicitly means to advance to head.
     *
     * E.
     * Both head and tail are permitted to lag.  In fact, failing to
     * update them every time one could is a significant optimization
     * (fewer CASes). As with LinkedTransferQueue (see the internal
     * documentation for that class), we use a slack threshold of two;
     * that is, we update head/tail when the current pointer appears
     * to be two or more steps away from the first/last node.
     *
     * F.
     * Since head and tail are updated concurrently and independently,
     * it is possible for tail to lag behind head (why not)?
     *
     * G.
     * CASing a Node's item reference to null atomically removes the
     * element from the queue.  Iterators skip over Nodes with null
     * items.  Prior implementations of this class had a race between
     * poll() and remove(Object) where the same element would appear
     * to be successfully removed by two concurrent operations.  The
     * method remove(Object) also lazily unlinks deleted Nodes, but
     * this is merely an optimization.
     *
     * H.
     * When constructing a Node (before enqueuing it) we avoid paying
     * for a volatile write to item by using Unsafe.putObject instead
     * of a normal write.  This allows the cost of enqueue to be
     * "one-and-a-half" CASes.
     *
     * I.
     * Both head and tail may or may not point to a Node with a
     * non-null item.  If the queue is empty, all items must of course
     * be null.  Upon creation, both head and tail refer to a dummy
     * Node with null item.  Both head and tail are only updated using
     * CAS, so they never regress, although again this is merely an
     * optimization.
     */

    private static class Node<E> {
        volatile E item;
        volatile Node<E> next;

        /**
         * 20210527
         * 构造一个新节点。 使用轻松写入，因为只有在通过casNext发布后才能看到项目。
         */
        /**
         * Constructs a new node.  Uses relaxed write because item can
         * only be seen after publication via casNext.
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

        // Unsafe mechanics

        private static final sun.misc.Unsafe UNSAFE;
        private static final long itemOffset;
        private static final long nextOffset;

        static {
            try {
                UNSAFE = sun.misc.Unsafe.getUnsafe();
                Class<?> k = Node.class;
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
     * 20210527
     * A. 可以在O（1）时间内从中到达第一个活动（未删除）节点（如果有）的节点。
     * B. 不变性：
     *      1) 所有活动节点都可以通过succ（）从头结点到达。
     *      2) head != null
     *      3) (tmp = head).next != tmp || tmp != head => 即head的next结点永远不会指向自己
     * C. 可变性：
     *      1) head.item可以为null，也可以不为null。
     *      2) 允许尾指针滞后于头指针，也就是说，此时不能从头结点访问到尾结点。
     */
    /**
     * A.
     * A node from which the first live (non-deleted) node (if any)
     * can be reached in O(1) time.
     *
     * B.
     * Invariants:
     * - all live nodes are reachable from head via succ()
     * - head != null
     * - (tmp = head).next != tmp || tmp != head
     * Non-invariants:
     * - head.item may or may not be null.
     * - it is permitted for tail to lag behind head, that is, for tail
     *   to not be reachable from head!
     */
    private transient volatile Node<E> head;

    /**
     * 20210527
     * A. 可以在O（1）时间内从中到达列表中的最后一个节点的节点（即，具有node.next == null的唯一节点）。
     * B. 不变形：
     *      1) 最后一个节点始终可以通过succ（）从尾结点到达。
     *      2) tail != null
     * C. 可变性：
     *      1) tail.item可以为null，也可以不为null。
     *      2) 允许尾指针滞后于头指针，也就是说，此时不能从头结点访问到尾结点。
     *      3) tail.next可能会或可能不会自动指向tail。
     */
    /**
     * A node from which the last node on list (that is, the unique
     * node with node.next == null) can be reached in O(1) time.
     * Invariants:
     * - the last node is always reachable from tail via succ()
     * - tail != null
     * Non-invariants:
     * - tail.item may or may not be null.
     * - it is permitted for tail to lag behind head, that is, for tail
     *   to not be reachable from head!
     * - tail.next may or may not be self-pointing to tail.
     */
    private transient volatile Node<E> tail;

    /**
     * Creates a {@code ConcurrentLinkedQueue} that is initially empty.
     */
    public ConcurrentLinkedQueue() {
        head = tail = new Node<E>(null);
    }

    /**
     * 20210526
     * 创建一个{@code ConcurrentLinkedQueue}，它最初包含给定集合的元素，并按集合的迭代器的遍历顺序添加。
     */
    /**
     * Creates a {@code ConcurrentLinkedQueue}
     * initially containing the elements of the given collection,
     * added in traversal order of the collection's iterator.
     *
     * @param c the collection of elements to initially contain
     * @throws NullPointerException if the specified collection or any
     *         of its elements are null
     */
    public ConcurrentLinkedQueue(Collection<? extends E> c) {
        Node<E> h = null, t = null;
        for (E e : c) {
            checkNotNull(e);
            Node<E> newNode = new Node<E>(e);
            if (h == null)
                h = t = newNode;
            else {
                t.lazySetNext(newNode);
                t = newNode;
            }
        }
        if (h == null)
            h = t = new Node<E>(null);
        head = h;
        tail = t;
    }

    // Have to override just to update the javadoc

    /**
     * 20210526
     * 将指定的元素插入此队列的末尾。 由于队列是无界的，因此此方法将永远不会抛出{@link IllegalStateException}或返回{@code false}。
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
        return offer(e);
    }

    /**
     * 20210527
     * 尝试CAS至p。 如果成功，则将旧的头重新指向自己，作为下面succ（）的标记。
     */
    /**
     * Tries to CAS head to p. If successful, repoint old head to itself
     * as sentinel for succ(), below.
     */
    // CAS旧的头结点h为最新的头结点p, 把原来的h结点的next结点设置为它本身, 实现结点的“删除"
    final void updateHead(Node<E> h, Node<E> p) {
        // 如果当前h结点不是最新的头结点, 则CAS为最新的头结点p
        if (h != p && casHead(h, p))
            // 然后把原来的h结点的next结点设置为它本身, 实现结点的“删除"
            h.lazySetNext(h);
    }

    /**
     * 20210527
     * 返回p的后继者；如果p.next已链接到self，则返回头节点；仅当使用现在不在列表中的陈旧指针进行遍历时，才返回true。
     */
    /**
     * Returns the successor of p, or the head node if p.next has been
     * linked to self, which will only be true if traversing with a
     * stale pointer that is now off the list.
     */
    // 返回p的next结点, 如果为p自身则返回头结点
    final Node<E> succ(Node<E> p) {
        Node<E> next = p.next;
        return (p == next) ? head : next;
    }

    /**
     * 20210526
     * 将指定的元素插入此队列的末尾。 由于队列是无界的，因此此方法将永远不会返回{@code false}。
     */
    /**
     * Inserts the specified element at the tail of this queue.
     * As the queue is unbounded, this method will never return {@code false}.
     *
     * @return {@code true} (as specified by {@link Queue#offer})
     * @throws NullPointerException if the specified element is null
     */
    public boolean offer(E e) {
        checkNotNull(e);
        final Node<E> newNode = new Node<E>(e);

        // 1. t、p为当前尾指针
        for (Node<E> t = tail, p = t;;) {
            // 2. 防止并发, 所以每轮需要重新获取next结点, q为当前尾指针p的next结点
            // 5. (重新自旋)最新一次的自旋, 获取最新p结点的next结点q
            Node<E> q = p.next;

            // 3. 高并发无阻塞队列的插入是写死CAS为null才能插入的, 所以要自旋判断下是否为null
            if (q == null) {
                // 4. 如果q是null的话, 说明是能够CAS的, 这时CAS当前结点为next结点
                if (p.casNext(null, newNode)) {
                    // 20210527 成功的CAS是e成为该队列元素以及newNode成为“实时”对象的线性化点。
                    // Successful CAS is the linearization point for e to become an element of this queue, and for newNode to become "live".

                    // 5. CAS next结点成功, 则判断p!=t, 说明当前线程是一个需要修改tail指针的线程, 来完成上一个线程直接返回true的工作。
                    if (p != t) // hop two nodes at a time  // 一次跳两个节点
                        casTail(t, newNode);  // Failure is OK. // 失败是可以的。

                    // 5. CAS next结点成功, 则可以直接返回true了, 无需再修改tail指针, 是一种优化机制: 只有两跳才会修改tail指针, 把修改工作交给下一个线程来做, 提高吞吐量
                    return true;
                }
                // Lost CAS race to another thread; re-read next
            }
            // 3. 如果为自身的话, 说明自身结点被删除了(由于不想影响其他线程, 所以删除不能真的把结点删除, 而是把next指针指向它自己)
            else if (p == q)
                /**
                 * 20210526
                 * 我们已经不在名单之列了。
                 * 如果tail保持不变，那么它也将不在列表中，在这种情况下，我们需要跳到head位置，从那里所有活动节点始终都是可访问的。 否则，新的尾巴是更好的选择。
                 */
                // We have fallen off list.  If tail is unchanged, it
                // will also be off-list, in which case we need to
                // jump to head, from which all live nodes are always
                // reachable.  Else the new tail is a better bet.

                // 4. 如果t==tail, 说明已经最后一个结点都被删除了, 这时需要p=tail=head,
                //      而t!=tail说明当前结点被删除了且别的线程插入了新的结点还更新尾指针, 这时需要把最新的尾指针赋值给p, 重新自旋
                p = (t != (t = tail)) ? t : head;
            // 3. 如果q不是null, 又不是自身的话, 则继续判断
            else
                // 在两跳之后检查尾部更新。
                // Check for tail updates after two hops.

                // 4. 如果p==t或者tail不是最新的尾结点, 则赋值q给p, 说明别的线程添加了next结点, 但还没移动tail指针, 这时需要跳到q结点, 自旋到下一次重新获取q
                // 5. 如果p!=t且tail为最新的尾结点, 则赋值t给p, 即让p成为最新的尾结点, 继续自旋插入
                p = (p != t && t != (t = tail)) ? t : q;
        }
    }

    public E poll() {
        restartFromHead:
        for (;;) {
            // 1. h为头结点, p=h, q = null
            for (Node<E> h = head, p = h, q;;) {

                // 2. 防止并发, 所以每轮需要重新获取item值
                // 5. (重新自旋)最新的一次的自旋, 获取item值
                E item = p.item;

                // 3. 如果item不为null, 且能够CAS item为null, 说明p确实为头结点
                if (item != null && p.casItem(item, null)) {
                    // 成功的CAS是要从此队列中删除的项目的线性化点。
                    // Successful CAS is the linearization point
                    // for item to be removed from this queue.

                    // 4. CAS item为null后, 如果p!=h, 说明上一个线程只做了CAS item操作, 还没做移动head指针的操作 所以当前线程需要做head指针的移动操作
                    if (p != h) // hop two nodes at a time // 一次跳两个节点
                        // 5. 如果p的next结点为null, 说明p为尾结点了, 则更新当前head指针为当前结点, 否则更新head为next结点
                        //    CAS旧的头结点h为最新的头结点p, 把原来的h结点的next结点设置为它本身, 实现结点的“删除" => 两个线程才移动一次head指针, 提高吞吐量
                        updateHead(h, ((q = p.next) != null) ? q : p);

                    // 4. CAS item为null后, 可以直接返回了, 把head指针的更改交给下一个线程, 减少其他线程的自旋次数, 提高吞吐量
                    return item;
                }
                // 3. 如果item为null或者CAS item为null失败, 且p的next结点为null, 说明p为尾结点且当前线程为移动head指针线程
                else if ((q = p.next) == null) {
                    // 4. 这时CAS旧的头结点h为最新的头结点p, 把原来的h结点的next结点设置为它本身, 实现结点的“删除"
                    updateHead(h, p);

                    // 5. 返回null是因为, 有数据的结点已经在上个线程置空并返回了, 当前线程只需要为移动head指针即可, 所以返回null
                    return null;
                }
                // 3. 如果item为null或者CAS item为null失败, 且p又不是尾结点, 且p的next结点等于p自身, 说明p结点已经被删除, 且head指针已经被移动过了
                else if (p == q)
                    // 4. 由于poll()是CAS+自旋操作, 是要肯定成功的, 所以继续自旋, 去poll下一个元素, 但又要获取最新的head结点, 所以直接重新进入自旋, 而不是下一轮自旋
                    continue restartFromHead;
                // 3. 否则p item为null 捉着 CAS失败、且又不是尾结点、且又还没被删除, 说明上一个线程做了CAS item为null的操作, 但是还没移动head指针
                else
                    // 4. 所以把p跳到下一个结点q去, 由于poll()是CAS+自旋操作, 是要肯定成功的, 所以继续自旋, 以在适当的时候置null元素、移动head指针
                    p = q;
            }
        }
    }

    // 与first()方法实现类似, 只不过first()返回的是第一个item不为null的结点, 而peek()返回的是第一个不为null的item
    public E peek() {
        restartFromHead:
        for (;;) {
            // 1. h、p为头结点
            for (Node<E> h = head, p = h, q;;) {
                E item = p.item;

                // 2. 如果item不为null, 或者p为尾结点
                if (item != null || (q = p.next) == null) {
                    // 4. 如果item不为null, 更新h相当于没做任何更新; 如果p为尾结点, 更新h相当于head=tail
                    updateHead(h, p);

                    // 5. 如果item不为null, 则返回头结点h=p, 即第一个有item的结点指针, 否则返回null
                    return item;
                }

                // 3. 如果p已经被删除了, 则重新更新head指针, 重新进入自旋
                else if (p == q)
                    continue restartFromHead;

                // 如果item为null且p也不是尾结点也没有被删除, 则移动p指针到next结点, 继续自旋, 找出第一个item不为null的结点
                else
                    p = q;
            }
        }
    }

    /**
     * 20210529
     * 返回列表中的第一个活动（未删除）节点，如果没有，则返回 null。 这是 poll/peek 的另一种变体； 这里返回第一个节点，而不是元素。
     * 我们可以使 peek() 成为 first() 的包装器，但这将花费额外的 item 读取不稳定，并且需要添加重试循环来处理与并发 poll() 竞争失败的可能性。
     */
    /**
     * Returns the first live (non-deleted) node on list, or null if none.
     * This is yet another variant of poll/peek; here returning the
     * first node, not element.  We could make peek() a wrapper around
     * first(), but that would cost an extra volatile read of item,
     * and the need to add a retry loop to deal with the possibility
     * of losing a race to a concurrent poll().
     */
    // 找出第一个item不为null的结点 或者head=tail的结点
    Node<E> first() {
        restartFromHead:
        for (;;) {
            // 1. h、p为头结点
            for (Node<E> h = head, p = h, q;;) {
                // 2. 判断item是否为null
                boolean hasItem = (p.item != null);

                // 3. 如果item不为null, 或者p为尾结点
                if (hasItem || (q = p.next) == null) {
                    // 4. 如果item不为null, 更新h相当于没做任何更新; 如果p为尾结点, 更新h相当于head=tail
                    updateHead(h, p);

                    // 5. 如果item不为null, 则返回头结点h=p, 即第一个有item的结点指针
                    return hasItem ? p : null;
                }

                // 3. 如果p已经被删除了, 则重新更新head指针, 重新进入自旋
                else if (p == q)
                    continue restartFromHead;

                // 3. 如果item为null且p也不是尾结点也没有被删除, 则移动p指针到next结点, 继续自旋, 找出第一个item不为null的结点
                else
                    p = q;
            }
        }
    }

    /**
     * Returns {@code true} if this queue contains no elements.
     *
     * @return {@code true} if this queue contains no elements
     */
    public boolean isEmpty() {
        return first() == null;
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
     * Additionally, if elements are added or removed during execution
     * of this method, the returned result may be inaccurate.  Thus,
     * this method is typically not very useful in concurrent
     * applications.
     *
     * @return the number of elements in this queue
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
     * Returns {@code true} if this queue contains the specified element.
     * More formally, returns {@code true} if and only if this queue contains
     * at least one element {@code e} such that {@code o.equals(e)}.
     *
     * @param o object to be checked for containment in this queue
     * @return {@code true} if this queue contains the specified element
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
     * 20210529
     * A. 从此队列中移除指定元素的单个实例（如果存在）。 更正式地，如果此队列包含一个或多个此类元素，则删除元素 {@code e} 使得 {@code o.equals(e)}。
     * B. 如果此队列包含指定的元素（或等效地，如果此队列因调用而更改），则返回 {@code true}。
     */
    /**
     * A.
     * Removes a single instance of the specified element from this queue,
     * if it is present.  More formally, removes an element {@code e} such
     * that {@code o.equals(e)}, if this queue contains one or more such
     * elements.
     *
     * B.
     * Returns {@code true} if this queue contained the specified element
     * (or equivalently, if this queue changed as a result of the call).
     *
     * @param o element to be removed from this queue, if present
     * @return {@code true} if this queue changed as a result of the call
     */
    public boolean remove(Object o) {
        if (o == null) return false;
        Node<E> pred = null;

        // 找出第一个item不为null的结点 或者head=tail的结点 => 从头开始遍历
        for (Node<E> p = first(); p != null;
             // 返回p的next结点, 如果为p自身则返回头结点 => 每次遍历结束, 都需要把p移动到下一个结点, 开启下一轮遍历
             p = succ(p)) {

            // 当前item
            E item = p.item;
            if (item != null && // 当前item不为null
                o.equals(item) && // 且equals指定元素
                p.casItem(item, null)) {// 这时则CAS置item为null

                // CAS置空item成功后, 如果为第一个结点或者为尾结点, 则直接返回就好, 移动head指针交由下一个线程负责, 而如果不是第一个结点也不是尾结点, 说明需要移动head指针
                Node<E> next = succ(p);
                if (pred != null && next != null)
                    // 移动head指针到p的next结点处
                    pred.casNext(p, next);
                return true;
            }

            // pred为p的前驱结点, 为null代表p为第一个结点
            pred = p;
        }

        // 如果遍历完还没找到, 则返回false
        return false;
    }

    /**
     * Appends all of the elements in the specified collection to the end of
     * this queue, in the order that they are returned by the specified
     * collection's iterator.  Attempts to {@code addAll} of a queue to
     * itself result in {@code IllegalArgumentException}.
     *
     * @param c the elements to be inserted into this queue
     * @return {@code true} if this queue changed as a result of the call
     * @throws NullPointerException if the specified collection or any
     *         of its elements are null
     * @throws IllegalArgumentException if the collection is this queue
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
                last = newNode;
            }
        }
        if (beginningOfTheEnd == null)
            return false;

        // Atomically append the chain at the tail of this collection
        for (Node<E> t = tail, p = t;;) {
            Node<E> q = p.next;
            if (q == null) {
                // p is last node
                if (p.casNext(null, beginningOfTheEnd)) {
                    // Successful CAS is the linearization point
                    // for all elements to be added to this queue.
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
            else if (p == q)
                // We have fallen off list.  If tail is unchanged, it
                // will also be off-list, in which case we need to
                // jump to head, from which all live nodes are always
                // reachable.  Else the new tail is a better bet.
                p = (t != (t = tail)) ? t : head;
            else
                // Check for tail updates after two hops.
                p = (p != t && t != (t = tail)) ? t : q;
        }
    }

    /**
     * Returns an array containing all of the elements in this queue, in
     * proper sequence.
     *
     * <p>The returned array will be "safe" in that no references to it are
     * maintained by this queue.  (In other words, this method must allocate
     * a new array).  The caller is thus free to modify the returned array.
     *
     * <p>This method acts as bridge between array-based and collection-based
     * APIs.
     *
     * @return an array containing all of the elements in this queue
     */
    public Object[] toArray() {
        // Use ArrayList to deal with resizing.
        ArrayList<E> al = new ArrayList<E>();
        for (Node<E> p = first(); p != null; p = succ(p)) {
            E item = p.item;
            if (item != null)
                al.add(item);
        }
        return al.toArray();
    }

    /**
     * Returns an array containing all of the elements in this queue, in
     * proper sequence; the runtime type of the returned array is that of
     * the specified array.  If the queue fits in the specified array, it
     * is returned therein.  Otherwise, a new array is allocated with the
     * runtime type of the specified array and the size of this queue.
     *
     * <p>If this queue fits in the specified array with room to spare
     * (i.e., the array has more elements than this queue), the element in
     * the array immediately following the end of the queue is set to
     * {@code null}.
     *
     * <p>Like the {@link #toArray()} method, this method acts as bridge between
     * array-based and collection-based APIs.  Further, this method allows
     * precise control over the runtime type of the output array, and may,
     * under certain circumstances, be used to save allocation costs.
     *
     * <p>Suppose {@code x} is a queue known to contain only strings.
     * The following code can be used to dump the queue into a newly
     * allocated array of {@code String}:
     *
     *  <pre> {@code String[] y = x.toArray(new String[0]);}</pre>
     *
     * Note that {@code toArray(new Object[0])} is identical in function to
     * {@code toArray()}.
     *
     * @param a the array into which the elements of the queue are to
     *          be stored, if it is big enough; otherwise, a new array of the
     *          same runtime type is allocated for this purpose
     * @return an array containing all of the elements in this queue
     * @throws ArrayStoreException if the runtime type of the specified array
     *         is not a supertype of the runtime type of every element in
     *         this queue
     * @throws NullPointerException if the specified array is null
     */
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] a) {
        // try to use sent-in array
        int k = 0;
        Node<E> p;
        for (p = first(); p != null && k < a.length; p = succ(p)) {
            E item = p.item;
            if (item != null)
                a[k++] = (T)item;
        }
        if (p == null) {
            if (k < a.length)
                a[k] = null;
            return a;
        }

        // If won't fit, use ArrayList version
        ArrayList<E> al = new ArrayList<E>();
        for (Node<E> q = first(); q != null; q = succ(q)) {
            E item = q.item;
            if (item != null)
                al.add(item);
        }
        return al.toArray(a);
    }

    /**
     * 20210526
     * A. 以适当的顺序返回对该队列中的元素的迭代器。 元素将按照从头（头）到尾（头）的顺序返回。
     * B. 返回的迭代器是弱一致性的。
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

    private class Itr implements Iterator<E> {
        /**
         * Next node to return item for.
         */
        private Node<E> nextNode;

        /**
         * 20210526
         * nextItem保留项字段，因为一旦我们声明hasNext（）中存在某个元素，即使在调用hasNext（）时该元素正处于删除过程中，我们也必须在接下来的next（）调用中将其返回。
         */
        /**
         * nextItem holds on to item fields because once we claim
         * that an element exists in hasNext(), we must return it in
         * the following next() call even if it was in the process of
         * being removed when hasNext() was called.
         */
        private E nextItem;

        /**
         * Node of the last returned item, to support remove.
         */
        private Node<E> lastRet;

        Itr() {
            advance();
        }

        /**
         * 20210526
         * 移至下一个有效节点，并返回要返回next（）的项目；如果没有，则返回null。
         */
        /**
         * Moves to next valid node and returns item to return for
         * next(), or null if no such.
         */
        private E advance() {
            lastRet = nextNode;
            E x = nextItem;

            Node<E> pred, p;
            if (nextNode == null) {
                p = first();
                pred = null;
            } else {
                pred = nextNode;
                p = succ(nextNode);
            }

            for (;;) {
                if (p == null) {
                    nextNode = null;
                    nextItem = null;
                    return x;
                }
                E item = p.item;
                if (item != null) {
                    nextNode = p;
                    nextItem = item;
                    return x;
                } else {
                    // skip over nulls
                    Node<E> next = succ(p);
                    if (pred != null && next != null)
                        pred.casNext(p, next);
                    p = next;
                }
            }
        }

        public boolean hasNext() {
            return nextNode != null;
        }

        public E next() {
            if (nextNode == null) throw new NoSuchElementException();
            return advance();
        }

        public void remove() {
            Node<E> l = lastRet;
            if (l == null) throw new IllegalStateException();
            // rely on a future traversal to relink.
            l.item = null;
            lastRet = null;
        }
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

        // Write out any hidden stuff
        s.defaultWriteObject();

        // Write out all elements in the proper order.
        for (Node<E> p = first(); p != null; p = succ(p)) {
            Object item = p.item;
            if (item != null)
                s.writeObject(item);
        }

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
                t = newNode;
            }
        }
        if (h == null)
            h = t = new Node<E>(null);
        head = h;
        tail = t;
    }

    /** A customized variant of Spliterators.IteratorSpliterator */
    static final class CLQSpliterator<E> implements Spliterator<E> {
        static final int MAX_BATCH = 1 << 25;  // max batch array size;
        final ConcurrentLinkedQueue<E> queue;
        Node<E> current;    // current node; null until initialized
        int batch;          // batch size for splits
        boolean exhausted;  // true when no more nodes
        CLQSpliterator(ConcurrentLinkedQueue<E> queue) {
            this.queue = queue;
        }

        public Spliterator<E> trySplit() {
            Node<E> p;
            final ConcurrentLinkedQueue<E> q = this.queue;
            int b = batch;
            int n = (b <= 0) ? 1 : (b >= MAX_BATCH) ? MAX_BATCH : b + 1;
            if (!exhausted &&
                ((p = current) != null || (p = q.first()) != null) &&
                p.next != null) {
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
            return null;
        }

        public void forEachRemaining(Consumer<? super E> action) {
            Node<E> p;
            if (action == null) throw new NullPointerException();
            final ConcurrentLinkedQueue<E> q = this.queue;
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
            final ConcurrentLinkedQueue<E> q = this.queue;
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
    @Override
    public Spliterator<E> spliterator() {
        return new CLQSpliterator<E>(this);
    }

    /**
     * Throws NullPointerException if argument is null.
     *
     * @param v the element
     */
    private static void checkNotNull(Object v) {
        if (v == null)
            throw new NullPointerException();
    }

    private boolean casTail(Node<E> cmp, Node<E> val) {
        return UNSAFE.compareAndSwapObject(this, tailOffset, cmp, val);
    }

    private boolean casHead(Node<E> cmp, Node<E> val) {
        return UNSAFE.compareAndSwapObject(this, headOffset, cmp, val);
    }

    // Unsafe mechanics

    private static final sun.misc.Unsafe UNSAFE;
    private static final long headOffset;
    private static final long tailOffset;
    static {
        try {
            UNSAFE = sun.misc.Unsafe.getUnsafe();
            Class<?> k = ConcurrentLinkedQueue.class;
            headOffset = UNSAFE.objectFieldOffset
                (k.getDeclaredField("head"));
            tailOffset = UNSAFE.objectFieldOffset
                (k.getDeclaredField("tail"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }
}
