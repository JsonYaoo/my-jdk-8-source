/*
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util.concurrent;
import java.util.Collection;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * 20210812
 * A. 计数信号量。从概念上讲，信号量维护一组许可。每个{@link#acquire}块如果必要的话，直到许可证是可用的，然后取它。每个{@link#RELEASE}添加一个许可，潜在地释放阻挡获取器。
 *    但是，没有使用实际的许可对象；在{@code Semaphore}只是让数的计数可用，采取相应的行动。
 * B. 信号量通常用于限制可以访问某些（物理或逻辑）资源的线程数。 例如，这是一个使用信号量来控制对项目池的访问的类：
 * {@code
 * class Pool {
 *   private static final int MAX_AVAILABLE = 100;
 *   private final Semaphore available = new Semaphore(MAX_AVAILABLE, true);
 *
 *   public Object getItem() throws InterruptedException {
 *     available.acquire();
 *     return getNextAvailableItem();
 *   }
 *
 *   public void putItem(Object x) {
 *     if (markAsUnused(x))
 *       available.release();
 *   }
 *
 *   // 不是特别有效的数据结构； 只是为了演示
 *   // Not a particularly efficient data structure; just for demo
 *
 *   protected Object[] items = ... whatever kinds of items being managed
 *   protected boolean[] used = new boolean[MAX_AVAILABLE];
 *
 *   protected synchronized Object getNextAvailableItem() {
 *     for (int i = 0; i < MAX_AVAILABLE; ++i) {
 *       if (!used[i]) {
 *          used[i] = true;
 *          return items[i];
 *       }
 *     }
 *     return null; // not reached
 *   }
 *
 *   protected synchronized boolean markAsUnused(Object item) {
 *     for (int i = 0; i < MAX_AVAILABLE; ++i) {
 *       if (item == items[i]) {
 *          if (used[i]) {
 *            used[i] = false;
 *            return true;
 *          } else
 *            return false;
 *       }
 *     }
 *     return false;
 *   }
 * }}
 * C. 在获得一个项目之前，每个线程必须从信号量中获得一个许可，以保证一个项目可供使用。当线程处理完该项目后，它会返回到池中，并且将许可返回给信号量，从而允许另一个线程获取该项目。
 *    请注意，调用 {@link #acquire} 时不会持有同步锁，因为这会阻止项目返回到池中。 信号量封装了限制访问池所需的同步，与维护池本身一致性所需的任何同步分开。
 * D. 初始化为 1 的信号量，并且使用时最多只有一个许可可用，可以用作互斥锁。 这通常被称为二元信号量，因为它只有两种状态：一个许可可用，或零个许可可用。
 *    以这种方式使用时，二进制信号量具有属性（与许多 {@link java.util.concurrent.locks.Lock} 实现不同），即“锁”可以由所有者以外的线程释放（因为信号量具有没有所有权的概念）。
 *    这在某些特定上下文中很有用，例如死锁恢复。
 * E. 此类的构造函数可以选择接受公平参数。当设置为 false 时，此类不保证线程获取许可的顺序。特别是，允许​​插入，
 *    即调用 {@link #acquire} 的线程可以在一直等待的线程之前分配一个许可——逻辑上，新线程将自己置于等待线程队列的头部。当公平性设置为真时，
 *    信号量保证调用任何 {@link #acquire()acquire} 方法的线程被选择以按照它们对这些方法的调用的处理顺序（先进先出）获得许可。先进先出）。
 *    请注意，FIFO 排序必然适用于这些方法中的特定内部执行点。因此，一个线程有可能在另一个线程之前调用 {@code Acquire}，但在另一个线程之后到达排序点，从方法返回时也是如此。
 *    另请注意，未计时的 {@link #tryAcquire() tryAcquire} 方法不遵守公平设置，但会采用任何可用的许可。
 * F. 通常，用于控制资源访问的信号量应初始化为公平的，以确保没有线程因访问资源而饿死。 当使用信号量进行其他类型的同步控制时，非公平排序的吞吐量优势通常超过公平性考虑。
 * G. 此类还提供了一次 {@link #acquire(int)acquire} 和 {@link #release(int) release} 多个许可的便捷方法。 当在没有公平设置的情况下使用这些方法时，请注意无限期推迟的风险增加。
 * H. 内存一致性影响：在调用“释放”方法（例如 {@code release()}）之前线程中的操作发生在另一个线程中成功的“获取”方法（例如 {@code Acquire()}）之后的操作之前。
 */
