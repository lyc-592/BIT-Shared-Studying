//package com.example.sharing.config;
//
//import org.hibernate.dialect.DatabaseVersion;
//import org.hibernate.dialect.Dialect;
//import org.hibernate.dialect.identity.IdentityColumnSupport;
//import org.hibernate.dialect.unique.CreateTableUniqueDelegate;
//import org.hibernate.dialect.unique.UniqueDelegate;
//
//import java.sql.Types;
//
//public class SQLiteDialect extends Dialect {
//
//    public SQLiteDialect() {
//        super(DatabaseVersion.make(3));
//    }
//
//    @Override
//    protected String columnType(int sqlTypeCode) {
//        return switch (sqlTypeCode) {
//            case Types.BIGINT -> "integer";   // Long -> integer
//            case Types.VARCHAR -> "varchar";
//            case Types.BOOLEAN -> "boolean";
//            case Types.DATE -> "date";
//            case Types.TIMESTAMP -> "datetime";
//            default -> super.columnType(sqlTypeCode);
//        };
//    }
//
//    @Override
//    public UniqueDelegate getUniqueDelegate() {
//        return new CreateTableUniqueDelegate(this);
//    }
//
//    @Override
//    public boolean supportsCascadeDelete() {
//        return true;
//    }
//
//    @Override
//    public boolean supportsOuterJoinForUpdate() {
//        return false;
//    }
//
//    @Override
//    public IdentityColumnSupport getIdentityColumnSupport() {
//        // ★ 再次启用我们自己的 Identity 支持
//        return new SQLiteIdentityColumnSupport();
//    }
//}