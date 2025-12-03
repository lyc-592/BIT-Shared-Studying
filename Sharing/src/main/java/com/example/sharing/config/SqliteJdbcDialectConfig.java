//package com.example.sharing.config;
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//import org.springframework.data.relational.core.dialect.AnsiDialect;
//import org.springframework.data.relational.core.dialect.Dialect;
//
//@Configuration
//public class SqliteJdbcDialectConfig {
//
//    @Bean
//    public Dialect jdbcDialect() {
//        // 用最通用的 ANSI 方言，适合简单 CRUD，先让项目跑起来
//        return AnsiDialect.INSTANCE;
//    }
//}