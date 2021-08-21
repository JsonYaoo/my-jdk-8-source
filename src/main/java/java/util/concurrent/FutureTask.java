/*
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util.concurrent;
import java.util.concurrent.locks.LockSupport;

/**
 * 20210726
 * A. 可取消的异步计算。此类提供了{@link Future}的基本实现，具有启动和取消计算、查询以查看计算是否完成以及检索计算结果的方法。计算完成后才能检索结果；
 *    如果计算尚未完成，{@code get}方法将阻塞。一旦计算完成，就不能重新开始或取消计算（除非使用 {@link #runAndReset} 调用计算）。
 * B. {@code FutureTask}可用于包装{@link Callable}或{@link Runnable}对象。因为{@code FutureTask}实现了{@code Runnable}，
 *    一个 {@code FutureTask}可以提交给一个{@link Executor}执行。
 * C. 除了用作独立类之外，该类还提供了{@code protected}功能，这在创建自定义任务类时可能很有用。
 */
/**
 * A.
 * A cancellable asynchronous computation.  This class provides a base
 * implementation of {@link Future}, with methods to start and cancel
 * a computation, query to see if the computation is complete, and
 * retrieve the result of the computation.  The result can only be
 * retrieved when the computation has completed; the {@code get}
 * methods will block if the computation has not yet completed.  Once
 * the computation has completed, the computation cannot be restarted
 * or cancelled (unless the computation is invoked using
 * {@link #runAndReset}).
 *
 * B.
 * <p>A {@code FutureTask} can be used to wrap a {@link Callable} or
 * {@link Runnable} object.  Because {@code FutureTask} implements
 * {@code Runnable}, a {@code FutureTask} can be submitted to an
 * {@link Executor} for execution.
 *
 * C.
 * <p>In addition to serving as a standalone class, this class provides
 * {@code protected} functionality that may be useful when creating
 * customized task classes.
 *
 * @since 1.5
 * @author Doug Lea
 * @param <V> The result type returned by this FutureTask's {@code get} methods
 */
public class FutureTask<V> implements RunnableFuture<V> {

    /**
     * 20210726
     * A. 修订说明：这与依赖AbstractQueuedSynchronizer的此类以前的版本不同，主要是为了避免用户在取消竞争期间保留中断状态而感到惊讶。
     *    当前设计中的同步控制依赖于通过CAS更新的“状态”字段来跟踪完成，以及一个简单的Treiber堆栈来保存等待线程。
     * B. 样式说明：像往常一样，我们绕过使用AtomicXFieldUpdaters的开销，而是直接使用Unsafe内部函数。
     */
    /*
     * A.
     * Revision notes: This differs from previous versions of this
     * class that relied on AbstractQueuedSynchronizer, mainly to
     * avoid surprising users about retaining interrupt status during
     * cancellation races. Sync control in the current design relies
     * on a "state" field updated via CAS to track completion, along
     * with a simple Treiber stack to hold waiting threads.
     *
     * B.
     * Style note: As usual, we bypass overhead of using
     * AtomicXFieldUpdaters and instead directly use Unsafe intrinsics.
     */

    /**
     * 20210726
     * A. 此任务的运行状态，最初是NEW。运行状态仅在set、setException和cancel方法中转换为终止状态。在完成期间，状态可能采用COMPLETING（在设置结果时）或
     *    INTERRUPTING（仅在中断运行程序以满足取消（真）时）的瞬态值。 从这些中间状态到最终状态的转换使用更便宜的有序/惰性写入，因为值是唯一的并且无法进一步修改。
     * B. 可能的状态转换：
     * NEW -> COMPLETING -> NORMAL: 新增 -> 正在完成 -> 已完成
     * NEW -> COMPLETING -> EXCEPTIONAL: 新增 -> 正在完成 -> 发生异常
     * NEW -> CANCELLED: 新增 -> 已取消
     * NEW -> INTERRUPTING -> INTERRUPTED: 新增 -> 发生中断 -> 已中断
     */
    /**
     * The run state of this task, initially NEW.  The run state
     * transitions to a terminal state only in methods set,
     * setException, and cancel.  During completion, state may take on
     * transient values of COMPLETING (while outcome is being set) or
     * INTERRUPTING (only while interrupting the runner to satisfy a
     * cancel(true)). Transitions from these intermediate to final
     * states use cheaper ordered/lazy writes because values are unique
     * and cannot be further modified.
     *
     * Possible state transitions:
     * NEW -> COMPLETING -> NORMAL
     * NEW -> COMPLETING -> EXCEPTIONAL
     * NEW -> CANCELLED
     * NEW -> INTERRUPTING -> INTERRUPTED
     */
    // 任务的运行状态，最初是NEW。运行状态仅在set、setException和cancel方法中转换为终止状态。在完成期间，状态可能采用COMPLETING（在设置结果时）或INTERRUPTING（仅在中断运行程序以满足取消（真）时）的瞬态值。 从这些中间状态到最终状态的转换使用更便宜的有序/惰性写入，因为值是唯一的并且无法进一步修改。
    private volatile int state;
    private static final int NEW          = 0;// 新增
    private static final int COMPLETING   = 1;// 正在完成
    private static final int NORMAL       = 2;// 已完成
    private static final int EXCEPTIONAL  = 3;// 发生异常
    private static final int CANCELLED    = 4;// 已取消
    private static final int INTERRUPTING = 5;// 发生中断
    private static final int INTERRUPTED  = 6;// 已中断

