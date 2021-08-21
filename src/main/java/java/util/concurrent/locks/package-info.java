/*
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

/**
 * 20210725
 * A. 接口和类提供了一个框架，用于锁定和等待不同于内置同步和监视器的条件。该框架允许在使用锁和条件时具有更大的灵活性，但代价是更笨拙的语法。
 * B. {@link java.util.concurrent.locks.Lock} 接口支持语义不同（可重入、公平等）的锁定规则，可用于非块结构上下文，包括hand-over-hand 和lock重排序算法。
 *    主要实现是{@link java.util.concurrent.locks.ReentrantLock}。
 * C. {@link java.util.concurrent.locks.ReadWriteLock} 接口类似地定义了可以在读取器之间共享但对写入器独占的锁。
 *    只提供了一个实现，{@link java.util.concurrent.locks.ReentrantReadWriteLock}，因为它涵盖了大多数标准使用上下文。但是程序员可以创建他们自己的实现来满足非标准要求。
 * D. {@link java.util.concurrent.locks.Condition}接口描述了可能与锁关联的条件变量。这些在用法上类似于使用 {@code Object.wait} 访问的隐式监视器，但提供了扩展功能。
 *    特别是，多个{@code Condition}对象可能与单个{@code Lock}相关联。为避免兼容性问题，{@code Condition}方法的名称与对应的{@code Object}版本不同。
 * E. {@link java.util.concurrent.locks.AbstractQueuedSynchronizer} 类是一个有用的超类，用于定义依赖于排队阻塞线程的锁和其他同步器。
 *    {@link java.util.concurrent.locks.AbstractQueuedLongSynchronizer} 类提供相同的功能，但将支持扩展到64位同步状态。
 *    两者都扩展了类{@link java.util.concurrent.locks.AbstractOwnableSynchronizer}，这是一个简单的类，有助于记录当前持有独占同步的线程。
 *    {@link java.util.concurrent.locks.LockSupport} 类提供较低级别的阻塞和解除阻塞支持，这对于实现他们自己的自定义锁类的开发人员非常有用。
 */
/**
 * A.
 * Interfaces and classes providing a framework for locking and waiting
 * for conditions that is distinct from built-in synchronization and
 * monitors.  The framework permits much greater flexibility in the use of
 * locks and conditions, at the expense of more awkward syntax.
 *
 * B.
 * <p>The {@link java.util.concurrent.locks.Lock} interface supports
 * locking disciplines that differ in semantics (reentrant, fair, etc),
 * and that can be used in non-block-structured contexts including
 * hand-over-hand and lock reordering algorithms.  The main implementation
 * is {@link java.util.concurrent.locks.ReentrantLock}.
 *
 * C.
 * <p>The {@link java.util.concurrent.locks.ReadWriteLock} interface
 * similarly defines locks that may be shared among readers but are
 * exclusive to writers.  Only a single implementation, {@link
 * java.util.concurrent.locks.ReentrantReadWriteLock}, is provided, since
 * it covers most standard usage contexts.  But programmers may create
 * their own implementations to cover nonstandard requirements.
 *
 * D.
 * <p>The {@link java.util.concurrent.locks.Condition} interface
 * describes condition variables that may be associated with Locks.
 * These are similar in usage to the implicit monitors accessed using
 * {@code Object.wait}, but offer extended capabilities.
 * In particular, multiple {@code Condition} objects may be associated
 * with a single {@code Lock}.  To avoid compatibility issues, the
 * names of {@code Condition} methods are different from the
 * corresponding {@code Object} versions.
 *
 * E.
 * <p>The {@link java.util.concurrent.locks.AbstractQueuedSynchronizer}
 * class serves as a useful superclass for defining locks and other
 * synchronizers that rely on queuing blocked threads.  The {@link
 * java.util.concurrent.locks.AbstractQueuedLongSynchronizer} class
 * provides the same functionality but extends support to 64 bits of
 * synchronization state.  Both extend class {@link
 * java.util.concurrent.locks.AbstractOwnableSynchronizer}, a simple
 * class that helps record the thread currently holding exclusive
 * synchronization.  The {@link java.util.concurrent.locks.LockSupport}
 * class provides lower-level blocking and unblocking support that is
 * useful for those developers implementing their own customized lock
 * classes.
 *
 * @since 1.5
 */
package java.util.concurrent.locks;
