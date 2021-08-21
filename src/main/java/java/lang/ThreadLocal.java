/*
 * Copyright (c) 1997, 2013, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package java.lang;
import java.lang.ref.*;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * 20210816
 * A. 此类提供线程局部变量。 这些变量与它们的普通对应物不同，因为每个访问一个的线程（通过其 {@code get} 或 {@code set} 方法）都有自己的、独立初始化的变量副本。
 *    {@code ThreadLocal} 实例通常是希望将状态与线程相关联的类中的私有静态字段（例如，用户 ID 或事务 ID）。
 * B. 例如，下面的类生成每个线程本地的唯一标识符。 线程的 id 在它第一次调用 {@code ThreadId.get()} 时被分配，并且在后续调用中保持不变:
 * import java.util.concurrent.atomic.AtomicInteger;
 *
 * public class ThreadId {
 *     // Atomic integer containing the next thread ID to be assigned
 *     private static final AtomicInteger nextId = new AtomicInteger(0);
 *
 *     // Thread local variable containing each thread's ID
 *     private static final ThreadLocal<Integer> threadId =
 *         new ThreadLocal<Integer>() {
 *             @Override protected Integer initialValue() {
 *                 return nextId.getAndIncrement();
 *         }
 *     };
 *
 *     // Returns the current thread's unique ID, assigning it if necessary
 *     public static int get() {
 *         return threadId.get();
 *     }
 * }
 * C. 只要线程处于活动状态并且 {@code ThreadLocal} 实例可访问，每个线程都持有对其线程局部变量副本的隐式引用；
 *    线程消失后，它的所有线程本地实例副本都将进行垃圾回收（除非存在对这些副本的其他引用）。
 */
/**
 * A.
 * This class provides thread-local variables.  These variables differ from
 * their normal counterparts in that each thread that accesses one (via its
 * {@code get} or {@code set} method) has its own, independently initialized
 * copy of the variable.  {@code ThreadLocal} instances are typically private
 * static fields in classes that wish to associate state with a thread (e.g.,
 * a user ID or Transaction ID).
 *
 * B.
 * <p>For example, the class below generates unique identifiers local to each
 * thread.
 * A thread's id is assigned the first time it invokes {@code ThreadId.get()}
 * and remains unchanged on subsequent calls.
 * <pre>
 * import java.util.concurrent.atomic.AtomicInteger;
 *
 * public class ThreadId {
 *     // Atomic integer containing the next thread ID to be assigned
 *     private static final AtomicInteger nextId = new AtomicInteger(0);
 *
 *     // Thread local variable containing each thread's ID
 *     private static final ThreadLocal&lt;Integer&gt; threadId =
 *         new ThreadLocal&lt;Integer&gt;() {
 *             &#64;Override protected Integer initialValue() {
 *                 return nextId.getAndIncrement();
 *         }
 *     };
 *
 *     // Returns the current thread's unique ID, assigning it if necessary
 *     public static int get() {
 *         return threadId.get();
 *     }
 * }
 * </pre>
 *
 * C.
 * <p>Each thread holds an implicit reference to its copy of a thread-local
 * variable as long as the thread is alive and the {@code ThreadLocal}
 * instance is accessible; after a thread goes away, all of its copies of
 * thread-local instances are subject to garbage collection (unless other
 * references to these copies exist).
 *
 * @author  Josh Bloch and Doug Lea
 * @since   1.2
 */
public class ThreadLocal<T> {

    /**
     * 20210816
     * ThreadLocals 依赖于附加到每个线程（Thread.threadLocals 和inheritableThreadLocals）的每线程线性探针哈希映射。
     * ThreadLocal 对象充当键，通过 threadLocalHashCode 进行搜索。 这是一个自定义哈希代码（仅在 ThreadLocalMaps 中有用），
     * 它消除了在相同线程使用连续构造的 ThreadLocals 的常见情况下的冲突，同时在不太常见的情况下保持良好行为。
     */
    /**
     * ThreadLocals rely on per-thread linear-probe hash maps attached
     * to each thread (Thread.threadLocals and
     * inheritableThreadLocals).  The ThreadLocal objects act as keys,
     * searched via threadLocalHashCode.  This is a custom hash code
     * (useful only within ThreadLocalMaps) that eliminates collisions
     * in the common case where consecutively constructed ThreadLocals
     * are used by the same threads, while remaining well-behaved in
     * less common cases.
     */
    // 线程本地副本hashCode
    private final int threadLocalHashCode = nextHashCode();

    // 要给出的下一个哈希码。 原子更新。 从零开始。
    /**
     * The next hash code to be given out. Updated atomically. Starts at
     * zero.
     */
    // 要给出的下一个哈希码, 原子更新, 从零开始
    private static AtomicInteger nextHashCode = new AtomicInteger();

    // 连续生成的哈希码之间的差异 - 将隐式顺序线程本地 ID 转换为接近最优分布的乘法哈希值，用于 2 次方大小的表。
    /**
     * The difference between successively generated hash codes - turns
     * implicit sequential thread-local IDs into near-optimally spread
     * multiplicative hash values for power-of-two-sized tables.
     */
    // 连续生成的哈希码之间的差异 - 将隐式顺序线程本地 ID 转换为接近最优分布的乘法哈希值，用于 2 次方大小的表。
    private static final int HASH_INCREMENT = 0x61c88647;

    // 返回下一个哈希码。
    /**
     * Returns the next hash code.
     */
    // 返回下一个哈希码
    private static int nextHashCode() {
        return nextHashCode.getAndAdd(HASH_INCREMENT);
    }

