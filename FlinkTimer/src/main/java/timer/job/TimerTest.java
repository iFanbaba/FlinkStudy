package timer.job;

import org.apache.flink.api.common.eventtime.SerializableTimestampAssigner;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;
import timer.pojo.WaterSensor;

import java.time.Duration;

/**
 * @create 2023/10/10
 * @create 13:05
 */
public class TimerTest {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);
        DataStream<String> stream = env.socketTextStream("172.31.0.104", 7777);

        DataStream<WaterSensor> streamOperator = stream.map(new MapFunction<String, WaterSensor>() {
            @Override
            public WaterSensor map(String value) throws Exception {
                String[] split = value.split(",");
                return new WaterSensor(split[0], Long.parseLong(split[1]), Integer.parseInt(split[2]));
            }
        });

        //定义时间语义
        WatermarkStrategy<WaterSensor> wms = WatermarkStrategy
                .<WaterSensor>forBoundedOutOfOrderness(Duration.ofSeconds(2))
                .withTimestampAssigner(
                        new SerializableTimestampAssigner<WaterSensor>() {
                            @Override
                            public long extractTimestamp(WaterSensor element, long recordTimestamp) {
                                return element.getTs() * 1000L;
                            }
                        }
                );

        //指定策略、分组、处理
        SingleOutputStreamOperator<WaterSensor> outputStreamOperator = streamOperator
                .assignTimestampsAndWatermarks(wms)
                .keyBy(WaterSensor::getId)
                .process(new KeyedProcessFunction<String, WaterSensor, WaterSensor>() {
                    //定义状态
                    private ValueState<Long> tsState;
                    private ValueState<Integer> vcState;

                    //状态在open方法实现初始化
                    @Override
                    public void open(Configuration parameters) throws Exception {
                        tsState = getRuntimeContext().getState(new ValueStateDescriptor<Long>("state-ts", Long.class));
                        vcState = getRuntimeContext().getState(new ValueStateDescriptor<Integer>("state-vc", Integer.class));
                    }

                    @Override
                    public void processElement(WaterSensor value, Context ctx, Collector<WaterSensor> out) throws Exception {
                        //ValueStateDescriptor方法已经弃用使用默认值，故在此判断
                        if (vcState.value() == null) {
                            vcState.update(Integer.MIN_VALUE);
                        }
                        //获取值
                        Long TimerTS = tsState.value();
                        Integer lastVC = vcState.value();
                        Integer currVC = value.getVc();

                        //对水位进行判断，注意删除定时器使用的时间
                        if (currVC > lastVC && TimerTS == null) {
                            System.out.println(value.getId() + "：注册定时器！");
                            Long ts = ctx.timerService().currentProcessingTime() + 10000L;
                            ctx.timerService().registerProcessingTimeTimer(ts);
                            tsState.update(ts);
                        } else if (value.getVc() <= lastVC && TimerTS != null) {
                            System.out.println(value.getId() + "：删除定时器！");
                            ctx.timerService().deleteProcessingTimeTimer(TimerTS);
                            tsState.clear();
                        }
                        vcState.update(currVC);
                        out.collect(value);
                    }

                    //定时器触发后的操作
                    @Override
                    public void onTimer(long timestamp, OnTimerContext ctx, Collector<WaterSensor> out) throws Exception {
                        ctx.output(new OutputTag<String>("outsideput") {
                                   }
                                , ctx.getCurrentKey() + "水位连续10秒没有下降！！！");
                    }
                });

        outputStreamOperator.print("主流>>>>>");
        outputStreamOperator.getSideOutput(new OutputTag<String>("outsideput") {
        }).print("侧输出流>>>>>");

        env.execute();
    }
}
