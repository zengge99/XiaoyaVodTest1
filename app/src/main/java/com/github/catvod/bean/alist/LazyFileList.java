package com.github.catvod.bean.alist;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.AbstractList;
import java.util.Iterator;
import java.util.List;
import java.io.RandomAccessFile;

public class LazyFileList extends AbstractList<String> {
    private final String filePath;
    private int size = -1;

    public LazyFileList(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public String get(int index) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            for (int i = 0; i <= index; i++) {
                String line = reader.readLine();
                if (i == index) {
                    return line; // 返回指定行的内容
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("读取文件失败: " + filePath, e);
        }
        throw new IndexOutOfBoundsException("文件行数不足: " + index);
    }

    /*
    @Override
    public String get(int index) {
        try (RandomAccessFile file = new RandomAccessFile(filePath, "r")) {
            long pointer = 0;
            int currentLineNumber = 0;
            while (currentLineNumber < index) {
                file.seek(pointer);
                String line = file.readLine();
                if (line == null) {
                    return null; // 目标行不存在
                }
                currentLineNumber++;
                pointer = file.getFilePointer();
            }
            file.seek(pointer);
            return file.readLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    */

    @Override
    public Iterator<String> iterator() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(filePath));
            return new Iterator<String>() {
                private String nextLine;

                @Override
                public boolean hasNext() {
                    try {
                        nextLine = reader.readLine(); // 读取下一行
                        return nextLine != null;
                    } catch (IOException e) {
                        throw new RuntimeException("读取文件失败", e);
                    }
                }

                @Override
                public String next() {
                    return nextLine; // 返回当前行
                }
            };
        } catch (IOException e) {
            throw new RuntimeException("无法打开文件: " + filePath, e);
        }
    }

    @Override
    public int size() {
        if (size != -1) {
            return size;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            int count = 0;
            while (reader.readLine() != null) {
                count++; // 统计文件行数
            }
            size = count;
            return size;
        } catch (IOException e) {
            throw new RuntimeException("读取文件失败: " + filePath, e);
        }
    }
}
