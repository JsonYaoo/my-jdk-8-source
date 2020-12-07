/*
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.sun.javadoc;

/**
 * 20201207
 * A. 表示通配符类型参数。 示例包括：
 *      {@code <?>}
 *      {@code <? extends E>}
 *      {@code <? super T>}
 * B. 通配符类型可以具有明确的extends界限或明确的super界限，也可以没有，但不能同时具有。
 */
/**
 * A.
 * Represents a wildcard type argument. Examples include:
 * <pre>
 * {@code <?>}
 * {@code <? extends E>}
 * {@code <? super T>}
 * </pre>
 *
 * B.
 * A wildcard type can have explicit <i>extends</i> bounds
 * or explicit <i>super</i> bounds or neither, but not both.
 *
 * @author Scott Seligman
 * @since 1.5
 */
// 20201207 表示通配符类型参数: {@code <?>} | {@code <? extends E>} | {@code <? super T>}
public interface WildcardType extends Type {

    /**
     * Return the upper bounds of this wildcard type argument
     * as given by the <i>extends</i> clause.
     * Return an empty array if no such bounds are explicitly given.
     *
     * @return the extends bounds of this wildcard type argument
     */
    Type[] extendsBounds();

    /**
     * Return the lower bounds of this wildcard type argument
     * as given by the <i>super</i> clause.
     * Return an empty array if no such bounds are explicitly given.
     *
     * @return the super bounds of this wildcard type argument
     */
    Type[] superBounds();
}
