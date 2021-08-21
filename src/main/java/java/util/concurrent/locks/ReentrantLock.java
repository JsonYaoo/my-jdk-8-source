/*
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util.concurrent.locks;
import java.util.concurrent.TimeUnit;
import java.util.Collection;

/**
 * 20210808
 * A. 可重入互斥{@link Lock}与使用{@code synchronized}方法和语句访问的隐式监视器锁具有相同的基本行为和语义，但具有扩展功能。
 * B. {@code ReentrantLock} 由上次成功锁定但尚未解锁的线程拥有。 当锁不属于另一个线程时，调用 {@code lock} 的线程将返回并成功获取锁。
 *    如果当前线程已经拥有锁，该方法将立即返回。 这可以使用方法 {@link #isHeldByCurrentThread} 和 {@link #getHoldCount} 进行检查。
 * C. 此类的构造函数接受一个可选的公平参数。 当设置{@code true}时，在争用情况下，锁倾向于授予对等待时间最长的线程的访问权限。 否则这个锁不能保证任何特定的访问顺序。
 *    与使用默认设置的程序相比，使用由多个线程访问的公平锁的程序可能会显示出较低的总体吞吐量（即更慢；通常慢得多），但在获取锁和保证不出现饥饿的时间上具有较小的差异。
 *    但是请注意，锁的公平性并不能保证线程调度的公平性。 因此，使用公平锁的许多线程之一可能会连续多次获得它，而其他活动线程没有进行并且当前没有持有该锁。
 *    另请注意，未计时的 {@link #tryLock()} 方法不符合公平性设置。 即使其他线程正在等待，如果锁可用，它也会成功。
 * D. 建议的做法是始终使用 {@code try} 块立即跟随对 {@code lock} 的调用，最常见的是在 before/after 构造中，例如：
 *  {@code
 * class X {
 *   private final ReentrantLock lock = new ReentrantLock();
 *   // ...
 *
 *   public void m() {
 *     lock.lock();  // block until condition holds
 *     try {
 *       // ... method body
 *     } finally {
 *       lock.unlock()
 *     }
 *   }
 * }}
 * E. 除了实现 {@link Lock} 接口之外，该类还定义了许多用于检查锁状态的 {@code public} 和 {@code protected} 方法。 其中一些方法仅对仪表和监控有用。
 * F. 此类的序列化与内置锁的行为方式相同：反序列化的锁处于解锁状态，无论其序列化时的状态如何。
 * G. 该锁最多支持同一线程2147483647个递归锁。尝试超过此限制会导致 {@link Error} 从锁定方法中抛出。
 */
/**
 * A.
 * A reentrant mutual exclusion {@link Lock} with the same basic
 * behavior and semantics as the implicit monitor lock accessed using
 * {@code synchronized} methods and statements, but with extended
 * capabilities.
 *
 * B.
 * <p>A {@code ReentrantLock} is <em>owned</em> by the thread last
 * successfully locking, but not yet unlocking it. A thread invoking
 * {@code lock} will return, successfully acquiring the lock, when
 * the lock is not owned by another thread. The method will return
 * immediately if the current thread already owns the lock. This can
 * be checked using methods {@link #isHeldByCurrentThread}, and {@link
 * #getHoldCount}.
 *
 * C.
 * <p>The constructor for this class accepts an optional
 * <em>fairness</em> parameter.  When set {@code true}, under
 * contention, locks favor granting access to the longest-waiting
 * thread.  Otherwise this lock does not guarantee any particular
 * access order.  Programs using fair locks accessed by many threads
 * may display lower overall throughput (i.e., are slower; often much
 * slower) than those using the default setting, but have smaller
 * variances in times to obtain locks and guarantee lack of
 * starvation. Note however, that fairness of locks does not guarantee
 * fairness of thread scheduling. Thus, one of many threads using a
 * fair lock may obtain it multiple times in succession while other
 * active threads are not progressing and not currently holding the
 * lock.
 * Also note that the untimed {@link #tryLock()} method does not
 * honor the fairness setting. It will succeed if the lock
 * is available even if other threads are waiting.
 *
 * D.
 * <p>It is recommended practice to <em>always</em> immediately
 * follow a call to {@code lock} with a {@code try} block, most
 * typically in a before/after construction such as:
 *
 *  <pre> {@code
 * class X {
 *   private final ReentrantLock lock = new ReentrantLock();
 *   // ...
 *
 *   public void m() {
 *     lock.lock();  // block until condition holds
 *     try {
 *       // ... method body
 *     } finally {
 *       lock.unlock()
 *     }
 *   }
 * }}</pre>
 *
 * E.
 * <p>In addition to implementing the {@link Lock} interface, this
 * class defines a number of {@code public} and {@code protected}
 * methods for inspecting the state of the lock.  Some of these
 * methods are only useful for instrumentation and monitoring.
 *
 * F.
 * <p>Serialization of this class behaves in the same way as built-in
 * locks: a deserialized lock is in the unlocked state, regardless of
 * its state when serialized.
 *
 * G.
 * <p>This lock supports a maximum of 2147483647 recursive locks by
 * the same thread. Attempts to exceed this limit result in
 * {@link Error} throws from locking methods.
 *
 * @since 1.5
 * @author Doug Lea
 */
