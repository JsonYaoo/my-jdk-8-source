/*
 * Copyright (c) 1994, 2013, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package java.lang;

/**
 * 20210725
 * A. Runnable 接口应该由线程执行的任何类实现。该类必须定义一个名为run的无参数方法。
 * B. 此接口旨在为希望在活动时执行代码的对象提供通用协议。例如，Runnable是由类Thread 实现的。处于活动状态仅意味着线程已启动且尚未停止。
 * C. 此外，Runnable提供了使类处于活动状态而不是子类化Thread的方法。通过实例化Thread实例并将自身作为目标传入，实现Runnable的类可以在不继承Thread的情况下运行。
 *    在大多数情况下，如果您只打算覆盖run()方法而不打算覆盖其他Thread方法，则应该使用Runnable接口。这很重要，因为除非程序员打算修改或增强类的基本行为，否则类不应被子类化。
 */
/**
 * A.
 * The <code>Runnable</code> interface should be implemented by any
 * class whose instances are intended to be executed by a thread. The
 * class must define a method of no arguments called <code>run</code>.
 *
 * B.
 * <p>
 * This interface is designed to provide a common protocol for objects that
 * wish to execute code while they are active. For example,
 * <code>Runnable</code> is implemented by class <code>Thread</code>.
 * Being active simply means that a thread has been started and has not
 * yet been stopped.
 *
 * C.
 * <p>
 * In addition, <code>Runnable</code> provides the means for a class to be
 * active while not subclassing <code>Thread</code>. A class that implements
 * <code>Runnable</code> can run without subclassing <code>Thread</code>
 * by instantiating a <code>Thread</code> instance and passing itself in
 * as the target.  In most cases, the <code>Runnable</code> interface should
 * be used if you are only planning to override the <code>run()</code>
 * method and no other <code>Thread</code> methods.
 * This is important because classes should not be subclassed
 * unless the programmer intends on modifying or enhancing the fundamental
 * behavior of the class.
 *
 * @author  Arthur van Hoff
 * @see     java.lang.Thread
 * @see     java.util.concurrent.Callable
 * @since   JDK1.0
 */
@FunctionalInterface
public interface Runnable {

    /**
     * 20210725
     * A. 当使用实现接口Runnable的对象创建线程时，启动线程会导致在单独执行的线程中调用对象的run方法。
     * B. 方法run的一般约定是它可以采取任何行动。
     */
    /**
     * A.
     * When an object implementing interface <code>Runnable</code> is used
     * to create a thread, starting the thread causes the object's
     * <code>run</code> method to be called in that separately executing
     * thread.
     *
     * B.
     * <p>
     * The general contract of the method <code>run</code> is that it may
     * take any action whatsoever.
     *
     * @see     java.lang.Thread#run()
     */
    public abstract void run();
}
