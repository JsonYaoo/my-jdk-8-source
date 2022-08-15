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
 * 20210810
 * A. {@link ReadWriteLock} 的实现支持与 {@link ReentrantLock} 类似的语义。
 * B. 该类具有以下属性：
 *      a. 获得顺序: 此类不会对锁定访问强加读取器或写入器首选项排序。 但是，它确实支持可选的公平策略。
 *      b. 非公平模式（默认）: 当构造为非公平（默认）时，读写锁的进入顺序是未指定的，受重入约束。
 *         持续竞争的非公平锁可能会无限期推迟一个或多个读取器或写入器线程，但通常比公平锁具有更高的吞吐量。
 *      c. 公平模式:
 *          1) 当构造为公平时，线程使用近似到达顺序策略竞争进入。当当前持有的锁被释放时，等待时间最长的单个写入器线程将被分配写锁，
 *             或者如果有一组读取器线程等待的时间比所有等待写入器线程都长，则该组将被分配读取锁。
 *          2) 如果写锁被持有，或者有一个等待的写线程，一个试图获取公平读锁（非可重入）的线程将被阻塞。 直到当前等待的最老的写入器线程获得并释放写锁之后，
 *             该线程才会获得读锁。 当然，如果一个等待的写入者放弃等待，留下一个或多个读取器线程作为队列中最长的等待者，并且写入锁空闲，那么这些读取器将被分配读取锁。
 *          3) 除非读锁和写锁都是空闲的（这意味着没有等待线程），否则尝试获取公平写锁（非可重入）的线程将阻塞。
 *            （请注意，非阻塞 {@link ReadLock#tryLock()} 和 {@link WriteLock#tryLock()} 方法不遵守此公平设置，如果可能，将立即获取锁，而不管等待线程。）
 *      d. 重入性:
 *          1) 此锁允许读取器和写入器以 {@link ReentrantLock} 的样式重新获取读或写锁。 在写线程持有的所有写锁都被释放之前，不允许非可重入读者。
 *          2) 此外，作者可以获取读锁，但反之则不行。 在其他应用程序中，当在调用或回调在读锁下执行读取的方法期间持有写锁时，可重入可能很有用。
 *             如果读者试图获取写锁，它将永远不会成功。
 *      e. 锁降级: 重入还允许从写锁降级到读锁，通过获取写锁，然后是读锁，然后释放写锁。 但是，从读锁升级到写锁是不可能的。
 *      f. 锁获取中断: 读锁和写锁都支持在锁获取期间中断。
 *      g. {@link Condition} 支持:
 *          1) 写锁提供了一个 {@link Condition} 实现，它的行为方式与写锁相同，就像 {@link ReentrantLock#newCondition} 为 {@link ReentrantLock} 提供的
 *             {@link Condition} 实现一样。 当然，这个 {@link Condition} 只能与写锁一起使用。
 *          2) 读锁不支持 {@link Condition} 并且 {@code readLock().newCondition()} 抛出 {@code UnsupportedOperationException}。
 *      h. 仪表: 此类支持确定是持有锁还是争用锁的方法。 这些方法设计用于监视系统状态，而不是用于同步控制。
 *    此类的序列化与内置锁的行为方式相同：反序列化的锁处于解锁状态，无论其序列化时的状态如何。
 * C. 示例用法。这是一个代码草图，显示了如何在更新缓存后执行锁降级（在以非嵌套方式处理多个锁时，异常处理特别棘手）：
 * {@code
 * class CachedData {
 *   Object data;
 *   volatile boolean cacheValid;
 *   final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
 *
 *   void processCachedData() {
 *     rwl.readLock().lock();
 *     if (!cacheValid) {
 *       // Must release read lock before acquiring write lock
 *       rwl.readLock().unlock();
 *       rwl.writeLock().lock();
 *       try {
 *         // Recheck state because another thread might have
 *         // acquired write lock and changed state before we did.
 *         if (!cacheValid) {
 *           data = ...
 *           cacheValid = true;
 *         }
 *         // Downgrade by acquiring read lock before releasing write lock
 *         rwl.readLock().lock();
 *       } finally {
 *         rwl.writeLock().unlock(); // Unlock write, still hold read
 *       }
 *     }
 *
 *     try {
 *       use(data);
 *     } finally {
 *       rwl.readLock().unlock();
 *     }
 *   }
 * }}
 * D. ReentrantReadWriteLocks可用于在某些类型的集合的某些用途中提高并发性。 这通常只有在预期集合很大、由比写入线程更多的读取线程访问并且需要开销超过同步开销的操作时才有意义。
 *    例如，这里有一个使用 TreeMap 的类，它预计会很大并且可以并发访问。
 *  {@code
 * class RWDictionary {
 *   private final Map m = new TreeMap();
 *   private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
 *   private final Lock r = rwl.readLock();
 *   private final Lock w = rwl.writeLock();
 *
 *   public Data get(String key) {
 *     r.lock();
 *     try { return m.get(key); }
 *     finally { r.unlock(); }
 *   }
 *   public String[] allKeys() {
 *     r.lock();
 *     try { return m.keySet().toArray(); }
 *     finally { r.unlock(); }
 *   }
 *   public Data put(String key, Data value) {
 *     w.lock();
 *     try { return m.put(key, value); }
 *     finally { w.unlock(); }
 *   }
 *   public void clear() {
 *     w.lock();
 *     try { m.clear(); }
 *     finally { w.unlock(); }
 *   }
 * }}
 * E. 实施说明: 此锁最多支持 65535 个递归写锁和 65535 个读锁。 尝试超过这些限制会导致 {@link Error} 从锁定方法中抛出。
 */
/**
 * A.
 * An implementation of {@link ReadWriteLock} supporting similar
 * semantics to {@link ReentrantLock}.
 *
 * B.
 * <p>This class has the following properties:
 *
 * <ul>
 * <li><b>Acquisition order</b>
 *
 * <p>This class does not impose a reader or writer preference
 * ordering for lock access.  However, it does support an optional
 * <em>fairness</em> policy.
 *
 * <dl>
 * <dt><b><i>Non-fair mode (default)</i></b>
 * <dd>When constructed as non-fair (the default), the order of entry
 * to the read and write lock is unspecified, subject to reentrancy
 * constraints.  A nonfair lock that is continuously contended may
 * indefinitely postpone one or more reader or writer threads, but
 * will normally have higher throughput than a fair lock.
 *
 * <dt><b><i>Fair mode</i></b>
 * <dd>When constructed as fair, threads contend for entry using an
 * approximately arrival-order policy. When the currently held lock
 * is released, either the longest-waiting single writer thread will
 * be assigned the write lock, or if there is a group of reader threads
 * waiting longer than all waiting writer threads, that group will be
 * assigned the read lock.
 *
 * <p>A thread that tries to acquire a fair read lock (non-reentrantly)
 * will block if either the write lock is held, or there is a waiting
 * writer thread. The thread will not acquire the read lock until
 * after the oldest currently waiting writer thread has acquired and
 * released the write lock. Of course, if a waiting writer abandons
 * its wait, leaving one or more reader threads as the longest waiters
 * in the queue with the write lock free, then those readers will be
 * assigned the read lock.
 *
 * <p>A thread that tries to acquire a fair write lock (non-reentrantly)
 * will block unless both the read lock and write lock are free (which
 * implies there are no waiting threads).  (Note that the non-blocking
 * {@link ReadLock#tryLock()} and {@link WriteLock#tryLock()} methods
 * do not honor this fair setting and will immediately acquire the lock
 * if it is possible, regardless of waiting threads.)
 * <p>
 * </dl>
 *
 * <li><b>Reentrancy</b>
 *
 * <p>This lock allows both readers and writers to reacquire read or
 * write locks in the style of a {@link ReentrantLock}. Non-reentrant
 * readers are not allowed until all write locks held by the writing
 * thread have been released.
 *
 * <p>Additionally, a writer can acquire the read lock, but not
 * vice-versa.  Among other applications, reentrancy can be useful
 * when write locks are held during calls or callbacks to methods that
 * perform reads under read locks.  If a reader tries to acquire the
 * write lock it will never succeed.
 *
 * <li><b>Lock downgrading</b>
 * <p>Reentrancy also allows downgrading from the write lock to a read lock,
 * by acquiring the write lock, then the read lock and then releasing the
 * write lock. However, upgrading from a read lock to the write lock is
 * <b>not</b> possible.
 *
 * <li><b>Interruption of lock acquisition</b>
 * <p>The read lock and write lock both support interruption during lock
 * acquisition.
 *
 * <li><b>{@link Condition} support</b>
 * <p>The write lock provides a {@link Condition} implementation that
 * behaves in the same way, with respect to the write lock, as the
 * {@link Condition} implementation provided by
 * {@link ReentrantLock#newCondition} does for {@link ReentrantLock}.
 * This {@link Condition} can, of course, only be used with the write lock.
 *
 * <p>The read lock does not support a {@link Condition} and
 * {@code readLock().newCondition()} throws
 * {@code UnsupportedOperationException}.
 *
 * <li><b>Instrumentation</b>
 * <p>This class supports methods to determine whether locks
 * are held or contended. These methods are designed for monitoring
 * system state, not for synchronization control.
 * </ul>
 *
 * <p>Serialization of this class behaves in the same way as built-in
 * locks: a deserialized lock is in the unlocked state, regardless of
 * its state when serialized.
 *
 * C.
 * <p><b>Sample usages</b>. Here is a code sketch showing how to perform
 * lock downgrading after updating a cache (exception handling is
 * particularly tricky when handling multiple locks in a non-nested
 * fashion):
 *
 * <pre> {@code
 * class CachedData {
 *   Object data;
 *   volatile boolean cacheValid;
 *   final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
 *
 *   void processCachedData() {
 *     rwl.readLock().lock();
 *     if (!cacheValid) {
 *       // Must release read lock before acquiring write lock
 *       rwl.readLock().unlock();
 *       rwl.writeLock().lock();
 *       try {
 *         // Recheck state because another thread might have
 *         // acquired write lock and changed state before we did.
 *         if (!cacheValid) {
 *           data = ...
 *           cacheValid = true;
 *         }
 *         // Downgrade by acquiring read lock before releasing write lock
 *         rwl.readLock().lock();
 *       } finally {
 *         rwl.writeLock().unlock(); // Unlock write, still hold read
 *       }
 *     }
 *
 *     try {
 *       use(data);
 *     } finally {
 *       rwl.readLock().unlock();
 *     }
 *   }
 * }}</pre>
 *
 * D.
 * ReentrantReadWriteLocks can be used to improve concurrency in some
 * uses of some kinds of Collections. This is typically worthwhile
 * only when the collections are expected to be large, accessed by
 * more reader threads than writer threads, and entail operations with
 * overhead that outweighs synchronization overhead. For example, here
 * is a class using a TreeMap that is expected to be large and
 * concurrently accessed.
 *
 *  <pre> {@code
 * class RWDictionary {
 *   private final Map<String, Data> m = new TreeMap<String, Data>();
 *   private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
 *   private final Lock r = rwl.readLock();
 *   private final Lock w = rwl.writeLock();
 *
 *   public Data get(String key) {
 *     r.lock();
 *     try { return m.get(key); }
 *     finally { r.unlock(); }
 *   }
 *   public String[] allKeys() {
 *     r.lock();
 *     try { return m.keySet().toArray(); }
 *     finally { r.unlock(); }
 *   }
 *   public Data put(String key, Data value) {
 *     w.lock();
 *     try { return m.put(key, value); }
 *     finally { w.unlock(); }
 *   }
 *   public void clear() {
 *     w.lock();
 *     try { m.clear(); }
 *     finally { w.unlock(); }
 *   }
 * }}</pre>
 *
 * E.
 * <h3>Implementation Notes</h3>
 *
 * <p>This lock supports a maximum of 65535 recursive write locks
 * and 65535 read locks. Attempts to exceed these limits result in
 * {@link Error} throws from locking methods.
 *
 * @since 1.5
 * @author Doug Lea
 */
