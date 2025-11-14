package com.yzx.web_flux_demo.file;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @className: FileDemo
 * @author: yzx
 * @date: 2025/11/12 16:46
 * @Version: 1.0
 * @description:
 */
public class FileDemo {

    public static void main(String[] args) throws IOException {
        Path path = Paths.get("E:\\致谢.txt");
        File file = path.toFile();
        byte[] bytes = new byte[1024];
        int len;
        BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(file));
        while ((len = bufferedInputStream.read(bytes)) != -1) {
            System.out.println(new String(bytes, 0, len));
        }
    }
}
