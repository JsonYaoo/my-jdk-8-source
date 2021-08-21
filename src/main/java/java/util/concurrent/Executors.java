/*
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util.concurrent;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import java.security.PrivilegedActionException;
import java.security.AccessControlException;
import sun.security.util.SecurityConstants;

/**
 * 20210815
 * 此包中定义的 {@link Executor}、{@link ExecutorService}、{@link ScheduledExecutorService}、{@link ThreadFactory} 和 {@link Callable} 类的工厂和实用方法。
 * 此类支持以下类型的方法：
 *      a. 创建和返回一个 {@link ExecutorService} 的方法设置了常用的配置设置。
 *      b. 使用常用配置设置创建和返回 {@link ScheduledExecutorService} 的方法。
 *      c. 创建和返回“包装的”ExecutorService 的方法，通过使特定于实现的方法不可访问来禁用重新配置。
 *      d. 创建并返回 {@link ThreadFactory} 的方法，该 {@link ThreadFactory} 将新创建的线程设置为已知状态。
 *      e. 从其他类似闭包的形式创建和返回 {@link Callable} 的方法，因此它们可以用于需要 {@code Callable} 的执行方法中。
 */

/**
 * Factory and utility methods for {@link Executor}, {@link
 * ExecutorService}, {@link ScheduledExecutorService}, {@link
 * ThreadFactory}, and {@link Callable} classes defined in this
 * package. This class supports the following kinds of methods:
 *
 * <ul>
 *   <li> Methods that create and return an {@link ExecutorService}
 *        set up with commonly useful configuration settings.
 *   <li> Methods that create and return a {@link ScheduledExecutorService}
 *        set up with commonly useful configuration settings.
 *   <li> Methods that create and return a "wrapped" ExecutorService, that
 *        disables reconfiguration by making implementation-specific methods
 *        inaccessible.
 *   <li> Methods that create and return a {@link ThreadFactory}
 *        that sets newly created threads to a known state.
 *   <li> Methods that create and return a {@link Callable}
 *        out of other closure-like forms, so they can be used
 *        in execution methods requiring {@code Callable}.
 * </ul>
 *
 * @since 1.5
 * @author Doug Lea
 */
public class Executors {

    /**
     * 20210815
     * 创建一个线程池，该线程池重用固定数量的线程在共享的无界队列中运行。
     * 在任何时候，最多 {@code nThreads} 个线程将是活动的处理任务。 如果在所有线程都处于活动状态时提交了其他任务，它们将在队列中等待，直到有线程可用。
     * 如果任何线程在关闭前的执行过程中因失败而终止，则在需要执行后续任务时，将有一个新线程取而代之。
     * 池中的线程将一直存在，直到明确{@link ExecutorService#shutdown shutdown}。
     */
    /**
     * Creates a thread pool that reuses a fixed number of threads
     * operating off a shared unbounded queue.  At any point, at most
     * {@code nThreads} threads will be active processing tasks.
     * If additional tasks are submitted when all threads are active,
     * they will wait in the queue until a thread is available.
     * If any thread terminates due to a failure during execution
     * prior to shutdown, a new one will take its place if needed to
     * execute subsequent tasks.  The threads in the pool will exist
     * until it is explicitly {@link ExecutorService#shutdown shutdown}.
     *
     * @param nThreads the number of threads in the pool
     * @return the newly created thread pool
     * @throws IllegalArgumentException if {@code nThreads <= 0}
     */
    // 创建一个固定线程数量、无界任务队列、默认线程工厂的线程池, 该实例强转后可以再次修改线程池参数
    public static ExecutorService newFixedThreadPool(int nThreads) {
        return new ThreadPoolExecutor(nThreads, nThreads,
                                      0L, TimeUnit.MILLISECONDS,
                                      new LinkedBlockingQueue<Runnable>());
    }

    /**
     * 20210815
     * 创建一个线程池，维护足够的线程以支持给定的并行度级别，并且可以使用多个队列来减少争用。 并行度级别对应于主动参与或可用于参与任务处理的最大线程数。
     * 线程的实际数量可能会动态增长和收缩。工作窃取池不保证提交任务的执行顺序。
     */
    /**
     * Creates a thread pool that maintains enough threads to support
     * the given parallelism level, and may use multiple queues to
     * reduce contention. The parallelism level corresponds to the
     * maximum number of threads actively engaged in, or available to
     * engage in, task processing. The actual number of threads may
     * grow and shrink dynamically. A work-stealing pool makes no
     * guarantees about the order in which submitted tasks are
     * executed.
     *
     * @param parallelism the targeted parallelism level
     * @return the newly created thread pool
     * @throws IllegalArgumentException if {@code parallelism <= 0}
     * @since 1.8
     */
    // 创建一个线程池，维护足够的线程以支持给定的并行度级别，并且可以使用多个队列来减少争用。
    public static ExecutorService newWorkStealingPool(int parallelism) {
        return new ForkJoinPool
            (parallelism,
             ForkJoinPool.defaultForkJoinWorkerThreadFactory,
             null, true);
    }