public class ReentrantReadWriteLock implements ReadWriteLock, java.io.Serializable {

    private static final long serialVersionUID = -6992448646407690164L;

    // 提供读锁的内部类
    /** Inner class providing readlock */
    // 读锁
    private final ReentrantReadWriteLock.ReadLock readerLock;

    // 提供写锁的内部类
    /** Inner class providing writelock */
    // 写锁
    private final ReentrantReadWriteLock.WriteLock writerLock;

    // 执行所有同步机制
    /** Performs all synchronization mechanics */
    // 同步执行对象
    final Sync sync;

    // 创建一个新的 {@code ReentrantReadWriteLock}默认（非公平）排序属性。
    /**
     * Creates a new {@code ReentrantReadWriteLock} with
     * default (nonfair) ordering properties.
     */
    // 创建ReentrantReadWriteLock实例, 同时实例化读锁和写锁对象, 默认为非公平的同步执行对象
    public ReentrantReadWriteLock() {
        this(false);
    }

    // 创建一个新的{@code ReentrantReadWriteLock}给定的公平政策。
    /**
     * Creates a new {@code ReentrantReadWriteLock} with
     * the given fairness policy.
     *
     * @param fair {@code true} if this lock should use a fair ordering policy
     */
    // 创建ReentrantReadWriteLock实例, 同时实例化读锁和写锁对象,  指定为true时为公平的同步执行对象, 指定为false时为公平的同步执行对象
    public ReentrantReadWriteLock(boolean fair) {
        sync = fair ? new FairSync() : new NonfairSync();
        readerLock = new ReadLock(this);
        writerLock = new WriteLock(this);
    }

    public ReentrantReadWriteLock.WriteLock writeLock() { return writerLock; }
    public ReentrantReadWriteLock.ReadLock  readLock()  { return readerLock; }

    // ReentrantReadWriteLock 的同步实现。分为公平和非公平版本。
    /**
     * Synchronization implementation for ReentrantReadWriteLock.
     * Subclassed into fair and nonfair versions.
     */
    // ReentrantReadWriteLock的同步控制基类
    abstract static class Sync extends AbstractQueuedSynchronizer {

        private static final long serialVersionUID = 6317671515068378041L;

        // 读取与写入计数提取常量和函数。 锁状态在逻辑上分为两个无符号shorts：较低的代表独占（写入者）锁持有计数，较高的代表共享（读取者）持有计数。
        /*
         * Read vs write count extraction constants and functions.
         * Lock state is logically divided into two unsigned shorts:
         * The lower one representing the exclusive (writer) lock hold count,
         * and the upper the shared (reader) hold count.
         */

        // 共享位偏移16位
        static final int SHARED_SHIFT   = 16;

        // 共享位单位65536, 1单位的共享计数(高位, 读锁)
        static final int SHARED_UNIT    = (1 << SHARED_SHIFT);

        // 最大计数65535
        static final int MAX_COUNT      = (1 << SHARED_SHIFT) - 1;

        // 独占掩码65535
        static final int EXCLUSIVE_MASK = (1 << SHARED_SHIFT) - 1;

        // 获取以计数表示的共享保留数: 取int高16位作为共享保留数(读锁)
        /** Returns the number of shared holds represented in count  */
        static int sharedCount(int c)    { return c >>> SHARED_SHIFT; }

        // 获取以计数表示的独占保留数: 取int低16位作为独占保留数(写锁)
        /** Returns the number of exclusive holds represented in count  */
        static int exclusiveCount(int c) { return c & EXCLUSIVE_MASK; }

        // 每个线程读取保持计数的计数器。作为ThreadLocal维护；缓存在cachedHoldCounter中
        /**
         * A counter for per-thread read hold counts.
         * Maintained as a ThreadLocal; cached in cachedHoldCounter
         */
        static final class HoldCounter {
            int count = 0;

            // 使用 id 而不是引用，以避免垃圾保留
            // Use id, not reference, to avoid garbage retention
            // 返回给定线程的线程ID
            final long tid = getThreadId(Thread.currentThread());
        }

        // ThreadLocal子类。为了反序列化机制，最容易明确定义。
        /**
         * ThreadLocal subclass. Easiest to explicitly define for sake
         * of deserialization mechanics.
         */
        static final class ThreadLocalHoldCounter extends ThreadLocal<HoldCounter> {
            public HoldCounter initialValue() {
                return new HoldCounter();
            }
        }

        // 当前线程持有的可重入读锁的数量。 仅在构造函数和 readObject 中初始化。 每当线程的读取保持计数降至 0 时删除。
        /**
         * The number of reentrant read locks held by current thread.
         * Initialized only in constructor and readObject.
         * Removed whenever a thread's read hold count drops to 0.
         */
        // 当前线程持有的可重入读锁的数量, 每当线程的读取保持计数降至0时删除
        private transient ThreadLocalHoldCounter readHolds;

        /**
         * 20210810
         * A. 成功获取readLock的最后一个线程的保持计数。 在下一个要释放的线程是最后一个要获取的线程的常见情况下，这可以节省ThreadLocal查找。 这是非volatile的，
         *    因为它仅用作启发式方法，并且非常适合线程缓存。
         * B. 可以比它缓存读取保持计数的线程寿命更长，但通过不保留对线程的引用来避免垃圾保留。
         * C. 通过良性数据竞争访问； 依赖于内存模型的最终字段和凭空保证。
         */
        /**
         * A.
         * The hold count of the last thread to successfully acquire
         * readLock. This saves ThreadLocal lookup in the common case
         * where the next thread to release is the last one to
         * acquire. This is non-volatile since it is just used
         * as a heuristic, and would be great for threads to cache.
         *
         * B.
         * <p>Can outlive the Thread for which it is caching the read
         * hold count, but avoids garbage retention by not retaining a
         * reference to the Thread.
         *
         * C.
         * <p>Accessed via a benign data race; relies on the memory
         * model's final field and out-of-thin-air guarantees.
         */
        // 成功获取readLock的最后一个线程读取保持计数的计数器, 可以节省ThreadLocal查找, 非常适合线程缓存
        private transient HoldCounter cachedHoldCounter;

        /**
         * 20210810
         * A. firstReader 是第一个获得读锁的线程。 firstReaderHoldCount 是 firstReader 的保留计数。
         * B. 更准确地说，firstReader 是最后一次将共享计数从 0 更改为 1 的唯一线程，此后一直没有释放读锁； 如果没有这样的线程，则为 null。
         * C. 除非线程在不放弃其读锁的情况下终止，否则不会导致垃圾保留，因为 tryReleaseShared 将其设置为 null。
         * D. 通过良性数据竞争访问； 依赖于内存模型对引用的无中生有的保证。
         * E. 这允许跟踪无竞争读锁的读保持非常便宜。
         */
        /**
         * A.
         * firstReader is the first thread to have acquired the read lock.
         * firstReaderHoldCount is firstReader's hold count.
         *
         * B.
         * <p>More precisely, firstReader is the unique thread that last
         * changed the shared count from 0 to 1, and has not released the
         * read lock since then; null if there is no such thread.
         *
         * C.
         * <p>Cannot cause garbage retention unless the thread terminated
         * without relinquishing its read locks, since tryReleaseShared
         * sets it to null.
         *
         * D.
         * <p>Accessed via a benign data race; relies on the memory
         * model's out-of-thin-air guarantees for references.
         *
         * E.
         * <p>This allows tracking of read holds for uncontended read
         * locks to be very cheap.
         */
        // 第一个获得读锁的线程, 它是最后一次将共享计数从0更改为1的唯一线程, 此后再没有释放读锁
        private transient Thread firstReader = null;

        // firstReader的保留计数, 允许跟踪无竞争读锁的读保持非常便宜
        private transient int firstReaderHoldCount;

        // 构造同步锁
        Sync() {
            readHolds = new ThreadLocalHoldCounter();
            setState(getState()); // ensures visibility of readHolds 确保 readHolds 的可见性
        }

        // 获取和释放对公平和非公平锁使用相同的代码，但在队列非空时它们是否/如何允许插入不同。
        /*
         * Acquires and releases use the same code for fair and
         * nonfair locks, but differ in whether/how they allow barging
         * when queues are non-empty.
         */

        // 如果当前线程在尝试获取读锁时以及其他有资格这样做时，应该由于超越其他等待线程的策略而阻塞，则返回 true。
        /**
         * Returns true if the current thread, when trying to acquire
         * the read lock, and otherwise eligible to do so, should block
         * because of policy for overtaking other waiting threads.
         */
        // 如果获取读锁应该被阻塞, 则返回true
        abstract boolean readerShouldBlock();