/**
 * A.
 * A counting semaphore.  Conceptually, a semaphore maintains a set of
 * permits.  Each {@link #acquire} blocks if necessary until a permit is
 * available, and then takes it.  Each {@link #release} adds a permit,
 * potentially releasing a blocking acquirer.
 * However, no actual permit objects are used; the {@code Semaphore} just
 * keeps a count of the number available and acts accordingly.
 *
 * B.
 * <p>Semaphores are often used to restrict the number of threads than can
 * access some (physical or logical) resource. For example, here is
 * a class that uses a semaphore to control access to a pool of items:
 *  <pre> {@code
 * class Pool {
 *   private static final int MAX_AVAILABLE = 100;
 *   private final Semaphore available = new Semaphore(MAX_AVAILABLE, true);
 *
 *   public Object getItem() throws InterruptedException {
 *     available.acquire();
 *     return getNextAvailableItem();
 *   }
 *
 *   public void putItem(Object x) {
 *     if (markAsUnused(x))
 *       available.release();
 *   }
 *
 *   // Not a particularly efficient data structure; just for demo
 *
 *   protected Object[] items = ... whatever kinds of items being managed
 *   protected boolean[] used = new boolean[MAX_AVAILABLE];
 *
 *   protected synchronized Object getNextAvailableItem() {
 *     for (int i = 0; i < MAX_AVAILABLE; ++i) {
 *       if (!used[i]) {
 *          used[i] = true;
 *          return items[i];
 *       }
 *     }
 *     return null; // not reached
 *   }
 *
 *   protected synchronized boolean markAsUnused(Object item) {
 *     for (int i = 0; i < MAX_AVAILABLE; ++i) {
 *       if (item == items[i]) {
 *          if (used[i]) {
 *            used[i] = false;
 *            return true;
 *          } else
 *            return false;
 *       }
 *     }
 *     return false;
 *   }
 * }}</pre>
 *
 * C.
 * <p>Before obtaining an item each thread must acquire a permit from
 * the semaphore, guaranteeing that an item is available for use. When
 * the thread has finished with the item it is returned back to the
 * pool and a permit is returned to the semaphore, allowing another
 * thread to acquire that item.  Note that no synchronization lock is
 * held when {@link #acquire} is called as that would prevent an item
 * from being returned to the pool.  The semaphore encapsulates the
 * synchronization needed to restrict access to the pool, separately
 * from any synchronization needed to maintain the consistency of the
 * pool itself.
 *
 * D.
 * <p>A semaphore initialized to one, and which is used such that it
 * only has at most one permit available, can serve as a mutual
 * exclusion lock.  This is more commonly known as a <em>binary
 * semaphore</em>, because it only has two states: one permit
 * available, or zero permits available.  When used in this way, the
 * binary semaphore has the property (unlike many {@link java.util.concurrent.locks.Lock}
 * implementations), that the &quot;lock&quot; can be released by a
 * thread other than the owner (as semaphores have no notion of
 * ownership).  This can be useful in some specialized contexts, such
 * as deadlock recovery.
 *
 * E.
 * <p> The constructor for this class optionally accepts a
 * <em>fairness</em> parameter. When set false, this class makes no
 * guarantees about the order in which threads acquire permits. In
 * particular, <em>barging</em> is permitted, that is, a thread
 * invoking {@link #acquire} can be allocated a permit ahead of a
 * thread that has been waiting - logically the new thread places itself at
 * the head of the queue of waiting threads. When fairness is set true, the
 * semaphore guarantees that threads invoking any of the {@link
 * #acquire() acquire} methods are selected to obtain permits in the order in
 * which their invocation of those methods was processed
 * (first-in-first-out; FIFO). Note that FIFO ordering necessarily
 * applies to specific internal points of execution within these
 * methods.  So, it is possible for one thread to invoke
 * {@code acquire} before another, but reach the ordering point after
 * the other, and similarly upon return from the method.
 * Also note that the untimed {@link #tryAcquire() tryAcquire} methods do not
 * honor the fairness setting, but will take any permits that are
 * available.
 *
 * F.
 * <p>Generally, semaphores used to control resource access should be
 * initialized as fair, to ensure that no thread is starved out from
 * accessing a resource. When using semaphores for other kinds of
 * synchronization control, the throughput advantages of non-fair
 * ordering often outweigh fairness considerations.
 *
 * G.
 * <p>This class also provides convenience methods to {@link
 * #acquire(int) acquire} and {@link #release(int) release} multiple
 * permits at a time.  Beware of the increased risk of indefinite
 * postponement when these methods are used without fairness set true.
 *
 * H.
 * <p>Memory consistency effects: Actions in a thread prior to calling
 * a "release" method such as {@code release()}
 * <a href="package-summary.html#MemoryVisibility"><i>happen-before</i></a>
 * actions following a successful "acquire" method such as {@code acquire()}
 * in another thread.
 *
 * @since 1.5
 * @author Doug Lea
 */
public class Semaphore implements java.io.Serializable {

    private static final long serialVersionUID = -3222578661600680210L;

    // 通过 AbstractQueuedSynchronizer 子类的所有机制
    /** All mechanics via AbstractQueuedSynchronizer subclass */
    private final Sync sync;// 同步执行对象

    // 信号量的同步实现。 使用 AQS 状态表示许可。 分为公平和非公平版本。
    /**
     * Synchronization implementation for semaphore.  Uses AQS state
     * to represent permits. Subclassed into fair and nonfair
     * versions.
     */
    // Semaphore同步控制对象
    abstract static class Sync extends AbstractQueuedSynchronizer {

        private static final long serialVersionUID = 1192457210091910933L;

        // 构造Semaphore同步控制对象, 设置许可数量, 使用同步器状态作为许可数量
        Sync(int permits) {
            setState(permits);
        }

        // 获取当前许可数量
        final int getPermits() {
            return getState();
        }

