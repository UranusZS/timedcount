/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.timedcount.tools;

import java.io.Serializable;
import java.util.Map;

/**
 * 这个类以滑动窗口的方式来给对象计数
 * 这个类被用来设计成：
 * 1) 为多个生产者线程提供对计数器的写操作，例如对特定对象的计数器作增加操作
 * 2） 赋予单一的消费者进程读取计数器的权限；尽管消费者线程扮演着读的操作，但是这个类会将滑动窗口计数器的头槽向前移动，这意味着消费者进程间接控制着生产者进程到哪里，虽然生产者自身没有移动头槽
 * 在使用基于滑动窗口进行分析数据的时候需要注意的是：在windowLengthInSlots所指定的时间段内，滑动窗口计数器总是会返回大于等于前一个迭代的对象计数，这是滑动窗口计数器开始启动的时候的装载过程。
 * 作为一个例子：使用一个5个槽的计数器，每个槽代表一分钟
 * Sliding window counts of an object X over time
 * 
 * Minute (timeline):
 * 1    2   3   4   5   6   7   8
 *
 * Observed counts per minute:
 * 1    1   1   1   0   0   0   0
 *
 * Counts returned by counter:
 * 1    2   3   4   4   3   2   1
 * 在这个例子中，第一个windowLengthInSlots（即第一个五分钟），计数器会返回大于等于之前的迭代（1, 2, 3, 4, 4），这点在分析数据的时候必须要考虑进去，比如主题趋势，否则你的算法会从计数器而来的趋势是连续上升的而得出错误的印象。
 * 记住在最初的加载阶段，计数器总是一个增加的计数器
 * 
 * 更抽象的来看，这个计数器展示了一种这样的特性，当在两分钟后询问计数器“在过去五分钟，这个对象的频率是？”，那么，计数器的回复应该是“只记录了两份”，暗示着，只能查看两分钟的，因为并没有记录5分钟那样长的时间。
 * 
 * @param <T> 想要计数的对象类型
 * 
 * This class counts objects in a sliding window fashion.
 * <p/> 
 * It is designed 1) to give multiple "producer" threads write access to the counter, i.e. being able to increment
 * counts of objects, and 2) to give a single "consumer" thread (e.g. {@link PeriodicSlidingWindowCounter}) read access
 * to the counter. Whenever the consumer thread performs a read operation, this class will advance the head slot of the
 * sliding window counter. This means that the consumer thread indirectly controls where writes of the producer threads
 * will go to. Also, by itself this class will not advance the head slot.
 * <p/>
 * A note for analyzing data based on a sliding window count: During the initial <code>windowLengthInSlots</code>
 * iterations, this sliding window counter will always return object counts that are equal or greater than in the
 * previous iteration. This is the effect of the counter "loading up" at the very start of its existence. Conceptually,
 * this is the desired behavior.
 * <p/>
 * To give an example, using a counter with 5 slots which for the sake of this example represent 1 minute of time each:
 * <p/>
 * <pre>
 * {@code
 * Sliding window counts of an object X over time
 *
 * Minute (timeline):
 * 1    2   3   4   5   6   7   8
 *
 * Observed counts per minute:
 * 1    1   1   1   0   0   0   0
 *
 * Counts returned by counter:
 * 1    2   3   4   4   3   2   1
 * }
 * </pre>
 * <p/>
 * As you can see in this example, for the first <code>windowLengthInSlots</code> (here: the first five minutes) the
 * counter will always return counts equal or greater than in the previous iteration (1, 2, 3, 4, 4). This initial load
 * effect needs to be accounted for whenever you want to perform analyses such as trending topics; otherwise your
 * analysis algorithm might falsely identify the object to be trending as the counter seems to observe continuously
 * increasing counts. Also, note that during the initial load phase <em>every object</em> will exhibit increasing
 * counts.
 * <p/>
 * On a high-level, the counter exhibits the following behavior: If you asked the example counter after two minutes,
 * "how often did you count the object during the past five minutes?", then it should reply "I have counted it 2 times
 * in the past five minutes", implying that it can only account for the last two of those five minutes because the
 * counter was not running before that time.
 *
 * @param <T> The type of those objects we want to count.
 */
public final class SlidingWindowCounter<T> implements Serializable {

  private static final long serialVersionUID = -2645063988768785810L;

  private SlotBasedCounter<T> objCounter;
  private int headSlot;
  private int tailSlot;
  
  /**
   * 窗口大小
   */
  private int windowLengthInSlots;

  public SlidingWindowCounter(int windowLengthInSlots) {
    if (windowLengthInSlots < 2) {
      throw new IllegalArgumentException(
          "Window length in slots must be at least two (you requested " + windowLengthInSlots + ")");
    }
    this.windowLengthInSlots = windowLengthInSlots;
    this.objCounter = new SlotBasedCounter<T>(this.windowLengthInSlots);

    this.headSlot = 0;
    this.tailSlot = slotAfter(headSlot);
  }

  /**
   * 对对象obj的头槽增加计数
   * @param <P> obj  
   * @return void 
   * @throws
   */
  public void incrementCount(T obj) {
    objCounter.incrementCount(obj, headSlot);
  }

  /**
   * 返回所有跟踪的对象的当前（总）计数。然后将窗口迁前移
   * 
   * 当这个方法被调用的时候，我们认为当前的滑动窗口可用并且已经处理完均衡（被调用者）。了解到这一点，我们就能在下一个滑动窗口块内计数对象的计数
   * @return 所有被跟踪的对象的当前（总）计数
   * 
   * Return the current (total) counts of all tracked objects, then advance the window.
   * <p/>
   * Whenever this method is called, we consider the counts of the current sliding window to be available to and
   * successfully processed "upstream" (i.e. by the caller). Knowing this we will start counting any subsequent
   * objects within the next "chunk" of the sliding window.
   *
   * @return The current (total) counts of all tracked objects.
   */
  public Map<T, Long> getCountsThenAdvanceWindow() {
    // 返回各个不同对象的计数总和
    Map<T, Long> counts = objCounter.getCounts();
    // 清除为零的计数
    objCounter.wipeZeros();
    // 给定槽，对任意对象的这个槽的计数清零
    objCounter.wipeSlot(tailSlot);
    // 前移
    advanceHead();
    return counts;
  }

 /**
  *     head tail
  * eg:    1  2 
  *     8        3
  *     7        4
  *        6  5 
  */
  private void advanceHead() {
    headSlot = tailSlot;
    tailSlot = slotAfter(tailSlot);
  }

  /**
   * 下一个槽
   * 将槽视为环形的。  1,2,3,4...windowLengthInSlots
   * eg:    1  2 
   *     8        3
   *     7        4
   *        6  5 
   * @param int slot  
   * @return int 
   * @throws
   */
  private int slotAfter(int slot) {
    return (slot + 1) % windowLengthInSlots;
  }

}
