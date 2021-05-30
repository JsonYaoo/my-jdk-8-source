/*
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

/*
 *
 *
 *
 *
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util;

/**
 * 20210521
 * A. 设计用于在处理之前容纳元素的集合。 除了基本的{@link java.util.Collection Collection}操作之外，队列还提供其他插入，提取和检查操作。 这些方法均以两种形式存在：
 *    一种在操作失败时引发异常，另一种返回特殊值（{@code null}或{@code false}，具体取决于操作）。 插入操作的后一种形式是专为与容量受限的{@code Queue}实现一起使用而设计的；
 *    在大多数实现中，插入操作不会失败。
 * B. 队列通常但不一定以FIFO（先进先出）的方式对元素进行排序。 例外情况包括优先级队列（根据提供的比较器对元素进行排序或元素的自然排序）和LIFO队列（堆栈），
 *    对LIFO进行排序（后进先出）。 无论使用哪种顺序，队列的开头都是该元素，可以通过调用{@link #remove（）}或{@link #poll（）}来删除该元素。
 *    在FIFO队列中，所有新元素都插入到队列的尾部。 其他种类的队列可能使用不同的放置规则。 每个{@code Queue}实现都必须指定其排序属性。
 * C. {@link #offer offer}方法在可能的情况下插入一个元素，否则返回{@code false}。 这与{@link java.util.Collection＃add Collection.add}方法不同，
 *    该方法只能通过抛出未经检查的异常来添加元素。 设计{@Code offer}方法是为了在正常情况下而不是在例外情况下（例如在固定容量（或“有界”）队列中）发生故障时使用。
 * D. {@link #remove（）}和{@link #poll（）}方法删除并返回队列的开头。 确切地说，从队列中删除了哪个元素是队列的排序策略的函数，每个实现的实现方法不同。
 *    {@code remove（）}和{@code poll（）}方法的区别仅在于队列为空时它们的行为：{@code remove（）}方法引发异常，而{@code poll（）} 方法返回{@code null}。
 * E. {@link #element（）}和{@link #peek（）}方法返回但不删除队列头。
 * F. {@code Queue}接口未定义阻塞队列方法，这在并发编程中很常见。 这些方法等待元素出现或空间可用，这些方法在扩展该接口的
 *    {@link java.util.concurrent.BlockingQueue}接口中定义。
 * G. {@code Queue}实现通常不允许插入{@code null}元素，尽管某些实现（例如{@link LinkedList}）不禁止插入{@code null}。 即使在允许的实现中，
 *    也不应将{@code null}插入{@code Queue}中，因为{@code poll}方法也将{@code null}用作特殊的返回值，以指示队列不包含任何元素。
 * H. {@code Queue}实现通常不定义方法{@code equals}和{@code hashCode}的基于元素的版本，而是从类{@code Object}继承基于身份的版本，
 *     因为基于元素的相等性并不总是对于具有相同元素但排序属性不同的队列，定义明确。
 * I. {@docRoot}/../technotes/guides/collections/index.html
 */

/**
 * A.
 * A collection designed for holding elements prior to processing.
 * Besides basic {@link java.util.Collection Collection} operations,
 * queues provide additional insertion, extraction, and inspection
 * operations.  Each of these methods exists in two forms: one throws
 * an exception if the operation fails, the other returns a special
 * value (either {@code null} or {@code false}, depending on the
 * operation).  The latter form of the insert operation is designed
 * specifically for use with capacity-restricted {@code Queue}
 * implementations; in most implementations, insert operations cannot
 * fail.
 *
 * <table BORDER CELLPADDING=3 CELLSPACING=1>
 * <caption>Summary of Queue methods</caption>
 *  <tr>
 *    <td></td>
 *    <td ALIGN=CENTER><em>Throws exception</em></td>
 *    <td ALIGN=CENTER><em>Returns special value</em></td>
 *  </tr>
 *  <tr>
 *    <td><b>Insert</b></td>
 *    <td>{@link Queue#add add(e)}</td>
 *    <td>{@link Queue#offer offer(e)}</td>
 *  </tr>
 *  <tr>
 *    <td><b>Remove</b></td>
 *    <td>{@link Queue#remove remove()}</td>
 *    <td>{@link Queue#poll poll()}</td>
 *  </tr>
 *  <tr>
 *    <td><b>Examine</b></td>
 *    <td>{@link Queue#element element()}</td>
 *    <td>{@link Queue#peek peek()}</td>
 *  </tr>
 * </table>
 *
 * B.
 * <p>Queues typically, but do not necessarily, order elements in a
 * FIFO (first-in-first-out) manner.  Among the exceptions are
 * priority queues, which order elements according to a supplied
 * comparator, or the elements' natural ordering, and LIFO queues (or
 * stacks) which order the elements LIFO (last-in-first-out).
 * Whatever the ordering used, the <em>head</em> of the queue is that
 * element which would be removed by a call to {@link #remove() } or
 * {@link #poll()}.  In a FIFO queue, all new elements are inserted at
 * the <em>tail</em> of the queue. Other kinds of queues may use
 * different placement rules.  Every {@code Queue} implementation
 * must specify its ordering properties.
 *
 * C.
 * <p>The {@link #offer offer} method inserts an element if possible,
 * otherwise returning {@code false}.  This differs from the {@link
 * java.util.Collection#add Collection.add} method, which can fail to
 * add an element only by throwing an unchecked exception.  The
 * {@code offer} method is designed for use when failure is a normal,
 * rather than exceptional occurrence, for example, in fixed-capacity
 * (or &quot;bounded&quot;) queues.
 *
 * D.
 * <p>The {@link #remove()} and {@link #poll()} methods remove and
 * return the head of the queue.
 * Exactly which element is removed from the queue is a
 * function of the queue's ordering policy, which differs from
 * implementation to implementation. The {@code remove()} and
 * {@code poll()} methods differ only in their behavior when the
 * queue is empty: the {@code remove()} method throws an exception,
 * while the {@code poll()} method returns {@code null}.
 *
 * E.
 * <p>The {@link #element()} and {@link #peek()} methods return, but do
 * not remove, the head of the queue.
 *
 * F.
 * <p>The {@code Queue} interface does not define the <i>blocking queue
 * methods</i>, which are common in concurrent programming.  These methods,
 * which wait for elements to appear or for space to become available, are
 * defined in the {@link java.util.concurrent.BlockingQueue} interface, which
 * extends this interface.
 *
 * G.
 * <p>{@code Queue} implementations generally do not allow insertion
 * of {@code null} elements, although some implementations, such as
 * {@link LinkedList}, do not prohibit insertion of {@code null}.
 * Even in the implementations that permit it, {@code null} should
 * not be inserted into a {@code Queue}, as {@code null} is also
 * used as a special return value by the {@code poll} method to
 * indicate that the queue contains no elements.
 *
 * H.
 * <p>{@code Queue} implementations generally do not define
 * element-based versions of methods {@code equals} and
 * {@code hashCode} but instead inherit the identity based versions
 * from class {@code Object}, because element-based equality is not
 * always well-defined for queues with the same elements but different
 * ordering properties.
 *
 * I.
 * <p>This interface is a member of the
 * <a href="{@docRoot}/../technotes/guides/collections/index.html">
 * Java Collections Framework</a>.
 *
 * @see java.util.Collection
 * @see LinkedList
 * @see PriorityQueue
 * @see java.util.concurrent.LinkedBlockingQueue
 * @see java.util.concurrent.BlockingQueue
 * @see java.util.concurrent.ArrayBlockingQueue
 * @see java.util.concurrent.LinkedBlockingQueue
 * @see java.util.concurrent.PriorityBlockingQueue
 * @since 1.5
 * @author Doug Lea
 * @param <E> the type of elements held in this collection
 */
