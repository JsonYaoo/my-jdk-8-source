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
 * Written by Doug Lea and Josh Bloch with assistance from members of
 * JCP JSR-166 Expert Group and released to the public domain, as explained
 * at http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util;

/**
 * 20210521
 * A. 支持在两端插入和删除元素的线性集合。 名称双端队列是“双端队列”的缩写，通常发音为“deck”。 大多数{@code Deque}实现对它们可能包含的元素数量没有固定的限制，
 *    但是此接口支持容量受限的双端队列以及没有固定大小限制的双端队列。
 * B. 该接口定义了访问双端队列两端的元素的方法。 提供了用于插入，删除和检查元素的方法。 这些方法均以两种形式存在：一种在操作失败时引发异常，另一种返回特殊值
 *    （{@code null}或{@code false}，具体取决于操作）。 后一种形式的插入操作是专为与容量受限的{@code Deque}实现一起使用而设计的； 在大多数实现中，插入操作不会失败。
 * C. 下表总结了上述十二种方法：
 *                                                              Summary of Deque methods
 *           First Element (Head)                   First Element (Head)                    Last Element (Tail)
 *           Throws exception	                    Special value	                        Throws exception	                    Special value
 * Insert	{@link Deque#addFirst addFirst(e)}	    {@link Deque#offerFirst offerFirst(e)}	{@link Deque#addLast addLast(e)}	    {@link Deque#offerLast offerLast(e)}
 * Remove	{@link Deque#removeFirst removeFirst()}	{@link Deque#pollFirst pollFirst()}	    {@link Deque#removeLast removeLast()}	{@link Deque#pollLast pollLast()}
 * Examine	{@link Deque#getFirst getFirst()}	    {@link Deque#peekFirst peekFirst()}	    {@link Deque#getLast getLast()}	        {@link Deque#peekLast peekLast()}
 * D. 此接口扩展了{@link Queue}接口。 当双端队列用作队列时，将导致FIFO（先进先出）行为。 元素在双端队列的末尾添加，并从开头删除。 如下表所示，
 *    从{@code Queue}接口继承的方法与{@code Deque}方法完全等效：
 *                                                          Comparison of Queue and Deque methods
 *           {@code Queue} Method	                    Equivalent {@code Deque} Method
 *           {@link java.util.Queue#add add(e)}         {@link #addLast addLast(e)}
 *           {@link java.util.Queue#offer offer(e)}     {@link #offerLast offerLast(e)}
 *           {@link java.util.Queue#remove remove()}    {@link #removeFirst removeFirst()}
 *           {@link java.util.Queue#poll poll()}        {@link #pollFirst pollFirst()}
 *           {@link java.util.Queue#element element()}  {@link #getFirst getFirst()}
 *           {@link java.util.Queue#peek peek()}        {@link #peek peekFirst()}
 * E. 双端队列也可以用作LIFO（后进先出）堆栈。 此接口应优先于旧式{@link Stack}类使用。 当双端队列用作堆栈时，元素从双端队列的开头被压入并弹出。
 *    堆栈方法完全等同于{@code Deque}方法，如下表所示：
 *                                                          Comparison of Stack and Deque methods
 *           Stack Method	                            Equivalent {@code Deque} Method
 *           {@link #push push(e)}	                    {@link #addFirst addFirst(e)}
 *           {@link #pop pop()}	                        {@link #removeFirst removeFirst()}
 *           {@link #peek peek()}	                    {@link #peekFirst peekFirst()}
 * F. 请注意，当将双端队列用作队列或堆栈时，{@link #peek peek}方法同样有效。 无论哪种情况，元素都是从双端队列的开头开始绘制的。
 * G. 该接口提供了两种删除内部元素的方法：{@link #removeFirstOccurrence removeFirstOccurrence}和{@link #removeLastOccurrence removeLastOccurrence}。
 * H. 与{@link List}接口不同，此接口不支持对元素的索引访问。
 * I. 虽然并非严格要求{@code Deque}实现禁止插入空元素，但强烈建议这样做。 强烈建议任何允许使用null元素的{@code Deque}实现的用户不要利用插入null的能力。
 *    之所以如此，是因为各种方法都将{@code null}用作特殊的返回值，以指示双端队列为空。
 * J. {@code Deque}实现通常不定义{@code equals}和{@code hashCode}方法的基于元素的版本，而是从类{@code Object}继承基于身份的版本。
 * K. {@docRoot}/../technotes/guides/collections/index.html
 */

