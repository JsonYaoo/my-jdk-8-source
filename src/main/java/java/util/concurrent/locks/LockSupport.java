/*
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util.concurrent.locks;
import sun.misc.Unsafe;

/**
 * 20210725
 * A. 用于创建锁和其他同步类的基本线程阻塞原语。
 * B. 此类与使用它的每个线程相关联，一个许可（在{@link java.util.concurrent.Semaphore Semaphore} 类的意义上）。如果许可证可用，调用{@code park} 将立即返回，
 *    并在此过程中使用它；否则可能会阻塞。如果许可证尚未可用，则调用 {@code unpark} 可使许可证可用。（尽管与信号量不同，许可不会累积。最多只有一个。）
 * C. 方法{@code park}和{@code unpark}提供了阻塞和解除阻塞线程的有效方法，这些线程不会遇到导致已弃用的方法{@code Thread.suspend}和{@code Thread.resume}无法用于的问题。
 *    此类目的：在一个调用{@code park}的线程和另一个试图{@code unpark}的线程之间进行竞争，由于许可，它将保持活跃度。此外，如果调用者的线程被中断，{@code park}将返回，
 *    并且支持超时版本。{@code park}方法也可能在任何其他时间返回，“无缘无故”，因此通常必须在返回时重新检查条件的循环中调用。 从这个意义上说，{@code park}是“忙等待”的优化，
 *    不会浪费太多时间旋转，但必须与{@code unpark} 配对才能有效。
 * D. {@code park}的三种形式都支持{@code blocker}对象参数。该对象在线程被阻塞时被记录，以允许监控和诊断工具识别线程被阻塞的原因。
 *    （此类工具可以使用方法{@link #getBlocker(Thread)} 访问阻止程序。）强烈建议使用这些形式而不是没有此参数的原始形式。
 *    在锁实现中作为{@code blocker} 提供的正常参数是{@code this}。
 * E. 这些方法旨在用作创建更高级别同步实用程序的工具，并且它们本身对大多数并发控制应用程序没有用处。 {@code park} 方法仅用于以下形式的构造：
 * {@code while (!canProceed()) { ... LockSupport.park(this); }}
 *    其中{@code canProceed}或调用{@code park}之前的任何其他操作都不需要锁定或阻止。由于每个线程只关联一个许可证，因此对{@code park}的任何中间使用都可能干扰其预期效果。
 * F. 示例用法, 这是先进先出非重入锁类的草图:
 * {@code
 * class FIFOMutex {
 *   private final AtomicBoolean locked = new AtomicBoolean(false);
 *   private final Queue waiters = new ConcurrentLinkedQueue();
 *
 *   public void lock() {
 *     boolean wasInterrupted = false;
 *     Thread current = Thread.currentThread();
 *     waiters.add(current);
 *
 *     // Block while not first in queue or cannot acquire lock
 *     while (waiters.peek() != current || !locked.compareAndSet(false, true)) {
 *       LockSupport.park(this);
 *       if (Thread.interrupted()) // ignore interrupts while waiting
 *         wasInterrupted = true;
 *     }
 *
 *     waiters.remove();
 *     if (wasInterrupted)          // reassert interrupt status on exit
 *       current.interrupt();
 *   }
 *
 *   public void unlock() {
 *     locked.set(false);
 *     LockSupport.unpark(waiters.peek());
 *   }
 * }}
 */