    // 使用所有 {@link Runtime#availableProcessors 可用处理器} 作为其目标并行级别来创建窃取工作的线程池。
    /**
     * Creates a work-stealing thread pool using all
     * {@link Runtime#availableProcessors available processors}
     * as its target parallelism level.
     *
     * @return the newly created thread pool
     * @see #newWorkStealingPool(int)
     * @since 1.8
     */
    // 使用所有 {@link Runtime#availableProcessors 可用处理器} 作为其目标并行级别来创建窃取工作的线程池。
    public static ExecutorService newWorkStealingPool() {
        return new ForkJoinPool
            (Runtime.getRuntime().availableProcessors(),
             ForkJoinPool.defaultForkJoinWorkerThreadFactory,
             null, true);
    }

    /**
     * 20210815
     * 创建一个线程池，该线程池重用固定数量的线程，这些线程在共享的无界队列中运行，并在需要时使用提供的 ThreadFactory 创建新线程。
     * 在任何时候，最多 {@code nThreads} 个线程将是活动的处理任务。 如果在所有线程都处于活动状态时提交了其他任务，它们将在队列中等待，直到有线程可用。
     * 如果任何线程在关闭前的执行过程中因失败而终止，则在需要执行后续任务时，将有一个新线程取而代之。
     * 池中的线程将一直存在，直到明确{@link ExecutorService#shutdown shutdown}。
     */
    /**
     * Creates a thread pool that reuses a fixed number of threads
     * operating off a shared unbounded queue, using the provided
     * ThreadFactory to create new threads when needed.  At any point,
     * at most {@code nThreads} threads will be active processing
     * tasks.  If additional tasks are submitted when all threads are
     * active, they will wait in the queue until a thread is
     * available.  If any thread terminates due to a failure during
     * execution prior to shutdown, a new one will take its place if
     * needed to execute subsequent tasks.  The threads in the pool will
     * exist until it is explicitly {@link ExecutorService#shutdown
     * shutdown}.
     *
     * @param nThreads the number of threads in the pool
     * @param threadFactory the factory to use when creating new threads
     * @return the newly created thread pool
     * @throws NullPointerException if threadFactory is null
     * @throws IllegalArgumentException if {@code nThreads <= 0}
     */
    // 创建一个固定线程数量、无界任务队列、指定线程工厂的线程池, 该实例强转后可以再次修改线程池参数
    public static ExecutorService newFixedThreadPool(int nThreads, ThreadFactory threadFactory) {
        return new ThreadPoolExecutor(nThreads, nThreads,
                                      0L, TimeUnit.MILLISECONDS,
                                      new LinkedBlockingQueue<Runnable>(),
                                      threadFactory);
    }

    /**
     * 20210815
     * 创建一个 Executor，它使用单个工作线程在无界队列中运行。（但是请注意，如果这个单线程在关闭之前由于执行失败而终止，如果需要执行后续任务，一个新线程将取代它。）
     * 保证任务按顺序执行，并且不会超过一个任务处于活动状态 在任何给定的时间。
     * 与其他等效的 {@code newFixedThreadPool(1)} 不同，返回的执行程序保证不可重新配置以使用其他线程。
     */
    /**
     * Creates an Executor that uses a single worker thread operating
     * off an unbounded queue. (Note however that if this single
     * thread terminates due to a failure during execution prior to
     * shutdown, a new one will take its place if needed to execute
     * subsequent tasks.)  Tasks are guaranteed to execute
     * sequentially, and no more than one task will be active at any
     * given time. Unlike the otherwise equivalent
     * {@code newFixedThreadPool(1)} the returned executor is
     * guaranteed not to be reconfigurable to use additional threads.
     *
     * @return the newly created single-threaded Executor
     */
    // 创建一个单线程、无界任务队列、默认线程工厂的线程池, 由于该实例被包装类包装过, 所以该实例强转后不可以再次修改线程池参数
    public static ExecutorService newSingleThreadExecutor() {
        return new FinalizableDelegatedExecutorService
            (new ThreadPoolExecutor(1, 1,
                                    0L, TimeUnit.MILLISECONDS,
                                    new LinkedBlockingQueue<Runnable>()));
    }

