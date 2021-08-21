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
import java.util.Date;

/**
 * 20210725
 * A. {@code Condition}将{@code Object}监控方法（{@link Object#wait() wait}、{@link Object#notify notify}和{@link Object#notifyAll notifyAll}）
 *    分解为不同的对象以给出通过将它们与任意{@link Lock}实现的使用相结合，每个对象具有多个等待集的效果。{@code Lock}代替了{@code synchronized}方法和语句的使用，
 *    {@code Condition}代替了对象监视器方法的使用。
 * B. 条件（也称为条件队列或条件变量）为一个线程提供了一种挂起执行（“等待”）的方法，直到另一个线程通知某个状态条件现在可能为真。因为对这个共享状态信息的访问发生在不同的线程中，
 *    它必须受到保护，所以某种形式的锁与条件相关联。等待条件提供的关键属性是它以原子方式释放关联的锁并挂起当前线程，就像{@code Object.wait}一样。
 * C. {@code Condition}实例本质上绑定到锁。要获取特定{@link Lock}实例的{@code Condition}实例，请使用其{@link Lock#newCondition newCondition()} 方法。
 * D. 例如，假设我们有一个支持{@code put}和{@code take}方法的有界缓冲区。如果在空缓冲区上尝试{@code take}，则线程将阻塞，直到项目可用；
 *    如果在一个完整的缓冲区上尝试{@code put}，则线程将阻塞，直到有可用空间为止。我们希望在不同的等待集中继续等待{@code put} 线程 {@code take} 线程，以便我们可以使用优化，
 *    当缓冲区中的项目或空间变得可用时，一次只通知一个线程。这可以使用两个{@link Condition}实例来实现。
 * class BoundedBuffer {
 *   final Lock lock = new ReentrantLock();
 *   final Condition notFull  = lock.newCondition();
 *   final Condition notEmpty = lock.newCondition();
 *
 *   final Object[] items = new Object[100];
 *   int putptr, takeptr, count;
 *
 *   public void put(Object x) throws InterruptedException {
 *     lock.lock();
 *     try {
 *       while (count == items.length)
 *         notFull.await();
 *       items[putptr] = x;
 *       if (++putptr == items.length) putptr = 0;
 *       ++count;
 *       notEmpty.signal();
 *     } finally {
 *       lock.unlock();
 *     }
 *   }
 *
 *   public Object take() throws InterruptedException {
 *     lock.lock();
 *     try {
 *       while (count == 0)
 *         notEmpty.await();
 *       Object x = items[takeptr];
 *       if (++takeptr == items.length) takeptr = 0;
 *       --count;
 *       notFull.signal();
 *       return x;
 *     } finally {
 *       lock.unlock();
 *     }
 *   }
 * }
 * （{@link java.util.concurrent.ArrayBlockingQueue} 类提供此功能，因此没有理由实现此示例用法类。）
 * E. {@code Condition}实现可以提供与{@code Object}监视器方法不同的行为和语义，例如保证通知的顺序，或者在执行通知时不需要持有锁。如果实现提供了这样的专门语义，
 *    那么实现必须记录这些语义。
 * F. 请注意，{@code Condition}实例只是普通对象，它们本身可以用作{@code synchronized} 语句中的目标，并且可以拥有自己的监视器{@link Object#wait wait}和
 *    {@link Object#notify 通知}方法调用。获取{@code Condition}实例的监视器锁，或使用其监视器方法，
 *    与获取与该{@code Condition}关联的{@link Lock}或使用其{@linkplain #await waiting }和{@linkplain #signal 信号} 方法。
 *    建议您不要以这种方式使用{@code Condition} 实例以避免混淆，除非在它们自己的实现中。
 * G. 除非另有说明，否则为任何参数传递 {@code null} 值将导致抛出 {@link NullPointerException}。
 *
 * 实施注意事项
 * H. 在等待{@code Condition}时，通常允许发生“虚假唤醒”，作为对底层平台语义的让步。这对大多数应用程序几乎没有实际影响，因为{@code Condition}应该始终在循环中等待，
 *    测试正在等待的状态谓词。实现可以自由地消除虚假唤醒的可能性，但建议应用程序程序员始终假设它们可能发生，因此始终在循环中等待。
 * I. 三种形式的条件等待（可中断、不可中断和定时）在某些平台上的实现难易程度和性能特征方面可能有所不同。特别是，可能很难提供这些功能并维护特定的语义，例如排序保证。
 *    此外，在所有平台上实现中断线程实际挂起的能力并不总是可行的。
 * J. 因此，实现不需要为所有三种等待形式定义完全相同的保证或语义，也不需要支持线程实际挂起的中断。
 * K. 实现需要明确的地等待方法提供的实现和记录保证，并且当一个确实支持线程挂起的中断时，它必须严格遵守接口中定义的中断故障。
 * L. 由于中断通常意味着取消，并且中断检查通常很少，因此实现可以倾向于响应中断而不是正常的方法返回。 即使可以证明中断发生在另一个可能已解除线程阻塞的操作之后也是如此。
 *    实现应记录此行为。
 */

