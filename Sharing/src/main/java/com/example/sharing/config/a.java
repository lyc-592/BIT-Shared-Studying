//package com.example.sharing.config;
//
//import org.springframework.data.relational.core.dialect.ArrayColumns;
//import org.springframework.data.relational.core.dialect.Dialect;
//import org.springframework.data.relational.core.dialect.LimitClause;
//import org.springframework.data.relational.core.dialect.LockClause;
//import org.springframework.data.relational.core.dialect.SelectContext;
//
///**
// * 极简 SQLite 方言：
// * 只覆盖 LIMIT / LIMIT ... OFFSET，防止生成 FETCH FIRST ... 语法。
// */
//public class SqliteDialect implements Dialect {
//
//    // 使用 SQLite 的 LIMIT 语法
//    @Override
//    public LimitClause limit() {
//        return new LimitClause() {
//            @Override
//            public String getLimit(long limit) {
//                // 生成 "... LIMIT 10"
//                return "LIMIT " + limit;
//            }
//
//            @Override
//            public String getLimitOffset(long limit, long offset) {
//                // 生成 "... LIMIT 10 OFFSET 20"
//                return "LIMIT " + limit + " OFFSET " + offset;
//            }
//        };
//    }
//
//    // 不支持行级锁，返回 noLock
//    @Override
//    public LockClause lock() {
//        return LockClause.noLock();
//    }
//
//    // 不支持数组列
//    @Override
//    public ArrayColumns getArraySupport() {
//        return ArrayColumns.unsupported();
//    }
//
//    // 你这个版本的 Dialect 要求实现 getSelectContext()
//    // 这里直接用默认的 SelectContext（不做特殊处理）
//    @Override
//    public SelectContext getSelectContext() {
//        return SelectContext.DEFAULT;
//    }
//}