        // 如果当前线程在尝试获取写锁时以及其他有资格这样做时，应该由于超越其他等待线程的策略而阻塞，则返回 true。
        /**
         * Returns true if the current thread, when trying to acquire
         * the write lock, and otherwise eligible to do so, should block
         * because of policy for overtaking other waiting threads.
         */
        // 如果获取写锁应该被阻塞, 则返回true
        abstract boolean writerShouldBlock();

        // 注意 tryRelease 和 tryAcquire 可以被条件调用。 因此，它们的参数可能包含在条件等待期间全部释放并在 tryAcquire 中重新建立的读取和写入保持。
        /*
         * Note that tryRelease and tryAcquire can be called by
         * Conditions. So it is possible that their arguments contain
         * both read and write holds that are all released during a
         * condition wait and re-established in tryAcquire.
         */

        // 尝试释放独占模式的同步器状态, 返回是否存在独占状态
        protected final boolean tryRelease(int releases) {
            // 判断当前线程是否为独占线程, 如果不是则抛出异常
            if (!isHeldExclusively())
                throw new IllegalMonitorStateException();

            // 扣减同步器状态(锁持有次数)
            int nextc = getState() - releases;

            // 获取以计数表示的独占保留数: 取int低16位作为独占保留数(写锁)
            boolean free = exclusiveCount(nextc) == 0;

            // 如果独占状态为0, 则清空独占线程
            if (free)
                setExclusiveOwnerThread(null);

            // 更新同步器状态(锁持有次数)
            setState(nextc);

            // 返回是否存在独占状态
            return free;
        }

        // 尝试以独占模式获取同步器状态
        protected final boolean tryAcquire(int acquires) {

            // 演练：
            //      1. 如果读取计数非零或写入计数非零且所有者是不同的线程，则失败。
            //      2. 如果计数会饱和，则失败。（这只会在 count 已经非零时发生。）
            //      3. 否则，如果该线程是可重入获取或队列策略允许，则该线程有资格获得锁定。如果是，请更新状态并设置所有者。
            /*
             * Walkthrough:
             * 1. If read count nonzero or write count nonzero
             *    and owner is a different thread, fail.
             * 2. If count would saturate, fail. (This can only
             *    happen if count is already nonzero.)
             * 3. Otherwise, this thread is eligible for lock if
             *    it is either a reentrant acquire or
             *    queue policy allows it. If so, update state
             *    and set owner.
             */
            Thread current = Thread.currentThread();
            int c = getState();

            // 获取以计数表示的独占保留数w: 取int低16位作为独占保留数(写锁)
            int w = exclusiveCount(c);
            if (c != 0) {
                // （注意：如果 c != 0 和 w == 0 则共享计数 != 0）
                // (Note: if c != 0 and w == 0 then shared count != 0)
                // 如果存在共享计数, 或者独享计数不为空且当前线程不为独占线程, 则返回false, 代表独占锁获取失败
                if (w == 0 || current != getExclusiveOwnerThread())
                    return false;

                // 如果独享计数大于最大值65536, 则抛出错误
                if (w + exclusiveCount(acquires) > MAX_COUNT)
                    throw new Error("Maximum lock count exceeded");

                // 更新同步器状态(锁持有次数), 代表可重入
                // Reentrant acquire
                setState(c + acquires);

                // 返回true, 代表独占锁获取成功
                return true;
            }

            // c为0, 如果写锁应该阻塞 或者 CAS更新锁持有次数失败, 则返回false, 代表独占锁获取失败
            if (writerShouldBlock() || !compareAndSetState(c, c + acquires))
                return false;

            // CAS更新锁持有次数成功, 则设置当前线程为独占线程, 然后返回true代表独占锁获取成功
            setExclusiveOwnerThread(current);
            return true;
        }

        // 尝试释放共享模式的同步器状态, 读锁计数完全释放成功, 返回true, 否则返回false
        protected final boolean tryReleaseShared(int unused) {
            Thread current = Thread.currentThread();

            // 如果命中一级缓存, 则直接减少一级缓存的计数值
            if (firstReader == current) {
                // assert firstReaderHoldCount > 0;
                if (firstReaderHoldCount == 1)
                    firstReader = null;
                else
                    firstReaderHoldCount--;
            }
            // 如果当前线程不为第一个读线程, 从缓存中获取当前线程的共享计数, 再减1
            else {
                // 先根据线程id从缓存中获取共享计数
                HoldCounter rh = cachedHoldCounter;
                if (rh == null || rh.tid != getThreadId(current))
                    rh = readHolds.get();
                int count = rh.count;

                // 如果共享计数小于1, 说明共享计数已失效, 则移除缓存, 如果为负数则抛出异常
                if (count <= 1) {
                    readHolds.remove();
                    if (count <= 0)
                        throw unmatchedUnlockException();
                }
                --rh.count;
            }

            // 开始自旋
            for (;;) {
                int c = getState();

                // CAS更新锁持有次数 - 1单位的共享计数(高位, 读锁)
                int nextc = c - SHARED_UNIT;
                if (compareAndSetState(c, nextc))
                    // 释放读锁对读者没有影响，但如果读锁和写锁现在都空闲，它可能允许等待的写者继续。
                    // Releasing the read lock has no effect on readers,
                    // but it may allow waiting writers to proceed if
                    // both read and write locks are now free.
                    // 如果减到0, 则说明读锁释放成功, 返回true, 否则返回false
                    return nextc == 0;
            }
        }

        private IllegalMonitorStateException unmatchedUnlockException() {
            return new IllegalMonitorStateException(
                "attempt to unlock read lock, not locked by current thread");
        }

        // 尝试以共享模式获取同步器状态, 失败(<0), 获取成功但后续共享不成功(=0), 获取成功且后续共享成功(>0)
        protected final int tryAcquireShared(int unused) {

            /**
             * 20210810
             * 演练：
             *      1. 如果另一个线程持有写锁，则失败。
             *      2. 否则，该线程有资格获得锁写入状态，因此询问它是否应该因为队列策略而阻塞。 如果没有，请尝试通过 CASing 状态和更新计数来授予。
             *         请注意，步骤不检查可重入获取，它被推迟到完整版本以避免在更典型的非可重入情况下检查保持计数。
             *      3. 如果第 2 步由于线程显然不符合条件或 CAS 失败或计数饱和而失败，则链接到具有完整重试循环的版本。
             */
            /*
             * Walkthrough:
             * 1. If write lock held by another thread, fail.
             * 2. Otherwise, this thread is eligible for
             *    lock wrt state, so ask if it should block
             *    because of queue policy. If not, try
             *    to grant by CASing state and updating count.
             *    Note that step does not check for reentrant
             *    acquires, which is postponed to full version
             *    to avoid having to check hold count in
             *    the more typical non-reentrant case.
             * 3. If step 2 fails either because thread
             *    apparently not eligible or CAS fails or count
             *    saturated, chain to version with full retry loop.
             */
            Thread current = Thread.currentThread();
            int c = getState();

            // 如果写锁被持有, 且独占线程不为当前线程, 则返回-1, 代表读锁获取失败
            if (exclusiveCount(c) != 0 && getExclusiveOwnerThread() != current)
                return -1;

            // 否则说明发生锁降级或者没有写锁线程, 则获取共享计数r
            int r = sharedCount(c);

            // 如果读锁不应该被阻塞, 且共享计数还没达到最大值, 则CAS更新锁持有次数+1单位
            if (!readerShouldBlock() && r < MAX_COUNT && compareAndSetState(c, c + SHARED_UNIT)) {
                // 如果共享计数r为0, 则设置第一个获取读锁线程以及读锁持有数量
                if (r == 0) {
                    firstReader = current;
                    firstReaderHoldCount = 1;
                }
                // 如果当前线程为第一个读锁线程, 则累加锁持有数量
                else if (firstReader == current) {
                    firstReaderHoldCount++;
                }
                // 如果当前线程不为第一个读取锁线程, 则操作缓存
                else {
                    HoldCounter rh = cachedHoldCounter;

                    // 如果线程id持有者rh为空, 或者持有者id不为当前线程id, 则从获取中获取
                    if (rh == null || rh.tid != getThreadId(current))
                        cachedHoldCounter = rh = readHolds.get();
                    // 如果存在rh, 但rh技术为0, 则更新计数以及缓存
                    else if (rh.count == 0)
                        readHolds.set(rh);
                    rh.count++;
                }

                // 最后返回1, 代表读锁获取成功
                return 1;
            }

            // 如果读锁应该被阻塞, 或者共享计数达到了最大值, 或者CAS更新锁持有次数+1单位失败, 则完整版获取读取, 自旋更新缓存计数
            return fullTryAcquireShared(current);
        }

