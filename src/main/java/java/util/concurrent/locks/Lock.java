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

/**
 * 20210725
 * A. {@code Lock} 实现提供了比使用 {@code synchronized} 方法和语句可以获得的更广泛的锁定操作。 它们允许更灵活的结构，可能具有完全不同的属性，
 *    并且可能支持多个关联的 {@link Condition} 对象。
 * B. 锁是用于控制多个线程对共享资源的访问的工具。通常，锁提供对共享资源的独占访问：一次只有一个线程可以获取锁，所有对共享资源的访问都需要先获取锁。
 *    但是，某些锁可能允许并发访问共享资源，例如 {@link ReadWriteLock} 的读锁。
 * C. {@code synchronized}方法或语句的使用提供对与每个对象关联的隐式监视器锁的访问，但强制所有锁获取和释放以块结构方式发生：
 *    当获取多个锁时，它们必须在相反的顺序，并且所有锁必须在它们被获取的同一个词法范围内释放。
 * D. 虽然{@code synchronized}方法和语句的作用域机制使使用监视器锁编程变得更加容易，并有助于避免许多涉及锁的常见编程错误，但在某些情况下，您需要以更灵活的方式使用锁。
 *    例如，一些遍历并发访问数据结构的算法需要使用“hand-over-hand”或“chainlocking”：你先获取节点A的锁，然后节点B，然后释放A，获取C，然后释放B 并获得 D 等等。
 *    {@code Lock}接口的实现允许在不同范围内获取和释放锁，并允许以任何顺序获取和释放多个锁，从而允许使用此类技术。
 * E. 这种增加的灵活性带来了额外的责任。块结构锁的缺失消除了{@code synchronized}方法和语句发生的锁的自动释放。在大多数情况下，应使用以下习语：
 * {@code
 * Lock l = ...;
 * l.lock();
 * try {
 *   // access the resource protected by this lock
 * } finally {
 *   l.unlock();
 * }}
 * 当锁定和解锁发生在不同的作用域时，必须注意确保持有锁时执行的所有代码都受到try-finally或try-catch的保护，以确保在必要时释放锁。
 * F. {@code Lock}实现通过提供非阻塞的获取锁的尝试({@link #tryLock()})来提供比使用{@code synchronized}方法和语句更多的功能，
 *    尝试获取锁可以被中断（{@link #lockInterruptably}，并且尝试获取可以超时的锁（{@link #tryLock(long, TimeUnit)}）。
 * G. {@code Lock}类还可以提供与隐式监视器锁完全不同的行为和语义，例如保证排序、不可重入使用或死锁检测。如果实现提供了这样的专门语义，那么实现必须记录这些语义。
 * H. 请注意，{@code Lock}实例只是普通对象，它们本身可以用作{@code synchronized}语句中的目标。
 *    获取{@code Lock}实例的监视器锁与调用该实例的任何{@link #lock}方法没有指定的关系。建议不要以这种方式使用{@code Lock} 实例以避免混淆，除非在它们自己的实现中。
 * I. 除非另有说明，否则为任何参数传递{@code null} 值将导致抛出 {@link NullPointerException}。
 *
 * 内存同步
 * J. 所有{@code Lock}实现都必须强制执行与内置监视器锁提供的相同的内存同步语义，如 Java 语言规范（17.4 内存模型）中所述：
 *      a. 成功的 {@code lock} 操作与成功的 Lock 操作具有相同的内存同步效果。
 *      b. 成功的 {@code unlock} 操作与成功的 Unlock 操作具有相同的内存同步效果。
 *    不成功的加锁和解锁操作，以及可重入加锁/解锁操作，不需要任何内存同步效果。
 *
 * 实施注意事项
 * K. 锁获取的三种形式（可中断、不可中断和定时）可能在它们的性能特征、排序保证或其他实现质量方面有所不同。此外，在给定的{@code Lock}类中可能无法中断正在进行的锁定获取。
 *    因此，实现不需要为所有三种形式的锁获取定义完全相同的保证或语义，也不需要支持正在进行的锁获取的中断。 需要一个实现来清楚地记录每个锁定方法提供的语义和保证。
 *    它还必须遵守此接口中定义的中断语义，以支持锁定获取的中断：完全或仅在方法入口上。
 * L. 由于中断通常意味着取消，并且中断检查通常很少，因此实现可以倾向于响应中断而不是正常的方法返回。
 *    即使可以证明中断发生在另一个动作可能已经解除了线程的阻塞之后也是如此。 实现应记录此行为。
 */

