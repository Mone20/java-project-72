package hexlet.code;


import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.resolve.ResourceCodeResolver;
import hexlet.code.controllers.CheckController;
import hexlet.code.controllers.RootController;
import hexlet.code.controllers.UrlsController;
import hexlet.code.model.web.Routes;
import hexlet.code.repo.UrlCheckRepository;
import hexlet.code.repo.UrlRepository;
import io.javalin.Javalin;
import io.javalin.rendering.template.JavalinJte;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.stream.Collectors;

@Slf4j
public class App {
    private static int getPort() {
        String port = System.getenv().getOrDefault("PORT", "7070");
        return Integer.parseInt(port);
    }

    private static String readResourceFile(String fileName) throws IOException {
        var inputStream = App.class.getClassLoader().getResourceAsStream(fileName);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    public static void main(String[] args) throws IOException, SQLException {
        var app = getApp();

        app.start(getPort());
    }

    private static String getDatabaseUrl() {
        return System.getenv().getOrDefault("JDBC_DATABASE_URL", "jdbc:h2:mem:project");
    }

    private static boolean isDropDbEnabled() {
        return Boolean.parseBoolean(System.getenv().getOrDefault("IS_DROP_DB_ENABLED", "false"));
    }


    public static Javalin getApp() throws IOException, SQLException {

        var hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(getDatabaseUrl());

        var dataSource = new HikariDataSource(hikariConfig);
        String schemaSql = readResourceFile("schema.sql");

        log.info(schemaSql);
        try (var connection = dataSource.getConnection();
             var statement = connection.createStatement()) {
            if (isDropDbEnabled()) {
                String dropSql = readResourceFile("drop.sql");
                statement.execute(dropSql);
            }
            statement.execute(schemaSql);
        }


        var app = Javalin.create(config -> config.fileRenderer(new JavalinJte(createTemplateEngine())));

        app.before(ctx -> ctx.contentType("text/html; charset=utf-8"));

        UrlRepository urlRepository = new UrlRepository(dataSource);
        UrlCheckRepository urlCheckRepository = new UrlCheckRepository(dataSource);


        UrlsController urlsController = new UrlsController(urlRepository, urlCheckRepository);
        CheckController checkController = new CheckController(urlRepository, urlCheckRepository);
        RootController rootController = new RootController();

        app.get(Routes.rootPath(), rootController::welcome);
        app.get(Routes.urlsPath(), urlsController::listUrls);
        app.post(Routes.urlsPath(), urlsController::createUrl);
        app.get(Routes.urlPath("{id}"), urlsController::showUrl);
        app.post(Routes.urlChecksPath("{id}"), checkController::checkUrl);

        return app;

    }

    private static TemplateEngine createTemplateEngine() {
        ClassLoader classLoader = App.class.getClassLoader();
        ResourceCodeResolver codeResolver = new ResourceCodeResolver("templates", classLoader);
        return TemplateEngine.create(codeResolver, ContentType.Html);
    }
}