public class ReentrantLock implements Lock, java.io.Serializable {

    private static final long serialVersionUID = 7373984872572414699L;

    // 提供所有实现机制的同步器
    /** Synchronizer providing all implementation mechanics */
    private final Sync sync;

    // 此锁的同步控制基础。 下面细分为公平和非公平版本。 使用AQS状态来表示锁的持有次数。
    /**
     * Base of synchronization control for this lock. Subclassed
     * into fair and nonfair versions below. Uses AQS state to
     * represent the number of holds on the lock.
     */
    abstract static class Sync extends AbstractQueuedSynchronizer {

        private static final long serialVersionUID = -5179523762034025860L;

        // 执行{@link Lock#lock}。子类化的主要原因是允许非公平版本的快速路径。
        /**
         * Performs {@link Lock#lock}. The main reason for subclassing
         * is to allow fast path for nonfair version.
         */
        // 获取锁，子类可以实现该方法，以支持非公平版本
        abstract void lock();

        // 执行不公平的tryLock。tryAcquire在子类中实现，但两者都需要对trylock方法进行非公平尝试。
        /**
         * Performs non-fair tryLock.  tryAcquire is implemented in
         * subclasses, but both need nonfair try for trylock method.
         */
        // 非公平方式获取锁
        final boolean nonfairTryAcquire(int acquires) {
            final Thread current = Thread.currentThread();

            // 获取同步器状态作为锁持有次数
            int c = getState();

            // 如果锁持有次数为0, 则CAS更新锁次数为acquires, 并设置当前线程独占, 返回true代表成功获得锁
            if (c == 0) {
                if (compareAndSetState(0, acquires)) {
                    setExclusiveOwnerThread(current);
                    return true;
                }
            }
            // 如果当前线程为独占中的线程, 则锁持有次数累加acquires, 代表可重入, 返回true代表锁重入成功
            else if (current == getExclusiveOwnerThread()) {
                int nextc = c + acquires;
                if (nextc < 0) // overflow
                    throw new Error("Maximum lock count exceeded");
                setState(nextc);
                return true;
            }

            // 如果尝试获取锁失败, 则返回false
            return false;
        }

        // 尝试释放独占模式的锁持有次数releases
        protected final boolean tryRelease(int releases) {
            // 持有次数减去releases次
            int c = getState() - releases;

            // 如果当前线程不为当前独占线程, 则抛出异常
            if (Thread.currentThread() != getExclusiveOwnerThread())
                throw new IllegalMonitorStateException();

            // 如果c为0, 则清空独占的线程, 并返回true, 代表锁释放成功; 否则返回fasle, 代表锁释放失败
            boolean free = false;
            if (c == 0) {
                free = true;
                setExclusiveOwnerThread(null);
            }
            setState(c);
            return free;
        }

        // 判断当前线程是否为独占的线程
        protected final boolean isHeldExclusively() {
            // 虽然我们通常必须在所有者之前读取状态，但我们不需要这样做来检查当前线程是否是所有者
            // While we must in general read state before owner,
            // we don't need to do so to check if current thread is owner
            return getExclusiveOwnerThread() == Thread.currentThread();
        }

        // 构建AQS实现的Condition接口实现类
        final ConditionObject newCondition() {
            return new ConditionObject();
        }

        // 从外部类中继的方法
        // Methods relayed from outer class

        // 获取当前独占的线程
        final Thread getOwner() {
            return getState() == 0 ? null : getExclusiveOwnerThread();
        }

        // 获取同步器的状态(即锁获取次数)
        final int getHoldCount() {
            return isHeldExclusively() ? getState() : 0;
        }

        // 判断是否存在同步器状态, 即锁获取次数是否为0
        final boolean isLocked() {
            return getState() != 0;
        }

        // 从流中重构实例（即反序列化它）。
        /**
         * Reconstitutes the instance from a stream (that is, deserializes it).
         */
        // 反序列化可重入锁, 反序列化后同步状态(锁获取次数为0)
        private void readObject(java.io.ObjectInputStream s) throws java.io.IOException, ClassNotFoundException {
            s.defaultReadObject();
            setState(0); // reset to unlocked state 重置为解锁状态
        }
    }

    // 非公平锁的同步对象
    /**
     * Sync object for non-fair locks
     */
    // 非公平锁同步控制实现类
    static final class NonfairSync extends Sync {

        private static final long serialVersionUID = 7316153563782823691L;

        // 执行锁定。 尝试立即驳船，在失败时备份到正常获取。
        /**
         * Performs lock.  Try immediate barge, backing up to normal
         * acquire on failure.
         */
        // 非公平获取锁, 先CAS更新锁持有次数为1
        final void lock() {
            // CAS成功则设置当前线程为独占线程
            if (compareAndSetState(0, 1))
                setExclusiveOwnerThread(Thread.currentThread());
            // CAS失败则以阻塞、独占模式获取同步器状态
            else
                acquire(1);
        }

        // 尝试以独占模式获取同步器状态
        protected final boolean tryAcquire(int acquires) {
            return nonfairTryAcquire(acquires);
        }
    }

