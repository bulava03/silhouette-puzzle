package com.silhouettepuzzle.config;

import java.util.Map;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.mongodb.autoconfigure.MongoConnectionDetails;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.stereotype.Component;

import liquibase.Scope;
import liquibase.analytics.configuration.AnalyticsArgs;
import liquibase.command.CommandScope;
import liquibase.command.core.UpdateCommandStep;
import liquibase.command.core.helpers.DbUrlConnectionArgumentsCommandStep;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.integration.spring.SpringResourceAccessor;

/**
 * Applies the Liquibase changelog in {@code db/changelog} to MongoDB on
 * startup, before the application serves anything. Liquibase records applied
 * change sets with checksums in {@code DATABASECHANGELOG} and holds a lock in
 * {@code DATABASECHANGELOGLOCK}, so several instances starting at once apply
 * each change once. Spring Boot's own Liquibase support needs a JDBC data
 * source, hence this small runner.
 */
@Component
public class DatabaseMigrations implements InitializingBean {

    static final String CHANGELOG = "db/changelog/changelog.yaml";

    private final MongoConnectionDetails connection;

    public DatabaseMigrations(MongoConnectionDetails connection) {
        this.connection = connection;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        update(connection.getConnectionString().getConnectionString());
    }

    /** Brings the database at the given MongoDB URL up to date with the changelog. */
    public static void update(String url) throws Exception {
        // Spring's resource loading, as Spring Boot's own Liquibase integration uses.
        SpringResourceAccessor resources = new SpringResourceAccessor(
                new DefaultResourceLoader(DatabaseMigrations.class.getClassLoader()));
        // Liquibase reports anonymous usage data by default; the application keeps to itself.
        Scope.child(Map.of(AnalyticsArgs.ENABLED.getKey(), false, Scope.Attr.resourceAccessor.name(), resources), () -> {
            try (Database database = DatabaseFactory.getInstance().openDatabase(url, null, null, null, resources)) {
                new CommandScope(UpdateCommandStep.COMMAND_NAME)
                        .addArgumentValue(DbUrlConnectionArgumentsCommandStep.DATABASE_ARG, database)
                        .addArgumentValue(UpdateCommandStep.CHANGELOG_FILE_ARG, CHANGELOG)
                        .execute();
            }
        });
    }
}
