package com.zjhl.project.controller;

import cn.dev33.satoken.stp.StpUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/file")
public class FileUploadController {

    @Value("${file.upload.path:/file}")
    private String uploadPath;

    /**
     * 文件上传
     */
    @PostMapping("/upload")
    public Map<String, Object> upload(@RequestParam("file") MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }

        if (file.isEmpty()) {
            result.put("code", 400);
            result.put("msg", "文件不能为空");
            return result;
        }

        try {
            // 按日期分目录：/file/2026/06/08/
            String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            String dirPath = uploadPath + File.separator + dateDir;
            File dir = new File(dirPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // 生成唯一文件名
            String originalName = file.getOriginalFilename();
            String ext = "";
            if (originalName != null && originalName.contains(".")) {
                ext = originalName.substring(originalName.lastIndexOf("."));
            }
            String newFileName = UUID.randomUUID().toString().replace("-", "") + ext;

            // 保存文件
            File dest = new File(dir, newFileName);
            file.transferTo(dest);

            // 返回相对路径
            String relativePath = dateDir + "/" + newFileName;
            result.put("code", 200);
            result.put("msg", "上传成功");
            result.put("data", relativePath);
            result.put("originalName", originalName);
            return result;
        } catch (IOException e) {
            result.put("code", 500);
            result.put("msg", "上传失败：" + e.getMessage());
            return result;
        }
    }

    /**
     * 删除文件
     */
    @DeleteMapping("/delete")
    public Map<String, Object> delete(@RequestParam String filePath) {
        Map<String, Object> result = new HashMap<>();
        if (!StpUtil.isLogin()) {
            result.put("code", 401);
            result.put("msg", "未登录");
            return result;
        }

        File file = new File(uploadPath + File.separator + filePath);
        if (file.exists() && file.delete()) {
            result.put("code", 200);
            result.put("msg", "删除成功");
        } else {
            result.put("code", 500);
            result.put("msg", "删除失败");
        }
        return result;
    }

    /**
     * 文件下载
     */
    @GetMapping("/download")
    public void download(@RequestParam String filePath, @RequestParam(required = false) String fileName, HttpServletResponse response) {
        if (!StpUtil.isLogin()) {
            try {
                response.setStatus(401);
                response.getWriter().write("{\"code\":401,\"msg\":\"未登录\"}");
            } catch (IOException ignored) {}
            return;
        }

        File file = new File(uploadPath + File.separator + filePath);
        if (!file.exists()) {
            try {
                response.setStatus(404);
                response.getWriter().write("{\"code\":404,\"msg\":\"文件不存在\"}");
            } catch (IOException ignored) {}
            return;
        }

        try {
            // 确定下载文件名
            String downloadName = (fileName != null && !fileName.isEmpty()) ? fileName : file.getName();
            String encodedName = URLEncoder.encode(downloadName, StandardCharsets.UTF_8.name()).replaceAll("\\+", "%20");

            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + encodedName + "\"; filename*=UTF-8''" + encodedName);
            response.setContentLengthLong(file.length());

            java.nio.file.Files.copy(file.toPath(), response.getOutputStream());
            response.getOutputStream().flush();
        } catch (IOException e) {
            try {
                response.setStatus(500);
                response.getWriter().write("{\"code\":500,\"msg\":\"下载失败\"}");
            } catch (IOException ignored) {}
        }
    }
}
