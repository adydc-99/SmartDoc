package com.smartdoc.ai.vision;

public final class NormalizedImage {
    private final byte[] bytes; private final String mediaType;
    public NormalizedImage(byte[] bytes,String mediaType){this.bytes=bytes.clone();this.mediaType=mediaType;}
    public byte[] getBytes(){return bytes.clone();} public String getMediaType(){return mediaType;}
}
