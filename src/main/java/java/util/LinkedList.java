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
 * 20201118
 * {@code list}和{@codedeque}接口的双链接列表实现。实现所有可选的列表操作，并允许所有元素（包括{@code null}）。
 */
/**
 * Doubly-linked list implementation of the {@code List} and {@code Deque}
 * interfaces.  Implements all optional list operations, and permits all
 * elements (including {@code null}).
 *
 * <p>All of the operations perform as could be expected for a doubly-linked
 * list.  Operations that index into the list will traverse the list from
 * the beginning or the end, whichever is closer to the specified index.
 *
 * <p><strong>Note that this implementation is not synchronized.</strong>
 * If multiple threads access a linked list concurrently, and at least
 * one of the threads modifies the list structurally, it <i>must</i> be
 * synchronized externally.  (A structural modification is any operation
 * that adds or deletes one or more elements; merely setting the value of
 * an element is not a structural modification.)  This is typically
 * accomplished by synchronizing on some object that naturally
 * encapsulates the list.
 *
 * If no such object exists, the list should be "wrapped" using the
 * {@link Collections#synchronizedList Collections.synchronizedList}
 * method.  This is best done at creation time, to prevent accidental
 * unsynchronized access to the list:<pre>
 *   List list = Collections.synchronizedList(new LinkedList(...));</pre>
 *
 * <p>The iterators returned by this class's {@code iterator} and
 * {@code listIterator} methods are <i>fail-fast</i>: if the list is
 * structurally modified at any time after the iterator is created, in
 * any way except through the Iterator's own {@code remove} or
 * {@code add} methods, the iterator will throw a {@link
 * ConcurrentModificationException}.  Thus, in the face of concurrent
 * modification, the iterator fails quickly and cleanly, rather than
 * risking arbitrary, non-deterministic behavior at an undetermined
 * time in the future.
 *
 * <p>Note that the fail-fast behavior of an iterator cannot be guaranteed
 * as it is, generally speaking, impossible to make any hard guarantees in the
 * presence of unsynchronized concurrent modification.  Fail-fast iterators
 * throw {@code ConcurrentModificationException} on a best-effort basis.
 * Therefore, it would be wrong to write a program that depended on this
 * exception for its correctness:   <i>the fail-fast behavior of iterators
 * should be used only to detect bugs.</i>
 *
 * <p>This class is a member of the
 * <a href="{@docRoot}/../technotes/guides/collections/index.html">
 * Java Collections Framework</a>.
 *
 * @author  Josh Bloch
 * @see     List
 * @see     ArrayList
 * @since 1.2
 * @param <E> the type of elements held in this collection
 */
// 20201118 继承AbstractSequentialList, 实现List、Deque、Cloneable、Serializble接口, 代表是可克隆、可序列化的
public class LinkedList<E> extends AbstractSequentialList<E> implements List<E>, Deque<E>, Cloneable, java.io.Serializable
{
    // 20201118 实际元素个数 -> transient禁止该属性参与序列化
    transient int size = 0;

    /**
     * Pointer to first node.
     * Invariant: (first == null && last == null) ||
     *            (first.prev == null && first.item != null)
     */
    // 20201118 头结点指针
    transient Node<E> first;

    /**
     * Pointer to last node.
     * Invariant: (first == null && last == null) ||
     *            (last.next == null && last.item != null)
     */
    // 20201118 末尾结点指针
    transient Node<E> last;

    /**
     * Constructs an empty list.
     */
    // 20201118 构造方法 -> 空参构造
    public LinkedList() {
    }

    /**
     * Constructs a list containing the elements of the specified
     * collection, in the order they are returned by the collection's
     * iterator.
     *
     * @param  c the collection whose elements are to be placed into this list
     * @throws NullPointerException if the specified collection is null
     */
    // 20201118 构造方法 -> 根据指定集合进行构造
    public LinkedList(Collection<? extends E> c) {
        this();// 20201118 调用普通构造方法
        addAll(c);// 20201118 添加集合元素到末尾
    }

    /**
     * Links e as first element.
     */
    // 20201118 链接到头指针前
    private void linkFirst(E e) {
        final Node<E> f = first;// 20201118 备份头指针
        final Node<E> newNode = new Node<>(null, e, f);// 20201118 在头指针前创建结点
        first = newNode;// 20201118 更新头指针指向新结点
        if (f == null)// 20201118 如果该结点为第一个元素
            last = newNode;// 20201118 则再赋值给末尾指针
        else
            f.prev = newNode;// 20201118 否则赋值给连接该结点

        // 20201118 更新实际元素个数
        size++;

        // 20201118 结构修改次数+1
        modCount++;
    }

    /**
     * Links e as last element.
     */
    // 20201118 链接到末尾结点后
    void linkLast(E e) {
        // 20201118 备份末尾结点
        final Node<E> l = last;

        // 20201118 在末尾结点后创建结点
        final Node<E> newNode = new Node<>(l, e, null);

        // 20201118 更新末尾结点为当前结点
        last = newNode;

        // 20201118 如果为第一个元素
        if (l == null)
            first = newNode;// 20201118 则再赋值给头指针
        else
            l.next = newNode;// 20201118 否则链接该结点

        // 20201118 更新实际元素个数
        size++;

        // 20201118 更新结构修改次数
        modCount++;
    }

