/*
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util.concurrent;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.*;

/**
 * 20210813
 * A. {@link ExecutorService} 使用可能的多个池线程之一执行每个提交的任务，通常使用 {@link Executors} 工厂方法进行配置。
 * B. 线程池解决两个不同的问题：由于减少了每个任务的调用开销，它们通常在执行大量异步任务时提供改进的性能，并且它们提供了一种限制和管理资源的方法，
 *    包括在执行集合时消耗的线程任务。 每个 {@code ThreadPoolExecutor} 还维护一些基本的统计信息，例如已完成的任务数。
 * C. 为了在广泛的上下文中有用，此类提供了许多可调整的参数和可扩展性挂钩。但是，强烈建议程序员使用更方便的{@link Executors}工厂方法
 *    {@link Executors#newCachedThreadPool}（无界线程池，具有自动线程回收）、
 *    {@link Executors#newFixedThreadPool}（固定大小的线程池）和
 *    { @link Executors#newSingleThreadExecutor}（单后台线程），为最常见的使用场景预配置设置。 否则，在手动配置和调整此类时使用以下指南：
 *    a. 核心和最大池大小: {@code ThreadPoolExecutor} 将根据corePoolSize（请参阅 {@link #getCorePoolSize}）和 maximumPoolSize
 *      （请参阅 {@link #getMaximumPoolSize}）设置的边界自动调整池大小（请参阅 {@link #getPoolSize}）。当在方法 {@link #execute(Runnable)} 中提交新任务，
 *       并且运行的线程少于 corePoolSize 时，即使其他工作线程空闲，也会创建一个新线程来处理请求。如果有超过 corePoolSize 但小于 maximumPoolSize 的线程正在运行，
 *       则只有在队列已满时才会创建新线程。通过将 corePoolSize 和 maximumPoolSize 设置为相同，您可以创建一个固定大小的线程池。
 *       通过将maximumPoolSize 设置为基本上无界的值，例如{@code Integer.MAX_VALUE}，您可以允许池容纳任意数量的并发任务。最典型的是，核心和最大池大小仅在构造时设置，
 *       但它们也可以使用 {@link #setCorePoolSize} 和 {@link #setMaximumPoolSize} 动态更改。
 *    b. 按需构建: 默认情况下，即使是核心线程也仅在新任务到达时最初创建和启动，但这可以使用方法 {@link #prestartCoreThread} 或 {@link #prestartAllCoreThreads} 动态覆盖。
 *       如果您使用非空队列构造池，您可能想要预启动线程。
 *    c. 创建新线程: 使用 {@link ThreadFactory} 创建新线程。 如果没有另外指定，则使用 {@link Executors#defaultThreadFactory}，
 *       它创建的线程都在同一个 {@link ThreadGroup} 中，并且具有相同的 {@code NORM_PRIORITY} 优先级和非守护进程状态。 通过提供不同的 ThreadFactory，
 *       您可以更改线程的名称、线程组、优先级、守护进程状态等。如果 {@code ThreadFactory} 在通过从 {@code newThread} 返回 null 的询问时未能创建线程，
 *       执行程序将 继续，但可能无法执行任何任务。 线程应该拥有“modifyThread”{@code RuntimePermission}。 如果工作线程或其他使用池的线程不具备此权限，
 *       则服务可能会降级：配置更改可能无法及时生效，关闭池可能会一直处于可以终止但未完成的状态。
 *    d. 保活时间: 如果池中当前有超过 corePoolSize 的线程，则多余的线程如果空闲时间超过 keepAliveTime（请参阅 {@link #getKeepAliveTime(TimeUnit)}）将被终止。
 *       这提供了一种在未积极使用池时减少资源消耗的方法。如果池稍后变得更加活跃，则将构建新线程。也可以使用方法 {@link #setKeepAliveTime(long, TimeUnit)} 动态更改此参数。
 *       使用 {@code Long.MAX_VALUE} {@link TimeUnit#NANOOSECONDS} 值可以有效地禁止空闲线程在关闭之前终止。 默认情况下，仅当有超过 corePoolSize 的线程时，
 *       保持活动策略才适用。 但是方法 {@link #allowCoreThreadTimeOut(boolean)} 也可用于将此超时策略应用于核心线程，只要 keepAliveTime 值不为零。
 *    e. 排队: 任何 {@link BlockingQueue} 都可用于传输和保留提交的任务。 此队列的使用与池大小交互：
 *          1. 如果运行的线程数少于 corePoolSize，则 Executor 总是喜欢添加新线程而不是排队。
 *          2. 如果 corePoolSize 或更多线程正在运行，Executor 总是喜欢将请求排队而不是添加新线程。
 *          3. 如果请求无法排队，则会创建一个新线程，除非这会超过 maximumPoolSize，在这种情况下，任务将被拒绝。
 *       排队的一般策略有以下三种：
 *          1. 直接交接。工作队列的一个很好的默认选择是 {@link SynchronousQueue}，它将任务交给线程而不用其他方式保留它们。 在这里，如果没有线程可立即运行，
 *             则将任务排队的尝试将失败，因此将构建一个新线程。 在处理可能具有内部依赖性的请求集时，此策略可避免锁定。
 *             直接切换通常需要无限的maximumPoolSizes 以避免拒绝新提交的任务。 这反过来又承认了当命令平均持续到达速度快于它们可以处理的速度时无限线程增长的可能性。
 *          2. 无界队列。 使用无界队列（例如，没有预定义容量的 {@link LinkedBlockingQueue}）将导致新任务在所有 corePoolSize 线程都忙时在队列中等待。
 *             因此，不会创建超过 corePoolSize 的线程。 （因此maximumPoolSize的值没有任何影响。）当每个任务完全独立于其他任务时，这可能是合适的，
 *             因此任务不会影响彼此的执行；例如，在网页服务器中。 虽然这种排队方式在平滑请求的瞬时爆发方面很有用，但它承认当命令的平均到达速度超过它们的处理速度时，
 *             工作队列可能会无限增长。
 *          3. 有界队列。有界队列（例如，{@link ArrayBlockingQueue}）在与有限的 maximumPoolSizes 一起使用时有助于防止资源耗尽，但可能更难以调整和控制。
 *             队列大小和最大池大小可以相互权衡：使用大队列和小池可以最大限度地减少CPU使用率、操作系统资源和上下文切换开销，但会导致人为地降低吞吐量。
 *             如果任务频繁阻塞（例如，如果它们受 I/O 限制），则系统可能能够为比您允许的更多线程安排时间。 使用小队列通常需要更大的池大小，这会使 CPU 更忙，
 *             但可能会遇到不可接受的调度开销，这也会降低吞吐量。
 *     f. 被拒绝的任务: 当 Executor 已经关闭，并且当 Executor 对最大线程和工作队列容量使用有限边界并且饱和时，在方法 {@link #execute(Runnable)} 中提交的新任务将被拒绝。
 *        在任何一种情况下，{@code execute} 方法都会调用其 {@link RejectedExecutionHandler} 的
 *        {@link RejectedExecutionHandler#rejectedExecution(Runnable, ThreadPoolExecutor)} 方法。 提供了四个预定义的处理程序策略：
 *          1. 在默认的 {@link ThreadPoolExecutor.AbortPolicy} 中，处理程序在拒绝时抛出运行时 {@link RejectedExecutionException}。
 *          2. 在 {@link ThreadPoolExecutor.CallerRunsPolicy} 中，调用 {@code execute} 的线程自己运行任务。这提供了一个简单的反馈控制机制，可以减慢提交新任务的速度。
 *          3. 在 {@link ThreadPoolExecutor.DiscardPolicy} 中，无法执行的任务被简单地丢弃。
 *          4. 在{@link ThreadPoolExecutor.DiscardOldestPolicy}中，如果执行器没有关闭，工作队列头部的任务被丢弃，然后重试执行（可能会再次失败，导致重复执行。）
 *        可以定义和使用其他类型的 {@link RejectedExecutionHandler} 类。 这样做需要小心，特别是当策略设计为仅在特定容量或排队策略下工作时。
 *     g. 钩子方法: 此类提供了 {@code protected} 可覆盖的 {@link #beforeExecute(Thread, Runnable)} 和 {@link #afterExecute(Runnable, Throwable)} 方法，
 *        这些方法在每个任务执行之前和之后调用。 这些可用于操作执行环境； 例如，重新初始化 ThreadLocals、收集统计信息或添加日志条目。
 *        此外，方法 {@link #terminated} 可以被覆盖以执行在 Executor 完全终止后需要完成的任何特殊处理。如果钩子或回调方法抛出异常，内部工作线程可能会失败并突然终止。
 *     h. 队列维护: 方法 {@link #getQueue()} 允许访问工作队列以进行监控和调试。 强烈建议不要将此方法用于任何其他目的。
 *        提供的两种方法 {@link #remove(Runnable)} 和 {@link #purge} 可用于在大量排队任务被取消时协助存储回收。
 *     i. 定稿: 程序中不再引用且没有剩余线程的池将自动{@code shutdown}。 如果您想确保即使用户忘记调用 {@link #shutdown} 也能回收未引用的池，
 *        那么您必须通过设置适当的保持活动时间，使用零核心线程的下限来安排未使用的线程最终死亡 和/或设置 {@link #allowCoreThreadTimeOut(boolean)}。
 * D. 扩展示例。此类的大多数扩展都会覆盖一个或多个受保护的挂钩方法。 例如，这是一个添加简单暂停/恢复功能的子类：
 *  {@code
 * class PausableThreadPoolExecutor extends ThreadPoolExecutor {
 *   private boolean isPaused;
 *   private ReentrantLock pauseLock = new ReentrantLock();
 *   private Condition unpaused = pauseLock.newCondition();
 *
 *   public PausableThreadPoolExecutor(...) { super(...); }
 *
 *   protected void beforeExecute(Thread t, Runnable r) {
 *     super.beforeExecute(t, r);
 *     pauseLock.lock();
 *     try {
 *       while (isPaused) unpaused.await();
 *     } catch (InterruptedException ie) {
 *       t.interrupt();
 *     } finally {
 *       pauseLock.unlock();
 *     }
 *   }
 *
 *   public void pause() {
 *     pauseLock.lock();
 *     try {
 *       isPaused = true;
 *     } finally {
 *       pauseLock.unlock();
 *     }
 *   }
 *
 *   public void resume() {
 *     pauseLock.lock();
 *     try {
 *       isPaused = false;
 *       unpaused.signalAll();
 *     } finally {
 *       pauseLock.unlock();
 *     }
 *   }
 * }}
 */
