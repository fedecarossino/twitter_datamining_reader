package com.jherenu.twitter_reader

import com.google.common.collect.Queues
import com.jherenu.twitter_reader.reader.TwitterStreamReader
import com.jherenu.twitter_reader.sentiment.SentimentCalculatorFactory
import com.jherenu.twitter_reader.utils.PropertiesAdapter
import com.jherenu.twitter_reader.validators.TweetDataValidatorFactory

class Main {
    static main(args) {
        PropertiesAdapter propertiesAdapter = new PropertiesAdapter()

        String keywords = 'germany'
        String extraWords = 'argelia,algeria,world cup,worldcup,brazil,brasil,football,soccer'
        def configuration = createDefaultTweetDataConfiguration(keywords, extraWords)

        def sentimentCalculator = new SentimentCalculatorFactory(propertiesAdapter).createSentimentCalculator(keywords)
        def tweetDataFactory = new TweetDataFactory(sentimentCalculator)

        def queue = Queues.newArrayBlockingQueue(20)
        def tweetDataCollection = [:]

        def twitterStreamReader = new TwitterStreamReader(configuration.getKeywords(), queue)
        twitterStreamReader.startConsumer()

        def tweetDataValidator = new TweetDataValidatorFactory().createValidator(configuration, tweetDataCollection)

        while(tweetDataCollection.size() < configuration.getSizeOfData()) {
            def tweetJSON = queue.poll()

            if(tweetJSON != null) {
                def tweetData = tweetDataFactory.createFromMap(tweetJSON)
                if(tweetDataValidator.validateTweetData(tweetData)) {
                    tweetDataCollection.put(tweetData.getId(), tweetData)
                }
            } else {
                println "Nothing to read, sleep 1000 ms"
                println "Size of results: ${tweetDataCollection.size()}"
                sleep 1000
            }
        }

        twitterStreamReader.stopConsumer()

        new File("salida.csv").withWriter { out ->
            out.writeLine(TweetDataToAnalyze.titleRow())
            tweetDataCollection.values().each() { tweetData ->
                out.writeLine( tweetData.toRow() )
            }
        }

    }

    static def createDefaultTweetDataConfiguration(keywords, extraWords) {
        return new TweetDataConfiguration().with { tdc ->
            tdc.keywords = keywords
            tdc.extraWords = extraWords
            return tdc
        }
    }
}
