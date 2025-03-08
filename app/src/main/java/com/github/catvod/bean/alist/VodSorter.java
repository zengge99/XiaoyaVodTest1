package com.github.catvod.bean.alist;

import java.util.*;
import java.util.stream.Collectors;
import android.text.TextUtils;
import com.github.catvod.bean.Vod;
import com.github.catvod.utils.Util;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import com.github.catvod.spider.Logger;


public class VodSorter {

    public static class FilterParams {
        public String douban;
        public String doubansort;
        public String random;
    }

    public static List<Vod> sortVods(List<Vod> vods, HashMap<String, String> fl) {
        // 解析豆瓣评分阈值（从 HashMap 获取）
        double doubanThreshold = parseDoubleSafe(fl.getOrDefault("douban", "0"));
        // 1. 过滤评分达标的视频
        List<Vod> filteredVods = vods.stream()
            .filter(vod -> parseDoubleSafe(vod.doubanInfo.getRating()) >= doubanThreshold)
            .collect(Collectors.toList());

        // 2. 排序处理（从 HashMap 获取排序类型）
        String sortType = fl.getOrDefault("doubansort", "0");
        if (!"0".equals(sortType)) {
            Comparator<Vod> comparator = Comparator.comparingDouble(
                v -> parseDoubleSafe(v.doubanInfo.getRating())
            );
            
            switch (sortType) {
                case "1":
                    filteredVods.sort(comparator.reversed()); // 降序
                    break;
                case "2":
                    filteredVods.sort(comparator); // 升序
                    break;
            }
        }

        // 3. 随机筛选（从 HashMap 获取随机数量）
        int randomCount = parseIntSafe(fl.getOrDefault("random", "0"));
        if (randomCount > 0) {
            boolean keepOrder = "1".equals(sortType) || "2".equals(sortType);
            filteredVods = getRandomElements(filteredVods, randomCount, keepOrder);
        }
        
        return filteredVods;
    }

    private static double parseDoubleSafe(String s) {
        try {
            return s != null ? Double.parseDouble(s) : 0.0;
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private static int parseIntSafe(String s) {
        try {
            return s != null ? Integer.parseInt(s) : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static List<Vod> getRandomElements(List<Vod> source, int count, boolean keepOrder) {
        if (source.size() <= count) {
            List<Vod> result = new ArrayList<>(source);
            if (!keepOrder) Collections.shuffle(result);
            return result;
        }

        List<Vod> shuffled = new ArrayList<>(source);
        Collections.shuffle(shuffled);
        List<Vod> randomSelection = shuffled.subList(0, count);
        
        if (keepOrder) {
            randomSelection.sort(Comparator.comparingInt(source::indexOf));
        }
        return randomSelection;
    }
}