/**
 * A.
 * {@code Lock} implementations provide more extensive locking
 * operations than can be obtained using {@code synchronized} methods
 * and statements.  They allow more flexible structuring, may have
 * quite different properties, and may support multiple associated
 * {@link Condition} objects.
 *
 * B.
 * <p>A lock is a tool for controlling access to a shared resource by
 * multiple threads. Commonly, a lock provides exclusive access to a
 * shared resource: only one thread at a time can acquire the lock and
 * all access to the shared resource requires that the lock be
 * acquired first. However, some locks may allow concurrent access to
 * a shared resource, such as the read lock of a {@link ReadWriteLock}.
 *
 * C.
 * <p>The use of {@code synchronized} methods or statements provides
 * access to the implicit monitor lock associated with every object, but
 * forces all lock acquisition and release to occur in a block-structured way:
 * when multiple locks are acquired they must be released in the opposite
 * order, and all locks must be released in the same lexical scope in which
 * they were acquired.
 *
 * D.
 * <p>While the scoping mechanism for {@code synchronized} methods
 * and statements makes it much easier to program with monitor locks,
 * and helps avoid many common programming errors involving locks,
 * there are occasions where you need to work with locks in a more
 * flexible way. For example, some algorithms for traversing
 * concurrently accessed data structures require the use of
 * &quot;hand-over-hand&quot; or &quot;chain locking&quot;: you
 * acquire the lock of node A, then node B, then release A and acquire
 * C, then release B and acquire D and so on.  Implementations of the
 * {@code Lock} interface enable the use of such techniques by
 * allowing a lock to be acquired and released in different scopes,
 * and allowing multiple locks to be acquired and released in any
 * order.
 *
 * E.
 * <p>With this increased flexibility comes additional
 * responsibility. The absence of block-structured locking removes the
 * automatic release of locks that occurs with {@code synchronized}
 * methods and statements. In most cases, the following idiom
 * should be used:
 *
 *  <pre> {@code
 * Lock l = ...;
 * l.lock();
 * try {
 *   // access the resource protected by this lock
 * } finally {
 *   l.unlock();
 * }}</pre>
 *
 * When locking and unlocking occur in different scopes, care must be
 * taken to ensure that all code that is executed while the lock is
 * held is protected by try-finally or try-catch to ensure that the
 * lock is released when necessary.
 *
 * F.
 * <p>{@code Lock} implementations provide additional functionality
 * over the use of {@code synchronized} methods and statements by
 * providing a non-blocking attempt to acquire a lock ({@link
 * #tryLock()}), an attempt to acquire the lock that can be
 * interrupted ({@link #lockInterruptibly}, and an attempt to acquire
 * the lock that can timeout ({@link #tryLock(long, TimeUnit)}).
 *
 * G.
 * <p>A {@code Lock} class can also provide behavior and semantics
 * that is quite different from that of the implicit monitor lock,
 * such as guaranteed ordering, non-reentrant usage, or deadlock
 * detection. If an implementation provides such specialized semantics
 * then the implementation must document those semantics.
 *
 * H.
 * <p>Note that {@code Lock} instances are just normal objects and can
 * themselves be used as the target in a {@code synchronized} statement.
 * Acquiring the
 * monitor lock of a {@code Lock} instance has no specified relationship
 * with invoking any of the {@link #lock} methods of that instance.
 * It is recommended that to avoid confusion you never use {@code Lock}
 * instances in this way, except within their own implementation.
 *
 * I.
 * <p>Except where noted, passing a {@code null} value for any
 * parameter will result in a {@link NullPointerException} being
 * thrown.
 *
 * <h3>Memory Synchronization</h3>
 *
 * J.
 * <p>All {@code Lock} implementations <em>must</em> enforce the same
 * memory synchronization semantics as provided by the built-in monitor
 * lock, as described in
 * <a href="https://docs.oracle.com/javase/specs/jls/se7/html/jls-17.html#jls-17.4">
 * The Java Language Specification (17.4 Memory Model)</a>:
 * <ul>
 * <li>A successful {@code lock} operation has the same memory
 * synchronization effects as a successful <em>Lock</em> action.
 * <li>A successful {@code unlock} operation has the same
 * memory synchronization effects as a successful <em>Unlock</em> action.
 * </ul>
 *
 * Unsuccessful locking and unlocking operations, and reentrant
 * locking/unlocking operations, do not require any memory
 * synchronization effects.
 *
 * <h3>Implementation Considerations</h3>
 *
 * K.
 * <p>The three forms of lock acquisition (interruptible,
 * non-interruptible, and timed) may differ in their performance
 * characteristics, ordering guarantees, or other implementation
 * qualities.  Further, the ability to interrupt the <em>ongoing</em>
 * acquisition of a lock may not be available in a given {@code Lock}
 * class.  Consequently, an implementation is not required to define
 * exactly the same guarantees or semantics for all three forms of
 * lock acquisition, nor is it required to support interruption of an
 * ongoing lock acquisition.  An implementation is required to clearly
 * document the semantics and guarantees provided by each of the
 * locking methods. It must also obey the interruption semantics as
 * defined in this interface, to the extent that interruption of lock
 * acquisition is supported: which is either totally, or only on
 * method entry.
 *
 * L.
 * <p>As interruption generally implies cancellation, and checks for
 * interruption are often infrequent, an implementation can favor responding
 * to an interrupt over normal method return. This is true even if it can be
 * shown that the interrupt occurred after another action may have unblocked
 * the thread. An implementation should document this behavior.
 *
 * @see ReentrantLock
 * @see Condition
 * @see ReadWriteLock
 *
 * @since 1.5
 * @author Doug Lea
 */
