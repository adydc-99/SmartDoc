package com.smartdoc.ai.vision;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;
import java.time.LocalDateTime;

@Mapper
public interface VisionCacheMapper extends BaseMapper<VisionCacheRecord> {
    @Select("SELECT * FROM ai_vision_cache WHERE user_id=#{userId} AND content_sha256=#{contentSha256} AND provider_id=#{providerId} AND model=#{model} AND prompt_version=#{promptVersion} AND expires_at > #{now} LIMIT 1")
    VisionCacheRecord selectOwned(@Param("userId")long userId,@Param("contentSha256")String contentSha256,@Param("providerId")long providerId,@Param("model")String model,@Param("promptVersion")String promptVersion,@Param("now")LocalDateTime now);
}
