/*
 * Copyright (c) 1995, 2008, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package java.lang;

/**
 * 20210727
 * A. 当调用（不推荐使用的）{@link Thread#stop()} 方法时，会在受害者线程中抛出一个 {@code ThreadDeath} 实例。
 * B. 仅当应用程序在异步终止后必须进行清理时，它才应捕获此类的实例。如果 {@code ThreadDeath}被方法捕获，重要的是将其重新抛出，以便线程实际死亡。
 * C. 如果 {@code ThreadDeath}从未被捕获，则{@linkplain ThreadGroup#uncaughtException顶级错误处理程序}不会打印出消息。
 * D. {@code ThreadDeath}类特别是{@code Error}而不是{@code Exception} 的子类，即使它是“正常发生”，因为许多应用程序捕获所有出现的{@code Exception}然后丢弃exception。
 */
/**
 * A.
 * An instance of {@code ThreadDeath} is thrown in the victim thread
 * when the (deprecated) {@link Thread#stop()} method is invoked.
 *
 * B.
 * <p>An application should catch instances of this class only if it
 * must clean up after being terminated asynchronously.  If
 * {@code ThreadDeath} is caught by a method, it is important that it
 * be rethrown so that the thread actually dies.
 *
 * C.
 * <p>The {@linkplain ThreadGroup#uncaughtException top-level error
 * handler} does not print out a message if {@code ThreadDeath} is
 * never caught.
 *
 * D.
 * <p>The class {@code ThreadDeath} is specifically a subclass of
 * {@code Error} rather than {@code Exception}, even though it is a
 * "normal occurrence", because many applications catch all
 * occurrences of {@code Exception} and then discard the exception.
 *
 * @since   JDK1.0
 */

public class ThreadDeath extends Error {
    private static final long serialVersionUID = -4417128565033088268L;
}
