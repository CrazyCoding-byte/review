package com.yzx.web_flux_demo.file;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


/**
 * @className: FileDemo
 * @author: yzx
 * @date: 2025/11/12 16:46
 * @Version: 1.0
 * @description:
 */
public class FileDemo {
    //bio找文件

    /**
     * BIO 查找的特点：
     * 手动递归：必须自己写递归逻辑，代码繁琐，容易出错（如忘记处理 listFiles() 返回 null 的情况）；
     * 效率较低：listFiles() 每次调用都会触发系统调用，且返回的是 File 数组（需要拷贝数据），大目录下性能较差；
     * 灵活性差：筛选条件（如文件后缀、大小）需要手动在循环中判断，无法直接复用
     * @param rootPath
     * @return
     */
    public static List<String> findTexFiles(String rootPath) {
        List<String> result = new ArrayList<>();
        File root = new File(rootPath);
        if (!root.exists() || !root.isDirectory()) {
            System.out.println("目录不存在");
            return result;
        }
        File[] files = root.listFiles(); // 1. 获取目录下的所有子项
        // 3. 遍历子项，递归处理
        for (File file : files) {
            if (file.isDirectory()) {
                // 递归遍历子目录
                result.addAll(findTexFiles(file.getAbsolutePath()));
            } else {
                // 筛选 .txt 文件
                if (file.getName().endsWith(".mp3")) {
                    result.add(file.getAbsolutePath());
                }
            }
        }
        return result;
    }

