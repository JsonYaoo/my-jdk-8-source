/*
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util.concurrent;
import java.util.*;

/**
 * 20210813
 * A. 提供 {@link ExecutorService} 执行方法的默认实现。该类使用 {@code newTaskFor} 返回的 {@link RunnableFuture} 实现了
 *    {@code submit}、{@code invokeAny} 和 {@code invokeAll} 方法，该类默认为本文档中提供的 {@link FutureTask} 类包裹。
 *    例如，{@code submit(Runnable)} 的实现创建了一个关联的 {@code RunnableFuture}，它被执行并返回。子类可以覆盖 {@code newTaskFor} 方法以返回
 *    {@code RunnableFuture} 实现而不是 {@code FutureTask}。
 * B. 扩展示例。 这是自定义 {@link ThreadPoolExecutor} 以使用 {@code CustomTask} 类而不是默认的 {@code FutureTask} 类的草图：
 * {@code
 * public class CustomThreadPoolExecutor extends ThreadPoolExecutor {
 *
 *   static class CustomTask implements RunnableFuture {...}
 *
 *   protected  RunnableFuture newTaskFor(Callable c) {
 *       return new CustomTask(c);
 *   }
 *   protected  RunnableFuture newTaskFor(Runnable r, V v) {
 *       return new CustomTask(r, v);
 *   }
 *   // ... add constructors, etc.
 * }}
 */

/**
 * A.
 * Provides default implementations of {@link ExecutorService}
 * execution methods. This class implements the {@code submit},
 * {@code invokeAny} and {@code invokeAll} methods using a
 * {@link RunnableFuture} returned by {@code newTaskFor}, which defaults
 * to the {@link FutureTask} class provided in this package.  For example,
 * the implementation of {@code submit(Runnable)} creates an
 * associated {@code RunnableFuture} that is executed and
 * returned. Subclasses may override the {@code newTaskFor} methods
 * to return {@code RunnableFuture} implementations other than
 * {@code FutureTask}.
 *
 * B.
 * <p><b>Extension example</b>. Here is a sketch of a class
 * that customizes {@link ThreadPoolExecutor} to use
 * a {@code CustomTask} class instead of the default {@code FutureTask}:
 *  <pre> {@code
 * public class CustomThreadPoolExecutor extends ThreadPoolExecutor {
 *
 *   static class CustomTask<V> implements RunnableFuture<V> {...}
 *
 *   protected <V> RunnableFuture<V> newTaskFor(Callable<V> c) {
 *       return new CustomTask<V>(c);
 *   }
 *   protected <V> RunnableFuture<V> newTaskFor(Runnable r, V v) {
 *       return new CustomTask<V>(r, v);
 *   }
 *   // ... add constructors, etc.
 * }}</pre>
 *
 * @since 1.5
 * @author Doug Lea
 */
public abstract class AbstractExecutorService implements ExecutorService {

