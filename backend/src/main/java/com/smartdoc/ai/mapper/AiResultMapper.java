package com.smartdoc.ai.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartdoc.ai.AiResultRecord;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
public interface AiResultMapper extends BaseMapper<AiResultRecord>{
 @Select("SELECT * FROM ai_result WHERE document_id=#{documentId} AND cache_key=#{cacheKey} ORDER BY created_at DESC,id DESC LIMIT 1")
 AiResultRecord selectLatestByCacheKey(@Param("documentId")long documentId,@Param("cacheKey")String cacheKey);
}
