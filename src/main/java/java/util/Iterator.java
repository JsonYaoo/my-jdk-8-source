/*
 * Copyright (c) 1997, 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.util.function.Consumer;

/**
 * An iterator over a collection.  {@code Iterator} takes the place of
 * {@link Enumeration} in the Java Collections Framework.  Iterators
 * differ from enumerations in two ways:
 *
 * <ul>
 *      <li> Iterators allow the caller to remove elements from the
 *           underlying collection during the iteration with well-defined
 *           semantics.
 *      <li> Method names have been improved.
 * </ul>
 *
 * <p>This interface is a member of the
 * <a href="{@docRoot}/../technotes/guides/collections/index.html">
 * Java Collections Framework</a>.
 *
 * @param <E> the type of elements returned by this iterator
 *
 * @author  Josh Bloch
 * @see Collection
 * @see ListIterator
 * @see Iterable
 * @since 1.2
 */
// 20201118 集合上的迭代器
public interface Iterator<E> {
    /**
     * 20201118
     * 如果此列表迭代器在向前遍历列表时有更多元素，则返回true。（换句话说，如果#next将返回元素而不是引发异常，则返回true。）
     */
    /**
     * Returns {@code true} if the iteration has more elements.
     * (In other words, returns {@code true} if {@link #next} would
     * return an element rather than throwing an exception.)
     *
     * @return {@code true} if the iteration has more elements
     */
    boolean hasNext();

    /**
     * Returns the next element in the iteration.
     *
     * @return the next element in the iteration
     * @throws NoSuchElementException if the iteration has no more elements
     */
    // 20201118 返回迭代中的下一个元素。
    E next();

    /**
     * 20201118
     * 从基础集合中移除此迭代器返回的最后一个元素（可选操作）。每次调用#next只能调用此方法一次。
     * 如果在迭代过程中以任何方式（而不是通过调用此方法）修改基础集合，则未指定迭代器的行为。
     */
    /**
     * Removes from the underlying collection the last element returned
     * by this iterator (optional operation).  This method can be called
     * only once per call to {@link #next}.  The behavior of an iterator
     * is unspecified if the underlying collection is modified while the
     * iteration is in progress in any way other than by calling this
     * method.
     *
     * @implSpec
     * The default implementation throws an instance of
     * {@link UnsupportedOperationException} and performs no other action.
     *
     * @throws UnsupportedOperationException if the {@code remove}
     *         operation is not supported by this iterator
     *
     * @throws IllegalStateException if the {@code next} method has not
     *         yet been called, or the {@code remove} method has already
     *         been called after the last call to the {@code next}
     *         method
     */
    // 20201118 删除迭代器最后一个元素(每次迭代时), 默认抛出异常
    default void remove() {
        throw new UnsupportedOperationException("remove");
    }

    /**
     * 20201118
     * 对剩余的每个元素执行给定的操作，直到所有元素都已处理或操作引发异常。如果指定了迭代顺序，则按迭代顺序执行操作。操作引发的异常被中继到调用方。
     */
    /**
     * Performs the given action for each remaining element until all elements
     * have been processed or the action throws an exception.  Actions are
     * performed in the order of iteration, if that order is specified.
     * Exceptions thrown by the action are relayed to the caller.
     *
     * @implSpec
     * <p>The default implementation behaves as if:
     * <pre>{@code
     *     while (hasNext())
     *         action.accept(next());
     * }</pre>
     *
     * @param action The action to be performed for each element
     * @throws NullPointerException if the specified action is null
     * @since 1.8
     */
    // 2020118 指定规则进行迭代元素
    default void forEachRemaining(Consumer<? super E> action) {
        // 20201118 空指针校验
        Objects.requireNonNull(action);

        // 20201118 如果有一个元素, 则使用指定规则遍历
        while (hasNext())
            action.accept(next());
    }
}