    /**
     * 20210816
     * A. 返回此线程局部变量的当前线程的“初始值”。 该方法将在线程第一次使用 {@link #get} 方法访问变量时被调用，除非线程之前调用了 {@link #set} 方法，
     *    在这种情况下，{@code initialValue} 方法将不会被调用 为线程调用。 通常，每个线程最多调用此方法一次，
     *    但在后续调用 {@link #remove} 后跟 {@link #get} 的情况下可能会再次调用。
     * B. 此实现仅返回 {@code null}; 如果程序员希望线程局部变量具有 {@code null} 以外的初始值，则必须对 {@code ThreadLocal} 进行子类化，并覆盖此方法。 通常，将使用匿名内部类。
     */
    /**
     * A.
     * Returns the current thread's "initial value" for this
     * thread-local variable.  This method will be invoked the first
     * time a thread accesses the variable with the {@link #get}
     * method, unless the thread previously invoked the {@link #set}
     * method, in which case the {@code initialValue} method will not
     * be invoked for the thread.  Normally, this method is invoked at
     * most once per thread, but it may be invoked again in case of
     * subsequent invocations of {@link #remove} followed by {@link #get}.
     *
     * B.
     * <p>This implementation simply returns {@code null}; if the
     * programmer desires thread-local variables to have an initial
     * value other than {@code null}, {@code ThreadLocal} must be
     * subclassed, and this method overridden.  Typically, an
     * anonymous inner class will be used.
     *
     * // 此线程本地的初始值
     * @return the initial value for this thread-local
     */
    // 返回此线程局部变量的当前线程的“初始值”。
    // 该方法将在线程第一次使用 {@link #get} 方法访问变量时被调用, 除非线程之前调用了 {@link #set} 方法, 在这种情况下，{@code initialValue} 方法将不会被调用为线程调用。
    // 通常, 每个线程最多调用此方法一次, 但在后续调用 {@link #remove} 后跟 {@link #get} 的情况下可能会再次调用。
    // 此实现仅返回 {@code null}; 如果程序员希望线程局部变量具有 {@code null} 以外的初始值，则必须对 {@code ThreadLocal} 进行子类化，并覆盖此方法。
    protected T initialValue() {
        return null;
    }

    // 创建线程局部变量。 变量的初始值是通过调用 {@code Supplier} 上的 {@code get} 方法确定的。
    /**
     * Creates a thread local variable. The initial value of the variable is
     * determined by invoking the {@code get} method on the {@code Supplier}.
     *
     * // 线程本地值的类型
     * @param <S> the type of the thread local's value
     *
     * // 用于确定初始值的supplier
     * @param supplier the supplier to be used to determine the initial value
     *
     * // 一个新的线程局部变量
     * @return a new thread local variable
     * @throws NullPointerException if the specified supplier is null
     * @since 1.8
     */
    // 创建线程局部变量。 变量的初始值是通过调用 {@code Supplier} 上的 {@code get} 方法确定的。
    public static <S> ThreadLocal<S> withInitial(Supplier<? extends S> supplier) {
        return new SuppliedThreadLocal<>(supplier);
    }

    // 创建线程局部变量。
    /**
     * Creates a thread local variable.
     *
     * @see #withInitial(java.util.function.Supplier)
     */
    // 创建线程局部变量
    public ThreadLocal() {
    }

    // 返回此线程局部变量的当前线程副本中的值。 如果变量没有当前线程的值，它首先被初始化为调用 {@link #initialValue} 方法返回的值。
    /**
     * Returns the value in the current thread's copy of this
     * thread-local variable.  If the variable has no value for the
     * current thread, it is first initialized to the value returned
     * by an invocation of the {@link #initialValue} method.
     *
     * // 此线程本地的当前线程的值
     * @return the current thread's value of this thread-local
     */
    // 先根据当前ThreadLocal实例的散列码获取ThreadLocalMap中的条目, 如果ThreadLocalMap还没被初始化, 则惰性构造默认容量16、默认负载因子16 * 2/3、当前ThreadLocal实例作为第一个Entry#Key的ThreadLocalMap, 并赋予初始值
    public T get() {
        // 获取当前线程
        Thread t = Thread.currentThread();

        // 获取ThreadLocal.ThreadLocalMap threadLocals
        ThreadLocalMap map = getMap(t);

        // 如果ThreadLocal.ThreadLocalMap threadLocals已被初始化
        if (map != null) {
            // 则根据ThreadLocal中的散列码, 来获取ThreadLocalMap中的Entry, 如果根据散列码索引获取不到Entry, 则调用getEntryAfterMiss查找: 如果键匹配则返回e, 如果键为空则清空陈旧的条目然后返回null, 否则获取下一个索引i的Entry
            ThreadLocalMap.Entry e = map.getEntry(this);

            // 如果当前ThreadLocal实例不为null, 则返回其对应的value值
            if (e != null) {
                @SuppressWarnings("unchecked")
                T result = (T)e.value;
                return result;
            }
        }

        // 如果ThreadLocal.ThreadLocalMap threadLocals还没初始化, 则先获取初始值, 如果ThreadLocal.ThreadLocalMap threadLocals已被初始化, 则为其赋初值, 否则惰性构造默认容量16、默认负载因子16 * 2/3、当前ThreadLocal实例作为第一个Entry#Key的ThreadLocalMap
        return setInitialValue();
    }

