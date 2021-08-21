/*
 * Copyright (c) 1994, 2005, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package sun.misc;

/**
 * 20210808
 * A. Lock 类为锁提供了一个简单、有用的接口。 与同步访问对象的监视器不同，锁同步访问任意一组资源（对象、方法、变量等）。
 * B. 使用锁的程序员必须负责明确定义其使用的语义，并应对异常情况进行死锁避免。
 * C. 例如，如果您想用锁保护一组方法调用，而其中一个方法可能会抛出异常，您必须准备好类似于以下示例释放锁：
 *      class SomeClass {
 *          Lock myLock = new Lock();
 *
 *          void someMethod() {
 *              myLock.lock();
 *              try {
 *                  StartOperation();
 *                  ContinueOperation();
 *                  EndOperation();
 *              } finally {
 *                  myLock.unlock();
 *              }
 *          }
 *      }
 */
/**
 * A.
 * The Lock class provides a simple, useful interface to a lock.
 * Unlike monitors which synchronize access to an object, locks
 * synchronize access to an arbitrary set of resources (objects,
 * methods, variables, etc.). <p>
 *
 * B.
 * The programmer using locks must be responsible for clearly defining
 * the semantics of their use and should handle deadlock avoidance in
 * the face of exceptions. <p>
 *
 * C.
 * For example, if you want to protect a set of method invocations with
 * a lock, and one of the methods may throw an exception, you must be
 * prepared to release the lock similarly to the following example:
 * <pre>
 *      class SomeClass {
 *          Lock myLock = new Lock();

 *          void someMethod() {
 *              myLock.lock();
 *              try {
 *                  StartOperation();
 *                  ContinueOperation();
 *                  EndOperation();
 *              } finally {
 *                  myLock.unlock();
 *              }
 *          }
 *      }
 * </pre>
 *
 * @author      Peter King
 */
public class Lock {

    private boolean locked = false;

    // 创建一个最初没有锁定的锁。
    /**
     * Create a lock, which is initially not locked.
     */
    public Lock () { }

    // 获得锁。如果其他人拥有该锁，请等待它被释放，然后再次尝试获取它。在获取锁之前，此方法不会返回。
    /**
     * Acquire the lock.  If someone else has the lock, wait until it
     * has been freed, and then try to acquire it again.  This method
     * will not return until the lock has been acquired.
     *
     * @exception  java.lang.InterruptedException if any thread has
     *               interrupted this thread.
     */
    public final synchronized void lock() throws InterruptedException {
        while (locked) {
            wait();
        }

        locked = true;
    }

    // 释放锁。如果其他人正在等待锁定，则会收到通知，以便他们可以再次尝试获取锁定。
    /**
     * Release the lock.  If someone else is waiting for the lock, the
     * will be notitified so they can try to acquire the lock again.
     */
    public final synchronized void unlock() {
        locked = false;
        notifyAll();
    }
}
