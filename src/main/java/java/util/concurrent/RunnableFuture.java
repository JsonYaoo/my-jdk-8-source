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
 * 20210726
 * 一个{@link Future}是{@link Runnable}。{@code run}方法的成功执行会导致{@code Future}的完成并允许访问其结果。
 */
/**
 * A {@link Future} that is {@link Runnable}. Successful execution of
 * the {@code run} method causes completion of the {@code Future}
 * and allows access to its results.
 *
 * @see FutureTask
 * @see Executor
 * @since 1.6
 * @author Doug Lea
 * @param <V> The result type returned by this Future's {@code get} method
 */
public interface RunnableFuture<V> extends Runnable, Future<V> {

    /**
     * 20210726
     * 将此 Future 设置为其计算结果，除非它已被取消。
     */
    /**
     * Sets this Future to the result of its computation
     * unless it has been cancelled.
     */
    // 将Future设置为其计算结果, 除非它已被取消
    void run();
}