    // 用于建立初始值的 set() 变体。 如果用户覆盖了 set() 方法，则代替 set() 使用。
    /**
     * Variant of set() to establish initialValue. Used instead
     * of set() in case user has overridden the set() method.
     *
     * @return the initial value
     */
    // 先获取初始值, 如果ThreadLocal.ThreadLocalMap threadLocals已被初始化, 则为其赋初值, 否则惰性构造默认容量16、默认负载因子16 * 2/3、当前ThreadLocal实例作为第一个Entry#Key的ThreadLocalMap
    private T setInitialValue() {
        // 返回此线程局部变量的当前线程的“初始值”。 该方法将在线程第一次使用 {@link #get} 方法访问变量时被调用，除非线程之前调用了 {@link #set} 方法
        T value = initialValue();

        // 获取当前线程
        Thread t = Thread.currentThread();

        // 获取ThreadLocal.ThreadLocalMap threadLocals
        ThreadLocalMap map = getMap(t);

        // 如果threadLocals已被初始化, 则为map赋予"初始值"
        if (map != null)
            map.set(this, value);
        // 如果threadLocals还没被初始化, 则惰性构造默认容量16、默认负载因子16 * 2/3、当前ThreadLocal实例作为第一个Entry#Key的ThreadLocalMap, 只有在至少有一个条目可以放入时才创建
        else
            createMap(t, value);

        // 然后返回"初始值"
        return value;
    }

    // 将此线程局部变量的当前线程副本设置为指定值。 大多数子类不需要重写这个方法，只依赖 {@link #initialValue} 方法来设置线程局部变量的值。
    /**
     * Sets the current thread's copy of this thread-local variable
     * to the specified value.  Most subclasses will have no need to
     * override this method, relying solely on the {@link #initialValue}
     * method to set the values of thread-locals.
     *
     * // value 要存储在此线程本地的当前线程副本中的值。
     * @param value the value to be stored in the current thread's copy of
     *        this thread-local.
     */
    // 将此线程局部变量的当前线程副本设置为指定值
    public void set(T value) {
        // 获取当前线程
        Thread t = Thread.currentThread();

        // 获取ThreadLocal.ThreadLocalMap threadLocals
        ThreadLocalMap map = getMap(t);

        // 如果threadLocals已被初始化, 则设置ThreadLocal-value条目, 如果ThreadLocal存在则替换旧值; 如果ThreadLocal存在空键, 则交换旧值到新桶并向后清空空键; 如果ThreadLocal不存在条目, 则新增Entry条目, 如果新增后实际大小超过了1/2则还需要清空所有弱键并扩容2倍散列表
        if (map != null)
            map.set(this, value);
        // 如果threadLocals还没被初始化, 则惰性构造默认容量16、默认负载因子16 * 2/3、当前ThreadLocal实例作为第一个Entry#Key的ThreadLocalMap, 只有在至少有一个条目可以放入时才创建
        else
            createMap(t, value);
    }

    /**
     * 20210816
     * 删除此线程局部变量的当前线程值。 如果这个线程局部变量随后被当前线程{@linkplain #get read}，它的值将通过调用它的{@link #initialValue} 方法重新初始化，
     * 除非它的值被当前线程{@linkplain #set set} 线程在此期间。 这可能会导致在当前线程中多次调用 {@code initialValue} 方法。
     *
     */
    /**
     * Removes the current thread's value for this thread-local
     * variable.  If this thread-local variable is subsequently
     * {@linkplain #get read} by the current thread, its value will be
     * reinitialized by invoking its {@link #initialValue} method,
     * unless its value is {@linkplain #set set} by the current thread
     * in the interim.  This may result in multiple invocations of the
     * {@code initialValue} method in the current thread.
     *
     * @since 1.5
     */
     // 如果threadLocals已经被初始化, 则删除ThreadLocal的条目, 如果找到e键为指定的ThreadLocal, 则清空其弱键条目, 否则什么也不做
     public void remove() {
         // 获取ThreadLocal.ThreadLocalMap threadLocals
         ThreadLocalMap m = getMap(Thread.currentThread());

         // 如果threadLocals已经被初始化, 则删除ThreadLocal的条目, 如果找到e键为指定的ThreadLocal, 则清空其弱键条目, 否则什么也不做
         if (m != null)
             m.remove(this);
     }

     // 获取与 ThreadLocal 关联的映射。 在 InheritableThreadLocal 中重写。
    /**
     * Get the map associated with a ThreadLocal. Overridden in
     * InheritableThreadLocal.
     *
     * @param  t the current thread
     * @return the map
     */
    // 获取ThreadLocal.ThreadLocalMap threadLocals
    ThreadLocalMap getMap(Thread t) {
        return t.threadLocals;
    }

    // 创建与 ThreadLocal 关联的映射。 在 InheritableThreadLocal 中重写。
    /**
     * Create the map associated with a ThreadLocal. Overridden in
     * InheritableThreadLocal.
     *
     * @param t the current thread
     * @param firstValue value for the initial entry of the map
     */
    // 惰性构造默认容量16、默认负载因子16 * 2/3、当前ThreadLocal实例作为第一个Entry#Key的ThreadLocalMap, 只有在至少有一个条目可以放入时才创建
    void createMap(Thread t, T firstValue) {
        t.threadLocals = new ThreadLocalMap(this, firstValue);
    }

    // 创建继承线程局部变量映射的工厂方法。 设计为仅从 Thread 构造函数调用。
    /**
     * Factory method to create map of inherited thread locals.
     * Designed to be called only from Thread constructor.
     *
     * @param  parentMap the map associated with parent thread
     *
     * // 包含父级可继承绑定的映射
     * @return a map containing the parent's inheritable bindings
     */
    // Thread#init调用, 创建继承线程局部变量映射的工厂方法
    static ThreadLocalMap createInheritedMap(ThreadLocalMap parentMap) {
        return new ThreadLocalMap(parentMap);
    }

