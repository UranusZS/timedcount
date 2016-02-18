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
package org.timedcount.spout;

import backtype.storm.spout.SpoutOutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichSpout;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;
import backtype.storm.utils.Utils;

import java.util.Map;
import java.util.Random;

import org.apache.log4j.Logger;

public class RandomSentenceSpout extends BaseRichSpout {

	private static final long serialVersionUID = 6519896400005340034L;
	
	private static final Logger LOG = Logger.getLogger(RandomSentenceSpout.class);
	SpoutOutputCollector _collector;
	Random _rand;

    /**
     * Called when a task for this component is initialized within a worker on the cluster.
     * It provides the spout with the environment in which the spout executes.
     *
     * <p>This includes the:</p>
     *
     * @param conf The Storm configuration for this spout. This is the configuration provided to the topology merged in with cluster configuration on this machine.
     * @param context This object can be used to get information about this task's place within the topology, including the task id and component id of this task, input and output information, etc.
     * @param collector The collector is used to emit tuples from this spout. Tuples can be emitted at any time, including the open and close methods. The collector is thread-safe and should be saved as an instance variable of this spout object.
     */
	@SuppressWarnings("rawtypes")
    public void open(Map conf, TopologyContext context, SpoutOutputCollector collector) {
	    _collector = collector;
        _rand = new Random();
    }
    
    /**
     * This method should be non-blocking, so if the Spout has no tuples to emit, this method should return.
     * nextTuple, ack, and fail are all called in a tight loop in a single thread in the spout task. 
     * When there are no tuples to emit, it is courteous to have nextTuple sleep for a short amount of time (like a single millisecond) so as not to waste too much CPU.
     * 
     * A tuple is a named list of values, which can be of any type of Java object (as long as the object is serializable). 
     * By default, Storm can serialize common types like strings, byte arrays, ArrayList, HashMap, and Hash-Set.
     */
    public void nextTuple() {
        Utils.sleep(100);
        String[] sentences = new String[]{ 
            "the cow jumped over the moon", 
            "an apple a day keeps the doctor away",
            "four score and seven years ago", 
            "snow white and the seven dwarfs", 
            "i am at two with nature" 
        };
        String sentence = sentences[_rand.nextInt(sentences.length)];
        String out = "===========nextTuple=========== " + sentence;
        LOG.info(out);
        // Values is an implementation of ArrayList, where the elements of the list are passed to the constructor.
        _collector.emit(new Values(sentence));
    }
    
    @Override
    public void ack(Object id) {
        // LOG.info("FAIL->" + id);
    }
    
    @Override
    public void fail(Object msgId) {
        LOG.warn("FAIL->" + msgId);
    }
    
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declare(new Fields("word"));
    }

}