package com.silhouettepuzzle.web;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
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

import com.silhouettepuzzle.attempt.LeaderboardEntry;
import com.silhouettepuzzle.attempt.LeaderboardService;
import com.silhouettepuzzle.attempt.ProgressEntry;
import com.silhouettepuzzle.level.Level;
import com.silhouettepuzzle.level.LevelRepository;
import com.silhouettepuzzle.level.LevelSummary;
import com.silhouettepuzzle.player.PlayerPrincipal;
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

    /** Edge colours of the levels, in order; they repeat after the last. */
    private static final List<String> ACCENTS = List.of("#6BD8DE", "#EE8DBD", "#E4AC59");

    /** How many places the leaderboard page shows. */
    private static final int LEADERBOARD_SIZE = 50;

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
            .withZone(ZoneOffset.UTC);

    private final LevelRepository levels;
    private final LeaderboardService leaderboards;
    private final PlayerService players;
    private final AuthenticationManager authentication;
    private final SecurityContextRepository contexts;

    /**
     * Creates the controller.
     *
     * @param levels         level storage
     * @param leaderboards   results queries
     * @param players        registration
     * @param authentication checks a new player's credentials to log them in
     * @param contexts       keeps the logged-in player in the session
     */
    public PageController(LevelRepository levels, LeaderboardService leaderboards, PlayerService players,
                          AuthenticationManager authentication, SecurityContextRepository contexts) {
        this.levels = levels;
        this.leaderboards = leaderboards;
        this.players = players;
        this.authentication = authentication;
        this.contexts = contexts;
    }

    /**
     * Makes the logged-in player, if any, available to every page.
     *
     * @param me the logged-in player, or null
     * @return the player for the {@code me} model attribute
     */
    @ModelAttribute("me")
    public PlayerPrincipal me(@AuthenticationPrincipal PlayerPrincipal me) {
        return me;
    }

    /**
     * The levels page: every picture with the player's bests.
     *
     * @param me    the logged-in player, or null
     * @param model view model
     * @return the template
     */
    @GetMapping("/")
    public String levels(@AuthenticationPrincipal PlayerPrincipal me, Model model) {
        Map<String, ProgressEntry> progress = me == null ? Map.of()
                : leaderboards.progressOf(me.getId()).stream()
                .collect(Collectors.toMap(ProgressEntry::levelId, Function.identity()));
        List<Level> all = levels.findAll().stream().sorted((a, b) -> Integer.compare(a.number(), b.number())).toList();
        List<LevelCard> cards = new ArrayList<>(all.size());
        for (int i = 0; i < all.size(); i++) {
            Level level = all.get(i);
            ProgressEntry best = progress.get(level.id());
            cards.add(new LevelCard(level.id(), "%02d".formatted(level.number()), level.name(), level.pieces().size(),
                    accent(i), Shapes.viewBox(level.silhouette(), 0.5),
                    level.silhouette().stream().map(Shapes::points).toList(), best != null,
                    best == null ? null : Times.clock(best.bestTimeMs()),
                    best == null ? null : best.moves()));
        }
        model.addAttribute("levels", cards);
        model.addAttribute("solvedCount", progress.size());
        return "levels";
    }

    /**
     * The game page for one level. The page script loads the level and
     * talks to the attempt API.
     *
     * @param id       the level
     * @param model    view model
     * @param response to answer 404 for an unknown level
     * @return the template
     */
    @GetMapping("/play/{id}")
    public String play(@PathVariable String id, Model model, HttpServletResponse response) {
        List<LevelSummary> order = levels.findAllByOrderByNumberAsc();
        int index = indexOf(order, id);
        if (index < 0) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return "not-found";
        }
        LevelSummary level = order.get(index);
        model.addAttribute("level", level);
        model.addAttribute("accent", accent(index));
        model.addAttribute("nextId", order.get((index + 1) % order.size()).id());
        model.addAttribute("tabs", tabs(order, id));
        return "play";
    }

    /**
     * The leaderboard of one level, the first one by default.
     *
     * @param levelId the level, or null for the first
     * @param me      the logged-in player, or null
     * @param model   view model
     * @param response to answer 404 for an unknown level
     * @return the template
     */
    @GetMapping("/leaderboard")
    public String leaderboard(@RequestParam(name = "level", required = false) String levelId,
                              @AuthenticationPrincipal PlayerPrincipal me, Model model,
                              HttpServletResponse response) {
        List<LevelSummary> order = levels.findAllByOrderByNumberAsc();
        String id = levelId != null ? levelId : order.isEmpty() ? null : order.getFirst().id();
        int index = id == null ? -1 : indexOf(order, id);
        if (index < 0) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return "not-found";
        }
        String myName = me == null ? null : me.getUsername();
        List<ResultRow> rows = leaderboards.leaderboard(id, LEADERBOARD_SIZE).stream()
                .map(entry -> row(entry, myName)).toList();
        model.addAttribute("level", order.get(index));
        model.addAttribute("tabs", tabs(order, id));
        model.addAttribute("rows", rows);
        model.addAttribute("podium", rows.subList(0, Math.min(3, rows.size())));
        return "leaderboard";
    }

    /**
     * The login and registration page.
     *
     * @param mode  {@code register} to open the registration form
     * @param model view model
     * @return the template
     */
    @GetMapping("/login")
    public String login(@RequestParam(defaultValue = "login") String mode, Model model) {
        model.addAttribute("register", "register".equals(mode));
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new RegisterRequest("", ""));
        }
        return loginPage(model);
    }

    /**
     * Registers a player from the form and logs them in.
     *
     * @param form     username and password
     * @param errors   validation errors of the form
     * @param model    view model
     * @param request  the request, to store the new login in the session
     * @param response the response, to store the new login in the session
     * @return a redirect to the levels, or the form again with errors
     */
    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("form") RegisterRequest form, BindingResult errors, Model model,
                           HttpServletRequest request, HttpServletResponse response) {
        model.addAttribute("register", true);
        if (errors.hasErrors()) {
            return loginPage(model);
        }
        try {
            players.register(form);
        } catch (ErrorResponseException e) {
            model.addAttribute("formError", Optional.ofNullable(e.getBody().getDetail()).orElse("Registration failed"));
            return loginPage(model);
        }
        logIn(form, request, response);
        return "redirect:/";
    }

    /**
     * Fills the login page's decoration, the first picture already built.
     *
     * @param model view model
     * @return the template
     */
    private String loginPage(Model model) {
        model.addAttribute("art", levels.findAll().stream()
                .min((a, b) -> Integer.compare(a.number(), b.number()))
                .map(Artwork::of)
                .orElse(null));
        return "login";
    }

    private void logIn(RegisterRequest form, HttpServletRequest request, HttpServletResponse response) {
        Authentication result = authentication.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(form.username(), form.password()));
        // A fresh session id on login, as the login form gets, against session fixation.
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(result);
        SecurityContextHolder.setContext(context);
        contexts.saveContext(context, request, response);
    }

    private static ResultRow row(LeaderboardEntry entry, String myName) {
        return new ResultRow(entry.rank(), entry.username(), Times.clock(entry.timeMs()), entry.moves(),
                DAY.format(entry.solvedAt()), entry.username().equals(myName));
    }

    private static List<LevelTab> tabs(List<LevelSummary> order, String currentId) {
        List<LevelTab> tabs = new ArrayList<>(order.size());
        for (int i = 0; i < order.size(); i++) {
            LevelSummary level = order.get(i);
            tabs.add(new LevelTab(level.id(), "%02d %s".formatted(level.number(), level.name()), accent(i),
                    level.id().equals(currentId)));
        }
        return tabs;
    }

    private static int indexOf(List<LevelSummary> order, String id) {
        for (int i = 0; i < order.size(); i++) {
            if (order.get(i).id().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    private static String accent(int index) {
        return ACCENTS.get(index % ACCENTS.size());
    }
}
