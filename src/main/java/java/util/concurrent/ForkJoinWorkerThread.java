/*
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util.concurrent;

import java.security.AccessControlContext;
import java.security.ProtectionDomain;

/**
 * 20210815
 * A. 由 {@link ForkJoinPool} 管理的线程，它执行{@link ForkJoinTask}。
 * B. 此类是可子类化的，只是为了添加功能——没有处理调度或执行的可覆盖方法。 但是，您可以覆盖围绕主任务处理循环的初始化和终止方法。
 *    如果您确实创建了这样的子类，您还需要在 {@code ForkJoinPool} 中向 {@linkplain ForkJoinPool#ForkJoinPool use it} 提供自定义
 *    {@link ForkJoinPool.ForkJoinWorkerThreadFactory}。
 */
/**
 * A.
 * A thread managed by a {@link ForkJoinPool}, which executes
 * {@link ForkJoinTask}s.
 *
 * B.
 * This class is subclassable solely for the sake of adding
 * functionality -- there are no overridable methods dealing with
 * scheduling or execution.  However, you can override initialization
 * and termination methods surrounding the main task processing loop.
 * If you do create such a subclass, you will also need to supply a
 * custom {@link ForkJoinPool.ForkJoinWorkerThreadFactory} to
 * {@linkplain ForkJoinPool#ForkJoinPool use it} in a {@code ForkJoinPool}.
 *
 * @since 1.7
 * @author Doug Lea
 */
public class ForkJoinWorkerThread extends Thread {

    /**
     * 20210815
     * A. ForkJoinWorkerThreads 由 ForkJoinPools 管理并执行 ForkJoinTasks。 有关说明，请参阅 ForkJoinPool 类的内部文档。
     * B. 这个类只维护到它的池和工作队列的链接。 pool 字段在构造时立即设置，但 workQueue 字段在对 registerWorker 的调用完成之前不会设置。
     *    这会导致可见性竞争，这是通过要求 workQueue 字段只能由拥有线程访问来容忍的。
     * C. 对（非公共）子类 InnocuousForkJoinWorkerThread 的支持要求我们在这里和子类中打破相当多的封装（通过 Unsafe）以访问和设置 Thread 字段。
     */
    /*
     * A.
     * ForkJoinWorkerThreads are managed by ForkJoinPools and perform
     * ForkJoinTasks. For explanation, see the internal documentation
     * of class ForkJoinPool.
     *
     * B.
     * This class just maintains links to its pool and WorkQueue.  The
     * pool field is set immediately upon construction, but the
     * workQueue field is not set until a call to registerWorker
     * completes. This leads to a visibility race, that is tolerated
     * by requiring that the workQueue field is only accessed by the
     * owning thread.
     *
     * C.
     * Support for (non-public) subclass InnocuousForkJoinWorkerThread
     * requires that we break quite a lot of encapsulation (via Unsafe)
     * both here and in the subclass to access and set Thread fields.
     */

    final ForkJoinPool pool;                // the pool this thread works in // 该线程所在的池
    final ForkJoinPool.WorkQueue workQueue; // work-stealing mechanics // 工作窃取机制

    // 创建在给定池中运行的 ForkJoinWorkerThread。
    /**
     * Creates a ForkJoinWorkerThread operating in the given pool.
     *
     * @param pool the pool this thread works in
     * @throws NullPointerException if pool is null
     */
    protected ForkJoinWorkerThread(ForkJoinPool pool) {
        // Use a placeholder until a useful name can be set in registerWorker
        super("aForkJoinWorkerThread");
        this.pool = pool;
        this.workQueue = pool.registerWorker(this);
    }

    // InnocuousForkJoinWorkerThread 的版本
    /**
     * Version for InnocuousForkJoinWorkerThread
     */
    ForkJoinWorkerThread(ForkJoinPool pool, ThreadGroup threadGroup, AccessControlContext acc) {
        super(threadGroup, null, "aForkJoinWorkerThread");
        U.putOrderedObject(this, INHERITEDACCESSCONTROLCONTEXT, acc);
        eraseThreadLocals(); // clear before registering
        this.pool = pool;
        this.workQueue = pool.registerWorker(this);
    }

