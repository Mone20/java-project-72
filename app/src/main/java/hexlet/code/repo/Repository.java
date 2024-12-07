package hexlet.code.repo;

import lombok.RequiredArgsConstructor;

import javax.sql.DataSource;

@RequiredArgsConstructor
public class Repository {
    protected final DataSource dataSource;

}
