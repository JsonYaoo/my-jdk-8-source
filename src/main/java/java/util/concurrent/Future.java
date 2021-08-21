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
 * 20210726
 * A. {@code Future}表示异步计算的结果。提供了检查计算是否完成、等待其完成以及检索计算结果的方法。结果只能在计算完成后使用方法{@code get} 检索，必要时阻塞直到它准备好。
 *    取消由{@code cancel}方法执行。提供了其他方法来确定任务是正常完成还是被取消。一旦计算完成，就不能取消计算。如果为了可取消性而想使用{@code Future}但不提供可用结果，
 *    则可以声明{@code Future}形式的类型并返回{@code null}作为底层任务的结果.
 * B. 示例用法（请注意，以下类都是虚构的。）:
 *  {@code
 * interface ArchiveSearcher { String search(String target); }
 * class App {
 *   ExecutorService executor = ...
 *   ArchiveSearcher searcher = ...
 *   void showSearch(final String target)
 *       throws InterruptedException {
 *     Future future
 *       = executor.submit(new Callable() {
 *         public String call() {
 *             return searcher.search(target);
 *         }});
 *     displayOtherThings(); // do other things while searching
 *     try {
 *       displayText(future.get()); // use future
 *     } catch (ExecutionException ex) { cleanup(); return; }
 *   }
 * }}
 * {@link FutureTask}类是实现{@code Runnable}的{@code Future}的实现，因此可以由{@code Executor} 执行。例如，上面带有 {@code submit} 的构造可以替换为：
 *  {@code
 * FutureTask future =
 *   new FutureTask(new Callable() {
 *     public String call() {
 *       return searcher.search(target);
 *   }});
 * executor.execute(future);}
 * C. 内存一致性影响：异步计算采取的操作发生在另一个线程中相应的 {@code Future.get()} 之后的操作之前。
 */
/**
 * A.
 * A {@code Future} represents the result of an asynchronous
 * computation.  Methods are provided to check if the computation is
 * complete, to wait for its completion, and to retrieve the result of
 * the computation.  The result can only be retrieved using method
 * {@code get} when the computation has completed, blocking if
 * necessary until it is ready.  Cancellation is performed by the
 * {@code cancel} method.  Additional methods are provided to
 * determine if the task completed normally or was cancelled. Once a
 * computation has completed, the computation cannot be cancelled.
 * If you would like to use a {@code Future} for the sake
 * of cancellability but not provide a usable result, you can
 * declare types of the form {@code Future<?>} and
 * return {@code null} as a result of the underlying task.
 *
 * B.
 * <p>
 * <b>Sample Usage</b> (Note that the following classes are all
 * made-up.)
 * <pre> {@code
 * interface ArchiveSearcher { String search(String target); }
 * class App {
 *   ExecutorService executor = ...
 *   ArchiveSearcher searcher = ...
 *   void showSearch(final String target)
 *       throws InterruptedException {
 *     Future<String> future
 *       = executor.submit(new Callable<String>() {
 *         public String call() {
 *             return searcher.search(target);
 *         }});
 *     displayOtherThings(); // do other things while searching
 *     try {
 *       displayText(future.get()); // use future
 *     } catch (ExecutionException ex) { cleanup(); return; }
 *   }
 * }}</pre>
 *
 * The {@link FutureTask} class is an implementation of {@code Future} that
 * implements {@code Runnable}, and so may be executed by an {@code Executor}.
 * For example, the above construction with {@code submit} could be replaced by:
 *  <pre> {@code
 * FutureTask<String> future =
 *   new FutureTask<String>(new Callable<String>() {
 *     public String call() {
 *       return searcher.search(target);
 *   }});
 * executor.execute(future);}</pre>
 *
 * C.
 * <p>Memory consistency effects: Actions taken by the asynchronous computation
 * <a href="package-summary.html#MemoryVisibility"> <i>happen-before</i></a>
 * actions following the corresponding {@code Future.get()} in another thread.
 *
 * @see FutureTask
 * @see Executor
 * @since 1.5
 * @author Doug Lea
 * @param <V> The result type returned by this Future's {@code get} method
 */
public interface Future<V> {