    // 公平锁的同步对象
    /**
     * Sync object for fair locks
     */
    // 公平锁同步控制实现类
    static final class FairSync extends Sync {

        private static final long serialVersionUID = -3000897897090466540L;

        // 以阻塞、独占模式获取同步器状态, 公平排队
        final void lock() {
            acquire(1);
        }

        // tryAcquire的公平版本。不要授予访问权限，除非递归调用或者没有服务员或者是第一个。
        /**
         * Fair version of tryAcquire.  Don't grant access unless
         * recursive call or no waiters or is first.
         */
        // 尝试以独占模式获取同步器状态, 对比非公平锁的tryAcquire, 多了需要先判断是否存在排队结点再进行CAS更新同步器状态
        protected final boolean tryAcquire(int acquires) {
            final Thread current = Thread.currentThread();

            // 获取同步器状态作为锁持有次数
            int c = getState();

            // 如果锁持有次数为0, 则CAS更新锁次数为acquires, 并设置当前线程独占, 返回true代表成功获得锁
            if (c == 0) {
                // 判断此刻CLH中是否存在有等待时间比当前线程长的线程, 旨在由公平同步器使用, 以避免插入结点到CLH队列中
                if (!hasQueuedPredecessors() &&
                    // 如果没有结点在排队, 则CAS更新同步器状态
                    compareAndSetState(0, acquires)) {
                    // CAS更新成功, 则设置当前线程为独占状态, 并返回true, 表示公平锁获取成功
                    setExclusiveOwnerThread(current);
                    return true;
                }
            }
            // 如果当前线程为独占中的线程, 则锁持有次数累加acquires, 代表可重入, 返回true代表锁重入成功
            else if (current == getExclusiveOwnerThread()) {
                int nextc = c + acquires;
                if (nextc < 0)
                    throw new Error("Maximum lock count exceeded");
                setState(nextc);
                return true;
            }

            // 如果尝试获取锁失败, 则返回false
            return false;
        }
    }

    // 创建一个 {@code ReentrantLock} 实例。这相当于使用 {@code ReentrantLock(false)}。
    /**
     * Creates an instance of {@code ReentrantLock}.
     * This is equivalent to using {@code ReentrantLock(false)}.
     */
    // 创建ReentrantLock实例, 默认使用非公平锁
    public ReentrantLock() {
        sync = new NonfairSync();
    }

    // 使用给定的公平策略创建 {@code ReentrantLock} 的实例。
    /**
     * Creates an instance of {@code ReentrantLock} with the
     * given fairness policy.
     *
     * @param fair {@code true} if this lock should use a fair ordering policy
     */
    // 创建ReentrantLock实例, 如果为true则使用公平锁, 如果为false则使用非公平锁
    public ReentrantLock(boolean fair) {
        sync = fair ? new FairSync() : new NonfairSync();
    }

    /**
     * 20210809
     * A. 获得锁。
     * B. 如果其他线程没有持有锁，则获取该锁并立即返回，将锁持有计数设置为1。
     * C. 如果当前线程已经持有锁，那么持有计数加一并且该方法立即返回。
     * D. 如果该锁被另一个线程持有，那么当前线程将因线程调度目的而被禁用并处于休眠状态，直到获得该锁为止，此时锁持有计数设置为1。
     */
    /**
     * A.
     * Acquires the lock.
     *
     * B.
     * <p>Acquires the lock if it is not held by another thread and returns
     * immediately, setting the lock hold count to one.
     *
     * C.
     * <p>If the current thread already holds the lock then the hold
     * count is incremented by one and the method returns immediately.
     *
     * D.
     * <p>If the lock is held by another thread then the
     * current thread becomes disabled for thread scheduling
     * purposes and lies dormant until the lock has been acquired,
     * at which time the lock hold count is set to one.
     */
    // 阻塞方式获取锁
    public void lock() {
        sync.lock();
    }

    /**
     * 20210523
     * A. 除非当前线程为{@linkplain Thread＃interrupt interrupted}(即阻塞等待锁时可以被中断)，否则获取锁。
     * B. 如果没有其他线程持有该锁，则获取该锁并立即返回，将锁保持计数设置为1。
     * C. 如果当前线程已经持有此锁，则持有计数将增加一，并且该方法将立即返回。
     * D. 如果锁是由另一个线程持有的，则出于线程调度目的，当前线程将被禁用，并处于休眠状态，直到发生以下两种情况之一：
     *      1) 该锁是由当前线程获取的；
     *      2) 或者其他一些线程{@linkplain Thread＃interrupt interrupts}当前线程。
     *    如果当前线程获取了锁，则锁保持计数将设置为1。如果当前线程：
     *      1) 在进入此方法时已设置其中断状态；
     *      2) 或者在获取锁的过程中被{@linkplain Thread＃interrupt interrupted中断了，
     *    然后抛出{@link InterruptedException}并清除当前线程的中断状态。
     * E. 在此实现中，由于此方法是显式的中断点，因此优先于对中断的响应而不是正常或可重入的锁获取。
     */
    /**
     * A.
     * Acquires the lock unless the current thread is
     * {@linkplain Thread#interrupt interrupted}.
     *
     * B.
     * <p>Acquires the lock if it is not held by another thread and returns
     * immediately, setting the lock hold count to one.
     *
     * C.
     * <p>If the current thread already holds this lock then the hold count
     * is incremented by one and the method returns immediately.
     *
     * D.
     * <p>If the lock is held by another thread then the
     * current thread becomes disabled for thread scheduling
     * purposes and lies dormant until one of two things happens:
     *
     * <ul>
     *
     * <li>The lock is acquired by the current thread; or
     *
     * <li>Some other thread {@linkplain Thread#interrupt interrupts} the
     * current thread.
     *
     * </ul>
     *
     * <p>If the lock is acquired by the current thread then the lock hold
     * count is set to one.
     *
     * <p>If the current thread:
     *
     * <ul>
     *
     * <li>has its interrupted status set on entry to this method; or
     *
     * <li>is {@linkplain Thread#interrupt interrupted} while acquiring
     * the lock,
     *
     * </ul>
     *
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     *
     * E.
     * <p>In this implementation, as this method is an explicit
     * interruption point, preference is given to responding to the
     * interrupt over normal or reentrant acquisition of the lock.
     *
     * @throws InterruptedException if the current thread is interrupted
     */
    // 以阻塞、独占可中断模式获取同步器
    public void lockInterruptibly() throws InterruptedException {
        sync.acquireInterruptibly(1);
    }