    /**
     * 20210816
     * 方法childValue明显是在子类InheritableThreadLocal中定义的，但是在这里内部定义是为了提供createInheritedMap工厂方法，
     * 而不需要在InheritableThreadLocal中子类化map类。 这种技术优于在方法中嵌入 instanceof 测试的替代方法。
     */
    /**
     * Method childValue is visibly defined in subclass
     * InheritableThreadLocal, but is internally defined here for the
     * sake of providing createInheritedMap factory method without
     * needing to subclass the map class in InheritableThreadLocal.
     * This technique is preferable to the alternative of embedding
     * instanceof tests in methods.
     */
    // 子类InheritableThreadLocal中定义的方法, 用于计算可继承线程局部变量的子级初始值, 作为创建子线程时父级值的函数
    T childValue(T parentValue) {
        throw new UnsupportedOperationException();
    }

    // ThreadLocal 的扩展，它从指定的 {@code Supplier} 获取其初始值。
    /**
     * An extension of ThreadLocal that obtains its initial value from
     * the specified {@code Supplier}.
     */
    // ThreadLocal 的扩展，它从指定的 {@code Supplier} 获取其初始值
    static final class SuppliedThreadLocal<T> extends ThreadLocal<T> {

        private final Supplier<? extends T> supplier;

        SuppliedThreadLocal(Supplier<? extends T> supplier) {
            this.supplier = Objects.requireNonNull(supplier);
        }

        @Override
        protected T initialValue() {
            return supplier.get();
        }
    }

    /**
     * 20210816
     * ThreadLocalMap 是一种定制的哈希映射，仅适用于维护线程本地值。 不会在 ThreadLocal 类之外导出任何操作。 该类是包私有的，以允许在类 Thread 中声明字段。
     * 为了帮助处理非常大和长期存在的用法，哈希表条目使用 WeakReferences 作为键。 但是，由于不使用引用队列，因此只有在表开始耗尽空间时才能保证删除陈旧条目。
     */
    /**
     * ThreadLocalMap is a customized hash map suitable only for
     * maintaining thread local values. No operations are exported
     * outside of the ThreadLocal class. The class is package private to
     * allow declaration of fields in class Thread.  To help deal with
     * very large and long-lived usages, the hash table entries use
     * WeakReferences for keys. However, since reference queues are not
     * used, stale entries are guaranteed to be removed only when
     * the table starts running out of space.
     */
    static class ThreadLocalMap {

        /**
         * 20210816
         * 这个哈希映射中的条目扩展了 WeakReference，使用它的主要 ref 字段作为键（它总是一个 ThreadLocal 对象）。
         * 请注意，空键（即 entry.get() == null）意味着不再引用该键，因此可以从表中删除该条目。 此类条目在以下代码中称为“陈旧条目”。
         */
        /**
         * The entries in this hash map extend WeakReference, using
         * its main ref field as the key (which is always a
         * ThreadLocal object).  Note that null keys (i.e. entry.get()
         * == null) mean that the key is no longer referenced, so the
         * entry can be expunged from table.  Such entries are referred to
         * as "stale entries" in the code that follows.
         */
        static class Entry extends WeakReference<ThreadLocal<?>> {
            // 与此 ThreadLocal 关联的值。
            /** The value associated with this ThreadLocal. */
            Object value;

            // Entry#Key 强引用=> WeakReference 弱引用-> ThreadLocal:v
            Entry(ThreadLocal<?> k, Object v) {
                super(k);
                value = v;
            }
        }

        // 初始容量 -- 必须是 2 的幂。
        /**
         * The initial capacity -- MUST be a power of two.
         */
        // 初始容量
        private static final int INITIAL_CAPACITY = 16;

        // 表格，根据需要调整大小。 table.length 必须始终是 2 的幂。
        /**
         * The table, resized as necessary.
         * table.length MUST always be a power of two.
         */
        // 散列表
        private Entry[] table;

        // 表中的条目数。
        /**
         * The number of entries in the table.
         */
        // 散列表实际大小
        private int size = 0;

        // 要调整大小的下一个大小值。
        /**
         * The next size value at which to resize.
         */
        // 散列表阈值
        private int threshold; // Default to 0 // 默认为 0

        // 设置调整大小阈值以在最坏情况下保持 2/3 的负载因子。
        /**
         * Set the resize threshold to maintain at worst a 2/3 load factor.
         */
        // 根据容量设置阈值 => 取容量的2/3
        private void setThreshold(int len) {
            threshold = len * 2 / 3;
        }

        // 增量 i modulo len。
        /**
         * Increment i modulo len.
         */
        // 线性探测法: 用于解决此时产生的冲突, 查找散列表中离冲突单元最近的空闲单元，并且把新的键插入这个空闲单元。
        // 同样的，查找也同插入如出一辙：从散列函数给出的散列值对应的单元开始查找，直到找到与键对应的值或者是找到空单元。

        // 线性探测法, 获取下一个索引, i+1和0循环获取
        private static int nextIndex(int i, int len) {
            return ((i + 1 < len) ? i + 1 : 0);
        }

        /**
         * Decrement i modulo len.
         */
        // 线性探测法, 获取上一个索引, i-1和n-1循环获取
        private static int prevIndex(int i, int len) {
            return ((i - 1 >= 0) ? i - 1 : len - 1);
        }

