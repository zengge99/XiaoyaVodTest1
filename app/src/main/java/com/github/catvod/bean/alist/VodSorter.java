package com.github.catvod.bean.alist;

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


public class VodSorter {
    
    public static class Vod {
        private String vod_remarks;
        // 其他字段...

        public String getVodRemarks() { return vod_remarks; }
        public void setVodRemarks(String r) { vod_remarks = r; }
    }

    public static class FilterParams {
        public String douban;
        public String doubansort;
        public String random;
    }

    // 移除了 isFullList 参数，其逻辑固定为 true
    public static String sortVods(List<Vod> vods, FilterParams fl) {
        // 解析豆瓣评分阈值
        double doubanThreshold = parseDoubleSafe(fl.douban);

        // 1. 过滤评分达标的视频
        List<Vod> filteredVods = vods.stream()
            .filter(vod -> parseDoubanRating(vod.getVodRemarks()) >= doubanThreshold)
            .collect(Collectors.toList());

        // 2. 排序处理
        if (fl.doubansort != null) {
            Comparator<Vod> comparator = Comparator.comparingDouble(
                v -> parseDoubanRating(v.getVodRemarks())
            );
            
            switch (fl.doubansort) {
                case "1":
                    filteredVods.sort(comparator.reversed());
                    break;
                case "2":
                    filteredVods.sort(comparator);
                    break;
            }
        }

        // 3. 随机筛选
        int randomCount = parseIntSafe(fl.random);
        if (randomCount > 0) {
            boolean keepOrder = "1".equals(fl.doubansort) || "2".equals(fl.doubansort);
            filteredVods = getRandomElements(filteredVods, randomCount, keepOrder);
        }

        // 构建结果集（pagecount 固定为 1）
        Map<String, Object> result = new LinkedHashMap<String, Object>() {{
            put("page", 1);
            put("pagecount", 1);  // 原 isFullList=true 的逻辑
            put("list", filteredVods);
        }};
        
        return new Gson().toJson(result);
    }

    // 以下辅助方法保持不变...
    private static double parseDoubanRating(String remarks) {
        if (remarks == null || !remarks.startsWith("豆瓣:")) return 0.0;
        try {
            return Double.parseDouble(remarks.substring(3).trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
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
