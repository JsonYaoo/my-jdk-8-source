/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package java.lang;

import java.lang.annotation.*;

/**
 * 20210725
 * A. 用于指示接口类型声明, 旨在成为Java语言规范定义的功能接口的信息性注释类型。
 * B. 从概念上讲，函数式接口只有一个抽象方法。 由于{@linkplain java.lang.reflect.Method#isDefault()默认方法}有一个实现，它们不是抽象的。
 *    如果接口声明了一个抽象方法覆盖{@code java.lang.Object} 的公共方法之一，这也不会计入接口的抽象方法计数，
 *    因为接口的任何实现都将具有{@code java.lang.Object} 或其他地方。
 * C. 请注意，可以使用lambda表达式、方法引用或构造函数引用创建函数式接口的实例。
 * D. 如果使用此注释类型对类型进行注释，则编译器需要生成错误消息，除非：
 *      a. 该类型是接口类型，而不是注释类型、枚举或类。
 *      b. 带注释的类型满足功能接口的要求。
 * E. 但是，无论接口声明中是否存在{@code FunctionalInterface}注释，编译器都会将满足函数式接口定义的任何接口视为函数式接口。
 */
/**
 * A.
 * An informative annotation type used to indicate that an interface
 * type declaration is intended to be a <i>functional interface</i> as
 * defined by the Java Language Specification.
 *
 * B.
 * Conceptually, a functional interface has exactly one abstract
 * method.  Since {@linkplain java.lang.reflect.Method#isDefault()
 * default methods} have an implementation, they are not abstract.  If
 * an interface declares an abstract method overriding one of the
 * public methods of {@code java.lang.Object}, that also does
 * <em>not</em> count toward the interface's abstract method count
 * since any implementation of the interface will have an
 * implementation from {@code java.lang.Object} or elsewhere.
 *
 * C.
 * <p>Note that instances of functional interfaces can be created with
 * lambda expressions, method references, or constructor references.
 *
 * D.
 * <p>If a type is annotated with this annotation type, compilers are
 * required to generate an error message unless:
 *
 * <ul>
 * <li> The type is an interface type and not an annotation type, enum, or class.
 * <li> The annotated type satisfies the requirements of a functional interface.
 * </ul>
 *
 * E.
 * <p>However, the compiler will treat any interface meeting the
 * definition of a functional interface as a functional interface
 * regardless of whether or not a {@code FunctionalInterface}
 * annotation is present on the interface declaration.
 *
 * @jls 4.3.2. The Class Object
 * @jls 9.8 Functional Interfaces
 * @jls 9.4.3 Interface Method Body
 * @since 1.8
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface FunctionalInterface {}
