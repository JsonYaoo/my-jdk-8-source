/*
 * Copyright (c) 2000, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package sun.misc;

import java.security.*;
import java.lang.reflect.*;

import sun.reflect.CallerSensitive;
import sun.reflect.Reflection;

/**
 * 20210808
 * 执行低级、不安全操作的方法集合。尽管该类和所有方法都是公共的，但该类的使用受到限制，因为只有受信任的代码才能获得它的实例。
 */
/**
 * A collection of methods for performing low-level, unsafe operations.
 * Although the class and all methods are public, use of this class is
 * limited because only trusted code can obtain instances of it.
 *
 * @author John R. Rose
 * @see #getUnsafe
 */

public final class Unsafe {

    private static native void registerNatives();
    static {
        registerNatives();
        sun.reflect.Reflection.registerMethodsToFilter(Unsafe.class, "getUnsafe");
    }

    private Unsafe() {}

    // 单例Unsafe
    private static final Unsafe theUnsafe = new Unsafe();

    /**
     * 20210808
     * A. 为调用者提供执行不安全操作的能力。
     * B. 调用者应该小心保护返回的 Unsafe 对象，因为它可用于在任意内存地址读取和写入数据。 它绝不能传递给不受信任的代码。
     * C. 此类中的大多数方法都非常低级，并且对应于少量硬件指令（在典型机器上）。 鼓励编译器相应地优化这些方法。
     * D. 这是使用不安全操作的建议习惯用法：
     * class MyTrustedClass {
     *   private static final Unsafe unsafe = Unsafe.getUnsafe();
     *   ...
     *   private long myCountAddress = ...;
     *   public int getCount() { return unsafe.getByte(myCountAddress); }
     * }
     * （它可以帮助编译器使局部变量成为最终变量。）
     */
    /**
     * A.
     * Provides the caller with the capability of performing unsafe
     * operations.
     *
     * B.
     * <p> The returned <code>Unsafe</code> object should be carefully guarded
     * by the caller, since it can be used to read and write data at arbitrary
     * memory addresses.  It must never be passed to untrusted code.
     *
     * C.
     * <p> Most methods in this class are very low-level, and correspond to a
     * small number of hardware instructions (on typical machines).  Compilers
     * are encouraged to optimize these methods accordingly.
     *
     * D.
     * <p> Here is a suggested idiom for using unsafe operations:
     *
     * <blockquote><pre>
     * class MyTrustedClass {
     *   private static final Unsafe unsafe = Unsafe.getUnsafe();
     *   ...
     *   private long myCountAddress = ...;
     *   public int getCount() { return unsafe.getByte(myCountAddress); }
     * }
     * </pre></blockquote>
     *
     * (It may assist compilers to make the local variable be
     * <code>final</code>.)
     *
     * @exception  SecurityException  if a security manager exists and its
     *             <code>checkPropertiesAccess</code> method doesn't allow
     *             access to the system properties.
     */
    @CallerSensitive
    public static Unsafe getUnsafe() {
        Class<?> caller = Reflection.getCallerClass();
        if (!VM.isSystemDomainLoader(caller.getClassLoader()))
            throw new SecurityException("Unsafe");
        return theUnsafe;
    }

    // peek 和 poke 操作（编译器应该将这些优化为内存操作）这些在 Java 堆中的对象字段上工作。 它们不适用于打包数组的元素。
    /// peek and poke operations
    /// (compilers should optimize these to memory ops)

    // These work on object fields in the Java heap.
    // They will not work on elements of packed arrays.

    /**
     * 20210728
     * A. 从给定的Java变量中获取一个值。更具体地说，在给定的偏移量处或（如果o为空）从其数值为给定偏移量的内存地址中获取给定对象o内的字段或数组元素。
     * B. 除非以下情况之一为真，否则结果是不确定的：
     *      a. 偏移量是从某个Java字段的{@link java.lang.reflect.Field}上的{@link #objectFieldOffset}获得的，并且o引用的对象属于与该字段的类兼容的类。
     *      b. 偏移量和对象引用o（空或非空）都是通过{@link #staticFieldOffset}和{@link #staticFieldBase}（分别）从某些Java字段的反射{@link Field}表示中获得的。
     *      c. o引用的对象是一个数组，偏移量为B+N*S形式的整数，其中N是数组的有效索引，B和S是{@link #arrayBaseOffset}获得的值和
     *         {@link #arrayIndexScale}（分别）来自数组的类。引用的值是数组的第N个元素。
     * C. 如果上述情况之一为真，则调用将引用特定的Java变量（字段或数组元素）。但是，如果该变量实际上不是此方法返回的类型，则结果未定义。
     * D. 此方法通过两个参数来引用一个变量，因此它为Java变量提供了（实际上）双寄存器寻址模式。当对象引用为空时，此方法使用其偏移量作为绝对地址。
     *    这在操作上类似于{@link #getInt(long)} 等方法，后者为非 Java 变量提供（实际上）单寄存器寻址模式。但是，由于Java变量在内存中的布局可能与非Java变量不同，
     *    因此程序员不应假设这两种寻址模式是等价的。此外，程序员应该记住，双寄存器寻址模式的偏移量不能与单寄存器寻址模式中使用的long混淆。
     */
    /**
     * A.
     * Fetches a value from a given Java variable.
     * More specifically, fetches a field or array element within the given
     * object <code>o</code> at the given offset, or (if <code>o</code> is
     * null) from the memory address whose numerical value is the given
     * offset.
     *
     * B.
     * <p>
     * The results are undefined unless one of the following cases is true:
     * <ul>
     * <li>The offset was obtained from {@link #objectFieldOffset} on
     * the {@link java.lang.reflect.Field} of some Java field and the object
     * referred to by <code>o</code> is of a class compatible with that
     * field's class.
     *
     * <li>The offset and object reference <code>o</code> (either null or
     * non-null) were both obtained via {@link #staticFieldOffset}
     * and {@link #staticFieldBase} (respectively) from the
     * reflective {@link Field} representation of some Java field.
     *
     * <li>The object referred to by <code>o</code> is an array, and the offset
     * is an integer of the form <code>B+N*S</code>, where <code>N</code> is
     * a valid index into the array, and <code>B</code> and <code>S</code> are
     * the values obtained by {@link #arrayBaseOffset} and {@link
     * #arrayIndexScale} (respectively) from the array's class.  The value
     * referred to is the <code>N</code><em>th</em> element of the array.
     *
     * </ul>
     *
     * C.
     * <p>
     * If one of the above cases is true, the call references a specific Java
     * variable (field or array element).  However, the results are undefined
     * if that variable is not in fact of the type returned by this method.
     *
     * D.
     * <p>
     * This method refers to a variable by means of two parameters, and so
     * it provides (in effect) a <em>double-register</em> addressing mode
     * for Java variables.  When the object reference is null, this method
     * uses its offset as an absolute address.  This is similar in operation
     * to methods such as {@link #getInt(long)}, which provide (in effect) a
     * <em>single-register</em> addressing mode for non-Java variables.
     * However, because Java variables may have a different layout in memory
     * from non-Java variables, programmers should not assume that these
     * two addressing modes are ever equivalent.  Also, programmers should
     * remember that offsets from the double-register addressing mode cannot
     * be portably confused with longs used in the single-register addressing
     * mode.
     *
     * // 变量所在的Java堆对象，如果有，否则为空
     * @param o Java heap object in which the variable resides, if any, else
     *        null
     *
     * // 变量在Java堆对象中驻留位置的偏移指示（如果有），否则是静态定位变量的内存地址
     * @param offset indication of where the variable resides in a Java heap
     *        object, if any, else a memory address locating the variable
     *        statically
     *
     * // 从指定的 Java 变量中获取的值
     * @return the value fetched from the indicated Java variable
     * @throws RuntimeException No defined exceptions are thrown, not even
     *         {@link NullPointerException}
     */
    // 根据对象和偏移量, 获取类中返回值为int的某个字段的值
    public native int getInt(Object o, long offset);