        // 非公平方式获取同步器状态
        final int nonfairTryAcquireShared(int acquires) {
            // 开始自旋
            for (;;) {
                // 获取最新的同步器状态
                int available = getState();

                // 扣减要获取的同步器状态
                int remaining = available - acquires;

                // 如果同步器状态不够, 或者CAS成功扣减时, 则返回剩余计数
                if (remaining < 0 ||
                    compareAndSetState(available, remaining))
                    // 如果返回负数, 代表数量不够, 获取失败; 如果返回非负数, 代表数量足够, 获取成功
                    return remaining;

                // 如果同步器够但CAS扣减失败, 则继续自旋
            }
        }

        // 尝试释放共享模式的同步器状态
        protected final boolean tryReleaseShared(int releases) {
            // 开始自旋
            for (;;) {
                // 获取最新的同步器状态
                int current = getState();

                // 加上需要获取的同步器状态
                int next = current + releases;

                // 如果相加失败, 则抛出异常
                if (next < current) // overflow
                    throw new Error("Maximum permit count exceeded");

                // 如果相加成功, 则CAS更新同步器状态, 返回true, 代表释放成功
                if (compareAndSetState(current, next))
                    return true;

                // 如果CAS更新失败, 则继续自旋
            }
        }

        // 减少信号量许可证
        final void reducePermits(int reductions) {
            // 开始自旋
            for (;;) {
                // 获取最新的同步器状态
                int current = getState();

                // 减去需要获取的同步器状态
                int next = current - reductions;

                // 如果扣减失败, 则抛出异常
                if (next > current) // underflow
                    throw new Error("Permit count underflow");

                // 如果扣减成功, 则CAS更新同步器状态
                if (compareAndSetState(current, next))
                    return;

                // 如果CAS更新成功, 则继续自旋
            }
        }

        // 清空信号量许可证
        final int drainPermits() {
            // 开始自旋
            for (;;) {
                // 获取最新的同步器状态
                int current = getState();

                // 如果同步器状态为0, 或者CAS更新为0成功, 则返回current(可能会为0, 可能不为0)
                if (current == 0 || compareAndSetState(current, 0))
                    return current;

                // 如果CAS更新失败, 则继续自旋
            }
        }
    }

    // 非公平版本
    /**
     * NonFair version
     */
    // 非公平版同步控制实现类
    static final class NonfairSync extends Sync {

        private static final long serialVersionUID = -2694183684443567898L;

        // 构造非公平版同步控制实现类对象, 设置许可数量, 使用同步器状态作为许可数量
        NonfairSync(int permits) {
            super(permits);
        }

        // 非公平获取同步器状态
        protected int tryAcquireShared(int acquires) {
            return nonfairTryAcquireShared(acquires);
        }
    }

    // 公平版
    /**
     * Fair version
     */
    // 公平版同步控制实现类
    static final class FairSync extends Sync {

        private static final long serialVersionUID = 2014338818796000944L;

        // 构造公平版同步控制实现类对象, 设置许可数量, 使用同步器状态作为许可数量
        FairSync(int permits) {
            super(permits);
        }

        // 尝试以共享模式获取同步器状态, 失败(<0), 获取成功但后续共享不成功(=0), 获取成功且后续共享成功(>0)
        protected int tryAcquireShared(int acquires) {
            // 开始自旋
            for (;;) {
                // 判断此刻CLH中是否存在有等待时间比当前线程长的线程, 旨在由公平同步器使用, 以避免插入结点到CLH队列中
                if (hasQueuedPredecessors())
                    // 如果存在排队线程, 则返回-1, 代表同步器状态获取失败
                    return -1;

                // 获取最新的同步器状态
                int available = getState();

                // 扣减需要获取的同步器状态
                int remaining = available - acquires;

                // 如果同步器状态不够, 或者CAS成功扣减时, 则返回剩余计数
                if (remaining < 0 ||
                    compareAndSetState(available, remaining))
                    return remaining;

                // 如果同步器够但CAS扣减失败, 则继续自旋
            }
        }
    }

    // 使用给定数量的许可和非公平公平设置创建一个 {@code Semaphore}。
    /**
     * Creates a {@code Semaphore} with the given number of
     * permits and nonfair fairness setting.
     *
     * @param permits the initial number of permits available.
     *        This value may be negative, in which case releases
     *        must occur before any acquires will be granted.
     */
    // 使用给定数量的许可设置创建一个非公平版本的{@code Semaphore}
    public Semaphore(int permits) {
        sync = new NonfairSync(permits);
    }

    // 创建{@code信号量}与所述给定的许可数和给定的公平设置。
    /**
     * Creates a {@code Semaphore} with the given number of
     * permits and the given fairness setting.
     *
     * // permit 可用的初始许可证数量。 该值可能为负数，在这种情况下，必须在授予任何获取之前进行释放。
     * @param permits the initial number of permits available.
     *        This value may be negative, in which case releases
     *        must occur before any acquires will be granted.
     *
     * // 公平 {@code true} 如果此信号量将保证争用许可的先进先出授予，否则 {@code false}
     * @param fair {@code true} if this semaphore will guarantee
     *        first-in first-out granting of permits under contention,
     *        else {@code false}
     */
    // 使用给定数量的许可设置创建一个{@code Semaphore}, 为true时为公平模式, 为false时为非公平模式
    public Semaphore(int permits, boolean fair) {
        sync = fair ? new FairSync(permits) : new NonfairSync(permits);
    }