/**
 * A.
 * Basic thread blocking primitives for creating locks and other
 * synchronization classes.
 *
 * B.
 * <p>This class associates, with each thread that uses it, a permit
 * (in the sense of the {@link java.util.concurrent.Semaphore
 * Semaphore} class). A call to {@code park} will return immediately
 * if the permit is available, consuming it in the process; otherwise
 * it <em>may</em> block.  A call to {@code unpark} makes the permit
 * available, if it was not already available. (Unlike with Semaphores
 * though, permits do not accumulate. There is at most one.)
 *
 * C.
 * <p>Methods {@code park} and {@code unpark} provide efficient
 * means of blocking and unblocking threads that do not encounter the
 * problems that cause the deprecated methods {@code Thread.suspend}
 * and {@code Thread.resume} to be unusable for such purposes: Races
 * between one thread invoking {@code park} and another thread trying
 * to {@code unpark} it will preserve liveness, due to the
 * permit. Additionally, {@code park} will return if the caller's
 * thread was interrupted, and timeout versions are supported. The
 * {@code park} method may also return at any other time, for "no
 * reason", so in general must be invoked within a loop that rechecks
 * conditions upon return. In this sense {@code park} serves as an
 * optimization of a "busy wait" that does not waste as much time
 * spinning, but must be paired with an {@code unpark} to be
 * effective.
 *
 * D.
 * <p>The three forms of {@code park} each also support a
 * {@code blocker} object parameter. This object is recorded while
 * the thread is blocked to permit monitoring and diagnostic tools to
 * identify the reasons that threads are blocked. (Such tools may
 * access blockers using method {@link #getBlocker(Thread)}.)
 * The use of these forms rather than the original forms without this
 * parameter is strongly encouraged. The normal argument to supply as
 * a {@code blocker} within a lock implementation is {@code this}.
 *
 * E.
 * <p>These methods are designed to be used as tools for creating
 * higher-level synchronization utilities, and are not in themselves
 * useful for most concurrency control applications.  The {@code park}
 * method is designed for use only in constructions of the form:
 *
 *  <pre> {@code
 * while (!canProceed()) { ... LockSupport.park(this); }}</pre>
 *
 * where neither {@code canProceed} nor any other actions prior to the
 * call to {@code park} entail locking or blocking.  Because only one
 * permit is associated with each thread, any intermediary uses of
 * {@code park} could interfere with its intended effects.
 *
 * F.
 * <p><b>Sample Usage.</b> Here is a sketch of a first-in-first-out
 * non-reentrant lock class:
 *  <pre> {@code
 * class FIFOMutex {
 *   private final AtomicBoolean locked = new AtomicBoolean(false);
 *   private final Queue<Thread> waiters
 *     = new ConcurrentLinkedQueue<Thread>();
 *
 *   public void lock() {
 *     boolean wasInterrupted = false;
 *     Thread current = Thread.currentThread();
 *     waiters.add(current);
 *
 *     // Block while not first in queue or cannot acquire lock
 *     while (waiters.peek() != current ||
 *            !locked.compareAndSet(false, true)) {
 *       LockSupport.park(this);
 *       if (Thread.interrupted()) // ignore interrupts while waiting
 *         wasInterrupted = true;
 *     }
 *
 *     waiters.remove();
 *     if (wasInterrupted)          // reassert interrupt status on exit
 *       current.interrupt();
 *   }
 *
 *   public void unlock() {
 *     locked.set(false);
 *     LockSupport.unpark(waiters.peek());
 *   }
 * }}</pre>
 */
public class LockSupport {

    private LockSupport() {} // Cannot be instantiated. 无法实例化。

    // 设置负责指定线程阻塞的同步对象(通常使用this), 该对象会在线程被阻塞时被记录, 以允许监控和诊断工具识别线程被阻塞的原因, 强烈建议这种形式而不是没有此参数的原始形式
    private static void setBlocker(Thread t, Object arg) {
        // 即使不稳定，热点在这里也不需要写屏障。
        // Even though volatile, hotspot doesn't need a write barrier here.

        // 根据对象和偏移量, 将x值存储到Java变量中, 其字段类型必须为Object类型
        UNSAFE.putObject(t, parkBlockerOffset, arg);
    }

    /**
     * 20210728
     * 使给定线程的许可可用（如果它尚不可用）。如果线程在{@code park}上被阻塞，那么它将解除阻塞。否则，它对{@code park}的下一次调用保证不会阻塞。
     * 如果给定的线程尚未启动，则无法保证此操作有任何效果。
     */
    /**
     * Makes available the permit for the given thread, if it
     * was not already available.  If the thread was blocked on
     * {@code park} then it will unblock.  Otherwise, its next call
     * to {@code park} is guaranteed not to block. This operation
     * is not guaranteed to have any effect at all if the given
     * thread has not been started.
     *
     * // 将线程线程化以解除停放，或 {@code null}，在这种情况下此操作无效
     * @param thread the thread to unpark, or {@code null}, in which case
     *        this operation has no effect
     */
    // 发放许可给指定的线程, 如果该线程已经在park上被阻塞, 则立即解除阻塞; 否则会保证对它的下一次park调用不会进行阻塞; 但是如果该线程未启动, 则此操作没有任何效果
    public static void unpark(Thread thread) {
        if (thread != null)
            // 唤醒在park上阻塞的指定线程, 如果该线程并未阻塞, 则它在后续调用park时不会被阻塞
            UNSAFE.unpark(thread);
    }

