/*
 * Copyright (c) 2004, 2013, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package java.io;

import java.io.IOException;

/**
 * 20201223
 * 可刷新是可刷新数据的目标。 调用flush方法将任何缓冲的输出写入基础流。
 */
/**
 * A <tt>Flushable</tt> is a destination of data that can be flushed.  The
 * flush method is invoked to write any buffered output to the underlying
 * stream.
 *
 * @since 1.5
 */
// 20201223 可刷新是可刷新数据的目标。 调用flush方法将任何缓冲的输出写入基础流。
public interface Flushable {

    /**
     * Flushes this stream by writing any buffered output to the underlying
     * stream.
     *
     * @throws IOException If an I/O error occurs
     */
    void flush() throws IOException;
}
