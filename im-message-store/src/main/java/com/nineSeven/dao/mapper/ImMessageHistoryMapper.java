package com.nineSeven.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nineSeven.dao.ImMessageHistoryEntity;
import org.springframework.stereotype.Repository;

import java.util.Collection;

@Repository
public interface ImMessageHistoryMapper extends BaseMapper<ImMessageHistoryEntity> {


    Integer insertBatchSomeColumn(Collection<ImMessageHistoryEntity> entityList);
}
