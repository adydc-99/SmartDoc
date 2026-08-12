package com.smartdoc.library;

import com.smartdoc.auth.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
public class LibraryController {
    private final LibraryService service;
    public LibraryController(LibraryService service){this.service=service;}
    @GetMapping("/api/folders") public List<FolderNode> folders(HttpServletRequest request){CurrentUser.from(request).getUserId();return service.folderTree();}
    @PostMapping("/api/folders") @ResponseStatus(HttpStatus.CREATED) public FolderNode createFolder(@RequestBody CreateFolderRequest body,HttpServletRequest request){CurrentUser.from(request).getUserId();return service.createFolder(body);}
    @PatchMapping("/api/folders/{id}") public FolderNode updateFolder(@PathVariable long id,@RequestBody UpdateFolderRequest body,HttpServletRequest request){CurrentUser.from(request).getUserId();return service.updateFolder(id,body);}
    @DeleteMapping("/api/folders/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteFolder(@PathVariable long id,HttpServletRequest request){CurrentUser.from(request).getUserId();service.deleteFolder(id);}
    @GetMapping("/api/tags") public List<TagView> tags(HttpServletRequest request){CurrentUser.from(request).getUserId();return service.listTags();}
    @PostMapping("/api/tags") @ResponseStatus(HttpStatus.CREATED) public TagView createTag(@RequestBody CreateTagRequest body,HttpServletRequest request){CurrentUser.from(request).getUserId();return service.createTag(body);}
    @DeleteMapping("/api/tags/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteTag(@PathVariable long id,HttpServletRequest request){CurrentUser.from(request).getUserId();service.deleteTag(id);}
    @PatchMapping("/api/documents/{id}/organization") public DocumentListItem organize(@PathVariable long id,@RequestBody OrganizationRequest body,HttpServletRequest request){return service.organize(CurrentUser.from(request).getUserId(),id,body);}
    @GetMapping("/api/library/documents") public List<DocumentListItem> documents(@RequestParam(required=false)Long folderId,@RequestParam(required=false)Long tagId,@RequestParam(required=false)Boolean favorite,@RequestParam(required=false)String type,@RequestParam(required=false)String sort,@RequestParam(required=false)String query,HttpServletRequest request){return service.listDocuments(CurrentUser.from(request).getUserId(),folderId,tagId,favorite,type,sort,query);}
}