    /**
     * 20210728
     * A. 除非许可可用，否则出于线程调度目的禁用当前线程。
     * B. 如果许可可用，则它被消耗并且调用立即返回； 否则，当前线程将因线程调度目的而被禁用并处于休眠状态，直到发生以下三种情况之一：
     *      a. 其他一些线程以当前线程为目标调用 {@link #unpark unpark}；
     *      b. 其他一些线程 {@linkplain Thread#interrupt 中断}当前线程；
     *      c. 虚假调用（即，无缘无故）返回。
     * C. 此方法不报告其中哪些导致方法返回。 调用者应该首先重新检查导致线程停放的条件。 例如，调用者还可以确定线程在返回时的中断状态。
     */
    /**
     * A.
     * Disables the current thread for thread scheduling purposes unless the
     * permit is available.
     *
     * B.
     * <p>If the permit is available then it is consumed and the call returns
     * immediately; otherwise
     * the current thread becomes disabled for thread scheduling
     * purposes and lies dormant until one of three things happens:
     *
     * <ul>
     * <li>Some other thread invokes {@link #unpark unpark} with the
     * current thread as the target; or
     *
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * the current thread; or
     *
     * <li>The call spuriously (that is, for no reason) returns.
     * </ul>
     *
     * C.
     * <p>This method does <em>not</em> report which of these caused the
     * method to return. Callers should re-check the conditions which caused
     * the thread to park in the first place. Callers may also determine,
     * for example, the interrupt status of the thread upon return.
     *
     * // 负责此线程停放的同步对象
     * @param blocker the synchronization object responsible for this thread parking
     * @since 1.6
     */
    // 无限阻塞当前线程, 直到当前线程unpark被调用、被中断、time时间过去(非绝对时为纳秒, 绝对时为毫秒, 为0时代表无限阻塞)
    public static void park(Object blocker) {
        // 获取当前线程
        Thread t = Thread.currentThread();

        // 设置负责指定线程阻塞的同步对象(通常使用this), 该对象会在线程被阻塞时被记录, 以允许监控和诊断工具识别线程被阻塞的原因, 强烈建议这种形式而不是没有此参数的原始形式
        setBlocker(t, blocker);

        // 阻塞当前线程, 直到当前线程unpark被调用、被中断、time时间过去(非绝对时为纳秒, 绝对时为毫秒, 为0时代表无限阻塞)
        UNSAFE.park(false, 0L);

        // 唤醒后清空负责指定线程阻塞的同步对象(通常使用this), 该对象会在线程被阻塞时被记录, 以允许监控和诊断工具识别线程被阻塞的原因, 强烈建议这种形式而不是没有此参数的原始形式
        setBlocker(t, null);
    }

    /**
     * 20210728
     * A. 为线程调度目的禁用当前线程，直至指定的等待时间，除非许可可用。
     * B. 如果许可可用，则它被消耗并且调用立即返回； 否则，当前线程将因线程调度目的而被禁用，并处于休眠状态，直到发生以下四种情况之一：
     *      a. 其他一些线程以当前线程为目标调用 {@link #unpark unpark}；
     *      b. 其他一些线程 {@linkplain Thread#interrupt}中断当前线程；
     *      c. 指定的等待时间过去；
     *      d. 虚假调用（即，无缘无故）返回。
     * C. 此方法不报告其中哪些导致方法返回。调用者应该首先重新检查导致线程停放的条件。例如，调用者还可以确定线程的中断状态或返回时经过的时间。
     */
    /**
     * A.
     * Disables the current thread for thread scheduling purposes, for up to
     * the specified waiting time, unless the permit is available.
     *
     * B.
     * <p>If the permit is available then it is consumed and the call
     * returns immediately; otherwise the current thread becomes disabled
     * for thread scheduling purposes and lies dormant until one of four
     * things happens:
     *
     * <ul>
     * <li>Some other thread invokes {@link #unpark unpark} with the
     * current thread as the target; or
     *
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * the current thread; or
     *
     * <li>The specified waiting time elapses; or
     *
     * <li>The call spuriously (that is, for no reason) returns.
     * </ul>
     *
     * C.
     * <p>This method does <em>not</em> report which of these caused the
     * method to return. Callers should re-check the conditions which caused
     * the thread to park in the first place. Callers may also determine,
     * for example, the interrupt status of the thread, or the elapsed time
     * upon return.
     *
     * // 负责此线程停放的同步对象
     * @param blocker the synchronization object responsible for this thread parking
     *
     * // 等待的最大纳秒数
     * @param nanos the maximum number of nanoseconds to wait
     * @since 1.6
     */
    // 在指定的等待时间内阻塞当前线程, 直到当前线程unpark被调用、被中断、time时间过去(非绝对时为纳秒, 绝对时为毫秒, 为0时代表无限阻塞)
    public static void parkNanos(Object blocker, long nanos) {
        if (nanos > 0) {
            // 获取当前线程
            Thread t = Thread.currentThread();

            // 设置负责指定线程阻塞的同步对象(通常使用this), 该对象会在线程被阻塞时被记录, 以允许监控和诊断工具识别线程被阻塞的原因, 强烈建议这种形式而不是没有此参数的原始形式
            setBlocker(t, blocker);

            // 阻塞当前线程, 直到当前线程unpark被调用、被中断、time时间过去(非绝对时为纳秒, 绝对时为毫秒, 为0时代表无限阻塞)
            UNSAFE.park(false, nanos);

            // 唤醒后清空负责指定线程阻塞的同步对象(通常使用this), 该对象会在线程被阻塞时被记录, 以允许监控和诊断工具识别线程被阻塞的原因, 强烈建议这种形式而不是没有此参数的原始形式
            setBlocker(t, null);
        }
    }