    /**
     * 20210812
     * A. 从此信号量获取许可，阻塞直到一个可用，或者线程{@linkplain Thread#interrupt interrupted}。
     * B. 获得许可证（如果有）并立即返回，将可用许可证的数量减少一个。
     * C. 如果没有可用的许可，则当前线程将出于线程调度目的而被禁用并处于休眠状态，直到发生以下两种情况之一：
     *      a. 其他一些线程为此信号量调用 {@link #release} 方法，当前线程接下来将被分配一个许可；
     *      b. 其他一些线程 {@linkplain Thread#interrupt 中断}当前线程。
     * D. 如果当前线程：
     *      a. 在进入此方法时设置其中断状态；
     *      b. 在等待许可时{@linkplain Thread#interrupt interrupted}，
     *    然后抛出 {@link InterruptedException} 并清除当前线程的中断状态。
     */
    /**
     * A.
     * Acquires a permit from this semaphore, blocking until one is
     * available, or the thread is {@linkplain Thread#interrupt interrupted}.
     *
     * B.
     * <p>Acquires a permit, if one is available and returns immediately,
     * reducing the number of available permits by one.
     *
     * C.
     * <p>If no permit is available then the current thread becomes
     * disabled for thread scheduling purposes and lies dormant until
     * one of two things happens:
     * <ul>
     * <li>Some other thread invokes the {@link #release} method for this
     * semaphore and the current thread is next to be assigned a permit; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * the current thread.
     * </ul>
     *
     * D.
     * <p>If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@linkplain Thread#interrupt interrupted} while waiting
     * for a permit,
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     *
     * @throws InterruptedException if the current thread is interrupted
     */
    // 以阻塞、共享可中断模式获取同步器状态
    public void acquire() throws InterruptedException {
        sync.acquireSharedInterruptibly(1);
    }

    /**
     * 20210812
     * A. 从此信号量获取许可，阻塞直到一个可用。
     * B. 获得许可证（如果有）并立即返回，将可用许可证的数量减少一个。
     * C. 如果没有可用的许可，则当前线程将出于线程调度目的而被禁用并处于休眠状态，直到某个其他线程为此信号量调用 {@link #release} 方法并且当前线程接下来将被分配许可。
     * D. 如果当前线程在等待许可时 {@linkplain Thread#interrupt interrupted} 则它将继续等待，但线程被分配许可的时间可能与它收到许可的时间相比发生变化没有发生中断。
     *    当线程确实从此方法返回时，将设置其中断状态。
     */
    /**
     * A.
     * Acquires a permit from this semaphore, blocking until one is
     * available.
     *
     * B.
     * <p>Acquires a permit, if one is available and returns immediately,
     * reducing the number of available permits by one.
     *
     * C.
     * <p>If no permit is available then the current thread becomes
     * disabled for thread scheduling purposes and lies dormant until
     * some other thread invokes the {@link #release} method for this
     * semaphore and the current thread is next to be assigned a permit.
     *
     * D.
     * <p>If the current thread is {@linkplain Thread#interrupt interrupted}
     * while waiting for a permit then it will continue to wait, but the
     * time at which the thread is assigned a permit may change compared to
     * the time it would have received the permit had no interruption
     * occurred.  When the thread does return from this method its interrupt
     * status will be set.
     */
    // 以阻塞、不可中断、共享模式获取同步器状态
    public void acquireUninterruptibly() {
        sync.acquireShared(1);
    }

    /**
     * 20210812
     * A. 从该信号量获取许可，仅当在调用时可用。
     * B. 获取一个许可证，如果有一个可用并立即返回，值为 {@code true}，将可用许可证的数量减少一。
     * C. 如果没有可用的许可，则此方法将立即返回值 {@code false}。
     * D. 即使此信号量已设置为使用公平排序策略，调用 {@code tryAcquire()} 也会立即获取许可（如果有），无论其他线程当前是否正在等待。
     *    这种“闯入”行为在某些情况下很有用，即使它破坏了公平。 如果你想尊重公平性设置，那么使用 {@link #tryAcquire(long, TimeUnit) tryAcquire(0, TimeUnit.SECONDS) }
     *    这几乎是等效的（它也检测中断）。
     */
    /**
     * A.
     * Acquires a permit from this semaphore, only if one is available at the
     * time of invocation.
     *
     * B.
     * <p>Acquires a permit, if one is available and returns immediately,
     * with the value {@code true},
     * reducing the number of available permits by one.
     *
     * C.
     * <p>If no permit is available then this method will return
     * immediately with the value {@code false}.
     *
     * D.
     * <p>Even when this semaphore has been set to use a
     * fair ordering policy, a call to {@code tryAcquire()} <em>will</em>
     * immediately acquire a permit if one is available, whether or not
     * other threads are currently waiting.
     * This &quot;barging&quot; behavior can be useful in certain
     * circumstances, even though it breaks fairness. If you want to honor
     * the fairness setting, then use
     * {@link #tryAcquire(long, TimeUnit) tryAcquire(0, TimeUnit.SECONDS) }
     * which is almost equivalent (it also detects interruption).
     *
     * @return {@code true} if a permit was acquired and {@code false}
     *         otherwise
     */
    // 非公平快速失败方式获取同步器状态
    public boolean tryAcquire() {
        return sync.nonfairTryAcquireShared(1) >= 0;
    }