/**
 * A.
 * An {@link ExecutorService} that executes each submitted task using
 * one of possibly several pooled threads, normally configured
 * using {@link Executors} factory methods.
 *
 * B.
 * <p>Thread pools address two different problems: they usually
 * provide improved performance when executing large numbers of
 * asynchronous tasks, due to reduced per-task invocation overhead,
 * and they provide a means of bounding and managing the resources,
 * including threads, consumed when executing a collection of tasks.
 * Each {@code ThreadPoolExecutor} also maintains some basic
 * statistics, such as the number of completed tasks.
 *
 * C.
 * <p>To be useful across a wide range of contexts, this class
 * provides many adjustable parameters and extensibility
 * hooks. However, programmers are urged to use the more convenient
 * {@link Executors} factory methods {@link
 * Executors#newCachedThreadPool} (unbounded thread pool, with
 * automatic thread reclamation), {@link Executors#newFixedThreadPool}
 * (fixed size thread pool) and {@link
 * Executors#newSingleThreadExecutor} (single background thread), that
 * preconfigure settings for the most common usage
 * scenarios. Otherwise, use the following guide when manually
 * configuring and tuning this class:
 *
 * <dl>
 *
 * <dt>Core and maximum pool sizes</dt>
 *
 * <dd>A {@code ThreadPoolExecutor} will automatically adjust the
 * pool size (see {@link #getPoolSize})
 * according to the bounds set by
 * corePoolSize (see {@link #getCorePoolSize}) and
 * maximumPoolSize (see {@link #getMaximumPoolSize}).
 *
 * When a new task is submitted in method {@link #execute(Runnable)},
 * and fewer than corePoolSize threads are running, a new thread is
 * created to handle the request, even if other worker threads are
 * idle.  If there are more than corePoolSize but less than
 * maximumPoolSize threads running, a new thread will be created only
 * if the queue is full.  By setting corePoolSize and maximumPoolSize
 * the same, you create a fixed-size thread pool. By setting
 * maximumPoolSize to an essentially unbounded value such as {@code
 * Integer.MAX_VALUE}, you allow the pool to accommodate an arbitrary
 * number of concurrent tasks. Most typically, core and maximum pool
 * sizes are set only upon construction, but they may also be changed
 * dynamically using {@link #setCorePoolSize} and {@link
 * #setMaximumPoolSize}. </dd>
 *
 * <dt>On-demand construction</dt>
 *
 * <dd>By default, even core threads are initially created and
 * started only when new tasks arrive, but this can be overridden
 * dynamically using method {@link #prestartCoreThread} or {@link
 * #prestartAllCoreThreads}.  You probably want to prestart threads if
 * you construct the pool with a non-empty queue. </dd>
 *
 * <dt>Creating new threads</dt>
 *
 * <dd>New threads are created using a {@link ThreadFactory}.  If not
 * otherwise specified, a {@link Executors#defaultThreadFactory} is
 * used, that creates threads to all be in the same {@link
 * ThreadGroup} and with the same {@code NORM_PRIORITY} priority and
 * non-daemon status. By supplying a different ThreadFactory, you can
 * alter the thread's name, thread group, priority, daemon status,
 * etc. If a {@code ThreadFactory} fails to create a thread when asked
 * by returning null from {@code newThread}, the executor will
 * continue, but might not be able to execute any tasks. Threads
 * should possess the "modifyThread" {@code RuntimePermission}. If
 * worker threads or other threads using the pool do not possess this
 * permission, service may be degraded: configuration changes may not
 * take effect in a timely manner, and a shutdown pool may remain in a
 * state in which termination is possible but not completed.</dd>
 *
 * <dt>Keep-alive times</dt>
 *
 * <dd>If the pool currently has more than corePoolSize threads,
 * excess threads will be terminated if they have been idle for more
 * than the keepAliveTime (see {@link #getKeepAliveTime(TimeUnit)}).
 * This provides a means of reducing resource consumption when the
 * pool is not being actively used. If the pool becomes more active
 * later, new threads will be constructed. This parameter can also be
 * changed dynamically using method {@link #setKeepAliveTime(long,
 * TimeUnit)}.  Using a value of {@code Long.MAX_VALUE} {@link
 * TimeUnit#NANOSECONDS} effectively disables idle threads from ever
 * terminating prior to shut down. By default, the keep-alive policy
 * applies only when there are more than corePoolSize threads. But
 * method {@link #allowCoreThreadTimeOut(boolean)} can be used to
 * apply this time-out policy to core threads as well, so long as the
 * keepAliveTime value is non-zero. </dd>
 *
 * <dt>Queuing</dt>
 *
 * <dd>Any {@link BlockingQueue} may be used to transfer and hold
 * submitted tasks.  The use of this queue interacts with pool sizing:
 *
 * <ul>
 *
 * <li> If fewer than corePoolSize threads are running, the Executor
 * always prefers adding a new thread
 * rather than queuing.</li>
 *
 * <li> If corePoolSize or more threads are running, the Executor
 * always prefers queuing a request rather than adding a new
 * thread.</li>
 *
 * <li> If a request cannot be queued, a new thread is created unless
 * this would exceed maximumPoolSize, in which case, the task will be
 * rejected.</li>
 *
 * </ul>
 *
 * There are three general strategies for queuing:
 * <ol>
 *
 * <li> <em> Direct handoffs.</em> A good default choice for a work
 * queue is a {@link SynchronousQueue} that hands off tasks to threads
 * without otherwise holding them. Here, an attempt to queue a task
 * will fail if no threads are immediately available to run it, so a
 * new thread will be constructed. This policy avoids lockups when
 * handling sets of requests that might have internal dependencies.
 * Direct handoffs generally require unbounded maximumPoolSizes to
 * avoid rejection of new submitted tasks. This in turn admits the
 * possibility of unbounded thread growth when commands continue to
 * arrive on average faster than they can be processed.  </li>
 *
 * <li><em> Unbounded queues.</em> Using an unbounded queue (for
 * example a {@link LinkedBlockingQueue} without a predefined
 * capacity) will cause new tasks to wait in the queue when all
 * corePoolSize threads are busy. Thus, no more than corePoolSize
 * threads will ever be created. (And the value of the maximumPoolSize
 * therefore doesn't have any effect.)  This may be appropriate when
 * each task is completely independent of others, so tasks cannot
 * affect each others execution; for example, in a web page server.
 * While this style of queuing can be useful in smoothing out
 * transient bursts of requests, it admits the possibility of
 * unbounded work queue growth when commands continue to arrive on
 * average faster than they can be processed.  </li>
 *
 * <li><em>Bounded queues.</em> A bounded queue (for example, an
 * {@link ArrayBlockingQueue}) helps prevent resource exhaustion when
 * used with finite maximumPoolSizes, but can be more difficult to
 * tune and control.  Queue sizes and maximum pool sizes may be traded
 * off for each other: Using large queues and small pools minimizes
 * CPU usage, OS resources, and context-switching overhead, but can
 * lead to artificially low throughput.  If tasks frequently block (for
 * example if they are I/O bound), a system may be able to schedule
 * time for more threads than you otherwise allow. Use of small queues
 * generally requires larger pool sizes, which keeps CPUs busier but
 * may encounter unacceptable scheduling overhead, which also
 * decreases throughput.  </li>
 *
 * </ol>
 *
 * </dd>
 *
 * <dt>Rejected tasks</dt>
 *
 * <dd>New tasks submitted in method {@link #execute(Runnable)} will be
 * <em>rejected</em> when the Executor has been shut down, and also when
 * the Executor uses finite bounds for both maximum threads and work queue
 * capacity, and is saturated.  In either case, the {@code execute} method
 * invokes the {@link
 * RejectedExecutionHandler#rejectedExecution(Runnable, ThreadPoolExecutor)}
 * method of its {@link RejectedExecutionHandler}.  Four predefined handler
 * policies are provided:
 *
 * <ol>
 *
 * <li> In the default {@link ThreadPoolExecutor.AbortPolicy}, the
 * handler throws a runtime {@link RejectedExecutionException} upon
 * rejection. </li>
 *
 * <li> In {@link ThreadPoolExecutor.CallerRunsPolicy}, the thread
 * that invokes {@code execute} itself runs the task. This provides a
 * simple feedback control mechanism that will slow down the rate that
 * new tasks are submitted. </li>
 *
 * <li> In {@link ThreadPoolExecutor.DiscardPolicy}, a task that
 * cannot be executed is simply dropped.  </li>
 *
 * <li>In {@link ThreadPoolExecutor.DiscardOldestPolicy}, if the
 * executor is not shut down, the task at the head of the work queue
 * is dropped, and then execution is retried (which can fail again,
 * causing this to be repeated.) </li>
 *
 * </ol>
 *
 * It is possible to define and use other kinds of {@link
 * RejectedExecutionHandler} classes. Doing so requires some care
 * especially when policies are designed to work only under particular
 * capacity or queuing policies. </dd>
 *
 * <dt>Hook methods</dt>
 *
 * <dd>This class provides {@code protected} overridable
 * {@link #beforeExecute(Thread, Runnable)} and
 * {@link #afterExecute(Runnable, Throwable)} methods that are called
 * before and after execution of each task.  These can be used to
 * manipulate the execution environment; for example, reinitializing
 * ThreadLocals, gathering statistics, or adding log entries.
 * Additionally, method {@link #terminated} can be overridden to perform
 * any special processing that needs to be done once the Executor has
 * fully terminated.
 *
 * <p>If hook or callback methods throw exceptions, internal worker
 * threads may in turn fail and abruptly terminate.</dd>
 *
 * <dt>Queue maintenance</dt>
 *
 * <dd>Method {@link #getQueue()} allows access to the work queue
 * for purposes of monitoring and debugging.  Use of this method for
 * any other purpose is strongly discouraged.  Two supplied methods,
 * {@link #remove(Runnable)} and {@link #purge} are available to
 * assist in storage reclamation when large numbers of queued tasks
 * become cancelled.</dd>
 *
 * <dt>Finalization</dt>
 *
 * <dd>A pool that is no longer referenced in a program <em>AND</em>
 * has no remaining threads will be {@code shutdown} automatically. If
 * you would like to ensure that unreferenced pools are reclaimed even
 * if users forget to call {@link #shutdown}, then you must arrange
 * that unused threads eventually die, by setting appropriate
 * keep-alive times, using a lower bound of zero core threads and/or
 * setting {@link #allowCoreThreadTimeOut(boolean)}.  </dd>
 *
 * </dl>
 *
 * D.
 * <p><b>Extension example</b>. Most extensions of this class
 * override one or more of the protected hook methods. For example,
 * here is a subclass that adds a simple pause/resume feature:
 *
 *  <pre> {@code
 * class PausableThreadPoolExecutor extends ThreadPoolExecutor {
 *   private boolean isPaused;
 *   private ReentrantLock pauseLock = new ReentrantLock();
 *   private Condition unpaused = pauseLock.newCondition();
 *
 *   public PausableThreadPoolExecutor(...) { super(...); }
 *
 *   protected void beforeExecute(Thread t, Runnable r) {
 *     super.beforeExecute(t, r);
 *     pauseLock.lock();
 *     try {
 *       while (isPaused) unpaused.await();
 *     } catch (InterruptedException ie) {
 *       t.interrupt();
 *     } finally {
 *       pauseLock.unlock();
 *     }
 *   }
 *
 *   public void pause() {
 *     pauseLock.lock();
 *     try {
 *       isPaused = true;
 *     } finally {
 *       pauseLock.unlock();
 *     }
 *   }
 *
 *   public void resume() {
 *     pauseLock.lock();
 *     try {
 *       isPaused = false;
 *       unpaused.signalAll();
 *     } finally {
 *       pauseLock.unlock();
 *     }
 *   }
 * }}</pre>
 *
 * @since 1.5
 * @author Doug Lea
 */
public class ThreadPoolExecutor extends AbstractExecutorService {

    /**
     * 20210813
     * A. 主池控制状态ctl是一个原子整数，封装了两个概念字段:
     *      a. workerCount，表示有效线程数
     *      b. runState，表示是否正在运行、正在关闭等
     * B. 为了将它们打包成一个int，我们将 workerCount 限制为 (2^29)-1（约 5 亿）个线程，而不是 (2^31)-1（20 亿）个其他可表示的线程。
     *    如果这在未来成为一个问题，可以将变量更改为 AtomicLong，并调整下面的移位/掩码常量。 但是在需要之前，这段代码使用 int 会更快更简单。
     * C. workerCount是允许启动和不允许停止的工人数量。该值可能与实际的活动线程数暂时不同，例如当 ThreadFactory 在被询问时未能创建线程时，
     *    以及退出线程在终止前仍在执行簿记时。 用户可见的池大小报告为工作人员集的当前大小。
     * D. runState 提供主要的生命周期控制，取值：
     *      a. RUNNING：接受新任务并处理排队任务
     *      b. SHUTDOWN：不接受新任务，但处理排队任务
     *      c. STOP：不接受新任务，不处理排队任务，并中断正在进行的任务
     *      d. TIDYING：所有任务都已终止，workerCount 为零，转换到状态 TIDYING 的线程将运行 terminate() 钩子方法
     *      e. TERMINATED： terminate() 已完成
     * E. 这些值之间的数字顺序很重要，以允许进行有序比较。runState 随时间单调增加，但不需要命中每个状态。转换是：
     *      a. RUNNING -> SHUTDOWN: 在调用 shutdown() 时，可能隐含在 finalize() 中
     *      b.（RUNNING 或 SHUTDOWN）-> STOP: 在调用 shutdownNow() 时
     *      c. SHUTDOWN -> TIDYING: 当队列和池都为空时
     *      d. STOP -> TIDYING: 当池为空时
     *      e. TIDYING -> TERMINATED: 当 terminate() 钩子方法完成时
     * F. 当状态达到 TERMINATED 时，在 awaitTermination()【阻塞直到所有任务在关闭请求后完成执行，或发生超时，或当前线程被中断，以先发生者为准】中等待的线程将返回。
     * G. 检测从 SHUTDOWN 到 TIDYING 的转换并不像您想要的那么直接，因为在非空之后队列可能会变空，在 SHUTDOWN 状态期间反之亦然，
     *    但是我们只能在看到它为空后看到 workerCount 时才终止 是 0（有时需要重新检查——见下文）。
     */
    /**
     * A.
     * The main pool control state, ctl, is an atomic integer packing
     * two conceptual fields
     *   workerCount, indicating the effective number of threads
     *   runState,    indicating whether running, shutting down etc
     *
     * B.
     * In order to pack them into one int, we limit workerCount to
     * (2^29)-1 (about 500 million) threads rather than (2^31)-1 (2
     * billion) otherwise representable. If this is ever an issue in
     * the future, the variable can be changed to be an AtomicLong,
     * and the shift/mask constants below adjusted. But until the need
     * arises, this code is a bit faster and simpler using an int.
     *
     * C.
     * The workerCount is the number of workers that have been
     * permitted to start and not permitted to stop.  The value may be
     * transiently different from the actual number of live threads,
     * for example when a ThreadFactory fails to create a thread when
     * asked, and when exiting threads are still performing
     * bookkeeping before terminating. The user-visible pool size is
     * reported as the current size of the workers set.
     *
     * D.
     * The runState provides the main lifecycle control, taking on values:
     *
     *   RUNNING:  Accept new tasks and process queued tasks
     *   SHUTDOWN: Don't accept new tasks, but process queued tasks
     *   STOP:     Don't accept new tasks, don't process queued tasks,
     *             and interrupt in-progress tasks
     *   TIDYING:  All tasks have terminated, workerCount is zero,
     *             the thread transitioning to state TIDYING
     *             will run the terminated() hook method
     *   TERMINATED: terminated() has completed
     *
     * E.
     * The numerical order among these values matters, to allow
     * ordered comparisons. The runState monotonically increases over
     * time, but need not hit each state. The transitions are:
     *
     * RUNNING -> SHUTDOWN
     *    On invocation of shutdown(), perhaps implicitly in finalize()
     * (RUNNING or SHUTDOWN) -> STOP
     *    On invocation of shutdownNow()
     * SHUTDOWN -> TIDYING
     *    When both queue and pool are empty
     * STOP -> TIDYING
     *    When pool is empty
     * TIDYING -> TERMINATED
     *    When the terminated() hook method has completed
     *
     * F.
     * Threads waiting in awaitTermination() will return when the
     * state reaches TERMINATED.
     *
     * G.
     * Detecting the transition from SHUTDOWN to TIDYING is less
     * straightforward than you'd like because the queue may become
     * empty after non-empty and vice versa during SHUTDOWN state, but
     * we can only terminate if, after seeing that it is empty, we see
     * that workerCount is 0 (which sometimes entails a recheck -- see
     * below).
     */

    // 线程池控制状态ctl, 高3位表示线程池状态, 低29位表示有效线程数, 初始为(1110, 0000, 0000, 0000, 0000, 0000, 0000, 0000) < 0
    private final AtomicInteger ctl = new AtomicInteger(ctlOf(RUNNING, 0));

    // 29位
    private static final int COUNT_BITS = Integer.SIZE - 3;

    // 低29位存储有效线程数(约5亿个线程)
    private static final int CAPACITY   = (1 << COUNT_BITS) - 1;

    //     * D. runState 提供主要的生命周期控制，取值：
    //     *      a. RUNNING：接受新任务并处理排队任务
    //     *      b. SHUTDOWN：不接受新任务，但处理排队任务
    //     *      c. STOP：不接受新任务，不处理排队任务，并中断正在进行的任务
    //     *      d. TIDYING：所有任务都已终止，workerCount 为零，转换到状态 TIDYING 的线程将运行 terminate() 钩子方法
    //     *      e. TERMINATED： terminate() 已完成
    // runState 存储在高位
    // runState is stored in the high-order bits
    private static final int RUNNING    = -1 << COUNT_BITS;// 运行状态, 高3位: 111
    private static final int SHUTDOWN   =  0 << COUNT_BITS;// 关闭状态, 高3位: 000
    private static final int STOP       =  1 << COUNT_BITS;// 停止状态, 高3位: 001
    private static final int TIDYING    =  2 << COUNT_BITS;// 整理状态, 高3位: 010
    private static final int TERMINATED =  3 << COUNT_BITS;// 终止状态, 高3位: 011

    // 打包和解包ctl
    // Packing and unpacking ctl

    // 根据控制位c获取运行状态: 与高3位(全1)相与, 得到高3位的值, 作为运行状态
    private static int runStateOf(int c)     { return c & ~CAPACITY; }

    // 根据控制位c获取工作线程数: 与低29位(全1)相与, 得到低29位的值, 作为工作线程数
    private static int workerCountOf(int c)  { return c & CAPACITY; }

    // 根据运行状态和工作线程数组装控制位ctl
    private static int ctlOf(int rs, int wc) { return rs | wc; }

    // 不需要解包 ctl 的位域访问器。 这些取决于位布局和 workerCount 永远不会为负。
    /*
     * Bit field accessors that don't require unpacking ctl.
     * These depend on the bit layout and on workerCount being never negative.
     */

    // 判断控制位c是否小于某状态s
    private static boolean runStateLessThan(int c, int s) {
        return c < s;
    }

    // 判断控制位c是否大于等于某状态s
    private static boolean runStateAtLeast(int c, int s) {
        return c >= s;
    }

    // 判断控制位c是否小于SHUTDOWN(0), 即是否为运行状态
    private static boolean isRunning(int c) {
        return c < SHUTDOWN;
    }

    // 尝试使用 CAS 递增 ctl 的 workerCount 字段。
    /**
     * Attempts to CAS-increment the workerCount field of ctl.
     */
    // CAS递增工作线程数
    private boolean compareAndIncrementWorkerCount(int expect) {
        return ctl.compareAndSet(expect, expect + 1);
    }

    // 尝试对 ctl 的 workerCount 字段进行 CAS 递减。
    /**
     * Attempts to CAS-decrement the workerCount field of ctl.
     */
    // CAS递减工作线程数
    private boolean compareAndDecrementWorkerCount(int expect) {
        return ctl.compareAndSet(expect, expect - 1);
    }

    // 减少 ctl 的 workerCount 字段。 这仅在线程突然终止时调用（请参阅 processWorkerExit）。 其他递减在 getTask 中执行。
    /**
     * Decrements the workerCount field of ctl. This is called only on
     * abrupt termination of a thread (see processWorkerExit). Other
     * decrements are performed within getTask.
     */
    // 减少一个工作线程, 底层依赖自旋+CAS递减工作线程数, 递减成功则退出自旋, 否则继续自旋
    private void decrementWorkerCount() {
        do {} while (!compareAndDecrementWorkerCount(ctl.get()));
    }

    // 用于保存任务和移交给工作线程的队列。 我们不要求 workQueue.poll() 返回 null 必然意味着 workQueue.isEmpty()，所以只依赖 isEmpty 来查看队列是否为空
    // （例如，在决定是否从 SHUTDOWN 转换到 TIDYING 时我们必须这样做）. 这适用于特殊用途的队列，例如允许 poll() 返回 null 的 DelayQueues，
    // 即使它稍后可能在延迟到期时返回非 null。
    /**
     * The queue used for holding tasks and handing off to worker
     * threads.  We do not require that workQueue.poll() returning
     * null necessarily means that workQueue.isEmpty(), so rely
     * solely on isEmpty to see if the queue is empty (which we must
     * do for example when deciding whether to transition from
     * SHUTDOWN to TIDYING).  This accommodates special-purpose
     * queues such as DelayQueues for which poll() is allowed to
     * return null even if it may later return non-null when delays
     * expire.
     */
    // 任务队列: 保存任务、转交任务给工作线程
    private final BlockingQueue<Runnable> workQueue;

