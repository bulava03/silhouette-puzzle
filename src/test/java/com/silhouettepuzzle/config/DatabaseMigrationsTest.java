package com.silhouettepuzzle.config;

import static com.mongodb.client.model.Filters.eq;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.mongodb.autoconfigure.MongoConnectionDetails;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Updates;
import com.silhouettepuzzle.IntegrationTestConfig;

/**
 * The Liquibase changelog, run on a database holding data in the old shape.
 */
@SpringBootTest
@Import(IntegrationTestConfig.class)
class DatabaseMigrationsTest {

    private static final Date T1 = Date.from(Instant.parse("2026-10-20T10:00:00Z"));
    private static final Date T2 = Date.from(Instant.parse("2026-10-21T10:00:00Z"));
    private static final String CHANGELOG_TABLE = "DATABASECHANGELOG";

    @Autowired
    private MongoTemplate mongo;

    @Autowired
    private MongoClient client;

    @Autowired
    private MongoConnectionDetails connection;

    private MongoDatabase old;
    private String oldUrl;

    @BeforeEach
    void oldDatabase() {
        old = client.getDatabase("migration-test");
        old.drop();
        oldUrl = "mongodb://" + String.join(",", connection.getConnectionString().getHosts()) + "/migration-test";
    }

    @Test
    void theApplicationDatabaseIsMigratedOnStartup() {
        assertThat(mongo.getDb().getCollection(CHANGELOG_TABLE).find())
                .extracting(entry -> entry.getString("id"))
                .containsExactly("001-player-indexes", "002-move-results-into-players", "002-drop-attempt-indexes",
                        "003-expire-old-attempts");
        assertThat(mongo.getDb().getCollection("players").listIndexes())
                .anySatisfy(index -> {
                    assertThat(index.getString("name")).isEqualTo("usernameKey");
                    assertThat(index.getBoolean("unique")).isTrue();
                });
    }

    @Test
    void movesSolvedAttemptsIntoThePlayersBestsAndCleansUp() throws Exception {
        ObjectId alice = new ObjectId();
        ObjectId bob = new ObjectId();
        old.getCollection("players").insertMany(List.of(
                new Document("_id", alice).append("username", "alice").append("usernameKey", "alice"),
                new Document("_id", bob).append("username", "bob").append("usernameKey", "bob")
                        .append("bests", List.of(best("house", 9_000, 4, T1, 1)))));
        old.getCollection("attempts").insertMany(List.of(
                solved(alice, "bus", 30_000, 20, T1),
                solved(alice, "bus", 25_000, 30, T2),
                solved(alice, "house", 40_000, 9, T1),
                solved(null, "bus", 1_000, 1, T1),                           // anonymous
                new Document("levelId", "bus").append("playerId", alice.toHexString())
                        .append("username", "alice").append("startedAt", T1), // never solved
                solved(bob, "bus", 10_000, 5, T1)));                          // bob already has bests
        old.getCollection("attempts").createIndex(new Document("levelId", 1).append("timeMs", 1),
                new IndexOptions().name("leaderboard"));

        DatabaseMigrations.update(oldUrl);

        assertThat(bestsOf(alice)).containsExactlyInAnyOrder(
                best("bus", 25_000, 30, T2, 2), best("house", 40_000, 9, T1, 1));
        assertThat(bestsOf(bob)).containsExactly(best("house", 9_000, 4, T1, 1));
        assertThat(old.getCollection("attempts").countDocuments(new Document("username", new Document("$exists", true))))
                .isZero();
        assertThat(old.getCollection("attempts").listIndexes())
                .extracting(index -> index.getString("name"))
                .containsExactlyInAnyOrder("_id_", "attempt-expiry");

        // Running again changes nothing.
        long applied = old.getCollection(CHANGELOG_TABLE).countDocuments();
        DatabaseMigrations.update(oldUrl);
        assertThat(old.getCollection(CHANGELOG_TABLE).countDocuments()).isEqualTo(applied);
    }

    @Test
    void refusesToStartWhenAnAppliedChangeSetWasEdited() throws Exception {
        DatabaseMigrations.update(oldUrl);
        old.getCollection(CHANGELOG_TABLE).updateOne(eq("id", "001-player-indexes"),
                Updates.set("md5sum", "9:0123456789abcdef0123456789abcdef"));

        assertThatThrownBy(() -> DatabaseMigrations.update(oldUrl))
                .hasStackTraceContaining("001-player-indexes");
    }

    private List<Document> bestsOf(ObjectId player) {
        return old.getCollection("players").find(eq("_id", player)).first().getList("bests", Document.class);
    }

    private static Document solved(ObjectId player, String levelId, long timeMs, int moves, Date at) {
        return new Document("levelId", levelId)
                .append("playerId", player == null ? null : player.toHexString())
                .append("username", player == null ? null : "someone")
                .append("startedAt", at).append("solvedAt", at).append("timeMs", timeMs).append("moves", moves);
    }

    private static Document best(String levelId, long timeMs, int moves, Date at, int solves) {
        return new Document("levelId", levelId).append("timeMs", timeMs).append("moves", moves)
                .append("solvedAt", at).append("solves", solves);
    }
}