public interface Lock {

    /**
     * 20210725
     * A. 获得锁。
     * B. 如果锁不可用，则当前线程将被禁用以用于线程调度目的并处于休眠状态，直到获得锁。
     * C. 实施注意事项: {@code Lock} 实现可能能够检测到锁的错误使用，例如会导致死锁的调用，并可能在这种情况下抛出（未经检查的）异常。
     *    {@code Lock} 实现必须记录情况和异常类型。
     */
    /**
     * A.
     * Acquires the lock.
     *
     * B.
     * <p>If the lock is not available then the current thread becomes
     * disabled for thread scheduling purposes and lies dormant until the
     * lock has been acquired.
     *
     * <p><b>Implementation Considerations</b>
     *
     * C.
     * <p>A {@code Lock} implementation may be able to detect erroneous use
     * of the lock, such as an invocation that would cause deadlock, and
     * may throw an (unchecked) exception in such circumstances.  The
     * circumstances and the exception type must be documented by that
     * {@code Lock} implementation.
     */
    // 阻塞方式获取锁
    void lock();

    /**
     * 20210725
     * A. 除非当前线程是{@linkplain Thread#interrupt interrupted}，否则获取锁。
     * B. 如果可用，则获取锁并立即返回。
     * C. 如果锁不可用，则当前线程将出于线程调度目的而被禁用并处于休眠状态，直到发生以下两种情况之一：
     *      a. 锁被当前线程获取；
     *      b. 其他一些线程{@linkplain Thread#interrupt}中断当前线程，支持中断获取锁。
     * D. 如果当前线程：
     *      a. 在进入此方法时设置其中断状态；
     *      b. 获取锁时是{@linkplain Thread#interrupt interrupted}，并且支持中断获取锁,
     *    然后抛出 {@link InterruptedException} 并清除当前线程的中断状态。
     *
     * 实施注意事项
     * E. 在某些实现中，中断获取锁的能力可能是不可能的，如果可能的话，这可能是一项昂贵的操作。 程序员应该意识到可能是这种情况。在这种情况下，实现应该记录。
     * F. 与正常方法返回相比，实现可以更倾向于响应中断。
     * G. {@code Lock}实现可能能够检测到锁的错误使用，例如会导致死锁的调用，并可能在这种情况下抛出（未经检查的）异常。 {@code Lock} 实现必须记录情况和异常类型。
     */
    /**
     * Acquires the lock unless the current thread is
     * {@linkplain Thread#interrupt interrupted}.
     *
     * <p>Acquires the lock if it is available and returns immediately.
     *
     * <p>If the lock is not available then the current thread becomes
     * disabled for thread scheduling purposes and lies dormant until
     * one of two things happens:
     *
     * <ul>
     * <li>The lock is acquired by the current thread; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts} the
     * current thread, and interruption of lock acquisition is supported.
     * </ul>
     *
     * <p>If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@linkplain Thread#interrupt interrupted} while acquiring the
     * lock, and interruption of lock acquisition is supported,
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     *
     * <p><b>Implementation Considerations</b>
     *
     * <p>The ability to interrupt a lock acquisition in some
     * implementations may not be possible, and if possible may be an
     * expensive operation.  The programmer should be aware that this
     * may be the case. An implementation should document when this is
     * the case.
     *
     * <p>An implementation can favor responding to an interrupt over
     * normal method return.
     *
     * <p>A {@code Lock} implementation may be able to detect
     * erroneous use of the lock, such as an invocation that would
     * cause deadlock, and may throw an (unchecked) exception in such
     * circumstances.  The circumstances and the exception type must
     * be documented by that {@code Lock} implementation.
     *
     * // 如果当前线程在获取锁时中断（并且支持中断获取锁）
     * @throws InterruptedException if the current thread is
     *         interrupted while acquiring the lock (and interruption
     *         of lock acquisition is supported)
     */
    // 阻塞可中断方式获取锁
    void lockInterruptibly() throws InterruptedException;