    /**
     * 20210814
     * 锁定对工人集和相关簿记的访问。 虽然我们可以使用某种并发集，但结果证明通常最好使用锁。 其中一个原因是它序列化了interruptIdleWorkers，从而避免了不必要的中断风暴，尤其是在关机期间。
     * 否则退出线程将同时中断那些尚未中断的线程。 它还简化了largestPoolSize等一些相关的统计簿记。我们在shutdown和shutdownNow时也持有mainLock，为了保证worker set稳定，
     * 同时分别检查中断和实际中断的权限。
     */
    /**
     * Lock held on access to workers set and related bookkeeping.
     * While we could use a concurrent set of some sort, it turns out
     * to be generally preferable to use a lock. Among the reasons is
     * that this serializes interruptIdleWorkers, which avoids
     * unnecessary interrupt storms, especially during shutdown.
     * Otherwise exiting threads would concurrently interrupt those
     * that have not yet interrupted. It also simplifies some of the
     * associated statistics bookkeeping of largestPoolSize etc. We
     * also hold mainLock on shutdown and shutdownNow, for the sake of
     * ensuring workers set is stable while separately checking
     * permission to interrupt and actually interrupting.
     */
    // 主锁, 用于稳定工作线程集合
    private final ReentrantLock mainLock = new ReentrantLock();

    // 包含池中所有工作线程的集合。 仅在持有 mainLock 时访问。
    /**
     * Set containing all worker threads in pool. Accessed only when
     * holding mainLock.
     */
    // 工作线程集合
    private final HashSet<Worker> workers = new HashSet<Worker>();

    // 等待条件支持 awaitTermination
    /**
     * Wait condition to support awaitTermination
     */
    // 主锁的终止条件Condition
    private final Condition termination = mainLock.newCondition();

    // 跟踪达到的最大池大小。 只能在 mainLock 下访问。
    /**
     * Tracks largest attained pool size. Accessed only under
     * mainLock.
     */
    // 目前为止, 线程池中出现的最大线程数
    private int largestPoolSize;

    // 完成任务的计数器。仅在工作线程终止时更新。只能在 mainLock 下访问。
    /**
     * Counter for completed tasks. Updated only on termination of
     * worker threads. Accessed only under mainLock.
     */
    // 任务完成数
    private long completedTaskCount;

    // 所有用户控制参数都声明为volatile，以便正在进行的操作基于最新值，但不需要锁定，因为没有内部不变量依赖于它们相对于其他操作同步更改。
    /*
     * All user control parameters are declared as volatiles so that
     * ongoing actions are based on freshest values, but without need
     * for locking, since no internal invariants depend on them
     * changing synchronously with respect to other actions.
     */

    /**
     * 20210814
     * A. 新线程的工厂。所有线程都是使用这个工厂创建的（通过方法 addWorker）。 所有调用者都必须为 addWorker 失败做好准备，这可能反映了系统或用户限制线程数的策略。
     *    即使它不被视为错误，创建线程失败也可能导致新任务被拒绝或现有任务卡在队列中。
     * B. 我们更进一步，即使在尝试创建线程时可能会抛出 OutOfMemoryError 等错误时，也保留池不变量。由于需要在 Thread.start 中分配本机堆栈，因此此类错误相当常见，
     *    并且用户将希望执行干净池关闭以进行清理。 可能有足够的内存可供清理代码完成而不会遇到另一个 OutOfMemoryError。
     *
     */
    /**
     * A.
     * Factory for new threads. All threads are created using this
     * factory (via method addWorker).  All callers must be prepared
     * for addWorker to fail, which may reflect a system or user's
     * policy limiting the number of threads.  Even though it is not
     * treated as an error, failure to create threads may result in
     * new tasks being rejected or existing ones remaining stuck in
     * the queue.
     *
     * B.
     * We go further and preserve pool invariants even in the face of
     * errors such as OutOfMemoryError, that might be thrown while
     * trying to create threads.  Such errors are rather common due to
     * the need to allocate a native stack in Thread.start, and users
     * will want to perform clean pool shutdown to clean up.  There
     * will likely be enough memory available for the cleanup code to
     * complete without encountering yet another OutOfMemoryError.
     */
    // 线程工厂: 通过方法addWorker方法创建线程
    private volatile ThreadFactory threadFactory;

    // 在执行中饱和或关闭时调用的处理程序。
    /**
     * Handler called when saturated or shutdown in execute.
     */
    // 拒绝策略: 核心线程数、任务队列、最大线程数饱和时, 再接受到新任务时会执行拒绝策略
    private volatile RejectedExecutionHandler handler;

    // 等待工作的空闲线程的超时时间（以纳秒为单位）。 当存在超过 corePoolSize 或允许 CoreThreadTimeOut 时，线程使用此超时。 否则他们永远等待新的工作。
    /**
     * Timeout in nanoseconds for idle threads waiting for work.
     * Threads use this timeout when there are more than corePoolSize
     * present or if allowCoreThreadTimeOut. Otherwise they wait
     * forever for new work.
     */
    // 空闲线程的超时时间: 默认作用在非工作线程上
    private volatile long keepAliveTime;

    // 如果为 false（默认），核心线程即使在空闲时也保持活动状态。 如果为 true，则核心线程使用 keepAliveTime 超时等待工作。
    /**
     * If false (default), core threads stay alive even when idle.
     * If true, core threads use keepAliveTime to time out waiting
     * for work.
     */
    // 空闲线程的超时时间, 是否允许作用在核心工作线程上
    private volatile boolean allowCoreThreadTimeOut;

    // 核心池大小是保持活动状态的最小工作线程数（不允许超时等），除非设置了 allowCoreThreadTimeOut，在这种情况下，最小值为零。
    /**
     * Core pool size is the minimum number of workers to keep alive
     * (and not allow to time out etc) unless allowCoreThreadTimeOut
     * is set, in which case the minimum is zero.
     */
    // 核心线程数
    private volatile int corePoolSize;

    // 最大池大小。 请注意，实际最大值受 CAPACITY 内部限制。
    /**
     * Maximum pool size. Note that the actual maximum is internally
     * bounded by CAPACITY.
     */
    // 最大线程数
    private volatile int maximumPoolSize;

    /**
     * The default rejected execution handler
     */
    // 默认拒绝执行处理程序
    private static final RejectedExecutionHandler defaultHandler = new AbortPolicy();

    /**
     * 20210814
     * A. shutdown 和 shutdownNow 调用者所需的权限。 我们还要求（参见 checkShutdownAccess）调用者有权实际中断工作集中的线程（由 Thread.interrupt 管理，
     *    它依赖于 ThreadGroup.checkAccess，而后者又依赖于 SecurityManager.checkAccess）。 仅当这些检查通过时才尝试关闭。
     * B. Thread.interrupt 的所有实际调用（参见interruptIdleWorkers 和interruptWorkers）都会忽略SecurityExceptions，这意味着尝试的中断会默默地失败。
     *    在关闭的情况下，除非 SecurityManager 具有不一致的策略，否则它们不应失败，有时允许访问线程有时不允许。 在这种情况下，未能真正中断线程可能会禁用或延迟完全终止。
     *    interruptIdleWorkers 的其他用途是建议性的，未能实际中断只会延迟对配置更改的响应，因此不会被异常处理。
     */
    /**
     * A.
     * Permission required for callers of shutdown and shutdownNow.
     * We additionally require (see checkShutdownAccess) that callers
     * have permission to actually interrupt threads in the worker set
     * (as governed by Thread.interrupt, which relies on
     * ThreadGroup.checkAccess, which in turn relies on
     * SecurityManager.checkAccess). Shutdowns are attempted only if
     * these checks pass.
     *
     * B.
     * All actual invocations of Thread.interrupt (see
     * interruptIdleWorkers and interruptWorkers) ignore
     * SecurityExceptions, meaning that the attempted interrupts
     * silently fail. In the case of shutdown, they should not fail
     * unless the SecurityManager has inconsistent policies, sometimes
     * allowing access to a thread and sometimes not. In such cases,
     * failure to actually interrupt threads may disable or delay full
     * termination. Other uses of interruptIdleWorkers are advisory,
     * and failure to actually interrupt will merely delay response to
     * configuration changes so is not handled exceptionally.
     */
    // shutdown和shutdownNow调用所需的权限
    private static final RuntimePermission shutdownPerm = new RuntimePermission("modifyThread");

    /**
     * 20210814
     * Class Worker 主要维护线程运行任务的中断控制状态，以及其他次要的簿记。此类机会性地扩展 AbstractQueuedSynchronizer 以简化获取和释放围绕每个任务执行的锁。
     * 这可以防止旨在唤醒等待任务的工作线程的中断，而不是中断正在运行的任务。 我们实现了一个简单的不可重入互斥锁，而不是使用 ReentrantLock，
     * 因为我们不希望工作任务在调用诸如 setCorePoolSize 之类的池控制方法时能够重新获取锁。 此外，为了在线程实际开始运行任务之前抑制中断，我们将锁定状态初始化为负值，
     * 并在启动时（在 runWorker 中）清除它。
     */
    /**
     * Class Worker mainly maintains interrupt control state for
     * threads running tasks, along with other minor bookkeeping.
     * This class opportunistically extends AbstractQueuedSynchronizer
     * to simplify acquiring and releasing a lock surrounding each
     * task execution.  This protects against interrupts that are
     * intended to wake up a worker thread waiting for a task from
     * instead interrupting a task being run.  We implement a simple
     * non-reentrant mutual exclusion lock rather than use
     * ReentrantLock because we do not want worker tasks to be able to
     * reacquire the lock when they invoke pool control methods like
     * setCorePoolSize.  Additionally, to suppress interrupts until
     * the thread actually starts running tasks, we initialize lock
     * state to a negative value, and clear it upon start (in
     * runWorker).
     */
    // 线程工人, 实现Runnable本身可以作为一个任务, 实现AQS以简化获取和释放围绕每个任务执行的锁, 实现了一个简单的不可重入互斥锁
    private final class Worker extends AbstractQueuedSynchronizer implements Runnable {

        // 这个类永远不会被序列化，但我们提供了一个 serialVersionUID 来抑制 javac 警告。
        /**
         * This class will never be serialized, but we provide a
         * serialVersionUID to suppress a javac warning.
         */
        private static final long serialVersionUID = 6138294804551838833L;

        // 该工人正在运行的线程。如果工厂失败，则为空。
        /** Thread this worker is running in.  Null if factory fails. */
        // 该工人正在运行的线程, 如果工厂生产线程失败, 则为null
        final Thread thread;

        // 要运行的初始任务。 可能为空。
        /** Initial task to run.  Possibly null. */
        // 该工人要运行的初始任务, 可能会为空
        Runnable firstTask;

        // 每线程任务计数器
        /** Per-thread task counter */
        // worker已完成任务计数器
        volatile long completedTasks;

        // 使用给定的第一个任务和来自 ThreadFactory 的线程创建。
        /**
         * Creates with given first task and thread from ThreadFactory.
         *
         * // 第一个任务（如果没有则为空）
         * @param firstTask the first task (null if none)
         */
        // 注意! 在构造Worker时, 使用了当前worker实例作为Thread#Runnable实例变量, 如果运行的目标任务不为null, 则调用Runnable#run方法
        Worker(Runnable firstTask) {
            setState(-1); // inhibit interrupts until runWorker 禁止中断直到 runWorker
            this.firstTask = firstTask;

            // 注意! 在构造Worker时, 使用了当前worker实例作为Thread#Runnable实例变量, 如果运行的目标任务不为null, 则调用Runnable#run方法
            this.thread = getThreadFactory().newThread(this);
        }

        // 将主运行循环委托给外部 runWorker
        /** Delegates main run loop to outer runWorker  */
        // 指定当前工人来运行任务: 先获取firstTask -> 从任务队列中获取任务 -> beforeExecute -> 运行获取到的任务 -> afterExecute -> 线程运行后的清理工作processWorkerExit
        public void run() {
            runWorker(this);
        }

        // Lock methods
        //
        // The value 0 represents the unlocked state. // 值 0 表示解锁状态。
        // The value 1 represents the locked state. // 值 1 表示锁定状态。

        // 是否存在锁独占线程
        protected boolean isHeldExclusively() {
            return getState() != 0;
        }

        // 快速失败式获取锁, 只会将0 CAS更新为1(unused没用), CAS成功则设置当前线程为独占线程, 并返回true, 代表获取成功, 否则返回false, 代表获取失败
        protected boolean tryAcquire(int unused) {
            if (compareAndSetState(0, 1)) {
                setExclusiveOwnerThread(Thread.currentThread());
                return true;
            }
            return false;
        }

        // 释放锁, 只会将同步状态设置为0, 并清空独占线程以及返回true, 代表释放成功
        protected boolean tryRelease(int unused) {
            setExclusiveOwnerThread(null);
            setState(0);
            return true;
        }

        // 阻塞式获取锁, 快速失败失败还会进去AQS队列中排队
        public void lock()        { acquire(1); }

        // 快速失败式获取锁, 只会将0 CAS更新为1(unused没用), CAS成功则设置当前线程为独占线程, 并返回true, 代表获取成功, 否则返回false, 代表获取失败
        public boolean tryLock()  { return tryAcquire(1); }

        // 释放锁, 只会将同步状态设置为0, 并清空独占线程以及返回true, 代表释放成功
        public void unlock()      { release(1); }

        // 判断worker是否会阻塞, 通过判断是否存在独占线程来判断
        public boolean isLocked() { return isHeldExclusively(); }

        // 启动时中断
        void interruptIfStarted() {
            Thread t;

            // 如果同步器状态大于0, 且t线程没有被中断, 则中断t线程
            if (getState() >= 0 && (t = thread) != null && !t.isInterrupted()) {
                try {
                    t.interrupt();
                } catch (SecurityException ignore) {
                }
            }
        }
    }

    // 设置控制状态的方法
    /*
     * Methods for setting control state
     */

    // 将 runState 转换为给定的目标，或者如果已经至少是给定的目标，则不理会它。
    /**
     * Transitions runState to given target, or leaves it alone if
     * already at least the given target.
     *
     * // targetState 所需的状态，SHUTDOWN 或 STOP（但不是 TIDYING 或 TERMINATED——为此使用 tryTerminate）
     * @param targetState the desired state, either SHUTDOWN or STOP
     *        (but not TIDYING or TERMINATED -- use tryTerminate for that)
     */
    // 转换线程池状态为targetState
    private void advanceRunState(int targetState) {
        // 开始自旋
        for (;;) {
            // 获取ctl控制位c
            int c = ctl.get();

            // 如果控制位c至少大于等于targetState, 则不做任何处理, 直接退出自旋; 如果c小于targetState, 则CAS更新为targetState状态
            if (runStateAtLeast(c, targetState) ||
                ctl.compareAndSet(c, ctlOf(targetState, workerCountOf(c))))
                break;
        }
    }

