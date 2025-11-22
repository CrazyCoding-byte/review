package com.yzx.web_flux_demo.file;

import java.io.*;
import java.nio.file.*;
import java.util.stream.Stream;

/**
 * @className: FilesDemo
 * @author: yzx
 * @date: 2025/11/22 0:43
 * @Version: 1.0
 * @description:
 */
public class FilesDemo {
    public static void bioDemo() {
        try (FileInputStream fis = new FileInputStream("xx")) {
            byte[] bytes = new byte[1024];
            int len;
            while ((len = fis.read(bytes)) != -1) {
                System.out.println(new String(bytes, 0, bytes.length));
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void nio() throws IOException {
        //判断文件 / 目录是否存在（LinkOption.NOFOLLOW_LINKS 不跟随符号链接
        Files.exists(Paths.get("xx"), LinkOption.NOFOLLOW_LINKS);
        Files.isRegularFile(Paths.get("xx"), LinkOption.NOFOLLOW_LINKS);//判断是否是文件
        Files.isDirectory(Paths.get("xx"), LinkOption.NOFOLLOW_LINKS);//判断是否是目录
        Files.createFile(Paths.get("xx"));//创建文件
        Files.createDirectories(Paths.get("xx/xx2"));//创建多级目录
        Files.delete(Paths.get("xx"));//删除文件
        Files.deleteIfExists(Paths.get("xx"));//如果存在就删除不存在不会有异常
        Files.copy(Paths.get("xx"), Paths.get("xx2"), StandardCopyOption.ATOMIC_MOVE);//复制文件
        Files.readAllBytes(Paths.get("xx"));//读取文件内容
        Files.write(Paths.get("xx"), "hello".getBytes());//写入文件
        Files.readAllLines(Paths.get("xx"));//读取文件内容
        Files.newBufferedReader(Paths.get("xx")).lines().forEach(System.out::println);//创建缓冲字符读取器
        Files.newBufferedWriter(Paths.get("xx")).write("hello");//创建缓冲字符写入器
        Files.walk(Paths.get("xx")).forEach(System.out::println);//遍历目录
        Files.walk(Paths.get("xx"), 2);//只要二级目录
        //找到符合的文件或者目录
        Files.find(Paths.get("xx"), 3, (path, attributes) -> path.getFileName().toString().endsWith(".mp3") && attributes.isRegularFile());
    }

    public static void nio2() throws IOException {
        Path path = Paths.get("xx1");
        Path path1 = Paths.get("xx2");
        //复制文件若是目标存在则覆盖
        Files.copy(path, path1, StandardCopyOption.REPLACE_EXISTING);//复制文件
        //// 遍历 "dir" 目录及其子目录，查找所有 .txt 文件
        Stream<Path> walk = Files.walk(Paths.get("xx"));
        walk.filter(Files::isRegularFile)
                .filter(item -> item.getFileName().toString().endsWith(".mp3")).forEach(System.out::println);
    }

    public static void main(String[] args) throws IOException {
        new File("xx").exists();//判断文件是否存在
        new File("xx").isFile();//判断是否是文件
        new File("xx").delete();//删除文件
        new File("xx").getAbsoluteFile(); //获取文件的绝对路径
        new File("xx").getName(); //获取文件名
        new File("xx").listFiles();//获取当前目录下列表
        new FileInputStream(new File("xx")).read();//读取单个字节
        byte[] bytes = new byte[1024];
        new FileInputStream(new File("xx")).read(bytes);//读取多个字节
        new FileInputStream(new File("xx")).close();//关闭文件
        new FileOutputStream(new File("xx")).write(65);//写入一个字节字符A
        new FileOutputStream(new File("xx")).write("hello".getBytes());//写入多个字节
        new FileOutputStream(new File("xx")).write(bytes, 0, bytes.length);//写入多个字节

        File file = new File("xx/xx1"); //创建多级目录
        if (!file.exists()) {
            file.mkdir();
        }
    }
}