    /**
     * 20210812
     * A. 如果一个信号量在给定的等待时间内变得可用并且当前线程未被{@linkplain Thread#interrupt interrupted}，则从此信号量获取许可。
     * B. 获取一个许可证，如果有一个可用并立即返回，值为 {@code true}，将可用许可证的数量减少一。
     * C. 如果没有可用的许可，则当前线程将出于线程调度目的而被禁用并处于休眠状态，直到发生以下三种情况之一：
     *      a. 其他一些线程为此信号量调用 {@link #release} 方法，当前线程接下来将被分配一个许可；
     *      b. 其他一些线程 {@linkplain Thread#interrupt 中断}当前线程；
     *      c. 指定的等待时间已过。
     * D. 如果获得许可，则返回值 {@code true}。
     * E. 如果当前线程：
     *      a. 在进入此方法时设置其中断状态；
     *      b. 在等待获得许可时{@linkplain Thread#interrupt interrupted}，
     *    然后抛出 {@link InterruptedException} 并清除当前线程的中断状态。
     * F. 如果指定的等待时间过去，则返回值 {@code false}。 如果时间小于或等于零，则该方法根本不会等待。
     */
    /**
     * A.
     * Acquires a permit from this semaphore, if one becomes available
     * within the given waiting time and the current thread has not
     * been {@linkplain Thread#interrupt interrupted}.
     *
     * B.
     * <p>Acquires a permit, if one is available and returns immediately,
     * with the value {@code true},
     * reducing the number of available permits by one.
     *
     * C.
     * <p>If no permit is available then the current thread becomes
     * disabled for thread scheduling purposes and lies dormant until
     * one of three things happens:
     * <ul>
     * <li>Some other thread invokes the {@link #release} method for this
     * semaphore and the current thread is next to be assigned a permit; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * the current thread; or
     * <li>The specified waiting time elapses.
     * </ul>
     *
     * D.
     * <p>If a permit is acquired then the value {@code true} is returned.
     *
     * E.
     * <p>If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@linkplain Thread#interrupt interrupted} while waiting
     * to acquire a permit,
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     *
     * F.
     * <p>If the specified waiting time elapses then the value {@code false}
     * is returned.  If the time is less than or equal to zero, the method
     * will not wait at all.
     *
     * @param timeout the maximum time to wait for a permit
     * @param unit the time unit of the {@code timeout} argument
     * @return {@code true} if a permit was acquired and {@code false}
     *         if the waiting time elapsed before a permit was acquired
     * @throws InterruptedException if the current thread is interrupted
     */
    // 以阻塞、共享、定时、可中断模式获取同步器状态
    public boolean tryAcquire(long timeout, TimeUnit unit) throws InterruptedException {
        return sync.tryAcquireSharedNanos(1, unit.toNanos(timeout));
    }

    /**
     * 20210812
     * A. 释放许可，将其返回给信号量。
     * B. 释放许可证，将可用许可证的数量增加一个。 如果有任何线程试图获得许可，则选择一个线程并给予刚刚释放的许可。 该线程已（重新）启用以用于线程调度目的。
     * C. 不要求释放许可的线程必须通过调用 {@link #acquire} 获得该许可。 信号量的正确使用是由应用程序中的编程约定确定的。
     */
    /**
     * A.
     * Releases a permit, returning it to the semaphore.
     *
     * B.
     * <p>Releases a permit, increasing the number of available permits by
     * one.  If any threads are trying to acquire a permit, then one is
     * selected and given the permit that was just released.  That thread
     * is (re)enabled for thread scheduling purposes.
     *
     * C.
     * <p>There is no requirement that a thread that releases a permit must
     * have acquired that permit by calling {@link #acquire}.
     * Correct usage of a semaphore is established by programming convention
     * in the application.
     */
    // 尝试释放共享模式的同步器状态, 如果释放成功, 则返回true; 如果释放失败, 则返回false
    public void release() {
        sync.releaseShared(1);
    }

