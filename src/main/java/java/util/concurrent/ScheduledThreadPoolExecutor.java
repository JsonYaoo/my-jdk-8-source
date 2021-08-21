/*
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util.concurrent;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.*;

/**
 * 20210815
 * A. 一个 {@link ThreadPoolExecutor} 可以额外安排命令在给定延迟后运行，或定期执行。
 *    当需要多个工作线程时，或者需要 {@link ThreadPoolExecutor}（此类扩展）的额外灵活性或功能时，此类比 {@link java.util.Timer} 更可取。
 * B. 延迟任务在启用后立即执行，但没有任何关于启用后何时开始的实时保证。 计划执行时间完全相同的任务按提交的先进先出 (FIFO) 顺序启用。
 * C. 当提交的任务在运行之前被取消时，执行会被抑制。 默认情况下，此类取消的任务不会自动从工作队列中删除，直到其延迟结束。
 *    虽然这可以实现进一步的检查和监控，但它也可能导致取消任务的无限保留。 为避免这种情况，请将 {@link #setRemoveOnCancelPolicy} 设置为 {@code true}，
 *    这会导致任务在取消时立即从工作队列中删除。
 * D. 通过 {@code scheduleAtFixedRate} 或 {@code scheduleWithFixedDelay} 安排的任务的连续执行不重叠。
 *    虽然不同的执行可能由不同的线程执行，但先前执行的效果发生在后续执行的效果之前。
 * E. 虽然这个类继承自 {@link ThreadPoolExecutor}，但一些继承的调优方法对它没有用。 特别是，因为它充当使用 {@code corePoolSize} 线程和无界队列的固定大小的池，
 *    因此对 {@code maximumPoolSize} 的调整没有任何有用的效果。
 *    此外，将 {@code corePoolSize} 设置为零或使用 {@code allowCoreThreadTimeOut} 几乎从来都不是一个好主意，因为一旦它们有资格运行，这可能会使池没有线程来处理任务。
 * F. 扩展说明：该类覆盖了 {@link ThreadPoolExecutor#execute(Runnable) execute} 和 {@link AbstractExecutorService#submit(Runnable) submit} 方法来生成内部
 *    {@link ScheduledFuture} 对象来控制每个任务的延迟和调度。 为了保留功能，子类中这些方法的任何进一步覆盖都必须调用超类版本，这有效地禁用了额外的任务自定义。
 *    但是，此类提供了替代的受保护扩展方法 {@code decorateTask}（{@code Runnable} 和 {@code Callable} 各有一个版本），可用于自定义用于执行通过
 *    {@code 输入的命令的具体任务类型 执行}、{@code submit}、{@code schedule}、{@code scheduleAtFixedRate} 和 {@code scheduleWithFixedDelay}。
 *    默认情况下，{@code ScheduledThreadPoolExecutor} 使用扩展 {@link FutureTask} 的任务类型。 但是，这可以使用以下形式的子类进行修改或替换：
 * {@code
 * public class CustomScheduledExecutor extends ScheduledThreadPoolExecutor {
 *
 *   static class CustomTask implements RunnableScheduledFuture { ... }
 *
 *   protected  RunnableScheduledFuture decorateTask(
 *                Runnable r, RunnableScheduledFuture task) {
 *       return new CustomTask(r, task);
 *   }
 *
 *   protected  RunnableScheduledFuture decorateTask(
 *                Callable c, RunnableScheduledFuture task) {
 *       return new CustomTask(c, task);
 *   }
 *   // ... add constructors, etc.
 * }}
 */
/**
 * A.
 * A {@link ThreadPoolExecutor} that can additionally schedule
 * commands to run after a given delay, or to execute
 * periodically. This class is preferable to {@link java.util.Timer}
 * when multiple worker threads are needed, or when the additional
 * flexibility or capabilities of {@link ThreadPoolExecutor} (which
 * this class extends) are required.
 *
 * B.
 * <p>Delayed tasks execute no sooner than they are enabled, but
 * without any real-time guarantees about when, after they are
 * enabled, they will commence. Tasks scheduled for exactly the same
 * execution time are enabled in first-in-first-out (FIFO) order of
 * submission.
 *
 * C.
 * <p>When a submitted task is cancelled before it is run, execution
 * is suppressed. By default, such a cancelled task is not
 * automatically removed from the work queue until its delay
 * elapses. While this enables further inspection and monitoring, it
 * may also cause unbounded retention of cancelled tasks. To avoid
 * this, set {@link #setRemoveOnCancelPolicy} to {@code true}, which
 * causes tasks to be immediately removed from the work queue at
 * time of cancellation.
 *
 * D.
 * <p>Successive executions of a task scheduled via
 * {@code scheduleAtFixedRate} or
 * {@code scheduleWithFixedDelay} do not overlap. While different
 * executions may be performed by different threads, the effects of
 * prior executions <a
 * href="package-summary.html#MemoryVisibility"><i>happen-before</i></a>
 * those of subsequent ones.
 *
 * E.
 * <p>While this class inherits from {@link ThreadPoolExecutor}, a few
 * of the inherited tuning methods are not useful for it. In
 * particular, because it acts as a fixed-sized pool using
 * {@code corePoolSize} threads and an unbounded queue, adjustments
 * to {@code maximumPoolSize} have no useful effect. Additionally, it
 * is almost never a good idea to set {@code corePoolSize} to zero or
 * use {@code allowCoreThreadTimeOut} because this may leave the pool
 * without threads to handle tasks once they become eligible to run.
 *
 * F.
 * <p><b>Extension notes:</b> This class overrides the
 * {@link ThreadPoolExecutor#execute(Runnable) execute} and
 * {@link AbstractExecutorService#submit(Runnable) submit}
 * methods to generate internal {@link ScheduledFuture} objects to
 * control per-task delays and scheduling.  To preserve
 * functionality, any further overrides of these methods in
 * subclasses must invoke superclass versions, which effectively
 * disables additional task customization.  However, this class
 * provides alternative protected extension method
 * {@code decorateTask} (one version each for {@code Runnable} and
 * {@code Callable}) that can be used to customize the concrete task
 * types used to execute commands entered via {@code execute},
 * {@code submit}, {@code schedule}, {@code scheduleAtFixedRate},
 * and {@code scheduleWithFixedDelay}.  By default, a
 * {@code ScheduledThreadPoolExecutor} uses a task type extending
 * {@link FutureTask}. However, this may be modified or replaced using
 * subclasses of the form:
 *
 *  <pre> {@code
 * public class CustomScheduledExecutor extends ScheduledThreadPoolExecutor {
 *
 *   static class CustomTask<V> implements RunnableScheduledFuture<V> { ... }
 *
 *   protected <V> RunnableScheduledFuture<V> decorateTask(
 *                Runnable r, RunnableScheduledFuture<V> task) {
 *       return new CustomTask<V>(r, task);
 *   }
 *
 *   protected <V> RunnableScheduledFuture<V> decorateTask(
 *                Callable<V> c, RunnableScheduledFuture<V> task) {
 *       return new CustomTask<V>(c, task);
 *   }
 *   // ... add constructors, etc.
 * }}</pre>
 *
 * @since 1.5
 * @author Doug Lea
 */
public class ScheduledThreadPoolExecutor extends ThreadPoolExecutor implements ScheduledExecutorService {