        // 构造一个最初包含 (firstKey, firstValue) 的新映射。 ThreadLocalMaps 是惰性构造的，所以我们只有在至少有一个条目可以放入时才创建一个。
        /**
         * Construct a new map initially containing (firstKey, firstValue).
         * ThreadLocalMaps are constructed lazily, so we only create
         * one when we have at least one entry to put in it.
         */
        // 惰性构造默认容量16、默认负载因子16 * 2/3、ThreadLocal作为第一个Entry#Key的ThreadLocalMap, 只有在至少有一个条目可以放入时才创建
        ThreadLocalMap(ThreadLocal<?> firstKey, Object firstValue) {
            // 构造默认容量16的散列表table
            table = new Entry[INITIAL_CAPACITY];

            // 根据线程本地副本hashCode计算所在散列表中的索引i
            int i = firstKey.threadLocalHashCode & (INITIAL_CAPACITY - 1);

            // 使用ThreadLocal作为Entry#Key, 使用firstValue作为Entry#Value, 构造Entry并放入散列表中指定索引的位置
            table[i] = new Entry(firstKey, firstValue);

            // 更新散列表实际大小
            size = 1;

            // 根据容量设置阈值 => 取容量的2/3
            setThreshold(INITIAL_CAPACITY);
        }

        // 从给定的父映射构造一个包含所有可继承线程本地的新映射。 仅由 createInheritedMap 调用。
        /**
         * Construct a new map including all Inheritable ThreadLocals
         * from given parent map. Called only by createInheritedMap.
         *
         * @param parentMap the map associated with parent thread.
         */
        // 从给定的父映射构造一个包含所有可继承线程本地的新映射。 仅由 createInheritedMap 调用
        private ThreadLocalMap(ThreadLocalMap parentMap) {
            // 获取父散列表parentTable, 父散列表容量len
            Entry[] parentTable = parentMap.table;
            int len = parentTable.length;

            // 根据len容量设置阈值 => 取容量的2/3, 构造len长的散列表
            setThreshold(len);
            table = new Entry[len];

            // 遍历父散列表, 调用childValue计算值, 以及重新散列, 重新构造子线程本地副本
            for (int j = 0; j < len; j++) {
                Entry e = parentTable[j];
                if (e != null) {
                    @SuppressWarnings("unchecked")
                    ThreadLocal<Object> key = (ThreadLocal<Object>) e.get();
                    if (key != null) {
                        Object value = key.childValue(e.value);
                        Entry c = new Entry(key, value);
                        int h = key.threadLocalHashCode & (len - 1);
                        while (table[h] != null)
                            h = nextIndex(h, len);
                        table[h] = c;
                        size++;
                    }
                }
            }
        }

        /**
         * 20210816
         * 获取与密钥关联的条目。 此方法本身仅处理快速路径：直接点击现有密钥。 否则它会中继到 getEntryAfterMiss。
         * 这旨在最大限度地提高直接命中的性能，部分原因是使该方法易于内联。
         */
        /**
         * Get the entry associated with key.  This method
         * itself handles only the fast path: a direct hit of existing
         * key. It otherwise relays to getEntryAfterMiss.  This is
         * designed to maximize performance for direct hits, in part
         * by making this method readily inlinable.
         *
         * @param  key the thread local object
         * @return the entry associated with key, or null if no such
         */
        // 根据ThreadLocal中的散列码, 来获取ThreadLocalMap中的Entry, 如果根据散列码索引获取不到Entry, 则调用getEntryAfterMiss查找: 如果键匹配则返回e, 如果键为空则清空陈旧的条目然后返回null, 否则获取下一个索引i的Entry
        private Entry getEntry(ThreadLocal<?> key) {
            int i = key.threadLocalHashCode & (table.length - 1);
            Entry e = table[i];

            // 如果弱键还没被清空, 则返回找到的Entry
            if (e != null && e.get() == key)
                return e;
            // 如果e不存在, 或者弱键已过期, 则调用getEntryAfterMiss获取
            else
                // 根据索引获取不到Entry后调用, 如果键匹配则返回e, 如果键为空则清空陈旧的条目然后返回null, 否则获取下一个索引i的Entry
                return getEntryAfterMiss(key, i, e);
        }

        // 当在其直接散列槽中找不到密钥时使用的 getEntry 方法的版本。
        /**
         * Version of getEntry method for use when key is not found in
         * its direct hash slot.
         *
         * // 线程本地对象
         * @param  key the thread local object
         *
         * // 键的哈希码的表索引
         * @param  i the table index for key's hash code
         *
         * // 表中的条目[i]
         * @param  e the entry at table[i]
         *
         * // 与 key 关联的条目，如果没有，则为 null
         * @return the entry associated with key, or null if no such
         */
        // 根据索引获取不到Entry后调用, 如果键匹配则返回e, 如果键为空则清空陈旧的条目然后返回null, 否则获取下一个索引i的Entry
        private Entry getEntryAfterMiss(ThreadLocal<?> key, int i, Entry e) {
            // 获取散列表tab, 散列表容量len
            Entry[] tab = table;
            int len = tab.length;

            // 如果e为null, 则返回null
            while (e != null) {
                // 获取e弱键中的ThreadLocal k
                ThreadLocal<?> k = e.get();

                // 如果弱键匹配, 说明弱引用还没被清除, 则返回e
                if (k == key)
                    return e;

                // 如果弱键为null, 说明弱键已被清除
                if (k == null)
                    // 则根据空键表索引清除陈旧的Entry, 清空staleSlot位置的Entry, 然后从后一位开始遍历清除所有空键的Entry, 或者将其整理到第一个为null的槽位中, 返回staleSlot下一个null的索引
                    expungeStaleEntry(i);
                // 如果弱键不匹配也不为null, 则获取下一个索引的Entry e
                else
                    i = nextIndex(i, len);
                e = tab[i];
            }

            // 如果e为null, 则返回null
            return null;
        }

