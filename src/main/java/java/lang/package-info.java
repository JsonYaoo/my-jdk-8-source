/*
 * Copyright (c) 1998, 2006, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

/**
 * 20210725
 * A. 提供对Java编程语言设计至关重要的类。最重要的类是{@code Object}，它是类层次结构的根，以及{@code Class}，它们的实例在运行时代表类。
 * B. 通常需要将原始类型的值表示为对象。包装类 {@code Boolean}、{@code Character}、{@code Integer}、{@code Long}、{@code Float} 和 {@code Double} 用于此目的。
 *    例如，类型为 {@code Double} 的对象包含一个类型为 double 的字段，以这样一种方式表示该值，即对它的引用可以存储在引用类型的变量中。
 *    这些类还提供了许多在原始值之间进行转换的方法，并支持诸如 equals 和 hashCode 等标准方法。 {@code Void} 类是一个不可实例化的类，它持有对表示 void 类型的
 *    {@code Class} 对象的引用。
 * C. {@code Math} 类提供了常用的数学函数，例如正弦、余弦和平方根。{@code String}、{@code StringBuffer} 和 {@code StringBuilder} 类类似地提供了常用的字符串操作。
 * D. 类{@code ClassLoader}、{@code Process}、{@code ProcessBuilder}、{@code Runtime}、{@code SecurityManager} 和 {@code System} 提供管理类动态加载的“系统操作”，
 *    外部进程的创建、主机环境查询（例如一天中的时间）以及安全策略的实施。
 * E. {@code Throwable} 类包含可能由 {@code throw} 语句抛出的对象。 {@code Throwable} 的子类表示错误和异常。
 *
 * 字符编码
 * {@link java.nio.charset.Charset java.nio.charset.Charset} 类的规范描述了字符编码的命名约定以及 Java 平台的每个实现必须支持的标准编码集。
 */
/**
 * A.
 * Provides classes that are fundamental to the design of the Java
 * programming language. The most important classes are {@code
 * Object}, which is the root of the class hierarchy, and {@code
 * Class}, instances of which represent classes at run time.
 *
 * B.
 * <p>Frequently it is necessary to represent a value of primitive
 * type as if it were an object. The wrapper classes {@code Boolean},
 * {@code Character}, {@code Integer}, {@code Long}, {@code Float},
 * and {@code Double} serve this purpose.  An object of type {@code
 * Double}, for example, contains a field whose type is double,
 * representing that value in such a way that a reference to it can be
 * stored in a variable of reference type.  These classes also provide
 * a number of methods for converting among primitive values, as well
 * as supporting such standard methods as equals and hashCode.  The
 * {@code Void} class is a non-instantiable class that holds a
 * reference to a {@code Class} object representing the type void.
 *
 * C.
 * <p>The class {@code Math} provides commonly used mathematical
 * functions such as sine, cosine, and square root. The classes {@code
 * String}, {@code StringBuffer}, and {@code StringBuilder} similarly
 * provide commonly used operations on character strings.
 *
 * D.
 * <p>Classes {@code ClassLoader}, {@code Process}, {@code
 * ProcessBuilder}, {@code Runtime}, {@code SecurityManager}, and
 * {@code System} provide "system operations" that manage the dynamic
 * loading of classes, creation of external processes, host
 * environment inquiries such as the time of day, and enforcement of
 * security policies.
 *
 * E.
 * <p>Class {@code Throwable} encompasses objects that may be thrown
 * by the {@code throw} statement. Subclasses of {@code Throwable}
 * represent errors and exceptions.
 *
 * <a name="charenc"></a>
 * <h3>Character Encodings</h3>
 *
 * F.
 * The specification of the {@link java.nio.charset.Charset
 * java.nio.charset.Charset} class describes the naming conventions
 * for character encodings as well as the set of standard encodings
 * that must be supported by every implementation of the Java
 * platform.
 *
 * @since JDK1.0
 */
package java.lang;
