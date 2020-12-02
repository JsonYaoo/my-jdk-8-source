/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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
package java.util.function;

/**
 * 20201202
 * A. 表示结果的提供者。
 * B. 每一次都有不同的supplier被调用或没有新的结果。
 * C. 这是一个<a href=“package-summary.html“>函数接口</a>，其函数方法是{@link #get（）}。
 */
/**
 * A.
 * Represents a supplier of results.
 *
 * B.
 * <p>There is no requirement that a new or distinct result be returned each
 * time the supplier is invoked.
 *
 * C.
 * <p>This is a <a href="package-summary.html">functional interface</a>
 * whose functional method is {@link #get()}.
 *
 * @param <T> the type of results supplied by this supplier // 20201202 supplier提供的结果类型
 *
 * @since 1.8
 */
// 20201202 表示结果的提供者
@FunctionalInterface
public interface Supplier<T> {

    /**
     * Gets a result.
     *
     * @return a result
     */
    // 20201202 得到一个结果。
    T get();
}