// 20201118 仅仅提供一些操作元素的方法声明
public interface Queue<E> extends Collection<E> {

    /**
     * 20210521
     * 如果可以立即将指定的元素插入此队列，而不会违反容量限制，则在成功后返回{@code true}，如果当前没有可用空间，则抛出{@code IllegalStateException}。
     */
    /**
     * Inserts the specified element into this queue if it is possible to do so
     * immediately without violating capacity restrictions, returning
     * {@code true} upon success and throwing an {@code IllegalStateException}
     * if no space is currently available.
     *
     * @param e the element to add
     * @return {@code true} (as specified by {@link Collection#add})
     * @throws IllegalStateException if the element cannot be added at this
     *         time due to capacity restrictions
     * @throws ClassCastException if the class of the specified element
     *         prevents it from being added to this queue
     * @throws NullPointerException if the specified element is null and
     *         this queue does not permit null elements
     * @throws IllegalArgumentException if some property of this element
     *         prevents it from being added to this queue
     */
    boolean add(E e);

    /**
     * 20210521
     * 如果可以立即将指定的元素插入此队列，而不会违反容量限制。 当使用容量受限的队列时，此方法通常比{@link #add}更可取，后者只能通过引发异常才能插入元素。
     */
    /**
     * Inserts the specified element into this queue if it is possible to do
     * so immediately without violating capacity restrictions.
     * When using a capacity-restricted queue, this method is generally
     * preferable to {@link #add}, which can fail to insert an element only
     * by throwing an exception.
     *
     * @param e the element to add
     * @return {@code true} if the element was added to this queue, else
     *         {@code false}
     * @throws ClassCastException if the class of the specified element
     *         prevents it from being added to this queue
     * @throws NullPointerException if the specified element is null and
     *         this queue does not permit null elements
     * @throws IllegalArgumentException if some property of this element
     *         prevents it from being added to this queue
     */
    boolean offer(E e);

    /**
     * 20210521
     * 检索并删除此队列的头。 此方法与{@link #poll poll}的不同之处仅在于，如果此队列为空，它将引发异常。
     */
    /**
     * Retrieves and removes the head of this queue.  This method differs
     * from {@link #poll poll} only in that it throws an exception if this
     * queue is empty.
     *
     * @return the head of this queue
     * @throws NoSuchElementException if this queue is empty
     */
    E remove();

    /**
     * 检索并删除此队列的开头，如果此队列为空，则返回{@code null}。
     */
    /**
     * Retrieves and removes the head of this queue,
     * or returns {@code null} if this queue is empty.
     *
     * @return the head of this queue, or {@code null} if this queue is empty
     */
    E poll();

    /**
     * 检索但不删除此队列的头。 此方法与{@link #peek peek}的不同之处仅在于，如果此队列为空，它将引发异常。
     */
    /**
     * Retrieves, but does not remove, the head of this queue.  This method
     * differs from {@link #peek peek} only in that it throws an exception
     * if this queue is empty.
     *
     * @return the head of this queue
     * @throws NoSuchElementException if this queue is empty
     */
    E element();

    /**
     * 检索但不删除此队列的开头，如果此队列为空，则返回{@code null}。
     */
    /**
     * Retrieves, but does not remove, the head of this queue,
     * or returns {@code null} if this queue is empty.
     *
     * @return the head of this queue, or {@code null} if this queue is empty
     */
    E peek();
}
