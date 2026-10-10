package com.zjhl.project.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MarkInfo 页面（SSR 渲染）内嵌数据解析器（纯 Java 实现，无第三方依赖）。
 *
 * 背景：历史数据接口 /rest/water/getHisData 被 WAF 拦截（真实站名直接 500），
 * 但 MarkInfo 页面是服务端渲染，完整的逐小时历史数据内嵌在 HTML 的
 * window.__NUXT__ 变量里。该变量是 devalue 压缩格式：
 *
 *   (function(a,b,c,...){return {...,pz:[{yl:b,sw:786.7,kr:g,tm:p,zs:b},...],...}}(null,0,800,...))
 *
 * 即：函数体里用形参名引用值，实参列表在末尾，一一对应。
 * 解析思路：形参表 + 实参表建立 变量名 -> 字面量 映射，
 * 再正则提取 pz 数组每一项，把变量替换成真实值。
 */
public class WaterHistoryParser {

    private static final String MARKER = "window.__NUXT__=";

    /** pz 数组单项的固定结构（字段顺序由该站点 SSR 模板决定，实测稳定） */
    private static final Pattern PZ_ITEM = Pattern.compile(
            "\\{yl:([^,]+),sw:([^,]+),kr:([^,]+),tm:([^,]+),zs:([^,}]+)\\}");

    /** 水位历史记录（tm=时间 yyyy-MM-ddTHH:mm:ss，sw=水位，可能为 null） */
    public static class PzRecord {
        public String tm;
        public String sw;
    }

    /**
     * 提取水位逐小时序列（rainList.pz）。解析失败或无数据返回空列表。
     */
    public static List<PzRecord> extractPz(String html) {
        List<PzRecord> result = new ArrayList<>();
        String expr = extractExpr(html);
        if (expr == null) {
            return result;
        }
        Map<String, String> vars = buildVarMap(expr);
        if (vars == null) {
            return result;
        }

        int pzIdx = expr.indexOf("pz:[");
        if (pzIdx < 0) {
            return result;
        }
        Matcher m = PZ_ITEM.matcher(expr);
        m.region(pzIdx, expr.length());
        while (m.find()) {
            PzRecord r = new PzRecord();
            r.sw = resolve(m.group(2), vars);
            r.tm = resolve(m.group(4), vars);
            // tm 必须是时间字符串，否则说明格式变了，停止解析避免脏数据
            if (r.tm == null || !r.tm.contains("T")) {
                break;
            }
            result.add(r);
        }
        return result;
    }

    /**
     * 提取汛限水位（detail[0].xxsw，水库站有，闸站等为空），无则返回 null。
     */
    public static String extractXxsw(String html) {
        String expr = extractExpr(html);
        if (expr == null) {
            return null;
        }
        Map<String, String> vars = buildVarMap(expr);
        if (vars == null) {
            return null;
        }
        Matcher m = Pattern.compile("xxsw:([^,}]+)").matcher(expr);
        if (m.find()) {
            return resolve(m.group(1), vars);
        }
        return null;
    }

    /** 截取 window.__NUXT__= 到 </script> 之间的表达式 */
    private static String extractExpr(String html) {
        if (html == null) {
            return null;
        }
        int i = html.indexOf(MARKER);
        if (i < 0) {
            return null;
        }
        int j = html.indexOf("</script>", i);
        if (j < 0) {
            return null;
        }
        String expr = html.substring(i + MARKER.length(), j).trim();
        if (expr.endsWith(";")) {
            expr = expr.substring(0, expr.length() - 1);
        }
        return expr;
    }

    /**
     * 解析 devalue 结构，建立 形参名 -> 实参字面量 的映射。
     * 结构：(function(a,b,...){return {...}}(arg0,arg1,...))
     */
    private static Map<String, String> buildVarMap(String expr) {
        int fnStart = expr.indexOf("function(");
        int fnEnd = expr.indexOf("){return", fnStart);
        // 实参列表从函数体最后的 "}(" 开始，到末尾的 "))" 结束
        int argsStart = expr.lastIndexOf("}(");
        if (fnStart < 0 || fnEnd < 0 || argsStart < 0) {
            return null;
        }
        String paramsStr = expr.substring(fnStart + "function(".length(), fnEnd);
        String argsStr = expr.substring(argsStart + 2, expr.length() - 2);

        String[] params = paramsStr.split(",");
        List<String> args = splitArgs(argsStr);
        Map<String, String> map = new HashMap<>(params.length * 2);
        for (int k = 0; k < params.length && k < args.size(); k++) {
            map.put(params[k].trim(), args.get(k));
        }
        return map;
    }

    /** 按顶层逗号切分实参列表（字符串内的逗号不切） */
    private static List<String> splitArgs(String s) {
        List<String> parts = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inString = false;
        boolean escape = false;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (inString) {
                cur.append(ch);
                if (escape) {
                    escape = false;
                } else if (ch == '\\') {
                    escape = true;
                } else if (ch == '"') {
                    inString = false;
                }
            } else if (ch == '"') {
                inString = true;
                cur.append(ch);
            } else if (ch == ',') {
                parts.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(ch);
            }
        }
        parts.add(cur.toString());
        return parts;
    }

    /**
     * 把一个 token 解析成真实值：
     * - 变量名 -> 查映射表递归解析
     * - "..." -> 字符串（去引号）
     * - null / void 0 -> null
     * - 其他 -> 数字字面量原样返回
     */
    private static String resolve(String token, Map<String, String> vars) {
        if (token == null) {
            return null;
        }
        token = token.trim();
        if (token.isEmpty() || "null".equals(token) || "void 0".equals(token)) {
            return null;
        }
        if (token.startsWith("\"")) {
            return unquote(token);
        }
        if (token.matches("[A-Za-z_$][A-Za-z0-9_$]*")) {
            String v = vars.get(token);
            return v == null ? null : resolve(v, vars);
        }
        return token; // 数字字面量
    }

    /** 去掉字符串两侧引号并处理常见转义 */
    private static String unquote(String s) {
        if (s.length() < 2) {
            return s;
        }
        String inner = s.substring(1, s.length() - 1);
        return inner.replace("\\\"", "\"").replace("\\\\", "\\");
    }
}