    /**
     * 20210726
     * A. 尝试取消此任务的执行。如果任务已完成、已被取消或由于其他原因无法取消，则此尝试将失败。如果成功，并且在调用{@code cancel}时此任务尚未启动，则不应运行此任务。
     *    如果任务已经开始，那么{@code mayInterruptIfRunning}参数决定是否应该中断执行该任务的线程以尝试停止该任务。
     * B. 此方法返回后，对{@link #isDone}的后续调用将始终返回{@code true}。如果此方法返回 {@code true}，则对{@link #isCancelled}的后续调用将始终返回{@code true}。
     */
    /**
     * A.
     * Attempts to cancel execution of this task.  This attempt will
     * fail if the task has already completed, has already been cancelled,
     * or could not be cancelled for some other reason. If successful,
     * and this task has not started when {@code cancel} is called,
     * this task should never run.  If the task has already started,
     * then the {@code mayInterruptIfRunning} parameter determines
     * whether the thread executing this task should be interrupted in
     * an attempt to stop the task.
     *
     * B.
     * <p>After this method returns, subsequent calls to {@link #isDone} will
     * always return {@code true}.  Subsequent calls to {@link #isCancelled}
     * will always return {@code true} if this method returned {@code true}.
     *
     * // {@code true} 如果执行此任务的线程应该被中断； 否则，允许完成正在进行的任务
     * @param mayInterruptIfRunning {@code true} if the thread executing this
     * task should be interrupted; otherwise, in-progress tasks are allowed
     * to complete
     *
     * // {@code false} 如果任务无法取消，通常是因为它已经正常完成；
     * @return {@code false} if the task could not be cancelled,
     * typically because it has already completed normally;
     * {@code true} otherwise
     */
    // 尝试取消此异步计算任务的执行, 如果任务已完成、已被取消或由于其他原因无法取消, 则返回false; 如果此任务在正常完成之前被取消, 则返回{@code true}; 如果任务已经开始, 那么{@code mayInterruptIfRunning}参数决定是否应该中断执行该任务的线程以尝试停止该任务
    boolean cancel(boolean mayInterruptIfRunning);

    // 如果此任务在正常完成之前被取消，则返回 {@code true}。
    /**
     * Returns {@code true} if this task was cancelled before it completed
     * normally.
     *
     * @return {@code true} if this task was cancelled before it completed
     */
    // 判断此异步计算任务是否取消成功, 如果任务已完成、已被取消或由于其他原因无法取消, 则返回false; 如果在正常完成之前被取消，则返回{@code true}
    boolean isCancelled();

    /**
     * 20210726
     * A. 如果此任务完成，则返回 {@code true}。
     * B. 完成可能是由于正常终止、异常或取消——在所有这些情况下，此方法将返回 {@code true}。
     */
    /**
     * A.
     * Returns {@code true} if this task completed.
     *
     * B.
     * Completion may be due to normal termination, an exception, or
     * cancellation -- in all of these cases, this method will return
     * {@code true}.
     *
     * @return {@code true} if this task completed
     */
    // 判断此异步计算任务是否已经完成, 在正常终止、异常或取消时, 则返回true
    boolean isDone();

    // 如有必要，等待计算完成，然后检索其结果。
    /**
     * Waits if necessary for the computation to complete, and then
     * retrieves its result.
     *
     * @return the computed result
     * @throws CancellationException if the computation was cancelled
     * @throws ExecutionException if the computation threw an
     * exception
     * @throws InterruptedException if the current thread was interrupted
     * while waiting
     */
    // 阻塞获取异步计算结果
    V get() throws InterruptedException, ExecutionException;

    // 如有必要，最多等待给定的计算完成时间，然后检索其结果（如果可用）。
    /**
     * Waits if necessary for at most the given time for the computation
     * to complete, and then retrieves its result, if available.
     *
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout argument
     * @return the computed result
     * @throws CancellationException if the computation was cancelled
     * @throws ExecutionException if the computation threw an
     * exception
     * @throws InterruptedException if the current thread was interrupted
     * while waiting
     * @throws TimeoutException if the wait timed out
     */
    // 阻塞等待给定时间来获取异步计算结果
    V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException;
}