        // 完整版获取读取，处理CAS未命中和可重入读取未在tryAcquireShared中处理。
        /**
         * Full version of acquire for reads, that handles CAS misses
         * and reentrant reads not dealt with in tryAcquireShared.
         */
        // 如果读锁应该被阻塞, 或者共享计数达到了最大值, 或者CAS更新锁持有次数+1单位失败, 则完整版获取读取, 自旋更新缓存计数
        final int fullTryAcquireShared(Thread current) {
            // 此代码与tryAcquireShared中的代码部分冗余，但总体上更简单，因为它不会使 tryAcquireShared 与重试和延迟读取保持计数之间的交互复杂化。
            /*
             * This code is in part redundant with that in
             * tryAcquireShared but is simpler overall by not
             * complicating tryAcquireShared with interactions between
             * retries and lazily reading hold counts.
             */
            HoldCounter rh = null;

            // 开始自旋
            for (;;) {
                int c = getState();

                // 如果存在写锁, 且当前线程不为独占线程, 则返回-1, 代表读锁获取失败
                if (exclusiveCount(c) != 0) {
                    if (getExclusiveOwnerThread() != current)
                        return -1;

                    // 否则我们持有排他锁； 在这里阻塞会导致死锁。
                    // else we hold the exclusive lock; blocking here
                    // would cause deadlock.
                }
                // 如果读锁应该阻塞, 判断当前线程是否为第一个获取读锁的线程, 如果是则什么也不做, 如果不是则更新缓存, 此时如果缓存计数为0, 则返回-1, 代表读锁获取失败
                else if (readerShouldBlock()) {
                    // 确保我们没有以可重入的方式获取读锁
                    // Make sure we're not acquiring read lock reentrantly
                    // 如果当前线程为第一个获取读锁线程, 则什么也不做
                    if (firstReader == current) {
                        // assert firstReaderHoldCount > 0;
                    }
                    // 如果当前线程不为第一个获取读锁线程, 则操作缓存
                    else {
                        if (rh == null) {
                            rh = cachedHoldCounter;
                            if (rh == null || rh.tid != getThreadId(current)) {
                                rh = readHolds.get();
                                if (rh.count == 0)
                                    readHolds.remove();
                            }
                        }

                        // 如果本地线程缓存计数值为0, 说明当前线程读锁重入次数为0, 也就是释放了读锁, 则返回-1, 代表读锁获取失败
                        if (rh.count == 0)
                            return -1;
                    }
                }

                // 如果共享计数等于最大值, 则抛出异常
                if (sharedCount(c) == MAX_COUNT)
                    throw new Error("Maximum lock count exceeded");

                // 如果判断重入性, 如果满足则CAS更新+1共享计数单位, 如果CAS失败则继续自旋
                if (compareAndSetState(c, c + SHARED_UNIT)) {
                    // 如果CAS成功, 再更新缓存计数
                    if (sharedCount(c) == 0) {
                        firstReader = current;
                        firstReaderHoldCount = 1;
                    } else if (firstReader == current) {
                        firstReaderHoldCount++;
                    } else {
                        if (rh == null)
                            rh = cachedHoldCounter;
                        if (rh == null || rh.tid != getThreadId(current))
                            rh = readHolds.get();
                        else if (rh.count == 0)
                            readHolds.set(rh);
                        rh.count++;
                        cachedHoldCounter = rh; // cache for release
                    }
                    return 1;
                }
            }
        }

        // 执行 tryLock 以进行写入，从而在两种模式下启用插入。 这与 tryAcquire 的效果相同，只是缺少对 writerShouldBlock 的调用。
        /**
         * Performs tryLock for write, enabling barging in both modes.
         * This is identical in effect to tryAcquire except for lack
         * of calls to writerShouldBlock.
         */
        // 执行 tryLock 以进行写, 与 tryAcquire 的效果相同，只是缺少对 writerShouldBlock 的调用
        final boolean tryWriteLock() {
            Thread current = Thread.currentThread();
            int c = getState();

            // 如果存在锁计数
            if (c != 0) {
                // 如果不存在独占, 或者独占线程不为当前线程, 则返回false, 代表写锁获取失败
                int w = exclusiveCount(c);
                if (w == 0 || current != getExclusiveOwnerThread())
                    return false;
                if (w == MAX_COUNT)
                    throw new Error("Maximum lock count exceeded");
            }

            // 如果不存在共享计数, 则CAS更新1单位的写锁, 如果CAS失败, 则返回false, 代表写锁获取失败
            if (!compareAndSetState(c, c + 1))
                return false;

            // CAS成功, 则设置当前线程为独享线程, 并返回true, 代表写锁获取成功
            setExclusiveOwnerThread(current);
            return true;
        }

        // 为读取执行tryLock，在两种模式下启用插入。这与 tryAcquireShared 的效果相同，除了缺少对 readerShouldBlock 的调用。
        /**
         * Performs tryLock for read, enabling barging in both modes.
         * This is identical in effect to tryAcquireShared except for
         * lack of calls to readerShouldBlock.
         */
        // 为读取执行tryLock, 这与 tryAcquireShared 的效果相同，除了缺少对 readerShouldBlock 的调用
        final boolean tryReadLock() {
            Thread current = Thread.currentThread();

            // 开始自旋
            for (;;) {
                int c = getState();

                // 如果存在写锁, 且独占线程不为当前线程, 则返回false, 代表读锁获取失败
                if (exclusiveCount(c) != 0 && getExclusiveOwnerThread() != current)
                    return false;

                // 获取共享计数r, 如果达到最大了, 则抛出异常
                int r = sharedCount(c);
                if (r == MAX_COUNT)
                    throw new Error("Maximum lock count exceeded");

                // CAS更新1单位共享计数, 如果CAS成功, 则更新缓存计数, 最后返回true, 代表读锁获取成功
                if (compareAndSetState(c, c + SHARED_UNIT)) {
                    if (r == 0) {
                        firstReader = current;
                        firstReaderHoldCount = 1;
                    } else if (firstReader == current) {
                        firstReaderHoldCount++;
                    } else {
                        HoldCounter rh = cachedHoldCounter;
                        if (rh == null || rh.tid != getThreadId(current))
                            cachedHoldCounter = rh = readHolds.get();
                        else if (rh.count == 0)
                            readHolds.set(rh);
                        rh.count++;
                    }
                    return true;
                }
                // 如果CAS失败, 则继续自旋
            }
        }

        // 判断当前线程是否为独占线程
        protected final boolean isHeldExclusively() {
            // 虽然我们通常必须在所有者之前读取状态，但我们不需要这样做来检查当前线程是否是所有者
            // While we must in general read state before owner,
            // we don't need to do so to check if current thread is owner
            return getExclusiveOwnerThread() == Thread.currentThread();
        }

        // 与外部类相关的方法
        // Methods relayed to outer class

        // 构建读写锁的ConditionObject实例
        final ConditionObject newCondition() {
            return new ConditionObject();
        }

        // 获取独占的线程实例
        final Thread getOwner() {
            // 必须在拥有者之前读取状态以确保内存一致性
            // Must read state before owner to ensure memory consistency
            return ((exclusiveCount(getState()) == 0) ? null : getExclusiveOwnerThread());
        }

        // 获取共享计数
        final int getReadLockCount() {
            return sharedCount(getState());
        }

        // 是否存在写锁
        final boolean isWriteLocked() {
            return exclusiveCount(getState()) != 0;
        }

        // 获取独占计数
        final int getWriteHoldCount() {
            return isHeldExclusively() ? exclusiveCount(getState()) : 0;
        }

        // 查询当前线程对该锁持有的可重入读次数
        final int getReadHoldCount() {
            if (getReadLockCount() == 0)
                return 0;

            // 从一级缓存中获取
            Thread current = Thread.currentThread();
            if (firstReader == current)
                return firstReaderHoldCount;

            // 从二级缓存中获取
            HoldCounter rh = cachedHoldCounter;
            if (rh != null && rh.tid == getThreadId(current))
                return rh.count;

            // 从ThreadLocal本体中获取
            int count = readHolds.get().count;
            if (count == 0) readHolds.remove();
            return count;
        }

        // 从流中重构实例（即反序列化它）。
        /**
         * Reconstitutes the instance from a stream (that is, deserializes it).
         */
        // 从流中重构实例（即反序列化它）
        private void readObject(java.io.ObjectInputStream s)
            throws java.io.IOException, ClassNotFoundException {
            s.defaultReadObject();
            readHolds = new ThreadLocalHoldCounter();
            setState(0); // reset to unlocked state
        }

        // 获取锁持有次数
        final int getCount() { return getState(); }
    }

    // 同步的非公平版本
    /**
     * Nonfair version of Sync
     */
    // 非公平锁同步控制实现类
    static final class NonfairSync extends Sync {

        private static final long serialVersionUID = -8159625535654395037L;

        // 非公平下判断写锁是否应该阻塞, 非公平下写锁总是可以互斥的, 所以返回false
        final boolean writerShouldBlock() {
            return false; // writers can always barge
        }

        // 非公平下判断读锁是否应该阻塞, 如果队头存在独占线程, 则返回false
        final boolean readerShouldBlock() {
            // 作为一种避免无限写入器饥饿的启发式方法，如果暂时出现在队列头（如果存在）的线程是等待写入器，则阻塞。
            // 这只是一种概率效应，因为如果在其他尚未从队列中排出的已启用读取器后面有等待写入器，则新读取器不会阻塞。
            /*
             * As a heuristic to avoid indefinite writer starvation,
             * block if the thread that momentarily appears to be head
             * of queue, if one exists, is a waiting writer.  This is
             * only a probabilistic effect since a new reader will not
             * block if there is a waiting writer behind other enabled
             * readers that have not yet drained from the queue.
             */
            // 判断第一个排队线程是否以独占模式等待
            return apparentlyFirstQueuedIsExclusive();
        }
    }

    // 同步的公平版本
    /**
     * Fair version of Sync
     */
    // 公平锁同步控制实现类
    static final class FairSync extends Sync {

        private static final long serialVersionUID = -2274990926593161451L;

        // 公平下判断写锁是否应该阻塞, 看前面是否有排队线程
        final boolean writerShouldBlock() {
            // 判断此刻CLH中是否存在有等待时间比当前线程长的线程, 旨在由公平同步器使用, 以避免插入结点到CLH队列中
            return hasQueuedPredecessors();
        }

        // 非公平下判断读锁是否应该阻塞, 看前面是否有排队线程
        final boolean readerShouldBlock() {
            // 判断此刻CLH中是否存在有等待时间比当前线程长的线程, 旨在由公平同步器使用, 以避免插入结点到CLH队列中
            return hasQueuedPredecessors();
        }
    }

    // 方法 {@link ReentrantReadWriteLock#readLock} 返回的锁。
    /**
     * The lock returned by method {@link ReentrantReadWriteLock#readLock}.
     */
    // 读锁实现类
    public static class ReadLock implements Lock, java.io.Serializable {

        private static final long serialVersionUID = -5992448646407690164L;
        private final Sync sync;

        // 子类使用的构造函数
        /**
         * Constructor for use by subclasses
         *
         * @param lock the outer lock object
         * @throws NullPointerException if the lock is null
         */
        // 构造ReentrantReadWriteLock的读锁实例
        protected ReadLock(ReentrantReadWriteLock lock) {
            sync = lock.sync;
        }

        /**
         * 20210811
         * A. 获取读锁。
         * B. 如果写锁未被另一个线程持有，则获取读锁并立即返回。
         * C. 如果写锁被另一个线程持有，那么当前线程将因线程调度目的而被禁用，并处于休眠状态，直到获得读锁为止。
         */
        /**
         * A.
         * Acquires the read lock.
         *
         * B.
         * <p>Acquires the read lock if the write lock is not held by
         * another thread and returns immediately.
         *
         * C.
         * <p>If the write lock is held by another thread then
         * the current thread becomes disabled for thread scheduling
         * purposes and lies dormant until the read lock has been acquired.
         */
        // 共享方式获取读锁, 如果写锁未被另一个线程持有，则获取读锁并立即返回; 如果写锁被另一个线程持有, 则当前线程会阻塞
        public void lock() {
            sync.acquireShared(1);
        }