    /**
     * 20210728
     * A. 将值存储到给定的 Java 变量中。
     * B. 前两个参数的解释与{@link #getInt(Object, long)}完全相同，以引用特定的Java变量（字段或数组元素）。给定的值存储在该变量中。
     * C. 该变量必须与方法参数 x 的类型相同。
     */
    /**
     * A.
     * Stores a value into a given Java variable.
     *
     * B.
     * <p>
     * The first two parameters are interpreted exactly as with
     * {@link #getInt(Object, long)} to refer to a specific
     * Java variable (field or array element).  The given value
     * is stored into that variable.
     *
     * C.
     * <p>
     * The variable must be of the same type as the method
     * parameter <code>x</code>.
     *
     * // 变量所在的Java堆对象，如果有，否则为空
     * @param o Java heap object in which the variable resides, if any, else
     *        null
     *
     * // 变量在 Java 堆对象中驻留位置的偏移指示（如果有），否则是静态定位变量的内存地址
     * @param offset indication of where the variable resides in a Java heap
     *        object, if any, else a memory address locating the variable
     *        statically
     *
     * // 要存储到指示的 Java 变量中的值
     * @param x the value to store into the indicated Java variable
     * @throws RuntimeException No defined exceptions are thrown, not even
     *         {@link NullPointerException}
     */
    // 根据对象和偏移量, 将int值存储到Java变量中, 其字段类型必须为int类型
    public native void putInt(Object o, long offset, int x);

    // 从给定的Java变量中获取引用值。
    /**
     * Fetches a reference value from a given Java variable.
     *
     * @see #getInt(Object, long)
     */
    public native Object getObject(Object o, long offset);

    /**
     * 20210728
     * A. 将引用值存储到给定的 Java 变量中。
     * B. 除非存储的引用x为空或匹配字段类型，否则结果是未定义的。如果引用o不为空，则更新该对象的park标记或其他商店障碍（如果 VM 需要它们）。
     */
    /**
     * A.
     * Stores a reference value into a given Java variable.
     *
     * B.
     * <p>
     * Unless the reference <code>x</code> being stored is either null
     * or matches the field type, the results are undefined.
     * If the reference <code>o</code> is non-null, car marks or
     * other store barriers for that object (if the VM requires them)
     * are updated.
     *
     * @see #putInt(Object, int, int)
     */
    // 根据对象和偏移量, 将x值存储到Java变量中, 其字段类型必须为Object类型
    public native void putObject(Object o, long offset, Object x);

    /** @see #getInt(Object, long) */
    public native boolean getBoolean(Object o, long offset);
    /** @see #putInt(Object, int, int) */
    public native void    putBoolean(Object o, long offset, boolean x);
    /** @see #getInt(Object, long) */
    public native byte    getByte(Object o, long offset);
    /** @see #putInt(Object, int, int) */
    public native void    putByte(Object o, long offset, byte x);
    /** @see #getInt(Object, long) */
    public native short   getShort(Object o, long offset);
    /** @see #putInt(Object, int, int) */
    public native void    putShort(Object o, long offset, short x);
    /** @see #getInt(Object, long) */
    public native char    getChar(Object o, long offset);
    /** @see #putInt(Object, int, int) */
    public native void    putChar(Object o, long offset, char x);
    /** @see #getInt(Object, long) */
    public native long    getLong(Object o, long offset);
    /** @see #putInt(Object, int, int) */
    public native void    putLong(Object o, long offset, long x);
    /** @see #getInt(Object, long) */
    public native float   getFloat(Object o, long offset);
    /** @see #putInt(Object, int, int) */
    public native void    putFloat(Object o, long offset, float x);
    /** @see #getInt(Object, long) */
    public native double  getDouble(Object o, long offset);
    /** @see #putInt(Object, int, int) */
    public native void    putDouble(Object o, long offset, double x);

    // 与所有其他具有32位偏移量的方法一样，此方法在以前的版本中是原生的，但现在是一个包装器，它只是将偏移量转换为长值。它提供与针对 1.4 编译的字节码的向后兼容性。
    /**
     * This method, like all others with 32-bit offsets, was native
     * in a previous release but is now a wrapper which simply casts
     * the offset to a long value.  It provides backward compatibility
     * with bytecodes compiled against 1.4.
     *
     * // 从 1.4.1 开始，将 32 位偏移参数转换为 long。
     * @deprecated As of 1.4.1, cast the 32-bit offset argument to a long.
     *
     * See {@link #staticFieldOffset}.
     */
    @Deprecated
    public int getInt(Object o, int offset) {
        return getInt(o, (long)offset);
    }

    // 从 1.4.1 开始，将 32 位偏移参数转换为 long。
    /**
     * @deprecated As of 1.4.1, cast the 32-bit offset argument to a long.
     *
     * See {@link #staticFieldOffset}.
     */
    @Deprecated
    public void putInt(Object o, int offset, int x) {
        putInt(o, (long)offset, x);
    }

    // 从 1.4.1 开始，将 32 位偏移参数转换为 long。
    /**
     * @deprecated As of 1.4.1, cast the 32-bit offset argument to a long.
     *
     * See {@link #staticFieldOffset}.
     */
    @Deprecated
    public Object getObject(Object o, int offset) {
        return getObject(o, (long)offset);
    }

    // 从 1.4.1 开始，将 32 位偏移参数转换为 long。
    /**
     * @deprecated As of 1.4.1, cast the 32-bit offset argument to a long.
     * See {@link #staticFieldOffset}.
     */
    @Deprecated
    public void putObject(Object o, int offset, Object x) {
        putObject(o, (long)offset, x);
    }

    // 从 1.4.1 开始，将 32 位偏移参数转换为 long。
    /**
     * @deprecated As of 1.4.1, cast the 32-bit offset argument to a long.
     * See {@link #staticFieldOffset}.
     */
    @Deprecated
    public boolean getBoolean(Object o, int offset) {
        return getBoolean(o, (long)offset);
    }

    // 从 1.4.1 开始，将 32 位偏移参数转换为 long。
    /**
     * @deprecated As of 1.4.1, cast the 32-bit offset argument to a long.
     * See {@link #staticFieldOffset}.
     */
    @Deprecated
    public void putBoolean(Object o, int offset, boolean x) {
        putBoolean(o, (long)offset, x);
    }

    // 从 1.4.1 开始，将 32 位偏移参数转换为 long。
    /**
     * @deprecated As of 1.4.1, cast the 32-bit offset argument to a long.
     * See {@link #staticFieldOffset}.
     */
    @Deprecated
    public byte getByte(Object o, int offset) {
        return getByte(o, (long)offset);
    }

    // 从 1.4.1 开始，将 32 位偏移参数转换为 long。
    /**
     * @deprecated As of 1.4.1, cast the 32-bit offset argument to a long.
     * See {@link #staticFieldOffset}.
     */
    @Deprecated
    public void putByte(Object o, int offset, byte x) {
        putByte(o, (long)offset, x);
    }

    // 从 1.4.1 开始，将 32 位偏移参数转换为 long。
    /**
     * @deprecated As of 1.4.1, cast the 32-bit offset argument to a long.
     * See {@link #staticFieldOffset}.
     */
    @Deprecated
    public short getShort(Object o, int offset) {
        return getShort(o, (long)offset);
    }

    // 从 1.4.1 开始，将 32 位偏移参数转换为 long。
    /**
     * @deprecated As of 1.4.1, cast the 32-bit offset argument to a long.
     * See {@link #staticFieldOffset}.
     */
    @Deprecated
    public void putShort(Object o, int offset, short x) {
        putShort(o, (long)offset, x);
    }

    // 从 1.4.1 开始，将 32 位偏移参数转换为 long。
    /**
     * @deprecated As of 1.4.1, cast the 32-bit offset argument to a long.
     * See {@link #staticFieldOffset}.
     */
    @Deprecated
    public char getChar(Object o, int offset) {
        return getChar(o, (long)offset);
    }

