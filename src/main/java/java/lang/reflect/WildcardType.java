/*
 * Copyright (c) 2003, 2004, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package java.lang.reflect;

/**
 * WildcardType represents a wildcard type expression, such as
 * {@code ?}, {@code ? extends Number}, or {@code ? super Integer}.
 *
 * @since 1.5
 */
// 20201207 WildcardType表示通配符类型表达式，例如{@code ?}，{@ code ? extends Number}或{@code ? super Integer}。
public interface WildcardType extends Type {

    /**
     * 20201207
     * A. 返回代表此类型变量上限的{@code Type}对象数组。 请注意，如果未明确声明上限，则上限为{@code Object}。
     * B. 对于每个上限B：
     *      a. 如果B是参数化类型或类型变量，则会创建它（有关参数化类型的创建过程的详细信息，请参见{@link java.lang.reflect.ParameterizedType ParameterizedType}）。
     *      b. 否则，B被解决。
     */
    /**
     * A.
     * Returns an array of {@code Type} objects representing the  upper
     * bound(s) of this type variable.  Note that if no upper bound is
     * explicitly declared, the upper bound is {@code Object}.
     *
     * B.
     * <p>For each upper bound B :
     * <ul>
     *  <li>if B is a parameterized type or a type variable, it is created,
     *  (see {@link java.lang.reflect.ParameterizedType ParameterizedType}
     *  for the details of the creation process for parameterized types).
     *  <li>Otherwise, B is resolved.
     * </ul>
     *
     * @return an array of Types representing the upper bound(s) of this type variable // 20201207 类型数组，表示此类型变量的上限
     * @throws TypeNotPresentException if any of the
     *     bounds refers to a non-existent type declaration
     * @throws MalformedParameterizedTypeException if any of the
     *     bounds refer to a parameterized type that cannot be instantiated
     *     for any reason
     */
    // 20201207 返回代表此类型变量上限的{@code Type}对象数组。 请注意，如果未明确声明上限，则上限为{@code Object}。
    Type[] getUpperBounds();

    /**
     * 20201207
     * A. 返回代表此类型变量下限的{@code Type}对象数组。 请注意，如果未明确声明下界，则下界为{@code null}的类型。 在这种情况下，将返回零长度数组。
     * B. 对于每个下限B：
     *      a. 如果B是参数化类型或类型变量，则会创建它（有关参数化类型的创建过程的详细信息，请参见{@link java.lang.reflect.ParameterizedType ParameterizedType}）。
     *      b. 否则，B被解决。
     */
    /**
     * A.
     * Returns an array of {@code Type} objects representing the
     * lower bound(s) of this type variable.  Note that if no lower bound is
     * explicitly declared, the lower bound is the type of {@code null}.
     * In this case, a zero length array is returned.
     *
     * B.
     * <p>For each lower bound B :
     * <ul>
     *   <li>if B is a parameterized type or a type variable, it is created,
     *  (see {@link java.lang.reflect.ParameterizedType ParameterizedType}
     *  for the details of the creation process for parameterized types).
     *   <li>Otherwise, B is resolved.
     * </ul>
     *
     * @return an array of Types representing the lower bound(s) of this type variable // 20201207 Types数组，表示此类型变量的下限
     * @throws TypeNotPresentException if any of the
     *     bounds refers to a non-existent type declaration
     * @throws MalformedParameterizedTypeException if any of the
     *     bounds refer to a parameterized type that cannot be instantiated
     *     for any reason
     */
    // 20201207 返回代表此类型变量下限的{@code Type}对象数组。 请注意，如果未明确声明下界，则下界为{@code null}的类型。 在这种情况下，将返回零长度数组。
    Type[] getLowerBounds();
    // 20201207 一个或多个? 符合语言规范； 目前只有一个，但是此API允许泛化。
    // one or many? Up to language spec; currently only one, but this API allows for generalization.
}
