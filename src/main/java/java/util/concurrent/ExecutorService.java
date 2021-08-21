/*
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util.concurrent;
import java.util.List;
import java.util.Collection;

/**
 * 20210813
 * A. 一个 {@link Executor} 提供管理终止的方法和可以生成 {@link Future} 以跟踪一个或多个异步任务进度的方法。
 * B. 可以关闭 {@code ExecutorService}，这将导致它拒绝新任务。 提供了两种不同的方法来关闭 {@code ExecutorService}。
 *    {@link #shutdown} 方法将允许先前提交的任务在终止之前执行，
 *    而 {@link #shutdownNow} 方法防止等待任务开始并尝试停止当前正在执行的任务。终止时，执行器没有正在执行的任务，没有等待执行的任务，也没有新的任务可以提交。
 *    应关闭未使用的 {@code ExecutorService} 以回收其资源。
 * C. 方法 {@code submit} 通过创建和返回可用于取消执行或者等待完成的 {@link Future} 来扩展基本方法 {@link Executor#execute(Runnable)}。
 *    方法 {@code invokeAny} 和 {@code invokeAll} 执行最常用的批量执行形式，执行一组任务，然后等待至少一个或全部完成。
 *    （类 {@link ExecutorCompletionService} 可用于编写这些方法的自定义变体。）
 * D. {@link Executors} 类为此包中提供的执行程序服务提供工厂方法。
 * E. 使用示例: 这是网络服务的草图，其中线程池中的线程为传入请求提供服务。它使用预配置的 {@link Executors#newFixedThreadPool} 工厂方法：
 * {@code
 * class NetworkService implements Runnable {
 *   private final ServerSocket serverSocket;
 *   private final ExecutorService pool;
 *
 *   public NetworkService(int port, int poolSize)
 *       throws IOException {
 *     serverSocket = new ServerSocket(port);
 *     pool = Executors.newFixedThreadPool(poolSize);
 *   }
 *
 *   public void run() { // run the service
 *     try {
 *       for (;;) {
 *         pool.execute(new Handler(serverSocket.accept()));
 *       }
 *     } catch (IOException ex) {
 *       pool.shutdown();
 *     }
 *   }
 * }
 *
 * class Handler implements Runnable {
 *   private final Socket socket;
 *   Handler(Socket socket) { this.socket = socket; }
 *   public void run() {
 *     // read and service request on socket
 *   }
 * }}
 * F. 以下方法分两个阶段关闭 {@code ExecutorService}，首先通过调用 {@code shutdown} 拒绝传入任务，然后在必要时调用 {@code shutdownNow} 取消任何延迟任务：
 * {@code
 * void shutdownAndAwaitTermination(ExecutorService pool) {
 *   pool.shutdown(); // Disable new tasks from being submitted 禁止提交新任务
 *   try {
 *     // Wait a while for existing tasks to terminate 等待现有任务终止
 *     if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
 *       pool.shutdownNow(); // Cancel currently executing tasks 取消当前正在执行的任务
 *
 *       // 等待任务响应被取消
 *       // Wait a while for tasks to respond to being cancelled
 *       if (!pool.awaitTermination(60, TimeUnit.SECONDS))
 *           System.err.println("Pool did not terminate");
 *     }
 *   } catch (InterruptedException ie) {
 *     // （重新）取消如果当前线程也被中断
 *     // (Re-)Cancel if current thread also interrupted
 *     pool.shutdownNow();
 *
 *     // 保留中断状态
 *     // Preserve interrupt status
 *     Thread.currentThread().interrupt();
 *   }
 * }}
 * G. 内存一致性影响：在将 {@code Runnable} 或 {@code Callable} 任务提交到 {@code ExecutorService} 之前线程中的操作发生在该任务采取的任何操作之前，
 *    而后者又发生在 结果通过 {@code Future.get()} 检索。
 */
