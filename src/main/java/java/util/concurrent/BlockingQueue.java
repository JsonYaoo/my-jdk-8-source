/*
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util.concurrent;

import java.util.Collection;
import java.util.Queue;

/**
 * 20210523
 * A. {@link java.util.Queue}另外还支持以下操作：在检索元素时等待队列变为非空，并在存储元素时等待队列中的空间变为可用。
 * B. {@code BlockingQueue}方法有四种形式，它们以不同的方式处理操作，这些方法无法立即满足，但将来可能会满足：
 *    一种抛出异常，
 *    第二种返回一个特殊值（{@code null}或{@code false}，具体取决于操作），
 *    第三个将无限期阻塞当前线程，直到操作成功为止；
 *    第四个阻塞仅放弃给定的最大时间限制。 下表总结了这些方法：
 *                                                          Summary of BlockingQueue methods
 *          Throws exception	          Special value	             Blocks	                 Times out
 * Insert	{@link #add add(e)}           {@link #offer offer(e)}    {@link #put put(e)}     {@link #offer(Object, long, TimeUnit) offer(e, time, unit)}
 * Remove	{@link #remove remove()}      {@link #poll poll()}       {@link #take take()}    {@link #poll(long, TimeUnit) poll(time, unit)}
 * Examine	{@link #element element()}    {@link #peek peek()}	     not applicable	         not applicable
 * C. {@code BlockingQueue}不接受{@code null}元素。 实现尝试对{@code add}，{@code put}或{@code offer}一个{@code null}进行尝试时，
 *    抛出{@code NullPointerException}。 {@code null}用作标记值，以指示{@code poll}操作失败。
 * D. {@code BlockingQueue}可能受容量限制。 在任何给定的时间，它都可能具有{@code leftCapacity}，超过该值之前，其他任何元素都不能被{@code put}阻塞。
 *    没有任何内部容量约束的{@code BlockingQueue}始终报告{@code Integer.MAX_VALUE}的剩余容量。
 * E. {@code BlockingQueue}实现被设计为主要用于生产者－消费者队列，但还支持{@link java.util.Collection}接口。 因此，例如，
 *    可以使用{@code remove（x）}从队列中删除任意元素。 但是，这样的操作通常不能非常有效地执行，并且仅用于偶尔的使用，例如在取消排队的消息时。
 * F. {@code BlockingQueue}实现是线程安全的。 所有排队方法都是使用内部锁或其他形式的并发控制来原子地实现其效果的。 但是，除非在实现中另行指定，
 *    否则不一定要自动执行批量Collection操作{@code addAll}，{@code containsAll}，{@code keepAll}和{@code removeAll}。 因此，例如，
 *    在仅在{@code c}中添加一些元素之后，{@code addAll（c）}可能会失败（引发异常）。
 * G. {@code BlockingQueue}本质上不支持任何类型的“关闭”操作。 或“关机”； 操作，表示将不再添加任何项目。 此类功能的需求和使用往往取决于实现。
 *    例如，一种常见的策略是让生产者插入特殊的流尾对象或有毒对象，当消费者采取这种方法时会对其进行相应的解释。
 * H. 使用示例，基于典型的生产者-消费者方案。 请注意，{@code BlockingQueue}可以安全地与多个生产者和多个消费者一起使用:
 * class Producer implements Runnable {
 *   private final BlockingQueue queue;
 *   Producer(BlockingQueue q) { queue = q; }
 *   public void run() {
 *     try {
 *       while (true) { queue.put(produce()); }
 *     } catch (InterruptedException ex) { ... handle ...}
 *   }
 *   Object produce() { ... }
 * }
 *
 * class Consumer implements Runnable {
 *   private final BlockingQueue queue;
 *   Consumer(BlockingQueue q) { queue = q; }
 *   public void run() {
 *     try {
 *       while (true) { consume(queue.take()); }
 *     } catch (InterruptedException ex) { ... handle ...}
 *   }
 *   void consume(Object x) { ... }
 * }
 *
 * class Setup {
 *   void main() {
 *     BlockingQueue q = new SomeQueueImplementation();
 *     Producer p = new Producer(q);
 *     Consumer c1 = new Consumer(q);
 *     Consumer c2 = new Consumer(q);
 *     new Thread(p).start();
 *     new Thread(c1).start();
 *     new Thread(c2).start();
 *   }
 * }}
 * I. 内存一致性影响：与其他并发集合一样，在将对象放入{@code BlockingQueue} <a href="package-summary.html#MemoryVisibility">先发生之后的线程中的线程之前的操作</a>
 *    在另一个线程中从{@code BlockingQueue}访问或删除该元素。
 * J. {@docRoot}/../technotes/guides/collections/index.html
 *
 */
