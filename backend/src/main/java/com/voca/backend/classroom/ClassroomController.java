package com.voca.backend.classroom;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/classes")
public class ClassroomController {

    private final ClassroomService classroomService;

    public ClassroomController(ClassroomService classroomService) {
        this.classroomService = classroomService;
    }

    @GetMapping
    public List<ClassroomResponse> list(Authentication authentication) {
        return classroomService.list(authentication);
    }

    @PostMapping
    public ClassroomResponse create(Authentication authentication, @Valid @RequestBody ClassroomRequest request) {
        return classroomService.create(authentication, request);
    }

    @GetMapping("/{classroomId}")
    public ClassroomResponse get(Authentication authentication, @PathVariable Long classroomId) {
        return classroomService.get(authentication, classroomId);
    }

    @PutMapping("/{classroomId}")
    public ClassroomResponse update(
            Authentication authentication,
            @PathVariable Long classroomId,
            @Valid @RequestBody ClassroomRequest request
    ) {
        return classroomService.update(authentication, classroomId, request);
    }

    @DeleteMapping("/{classroomId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication authentication, @PathVariable Long classroomId) {
        classroomService.delete(authentication, classroomId);
    }

    @PostMapping("/join")
    public ClassroomResponse join(Authentication authentication, @Valid @RequestBody JoinClassroomRequest request) {
        return classroomService.join(authentication, request);
    }

    @PostMapping("/{classroomId}/rotate-code")
    public ClassroomResponse rotateCode(Authentication authentication, @PathVariable Long classroomId) {
        return classroomService.rotateCode(authentication, classroomId);
    }

    @PostMapping("/{classroomId}/decks")
    public ClassroomResponse addDeck(
            Authentication authentication,
            @PathVariable Long classroomId,
            @Valid @RequestBody AddClassroomDeckRequest request
    ) {
        return classroomService.addDeck(authentication, classroomId, request);
    }

    @DeleteMapping("/{classroomId}/decks/{deckId}")
    public ClassroomResponse removeDeck(
            Authentication authentication,
            @PathVariable Long classroomId,
            @PathVariable Long deckId
    ) {
        return classroomService.removeDeck(authentication, classroomId, deckId);
    }
}
