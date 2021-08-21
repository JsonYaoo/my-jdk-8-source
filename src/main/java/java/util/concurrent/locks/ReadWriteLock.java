/*
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util.concurrent.locks;

/**
 * 20210810
 * A. {@code ReadWriteLock}维护一对关联的{@link Lock 锁}，一个用于只读操作，一个用于写入。{@link #readLock 读锁}可以由多个读取器线程同时持有，只要没有写入器即可。
 *    {@link #writeLock写锁}是独占的。
 * B. 所有 {@code ReadWriteLock} 实现都必须保证 {@code writeLock} 操作（如 {@link Lock} 接口中指定）的内存同步效果也适用于相关联的 {@code readLock}。
 *    也就是说，成功获取读锁的线程将看到在之前释放写锁时所做的所有更新。
 * C. 与互斥锁相比，读写锁允许更高级别的并发访问共享数据。 它利用了这样一个事实，即虽然一次只有一个线程（写入线程）可以修改共享数据，但在许多情况下，
 *    任意数量的线程都可以同时读取数据（因此是读取线程）。理论上，与使用互斥锁相比，使用读写锁所允许的并发性增加将导致性能改进。
 *    在实践中，这种并发性的增加只有在多处理器上才能完全实现，而且只有在共享数据的访问模式合适的情况下才能实现。
 * D. 与使用互斥锁相比，读写锁是否会提高性能取决于读取数据与修改数据的频率、读写操作的持续时间以及对数据的争用 - 即即，将尝试同时读取或写入数据的线程数。
 *    例如，最初用数据填充然后很少修改但经常被搜索（例如某种目录）的集合是使用读写锁的理想候选者。但是，如果更新变得频繁，那么数据的大部分时间都被排他性地锁定，
 *    并且并发性几乎没有增加。此外，如果读操作太短，读写锁实现的开销（本质上比互斥锁更复杂）可能会支配执行成本，特别是因为许多读写锁实现仍然通过一个序列化所有线程一小段代码。
 *    最终，只有分析和测量才能确定读写锁的使用是否适合您的应用程序。
 * E. 尽管读写锁的基本操作很简单，但实现必须做出许多策略决策，这可能会影响给定应用程序中读写锁的有效性。 这些政策的例子包括：
 *      a. 确定是授予读锁还是写锁，当读者和写者都在等待时，在写者释放写锁时。 Writer 偏好很常见，因为预计写入时间较短且不频繁。
 *         读者偏好不太常见，因为如果读者如预期的那样频繁且长期存在，它会导致写入的长时间延迟。 公平或“有序”的实现也是可能的。
 *      b. 确定在读取器处于活动状态且写入器正在等待时请求读取锁定的读取器是否被授予读取锁定。 对读者的偏好可以无限期地延迟写者，而对写者的偏好可以降低并发的可能性。
 *      c. 确定锁是否可重入：具有写锁的线程可以重新获取它吗？ 它可以在持有写锁的同时获取读锁吗？ 读锁本身是可重入的吗？
 *      d. 是否可以将写锁降级为读锁而不允许干预写入器？ 读锁可以升级为写锁，优先于其他等待的读取器或写入器吗？
 *    在评估给定实现对您的应用程序的适用性时，您应该考虑所有这些事情。
 */
/**
 * A.
 * A {@code ReadWriteLock} maintains a pair of associated {@link
 * Lock locks}, one for read-only operations and one for writing.
 * The {@link #readLock read lock} may be held simultaneously by
 * multiple reader threads, so long as there are no writers.  The
 * {@link #writeLock write lock} is exclusive.
 *
 * B.
 * <p>All {@code ReadWriteLock} implementations must guarantee that
 * the memory synchronization effects of {@code writeLock} operations
 * (as specified in the {@link Lock} interface) also hold with respect
 * to the associated {@code readLock}. That is, a thread successfully
 * acquiring the read lock will see all updates made upon previous
 * release of the write lock.
 *
 * C.
 * <p>A read-write lock allows for a greater level of concurrency in
 * accessing shared data than that permitted by a mutual exclusion lock.
 * It exploits the fact that while only a single thread at a time (a
 * <em>writer</em> thread) can modify the shared data, in many cases any
 * number of threads can concurrently read the data (hence <em>reader</em>
 * threads).
 * In theory, the increase in concurrency permitted by the use of a read-write
 * lock will lead to performance improvements over the use of a mutual
 * exclusion lock. In practice this increase in concurrency will only be fully
 * realized on a multi-processor, and then only if the access patterns for
 * the shared data are suitable.
 *
 * D.
 * <p>Whether or not a read-write lock will improve performance over the use
 * of a mutual exclusion lock depends on the frequency that the data is
 * read compared to being modified, the duration of the read and write
 * operations, and the contention for the data - that is, the number of
 * threads that will try to read or write the data at the same time.
 * For example, a collection that is initially populated with data and
 * thereafter infrequently modified, while being frequently searched
 * (such as a directory of some kind) is an ideal candidate for the use of
 * a read-write lock. However, if updates become frequent then the data
 * spends most of its time being exclusively locked and there is little, if any
 * increase in concurrency. Further, if the read operations are too short
 * the overhead of the read-write lock implementation (which is inherently
 * more complex than a mutual exclusion lock) can dominate the execution
 * cost, particularly as many read-write lock implementations still serialize
 * all threads through a small section of code. Ultimately, only profiling
 * and measurement will establish whether the use of a read-write lock is
 * suitable for your application.
 *
 * E.
 * <p>Although the basic operation of a read-write lock is straight-forward,
 * there are many policy decisions that an implementation must make, which
 * may affect the effectiveness of the read-write lock in a given application.
 * Examples of these policies include:
 * <ul>
 * <li>Determining whether to grant the read lock or the write lock, when
 * both readers and writers are waiting, at the time that a writer releases
 * the write lock. Writer preference is common, as writes are expected to be
 * short and infrequent. Reader preference is less common as it can lead to
 * lengthy delays for a write if the readers are frequent and long-lived as
 * expected. Fair, or &quot;in-order&quot; implementations are also possible.
 *
 * <li>Determining whether readers that request the read lock while a
 * reader is active and a writer is waiting, are granted the read lock.
 * Preference to the reader can delay the writer indefinitely, while
 * preference to the writer can reduce the potential for concurrency.
 *
 * <li>Determining whether the locks are reentrant: can a thread with the
 * write lock reacquire it? Can it acquire a read lock while holding the
 * write lock? Is the read lock itself reentrant?
 *
 * <li>Can the write lock be downgraded to a read lock without allowing
 * an intervening writer? Can a read lock be upgraded to a write lock,
 * in preference to other waiting readers or writers?
 *
 * </ul>
 * You should consider all of these things when evaluating the suitability
 * of a given implementation for your application.
 *
 * @see ReentrantReadWriteLock
 * @see Lock
 * @see ReentrantLock
 *
 * @since 1.5
 * @author Doug Lea
 */
public interface ReadWriteLock {

    // 返回用于读取的锁。
    /**
     * Returns the lock used for reading.
     *
     * @return the lock used for reading
     */
    Lock readLock();

    // 返回用于写入的锁。
    /**
     * Returns the lock used for writing.
     *
     * @return the lock used for writing
     */
    Lock writeLock();
}
