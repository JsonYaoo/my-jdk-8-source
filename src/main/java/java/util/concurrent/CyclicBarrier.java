/*
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util.concurrent;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 20210812
 * A. 一种同步辅助工具，它允许一组线程全部等待彼此到达公共屏障点。 CyclicBarriers 在涉及固定大小的线程组的程序中很有用，这些线程必须偶尔相互等待。
 *    屏障被称为循环的，因为它可以在等待线程被释放后重新使用。
 * B. {@code CyclicBarrier} 支持一个可选的 {@link Runnable} 命令，该命令在每个屏障点运行一次，在派对中的最后一个线程到达之后，但在任何线程被释放之前。
 *    此屏障操作对于在任何一方继续之前更新共享状态很有用。
 * C. 示例用法：以下是在并行分解设计中使用屏障的示例：
 * {@code
 * class Solver {
 *   final int N;
 *   final float[][] data;
 *   final CyclicBarrier barrier;
 *
 *   class Worker implements Runnable {
 *     int myRow;
 *     Worker(int row) { myRow = row; }
 *     public void run() {
 *       while (!done()) {
 *         processRow(myRow);
 *
 *         try {
 *           barrier.await();
 *         } catch (InterruptedException ex) {
 *           return;
 *         } catch (BrokenBarrierException ex) {
 *           return;
 *         }
 *       }
 *     }
 *   }
 *
 *   public Solver(float[][] matrix) {
 *     data = matrix;
 *     N = matrix.length;
 *     Runnable barrierAction =
 *       new Runnable() { public void run() { mergeRows(...); }};
 *     barrier = new CyclicBarrier(N, barrierAction);
 *
 *     List threads = new ArrayList(N);
 *     for (int i = 0; i < N; i++) {
 *       Thread thread = new Thread(new Worker(i));
 *       threads.add(thread);
 *       thread.start();
 *     }
 *
 *     // wait until done
 *     for (Thread thread : threads)
 *       thread.join();
 *   }
 * }}
 * D. 在这里，每个工作线程处理矩阵的一行，然后在屏障处等待，直到处理完所有行。 处理完所有行后，将执行提供的 {@link Runnable} 屏障操作并合并行。
 *    如果合并确定已找到解决方案，则 {@code done()} 将返回 {@code true} 并且每个 worker 将终止。
 * E. 如果屏障动作在执行时不依赖于被挂起的各方，那么当它被释放时，该方中的任何线程都可以执行该动作。 为方便起见，每次调用 {@link #await} 都会返回该线程在屏障处的到达索引。
 *    然后您可以选择哪个线程应该执行屏障操作，例如：
 * {@code
 * if (barrier.await() == 0) {
 *   // log the completion of this iteration
 * }}
 * F. {@code CyclicBarrier} 对失败的同步尝试使用all-or-none 破坏模型：如果一个线程由于中断、失败或超时而过早离开屏障点，则在该屏障点等待的所有其他线程也将通过
 *    {@link BrokenBarrierException}（或 {@link InterruptedException}，如果它们也几乎同时被中断）。
 * G. 内存一致性效果：在调用之前操作在一个线程{@code AWAIT（）}发生-之前是以下从成功返回的阻挡作用，这反过来又发生-前行动的一部分的动作对应的{@code AWAIT（）}在其他线程。
 */
