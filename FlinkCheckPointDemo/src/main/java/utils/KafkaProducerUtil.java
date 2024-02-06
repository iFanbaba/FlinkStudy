package utils;

import org.apache.flink.api.java.utils.MultipleParameterTool;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.io.IOException;
import java.util.Properties;

/**
 * @create 2023/9/7
 * @create 10:31
 */
public class KafkaProducerUtil {
    public static void main(String[] args) throws IOException {
        ParameterTool params = ParameterTool.fromArgs(args);
        String topic = params.get("topic","myTest");
        String brokers = params.get("brokers","172.31.0.104:9092");

        Properties props = new Properties();
        props.put("bootstrap.servers",brokers);
        props.put("group.id", "test");//消费者的组id
        props.put("enable.auto.commit", "true");
        props.put("auto.commit.interval.ms", "1000");
        props.put("session.timeout.ms", "30000");
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("topic", topic);
        String [] channels = {"www.xxx.com","www.sdfs.com","www.qqq.com","www.dfff.com"};
        String [] strings = {"上海","北京","深圳","杭州","武汉","青岛","天津","广州","长沙","郑州"};

        Producer<String, String> producer = new KafkaProducer<String, String>(props);

        for (int i = 0; i < 100; i++) {
            producer.send(new ProducerRecord<String, String>(topic, Integer.toString(i), Integer.toString(i)));
        }
    }
}