    /**
     * Inserts element e before non-null Node succ.
     */
    // 20201118 插入一个元素到某个结点前
    void linkBefore(E e, Node<E> succ) {
        // assert succ != null;
        // 20201118 获取succ结点的前结点
        final Node<E> pred = succ.prev;

        // 20201118 在pred与succ结点之间创建新结点
        final Node<E> newNode = new Node<>(pred, e, succ);

        // 20201118 succ链接新结点
        succ.prev = newNode;

        // 20201118 如果前结点为空, 说明succ结点为第一个结点
        if (pred == null)
            first = newNode;// 20201118 则设置新结点为头指针
        else
            pred.next = newNode;// 20201118 否则前结点也链接新结点

        // 20201118 实际元素大小+1
        size++;

        // 20201118 结构修改次数+1
        modCount++;
    }

    /**
     * Unlinks non-null first node f.
     */
    // 20201118 解除头结点链接
    private E unlinkFirst(Node<E> f) {
        // assert f == first && f != null;
        // 20201118 获取该结点元素
        final E element = f.item;

        // 20201118 获取该结点的下一个结点
        final Node<E> next = f.next;

        // 20201118 清空该结点
        f.item = null;
        f.next = null; // help GC // 20201118 通知GC回收

        // 20201118 头节点更新为next结点
        first = next;

        // 20201118 如果没有next结点, 说明为最后一个结点, 解除后, 将不存在结点
        if (next == null)
            last = null;// 20201118 则末尾结点为null
        else
            next.prev = null;// 20201118 否则, 头节点的前结点置为null

        // 20201118 更新实际元素个数
        size--;

        // 20201118 更新结构修改次数
        modCount++;

        // 20201118 返回头节点中的元素
        return element;
    }

    /**
     * Unlinks non-null last node l.
     */
    // 20201118 解除末尾结点
    private E unlinkLast(Node<E> l) {
        // assert l == last && l != null;
        // 20201118 获取末尾结点元素
        final E element = l.item;

        // 20201118 获取前一个结点prev
        final Node<E> prev = l.prev;

        // 20201118 清空该结点
        l.item = null;
        l.prev = null; // help GC

        // 20201118 更新末尾结点为前结点prev
        last = prev;

        // 20201118 如果前结点为空, 说明为第一个元素, 解除后链表将不存在元素
        if (prev == null)
            first = null;// 20201118 这时置空头指针
        else
            prev.next = null;// 20201118 否则前结点为末尾结点

        // 20201118 更新实际元素个数
        size--;

        // 20201118 更新结构修改次数
        modCount++;
        return element;
    }

    /**
     * Unlinks non-null node x.
     */
    // 20201118 解除指定结点
    E unlink(Node<E> x) {
        // assert x != null;
        // 20201118 获取该结点的元素
        final E element = x.item;

        // 20201118 获取该结点的后结点
        final Node<E> next = x.next;

        // 20201118 获取该结点的前结点
        final Node<E> prev = x.prev;

        // 20201118 如果前结点为空, 说明为第一个元素
        if (prev == null) {
            first = next;// 20201118 则头节点为后结点
        } else {// 20201118 否则说明不为第一个元素
            prev.next = next;// 20201118 则链接前结点与后结点
            x.prev = null;// 20201118 解除该结点与前结点的关系
        }

        // 20201118 同理如果后结点为空, 说明为最后一个元素
        if (next == null) {
            last = prev;// 20201118 则末尾结点为前结点
        } else {// 20201118 否则说明不为最后一个元素
            next.prev = prev;// 20201118 则链接前结点与后结点
            x.next = null;// 20201118 解除该结点与后结点的关系
        }

        // 20201118 清空该节点元素, 通知GC回收
        x.item = null;

        // 20201118 更新实际元素个数
        size--;

        // 20201118 更新结构修改次数
        modCount++;

        // 20201118 返回该结点原来的元素
        return element;
    }

    /**
     * Returns the first element in this list.
     *
     * @return the first element in this list
     * @throws NoSuchElementException if this list is empty
     */
    // 20201119 获取头结点元素
    public E getFirst() {
        final Node<E> f = first;
        if (f == null)
            throw new NoSuchElementException();
        return f.item;
    }

    /**
     * Returns the last element in this list.
     *
     * @return the last element in this list
     * @throws NoSuchElementException if this list is empty
     */
    // 20201119 获取末尾结点元素
    public E getLast() {
        final Node<E> l = last;
        if (l == null)
            throw new NoSuchElementException();
        return l.item;
    }

    /**
     * Removes and returns the first element from this list.
     *
     * @return the first element from this list
     * @throws NoSuchElementException if this list is empty
     */
    // 20201119 删除头结点
    public E removeFirst() {
        final Node<E> f = first;
        if (f == null)
            throw new NoSuchElementException();
        return unlinkFirst(f);
    }

    /**
     * Removes and returns the last element from this list.
     *
     * @return the last element from this list
     * @throws NoSuchElementException if this list is empty
     */
    // 20201119 删除末尾结点
    public E removeLast() {
        final Node<E> l = last;
        if (l == null)
            throw new NoSuchElementException();
        return unlinkLast(l);
    }

