/*
 * Copyright (c) 1994, 2013, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package java.lang;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.security.AccessController;
import java.security.AccessControlContext;
import java.security.PrivilegedAction;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.LockSupport;
import sun.nio.ch.Interruptible;
import sun.reflect.CallerSensitive;
import sun.reflect.Reflection;
import sun.security.util.SecurityConstants;

/**
 * 20210725
 * A. Thread是程序中的执行线程。Java虚拟机允许应用程序同时运行多个执行线程。
 * B. 每个线程都有一个优先级。具有较高优先级的线程优先于具有较低优先级的线程执行。每个线程可能也可能不标记为守护进程。当在某个线程中运行的代码创建一个新的Thread对象时，
 *    新线程的优先级最初设置为等于创建线程的优先级，并且当且仅当创建线程是守护进程时，新线程才是守护线程。
 * C. 当Java虚拟机启动时，通常会有一个非守护线程（通常调用某个指定类的名为main的方法）。Java虚拟机继续执行线程，直到发生以下任一情况：
 *      a. 已调用Runtime类的退出方法并且安全管理器已允许退出操作发生。
 *      b. 不是守护线程的所有线程都已死亡，无论是从对run方法的调用返回，还是通过抛出传播到run方法之外的异常。
 * D. 有两种方法可以创建新的执行线程。一种是将类声明为Thread的子类。这个子类应该覆盖类Thread的run方法。然后可以分配和启动子类的实例。
 *    例如，计算大于规定值的素数的线程可以写成如下：
 *     class PrimeThread extends Thread {
 *         long minPrime;
 *         PrimeThread(long minPrime) {
 *             this.minPrime = minPrime;
 *         }
 *
 *         public void run() {
 *             // compute primes larger than minPrime
 *              . . .
 *         }
 *     }
 * E. 下面的代码将创建一个线程并启动它运行：
 *     PrimeThread p = new PrimeThread(143);
 *     p.start();
 * F. 创建线程的另一种方法是声明一个实现Runnable接口的类。然后该类实现run方法。然后可以分配类的实例，在创建Thread时将其作为参数传递并启动。
 *    其他样式中的相同示例如下所示：
 *     class PrimeRun implements Runnable {
 *         long minPrime;
 *         PrimeRun(long minPrime) {
 *             this.minPrime = minPrime;
 *         }
 *
 *         public void run() {
 *             // compute primes larger than minPrime
 *              . . .
 *         }
 *     }
 * G. 下面的代码将创建一个线程并启动它运行：
 *     PrimeRun p = new PrimeRun(143);
 *     new Thread(p).start();
 * H. 每个线程都有一个用于识别的名称。多个线程可能具有相同的名称。如果在创建线程时未指定名称，则会为其生成一个新名称。
 * I. 除非另有说明，否则将{@code null}参数传递给此类中的构造函数或方法将导致抛出{@link NullPointerException}。
 */
/**
 * A.
 * A <i>thread</i> is a thread of execution in a program. The Java
 * Virtual Machine allows an application to have multiple threads of
 * execution running concurrently.
 *
 * B.
 * <p>
 * Every thread has a priority. Threads with higher priority are
 * executed in preference to threads with lower priority. Each thread
 * may or may not also be marked as a daemon. When code running in
 * some thread creates a new <code>Thread</code> object, the new
 * thread has its priority initially set equal to the priority of the
 * creating thread, and is a daemon thread if and only if the
 * creating thread is a daemon.
 *
 * C.
 * <p>
 * When a Java Virtual Machine starts up, there is usually a single
 * non-daemon thread (which typically calls the method named
 * <code>main</code> of some designated class). The Java Virtual
 * Machine continues to execute threads until either of the following
 * occurs:
 * <ul>
 * <li>The <code>exit</code> method of class <code>Runtime</code> has been
 *     called and the security manager has permitted the exit operation
 *     to take place.
 * <li>All threads that are not daemon threads have died, either by
 *     returning from the call to the <code>run</code> method or by
 *     throwing an exception that propagates beyond the <code>run</code>
 *     method.
 * </ul>
 *
 * D.
 * <p>
 * There are two ways to create a new thread of execution. One is to
 * declare a class to be a subclass of <code>Thread</code>. This
 * subclass should override the <code>run</code> method of class
 * <code>Thread</code>. An instance of the subclass can then be
 * allocated and started. For example, a thread that computes primes
 * larger than a stated value could be written as follows:
 * <hr><blockquote><pre>
 *     class PrimeThread extends Thread {
 *         long minPrime;
 *         PrimeThread(long minPrime) {
 *             this.minPrime = minPrime;
 *         }
 *
 *         public void run() {
 *             // compute primes larger than minPrime
 *             &nbsp;.&nbsp;.&nbsp;.
 *         }
 *     }
 * </pre></blockquote><hr>
 *
 * E.
 * <p>
 * The following code would then create a thread and start it running:
 * <blockquote><pre>
 *     PrimeThread p = new PrimeThread(143);
 *     p.start();
 * </pre></blockquote>
 *
 * F.
 * <p>
 * The other way to create a thread is to declare a class that
 * implements the <code>Runnable</code> interface. That class then
 * implements the <code>run</code> method. An instance of the class can
 * then be allocated, passed as an argument when creating
 * <code>Thread</code>, and started. The same example in this other
 * style looks like the following:
 * <hr><blockquote><pre>
 *     class PrimeRun implements Runnable {
 *         long minPrime;
 *         PrimeRun(long minPrime) {
 *             this.minPrime = minPrime;
 *         }
 *
 *         public void run() {
 *             // compute primes larger than minPrime
 *             &nbsp;.&nbsp;.&nbsp;.
 *         }
 *     }
 * </pre></blockquote><hr>
 *
 * G.
 * <p>
 * The following code would then create a thread and start it running:
 * <blockquote><pre>
 *     PrimeRun p = new PrimeRun(143);
 *     new Thread(p).start();
 * </pre></blockquote>
 *
 * H.
 * <p>
 * Every thread has a name for identification purposes. More than
 * one thread may have the same name. If a name is not specified when
 * a thread is created, a new name is generated for it.
 *
 * I.
 * <p>
 * Unless otherwise noted, passing a {@code null} argument to a constructor
 * or method in this class will cause a {@link NullPointerException} to be
 * thrown.
 *
 * @author  unascribed
 * @see     Runnable
 * @see     Runtime#exit(int)
 * @see     #run()
 * @see     #stop()
 * @since   JDK1.0
 */
public class Thread implements Runnable {

    // 确保registerNatives是<clinit>做的第一件事。
    /* Make sure registerNatives is the first thing <clinit> does. */
    private static native void registerNatives();
    static {
        registerNatives();
    }

    private volatile char  name[];
    private int            priority;
    private Thread         threadQ;
    private long           eetop;

    // 是否单步执行此线程。
    /* Whether or not to single_step this thread. */
    private boolean     single_step;

    // 线程是否为守护线程。
    /* Whether or not the thread is a daemon thread. */
    private boolean     daemon = false;

    // JVM 状态
    /* JVM state */
    private boolean     stillborn = false;

    // 将运行什么。
    /* What will be run. */
    // 运行的目标任务
    private Runnable target;

    // 这个线程的组
    /* The group of this thread */
    private ThreadGroup group;

    // 此线程的上下文类加载器
    /* The context ClassLoader for this thread */
    private ClassLoader contextClassLoader;

    // 该线程继承的AccessControlContext
    /* The inherited AccessControlContext of this thread */
    private AccessControlContext inheritedAccessControlContext;

    // 用于自动编号匿名线程。
    /* For autonumbering anonymous threads. */
    private static int threadInitNumber;
    private static synchronized int nextThreadNum() {
        return threadInitNumber++;
    }

    // 与此线程有关的ThreadLocal值。该映射由ThreadLocal类维护。
    /* ThreadLocal values pertaining to this thread. This map is maintained by the ThreadLocal class. */
    ThreadLocal.ThreadLocalMap threadLocals = null;

    // 与此线程有关的InheritableThreadLocal值。该映射由InheritableThreadLocal类维护。
    /*
     * InheritableThreadLocal values pertaining to this thread. This map is
     * maintained by the InheritableThreadLocal class.
     */
    ThreadLocal.ThreadLocalMap inheritableThreadLocals = null;

    // 此线程请求的堆栈大小，如果创建者未指定堆栈大小，则为0。虚拟机可以用这个数字做任何它喜欢的事情；一些虚拟机会忽略它。
    /*
     * The requested stack size for this thread, or 0 if the creator did
     * not specify a stack size.  It is up to the VM to do whatever it
     * likes with this number; some VMs will ignore it.
     */
    private long stackSize;

    // 在本机线程终止后持续存在的JVM私有状态。
    /*
     * JVM-private state that persists after native thread termination.
     */
    private long nativeParkEventPointer;

    // 线程 ID
    /*
     * Thread ID
     */
    private long tid;

    // 用于生成线程ID
    /* For generating thread ID */
    private static long threadSeqNumber;
    private static synchronized long nextThreadID() {
        return ++threadSeqNumber;
    }

    // 工具的Java线程状态，已初始化以指示线程“尚未启动”
    /*
     * Java thread status for tools,
     * initialized to indicate thread 'not yet started'
     */
    private volatile int threadStatus = 0;// 用于组合VM类常量, 以得出线程的State状态

    /**
     * 20210725
     * 提供给当前调用java.util.concurrent.locks.LockSupport.park的参数。由（私有）java.util.concurrent.locks.LockSupport.setBlocker设置。
     * 使用java.util.concurrent.locks.LockSupport.getBlocker进行访问。
     */
    /**
     * The argument supplied to the current call to
     * java.util.concurrent.locks.LockSupport.park.
     * Set by (private) java.util.concurrent.locks.LockSupport.setBlocker
     * Accessed using java.util.concurrent.locks.LockSupport.getBlocker
     */
    volatile Object parkBlocker;

    /**
     * 20210725
     * 此线程在可中断I/O操作中被阻塞的对象（如果有）。设置此线程的中断状态后，应调用阻塞程序的中断方法。
     */
    /*
     * The object in which this thread is blocked in an interruptible I/O
     * operation, if any.  The blocker's interrupt method should be invoked
     * after setting this thread's interrupt status.
     */
    private volatile Interruptible blocker;
    private final Object blockerLock = new Object();

    // 设置blocker字段；通过来自java.nio代码的sun.misc.SharedSecrets调用
    /*
     * Set the blocker field; invoked via sun.misc.SharedSecrets from java.nio code
     */
    void blockedOn(Interruptible b) {
        synchronized (blockerLock) {
            blocker = b;
        }
    }

    // 线程可以拥有的最低优先级。
    /**
     * The minimum priority that a thread can have.
     */
    public final static int MIN_PRIORITY = 1;

    // 分配给线程的默认优先级。
   /**
     * The default priority that is assigned to a thread.
     */
    public final static int NORM_PRIORITY = 5;

    // 线程可以拥有的最大优先级。
    /**
     * The maximum priority that a thread can have.
     */
    public final static int MAX_PRIORITY = 10;

    // 返回对当前正在执行的线程对象的引用。
    /**
     * Returns a reference to the currently executing thread object.
     *
     * @return  the currently executing thread.
     */
    public static native Thread currentThread();

    /**
     * 20210725
     * A. 向调度程序提示当前线程愿意放弃其当前对处理器的使用。调度程序可以随意忽略此提示。
     * B. Yield是一种启发式尝试，旨在改善线程之间的相对进展，否则会过度使用CPU。它的使用应与详细的分析和基准测试相结合，以确保它确实具有预期的效果。
     * C. 很少适合使用这种方法。它对于调试或测试目的可能很有用，它可能有助于重现由于竞争条件引起的错误。
     *    在设计并发控制结构（例如 {@link java.util.concurrent.locks} 包中的结构）时，它也可能很有用。
     */
    /**
     * A.
     * A hint to the scheduler that the current thread is willing to yield
     * its current use of a processor. The scheduler is free to ignore this
     * hint.
     *
     * B.
     * <p> Yield is a heuristic attempt to improve relative progression
     * between threads that would otherwise over-utilise a CPU. Its use
     * should be combined with detailed profiling and benchmarking to
     * ensure that it actually has the desired effect.
     *
     * C.
     * <p> It is rarely appropriate to use this method. It may be useful
     * for debugging or testing purposes, where it may help to reproduce
     * bugs due to race conditions. It may also be useful when designing
     * concurrency control constructs such as the ones in the
     * {@link java.util.concurrent.locks} package.
     */
    // 向调度程序提示, 当前线程愿意放弃其当前对处理器的使用, 而调度程序可以随意忽略此提示
    public static native void yield();

    /**
     * 20210725
     * 使当前正在执行的线程休眠（暂时停止执行）指定的毫秒数，取决于系统计时器和调度程序的精度和准确性。该线程不会失去任何监视器的所有权。
     */
    /**
     * Causes the currently executing thread to sleep (temporarily cease
     * execution) for the specified number of milliseconds, subject to
     * the precision and accuracy of system timers and schedulers. The thread
     * does not lose ownership of any monitors.
     *
     * @param  millis
     *         the length of time to sleep in milliseconds
     *
     * @throws  IllegalArgumentException
     *          if the value of {@code millis} is negative
     *
     * @throws  InterruptedException
     *          if any thread has interrupted the current thread. The
     *          <i>interrupted status</i> of the current thread is
     *          cleared when this exception is thrown.
     */
    // 使当前正在执行的线程休眠（暂时停止执行）指定的毫秒数，且当前线程不会失去任何监视器的所有权
    public static native void sleep(long millis) throws InterruptedException;

    /**
     * 20210725
     * 使当前正在执行的线程休眠（暂时停止执行）指定的毫秒数加上指定的纳秒数，取决于系统计时器和调度程序的精度和准确性。 该线程不会失去任何监视器的所有权。
     */
    /**
     * Causes the currently executing thread to sleep (temporarily cease
     * execution) for the specified number of milliseconds plus the specified
     * number of nanoseconds, subject to the precision and accuracy of system
     * timers and schedulers. The thread does not lose ownership of any
     * monitors.
     *
     * @param  millis
     *         the length of time to sleep in milliseconds
     *
     * @param  nanos
     *         {@code 0-999999} additional nanoseconds to sleep
     *
     * @throws  IllegalArgumentException
     *          if the value of {@code millis} is negative, or the value of
     *          {@code nanos} is not in the range {@code 0-999999}
     *
     * @throws  InterruptedException
     *          if any thread has interrupted the current thread. The
     *          <i>interrupted status</i> of the current thread is
     *          cleared when this exception is thrown.
     */
    // 使当前正在执行的线程休眠（暂时停止执行）指定的毫秒数(timeout毫秒加上nanos纳秒)，且当前线程不会失去任何监视器的所有权
    public static void sleep(long millis, int nanos) throws InterruptedException {
        if (millis < 0) {
            throw new IllegalArgumentException("timeout value is negative");
        }

        if (nanos < 0 || nanos > 999999) {
            throw new IllegalArgumentException(
                                "nanosecond timeout value out of range");
        }

        if (nanos >= 500000 || (nanos != 0 && millis == 0)) {
            millis++;
        }

        sleep(millis);
    }

