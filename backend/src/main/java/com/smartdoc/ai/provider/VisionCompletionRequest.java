package com.smartdoc.ai.provider;

public final class VisionCompletionRequest {
    private final String prompt;
    private final byte[] imageBytes;
    private final String mediaType;
    private final int maxOutputTokens;

    public VisionCompletionRequest(String prompt, byte[] imageBytes, String mediaType, int maxOutputTokens) {
        this.prompt = prompt == null ? "" : prompt;
        this.imageBytes = imageBytes == null ? new byte[0] : imageBytes.clone();
        this.mediaType = mediaType;
        this.maxOutputTokens = maxOutputTokens;
    }

    public String getPrompt() { return prompt; }
    public byte[] getImageBytes() { return imageBytes.clone(); }
    public String getMediaType() { return mediaType; }
    public int getMaxOutputTokens() { return maxOutputTokens; }
}
