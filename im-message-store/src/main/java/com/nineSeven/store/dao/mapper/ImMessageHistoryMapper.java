package com.nineSeven.store.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nineSeven.store.dao.ImMessageHistoryEntity;
import org.springframework.stereotype.Repository;

import java.util.Collection;

@Repository
public interface ImMessageHistoryMapper extends BaseMapper<ImMessageHistoryEntity> {


    Integer insertBatchSomeColumn(Collection<ImMessageHistoryEntity> entityList);
}