    // 用当前的 AccessControlContext 初始化一个线程。
    /**
     * Initializes a Thread with the current AccessControlContext.
     *
     * @see #init(ThreadGroup,Runnable,String,long,AccessControlContext)
     */
    // 先获取当前Thread线程的引用, 然后初始化线程, 默认使用当前线程的线程组、守护线程标记、优先级、线程上下文类加载器、权限上下文等, 然后设置指定的运行任务和生成线程ID
    private void init(ThreadGroup g, Runnable target, String name, long stackSize) {
        init(g, target, name, stackSize, null);
    }

    // 初始化一个线程。
    /**
     * Initializes a Thread.
     *
     * @param g the Thread group // 线程组
     * @param target the object whose run() method gets called // 其 run() 方法被调用的对象
     * @param name the name of the new Thread // 新线程的名称
     * @param stackSize the desired stack size for the new thread, or // 新线程所需的堆栈大小，或为零表示将忽略此参数。
     *        zero to indicate that this parameter is to be ignored.
     * @param acc the AccessControlContext to inherit, or // 要继承的AccessControlContext，如果为 null，则为AccessController.getContext()
     *            AccessController.getContext() if null
     */
    // 初始化线程, 默认使用当前线程的线程组、守护线程标记、优先级、线程上下文类加载器、权限上下文等, 然后设置指定的运行任务和生成线程ID
    private void init(ThreadGroup g, Runnable target, String name, long stackSize, AccessControlContext acc) {
        if (name == null) {
            throw new NullPointerException("name cannot be null");
        }

        // 根据传入的name赋值给线程名称数组
        this.name = name.toCharArray();

        // 获取当前线程实例, 作为新初始化线程的父线程
        Thread parent = currentThread();
        SecurityManager security = System.getSecurityManager();

        // 如果没有指定线程组, 则使用当前线程的线程组, 作为新初始化线程的线程组
        if (g == null) {
            // 确定它是否是小程序
            /* Determine if it's an applet or not */

            // 如果有安全经理，请询问安全经理要做什么。
            /* If there is a security manager, ask the security manager what to do. */
            if (security != null) {
                g = security.getThreadGroup();
            }

            // 如果安全性对此事没有强烈意见，请使用父线程组。
            /* If the security doesn't have a strong opinion of the matter use the parent thread group. */
            if (g == null) {
                g = parent.getThreadGroup();
            }
        }

        // checkAccess无论是否显式传入线程组。
        /* checkAccess regardless of whether or not threadgroup is explicitly passed in. */
        // 确定当前运行的线程是否有权限修改这个线程组
        g.checkAccess();

        /*
         * Do we have the required permissions?
         */
        // 我们是否拥有所需的权限？
        if (security != null) {
            if (isCCLOverridden(getClass())) {
                security.checkPermission(SUBCLASS_IMPLEMENTATION_PERMISSION);
            }
        }

        // 增加线程组中未启动线程的计数, 未启动的线程不会被添加到线程组中, 但必须对其进行计数, 以便其中包含未启动线程的守护线程组不会被销毁
        g.addUnstarted();

        // 设置线程组、守护线程标记(取当前线程的)、优先级(取当前线程的)、线程上下文类加载器、权限上下文、运行的目标任务、InheritableThreadLocal值、请求的堆栈大小、以及生成线程ID
        this.group = g;
        this.daemon = parent.isDaemon();
        this.priority = parent.getPriority();
        if (security == null || isCCLOverridden(parent.getClass()))
            this.contextClassLoader = parent.getContextClassLoader();
        else
            this.contextClassLoader = parent.contextClassLoader;
        this.inheritedAccessControlContext = acc != null ? acc : AccessController.getContext();
        this.target = target;
        setPriority(priority);
        if (parent.inheritableThreadLocals != null)
            this.inheritableThreadLocals =
                ThreadLocal.createInheritedMap(parent.inheritableThreadLocals);
        // 存储指定的堆栈大小以防 VM 关心
        /* Stash the specified stack size in case the VM cares */
        this.stackSize = stackSize;

        // 设置线程 ID
        /* Set thread ID */
        tid = nextThreadID();
    }

