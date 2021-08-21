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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import sun.misc.Unsafe;

/**
 * 20210722
 * A. 提供一个框架，用于实现依赖先进先出 (FIFO) 等待队列的阻塞锁和相关同步器（信号量、事件等）。 此类旨在成为大多数依赖单个原子 {@code int} 值来表示状态的同步器的有用基础。
 *    子类必须定义更改此状态的受保护方法，并定义该状态在获取或释放此对象方面的含义。 鉴于这些，此类中的其他方法执行所有排队和阻塞机制。 子类可以维护其他状态字段，
 *    但只有使用方法 {@link #getState}、{@link #setState} 和 {@link #compareAndSetState} 操作的原子更新的 {@code int} 值才会在同步方面进行跟踪。
 * B. 子类应定义为非公共内部帮助类，用于实现其封闭类的同步属性。 {@code AbstractQueuedSynchronizer} 类没有实现任何同步接口。 相反，它定义了诸如
 *    {@link #acquireInterruptably} 之类的方法，这些方法可以由具体锁和相关同步器适当调用以实现它们的公共方法。
 * C. 此类支持默认独占模式和共享模式中的一种或两种。 当以独占模式获取时，其他线程尝试获取不会成功。 多个线程获取的共享模式可能（但不一定）成功。 这个类不“理解”这些差异，
 *    除了机械意义上的区别，当共享模式获取成功时，下一个等待线程（如果存在）也必须确定它是否也可以获取。 在不同模式下等待的线程共享同一个 FIFO 队列。
 *    通常，实现子类仅支持这些模式中的一种，但两种模式都可以发挥作用，例如在 {@link ReadWriteLock} 中。 仅支持独占或仅共享模式的子类不需要定义支持未使用模式的方法。
 * D. 该类定义了一个嵌套的 {@link ConditionObject} 类，该类可以被支持独占模式的子类用作 {@link Condition} 实现，其中方法 {@link #isHeldExclusively}
 *    报告是否针对当前线程独占同步， 使用当前 {@link #getState} 值调用的方法完全释放此对象，并且 {@link #acquire} 给定此保存的状态值，最终将此对象恢复到其先前获取的状态。
 *    没有 {@code AbstractQueuedSynchronizer} 方法否则会创建这样的条件，因此如果无法满足此约束，请不要使用它。
 *    {@link ConditionObject} 的行为当然取决于其同步器实现的语义。
 * E. 此类为内部队列提供检查、检测和监视方法，以及为ConditionObject提供类似方法。 这些可以根据需要导出到类中，使用 {@code AbstractQueuedSynchronizer} 作为它们的同步机制。
 * F. 此类的序列化仅存储底层原子整数维护状态，因此反序列化对象具有空线程队列。 需要可序列化的典型子类将定义一个 {@code readObject} 方法，该方法在反序列化时将其恢复到已知的初始状态。
 *
 * 用法
 * G. 要将此类用作同步器的基础，请根据适用情况重新定义以下方法，方法是使用 {@link #getState}、{@link #setState} 和/或 {@link #compareAndSetState 检查和/或修改同步状态}：
 *      {@link #tryAcquire}
 *      {@link #tryRelease}
 *      {@link #tryAcquireShared}
 *      {@link #tryReleaseShared}
 *      {@link #isHeldExclusively}
 * H. 默认情况下，这些方法中的每一个都会抛出 {@link UnsupportedOperationException}。 这些方法的实现必须是内部线程安全的，并且通常应该是简短的而不是阻塞的。
 *    定义这些方法是使用此类的唯一支持方式。 所有其他方法都声明为 {@code final}，因为它们不能独立变化。
 * I. 您可能还会发现 {@link AbstractOwnableSynchronizer} 的继承方法对于跟踪拥有独占同步器的线程很有用。 鼓励您使用它们——
 *    这使监视和诊断工具能够帮助用户确定哪些线程持有锁。
 * J. 即使此类基于内部 FIFO 队列，它也不会自动执行 FIFO 采集策略。 独占同步的核心形式为：
 *      Acquire:
 *          while (!tryAcquire(arg)) {
 *              enqueue thread if it is not already queued;
 *              possibly block current thread;
 *          }
 *
 *      Release:
 *          if (tryRelease(arg))
 *              unblock the first queued thread;
 * K. （共享模式类似，但可能涉及级联信号。）因为在入队之前调用获取中的检查，所以新的获取线程可能会抢在其他被阻塞和排队的线程之前。 但是，如果需要，您可以定义
 *     {@code tryAcquire} 和/或 {@code tryAcquireShared} 以通过内部调用一种或多种检查方法来禁用插入，从而提供公平的 FIFO 获取顺序。 特别是，如果
 *     {@link #hasQueuedPredecessors}（一种专门设计用于公平同步器使用的方法）返回 {@code true}，大多数公平同步器可以定义 {@code tryAcquire} 以返回 {@code false}。
 *     其他变化也是可能的。
 * L. 默认插入（也称为贪婪、放弃和避免护送）策略的吞吐量和可扩展性通常最高。 虽然这不能保证公平或无饥饿，但允许较早的排队线程在较晚的排队线程之前重新竞争，
 *    并且每次重新竞争都有机会成功对抗传入的线程。 此外，虽然获取在通常意义上不会“旋转”，但它们可能会执行多次调用 {@code tryAcquire}，并在阻塞之前穿插其他计算。
 *    当仅短暂保持独占同步时，这提供了自旋的大部分好处，而在不保持时则没有大部分责任。 如果需要，您可以通过使用“快速路径”检查预先调用获取方法来增强这一点，
 *    可能预先检查 {@link #hasContended} 和/或 {@link #hasQueuedThreads} 仅在同步器可能不这样做时才这样做被争夺。
 * M. 此类通过将其使用范围专门用于可以依赖 {@code int} 状态、获取和释放参数以及内部 FIFO 等待队列的同步器，为同步提供了高效且可扩展的基础。 如果这还不够，
 *    您可以使用 {@link java.util.concurrent.atomic atomic} 类、您自己的自定义 {@link java.util.Queue} 类和 {@link LockSupport} 阻塞从较低级别构建同步器支持。
 *
 * 使用示例
 * N. 这里是一个不可重入的互斥锁类，它使用值0表示解锁状态，使用值1表示锁定状态。虽然不可重入锁并不严格要求记录当前所有者线程，但这个类无论如何这样做是为了使使用更容易监控。
 *    它还支持条件并公开一种检测方法：
 * {@code
 * class Mutex implements Lock, java.io.Serializable {
 *
 *   // Our internal helper class
 *   private static class Sync extends AbstractQueuedSynchronizer {
 *     // Reports whether in locked state
 *     protected boolean isHeldExclusively() {
 *       return getState() == 1;
 *     }
 *
 *     // Acquires the lock if state is zero
 *     public boolean tryAcquire(int acquires) {
 *       assert acquires == 1; // Otherwise unused
 *       if (compareAndSetState(0, 1)) {
 *         setExclusiveOwnerThread(Thread.currentThread());
 *         return true;
 *       }
 *       return false;
 *     }
 *
 *     // Releases the lock by setting state to zero
 *     protected boolean tryRelease(int releases) {
 *       assert releases == 1; // Otherwise unused
 *       if (getState() == 0) throw new IllegalMonitorStateException();
 *       setExclusiveOwnerThread(null);
 *       setState(0);
 *       return true;
 *     }
 *
 *     // Provides a Condition
 *     Condition newCondition() { return new ConditionObject(); }
 *
 *     // Deserializes properly
 *     private void readObject(ObjectInputStream s)
 *         throws IOException, ClassNotFoundException {
 *       s.defaultReadObject();
 *       setState(0); // reset to unlocked state
 *     }
 *   }
 *
 *   // The sync object does all the hard work. We just forward to it.
 *   private final Sync sync = new Sync();
 *
 *   public void lock()                { sync.acquire(1); }
 *   public boolean tryLock()          { return sync.tryAcquire(1); }
 *   public void unlock()              { sync.release(1); }
 *   public Condition newCondition()   { return sync.newCondition(); }
 *   public boolean isLocked()         { return sync.isHeldExclusively(); }
 *   public boolean hasQueuedThreads() { return sync.hasQueuedThreads(); }
 *   public void lockInterruptibly() throws InterruptedException {
 *     sync.acquireInterruptibly(1);
 *   }
 *   public boolean tryLock(long timeout, TimeUnit unit)
 *       throws InterruptedException {
 *     return sync.tryAcquireNanos(1, unit.toNanos(timeout));
 *   }
 * }}
 *
 * O. 这是一个类似于 {@link java.util.concurrent.CountDownLatch CountDownLatch} 的闩锁类，只是它只需要一个{@code 信号} 即可触发。因为锁存器是非独占的，
 *    所以它使用 {@code shared} 获取和释放方法:
 * {@code
 * class BooleanLatch {
 *
 *   private static class Sync extends AbstractQueuedSynchronizer {
 *     boolean isSignalled() { return getState() != 0; }
 *
 *     protected int tryAcquireShared(int ignore) {
 *       return isSignalled() ? 1 : -1;
 *     }
 *
 *     protected boolean tryReleaseShared(int ignore) {
 *       setState(1);
 *       return true;
 *     }
 *   }
 *
 *   private final Sync sync = new Sync();
 *   public boolean isSignalled() { return sync.isSignalled(); }
 *   public void signal()         { sync.releaseShared(1); }
 *   public void await() throws InterruptedException {
 *     sync.acquireSharedInterruptibly(1);
 *   }
 * }}
 */
/**
 * A.
 * Provides a framework for implementing blocking locks and related
 * synchronizers (semaphores, events, etc) that rely on
 * first-in-first-out (FIFO) wait queues.  This class is designed to
 * be a useful basis for most kinds of synchronizers that rely on a
 * single atomic {@code int} value to represent state. Subclasses
 * must define the protected methods that change this state, and which
 * define what that state means in terms of this object being acquired
 * or released.  Given these, the other methods in this class carry
 * out all queuing and blocking mechanics. Subclasses can maintain
 * other state fields, but only the atomically updated {@code int}
 * value manipulated using methods {@link #getState}, {@link
 * #setState} and {@link #compareAndSetState} is tracked with respect
 * to synchronization.
 *
 * B.
 * <p>Subclasses should be defined as non-public internal helper
 * classes that are used to implement the synchronization properties
 * of their enclosing class.  Class
 * {@code AbstractQueuedSynchronizer} does not implement any
 * synchronization interface.  Instead it defines methods such as
 * {@link #acquireInterruptibly} that can be invoked as
 * appropriate by concrete locks and related synchronizers to
 * implement their public methods.
 *
 * C.
 * <p>This class supports either or both a default <em>exclusive</em>
 * mode and a <em>shared</em> mode. When acquired in exclusive mode,
 * attempted acquires by other threads cannot succeed. Shared mode
 * acquires by multiple threads may (but need not) succeed. This class
 * does not &quot;understand&quot; these differences except in the
 * mechanical sense that when a shared mode acquire succeeds, the next
 * waiting thread (if one exists) must also determine whether it can
 * acquire as well. Threads waiting in the different modes share the
 * same FIFO queue. Usually, implementation subclasses support only
 * one of these modes, but both can come into play for example in a
 * {@link ReadWriteLock}. Subclasses that support only exclusive or
 * only shared modes need not define the methods supporting the unused mode.
 *
 * D.
 * <p>This class defines a nested {@link ConditionObject} class that
 * can be used as a {@link Condition} implementation by subclasses
 * supporting exclusive mode for which method {@link
 * #isHeldExclusively} reports whether synchronization is exclusively
 * held with respect to the current thread, method {@link #release}
 * invoked with the current {@link #getState} value fully releases
 * this object, and {@link #acquire}, given this saved state value,
 * eventually restores this object to its previous acquired state.  No
 * {@code AbstractQueuedSynchronizer} method otherwise creates such a
 * condition, so if this constraint cannot be met, do not use it.  The
 * behavior of {@link ConditionObject} depends of course on the
 * semantics of its synchronizer implementation.
 *
 * E.
 * <p>This class provides inspection, instrumentation, and monitoring
 * methods for the internal queue, as well as similar methods for
 * condition objects. These can be exported as desired into classes
 * using an {@code AbstractQueuedSynchronizer} for their
 * synchronization mechanics.
 *
 * F.
 * <p>Serialization of this class stores only the underlying atomic
 * integer maintaining state, so deserialized objects have empty
 * thread queues. Typical subclasses requiring serializability will
 * define a {@code readObject} method that restores this to a known
 * initial state upon deserialization.
 *
 * <h3>Usage</h3>
 *
 * G.
 * <p>To use this class as the basis of a synchronizer, redefine the
 * following methods, as applicable, by inspecting and/or modifying
 * the synchronization state using {@link #getState}, {@link
 * #setState} and/or {@link #compareAndSetState}:
 *
 * <ul>
 * <li> {@link #tryAcquire}
 * <li> {@link #tryRelease}
 * <li> {@link #tryAcquireShared}
 * <li> {@link #tryReleaseShared}
 * <li> {@link #isHeldExclusively}
 * </ul>
 *
 * H.
 * Each of these methods by default throws {@link
 * UnsupportedOperationException}.  Implementations of these methods
 * must be internally thread-safe, and should in general be short and
 * not block. Defining these methods is the <em>only</em> supported
 * means of using this class. All other methods are declared
 * {@code final} because they cannot be independently varied.
 *
 * I.
 * <p>You may also find the inherited methods from {@link
 * AbstractOwnableSynchronizer} useful to keep track of the thread
 * owning an exclusive synchronizer.  You are encouraged to use them
 * -- this enables monitoring and diagnostic tools to assist users in
 * determining which threads hold locks.
 *
 * J.
 * <p>Even though this class is based on an internal FIFO queue, it
 * does not automatically enforce FIFO acquisition policies.  The core
 * of exclusive synchronization takes the form:
 *
 * <pre>
 * Acquire:
 *     while (!tryAcquire(arg)) {
 *        <em>enqueue thread if it is not already queued</em>;
 *        <em>possibly block current thread</em>;
 *     }
 *
 * Release:
 *     if (tryRelease(arg))
 *        <em>unblock the first queued thread</em>;
 * </pre>
 *
 * K.
 * (Shared mode is similar but may involve cascading signals.)
 *
 * <p id="barging">Because checks in acquire are invoked before
 * enqueuing, a newly acquiring thread may <em>barge</em> ahead of
 * others that are blocked and queued.  However, you can, if desired,
 * define {@code tryAcquire} and/or {@code tryAcquireShared} to
 * disable barging by internally invoking one or more of the inspection
 * methods, thereby providing a <em>fair</em> FIFO acquisition order.
 * In particular, most fair synchronizers can define {@code tryAcquire}
 * to return {@code false} if {@link #hasQueuedPredecessors} (a method
 * specifically designed to be used by fair synchronizers) returns
 * {@code true}.  Other variations are possible.
 *
 * L.
 * <p>Throughput and scalability are generally highest for the
 * default barging (also known as <em>greedy</em>,
 * <em>renouncement</em>, and <em>convoy-avoidance</em>) strategy.
 * While this is not guaranteed to be fair or starvation-free, earlier
 * queued threads are allowed to recontend before later queued
 * threads, and each recontention has an unbiased chance to succeed
 * against incoming threads.  Also, while acquires do not
 * &quot;spin&quot; in the usual sense, they may perform multiple
 * invocations of {@code tryAcquire} interspersed with other
 * computations before blocking.  This gives most of the benefits of
 * spins when exclusive synchronization is only briefly held, without
 * most of the liabilities when it isn't. If so desired, you can
 * augment this by preceding calls to acquire methods with
 * "fast-path" checks, possibly prechecking {@link #hasContended}
 * and/or {@link #hasQueuedThreads} to only do so if the synchronizer
 * is likely not to be contended.
 *
 * M.
 * <p>This class provides an efficient and scalable basis for
 * synchronization in part by specializing its range of use to
 * synchronizers that can rely on {@code int} state, acquire, and
 * release parameters, and an internal FIFO wait queue. When this does
 * not suffice, you can build synchronizers from a lower level using
 * {@link java.util.concurrent.atomic atomic} classes, your own custom
 * {@link java.util.Queue} classes, and {@link LockSupport} blocking
 * support.
 *
 * N.
 * <h3>Usage Examples</h3>
 *
 * <p>Here is a non-reentrant mutual exclusion lock class that uses
 * the value zero to represent the unlocked state, and one to
 * represent the locked state. While a non-reentrant lock
 * does not strictly require recording of the current owner
 * thread, this class does so anyway to make usage easier to monitor.
 * It also supports conditions and exposes
 * one of the instrumentation methods:
 *
 *  <pre> {@code
 * class Mutex implements Lock, java.io.Serializable {
 *
 *   // Our internal helper class
 *   private static class Sync extends AbstractQueuedSynchronizer {
 *     // Reports whether in locked state
 *     protected boolean isHeldExclusively() {
 *       return getState() == 1;
 *     }
 *
 *     // Acquires the lock if state is zero
 *     public boolean tryAcquire(int acquires) {
 *       assert acquires == 1; // Otherwise unused
 *       if (compareAndSetState(0, 1)) {
 *         setExclusiveOwnerThread(Thread.currentThread());
 *         return true;
 *       }
 *       return false;
 *     }
 *
 *     // Releases the lock by setting state to zero
 *     protected boolean tryRelease(int releases) {
 *       assert releases == 1; // Otherwise unused
 *       if (getState() == 0) throw new IllegalMonitorStateException();
 *       setExclusiveOwnerThread(null);
 *       setState(0);
 *       return true;
 *     }
 *
 *     // Provides a Condition
 *     Condition newCondition() { return new ConditionObject(); }
 *
 *     // Deserializes properly
 *     private void readObject(ObjectInputStream s)
 *         throws IOException, ClassNotFoundException {
 *       s.defaultReadObject();
 *       setState(0); // reset to unlocked state
 *     }
 *   }
 *
 *   // The sync object does all the hard work. We just forward to it.
 *   private final Sync sync = new Sync();
 *
 *   public void lock()                { sync.acquire(1); }
 *   public boolean tryLock()          { return sync.tryAcquire(1); }
 *   public void unlock()              { sync.release(1); }
 *   public Condition newCondition()   { return sync.newCondition(); }
 *   public boolean isLocked()         { return sync.isHeldExclusively(); }
 *   public boolean hasQueuedThreads() { return sync.hasQueuedThreads(); }
 *   public void lockInterruptibly() throws InterruptedException {
 *     sync.acquireInterruptibly(1);
 *   }
 *   public boolean tryLock(long timeout, TimeUnit unit)
 *       throws InterruptedException {
 *     return sync.tryAcquireNanos(1, unit.toNanos(timeout));
 *   }
 * }}</pre>
 *
 * O.
 * <p>Here is a latch class that is like a
 * {@link java.util.concurrent.CountDownLatch CountDownLatch}
 * except that it only requires a single {@code signal} to
 * fire. Because a latch is non-exclusive, it uses the {@code shared}
 * acquire and release methods.
 *
 *  <pre> {@code
 * class BooleanLatch {
 *
 *   private static class Sync extends AbstractQueuedSynchronizer {
 *     boolean isSignalled() { return getState() != 0; }
 *
 *     protected int tryAcquireShared(int ignore) {
 *       return isSignalled() ? 1 : -1;
 *     }
 *
 *     protected boolean tryReleaseShared(int ignore) {
 *       setState(1);
 *       return true;
 *     }
 *   }
 *
 *   private final Sync sync = new Sync();
 *   public boolean isSignalled() { return sync.isSignalled(); }
 *   public void signal()         { sync.releaseShared(1); }
 *   public void await() throws InterruptedException {
 *     sync.acquireSharedInterruptibly(1);
 *   }
 * }}</pre>
 *
 * @since 1.5
 * @author Doug Lea
 */
