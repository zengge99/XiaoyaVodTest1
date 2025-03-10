package com.github.catvod.bean.alist;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class XiaoyaLocalIndex {

    public static void downlodadAndUnzip() {
        String fileUrl = "http://zengge99.1996999.xyz:5678/data/index.zip";
        String saveDir = "/storage/emulated/0/TV/index"; // 保存到指定目录
        String savePath = saveDir + "/index.zip"; // 下载文件的保存路径
        String extractDir = saveDir + "/extracted"; // 解压目录

        try {
            // 1. 下载文件
            downloadFile(fileUrl, savePath);
            System.out.println("文件下载完成: " + savePath);

            // 2. 解压文件
            unzipFile(savePath, extractDir);
            System.out.println("文件解压完成，解压到: " + extractDir);

            // 3. 删除指定文件
            deleteFiles(extractDir, "index.docu.*.txt");
            deleteFiles(extractDir, "index.music.txt");
            deleteFiles(extractDir, "index.non.video.txt");
            System.out.println("指定文件已删除");

            // 4. 合并剩余文件
            mergeFiles(extractDir, outputFile);
            System.out.println("文件合并完成，保存为: " + outputFile);

            // 5. 删除解压目录中的所有文件（排除 index.video.txt）
            deleteAllFilesExcept(extractDir, "index.video.txt");
            System.out.println("解压目录中的其他文件已删除");

        } catch (IOException e) {
            System.err.println("操作失败: " + e.getMessage());
        }
    }

    /**
     * 下载文件
     *
     * @param fileUrl  文件URL
     * @param savePath 保存路径
     */
    private static void downloadFile(String fileUrl, String savePath) throws IOException {
        URL url = new URL(fileUrl);
        try (InputStream in = new BufferedInputStream(url.openStream());
                FileOutputStream out = new FileOutputStream(savePath)) {
            byte[] buffer = new byte[8 * 1024]; // 分块读取，每次读取 8KB
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
    }

    /**
     * 解压文件
     *
     * @param zipPath    ZIP文件路径
     * @param extractDir 解压目录
     */
    private static void unzipFile(String zipPath, String extractDir) throws IOException {
        Path extractPath = Paths.get(extractDir);
        if (!Files.exists(extractPath)) {
            Files.createDirectories(extractPath);
        }

        try (ZipFile zipFile = new ZipFile(zipPath)) {
            var entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                Path entryPath = extractPath.resolve(entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    try (InputStream is = zipFile.getInputStream(entry)) {
                        Files.copy(is, entryPath);
                    }
                }
            }
        }
    }

    /**
     * 删除指定文件
     *
     * @param extractDir 解压目录
     * @param pattern    文件名模式（支持通配符 *）
     */
    private static void deleteFiles(String extractDir, String pattern) throws IOException {
        Path dirPath = Paths.get(extractDir);
        try (var stream = Files.newDirectoryStream(dirPath, pattern)) {
            for (Path file : stream) {
                Files.delete(file);
                System.out.println("已删除文件: " + file);
            }
        }
    }

    /**
     * 合并文件
     *
     * @param extractDir 解压目录
     * @param outputFile 合并后的文件路径
     */
    private static void mergeFiles(String extractDir, String outputFile) throws IOException {
        Path dirPath = Paths.get(extractDir);
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(outputFile))) {
            try (var stream = Files.newDirectoryStream(dirPath, "*.txt")) {
                for (Path file : stream) {
                    try (BufferedReader reader = Files.newBufferedReader(file)) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            writer.write(line);
                            writer.newLine();
                        }
                    }
                    System.out.println("已合并文件: " + file);
                }
            }
        }
    }

    /**
     * 删除解压目录中的所有文件（排除指定文件）
     *
     * @param extractDir  解压目录
     * @param excludeFile 需要排除的文件名
     */
    private static void deleteAllFilesExcept(String extractDir, String excludeFile) throws IOException {
        Path dirPath = Paths.get(extractDir);
        try (var stream = Files.newDirectoryStream(dirPath)) {
            for (Path file : stream) {
                if (Files.isRegularFile(file) && !file.getFileName().toString().equals(excludeFile)) {
                    Files.delete(file);
                    System.out.println("已删除文件: " + file);
                }
            }
        }
    }
}