        /**
         * Set the value associated with key.
         *
         * @param key the thread local object
         * @param value the value to be set
         */
        // 设置ThreadLocal-value条目, 如果ThreadLocal存在则替换旧值; 如果ThreadLocal存在空键, 则交换旧值到新桶并向后清空空键; 如果ThreadLocal不存在条目, 则新增Entry条目, 如果新增后实际大小超过了1/2则还需要清空所有弱键并扩容2倍散列表
        private void set(ThreadLocal<?> key, Object value) {

            // 我们不像 get() 那样使用快速路径，因为使用 set() 创建新条目至少与替换现有条目一样常见，在这种情况下，快速路径通常会失败 .
            // We don't use a fast path as with get() because it is at
            // least as common to use set() to create new entries as
            // it is to replace existing ones, in which case, a fast
            // path would fail more often than not.

            // 根据ThreadLocal散列码获取表索引
            Entry[] tab = table;
            int len = tab.length;
            int i = key.threadLocalHashCode & (len-1);

            // 根据表索引获取Entry e, 遍历散列表, 直到为null的e
            for (Entry e = tab[i]; e != null; e = tab[i = nextIndex(i, len)]) {
                // 获取e的弱键k
                ThreadLocal<?> k = e.get();

                // 如果k为指定的ThreadLocal, 则替换e值为value后, 直接返回即可
                if (k == key) {
                    e.value = value;
                    return;
                }

                // 如果k为null, 说明弱键已被清除
                if (k == null) {
                    // 如果value存放的桶本来有弱键, 则交换弱键到新桶, 并向后清空弱键的条目; 如果没有则在staleSlot处构造key-value条目, 并向后清空弱键的条目
                    replaceStaleEntry(key, value, i);
                    return;
                }
            }

            // 如果i索引或者向后索引处桶为null, 则根据key-value构造Entry, 并更新散列表的实际大小
            tab[i] = new Entry(key, value);
            int sz = ++size;

            // 从i处开始遍历散列表, 清空一路上的空键Entry, 如果有发生删除则返回true, 否则返回false
            if (!cleanSomeSlots(i, sz) && sz >= threshold)
                // 如果没有删除弱键且实际大小超过了阈值, 则清除表中的所有陈旧条目, 如果实际大小超过了1/2, 则扩容散列表为原来的2倍, 遍历旧表, 如果为空键, 则清空条目; 如果不是, 则重新哈希Entry放入到新表的新桶中
                rehash();
        }

        /**
         * Remove the entry for key.
         */
        // 删除ThreadLocal的条目, 如果找到e键为指定的ThreadLocal, 则清空其弱键条目, 否则什么也不做
        private void remove(ThreadLocal<?> key) {
            // 根据ThreadLocal散列码获取表索引i
            Entry[] tab = table;
            int len = tab.length;
            int i = key.threadLocalHashCode & (len-1);

            // 从i处向后遍历散列表, 直到为null的条目
            for (Entry e = tab[i]; e != null; e = tab[i = nextIndex(i, len)]) {
                // 如果e键为指定的ThreadLocal, 则清空其弱键条目
                if (e.get() == key) {
                    // (非JVM调用)清空软/弱/虚 ref引用的实例对象(即data实际业务对象)
                    e.clear();

                    // 根据空键表索引清除陈旧的Entry, 清空staleSlot位置的Entry, 然后从后一位开始遍历清除所有空键的Entry, 或者将其整理到第一个为null的槽位中, 返回staleSlot下一个null的索引
                    expungeStaleEntry(i);
                    return;
                }
            }
        }