    /**
     * 20210814
     * 如果（关闭和池和队列为空）或（停止和池为空），则转换到 TERMINATED 状态。 如果有资格终止但 workerCount 非零，则中断空闲的工作程序以确保关闭信号传播。
     * 必须在任何可能使终止成为可能的操作之后调用此方法 - 在关闭期间减少工作线程数或从队列中删除任务。 该方法是非私有的，允许从 ScheduledThreadPoolExecutor 访问。
     */
    /**
     * Transitions to TERMINATED state if either (SHUTDOWN and pool
     * and queue empty) or (STOP and pool empty).  If otherwise
     * eligible to terminate but workerCount is nonzero, interrupts an
     * idle worker to ensure that shutdown signals propagate. This
     * method must be called following any action that might make
     * termination possible -- reducing worker count or removing tasks
     * from the queue during shutdown. The method is non-private to
     * allow access from ScheduledThreadPoolExecutor.
     */
    // 尝试线程池状态转换: STOP/SHUTDOWN -> 中断其中一个线程 -> TIDYING(工作线程数为0) -> TERMINATED状态 -> 通知所有等待线程池主锁Condition的线程
    final void tryTerminate() {
        // 开始自旋
        for (;;) {
            // 获取状态控制位c
            int c = ctl.get();

            // 如果线程池为SHUTDOWN、TIDYING、TERMINATED状态, 或者为SHUTDOWN状态且任务队列非空时, 说明线程池不应该被终止或者已经被终止, 此时直接返回即可
            if (isRunning(c) ||
                runStateAtLeast(c, TIDYING) ||
                (runStateOf(c) == SHUTDOWN && ! workQueue.isEmpty()))
                return;

            // 线程池为STOP状态 或者 为SHUTDOWN状态且队列为空, 如果工作线程数不为0, 此时确实需要清空工作线程
            if (workerCountOf(c) != 0) { // Eligible to terminate // 有资格终止
                // 中断可能正在等待任务的线程, 只中断其中一个线程, 中断后则返回
                interruptIdleWorkers(ONLY_ONE);
                return;
            }

            // 线程池为STOP状态 或者 为SHUTDOWN状态且队列为空, 如果工作线程数为0, 则获取线程池主锁
            final ReentrantLock mainLock = this.mainLock;
            mainLock.lock();
            try {
                // CAS更新状态控制位为TIDYING, 并终止线程池
                if (ctl.compareAndSet(c, ctlOf(TIDYING, 0))) {
                    try {
                        terminated();
                    } finally {
                        // 终止完毕则更新ctl为TERMINATED
                        ctl.set(ctlOf(TERMINATED, 0));

                        // 唤醒所有等待线程池主锁的终止条件Condition的线程, 然后返回
                        termination.signalAll();
                    }
                    return;
                }
            } finally {
                // 最后释放线程池主锁
                mainLock.unlock();
            }

            // 否则重试失败的 CAS
            // else retry on failed CAS
        }
    }

    // 控制工作线程中断的方法。
    /*
     * Methods for controlling interrupts to worker threads.
     */