    /**
     * 20210816
     * 此类专门用于 ThreadPoolExecutor 实现:
     * 1. 使用自定义任务类型 ScheduledFutureTask 用于任务，即使是那些不需要调度的任务（即使用 ExecutorService 执行提交的任务，
     *    而不是 ScheduledExecutorService 方法），这些任务被视为延迟为零的延迟任务。
     * 2. 使用自定义队列（DelayedWorkQueue），无界DelayQueue的变种。 与 ThreadPoolExecutor 相比，
     *    缺少容量约束以及 corePoolSize 和 maximumPoolSize 实际上相同的事实简化了一些执行机制（请参阅 delayExecute）。
     * 3. 支持可选的run-after-shutdown参数，这会导致覆盖关闭方法以删除和取消关闭后不应运行的任务，以及当任务（重新）提交与关闭重叠时不同的重新检查逻辑。
     * 4. 允许拦截和检测的任务装饰方法，这是必需的，因为子类不能以其他方式覆盖提交方法以获得此效果。 不过，这些对池控制逻辑没有任何影响。
     */
    /*
     * This class specializes ThreadPoolExecutor implementation by
     *
     * 1. Using a custom task type, ScheduledFutureTask for
     *    tasks, even those that don't require scheduling (i.e.,
     *    those submitted using ExecutorService execute, not
     *    ScheduledExecutorService methods) which are treated as
     *    delayed tasks with a delay of zero.
     *
     * 2. Using a custom queue (DelayedWorkQueue), a variant of
     *    unbounded DelayQueue. The lack of capacity constraint and
     *    the fact that corePoolSize and maximumPoolSize are
     *    effectively identical simplifies some execution mechanics
     *    (see delayedExecute) compared to ThreadPoolExecutor.
     *
     * 3. Supporting optional run-after-shutdown parameters, which
     *    leads to overrides of shutdown methods to remove and cancel
     *    tasks that should NOT be run after shutdown, as well as
     *    different recheck logic when task (re)submission overlaps
     *    with a shutdown.
     *
     * 4. Task decoration methods to allow interception and
     *    instrumentation, which are needed because subclasses cannot
     *    otherwise override submit methods to get this effect. These
     *    don't have any impact on pool control logic though.
     */

    // 如果应该在关机时取消/抑制定期任务，则为 False。
    /**
     * False if should cancel/suppress periodic tasks on shutdown.
     */
    // 判断是否应该在关闭时取消定期任务, 如果需要取消则为false, 如果不需要取消则为true
    private volatile boolean continueExistingPeriodicTasksAfterShutdown;

    // 如果应该在关闭时取消非周期性任务，则为 False。
    /**
     * False if should cancel non-periodic tasks on shutdown.
     */
    // 判断是否应该在关闭时取消非周期性任务, 如果需要取消则为false, 如果不需要取消则为true
    private volatile boolean executeExistingDelayedTasksAfterShutdown = true;

    // 如果 ScheduledFutureTask.cancel 应该从队列中删除，则为 True
    /**
     * True if ScheduledFutureTask.cancel should remove from queue
     */
    // 判断ScheduledFutureTask取消后是否应该从任务队列中删除, 如果需要则为true, 如果不需要则为false
    private volatile boolean removeOnCancel = false;

    // 用于打破调度关系的序列号，进而保证绑定条目之间的 FIFO 顺序。
    /**
     * Sequence number to break scheduling ties, and in turn to
     * guarantee FIFO order among tied entries.
     */
    // 用于打破调度关系的序列号，进而保证绑定条目之间的 FIFO 顺序
    private static final AtomicLong sequencer = new AtomicLong();

    // 返回当前纳秒时间。
    /**
     * Returns current nanosecond time.
     */
    // 返回当前纳秒时间
    final long now() {
        return System.nanoTime();
    }

    // ScheduledFutureTask
    private class ScheduledFutureTask<V> extends FutureTask<V> implements RunnableScheduledFuture<V> {

        // 打破关系的序列号 FIFO
        /** Sequence number to break ties FIFO */
        // 用于打破调度关系的序列号，进而保证绑定条目之间的 FIFO 顺序
        private final long sequenceNumber;

        // 启用任务以纳米时间为单位执行的时间
        /** The time the task is enabled to execute in nanoTime units */
        // 启用任务以纳米时间, 作为单位执行的时间
        private long time;

        // 重复任务的周期（以纳秒为单位）。正值表示固定速率执行。负值表示固定延迟执行。0值表示非重复任务。
        /**
         * Period in nanoseconds for repeating tasks.  A positive
         * value indicates fixed-rate execution.  A negative value
         * indicates fixed-delay execution.  A value of 0 indicates a
         * non-repeating task.
         */
        // 重复任务的周期(以纳秒为单位), 正值表示固定速率(相对周期)执行, 负值表示固定延迟(绝对周期)执行, 0值表示非重复任务(非周期)
        private final long period;

        // reExecutePeriodic 重新入队的实际任务
        /** The actual task to be re-enqueued by reExecutePeriodic */
        // 对于周期任务, 在reExecutePeriodic重新入队的实际任务
        RunnableScheduledFuture<V> outerTask = this;

        /**
         * 20210523
         * 索引到延迟队列中，以支持更快的取消。
         */
        /**
         * Index into delay queue, to support faster cancellation.
         */
        // 索引到延迟队列中，以支持更快的取消
        int heapIndex;

        // 使用给定的基于 nanoTime 的触发时间创建一次性动作。
        /**
         * Creates a one-shot action with given nanoTime-based trigger time.
         */
        // 使用给定的基于nanoTime的触发时间创建一次性动作
        ScheduledFutureTask(Runnable r, V result, long ns) {
            super(r, result);
            this.time = ns;
            this.period = 0;
            this.sequenceNumber = sequencer.getAndIncrement();
        }

        // 创建具有给定纳米时间和周期的周期性动作。
        /**
         * Creates a periodic action with given nano time and period.
         */
        // 创建具有给定纳米时间和周期的周期性动作
        ScheduledFutureTask(Runnable r, V result, long ns, long period) {
            super(r, result);
            this.time = ns;
            this.period = period;
            this.sequenceNumber = sequencer.getAndIncrement();
        }

        // 使用给定的基于 nanoTime 的触发时间创建一次性动作。
        /**
         * Creates a one-shot action with given nanoTime-based trigger time.
         */
        // 使用给定的基于nanoTime的触发时间创建一次性动作
        ScheduledFutureTask(Callable<V> callable, long ns) {
            super(callable);
            this.time = ns;
            this.period = 0;
            this.sequenceNumber = sequencer.getAndIncrement();
        }

        // 以给定的时间单位返回与此对象关联的剩余延迟。
        public long getDelay(TimeUnit unit) {
            return unit.convert(time - now(), NANOSECONDS);
        }

        // Delayed接口继承Comparable接口, 比较延迟甚至序列号
        public int compareTo(Delayed other) {
            // 相同对象, 延迟相等
            if (other == this) // compare zero if same object // 如果相同的对象比较零
                return 0;

            // 如果为ScheduledFutureTask, 则继续比较
            if (other instanceof ScheduledFutureTask) {
                ScheduledFutureTask<?> x = (ScheduledFutureTask<?>)other;

                // 比较剩余延迟(纳秒), 如果当前对象的小于other对象的, 则返回-1, 代表小
                long diff = time - x.time;
                if (diff < 0)
                    return -1;
                // 如果当前对象的大于other对象的, 则返回1, 代表大
                else if (diff > 0)
                    return 1;
                // 如果延迟相等, 则继续比较序列号, 序列号小的则小, 序列号大的则大
                else if (sequenceNumber < x.sequenceNumber)
                    return -1;
                else
                    return 1;
            }

            // 如果不为ScheduledFutureTask, 则比较剩余延迟(纳秒), 如果当前对象的小于other对象的, 则返回-1, 代表小; 如果当前对象的大于other对象的, 则返回1, 代表大; 否则返回0, 代表相等
            long diff = getDelay(NANOSECONDS) - other.getDelay(NANOSECONDS);
            return (diff < 0) ? -1 : (diff > 0) ? 1 : 0;
        }

        // 如果这是一个周期性（不是一次性）动作，则返回 {@code true}。
        /**
         * Returns {@code true} if this is a periodic (not a one-shot) action.
         *
         * @return {@code true} if periodic
         */
        // 判断该任务是否为周期任务, 如果是则返回true, 否则返回false
        public boolean isPeriodic() {
            return period != 0;
        }

        // 设置下一次运行周期性任务的时间。
        /**
         * Sets the next time to run for a periodic task.
         */
        // 设置下一次运行周期性任务的时间
        private void setNextRunTime() {
            long p = period;

            // 如果p为正数, 说明为固定速率, 则执行时间time累加周期时间p(相对延迟)
            if (p > 0)
                time += p;
            // 如果p为0代表没有周期, 如果p为负数, 说明为固定延迟, 则当前时间+p延迟(绝对延迟)
            else
                time = triggerTime(-p);
        }