    public static void nioSearchFiles(String rootPath) {
        //1.递归遍历目录
        try {
            List<String> collect = Files.walk(Paths.get(rootPath)).
                    filter(Files::isRegularFile)//排除目录
                    .filter(path -> path.getFileName().toString().endsWith(".mp3"))//筛选文件
                    //3.转换为绝对路径字符串
                    .map(path -> path.toAbsolutePath().toString()).
                    //4.收集结果
                            collect(Collectors.toList());
            System.out.println("找到 " + collect.size() + " 个 .txt 文件：");
            collect.forEach(System.out::println);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void nioSearchFiles2(String rootPath) {
        //1.文件路径 2.搜索深度 3.筛选条件 4.处理结果
        try {
            List<String> collect = Files.find(Paths.get(rootPath), Integer.MAX_VALUE, (path, basicFileAttributes) -> basicFileAttributes.isRegularFile() && path.getFileName().toString().endsWith(".mp3"))
                    .map(Path::toAbsolutePath)
                    .map(Path::toString)
                    .collect(Collectors.toList());
            System.out.println("找到 " + collect.size() + " 个 .txt 文件：");
            collect.forEach(System.out::println);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    //只删除文件不删除目录

    /**
     * 循环删除（例如清空文件夹内所有文件和子目录）是开发中常见的需求，BIO 和 NIO 都能实现，但 NIO 更高效简洁。以下是两种方式的详细实现及对比：
     * 一、核心需求说明
     * 清空文件夹的两种常见场景：
     * 仅删除文件，保留目录结构（即删除所有文件，但子文件夹仍存在）；
     * 删除所有文件及子目录（即递归删除文件夹内所有内容，包括子文件夹本身）。
     * 以下分别用 BIO 和 NIO 实现这两种场景。
     * 二、BIO 实现循环删除（基于 java.io.File）
     * BIO 的核心思路是 递归遍历目录树：先处理子文件 / 子目录，再根据需求决定是否删除当前目录。
     * @param dirPath
     */
    public static void bioDeleteFilesInDir(String dirPath) {
        File file = new File(dirPath);
        // 检查目录是否存在
        if (!file.exists() || !file.isDirectory()) {
            System.out.println("目录不存在或不是目录：" + dirPath);
            return;
        }
        File[] files = file.listFiles();
        for (File f : files) {
            if (f.isFile()) {
                if (f.getName().endsWith(".mav")) {
                    if (f.delete()) {
                        System.out.println("删除文件成功：" + f.getAbsolutePath());
                    }
                } else {
                    System.out.println("删除文件失败：" + f.getAbsolutePath());
                }

            } else if (f.isDirectory()) {
                // 是目录，递归处理子目录（仅删除子目录内的文件）
                bioDeleteFilesInDir(file.getAbsolutePath());
            }
        }
    }

    //删除文件又删除目录
    public static void bioDeleteFilesInDir2(String dirPath) {
        File file = new File(dirPath);
        if (!file.exists() || !file.isDirectory()) {
            System.out.println("目录不存在或不是目录：" + dirPath);
            return;
        }
        File[] files = file.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isFile()) {
                    if (f.getName().endsWith(".mav")) {
                        if (f.delete()) {
                            System.out.println("删除文件成功：" + f.getAbsolutePath());
                        }
                    } else {
                        System.out.println("删除文件失败：" + f.getAbsolutePath());
                    }
                } else if (f.isDirectory()) {
                    bioDeleteFilesInDir2(f.getAbsolutePath());
                }
            }
        }
        //删除当前空目录
        file.delete();
        System.out.println("删除目录成功：" + dirPath);
    }

    /**
     * BIO 循环删除的特点：
     * 逻辑直观：通过递归遍历，手动控制文件和目录的删除顺序；
     * 代码繁琐：需手动判断文件 / 目录类型，处理 listFiles() 返回 null 的情况（权限不足等）；
     * 效率较低：listFiles() 每次触发系统调用，递归过程中多次创建 File 对象，大目录下性能较差。
     * 三、NIO 实现循环删除（基于 java.nio.file）
     * NIO 提供 Files.walkFileTree() 方法，支持 深度优先遍历目录树，结合 FileVisitor 接口实现灵活的删除逻辑，无需手动递归。
     * 核心 API：
     * Files.walkFileTree(Path start, FileVisitor<? super Path> visitor)：遍历目录树，FileVisitor 定义了遍历过程中的回调方法（访问前、访问文件、访问后、失败时）；
     * Files.delete(Path path)：删除文件或空目录；
     * SimpleFileVisitor：FileVisitor 的默认实现，可重写需要的方法（无需实现所有接口方法）。
     * @param dirPath
     */
    public static void nioDeleteFilesInDir(String dirPath) {
        try {
            Path path = Paths.get(dirPath);
            //遍历目录树,重写visitFile方法删除文件
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                //访问文件时触发(仅删除文件)
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    System.out.println("删除文件成功：" + file.toAbsolutePath());
                    return FileVisitResult.CONTINUE;//继续遍历下一个文件
                }

                //访问目录时触发(不删除目录,仅仅继续遍历子目录)
                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    return FileVisitResult.CONTINUE;//继续遍历下一个目录
                }
            });
            System.out.println("文件删除成功：" + dirPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void nioDeleteFilesInDir2(String dirPath) {
        Path targetDir = Paths.get(dirPath);
        try {
            // 遍历目录树，先删除文件，再删除目录
            Files.walkFileTree(targetDir, new SimpleFileVisitor<Path>() {
                // 访问文件时删除
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    System.out.println("删除文件：" + file);
                    return FileVisitResult.CONTINUE;
                }

                // 访问目录后删除（确保子项已全部删除）

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    System.out.println("删除目录：" + dir);
                    return FileVisitResult.CONTINUE;
                }
            });
            System.out.println("目录彻底清空完成！");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
//        Path path = Paths.get("E:\\致谢.txt");
//        File file = path.toFile();
//        byte[] bytes = new byte[1024];
//        int len;
//        BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(file));
//        while ((len = bufferedInputStream.read(bytes)) != -1) {
//            System.out.println(new String(bytes, 0, len));
//        }
//        List<String> txtFiles = findTexFiles("C:\\迪斯科");
//        System.out.println("找到 " + txtFiles.size() + " 个 .mp3 文件：");
//        for (String path : txtFiles) {
//            System.out.println(path);
//        }
        nioSearchFiles("C:\\迪斯科");
    }
}