    // 抛出CloneNotSupportedException作为无法有意义地克隆的线程。而是构造一个新线程。
    /**
     * Throws CloneNotSupportedException as a Thread can not be meaningfully
     * cloned. Construct a new Thread instead.
     *
     * @throws  CloneNotSupportedException
     *          always
     */
    @Override
    protected Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }

    /**
     * 20210725
     * 分配一个新的{@code Thread}对象。此构造函数与{@linkplain #Thread(ThreadGroup,Runnable,String) Thread} {@code (null, null, gname)} 效果相同，
     * 其中 {@code gname} 是新生成的名称。 自动生成的名称格式为 {@code "Thread-"+}<i>n</i>，其中 <i>n</i> 是一个整数。
     */
    /**
     * Allocates a new {@code Thread} object. This constructor has the same
     * effect as {@linkplain #Thread(ThreadGroup,Runnable,String) Thread}
     * {@code (null, null, gname)}, where {@code gname} is a newly generated
     * name. Automatically generated names are of the form
     * {@code "Thread-"+}<i>n</i>, where <i>n</i> is an integer.
     */
    // 空参构造函数, 先获取当前Thread线程的引用, 然后初始化线程, 默认使用当前线程的线程组、守护线程标记、优先级、线程上下文类加载器、权限上下文等, 然后设置指定的运行任务和生成线程ID, 以及线程名称"Thread-n"
    public Thread() {
        init(null, null, "Thread-" + nextThreadNum(), 0);
    }

    /**
     * 20210725
     * 分配一个新的 {@code Thread} 对象。 此构造函数与 {@linkplain #Thread(ThreadGroup,Runnable,String) Thread} {@code (null, target, gname)} 效果相同，
     * 其中 {@code gname} 是新生成的名称。 自动生成的名称格式为 {@code "Thread-"+}<i>n</i>，其中 <i>n</i> 是一个整数。
     */
    /**
     * Allocates a new {@code Thread} object. This constructor has the same
     * effect as {@linkplain #Thread(ThreadGroup,Runnable,String) Thread}
     * {@code (null, target, gname)}, where {@code gname} is a newly generated
     * name. Automatically generated names are of the form
     * {@code "Thread-"+}<i>n</i>, where <i>n</i> is an integer.
     *
     * @param  target
     *         the object whose {@code run} method is invoked when this thread
     *         is started. If {@code null}, this classes {@code run} method does
     *         nothing.
     */
    // 指定运行任务的构造函数(如果为null则执行Thread#run), 先获取当前Thread线程的引用, 然后初始化线程, 默认使用当前线程的线程组、守护线程标记、优先级、线程上下文类加载器、权限上下文等, 然后设置指定的运行任务和生成线程ID, 以及线程名称"Thread-n"
    public Thread(Runnable target) {
        init(null, target, "Thread-" + nextThreadNum(), 0);
    }

    // 创建一个继承给定 AccessControlContext 的新线程。 这不是公共构造函数。
    /**
     * Creates a new Thread that inherits the given AccessControlContext.
     * This is not a public constructor.
     */
    // 内部构造方法, 指定运行任务和权限上下文地构造Thread对象
    Thread(Runnable target, AccessControlContext acc) {
        init(null, target, "Thread-" + nextThreadNum(), 0, acc);
    }

    /**
     * 20210725
     * 分配一个新的 {@code Thread} 对象。 此构造函数与 {@linkplain #Thread(ThreadGroup,Runnable,String) Thread} {@code (null, target, gname)} 效果相同，
     * 其中 {@code gname} 是新生成的名称。 自动生成的名称格式为 {@code "Thread-"+}<i>n</i>，其中 <i>n</i> 是一个整数。
     */
    /**
     * Allocates a new {@code Thread} object. This constructor has the same
     * effect as {@linkplain #Thread(ThreadGroup,Runnable,String) Thread}
     * {@code (group, target, gname)} ,where {@code gname} is a newly generated
     * name. Automatically generated names are of the form
     * {@code "Thread-"+}<i>n</i>, where <i>n</i> is an integer.
     *
     * // 线程组。如果{@code null}且有安全管理器，则组由 {@linkplain SecurityManager#getThreadGroup SecurityManager.getThreadGroup()}确定。
     *    如果没有安全管理器或 {@code SecurityManager.getThreadGroup()} 返回 {@code null}，则该组设置为当前线程的线程组。
     * @param  group
     *         the thread group. If {@code null} and there is a security
     *         manager, the group is determined by {@linkplain
     *         SecurityManager#getThreadGroup SecurityManager.getThreadGroup()}.
     *         If there is not a security manager or {@code
     *         SecurityManager.getThreadGroup()} returns {@code null}, the group
     *         is set to the current thread's thread group.
     *
     * // 启动此线程时调用其{@code run}方法的对象。如果{@code null}，则调用此线程的run方法。
     * @param  target
     *         the object whose {@code run} method is invoked when this thread
     *         is started. If {@code null}, this thread's run method is invoked.
     *
     * @throws  SecurityException
     *          if the current thread cannot create a thread in the specified
     *          thread group
     */
    // 指定运行任务(如果为null则执行Thread#run)和线程组的构造函数, 先获取当前Thread线程的引用, 然后初始化线程, 默认使用当前线程的线程组、守护线程标记、优先级、线程上下文类加载器、权限上下文等, 然后设置指定的运行任务和生成线程ID, 以及线程名称"Thread-n"
    public Thread(ThreadGroup group, Runnable target) {
        init(group, target, "Thread-" + nextThreadNum(), 0);
    }

    /**
     * 20210725
     * 分配一个新的 {@code Thread} 对象。 此构造函数与 {@linkplain #Thread(ThreadGroup,Runnable,String) Thread} {@code (null, null, name)} 具有相同的效果。
     */
    /**
     * Allocates a new {@code Thread} object. This constructor has the same
     * effect as {@linkplain #Thread(ThreadGroup,Runnable,String) Thread}
     * {@code (null, null, name)}.
     *
     * @param   name
     *          the name of the new thread
     */
    // 指定线程名称的构造函数, 先获取当前Thread线程的引用, 然后初始化线程, 默认使用当前线程的线程组、守护线程标记、优先级、线程上下文类加载器、权限上下文等, 然后设置指定的运行任务和生成线程ID, 以及线程名称"Thread-n"
    public Thread(String name) {
        init(null, null, name, 0);
    }

    /**
     * 20210725
     * 分配一个新的 {@code Thread} 对象。 此构造函数与 {@linkplain #Thread(ThreadGroup,Runnable,String) Thread} {@code (group, null, name)} 具有相同的效果。
     */
    /**
     * Allocates a new {@code Thread} object. This constructor has the same
     * effect as {@linkplain #Thread(ThreadGroup,Runnable,String) Thread}
     * {@code (group, null, name)}.
     *
     * // 线程组。如果{@code null}且有安全管理器，则组由 {@linkplain SecurityManager#getThreadGroup SecurityManager.getThreadGroup()}确定。
     *    如果没有安全管理器或 {@code SecurityManager.getThreadGroup()} 返回 {@code null}，则该组设置为当前线程的线程组。
     * @param  group
     *         the thread group. If {@code null} and there is a security
     *         manager, the group is determined by {@linkplain
     *         SecurityManager#getThreadGroup SecurityManager.getThreadGroup()}.
     *         If there is not a security manager or {@code
     *         SecurityManager.getThreadGroup()} returns {@code null}, the group
     *         is set to the current thread's thread group.
     *
     * @param  name
     *         the name of the new thread
     *
     * @throws  SecurityException
     *          if the current thread cannot create a thread in the specified
     *          thread group
     */
    // 指定线程名称和线程组的构造函数, 先获取当前Thread线程的引用, 然后初始化线程, 默认使用当前线程的线程组、守护线程标记、优先级、线程上下文类加载器、权限上下文等, 然后设置指定的运行任务和生成线程ID, 以及线程名称"Thread-n"
    public Thread(ThreadGroup group, String name) {
        init(group, null, name, 0);
    }

    /**
     * 20210725
     * 分配一个新的 {@code Thread} 对象。此构造函数与{@linkplain #Thread(ThreadGroup,Runnable,String) Thread} {@code (null, target, name)}具有相同的效果。
     */
    /**
     * Allocates a new {@code Thread} object. This constructor has the same
     * effect as {@linkplain #Thread(ThreadGroup,Runnable,String) Thread}
     * {@code (null, target, name)}.
     *
     * @param  target
     *         the object whose {@code run} method is invoked when this thread
     *         is started. If {@code null}, this thread's run method is invoked.
     *
     * @param  name
     *         the name of the new thread
     */
    // 指定运行任务(如果为null则执行Thread#run)和线程名的构造函数, 先获取当前Thread线程的引用, 然后初始化线程, 默认使用当前线程的线程组、守护线程标记、优先级、线程上下文类加载器、权限上下文等, 然后设置指定的运行任务和生成线程ID, 以及线程名称"Thread-n"
    public Thread(Runnable target, String name) {
        init(null, target, name, 0);
    }

    /**
     * 20210725
     * A. 分配一个新的 {@code Thread} 对象，使其具有 {@code target} 作为其运行对象，具有指定的 {@code name} 作为其名称，并且属于 {@code group} 所引用的线程组。
     * B. 如果存在安全管理器，则调用其 {@link SecurityManager#checkAccess(ThreadGroup) checkAccess} 方法并使用 ThreadGroup 作为其参数。
     * C. 此外，当由覆盖{@code getContextClassLoader}或{@code setContextClassLoader}的子类的构造函数直接或间接调用时，它的{@code checkPermission}方法使用
     *    {@code RuntimePermission("enableContextClassLoaderOverride")}权限调用方法。
     * D. 新创建线程的优先级设置为等于创建它的线程的优先级，即当前运行的线程。 方法 {@linkplain #setPriority setPriority} 可用于将优先级更改为新值。
     * C. 当且仅当创建它的线程当前被标记为守护线程时，新创建的线程最初被标记为守护线程。 方法 {@linkplain #setDaemon setDaemon} 可用于更改线程是否为守护进程。
     */
    /**
     * A.
     * Allocates a new {@code Thread} object so that it has {@code target}
     * as its run object, has the specified {@code name} as its name,
     * and belongs to the thread group referred to by {@code group}.
     *
     * B.
     * <p>If there is a security manager, its
     * {@link SecurityManager#checkAccess(ThreadGroup) checkAccess}
     * method is invoked with the ThreadGroup as its argument.
     *
     * C.
     * <p>In addition, its {@code checkPermission} method is invoked with
     * the {@code RuntimePermission("enableContextClassLoaderOverride")}
     * permission when invoked directly or indirectly by the constructor
     * of a subclass which overrides the {@code getContextClassLoader}
     * or {@code setContextClassLoader} methods.
     *
     * D.
     * <p>The priority of the newly created thread is set equal to the
     * priority of the thread creating it, that is, the currently running
     * thread. The method {@linkplain #setPriority setPriority} may be
     * used to change the priority to a new value.
     *
     * E.
     * <p>The newly created thread is initially marked as being a daemon
     * thread if and only if the thread creating it is currently marked
     * as a daemon thread. The method {@linkplain #setDaemon setDaemon}
     * may be used to change whether or not a thread is a daemon.
     *
     * @param  group
     *         the thread group. If {@code null} and there is a security
     *         manager, the group is determined by {@linkplain
     *         SecurityManager#getThreadGroup SecurityManager.getThreadGroup()}.
     *         If there is not a security manager or {@code
     *         SecurityManager.getThreadGroup()} returns {@code null}, the group
     *         is set to the current thread's thread group.
     *
     * @param  target
     *         the object whose {@code run} method is invoked when this thread
     *         is started. If {@code null}, this thread's run method is invoked.
     *
     * @param  name
     *         the name of the new thread
     *
     * @throws  SecurityException
     *          if the current thread cannot create a thread in the specified
     *          thread group or cannot override the context class loader methods.
     */
    // 指定线程组、运行任务(如果为null则执行Thread#run)和线程名的构造函数, 先获取当前Thread线程的引用, 然后初始化线程, 默认使用当前线程的线程组、守护线程标记、优先级、线程上下文类加载器、权限上下文等, 然后设置指定的运行任务和生成线程ID, 以及线程名称"Thread-n"
    public Thread(ThreadGroup group, Runnable target, String name) {
        init(group, target, name, 0);
    }

    /**
     * 20210725
     * A. 分配一个新的 {@code Thread} 对象，使其具有 {@code target} 作为其运行对象，具有指定的 {@code name} 作为其名称，并且属于 {@code group} 所引用的线程组， 并具有指定的堆栈大小。
     * B. 此构造函数与 {@link #Thread(ThreadGroup,Runnable,String)} 相同，不同之处在于它允许指定线程堆栈大小。
     *    堆栈大小是虚拟机要为此线程堆栈分配的地址空间的近似字节数。 {@code stackSize} 参数的效果（如果有）高度依赖于平台。
     * C. 在某些平台上，为{@code stackSize} 参数指定更高的值可能允许线程在抛出 {@link StackOverflowError} 之前实现更大的递归深度。
     *    类似地，指定较低的值可能允许更多线程同时存在，而不会引发{@link OutOfMemoryError}（或其他内部错误）。
     *    stackSize参数的值与最大递归深度和并发级别之间的关系细节取决于平台。在某些平台上，{@code stackSize} 参数的值可能没有任何影响。
     * D. 虚拟机可以自由地将{@code stackSize}参数视为建议。如果平台的指定值过低，虚拟机可能会改为使用某些特定于平台的最小值；
     *    如果指定的值过高，虚拟机可能会改为使用某些特定于平台的最大值。同样，虚拟机可以随意向上或向下舍入它认为合适的指定值（或完全忽略它）。
     * E. 为{@code stackSize}参数指定零值将导致此构造函数的行为与{@code Thread(ThreadGroup, Runnable, String)} 构造函数完全相同。
     * F. 由于此构造函数的行为具有平台相关性，因此在使用时应格外小心。执行给定计算所需的线程堆栈大小可能因JRE实现而异。鉴于这种变化，可能需要仔细调整堆栈大小参数，
     *    并且可能需要针对要在其上运行应用程序的每个JRE实现重复调整。
     * G. 实现注意事项：鼓励Java平台实现者记录他们关于{@code stackSize}参数的实现行为。
     */
    /**
     * A.
     * Allocates a new {@code Thread} object so that it has {@code target}
     * as its run object, has the specified {@code name} as its name,
     * and belongs to the thread group referred to by {@code group}, and has
     * the specified <i>stack size</i>.
     *
     * B.
     * <p>This constructor is identical to {@link
     * #Thread(ThreadGroup,Runnable,String)} with the exception of the fact
     * that it allows the thread stack size to be specified.  The stack size
     * is the approximate number of bytes of address space that the virtual
     * machine is to allocate for this thread's stack.  <b>The effect of the
     * {@code stackSize} parameter, if any, is highly platform dependent.</b>
     *
     * C.
     * <p>On some platforms, specifying a higher value for the
     * {@code stackSize} parameter may allow a thread to achieve greater
     * recursion depth before throwing a {@link StackOverflowError}.
     * Similarly, specifying a lower value may allow a greater number of
     * threads to exist concurrently without throwing an {@link
     * OutOfMemoryError} (or other internal error).  The details of
     * the relationship between the value of the <tt>stackSize</tt> parameter
     * and the maximum recursion depth and concurrency level are
     * platform-dependent.  <b>On some platforms, the value of the
     * {@code stackSize} parameter may have no effect whatsoever.</b>
     *
     * D.
     * <p>The virtual machine is free to treat the {@code stackSize}
     * parameter as a suggestion.  If the specified value is unreasonably low
     * for the platform, the virtual machine may instead use some
     * platform-specific minimum value; if the specified value is unreasonably
     * high, the virtual machine may instead use some platform-specific
     * maximum.  Likewise, the virtual machine is free to round the specified
     * value up or down as it sees fit (or to ignore it completely).
     *
     * E.
     * <p>Specifying a value of zero for the {@code stackSize} parameter will
     * cause this constructor to behave exactly like the
     * {@code Thread(ThreadGroup, Runnable, String)} constructor.
     *
     * F.
     * <p><i>Due to the platform-dependent nature of the behavior of this
     * constructor, extreme care should be exercised in its use.
     * The thread stack size necessary to perform a given computation will
     * likely vary from one JRE implementation to another.  In light of this
     * variation, careful tuning of the stack size parameter may be required,
     * and the tuning may need to be repeated for each JRE implementation on
     * which an application is to run.</i>
     *
     * G.
     * <p>Implementation note: Java platform implementers are encouraged to
     * document their implementation's behavior with respect to the
     * {@code stackSize} parameter.
     *
     * @param  group
     *         the thread group. If {@code null} and there is a security
     *         manager, the group is determined by {@linkplain
     *         SecurityManager#getThreadGroup SecurityManager.getThreadGroup()}.
     *         If there is not a security manager or {@code
     *         SecurityManager.getThreadGroup()} returns {@code null}, the group
     *         is set to the current thread's thread group.
     *
     * @param  target
     *         the object whose {@code run} method is invoked when this thread
     *         is started. If {@code null}, this thread's run method is invoked.
     *
     * @param  name
     *         the name of the new thread
     *
     * @param  stackSize
     *         the desired stack size for the new thread, or zero to indicate
     *         that this parameter is to be ignored.
     *
     * @throws  SecurityException
     *          if the current thread cannot create a thread in the specified
     *          thread group
     *
     * @since 1.4
     */
    // 指定线程组、运行任务(如果为null则执行Thread#run)和线程名以及堆栈大小(会有平台相关性)的构造函数, 先获取当前Thread线程的引用, 然后初始化线程, 默认使用当前线程的线程组、守护线程标记、优先级、线程上下文类加载器、权限上下文等, 然后设置指定的运行任务和生成线程ID, 以及线程名称"Thread-n"
    public Thread(ThreadGroup group, Runnable target, String name, long stackSize) {
        init(group, target, name, stackSize);
    }

    /**
     * 20210725
     * A. 使该线程开始执行；Java虚拟机调用这个线程的run方法。
     * B. 结果是两个线程并发运行：当前线程（从调用start方法返回）和另一个线程（执行其run方法）。
     * C. 多次启动一个线程是不合法的。特别是，线程一旦完成执行就可能不会重新启动。
     */
    /**
     * A.
     * Causes this thread to begin execution; the Java Virtual Machine
     * calls the <code>run</code> method of this thread.
     *
     * B.
     * <p>
     * The result is that two threads are running concurrently: the
     * current thread (which returns from the call to the
     * <code>start</code> method) and the other thread (which executes its
     * <code>run</code> method).
     *
     * C.
     * <p>
     * It is never legal to start a thread more than once.
     * In particular, a thread may not be restarted once it has completed
     * execution.
     *
     * @exception  IllegalThreadStateException  if the thread was already
     *               started.
     * @see        #run()
     * @see        #stop()
     */
    // 开始执行该Thread执行线程, JVM调用其run方法, 而多次启动一个Thread执行线程是不合法的
    public synchronized void start() {

        /**
         * 20210725
         * A. 不会为VM创建/设置的主方法线程或“系统”组线程调用此方法。将来添加到此方法的任何新功能也可能必须添加到VM。
         * B. 零状态值对应于状态“NEW”。
         */
        /**
         * A.
         * This method is not invoked for the main method thread or "system"
         * group threads created/set up by the VM. Any new functionality added
         * to this method in the future may have to also be added to the VM.
         *
         * B.
         * A zero status value corresponds to state "NEW".
         */
        // 工具的Java线程状态，0表示线程“尚未启动”, 因此启动过的线程是不能被再次启动的
        if (threadStatus != 0)
            throw new IllegalThreadStateException();

        /**
         * 20210725
         * 通知组此线程即将启动，以便将其添加到组的线程列表中，并且可以递减组的未启动计数。
         */
        /*
         * Notify the group that this thread is about to be started
         * so that it can be added to the group's list of threads
         * and the group's unstarted count can be decremented.
         */
        // 将指定线程添加到线程组中
        group.add(this);

        // 调用native方法, 启动该Thread执行线程
        boolean started = false;
        try {
            start0();
            started = true;
        } finally {
            try {
                if (!started) {
                    // 通知组线程 {@code t} 尝试启动失败, 线程组的状态被回滚，就好像从未发生过启动线程的尝试一样
                    group.threadStartFailed(this);
                }
            } catch (Throwable ignore) {
                // 没做什么。如果start0抛出一个Throwable那么它将被传递到调用堆栈
                /* do nothing. If start0 threw a Throwable then it will be passed up the call stack */
            }
        }
    }

    // native方法, 启动该Thread执行线程
    private native void start0();

    /**
     * 20210727
     * A. 如果该线程是使用单独的 Runnable 运行对象构造的，则调用该 Runnable 对象的 run 方法； 否则，此方法不执行任何操作并返回。
     * B. Thread的子类应该覆盖这个方法。
     */
    /**
     * A.
     * If this thread was constructed using a separate
     * <code>Runnable</code> run object, then that
     * <code>Runnable</code> object's <code>run</code> method is called;
     * otherwise, this method does nothing and returns.
     *
     * B.
     * <p>
     * Subclasses of <code>Thread</code> should override this method.
     *
     * @see     #start()
     * @see     #stop()
     * @see     #Thread(ThreadGroup, Runnable, String)
     */
    // 如果运行的目标任务不为null, 则调用Runnable#run方法; 如果子类去重写这个方法, 则是通过多态调用来执行了(不过Thread类本身也是个Runnable实现)
    @Override
    public void run() {
        // 如果运行的目标任务不为null, 则调用Runnable#run方法
        if (target != null) {
            target.run();
        }
    }

    // 该方法由系统调用，以在线程实际退出之前给它一个清理的机会。
    /**
     * This method is called by the system to give a Thread
     * a chance to clean up before it actually exits.
     */
    // 系统调用, 用于线程实际退出之前做最后的清理工作
    private void exit() {
        // 如果线程组不为空, 则通知组线程{@code t}尝试启动失败, 并清空线程组
        if (group != null) {
            group.threadTerminated(this);
            group = null;
        }

        // 积极清除所有引用字段：参见错误 4006245
        /* Aggressively null out all reference fields: see bug 4006245 */
        target = null;

        // 释放其中一些资源
        /* Speed the release of some of these resources */
        threadLocals = null;
        inheritableThreadLocals = null;
        inheritedAccessControlContext = null;
        blocker = null;
        uncaughtExceptionHandler = null;
    }

    /**
     * 20210727
     * A. 强制线程停止执行。
     * B. 如果安装了安全管理器，则使用this作为参数调用其checkAccess方法。这可能会导致引发SecurityException（在当前线程中）。
     * C. 如果此线程与当前线程不同（即当前线程正在尝试停止除自身之外的线程），则另外调用安全管理器的 checkPermission 方法（带有 RuntimePermission("stopThread") 参数）。
     *    同样，这可能会导致抛出 SecurityException（在当前线程中）。
     * D. 该线程所代表的线程被迫停止任何它正在执行的异常操作，并将新创建的ThreadDeath对象作为异常抛出。
     * E. 允许停止尚未启动的线程。 如果线程最终启动，它会立即终止。
     * F. 应用程序通常不应该尝试捕获ThreadDeath，除非它必须执行一些特殊的清理操作（请注意，抛出ThreadDeath会导致在线程正式死亡之前执行try语句的finally 子句）。
     *    如果catch子句捕获了ThreadDeath对象，则重新抛出该对象很重要，这样线程才能真正死亡。
     * G. 如果未捕获的异常是ThreadDeath的实例，则对其他未捕获的异常做出反应的顶级错误处理程序不会打印出消息或以其他方式通知应用程序。
     */
    /**
     * A.
     * Forces the thread to stop executing.
     *
     * B.
     * <p>
     * If there is a security manager installed, its <code>checkAccess</code>
     * method is called with <code>this</code>
     * as its argument. This may result in a
     * <code>SecurityException</code> being raised (in the current thread).
     *
     * C.
     * <p>
     * If this thread is different from the current thread (that is, the current
     * thread is trying to stop a thread other than itself), the
     * security manager's <code>checkPermission</code> method (with a
     * <code>RuntimePermission("stopThread")</code> argument) is called in
     * addition.
     * Again, this may result in throwing a
     * <code>SecurityException</code> (in the current thread).
     *
     * D.
     * <p>
     * The thread represented by this thread is forced to stop whatever
     * it is doing abnormally and to throw a newly created
     * <code>ThreadDeath</code> object as an exception.
     *
     * E.
     * <p>
     * It is permitted to stop a thread that has not yet been started.
     * If the thread is eventually started, it immediately terminates.
     *
     * F.
     * <p>
     * An application should not normally try to catch
     * <code>ThreadDeath</code> unless it must do some extraordinary
     * cleanup operation (note that the throwing of
     * <code>ThreadDeath</code> causes <code>finally</code> clauses of
     * <code>try</code> statements to be executed before the thread
     * officially dies).  If a <code>catch</code> clause catches a
     * <code>ThreadDeath</code> object, it is important to rethrow the
     * object so that the thread actually dies.
     *
     * G.
     * <p>
     * The top-level error handler that reacts to otherwise uncaught
     * exceptions does not print out a message or otherwise notify the
     * application if the uncaught exception is an instance of
     * <code>ThreadDeath</code>.
     *
     * @exception  SecurityException  if the current thread cannot
     *               modify this thread.
     * @see        #interrupt()
     * @see        #checkAccess()
     * @see        #run()
     * @see        #start()
     * @see        ThreadDeath
     * @see        ThreadGroup#uncaughtException(Thread,Throwable)
     * @see        SecurityManager#checkAccess(Thread)
     * @see        SecurityManager#checkPermission
     *
     * // 这种方法本质上是不安全的。使用Thread.stop停止线程会使其解锁所有已锁定的监视器（这是未经检查的ThreadDeath异常向上传播堆栈的自然结果）。
     *    如果先前受这些监视器保护的任何对象处于不一致状态，则损坏的对象将对其他线程可见，从而可能导致任意行为。
     *    stop的许多用途应该被替换为简单地修改一些变量以指示目标线程应该停止运行的代码。目标线程应该定期检查这个变量，如果变量表明它要停止运行，
     *    则以有序的方式从它的run方法返回。 如果目标线程等待很长时间（例如在条件变量上），则应使用中断方法来中断等待。
     *    有关更多信息，请参阅为什么不推荐使用Thread.stop、Thread.suspend和Thread.resume？。
     * @deprecated This method is inherently unsafe.  Stopping a thread with
     *       Thread.stop causes it to unlock all of the monitors that it
     *       has locked (as a natural consequence of the unchecked
     *       <code>ThreadDeath</code> exception propagating up the stack).  If
     *       any of the objects previously protected by these monitors were in
     *       an inconsistent state, the damaged objects become visible to
     *       other threads, potentially resulting in arbitrary behavior.  Many
     *       uses of <code>stop</code> should be replaced by code that simply
     *       modifies some variable to indicate that the target thread should
     *       stop running.  The target thread should check this variable
     *       regularly, and return from its run method in an orderly fashion
     *       if the variable indicates that it is to stop running.  If the
     *       target thread waits for long periods (on a condition variable,
     *       for example), the <code>interrupt</code> method should be used to
     *       interrupt the wait.
     *       For more information, see
     *       <a href="{@docRoot}/../technotes/guides/concurrency/threadPrimitiveDeprecation.html">Why
     *       are Thread.stop, Thread.suspend and Thread.resume Deprecated?</a>.
     */
    // 强制使实例线程停止执行, 被迫停止它任何正在执行的操作, 并将新创建的ThreadDeath Error对象作为Throwable抛出, 已被废弃, 因为会出现数据不同步, 所以可以使用同步变量作为停止标记来实现线程停止(Thread#isInterrupted()本质上就是个中断标记)
    @Deprecated
    public final void stop() {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            checkAccess();
            if (this != Thread.currentThread()) {
                security.checkPermission(SecurityConstants.STOP_THREAD_PERMISSION);
            }
        }

        // 零状态值对应于“NEW”，它不能更改为not-NEW，因为我们持有锁。
        // A zero status value corresponds to "NEW", it can't change to
        // not-NEW because we hold the lock.
        if (threadStatus != 0) {
            // 恢复挂起的线程, 已被弃用，因为容易导致死锁: 当该线程持有锁被挂起(保持等待), 而另一个线程需要请求该锁才能恢复该线程, 则发生死锁
            resume(); // Wake up thread if it was suspended; no-op otherwise // 如果线程被挂起，则唤醒线程；否则无操作
        }

        // VM 可以处理所有线程状态
        // The VM can handle all thread states
        stop0(new ThreadDeath());
    }

    // 抛出 {@code UnsupportedOperationException}。
    /**
     * Throws {@code UnsupportedOperationException}.
     *
     * @param obj ignored
     *
     * // 此方法最初旨在强制线程停止并将给定的 {@code Throwable} 作为异常抛出。 它本质上是不安全的（有关详细信息，请参阅 {@link #stop()}），
     *    而且还可用于生成目标线程未准备好处理的异常。 有关更多信息，请参阅为什么不推荐使用 Thread.stop、Thread.suspend 和 Thread.resume？。
     * @deprecated This method was originally designed to force a thread to stop
     *        and throw a given {@code Throwable} as an exception. It was
     *        inherently unsafe (see {@link #stop()} for details), and furthermore
     *        could be used to generate exceptions that the target thread was
     *        not prepared to handle.
     *        For more information, see
     *        <a href="{@docRoot}/../technotes/guides/concurrency/threadPrimitiveDeprecation.html">Why
     *        are Thread.stop, Thread.suspend and Thread.resume Deprecated?</a>.
     */
    // 强制线程停止并将给定的 {@code Throwable} 作为异常抛出, 已被废弃, 因为可能会生成目标线程未准备好处理的异常
    @Deprecated
    public final synchronized void stop(Throwable obj) {
        throw new UnsupportedOperationException();
    }

    /**
     * 20210726
     * A. 中断此线程。
     * B. 除非当前线程自己中断，这总是被允许的，否则会调用此线程的 {@link #checkAccess() checkAccess} 方法，这可能会导致抛出 {@link SecurityException}。
     * C. 如果此线程在调用{@link Object#wait() wait()}、{@link Object#wait(long) wait(long)} 或 {@link Object#wait(long, int)时被阻塞{@link Object}类
     *    或{@link #join()}、{@link #join(long)}、{@link #join(long, int)}的wait(long, int)} 方法 , {@link #sleep(long)}, or{@link #sleep(long, int)}，
     *    这个类的方法，那么它的中断状态将被清除，它会收到一个{@link InterruptedException}。
     * D. 如果此线程在{@link java.nio.channels.InterruptibleChannel InterruptibleChannel}上的I/O操作中被阻塞，则通道将关闭，线程的中断状态将被设置，
     *    线程将收到一个{@link java.nio.channels.ClosedByInterruptException}。
     * E. 如果此线程在{@link java.nio.channels.Selector}中被阻塞，则该线程的中断状态将被设置，并且它将立即从选择操作中返回，可能具有非零值，
     *    就像选择器的{@link java.nio.channels.Selector#wakeup wakeup}方法被调用。
     * F. 如果前面的条件都不成立，则将设置此线程的中断状态。
     * G. 中断一个不活跃的线程不需要有任何影响。
     */
    /**
     * A.
     * Interrupts this thread.
     *
     * B.
     * <p> Unless the current thread is interrupting itself, which is
     * always permitted, the {@link #checkAccess() checkAccess} method
     * of this thread is invoked, which may cause a {@link
     * SecurityException} to be thrown.
     *
     * C.
     * <p> If this thread is blocked in an invocation of the {@link
     * Object#wait() wait()}, {@link Object#wait(long) wait(long)}, or {@link
     * Object#wait(long, int) wait(long, int)} methods of the {@link Object}
     * class, or of the {@link #join()}, {@link #join(long)}, {@link
     * #join(long, int)}, {@link #sleep(long)}, or {@link #sleep(long, int)},
     * methods of this class, then its interrupt status will be cleared and it
     * will receive an {@link InterruptedException}.
     *
     * D.
     * <p> If this thread is blocked in an I/O operation upon an {@link
     * java.nio.channels.InterruptibleChannel InterruptibleChannel}
     * then the channel will be closed, the thread's interrupt
     * status will be set, and the thread will receive a {@link
     * java.nio.channels.ClosedByInterruptException}.
     *
     * E.
     * <p> If this thread is blocked in a {@link java.nio.channels.Selector}
     * then the thread's interrupt status will be set and it will return
     * immediately from the selection operation, possibly with a non-zero
     * value, just as if the selector's {@link
     * java.nio.channels.Selector#wakeup wakeup} method were invoked.
     *
     * F.
     * <p> If none of the previous conditions hold then this thread's interrupt
     * status will be set. </p>
     *
     * G.
     * <p> Interrupting a thread that is not alive need not have any effect.
     *
     * @throws  SecurityException
     *          if the current thread cannot modify this thread
     *
     * @revised 6.0
     * @spec JSR-51
     */
    // 中断该线程, 如果从Thread其他实例方法调用该方法, 则会清除中断状态, 然后会收到一个{@link InterruptedException}
    public void interrupt() {
        if (this != Thread.currentThread())
            checkAccess();

        synchronized (blockerLock) {
            Interruptible b = blocker;
            if (b != null) {
                interrupt0();           // Just to set the interrupt flag
                b.interrupt(this);
                return;
            }
        }
        interrupt0();
    }

    /**
     * 20210726
     * A. 测试当前线程是否被中断。通过该方法清除线程的中断状态。换句话说，如果这个方法被连续调用两次，
     *    第二次调用将返回false（除非当前线程再次被中断，在第一个调用清除其中断状态之后和第二个调用检查它之前）。
     * B. 由于线程在中断时未处于活动状态而被忽略的线程中断将通过此方法返回false来反映。
     */
    /**
     * A.
     * Tests whether the current thread has been interrupted.  The
     * <i>interrupted status</i> of the thread is cleared by this method.  In
     * other words, if this method were to be called twice in succession, the
     * second call would return false (unless the current thread were
     * interrupted again, after the first call had cleared its interrupted
     * status and before the second call had examined it).
     *
     * B.
     * <p>A thread interruption ignored because a thread was not alive
     * at the time of the interrupt will be reflected by this method
     * returning false.
     *
     * // 如果当前线程已被中断，则为真； 否则为假。
     * @return  <code>true</code> if the current thread has been interrupted;
     *          <code>false</code> otherwise.
     * @see #isInterrupted()
     * @revised 6.0
     */
    // 测试当前线程是否被中断, 该方法会清除线程的中断状态
    public static boolean interrupted() {
        return currentThread().isInterrupted(true);
    }

    /**
     * 20210726
     * A. 测试此线程是否已被中断。 线程的中断状态不受此方法的影响。
     * B. 由于线程在中断时未处于活动状态而被忽略的线程中断将通过此方法返回 false 来反映。
     */
    /**
     * A.
     * Tests whether this thread has been interrupted.  The <i>interrupted
     * status</i> of the thread is unaffected by this method.
     *
     * B.
     * <p>A thread interruption ignored because a thread was not alive
     * at the time of the interrupt will be reflected by this method
     * returning false.
     *
     * @return  <code>true</code> if this thread has been interrupted;
     *          <code>false</code> otherwise.
     * @see     #interrupted()
     * @revised 6.0
     */
    // 测试当前线程是否被中断, 该方法不会清除线程的中断状态
    public boolean isInterrupted() {
        return isInterrupted(false);
    }

    /**
     * 20210726
     * 测试某个线程是否已被中断。中断状态是否根据传递的ClearInterrupted值重置。
     */
    /**
     * Tests if some Thread has been interrupted.  The interrupted state
     * is reset or not based on the value of ClearInterrupted that is
     * passed.
     */
    // 测试线程是否已被中断, ClearInterrupted为true时会清除线程的中断状态
    private native boolean isInterrupted(boolean ClearInterrupted);

    // 抛出 {@link NoSuchMethodError}。
    /**
     * Throws {@link NoSuchMethodError}.
     *
     * // 该方法最初设计用于在不进行任何清理的情况下销毁该线程。它持有的任何监视器都将保持锁定状态。但是，该方法从未实施。
     *    如果要实施，它将以{@link #suspend}的方式容易死锁。如果目标线程在关键系统资源被销毁时持有一个锁来保护它，那么没有线程可以再次访问该资源。
     *    如果另一个线程试图锁定此资源，则会导致死锁。这种死锁通常表现为“冻结”进程。
     *    有关更多信息，请参阅为什么不推荐使用 Thread.stop、Thread.suspend 和 Thread.resume？。
     * @deprecated This method was originally designed to destroy this
     *     thread without any cleanup. Any monitors it held would have
     *     remained locked. However, the method was never implemented.
     *     If if were to be implemented, it would be deadlock-prone in
     *     much the manner of {@link #suspend}. If the target thread held
     *     a lock protecting a critical system resource when it was
     *     destroyed, no thread could ever access this resource again.
     *     If another thread ever attempted to lock this resource, deadlock
     *     would result. Such deadlocks typically manifest themselves as
     *     "frozen" processes. For more information, see
     *     <a href="{@docRoot}/../technotes/guides/concurrency/threadPrimitiveDeprecation.html">
     *     Why are Thread.stop, Thread.suspend and Thread.resume Deprecated?</a>.
     * @throws NoSuchMethodError always
     */
    // 用于在不进行任何清理的情况下销毁实例线程, 该方法未做未实现, 已被废弃, 因为和suspend一样容易导致死锁, 会在线程资源被销毁后仍然持有锁
    @Deprecated
    public void destroy() {
        throw new NoSuchMethodError();
    }

    // 测试此线程是否存活。如果线程已启动且尚未死亡，则该线程处于活动状态。
    /**
     * Tests if this thread is alive. A thread is alive if it has
     * been started and has not yet died.
     *
     * // 如果此线程还活着，则为真； 否则为假。
     * @return  <code>true</code> if this thread is alive;
     *          <code>false</code> otherwise.
     */
    // 判断该线程是否还存活
    public final native boolean isAlive();

    /**
     * 20210727
     * A. 暂停此线程。
     * B. 首先，不带参数调用此线程的 checkAccess 方法。 这可能会导致抛出 SecurityException（在当前线程中）。
     * C. 如果线程处于活动状态，则它会被挂起并且不会进一步进行，除非并且直到它被恢复。
     */
    /**
     * A.
     * Suspends this thread.
     *
     * B.
     * <p>
     * First, the <code>checkAccess</code> method of this thread is called
     * with no arguments. This may result in throwing a
     * <code>SecurityException </code>(in the current thread).
     *
     * C.
     * <p>
     * If the thread is alive, it is suspended and makes no further
     * progress unless and until it is resumed.
     *
     * @exception  SecurityException  if the current thread cannot modify
     *               this thread.
     * @see #checkAccess
     *
     * // 此方法已被弃用，因为它本质上容易死锁。如果目标线程在挂起时保护关键系统资源的监视器上持有锁，则在目标线程恢复之前，没有线程可以访问该资源。
     *    如果将恢复目标线程的线程尝试在调用resume之前锁定此监视器，则会导致死锁。这种死锁通常表现为“冻结”进程。有关更多信息，
     *    请参阅为什么不推荐使用Thread.stop、Thread.suspend 和 Thread.resume？。
     * @deprecated   This method has been deprecated, as it is
     *   inherently deadlock-prone.  If the target thread holds a lock on the
     *   monitor protecting a critical system resource when it is suspended, no
     *   thread can access this resource until the target thread is resumed. If
     *   the thread that would resume the target thread attempts to lock this
     *   monitor prior to calling <code>resume</code>, deadlock results.  Such
     *   deadlocks typically manifest themselves as "frozen" processes.
     *   For more information, see
     *   <a href="{@docRoot}/../technotes/guides/concurrency/threadPrimitiveDeprecation.html">Why
     *   are Thread.stop, Thread.suspend and Thread.resume Deprecated?</a>.
     */
    // 暂停该线程, 已被弃用，因为容易导致死锁: 当该线程持有锁被挂起(保持等待), 而另一个线程需要请求该锁才能恢复该线程, 则发生死锁
    @Deprecated
    public final void suspend() {
        checkAccess();
        suspend0();
    }

    /**
     * 20210727
     * A. 恢复挂起的线程。
     * B. 首先，不带参数调用此线程的 checkAccess 方法。 这可能会导致抛出 SecurityException（在当前线程中）。
     * C. 如果线程处于活动状态但被挂起，则它会被恢复并允许在其执行过程中取得进展。
     */
    /**
     * A.
     * Resumes a suspended thread.
     *
     * B.
     * <p>
     * First, the <code>checkAccess</code> method of this thread is called
     * with no arguments. This may result in throwing a
     * <code>SecurityException</code> (in the current thread).
     *
     * C.
     * <p>
     * If the thread is alive but suspended, it is resumed and is
     * permitted to make progress in its execution.
     *
     * @exception  SecurityException  if the current thread cannot modify this
     *               thread.
     * @see        #checkAccess
     * @see        #suspend()
     *
     * // 此方法仅用于与 {@link #suspend} 一起使用，该方法已被弃用，因为它容易死锁。
     *    有关更多信息，请参阅为什么不推荐使用 Thread.stop、Thread.suspend 和 Thread.resume？。
     * @deprecated This method exists solely for use with {@link #suspend},
     *     which has been deprecated because it is deadlock-prone.
     *     For more information, see
     *     <a href="{@docRoot}/../technotes/guides/concurrency/threadPrimitiveDeprecation.html">Why
     *     are Thread.stop, Thread.suspend and Thread.resume Deprecated?</a>.
     */
    // 恢复挂起的线程, 已被弃用，因为容易导致死锁: 当该线程持有锁被挂起(保持等待), 而另一个线程需要请求该锁才能恢复该线程, 则发生死锁
    @Deprecated
    public final void resume() {
        checkAccess();
        resume0();
    }

    /**
     * 20210728
     * A. 更改此线程的优先级。
     * B. 首先不带参数调用此线程的 checkAccess 方法。 这可能会导致抛出 SecurityException。
     * C. 否则，该线程的优先级被设置为指定的 newPriority 和线程的线程组的最大允许优先级中的较小者。
     */
    /**
     * A.
     * Changes the priority of this thread.
     *
     * B.
     * <p>
     * First the <code>checkAccess</code> method of this thread is called
     * with no arguments. This may result in throwing a
     * <code>SecurityException</code>.
     *
     * C.
     * <p>
     * Otherwise, the priority of this thread is set to the smaller of
     * the specified <code>newPriority</code> and the maximum permitted
     * priority of the thread's thread group.
     *
     * @param newPriority priority to set this thread to
     * @exception  IllegalArgumentException  If the priority is not in the
     *               range <code>MIN_PRIORITY</code> to
     *               <code>MAX_PRIORITY</code>.
     * @exception  SecurityException  if the current thread cannot modify
     *               this thread.
     * @see        #getPriority
     * @see        #checkAccess()
     * @see        #getThreadGroup()
     * @see        #MAX_PRIORITY
     * @see        #MIN_PRIORITY
     * @see        ThreadGroup#getMaxPriority()
     */
    // 更改此线程的优先级(1<x<10), 如果指定的优先级大于线程组最大优先级, 则使用线程组最大的优先级
    public final void setPriority(int newPriority) {
        ThreadGroup g;
        checkAccess();

        // 如果指定的优先级大于10, 或者小于1, 则抛出异常
        if (newPriority > MAX_PRIORITY || newPriority < MIN_PRIORITY) {
            throw new IllegalArgumentException();
        }

        // 获取该线程组, 如果指定的优先级大于线程组最大优先级, 则使用线程组最大的优先级
        if((g = getThreadGroup()) != null) {
            if (newPriority > g.getMaxPriority()) {
                newPriority = g.getMaxPriority();
            }
            setPriority0(priority = newPriority);
        }
    }

    // 返回此线程的优先级。
    /**
     * Returns this thread's priority.
     *
     * @return  this thread's priority.
     * @see     #setPriority
     */
    // 返回实例线程的优先级
    public final int getPriority() {
        return priority;
    }

    /**
     * 20210715
     * A. 将此线程的名称更改为等于参数名称。
     * B. 首先不带参数调用此线程的 checkAccess 方法。 这可能会导致抛出 SecurityException。
     */
    /**
     * A.
     * Changes the name of this thread to be equal to the argument
     * <code>name</code>.
     *
     * B.
     * <p>
     * First the <code>checkAccess</code> method of this thread is called
     * with no arguments. This may result in throwing a
     * <code>SecurityException</code>.
     *
     * @param      name   the new name for this thread.
     * @exception  SecurityException  if the current thread cannot modify this
     *               thread.
     * @see        #getName
     * @see        #checkAccess()
     */
    // 将实例线程名称更改指定名称
    public final synchronized void setName(String name) {
        checkAccess();
        this.name = name.toCharArray();
        if (threadStatus != 0) {
            setNativeName(name);
        }
    }

    // 返回此线程的名称。
    /**
     * Returns this thread's name.
     *
     * @return  this thread's name.
     * @see     #setName(String)
     */
    // 返回实例线程的名称, 返回新的String对象
    public final String getName() {
        return new String(name, true);
    }

    // 返回该线程所属的线程组。 如果此线程已死亡（已停止），则此方法返回 null。
    /**
     * Returns the thread group to which this thread belongs.
     * This method returns null if this thread has died
     * (been stopped).
     *
     * @return  this thread's thread group.
     */
    // 返回实例线程所属的线程组, 如果线程已死亡（已停止）, 则返回null
    public final ThreadGroup getThreadGroup() {
        return group;
    }

    /**
     * 20210728
     * A. 返回当前线程的 {@linkplain java.lang.ThreadGroup 线程组} 及其子组中活动线程数的估计值。 递归迭代当前线程的线程组中的所有子组。
     * B. 返回值只是一个估计值，因为该方法遍历内部数据结构时，线程数可能会动态变化，并且可能会受到某些系统线程存在的影响。 此方法主要用于调试和监视目的。
     */
    /**
     * A.
     * Returns an estimate of the number of active threads in the current
     * thread's {@linkplain java.lang.ThreadGroup thread group} and its
     * subgroups. Recursively iterates over all subgroups in the current
     * thread's thread group.
     *
     * B.
     * <p> The value returned is only an estimate because the number of
     * threads may change dynamically while this method traverses internal
     * data structures, and might be affected by the presence of certain
     * system threads. This method is intended primarily for debugging
     * and monitoring purposes.
     *
     * @return  an estimate of the number of active threads in the current
     *          thread's thread group and in any other thread group that
     *          has the current thread's thread group as an ancestor
     */
    // 递归迭代当前线程的线程组中的所有子组, 返回当前线程组及其子组中活动线程数的估计值
    public static int activeCount() {
        return currentThread().getThreadGroup().activeCount();
    }

    /**
     * 20210728
     * A. 将当前线程的线程组及其子组中的每个活动线程复制到指定的数组中。该方法只是调用当前线程的线程组的{@link java.lang.ThreadGroup#enumerate(Thread[])} 方法。
     * B. 应用程序可能会使用 {@linkplain #activeCount activeCount} 方法来估计数组应该有多大，但是如果数组太短而无法容纳所有线程，则额外的线程将被默默忽略。
     *    如果获取当前线程的线程组及其子组中的每个活动线程至关重要，则调用者应验证返回的 int 值是否严格小于 {@code tarray} 的长度。
     * C. 由于此方法固有的竞争条件，建议该方法仅用于调试和监视目的。
     */
    /**
     * A.
     * Copies into the specified array every active thread in the current
     * thread's thread group and its subgroups. This method simply
     * invokes the {@link java.lang.ThreadGroup#enumerate(Thread[])}
     * method of the current thread's thread group.
     *
     * B.
     * <p> An application might use the {@linkplain #activeCount activeCount}
     * method to get an estimate of how big the array should be, however
     * <i>if the array is too short to hold all the threads, the extra threads
     * are silently ignored.</i>  If it is critical to obtain every active
     * thread in the current thread's thread group and its subgroups, the
     * invoker should verify that the returned int value is strictly less
     * than the length of {@code tarray}.
     *
     * C.
     * <p> Due to the inherent race condition in this method, it is recommended
     * that the method only be used for debugging and monitoring purposes.
     *
     * @param  tarray
     *         an array into which to put the list of threads
     *
     * @return  the number of threads put into the array
     *
     * @throws  SecurityException
     *          if {@link java.lang.ThreadGroup#checkAccess} determines that
     *          the current thread cannot access its thread group
     */
    // 将当前线程的线程组及其子组中的每个活动线程复制到指定的数组中, 其中会使用activeCount方法来估计数组应该有多大，如果数组太短而无法容纳所有线程，则额外的线程将被忽略
    public static int enumerate(Thread tarray[]) {
        return currentThread().getThreadGroup().enumerate(tarray);
    }

    // 计算此线程中的堆栈帧数。 线程必须被挂起。
    /**
     * Counts the number of stack frames in this thread. The thread must
     * be suspended.
     *
     * @return     the number of stack frames in this thread.
     * @exception  IllegalThreadStateException  if this thread is not
     *             suspended.
     *
     * // 此调用的定义取决于已弃用的 {@link #suspend}。 此外，此调用的结果从未明确定义。
     * @deprecated The definition of this call depends on {@link #suspend},
     *             which is deprecated.  Further, the results of this call
     *             were never well-defined.
     */
    // 计算此线程中的堆栈帧数, 但线程必须被挂起, 已被废弃, 和suspend一样, 会容易导致死锁
    @Deprecated
    public native int countStackFrames();

    /**
     * 20210728
     * A. 最多等待{@code millis}毫秒让该线程死亡。{@code 0} 超时意味着永远等待。
     * B. 此实现使用以{@code this.isAlive}为条件的{@code this.wait}调用循环。当线程终止时，调用{@code this.notifyAll}方法。
     *    建议应用程序不要在{@code Thread}实例上使用{@code wait}、{@code notify}或{@code notifyAll}。
     */
    /**
     * A.
     * Waits at most {@code millis} milliseconds for this thread to
     * die. A timeout of {@code 0} means to wait forever.
     *
     * B.
     * <p> This implementation uses a loop of {@code this.wait} calls
     * conditioned on {@code this.isAlive}. As a thread terminates the
     * {@code this.notifyAll} method is invoked. It is recommended that
     * applications not use {@code wait}, {@code notify}, or
     * {@code notifyAll} on {@code Thread} instances.
     *
     * @param  millis
     *         the time to wait in milliseconds
     *
     * @throws  IllegalArgumentException
     *          if the value of {@code millis} is negative
     *
     * // 如果任何线程中断了当前线程。抛出此异常时清除当前线程的中断状态。
     * @throws  InterruptedException
     *          if any thread has interrupted the current thread. The
     *          <i>interrupted status</i> of the current thread is
     *          cleared when this exception is thrown.
     */
    // 当前线程等待millis毫秒让实例线程死亡, 为0则意味着永远等待, 实质上是调用当前Thread实例的wait方法进行阻塞等待, 因此建议应用程序不要在{@code Thread}实例上使用{@code wait}、{@code notify}或{@code notifyAll}。
    // 如果Thread实例被销毁, 则其等待集也会被销毁, 从而使得当前线程得以释放, 唤醒该线程, 又由于Thread实例已死亡, 则会退出循环, 停止等待
    public final synchronized void join(long millis) throws InterruptedException {
        // 获取系统当前毫秒数
        long base = System.currentTimeMillis();
        long now = 0;
        if (millis < 0) {
            throw new IllegalArgumentException("timeout value is negative");
        }

        // 如果指定的millis为0, 说明需要永久阻塞等待, 直到该Thread实例对象调用notify、notifyAll、中断或者指定时间过去(为0时需要一直等待)
        if (millis == 0) {
            // 判断该实例线程是否还存活
            while (isAlive()) {
                // 当前线程阻塞等待该Thread实例对象调用notify、notifyAll、中断或者指定时间过去(为0时需要一直等待), 只能由作为此对象监视器的所有者线程调用(通过synchronized获取): 一次只有一个线程可以拥有一个对象的监视器, 但监视器上可以有多个等待线程(通过await方法成为等待线程)
                // 如果Thread实例被销毁, 则其等待集也会被销毁, 从而使得当前线程得以释放, 唤醒该线程, 又由于Thread实例已死亡, 则会退出循环, 停止等待
                wait(0);
            }
        }
        // 如果指定的millis大于0, 说明需要定时等待, 直到该Thread实例对象调用notify、notifyAll、中断或者指定时间过去(为0时需要一直等待)
        else {
            // 判断该实例线程是否还存活
            while (isAlive()) {
                // 过期时长 = 指定的时长 - 现在时长(默认为0)
                long delay = millis - now;

                // 如果过期时长 < 0, 则退出循环
                if (delay <= 0) {
                    break;
                }

                // 当前线程阻塞等待该Thread实例对象调用notify、notifyAll、中断或者指定时间过去(为0时需要一直等待), 只能由作为此对象监视器的所有者线程调用(通过synchronized获取): 一次只有一个线程可以拥有一个对象的监视器, 但监视器上可以有多个等待线程(通过await方法成为等待线程)
                // 如果Thread实例被销毁, 则其等待集也会被销毁, 从而使得当前线程得以释放, 唤醒该线程, 又由于Thread实例已死亡, 则会退出循环, 停止等待
                wait(delay);

                // 重新计算现在时长 = 系统最新的毫秒数 - 过去系统的毫秒数
                now = System.currentTimeMillis() - base;
            }
        }
    }

    /**
     * 20210728
     * A. 最多等待{@code millis}毫秒加上{@code nanos}纳秒以使该线程死亡。
     * B. 此实现使用以{@code this.isAlive}为条件的{@code this.wait}调用循环。当线程终止时，调用{@code this.notifyAll}方法。
     *    建议应用程序不要在{@code Thread}实例上使用{@code wait}、{@code notify}或{@code notifyAll}。
     */
    /**
     * A.
     * Waits at most {@code millis} milliseconds plus
     * {@code nanos} nanoseconds for this thread to die.
     *
     * B.
     * <p> This implementation uses a loop of {@code this.wait} calls
     * conditioned on {@code this.isAlive}. As a thread terminates the
     * {@code this.notifyAll} method is invoked. It is recommended that
     * applications not use {@code wait}, {@code notify}, or
     * {@code notifyAll} on {@code Thread} instances.
     *
     * @param  millis
     *         the time to wait in milliseconds
     *
     * @param  nanos
     *         {@code 0-999999} additional nanoseconds to wait
     *
     * @throws  IllegalArgumentException
     *          if the value of {@code millis} is negative, or the value
     *          of {@code nanos} is not in the range {@code 0-999999}
     *
     * // 如果任何线程中断了当前线程。抛出此异常时清除当前线程的中断状态。
     * @throws  InterruptedException
     *          if any thread has interrupted the current thread. The
     *          <i>interrupted status</i> of the current thread is
     *          cleared when this exception is thrown.
     */
    // 当前线程等待(millis毫秒+nanos纳秒)让实例线程死亡, 实质上是调用当前Thread实例的wait方法进行阻塞等待, 因此建议应用程序不要在{@code Thread}实例上使用{@code wait}、{@code notify}或{@code notifyAll}。
    // 如果Thread实例被销毁, 则其等待集也会被销毁, 从而使得当前线程得以释放, 唤醒该线程, 又由于Thread实例已死亡, 则会退出循环, 停止等待
    public final synchronized void join(long millis, int nanos) throws InterruptedException {
        if (millis < 0) {
            throw new IllegalArgumentException("timeout value is negative");
        }
        if (nanos < 0 || nanos > 999999) {
            throw new IllegalArgumentException(
                                "nanosecond timeout value out of range");
        }
        if (nanos >= 500000 || (nanos != 0 && millis == 0)) {
            millis++;
        }

        // 当前线程等待millis毫秒让实例线程死亡, 为0则意味着永远等待, 实质上是调用当前Thread实例的wait方法进行阻塞等待, 因此建议应用程序不要在{@code Thread}实例上使用{@code wait}、{@code notify}或{@code notifyAll}。
        // 如果Thread实例被销毁, 则其等待集也会被销毁, 从而使得当前线程得以释放, 唤醒该线程, 又由于Thread实例已死亡, 则会退出循环, 停止等待
        join(millis);
    }

    /**
     * 20210728
     * A. 等待这个线程死亡。
     * B. 此方法的调用行为与调用完全相同。
     */
    /**
     * A.
     * Waits for this thread to die.
     *
     * B.
     * <p> An invocation of this method behaves in exactly the same
     * way as the invocation
     *
     * <blockquote>
     * {@linkplain #join(long) join}{@code (0)}
     * </blockquote>
     *
     * // 如果任何线程中断了当前线程。抛出此异常时清除当前线程的中断状态。
     * @throws  InterruptedException
     *          if any thread has interrupted the current thread. The
     *          <i>interrupted status</i> of the current thread is
     *          cleared when this exception is thrown.
     */
    // 当前线程永远等待实例线程死亡, 实质上是调用当前Thread实例的wait方法进行阻塞等待, 因此建议应用程序不要在{@code Thread}实例上使用{@code wait}、{@code notify}或{@code notifyAll}。
    // 如果Thread实例被销毁, 则其等待集也会被销毁, 从而使得当前线程得以释放, 唤醒该线程, 又由于Thread实例已死亡, 则会退出循环, 停止等待
    public final void join() throws InterruptedException {
        join(0);
    }

    // 将当前线程的堆栈跟踪打印到标准错误流。 此方法仅用于调试。
    /**
     * Prints a stack trace of the current thread to the standard error stream.
     * This method is used only for debugging.
     *
     * @see     Throwable#printStackTrace()
     */
    public static void dumpStack() {
        new Exception("Stack trace").printStackTrace();
    }

    /**
     * 20210728
     * A. 将此线程标记为{@linkplain #isDaemon daemon}线程或用户线程。当唯一运行的线程都是守护线程时，Java虚拟机退出。
     * B. 必须在线程启动之前调用此方法。
     */
    /**
     * A.
     * Marks this thread as either a {@linkplain #isDaemon daemon} thread
     * or a user thread. The Java Virtual Machine exits when the only
     * threads running are all daemon threads.
     *
     * B.
     * <p> This method must be invoked before the thread is started.
     *
     * // 如果 {@code true}，则将此线程标记为守护线程
     * @param  on
     *         if {@code true}, marks this thread as a daemon thread
     *
     * @throws  IllegalThreadStateException
     *          if this thread is {@linkplain #isAlive alive}
     *
     * @throws  SecurityException
     *          if {@link #checkAccess} determines that the current
     *          thread cannot modify this thread
     */
    // 在线程启动之前, 将实例线程标记为守护线程/用户线程, 如果{@code true}, 则将此线程标记为守护线程
    public final void setDaemon(boolean on) {
        checkAccess();
        if (isAlive()) {
            throw new IllegalThreadStateException();
        }
        daemon = on;
    }

    // 测试此线程是否为守护线程。
    /**
     * Tests if this thread is a daemon thread.
     *
     * // 如果此线程是守护线程，则为 true； 否则为假。
     * @return  <code>true</code> if this thread is a daemon thread;
     *          <code>false</code> otherwise.
     * @see     #setDaemon(boolean)
     */
    // 如果实例线程为守护线程，则为true; 如果实例线程为用户线程, 则为false
    public final boolean isDaemon() {
        return daemon;
    }

    /**
     * 20210728
     * A. 确定当前运行的线程是否有权修改此线程。
     * B. 如果存在安全管理器，则使用该线程作为参数调用其checkAccess方法。 这可能会导致抛出 SecurityException。
     */
    /**
     * A.
     * Determines if the currently running thread has permission to
     * modify this thread.
     *
     * B.
     * <p>
     * If there is a security manager, its <code>checkAccess</code> method
     * is called with this thread as its argument. This may result in
     * throwing a <code>SecurityException</code>.
     *
     * @exception  SecurityException  if the current thread is not allowed to
     *               access this thread.
     * @see        SecurityManager#checkAccess(Thread)
     */
    // 检查当前线程是否有权修改此线程
    public final void checkAccess() {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkAccess(this);
        }
    }

    // 返回此线程的字符串表示形式，包括线程的名称、优先级和线程组。
    /**
     * Returns a string representation of this thread, including the
     * thread's name, priority, and thread group.
     *
     * @return  a string representation of this thread.
     */
    // 返回此线程的字符串表示形式，包括线程的名称、优先级和线程组
    public String toString() {
        ThreadGroup group = getThreadGroup();
        if (group != null) {
            return "Thread[" + getName() + "," + getPriority() + "," +
                           group.getName() + "]";
        } else {
            return "Thread[" + getName() + "," + getPriority() + "," +
                            "" + "]";
        }
    }

    /**
     * 20210728
     * A. 返回此线程的上下文类加载器。 上下文ClassLoader由线程的创建者提供，供在该线程中运行的代码在加载类和资源时使用。
     *    如果不是{@linkplain #setContextClassLoader set}，则默认为父线程的 ClassLoader 上下文。 原始线程的上下文类加载器通常设置为用于加载应用程序的类加载器。
     * B. 如果存在安全管理器，并且调用者的类加载器不是{@code null}并且与上下文类加载器不同或不是其祖先，
     *    则此方法调用安全管理器的{@link SecurityManager#checkPermission(java.security.Permission) checkPermission}方法和
     *    {@link RuntimePermission RuntimePermission}{@code ("getClassLoader")} 权限来验证是否允许检索上下文类加载器。
     */
    /**
     * A.
     * Returns the context ClassLoader for this Thread. The context
     * ClassLoader is provided by the creator of the thread for use
     * by code running in this thread when loading classes and resources.
     * If not {@linkplain #setContextClassLoader set}, the default is the
     * ClassLoader context of the parent Thread. The context ClassLoader of the
     * primordial thread is typically set to the class loader used to load the
     * application.
     *
     * B.
     * <p>If a security manager is present, and the invoker's class loader is not
     * {@code null} and is not the same as or an ancestor of the context class
     * loader, then this method invokes the security manager's {@link
     * SecurityManager#checkPermission(java.security.Permission) checkPermission}
     * method with a {@link RuntimePermission RuntimePermission}{@code
     * ("getClassLoader")} permission to verify that retrieval of the context
     * class loader is permitted.
     *
     * // 此线程的上下文类加载器，或 {@code null} 指示系统类加载器（或者，如果失败，引导类加载器）
     * @return  the context ClassLoader for this Thread, or {@code null}
     *          indicating the system class loader (or, failing that, the
     *          bootstrap class loader)
     *
     * @throws  SecurityException
     *          if the current thread cannot get the context ClassLoader
     *
     * @since 1.2
     */
    // 获取实例线程的上下文类加载器, 以供实例线程中运行的代码在加载类和资源时使用
    @CallerSensitive
    public ClassLoader getContextClassLoader() {
        if (contextClassLoader == null)
            return null;
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            ClassLoader.checkClassLoaderPermission(contextClassLoader,
                                                   Reflection.getCallerClass());
        }
        return contextClassLoader;
    }

    /**
     * 20210728
     * A. 设置此线程的上下文类加载器。上下文ClassLoader可以在线程创建时设置，并允许线程的创建者提供合适的类加载器，通过{@code getContextClassLoader}，
     *    在加载类和资源时在线程中运行的代码。
     * B. 如果存在安全管理器，则使用 {@link RuntimePermission RuntimePermission}{@code ("setContextClassLoader")} 权限调用其
     *    {@link SecurityManager#checkPermission(java.security.Permission) checkPermission}方法以查看是否设置上下文允许类加载器。
     */
    /**
     * A.
     * Sets the context ClassLoader for this Thread. The context
     * ClassLoader can be set when a thread is created, and allows
     * the creator of the thread to provide the appropriate class loader,
     * through {@code getContextClassLoader}, to code running in the thread
     * when loading classes and resources.
     *
     * B.
     * <p>If a security manager is present, its {@link
     * SecurityManager#checkPermission(java.security.Permission) checkPermission}
     * method is invoked with a {@link RuntimePermission RuntimePermission}{@code
     * ("setContextClassLoader")} permission to see if setting the context
     * ClassLoader is permitted.
     *
     * // 此线程的上下文类加载器，或 null 指示系统类加载器（或者，如果失败，引导类加载器）
     * @param  cl
     *         the context ClassLoader for this Thread, or null  indicating the
     *         system class loader (or, failing that, the bootstrap class loader)
     *
     * @throws  SecurityException
     *          if the current thread cannot set the context ClassLoader
     *
     * @since 1.2
     */
    // 设置实例线程的上下文类加载器, 以供实例线程中运行的代码在加载类和资源时使用
    public void setContextClassLoader(ClassLoader cl) {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new RuntimePermission("setContextClassLoader"));
        }
        contextClassLoader = cl;
    }

    /**
     * 20210728
     * A. 当且仅当当前线程持有指定对象的监视器锁时，才返回true。
     * B. 此方法旨在允许程序断言当前线程已持有指定的锁：
     *      assert Thread.holdsLock(obj);
     */
    /**
     * A.
     * Returns <tt>true</tt> if and only if the current thread holds the
     * monitor lock on the specified object.
     *
     * B.
     * <p>This method is designed to allow a program to assert that
     * the current thread already holds a specified lock:
     * <pre>
     *     assert Thread.holdsLock(obj);
     * </pre>
     *
     * @param  obj the object on which to test lock ownership
     * @throws NullPointerException if obj is <tt>null</tt>
     *
     * // 如果当前线程持有指定对象上的监视器锁，则为 true。
     * @return <tt>true</tt> if the current thread holds the monitor lock on
     *         the specified object.
     * @since 1.4
     */
    // 判断当前线程是否持有obj对象锁
    public static native boolean holdsLock(Object obj);

    private static final StackTraceElement[] EMPTY_STACK_TRACE = new StackTraceElement[0];

    /**
     * 20210728
     * A. 返回表示此线程的堆栈转储的堆栈跟踪元素数组。如果此线程尚未启动、已启动但尚未被系统安排运行或已终止，则此方法将返回一个零长度数组。
     *    如果返回的数组长度不为零，则数组的第一个元素表示堆栈的顶部，这是序列中最近的方法调用。数组的最后一个元素代表堆栈的底部，它是序列中最近的方法调用。
     * B. 如果存在安全管理器，并且该线程不是当前线程，则使用 RuntimePermission("getStackTrace") 权限调用安全管理器的 checkPermission 方法，以查看是否可以获取堆栈跟踪。
     * C. 在某些情况下，某些虚拟机可能会从堆栈跟踪中省略一个或多个堆栈帧。 在极端情况下，允许没有与此线程相关的堆栈跟踪信息的虚拟机从此方法返回零长度数组。
     */
    /**
     * A.
     * Returns an array of stack trace elements representing the stack dump
     * of this thread.  This method will return a zero-length array if
     * this thread has not started, has started but has not yet been
     * scheduled to run by the system, or has terminated.
     * If the returned array is of non-zero length then the first element of
     * the array represents the top of the stack, which is the most recent
     * method invocation in the sequence.  The last element of the array
     * represents the bottom of the stack, which is the least recent method
     * invocation in the sequence.
     *
     * B.
     * <p>If there is a security manager, and this thread is not
     * the current thread, then the security manager's
     * <tt>checkPermission</tt> method is called with a
     * <tt>RuntimePermission("getStackTrace")</tt> permission
     * to see if it's ok to get the stack trace.
     *
     * C.
     * <p>Some virtual machines may, under some circumstances, omit one
     * or more stack frames from the stack trace.  In the extreme case,
     * a virtual machine that has no stack trace information concerning
     * this thread is permitted to return a zero-length array from this
     * method.
     *
     * // 一个StackTraceElement数组，每个代表一个堆栈帧。
     * @return an array of <tt>StackTraceElement</tt>,
     * each represents one stack frame.
     *
     * @throws SecurityException
     *        if a security manager exists and its
     *        <tt>checkPermission</tt> method doesn't allow
     *        getting the stack trace of thread.
     * @see SecurityManager#checkPermission
     * @see RuntimePermission
     * @see Throwable#getStackTrace
     *
     * @since 1.5
     */
    // 返回表示实例线程的堆栈转储的堆栈跟踪元素数组, 如果实例线程尚未启动、已启动但尚未被系统安排运行或者已终止，则返回一个零长度的数组
    public StackTraceElement[] getStackTrace() {
        if (this != Thread.currentThread()) {
            // 检查getStackTrace权限
            // check for getStackTrace permission
            SecurityManager security = System.getSecurityManager();
            if (security != null) {
                security.checkPermission(SecurityConstants.GET_STACK_TRACE_PERMISSION);
            }

            // 优化，因此我们不会为尚未启动或已终止的线程调用vm
            // optimization so we do not call into the vm for threads that
            // have not yet started or have terminated
            if (!isAlive()) {
                return EMPTY_STACK_TRACE;
            }

            StackTraceElement[][] stackTraceArray = dumpThreads(new Thread[] {this});
            StackTraceElement[] stackTrace = stackTraceArray[0];

            // 在前一个isAlive调用期间处于活动状态的线程可能已经终止，因此没有堆栈跟踪。
            // a thread that was alive during the previous isAlive call may have
            // since terminated, therefore not having a stacktrace.
            if (stackTrace == null) {
                stackTrace = EMPTY_STACK_TRACE;
            }
            return stackTrace;
        } else {
            // 当前线程不需要JVM帮助
            // Don't need JVM help for current thread
            return (new Exception()).getStackTrace();
        }
    }

    /**
     * 20210728
     * A. 返回所有活动线程的堆栈跟踪图。映射键是线程，每个映射值是一个StackTraceElement数组，表示相应线程的堆栈转储。
     *    返回的堆栈跟踪采用为{@link #getStackTrace getStackTrace} 方法指定的格式。
     * B. 调用此方法时，线程可能正在执行。每个线程的堆栈轨迹仅代表一个快照，每个堆栈轨迹可能在不同的时间获得。 如果虚拟机没有关于线程的堆栈跟踪信息，则映射值中将返回一个长度为零的数组。
     * C. 如果有安全管理器，则使用 RuntimePermission("getStackTrace") 权限和 RuntimePermission("modifyThreadGroup") 权限调用安全管理器的 checkPermission 方法，
     *    以查看是否可以获取所有线程的堆栈跟踪。
     */
    /**
     * A.
     * Returns a map of stack traces for all live threads.
     * The map keys are threads and each map value is an array of
     * <tt>StackTraceElement</tt> that represents the stack dump
     * of the corresponding <tt>Thread</tt>.
     * The returned stack traces are in the format specified for
     * the {@link #getStackTrace getStackTrace} method.
     *
     * B.
     * <p>The threads may be executing while this method is called.
     * The stack trace of each thread only represents a snapshot and
     * each stack trace may be obtained at different time.  A zero-length
     * array will be returned in the map value if the virtual machine has
     * no stack trace information about a thread.
     *
     * C.
     * <p>If there is a security manager, then the security manager's
     * <tt>checkPermission</tt> method is called with a
     * <tt>RuntimePermission("getStackTrace")</tt> permission as well as
     * <tt>RuntimePermission("modifyThreadGroup")</tt> permission
     * to see if it is ok to get the stack trace of all threads.
     *
     * // 从 Thread 到 StackTraceElement 数组的 Map，该数组表示相应线程的堆栈跟踪。
     * @return a <tt>Map</tt> from <tt>Thread</tt> to an array of
     * <tt>StackTraceElement</tt> that represents the stack trace of
     * the corresponding thread.
     *
     * @throws SecurityException
     *        if a security manager exists and its
     *        <tt>checkPermission</tt> method doesn't allow
     *        getting the stack trace of thread.
     * @see #getStackTrace
     * @see SecurityManager#checkPermission
     * @see RuntimePermission
     * @see Throwable#getStackTrace
     *
     * @since 1.5
     */
    // 返回所有活动线程的堆栈跟踪图, Key是线程, Value是一个StackTraceElement数组, 表示相应线程的堆栈转储, 其格式采用{@link #getStackTrace getStackTrace}方法指定的格式。
    public static Map<Thread, StackTraceElement[]> getAllStackTraces() {
        // 检查 getStackTrace 权限
        // check for getStackTrace permission
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkPermission(
                SecurityConstants.GET_STACK_TRACE_PERMISSION);
            security.checkPermission(
                SecurityConstants.MODIFY_THREADGROUP_PERMISSION);
        }

        // 获取所有线程列表的快照
        // Get a snapshot of the list of all threads
        Thread[] threads = getThreads();
        StackTraceElement[][] traces = dumpThreads(threads);
        Map<Thread, StackTraceElement[]> m = new HashMap<>(threads.length);
        for (int i = 0; i < threads.length; i++) {
            StackTraceElement[] stackTrace = traces[i];
            if (stackTrace != null) {
                m.put(threads[i], stackTrace);
            }
            // else terminated so we don't put it in the map
            // else 终止所以我们不把它放在地图上
        }
        return m;
    }


    private static final RuntimePermission SUBCLASS_IMPLEMENTATION_PERMISSION = new RuntimePermission("enableContextClassLoaderOverride");

    // 子类安全审计结果的缓存, 如果它出现在未来版本中时替换为ConcurrentReferenceHashMap
    /** cache of subclass security audit results */
    /* Replace with ConcurrentReferenceHashMap when/if it appears in a future
     * release */
    private static class Caches {
        // 子类安全审计结果的缓存
        /** cache of subclass security audit results */
        static final ConcurrentMap<WeakClassKey,Boolean> subclassAudits = new ConcurrentHashMap<>();

        // WeakReferences 到已审计子类的队列
        /** queue for WeakReferences to audited subclasses */
        static final ReferenceQueue<Class<?>> subclassAuditsQueue = new ReferenceQueue<>();
    }

    // 验证可以在不违反安全约束的情况下构造此（可能是子类）实例：子类不得覆盖安全敏感的非最终方法，否则检查“enableContextClassLoaderOverride”RuntimePermission。
    /**
     * Verifies that this (possibly subclass) instance can be constructed
     * without violating security constraints: the subclass must not override
     * security-sensitive non-final methods, or else the
     * "enableContextClassLoaderOverride" RuntimePermission is checked.
     */
    private static boolean isCCLOverridden(Class<?> cl) {
        if (cl == Thread.class)
            return false;

        processQueue(Caches.subclassAuditsQueue, Caches.subclassAudits);
        WeakClassKey key = new WeakClassKey(cl, Caches.subclassAuditsQueue);
        Boolean result = Caches.subclassAudits.get(key);
        if (result == null) {
            result = Boolean.valueOf(auditSubclass(cl));
            Caches.subclassAudits.putIfAbsent(key, result);
        }

        return result.booleanValue();
    }

    // 对给定的子类执行反射检查以验证它没有覆盖安全敏感的非最终方法。 如果子类覆盖任何方法，则返回 true，否则返回 false。
    /**
     * Performs reflective checks on given subclass to verify that it doesn't
     * override security-sensitive non-final methods.  Returns true if the
     * subclass overrides any of the methods, false otherwise.
     */
    private static boolean auditSubclass(final Class<?> subcl) {
        Boolean result = AccessController.doPrivileged(
            new PrivilegedAction<Boolean>() {
                public Boolean run() {
                    for (Class<?> cl = subcl;
                         cl != Thread.class;
                         cl = cl.getSuperclass())
                    {
                        try {
                            cl.getDeclaredMethod("getContextClassLoader", new Class<?>[0]);
                            return Boolean.TRUE;
                        } catch (NoSuchMethodException ex) {
                        }
                        try {
                            Class<?>[] params = {ClassLoader.class};
                            cl.getDeclaredMethod("setContextClassLoader", params);
                            return Boolean.TRUE;
                        } catch (NoSuchMethodException ex) {
                        }
                    }
                    return Boolean.FALSE;
                }
            }
        );
        return result.booleanValue();
    }

    private native static StackTraceElement[][] dumpThreads(Thread[] threads);
    private native static Thread[] getThreads();

    // 返回此线程的标识符。线程ID是在创建该线程时生成的一个长正数。线程ID是唯一的，并且在其生命周期内保持不变。当一个线程终止时，这个线程ID可能会被重用。
    /**
     * Returns the identifier of this Thread.  The thread ID is a positive
     * <tt>long</tt> number generated when this thread was created.
     * The thread ID is unique and remains unchanged during its lifetime.
     * When a thread is terminated, this thread ID may be reused.
     *
     * @return this thread's ID.
     * @since 1.5
     */
    // 返回实例线程的标识符, 线程ID是唯一的, 并且在其生命周期内保持不变, 而当一个线程终止时, 该线程ID可能会被重用
    public long getId() {
        return tid;
    }

    /**
     * 20210728
     * A. 线程状态。 线程可以处于以下状态之一：
     *      a. {@link #NEW} 尚未启动的线程处于此状态。
     *      b. {@link #RUNNABLE} 在Java虚拟机中执行的线程处于此状态。
     *      c. {@link #BLOCKED} 一个被阻塞等待监视器锁的线程处于这种状态。
     *      d. {@link #WAITING} 无限期等待另一个线程执行特定操作的线程处于此状态。
     *      e. {@link #TIMED_WAITING} 等待另一个线程执行操作的线程在指定的等待时间内处于此状态。
     *      f. {@link #TERMINATED} 已退出的线程处于此状态。
     * B. 一个线程在给定的时间点只能处于一种状态。这些状态是不反映任何操作系统线程状态的虚拟机状态。
     */
    /**
     * A.
     * A thread state.  A thread can be in one of the following states:
     * <ul>
     * <li>{@link #NEW}<br>
     *     A thread that has not yet started is in this state.
     *     </li>
     * <li>{@link #RUNNABLE}<br>
     *     A thread executing in the Java virtual machine is in this state.
     *     </li>
     * <li>{@link #BLOCKED}<br>
     *     A thread that is blocked waiting for a monitor lock
     *     is in this state.
     *     </li>
     * <li>{@link #WAITING}<br>
     *     A thread that is waiting indefinitely for another thread to
     *     perform a particular action is in this state.
     *     </li>
     * <li>{@link #TIMED_WAITING}<br>
     *     A thread that is waiting for another thread to perform an action
     *     for up to a specified waiting time is in this state.
     *     </li>
     * <li>{@link #TERMINATED}<br>
     *     A thread that has exited is in this state.
     *     </li>
     * </ul>
     *
     * B.
     * <p>
     * A thread can be in only one state at a given point in time.
     * These states are virtual machine states which do not reflect
     * any operating system thread states.
     *
     * @since   1.5
     * @see #getState
     */
    // 线程状态, 一个线程在给定的时间点只能处于一种状态, 其中这些状态为虚拟机状态, 不代表任何操作系统的线程状态
    public enum State {

        // 尚未启动的线程的线程状态。
        /**
         * Thread state for a thread which has not yet started.
         */
        // 尚未启动的线程处于此状态
        NEW,

        // 可运行线程的线程状态。处于可运行状态的线程正在Java虚拟机中执行，但它可能正在等待来自操作系统的其他资源，例如处理器。
        /**
         * Thread state for a runnable thread.  A thread in the runnable
         * state is executing in the Java virtual machine but it may
         * be waiting for other resources from the operating system
         * such as processor.
         */
        // 在Java虚拟机中执行的线程处于此状态
        RUNNABLE,

        // 线程阻塞等待监视器锁的线程状态。处于阻塞状态的线程, 指的是在调用{@link Object#wait() Object.wait}后正在等待监视器锁进入同步块/方法或重新进入同步块/方法。
        /**
         * Thread state for a thread blocked waiting for a monitor lock.
         * A thread in the blocked state is waiting for a monitor lock
         * to enter a synchronized block/method or
         * reenter a synchronized block/method after calling
         * {@link Object#wait() Object.wait}.
         */
        // 一个被阻塞等待监视器锁的线程处于这种状态
        BLOCKED,

        /**
         * 20210728
         * A. 等待线程的线程状态。由于调用以下方法之一，线程处于等待状态：
         *      a. {@link Object#wait() Object.wait} 没有超时
         *      b. {@link #join() Thread.join} 没有超时
         *      c. {@link LockSupport#park() LockSupport.park}
         * B. 处于等待状态的线程正在等待另一个线程执行特定操作。例如，在对象上调用Object.wait()的线程正在等待另一个线程在该对象上调用Object.notify()
         *    或Object.notifyAll()。调用Thread.join()的线程正在等待指定的线程终止。
         */
        /**
         * A.
         * Thread state for a waiting thread.
         * A thread is in the waiting state due to calling one of the
         * following methods:
         * <ul>
         *   <li>{@link Object#wait() Object.wait} with no timeout</li>
         *   <li>{@link #join() Thread.join} with no timeout</li>
         *   <li>{@link LockSupport#park() LockSupport.park}</li>
         * </ul>
         *
         * B.
         * <p>A thread in the waiting state is waiting for another thread to
         * perform a particular action.
         *
         * For example, a thread that has called <tt>Object.wait()</tt>
         * on an object is waiting for another thread to call
         * <tt>Object.notify()</tt> or <tt>Object.notifyAll()</tt> on
         * that object. A thread that has called <tt>Thread.join()</tt>
         * is waiting for a specified thread to terminate.
         */
        // 无限期等待另一个线程执行特定操作的线程处于此状态
        WAITING,

        /**
         * 20210728
         * 具有指定等待时间的等待线程的线程状态。由于使用指定的正等待时间调用以下方法之一，线程处于定时等待状态：
         *      a. {@link #sleep Thread.sleep}
         *      b. {@link Object#wait(long) Object.wait} 超时
         *      c. {@link #join(long) Thread.join} 超时
         *      d. {@link LockSupport#parkNanos LockSupport.parkNanos}
         *      c. {@link LockSupport#parkUntil LockSupport.parkUntil}
         */
        /**
         * Thread state for a waiting thread with a specified waiting time.
         * A thread is in the timed waiting state due to calling one of
         * the following methods with a specified positive waiting time:
         * <ul>
         *   <li>{@link #sleep Thread.sleep}</li>
         *   <li>{@link Object#wait(long) Object.wait} with timeout</li>
         *   <li>{@link #join(long) Thread.join} with timeout</li>
         *   <li>{@link LockSupport#parkNanos LockSupport.parkNanos}</li>
         *   <li>{@link LockSupport#parkUntil LockSupport.parkUntil}</li>
         * </ul>
         */
        // 等待另一个线程执行操作的线程在指定的等待时间内处于此状态
        TIMED_WAITING,

        // 终止线程的线程状态。线程已完成执行。
        /**
         * Thread state for a terminated thread.
         * The thread has completed execution.
         */
        // 已退出的线程处于此状态
        TERMINATED;
    }

    // 返回此线程的状态。此方法设计用于监视系统状态，而不是用于同步控制。
    /**
     * Returns the state of this thread.
     * This method is designed for use in monitoring of the system state,
     * not for synchronization control.
     *
     * @return this thread's state.
     * @since 1.5
     */
    // 返回实例线程的状态, 用于监视系统状态, 而不是用于同步控制
    public State getState() {
        // 获取当前线程状态
        // get current thread state
        return sun.misc.VM.toThreadState(threadStatus);
    }

    // Added in JSR-166

    /**
     * 20210728
     * A. 当线程由于未捕获的异常突然终止时调用的处理程序接口。
     * B. 当线程由于未捕获的异常而即将终止时，Java虚拟机将使用{@link #getUncaughtExceptionHandler}查询线程的UncaughtExceptionHandler，
     *    并将调用处理程序的uncaughtException方法，将线程和异常作为参数传递。如果一个线程没有明确设置它的UncaughtExceptionHandler，
     *    那么它的ThreadGroup对象作为它的UncaughtExceptionHandler。如果ThreadGroup对象对异常处理没有特殊要求，
     *    可以将调用转发给{@linkplain #getDefaultUncaughtExceptionHandler default uncaught exception handler}。
     */
    /**
     * A.
     * Interface for handlers invoked when a <tt>Thread</tt> abruptly
     * terminates due to an uncaught exception.
     *
     * B.
     * <p>When a thread is about to terminate due to an uncaught exception
     * the Java Virtual Machine will query the thread for its
     * <tt>UncaughtExceptionHandler</tt> using
     * {@link #getUncaughtExceptionHandler} and will invoke the handler's
     * <tt>uncaughtException</tt> method, passing the thread and the
     * exception as arguments.
     * If a thread has not had its <tt>UncaughtExceptionHandler</tt>
     * explicitly set, then its <tt>ThreadGroup</tt> object acts as its
     * <tt>UncaughtExceptionHandler</tt>. If the <tt>ThreadGroup</tt> object
     * has no
     * special requirements for dealing with the exception, it can forward
     * the invocation to the {@linkplain #getDefaultUncaughtExceptionHandler
     * default uncaught exception handler}.
     *
     * @see #setDefaultUncaughtExceptionHandler
     * @see #setUncaughtExceptionHandler
     * @see ThreadGroup#uncaughtException
     * @since 1.5
     */
    @FunctionalInterface
    public interface UncaughtExceptionHandler {

        /**
         * 20210728
         * A. 当给定线程由于给定的未捕获异常而终止时调用的方法。
         * B. Java 虚拟机将忽略此方法抛出的任何异常。
         */
        /**
         * A.
         * Method invoked when the given thread terminates due to the
         * given uncaught exception.
         *
         * B.
         * <p>Any exception thrown by this method will be ignored by the
         * Java Virtual Machine.
         *
         * @param t the thread
         * @param e the exception
         */
        // 线程由于未捕获的异常突然终止时, 会调用该处理程序接口
        void uncaughtException(Thread t, Throwable e);
    }

    // null 除非明确设置
    // null unless explicitly set
    private volatile UncaughtExceptionHandler uncaughtExceptionHandler;

    // null 除非明确设置
    // null unless explicitly set
    private static volatile UncaughtExceptionHandler defaultUncaughtExceptionHandler;

    /**
     * 20210728
     * A. 设置当线程由于未捕获的异常而突然终止时调用的默认处理程序，并且没有为该线程定义其他处理程序。
     * B. 未捕获的异常处理首先由线程控制，然后由线程的{@link ThreadGroup}对象控制，最后由默认的未捕获异常处理程序控制。
     *    如果线程没有设置显式的未捕获异常处理程序，并且线程的线程组（包括父线程组）没有专门化其uncaughtException方法，
     *    则将调用默认处理程序的uncaughtException方法。
     * C. 通过设置默认的未捕获异常处理程序，应用程序可以为那些已经接受系统提供的任何“默认”行为的线程更改处理未捕获异常的方式（例如记录到特定设备或文件）。
     * D. 请注意，默认的未捕获异常处理程序通常不应遵循线程的ThreadGroup对象，因为这可能会导致无限递归。
     */
    /**
     * A.
     * Set the default handler invoked when a thread abruptly terminates
     * due to an uncaught exception, and no other handler has been defined
     * for that thread.
     *
     * B.
     * <p>Uncaught exception handling is controlled first by the thread, then
     * by the thread's {@link ThreadGroup} object and finally by the default
     * uncaught exception handler. If the thread does not have an explicit
     * uncaught exception handler set, and the thread's thread group
     * (including parent thread groups)  does not specialize its
     * <tt>uncaughtException</tt> method, then the default handler's
     * <tt>uncaughtException</tt> method will be invoked.
     *
     * C.
     * <p>By setting the default uncaught exception handler, an application
     * can change the way in which uncaught exceptions are handled (such as
     * logging to a specific device, or file) for those threads that would
     * already accept whatever &quot;default&quot; behavior the system
     * provided.
     *
     * D.
     * <p>Note that the default uncaught exception handler should not usually
     * defer to the thread's <tt>ThreadGroup</tt> object, as that could cause
     * infinite recursion.
     *
     * @param eh the object to use as the default uncaught exception handler.
     * If <tt>null</tt> then there is no default handler.
     *
     * @throws SecurityException if a security manager is present and it
     *         denies <tt>{@link RuntimePermission}
     *         (&quot;setDefaultUncaughtExceptionHandler&quot;)</tt>
     *
     * @see #setUncaughtExceptionHandler
     * @see #getUncaughtExceptionHandler
     * @see ThreadGroup#uncaughtException
     * @since 1.5
     */
    // 设置由于当前线程未捕获的异常而突然终止时调用的默认处理程序
    public static void setDefaultUncaughtExceptionHandler(UncaughtExceptionHandler eh) {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(
                new RuntimePermission("setDefaultUncaughtExceptionHandler")
                    );
        }

         defaultUncaughtExceptionHandler = eh;
     }

     // 返回当线程由于未捕获的异常突然终止时调用的默认处理程序。 如果返回值为空，则没有默认值。
    /**
     * Returns the default handler invoked when a thread abruptly terminates
     * due to an uncaught exception. If the returned value is <tt>null</tt>,
     * there is no default.
     *
     * @since 1.5
     * @see #setDefaultUncaughtExceptionHandler
     * @return the default uncaught exception handler for all threads
     */
    // 返回当前线程由于未捕获的异常突然终止时调用的默认处理程序, 如果返回值为null, 代表没有默认处理程序
    public static UncaughtExceptionHandler getDefaultUncaughtExceptionHandler(){
        return defaultUncaughtExceptionHandler;
    }

    /**
     * 20210728
     * 返回此线程由于未捕获的异常而突然终止时调用的处理程序。如果该线程没有显式设置未捕获的异常处理程序，则返回该线程的ThreadGroup对象，
     * 除非该线程已终止，在这种情况下返回null。
     */
    /**
     * Returns the handler invoked when this thread abruptly terminates
     * due to an uncaught exception. If this thread has not had an
     * uncaught exception handler explicitly set then this thread's
     * <tt>ThreadGroup</tt> object is returned, unless this thread
     * has terminated, in which case <tt>null</tt> is returned.
     *
     * @since 1.5
     * @return the uncaught exception handler for this thread
     */
    // 返回实例线程由于未捕获的异常而突然终止时调用的处理程序, 如果没有显示设置则返回该线程的ThreadGroup对象
    public UncaughtExceptionHandler getUncaughtExceptionHandler() {
        return uncaughtExceptionHandler != null ? uncaughtExceptionHandler : group;
    }

    /**
     * 20210728
     * A. 设置该线程由于未捕获的异常而突然终止时调用的处理程序。
     * B. 线程可以通过显式设置其未捕获的异常处理程序来完全控制它如何响应未捕获的异常。如果未设置此类处理程序，则线程的ThreadGroup对象将充当其处理程序。
     */
    /**
     * A.
     * Set the handler invoked when this thread abruptly terminates
     * due to an uncaught exception.
     *
     * B.
     * <p>A thread can take full control of how it responds to uncaught
     * exceptions by having its uncaught exception handler explicitly set.
     * If no such handler is set then the thread's <tt>ThreadGroup</tt>
     * object acts as its handler.
     *
     * @param eh the object to use as this thread's uncaught exception
     * handler. If <tt>null</tt> then this thread has no explicit handler.
     * @throws  SecurityException  if the current thread is not allowed to
     *          modify this thread.
     * @see #setDefaultUncaughtExceptionHandler
     * @see ThreadGroup#uncaughtException
     * @since 1.5
     */
    // 设置实例线程由于未捕获的异常而突然终止时调用的处理程序
    public void setUncaughtExceptionHandler(UncaughtExceptionHandler eh) {
        checkAccess();
        uncaughtExceptionHandler = eh;
    }

    /**
     * Dispatch an uncaught exception to the handler. This method is
     * intended to be called only by the JVM.
     */
    // 向处理程序发送未捕获的异常, 此方法旨在仅由JVM调用
    private void dispatchUncaughtException(Throwable e) {
        getUncaughtExceptionHandler().uncaughtException(this, e);
    }

    /**
     * Removes from the specified map any keys that have been enqueued
     * on the specified reference queue.
     */
    // 从指定的映射中, 删除所有已在指定的引用队列中排队的键
    static void processQueue(ReferenceQueue<Class<?>> queue, ConcurrentMap<? extends WeakReference<Class<?>>, ?> map) {
        Reference<? extends Class<?>> ref;
        while((ref = queue.poll()) != null) {
            map.remove(ref);
        }
    }

    /**
     *  Weak key for Class objects.
     **/
    // Class对象的弱键
    static class WeakClassKey extends WeakReference<Class<?>> {
        /**
         * saved value of the referent's identity hash code, to maintain
         * a consistent hash code after the referent has been cleared
         */
        // 保存的引用对象身份哈希码的值, 以在引用对象被清除后保持一致的哈希码
        private final int hash;

        /**
         * Create a new WeakClassKey to the given object, registered
         * with a queue.
         */
        // 为给定的对象创建一个新的WeakClassKey, 并在队列中注册
        WeakClassKey(Class<?> cl, ReferenceQueue<Class<?>> refQueue) {
            super(cl, refQueue);
            hash = System.identityHashCode(cl);
        }

        /**
         * Returns the identity hash code of the original referent.
         */
        // 返回原始所指对象的身份哈希码。
        @Override
        public int hashCode() {
            return hash;
        }

        /**
         * 20210728
         * 如果给定对象是这个相同的WeakClassKey实例, 或者如果该对象的所指对象尚未清除, 如果给定对象是另一个WeakClassKey实例, 该实例具有与此相同的非空引用对象, 则返回true
         */
        /**
         * Returns true if the given object is this identical
         * WeakClassKey instance, or, if this object's referent has not
         * been cleared, if the given object is another WeakClassKey
         * instance with the identical non-null referent as this one.
         */
        @Override
        public boolean equals(Object obj) {
            // 如果弱引用相等, 则为true
            if (obj == this)
                return true;

            // 如果弱引用后面的真实对象相等, 则为true
            if (obj instanceof WeakClassKey) {
                Object referent = get();
                return (referent != null) &&
                       (referent == ((WeakClassKey) obj).get());
            }
            // 如果弱引用不等, 且真实对象也不等, 则返回false
            else {
                return false;
            }
        }
    }

    /**
     * 20210728
     * 以下三个最初未初始化的字段由类java.util.concurrent.ThreadLocalRandom专门管理。这些字段用于在并发代码中构建高性能PRNG(伪随机数发生器)，我们不能冒意外错误共享的风险。
     * 因此，这些字段与@Contended隔离。
     */
    // The following three initially uninitialized fields are exclusively
    // managed by class java.util.concurrent.ThreadLocalRandom. These
    // fields are used to build the high-performance PRNGs in the
    // concurrent code, and we can not risk accidental false sharing.
    // Hence, the fields are isolated with @Contended.

    // ThreadLocalRandom 的当前种子
    /** The current seed for a ThreadLocalRandom */
    @sun.misc.Contended("tlr")
    long threadLocalRandomSeed;

    // 探测哈希值; 如果threadLocalRandomSeed初始化，则非零
    /** Probe hash value; nonzero if threadLocalRandomSeed initialized */
    @sun.misc.Contended("tlr")
    int threadLocalRandomProbe;

    // 与公共ThreadLocalRandom序列隔离的二级种子
    /** Secondary seed isolated from public ThreadLocalRandom sequence */
    @sun.misc.Contended("tlr")
    int threadLocalRandomSecondarySeed;

    // 一些私有native帮助方法
    /* Some private helper methods */
    private native void setPriority0(int newPriority);
    private native void stop0(Object o);
    private native void suspend0();
    private native void resume0();
    private native void interrupt0();
    private native void setNativeName(String name);
}