    /**
     * 20210728
     * A. 出于线程调度目的禁用当前线程，直到指定的截止日期，除非许可可用。
     * B. 如果许可可用，则它被消耗并且调用立即返回； 否则，当前线程将因线程调度目的而被禁用，并处于休眠状态，直到发生以下四种情况之一：
     *      a. 其他一些线程以当前线程为目标调用{@link #unpark unpark}；
     *      b. 其他一些线程{@linkplain Thread#interrupt}中断当前线程；
     *      c. 指定的截止日期已过；
     *      d. 虚假调用（即，无缘无故）返回。
     * C. 此方法不报告其中哪些导致方法返回。 调用者应该首先重新检查导致线程停放的条件。例如，调用者还可以确定线程的中断状态或返回时的当前时间。
     */
    /**
     * A.
     * Disables the current thread for thread scheduling purposes, until
     * the specified deadline, unless the permit is available.
     *
     * B.
     * <p>If the permit is available then it is consumed and the call
     * returns immediately; otherwise the current thread becomes disabled
     * for thread scheduling purposes and lies dormant until one of four
     * things happens:
     *
     * <ul>
     * <li>Some other thread invokes {@link #unpark unpark} with the
     * current thread as the target; or
     *
     * <li>Some other thread {@linkplain Thread#interrupt interrupts} the
     * current thread; or
     *
     * <li>The specified deadline passes; or
     *
     * <li>The call spuriously (that is, for no reason) returns.
     * </ul>
     *
     * C.
     * <p>This method does <em>not</em> report which of these caused the
     * method to return. Callers should re-check the conditions which caused
     * the thread to park in the first place. Callers may also determine,
     * for example, the interrupt status of the thread, or the current time
     * upon return.
     *
     * // 负责此线程停放的同步对象
     * @param blocker the synchronization object responsible for this thread parking
     *
     * // 绝对时间，以从纪元开始的毫秒数，等待
     * @param deadline the absolute time, in milliseconds from the Epoch, to wait until
     * @since 1.6
     */
    // 在绝对时间前阻塞当前线程, 直到当前线程unpark被调用、被中断、time时间过去(非绝对时为纳秒, 绝对时为毫秒, 为0时代表无限阻塞)
    public static void parkUntil(Object blocker, long deadline) {
        // 获取当前线程
        Thread t = Thread.currentThread();

        // 设置负责指定线程阻塞的同步对象(通常使用this), 该对象会在线程被阻塞时被记录, 以允许监控和诊断工具识别线程被阻塞的原因, 强烈建议这种形式而不是没有此参数的原始形式
        setBlocker(t, blocker);

        // 阻塞当前线程, 直到当前线程unpark被调用、被中断、time时间过去(非绝对时为纳秒, 绝对时为毫秒, 为0时代表无限阻塞)
        UNSAFE.park(true, deadline);

        // 唤醒后清空负责指定线程阻塞的同步对象(通常使用this), 该对象会在线程被阻塞时被记录, 以允许监控和诊断工具识别线程被阻塞的原因, 强烈建议这种形式而不是没有此参数的原始形式
        setBlocker(t, null);
    }

