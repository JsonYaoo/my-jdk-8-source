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
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * // 20201117 List接口的可调整大小的数组实现. 实现所有可选的列表操作, 并允许所有元素, 包括null
 * // 20201117 除了实现List接口之外, 此类还提供一些方法来操纵内部用于存储列表的数组的大小(此类与Vector大致等效, 但它是不同步的)
 * Resizable-array implementation of the <tt>List</tt> interface.  Implements
 * all optional list operations, and permits all elements, including
 * <tt>null</tt>.  In addition to implementing the <tt>List</tt> interface,
 * this class provides methods to manipulate the size of the array that is
 * used internally to store the list.  (This class is roughly equivalent to
 * <tt>Vector</tt>, except that it is unsynchronized.)
 *
 * <p>The <tt>size</tt>, <tt>isEmpty</tt>, <tt>get</tt>, <tt>set</tt>,
 * <tt>iterator</tt>, and <tt>listIterator</tt> operations run in constant
 * time.  The <tt>add</tt> operation runs in <i>amortized constant time</i>,
 * that is, adding n elements requires O(n) time.  All of the other operations
 * run in linear time (roughly speaking).  The constant factor is low compared
 * to that for the <tt>LinkedList</tt> implementation.
 *
 * <p>Each <tt>ArrayList</tt> instance has a <i>capacity</i>.  The capacity is
 * the size of the array used to store the elements in the list.  It is always
 * at least as large as the list size.  As elements are added to an ArrayList,
 * its capacity grows automatically.  The details of the growth policy are not
 * specified beyond the fact that adding an element has constant amortized
 * time cost.
 *
 * <p>An application can increase the capacity of an <tt>ArrayList</tt> instance
 * before adding a large number of elements using the <tt>ensureCapacity</tt>
 * operation.  This may reduce the amount of incremental reallocation.
 *
 * <p><strong>Note that this implementation is not synchronized.</strong>
 * If multiple threads access an <tt>ArrayList</tt> instance concurrently,
 * and at least one of the threads modifies the list structurally, it
 * <i>must</i> be synchronized externally.  (A structural modification is
 * any operation that adds or deletes one or more elements, or explicitly
 * resizes the backing array; merely setting the value of an element is not
 * a structural modification.)  This is typically accomplished by
 * synchronizing on some object that naturally encapsulates the list.
 *
 * If no such object exists, the list should be "wrapped" using the
 * {@link Collections#synchronizedList Collections.synchronizedList}
 * method.  This is best done at creation time, to prevent accidental
 * unsynchronized access to the list:<pre>
 *   List list = Collections.synchronizedList(new ArrayList(...));</pre>
 *
 * <p><a name="fail-fast">
 * The iterators returned by this class's {@link #iterator() iterator} and
 * {@link #listIterator(int) listIterator} methods are <em>fail-fast</em>:</a>
 * if the list is structurally modified at any time after the iterator is
 * created, in any way except through the iterator's own
 * {@link ListIterator#remove() remove} or
 * {@link ListIterator#add(Object) add} methods, the iterator will throw a
 * {@link ConcurrentModificationException}.  Thus, in the face of
 * concurrent modification, the iterator fails quickly and cleanly, rather
 * than risking arbitrary, non-deterministic behavior at an undetermined
 * time in the future.
 *
 * <p>Note that the fail-fast behavior of an iterator cannot be guaranteed
 * as it is, generally speaking, impossible to make any hard guarantees in the
 * presence of unsynchronized concurrent modification.  Fail-fast iterators
 * throw {@code ConcurrentModificationException} on a best-effort basis.
 * Therefore, it would be wrong to write a program that depended on this
 * exception for its correctness:  <i>the fail-fast behavior of iterators
 * should be used only to detect bugs.</i>
 *
 * <p>This class is a member of the
 * <a href="{@docRoot}/../technotes/guides/collections/index.html">
 * Java Collections Framework</a>.
 *
 * @author  Josh Bloch
 * @author  Neal Gafter
 * @see     Collection
 * @see     List
 * @see     LinkedList
 * @see     Vector
 * @since   1.2
 */
// 20201117 实现了AbstractList, List接口, 允许随机访问、Clone调用、可序列化
public class ArrayList<E> extends AbstractList<E> implements List<E>, RandomAccess, Cloneable, java.io.Serializable
{
    private static final long serialVersionUID = 8683452581122892189L;

    /**
     * Default initial capacity.
     */
    // 20201117 默认了容量10个元素
    private static final int DEFAULT_CAPACITY = 10;

    /**
     * Shared empty array instance used for empty instances.
     */
    // 20201117 初始空数组
    private static final Object[] EMPTY_ELEMENTDATA = {};

    /**
     * Shared empty array instance used for default sized empty instances. We
     * distinguish this from EMPTY_ELEMENTDATA to know how much to inflate when
     * first element is added.
     */
    // 20201117 初始空数组, 将此与EMPTY_ELEMENTDATA区别开来, 以了解添加第一个元素时要填入多少
    private static final Object[] DEFAULTCAPACITY_EMPTY_ELEMENTDATA = {};

    /**
     * The array buffer into which the elements of the ArrayList are stored.
     * The capacity of the ArrayList is the length of this array buffer. Any
     * empty ArrayList with elementData == DEFAULTCAPACITY_EMPTY_ELEMENTDATA
     * will be expanded to DEFAULT_CAPACITY when the first element is added.
     */
    // 20201117 transient:当前属性不参与序列化(敏感信息) => 实际元素数组
    // 20201117 存储ArrayList的元素的数组缓冲区. ArrayList的容量是此数组缓冲区的长度.
    // 20201117 任何具有elementData == DEFAULTCAPACITY_EMPTY_ELEMENTDATA的空ArrayList, 添加第一个元素时, 将扩展为DEFAULT_CAPACITY
    transient Object[] elementData; // non-private to simplify nested class access // 20201117 非私有以简化嵌套类访问

    /**
     * The size of the ArrayList (the number of elements it contains).
     *
     * @serial
     */
    // 20201117 列表元素个数
    private int size;

    /**
     * Constructs an empty list with the specified initial capacity.
     *
     * @param  initialCapacity  the initial capacity of the list
     * @throws IllegalArgumentException if the specified initial capacity
     *         is negative
     */
    // 20201117 构造一个具有指定初始容量的空列表
    public ArrayList(int initialCapacity) {
        if (initialCapacity > 0) {
            // 20201117 初始化指定容量大小数组, 元素为{}
            this.elementData = new Object[initialCapacity];
        } else if (initialCapacity == 0) {
            // 20201117 初始化数组 = {}
            this.elementData = EMPTY_ELEMENTDATA;
        } else {
            // 20201117 参数非法异常
            throw new IllegalArgumentException("Illegal Capacity: "+
                                               initialCapacity);
        }
    }

    /**
     * Constructs an empty list with an initial capacity of ten.
     */
    // 20201117 构造一个初始容量为10的空列表
    public ArrayList() {
        // 20201117 初始化容量默认为10的空数组 = {}
        this.elementData = DEFAULTCAPACITY_EMPTY_ELEMENTDATA;
    }

    /**
     * Constructs a list containing the elements of the specified
     * collection, in the order they are returned by the collection's
     * iterator.
     *
     * @param c the collection whose elements are to be placed into this list
     * @throws NullPointerException if the specified collection is null
     */
    // 20201117 构造一个列表, 该列表包含指定集合的元素, 其顺序由集合的迭代器返回
    public ArrayList(Collection<? extends E> c) {
        // 20201117 多态性, 调用Collection接口的实现类方法 => 返回值Object[]类型 => 赋值给容器元素数组
        elementData = c.toArray();

        // 20201117 元素个数不为0
        if ((size = elementData.length) != 0) {
            // c.toArray might (incorrectly) not return Object[] (see 6260652)
            if (elementData.getClass() != Object[].class)
                // 20201117 如果不是Object类型, 则拷贝size个数组元素, 转换为Object类型存到elementData数组中来
                elementData = Arrays.copyOf(elementData, size, Object[].class);
        } else {
            // replace with empty array.
            // 20201117 如果元素个数为0, 则默认为{}
            this.elementData = EMPTY_ELEMENTDATA;
        }
    }

    /**
     * Trims the capacity of this <tt>ArrayList</tt> instance to be the
     * list's current size.  An application can use this operation to minimize
     * the storage of an <tt>ArrayList</tt> instance.
     */
    // 20201117 将此ArrayList实例的容量调整为列表的当前大小。 应用程序可以使用此操作来最小化ArrayList实例的存储。
    public void trimToSize() {
        // 20201117 AbstractList非序列化属性: 数组结构修改次数
        modCount++;// 2020117 结构修改次数+1

        // 20201117 如果list大小小于数组元素个数
        if (size < elementData.length) {
            // 20201117 如果list为0, 则初始化数组元素为{}, 否则返回指定size大小的数组元素
            elementData = (size == 0)
              ? EMPTY_ELEMENTDATA
              : Arrays.copyOf(elementData, size);
        }
    }

    /**
     * Increases the capacity of this <tt>ArrayList</tt> instance, if
     * necessary, to ensure that it can hold at least the number of elements
     * specified by the minimum capacity argument.
     *
     * @param   minCapacity   the desired minimum capacity
     */
    // 20201117 如有必要，增加此ArrayList实例的容量，以确保它至少可以容纳最小容量参数指定的元素数。
    public void ensureCapacity(int minCapacity) {
        // 20201117 如果元素数组为空, 则为0; 否则应该为默认容量 => 最小扩展数
        int minExpand = (elementData != DEFAULTCAPACITY_EMPTY_ELEMENTDATA)
            // any size if not default element table
            ? 0
            // larger than default for default empty table. It's already
            // supposed to be at default size.
            : DEFAULT_CAPACITY;

        // 20201117 如果指定容量大于最小扩展数
        if (minCapacity > minExpand) {
            // 20201117
            ensureExplicitCapacity(minCapacity);
        }
    }

    // 20201117 确认内部容量
    private void ensureCapacityInternal(int minCapacity) {
        // 20201117 如果元素数组为空数组{}
        if (elementData == DEFAULTCAPACITY_EMPTY_ELEMENTDATA) {
            // 20201117 则设置最小容量, 默认为10, 否则为指定容量大小
            minCapacity = Math.max(DEFAULT_CAPACITY, minCapacity);
        }

        // 20201117 确定下一个容量
        ensureExplicitCapacity(minCapacity);
    }

    // 20201117 确定明确的容量 -> 容量增加为下一个规则容量
    private void ensureExplicitCapacity(int minCapacity) {
        modCount++;// 20201117 数组结构修改次数

        // overflow-conscious code
        // 20201117 指定的最小容量大于数组元素个数时
        if (minCapacity - elementData.length > 0)
            // 20201117 指定列表数组增长到指定容量的数组, 多余的为null
            grow(minCapacity);
    }

    /**
     * The maximum size of array to allocate.
     * Some VMs reserve some header words in an array.
     * Attempts to allocate larger arrays may result in
     * OutOfMemoryError: Requested array size exceeds VM limit
     */
    // 20201117 要分配的最大数组大小。 一些虚拟机在数组中保留一些头字。 尝试分配更大的阵列可能会导致OutOfMemoryError：请求的阵列大小超出VM限制
    // 20201117 列表最大长度 = 0x7fffffff - 0x0000001000
    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

    /**
     * Increases the capacity to ensure that it can hold at least the
     * number of elements specified by the minimum capacity argument.
     *
     * @param minCapacity the desired minimum capacity
     */
    // 20201117 增加容量以确保其至少可以容纳最小容量参数指定的元素数。
    // 20201117 指定列表数组增长到指定容量的数组, 多余的为null
    private void grow(int minCapacity) {
        // overflow-conscious code // 20201117 有溢出意识的代码
        // 20201117 旧容量为旧的数组元素大小
        int oldCapacity = elementData.length;

        // 20201117 新容量 = 旧容量 + 旧容量/2 = 旧容量 * (1 + 1/2) => 增加的容量为原始容量的一半
        int newCapacity = oldCapacity + (oldCapacity >> 1);

        // 20201117 如果新容量小于旧容量
        if (newCapacity - minCapacity < 0)
            // 20201117 则新容量 = 旧容量
            newCapacity = minCapacity;

        // 20201117 如果新容量超出最大容量
        if (newCapacity - MAX_ARRAY_SIZE > 0)
            // 20201117 则获取列表最大长度
            newCapacity = hugeCapacity(minCapacity);
        // minCapacity is usually close to size, so this is a win:
        // 20201117 创建指定容量的数组, 并且拷贝旧的数组过去, 多余的为null
        elementData = Arrays.copyOf(elementData, newCapacity);
    }

    // 2020117 获取最大列表长度
    private static int hugeCapacity(int minCapacity) {
        // 20201117 如果指定容量小于0, 则报内存溢出
        if (minCapacity < 0) // overflow
            throw new OutOfMemoryError();

        // 20201117 如果大于列表最大长度, 则为0x7fffffff, 否则为列表最大长度
        return (minCapacity > MAX_ARRAY_SIZE) ?
            Integer.MAX_VALUE :
            MAX_ARRAY_SIZE;
    }

    /**
     * Returns the number of elements in this list.
     *
     * @return the number of elements in this list
     */
    // 20201117 返回此列表中的元素数。
    public int size() {
        return size;// 20201117 列表元素个数
    }

    /**
     * Returns <tt>true</tt> if this list contains no elements.
     *
     * @return <tt>true</tt> if this list contains no elements
     */
    // 20201117 判断列表元素个数是否为0
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Returns <tt>true</tt> if this list contains the specified element.
     * More formally, returns <tt>true</tt> if and only if this list contains
     * at least one element <tt>e</tt> such that
     * <tt>(o==null&nbsp;?&nbsp;e==null&nbsp;:&nbsp;o.equals(e))</tt>.
     *
     * @param o element whose presence in this list is to be tested
     * @return <tt>true</tt> if this list contains the specified element
     */
    // 20201117 如果此列表包含指定的元素，则返回true。 更正式地说，当且仅当此列表包含至少一个这样的元素时，才返回true。
    public boolean contains(Object o) {
        // 20201117 返回-1代表找不到, 此时为false; 找得到索引就>=0, 此时返回true
        return indexOf(o) >= 0;
    }

    /**
     * Returns the index of the first occurrence of the specified element
     * in this list, or -1 if this list does not contain the element.
     * More formally, returns the lowest index <tt>i</tt> such that
     * <tt>(o==null&nbsp;?&nbsp;get(i)==null&nbsp;:&nbsp;o.equals(get(i)))</tt>,
     * or -1 if there is no such index.
     */
    // 20201117 返回指定元素在此列表中首次出现的索引；如果此列表不包含该元素，则返回-1。 更正式地，返回最小的索引i
    public int indexOf(Object o) {
        // 20201117 如果传入的对象为null
        if (o == null) {
            // 20201117 则遍历列表, 找到第一个null元素, 返回其下标
            for (int i = 0; i < size; i++)
                if (elementData[i]==null)
                    return i;
        } else {
            // 20201117 如果传入的对象不为null, 则遍历找到该对象, 注意的是使用equals去判断, 代表的是同一个实例
            for (int i = 0; i < size; i++)
                if (o.equals(elementData[i]))
                    return i;// 20201117 找到则返回索引
        }
        return -1;// 20201117 找不到返回-1
    }

    /**
     * Returns the index of the last occurrence of the specified element
     * in this list, or -1 if this list does not contain the element.
     * More formally, returns the highest index <tt>i</tt> such that
     * <tt>(o==null&nbsp;?&nbsp;get(i)==null&nbsp;:&nbsp;o.equals(get(i)))</tt>,
     * or -1 if there is no such index.
     */
    // 20201117 返回指定元素在此列表中最后一次出现的索引；如果此列表不包含该元素，则返回-1。 更正式地讲，返回最大索引i，如果没有这样的索引，则返回-1。
    public int lastIndexOf(Object o) {
        // 20201117 如果该对象为null
        if (o == null) {
            // 20201117 则遍历查找null对象
            for (int i = size-1; i >= 0; i--)
                // 20201117 找到则返回索引
                if (elementData[i]==null)
                    return i;
        } else {
            // 2020117 如果该对象不为null, 则遍历查找这个对象, 使用equals判断同一个实例
            for (int i = size-1; i >= 0; i--)
                if (o.equals(elementData[i]))
                    return i;// 20201117 找到则返回索引
        }
        return -1;// 20201117 找不到则返回-1
    }

    /**
     * Returns a shallow copy of this <tt>ArrayList</tt> instance.  (The
     * elements themselves are not copied.)
     *
     * @return a clone of this <tt>ArrayList</tt> instance
     */
    // 20201117 返回此ArrayList实例的浅表副本。 （元素本身不会被复制。）
    public Object clone() {
        try {
            // 20201117 复制列表实例
            ArrayList<?> v = (ArrayList<?>) super.clone();

            // 20201117 设置新列表的元素
            v.elementData = Arrays.copyOf(elementData, size);

            // 20201117 初始化数组结构修改次数
            v.modCount = 0;
            return v;// 20201117 返回新列表引用
        } catch (CloneNotSupportedException e) {
            // this shouldn't happen, since we are Cloneable
            throw new InternalError(e);
        }
    }

    /**
     * Returns an array containing all of the elements in this list
     * in proper sequence (from first to last element).
     *
     * <p>The returned array will be "safe" in that no references to it are
     * maintained by this list.  (In other words, this method must allocate
     * a new array).  The caller is thus free to modify the returned array.
     *
     * <p>This method acts as bridge between array-based and collection-based
     * APIs.
     *
     * @return an array containing all of the elements in this list in
     *         proper sequence
     */
    // 20201117 以正确的顺序（从第一个元素到最后一个元素）返回包含此列表中所有元素的数组。
    public Object[] toArray() {
        // 2020117 返回size长度的元素Object类型数组副本
        return Arrays.copyOf(elementData, size);
    }

    /**
     * 20201117
     * 返回一个数组，该数组按适当顺序（从第一个元素到最后一个元素）包含此列表中的所有元素；
     * 返回数组的运行时类型是指定数组的运行时类型。 如果列表适合指定的数组，则将其返回。
     * 否则，将使用指定数组的运行时类型和此列表的大小分配一个新数组。
     */
    /**
     * Returns an array containing all of the elements in this list in proper
     * sequence (from first to last element); the runtime type of the returned
     * array is that of the specified array.  If the list fits in the
     * specified array, it is returned therein.  Otherwise, a new array is
     * allocated with the runtime type of the specified array and the size of
     * this list.
     *
     * <p>If the list fits in the specified array with room to spare
     * (i.e., the array has more elements than the list), the element in
     * the array immediately following the end of the collection is set to
     * <tt>null</tt>.  (This is useful in determining the length of the
     * list <i>only</i> if the caller knows that the list does not contain
     * any null elements.)
     *
     * @param a the array into which the elements of the list are to
     *          be stored, if it is big enough; otherwise, a new array of the
     *          same runtime type is allocated for this purpose.
     * @return an array containing the elements of the list
     * @throws ArrayStoreException if the runtime type of the specified array
     *         is not a supertype of the runtime type of every element in
     *         this list
     * @throws NullPointerException if the specified array is null
     */
    // 20201117 返回指定类型的数组
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] a) {
        // 20201117 如果该数组长度小于列表长度
        if (a.length < size)
            // 20201117 则根据类型返回size长度的数组, 其中已经对元素进行复制了
            // Make a new array of a's runtime type, but my contents:
            return (T[]) Arrays.copyOf(elementData, size, a.getClass());

        // 20201117 如果该数组长度不小于列表长度, 则对其元素进行复制
        System.arraycopy(elementData, 0, a, 0, size);

        // 2020117 如果该数组还有剩余元素, 则设置为null
        if (a.length > size)
            a[size] = null;
        return a;
    }

    // Positional Access Operations

    @SuppressWarnings("unchecked")// 20201117 告诉编译器忽略 unchecked 警告信息，如使用List，ArrayList等未进行参数化产生的警告信息。
    // 20201117 default修饰的方法 -> 根据索引返回列表数组中的元素
    E elementData(int index) {
        return (E) elementData[index];
    }

    /**
     * Returns the element at the specified position in this list.
     *
     * @param  index index of the element to return
     * @return the element at the specified position in this list
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    // 20201117 返回此列表中指定位置的元素。
    public E get(int index) {
        // 2020117 数组越界检查
        rangeCheck(index);

        // 2020117 根据索引返回元素数组中的元素
        return elementData(index);
    }

    /**
     * Replaces the element at the specified position in this list with
     * the specified element.
     *
     * @param index index of the element to replace
     * @param element element to be stored at the specified position
     * @return the element previously at the specified position
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    // 20201117 用指定的元素替换此列表中指定位置的元素。
    public E set(int index, E element) {
        // 20201117 索引越界检查
        rangeCheck(index);

        // 20201117 根据索引获取元素数组的值
        E oldValue = elementData(index);

        // 20201117 根据索引设置元素数组的值
        elementData[index] = element;

        //20201117 返回旧值
        return oldValue;
    }

    /**
     * Appends the specified element to the end of this list.
     *
     * @param e element to be appended to this list
     * @return <tt>true</tt> (as specified by {@link Collection#add})
     */
    // 20201117 将指定的元素追加到此列表的末尾。
    public boolean add(E e) {
        // 20201117 指定容量为size+1, 确认下一个容量, 并增长元素数组到那个容量
        ensureCapacityInternal(size + 1);  // Increments modCount!! // 20201117 结构修改次数增加

        // 20201117 元素数组设置值, 元素实际个数+1
        elementData[size++] = e;

        // 20201117 添加成功返回true
        return true;
    }

    /**
     * Inserts the specified element at the specified position in this
     * list. Shifts the element currently at that position (if any) and
     * any subsequent elements to the right (adds one to their indices).
     *
     * @param index index at which the specified element is to be inserted
     * @param element element to be inserted
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    // 20201117 将指定的元素插入此列表中的指定位置。 将当前在该位置的元素（如果有）和任何后续元素右移（将其索引加一）。
    public void add(int index, E element) {
        // 20201117 索引越界校验
        rangeCheckForAdd(index);

        // 20201117 根据size+1确认下一个容量, 并设置元素数组等于那个容量, 且结构修改次数+1
        ensureCapacityInternal(size + 1);  // Increments modCount!!

        // 20201117 调用native方法对元素数组进行index位置的元素右移操作
        System.arraycopy(elementData, index, elementData, index + 1,
                         size - index);

        // 20201117 元素数组index位置赋值, 并且实际元素个数+1
        elementData[index] = element;
        size++;
    }

    /**
     * Removes the element at the specified position in this list.
     * Shifts any subsequent elements to the left (subtracts one from their
     * indices).
     *
     * @param index the index of the element to be removed
     * @return the element that was removed from the list
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    // 20201117 删除此列表中指定位置的元素。 将所有后续元素向左移动（从其索引中减去一个）。
    public E remove(int index) {
        // 20201117 索引越界校验
        rangeCheck(index);

        // 20201117 结构修改次数+1
        modCount++;

        // 20201117 根据索引获取元素数组元素
        E oldValue = elementData(index);

        // 20201117 获取index后有几个元素
        int numMoved = size - index - 1;

        // 20201117 如果有>0个
        if (numMoved > 0)
            // 2020117 则对index+1处的元素进行左移
            System.arraycopy(elementData, index+1, elementData, index,
                             numMoved);

        // 20201117 置空最后一个元素, 并且实际元素个数-1
        elementData[--size] = null; // clear to let GC do its work // 通知GC回收

        // 20201117 返回旧值
        return oldValue;
    }

    /**
     * 20201117
     * 如果存在指定元素，则从该列表中删除该元素的第一次出现。如果列表不包含该元素，则该元素不变。
     * 更正式地讲，删除索引i最低的元素（如果存在这样的元素）。 如果此列表包含指定的元素，则返回true
     */
    /**
     * Removes the first occurrence of the specified element from this list,
     * if it is present.  If the list does not contain the element, it is
     * unchanged.  More formally, removes the element with the lowest index
     * <tt>i</tt> such that
     * <tt>(o==null&nbsp;?&nbsp;get(i)==null&nbsp;:&nbsp;o.equals(get(i)))</tt>
     * (if such an element exists).  Returns <tt>true</tt> if this list
     * contained the specified element (or equivalently, if this list
     * changed as a result of the call).
     *
     * @param o element to be removed from this list, if present
     * @return <tt>true</tt> if this list contained the specified element
     */
    // 20201117 根据实例删除列表元素
    public boolean remove(Object o) {
        // 20201117 如果该对象为null
        if (o == null) {
            // 2020117 则遍历查找元素数组
            for (int index = 0; index < size; index++)
                // 20201117 如果找到元素为null
                if (elementData[index] == null) {
                    fastRemove(index);// 20201117 则快速删除该元素, 没有返回值
                    return true;// 20201117 删除成功, 返回true
                }
        } else {
            // 20201117 如果该对象不为null, 则遍历元素数组
            for (int index = 0; index < size; index++)
                // 20201117 如果找到该实例(同一个)
                if (o.equals(elementData[index])) {
                    // 20201117 同样, 快速删除该元素, 删除成功返回true
                    fastRemove(index);
                    return true;
                }
        }

        // 20201117 如果找不到, 则删除失败返回fasle
        return false;
    }

    /*
     * Private remove method that skips bounds checking and does not
     * return the value removed.
     */
    // 20201117 专用的remove方法，跳过边界检查，并且不返回已删除的值。
    private void fastRemove(int index) {
        // 20201117 结构修改次数+1
        modCount++;

        // 20201117 获取index后的元素个数
        int numMoved = size - index - 1;

        // 20201117 如果元素个数>0
        if (numMoved > 0)
            // 20201117 则调用native方法对index+1后的元素进行左移
            System.arraycopy(elementData, index+1, elementData, index,
                             numMoved);

        // 20201117 置空最后一个元素, 通知GC回收, 并且实际元素个数-1
        elementData[--size] = null; // clear to let GC do its work
    }

    /**
     * Removes all of the elements from this list.  The list will
     * be empty after this call returns.
     */
    // 20201117 从此列表中删除所有元素。 该调用返回后，该列表将为空。
    public void clear() {
        // 20201117 结构修改次数+1
        modCount++;

        // clear to let GC do its work
        // 20201117 遍历清空元素数组, 通知GC回收
        for (int i = 0; i < size; i++)
            elementData[i] = null;

        // 20201117 重置实际元素大小为0
        size = 0;
    }

    /**
     * 20201117
     * 按照指定集合的Iterator返回的顺序，将指定集合中的所有元素追加到此列表的末尾。
     * 如果在操作进行过程中修改了指定的集合，则此操作的行为是不确定的。 （这意味着如果指定的集合是此列表，并且此列表是非空的，则此调用的行为是不确定的。）
     */
    /**
     * Appends all of the elements in the specified collection to the end of
     * this list, in the order that they are returned by the
     * specified collection's Iterator.  The behavior of this operation is
     * undefined if the specified collection is modified while the operation
     * is in progress.  (This implies that the behavior of this call is
     * undefined if the specified collection is this list, and this
     * list is nonempty.)
     *
     * @param c collection containing elements to be added to this list
     * @return <tt>true</tt> if this list changed as a result of the call
     * @throws NullPointerException if the specified collection is null
     */
    // 20201117 遍历添加列表参数中的元素
    public boolean addAll(Collection<? extends E> c) {
        // 20201117 获取目标列表的Object类型数组
        Object[] a = c.toArray();

        // 20201117 获取目标数组的长度
        int numNew = a.length;

        // 20201117 根据size+目标数组长度确定下一个容量, 并且增长元素数组到那个容量大小, 且结构修改次数+1
        ensureCapacityInternal(size + numNew);  // Increments modCount

        // 20201117 调用本地方法在元素数组末尾添加目标数组
        System.arraycopy(a, 0, elementData, size, numNew);

        // 20201117 实际元素个数更i性能
        size += numNew;

        // 20201117 如果目标数组长度不为0, 则返回true
        return numNew != 0;
    }

    /**
     * 20201117
     * 从指定位置开始，将指定集合中的所有元素插入此列表。 将当前在该位置的元素（如果有）和任何后续元素右移（增加其索引）。
     * 新元素将按照指定集合的迭代器返回的顺序显示在列表中。
     */
    /**
     * Inserts all of the elements in the specified collection into this
     * list, starting at the specified position.  Shifts the element
     * currently at that position (if any) and any subsequent elements to
     * the right (increases their indices).  The new elements will appear
     * in the list in the order that they are returned by the
     * specified collection's iterator.
     *
     * @param index index at which to insert the first element from the
     *              specified collection
     * @param c collection containing elements to be added to this list
     * @return <tt>true</tt> if this list changed as a result of the call
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws NullPointerException if the specified collection is null
     */
    // 20201117 添加集合中所有元素到指定位置
    public boolean addAll(int index, Collection<? extends E> c) {
        // 20201117 索引越界校验
        rangeCheckForAdd(index);

        // 20201117 获取目标数组
        Object[] a = c.toArray();

        // 20201117 获取目标数组大小
        int numNew = a.length;

        // 20201117 根据新size确定下一个容量, 并且扩容元素数组以及结构次数+1
        ensureCapacityInternal(size + numNew);  // Increments modCount

        // 20201117 获取index后面元素个数
        int numMoved = size - index;

        // 20201117 如果元素个数>0
        if (numMoved > 0)
            // 20201117 调用native方法对index后面的元素右移
            System.arraycopy(elementData, index, elementData, index + numNew,
                             numMoved);

        // 20201117 如果元素个数为0, 则直接将元素添加到数组开始的地方索引0的位置
        System.arraycopy(a, 0, elementData, index, numNew);

        // 20201117 更新实际元素个数
        size += numNew;

        // 20201117 如果目标数组长度不为0, 则返回true
        return numNew != 0;
    }

    /**
     * 20201117
     * 从此列表中删除索引在from，inclusive和toIndex Exclusive之间的所有元素。
     * 将所有后续元素向左移动（减少其索引）。 此调用通过（toIndex-fromIndex）元素来缩短列表。 （如果toIndex == fromIndex，则此操作无效。）
     */
    /**
     * Removes from this list all of the elements whose index is between
     * {@code fromIndex}, inclusive, and {@code toIndex}, exclusive.
     * Shifts any succeeding elements to the left (reduces their index).
     * This call shortens the list by {@code (toIndex - fromIndex)} elements.
     * (If {@code toIndex==fromIndex}, this operation has no effect.)
     *
     * @throws IndexOutOfBoundsException if {@code fromIndex} or
     *         {@code toIndex} is out of range
     *         ({@code fromIndex < 0 ||
     *          fromIndex >= size() ||
     *          toIndex > size() ||
     *          toIndex < fromIndex})
     */
    // 20201117 指定索引范围删除列表元素
    protected void removeRange(int fromIndex, int toIndex) {
        // 20201117 结构修改次数+1
        modCount++;

        // 20201117 获取index之后的元素个数
        int numMoved = size - toIndex;

        // 20201117 调用native方法对toIndex后的元素左移到fromIndex处
        System.arraycopy(elementData, toIndex, elementData, fromIndex,
                         numMoved);

        // clear to let GC do its work
        // 20201117 遍历置空后面多余的元素, 通知GC回收
        int newSize = size - (toIndex-fromIndex);
        for (int i = newSize; i < size; i++) {
            elementData[i] = null;
        }

        // 20201117 更新实际元素个数
        size = newSize;
    }

    /**
     * Checks if the given index is in range.  If not, throws an appropriate
     * runtime exception.  This method does *not* check if the index is
     * negative: It is always used immediately prior to an array access,
     * which throws an ArrayIndexOutOfBoundsException if index is negative.
     */
    // 20201117 检查给定的索引是否在范围内。 如果不是，则抛出适当的运行时异常。
    // 20201117 此方法不检查索引是否为负：始终在数组访问之前立即使用它，如果索引为负，则抛出ArrayIndexOutOfBoundsException。
    // 20201117 索引越界检查
    private void rangeCheck(int index) {
        // 20201117 如果索引大于列表大小时, 则抛出索引越界异常
        if (index >= size)
            throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
    }

    /**
     * A version of rangeCheck used by add and addAll.
     */
    // 20201117 add和addAll使用的rangeCheck版本
    private void rangeCheckForAdd(int index) {
        // 20201117 如果索引大于列表长度 或者 <0, 则抛出索引越界异常
        if (index > size || index < 0)
            throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
    }

    /**
     * Constructs an IndexOutOfBoundsException detail message.
     * Of the many possible refactorings of the error handling code,
     * this "outlining" performs best with both server and client VMs.
     */
    // 20201117 构造一个IndexOutOfBoundsException详细信息。 在错误处理代码的许多可能重构中，此“概述”在服务器和客户端VM上均表现最佳。
    // 20201117 => index: 2, size: 1
    private String outOfBoundsMsg(int index) {
        return "Index: "+index+", Size: "+size;
    }

    /**
     * Removes from this list all of its elements that are contained in the
     * specified collection.
     *
     * @param c collection containing elements to be removed from this list
     * @return {@code true} if this list changed as a result of the call
     * @throws ClassCastException if the class of an element of this list
     *         is incompatible with the specified collection
     * (<a href="Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException if this list contains a null element and the
     *         specified collection does not permit null elements
     * (<a href="Collection.html#optional-restrictions">optional</a>),
     *         or if the specified collection is null
     * @see Collection#contains(Object)
     */
    // 20201117 从此列表中删除指定集合中包含的所有元素。
    public boolean removeAll(Collection<?> c) {
        // 20201117 对指定的列表进行校验, 如果有元素为null, 则抛出空指针异常
        Objects.requireNonNull(c);
        return batchRemove(c, false);
    }

    /**
     * Retains only the elements in this list that are contained in the
     * specified collection.  In other words, removes from this list all
     * of its elements that are not contained in the specified collection.
     *
     * @param c collection containing elements to be retained in this list
     * @return {@code true} if this list changed as a result of the call
     * @throws ClassCastException if the class of an element of this list
     *         is incompatible with the specified collection
     * (<a href="Collection.html#optional-restrictions">optional</a>)
     * @throws NullPointerException if this list contains a null element and the
     *         specified collection does not permit null elements
     * (<a href="Collection.html#optional-restrictions">optional</a>),
     *         or if the specified collection is null
     * @see Collection#contains(Object)
     */
    public boolean retainAll(Collection<?> c) {
        Objects.requireNonNull(c);
        return batchRemove(c, true);
    }

    // 20201117 根据指定列表进行批量删除
    private boolean batchRemove(Collection<?> c, boolean complement) {
        // 20201117 获取元素数组
        final Object[] elementData = this.elementData;

        // 20201117 初始化索引
        int r = 0, w = 0;

        // 20201117 默认为没修改
        boolean modified = false;
        try {
            // 20201117 遍历元素数组
            for (; r < size; r++)
                // 20201117 如果元素是否存在于列表中的结果 == 指定的结果
                if (c.contains(elementData[r]) == complement)
                    // 20201117 则将该元素赋值到w位置
                    elementData[w++] = elementData[r];
        } finally {
            // Preserve behavioral compatibility with AbstractCollection,
            // even if c.contains() throws.
            // 20201117 即使c.contains（）抛出异常，仍保留与AbstractCollection的行为兼容性。
            // 20201117 在结束之前, 如果元素数组没有遍历完
            if (r != size) {
                // 20201117 则
                System.arraycopy(elementData, r,
                                 elementData, w,
                                 size - r);
                w += size - r;
            }
            if (w != size) {
                // clear to let GC do its work
                for (int i = w; i < size; i++)
                    elementData[i] = null;
                modCount += size - w;
                size = w;
                modified = true;
            }
        }
        return modified;
    }

    /**
     * Save the state of the <tt>ArrayList</tt> instance to a stream (that
     * is, serialize it).
     *
     * @serialData The length of the array backing the <tt>ArrayList</tt>
     *             instance is emitted (int), followed by all of its elements
     *             (each an <tt>Object</tt>) in the proper order.
     */
    private void writeObject(java.io.ObjectOutputStream s)
        throws java.io.IOException{
        // Write out element count, and any hidden stuff
        int expectedModCount = modCount;
        s.defaultWriteObject();

        // Write out size as capacity for behavioural compatibility with clone()
        s.writeInt(size);

        // Write out all elements in the proper order.
        for (int i=0; i<size; i++) {
            s.writeObject(elementData[i]);
        }

        if (modCount != expectedModCount) {
            throw new ConcurrentModificationException();
        }
    }

    /**
     * Reconstitute the <tt>ArrayList</tt> instance from a stream (that is,
     * deserialize it).
     */
    private void readObject(java.io.ObjectInputStream s)
        throws java.io.IOException, ClassNotFoundException {
        elementData = EMPTY_ELEMENTDATA;

        // Read in size, and any hidden stuff
        s.defaultReadObject();

        // Read in capacity
        s.readInt(); // ignored

        if (size > 0) {
            // be like clone(), allocate array based upon size not capacity
            ensureCapacityInternal(size);

            Object[] a = elementData;
            // Read in all elements in the proper order.
            for (int i=0; i<size; i++) {
                a[i] = s.readObject();
            }
        }
    }

    /**
     * Returns a list iterator over the elements in this list (in proper
     * sequence), starting at the specified position in the list.
     * The specified index indicates the first element that would be
     * returned by an initial call to {@link ListIterator#next next}.
     * An initial call to {@link ListIterator#previous previous} would
     * return the element with the specified index minus one.
     *
     * <p>The returned list iterator is <a href="#fail-fast"><i>fail-fast</i></a>.
     *
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    public ListIterator<E> listIterator(int index) {
        if (index < 0 || index > size)
            throw new IndexOutOfBoundsException("Index: "+index);
        return new ListItr(index);
    }

    /**
     * Returns a list iterator over the elements in this list (in proper
     * sequence).
     *
     * <p>The returned list iterator is <a href="#fail-fast"><i>fail-fast</i></a>.
     *
     * @see #listIterator(int)
     */
    public ListIterator<E> listIterator() {
        return new ListItr(0);
    }

    /**
     * Returns an iterator over the elements in this list in proper sequence.
     *
     * <p>The returned iterator is <a href="#fail-fast"><i>fail-fast</i></a>.
     *
     * @return an iterator over the elements in this list in proper sequence
     */
    public Iterator<E> iterator() {
        return new Itr();
    }

    /**
     * An optimized version of AbstractList.Itr
     */
    private class Itr implements Iterator<E> {
        int cursor;       // index of next element to return
        int lastRet = -1; // index of last element returned; -1 if no such
        int expectedModCount = modCount;

        public boolean hasNext() {
            return cursor != size;
        }

        @SuppressWarnings("unchecked")
        public E next() {
            checkForComodification();
            int i = cursor;
            if (i >= size)
                throw new NoSuchElementException();
            Object[] elementData = ArrayList.this.elementData;
            if (i >= elementData.length)
                throw new ConcurrentModificationException();
            cursor = i + 1;
            return (E) elementData[lastRet = i];
        }

        public void remove() {
            if (lastRet < 0)
                throw new IllegalStateException();
            checkForComodification();

            try {
                ArrayList.this.remove(lastRet);
                cursor = lastRet;
                lastRet = -1;
                expectedModCount = modCount;
            } catch (IndexOutOfBoundsException ex) {
                throw new ConcurrentModificationException();
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public void forEachRemaining(Consumer<? super E> consumer) {
            Objects.requireNonNull(consumer);
            final int size = ArrayList.this.size;
            int i = cursor;
            if (i >= size) {
                return;
            }
            final Object[] elementData = ArrayList.this.elementData;
            if (i >= elementData.length) {
                throw new ConcurrentModificationException();
            }
            while (i != size && modCount == expectedModCount) {
                consumer.accept((E) elementData[i++]);
            }
            // update once at end of iteration to reduce heap write traffic
            cursor = i;
            lastRet = i - 1;
            checkForComodification();
        }

        final void checkForComodification() {
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
        }
    }

    /**
     * An optimized version of AbstractList.ListItr
     */
    private class ListItr extends Itr implements ListIterator<E> {
        ListItr(int index) {
            super();
            cursor = index;
        }

        public boolean hasPrevious() {
            return cursor != 0;
        }

        public int nextIndex() {
            return cursor;
        }

        public int previousIndex() {
            return cursor - 1;
        }

        @SuppressWarnings("unchecked")
        public E previous() {
            checkForComodification();
            int i = cursor - 1;
            if (i < 0)
                throw new NoSuchElementException();
            Object[] elementData = ArrayList.this.elementData;
            if (i >= elementData.length)
                throw new ConcurrentModificationException();
            cursor = i;
            return (E) elementData[lastRet = i];
        }

        public void set(E e) {
            if (lastRet < 0)
                throw new IllegalStateException();
            checkForComodification();

            try {
                ArrayList.this.set(lastRet, e);
            } catch (IndexOutOfBoundsException ex) {
                throw new ConcurrentModificationException();
            }
        }

        public void add(E e) {
            checkForComodification();

            try {
                int i = cursor;
                ArrayList.this.add(i, e);
                cursor = i + 1;
                lastRet = -1;
                expectedModCount = modCount;
            } catch (IndexOutOfBoundsException ex) {
                throw new ConcurrentModificationException();
            }
        }
    }

    /**
     * Returns a view of the portion of this list between the specified
     * {@code fromIndex}, inclusive, and {@code toIndex}, exclusive.  (If
     * {@code fromIndex} and {@code toIndex} are equal, the returned list is
     * empty.)  The returned list is backed by this list, so non-structural
     * changes in the returned list are reflected in this list, and vice-versa.
     * The returned list supports all of the optional list operations.
     *
     * <p>This method eliminates the need for explicit range operations (of
     * the sort that commonly exist for arrays).  Any operation that expects
     * a list can be used as a range operation by passing a subList view
     * instead of a whole list.  For example, the following idiom
     * removes a range of elements from a list:
     * <pre>
     *      list.subList(from, to).clear();
     * </pre>
     * Similar idioms may be constructed for {@link #indexOf(Object)} and
     * {@link #lastIndexOf(Object)}, and all of the algorithms in the
     * {@link Collections} class can be applied to a subList.
     *
     * <p>The semantics of the list returned by this method become undefined if
     * the backing list (i.e., this list) is <i>structurally modified</i> in
     * any way other than via the returned list.  (Structural modifications are
     * those that change the size of this list, or otherwise perturb it in such
     * a fashion that iterations in progress may yield incorrect results.)
     *
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     */
    public List<E> subList(int fromIndex, int toIndex) {
        subListRangeCheck(fromIndex, toIndex, size);
        return new SubList(this, 0, fromIndex, toIndex);
    }

    static void subListRangeCheck(int fromIndex, int toIndex, int size) {
        if (fromIndex < 0)
            throw new IndexOutOfBoundsException("fromIndex = " + fromIndex);
        if (toIndex > size)
            throw new IndexOutOfBoundsException("toIndex = " + toIndex);
        if (fromIndex > toIndex)
            throw new IllegalArgumentException("fromIndex(" + fromIndex +
                                               ") > toIndex(" + toIndex + ")");
    }

    private class SubList extends AbstractList<E> implements RandomAccess {
        private final AbstractList<E> parent;
        private final int parentOffset;
        private final int offset;
        int size;

        SubList(AbstractList<E> parent,
                int offset, int fromIndex, int toIndex) {
            this.parent = parent;
            this.parentOffset = fromIndex;
            this.offset = offset + fromIndex;
            this.size = toIndex - fromIndex;
            this.modCount = ArrayList.this.modCount;
        }

        public E set(int index, E e) {
            rangeCheck(index);
            checkForComodification();
            E oldValue = ArrayList.this.elementData(offset + index);
            ArrayList.this.elementData[offset + index] = e;
            return oldValue;
        }

        public E get(int index) {
            rangeCheck(index);
            checkForComodification();
            return ArrayList.this.elementData(offset + index);
        }

        public int size() {
            checkForComodification();
            return this.size;
        }

        public void add(int index, E e) {
            rangeCheckForAdd(index);
            checkForComodification();
            parent.add(parentOffset + index, e);
            this.modCount = parent.modCount;
            this.size++;
        }

        public E remove(int index) {
            rangeCheck(index);
            checkForComodification();
            E result = parent.remove(parentOffset + index);
            this.modCount = parent.modCount;
            this.size--;
            return result;
        }

        protected void removeRange(int fromIndex, int toIndex) {
            checkForComodification();
            parent.removeRange(parentOffset + fromIndex,
                               parentOffset + toIndex);
            this.modCount = parent.modCount;
            this.size -= toIndex - fromIndex;
        }

        public boolean addAll(Collection<? extends E> c) {
            return addAll(this.size, c);
        }

        public boolean addAll(int index, Collection<? extends E> c) {
            rangeCheckForAdd(index);
            int cSize = c.size();
            if (cSize==0)
                return false;

            checkForComodification();
            parent.addAll(parentOffset + index, c);
            this.modCount = parent.modCount;
            this.size += cSize;
            return true;
        }

        public Iterator<E> iterator() {
            return listIterator();
        }

        public ListIterator<E> listIterator(final int index) {
            checkForComodification();
            rangeCheckForAdd(index);
            final int offset = this.offset;

            return new ListIterator<E>() {
                int cursor = index;
                int lastRet = -1;
                int expectedModCount = ArrayList.this.modCount;

                public boolean hasNext() {
                    return cursor != SubList.this.size;
                }

                @SuppressWarnings("unchecked")
                public E next() {
                    checkForComodification();
                    int i = cursor;
                    if (i >= SubList.this.size)
                        throw new NoSuchElementException();
                    Object[] elementData = ArrayList.this.elementData;
                    if (offset + i >= elementData.length)
                        throw new ConcurrentModificationException();
                    cursor = i + 1;
                    return (E) elementData[offset + (lastRet = i)];
                }

                public boolean hasPrevious() {
                    return cursor != 0;
                }

                @SuppressWarnings("unchecked")
                public E previous() {
                    checkForComodification();
                    int i = cursor - 1;
                    if (i < 0)
                        throw new NoSuchElementException();
                    Object[] elementData = ArrayList.this.elementData;
                    if (offset + i >= elementData.length)
                        throw new ConcurrentModificationException();
                    cursor = i;
                    return (E) elementData[offset + (lastRet = i)];
                }

                @SuppressWarnings("unchecked")
                public void forEachRemaining(Consumer<? super E> consumer) {
                    Objects.requireNonNull(consumer);
                    final int size = SubList.this.size;
                    int i = cursor;
                    if (i >= size) {
                        return;
                    }
                    final Object[] elementData = ArrayList.this.elementData;
                    if (offset + i >= elementData.length) {
                        throw new ConcurrentModificationException();
                    }
                    while (i != size && modCount == expectedModCount) {
                        consumer.accept((E) elementData[offset + (i++)]);
                    }
                    // update once at end of iteration to reduce heap write traffic
                    lastRet = cursor = i;
                    checkForComodification();
                }

                public int nextIndex() {
                    return cursor;
                }

                public int previousIndex() {
                    return cursor - 1;
                }

                public void remove() {
                    if (lastRet < 0)
                        throw new IllegalStateException();
                    checkForComodification();

                    try {
                        SubList.this.remove(lastRet);
                        cursor = lastRet;
                        lastRet = -1;
                        expectedModCount = ArrayList.this.modCount;
                    } catch (IndexOutOfBoundsException ex) {
                        throw new ConcurrentModificationException();
                    }
                }

                public void set(E e) {
                    if (lastRet < 0)
                        throw new IllegalStateException();
                    checkForComodification();

                    try {
                        ArrayList.this.set(offset + lastRet, e);
                    } catch (IndexOutOfBoundsException ex) {
                        throw new ConcurrentModificationException();
                    }
                }

                public void add(E e) {
                    checkForComodification();

                    try {
                        int i = cursor;
                        SubList.this.add(i, e);
                        cursor = i + 1;
                        lastRet = -1;
                        expectedModCount = ArrayList.this.modCount;
                    } catch (IndexOutOfBoundsException ex) {
                        throw new ConcurrentModificationException();
                    }
                }

                final void checkForComodification() {
                    if (expectedModCount != ArrayList.this.modCount)
                        throw new ConcurrentModificationException();
                }
            };
        }

        public List<E> subList(int fromIndex, int toIndex) {
            subListRangeCheck(fromIndex, toIndex, size);
            return new SubList(this, offset, fromIndex, toIndex);
        }

        private void rangeCheck(int index) {
            if (index < 0 || index >= this.size)
                throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
        }

        private void rangeCheckForAdd(int index) {
            if (index < 0 || index > this.size)
                throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
        }

        private String outOfBoundsMsg(int index) {
            return "Index: "+index+", Size: "+this.size;
        }

        private void checkForComodification() {
            if (ArrayList.this.modCount != this.modCount)
                throw new ConcurrentModificationException();
        }

        public Spliterator<E> spliterator() {
            checkForComodification();
            return new ArrayListSpliterator<E>(ArrayList.this, offset,
                                               offset + this.size, this.modCount);
        }
    }

    @Override
    public void forEach(Consumer<? super E> action) {
        Objects.requireNonNull(action);
        final int expectedModCount = modCount;
        @SuppressWarnings("unchecked")
        final E[] elementData = (E[]) this.elementData;
        final int size = this.size;
        for (int i=0; modCount == expectedModCount && i < size; i++) {
            action.accept(elementData[i]);
        }
        if (modCount != expectedModCount) {
            throw new ConcurrentModificationException();
        }
    }

    /**
     * Creates a <em><a href="Spliterator.html#binding">late-binding</a></em>
     * and <em>fail-fast</em> {@link Spliterator} over the elements in this
     * list.
     *
     * <p>The {@code Spliterator} reports {@link Spliterator#SIZED},
     * {@link Spliterator#SUBSIZED}, and {@link Spliterator#ORDERED}.
     * Overriding implementations should document the reporting of additional
     * characteristic values.
     *
     * @return a {@code Spliterator} over the elements in this list
     * @since 1.8
     */
    @Override
    public Spliterator<E> spliterator() {
        return new ArrayListSpliterator<>(this, 0, -1, 0);
    }

    /** Index-based split-by-two, lazily initialized Spliterator */
    static final class ArrayListSpliterator<E> implements Spliterator<E> {

        /*
         * If ArrayLists were immutable, or structurally immutable (no
         * adds, removes, etc), we could implement their spliterators
         * with Arrays.spliterator. Instead we detect as much
         * interference during traversal as practical without
         * sacrificing much performance. We rely primarily on
         * modCounts. These are not guaranteed to detect concurrency
         * violations, and are sometimes overly conservative about
         * within-thread interference, but detect enough problems to
         * be worthwhile in practice. To carry this out, we (1) lazily
         * initialize fence and expectedModCount until the latest
         * point that we need to commit to the state we are checking
         * against; thus improving precision.  (This doesn't apply to
         * SubLists, that create spliterators with current non-lazy
         * values).  (2) We perform only a single
         * ConcurrentModificationException check at the end of forEach
         * (the most performance-sensitive method). When using forEach
         * (as opposed to iterators), we can normally only detect
         * interference after actions, not before. Further
         * CME-triggering checks apply to all other possible
         * violations of assumptions for example null or too-small
         * elementData array given its size(), that could only have
         * occurred due to interference.  This allows the inner loop
         * of forEach to run without any further checks, and
         * simplifies lambda-resolution. While this does entail a
         * number of checks, note that in the common case of
         * list.stream().forEach(a), no checks or other computation
         * occur anywhere other than inside forEach itself.  The other
         * less-often-used methods cannot take advantage of most of
         * these streamlinings.
         */

        private final ArrayList<E> list;
        private int index; // current index, modified on advance/split
        private int fence; // -1 until used; then one past last index
        private int expectedModCount; // initialized when fence set

        /** Create new spliterator covering the given  range */
        ArrayListSpliterator(ArrayList<E> list, int origin, int fence,
                             int expectedModCount) {
            this.list = list; // OK if null unless traversed
            this.index = origin;
            this.fence = fence;
            this.expectedModCount = expectedModCount;
        }

        private int getFence() { // initialize fence to size on first use
            int hi; // (a specialized variant appears in method forEach)
            ArrayList<E> lst;
            if ((hi = fence) < 0) {
                if ((lst = list) == null)
                    hi = fence = 0;
                else {
                    expectedModCount = lst.modCount;
                    hi = fence = lst.size;
                }
            }
            return hi;
        }

        public ArrayListSpliterator<E> trySplit() {
            int hi = getFence(), lo = index, mid = (lo + hi) >>> 1;
            return (lo >= mid) ? null : // divide range in half unless too small
                new ArrayListSpliterator<E>(list, lo, index = mid,
                                            expectedModCount);
        }

        public boolean tryAdvance(Consumer<? super E> action) {
            if (action == null)
                throw new NullPointerException();
            int hi = getFence(), i = index;
            if (i < hi) {
                index = i + 1;
                @SuppressWarnings("unchecked") E e = (E)list.elementData[i];
                action.accept(e);
                if (list.modCount != expectedModCount)
                    throw new ConcurrentModificationException();
                return true;
            }
            return false;
        }

        public void forEachRemaining(Consumer<? super E> action) {
            int i, hi, mc; // hoist accesses and checks from loop
            ArrayList<E> lst; Object[] a;
            if (action == null)
                throw new NullPointerException();
            if ((lst = list) != null && (a = lst.elementData) != null) {
                if ((hi = fence) < 0) {
                    mc = lst.modCount;
                    hi = lst.size;
                }
                else
                    mc = expectedModCount;
                if ((i = index) >= 0 && (index = hi) <= a.length) {
                    for (; i < hi; ++i) {
                        @SuppressWarnings("unchecked") E e = (E) a[i];
                        action.accept(e);
                    }
                    if (lst.modCount == mc)
                        return;
                }
            }
            throw new ConcurrentModificationException();
        }

        public long estimateSize() {
            return (long) (getFence() - index);
        }

        public int characteristics() {
            return Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED;
        }
    }

    @Override
    public boolean removeIf(Predicate<? super E> filter) {
        Objects.requireNonNull(filter);
        // figure out which elements are to be removed
        // any exception thrown from the filter predicate at this stage
        // will leave the collection unmodified
        int removeCount = 0;
        final BitSet removeSet = new BitSet(size);
        final int expectedModCount = modCount;
        final int size = this.size;
        for (int i=0; modCount == expectedModCount && i < size; i++) {
            @SuppressWarnings("unchecked")
            final E element = (E) elementData[i];
            if (filter.test(element)) {
                removeSet.set(i);
                removeCount++;
            }
        }
        if (modCount != expectedModCount) {
            throw new ConcurrentModificationException();
        }

        // shift surviving elements left over the spaces left by removed elements
        final boolean anyToRemove = removeCount > 0;
        if (anyToRemove) {
            final int newSize = size - removeCount;
            for (int i=0, j=0; (i < size) && (j < newSize); i++, j++) {
                i = removeSet.nextClearBit(i);
                elementData[j] = elementData[i];
            }
            for (int k=newSize; k < size; k++) {
                elementData[k] = null;  // Let gc do its work
            }
            this.size = newSize;
            if (modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
            modCount++;
        }

        return anyToRemove;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void replaceAll(UnaryOperator<E> operator) {
        Objects.requireNonNull(operator);
        final int expectedModCount = modCount;
        final int size = this.size;
        for (int i=0; modCount == expectedModCount && i < size; i++) {
            elementData[i] = operator.apply((E) elementData[i]);
        }
        if (modCount != expectedModCount) {
            throw new ConcurrentModificationException();
        }
        modCount++;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void sort(Comparator<? super E> c) {
        final int expectedModCount = modCount;
        Arrays.sort((E[]) elementData, 0, size, c);
        if (modCount != expectedModCount) {
            throw new ConcurrentModificationException();
        }
        modCount++;
    }
}
