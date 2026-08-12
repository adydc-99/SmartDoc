package com.smartdoc.note.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartdoc.note.NoteRecord;
import org.apache.ibatis.annotations.*;
import java.util.List;
public interface NoteMapper extends BaseMapper<NoteRecord>{
 @Select("SELECT n.* FROM note n JOIN document_record d ON d.id=n.document_id WHERE n.id=#{id} AND d.user_id=#{userId}") NoteRecord selectOwned(@Param("userId")long userId,@Param("id")long id);
 @Select("SELECT n.* FROM note n JOIN document_record d ON d.id=n.document_id WHERE n.id=#{id} AND d.user_id=#{userId} FOR UPDATE") NoteRecord selectOwnedForUpdate(@Param("userId")long userId,@Param("id")long id);
 @Select("SELECT n.* FROM note n JOIN document_record d ON d.id=n.document_id WHERE n.document_id=#{documentId} AND d.user_id=#{userId} ORDER BY n.page_number,n.created_at,n.id") List<NoteRecord> selectDocumentNotes(@Param("userId")long userId,@Param("documentId")long documentId);
 @Select("<script>SELECT DISTINCT n.* FROM note n JOIN document_record d ON d.id=n.document_id <if test='tagId != null'>JOIN note_tag nt ON nt.note_id=n.id</if> WHERE d.user_id=#{userId}<if test='documentId != null'> AND n.document_id=#{documentId}</if><if test='favorite != null'> AND n.favorite=#{favorite}</if><if test='tagId != null'> AND nt.tag_id=#{tagId}</if><if test='pattern != null'> AND (LOWER(n.content_markdown) LIKE LOWER(#{pattern}) ESCAPE '!' OR LOWER(COALESCE(n.source_text,'')) LIKE LOWER(#{pattern}) ESCAPE '!')</if> ORDER BY n.updated_at DESC,n.id DESC LIMIT 200</script>") List<NoteRecord> searchOwned(@Param("userId")long userId,@Param("pattern")String pattern,@Param("favorite")Boolean favorite,@Param("documentId")Long documentId,@Param("tagId")Long tagId);
 @Select("SELECT COUNT(*) FROM note WHERE document_id=#{documentId}") long countDocument(@Param("documentId")long documentId);
 @Select("SELECT COUNT(*) FROM note WHERE document_id=#{documentId} AND source_text IS NOT NULL AND LENGTH(TRIM(source_text))>0") long countExcerpts(@Param("documentId")long documentId);
 @Delete("DELETE FROM note_tag WHERE note_id IN (SELECT id FROM note WHERE document_id=#{documentId})") int deleteTagsForDocument(@Param("documentId")long documentId);
 @Delete("DELETE FROM note WHERE document_id=#{documentId}") int deleteDocumentNotes(@Param("documentId")long documentId);
}
