// src/router/index.js
import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import LoginView from '../views/LoginView.vue'
import RegisterView from '../views/RegisterView.vue'
import CoursesView from "@/views/CoursesView.vue";
import SettingsView from "@/views/SettingsView.vue";

const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes: [
        {
            path: '/',
            name: 'home',
            component: HomeView
        },
        {
            path: '/login',
            name: 'login',
            component: LoginView
        },
        {
            path: '/register',
            name: 'register',
            component: RegisterView
        }
        ,        {
            path: '/courses',
            name: 'courses',
            component: CoursesView
        },
        // section START: 新增课程详情页路由
        {
            // :courseNo 是一个动态参数，代表课程号
            path: '/course/:courseNo',
            name: 'CourseDetail',
            // 你需要新建这个文件，下面第二步会讲
            component: () => import('../views/CourseDetailView.vue')
        },
        // section END
        {
            path: '/course/:courseNo',
            name: 'CourseDetail',
            // 这里我们重定向，默认进去就看树图
            redirect: to => `/course/${to.params.courseNo}/tree`,
            children: [
                {
                    // 树形图页面
                    path: 'tree',
                    name: 'CourseTree',
                    component: () => import('../views/CourseTreeView.vue')
                },
                {
                    // 文件夹内文件展示页面 (使用 query 传递路径 path)
                    path: 'folder',
                    name: 'FolderFiles',
                    component: () => import('../views/FolderFilesView.vue')
                }
            ]
        },
        {
            path: '/settings',
            name: 'Settings',
            component: SettingsView
        },
        {
            path: '/course/:courseNo/forum',
            name: 'CourseForum',
            component: () => import('@/views/CourseForumView.vue'), // 对应我上个回复给你的论坛主页
            props: true
        },
        {
            path: '/topic/:topicId',
            name: 'TopicDetail',
            component: () => import('@/views/TopicDetailView.vue'), // 对应话题详情页
            props: true
        },
        {
            path: '/course/:courseNo/forum/post',
            name: 'TopicPost',
            component: () => import('@/views/TopicPostView.vue'), // 对应发帖页
            props: true
        },
        {
            path: '/inbox',
            name: 'Inbox',
            component: () => import('@/views/InboxView.vue')
        }
    ]
})

export default router