    /**
     * 20210809
     * A. 仅当调用时其他线程未持有该锁时才获取该锁。
     * B. 如果锁未被另一个线程持有，则获取该锁并立即返回值{@code true}，将锁持有计数设置为1。即使此锁已设置为使用公平排序策略，
     *    调用{@code tryLock()}也会立即获取该锁（如果可用），无论其他线程当前是否正在等待该锁。这种“闯入”行为在某些情况下很有用，即使它破坏了公平。
     *    如果你想尊重这个锁的公平性设置，那么使用{@link #tryLock(long, TimeUnit) tryLock(0, TimeUnit.SECONDS)}这几乎是等效的（它也检测中断）。
     */
    /**
     * A.
     * Acquires the lock only if it is not held by another thread at the time
     * of invocation.
     *
     * B.
     * <p>Acquires the lock if it is not held by another thread and
     * returns immediately with the value {@code true}, setting the
     * lock hold count to one. Even when this lock has been set to use a
     * fair ordering policy, a call to {@code tryLock()} <em>will</em>
     * immediately acquire the lock if it is available, whether or not
     * other threads are currently waiting for the lock.
     * This &quot;barging&quot; behavior can be useful in certain
     * circumstances, even though it breaks fairness. If you want to honor
     * the fairness setting for this lock, then use
     * {@link #tryLock(long, TimeUnit) tryLock(0, TimeUnit.SECONDS) }
     * which is almost equivalent (it also detects interruption).
     *
     * C.
     * <p>If the current thread already holds this lock then the hold
     * count is incremented by one and the method returns {@code true}.
     *
     * D.
     * <p>If the lock is held by another thread then this method will return
     * immediately with the value {@code false}.
     *
     * @return {@code true} if the lock was free and was acquired by the
     *         current thread, or the lock was already held by the current
     *         thread; and {@code false} otherwise
     */
    // 非公平方式尝试获取锁, 如果可用则获取锁并立即返回值{@code true}; 如果锁不可用, 则立即返回值{@code false}
    public boolean tryLock() {
        return sync.nonfairTryAcquire(1);
    }

