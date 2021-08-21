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

/**
 * 20210812
 * A. 一种同步辅助，允许一个或多个线程等待，直到在其他线程中执行的一组操作完成。
 * B. {@code CountDownLatch} 使用给定的计数进行初始化。{@link #await await} 方法阻塞，直到当前计数由于 {@link #countDown} 方法的调用而达到零，
 *    之后所有等待线程都被释放并且 {@link #await await} 的任何后续调用立即返回 . 这是一种一次性现象——计数无法重置。
 *    如果您需要重置计数的版本，请考虑使用 {@link CyclicBarrier}。
 * C. {@code CountDownLatch} 是一种多功能同步工具，可用于多种用途。 初始化为 1 的 {@code CountDownLatch} 用作简单的开/关锁存器或门：
 *    所有调用 {@link #await await} 的线程在门处等待，直到它被调用 {@link # 的线程打开 倒数}。 初始化为 N 的 {@code CountDownLatch} 可用于使一个线程等待，
 *    直到 N 个线程完成某个操作，或者某个操作已完成 N 次。
 * D. {@code CountDownLatch} 的一个有用属性是它不需要调用 {@code countDown} 的线程在继续之前等待计数达到零，
 *    它只是阻止任何线程通过 {@link #await await} 直到所有线程都可以通过。
 * E. 示例用法：这是一对类，其中一组工作线程使用两个倒计时闩锁：
 *      a. 第一个是启动信号，它阻止任何工人继续，直到驱动程序准备好让他们继续；
 *      b. 第二个是完成信号，它允许驱动程序等待所有工人完成。
 * {@code
 * class Driver { // ...
 *   void main() throws InterruptedException {
 *     CountDownLatch startSignal = new CountDownLatch(1);
 *     CountDownLatch doneSignal = new CountDownLatch(N);
 *
 *     for (int i = 0; i < N; ++i) // create and start threads
 *       new Thread(new Worker(startSignal, doneSignal)).start();
 *
 *     doSomethingElse();            // don't let run yet
 *     startSignal.countDown();      // let all threads proceed
 *     doSomethingElse();
 *     doneSignal.await();           // wait for all to finish
 *   }
 * }
 *
 * class Worker implements Runnable {
 *   private final CountDownLatch startSignal;
 *   private final CountDownLatch doneSignal;
 *   Worker(CountDownLatch startSignal, CountDownLatch doneSignal) {
 *     this.startSignal = startSignal;
 *     this.doneSignal = doneSignal;
 *   }
 *   public void run() {
 *     try {
 *       startSignal.await();
 *       doWork();
 *       doneSignal.countDown();
 *     } catch (InterruptedException ex) {} // return;
 *   }
 *
 *   void doWork() { ... }
 * }}
 * F. 另一个典型的用法是将问题分成 N 个部分，用一个 Runnable 描述每个部分，该 Runnable 执行该部分并在闩锁上倒计时，并将所有 Runnable 排到一个 Executor 中。
 *    当所有子部分完成后，协调线程将能够通过await。 （当线程必须以这种方式重复倒计时时，请改用 {@link CyclicBarrier}。）
 * {@code
 * class Driver2 { // ...
 *   void main() throws InterruptedException {
 *     CountDownLatch doneSignal = new CountDownLatch(N);
 *     Executor e = ...
 *
 *     for (int i = 0; i < N; ++i) // create and start threads
 *       e.execute(new WorkerRunnable(doneSignal, i));
 *
 *     doneSignal.await();           // wait for all to finish
 *   }
 * }
 *
 * class WorkerRunnable implements Runnable {
 *   private final CountDownLatch doneSignal;
 *   private final int i;
 *   WorkerRunnable(CountDownLatch doneSignal, int i) {
 *     this.doneSignal = doneSignal;
 *     this.i = i;
 *   }
 *   public void run() {
 *     try {
 *       doWork(i);
 *       doneSignal.countDown();
 *     } catch (InterruptedException ex) {} // return;
 *   }
 *
 *   void doWork() { ... }
 * }}
 * G. 内存一致性影响：在计数达到零之前，调用 {@code countDown()} 之前的线程中的操作发生在从另一个线程中相应的 {@code await()} 成功返回之后的操作之前。
 */
