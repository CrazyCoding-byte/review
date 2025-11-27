package com.yzx.web_flux_demo.file;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;

import java.io.File;

/**
 * @className: ZipPasswordCracker
 * @author: yzx
 * @date: 2025/11/23 3:23
 * @Version: 1.0
 * @description:
 */
public class ZipPasswordCracker {
    // 破解成功的密码
    private static String foundPassword = null;

    public static void main(String[] args) {
        String zipFilePath = "C:\\迪斯科\\DEMO\\12  开车不犯困(100首).zip"; // 要破解的 ZIP 文件路径
        int minLength = 1; // 密码最小长度
        int maxLength = 4; // 密码最大长度

        System.out.println("开始破解 ZIP 文件：" + zipFilePath);
        System.out.println("密码长度范围：" + minLength + "-" + maxLength);

        // 定义字符集（可以根据需要扩展）
        char[] charset = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ!@#$%^&*()_+-=[]{}|;':\",./<>?".toCharArray();

        // 开始暴力破解
        long startTime = System.currentTimeMillis();
        bruteForce(zipFilePath, minLength, maxLength, charset);
        long endTime = System.currentTimeMillis();

        if (foundPassword != null) {
            System.out.println("破解成功！密码是：" + foundPassword);
            System.out.println("耗时：" + (endTime - startTime) / 1000.0 + " 秒");
        } else {
            System.out.println("破解失败，未找到密码（可能密码长度超过范围或字符集不匹配）。");
        }
    }

    /**
     * 暴力破解 ZIP 密码
     * @param zipFilePath ZIP 文件路径
     * @param minLength 密码最小长度
     * @param maxLength 密码最大长度
     * @param charset 字符集
     */
    private static void bruteForce(String zipFilePath, int minLength, int maxLength, char[] charset) {
        // 遍历密码长度
        for (int length = minLength; length <= maxLength; length++) {
            System.out.println("正在尝试密码长度：" + length);
            // 生成当前长度的所有可能密码组合
            generatePasswords(zipFilePath, length, charset, new char[length], 0);
            // 如果已经找到密码，提前退出
            if (foundPassword != null) {
                break;
            }
        }
    }

    /**
     * 递归生成密码组合并尝试破解
     * @param zipFilePath ZIP 文件路径
     * @param length 密码长度
     * @param charset 字符集
     * @param currentPassword 当前生成的密码
     * @param index 当前字符索引
     */
    private static void generatePasswords(String zipFilePath, int length, char[] charset, char[] currentPassword, int index) {
        // 如果已经找到密码，直接返回
        if (foundPassword != null) {
            return;
        }

        // 密码生成完成，尝试破解
        if (index == length) {
            String password = new String(currentPassword);
            // 每尝试 1000 个密码输出一次进度
            String digitsOnly = new String(currentPassword).replaceAll("[^0-9]", "");
            if (!digitsOnly.isEmpty() && Integer.parseInt(digitsOnly) % 1000 == 0) {
                System.out.println("尝试密码：" + password);
            }
            if (isPasswordValid(zipFilePath, password)) {
                foundPassword = password;
            }
            return;
        }

        // 递归生成下一个字符
        for (char c : charset) {
            currentPassword[index] = c;
            generatePasswords(zipFilePath, length, charset, currentPassword, index + 1);
            // 如果已经找到密码，提前退出
            if (foundPassword != null) {
                break;
            }
        }
    }

    /**
     * 验证密码是否正确
     * @param zipFilePath ZIP 文件路径
     * @param password 待验证的密码
     * @return 是否正确
     */
    private static boolean isPasswordValid(String zipFilePath, String password) {
        try {
            ZipFile zipFile = new ZipFile(zipFilePath);
            // 如果 ZIP 文件已加密，尝试用密码解密
            if (zipFile.isEncrypted()) {
                zipFile.setPassword(password.toCharArray());
            }
            // 尝试提取一个文件来验证密码（提取到临时目录）
            String tempDir = System.getProperty("java.io.tmpdir") + File.separator + "zip-test";
            zipFile.extractAll(tempDir);
            // 验证成功，删除临时文件
            deleteDir(new File(tempDir));
            return true;
        } catch (ZipException e) {
            // 密码错误或其他异常，返回 false
            return false;
        }
    }

    /**
     * 删除目录（用于清理临时文件）
     * @param dir 目录
     * @return 是否删除成功
     */
    private static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            File[] children = dir.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteDir(child);
                }
            }
        }
        return dir.delete();
    }
}