public abstract class AbstractQueuedSynchronizer extends AbstractOwnableSynchronizer implements java.io.Serializable {

    private static final long serialVersionUID = 7373984972572414691L;

    /**
     * Creates a new {@code AbstractQueuedSynchronizer} instance
     * with initial synchronization state of zero.
     */
    // 创建一个新的{@code AbstractQueuedSynchronizer} 实例，初始同步状态为零。
    protected AbstractQueuedSynchronizer() { }

    /**
     * 20210722
     * 等待队列节点类
     * A. 等待队列是“CLH”（Craig、Landin 和 Hagersten）锁定队列的变体。 CLH 锁通常用于自旋锁。 我们改为将它们用于阻塞同步器，但使用相同的基本策略，
     *    即在其节点的前驱中保存有关线程的一些控制信息。 每个节点中的“状态”字段跟踪线程是否应该阻塞。 节点在其前任发布时收到信号。
     *    队列的每个节点都充当一个特定的通知式监视器，持有一个等待线程。 尽管状态字段不控制线程是否被授予锁定等。 一个线程可能会尝试获取它是否在队列中的第一个。
     *    但成为第一并不能保证成功； 它只给予抗争的权利。 所以当前发布的竞争者线程可能需要重新等待。
     * B. 要加入CLH锁，您可以原子地将其拼接为新的尾部。要出列，您只需设置head字段。
     *      +------+  prev +-----+       +-----+
     * head |      | <---- |     | <---- |     |  tail
     *      +------+       +-----+       +-----+
     * C. 插入CLH队列只需要对“tail”进行一次原子操作，因此从未排队到排队有一个简单的原子分界点。类似地，出列只涉及更新“头”。
     *    然而，节点需要做更多的工作来确定他们的继任者是谁，部分是为了处理由于超时和中断可能导致的取消。
     * D. “prev”链接（未在原始CLH锁中使用）主要用于处理取消。如果一个节点被取消，它的后继（通常）会重新链接到一个未取消的前驱。有关自旋锁情况下类似机制的解释，
     *     请参阅 Scott 和 Scherer 的论文，网址为 http://www.cs.rochester.edu/u/scott/synchronization/
     * E. 我们还使用“next”链接来实现阻塞机制。每个节点的线程id保存在它自己的节点中，因此前驱通过遍历下一个链接来确定它是哪个线程来通知下一个节点唤醒。
     *    确定后继节点必须避免与新排队节点竞争以设置其前驱节点的“下一个”字段。当节点的后继节点似乎为空时，通过从原子更新的“尾部”向后检查，在必要时解决此问题。
     *    （或者，换句话说，下一个链接是一种优化，因此我们通常不需要向后扫描。）
     * F. 取消为基本算法引入了一些保守性。由于我们必须轮询其他节点的取消，因此我们可能无法注意到被取消的节点是在我们前面还是在我们后面。
     *    这是通过在取消时总是解除后继者来处理的，允许他们稳定在新的前任者上，除非我们能确定一个未取消的前任者将承担这个责任。
     * G. CLH队列需要一个虚拟头节点来启动。但是我们不会在构建时创建它们，因为如果从不存在争用，那将是浪费精力。相反，在第一次争用时构造节点并设置头指针和尾指针。
     * H. 等待条件的线程使用相同的节点，但使用额外的链接。条件只需要链接简单（非并发）链接队列中的节点，因为它们仅在独占时才被访问。在等待时，一个节点被插入到条件队列中。
     *    根据信号，节点被转移到主队列。 status 字段的特殊值用于标记节点所在的队列。
     * I. 感谢 Dave Dice、Mark Moir、Victor Luchangco、Bill Scherer 和 Michael Scott 以及 JSR-166 专家组的成员，他们对本课程的设计提出了有益的想法、讨论和批评。
     */
    /**
     * Wait queue node class.
     *
     * A.
     * <p>The wait queue is a variant of a "CLH" (Craig, Landin, and
     * Hagersten) lock queue. CLH locks are normally used for
     * spinlocks.  We instead use them for blocking synchronizers, but
     * use the same basic tactic of holding some of the control
     * information about a thread in the predecessor of its node.  A
     * "status" field in each node keeps track of whether a thread
     * should block.  A node is signalled when its predecessor
     * releases.  Each node of the queue otherwise serves as a
     * specific-notification-style monitor holding a single waiting
     * thread. The status field does NOT control whether threads are
     * granted locks etc though.  A thread may try to acquire if it is
     * first in the queue. But being first does not guarantee success;
     * it only gives the right to contend.  So the currently released
     * contender thread may need to rewait.
     *
     * B.
     * <p>To enqueue into a CLH lock, you atomically splice it in as new
     * tail. To dequeue, you just set the head field.
     * <pre>
     *      +------+  prev +-----+       +-----+
     * head |      | <---- |     | <---- |     |  tail
     *      +------+       +-----+       +-----+
     * </pre>
     *
     * C.
     * <p>Insertion into a CLH queue requires only a single atomic
     * operation on "tail", so there is a simple atomic point of
     * demarcation from unqueued to queued. Similarly, dequeuing
     * involves only updating the "head". However, it takes a bit
     * more work for nodes to determine who their successors are,
     * in part to deal with possible cancellation due to timeouts
     * and interrupts.
     *
     * D.
     * <p>The "prev" links (not used in original CLH locks), are mainly
     * needed to handle cancellation. If a node is cancelled, its
     * successor is (normally) relinked to a non-cancelled
     * predecessor. For explanation of similar mechanics in the case
     * of spin locks, see the papers by Scott and Scherer at
     * http://www.cs.rochester.edu/u/scott/synchronization/
     *
     * E.
     * <p>We also use "next" links to implement blocking mechanics.
     * The thread id for each node is kept in its own node, so a
     * predecessor signals the next node to wake up by traversing
     * next link to determine which thread it is.  Determination of
     * successor must avoid races with newly queued nodes to set
     * the "next" fields of their predecessors.  This is solved
     * when necessary by checking backwards from the atomically
     * updated "tail" when a node's successor appears to be null.
     * (Or, said differently, the next-links are an optimization
     * so that we don't usually need a backward scan.)
     *
     * <p>Cancellation introduces some conservatism to the basic
     * algorithms.  Since we must poll for cancellation of other
     * nodes, we can miss noticing whether a cancelled node is
     * ahead or behind us. This is dealt with by always unparking
     * successors upon cancellation, allowing them to stabilize on
     * a new predecessor, unless we can identify an uncancelled
     * predecessor who will carry this responsibility.
     *
     * <p>CLH queues need a dummy header node to get started. But
     * we don't create them on construction, because it would be wasted
     * effort if there is never contention. Instead, the node
     * is constructed and head and tail pointers are set upon first
     * contention.
     *
     * <p>Threads waiting on Conditions use the same nodes, but
     * use an additional link. Conditions only need to link nodes
     * in simple (non-concurrent) linked queues because they are
     * only accessed when exclusively held.  Upon await, a node is
     * inserted into a condition queue.  Upon signal, the node is
     * transferred to the main queue.  A special value of status
     * field is used to mark which queue a node is on.
     *
     * <p>Thanks go to Dave Dice, Mark Moir, Victor Luchangco, Bill
     * Scherer and Michael Scott, along with members of JSR-166
     * expert group, for helpful ideas, discussions, and critiques
     * on the design of this class.
     */
    // AQS等待队列结点
    static final class Node {

        // 共享模式, 指示节点在共享模式下等待的标记
        /** Marker to indicate a node is waiting in shared mode */
        static final Node SHARED = new Node();

        // 独占模式, 指示节点正在以独占模式等待的标记
        /** Marker to indicate a node is waiting in exclusive mode */
        static final Node EXCLUSIVE = null;

        // 已取消状态, 指示线程已取消的waitStatus值
        /** waitStatus value to indicate thread has cancelled */
        static final int CANCELLED =  1;

        // 需通知后继结点状态, 指示后继线程需要解停的waitStatus值
        /** waitStatus value to indicate successor's thread needs unparking */
        static final int SIGNAL    = -1;

        // 条件等待状态, waitStatus值指示线程正在等待条件
        /** waitStatus value to indicate thread is waiting on condition */
        static final int CONDITION = -2;

        /**
         * waitStatus value to indicate the next acquireShared should
         * unconditionally propagate
         */
        // 需传播状态, 指示下一个acquireShared应无条件传播的waitStatus值
        static final int PROPAGATE = -3;

        /**
         * 20210722
         * A. 状态字段，仅采用以下值：
         *      1,  CANCELLED：由于超时或中断，该节点被取消。节点永远不会离开这个状态。特别是，取消节点的线程永远不会再次阻塞。
         *      0            : 等待队列中的初始化结点(未设置状态)
         *      -1, SIGNAL   : 该节点的后继节点被（或即将）阻塞（通过park），因此当前节点在释放或取消时必须解除其后继节点的停放。
         *                     为了避免竞争，获取方法必须首先表明它们需要一个信号，然后重试原子获取，然后在失败时阻塞。
         *      -2, CONDITION: 该节点当前在条件队列中。它在传输之前不会用作同步队列节点，此时状态将设置为0。（此处使用此值与该字段的其他用途无关，但简化了机制。）
         *      -3, PROPAGATE: A releaseShared应该传播到其他节点。这在doReleaseShared中设置（仅适用于头节点）以确保传播继续，即使其他操作已经介入。
         * B. 这些值按数字排列以简化使用。非负值意味着节点不需要发信号。 因此，大多数代码不需要检查特定值，只需检查符号。
         * C. 对于普通同步节点，该字段被初始化为0，对于条件节点，该字段被初始化为CONDITION。它使用CAS（或在可能的情况下，无条件volatile写入）进行修改。
         */
        /**
         * A.
         * Status field, taking on only the values:
         *   SIGNAL:     The successor of this node is (or will soon be)
         *               blocked (via park), so the current node must
         *               unpark its successor when it releases or
         *               cancels. To avoid races, acquire methods must
         *               first indicate they need a signal,
         *               then retry the atomic acquire, and then,
         *               on failure, block.
         *   CANCELLED:  This node is cancelled due to timeout or interrupt.
         *               Nodes never leave this state. In particular,
         *               a thread with cancelled node never again blocks.
         *   CONDITION:  This node is currently on a condition queue.
         *               It will not be used as a sync queue node
         *               until transferred, at which time the status
         *               will be set to 0. (Use of this value here has
         *               nothing to do with the other uses of the
         *               field, but simplifies mechanics.)
         *   PROPAGATE:  A releaseShared should be propagated to other
         *               nodes. This is set (for head node only) in
         *               doReleaseShared to ensure propagation
         *               continues, even if other operations have
         *               since intervened.
         *   0:          None of the above
         *
         * B.
         * The values are arranged numerically to simplify use.
         * Non-negative values mean that a node doesn't need to
         * signal. So, most code doesn't need to check for particular
         * values, just for sign.
         *
         * C.
         * The field is initialized to 0 for normal sync nodes, and
         * CONDITION for condition nodes.  It is modified using CAS
         * (or when possible, unconditional volatile writes).
         */
        // Node等待状态, 已取消的结点CANCELLED(1), 等待队列中的初始化结点(未设置状态), 通知结点SIGNAL(-1), 条件队列结点CONDITION(-2), 传播结点PROPAGATE(-3)
        volatile int waitStatus;

        /**
         * 20210722
         * 链接到当前节点/线程依赖于检查waitStatus的前驱节点。在入队期间分配，并仅在出队时取消（为了 GC）。此外，在取消前任时，我们在找到一个未取消的时进行短路，
         * 这将始终存在，因为头节点永远不会被取消：只有成功获取的结果，节点才成为头。 一个被取消的线程永远不会成功获取，并且一个线程只会取消自己，而不是任何其他节点。
         */
        /**
         * Link to predecessor node that current node/thread relies on
         * for checking waitStatus. Assigned during enqueuing, and nulled
         * out (for sake of GC) only upon dequeuing.  Also, upon
         * cancellation of a predecessor, we short-circuit while
         * finding a non-cancelled one, which will always exist
         * because the head node is never cancelled: A node becomes
         * head only as a result of successful acquire. A
         * cancelled thread never succeeds in acquiring, and a thread only
         * cancels itself, not any other node.
         */
        // AQS等待主队列前驱
        volatile Node prev;

        /**
         * 20210722
         * 链接到当前节点/线程在释放时解驻的后继节点。 在入队期间分配，在绕过取消的前任时进行调整，并在出队时取消（为了GC）。enq操作直到连接后才分配前驱的next字段，
         * 因此看到next字段为null的结点并不一定意味着该节点位于队列末尾。但是，如果next字段显示为空，我们可以从尾部扫描上一个字段以进行仔细检查。
         * 取消节点的next字段设置为指向节点本身而不是null，以使isOnSyncQueue的工作更轻松。
         */
        /**
         * Link to the successor node that the current node/thread
         * unparks upon release. Assigned during enqueuing, adjusted
         * when bypassing cancelled predecessors, and nulled out (for
         * sake of GC) when dequeued.  The enq operation does not
         * assign next field of a predecessor until after attachment,
         * so seeing a null next field does not necessarily mean that
         * node is at end of queue. However, if a next field appears
         * to be null, we can scan prev's from the tail to
         * double-check.  The next field of cancelled nodes is set to
         * point to the node itself instead of null, to make life
         * easier for isOnSyncQueue.
         */
        // AQS等待主队列后继
        volatile Node next;

        /**
         * 20210722
         * 将该节点加入队列的线程。在构造时初始化并在使用后归零。
         */
        /**
         * The thread that enqueued this node.  Initialized on
         * construction and nulled out after use.
         */
        // ASQ等待主队列、条件队列中的排队线程
        volatile Thread thread;

        /**
         * 20210722
         * 链接到下一个等待条件的节点，或特殊值SHARED。因为条件队列只有在独占模式下才会被访问，所以我们只需要一个简单的链接队列来保存节点，因为它们正在等待条件。
         * 然后将它们转移到队列以重新获取。 并且因为条件只能是独占的，所以我们通过使用特殊值来表示共享模式来保存字段。
         */
        /**
         * Link to next node waiting on condition, or the special
         * value SHARED.  Because condition queues are accessed only
         * when holding in exclusive mode, we just need a simple
         * linked queue to hold nodes while they are waiting on
         * conditions. They are then transferred to the queue to
         * re-acquire. And because conditions can only be exclusive,
         * we save a field by using special value to indicate shared
         * mode.
         */
        // 条件队列的后继, 或者代表等待主队列中的特殊值
        Node nextWaiter;

        /**
         * 20210722
         * 如果节点在共享模式下等待，则返回 true。
         */
        /**
         * Returns true if node is waiting in shared mode.
         */
        // 判断条件队列后继是否为特殊值SHARED
        final boolean isShared() {
            return nextWaiter == SHARED;
        }

        /**
         * 20210722
         * 返回上一个节点，如果为null，则抛出NullPointerException。 当前身不能为空时使用。 可以省略空检查，但存在以帮助 VM。
         */
        /**
         * Returns previous node, or throws NullPointerException if null.
         * Use when predecessor cannot be null.  The null check could
         * be elided, but is present to help the VM.
         *
         * @return the predecessor of this node
         */
        // volatile方式获取AQS等待队列前驱
        final Node predecessor() throws NullPointerException {
            Node p = prev;
            if (p == null)
                throw new NullPointerException();
            else
                return p;
        }

        // 无参构造函数
        Node() {    // Used to establish initial head or SHARED marker 用于建立初始头部或共享标记
        }

        // 根据指定线程和后继构造排队结点
        Node(Thread thread, Node mode) {     // Used by addWaiter 由 addWaiter 使用
            this.nextWaiter = mode;
            this.thread = thread;
        }

        // 根据指定线程和等待状态构造排队结点
        Node(Thread thread, int waitStatus) { // Used by Condition 按条件使用
            this.waitStatus = waitStatus;
            this.thread = thread;
        }
    }

    /**
     * 20210722
     * 等待队列的头部，延迟初始化。除初始化外，仅通过setHead方法进行修改。注意：如果head存在，则保证其waitStatus不会被CANCELLED。
     */
    /**
     * Head of the wait queue, lazily initialized.  Except for
     * initialization, it is modified only via method setHead.  Note:
     * If head exists, its waitStatus is guaranteed not to be
     * CANCELLED.
     */
    // AQS等待主队列头结点
    private transient volatile Node head;

    /**
     * 20210722
     * 等待队列的尾部，延迟初始化。仅通过方法enq修改以添加新的等待节点。
     */
    /**
     * Tail of the wait queue, lazily initialized.  Modified only via
     * method enq to add new wait node.
     */
    // AQS等待队列尾结点
    private transient volatile Node tail;

    /**
     * 20210722
     * 同步状态。
     */
    /**
     * The synchronization state.
     */
    // AQS同步器状态, 其语义看具体的实现
    private volatile int state;

    /**
     * 20210722
     * 返回同步状态的当前值。此操作具有 {@code volatile} 读取的内存语义。
     */
    /**
     * Returns the current value of synchronization state.
     * This operation has memory semantics of a {@code volatile} read.
     *
     * @return current state value
     */
    // volatile方式获取AQS同步器状态
    protected final int getState() {
        return state;
    }

    /**
     * 20210722
     * 设置同步状态的值。此操作具有{@code volatile}写入的内存语义。
     */
    /**
     * Sets the value of synchronization state.
     * This operation has memory semantics of a {@code volatile} write.
     *
     * @param newState the new state value
     */
    // volatile方式设置AQS同步器状态
    protected final void setState(int newState) {
        state = newState;
    }

    /**
     * 20210722
     * 如果当前状态值等于预期值，则原子地将同步状态设置为给定的更新值。此操作具有{@code volatile}读写的内存语义。
     */
    /**
     * Atomically sets synchronization state to the given updated
     * value if the current state value equals the expected value.
     * This operation has memory semantics of a {@code volatile} read
     * and write.
     *
     * @param expect the expected value
     * @param update the new value
     * @return {@code true} if successful. False return indicates that the actual
     *         value was not equal to the expected value.
     */
    // CAS原子式更新AQS同步器状态
    protected final boolean compareAndSetState(int expect, int update) {
        // See below for intrinsics setup to support this 请参阅下面的内在设置以支持此功能
        return unsafe.compareAndSwapInt(this, stateOffset, expect, update);
    }

    // Queuing utilities

    // 旋转速度比使用定时停车更快的纳秒数。 粗略估计足以在非常短的超时时间内提高响应能力。
    /**
     * The number of nanoseconds for which it is faster to spin
     * rather than to use timed park. A rough estimate suffices
     * to improve responsiveness with very short timeouts.
     */
    // 自旋时可以进入阻塞的时间
    static final long spinForTimeoutThreshold = 1000L;

    /**
     * 20210722
     * 将节点插入队列，必要时进行初始化。 见上图。
     */
    /**
     * Inserts node into queue, initializing if necessary. See picture above.
     *
     * @param node the node to insert
     * @return node's predecessor
     */
    // node结点(含排队线程)自旋入队方法, 自旋 + CAS入队, 如果队列还没初始化则需要初始化
    private Node enq(final Node node) {
        // 自旋, 如果最新的尾结点为null, 说明需要初始化, 则CAS添加新结点作为头尾结点
        for (;;) {
            Node t = tail;
            if (t == null) { // Must initialize 必须初始化
                if (compareAndSetHead(new Node()))
                    tail = head;
            }
            // 如果最新的尾结点不为null, 说明已经被初始化了, 则CAS结点(含排队线程)加入队尾
            else {
                // 设置node前驱与t后继可以不是原子的, 保证t后继原子也能达到并发安全的效果, 因为t如果不为真正的前驱, 那么CAS设置后继也会失败, 从而导致下轮自旋重新更新node前驱
                node.prev = t;
                if (compareAndSetTail(t, node)) {
                    t.next = node;
                    return t;
                }
                // CAS失败, 说明队尾已被别的线程更新了, 则继续自旋
            }
        }
    }