/**
 * A.
 * A synchronization aid that allows one or more threads to wait until
 * a set of operations being performed in other threads completes.
 *
 * B.
 * <p>A {@code CountDownLatch} is initialized with a given <em>count</em>.
 * The {@link #await await} methods block until the current count reaches
 * zero due to invocations of the {@link #countDown} method, after which
 * all waiting threads are released and any subsequent invocations of
 * {@link #await await} return immediately.  This is a one-shot phenomenon
 * -- the count cannot be reset.  If you need a version that resets the
 * count, consider using a {@link CyclicBarrier}.
 *
 * C.
 * <p>A {@code CountDownLatch} is a versatile synchronization tool
 * and can be used for a number of purposes.  A
 * {@code CountDownLatch} initialized with a count of one serves as a
 * simple on/off latch, or gate: all threads invoking {@link #await await}
 * wait at the gate until it is opened by a thread invoking {@link
 * #countDown}.  A {@code CountDownLatch} initialized to <em>N</em>
 * can be used to make one thread wait until <em>N</em> threads have
 * completed some action, or some action has been completed N times.
 *
 * D.
 * <p>A useful property of a {@code CountDownLatch} is that it
 * doesn't require that threads calling {@code countDown} wait for
 * the count to reach zero before proceeding, it simply prevents any
 * thread from proceeding past an {@link #await await} until all
 * threads could pass.
 *
 * E.
 * <p><b>Sample usage:</b> Here is a pair of classes in which a group
 * of worker threads use two countdown latches:
 * <ul>
 * <li>The first is a start signal that prevents any worker from proceeding
 * until the driver is ready for them to proceed;
 * <li>The second is a completion signal that allows the driver to wait
 * until all workers have completed.
 * </ul>
 *
 *  <pre> {@code
 * class Driver { // ...
 *   void main() throws InterruptedException {
 *     CountDownLatch startSignal = new CountDownLatch(1);
 *     CountDownLatch doneSignal = new CountDownLatch(N);
 *
 *     for (int i = 0; i < N; ++i) // create and start threads
 *       new Thread(new Worker(startSignal, doneSignal)).start();
 *
 *     doSomethingElse();            // don't let run yet
 *     startSignal.countDown();      // let all threads proceed
 *     doSomethingElse();
 *     doneSignal.await();           // wait for all to finish
 *   }
 * }
 *
 * class Worker implements Runnable {
 *   private final CountDownLatch startSignal;
 *   private final CountDownLatch doneSignal;
 *   Worker(CountDownLatch startSignal, CountDownLatch doneSignal) {
 *     this.startSignal = startSignal;
 *     this.doneSignal = doneSignal;
 *   }
 *   public void run() {
 *     try {
 *       startSignal.await();
 *       doWork();
 *       doneSignal.countDown();
 *     } catch (InterruptedException ex) {} // return;
 *   }
 *
 *   void doWork() { ... }
 * }}</pre>
 *
 * F.
 * <p>Another typical usage would be to divide a problem into N parts,
 * describe each part with a Runnable that executes that portion and
 * counts down on the latch, and queue all the Runnables to an
 * Executor.  When all sub-parts are complete, the coordinating thread
 * will be able to pass through await. (When threads must repeatedly
 * count down in this way, instead use a {@link CyclicBarrier}.)
 *
 *  <pre> {@code
 * class Driver2 { // ...
 *   void main() throws InterruptedException {
 *     CountDownLatch doneSignal = new CountDownLatch(N);
 *     Executor e = ...
 *
 *     for (int i = 0; i < N; ++i) // create and start threads
 *       e.execute(new WorkerRunnable(doneSignal, i));
 *
 *     doneSignal.await();           // wait for all to finish
 *   }
 * }
 *
 * class WorkerRunnable implements Runnable {
 *   private final CountDownLatch doneSignal;
 *   private final int i;
 *   WorkerRunnable(CountDownLatch doneSignal, int i) {
 *     this.doneSignal = doneSignal;
 *     this.i = i;
 *   }
 *   public void run() {
 *     try {
 *       doWork(i);
 *       doneSignal.countDown();
 *     } catch (InterruptedException ex) {} // return;
 *   }
 *
 *   void doWork() { ... }
 * }}</pre>
 *
 * G.
 * <p>Memory consistency effects: Until the count reaches
 * zero, actions in a thread prior to calling
 * {@code countDown()}
 * <a href="package-summary.html#MemoryVisibility"><i>happen-before</i></a>
 * actions following a successful return from a corresponding
 * {@code await()} in another thread.
 *
 * @since 1.5
 * @author Doug Lea
 */