        /**
         * 20210816
         * A. 将设置操作期间遇到的陈旧条目替换为指定键的条目。 在 value 参数中传递的值存储在条目中，无论指定键的条目是否已经存在。
         * B. 作为副作用，此方法会清除包含陈旧条目的“运行”中的所有陈旧条目。 （运行是两个空槽之间的一系列条目。）
         */
        /**
         * A.
         * Replace a stale entry encountered during a set operation
         * with an entry for the specified key.  The value passed in
         * the value parameter is stored in the entry, whether or not
         * an entry already exists for the specified key.
         *
         * B.
         * As a side effect, this method expunges all stale entries in the
         * "run" containing the stale entry.  (A run is a sequence of entries
         * between two null slots.)
         *
         * @param  key the key
         * @param  value the value to be associated with key
         *
         * // 搜索键时遇到的第一个过时条目的索引。
         * @param  staleSlot index of the first stale entry encountered while
         *         searching for key.
         */
        // 如果value存放的桶本来有弱键, 则交换弱键到新桶, 并向后清空弱键的条目; 如果没有则在staleSlot处构造key-value条目, 并向后清空弱键的条目
        private void replaceStaleEntry(ThreadLocal<?> key, Object value, int staleSlot) {
            Entry[] tab = table;
            int len = tab.length;
            Entry e;

            // 备份以检查当前运行中先前的陈旧条目。 我们一次清除整个运行，以避免由于垃圾收集器成束地释放引用（即，每当收集器运行时）而导致的持续增量重新散列。
            // Back up to check for prior stale entry in current run.
            // We clean out whole runs at a time to avoid continual
            // incremental rehashing due to garbage collector freeing
            // up refs in bunches (i.e., whenever the collector runs).
            // 备份空键索引slotToExpunge, 向前遍历散列表, 如果遇到空键则赋予给slotToExpunge, 代表第一个空键索引
            int slotToExpunge = staleSlot;
            for (int i = prevIndex(staleSlot, len); (e = tab[i]) != null; i = prevIndex(i, len))
                if (e.get() == null)
                    slotToExpunge = i;

            // 找到 run 的键或尾随空槽，以先发生者为准
            // Find either the key or trailing null slot of run, whichever
            // occurs first
            // 从staleSlot向后遍历散列表, 直到遇到null的条目
            for (int i = nextIndex(staleSlot, len); (e = tab[i]) != null; i = nextIndex(i, len)) {
                // 获取e的弱键k
                ThreadLocal<?> k = e.get();

                // 如果我们找到键，那么我们需要将它与陈旧条目交换以维护哈希表顺序。 然后可以将新的陈旧槽或在其上方遇到的任何其他陈旧槽发送到 expungeStaleEntry
                // 以删除或重新散列运行中的所有其他条目。
                // If we find key, then we need to swap it
                // with the stale entry to maintain hash table order.
                // The newly stale slot, or any other stale slot
                // encountered above it, can then be sent to expungeStaleEntry
                // to remove or rehash all of the other entries in run.
                // 如果k与指定的ThreadLocal相等, 则将value交换到e上, 即索引staleSlot的条目与索引i的条目进行交换
                if (k == key) {
                    e.value = value;

                    tab[i] = tab[staleSlot];
                    tab[staleSlot] = e;

                    // 如果存在，则从前面的陈旧条目开始清除, 则从i位置开始向后清除空键Entry
                    // Start expunge at preceding stale entry if it exists
                    if (slotToExpunge == staleSlot)
                        slotToExpunge = i;

                    // 从i处开始遍历散列表, 清空一路上的空键Entry, 如果有发生删除则返回true, 否则返回false
                    cleanSomeSlots(
                            // 根据空键表索引清除陈旧的Entry, 清空staleSlot位置的Entry, 然后从后一位开始遍历清除所有空键的Entry, 或者将其整理到第一个为null的槽位中, 返回staleSlot下一个null的索引
                            expungeStaleEntry(slotToExpunge), len);

                    // 清除后则返回
                    return;
                }

                // 如果我们在向后扫描中没有找到陈旧条目，则在扫描密钥时看到的第一个陈旧条目是运行中仍然存在的第一个条目。
                // If we didn't find stale entry on backward scan, the
                // first stale entry seen while scanning for key is the
                // first still present in the run.
                // 如果k为null, 则从staleSlot处开始向后清除空键Entry
                if (k == null && slotToExpunge == staleSlot)
                    slotToExpunge = i;
            }

            // 如果未找到密钥，则将新条目放入陈旧插槽中
            // If key not found, put new entry in stale slot
            tab[staleSlot].value = null;
            tab[staleSlot] = new Entry(key, value);

            // 如果运行中有任何其他陈旧条目，请删除它们
            // If there are any other stale entries in run, expunge them
            // 在staleSlot构造完Entry后, 如果staleSlot不等于slotToExpunge, 说明散列表中至少有2个条目
            if (slotToExpunge != staleSlot)
                // 则从i处开始遍历散列表, 清空一路上的空键Entry, 如果有发生删除则返回true, 否则返回false
                cleanSomeSlots(
                        // 根据空键表索引清除陈旧的Entry, 清空staleSlot位置的Entry, 然后从后一位开始遍历清除所有空键的Entry, 或者将其整理到第一个为null的槽位中, 返回staleSlot下一个null的索引
                        expungeStaleEntry(slotToExpunge), len);
        }

        // 通过重新散列位于 staleSlot 和下一个空槽之间的任何可能冲突的条目来清除陈旧的条目。 这也会清除在尾随空值之前遇到的任何其他陈旧条目。 见 Knuth，第 6.4 节
        /**
         * Expunge a stale entry by rehashing any possibly colliding entries
         * lying between staleSlot and the next null slot.  This also expunges
         * any other stale entries encountered before the trailing null.  See
         * Knuth, Section 6.4
         *
         * // 已知具有空键的插槽索引
         * @param staleSlot index of slot known to have null key
         *
         * // staleSlot 之后的下一个空槽的索引（所有在 staleSlot 和这个槽之间的都将被检查以进行清除）。
         * @return the index of the next null slot after staleSlot
         * (all between staleSlot and this slot will have been checked
         * for expunging).
         */
        // 根据空键表索引清除陈旧的Entry, 清空staleSlot位置的Entry, 然后从后一位开始遍历清除所有空键的Entry, 或者将其整理到第一个为null的槽位中, 返回staleSlot下一个null的索引
        private int expungeStaleEntry(int staleSlot) {
            Entry[] tab = table;
            int len = tab.length;

            // 在 staleSlot 删除条目
            // expunge entry at staleSlot
            tab[staleSlot].value = null;
            tab[staleSlot] = null;
            size--;

            // 重新哈希直到我们遇到 null
            // Rehash until we encounter null
            Entry e;
            int i;

            // 从staleSlot后一位开始遍历, 直到碰到为null的Entry
            for (i = nextIndex(staleSlot, len); (e = tab[i]) != null; i = nextIndex(i, len)) {
                // 获取其弱键k
                ThreadLocal<?> k = e.get();

                // 如果k为null, 说明k也被清除了, 则清除关联的条目
                if (k == null) {
                    e.value = null;
                    tab[i] = null;
                    size--;
                }
                // 如果没有被清除, 则根据k重新散列, 然后清空原i的槽, 然后从h扫描直到碰到为null的槽, 最后把e放入
                else {
                    int h = k.threadLocalHashCode & (len - 1);
                    if (h != i) {
                        tab[i] = null;

                        // 与 Knuth 6.4 算法 R 不同，我们必须扫描直到为空，因为多个条目可能已经过时。
                        // Unlike Knuth 6.4 Algorithm R, we must scan until
                        // null because multiple entries could have been stale.
                        while (tab[h] != null)
                            h = nextIndex(h, len);
                        tab[h] = e;
                    }
                }
            }

            // 返回staleSlot下一个null的索引
            return i;
        }