    /**
     * Inserts the specified element at the beginning of this list.
     *
     * @param e the element to add
     */
    // 20201119 头结点前添加元素
    public void addFirst(E e) {
        linkFirst(e);
    }

    /**
     * Appends the specified element to the end of this list.
     *
     * <p>This method is equivalent to {@link #add}.
     *
     * @param e the element to add
     */
    // 20201119 末尾结点后添加元素
    public void addLast(E e) {
        linkLast(e);
    }

    /**
     * Returns {@code true} if this list contains the specified element.
     * More formally, returns {@code true} if and only if this list contains
     * at least one element {@code e} such that
     * <tt>(o==null&nbsp;?&nbsp;e==null&nbsp;:&nbsp;o.equals(e))</tt>.
     *
     * @param o element whose presence in this list is to be tested
     * @return {@code true} if this list contains the specified element
     */
    // 20201119 判断是否包含指定元素, 包含返回true, 不包含false
    public boolean contains(Object o) {
        return indexOf(o) != -1;
    }

    /**
     * Returns the number of elements in this list.
     *
     * @return the number of elements in this list
     */
    // 20201119 获取实际元素个数
    public int size() {
        return size;
    }

    /**
     * Appends the specified element to the end of this list.
     *
     * <p>This method is equivalent to {@link #addLast}.
     *
     * @param e element to be appended to this list
     * @return {@code true} (as specified by {@link Collection#add})
     */
    // 20201119 添加元素到末尾
    public boolean add(E e) {
        linkLast(e);
        return true;
    }

    /**
     * Removes the first occurrence of the specified element from this list,
     * if it is present.  If this list does not contain the element, it is
     * unchanged.  More formally, removes the element with the lowest index
     * {@code i} such that
     * <tt>(o==null&nbsp;?&nbsp;get(i)==null&nbsp;:&nbsp;o.equals(get(i)))</tt>
     * (if such an element exists).  Returns {@code true} if this list
     * contained the specified element (or equivalently, if this list
     * changed as a result of the call).
     *
     * @param o element to be removed from this list, if present
     * @return {@code true} if this list contained the specified element
     */
    // 20201119 删除指定元素
    public boolean remove(Object o) {
        // 20201119 如果元素为null
        if (o == null) {
            // 20201119 则遍历找到第一个null, 解除该结点
            for (Node<E> x = first; x != null; x = x.next) {
                if (x.item == null) {
                    unlink(x);
                    return true;// 删除成功返回true
                }
            }
        } else {
            // 20201119 遍历找到同一个元素, 解除该结点
            for (Node<E> x = first; x != null; x = x.next) {
                if (o.equals(x.item)) {
                    unlink(x);
                    return true;// 删除成功返回true
                }
            }
        }

        // 20201119 找不到元素, 删除失败返回false
        return false;
    }

    /**
     * Appends all of the elements in the specified collection to the end of
     * this list, in the order that they are returned by the specified
     * collection's iterator.  The behavior of this operation is undefined if
     * the specified collection is modified while the operation is in
     * progress.  (Note that this will occur if the specified collection is
     * this list, and it's nonempty.)
     *
     * @param c collection containing elements to be added to this list
     * @return {@code true} if this list changed as a result of the call
     * @throws NullPointerException if the specified collection is null
     */
    // 20201118 添加集合元素到末尾
    public boolean addAll(Collection<? extends E> c) {
        return addAll(size, c);
    }

    /**
     * 20201118
     * 从指定位置开始，将指定集合中的所有元素插入此列表。
     * 将当前位于该位置的元素（如果有）和任何后续元素向右移动（增加其索引）。
     * 新元素将按照指定集合的迭代器返回的顺序出现在列表中。
     */
    /**
     * Inserts all of the elements in the specified collection into this
     * list, starting at the specified position.  Shifts the element
     * currently at that position (if any) and any subsequent elements to
     * the right (increases their indices).  The new elements will appear
     * in the list in the order that they are returned by the
     * specified collection's iterator.
     *
     * @param index index at which to insert the first element
     *              from the specified collection
     * @param c collection containing elements to be added to this list
     * @return {@code true} if this list changed as a result of the call
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws NullPointerException if the specified collection is null
     */
    // 20201118 添加结合元素到指定位置 -> index前方
    public boolean addAll(int index, Collection<? extends E> c) {
        // 20201118 指针索引校验
        checkPositionIndex(index);

        // 20201118 获取目标数组
        Object[] a = c.toArray();
        int numNew = a.length;
        if (numNew == 0)
            return false;

        // 20201118 初始化临时结点, succ节点为当前节点, pred节点为前一个节点
        Node<E> pred, succ;
        if (index == size) {// 如果指定添加到末尾
            succ = null;
            pred = last;// pred为末尾结点
        } else {
            succ = node(index);// 否则创建index位置结点
            pred = succ.prev;// pred结点为succ结点的前一个结点
        }

        // 20201118 遍历目标数组
        for (Object o : a) {
            // 20201118 获取元素创建节点
            @SuppressWarnings("unchecked") E e = (E) o;
            Node<E> newNode = new Node<>(pred, e, null);

            // 20201118  如果链表为空
            if (pred == null)
                first = newNode;// 20201118 则赋值给头指针
            else
                pred.next = newNode;// 20201118 如果不为空, 则追加结点
            pred = newNode;// 更新pred结点到新结点位置, 准备开始新一轮遍历
        }

        // 20201118 如果是追加到链表末尾
        if (succ == null) {
            last = pred;// 20201118 则把遍历后的pred结点赋值给末尾结点
        } else {// 20201118 否则, 添加到链表中间时, 则指定index结点为末尾结点
            pred.next = succ;
            succ.prev = pred;
        }

        // 20201118 更新实际元素个数
        size += numNew;

        // 20201118 更新结构修改次数
        modCount++;
        return true;
    }