/**
 * A.
 * {@code Condition} factors out the {@code Object} monitor
 * methods ({@link Object#wait() wait}, {@link Object#notify notify}
 * and {@link Object#notifyAll notifyAll}) into distinct objects to
 * give the effect of having multiple wait-sets per object, by
 * combining them with the use of arbitrary {@link Lock} implementations.
 * Where a {@code Lock} replaces the use of {@code synchronized} methods
 * and statements, a {@code Condition} replaces the use of the Object
 * monitor methods.
 *
 * B.
 * <p>Conditions (also known as <em>condition queues</em> or
 * <em>condition variables</em>) provide a means for one thread to
 * suspend execution (to &quot;wait&quot;) until notified by another
 * thread that some state condition may now be true.  Because access
 * to this shared state information occurs in different threads, it
 * must be protected, so a lock of some form is associated with the
 * condition. The key property that waiting for a condition provides
 * is that it <em>atomically</em> releases the associated lock and
 * suspends the current thread, just like {@code Object.wait}.
 *
 * C.
 * <p>A {@code Condition} instance is intrinsically bound to a lock.
 * To obtain a {@code Condition} instance for a particular {@link Lock}
 * instance use its {@link Lock#newCondition newCondition()} method.
 *
 * D.
 * <p>As an example, suppose we have a bounded buffer which supports
 * {@code put} and {@code take} methods.  If a
 * {@code take} is attempted on an empty buffer, then the thread will block
 * until an item becomes available; if a {@code put} is attempted on a
 * full buffer, then the thread will block until a space becomes available.
 * We would like to keep waiting {@code put} threads and {@code take}
 * threads in separate wait-sets so that we can use the optimization of
 * only notifying a single thread at a time when items or spaces become
 * available in the buffer. This can be achieved using two
 * {@link Condition} instances.
 * <pre>
 * class BoundedBuffer {
 *   <b>final Lock lock = new ReentrantLock();</b>
 *   final Condition notFull  = <b>lock.newCondition(); </b>
 *   final Condition notEmpty = <b>lock.newCondition(); </b>
 *
 *   final Object[] items = new Object[100];
 *   int putptr, takeptr, count;
 *
 *   public void put(Object x) throws InterruptedException {
 *     <b>lock.lock();
 *     try {</b>
 *       while (count == items.length)
 *         <b>notFull.await();</b>
 *       items[putptr] = x;
 *       if (++putptr == items.length) putptr = 0;
 *       ++count;
 *       <b>notEmpty.signal();</b>
 *     <b>} finally {
 *       lock.unlock();
 *     }</b>
 *   }
 *
 *   public Object take() throws InterruptedException {
 *     <b>lock.lock();
 *     try {</b>
 *       while (count == 0)
 *         <b>notEmpty.await();</b>
 *       Object x = items[takeptr];
 *       if (++takeptr == items.length) takeptr = 0;
 *       --count;
 *       <b>notFull.signal();</b>
 *       return x;
 *     <b>} finally {
 *       lock.unlock();
 *     }</b>
 *   }
 * }
 * </pre>
 *
 * (The {@link java.util.concurrent.ArrayBlockingQueue} class provides
 * this functionality, so there is no reason to implement this
 * sample usage class.)
 *
 * E.
 * <p>A {@code Condition} implementation can provide behavior and semantics
 * that is
 * different from that of the {@code Object} monitor methods, such as
 * guaranteed ordering for notifications, or not requiring a lock to be held
 * when performing notifications.
 * If an implementation provides such specialized semantics then the
 * implementation must document those semantics.
 *
 * F.
 * <p>Note that {@code Condition} instances are just normal objects and can
 * themselves be used as the target in a {@code synchronized} statement,
 * and can have their own monitor {@link Object#wait wait} and
 * {@link Object#notify notification} methods invoked.
 * Acquiring the monitor lock of a {@code Condition} instance, or using its
 * monitor methods, has no specified relationship with acquiring the
 * {@link Lock} associated with that {@code Condition} or the use of its
 * {@linkplain #await waiting} and {@linkplain #signal signalling} methods.
 * It is recommended that to avoid confusion you never use {@code Condition}
 * instances in this way, except perhaps within their own implementation.
 *
 * G.
 * <p>Except where noted, passing a {@code null} value for any parameter
 * will result in a {@link NullPointerException} being thrown.
 *
 * <h3>Implementation Considerations</h3>
 *
 * H.
 * <p>When waiting upon a {@code Condition}, a &quot;<em>spurious
 * wakeup</em>&quot; is permitted to occur, in
 * general, as a concession to the underlying platform semantics.
 * This has little practical impact on most application programs as a
 * {@code Condition} should always be waited upon in a loop, testing
 * the state predicate that is being waited for.  An implementation is
 * free to remove the possibility of spurious wakeups but it is
 * recommended that applications programmers always assume that they can
 * occur and so always wait in a loop.
 *
 * I.
 * <p>The three forms of condition waiting
 * (interruptible, non-interruptible, and timed) may differ in their ease of
 * implementation on some platforms and in their performance characteristics.
 * In particular, it may be difficult to provide these features and maintain
 * specific semantics such as ordering guarantees.
 * Further, the ability to interrupt the actual suspension of the thread may
 * not always be feasible to implement on all platforms.
 *
 * J.
 * <p>Consequently, an implementation is not required to define exactly the
 * same guarantees or semantics for all three forms of waiting, nor is it
 * required to support interruption of the actual suspension of the thread.
 *
 * K.
 * <p>An implementation is required to
 * clearly document the semantics and guarantees provided by each of the
 * waiting methods, and when an implementation does support interruption of
 * thread suspension then it must obey the interruption semantics as defined
 * in this interface.
 *
 * L.
 * <p>As interruption generally implies cancellation, and checks for
 * interruption are often infrequent, an implementation can favor responding
 * to an interrupt over normal method return. This is true even if it can be
 * shown that the interrupt occurred after another action that may have
 * unblocked the thread. An implementation should document this behavior.
 *
 * @since 1.5
 * @author Doug Lea
 */