        /**
         * 20210811
         * A. 除非当前线程是{@linkplain Thread#interrupt interrupted}，否则获取读锁。
         * B. 如果写锁未被另一个线程持有，则获取读锁并立即返回。
         * C. 如果写锁由另一个线程持有，则当前线程将出于线程调度目的而被禁用并处于休眠状态，直到发生以下两种情况之一：
         *      a. 读锁由当前线程获取；
         *      b. 其他一些线程 {@linkplain Thread#interrupt 中断}当前线程。
         *    如果当前线程：
         *      a. 在进入此方法时设置其中断状态；
         *      b. 获取读锁时{@linkplain Thread#interrupt interrupted}，
         *    然后抛出 {@link InterruptedException} 并清除当前线程的中断状态。
         * D. 在此实现中，由于此方法是显式中断点，因此优先响应中断而不是正常或可重入获取锁。
         */
        /**
         * A.
         * Acquires the read lock unless the current thread is
         * {@linkplain Thread#interrupt interrupted}.
         *
         * B.
         * <p>Acquires the read lock if the write lock is not held
         * by another thread and returns immediately.
         *
         * C.
         * <p>If the write lock is held by another thread then the
         * current thread becomes disabled for thread scheduling
         * purposes and lies dormant until one of two things happens:
         *
         * <ul>
         *
         * <li>The read lock is acquired by the current thread; or
         *
         * <li>Some other thread {@linkplain Thread#interrupt interrupts}
         * the current thread.
         *
         * </ul>
         *
         * <p>If the current thread:
         *
         * <ul>
         *
         * <li>has its interrupted status set on entry to this method; or
         *
         * <li>is {@linkplain Thread#interrupt interrupted} while
         * acquiring the read lock,
         *
         * </ul>
         *
         * then {@link InterruptedException} is thrown and the current
         * thread's interrupted status is cleared.
         *
         * D.
         * <p>In this implementation, as this method is an explicit
         * interruption point, preference is given to responding to
         * the interrupt over normal or reentrant acquisition of the
         * lock.
         *
         * @throws InterruptedException if the current thread is interrupted
         */
        // 可中断共享方式获取读锁, 如果写锁未被另一个线程持有，则获取读锁并立即返回; 如果写锁被另一个线程持有, 则当前线程会阻塞
        public void lockInterruptibly() throws InterruptedException {
            sync.acquireSharedInterruptibly(1);
        }

        /**
         * 20210811
         * A. 仅当调用时另一个线程未持有写锁时才获取读锁。
         * B. 如果写锁未被另一个线程持有，则获取读锁并立即返回值 {@code true}。 即使此锁已设置为使用公平排序策略，调用 {@code tryLock()} 将立即获取可用的读锁，
         *    无论其他线程当前是否正在等待读锁。 这种“闯入”行为在某些情况下很有用，即使它破坏了公平。 如果你想尊重这个锁的公平性设置，
         *    那么使用 {@link #tryLock(long, TimeUnit) tryLock(0, TimeUnit.SECONDS) } 这几乎是等效的（它也检测中断）。
         * C. 如果写锁被另一个线程持有，则此方法将立即返回值 {@code false}。
         */
        /**
         * A.
         * Acquires the read lock only if the write lock is not held by
         * another thread at the time of invocation.
         *
         * B.
         * <p>Acquires the read lock if the write lock is not held by
         * another thread and returns immediately with the value
         * {@code true}. Even when this lock has been set to use a
         * fair ordering policy, a call to {@code tryLock()}
         * <em>will</em> immediately acquire the read lock if it is
         * available, whether or not other threads are currently
         * waiting for the read lock.  This &quot;barging&quot; behavior
         * can be useful in certain circumstances, even though it
         * breaks fairness. If you want to honor the fairness setting
         * for this lock, then use {@link #tryLock(long, TimeUnit)
         * tryLock(0, TimeUnit.SECONDS) } which is almost equivalent
         * (it also detects interruption).
         *
         * C.
         * <p>If the write lock is held by another thread then
         * this method will return immediately with the value
         * {@code false}.
         *
         * @return {@code true} if the read lock was acquired
         */
        // 非公平、共享方式尝试获取锁, 如果写锁未被另一个线程持有, 则获取读锁并立即返回值 {@code true}; 如果写锁被另一个线程持有，则此方法将立即返回值 {@code false}
        public boolean tryLock() {
            return sync.tryReadLock();
        }

        /**
         * 20210811
         * A. 如果写锁在给定的等待时间内没有被另一个线程持有，并且当前线程没有被{@linkplain Thread#interrupt interrupted}，则获取读锁。
         * B. 如果写锁未被另一个线程持有，则获取读锁并立即返回值 {@code true}。 如果此锁已设置为使用公平排序策略，则如果任何其他线程正在等待该锁，则不会获取可用锁。
         *    这与 {@link #tryLock()} 方法形成对比。 如果您想要一个允许插入公平锁的定时 {@code tryLock}，则将定时和非定时形式组合在一起：
         *  {@code
         * if (lock.tryLock() ||
         *     lock.tryLock(timeout, unit)) {
         *   ...
         * }}
         * C. 如果写锁由另一个线程持有，则当前线程将出于线程调度目的而被禁用并处于休眠状态，直到发生以下三种情况之一：
         *      a. 读锁由当前线程获取
         *      b. 其他一些线程 {@linkplain Thread#interrupt 中断}当前线程
         *      c. 指定的等待时间已过
         * D. 如果获取了读锁，则返回值 {@code true}。
         *     如果当前线程：
         *      a. 在进入此方法时设置其中断状态
         *      b. 获取读锁时{@linkplain Thread#interrupt interrupted}
         *    然后抛出 {@link InterruptedException} 并清除当前线程的中断状态。
         * E. 如果指定的等待时间过去，则返回值 {@code false}。 如果时间小于或等于零，则该方法根本不会等待。
         * F. 在此实现中，由于此方法是显式中断点，因此优先响应中断而不是正常或可重入获取锁，并优先报告等待时间的过去。
         */
        /**
         * A.
         * Acquires the read lock if the write lock is not held by
         * another thread within the given waiting time and the
         * current thread has not been {@linkplain Thread#interrupt
         * interrupted}.
         *
         * B.
         * <p>Acquires the read lock if the write lock is not held by
         * another thread and returns immediately with the value
         * {@code true}. If this lock has been set to use a fair
         * ordering policy then an available lock <em>will not</em> be
         * acquired if any other threads are waiting for the
         * lock. This is in contrast to the {@link #tryLock()}
         * method. If you want a timed {@code tryLock} that does
         * permit barging on a fair lock then combine the timed and
         * un-timed forms together:
         *
         *  <pre> {@code
         * if (lock.tryLock() ||
         *     lock.tryLock(timeout, unit)) {
         *   ...
         * }}</pre>
         *
         * C.
         * <p>If the write lock is held by another thread then the
         * current thread becomes disabled for thread scheduling
         * purposes and lies dormant until one of three things happens:
         *
         * <ul>
         *
         * <li>The read lock is acquired by the current thread; or
         *
         * <li>Some other thread {@linkplain Thread#interrupt interrupts}
         * the current thread; or
         *
         * <li>The specified waiting time elapses.
         *
         * </ul>
         *
         * D.
         * <p>If the read lock is acquired then the value {@code true} is
         * returned.
         *
         * <p>If the current thread:
         *
         * <ul>
         *
         * <li>has its interrupted status set on entry to this method; or
         *
         * <li>is {@linkplain Thread#interrupt interrupted} while
         * acquiring the read lock,
         *
         * </ul> then {@link InterruptedException} is thrown and the
         * current thread's interrupted status is cleared.
         *
         * E.
         * <p>If the specified waiting time elapses then the value
         * {@code false} is returned.  If the time is less than or
         * equal to zero, the method will not wait at all.
         *
         * F.
         * <p>In this implementation, as this method is an explicit
         * interruption point, preference is given to responding to
         * the interrupt over normal or reentrant acquisition of the
         * lock, and over reporting the elapse of the waiting time.
         *
         * @param timeout the time to wait for the read lock
         * @param unit the time unit of the timeout argument
         * @return {@code true} if the read lock was acquired
         * @throws InterruptedException if the current thread is interrupted
         * @throws NullPointerException if the time unit is null
         */
        // 非公平、定时、可中断、共享方式尝试获取锁, 如果写锁未被另一个线程持有, 则获取读锁并立即返回值 {@code true}; 如果写锁被另一个线程持有，则此方法将立即返回值 {@code false}
        public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException {
            return sync.tryAcquireSharedNanos(1, unit.toNanos(timeout));
        }

        /**
         * 20210811
         * A. 尝试释放此锁。
         * B. 如果读取器的数量现在为零，则该锁可用于写锁尝试。
         */
        /**
         * A.
         * Attempts to release this lock.
         *
         * B.
         * <p>If the number of readers is now zero then the lock
         * is made available for write lock attempts.
         */
        // 共享方式释放锁, 如果读锁同步器状态现在为0, 则会尝试共享方式释放读锁
        public void unlock() {
            sync.releaseShared(1);
        }

        // 抛出 {@code UnsupportedOperationException}，因为 {@code ReadLocks} 不支持条件。
        /**
         * Throws {@code UnsupportedOperationException} because
         * {@code ReadLocks} do not support conditions.
         *
         * @throws UnsupportedOperationException always
         */
        // 读锁不支持newCondition
        public Condition newCondition() {
            throw new UnsupportedOperationException();
        }

        // 返回标识此锁及其锁状态的字符串。 括号中的状态包括字符串 {@code "Read locks ="} 后跟持有的读锁数。
        /**
         * Returns a string identifying this lock, as well as its lock state.
         * The state, in brackets, includes the String {@code "Read locks ="}
         * followed by the number of held read locks.
         *
         * // 标识此锁及其锁状态的字符串
         * @return a string identifying this lock, as well as its lock state
         */
        public String toString() {
            int r = sync.getReadLockCount();
            return super.toString() +
                "[Read locks = " + r + "]";
        }
    }