    // 底层可调用；运行后清零
    /** The underlying callable; nulled out after running */
    // 底层可调用任务；运行后清零
    private Callable<V> callable;

    // 从get()返回的结果或抛出的异常
    /** The result to return or exception to throw from get() */
    // 从get()返回的结果或抛出的异常, 非volatile, 需要有状态读/写保护
    private Object outcome; // non-volatile, protected by state reads/writes 非易失性，受状态读/写保护

    // 运行可调用的线程；在run()期间被CASed
    /** The thread running the callable; CASed during run() */
    // 运行后可调用的线程, 在run()时被CAS为当前线程
    private volatile Thread runner;

    // 等待线程的Treiber堆栈
    /** Treiber stack of waiting threads */
    // 等待线程WaitNode结点的链表
    private volatile WaitNode waiters;

    // 为完成的任务返回结果或抛出异常。
    /**
     * Returns result or throws exception for completed task.
     *
     * @param s completed state value
     */
    // 为已完成的任务返回结果或者抛出异常
    @SuppressWarnings("unchecked")
    private V report(int s) throws ExecutionException {
        // 获取当前从get()返回的结果或抛出的异常x
        Object x = outcome;

        // 如果此异步计算任务的运行状态为正常状态, 说明结果已经计算好了, 则返回x
        if (s == NORMAL)
            return (V)x;

        // 如果此异步计算任务的运行状态为已取消、发生中断或者中断状态, 则抛出CancellationException异常
        if (s >= CANCELLED)
            throw new CancellationException();

        // 如果此异步计算任务的运行状态为其他状态, 则抛出ExecutionException异常
        throw new ExecutionException((Throwable)x);
    }

    // 创建一个{@code FutureTask}，它将在运行时执行给定的{@code Callable}。
    /**
     * Creates a {@code FutureTask} that will, upon running, execute the
     * given {@code Callable}.
     *
     * @param  callable the callable task // 可调用的任务
     * @throws NullPointerException if the callable is null
     */
    // 设置将要运行的可调用任务, 并更新任务的运行状态为创建状态
    public FutureTask(Callable<V> callable) {
        if (callable == null)
            throw new NullPointerException();

        // 设置将要运行的可调用任务, 并更新任务的运行状态为创建状态
        this.callable = callable;
        this.state = NEW;       // ensure visibility of callable 确保可调用的可见性
    }

    /**
     * 20210726
     * 创建一个{@code FutureTask}，它将在运行时执行给定的{@code Runnable}，并安排{@code get}在成功完成后返回给定的结果。
     */
    /**
     * Creates a {@code FutureTask} that will, upon running, execute the
     * given {@code Runnable}, and arrange that {@code get} will return the
     * given result on successful completion.
     *
     * @param runnable the runnable task
     *
     * // 成功完成后返回的结果。如果您不需要特定结果，请考虑使用以下形式的构造：
     *                 {@code Future<?> f = new FutureTask<Void>(runnable, null)}
     * @param result the result to return on successful completion. If
     * you don't need a particular result, consider using
     * constructions of the form:
     * {@code Future<?> f = new FutureTask<Void>(runnable, null)}
     * @throws NullPointerException if the runnable is null
     */
    // 包装可运行任务与成功返回的结果, 成为可调度任务, 然后设置将要运行的可调用任务, 并更新任务的运行状态为创建状态
    public FutureTask(Runnable runnable, V result) {
        // 包装可运行任务与成功返回的结果, 成为可调度任务, 然后设置将要运行的可调用任务, 并更新任务的运行状态为创建状态
        this.callable = Executors.callable(runnable, result);
        this.state = NEW;       // ensure visibility of callable
    }

