package com.nineSeven.group.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nineSeven.group.dao.ImGroupEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;

@Mapper
public interface ImGroupMapper extends BaseMapper<ImGroupEntity> {

    @Select(" <script> " +
            " select max(sequence) from im_group where app_id = #{appId} and group_id in " +
            "<foreach collection='groupId' index='index' item='id' separator=',' close=')' open='('>" +
            " #{id} " +
            "</foreach>" +
            " </script> ")
    Long getGroupMaxSeq(Collection<String> data, Integer appId);
}
