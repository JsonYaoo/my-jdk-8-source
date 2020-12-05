/*
 * Copyright (c) 2003, 2013, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package java.lang.reflect;

import java.lang.annotation.Annotation;
import java.lang.annotation.AnnotationFormatError;
import java.lang.annotation.Repeatable;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import sun.reflect.annotation.AnnotationSupport;
import sun.reflect.annotation.AnnotationType;

/**
 * 20201203
 * A. 表示当前在此VM中运行的程序的带注解元素。此接口允许以反射方式读取注解。此接口中的方法返回的所有注解都是不可变的和可序列化的。调用方可以修改此接口方法返回的数组，而不影响返回给其他调用方的数组。
 * B. {@link #getAnnotationsByType（Class）}和{@link #getDeclaredAnnotationsByType（Class）}方法支持元素上相同类型的多个注解。如果任一方法的参数是可重复的注解类型（JLS9.6），
 *    则该方法将“浏览”容器注解（JLS9.7）（如果存在），并返回容器内的任何注解。可以在编译时生成容器注解来包装参数类型的多个注解。
 * C. 在整个接口中，术语“directly present”、“indirectly present”、“present”和“associated”用于精确描述由以下方法返回的注解:
 *      a. 如果E具有{@code RuntimeVisibleAnnotations}或{@code RuntimeVisibleParameterAnnotations}或{@code RuntimeVisibleTypeAnnotations}属性，并且该属性包含A，
 *          则注解A将直接出现在元素E上。
 *      b. 如果元素E具有{@code RuntimeVisibleAnnotations}或{@code RuntimeVisibleParameterAnnotations}或{@code RuntimeVisibleTypeAnnotations}属性，
 *         并且A的类型是可重复的，则注解A间接存在于元素E上，并且该属性正好包含一个注解，其value元素包含A，其类型是A类型的包含注解类型。
 *      c. 元素E上存在注解A，如果：
 *          c.1. A直接存在于E上
 *          c.2. 或者在E上没有直接存在的A类型的注解，E是一个类，A的类型是可继承的，A在E的超类中存在
 *      d. 注解A与元素E关联，如果：
 *          d.1. A直接或间接地存在于E上
 *          d.2. A类型的注解没有直接或间接地出现在E上，E是一个类，A的类型是可继承的，A与E的超类相关联
 * D. 下表总结了此接口中不同方法检查的注解类型, 见下表。
 * E. 对于{@code get[Declared]AnnotationsByType（Class<T>）}的调用，计算直接或间接出现在元素E上的注解的顺序，就好像在E上间接出现的注解直接出现在E上而不是它们的容器注解，
 *    按照它们在容器的value元素中出现的顺序注解。
 * F. 如果注解类型T最初是不可重复的，后来被修改为可重复的，那么需要记住几个兼容性问题:
 *      a. T的包含注解类型是TC:
 *          a.1. 将T修改为可重复的是源代码和二进制代码与T的现有用法和TC的现有用法兼容:
 *              a.1.1. 也就是说，为了源代码的兼容性，带有T或TC类型注解的源代码仍然可以编译。为了实现二进制兼容性，带有类型T或类型TC的注解（或具有类型T或类型TC的其他类型的
 *                     使用）的类文件将链接到T的修改版本（如果它们链接到早期版本）。
 *              a.1.2. （注解类型TC可以非正式地充当包含注解类型的动作，在T被修改为可正式重复之前。或者，当T可重复时，TC可以作为一种新类型引入。）
 *      b. 如果元素上存在注解类型TC，并且T被修改为可重复，并将TC作为其包含注解类型，则：
 *          b.1. 对T的更改在行为上与{@code get[Declared]注解（Class<T>）}（用T或TC参数调用）和{@code get[Declared]Annotations（）}方法兼容，因为TC成为T的包含注解类型，
 *               因此方法的结果不会更改
 *          b.2. 对T的更改会更改用T参数调用的{@code get[Declared]AnnotationsByType（Class<T>）}方法的结果，因为这些方法现在将把TC类型的注解识别为T的容器注解，并将“查看”它
 *               以公开T类型的注解
 *      c. 如果元素上存在类型为T的注解，并且使T可重复，并且向元素添加更多类型为T的注解：
 *          c.1. T类型注解的添加既与源代码兼容，又与二进制兼容。
 *          c.2. 添加T类型的注解会更改{@code get[Declared]Annotation（Class<T>）}方法和{@code get[Declared]annotations（）}方法的结果，因为这些方法现在只会看到元素上的
 *               容器注解，而看不到类型为<i>T的注解
 *          c.4. 添加类型T的注解会更改{@code get[declarated]AnnotationsByType（Class<T>）}方法的结果，因为它们的结果将公开类型T的附加注解，而以前它们只公开类型T的单个注解
 * G. 如果此接口中的方法返回的注解包含（直接或间接）引用在此VM中不可访问的类的{@link Class}值成员，则尝试通过对返回的注解调用相关的类返回方法来读取该类将导致
 *    {@link TypeNotPresentException}。
 * H. 类似地，如果注解中的枚举常量不再存在于枚举类型中，则尝试读取枚举值成员将导致{@link EnumConstantNotPresentException}。
 * I. 如果注解类型T是用{@code @Repeatable}注解（其value元素表示类型TC），但TC没有声明返回类型为T{@code []}的{@code value（）}方法，则
 *    {@link java.lang.annotation.AnnotationFormatError}类型的异常被抛出。
 * J. 最后，尝试读取其定义演变不兼容的成员将导致{@link java.lang.annotation.AnnotationTypeMitchException}或{@link java.lang.annotation.IncompleteAnnotationException}。
 */