/**
 * A.
 * A linear collection that supports element insertion and removal at
 * both ends.  The name <i>deque</i> is short for "double ended queue"
 * and is usually pronounced "deck".  Most {@code Deque}
 * implementations place no fixed limits on the number of elements
 * they may contain, but this interface supports capacity-restricted
 * deques as well as those with no fixed size limit.
 *
 * B.
 * <p>This interface defines methods to access the elements at both
 * ends of the deque.  Methods are provided to insert, remove, and
 * examine the element.  Each of these methods exists in two forms:
 * one throws an exception if the operation fails, the other returns a
 * special value (either {@code null} or {@code false}, depending on
 * the operation).  The latter form of the insert operation is
 * designed specifically for use with capacity-restricted
 * {@code Deque} implementations; in most implementations, insert
 * operations cannot fail.
 *
 * C.
 * <p>The twelve methods described above are summarized in the
 * following table:
 *
 * <table BORDER CELLPADDING=3 CELLSPACING=1>
 * <caption>Summary of Deque methods</caption>
 *  <tr>
 *    <td></td>
 *    <td ALIGN=CENTER COLSPAN = 2> <b>First Element (Head)</b></td>
 *    <td ALIGN=CENTER COLSPAN = 2> <b>Last Element (Tail)</b></td>
 *  </tr>
 *  <tr>
 *    <td></td>
 *    <td ALIGN=CENTER><em>Throws exception</em></td>
 *    <td ALIGN=CENTER><em>Special value</em></td>
 *    <td ALIGN=CENTER><em>Throws exception</em></td>
 *    <td ALIGN=CENTER><em>Special value</em></td>
 *  </tr>
 *  <tr>
 *    <td><b>Insert</b></td>
 *    <td>{@link Deque#addFirst addFirst(e)}</td>
 *    <td>{@link Deque#offerFirst offerFirst(e)}</td>
 *    <td>{@link Deque#addLast addLast(e)}</td>
 *    <td>{@link Deque#offerLast offerLast(e)}</td>
 *  </tr>
 *  <tr>
 *    <td><b>Remove</b></td>
 *    <td>{@link Deque#removeFirst removeFirst()}</td>
 *    <td>{@link Deque#pollFirst pollFirst()}</td>
 *    <td>{@link Deque#removeLast removeLast()}</td>
 *    <td>{@link Deque#pollLast pollLast()}</td>
 *  </tr>
 *  <tr>
 *    <td><b>Examine</b></td>
 *    <td>{@link Deque#getFirst getFirst()}</td>
 *    <td>{@link Deque#peekFirst peekFirst()}</td>
 *    <td>{@link Deque#getLast getLast()}</td>
 *    <td>{@link Deque#peekLast peekLast()}</td>
 *  </tr>
 * </table>
 *
 * D.
 * <p>This interface extends the {@link Queue} interface.  When a deque is
 * used as a queue, FIFO (First-In-First-Out) behavior results.  Elements are
 * added at the end of the deque and removed from the beginning.  The methods
 * inherited from the {@code Queue} interface are precisely equivalent to
 * {@code Deque} methods as indicated in the following table:
 *
 * <table BORDER CELLPADDING=3 CELLSPACING=1>
 * <caption>Comparison of Queue and Deque methods</caption>
 *  <tr>
 *    <td ALIGN=CENTER> <b>{@code Queue} Method</b></td>
 *    <td ALIGN=CENTER> <b>Equivalent {@code Deque} Method</b></td>
 *  </tr>
 *  <tr>
 *    <td>{@link java.util.Queue#add add(e)}</td>
 *    <td>{@link #addLast addLast(e)}</td>
 *  </tr>
 *  <tr>
 *    <td>{@link java.util.Queue#offer offer(e)}</td>
 *    <td>{@link #offerLast offerLast(e)}</td>
 *  </tr>
 *  <tr>
 *    <td>{@link java.util.Queue#remove remove()}</td>
 *    <td>{@link #removeFirst removeFirst()}</td>
 *  </tr>
 *  <tr>
 *    <td>{@link java.util.Queue#poll poll()}</td>
 *    <td>{@link #pollFirst pollFirst()}</td>
 *  </tr>
 *  <tr>
 *    <td>{@link java.util.Queue#element element()}</td>
 *    <td>{@link #getFirst getFirst()}</td>
 *  </tr>
 *  <tr>
 *    <td>{@link java.util.Queue#peek peek()}</td>
 *    <td>{@link #peek peekFirst()}</td>
 *  </tr>
 * </table>
 *
 * E.
 * <p>Deques can also be used as LIFO (Last-In-First-Out) stacks.  This
 * interface should be used in preference to the legacy {@link Stack} class.
 * When a deque is used as a stack, elements are pushed and popped from the
 * beginning of the deque.  Stack methods are precisely equivalent to
 * {@code Deque} methods as indicated in the table below:
 *
 * <table BORDER CELLPADDING=3 CELLSPACING=1>
 * <caption>Comparison of Stack and Deque methods</caption>
 *  <tr>
 *    <td ALIGN=CENTER> <b>Stack Method</b></td>
 *    <td ALIGN=CENTER> <b>Equivalent {@code Deque} Method</b></td>
 *  </tr>
 *  <tr>
 *    <td>{@link #push push(e)}</td>
 *    <td>{@link #addFirst addFirst(e)}</td>
 *  </tr>
 *  <tr>
 *    <td>{@link #pop pop()}</td>
 *    <td>{@link #removeFirst removeFirst()}</td>
 *  </tr>
 *  <tr>
 *    <td>{@link #peek peek()}</td>
 *    <td>{@link #peekFirst peekFirst()}</td>
 *  </tr>
 * </table>
 *
 * F.
 * <p>Note that the {@link #peek peek} method works equally well when
 * a deque is used as a queue or a stack; in either case, elements are
 * drawn from the beginning of the deque.
 *
 * G.
 * <p>This interface provides two methods to remove interior
 * elements, {@link #removeFirstOccurrence removeFirstOccurrence} and
 * {@link #removeLastOccurrence removeLastOccurrence}.
 *
 * H.
 * <p>Unlike the {@link List} interface, this interface does not
 * provide support for indexed access to elements.
 *
 * I.
 * <p>While {@code Deque} implementations are not strictly required
 * to prohibit the insertion of null elements, they are strongly
 * encouraged to do so.  Users of any {@code Deque} implementations
 * that do allow null elements are strongly encouraged <i>not</i> to
 * take advantage of the ability to insert nulls.  This is so because
 * {@code null} is used as a special return value by various methods
 * to indicated that the deque is empty.
 *
 * J.
 * <p>{@code Deque} implementations generally do not define
 * element-based versions of the {@code equals} and {@code hashCode}
 * methods, but instead inherit the identity-based versions from class
 * {@code Object}.
 *
 * K.
 * <p>This interface is a member of the <a
 * href="{@docRoot}/../technotes/guides/collections/index.html"> Java Collections
 * Framework</a>.
 *
 * @author Doug Lea
 * @author Josh Bloch
 * @since  1.6
 * @param <E> the type of elements held in this collection
 */
