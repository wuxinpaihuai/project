package com.zjhl.project.util;

import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;

/**
 * 建设项目环境影响评价审批公示公告扫描工具。
 * <p>
 * 启动前请修改 {@link #TARGET_URL} 和 {@link #TARGET_CONTENT} 为实际要监控的网址和内容。
 * 程序每 10 分钟扫描一次目标页面，一旦页面文本包含指定内容，即在控制台打印“公示已出”并退出。
 *
 * @author zjhl
 */
public class PublicNoticeScanner {

    /**
     * 要扫描的建设项目环境影响评价审批公示网站地址。
     * 启动前请替换为实际网址。
     */
    private static final String TARGET_URL = "https://sthjj.zjtz.gov.cn/col/col1229896756/index.html";

    /**
     * 需要在公示公告中查找的指定内容。
     * 启动前请替换为实际要监控的内容关键字。
     */
    private static final String TARGET_CONTENT = "蓝色";

    /**
     * 扫描间隔，单位：分钟。
     */
    private static final long SCAN_INTERVAL_MINUTES = 10L;

    /**
     * 请求超时时间，单位：毫秒。
     */
    private static final int TIMEOUT_MS = 30_000;

    /**
     * 模拟浏览器请求头。
     */
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36";

    /**
     * 用于匹配浙江省 CMS 动态加载脚本中的 API 地址与参数。
     */
    private static final Pattern UNIT_BUILD_SCRIPT_PATTERN = Pattern.compile(
            "<script[^>]+?src=\"[^\"]*unitbuild\\.js[^\"]*\"[^>]*?\\s+url=\"([^\"]+)\"[^>]*?\\s+queryData=\"([^\"]+)\"",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    public static void main(String[] args) {
        System.out.println("公示扫描器已启动...");
        System.out.println("目标网址：" + TARGET_URL);
        System.out.println("监控内容：" + TARGET_CONTENT);
        System.out.println("扫描间隔：" + SCAN_INTERVAL_MINUTES + " 分钟");

        // 单线程调度，便于扫描结束后直接关闭
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "public-notice-scanner");
            t.setDaemon(false);
            return t;
        });

        Runnable scanTask = new Runnable() {
            @Override
            public void run() {
                try {
                    System.out.println("正在扫描：" + TARGET_URL);
                    String html = fetchHtml(TARGET_URL);

                    if (html == null || html.trim().isEmpty()) {
                        System.out.println("未获取到页面内容，" + SCAN_INTERVAL_MINUTES + " 分钟后将再次扫描。");
                        return;
                    }

                    System.out.println("页面大小：" + html.length() + " 字符");

                    // 先尝试直接匹配静态页面内容
                    String text = extractText(html);
                    boolean found = text.contains(TARGET_CONTENT);

                    // 未命中且页面包含 CMS 动态脚本时，尝试调用接口获取真实列表
                    if (!found) {
                        String dynamicContent = fetchDynamicContent(TARGET_URL, html);
                        if (dynamicContent != null && !dynamicContent.trim().isEmpty()) {
                            System.out.println("动态接口内容大小：" + dynamicContent.length() + " 字符");
                            text = extractText(dynamicContent);
                            System.out.println( text );
                            
                            found = text.contains(TARGET_CONTENT);
                        }
                    }

                    if (found) {
                        System.out.println("公示已出");
                        scheduler.shutdown();
                        System.exit(0);
                    } else {
                        System.out.println("暂未发现目标内容，" + SCAN_INTERVAL_MINUTES + " 分钟后将再次扫描。");
                    }
                } catch (Exception e) {
                    System.err.println("扫描异常：" + e.getMessage());
                    e.printStackTrace();
                }
            }
        };

        // 首次立即执行，之后按设定间隔执行
        scheduler.scheduleAtFixedRate(scanTask, 0L, SCAN_INTERVAL_MINUTES, TimeUnit.MINUTES);
    }

    /**
     * 使用浏览器请求头获取页面 HTML。
     */
    private static String fetchHtml(String url) {
        try (HttpResponse response = HttpRequest.get(url)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                .timeout(TIMEOUT_MS)
                .execute()) {
            return response.body();
        }
    }

    /**
     * 从页面 HTML 中提取 CMS 动态加载参数，并请求接口获取真实列表内容。
     *
     * @param pageUrl 原页面地址，用于拼接相对路径
     * @param html    原页面 HTML
     * @return 接口返回内容（JSON 中的 html 字段）；未匹配到动态脚本时返回 null
     */
    private static String fetchDynamicContent(String pageUrl, String html) {
        Matcher matcher = UNIT_BUILD_SCRIPT_PATTERN.matcher(html);
        if (!matcher.find()) {
            System.out.println("未检测到动态加载脚本，按静态页面处理。");
            return null;
        }

        String apiPath = matcher.group(1);
        String queryData = matcher.group(2);

        // 将 queryData 中的单引号替换为双引号，便于 JSON 解析
        String jsonParams = queryData.replace("'", "\"");

        String apiUrl;
        try {
            URL base = new URL(pageUrl);
            apiUrl = new URL(base, apiPath).toString();
        } catch (Exception e) {
            apiUrl = apiPath;
        }

        System.out.println("检测到动态接口：" + apiUrl);

        // 拼接查询参数
        StringBuilder fullUrl = new StringBuilder(apiUrl);
        if (fullUrl.indexOf("?") < 0) {
            fullUrl.append("?");
        } else {
            fullUrl.append("&");
        }

        JSONUtil.parseObj(jsonParams).forEach((key, value) -> {
            try {
                fullUrl.append(java.net.URLEncoder.encode(key, "UTF-8")).append("=");
                if (value != null) {
                    fullUrl.append(java.net.URLEncoder.encode(value.toString(), "UTF-8"));
                }
                fullUrl.append("&");
            } catch (Exception e) {
                System.err.println("参数编码失败：" + key + "=" + value);
            }
        });

        // 移除末尾多余的 &
        if (fullUrl.charAt(fullUrl.length() - 1) == '&') {
            fullUrl.setLength(fullUrl.length() - 1);
        }

        try (HttpResponse response = HttpRequest.get(fullUrl.toString())
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json, text/javascript, */*; q=0.01")
                .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                .header("Referer", pageUrl)
                .header("X-Requested-With", "XMLHttpRequest")
                .timeout(TIMEOUT_MS)
                .execute()) {
            String body = response.body();
            if (body == null || body.trim().isEmpty()) {
                return null;
            }
            // 接口返回 JSON，html 字段中包含真实列表 HTML
            if (body.trim().startsWith("{")) {
                return JSONUtil.parseObj(body).getJSONObject("data").getStr("html");
            }
            return body;
        }
    }

    /**
     * 去除 HTML 标签与多余空白，便于文本匹配。
     * <p>
     * 由于部分政府网站列表仅把完整标题放在 {@code title} 属性中，
     * 页面可见文本会被截断，因此先把所有 {@code title="..."} 提取出来参与匹配。
     */
    private static String extractText(String html) {
        StringBuilder sb = new StringBuilder();

        // 提取 title 属性中的完整文本
        Matcher titleMatcher = Pattern.compile("title=\"([^\"]+)\"").matcher(html);
        while (titleMatcher.find()) {
            sb.append(titleMatcher.group(1));
        }

        // 再去掉 HTML 标签，保留可见文本
        String visibleText = html.replaceAll("<[^>]+>", "");
        sb.append(visibleText);

        return sb.toString()
                .replaceAll("\\s+", "")
                .trim();
    }
}