    // 从 1.4.1 开始，将 32 位偏移参数转换为 long。
    /**
     * @deprecated As of 1.4.1, cast the 32-bit offset argument to a long.
     * See {@link #staticFieldOffset}.
     */
    @Deprecated
    public void putChar(Object o, int offset, char x) {
        putChar(o, (long)offset, x);
    }

    // 从 1.4.1 开始，将 32 位偏移参数转换为 long。
    /**
     * @deprecated As of 1.4.1, cast the 32-bit offset argument to a long.
     * See {@link #staticFieldOffset}.
     */
    @Deprecated
    public long getLong(Object o, int offset) {
        return getLong(o, (long)offset);
    }

    // 从 1.4.1 开始，将 32 位偏移参数转换为 long。
    /**
     * @deprecated As of 1.4.1, cast the 32-bit offset argument to a long.
     * See {@link #staticFieldOffset}.
     */
    @Deprecated
    public void putLong(Object o, int offset, long x) {
        putLong(o, (long)offset, x);
    }

    // 从 1.4.1 开始，将 32 位偏移参数转换为 long。
    /**
     * @deprecated As of 1.4.1, cast the 32-bit offset argument to a long.
     * See {@link #staticFieldOffset}.
     */
    @Deprecated
    public float getFloat(Object o, int offset) {
        return getFloat(o, (long)offset);
    }

    // 从 1.4.1 开始，将 32 位偏移参数转换为 long。
    /**
     * @deprecated As of 1.4.1, cast the 32-bit offset argument to a long.
     * See {@link #staticFieldOffset}.
     */
    @Deprecated
    public void putFloat(Object o, int offset, float x) {
        putFloat(o, (long)offset, x);
    }

    // 从 1.4.1 开始，将 32 位偏移参数转换为 long。
    /**
     * @deprecated As of 1.4.1, cast the 32-bit offset argument to a long.
     * See {@link #staticFieldOffset}.
     */
    @Deprecated
    public double getDouble(Object o, int offset) {
        return getDouble(o, (long)offset);
    }

    // 从 1.4.1 开始，将 32 位偏移参数转换为 long。
    /**
     * @deprecated As of 1.4.1, cast the 32-bit offset argument to a long.
     * See {@link #staticFieldOffset}.
     */
    @Deprecated
    public void putDouble(Object o, int offset, double x) {
        putDouble(o, (long)offset, x);
    }

    // 这些对 C 堆中的值起作用。
    // These work on values in the C heap.

    // 从给定的内存地址获取一个值。 如果地址为零，或不指向从 {@link #allocateMemory} 获得的块，结果未定义。
    /**
     * Fetches a value from a given memory address.  If the address is zero, or
     * does not point into a block obtained from {@link #allocateMemory}, the
     * results are undefined.
     *
     * @see #allocateMemory
     */
    public native byte    getByte(long address);

    // 将值存储到给定的内存地址中。 如果地址为零，或不指向从 {@link #allocateMemory} 获得的块，结果未定义。
    /**
     * Stores a value into a given memory address.  If the address is zero, or
     * does not point into a block obtained from {@link #allocateMemory}, the
     * results are undefined.
     *
     * @see #getByte(long)
     */
    public native void    putByte(long address, byte x);

    /** @see #getByte(long) */
    public native short   getShort(long address);
    /** @see #putByte(long, byte) */
    public native void    putShort(long address, short x);
    /** @see #getByte(long) */
    public native char    getChar(long address);
    /** @see #putByte(long, byte) */
    public native void    putChar(long address, char x);
    /** @see #getByte(long) */
    public native int     getInt(long address);
    /** @see #putByte(long, byte) */
    public native void    putInt(long address, int x);
    /** @see #getByte(long) */
    public native long    getLong(long address);
    /** @see #putByte(long, byte) */
    public native void    putLong(long address, long x);
    /** @see #getByte(long) */
    public native float   getFloat(long address);
    /** @see #putByte(long, byte) */
    public native void    putFloat(long address, float x);
    /** @see #getByte(long) */
    public native double  getDouble(long address);
    /** @see #putByte(long, byte) */
    public native void    putDouble(long address, double x);

    /**
     * 20210808
     * A. 从给定的内存地址获取本机指针。如果地址为零，或不指向从 {@link #allocateMemory} 获得的块，则结果未定义。
     * B. 如果本机指针小于 64 位宽，则将其作为无符号数扩展为 Java long。 指针可以由任何给定的字节偏移量索引，只需将该偏移量（作为简单整数）添加到表示指针的 long 中即可。
     *    从目标地址实际读取的字节数可以通过咨询 {@link #addressSize} 来确定。
     */
    /**
     * A.
     * Fetches a native pointer from a given memory address.  If the address is
     * zero, or does not point into a block obtained from {@link
     * #allocateMemory}, the results are undefined.
     *
     * B.
     * <p> If the native pointer is less than 64 bits wide, it is extended as
     * an unsigned number to a Java long.  The pointer may be indexed by any
     * given byte offset, simply by adding that offset (as a simple integer) to
     * the long representing the pointer.  The number of bytes actually read
     * from the target address maybe determined by consulting {@link
     * #addressSize}.
     *
     * @see #allocateMemory
     */
    public native long getAddress(long address);

    /**
     * 20210808
     * A. 将本机指针存储到给定的内存地址。 如果地址为零，或不指向从 {@link #allocateMemory} 获得的块，则结果未定义。
     * B. 实际写入目标地址的字节数可以通过咨询 {@link #addressSize} 来确定。
     */
    /**
     * A.
     * Stores a native pointer into a given memory address.  If the address is
     * zero, or does not point into a block obtained from {@link
     * #allocateMemory}, the results are undefined.
     *
     * B.
     * <p> The number of bytes actually written at the target address maybe
     * determined by consulting {@link #addressSize}.
     *
     * @see #getAddress(long)
     */
    public native void putAddress(long address, long x);

    // malloc、realloc、free 的包装器：
    /// wrappers for malloc, realloc, free:

    /**
     * 20210808
     * 分配一个新的本机内存块，以字节为单位给定大小。内存内容未初始化；它们通常是垃圾。生成的本机指针永远不会为零，并将针对所有值类型对齐。
     * 通过调用 {@link #freeMemory} 处理此内存，或使用 {@link #reallocateMemory} 调整其大小。
     */
    /**
     * Allocates a new block of native memory, of the given size in bytes.  The
     * contents of the memory are uninitialized; they will generally be
     * garbage.  The resulting native pointer will never be zero, and will be
     * aligned for all value types.  Dispose of this memory by calling {@link
     * #freeMemory}, or resize it with {@link #reallocateMemory}.
     *
     * @throws IllegalArgumentException if the size is negative or too large
     *         for the native size_t type
     *
     * @throws OutOfMemoryError if the allocation is refused by the system
     *
     * @see #getByte(long)
     * @see #putByte(long, byte)
     */
    public native long allocateMemory(long bytes);

    /**
     * 20210808
     * 将新的本机内存块调整为给定的字节大小。超过旧块大小的新块的内容未初始化；它们通常是垃圾。当且仅当请求的大小为零时，生成的本机指针将为零。
     * 生成的本机指针将针对所有值类型对齐。通过调用 {@link #freeMemory}处理此内存，或使用 {@link #reallocateMemory} 调整其大小。 传递给此方法的地址可能为空，
     * 在这种情况下将执行分配。
     */
    /**
     * Resizes a new block of native memory, to the given size in bytes.  The
     * contents of the new block past the size of the old block are
     * uninitialized; they will generally be garbage.  The resulting native
     * pointer will be zero if and only if the requested size is zero.  The
     * resulting native pointer will be aligned for all value types.  Dispose
     * of this memory by calling {@link #freeMemory}, or resize it with {@link
     * #reallocateMemory}.  The address passed to this method may be null, in
     * which case an allocation will be performed.
     *
     * @throws IllegalArgumentException if the size is negative or too large
     *         for the native size_t type
     *
     * @throws OutOfMemoryError if the allocation is refused by the system
     *
     * @see #allocateMemory
     */
    public native long reallocateMemory(long address, long bytes);

