/*
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util.concurrent;

/**
 * 20210530
 * A. {@link BlockingQueue}，生产者可以在其中等待消费者接收元素。 {@code TransferQueue}例如在消息传递应用程序中可能很有用，在该应用程序中，
 *    生产者有时（使用方法{@link #transfer}）等待消费者通过调用{@code take}或{@code poll}来接收元素，而在其他时候排队元素（通过方法{@code put}）而不等待接收。
 *    {@linkplain #tryTransfer(Object) Non-blocking} 和 {@linkplain #tryTransfer(Object,long,TimeUnit) 超时} 版本的 {@code tryTransfer} 也可用。
 *    {@code TransferQueue} 也可以通过 {@link #hasWaitingConsumer} 查询是否有任何线程在等待项目，这与 {@code peek} 操作相反。
 * B. 与其他阻塞队列一样，{@code TransferQueue} 可能有容量限制。 如果是，则尝试的传输操作可能最初阻塞等待可用空间，和/或随后阻塞等待消费者接收。
 *    请注意，在零容量队列中，例如 {@link SynchronousQueue}，{@code put} 和 {@code transfer} 实际上是同义词。
 * C. {@docRoot}/../technotes/guides/collections/index.html
 */

/**
 * A.
 * A {@link BlockingQueue} in which producers may wait for consumers
 * to receive elements.  A {@code TransferQueue} may be useful for
 * example in message passing applications in which producers
 * sometimes (using method {@link #transfer}) await receipt of
 * elements by consumers invoking {@code take} or {@code poll}, while
 * at other times enqueue elements (via method {@code put}) without
 * waiting for receipt.
 * {@linkplain #tryTransfer(Object) Non-blocking} and
 * {@linkplain #tryTransfer(Object,long,TimeUnit) time-out} versions of
 * {@code tryTransfer} are also available.
 * A {@code TransferQueue} may also be queried, via {@link
 * #hasWaitingConsumer}, whether there are any threads waiting for
 * items, which is a converse analogy to a {@code peek} operation.
 *
 * B.
 * <p>Like other blocking queues, a {@code TransferQueue} may be
 * capacity bounded.  If so, an attempted transfer operation may
 * initially block waiting for available space, and/or subsequently
 * block waiting for reception by a consumer.  Note that in a queue
 * with zero capacity, such as {@link SynchronousQueue}, {@code put}
 * and {@code transfer} are effectively synonymous.
 *
 * C.
 * <p>This interface is a member of the
 * <a href="{@docRoot}/../technotes/guides/collections/index.html">
 * Java Collections Framework</a>.
 *
 * @since 1.7
 * @author Doug Lea
 * @param <E> the type of elements held in this collection
 */
public interface TransferQueue<E> extends BlockingQueue<E> {

    /**
     * 20210530
     * A. 如果可能，立即将元素传输给等待的使用者。
     * B. 更准确地说，如果存在已经等待接收它的消费者（在 {@link #take} 或定时 {@link #poll(long,TimeUnit) poll}），则立即传输指定的元素，
     *    否则返回 {@code false} 而不元素入队。
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
     * @param e the element to transfer
     * @return {@code true} if the element was transferred, else
     *         {@code false}
     * @throws ClassCastException if the class of the specified element
     *         prevents it from being added to this queue
     * @throws NullPointerException if the specified element is null
     * @throws IllegalArgumentException if some property of the specified
     *         element prevents it from being added to this queue
     */
    boolean tryTransfer(E e);

    /**
     * 20210530
     * A. 将元素传输给消费者，必要时等待。
     * B. 更准确地说，如果存在已经等待接收它的消费者（在 {@link #take} 或定时 {@link #poll(long,TimeUnit) poll}），则立即传输指定的元素，否则等待直到元素被消费者接收。
     */
    /**
     * A.
     * Transfers the element to a consumer, waiting if necessary to do so.
     *
     * B.
     * <p>More precisely, transfers the specified element immediately
     * if there exists a consumer already waiting to receive it (in
     * {@link #take} or timed {@link #poll(long,TimeUnit) poll}),
     * else waits until the element is received by a consumer.
     *
     * @param e the element to transfer
     * @throws InterruptedException if interrupted while waiting,
     *         in which case the element is not left enqueued
     * @throws ClassCastException if the class of the specified element
     *         prevents it from being added to this queue
     * @throws NullPointerException if the specified element is null
     * @throws IllegalArgumentException if some property of the specified
     *         element prevents it from being added to this queue
     */
    void transfer(E e) throws InterruptedException;

    /**
     * 20210530
     * A. 如果有可能，在超时之前将元素转移给使用者。
     * B. 更准确地说，如果存在已经等待接收它的消费者（在 {@link #take} 或定时 {@link #poll(long,TimeUnit) poll}），则立即传输指定的元素，否则等待直到元素被消费者接收，
     *    如果指定的等待时间过去后元素可以被传输，则返回 {@code false}。
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
     * else waits until the element is received by a consumer,
     * returning {@code false} if the specified wait time elapses
     * before the element can be transferred.
     *
     * @param e the element to transfer
     * @param timeout how long to wait before giving up, in units of
     *        {@code unit}
     * @param unit a {@code TimeUnit} determining how to interpret the
     *        {@code timeout} parameter
     * @return {@code true} if successful, or {@code false} if
     *         the specified waiting time elapses before completion,
     *         in which case the element is not left enqueued
     * @throws InterruptedException if interrupted while waiting,
     *         in which case the element is not left enqueued
     * @throws ClassCastException if the class of the specified element
     *         prevents it from being added to this queue
     * @throws NullPointerException if the specified element is null
     * @throws IllegalArgumentException if some property of the specified
     *         element prevents it from being added to this queue
     */
    boolean tryTransfer(E e, long timeout, TimeUnit unit) throws InterruptedException;

    /**
     * 20210530
     * 如果至少有一个消费者等待通过 {@link #take} 或定时 {@link #poll(long,TimeUnit) poll} 接收元素，则返回 {@code true}。 返回值表示事务的瞬时状态。
     */
    /**
     * Returns {@code true} if there is at least one consumer waiting
     * to receive an element via {@link #take} or
     * timed {@link #poll(long,TimeUnit) poll}.
     * The return value represents a momentary state of affairs.
     *
     * @return {@code true} if there is at least one waiting consumer
     */
    boolean hasWaitingConsumer();

    /**
     * 20210530
     * 通过 {@link #take} 或定时 {@link #poll(long,TimeUnit) poll} 返回等待接收元素的消费者数量的估计。
     * 返回值是瞬时状态的近似值，如果消费者已完成或放弃等待，则该值可能不准确。 该值可能对监视和启发式有用，但不适用于同步控制。
     * 此方法的实现可能明显慢于 {@link #hasWaitingConsumer} 的实现。
     */
    /**
     * Returns an estimate of the number of consumers waiting to
     * receive elements via {@link #take} or timed
     * {@link #poll(long,TimeUnit) poll}.  The return value is an
     * approximation of a momentary state of affairs, that may be
     * inaccurate if consumers have completed or given up waiting.
     * The value may be useful for monitoring and heuristics, but
     * not for synchronization control.  Implementations of this
     * method are likely to be noticeably slower than those for
     * {@link #hasWaitingConsumer}.
     *
     * @return the number of consumers waiting to receive elements
     */
    int getWaitingConsumerCount();
}
