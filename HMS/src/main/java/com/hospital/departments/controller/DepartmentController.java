package com.hospital.departments.controller;

import com.hospital.departments.entity.Department;
import com.hospital.departments.repository.DepartmentRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/departments")
@CrossOrigin(origins = "http://localhost:3000")
public class DepartmentController {

    private final DepartmentRepository departmentRepository;

    public DepartmentController(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    @GetMapping("/public")
    public ResponseEntity<List<Department>> getPublicDepartments() {
        return ResponseEntity.ok(departmentRepository.findAll());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createDepartment(@RequestBody Department department) {
        if (department.getCode() == null || department.getCode().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Department code is required"));
        }
        if (departmentRepository.findAll().stream().anyMatch(d -> d.getCode().equalsIgnoreCase(department.getCode()))) {
            return ResponseEntity.badRequest().body(Map.of("message", "Department code already exists"));
        }
        Department saved = departmentRepository.save(department);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateDepartment(@PathVariable Long id, @RequestBody Department departmentDetails) {
        return departmentRepository.findById(id).map(dept -> {
            dept.setName(departmentDetails.getName());
            dept.setCode(departmentDetails.getCode());
            dept.setDescription(departmentDetails.getDescription());
            Department updated = departmentRepository.save(dept);
            return ResponseEntity.ok(updated);
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteDepartment(@PathVariable Long id) {
        if (!departmentRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        departmentRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Department deleted successfully"));
    }

    @PostMapping("/import")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> importDepartments(@RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        try (java.io.InputStream is = file.getInputStream();
             org.apache.poi.ss.usermodel.Workbook workbook = org.apache.poi.ss.usermodel.WorkbookFactory.create(is)) {
             
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                org.apache.poi.ss.usermodel.Row row = sheet.getRow(i);
                if (row == null) continue;

                org.apache.poi.ss.usermodel.Cell nameCell = row.getCell(0);
                if (nameCell == null) continue;
                String name = nameCell.getStringCellValue().trim();
                if (name.isEmpty()) continue;

                org.apache.poi.ss.usermodel.Cell codeCell = row.getCell(1);
                String code = codeCell != null ? codeCell.getStringCellValue().trim().toUpperCase() : "";
                if (code.isEmpty()) continue;

                org.apache.poi.ss.usermodel.Cell descCell = row.getCell(2);
                String description = descCell != null ? descCell.getStringCellValue().trim() : "";

                boolean exists = departmentRepository.findAll().stream().anyMatch(d -> d.getCode().equalsIgnoreCase(code));
                if (!exists) {
                    Department dept = new Department();
                    dept.setName(name);
                    dept.setCode(code);
                    dept.setDescription(description);
                    departmentRepository.save(dept);
                }
            }
            return ResponseEntity.ok(Map.of("message", "Departments successfully imported"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Failed to import departments: " + e.getMessage()));
        }
    }

    @GetMapping("/export")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportDepartments() {
        try (org.apache.poi.ss.usermodel.Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
             java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
             
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Departments");

            org.apache.poi.ss.usermodel.CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
            String[] columns = {"Name", "Code", "Description"};
            for (int i = 0; i < columns.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            List<Department> departments = departmentRepository.findAll();
            int rowIdx = 1;
            for (Department dept : departments) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(dept.getName());
                row.createCell(1).setCellValue(dept.getCode());
                row.createCell(2).setCellValue(dept.getDescription() != null ? dept.getDescription() : "");
            }

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            byte[] data = out.toByteArray();
            return ResponseEntity.ok()
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=departments.xlsx")
                    .contentType(org.springframework.http.MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(data);
        } catch (Exception e) {
            throw new RuntimeException("Failed to export departments", e);
        }
    }
}
