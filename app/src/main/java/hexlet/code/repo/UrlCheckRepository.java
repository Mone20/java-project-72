package hexlet.code.repo;

import hexlet.code.model.UrlCheck;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class UrlCheckRepository extends Repository {
    public UrlCheckRepository(DataSource dataSource) {
        super(dataSource);
    }

    public void save(UrlCheck check) throws SQLException {
        var sql = "INSERT INTO url_checks (url_id, status_code, h1, title, description, created_at)"
                + "VALUES (?, ?, ?, ?, ?, ?)";
        var datetime = new Timestamp(System.currentTimeMillis());
        try (var conn = dataSource.getConnection();
             var preparedStatement = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preparedStatement.setLong(1, check.getUrlId());
            preparedStatement.setInt(2, check.getStatusCode());
            preparedStatement.setString(3, check.getH1());
            preparedStatement.setString(4, check.getTitle());
            preparedStatement.setString(5, check.getDescription());
            preparedStatement.setTimestamp(6, datetime);
            preparedStatement.executeUpdate();
            var generatedKeys = preparedStatement.getGeneratedKeys();
            if (generatedKeys.next()) {
                check.setId(generatedKeys.getLong(1));
                check.setCreatedAt(datetime);
            } else {
                throw new SQLException("DB have not returned an id after saving an entity");
            }
        }
    }

    public List<UrlCheck> findByUrlId(long urlId) throws SQLException {
        var sql = "SELECT * FROM url_checks WHERE url_id = ? ORDER BY id DESC";
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, urlId);
            var resultSet = stmt.executeQuery();
            var result = new ArrayList<UrlCheck>();
            while (resultSet.next()) {
                result.add(fillCheckEntity(resultSet));
            }
            return result;
        }
    }

    public Map<Long, UrlCheck> findLatestChecks() throws SQLException {
        var sql = "SELECT DISTINCT ON (url_id) * from url_checks order by url_id DESC, id DESC";
        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            var resultSet = stmt.executeQuery();
            var result = new HashMap<Long, UrlCheck>();
            while (resultSet.next()) {
                var check = fillCheckEntity(resultSet);
                var urlId = resultSet.getLong("url_id");
                check.setUrlId(urlId);
                result.put(urlId, check);
            }
            return result;
        }
    }

    private UrlCheck fillCheckEntity(ResultSet resultSet) throws SQLException {
        var id = resultSet.getLong("id");
        var statusCode = resultSet.getInt("status_code");
        var title = resultSet.getString("title");
        var h1 = resultSet.getString("h1");
        var description = resultSet.getString("description");
        var createdAt = resultSet.getTimestamp("created_at");
        var check = new UrlCheck(statusCode, title, h1, description);
        check.setId(id);
        check.setCreatedAt(createdAt);
        return check;
    }
}