    // 为给定的可运行和默认值返回 {@code RunnableFuture}。
    /**
     * Returns a {@code RunnableFuture} for the given runnable and default value.
     *
     * @param runnable the runnable task being wrapped
     * @param value the default value for the returned future
     * @param <T> the type of the given value
     * @return a {@code RunnableFuture} which, when run, will run the
     * underlying runnable and which, as a {@code Future}, will yield
     * the given value as its result and provide for cancellation of
     * the underlying task
     * @since 1.6
     */
    // 为给定的可运行任务和默认值返回{@code RunnableFuture}
    protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
        return new FutureTask<T>(runnable, value);
    }

    // 为给定的可调用任务返回 {@code RunnableFuture}。
    /**
     * Returns a {@code RunnableFuture} for the given callable task.
     *
     * @param callable the callable task being wrapped
     * @param <T> the type of the callable's result
     *
     * // 一个 {@code RunnableFuture}，它在运行时将调用底层的可调用对象，并且作为 {@code Future}，将产生可调用对象的结果作为其结果并提供底层任务的取消
     * @return a {@code RunnableFuture} which, when run, will call the
     * underlying callable and which, as a {@code Future}, will yield
     * the callable's result as its result and provide for
     * cancellation of the underlying task
     *
     * @since 1.6
     */
    // 为给定的可调用任务返回 {@code RunnableFuture}
    protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
        return new FutureTask<T>(callable);
    }

    /**
     * @throws RejectedExecutionException {@inheritDoc}
     * @throws NullPointerException       {@inheritDoc}
     */
    // 实现ExecutorService方法, 运行给定的任务并返回{@code Future}对象
    public Future<?> submit(Runnable task) {
        if (task == null) throw new NullPointerException();

        // 为给定的可运行任务和空的默认值返回{@code RunnableFuture}
        RunnableFuture<Void> ftask = newTaskFor(task, null);

        // Executor#execute方法, 交由子类实现
        execute(ftask);
        return ftask;
    }

    /**
     * @throws RejectedExecutionException {@inheritDoc}
     * @throws NullPointerException       {@inheritDoc}
     */
    // 实现ExecutorService方法, 运行给定的任务并返回设置了result默认值的{@code Future}对象
    public <T> Future<T> submit(Runnable task, T result) {
        if (task == null) throw new NullPointerException();

        // 为给定的可运行任务和默认值返回{@code RunnableFuture}
        RunnableFuture<T> ftask = newTaskFor(task, result);

        // Executor#execute方法, 交由子类实现
        execute(ftask);
        return ftask;
    }

    /**
     * @throws RejectedExecutionException {@inheritDoc}
     * @throws NullPointerException       {@inheritDoc}
     */
    // 实现ExecutorService方法, 运行给定的任务并返回{@code Future}对象
    public <T> Future<T> submit(Callable<T> task) {
        if (task == null) throw new NullPointerException();

        // 为给定的可调用任务返回 {@code RunnableFuture}
        RunnableFuture<T> ftask = newTaskFor(task);

        // Executor#execute方法, 交由子类实现
        execute(ftask);
        return ftask;
    }

    // invokeAny 的主要机制。
    /**
     * the main mechanics of invokeAny.
     */
    // invokeAny主要机制, 执行给定的任务，当全部完成或超时到期时，返回一个保存其状态和结果的 Futures 列表，以先发生者为准。如果在此操作进行时修改了给定的集合，则此方法的结果未定义。
    private <T> T doInvokeAny(Collection<? extends Callable<T>> tasks, boolean timed, long nanos) throws InterruptedException, ExecutionException, TimeoutException {
        if (tasks == null)
            throw new NullPointerException();
        int ntasks = tasks.size();
        if (ntasks == 0)
            throw new IllegalArgumentException();
        ArrayList<Future<T>> futures = new ArrayList<Future<T>>(ntasks);

        // 使用阻塞队列和ExecutorCompletionService包装Executor与Future结果
        ExecutorCompletionService<T> ecs = new ExecutorCompletionService<T>(this);

        // 为了提高效率，尤其是在并行度有限的执行器中，请在提交更多任务之前检查之前提交的任务是否已完成。 这种交错加上异常机制解释了主循环的混乱。
        // For efficiency, especially in executors with limited
        // parallelism, check to see if previously submitted tasks are
        // done before submitting more of them. This interleaving
        // plus the exception mechanics account for messiness of main
        // loop.

        try {
            // 记录异常，以便如果我们无法获得任何结果，我们可以抛出我们得到的最后一个异常。
            // Record exceptions so that if we fail to obtain any
            // result, we can throw the last exception we got.
            ExecutionException ee = null;
            final long deadline = timed ? System.nanoTime() + nanos : 0L;
            Iterator<? extends Callable<T>> it = tasks.iterator();

            // 确定开始一项任务； 其余的逐渐增加
            // Start one task for sure; the rest incrementally
            // 初始时, 提交任务到ExecutorCompletionService
            futures.add(ecs.submit(it.next()));
            --ntasks;
            int active = 1;

            // 开始自旋
            for (;;) {
                // 获取完成的Future结果, 如果没有则返回null
                Future<T> f = ecs.poll();

                // 如果f为null, 说明提交的任务还没执行完毕
                if (f == null) {
                    // 如果还有剩余的任务没有提交, 则继续提交任务到ExecutorCompletionService
                    if (ntasks > 0) {
                        --ntasks;
                        futures.add(ecs.submit(it.next()));
                        ++active;
                    }
                    // 如果没有剩余的任务需要提交, 但活跃任务为0, 说明状态不合法, 则退出自旋再抛出异常
                    else if (active == 0)
                        break;
                    // 如果需要定时, 则定时获取任务f, 如果f为null, 则抛出TimeoutException异常; 如果f不为null, 则更新定时时间
                    else if (timed) {
                        f = ecs.poll(nanos, TimeUnit.NANOSECONDS);
                        if (f == null)
                            throw new TimeoutException();
                        nanos = deadline - System.nanoTime();
                    }
                    // 如果不需要定时, 则从阻塞队列中阻塞获取执行结果f
                    else
                        f = ecs.take();
                }
                // 如果f不为null, 说明提交的任务已执行完毕
                if (f != null) {
                    // 执行完毕则活跃的任务数-1
                    --active;
                    try {
                        // 返回第一个获取得到的Future结果
                        return f.get();
                    } catch (ExecutionException eex) {
                        // 如果获取期间抛出异常, 则记录异常
                        ee = eex;
                    } catch (RuntimeException rex) {
                        // 如果获取期间抛出异常, 则记录异常
                        ee = new ExecutionException(rex);
                    }
                }
            }

            // 如果没有任何一个任务正常返回的话, 则抛出ExecutionException异常
            if (ee == null)
                ee = new ExecutionException();
            throw ee;
        } finally {
            // 返回或者抛出异常之前, 则尝试取消此异步计算任务的执行, 如果任务已完成、已被取消或由于其他原因无法取消, 则返回false; 如果此任务在正常完成之前被取消, 则返回{@code true}
            for (int i = 0, size = futures.size(); i < size; i++)
                futures.get(i).cancel(true);
        }
    }

    // 实现ExecutorService方法, 执行给定的任务，返回成功完成的任务的结果（即不抛出异常），如果有的话。在正常或异常返回时，未完成的任务将被取消。 如果在此操作进行时修改了给定的集合，则此方法的结果未定义。
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        try {
            return doInvokeAny(tasks, false, 0);
        } catch (TimeoutException cannotHappen) {
            assert false;
            return null;
        }
    }

    // 实现ExecutorService方法, 执行给定的任务，返回已成功完成的任务的结果（即，不抛出异常），如果在给定的超时时间之前执行任何操作。 在正常或异常返回时，未完成的任务将被取消。如果在此操作进行时修改了给定的集合，则此方法的结果未定义。
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return doInvokeAny(tasks, true, unit.toNanos(timeout));
    }

    // 实现ExecutorService方法, 执行给定的任务，返回一个 Futures 列表，在所有完成时保存它们的状态和结果。 {@link Future#isDone} 对于返回列表的每个元素都是 {@code true}。
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        if (tasks == null)
            throw new NullPointerException();
        ArrayList<Future<T>> futures = new ArrayList<Future<T>>(tasks.size());
        boolean done = false;
        try {
            // 遍历传入的tasks集合, 包装成RunnableFuture, 并交由子类去执行
            for (Callable<T> t : tasks) {
                RunnableFuture<T> f = newTaskFor(t);
                futures.add(f);
                execute(f);
            }

            // 遍历获取futures结果集
            for (int i = 0, size = futures.size(); i < size; i++) {
                Future<T> f = futures.get(i);

                // 如果有任务没执行完, 则阻塞获取
                if (!f.isDone()) {
                    try {
                        f.get();
                    } catch (CancellationException ignore) {
                    } catch (ExecutionException ignore) {
                    }
                }
            }

            // 遍历获取完毕, 则更新标记为done为true, 并返回结果集
            done = true;
            return futures;
        } finally {
            // 返回或者抛出异常之前, 如果任务还没执行完, 则遍历取消任务的执行
            if (!done)
                for (int i = 0, size = futures.size(); i < size; i++)
                    futures.get(i).cancel(true);
        }
    }

    // 实现ExecutorService方法, 执行给定的任务，当全部完成或超时到期时，返回一个保存其状态和结果的 Futures 列表，以先发生者为准。
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        if (tasks == null)
            throw new NullPointerException();
        long nanos = unit.toNanos(timeout);
        ArrayList<Future<T>> futures = new ArrayList<Future<T>>(tasks.size());
        boolean done = false;
        try {
            // 遍历传入的tasks集合, 包装成RunnableFuture
            for (Callable<T> t : tasks)
                futures.add(newTaskFor(t));

            // 获取超时时间
            final long deadline = System.nanoTime() + nanos;
            final int size = futures.size();

            // 交错时间检查和调用执行以防执行器没有任何/很多并行性。
            // Interleave time checks and calls to execute in case
            // executor doesn't have any/much parallelism.
            // 遍历futures交由子类去执行
            for (int i = 0; i < size; i++) {
                execute((Runnable)futures.get(i));
                nanos = deadline - System.nanoTime();

                // 如果超时, 则返回此刻的结果集
                if (nanos <= 0L)
                    return futures;
            }

            // 遍历获取任务执行结果
            for (int i = 0; i < size; i++) {
                Future<T> f = futures.get(i);

                // 如果任务没执行完毕, 但发生了超时, 则返回此刻的结果集
                if (!f.isDone()) {
                    if (nanos <= 0L)
                        return futures;
                    try {
                        // 定时获取f的执行结果, 如果抛出异常则执行返回结果集
                        f.get(nanos, TimeUnit.NANOSECONDS);
                    } catch (CancellationException ignore) {
                    } catch (ExecutionException ignore) {
                    } catch (TimeoutException toe) {
                        return futures;
                    }
                    nanos = deadline - System.nanoTime();
                }
            }
            done = true;
            return futures;
        } finally {
            // 返回或者抛出异常之前, 如果任务还没执行完, 则遍历取消任务的执行
            if (!done)
                for (int i = 0, size = futures.size(); i < size; i++)
                    futures.get(i).cancel(true);
        }
    }

}
