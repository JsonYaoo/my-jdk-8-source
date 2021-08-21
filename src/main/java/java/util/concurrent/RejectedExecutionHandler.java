/*
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util.concurrent;

// 无法由 {@link ThreadPoolExecutor} 执行的任务的处理程序。
/**
 * A handler for tasks that cannot be executed by a {@link ThreadPoolExecutor}.
 *
 * @since 1.5
 * @author Doug Lea
 */
public interface RejectedExecutionHandler {

    /**
     * 20210814
     * A. 当 {@link ThreadPoolExecutor#execute execute} 无法接受任务时，可以由 {@link ThreadPoolExecutor} 调用的方法。
     *    当没有更多线程或队列插槽可用时，可能会发生这种情况，因为它们的边界会被超出，或者在 Executor 关闭时。
     * B. 在没有其他替代方法的情况下，该方法可能会抛出未经检查的 {@link RejectedExecutionException}，该异常将传播给 {@code execute} 的调用者。
     */
    /**
     * A.
     * Method that may be invoked by a {@link ThreadPoolExecutor} when
     * {@link ThreadPoolExecutor#execute execute} cannot accept a
     * task.  This may occur when no more threads or queue slots are
     * available because their bounds would be exceeded, or upon
     * shutdown of the Executor.
     *
     * B.
     * <p>In the absence of other alternatives, the method may throw
     * an unchecked {@link RejectedExecutionException}, which will be
     * propagated to the caller of {@code execute}.
     *
     * // 请求执行的可运行任务
     * @param r the runnable task requested to be executed
     *
     * // 试图执行此任务的执行者
     * @param executor the executor attempting to execute this task
     *
     * // 如果没有补救措施
     * @throws RejectedExecutionException if there is no remedy
     */
    // 履行任务r和任务执行者executor的拒绝策略
    void rejectedExecution(Runnable r, ThreadPoolExecutor executor);
}