    /**
     * 20210812
     * A. 从此信号量获取给定数量的许可，阻塞直到所有可用，或者线程{@linkplain Thread#interrupt interrupted}。
     * B. 获取给定数量的许可证（如果可用），并立即返回，减少给定数量的可用许可证数量。
     * C. 如果没有足够的许可可用，则当前线程将出于线程调度目的而被禁用并处于休眠状态，直到发生以下两种情况之一：
     *      a. 其他一些线程为此信号量调用 {@link #release() release} 方法之一，当前线程是下一个被分配的许可，并且可用许可的数量满足该请求；
     *      b. 其他一些线程 {@linkplain Thread#interrupt 中断}当前线程。
     * D. 如果当前线程：
     *      a. 在进入此方法时设置其中断状态；
     *      b. 在等待许可时{@linkplain Thread#interrupt interrupted}，
     *    然后抛出 {@link InterruptedException} 并清除当前线程的中断状态。将分配给该线程的任何许可改为分配给尝试获取许可的其他线程，
     *    就好像通过调用 {@link #release()} 获得了许可。
     */
    /**
     * A.
     * Acquires the given number of permits from this semaphore,
     * blocking until all are available,
     * or the thread is {@linkplain Thread#interrupt interrupted}.
     *
     * B.
     * <p>Acquires the given number of permits, if they are available,
     * and returns immediately, reducing the number of available permits
     * by the given amount.
     *
     * C.
     * <p>If insufficient permits are available then the current thread becomes
     * disabled for thread scheduling purposes and lies dormant until
     * one of two things happens:
     * <ul>
     * <li>Some other thread invokes one of the {@link #release() release}
     * methods for this semaphore, the current thread is next to be assigned
     * permits and the number of available permits satisfies this request; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * the current thread.
     * </ul>
     *
     * D.
     * <p>If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@linkplain Thread#interrupt interrupted} while waiting
     * for a permit,
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     * Any permits that were to be assigned to this thread are instead
     * assigned to other threads trying to acquire permits, as if
     * permits had been made available by a call to {@link #release()}.
     *
     * @param permits the number of permits to acquire
     * @throws InterruptedException if the current thread is interrupted
     * @throws IllegalArgumentException if {@code permits} is negative
     */
    // 以阻塞、共享可中断模式获取同步器状态
    public void acquire(int permits) throws InterruptedException {
        if (permits < 0) throw new IllegalArgumentException();
        sync.acquireSharedInterruptibly(permits);
    }

    /**
     * 20210812
     * A. 从此信号量获取给定数量的许可，阻塞直到所有许可都可用。
     * B. 获取给定数量的许可证（如果可用），并立即返回，减少给定数量的可用许可证数量。
     * C. 如果没有足够的许可可用，则当前线程将因线程调度目的而被禁用并处于休眠状态，直到某个其他线程为此信号量调用 {@link #release() release} 方法之一，
     *    当前线程接下来将被分配许可和 可用许可证的数量满足这一要求。
     * D. 如果当前线程在等待许可时{@linkplain Thread#interrupt interrupted}，那么它将继续等待并且其在队列中的位置不受影响。 当线程确实从此方法返回时，将设置其中断状态。
     */
    /**
     * A.
     * Acquires the given number of permits from this semaphore,
     * blocking until all are available.
     *
     * B.
     * <p>Acquires the given number of permits, if they are available,
     * and returns immediately, reducing the number of available permits
     * by the given amount.
     *
     * C.
     * <p>If insufficient permits are available then the current thread becomes
     * disabled for thread scheduling purposes and lies dormant until
     * some other thread invokes one of the {@link #release() release}
     * methods for this semaphore, the current thread is next to be assigned
     * permits and the number of available permits satisfies this request.
     *
     * D.
     * <p>If the current thread is {@linkplain Thread#interrupt interrupted}
     * while waiting for permits then it will continue to wait and its
     * position in the queue is not affected.  When the thread does return
     * from this method its interrupt status will be set.
     *
     * @param permits the number of permits to acquire
     * @throws IllegalArgumentException if {@code permits} is negative
     */
    // 以阻塞、不可中断、共享模式获取同步器状态
    public void acquireUninterruptibly(int permits) {
        if (permits < 0) throw new IllegalArgumentException();
        sync.acquireShared(permits);
    }

    /**
     * 20210812
     * A. 从这个信号量获取给定数量的许可，只有在调用时所有许可都可用。
     * B. 获取给定数量的许可证，如果它们可用，并立即返回，值为 {@code true}，将可用许可证的数量减少给定的数量。
     * C. 如果没有足够的许可证可用，则此方法将立即返回值 {@code false} 并且可用许可证的数量不变。
     * D. 即使此信号量已设置为使用公平排序策略，调用 {@code tryAcquire} 也会立即获取许可（如果有），无论其他线程当前是否正在等待。
     *    这种“闯入”行为在某些情况下很有用，即使它破坏了公平。 如果你想尊重公平性设置，
     *    那么使用 {@link #tryAcquire(int, long, TimeUnit) tryAcquire(permits, 0, TimeUnit.SECONDS) } 这几乎是等效的（它也检测中断）。
     */
    /**
     * A.
     * Acquires the given number of permits from this semaphore, only
     * if all are available at the time of invocation.
     *
     * B.
     * <p>Acquires the given number of permits, if they are available, and
     * returns immediately, with the value {@code true},
     * reducing the number of available permits by the given amount.
     *
     * C.
     * <p>If insufficient permits are available then this method will return
     * immediately with the value {@code false} and the number of available
     * permits is unchanged.
     *
     * D.
     * <p>Even when this semaphore has been set to use a fair ordering
     * policy, a call to {@code tryAcquire} <em>will</em>
     * immediately acquire a permit if one is available, whether or
     * not other threads are currently waiting.  This
     * &quot;barging&quot; behavior can be useful in certain
     * circumstances, even though it breaks fairness. If you want to
     * honor the fairness setting, then use {@link #tryAcquire(int,
     * long, TimeUnit) tryAcquire(permits, 0, TimeUnit.SECONDS) }
     * which is almost equivalent (it also detects interruption).
     *
     * @param permits the number of permits to acquire
     * @return {@code true} if the permits were acquired and
     *         {@code false} otherwise
     * @throws IllegalArgumentException if {@code permits} is negative
     */
    // 非公平快速失败方式获取同步器状态
    public boolean tryAcquire(int permits) {
        if (permits < 0) throw new IllegalArgumentException();
        return sync.nonfairTryAcquireShared(permits) >= 0;
    }