    /**
     * 20210725
     * A. 仅在调用时空闲时才获取锁。
     * B. 如果可用，则获取锁并立即返回值 {@code true}。 如果锁不可用，则此方法将立即返回值 {@code false}。
     * C. 这种方法的典型用法是：
     *  {@code
     * Lock lock = ...;
     * if (lock.tryLock()) {
     *   try {
     *     // manipulate protected state
     *   } finally {
     *     lock.unlock();
     *   }
     * } else {
     *   // perform alternative actions
     * }}
     * 此用法可确保在获得锁时解锁，如果未获得锁，则不会尝试解锁。
     */
    /**
     * A.
     * Acquires the lock only if it is free at the time of invocation.
     *
     * B.
     * <p>Acquires the lock if it is available and returns immediately
     * with the value {@code true}.
     * If the lock is not available then this method will return
     * immediately with the value {@code false}.
     *
     * C.
     * <p>A typical usage idiom for this method would be:
     *  <pre> {@code
     * Lock lock = ...;
     * if (lock.tryLock()) {
     *   try {
     *     // manipulate protected state
     *   } finally {
     *     lock.unlock();
     *   }
     * } else {
     *   // perform alternative actions
     * }}</pre>
     *
     * This usage ensures that the lock is unlocked if it was acquired, and
     * doesn't try to unlock if the lock was not acquired.
     *
     * @return {@code true} if the lock was acquired and
     *         {@code false} otherwise
     */
    // 尝试获取锁, 如果可用则获取锁并立即返回值{@code true}; 如果锁不可用, 则立即返回值{@code false}
    boolean tryLock();