/**
 * A.
 * A synchronization aid that allows a set of threads to all wait for
 * each other to reach a common barrier point.  CyclicBarriers are
 * useful in programs involving a fixed sized party of threads that
 * must occasionally wait for each other. The barrier is called
 * <em>cyclic</em> because it can be re-used after the waiting threads
 * are released.
 *
 * B.
 * <p>A {@code CyclicBarrier} supports an optional {@link Runnable} command
 * that is run once per barrier point, after the last thread in the party
 * arrives, but before any threads are released.
 * This <em>barrier action</em> is useful
 * for updating shared-state before any of the parties continue.
 *
 * C.
 * <p><b>Sample usage:</b> Here is an example of using a barrier in a
 * parallel decomposition design:
 *
 *  <pre> {@code
 * class Solver {
 *   final int N;
 *   final float[][] data;
 *   final CyclicBarrier barrier;
 *
 *   class Worker implements Runnable {
 *     int myRow;
 *     Worker(int row) { myRow = row; }
 *     public void run() {
 *       while (!done()) {
 *         processRow(myRow);
 *
 *         try {
 *           barrier.await();
 *         } catch (InterruptedException ex) {
 *           return;
 *         } catch (BrokenBarrierException ex) {
 *           return;
 *         }
 *       }
 *     }
 *   }
 *
 *   public Solver(float[][] matrix) {
 *     data = matrix;
 *     N = matrix.length;
 *     Runnable barrierAction =
 *       new Runnable() { public void run() { mergeRows(...); }};
 *     barrier = new CyclicBarrier(N, barrierAction);
 *
 *     List<Thread> threads = new ArrayList<Thread>(N);
 *     for (int i = 0; i < N; i++) {
 *       Thread thread = new Thread(new Worker(i));
 *       threads.add(thread);
 *       thread.start();
 *     }
 *
 *     // wait until done
 *     for (Thread thread : threads)
 *       thread.join();
 *   }
 * }}</pre>
 *
 * D.
 * Here, each worker thread processes a row of the matrix then waits at the
 * barrier until all rows have been processed. When all rows are processed
 * the supplied {@link Runnable} barrier action is executed and merges the
 * rows. If the merger
 * determines that a solution has been found then {@code done()} will return
 * {@code true} and each worker will terminate.
 *
 * E.
 * <p>If the barrier action does not rely on the parties being suspended when
 * it is executed, then any of the threads in the party could execute that
 * action when it is released. To facilitate this, each invocation of
 * {@link #await} returns the arrival index of that thread at the barrier.
 * You can then choose which thread should execute the barrier action, for
 * example:
 *  <pre> {@code
 * if (barrier.await() == 0) {
 *   // log the completion of this iteration
 * }}</pre>
 *
 * F.
 * <p>The {@code CyclicBarrier} uses an all-or-none breakage model
 * for failed synchronization attempts: If a thread leaves a barrier
 * point prematurely because of interruption, failure, or timeout, all
 * other threads waiting at that barrier point will also leave
 * abnormally via {@link BrokenBarrierException} (or
 * {@link InterruptedException} if they too were interrupted at about
 * the same time).
 *
 * G.
 * <p>Memory consistency effects: Actions in a thread prior to calling
 * {@code await()}
 * <a href="package-summary.html#MemoryVisibility"><i>happen-before</i></a>
 * actions that are part of the barrier action, which in turn
 * <i>happen-before</i> actions following a successful return from the
 * corresponding {@code await()} in other threads.
 *
 * @since 1.5
 * @see CountDownLatch
 *
 * @author Doug Lea
 */
public class CyclicBarrier {

    /**
     * 20210812
     * 屏障的每次使用都表示为一个生成实例。每当障碍物被触发或重置时，Generation都会发生变化。可以有许多代与使用屏障的线程相关联 - 由于锁可能以不确定的方式分配给等待线程 -
     * 但一次只能激活其中之一（{@code count} 适用的那个） ) 其余的要么坏了要么被绊倒了。 如果有中断但没有后续复位，则不需要有活动代。
     */
    /**
     * Each use of the barrier is represented as a generation instance.
     * The generation changes whenever the barrier is tripped, or
     * is reset. There can be many generations associated with threads
     * using the barrier - due to the non-deterministic way the lock
     * may be allocated to waiting threads - but only one of these
     * can be active at a time (the one to which {@code count} applies)
     * and all the rest are either broken or tripped.
     * There need not be an active generation if there has been a break
     * but no subsequent reset.
     */
    // CyclicBarrier分代实例
    private static class Generation {
        // 栅栏是否已损坏
        boolean broken = false;
    }

    // 护栏入口锁
    /** The lock for guarding barrier entry */
    // 栅栏入口锁
    private final ReentrantLock lock = new ReentrantLock();

    // 等待直到跳闸的条件
    /** Condition to wait on until tripped */
    // 线程绊倒Condition
    private final Condition trip = lock.newCondition();

    // 当事人数
    /** The number of parties */
    // 参与线程数
    private final int parties;

    // 跳闸时运行的命令
    /* The command to run when tripped */
    // 栅栏跳闸时运行的任务
    private final Runnable barrierCommand;

    // 现在的一代
    /** The current generation */
    // 当前代实例
    private Generation generation = new Generation();

    // 仍在等待的参与者数量。每一代从参与倒计时到0。 它会在每一代或损坏时重置为参与。
    /**
     * Number of parties still waiting. Counts down from parties to 0
     * on each generation.  It is reset to parties on each new
     * generation or when broken.
     */
    // 触发栅栏跳闸时的线程数
    private int count;

