/*
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

/**
 * 20210813
 * A. 实用程序类通常用于并发编程。这个包包括一些小的标准化的可扩展框架，以及一些提供有用功能的类，否则很乏味或难以实现。以下是主要组件的简要说明。
 *    另请参阅 {@link java.util.concurrent.locks} 和 {@link java.util.concurrent.atomic} 包。
 *
 * Executors接口
 * B. {@link java.util.concurrent.Executor} 是一个简单的标准化接口，用于定义自定义的类线程子系统，包括线程池、异步 I/O 和轻量级任务框架。
 *    根据使用的具体 Executor 类，任务可以在新创建的线程、现有任务执行线程或调用 {@link java.util.concurrent.Executor#execute execute} 的线程中执行，
 *    并且可以顺序执行或 同时。
 * C. {@link java.util.concurrent.ExecutorService} 提供了更完整的异步任务执行框架。ExecutorService 管理任务的排队和调度，并允许受控关闭。
 * D. {@link java.util.concurrent.ScheduledExecutorService} 子接口和相关接口添加了对延迟和周期性任务执行的支持。 ExecutorServices 提供了安排任何函数的异步执行的方法，
 *    这些函数表示为 {@link java.util.concurrent.Callable}，{@link java.lang.Runnable} 的结果承载模拟。
 * E. {@link java.util.concurrent.Future} 返回函数的结果，允许确定执行是否已完成，并提供取消执行的方法。
 * F. {@link java.util.concurrent.RunnableFuture} 是一个 {@code Future}，它拥有一个 {@code run} 方法，该方法在执行时设置其结果。
 *
 * Executors实现类
 * G. 类 {@link java.util.concurrent.ThreadPoolExecutor} 和 {@link java.util.concurrent.ScheduledThreadPoolExecutor} 提供了可调的、灵活的线程池。
 * H. {@link java.util.concurrent.Executors} 类为最常见的 Executor 种类和配置提供了工厂方法，以及一些使用它们的实用方法。
 *    其他基于 {@code Executors} 的实用程序包括提供 Futures 通用可扩展实现的具体类 {@link java.util.concurrent.FutureTask} 和
 *    {@link java.util.concurrent.ExecutorCompletionService}，帮助协调 处理异步任务组。
 * I. {@link java.util.concurrent.ForkJoinPool} 类提供了一个 Executor，主要用于处理 {@link java.util.concurrent.ForkJoinTask} 及其子类的实例。
 *    这些类采用工作窃取调度程序，为符合计算密集型并行处理中通常存在的限制的任务获得高吞吐量。
 *
 * Queues
 * J. {@link java.util.concurrent.ConcurrentLinkedQueue} 类提供了一个高效的可扩展线程安全非阻塞 FIFO 队列。
 *    {@link java.util.concurrent.ConcurrentLinkedDeque} 类与此类似，但额外支持 {@link java.util.Deque} 接口。
 * K. {@code java.util.concurrent} 中的五个实现支持扩展的 {@link java.util.concurrent.BlockingQueue} 接口，该接口定义了 put 和 take 的阻塞版本：
 *    {@link java.util.concurrent.LinkedBlockingQueue}，{@link java.util.concurrent.ArrayBlockingQueue}、{@link java.util.concurrent.SynchronousQueue}、
 *    {@link java.util.concurrent.PriorityBlockingQueue} 和 {@link java.util.concurrent.DelayQueue}。
 *    不同的类涵盖了生产者-消费者、消息传递、并行任务和相关并发设计的最常见使用上下文。
 * L. 扩展接口 {@link java.util.concurrent.TransferQueue} 和实现 {@link java.util.concurrent.LinkedTransferQueue}
 *    引入了同步 {@code transfer} 方法（以及相关功能），其中生产者可以选择阻止等待 它的消费者。
 * M. {@link java.util.concurrent.BlockingDeque} 接口扩展了 {@code BlockingQueue} 以支持 FIFO 和 LIFO（基于堆栈）操作。
 *    类 {@link java.util.concurrent.LinkedBlockingDeque} 提供了一个实现。
 *
 * Timing
 * N. {@link java.util.concurrent.TimeUnit} 类提供多种粒度（包括纳秒）用于指定和控制基于超时的操作。 除了无限期等待之外，包中的大多数类都包含基于超时的操作。
 *    在使用超时的所有情况下，超时指定方法在指示超时之前应等待的最短时间。 实现会“尽最大努力”在超时发生后尽快检测到超时。
 *    但是，在检测到超时和在该超时之后实际再次执行线程之间可能会经过一段不确定的时间。 所有接受超时参数的方法都将小于或等于零的值视为根本不等待。
 *    要“永远”等待，您可以使用 {@code Long.MAX_VALUE} 值。
 *
 * 同步器
 * O. 五个类有助于常见的专用同步习语:
 *      a. {@link java.util.concurrent.Semaphore} 是一个经典的并发工具。
 *      b. {@link java.util.concurrent.CountDownLatch} 是一个非常简单但非常常见的实用程序，用于阻塞直到给定数量的信号、事件或条件成立。
 *      c. {@link java.util.concurrent.CyclicBarrier} 是一个可重置的多路同步点，在某些并行编程风格中很有用。
 *      d. {@link java.util.concurrent.Phaser} 提供了一种更灵活的屏障形式，可用于控制多个线程之间的分阶段计算。
 *      e. {@link java.util.concurrent.Exchanger} 允许两个线程在集合点交换对象，并且在多种管道设计中很有用。
 *
 * Concurrent Collections(并发集合)
 * P. 除了队列之外，这个包还提供了专为在多线程上下文中使用而设计的集合实现：{@link java.util.concurrent.ConcurrentHashMap}、
 *    {@link java.util.concurrent.ConcurrentSkipListMap}、{@link java.util.concurrent.ConcurrentSkipListSet} 、
 *    {@link java.util.concurrent.CopyOnWriteArrayList} 和 {@link java.util.concurrent.CopyOnWriteArraySet}。
 *    当需要许多线程访问给定集合时，{@code ConcurrentHashMap} 通常优于同步的 {@code HashMap}，
 *    而 {@code ConcurrentSkipListMap} 通常优于同步的 {@code TreeMap}。 当预期的读取和遍历次数大大超过列表的更新次数时，
 *    {@code CopyOnWriteArrayList} 比同步的 {@code ArrayList} 更可取。
 * Q. 与此包中的某些类一起使用的“并发”前缀是一种简写，表示与类似的“同步”类的几个不同之处。 例如 {@code java.util.Hashtable} 和
 *    {@code Collections.synchronizedMap(new HashMap())} 是同步的。但是 {@link java.util.concurrent.ConcurrentHashMap} 是“并发的”。
 *    并发集合是线程安全的，但不受单个排除锁的控制。 在 ConcurrentHashMap 的特殊情况下，它安全地允许任意数量的并发读取以及可调整数量的并发写入。
 *    当您需要通过单个锁阻止对集合的所有访问时，“同步”类可能很有用，但代价是可扩展性较差。在期望多个线程访问公共集合的其他情况下，“并发”版本通常更可取。
 *    当集合未共享或仅在持有其他锁时才可访问时，最好使用非同步集合。
 * R. 大多数并发集合实现（包括大多数队列）也不同于通常的 {@code java.util} 约定，因为它们的 {@linkplain java.util.Iterator Iterators}
 *    和 {@linkplain java.util.Spliterator Spliterator} 提供了弱一致性而不是 比快速失败遍历：
 *      a. 他们可以与其他操作同时进行；
 *      b. 他们永远不会抛出 {@link java.util.ConcurrentModificationException ConcurrentModificationException}
 *      c. 它们保证遍历元素，因为它们在构造时就存在过一次，并且可能（但不保证）反映构造后的任何修改。
 *
 * Memory Consistency Properties(内存一致性属性)
 * S. Java 语言规范的第 17 章定义了内存操作（例如共享变量的读取和写入）的发生前关系(happens-befor)。
 *    只有在写操作发生在读操作之前，才能保证一个线程的写入结果对另一个线程的读取可见。 {@code synchronized} 和 {@code volatile} 构造，以及 {@code Thread.start()}
 *    和 {@code Thread.join()} 方法，可以形成先发生关系。 特别是：
 *      a. 线程中的每个操作都发生在该线程中按程序顺序稍后出现的每个操作之前。
 *      b. 监视器的解锁（{@code synchronized} 块或方法退出）发生在同一监视器的每个后续锁定（{@code synchronized} 块或方法入口）之前。
 *         并且因为happens-before 关系是可传递的，线程在解锁之前的所有操作都发生在该监控的任何线程锁定之后的所有操作之前。
 *      c. 对 {@code volatile} 字段的写入发生在每次后续读取同一字段之前。 {@code volatile} 字段的写入和读取与进入和退出监视器具有类似的内存一致性效果，但不需要互斥锁定。
 *      d. 在线程上调用 {@code start} 发生在已启动线程中的任何操作之前。
 *      e. 线程中的所有操作都发生在任何其他线程从该线程上的 {@code join} 成功返回之前。
 * T. {@code java.util.concurrent} 及其子包中所有类的方法将这些保证扩展到更高级别的同步。 特别是：
 *      a. 在将对象放入任何并发集合之前线程中的操作发生在从另一个线程中的集合访问或删除该元素之后的操作之前。
 *      b. 在将 {@code Runnable} 提交给 {@code Executor} 之前线程中的操作发生在其执行开始之前。 对于提交给 {@code ExecutorService} 的 {@code Callables} 也是如此。
 *      c. 在另一个线程中通过 {@code Future.get()} 检索结果之后，由 {@code Future} 表示的异步计算采取的操作发生在操作之前。
 *      e. “释放”同步器方法之前的操作，例如 {@code Lock.unlock}、{@code Semaphore.release} 和 {@code CountDownLatch.countDown} 发生在成功的“获取”方法之后的操作之前，
 *         例如 {@code Lock.lock}、{@code Semaphore.acquire}、{@code Condition.await} 和 {@code CountDownLatch.await} 在另一个线程中的同一个同步器对象上。
 *      f. 对于通过 {@code Exchanger} 成功交换对象的每一对线程，每个线程中 {@code exchange()} 之前的操作发生在另一个线程中相应 {@code exchange()} 之后的操作之前。
 *      g. 调用 {@code CyclicBarrier.await} 和 {@code Phaser.awaitAdvance}（及其变体）之前的动作发生在屏障动作执行的动作之前，屏障动作执行的动作发生在屏障动作之后的动作发生之前
 *         从其他线程中相应的 {@code await} 成功返回。
 */