/**
 * A.
 * Represents an annotated element of the program currently running in this
 * VM.  This interface allows annotations to be read reflectively.  All
 * annotations returned by methods in this interface are immutable and
 * serializable. The arrays returned by methods of this interface may be modified
 * by callers without affecting the arrays returned to other callers.
 *
 * B.
 * <p>The {@link #getAnnotationsByType(Class)} and {@link
 * #getDeclaredAnnotationsByType(Class)} methods support multiple
 * annotations of the same type on an element. If the argument to
 * either method is a repeatable annotation type (JLS 9.6), then the
 * method will "look through" a container annotation (JLS 9.7), if
 * present, and return any annotations inside the container. Container
 * annotations may be generated at compile-time to wrap multiple
 * annotations of the argument type.
 *
 * C.
 * <p>The terms <em>directly present</em>, <em>indirectly present</em>,
 * <em>present</em>, and <em>associated</em> are used throughout this
 * interface to describe precisely which annotations are returned by
 * methods:
 *
 * <ul>
 *
 * <li> An annotation <i>A</i> is <em>directly present</em> on an
 * element <i>E</i> if <i>E</i> has a {@code
 * RuntimeVisibleAnnotations} or {@code
 * RuntimeVisibleParameterAnnotations} or {@code
 * RuntimeVisibleTypeAnnotations} attribute, and the attribute
 * contains <i>A</i>.
 *
 * <li>An annotation <i>A</i> is <em>indirectly present</em> on an
 * element <i>E</i> if <i>E</i> has a {@code RuntimeVisibleAnnotations} or
 * {@code RuntimeVisibleParameterAnnotations} or {@code RuntimeVisibleTypeAnnotations}
 * attribute, and <i>A</i> 's type is repeatable, and the attribute contains
 * exactly one annotation whose value element contains <i>A</i> and whose
 * type is the containing annotation type of <i>A</i> 's type.
 *
 * <li>An annotation <i>A</i> is present on an element <i>E</i> if either:
 *
 * <ul>
 *
 * <li><i>A</i> is directly present on <i>E</i>; or
 *
 * <li>No annotation of <i>A</i> 's type is directly present on
 * <i>E</i>, and <i>E</i> is a class, and <i>A</i> 's type is
 * inheritable, and <i>A</i> is present on the superclass of <i>E</i>.
 *
 * </ul>
 *
 * <li>An annotation <i>A</i> is <em>associated</em> with an element <i>E</i>
 * if either:
 *
 * <ul>
 *
 * <li><i>A</i> is directly or indirectly present on <i>E</i>; or
 *
 * <li>No annotation of <i>A</i> 's type is directly or indirectly
 * present on <i>E</i>, and <i>E</i> is a class, and <i>A</i>'s type
 * is inheritable, and <i>A</i> is associated with the superclass of
 * <i>E</i>.
 *
 * </ul>
 *
 * </ul>
 *
 * D.
 * <p>The table below summarizes which kind of annotation presence
 * different methods in this interface examine.
 *
 * <table border>
 * <caption>Overview of kind of presence detected by different AnnotatedElement methods</caption>
 * <tr><th colspan=2></th><th colspan=4>Kind of Presence</th>
 * <tr><th colspan=2>Method</th><th>Directly Present</th><th>Indirectly Present</th><th>Present</th><th>Associated</th>
 * <tr><td align=right>{@code T}</td><td>{@link #getAnnotation(Class) getAnnotation(Class&lt;T&gt;)}
 * <td></td><td></td><td>X</td><td></td>
 * </tr>
 * <tr><td align=right>{@code Annotation[]}</td><td>{@link #getAnnotations getAnnotations()}
 * <td></td><td></td><td>X</td><td></td>
 * </tr>
 * <tr><td align=right>{@code T[]}</td><td>{@link #getAnnotationsByType(Class) getAnnotationsByType(Class&lt;T&gt;)}
 * <td></td><td></td><td></td><td>X</td>
 * </tr>
 * <tr><td align=right>{@code T}</td><td>{@link #getDeclaredAnnotation(Class) getDeclaredAnnotation(Class&lt;T&gt;)}
 * <td>X</td><td></td><td></td><td></td>
 * </tr>
 * <tr><td align=right>{@code Annotation[]}</td><td>{@link #getDeclaredAnnotations getDeclaredAnnotations()}
 * <td>X</td><td></td><td></td><td></td>
 * </tr>
 * <tr><td align=right>{@code T[]}</td><td>{@link #getDeclaredAnnotationsByType(Class) getDeclaredAnnotationsByType(Class&lt;T&gt;)}
 * <td>X</td><td>X</td><td></td><td></td>
 * </tr>
 * </table>
 *
 * E.
 * <p>For an invocation of {@code get[Declared]AnnotationsByType( Class <
 * T >)}, the order of annotations which are directly or indirectly
 * present on an element <i>E</i> is computed as if indirectly present
 * annotations on <i>E</i> are directly present on <i>E</i> in place
 * of their container annotation, in the order in which they appear in
 * the value element of the container annotation.
 *
 * F.
 * <p>There are several compatibility concerns to keep in mind if an
 * annotation type <i>T</i> is originally <em>not</em> repeatable and
 * later modified to be repeatable.
 *
 * The containing annotation type for <i>T</i> is <i>TC</i>.
 *
 * <ul>
 *
 * <li>Modifying <i>T</i> to be repeatable is source and binary
 * compatible with existing uses of <i>T</i> and with existing uses
 * of <i>TC</i>.
 *
 * That is, for source compatibility, source code with annotations of
 * type <i>T</i> or of type <i>TC</i> will still compile. For binary
 * compatibility, class files with annotations of type <i>T</i> or of
 * type <i>TC</i> (or with other kinds of uses of type <i>T</i> or of
 * type <i>TC</i>) will link against the modified version of <i>T</i>
 * if they linked against the earlier version.
 *
 * (An annotation type <i>TC</i> may informally serve as an acting
 * containing annotation type before <i>T</i> is modified to be
 * formally repeatable. Alternatively, when <i>T</i> is made
 * repeatable, <i>TC</i> can be introduced as a new type.)
 *
 * <li>If an annotation type <i>TC</i> is present on an element, and
 * <i>T</i> is modified to be repeatable with <i>TC</i> as its
 * containing annotation type then:
 *
 * <ul>
 *
 * <li>The change to <i>T</i> is behaviorally compatible with respect
 * to the {@code get[Declared]Annotation(Class<T>)} (called with an
 * argument of <i>T</i> or <i>TC</i>) and {@code
 * get[Declared]Annotations()} methods because the results of the
 * methods will not change due to <i>TC</i> becoming the containing
 * annotation type for <i>T</i>.
 *
 * <li>The change to <i>T</i> changes the results of the {@code
 * get[Declared]AnnotationsByType(Class<T>)} methods called with an
 * argument of <i>T</i>, because those methods will now recognize an
 * annotation of type <i>TC</i> as a container annotation for <i>T</i>
 * and will "look through" it to expose annotations of type <i>T</i>.
 *
 * </ul>
 *
 * <li>If an annotation of type <i>T</i> is present on an
 * element and <i>T</i> is made repeatable and more annotations of
 * type <i>T</i> are added to the element:
 *
 * <ul>
 *
 * <li> The addition of the annotations of type <i>T</i> is both
 * source compatible and binary compatible.
 *
 * <li>The addition of the annotations of type <i>T</i> changes the results
 * of the {@code get[Declared]Annotation(Class<T>)} methods and {@code
 * get[Declared]Annotations()} methods, because those methods will now
 * only see a container annotation on the element and not see an
 * annotation of type <i>T</i>.
 *
 * <li>The addition of the annotations of type <i>T</i> changes the
 * results of the {@code get[Declared]AnnotationsByType(Class<T>)}
 * methods, because their results will expose the additional
 * annotations of type <i>T</i> whereas previously they exposed only a
 * single annotation of type <i>T</i>.
 *
 * </ul>
 *
 * </ul>
 *
 * G.
 * <p>If an annotation returned by a method in this interface contains
 * (directly or indirectly) a {@link Class}-valued member referring to
 * a class that is not accessible in this VM, attempting to read the class
 * by calling the relevant Class-returning method on the returned annotation
 * will result in a {@link TypeNotPresentException}.
 *
 * H.
 * <p>Similarly, attempting to read an enum-valued member will result in
 * a {@link EnumConstantNotPresentException} if the enum constant in the
 * annotation is no longer present in the enum type.
 *
 * I.
 * <p>If an annotation type <i>T</i> is (meta-)annotated with an
 * {@code @Repeatable} annotation whose value element indicates a type
 * <i>TC</i>, but <i>TC</i> does not declare a {@code value()} method
 * with a return type of <i>T</i>{@code []}, then an exception of type
 * {@link java.lang.annotation.AnnotationFormatError} is thrown.
 *
 * J.
 * <p>Finally, attempting to read a member whose definition has evolved
 * incompatibly will result in a {@link
 * java.lang.annotation.AnnotationTypeMismatchException} or an
 * {@link java.lang.annotation.IncompleteAnnotationException}.
 *
 * @see java.lang.EnumConstantNotPresentException
 * @see java.lang.TypeNotPresentException
 * @see AnnotationFormatError
 * @see java.lang.annotation.AnnotationTypeMismatchException
 * @see java.lang.annotation.IncompleteAnnotationException
 * @since 1.5
 * @author Josh Bloch
 */
