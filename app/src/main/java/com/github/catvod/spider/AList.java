package com.github.catvod.spider;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import com.github.catvod.bean.Class;
import com.github.catvod.bean.Filter;
import com.github.catvod.bean.Result;
import com.github.catvod.bean.Sub;
import com.github.catvod.bean.Vod;
import com.github.catvod.bean.alist.Drive;
import com.github.catvod.bean.alist.Item;
import com.github.catvod.bean.alist.Sorter;
import com.github.catvod.crawler.Spider;
import com.github.catvod.crawler.SpiderDebug;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.Util;

import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.net.URLEncoder;
import java.net.URLDecoder;

public class AList extends Spider {

    private List<Drive> drives;
    private String vodPic;
    private String ext;

    private List<Filter> getFilter() {
        List<Filter> items = new ArrayList<>();
        items.add(new Filter("type", "排序类型", Arrays.asList(new Filter.Value("默认", ""), new Filter.Value("名称", "name"),
                new Filter.Value("大小", "size"), new Filter.Value("修改时间", "date"))));
        items.add(new Filter("order", "排序方式", Arrays.asList(new Filter.Value("默认", ""), new Filter.Value("⬆", "asc"),
                new Filter.Value("⬇", "desc"))));
        return items;
    }

    private void fetchRule() {
        if (drives != null && !drives.isEmpty())
            return;
        if (ext.startsWith("http"))
            ext = OkHttp.string(ext);
        String ext1 = "{\"drives\":" + ext + "}";
        Drive drive = Drive.objectFrom(ext1);
        drives = drive.getDrives();
        // vodPic = drive.getVodPic();
        vodPic = "";
    }

    private Drive getDrive(String name) {
        return drives.get(drives.indexOf(new Drive(name))).check();
    }

    private String post(Drive drive, String url, String param) {
        return post(drive, url, param, true);
    }

    private String post(Drive drive, String url, String param, boolean retry) {
        try {
            Logger.log(url);
            Logger.log(param);
            String urlParams = convertToUrlParams(new JSONObject(param));
            url = url + "?" + urlParams;
            Logger.log(url);
            Logger.log(URLEncoder.encode(url));
            //JSONObject params = convertFromUrlParams("do=gen&thread=0&url=" + URLEncoder.encode(url));
            JSONObject params = new JSONObject();
            params.put("do", "gen");
            params.put("thread", "0");
            params.put("url", url);
            Logger.log(params.toString());
            String response = OkHttp.post("http://127.0.0.1:9988/proxy", params.toString(), drive.getHeader()).getBody();
            Logger.log(response);
            SpiderDebug.log(response);
            if (retry && response.contains("Guest user is disabled") && login(drive))
                return post(drive, url, param, false);
            return response;
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    private String convertToUrlParams(JSONObject params) throws Exception {
        StringBuilder urlParams = new StringBuilder();
        boolean isFirst = true;
        Iterator<String> keys = params.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            if (!isFirst) {
                urlParams.append("&");
            } else {
                isFirst = false;
            }
            String encodedKey = URLEncoder.encode(key);
            String encodedValue = URLEncoder.encode(params.get(key).toString());
            urlParams.append(encodedKey).append("=").append(encodedValue);
        }
        return urlParams.toString();
    }

    private JSONObject convertFromUrlParams(String urlParams) throws Exception {
        JSONObject jsonObject = new JSONObject();
        if (urlParams == null || urlParams.isEmpty()) {
            return jsonObject;
        }

        String[] pairs = urlParams.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            if (keyValue.length == 2) {
                String key = keyValue[0];
                String value = keyValue[1];
                jsonObject.put(key, value);
            }
        }

        return jsonObject;
    }

    private String postBak(Drive drive, String url, String param, boolean retry) {
        String response = OkHttp.post(url, param, drive.getHeader()).getBody();
        SpiderDebug.log(response);
        if (retry && response.contains("Guest user is disabled") && login(drive))
            return post(drive, url, param, false);
        return response;
    }

    @Override
    public void init(Context context, String extend) {
        try {
            ext = extend;
            fetchRule();
        } catch (Exception ignored) {
        }
    }