/**
 * A.
 * An {@link Executor} that provides methods to manage termination and
 * methods that can produce a {@link Future} for tracking progress of
 * one or more asynchronous tasks.
 *
 * B.
 * <p>An {@code ExecutorService} can be shut down, which will cause
 * it to reject new tasks.  Two different methods are provided for
 * shutting down an {@code ExecutorService}. The {@link #shutdown}
 * method will allow previously submitted tasks to execute before
 * terminating, while the {@link #shutdownNow} method prevents waiting
 * tasks from starting and attempts to stop currently executing tasks.
 * Upon termination, an executor has no tasks actively executing, no
 * tasks awaiting execution, and no new tasks can be submitted.  An
 * unused {@code ExecutorService} should be shut down to allow
 * reclamation of its resources.
 *
 * C.
 * <p>Method {@code submit} extends base method {@link
 * Executor#execute(Runnable)} by creating and returning a {@link Future}
 * that can be used to cancel execution and/or wait for completion.
 * Methods {@code invokeAny} and {@code invokeAll} perform the most
 * commonly useful forms of bulk execution, executing a collection of
 * tasks and then waiting for at least one, or all, to
 * complete. (Class {@link ExecutorCompletionService} can be used to
 * write customized variants of these methods.)
 *
 * D.
 * <p>The {@link Executors} class provides factory methods for the
 * executor services provided in this package.
 *
 * <h3>Usage Examples</h3>
 *
 * E.
 * Here is a sketch of a network service in which threads in a thread
 * pool service incoming requests. It uses the preconfigured {@link
 * Executors#newFixedThreadPool} factory method:
 *
 *  <pre> {@code
 * class NetworkService implements Runnable {
 *   private final ServerSocket serverSocket;
 *   private final ExecutorService pool;
 *
 *   public NetworkService(int port, int poolSize)
 *       throws IOException {
 *     serverSocket = new ServerSocket(port);
 *     pool = Executors.newFixedThreadPool(poolSize);
 *   }
 *
 *   public void run() { // run the service
 *     try {
 *       for (;;) {
 *         pool.execute(new Handler(serverSocket.accept()));
 *       }
 *     } catch (IOException ex) {
 *       pool.shutdown();
 *     }
 *   }
 * }
 *
 * class Handler implements Runnable {
 *   private final Socket socket;
 *   Handler(Socket socket) { this.socket = socket; }
 *   public void run() {
 *     // read and service request on socket
 *   }
 * }}</pre>
 *
 * F.
 * The following method shuts down an {@code ExecutorService} in two phases,
 * first by calling {@code shutdown} to reject incoming tasks, and then
 * calling {@code shutdownNow}, if necessary, to cancel any lingering tasks:
 *
 *  <pre> {@code
 * void shutdownAndAwaitTermination(ExecutorService pool) {
 *   pool.shutdown(); // Disable new tasks from being submitted
 *   try {
 *     // Wait a while for existing tasks to terminate
 *     if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
 *       pool.shutdownNow(); // Cancel currently executing tasks
 *       // Wait a while for tasks to respond to being cancelled
 *       if (!pool.awaitTermination(60, TimeUnit.SECONDS))
 *           System.err.println("Pool did not terminate");
 *     }
 *   } catch (InterruptedException ie) {
 *     // (Re-)Cancel if current thread also interrupted
 *     pool.shutdownNow();
 *     // Preserve interrupt status
 *     Thread.currentThread().interrupt();
 *   }
 * }}</pre>
 *
 * G.
 * <p>Memory consistency effects: Actions in a thread prior to the
 * submission of a {@code Runnable} or {@code Callable} task to an
 * {@code ExecutorService}
 * <a href="package-summary.html#MemoryVisibility"><i>happen-before</i></a>
 * any actions taken by that task, which in turn <i>happen-before</i> the
 * result is retrieved via {@code Future.get()}.
 *
 * @since 1.5
 * @author Doug Lea
 */
