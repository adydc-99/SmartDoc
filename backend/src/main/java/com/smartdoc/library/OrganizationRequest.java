package com.smartdoc.library;
import java.util.List;
public class OrganizationRequest {
    private Long folderId; private Boolean favorite; private List<Long> tagIds;
    private boolean folderIdSupplied; private boolean favoriteSupplied;
    public OrganizationRequest() {}
    public OrganizationRequest(Long folderId,Boolean favorite,List<Long> tagIds){this.folderId=folderId;this.favorite=favorite;this.tagIds=tagIds;this.folderIdSupplied=folderId!=null;this.favoriteSupplied=favorite!=null;}
    public Long getFolderId(){return folderId;} public void setFolderId(Long folderId){this.folderId=folderId;this.folderIdSupplied=true;}
    public Boolean getFavorite(){return favorite;} public void setFavorite(Boolean favorite){this.favorite=favorite;this.favoriteSupplied=true;}
    public List<Long> getTagIds(){return tagIds;} public void setTagIds(List<Long> tagIds){this.tagIds=tagIds;}
    public boolean isFolderIdSupplied(){return folderIdSupplied;} public boolean isFavoriteSupplied(){return favoriteSupplied;}
}
