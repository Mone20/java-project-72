package hexlet.code.repo;

import hexlet.code.model.Url;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


public final class UrlRepository extends Repository {

    public UrlRepository(DataSource dataSource) {
        super(dataSource);
    }

    public void save(Url url) throws SQLException {
        var sql = "INSERT INTO urls (name, created_at) VALUES (?, ?)";
        var datetime = new Timestamp(System.currentTimeMillis());
        try (var conn = dataSource.getConnection();
             var preparedStatement = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preparedStatement.setString(1, url.getName());
            preparedStatement.setTimestamp(2, datetime);
            preparedStatement.executeUpdate();
            var generatedKeys = preparedStatement.getGeneratedKeys();
            if (generatedKeys.next()) {
                url.setId(generatedKeys.getLong(1));
                url.setCreatedAt(datetime);
            } else {
                throw new SQLException("DB have not returned an id after saving an entity");
            }
        }
    }

    private Optional<Url> find(String key, Object value) throws SQLException {
        var sql = String.format("SELECT * FROM urls WHERE %s = ?", key);
        try (var conn = dataSource.getConnection(); var stmt = conn.prepareStatement(sql)) {
            if (value instanceof Long) {
                stmt.setLong(1, (Long) value);
            }
            if (value instanceof String) {
                stmt.setString(1, (String) value);
            }
            var resultSet = stmt.executeQuery();
            if (resultSet.next()) {
                return Optional.of(fillUrlEntity(resultSet));
            }
            return Optional.empty();
        }
    }

    public Optional<Url> findByName(String urlName) throws SQLException {
        return find("name", urlName);
    }

    public Optional<Url> findById(Long id) throws SQLException {
        return find("id", id);
    }

    public List<Url> getEntities() throws SQLException {
        var sql = "SELECT * FROM urls ORDER BY id";
        try (var conn = dataSource.getConnection(); var stmt = conn.prepareStatement(sql)) {
            var resultSet = stmt.executeQuery();
            var result = new ArrayList<Url>();
            while (resultSet.next()) {
                result.add(fillUrlEntity(resultSet));
            }
            return result;
        }
    }

    private Url fillUrlEntity(ResultSet resultSet) throws SQLException {
        var id = resultSet.getLong("id");
        var name = resultSet.getString("name");
        var createdAt = resultSet.getTimestamp("created_at");
        var url = new Url(name);
        url.setId(id);
        url.setCreatedAt(createdAt);
        return url;
    }
}
