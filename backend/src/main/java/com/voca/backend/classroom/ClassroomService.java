package com.voca.backend.classroom;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.voca.backend.deck.Deck;
import com.voca.backend.deck.DeckResponse;
import com.voca.backend.deck.DeckRepository;
import com.voca.backend.deck.DeckService;
import com.voca.backend.user.User;
import com.voca.backend.user.UserService;
import com.voca.backend.vocab.UserProgress;
import com.voca.backend.vocab.UserProgressRepository;
import com.voca.backend.vocab.VocabItem;
import com.voca.backend.vocab.VocabItemRepository;
import com.voca.backend.vocab.VocabProgressStatus;

@Service
public class ClassroomService {

    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Set<VocabProgressStatus> LEARNED_STATUSES = Set.of(
            VocabProgressStatus.REVIEW,
            VocabProgressStatus.MASTERED
    );

    private final ClassroomRepository classroomRepository;
    private final ClassroomMemberRepository memberRepository;
    private final ClassroomDeckRepository classroomDeckRepository;
    private final DeckRepository deckRepository;
    private final VocabItemRepository vocabItemRepository;
    private final UserProgressRepository progressRepository;
    private final UserService userService;
    private final DeckService deckService;

    public ClassroomService(
            ClassroomRepository classroomRepository,
            ClassroomMemberRepository memberRepository,
            ClassroomDeckRepository classroomDeckRepository,
            DeckRepository deckRepository,
            VocabItemRepository vocabItemRepository,
            UserProgressRepository progressRepository,
            UserService userService,
            DeckService deckService
    ) {
        this.classroomRepository = classroomRepository;
        this.memberRepository = memberRepository;
        this.classroomDeckRepository = classroomDeckRepository;
        this.deckRepository = deckRepository;
        this.vocabItemRepository = vocabItemRepository;
        this.progressRepository = progressRepository;
        this.userService = userService;
        this.deckService = deckService;
    }

    @Transactional
    public ClassroomResponse create(Authentication authentication, ClassroomRequest request) {
        User owner = userService.currentUser(authentication);

        Classroom classroom = new Classroom();
        classroom.setOwner(owner);
        apply(classroom, request);
        classroom.setInviteCode(generateUniqueCode());
        classroomRepository.save(classroom);

        ClassroomMember member = new ClassroomMember();
        member.setClassroom(classroom);
        member.setUser(owner);
        member.setRole(ClassroomRole.OWNER);
        memberRepository.save(member);

        return toResponse(classroom, member, true);
    }

    @Transactional(readOnly = true)
    public List<ClassroomResponse> list(Authentication authentication) {
        User user = userService.currentUser(authentication);
        return toResponses(memberRepository.findAllByUserIdOrderByJoinedAtDesc(user.getId()), false);
    }

    @Transactional(readOnly = true)
    public ClassroomResponse get(Authentication authentication, String classroomId) {
        User user = userService.currentUser(authentication);
        ClassroomMember membership = requireMembership(classroomId, user);
        return toResponse(membership.getClassroom(), membership, true);
    }

    @Transactional
    public ClassroomResponse update(Authentication authentication, String classroomId, ClassroomRequest request) {
        User user = userService.currentUser(authentication);
        ClassroomMember membership = requireOwner(classroomId, user);
        Classroom classroom = membership.getClassroom();
        apply(classroom, request);
        return toResponse(classroom, membership, true);
    }

    @Transactional
    public void delete(Authentication authentication, String classroomId) {
        User user = userService.currentUser(authentication);
        ClassroomMember membership = requireOwner(classroomId, user);
        classroomRepository.delete(membership.getClassroom());
    }

