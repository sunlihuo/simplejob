package com.github.hls.base.rocketMQ;

import com.alibaba.rocketmq.client.consumer.DefaultMQPushConsumer;
import com.alibaba.rocketmq.client.exception.MQClientException;
import com.github.hls.base.exception.RocketMQException;
import lombok.extern.log4j.Log4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

@Configuration
@Log4j
public class RocketMQConsumerConfiguration {
    @Value("${rocketmq.consumer.namesrvAddr}")
    private String namesrvAddr;
    @Value("${rocketmq.consumer.groupName}")
    private String groupName;
    @Value("${rocketmq.consumer.topic}")
    private String topic;
    @Value("${rocketmq.consumer.tag}")
    private String tag;
    @Value("${rocketmq.consumer.consumeThreadMin}")
    private int consumeThreadMin;
    @Value("${rocketmq.consumer.consumeThreadMax}")
    private int consumeThreadMax;

    @Resource
    private MessageListener messageListener;

    @Bean
    public DefaultMQPushConsumer getRocketMQConsumer() throws RocketMQException {
        if (StringUtils.isBlank(this.groupName)) {
            throw new RocketMQException("groupName is null !!!");
        }
        if (StringUtils.isBlank(this.namesrvAddr)) {
            throw new RocketMQException("namesrvAddr is null !!!");
        }
        if (StringUtils.isBlank(this.topic)) {
            throw new RocketMQException("topic is null !!!");
        }
        if (StringUtils.isBlank(this.tag)) {
            throw new RocketMQException("tag is null !!!");
        }
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(this.groupName);
        consumer.setNamesrvAddr(this.namesrvAddr);
        consumer.setConsumeThreadMin(this.consumeThreadMin);
        consumer.setConsumeThreadMax(this.consumeThreadMax);
        consumer.registerMessageListener(this.messageListener);
        try {
            consumer.subscribe(this.topic, this.tag);
            consumer.start();
            log.info(String.format("consumer is start !!! groupName:[%s],topic:[%s],namesrvAddr:[%s]", this.groupName, this.topic, this.namesrvAddr));
        } catch (MQClientException e) {
            log.error(String.format("consumer is start !!! groupName:[%s],topic:[%s],namesrvAddr:[%s]", this.groupName, this.topic, this.namesrvAddr), e);
            throw new RocketMQException(e);
        }
        return consumer;
    }
}