// 20201118 双头队列接口
public interface Deque<E> extends Queue<E> {
    /**
     * Inserts the specified element at the front of this deque if it is
     * possible to do so immediately without violating capacity restrictions,
     * throwing an {@code IllegalStateException} if no space is currently
     * available.  When using a capacity-restricted deque, it is generally
     * preferable to use method {@link #offerFirst}.
     *
     * @param e the element to add
     * @throws IllegalStateException if the element cannot be added at this
     *         time due to capacity restrictions
     * @throws ClassCastException if the class of the specified element
     *         prevents it from being added to this deque
     * @throws NullPointerException if the specified element is null and this
     *         deque does not permit null elements
     * @throws IllegalArgumentException if some property of the specified
     *         element prevents it from being added to this deque
     */
    void addFirst(E e);

    /**
     * Inserts the specified element at the end of this deque if it is
     * possible to do so immediately without violating capacity restrictions,
     * throwing an {@code IllegalStateException} if no space is currently
     * available.  When using a capacity-restricted deque, it is generally
     * preferable to use method {@link #offerLast}.
     *
     * <p>This method is equivalent to {@link #add}.
     *
     * @param e the element to add
     * @throws IllegalStateException if the element cannot be added at this
     *         time due to capacity restrictions
     * @throws ClassCastException if the class of the specified element
     *         prevents it from being added to this deque
     * @throws NullPointerException if the specified element is null and this
     *         deque does not permit null elements
     * @throws IllegalArgumentException if some property of the specified
     *         element prevents it from being added to this deque
     */
    void addLast(E e);

