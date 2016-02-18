package org.timedcount;

import org.apache.log4j.PropertyConfigurator;
import org.timedcount.bolt.RollingCountBolt;
import org.timedcount.spout.RandomSentenceSpout;
import org.timedcount.util.StormRunner;

import backtype.storm.Config;
import backtype.storm.generated.AlreadyAliveException;
import backtype.storm.generated.InvalidTopologyException;
import backtype.storm.topology.TopologyBuilder;
import backtype.storm.tuple.Fields;

/**
 * 
 * @ClassName: StormStarter
 * @Description: To config and start Storm.
 * @author ZS
 * @date Feb 3, 2016 4:05:45 PM
 *
 */
public class StormStarter 
{
    private static String topologyName = "wordcount";
    private static int secondsToRun = 20;

    public static void main( String[] args )
    {
        System.out.println( "Storm StormStarter" );
        
        try {      // local
            init();
            TopologyBuilder builder = defineTopology();
            Config conf = buildConf();
            StormRunner.runTopologyLocally(builder.createTopology(), topologyName, conf, secondsToRun);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        /*
        try {      // remote
            init();
            TopologyBuilder builder = defineTopology();
            Config conf = buildConf();
            StormRunner.runTopologyRemotely(builder.createTopology(), topologyName, conf);
        } catch (AlreadyAliveException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InvalidTopologyException e) {
            // TODO Auto-generated catch block
            e.printStackTrace(); 
        }
        */
        System.out.println( "Storm StormStarter" );

    }
    
    /**
     * 
     * @Title: defineTopology
     * @Description: define the topology of the storm
     * @param  void
     * @return TopologyBuilder 
     * @throws
     */
    public static TopologyBuilder defineTopology() {
        // Topology definition. tells Storm how the nodes are arranged and how they exchange data.
        TopologyBuilder builder = new TopologyBuilder();
        builder.setSpout("word-reader", new RandomSentenceSpout());
        builder.setBolt("word-counter", new RollingCountBolt(), 1)
            .fieldsGrouping("word-reader", new Fields("word"));
        return builder;
    }
    
    /**
     * 
     * @Title: buildConf
     * @Description: TODO
     * @param  void
     * @return Config 
     * @throws
     */
    public static Config buildConf() {
        //Configuration. create a Config object containing the topology configuration, which is merged with the cluster configuration at run time and sent to all nodes with the prepare method.
        Config conf = new Config();
        // When debug is true, Storm prints all messages exchanged between nodes, and other debug data useful for understanding how the topology is running.
        conf.setDebug(false);
        //Topology run
        conf.put(Config.TOPOLOGY_MAX_SPOUT_PENDING, 1);
        return conf;
    }
    
    public static void init() {
        //PropertyConfigurator.configure("resources/log4j.properties"); 
        String file = System.getProperty("user.dir") + "/resources/log4j.properties";
        PropertyConfigurator.configure(file); 
        //System.out.println(file);
    }
}
