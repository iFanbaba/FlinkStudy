package org.example;

import com.starrocks.connector.flink.StarRocksSink;
import com.starrocks.connector.flink.table.sink.StarRocksSinkOptions;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.java.utils.MultipleParameterTool;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.DataTypes;
import org.apache.flink.table.api.TableSchema;
/**
 * @create 2023/9/5
 * @create 22:35
 */
public class Kafka2SR {
    public static void main(String[] args) throws Exception {
        final MultipleParameterTool params = MultipleParameterTool.fromArgs(args);
//        "kafka-cnai5p14gfvjistf.kafka.ivolces.com:9092"
        String brokers = params.get("brokers", "kafka-cnai5p14gfvjistf.kafka.ivolces.com:9092");
        String topic = params.get("topic","myfTest");

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        KafkaSource<String> source = KafkaSource.<String>builder()
                .setBootstrapServers(brokers)
                .setTopics(topic)
                .setGroupId("group1")
                .setStartingOffsets(OffsetsInitializer.earliest())
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();
        DataStreamSource<String> kafka_source = env.fromSource(source, WatermarkStrategy.noWatermarks(), "Kafka Source");
        kafka_source.print("source-> ");

        StarRocksSink.sink(
                StarRocksSinkOptions.builder()
                        .withProperty("connector","starrocks")
                        .withProperty("jdbc-url","jdbc:mysql://10.x.x.x:9030?characterEncoding=utf-8&useSSL=false")
                        .withProperty("load-url","10.x.x.x:8030;10.x.x.x:8030;10.x.x.x:8030")
                        .withProperty("username","root")
                        .withProperty("password","pass")
                        .withProperty("table-name","book")
                        .withProperty("database-name","database_name")
                        .withProperty("sink.properties.column_separator", "\\x01")
                        .withProperty("sink.properties.row_delimiter", "\\x02")
                        .withProperty("sink.buffer-flush.interval-ms", "10000")
                        .build()
        );
        env.execute("TEST");
    }
}
