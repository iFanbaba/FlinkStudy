package ck;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.ReduceFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.utils.MultipleParameterTool;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.contrib.streaming.state.RocksDBStateBackend;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.KeyedStream;
import org.apache.flink.streaming.api.environment.CheckpointConfig;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.util.UUID;


/**
 * @create 2023/9/19
 * @create 10:19
 */
public class RocksDBTest {
    public static void main(String[] args) throws Exception {

        System.setProperty("HADOOP_USER_NAME","root");

        final MultipleParameterTool params = MultipleParameterTool.fromArgs(args);
        String brokers = params.get("brokers", "172.31.0.104:9092");
        String topic = params.get("topic","myfTest");

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        // 使用HDFS作为State Backend
//        env.setStateBackend(new FsStateBackend("hdfs://node1:9820/CKTest/checkpoints"));
        RocksDBStateBackend stateBackend = new RocksDBStateBackend("hdfs://node1:9820/ck", true);
        env.setStateBackend(stateBackend);
//        env.enableCheckpointing(300_000);
        env.enableCheckpointing(5000);
        env.getCheckpointConfig().setCheckpointTimeout(10000);
        env.getCheckpointConfig().setMinPauseBetweenCheckpoints(5000);
        env.getCheckpointConfig().setMaxConcurrentCheckpoints(1);
        env.getCheckpointConfig().setFailOnCheckpointingErrors(false);
        env.getCheckpointConfig().enableExternalizedCheckpoints(CheckpointConfig.ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION);

        KafkaSource<String> source = KafkaSource.<String>builder()
                .setBootstrapServers(brokers)
                .setTopics(topic)
                .setGroupId(UUID.randomUUID().toString())
                .setStartingOffsets(OffsetsInitializer.earliest())
//                .setStartingOffsets(OffsetsInitializer.latest())
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();

        DataStreamSource<String> kafka_source =
                env.fromSource(source, WatermarkStrategy.noWatermarks(), "Kafka Source").setParallelism(1);

        KeyedStream<Tuple2<String, Integer>, String> keyedStream =
                kafka_source
                .map(str -> Tuple2.of(str, 1))
                .returns(Types.TUPLE(Types.STRING, Types.INT))
                .keyBy(tup -> tup.f0);

        DataStream<Tuple2<String, Integer>> reduce = keyedStream.reduce(new ReduceFunction<Tuple2<String, Integer>>() {
            @Override
            public Tuple2<String, Integer> reduce(Tuple2<String, Integer> value1, Tuple2<String, Integer> value2) throws Exception {
                return Tuple2.of(value1.f0, value1.f1 + value2.f1);
            }
        });

        reduce.print("result -> ");
        env.execute("test");

    }
}
