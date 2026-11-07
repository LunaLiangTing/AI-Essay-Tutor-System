package com.example.controller;

import com.example.common.Result;
import com.example.entity.SysUser;
import com.example.service.StudentService;
import com.example.service.TeacherStudentService;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/teacher/student")
public class TeacherStudentController {

    @Autowired
    private TeacherStudentService teacherStudentService; // 🔥 换成新的 Service

    //导入xlsx
    @PostMapping("/import")
    public Result importStudents(@RequestAttribute("userId") Long teacherId,
                                 @RequestParam("file") MultipartFile file,
                                 @RequestParam("classId") Long classId) {
        try {
            List<Map<String, String>> resultList = teacherStudentService.importStudents(file, classId, teacherId);
            return Result.success("成功导入 " + resultList.size() + " 名学生！", resultList);
        } catch (Exception e) {
            return Result.error("导入失败：" + e.getMessage());
        }
    }

    // 获取班级学生
    @GetMapping("/list/{classId}")
    public Result getStudents(@PathVariable Long classId) {
        try {
            List<SysUser> list = teacherStudentService.getStudentsByClass(classId);
            return Result.success(list);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    // 删除学生
    @DeleteMapping("/{studentId}")
    public Result deleteStudent(@PathVariable Long studentId, @RequestParam Long classId) {
        try {
            teacherStudentService.deleteStudent(studentId, classId);
            return Result.success("删除成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }


    // 🔥 新增：下载 Excel 模板
    @GetMapping("/template")
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        // 1. 设置响应头，告诉浏览器这是一个下载的 Excel 文件
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=student_import_template.xlsx");

        // 2. 创建一个 Excel 工作簿
        try (Workbook workbook = new XSSFWorkbook();
             OutputStream out = response.getOutputStream()) {

            Sheet sheet = workbook.createSheet("学生名单");

            // 3. 创建表头行
            Row headerRow = sheet.createRow(0);
            String[] headers = {"姓名", "性别", "学号"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                // 设置一下列宽，好看一点
                sheet.setColumnWidth(i, 15 * 256);
            }

            // 4. 创建两行示例数据
            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("张三");
            row1.createCell(1).setCellValue("男");
            row1.createCell(2).setCellValue("20230001");

            Row row2 = sheet.createRow(2);
            row2.createCell(0).setCellValue("李四");
            row2.createCell(1).setCellValue("女");
            row2.createCell(2).setCellValue("20230002");

            // 5. 写入输出流
            workbook.write(out);
            out.flush();
        }
    }

    // 🔥 新增：导出班级学生名单（含账号密码）给学生
    @GetMapping("/export/{classId}")
    public void exportStudents(@PathVariable Long classId,
                               @RequestParam(required = false, defaultValue = "学生名单") String className,
                               HttpServletResponse response) throws IOException {
        // 查询该班级的学生
        List<SysUser> students = teacherStudentService.getStudentsByClass(classId);

        // 设置响应头
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        // 处理中文文件名乱码
        String fileName = URLEncoder.encode(className + "_学生账号信息.xlsx", StandardCharsets.UTF_8.name());
        response.setHeader("Content-Disposition", "attachment; filename=" + fileName);

        try (Workbook workbook = new XSSFWorkbook();
             OutputStream out = response.getOutputStream()) {

            Sheet sheet = workbook.createSheet("学生名单");

            // 表头
            Row headerRow = sheet.createRow(0);
            String[] headers = {"姓名", "性别", "学号", "登录账号", "初始密码"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                sheet.setColumnWidth(i, 15 * 256);
            }

            // 填充数据
            int rowIndex = 1;
            for (SysUser student : students) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(student.getName());
                row.createCell(1).setCellValue(student.getGender());
                row.createCell(2).setCellValue(student.getStudentId());
                row.createCell(3).setCellValue(student.getUsername());
                row.createCell(4).setCellValue(student.getInitPassword() != null ? student.getInitPassword() : "未记录");
            }

            workbook.write(out);
            out.flush();
        }
    }
}