    /**
     * 20210722
     * 为当前线程和给定模式创建和排队节点。
     */
    /**
     * Creates and enqueues node for current thread and given mode.
     *
     * // Node.EXCLUSIVE为独占, Node.SHARED为共享
     * @param mode Node.EXCLUSIVE for exclusive, Node.SHARED for shared
     * @return the new node
     */
    // 使用当前线程构建独占/共享模式的Node结点, 并CAS+自旋直至入队成功
    private Node addWaiter(Node mode) {
        // 当前线程作为等待队列结点中的排队线程, mode为传入的模式(Node.EXCLUSIVE为独占, Node.SHARED为共享)
        Node node = new Node(Thread.currentThread(), mode);

        // 尝试enq的fast path; 失败时备份到完整的enq
        // Try the fast path of enq; backup to full enq on failure
        // enq自旋入队前的尝试, 获取最新的尾结点, 尝试CAS当前线程结点加入队尾
        Node pred = tail;
        if (pred != null) {
            node.prev = pred;
            if (compareAndSetTail(pred, node)) {
                pred.next = node;
                return node;
            }

        }

        // 如果CAS失败, 说明队尾已被别的线程更新了, 则调用Node结点(含排队线程)自旋入队方法, 自旋 + CAS入队, 如果队列还没初始化则需要初始化
        enq(node);

        // node(含排队线程)自旋入队成功, 则返回node结点
        return node;
    }

    /**
     * 20210722
     * 将队列头设置为节点，从而出队。仅由获取方法调用。为了GC和抑制不必要的信号和遍历，还清空了未使用的字段。
     */
    /**
     * Sets head of queue to be node, thus dequeuing. Called only by
     * acquire methods.  Also nulls out unused fields for sake of GC
     * and to suppress unnecessary signals and traversals.
     *
     * @param node the node
     */
    // volatile更新node为头结点, 并清空排队线程、前驱指针, 方便GC回收thread和prev
    private void setHead(Node node) {
        // volatile设置node为头结点
        head = node;

        // 清空排队线程、前驱指针, 方便GC回收thread和prev
        node.thread = null;
        node.prev = null;
    }

    /**
     * 20210722
     * 唤醒节点的后继节点（如果存在）。
     */
    /**
     * Wakes up node's successor, if one exists.
     *
     * @param node the node
     */
    // 唤醒node后继结点中的排队线程, 如果不存在有效的后继结点, 则从最新的尾结点向前找(非公平方式)第一个有效结点中的排队线程来进行唤醒
    private void unparkSuccessor(Node node) {

        /**
         * 20210722
         * 如果状态为负（即可能需要信号），请尝试清除以期待信号。如果此操作失败或等待线程更改状态，则可以。
         */
        /*
         * If status is negative (i.e., possibly needing signal) try
         * to clear in anticipation of signalling.  It is OK if this
         * fails or if status is changed by waiting thread.
         */
        // Node等待状态, 已取消的结点CANCELLED(1), 普通等待队列结点(0), 通知结点SIGNAL(-1), 条件队列结点CONDITION(-2), 传播结点PROPAGATE(-3)
        int ws = node.waitStatus;

        // 先更新node为普通结点
        // 如果Node等待状态小于0, 即可能是通知结点SIGNAL(-1), 条件队列结点CONDITION(-2), 传播结点PROPAGATE(-3), 则CAS更新等待状态为普通等待队列结点(0)
        if (ws < 0)
            compareAndSetWaitStatus(node, ws, 0);

        /**
         * 20210722
         * unpark的线程保留在后继节点中，通常只是下一个节点。但如果被取消或明显为空，则从尾部向后遍历以找到实际的未取消后继者。
         */
        /*
         * Thread to unpark is held in successor, which is normally
         * just the next node.  But if cancelled or apparently null,
         * traverse backwards from tail to find the actual
         * non-cancelled successor.
         */
        // volatile方式获取next结点作为唤醒的结点s
        Node s = node.next;

        // 如果s结点为null, 或者s为已取消的结点CANCELLED(1), 则无需唤醒
        if (s == null || s.waitStatus > 0) {
            // 清空s结点
            s = null;

            // 获取最新的尾结点t, 从后往前找第一个为有效的结点, 作为需要唤醒的结点s
            for (Node t = tail; t != null && t != node; t = t.prev)
                if (t.waitStatus <= 0)
                    s = t;
        }

        // 如果确实有需要唤醒的s结点, 则LockSupport.unpark该结点的排队线程
        if (s != null)
            LockSupport.unpark(s.thread);
    }

    /**
     * 20210722
     * 共享模式的释放动作——表示后继者并确保传播。（注意：对于独占模式，如果需要信号，释放就相当于调用头部的unparkSuccessor。）
     */
    /**
     * Release action for shared mode -- signals successor and ensures
     * propagation. (Note: For exclusive mode, release just amounts
     * to calling unparkSuccessor of head if it needs signal.)
     */
    // 执行结点释放动作, 在共享模式下, CAS更新为传播结点, 代表发布传播成功; 在独占模式下, 如果为SIGNAL结点, 则需要唤醒node后继结点中的排队线程或者从后往前找(非公平方式)的第一个有效结点的排队线程
    private void doReleaseShared() {

        /**
         * 20210722
         * 确保发布传播，即使有其他正在进行的获取/发布。如果需要信号，这会以通常的方式尝试 unparkSuccessor 头部。
         * 但如果不是，则状态设置为PROPAGATE以确保在发布时继续传播。此外，我们必须循环以防在我们这样做时添加了新节点。此外，与unparkSuccessor的其他用途不同，
         * 我们需要知道CAS重置状态是否失败，如果失败则重新检查。
         */
        /*
         * Ensure that a release propagates, even if there are other
         * in-progress acquires/releases.  This proceeds in the usual
         * way of trying to unparkSuccessor of head if it needs
         * signal. But if it does not, status is set to PROPAGATE to
         * ensure that upon release, propagation continues.
         * Additionally, we must loop in case a new node is added
         * while we are doing this. Also, unlike other uses of
         * unparkSuccessor, we need to know if CAS to reset status
         * fails, if so rechecking.
         */
        // 开始自旋
        for (;;) {
            // volatile方式获取头结点h
            Node h = head;

            // 如果CLH队列中至少有1个以上的结点
            if (h != null && h != tail) {
                // Node等待状态, 已取消的结点CANCELLED(1), 普通等待队列结点(0), 通知结点SIGNAL(-1), 条件队列结点CONDITION(-2), 传播结点PROPAGATE(-3)
                int ws = h.waitStatus;

                // 如果h为通知结点SIGNAL(-1), 则CAS更新h等待状态为0
                if (ws == Node.SIGNAL) {
                    if (!compareAndSetWaitStatus(h, Node.SIGNAL, 0))
                        // CAS更新失败, 则继续自旋
                        continue;            // loop to recheck cases

                    // CAS更新成功, 则唤醒node后继结点中的排队线程, 如果不存在有效的后继结点, 则从最新的尾结点向前找(非公平方式)第一个有效结点中的排队线程来进行唤醒
                    unparkSuccessor(h);
                }
                // 如果h为普通等待队列结点(0), 则CAS更新为传播结点PROPAGATE(-3)
                else if (ws == 0 && !compareAndSetWaitStatus(h, 0, Node.PROPAGATE))
                    // CAS更新失败, 则继续自旋
                    continue;                // loop on failed CAS
            }

            // 如果CLH队列只有1个结点 | 最新的head结点通知成功 | 更新为传播结点, 则退出自旋, 代表传播发布成功
            if (h == head)                   // loop if head changed
                break;
        }
    }

    /**
     * 20210722
     * 设置队列头，并检查后继者是否可能在共享模式下等待，如果传播 > 0 或设置了 PROPAGATE 状态，则传播。
     */
    /**
     * Sets head of queue, and checks if successor may be waiting
     * in shared mode, if so propagating if either propagate > 0 or
     * PROPAGATE status was set.
     *
     * @param node the node
     * @param propagate the return value from a tryAcquireShared // tryAcquireShared 的返回值
     */
    // 设置node为最新的结点, 执行node结点的通知、传播特性
    private void setHeadAndPropagate(Node node, int propagate) {
        // propagate: 失败(<0), 获取成功但后续共享不成功(=0), 获取成功且后续共享成功(>0)
        // 获取最新的头结点
        Node h = head; // Record old head for check below 记录下旧头以供检查

        // volatile更新node为头结点, 并清空排队线程、前驱指针, 方便GC回收thread和prev
        setHead(node);

        /**
         * 20210723
         * 如果出现以下情况，请尝试向下一个排队节点发出信号：传播由调用者指示，或被前一个操作记录（在setHead之前或之后作为h.waitStatus）
         *（注意：这使用waitStatus的符号检查，因为传播状态可能会转换为SIGNAL。)和下一个节点在共享模式等待，或者我们不知道，
         * 因为它出现null这两个检查的保守性可能会导致不必要的唤醒，但只有在有多个竞速获取/释放时，所以最需要现在或很快发出信号。
         */
        /*
         * Try to signal next queued node if:
         *   Propagation was indicated by caller,
         *     or was recorded (as h.waitStatus either before
         *     or after setHead) by a previous operation
         *     (note: this uses sign-check of waitStatus because
         *      PROPAGATE status may transition to SIGNAL.)
         * and
         *   The next node is waiting in shared mode,
         *     or we don't know, because it appears null
         *
         * The conservatism in both of these checks may cause
         * unnecessary wake-ups, but only when there are multiple
         * racing acquires/releases, so most need signals now or soon
         * anyway.
         */
        // 先看获得的同步器, 如果大于0, 说明需要传播
        if (propagate > 0
                // 如果没有同步器, 如果h为空, 说明队列中没有任何结点
                || h == null
                // 如果队列中有结点, 且h为通知|条件|传播结点, 说明有可能要传播
                || h.waitStatus < 0
                // 如果h也不需要传播, 则再重新获取最新头结点, 如果h为空, 则说明队列中没有任何结点
                || (h = head) == null
                // 如果队列中有结点, 且h为通知|条件|传播结点, 说明有可能要传播
                || h.waitStatus < 0) {
            // 获取指定node的后继s
            Node s = node.next;

            // 如果node后继的下一个等待条件的节点为特殊值SHARED, 则执行结点释放动作, 在共享模式下, CAS更新为传播结点, 代表发布传播成功;
            // 在独占模式下, 如果为SIGNAL结点, 则需要唤醒node后继结点中的排队线程或者从后往前找(非公平)的第一个有效结点的排队线程
            if (s == null || s.isShared())
                doReleaseShared();
        }
    }

    // Utilities for various versions of acquire
    // 各种版本的获取实用程序

    /**
     * 20210723
     * 取消正在进行的获取尝试。
     */
    /**
     * Cancels an ongoing attempt to acquire.
     *
     * @param node the node
     */
    // 取消并脱钩node结点, 唤醒node后继中等待线程
    private void cancelAcquire(Node node) {
        // Ignore if node doesn't exist
        // 如果节点不存在则忽略
        if (node == null)
            return;

        // 清空node的排队线程
        node.thread = null;

        // Skip cancelled predecessors
        // 跳过已取消的前驱, 直到找到为通知|条件|传播的结点作为新的前驱
        Node pred = node.prev;
        while (pred.waitStatus > 0)
            node.prev = pred = pred.prev;

        /**
         * 20210723
         * predNext是要取消拼接的明显节点。如果没有，下面的CAS将失败，在这种情况下，我们在与另一个取消或信号的比赛中输了，
         * 所以不需要采取进一步的行动。
         */
        // predNext is the apparent node to unsplice. CASes below will
        // fail if not, in which case, we lost race vs another cancel
        // or signal, so no further action is necessary.
        // volatile方式获取前驱的后继
        Node predNext = pred.next;

        /**
         * 20210723
         * 这里可以使用无条件写入代替CAS。经过这个原子步骤，其他Node可以跳过我们。之前，我们不受其他线程的干扰。
         */
        // Can use unconditional write instead of CAS here.
        // After this atomic step, other Nodes can skip past us.
        // Before, we are free of interference from other threads.
        // volatile方式更新node为取消结点
        node.waitStatus = Node.CANCELLED;

        // 如果我们是尾巴，请移开我们自己
        // If we are the tail, remove ourselves.
        // 如果node是最新的尾结点, 且尝试CAS更新尾结点为前驱
        if (node == tail && compareAndSetTail(node, pred)) {
            // 如果CAS更新尾结点成功, 则又CAS更新前驱的后继为null
            compareAndSetNext(pred, predNext, null);
            // 无论CAS更新前驱的后继结果怎么样, pred都为尾结点, 且它的next为null
        }
        // 如果node不是最新的尾结点
        else {
            /**
             * 20210723
             * 如果后继者需要信号，尝试设置pred的next链接，这样它就会得到一个。 否则唤醒它传播。
             */
            // If successor needs signal, try to set pred's next-link
            // so it will get one. Otherwise wake it up to propagate.
            int ws;

            // 如果前驱不为头结点, 且还没有被取消, 则CAS更新为通知结点
            if (pred != head &&
                ((ws = pred.waitStatus) == Node.SIGNAL || (ws <= 0 && compareAndSetWaitStatus(pred, ws, Node.SIGNAL))) &&
                pred.thread != null) {
                // 如果CAS更新前驱为通知结点成功, 且前驱的排队线程存在, 则获取node最新的后继next
                Node next = node.next;

                // 如果后继next存在, 且还没有被取消, 则CAS更新前驱结点的后继为node最新的后继, 跳过了node结点, 说明node脱钩了
                if (next != null && next.waitStatus <= 0)
                    compareAndSetNext(pred, predNext, next);
            }
            // 如果前驱为头结点, 或者已经被取消了, 或者CAS更新前驱为通知结点失败了
            else {
                // 则唤醒node后继结点中的排队线程, 如果不存在有效的后继结点, 则从最新的尾结点向前找第一个有效结点中的排队线程来进行唤醒
                unparkSuccessor(node);
            }

            // 如果node结点被取消了, 或者出队唤醒了, 则next自链接为自己, 方便GC
            node.next = node; // help GC
        }
    }

    /**
     * 20210723
     * 检查和更新未能获取的节点的状态。如果线程应该阻塞，则返回true。这是所有获取循环中的主要信号控制。要求pred == node.prev。
     */
    /**
     * Checks and updates status for a node that failed to acquire.
     * Returns true if thread should block. This is the main signal
     * control in all acquire loops.  Requires that pred == node.prev.
     *
     * @param pred node's predecessor holding status
     * @param node the node
     * @return {@code true} if thread should block // {@code true} 如果线程应该阻塞
     */
    // 放置node结点并阻塞其排队线程是否合理, 如果返回true代表确定了前驱为通知结点, 此时node的排队线程需要阻塞; 如果返回false代表还没能确定前驱为通知结点, 需要返回继续自旋判断, 此时node的排队线程不能被阻塞
    private static boolean shouldParkAfterFailedAcquire(Node pred, Node node) {
        // Node前驱的等待状态, 已取消的结点CANCELLED(1), 普通等待队列结点(0), 通知结点SIGNAL(-1), 条件队列结点CONDITION(-2), 传播结点PROPAGATE(-3)
        int ws = pred.waitStatus;

        // 如果前驱为通知结点, 由于已经设置了通知状态(释放时需要unpark后继结点的排队线程), 因此可以直接返回true即可, 代表前驱肯定为通知结点, 且node结点的排队线程需要阻塞
        if (ws == Node.SIGNAL)
            /**
             * 20210723
             * 这个节点已经设置了状态，要求释放信号，所以它可以安全地停放。
             */
            /*
             * This node has already set status asking a release
             * to signal it, so it can safely park.
             */
            return true;

        // 如果前驱为已取消的结点, 则跳过已取消的前驱, 向前找到第一个有效结点作为前驱并链接
        if (ws > 0) {
            /**
             * 20210723
             * 前任被取消。 跳过前辈并指示重试。
             */
            /*
             * Predecessor was cancelled. Skip over predecessors and
             * indicate retry.
             */
            do {
                node.prev = pred = pred.prev;
            } while (pred.waitStatus > 0);
            pred.next = node;
        }
        // 如果前驱为普通结点|条件结点|传播结点, 则CAS更新前驱为通知结点, 代表需要前驱释放后需要unpark后继结点的排队线程
        else {
            /**
             * 20210723
             * waitStatus必须为0或PROPAGATE。表明我们需要一个信号，但不要park。呼叫者将需要重试以确保其无法在park前获取。
             */
            /*
             * waitStatus must be 0 or PROPAGATE.  Indicate that we
             * need a signal, but don't park yet.  Caller will need to
             * retry to make sure it cannot acquire before parking.
             */
            compareAndSetWaitStatus(pred, ws, Node.SIGNAL);
        }

        // 如果node前驱一开始不为通知结点, 则返回false, 代表node结点的排队线程不需要阻塞, 因为没能保证前驱为通知结点, 需要返回做自旋确认
        return false;
    }

    /**
     * Convenience method to interrupt current thread.
     */
    // 中断该线程, 如果从Thread其他实例方法调用该方法, 则会清除中断状态, 然后会收到一个{@link InterruptedException}
    static void selfInterrupt() {
        Thread.currentThread().interrupt();
    }

    /**
     * Convenience method to park and then check if interrupted
     *
     * @return {@code true} if interrupted
     */
    // park阻塞当前线程, 然后检查当前线程是否中断(当前线程被中断, 会解除线程阻塞状态)
    private final boolean parkAndCheckInterrupt() {
        LockSupport.park(this);
        return Thread.interrupted();
    }

    /**
     * 20210723
     * 各种风格的获取，在独占/共享和控制模式中各不相同。每个都大致相同，但令人讨厌的不同。
     * 由于异常机制（包括确保我们在 tryAcquire 抛出异常时取消）和其他控制的相互作用，只能进行一点分解，至少不会在不过度损害性能的情况下。
     */
    /*
     * Various flavors of acquire, varying in exclusive/shared and
     * control modes.  Each is mostly the same, but annoyingly
     * different.  Only a little bit of factoring is possible due to
     * interactions of exception mechanics (including ensuring that we
     * cancel if tryAcquire throws exception) and other control, at
     * least not without hurting performance too much.
     */

