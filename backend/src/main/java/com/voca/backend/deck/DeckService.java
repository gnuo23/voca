package com.voca.backend.deck;

import com.voca.backend.user.User;
import com.voca.backend.user.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class DeckService {

    private final DeckRepository deckRepository;
    private final UserService userService;

    public DeckService(DeckRepository deckRepository, UserService userService) {
        this.deckRepository = deckRepository;
        this.userService = userService;
    }

    @Transactional
    public DeckResponse create(Authentication authentication, DeckRequest request) {
        User owner = userService.currentUser(authentication);

        Deck deck = new Deck();
        deck.setOwner(owner);
        apply(deck, request);

        return DeckResponse.from(deckRepository.save(deck));
    }

    @Transactional(readOnly = true)
    public List<DeckResponse> list(Authentication authentication) {
        User owner = userService.currentUser(authentication);
        return deckRepository.findAllByOwnerIdOrderByUpdatedAtDesc(owner.getId())
                .stream()
                .map(DeckResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public DeckResponse get(Authentication authentication, Long deckId) {
        return DeckResponse.from(findOwnedDeck(authentication, deckId));
    }

    @Transactional
    public DeckResponse update(Authentication authentication, Long deckId, DeckRequest request) {
        Deck deck = findOwnedDeck(authentication, deckId);
        apply(deck, request);
        return DeckResponse.from(deck);
    }

    @Transactional
    public void delete(Authentication authentication, Long deckId) {
        Deck deck = findOwnedDeck(authentication, deckId);
        deckRepository.delete(deck);
    }

    private Deck findOwnedDeck(Authentication authentication, Long deckId) {
        User owner = userService.currentUser(authentication);
        return deckRepository.findByIdAndOwnerId(deckId, owner.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Deck not found"));
    }

    private void apply(Deck deck, DeckRequest request) {
        deck.setName(request.name().trim());
        deck.setDescription(request.description() == null ? null : request.description().trim());
    }
}
