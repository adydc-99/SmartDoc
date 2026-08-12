package com.smartdoc.document.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartdoc.document.DocumentRecord;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Select;
import java.time.LocalDateTime;
public interface DocumentMapper extends BaseMapper<DocumentRecord> {
    @Select("SELECT * FROM document_record WHERE id=#{id} AND user_id=#{userId}")
    DocumentRecord selectOwned(@Param("id")long id,@Param("userId")long userId);
    @Select("SELECT * FROM document_record WHERE id=#{id} AND user_id=#{userId} FOR UPDATE")
    DocumentRecord selectOwnedForUpdate(@Param("id")long id,@Param("userId")long userId);
    @Select("SELECT * FROM document_record WHERE id=#{id} FOR UPDATE")
    DocumentRecord selectForUpdate(@Param("id")long id);
    @Update("UPDATE document_record SET status='DELETING', updated_at=#{updatedAt} WHERE id=#{id} AND user_id=#{userId} AND status != 'DELETING'")
    int claimDeleting(@Param("id")long id,@Param("userId")long userId,@Param("updatedAt")LocalDateTime updatedAt);
    @Update("UPDATE document_record SET last_opened_at=#{openedAt} WHERE id=#{id} AND user_id=#{userId}")
    int touchLastOpened(@Param("id")long id,@Param("userId")long userId,@Param("openedAt")LocalDateTime openedAt);
    @Update("UPDATE document_record SET status='PROCESSING', error_message=NULL, content_text=NULL, page_count=NULL, summary=NULL, keywords=NULL, " +
            "processing_version=processing_version+1, updated_at=#{claimedAt} WHERE id=#{id} AND user_id=#{userId} AND status='FAILED' AND processing_version=#{expectedVersion}")
    int claimFailed(@Param("id")long id,@Param("userId")long userId,@Param("expectedVersion")long expectedVersion,@Param("claimedAt")LocalDateTime claimedAt);
}