    /**
     * 20210723
     * 以独占不间断模式获取已在队列中的线程。 由条件等待方法以及获取使用。
     */
    /**
     * Acquires in exclusive uninterruptible mode for thread already in
     * queue. Used by condition wait methods as well as acquire.
     *
     * @param node the node
     * @param arg the acquire argument
     * @return {@code true} if interrupted while waiting
     */
    // 独占式自旋入队的核心逻辑, 如果node前驱刚释放, 则更新node为头结点且不用阻塞直接返回; 否则放置node结点并阻塞其排队线程, 如果当前线程有被中断过, 则还需要继续自旋, 直到获取到同步器, 返回true代表有被中断过让上层API处理; 另外如果调用tryAcquire方法时发生了异常, 则还需要则取消node结点
    final boolean acquireQueued(final Node node, int arg) {
        boolean failed = true;
        try {
            boolean interrupted = false;

            // 开始自旋
            for (;;) {
                // volatile方式获取前驱p
                final Node p = node.predecessor();

                // 如果p为头结点, 则尝试以独占模式获取同步器
                if (p == head && tryAcquire(arg)) {
                    // 如果获取同步器成功, 则volatile更新node为头结点, 并清空排队线程、前驱指针, 方便GC回收thread和prev
                    setHead(node);
                    p.next = null; // help GC

                    // 更新失败为false, 返回interrupted, 为false代表当前线程自旋过程中没有被中断过, 为true代表当前线程自旋过程中有被中断过(含中断异常的方法调用时会在外面抛出中断异常)
                    failed = false;
                    return interrupted;
                }

                // 如果获取同步器失败, 则放置node结点并阻塞其排队线程是否合理, 如果返回true代表确定了前驱为通知结点, 此时node的排队线程需要阻塞; 如果返回false代表还没能确定前驱为通知结点, 需要返回继续自旋判断, 此时node的排队线程不能被阻塞
                if (shouldParkAfterFailedAcquire(p, node) &&
                    // 如果node结点放置成功并且需要阻塞, 则park阻塞当前线程, 然后检查当前线程是否中断(当前线程被中断, 会解除线程阻塞状态)
                    parkAndCheckInterrupt())
                    // 如果当前线程被中断了(Java中中断不了线程, 只是设置了中断标记位为true), 则更新interrupted为true, 代表当前线程有被中断过, 然后继续自旋, 直到获取到同步器
                    interrupted = true;
            }
        } finally {
            // 走到这里但failed为false, 说明在获取子类实现的tryAcquire方法时发生了异常
            if (failed)
                // 则取消node结点(前驱不为头结点且需要传播时), 或者唤醒node结点的排队线程(前驱为头结点时)
                cancelAcquire(node);
        }
    }

    /**
     * 20210723
     * 在独占可中断模式下获取。
     */
    /**
     * Acquires in exclusive interruptible mode.
     *
     * @param arg the acquire argument
     */
    // 独占可中断式自旋入队的核心逻辑, 如果node前驱刚释放, 则更新node为头结点且不用阻塞直接返回; 否则放置node结点并阻塞其排队线程, 如果调用tryAcquire方法时发生了异常或者自旋过程中发生了中断异常, 则还需要则取消node结点
    private void doAcquireInterruptibly(int arg) throws InterruptedException {
        // 使用当前线程构建独占Node结点, 并CAS+自旋直至入队成功
        final Node node = addWaiter(Node.EXCLUSIVE);
        boolean failed = true;
        try {
            // 开始自旋
            for (;;) {
                // volatile方式获取前驱p
                final Node p = node.predecessor();

                // 如果p为头结点, 则尝试以独占模式获取同步器
                if (p == head && tryAcquire(arg)) {
                    // 如果获取同步器成功, 则volatile更新node为头结点, 并清空排队线程、前驱指针, 方便GC回收thread和prev
                    setHead(node);
                    p.next = null; // help GC

                    // 更新失败为false并返回
                    failed = false;
                    return;
                }

                // 如果获取同步器失败, 则放置node结点并阻塞其排队线程是否合理, 如果返回true代表确定了前驱为通知结点, 此时node的排队线程需要阻塞; 如果返回false代表还没能确定前驱为通知结点, 需要返回继续自旋判断, 此时node的排队线程不能被阻塞
                if (shouldParkAfterFailedAcquire(p, node) &&
                        // 如果node结点放置成功并且需要阻塞, 则park阻塞当前线程, 然后检查当前线程是否中断(当前线程被中断, 会解除线程阻塞状态)
                        parkAndCheckInterrupt())
                    // 如果当前线程被中断了, 则抛出中断异常, 进行内部消化, 而不像acquireQueued那样返回中断标记
                    throw new InterruptedException();
            }
        } finally {
            // 走到这里但failed为false, 说明在获取子类实现的tryAcquire方法时发生了异常, 或者自旋过程中发生了中断异常
            if (failed)
                // 则取消node结点(前驱不为头结点且需要传播时), 或者唤醒node结点的排队线程(前驱为头结点时)
                cancelAcquire(node);
        }
    }

    /**
     * 20210723
     * 以独占定时模式获取。
     */
    /**
     * Acquires in exclusive timed mode.
     *
     * @param arg the acquire argument
     * @param nanosTimeout max wait time
     * @return {@code true} if acquired
     */
    // 独占定时可中断式自旋入队的核心逻辑, 如果node前驱刚释放, 则更新node为头结点且不用阻塞直接返回; 否则放置node结点并阻塞其排队线程, 如果调用tryAcquire方法时发生了异常或者自旋过程中发生了中断异常, 则还需要则取消node结点
    private boolean doAcquireNanos(int arg, long nanosTimeout) throws InterruptedException {
        // 超时时间小于等于0, 则直接返回false, 代表没获得同步状态
        if (nanosTimeout <= 0L)
            return false;

        // 系统纳米时间 + 超时时间 = 过期总时间
        final long deadline = System.nanoTime() + nanosTimeout;

        // 使用当前线程构建独占Node结点, 并CAS+自旋直至入队成功
        final Node node = addWaiter(Node.EXCLUSIVE);
        boolean failed = true;
        try {
            // 开始自旋
            for (;;) {
                // volatile方式获取前驱p
                final Node p = node.predecessor();

                // 如果p为头结点, 则尝试以独占模式获取同步器
                if (p == head && tryAcquire(arg)) {
                    // 如果获取同步器成功, 则volatile更新node为头结点, 并清空排队线程、前驱指针, 方便GC回收thread和prev
                    setHead(node);
                    p.next = null; // help GC

                    // 更新失败为false并返回
                    failed = false;
                    return true;
                }

                // 如果获取同步器失败, 则重新获取系统纳米时间, 用于更新过期总时间, 如果过期总时间小于等于0, 则直接返回false, 代表没获得同步状态
                nanosTimeout = deadline - System.nanoTime();
                if (nanosTimeout <= 0L)
                    return false;

                // 如果过期总时间大于0, 说明仍有效, 则放置node结点并阻塞其排队线程是否合理, 如果返回true代表确定了前驱为通知结点, 此时node的排队线程需要阻塞; 如果返回false代表还没能确定前驱为通知结点, 需要返回继续自旋判断, 此时node的排队线程不能被阻塞
                if (shouldParkAfterFailedAcquire(p, node)
                        // 如果过期总时间仍大于1s钟, 说明仍然需要阻塞线程, 则调用LockSupport的定时阻塞方法阻塞当前线程
                        && nanosTimeout > spinForTimeoutThreshold)
                    LockSupport.parkNanos(this, nanosTimeout);

                // 如果当前线程被中断或者退出阻塞, 则判断获取中断标志位, 如果为true则抛出中断异常
                if (Thread.interrupted())
                    throw new InterruptedException();
            }
        } finally {
            // 走到这里但failed为false, 说明在获取子类实现的tryAcquire方法时发生了异常, 或者自旋过程中发生了中断异常
            if (failed)
                // 则取消node结点(前驱不为头结点且需要传播时), 或者唤醒node结点的排队线程(前驱为头结点时)
                cancelAcquire(node);
        }
    }

    /**
     * 20210723
     * 在共享不间断模式下获取。
     */
    /**
     * Acquires in shared uninterruptible mode.
     *
     * @param arg the acquire argument
     */
    // 共享式自旋入队的核心逻辑, 如果node前驱刚释放, 则更新node为头结点且不用阻塞直接返回; 否则放置node结点并阻塞其排队线程, 如果当前线程有被中断过, 则还需要继续自旋, 直到获取到同步器, 返回true代表有被中断过让上层API处理; 另外如果调用tryAcquire方法时发生了异常, 则还需要则取消node结点
    private void doAcquireShared(int arg) {
        // 使用当前线程构建共享模式的Node结点, 并CAS+自旋直至入队成功
        final Node node = addWaiter(Node.SHARED);
        boolean failed = true;
        try {
            boolean interrupted = false;

            // 开始自旋
            for (;;) {
                // volatile方式获取前驱p
                final Node p = node.predecessor();

                // 如果p为头结点, 则尝试以共享模式获取同步器
                if (p == head) {
                    // 失败(<0), 获取成功但后续共享不成功(=0), 获取成功且后续共享成功(>0)
                    int r = tryAcquireShared(arg);

                    // 如果获取成功且后续共享成功(>0), 则volatile更新node为头结点, 并CAS更新为传播结点
                    if (r >= 0) {
                        setHeadAndPropagate(node, r);
                        p.next = null; // help GC

                        // 如果interrupted为true, 说明当前线程有被中断过, 则重新设置中断标记为true
                        if (interrupted)
                            selfInterrupt();
                        failed = false;
                        return;
                    }
                }

                // 如果获取同步器失败, 则放置node结点并阻塞其排队线程是否合理, 如果返回true代表确定了前驱为通知结点, 此时node的排队线程需要阻塞; 如果返回false代表还没能确定前驱为通知结点, 需要返回继续自旋判断, 此时node的排队线程不能被阻塞
                if (shouldParkAfterFailedAcquire(p, node) &&
                        // 如果node结点放置成功并且需要阻塞, 则park阻塞当前线程, 然后检查当前线程是否中断(当前线程被中断, 会解除线程阻塞状态)
                        parkAndCheckInterrupt())
                    // 如果当前线程被中断了(Java中中断不了线程, 只是设置了中断标记位为true), 则更新interrupted为true, 代表当前线程有被中断过, 然后继续自旋, 直到获取到同步器
                    interrupted = true;
            }
        } finally {
            // 走到这里但failed为false, 说明在获取子类实现的tryAcquire方法时发生了异常
            if (failed)
                // 则取消node结点(前驱不为头结点且需要传播时), 或者唤醒node结点的排队线程(前驱为头结点时)
                cancelAcquire(node);
        }
    }

    /**
     * 20210724
     * 在共享可中断模式下获取。
     */
    /**
     * Acquires in shared interruptible mode.
     *
     * @param arg the acquire argument
     */
    // 共享可中断式自旋入队的核心逻辑, 如果node前驱刚释放, 则更新node为头结点且不用阻塞直接返回; 否则放置node结点并阻塞其排队线程, 如果调用tryAcquire方法时发生了异常或者自旋过程中发生了中断异常, 则还需要则取消node结点
    private void doAcquireSharedInterruptibly(int arg) throws InterruptedException {
        // 使用当前线程构建共享Node结点, 并CAS+自旋直至入队成功
        final Node node = addWaiter(Node.SHARED);
        boolean failed = true;
        try {
            // 开始自旋
            for (;;) {
                // volatile方式获取前驱p
                final Node p = node.predecessor();

                // 如果p为头结点, 则尝试以独占模式获取同步器
                if (p == head) {
                    // 失败(<0), 获取成功但后续共享不成功(=0), 获取成功且后续共享成功(>0)
                    int r = tryAcquireShared(arg);

                    // 如果获取成功且后续共享成功(>0), 则volatile更新node为头结点, 并CAS更新为传播结点
                    if (r >= 0) {
                        setHeadAndPropagate(node, r);
                        p.next = null; // help GC
                        failed = false;
                        return;
                    }
                }

                // 如果获取同步器失败, 则放置node结点并阻塞其排队线程是否合理, 如果返回true代表确定了前驱为通知结点, 此时node的排队线程需要阻塞; 如果返回false代表还没能确定前驱为通知结点, 需要返回继续自旋判断, 此时node的排队线程不能被阻塞
                if (shouldParkAfterFailedAcquire(p, node) &&
                        // 如果node结点放置成功并且需要阻塞, 则park阻塞当前线程, 然后检查当前线程是否中断(当前线程被中断, 会解除线程阻塞状态)
                        parkAndCheckInterrupt())
                    // 如果当前线程被中断了, 则抛出中断异常, 进行内部消化
                    throw new InterruptedException();
            }
        } finally {
            // 走到这里但failed为false, 说明在获取子类实现的tryAcquire方法时发生了异常, 或者自旋过程中发生了中断异常
            if (failed)
                // 则取消node结点(前驱不为头结点且需要传播时), 或者唤醒node结点的排队线程(前驱为头结点时)
                cancelAcquire(node);
        }
    }

    /**
     * 20210724
     * 在共享定时模式下获取。
     */
    /**
     * Acquires in shared timed mode.
     *
     * @param arg the acquire argument
     * @param nanosTimeout max wait time
     * @return {@code true} if acquired
     */
    // 共享定时可中断式自旋入队的核心逻辑, 如果node前驱刚释放, 则更新node为头结点且不用阻塞直接返回; 否则放置node结点并阻塞其排队线程, 如果调用tryAcquire方法时发生了异常或者自旋过程中发生了中断异常, 则还需要则取消node结点
    private boolean doAcquireSharedNanos(int arg, long nanosTimeout) throws InterruptedException {
        // 超时时间小于等于0, 则直接返回false, 代表没获得同步状态
        if (nanosTimeout <= 0L)
            return false;

        // 系统纳米时间 + 超时时间 = 过期总时间
        final long deadline = System.nanoTime() + nanosTimeout;

        // 使用当前线程构建共享Node结点, 并CAS+自旋直至入队成功
        final Node node = addWaiter(Node.SHARED);
        boolean failed = true;
        try {
            // 开始自旋
            for (;;) {
                // volatile方式获取前驱p
                final Node p = node.predecessor();

                // 如果p为头结点, 则尝试以共享模式获取同步器
                if (p == head) {
                    // 失败(<0), 获取成功但后续共享不成功(=0), 获取成功且后续共享成功(>0)
                    int r = tryAcquireShared(arg);

                    // 如果获取成功且后续共享成功(>0), 则volatile更新node为头结点, 并CAS更新为传播结点
                    if (r >= 0) {
                        setHeadAndPropagate(node, r);
                        p.next = null; // help GC
                        failed = false;
                        return true;
                    }
                }

                // 如果获取同步器失败, 则重新获取系统纳米时间, 用于更新过期总时间, 如果过期总时间小于等于0, 则直接返回false, 代表没获得同步状态
                nanosTimeout = deadline - System.nanoTime();
                if (nanosTimeout <= 0L)
                    return false;

                // 如果过期总时间大于0, 说明仍有效, 则放置node结点并阻塞其排队线程是否合理, 如果返回true代表确定了前驱为通知结点, 此时node的排队线程需要阻塞; 如果返回false代表还没能确定前驱为通知结点, 需要返回继续自旋判断, 此时node的排队线程不能被阻塞
                if (shouldParkAfterFailedAcquire(p, node) &&
                        // 如果过期总时间仍大于1s钟, 说明仍然需要阻塞线程, 则调用LockSupport的定时阻塞方法阻塞当前线程
                        nanosTimeout > spinForTimeoutThreshold)
                    LockSupport.parkNanos(this, nanosTimeout);

                // 如果当前线程被中断或者退出阻塞, 则判断获取中断标志位, 如果为true则抛出中断异常
                if (Thread.interrupted())
                    throw new InterruptedException();
            }
        } finally {
            // 走到这里但failed为false, 说明在获取子类实现的tryAcquire方法时发生了异常, 或者自旋过程中发生了中断异常
            if (failed)
                // 则取消node结点(前驱不为头结点且需要传播时), 或者唤醒node结点的排队线程(前驱为头结点时)
                cancelAcquire(node);
        }
    }

    // Main exported methods 主要导出方法

    /**
     * 20210723
     * A. 尝试以独占模式获取。该方法应该查询对象的状态是否允许以独占模式获取它，如果允许则获取它。
     * B. 此方法始终由执行获取的线程调用。如果此方法报告失败，acquire方法可能会将线程排队（如果它尚未排队），直到收到来自某个其他线程的释放信号。
     *    这可用于实现方法 {@link Lock#tryLock()}。
     * C. 默认实现抛出{@link UnsupportedOperationException}。
     */
    /**
     * A.
     * Attempts to acquire in exclusive mode. This method should query
     * if the state of the object permits it to be acquired in the
     * exclusive mode, and if so to acquire it.
     *
     * B.
     * <p>This method is always invoked by the thread performing
     * acquire.  If this method reports failure, the acquire method
     * may queue the thread, if it is not already queued, until it is
     * signalled by a release from some other thread. This can be used
     * to implement method {@link Lock#tryLock()}.
     *
     * C.
     * <p>The default
     * implementation throws {@link UnsupportedOperationException}.
     *
     * // 获取参数。该值始终是传递给获取方法的值，或者是在进入条件等待时保存的值。该值是未经解释的，可以表示您喜欢的任何内容。
     * @param arg the acquire argument. This value is always the one
     *        passed to an acquire method, or is the value saved on entry
     *        to a condition wait.  The value is otherwise uninterpreted
     *        and can represent anything you like.
     * // {@code true}如果成功。成功后，该对象已获得。
     * @return {@code true} if successful. Upon success, this object has
     *         been acquired.
     * // 如果获取会将这个同步器置于非法状态。 必须以一致的方式抛出此异常，同步才能正常工作。
     * @throws IllegalMonitorStateException if acquiring would place this
     *         synchronizer in an illegal state. This exception must be
     *         thrown in a consistent fashion for synchronization to work
     *         correctly.
     * @throws UnsupportedOperationException if exclusive mode is not supported
     */
    // 尝试以独占模式获取同步器
    protected boolean tryAcquire(int arg) {
        throw new UnsupportedOperationException();
    }

    /**
     * 20210724
     * A. 尝试设置状态以反映独占模式下的释放。
     * B. 此方法始终由执行释放的线程调用。
     * C. 默认实现抛出 {@link UnsupportedOperationException}。
     */
    /**
     * A.
     * Attempts to set the state to reflect a release in exclusive mode.
     *
     * B.
     * <p>This method is always invoked by the thread performing release.
     *
     * C.
     * <p>The default implementation throws {@link UnsupportedOperationException}.
     *
     * // 释放参数。 该值始终是传递给释放方法的值，或者是进入条件等待时的当前状态值。 该值是未经解释的，可以表示您喜欢的任何内容。
     * @param arg the release argument. This value is always the one
     *        passed to a release method, or the current state value upon
     *        entry to a condition wait.  The value is otherwise
     *        uninterpreted and can represent anything you like.
     *
     * // {@code true} 如果此对象现在处于完全释放状态，以便任何等待的线程都可以尝试获取； 和 {@code false} 否则。
     * @return {@code true} if this object is now in a fully released
     *         state, so that any waiting threads may attempt to acquire;
     *         and {@code false} otherwise.
     *
     * // 如果释放会使这个同步器处于非法状态。 必须以一致的方式抛出此异常，同步才能正常工作。
     * @throws IllegalMonitorStateException if releasing would place this
     *         synchronizer in an illegal state. This exception must be
     *         thrown in a consistent fashion for synchronization to work
     *         correctly.
     *
     * // 如果不支持独占模式
     * @throws UnsupportedOperationException if exclusive mode is not supported
     */
    // 尝试释放独占模式的同步器
    protected boolean tryRelease(int arg) {
        throw new UnsupportedOperationException();
    }