    // 判断任务是否已取消、发生中断或者已中断
    public boolean isCancelled() {
        return state >= CANCELLED;
    }

    // 判断任务是否完成中、已完成、已发生异常、已取消、发生中断或者已中断
    public boolean isDone() {
        return state != NEW;
    }

    // 尝试取消此异步计算任务的执行, 如果任务已完成、已被取消或由于其他原因无法取消, 则返回false; 如果此任务在正常完成之前被取消, 则返回{@code true}
    public boolean cancel(boolean mayInterruptIfRunning) {
        // 如果此时为新增状态, 且应该中断, 则CAS更新状态为发生中断; 如果不应该中断, 则CAS更新状态为已取消
        if (!(state == NEW && UNSAFE.compareAndSwapInt(this, stateOffset, NEW, mayInterruptIfRunning ? INTERRUPTING : CANCELLED)))
            // CAS成功则返回false, 代表由于发生中断导致的取消
            return false;

        // 如果CAS更新失败, 且应该中断, 则中断该线程, 如果从Thread其他实例方法调用该方法, 则会清除中断状态, 然后会收到一个{@link InterruptedException}
        try {    // in case call to interrupt throws exception // 如果调用中断引发异常
            if (mayInterruptIfRunning) {
                try {
                    Thread t = runner;
                    if (t != null)
                        t.interrupt();
                } finally { // final state
                    // 标记状态为已中断
                    UNSAFE.putOrderedInt(this, stateOffset, INTERRUPTED);
                }
            }
        } finally {
            // 遍历线程堆栈结点, 并清空唤醒每个结点的线程, 最后完成前调用done方法触发子类的回调, 以及清空运行的任务
            finishCompletion();
        }

        // 如果正常取消则返回true
        return true;
    }

    /**
     * @throws CancellationException {@inheritDoc}
     */
    // 阻塞获取异步计算结果, 如果任务处于未完成状态, 则会自旋+阻塞/中断/超时, 以等待任务已经完成、已取消、已发生异常或者已中断, 则清空等待结点, 并返回任务完成时的状态, 最后再为已完成的任务返回结果或者抛出异常
    public V get() throws InterruptedException, ExecutionException {
        // volatile方式获取异步任务的状态
        int s = state;

        // 如果任务为新增或者正在完成的状态
        if (s <= COMPLETING)
            // 自旋+阻塞/中断/超时, 以等待任务已经完成、已取消、已发生异常或者已中断, 则清空等待结点, 并返回任务完成时的状态
            s = awaitDone(false, 0L);

        // 如果阻塞等待到任务完成后, 则为已完成的任务返回结果或者抛出异常
        return report(s);
    }

    /**
     * @throws CancellationException {@inheritDoc}
     */
    // 定时阻塞获取异步计算结果, 如果任务处于未完成状态, 则会自旋+阻塞/中断/超时, 以等待任务已经完成、已取消、已发生异常或者已中断, 则清空等待结点, 并返回任务完成时的状态, 最后再为已完成的任务返回结果或者抛出异常
    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        if (unit == null)
            throw new NullPointerException();

        // volatile方式获取异步任务的状态
        int s = state;

        // 如果任务为新增或者正在完成的状态
        if (s <= COMPLETING &&
                // 自旋+阻塞/中断/超时, 以等待任务已经完成、已取消、已发生异常或者已中断, 则清空等待结点, 并返回任务完成时的状态
                (s = awaitDone(true, unit.toNanos(timeout))) <= COMPLETING)
            // 如果返回的是未完成的状态, 说明发生了超时, 则抛出TimeoutException
            throw new TimeoutException();

