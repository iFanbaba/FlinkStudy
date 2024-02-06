package ck;

import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.functions.ReduceFunction;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.runtime.state.filesystem.FsStateBackend;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.KeyedStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

/**
 * @create 2023/9/19
 * @create 14:57
 */
@Slf4j
public class FsStateBackendTest_hdfs {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        // 使用HDFS作为State Backend
        FsStateBackend stateBackend = new FsStateBackend("hdfs:///ck", true);
        env.setStateBackend(stateBackend);
        env.enableCheckpointing(5000);
        env.getCheckpointConfig().setCheckpointTimeout(10000);
        env.getCheckpointConfig().setMinPauseBetweenCheckpoints(5000);
        env.getCheckpointConfig().setMaxConcurrentCheckpoints(1);
        env.getCheckpointConfig().setFailOnCheckpointingErrors(true);

        env.getCheckpointConfig().setCheckpointingMode(CheckpointingMode.EXACTLY_ONCE);


        DataStreamSource<String> source = env.socketTextStream("172.31.0.104", 7777);

        KeyedStream<Tuple2<String, Integer>, String> keyedStream = source
                .map(str -> Tuple2.of(str, 1))
                .returns(Types.TUPLE(Types.STRING, Types.INT))
                .keyBy(tup -> tup.f0);

        DataStream<Tuple2<String, Integer>> reduce = keyedStream.reduce(new ReduceFunction<Tuple2<String, Integer>>() {
            @Override
            public Tuple2<String, Integer> reduce(Tuple2<String, Integer> value1, Tuple2<String, Integer> value2) throws Exception {
                if (value1.f0.equals("s")){
                    log.error(" 出错了 %s",12);
                    return null;
                } else {
                    return Tuple2.of(value1.f0, value1.f1 + value2.f1);
                }
            }
        });

        reduce.print("result -> ");

        env.execute("test");

    }
}
