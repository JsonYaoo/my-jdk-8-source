/*
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util.concurrent.atomic;
import java.util.function.IntUnaryOperator;
import java.util.function.IntBinaryOperator;
import sun.misc.Unsafe;

/**
 * 20210811
 * 可以原子更新的 {@code int} 值。 有关原子变量属性的描述，请参阅 {@link java.util.concurrent.atomic} 包规范。
 * {@code AtomicInteger} 用于原子递增计数器等应用程序中，不能用作 {@link java.lang.Integer} 的替代品。
 * 但是，此类确实扩展了 {@code Number} 以允许处理基于数字的类的工具和实用程序进行统一访问。
 */
/**
 * An {@code int} value that may be updated atomically.  See the
 * {@link java.util.concurrent.atomic} package specification for
 * description of the properties of atomic variables. An
 * {@code AtomicInteger} is used in applications such as atomically
 * incremented counters, and cannot be used as a replacement for an
 * {@link java.lang.Integer}. However, this class does extend
 * {@code Number} to allow uniform access by tools and utilities that
 * deal with numerically-based classes.
 *
 * @since 1.5
 * @author Doug Lea
*/
public class AtomicInteger extends Number implements java.io.Serializable {

    private static final long serialVersionUID = 6214790243416807050L;

    // 设置使用 Unsafe.compareAndSwapInt 进行更新
    // setup to use Unsafe.compareAndSwapInt for updates
    private static final Unsafe unsafe = Unsafe.getUnsafe();
    private static final long valueOffset;

    static {
        try {
            valueOffset = unsafe.objectFieldOffset
                (AtomicInteger.class.getDeclaredField("value"));
        } catch (Exception ex) { throw new Error(ex); }
    }

    private volatile int value;

    // 使用给定的初始值创建一个新的 AtomicInteger。
    /**
     * Creates a new AtomicInteger with the given initial value.
     *
     * @param initialValue the initial value
     */
    // 使用给定的初始值创建一个新的 AtomicInteger
    public AtomicInteger(int initialValue) {
        value = initialValue;
    }

    // 使用初始值 {@code 0} 创建一个新的 AtomicInteger。
    /**
     * Creates a new AtomicInteger with initial value {@code 0}.
     */
    // 使用初始值 {@code 0} 创建一个新的 AtomicInteger
    public AtomicInteger() {
    }

    // 获取当前值。
    /**
     * Gets the current value.
     *
     * @return the current value
     */
    // 获取当前值
    public final int get() {
        return value;
    }

    // 设置为给定值。
    /**
     * Sets to the given value.
     *
     * @param newValue the new value
     */
    // 设置为给定值
    public final void set(int newValue) {
        value = newValue;
    }

    // 最终设置为给定值。
    /**
     * Eventually sets to the given value.
     *
     * @param newValue the new value
     * @since 1.6
     */
    // 最终设置为给定值
    public final void lazySet(int newValue) {
        // 使用volatile存储语义将引用值存储到给定的Java变量中。否则等同于{@link #putObject(Object, long, Object)}, 该方法不保证存储对其他线程的立即可见性
        unsafe.putOrderedInt(this, valueOffset, newValue);
    }

    // 原子地设置为给定值并返回旧值。
    /**
     * Atomically sets to the given value and returns the old value.
     *
     * @param newValue the new value
     * @return the previous value
     */
    // 原子地设置为给定值并返回旧值
    public final int getAndSet(int newValue) {
        return unsafe.getAndSetInt(this, valueOffset, newValue);
    }

    // 如果当前值 {@code ==} 是预期值，则原子地将值设置为给定的更新值。
    /**
     * Atomically sets the value to the given updated value
     * if the current value {@code ==} the expected value.
     *
     * @param expect the expected value
     * @param update the new value
     *
     * // {@code true} 如果成功。 假返回表示实际值不等于预期值。
     * @return {@code true} if successful. False return indicates that
     * the actual value was not equal to the expected value.
     */
    // CAS设置为目标值
    public final boolean compareAndSet(int expect, int update) {
        return unsafe.compareAndSwapInt(this, valueOffset, expect, update);
    }