    /**
     * 20210815
     * 创建一个 Executor，它使用单个工作线程在无界队列中运行，并在需要时使用提供的 ThreadFactory 创建一个新线程。
     * 与其他等效的 {@code newFixedThreadPool(1, threadFactory)} 不同，返回的执行器保证不可重新配置以使用其他线程。
     */
    /**
     * Creates an Executor that uses a single worker thread operating
     * off an unbounded queue, and uses the provided ThreadFactory to
     * create a new thread when needed. Unlike the otherwise
     * equivalent {@code newFixedThreadPool(1, threadFactory)} the
     * returned executor is guaranteed not to be reconfigurable to use
     * additional threads.
     *
     * @param threadFactory the factory to use when creating new
     * threads
     *
     * @return the newly created single-threaded Executor
     * @throws NullPointerException if threadFactory is null
     */
    // 创建一个单线程、无界任务队列、指定线程工厂的线程池, 由于该实例被包装类包装过, 所以该实例强转后不可以再次修改线程池参数
    public static ExecutorService newSingleThreadExecutor(ThreadFactory threadFactory) {
        return new FinalizableDelegatedExecutorService
            (new ThreadPoolExecutor(1, 1,
                                    0L, TimeUnit.MILLISECONDS,
                                    new LinkedBlockingQueue<Runnable>(),
                                    threadFactory));
    }

    /**
     * 20210815
     * 创建一个线程池，根据需要创建新线程，但在可用时将重用先前构造的线程。 这些池通常会提高执行许多短期异步任务的程序的性能。
     * 如果可用，调用 {@code execute} 将重用先前构造的线程。 如果没有可用的现有线程，则会创建一个新线程并将其添加到池中。
     * 60 秒内未使用的线程将被终止并从缓存中删除。 因此，保持空闲足够长时间的池不会消耗任何资源。
     * 请注意，可以使用 {@link ThreadPoolExecutor} 构造函数创建具有相似属性但不同细节（例如，超时参数）的池。
     */
    /**
     * Creates a thread pool that creates new threads as needed, but
     * will reuse previously constructed threads when they are
     * available.  These pools will typically improve the performance
     * of programs that execute many short-lived asynchronous tasks.
     * Calls to {@code execute} will reuse previously constructed
     * threads if available. If no existing thread is available, a new
     * thread will be created and added to the pool. Threads that have
     * not been used for sixty seconds are terminated and removed from
     * the cache. Thus, a pool that remains idle for long enough will
     * not consume any resources. Note that pools with similar
     * properties but different details (for example, timeout parameters)
     * may be created using {@link ThreadPoolExecutor} constructors.
     *
     * @return the newly created thread pool
     */
    // 创建一个无核心线程、无界最大线程、无容量同步队列、默认线程工厂的线程池, 该实例强转后可以再次修改线程池参数
    public static ExecutorService newCachedThreadPool() {
        return new ThreadPoolExecutor(0, Integer.MAX_VALUE,
                                      60L, TimeUnit.SECONDS,
                                      new SynchronousQueue<Runnable>());
    }

    // 创建一个线程池，根据需要创建新线程，但在可用时将重用先前构造的线程，并在需要时使用提供的 ThreadFactory 创建新线程。
    /**
     * Creates a thread pool that creates new threads as needed, but
     * will reuse previously constructed threads when they are
     * available, and uses the provided
     * ThreadFactory to create new threads when needed.
     *
     * @param threadFactory the factory to use when creating new threads
     * @return the newly created thread pool
     * @throws NullPointerException if threadFactory is null
     */
    // 创建一个无核心线程、无界最大线程、无容量同步队列、指定线程工厂的线程池, 该实例强转后可以再次修改线程池参数
    public static ExecutorService newCachedThreadPool(ThreadFactory threadFactory) {
        return new ThreadPoolExecutor(0, Integer.MAX_VALUE,
                                      60L, TimeUnit.SECONDS,
                                      new SynchronousQueue<Runnable>(),
                                      threadFactory);
    }

    /**
     * 20210815
     * 创建一个单线程执行器，它可以安排命令在给定的延迟后运行，或定期执行。（但是请注意，如果这个单线程在关闭之前由于执行失败而终止，如果需要执行后续任务，
     * 一个新线程将取代它。）保证任务按顺序执行，并且不会超过一个任务处于活动状态在任何给定的时间。
     * 与其他等效的 {@code newScheduledThreadPool(1)} 不同，返回的执行器保证不可重新配置以使用其他线程。
     */
    /**
     * Creates a single-threaded executor that can schedule commands
     * to run after a given delay, or to execute periodically.
     * (Note however that if this single
     * thread terminates due to a failure during execution prior to
     * shutdown, a new one will take its place if needed to execute
     * subsequent tasks.)  Tasks are guaranteed to execute
     * sequentially, and no more than one task will be active at any
     * given time. Unlike the otherwise equivalent
     * {@code newScheduledThreadPool(1)} the returned executor is
     * guaranteed not to be reconfigurable to use additional threads.
     *
     * @return the newly created scheduled executor
     */
    // 创建一个单线程执行器，它可以安排命令在给定的延迟后运行, 或定期执行, 由于该实例被包装类包装过, 所以该实例强转后不可以再次修改线程池参数
    public static ScheduledExecutorService newSingleThreadScheduledExecutor() {
        return new DelegatedScheduledExecutorService
            (new ScheduledThreadPoolExecutor(1));
    }

