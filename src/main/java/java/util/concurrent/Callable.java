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
 * 20210725
 * A. 返回结果并可能引发异常的任务。实现者定义了一个没有参数的单一方法，称为 {@code call}。
 * B. {@code Callable} 接口类似于{@link java.lang.Runnable}，因为两者都是为实例可能由另一个线程执行的类而设计的。
 *    但是，{@code Runnable} 不会返回结果，也不会抛出已检查的异常。
 * C. {@link Executors} 类包含将其他常见形式转换为 {@code Callable} 类的实用方法。
 */
/**
 * A.
 * A task that returns a result and may throw an exception.
 * Implementors define a single method with no arguments called
 * {@code call}.
 *
 * B.
 * <p>The {@code Callable} interface is similar to {@link
 * java.lang.Runnable}, in that both are designed for classes whose
 * instances are potentially executed by another thread.  A
 * {@code Runnable}, however, does not return a result and cannot
 * throw a checked exception.
 *
 * C.
 * <p>The {@link Executors} class contains utility methods to
 * convert from other common forms to {@code Callable} classes.
 *
 * @see Executor
 * @since 1.5
 * @author Doug Lea
 * @param <V> the result type of method {@code call}
 */
@FunctionalInterface
public interface Callable<V> {

    /**
     * 20210725
     * 计算结果，如果无法计算则抛出异常。
     */
    /**
     * Computes a result, or throws an exception if unable to do so.
     *
     * @return computed result 计算的结果
     * @throws Exception if unable to compute a result
     */
    // 计算结果，如果无法计算则抛出异常
    V call() throws Exception;
}