public class CountDownLatch {

    // CountDownLatch 的同步控制。 使用 AQS 状态来表示计数。
    /**
     * Synchronization control For CountDownLatch.
     * Uses AQS state to represent count.
     */
    // CountDownLatch的同步控制对象, 它使用AQS状态来表示计数
    private static final class Sync extends AbstractQueuedSynchronizer {

        private static final long serialVersionUID = 4982264981922014374L;

        // 构造CountDownLatch的同步控制对象实例, 使用传入的计数作为同步器状态
        Sync(int count) {
            setState(count);
        }

        // 获取计数, 使用同步器状态作为计数
        int getCount() {
            return getState();
        }

        // 尝试以共享模式获取同步器状态, 失败(<0), 获取成功但后续共享不成功(=0), 获取成功且后续共享成功(>0)
        protected int tryAcquireShared(int acquires) {
            return (getState() == 0) ? 1 : -1;
        }

        // 尝试释放共享模式的同步器状态
        protected boolean tryReleaseShared(int releases) {
            // 递减计数； 过渡到零时的信号
            // Decrement count; signal when transition to zero
            for (;;) {
                int c = getState();
                if (c == 0)
                    return false;
                int nextc = c-1;
                if (compareAndSetState(c, nextc))
                    return nextc == 0;
            }
        }
    }

    private final Sync sync;// 同步执行对象

    // 构造一个用给定计数初始化的 {@code CountDownLatch}。
    /**
     * Constructs a {@code CountDownLatch} initialized with the given count.
     *
     * @param count the number of times {@link #countDown} must be invoked
     *        before threads can pass through {@link #await}
     * @throws IllegalArgumentException if {@code count} is negative
     */
    // 构造一个用给定计数初始化的{@code CountDownLatch}实例
    public CountDownLatch(int count) {
        if (count < 0) throw new IllegalArgumentException("count < 0");
        this.sync = new Sync(count);
    }

    /**
     * 20210812
     * A. 导致当前线程等待直到闩锁倒计时为零，除非线程是{@linkplain Thread#interrupt interrupted}。
     * B. 如果当前计数为零，则此方法立即返回。
     * C. 如果当前计数大于零，则当前线程出于线程调度目的而被禁用并处于休眠状态，直到发生以下两种情况之一：
     *      a. 由于调用了 {@link #countDown} 方法，计数达到零；
     *      b. 其他一些线程 {@linkplain Thread#interrupt 中断}当前线程。
     * D. 如果当前线程：
     *      a. 在进入此方法时设置其中断状态；
     *      b. 等待时{@linkplain Thread#interrupt interrupted}；
     *    然后抛出 {@link InterruptedException} 并清除当前线程的中断状态。
     */
    /**
     * A.
     * Causes the current thread to wait until the latch has counted down to
     * zero, unless the thread is {@linkplain Thread#interrupt interrupted}.
     *
     * B.
     * <p>If the current count is zero then this method returns immediately.
     *
     * C.
     * <p>If the current count is greater than zero then the current
     * thread becomes disabled for thread scheduling purposes and lies
     * dormant until one of two things happen:
     * <ul>
     * <li>The count reaches zero due to invocations of the
     * {@link #countDown} method; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * the current thread.
     * </ul>
     *
     * D.
     * <p>If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@linkplain Thread#interrupt interrupted} while waiting,
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     *
     * @throws InterruptedException if the current thread is interrupted
     *         while waiting
     */
    // 以阻塞、共享、可中断模式获取同步器状态, 直到当前线程等待直到闩锁倒计时为零, 或者线程被中断
    public void await() throws InterruptedException {
        sync.acquireSharedInterruptibly(1);
    }

