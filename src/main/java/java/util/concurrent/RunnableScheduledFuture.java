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
 * 20210523
 * {@link Rundable}的{@link ScheduledFuture}。 成功执行{@code run}方法会导致{@code Future}完成并允许访问其结果。
 */
/**
 * A {@link ScheduledFuture} that is {@link Runnable}. Successful
 * execution of the {@code run} method causes completion of the
 * {@code Future} and allows access to its results.
 * @see FutureTask
 * @see Executor
 * @since 1.6
 * @author Doug Lea
 * @param <V> The result type returned by this Future's {@code get} method
 */
public interface RunnableScheduledFuture<V> extends RunnableFuture<V>, ScheduledFuture<V> {

    /**
     * 20210523
     * 如果此任务是周期性的，则返回{@code true}。 定期任务可以根据一些时间表重新运行。 非定期任务只能运行一次。
     */
    /**
     * Returns {@code true} if this task is periodic. A periodic task may
     * re-run according to some schedule. A non-periodic task can be
     * run only once.
     *
     * @return {@code true} if this task is periodic
     */
    // 判断任务是否为周期性的, 如果是则返回true, 否则返回false; 定期任务可以根据一些时间表重新运行, 而非定期任务只能运行一次
    boolean isPeriodic();
}