public interface ExecutorService extends Executor {

    /**
     * 20210813
     * A. 启动有序关闭，其中执行先前提交的任务，但不会接受新任务。 如果已经关闭，调用没有额外的效果。
     * B. 此方法不等待先前提交的任务完成执行。 使用 {@link #awaitTermination awaitTermination} 来做到这一点。
     */
    /**
     * A.
     * Initiates an orderly shutdown in which previously submitted
     * tasks are executed, but no new tasks will be accepted.
     * Invocation has no additional effect if already shut down.
     *
     * B.
     * <p>This method does not wait for previously submitted tasks to
     * complete execution.  Use {@link #awaitTermination awaitTermination}
     * to do that.
     *
     * // SecurityException 如果安全管理器存在并且关闭此 ExecutorService 可能会操纵调用者不允许修改的线程，
     *    因为它不持有 {@link java.lang.RuntimePermission}{@code ("modifyThread")}，或安全管理器的 {@code checkAccess} 方法拒绝访问。
     * @throws SecurityException if a security manager exists and
     *         shutting down this ExecutorService may manipulate
     *         threads that the caller is not permitted to modify
     *         because it does not hold {@link
     *         java.lang.RuntimePermission}{@code ("modifyThread")},
     *         or the security manager's {@code checkAccess} method
     *         denies access.
     */
    // 有序关闭Executor, 会执行先前提交的任务, 但不会接受新任务; 如果已经关闭, 则调用没有额外的效果; 此方法不等待先前提交的任务完成执行, 可以使用{@link #awaitTermination awaitTermination}来做到这一点
    void shutdown();

    /**
     * 20210813
     * A. 尝试停止所有正在执行的任务，停止等待任务的处理，并返回等待执行的任务列表。
     * B. 此方法不会等待主动执行的任务终止。 使用 {@link #awaitTermination awaitTermination} 来做到这一点。
     * C. 除了尽力尝试停止处理正在执行的任务之外，没有任何保证。 例如，典型的实现将通过 {@link Thread#interrupt} 取消，因此任何未能响应中断的任务可能永远不会终止。
     */
    /**
     * A.
     * Attempts to stop all actively executing tasks, halts the
     * processing of waiting tasks, and returns a list of the tasks
     * that were awaiting execution.
     *
     * B.
     * <p>This method does not wait for actively executing tasks to
     * terminate.  Use {@link #awaitTermination awaitTermination} to
     * do that.
     *
     * C.
     * <p>There are no guarantees beyond best-effort attempts to stop
     * processing actively executing tasks.  For example, typical
     * implementations will cancel via {@link Thread#interrupt}, so any
     * task that fails to respond to interrupts may never terminate.
     *
     * @return list of tasks that never commenced execution
     * @throws SecurityException if a security manager exists and
     *         shutting down this ExecutorService may manipulate
     *         threads that the caller is not permitted to modify
     *         because it does not hold {@link
     *         java.lang.RuntimePermission}{@code ("modifyThread")},
     *         or the security manager's {@code checkAccess} method
     *         denies access.
     */
    // 有序关闭Executor, 会停止所有正在执行的任务, 停止等待任务的处理, 并返回等待执行的任务列表; 此方法不等待先前提交的任务完成执行, 可以使用{@link #awaitTermination awaitTermination}来做到这一点
    List<Runnable> shutdownNow();

    // 如果此执行程序已关闭，则返回 {@code true}。
    /**
     * Returns {@code true} if this executor has been shut down.
     *
     * @return {@code true} if this executor has been shut down
     */
    // 判断Executor是否已经关闭, 如果是则返回{@code true}
    boolean isShutdown();