    /**
     * 20210724
     * A. 尝试以共享模式获取。 该方法应该查询对象的状态是否允许在共享模式下获取它，如果允许则获取它。
     * B. 此方法始终由执行获取的线程调用。 如果此方法报告失败，acquire 方法可能会将线程排队（如果它尚未排队），直到收到来自某个其他线程的释放信号。
     * C. 默认实现抛出 {@link UnsupportedOperationException}。
     */
    /**
     * A.
     * Attempts to acquire in shared mode. This method should query if
     * the state of the object permits it to be acquired in the shared
     * mode, and if so to acquire it.
     *
     * B.
     * <p>This method is always invoked by the thread performing
     * acquire.  If this method reports failure, the acquire method
     * may queue the thread, if it is not already queued, until it is
     * signalled by a release from some other thread.
     *
     * C.
     * <p>The default implementation throws {@link UnsupportedOperationException}.
     *
     * // 获取参数。该值始终是传递给获取方法的值，或者是在进入条件等待时保存的值。 该值是未经解释的，可以表示您喜欢的任何内容。
     * @param arg the acquire argument. This value is always the one
     *        passed to an acquire method, or is the value saved on entry
     *        to a condition wait.  The value is otherwise uninterpreted
     *        and can represent anything you like.
     *
     * // 失败的负值; 如果在共享模式下获取成功但后续共享模式获取不能成功，则为零;
     * 如果在共享模式下获取成功并且后续共享模式获取也可能成功，则为正值，在这种情况下，后续等待线程必须检查可用性。
     * （对三种不同返回值的支持使此方法能够在仅有时仅执行独占行为的上下文中使用。）成功后，该对象已被获取。
     * @return a negative value on failure; zero if acquisition in shared
     *         mode succeeded but no subsequent shared-mode acquire can
     *         succeed; and a positive value if acquisition in shared
     *         mode succeeded and subsequent shared-mode acquires might
     *         also succeed, in which case a subsequent waiting thread
     *         must check availability. (Support for three different
     *         return values enables this method to be used in contexts
     *         where acquires only sometimes act exclusively.)  Upon
     *         success, this object has been acquired.
     * @throws IllegalMonitorStateException if acquiring would place this
     *         synchronizer in an illegal state. This exception must be
     *         thrown in a consistent fashion for synchronization to work
     *         correctly.
     * @throws UnsupportedOperationException if shared mode is not supported
     */
    // 尝试以共享模式获取同步器, 失败(<0), 获取成功但后续共享不成功(=0), 获取成功且后续共享成功(>0)
    protected int tryAcquireShared(int arg) {
        throw new UnsupportedOperationException();
    }

    /**
     * 20210724
     * A. 尝试设置状态以反映共享模式下的发布。
     * B. 此方法始终由执行释放的线程调用。
     * C. 默认实现抛出 {@link UnsupportedOperationException}。
     */
    /**
     * A.
     * Attempts to set the state to reflect a release in shared mode.
     *
     * B.
     * <p>This method is always invoked by the thread performing release.
     *
     * C.
     * <p>The default implementation throws {@link UnsupportedOperationException}.
     *
     * @param arg the release argument. This value is always the one
     *        passed to a release method, or the current state value upon
     *        entry to a condition wait.  The value is otherwise
     *        uninterpreted and can represent anything you like.
     *
     * // {@code true} 如果此共享模式的发布可能允许等待的获取（共享或独占）成功； 和 {@code false} 否则
     * @return {@code true} if this release of shared mode may permit a
     *         waiting acquire (shared or exclusive) to succeed; and
     *         {@code false} otherwise
     * @throws IllegalMonitorStateException if releasing would place this
     *         synchronizer in an illegal state. This exception must be
     *         thrown in a consistent fashion for synchronization to work
     *         correctly.
     * @throws UnsupportedOperationException if shared mode is not supported
     */
    // 尝试释放共享模式的同步器
    protected boolean tryReleaseShared(int arg) {
        throw new UnsupportedOperationException();
    }

    /**
     * 20210724
     * A. 如果同步只针对当前（调用）线程进行，则返回 {@code true}。每次调用非等待 {@link ConditionObject} 方法时都会调用此方法。（等待方法改为调用 {@link #release}。）
     * B. 默认实现抛出 {@link UnsupportedOperationException}。 此方法仅在 {@link ConditionObject} 方法内部调用，因此如果不使用条件则无需定义。
     */
    /**
     * A.
     * Returns {@code true} if synchronization is held exclusively with
     * respect to the current (calling) thread.  This method is invoked
     * upon each call to a non-waiting {@link ConditionObject} method.
     * (Waiting methods instead invoke {@link #release}.)
     *
     * B.
     * <p>The default implementation throws {@link
     * UnsupportedOperationException}. This method is invoked
     * internally only within {@link ConditionObject} methods, so need
     * not be defined if conditions are not used.
     *
     * // {@code true} 如果同步是独占的； {@code false} 否则
     * @return {@code true} if synchronization is held exclusively;
     *         {@code false} otherwise
     * @throws UnsupportedOperationException if conditions are not supported
     */
    // 判断同步模式是独占模式还是共享模式
    protected boolean isHeldExclusively() {
        throw new UnsupportedOperationException();
    }

    /**
     * 20210724
     * 以独占模式获取，忽略中断。通过至少调用一次{@link #tryAcquire}实现，成功返回。否则线程会排队，可能会反复阻塞和解除阻塞，调用 {@link #tryAcquire} 直到成功。
     * 该方法可用于实现方法{@link Lock#lock}。
     */
    /**
     * Acquires in exclusive mode, ignoring interrupts.  Implemented
     * by invoking at least once {@link #tryAcquire},
     * returning on success.  Otherwise the thread is queued, possibly
     * repeatedly blocking and unblocking, invoking {@link
     * #tryAcquire} until success.  This method can be used
     * to implement method {@link Lock#lock}.
     *
     * @param arg the acquire argument.  This value is conveyed to
     *        {@link #tryAcquire} but is otherwise uninterpreted and
     *        can represent anything you like.
     */
    // 以阻塞、独占模式获取同步器
    public final void acquire(int arg) {
        // 尝试以独占模式获取同步器
        if (!tryAcquire(arg) &&
                // 独占式自旋入队的核心逻辑, 如果node前驱刚释放, 则更新node为头结点且不用阻塞直接返回; 否则放置node结点并阻塞其排队线程, 如果当前线程有被中断过, 则还需要继续自旋, 直到获取到同步器, 返回true代表有被中断过让上层API处理; 另外如果调用tryAcquire方法时发生了异常, 则还需要则取消node结点
                acquireQueued(
                    // 使用当前线程构建独占模式的Node结点, 并CAS+自旋直至入队成功
                    addWaiter(Node.EXCLUSIVE), arg
                )
            )
            // 中断当前线程
            selfInterrupt();
    }

    /**
     * 20210724
     * 以独占模式获取，如果中断则中止。通过首先检查中断状态来实现，然后至少调用一次{@link #tryAcquire}，成功返回。
     * 否则线程会排队，可能会重复阻塞和解除阻塞，调用{@link #tryAcquire}直到成功或线程被中断。该方法可用于实现方法{@link Lock#lockInterruptably}。
     */
    /**
     * Acquires in exclusive mode, aborting if interrupted.
     * Implemented by first checking interrupt status, then invoking
     * at least once {@link #tryAcquire}, returning on
     * success.  Otherwise the thread is queued, possibly repeatedly
     * blocking and unblocking, invoking {@link #tryAcquire}
     * until success or the thread is interrupted.  This method can be
     * used to implement method {@link Lock#lockInterruptibly}.
     *
     * @param arg the acquire argument.  This value is conveyed to
     *        {@link #tryAcquire} but is otherwise uninterpreted and
     *        can represent anything you like.
     * @throws InterruptedException if the current thread is interrupted
     */
    // 以阻塞、独占可中断模式获取同步器
    public final void acquireInterruptibly(int arg) throws InterruptedException {
        // 如果当前线程被中断了, 则抛出中断异常
        if (Thread.interrupted())
            throw new InterruptedException();

        // 尝试以独占模式获取同步器
        if (!tryAcquire(arg))
            // 独占可中断式自旋入队的核心逻辑, 如果node前驱刚释放, 则更新node为头结点且不用阻塞直接返回; 否则放置node结点并阻塞其排队线程, 如果调用tryAcquire方法时发生了异常或者自旋过程中发生了中断异常, 则还需要则取消node结点
            doAcquireInterruptibly(arg);
    }

    /**
     * 20210724
     * 尝试以独占模式获取，如果中断则中止，如果给定的超时时间过去则失败。通过首先检查中断状态来实现，然后至少调用一次 {@link #tryAcquire}，成功返回。
     * 否则，线程将排队，可能会重复阻塞和解除阻塞，调用{@link #tryAcquire}直到成功或线程被中断或超时。 该方法可用于实现方法{@link Lock#tryLock(long, TimeUnit)}。
     */
    /**
     * Attempts to acquire in exclusive mode, aborting if interrupted,
     * and failing if the given timeout elapses.  Implemented by first
     * checking interrupt status, then invoking at least once {@link
     * #tryAcquire}, returning on success.  Otherwise, the thread is
     * queued, possibly repeatedly blocking and unblocking, invoking
     * {@link #tryAcquire} until success or the thread is interrupted
     * or the timeout elapses.  This method can be used to implement
     * method {@link Lock#tryLock(long, TimeUnit)}.
     *
     * @param arg the acquire argument.  This value is conveyed to
     *        {@link #tryAcquire} but is otherwise uninterpreted and
     *        can represent anything you like.
     * @param nanosTimeout the maximum number of nanoseconds to wait
     * @return {@code true} if acquired; {@code false} if timed out
     * @throws InterruptedException if the current thread is interrupted
     */
    // 以阻塞、独占定时可中断模式获取同步器
    public final boolean tryAcquireNanos(int arg, long nanosTimeout) throws InterruptedException {
        // 如果当前线程被中断了, 则抛出中断异常
        if (Thread.interrupted())
            throw new InterruptedException();
        // 尝试以独占模式获取同步器
        return tryAcquire(arg) ||
                // 独占定时可中断式自旋入队的核心逻辑, 如果node前驱刚释放, 则更新node为头结点且不用阻塞直接返回; 否则放置node结点并阻塞其排队线程, 如果调用tryAcquire方法时发生了异常或者自旋过程中发生了中断异常, 则还需要则取消node结点
                doAcquireNanos(arg, nanosTimeout);
    }

    /**
     * 20210724
     * 以独占模式释放。如果{@link #tryRelease}返回true，则通过解除阻塞一个或多个线程来实现。 该方法可用于实现方法{@link Lock#unlock}。
     */
    /**
     * Releases in exclusive mode.  Implemented by unblocking one or
     * more threads if {@link #tryRelease} returns true.
     * This method can be used to implement method {@link Lock#unlock}.
     *
     * @param arg the release argument.  This value is conveyed to
     *        {@link #tryRelease} but is otherwise uninterpreted and
     *        can represent anything you like.
     *
     * // 从 {@link #tryRelease} 返回的值
     * @return the value returned from {@link #tryRelease}
     */
    // 尝试释放独占模式的同步器, 如果释放成功, 则返回true; 如果释放失败, 则返回false
    public final boolean release(int arg) {
        // 尝试释放独占模式的同步器
        if (tryRelease(arg)) {
            Node h = head;

            // 释放成功, 如果头结点(也就是被释放的结点)不为普通结点
            if (h != null && h.waitStatus != 0)
                // 则唤醒node后继结点中的排队线程, 如果不存在有效的后继结点, 则从最新的尾结点向前找第一个有效结点中的排队线程来进行唤醒
                unparkSuccessor(h);

            // 唤醒后返回true, 代表已成功释放
            return true;
        }

        // 释放失败则返回false, 代表释放失败
        return false;
    }

    /**
     * 20210724
     * 在共享模式下获取，忽略中断。通过首先调用至少一次{@link #tryAcquireShared}来实现，成功返回。否则线程会排队，可能会反复阻塞和解除阻塞，
     * 调用{@link #tryAcquireShared} 直到成功。
     */
    /**
     * Acquires in shared mode, ignoring interrupts.  Implemented by
     * first invoking at least once {@link #tryAcquireShared},
     * returning on success.  Otherwise the thread is queued, possibly
     * repeatedly blocking and unblocking, invoking {@link
     * #tryAcquireShared} until success.
     *
     * @param arg the acquire argument.  This value is conveyed to
     *        {@link #tryAcquireShared} but is otherwise uninterpreted
     *        and can represent anything you like.
     */
    // 以阻塞、共享模式获取同步器
    public final void acquireShared(int arg) {
        // 尝试以共享模式获取同步器, 失败(<0), 获取成功但后续共享不成功(=0), 获取成功且后续共享成功(>0)
        if (tryAcquireShared(arg) < 0)
            // 共享式自旋入队的核心逻辑, 如果node前驱刚释放, 则更新node为头结点且不用阻塞直接返回; 否则放置node结点并阻塞其排队线程, 如果当前线程有被中断过, 则还需要继续自旋, 直到获取到同步器, 返回true代表有被中断过让上层API处理; 另外如果调用tryAcquire方法时发生了异常, 则还需要则取消node结点
            doAcquireShared(arg);
    }

    /**
     * 20210724
     * 在共享模式下获取，如果中断则中止。通过首先检查中断状态，然后至少调用一次{@link #tryAcquireShared}来实现，成功返回。 否则线程会排队，可能会重复阻塞和解除阻塞，
     * 调用{@link #tryAcquireShared}直到成功或线程被中断。
     */
    /**
     * Acquires in shared mode, aborting if interrupted.  Implemented
     * by first checking interrupt status, then invoking at least once
     * {@link #tryAcquireShared}, returning on success.  Otherwise the
     * thread is queued, possibly repeatedly blocking and unblocking,
     * invoking {@link #tryAcquireShared} until success or the thread
     * is interrupted.
     *
     * @param arg the acquire argument.
     * This value is conveyed to {@link #tryAcquireShared} but is
     * otherwise uninterpreted and can represent anything
     * you like.
     * @throws InterruptedException if the current thread is interrupted
     */
    // 以阻塞、共享可中断模式获取同步器
    public final void acquireSharedInterruptibly(int arg) throws InterruptedException {
        // 如果当前线程被中断了, 则抛出中断异常
        if (Thread.interrupted())
            throw new InterruptedException();

        // 尝试以共享模式获取同步器, 失败(<0), 获取成功但后续共享不成功(=0), 获取成功且后续共享成功(>0)
        if (tryAcquireShared(arg) < 0)
            // 共享可中断式自旋入队的核心逻辑, 如果node前驱刚释放, 则更新node为头结点且不用阻塞直接返回; 否则放置node结点并阻塞其排队线程, 如果调用tryAcquire方法时发生了异常或者自旋过程中发生了中断异常, 则还需要则取消node结点
            doAcquireSharedInterruptibly(arg);
    }

    /**
     * 20210724
     * 尝试在共享模式下获取，如果被中断则中止，如果给定的超时时间过去则失败。通过首先检查中断状态，然后至少调用一次{@link #tryAcquireShared}来实现，成功返回。
     * 否则，线程将排队，可能会重复阻塞和解除阻塞，调用 {@link #tryAcquireShared} 直到成功或线程被中断或超时。
     */
    /**
     * Attempts to acquire in shared mode, aborting if interrupted, and
     * failing if the given timeout elapses.  Implemented by first
     * checking interrupt status, then invoking at least once {@link
     * #tryAcquireShared}, returning on success.  Otherwise, the
     * thread is queued, possibly repeatedly blocking and unblocking,
     * invoking {@link #tryAcquireShared} until success or the thread
     * is interrupted or the timeout elapses.
     *
     * @param arg the acquire argument.  This value is conveyed to
     *        {@link #tryAcquireShared} but is otherwise uninterpreted
     *        and can represent anything you like.
     * @param nanosTimeout the maximum number of nanoseconds to wait
     * @return {@code true} if acquired; {@code false} if timed out
     * @throws InterruptedException if the current thread is interrupted
     */
    // 以阻塞、共享定时可中断模式获取同步器
    public final boolean tryAcquireSharedNanos(int arg, long nanosTimeout) throws InterruptedException {
        // 如果当前线程被中断了, 则抛出中断异常
        if (Thread.interrupted())
            throw new InterruptedException();

        // 尝试以共享模式获取同步器, 失败(<0), 获取成功但后续共享不成功(=0), 获取成功且后续共享成功(>0)
        return tryAcquireShared(arg) >= 0 ||
                // 共享定时可中断式自旋入队的核心逻辑, 如果node前驱刚释放, 则更新node为头结点且不用阻塞直接返回; 否则放置node结点并阻塞其排队线程, 如果调用tryAcquire方法时发生了异常或者自旋过程中发生了中断异常, 则还需要则取消node结点
                doAcquireSharedNanos(arg, nanosTimeout);
    }

    /**
     * 20210724
     * 以共享模式释放。如果{@link #tryReleaseShared} 返回 true，则通过解除阻塞一个或多个线程来实现。
     */
    /**
     * Releases in shared mode.  Implemented by unblocking one or more
     * threads if {@link #tryReleaseShared} returns true.
     *
     * @param arg the release argument.  This value is conveyed to
     *        {@link #tryReleaseShared} but is otherwise uninterpreted
     *        and can represent anything you like.
     * @return the value returned from {@link #tryReleaseShared}
     */
    // 释放共享模式的同步器, 如果释放成功, 则返回true; 如果释放失败, 则返回false
    public final boolean releaseShared(int arg) {
        // 尝试释放共享模式的同步器
        if (tryReleaseShared(arg)) {
            // 执行结点释放动作, 在共享模式下, CAS更新为传播结点, 代表发布传播成功; 在独占模式下, 如果为SIGNAL结点, 则需要唤醒node后继结点中的排队线程或者从后往前找的第一个有效结点的排队线程
            doReleaseShared();

            // 传播后返回true, 代表已成功释放
            return true;
        }

        // 释放失败则返回false, 代表释放失败
        return false;
    }

    // Queue inspection methods 队列检查方法

    /**
     * 20210724
     * A. 查询是否有线程在等待获取。 请注意，由于中断和超时导致的取消随时可能发生，{@code true} 返回并不能保证任何其他线程将永远获得。
     * B. 在此实现中，此操作以恒定时间返回。
     */
    /**
     * A.
     * Queries whether any threads are waiting to acquire. Note that
     * because cancellations due to interrupts and timeouts may occur
     * at any time, a {@code true} return does not guarantee that any
     * other thread will ever acquire.
     *
     * B.
     * <p>In this implementation, this operation returns in
     * constant time.
     *
     * @return {@code true} if there may be other threads waiting to acquire // {@code true} 如果可能有其他线程等待获取
     */
    // 判断CLH队列中是否有线程在等待获取同步器, 由于中断和超时导致的结点取消随时都可能发生, 所以返回{@code true}并不能保证CLH队列中接下来都存在结点。
    public final boolean hasQueuedThreads() {
        return head != tail;
    }

    /**
     * 20210724
     * A. 查询是否有线程争用过这个同步器；也就是说，如果一个获取方法曾经被阻塞。
     * B. 在此实现中，此操作以恒定时间返回。
     */
    /**
     * A.
     * Queries whether any threads have ever contended to acquire this
     * synchronizer; that is if an acquire method has ever blocked.
     *
     * B.
     * <p>In this implementation, this operation returns in
     * constant time.
     *
     * @return {@code true} if there has ever been contention // {@code true} 如果曾经有过争用
     */
    // 判断CLH队头是否存在, 如果存在, 则代表同步器有被别的结点发生过争抢
    public final boolean hasContended() {
        return head != null;
    }

    /**
     * 20210724
     * A. 返回队列中的第一个（等待时间最长的）线程，如果当前没有线程排队，则返回 {@code null}。
     * B. 在此实现中，此操作通常以恒定时间返回，但如果其他线程同时修改队列，则可能会在争用时进行迭代。
     */
    /**
     * A.
     * Returns the first (longest-waiting) thread in the queue, or
     * {@code null} if no threads are currently queued.
     *
     * B.
     * <p>In this implementation, this operation normally returns in
     * constant time, but may iterate upon contention if other threads are
     * concurrently modifying the queue.
     *
     * // 队列中的第一个（等待时间最长的）线程，如果当前没有线程排队，则为 {@code null}
     * @return the first (longest-waiting) thread in the queue, or
     *         {@code null} if no threads are currently queued
     */
    // 获取CLH队列中第一个排队线程(头后继检查两次+从尾到前遍历), 如果没有则返回null
    public final Thread getFirstQueuedThread() {
        // handle only fast path, else relay 只处理快速路径，否则中继
        // 如果CLH队列中没有结点, 则返回null; 否则, 获取CLH中的第一个排队线程: 先检查从头检查两次头后继结点, 如果存在排队线程则返回; 如果不存在则从尾往前找, 返回第一个有效的不为头结点的排队结点的排队线程
        return (head == tail) ? null : fullGetFirstQueuedThread();
    }

