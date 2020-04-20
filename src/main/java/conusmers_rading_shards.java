import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.awssdk.services.kinesis.model.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;


class SubscribeToShardSimpleImpl {

    private static final String ODD_NUMBER_CONSUMER_ARN = "ARN";
    private static final String EVEN_NUMBER_CONSUMER_ARN = "ARN";
    private static final String STREAM_NAME = "number-generator-stream";
    private static List<String> SHARD_IDS = new ArrayList<String>();

    static List<String> ListShards(){

        KinesisAsyncClient client = KinesisAsyncClient.builder().build();
        ListShardsRequest request = ListShardsRequest
                .builder().streamName(STREAM_NAME)
                .build();
        try {

            ListShardsResponse response = client.listShards(request).get(5000, TimeUnit.MILLISECONDS);
            int totalShardsSize = response.shards().size();
            for (int shard = 0; shard < totalShardsSize; shard++){
                if(response.shards().get(shard).parentShardId() == null)
                    SHARD_IDS.add(response.shards().get(shard).shardId());
            }

            return SHARD_IDS;

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return null;
    }

    static void SubscribeToShards(String ConsumerArn, String ShardId){

        KinesisAsyncClient client = KinesisAsyncClient.create();

        SubscribeToShardRequest request = SubscribeToShardRequest.builder()
                .consumerARN(ConsumerArn)
                .shardId(ShardId)
                .startingPosition(s -> s.type(ShardIteratorType.TRIM_HORIZON))
                .build();

        callSubscribeToShardWithVisitor(client, request).join();

/*
        while(true) {
            callSubscribeToShardWithVisitor(client, request).join();
        }*/
    }


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

    public static void main(String[] args) throws InterruptedException {

         RunSimultaneously();

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
}



