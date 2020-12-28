/*
 * Copyright (c) 1999, 2005, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package javax.management;

import java.util.concurrent.CopyOnWriteArrayList;  // for Javadoc

/**
 * 20201228
 * A. 由发出通知的MBean实现的接口。 它允许将侦听器注册到MBean作为通知侦听器。
 * B. 通知发送
 * C. 当MBean发出通知时，它将考虑已使用{@link #addNotificationListener addNotificationListener}添加且随后未使用{@link #removeNotificationListener removeNotificationListener}删除的
 *    每个侦听器。 如果该侦听器提供了过滤器，并且该过滤器的{@link NotificationFilter＃isNotificationEnabled isNotificationEnabled}方法返回false，则将忽略该侦听器。 否则，将与通知一起调用
 *    侦听器的{@link NotificationListener＃handleNotification handleNotification}方法，并向{@code addNotificationListener}提供递归对象。
 * D. 如果多次添加同一个侦听器，则将其视为添加的次数相同。 将相同的侦听器与不同的过滤器或递归对象一起添加通常很有用。
 * E. 对于调用过滤器和侦听器方法的线程，此接口的实现可能有所不同。
 * F. 如果过滤器或侦听器的方法调用抛出{@link Exception}，则该异常不应阻止其他侦听器的调用。 但是，如果方法调用抛出{@link Error}，则建议在该点停止通知的处理，并且有可能将{@code Error}
 *    传播给以下对象的发送者：通知，应该这样做。
 * G. 新代码应改用{@link NotificationEmitter}接口。
 * H. 此接口和{@code NotificationBroadcaster}的实现在同步时应格外小心。 特别是，实现在调用侦听器时保持任何锁不是一个好主意。 为了处理在发送通知时侦听器列表可能更改的可能性，
 *    一个好的策略是对此列表使用{@link CopyOnWriteArrayList}。
 */
/**
 * A.
 * <p>Interface implemented by an MBean that emits Notifications. It
 * allows a listener to be registered with the MBean as a notification
 * listener.</p>
 *
 * B.
 * <h3>Notification dispatch</h3>
 *
 * C.
 * <p>When an MBean emits a notification, it considers each listener that has been
 * added with {@link #addNotificationListener addNotificationListener} and not
 * subsequently removed with {@link #removeNotificationListener removeNotificationListener}.
 * If a filter was provided with that listener, and if the filter's
 * {@link NotificationFilter#isNotificationEnabled isNotificationEnabled} method returns
 * false, the listener is ignored.  Otherwise, the listener's
 * {@link NotificationListener#handleNotification handleNotification} method is called with
 * the notification, as well as the handback object that was provided to
 * {@code addNotificationListener}.</p>
 *
 * D.
 * <p>If the same listener is added more than once, it is considered as many times as it was
 * added.  It is often useful to add the same listener with different filters or handback
 * objects.</p>
 *
 * E.
 * <p>Implementations of this interface can differ regarding the thread in which the methods
 * of filters and listeners are called.</p>
 *
 * F.
 * <p>If the method call of a filter or listener throws an {@link Exception}, then that
 * exception should not prevent other listeners from being invoked.  However, if the method
 * call throws an {@link Error}, then it is recommended that processing of the notification
 * stop at that point, and if it is possible to propagate the {@code Error} to the sender of
 * the notification, this should be done.</p>
 *
 * G.
 * <p>New code should use the {@link NotificationEmitter} interface
 * instead.</p>
 *
 * H.
 * <p>Implementations of this interface and of {@code NotificationEmitter}
 * should be careful about synchronization.  In particular, it is not a good
 * idea for an implementation to hold any locks while it is calling a
 * listener.  To deal with the possibility that the list of listeners might
 * change while a notification is being dispatched, a good strategy is to
 * use a {@link CopyOnWriteArrayList} for this list.
 *
 * @since 1.5
 */
public interface NotificationBroadcaster {

    /**
     * Adds a listener to this MBean.
     *
     * @param listener The listener object which will handle the
     * notifications emitted by the broadcaster.
     * @param filter The filter object. If filter is null, no
     * filtering will be performed before handling notifications.
     * @param handback An opaque object to be sent back to the
     * listener when a notification is emitted. This object cannot be
     * used by the Notification broadcaster object. It should be
     * resent unchanged with the notification to the listener.
     *
     * @exception IllegalArgumentException Listener parameter is null.
     *
     * @see #removeNotificationListener
     */
    public void addNotificationListener(NotificationListener listener,
                                        NotificationFilter filter,
                                        Object handback)
            throws java.lang.IllegalArgumentException;

    /**
     * Removes a listener from this MBean.  If the listener
     * has been registered with different handback objects or
     * notification filters, all entries corresponding to the listener
     * will be removed.
     *
     * @param listener A listener that was previously added to this
     * MBean.
     *
     * @exception ListenerNotFoundException The listener is not
     * registered with the MBean.
     *
     * @see #addNotificationListener
     * @see NotificationEmitter#removeNotificationListener
     */
    public void removeNotificationListener(NotificationListener listener)
            throws ListenerNotFoundException;

    /**
     * <p>Returns an array indicating, for each notification this
     * MBean may send, the name of the Java class of the notification
     * and the notification type.</p>
     *
     * <p>It is not illegal for the MBean to send notifications not
     * described in this array.  However, some clients of the MBean
     * server may depend on the array being complete for their correct
     * functioning.</p>
     *
     * @return the array of possible notifications.
     */
    public MBeanNotificationInfo[] getNotificationInfo();
}