    /**
     * 20210815
     * 创建一个单线程执行器，它可以安排命令在给定的延迟后运行，或定期执行。（但是请注意，如果这个单线程在关闭之前由于执行失败而终止，如果需要执行后续任务，
     * 一个新线程将取代它。）保证任务按顺序执行，并且不会超过一个任务处于活动状态 在任何给定的时间。
     * 与其他等效的 {@code newScheduledThreadPool(1, threadFactory)} 不同，返回的执行器保证不可重新配置以使用其他线程。
     */
    /**
     * Creates a single-threaded executor that can schedule commands
     * to run after a given delay, or to execute periodically.  (Note
     * however that if this single thread terminates due to a failure
     * during execution prior to shutdown, a new one will take its
     * place if needed to execute subsequent tasks.)  Tasks are
     * guaranteed to execute sequentially, and no more than one task
     * will be active at any given time. Unlike the otherwise
     * equivalent {@code newScheduledThreadPool(1, threadFactory)}
     * the returned executor is guaranteed not to be reconfigurable to
     * use additional threads.
     *
     * @param threadFactory the factory to use when creating new
     * threads
     * @return a newly created scheduled executor
     * @throws NullPointerException if threadFactory is null
     */
    // 使用指定的线程工厂创建一个单线程执行器，它可以安排命令在给定的延迟后运行, 或定期执行, 由于该实例被包装类包装过, 所以该实例强转后不可以再次修改线程池参数
    public static ScheduledExecutorService newSingleThreadScheduledExecutor(ThreadFactory threadFactory) {
        return new DelegatedScheduledExecutorService
            (new ScheduledThreadPoolExecutor(1, threadFactory));
    }

    // 创建一个线程池，可以安排命令在给定延迟后运行，或定期执行。
    /**
     * Creates a thread pool that can schedule commands to run after a
     * given delay, or to execute periodically.
     *
     * @param corePoolSize the number of threads to keep in the pool,
     * even if they are idle
     * @return a newly created scheduled thread pool
     * @throws IllegalArgumentException if {@code corePoolSize < 0}
     */
    // 指定核心线程数创建一个线程池，可以安排命令在给定延迟后运行，或定期执行，该实例强转后可以再次修改线程池参数
    public static ScheduledExecutorService newScheduledThreadPool(int corePoolSize) {
        return new ScheduledThreadPoolExecutor(corePoolSize);
    }

    // 创建一个线程池，可以安排命令在给定延迟后运行，或定期执行。
    /**
     * Creates a thread pool that can schedule commands to run after a
     * given delay, or to execute periodically.
     *
     * @param corePoolSize the number of threads to keep in the pool,
     * even if they are idle
     * @param threadFactory the factory to use when the executor
     * creates a new thread
     * @return a newly created scheduled thread pool
     * @throws IllegalArgumentException if {@code corePoolSize < 0}
     * @throws NullPointerException if threadFactory is null
     */
    // 指定核心线程数和线程工厂创建一个线程池，可以安排命令在给定延迟后运行，或定期执行，该实例强转后可以再次修改线程池参数
    public static ScheduledExecutorService newScheduledThreadPool(int corePoolSize, ThreadFactory threadFactory) {
        return new ScheduledThreadPoolExecutor(corePoolSize, threadFactory);
    }

    // 返回一个对象，该对象将所有已定义的 {@link ExecutorService} 方法委托给给定的执行程序，但不包括任何其他可以使用强制转换访问的方法。
    // 这提供了一种安全“冻结”配置并禁止调整给定具体实现的方法。
    /**
     * Returns an object that delegates all defined {@link
     * ExecutorService} methods to the given executor, but not any
     * other methods that might otherwise be accessible using
     * casts. This provides a way to safely "freeze" configuration and
     * disallow tuning of a given concrete implementation.
     *
     * @param executor the underlying implementation
     * @return an {@code ExecutorService} instance
     * @throws NullPointerException if executor null
     */
    // 包装指定线程池, 以屏蔽ThreadPoolExecutor的参数设置方法
    public static ExecutorService unconfigurableExecutorService(ExecutorService executor) {
        if (executor == null)
            throw new NullPointerException();
        return new DelegatedExecutorService(executor);
    }