        // 中断FutureTask任务中的线程, 并从线程池任务队列中移除
        public boolean cancel(boolean mayInterruptIfRunning) {
            // 尝试取消此异步计算任务的执行, 如果任务已完成、已被取消或由于其他原因无法取消, 则返回false; 如果此任务在正常完成之前被取消, 则返回{@code true}
            boolean cancelled = super.cancel(mayInterruptIfRunning);

            // 如果此任务存在，则从执行程序的内部队列中删除该任务，从而导致它在尚未启动时无法运行
            if (cancelled && removeOnCancel && heapIndex >= 0)
                remove(this);

            return cancelled;
        }

        // 覆盖 FutureTask 版本，以便在定期重置/重新排队。
        /**
         * Overrides FutureTask version so as to reset/requeue if periodic.
         */
        // 如果为延迟任务, 则调用父类run方法运行; 如果为周期任务, 则调用runAndReset, 在运行后清空任务的线程、设置下一次运行的时间、以及重新到任务队列中排队
        public void run() {
            boolean periodic = isPeriodic();

            // 如果为周期任务, 且指定了退出不关闭周期任务, 则线程池为SHUTDOWN状态时返回false; 同理, 如果为延迟任务, 且指定了退出不关闭周期任务, 则线程池为SHUTDOWN状态时返回false
            if (!canRunInCurrentRunState(periodic))
                // 如果线程池为SHUTDOWN状态时不需要关闭周期或者延迟线程, 则不用中断FutureTask任务中的线程, 只需要从线程池任务队列中移除即可
                cancel(false);
            // 如果任务可以继续运行, 且为延迟任务时, 则调用父类FutureTask正常运行run方法
            else if (!periodic)
                ScheduledFutureTask.super.run();
            // 如果任务可以继续运行, 且为周期任务时, 则本质上是调用Callable#call()运行目标任务, 不将计算结果设置到outcome中, 但会设置异常结果, 返回true代表本次运行成功且仍然可以继续运行; 返回false代表本次运行失败且不可以继续运行
            else if (ScheduledFutureTask.super.runAndReset()) {
                // 如果本次运行成功且仍然可以继续运行, 则设置下一次运行周期性任务的时间
                setNextRunTime();

                // 周期任务重新排队, 排队后会再次检查是否需要继续运行, 如果需要则会保证一定有一个核心线程在运行, 否则会取消该任务、中断任务的线程
                reExecutePeriodic(outerTask);
            }
        }
    }

    // 如果可以在给定当前运行状态和关机后运行参数的情况下运行任务，则返回 true。
    /**
     * Returns true if can run a task given current run state
     * and run-after-shutdown parameters.
     *
     * // 如果此任务是周期性的，则为 true，如果延迟则为 false
     * @param periodic true if this task periodic, false if delayed
     */
    // 如果为周期任务, 且指定了退出不关闭周期任务, 则线程池为SHUTDOWN状态时返回false; 同理, 如果为延迟任务, 且指定了退出不关闭周期任务, 则线程池为SHUTDOWN状态时返回false
    boolean canRunInCurrentRunState(boolean periodic) {
        // 如果线程池为运行状态, 或者为关闭状态且已经允许关闭了, 则返回true
        return isRunningOrShutdown(periodic ?
                                   continueExistingPeriodicTasksAfterShutdown :
                                   executeExistingDelayedTasksAfterShutdown);
    }

    /**
     * 20210816
     * 延迟或周期性任务的主要执行方法。如果池关闭，则拒绝任务。否则，将任务添加到队列并在必要时启动一个线程来运行它。
     * （我们不能预先启动线程来运行任务，因为任务（可能）不应该运行。）如果在添加任务时池被关闭，如果状态和运行后需要取消并删除它 - 关机参数。
     *
     */
    /**
     * Main execution method for delayed or periodic tasks.  If pool
     * is shut down, rejects the task. Otherwise adds task to queue
     * and starts a thread, if necessary, to run it.  (We cannot
     * prestart the thread to run the task because the task (probably)
     * shouldn't be run yet.)  If the pool is shut down while the task
     * is being added, cancel and remove it if required by state and
     * run-after-shutdown parameters.
     *
     * @param task the task
     */
    // 执行延迟或者周期任务, 在任务队列追加任务后, 需要再次检查线程池以及任务状态, 如果任务还需要继续运行, 则要确保至少还有一个核心线程运; 如果任务不需要继续运行, 则从任务队列中移除并中断任务线程
    private void delayedExecute(RunnableScheduledFuture<?> task) {
        // 判断线程池是否已经关闭, 如果线程池已经关闭, 则履行任务command和当前任务执行者executor的拒绝策略
        if (isShutdown())
            reject(task);
        // 如果线程池还没关闭, 则将任务入队
        else {
            super.getQueue().add(task);

            // 再次检查线程池是否已经关闭, 如果已经关闭, 且任务不需要继续运行, 则从任务队列移除任务, 并中断任务线程取消任务的执行
            if (isShutdown() &&
                !canRunInCurrentRunState(task.isPeriodic()) &&
                remove(task))
                task.cancel(false);
            // 还没关闭, 或者任务还需要继续运行, 则确保至少还有一个核心线程运行
            else
                ensurePrestart();
        }
    }

    // 重新排队定期任务，除非当前运行状态排除它。 除了删除任务而不是拒绝之外，与delayedExecute 的想法相同。
    /**
     * Requeues a periodic task unless current run state precludes it.
     * Same idea as delayedExecute except drops task rather than rejecting.
     *
     * @param task the task
     */
    // 周期任务重新排队, 排队后会再次检查是否需要继续运行, 如果需要则会保证一定有一个核心线程在运行, 否则会取消该任务、中断任务的线程
    void reExecutePeriodic(RunnableScheduledFuture<?> task) {
        // 如果为周期任务, 且指定了退出不关闭周期任务, 则线程池为SHUTDOWN状态时返回false; 同理, 如果为延迟任务, 且指定了退出不关闭周期任务, 则线程池为SHUTDOWN状态时返回false
        if (canRunInCurrentRunState(true)) {
            // 线程池为SHUTDOWN状态时, 周期任务仍需继续运行, 则往任务队列中添加该周期任务
            super.getQueue().add(task);

            // 再次检查周期任务是否需要运行, 如果不需要继续运行, 且此任务存在，则从执行程序的内部队列中删除该任务，从而导致它在尚未启动时无法运行
            if (!canRunInCurrentRunState(true) && remove(task))
                // 尝试取消此异步计算任务的执行, 如果任务已完成、已被取消或由于其他原因无法取消, 则返回false; 如果此任务在正常完成之前被取消, 则返回{@code true}
                task.cancel(false);
            // 如果还需要继续运行, 则无论如何(即使核心线程数为0)也会启动一个核心线程, 使其空闲等待工作, 这将覆盖仅在执行新任务时才启动核心线程的默认策略
            else
                ensurePrestart();
        }
    }

    // 取消并清除由于关闭策略而不应运行的所有任务的队列。 在 super.shutdown 中调用。
    /**
     * Cancels and clears the queue of all tasks that should not be run
     * due to shutdown policy.  Invoked within super.shutdown.
     */
    // 取消并清除由于关闭策略而不应运行的所有任务的队列。 在 super.shutdown 中调用
    @Override
    void onShutdown() {
        // 获取任务队列
        BlockingQueue<Runnable> q = super.getQueue();

        // 判断是否应该在关闭时取消非周期性任务, 如果需要取消则为false, 如果不需要取消则为true
        boolean keepDelayed = getExecuteExistingDelayedTasksAfterShutdownPolicy();

        // 判断是否应该在关闭时取消定期任务, 如果需要取消则为false, 如果不需要取消则为true
        boolean keepPeriodic = getContinueExistingPeriodicTasksAfterShutdownPolicy();

        // 如果需要同时取消非周期以及定期任务, 则遍历任务队列, 取消所有任务, 并清空任务队列
        if (!keepDelayed && !keepPeriodic) {
            for (Object e : q.toArray())
                if (e instanceof RunnableScheduledFuture<?>)
                    ((RunnableScheduledFuture<?>) e).cancel(false);
            q.clear();
        }
        else {
            // 遍历快照以避免迭代器异常, 如果keepPeriodic或者keepDelayed为false, 则根据isPeriodic来取消并删除任务
            // Traverse snapshot to avoid iterator exceptions
            for (Object e : q.toArray()) {
                if (e instanceof RunnableScheduledFuture) {
                    RunnableScheduledFuture<?> t = (RunnableScheduledFuture<?>)e;
                    if ((t.isPeriodic() ? !keepPeriodic : !keepDelayed) ||
                        t.isCancelled()) { // also remove if already cancelled // 如果已经取消，也删除
                        if (q.remove(t))
                            t.cancel(false);
                    }
                }
            }
        }

        // 尝试线程池状态转换: STOP/SHUTDOWN -> 中断其中一个线程 -> TIDYING(工作线程数为0) -> TERMINATED状态 -> 通知所有等待线程池主锁Condition的线程
        tryTerminate();
    }