    /**
     * 20210811
     * A. 如果当前值 {@code ==} 是预期值，则原子地将值设置为给定的更新值。
     * B. 可能会错误地失败并且不提供排序保证，因此很少是 {@code compareAndSet} 的合适替代品。
     */
    /**
     * A.
     * Atomically sets the value to the given updated value
     * if the current value {@code ==} the expected value.
     *
     * B.
     * <p><a href="package-summary.html#weakCompareAndSet">May fail
     * spuriously and does not provide ordering guarantees</a>, so is
     * only rarely an appropriate alternative to {@code compareAndSet}.
     *
     * @param expect the expected value
     * @param update the new value
     * @return {@code true} if successful
     */
    // CAS设置为目标值, 可能会错误地失败并且不提供排序保证, 是 {@code compareAndSet} 的合适替代品
    public final boolean weakCompareAndSet(int expect, int update) {
        return unsafe.compareAndSwapInt(this, valueOffset, expect, update);
    }

    // 以原子方式将当前值递增 1。
    /**
     * Atomically increments by one the current value.
     *
     * @return the previous value
     */
    // CAS递增1, 并返回旧值
    public final int getAndIncrement() {
        return unsafe.getAndAddInt(this, valueOffset, 1);
    }

    // 以原子方式将当前值递减 1。
    /**
     * Atomically decrements by one the current value.
     *
     * @return the previous value
     */
    // CAS递减1, 并返回旧值
    public final int getAndDecrement() {
        return unsafe.getAndAddInt(this, valueOffset, -1);
    }

    // 以原子方式将给定值添加到当前值。
    /**
     * Atomically adds the given value to the current value.
     *
     * @param delta the value to add
     * @return the previous value
     */
    // CAS递增delta, 并返回旧值
    public final int getAndAdd(int delta) {
        return unsafe.getAndAddInt(this, valueOffset, delta);
    }

    // 以原子方式将当前值递增 1。
    /**
     * Atomically increments by one the current value.
     *
     * @return the updated value
     */
    // CAS递增1, 并返回新值
    public final int incrementAndGet() {
        return unsafe.getAndAddInt(this, valueOffset, 1) + 1;
    }

    // 以原子方式将当前值递减 1。
    /**
     * Atomically decrements by one the current value.
     *
     * @return the updated value
     */
    // CAS递减1, 并返回新值
    public final int decrementAndGet() {
        return unsafe.getAndAddInt(this, valueOffset, -1) - 1;
    }

    // 以原子方式将给定值添加到当前值。
    /**
     * Atomically adds the given value to the current value.
     *
     * @param delta the value to add
     * @return the updated value
     */
    // CAS递增delta, 并返回新值
    public final int addAndGet(int delta) {
        return unsafe.getAndAddInt(this, valueOffset, delta) + delta;
    }

    // 使用给定函数的应用结果原子地更新当前值，返回前一个值。 该函数应该是无副作用的，因为当尝试更新由于线程之间的争用而失败时，它可能会被重新应用。
    /**
     * Atomically updates the current value with the results of
     * applying the given function, returning the previous value. The
     * function should be side-effect-free, since it may be re-applied
     * when attempted updates fail due to contention among threads.
     *
     * // 无副作用的功能
     * @param updateFunction a side-effect-free function
     * @return the previous value
     * @since 1.8
     */
    // CAS递增updateFunction的结果, 并返回旧值
    public final int getAndUpdate(IntUnaryOperator updateFunction) {
        int prev, next;
        do {
            prev = get();
            next = updateFunction.applyAsInt(prev);
        } while (!compareAndSet(prev, next));
        return prev;
    }