    // 返回托管此线程的池。
    /**
     * Returns the pool hosting this thread.
     *
     * @return the pool
     */
    public ForkJoinPool getPool() {
        return pool;
    }

    /**
     * 20210815
     * 返回此线程在其池中的唯一索引号。返回值的范围从 0 到池中可能存在的最大线程数（减去 1），并且在线程的生命周期内不会改变。
     * 此方法对于跟踪状态或收集每个工作线程而不是每个任务的结果的应用程序可能很有用。
     */
    /**
     * Returns the unique index number of this thread in its pool.
     * The returned value ranges from zero to the maximum number of
     * threads (minus one) that may exist in the pool, and does not
     * change during the lifetime of the thread.  This method may be
     * useful for applications that track status or collect results
     * per-worker-thread rather than per-task.
     *
     * @return the index number
     */
    public int getPoolIndex() {
        return workQueue.getPoolIndex();
    }

    /**
     * 20210815
     * 在构造之后但在处理任何任务之前初始化内部状态。如果覆盖此方法，则必须在方法的开头调用 {@code super.onStart()}。
     * 初始化需要注意：大多数字段必须具有合法的默认值，以确保即使在该线程开始处理任务之前，来自其他线程的尝试访问也能正常工作。
     */
    /**
     * Initializes internal state after construction but before
     * processing any tasks. If you override this method, you must
     * invoke {@code super.onStart()} at the beginning of the method.
     * Initialization requires care: Most fields must have legal
     * default values, to ensure that attempted accesses from other
     * threads work correctly even before this thread starts
     * processing tasks.
     */
    protected void onStart() {
    }

    // 执行与终止此工作线程相关的清理工作。如果您覆盖此方法，则必须在覆盖方法的末尾调用 {@code super.onTermination}。
    /**
     * Performs cleanup associated with termination of this worker
     * thread.  If you override this method, you must invoke
     * {@code super.onTermination} at the end of the overridden method.
     *
     * @param exception the exception causing this thread to abort due
     * to an unrecoverable error, or {@code null} if completed normally
     */
    protected void onTermination(Throwable exception) {
    }

    // 此方法必须是公开的，但不应显式调用。 它执行主运行循环来执行 {@link ForkJoinTask}。
    /**
     * This method is required to be public, but should never be
     * called explicitly. It performs the main run loop to execute
     * {@link ForkJoinTask}s.
     */
    public void run() {
        // 如果ForkJoinTask任务队列中的数组为空
        if (workQueue.array == null) { // only run once // 只运行一次
            Throwable exception = null;
            try {
                // 在构造之后但在处理任何任务之前初始化内部状态
                onStart();

                // 工人的顶级运行循环，由 ForkJoinWorkerThread.run 调用。
                pool.runWorker(workQueue);
            } catch (Throwable ex) {
                exception = ex;
            } finally {
                try {
                    // 执行与终止此工作线程相关的清理工作
                    onTermination(exception);
                } catch (Throwable ex) {
                    if (exception == null)
                        exception = ex;
                } finally {
                    // 终止工作者的最终回调，以及构造或启动工作者失败时的回调。 从数组中删除工人的记录，并调整计数。 如果池正在关闭，则尝试完成终止。
                    pool.deregisterWorker(this, exception);
                }
            }
        }
    }

    // 通过清空线程映射来擦除 ThreadLocals。
    /**
     * Erases ThreadLocals by nulling out Thread maps.
     */
    final void eraseThreadLocals() {
        U.putObject(this, THREADLOCALS, null);
        U.putObject(this, INHERITABLETHREADLOCALS, null);
    }

    // InnocuousForkJoinWorkerThread 的非公共钩子方法
    /**
     * Non-public hook method for InnocuousForkJoinWorkerThread
     */
    void afterTopLevelExec() {
    }