        /**
         * 20210816
         * 启发式扫描一些单元格以查找过时的条目。 这在添加新元素或删除另一个陈旧元素时调用。
         * 它执行对数扫描，作为不扫描（快速但保留垃圾）和扫描次数与元素数量成正比之间的平衡，这将找到所有垃圾但会导致某些插入花费 O(n) 时间。
         */
        /**
         * Heuristically scan some cells looking for stale entries.
         * This is invoked when either a new element is added, or
         * another stale one has been expunged. It performs a
         * logarithmic number of scans, as a balance between no
         * scanning (fast but retains garbage) and a number of scans
         * proportional to number of elements, that would find all
         * garbage but would cause some insertions to take O(n) time.
         *
         * // 一个已知不会持有过时条目的位置。 扫描从 i 之后的元素开始。
         * @param i a position known NOT to hold a stale entry. The
         * scan starts at the element after i.
         *
         * // 扫描控制：扫描 {@code log2(n)} 个单元格，除非找到过时的条目，在这种情况下，扫描 {@code log2(table.length)-1} 个额外的单元格。
         *             从插入调用时，此参数是元素数，但从replaceStaleEntry 调用时，它是表长度。
         *             （注意：所有这些都可以通过对 n 进行加权而不是仅使用直接对数 n 来更改为或多或少的激进。但此版本简单、快速，并且似乎运行良好。）
         * @param n scan control: {@code log2(n)} cells are scanned,
         * unless a stale entry is found, in which case
         * {@code log2(table.length)-1} additional cells are scanned.
         * When called from insertions, this parameter is the number
         * of elements, but when from replaceStaleEntry, it is the
         * table length. (Note: all this could be changed to be either
         * more or less aggressive by weighting n instead of just
         * using straight log n. But this version is simple, fast, and
         * seems to work well.)
         *
         * // 如果已删除任何陈旧条目，则为 true。
         * @return true if any stale entries have been removed.
         */
        // 从i处开始遍历散列表, 清空一路上的空键Entry, 如果有发生删除则返回true, 否则返回false
        private boolean cleanSomeSlots(int i, int n) {
            boolean removed = false;
            Entry[] tab = table;
            int len = tab.length;
            do {
                // 从i处开始遍历散列表, 碰到弱键则根据空键表索引清除陈旧的Entry, 清空staleSlot位置的Entry, 然后从后一位开始遍历清除所有空键的Entry, 或者将其整理到第一个为null的槽位中, 返回staleSlot下一个null的索引作为新的索引i
                i = nextIndex(i, len);
                Entry e = tab[i];
                if (e != null && e.get() == null) {
                    n = len;
                    removed = true;
                    i = expungeStaleEntry(i);
                }
            } while ( (n >>>= 1) != 0);
            return removed;
        }

        // 重新包装和/或重新调整表格的大小。 首先扫描整个表，删除陈旧的条目。 如果这不能充分缩小表格的大小，请将表格大小加倍。
        /**
         * Re-pack and/or re-size the table. First scan the entire
         * table removing stale entries. If this doesn't sufficiently
         * shrink the size of the table, double the table size.
         */
        // 清除表中的所有陈旧条目, 如果实际大小超过了1/2, 则扩容散列表为原来的2倍, 遍历旧表, 如果为空键, 则清空条目; 如果不是, 则重新哈希Entry放入到新表的新桶中
        private void rehash() {
            // 清除表中的所有陈旧条目
            expungeStaleEntries();

            // 使用较低的加倍阈值以避免滞后
            // Use lower threshold for doubling to avoid hysteresis
            // 如果实际大小超过了2/3=8/12 - 2/12=6/12=1/2, 则扩容散列表为原来的2倍, 遍历旧表, 如果为空键, 则清空条目; 如果不是, 则重新哈希Entry放入到新表的新桶中
            if (size >= threshold - threshold / 4)
                resize();
        }

        /**
         * Double the capacity of the table.
         */
        // 扩容散列表为原来的2倍, 遍历旧表, 如果为空键, 则清空条目; 如果不是, 则重新哈希Entry放入到新表的新桶中
        private void resize() {
            Entry[] oldTab = table;
            int oldLen = oldTab.length;
            int newLen = oldLen * 2;
            Entry[] newTab = new Entry[newLen];
            int count = 0;

            // 遍历旧表, 如果为空键, 则清空条目; 如果不是, 则重新哈希Entry放入到新表的新桶中
            for (int j = 0; j < oldLen; ++j) {
                Entry e = oldTab[j];
                if (e != null) {
                    ThreadLocal<?> k = e.get();
                    if (k == null) {
                        e.value = null; // Help the GC
                    } else {
                        int h = k.threadLocalHashCode & (newLen - 1);
                        while (newTab[h] != null)
                            h = nextIndex(h, newLen);
                        newTab[h] = e;
                        count++;
                    }
                }
            }

            // 根据容量设置阈值 => 取容量的2/3
            setThreshold(newLen);
            size = count;
            table = newTab;
        }

        /**
         * Expunge all stale entries in the table.
         */
        // 清除表中的所有陈旧条目
        private void expungeStaleEntries() {
            Entry[] tab = table;
            int len = tab.length;
            for (int j = 0; j < len; j++) {
                Entry e = tab[j];
                if (e != null && e.get() == null)
                    // 根据空键表索引清除陈旧的Entry, 清空staleSlot位置的Entry, 然后从后一位开始遍历清除所有空键的Entry, 或者将其整理到第一个为null的槽位中, 返回staleSlot下一个null的索引
                    expungeStaleEntry(j);
            }
        }
    }
}
