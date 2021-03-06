package com.github.hls.base.task;

import com.alibaba.rocketmq.common.message.Message;
import com.github.hls.base.disruptor.Disruptor;
import com.github.hls.base.disruptor.Producer;
import com.github.hls.base.enums.SimpleJobEnum;
import com.github.hls.base.exception.DependenceException;
import com.github.hls.base.simplejob.base.SimpleJobStrategy;
import com.github.hls.domain.SimpleJobDO;
import com.github.hls.service.SimpleJobServer;
import com.github.hls.utils.DateUtils;
import com.github.hls.utils.SpringUtil;
import lombok.extern.log4j.Log4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.github.hls.utils.SimpleJobUtils.transList2Map;

@Service
@Log4j
public class SimpleJobTask{

    @Resource
    private SimpleJobServer simpleJobServer;
    @Resource
    private Disruptor disruptor;
    @Resource
    private DependenceTask dependenceTask;

    public boolean handleMessage(Message msg) throws UnsupportedEncodingException {
        String jobName = new String(msg.getBody(), "UTF-8");
        log.info("接收MQ消息 SimpleJobTask handleMessage jobName=" + jobName);

        final SimpleJobDO simpleJobDO = new SimpleJobDO();
        simpleJobDO.setJobName(jobName);
        return handleHttp(simpleJobDO);
    }

    public boolean handleHttp(SimpleJobDO simpleJobDO) {
        final List<SimpleJobDO> simpleJobS = simpleJobServer.queryRunningJob(simpleJobDO);
        if (simpleJobS == null || simpleJobS.size() == 0){
            log.error("simplejob is null");
            return false;
        }

        handleJob(simpleJobS);
        return true;
    }

    public void handleJob(List<SimpleJobDO> simpleJobList) {
        Producer producer = disruptor.getProducer();

        try {
            long countCurrent = System.currentTimeMillis();
            int i = 0;
            final Map<String, List<SimpleJobDO>> simpleJobMap = transList2Map(simpleJobList);

            log.info("begin JobThreadService list  = " + simpleJobMap.values().size());

            final Iterator<List<SimpleJobDO>> iterator = simpleJobMap.values().iterator();
            while (iterator.hasNext()) {
                boolean isSuccess = true;
                List<SimpleJobDO> jobList = iterator.next();

                for (SimpleJobDO simpleJob : jobList) {
                    Long current = System.currentTimeMillis();
                    log.info("开始第"+ ++i +"个任务 jobId = " + simpleJob.getSimpleJobId() + " ; jobName = " + simpleJob.getJobName() + " ;SourceType="+simpleJob.getSourceType());

                    try {
                        dependenceTask.isNotKeepGoing(simpleJob);

                        String beanName = SimpleJobEnum.SOURCE_TYPE.valueOf(simpleJob.getSourceType()).getBeanName();
                        SimpleJobStrategy simpleJobStrategy = (SimpleJobStrategy) SpringUtil.getBean(beanName);
                        simpleJobStrategy.setProducer(producer);
                        simpleJobStrategy.handle(simpleJob);
                    } catch (Exception e) {
                        isSuccess = false;
                        if (e instanceof DependenceException) {
                            simpleJobServer.insertJobMonitor(jobList.get(0), "waiting");
                            break;
                        }

                        log.error("SimpleJobTask error", e);
                        simpleJobServer.insertJobMonitor(jobList.get(0), "waiting");
                        if (!"Y".equalsIgnoreCase(simpleJob.getErrorGoOn())){
                            break;
                        }
                    }

                    log.info("结束第"+ i +"个任务 jobId = " + simpleJob.getSimpleJobId() + " ; jobName = " + simpleJob.getJobName() + " ;耗时 = " + DateUtils.dateDiff(current, System.currentTimeMillis()));
                }

                if (isSuccess) {
                    //一组任务完成
                    simpleJobServer.insertJobMonitor(jobList.get(0), "success");
                    //每组完成后，依赖子任务触发
                    dependenceTask.handleWaitingSimpleJob(jobList.get(0));
                }

            }

            log.info("end JobThreadService" + " ;耗时 = " + DateUtils.dateDiff(countCurrent, System.currentTimeMillis()));
        } catch (Exception e) {
            log.error("SimpleJobTask error", e);
        } finally {
            disruptor.drainAndHalt();
        }

    }


}