    // 如果关闭后所有任务都已完成，则返回 {@code true}。请注意，除非{@code isTerminated} 永远不会{@code true}。{@code shutdown} 或 {@code shutdownNow} 首先被调用。
    /**
     * Returns {@code true} if all tasks have completed following shut down.
     * Note that {@code isTerminated} is never {@code true} unless
     * either {@code shutdown} or {@code shutdownNow} was called first.
     *
     * // {@code true} 如果关闭后所有任务都已完成
     * @return {@code true} if all tasks have completed following shut down
     */
    // 如果关闭后所有任务都已完成, 则返回true
    boolean isTerminated();

    // 阻塞直到所有任务在关闭请求后完成执行，或发生超时，或当前线程被中断，以先发生者为准。
    /**
     * Blocks until all tasks have completed execution after a shutdown
     * request, or the timeout occurs, or the current thread is
     * interrupted, whichever happens first.
     *
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout argument
     *
     * // {@code true} 如果此执行程序终止。{@code false} 如果在终止前超时已过。
     * @return {@code true} if this executor terminated and
     *         {@code false} if the timeout elapsed before termination
     *
     * // 如果在等待时中断
     * @throws InterruptedException if interrupted while waiting
     */
    // 阻塞直到所有任务在关闭请求后执行完成, 或发生超时, 或当前线程被中断, 以先发生者为准
    boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException;

    /**
     * 20210813
     * A. 提交一个返回值的任务以供执行，并返回一个表示任务未决结果的 Future。 Future 的 {@code get} 方法将在成功完成后返回任务的结果。
     * B. 如果您想立即阻止等待任务，可以使用 {@code result = exec.submit(aCallable).get();} 形式的构造
     * C. 注意：{@link Executors} 类包含一组方法，可以将一些其他常见的类似闭包的对象（例如，{@link java.security.PrivilegedAction}）转换为 {@link Callable} 表单，以便它们可以被提交。
     */
    /**
     * A.
     * Submits a value-returning task for execution and returns a
     * Future representing the pending results of the task. The
     * Future's {@code get} method will return the task's result upon
     * successful completion.
     *
     * B.
     * <p>
     * If you would like to immediately block waiting
     * for a task, you can use constructions of the form
     * {@code result = exec.submit(aCallable).get();}
     *
     * C.
     * <p>Note: The {@link Executors} class includes a set of methods
     * that can convert some other common closure-like objects,
     * for example, {@link java.security.PrivilegedAction} to
     * {@link Callable} form so they can be submitted.
     *
     * @param task the task to submit
     * @param <T> the type of the task's result
     *
     * // 代表待完成任务的 Future
     * @return a Future representing pending completion of the task
     *
     * // 如果无法安排任务执行
     * @throws RejectedExecutionException if the task cannot be scheduled for execution
     *
     * // 如果任务为空
     * @throws NullPointerException if the task is null
     */
    // 提交一个返回值的任务以供执行, 并返回一个表示任务未决结果的Future, Future的{@code get}方法将在成功完成后返回任务的结果
    <T> Future<T> submit(Callable<T> task);

    // 提交一个 Runnable 任务以供执行，并返回一个代表该任务的 Future。 Future 的 {@code get} 方法将在成功完成后返回给定的结果。
    /**
     * Submits a Runnable task for execution and returns a Future
     * representing that task. The Future's {@code get} method will
     * return the given result upon successful completion.
     *
     * @param task the task to submit
     * @param result the result to return
     * @param <T> the type of the result
     *
     * // 代表待完成任务的 Future
     * @return a Future representing pending completion of the task
     *
     * // 如果无法安排任务执行
     * @throws RejectedExecutionException if the task cannot be scheduled for execution
     *
     * // 如果任务为空
     * @throws NullPointerException if the task is null
     */
    // 提交一个Runnable任务以供执行，并返回一个代表该任务的Future, Future的{@code get}方法将在成功完成后返回给定的结果
    <T> Future<T> submit(Runnable task, T result);

