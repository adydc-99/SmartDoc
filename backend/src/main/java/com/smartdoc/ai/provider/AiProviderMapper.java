package com.smartdoc.ai.provider;
import com.baomidou.mybatisplus.core.mapper.BaseMapper; import org.apache.ibatis.annotations.*; import org.apache.ibatis.annotations.Mapper; import java.time.LocalDateTime; import java.util.List;
@Mapper public interface AiProviderMapper extends BaseMapper<AiProviderConfig> {
 @Select("SELECT * FROM ai_provider_config WHERE id=#{id} AND user_id=#{userId}") AiProviderConfig selectOwned(@Param("id")long id,@Param("userId")long userId);
 @Select("SELECT * FROM ai_provider_config WHERE id=#{id} AND user_id=#{userId} AND enabled=TRUE") AiProviderConfig selectOwnedEnabled(@Param("id")long id,@Param("userId")long userId);
 @Select("SELECT * FROM ai_provider_config WHERE user_id=#{userId} ORDER BY created_at,id") List<AiProviderConfig> listOwned(@Param("userId")long userId);
 @Update("UPDATE ai_provider_config SET encrypted_api_key=#{ciphertext},updated_at=#{updatedAt} WHERE id=#{id} AND user_id=#{userId}") int updateEncryptedKeyOwned(@Param("id")long id,@Param("userId")long userId,@Param("ciphertext")String ciphertext,@Param("updatedAt")LocalDateTime updatedAt);
 @Update("UPDATE ai_provider_config SET encrypted_api_key=NULL,updated_at=#{updatedAt} WHERE id=#{id} AND user_id=#{userId}") int clearEncryptedKeyOwned(@Param("id")long id,@Param("userId")long userId,@Param("updatedAt")LocalDateTime updatedAt);
 @Update("UPDATE ai_provider_config SET display_name=#{p.displayName},preset_code=#{p.presetCode},protocol=#{p.protocol},base_url=#{p.baseUrl},model=#{p.model},supports_text=#{p.supportsText},supports_vision=#{p.supportsVision},enabled=#{p.enabled},updated_at=#{p.updatedAt} WHERE id=#{p.id} AND user_id=#{userId}") int updateOwned(@Param("p")AiProviderConfig p,@Param("userId")long userId);
 @Delete("DELETE FROM ai_provider_config WHERE id=#{id} AND user_id=#{userId}") int deleteOwned(@Param("id")long id,@Param("userId")long userId);
}