    /**
     * Version of getFirstQueuedThread called when fastpath fails 快速路径失败时调用的getFirstQueuedThread版本
     */
    // 获取CLH中的第一个排队线程: 先检查从头检查两次头后继结点, 如果存在排队线程则返回; 如果不存在则从尾往前找, 返回第一个有效的不为头结点的排队结点的排队线程
    private Thread fullGetFirstQueuedThread() {
        /**
         * 20210724
         * 第一个节点通常是head.next。尝试获取其线程字段，确保读取一致：
         * 如果线程字段被清空或s.prev不再是头，那么在我们的一些读取之间，一些其他线程并发执行setHead。在诉诸遍历之前，我们尝试了两次。
         */
        /*
         * The first node is normally head.next. Try to get its
         * thread field, ensuring consistent reads: If thread
         * field is nulled out or s.prev is no longer head, then
         * some other thread(s) concurrently performed setHead in
         * between some of our reads. We try this twice before
         * resorting to traversal.
         */
        Node h, s;
        Thread st;

        // 头结点h, 头结点后继s作为第一个结点, 如果s前驱为头结点, 且存在排队线程st
        if (((h = head) != null && (s = h.next) != null &&
             s.prev == head && (st = s.thread) != null) ||
             // 如果不存在, 则再检查一次: 头结点h, 头结点后继s作为第一个结点, 如果s前驱为头结点, 且存在排队线程st
            ((h = head) != null && (s = h.next) != null &&
             s.prev == head && (st = s.thread) != null))
            // 检查两次了两次, 如果排队线程st存在, 则返回st作为CLH中的第一个排队线程
            return st;

        /**
         * 20210724
         * Head的next字段可能尚未设置，或者可能在setHead之后未设置。所以我们必须检查tail是否实际上是第一个节点。
         * 如果没有，我们继续，安全地从尾部回到头部找到第一个，保证终止。
         */
        /*
         * Head's next field might not have been set yet, or may have
         * been unset after setHead. So we must check to see if tail
         * is actually first node. If not, we continue on, safely
         * traversing from tail back to head to find first,
         * guaranteeing termination.
         */
        // 如果检查了两次都没找到第一个排队线程, 则获取最新的尾结点, 从后往前找
        Node t = tail;
        Thread firstThread = null;

        // 如果t不为null, 且不为头结点, 则停止向前遍历, 即向前找到第一个有效的不为头结点的排队结点
        while (t != null && t != head) {
            Thread tt = t.thread;
            if (tt != null)
                firstThread = tt;
            t = t.prev;
        }

        // 返回找到的第一个有效的不为头结点的排队结点的排队线程, 作为CLH中的第一个排队线程
        return firstThread;
    }

    /**
     * 20210724
     * A. 如果给定线程当前正在排队，则返回true。
     * B. 此实现遍历队列以确定给定线程的存在。
     */
    /**
     * A.
     * Returns true if the given thread is currently queued.
     *
     * B.
     * <p>This implementation traverses the queue to determine
     * presence of the given thread.
     *
     * @param thread the thread
     * @return {@code true} if the given thread is on the queue // {@code true} 如果给定的线程在队列中
     * @throws NullPointerException if the thread is null
     */
    // 判断目标线程是否在CLH队列中: 从尾往前遍历, 如果Thread为同一个地址, 在返回true, 否则返回false
    public final boolean isQueued(Thread thread) {
        if (thread == null)
            throw new NullPointerException();

        // 从尾往前遍历, 如果Thread为同一个地址, 在返回true, 发欧泽返回false
        for (Node p = tail; p != null; p = p.prev)
            if (p.thread == thread)
                return true;
        return false;
    }

    /**
     * 20210724
     * 如果明显的第一个排队线程（如果存在）正在以独占模式等待，则返回 {@code true}。如果此方法返回{@code true}，
     * 并且当前线程试图以共享模式获取（即此方法是从{@link #tryAcquireShared} 调用的），则保证当前线程不是第一个排队线程。
     * 仅用作ReentrantReadWriteLock中的启发式方法。
     */
    /**
     * Returns {@code true} if the apparent first queued thread, if one
     * exists, is waiting in exclusive mode.  If this method returns
     * {@code true}, and the current thread is attempting to acquire in
     * shared mode (that is, this method is invoked from {@link
     * #tryAcquireShared}) then it is guaranteed that the current thread
     * is not the first queued thread.  Used only as a heuristic in
     * ReentrantReadWriteLock.
     */
    // 判断第一个排队线程是否以独占模式等待
    final boolean apparentlyFirstQueuedIsExclusive() {
        Node h, s;

        // 头结点h, 头结点后继s, 判断下一个等待条件的节点是否为特殊值SHARED, 如果不是且s的排队线程不为null, 则返回true; 否则返回false
        return (h = head) != null &&
            (s = h.next)  != null &&
            !s.isShared()         &&
            s.thread != null;
    }

    /**
     * 20210724
     * A. 查询是否有任何线程等待获取的时间比当前线程长。
     * B. 调用此方法等效于（但可能比）：
     * {@code
     *  getFirstQueuedThread() != Thread.currentThread() && asQueuedThreads()
     * }
     * C. 请注意，由于中断和超时导致的取消随时可能发生，因此{@code true}返回并不能保证其他线程会在当前线程之前获取。
     *    同样，由于队列为空，在此方法返回{@code false}后，另一个线程可能会赢得排队竞争。
     * D. 此方法旨在由公平同步器使用以避免插入。这样一个同步器的{@link #tryAcquire}方法应该返回 {@code false}，它的{@link #tryAcquireShared}方法应该返回一个负值，
     *    如果这个方法返回{@code true}（除非这是一个可重入获取）. 例如，公平、可重入、独占模式同步器的 {@code tryAcquire} 方法可能如下所示：
     * {@code
     * protected boolean tryAcquire(int arg) {
     *   if (isHeldExclusively()) {
     *     // A reentrant acquire; increment hold count
     *     return true;
     *   } else if (hasQueuedPredecessors()) {
     *     return false;
     *   } else {
     *     // try to acquire normally
     *   }
     * }}
     */
    /**
     * A.
     * Queries whether any threads have been waiting to acquire longer
     * than the current thread.
     *
     * B.
     * <p>An invocation of this method is equivalent to (but may be
     * more efficient than):
     *  <pre> {@code
     * getFirstQueuedThread() != Thread.currentThread() &&
     * hasQueuedThreads()}</pre>
     *
     * C.
     * <p>Note that because cancellations due to interrupts and
     * timeouts may occur at any time, a {@code true} return does not
     * guarantee that some other thread will acquire before the current
     * thread.  Likewise, it is possible for another thread to win a
     * race to enqueue after this method has returned {@code false},
     * due to the queue being empty.
     *
     * D.
     * <p>This method is designed to be used by a fair synchronizer to
     * avoid <a href="AbstractQueuedSynchronizer#barging">barging</a>.
     * Such a synchronizer's {@link #tryAcquire} method should return
     * {@code false}, and its {@link #tryAcquireShared} method should
     * return a negative value, if this method returns {@code true}
     * (unless this is a reentrant acquire).  For example, the {@code
     * tryAcquire} method for a fair, reentrant, exclusive mode
     * synchronizer might look like this:
     *
     *  <pre> {@code
     * protected boolean tryAcquire(int arg) {
     *   if (isHeldExclusively()) {
     *     // A reentrant acquire; increment hold count
     *     return true;
     *   } else if (hasQueuedPredecessors()) {
     *     return false;
     *   } else {
     *     // try to acquire normally
     *   }
     * }}</pre>
     *
     * // {@code true} 如果当前线程之前有一个排队线程，如果当前线程在队列的头部或队列为空，则 {@code false}
     * @return {@code true} if there is a queued thread preceding the
     *         current thread, and {@code false} if the current thread
     *         is at the head of the queue or the queue is empty
     * @since 1.7
     */
    // 判断此刻CLH中是否存在有等待时间比当前线程长的线程, 旨在由公平同步器使用, 以避免插入结点到CLH队列中
    public final boolean hasQueuedPredecessors() {
        // 这正确性取决于head在tail之前被初始化，并且head.next如果当前线程在队列中是第一个则是准确的。
        // The correctness of this depends on head being initialized
        // before tail and on head.next being accurate if the current
        // thread is first in queue.
        Node t = tail; // Read fields in reverse initialization order 以相反的初始化顺序读取字段
        Node h = head;
        Node s;

        // 如果CLH队列中多于一个结点, 如果头后继s刚刚释放, 或者头后继s的等待线程不为当前线程, 说明此刻CLH中确实存在有等待时间比当前线程长的线程
        return h != t && ((s = h.next) == null || s.thread != Thread.currentThread());
    }


    // Instrumentation and monitoring methods 仪器仪表和监测方法

    /**
     * 20210724
     * 获取等待获取的线程数的估计值。该值只是一个估计值，因为当此方法遍历内部数据结构时，线程数可能会动态变化。该方法设计用于监视系统状态，而不是用于同步控制。
     */
    /**
     * Returns an estimate of the number of threads waiting to
     * acquire.  The value is only an estimate because the number of
     * threads may change dynamically while this method traverses
     * internal data structures.  This method is designed for use in
     * monitoring system state, not for synchronization
     * control.
     *
     * @return the estimated number of threads waiting to acquire 等待获取的估计线程数
     */
    // 从尾向前遍历统计非null的排队线程个数
    public final int getQueueLength() {
        int n = 0;

        // 从尾向前遍历统计非null的排队线程个数
        for (Node p = tail; p != null; p = p.prev) {
            if (p.thread != null)
                ++n;
        }
        return n;
    }

    /**
     * 20210724
     * 返回一个包含可能正在等待获取的线程的集合。由于在构造此结果时实际线程集可能会动态更改，因此返回的集合只是尽力而为的估计。返回集合的元素没有特定的顺序。
     * 此方法旨在促进子类的构建，以提供更广泛的监视设施。
     */
    /**
     * Returns a collection containing threads that may be waiting to
     * acquire.  Because the actual set of threads may change
     * dynamically while constructing this result, the returned
     * collection is only a best-effort estimate.  The elements of the
     * returned collection are in no particular order.  This method is
     * designed to facilitate construction of subclasses that provide
     * more extensive monitoring facilities.
     *
     * @return the collection of threads
     */
    // 从尾向前遍历获取非null的排队线程列表
    public final Collection<Thread> getQueuedThreads() {
        ArrayList<Thread> list = new ArrayList<Thread>();

        // 从尾向前遍历获取非null的排队线程列表
        for (Node p = tail; p != null; p = p.prev) {
            Thread t = p.thread;
            if (t != null)
                list.add(t);
        }
        return list;
    }

    /**
     * 20210724
     * 返回一个包含可能在独占模式下等待获取的线程的集合。这与 {@link #getQueuedThreads} 具有相同的属性，除了它只返回由于独占获取而等待的线程。
     */
    /**
     * Returns a collection containing threads that may be waiting to
     * acquire in exclusive mode. This has the same properties
     * as {@link #getQueuedThreads} except that it only returns
     * those threads waiting due to an exclusive acquire.
     *
     * @return the collection of threads
     */
    // 从尾向前遍历获取非共享的排队线程列表
    public final Collection<Thread> getExclusiveQueuedThreads() {
        ArrayList<Thread> list = new ArrayList<Thread>();

        // 从尾向前遍历获取非共享的排队线程列表
        for (Node p = tail; p != null; p = p.prev) {
            if (!p.isShared()) {
                Thread t = p.thread;
                if (t != null)
                    list.add(t);
            }
        }
        return list;
    }

    /**
     * 20210724
     * 返回一个包含可能在共享模式下等待获取的线程的集合。 这与 {@link #getQueuedThreads} 具有相同的属性，除了它只返回由于共享获取而等待的线程。
     */
    /**
     * Returns a collection containing threads that may be waiting to
     * acquire in shared mode. This has the same properties
     * as {@link #getQueuedThreads} except that it only returns
     * those threads waiting due to a shared acquire.
     *
     * @return the collection of threads
     */
    // 从尾向前遍历获取共享的排队线程列表
    public final Collection<Thread> getSharedQueuedThreads() {
        ArrayList<Thread> list = new ArrayList<Thread>();

        // 从尾向前遍历获取共享的排队线程列表
        for (Node p = tail; p != null; p = p.prev) {
            if (p.isShared()) {
                Thread t = p.thread;
                if (t != null)
                    list.add(t);
            }
        }
        return list;
    }

    /**
     * 20210724
     * 返回标识此同步器及其状态的字符串。括号中的状态包括字符串{@code "State ="}后跟{@link #getState} 的当前值，
     * 以及{@code "nonempty"}或{@code "empty"} 取决于是否队列是空的。
     */
    /**
     * Returns a string identifying this synchronizer, as well as its state.
     * The state, in brackets, includes the String {@code "State ="}
     * followed by the current value of {@link #getState}, and either
     * {@code "nonempty"} or {@code "empty"} depending on whether the
     * queue is empty.
     *
     * @return a string identifying this synchronizer, as well as its state
     */
    // 返回标识此同步器及其状态的字符串
    public String toString() {
        int s = getState();
        String q  = hasQueuedThreads() ? "non" : "";
        return super.toString() +
            "[State = " + s + ", " + q + "empty queue]";
    }


    // Internal support methods for Conditions // Conditions的内部支持方法

    /**
     * 20210724
     * 如果一个节点（始终是最初放置在条件队列中的节点）现在正在等待重新获取同步队列，则返回true。
     */
    /**
     * Returns true if a node, always one that was initially placed on
     * a condition queue, is now waiting to reacquire on sync queue.
     *
     * @param node the node
     * @return true if is reacquiring 如果重新获取则为真
     */
    // 判断node结点的是不是在公平队列中排队
    final boolean isOnSyncQueue(Node node) {
        // 如果node为条件结点, 或者node为头结点则返回false
        if (node.waitStatus == Node.CONDITION || node.prev == null)
            return false;

        // 如果有前驱不为头结点, 且有后继时，则认为node结点肯定在队列中, 此时返回true
        if (node.next != null) // If has successor, it must be on queue
            return true;

        /**
         * 20210724
         * node.prev可以为非null，但尚未在队列中，因为将其放入队列的CAS可能会失败。所以我们必须从尾部遍历以确保它确实做到了。在调用这个方法时它总是靠近尾部，
         * 除非CAS失败（这不太可能），它会在那里，所以我们几乎不会遍历太多。
         */
        /*
         * node.prev can be non-null, but not yet on queue because
         * the CAS to place it on queue can fail. So we have to
         * traverse from tail to make sure it actually made it.  It
         * will always be near the tail in calls to this method, and
         * unless the CAS failed (which is unlikely), it will be
         * there, so we hardly ever traverse much.
         */
        // 如果有前驱不为头结点, 且没有后继, 可能该结点CAS失败还没入队(即设置了前驱但CAS后继失败时), 则获取最新尾结点开始自旋向前找, 如果找到对应的结点则返回true, 如果找不到则返回false
        return findNodeFromTail(node);
    }

    /**
     * 20210724
     * 如果通过从尾部向后搜索节点在同步队列上，则返回true。仅在isOnSyncQueue 需要时调用。
     */
    /**
     * Returns true if node is on sync queue by searching backwards from tail.
     * Called only when needed by isOnSyncQueue.
     *
     * @return true if present
     */
    // 获取最新尾结点开始自旋向前找, 如果找到对应的结点则返回true, 如果找不到则返回false
    private boolean findNodeFromTail(Node node) {
        Node t = tail;

        // 获取最新尾结点开始自旋向前找, 如果找到对应的结点则返回true, 如果找不到则返回false
        for (;;) {
            if (t == node)
                return true;
            if (t == null)
                return false;
            t = t.prev;
        }
    }

    /**
     * 20210725
     * 将节点从条件队列转移到同步队列。 如果成功则返回真。
     */
    /**
     * Transfers a node from a condition queue onto sync queue.
     * Returns true if successful.
     *
     * @param node the node
     *
     * // 如果成功传输则为真（否则节点在发出信号之前被取消）
     * @return true if successfully transferred (else the node was cancelled before signal)
     */
    // 将node从条件队列转移到了AQS同步队列, 入队后|入队唤醒成功后, 则返回true
    final boolean transferForSignal(Node node) {
        /*
         * If cannot change waitStatus, the node has been cancelled.
         * 如果无法更改waitStatus，则该节点已被取消。
         */
        // CAS更新为普通结点, 如果CAS更新失败, 则返回false, 代表结点已被更新(取消)
        if (!compareAndSetWaitStatus(node, Node.CONDITION, 0))
            return false;

        /**
         * 20210725
         * 拼接到队列并尝试设置前驱的waitStatus以指示线程（可能）正在等待。
         * 如果取消或尝试设置waitStatus失败，则唤醒以重新同步（在这种情况下，waitStatus可能是暂时且无害的错误）。
         */
        /*
         * Splice onto queue and try to set waitStatus of predecessor to
         * indicate that thread is (probably) waiting. If cancelled or
         * attempt to set waitStatus fails, wake up to resync (in which
         * case the waitStatus can be transiently and harmlessly wrong).
         */
        // node结点(含排队线程)自旋入队方法, 自旋 + CAS入队, 如果队列还没初始化则需要初始化
        Node p = enq(node);

        // node结点入队成功后, 如果结点状态为已取消, 或者CAS更新node结点为通知结点失败, 则唤醒node的排队线程
        int ws = p.waitStatus;
        if (ws > 0 || !compareAndSetWaitStatus(p, ws, Node.SIGNAL))
            LockSupport.unpark(node.thread);

        // 入队后|入队唤醒成功后, 则返回true, 代表node成功从条件队列转移到了同步队列
        return true;
    }

    /**
     * 20210725
     * 如有必要，在取消等待后将节点传输到同步队列。 如果线程在发出信号之前被取消，则返回 true。
     */
    /**
     * Transfers node, if necessary, to sync queue after a cancelled wait.
     * Returns true if thread was cancelled before being signalled.
     *
     * @param node the node
     *
     * // 如果在节点收到信号之前取消，则为 true
     * @return true if cancelled before the node was signalled
     */
    // 将已经取消了等待的节点转移到同步队列, 如果转移成功则返回true, 失败则返回false
    final boolean transferAfterCancelledWait(Node node) {
        // CAS更新为普通结点
        if (compareAndSetWaitStatus(node, Node.CONDITION, 0)) {
            // 如果CAS更新成功, 则node结点(含排队线程)自旋入队方法, 自旋 + CAS入队, 如果队列还没初始化则需要初始化
            enq(node);

            // 入队成功后返回true
            return true;
        }

        /**
         * 20210725
         * 如果我们输给了一个signal()，那么在它完成enq()之前我们不能继续。在不完整的转移期间取消既罕见又短暂，因此只需旋转即可。
         */
        /*
         * If we lost out to a signal(), then we can't proceed
         * until it finishes its enq().  Cancelling during an
         * incomplete transfer is both rare and transient, so just
         * spin.
         */
        // 判断node结点的是不是在公平队列中排队, 如果不在则继续让步+自旋
        while (!isOnSyncQueue(node))
            Thread.yield();

        // 如果node结点在公平队列中排队了, 则返回false
        return false;
    }