public interface Condition {

    /**
     * 20210725
     * A. 导致当前线程等待，直到收到信号或{@linkplain Thread#interrupt interrupted}。
     * B. 与此{@code Condition}关联的锁被自动释放，当前线程出于线程调度目的而被禁用并处于休眠状态，直到发生以下四种情况之一：
     *      a. 其他一些线程为此{@code Condition} 调用{@link #signal} 方法，并且当前线程恰好被选为要唤醒的线程；
     *      b. 其他一些线程为此 {@code Condition} 调用 {@link #signalAll} 方法；
     *      c. 其他一些线程{@linkplain Thread#interrupt中断}当前线程，支持线程挂起中断；
     *      d. 发生“虚假唤醒”。
     * C. 在所有情况下，在此方法可以返回当前线程之前，必须重新获取与此条件关联的锁。 当线程返回时，它保证持有这个锁。
     * D. 如果当前线程：
     *      a. 在进入此方法时设置其中断状态；
     *      b. 是{@linkplain Thread#interrupt interrupted}，同时支持线程暂停和中断;
     *    然后抛出 {@link InterruptedException} 并清除当前线程的中断状态。 对于第一种情况，在释放锁之前是否进行中断测试没有规定。
     *
     * 实施注意事项
     * E. 调用此方法时，假定当前线程持有与此{@code Condition}关联的锁。由实施来确定是否是这种情况，如果不是，则如何响应。
     *    通常，会抛出异常（例如 {@link IllegalMonitorStateException}）并且实现必须记录该事实。
     * F. 与响应信号的正常方法返回相比，实现更倾向于响应中断。 在这种情况下，实现必须确保信号被重定向到另一个等待线程，如果有的话。
     */
    /**
     * A.
     * Causes the current thread to wait until it is signalled or
     * {@linkplain Thread#interrupt interrupted}.
     *
     * B.
     * <p>The lock associated with this {@code Condition} is atomically
     * released and the current thread becomes disabled for thread scheduling
     * purposes and lies dormant until <em>one</em> of four things happens:
     * <ul>
     * <li>Some other thread invokes the {@link #signal} method for this
     * {@code Condition} and the current thread happens to be chosen as the
     * thread to be awakened; or
     * <li>Some other thread invokes the {@link #signalAll} method for this
     * {@code Condition}; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts} the
     * current thread, and interruption of thread suspension is supported; or
     * <li>A &quot;<em>spurious wakeup</em>&quot; occurs.
     * </ul>
     *
     * C.
     * <p>In all cases, before this method can return the current thread must
     * re-acquire the lock associated with this condition. When the
     * thread returns it is <em>guaranteed</em> to hold this lock.
     *
     * D.
     * <p>If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@linkplain Thread#interrupt interrupted} while waiting
     * and interruption of thread suspension is supported,
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared. It is not specified, in the first
     * case, whether or not the test for interruption occurs before the lock
     * is released.
     *
     * <p><b>Implementation Considerations</b>
     *
     * E.
     * <p>The current thread is assumed to hold the lock associated with this
     * {@code Condition} when this method is called.
     * It is up to the implementation to determine if this is
     * the case and if not, how to respond. Typically, an exception will be
     * thrown (such as {@link IllegalMonitorStateException}) and the
     * implementation must document that fact.
     *
     * F.
     * <p>An implementation can favor responding to an interrupt over normal
     * method return in response to a signal. In that case the implementation
     * must ensure that the signal is redirected to another waiting thread, if
     * there is one.
     *
     * // InterruptedException 如果当前线程被中断（并且支持线程挂起中断）
     * @throws InterruptedException if the current thread is interrupted
     *         (and interruption of thread suspension is supported)
     */
    // 当前线程阻塞等待, 直到收到信号或{@linkplain Thread#interrupt interrupted}, 在可以返回当前线程之前, 必须重新获取与此Condition关联的锁
    void await() throws InterruptedException;