    // 如果有安全管理器，请确保调用者通常有权关闭线程（请参阅 shutdownPerm）。 如果这通过，还要确保调用者可以中断每个工作线程。
    // 即使第一次检查通过，如果 SecurityManager 特殊对待某些线程，这也可能不是真的。
    /**
     * If there is a security manager, makes sure caller has
     * permission to shut down threads in general (see shutdownPerm).
     * If this passes, additionally makes sure the caller is allowed
     * to interrupt each worker thread. This might not be true even if
     * first check passed, if the SecurityManager treats some threads
     * specially.
     */
    // 如果有安全管理器，请确保调用者通常有权关闭线程（请参阅 shutdownPerm）。 如果这通过，还要确保调用者可以中断每个工作线程。
    private void checkShutdownAccess() {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(shutdownPerm);
            final ReentrantLock mainLock = this.mainLock;
            mainLock.lock();
            try {
                for (Worker w : workers)
                    security.checkAccess(w.thread);
            } finally {
                mainLock.unlock();
            }
        }
    }

    // 中断所有线程，即使是活动线程。 忽略 SecurityExceptions（在这种情况下，某些线程可能保持不间断）。
    /**
     * Interrupts all threads, even if active. Ignores SecurityExceptions
     * (in which case some threads may remain uninterrupted).
     */
    // 中断所有线程，即使是活动线程。 忽略 SecurityExceptions（在这种情况下，某些线程可能保持不间断）
    private void interruptWorkers() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            for (Worker w : workers)
                w.interruptIfStarted();
        } finally {
            mainLock.unlock();
        }
    }

    /**
     * 20210814
     * 中断可能正在等待任务的线程（如未被锁定所示），以便它们可以检查终止或配置更改。 忽略 SecurityExceptions（在这种情况下，某些线程可能保持不间断）。
     */
    /**
     * Interrupts threads that might be waiting for tasks (as
     * indicated by not being locked) so they can check for
     * termination or configuration changes. Ignores
     * SecurityExceptions (in which case some threads may remain
     * uninterrupted).
     *
     * // onlyOne 如果为真，则最多中断一名工人。 仅当以其他方式启用终止但仍有其他工作人员时，才从 tryTerminate 调用此方法。
     *    在这种情况下，在所有线程当前都在等待的情况下，最多一个等待的 worker 被中断以传播关闭信号。 中断任何任意线程可确保自关闭开始以来新到达的工人也将最终退出。
     *    为了保证最终终止，总是只中断一个空闲的worker就足够了，但是shutdown()会中断所有空闲的worker，以便冗余的worker立即退出，而不是等待一个落后的任务完成。
     * @param onlyOne If true, interrupt at most one worker. This is
     * called only from tryTerminate when termination is otherwise
     * enabled but there are still other workers.  In this case, at
     * most one waiting worker is interrupted to propagate shutdown
     * signals in case all threads are currently waiting.
     * Interrupting any arbitrary thread ensures that newly arriving
     * workers since shutdown began will also eventually exit.
     * To guarantee eventual termination, it suffices to always
     * interrupt only one idle worker, but shutdown() interrupts all
     * idle workers so that redundant workers exit promptly, not
     * waiting for a straggler task to finish.
     */
    // 中断可能正在等待任务的线程, 如果指定onlyOne, 则只中断其中一个线程
    private void interruptIdleWorkers(boolean onlyOne) {
        // 获取线程池主锁
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            // 遍历工作线程集合
            for (Worker w : workers) {
                // 获取工人w中的线程t
                Thread t = w.thread;

                // 如果线程t没有被中断, 且获取工人w中的不可重入锁成功(快速失败获取, 如果获取到说明线程空闲需要被中断, 否则说明线程繁忙不需要被中断), 则中断t线程
                if (!t.isInterrupted() && w.tryLock()) {
                    try {
                        t.interrupt();
                    } catch (SecurityException ignore) {
                    } finally {
                        // 释放工人w中的不可重入锁
                        w.unlock();
                    }
                }

                // 如果只中断一个线程, 则退出遍历
                if (onlyOne)
                    break;
            }
        } finally {
            // 释放线程池主锁
            mainLock.unlock();
        }
    }

    // interruptIdleWorkers 的常见形式，以避免必须记住布尔参数的含义。
    /**
     * Common form of interruptIdleWorkers, to avoid having to
     * remember what the boolean argument means.
     */
    // 中断可能正在等待任务的线程, 不止中断一个线程, 而是所有空闲的工作线程集合
    private void interruptIdleWorkers() {
        interruptIdleWorkers(false);
    }
    private static final boolean ONLY_ONE = true;

    // 杂项实用程序，其中大部分也导出到 ScheduledThreadPoolExecutor
    /*
     * Misc utilities, most of which are also exported to
     * ScheduledThreadPoolExecutor
     */

    // 为给定的命令调用被拒绝的执行处理程序。 包保护供 ScheduledThreadPoolExecutor 使用。
    /**
     * Invokes the rejected execution handler for the given command.
     * Package-protected for use by ScheduledThreadPoolExecutor.
     */
    // 履行任务command和当前任务执行者executor的拒绝策略
    final void reject(Runnable command) {
        handler.rejectedExecution(command, this);
    }

    // 在调用关闭时运行状态转换后执行任何进一步的清理。 此处无操作，但被 ScheduledThreadPoolExecutor 用于取消延迟任务。
    /**
     * Performs any further cleanup following run state transition on
     * invocation of shutdown.  A no-op here, but used by
     * ScheduledThreadPoolExecutor to cancel delayed tasks.
     */
    // 在调用关闭时运行状态转换后执行任何进一步的清理。 此处无操作，但被 ScheduledThreadPoolExecutor 用于取消延迟任务。
    void onShutdown() {
    }

    // ScheduledThreadPoolExecutor 需要检查状态以在关闭期间启用正在运行的任务。
    /**
     * State check needed by ScheduledThreadPoolExecutor to
     * enable running tasks during shutdown.
     *
     * // true 如果应该返回 true 如果 SHUTDOWN
     * @param shutdownOK true if should return true if SHUTDOWN
     */
    // 如果线程池为运行状态, 或者为关闭状态且已经允许关闭了, 则返回true
    final boolean isRunningOrShutdown(boolean shutdownOK) {
        int rs = runStateOf(ctl.get());
        return rs == RUNNING || (rs == SHUTDOWN && shutdownOK);
    }

    // 将任务队列排到一个新列表中，通常使用drainTo。
    // 但是如果队列是 DelayQueue 或任何其他类型的队列，对于它的 poll 或 drainTo 可能无法删除某些元素，它会一个一个地删除它们。
    /**
     * Drains the task queue into a new list, normally using
     * drainTo. But if the queue is a DelayQueue or any other kind of
     * queue for which poll or drainTo may fail to remove some
     * elements, it deletes them one by one.
     */
    // 将任务队列排到一个新列表中
    private List<Runnable> drainQueue() {
        BlockingQueue<Runnable> q = workQueue;
        ArrayList<Runnable> taskList = new ArrayList<Runnable>();
        q.drainTo(taskList);
        if (!q.isEmpty()) {
            for (Runnable r : q.toArray(new Runnable[0])) {
                if (q.remove(r))
                    taskList.add(r);
            }
        }
        return taskList;
    }

    // 创建、运行和清理工人后的方法
    /*
     * Methods for creating, running and cleaning up after workers
     */

    /**
     * 20210814
     * 检查是否可以根据当前池状态和给定界限（核心或最大值）添加新的工作线程。
     * 如果是这样，则相应地调整工作人员数量，并且如果可能，将创建并启动一个新工作人员，将 firstTask 作为其第一个任务运行。
     * 如果池已停止或有资格关闭，则此方法返回 false。
     * 如果线程工厂在询问时未能创建线程，它也会返回 false。
     * 如果线程创建失败，要么是由于线程工厂返回 null，要么是由于异常（通常是 Thread.start() 中的 OutOfMemoryError），我们会干净利落地回滚。
     */
    /**
     * Checks if a new worker can be added with respect to current
     * pool state and the given bound (either core or maximum). If so,
     * the worker count is adjusted accordingly, and, if possible, a
     * new worker is created and started, running firstTask as its
     * first task. This method returns false if the pool is stopped or
     * eligible to shut down. It also returns false if the thread
     * factory fails to create a thread when asked.  If the thread
     * creation fails, either due to the thread factory returning
     * null, or due to an exception (typically OutOfMemoryError in
     * Thread.start()), we roll back cleanly.
     *
     * // firstTask 新线程应该首先运行的任务（如果没有，则为 null）。
     *    当线程少于 corePoolSize 时（在这种情况下我们总是启动一个），或者当队列已满（在这种情况下我们必须绕过队列）时，使用初始的第一个任务（在方法 execute() 中）创建工人 .
     *    最初空闲线程通常通过 prestartCoreThread 创建或替换其他垂死的工人。
     * @param firstTask the task the new thread should run first (or
     * null if none). Workers are created with an initial first task
     * (in method execute()) to bypass queuing when there are fewer
     * than corePoolSize threads (in which case we always start one),
     * or when the queue is full (in which case we must bypass queue).
     * Initially idle threads are usually created via
     * prestartCoreThread or to replace other dying workers.
     *
     * // 如果为 true，则使用 corePoolSize 作为绑定，否则为 maximumPoolSize。 （此处使用布尔指标而不是值以确保在检查其他池状态后读取新值）。
     * @param core if true use corePoolSize as bound, else
     * maximumPoolSize. (A boolean indicator is used here rather than a
     * value to ensure reads of fresh values after checking other pool
     * state).
     *
     * // 如果成功则为真
     * @return true if successful
     */
    // 检查是否可以根据当前池状态和给定界限（核心或最大值）添加新的工作线程, 如果添加工作线程成功, 则返回true; 如果添加工作线程失败, 则回滚工作线程并返回false
    private boolean addWorker(Runnable firstTask, boolean core) {
        // 重试点retry
        retry:

        // 开始自旋, 先判断线程状态是否适合增加线程数, 再自旋增加ctl工作线程数
        for (;;) {
            // 获取ctl状态控制位c, 运行状态rs
            int c = ctl.get();
            int rs = runStateOf(c);

            // 仅在必要时检查队列是否为空。
            // Check if queue empty only if necessary.
            // 如果运行状态不为RUNNING状态
            if (rs >= SHUTDOWN &&
                // 如果不为SHUTDOWN状态, 或者为SHUTDOWN状态但指定了任务(因为此时不接受新的任务), 或者任务队列为非空时, 则返回false, 代表不需要添加新线程
                ! (rs == SHUTDOWN && firstTask == null && ! workQueue.isEmpty()))
                return false;

            // 如果状态为运行状态, 或者为SHUTDOWN状态状态但任务队列不为空, 则再次开始自旋, 开始增加ctl工作线程数
            for (;;) {
                // 根据ctl状态控制位c获取工作线程数wc
                int wc = workerCountOf(c);

                // 如果工作线程数wc已经大于最大线程容量(5亿)
                if (wc >= CAPACITY ||
                    // 或者如果wc已经大于核心线程数或者最大线程数
                    wc >= (core ? corePoolSize : maximumPoolSize))
                    // 则返回false, 表示不需要添加新线程
                    return false;

                // 如果需要添加新线程, 则CAS递增ctl工作线程数
                if (compareAndIncrementWorkerCount(c))
                    // 如果CAS更新成功, 则退出最外层的自旋
                    break retry;

                // 如果CAS更新失败, 则重新获取ctl状态控制位
                c = ctl.get();  // Re-read ctl 重读ctl

                // 如果运行状态发生了变化, 为防止线程池已关闭, 则重试最外层循环
                if (runStateOf(c) != rs)
                    continue retry;

                // 否则 CAS 由于 workerCount 变化而失败； 重试内循环
                // else CAS failed due to workerCount change; retry inner loop
            }
        }

        // 更新完ctl工作线程数后, 真正开始创建新的worker
        boolean workerStarted = false;
        boolean workerAdded = false;
        Worker w = null;
        try {
            // 使用给定的第一个任务和来自 ThreadFactory 的线程创建
            // 注意! 在构造Worker时, 使用了当前worker实例作为Thread#Runnable实例变量, 如果运行的目标任务不为null, 则调用Runnable#run方法
            w = new Worker(firstTask);

            // 获取工人w中的线程t
            final Thread t = w.thread;

            // 如果t不为空, 则获取线程池主锁
            if (t != null) {
                final ReentrantLock mainLock = this.mainLock;
                mainLock.lock();
                try {
                    // 保持锁定时重新检查。 在 ThreadFactory 失败或在获取锁之前关闭时退出。
                    // Recheck while holding lock.
                    // Back out on ThreadFactory failure or if
                    // shut down before lock acquired.
                    // 获取状态控制位ctl的运行状态rs
                    int rs = runStateOf(ctl.get());

                    // 如果rs为RUNNING状态 或者 为SHUTDOWN状态且firstTask为空, 则检查t线程是否已经启动, 如果已经启动则抛出异常
                    if (rs < SHUTDOWN || (rs == SHUTDOWN && firstTask == null)) {
                        if (t.isAlive()) // precheck that t is startable 预先检查 t 是否可启动
                            throw new IllegalThreadStateException();

                        // 如果t线程仍未启动, 说明线程t有效, 则将工人w添加到工作线程集合中
                        workers.add(w);

                        // 获取工作线程集合中大小s
                        int s = workers.size();

                        // 如果s 大于 能跟踪到的最大线程数, 则更新能跟踪到的最大线程数为s, 即目前为止, 线程池中出现的最大线程数
                        if (s > largestPoolSize)
                            largestPoolSize = s;

                        // 最后更新worker已添加成功
                        workerAdded = true;
                    }
                    // 如果rs不为RUNNING状态, 当为SHUTDOWN状态且还提交了任务firstTask, 则不做任何操作
                } finally {
                    // 释放线程池主锁
                    mainLock.unlock();
                }

                // 如果worker成功添加, 则启动t线程, 并更新worker已启动
                if (workerAdded) {
                    t.start();
                    workerStarted = true;
                }
            }
        } finally {
            // 如果最后worker仍未启动, 则回滚工作线程: 从工作线程集合中移除、减少ctl工作线程数、尝试线程池状态转换
            if (! workerStarted)
                addWorkerFailed(w);
        }

        // 返回worker是否启动成功
        return workerStarted;
    }

    /**
     * 20210814
     * 回滚工作线程创建。
     * - 从工人中删除工人，如果存在
     * - 减少工人数量
     * - 重新检查终止，以防该工人的存在阻碍终止
     */
    /**
     * Rolls back the worker thread creation.
     * - removes worker from workers, if present
     * - decrements worker count
     * - rechecks for termination, in case the existence of this
     *   worker was holding up termination
     */
    // 回滚工作线程: 从工作线程集合中移除、减少ctl工作线程数、尝试线程池状态转换
    private void addWorkerFailed(Worker w) {
        // 获取线程池主锁
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            // 如果需要回滚的线程不为空, 则从工作线程集合中移除
            if (w != null)
                workers.remove(w);

            // 减少一个工作线程, 底层依赖自旋+CAS递减工作线程数, 递减成功则退出自旋, 否则继续自旋
            decrementWorkerCount();

            // 尝试线程池状态转换: STOP/SHUTDOWN -> 中断其中一个线程 -> TIDYING(工作线程数为0) -> TERMINATED状态 -> 通知所有等待线程池主锁Condition的线程
            tryTerminate();
        } finally {
            // 释放线程池主锁
            mainLock.unlock();
        }
    }

    /**
     * 20210814
     * 为垂死的工人执行清理和簿记。 仅从工作线程调用。 除非设置了 CompletedAbruptly，否则假定已经调整了 workerCount 以考虑退出。
     * 此方法从工作线程集中删除线程，并且可能会终止池或替换工作线程，如果它由于用户任务异常退出，或者如果少于 corePoolSize 的工作线程正在运行或队列非空但没有工作线程。
     */
    /**
     * Performs cleanup and bookkeeping for a dying worker. Called
     * only from worker threads. Unless completedAbruptly is set,
     * assumes that workerCount has already been adjusted to account
     * for exit.  This method removes thread from worker set, and
     * possibly terminates the pool or replaces the worker if either
     * it exited due to user task exception or if fewer than
     * corePoolSize workers are running or queue is non-empty but
     * there are no workers.
     *
     * @param w the worker
     *
     * // 如果工人因用户异常而死亡
     * @param completedAbruptly if the worker died due to user exception
     */
    // 从工作线程集中删除线程, 并且可能会终止池或替换工作线程, 当指定的completedAbruptly为true时, 会先减少ctl工作线程数并替换工作线程
    private void processWorkerExit(Worker w, boolean completedAbruptly) {
        // 如果worker需要突然死亡, 则减少一个ctl中的工作线程, 底层依赖自旋+CAS递减工作线程数, 递减成功则退出自旋, 否则继续自旋
        if (completedAbruptly) // If abrupt, then workerCount wasn't adjusted // 如果突然，则 workerCount 未调整
            decrementWorkerCount();

        // 获取线程池主锁
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            // 累加worker任务完成数作为线程池任务完成数
            completedTaskCount += w.completedTasks;

            // 将w工人从worker集合中移除
            workers.remove(w);
        } finally {
            // 释放线程池主锁
            mainLock.unlock();
        }

        // 尝试线程池状态的转换: STOP/SHUTDOWN -> 中断其中一个线程 -> TIDYING(工作线程数为0) -> TERMINATED状态 -> 通知所有等待线程池主锁Condition的线程
        tryTerminate();

        // 获取状态控制位ctl
        int c = ctl.get();

        // 如果状态为RUNNING或者SHUTDOWN
        if (runStateLessThan(c, STOP)) {
            // 如果之前没有减少过ctl的工作线程数
            if (!completedAbruptly) {
                // 获取最少线程数
                int min = allowCoreThreadTimeOut ? 0 : corePoolSize;

                // 如果最少线程数为0(此时肯定设置了核心线程数的过期时间), 同时任务队列不为空, 则至少保留一个线程
                if (min == 0 && ! workQueue.isEmpty())
                    min = 1;

                // 如果工作线程数大于等于最少线程数, 则直接返回即可, 代表不需要更换worker(此时ctl)
                if (workerCountOf(c) >= min)
                    return; // replacement not needed // 不需要更换
            }

            // 如果worker之前已经突然死亡了, 则检查是否可以根据当前池状态和给定界限（核心或最大值）添加新的工作线程, 如果添加工作线程成功, 则返回true; 如果添加工作线程失败, 则回滚工作线程并返回false
            addWorker(null, false);
        }
    }

    /**
     * 20210814
     * 执行阻塞或定时等待任务，具体取决于当前的配置设置，或者如果由于以下任何原因而必须退出该工作程序，则返回 null：
     *      1. 有多个maximumPoolSize worker（由于调用setMaximumPoolSize）。
     *      2. 池停止。
     *      3. 池关闭，队列为空。
     *      4. 这个worker等待任务超时，超时的worker在超时等待之前和之后都会被终止（即{@code allowCoreThreadTimeOut || workerCount > corePoolSize}），
     *         如果队列是非空，这个工作者不是池中的最后一个线程。
     */
    /**
     * Performs blocking or timed wait for a task, depending on
     * current configuration settings, or returns null if this worker
     * must exit because of any of:
     * 1. There are more than maximumPoolSize workers (due to
     *    a call to setMaximumPoolSize).
     * 2. The pool is stopped.
     * 3. The pool is shutdown and the queue is empty.
     * 4. This worker timed out waiting for a task, and timed-out
     *    workers are subject to termination (that is,
     *    {@code allowCoreThreadTimeOut || workerCount > corePoolSize})
     *    both before and after the timed wait, and if the queue is
     *    non-empty, this worker is not the last thread in the pool.
     *
     * // 任务，如果工作人员必须退出，则为 null，在这种情况下，workerCount 递减
     * @return task, or null if the worker must exit, in which case
     *         workerCount is decremented
     */
    // 执行阻塞或定时等待任务, 如果需要淘汰线程, 则使用存活时间定时获取任务, 在获取不到时则标记超时等待下一轮清空多余线程; 如果不需要淘汰线程, 则阻塞获取任务
    private Runnable getTask() {
        boolean timedOut = false; // Did the last poll() time out? // 上次 poll() 是否超时？

        // 开始自旋
        for (;;) {
            // 获取ctl状态控制位c, 运行状态rs
            int c = ctl.get();
            int rs = runStateOf(c);

            // 仅在必要时检查队列是否为空。
            // Check if queue empty only if necessary.
            // 如果运行状态为停止(不接受新任务)、整理(任务终止)、终止状态(已完成), 或者任务队列为空
            if (rs >= SHUTDOWN && (rs >= STOP || workQueue.isEmpty())) {
                // 减少一个工作线程, 底层依赖自旋+CAS递减工作线程数, 递减成功则退出自旋, 否则继续自旋
                decrementWorkerCount();

                // 返回null, 表示当前线程必须退出
                return null;
            }

            // 根据状态控制位c获取工作线程数wc
            int wc = workerCountOf(c);

            // 工人会被淘汰吗？
            // Are workers subject to culling?
            // 如果允许核心线程超时, 或者工作线程数大于核心线程数(即存在非核心线程时)， 则认为可能有线程需要淘汰
            boolean timed = allowCoreThreadTimeOut || wc > corePoolSize;

            // 如果(工作线程数大于最大线程数 或者 有线程需要淘汰) 同时 (工作线程数至少为1 或者 任务队列为空), 则CAS递减工作线程数
            if ((wc > maximumPoolSize || (timed && timedOut)) && (wc > 1 || workQueue.isEmpty())) {
                // 如果CAS成功, 则返回null, 表示当前线程必须退出
                if (compareAndDecrementWorkerCount(c))
                    return null;

                // 如果CAS失败, 则继续自旋判断
                continue;
            }

            // 当前线程数正常
            try {
                Runnable r = timed ?
                        // 如果有线程需要淘汰(大于核心数 或者 核心数需要超时), 则根据存活时间定时从任务队列获取任务r
                        workQueue.poll(keepAliveTime, TimeUnit.NANOSECONDS) :
                        // 如果没有线程需要淘汰[!(大于核心数 或者 核心数需要超时)], 则阻塞获取任务队列中的任务r
                        workQueue.take();

                // 如果获取到任务r, 则返回任务r, 表示获取成功
                if (r != null)
                    return r;

                // 如果没获取到任务r, 则标识timedOut为true, 代表获取超时(由于线程争抢激烈导致, 此时设置超时会在下轮自旋时进行清空多余线程)
                timedOut = true;
            } catch (InterruptedException retry) {
                // 如果定时获取期间抛出异常, 则任务还没超时
                timedOut = false;
            }
        }
    }

    /**
     * 20210814
     * A. 主工作线程运行循环。 反复从队列中获取任务并执行，同时解决了一些问题：
     *      1. 我们可以从一个初始任务开始，在这种情况下我们不需要得到第一个。 否则，只要 pool 正在运行，我们就会从 getTask 获取任务。
     *         如果它返回 null，则由于池状态或配置参数发生变化，worker 将退出。 其他退出是由外部代码中的异常抛出引起的，在这种情况下completedAbruptly 成立，
     *         这通常会导致processWorkerExit 替换此线程。
     *      2. 在运行任何任务之前，获取锁以防止任务执行时其他池中断，然后我们确保除非池停止，否则该线程没有其中断设置。
     *      3. 每个任务运行之前都会调用beforeExecute，这可能会抛出异常，在这种情况下，我们会导致线程死亡（以completedAbruptly true 中断循环）而不处理任务。
     *      4. 假设 beforeExecute 正常完成，我们运行任务，收集任何抛出的异常发送给 afterExecute。
     *         我们分别处理 RuntimeException、Error（规范保证我们捕获这两个）和任意 Throwables。
     *         因为我们不能在 Runnable.run 中重新抛出 Throwables，所以我们在出路时将它们包装在 Errors 中（到线程的 UncaughtExceptionHandler）。
     *         任何抛出的异常也会保守地导致线程死亡。
     *      5、task.run完成后，我们调用afterExecute，也有可能抛出异常，也会导致线程死亡。 根据 JLS Sec 14.20，即使 task.run 抛出，这个异常也会生效。
     * B. 异常机制的最终效果是 afterExecute 和线程的 UncaughtExceptionHandler 拥有我们所能提供的关于用户代码遇到的任何问题的准确信息。
     */
    /**
     * A.
     * Main worker run loop.  Repeatedly gets tasks from queue and
     * executes them, while coping with a number of issues:
     *
     * 1. We may start out with an initial task, in which case we
     * don't need to get the first one. Otherwise, as long as pool is
     * running, we get tasks from getTask. If it returns null then the
     * worker exits due to changed pool state or configuration
     * parameters.  Other exits result from exception throws in
     * external code, in which case completedAbruptly holds, which
     * usually leads processWorkerExit to replace this thread.
     *
     * 2. Before running any task, the lock is acquired to prevent
     * other pool interrupts while the task is executing, and then we
     * ensure that unless pool is stopping, this thread does not have
     * its interrupt set.
     *
     * 3. Each task run is preceded by a call to beforeExecute, which
     * might throw an exception, in which case we cause thread to die
     * (breaking loop with completedAbruptly true) without processing
     * the task.
     *
     * 4. Assuming beforeExecute completes normally, we run the task,
     * gathering any of its thrown exceptions to send to afterExecute.
     * We separately handle RuntimeException, Error (both of which the
     * specs guarantee that we trap) and arbitrary Throwables.
     * Because we cannot rethrow Throwables within Runnable.run, we
     * wrap them within Errors on the way out (to the thread's
     * UncaughtExceptionHandler).  Any thrown exception also
     * conservatively causes thread to die.
     *
     * 5. After task.run completes, we call afterExecute, which may
     * also throw an exception, which will also cause thread to
     * die. According to JLS Sec 14.20, this exception is the one that
     * will be in effect even if task.run throws.
     *
     * B.
     * The net effect of the exception mechanics is that afterExecute
     * and the thread's UncaughtExceptionHandler have as accurate
     * information as we can provide about any problems encountered by
     * user code.
     *
     * @param w the worker
     */
    // 使用指定工人运行任务: 先获取firstTask -> 从任务队列中获取任务 -> beforeExecute -> 运行获取到的任务 -> afterExecute -> 线程运行后的清理工作processWorkerExit
    final void runWorker(Worker w) {
        // 获取当前线程wt， 用于运行beforeExecute方法
        Thread wt = Thread.currentThread();

        // 获取要运行的初始任务task
        Runnable task = w.firstTask;
        w.firstTask = null;

        // 先释放锁, 同步器状态从-1更改为0, 允许中断当前线程
        w.unlock(); // allow interrupts 允许中断

        // worker需要突然死亡
        boolean completedAbruptly = true;
        try {
            // 执行阻塞或定时等待任务, 如果需要淘汰线程, 则使用存活时间定时获取任务, 在获取不到时则标记超时等待下一轮清空多余线程; 如果不需要淘汰线程, 则阻塞获取任务
            // 通过worker里的线程启动后, 自旋获取任务队列中的任务, 实现线程复用!!! 通过存活时间、核心线程与任务队列, 控制资源消耗
            while (task != null || (task = getTask()) != null) {
                // 如果任务不为空, 则获取worker锁, 设置当前线程为独占线程
                w.lock();

                // 如果池正在停止，确保线程被中断； 如果没有，请确保线程不被中断。 这需要在第二种情况下重新检查以在清除中断的同时处理 shutdownNow 竞争
                // If pool is stopping, ensure thread is interrupted;
                // if not, ensure thread is not interrupted.  This
                // requires a recheck in second case to deal with
                // shutdownNow race while clearing interrupt

                // 如果运行状态为停止(不接受新任务)、整理(任务终止)、终止状态(已完成), 且线程已被中断, 但当前线程中断标记位不为true, 则中断当前线程
                if ((runStateAtLeast(ctl.get(), STOP) || (Thread.interrupted() && runStateAtLeast(ctl.get(), STOP))) && !wt.isInterrupted())
                    // 中断当前线程, 如果从Thread其他实例方法调用该方法, 则会清除中断状态, 然后会收到一个{@link InterruptedException}
                    wt.interrupt();
                try {
                    // 执行任务前的钩子方法, 用于给子类实现回调
                    beforeExecute(wt, task);

                    // 运行任务
                    Throwable thrown = null;
                    try {
                        task.run();
                    } catch (RuntimeException x) {
                        thrown = x; throw x;
                    } catch (Error x) {
                        thrown = x; throw x;
                    } catch (Throwable x) {
                        thrown = x; throw new Error(x);
                    } finally {
                        // 执行任务后的钩子方法, 用于给子类实现回调, 该方法由执行任务的线程调用, t为执行任务期间抛出的Throwable
                        afterExecute(task, thrown);
                    }
                } finally {
                    task = null;

                    // 更新worker的任务完成数
                    w.completedTasks++;

                    // 释放锁, 同步器状态从1更改为0
                    w.unlock();
                }
            }

            // worker需要不突然死亡
            completedAbruptly = false;
        } finally {
            // 从工作线程集中删除线程, 并且可能会终止池或替换工作线程, 当指定的completedAbruptly为true时, 会先减少ctl工作线程数并替换工作线程
            processWorkerExit(w, completedAbruptly);
        }
    }

    // 公共构造函数和方法
    // Public constructors and methods

    // 使用给定的初始参数和默认线程工厂以及被拒绝的执行处理程序创建一个新的 {@code ThreadPoolExecutor}。
    // 使用 {@link Executors} 工厂方法之一代替此通用构造函数可能更方便。
    /**
     * Creates a new {@code ThreadPoolExecutor} with the given initial
     * parameters and default thread factory and rejected execution handler.
     * It may be more convenient to use one of the {@link Executors} factory
     * methods instead of this general purpose constructor.
     *
     * // corePoolSize 要保留在池中的线程数，即使它们处于空闲状态，除非设置了 {@code allowCoreThreadTimeOut}
     * @param corePoolSize the number of threads to keep in the pool, even
     *        if they are idle, unless {@code allowCoreThreadTimeOut} is set
     *
     * // maximumPoolSize 池中允许的最大线程数
     * @param maximumPoolSize the maximum number of threads to allow in the
     *        pool
     *
     * // keepAliveTime 当线程数大于核心时，这是多余的空闲线程在终止前等待新任务的最长时间。
     * @param keepAliveTime when the number of threads is greater than
     *        the core, this is the maximum time that excess idle threads
     *        will wait for new tasks before terminating.
     *
     * // unit {@code keepAliveTime} 参数的时间单位
     * @param unit the time unit for the {@code keepAliveTime} argument
     *
     * // workQueue 用于在执行任务之前保存任务的队列。 该队列将仅保存由 {@code execute} 方法提交的 {@code Runnable} 任务。
     * @param workQueue the queue to use for holding tasks before they are
     *        executed.  This queue will hold only the {@code Runnable}
     *        tasks submitted by the {@code execute} method.
     *
     * // 如果以下情况之一成立，则为 IllegalArgumentException：
     *      {@code corePoolSize < 0}
     *      {@code keepAliveTime < 0}
     *      {@code maximumPoolSize <= 0}
     *      {@code maximumPoolSize < corePoolSize}
     * @throws IllegalArgumentException if one of the following holds:<br>
     *         {@code corePoolSize < 0}<br>
     *         {@code keepAliveTime < 0}<br>
     *         {@code maximumPoolSize <= 0}<br>
     *         {@code maximumPoolSize < corePoolSize}
     *
     * // 如果 {@code workQueue} 为空
     * @throws NullPointerException if {@code workQueue} is null
     */
    // 指定基本线程池参数与任务队列, 使用默认的线程工厂和拒绝策略程序来创建线程池
    public ThreadPoolExecutor(int corePoolSize,
                              int maximumPoolSize,
                              long keepAliveTime,
                              TimeUnit unit,
                              BlockingQueue<Runnable> workQueue) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
             // 默认创建优先级为5的非守护线程, 线程名称为pool-[?: poolNumber]-thread-[?: threadNumber]
             Executors.defaultThreadFactory(), defaultHandler);
    }

    // 使用给定的初始参数和默认的拒绝执行处理程序创建一个新的 {@code ThreadPoolExecutor}。
    /**
     * Creates a new {@code ThreadPoolExecutor} with the given initial
     * parameters and default rejected execution handler.
     *
     * @param corePoolSize the number of threads to keep in the pool, even
     *        if they are idle, unless {@code allowCoreThreadTimeOut} is set
     * @param maximumPoolSize the maximum number of threads to allow in the
     *        pool
     * @param keepAliveTime when the number of threads is greater than
     *        the core, this is the maximum time that excess idle threads
     *        will wait for new tasks before terminating.
     * @param unit the time unit for the {@code keepAliveTime} argument
     * @param workQueue the queue to use for holding tasks before they are
     *        executed.  This queue will hold only the {@code Runnable}
     *        tasks submitted by the {@code execute} method.
     * @param threadFactory the factory to use when the executor
     *        creates a new thread
     * @throws IllegalArgumentException if one of the following holds:<br>
     *         {@code corePoolSize < 0}<br>
     *         {@code keepAliveTime < 0}<br>
     *         {@code maximumPoolSize <= 0}<br>
     *         {@code maximumPoolSize < corePoolSize}
     * @throws NullPointerException if {@code workQueue}
     *         or {@code threadFactory} is null
     */
    // 指定基本线程池参数、任务队列与线程工厂, 使用默认的拒绝策略程序来创建线程池
    public ThreadPoolExecutor(int corePoolSize,
                              int maximumPoolSize,
                              long keepAliveTime,
                              TimeUnit unit,
                              BlockingQueue<Runnable> workQueue,
                              ThreadFactory threadFactory) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
             threadFactory, defaultHandler);
    }

    // 使用给定的初始参数和默认线程工厂创建一个新的 {@code ThreadPoolExecutor}。
    /**
     * Creates a new {@code ThreadPoolExecutor} with the given initial
     * parameters and default thread factory.
     *
     * @param corePoolSize the number of threads to keep in the pool, even
     *        if they are idle, unless {@code allowCoreThreadTimeOut} is set
     * @param maximumPoolSize the maximum number of threads to allow in the
     *        pool
     * @param keepAliveTime when the number of threads is greater than
     *        the core, this is the maximum time that excess idle threads
     *        will wait for new tasks before terminating.
     * @param unit the time unit for the {@code keepAliveTime} argument
     * @param workQueue the queue to use for holding tasks before they are
     *        executed.  This queue will hold only the {@code Runnable}
     *        tasks submitted by the {@code execute} method.
     * @param handler the handler to use when execution is blocked
     *        because the thread bounds and queue capacities are reached
     * @throws IllegalArgumentException if one of the following holds:<br>
     *         {@code corePoolSize < 0}<br>
     *         {@code keepAliveTime < 0}<br>
     *         {@code maximumPoolSize <= 0}<br>
     *         {@code maximumPoolSize < corePoolSize}
     * @throws NullPointerException if {@code workQueue}
     *         or {@code handler} is null
     */
    // 指定基本线程池参数、任务队列与拒绝策略程序, 使用默认的线程工厂来创建线程池
    public ThreadPoolExecutor(int corePoolSize,
                              int maximumPoolSize,
                              long keepAliveTime,
                              TimeUnit unit,
                              BlockingQueue<Runnable> workQueue,
                              RejectedExecutionHandler handler) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
             Executors.defaultThreadFactory(), handler);
    }

    // 使用给定的初始参数创建一个新的 {@code ThreadPoolExecutor}。
    /**
     * Creates a new {@code ThreadPoolExecutor} with the given initial
     * parameters.
     *
     * @param corePoolSize the number of threads to keep in the pool, even
     *        if they are idle, unless {@code allowCoreThreadTimeOut} is set
     * @param maximumPoolSize the maximum number of threads to allow in the
     *        pool
     * @param keepAliveTime when the number of threads is greater than
     *        the core, this is the maximum time that excess idle threads
     *        will wait for new tasks before terminating.
     * @param unit the time unit for the {@code keepAliveTime} argument
     * @param workQueue the queue to use for holding tasks before they are
     *        executed.  This queue will hold only the {@code Runnable}
     *        tasks submitted by the {@code execute} method.
     * @param threadFactory the factory to use when the executor
     *        creates a new thread
     * @param handler the handler to use when execution is blocked
     *        because the thread bounds and queue capacities are reached
     * @throws IllegalArgumentException if one of the following holds:<br>
     *         {@code corePoolSize < 0}<br>
     *         {@code keepAliveTime < 0}<br>
     *         {@code maximumPoolSize <= 0}<br>
     *         {@code maximumPoolSize < corePoolSize}
     * @throws NullPointerException if {@code workQueue}
     *         or {@code threadFactory} or {@code handler} is null
     */
    // 指定基本线程池参数、任务队列、线程工厂和拒绝策略程序来创建线程池
    public ThreadPoolExecutor(int corePoolSize,
                              int maximumPoolSize,
                              long keepAliveTime,
                              TimeUnit unit,
                              BlockingQueue<Runnable> workQueue,
                              ThreadFactory threadFactory,
                              RejectedExecutionHandler handler) {
        if (corePoolSize < 0 ||
            maximumPoolSize <= 0 ||
            maximumPoolSize < corePoolSize ||
            keepAliveTime < 0)
            throw new IllegalArgumentException();
        if (workQueue == null || threadFactory == null || handler == null)
            throw new NullPointerException();
        this.corePoolSize = corePoolSize;
        this.maximumPoolSize = maximumPoolSize;
        this.workQueue = workQueue;
        this.keepAliveTime = unit.toNanos(keepAliveTime);
        this.threadFactory = threadFactory;
        this.handler = handler;
    }

    /**
     * 20210814
     * A. 在未来的某个时间执行给定的任务。 任务可以在新线程或现有池线程中执行。
     * B. 如果任务无法提交执行，要么因为此执行器已关闭或因为其容量已达到，则该任务由当前 {@code RejectedExecutionHandler} 处理。
     */
    /**
     * A.
     * Executes the given task sometime in the future.  The task
     * may execute in a new thread or in an existing pooled thread.
     *
     * B.
     * If the task cannot be submitted for execution, either because this
     * executor has been shutdown or because its capacity has been reached,
     * the task is handled by the current {@code RejectedExecutionHandler}.
     *
     * @param command the task to execute
     * @throws RejectedExecutionException at discretion of
     *         {@code RejectedExecutionHandler}, if the task
     *         cannot be accepted for execution
     * @throws NullPointerException if {@code command} is null
     */
    // 在未来的某个时间执行给定的任务, 任务可以在新线程或现有池线程中执行, 如果任务无法提交执行, 则该任务由当前 {@code RejectedExecutionHandler} 来处理
    public void execute(Runnable command) {
        if (command == null)
            throw new NullPointerException();

        /**
         * 20210814
         * 分3步进行：
         * 1. 如果运行的线程数少于 corePoolSize，请尝试使用给定命令启动一个新线程作为其第一个任务。对 addWorker 的调用以原子方式检查 runState 和 workerCount，
         *    从而通过返回 false 来防止在不应该添加线程时出现误报。
         * 2. 如果任务可以成功排队，那么我们仍然需要仔细检查是否应该添加一个线程（因为自上次检查以来现有线程已死亡）或池自进入此方法后关闭。 因此，我们重新检查状态，
         *    并在必要时在停止时回滚入队，如果没有则启动一个新线程。
         * 3. 如果我们不能排队任务，那么我们尝试添加一个新线程。 如果它失败了，我们知道我们已经关闭或饱和，因此拒绝该任务。
         */
        /*
         * Proceed in 3 steps:
         *
         * 1. If fewer than corePoolSize threads are running, try to
         * start a new thread with the given command as its first
         * task.  The call to addWorker atomically checks runState and
         * workerCount, and so prevents false alarms that would add
         * threads when it shouldn't, by returning false.
         *
         * 2. If a task can be successfully queued, then we still need
         * to double-check whether we should have added a thread
         * (because existing ones died since last checking) or that
         * the pool shut down since entry into this method. So we
         * recheck state and if necessary roll back the enqueuing if
         * stopped, or start a new thread if there are none.
         *
         * 3. If we cannot queue task, then we try to add a new
         * thread.  If it fails, we know we are shut down or saturated
         * and so reject the task.
         */
        // 获取ctl控制位c
        int c = ctl.get();

        // 根据控制位ctl获取工作线程数, 如果工作线程数小于核心线程数
        if (workerCountOf(c) < corePoolSize) {
            // 则检查是否可以根据当前池状态和给定界限（核心或最大值）添加新的工作线程, 如果添加工作线程成功, 则返回true; 如果添加工作线程失败, 则回滚工作线程并返回false
            if (addWorker(command, true))
                // 如果启动工作线程成功, 则直接返回
                return;

            // 如果启动工作线程失败, 则重新获取ctl控制位c
            c = ctl.get();
        }

        // 如果线程池仍为运行状态, 则往任务队列填充任务command
        if (isRunning(c) && workQueue.offer(command)) {
            // 如果任务填充成功, 则再获取ctl控制位recheck
            int recheck = ctl.get();

            // 如果此时线程池不为运行状态, 则从执行程序的内部队列中删除该任务，从而导致它在尚未启动时无法运行
            if (! isRunning(recheck) && remove(command))
                // 如果删除成功, 则履行任务command和当前任务执行者executor的拒绝策略
                reject(command);
            // 如果此时线程池仍为运行状态, 但工作线程数为0, 则检查是否可以根据当前池状态和给定界限（核心或最大值）添加新的工作线程, 如果添加工作线程成功, 则返回true; 如果添加工作线程失败, 则回滚工作线程并返回false
            else if (workerCountOf(recheck) == 0)
                addWorker(null, false);
        }
        // 如果线程池不为运行状态, 或者往任务队列填充任务command失败, 则检查是否可以根据当前池状态和给定界限（核心或最大值）添加新的工作线程, 如果添加工作线程成功, 则返回true; 如果添加工作线程失败, 则回滚工作线程并返回false
        else if (!addWorker(command, false))
            // 如果worker添加失败, 则履行任务command和当前任务执行者executor的拒绝策略
            reject(command);
    }

    /**
     * 20210814
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
     * @throws SecurityException {@inheritDoc}
     */
    // 启动有序关闭，其中执行先前提交的任务，但不会接受新任务, 此方法不等待先前提交的任务完成执行, 可使用 {@link #awaitTermination awaitTermination} 来做到这一点
    public void shutdown() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            checkShutdownAccess();

            // 转换线程池状态为SHUTDOWN
            advanceRunState(SHUTDOWN);

            // 中断可能正在等待任务的线程, 不止中断一个线程, 而是所有空闲的工作线程集合
            interruptIdleWorkers();

            // ScheduledThreadPoolExecutor 的钩子
            onShutdown(); // hook for ScheduledThreadPoolExecutor
        } finally {
            mainLock.unlock();
        }

        // 尝试线程池状态转换: STOP/SHUTDOWN -> 中断其中一个线程 -> TIDYING(工作线程数为0) -> TERMINATED状态 -> 通知所有等待线程池主锁Condition的线程
        tryTerminate();
    }

    /**
     * 20210814
     * A. 尝试停止所有正在执行的任务，停止等待任务的处理，并返回等待执行的任务列表。 从该方法返回时，这些任务将从任务队列中排出（移除）。
     * B. 此方法不会等待主动执行的任务终止。 使用 {@link #awaitTermination awaitTermination} 来做到这一点。
     * C. 除了尽力尝试停止处理正在执行的任务之外，没有任何保证。 此实现通过 {@link Thread#interrupt} 取消任务，因此任何未能响应中断的任务可能永远不会终止。
     */
    /**
     * A.
     * Attempts to stop all actively executing tasks, halts the
     * processing of waiting tasks, and returns a list of the tasks
     * that were awaiting execution. These tasks are drained (removed)
     * from the task queue upon return from this method.
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
     * @throws SecurityException {@inheritDoc}
     */
    // 尝试停止所有正在执行的任务, 停止等待任务的处理, 并返回等待执行的任务列表, 此方法不会等待主动执行的任务终止, 可以使用 {@link #awaitTermination awaitTermination} 来做到这一点
    public List<Runnable> shutdownNow() {
        List<Runnable> tasks;
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            checkShutdownAccess();

            // 转换线程池状态为STOP
            advanceRunState(STOP);

            // 中断所有线程，即使是活动线程。 忽略 SecurityExceptions（在这种情况下，某些线程可能保持不间断）。
            interruptWorkers();

            // 将任务队列排到一个新列表中
            tasks = drainQueue();
        } finally {
            mainLock.unlock();
        }

        // 尝试线程池状态转换: STOP/SHUTDOWN -> 中断其中一个线程 -> TIDYING(工作线程数为0) -> TERMINATED状态 -> 通知所有等待线程池主锁Condition的线程
        tryTerminate();

        // 返回所有排出的任务
        return tasks;
    }

    // 判断线程池是否已经关闭
    public boolean isShutdown() {
        return !isRunning(ctl.get());
    }

    /**
     * 20210814
     * 如果此执行程序在 {@link #shutdown} 或 {@link #shutdownNow} 之后正在终止但尚未完全终止，则返回 true。
     * 此方法可能对调试有用。 返回 {@code true} 报告关闭后足够长的时间可能表明提交的任务已忽略或抑制中断，导致此执行程序无法正常终止。
     */
    /**
     * Returns true if this executor is in the process of terminating
     * after {@link #shutdown} or {@link #shutdownNow} but has not
     * completely terminated.  This method may be useful for
     * debugging. A return of {@code true} reported a sufficient
     * period after shutdown may indicate that submitted tasks have
     * ignored or suppressed interruption, causing this executor not
     * to properly terminate.
     *
     * @return {@code true} if terminating but not yet terminated
     */
    // 如果此执行程序在 {@link #shutdown} 或 {@link #shutdownNow} 之后正在终止但尚未完全终止，则返回 true。
    public boolean isTerminating() {
        int c = ctl.get();
        return ! isRunning(c) && runStateLessThan(c, TERMINATED);
    }

    // 判断线程池是否为TERMINATED状态
    public boolean isTerminated() {
        return runStateAtLeast(ctl.get(), TERMINATED);
    }

    // 阻塞直到所有任务在关闭请求后完成执行，或发生超时，或当前线程被中断，以先发生者为准。
    public boolean awaitTermination(long timeout, TimeUnit unit)
        throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            // 开始自旋
            for (;;) {
                // 如果线程池为TERMINATED状态, 则返回true, 代表等待成功
                if (runStateAtLeast(ctl.get(), TERMINATED))
                    return true;

                // 如果发生超时, 则返回false, 代表等待失败
                if (nanos <= 0)
                    return false;

                // 如果没发生超时, 则调用主锁的Condition#awaitNanos阻塞当前线程nanos时间, 以方便其他线程执行完任务再中断它们的线程
                nanos = termination.awaitNanos(nanos);
            }
        } finally {
            mainLock.unlock();
        }
    }

    // 当不再引用此执行程序并且它没有线程时调用 {@code shutdown}。
    /**
     * Invokes {@code shutdown} when this executor is no longer
     * referenced and it has no threads.
     */
    // 线程池析构函数
    protected void finalize() {
        // 启动有序关闭，其中执行先前提交的任务，但不会接受新任务, 此方法不等待先前提交的任务完成执行, 可使用 {@link #awaitTermination awaitTermination} 来做到这一点
        shutdown();
    }

    // 设置用于创建新线程的线程工厂。
    /**
     * Sets the thread factory used to create new threads.
     *
     * @param threadFactory the new thread factory
     * @throws NullPointerException if threadFactory is null
     * @see #getThreadFactory
     */
    // 设置用于创建新线程的线程工厂
    public void setThreadFactory(ThreadFactory threadFactory) {
        if (threadFactory == null)
            throw new NullPointerException();
        this.threadFactory = threadFactory;
    }

    // 返回用于创建新线程的线程工厂。
    /**
     * Returns the thread factory used to create new threads.
     *
     * @return the current thread factory
     * @see #setThreadFactory(ThreadFactory)
     */
    // 返回用于创建新线程的线程工厂
    public ThreadFactory getThreadFactory() {
        return threadFactory;
    }

    // 为无法执行的任务设置新的处理程序。
    /**
     * Sets a new handler for unexecutable tasks.
     *
     * @param handler the new handler
     * @throws NullPointerException if handler is null
     * @see #getRejectedExecutionHandler
     */
    // 为无法执行的任务设置新的处理程序
    public void setRejectedExecutionHandler(RejectedExecutionHandler handler) {
        if (handler == null)
            throw new NullPointerException();
        this.handler = handler;
    }

    // 返回不可执行任务的当前处理程序。
    /**
     * Returns the current handler for unexecutable tasks.
     *
     * @return the current handler
     * @see #setRejectedExecutionHandler(RejectedExecutionHandler)
     */
    // 返回不可执行任务的当前处理程序
    public RejectedExecutionHandler getRejectedExecutionHandler() {
        return handler;
    }

    // 设置核心线程数。 这会覆盖构造函数中设置的任何值。 如果新值小于当前值，多余的现有线程将在下一次空闲时终止。 如果更大，将在需要时启动新线程来执行任何排队的任务。
    /**
     * Sets the core number of threads.  This overrides any value set
     * in the constructor.  If the new value is smaller than the
     * current value, excess existing threads will be terminated when
     * they next become idle.  If larger, new threads will, if needed,
     * be started to execute any queued tasks.
     *
     * @param corePoolSize the new core size
     * @throws IllegalArgumentException if {@code corePoolSize < 0}
     * @see #getCorePoolSize
     */
    // 设置核心线程数, 这会覆盖构造函数中设置的任何值, 如果属于线程减少, 则中断空闲线程; 如果属于线程增加, 则创建增量个数或者任务队列实际长度数的线程
    public void setCorePoolSize(int corePoolSize) {
        if (corePoolSize < 0)
            throw new IllegalArgumentException();

        // 获取增量delta
        int delta = corePoolSize - this.corePoolSize;
        this.corePoolSize = corePoolSize;

        // 如果属于线程减少, 则中断可能正在等待任务的线程, 不止中断一个线程, 而是所有空闲的工作线程集合
        if (workerCountOf(ctl.get()) > corePoolSize)
            interruptIdleWorkers();
        // 如果属于线程增加
        else if (delta > 0) {
            // 我们真的不知道“需要”多少新线程。作为一种启发式方法，预先启动足够多的新工作程序（最多达到新的核心大小）来处理队列中的当前任务数，
            // 但如果在执行此操作时队列变空，则停止。
            // We don't really know how many new threads are "needed".
            // As a heuristic, prestart enough new workers (up to new
            // core size) to handle the current number of tasks in
            // queue, but stop if queue becomes empty while doing so.
            // 则从增量delta和任务队列实际大小中取最小值k
            int k = Math.min(delta, workQueue.size());

            // 创建k个工作线程, 直到k为0, 或者线程添加失败, 或者任务队列为空
            while (k-- > 0 && addWorker(null, true)) {
                if (workQueue.isEmpty())
                    break;
            }
        }
    }

    // 返回核心线程数。
    /**
     * Returns the core number of threads.
     *
     * @return the core number of threads
     * @see #setCorePoolSize
     */
    // 返回核心线程数
    public int getCorePoolSize() {
        return corePoolSize;
    }

    // 启动一个核心线程，使其空闲等待工作。 这将覆盖仅在执行新任务时启动核心线程的默认策略。 如果所有核心线程都已启动，此方法将返回 {@code false}。
    /**
     * Starts a core thread, causing it to idly wait for work. This
     * overrides the default policy of starting core threads only when
     * new tasks are executed. This method will return {@code false}
     * if all core threads have already been started.
     *
     * @return {@code true} if a thread was started
     */
    // 启动一个核心线程，使其空闲等待工作, 这将覆盖仅在执行新任务时才启动核心线程的默认策略
    public boolean prestartCoreThread() {
        return workerCountOf(ctl.get()) < corePoolSize && addWorker(null, true);
    }

    // 与 prestartCoreThread 相同，除了设置即使 corePoolSize 为 0 也至少启动一个线程。
    /**
     * Same as prestartCoreThread except arranges that at least one
     * thread is started even if corePoolSize is 0.
     */
    // ScheduledThreadPoolExecutor调用, 无论如何(即使核心线程数为0)也会启动一个核心线程, 使其空闲等待工作, 这将覆盖仅在执行新任务时才启动核心线程的默认策略
    void ensurePrestart() {
        int wc = workerCountOf(ctl.get());
        if (wc < corePoolSize)
            addWorker(null, true);
        else if (wc == 0)
            addWorker(null, false);
    }

    // 启动所有核心线程，导致它们空闲等待工作。 这将覆盖仅在执行新任务时启动核心线程的默认策略。
    /**
     * Starts all core threads, causing them to idly wait for work. This
     * overrides the default policy of starting core threads only when
     * new tasks are executed.
     *
     * @return the number of threads started
     */
    // 启动所有核心线程，导致它们空闲等待工作, 这将覆盖仅在执行新任务时才启动核心线程的默认策略
    public int prestartAllCoreThreads() {
        int n = 0;
        while (addWorker(null, true))
            ++n;
        return n;
    }

    /**
     * 20210814
     * 如果此池允许核心线程超时并在keepAlive时间内没有任务到达时终止，则返回true，并在新任务到达时根据需要进行替换。
     * 如果为真，则适用于非核心线程的相同保活策略也适用于核心线程。 当为 false（默认值）时，核心线程永远不会因缺少传入任务而终止。
     */
    /**
     * Returns true if this pool allows core threads to time out and
     * terminate if no tasks arrive within the keepAlive time, being
     * replaced if needed when new tasks arrive. When true, the same
     * keep-alive policy applying to non-core threads applies also to
     * core threads. When false (the default), core threads are never
     * terminated due to lack of incoming tasks.
     *
     * @return {@code true} if core threads are allowed to time out,
     *         else {@code false}
     *
     * @since 1.6
     */
    // 判断核心线程是否会超时
    public boolean allowsCoreThreadTimeOut() {
        return allowCoreThreadTimeOut;
    }

    /**
     * 20210814
     * 设置策略，如果在保持活动时间内没有任务到达，核心线程是否可以超时和终止，并在新任务到达时根据需要进行替换。
     * 当为 false 时，核心线程永远不会因缺少传入任务而终止。 如果为真，则适用于非核心线程的相同保活策略也适用于核心线程。
     * 为避免不断更换线程，设置 {@code true} 时保持活动时间必须大于零。 通常应在主动使用池之前调用此方法。
     */
    /**
     * Sets the policy governing whether core threads may time out and
     * terminate if no tasks arrive within the keep-alive time, being
     * replaced if needed when new tasks arrive. When false, core
     * threads are never terminated due to lack of incoming
     * tasks. When true, the same keep-alive policy applying to
     * non-core threads applies also to core threads. To avoid
     * continual thread replacement, the keep-alive time must be
     * greater than zero when setting {@code true}. This method
     * should in general be called before the pool is actively used.
     *
     * @param value {@code true} if should time out, else {@code false}
     * @throws IllegalArgumentException if value is {@code true}
     *         and the current keep-alive time is not greater than zero
     *
     * @since 1.6
     */
    // 是否允许核心线程过期, 如果允许核心线程过期, 则空闲线程存活时间必须大于0, 通常应在主动使用池之前调用此方法
    public void allowCoreThreadTimeOut(boolean value) {
        // 如果允许核心线程过期, 则空闲线程存活时间必须大于0
        if (value && keepAliveTime <= 0)
            throw new IllegalArgumentException("Core threads must have nonzero keep alive times");

        // 更新允许标志, 如果允许过期, 则中断可能正在等待任务的线程, 不止中断一个线程, 而是所有空闲的工作线程集合
        if (value != allowCoreThreadTimeOut) {
            allowCoreThreadTimeOut = value;
            if (value)
                interruptIdleWorkers();
        }
    }

    // 设置允许的最大线程数。这会覆盖构造函数中设置的任何值。如果新值小于当前值，多余的现有线程将在下一次空闲时终止。
    /**
     * Sets the maximum allowed number of threads. This overrides any
     * value set in the constructor. If the new value is smaller than
     * the current value, excess existing threads will be
     * terminated when they next become idle.
     *
     * @param maximumPoolSize the new maximum
     * @throws IllegalArgumentException if the new maximum is
     *         less than or equal to zero, or
     *         less than the {@linkplain #getCorePoolSize core pool size}
     * @see #getMaximumPoolSize
     */
    // 设置允许的最大线程数, 这会覆盖构造函数中设置的任何值, 如果新值小于当前值, 则多余的现有线程将在下一次空闲时终止
    public void setMaximumPoolSize(int maximumPoolSize) {
        if (maximumPoolSize <= 0 || maximumPoolSize < corePoolSize)
            throw new IllegalArgumentException();
        this.maximumPoolSize = maximumPoolSize;

        // 如果属于线程减少, 则中断可能正在等待任务的线程, 不止中断一个线程, 而是所有空闲的工作线程集合
        if (workerCountOf(ctl.get()) > maximumPoolSize)
            interruptIdleWorkers();
    }

    // 返回允许的最大线程数。
    /**
     * Returns the maximum allowed number of threads.
     *
     * @return the maximum allowed number of threads
     * @see #setMaximumPoolSize
     */
    // 返回允许的最大线程数
    public int getMaximumPoolSize() {
        return maximumPoolSize;
    }

    // 设置线程在终止之前可以保持空闲的时间限制。如果当前池中的线程数超过核心数量，则在等待这段时间而不处理任务后，将终止多余的线程。这会覆盖构造函数中设置的任何值。
    /**
     * Sets the time limit for which threads may remain idle before
     * being terminated.  If there are more than the core number of
     * threads currently in the pool, after waiting this amount of
     * time without processing a task, excess threads will be
     * terminated.  This overrides any value set in the constructor.
     *
     * @param time the time to wait.  A time value of zero will cause
     *        excess threads to terminate immediately after executing tasks.
     * @param unit the time unit of the {@code time} argument
     * @throws IllegalArgumentException if {@code time} less than zero or
     *         if {@code time} is zero and {@code allowsCoreThreadTimeOut}
     * @see #getKeepAliveTime(TimeUnit)
     */
    // 设置空闲线程存活时间, 即如果线程池中的线程数超过核心数, 在等待存活时间得不到任务处理, 则会终止这些多余的线程
    public void setKeepAliveTime(long time, TimeUnit unit) {
        if (time < 0)
            throw new IllegalArgumentException();
        if (time == 0 && allowsCoreThreadTimeOut())
            throw new IllegalArgumentException("Core threads must have nonzero keep alive times");
        long keepAliveTime = unit.toNanos(time);
        long delta = keepAliveTime - this.keepAliveTime;
        this.keepAliveTime = keepAliveTime;

        // 如果属于存活时间减少, 则中断可能正在等待任务的线程, 不止中断一个线程, 而是所有空闲的工作线程集合
        if (delta < 0)
            interruptIdleWorkers();
    }

    // 返回线程保持活动时间，这是超过核心池大小的线程在终止之前可能保持空闲的时间量。
    /**
     * Returns the thread keep-alive time, which is the amount of time
     * that threads in excess of the core pool size may remain
     * idle before being terminated.
     *
     * @param unit the desired time unit of the result
     * @return the time limit
     * @see #setKeepAliveTime(long, TimeUnit)
     */
    // 返回线程保持活动时间, 这是超过核心池大小的线程在终止之前可能保持空闲的时间量
    public long getKeepAliveTime(TimeUnit unit) {
        return unit.convert(keepAliveTime, TimeUnit.NANOSECONDS);
    }

    // 用户级队列实用程序
    /* User-level queue utilities */

    // 返回此执行程序使用的任务队列。访问任务队列主要用于调试和监控。此队列可能正在使用中。检索任务队列不会阻止排队的任务执行。
    /**
     * Returns the task queue used by this executor. Access to the
     * task queue is intended primarily for debugging and monitoring.
     * This queue may be in active use.  Retrieving the task queue
     * does not prevent queued tasks from executing.
     *
     * @return the task queue
     */
    // 获取线程池所使用的任务队列, 该方法主要用于调试和监控
    public BlockingQueue<Runnable> getQueue() {
        return workQueue;
    }

    /**
     * 20210814
     * A. 如果此任务存在，则从执行程序的内部队列中删除该任务，从而导致它在尚未启动时无法运行。
     * B. 此方法可用作取消方案的一部分。 在放入内部队列之前，它可能无法删除已转换为其他形式的任务。
     * 例如，使用 {@code submit} 输入的任务可能会转换为保持 {@code Future} 状态的表单。 但是，在这种情况下，可以使用方法 {@link #purge} 删除那些已取消的期货。
     */
    /**
     * A.
     * Removes this task from the executor's internal queue if it is
     * present, thus causing it not to be run if it has not already
     * started.
     *
     * B.
     * <p>This method may be useful as one part of a cancellation
     * scheme.  It may fail to remove tasks that have been converted
     * into other forms before being placed on the internal queue. For
     * example, a task entered using {@code submit} might be
     * converted into a form that maintains {@code Future} status.
     * However, in such cases, method {@link #purge} may be used to
     * remove those Futures that have been cancelled.
     *
     * @param task the task to remove
     * @return {@code true} if the task was removed
     */
    // 如果此任务存在，则从执行程序的内部队列中删除该任务，从而导致它在尚未启动时无法运行
    public boolean remove(Runnable task) {
        boolean removed = workQueue.remove(task);

        // 尝试线程池状态转换: STOP/SHUTDOWN -> 中断其中一个线程 -> TIDYING(工作线程数为0) -> TERMINATED状态 -> 通知所有等待线程池主锁Condition的线程
        tryTerminate(); // In case SHUTDOWN and now empty // 如果 SHUTDOWN 现在为空

        // 返回删除结果
        return removed;
    }

    /**
     * 20210814
     * 尝试从工作队列中删除所有已取消的 {@link Future} 任务。 此方法可用作存储回收操作，对功能没有其他影响。
     * 取消的任务永远不会执行，但可能会在工作队列中累积，直到工作线程可以主动删除它们。
     * 现在调用此方法会尝试删除它们。 但是，这种方法可能会在存在其他线程干扰的情况下无法删除任务。
     */
    /**
     * Tries to remove from the work queue all {@link Future}
     * tasks that have been cancelled. This method can be useful as a
     * storage reclamation operation, that has no other impact on
     * functionality. Cancelled tasks are never executed, but may
     * accumulate in work queues until worker threads can actively
     * remove them. Invoking this method instead tries to remove them now.
     * However, this method may fail to remove tasks in
     * the presence of interference by other threads.
     */
    // 尝试从工作队列中删除所有已取消的 {@link Future} 任务, 取消的任务永远不会执行, 但可能会在工作队列中累积, 直到工作线程可以主动删除它们
    public void purge() {
        final BlockingQueue<Runnable> q = workQueue;
        try {
            // 迭代任务队列, 如果Future标记为已取消, 则从任务队列中移除掉
            Iterator<Runnable> it = q.iterator();
            while (it.hasNext()) {
                Runnable r = it.next();
                if (r instanceof Future<?> && ((Future<?>)r).isCancelled())
                    it.remove();
            }
        } catch (ConcurrentModificationException fallThrough) {
            // 如果我们在遍历过程中遇到干扰，请走慢路径。为遍历制作副本并为取消的条目调用remove 。慢路径更有可能是 O(N*N)。
            // Take slow path if we encounter interference during traversal.
            // Make copy for traversal and call remove for cancelled entries.
            // The slow path is more likely to be O(N*N).
            for (Object r : q.toArray())
                if (r instanceof Future<?> && ((Future<?>)r).isCancelled())
                    q.remove(r);
        }

        // 尝试线程池状态转换: STOP/SHUTDOWN -> 中断其中一个线程 -> TIDYING(工作线程数为0) -> TERMINATED状态 -> 通知所有等待线程池主锁Condition的线程
        tryTerminate(); // In case SHUTDOWN and now empty // 如果 SHUTDOWN 现在为空
    }

    // 统计数据
    /* Statistics */

    // 返回池中的当前线程数。
    /**
     * Returns the current number of threads in the pool.
     *
     * @return the number of threads
     */
    // 返回池中的当前线程数, 如果线程池状态为TIDYING或者TERMINATED, 则返回0; 否则返回工作线程集合的实际大小
    public int getPoolSize() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            // 删除 isTerminated() && getPoolSize() > 0 的罕见和令人惊讶的可能性
            // Remove rare and surprising possibility of
            // isTerminated() && getPoolSize() > 0

            // 如果线程池状态为TIDYING或者TERMINATED, 则返回0; 否则返回工作线程集合的实际大小
            return runStateAtLeast(ctl.get(), TIDYING) ? 0 : workers.size();
        } finally {
            mainLock.unlock();
        }
    }

    // 返回正在积极执行任务的线程的大致数量。
    /**
     * Returns the approximate number of threads that are actively
     * executing tasks.
     *
     * @return the number of threads
     */
    // 返回正在积极执行任务的线程的大致数量, 通过判断工人worker是否持有锁来判断
    public int getActiveCount() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            int n = 0;
            for (Worker w : workers)
                if (w.isLocked())
                    ++n;
            return n;
        } finally {
            mainLock.unlock();
        }
    }

    // 返回曾经同时进入池中的最大线程数。
    /**
     * Returns the largest number of threads that have ever
     * simultaneously been in the pool.
     *
     * @return the number of threads
     */
    // 返回到目前为止, 线程池中出现的最大线程数
    public int getLargestPoolSize() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            return largestPoolSize;
        } finally {
            mainLock.unlock();
        }
    }

    // 返回已安排执行的大致任务总数。 由于任务和线程的状态在计算过程中可能会动态变化，因此返回值只是一个近似值。
    /**
     * Returns the approximate total number of tasks that have ever been
     * scheduled for execution. Because the states of tasks and
     * threads may change dynamically during computation, the returned
     * value is only an approximation.
     *
     * @return the number of tasks
     */
    // 返回已安排执行的大致任务总数 = 加锁的工人worker数 + 任务队列的实际大小
    public long getTaskCount() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            long n = completedTaskCount;
            for (Worker w : workers) {
                n += w.completedTasks;
                if (w.isLocked())
                    ++n;
            }
            return n + workQueue.size();
        } finally {
            mainLock.unlock();
        }
    }

    // 返回已完成执行的大致任务总数。 由于任务和线程的状态在计算过程中可能会动态变化，因此返回值只是一个近似值，但在连续调用中永远不会减少。
    /**
     * Returns the approximate total number of tasks that have
     * completed execution. Because the states of tasks and threads
     * may change dynamically during computation, the returned value
     * is only an approximation, but one that does not ever decrease
     * across successive calls.
     *
     * @return the number of tasks
     */
    // 返回已完成执行的大致任务总数, 累加每个worker完成的任务数
    public long getCompletedTaskCount() {
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            long n = completedTaskCount;
            for (Worker w : workers)
                n += w.completedTasks;
            return n;
        } finally {
            mainLock.unlock();
        }
    }

    // 返回标识此池及其状态的字符串，包括运行状态的指示以及估计的工作程序和任务计数。
    /**
     * Returns a string identifying this pool, as well as its state,
     * including indications of run state and estimated worker and
     * task counts.
     *
     * // 标识此池及其状态的字符串
     * @return a string identifying this pool, as well as its state
     */
    // 返回标识此池及其状态的字符串, 包括运行状态、总线程数、正在工作的线程数、总任务数、已完成的任务数
    public String toString() {
        long ncompleted;
        int nworkers, nactive;
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            ncompleted = completedTaskCount;
            nactive = 0;
            nworkers = workers.size();
            for (Worker w : workers) {
                ncompleted += w.completedTasks;
                if (w.isLocked())
                    ++nactive;
            }
        } finally {
            mainLock.unlock();
        }
        int c = ctl.get();
        String rs = (runStateLessThan(c, SHUTDOWN) ? "Running" :
                     (runStateAtLeast(c, TERMINATED) ? "Terminated" :
                      "Shutting down"));
        return super.toString() +
            "[" + rs +
            ", pool size = " + nworkers +
            ", active threads = " + nactive +
            ", queued tasks = " + workQueue.size() +
            ", completed tasks = " + ncompleted +
            "]";
    }

    // 延长挂钩
    /* Extension hooks */

    /**
     * 20210814
     * A. 在给定线程中执行给定 Runnable 之前调用的方法。 此方法由将执行任务 {@code r} 的线程 {@code t} 调用，并可用于重新初始化 ThreadLocals，或执行日志记录。
     * B. 这个实现什么都不做，但可以在子类中自定义。 注意：为了正确嵌套多个覆盖，子类通常应该在此方法的末尾调用 {@code super.beforeExecute}。
     */
    /**
     * A.
     * Method invoked prior to executing the given Runnable in the
     * given thread.  This method is invoked by thread {@code t} that
     * will execute task {@code r}, and may be used to re-initialize
     * ThreadLocals, or to perform logging.
     *
     * B.
     * <p>This implementation does nothing, but may be customized in
     * subclasses. Note: To properly nest multiple overridings, subclasses
     * should generally invoke {@code super.beforeExecute} at the end of
     * this method.
     *
     * // 将运行任务 {@code r} 的线程
     * @param t the thread that will run task {@code r}
     *
     * // 将要执行的任务
     * @param r the task that will be executed
     */
    // 执行任务前的钩子方法, 用于给子类实现回调
    protected void beforeExecute(Thread t, Runnable r) { }

    /**
     * 20210814
     * A. 在给定 Runnable 的执行完成后调用的方法。 该方法由执行任务的线程调用。 如果非空，则 Throwable 是导致执行突然终止的未捕获的 {@code RuntimeException} 或 {@code Error}。
     * B. 这个实现什么都不做，但可以在子类中自定义。 注意：要正确嵌套多个覆盖，子类通常应在此方法的开头调用 {@code super.afterExecute}。
     * C. 注意：当动作被明确地或通过诸如{@code submit}之类的方法包含在任务中（例如{@link FutureTask}）时，这些任务对象会捕获并维护计算异常，因此它们不会导致突然终止，
     *    并且内部异常不会传递给此方法。 如果您想在此方法中捕获这两种失败，您可以进一步探测此类情况，如在此示例子类中，如果任务已中止，它将打印直接原因或底层异常：
     * {@code
     * class ExtendedExecutor extends ThreadPoolExecutor {
     *   // ...
     *   protected void afterExecute(Runnable r, Throwable t) {
     *     super.afterExecute(r, t);
     *     if (t == null && r instanceof Future) {
     *       try {
     *         Object result = ((Future) r).get();
     *       } catch (CancellationException ce) {
     *           t = ce;
     *       } catch (ExecutionException ee) {
     *           t = ee.getCause();
     *       } catch (InterruptedException ie) {
     *           Thread.currentThread().interrupt(); // ignore/reset
     *       }
     *     }
     *     if (t != null)
     *       System.out.println(t);
     *   }
     * }}
     */
    /**
     * A.
     * Method invoked upon completion of execution of the given Runnable.
     * This method is invoked by the thread that executed the task. If
     * non-null, the Throwable is the uncaught {@code RuntimeException}
     * or {@code Error} that caused execution to terminate abruptly.
     *
     * B.
     * <p>This implementation does nothing, but may be customized in
     * subclasses. Note: To properly nest multiple overridings, subclasses
     * should generally invoke {@code super.afterExecute} at the
     * beginning of this method.
     *
     * C.
     * <p><b>Note:</b> When actions are enclosed in tasks (such as
     * {@link FutureTask}) either explicitly or via methods such as
     * {@code submit}, these task objects catch and maintain
     * computational exceptions, and so they do not cause abrupt
     * termination, and the internal exceptions are <em>not</em>
     * passed to this method. If you would like to trap both kinds of
     * failures in this method, you can further probe for such cases,
     * as in this sample subclass that prints either the direct cause
     * or the underlying exception if a task has been aborted:
     *
     *  <pre> {@code
     * class ExtendedExecutor extends ThreadPoolExecutor {
     *   // ...
     *   protected void afterExecute(Runnable r, Throwable t) {
     *     super.afterExecute(r, t);
     *     if (t == null && r instanceof Future<?>) {
     *       try {
     *         Object result = ((Future<?>) r).get();
     *       } catch (CancellationException ce) {
     *           t = ce;
     *       } catch (ExecutionException ee) {
     *           t = ee.getCause();
     *       } catch (InterruptedException ie) {
     *           Thread.currentThread().interrupt(); // ignore/reset
     *       }
     *     }
     *     if (t != null)
     *       System.out.println(t);
     *   }
     * }}</pre>
     *
     * // 已完成的 runnable
     * @param r the runnable that has completed
     *
     * // t 导致终止的异常，如果执行正常完成则为 null
     * @param t the exception that caused termination, or null if
     * execution completed normally
     */
    // 执行任务后的钩子方法, 用于给子类实现回调, 该方法由执行任务的线程调用, t为执行任务期间抛出的Throwable
    protected void afterExecute(Runnable r, Throwable t) { }

    // 当 Executor 终止时调用的方法。 默认实现什么都不做。 注意：要正确嵌套多个覆盖，子类通常应在此方法中调用 {@code super.terminated}。
    /**
     * Method invoked when the Executor has terminated.  Default
     * implementation does nothing. Note: To properly nest multiple
     * overridings, subclasses should generally invoke
     * {@code super.terminated} within this method.
     */
    // tryTerminate方法中, 尝试将线程池状态TIDYING->TERMINATED转换的中间钩子方法
    protected void terminated() { }

    // 预定义的 RejectedExecutionHandlers
    /* Predefined RejectedExecutionHandlers */

    // 被拒绝任务的处理程序，它直接在 {@code execute} 方法的调用线程中运行被拒绝的任务，除非执行程序已关闭，在这种情况下任务将被丢弃。
    /**
     * A handler for rejected tasks that runs the rejected task
     * directly in the calling thread of the {@code execute} method,
     * unless the executor has been shut down, in which case the task
     * is discarded.
     */
    // 拒绝策略一: 直接在 {@code execute} 方法的调用线程中运行被拒绝的任务, 如果执行程序已关闭, 则会丢弃任务
    public static class CallerRunsPolicy implements RejectedExecutionHandler {

        /**
         * Creates a {@code CallerRunsPolicy}.
         */
        public CallerRunsPolicy() { }

        // 在调用者的线程中执行任务 r，除非执行器已关闭，在这种情况下任务将被丢弃。
        /**
         * Executes task r in the caller's thread, unless the executor
         * has been shut down, in which case the task is discarded.
         *
         * @param r the runnable task requested to be executed
         * @param e the executor attempting to execute this task
         */
        // 履行拒绝策略: 如果没有线程池没有关闭, 则使用当前线程直接运行任务r
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
            if (!e.isShutdown()) {
                r.run();
            }
        }
    }

    // 抛出 {@code RejectedExecutionException} 的被拒绝任务的处理程序。
    /**
     * A handler for rejected tasks that throws a
     * {@code RejectedExecutionException}.
     */
    // 拒绝策略二: 抛出RejectedExecutionException异常, 从而拒绝任务的执行
    public static class AbortPolicy implements RejectedExecutionHandler {
        /**
         * Creates an {@code AbortPolicy}.
         */
        public AbortPolicy() { }

        /**
         * Always throws RejectedExecutionException.
         *
         * @param r the runnable task requested to be executed
         * @param e the executor attempting to execute this task
         * @throws RejectedExecutionException always
         */
        // 总是抛出 RejectedExecutionException
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
            throw new RejectedExecutionException("Task " + r.toString() +
                                                 " rejected from " +
                                                 e.toString());
        }
    }

    // 被拒绝任务的处理程序，它默默地丢弃被拒绝的任务。
    /**
     * A handler for rejected tasks that silently discards the
     * rejected task.
     */
    // 拒绝策略三: 不做任何事情, 丢弃所有的任务
    public static class DiscardPolicy implements RejectedExecutionHandler {
        /**
         * Creates a {@code DiscardPolicy}.
         */
        public DiscardPolicy() { }

        /**
         * Does nothing, which has the effect of discarding task r.
         *
         * @param r the runnable task requested to be executed
         * @param e the executor attempting to execute this task
         */
        // 不做任何事情，具有丢弃任务 r 的效果。
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
        }
    }

    // 被拒绝任务的处理程序，它会丢弃最旧的未处理请求，然后重试 {@code execute}，除非执行程序关闭，在这种情况下任务将被丢弃。
    /**
     * A handler for rejected tasks that discards the oldest unhandled
     * request and then retries {@code execute}, unless the executor
     * is shut down, in which case the task is discarded.
     */
    // 拒绝策略四: 丢弃任何任务最旧的未处理请求, 然后重试 {@code execute}, 如果执行程序已关闭, 则会丢弃任务
    public static class DiscardOldestPolicy implements RejectedExecutionHandler {
        /**
         * Creates a {@code DiscardOldestPolicy} for the given executor.
         */
        public DiscardOldestPolicy() { }

        // 获取并忽略执行程序将执行的下一个任务，如果一个任务立即可用，然后重试任务 r 的执行，除非执行程序关闭，在这种情况下，任务 r 被丢弃。
        /**
         * Obtains and ignores the next task that the executor
         * would otherwise execute, if one is immediately available,
         * and then retries execution of task r, unless the executor
         * is shut down, in which case task r is instead discarded.
         *
         * @param r the runnable task requested to be executed
         * @param e the executor attempting to execute this task
         */
        // 如果线程池没被关闭, 则删除队头任务, 删除后重新执行execute方法
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
            if (!e.isShutdown()) {
                e.getQueue().poll();
                e.execute(r);
            }
        }
    }
}