    /**
     * 20210809
     * A. 如果在给定的等待时间内没有被另一个线程持有并且当前线程没有被{@linkplain Thread#interrupt interrupted}，则获取锁。
     * B. 如果锁未被另一个线程持有，则获取该锁并立即返回值{@code true}，将锁持有计数设置为1。如果此锁已设置为使用公平排序策略，则如果任何其他线程正在等待该锁，
     *    则不会获取可用锁。 这与 {@link #tryLock()} 方法形成对比。 如果您想要一个允许插入公平锁的定时 {@code tryLock}，则将定时和非定时形式组合在一起：
     *  {@code
     * if (lock.tryLock() ||
     *     lock.tryLock(timeout, unit)) {
     *   ...
     * }}
     * C. 如果当前线程已经持有这个锁，那么持有计数增加一并且该方法返回{@code true}。
     * D. 如果锁被另一个线程持有，那么当前线程将被禁用以进行线程调度并处于休眠状态，直到发生以下三种情况之一：
     *      a. 锁被当前线程获取；
     *      b. 其他一些线程 {@linkplain Thread#interrupt}中断当前线程；
     *      c. 经过指定的等待时间
     * E. 如果获取了锁，则返回值 {@code true} 并将锁保持计数设置为 1。
     * F. 如果当前线程：
     *      a. 在进入此方法时设置其中断状态；
     *      b. 获取锁时{@linkplain Thread#interrupt interrupted}，
     *    然后抛出 {@link InterruptedException} 并清除当前线程的中断状态。
     * G. 如果指定的等待时间过去，则返回值 {@code false}。 如果时间小于或等于零，则该方法根本不会等待。
     * H. 在此实现中，由于此方法是显式中断点，因此优先响应中断而不是正常或可重入获取锁，并优先报告等待时间的过去。
     */
    /**
     * A.
     * Acquires the lock if it is not held by another thread within the given
     * waiting time and the current thread has not been
     * {@linkplain Thread#interrupt interrupted}.
     *
     * B.
     * <p>Acquires the lock if it is not held by another thread and returns
     * immediately with the value {@code true}, setting the lock hold count
     * to one. If this lock has been set to use a fair ordering policy then
     * an available lock <em>will not</em> be acquired if any other threads
     * are waiting for the lock. This is in contrast to the {@link #tryLock()}
     * method. If you want a timed {@code tryLock} that does permit barging on
     * a fair lock then combine the timed and un-timed forms together:
     *
     *  <pre> {@code
     * if (lock.tryLock() ||
     *     lock.tryLock(timeout, unit)) {
     *   ...
     * }}</pre>
     *
     * C.
     * <p>If the current thread
     * already holds this lock then the hold count is incremented by one and
     * the method returns {@code true}.
     *
     * D.
     * <p>If the lock is held by another thread then the
     * current thread becomes disabled for thread scheduling
     * purposes and lies dormant until one of three things happens:
     *
     * <ul>
     *
     * <li>The lock is acquired by the current thread; or
     *
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * the current thread; or
     *
     * <li>The specified waiting time elapses
     *
     * </ul>
     *
     * E.
     * <p>If the lock is acquired then the value {@code true} is returned and
     * the lock hold count is set to one.
     *
     * F.
     * <p>If the current thread:
     *
     * <ul>
     *
     * <li>has its interrupted status set on entry to this method; or
     *
     * <li>is {@linkplain Thread#interrupt interrupted} while
     * acquiring the lock,
     *
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     *
     * G.
     * <p>If the specified waiting time elapses then the value {@code false}
     * is returned.  If the time is less than or equal to zero, the method
     * will not wait at all.
     *
     * H.
     * <p>In this implementation, as this method is an explicit
     * interruption point, preference is given to responding to the
     * interrupt over normal or reentrant acquisition of the lock, and
     * over reporting the elapse of the waiting time.
     *
     * @param timeout the time to wait for the lock
     * @param unit the time unit of the timeout argument
     *
     * // {@code true} 如果锁是空闲的并且被当前线程获取，或者锁已经被当前线程持有； 和 {@code false} 如果在获得锁之前等待时间已经过去
     * @return {@code true} if the lock was free and was acquired by the
     *         current thread, or the lock was already held by the current
     *         thread; and {@code false} if the waiting time elapsed before
     *         the lock could be acquired
     * @throws InterruptedException if the current thread is interrupted
     * @throws NullPointerException if the time unit is null
     */
    // 定时可中断式尝试获取锁, 获得则返回true, 否则返回false
    public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException {
        return sync.tryAcquireNanos(1, unit.toNanos(timeout));
    }

    /**
     * 20210809
     * A. 尝试释放此锁。
     * B. 如果当前线程是此锁的持有者，则持有计数递减。如果保持计数现在为零，则锁定被释放。如果当前线程不是此锁的持有者，则抛出 {@link IllegalMonitorStateException}。
     */
    /**
     * A.
     * Attempts to release this lock.
     *
     * B.
     * <p>If the current thread is the holder of this lock then the hold
     * count is decremented.  If the hold count is now zero then the lock
     * is released.  If the current thread is not the holder of this
     * lock then {@link IllegalMonitorStateException} is thrown.
     *
     * @throws IllegalMonitorStateException if the current thread does not
     *         hold this lock
     */
    // 释放锁
    public void unlock() {
        sync.release(1);
    }

    /**
     * 20210809
     * A. 返回与此 {@link Lock} 实例一起使用的 {@link Condition} 实例。
     * B. 返回的 {@link Condition} 实例支持与 {@link Object} 监控方法
     *   （{@link Object#wait() wait}、{@link Object#notify notify} 和 {@link Object#notifyAll）notifyAll})相同的用法 与内置监视器锁一起使用时。
     *      a. 如果在调用任何 {@link Condition} {@linkplain Condition#await() waiting} 或 {@linkplain Condition#signal signalling} 方法时未持有此锁，
     *         则抛出 {@link IllegalMonitorStateException}。
     *      b. 当条件 {@linkplain Condition#await() waiting} 方法被调用时，锁被释放，在它们返回之前，锁被重新获取，锁保持计数恢复到调用方法时的状态。
     *      c. 如果线程在等待时{@linkplain Thread#interrupt interrupted}，则等待将终止，将抛出{@link InterruptedException}，并清除线程的中断状态。
     *      d. 等待线程按 FIFO 顺序发出信号。
     *      e. 从等待方法返回的线程重新获取锁的顺序与最初获取锁的线程相同，在默认情况下未指定，但对于公平锁，那些等待时间最长的线程优先。
     */
    /**
     * A.
     * Returns a {@link Condition} instance for use with this
     * {@link Lock} instance.
     *
     * B.
     * <p>The returned {@link Condition} instance supports the same
     * usages as do the {@link Object} monitor methods ({@link
     * Object#wait() wait}, {@link Object#notify notify}, and {@link
     * Object#notifyAll notifyAll}) when used with the built-in
     * monitor lock.
     *
     * <ul>
     *
     * <li>If this lock is not held when any of the {@link Condition}
     * {@linkplain Condition#await() waiting} or {@linkplain
     * Condition#signal signalling} methods are called, then an {@link
     * IllegalMonitorStateException} is thrown.
     *
     * <li>When the condition {@linkplain Condition#await() waiting}
     * methods are called the lock is released and, before they
     * return, the lock is reacquired and the lock hold count restored
     * to what it was when the method was called.
     *
     * <li>If a thread is {@linkplain Thread#interrupt interrupted}
     * while waiting then the wait will terminate, an {@link
     * InterruptedException} will be thrown, and the thread's
     * interrupted status will be cleared.
     *
     * <li> Waiting threads are signalled in FIFO order.
     *
     * <li>The ordering of lock reacquisition for threads returning
     * from waiting methods is the same as for threads initially
     * acquiring the lock, which is in the default case not specified,
     * but for <em>fair</em> locks favors those threads that have been
     * waiting the longest.
     *
     * </ul>
     *
     * @return the Condition object
     */
    // 返回绑定到此{@code Lock}实例的{@link Condition}实例 => eg: 在Condition#await之前，当前线程必须持有锁
    public Condition newCondition() {
        return sync.newCondition();
    }

