package com.github.catvod.bean.alist;

import com.google.gson.Gson;
import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class FileBasedList<T> implements Iterable<T> {
    private final File file; // 存储数据的文件
    private final Gson gson; // Gson 用于序列化和反序列化
    private final Class<T> type; // 泛型类型
    private int size; // 当前列表的大小
    private final List<Long> linePositions; // 记录每一行的文件位置

    public FileBasedList(String filePath, Class<T> type) {
        this.file = new File(filePath);
        this.gson = new Gson();
        this.type = type;
        this.linePositions = new ArrayList<>();

        // 如果文件不存在，则创建
        if (!file.exists()) {
            try {
                file.createNewFile();
                this.size = 0; // 新文件大小为 0
            } catch (IOException e) {
                throw new RuntimeException("Failed to create file: " + filePath, e);
            }
        } else {
            // 如果文件已存在，初始化文件位置和大小
            initializeLinePositions();
        }
    }

    /**
     * 初始化文件位置和大小
     */
    private void initializeLinePositions() {
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r")) {
            long position = randomAccessFile.getFilePointer();
            String line;
            while ((line = randomAccessFile.readLine()) != null) {
                linePositions.add(position); // 记录当前行的起始位置
                position = randomAccessFile.getFilePointer(); // 更新位置
            }
            this.size = linePositions.size();
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize line positions", e);
        }
    }

    /**
     * 向文件追加一个对象
     */
    public void add(T item) {
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw")) {
            randomAccessFile.seek(randomAccessFile.length()); // 跳转到文件末尾
            long position = randomAccessFile.getFilePointer(); // 记录新行的起始位置
            String json = gson.toJson(item); // 序列化为 JSON
            randomAccessFile.writeBytes(json + System.lineSeparator()); // 追加一行
            linePositions.add(position); // 记录新行的起始位置
            size++; // 大小增加
        } catch (IOException e) {
            throw new RuntimeException("Failed to write to file", e);
        }
    }

    /**
     * 从文件中读取指定行的对象（快速访问）
     */
    public T get(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index " + index + " is out of bounds");
        }

        try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r")) {
            long position = linePositions.get(index); // 获取指定行的起始位置
            randomAccessFile.seek(position); // 跳转到指定位置
            String line = randomAccessFile.readLine(); // 读取一行
            return gson.fromJson(line, type); // 反序列化为对象
        } catch (IOException e) {
            throw new RuntimeException("Failed to read from file", e);
        }
    }

    /**
     * 获取文件中的对象总数
     */
    public int size() {
        return size; // 直接返回维护的大小
    }

    /**
     * 清空文件中的所有数据
     */
    public void clear() {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(""); // 清空文件内容
            size = 0; // 大小重置为 0
            linePositions.clear(); // 清空文件位置记录
        } catch (IOException e) {
            throw new RuntimeException("Failed to clear file", e);
        }
    }

    /**
     * 实现 Iterable 接口，返回一个 Iterator
     */
    @Override
    public Iterator<T> iterator() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            return new Iterator<T>() {
                private String nextLine = reader.readLine(); // 读取第一行

                @Override
                public boolean hasNext() {
                    return nextLine != null;
                }

                @Override
                public T next() {
                    try {
                        T item = gson.fromJson(nextLine, type); // 反序列化为对象
                        nextLine = reader.readLine(); // 读取下一行
                        return item;
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to read next line", e);
                    }
                }
            };
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize iterator", e);
        }
    }

    /**
     * 实现 forEach 方法
     */
    @Override
    public void forEach(Consumer<? super T> action) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                T item = gson.fromJson(line, type); // 反序列化为对象
                action.accept(item); // 执行操作
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read from file", e);
        }
    }

    /**
     * 返回一个流，支持 filter、map 等操作
     */
    public Stream<T> stream() {
        Spliterator<T> spliterator = Spliterators.spliteratorUnknownSize(iterator(), Spliterator.ORDERED);
        return StreamSupport.stream(spliterator, false); // 不支持并行流
    }
}
