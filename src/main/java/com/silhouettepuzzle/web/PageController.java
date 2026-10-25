package com.silhouettepuzzle.web;

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

import com.silhouettepuzzle.attempt.LeaderboardService;
import com.silhouettepuzzle.level.Level;
import com.silhouettepuzzle.level.LevelRepository;
import com.silhouettepuzzle.player.Player;
import com.silhouettepuzzle.player.PlayerBest;
import com.silhouettepuzzle.player.PlayerPrincipal;
import com.silhouettepuzzle.player.PlayerRepository;
import com.silhouettepuzzle.player.PlayerService;
import com.silhouettepuzzle.player.RegisterRequest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

/**
 * The HTML pages. Levels, leaderboards and the login page are rendered on
 * the server; the game itself runs in the browser against the REST API.
 */
@Controller
public class PageController {

    private final LevelRepository levels;
    private final PlayerRepository players;
    private final LeaderboardService leaderboards;
    private final PlayerService registration;
    private final AuthenticationManager authentication;
    private final SecurityContextRepository contexts;
    private final Svg svg;

    public PageController(LevelRepository levels, PlayerRepository players, LeaderboardService leaderboards,
                          PlayerService registration, AuthenticationManager authentication,
                          SecurityContextRepository contexts, Svg svg) {
        this.levels = levels;
        this.players = players;
        this.leaderboards = leaderboards;
        this.registration = registration;
        this.authentication = authentication;
        this.contexts = contexts;
        this.svg = svg;
    }

    @ModelAttribute
    public void common(@AuthenticationPrincipal PlayerPrincipal me, Model model) {
        model.addAttribute("me", me);
        model.addAttribute("svg", svg);
        model.addAttribute("levels", levels.findAllByOrderByNumberAsc());
    }

    @GetMapping("/")
    public String levels(@AuthenticationPrincipal PlayerPrincipal me, Model model) {
        Map<String, PlayerBest> bests = me == null ? Map.of()
                : players.findById(me.getId()).map(Player::bestsOrEmpty).orElse(List.of()).stream()
                .collect(Collectors.toMap(PlayerBest::levelId, Function.identity()));
        model.addAttribute("bests", bests);
        return "levels";
    }

    @GetMapping("/play/{id}")
    public String play(@PathVariable String id, Model model, HttpServletResponse response) {
        return select(id, model, response).isPresent() ? "play" : "not-found";
    }

    @GetMapping("/leaderboard")
    public String leaderboard(@RequestParam(name = "level", required = false) String id, Model model,
                              HttpServletResponse response) {
        Optional<Level> level = select(id, model, response);
        level.ifPresent(found -> model.addAttribute("rows", leaderboards.leaderboard(found.id(), 50)));
        return level.isPresent() ? "leaderboard" : "not-found";
    }

    @GetMapping("/login")
    public String login(@RequestParam(defaultValue = "login") String mode, Model model) {
        model.addAttribute("register", "register".equals(mode));
        model.addAttribute("form", new RegisterRequest("", ""));
        return "login";
    }

    /** Registers a player from the form and logs them in, or shows the form again with errors. */
    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("form") RegisterRequest form, BindingResult errors, Model model,
                           HttpServletRequest request, HttpServletResponse response) {
        model.addAttribute("register", true);
        if (errors.hasErrors()) {
            return "login";
        }
        try {
            registration.register(form);
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
        List<Level> all = levels.findAllByOrderByNumberAsc();
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