    /**
     * Inserts the specified element at the front of this deque unless it would
     * violate capacity restrictions.  When using a capacity-restricted deque,
     * this method is generally preferable to the {@link #addFirst} method,
     * which can fail to insert an element only by throwing an exception.
     *
     * @param e the element to add
     * @return {@code true} if the element was added to this deque, else
     *         {@code false}
     * @throws ClassCastException if the class of the specified element
     *         prevents it from being added to this deque
     * @throws NullPointerException if the specified element is null and this
     *         deque does not permit null elements
     * @throws IllegalArgumentException if some property of the specified
     *         element prevents it from being added to this deque
     */
    boolean offerFirst(E e);

    /**
     * Inserts the specified element at the end of this deque unless it would
     * violate capacity restrictions.  When using a capacity-restricted deque,
     * this method is generally preferable to the {@link #addLast} method,
     * which can fail to insert an element only by throwing an exception.
     *
     * @param e the element to add
     * @return {@code true} if the element was added to this deque, else
     *         {@code false}
     * @throws ClassCastException if the class of the specified element
     *         prevents it from being added to this deque
     * @throws NullPointerException if the specified element is null and this
     *         deque does not permit null elements
     * @throws IllegalArgumentException if some property of the specified
     *         element prevents it from being added to this deque
     */
    boolean offerLast(E e);

    /**
     * Retrieves and removes the first element of this deque.  This method
     * differs from {@link #pollFirst pollFirst} only in that it throws an
     * exception if this deque is empty.
     *
     * @return the head of this deque
     * @throws NoSuchElementException if this deque is empty
     */
    E removeFirst();

    /**
     * Retrieves and removes the last element of this deque.  This method
     * differs from {@link #pollLast pollLast} only in that it throws an
     * exception if this deque is empty.
     *
     * @return the tail of this deque
     * @throws NoSuchElementException if this deque is empty
     */
    E removeLast();

    /**
     * Retrieves and removes the first element of this deque,
     * or returns {@code null} if this deque is empty.
     *
     * @return the head of this deque, or {@code null} if this deque is empty
     */
    E pollFirst();

    /**
     * Retrieves and removes the last element of this deque,
     * or returns {@code null} if this deque is empty.
     *
     * @return the tail of this deque, or {@code null} if this deque is empty
     */
    E pollLast();

    /**
     * Retrieves, but does not remove, the first element of this deque.
     *
     * This method differs from {@link #peekFirst peekFirst} only in that it
     * throws an exception if this deque is empty.
     *
     * @return the head of this deque
     * @throws NoSuchElementException if this deque is empty
     */
    E getFirst();

    /**
     * Retrieves, but does not remove, the last element of this deque.
     * This method differs from {@link #peekLast peekLast} only in that it
     * throws an exception if this deque is empty.
     *
     * @return the tail of this deque
     * @throws NoSuchElementException if this deque is empty
     */
    E getLast();

    /**
     * Retrieves, but does not remove, the first element of this deque,
     * or returns {@code null} if this deque is empty.
     *
     * @return the head of this deque, or {@code null} if this deque is empty
     */
    E peekFirst();

    /**
     * Retrieves, but does not remove, the last element of this deque,
     * or returns {@code null} if this deque is empty.
     *
     * @return the tail of this deque, or {@code null} if this deque is empty
     */
    E peekLast();