    // 返回一个对象，该对象将所有已定义的 {@link ScheduledExecutorService} 方法委托给给定的执行程序，但不包括任何其他可以使用强制转换访问的方法。
    // 这提供了一种安全“冻结”配置并禁止调整给定具体实现的方法。
    /**
     * Returns an object that delegates all defined {@link
     * ScheduledExecutorService} methods to the given executor, but
     * not any other methods that might otherwise be accessible using
     * casts. This provides a way to safely "freeze" configuration and
     * disallow tuning of a given concrete implementation.
     *
     * @param executor the underlying implementation
     * @return a {@code ScheduledExecutorService} instance
     * @throws NullPointerException if executor null
     */
    // 包装指定线程池, 以屏蔽ScheduledExecutorService的参数设置方法
    public static ScheduledExecutorService unconfigurableScheduledExecutorService(ScheduledExecutorService executor) {
        if (executor == null)
            throw new NullPointerException();
        return new DelegatedScheduledExecutorService(executor);
    }

    // 返回用于创建新线程的默认线程工厂。 该工厂在同一个 {@link ThreadGroup} 中创建由 Executor 使用的所有新线程。 如果有 {@link java.lang.SecurityManager}，
    // 则使用 {@link System#getSecurityManager} 组，否则使用调用此 {@code defaultThreadFactory} 方法的线程组。 每个新线程都被创建为非守护线程，其优先级设置为
    // {@code Thread.NORM_PRIORITY} 和线程组中允许的最大优先级中的较小者。 新线程的名称可以通过 pool-N-thread-M 的 {@link Thread#getName} 访问，
    // 其中 N 是这个工厂的序列号，M 是这个工厂创建的线程的序列号。
    /**
     * Returns a default thread factory used to create new threads.
     * This factory creates all new threads used by an Executor in the
     * same {@link ThreadGroup}. If there is a {@link
     * java.lang.SecurityManager}, it uses the group of {@link
     * System#getSecurityManager}, else the group of the thread
     * invoking this {@code defaultThreadFactory} method. Each new
     * thread is created as a non-daemon thread with priority set to
     * the smaller of {@code Thread.NORM_PRIORITY} and the maximum
     * priority permitted in the thread group.  New threads have names
     * accessible via {@link Thread#getName} of
     * <em>pool-N-thread-M</em>, where <em>N</em> is the sequence
     * number of this factory, and <em>M</em> is the sequence number
     * of the thread created by this factory.
     *
     * @return a thread factory
     */
    // 返回用于创建新线程的默认线程工厂
    public static ThreadFactory defaultThreadFactory() {
        return new DefaultThreadFactory();
    }

    /**
     * 20210815
     * A. 返回一个线程工厂，用于创建与当前线程具有相同权限的新线程。该工厂使用与 {@link Executors#defaultThreadFactory} 相同的设置创建线程，
     *    另外将新线程的 AccessControlContext 和 contextClassLoader 设置为与调用此 {@code privilegedThreadFactory} 方法的线程相同。
     *    可以在设置当前线程的访问控制上下文的 {@link AccessController#doPrivileged AccessController.doPrivileged} 操作中创建一个新的
     *    {@code privilegedThreadFactory}，以创建具有在该操作中保留的选定权限设置的线程。
     * B. 请注意，虽然在此类线程中运行的任务将具有与当前线程相同的访问控制和类加载器设置，但它们不需要具有相同的 {@link java.lang.ThreadLocal} 或
     *    {@link java.lang.InheritableThreadLocal} 值。 如有必要，可以在使用 {@link ThreadPoolExecutor#beforeExecute(Thread, Runnable)}
     *    在 {@link ThreadPoolExecutor} 子类中运行任何任务之前设置或重置线程局部变量的特定值。 此外，如果需要将工作线程初始化为具有与其他指定线程相同的
     *    InheritableThreadLocal 设置，您可以创建一个自定义 ThreadFactory，该线程在其中等待并服务请求以创建将继承其值的其他线程。
     */
    /**
     * A.
     * Returns a thread factory used to create new threads that
     * have the same permissions as the current thread.
     * This factory creates threads with the same settings as {@link
     * Executors#defaultThreadFactory}, additionally setting the
     * AccessControlContext and contextClassLoader of new threads to
     * be the same as the thread invoking this
     * {@code privilegedThreadFactory} method.  A new
     * {@code privilegedThreadFactory} can be created within an
     * {@link AccessController#doPrivileged AccessController.doPrivileged}
     * action setting the current thread's access control context to
     * create threads with the selected permission settings holding
     * within that action.
     *
     * B.
     * <p>Note that while tasks running within such threads will have
     * the same access control and class loader settings as the
     * current thread, they need not have the same {@link
     * java.lang.ThreadLocal} or {@link
     * java.lang.InheritableThreadLocal} values. If necessary,
     * particular values of thread locals can be set or reset before
     * any task runs in {@link ThreadPoolExecutor} subclasses using
     * {@link ThreadPoolExecutor#beforeExecute(Thread, Runnable)}.
     * Also, if it is necessary to initialize worker threads to have
     * the same InheritableThreadLocal settings as some other
     * designated thread, you can create a custom ThreadFactory in
     * which that thread waits for and services requests to create
     * others that will inherit its values.
     *
     * @return a thread factory
     * @throws AccessControlException if the current access control
     * context does not have permission to both get and set context
     * class loader
     */
    // 返回一个线程工厂，用于创建与当前线程具有相同权限的新线程
    public static ThreadFactory privilegedThreadFactory() {
        return new PrivilegedThreadFactory();
    }

