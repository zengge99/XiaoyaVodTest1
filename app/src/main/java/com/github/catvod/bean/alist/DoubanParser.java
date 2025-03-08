package com.github.catvod.spider;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.github.catvod.net.OkHttp;

public class DoubanParser {
    
    public static class DoubanInfo {
        private String plot;
        private String year;
        private String region;
        private String actors;
        private String director;
        private String type;

        // Getters 略...
    }

    public static DoubanInfo getDoubanInfo(String id) {
        if (id == null || id.isEmpty()) {
            return new DoubanInfo();
        }

        try {
            String url = "https://movie.douban.com/subject/" + id + "/";
            Document doc = Jsoup.parse(OkHttp.string(url);

            // 解析剧情简介
            String plot = doc.select("#link-report-intra span[property=v:summary]").text().trim();

            // 解析年份
            Element yearElement = doc.selectFirst(".year");
            String year = yearElement != null ? 
                yearElement.text().replaceAll("[()]", "") : "";

            // 解析国家地区
            String region = parseRegion(doc.html());

            // 解析演员列表
            Elements actorElements = doc.select("meta[property^=video:actor]");
            List<String> actorList = new ArrayList<>();
            for (Element el : actorElements) {
                actorList.add(el.attr("content"));
            }
            String actors = String.join("/", actorList);

            // 解析导演
            Element directorElement = doc.selectFirst("meta[property=video:director]");
            String director = directorElement != null ? 
                directorElement.attr("content") : "";

            // 解析类型
            Element genreElement = doc.selectFirst("span.pl:containsOwn(类型:)");
            Elements typeElements = genreElement != null ?
                genreElement.parent().select("span[property=v:genre]") : new Elements();
            List<String> typeList = new ArrayList<>();
            for (Element el : typeElements) {
                typeList.add(el.text());
            }
            String type = String.join("/", typeList);

            // 构建结果对象
            DoubanInfo info = new DoubanInfo();
            info.plot = plot;
            info.year = year;
            info.region = region;
            info.actors = actors;
            info.director = director;
            info.type = type;
            
            return info;
        } catch (IOException e) {
            return new DoubanInfo();
        }
    }

    private static String parseRegion(String html) {
        Pattern pattern = Pattern.compile("<span class=\"pl\">制片国家/地区:</span>[\\s\\n]*([^<]+)");
        Matcher matcher = pattern.matcher(html);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }
}