    // 设置为允许在构造函数中设置线程字段
    // Set up to allow setting thread fields in constructor
    private static final sun.misc.Unsafe U;
    private static final long THREADLOCALS;// threadLocals
    private static final long INHERITABLETHREADLOCALS;// inheritableThreadLocals
    private static final long INHERITEDACCESSCONTROLCONTEXT;// inheritedAccessControlContext
    static {
        try {
            U = sun.misc.Unsafe.getUnsafe();
            Class<?> tk = Thread.class;
            THREADLOCALS = U.objectFieldOffset
                (tk.getDeclaredField("threadLocals"));
            INHERITABLETHREADLOCALS = U.objectFieldOffset
                (tk.getDeclaredField("inheritableThreadLocals"));
            INHERITEDACCESSCONTROLCONTEXT = U.objectFieldOffset
                (tk.getDeclaredField("inheritedAccessControlContext"));

        } catch (Exception e) {
            throw new Error(e);
        }
    }

    // 一个没有权限的工作线程，不是任何用户定义的线程组的成员，并且在运行每个顶级任务后擦除所有的 ThreadLocals。
    /**
     * A worker thread that has no permissions, is not a member of any
     * user-defined ThreadGroup, and erases all ThreadLocals after
     * running each top-level task.
     */
    static final class InnocuousForkJoinWorkerThread extends ForkJoinWorkerThread {

        // 所有 InnocuousForkJoinWorkerThreads 的 ThreadGroup
        /** The ThreadGroup for all InnocuousForkJoinWorkerThreads */
        private static final ThreadGroup innocuousThreadGroup = createThreadGroup();

        // 不支持特权的 AccessControlContext
        /** An AccessControlContext supporting no privileges */
        private static final AccessControlContext INNOCUOUS_ACC =
            new AccessControlContext(
                new ProtectionDomain[] {
                    new ProtectionDomain(null, null)
                });

        InnocuousForkJoinWorkerThread(ForkJoinPool pool) {
            super(pool, innocuousThreadGroup, INNOCUOUS_ACC);
        }

        @Override // to erase ThreadLocals // 擦除 ThreadLocals
        void afterTopLevelExec() {
            eraseThreadLocals();
        }

        @Override // to always report system loader // 始终报告系统加载程序
        public ClassLoader getContextClassLoader() {
            return ClassLoader.getSystemClassLoader();
        }

        @Override // to silently fail // 默默地失败
        public void setUncaughtExceptionHandler(UncaughtExceptionHandler x) { }

        @Override // paranoically // 偏执地
        public void setContextClassLoader(ClassLoader cl) {
            throw new SecurityException("setContextClassLoader");
        }

        // 返回一个以系统 ThreadGroup（最顶层的无父级组）为父级的新组。 使用 Unsafe 遍历 Thread.group 和 ThreadGroup.parent 字段。
        /**
         * Returns a new group with the system ThreadGroup (the
         * topmost, parent-less group) as parent.  Uses Unsafe to
         * traverse Thread.group and ThreadGroup.parent fields.
         */
        private static ThreadGroup createThreadGroup() {
            try {
                sun.misc.Unsafe u = sun.misc.Unsafe.getUnsafe();
                Class<?> tk = Thread.class;
                Class<?> gk = ThreadGroup.class;
                long tg = u.objectFieldOffset(tk.getDeclaredField("group"));
                long gp = u.objectFieldOffset(gk.getDeclaredField("parent"));
                ThreadGroup group = (ThreadGroup)
                    u.getObject(Thread.currentThread(), tg);
                while (group != null) {
                    ThreadGroup parent = (ThreadGroup)u.getObject(group, gp);
                    if (parent == null)
                        return new ThreadGroup(group,
                                               "InnocuousForkJoinWorkerThreadGroup");
                    group = parent;
                }
            } catch (Exception e) {
                throw new Error(e);
            }
            // fall through if null as cannot-happen safeguard
            throw new Error("Cannot create ThreadGroup");
        }
    }

}