    /**
     * 20210725
     * 使用当前状态值调用release；返回保存状态。取消节点并在失败时抛出异常。
     */
    /**
     * Invokes release with current state value; returns saved state.
     * Cancels node and throws exception on failure.
     *
     * @param node the condition node for this wait
     * @return previous sync state 之前的同步状态
     */
    // volatile方式获取同步状态并尝试释放独占模式的同步器, 如果释放成功, 则返回刚刚获取到的同步状态; 如果失败则抛出异常后更改结点为取消结点
    final int fullyRelease(Node node) {
        boolean failed = true;
        try {
            // volatile方式获取同步状态
            int savedState = getState();

            // 尝试释放独占模式的同步器, 如果释放成功, 则返回刚刚获取到的同步状态
            if (release(savedState)) {
                failed = false;
                return savedState;
            }
            // 如果释放失败, 则抛出IllegalMonitorStateException异常
            else {
                throw new IllegalMonitorStateException();
            }
        } finally {
            // 最后如果释放失败, 还需要volatile方式更改结点为取消结点
            if (failed)
                node.waitStatus = Node.CANCELLED;
        }
    }

    // Instrumentation methods for conditions conditions检测方法

    /**
     * 20210725
     * 查询给定的ConditionObject是否使用此同步器作为其锁。
     */
    /**
     * Queries whether the given ConditionObject
     * uses this synchronizer as its lock.
     *
     * @param condition the condition
     * @return {@code true} if owned
     * @throws NullPointerException if the condition is null
     */
    // 查询给定的ConditionObject是否使用此同步器作为其锁
    public final boolean owns(ConditionObject condition) {
        return condition.isOwnedBy(this);
    }

    /**
     * 20210725
     * 查询是否有任何线程正在等待与此同步器关联的给定条件。 请注意，由于超时和中断可能随时发生，因此 {@code true} 返回并不能保证未来的 {@code signal} 会唤醒任何线程。 该方法主要设计用于监视系统状态。
     */
    /**
     * Queries whether any threads are waiting on the given condition
     * associated with this synchronizer. Note that because timeouts
     * and interrupts may occur at any time, a {@code true} return
     * does not guarantee that a future {@code signal} will awaken
     * any threads.  This method is designed primarily for use in
     * monitoring of the system state.
     *
     * @param condition the condition
     *
     * // {@code true} 如果有任何等待线程
     * @return {@code true} if there are any waiting threads
     *
     * @throws IllegalMonitorStateException if exclusive synchronization
     *         is not held
     * @throws IllegalArgumentException if the given condition is
     *         not associated with this synchronizer
     * @throws NullPointerException if the condition is null
     */
    // 查询是否有任何线程正在等待与此同步器关联的给定条件
    public final boolean hasWaiters(ConditionObject condition) {
        if (!owns(condition))
            throw new IllegalArgumentException("Not owner");
        return condition.hasWaiters();
    }

    // 返回等待与此同步器关联的给定条件的线程数的估计值。
    // 请注意，由于超时和中断可能随时发生，因此估计值仅用作实际服务员人数的上限。 此方法设计用于监视系统状态，而不是用于同步控制。
    /**
     * Returns an estimate of the number of threads waiting on the
     * given condition associated with this synchronizer. Note that
     * because timeouts and interrupts may occur at any time, the
     * estimate serves only as an upper bound on the actual number of
     * waiters.  This method is designed for use in monitoring of the
     * system state, not for synchronization control.
     *
     * @param condition the condition
     * @return the estimated number of waiting threads
     * @throws IllegalMonitorStateException if exclusive synchronization
     *         is not held
     * @throws IllegalArgumentException if the given condition is
     *         not associated with this synchronizer
     * @throws NullPointerException if the condition is null
     */
    // 返回等待与此同步器关联的给定条件的线程数的估计值
    public final int getWaitQueueLength(ConditionObject condition) {
        if (!owns(condition))
            throw new IllegalArgumentException("Not owner");
        return condition.getWaitQueueLength();
    }

    /**
     * 20210809
     * 返回一个包含可能正在等待与此同步器关联的给定条件的线程的集合。
     * 由于在构造此结果时实际线程集可能会动态更改，因此返回的集合只是尽力而为的估计。 返回集合的元素没有特定的顺序。
     */
    /**
     * Returns a collection containing those threads that may be
     * waiting on the given condition associated with this
     * synchronizer.  Because the actual set of threads may change
     * dynamically while constructing this result, the returned
     * collection is only a best-effort estimate. The elements of the
     * returned collection are in no particular order.
     *
     * @param condition the condition
     * @return the collection of threads
     * @throws IllegalMonitorStateException if exclusive synchronization
     *         is not held
     * @throws IllegalArgumentException if the given condition is
     *         not associated with this synchronizer
     * @throws NullPointerException if the condition is null
     */
    // 返回一个包含可能正在等待与此同步器关联的给定条件的线程的集合
    public final Collection<Thread> getWaitingThreads(ConditionObject condition) {
        if (!owns(condition))
            throw new IllegalArgumentException("Not owner");

        // 返回一个包含可能正在等待此Condition的线程的集合 => 从头遍历条件队列, 收集CONDITION结点的线程
        return condition.getWaitingThreads();
    }

    /**
     * 20210725
     * A. 作为{@link Lock}实现基础的{@link AbstractQueuedSynchronizer}的条件实现。
     * B. 此类的方法文档从锁定和条件用户的角度描述了机制，而不是行为规范。 此类的导出版本通常需要随附描述依赖于关联{@code AbstractQueuedSynchronizer}的条件语义的文档。
     * C. 此类是可序列化的，但所有字段都是瞬态的，因此反序列化的条件没有等待者。
     */
    /**
     * A.
     * Condition implementation for a {@link
     * AbstractQueuedSynchronizer} serving as the basis of a {@link
     * Lock} implementation.
     *
     * B.
     * <p>Method documentation for this class describes mechanics,
     * not behavioral specifications from the point of view of Lock
     * and Condition users. Exported versions of this class will in
     * general need to be accompanied by documentation describing
     * condition semantics that rely on those of the associated
     * {@code AbstractQueuedSynchronizer}.
     *
     * C.
     * <p>This class is Serializable, but all fields are transient,
     * so deserialized conditions have no waiters.
     */
    // AQS实现的Condition接口实现类
    public class ConditionObject implements Condition, java.io.Serializable {

        private static final long serialVersionUID = 1173984872572414699L;

        // 条件队列的第一个节点。
        /** First node of condition queue. */
        private transient Node firstWaiter;

        // 条件队列的最后一个节点。
        /** Last node of condition queue. */
        private transient Node lastWaiter;

        /**
         * Creates a new {@code ConditionObject} instance.
         */
        // 创建一个新的 {@code ConditionObject} 实例。
        public ConditionObject() { }

        // Internal methods 内部方法

        /**
         * 20210725
         * 添加一个新的服务员到等待队列。
         */
        /**
         * Adds a new waiter to wait queue.
         *
         * @return its new wait node 新的等待节点
         */
        // 添加一个条件结点, 如果最后一个条件结点t不是条件结点, 则会从条件队列中取消非条件结点, 将其nextWaiter链接到最后一个条件结点末尾
        private Node addConditionWaiter() {
            // 获取条件队列的最后一个节点t
            Node t = lastWaiter;

            // If lastWaiter is cancelled, clean out. // 如果lastWaiter被取消，则清除。
            // 如果最后一个条件结点t不是条件结点
            if (t != null && t.waitStatus != Node.CONDITION) {
                // 从条件队列中取消非条件结点, 将其nextWaiter链接到最后一个条件结点末尾
                unlinkCancelledWaiters();

                // 获取最后一个条件结点
                t = lastWaiter;
            }

            // 如果最后一个条件结点仍然是条件结点, 或者条件队列经过整理后, 则使用当前线程构造条件结点node
            Node node = new Node(Thread.currentThread(), Node.CONDITION);

            // 更新node为最后一个条件结点
            if (t == null)
                firstWaiter = node;
            else
                t.nextWaiter = node;
            lastWaiter = node;

            // 返回新构建的node条件结点
            return node;
        }

        // 删除并传输节点，直到命中未取消的一个结点或为null的结点。从信号中分离出来部分是为了鼓励编译器内联没有waiter的情况。
        /**
         * Removes and transfers nodes until hit non-cancelled one or
         * null. Split out from signal in part to encourage compilers
         * to inline the case of no waiters.
         *
         * // （非空）条件队列上的第一个节点
         * @param first (non-null) the first node on condition queue
         */
        // 删除并传输节点到AQS同步队列, 直到命中未取消的一个结点(第一个转移成功的)或为null的(结尾的)结点
        private void doSignal(Node first) {
            do {
                // 遍历first结点的条件队列节点, 如果为null, 则清空该结点的条件队列
                if ( (firstWaiter = first.nextWaiter) == null)
                    lastWaiter = null;
                first.nextWaiter = null;
            } while (
                    // 清空后, 则将first结点从条件队列转移到了AQS同步队列, 入队后|入队唤醒成功后, 则返回true, 此时直接返回, 代表转移一次first结点即可
                    !transferForSignal(first) &&
                     // 如果CAS失败, 说明first结点转移失败, 则继续转移通知原first条件队列的结点, 直到结尾
                     (first = firstWaiter) != null);
        }

        // 删除并转移所有节点。
        /**
         * Removes and transfers all nodes.
         *
         * @param first (non-null) the first node on condition queue
         */
        // 遍历、清空、转移first结点的排队线程到AQS同步队列
        private void doSignalAll(Node first) {
            lastWaiter = firstWaiter = null;
            do {
                Node next = first.nextWaiter;
                first.nextWaiter = null;

                // 将first从条件队列转移到了AQS同步队列, 入队后|入队唤醒成功后, 则返回true
                transferForSignal(first);

                // 遍历first结点的排队线程
                first = next;
            } while (first != null);
        }

        /**
         * 20210725
         * 从条件队列中取消链接已取消的等待节点。仅在持有锁时调用。当在条件等待期间发生取消时，以及在看到lastWaiter已被取消时插入新的服务员时，将调用此方法。
         * 需要这种方法来避免在没有信号的情况下垃圾保留。因此，即使它可能需要完全遍历，它也仅在没有信号的情况下发生超时或取消时才起作用。
         * 它遍历所有节点而不是在特定目标处停止以取消所有指向垃圾节点的指针的链接，而无需在取消风暴期间进行多次重新遍历。
         */
        /**
         * Unlinks cancelled waiter nodes from condition queue.
         * Called only while holding lock. This is called when
         * cancellation occurred during condition wait, and upon
         * insertion of a new waiter when lastWaiter is seen to have
         * been cancelled. This method is needed to avoid garbage
         * retention in the absence of signals. So even though it may
         * require a full traversal, it comes into play only when
         * timeouts or cancellations occur in the absence of
         * signals. It traverses all nodes rather than stopping at a
         * particular target to unlink all pointers to garbage nodes
         * without requiring many re-traversals during cancellation
         * storms.
         */
        // 从条件队列中取消非条件结点, 将其nextWaiter链接到最后一个条件结点末尾
        private void unlinkCancelledWaiters() {
            // 条件队列的第一个节点t
            Node t = firstWaiter;
            Node trail = null;

            // 从头开始遍历原条件队列链表
            while (t != null) {
                // 条件队列的下一个节点next
                Node next = t.nextWaiter;

                // 如果结点不为条件结点
                if (t.waitStatus != Node.CONDITION) {
                    // 清空原下一个结点
                    t.nextWaiter = null;

                    // 使用next结点重新构造新的条件trail链表
                    if (trail == null)
                        firstWaiter = next;
                    else
                        trail.nextWaiter = next;

                    // 遍历原链表尾部后, 追加新构造的链表到最后一个条件结点末尾
                    if (next == null)
                        lastWaiter = trail;
                }
                // 如果为条件结点, 则更换trail链头, 代表使用最后一个条件结点
                else
                    trail = t;

                // 继续遍历原条件队列链表
                t = next;
            }
        }

        // public methods

        // 将等待时间最长的线程（如果存在）从此条件的等待队列移动到拥有锁的等待队列。
        /**
         * Moves the longest-waiting thread, if one exists, from the
         * wait queue for this condition to the wait queue for the
         * owning lock.
         *
         * @throws IllegalMonitorStateException if {@link #isHeldExclusively}
         *         returns {@code false}
         */
        // 独占模式下唤醒第一个条件等待队列结点
        public final void signal() {
            // 判断当前线程是否为独占的线程, 如果不是则抛出异常
            if (!isHeldExclusively())
                throw new IllegalMonitorStateException();

            // 获取第一个条件等待队列结点first
            Node first = firstWaiter;
            if (first != null)
                // 删除并传输first节点到AQS同步队列, 直到命中未取消的一个结点(第一个转移成功的)或为null的(结尾的)结点
                doSignal(first);
        }

        // 将所有线程从此条件的等待队列移动到拥有锁的等待队列。
        /**
         * Moves all threads from the wait queue for this condition to
         * the wait queue for the owning lock.
         *
         * @throws IllegalMonitorStateException if {@link #isHeldExclusively}
         *         returns {@code false}
         */
        // 独占模式下唤醒所有条件等待队列结点
        public final void signalAll() {
            // 判断当前线程是否为独占的线程, 如果不是则抛出异常
            if (!isHeldExclusively())
                throw new IllegalMonitorStateException();

            // 获取第一个条件等待队列结点first
            Node first = firstWaiter;
            if (first != null)
                // 遍历、清空、转移first结点的排队线程到AQS同步队列
                doSignalAll(first);
        }

        /**
         * 20210813
         * 实现不间断条件等待：
         *      1. 保存 {@link #getState} 返回的锁状态。
         *      2. 使用保存的状态作为参数调用 {@link #release}，如果失败则抛出 IllegalMonitorStateException。
         *      3. 阻塞直到发出信号。
         *      4. 通过调用特殊版本的 {@link #acquire} 以保存的状态作为参数重新获取。
         */
        /**
         * Implements uninterruptible condition wait.
         * <ol>
         * <li> Save lock state returned by {@link #getState}.
         * <li> Invoke {@link #release} with saved state as argument,
         *      throwing IllegalMonitorStateException if it fails.
         * <li> Block until signalled.
         * <li> Reacquire by invoking specialized version of
         *      {@link #acquire} with saved state as argument.
         * </ol>
         */
        public final void awaitUninterruptibly() {
            // 添加一个条件结点, 如果最后一个条件结点t不是条件结点, 则会从条件队列中取消非条件结点, 将其nextWaiter链接到最后一个条件结点末尾
            Node node = addConditionWaiter();

            // volatile方式获取同步状态并尝试释放独占模式的同步器, 如果释放成功, 则返回刚刚获取到的同步状态; 如果失败则抛出异常后更改结点为取消结点
            int savedState = fullyRelease(node);

            // 判断node结点的是不是在公平队列中排队, 如果不是则阻塞当前线程
            boolean interrupted = false;
            while (!isOnSyncQueue(node)) {
                LockSupport.park(this);

                // 如果当前线程被唤醒, 则判断线程是否已中断, 如果是则更新中断标志
                if (Thread.interrupted())
                    interrupted = true;
            }

            // 如果在同步队列中, 则独占式自旋入队的核心逻辑, 如果node前驱刚释放, 则更新node为头结点且不用阻塞直接返回;
            // 否则放置node结点并阻塞其排队线程, 如果当前线程有被中断过, 则还需要继续自旋, 直到获取到同步器, 返回true代表有被中断过让上层API处理;
            // 另外如果调用tryAcquire方法时发生了异常, 则还需要则取消node结点
            if (acquireQueued(node, savedState) || interrupted)
                // 中断该线程, 如果从Thread其他实例方法调用该方法, 则会清除中断状态, 然后会收到一个{@link InterruptedException}
                selfInterrupt();
        }

        // 对于可中断的等待，我们需要跟踪是否抛出 InterruptedException，如果在条件阻塞时中断，与重新中断当前线程，如果在阻塞等待重新获取时中断。
        /*
         * For interruptible waits, we need to track whether to throw
         * InterruptedException, if interrupted while blocked on
         * condition, versus reinterrupt current thread, if
         * interrupted while blocked waiting to re-acquire.
         */

        // 模式意味着在退出等待时重新中断
        /** Mode meaning to reinterrupt on exit from wait */
        private static final int REINTERRUPT =  1;

        // 模式意味着在退出等待时抛出 InterruptedException
        /** Mode meaning to throw InterruptedException on exit from wait */
        private static final int THROW_IE    = -1;

        // 检查中断，如果在发出信号之前中断，则返回 THROW_IE，如果发出信号则返回 REINTERRUPT，如果未中断则返回 0。
        /**
         * Checks for interrupt, returning THROW_IE if interrupted
         * before signalled, REINTERRUPT if after signalled, or
         * 0 if not interrupted.
         */
        // 检查是否存在等待时中断, 如果当前线程被中断, 则将等待结点转移到同步队列, 如果转移成功则返回THROW_IE(-1), 如果转移失败则返回REINTERRUPT(1); 如果等待时没有发生异常, 则返回0
        private int checkInterruptWhileWaiting(Node node) {
            // 测试当前线程是否被中断, 该方法会清除线程的中断状态
            return Thread.interrupted() ?
                    // 将已经取消了等待的节点转移到同步队列, 如果转移成功则返回true, 失败则返回false
                    (transferAfterCancelledWait(node) ? THROW_IE : REINTERRUPT) : 0;
        }

        // 根据模式，抛出 InterruptedException、重新中断当前线程或不执行任何操作。
        /**
         * Throws InterruptedException, reinterrupts current thread, or
         * does nothing, depending on mode.
         */
        // 根据模式，抛出 InterruptedException、重新中断当前线程或不执行任何操作
        private void reportInterruptAfterWait(int interruptMode) throws InterruptedException {
            // 模式意味着在退出等待时抛出 InterruptedException
            if (interruptMode == THROW_IE)
                throw new InterruptedException();
            // 模式意味着在退出等待时重新中断
            else if (interruptMode == REINTERRUPT)
                // 中断该线程, 如果从Thread其他实例方法调用该方法, 则会清除中断状态, 然后会收到一个{@link InterruptedException}
                selfInterrupt();
        }

        /**
         * 20210813
         * 实现可中断条件等待:
         *      1. 如果当前线程被中断，则抛出 InterruptedException。
         *      2. 保存 {@link #getState} 返回的锁状态。
         *      3. 使用保存的状态作为参数调用 {@link #release}，如果失败则抛出 IllegalMonitorStateException。
         *      4. 阻塞直到发出信号或被中断。
         *      5. 通过调用特殊版本的 {@link #acquire} 以保存的状态作为参数重新获取。
         *      6. 如果在步骤 4 中被阻塞时被中断，则抛出 InterruptedException。
         */
        /**
         * Implements interruptible condition wait.
         * <ol>
         * <li> If current thread is interrupted, throw InterruptedException.
         * <li> Save lock state returned by {@link #getState}.
         * <li> Invoke {@link #release} with saved state as argument,
         *      throwing IllegalMonitorStateException if it fails.
         * <li> Block until signalled or interrupted.
         * <li> Reacquire by invoking specialized version of
         *      {@link #acquire} with saved state as argument.
         * <li> If interrupted while blocked in step 4, throw InterruptedException.
         * </ol>
         */
        // 阻塞等待当前线程, 如果线程被中断则加入同步队列, 且在获取到同步器后处理中断
        public final void await() throws InterruptedException {
            // 测试当前线程是否被中断, 该方法会清除线程的中断状态
            if (Thread.interrupted())
                throw new InterruptedException();

            // 添加一个条件结点, 如果最后一个条件结点t不是条件结点, 则会从条件队列中取消非条件结点, 将其nextWaiter链接到最后一个条件结点末尾
            Node node = addConditionWaiter();

            // volatile方式获取同步状态并尝试释放独占模式的同步器, 如果释放成功, 则返回刚刚获取到的同步状态; 如果失败则抛出异常后更改结点为取消结点
            int savedState = fullyRelease(node);

            // 判断node结点的是不是在公平队列中排队, 如果不是则阻塞当前线程
            int interruptMode = 0;
            while (!isOnSyncQueue(node)) {
                LockSupport.park(this);

                // 检查是否存在等待时中断, 如果当前线程被中断, 则将等待结点转移到同步队列, 如果转移成功则返回THROW_IE(-1), 如果转移失败则返回REINTERRUPT(1); 如果等待时没有发生异常, 则返回0
                if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
                    // 如果抛出了异常, 则退出自旋
                    break;
            }

            // 如果在同步队列中, 则独占式自旋入队的核心逻辑, 如果node前驱刚释放, 则更新node为头结点且不用阻塞直接返回;
            // 否则放置node结点并阻塞其排队线程, 如果当前线程有被中断过, 则还需要继续自旋, 直到获取到同步器, 返回true代表有被中断过让上层API处理;
            // 另外如果调用tryAcquire方法时发生了异常, 则还需要则取消node结点
            if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
                interruptMode = REINTERRUPT;

            // 从条件队列中取消非条件结点, 将其nextWaiter链接到最后一个条件结点末尾
            if (node.nextWaiter != null) // clean up if cancelled 如果取消则清理
                unlinkCancelledWaiters();

            // 如果等待期间发生中断, 则抛出InterruptedException、重新中断当前线程或不执行任何操作
            if (interruptMode != 0)
                reportInterruptAfterWait(interruptMode);
        }

