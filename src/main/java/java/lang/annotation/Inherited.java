/*
 * Copyright (c) 2003, 2013, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package java.lang.annotation;

/**
 * 20201205
 * A. 指示注解类型是自动继承的。 如果注解类型声明中存在继承的元注解，并且用户在类声明中查询了注解类型，并且该类声明中没有该类型的注解，则将自动查询该类的超类以获取注解类型。
 *    重复此过程，直到找到该类型的注解，或到达类层次结构（对象）的顶部为止。 如果没有超类对此类型进行注解，则查询将指示所讨论的类没有此类注解。
 * B. 请注意，如果带注解的类型用于注解除类之外的任何内容，则此元注解类型无效。 另请注意，此元注解仅使注解从超类继承； 已实现的接口上的注解无效。
 */
/**
 * A.
 * Indicates that an annotation type is automatically inherited.  If
 * an Inherited meta-annotation is present on an annotation type
 * declaration, and the user queries the annotation type on a class
 * declaration, and the class declaration has no annotation for this type,
 * then the class's superclass will automatically be queried for the
 * annotation type.  This process will be repeated until an annotation for this
 * type is found, or the top of the class hierarchy (Object)
 * is reached.  If no superclass has an annotation for this type, then
 * the query will indicate that the class in question has no such annotation.
 *
 * B.
 * <p>Note that this meta-annotation type has no effect if the annotated
 * type is used to annotate anything other than a class.  Note also
 * that this meta-annotation only causes annotations to be inherited
 * from superclasses; annotations on implemented interfaces have no
 * effect.
 *
 * @author  Joshua Bloch
 * @since 1.5
 * @jls 9.6.3.3 @Inherited
 */
// 20201205 Inherited注解仅使注解从超类继承； 已实现的接口上的注解无效。
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface Inherited {
}