    /**
     * Removes the first occurrence of the specified element from this deque.
     * If the deque does not contain the element, it is unchanged.
     * More formally, removes the first element {@code e} such that
     * <tt>(o==null&nbsp;?&nbsp;e==null&nbsp;:&nbsp;o.equals(e))</tt>
     * (if such an element exists).
     * Returns {@code true} if this deque contained the specified element
     * (or equivalently, if this deque changed as a result of the call).
     *
     * @param o element to be removed from this deque, if present
     * @return {@code true} if an element was removed as a result of this call
     * @throws ClassCastException if the class of the specified element
     *         is incompatible with this deque
     * (<a href="Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException if the specified element is null and this
     *         deque does not permit null elements
     * (<a href="Collection.html#optional-restrictions">optional</a>)
     */
    boolean removeFirstOccurrence(Object o);

    /**
     * Removes the last occurrence of the specified element from this deque.
     * If the deque does not contain the element, it is unchanged.
     * More formally, removes the last element {@code e} such that
     * <tt>(o==null&nbsp;?&nbsp;e==null&nbsp;:&nbsp;o.equals(e))</tt>
     * (if such an element exists).
     * Returns {@code true} if this deque contained the specified element
     * (or equivalently, if this deque changed as a result of the call).
     *
     * @param o element to be removed from this deque, if present
     * @return {@code true} if an element was removed as a result of this call
     * @throws ClassCastException if the class of the specified element
     *         is incompatible with this deque
     * (<a href="Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException if the specified element is null and this
     *         deque does not permit null elements
     * (<a href="Collection.html#optional-restrictions">optional</a>)
     */
    boolean removeLastOccurrence(Object o);

    // *** Queue methods ***

    /**
     * Inserts the specified element into the queue represented by this deque
     * (in other words, at the tail of this deque) if it is possible to do so
     * immediately without violating capacity restrictions, returning
     * {@code true} upon success and throwing an
     * {@code IllegalStateException} if no space is currently available.
     * When using a capacity-restricted deque, it is generally preferable to
     * use {@link #offer(Object) offer}.
     *
     * <p>This method is equivalent to {@link #addLast}.
     *
     * @param e the element to add
     * @return {@code true} (as specified by {@link Collection#add})
     * @throws IllegalStateException if the element cannot be added at this
     *         time due to capacity restrictions
     * @throws ClassCastException if the class of the specified element
     *         prevents it from being added to this deque
     * @throws NullPointerException if the specified element is null and this
     *         deque does not permit null elements
     * @throws IllegalArgumentException if some property of the specified
     *         element prevents it from being added to this deque
     */
    boolean add(E e);

    /**
     * Inserts the specified element into the queue represented by this deque
     * (in other words, at the tail of this deque) if it is possible to do so
     * immediately without violating capacity restrictions, returning
     * {@code true} upon success and {@code false} if no space is currently
     * available.  When using a capacity-restricted deque, this method is
     * generally preferable to the {@link #add} method, which can fail to
     * insert an element only by throwing an exception.
     *
     * <p>This method is equivalent to {@link #offerLast}.
     *
     * @param e the element to add
     * @return {@code true} if the element was added to this deque, else
     *         {@code false}
     * @throws ClassCastException if the class of the specified element
     *         prevents it from being added to this deque
     * @throws NullPointerException if the specified element is null and this
     *         deque does not permit null elements
     * @throws IllegalArgumentException if some property of the specified
     *         element prevents it from being added to this deque
     */
    boolean offer(E e);

    /**
     * Retrieves and removes the head of the queue represented by this deque
     * (in other words, the first element of this deque).
     * This method differs from {@link #poll poll} only in that it throws an
     * exception if this deque is empty.
     *
     * <p>This method is equivalent to {@link #removeFirst()}.
     *
     * @return the head of the queue represented by this deque
     * @throws NoSuchElementException if this deque is empty
     */
    E remove();

    /**
     * Retrieves and removes the head of the queue represented by this deque
     * (in other words, the first element of this deque), or returns
     * {@code null} if this deque is empty.
     *
     * <p>This method is equivalent to {@link #pollFirst()}.
     *
     * @return the first element of this deque, or {@code null} if
     *         this deque is empty
     */
    E poll();

