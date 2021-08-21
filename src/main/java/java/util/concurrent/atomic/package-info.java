/*
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

/**
 * 20210811
 * A. 支持对单个变量进行无锁线程安全编程的小型类工具包。 本质上，这个包中的类将 {@code volatile} 值、字段和数组元素的概念扩展到那些还提供以下形式的原子条件更新操作的类：
 *       {@code boolean compareAndSet(expectedValue, updateValue);}
 * B. 如果该方法当前持有 {@code expectedValue}，则该方法（不同类的参数类型不同）原子地将变量设置为 {@code updateValue}，成功时报告 {@code true}。
 *    此包中的类还包含获取和无条件设置值的方法，以及下面描述的较弱的条件原子更新操作 {@code weakCompareAndSet}。
 * C. 这些方法的规范使实现能够使用当代处理器上可用的高效机器级原子指令。 然而，在某些平台上，支持可能需要某种形式的内部锁定。
 *    因此，这些方法不能严格保证是非阻塞的——线程在执行操作之前可能会暂时阻塞。
 * D. 类的实例 {@link java.util.concurrent.atomic.AtomicBoolean}、{@link java.util.concurrent.atomic.AtomicInteger}、
 *    {@link java.util.concurrent.atomic.AtomicLong} 和 {@link java .util.concurrent.atomic.AtomicReference} 每个都提供对相应类型的单个变量的访问和更新。
 *    每个类还为该类型提供适当的实用程序方法。 例如，类 {@code AtomicLong} 和 {@code AtomicInteger} 提供原子增量方法。 一种应用是生成序列号，如：
 * {@code
 * class Sequencer {
 *   private final AtomicLong sequenceNumber
 *     = new AtomicLong(0);
 *   public long next() {
 *     return sequenceNumber.getAndIncrement();
 *   }
 * }}
 * E. 定义新的实用函数很简单，比如 {@code getAndIncrement}，将函数以原子方式应用于值。 例如，给定一些转换:
 *       {@code long transform(long input)}
 *    编写您的实用方法如下：
 *  {@code
 * long getAndTransform(AtomicLong var) {
 *   long prev, next;
 *   do {
 *     prev = var.get();
 *     next = transform(prev);
 *   } while (!var.compareAndSet(prev, next));
 *   return prev; // return next; for transformAndGet
 * }}
 * F. 原子的访问和更新的内存效应通常遵循 volatile 的规则，如 The Java Language Specification (17.4 Memory Model) 中所述：
 *      a. {@code get} 具有读取 {@code volatile} 变量的记忆效应。
 *      b. {@code set} 具有写入（分配）{@code volatile} 变量的记忆效应。
 *      c. {@code lazySet} 具有写入（分配）{@code volatile} 变量的记忆效应，但它允许对后续（但不是先前）内存操作进行重新排序，
 *         这些操作本身不会对普通非 {@code volatile} 施加重新排序约束 } 写道。 在其他使用上下文中，{@code lazySet} 可能会在清空时应用，为了垃圾收集，
 *         永远不会再次访问的引用。
 *      d. {@code weakCompareAndSet} 以原子方式读取和有条件地写入变量，但不会创建任何发生在排序之前，
 *         因此不提供对除 {@code weakCompareAndSet} 目标之外的任何变量的先前或后续读取和写入的保证。
 *      f. @code compareAndSet} 和所有其他读取和更新操作（例如 {@code getAndIncrement}）具有读取和写入 {@code volatile} 变量的记忆效应。
 * G. 除了表示单个值的类之外，此包还包含更新程序类，可用于在任何选定类的任何选定 {@code volatile} 字段上获取 {@code compareAndSet} 操作。
 *    {@link java.util.concurrent.atomic.AtomicReferenceFieldUpdater}、{@link java.util.concurrent.atomic.AtomicIntegerFieldUpdater} 和
 *    {@link java.util.concurrent.atomic.AtomicLongFieldUpdater} 是基于反射的实用程序，提供访问 到关联的字段类型。 这些主要用于原子数据结构，
 *    其中同一节点的几个 {@code volatile} 字段（例如，树节点的链接）独立地受到原子更新。 这些类在如何以及何时使用原子更新方面提供了更大的灵活性，
 *    但代价是更笨拙的基于反射的设置、不太方便的使用和较弱的保证。
 * H. {@link java.util.concurrent.atomic.AtomicIntegerArray}、{@link java.util.concurrent.atomic.AtomicLongArray} 和
 *    {@link java.util.concurrent.atomic.AtomicReferenceArray} 类进一步将原子操作支持扩展到 这些类型的数组。
 *    这些类在为其数组元素提供 {@code volatile} 访问语义方面也值得注意，普通数组不支持这种语义。
 * I. 原子类还支持 {@code weakCompareAndSet} 方法，该方法适用性有限。在某些平台上，弱版本在正常情况下可能比 {@code compareAndSet} 更有效，
 *    但不同之处在于 {@code weakCompareAndSet} 方法的任何给定调用都可能虚假地返回 {@code false}（即，对于没有明显的原因）。
 *    {@code false} 返回意味着仅在需要时可以重试操作，这依赖于在变量持有 {@code expectedValue} 并且没有其他线程也尝试设置变量时重复调用最终会成功的保证。
 *    （例如，这种虚假故障可能是由于与预期值和当前值是否相等无关的内存争用效应。）此外，{@code weakCompareAndSet} 不提供同步控制通常需要的排序保证。
 *    然而，当此类更新与程序的其他发生前排序无关时，该方法可能对更新计数器和统计数据很有用。当线程看到由 {@code weakCompareAndSet} 引起的原子变量更新时，
 *    它不一定会看到在 {@code weakCompareAndSet} 之前发生的任何其他变量的更新。例如，在更新性能统计数据时，这可能是可以接受的，但在其他情况下则很少。
 * J. {@link java.util.concurrent.atomic.AtomicMarkableReference} 类将单个布尔值与引用相关联。 例如，该位可能用于数据结构内部，表示被引用的对象已被逻辑删除。
 * K. {@link java.util.concurrent.atomic.AtomicStampedReference} 类将整数值与引用相关联。 例如，这可以用来表示对应于一系列更新的版本号。
 * L. 原子类主要设计为用于实现非阻塞数据结构和相关基础设施类的构建块。 {@code compareAndSet} 方法不是锁定的一般替代品。 它仅适用于对象的关键更新仅限于单个变量时。
 * M. 原子类不是 {@code java.lang.Integer} 和相关类的通用替代品。 它们没有定义 {@code equals}、{@code hashCode} 和 {@code compareTo} 等方法。
 *   （因为原子变量会被改变，所以它们不是哈希表键的糟糕选择。）此外，只为那些在预期应用程序中通常有用的类型提供类。 例如，没有用于表示 {@code byte} 的原子类。
 *    在您希望这样做的罕见情况下，您可以使用 {@code AtomicInteger} 来保存 {@code byte} 值，并进行适当的转换。
 * N. 您还可以使用 {@link java.lang.Float#floatToRawIntBits} 和 {@link java.lang.Float#intBitsToFloat} 转换保持浮动，
 *    并使用 {@link java.lang.Double#doubleToRawLongBits} 和 {@link java .lang.Double#longBitsToDouble} 次转换。
 */