    // 方法 {@link ReentrantReadWriteLock#writeLock} 返回的锁。
    /**
     * The lock returned by method {@link ReentrantReadWriteLock#writeLock}.
     */
    // 写锁实现类
    public static class WriteLock implements Lock, java.io.Serializable {

        private static final long serialVersionUID = -4992448646407690164L;
        private final Sync sync;// ReentrantReadWriteLock的同步控制基类

        // 子类使用的构造函数
        /**
         * Constructor for use by subclasses
         *
         * @param lock the outer lock object
         * @throws NullPointerException if the lock is null
         */
        // 构造ReentrantReadWriteLock的写锁实例
        protected WriteLock(ReentrantReadWriteLock lock) {
            sync = lock.sync;
        }

        /**
         * 20210811
         * A. 获取写锁。
         * B. 如果读锁和写锁都没有被另一个线程持有，则获取写锁并立即返回，将写锁持有计数设置为 1。
         * C. 如果当前线程已经持有写锁，则持有计数加一并且该方法立即返回。
         * D. 如果该锁由另一个线程持有，则当前线程将出于线程调度目的而被禁用并处于休眠状态，直到获得写锁为止，此时写锁持有计数设置为 1。
         */
        /**
         * A.
         * Acquires the write lock.
         *
         * B.
         * <p>Acquires the write lock if neither the read nor write lock
         * are held by another thread
         * and returns immediately, setting the write lock hold count to
         * one.
         *
         * C.
         * <p>If the current thread already holds the write lock then the
         * hold count is incremented by one and the method returns
         * immediately.
         *
         * D.
         * <p>If the lock is held by another thread then the current
         * thread becomes disabled for thread scheduling purposes and
         * lies dormant until the write lock has been acquired, at which
         * time the write lock hold count is set to one.
         */
        // 独占方式获取写锁, 如果读锁和写锁都没有被另一个线程持有, 则获取写锁并立即返回, 将写锁持有计数设置为1; 如果当前线程已经持有写锁, 则持有计数加一并且该方法立即返回(重入性); 如果该锁由另一个线程持有，则当前线程将阻塞, 直到获得写锁为止, 然后写锁持有计数设置为1
        public void lock() {
            sync.acquire(1);
        }

        /**
         * 20210811
         * A. 除非当前线程是{@linkplain Thread#interrupt interrupted}，否则获取写锁。
         * B. 如果读锁和写锁都没有被另一个线程持有，则获取写锁并立即返回，将写锁持有计数设置为 1。
         * C. 如果当前线程已经持有这个锁，那么持有计数就会增加一并且该方法立即返回。
         * D. 如果锁被另一个线程持有，那么当前线程将被禁用以进行线程调度并处于休眠状态，直到发生以下两种情况之一：
         *      a. 写锁由当前线程获取；
         *      b. 其他一些线程 {@linkplain Thread#interrupt 中断}当前线程。
         *    如果当前线程获取了写锁，则锁保持计数设置为 1。
         * E. 如果当前线程：
         *      a. 在进入此方法时设置其中断状态；
         *      b. 获取写锁时{@linkplain Thread#interrupt interrupted}
         *    然后抛出 {@link InterruptedException} 并清除当前线程的中断状态。
         * F. 在此实现中，由于此方法是显式中断点，因此优先响应中断而不是正常或可重入获取锁。
         */
        /**
         * A.
         * Acquires the write lock unless the current thread is
         * {@linkplain Thread#interrupt interrupted}.
         *
         * B.
         * <p>Acquires the write lock if neither the read nor write lock
         * are held by another thread
         * and returns immediately, setting the write lock hold count to
         * one.
         *
         * C.
         * <p>If the current thread already holds this lock then the
         * hold count is incremented by one and the method returns
         * immediately.
         *
         * D.
         * <p>If the lock is held by another thread then the current
         * thread becomes disabled for thread scheduling purposes and
         * lies dormant until one of two things happens:
         *
         * <ul>
         *
         * <li>The write lock is acquired by the current thread; or
         *
         * <li>Some other thread {@linkplain Thread#interrupt interrupts}
         * the current thread.
         *
         * </ul>
         *
         * <p>If the write lock is acquired by the current thread then the
         * lock hold count is set to one.
         *
         * E.
         * <p>If the current thread:
         *
         * <ul>
         *
         * <li>has its interrupted status set on entry to this method;
         * or
         *
         * <li>is {@linkplain Thread#interrupt interrupted} while
         * acquiring the write lock,
         *
         * </ul>
         *
         * then {@link InterruptedException} is thrown and the current
         * thread's interrupted status is cleared.
         *
         * F.
         * <p>In this implementation, as this method is an explicit
         * interruption point, preference is given to responding to
         * the interrupt over normal or reentrant acquisition of the
         * lock.
         *
         * @throws InterruptedException if the current thread is interrupted
         */
        // 独占、可中断方式获取写锁, 如果读锁和写锁都没有被另一个线程持有, 则获取写锁并立即返回, 将写锁持有计数设置为1; 如果当前线程已经持有写锁, 则持有计数加一并且该方法立即返回(重入性); 如果该锁由另一个线程持有，则当前线程将阻塞, 直到获得写锁为止, 然后写锁持有计数设置为1
        public void lockInterruptibly() throws InterruptedException {
            sync.acquireInterruptibly(1);
        }

        /**
         * 20210811
         * A. 仅当在调用时未被另一个线程持有时才获取写锁。
         * B. 如果读锁和写锁都没有被另一个线程持有，则获取写锁，并立即返回值 {@code true}，将写锁持有计数设置为 1。 即使此锁已设置为使用公平排序策略，
         *    调用 {@code tryLock()} 也会立即获取该锁（如果可用），无论其他线程当前是否正在等待写锁。 这种“闯入”行为在某些情况下很有用，即使它破坏了公平。
         *    如果你想尊重这个锁的公平性设置，那么使用 {@link #tryLock(long, TimeUnit) tryLock(0, TimeUnit.SECONDS) } 这几乎是等效的（它也检测中断）。
         * C. 如果当前线程已经持有这个锁，那么持有计数增加一并且该方法返回{@code true}。
         * D. 如果锁被另一个线程持有，则此方法将立即返回值 {@code false}。
         */
        /**
         * A.
         * Acquires the write lock only if it is not held by another thread
         * at the time of invocation.
         *
         * B.
         * <p>Acquires the write lock if neither the read nor write lock
         * are held by another thread
         * and returns immediately with the value {@code true},
         * setting the write lock hold count to one. Even when this lock has
         * been set to use a fair ordering policy, a call to
         * {@code tryLock()} <em>will</em> immediately acquire the
         * lock if it is available, whether or not other threads are
         * currently waiting for the write lock.  This &quot;barging&quot;
         * behavior can be useful in certain circumstances, even
         * though it breaks fairness. If you want to honor the
         * fairness setting for this lock, then use {@link
         * #tryLock(long, TimeUnit) tryLock(0, TimeUnit.SECONDS) }
         * which is almost equivalent (it also detects interruption).
         *
         * C.
         * <p>If the current thread already holds this lock then the
         * hold count is incremented by one and the method returns
         * {@code true}.
         *
         * D.
         * <p>If the lock is held by another thread then this method
         * will return immediately with the value {@code false}.
         *
         * // {@code true} 如果锁是空闲的并且被当前线程获取，或者写锁已经被当前线程持有； 和 {@code false} 否则。
         * @return {@code true} if the lock was free and was acquired
         * by the current thread, or the write lock was already held
         * by the current thread; and {@code false} otherwise.
         */
        // 非公平、独占方式获取写锁, 如果读锁和写锁都没有被另一个线程持有, 则获取写锁并立即返回, 将写锁持有计数设置为1; 如果当前线程已经持有写锁, 则持有计数加一并且该方法立即返回(重入性); 如果该锁由另一个线程持有，则当前线程将阻塞, 直到获得写锁为止, 然后写锁持有计数设置为1
        public boolean tryLock( ) {
            return sync.tryWriteLock();
        }