    /**
     * 20210812
     * A. 从此信号量获取给定数量的许可，如果在给定的等待时间内所有许可都可用，并且当前线程尚未{@linkplain Thread#interrupt interrupted}。
     * B. 获取给定数量的许可证，如果它们可用并立即返回，值为 {@code true}，将可用许可证的数量减少给定的数量。
     * C. 如果没有足够的许可可用，则当前线程将出于线程调度目的而被禁用并处于休眠状态，直到发生以下三种情况之一：
     *      a. 其他一些线程为此信号量调用 {@link #release() release} 方法之一，当前线程是下一个被分配的许可，并且可用许可的数量满足该请求；
     *      b. 其他一些线程 {@linkplain Thread#interrupt 中断}当前线程；
     *      c. 指定的等待时间已过。
     * D. 如果获得许可，则返回值 {@code true}。
     * E. 如果当前线程：
     *      a. 在进入此方法时设置其中断状态；
     *      b. 在等待获取许可时{@linkplain Thread#interrupt interrupted}，
     *    然后抛出 {@link InterruptedException} 并清除当前线程的中断状态。 将分配给该线程的任何许可，改为分配给尝试获取许可的其他线程，
     *    就好像通过调用 {@link #release()} 使许可可用一样。
     * F. 如果指定的等待时间过去，则返回值 {@code false}。 如果时间小于或等于零，则该方法根本不会等待。 将分配给该线程的任何许可，改为分配给尝试获取许可的其他线程，
     *    就好像通过调用 {@link #release()} 使许可可用一样。
     */
    /**
     * A.
     * Acquires the given number of permits from this semaphore, if all
     * become available within the given waiting time and the current
     * thread has not been {@linkplain Thread#interrupt interrupted}.
     *
     * B.
     * <p>Acquires the given number of permits, if they are available and
     * returns immediately, with the value {@code true},
     * reducing the number of available permits by the given amount.
     *
     * C.
     * <p>If insufficient permits are available then
     * the current thread becomes disabled for thread scheduling
     * purposes and lies dormant until one of three things happens:
     * <ul>
     * <li>Some other thread invokes one of the {@link #release() release}
     * methods for this semaphore, the current thread is next to be assigned
     * permits and the number of available permits satisfies this request; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * the current thread; or
     * <li>The specified waiting time elapses.
     * </ul>
     *
     * D.
     * <p>If the permits are acquired then the value {@code true} is returned.
     *
     * E.
     * <p>If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@linkplain Thread#interrupt interrupted} while waiting
     * to acquire the permits,
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     * Any permits that were to be assigned to this thread, are instead
     * assigned to other threads trying to acquire permits, as if
     * the permits had been made available by a call to {@link #release()}.
     *
     * F.
     * <p>If the specified waiting time elapses then the value {@code false}
     * is returned.  If the time is less than or equal to zero, the method
     * will not wait at all.  Any permits that were to be assigned to this
     * thread, are instead assigned to other threads trying to acquire
     * permits, as if the permits had been made available by a call to
     * {@link #release()}.
     *
     * @param permits the number of permits to acquire
     * @param timeout the maximum time to wait for the permits
     * @param unit the time unit of the {@code timeout} argument
     *
     * // {@code true} 如果获得了所有许可证，如果在获得所有许可证之前等待时间过去了 {@code false}
     * @return {@code true} if all permits were acquired and {@code false}
     *         if the waiting time elapsed before all permits were acquired
     * @throws InterruptedException if the current thread is interrupted
     * @throws IllegalArgumentException if {@code permits} is negative
     */
    // 以阻塞、共享、定时、可中断模式获取同步器状态
    public boolean tryAcquire(int permits, long timeout, TimeUnit unit) throws InterruptedException {
        if (permits < 0) throw new IllegalArgumentException();
        return sync.tryAcquireSharedNanos(permits, unit.toNanos(timeout));
    }

    /**
     * 20210812
     * A. 释放给定数量的许可，将它们返回给信号量。
     * B. 释放给定数量的许可，将可用许可的数量增加该数量。 如果任何线程试图获取许可，则选择一个线程并给予刚刚释放的许可。
     *    如果可用许可的数量满足该线程的请求，则该线程被（重新）启用以用于线程调度； 否则线程将等待，直到有足够的许可可用。
     *    如果在满足此线程的请求后仍有可用的许可，则这些许可将依次分配给其他试图获取许可的线程。
     * C. 不要求释放许可的线程必须通过调用 {@link Semaphore#acquire Acquire Acquire} 获得该许可。 信号量的正确使用是由应用程序中的编程约定建立的。
     */
    /**
     * A.
     * Releases the given number of permits, returning them to the semaphore.
     *
     * B.
     * <p>Releases the given number of permits, increasing the number of
     * available permits by that amount.
     * If any threads are trying to acquire permits, then one
     * is selected and given the permits that were just released.
     * If the number of available permits satisfies that thread's request
     * then that thread is (re)enabled for thread scheduling purposes;
     * otherwise the thread will wait until sufficient permits are available.
     * If there are still permits available
     * after this thread's request has been satisfied, then those permits
     * are assigned in turn to other threads trying to acquire permits.
     *
     * C.
     * <p>There is no requirement that a thread that releases a permit must
     * have acquired that permit by calling {@link Semaphore#acquire acquire}.
     * Correct usage of a semaphore is established by programming convention
     * in the application.
     *
     * @param permits the number of permits to release
     * @throws IllegalArgumentException if {@code permits} is negative
     */
    // 释放共享模式的同步器状态, 如果释放成功, 则返回true; 如果释放失败, 则返回false
    public void release(int permits) {
        if (permits < 0) throw new IllegalArgumentException();
        sync.releaseShared(permits);
    }

