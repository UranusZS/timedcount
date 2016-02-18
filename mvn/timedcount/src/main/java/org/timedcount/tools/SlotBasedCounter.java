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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 这个类提供了对于每个槽出现的对象的次数
 * 它用来，例如：作为一个模块实现对象的滑动窗口计数
 * @param <T> 想要计数的对象类型
 * This class provides per-slot counts of the occurrences of objects.
 * <p/>
 * It can be used, for instance, as a building block for implementing sliding window counting of objects.
 *
 * @param <T> The type of those objects we want to count.
 */
public final class SlotBasedCounter<T> implements Serializable {

  private static final long serialVersionUID = 4858185737378394432L;

  /**
   * 一个表，对象=>计数数组。
   * 对象数组可以看成是多个槽，每个槽中放置不同数量的对象
   */
  private final Map<T, long[]> objToCounts = new HashMap<T, long[]>();
  /**
   * 槽的数量
   */
  private final int numSlots;

  public SlotBasedCounter(int numSlots) {
    if (numSlots <= 0) {
      throw new IllegalArgumentException("Number of slots must be greater than zero (you requested " + numSlots + ")");
    }
    this.numSlots = numSlots;
  }

  /**
   * 增加某个槽的数量
   * @param <T> obj
   * @param int slot  
   * @return void 
   * @throws
   */
  public void incrementCount(T obj, int slot) {
    long[] counts = objToCounts.get(obj);
    // 之前没有计数过
    if (counts == null) {
      counts = new long[this.numSlots];
      objToCounts.put(obj, counts);
    }
    // 之前已经出现过这个对象
    counts[slot]++;
  }

  /**
   * 返回某个对象某个槽的数量
   * @param <T> obj
   * @param int slot  
   * @return long 
   * @throws
   */
  public long getCount(T obj, int slot) {
    long[] counts = objToCounts.get(obj);
    if (counts == null) {
      return 0;
    }
    else {
      return counts[slot];
    }
  }

  /**
   * 返回各个不同对象的计数总和
   * @param void 
   * @return Map<T,Long> 
   * @throws
   */
  public Map<T, Long> getCounts() {
    Map<T, Long> result = new HashMap<T, Long>();
    for (T obj : objToCounts.keySet()) {
      result.put(obj, computeTotalCount(obj));
    }
    return result;
  }

  /**
   * 对计数数组加和
   * @param <T> obj 
   * @return long 
   * @throws
   */
  private long computeTotalCount(T obj) {
    long[] curr = objToCounts.get(obj);
    long total = 0;
    for (long l : curr) {
      total += l;
    }
    return total;
  }

  /**
   * Reset the slot count of any tracked objects to zero for the given slot.
   * 给定槽，对任意对象的这个槽的计数清零
   * @param slot
   */
  public void wipeSlot(int slot) {
    for (T obj : objToCounts.keySet()) {
      resetSlotCountToZero(obj, slot);
    }
  }

  /**
   * 对于给定对象的给定槽的计数清零
   * @param <T> obj
   * @param int slot  
   * @return void 
   * @throws
   */
  private void resetSlotCountToZero(T obj, int slot) {
    long[] counts = objToCounts.get(obj);
    counts[slot] = 0;
  }

  /**
   * 判断是否可以从计数map中清除，如果该对象计数为零的话，可以清除
   * @param <T> obj 
   * @return boolean 
   * @throws
   */
  private boolean shouldBeRemovedFromCounter(T obj) {
    return computeTotalCount(obj) == 0;
  }

  /**
   * Remove any object from the counter whose total count is zero (to free up memory).
   * 清除为零的计数
   */
  public void wipeZeros() {
    Set<T> objToBeRemoved = new HashSet<T>();
    for (T obj : objToCounts.keySet()) {
      if (shouldBeRemovedFromCounter(obj)) {
        objToBeRemoved.add(obj);
      }
    }
    for (T obj : objToBeRemoved) {
      objToCounts.remove(obj);
    }
  }

}
