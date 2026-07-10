package com.voca.backend.toeic;

import java.util.List;

public record ToeicGroupResponse(
        Long id,
        String questionPart,
        int groupOrder,
        String passageText,
        List<ToeicMediaResponse> media,
        List<ToeicQuestionResponse> questions
) {
    public static ToeicGroupResponse from(ToeicQuestionGroup group) {
        return new ToeicGroupResponse(
                group.getId(),
                group.getQuestionPart(),
                group.getGroupOrder(),
                group.getPassageText(),
                group.getMedia().stream().map(ToeicMediaResponse::from).toList(),
                group.getQuestions().stream().map(ToeicQuestionResponse::from).toList()
        );
    }
}
