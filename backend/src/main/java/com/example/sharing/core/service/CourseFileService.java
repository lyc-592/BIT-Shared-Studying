package com.example.sharing.core.service;

import com.example.sharing.core.dto.FileNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class CourseFileService {

    // 直接在代码里写死根路径
    @Value("${file.storage.base-path}")
    private String COURSE_ROOT;

    /**
     * 对外：根据课程号返回文件树（数组形式，根节点只有一个元素）
     */
    public List<FileNode> getCourseFileTree(String courseId) throws IOException {
        Path courseRootPath = Paths.get(COURSE_ROOT).toAbsolutePath().normalize();

        if (!Files.exists(courseRootPath) || !Files.isDirectory(courseRootPath)) {
            // 根目录不存在，直接返回空
            return new ArrayList<>();
        }

        // 1. 根据前缀模糊查找课程目录
        Path courseDir = findCourseDirectory(courseRootPath, courseId);
        if (courseDir == null) {
            // 没找到对应课程目录
            return new ArrayList<>();
        }

        // 2. 构建树
        FileNode rootNode = buildNodeRecursive(courseDir, courseDir);

        List<FileNode> result = new ArrayList<>();
        result.add(rootNode);   // 根节点数组只放这一个
        return result;
    }

    /**
     * 在根目录下，根据课程号前缀找到唯一的课程目录
     */
    private Path findCourseDirectory(Path courseRootPath, String courseId) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(courseRootPath)) {
            for (Path path : stream) {
                if (Files.isDirectory(path)) {
                    String dirName = path.getFileName().toString();
                    if (dirName.startsWith(courseId)) {
                        return path;    // 找到第一个匹配的就返回
                    }
                }
            }
        }
        return null;
    }

    /**
     * 递归构造文件树节点
     *
     * @param current    当前路径
     * @param courseRoot 课程目录根，用于计算相对路径
     */
    private FileNode buildNodeRecursive(Path current, Path courseRoot) throws IOException {
        String name = current.getFileName().toString();
        String relativePath = courseRoot.relativize(current).toString().replace("\\", "/");

        if (Files.isDirectory(current)) {
            List<FileNode> children = new ArrayList<>();

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(current)) {
                for (Path child : stream) {
                    children.add(buildNodeRecursive(child, courseRoot));
                }
            }

            // 目录在前，文件在后，按名称排序
            children.sort(Comparator
                    .comparing((FileNode n) -> "file".equals(n.getType()) ? 1 : 0)
                    .thenComparing(FileNode::getName));

            return new FileNode(name, relativePath, "directory", children);
        } else {
            return new FileNode(name, relativePath, "file", null);
        }
    }
}