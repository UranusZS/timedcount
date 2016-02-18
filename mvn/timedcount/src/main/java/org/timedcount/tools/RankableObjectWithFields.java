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

import backtype.storm.tuple.Tuple;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.io.Serializable;
import java.util.List;

/**
 * This class wraps an objects and its associated count, including any additional data fields.
 * <p/>
 * This class can be used, for instance, to track the number of occurrences of an object in a Storm topology.
 */
public class RankableObjectWithFields implements Rankable, Serializable {

  private static final long serialVersionUID = -9102878650001058090L;
  private static final String toStringSeparator = "|";

  private final Object obj;
  private final long count;
  /**
   * java.util.Collections类提供了一系列unmodifiableFoo的静态方法供使用，
   * 这些unmodifiableFoo实际上是原有集合的视图包装，所以可以认为新生成的不变集合和原有集合是同一对象，
   * 只是不变集合不能调用修改操作。这种效果有时并不是真正想要的，有时需要的是生成的不变集合和原有集合是分离的，原有集合的后续操作不影响不变集合
   * 。google collections就提供了该功能，具体的就包括ImmutableList、ImmutableMap、ImmutableSet等。
   */
  private final ImmutableList<Object> fields;
  
  public RankableObjectWithFields(Object obj, long count, Object... otherFields) {
    if (obj == null) {
      throw new IllegalArgumentException("The object must not be null");
    }
    if (count < 0) {
      throw new IllegalArgumentException("The count must be >= 0");
    }
    this.obj = obj;
    this.count = count;
    fields = ImmutableList.copyOf(otherFields);

  }

  /**
   * 由Tuple构造一个新的实例
   * 
   * 该方法期望对象能够以给定tuple的第一个域（index 0）来进行排序，而对象计数出现在第二个域（index 1）。
   * 其他的域也能够被提取和跟踪。这些域可以由RankableObjectWithFields::getFields()来获取
   * @return new instance based on the provided tuple
   * 
   * Construct a new instance based on the provided {@link Tuple}.
   * <p/>
   * This method expects the object to be ranked in the first field (index 0) of the provided tuple, and the number of
   * occurrences of the object (its count) in the second field (index 1). Any further fields in the tuple will be
   * extracted and tracked, too. These fields can be accessed via {@link RankableObjectWithFields#getFields()}.
   *
   * @param tuple
   *
   * @return new instance based on the provided tuple
   */
  public static RankableObjectWithFields from(Tuple tuple) {
    List<Object> otherFields = Lists.newArrayList(tuple.getValues());
    Object obj = otherFields.remove(0);
    Long count = (Long) otherFields.remove(0);
    return new RankableObjectWithFields(obj, count, otherFields.toArray());
  }

  public Object getObject() {
    return obj;
  }

  public long getCount() {
    return count;
  }

  /**
   * @return an immutable list of any additional data fields of the object (may be empty but will never be null)
   */
  public List<Object> getFields() {
    return fields;
  }

  public int compareTo(Rankable other) {
    long delta = this.getCount() - other.getCount();
    if (delta > 0) {
      return 1;
    }
    else if (delta < 0) {
      return -1;
    }
    else {
      return 0;
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof RankableObjectWithFields)) {
      return false;
    }
    RankableObjectWithFields other = (RankableObjectWithFields) o;
    return obj.equals(other.obj) && count == other.count;
  }

  @Override
  public int hashCode() {
    int result = 17;
    int countHash = (int) (count ^ (count >>> 32));
    result = 31 * result + countHash;
    result = 31 * result + obj.hashCode();
    return result;
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("[");
    buf.append(obj);
    buf.append(toStringSeparator);
    buf.append(count);
    for (Object field : fields) {
      buf.append(toStringSeparator);
      buf.append(field);
    }
    buf.append("]");
    return buf.toString();
  }

  /**
   * Note: We do not defensively copy the wrapped object and any accompanying fields.  We do guarantee, however,
   * do return a defensive (shallow) copy of the List object that is wrapping any accompanying fields.
   *
   * @return
   */
  public Rankable copy() {
    List<Object> shallowCopyOfFields = ImmutableList.copyOf(getFields());
    return new RankableObjectWithFields(getObject(), getCount(), shallowCopyOfFields);
  }

}