    // 返回一个 {@link Callable} 对象，该对象在调用时运行给定的任务并返回给定的结果。 这在将需要 {@code Callable} 的方法应用于其他无结果的操作时非常有用。
    /**
     * Returns a {@link Callable} object that, when
     * called, runs the given task and returns the given result.  This
     * can be useful when applying methods requiring a
     * {@code Callable} to an otherwise resultless action.
     *
     * @param task the task to run
     * @param result the result to return
     * @param <T> the type of the result
     * @return a callable object
     * @throws NullPointerException if task null
     */
    // 返回一个Callable对象, 该对象会在任务task运行完毕后返回, 包含任务task和指定的结果result
    public static <T> Callable<T> callable(Runnable task, T result) {
        if (task == null)
            throw new NullPointerException();
        return new RunnableAdapter<T>(task, result);
    }

    // 返回一个 {@link Callable} 对象，该对象在调用时运行给定的任务并返回 {@code null}。
    /**
     * Returns a {@link Callable} object that, when
     * called, runs the given task and returns {@code null}.
     *
     * @param task the task to run
     * @return a callable object
     * @throws NullPointerException if task null
     */
    // 返回一个Callable对象, 该对象会在任务task运行完毕后返回, 包含任务task和null结果result
    public static Callable<Object> callable(Runnable task) {
        if (task == null)
            throw new NullPointerException();
        return new RunnableAdapter<Object>(task, null);
    }

    // 返回一个 {@link Callable} 对象，该对象在调用时运行给定的特权操作并返回其结果。
    /**
     * Returns a {@link Callable} object that, when
     * called, runs the given privileged action and returns its result.
     *
     * @param action the privileged action to run
     * @return a callable object
     * @throws NullPointerException if action null
     */
    // 返回一个Callable对象, 该对象会在action运行完毕后返回
    public static Callable<Object> callable(final PrivilegedAction<?> action) {
        if (action == null)
            throw new NullPointerException();
        return new Callable<Object>() {
            public Object call() { return action.run(); }};
    }

    // 返回一个 {@link Callable} 对象，该对象在调用时运行给定的特权异常操作并返回其结果。
    /**
     * Returns a {@link Callable} object that, when
     * called, runs the given privileged exception action and returns
     * its result.
     *
     * @param action the privileged exception action to run
     * @return a callable object
     * @throws NullPointerException if action null
     */
    // 返回一个Callable对象, 该对象会在action运行完毕后返回
    public static Callable<Object> callable(final PrivilegedExceptionAction<?> action) {
        if (action == null)
            throw new NullPointerException();
        return new Callable<Object>() {
            public Object call() throws Exception { return action.run(); }};
    }

    // 返回一个 {@link Callable} 对象，该对象将在调用时在当前访问控制上下文下执行给定的 {@code callable}。
    // 此方法通常应在 {@link AccessController#doPrivileged AccessController.doPrivileged} 操作中调用，以创建可调用对象，如果可能，
    // 将在该操作中保留的选定权限设置下执行； 或者，如果不可能，则抛出关联的 {@link AccessControlException}。
    /**
     * Returns a {@link Callable} object that will, when called,
     * execute the given {@code callable} under the current access
     * control context. This method should normally be invoked within
     * an {@link AccessController#doPrivileged AccessController.doPrivileged}
     * action to create callables that will, if possible, execute
     * under the selected permission settings holding within that
     * action; or if not possible, throw an associated {@link
     * AccessControlException}.
     *
     * @param callable the underlying task
     * @param <T> the type of the callable's result
     * @return a callable object
     * @throws NullPointerException if callable null
     */
    // 返回一个Callable对象, 该对象将在调用时在当前访问控制上下文下执行给定的 {@code callable}
    public static <T> Callable<T> privilegedCallable(Callable<T> callable) {
        if (callable == null)
            throw new NullPointerException();
        return new PrivilegedCallable<T>(callable);
    }