    /**
     * 20210725
     * A. 导致当前线程等待，直到它被发出信号。
     * B. 与此条件相关联的锁被自动释放，当前线程因线程调度目的而被禁用并处于休眠状态，直到发生以下三种情况之一：
     *      a. 其他一些线程为此{@code Condition} 调用{@link #signal} 方法，并且当前线程恰好被选为要唤醒的线程；
     *      b. 其他一些线程为此 {@code Condition} 调用 {@link #signalAll} 方法；
     *      c. 发生“虚假唤醒”。
     * C. 在所有情况下，在此方法可以返回当前线程之前，必须重新获取与此条件关联的锁。 当线程返回时，它保证持有这个锁。
     * D. 如果当前线程在进入该方法时被设置为中断状态，或者在等待过程中被{@linkplain Thread#interrupt interrupted}，则将继续等待直到有signalled。
     *    当它最终从这个方法返回时，它的中断状态仍将被设置。
     *
     * 实施注意事项
     * E. 调用此方法时，假定当前线程持有与此{@code Condition}关联的锁。由实施来确定是否是这种情况，如果不是，则如何响应。
     *    通常，会抛出异常（例如 {@link IllegalMonitorStateException}）并且实现必须记录该事实。
     */
    /**
     * A.
     * Causes the current thread to wait until it is signalled.
     *
     * B.
     * <p>The lock associated with this condition is atomically
     * released and the current thread becomes disabled for thread scheduling
     * purposes and lies dormant until <em>one</em> of three things happens:
     * <ul>
     * <li>Some other thread invokes the {@link #signal} method for this
     * {@code Condition} and the current thread happens to be chosen as the
     * thread to be awakened; or
     * <li>Some other thread invokes the {@link #signalAll} method for this
     * {@code Condition}; or
     * <li>A &quot;<em>spurious wakeup</em>&quot; occurs.
     * </ul>
     *
     * C.
     * <p>In all cases, before this method can return the current thread must
     * re-acquire the lock associated with this condition. When the
     * thread returns it is <em>guaranteed</em> to hold this lock.
     *
     * D.
     * <p>If the current thread's interrupted status is set when it enters
     * this method, or it is {@linkplain Thread#interrupt interrupted}
     * while waiting, it will continue to wait until signalled. When it finally
     * returns from this method its interrupted status will still
     * be set.
     *
     * <p><b>Implementation Considerations</b>
     *
     * E.
     * <p>The current thread is assumed to hold the lock associated with this
     * {@code Condition} when this method is called.
     * It is up to the implementation to determine if this is
     * the case and if not, how to respond. Typically, an exception will be
     * thrown (such as {@link IllegalMonitorStateException}) and the
     * implementation must document that fact.
     */
    // 当前线程阻塞等待, 直到收到信号(如果期间有中断会先继续等待, 在返回后再重新设置回中断标记位), 在可以返回当前线程之前, 必须重新获取与此Condition关联的锁
    void awaitUninterruptibly();

