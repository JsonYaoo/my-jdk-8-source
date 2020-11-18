/*
 * Copyright (c) 1997, 2011, Oracle and/or its affiliates. All rights reserved.
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

package java.util;

/**
 * // 20201117 列表迭代器, 允许程序员沿任一方向遍历列表, 在迭代过程中修改列表并获取迭代器在列表中的当前位置
 * // 20201117 ListIterator没有当前元素. 它的光标位置始终位于上一个代码调用返回的元素和下一个代码调用返回的元素之间
 * An iterator for lists that allows the programmer
 * to traverse the list in either direction, modify
 * the list during iteration, and obtain the iterator's
 * current position in the list. A {@code ListIterator}
 * has no current element; its <I>cursor position</I> always
 * lies between the element that would be returned by a call
 * to {@code previous()} and the element that would be
 * returned by a call to {@code next()}.
 * An iterator for a list of length {@code n} has {@code n+1} possible
 * cursor positions, as illustrated by the carets ({@code ^}) below:
 * <PRE>
 *                      Element(0)   Element(1)   Element(2)   ... Element(n-1)
 * cursor positions:  ^            ^            ^            ^                  ^
 * </PRE>
 * Note that the {@link #remove} and {@link #set(Object)} methods are
 * <i>not</i> defined in terms of the cursor position;  they are defined to
 * operate on the last element returned by a call to {@link #next} or
 * {@link #previous()}.
 *
 * <p>This interface is a member of the
 * <a href="{@docRoot}/../technotes/guides/collections/index.html">
 * Java Collections Framework</a>.
 *
 * @author  Josh Bloch
 * @see Collection
 * @see List
 * @see Iterator
 * @see Enumeration
 * @see List#listIterator()
 * @since   1.2
 */
// 2020117 实现了Iterator接口, 还增加了previous等方法、获取索引方法、添加&替换等方法的声明
public interface ListIterator<E> extends Iterator<E> {
    // Query Operations

    /**
     * Returns {@code true} if this list iterator has more elements when
     * traversing the list in the forward direction. (In other words,
     * returns {@code true} if {@link #next} would return an element rather
     * than throwing an exception.)
     *
     * @return {@code true} if the list iterator has more elements when
     *         traversing the list in the forward direction
     */
    // 20201118 如果此列表迭代器在向前遍历列表时有更多元素，则返回true。（换句话说，如果next返回元素而不是引发异常，则返回true）
    boolean hasNext();

    /**
     * 20201118
     * 返回列表中的下一个元素并前进光标位置。这个方法可以被反复调用来遍历列表，或者与{@link#previous}的调用混合在一起来回进行。
     * （注意，交替调用{@code next}和{@code previous}将重复返回同一元素。）
     */
    /**
     * Returns the next element in the list and advances the cursor position.
     * This method may be called repeatedly to iterate through the list,
     * or intermixed with calls to {@link #previous} to go back and forth.
     * (Note that alternating calls to {@code next} and {@code previous}
     * will return the same element repeatedly.)
     *
     * @return the next element in the list
     * @throws NoSuchElementException if the iteration has no next element
     */
    E next();

    /**
     * 20201118
     * 如果此列表迭代器在反向遍历列表时有更多元素，则返回{@code true}。（换句话说，如果{@link#previous}将返回元素而不是引发异常，则返回{@code true}。）
     */
    /**
     * Returns {@code true} if this list iterator has more elements when
     * traversing the list in the reverse direction.  (In other words,
     * returns {@code true} if {@link #previous} would return an element
     * rather than throwing an exception.)
     *
     * @return {@code true} if the list iterator has more elements when
     *         traversing the list in the reverse direction
     */
    boolean hasPrevious();

    /**
     * 20201118
     * 返回列表中的上一个元素并向后移动光标位置。可以反复调用此方法来向后迭代列表，也可以与对{@link#next}的调用混合在一起来回执行。
     * （注意，交替调用{@code next}和{@code previous}将重复返回同一元素。）
     */
    /**
     * Returns the previous element in the list and moves the cursor
     * position backwards.  This method may be called repeatedly to
     * iterate through the list backwards, or intermixed with calls to
     * {@link #next} to go back and forth.  (Note that alternating calls
     * to {@code next} and {@code previous} will return the same
     * element repeatedly.)
     *
     * @return the previous element in the list
     * @throws NoSuchElementException if the iteration has no previous
     *         element
     */
    E previous();

