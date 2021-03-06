/**
 * Copyright@Flux7
 */
package com.flux7.bolts;

import java.util.Map;

import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

import com.flux7.tweetsentiment.TweetScorer;
/**
 * 
 * @author rsharif
 *
 */
public class ScoreFetcherBolt extends BaseRichBolt {

	public static final String LESS_THAN_TWO = "less-than-two";

	public static final String GREATER_THAN_TWO = "greater-than-two";

	public static final String LESS_THAN_NEGATIVE_2 = "less-than-negative-2";

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private transient OutputCollector collector;
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ScoreFetcherBolt.class);
	
	private transient TweetScorer scorer;

	private String affinFileName;

	public void prepare(@SuppressWarnings("rawtypes") Map stormConf, TopologyContext context,
			OutputCollector collector) {
		this.collector = collector;
		this.scorer = new TweetScorer(affinFileName);
		
	}
	public ScoreFetcherBolt(String affinFileName){
		this.affinFileName = affinFileName;
	}
	public void execute(Tuple input) {
		String tweet  = input.getString(0);

		if(tweet.length() != 0){
			try {
				String tweetText = scorer.getTweetText(tweet);
				if(tweetText == null)return;
				
				float score = scorer.getScore(tweetText);
				if(score < -2 ){
					collector.emit(LESS_THAN_NEGATIVE_2,new Values(score,tweetText));
				}else if( score < 2){
					collector.emit(LESS_THAN_TWO,new Values(score,tweetText));
				}else{
					collector.emit(GREATER_THAN_TWO,new Values(score,tweetText));
				}
				
			} catch (ParseException e) {
				LOGGER.error("Failed to parse tweet {}",tweet);
			}
		}
	}

	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		
		declarer.declareStream(LESS_THAN_NEGATIVE_2, new Fields("score","tweetText"));
		declarer.declareStream(LESS_THAN_TWO, new Fields("score","tweetText"));
		declarer.declareStream(GREATER_THAN_TWO, new Fields("score","tweetText"));
	}

}