    /**
     * 20210725
     * A. 使当前线程等待，直到它被发出信号或被中断，或者指定的等待时间过去。
     * B. 与此条件关联的锁被自动释放，当前线程因线程调度目的而被禁用并处于休眠状态，直到发生以下五件事之一：
     *      a. 其他一些线程为此{@code Condition} 调用{@link #signal} 方法，并且当前线程恰好被选为要唤醒的线程；
     *      b. 其他一些线程为此 {@code Condition} 调用 {@link #signalAll} 方法；
     *      c. 其他一些线程{@linkplain Thread#interrupt}中断当前线程，支持线程挂起中断；
     *      d. 指定的等待时间过去；
     *      e. 发生“虚假唤醒”。
     * C. 在所有情况下，在此方法可以返回当前线程之前，必须重新获取与此条件关联的锁。 当线程返回时，它保证持有这个锁。
     * D. 如果当前线程：
     *      a. 在进入此方法时设置其中断状态；
     *      b. 是{@linkplain Thread#interrupt interrupted}，同时支持线程暂停和中断。
     *    然后抛出 {@link InterruptedException} 并清除当前线程的中断状态。 对于第一种情况，在释放锁之前是否进行中断测试没有规定。
     * E. 该方法返回给定返回时提供的 {@code nanosTimeout} 值的剩余等待纳秒数的估计值，或者如果超时则返回小于或等于0的值。
     *    此值可用于确定在等待返回但等待条件仍然不成立的情况下是否重新等待以及重新等待多长时间。 此方法的典型用途采用以下形式：
     * {@code
     * boolean aMethod(long timeout, TimeUnit unit) {
     *   long nanos = unit.toNanos(timeout);
     *   lock.lock();
     *   try {
     *     while (!conditionBeingWaitedFor()) {
     *       if (nanos <= 0L)
     *         return false;
     *       nanos = theCondition.awaitNanos(nanos);
     *     }
     *     // ...
     *   } finally {
     *     lock.unlock();
     *   }
     * }}
     * F. 设计说明：此方法需要纳秒参数，以避免报告剩余时间时出现截断错误。 这种精度损失将使程序员难以确保总等待时间不会系统地短于重新等待发生时的指定时间。
     *
     * 实施注意事项
     * G. 调用此方法时，假定当前线程持有与此 {@code Condition} 关联的锁。 由实施来确定是否是这种情况，如果不是，则如何响应。
     *    通常，会抛出异常（例如 {@link IllegalMonitorStateException}）并且实现必须记录该事实。
     * H. 一个实现可以倾向于响应中断而不是响应信号的正常方法返回，或者超过指示指定等待时间的过去。 在任何一种情况下，实现都必须确保将信号重定向到另一个等待线程（如果有）。
     */
    /**
     * A.
     * Causes the current thread to wait until it is signalled or interrupted,
     * or the specified waiting time elapses.
     *
     * B.
     * <p>The lock associated with this condition is atomically
     * released and the current thread becomes disabled for thread scheduling
     * purposes and lies dormant until <em>one</em> of five things happens:
     * <ul>
     * <li>Some other thread invokes the {@link #signal} method for this
     * {@code Condition} and the current thread happens to be chosen as the
     * thread to be awakened; or
     * <li>Some other thread invokes the {@link #signalAll} method for this
     * {@code Condition}; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts} the
     * current thread, and interruption of thread suspension is supported; or
     * <li>The specified waiting time elapses; or
     * <li>A &quot;<em>spurious wakeup</em>&quot; occurs.
     * </ul>
     *
     * C.
     * <p>In all cases, before this method can return the current thread must
     * re-acquire the lock associated with this condition. When the
     * thread returns it is <em>guaranteed</em> to hold this lock.
     *
     * D.
     * <p>If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@linkplain Thread#interrupt interrupted} while waiting
     * and interruption of thread suspension is supported,
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared. It is not specified, in the first
     * case, whether or not the test for interruption occurs before the lock
     * is released.
     *
     * E.
     * <p>The method returns an estimate of the number of nanoseconds
     * remaining to wait given the supplied {@code nanosTimeout}
     * value upon return, or a value less than or equal to zero if it
     * timed out. This value can be used to determine whether and how
     * long to re-wait in cases where the wait returns but an awaited
     * condition still does not hold. Typical uses of this method take
     * the following form:
     *
     *  <pre> {@code
     * boolean aMethod(long timeout, TimeUnit unit) {
     *   long nanos = unit.toNanos(timeout);
     *   lock.lock();
     *   try {
     *     while (!conditionBeingWaitedFor()) {
     *       if (nanos <= 0L)
     *         return false;
     *       nanos = theCondition.awaitNanos(nanos);
     *     }
     *     // ...
     *   } finally {
     *     lock.unlock();
     *   }
     * }}</pre>
     *
     * F.
     * <p>Design note: This method requires a nanosecond argument so
     * as to avoid truncation errors in reporting remaining times.
     * Such precision loss would make it difficult for programmers to
     * ensure that total waiting times are not systematically shorter
     * than specified when re-waits occur.
     *
     * <p><b>Implementation Considerations</b>
     *
     * G.
     * <p>The current thread is assumed to hold the lock associated with this
     * {@code Condition} when this method is called.
     * It is up to the implementation to determine if this is
     * the case and if not, how to respond. Typically, an exception will be
     * thrown (such as {@link IllegalMonitorStateException}) and the
     * implementation must document that fact.
     *
     * H.
     * <p>An implementation can favor responding to an interrupt over normal
     * method return in response to a signal, or over indicating the elapse
     * of the specified waiting time. In either case the implementation
     * must ensure that the signal is redirected to another waiting thread, if
     * there is one.
     *
     * @param nanosTimeout the maximum time to wait, in nanoseconds
     *
     * // {@code nanosTimeout}值减去等待此方法返回所花费的时间的估计值。可以使用正值作为对该方法的后续调用的参数，以完成等待所需的时间。 小于或等于零的值表示没有剩余时间。
     * @return an estimate of the {@code nanosTimeout} value minus
     *         the time spent waiting upon return from this method.
     *         A positive value may be used as the argument to a
     *         subsequent call to this method to finish waiting out
     *         the desired time.  A value less than or equal to zero
     *         indicates that no time remains.
     * @throws InterruptedException if the current thread is interrupted
     *         (and interruption of thread suspension is supported)
     */
    // 当前线程阻塞等待, 直到收到信号、中断或者指定的等待时间过去, 在可以返回当前线程之前, 必须重新获取与此Condition关联的锁
    long awaitNanos(long nanosTimeout) throws InterruptedException;

