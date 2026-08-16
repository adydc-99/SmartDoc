package com.smartdoc.library;

import com.smartdoc.document.InvalidDocumentException;
import com.smartdoc.document.DocumentNotFoundException;
import com.smartdoc.document.DocumentRecord;
import com.smartdoc.document.DocumentAccessPolicy;
import com.smartdoc.document.mapper.DocumentMapper;
import com.smartdoc.library.mapper.DocumentTagMapper;
import com.smartdoc.library.mapper.FolderMapper;
import com.smartdoc.library.mapper.TagMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LibraryServiceTest {
    private final FolderMapper folders = mock(FolderMapper.class);
    private final TagMapper tags = mock(TagMapper.class);
    private final DocumentTagMapper documentTags = mock(DocumentTagMapper.class);
    private final DocumentMapper documents = mock(DocumentMapper.class);
    private final LibraryService service = new LibraryService(folders, tags, documentTags, documents, new DocumentAccessPolicy());

    @Test
    void buildsSortedNestedFolderTree() {
        when(folders.selectList(any())).thenReturn(Arrays.asList(
                folder(4L, 1L, "Beta", 0), folder(2L, null, "Zulu", 0),
                folder(3L, 1L, "Alpha", 0), folder(1L, null, "Root", 0)));

        List<FolderNode> tree = service.folderTree();

        assertEquals(Arrays.asList("Root", "Zulu"), Arrays.asList(tree.get(0).getName(), tree.get(1).getName()));
        assertEquals(Arrays.asList("Alpha", "Beta"), Arrays.asList(
                tree.get(0).getChildren().get(0).getName(), tree.get(0).getChildren().get(1).getName()));
    }

    @Test
    void rejectsMovingFolderBelowItsDescendant() {
        when(folders.selectById(1L)).thenReturn(folder(1L, null, "Root", 0));
        when(folders.selectById(3L)).thenReturn(folder(3L, 1L, "Child", 0));

        assertThrows(InvalidDocumentException.class,
                () -> service.updateFolder(1L, new UpdateFolderRequest(null, 3L, true)));
    }

    @Test
    void rejectsBlankFolderRename() {
        when(folders.selectById(1L)).thenReturn(folder(1L, null, "Root", 0));

        assertThrows(InvalidDocumentException.class,
                () -> service.updateFolder(1L, new UpdateFolderRequest("  ", null, false)));
        verify(folders, never()).updateById(any());
    }

    @Test
    void rejectsInvalidTagColor() {
        assertThrows(InvalidDocumentException.class,
                () -> service.createTag(new CreateTagRequest("Research", "red")));
        verify(tags, never()).insert(any());
    }

    @Test
    void rejectsDeletingFolderContainingChildren() {
        when(folders.selectById(1L)).thenReturn(folder(1L, null, "Root", 0));
        when(folders.selectCount(any())).thenReturn(1L);

        InvalidDocumentException error = assertThrows(InvalidDocumentException.class,
                () -> service.deleteFolder(1L));

        assertEquals("Move folder contents before deleting it", error.getMessage());
        verify(folders, never()).deleteById(any(Long.class));
    }

    @Test
    void organizeHidesDocumentOwnedByAnotherUser() {
        DocumentRecord document = document(8L, 2L);
        when(documents.selectById(8L)).thenReturn(document);

        assertThrows(DocumentNotFoundException.class,
                () -> service.organize(1L, 8L, new OrganizationRequest(null, null, Arrays.asList(1L))));
        verify(documentTags, never()).delete(any());
        verify(documents, never()).updateById(any());
    }

    @Test
    void suppliedTagIdsReplaceExistingLinks() {
        when(documents.selectById(8L)).thenReturn(document(8L, 1L));
        when(tags.selectBatchIds(anyCollection())).thenReturn(Arrays.asList(tag(2L), tag(3L)));

        service.organize(1L, 8L, new OrganizationRequest(null, null, Arrays.asList(2L, 3L)));

        verify(documentTags).delete(any());
        verify(documentTags, times(2)).insert(any(DocumentTagRecord.class));
    }

    @Test
    void emptyTagIdsClearLinksWhileNullPreservesThem() {
        when(documents.selectById(8L)).thenReturn(document(8L, 1L));

        service.organize(1L, 8L, new OrganizationRequest(null, null, Arrays.asList()));
        verify(documentTags).delete(any());
        clearInvocations(documentTags);

        service.organize(1L, 8L, new OrganizationRequest(null, null, null));
        verify(documentTags, never()).delete(any());
        verify(documentTags, never()).insert(any());
    }

    @Test
    void rejectsUnknownSortInsteadOfPassingItToSql() {
        assertThrows(InvalidDocumentException.class,
                () -> service.listDocuments(1L, null, null, null, null, "updated_at desc; drop table tag", null));
        verifyNoInteractions(documents);
    }

    @ParameterizedTest
    @ValueSource(strings={"updated-desc","updated_desc","updated:desc","updated,desc"})
    void acceptsDocumentedSortSeparators(String sort) {
        when(documents.selectList(any())).thenReturn(Collections.emptyList());

        assertEquals(Collections.emptyList(), service.listDocuments(1L, null, null, null, null, sort, null));

        verify(documents).selectList(any());
    }

    @Test
    void batchesTagHydrationForAllReturnedDocuments() {
        DocumentRecord first = document(8L, 1L); first.setName("First");
        DocumentRecord second = document(9L, 1L); second.setName("Second");
        when(documents.selectList(any())).thenReturn(Arrays.asList(first, second));
        DocumentTagRecord firstBeta = link(8L, 3L);
        DocumentTagRecord firstAlpha = link(8L, 2L);
        DocumentTagRecord secondAlpha = link(9L, 2L);
        when(documentTags.selectList(any())).thenReturn(Arrays.asList(firstBeta, firstAlpha, secondAlpha));
        when(tags.selectBatchIds(anyCollection())).thenReturn(Arrays.asList(namedTag(3L, "Beta"), namedTag(2L, "Alpha")));

        List<DocumentListItem> result = service.listDocuments(1L, null, null, null, null, null, null);

        assertEquals(Arrays.asList(8L, 9L), Arrays.asList(result.get(0).getId(), result.get(1).getId()));
        assertEquals(Arrays.asList("Alpha", "Beta"), Arrays.asList(
                result.get(0).getTags().get(0).getName(), result.get(0).getTags().get(1).getName()));
        assertEquals(Collections.singletonList("Alpha"), Collections.singletonList(result.get(1).getTags().get(0).getName()));
        verify(documentTags, times(1)).selectList(any());
        verify(tags, times(1)).selectBatchIds(anyCollection());
    }

    private FolderRecord folder(long id, Long parentId, String name, int sortOrder) {
        FolderRecord folder = new FolderRecord();
        folder.setId(id);
        folder.setParentId(parentId);
        folder.setName(name);
        folder.setSortOrder(sortOrder);
        return folder;
    }

    private DocumentRecord document(long id, long userId) {
        DocumentRecord document = new DocumentRecord(); document.setId(id); document.setUserId(userId); return document;
    }

    private TagRecord tag(long id) {
        TagRecord tag = new TagRecord(); tag.setId(id); tag.setName("tag-" + id); tag.setColor("#123456"); return tag;
    }

    private TagRecord namedTag(long id, String name) {
        TagRecord tag = tag(id); tag.setName(name); return tag;
    }

    private DocumentTagRecord link(long documentId, long tagId) {
        DocumentTagRecord link = new DocumentTagRecord(); link.setDocumentId(documentId); link.setTagId(tagId); return link;
    }
}