/**
 * A.
 * Utility classes commonly useful in concurrent programming.  This
 * package includes a few small standardized extensible frameworks, as
 * well as some classes that provide useful functionality and are
 * otherwise tedious or difficult to implement.  Here are brief
 * descriptions of the main components.  See also the
 * {@link java.util.concurrent.locks} and
 * {@link java.util.concurrent.atomic} packages.
 *
 * <h2>Executors</h2>
 *
 * <b>Interfaces.</b>
 *
 * B.
 * {@link java.util.concurrent.Executor} is a simple standardized
 * interface for defining custom thread-like subsystems, including
 * thread pools, asynchronous I/O, and lightweight task frameworks.
 * Depending on which concrete Executor class is being used, tasks may
 * execute in a newly created thread, an existing task-execution thread,
 * or the thread calling {@link java.util.concurrent.Executor#execute
 * execute}, and may execute sequentially or concurrently.
 *
 * C.
 * {@link java.util.concurrent.ExecutorService} provides a more
 * complete asynchronous task execution framework.  An
 * ExecutorService manages queuing and scheduling of tasks,
 * and allows controlled shutdown.
 *
 * D.
 * The {@link java.util.concurrent.ScheduledExecutorService}
 * subinterface and associated interfaces add support for
 * delayed and periodic task execution.  ExecutorServices
 * provide methods arranging asynchronous execution of any
 * function expressed as {@link java.util.concurrent.Callable},
 * the result-bearing analog of {@link java.lang.Runnable}.
 *
 * E.
 * A {@link java.util.concurrent.Future} returns the results of
 * a function, allows determination of whether execution has
 * completed, and provides a means to cancel execution.
 *
 * F.
 * A {@link java.util.concurrent.RunnableFuture} is a {@code Future}
 * that possesses a {@code run} method that upon execution,
 * sets its results.
 *
 * <p>
 *
 * <b>Implementations.</b>
 *
 * G.
 * Classes {@link java.util.concurrent.ThreadPoolExecutor} and
 * {@link java.util.concurrent.ScheduledThreadPoolExecutor}
 * provide tunable, flexible thread pools.
 *
 * H.
 * The {@link java.util.concurrent.Executors} class provides
 * factory methods for the most common kinds and configurations
 * of Executors, as well as a few utility methods for using
 * them.  Other utilities based on {@code Executors} include the
 * concrete class {@link java.util.concurrent.FutureTask}
 * providing a common extensible implementation of Futures, and
 * {@link java.util.concurrent.ExecutorCompletionService}, that
 * assists in coordinating the processing of groups of
 * asynchronous tasks.
 *
 * I.
 * <p>Class {@link java.util.concurrent.ForkJoinPool} provides an
 * Executor primarily designed for processing instances of {@link
 * java.util.concurrent.ForkJoinTask} and its subclasses.  These
 * classes employ a work-stealing scheduler that attains high
 * throughput for tasks conforming to restrictions that often hold in
 * computation-intensive parallel processing.
 *
 * <h2>Queues</h2>
 *
 * J.
 * The {@link java.util.concurrent.ConcurrentLinkedQueue} class
 * supplies an efficient scalable thread-safe non-blocking FIFO queue.
 * The {@link java.util.concurrent.ConcurrentLinkedDeque} class is
 * similar, but additionally supports the {@link java.util.Deque}
 * interface.
 *
 * K.
 * <p>Five implementations in {@code java.util.concurrent} support
 * the extended {@link java.util.concurrent.BlockingQueue}
 * interface, that defines blocking versions of put and take:
 * {@link java.util.concurrent.LinkedBlockingQueue},
 * {@link java.util.concurrent.ArrayBlockingQueue},
 * {@link java.util.concurrent.SynchronousQueue},
 * {@link java.util.concurrent.PriorityBlockingQueue}, and
 * {@link java.util.concurrent.DelayQueue}.
 * The different classes cover the most common usage contexts
 * for producer-consumer, messaging, parallel tasking, and
 * related concurrent designs.
 *
 * L.
 * <p>Extended interface {@link java.util.concurrent.TransferQueue},
 * and implementation {@link java.util.concurrent.LinkedTransferQueue}
 * introduce a synchronous {@code transfer} method (along with related
 * features) in which a producer may optionally block awaiting its
 * consumer.
 *
 * M.
 * <p>The {@link java.util.concurrent.BlockingDeque} interface
 * extends {@code BlockingQueue} to support both FIFO and LIFO
 * (stack-based) operations.
 * Class {@link java.util.concurrent.LinkedBlockingDeque}
 * provides an implementation.
 *
 * <h2>Timing</h2>
 *
 * N.
 * The {@link java.util.concurrent.TimeUnit} class provides
 * multiple granularities (including nanoseconds) for
 * specifying and controlling time-out based operations.  Most
 * classes in the package contain operations based on time-outs
 * in addition to indefinite waits.  In all cases that
 * time-outs are used, the time-out specifies the minimum time
 * that the method should wait before indicating that it
 * timed-out.  Implementations make a &quot;best effort&quot;
 * to detect time-outs as soon as possible after they occur.
 * However, an indefinite amount of time may elapse between a
 * time-out being detected and a thread actually executing
 * again after that time-out.  All methods that accept timeout
 * parameters treat values less than or equal to zero to mean
 * not to wait at all.  To wait "forever", you can use a value
 * of {@code Long.MAX_VALUE}.
 *
 * <h2>Synchronizers</h2>
 *
 * O.
 * Five classes aid common special-purpose synchronization idioms.
 * <ul>
 *
 * <li>{@link java.util.concurrent.Semaphore} is a classic concurrency tool.
 *
 * <li>{@link java.util.concurrent.CountDownLatch} is a very simple yet
 * very common utility for blocking until a given number of signals,
 * events, or conditions hold.
 *
 * <li>A {@link java.util.concurrent.CyclicBarrier} is a resettable
 * multiway synchronization point useful in some styles of parallel
 * programming.
 *
 * <li>A {@link java.util.concurrent.Phaser} provides
 * a more flexible form of barrier that may be used to control phased
 * computation among multiple threads.
 *
 * <li>An {@link java.util.concurrent.Exchanger} allows two threads to
 * exchange objects at a rendezvous point, and is useful in several
 * pipeline designs.
 *
 * </ul>
 *
 * <h2>Concurrent Collections</h2>
 *
 * P.
 * Besides Queues, this package supplies Collection implementations
 * designed for use in multithreaded contexts:
 * {@link java.util.concurrent.ConcurrentHashMap},
 * {@link java.util.concurrent.ConcurrentSkipListMap},
 * {@link java.util.concurrent.ConcurrentSkipListSet},
 * {@link java.util.concurrent.CopyOnWriteArrayList}, and
 * {@link java.util.concurrent.CopyOnWriteArraySet}.
 * When many threads are expected to access a given collection, a
 * {@code ConcurrentHashMap} is normally preferable to a synchronized
 * {@code HashMap}, and a {@code ConcurrentSkipListMap} is normally
 * preferable to a synchronized {@code TreeMap}.
 * A {@code CopyOnWriteArrayList} is preferable to a synchronized
 * {@code ArrayList} when the expected number of reads and traversals
 * greatly outnumber the number of updates to a list.
 *
 * Q.
 * <p>The "Concurrent" prefix used with some classes in this package
 * is a shorthand indicating several differences from similar
 * "synchronized" classes.  For example {@code java.util.Hashtable} and
 * {@code Collections.synchronizedMap(new HashMap())} are
 * synchronized.  But {@link
 * java.util.concurrent.ConcurrentHashMap} is "concurrent".  A
 * concurrent collection is thread-safe, but not governed by a
 * single exclusion lock.  In the particular case of
 * ConcurrentHashMap, it safely permits any number of
 * concurrent reads as well as a tunable number of concurrent
 * writes.  "Synchronized" classes can be useful when you need
 * to prevent all access to a collection via a single lock, at
 * the expense of poorer scalability.  In other cases in which
 * multiple threads are expected to access a common collection,
 * "concurrent" versions are normally preferable.  And
 * unsynchronized collections are preferable when either
 * collections are unshared, or are accessible only when
 * holding other locks.
 *
 * R.
 * <p id="Weakly">Most concurrent Collection implementations
 * (including most Queues) also differ from the usual {@code java.util}
 * conventions in that their {@linkplain java.util.Iterator Iterators}
 * and {@linkplain java.util.Spliterator Spliterators} provide
 * <em>weakly consistent</em> rather than fast-fail traversal:
 * <ul>
 * <li>they may proceed concurrently with other operations
 * <li>they will never throw {@link java.util.ConcurrentModificationException
 * ConcurrentModificationException}
 * <li>they are guaranteed to traverse elements as they existed upon
 * construction exactly once, and may (but are not guaranteed to)
 * reflect any modifications subsequent to construction.
 * </ul>
 *
 * S.
 * <h2 id="MemoryVisibility">Memory Consistency Properties</h2>
 *
 * <a href="https://docs.oracle.com/javase/specs/jls/se7/html/jls-17.html#jls-17.4.5">
 * Chapter 17 of the Java Language Specification</a> defines the
 * <i>happens-before</i> relation on memory operations such as reads and
 * writes of shared variables.  The results of a write by one thread are
 * guaranteed to be visible to a read by another thread only if the write
 * operation <i>happens-before</i> the read operation.  The
 * {@code synchronized} and {@code volatile} constructs, as well as the
 * {@code Thread.start()} and {@code Thread.join()} methods, can form
 * <i>happens-before</i> relationships.  In particular:
 *
 * <ul>
 *   <li>Each action in a thread <i>happens-before</i> every action in that
 *   thread that comes later in the program's order.
 *
 *   <li>An unlock ({@code synchronized} block or method exit) of a
 *   monitor <i>happens-before</i> every subsequent lock ({@code synchronized}
 *   block or method entry) of that same monitor.  And because
 *   the <i>happens-before</i> relation is transitive, all actions
 *   of a thread prior to unlocking <i>happen-before</i> all actions
 *   subsequent to any thread locking that monitor.
 *
 *   <li>A write to a {@code volatile} field <i>happens-before</i> every
 *   subsequent read of that same field.  Writes and reads of
 *   {@code volatile} fields have similar memory consistency effects
 *   as entering and exiting monitors, but do <em>not</em> entail
 *   mutual exclusion locking.
 *
 *   <li>A call to {@code start} on a thread <i>happens-before</i> any
 *   action in the started thread.
 *
 *   <li>All actions in a thread <i>happen-before</i> any other thread
 *   successfully returns from a {@code join} on that thread.
 *
 * </ul>
 *
 * T.
 * The methods of all classes in {@code java.util.concurrent} and its
 * subpackages extend these guarantees to higher-level
 * synchronization.  In particular:
 *
 * <ul>
 *
 *   <li>Actions in a thread prior to placing an object into any concurrent
 *   collection <i>happen-before</i> actions subsequent to the access or
 *   removal of that element from the collection in another thread.
 *
 *   <li>Actions in a thread prior to the submission of a {@code Runnable}
 *   to an {@code Executor} <i>happen-before</i> its execution begins.
 *   Similarly for {@code Callables} submitted to an {@code ExecutorService}.
 *
 *   <li>Actions taken by the asynchronous computation represented by a
 *   {@code Future} <i>happen-before</i> actions subsequent to the
 *   retrieval of the result via {@code Future.get()} in another thread.
 *
 *   <li>Actions prior to "releasing" synchronizer methods such as
 *   {@code Lock.unlock}, {@code Semaphore.release}, and
 *   {@code CountDownLatch.countDown} <i>happen-before</i> actions
 *   subsequent to a successful "acquiring" method such as
 *   {@code Lock.lock}, {@code Semaphore.acquire},
 *   {@code Condition.await}, and {@code CountDownLatch.await} on the
 *   same synchronizer object in another thread.
 *
 *   <li>For each pair of threads that successfully exchange objects via
 *   an {@code Exchanger}, actions prior to the {@code exchange()}
 *   in each thread <i>happen-before</i> those subsequent to the
 *   corresponding {@code exchange()} in another thread.
 *
 *   <li>Actions prior to calling {@code CyclicBarrier.await} and
 *   {@code Phaser.awaitAdvance} (as well as its variants)
 *   <i>happen-before</i> actions performed by the barrier action, and
 *   actions performed by the barrier action <i>happen-before</i> actions
 *   subsequent to a successful return from the corresponding {@code await}
 *   in other threads.
 *
 * </ul>
 *
 * @since 1.5
 */
package java.util.concurrent;