    /**
     * Removes all of the elements from this list.
     * The list will be empty after this call returns.
     */
    // 20201119 清空链表
    public void clear() {
        // Clearing all of the links between nodes is "unnecessary", but:
        // - helps a generational GC if the discarded nodes inhabit
        //   more than one generation
        // - is sure to free memory even if there is a reachable Iterator
        // 20201119 遍历链表
        for (Node<E> x = first; x != null; ) {
            Node<E> next = x.next;

            // 清空每一个节点
            x.item = null;
            x.next = null;
            x.prev = null;
            x = next;
        }

        // 20201119 清空头尾结点
        first = last = null;

        // 20201119 初始化实际元素个数
        size = 0;

        // 20201119 更新结构修改次数
        modCount++;
    }


    // Positional Access Operations

    /**
     * Returns the element at the specified position in this list.
     *
     * @param index index of the element to return
     * @return the element at the specified position in this list
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    // 20201119 根据索引获取元素
    public E get(int index) {
        // 20201119 索引越界校验
        checkElementIndex(index);

        // 20201119 根据索引获取结点, 返回该结点元素
        return node(index).item;
    }

    /**
     * Replaces the element at the specified position in this list with the
     * specified element.
     *
     * @param index index of the element to replace
     * @param element element to be stored at the specified position
     * @return the element previously at the specified position
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    // 20201119 设置元素到指定位置
    public E set(int index, E element) {
        // 20201119 索引越界校验
        checkElementIndex(index);

        // 20201119 根据索引获取结点
        Node<E> x = node(index);

        // 20201119 获取该结点元素
        E oldVal = x.item;

        // 20201119 替换结点元素
        x.item = element;

        // 20201119 返回旧值
        return oldVal;
    }

    /**
     * Inserts the specified element at the specified position in this list.
     * Shifts the element currently at that position (if any) and any
     * subsequent elements to the right (adds one to their indices).
     *
     * @param index index at which the specified element is to be inserted
     * @param element element to be inserted
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    // 20201119 添加元素到指定索引位置
    public void add(int index, E element) {
        // 20201119 索引指针校验
        checkPositionIndex(index);

        // 20201119 如果是末尾, 则添加到末尾
        if (index == size)
            linkLast(element);
        else
            // 20201119 否则添加到索引之前的位置
            linkBefore(element, node(index));
    }

    /**
     * Removes the element at the specified position in this list.  Shifts any
     * subsequent elements to the left (subtracts one from their indices).
     * Returns the element that was removed from the list.
     *
     * @param index the index of the element to be removed
     * @return the element previously at the specified position
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    // 20201119 根据索引删除元素
    public E remove(int index) {
        // 20201119 索引越界校验
        checkElementIndex(index);

        // 20201119 根据索引获取结点, 然后解除结点
        return unlink(node(index));
    }

    /**
     * Tells if the argument is the index of an existing element.
     */
    // 20201119 index >= && index < size => true
    private boolean isElementIndex(int index) {
        return index >= 0 && index < size;
    }

    /**
     * Tells if the argument is the index of a valid position for an
     * iterator or an add operation.
     */
    // 20201118 索引大于0且小于等于size时返回true
    private boolean isPositionIndex(int index) {
        return index >= 0 && index <= size;
    }

    /**
     * Constructs an IndexOutOfBoundsException detail message.
     * Of the many possible refactorings of the error handling code,
     * this "outlining" performs best with both server and client VMs.
     */
    private String outOfBoundsMsg(int index) {
        return "Index: "+index+", Size: "+size;
    }

    // 20201119 索引越界校验
    private void checkElementIndex(int index) {
        // 20201119 index >= && index < size => true
        if (!isElementIndex(index))
            throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
    }

    // 20201118 指针索引校验
    private void checkPositionIndex(int index) {
        if (!isPositionIndex(index))
            throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
    }

    /**
     * Returns the (non-null) Node at the specified element index.
     */
    // 20201118 根据索引获取结点
    Node<E> node(int index) {
        // assert isElementIndex(index);

        // 20201118 如果指针在链表的前半部分
        if (index < (size >> 1)) {
            // 20201118 则从前半部分查找, 返回index位置结点
            Node<E> x = first;
            for (int i = 0; i < index; i++)
                x = x.next;
            return x;
        } else {
            // 20201118 则从后半部分查找, 返回index位置结点
            Node<E> x = last;
            for (int i = size - 1; i > index; i--)
                x = x.prev;
            return x;
        }
    }

    // Search Operations

