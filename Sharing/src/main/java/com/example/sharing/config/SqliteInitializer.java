//package com.example.sharing.config;
//
//import jakarta.annotation.PostConstruct;
//import org.springframework.jdbc.core.JdbcTemplate;
//import org.springframework.stereotype.Component;
//
//@Component
//public class SqliteInitializer {
//
//    private final JdbcTemplate jdbcTemplate;
//
//    public SqliteInitializer(JdbcTemplate jdbcTemplate) {
//        this.jdbcTemplate = jdbcTemplate;
//    }
//
//    @PostConstruct
//    public void init() {
//        // 启动时执行一次，建表（不存在才建）
//        jdbcTemplate.execute("""
//            create table if not exists users (
//                id integer primary key autoincrement,
//                username varchar(50) not null unique,
//                email varchar(100) not null unique,
//                password varchar not null,
//                role integer not null
//            )
//        """);
//    }
//}