    /**
     * 20210809
     * A. 查询当前线程持有该锁的次数。
     * B. 一个线程为每个与解锁操作不匹配的锁操作持有一个锁。
     * C. 保持计数信息通常仅用于测试和调试目的。例如，如果不应该在已经持有锁的情况下输入某段代码，那么我们可以断言这个事实：
     *  {@code
     * class X {
     *   ReentrantLock lock = new ReentrantLock();
     *   // ...
     *   public void m() {
     *     assert lock.getHoldCount() == 0;
     *     lock.lock();
     *     try {
     *       // ... method body
     *     } finally {
     *       lock.unlock();
     *     }
     *   }
     * }}
     */
    /**
     * A.
     * Queries the number of holds on this lock by the current thread.
     *
     * B.
     * <p>A thread has a hold on a lock for each lock action that is not
     * matched by an unlock action.
     *
     * C.
     * <p>The hold count information is typically only used for testing and
     * debugging purposes. For example, if a certain section of code should
     * not be entered with the lock already held then we can assert that
     * fact:
     *
     *  <pre> {@code
     * class X {
     *   ReentrantLock lock = new ReentrantLock();
     *   // ...
     *   public void m() {
     *     assert lock.getHoldCount() == 0;
     *     lock.lock();
     *     try {
     *       // ... method body
     *     } finally {
     *       lock.unlock();
     *     }
     *   }
     * }}</pre>
     *
     * @return the number of holds on this lock by the current thread,
     *         or zero if this lock is not held by the current thread
     */
    // 查询当前线程持有该锁的次数
    public int getHoldCount() {
        return sync.getHoldCount();
    }

    /**
     * 20210809
     * A. 查询当前线程是否持有此锁。
     * B. 类似于内置监视器锁的{@link Thread#holdsLock(Object)}方法，此方法通常用于调试和测试。例如，一个只应在持有锁时调用的方法可以断言是这种情况：
     *  {@code
     * class X {
     *   ReentrantLock lock = new ReentrantLock();
     *   // ...
     *
     *   public void m() {
     *       assert lock.isHeldByCurrentThread();
     *       // ... method body
     *   }
     * }}
     * C. 它还可以用于确保以不可重入的方式使用可重入锁，例如：
     *  {@code
     * class X {
     *   ReentrantLock lock = new ReentrantLock();
     *   // ...
     *
     *   public void m() {
     *       assert !lock.isHeldByCurrentThread();
     *       lock.lock();
     *       try {
     *           // ... method body
     *       } finally {
     *           lock.unlock();
     *       }
     *   }
     * }}
     */
    /**
     * A.
     * Queries if this lock is held by the current thread.
     *
     * B.
     * <p>Analogous to the {@link Thread#holdsLock(Object)} method for
     * built-in monitor locks, this method is typically used for
     * debugging and testing. For example, a method that should only be
     * called while a lock is held can assert that this is the case:
     *
     *  <pre> {@code
     * class X {
     *   ReentrantLock lock = new ReentrantLock();
     *   // ...
     *
     *   public void m() {
     *       assert lock.isHeldByCurrentThread();
     *       // ... method body
     *   }
     * }}</pre>
     *
     * C.
     * <p>It can also be used to ensure that a reentrant lock is used
     * in a non-reentrant manner, for example:
     *
     *  <pre> {@code
     * class X {
     *   ReentrantLock lock = new ReentrantLock();
     *   // ...
     *
     *   public void m() {
     *       assert !lock.isHeldByCurrentThread();
     *       lock.lock();
     *       try {
     *           // ... method body
     *       } finally {
     *           lock.unlock();
     *       }
     *   }
     * }}</pre>
     *
     * // {@code true} 如果当前线程持有这个锁，否则 {@code false}
     * @return {@code true} if current thread holds this lock and
     *         {@code false} otherwise
     */
    // 查询当前线程是否持有此锁
    public boolean isHeldByCurrentThread() {
        return sync.isHeldExclusively();
    }