    /**
     * 20210808
     * A. 将给定内存块中的所有字节设置为固定值（通常为零）。
     * B. 此方法通过两个参数确定块的基地址，因此它提供（实际上）双寄存器寻址模式，如 {@link #getInt(Object,long)} 中所述。 当对象引用为空时，偏移量提供一个绝对基地址。
     * C. 存储在大小由地址和长度参数确定的相干（原子）单元中。如果有效地址和长度都是偶数模 8，则存储以“long”单位进行。
     * 如果有效地址和长度是（分别）模 4 或 2，则存储以“int”或“short”为单位进行。
     */
    /**
     * A.
     * Sets all bytes in a given block of memory to a fixed value
     * (usually zero).
     *
     * B.
     * <p>This method determines a block's base address by means of two parameters,
     * and so it provides (in effect) a <em>double-register</em> addressing mode,
     * as discussed in {@link #getInt(Object,long)}.  When the object reference is null,
     * the offset supplies an absolute base address.
     *
     * C.
     * <p>The stores are in coherent (atomic) units of a size determined
     * by the address and length parameters.  If the effective address and
     * length are all even modulo 8, the stores take place in 'long' units.
     * If the effective address and length are (resp.) even modulo 4 or 2,
     * the stores take place in units of 'int' or 'short'.
     *
     * @since 1.7
     */
    // 将给定内存块中的所有字节设置为固定值(通常为零), 当对象引用为空时, 偏移量提供一个绝对基地址
    public native void setMemory(Object o, long offset, long bytes, byte value);

    /**
     * 20210808
     * A. 将给定内存块中的所有字节设置为固定值（通常为零）。 这提供了单寄存器寻址模式，如 {@link #getInt(Object,long)} 中所述。
     * B. 等效于 setMemory(null, address, bytes, value)。
     */
    /**
     * A.
     * Sets all bytes in a given block of memory to a fixed value
     * (usually zero).  This provides a <em>single-register</em> addressing mode,
     * as discussed in {@link #getInt(Object,long)}.
     *
     * B.
     * <p>Equivalent to <code>setMemory(null, address, bytes, value)</code>.
     */
    // 将给定内存块中的所有字节设置为固定值(通常为零), 当对象引用为空时, 偏移量提供一个绝对基地址
    public void setMemory(long address, long bytes, byte value) {
        setMemory(null, address, bytes, value);
    }

    /**
     * 20210808
     * A. 将给定内存块中的所有字节设置为另一个块的副本。
     * B. 此方法通过两个参数确定每个块的基地址，因此它提供（实际上）双寄存器寻址模式，如 {@link #getInt(Object,long)} 中所述。 当对象引用为空时，偏移量提供一个绝对基地址。
     * C. 传输是在大小由地址和长度参数确定的相干（原子）单元中进行的。 如果有效地址和长度都是偶数模 8，则传输以“long”单位进行。
     *    如果有效地址和长度是（分别）模 4 或 2，则传输以“int”或“short”为单位进行。
     */
    /**
     * A.
     * Sets all bytes in a given block of memory to a copy of another
     * block.
     *
     * B.
     * <p>This method determines each block's base address by means of two parameters,
     * and so it provides (in effect) a <em>double-register</em> addressing mode,
     * as discussed in {@link #getInt(Object,long)}.  When the object reference is null,
     * the offset supplies an absolute base address.
     *
     * C.
     * <p>The transfers are in coherent (atomic) units of a size determined
     * by the address and length parameters.  If the effective addresses and
     * length are all even modulo 8, the transfer takes place in 'long' units.
     * If the effective addresses and length are (resp.) even modulo 4 or 2,
     * the transfer takes place in units of 'int' or 'short'.
     *
     * @since 1.7
     */
    // 将给定内存块中的所有字节设置为另一个块的副本, 当对象引用为空时, 偏移量提供一个绝对基地址
    public native void copyMemory(Object srcBase, long srcOffset,
                                  Object destBase, long destOffset,
                                  long bytes);
    /**
     * 20210808
     * 将给定内存块中的所有字节设置为另一个块的副本。 这提供了单寄存器寻址模式，如 {@link #getInt(Object,long)} 中所述。
     * 等效于 copyMemory(null, srcAddress, null, destAddress, bytes)。
     */
    /**
     * Sets all bytes in a given block of memory to a copy of another
     * block.  This provides a <em>single-register</em> addressing mode,
     * as discussed in {@link #getInt(Object,long)}.
     *
     * Equivalent to <code>copyMemory(null, srcAddress, null, destAddress, bytes)</code>.
     */
    // 将给定内存块中的所有字节设置为另一个块的副本, 当对象引用为空时, 偏移量提供一个绝对基地址
    public void copyMemory(long srcAddress, long destAddress, long bytes) {
        copyMemory(null, srcAddress, null, destAddress, bytes);
    }

    /**
     * 20210808
     * 处理从 {@link 获得的本机内存块#allocateMemory} 或 {@link #reallocateMemory}。 地址传递给此方法可能为空，在这种情况下不采取任何行动。
     */
    /**
     * Disposes of a block of native memory, as obtained from {@link
     * #allocateMemory} or {@link #reallocateMemory}.  The address passed to
     * this method may be null, in which case no action is taken.
     *
     * @see #allocateMemory
     */
    // 释放本地内存块
    public native void freeMemory(long address);

    // 随机查询
    /// random queries

    /**
     * 20210808
     * 该常量不同于所有从 {@link #staticFieldOffset}、{@link #objectFieldOffset} 或 {@link #arrayBaseOffset} 返回的结果。
     */
    /**
     * This constant differs from all results that will ever be returned from
     * {@link #staticFieldOffset}, {@link #objectFieldOffset},
     * or {@link #arrayBaseOffset}.
     */
    public static final int INVALID_FIELD_OFFSET   = -1;

    /**
     * 20120808
     * 返回字段的偏移量，截断为 32 位。 该方法实现如下：
     * public int fieldOffset(Field f) {
     *     if (Modifier.isStatic(f.getModifiers()))
     *         return (int) staticFieldOffset(f);
     *     else
     *         return (int) objectFieldOffset(f);
     * }
     * 从 1.4.1 开始，对静态字段使用 {@link #staticFieldOffset}，对非静态字段使用 {@link #objectFieldOffset}。
     */
    /**
     * Returns the offset of a field, truncated to 32 bits.
     * This method is implemented as follows:
     * <blockquote><pre>
     * public int fieldOffset(Field f) {
     *     if (Modifier.isStatic(f.getModifiers()))
     *         return (int) staticFieldOffset(f);
     *     else
     *         return (int) objectFieldOffset(f);
     * }
     * </pre></blockquote>
     * @deprecated As of 1.4.1, use {@link #staticFieldOffset} for static
     * fields and {@link #objectFieldOffset} for non-static fields.
     */
    // 返回字段的32位偏移量
    @Deprecated
    public int fieldOffset(Field f) {
        if (Modifier.isStatic(f.getModifiers()))
            return (int) staticFieldOffset(f);
        else
            return (int) objectFieldOffset(f);
    }