    // 提交一个 Runnable 任务以供执行，并返回一个代表该任务的 Future。 Future 的 {@code get} 方法将在成功完成后返回 {@code null}。
    /**
     * Submits a Runnable task for execution and returns a Future
     * representing that task. The Future's {@code get} method will
     * return {@code null} upon <em>successful</em> completion.
     *
     * @param task the task to submit
     *
     * // 代表待完成任务的 Future
     * @return a Future representing pending completion of the task
     *
     * // 如果无法安排任务执行
     * @throws RejectedExecutionException if the task cannot be scheduled for execution
     *
     * // 如果任务为空
     * @throws NullPointerException if the task is null
     */
    // 提交一个 Runnable 任务以供执行，并返回一个代表该任务的 Future。 Future 的 {@code get} 方法将在成功完成后返回 {@code null}
    Future<?> submit(Runnable task);

    /**
     * 20210813
     * 执行给定的任务，返回一个 Futures 列表，在所有完成时保存它们的状态和结果。 {@link Future#isDone} 对于返回列表的每个元素都是 {@code true}。
     * 请注意，已完成的任务可能已正常终止或通过抛出异常终止。 如果在此操作进行时修改了给定的集合，则此方法的结果未定义。
     */
    /**
     * Executes the given tasks, returning a list of Futures holding
     * their status and results when all complete.
     * {@link Future#isDone} is {@code true} for each
     * element of the returned list.
     * Note that a <em>completed</em> task could have
     * terminated either normally or by throwing an exception.
     * The results of this method are undefined if the given
     * collection is modified while this operation is in progress.
     *
     * @param tasks the collection of tasks
     * @param <T> the type of the values returned from the tasks
     *
     * // 代表任务的 Future 列表，与迭代器为给定任务列表生成的顺序相同，每个任务都已完成
     * @return a list of Futures representing the tasks, in the same
     *         sequential order as produced by the iterator for the
     *         given task list, each of which has completed
     *
     * // 如果在等待时中断，则取消未完成的任务
     * @throws InterruptedException if interrupted while waiting, in which case unfinished tasks are cancelled
     *
     * // 如果任务或其任何元素为 {@code null}
     * @throws NullPointerException if tasks or any of its elements are {@code null}
     *
     * // 如果无法安排任何任务执行
     * @throws RejectedExecutionException if any task cannot be scheduled for execution
     */
    // 执行给定的任务，返回一个 Futures 列表，在所有完成时保存它们的状态和结果。如果在此操作进行时修改了给定的集合，则此方法的结果未定义。
    <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException;

    /**
     * 20210813
     * 执行给定的任务，当全部完成或超时到期时，返回一个保存其状态和结果的 Futures 列表，以先发生者为准。
     * {@link Future#isDone} 对于返回列表的每个元素都是 {@code true}。 返回后，未完成的任务将被取消。
     * 请注意，已完成的任务可能已正常终止或通过抛出异常终止。 如果在此操作进行时修改了给定的集合，则此方法的结果未定义。
     */
    /**
     * Executes the given tasks, returning a list of Futures holding
     * their status and results
     * when all complete or the timeout expires, whichever happens first.
     * {@link Future#isDone} is {@code true} for each
     * element of the returned list.
     * Upon return, tasks that have not completed are cancelled.
     * Note that a <em>completed</em> task could have
     * terminated either normally or by throwing an exception.
     * The results of this method are undefined if the given
     * collection is modified while this operation is in progress.
     *
     * @param tasks the collection of tasks
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout argument
     * @param <T> the type of the values returned from the tasks
     *
     * // 表示任务的 Future 列表，其顺序与迭代器为给定任务列表生成的顺序相同。 如果操作没有超时，则每个任务都已完成。 如果它确实超时，其中一些任务将无法完成。
     * @return a list of Futures representing the tasks, in the same
     *         sequential order as produced by the iterator for the
     *         given task list. If the operation did not time out,
     *         each task will have completed. If it did time out, some
     *         of these tasks will not have completed.
     *
     * // 如果在等待时中断，则取消未完成的任务
     * @throws InterruptedException if interrupted while waiting, in which case unfinished tasks are cancelled
     *
     * 如果任务、其任何元素或单元为 {@code null}
     * @throws NullPointerException if tasks, any of its elements, or unit are {@code null}
     *
     * // 如果无法安排任何任务执行
     * @throws RejectedExecutionException if any task cannot be scheduled for execution
     */
    // 执行给定的任务，当全部完成或超时到期时，返回一个保存其状态和结果的 Futures 列表，以先发生者为准。如果在此操作进行时修改了给定的集合，则此方法的结果未定义。
    <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException;

