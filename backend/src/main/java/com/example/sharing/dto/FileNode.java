package com.example.sharing.dto;


import java.util.List;

public class FileNode {
    private String name;        // 文件/目录名
    private String path;        // 相对课程目录的路径
    private String type;        // "file" or "directory"
    private List<FileNode> children;

    public FileNode() {}

    public FileNode(String name, String path, String type, List<FileNode> children) {
        this.name = name;
        this.path = path;
        this.type = type;
        this.children = children;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<FileNode> getChildren() {
        return children;
    }

    public void setChildren(List<FileNode> children) {
        this.children = children;
    }
}