    /**
     * 20210808
     * 返回访问给定类中某个静态字段的基地址。 该方法实现如下：
     * public Object staticFieldBase(Class c) {
     *     Field[] fields = c.getDeclaredFields();
     *     for (int i = 0; i < fields.length; i++) {
     *         if (Modifier.isStatic(fields[i].getModifiers())) {
     *             return staticFieldBase(fields[i]);
     *         }
     *     }
     *     return null;
     * }
     * 从 1.4.1 开始，使用 {@link #staticFieldBase(Field)} 获取与特定 {@link Field} 相关的基数。 此方法仅适用于将给定类的所有静态数据存储在一个位置的 JVM。
     */
    /**
     * Returns the base address for accessing some static field
     * in the given class.  This method is implemented as follows:
     * <blockquote><pre>
     * public Object staticFieldBase(Class c) {
     *     Field[] fields = c.getDeclaredFields();
     *     for (int i = 0; i < fields.length; i++) {
     *         if (Modifier.isStatic(fields[i].getModifiers())) {
     *             return staticFieldBase(fields[i]);
     *         }
     *     }
     *     return null;
     * }
     * </pre></blockquote>
     * @deprecated As of 1.4.1, use {@link #staticFieldBase(Field)}
     * to obtain the base pertaining to a specific {@link Field}.
     * This method works only for JVMs which store all statics
     * for a given class in one place.
     */
    // 返回访问给定类中某个静态字段的基地址
    @Deprecated
    public Object staticFieldBase(Class<?> c) {
        Field[] fields = c.getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            if (Modifier.isStatic(fields[i].getModifiers())) {
                return staticFieldBase(fields[i]);
            }
        }
        return null;
    }

    /**
     * 20210728
     * A. 报告给定字段在其类的存储分配中的位置。不要期望对这个偏移量执行任何类型的算术；它只是一个传递给不安全堆内存访问器的cookie。
     * B. 任何给定的字段将始终具有相同的偏移量和基数，并且同一类的两个不同字段永远不会具有相同的偏移量和基数。
     * C. 从1.4.1开始，字段的偏移量表示为长值，尽管Sun JVM不使用最高有效32位。但是，将静态字段存储在绝对地址的JVM实现可以使用长偏移量和空基指针以
     *    {@link #getInt(Object,long)}可用的形式表示字段位置。因此，将移植到64位平台上的此类JVM的代码必须保留静态字段偏移的所有位。
     */
    /**
     * A.
     * Report the location of a given field in the storage allocation of its
     * class.  Do not expect to perform any sort of arithmetic on this offset;
     * it is just a cookie which is passed to the unsafe heap memory accessors.
     *
     * B.
     * <p>Any given field will always have the same offset and base, and no
     * two distinct fields of the same class will ever have the same offset
     * and base.
     *
     * C.
     * <p>As of 1.4.1, offsets for fields are represented as long values,
     * although the Sun JVM does not use the most significant 32 bits.
     * However, JVM implementations which store static fields at absolute
     * addresses can use long offsets and null base pointers to express
     * the field locations in a form usable by {@link #getInt(Object,long)}.
     * Therefore, code which will be ported to such JVMs on 64-bit platforms
     * must preserve all bits of static field offsets.
     *
     * @see #getInt(Object, long)
     */
    // 返回给定静态字段的位置, 任何给定的字段将始终具有相同的偏移量, 并且同一类的两个不同字段永远不会具有相同的偏移量和基数
    public native long staticFieldOffset(Field f);

    /**
     * 20210808
     * A. 结合 {@link #staticFieldBase} 报告给定静态字段的位置。
     * B. 不要期望对这个偏移量执行任何类型的算术； 它只是一个传递给不安全堆内存访问器的 cookie。
     * C. 任何给定的字段将始终具有相同的偏移量，并且同一类的两个不同的字段永远不会具有相同的偏移量。
     * D. 从 1.4.1 开始，字段的偏移量表示为long值，尽管 Sun JVM 不使用最高有效 32 位。 很难想象 JVM 技术需要多于几位来编码非数组对象内的偏移量，
     *    但是，为了与此类中的其他方法保持一致，此方法将其结果报告为长值。 @see #getInt(Object, long)
     */
    /**
     * A.
     * Report the location of a given static field, in conjunction with {@link
     * #staticFieldBase}.
     *
     * B.
     * <p>Do not expect to perform any sort of arithmetic on this offset;
     * it is just a cookie which is passed to the unsafe heap memory accessors.
     *
     * C.
     * <p>Any given field will always have the same offset, and no two distinct
     * fields of the same class will ever have the same offset.
     *
     * D.
     * <p>As of 1.4.1, offsets for fields are represented as long values,
     * although the Sun JVM does not use the most significant 32 bits.
     * It is hard to imagine a JVM technology which needs more than
     * a few bits to encode an offset within a non-array object,
     * However, for consistency with other methods in this class,
     * this method reports its result as a long value.
     * @see #getInt(Object, long)
     */
    // 返回给定字段在其类的存储分配中的位置, 任何给定的字段将始终具有相同的偏移量, 并且同一类的两个不同字段永远不会具有相同的偏移量和基数
    public native long objectFieldOffset(Field f);

    /**
     * 20210808
     * A. 结合 {@link #staticFieldOffset} 报告给定静态字段的位置。
     * B. 获取基本“对象”，如果有的话，可以通过 {@link #getInt(Object, long)} 之类的方法访问给定类的静态字段。 该值可能为空。
     *    这个值可能引用一个对象，它是一个“cookie”，不能保证是一个真正的对象，它不应该以任何方式使用，除了作为此类中 get 和 put 例程的参数。
     */
    /**
     * A.
     * Report the location of a given static field, in conjunction with {@link
     * #staticFieldOffset}.
     *
     * B.
     * <p>Fetch the base "Object", if any, with which static fields of the
     * given class can be accessed via methods like {@link #getInt(Object,
     * long)}.  This value may be null.  This value may refer to an object
     * which is a "cookie", not guaranteed to be a real Object, and it should
     * not be used in any way except as argument to the get and put routines in
     * this class.
     */
    // 获取给定静态字段的基本“对象”(可能引用一个对象, 它是一个“cookie”, 不能保证是一个真正的对象, 它不应该以任何方式使用, 除了作为此类中get和put例程的参数), 如果有的话,可以通过 {@link #getInt(Object, long)} 之类的方法访问给定类的静态字段
    public native Object staticFieldBase(Field f);

    /**
     * 20210808
     * 检测给定的类是否需要初始化。这通常与获取类的静态字段基数一起使用。
     */
    /**
     * Detect if the given class may need to be initialized. This is often
     * needed in conjunction with obtaining the static field base of a
     * class.
     *
     * // 仅当调用 {@code ensureClassInitialized} 无效时才为 false
     * @return false only if a call to {@code ensureClassInitialized} would have no effect
     */
    // 检测给定的类是否需要初始化
    public native boolean shouldBeInitialized(Class<?> c);

    /**
     * 20210808
     * 确保给定的类已初始化。这通常与获取类的静态字段基数一起使用。
     */
    /**
     * Ensure the given class has been initialized. This is often
     * needed in conjunction with obtaining the static field base of a
     * class.
     */
    // 确保给定的类已初始化
    public native void ensureClassInitialized(Class<?> c);

    /**
     * 20210808
     * 报告给定数组类的存储分配中第一个元素的偏移量。 如果 {@link #arrayIndexScale} 为同一个类返回一个非零值，
     * 您可以使用该比例因子和这个基偏移量来形成新的偏移量来访问给定类的数组元素。
     */
    /**
     * Report the offset of the first element in the storage allocation of a
     * given array class.  If {@link #arrayIndexScale} returns a non-zero value
     * for the same class, you may use that scale factor, together with this
     * base offset, to form new offsets to access elements of arrays of the
     * given class.
     *
     * @see #getInt(Object, long)
     * @see #putInt(Object, long, int)
     */
    // 返回给定数组Class的存储分配中第一个元素的偏移量
    public native int arrayBaseOffset(Class<?> arrayClass);

    /** The value of {@code arrayBaseOffset(boolean[].class)} */
    public static final int ARRAY_BOOLEAN_BASE_OFFSET
            = theUnsafe.arrayBaseOffset(boolean[].class);

    /** The value of {@code arrayBaseOffset(byte[].class)} */
    public static final int ARRAY_BYTE_BASE_OFFSET
            = theUnsafe.arrayBaseOffset(byte[].class);

    /** The value of {@code arrayBaseOffset(short[].class)} */
    public static final int ARRAY_SHORT_BASE_OFFSET
            = theUnsafe.arrayBaseOffset(short[].class);

    /** The value of {@code arrayBaseOffset(char[].class)} */
    public static final int ARRAY_CHAR_BASE_OFFSET
            = theUnsafe.arrayBaseOffset(char[].class);

    /** The value of {@code arrayBaseOffset(int[].class)} */
    public static final int ARRAY_INT_BASE_OFFSET
            = theUnsafe.arrayBaseOffset(int[].class);

    /** The value of {@code arrayBaseOffset(long[].class)} */
    public static final int ARRAY_LONG_BASE_OFFSET
            = theUnsafe.arrayBaseOffset(long[].class);

    /** The value of {@code arrayBaseOffset(float[].class)} */
    public static final int ARRAY_FLOAT_BASE_OFFSET
            = theUnsafe.arrayBaseOffset(float[].class);

    /** The value of {@code arrayBaseOffset(double[].class)} */
    public static final int ARRAY_DOUBLE_BASE_OFFSET
            = theUnsafe.arrayBaseOffset(double[].class);

    /** The value of {@code arrayBaseOffset(Object[].class)} */
    public static final int ARRAY_OBJECT_BASE_OFFSET
            = theUnsafe.arrayBaseOffset(Object[].class);

    /**
     * 20210619
     * 报告在给定数组类的存储分配中寻址元素的比例因子。
     * 但是，“narrow”类型的数组通常无法与{@link #getByte(Object, int)} 等访问器一起正常工作，因此此类类的比例因子报告为零。
     */
    /**
     * Report the scale factor for addressing elements in the storage
     * allocation of a given array class.  However, arrays of "narrow" types
     * will generally not work properly with accessors like {@link
     * #getByte(Object, int)}, so the scale factor for such classes is reported
     * as zero.
     *
     * @see #arrayBaseOffset
     * @see #getInt(Object, long)
     * @see #putInt(Object, long, int)
     */
    // 返回在给定数组类的存储分配中寻址元素的比例因子
    public native int arrayIndexScale(Class<?> arrayClass);

    /** The value of {@code arrayIndexScale(boolean[].class)} */
    public static final int ARRAY_BOOLEAN_INDEX_SCALE
            = theUnsafe.arrayIndexScale(boolean[].class);

    /** The value of {@code arrayIndexScale(byte[].class)} */
    public static final int ARRAY_BYTE_INDEX_SCALE
            = theUnsafe.arrayIndexScale(byte[].class);

    /** The value of {@code arrayIndexScale(short[].class)} */
    public static final int ARRAY_SHORT_INDEX_SCALE
            = theUnsafe.arrayIndexScale(short[].class);

    /** The value of {@code arrayIndexScale(char[].class)} */
    public static final int ARRAY_CHAR_INDEX_SCALE
            = theUnsafe.arrayIndexScale(char[].class);

    /** The value of {@code arrayIndexScale(int[].class)} */
    public static final int ARRAY_INT_INDEX_SCALE
            = theUnsafe.arrayIndexScale(int[].class);

    /** The value of {@code arrayIndexScale(long[].class)} */
    public static final int ARRAY_LONG_INDEX_SCALE
            = theUnsafe.arrayIndexScale(long[].class);

    /** The value of {@code arrayIndexScale(float[].class)} */
    public static final int ARRAY_FLOAT_INDEX_SCALE
            = theUnsafe.arrayIndexScale(float[].class);

    /** The value of {@code arrayIndexScale(double[].class)} */
    public static final int ARRAY_DOUBLE_INDEX_SCALE
            = theUnsafe.arrayIndexScale(double[].class);

    /** The value of {@code arrayIndexScale(Object[].class)} */
    public static final int ARRAY_OBJECT_INDEX_SCALE
            = theUnsafe.arrayIndexScale(Object[].class);

    /**
     * 20210808
     * 报告本地指针的字节大小，如通过 {@link 存储的#putAddress}。 该值将是 4 或 8。请注意，确定其他原始类型（如存储在本机内存块中）完全由他们的信息内容决定。
     */
    /**
     * Report the size in bytes of a native pointer, as stored via {@link
     * #putAddress}.  This value will be either 4 or 8.  Note that the sizes of
     * other primitive types (as stored in native memory blocks) is determined
     * fully by their information content.
     */
    // 获取本地指针的字节大小
    public native int addressSize();

    // {@code addressSize()} 的值
    /** The value of {@code addressSize()} */
    public static final int ADDRESS_SIZE = theUnsafe.addressSize();

    // 报告本机内存页面的字节大小（无论是什么）。 该值将始终是 2 的幂。
    /**
     * Report the size in bytes of a native memory page (whatever that is).
     * This value will always be a power of two.
     */
    // 获取本地内存页面的字节大小
    public native int pageSize();

    // 来自 JNI 的随机可信操作：
    /// random trusted operations from JNI:

    // 告诉 VM 定义一个类，而不进行安全检查。 默认情况下，类加载器和保护域来自调用者的类。
    /**
     * Tell the VM to define a class, without security checks.  By default, the
     * class loader and protection domain come from the caller's class.
     */
    // 告诉VM定义一个类
    public native Class<?> defineClass(String name, byte[] b, int off, int len,
                                       ClassLoader loader,
                                       ProtectionDomain protectionDomain);

    /**
     * 20210808
     * A. 定义一个类，但不要让类加载器或系统字典知道它。
     * B. 对于每个 CP 条目，相应的 CP 补丁必须为空或具有与其标签匹配的格式：
     *      a. Integer、Long、Float、Double：对应的包装对象类型来自 java.lang
     *      b. utf8：一个字符串（如果用作签名或名称，必须有合适的语法）
     *      c. Class: 任何 java.lang.Class 对象
     *      d. String：任何对象（不仅仅是 java.lang.String）
     *      e. InterfaceMethodRef: (NYI) 调用该调用站点参数的方法句柄
     */
    /**
     * A.
     * Define a class but do not make it known to the class loader or system dictionary.
     *
     * B.
     * <p>
     * For each CP entry, the corresponding CP patch must either be null or have
     * the a format that matches its tag:
     * <ul>
     * <li>Integer, Long, Float, Double: the corresponding wrapper object type from java.lang
     * <li>Utf8: a string (must have suitable syntax if used as signature or name)
     * <li>Class: any java.lang.Class object
     * <li>String: any object (not just a java.lang.String)
     * <li>InterfaceMethodRef: (NYI) a method handle to invoke on that call site's arguments
     * </ul>
     *
     * // 用于链接、访问控制、保护域和类加载器的上下文
     * @params hostClass context for linkage, access control, protection domain, and class loader
     *
     * // 类文件的字节数
     * @params data      bytes of a class file
     *
     * // 如果存在非空条目，它们将替换数据中的相应 CP 条目
     * @params cpPatches where non-null entries exist, they replace corresponding CP entries in data
     */
    // 定义一个类，但不要让类加载器或系统字典知道它
    public native Class<?> defineAnonymousClass(Class<?> hostClass, byte[] data, Object[] cpPatches);

    // 分配一个实例，但不运行任何构造函数。如果尚未初始化类，则初始化该类。
    /** Allocate an instance but do not run any constructor.Initializes the class if it has not yet been. */
    // 分配一个实例，但不运行任何构造函数。如果尚未初始化类，则初始化该类
    public native Object allocateInstance(Class<?> cls) throws InstantiationException;

    // 锁定对象。 它必须通过 {@link #monitorExit} 解锁。
    /** Lock the object.  It must get unlocked via {@link #monitorExit}. */
    // 锁定对象, 锁定后必须通过 {@link #monitorExit} 解锁
    @Deprecated
    public native void monitorEnter(Object o);

    // 解锁对象。 它必须已通过 {@link #monitorEnter} 锁定。
    /**
     * Unlock the object.  It must have been locked via {@link
     * #monitorEnter}.
     */
    // 解锁对象, 必须先通过 {@link #monitorEnter} 锁定
    @Deprecated
    public native void monitorExit(Object o);

    // 试图锁定对象。 返回 true 或 false 以指示锁定是否成功。 如果是，则必须通过 {@link #monitorExit} 解锁对象。
    /**
     * Tries to lock the object.  Returns true or false to indicate
     * whether the lock succeeded.  If it did, the object must be
     * unlocked via {@link #monitorExit}.
     */
    // 试图锁定对象。 返回 true 或 false 以指示锁定是否成功。 它必须通过 {@link #monitorExit} 解锁。
    @Deprecated
    public native boolean tryMonitorEnter(Object o);

    // 在不告诉验证者的情况下抛出异常。
    /** Throw the exception without telling the verifier. */
    // 在不告诉验证者的情况下抛出异常
    public native void throwException(Throwable ee);

    /**
     * Atomically update Java variable to <tt>x</tt> if it is currently
     * holding <tt>expected</tt>.
     * @return <tt>true</tt> if successful
     */
    // 如果当前保持预期状态，则将Java变量原子更新为x。
    public final native boolean compareAndSwapObject(Object o, long offset, Object expected, Object x);

    /**
     * 20210808
     * 如果当前保持预期，则将 Java 变量原子更新为 x。
     */
    /**
     * Atomically update Java variable to <tt>x</tt> if it is currently
     * holding <tt>expected</tt>.
     *
     * @return <tt>true</tt> if successful
     */
    // 如果当前保持预期，则将 Java 变量原子更新为 x
    public final native boolean compareAndSwapInt(Object o, long offset, int expected, int x);

    // 如果当前保持预期，则将 Java 变量原子更新为 x。
    /**
     * Atomically update Java variable to <tt>x</tt> if it is currently
     * holding <tt>expected</tt>.
     *
     * @return <tt>true</tt> if successful
     */
    // 如果当前保持预期，则将 Java 变量原子更新为 x
    public final native boolean compareAndSwapLong(Object o, long offset, long expected, long x);

    // 从给定的 Java 变量中获取引用值，具有 volatile 加载语义。 否则等同于 {@link #getObject(Object, long)}
    /**
     * Fetches a reference value from a given Java variable, with volatile
     * load semantics. Otherwise identical to {@link #getObject(Object, long)}
     */
    // 从给定具有 volatile 加载语义的Java变量中获取引用值, 否则等同于 {@link #getObject(Object, long)}, 不保证存储对其他线程的立即可见性
    public native Object getObjectVolatile(Object o, long offset);

    /**
     * 20210619
     * 使用volatile存储语义将引用值存储到给定的Java变量中。否则等同于{@link #putObject(Object, long, Object)}
     */
    /**
     * Stores a reference value into a given Java variable, with
     * volatile store semantics. Otherwise identical to {@link #putObject(Object, long, Object)}
     */
    // 使用volatile存储语义将引用值存储到给定的Java变量中。否则等同于{@link #putObject(Object, long, Object)}, 不保证存储对其他线程的立即可见性
    public native void    putObjectVolatile(Object o, long offset, Object x);

    /** Volatile version of {@link #getInt(Object, long)}  */
    public native int     getIntVolatile(Object o, long offset);

    /** Volatile version of {@link #putInt(Object, long, int)}  */
    public native void    putIntVolatile(Object o, long offset, int x);

    /** Volatile version of {@link #getBoolean(Object, long)}  */
    public native boolean getBooleanVolatile(Object o, long offset);

    /** Volatile version of {@link #putBoolean(Object, long, boolean)}  */
    public native void    putBooleanVolatile(Object o, long offset, boolean x);

    /** Volatile version of {@link #getByte(Object, long)}  */
    public native byte    getByteVolatile(Object o, long offset);

    /** Volatile version of {@link #putByte(Object, long, byte)}  */
    public native void    putByteVolatile(Object o, long offset, byte x);

    /** Volatile version of {@link #getShort(Object, long)}  */
    public native short   getShortVolatile(Object o, long offset);

    /** Volatile version of {@link #putShort(Object, long, short)}  */
    public native void    putShortVolatile(Object o, long offset, short x);

    /** Volatile version of {@link #getChar(Object, long)}  */
    public native char    getCharVolatile(Object o, long offset);

    /** Volatile version of {@link #putChar(Object, long, char)}  */
    public native void    putCharVolatile(Object o, long offset, char x);

    /** Volatile version of {@link #getLong(Object, long)}  */
    public native long    getLongVolatile(Object o, long offset);

    /** Volatile version of {@link #putLong(Object, long, long)}  */
    public native void    putLongVolatile(Object o, long offset, long x);

    /** Volatile version of {@link #getFloat(Object, long)}  */
    public native float   getFloatVolatile(Object o, long offset);

    /** Volatile version of {@link #putFloat(Object, long, float)}  */
    public native void    putFloatVolatile(Object o, long offset, float x);

    /** Volatile version of {@link #getDouble(Object, long)}  */
    public native double  getDoubleVolatile(Object o, long offset);

    /** Volatile version of {@link #putDouble(Object, long, double)}  */
    public native void    putDoubleVolatile(Object o, long offset, double x);

    /**
     * 20210701
     * {@link #putObjectVolatile(Object, long, Object)} 的版本不保证存储对其他线程的立即可见性。
     * 此方法通常仅在基础字段是 Java volatile字段（或数组单元格，否则只能使用易失性访问进行访问）时才有用。
     */
    /**
     * Version of {@link #putObjectVolatile(Object, long, Object)}
     * that does not guarantee immediate visibility of the store to
     * other threads. This method is generally only useful if the
     * underlying field is a Java volatile (or if an array cell, one
     * that is otherwise only accessed using volatile accesses).
     */
    // 使用volatile存储语义将引用值存储到给定的Java变量中。否则等同于{@link #putObject(Object, long, Object)}, 该方法不保证存储对其他线程的立即可见性
    public native void    putOrderedObject(Object o, long offset, Object x);

    // {@link #putIntVolatile(Object, long, int)} 的有序/懒惰版本
    /** Ordered/Lazy version of {@link #putIntVolatile(Object, long, int)}  */
    // 使用volatile存储语义将引用值存储到给定的Java变量中。否则等同于{@link #putObject(Object, long, Object)}, 该方法不保证存储对其他线程的立即可见性
    public native void    putOrderedInt(Object o, long offset, int x);

    // {@link #putLongVolatile(Object, long, long)} 的有序/懒惰版本
    /** Ordered/Lazy version of {@link #putLongVolatile(Object, long, long)} */
    // 使用volatile存储语义将引用值存储到给定的Java变量中。否则等同于{@link #putObject(Object, long, Object)}, 该方法不保证存储对其他线程的立即可见性
    public native void    putOrderedLong(Object o, long offset, long x);

    /**
     * 20210728
     * 取消阻塞在park上阻塞的给定线程，
     * 或者，如果它未被阻塞，则导致后续调用 park 不被阻塞。
     * 注意：这个操作是“不安全的”，仅仅是因为调用者必须以某种方式确保线程没有被破坏。
     * 当从Java调用时，通常不需要任何特殊的东西来确保这一点（其中通常会有对线程的实时引用），但是当从本机代码调用时，这几乎不是自动的。
     */
    /**
     * Unblock the given thread blocked on <tt>park</tt>, or, if it is
     * not blocked, cause the subsequent call to <tt>park</tt> not to
     * block.  Note: this operation is "unsafe" solely because the
     * caller must somehow ensure that the thread has not been
     * destroyed. Nothing special is usually required to ensure this
     * when called from Java (in which there will ordinarily be a live
     * reference to the thread) but this is not nearly-automatically
     * so when calling from native code.
     * @param thread the thread to unpark.
     *
     */
    // 唤醒在park上阻塞的指定线程, 如果该线程并未阻塞, 则它在后续调用park时不会被阻塞
    public native void unpark(Object thread);

    /**
     * 20210728
     * 阻塞当前线程，在发生平衡解除停放、平衡解除停放已经发生、或线程被中断时返回，
     * 或者，如果不是绝对的且时间不为零，则给定的时间纳秒已经过去，
     * 或者如果是绝对的，则给定的期限以毫秒为单位，自Epoch过去或虚假（即，无缘无故返回）。
     * 注意：这个操作在Unsafe类中只是因为unpark是，所以把它放在其他地方会很奇怪。
     */
    /**
     * Block current thread, returning when a balancing
     * <tt>unpark</tt> occurs, or a balancing <tt>unpark</tt> has
     * already occurred, or the thread is interrupted, or, if not
     * absolute and time is not zero, the given time nanoseconds have
     * elapsed, or if absolute, the given deadline in milliseconds
     * since Epoch has passed, or spuriously (i.e., returning for no
     * "reason"). Note: This operation is in the Unsafe class only
     * because <tt>unpark</tt> is, so it would be strange to place it
     * elsewhere.
     */
    // 阻塞当前线程, 直到当前线程unpark被调用、被中断、time时间过去(非绝对时为纳秒, 绝对时为毫秒, 为0时代表无限阻塞)
    public native void park(boolean isAbsolute, long time);

    /**
     * 20210808
     * 获取分配给可用处理器的系统运行队列中的平均负载在不同时间段内的平均值。 此方法检索给定的 nelem 样本并分配给给定 loadavg 数组的元素。
     * 系统最多施加 3 个样本，分别代表过去 1、5 和 15 分钟的平均值。
     */
    /**
     * Gets the load average in the system run queue assigned
     * to the available processors averaged over various periods of time.
     * This method retrieves the given <tt>nelem</tt> samples and
     * assigns to the elements of the given <tt>loadavg</tt> array.
     * The system imposes a maximum of 3 samples, representing
     * averages over the last 1,  5,  and  15 minutes, respectively.
     *
     * // loadavg 一个双倍大小的 nelems 数组
     * @params loadavg an array of double of size nelems
     *
     * // nelems 要检索的样本数和必须是 1 到 3。
     * @params nelems the number of samples to be retrieved and
     *         must be 1 to 3.
     *
     * // 实际检索到的样本数量； 如果无法获得平均负载，则为 -1。
     * @return the number of samples actually retrieved; or -1
     *         if the load average is unobtainable.
     */
    // 获取分配给可用处理器的系统运行队列中的平均负载在不同时间段内的平均值
    public native int getLoadAverage(double[] loadavg, int nelems);

    // 以下包含在不支持本机指令的平台上使用的基于 CAS 的 Java 实现
    // The following contain CAS-based Java implementations used on
    // platforms not supporting native instructions

    // 以原子方式将给定值添加到给定对象o中给定偏移量处的字段或数组元素的当前值。
    /**
     * Atomically adds the given value to the current value of a field
     * or array element within the given object <code>o</code>
     * at the given <code>offset</code>.
     *
     * @param o object/array to update the field/element in
     * @param offset field/element offset
     * @param delta the value to add
     * @return the previous value
     * @since 1.8
     */
    // 以原子方式将给定值添加到给定对象o中给定偏移量处的字段或数组元素的当前值
    public final int getAndAddInt(Object o, long offset, int delta) {
        int v;
        do {
            v = getIntVolatile(o, offset);
        } while (!compareAndSwapInt(o, offset, v, v + delta));
        return v;
    }

    // 以原子方式将给定值添加到给定对象o中给定偏移量处的字段或数组元素的当前值。
    /**
     * Atomically adds the given value to the current value of a field
     * or array element within the given object <code>o</code>
     * at the given <code>offset</code>.
     *
     * @param o object/array to update the field/element in
     * @param offset field/element offset
     * @param delta the value to add
     * @return the previous value
     * @since 1.8
     */
    // 以原子方式将给定值添加到给定对象o中给定偏移量处的字段或数组元素的当前值
    public final long getAndAddLong(Object o, long offset, long delta) {
        long v;
        do {
            v = getLongVolatile(o, offset);
        } while (!compareAndSwapLong(o, offset, v, v + delta));
        return v;
    }

    // 在给定的偏移量处以原子方式将给定值与给定对象 o 内的字段或数组元素的当前值进行交换。
    /**
     * Atomically exchanges the given value with the current value of
     * a field or array element within the given object <code>o</code>
     * at the given <code>offset</code>.
     *
     * @param o object/array to update the field/element in
     * @param offset field/element offset
     * @param newValue new value
     * @return the previous value
     * @since 1.8
     */
    // 在给定的偏移量处以原子方式将给定值与给定对象 o 内的字段或数组元素的当前值进行交换
    public final int getAndSetInt(Object o, long offset, int newValue) {
        int v;
        do {
            v = getIntVolatile(o, offset);
        } while (!compareAndSwapInt(o, offset, v, newValue));
        return v;
    }

    // 在给定的偏移量处以原子方式将给定值与给定对象 o 内的字段或数组元素的当前值进行交换。
    /**
     * Atomically exchanges the given value with the current value of
     * a field or array element within the given object <code>o</code>
     * at the given <code>offset</code>.
     *
     * @param o object/array to update the field/element in
     * @param offset field/element offset
     * @param newValue new value
     * @return the previous value
     * @since 1.8
     */
    // 在给定的偏移量处以原子方式将给定值与给定对象 o 内的字段或数组元素的当前值进行交换
    public final long getAndSetLong(Object o, long offset, long newValue) {
        long v;
        do {
            v = getLongVolatile(o, offset);
        } while (!compareAndSwapLong(o, offset, v, newValue));
        return v;
    }

    // 在给定的偏移量处以原子方式将给定的参考值与给定对象 o 内的字段或数组元素的当前参考值进行交换。
    /**
     * Atomically exchanges the given reference value with the current
     * reference value of a field or array element within the given
     * object <code>o</code> at the given <code>offset</code>.
     *
     * @param o object/array to update the field/element in
     * @param offset field/element offset
     * @param newValue new value
     * @return the previous value
     * @since 1.8
     */
    // 在给定的偏移量处以原子方式将给定的参考值与给定对象 o 内的字段或数组元素的当前参考值进行交换
    public final Object getAndSetObject(Object o, long offset, Object newValue) {
        Object v;
        do {
            v = getObjectVolatile(o, offset);
        } while (!compareAndSwapObject(o, offset, v, newValue));
        return v;
    }

    // 确保在栅栏之前没有对load重新排序，在栅栏后loads或stores。
    /**
     * Ensures lack of reordering of loads before the fence
     * with loads or stores after the fence.
     *
     * @since 1.8
     */
    // 确保在栅栏之前不会对loads重排序, 在栅栏后不会对loads或stores重排序
    public native void loadFence();

    // 确保栅栏前的store不会重新排序, 栅栏后的loads或stores。
    /**
     * Ensures lack of reordering of stores before the fence
     * with loads or stores after the fence.
     *
     * @since 1.8
     */
    // 确保栅栏前不会对stores重排序, 在栅栏后不会对loads或stores重排序
    public native void storeFence();

    // 确保在栅栏之前不会对loads或stores进行重新排序，而在栅栏之后则不会对stores或loads进行重新排序。
    /**
     * Ensures lack of reordering of loads or stores before the fence
     * with loads or stores after the fence.
     *
     * @since 1.8
     */
    // 确保在栅栏之前不会对loads或stores进行重新排序，而在栅栏之后则不会对stores或loads进行重新排序
    public native void fullFence();

    // 抛出非法访问错误； 供虚拟机使用。
    /**
     * Throws IllegalAccessError; for use by the VM.
     *
     * @since 1.8
     */
    // 抛出非法访问错误, 供虚拟机使用
    private static void throwIllegalAccessError() {
       throw new IllegalAccessError();
    }

}
