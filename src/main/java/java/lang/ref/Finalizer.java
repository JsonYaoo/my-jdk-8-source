/*
 * Copyright (c) 1997, 2013, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package java.lang.ref;

import java.security.PrivilegedAction;
import java.security.AccessController;
import sun.misc.JavaLangAccess;
import sun.misc.SharedSecrets;
import sun.misc.VM;

/**
 * 20210614
 * 包-私有； 必须与 Reference 类在同一个包中
 */
final class Finalizer extends FinalReference<Object> { /* Package-private; must be in
                                                          same package as the Reference
                                                          class */

    private static ReferenceQueue<Object> queue = new ReferenceQueue<>();
    private static Finalizer unfinalized = null;
    private static final Object lock = new Object();

    private Finalizer
        next = null,
        prev = null;

    private boolean hasBeenFinalized() {
        return (next == this);
    }

    private void add() {
        synchronized (lock) {
            if (unfinalized != null) {
                this.next = unfinalized;
                unfinalized.prev = this;
            }
            unfinalized = this;
        }
    }

    private void remove() {
        synchronized (lock) {
            if (unfinalized == this) {
                if (this.next != null) {
                    unfinalized = this.next;
                } else {
                    unfinalized = this.prev;
                }
            }
            if (this.next != null) {
                this.next.prev = this.prev;
            }
            if (this.prev != null) {
                this.prev.next = this.next;
            }
            this.next = this;   /* Indicates that this has been finalized */
            this.prev = this;
        }
    }

    private Finalizer(Object finalizee) {
        super(finalizee, queue);
        add();
    }

    /* Invoked by VM */
    static void register(Object finalizee) {
        new Finalizer(finalizee);
    }

    private void runFinalizer(JavaLangAccess jla) {
        synchronized (this) {
            if (hasBeenFinalized()) return;
            remove();
        }
        try {
            Object finalizee = this.get();
            if (finalizee != null && !(finalizee instanceof java.lang.Enum)) {
                jla.invokeFinalize(finalizee);

                /* Clear stack slot containing this variable, to decrease
                   the chances of false retention with a conservative GC */
                finalizee = null;
            }
        } catch (Throwable x) { }
        super.clear();
    }

    /**
     * 20210614
     * A. 在系统线程组中为给定的Runnable创建一个特权辅助终结器线程，并等待它完成。
     * B. runFinalization 和 runFinalizersOnExit 都使用此方法。 前一种方法调用所有挂起的终结器，而如果启用了退出终结器，后一种方法会调用所有未调用的终结器。
     * C. 这两种方法可以通过将它们的工作卸载到常规终结器线程并等待该线程完成来实现。然而，创建新线程的优点是它将这些方法的调用者与停滞或死锁的终结器线程隔离开来。
     */
    /*
       A.
       Create a privileged secondary finalizer thread in the system thread
       group for the given Runnable, and wait for it to complete.

       B.
       This method is used by both runFinalization and runFinalizersOnExit.
       The former method invokes all pending finalizers, while the latter
       invokes all uninvoked finalizers if on-exit finalization has been
       enabled.

       C.
       These two methods could have been implemented by offloading their work
       to the regular finalizer thread and waiting for that thread to finish.
       The advantage of creating a fresh thread, however, is that it insulates
       invokers of these methods from a stalled or deadlocked finalizer thread.
     */
    private static void forkSecondaryFinalizer(final Runnable proc) {
        AccessController.doPrivileged(
            new PrivilegedAction<Void>() {
                public Void run() {
                ThreadGroup tg = Thread.currentThread().getThreadGroup();
                for (ThreadGroup tgn = tg;
                     tgn != null;
                     tg = tgn, tgn = tg.getParent());
                Thread sft = new Thread(tg, proc, "Secondary finalizer");
                sft.start();
                try {
                    sft.join();
                } catch (InterruptedException x) {
                    /* Ignore */
                }
                return null;
                }});
    }

    /* Called by Runtime.runFinalization() */
    static void runFinalization() {
        if (!VM.isBooted()) {
            return;
        }

        forkSecondaryFinalizer(new Runnable() {
            private volatile boolean running;
            public void run() {
                if (running)
                    return;
                final JavaLangAccess jla = SharedSecrets.getJavaLangAccess();
                running = true;
                for (;;) {
                    Finalizer f = (Finalizer)queue.poll();
                    if (f == null) break;
                    f.runFinalizer(jla);
                }
            }
        });
    }

    /* Invoked by java.lang.Shutdown */
    static void runAllFinalizers() {
        if (!VM.isBooted()) {
            return;
        }

        forkSecondaryFinalizer(new Runnable() {
            private volatile boolean running;
            public void run() {
                if (running)
                    return;
                final JavaLangAccess jla = SharedSecrets.getJavaLangAccess();
                running = true;
                for (;;) {
                    Finalizer f;
                    synchronized (lock) {
                        f = unfinalized;
                        if (f == null) break;
                        unfinalized = f.next;
                    }
                    f.runFinalizer(jla);
                }}});
    }

    private static class FinalizerThread extends Thread {
        private volatile boolean running;
        FinalizerThread(ThreadGroup g) {
            super(g, "Finalizer");
        }
        public void run() {
            if (running)
                return;

            // Finalizer thread starts before System.initializeSystemClass
            // is called.  Wait until JavaLangAccess is available
            while (!VM.isBooted()) {
                // delay until VM completes initialization
                try {
                    VM.awaitBooted();
                } catch (InterruptedException x) {
                    // ignore and continue
                }
            }
            final JavaLangAccess jla = SharedSecrets.getJavaLangAccess();
            running = true;
            for (;;) {
                try {
                    Finalizer f = (Finalizer)queue.remove();
                    f.runFinalizer(jla);
                } catch (InterruptedException x) {
                    // ignore and continue
                }
            }
        }
    }

    static {
        ThreadGroup tg = Thread.currentThread().getThreadGroup();
        for (ThreadGroup tgn = tg;
             tgn != null;
             tg = tgn, tgn = tg.getParent());
        Thread finalizer = new FinalizerThread(tg);
        finalizer.setPriority(Thread.MAX_PRIORITY - 2);
        finalizer.setDaemon(true);
        finalizer.start();
    }

}