    /**
     * Returns the index of the first occurrence of the specified element
     * in this list, or -1 if this list does not contain the element.
     * More formally, returns the lowest index {@code i} such that
     * <tt>(o==null&nbsp;?&nbsp;get(i)==null&nbsp;:&nbsp;o.equals(get(i)))</tt>,
     * or -1 if there is no such index.
     *
     * @param o element to search for
     * @return the index of the first occurrence of the specified element in
     *         this list, or -1 if this list does not contain the element
     */
    // 20201118 获取指定元素的索引
    public int indexOf(Object o) {
        int index = 0;

        // 20201118 如果元素为null
        if (o == null) {
            // 20201118 遍历找到第一个null元素, 返回当前索引
            for (Node<E> x = first; x != null; x = x.next) {
                if (x.item == null)
                    return index;
                index++;
            }
        } else {
            // 20201118 否则遍历找到同一个元素(equals), 返回当期索引
            for (Node<E> x = first; x != null; x = x.next) {
                if (o.equals(x.item))
                    return index;
                index++;
            }
        }

        // 20201118 找不到则返回-1
        return -1;
    }

    /**
     * Returns the index of the last occurrence of the specified element
     * in this list, or -1 if this list does not contain the element.
     * More formally, returns the highest index {@code i} such that
     * <tt>(o==null&nbsp;?&nbsp;get(i)==null&nbsp;:&nbsp;o.equals(get(i)))</tt>,
     * or -1 if there is no such index.
     *
     * @param o element to search for
     * @return the index of the last occurrence of the specified element in
     *         this list, or -1 if this list does not contain the element
     */
    // 20201119 获取元素最后的索引
    public int lastIndexOf(Object o) {
        // 20201119 从尾部开始遍历
        int index = size;
        if (o == null) {
            // 20201119 如果元素为null
            for (Node<E> x = last; x != null; x = x.prev) {
                index--;
                if (x.item == null)
                    return index;// 20201119 找到尾部第一个null元素, 返回索引
            }
        } else {
            // 20201119 如果元素不为null
            for (Node<E> x = last; x != null; x = x.prev) {
                index--;
                if (o.equals(x.item))
                    return index;// 20201119 找到同一个元素, 返回索引
            }
        }

        // 20201119 如果找不到则返回-1
        return -1;
    }

    // Queue operations.

    /**
     * Retrieves, but does not remove, the head (first element) of this list.
     *
     * @return the head of this list, or {@code null} if this list is empty
     * @since 1.5
     */
    // 20201119 Deque接口实现-检索但不删除此列表的头（第一个元素）。
    public E peek() {
        // 20201119 获取头结点
        final Node<E> f = first;

        // 20201119 如果头结点为空则返回null, 如果头结点不为空, 则返回头结点元素
        return (f == null) ? null : f.item;
    }

    /**
     * Retrieves, but does not remove, the head (first element) of this list.
     *
     * @return the head of this list
     * @throws NoSuchElementException if this list is empty
     * @since 1.5
     */
    // 20201119 Deque接口实现-获取头结点元素
    public E element() {
        return getFirst();
    }

    /**
     * Retrieves and removes the head (first element) of this list.
     *
     * @return the head of this list, or {@code null} if this list is empty
     * @since 1.5
     */
    // 20201119 Deque接口实现-检索并删除此列表的头（第一个元素）。
    public E poll() {
        // 20201119 获取头结点
        final Node<E> f = first;

        // 20201119 删除头结点
        return (f == null) ? null : unlinkFirst(f);
    }

    /**
     * Retrieves and removes the head (first element) of this list.
     *
     * @return the head of this list
     * @throws NoSuchElementException if this list is empty
     * @since 1.5
     */
    // 20201119 Deque接口实现-获取删除头结点
    public E remove() {
        return removeFirst();
    }

    /**
     * Adds the specified element as the tail (last element) of this list.
     *
     * @param e the element to add
     * @return {@code true} (as specified by {@link Queue#offer})
     * @since 1.5
     */
    // 20201119 Deque接口实现-添加指定的元素作为此列表的尾部（最后一个元素）。
    public boolean offer(E e) {
        return add(e);
    }

    // Deque operations
    /**
     * Inserts the specified element at the front of this list.
     *
     * @param e the element to insert
     * @return {@code true} (as specified by {@link Deque#offerFirst})
     * @since 1.6
     */
    // 20201119 Deque接口实现-在此列表的前面插入指定的元素。
    public boolean offerFirst(E e) {
        addFirst(e);
        return true;
    }

    /**
     * Inserts the specified element at the end of this list.
     *
     * @param e the element to insert
     * @return {@code true} (as specified by {@link Deque#offerLast})
     * @since 1.6
     */
    // 20201119 Deque接口实现-在此列表的末尾插入指定的元素。
    public boolean offerLast(E e) {
        addLast(e);
        return true;
    }

    /**
     * Retrieves, but does not remove, the first element of this list,
     * or returns {@code null} if this list is empty.
     *
     * @return the first element of this list, or {@code null}
     *         if this list is empty
     * @since 1.6
     */
    // 20201119 Deque接口实现-检索但不删除此列表的第一个元素，如果该列表为空，则返回{@code null}。
    public E peekFirst() {
        final Node<E> f = first;
        return (f == null) ? null : f.item;
     }