    // 查询此锁是否被任何线程持有。此方法设计用于监视系统状态，而不是用于同步控制。
    /**
     * Queries if this lock is held by any thread. This method is
     * designed for use in monitoring of the system state,
     * not for synchronization control.
     *
     * // {@code true} 如果任何线程持有此锁，否则 {@code false}
     * @return {@code true} if any thread holds this lock and
     *         {@code false} otherwise
     */
    // 查询此锁是否被任何线程持有
    public boolean isLocked() {
        return sync.isLocked();
    }

    // 如果此锁的公平性设置为 true，则返回 {@code true}。
    /**
     * Returns {@code true} if this lock has fairness set true.
     *
     * @return {@code true} if this lock has fairness set true
     */
    // 判断此锁是否为公平锁
    public final boolean isFair() {
        return sync instanceof FairSync;
    }

    /**
     * 20210809
     * 返回当前拥有此锁的线程，如果不拥有，则返回 {@code null}。
     * 当此方法由不是所有者的线程调用时，返回值反映了当前锁定状态的尽力而为的近似值。
     * 例如，即使有线程试图获取锁但尚未这样做，所有者也可能暂时{@code null}。 此方法旨在促进子类的构建，以提供更广泛的锁监控设施。
     */
    /**
     * Returns the thread that currently owns this lock, or
     * {@code null} if not owned. When this method is called by a
     * thread that is not the owner, the return value reflects a
     * best-effort approximation of current lock status. For example,
     * the owner may be momentarily {@code null} even if there are
     * threads trying to acquire the lock but have not yet done so.
     * This method is designed to facilitate construction of
     * subclasses that provide more extensive lock monitoring
     * facilities.
     *
     * @return the owner, or {@code null} if not owned
     */
    // 返回当前拥有此锁的线程，如果不拥有，则返回 {@code null}
    protected Thread getOwner() {
        return sync.getOwner();
    }

    // 查询是否有线程正在等待获取此锁。请注意，因为取消可能随时发生，返回{@code true}并不能保证任何其他线程将永远获得此锁。 该方法主要设计用于监视系统状态。
    /**
     * Queries whether any threads are waiting to acquire this lock. Note that
     * because cancellations may occur at any time, a {@code true}
     * return does not guarantee that any other thread will ever
     * acquire this lock.  This method is designed primarily for use in
     * monitoring of the system state.
     *
     * @return {@code true} if there may be other threads waiting to
     *         acquire the lock
     */
    // 查询是否有线程正在等待获取此锁
    public final boolean hasQueuedThreads() {
        return sync.hasQueuedThreads();
    }

    // 查询给定线程是否正在等待获取此锁。请注意，因为取消可能随时发生，{@code true} 返回并不能保证该线程将永远获得此锁。 该方法主要设计用于监视系统状态。
    /**
     * Queries whether the given thread is waiting to acquire this
     * lock. Note that because cancellations may occur at any time, a
     * {@code true} return does not guarantee that this thread
     * will ever acquire this lock.  This method is designed primarily for use
     * in monitoring of the system state.
     *
     * @param thread the thread
     * @return {@code true} if the given thread is queued waiting for this lock
     * @throws NullPointerException if the thread is null
     */
    // 查询给定线程是否正在等待获取此锁
    public final boolean hasQueuedThread(Thread thread) {
        // 判断目标线程是否在CLH队列中: 从尾往前遍历, 如果Thread为同一个地址, 在返回true, 否则返回false
        return sync.isQueued(thread);
    }

    /**
     * 20210809
     * 返回等待获取此锁的线程数的估计值。该值只是一个估计值，因为当此方法遍历内部数据结构时，线程数可能会动态变化。此方法设计用于监视系统状态，而不是用于同步控制。
     */
    /**
     * Returns an estimate of the number of threads waiting to
     * acquire this lock.  The value is only an estimate because the number of
     * threads may change dynamically while this method traverses
     * internal data structures.  This method is designed for use in
     * monitoring of the system state, not for synchronization
     * control.
     *
     * @return the estimated number of threads waiting for this lock
     */
    // 返回等待获取此锁的线程数的估计值
    public final int getQueueLength() {
        // 从尾向前遍历统计非null的排队线程个数
        return sync.getQueueLength();
    }

    // 返回一个包含可能正在等待获取此锁的线程的集合。 由于在构造此结果时实际线程集可能会动态更改，因此返回的集合只是尽力而为的估计。 返回集合的元素没有特定的顺序。
    // 此方法旨在促进子类的构建，以提供更广泛的监视设施。
    /**
     * Returns a collection containing threads that may be waiting to
     * acquire this lock.  Because the actual set of threads may change
     * dynamically while constructing this result, the returned
     * collection is only a best-effort estimate.  The elements of the
     * returned collection are in no particular order.  This method is
     * designed to facilitate construction of subclasses that provide
     * more extensive monitoring facilities.
     *
     * @return the collection of threads
     */
    // 返回一个包含可能正在等待获取此锁的线程的集合
    protected Collection<Thread> getQueuedThreads() {
        // 从尾向前遍历获取非null的排队线程列表
        return sync.getQueuedThreads();
    }

