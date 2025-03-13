package com.github.catvod.bean.alist;

import java.io.*; 
import java.net.URL;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import com.github.catvod.bean.Vod;
import com.github.catvod.spider.Logger;
import android.os.Debug;

public class XiaoyaLocalIndex {
    private static Map<String, List<String>> cacheMap = new HashMap<>();
    private static Map<String, Map<String, List<Integer>>> invertedIndexMap = new HashMap<>();

    public static synchronized List<String> downlodadAndUnzip(String server) {

        System.gc();
        long startMemory = Debug.getNativeHeapAllocatedSize();
        long usedMemory = 0;

        List<String> lines = cacheMap.get(server);
        if (lines != null) {
            return lines;
        }

        try {
            System.gc();
            long startMemory2 = Debug.getNativeHeapAllocatedSize();
            String fileUrl = server + "/tvbox/data";
            String saveDir = com.github.catvod.utils.Path.root().getPath() + "/TV/index/" + server.split("//")[1].replace(":", "_port");
            Logger.log(saveDir);

            // 0. 清空目录
            deleteFiles(saveDir, null); // 删除 saveDir 中的所有文件

            // 1. 确保目录存在
            createDirectoryIfNotExists(saveDir);

            // 2. 下载文件
            downloadFile(fileUrl + "/index.video.tgz", saveDir + "/index.video.tgz");
            downloadFile(fileUrl + "/index.115.tgz", saveDir + "/index.115.tgz");

            // 3. 解压文件
            unzipFile(saveDir + "/index.video.tgz", saveDir);
            unzipFile(saveDir + "/index.115.tgz", saveDir);

            mergeFiles(saveDir, saveDir + "/index.all.txt");

            // 4. 删除指定文件
            deleteFilesExclude(saveDir, "index.all.txt");
            deleteFiles(saveDir, "*.tgz");
            System.gc();
            usedMemory = Debug.getNativeHeapAllocatedSize() - startMemory2;
            Logger.log("文件处理消耗内存：" + usedMemory);

            System.gc();
            long startMemory1 = Debug.getNativeHeapAllocatedSize();
            //lines = Files.readAllLines(Paths.get(saveDir + "/index.all.txt"));
            lines = new LazyFileList(saveDir + "/index.all.txt");
            System.gc();
            usedMemory = Debug.getNativeHeapAllocatedSize() - startMemory1;
            Logger.log("仅索引部分消耗内存：" + usedMemory);

            // 构建倒排索引，用于快速查找
            Map<String, List<Integer>> invertedIndex = new HashMap<>();
            for (int i = 0; i < lines.size(); i++) {
                String[] words = lines.get(i).split("#");
                if (words.length < 2) {
                    continue;
                }
                String word = words[1];
                invertedIndex.computeIfAbsent(word.toLowerCase(), k -> new ArrayList<>()).add(i);
            }

            System.gc();
            long startMemory3 = Debug.getNativeHeapAllocatedSize();
            invertedIndexMap.put(server, invertedIndex);
            cacheMap.put(server, lines);
            System.gc();
            usedMemory = Debug.getNativeHeapAllocatedSize() - startMemory3;
            Logger.log("倒排索引消耗内存：" + usedMemory);

        } catch (IOException e) {
            log("操作失败: " + e.getMessage());
        }

        System.gc();
        usedMemory = Debug.getNativeHeapAllocatedSize() - startMemory;
        Logger.log("本地索引整体消耗内存：" + usedMemory);

        return lines;
    }