    /**
     * 20210725
     * 使当前线程等待，直到它被发出信号或被中断，或者指定的等待时间过去。 此方法在行为上等效于：
     *  {@code awaitNanos(unit.toNanos(time)) > 0}
     */
    /**
     * Causes the current thread to wait until it is signalled or interrupted,
     * or the specified waiting time elapses. This method is behaviorally
     * equivalent to:
     *  <pre> {@code awaitNanos(unit.toNanos(time)) > 0}</pre>
     *
     * @param time the maximum time to wait
     * @param unit the time unit of the {@code time} argument
     *
     * // {@code false} 如果在从方法返回之前可检测到等待时间已经过去，否则 {@code true}
     * @return {@code false} if the waiting time detectably elapsed
     *         before return from the method, else {@code true}
     * @throws InterruptedException if the current thread is interrupted
     *         (and interruption of thread suspension is supported)
     */
    // 等价于{@code awaitNanos(unit.toNanos(time)) > 0}, 当前线程阻塞等待, 直到收到信号、中断或者指定的等待时间过去, 在可以返回当前线程之前, 必须重新获取与此Condition关联的锁
    boolean await(long time, TimeUnit unit) throws InterruptedException;

    /**
     * 20210725
     * A. 导致当前线程等待，直到它被发出信号或被中断，或者指定的截止日期过去。
     * B. 与此条件关联的锁被自动释放，当前线程因线程调度目的而被禁用并处于休眠状态，直到发生以下五件事之一：
     *      a. 其他一些线程为此{@code Condition} 调用{@link #signal} 方法，并且当前线程恰好被选为要唤醒的线程；
     *      b. 其他一些线程为此 {@code Condition} 调用 {@link #signalAll} 方法；
     *      c. 其他一些线程{@linkplain Thread#interrupt中断}当前线程，支持线程挂起中断；
     *      d. 指定的截止日期已过；
     *      e. 发生“虚假唤醒”。
     * C. 在所有情况下，在此方法可以返回当前线程之前，必须重新获取与此条件关联的锁。 当线程返回时，它保证持有这个锁。
     * D. 如果当前线程：
     *      a. 在进入此方法时设置其中断状态；
     *      b. 是{@linkplain Thread#interrupt interrupted}，同时支持线程暂停和中断;
     *    然后抛出 {@link InterruptedException} 并清除当前线程的中断状态。 对于第一种情况，在释放锁之前是否进行中断测试没有规定。
     * E. 返回值表示是否超过了截止时间，可以如下使用：
     * {@code
     * boolean aMethod(Date deadline) {
     *   boolean stillWaiting = true;
     *   lock.lock();
     *   try {
     *     while (!conditionBeingWaitedFor()) {
     *       if (!stillWaiting)
     *         return false;
     *       stillWaiting = theCondition.awaitUntil(deadline);
     *     }
     *     // ...
     *   } finally {
     *     lock.unlock();
     *   }
     * }}
     *
     * 实施注意事项
     * F. 调用此方法时，假定当前线程持有与此 {@code Condition} 关联的锁。 由实施来确定是否是这种情况，如果不是，则如何响应。
     *    通常，会抛出异常（例如 {@link IllegalMonitorStateException}）并且实现必须记录该事实。
     * G. 一个实现可以倾向于响应中断而不是响应信号的正常方法返回，或者超过指示指定期限的过去。 在任何一种情况下，实现都必须确保将信号重定向到另一个等待线程（如果有）。
     */
    /**
     * A.
     * Causes the current thread to wait until it is signalled or interrupted,
     * or the specified deadline elapses.
     *
     * B.
     * <p>The lock associated with this condition is atomically
     * released and the current thread becomes disabled for thread scheduling
     * purposes and lies dormant until <em>one</em> of five things happens:
     * <ul>
     * <li>Some other thread invokes the {@link #signal} method for this
     * {@code Condition} and the current thread happens to be chosen as the
     * thread to be awakened; or
     * <li>Some other thread invokes the {@link #signalAll} method for this
     * {@code Condition}; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts} the
     * current thread, and interruption of thread suspension is supported; or
     * <li>The specified deadline elapses; or
     * <li>A &quot;<em>spurious wakeup</em>&quot; occurs.
     * </ul>
     *
     * C.
     * <p>In all cases, before this method can return the current thread must
     * re-acquire the lock associated with this condition. When the
     * thread returns it is <em>guaranteed</em> to hold this lock.
     *
     * D.
     * <p>If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@linkplain Thread#interrupt interrupted} while waiting
     * and interruption of thread suspension is supported,
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared. It is not specified, in the first
     * case, whether or not the test for interruption occurs before the lock
     * is released.
     *
     * E.
     * <p>The return value indicates whether the deadline has elapsed,
     * which can be used as follows:
     *  <pre> {@code
     * boolean aMethod(Date deadline) {
     *   boolean stillWaiting = true;
     *   lock.lock();
     *   try {
     *     while (!conditionBeingWaitedFor()) {
     *       if (!stillWaiting)
     *         return false;
     *       stillWaiting = theCondition.awaitUntil(deadline);
     *     }
     *     // ...
     *   } finally {
     *     lock.unlock();
     *   }
     * }}</pre>
     *
     * <p><b>Implementation Considerations</b>
     *
     * F.
     * <p>The current thread is assumed to hold the lock associated with this
     * {@code Condition} when this method is called.
     * It is up to the implementation to determine if this is
     * the case and if not, how to respond. Typically, an exception will be
     * thrown (such as {@link IllegalMonitorStateException}) and the
     * implementation must document that fact.
     *
     * G.
     * <p>An implementation can favor responding to an interrupt over normal
     * method return in response to a signal, or over indicating the passing
     * of the specified deadline. In either case the implementation
     * must ensure that the signal is redirected to another waiting thread, if
     * there is one.
     *
     * @param deadline the absolute time to wait until
     * @return {@code false} if the deadline has elapsed upon return, else
     *         {@code true}
     * @throws InterruptedException if the current thread is interrupted
     *         (and interruption of thread suspension is supported)
     */
    // 当前线程阻塞等待, 直到收到信号、中断或者指定的截止日期过去, 在可以返回当前线程之前, 必须重新获取与此Condition关联的锁
    boolean awaitUntil(Date deadline) throws InterruptedException;

