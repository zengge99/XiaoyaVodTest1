import java.io.BufferedInputStream;
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
        String savePath = "index.zip"; // 下载文件的保存路径
        String extractDir = "extracted"; // 解压目录

        try {
            // 1. 下载文件
            downloadFile(fileUrl, savePath);
            System.out.println("文件下载完成: " + savePath);

            // 2. 解压文件
            unzipFile(savePath, extractDir);
            System.out.println("文件解压完成，解压到: " + extractDir);
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
            byte[] buffer = new byte;
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
    }

    /**
     * 解压文件
     *
     * @param zipPath   ZIP文件路径
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
}