    /**
     * 20210809
     * 查询是否有任何线程正在等待与此锁关联的给定条件。请注意，由于超时和中断可能随时发生，因此返回{@code true}并不能保证未来的{@code signal}会唤醒任何线程。
     * 该方法主要设计用于监视系统状态。
     */
    /**
     * Queries whether any threads are waiting on the given condition
     * associated with this lock. Note that because timeouts and
     * interrupts may occur at any time, a {@code true} return does
     * not guarantee that a future {@code signal} will awaken any
     * threads.  This method is designed primarily for use in
     * monitoring of the system state.
     *
     * @param condition the condition
     * @return {@code true} if there are any waiting threads
     * @throws IllegalMonitorStateException if this lock is not held
     * @throws IllegalArgumentException if the given condition is
     *         not associated with this lock
     * @throws NullPointerException if the condition is null
     */
    // 查询指定Condition对象下是否有等待线程 => 从头遍历条件队列, 如果结点状态为CONDITION结点, 则返回true, 否则返回false
    public boolean hasWaiters(Condition condition) {
        if (condition == null)
            throw new NullPointerException();
        if (!(condition instanceof AbstractQueuedSynchronizer.ConditionObject))
            throw new IllegalArgumentException("not owner");

        // 查询是否有线程在此条件下等待, 从头遍历条件队列, 如果结点状态为CONDITION结点, 则返回true, 否则返回false
        return sync.hasWaiters((AbstractQueuedSynchronizer.ConditionObject)condition);
    }

    // 返回等待与此锁关联的给定条件的线程数的估计值。
    // 请注意，由于超时和中断可能随时发生，因此估计值仅用作实际服务员人数的上限。此方法设计用于监视系统状态，而不是用于同步控制。
    /**
     * Returns an estimate of the number of threads waiting on the
     * given condition associated with this lock. Note that because
     * timeouts and interrupts may occur at any time, the estimate
     * serves only as an upper bound on the actual number of waiters.
     * This method is designed for use in monitoring of the system
     * state, not for synchronization control.
     *
     * @param condition the condition
     * @return the estimated number of waiting threads
     * @throws IllegalMonitorStateException if this lock is not held
     * @throws IllegalArgumentException if the given condition is
     *         not associated with this lock
     * @throws NullPointerException if the condition is null
     */
    // 返回等待与此锁关联的给定条件的线程数的估计值
    public int getWaitQueueLength(Condition condition) {
        if (condition == null)
            throw new NullPointerException();
        if (!(condition instanceof AbstractQueuedSynchronizer.ConditionObject))
            throw new IllegalArgumentException("not owner");

        // 返回等待此条件的线程数的估计值 => 从头遍历条件队列, 统计CONDITION结点的个数
        return sync.getWaitQueueLength((AbstractQueuedSynchronizer.ConditionObject)condition);
    }

    // 返回一个包含可能正在等待与此锁关联的给定条件的线程的集合。 由于在构造此结果时实际线程集可能会动态更改，因此返回的集合只是尽力而为的估计。
    // 返回集合的元素没有特定的顺序。 此方法旨在促进子类的构建，以提供更广泛的状态监控设施。
    /**
     * Returns a collection containing those threads that may be
     * waiting on the given condition associated with this lock.
     * Because the actual set of threads may change dynamically while
     * constructing this result, the returned collection is only a
     * best-effort estimate. The elements of the returned collection
     * are in no particular order.  This method is designed to
     * facilitate construction of subclasses that provide more
     * extensive condition monitoring facilities.
     *
     * @param condition the condition
     * @return the collection of threads
     * @throws IllegalMonitorStateException if this lock is not held
     * @throws IllegalArgumentException if the given condition is
     *         not associated with this lock
     * @throws NullPointerException if the condition is null
     */
    // 返回一个包含可能正在等待此Condition的线程的集合 => 从头遍历条件队列, 收集CONDITION结点的线程
    protected Collection<Thread> getWaitingThreads(Condition condition) {
        if (condition == null)
            throw new NullPointerException();
        if (!(condition instanceof AbstractQueuedSynchronizer.ConditionObject))
            throw new IllegalArgumentException("not owner");

        // 返回一个包含可能正在等待此Condition的线程的集合 => 从头遍历条件队列, 收集CONDITION结点的线程
        return sync.getWaitingThreads((AbstractQueuedSynchronizer.ConditionObject)condition);
    }

    // 返回标识此锁及其锁状态的字符串。括号中的状态包括字符串 {@code "Unlocked"} 或字符串 {@code "Locked by"} 后跟拥有线程的 {@linkplain Thread#getName name}。
    /**
     * Returns a string identifying this lock, as well as its lock state.
     * The state, in brackets, includes either the String {@code "Unlocked"}
     * or the String {@code "Locked by"} followed by the
     * {@linkplain Thread#getName name} of the owning thread.
     *
     * @return a string identifying this lock, as well as its lock state
     */
    // 返回标识此锁及其锁状态的字符串, 其中包括独占线程的名称
    public String toString() {
        Thread o = sync.getOwner();
        return super.toString() + ((o == null) ?
                                   "[Unlocked]" :
                                   "[Locked by thread " + o.getName() + "]");
    }
}