    /**
     * Retrieves, but does not remove, the last element of this list,
     * or returns {@code null} if this list is empty.
     *
     * @return the last element of this list, or {@code null}
     *         if this list is empty
     * @since 1.6
     */
    // 20201119 Deque接口实现-检索但不删除此列表的最后一个元素，如果此列表为空，则返回{@code null}。
    public E peekLast() {
        final Node<E> l = last;
        return (l == null) ? null : l.item;
    }

    /**
     * Retrieves and removes the first element of this list,
     * or returns {@code null} if this list is empty.
     *
     * @return the first element of this list, or {@code null} if
     *     this list is empty
     * @since 1.6
     */
    // 20201119 Deque接口实现-检索并删除此列表的第一个元素，如果此列表为空，则返回{@code null}。
    public E pollFirst() {
        final Node<E> f = first;
        return (f == null) ? null : unlinkFirst(f);
    }

    /**
     * Retrieves and removes the last element of this list,
     * or returns {@code null} if this list is empty.
     *
     * @return the last element of this list, or {@code null} if
     *     this list is empty
     * @since 1.6
     */
    // 20201119 Deque接口实现-检索并删除此列表的最后一个元素，如果此列表为空，则返回{@code null}。
    public E pollLast() {
        final Node<E> l = last;
        return (l == null) ? null : unlinkLast(l);
    }

    /**
     * Pushes an element onto the stack represented by this list.  In other
     * words, inserts the element at the front of this list.
     *
     * <p>This method is equivalent to {@link #addFirst}.
     *
     * @param e the element to push
     * @since 1.6
     */
    // 20201119 Deque接口实现-将元素推到由此列表表示的堆栈上。换句话说，在列表前面插入元素。
    public void push(E e) {
        addFirst(e);
    }

    /**
     * Pops an element from the stack represented by this list.  In other
     * words, removes and returns the first element of this list.
     *
     * <p>This method is equivalent to {@link #removeFirst()}.
     *
     * @return the element at the front of this list (which is the top
     *         of the stack represented by this list)
     * @throws NoSuchElementException if this list is empty
     * @since 1.6
     */
    // 20201119 Deque接口实现-从这个列表表示的堆栈中弹出一个元素。换句话说，删除并返回此列表的第一个元素。
    public E pop() {
        return removeFirst();
    }

    /**
     * Removes the first occurrence of the specified element in this
     * list (when traversing the list from head to tail).  If the list
     * does not contain the element, it is unchanged.
     *
     * @param o element to be removed from this list, if present
     * @return {@code true} if the list contained the specified element
     * @since 1.6
     */
    // 20201119 删除此列表中指定元素的第一个匹配项（从头到尾遍历列表时）。如果列表中不包含元素，则它将保持不变。
    public boolean removeFirstOccurrence(Object o) {
        return remove(o);
    }