    // 执行给定的任务，返回成功完成的任务的结果（即不抛出异常），如果有的话。在正常或异常返回时，未完成的任务将被取消。 如果在此操作进行时修改了给定的集合，则此方法的结果未定义。
    /**
     * Executes the given tasks, returning the result
     * of one that has completed successfully (i.e., without throwing
     * an exception), if any do. Upon normal or exceptional return,
     * tasks that have not completed are cancelled.
     * The results of this method are undefined if the given
     * collection is modified while this operation is in progress.
     *
     * @param tasks the collection of tasks
     * @param <T> the type of the values returned from the tasks
     *
     * // 其中一项任务返回的结果
     * @return the result returned by one of the tasks
     *
     * // 如果在等待时中断
     * @throws InterruptedException if interrupted while waiting
     *
     * // 如果要执行的任务或任何元素任务是 {@code null}
     * @throws NullPointerException if tasks or any element task subject to execution is {@code null}
     *
     * // 如果任务为空
     * @throws IllegalArgumentException if tasks is empty
     *
     * // 如果没有任务成功完成
     * @throws ExecutionException if no task successfully completes
     *
     * // 如果无法安排任务执行
     * @throws RejectedExecutionException if tasks cannot be scheduled for execution
     */
    // 执行给定的任务，返回成功完成的任务的结果（即不抛出异常），如果有的话。在正常或异常返回时，未完成的任务将被取消。如果在此操作进行时修改了给定的集合，则此方法的结果未定义。
    <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException;

    // 执行给定的任务，返回已成功完成的任务的结果（即，不抛出异常），如果在给定的超时时间之前执行任何操作。 在正常或异常返回时，未完成的任务将被取消。如果在此操作进行时修改了给定的集合，则此方法的结果未定义。
    /**
     * Executes the given tasks, returning the result
     * of one that has completed successfully (i.e., without throwing
     * an exception), if any do before the given timeout elapses.
     * Upon normal or exceptional return, tasks that have not
     * completed are cancelled.
     * The results of this method are undefined if the given
     * collection is modified while this operation is in progress.
     *
     * @param tasks the collection of tasks
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout argument
     * @param <T> the type of the values returned from the tasks
     *
     * // 其中一项任务返回的结果
     * @return the result returned by one of the tasks
     *
     * // 如果在等待时中断
     * @throws InterruptedException if interrupted while waiting
     *
     * // 如果要执行的任务、单元或任何元素任务是 {@code null}
     * @throws NullPointerException if tasks, or unit, or any element task subject to execution is {@code null}
     *
     * // 如果在任何任务成功完成之前给定的超时时间过去
     * @throws TimeoutException if the given timeout elapses before any task successfully completes
     *
     * // 如果没有任务成功完成
     * @throws ExecutionException if no task successfully completes
     *
     * // 如果无法安排任务执行
     * @throws RejectedExecutionException if tasks cannot be scheduled for execution
     */
    // 执行给定的任务，返回已成功完成的任务的结果（即不抛出异常），如果在给定的超时时间之前执行任何操作。 在正常或异常返回时，未完成的任务将被取消。如果在此操作进行时修改了给定的集合，则此方法的结果未定义。
    <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException;
}
