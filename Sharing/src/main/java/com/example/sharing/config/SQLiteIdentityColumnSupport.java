//package com.example.sharing.config;
//
//import org.hibernate.dialect.identity.IdentityColumnSupportImpl;
//
//public class SQLiteIdentityColumnSupport extends IdentityColumnSupportImpl {
//
//    @Override
//    public boolean supportsIdentityColumns() {
//        return true;
//    }
//
//    @Override
//    public String getIdentityColumnString(int type) {
//        return "primary key autoincrement";
//    }
//
//    @Override
//    public String getIdentitySelectString(String table, String column, int type) {
//        return "select last_insert_rowid()";
//    }
//}