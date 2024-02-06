import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.net.URL;

/**
 * @create 2023/9/12
 * @create 17:13
 */
public class flinkDemo {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);
        env.setParallelism(1);

        // 1. 读取事件数据，创建简单事件流
//        URL resource = flinkDemo.class.getResource("source.csv");
        DataStream<String> source = env.readTextFile("src/main/resources/source.csv");
//        source.print();

        env.execute("test");
    }
}
