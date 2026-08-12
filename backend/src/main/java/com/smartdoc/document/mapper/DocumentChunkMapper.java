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
}