/**
 * A.
 * A small toolkit of classes that support lock-free thread-safe
 * programming on single variables.  In essence, the classes in this
 * package extend the notion of {@code volatile} values, fields, and
 * array elements to those that also provide an atomic conditional update
 * operation of the form:
 *
 *  <pre> {@code boolean compareAndSet(expectedValue, updateValue);}</pre>
 *
 * B.
 * <p>This method (which varies in argument types across different
 * classes) atomically sets a variable to the {@code updateValue} if it
 * currently holds the {@code expectedValue}, reporting {@code true} on
 * success.  The classes in this package also contain methods to get and
 * unconditionally set values, as well as a weaker conditional atomic
 * update operation {@code weakCompareAndSet} described below.
 *
 * C.
 * <p>The specifications of these methods enable implementations to
 * employ efficient machine-level atomic instructions that are available
 * on contemporary processors.  However on some platforms, support may
 * entail some form of internal locking.  Thus the methods are not
 * strictly guaranteed to be non-blocking --
 * a thread may block transiently before performing the operation.
 *
 * D.
 * <p>Instances of classes
 * {@link java.util.concurrent.atomic.AtomicBoolean},
 * {@link java.util.concurrent.atomic.AtomicInteger},
 * {@link java.util.concurrent.atomic.AtomicLong}, and
 * {@link java.util.concurrent.atomic.AtomicReference}
 * each provide access and updates to a single variable of the
 * corresponding type.  Each class also provides appropriate utility
 * methods for that type.  For example, classes {@code AtomicLong} and
 * {@code AtomicInteger} provide atomic increment methods.  One
 * application is to generate sequence numbers, as in:
 *
 *  <pre> {@code
 * class Sequencer {
 *   private final AtomicLong sequenceNumber
 *     = new AtomicLong(0);
 *   public long next() {
 *     return sequenceNumber.getAndIncrement();
 *   }
 * }}</pre>
 *
 * E.
 * <p>It is straightforward to define new utility functions that, like
 * {@code getAndIncrement}, apply a function to a value atomically.
 * For example, given some transformation
 * <pre> {@code long transform(long input)}</pre>
 *
 * write your utility method as follows:
 *  <pre> {@code
 * long getAndTransform(AtomicLong var) {
 *   long prev, next;
 *   do {
 *     prev = var.get();
 *     next = transform(prev);
 *   } while (!var.compareAndSet(prev, next));
 *   return prev; // return next; for transformAndGet
 * }}</pre>
 *
 * F.
 * <p>The memory effects for accesses and updates of atomics generally
 * follow the rules for volatiles, as stated in
 * <a href="https://docs.oracle.com/javase/specs/jls/se7/html/jls-17.html#jls-17.4">
 * The Java Language Specification (17.4 Memory Model)</a>:
 *
 * <ul>
 *
 *   <li> {@code get} has the memory effects of reading a
 * {@code volatile} variable.
 *
 *   <li> {@code set} has the memory effects of writing (assigning) a
 * {@code volatile} variable.
 *
 *   <li> {@code lazySet} has the memory effects of writing (assigning)
 *   a {@code volatile} variable except that it permits reorderings with
 *   subsequent (but not previous) memory actions that do not themselves
 *   impose reordering constraints with ordinary non-{@code volatile}
 *   writes.  Among other usage contexts, {@code lazySet} may apply when
 *   nulling out, for the sake of garbage collection, a reference that is
 *   never accessed again.
 *
 *   <li>{@code weakCompareAndSet} atomically reads and conditionally
 *   writes a variable but does <em>not</em>
 *   create any happens-before orderings, so provides no guarantees
 *   with respect to previous or subsequent reads and writes of any
 *   variables other than the target of the {@code weakCompareAndSet}.
 *
 *   <li> {@code compareAndSet}
 *   and all other read-and-update operations such as {@code getAndIncrement}
 *   have the memory effects of both reading and
 *   writing {@code volatile} variables.
 * </ul>
 *
 * G.
 * <p>In addition to classes representing single values, this package
 * contains <em>Updater</em> classes that can be used to obtain
 * {@code compareAndSet} operations on any selected {@code volatile}
 * field of any selected class.
 *
 * {@link java.util.concurrent.atomic.AtomicReferenceFieldUpdater},
 * {@link java.util.concurrent.atomic.AtomicIntegerFieldUpdater}, and
 * {@link java.util.concurrent.atomic.AtomicLongFieldUpdater} are
 * reflection-based utilities that provide access to the associated
 * field types.  These are mainly of use in atomic data structures in
 * which several {@code volatile} fields of the same node (for
 * example, the links of a tree node) are independently subject to
 * atomic updates.  These classes enable greater flexibility in how
 * and when to use atomic updates, at the expense of more awkward
 * reflection-based setup, less convenient usage, and weaker
 * guarantees.
 *
 * H.
 * <p>The
 * {@link java.util.concurrent.atomic.AtomicIntegerArray},
 * {@link java.util.concurrent.atomic.AtomicLongArray}, and
 * {@link java.util.concurrent.atomic.AtomicReferenceArray} classes
 * further extend atomic operation support to arrays of these types.
 * These classes are also notable in providing {@code volatile} access
 * semantics for their array elements, which is not supported for
 * ordinary arrays.
 *
 * I.
 * <p id="weakCompareAndSet">The atomic classes also support method
 * {@code weakCompareAndSet}, which has limited applicability.  On some
 * platforms, the weak version may be more efficient than {@code
 * compareAndSet} in the normal case, but differs in that any given
 * invocation of the {@code weakCompareAndSet} method may return {@code
 * false} <em>spuriously</em> (that is, for no apparent reason).  A
 * {@code false} return means only that the operation may be retried if
 * desired, relying on the guarantee that repeated invocation when the
 * variable holds {@code expectedValue} and no other thread is also
 * attempting to set the variable will eventually succeed.  (Such
 * spurious failures may for example be due to memory contention effects
 * that are unrelated to whether the expected and current values are
 * equal.)  Additionally {@code weakCompareAndSet} does not provide
 * ordering guarantees that are usually needed for synchronization
 * control.  However, the method may be useful for updating counters and
 * statistics when such updates are unrelated to the other
 * happens-before orderings of a program.  When a thread sees an update
 * to an atomic variable caused by a {@code weakCompareAndSet}, it does
 * not necessarily see updates to any <em>other</em> variables that
 * occurred before the {@code weakCompareAndSet}.  This may be
 * acceptable when, for example, updating performance statistics, but
 * rarely otherwise.
 *
 * J.
 * <p>The {@link java.util.concurrent.atomic.AtomicMarkableReference}
 * class associates a single boolean with a reference.  For example, this
 * bit might be used inside a data structure to mean that the object
 * being referenced has logically been deleted.
 *
 * K.
 * The {@link java.util.concurrent.atomic.AtomicStampedReference}
 * class associates an integer value with a reference.  This may be
 * used for example, to represent version numbers corresponding to
 * series of updates.
 *
 * L.
 * <p>Atomic classes are designed primarily as building blocks for
 * implementing non-blocking data structures and related infrastructure
 * classes.  The {@code compareAndSet} method is not a general
 * replacement for locking.  It applies only when critical updates for an
 * object are confined to a <em>single</em> variable.
 *
 * M.
 * <p>Atomic classes are not general purpose replacements for
 * {@code java.lang.Integer} and related classes.  They do <em>not</em>
 * define methods such as {@code equals}, {@code hashCode} and
 * {@code compareTo}.  (Because atomic variables are expected to be
 * mutated, they are poor choices for hash table keys.)  Additionally,
 * classes are provided only for those types that are commonly useful in
 * intended applications.  For example, there is no atomic class for
 * representing {@code byte}.  In those infrequent cases where you would
 * like to do so, you can use an {@code AtomicInteger} to hold
 * {@code byte} values, and cast appropriately.
 *
 * N.
 * You can also hold floats using
 * {@link java.lang.Float#floatToRawIntBits} and
 * {@link java.lang.Float#intBitsToFloat} conversions, and doubles using
 * {@link java.lang.Double#doubleToRawLongBits} and
 * {@link java.lang.Double#longBitsToDouble} conversions.
 *
 * @since 1.5
 */
package java.util.concurrent.atomic;