    // 更新障碍旅行的状态并唤醒每个人。仅在持有锁时调用。
    /**
     * Updates state on barrier trip and wakes up everyone.
     * Called only while holding lock.
     */
    // 唤醒所有等待的线程, 重置栅栏跳闸所需线程数, 并生成新代实例
    private void nextGeneration() {
        // 上一代信号完成
        // signal completion of last generation
        trip.signalAll();

        // 建立下一代
        // set up next generation
        count = parties;
        generation = new Generation();
    }

    // 将当前屏障生成设置为已破坏并唤醒所有人。 仅在持有锁时调用。
    /**
     * Sets current barrier generation as broken and wakes up everyone.
     * Called only while holding lock.
     */
    // 标记当前代为已损坏, 重置栅栏跳闸所需线程数, 并唤醒所有等待的线程
    private void breakBarrier() {
        generation.broken = true;
        count = parties;
        trip.signalAll();
    }

    // 主要障碍代码，涵盖各种政策。
    /**
     * Main barrier code, covering the various policies.
     */
    // CyclicBarrier阻塞核心代码
    private int dowait(boolean timed, long nanos) throws InterruptedException, BrokenBarrierException, TimeoutException {
        // 先获取栅栏入口锁
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            // 加锁后获取分代Generation
            final Generation g = generation;

            // 如果栅栏已损坏, 则抛出异常
            if (g.broken)
                throw new BrokenBarrierException();

            // 如果线程已中断, 则标记当前代为已损坏, 重置栅栏跳闸所需线程数, 并唤醒所有等待的线程
            if (Thread.interrupted()) {
                breakBarrier();
                throw new InterruptedException();
            }

            // 如果栅栏没有损坏, 且线程也没有被中断, 则栅栏线程计数-1
            int index = --count;

            // 如果栅栏线程计数减到0了, 则触发栅栏回调
            if (index == 0) {  // tripped // 绊倒
                boolean ranAction = false;
                try {
                    // 运行栅栏回调任务, 并生成新代Generation
                    final Runnable command = barrierCommand;
                    if (command != null)
                        command.run();
                    ranAction = true;

                    // 唤醒所有等待的线程, 重置栅栏跳闸所需线程数, 并生成新代实例
                    nextGeneration();
                    return 0;
                } finally {
                    // 如果运行失败, 则将当前屏障生成设置为已破坏并唤醒所有人
                    if (!ranAction)
                        breakBarrier();
                }
            }

            // 循环直到跳闸、损坏、中断或超时
            // loop until tripped, broken, interrupted, or timed out
            // 如果还没达到栅栏处, 则开始自旋
            for (;;) {
                try {
                    // 如果不需要超时, 则调用Condition#await阻塞当前线程
                    if (!timed)
                        trip.await();
                    // 如果需要超时, 且超时时间大于0, 则调用Condition#awaitNanos阻塞当前线程nanos时间
                    else if (nanos > 0L)
                        nanos = trip.awaitNanos(nanos);
                } catch (InterruptedException ie) {
                    // 如果发生异常, 则标记当前代为已损坏, 重置栅栏跳闸所需线程数, 并唤醒所有等待的线程
                    if (g == generation && ! g.broken) {
                        breakBarrier();
                        throw ie;
                    } else {
                        // 即使我们没有被中断，我们也即将完成等待，因此该中断被视为“属于”后续执行。
                        // We're about to finish waiting even if we had not
                        // been interrupted, so this interrupt is deemed to
                        // "belong" to subsequent execution.
                        Thread.currentThread().interrupt();
                    }
                }

                // 如果当前线程被唤醒后代被损坏, 则抛出异常
                if (g.broken)
                    throw new BrokenBarrierException();

                // 如果当前线程被唤醒后不为同一代, 说明上一代到达了栅栏, 则返回到达栅栏的索引号(为0代表最后一个线程)
                if (g != generation)
                    return index;

                // 如果当前线程被唤醒后仍为同一代, 说明上一代仍然没到达栅栏, 发生了虚假唤醒
                // 如果需要超时, 且发生了超时, 则将当前屏障生成设置为已破坏并唤醒所有人, 并抛出超时异常
                if (timed && nanos <= 0L) {
                    breakBarrier();
                    throw new TimeoutException();
                }
                // 如果不需要超时或者仍然没超时, 则继续自旋+阻塞
            }
        } finally {
            // 任何异常和返回都会先释放入口锁
            lock.unlock();
        }
    }

    /**
     * 20210812
     * 创建一个新的 {@code CyclicBarrier}，当给定数量的参与方（线程）正在等待它时，它将触发，并在屏障被触发时执行给定的屏障操作，由进入屏障的最后一个线程执行。
     */
    /**
     * Creates a new {@code CyclicBarrier} that will trip when the
     * given number of parties (threads) are waiting upon it, and which
     * will execute the given barrier action when the barrier is tripped,
     * performed by the last thread entering the barrier.
     *
     * // 在障碍被触发之前必须调用 {@link #await} 的线程数
     * @param parties the number of threads that must invoke {@link #await}
     *        before the barrier is tripped
     *
     * // 当障碍物被触发时执行的命令，如果没有动作，则 {@code null}
     * @param barrierAction the command to execute when the barrier is
     *        tripped, or {@code null} if there is no action
     *
     * @throws IllegalArgumentException if {@code parties} is less than 1
     */
    // 创建一个新的 {@code CyclicBarrier}，当给定数量的参与方（线程）正在等待它时，它将触发，并在屏障被触发时会由进入屏障的最后一个线程执行给定的屏障操作。
    public CyclicBarrier(int parties, Runnable barrierAction) {
        if (parties <= 0) throw new IllegalArgumentException();
        this.parties = parties;
        this.count = parties;
        this.barrierCommand = barrierAction;
    }

    // 创建一个新的 {@code CyclicBarrier}，当给定数量的参与方（线程）正在等待它时，它将跳闸，并且当屏障跳闸时不执行预定义的操作。
    /**
     * Creates a new {@code CyclicBarrier} that will trip when the
     * given number of parties (threads) are waiting upon it, and
     * does not perform a predefined action when the barrier is tripped.
     *
     * @param parties the number of threads that must invoke {@link #await}
     *        before the barrier is tripped
     * @throws IllegalArgumentException if {@code parties} is less than 1
     */
    // 创建一个新的 {@code CyclicBarrier}，当给定数量的参与方（线程）正在等待它时，它将跳闸，并且当屏障跳闸时不会执行其他预定义的操作。
    public CyclicBarrier(int parties) {
        this(parties, null);
    }

    // 返回触发此障碍所需的参与方数量。
    /**
     * Returns the number of parties required to trip this barrier.
     *
     * @return the number of parties required to trip this barrier
     */
    // 返回触发CyclicBarrier所需的参与线程数量
    public int getParties() {
        return parties;
    }

    /**
     * 20210812
     * A. 等待直到所有 {@linkplain #getParties 方} 都在此屏障上调用了 {@code await}。
     * B. 如果当前线程不是最后一个到达的线程，那么它会出于线程调度目的而被禁用并处于休眠状态，直到发生以下情况之一：
     *      a. 最后一个线程到达；
     *      b. 其他一些线程 {@linkplain Thread#interrupt 中断}当前线程；
     *      c. 其他一些线程 {@linkplain Thread#interrupt interrupts} 其他等待线程之一；
     *      d. 其他一些线程在等待屏障时超时；
     *      e. 其他一些线程在此屏障上调用 {@link #reset}。
     * C. 如果当前线程：
     *      a. 在进入此方法时设置其中断状态；
     *      b. 等待时是{@linkplain Thread#interrupt interrupted}
     *    然后抛出 {@link InterruptedException} 并清除当前线程的中断状态。
     * D. 如果在任何线程正在等待时屏障是 {@link #reset}，或者在调用 {@code await} 或任何线程正在等待时屏障 {@linkplain #isBroken 被破坏}，
     *    则 {@link BrokenBarrierException} 被抛出。
     * E. 如果任何线程在等待时{@linkplain Thread#interrupt interrupted}，则所有其他等待线程将抛出{@link BrokenBarrierException} 并且屏障被置于破坏状态。
     * F. 如果当前线程是最后一个到达的线程，并且在构造函数中提供了非空屏障操作，则当前线程在允许其他线程继续之前运行该操作。
     *    如果在屏障操作期间发生异常，则该异常将在当前线程中传播，并且屏障被置于破坏状态。
     */
    /**
     * A.
     * Waits until all {@linkplain #getParties parties} have invoked
     * {@code await} on this barrier.
     *
     * B.
     * <p>If the current thread is not the last to arrive then it is
     * disabled for thread scheduling purposes and lies dormant until
     * one of the following things happens:
     * <ul>
     * <li>The last thread arrives; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * the current thread; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * one of the other waiting threads; or
     * <li>Some other thread times out while waiting for barrier; or
     * <li>Some other thread invokes {@link #reset} on this barrier.
     * </ul>
     *
     * C.
     * <p>If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@linkplain Thread#interrupt interrupted} while waiting
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     *
     * D.
     * <p>If the barrier is {@link #reset} while any thread is waiting,
     * or if the barrier {@linkplain #isBroken is broken} when
     * {@code await} is invoked, or while any thread is waiting, then
     * {@link BrokenBarrierException} is thrown.
     *
     * E.
     * <p>If any thread is {@linkplain Thread#interrupt interrupted} while waiting,
     * then all other waiting threads will throw
     * {@link BrokenBarrierException} and the barrier is placed in the broken
     * state.
     *
     * F.
     * <p>If the current thread is the last thread to arrive, and a
     * non-null barrier action was supplied in the constructor, then the
     * current thread runs the action before allowing the other threads to
     * continue.
     * If an exception occurs during the barrier action then that exception
     * will be propagated in the current thread and the barrier is placed in
     * the broken state.
     *
     * @return the arrival index of the current thread, where index
     *         {@code getParties() - 1} indicates the first
     *         to arrive and zero indicates the last to arrive
     * @throws InterruptedException if the current thread was interrupted
     *         while waiting
     * @throws BrokenBarrierException if <em>another</em> thread was
     *         interrupted or timed out while the current thread was
     *         waiting, or the barrier was reset, or the barrier was
     *         broken when {@code await} was called, or the barrier
     *         action (if present) failed due to an exception
     */
    // 无限阻塞当前线程, 所有线程到达、或者任何线程中断、超时、或者栅栏被重置则被唤醒
    public int await() throws InterruptedException, BrokenBarrierException {
        try {
            return dowait(false, 0L);
        } catch (TimeoutException toe) {
            throw new Error(toe); // cannot happen
        }
    }

    /**
     * 20210812
     * A. 等待直到所有 {@linkplain #getParties 方} 都在此屏障上调用了 {@code await}，或者指定的等待时间已过。
     * B. 如果当前线程不是最后一个到达的线程，那么它会出于线程调度目的而被禁用并处于休眠状态，直到发生以下情况之一：
     *      a. 最后一个线程到达；
     *      b. 指定的超时时间已过；
     *      c. 其他一些线程 {@linkplain Thread#interrupt 中断}当前线程；
     *      d. 其他一些线程 {@linkplain Thread#interrupt interrupts} 其他等待线程之一；
     *      e. 其他一些线程在等待屏障时超时；
     *      f. 其他一些线程在此屏障上调用 {@link #reset}。
     * C. 如果当前线程：
     *      a. 在进入此方法时设置其中断状态；
     *      b. 等待时是{@linkplain Thread#interrupt interrupted}
     *    然后抛出 {@link InterruptedException} 并清除当前线程的中断状态。
     * D. 如果指定的等待时间已过，则抛出 {@link TimeoutException}。 如果时间小于或等于零，则该方法根本不会等待。
     * E. 如果在任何线程正在等待时屏障是 {@link #reset}，或者在调用 {@code await} 或任何线程正在等待时屏障 {@linkplain #isBroken 被破坏}，
     *    则 {@link BrokenBarrierException} 被抛出。
     * F. 如果任何线程在等待时{@linkplain Thread#interrupt interrupted}，则所有其他等待线程将抛出{@link BrokenBarrierException} 并且屏障被置于破坏状态。
     * G. 如果当前线程是最后一个到达的线程，并且在构造函数中提供了非空屏障操作，则当前线程在允许其他线程继续之前运行该操作。
     *    如果在屏障操作期间发生异常，则该异常将在当前线程中传播，并且屏障被置于破坏状态。
     */
    /**
     * A.
     * Waits until all {@linkplain #getParties parties} have invoked
     * {@code await} on this barrier, or the specified waiting time elapses.
     *
     * B.
     * <p>If the current thread is not the last to arrive then it is
     * disabled for thread scheduling purposes and lies dormant until
     * one of the following things happens:
     * <ul>
     * <li>The last thread arrives; or
     * <li>The specified timeout elapses; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * the current thread; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * one of the other waiting threads; or
     * <li>Some other thread times out while waiting for barrier; or
     * <li>Some other thread invokes {@link #reset} on this barrier.
     * </ul>
     *
     * C.
     * <p>If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@linkplain Thread#interrupt interrupted} while waiting
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     *
     * D.
     * <p>If the specified waiting time elapses then {@link TimeoutException}
     * is thrown. If the time is less than or equal to zero, the
     * method will not wait at all.
     *
     * E.
     * <p>If the barrier is {@link #reset} while any thread is waiting,
     * or if the barrier {@linkplain #isBroken is broken} when
     * {@code await} is invoked, or while any thread is waiting, then
     * {@link BrokenBarrierException} is thrown.
     *
     * F.
     * <p>If any thread is {@linkplain Thread#interrupt interrupted} while
     * waiting, then all other waiting threads will throw {@link
     * BrokenBarrierException} and the barrier is placed in the broken
     * state.
     *
     * G.
     * <p>If the current thread is the last thread to arrive, and a
     * non-null barrier action was supplied in the constructor, then the
     * current thread runs the action before allowing the other threads to
     * continue.
     * If an exception occurs during the barrier action then that exception
     * will be propagated in the current thread and the barrier is placed in
     * the broken state.
     *
     * @param timeout the time to wait for the barrier
     * @param unit the time unit of the timeout parameter
     *
     * // 当前线程的到达索引，其中 index {@code getParties() - 1} 表示第一个到达，零表示最后一个到达
     * @return the arrival index of the current thread, where index
     *         {@code getParties() - 1} indicates the first
     *         to arrive and zero indicates the last to arrive
     * @throws InterruptedException if the current thread was interrupted
     *         while waiting
     * @throws TimeoutException if the specified timeout elapses.
     *         In this case the barrier will be broken.
     * @throws BrokenBarrierException if <em>another</em> thread was
     *         interrupted or timed out while the current thread was
     *         waiting, or the barrier was reset, or the barrier was broken
     *         when {@code await} was called, or the barrier action (if
     *         present) failed due to an exception
     */
    // 阻塞当前线程指定时间, 所有线程到达、或者任何线程中断、超时、或者栅栏被重置则被唤醒
    public int await(long timeout, TimeUnit unit) throws InterruptedException, BrokenBarrierException, TimeoutException {
        return dowait(true, unit.toNanos(timeout));
    }

    // 查询此屏障是否处于破坏状态。
    /**
     * Queries if this barrier is in a broken state.
     *
     * // {@code true} 如果一个或多个参与方由于自构建或上次重置以来的中断或超时而突破此障碍，或者由于异常导致障碍操作失败； {@code false} 否则。
     * @return {@code true} if one or more parties broke out of this
     *         barrier due to interruption or timeout since
     *         construction or the last reset, or a barrier action
     *         failed due to an exception; {@code false} otherwise.
     */
    // 查询CyclicBarrier(当前代)是否处于损坏状态
    public boolean isBroken() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return generation.broken;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 20210812
     * 将障碍重置为其初始状态。 如果任何一方当前正在屏障处等待，他们将返回 {@link BrokenBarrierException}。 请注意，由于其他原因发生损坏后的复位操作可能很复杂；
     * 线程需要以其他方式重新同步，并选择一个来执行重置。 最好为后续使用创建一个新的屏障。
     */
    /**
     * Resets the barrier to its initial state.  If any parties are
     * currently waiting at the barrier, they will return with a
     * {@link BrokenBarrierException}. Note that resets <em>after</em>
     * a breakage has occurred for other reasons can be complicated to
     * carry out; threads need to re-synchronize in some other way,
     * and choose one to perform the reset.  It may be preferable to
     * instead create a new barrier for subsequent use.
     */
    // 重置CyclicBarrier为初始状态, 标记当前代为已损坏, 生成新代实例
    public void reset() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            // 标记当前代为已损坏, 重置栅栏跳闸所需线程数, 并唤醒所有等待的线程
            breakBarrier();   // break the current generation 打破当前的一代

            // 唤醒所有等待的线程, 重置栅栏跳闸所需线程数, 并生成新代实例
            nextGeneration(); // start a new generation 开始新的一代
        } finally {
            lock.unlock();
        }
    }

    // 返回当前在障碍处等待的参与方数量parties - count。 此方法主要用于调试和断言。
    /**
     * Returns the number of parties currently waiting at the barrier.
     * This method is primarily useful for debugging and assertions.
     *
     * @return the number of parties currently blocked in {@link #await}
     */
    // 返回当前处于等待的线程数 = parties - count
    public int getNumberWaiting() {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            return parties - count;
        } finally {
            lock.unlock();
        }
    }
}