    // 返回一个 {@link Callable} 对象，该对象将在调用时在当前访问控制上下文下执行给定的 {@code callable}，并将当前上下文类加载器作为上下文类加载器。
    // 此方法通常应在 {@link AccessController#doPrivileged AccessController.doPrivileged} 操作中调用，以创建可调用对象，如果可能，将在该操作中保留的选定权限设置下执行； 或者，如果不可能，则抛出关联的 {@link AccessControlException}。
    /**
     * Returns a {@link Callable} object that will, when called,
     * execute the given {@code callable} under the current access
     * control context, with the current context class loader as the
     * context class loader. This method should normally be invoked
     * within an
     * {@link AccessController#doPrivileged AccessController.doPrivileged}
     * action to create callables that will, if possible, execute
     * under the selected permission settings holding within that
     * action; or if not possible, throw an associated {@link
     * AccessControlException}.
     *
     * @param callable the underlying task
     * @param <T> the type of the callable's result
     * @return a callable object
     * @throws NullPointerException if callable null
     * @throws AccessControlException if the current access control
     * context does not have permission to both set and get context
     * class loader
     */
    // // 返回一个Callable对象, 该对象将在调用时在当前访问控制上下文下执行给定的 {@code callable}，并将当前上下文类加载器作为上下文类加载器
    public static <T> Callable<T> privilegedCallableUsingCurrentClassLoader(Callable<T> callable) {
        if (callable == null)
            throw new NullPointerException();
        return new PrivilegedCallableUsingCurrentClassLoader<T>(callable);
    }

    // 支持公共方法的非公共类
    // Non-public classes supporting the public methods

    // 运行给定任务并返回给定结果的可调用对象
    /**
     * A callable that runs given task and returns given result
     */
    // 实现Callable接口, 运行给定任务, 并返回给定结果的可调用对象
    static final class RunnableAdapter<T> implements Callable<T> {

        final Runnable task;
        final T result;

        RunnableAdapter(Runnable task, T result) {
            this.task = task;
            this.result = result;
        }

        public T call() {
            task.run();
            return result;
        }
    }

    // 在既定的访问控制设置下运行的可调用对象
    /**
     * A callable that runs under established access control settings
     */
    // 实现Callable接口, 能够在既定的访问控制设置下运行的可调用对象
    static final class PrivilegedCallable<T> implements Callable<T> {

        private final Callable<T> task;
        private final AccessControlContext acc;

        PrivilegedCallable(Callable<T> task) {
            this.task = task;
            this.acc = AccessController.getContext();
        }

        public T call() throws Exception {
            try {
                return AccessController.doPrivileged(
                    new PrivilegedExceptionAction<T>() {
                        public T run() throws Exception {
                            return task.call();
                        }
                    }, acc);
            } catch (PrivilegedActionException e) {
                throw e.getException();
            }
        }
    }

    // 在已建立的访问控制设置和当前 ClassLoader 下运行的可调用对象
    /**
     * A callable that runs under established access control settings and
     * current ClassLoader
     */
    // 实现Callable接口, 能够在已建立的访问控制设置和当前ClassLoader下运行的可调用对象
    static final class PrivilegedCallableUsingCurrentClassLoader<T> implements Callable<T> {

        private final Callable<T> task;
        private final AccessControlContext acc;
        private final ClassLoader ccl;

        PrivilegedCallableUsingCurrentClassLoader(Callable<T> task) {
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                // 从这个类调用 getContextClassLoader 永远不会触发安全检查，但我们会检查我们的调用者是否有这个权限。
                // Calls to getContextClassLoader from this class
                // never trigger a security check, but we check
                // whether our callers have this permission anyways.
                sm.checkPermission(SecurityConstants.GET_CLASSLOADER_PERMISSION);

                // 不管 setContextClassLoader 是否有必要，如果权限不可用，我们会很快失败。
                // Whether setContextClassLoader turns out to be necessary
                // or not, we fail fast if permission is not available.
                sm.checkPermission(new RuntimePermission("setContextClassLoader"));
            }

            this.task = task;
            this.acc = AccessController.getContext();
            this.ccl = Thread.currentThread().getContextClassLoader();
        }

