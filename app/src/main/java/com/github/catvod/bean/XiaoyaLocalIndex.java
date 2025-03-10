package com.github.catvod.bean.alist;

import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class XiaoyaLocalIndex {

    public static void downlodadAndUnzip() {
        String fileUrl = "http://zengge99.1996999.xyz:5678/data/index.zip";
        String saveDir = "/storage/emulated/0/TV/index"; // 保存到指定目录
        String savePath = saveDir + "/index.zip"; // 下载文件的保存路径
        String extractDir = saveDir + "/extracted"; // 解压目录
        String outputFile = saveDir + "/index.video.txt"; // 合并后的文件路径

        try {
            // 0. 清空目录
            //deleteFiles(saveDir, null); // 删除 saveDir 中的所有文件
            log("目录已清空: " + saveDir);

            // 1. 确保目录存在
            createDirectoryIfNotExists(saveDir);
            createDirectoryIfNotExists(extractDir);

            // 2. 下载文件
            downloadFile(fileUrl, savePath);
            log("文件下载完成: " + savePath);

            // 3. 解压文件
            unzipFile(savePath, extractDir);
            log("文件解压完成，解压到: " + extractDir);

            // 4. 删除指定文件
            deleteFiles(extractDir, "index.docu.*.txt");
            deleteFiles(extractDir, "index.music.txt");
            deleteFiles(extractDir, "index.non.video.txt");
            log("指定文件已删除");

            // 5. 合并剩余文件
            mergeFiles(extractDir, outputFile);
            log("文件合并完成，保存为: " + outputFile);

            // 6. 删除解压目录中的所有文件
            deleteFiles(extractDir, null); // 删除 extractDir 中的所有文件
            log("解压目录中的其他文件已删除");

        } catch (IOException e) {
            log("操作失败: " + e.getMessage());
        }
    }

    /**
     * 确保目录存在，如果不存在则创建
     *
     * @param dirPath 目录路径
     */
    private static void createDirectoryIfNotExists(String dirPath) throws IOException {
        Path path = Paths.get(dirPath);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
            log("目录已创建: " + dirPath);
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
        } catch (IOException e) {
            throw new IOException("下载文件失败: " + fileUrl, e);
        }
    }

    /**
     * 解压文件
     *
     * @param zipPath    ZIP文件路径
     * @param extractDir 解压目录
     */
    private static void unzipFile(String zipPath, String extractDir) throws IOException {
        createDirectoryIfNotExists(extractDir); // 确保解压目录存在

        try (ZipFile zipFile = new ZipFile(zipPath)) {
            var entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                Path entryPath = Paths.get(extractDir, entry.getName());
                if (entry.isDirectory()) {
                    createDirectoryIfNotExists(entryPath.toString());
                } else {
                    try (InputStream is = zipFile.getInputStream(entry)) {
                        Files.copy(is, entryPath);
                    }
                }
            }
        } catch (IOException e) {
            throw new IOException("解压文件失败: " + zipPath, e);
        }
    }

    /**
     * 删除指定文件
     *
     * @param dirPath 目录路径
     * @param pattern 文件名模式（支持通配符 *，如果为 null 则删除所有文件）
     */
    private static void deleteFiles(String dirPath, String pattern) throws IOException {
        Path path = Paths.get(dirPath);
        try (var stream = pattern == null ? Files.newDirectoryStream(path) : Files.newDirectoryStream(path, pattern)) {
            for (Path file : stream) {
                Files.delete(file);
                log("已删除文件: " + file);
            }
        } catch (IOException e) {
            throw new IOException("删除文件失败: " + dirPath, e);
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
                    log("已合并文件: " + file);
                }
            }
        } catch (IOException e) {
            throw new IOException("合并文件失败: " + extractDir, e);
        }
    }

    /**
     * 日志输出
     *
     * @param message 日志信息
     */
    private static void log(String message) {
        System.out.println(message);
    }
}
