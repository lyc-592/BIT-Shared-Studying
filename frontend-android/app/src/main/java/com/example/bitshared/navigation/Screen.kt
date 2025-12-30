package com.example.bitshared.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Courses : Screen("courses")
    object Profile : Screen("profile")
    object AdminPanel : Screen("admin_panel")

    object MajorDetail : Screen("majorDetail/{majorNo}/{majorName}") {
        fun createRoute(majorNo: Long, majorName: String) = "majorDetail/$majorNo/$majorName"
    }

    object CourseDetail : Screen("courseDetail/{courseNo}/{courseName}") {
        fun createRoute(courseNo: Long, courseName: String) = "courseDetail/$courseNo/$courseName"
    }

    // 话题详情页
    object TopicDetail : Screen("topicDetail/{topicId}") {
        fun createRoute(topicId: Long) = "topicDetail/$topicId"
    }

    // 发帖页：referencePath 设为查询参数，避免路径斜杠干扰路由
    object CreateTopic : Screen("createTopic/{courseNo}?refPath={refPath}") {
        fun createRoute(courseNo: Long, refPath: String? = null): String {
            return if (refPath != null) "createTopic/$courseNo?refPath=$refPath"
            else "createTopic/$courseNo"
        }
    }
}