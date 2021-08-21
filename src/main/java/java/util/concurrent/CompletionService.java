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
 * 20210813
 * A. 一种将新异步任务的产生与已完成任务的结果的消耗分离的服务。 生产者 {@code submit} 执行任务。 消费者 {@code take} 完成的任务并按照他们完成的顺序处理他们的结果。
 *    例如，{@code CompletionService} 可用于管理异步 I/O，其中执行读取的任务在程序或系统的一个部分提交，然后在读取完成时在程序的不同部分执行 ，可能与请求的顺序不同。
 * B. 通常，{@code CompletionService} 依赖单独的 {@link Executor} 来实际执行任务，在这种情况下，{@code CompletionService} 仅管理内部完成队列。
 *    {@link ExecutorCompletionService} 类提供了这种方法的实现。
 * C. 内存一致性影响：在将任务提交给 {@code CompletionService} 之前线程中的操作发生在该任务采取的操作之前，而在从相应的 {@code take()} 成功返回之后又发生在操作之前 .
 */
/**
 * A.
 * A service that decouples the production of new asynchronous tasks
 * from the consumption of the results of completed tasks.  Producers
 * {@code submit} tasks for execution. Consumers {@code take}
 * completed tasks and process their results in the order they
 * complete.  A {@code CompletionService} can for example be used to
 * manage asynchronous I/O, in which tasks that perform reads are
 * submitted in one part of a program or system, and then acted upon
 * in a different part of the program when the reads complete,
 * possibly in a different order than they were requested.
 *
 * B.
 * <p>Typically, a {@code CompletionService} relies on a separate
 * {@link Executor} to actually execute the tasks, in which case the
 * {@code CompletionService} only manages an internal completion
 * queue. The {@link ExecutorCompletionService} class provides an
 * implementation of this approach.
 *
 * C.
 * <p>Memory consistency effects: Actions in a thread prior to
 * submitting a task to a {@code CompletionService}
 * <a href="package-summary.html#MemoryVisibility"><i>happen-before</i></a>
 * actions taken by that task, which in turn <i>happen-before</i>
 * actions following a successful return from the corresponding {@code take()}.
 */
public interface CompletionService<V> {

    // 提交一个返回值的任务以供执行，并返回一个表示任务未决结果的 Future。 完成后，可以执行或轮询此任务。
    /**
     * Submits a value-returning task for execution and returns a Future
     * representing the pending results of the task.  Upon completion,
     * this task may be taken or polled.
     *
     * @param task the task to submit
     *
     * // 代表待完成任务的 Future
     * @return a Future representing pending completion of the task
     * @throws RejectedExecutionException if the task cannot be
     *         scheduled for execution
     * @throws NullPointerException if the task is null
     */
    Future<V> submit(Callable<V> task);

    // 提交一个 Runnable 任务以供执行，并返回一个代表该任务的 Future。 完成后，可以执行或轮询此任务。
    /**
     * Submits a Runnable task for execution and returns a Future
     * representing that task.  Upon completion, this task may be
     * taken or polled.
     *
     * @param task the task to submit
     *
     * // 成功完成后返回的结果
     * @param result the result to return upon successful completion
     *
     * // 表示待完成任务的 Future，其 {@code get()} 方法将在完成后返回给定的结果值
     * @return a Future representing pending completion of the task,
     *         and whose {@code get()} method will return the given
     *         result value upon completion
     * @throws RejectedExecutionException if the task cannot be
     *         scheduled for execution
     * @throws NullPointerException if the task is null
     */
    Future<V> submit(Runnable task, V result);

    // 检索并删除代表下一个已完成任务的Future，如果尚不存在则等待。
    /**
     * Retrieves and removes the Future representing the next
     * completed task, waiting if none are yet present.
     *
     * // Future 代表下一个完成的任务
     * @return the Future representing the next completed task
     * @throws InterruptedException if interrupted while waiting
     */
    Future<V> take() throws InterruptedException;

    // 检索并删除代表下一个已完成任务的 Future，如果不存在则为 {@code null}。
    /**
     * Retrieves and removes the Future representing the next
     * completed task, or {@code null} if none are present.
     *
     * // Future 代表下一个完成的任务，或者 {@code null} 如果不存在
     * @return the Future representing the next completed task, or {@code null} if none are present
     */
    Future<V> poll();

    // 检索并删除代表下一个已完成任务的Future，如果尚不存在，则在必要时等待指定的等待时间。
    /**
     * Retrieves and removes the Future representing the next
     * completed task, waiting if necessary up to the specified wait
     * time if none are yet present.
     *
     * @param timeout how long to wait before giving up, in units of
     *        {@code unit}
     * @param unit a {@code TimeUnit} determining how to interpret the
     *        {@code timeout} parameter
     *
     * // Future 代表下一个完成的任务或 {@code null} 如果指定的等待时间在一个存在之前过去了
     * @return the Future representing the next completed task or
     *         {@code null} if the specified waiting time elapses
     *         before one is present
     * @throws InterruptedException if interrupted while waiting
     */
    Future<V> poll(long timeout, TimeUnit unit) throws InterruptedException;
}