/**
 * A.
 * A {@link java.util.Queue} that additionally supports operations
 * that wait for the queue to become non-empty when retrieving an
 * element, and wait for space to become available in the queue when
 * storing an element.
 *
 * B.
 * <p>{@code BlockingQueue} methods come in four forms, with different ways
 * of handling operations that cannot be satisfied immediately, but may be
 * satisfied at some point in the future:
 * one throws an exception, the second returns a special value (either
 * {@code null} or {@code false}, depending on the operation), the third
 * blocks the current thread indefinitely until the operation can succeed,
 * and the fourth blocks for only a given maximum time limit before giving
 * up.  These methods are summarized in the following table:
 *
 * <table BORDER CELLPADDING=3 CELLSPACING=1>
 * <caption>Summary of BlockingQueue methods</caption>
 *  <tr>
 *    <td></td>
 *    <td ALIGN=CENTER><em>Throws exception</em></td>
 *    <td ALIGN=CENTER><em>Special value</em></td>
 *    <td ALIGN=CENTER><em>Blocks</em></td>
 *    <td ALIGN=CENTER><em>Times out</em></td>
 *  </tr>
 *  <tr>
 *    <td><b>Insert</b></td>
 *    <td>{@link #add add(e)}</td>
 *    <td>{@link #offer offer(e)}</td>
 *    <td>{@link #put put(e)}</td>
 *    <td>{@link #offer(Object, long, TimeUnit) offer(e, time, unit)}</td>
 *  </tr>
 *  <tr>
 *    <td><b>Remove</b></td>
 *    <td>{@link #remove remove()}</td>
 *    <td>{@link #poll poll()}</td>
 *    <td>{@link #take take()}</td>
 *    <td>{@link #poll(long, TimeUnit) poll(time, unit)}</td>
 *  </tr>
 *  <tr>
 *    <td><b>Examine</b></td>
 *    <td>{@link #element element()}</td>
 *    <td>{@link #peek peek()}</td>
 *    <td><em>not applicable</em></td>
 *    <td><em>not applicable</em></td>
 *  </tr>
 * </table>
 *
 * C.
 * <p>A {@code BlockingQueue} does not accept {@code null} elements.
 * Implementations throw {@code NullPointerException} on attempts
 * to {@code add}, {@code put} or {@code offer} a {@code null}.  A
 * {@code null} is used as a sentinel value to indicate failure of
 * {@code poll} operations.
 *
 * D.
 * <p>A {@code BlockingQueue} may be capacity bounded. At any given
 * time it may have a {@code remainingCapacity} beyond which no
 * additional elements can be {@code put} without blocking.
 * A {@code BlockingQueue} without any intrinsic capacity constraints always
 * reports a remaining capacity of {@code Integer.MAX_VALUE}.
 *
 * E.
 * <p>{@code BlockingQueue} implementations are designed to be used
 * primarily for producer-consumer queues, but additionally support
 * the {@link java.util.Collection} interface.  So, for example, it is
 * possible to remove an arbitrary element from a queue using
 * {@code remove(x)}. However, such operations are in general
 * <em>not</em> performed very efficiently, and are intended for only
 * occasional use, such as when a queued message is cancelled.
 *
 * F.
 * <p>{@code BlockingQueue} implementations are thread-safe.  All
 * queuing methods achieve their effects atomically using internal
 * locks or other forms of concurrency control. However, the
 * <em>bulk</em> Collection operations {@code addAll},
 * {@code containsAll}, {@code retainAll} and {@code removeAll} are
 * <em>not</em> necessarily performed atomically unless specified
 * otherwise in an implementation. So it is possible, for example, for
 * {@code addAll(c)} to fail (throwing an exception) after adding
 * only some of the elements in {@code c}.
 *
 * G.
 * <p>A {@code BlockingQueue} does <em>not</em> intrinsically support
 * any kind of &quot;close&quot; or &quot;shutdown&quot; operation to
 * indicate that no more items will be added.  The needs and usage of
 * such features tend to be implementation-dependent. For example, a
 * common tactic is for producers to insert special
 * <em>end-of-stream</em> or <em>poison</em> objects, that are
 * interpreted accordingly when taken by consumers.
 *
 * H.
 * <p>
 * Usage example, based on a typical producer-consumer scenario.
 * Note that a {@code BlockingQueue} can safely be used with multiple
 * producers and multiple consumers.
 *  <pre> {@code
 * class Producer implements Runnable {
 *   private final BlockingQueue queue;
 *   Producer(BlockingQueue q) { queue = q; }
 *   public void run() {
 *     try {
 *       while (true) { queue.put(produce()); }
 *     } catch (InterruptedException ex) { ... handle ...}
 *   }
 *   Object produce() { ... }
 * }
 *
 * class Consumer implements Runnable {
 *   private final BlockingQueue queue;
 *   Consumer(BlockingQueue q) { queue = q; }
 *   public void run() {
 *     try {
 *       while (true) { consume(queue.take()); }
 *     } catch (InterruptedException ex) { ... handle ...}
 *   }
 *   void consume(Object x) { ... }
 * }
 *
 * class Setup {
 *   void main() {
 *     BlockingQueue q = new SomeQueueImplementation();
 *     Producer p = new Producer(q);
 *     Consumer c1 = new Consumer(q);
 *     Consumer c2 = new Consumer(q);
 *     new Thread(p).start();
 *     new Thread(c1).start();
 *     new Thread(c2).start();
 *   }
 * }}</pre>
 *
 * I.
 * <p>Memory consistency effects: As with other concurrent
 * collections, actions in a thread prior to placing an object into a
 * {@code BlockingQueue}
 * <a href="package-summary.html#MemoryVisibility"><i>happen-before</i></a>
 * actions subsequent to the access or removal of that element from
 * the {@code BlockingQueue} in another thread.
 *
 * J.
 * <p>This interface is a member of the
 * <a href="{@docRoot}/../technotes/guides/collections/index.html">
 * Java Collections Framework</a>.
 *
 * @since 1.5
 * @author Doug Lea
 * @param <E> the type of elements held in this collection
 */
