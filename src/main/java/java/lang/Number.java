/*
 * Copyright (c) 1994, 2011, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package java.lang;

/**
 * 20210819
 * A. 抽象类 {@code Number} 是平台类的超类，表示可转换为基本类型 {@code byte}、{@code double}、{@code float}、{@code int}、{@code long}和{@code short}。
 * B. 从特定 {@code Number} 实现的数值到给定原始类型的转换的特定语义由所讨论的 {@code Number} 实现定义。
 * C. 对于平台类，转换通常类似于在 Java™ 语言规范中定义的用于在原始类型之间转换的缩小原语转换或扩大原语转换。
 *    因此，转换可能会丢失有关数值整体大小的信息，可能会丢失精度，甚至可能返回与输入符号不同的结果。
 * D. 有关转换的详细信息，请参阅给定 {@code Number} 实现的文档。
 */
/**
 * A.
 * The abstract class {@code Number} is the superclass of platform
 * classes representing numeric values that are convertible to the
 * primitive types {@code byte}, {@code double}, {@code float}, {@code
 * int}, {@code long}, and {@code short}.
 *
 * B.
 * The specific semantics of the conversion from the numeric value of
 * a particular {@code Number} implementation to a given primitive
 * type is defined by the {@code Number} implementation in question.
 *
 * C.
 * For platform classes, the conversion is often analogous to a
 * narrowing primitive conversion or a widening primitive conversion
 * as defining in <cite>The Java&trade; Language Specification</cite>
 * for converting between primitive types.  Therefore, conversions may
 * lose information about the overall magnitude of a numeric value, may
 * lose precision, and may even return a result of a different sign
 * than the input.
 *
 * D.
 * See the documentation of a given {@code Number} implementation for
 * conversion details.
 *
 * @author      Lee Boynton
 * @author      Arthur van Hoff
 * @jls 5.1.2 Widening Primitive Conversions
 * @jls 5.1.3 Narrowing Primitive Conversions
 * @since   JDK1.0
 */
public abstract class Number implements java.io.Serializable {

    // 以 {@code int} 形式返回指定数字的值，这可能涉及舍入或截断。
    /**
     * Returns the value of the specified number as an {@code int},
     * which may involve rounding or truncation.
     *
     * // 转换为 {@code int} 类型后此对象表示的数值。
     * @return  the numeric value represented by this object after conversion
     *          to type {@code int}.
     */
    // 以 {@code int} 形式返回指定数字的值，这可能涉及舍入或截断
    public abstract int intValue();

    /**
     * Returns the value of the specified number as a {@code long},
     * which may involve rounding or truncation.
     *
     * @return  the numeric value represented by this object after conversion
     *          to type {@code long}.
     */
    // 以 {@code long} 形式返回指定数字的值，这可能涉及舍入或截断。
    public abstract long longValue();

    /**
     * Returns the value of the specified number as a {@code float},
     * which may involve rounding.
     *
     * @return  the numeric value represented by this object after conversion
     *          to type {@code float}.
     */
    // 以 {@code float} 形式返回指定数字的值，这可能涉及四舍五入。
    public abstract float floatValue();

    /**
     * Returns the value of the specified number as a {@code double},
     * which may involve rounding.
     *
     * @return  the numeric value represented by this object after conversion
     *          to type {@code double}.
     */
    // 以 {@code double} 形式返回指定数字的值，这可能涉及四舍五入。
    public abstract double doubleValue();

    /**
     * 20210819
     * A. 将指定数字的值作为 {@code byte} 返回，这可能涉及舍入或截断。
     * B. 此实现将 {@link #intValue} 转换为 {@code byte} 的结果返回。
     */
    /**
     * A.
     * Returns the value of the specified number as a {@code byte},
     * which may involve rounding or truncation.
     *
     * B.
     * <p>This implementation returns the result of {@link #intValue} cast
     * to a {@code byte}.
     *
     * @return  the numeric value represented by this object after conversion
     *          to type {@code byte}.
     * @since   JDK1.1
     */
    // 将指定数字的值作为 {@code byte} 返回，这可能涉及舍入或截断, 此实现将 {@link #intValue} 转换为 {@code byte} 的结果返回
    public byte byteValue() {
        return (byte)intValue();
    }

    /**
     * 20210819
     * A. 将指定数字的值作为 {@code short} 返回，这可能涉及舍入或截断。
     * B. 此实现将 {@link #intValue} 转换为 {@code short} 的结果返回。
     */
    /**
     * A.
     * Returns the value of the specified number as a {@code short},
     * which may involve rounding or truncation.
     *
     * B.
     * <p>This implementation returns the result of {@link #intValue} cast
     * to a {@code short}.
     *
     * @return  the numeric value represented by this object after conversion
     *          to type {@code short}.
     * @since   JDK1.1
     */
    // 将指定数字的值作为 {@code short} 返回，这可能涉及舍入或截断，此实现将 {@link #intValue} 转换为 {@code short} 的结果返回
    public short shortValue() {
        return (short)intValue();
    }

    /** use serialVersionUID from JDK 1.0.2 for interoperability */
    private static final long serialVersionUID = -8742448824652078965L;
}
