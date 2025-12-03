//package com.example.sharing.config;
//
//import org.hibernate.dialect.DatabaseVersion;
//import org.hibernate.dialect.Dialect;
//import org.hibernate.dialect.identity.IdentityColumnSupport;
//import org.hibernate.dialect.pagination.LimitHandler;
//import org.hibernate.dialect.pagination.LimitLimitHandler;
//
//import java.sql.Types;
//
//public class SQLiteDialect6 extends Dialect {
//
//    public SQLiteDialect6() {
//        // SQLite 大概是 3.x 系列
//        super(DatabaseVersion.make(3));
//    }
//
//    // 类型映射：可以按需精简或扩展
//    @Override
//    protected String columnType(int sqlTypeCode) {
//        return switch (sqlTypeCode) {
//            case Types.BIGINT, Types.INTEGER -> "integer";
//            case Types.VARCHAR -> "varchar";
//            case Types.BOOLEAN -> "boolean";
//            case Types.DATE -> "date";
//            case Types.TIMESTAMP -> "datetime";
//            default -> super.columnType(sqlTypeCode);
//        };
//    }
//
//    // 主键自增支持：沿用你自己的实现
//    @Override
//    public IdentityColumnSupport getIdentityColumnSupport() {
//        return new SQLiteIdentityColumnSupport();
//    }
//
//    // 关键：告诉 Hibernate 使用 LIMIT / OFFSET 语法分页
//    @Override
//    public LimitHandler getLimitHandler() {
//        // Hibernate 6 内置的一个 "LIMIT/LIMIT+OFFSET" 处理器
//        return LimitLimitHandler.INSTANCE;
//    }
//}