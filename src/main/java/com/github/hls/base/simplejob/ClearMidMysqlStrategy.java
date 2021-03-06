package com.github.hls.base.simplejob;

import com.github.hls.base.simplejob.base.SimpleJobStrategy;
import com.github.hls.domain.SimpleJobDO;
import com.github.hls.utils.SimpleDBUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

@Service
public class ClearMidMysqlStrategy extends SimpleJobStrategy {
    @Resource
    private DataSource midDataSource;

    public void handle(SimpleJobDO simpleJob) {

        List<Map<String, Object>> resultList = SimpleDBUtils.queryListMap(simpleJob.getSelectSQL(), midDataSource);
        //TODO
    }

}
