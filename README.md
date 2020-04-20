Amazon Kinesis Data Streams (KDS) is a massively scalable and durable real-time data streaming service. KDS can continuously capture gigabytes of data per second from hundreds of thousands of sources such as website clickstreams, database event streams, financial transactions, social media feeds, IT logs, and location-tracking events. The data collected is available in milliseconds to enable real-time analytics use cases such as real-time dashboards, real-time anomaly detection, dynamic pricing, and more.

**Kinesis Data Streams Terminology:**
    
* Producer – Producers put records into Amazon Kinesis Data Streams. For example, a web server sending log data to a stream is a producer.

* Consumer – Consumers get records from Amazon Kinesis Data Streams and process them. 

* Shard – A shard is a uniquely identified sequence of data records in a stream. A stream is composed of one or more shards, each of which provides a fixed unit of capacity.

* Partition Key – A partition key is used to group data by shard within a stream. Kinesis Data Streams segregates the data records belonging to a stream into multiple shards. It uses the partition key that is associated with each data record to determine which shard a given data record belongs to. When an application puts data into a stream, it must specify a partition key.


**Steps to create Kinesis Streams:**

Before we dive into creating kinesis streams, here are some prerequisite that we will be need while bulding this in java.

**Prerequisite:**

  We need Amazon Kinesis Client Library. The recommended way to use the KCL for Java is to consume it from Maven.

        <dependency>
            <groupId>software.amazon.kinesis</groupId>
            <artifactId>amazon-kinesis-client</artifactId>
            <version>2.2.9</version>
        </dependency>

**Step 1:**

On the Create Kinesis stream page, enter a name for your stream and the number of shards you need, and then click Create Kinesis stream.

**Step 2:**

We will put data into kinesis stream. We can put data using lamda function, cli and java. Here for testing puropse we put data using aws cli. Command is
    
    aws kinesis put-record --stream-name <stream_name>  --data=<value>, --partition-key=<key_name>

for instance just suppose you have 2 shards. You want to put specific data in one shard and other data into another shard. You can do that using partition key. In simple word data with a specific partition key will go to the one specific shard.
Here are the bash scripts that I used to add data into stream.

even.sh

    #!bin/bash
    even_number=(2 4 6 8 10 12 14 16 18 20 22 24 26 28 30 32 34)
    for i in ${even_number[@]}
    do
      aws kinesis put-record --stream-name number-generator-stream  --data=$i, --partition-key=evenKey
    done

odd.sh

    #!bin/bash
    odd_number=(1 3 5 7 9 11 13 15 17 19 21 23 25 27 29 33 55 67)
    for i in ${odd_number[@]}
    do
      aws kinesis put-record --stream-name number-generator-stream  --data=$i, --partition-key=oddKey
    done


**Step 3:**

In this step we will be registering consumers, consumers will be registering using aws cli. 

    aws kinesis register-stream-consumer --stream-arn <stream_arn> --consumer-name <enter_name_of_consumer>

**Step 4:**

In this step we will use Amazon Kinesis Client Library in java to get consumer subcribed with shards. We need to have 2 things to get consumer subcribed with shard.
    
    1- Consumer ARN
    2- ShardId parameter

Here we have one more importnant parameter, *ShardIteratorType*, let's discuss about its type.

* AT_SEQUENCE_NUMBER: Start streaming from the position denoted by the sequence number specified in the SequenceNumber field.

* AFTER_SEQUENCE_NUMBER: Start streaming right after the position denoted by the sequence number specified in the SequenceNumber field.

* AT_TIMESTAMP: Start streaming from the position denoted by the time stamp specified in the Timestamp field.

* TRIM_HORIZON: Start streaming at the last untrimmed record in the shard, which is the oldest data record in the shard.

* LATEST: Start streaming just after the most recent record in the shard, so that you always read the most recent data in the shard.

        static void SubscribeToShards(String ConsumerArn, String ShardId){

            KinesisAsyncClient client = KinesisAsyncClient.create();

            SubscribeToShardRequest request = SubscribeToShardRequest.builder()
                    .consumerARN(ConsumerArn)
                    .shardId(ShardId)
                    .startingPosition(s -> s.type(ShardIteratorType.TRIM_HORIZON))
                    .build();

            callSubscribeToShardWithVisitor(client, request).join();
        }

        private static CompletableFuture<Void> callSubscribeToShardWithVisitor(KinesisAsyncClient client, SubscribeToShardRequest request) {
            SubscribeToShardResponseHandler.Visitor visitor = new SubscribeToShardResponseHandler.Visitor() {
                @Override
                public void visit(SubscribeToShardEvent event) {
                    System.out.println("Received subscribe to shard event " + event);
                }
            };
            SubscribeToShardResponseHandler responseHandler = SubscribeToShardResponseHandler
                    .builder()
                    .onError(t -> System.err.println("Error during stream - " + t.getMessage()))
                    .subscriber(visitor)
                    .build();
            return client.subscribeToShard(request, responseHandler);
        }

**Step 5:**

If we have multiple consumer and we want them to run simultaneously, we can use java's threads to achieve it.

    static void RunSimultaneously() throws InterruptedException {

        String OddShardId = ListShards().get(0);
        String EvenShardId = ListShards().get(1);

        Thread thread1 = new Thread() {
            public void run() {
                SubscribeToShards(ODD_NUMBER_CONSUMER_ARN, OddShardId);
            }
        };

        Thread thread2 = new Thread() {
            public void run() {
                SubscribeToShards(EVEN_NUMBER_CONSUMER_ARN, EvenShardId);
            }
        };

        thread1.start();
        thread2.start();

        thread1.join();
        thread2.join();
    }