    /**
     * 20210523
     * A. 唤醒一个等待线程。
     * B. 如果有任何线程在这种情况下等待，则选择一个线程进行唤醒。 然后，该线程必须重新获取锁，然后才能从{@code await}返回。
     * C. 实施注意事项: 当调用此方法时，实现可能（并且通常确实）要求当前线程持有与此{@code Condition}关联的锁。 实现必须记录此前提条件，以及如果未持有该锁，
     *    则应采取的任何措施。 通常，将引发诸如{@link IllegalMonitorStateException}之类的异常。
     */
    /**
     * A.
     * Wakes up one waiting thread.
     *
     * B.
     * <p>If any threads are waiting on this condition then one
     * is selected for waking up. That thread must then re-acquire the
     * lock before returning from {@code await}.
     *
     * C.
     * <p><b>Implementation Considerations</b>
     * <p>An implementation may (and typically does) require that the
     * current thread hold the lock associated with this {@code
     * Condition} when this method is called. Implementations must
     * document this precondition and any actions taken if the lock is
     * not held. Typically, an exception such as {@link
     * IllegalMonitorStateException} will be thrown.
     */
    // 随机唤醒一个等待的线程
    void signal();

    /**
     * 20210725
     * A. 唤醒所有等待的线程。
     * B. 如果任何线程正在等待这种情况，那么它们都会被唤醒。 每个线程必须重新获取锁才能从 {@code await} 返回。
     * C. 实施注意事项: 当调用此方法时，实现可能（并且通常确实）要求当前线程持有与此 {@code Condition} 关联的锁。 实现必须记录此先决条件以及在未持有锁时采取的任何操作。
     *    通常，会抛出诸如 {@link IllegalMonitorStateException} 之类的异常。
     */
    /**
     * A.
     * Wakes up all waiting threads.
     *
     * B.
     * <p>If any threads are waiting on this condition then they are
     * all woken up. Each thread must re-acquire the lock before it can
     * return from {@code await}.
     *
     * <p><b>Implementation Considerations</b>
     *
     * C.
     * <p>An implementation may (and typically does) require that the
     * current thread hold the lock associated with this {@code
     * Condition} when this method is called. Implementations must
     * document this precondition and any actions taken if the lock is
     * not held. Typically, an exception such as {@link
     * IllegalMonitorStateException} will be thrown.
     */
    // 唤醒所有等待的线程
    void signalAll();
}
