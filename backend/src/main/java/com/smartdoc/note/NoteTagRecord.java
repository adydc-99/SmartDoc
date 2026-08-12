package com.smartdoc.note;
import com.baomidou.mybatisplus.annotation.*;
@TableName("note_tag")
public class NoteTagRecord {
    @TableId(type=IdType.AUTO) private Long id; private Long noteId; private Long tagId;
    public Long getId(){return id;} public void setId(Long value){id=value;}
    public Long getNoteId(){return noteId;} public void setNoteId(Long value){noteId=value;}
    public Long getTagId(){return tagId;} public void setTagId(Long value){tagId=value;}
}