    /**
     * Removes the last occurrence of the specified element in this
     * list (when traversing the list from head to tail).  If the list
     * does not contain the element, it is unchanged.
     *
     * @param o element to be removed from this list, if present
     * @return {@code true} if the list contained the specified element
     * @since 1.6
     */
    // 20201119 删除此列表中指定元素的最后一个匹配项（从头到尾遍历列表时）。如果列表中不包含元素，则它将保持不变。
    public boolean removeLastOccurrence(Object o) {
        if (o == null) {
            for (Node<E> x = last; x != null; x = x.prev) {
                if (x.item == null) {
                    unlink(x);
                    return true;
                }
            }
        } else {
            for (Node<E> x = last; x != null; x = x.prev) {
                if (o.equals(x.item)) {
                    unlink(x);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns a list-iterator of the elements in this list (in proper
     * sequence), starting at the specified position in the list.
     * Obeys the general contract of {@code List.listIterator(int)}.<p>
     *
     * The list-iterator is <i>fail-fast</i>: if the list is structurally
     * modified at any time after the Iterator is created, in any way except
     * through the list-iterator's own {@code remove} or {@code add}
     * methods, the list-iterator will throw a
     * {@code ConcurrentModificationException}.  Thus, in the face of
     * concurrent modification, the iterator fails quickly and cleanly, rather
     * than risking arbitrary, non-deterministic behavior at an undetermined
     * time in the future.
     *
     * @param index index of the first element to be returned from the
     *              list-iterator (by a call to {@code next})
     * @return a ListIterator of the elements in this list (in proper
     *         sequence), starting at the specified position in the list
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @see List#listIterator(int)
     */
    // 20201119 返回此列表中元素的列表迭代器（按正确的顺序），从列表中指定的位置开始。遵守{@代码的总合同列表迭代器（内景）}。
    public ListIterator<E> listIterator(int index) {
        // 20201119 指针索引校验
        checkPositionIndex(index);

        // 20201119 返回ListIterator实现类
        return new ListItr(index);
    }

    // 20201119 ListIterator内部实现类
    private class ListItr implements ListIterator<E> {
        private Node<E> lastReturned;
        private Node<E> next;
        private int nextIndex;
        private int expectedModCount = modCount;

        ListItr(int index) {
            // assert isPositionIndex(index);
            next = (index == size) ? null : node(index);
            nextIndex = index;
        }

        public boolean hasNext() {
            return nextIndex < size;
        }

        public E next() {
            checkForComodification();
            if (!hasNext())
                throw new NoSuchElementException();

            lastReturned = next;
            next = next.next;
            nextIndex++;
            return lastReturned.item;
        }

        public boolean hasPrevious() {
            return nextIndex > 0;
        }

        public E previous() {
            checkForComodification();
            if (!hasPrevious())
                throw new NoSuchElementException();

            lastReturned = next = (next == null) ? last : next.prev;
            nextIndex--;
            return lastReturned.item;
        }

        public int nextIndex() {
            return nextIndex;
        }

        public int previousIndex() {
            return nextIndex - 1;
        }

        public void remove() {
            checkForComodification();
            if (lastReturned == null)
                throw new IllegalStateException();

            Node<E> lastNext = lastReturned.next;
            unlink(lastReturned);
            if (next == lastReturned)
                next = lastNext;
            else
                nextIndex--;
            lastReturned = null;
            expectedModCount++;
        }

        public void set(E e) {
            if (lastReturned == null)
                throw new IllegalStateException();
            checkForComodification();
            lastReturned.item = e;
        }

        public void add(E e) {
            checkForComodification();
            lastReturned = null;
            if (next == null)
                linkLast(e);
            else
                linkBefore(e, next);
            nextIndex++;
            expectedModCount++;
        }

        public void forEachRemaining(Consumer<? super E> action) {
            Objects.requireNonNull(action);
            while (modCount == expectedModCount && nextIndex < size) {
                action.accept(next.item);
                lastReturned = next;
                next = next.next;
                nextIndex++;
            }
            checkForComodification();
        }

        final void checkForComodification() {
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
        }
    }

    // 20201118 内部静态结点类
    private static class Node<E> {
        E item;// 实际元素
        Node<E> next;// 下一个结点
        Node<E> prev;// 上一个结点

        Node(Node<E> prev, E element, Node<E> next) {
            this.item = element;
            this.next = next;
            this.prev = prev;
        }
    }

    /**
     * @since 1.6
     */
    // 20201119 逆序迭代器
    public Iterator<E> descendingIterator() {
        return new DescendingIterator();
    }

    /**
     * Adapter to provide descending iterators via ListItr.previous
     */
    // 20201119 适配器提供降序迭代ListItr.previous
    private class DescendingIterator implements Iterator<E> {
        private final ListItr itr = new ListItr(size());
        public boolean hasNext() {
            return itr.hasPrevious();
        }
        public E next() {
            return itr.previous();
        }
        public void remove() {
            itr.remove();
        }
    }

    // 20201119 获取链表的浅复制对象
    @SuppressWarnings("unchecked")
    private LinkedList<E> superClone() {
        try {
            return (LinkedList<E>) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }

    /**
     * Returns a shallow copy of this {@code LinkedList}. (The elements
     * themselves are not cloned.)
     *
     * @return a shallow copy of this {@code LinkedList} instance
     */
    // 20201119 返回此{@code LinkedList}的浅副本。（元素本身不会被克隆。）
    public Object clone() {
        LinkedList<E> clone = superClone();

        // Put clone into "virgin" state
        clone.first = clone.last = null;
        clone.size = 0;
        clone.modCount = 0;

        // Initialize clone with our elements
        for (Node<E> x = first; x != null; x = x.next)
            clone.add(x.item);

        return clone;
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
     * @return an array containing all of the elements in this list
     *         in proper sequence
     */
    // 20201119 遍历获取链表元素Object数组
    public Object[] toArray() {
        Object[] result = new Object[size];
        int i = 0;
        for (Node<E> x = first; x != null; x = x.next)
            result[i++] = x.item;
        return result;
    }

    /**
     * Returns an array containing all of the elements in this list in
     * proper sequence (from first to last element); the runtime type of
     * the returned array is that of the specified array.  If the list fits
     * in the specified array, it is returned therein.  Otherwise, a new
     * array is allocated with the runtime type of the specified array and
     * the size of this list.
     *
     * <p>If the list fits in the specified array with room to spare (i.e.,
     * the array has more elements than the list), the element in the array
     * immediately following the end of the list is set to {@code null}.
     * (This is useful in determining the length of the list <i>only</i> if
     * the caller knows that the list does not contain any null elements.)
     *
     * <p>Like the {@link #toArray()} method, this method acts as bridge between
     * array-based and collection-based APIs.  Further, this method allows
     * precise control over the runtime type of the output array, and may,
     * under certain circumstances, be used to save allocation costs.
     *
     * <p>Suppose {@code x} is a list known to contain only strings.
     * The following code can be used to dump the list into a newly
     * allocated array of {@code String}:
     *
     * <pre>
     *     String[] y = x.toArray(new String[0]);</pre>
     *
     * Note that {@code toArray(new Object[0])} is identical in function to
     * {@code toArray()}.
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
    // 20201119 遍历获取链表元素指定类型数组 -> 可以学到返回指定类型对象的写法
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] a) {
        // 20201119 如果数组长度小于实际元素个数
        if (a.length < size)
            // 20201119 则构建size长度的T类型数组
            a = (T[])java.lang.reflect.Array.newInstance(
                                a.getClass().getComponentType(), size);

        // 20201119 开始遍历链表
        int i = 0;
        Object[] result = a;// 20201119 给予a指针给result
        for (Node<E> x = first; x != null; x = x.next)
            // 20201119 填充a数组
            result[i++] = x.item;

        // 20201119 填充剩余元素为null
        if (a.length > size)
            a[size] = null;

        // 20201119 返回a数组
        return a;
    }

    private static final long serialVersionUID = 876323262645176354L;

    /**
     * Saves the state of this {@code LinkedList} instance to a stream
     * (that is, serializes it).
     *
     * @serialData The size of the list (the number of elements it
     *             contains) is emitted (int), followed by all of its
     *             elements (each an Object) in the proper order.
     */
    // 20201119 序列化写方法
    private void writeObject(java.io.ObjectOutputStream s)
        throws java.io.IOException {
        // Write out any hidden serialization magic
        s.defaultWriteObject();

        // Write out size
        s.writeInt(size);

        // Write out all elements in the proper order.
        for (Node<E> x = first; x != null; x = x.next)
            s.writeObject(x.item);
    }

    /**
     * Reconstitutes this {@code LinkedList} instance from a stream
     * (that is, deserializes it).
     */
    // 20201119 反序列化读方法
    @SuppressWarnings("unchecked")
    private void readObject(java.io.ObjectInputStream s)
        throws java.io.IOException, ClassNotFoundException {
        // Read in any hidden serialization magic
        s.defaultReadObject();

        // Read in size
        int size = s.readInt();

        // Read in all elements in the proper order.
        for (int i = 0; i < size; i++)
            linkLast((E)s.readObject());
    }

    /**
     * Creates a <em><a href="Spliterator.html#binding">late-binding</a></em>
     * and <em>fail-fast</em> {@link Spliterator} over the elements in this
     * list.
     *
     * <p>The {@code Spliterator} reports {@link Spliterator#SIZED} and
     * {@link Spliterator#ORDERED}.  Overriding implementations should document
     * the reporting of additional characteristic values.
     *
     * @implNote
     * The {@code Spliterator} additionally reports {@link Spliterator#SUBSIZED}
     * and implements {@code trySplit} to permit limited parallelism..
     *
     * @return a {@code Spliterator} over the elements in this list
     * @since 1.8
     */
    // 20201119 分割器
    @Override
    public Spliterator<E> spliterator() {
        return new LLSpliterator<E>(this, -1, 0);
    }

    // 20201119 分割器实现类
    /** A customized variant of Spliterators.IteratorSpliterator */
    static final class LLSpliterator<E> implements Spliterator<E> {
        static final int BATCH_UNIT = 1 << 10;  // batch array size increment
        static final int MAX_BATCH = 1 << 25;  // max batch array size;
        final LinkedList<E> list; // null OK unless traversed
        Node<E> current;      // current node; null until initialized
        int est;              // size estimate; -1 until first needed
        int expectedModCount; // initialized when est set
        int batch;            // batch size for splits

        LLSpliterator(LinkedList<E> list, int est, int expectedModCount) {
            this.list = list;
            this.est = est;
            this.expectedModCount = expectedModCount;
        }

        final int getEst() {
            int s; // force initialization
            final LinkedList<E> lst;
            if ((s = est) < 0) {
                if ((lst = list) == null)
                    s = est = 0;
                else {
                    expectedModCount = lst.modCount;
                    current = lst.first;
                    s = est = lst.size;
                }
            }
            return s;
        }

        public long estimateSize() { return (long) getEst(); }

        public Spliterator<E> trySplit() {
            Node<E> p;
            int s = getEst();
            if (s > 1 && (p = current) != null) {
                int n = batch + BATCH_UNIT;
                if (n > s)
                    n = s;
                if (n > MAX_BATCH)
                    n = MAX_BATCH;
                Object[] a = new Object[n];
                int j = 0;
                do { a[j++] = p.item; } while ((p = p.next) != null && j < n);
                current = p;
                batch = j;
                est = s - j;
                return Spliterators.spliterator(a, 0, j, Spliterator.ORDERED);
            }
            return null;
        }

        public void forEachRemaining(Consumer<? super E> action) {
            Node<E> p; int n;
            if (action == null) throw new NullPointerException();
            if ((n = getEst()) > 0 && (p = current) != null) {
                current = null;
                est = 0;
                do {
                    E e = p.item;
                    p = p.next;
                    action.accept(e);
                } while (p != null && --n > 0);
            }
            if (list.modCount != expectedModCount)
                throw new ConcurrentModificationException();
        }

        public boolean tryAdvance(Consumer<? super E> action) {
            Node<E> p;
            if (action == null) throw new NullPointerException();
            if (getEst() > 0 && (p = current) != null) {
                --est;
                E e = p.item;
                current = p.next;
                action.accept(e);
                if (list.modCount != expectedModCount)
                    throw new ConcurrentModificationException();
                return true;
            }
            return false;
        }

        public int characteristics() {
            return Spliterator.ORDERED | Spliterator.SIZED | Spliterator.SUBSIZED;
        }
    }

}