// 20201204 表示当前在此VM中运行的程序的带注解元素。此接口允许以反射方式读取注解。此接口中的方法返回的所有注解都是不可变的和可序列化的。
public interface AnnotatedElement {

    /**
     * 20201205
     * A. 如果此元素上存在指定类型的注解，则返回true，否则返回false。 设计此方法主要是为了方便访问标记注解。
     * B. 此方法返回的真值等效于：{@code getAnnotation（annotationClass）！= null}
     * C. 默认方法的主体指定为上面的代码。
     */
    /**
     * A.
     * Returns true if an annotation for the specified type
     * is <em>present</em> on this element, else false.  This method
     * is designed primarily for convenient access to marker annotations.
     *
     * B.
     * <p>The truth value returned by this method is equivalent to:
     * {@code getAnnotation(annotationClass) != null}
     *
     * C.
     * <p>The body of the default method is specified to be the code
     * above.
     *
     * @param annotationClass the Class object corresponding to the
     *        annotation type
     * @return true if an annotation for the specified annotation
     *     type is present on this element, else false
     * @throws NullPointerException if the given annotation class is null
     * @since 1.5
     */
    // 20201205 判断此元素上是否存在指定类型的注解
    default boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
        return getAnnotation(annotationClass) != null;
    }

   /**
     * Returns this element's annotation for the specified type if
     * such an annotation is <em>present</em>, else null.
     *
     * @param <T> the type of the annotation to query for and return if present
     * @param annotationClass the Class object corresponding to the
     *        annotation type
     * @return this element's annotation for the specified annotation type if
     *     present on this element, else null
     * @throws NullPointerException if the given annotation class is null
     * @since 1.5
     */
    // 20201205 如果存在这样的注解，则返回指定类型的该元素的注解，否则为null。
    <T extends Annotation> T getAnnotation(Class<T> annotationClass);

    /**
     * Returns annotations that are <em>present</em> on this element.
     *
     * If there are no annotations <em>present</em> on this element, the return
     * value is an array of length 0.
     *
     * The caller of this method is free to modify the returned array; it will
     * have no effect on the arrays returned to other callers.
     *
     * @return annotations present on this element
     * @since 1.5
     */
    Annotation[] getAnnotations();

    /**
     * Returns annotations that are <em>associated</em> with this element.
     *
     * If there are no annotations <em>associated</em> with this element, the return
     * value is an array of length 0.
     *
     * The difference between this method and {@link #getAnnotation(Class)}
     * is that this method detects if its argument is a <em>repeatable
     * annotation type</em> (JLS 9.6), and if so, attempts to find one or
     * more annotations of that type by "looking through" a container
     * annotation.
     *
     * The caller of this method is free to modify the returned array; it will
     * have no effect on the arrays returned to other callers.
     *
     * @implSpec The default implementation first calls {@link
     * #getDeclaredAnnotationsByType(Class)} passing {@code
     * annotationClass} as the argument. If the returned array has
     * length greater than zero, the array is returned. If the returned
     * array is zero-length and this {@code AnnotatedElement} is a
     * class and the argument type is an inheritable annotation type,
     * and the superclass of this {@code AnnotatedElement} is non-null,
     * then the returned result is the result of calling {@link
     * #getAnnotationsByType(Class)} on the superclass with {@code
     * annotationClass} as the argument. Otherwise, a zero-length
     * array is returned.
     *
     * @param <T> the type of the annotation to query for and return if present
     * @param annotationClass the Class object corresponding to the
     *        annotation type
     * @return all this element's annotations for the specified annotation type if
     *     associated with this element, else an array of length zero
     * @throws NullPointerException if the given annotation class is null
     * @since 1.8
     */
    default <T extends Annotation> T[] getAnnotationsByType(Class<T> annotationClass) {
         /*
          * Definition of associated: directly or indirectly present OR
          * neither directly nor indirectly present AND the element is
          * a Class, the annotation type is inheritable, and the
          * annotation type is associated with the superclass of the
          * element.
          */
         T[] result = getDeclaredAnnotationsByType(annotationClass);

         if (result.length == 0 && // Neither directly nor indirectly present
             this instanceof Class && // the element is a class
             AnnotationType.getInstance(annotationClass).isInherited()) { // Inheritable
             Class<?> superClass = ((Class<?>) this).getSuperclass();
             if (superClass != null) {
                 // Determine if the annotation is associated with the
                 // superclass
                 result = superClass.getAnnotationsByType(annotationClass);
             }
         }

         return result;
     }

    /**
     * Returns this element's annotation for the specified type if
     * such an annotation is <em>directly present</em>, else null.
     *
     * This method ignores inherited annotations. (Returns null if no
     * annotations are directly present on this element.)
     *
     * @implSpec The default implementation first performs a null check
     * and then loops over the results of {@link
     * #getDeclaredAnnotations} returning the first annotation whose
     * annotation type matches the argument type.
     *
     * @param <T> the type of the annotation to query for and return if directly present
     * @param annotationClass the Class object corresponding to the
     *        annotation type
     * @return this element's annotation for the specified annotation type if
     *     directly present on this element, else null
     * @throws NullPointerException if the given annotation class is null
     * @since 1.8
     */
    default <T extends Annotation> T getDeclaredAnnotation(Class<T> annotationClass) {
         Objects.requireNonNull(annotationClass);
         // Loop over all directly-present annotations looking for a matching one
         for (Annotation annotation : getDeclaredAnnotations()) {
             if (annotationClass.equals(annotation.annotationType())) {
                 // More robust to do a dynamic cast at runtime instead
                 // of compile-time only.
                 return annotationClass.cast(annotation);
             }
         }
         return null;
     }

    /**
     * Returns this element's annotation(s) for the specified type if
     * such annotations are either <em>directly present</em> or
     * <em>indirectly present</em>. This method ignores inherited
     * annotations.
     *
     * If there are no specified annotations directly or indirectly
     * present on this element, the return value is an array of length
     * 0.
     *
     * The difference between this method and {@link
     * #getDeclaredAnnotation(Class)} is that this method detects if its
     * argument is a <em>repeatable annotation type</em> (JLS 9.6), and if so,
     * attempts to find one or more annotations of that type by "looking
     * through" a container annotation if one is present.
     *
     * The caller of this method is free to modify the returned array; it will
     * have no effect on the arrays returned to other callers.
     *
     * @implSpec The default implementation may call {@link
     * #getDeclaredAnnotation(Class)} one or more times to find a
     * directly present annotation and, if the annotation type is
     * repeatable, to find a container annotation. If annotations of
     * the annotation type {@code annotationClass} are found to be both
     * directly and indirectly present, then {@link
     * #getDeclaredAnnotations()} will get called to determine the
     * order of the elements in the returned array.
     *
     * <p>Alternatively, the default implementation may call {@link
     * #getDeclaredAnnotations()} a single time and the returned array
     * examined for both directly and indirectly present
     * annotations. The results of calling {@link
     * #getDeclaredAnnotations()} are assumed to be consistent with the
     * results of calling {@link #getDeclaredAnnotation(Class)}.
     *
     * @param <T> the type of the annotation to query for and return
     * if directly or indirectly present
     * @param annotationClass the Class object corresponding to the
     *        annotation type
     * @return all this element's annotations for the specified annotation type if
     *     directly or indirectly present on this element, else an array of length zero
     * @throws NullPointerException if the given annotation class is null
     * @since 1.8
     */
    default <T extends Annotation> T[] getDeclaredAnnotationsByType(Class<T> annotationClass) {
        Objects.requireNonNull(annotationClass);
        return AnnotationSupport.
            getDirectlyAndIndirectlyPresent(Arrays.stream(getDeclaredAnnotations()).
                                            collect(Collectors.toMap(Annotation::annotationType,
                                                                     Function.identity(),
                                                                     ((first,second) -> first),
                                                                     LinkedHashMap::new)),
                                            annotationClass);
    }

    /**
     * Returns annotations that are <em>directly present</em> on this element.
     * This method ignores inherited annotations.
     *
     * If there are no annotations <em>directly present</em> on this element,
     * the return value is an array of length 0.
     *
     * The caller of this method is free to modify the returned array; it will
     * have no effect on the arrays returned to other callers.
     *
     * @return annotations directly present on this element
     * @since 1.5
     */
    Annotation[] getDeclaredAnnotations();
}