    // 修改或替换用于执行可运行的任务。 此方法可用于覆盖用于管理内部任务的具体类。 默认实现只是返回给定的任务。
    /**
     * Modifies or replaces the task used to execute a runnable.
     * This method can be used to override the concrete
     * class used for managing internal tasks.
     * The default implementation simply returns the given task.
     *
     * // 提交的 Runnable
     * @param runnable the submitted Runnable
     *
     * // 为执行可运行而创建的任务
     * @param task the task created to execute the runnable
     *
     * // 任务结果的类型
     * @param <V> the type of the task's result
     *
     * // 一个可以执行runnable的任务
     * @return a task that can execute the runnable
     *
     * @since 1.6
     */
    // 修改或替换用于执行可运行的任务, 默认实现只是返回给定的任务
    protected <V> RunnableScheduledFuture<V> decorateTask(Runnable runnable, RunnableScheduledFuture<V> task) {
        return task;
    }

    /**
     * 20210816
     * 修改或替换用于执行可调用的任务。 此方法可用于覆盖用于管理内部任务的具体类。 默认实现只是返回给定的任务。
     */
    /**
     * Modifies or replaces the task used to execute a callable.
     * This method can be used to override the concrete
     * class used for managing internal tasks.
     * The default implementation simply returns the given task.
     *
     * @param callable the submitted Callable
     * @param task the task created to execute the callable
     * @param <V> the type of the task's result
     * @return a task that can execute the callable
     * @since 1.6
     */
    // 修改或替换用于执行可运行的任务, 默认实现只是返回给定的任务
    protected <V> RunnableScheduledFuture<V> decorateTask(Callable<V> callable, RunnableScheduledFuture<V> task) {
        return task;
    }

    // 使用给定的核心池大小创建一个新的 {@code ScheduledThreadPoolExecutor}。
    /**
     * Creates a new {@code ScheduledThreadPoolExecutor} with the
     * given core pool size.
     *
     * @param corePoolSize the number of threads to keep in the pool, even
     *        if they are idle, unless {@code allowCoreThreadTimeOut} is set
     * @throws IllegalArgumentException if {@code corePoolSize < 0}
     */
    // 使用指定核心线程数、默认线程工厂、默认拒绝策略以及专门的延迟队列, 构造无界最大线程数、无界任务队列的ScheduledThreadPoolExecutor
    public ScheduledThreadPoolExecutor(int corePoolSize) {
        super(corePoolSize, Integer.MAX_VALUE, 0, NANOSECONDS, new DelayedWorkQueue());
    }

    /**
     * Creates a new {@code ScheduledThreadPoolExecutor} with the
     * given initial parameters.
     *
     * @param corePoolSize the number of threads to keep in the pool, even
     *        if they are idle, unless {@code allowCoreThreadTimeOut} is set
     * @param threadFactory the factory to use when the executor
     *        creates a new thread
     * @throws IllegalArgumentException if {@code corePoolSize < 0}
     * @throws NullPointerException if {@code threadFactory} is null
     */
    // 使用指定核心线程数、指定线程工厂、默认拒绝策略以及专门的延迟队列, 构造无界最大线程数、无界任务队列的ScheduledThreadPoolExecutor
    public ScheduledThreadPoolExecutor(int corePoolSize, ThreadFactory threadFactory) {
        super(corePoolSize, Integer.MAX_VALUE, 0, NANOSECONDS, new DelayedWorkQueue(), threadFactory);
    }

    /**
     * Creates a new ScheduledThreadPoolExecutor with the given
     * initial parameters.
     *
     * @param corePoolSize the number of threads to keep in the pool, even
     *        if they are idle, unless {@code allowCoreThreadTimeOut} is set
     * @param handler the handler to use when execution is blocked
     *        because the thread bounds and queue capacities are reached
     * @throws IllegalArgumentException if {@code corePoolSize < 0}
     * @throws NullPointerException if {@code handler} is null
     */
    // 使用指定核心线程数、默认线程工厂、指定拒绝策略以及专门的延迟队列, 构造无界最大线程数、无界任务队列的ScheduledThreadPoolExecutor
    public ScheduledThreadPoolExecutor(int corePoolSize, RejectedExecutionHandler handler) {
        super(corePoolSize, Integer.MAX_VALUE, 0, NANOSECONDS, new DelayedWorkQueue(), handler);
    }

    /**
     * Creates a new ScheduledThreadPoolExecutor with the given
     * initial parameters.
     *
     * @param corePoolSize the number of threads to keep in the pool, even
     *        if they are idle, unless {@code allowCoreThreadTimeOut} is set
     * @param threadFactory the factory to use when the executor
     *        creates a new thread
     * @param handler the handler to use when execution is blocked
     *        because the thread bounds and queue capacities are reached
     * @throws IllegalArgumentException if {@code corePoolSize < 0}
     * @throws NullPointerException if {@code threadFactory} or
     *         {@code handler} is null
     */
    // 使用指定核心线程数、指定线程工厂、指定拒绝策略以及专门的延迟队列, 构造无界最大线程数、无界任务队列的ScheduledThreadPoolExecutor
    public ScheduledThreadPoolExecutor(int corePoolSize, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
        super(corePoolSize, Integer.MAX_VALUE, 0, NANOSECONDS, new DelayedWorkQueue(), threadFactory, handler);
    }

    // 返回延迟动作的触发时间。
    /**
     * Returns the trigger time of a delayed action.
     */
    // 返回延迟动作的触发时间
    private long triggerTime(long delay, TimeUnit unit) {
        return triggerTime(unit.toNanos((delay < 0) ? 0 : delay));
    }

    // 返回延迟动作的触发时间。
    /**
     * Returns the trigger time of a delayed action.
     */
    // 返回延迟动作的触发时间
    long triggerTime(long delay) {
        return now() + ((delay < (Long.MAX_VALUE >> 1)) ? delay : overflowFree(delay));
    }

    // 将队列中所有延迟的值限制在彼此的 Long.MAX_VALUE 之内，以避免在 compareTo 中溢出。 如果某个任务有资格出队但尚未出队，则可能会发生这种情况，而其他一些任务已添加，延迟为 Long.MAX_VALUE。
    /**
     * Constrains the values of all delays in the queue to be within
     * Long.MAX_VALUE of each other, to avoid overflow in compareTo.
     * This may occur if a task is eligible to be dequeued, but has
     * not yet been, while some other task is added with a delay of
     * Long.MAX_VALUE.
     */
    // 将队列中所有延迟的值限制在彼此的 Long.MAX_VALUE 之内，以避免在 compareTo 中溢出。如果某个任务有资格出队但尚未出队，则可能会发生这种情况，而其他一些任务已添加，延迟为 Long.MAX_VALUE
    private long overflowFree(long delay) {
        Delayed head = (Delayed) super.getQueue().peek();
        if (head != null) {
            long headDelay = head.getDelay(NANOSECONDS);
            if (headDelay < 0 && (delay - headDelay < 0))
                delay = Long.MAX_VALUE + headDelay;
        }
        return delay;
    }

    /**
     * @throws RejectedExecutionException {@inheritDoc}
     * @throws NullPointerException       {@inheritDoc}
     */
    // 延迟delay时间执行一次command任务
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        if (command == null || unit == null)
            throw new NullPointerException();