        /**
         * 20210811
         * A. 如果在给定的等待时间内没有被另一个线程持有并且当前线程没有被{@linkplain Thread#interrupt interrupted}，则获取写锁。
         * B. 如果读锁和写锁都没有被另一个线程持有，则获取写锁，并立即返回值 {@code true}，将写锁持有计数设置为 1。 如果此锁已设置为使用公平排序策略，则如果任何其他线程正在等待写锁，则不会获取可用锁。
         *    这与 {@link #tryLock()} 方法形成对比。 如果您想要一个允许插入公平锁的定时 {@code tryLock}，则将定时和非定时形式组合在一起：
         *  {@code
         * if (lock.tryLock() ||
         *     lock.tryLock(timeout, unit)) {
         *   ...
         * }}
         * C. 如果当前线程已经持有这个锁，那么持有计数增加一并且该方法返回{@code true}。
         * D. 如果锁被另一个线程持有，那么当前线程将被禁用以进行线程调度并处于休眠状态，直到发生以下三种情况之一：
         *      a. 写锁由当前线程获取；
         *      b. 其他一些线程 {@linkplain Thread#interrupt 中断}当前线程；
         *      c. 经过指定的等待时间
         *    如果获得了写锁，则返回值 {@code true} 并将写锁保持计数设置为 1。
         * E. 如果当前线程：
         *      a. 在进入此方法时设置其中断状态；
         *      b. 获取写锁时{@linkplain Thread#interrupt interrupted}，
         *    然后抛出 {@link InterruptedException} 并清除当前线程的中断状态。
         * F. 如果指定的等待时间过去，则返回值 {@code false}。 如果时间小于或等于零，则该方法根本不会等待。
         * G. 在此实现中，由于此方法是显式中断点，因此优先响应中断而不是正常或可重入获取锁，并优先报告等待时间的过去。
         */
        /**
         * A.
         * Acquires the write lock if it is not held by another thread
         * within the given waiting time and the current thread has
         * not been {@linkplain Thread#interrupt interrupted}.
         *
         * B.
         * <p>Acquires the write lock if neither the read nor write lock
         * are held by another thread
         * and returns immediately with the value {@code true},
         * setting the write lock hold count to one. If this lock has been
         * set to use a fair ordering policy then an available lock
         * <em>will not</em> be acquired if any other threads are
         * waiting for the write lock. This is in contrast to the {@link
         * #tryLock()} method. If you want a timed {@code tryLock}
         * that does permit barging on a fair lock then combine the
         * timed and un-timed forms together:
         *
         *  <pre> {@code
         * if (lock.tryLock() ||
         *     lock.tryLock(timeout, unit)) {
         *   ...
         * }}</pre>
         *
         * C.
         * <p>If the current thread already holds this lock then the
         * hold count is incremented by one and the method returns
         * {@code true}.
         *
         * D.
         * <p>If the lock is held by another thread then the current
         * thread becomes disabled for thread scheduling purposes and
         * lies dormant until one of three things happens:
         *
         * <ul>
         *
         * <li>The write lock is acquired by the current thread; or
         *
         * <li>Some other thread {@linkplain Thread#interrupt interrupts}
         * the current thread; or
         *
         * <li>The specified waiting time elapses
         *
         * </ul>
         *
         * <p>If the write lock is acquired then the value {@code true} is
         * returned and the write lock hold count is set to one.
         *
         * E.
         * <p>If the current thread:
         *
         * <ul>
         *
         * <li>has its interrupted status set on entry to this method;
         * or
         *
         * <li>is {@linkplain Thread#interrupt interrupted} while
         * acquiring the write lock,
         *
         * </ul>
         *
         * then {@link InterruptedException} is thrown and the current
         * thread's interrupted status is cleared.
         *
         * F.
         * <p>If the specified waiting time elapses then the value
         * {@code false} is returned.  If the time is less than or
         * equal to zero, the method will not wait at all.
         *
         * G.
         * <p>In this implementation, as this method is an explicit
         * interruption point, preference is given to responding to
         * the interrupt over normal or reentrant acquisition of the
         * lock, and over reporting the elapse of the waiting time.
         *
         * // @param timeout 等待写锁的时间
         * // @param unit 超时参数的时间单位
         * @param timeout the time to wait for the write lock
         * @param unit the time unit of the timeout argument
         *
         * // @return {@code true} 如果锁是空闲的并且被当前线程获取，或者写锁已经被线程持有 当前线程； 和 {@code false} 如果在获得锁之前等待时间已经过去。
         * @return {@code true} if the lock was free and was acquired
         * by the current thread, or the write lock was already held by the
         * current thread; and {@code false} if the waiting time
         * elapsed before the lock could be acquired.
         *
         * @throws InterruptedException if the current thread is interrupted
         * @throws NullPointerException if the time unit is null
         */
        // 非公平、定时、可中断、独占方式获取写锁, 如果读锁和写锁都没有被另一个线程持有, 则获取写锁并立即返回, 将写锁持有计数设置为1; 如果当前线程已经持有写锁, 则持有计数加一并且该方法立即返回(重入性); 如果该锁由另一个线程持有，则当前线程将阻塞, 直到获得写锁为止, 然后写锁持有计数设置为1
        public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException {
            return sync.tryAcquireNanos(1, unit.toNanos(timeout));
        }

        /**
         * 20210811
         * A. 尝试释放此锁。
         * B. 如果当前线程是此锁的持有者，则持有计数递减。 如果保持计数现在为零，则锁定被释放。 如果当前线程不是此锁的持有者，则抛出 {@link IllegalMonitorStateException}。
         */
        /**
         * A.
         * Attempts to release this lock.
         *
         * B.
         * <p>If the current thread is the holder of this lock then
         * the hold count is decremented. If the hold count is now
         * zero then the lock is released.  If the current thread is
         * not the holder of this lock then {@link
         * IllegalMonitorStateException} is thrown.
         *
         * @throws IllegalMonitorStateException if the current thread does not
         * hold this lock
         */
        // 独占方式释放锁, 如果当前线程是此锁的持有者, 则持有计数递减; 如果保持计数现在为零, 则锁定被释放; 如果当前线程不是此锁的持有者, 则抛出 {@link IllegalMonitorStateException}
        public void unlock() {
            sync.release(1);
        }

        /**
         * 20210811
         * A. 返回与此 {@link Lock} 实例一起使用的 {@link Condition} 实例。
         * B. 返回的 {@link Condition} 实例支持与 {@link Object} 监控方法（{@link Object#wait() wait}、{@link Object#notify notify} 和
         *    {@link Object#notifyAll）相同的用法 notifyAll}) 与内置监视器锁一起使用时:
         *      a. 如果在调用任何 {@link Condition} 方法时未持有此写锁，则会引发 {@link IllegalMonitorStateException}。
         *        （读锁独立于写锁，因此不会被检查或影响。然而，当当前线程也获得了读锁时调用条件等待方法本质上总是一个错误，因为其他线程可以解除阻塞它不会被能够获取写锁。）
         *      b. 当条件 {@linkplain Condition#await() waiting} 方法被调用时，写锁被释放，在它们返回之前，写锁被重新获取，锁保持计数恢复到调用方法时的状态。
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
         * <li>If this write lock is not held when any {@link
         * Condition} method is called then an {@link
         * IllegalMonitorStateException} is thrown.  (Read locks are
         * held independently of write locks, so are not checked or
         * affected. However it is essentially always an error to
         * invoke a condition waiting method when the current thread
         * has also acquired read locks, since other threads that
         * could unblock it will not be able to acquire the write
         * lock.)
         *
         * <li>When the condition {@linkplain Condition#await() waiting}
         * methods are called the write lock is released and, before
         * they return, the write lock is reacquired and the lock hold
         * count restored to what it was when the method was called.
         *
         * <li>If a thread is {@linkplain Thread#interrupt interrupted} while
         * waiting then the wait will terminate, an {@link
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
        // 返回与此 {@link Lock} 实例一起使用的 {@link Condition} 实例; 如果在调用任何{@link Condition}方法时未持有此写锁, 则会引发 {@link IllegalMonitorStateException};
        public Condition newCondition() {
            return sync.newCondition();
        }

        // 返回标识此锁及其锁状态的字符串。 括号中的状态包括字符串 {@code "Unlocked"} 或字符串 {@code "Locked by"} 后跟拥有线程的 {@linkplain Thread#getName name}。
        /**
         * Returns a string identifying this lock, as well as its lock
         * state.  The state, in brackets includes either the String
         * {@code "Unlocked"} or the String {@code "Locked by"}
         * followed by the {@linkplain Thread#getName name} of the owning thread.
         *
         * // 标识此锁及其锁状态的字符串
         * @return a string identifying this lock, as well as its lock state
         */
        public String toString() {
            Thread o = sync.getOwner();
            return super.toString() + ((o == null) ?
                                       "[Unlocked]" :
                                       "[Locked by thread " + o.getName() + "]");
        }

        // 查询当前线程是否持有此写锁。 与 {@link ReentrantReadWriteLock#isWriteLockedByCurrentThread} 效果相同。
        /**
         * Queries if this write lock is held by the current thread.
         * Identical in effect to {@link
         * ReentrantReadWriteLock#isWriteLockedByCurrentThread}.
         *
         * @return {@code true} if the current thread holds this lock and
         *         {@code false} otherwise
         * @since 1.6
         */
        // 查询当前线程是否持有此写锁。 与 {@link ReentrantReadWriteLock#isWriteLockedByCurrentThread} 效果相同
        public boolean isHeldByCurrentThread() {
            return sync.isHeldExclusively();
        }

        // 查询当前线程持有该写锁的次数。一个线程为每个与解锁操作不匹配的锁操作持有一个锁。 与 {@link ReentrantReadWriteLock#getWriteHoldCount} 效果相同。
        /**
         * Queries the number of holds on this write lock by the current
         * thread.  A thread has a hold on a lock for each lock action
         * that is not matched by an unlock action.  Identical in effect
         * to {@link ReentrantReadWriteLock#getWriteHoldCount}.
         *
         * @return the number of holds on this lock by the current thread,
         *         or zero if this lock is not held by the current thread
         * @since 1.6
         */
        // 查询当前线程持有该写锁的次数
        public int getHoldCount() {
            return sync.getWriteHoldCount();
        }
    }

    // 仪表和状态
    // Instrumentation and status

    // 如果此锁的公平性设置为 true，则返回 {@code true}。
    /**
     * Returns {@code true} if this lock has fairness set true.
     *
     * @return {@code true} if this lock has fairness set true
     */
    // 如果此锁的公平性设置为 true，则返回 {@code true}。
    public final boolean isFair() {
        return sync instanceof FairSync;
    }

    // 返回当前拥有写锁的线程，如果不拥有，则返回 {@code null}。 当此方法由不是所有者的线程调用时，返回值反映了当前锁定状态的尽力而为的近似值。
    // 例如，即使有线程试图获取锁但尚未这样做，所有者也可能暂时{@code null}。 此方法旨在促进子类的构建，以提供更广泛的锁监控设施。
    /**
     * Returns the thread that currently owns the write lock, or
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
    // 返回当前拥有写锁的线程，如果不拥有，则返回 {@code null}。 当此方法由不是所有者的线程调用时，返回值反映了当前锁定状态的尽力而为的近似值。
    protected Thread getOwner() {
        return sync.getOwner();
    }

    // 查询为此锁持有的读锁数。 该方法设计用于监视系统状态，而不是用于同步控制。
    /**
     * Queries the number of read locks held for this lock. This
     * method is designed for use in monitoring system state, not for
     * synchronization control.
     *
     * @return the number of read locks held
     */
    // 查询此锁持有的读锁数。 该方法设计用于监视系统状态，而不是用于同步控制
    public int getReadLockCount() {
        return sync.getReadLockCount();
    }

    // 查询写锁是否被任何线程持有。 该方法设计用于监视系统状态，而不是用于同步控制。
    /**
     * Queries if the write lock is held by any thread. This method is
     * designed for use in monitoring system state, not for
     * synchronization control.
     *
     * @return {@code true} if any thread holds the write lock and
     *         {@code false} otherwise
     */
    // 查询写锁是否被任何线程持有。 该方法设计用于监视系统状态，而不是用于同步控制
    public boolean isWriteLocked() {
        return sync.isWriteLocked();
    }