    public static List<String> quickSearch(String server, String keyword) {

        downlodadAndUnzip(server);
        List<Integer> lineNumbers = invertedIndexMap.get(server).get(keyword);
        if (lineNumbers == null) {
            return new ArrayList<>();
        }
        List<String> lines = new ArrayList<>();
        for (Integer i : lineNumbers) {
            lines.add(cacheMap.get(server).get(i));
        }
        return lines;
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
     * 解压文件（支持 ZIP 和 TGZ 格式）
     *
     * @param filePath   压缩文件路径
     * @param extractDir 解压目录
     */
    private static void unzipFile(String filePath, String extractDir) throws IOException {
        createDirectoryIfNotExists(extractDir); // 确保解压目录存在

        if (filePath.toLowerCase().endsWith(".zip")) {
            // 处理 ZIP 文件
            try (ZipFile zipFile = new ZipFile(filePath)) {
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
            }
        } else if (filePath.toLowerCase().endsWith(".tgz") || filePath.toLowerCase().endsWith(".tar.gz")) {
            // 处理 TGZ 文件
            try (InputStream fi = new FileInputStream(filePath);
                    InputStream gi = new GzipCompressorInputStream(fi);
                    TarArchiveInputStream ti = new TarArchiveInputStream(gi)) {
                TarArchiveEntry entry;
                while ((entry = ti.getNextTarEntry()) != null) {
                    Path entryPath = Paths.get(extractDir, entry.getName());
                    if (entry.isDirectory()) {
                        createDirectoryIfNotExists(entryPath.toString());
                    } else {
                        try (OutputStream os = Files.newOutputStream(entryPath)) {
                            byte[] buffer = new byte[1024 * 1024]; // 分块读取，每次读取 1MB
                            int bytesRead;
                            while ((bytesRead = ti.read(buffer)) != -1) {
                                os.write(buffer, 0, bytesRead);
                            }
                        }
                    }
                }
            }
        } else {
            throw new IOException("不支持的文件格式: " + filePath);
        }
    }

    /**
     * 删除目录中除了指定文件以外的所有文件
     *
     * @param dirPath      目录路径
     * @param excludeFiles 需要排除的文件名（不删除这些文件）
     */
    private static void deleteFilesExclude(String dirPath, String... excludeFiles) throws IOException {
        Path path = Paths.get(dirPath);

        // 如果目录不存在，直接返回
        if (!Files.exists(path)) {
            log("目录不存在，无需删除: " + dirPath);
            return;
        }

        // 如果路径不是目录，抛出异常
        if (!Files.isDirectory(path)) {
            throw new IOException("路径不是目录: " + dirPath);
        }

        try (var stream = Files.newDirectoryStream(path)) {
            for (Path file : stream) {
                // 检查是否需要排除该文件
                boolean shouldExclude = false;
                for (String excludeFile : excludeFiles) {
                    if (file.getFileName().toString().equals(excludeFile)) {
                        shouldExclude = true;
                        break;
                    }
                }

                if (!shouldExclude) {
                    if (Files.isDirectory(file)) {
                        // 如果是目录，递归删除
                        deleteFilesExclude(file.toString(), excludeFiles);
                        Files.delete(file); // 删除空目录
                        log("已删除目录: " + file);
                    } else {
                        Files.delete(file);
                        log("已删除文件: " + file);
                    }
                }
            }
        } catch (IOException e) {
            throw new IOException("删除文件失败: " + dirPath, e);
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

        // 如果目录不存在，直接返回
        if (!Files.exists(path)) {
            log("目录不存在，无需删除: " + dirPath);
            return;
        }

        // 如果路径不是目录，抛出异常
        if (!Files.isDirectory(path)) {
            throw new IOException("路径不是目录: " + dirPath);
        }

        try (var stream = pattern == null ? Files.newDirectoryStream(path) : Files.newDirectoryStream(path, pattern)) {
            for (Path file : stream) {
                if (Files.isDirectory(file)) {
                    // 如果是目录，递归删除
                    deleteFiles(file.toString(), null);
                    Files.delete(file); // 删除空目录
                    log("已删除目录: " + file);
                } else {
                    Files.delete(file);
                    log("已删除文件: " + file);
                }
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
        Path outputFilePath = Paths.get(outputFile);
    
        try ( Path outputFileObj = Paths.get(outputFilePath)) {
            try (var stream = Files.newDirectoryStream(dirPath, "*.txt")) {
                for (Path file : stream) {
                    if (file.equals(outputFilePath)) {
                        log("跳过文件: " + file);
                        continue;
                    }
                    Files.copy(file, outputFileObj);
                    log("已合并文件: " + file);
                }
            }
        } catch (IOException e) {
            throw new IOException("合并文件失败: " + extractDir, e);
        }
    }
    /* 
    private static void mergeFiles(String extractDir, String outputFile) throws IOException {
        Path dirPath = Paths.get(extractDir);
        Path outputFilePath = Paths.get(outputFile);

        try (BufferedWriter writer = Files.newBufferedWriter(outputFilePath)) {
            try (var stream = Files.newDirectoryStream(dirPath, "*.txt")) {
                for (Path file : stream) {
                    // 跳过 outputFile
                    if (file.equals(outputFilePath)) {
                        log("跳过文件: " + file);
                        continue;
                    }

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
    */

    /**
     * 日志输出
     *
     * @param message 日志信息
     */
    private static void log(String message) {
        System.out.println(message);
    }
}