    /**
     * Returns the index of the element that would be returned by a
     * subsequent call to {@link #next}. (Returns list size if the list
     * iterator is at the end of the list.)
     *
     * @return the index of the element that would be returned by a
     *         subsequent call to {@code next}, or list size if the list
     *         iterator is at the end of the list
     */
    // 20201118 返回后续调用{@link#next}将返回的元素的索引。（如果列表迭代器位于列表末尾，则返回列表大小。）
    int nextIndex();

    /**
     * Returns the index of the element that would be returned by a
     * subsequent call to {@link #previous}. (Returns -1 if the list
     * iterator is at the beginning of the list.)
     *
     * @return the index of the element that would be returned by a
     *         subsequent call to {@code previous}, or -1 if the list
     *         iterator is at the beginning of the list
     */
    // 20201118 返回后续调用{@link#previous}将返回的元素的索引。（如果列表迭代器位于列表的开头，则返回-1。）
    int previousIndex();


    // Modification Operations

    /**
     * 20201118
     * 从列表中删除{@link#next}或{@link}previous}（可选操作）返回的最后一个元素。
     * 每次对{@code next}或{@code previous}的调用只能进行一次。
     * 只有在最后一次调用{@code next}或{@code previous}后未调用{@link}add}时，才可以执行此操作。
     */
    /**
     * Removes from the list the last element that was returned by {@link
     * #next} or {@link #previous} (optional operation).  This call can
     * only be made once per call to {@code next} or {@code previous}.
     * It can be made only if {@link #add} has not been
     * called after the last call to {@code next} or {@code previous}.
     *
     * @throws UnsupportedOperationException if the {@code remove}
     *         operation is not supported by this list iterator
     * @throws IllegalStateException if neither {@code next} nor
     *         {@code previous} have been called, or {@code remove} or
     *         {@code add} have been called after the last call to
     *         {@code next} or {@code previous}
     */
    void remove();

    /**
     * 20201118
     * 将{@link#next}或{@link#previous}返回的最后一个元素替换为指定的元素（可选操作）。
     * 只有在最后一次调用{@code next}或{@code previous}之后，没有调用{@link}和{@link}add}，才能进行此调用。
     */
    /**
     * Replaces the last element returned by {@link #next} or
     * {@link #previous} with the specified element (optional operation).
     * This call can be made only if neither {@link #remove} nor {@link
     * #add} have been called after the last call to {@code next} or
     * {@code previous}.
     *
     * @param e the element with which to replace the last element returned by
     *          {@code next} or {@code previous}
     * @throws UnsupportedOperationException if the {@code set} operation
     *         is not supported by this list iterator
     * @throws ClassCastException if the class of the specified element
     *         prevents it from being added to this list
     * @throws IllegalArgumentException if some aspect of the specified
     *         element prevents it from being added to this list
     * @throws IllegalStateException if neither {@code next} nor
     *         {@code previous} have been called, or {@code remove} or
     *         {@code add} have been called after the last call to
     *         {@code next} or {@code previous}
     */
    void set(E e);

    /**
     * 20201118
     * 在列表中插入指定的元素（可选操作）。元素插入在{@link#next}（如果有）将返回的元素之前，以及{@link#previous}（如果有）返回的元素之后。
     * （如果列表不包含元素，新元素将成为列表中唯一的元素。）新元素插入到隐式游标之前：对{@code next}的后续调用将不受影响，对{@code previous}的后续调用将返回新元素。
     * （此调用将acall返回给{@code nextIndex}或{@code previousIndex}的值增加一倍。）
     */
    /**
     * Inserts the specified element into the list (optional operation).
     * The element is inserted immediately before the element that
     * would be returned by {@link #next}, if any, and after the element
     * that would be returned by {@link #previous}, if any.  (If the
     * list contains no elements, the new element becomes the sole element
     * on the list.)  The new element is inserted before the implicit
     * cursor: a subsequent call to {@code next} would be unaffected, and a
     * subsequent call to {@code previous} would return the new element.
     * (This call increases by one the value that would be returned by a
     * call to {@code nextIndex} or {@code previousIndex}.)
     *
     * @param e the element to insert
     * @throws UnsupportedOperationException if the {@code add} method is
     *         not supported by this list iterator
     * @throws ClassCastException if the class of the specified element
     *         prevents it from being added to this list
     * @throws IllegalArgumentException if some aspect of this element
     *         prevents it from being added to this list
     */
    void add(E e);
}