        /**
         * 20210813
         * A. 实现定时条件等待:
         *      1. 如果当前线程被中断，则抛出 InterruptedException。
         *      2. 保存 {@link #getState} 返回的锁状态。
         *      3. 使用保存的状态作为参数调用 {@link #release}，如果失败则抛出 IllegalMonitorStateException。
         *      4. 阻塞直到发出信号、中断或超时。
         *      5. 通过调用特殊版本的 {@link #acquire} 以保存的状态作为参数重新获取。
         *      6. 如果在步骤 4 中被阻塞时被中断，则抛出 InterruptedException。
         */
        /**
         * Implements timed condition wait.
         * <ol>
         * <li> If current thread is interrupted, throw InterruptedException.
         * <li> Save lock state returned by {@link #getState}.
         * <li> Invoke {@link #release} with saved state as argument,
         *      throwing IllegalMonitorStateException if it fails.
         * <li> Block until signalled, interrupted, or timed out.
         * <li> Reacquire by invoking specialized version of
         *      {@link #acquire} with saved state as argument.
         * <li> If interrupted while blocked in step 4, throw InterruptedException.
         * </ol>
         */
        // 定时阻塞等待当前线程, 如果线程被中断则加入同步队列, 且在获取到同步器后处理中断
        public final long awaitNanos(long nanosTimeout) throws InterruptedException {
            // 测试当前线程是否被中断, 该方法会清除线程的中断状态
            if (Thread.interrupted())
                throw new InterruptedException();

            // 添加一个条件结点, 如果最后一个条件结点t不是条件结点, 则会从条件队列中取消非条件结点, 将其nextWaiter链接到最后一个条件结点末尾
            Node node = addConditionWaiter();

            // volatile方式获取同步状态并尝试释放独占模式的同步器, 如果释放成功, 则返回刚刚获取到的同步状态; 如果失败则抛出异常后更改结点为取消结点
            int savedState = fullyRelease(node);

            // 判断node结点的是不是在公平队列中排队, 如果不是则阻塞当前线程
            final long deadline = System.nanoTime() + nanosTimeout;
            int interruptMode = 0;
            while (!isOnSyncQueue(node)) {
                // 如果超时时间小于0, 说明发生了超时, 将已经取消了等待的节点转移到同步队列, 如果转移成功则返回true, 失败则返回false
                if (nanosTimeout <= 0L) {
                    transferAfterCancelledWait(node);
                    break;
                }
                // 如果过期总时间仍大于1s钟, 说明仍然需要阻塞线程, 则调用LockSupport的定时阻塞方法阻塞当前线程
                if (nanosTimeout >= spinForTimeoutThreshold)
                    LockSupport.parkNanos(this, nanosTimeout);

                // 检查是否存在等待时中断, 如果当前线程被中断, 则将等待结点转移到同步队列, 如果转移成功则返回THROW_IE(-1), 如果转移失败则返回REINTERRUPT(1); 如果等待时没有发生异常, 则返回0
                if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
                    break;

                // 重新计算超时时间
                nanosTimeout = deadline - System.nanoTime();
            }

            // 如果在同步队列中, 则独占式自旋入队的核心逻辑, 如果node前驱刚释放, 则更新node为头结点且不用阻塞直接返回;
            // 否则放置node结点并阻塞其排队线程, 如果当前线程有被中断过, 则还需要继续自旋, 直到获取到同步器, 返回true代表有被中断过让上层API处理;
            // 另外如果调用tryAcquire方法时发生了异常, 则还需要则取消node结点
            if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
                interruptMode = REINTERRUPT;

            // 从条件队列中取消非条件结点, 将其nextWaiter链接到最后一个条件结点末尾
            if (node.nextWaiter != null)
                unlinkCancelledWaiters();

            // 如果等待期间发生中断, 则抛出InterruptedException、重新中断当前线程或不执行任何操作
            if (interruptMode != 0)
                reportInterruptAfterWait(interruptMode);

            // 返回剩余超时时间
            return deadline - System.nanoTime();
        }

        /**
         * 20210813
         * 实现绝对定时条件等待：
         *      1. 如果当前线程被中断，则抛出 InterruptedException。
         *      2. 保存 {@link #getState} 返回的锁状态。
         *      3. 使用保存的状态作为参数调用 {@link #release}，如果失败则抛出 IllegalMonitorStateException。
         *      4. 阻塞直到发出信号、中断或超时。
         *      5. 通过调用特殊版本的 {@link #acquire} 以保存的状态作为参数重新获取。
         *      6. 如果在步骤 4 中被阻塞时被中断，则抛出 InterruptedException。
         *      7. 如果在步骤 4 中阻塞时超时，则返回 false，否则返回 true。
         */
        /**
         * Implements absolute timed condition wait.
         * <ol>
         * <li> If current thread is interrupted, throw InterruptedException.
         * <li> Save lock state returned by {@link #getState}.
         * <li> Invoke {@link #release} with saved state as argument,
         *      throwing IllegalMonitorStateException if it fails.
         * <li> Block until signalled, interrupted, or timed out.
         * <li> Reacquire by invoking specialized version of
         *      {@link #acquire} with saved state as argument.
         * <li> If interrupted while blocked in step 4, throw InterruptedException.
         * <li> If timed out while blocked in step 4, return false, else true.
         * </ol>
         */
        // 绝对定时阻塞等待当前线程, 如果线程被中断则加入同步队列, 且在获取到同步器后处理中断
        public final boolean awaitUntil(Date deadline) throws InterruptedException {
            // 获取指定超时的绝对时间
            long abstime = deadline.getTime();

            // 测试当前线程是否被中断, 该方法会清除线程的中断状态
            if (Thread.interrupted())
                throw new InterruptedException();

            // 添加一个条件结点, 如果最后一个条件结点t不是条件结点, 则会从条件队列中取消非条件结点, 将其nextWaiter链接到最后一个条件结点末尾
            Node node = addConditionWaiter();

            // volatile方式获取同步状态并尝试释放独占模式的同步器, 如果释放成功, 则返回刚刚获取到的同步状态; 如果失败则抛出异常后更改结点为取消结点
            int savedState = fullyRelease(node);

            // 判断node结点的是不是在公平队列中排队, 如果不是则阻塞当前线程
            boolean timedout = false;
            int interruptMode = 0;
            while (!isOnSyncQueue(node)) {
                // 如果发生了超时,  则将已经取消了等待的节点转移到同步队列, 如果转移成功则返回true, 失败则返回false
                if (System.currentTimeMillis() > abstime) {
                    timedout = transferAfterCancelledWait(node);
                    break;
                }

                // 如果还没超时, 则阻塞指定的时间
                LockSupport.parkUntil(this, abstime);

                // 检查是否存在等待时中断, 如果当前线程被中断, 则将等待结点转移到同步队列, 如果转移成功则返回THROW_IE(-1), 如果转移失败则返回REINTERRUPT(1); 如果等待时没有发生异常, 则返回0
                if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
                    break;
            }

            // 如果在同步队列中, 则独占式自旋入队的核心逻辑, 如果node前驱刚释放, 则更新node为头结点且不用阻塞直接返回;
            // 否则放置node结点并阻塞其排队线程, 如果当前线程有被中断过, 则还需要继续自旋, 直到获取到同步器, 返回true代表有被中断过让上层API处理;
            // 另外如果调用tryAcquire方法时发生了异常, 则还需要则取消node结点
            if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
                interruptMode = REINTERRUPT;

            // 从条件队列中取消非条件结点, 将其nextWaiter链接到最后一个条件结点末尾
            if (node.nextWaiter != null)
                unlinkCancelledWaiters();

            // 如果等待期间发生中断, 则抛出InterruptedException、重新中断当前线程或不执行任何操作
            if (interruptMode != 0)
                reportInterruptAfterWait(interruptMode);

            // 返回是否发生了超时
            return !timedout;
        }

        /**
         * 20210813
         * 实现定时条件等待:
         *      1. 如果当前线程被中断，则抛出 InterruptedException。
         *      2. 保存 {@link #getState} 返回的锁状态。
         *      3. 使用保存的状态作为参数调用 {@link #release}，如果失败则抛出 IllegalMonitorStateException。
         *      4. 阻塞直到发出信号、中断或超时。
         *      5. 通过调用特殊版本的 {@link #acquire} 以保存的状态作为参数重新获取。
         *      6. 如果在步骤 4 中被阻塞时被中断，则抛出 InterruptedException。
         *      7. 如果在步骤 4 中阻塞时超时，则返回 false，否则返回 true。
         */
        /**
         * Implements timed condition wait.
         * <ol>
         * <li> If current thread is interrupted, throw InterruptedException.
         * <li> Save lock state returned by {@link #getState}.
         * <li> Invoke {@link #release} with saved state as argument,
         *      throwing IllegalMonitorStateException if it fails.
         * <li> Block until signalled, interrupted, or timed out.
         * <li> Reacquire by invoking specialized version of
         *      {@link #acquire} with saved state as argument.
         * <li> If interrupted while blocked in step 4, throw InterruptedException.
         * <li> If timed out while blocked in step 4, return false, else true.
         * </ol>
         */
        // 定时阻塞等待当前线程, 如果线程被中断则加入同步队列, 且在获取到同步器后处理中断
        public final boolean await(long time, TimeUnit unit) throws InterruptedException {
            // 获取指定的超时时间
            long nanosTimeout = unit.toNanos(time);

            // 测试当前线程是否被中断, 该方法会清除线程的中断状态
            if (Thread.interrupted())
                throw new InterruptedException();

            // 添加一个条件结点, 如果最后一个条件结点t不是条件结点, 则会从条件队列中取消非条件结点, 将其nextWaiter链接到最后一个条件结点末尾
            Node node = addConditionWaiter();

            // volatile方式获取同步状态并尝试释放独占模式的同步器, 如果释放成功, 则返回刚刚获取到的同步状态; 如果失败则抛出异常后更改结点为取消结点
            int savedState = fullyRelease(node);

            // 判断node结点的是不是在公平队列中排队, 如果不是则阻塞当前线程
            final long deadline = System.nanoTime() + nanosTimeout;
            boolean timedout = false;
            int interruptMode = 0;
            while (!isOnSyncQueue(node)) {
                // 如果超时时间小于0, 说明发生了超时, 将已经取消了等待的节点转移到同步队列, 如果转移成功则返回true, 失败则返回false
                if (nanosTimeout <= 0L) {
                    timedout = transferAfterCancelledWait(node);
                    break;
                }
                // 如果过期总时间仍大于1s钟, 说明仍然需要阻塞线程, 则调用LockSupport的定时阻塞方法阻塞当前线程
                if (nanosTimeout >= spinForTimeoutThreshold)
                    LockSupport.parkNanos(this, nanosTimeout);

                // 检查是否存在等待时中断, 如果当前线程被中断, 则将等待结点转移到同步队列, 如果转移成功则返回THROW_IE(-1), 如果转移失败则返回REINTERRUPT(1); 如果等待时没有发生异常, 则返回0
                if ((interruptMode = checkInterruptWhileWaiting(node)) != 0)
                    break;

                // 重新计算超时时间
                nanosTimeout = deadline - System.nanoTime();
            }

            // 如果在同步队列中, 则独占式自旋入队的核心逻辑, 如果node前驱刚释放, 则更新node为头结点且不用阻塞直接返回;
            // 否则放置node结点并阻塞其排队线程, 如果当前线程有被中断过, 则还需要继续自旋, 直到获取到同步器, 返回true代表有被中断过让上层API处理;
            // 另外如果调用tryAcquire方法时发生了异常, 则还需要则取消node结点
            if (acquireQueued(node, savedState) && interruptMode != THROW_IE)
                interruptMode = REINTERRUPT;

            // 从条件队列中取消非条件结点, 将其nextWaiter链接到最后一个条件结点末尾
            if (node.nextWaiter != null)
                unlinkCancelledWaiters();

            // 如果等待期间发生中断, 则抛出InterruptedException、重新中断当前线程或不执行任何操作
            if (interruptMode != 0)
                reportInterruptAfterWait(interruptMode);

            // 返回是否发生了超时
            return !timedout;
        }

        // 仪表支持
        //  support for instrumentation

        // 如果此条件是由给定的条件创建的，则返回true同步对象。
        /**
         * Returns true if this condition was created by the given
         * synchronization object.
         *
         * @return {@code true} if owned
         */
        // 如果由给定的AQS是否当前AQS创建的，则返回true
        final boolean isOwnedBy(AbstractQueuedSynchronizer sync) {
            return sync == AbstractQueuedSynchronizer.this;
        }

        // 查询是否有线程在此条件下等待。 实现{@link AbstractQueuedSynchronizer#hasWaiters(ConditionObject)}。
        /**
         * Queries whether any threads are waiting on this condition.
         * Implements {@link AbstractQueuedSynchronizer#hasWaiters(ConditionObject)}.
         *
         * @return {@code true} if there are any waiting threads
         * @throws IllegalMonitorStateException if {@link #isHeldExclusively}
         *         returns {@code false}
         */
        // 查询是否有线程在此条件下等待, 从头遍历条件队列, 如果结点状态为CONDITION结点, 则返回true, 否则返回false
        protected final boolean hasWaiters() {
            // 判断当前线程是否为独占的线程, 如果不是则抛出异常
            if (!isHeldExclusively())
                throw new IllegalMonitorStateException();

            // 从头遍历条件队列, 如果结点状态为CONDITION结点, 则返回true, 否则返回false
            for (Node w = firstWaiter; w != null; w = w.nextWaiter) {
                if (w.waitStatus == Node.CONDITION)
                    return true;
            }

            return false;
        }

        // 返回等待此条件的线程数的估计值。
        // 实现{@link AbstractQueuedSynchronizer#getWaitQueueLength(ConditionObject)}。
        /**
         * Returns an estimate of the number of threads waiting on
         * this condition.
         * Implements {@link AbstractQueuedSynchronizer#getWaitQueueLength(ConditionObject)}.
         *
         * @return the estimated number of waiting threads
         * @throws IllegalMonitorStateException if {@link #isHeldExclusively}
         *         returns {@code false}
         */
        // 返回等待此条件的线程数的估计值 => 从头遍历条件队列, 统计CONDITION结点的个数
        protected final int getWaitQueueLength() {
            // 判断当前线程是否为独占的线程, 如果不是则抛出异常
            if (!isHeldExclusively())
                throw new IllegalMonitorStateException();

            // 从头遍历条件队列, 统计CONDITION结点的个数
            int n = 0;
            for (Node w = firstWaiter; w != null; w = w.nextWaiter) {
                if (w.waitStatus == Node.CONDITION)
                    ++n;
            }
            return n;
        }

        // 返回一个包含可能正在等待此Condition的线程的集合。 实现 {@link AbstractQueuedSynchronizer#getWaitingThreads(ConditionObject)}。
        /**
         * Returns a collection containing those threads that may be
         * waiting on this Condition.
         * Implements {@link AbstractQueuedSynchronizer#getWaitingThreads(ConditionObject)}.
         *
         * @return the collection of threads
         * @throws IllegalMonitorStateException if {@link #isHeldExclusively}
         *         returns {@code false}
         */
        // 返回一个包含可能正在等待此Condition的线程的集合 => 从头遍历条件队列, 收集CONDITION结点的线程
        protected final Collection<Thread> getWaitingThreads() {
            // 判断当前线程是否为独占的线程, 如果不是则抛出异常
            if (!isHeldExclusively())
                throw new IllegalMonitorStateException();

            // 从头遍历条件队列, 收集CONDITION结点的线程
            ArrayList<Thread> list = new ArrayList<Thread>();
            for (Node w = firstWaiter; w != null; w = w.nextWaiter) {
                if (w.waitStatus == Node.CONDITION) {
                    Thread t = w.thread;
                    if (t != null)
                        list.add(t);
                }
            }
            return list;
        }
    }

    /**
     * 20210722
     * 设置以支持compareAndSet。我们需要在这里本地实现：为了允许未来的增强，我们不能显式地继承AtomicInteger，否则这将是有效和有用的。
     * 因此，作为较小的弊端，我们使用热点内在函数API本地实现。当我们这样做时，我们对其他CASable字段也做同样的事情（否则可以使用原子字段更新程序来完成）。
     */
    /**
     * Setup to support compareAndSet. We need to natively implement
     * this here: For the sake of permitting future enhancements, we
     * cannot explicitly subclass AtomicInteger, which would be
     * efficient and useful otherwise. So, as the lesser of evils, we
     * natively implement using hotspot intrinsics API. And while we
     * are at it, we do the same for other CASable fields (which could
     * otherwise be done with atomic field updaters).
     */
    private static final Unsafe unsafe = Unsafe.getUnsafe();
    private static final long stateOffset;// state
    private static final long headOffset;// head
    private static final long tailOffset;// tail
    private static final long waitStatusOffset;// waitStatus
    private static final long nextOffset;// next

    static {
        try {
            stateOffset = unsafe.objectFieldOffset
                (AbstractQueuedSynchronizer.class.getDeclaredField("state"));
            headOffset = unsafe.objectFieldOffset
                (AbstractQueuedSynchronizer.class.getDeclaredField("head"));
            tailOffset = unsafe.objectFieldOffset
                (AbstractQueuedSynchronizer.class.getDeclaredField("tail"));
            waitStatusOffset = unsafe.objectFieldOffset
                (Node.class.getDeclaredField("waitStatus"));
            nextOffset = unsafe.objectFieldOffset
                (Node.class.getDeclaredField("next"));

        } catch (Exception ex) { throw new Error(ex); }
    }

    /**
     * CAS head field. Used only by enq.
     */
    // CAS 头域。 仅由 enq 使用。
    private final boolean compareAndSetHead(Node update) {
        return unsafe.compareAndSwapObject(this, headOffset, null, update);
    }

    /**
     * CAS tail field. Used only by enq.
     */
    // CAS 尾场。 仅由 enq 使用。
    private final boolean compareAndSetTail(Node expect, Node update) {
        return unsafe.compareAndSwapObject(this, tailOffset, expect, update);
    }

    /**
     * CAS waitStatus field of a node.
     */
    // 节点的 CAS waitStatus 字段。
    private static final boolean compareAndSetWaitStatus(Node node,
                                                         int expect,
                                                         int update) {
        return unsafe.compareAndSwapInt(node, waitStatusOffset,
                                        expect, update);
    }

    /**
     * CAS next field of a node.
     */
    // 节点的 CAS 下一个字段。
    private static final boolean compareAndSetNext(Node node,
                                                   Node expect,
                                                   Node update) {
        return unsafe.compareAndSwapObject(node, nextOffset, expect, update);
    }
}