    // 使用给定函数的应用结果原子地更新当前值，返回更新后的值。 该函数应该是无副作用的，因为当尝试更新由于线程之间的争用而失败时，它可能会被重新应用。
    /**
     * Atomically updates the current value with the results of
     * applying the given function, returning the updated value. The
     * function should be side-effect-free, since it may be re-applied
     * when attempted updates fail due to contention among threads.
     *
     * @param updateFunction a side-effect-free function
     * @return the updated value
     * @since 1.8
     */
    // CAS递增updateFunction的结果, 并返回新值
    public final int updateAndGet(IntUnaryOperator updateFunction) {
        int prev, next;
        do {
            prev = get();
            next = updateFunction.applyAsInt(prev);
        } while (!compareAndSet(prev, next));
        return next;
    }

    // 使用将给定函数应用于当前和给定值的结果原子地更新当前值，返回前一个值。
    // 该函数应该是无副作用的，因为当尝试更新由于线程之间的争用而失败时，它可能会被重新应用。 该函数以当前值作为第一个参数，给定的更新作为第二个参数。
    /**
     * Atomically updates the current value with the results of
     * applying the given function to the current and given values,
     * returning the previous value. The function should be
     * side-effect-free, since it may be re-applied when attempted
     * updates fail due to contention among threads.  The function
     * is applied with the current value as its first argument,
     * and the given update as the second argument.
     *
     * @param x the update value
     * @param accumulatorFunction a side-effect-free function of two arguments
     * @return the previous value
     * @since 1.8
     */
    // CAS递增accumulatorFunction的结果, 并返回旧值
    public final int getAndAccumulate(int x, IntBinaryOperator accumulatorFunction) {
        int prev, next;
        do {
            prev = get();
            next = accumulatorFunction.applyAsInt(prev, x);
        } while (!compareAndSet(prev, next));
        return prev;
    }

    // 将给定函数应用于当前和给定值的结果原子地更新当前值，返回更新后的值。
    // 该函数应该是无副作用的，因为当尝试更新由于线程之间的争用而失败时，它可能会被重新应用。 该函数以当前值作为第一个参数，给定的更新作为第二个参数。
    /**
     * Atomically updates the current value with the results of
     * applying the given function to the current and given values,
     * returning the updated value. The function should be
     * side-effect-free, since it may be re-applied when attempted
     * updates fail due to contention among threads.  The function
     * is applied with the current value as its first argument,
     * and the given update as the second argument.
     *
     * @param x the update value
     * @param accumulatorFunction a side-effect-free function of two arguments
     * @return the updated value
     * @since 1.8
     */
    // CAS递增accumulatorFunction的结果, 并返回新值
    public final int accumulateAndGet(int x, IntBinaryOperator accumulatorFunction) {
        int prev, next;
        do {
            prev = get();
            next = accumulatorFunction.applyAsInt(prev, x);
        } while (!compareAndSet(prev, next));
        return next;
    }

    /**
     * Returns the String representation of the current value.
     *
     * @return the String representation of the current value
     */
    public String toString() {
        return Integer.toString(get());
    }

    /**
     * Returns the value of this {@code AtomicInteger} as an {@code int}.
     */
    // 将此 {@code AtomicInteger} 的值作为 {@code int} 返回。
    public int intValue() {
        return get();
    }

    /**
     * Returns the value of this {@code AtomicInteger} as a {@code long}
     * after a widening primitive conversion.
     *
     * @jls 5.1.2 Widening Primitive Conversions
     */
    // 在扩展原始转换后，将此 {@code AtomicInteger} 的值作为 {@code long} 返回。
    public long longValue() {
        return (long)get();
    }

    /**
     * Returns the value of this {@code AtomicInteger} as a {@code float}
     * after a widening primitive conversion.
     * @jls 5.1.2 Widening Primitive Conversions
     */
    // 在扩展原始转换后，将此 {@code AtomicInteger} 的值作为 {@code float} 返回。
    public float floatValue() {
        return (float)get();
    }

    /**
     * Returns the value of this {@code AtomicInteger} as a {@code double}
     * after a widening primitive conversion.
     * @jls 5.1.2 Widening Primitive Conversions
     */
    // 在扩展原始转换后，将此 {@code AtomicInteger} 的值作为 {@code double} 返回。
    public double doubleValue() {
        return (double)get();
    }

}