public interface BlockingQueue<E> extends Queue<E> {
    /**
     * Inserts the specified element into this queue if it is possible to do
     * so immediately without violating capacity restrictions, returning
     * {@code true} upon success and throwing an
     * {@code IllegalStateException} if no space is currently available.
     * When using a capacity-restricted queue, it is generally preferable to
     * use {@link #offer(Object) offer}.
     *
     * @param e the element to add
     * @return {@code true} (as specified by {@link Collection#add})
     * @throws IllegalStateException if the element cannot be added at this
     *         time due to capacity restrictions
     * @throws ClassCastException if the class of the specified element
     *         prevents it from being added to this queue
     * @throws NullPointerException if the specified element is null
     * @throws IllegalArgumentException if some property of the specified
     *         element prevents it from being added to this queue
     */
    boolean add(E e);

    /**
     * Inserts the specified element into this queue if it is possible to do
     * so immediately without violating capacity restrictions, returning
     * {@code true} upon success and {@code false} if no space is currently
     * available.  When using a capacity-restricted queue, this method is
     * generally preferable to {@link #add}, which can fail to insert an
     * element only by throwing an exception.
     *
     * @param e the element to add
     * @return {@code true} if the element was added to this queue, else
     *         {@code false}
     * @throws ClassCastException if the class of the specified element
     *         prevents it from being added to this queue
     * @throws NullPointerException if the specified element is null
     * @throws IllegalArgumentException if some property of the specified
     *         element prevents it from being added to this queue
     */
    boolean offer(E e);

    /**
     * Inserts the specified element into this queue, waiting if necessary
     * for space to become available.
     *
     * @param e the element to add
     * @throws InterruptedException if interrupted while waiting
     * @throws ClassCastException if the class of the specified element
     *         prevents it from being added to this queue
     * @throws NullPointerException if the specified element is null
     * @throws IllegalArgumentException if some property of the specified
     *         element prevents it from being added to this queue
     */
    void put(E e) throws InterruptedException;

    /**
     * Inserts the specified element into this queue, waiting up to the
     * specified wait time if necessary for space to become available.
     *
     * @param e the element to add
     * @param timeout how long to wait before giving up, in units of
     *        {@code unit}
     * @param unit a {@code TimeUnit} determining how to interpret the
     *        {@code timeout} parameter
     * @return {@code true} if successful, or {@code false} if
     *         the specified waiting time elapses before space is available
     * @throws InterruptedException if interrupted while waiting
     * @throws ClassCastException if the class of the specified element
     *         prevents it from being added to this queue
     * @throws NullPointerException if the specified element is null
     * @throws IllegalArgumentException if some property of the specified
     *         element prevents it from being added to this queue
     */
    boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException;

    /**
     * Retrieves and removes the head of this queue, waiting if necessary
     * until an element becomes available.
     *
     * @return the head of this queue
     * @throws InterruptedException if interrupted while waiting
     */
    E take() throws InterruptedException;

