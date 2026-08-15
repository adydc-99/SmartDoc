package com.smartdoc.document.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartdoc.document.DocumentChunkRecord;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;
public interface DocumentChunkMapper extends BaseMapper<DocumentChunkRecord> {
    @Select("SELECT c.* FROM document_chunk c JOIN document_record d ON d.id=c.document_id " +
            "WHERE c.document_id=#{documentId} AND d.user_id=#{userId} ORDER BY c.page_number,c.chunk_index")
    List<DocumentChunkRecord> selectOwnedOrdered(@Param("userId")long userId,@Param("documentId")long documentId);
    @Select("SELECT c.* FROM document_chunk c JOIN document_record d ON d.id=c.document_id " +
            "WHERE c.document_id=#{documentId} AND d.user_id=#{userId} " +
            "ORDER BY CASE WHEN c.page_number=#{currentPage} THEN 0 ELSE 1 END,c.page_number,c.chunk_index LIMIT #{limit}")
    List<DocumentChunkRecord> selectOwnedForVisionContext(@Param("userId")long userId,@Param("documentId")long documentId,
            @Param("currentPage")int currentPage,@Param("limit")int limit);
    @Select("SELECT c.*,LOCATE(LOWER(#{needle}),LOWER(c.content)) match_position," +
            "SUBSTRING(c.content,1,LOCATE(LOWER(#{needle}),LOWER(c.content))-1) match_prefix " +
            "FROM document_chunk c JOIN document_record d ON d.id=c.document_id " +
            "WHERE c.document_id=#{documentId} AND d.user_id=#{userId} AND LOWER(c.content) LIKE LOWER(#{pattern}) ESCAPE '!' " +
            "ORDER BY c.page_number,c.chunk_index LIMIT #{limit}")
    List<DocumentChunkRecord> selectOwnedMatching(@Param("userId")long userId,@Param("documentId")long documentId,
            @Param("needle")String needle,@Param("pattern")String pattern,@Param("limit")int limit);
}
