/*
 * Copyright (c) 1994, 2005, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package java.util;

/**
 * 20210616
 * A. 实现 Enumeration 接口的对象一次生成一系列元素。 对nextElement方法的连续调用将返回系列的连续元素。
 * B. 例如，要打印 a 的所有元素 Vector<E> v:
 *      for (Enumeration<E> e = v.elements(); e.hasMoreElements();)
 *          System.out.println(e.nextElement());
 * C. 提供了方法来枚举向量的元素、哈希表的键和哈希表中的值。 枚举也用于指定SequenceInputStream的输入流。
 * D. 注意：此接口的功能由Iterator接口复制。此外，Iterator 添加了一个可选的删除操作，并具有较短的方法名称。新的实现应该考虑使用Iterator而不是Enumeration。
 */
/**
 * A.
 * An object that implements the Enumeration interface generates a
 * series of elements, one at a time. Successive calls to the
 * <code>nextElement</code> method return successive elements of the
 * series.
 *
 * B.
 * <p>
 * For example, to print all elements of a <tt>Vector&lt;E&gt;</tt> <i>v</i>:
 * <pre>
 *   for (Enumeration&lt;E&gt; e = v.elements(); e.hasMoreElements();)
 *       System.out.println(e.nextElement());</pre>
 * <p>
 *
 * C.
 * Methods are provided to enumerate through the elements of a
 * vector, the keys of a hashtable, and the values in a hashtable.
 * Enumerations are also used to specify the input streams to a
 * <code>SequenceInputStream</code>.
 *
 * D.
 * <p>
 * NOTE: The functionality of this interface is duplicated by the Iterator
 * interface.  In addition, Iterator adds an optional remove operation, and
 * has shorter method names.  New implementations should consider using
 * Iterator in preference to Enumeration.
 *
 * @see     java.util.Iterator
 * @see     java.io.SequenceInputStream
 * @see     java.util.Enumeration#nextElement()
 * @see     java.util.Hashtable
 * @see     java.util.Hashtable#elements()
 * @see     java.util.Hashtable#keys()
 * @see     java.util.Vector
 * @see     java.util.Vector#elements()
 *
 * @author  Lee Boynton
 * @since   JDK1.0
 */
public interface Enumeration<E> {
    /**
     * Tests if this enumeration contains more elements.
     *
     * // 20201205 当且仅当此枚举对象包含至少一个以上要提供的元素时，才为true；否则为false。
     * @return  <code>true</code> if and only if this enumeration object
     *           contains at least one more element to provide;
     *          <code>false</code> otherwise.
     */
    // 20201205 测试此枚举是否包含更多元素 -> 包含至少一个以上要提供的元素才为true
    boolean hasMoreElements();

    /**
     * Returns the next element of this enumeration if this enumeration
     * object has at least one more element to provide.
     *
     * @return     the next element of this enumeration. // 20201205 此枚举的下一个元素。
     * @exception  NoSuchElementException  if no more elements exist.
     */
    // 20201205 如果此枚举对象还有至少一个要提供的元素，则返回此枚举的下一个元素。
    E nextElement();
}