    // 查询当前线程是否持有写锁。
    /**
     * Queries if the write lock is held by the current thread.
     *
     * @return {@code true} if the current thread holds the write lock and
     *         {@code false} otherwise
     */
    // 查询当前线程是否持有写锁
    public boolean isWriteLockedByCurrentThread() {
        return sync.isHeldExclusively();
    }

    // 查询当前线程对该锁的可重入写持有次数。一个写线程为每个与解锁操作不匹配的锁操作持有一个锁。
    /**
     * Queries the number of reentrant write holds on this lock by the
     * current thread.  A writer thread has a hold on a lock for
     * each lock action that is not matched by an unlock action.
     *
     * @return the number of holds on the write lock by the current thread,
     *         or zero if the write lock is not held by the current thread
     */
    // 查询当前线程对该锁的可重入写持有次数
    public int getWriteHoldCount() {
        return sync.getWriteHoldCount();
    }

    // 查询当前线程对该锁持有的可重入读次数。 读取器线程为每个与解锁操作不匹配的锁操作持有一个锁。
    /**
     * Queries the number of reentrant read holds on this lock by the
     * current thread.  A reader thread has a hold on a lock for
     * each lock action that is not matched by an unlock action.
     *
     * @return the number of holds on the read lock by the current thread,
     *         or zero if the read lock is not held by the current thread
     * @since 1.6
     */
    // 查询当前线程对该锁持有的可重入读次数
    public int getReadHoldCount() {
        return sync.getReadHoldCount();
    }

    // 返回一个包含可能正在等待获取写锁的线程的集合。 由于在构造此结果时实际线程集可能会动态更改，因此返回的集合只是尽力而为的估计。
    // 返回集合的元素没有特定的顺序。 此方法旨在促进子类的构建，以提供更广泛的锁监控设施。
    /**
     * Returns a collection containing threads that may be waiting to
     * acquire the write lock.  Because the actual set of threads may
     * change dynamically while constructing this result, the returned
     * collection is only a best-effort estimate.  The elements of the
     * returned collection are in no particular order.  This method is
     * designed to facilitate construction of subclasses that provide
     * more extensive lock monitoring facilities.
     *
     * @return the collection of threads
     */
    // 返回一个包含可能正在等待获取写锁的线程的集合
    protected Collection<Thread> getQueuedWriterThreads() {
        return sync.getExclusiveQueuedThreads();
    }

    // 返回一个包含可能正在等待获取读锁的线程的集合。 由于在构造此结果时实际线程集可能会动态更改，因此返回的集合只是尽力而为的估计。
    // 返回集合的元素没有特定的顺序。 此方法旨在促进子类的构建，以提供更广泛的锁监控设施。
    /**
     * Returns a collection containing threads that may be waiting to
     * acquire the read lock.  Because the actual set of threads may
     * change dynamically while constructing this result, the returned
     * collection is only a best-effort estimate.  The elements of the
     * returned collection are in no particular order.  This method is
     * designed to facilitate construction of subclasses that provide
     * more extensive lock monitoring facilities.
     *
     * @return the collection of threads
     */
    // 返回一个包含可能正在等待获取读锁的线程的集合
    protected Collection<Thread> getQueuedReaderThreads() {
        return sync.getSharedQueuedThreads();
    }

    // 查询是否有线程正在等待获取读锁或写锁。 请注意，因为取消可能随时发生，{@code true} 返回并不能保证任何其他线程将永远获得锁。 该方法主要设计用于监视系统状态。
    /**
     * Queries whether any threads are waiting to acquire the read or
     * write lock. Note that because cancellations may occur at any
     * time, a {@code true} return does not guarantee that any other
     * thread will ever acquire a lock.  This method is designed
     * primarily for use in monitoring of the system state.
     *
     * @return {@code true} if there may be other threads waiting to
     *         acquire the lock
     */
    // 查询是否有线程正在等待获取读锁或写锁
    public final boolean hasQueuedThreads() {
        return sync.hasQueuedThreads();
    }

    // 查询给定线程是否正在等待获取读锁或写锁。 请注意，因为取消可能随时发生，{@code true} 返回并不能保证该线程将永远获得锁。 该方法主要设计用于监视系统状态。
    /**
     * Queries whether the given thread is waiting to acquire either
     * the read or write lock. Note that because cancellations may
     * occur at any time, a {@code true} return does not guarantee
     * that this thread will ever acquire a lock.  This method is
     * designed primarily for use in monitoring of the system state.
     *
     * @param thread the thread
     * @return {@code true} if the given thread is queued waiting for this lock
     * @throws NullPointerException if the thread is null
     */
    // 查询给定线程是否正在等待获取读锁或写锁
    public final boolean hasQueuedThread(Thread thread) {
        return sync.isQueued(thread);
    }

    // 返回等待获取读锁或写锁的线程数的估计值。 该值只是一个估计值，因为当此方法遍历内部数据结构时，线程数可能会动态变化。 此方法设计用于监视系统状态，而不是用于同步控制。
    /**
     * Returns an estimate of the number of threads waiting to acquire
     * either the read or write lock.  The value is only an estimate
     * because the number of threads may change dynamically while this
     * method traverses internal data structures.  This method is
     * designed for use in monitoring of the system state, not for
     * synchronization control.
     *
     * @return the estimated number of threads waiting for this lock
     */
    // 返回等待获取读锁或写锁的线程数的估计值
    public final int getQueueLength() {
        return sync.getQueueLength();
    }

    // 返回一个包含可能正在等待获取读锁或写锁的线程的集合。 由于在构造此结果时实际线程集可能会动态更改，因此返回的集合只是尽力而为的估计。
    // 返回集合的元素没有特定的顺序。 此方法旨在促进子类的构建，以提供更广泛的监视设施。
    /**
     * Returns a collection containing threads that may be waiting to
     * acquire either the read or write lock.  Because the actual set
     * of threads may change dynamically while constructing this
     * result, the returned collection is only a best-effort estimate.
     * The elements of the returned collection are in no particular
     * order.  This method is designed to facilitate construction of
     * subclasses that provide more extensive monitoring facilities.
     *
     * @return the collection of threads
     */
    // 返回一个包含可能正在等待获取读锁或写锁的线程的集合
    protected Collection<Thread> getQueuedThreads() {
        return sync.getQueuedThreads();
    }

    // 查询是否有任何线程正在等待与写锁关联的给定条件。
    // 请注意，由于超时和中断可能随时发生，因此 {@code true} 返回并不能保证未来的 {@code signal} 会唤醒任何线程。 该方法主要设计用于监视系统状态。
    /**
     * Queries whether any threads are waiting on the given condition
     * associated with the write lock. Note that because timeouts and
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
    // 查询是否有任何线程正在等待与写锁关联的给定条件
    public boolean hasWaiters(Condition condition) {
        if (condition == null)
            throw new NullPointerException();
        if (!(condition instanceof AbstractQueuedSynchronizer.ConditionObject))
            throw new IllegalArgumentException("not owner");
        return sync.hasWaiters((AbstractQueuedSynchronizer.ConditionObject)condition);
    }

    // 返回等待与写锁关联的给定条件的线程数的估计值。 请注意，由于超时和中断可能随时发生，因此估计值仅用作实际服务员人数的上限。 此方法设计用于监视系统状态，而不是用于同步控制。
    /**
     * Returns an estimate of the number of threads waiting on the
     * given condition associated with the write lock. Note that because
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
    // 返回等待与写锁关联的给定条件的线程数的估计值
    public int getWaitQueueLength(Condition condition) {
        if (condition == null)
            throw new NullPointerException();
        if (!(condition instanceof AbstractQueuedSynchronizer.ConditionObject))
            throw new IllegalArgumentException("not owner");
        return sync.getWaitQueueLength((AbstractQueuedSynchronizer.ConditionObject)condition);
    }

    // 返回一个集合，其中包含那些可能正在等待与写锁关联的给定条件的线程。 由于在构造此结果时实际线程集可能会动态更改，因此返回的集合只是尽力而为的估计。
    // 返回集合的元素没有特定的顺序。 此方法旨在促进子类的构建，以提供更广泛的状态监控设施。
    /**
     * Returns a collection containing those threads that may be
     * waiting on the given condition associated with the write lock.
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
    // 返回一个集合，其中包含那些可能正在等待与写锁关联的给定条件的线程
    protected Collection<Thread> getWaitingThreads(Condition condition) {
        if (condition == null)
            throw new NullPointerException();
        if (!(condition instanceof AbstractQueuedSynchronizer.ConditionObject))
            throw new IllegalArgumentException("not owner");
        return sync.getWaitingThreads((AbstractQueuedSynchronizer.ConditionObject)condition);
    }

    // 返回标识此锁及其锁状态的字符串。 括号中的状态包括 String {@code "Write locks ="} 后跟可重入持有的写锁数量，以及 String {@code "Read locks ="} 后跟持有的读锁数量。
    /**
     * Returns a string identifying this lock, as well as its lock state.
     * The state, in brackets, includes the String {@code "Write locks ="}
     * followed by the number of reentrantly held write locks, and the
     * String {@code "Read locks ="} followed by the number of held
     * read locks.
     *
     * @return a string identifying this lock, as well as its lock state
     */
    public String toString() {
        int c = sync.getCount();
        int w = Sync.exclusiveCount(c);
        int r = Sync.sharedCount(c);

        return super.toString() +
            "[Write locks = " + w + ", Read locks = " + r + "]";
    }

    // 返回给定线程的线程ID。我们必须直接访问它，而不是通过Thread.getId()方法，因为getId()不是最终的，并且已知以不保留唯一映射的方式被覆盖。
    /**
     * Returns the thread id for the given thread.  We must access
     * this directly rather than via method Thread.getId() because
     * getId() is not final, and has been known to be overridden in
     * ways that do not preserve unique mappings.
     */
    static final long getThreadId(Thread thread) {
        return UNSAFE.getLongVolatile(thread, TID_OFFSET);
    }

    // 不安全的机制
    // Unsafe mechanics
    private static final sun.misc.Unsafe UNSAFE;
    private static final long TID_OFFSET;// tid
    static {
        try {
            UNSAFE = sun.misc.Unsafe.getUnsafe();
            Class<?> tk = Thread.class;
            TID_OFFSET = UNSAFE.objectFieldOffset(tk.getDeclaredField("tid"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }

}