    /**
     * 20210812
     * A. 返回此信号量中可用的当前许可数。
     * B. 此方法通常用于调试和测试目的。
     */
    /**
     * A.
     * Returns the current number of permits available in this semaphore.
     *
     * B.
     * <p>This method is typically used for debugging and testing purposes.
     *
     * @return the number of permits available in this semaphore
     */
    // 返回此信号量中可用的当前许可数
    public int availablePermits() {
        return sync.getPermits();
    }

    // 获取并返回所有立即可用的许可证。
    /**
     * Acquires and returns all permits that are immediately available.
     *
     * @return the number of permits acquired
     */
    // 获取并返回所有立即可用的许可证
    public int drainPermits() {
        return sync.drainPermits();
    }

    /**
     * 20210812
     * 按指示的减少量减少可用许可证的数量。 此方法在使用信号量跟踪变得不可用的资源的子类中很有用。 此方法与 {@code Acquire} 的不同之处在于它不会阻止等待许可变为可用。
     */
    /**
     * Shrinks the number of available permits by the indicated
     * reduction. This method can be useful in subclasses that use
     * semaphores to track resources that become unavailable. This
     * method differs from {@code acquire} in that it does not block
     * waiting for permits to become available.
     *
     * @param reduction the number of permits to remove
     * @throws IllegalArgumentException if {@code reduction} is negative
     */
    // 按指示的减少量减少可用许可证的数量, 此方法在使用信号量跟踪变得不可用的资源的子类中很有用
    protected void reducePermits(int reduction) {
        if (reduction < 0) throw new IllegalArgumentException();
        sync.reducePermits(reduction);
    }

    // 如果此信号量的公平性设置为 true，则返回 {@code true}。
    /**
     * Returns {@code true} if this semaphore has fairness set true.
     *
     * @return {@code true} if this semaphore has fairness set true
     */
    // 判断信号量是否为公平版本, 如果此信号量的公平性设置为 true，则返回 {@code true}
    public boolean isFair() {
        return sync instanceof FairSync;
    }

    // 查询是否有线程在等待获取。 请注意，因为取消可能随时发生，{@code true} 返回并不能保证任何其他线程将永远获得。 该方法主要用于监控系统状态。
    /**
     * Queries whether any threads are waiting to acquire. Note that
     * because cancellations may occur at any time, a {@code true}
     * return does not guarantee that any other thread will ever
     * acquire.  This method is designed primarily for use in
     * monitoring of the system state.
     *
     * @return {@code true} if there may be other threads waiting to
     *         acquire the lock
     */
    // 查询是否有线程在等待获取
    public final boolean hasQueuedThreads() {
        return sync.hasQueuedThreads();
    }

    // 返回等待获取的线程数的估计值。 该值只是一个估计值，因为当此方法遍历内部数据结构时，线程数可能会动态变化。 此方法设计用于监视系统状态，而不是用于同步控制。
    /**
     * Returns an estimate of the number of threads waiting to acquire.
     * The value is only an estimate because the number of threads may
     * change dynamically while this method traverses internal data
     * structures.  This method is designed for use in monitoring of the
     * system state, not for synchronization control.
     *
     * @return the estimated number of threads waiting for this lock
     */
    // 返回等待获取的线程数的估计值
    public final int getQueueLength() {
        return sync.getQueueLength();
    }

    // 返回一个包含可能正在等待获取的线程的集合。 由于在构造此结果时实际的线程集可能会动态更改，因此返回的集合只是尽力而为的估计。
    // 返回集合的元素没有特定的顺序。 此方法旨在促进子类的构建，以提供更广泛的监视设施。
    /**
     * Returns a collection containing threads that may be waiting to acquire.
     * Because the actual set of threads may change dynamically while
     * constructing this result, the returned collection is only a best-effort
     * estimate.  The elements of the returned collection are in no particular
     * order.  This method is designed to facilitate construction of
     * subclasses that provide more extensive monitoring facilities.
     *
     * @return the collection of threads
     */
    // 返回一个包含可能正在等待获取的线程的集合
    protected Collection<Thread> getQueuedThreads() {
        return sync.getQueuedThreads();
    }

    // 返回标识此信号量的字符串及其状态。 括号中的状态包括字符串 {@code "Permits ="} 后跟许可数量。
    /**
     * Returns a string identifying this semaphore, as well as its state.
     * The state, in brackets, includes the String {@code "Permits ="}
     * followed by the number of permits.
     *
     * @return a string identifying this semaphore, as well as its state
     */
    public String toString() {
        return super.toString() + "[Permits = " + sync.getPermits() + "]";
    }
}
