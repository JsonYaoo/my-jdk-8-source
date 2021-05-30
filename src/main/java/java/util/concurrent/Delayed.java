/*
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util.concurrent;

/**
 * 20210523
 * A. 混合样式界面，用于标记在给定延迟后应作用的对象。
 * B. 此接口的实现必须定义一个{@code compareTo}方法，该方法必须提供与其{@code getDelay}方法一致的排序。
 */

/**
 * A.
 * A mix-in style interface for marking objects that should be
 * acted upon after a given delay.
 *
 * B.
 * <p>An implementation of this interface must define a
 * {@code compareTo} method that provides an ordering consistent with
 * its {@code getDelay} method.
 *
 * @since 1.5
 * @author Doug Lea
 */
public interface Delayed extends Comparable<Delayed> {

    /**
     * 20210523
     * 以给定的时间单位返回与此对象关联的剩余延迟。
     */
    /**
     * Returns the remaining delay associated with this object, in the
     * given time unit.
     *
     * @param unit the time unit
     *
     * // 剩余的延迟； 零或负值表示延迟已经过去
     * @return the remaining delay; zero or negative values indicate
     * that the delay has already elapsed
     */
    long getDelay(TimeUnit unit);
}