        public T call() throws Exception {
            try {
                return AccessController.doPrivileged(
                    new PrivilegedExceptionAction<T>() {
                        public T run() throws Exception {
                            Thread t = Thread.currentThread();
                            ClassLoader cl = t.getContextClassLoader();
                            if (ccl == cl) {
                                return task.call();
                            } else {
                                t.setContextClassLoader(ccl);
                                try {
                                    return task.call();
                                } finally {
                                    t.setContextClassLoader(cl);
                                }
                            }
                        }
                    }, acc);
            } catch (PrivilegedActionException e) {
                throw e.getException();
            }
        }
    }

    // 默认线程工厂
    /**
     * The default thread factory
     */
    // 默认线程工厂
    static class DefaultThreadFactory implements ThreadFactory {

        private static final AtomicInteger poolNumber = new AtomicInteger(1);
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        // pool-[?: poolNumber]-thread-[?: threadNumber]
        DefaultThreadFactory() {
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() :
                                  Thread.currentThread().getThreadGroup();
            namePrefix = "pool-" +
                          poolNumber.getAndIncrement() +
                         "-thread-";
        }

        // 默认创建优先级为5的非守护线程, 线程名称为pool-[?: poolNumber]-thread-[?: threadNumber]
        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r,
                                  namePrefix + threadNumber.getAndIncrement(),
                                  0);

            // 默认非守护线程
            if (t.isDaemon())
                t.setDaemon(false);

            // 默认优先级为5
            if (t.getPriority() != Thread.NORM_PRIORITY)
                t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    }

    // 线程工厂捕获访问控制上下文和类加载器
    /**
     * Thread factory capturing access control context and class loader
     */
    // 能够继承权限的线程工厂, 用于捕获访问、控制上下文和类加载器
    static class PrivilegedThreadFactory extends DefaultThreadFactory {

        private final AccessControlContext acc;
        private final ClassLoader ccl;

        PrivilegedThreadFactory() {
            super();
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                // 从这个类调用 getContextClassLoader 永远不会触发安全检查，但我们会检查我们的调用者是否有这个权限。
                // Calls to getContextClassLoader from this class
                // never trigger a security check, but we check
                // whether our callers have this permission anyways.
                sm.checkPermission(SecurityConstants.GET_CLASSLOADER_PERMISSION);

                // Fail fast
                sm.checkPermission(new RuntimePermission("setContextClassLoader"));
            }
            this.acc = AccessController.getContext();
            this.ccl = Thread.currentThread().getContextClassLoader();
        }

        public Thread newThread(final Runnable r) {
            return super.newThread(new Runnable() {
                public void run() {
                    AccessController.doPrivileged(new PrivilegedAction<Void>() {
                        public Void run() {
                            Thread.currentThread().setContextClassLoader(ccl);
                            r.run();
                            return null;
                        }
                    }, acc);
                }
            });
        }
    }

    // 仅公开 ExecutorService 实现的 ExecutorService 方法的包装类。
    /**
     * A wrapper class that exposes only the ExecutorService methods
     * of an ExecutorService implementation.
     */
    // 仅公开 ExecutorService 实现的 ExecutorService 方法的包装类
    static class DelegatedExecutorService extends AbstractExecutorService {

        private final ExecutorService e;

        DelegatedExecutorService(ExecutorService executor) { e = executor; }

        public void execute(Runnable command) { e.execute(command); }

        public void shutdown() { e.shutdown(); }

        public List<Runnable> shutdownNow() { return e.shutdownNow(); }

        public boolean isShutdown() { return e.isShutdown(); }

        public boolean isTerminated() { return e.isTerminated(); }

        public boolean awaitTermination(long timeout, TimeUnit unit)
            throws InterruptedException {
            return e.awaitTermination(timeout, unit);
        }
        public Future<?> submit(Runnable task) {
            return e.submit(task);
        }

        public <T> Future<T> submit(Callable<T> task) {
            return e.submit(task);
        }

        public <T> Future<T> submit(Runnable task, T result) {
            return e.submit(task, result);
        }

        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
            throws InterruptedException {
            return e.invokeAll(tasks);
        }

        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks,
                                             long timeout, TimeUnit unit)
            throws InterruptedException {
            return e.invokeAll(tasks, timeout, unit);
        }

        public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
            throws InterruptedException, ExecutionException {
            return e.invokeAny(tasks);
        }

        public <T> T invokeAny(Collection<? extends Callable<T>> tasks,
                               long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
            return e.invokeAny(tasks, timeout, unit);
        }
    }

    // 仅公开 ExecutorService 实现的 ExecutorService 方法的包装类, 再加上finalize析构函数
    static class FinalizableDelegatedExecutorService extends DelegatedExecutorService {

        FinalizableDelegatedExecutorService(ExecutorService executor) {
            super(executor);
        }

        protected void finalize() {
            super.shutdown();
        }
    }

    // 仅公开 ScheduledExecutorService 实现的 ScheduledExecutorService 方法的包装类。
    /**
     * A wrapper class that exposes only the ScheduledExecutorService
     * methods of a ScheduledExecutorService implementation.
     */
    // 仅公开 ScheduledExecutorService 实现的 ScheduledExecutorService 方法的包装类
    static class DelegatedScheduledExecutorService extends DelegatedExecutorService implements ScheduledExecutorService {

        private final ScheduledExecutorService e;

        DelegatedScheduledExecutorService(ScheduledExecutorService executor) {
            super(executor);
            e = executor;
        }

        public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
            return e.schedule(command, delay, unit);
        }

        public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
            return e.schedule(callable, delay, unit);
        }

        public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
            return e.scheduleAtFixedRate(command, initialDelay, period, unit);
        }

        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
            return e.scheduleWithFixedDelay(command, initialDelay, delay, unit);
        }
    }

    // 无法实例化。
    /** Cannot instantiate. */
    private Executors() {}
}