    /**
     * 20210812
     * A. 导致当前线程等待直到闩锁倒计时到零，除非线程{@linkplain Thread#interrupt interrupted}，或者指定的等待时间已过。
     * B. 如果当前计数为零，则此方法立即返回值 {@code true}。
     * C. 如果当前计数大于零，则当前线程出于线程调度目的而被禁用并处于休眠状态，直到发生以下三种情况之一：
     *      a. 因为调用了 {@link #countDown} 方法，计数达到零；
     *      b. 其他一些线程 {@linkplain Thread#interrupt 中断}当前线程；
     *      c. 指定的等待时间已过。
     * D. 如果计数达到零，则该方法返回值 {@code true}。
     * E. 如果当前线程：
     *      a. 在进入此方法时设置其中断状态；
     *      b. 等待时{@linkplain Thread#interrupt interrupted}；
     *    然后抛出 {@link InterruptedException} 并清除当前线程的中断状态。
     * F. 如果指定的等待时间过去，则返回值 {@code false}。 如果时间小于或等于零，则该方法根本不会等待。
     */
    /**
     * A.
     * Causes the current thread to wait until the latch has counted down to
     * zero, unless the thread is {@linkplain Thread#interrupt interrupted},
     * or the specified waiting time elapses.
     *
     * B.
     * <p>If the current count is zero then this method returns immediately
     * with the value {@code true}.
     *
     * C.
     * <p>If the current count is greater than zero then the current
     * thread becomes disabled for thread scheduling purposes and lies
     * dormant until one of three things happen:
     * <ul>
     * <li>The count reaches zero due to invocations of the
     * {@link #countDown} method; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * the current thread; or
     * <li>The specified waiting time elapses.
     * </ul>
     *
     * D.
     * <p>If the count reaches zero then the method returns with the
     * value {@code true}.
     *
     * E.
     * <p>If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@linkplain Thread#interrupt interrupted} while waiting,
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     *
     * F.
     * <p>If the specified waiting time elapses then the value {@code false}
     * is returned.  If the time is less than or equal to zero, the method
     * will not wait at all.
     *
     * @param timeout the maximum time to wait
     * @param unit the time unit of the {@code timeout} argument
     * @return {@code true} if the count reached zero and {@code false}
     *         if the waiting time elapsed before the count reached zero
     * @throws InterruptedException if the current thread is interrupted
     *         while waiting
     */
    // 以阻塞、共享、定时、可中断模式获取同步器状态, 直到当前线程等待直到闩锁倒计时为零, 或者线程被中断, 或者指定的等待时间已过
    public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
        return sync.tryAcquireSharedNanos(1, unit.toNanos(timeout));
    }

    /**
     * 20210812
     * A. 递减锁存器的计数，如果计数达到零，则释放所有等待的线程。
     * B. 如果当前计数大于零，则递减。 如果新计数为零，则为线程调度目的重新启用所有等待线程。
     * C. 如果当前计数为零，则什么也不会发生。
     */
    /**
     * A.
     * Decrements the count of the latch, releasing all waiting threads if
     * the count reaches zero.
     *
     * B.
     * <p>If the current count is greater than zero then it is decremented.
     * If the new count is zero then all waiting threads are re-enabled for
     * thread scheduling purposes.
     *
     * C.
     * <p>If the current count equals zero then nothing happens.
     */
    // 如果当前计数大于零, 则以共享模式递减锁存器的计数; 如果计数达到0, 则释放所有等待的线程; 如果当时计数为0, 则什么也不会发生
    public void countDown() {
        sync.releaseShared(1);
    }

    /**
     * 20210812
     * A. 返回当前计数。
     * B. 此方法通常用于调试和测试目的。
     */
    /**
     * A.
     * Returns the current count.
     *
     * B.
     * <p>This method is typically used for debugging and testing purposes.
     *
     * @return the current count
     */
    // 返回当前计数, 通常用于调试和测试目的
    public long getCount() {
        return sync.getCount();
    }

    // 返回标识此闩锁及其状态的字符串。 括号中的状态包括字符串 {@code "Count ="} 后跟当前计数。
    /**
     * Returns a string identifying this latch, as well as its state.
     * The state, in brackets, includes the String {@code "Count ="}
     * followed by the current count.
     *
     * @return a string identifying this latch, as well as its state
     */
    public String toString() {
        return super.toString() + "[Count = " + sync.getCount() + "]";
    }
}
