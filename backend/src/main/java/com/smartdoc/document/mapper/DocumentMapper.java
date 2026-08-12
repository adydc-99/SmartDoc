package com.smartdoc.document.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartdoc.document.DocumentRecord;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import java.time.LocalDateTime;
public interface DocumentMapper extends BaseMapper<DocumentRecord> {
    @Update("UPDATE document_record SET status='PROCESSING', error_message=NULL, content_text=NULL, page_count=NULL, summary=NULL, keywords=NULL, " +
            "processing_version=processing_version+1, updated_at=#{claimedAt} WHERE id=#{id} AND user_id=#{userId} AND status='FAILED' AND processing_version=#{expectedVersion}")
    int claimFailed(@Param("id")long id,@Param("userId")long userId,@Param("expectedVersion")long expectedVersion,@Param("claimedAt")LocalDateTime claimedAt);
}
