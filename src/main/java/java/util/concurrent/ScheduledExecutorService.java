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
 * 20210815
 * A. 一个 {@link ExecutorService}，可以安排命令在给定的延迟后运行，或定期执行。
 * B. {@code schedule} 方法创建具有各种延迟的任务并返回可用于取消或检查执行的任务对象。
 *    {@code scheduleAtFixedRate} 和 {@code scheduleWithFixedDelay} 方法创建和执行定期运行直到被取消的任务。
 * C. 使用 {@link Executor#execute(Runnable)} 和 {@link ExecutorService} {@code submit} 方法提交的命令的调度请求延迟为零。
 *    {@code schedule} 方法中也允许零延迟和负延迟（但不是周期），并被视为立即执行的请求。
 * D. 所有 {@code schedule} 方法都接受相对延迟和时间段作为参数，而不是绝对时间或日期。 将表示为 {@link java.util.Date} 的绝对时间转换为所需的形式是一件简单的事情。
 *    例如，要安排在某个未来的 {@code date}，您可以使用：
 *    {@code schedule(task, date.getTime() - System.currentTimeMillis(), TimeUnit.MILLISECONDS)}。
 *    但是请注意，由于网络时间同步协议、时钟漂移或其他因素，相对延迟的到期不必与启用任务的当前 {@code Date} 重合。
 * E. {@link Executors} 类为此包中提供的 ScheduledExecutorService 实现提供了方便的工厂方法。
 * F. 这是一个带有方法的类，该类将 ScheduledExecutorService 设置为每 10 秒发出哔声一小时：
 * {@code
 * import static java.util.concurrent.TimeUnit.*;
 *
 * class BeeperControl {
 *   private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
 *
 *   public void beepForAnHour() {
 *
 *     final Runnable beeper = new Runnable() {
 *       public void run() { System.out.println("beep"); }
 *     };
 *
 *     final ScheduledFuture beeperHandle = scheduler.scheduleAtFixedRate(beeper, 10, 10, SECONDS);
 *     scheduler.schedule(new Runnable() {
 *       public void run() { beeperHandle.cancel(true); }
 *     }, 60 * 60, SECONDS);
 *   }
 * }}
 */
/**
 * A.
 * An {@link ExecutorService} that can schedule commands to run after a given
 * delay, or to execute periodically.
 *
 * B.
 * <p>The {@code schedule} methods create tasks with various delays
 * and return a task object that can be used to cancel or check
 * execution. The {@code scheduleAtFixedRate} and
 * {@code scheduleWithFixedDelay} methods create and execute tasks
 * that run periodically until cancelled.
 *
 * C.
 * <p>Commands submitted using the {@link Executor#execute(Runnable)}
 * and {@link ExecutorService} {@code submit} methods are scheduled
 * with a requested delay of zero. Zero and negative delays (but not
 * periods) are also allowed in {@code schedule} methods, and are
 * treated as requests for immediate execution.
 *
 * D.
 * <p>All {@code schedule} methods accept <em>relative</em> delays and
 * periods as arguments, not absolute times or dates. It is a simple
 * matter to transform an absolute time represented as a {@link
 * java.util.Date} to the required form. For example, to schedule at
 * a certain future {@code date}, you can use: {@code schedule(task,
 * date.getTime() - System.currentTimeMillis(),
 * TimeUnit.MILLISECONDS)}. Beware however that expiration of a
 * relative delay need not coincide with the current {@code Date} at
 * which the task is enabled due to network time synchronization
 * protocols, clock drift, or other factors.
 *
 * E.
 * <p>The {@link Executors} class provides convenient factory methods for
 * the ScheduledExecutorService implementations provided in this package.
 *
 * <h3>Usage Example</h3>
 *
 * F.
 * Here is a class with a method that sets up a ScheduledExecutorService
 * to beep every ten seconds for an hour:
 *
 *  <pre> {@code
 * import static java.util.concurrent.TimeUnit.*;
 * class BeeperControl {
 *   private final ScheduledExecutorService scheduler =
 *     Executors.newScheduledThreadPool(1);
 *
 *   public void beepForAnHour() {
 *     final Runnable beeper = new Runnable() {
 *       public void run() { System.out.println("beep"); }
 *     };
 *     final ScheduledFuture<?> beeperHandle =
 *       scheduler.scheduleAtFixedRate(beeper, 10, 10, SECONDS);
 *     scheduler.schedule(new Runnable() {
 *       public void run() { beeperHandle.cancel(true); }
 *     }, 60 * 60, SECONDS);
 *   }
 * }}</pre>
 *
 * @since 1.5
 * @author Doug Lea
 */
public interface ScheduledExecutorService extends ExecutorService {

    // 创建并执行在给定延迟后启用的一次性操作。
    /**
     * Creates and executes a one-shot action that becomes enabled
     * after the given delay.
     *
     * // 要执行的任务
     * @param command the task to execute
     *
     * // 从现在开始延迟执行的时间
     * @param delay the time from now to delay execution
     *
     * // 延迟参数的时间单位
     * @param unit the time unit of the delay parameter
     *
     * // 一个 ScheduledFuture 表示待完成的任务，其 {@code get()} 方法将在完成后返回 {@code null}
     * @return a ScheduledFuture representing pending completion of
     *         the task and whose {@code get()} method will return
     *         {@code null} upon completion
     * @throws RejectedExecutionException if the task cannot be
     *         scheduled for execution
     * @throws NullPointerException if command is null
     */
    // 在给定延迟后执行一次性的command任务
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit);

    // 创建并执行在给定延迟后启用的 ScheduledFuture。
    /**
     * Creates and executes a ScheduledFuture that becomes enabled after the
     * given delay.
     *
     * @param callable the function to execute
     * @param delay the time from now to delay execution
     * @param unit the time unit of the delay parameter
     *
     * // 可调用结果的类型
     * @param <V> the type of the callable's result
     *
     * // 可用于提取结果或取消的 ScheduledFuture
     * @return a ScheduledFuture that can be used to extract result or cancel
     * @throws RejectedExecutionException if the task cannot be
     *         scheduled for execution
     * @throws NullPointerException if callable is null
     */
    // 在给定延迟后执行一次性的command任务
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit);

    /**
     * 20210815
     * 创建并执行一个周期性动作，在给定的初始延迟后首先启用，然后在给定的时间段内启用； 即执行将在 {@code initialDelay} 之后开始，
     * 然后是 {@code initialDelay+period}，然后是 {@code initialDelay + 2 * period}，依此类推。 如果任务的任何执行遇到异常，则后续执行将被抑制。
     * 否则，任务只会通过取消或终止执行程序而终止。 如果此任务的任何执行时间超过其周期，则后续执行可能会延迟开始，但不会并发执行。
     */
    /**
     * Creates and executes a periodic action that becomes enabled first
     * after the given initial delay, and subsequently with the given
     * period; that is executions will commence after
     * {@code initialDelay} then {@code initialDelay+period}, then
     * {@code initialDelay + 2 * period}, and so on.
     * If any execution of the task
     * encounters an exception, subsequent executions are suppressed.
     * Otherwise, the task will only terminate via cancellation or
     * termination of the executor.  If any execution of this task
     * takes longer than its period, then subsequent executions
     * may start late, but will not concurrently execute.
     *
     * // 要执行的任务
     * @param command the task to execute
     *
     * // 延迟第一次执行的时间
     * @param initialDelay the time to delay first execution
     *
     * // 连续执行之间的时间间隔
     * @param period the period between successive executions
     *
     * // initialDelay 和 period 参数的时间单位
     * @param unit the time unit of the initialDelay and period parameters
     *
     * // 一个 ScheduledFuture 表示待完成的任务，其 {@code get()} 方法将在取消时抛出异常
     * @return a ScheduledFuture representing pending completion of
     *         the task, and whose {@code get()} method will throw an
     *         exception upon cancellation
     * @throws RejectedExecutionException if the task cannot be
     *         scheduled for execution
     * @throws NullPointerException if command is null
     * @throws IllegalArgumentException if period less than or equal to zero
     */
    // (相对周期)创建并执行一个周期性动作, 任务会在initialDelay后执行, 之后则以period为周期执行, 如果遇到异常, 则会终止后续执行; 如果执行时间超过period, 后续执行则会延后而不是并发执行
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command,
                                                  long initialDelay,
                                                  long period,
                                                  TimeUnit unit);

    /**
     * 20210815
     * 创建并执行一个周期性动作，该动作首先在给定的初始延迟后启用，随后在一个执行终止和下一个执行开始之间具有给定的延迟。
     * 如果任务的任何执行遇到异常，则后续执行将被抑制。 否则，任务只会通过取消或终止执行程序而终止。
     */
    /**
     * Creates and executes a periodic action that becomes enabled first
     * after the given initial delay, and subsequently with the
     * given delay between the termination of one execution and the
     * commencement of the next.  If any execution of the task
     * encounters an exception, subsequent executions are suppressed.
     * Otherwise, the task will only terminate via cancellation or
     * termination of the executor.
     *
     * @param command the task to execute
     * @param initialDelay the time to delay first execution
     *
     * // 一个执行终止和下一个执行开始之间的延迟
     * @param delay the delay between the termination of one
     * execution and the commencement of the next
     * @param unit the time unit of the initialDelay and delay parameters
     * @return a ScheduledFuture representing pending completion of
     *         the task, and whose {@code get()} method will throw an
     *         exception upon cancellation
     * @throws RejectedExecutionException if the task cannot be
     *         scheduled for execution
     * @throws NullPointerException if command is null
     * @throws IllegalArgumentException if delay less than or equal to zero
     */
    // (绝对周期)创建并执行一个周期性动作, 任务会在initialDelay后执行, 任务执行结束之后则延迟delay后再次执行, 任务执行慢时可能会有并发执行
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command,
                                                     long initialDelay,
                                                     long delay,
                                                     TimeUnit unit);

}