        // 如果阻塞等待到任务完成后, 则为已完成的任务返回结果或者抛出异常
        return report(s);
    }

    /**
     * 20210726
     * 当此任务转换到状态{@code isDone}时调用受保护的方法（无论是正常还是通过取消）。默认实现什么都不做。子类可以覆盖此方法以调用完成回调或执行簿记。
     * 请注意，您可以在此方法的执行内部查询状态，以确定此任务是否已被取消。
     */
    /**
     * Protected method invoked when this task transitions to state
     * {@code isDone} (whether normally or via cancellation). The
     * default implementation does nothing.  Subclasses may override
     * this method to invoke completion callbacks or perform
     * bookkeeping. Note that you can query status inside the
     * implementation of this method to determine whether this task
     * has been cancelled.
     */
    // 正常结束或者已取消完毕时调用, 默认空实现, 子类可以覆盖此方法实现回调
    protected void done() { }

    /**
     * 20210727
     * A. 将此未来的结果设置为给定值，除非此未来已被设置或已被取消。
     * B. 此方法在计算成功完成后由 {@link #run} 方法内部调用。
     */
    /**
     * A.
     * Sets the result of this future to the given value unless
     * this future has already been set or has been cancelled.
     *
     * B.
     * <p>This method is invoked internally by the {@link #run} method
     * upon successful completion of the computation.
     *
     * @param v the value
     */
    // 为异步计算结果设置value值, 并更新任务状态为已完成状态, 最后遍历等待线程堆栈结点, 并清空唤醒每个结点的线程, 并在完成前调用done方法触发子类的回调, 以及清空运行的任务
    protected void set(V v) {
        // 先CAS更新任务状态为正在完成状态
        if (UNSAFE.compareAndSwapInt(this, stateOffset, NEW, COMPLETING)) {
            // 为异步计算结果设置value值
            outcome = v;

            // 顺序更新任务状态为已完成状态
            UNSAFE.putOrderedInt(this, stateOffset, NORMAL); // final state

            // 遍历线程堆栈结点, 并清空唤醒每个结点的线程, 最后完成前调用done方法触发子类的回调, 以及清空运行的任务
            finishCompletion();
        }
    }

    /**
     * 20210727
     * A. 导致此未来报告 {@link ExecutionException} 以给定的 throwable 作为其原因，除非此未来已被设置或已被取消。
     * B. 此方法在计算失败时由{@link #run}方法内部调用。
     */
    /**
     * A.
     * Causes this future to report an {@link ExecutionException}
     * with the given throwable as its cause, unless this future has
     * already been set or has been cancelled.
     *
     * B.
     * <p>This method is invoked internally by the {@link #run} method
     * upon failure of the computation.
     *
     * @param t the cause of failure
     */
    // 为异步计算结果设置异常结果, 并更新任务状态为已发生异常状态, 最后遍历等待线程堆栈结点, 并清空唤醒每个结点的线程, 并在完成前调用done方法触发子类的回调, 以及清空运行的任务
    protected void setException(Throwable t) {
        // 先CAS更新任务状态为正在完成状态
        if (UNSAFE.compareAndSwapInt(this, stateOffset, NEW, COMPLETING)) {
            // 为异步计算结果设置异常结果
            outcome = t;

            // 顺序更新任务状态为已发生异常状态
            UNSAFE.putOrderedInt(this, stateOffset, EXCEPTIONAL); // final state

            // 遍历线程堆栈结点, 并清空唤醒每个结点的线程, 最后完成前调用done方法触发子类的回调, 以及清空运行的任务
            finishCompletion();
        }
    }

    // 本质上是调用Callable#call()运行目标任务, 并将计算结果设置到outcome中(包括正确的结果或者异常), 最后遍历等待线程堆栈结点, 并清空唤醒每个结点的线程, 并在完成前调用done方法触发子类的回调, 以及清空运行的任务
    public void run() {
        // 如果任务为新增状态, 则CAS更新运行的线程为当前线程
        if (state != NEW || !UNSAFE.compareAndSwapObject(this, runnerOffset, null, Thread.currentThread()))
            // 如果CAS更新线程失败, 或者本来就是非新增状态, 则直接返回, 不用运行任务了
            return;
        // 如果CAS更新成功, 且任务为新增状态, 则获取要运行的目标任务, 调用call方法计算结果
        try {
            Callable<V> c = callable;
            if (c != null && state == NEW) {
                V result;
                boolean ran;
                try {
                    result = c.call();
                    ran = true;
                } catch (Throwable ex) {
                    result = null;
                    ran = false;

                    // 如果计算时发生异常, 则为异步计算结果设置异常结果, 并更新任务状态为已发生异常状态, 最后遍历等待线程堆栈结点, 并清空唤醒每个结点的线程, 并在完成前调用done方法触发子类的回调, 以及清空运行的任务
                    setException(ex);
                }
                // 如果计算成功, 则为异步计算结果设置value值, 并更新任务状态为已完成状态, 最后遍历等待线程堆栈结点, 并清空唤醒每个结点的线程, 并在完成前调用done方法触发子类的回调, 以及清空运行的任务
                if (ran)
                    set(result);
            }
        } finally {
            // runner必须非空，直到状态稳定以防止并发调用run()
            // runner must be non-null until state is settled to
            // prevent concurrent calls to run()
            // volatile方式清空运行线程, 以防止并发调用run()
            runner = null;

            // 将运行器归零后必须重新读取状态以防止泄漏中断
            // state must be re-read after nulling runner to prevent
            // leaked interrupts
            // volatile方式获取任务状态, 如果为发生中断或者已中断, 则一直让步CPU保持等待, 直到为中断状态
            int s = state;
            if (s >= INTERRUPTING)
                handlePossibleCancellationInterrupt(s);
        }
    }

    /**
     * 20210727
     * 执行计算而不设置其结果，然后将此未来重置为初始状态，如果计算遇到异常或被取消，则无法这样做。 这是专为与本质上执行多次的任务一起使用而设计的。
     */
    /**
     * Executes the computation without setting its result, and then
     * resets this future to initial state, failing to do so if the
     * computation encounters an exception or is cancelled.  This is
     * designed for use with tasks that intrinsically execute more
     * than once.
     *
     * @return {@code true} if successfully run and reset // 如果成功运行并重置
     */
    // 本质上是调用Callable#call()运行目标任务, 不将计算结果设置到outcome中, 但会设置异常结果, 返回true代表本次运行成功且仍然可以继续运行; 返回false代表本次运行失败且不可以继续运行
    protected boolean runAndReset() {
        // 如果任务为新增状态, 则CAS更新运行的线程为当前线程
        if (state != NEW || !UNSAFE.compareAndSwapObject(this, runnerOffset, null, Thread.currentThread()))
            // 如果CAS更新线程失败, 或者本来就是非新增状态, 则直接返回false, 代表本次运行失败且不可以继续运行
            return false;

        // 如果CAS更新成功, 且任务为新增状态, 则获取要运行的目标任务, 调用call方法计算结果
        boolean ran = false;
        int s = state;
        try {
            Callable<V> c = callable;
            if (c != null && s == NEW) {
                try {
                    c.call(); // don't set result
                    ran = true;
                } catch (Throwable ex) {
                    // 如果计算时发生异常, 则为异步计算结果设置异常结果, 并更新任务状态为已发生异常状态, 最后遍历等待线程堆栈结点, 并清空唤醒每个结点的线程, 并在完成前调用done方法触发子类的回调, 以及清空运行的任务
                    setException(ex);
                }
            }
        } finally {
            // runner必须非空，直到状态稳定以防止并发调用run()
            // runner must be non-null until state is settled to
            // prevent concurrent calls to run()
            // volatile方式清空运行线程, 以防止并发调用run()
            runner = null;

            // 将运行器归零后必须重新读取状态以防止泄漏中断
            // state must be re-read after nulling runner to prevent
            // leaked interrupts
            // volatile方式获取任务状态, 如果为发生中断或者已中断, 则一直让步CPU保持等待, 直到为中断状态
            s = state;
            if (s >= INTERRUPTING)
                handlePossibleCancellationInterrupt(s);
        }

        // 如果运行完毕, 且任务状态仍为新增状态, 则返回true, 代表本次运行成功且仍然可以继续运行
        return ran && s == NEW;
    }

    // 确保来自可能的 cancel(true) 的任何中断仅在运行或 runAndReset 时传递给任务。
    /**
     * Ensures that any interrupt from a possible cancel(true) is only
     * delivered to a task while in run or runAndReset.
     */
    // 如果为正在中断状态, 则一直让步CPU保持等待, 直到为中断状态
    private void handlePossibleCancellationInterrupt(int s) {
        // 我们的干扰者有可能在有机会打断我们之前就停止了。 让我们耐心等待。
        // It is possible for our interrupter to stall before getting a
        // chance to interrupt us.  Let's spin-wait patiently.
        // 如果为正在中断状态, 则一直让步CPU保持等待, 直到为中断状态
        if (s == INTERRUPTING)
            while (state == INTERRUPTING)
                Thread.yield(); // wait out pending interrupt // 等待挂起的中断

        // assert state == INTERRUPTED;

        // 我们想清除我们可能从cancel(true)收到的任何中断。 但是，允许使用中断作为任务与其调用者通信的独立机制，并且没有办法仅清除取消中断。
        // We want to clear any interrupt we may have received from
        // cancel(true).  However, it is permissible to use interrupts
        // as an independent mechanism for a task to communicate with
        // its caller, and there is no way to clear only the
        // cancellation interrupt.
        //
        // Thread.interrupted();
    }

    /**
     * 20210726
     * 用于记录Treiber堆栈中等待线程的简单链表节点。有关更详细的说明，请参阅其他类，例如Phaser和SynchronousQueue。
     */
    /**
     * Simple linked list nodes to record waiting threads in a Treiber
     * stack.  See other classes such as Phaser and SynchronousQueue
     * for more detailed explanation.
     */
    // 等待线程结点
    static final class WaitNode {
        volatile Thread thread;
        volatile WaitNode next;

        // 使用当前线程作为等待线程
        WaitNode() { thread = Thread.currentThread(); }
    }

    // 删除所有等待线程并发出信号，调用done()，并将可调用对象设为null。
    /**
     * Removes and signals all waiting threads, invokes done(), and
     * nulls out callable.
     */
    // 遍历线程堆栈结点, 并清空唤醒每个结点的线程, 最后完成前调用done方法触发子类的回调, 以及清空运行的任务
    private void finishCompletion() {
        // assert state > COMPLETING;
        // 遍历线程结点的堆栈
        for (WaitNode q; (q = waiters) != null;) {
            // CAS置null线程结点
            if (UNSAFE.compareAndSwapObject(this, waitersOffset, q, null)) {
                // 如果CAS成功, 说明当前线程抢到了清空操作的机会, 则开始自旋
                for (;;) {
                    // volatile方式获取堆栈结点线程t
                    Thread t = q.thread;

                    // 置null该结点thread引用, 并唤醒该线程t
                    if (t != null) {
                        q.thread = null;
                        LockSupport.unpark(t);
                    }

                    // 如果线程t为null, 或者唤醒t完毕, 则volatile方式获取下一个结点next
                    WaitNode next = q.next;

                    // 如果遍历到链尾, 则退出自旋
                    if (next == null)
                        break;

                    // 置null原next引用, 并继续遍历
                    q.next = null; // unlink to help gc
                    q = next;
                }
                break;
            }
            // 如果CAS失败, 说明当前没抢到清空操作的机会, 则重新CAS
        }

        // 正常结束或者已取消完毕时调用, 默认空实现, 子类可以覆盖此方法实现回调
        done();

        // 置null运行的任务
        callable = null;        // to reduce footprint 减少足迹
    }

    // 在中断或超时时等待完成或中止。
    /**
     * Awaits completion or aborts on interrupt or timeout.
     *
     * @param timed true if use timed waits // 如果使用定时等待，则为 true
     * @param nanos time to wait, if timed // 时间等待，如果定时
     * @return state upon completion // 完成时的状态
     */
    // 自旋+阻塞/中断/超时, 以等待任务已经完成、已取消、已发生异常或者已中断, 则清空等待结点, 并返回任务完成时的状态
    private int awaitDone(boolean timed, long nanos) throws InterruptedException {
        // 如果需要定时, 则计算截止时间 = 系统当前纳秒数 + 定时纳秒数; 否则截止时间为0
        final long deadline = timed ? System.nanoTime() + nanos : 0L;
        WaitNode q = null;
        boolean queued = false;

        // 开始自旋
        for (;;) {
            // 自旋过程中, 第一步就是测试当前线程是否被中断, 该方法会清除线程的中断状态
            if (Thread.interrupted()) {
                // 从头遍历等待线程堆栈链表, 已脱钩其中的垃圾结点(等待线程为null的结点)
                removeWaiter(q);

                // 清空完垃圾结点然后抛出中断异常
                throw new InterruptedException();
            }

            // 如果自旋过程中, 当前线程没有被中断, 则volatile方式获取任务的状态s
            int s = state;

            // 如果任务已经完成、已取消、已发生异常或者已中断, 则清空q结点的thread引用, 并返回s状态, 代表完成时的状态
            if (s > COMPLETING) {
                if (q != null)
                    q.thread = null;
                return s;
            }
            // 如果任务正在完成中, 则当前线程先让出CPU, 缓一缓
            else if (s == COMPLETING) // cannot time out yet 还不能超时
                Thread.yield();
            // 如果q为null, 则使用当前线程初始化q为等待结点
            else if (q == null)
                q = new WaitNode();
            // 如果q不为null, 说明q已经在以上轮自旋中初始化完毕了, 且还没入栈的话, 则以volatile方式将q从头入栈, 并CAS更新waiters头结点为q
            else if (!queued)
                // CAS更新结果记录到入栈标识queued中
                queued = UNSAFE.compareAndSwapObject(this, waitersOffset, q.next = waiters, q);
            // 如果需要定时时, 则还要判断是否超时, 剩余超时时间 = 截止时间 - 系统当前纳秒数
            else if (timed) {
                nanos = deadline - System.nanoTime();

                // 如果发生了超时, 则从头遍历等待线程堆栈链表, 已脱钩其中的垃圾结点(等待线程为null的结点), 并返回当前的任务状态, 代表完成时的状态
                if (nanos <= 0L) {
                    removeWaiter(q);
                    return state;
                }

                // 如果还没发生超时, 则阻塞当前线程nanos时间
                LockSupport.parkNanos(this, nanos);
            }
            // 如果不需要定时, 则直接阻塞当前线程
            else
                LockSupport.park(this);
        }
    }

    /**
     * 20210726
     * 尝试取消链接超时或中断的等待节点以避免累积垃圾。内部节点在没有CAS的情况下只是未拼接，因为如果发布者无论如何遍历它们都是无害的。
     * 为了避免从已经删除的节点中解开的影响，如果出现明显的竞争，则对列表进行回溯。当有很多节点时，这很慢，但我们不希望列表足够长以超过更高开销的方案。
     */
    /**
     * Tries to unlink a timed-out or interrupted wait node to avoid
     * accumulating garbage.  Internal nodes are simply unspliced
     * without CAS since it is harmless if they are traversed anyway
     * by releasers.  To avoid effects of unsplicing from already
     * removed nodes, the list is retraversed in case of an apparent
     * race.  This is slow when there are a lot of nodes, but we don't
     * expect lists to be long enough to outweigh higher-overhead
     * schemes.
     */
    // 从头遍历等待线程堆栈链表, 已脱钩其中的垃圾结点(等待线程为null的结点)
    private void removeWaiter(WaitNode node) {
        // 如果node结点不为null
        if (node != null) {
            // 则清空node结点的thread引用
            node.thread = null;

            // 开始自旋
            retry:
            for (;;) {          // restart on removeWaiter race // 在removeWaiter 比赛中重新开始
                // 遍历线程堆栈结点
                for (WaitNode pred = null, q = waiters, s; q != null; q = s) {
                    // volatile方式获取结点q后继s
                    s = q.next;

                    // 如果q的thread引用不为null, 则继续往后遍历堆栈结点
                    if (q.thread != null)
                        pred = q;
                    // 如果q的thread引用为null, 且前驱不为null, 说明该结点是个垃圾结点, 且在堆栈链表中间
                    else if (pred != null) {
                        // 则volatile方式链接前驱和后继, 脱钩结点q
                        pred.next = s;

                        // volatile方式获取前驱的thread引用, 如果此时thread引用也为null, 说明前驱也是个垃圾结点, 则重新自旋, 重头开始遍历
                        if (pred.thread == null) // check for race
                            continue retry;
                    }
                    // 如果q的thread引用为null, 且前驱也为null, 说明头结点为一个垃圾结点, 则CAS更新waiters引用指向下一个堆栈结点
                    else if (!UNSAFE.compareAndSwapObject(this, waitersOffset, q, s))
                        // 然后重新自旋, 重头开始遍历
                        continue retry;
                }

                // 遍历结束, 代表垃圾结点(线程为null的结点)已被脱钩完毕, 则退出自旋
                break;
            }
        }
    }

    // Unsafe mechanics
    private static final sun.misc.Unsafe UNSAFE;
    private static final long stateOffset;// state
    private static final long runnerOffset;// runner
    private static final long waitersOffset;// waiters
    static {
        try {
            UNSAFE = sun.misc.Unsafe.getUnsafe();
            Class<?> k = FutureTask.class;
            stateOffset = UNSAFE.objectFieldOffset
                (k.getDeclaredField("state"));
            runnerOffset = UNSAFE.objectFieldOffset
                (k.getDeclaredField("runner"));
            waitersOffset = UNSAFE.objectFieldOffset
                (k.getDeclaredField("waiters"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }

}