    @Transactional
    public ClassroomResponse join(Authentication authentication, JoinClassroomRequest request) {
        User user = userService.currentUser(authentication);
        String code = cleanCode(request.code());
        Classroom classroom = classroomRepository.findByInviteCode(code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Class code not found"));

        ClassroomMember membership = memberRepository.findByClassroomIdAndUserId(classroom.getId(), user.getId())
                .orElseGet(() -> {
                    ClassroomMember member = new ClassroomMember();
                    member.setClassroom(classroom);
                    member.setUser(user);
                    member.setRole(classroom.getOwner().getId().equals(user.getId()) ? ClassroomRole.OWNER : ClassroomRole.STUDENT);
                    return memberRepository.save(member);
                });

        return toResponse(classroom, membership, true);
    }

    @Transactional
    public ClassroomResponse rotateCode(Authentication authentication, String classroomId) {
        User user = userService.currentUser(authentication);
        ClassroomMember membership = requireOwner(classroomId, user);
        Classroom classroom = membership.getClassroom();
        classroom.setInviteCode(generateUniqueCode());
        return toResponse(classroom, membership, true);
    }

    @Transactional
    public ClassroomResponse addDeck(Authentication authentication, String classroomId, AddClassroomDeckRequest request) {
        User user = userService.currentUser(authentication);
        ClassroomMember membership = requireOwner(classroomId, user);
        Classroom classroom = membership.getClassroom();
        Deck deck = deckRepository.findByIdAndOwnerId(request.deckId(), user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Deck not found"));

        if (!classroomDeckRepository.existsByClassroomIdAndDeckId(classroom.getId(), deck.getId())) {
            ClassroomDeck classroomDeck = new ClassroomDeck();
            classroomDeck.setClassroom(classroom);
            classroomDeck.setDeck(deck);
            classroomDeckRepository.save(classroomDeck);
        }

        return toResponse(classroom, membership, true);
    }

    @Transactional(readOnly = true)
    public DeckResponse getDeck(Authentication authentication, String classroomId, Long deckId) {
        User user = userService.currentUser(authentication);
        ClassroomMember membership = requireMembership(classroomId, user);
        ClassroomDeck classroomDeck = classroomDeckRepository.findByClassroomIdAndDeckId(membership.getClassroom().getId(), deckId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Deck not found in class"));
        return deckService.toResponse(classroomDeck.getDeck(), user);
    }

    @Transactional
    public ClassroomResponse removeDeck(Authentication authentication, String classroomId, Long deckId) {
        User user = userService.currentUser(authentication);
        ClassroomMember membership = requireOwner(classroomId, user);
        Classroom classroom = membership.getClassroom();
        classroomDeckRepository.findByClassroomIdAndDeckId(classroom.getId(), deckId)
                .ifPresent(classroomDeckRepository::delete);
        return toResponse(classroom, membership, true);
    }

    private ClassroomMember requireMembership(String classroomId, User user) {
        Classroom classroom = findClassroom(classroomId);
        return memberRepository.findByClassroomIdAndUserId(classroom.getId(), user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Class not found"));
    }

    private ClassroomMember requireOwner(String classroomId, User user) {
        ClassroomMember membership = requireMembership(classroomId, user);
        if (membership.getRole() != ClassroomRole.OWNER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the class owner can do this");
        }
        return membership;
    }

    private Classroom findClassroom(String classroomId) {
        String normalized = cleanCode(classroomId);
        if (normalized.matches("\\d+")) {
            try {
                return classroomRepository.findById(Long.parseLong(normalized))
                        .or(() -> classroomRepository.findByInviteCode(normalized))
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Class not found"));
            } catch (NumberFormatException ignored) {
                // Fall through to invite-code lookup below.
            }
        }
        return classroomRepository.findByInviteCode(normalized)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Class not found"));
    }

    private ClassroomResponse toResponse(Classroom classroom, ClassroomMember viewerMembership, boolean includeDetails) {
        return toResponses(List.of(viewerMembership), includeDetails).getFirst();
    }

    private List<ClassroomResponse> toResponses(List<ClassroomMember> viewerMemberships, boolean includeDetails) {
        if (viewerMemberships.isEmpty()) {
            return List.of();
        }

        List<Long> classroomIds = viewerMemberships.stream().map(member -> member.getClassroom().getId()).toList();
        List<ClassroomDeck> classroomDecks = classroomDeckRepository.findAllByClassroomIdInOrderByAddedAtDesc(classroomIds);
        Map<Long, List<ClassroomDeck>> decksByClassroom = classroomDecks.stream()
                .collect(Collectors.groupingBy(deck -> deck.getClassroom().getId()));
        List<Long> deckIds = classroomDecks.stream().map(deck -> deck.getDeck().getId()).toList();
        List<VocabItem> vocabItems = deckIds.isEmpty() ? List.of() : vocabItemRepository.findAllByDeckIdIn(deckIds);
        Map<Long, List<VocabItem>> vocabByDeck = vocabItems.stream()
                .collect(Collectors.groupingBy(item -> item.getDeck().getId()));
        List<Long> allVocabIds = vocabItems.stream().map(VocabItem::getId).toList();

        List<ClassroomMember> members = memberRepository.findAllByClassroomIdInOrderByJoinedAtAsc(classroomIds);
        Map<Long, List<ClassroomMember>> membersByClassroom = members.stream()
                .collect(Collectors.groupingBy(member -> member.getClassroom().getId()));
        Long viewerUserId = viewerMemberships.getFirst().getUser().getId();
        List<UserProgress> viewerProgress = allVocabIds.isEmpty()
                ? List.of()
                : progressRepository.findAllByUserIdAndVocabItemIdIn(viewerUserId, allVocabIds);
        Map<Long, List<UserProgress>> viewerProgressByVocab = viewerProgress.stream()
                .collect(Collectors.groupingBy(progress -> progress.getVocabItem().getId()));

        boolean needsMemberProgress = includeDetails && viewerMemberships.stream()
                .anyMatch(membership -> membership.getRole() == ClassroomRole.OWNER);
        Map<Long, List<UserProgress>> progressByUser = needsMemberProgress && !allVocabIds.isEmpty()
                ? progressRepository.findAllByUserIdInAndVocabItemIdIn(
                                members.stream().map(member -> member.getUser().getId()).distinct().toList(), allVocabIds)
                        .stream()
                        .collect(Collectors.groupingBy(progress -> progress.getUser().getId()))
                : Map.of();

        return viewerMemberships.stream()
                .map(viewerMembership -> toResponse(
                        viewerMembership,
                        includeDetails,
                        decksByClassroom.getOrDefault(viewerMembership.getClassroom().getId(), List.of()),
                        membersByClassroom.getOrDefault(viewerMembership.getClassroom().getId(), List.of()),
                        vocabByDeck,
                        viewerProgressByVocab,
                        progressByUser))
                .toList();
    }

    private ClassroomResponse toResponse(
            ClassroomMember viewerMembership,
            boolean includeDetails,
            List<ClassroomDeck> classroomDecks,
            List<ClassroomMember> members,
            Map<Long, List<VocabItem>> vocabByDeck,
            Map<Long, List<UserProgress>> viewerProgressByVocab,
            Map<Long, List<UserProgress>> progressByUser
    ) {
        Classroom classroom = viewerMembership.getClassroom();
        List<VocabItem> vocabItems = classroomDecks.stream()
                .flatMap(deck -> vocabByDeck.getOrDefault(deck.getDeck().getId(), List.of()).stream())
                .toList();
        List<Long> vocabIds = vocabItems.stream().map(VocabItem::getId).toList();
        List<UserProgress> viewerProgress = vocabIds.stream()
                .flatMap(vocabId -> viewerProgressByVocab.getOrDefault(vocabId, List.of()).stream())
                .toList();

        List<ClassroomDeckResponse> deckResponses = includeDetails
                ? toDeckResponses(classroomDecks, vocabByDeck, viewerProgressByVocab)
                : List.of();
        List<ClassroomMemberProgressResponse> memberResponses = includeDetails && viewerMembership.getRole() == ClassroomRole.OWNER
                ? members.stream().map(member -> progressForMember(
                        member,
                        vocabIds,
                        progressForVocabIds(progressByUser.getOrDefault(member.getUser().getId(), List.of()), vocabIds)))
                        .toList()
                : List.of();

        return new ClassroomResponse(classroom.getId(), classroom.getName(), classroom.getDescription(),
                classroom.getInviteCode(), viewerMembership.getRole(), classroomDecks.size(), members.size(),
                vocabItems.size(), learnedWords(viewerProgress), deckResponses, memberResponses,
                classroom.getCreatedAt(), classroom.getUpdatedAt());
    }

    private List<ClassroomDeckResponse> toDeckResponses(
            List<ClassroomDeck> classroomDecks,
            Map<Long, List<VocabItem>> vocabByDeck,
            Map<Long, List<UserProgress>> viewerProgressByVocab
    ) {
        return classroomDecks.stream().map(classroomDeck -> {
            Deck deck = classroomDeck.getDeck();
            List<VocabItem> deckItems = vocabByDeck.getOrDefault(deck.getId(), List.of());
            List<UserProgress> progress = deckItems.stream()
                    .flatMap(item -> viewerProgressByVocab.getOrDefault(item.getId(), List.of()).stream())
                    .toList();
            long learned = learnedWords(progress);
            long dueToday = progress.stream()
                    .filter(item -> item.getStatus() != VocabProgressStatus.NEW)
                    .filter(item -> item.getNextReviewAt() != null)
                    .filter(item -> !item.getNextReviewAt().isAfter(LocalDateTime.now()))
                    .count();
            long total = deckItems.size();
            return new ClassroomDeckResponse(deck.getId(), deck.getName(), deck.getDescription(), total, learned,
                    Math.max(0, total - learned), dueToday, classroomDeck.getAddedAt());
        }).toList();
    }

    private List<UserProgress> progressForVocabIds(List<UserProgress> progress, List<Long> vocabIds) {
        Set<Long> vocabIdSet = Set.copyOf(vocabIds);
        return progress.stream().filter(item -> vocabIdSet.contains(item.getVocabItem().getId())).toList();
    }

    private ClassroomMemberProgressResponse progressForMember(
            ClassroomMember member,
            List<Long> vocabIds,
            List<UserProgress> progress
    ) {
        long touched = progress.size();

        long learned = progress.stream().filter(item -> LEARNED_STATUSES.contains(item.getStatus())).count();
        long mastered = progress.stream().filter(item -> item.getStatus() == VocabProgressStatus.MASTERED).count();
        long review = progress.stream().filter(item -> item.getStatus() == VocabProgressStatus.REVIEW).count();
        long difficult = progress.stream().filter(item -> item.getStatus() == VocabProgressStatus.DIFFICULT).count();
        long correct = progress.stream().mapToLong(UserProgress::getCorrectCount).sum();
        long wrong = progress.stream().mapToLong(UserProgress::getWrongCount).sum();
        long totalAnswers = correct + wrong;
        int accuracy = totalAnswers == 0 ? 0 : Math.round((correct * 100f) / totalAnswers);
        LocalDateTime lastActivity = progress.stream()
                .map(this::activityAt)
                .filter(value -> value != null)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        return new ClassroomMemberProgressResponse(
                member.getUser().getId(),
                member.getUser().getDisplayName(),
                member.getUser().getEmail(),
                member.getRole(),
                vocabIds.size(),
                touched,
                learned,
                mastered,
                review,
                difficult,
                correct,
                wrong,
                accuracy,
                lastActivity,
                member.getJoinedAt()
        );
    }

    private long learnedWords(List<UserProgress> progress) {
        return progress
                .stream()
                .filter(item -> LEARNED_STATUSES.contains(item.getStatus()))
                .count();
    }

    private LocalDateTime activityAt(UserProgress progress) {
        if (progress.getLastReviewedAt() != null) {
            return progress.getLastReviewedAt();
        }
        return progress.getLastMarkedAt();
    }

    private void apply(Classroom classroom, ClassroomRequest request) {
        classroom.setName(request.name().trim());
        classroom.setDescription(request.description() == null || request.description().trim().isEmpty()
                ? null
                : request.description().trim());
    }

    private String cleanCode(String code) {
        return code == null ? "" : code.trim().replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
    }

    private String generateUniqueCode() {
        String code;
        do {
            code = randomCode();
        } while (classroomRepository.existsByInviteCode(code));
        return code;
    }

    private String randomCode() {
        StringBuilder builder = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            builder.append(CODE_ALPHABET.charAt(SECURE_RANDOM.nextInt(CODE_ALPHABET.length())));
        }
        return builder.toString();
    }
}