    @Override
    public String homeContent(boolean filter) throws Exception {
        fetchRule();
        List<Class> classes = new ArrayList<>();
        LinkedHashMap<String, List<Filter>> filters = new LinkedHashMap<>();
        for (Drive drive : drives)
            if (!drive.hidden())
                classes.add(drive.toType());
        for (Class item : classes)
            filters.put(item.getTypeId(), getFilter());
        Logger.log(Result.string(classes, filters));
        return Result.string(classes, filters);
    }

    @Override
    public String categoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend)
            throws Exception {
        String key = tid.contains("/") ? tid.substring(0, tid.indexOf("/")) : tid;
        Drive drive = getDrive(key);
        if (drive.noPoster()) {
            return alistCategoryContent(tid, pg, filter, extend);
        } else {
            return xiaoyaCategoryContent(tid, pg, filter, extend);
        }
    }

    @Override
    public String detailContent(List<String> ids) throws Exception {
        String id = ids.get(0);
        if (id.endsWith("~soulist") || id.endsWith("~playlist")) {
            return listDetailContent(ids);
        }
        return defaultDetailContent(ids);
    }

    @Override
    public String searchContent(String keyword, boolean quick) throws Exception {
        fetchRule();
        List<Vod> list = new ArrayList<>();
        List<Job> jobs = new ArrayList<>();
        ExecutorService executor = Executors.newCachedThreadPool();
        for (Drive drive : drives) {
            if (drive.search()) {
                jobs.add(new Job(drive.check(), keyword));
            }
        }
        for (Future<List<Vod>> future : executor.invokeAll(jobs, 15, TimeUnit.SECONDS))
            list.addAll(future.get());
        Logger.log(Result.string(list));
        return Result.get().vod(list).page().string();
    }

    @Override
    public String playerContent(String flag, String id, List<String> vipFlags) {
        Logger.log(flag);
        Logger.log(id);
        String[] ids = id.split("~~~");
        String url = getDetail(ids[0]).getUrl();
        Logger.log(Result.get().url(url).header(getPlayHeader(url)).subs(getSubs(ids)).string());
        return Result.get().url(url).header(getPlayHeader(url)).subs(getSubs(ids)).string();
    }

    private String defaultDetailContent(List<String> ids) throws Exception {
        Logger.log(ids);
        fetchRule();
        String id = ids.get(0);
        String key = id.contains("/") ? id.substring(0, id.indexOf("/")) : id;
        String name = id.substring(id.lastIndexOf("/") + 1);
        Vod vod = new Vod();
        vod.setVodPlayFrom(key);
        vod.setVodId(id);
        vod.setVodName(name);
        vod.setVodPic(vodPic);
        vod.setVodPlayUrl(name + "$" + id);
        Logger.log(Result.string(vod));
        return Result.string(vod);
    }

    private String listDetailContent(List<String> ids) throws Exception {
        Logger.log(ids);
        fetchRule();
        String id = ids.get(0);
        String key = id.contains("/") ? id.substring(0, id.indexOf("/")) : id;
        String path = id.substring(0, id.lastIndexOf("/"));
        String name = path.substring(path.lastIndexOf("/") + 1);
        Drive drive = getDrive(key);
        StringBuilder from = new StringBuilder();
        StringBuilder url = new StringBuilder();
        if (id.endsWith("~soulist")) {
            walkFolder(drive, path, from, url, true);
        } else {
            walkFolder(drive, path, from, url, false);
        }
        Vod vod = new Vod();
        vod.setVodPlayFrom(from.toString());
        vod.setVodPlayUrl(url.toString());
        vod.setVodId(id);
        vod.setVodName(name);
        vod.setVodPic(vodPic);
        Logger.log(Result.string(vod));
        return Result.string(vod);
    }

    private void walkFolder(Drive drive, String path, StringBuilder from, StringBuilder url, Boolean recursive)
            throws Exception {
        List<Item> items = getList(path, false);
        String name = path.substring(path.lastIndexOf("/") + 1);
        Sorter.sort("name", "asc", items);
        List<String> playUrls = new ArrayList<>();
        Boolean haveFile = false;
        for (Item item : items)
            if (item.isMedia(drive.isNew())) {
                playUrls.add(item.getName() + "$" + item.getVodId(path) + findSubs(path, items));
                haveFile = true;
            }
        if (haveFile) {
            url.append("$$$" + TextUtils.join("#", playUrls));
            from.append("$$$" + name);
        }
        if (recursive) {
            for (Item item : items)
                if (!item.isMedia(drive.isNew())) {
                    walkFolder(drive, item.getPath(), from, url, recursive);
                }
        }
        if (url.indexOf("$$$") == 0) {
            url.delete(0, 3);
            from.delete(0, 3);
        }
    }

    private static Map<String, String> getPlayHeader(String url) {
        try {
            Uri uri = Uri.parse(url);
            Map<String, String> header = new HashMap<>();
            if (uri.getHost().contains("115.com"))
                header.put("User-Agent", Util.CHROME);
            else if (uri.getHost().contains("baidupcs.com"))
                header.put("User-Agent", "pan.baidu.com");
            return header;
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private String xiaoyaCategoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend)
            throws Exception {
        Logger.log(tid);
        fetchRule();
        List<Vod> list = new ArrayList<>();
        List<Job> jobs = new ArrayList<>();
        ExecutorService executor = Executors.newCachedThreadPool();
        String key = tid.contains("/") ? tid.substring(0, tid.indexOf("/")) : tid;
        Drive drive = getDrive(key);
        jobs.add(new Job(drive.check(), drive.getPath()));
        for (Future<List<Vod>> future : executor.invokeAll(jobs, 15, TimeUnit.SECONDS))
            list.addAll(future.get());
        Logger.log(Result.string(list));
        return Result.get().vod(list).page().string();
    }

    private String alistCategoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend)
            throws Exception {
        Logger.log(tid);
        fetchRule();
        String type = extend.containsKey("type") ? extend.get("type") : "";
        String order = extend.containsKey("order") ? extend.get("order") : "";
        List<Item> folders = new ArrayList<>();
        List<Item> files = new ArrayList<>();
        List<Vod> list = new ArrayList<>();

        for (Item item : getList(tid, true)) {
            if (item.isFolder())
                folders.add(item);
            else
                files.add(item);
        }
        if (!TextUtils.isEmpty(type) && !TextUtils.isEmpty(order)) {
            Sorter.sort(type, order, folders);
            Sorter.sort(type, order, files);
        }

        Vod playlistVod = null;
        if (files.size() > 0) {
            String remark = String.format("共 %d 集", files.size());
            playlistVod = new Vod(tid + "/~playlist", "播放列表", "", remark, false);
            list.add(playlistVod);
        }

        for (Item item : folders)
            list.add(item.getVod(tid, vodPic));
        for (Item item : files)
            list.add(item.getVod(tid, vodPic));
        Logger.log(Result.get().vod(list).page().string());
        return Result.get().vod(list).page().string();
    }

    private boolean login(Drive drive) {
        try {
            JSONObject params = new JSONObject();
            params.put("username", drive.getLogin().getUsername());
            params.put("password", drive.getLogin().getPassword());
            String response = OkHttp.post(drive.loginApi(), params.toString());
            drive.setToken(new JSONObject(response).getJSONObject("data").getString("token"));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private Item getDetail(String id) {
        try {
            String key = id.contains("/") ? id.substring(0, id.indexOf("/")) : id;
            String path = id.contains("/") ? id.substring(id.indexOf("/")) : "";
            Drive drive = getDrive(key);
            path = path.startsWith(drive.getPath()) ? path : drive.getPath() + path;
            JSONObject params = new JSONObject();
            params.put("path", path);
            params.put("password", drive.findPass(path));
            String response = post(drive, drive.getApi(), params.toString());
            return Item.objectFrom(getDetailJson(drive.isNew(), response));
        } catch (Exception e) {
            return new Item();
        }
    }

    private List<Item> getList(String id, boolean filter) {
        try {
            String key = id.contains("/") ? id.substring(0, id.indexOf("/")) : id;
            String path = id.contains("/") ? id.substring(id.indexOf("/")) : "";
            Drive drive = getDrive(key);
            path = path.startsWith(drive.getPath()) ? path : drive.getPath() + path;
            JSONObject params = new JSONObject();
            params.put("path", path);
            params.put("password", drive.findPass(path));
            String response = post(drive, drive.listApi(), params.toString());
            List<Item> items = Item.arrayFrom(getListJson(drive.isNew(), response));
            Iterator<Item> iterator = items.iterator();
            if (filter)
                while (iterator.hasNext())
                    if (iterator.next().ignore(drive.isNew()))
                        iterator.remove();
            return items;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private String getListJson(boolean isNew, String response) throws Exception {
        if (isNew) {
            return new JSONObject(response).getJSONObject("data").getJSONArray("content").toString();
        } else {
            return new JSONObject(response).getJSONObject("data").getJSONArray("files").toString();
        }
    }

    private String getDetailJson(boolean isNew, String response) throws Exception {
        if (isNew) {
            return new JSONObject(response).getJSONObject("data").toString();
        } else {
            return new JSONObject(response).getJSONObject("data").getJSONArray("files").getJSONObject(0).toString();
        }
    }

    private String getSearchJson(boolean isNew, String response) throws Exception {
        if (isNew) {
            return new JSONObject(response).getJSONObject("data").getJSONArray("content").toString();
        } else {
            return new JSONObject(response).getJSONArray("data").toString();
        }
    }

    private String findSubs(String path, List<Item> items) {
        StringBuilder sb = new StringBuilder();
        for (Item item : items)
            if (Util.isSub(item.getExt()))
                sb.append("~~~").append(item.getName()).append("@@@").append(item.getExt()).append("@@@")
                        .append(item.getVodId(path));
        return sb.toString();
    }

    private List<Sub> getSubs(String[] ids) {
        List<Sub> sub = new ArrayList<>();
        for (String text : ids) {
            if (!text.contains("@@@"))
                continue;
            String[] split = text.split("@@@");
            String name = split[0];
            String ext = split[1];
            String url = getDetail(split[2]).getUrl();
            sub.add(Sub.create().name(name).ext(ext).url(url));
        }
        return sub;
    }

    class Job implements Callable<List<Vod>> {

        private final Drive drive;
        private final String keyword;

        public Job(Drive drive, String keyword) {
            this.drive = drive;
            this.keyword = keyword;
        }

        @Override
        public List<Vod> call() {
            // List<Vod> alist = alist();
            // return alist.size() > 0 ? alist : xiaoya();
            // 魔改：只有小雅才支持搜索
            return xiaoya();
        }

        private List<Vod> xiaoya() {
            List<Vod> list = new ArrayList<>();
            List<Vod> noPicList = new ArrayList<>();
            Document doc = Jsoup.parse(OkHttp.string(drive.searchApi(keyword)));
            for (Element a : doc.select("ul > a")) {
                String[] splits = a.text().split("#");
                if (!splits[0].contains("/"))
                    continue;
                int index = splits[0].lastIndexOf("/");
                boolean file = Util.isMedia(splits[0]);
                if (splits[0].endsWith("/")) {
                    file = false;
                    splits[0] = splits[0].substring(0, index);
                    index = splits[0].lastIndexOf("/");
                }
                Item item = new Item();
                // item.setType(file ? 0 : 1);
                item.setType(0); // 海报模式总是认为是文件模式，直接点击播放
                item.setThumb(splits.length >= 5 ? splits[4] : "");
                item.setPath("/" + splits[0].substring(0, index));
                item.setName(splits[0].substring(index + 1));
                if (item.getPath().startsWith(drive.getPath())) {
                    Vod vod = item.getVod(drive, vodPic);
                    if (!file) {
                        vod.setVodId(drive.getName() + item.getPath() + "/" + item.getName() + "/~soulist");
                    }
                    if (TextUtils.isEmpty(item.getThumb())) {
                        noPicList.add(vod);
                    } else {
                        list.add(vod);
                    }
                }
            }
            list.addAll(noPicList);
            return list;
        }

        private List<Vod> alist() {
            try {
                List<Vod> list = new ArrayList<>();
                String response = post(drive, drive.searchApi(), drive.params(keyword));
                List<Item> items = Item.arrayFrom(getSearchJson(drive.isNew(), response));
                for (Item item : items)
                    if (!item.ignore(drive.isNew()))
                        list.add(item.getVod(drive, vodPic));
                return list;
            } catch (Exception e) {
                return Collections.emptyList();
            }
        }
    }
}
