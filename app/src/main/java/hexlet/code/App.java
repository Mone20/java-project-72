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

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Driver;
import java.sql.DriverManager;
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
        var hikariConfig = getHikariConfig();
        var dataSource = new HikariDataSource(hikariConfig);
        var app = getApp(dataSource);

        app.start(getPort());
    }

    private static String getDatabaseUrl() {
        return System.getenv().getOrDefault("JDBC_DATABASE_URL", "jdbc:postgresql://localhost:5432/urlchecker");
    }

    private static String getDriverName() {
        Driver driver;
        try {
            driver = DriverManager.getDriver(getDatabaseUrl());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return driver.getClass().getName();
    }

    private static boolean isDropDbEnabled() {
        return Boolean.parseBoolean(System.getenv().getOrDefault("IS_DROP_DB_ENABLED", "false"));
    }

    private static HikariConfig getHikariConfig() {
        var hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(getDatabaseUrl());
        String driverClassName = getDriverName();
        switch (driverClassName) {
            case "org.postgresql.Driver":
                hikariConfig.setUsername(System.getenv().getOrDefault("DB_USER", "admin"));
                hikariConfig.setPassword(System.getenv().getOrDefault("DB_PASSWORD", "admin"));
                hikariConfig.setDriverClassName(driverClassName);
                break;
            default:
                hikariConfig.setDriverClassName(driverClassName);
        }
        return hikariConfig;
    }

    private static void executeSchemaScript(DataSource dataSource) throws IOException, SQLException {
        String schemaSql;
        switch (getDriverName()) {
            case "org.postgresql.Driver":
                schemaSql = readResourceFile("postgresql/schema.sql");
                break;
            default:
                schemaSql = readResourceFile("h2/schema.sql");
                break;
        }

        log.info(schemaSql);
        try (var connection = dataSource.getConnection();
             var statement = connection.createStatement()) {
            if (isDropDbEnabled()) {
                String dropSql = readResourceFile("drop.sql");
                statement.execute(dropSql);
            }
            statement.execute(schemaSql);
        }
    }


    public static Javalin getApp(DataSource dataSource) throws IOException, SQLException {


        executeSchemaScript(dataSource);

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
