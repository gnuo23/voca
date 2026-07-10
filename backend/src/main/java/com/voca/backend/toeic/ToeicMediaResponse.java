package com.voca.backend.toeic;

public record ToeicMediaResponse(
        String url,
        String fileType,
        int displayOrder
) {
    public static ToeicMediaResponse from(ToeicGroupMedia media) {
        return new ToeicMediaResponse(
                media.getUrl(),
                media.getFileType(),
                media.getDisplayOrder()
        );
    }
}
