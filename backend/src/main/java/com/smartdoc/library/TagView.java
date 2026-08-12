package com.smartdoc.library;
public class TagView {
    private final Long id; private final String name; private final String color;
    public TagView(TagRecord tag){this.id=tag.getId();this.name=tag.getName();this.color=tag.getColor();}
    public Long getId(){return id;} public String getName(){return name;} public String getColor(){return color;}
}
