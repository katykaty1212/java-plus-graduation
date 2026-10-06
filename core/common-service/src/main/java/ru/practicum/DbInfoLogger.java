package ru.practicum;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

@Slf4j
@Component
public class DbInfoLogger implements ApplicationRunner {

    private final DataSource dataSource;

    public DbInfoLogger(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (Connection c = dataSource.getConnection()) {
            log.info("### DB-INFO ### URL={} SCHEMA={} USER={} ###",
                    c.getMetaData().getURL(),
                    c.getSchema(),
                    c.getMetaData().getUserName());
        }
    }
}