    /**
     * 20210725
     * A. 如果在给定的等待时间内空闲并且当前线程未被{@linkplain Thread#interrupt interrupted}，则获取锁。
     * B. 如果锁可用，则此方法立即返回值 {@code true}。 如果锁不可用，则当前线程将出于线程调度目的而被禁用并处于休眠状态，直到发生以下三种情况之一：
     *      a. 锁被当前线程获取；
     *      b. 其他一些线程{@linkplain Thread#interrupt中断}当前线程，支持锁获取中断；
     *      c. 经过指定的等待时间
     * C. 如果获得了锁，则返回值 {@code true}。
     * D. 如果当前线程：
     *      a. 在进入此方法时设置其中断状态；
     *      b. 获取锁时是{@linkplain Thread#interrupt interrupted}，并且支持中断获取锁，
     *    然后抛出 {@link InterruptedException} 并清除当前线程的中断状态。
     * E. 如果指定的等待时间过去，则返回值 {@code false}。 如果时间小于或等于零，则该方法根本不会等待。
     *
     * 实施注意事项
     * F. 在某些实现中，中断获取锁的能力可能是不可能的，如果可能的话，这可能是一项昂贵的操作。 程序员应该意识到可能是这种情况。 在这种情况下，实现应该记录。
     * G. 实现可以倾向于响应中断而不是正常方法返回，或报告超时。
     * H. {@code Lock} 实现可能能够检测到锁的错误使用，例如会导致死锁的调用，并可能在这种情况下抛出（未经检查的）异常。 {@code Lock} 实现必须记录情况和异常类型。
     */
    /**
     * A.
     * Acquires the lock if it is free within the given waiting time and the
     * current thread has not been {@linkplain Thread#interrupt interrupted}.
     *
     * B.
     * <p>If the lock is available this method returns immediately
     * with the value {@code true}.
     * If the lock is not available then
     * the current thread becomes disabled for thread scheduling
     * purposes and lies dormant until one of three things happens:
     * <ul>
     * <li>The lock is acquired by the current thread; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts} the
     * current thread, and interruption of lock acquisition is supported; or
     * <li>The specified waiting time elapses
     * </ul>
     *
     * C.
     * <p>If the lock is acquired then the value {@code true} is returned.
     *
     * D.
     * <p>If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@linkplain Thread#interrupt interrupted} while acquiring
     * the lock, and interruption of lock acquisition is supported,
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     *
     * E.
     * <p>If the specified waiting time elapses then the value {@code false}
     * is returned.
     * If the time is
     * less than or equal to zero, the method will not wait at all.
     *
     * <p><b>Implementation Considerations</b>
     *
     * F.
     * <p>The ability to interrupt a lock acquisition in some implementations
     * may not be possible, and if possible may
     * be an expensive operation.
     * The programmer should be aware that this may be the case. An
     * implementation should document when this is the case.
     *
     * G.
     * <p>An implementation can favor responding to an interrupt over normal
     * method return, or reporting a timeout.
     *
     * H.
     * <p>A {@code Lock} implementation may be able to detect
     * erroneous use of the lock, such as an invocation that would cause
     * deadlock, and may throw an (unchecked) exception in such circumstances.
     * The circumstances and the exception type must be documented by that
     * {@code Lock} implementation.
     *
     * @param time the maximum time to wait for the lock
     * @param unit the time unit of the {@code time} argument
     * @return {@code true} if the lock was acquired and {@code false}
     *         if the waiting time elapsed before the lock was acquired
     *
     * @throws InterruptedException if the current thread is interrupted
     *         while acquiring the lock (and interruption of lock
     *         acquisition is supported)
     */
    // 定时可中断式尝试获取锁, 获得则返回true, 否则返回false
    boolean tryLock(long time, TimeUnit unit) throws InterruptedException;

    /**
     * 20210725
     * A. 释放锁。
     *
     * 实施注意事项
     * B. {@code Lock} 实现通常会对可以释放锁的线程施加限制（通常只有锁的持有者可以释放它）并且如果违反限制可能会抛出（未经检查的）异常。{@code Lock} 实现必须记录任何限制和异常类型。
     */
    /**
     * Releases the lock.
     *
     * <p><b>Implementation Considerations</b>
     *
     * <p>A {@code Lock} implementation will usually impose
     * restrictions on which thread can release a lock (typically only the
     * holder of the lock can release it) and may throw
     * an (unchecked) exception if the restriction is violated.
     * Any restrictions and the exception
     * type must be documented by that {@code Lock} implementation.
     */
    // 释放锁
    void unlock();

    /**
     * 20210725
     * A. 返回绑定到此{@code Lock}实例的新{@link Condition}实例。
     * B. 在等待条件之前，当前线程必须持有锁。调用 {@link Condition#await()}将在等待之前自动释放锁，并在等待返回之前重新获取锁。
     *
     * 实施注意事项
     * C. {@link Condition}实例的确切操作取决于{@code Lock}实现，并且必须由该实现记录。
     */
    /**
     * A.
     * Returns a new {@link Condition} instance that is bound to this
     * {@code Lock} instance.
     *
     * B.
     * <p>Before waiting on the condition the lock must be held by the
     * current thread.
     * A call to {@link Condition#await()} will atomically release the lock
     * before waiting and re-acquire the lock before the wait returns.
     *
     * <p><b>Implementation Considerations</b>
     *
     * C.
     * <p>The exact operation of the {@link Condition} instance depends on
     * the {@code Lock} implementation and must be documented by that
     * implementation.
     *
     * @return A new {@link Condition} instance for this {@code Lock} instance
     * @throws UnsupportedOperationException if this {@code Lock}
     *         implementation does not support conditions
     */
    // 返回绑定到此{@code Lock}实例的{@link Condition}实例 => eg: 在Condition#await之前，当前线程必须持有锁
    Condition newCondition();
}