    /**
     * Retrieves and removes the head of this queue, waiting up to the
     * specified wait time if necessary for an element to become available.
     *
     * @param timeout how long to wait before giving up, in units of
     *        {@code unit}
     * @param unit a {@code TimeUnit} determining how to interpret the
     *        {@code timeout} parameter
     * @return the head of this queue, or {@code null} if the
     *         specified waiting time elapses before an element is available
     * @throws InterruptedException if interrupted while waiting
     */
    E poll(long timeout, TimeUnit unit) throws InterruptedException;

    /**
     * Returns the number of additional elements that this queue can ideally
     * (in the absence of memory or resource constraints) accept without
     * blocking, or {@code Integer.MAX_VALUE} if there is no intrinsic
     * limit.
     *
     * <p>Note that you <em>cannot</em> always tell if an attempt to insert
     * an element will succeed by inspecting {@code remainingCapacity}
     * because it may be the case that another thread is about to
     * insert or remove an element.
     *
     * @return the remaining capacity
     */
    int remainingCapacity();

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
     * @throws ClassCastException if the class of the specified element
     *         is incompatible with this queue
     *         (<a href="../Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException if the specified element is null
     *         (<a href="../Collection.html#optional-restrictions">optional</a>)
     */
    boolean remove(Object o);

    /**
     * Returns {@code true} if this queue contains the specified element.
     * More formally, returns {@code true} if and only if this queue contains
     * at least one element {@code e} such that {@code o.equals(e)}.
     *
     * @param o object to be checked for containment in this queue
     * @return {@code true} if this queue contains the specified element
     * @throws ClassCastException if the class of the specified element
     *         is incompatible with this queue
     *         (<a href="../Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException if the specified element is null
     *         (<a href="../Collection.html#optional-restrictions">optional</a>)
     */
    public boolean contains(Object o);

    /**
     * 20210523
     * 从此队列中删除所有可用的元素，并将它们添加到给定的集合中。 与重复轮询此队列相比，此操作可能更有效。 尝试将元素添加到集合{@code c}时遇到故障，
     * 可能会导致在引发关联异常时元素既不在两个集合中，又不在两个集合中。 尝试排空队列本身会导致{@code IllegalArgumentException}。
     * 此外，如果在操作进行过程中修改了指定的集合，则此操作的行为是不确定的。
     */
    /**
     * Removes all available elements from this queue and adds them
     * to the given collection.  This operation may be more
     * efficient than repeatedly polling this queue.  A failure
     * encountered while attempting to add elements to
     * collection {@code c} may result in elements being in neither,
     * either or both collections when the associated exception is
     * thrown.  Attempts to drain a queue to itself result in
     * {@code IllegalArgumentException}. Further, the behavior of
     * this operation is undefined if the specified collection is
     * modified while the operation is in progress.
     *
     * @param c the collection to transfer elements into
     * @return the number of elements transferred
     * @throws UnsupportedOperationException if addition of elements
     *         is not supported by the specified collection
     * @throws ClassCastException if the class of an element of this queue
     *         prevents it from being added to the specified collection
     * @throws NullPointerException if the specified collection is null
     * @throws IllegalArgumentException if the specified collection is this
     *         queue, or some property of an element of this queue prevents
     *         it from being added to the specified collection
     */
    int drainTo(Collection<? super E> c);

    /**
     * 20210523
     * 从此队列中最多删除给定数量的可用元素，并将它们添加到给定的集合中。 尝试将元素添加到集合{@code c}时遇到故障，可能会导致在引发关联异常时元素既不在两个集合中，
     * 又不在两个集合中。 尝试排空队列本身会导致{@code IllegalArgumentException}。 此外，如果在操作进行过程中修改了指定的集合，则此操作的行为是不确定的。
     */
    /**
     * Removes at most the given number of available elements from
     * this queue and adds them to the given collection.  A failure
     * encountered while attempting to add elements to
     * collection {@code c} may result in elements being in neither,
     * either or both collections when the associated exception is
     * thrown.  Attempts to drain a queue to itself result in
     * {@code IllegalArgumentException}. Further, the behavior of
     * this operation is undefined if the specified collection is
     * modified while the operation is in progress.
     *
     * @param c the collection to transfer elements into
     * @param maxElements the maximum number of elements to transfer
     * @return the number of elements transferred
     * @throws UnsupportedOperationException if addition of elements
     *         is not supported by the specified collection
     * @throws ClassCastException if the class of an element of this queue
     *         prevents it from being added to the specified collection
     * @throws NullPointerException if the specified collection is null
     * @throws IllegalArgumentException if the specified collection is this
     *         queue, or some property of an element of this queue prevents
     *         it from being added to the specified collection
     */
    int drainTo(Collection<? super E> c, int maxElements);
}