    /**
     * Retrieves, but does not remove, the head of the queue represented by
     * this deque (in other words, the first element of this deque).
     * This method differs from {@link #peek peek} only in that it throws an
     * exception if this deque is empty.
     *
     * <p>This method is equivalent to {@link #getFirst()}.
     *
     * @return the head of the queue represented by this deque
     * @throws NoSuchElementException if this deque is empty
     */
    E element();

    /**
     * Retrieves, but does not remove, the head of the queue represented by
     * this deque (in other words, the first element of this deque), or
     * returns {@code null} if this deque is empty.
     *
     * <p>This method is equivalent to {@link #peekFirst()}.
     *
     * @return the head of the queue represented by this deque, or
     *         {@code null} if this deque is empty
     */
    E peek();


    // *** Stack methods ***

    /**
     * Pushes an element onto the stack represented by this deque (in other
     * words, at the head of this deque) if it is possible to do so
     * immediately without violating capacity restrictions, throwing an
     * {@code IllegalStateException} if no space is currently available.
     *
     * <p>This method is equivalent to {@link #addFirst}.
     *
     * @param e the element to push
     * @throws IllegalStateException if the element cannot be added at this
     *         time due to capacity restrictions
     * @throws ClassCastException if the class of the specified element
     *         prevents it from being added to this deque
     * @throws NullPointerException if the specified element is null and this
     *         deque does not permit null elements
     * @throws IllegalArgumentException if some property of the specified
     *         element prevents it from being added to this deque
     */
    void push(E e);

    /**
     * Pops an element from the stack represented by this deque.  In other
     * words, removes and returns the first element of this deque.
     *
     * <p>This method is equivalent to {@link #removeFirst()}.
     *
     * @return the element at the front of this deque (which is the top
     *         of the stack represented by this deque)
     * @throws NoSuchElementException if this deque is empty
     */
    E pop();


    // *** Collection methods ***

    /**
     * Removes the first occurrence of the specified element from this deque.
     * If the deque does not contain the element, it is unchanged.
     * More formally, removes the first element {@code e} such that
     * <tt>(o==null&nbsp;?&nbsp;e==null&nbsp;:&nbsp;o.equals(e))</tt>
     * (if such an element exists).
     * Returns {@code true} if this deque contained the specified element
     * (or equivalently, if this deque changed as a result of the call).
     *
     * <p>This method is equivalent to {@link #removeFirstOccurrence(Object)}.
     *
     * @param o element to be removed from this deque, if present
     * @return {@code true} if an element was removed as a result of this call
     * @throws ClassCastException if the class of the specified element
     *         is incompatible with this deque
     * (<a href="Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException if the specified element is null and this
     *         deque does not permit null elements
     * (<a href="Collection.html#optional-restrictions">optional</a>)
     */
    boolean remove(Object o);

    /**
     * Returns {@code true} if this deque contains the specified element.
     * More formally, returns {@code true} if and only if this deque contains
     * at least one element {@code e} such that
     * <tt>(o==null&nbsp;?&nbsp;e==null&nbsp;:&nbsp;o.equals(e))</tt>.
     *
     * @param o element whose presence in this deque is to be tested
     * @return {@code true} if this deque contains the specified element
     * @throws ClassCastException if the type of the specified element
     *         is incompatible with this deque
     * (<a href="Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException if the specified element is null and this
     *         deque does not permit null elements
     * (<a href="Collection.html#optional-restrictions">optional</a>)
     */
    boolean contains(Object o);

    /**
     * Returns the number of elements in this deque.
     *
     * @return the number of elements in this deque
     */
    public int size();

    /**
     * Returns an iterator over the elements in this deque in proper sequence.
     * The elements will be returned in order from first (head) to last (tail).
     *
     * @return an iterator over the elements in this deque in proper sequence
     */
    Iterator<E> iterator();

    /**
     * Returns an iterator over the elements in this deque in reverse
     * sequential order.  The elements will be returned in order from
     * last (tail) to first (head).
     *
     * @return an iterator over the elements in this deque in reverse
     * sequence
     */
    Iterator<E> descendingIterator();

}