    /**
     * 20210728
     * 返回提供给尚未解除阻塞的park方法的最近调用的阻塞程序对象，如果未阻塞，则返回null。
     * 返回的值只是一个瞬间的快照——线程可能已经在不同的阻塞器对象上解除阻塞或阻塞。
     */
    /**
     * Returns the blocker object supplied to the most recent
     * invocation of a park method that has not yet unblocked, or null
     * if not blocked.  The value returned is just a momentary
     * snapshot -- the thread may have since unblocked or blocked on a
     * different blocker object.
     *
     * @param t the thread
     * @return the blocker
     * @throws NullPointerException if argument is null
     * @since 1.6
     */
    // 返回提供给尚未解除park阻塞方法的最近调用的同步对象快照, 如果未阻塞, 则返回null: 该对象会在线程被阻塞时被记录, 以允许监控和诊断工具识别线程被阻塞的原因, 强烈建议这种形式而不是没有此参数的原始形式
    public static Object getBlocker(Thread t) {
        if (t == null)
            throw new NullPointerException();
        return UNSAFE.getObjectVolatile(t, parkBlockerOffset);
    }

    /**
     * 20210728
     * A. 除非许可可用，否则出于线程调度目的禁用当前线程。
     * B. 如果许可可用，则它被消耗并且调用立即返回； 否则，当前线程将因线程调度目的而被禁用并处于休眠状态，直到发生以下三种情况之一：
     *      a. 其他一些线程以当前线程为目标调用 {@link #unpark unpark}；
     *      b. 其他一些线程 {@linkplain Thread#interrupt}中断当前线程；
     *      c. 虚假调用（即，无缘无故）返回。
     * C. 此方法不报告其中哪些导致方法返回。 调用者应该首先重新检查导致线程停放的条件。 例如，调用者还可以确定线程在返回时的中断状态。
     */
    /**
     * A.
     * Disables the current thread for thread scheduling purposes unless the
     * permit is available.
     *
     * B.
     * <p>If the permit is available then it is consumed and the call
     * returns immediately; otherwise the current thread becomes disabled
     * for thread scheduling purposes and lies dormant until one of three
     * things happens:
     *
     * <ul>
     *
     * <li>Some other thread invokes {@link #unpark unpark} with the
     * current thread as the target; or
     *
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * the current thread; or
     *
     * <li>The call spuriously (that is, for no reason) returns.
     * </ul>
     *
     * C.
     * <p>This method does <em>not</em> report which of these caused the
     * method to return. Callers should re-check the conditions which caused
     * the thread to park in the first place. Callers may also determine,
     * for example, the interrupt status of the thread upon return.
     */
    // 无同步对象记录方式地无限阻塞当前线程(不建议使用), 直到当前线程unpark被调用、被中断、time时间过去(非绝对时为纳秒, 绝对时为毫秒, 为0时代表无限阻塞)
    public static void park() {
        UNSAFE.park(false, 0L);
    }

    /**
     * 20210728
     * A. 为线程调度目的禁用当前线程，直至指定的等待时间，除非许可可用。
     * B. 如果许可可用，则它被消耗并且调用立即返回； 否则，当前线程将因线程调度目的而被禁用，并处于休眠状态，直到发生以下四种情况之一：
     *      a. 其他一些线程以当前线程为目标调用 {@link #unpark unpark}；
     *      b. 其他一些线程 {@linkplain Thread#interrupt}中断当前线程；
     *      c. 指定的等待时间过去；
     *      d. 虚假调用（即，无缘无故）返回。
     * C. 此方法不报告其中哪些导致方法返回。调用者应该首先重新检查导致线程停放的条件。例如，调用者还可以确定线程的中断状态或返回时经过的时间。
     */
    /**
     * A.
     * Disables the current thread for thread scheduling purposes, for up to
     * the specified waiting time, unless the permit is available.
     *
     * B.
     * <p>If the permit is available then it is consumed and the call
     * returns immediately; otherwise the current thread becomes disabled
     * for thread scheduling purposes and lies dormant until one of four
     * things happens:
     *
     * <ul>
     * <li>Some other thread invokes {@link #unpark unpark} with the
     * current thread as the target; or
     *
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * the current thread; or
     *
     * <li>The specified waiting time elapses; or
     *
     * <li>The call spuriously (that is, for no reason) returns.
     * </ul>
     *
     * C.
     * <p>This method does <em>not</em> report which of these caused the
     * method to return. Callers should re-check the conditions which caused
     * the thread to park in the first place. Callers may also determine,
     * for example, the interrupt status of the thread, or the elapsed time
     * upon return.
     *
     * @param nanos the maximum number of nanoseconds to wait
     */
    // 无同步对象记录方式地在指定的等待时间内阻塞当前线程(不建议使用), 直到当前线程unpark被调用、被中断、time时间过去(非绝对时为纳秒, 绝对时为毫秒, 为0时代表无限阻塞)
    public static void parkNanos(long nanos) {
        if (nanos > 0)
            UNSAFE.park(false, nanos);
    }

