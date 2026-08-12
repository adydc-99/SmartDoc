package com.smartdoc.reader.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartdoc.reader.ReadingProgressRecord;
import org.apache.ibatis.annotations.*;
import java.time.LocalDateTime;

public interface ReadingProgressMapper extends BaseMapper<ReadingProgressRecord> {
    @Select("SELECT p.* FROM reading_progress p JOIN document_record d ON d.id=p.document_id " +
            "WHERE p.document_id=#{documentId} AND d.user_id=#{userId}")
    ReadingProgressRecord selectOwned(@Param("userId") long userId, @Param("documentId") long documentId);

    @Update("UPDATE reading_progress SET page_number=#{pageNumber}, scroll_ratio=#{scrollRatio}, zoom=#{zoom}, updated_at=#{updatedAt} " +
            "WHERE document_id=#{documentId} AND EXISTS (SELECT 1 FROM document_record d WHERE d.id=#{documentId} AND d.user_id=#{userId})")
    int updateOwned(@Param("userId") long userId, @Param("documentId") long documentId,
                    @Param("pageNumber") int pageNumber, @Param("scrollRatio") double scrollRatio,
                    @Param("zoom") double zoom, @Param("updatedAt") LocalDateTime updatedAt);

    @Insert("INSERT INTO reading_progress(document_id,page_number,scroll_ratio,zoom,updated_at) " +
            "SELECT id,#{pageNumber},#{scrollRatio},#{zoom},#{updatedAt} FROM document_record WHERE id=#{documentId} AND user_id=#{userId}")
    int insertOwned(@Param("userId") long userId, @Param("documentId") long documentId,
                    @Param("pageNumber") int pageNumber, @Param("scrollRatio") double scrollRatio,
                    @Param("zoom") double zoom, @Param("updatedAt") LocalDateTime updatedAt);
}
