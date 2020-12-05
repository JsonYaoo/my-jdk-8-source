/*
 * Copyright (c) 1997, 2013, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package java.text;

/**
 * 20201205
 * A. 如果属性具有注解特征，则将Annotation对象用作文本属性值的包装。 这些特征是：
 *      a. 属性所应用的文本范围对于范围的语义至关重要。 这意味着该属性不能应用于它所应用于的文本范围的子范围，并且，如果两个相邻的文本范围对此属性具有相同的值，则该属性仍然
 *         不能应用于整个组合范围（具有该值） 。
 *      b. 如果基础文本发生更改，则该属性或其值通常不再适用。
 * B. 一个例子是附加在句子上的语法信息：对于上一个句子，您可以说“一个例子”是主语，但是不能对“一个”，“例子”或“测试”说同样的话。 更改文本后，语法信息通常会变得无效。
 *    另一个例子是日语阅读信息（yomi）。
 * C. 将属性值包装到Annotation对象中可确保即使属性值相等也不会合并相邻的文本运行，并向文本容器指示如果修改了基础文本则应丢弃该属性。
 */
/**
* A.
* An Annotation object is used as a wrapper for a text attribute value if
* the attribute has annotation characteristics. These characteristics are:
* <ul>
* <li>The text range that the attribute is applied to is critical to the
* semantics of the range. That means, the attribute cannot be applied to subranges
* of the text range that it applies to, and, if two adjacent text ranges have
* the same value for this attribute, the attribute still cannot be applied to
* the combined range as a whole with this value.
* <li>The attribute or its value usually do no longer apply if the underlying text is
* changed.
* </ul>
*
* B.
* An example is grammatical information attached to a sentence:
* For the previous sentence, you can say that "an example"
* is the subject, but you cannot say the same about "an", "example", or "exam".
* When the text is changed, the grammatical information typically becomes invalid.
* Another example is Japanese reading information (yomi).
*
* C.
* <p>
* Wrapping the attribute value into an Annotation object guarantees that
* adjacent text runs don't get merged even if the attribute values are equal,
* and indicates to text containers that the attribute should be discarded if
* the underlying text is modified.
*
* @see AttributedCharacterIterator
* @since 1.2
*/
// 20201205 Annotation注解类型对象, 包装注解属性值 => 可确保即使属性值相等也不会合并相邻的文本运行，并向文本容器指示如果修改了基础文本则应丢弃该属性
public class Annotation {

    /**
     * Constructs an annotation record with the given value, which
     * may be null.
     *
     * @param value the value of the attribute
     */
    public Annotation(Object value) {
        this.value = value;
    }

    /**
     * Returns the value of the attribute, which may be null.
     *
     * @return the value of the attribute
     */
    public Object getValue() {
        return value;
    }

    /**
     * Returns the String representation of this Annotation.
     *
     * @return the {@code String} representation of this {@code Annotation}
     */
    public String toString() {
        return getClass().getName() + "[value=" + value + "]";
    }

    private Object value;

};