    /**
     * 20210728
     * A. 出于线程调度目的禁用当前线程，直到指定的截止日期，除非许可可用。
     * B. 如果许可可用，则它被消耗并且调用立即返回； 否则，当前线程将因线程调度目的而被禁用，并处于休眠状态，直到发生以下四种情况之一：
     *      a. 其他一些线程以当前线程为目标调用 {@link #unpark unpark}；
     *      b. 其他一些线程 {@linkplain Thread#interrupt interrupts} 当前线程；
     *      c. 指定的截止日期已过；
     *      d. 虚假调用（即，无缘无故）返回。
     * C. 此方法不报告其中哪些导致方法返回。 调用者应该首先重新检查导致线程停放的条件。 例如，调用者还可以确定线程的中断状态或返回时的当前时间。
     */
    /**
     * A.
     * Disables the current thread for thread scheduling purposes, until
     * the specified deadline, unless the permit is available.
     *
     * B.
     * <p>If the permit is available then it is consumed and the call
     * returns immediately; otherwise the current thread becomes disabled
     * for thread scheduling purposes and lies dormant until one of four
     * things happens:
     *
     * <ul>
     * <li>Some other thread invokes {@link #unpark unpark} with the
     * current thread as the target; or
     *
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * the current thread; or
     *
     * <li>The specified deadline passes; or
     *
     * <li>The call spuriously (that is, for no reason) returns.
     * </ul>
     *
     * C.
     * <p>This method does <em>not</em> report which of these caused the
     * method to return. Callers should re-check the conditions which caused
     * the thread to park in the first place. Callers may also determine,
     * for example, the interrupt status of the thread, or the current time
     * upon return.
     *
     * @param deadline the absolute time, in milliseconds from the Epoch,
     *        to wait until
     */
    // 无同步对象记录方式地在绝对时间前阻塞当前线程阻塞当前线程(不建议使用), 直到当前线程unpark被调用、被中断、time时间过去(非绝对时为纳秒, 绝对时为毫秒, 为0时代表无限阻塞)
    public static void parkUntil(long deadline) {
        UNSAFE.park(true, deadline);
    }

    /**
     * Returns the pseudo-randomly initialized or updated secondary seed.
     * Copied from ThreadLocalRandom due to package access restrictions.
     */
    // 返回伪随机初始化或更新的辅助种子。由于包访问限制，从ThreadLocalRandom复制。
    static final int nextSecondarySeed() {
        int r;
        Thread t = Thread.currentThread();
        if ((r = UNSAFE.getInt(t, SECONDARY)) != 0) {
            r ^= r << 13;   // xorshift
            r ^= r >>> 17;
            r ^= r << 5;
        }
        else if ((r = java.util.concurrent.ThreadLocalRandom.current().nextInt()) == 0)
            r = 1; // avoid zero
        UNSAFE.putInt(t, SECONDARY, r);
        return r;
    }

    // 通过内在 API 实现热点
    // Hotspot implementation via intrinsics API
    private static final sun.misc.Unsafe UNSAFE;
    private static final long parkBlockerOffset;// Thread.parkBlocker
    private static final long SEED;// threadLocalRandomSeed
    private static final long PROBE;// threadLocalRandomProbe
    private static final long SECONDARY;// threadLocalRandomSecondarySeed
    static {
        try {
            UNSAFE = sun.misc.Unsafe.getUnsafe();
            Class<?> tk = Thread.class;
            parkBlockerOffset = UNSAFE.objectFieldOffset
                (tk.getDeclaredField("parkBlocker"));
            SEED = UNSAFE.objectFieldOffset
                (tk.getDeclaredField("threadLocalRandomSeed"));
            PROBE = UNSAFE.objectFieldOffset
                (tk.getDeclaredField("threadLocalRandomProbe"));
            SECONDARY = UNSAFE.objectFieldOffset
                (tk.getDeclaredField("threadLocalRandomSecondarySeed"));
        } catch (Exception ex) { throw new Error(ex); }
    }

}
