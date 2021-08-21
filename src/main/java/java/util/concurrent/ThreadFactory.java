/*
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util.concurrent;

/**
 * 20210814
 * A. 按需创建新线程的对象。 使用线程工厂消除了对 {@link Thread#Thread(Runnable) new Thread} 的调用的硬连线，使应用程序能够使用特殊的线程子类、优先级等。
 * B. 这个接口最简单的实现就是：
 * {@code
 * class SimpleThreadFactory implements ThreadFactory {
 *   public Thread newThread(Runnable r) {
 *     return new Thread(r);
 *   }
 * }}
 * C. {@link Executors#defaultThreadFactory} 方法提供了一个更有用的简单实现，它在返回之前将创建的线程上下文设置为已知值。
 */
/**
 * A.
 * An object that creates new threads on demand.  Using thread factories
 * removes hardwiring of calls to {@link Thread#Thread(Runnable) new Thread},
 * enabling applications to use special thread subclasses, priorities, etc.
 *
 * B.
 * <p>
 * The simplest implementation of this interface is just:
 *  <pre> {@code
 * class SimpleThreadFactory implements ThreadFactory {
 *   public Thread newThread(Runnable r) {
 *     return new Thread(r);
 *   }
 * }}</pre>
 *
 * C.
 * The {@link Executors#defaultThreadFactory} method provides a more
 * useful simple implementation, that sets the created thread context
 * to known values before returning it.
 *
 * @since 1.5
 * @author Doug Lea
 */
public interface ThreadFactory {

    // 构造一个新的 {@code Thread}。 实现还可以初始化优先级、名称、守护进程状态、{@code ThreadGroup} 等。
    /**
     * Constructs a new {@code Thread}.  Implementations may also initialize
     * priority, name, daemon status, {@code ThreadGroup}, etc.
     *
     * @param r a runnable to be executed by new thread instance
     *
     * // 构造线程，或者 {@code null} 如果请求创建线程被拒绝
     * @return constructed thread, or {@code null} if the request to
     *         create a thread is rejected
     */
    Thread newThread(Runnable r);
}
