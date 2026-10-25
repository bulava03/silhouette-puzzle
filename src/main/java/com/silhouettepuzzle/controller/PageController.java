package com.silhouettepuzzle.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.silhouettepuzzle.dto.RegisterForm;
import com.silhouettepuzzle.entity.Attempt;
import com.silhouettepuzzle.entity.Level;
import com.silhouettepuzzle.entity.Player;
import com.silhouettepuzzle.security.PlayerPrincipal;
import com.silhouettepuzzle.service.AttemptService;
import com.silhouettepuzzle.service.LevelService;
import com.silhouettepuzzle.service.PlayerService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import tools.jackson.databind.json.JsonMapper;

/**
 * The pages, rendered on the server. The game page is the only one with a
 * script, and it gets just what it needs: the silhouette and the parts of
 * its level and, for a logged-in player, a freshly started timed attempt.
 */
@Controller
public class PageController {

    private static final int LEADERBOARD_SIZE = 50;

    private final LevelService levels;
    private final PlayerService players;
    private final AttemptService attempts;
    private final AuthenticationManager authentication;
    private final SecurityContextRepository contexts;
    private final JsonMapper json;
    private final Svg svg;

    public PageController(LevelService levels, PlayerService players, AttemptService attempts,
                          AuthenticationManager authentication, SecurityContextRepository contexts,
                          JsonMapper json, Svg svg) {
        this.levels = levels;
        this.players = players;
        this.attempts = attempts;
        this.authentication = authentication;
        this.contexts = contexts;
        this.json = json;
        this.svg = svg;
    }

    @ModelAttribute
    public void common(@AuthenticationPrincipal PlayerPrincipal me, Model model) {
        model.addAttribute("me", me);
        model.addAttribute("svg", svg);
        model.addAttribute("levels", levels.all());
    }

    @GetMapping("/")
    public String levels(@AuthenticationPrincipal PlayerPrincipal me, Model model) {
        List<Attempt> bests = me == null ? List.of()
                : players.find(me.getId()).map(Player::bests).orElse(null);
        model.addAttribute("bests", bests == null ? Map.of()
                : bests.stream().collect(Collectors.toMap(Attempt::levelId, Function.identity())));
        return "levels";
    }

    @GetMapping("/play/{id}")
    public String play(@PathVariable String id, @AuthenticationPrincipal PlayerPrincipal me, Model model,
                       HttpServletResponse response) {
        Optional<Level> level = select(id, model, response);
        if (level.isEmpty()) {
            return "not-found";
        }
        model.addAttribute("levelJson", json.writeValueAsString(
                Map.of("silhouette", level.get().silhouette(), "pieces", level.get().pieces())));
        model.addAttribute("attemptId", me == null ? null : attempts.start(level.get(), me).id());
        return "play";
    }

    @GetMapping("/leaderboard")
    public String leaderboard(@RequestParam(name = "level", required = false) String id, Model model,
                              HttpServletResponse response) {
        Optional<Level> level = select(id, model, response);
        level.ifPresent(found -> model.addAttribute("rows", players.leaderboard(found.id(), LEADERBOARD_SIZE)));
        return level.isPresent() ? "leaderboard" : "not-found";
    }

    @GetMapping("/login")
    public String login(@RequestParam(defaultValue = "login") String mode, Model model) {
        model.addAttribute("register", "register".equals(mode));
        model.addAttribute("form", new RegisterForm("", ""));
        return "login";
    }

    /** Registers a player from the form and logs them in, or shows the form again with errors. */
    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("form") RegisterForm form, BindingResult errors, Model model,
                           HttpServletRequest request, HttpServletResponse response) {
        model.addAttribute("register", true);
        if (errors.hasErrors()) {
            return "login";
        }
        try {
            players.register(form);
        } catch (ErrorResponseException e) {
            model.addAttribute("formError", Optional.ofNullable(e.getBody().getDetail()).orElse("Registration failed"));
            return "login";
        }
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(form.username(), form.password())));
        if (request.getSession(false) != null) {
            request.changeSessionId(); // a fresh session id on login, against session fixation
        }
        SecurityContextHolder.setContext(context);
        contexts.saveContext(context, request, response);
        return "redirect:/";
    }

    /**
     * Puts the level (the first one when no id is given), its position and
     * the next level into the model, or answers 404.
     */
    private Optional<Level> select(String id, Model model, HttpServletResponse response) {
        List<Level> all = levels.all();
        for (int i = 0; i < all.size(); i++) {
            if (id == null || all.get(i).id().equals(id)) {
                model.addAttribute("level", all.get(i));
                model.addAttribute("index", i);
                model.addAttribute("next", all.get((i + 1) % all.size()));
                return Optional.of(all.get(i));
            }
        }
        response.setStatus(HttpStatus.NOT_FOUND.value());
        return Optional.empty();
    }
}
