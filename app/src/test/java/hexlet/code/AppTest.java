package hexlet.code;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import hexlet.code.model.Url;
import hexlet.code.repo.UrlCheckRepository;
import hexlet.code.repo.UrlRepository;
import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;


import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.extension.ExtendWith;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

@ExtendWith(SystemStubsExtension.class)
class AppTest {

    private static Javalin app;
    private static DataSource dataSource;
    private static MockWebServer mockServer;
    private static UrlRepository urlRepository;
    private static UrlCheckRepository urlCheckRepository;
    private static final String JDBC_URL = "jdbc:h2:mem:project";

    private static DataSource getDataSource() {
        var hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(JDBC_URL);
        return new HikariDataSource(hikariConfig);
    }

    private static Path getFixturePath(String fileName) {
        return Paths.get("src", "test", "resources", "fixtures", fileName)
                .toAbsolutePath().normalize();
    }

    private static String readFixture(String fileName) throws IOException {
        Path filePath = getFixturePath(fileName);
        return Files.readString(filePath).trim();
    }

    @BeforeAll
    public static void beforeAll(EnvironmentVariables env) throws IOException {
        env.set("IS_DROP_DB_ENABLED", "true");
        env.set("JDBC_DATABASE_URL", JDBC_URL);
        mockServer = new MockWebServer();
        MockResponse mockedResponse = new MockResponse()
                .setBody(readFixture("index.html"));
        mockServer.enqueue(mockedResponse);
        mockServer.start();
        dataSource = getDataSource();
        urlRepository = new UrlRepository(dataSource);
        urlCheckRepository = new UrlCheckRepository(dataSource);
    }

    @AfterAll
    public static void afterAll() throws IOException {
        mockServer.shutdown();
    }

    @BeforeEach
    void setUp() throws SQLException, IOException {
        app = App.getApp(dataSource);
    }


    @Nested
    class RootTest {

        @Test
        void testIndex() {
            JavalinTest.test(app, (server, client) -> {
                assertThat(client.get("/").code()).isEqualTo(200);
            });
        }
    }

    @Nested
    class UrlTest {

        @Test
        void testIndex() {
            JavalinTest.test(app, (server, client) -> {
                assertThat(client.get("/urls").code()).isEqualTo(200);
            });
        }

        @Test
        void testShow() throws SQLException {
            var url = new Url("http://test.io");
            urlRepository.save(url);
            JavalinTest.test(app, (server, client) -> {
                var responce = client.get("/urls/" + url.getId());
                assertThat(responce.code()).isEqualTo(200);
            });
        }

        @Test
        void testStore() throws SQLException {
            String inputUrl = "https://ru.hexlet.io";

            JavalinTest.test(app, (server, client) -> {
                var requestBody = "url=" + inputUrl;
                var response = client.post("/urls", requestBody);
                assertThat(response.code()).isEqualTo(200);
            });

            Url actualUrl = urlRepository.findByName(inputUrl).orElse(null);

            assertThat(actualUrl).isNotNull();
            assertThat(actualUrl.getName()).isEqualTo(inputUrl);
        }
    }

    @Nested
    class UrlCheckTest {

        @Test
        void testStore() {
            String url = mockServer.url("/").toString().replaceAll("/$", "");

            JavalinTest.test(app, (server, client) -> {
                var requestBody = "url=" + url;
                assertThat(client.post("/urls", requestBody).code()).isEqualTo(200);

                var actualUrl = urlRepository.findByName(url).orElse(null);


                assertThat(actualUrl).isNotNull();
                assertThat(actualUrl.getName()).isEqualTo(url);

                client.post("/urls/" + actualUrl.getId() + "/checks");

                var responce = client.get("/urls/" + actualUrl.getId());
                assertThat(responce.code()).isEqualTo(200);
                assertThat(responce.body().string()).contains(url);

                var actualCheckUrl = urlCheckRepository
                        .findLatestChecks().get(actualUrl.getId());

                assertThat(actualCheckUrl).isNotNull();
                assertThat(actualCheckUrl.getStatusCode()).isEqualTo(200);
                assertThat(actualCheckUrl.getTitle()).isEqualTo("Test page");
                assertThat(actualCheckUrl.getH1()).isEqualTo("Do not expect a miracle, miracles yourself!");
                assertThat(actualCheckUrl.getDescription()).contains("statements of great people");
            });
        }
    }
}