        // 修改或替换用于执行可运行的任务, 默认实现只是返回ScheduledFutureTask, 没设置周期
        RunnableScheduledFuture<?> t = decorateTask(command, new ScheduledFutureTask<Void>(command, null, triggerTime(delay, unit)));

        // 执行延迟或者周期任务, 在任务队列追加任务后, 需要再次检查线程池以及任务状态, 如果任务还需要继续运行, 则要确保至少还有一个核心线程运; 如果任务不需要继续运行, 则从任务队列中移除并中断任务线程
        delayedExecute(t);
        return t;
    }

    /**
     * @throws RejectedExecutionException {@inheritDoc}
     * @throws NullPointerException       {@inheritDoc}
     */
    // 延迟delay时间执行一次callable任务
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        if (callable == null || unit == null)
            throw new NullPointerException();

        // 修改或替换用于执行可运行的任务, 默认实现只是返回ScheduledFutureTask, 没设置周期
        RunnableScheduledFuture<V> t = decorateTask(callable, new ScheduledFutureTask<V>(callable, triggerTime(delay, unit)));

        // 执行延迟或者周期任务, 在任务队列追加任务后, 需要再次检查线程池以及任务状态, 如果任务还需要继续运行, 则要确保至少还有一个核心线程运; 如果任务不需要继续运行, 则从任务队列中移除并中断任务线程
        delayedExecute(t);
        return t;
    }

    /**
     * @throws RejectedExecutionException {@inheritDoc}
     * @throws NullPointerException       {@inheritDoc}
     * @throws IllegalArgumentException   {@inheritDoc}
     */
    // (相对周期)创建并执行一个周期性动作, 任务会在initialDelay后执行, 之后则以period为周期执行, 如果遇到异常, 则会终止后续执行; 如果执行时间超过period, 后续执行则会延后而不是并发执行
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        if (command == null || unit == null)
            throw new NullPointerException();

        // 周期必须大于0
        if (period <= 0)
            throw new IllegalArgumentException();

        // 修改或替换用于执行可运行的任务, 默认实现只是返回ScheduledFutureTask, period作为运行周期(相对周期)
        ScheduledFutureTask<Void> sft = new ScheduledFutureTask<Void>(command, null, triggerTime(initialDelay, unit), unit.toNanos(period));
        RunnableScheduledFuture<Void> t = decorateTask(command, sft);
        sft.outerTask = t;

        // 执行延迟或者周期任务, 在任务队列追加任务后, 需要再次检查线程池以及任务状态, 如果任务还需要继续运行, 则要确保至少还有一个核心线程运; 如果任务不需要继续运行, 则从任务队列中移除并中断任务线程
        delayedExecute(t);
        return t;
    }

    /**
     * @throws RejectedExecutionException {@inheritDoc}
     * @throws NullPointerException       {@inheritDoc}
     * @throws IllegalArgumentException   {@inheritDoc}
     */
    // (绝对周期)创建并执行一个周期性动作, 任务会在initialDelay后执行, 任务执行结束之后则延迟delay后再次执行, 任务执行慢时可能会有并发执行
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command,
                                                     long initialDelay,
                                                     long delay,
                                                     TimeUnit unit) {
        if (command == null || unit == null)
            throw new NullPointerException();

        // 延迟必须大于0
        if (delay <= 0)
            throw new IllegalArgumentException();

        // 修改或替换用于执行可运行的任务, 默认实现只是返回ScheduledFutureTask, -delay作为运行周期(绝对周期)
        ScheduledFutureTask<Void> sft = new ScheduledFutureTask<Void>(command, null, triggerTime(initialDelay, unit), unit.toNanos(-delay));
        RunnableScheduledFuture<Void> t = decorateTask(command, sft);
        sft.outerTask = t;

        // 执行延迟或者周期任务, 在任务队列追加任务后, 需要再次检查线程池以及任务状态, 如果任务还需要继续运行, 则要确保至少还有一个核心线程运; 如果任务不需要继续运行, 则从任务队列中移除并中断任务线程
        delayedExecute(t);
        return t;
    }

    /**
     * 20210816
     * A. 以零所需延迟执行 {@code command}。 这具有等效于 {@link #schedule(Runnable,long,TimeUnit) schedule(command, 0, anyUnit)} 的效果。
     *    请注意，对 {@code shutdownNow} 返回的队列和列表的检查将访问零延迟的 {@link ScheduledFuture}，而不是 {@code 命令}本身。
     * B. 使用 {@code ScheduledFuture} 对象的结果是 {@link ThreadPoolExecutor#afterExecute afterExecute} 总是使用空的第二个 {@code Throwable} 参数调用，
     *    即使 {@code command} 突然终止。 相反，此类任务抛出的 {@code Throwable} 可以通过 {@link Future#get} 获取。
     */
    /**
     * A.
     * Executes {@code command} with zero required delay.
     * This has effect equivalent to
     * {@link #schedule(Runnable,long,TimeUnit) schedule(command, 0, anyUnit)}.
     * Note that inspections of the queue and of the list returned by
     * {@code shutdownNow} will access the zero-delayed
     * {@link ScheduledFuture}, not the {@code command} itself.
     *
     * B.
     * <p>A consequence of the use of {@code ScheduledFuture} objects is
     * that {@link ThreadPoolExecutor#afterExecute afterExecute} is always
     * called with a null second {@code Throwable} argument, even if the
     * {@code command} terminated abruptly.  Instead, the {@code Throwable}
     * thrown by such a task can be obtained via {@link Future#get}.
     *
     * // RejectedExecutionException 由 {@code RejectedExecutionHandler} 自行决定，如果由于执行程序已关闭而无法接受任务执行
     * @throws RejectedExecutionException at discretion of
     *         {@code RejectedExecutionHandler}, if the task
     *         cannot be accepted for execution because the
     *         executor has been shut down
     * @throws NullPointerException {@inheritDoc}
     */
    // 延迟0时间执行一次command任务
    public void execute(Runnable command) {
        schedule(command, 0, NANOSECONDS);
    }

    // 覆盖 AbstractExecutorService 方法
    // Override AbstractExecutorService methods

    /**
     * @throws RejectedExecutionException {@inheritDoc}
     * @throws NullPointerException       {@inheritDoc}
     */
    // 延迟0时间执行一次command任务
    public Future<?> submit(Runnable task) {
        return schedule(task, 0, NANOSECONDS);
    }

    /**
     * @throws RejectedExecutionException {@inheritDoc}
     * @throws NullPointerException       {@inheritDoc}
     */
    // 延迟0时间执行一次command任务
    public <T> Future<T> submit(Runnable task, T result) {
        return schedule(Executors.callable(task, result), 0, NANOSECONDS);
    }

    /**
     * @throws RejectedExecutionException {@inheritDoc}
     * @throws NullPointerException       {@inheritDoc}
     */
    // 延迟0时间执行一次command任务
    public <T> Future<T> submit(Callable<T> task) {
        return schedule(task, 0, NANOSECONDS);
    }

    /**
     * 20210816
     * 设置是否在此执行器已{@code shutdown} 时继续执行现有周期性任务的策略。
     * 在这种情况下，这些任务只会在 {@code shutdownNow} 或在已关闭时将策略设置为 {@code false} 后终止。 此值默认为 {@code false}。
     */
    /**
     * Sets the policy on whether to continue executing existing
     * periodic tasks even when this executor has been {@code shutdown}.
     * In this case, these tasks will only terminate upon
     * {@code shutdownNow} or after setting the policy to
     * {@code false} when already shutdown.
     * This value is by default {@code false}.
     *
     * @param value if {@code true}, continue after shutdown, else don't
     * @see #getContinueExistingPeriodicTasksAfterShutdownPolicy
     */
    // 设置是否应该在关闭时取消定期任务, 如果需要取消则为false, 如果不需要取消则为true
    public void setContinueExistingPeriodicTasksAfterShutdownPolicy(boolean value) {
        continueExistingPeriodicTasksAfterShutdown = value;
        if (!value && isShutdown())
            onShutdown();
    }

    /**
     * 20210816
     * 获取有关是否继续执行现有周期性任务的策略，即使此执行程序已 {@code shutdown}。
     * 在这种情况下，这些任务只会在 {@code shutdownNow} 或在已关闭时将策略设置为 {@code false} 后终止。 此值默认为 {@code false}。
     */
    /**
     * Gets the policy on whether to continue executing existing
     * periodic tasks even when this executor has been {@code shutdown}.
     * In this case, these tasks will only terminate upon
     * {@code shutdownNow} or after setting the policy to
     * {@code false} when already shutdown.
     * This value is by default {@code false}.
     *
     * @return {@code true} if will continue after shutdown
     * @see #setContinueExistingPeriodicTasksAfterShutdownPolicy
     */
    // 判断是否应该在关闭时取消定期任务, 如果需要取消则为false, 如果不需要取消则为true
    public boolean getContinueExistingPeriodicTasksAfterShutdownPolicy() {
        return continueExistingPeriodicTasksAfterShutdown;
    }

    /**
     * 20210816
     * 设置是否在此执行器已 {@code shutdown} 时执行现有延迟任务的策略。
     * 在这种情况下，这些任务只会在 {@code shutdownNow} 时终止，或者在已关闭时将策略设置为 {@code false} 后终止。 此值默认为 {@code true}。
     */
    /**
     * Sets the policy on whether to execute existing delayed
     * tasks even when this executor has been {@code shutdown}.
     * In this case, these tasks will only terminate upon
     * {@code shutdownNow}, or after setting the policy to
     * {@code false} when already shutdown.
     * This value is by default {@code true}.
     *
     * @param value if {@code true}, execute after shutdown, else don't
     * @see #getExecuteExistingDelayedTasksAfterShutdownPolicy
     */
    // 设置是否应该在关闭时取消非周期性任务, 如果需要取消则为false, 如果不需要取消则为true
    public void setExecuteExistingDelayedTasksAfterShutdownPolicy(boolean value) {
        executeExistingDelayedTasksAfterShutdown = value;
        if (!value && isShutdown())
            onShutdown();
    }

    /**
     * 20210816
     * 获取有关是否在此执行程序已 {@code shutdown} 时执行现有延迟任务的策略。 在这种情况下，这些任务只会在 {@code shutdownNow} 时终止，
     * 或者在已关闭时将策略设置为 {@code false} 后终止。 此值默认为 {@code true}。
     *
     */
    /**
     * Gets the policy on whether to execute existing delayed
     * tasks even when this executor has been {@code shutdown}.
     * In this case, these tasks will only terminate upon
     * {@code shutdownNow}, or after setting the policy to
     * {@code false} when already shutdown.
     * This value is by default {@code true}.
     *
     * @return {@code true} if will execute after shutdown
     * @see #setExecuteExistingDelayedTasksAfterShutdownPolicy
     */
    // 判断是否应该在关闭时取消非周期性任务, 如果需要取消则为false, 如果不需要取消则为true
    public boolean getExecuteExistingDelayedTasksAfterShutdownPolicy() {
        return executeExistingDelayedTasksAfterShutdown;
    }

    // 设置有关在取消时是否应立即从工作队列中删除已取消任务的策略。 此值默认为 {@code false}。
    /**
     * Sets the policy on whether cancelled tasks should be immediately
     * removed from the work queue at time of cancellation.  This value is
     * by default {@code false}.
     *
     * @param value if {@code true}, remove on cancellation, else don't
     * @see #getRemoveOnCancelPolicy
     * @since 1.7
     */
    // 设置ScheduledFutureTask取消后是否应该从任务队列中删除, 如果需要则为true, 如果不需要则为false
    public void setRemoveOnCancelPolicy(boolean value) {
        removeOnCancel = value;
    }

    // 获取有关在取消时是否应立即从工作队列中删除已取消任务的策略。 此值默认为 {@code false}。
    /**
     * Gets the policy on whether cancelled tasks should be immediately
     * removed from the work queue at time of cancellation.  This value is
     * by default {@code false}.
     *
     * @return {@code true} if cancelled tasks are immediately removed
     *         from the queue
     * @see #setRemoveOnCancelPolicy
     * @since 1.7
     */
    // 判断ScheduledFutureTask取消后是否应该从任务队列中删除, 如果需要则为true, 如果不需要则为false
    public boolean getRemoveOnCancelPolicy() {
        return removeOnCancel;
    }

    /**
     * 20210816
     * A. 启动有序关闭，其中执行先前提交的任务，但不会接受新任务。 如果已经关闭，调用没有额外的效果。
     * B. 此方法不等待先前提交的任务完成执行。 使用 {@link #awaitTermination awaitTermination} 来做到这一点。
     * C. 如果 {@code ExecuteExistingDelayedTasksAfterShutdownPolicy} 已设置为 {@code false}，则延迟尚未结束的现有延迟任务将被取消。
     *    除非 {@code ContinueExistingPeriodicTasksAfterShutdownPolicy} 已设置为 {@code true}，否则将取消现有周期性任务的未来执行。
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
     * C.
     * <p>If the {@code ExecuteExistingDelayedTasksAfterShutdownPolicy}
     * has been set {@code false}, existing delayed tasks whose delays
     * have not yet elapsed are cancelled.  And unless the {@code
     * ContinueExistingPeriodicTasksAfterShutdownPolicy} has been set
     * {@code true}, future executions of existing periodic tasks will
     * be cancelled.
     *
     * @throws SecurityException {@inheritDoc}
     */
    public void shutdown() {
        super.shutdown();
    }

    /**
     * 20210816
     * A. 尝试停止所有正在执行的任务，停止等待任务的处理，并返回等待执行的任务列表。
     * B. 此方法不会等待主动执行的任务终止。 使用 {@link #awaitTermination awaitTermination} 来做到这一点。
     * C. 除了尽力尝试停止处理正在执行的任务之外，没有任何保证。 此实现通过 {@link Thread#interrupt} 取消任务，因此任何未能响应中断的任务可能永远不会终止。
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
     * processing actively executing tasks.  This implementation
     * cancels tasks via {@link Thread#interrupt}, so any task that
     * fails to respond to interrupts may never terminate.
     *
     * @return list of tasks that never commenced execution.
     *         Each element of this list is a {@link ScheduledFuture},
     *         including those tasks submitted using {@code execute},
     *         which are for scheduling purposes used as the basis of a
     *         zero-delay {@code ScheduledFuture}.
     * @throws SecurityException {@inheritDoc}
     */
    public List<Runnable> shutdownNow() {
        return super.shutdownNow();
    }

    /**
     * 20210816
     * 返回此执行程序使用的任务队列。 该队列的每个元素都是一个 {@link ScheduledFuture}，包括使用 {@code execute} 提交的那些任务，这些任务用于调度目的，
     * 用作零延迟 {@code ScheduledFuture} 的基础。 不能保证对这个队列的迭代按任务执行的顺序遍历任务。
     */
    /**
     * Returns the task queue used by this executor.  Each element of
     * this queue is a {@link ScheduledFuture}, including those
     * tasks submitted using {@code execute} which are for scheduling
     * purposes used as the basis of a zero-delay
     * {@code ScheduledFuture}.  Iteration over this queue is
     * <em>not</em> guaranteed to traverse tasks in the order in
     * which they will execute.
     *
     * @return the task queue
     */
    public BlockingQueue<Runnable> getQueue() {
        return super.getQueue();
    }

    /**
     * 20210523
     * 专门的延迟队列。 为了与TPE声明进行网格划分，即使该类只能容纳RunnableScheduledFutures，也必须将其声明为BlockingQueue <Runnable>。
     */
    /**
     * Specialized delay queue. To mesh with TPE declarations, this
     * class must be declared as a BlockingQueue<Runnable> even though
     * it can only hold RunnableScheduledFutures.
     */
    static class DelayedWorkQueue extends AbstractQueue<Runnable> implements BlockingQueue<Runnable> {

        /**
         * 20210523
         * A. 除了每个ScheduledFutureTask还将其索引记录到堆数组中之外，DelayedWorkQueue都基于基于堆的数据结构，如DelayQueue和PriorityQueue中的数据结构。
         *    这样就消除了在取消时查找任务的需要，极大地加快了清除速度（从O（n）降到O（log n）），并减少了垃圾保留，否则将通过等待元素在清除之前上升到顶部而发生垃圾保留。
         *    但是由于队列中还可能包含不是ScheduledFutureTasks的RunnableScheduledFuture，所以我们不能保证有这样的索引可用，在这种情况下，我们将退回到线性搜索。
         *    （我们希望大多数任务不会被修饰，并且更快的案例会更加常见。）
         * B. 所有堆操作必须记录索引更改-主要在siftUp和siftDown中。 删除后，任务的heapIndex设置为-1。 请注意，ScheduledFutureTasks最多可以在队列中出现一次
         *    （对于其他类型的任务或工作队列，则不必为true），因此由heapIndex唯一标识。
         */
        /*
         * A.
         * A DelayedWorkQueue is based on a heap-based data structure
         * like those in DelayQueue and PriorityQueue, except that
         * every ScheduledFutureTask also records its index into the
         * heap array. This eliminates the need to find a task upon
         * cancellation, greatly speeding up removal (down from O(n)
         * to O(log n)), and reducing garbage retention that would
         * otherwise occur by waiting for the element to rise to top
         * before clearing. But because the queue may also hold
         * RunnableScheduledFutures that are not ScheduledFutureTasks,
         * we are not guaranteed to have such indices available, in
         * which case we fall back to linear search. (We expect that
         * most tasks will not be decorated, and that the faster cases
         * will be much more common.)
         *
         * B.
         * All heap operations must record index changes -- mainly
         * within siftUp and siftDown. Upon removal, a task's
         * heapIndex is set to -1. Note that ScheduledFutureTasks can
         * appear at most once in the queue (this need not be true for
         * other kinds of tasks or work queues), so are uniquely
         * identified by heapIndex.
         */

        private static final int INITIAL_CAPACITY = 16;
        private RunnableScheduledFuture<?>[] queue = new RunnableScheduledFuture<?>[INITIAL_CAPACITY];
        private final ReentrantLock lock = new ReentrantLock();
        private int size = 0;

        /**
         * 20210523
         * 指定用于在队列开头等待任务的线程。 Leader-Follower模式的这种变体（http://www.cs.wustl.edu/~schmidt/POSA/POSA2/）用于最大程度地减少不必要的定时等待。
         * 当某个线程成为领导者时，它仅等待下一个延迟过去，但是其他线程将无限期地等待。 引导线程必须在从take（）或poll（...）返回之前向其他线程发出信号，
         * 除非其他线程成为过渡期间的引导者。 每当队列的开头被具有更早到期时间的任务替换时，领导字段将通过重置为null来无效，
         * 并且会发出一些等待线程（但不一定是当前领导）的信号。 因此，等待线程必须准备好在等待时获得并失去领导能力。
         */
        /**
         * Thread designated to wait for the task at the head of the
         * queue.  This variant of the Leader-Follower pattern
         * (http://www.cs.wustl.edu/~schmidt/POSA/POSA2/) serves to
         * minimize unnecessary timed waiting.  When a thread becomes
         * the leader, it waits only for the next delay to elapse, but
         * other threads await indefinitely.  The leader thread must
         * signal some other thread before returning from take() or
         * poll(...), unless some other thread becomes leader in the
         * interim.  Whenever the head of the queue is replaced with a
         * task with an earlier expiration time, the leader field is
         * invalidated by being reset to null, and some waiting
         * thread, but not necessarily the current leader, is
         * signalled.  So waiting threads must be prepared to acquire
         * and lose leadership while waiting.
         */
        private Thread leader = null;

        /**
         * 20210523
         * 当更新的任务在队列的开头可用时，或者在新的线程可能需要成为领导者时，会发出条件信号。
         */
        /**
         * Condition signalled when a newer task becomes available at the
         * head of the queue or a new thread may need to become leader.
         */
        private final Condition available = lock.newCondition();

        /**
         * 20210523
         * 如果f是ScheduledFutureTask，则设置f的heapIndex。
         */
        /**
         * Sets f's heapIndex if it is a ScheduledFutureTask.
         */
        private void setIndex(RunnableScheduledFuture<?> f, int idx) {
            if (f instanceof ScheduledFutureTask)
                ((ScheduledFutureTask)f).heapIndex = idx;
        }

        /**
         * 20210523
         * 筛选元素从底部开始添加到按堆排序的位置。 仅在保持锁定状态时呼叫。
         */
        /**
         * Sifts element added at bottom up to its heap-ordered spot.
         * Call only when holding lock.
         */
        private void siftUp(int k, RunnableScheduledFuture<?> key) {
            while (k > 0) {
                int parent = (k - 1) >>> 1;// (n-1)/2 => 父结点
                RunnableScheduledFuture<?> e = queue[parent];
                // 如果当前元素的值大于父结点, 则可以直接跳出循环
                if (key.compareTo(e) >= 0)
                    break;

                // 否则说明当前元素的值小于父结点, 则父结点插入当前位置, 当前位置走上父结点位置, 继续循环比较下一个父结点
                queue[k] = e;
                setIndex(e, k);
                k = parent;
            }
            // 插入当前元素, 经过了上面的调整, 该位置一定是符合小顶堆的位置(比上大时)
            queue[k] = key;
            setIndex(key, k);
        }

        // 将自上而下添加到其堆排序位置的筛选元素。 仅在持有锁定时调用。
        /**
         * Sifts element added at top down to its heap-ordered spot.
         * Call only when holding lock.
         */
        private void siftDown(int k, RunnableScheduledFuture<?> key) {
            int half = size >>> 1;
            while (k < half) {
                int child = (k << 1) + 1;// 左孩子: 2k + 1
                RunnableScheduledFuture<?> c = queue[child];
                int right = child + 1;// 右孩子: 2k + 2
                // 如果存在右孩子, 且左孩子比右孩子大, 则取右孩子(较小的一个)设置为待交换结点
                if (right < size && c.compareTo(queue[right]) > 0)
                    c = queue[child = right];
                // 如果当前元素的值比待交换结点的值还小, 说明当前元素的值已经是最小的了, 适合插入, 则退出循环
                if (key.compareTo(c) <= 0)
                    break;
                // 否则说明, 当前元素的值比左(右)孩子的大, 则带交换结点插入当前位置, 当前位置走下到待交换结点的位置, 继续循环比较新的左右孩子
                queue[k] = c;
                setIndex(c, k);
                k = child;
            }
            // 插入当前元素, 经过了上面的调整, 该位置一定是符合小顶堆的位置(比下小时)
            queue[k] = key;
            setIndex(key, k);
        }

        /**
         * 20210523
         * 调整堆数组的大小。 仅在保持锁定状态时呼叫。
         */
        /**
         * Resizes the heap array.  Call only when holding lock.
         */
        private void grow() {
            int oldCapacity = queue.length;
            int newCapacity = oldCapacity + (oldCapacity >> 1); // grow 50%
            if (newCapacity < 0) // overflow
                newCapacity = Integer.MAX_VALUE;
            queue = Arrays.copyOf(queue, newCapacity);
        }

        /**
         * Finds index of given object, or -1 if absent.
         */
        // 查找给定对象的索引，如果不存在，则为 -1。
        private int indexOf(Object x) {
            if (x != null) {
                if (x instanceof ScheduledFutureTask) {
                    int i = ((ScheduledFutureTask) x).heapIndex;
                    // Sanity check; x could conceivably be a
                    // ScheduledFutureTask from some other pool.
                    if (i >= 0 && i < size && queue[i] == x)
                        return i;
                } else {
                    for (int i = 0; i < size; i++)
                        if (x.equals(queue[i]))
                            return i;
                }
            }
            return -1;
        }

        public boolean contains(Object x) {
            final ReentrantLock lock = this.lock;
            lock.lock();
            try {
                return indexOf(x) != -1;
            } finally {
                lock.unlock();
            }
        }

        public boolean remove(Object x) {
            final ReentrantLock lock = this.lock;
            lock.lock();
            try {
                int i = indexOf(x);
                if (i < 0)
                    return false;

                setIndex(queue[i], -1);
                int s = --size;
                RunnableScheduledFuture<?> replacement = queue[s];
                queue[s] = null;
                if (s != i) {
                    siftDown(i, replacement);
                    if (queue[i] == replacement)
                        siftUp(i, replacement);
                }
                return true;
            } finally {
                lock.unlock();
            }
        }

        public int size() {
            final ReentrantLock lock = this.lock;
            lock.lock();
            try {
                return size;
            } finally {
                lock.unlock();
            }
        }

        public boolean isEmpty() {
            return size() == 0;
        }

        public int remainingCapacity() {
            return Integer.MAX_VALUE;
        }

        public RunnableScheduledFuture<?> peek() {
            final ReentrantLock lock = this.lock;
            lock.lock();
            try {
                return queue[0];
            } finally {
                lock.unlock();
            }
        }

        public boolean offer(Runnable x) {
            if (x == null)
                throw new NullPointerException();
            RunnableScheduledFuture<?> e = (RunnableScheduledFuture<?>)x;
            final ReentrantLock lock = this.lock;
            lock.lock();
            try {
                int i = size;
                if (i >= queue.length)
                    grow();
                size = i + 1;
                if (i == 0) {
                    queue[0] = e;
                    setIndex(e, 0);
                } else {
                    siftUp(i, e);
                }
                if (queue[0] == e) {
                    leader = null;
                    available.signal();
                }
            } finally {
                lock.unlock();
            }
            return true;
        }

        public void put(Runnable e) {
            offer(e);
        }

        public boolean add(Runnable e) {
            return offer(e);
        }

        public boolean offer(Runnable e, long timeout, TimeUnit unit) {
            return offer(e);
        }

        /**
         * Performs common bookkeeping for poll and take: Replaces
         * first element with last and sifts it down.  Call only when
         * holding lock.
         * @param f the task to remove and return
         */
        // 获取最小的RunnableScheduledFuture
        private RunnableScheduledFuture<?> finishPoll(RunnableScheduledFuture<?> f) {
            int s = --size;
            RunnableScheduledFuture<?> x = queue[s];
            queue[s] = null;
            if (s != 0)
                siftDown(0, x);
            setIndex(f, -1);
            return f;
        }

        // 从任务队列获取RunnableScheduledFuture
        public RunnableScheduledFuture<?> poll() {
            final ReentrantLock lock = this.lock;
            lock.lock();
            try {
                // 如果没有到期的RunnableScheduledFuture, 则返回null
                RunnableScheduledFuture<?> first = queue[0];
                if (first == null || first.getDelay(NANOSECONDS) > 0)
                    return null;
                else
                    // 获取最小的RunnableScheduledFuture
                    return finishPoll(first);
            } finally {
                lock.unlock();
            }
        }

        public RunnableScheduledFuture<?> take() throws InterruptedException {
            final ReentrantLock lock = this.lock;
            lock.lockInterruptibly();
            try {
                for (;;) {
                    RunnableScheduledFuture<?> first = queue[0];
                    if (first == null)
                        available.await();
                    else {
                        long delay = first.getDelay(NANOSECONDS);
                        if (delay <= 0)
                            return finishPoll(first);
                        first = null; // don't retain ref while waiting
                        if (leader != null)
                            available.await();
                        else {
                            Thread thisThread = Thread.currentThread();
                            leader = thisThread;
                            try {
                                available.awaitNanos(delay);
                            } finally {
                                if (leader == thisThread)
                                    leader = null;
                            }
                        }
                    }
                }
            } finally {
                if (leader == null && queue[0] != null)
                    available.signal();
                lock.unlock();
            }
        }

        public RunnableScheduledFuture<?> poll(long timeout, TimeUnit unit)
            throws InterruptedException {
            long nanos = unit.toNanos(timeout);
            final ReentrantLock lock = this.lock;
            lock.lockInterruptibly();
            try {
                for (;;) {
                    RunnableScheduledFuture<?> first = queue[0];
                    if (first == null) {
                        if (nanos <= 0)
                            return null;
                        else
                            nanos = available.awaitNanos(nanos);
                    } else {
                        long delay = first.getDelay(NANOSECONDS);
                        if (delay <= 0)
                            return finishPoll(first);
                        if (nanos <= 0)
                            return null;
                        first = null; // don't retain ref while waiting
                        if (nanos < delay || leader != null)
                            nanos = available.awaitNanos(nanos);
                        else {
                            Thread thisThread = Thread.currentThread();
                            leader = thisThread;
                            try {
                                long timeLeft = available.awaitNanos(delay);
                                nanos -= delay - timeLeft;
                            } finally {
                                if (leader == thisThread)
                                    leader = null;
                            }
                        }
                    }
                }
            } finally {
                if (leader == null && queue[0] != null)
                    available.signal();
                lock.unlock();
            }
        }

        public void clear() {
            final ReentrantLock lock = this.lock;
            lock.lock();
            try {
                for (int i = 0; i < size; i++) {
                    RunnableScheduledFuture<?> t = queue[i];
                    if (t != null) {
                        queue[i] = null;
                        setIndex(t, -1);
                    }
                }
                size = 0;
            } finally {
                lock.unlock();
            }
        }

        /**
         * Returns first element only if it is expired.
         * Used only by drainTo.  Call only when holding lock.
         */
        private RunnableScheduledFuture<?> peekExpired() {
            // assert lock.isHeldByCurrentThread();
            RunnableScheduledFuture<?> first = queue[0];
            return (first == null || first.getDelay(NANOSECONDS) > 0) ?
                null : first;
        }

        public int drainTo(Collection<? super Runnable> c) {
            if (c == null)
                throw new NullPointerException();
            if (c == this)
                throw new IllegalArgumentException();
            final ReentrantLock lock = this.lock;
            lock.lock();
            try {
                RunnableScheduledFuture<?> first;
                int n = 0;
                while ((first = peekExpired()) != null) {
                    c.add(first);   // In this order, in case add() throws.
                    finishPoll(first);
                    ++n;
                }
                return n;
            } finally {
                lock.unlock();
            }
        }

        public int drainTo(Collection<? super Runnable> c, int maxElements) {
            if (c == null)
                throw new NullPointerException();
            if (c == this)
                throw new IllegalArgumentException();
            if (maxElements <= 0)
                return 0;
            final ReentrantLock lock = this.lock;
            lock.lock();
            try {
                RunnableScheduledFuture<?> first;
                int n = 0;
                while (n < maxElements && (first = peekExpired()) != null) {
                    c.add(first);   // In this order, in case add() throws.
                    finishPoll(first);
                    ++n;
                }
                return n;
            } finally {
                lock.unlock();
            }
        }

        public Object[] toArray() {
            final ReentrantLock lock = this.lock;
            lock.lock();
            try {
                return Arrays.copyOf(queue, size, Object[].class);
            } finally {
                lock.unlock();
            }
        }

        @SuppressWarnings("unchecked")
        public <T> T[] toArray(T[] a) {
            final ReentrantLock lock = this.lock;
            lock.lock();
            try {
                if (a.length < size)
                    return (T[]) Arrays.copyOf(queue, size, a.getClass());
                System.arraycopy(queue, 0, a, 0, size);
                if (a.length > size)
                    a[size] = null;
                return a;
            } finally {
                lock.unlock();
            }
        }

        public Iterator<Runnable> iterator() {
            return new Itr(Arrays.copyOf(queue, size));
        }

        /**
         * 20210523
         * 快照迭代器，可处理基础q数组的副本。
         */
        /**
         * Snapshot iterator that works off copy of underlying q array.
         */
        private class Itr implements Iterator<Runnable> {
            final RunnableScheduledFuture<?>[] array;
            int cursor = 0;     // index of next element to return
            int lastRet = -1;   // index of last element, or -1 if no such

            Itr(RunnableScheduledFuture<?>[] array) {
                this.array = array;
            }

            public boolean hasNext() {
                return cursor < array.length;
            }

            public Runnable next() {
                if (cursor >= array.length)
                    throw new NoSuchElementException();
                lastRet = cursor;
                return array[cursor++];
            }

            public void remove() {
                if (lastRet < 0)
                    throw new IllegalStateException();
                DelayedWorkQueue.this.remove(array[lastRet]);
                lastRet = -1;
            }
        }
    }
}
