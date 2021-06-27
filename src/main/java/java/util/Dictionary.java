/*
 * Copyright (c) 1995, 2004, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package java.util;

/**
 * 20210616
 * A. Dictionary 类是任何类的抽象父类，例如 Hashtable，它将键映射到值。 每个键和每个值都是一个对象。 在任何一个 Dictionary 对象中，每个键最多与一个值相关联。
 *    给定一个字典和一个键，可以查找相关的元素。 任何非null对象都可以用作键和值。
 * B. 通常，此类的实现应使用equals方法来确定两个键是否相同。
 * C. 注意：这个类已经过时了。 新的实现应该实现 Map 接口，而不是扩展这个类。
 */

/**
 * A.
 * The <code>Dictionary</code> class is the abstract parent of any
 * class, such as <code>Hashtable</code>, which maps keys to values.
 * Every key and every value is an object. In any one <tt>Dictionary</tt>
 * object, every key is associated with at most one value. Given a
 * <tt>Dictionary</tt> and a key, the associated element can be looked up.
 * Any non-<code>null</code> object can be used as a key and as a value.
 * <p>
 *
 * B.
 * As a rule, the <code>equals</code> method should be used by
 * implementations of this class to decide if two keys are the same.
 *
 * C.
 * <p>
 * <strong>NOTE: This class is obsolete.  New implementations should
 * implement the Map interface, rather than extending this class.</strong>
 *
 * @author  unascribed
 * @see     java.util.Map
 * @see     java.lang.Object#equals(java.lang.Object)
 * @see     java.lang.Object#hashCode()
 * @see     java.util.Hashtable
 * @since   JDK1.0
 */
public abstract class Dictionary<K,V> {

    /**
     * Sole constructor.  (For invocation by subclass constructors, typically implicit.)
     */
    // 唯一的构造函数。 （对于子类构造函数的调用，通常是隐式的。）
    public Dictionary() {
    }

    /**
     * Returns the number of entries (distinct keys) in this dictionary.
     *
     * @return  the number of keys in this dictionary.
     */
    // 返回此字典中的条目数（不同的键）。
    abstract public int size();

    /**
     * Tests if this dictionary maps no keys to value. The general contract
     * for the <tt>isEmpty</tt> method is that the result is true if and only
     * if this dictionary contains no entries.
     *
     * @return  <code>true</code> if this dictionary maps no keys to values;
     *          <code>false</code> otherwise.
     */
    // 测试此字典是否没有将键映射到值。 isEmpty 方法的一般约定是，当且仅当此字典不包含任何条目时，结果才为真。
    abstract public boolean isEmpty();

    /**
     * Returns an enumeration of the keys in this dictionary. The general
     * contract for the keys method is that an <tt>Enumeration</tt> object
     * is returned that will generate all the keys for which this dictionary
     * contains entries.
     *
     * @return  an enumeration of the keys in this dictionary.
     * @see     java.util.Dictionary#elements()
     * @see     java.util.Enumeration
     */
    // 返回此字典中键的枚举。 keys方法的一般约定是返回一个Enumeration对象，该对象将生成此字典包含条目的所有键。
    abstract public Enumeration<K> keys();

    /**
     * Returns an enumeration of the values in this dictionary. The general
     * contract for the <tt>elements</tt> method is that an
     * <tt>Enumeration</tt> is returned that will generate all the elements
     * contained in entries in this dictionary.
     *
     * @return  an enumeration of the values in this dictionary.
     * @see     java.util.Dictionary#keys()
     * @see     java.util.Enumeration
     */
    // 返回此字典中值的枚举。元素方法的一般约定是返回一个枚举，它将生成此字典中条目中包含的所有元素。
    abstract public Enumeration<V> elements();

    /**
     * Returns the value to which the key is mapped in this dictionary.
     * The general contract for the <tt>isEmpty</tt> method is that if this
     * dictionary contains an entry for the specified key, the associated
     * value is returned; otherwise, <tt>null</tt> is returned.
     *
     * @return  the value to which the key is mapped in this dictionary;
     * @param   key   a key in this dictionary.
     *          <code>null</code> if the key is not mapped to any value in
     *          this dictionary.
     * @exception NullPointerException if the <tt>key</tt> is <tt>null</tt>.
     * @see     java.util.Dictionary#put(java.lang.Object, java.lang.Object)
     */
    // 返回此字典中键映射到的值。 get方法的一般约定是，如果此字典包含指定键的条目，则返回关联的值； 否则，返回 null。
    abstract public V get(Object key);

    /**
     * 20210616
     * A. 将指定的键映射到此字典中的指定值。 键和值都不能为null。
     * B. 如果此字典已包含指定键的条目，则在修改条目以包含新元素后，将返回此字典中该键的已有值。 如果此字典还没有指定键的条目，则为指定的键和值创建一个条目，并返回 null。
     * C. 可以通过使用与原始键相等的键调用 get 方法来检索该值。
     */
    /**
     * A.
     * Maps the specified <code>key</code> to the specified
     * <code>value</code> in this dictionary. Neither the key nor the
     * value can be <code>null</code>.
     *
     * B.
     * <p>
     * If this dictionary already contains an entry for the specified
     * <tt>key</tt>, the value already in this dictionary for that
     * <tt>key</tt> is returned, after modifying the entry to contain the
     *  new element. <p>If this dictionary does not already have an entry
     *  for the specified <tt>key</tt>, an entry is created for the
     *  specified <tt>key</tt> and <tt>value</tt>, and <tt>null</tt> is
     *  returned.
     *
     * C.
     * <p>
     * The <code>value</code> can be retrieved by calling the
     * <code>get</code> method with a <code>key</code> that is equal to
     * the original <code>key</code>.
     *
     * @param      key     the hashtable key.
     * @param      value   the value.
     * @return     the previous value to which the <code>key</code> was mapped
     *             in this dictionary, or <code>null</code> if the key did not
     *             have a previous mapping.
     * @exception  NullPointerException  if the <code>key</code> or
     *               <code>value</code> is <code>null</code>.
     * @see        java.lang.Object#equals(java.lang.Object)
     * @see        java.util.Dictionary#get(java.lang.Object)
     */
    abstract public V put(K key, V value);

    /**
     * Removes the <code>key</code> (and its corresponding
     * <code>value</code>) from this dictionary. This method does nothing
     * if the <code>key</code> is not in this dictionary.
     *
     * @param   key   the key that needs to be removed.
     * @return  the value to which the <code>key</code> had been mapped in this
     *          dictionary, or <code>null</code> if the key did not have a
     *          mapping.
     * @exception NullPointerException if <tt>key</tt> is <tt>null</tt>.
     */
    // 从此字典中删除键（及其相应的值）。 如果键不在此字典中，则此方法不执行任何操作。
    abstract public V remove(Object key);
}
