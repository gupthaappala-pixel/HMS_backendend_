package com.hospital.departments.controller;

import com.hospital.departments.entity.Department;
import com.hospital.departments.entity.Specialization;
import com.hospital.departments.repository.DepartmentRepository;
import com.hospital.departments.repository.SpecializationRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/specializations")
@CrossOrigin(origins = "http://localhost:3000")
public class SpecializationController {

    private final SpecializationRepository specializationRepository;
    private final DepartmentRepository departmentRepository;

    public SpecializationController(SpecializationRepository specializationRepository, DepartmentRepository departmentRepository) {
        this.specializationRepository = specializationRepository;
        this.departmentRepository = departmentRepository;
    }

    @GetMapping("/public")
    public ResponseEntity<List<Specialization>> getAllPublicSpecializations() {
        return ResponseEntity.ok(specializationRepository.findAll());
    }

    @GetMapping("/public/by-department/{deptId}")
    public ResponseEntity<List<Specialization>> getPublicSpecializationsByDepartment(@PathVariable Long deptId) {
        return ResponseEntity.ok(specializationRepository.findByDepartmentId(deptId));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createSpecialization(@RequestBody Map<String, Object> payload) {
        String name = (String) payload.get("name");
        String description = (String) payload.get("description");
        Long departmentId = payload.get("departmentId") != null ? Long.valueOf(payload.get("departmentId").toString()) : null;

        if (name == null || name.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Name is required"));
        }

        Department department = null;
        if (departmentId != null) {
            department = departmentRepository.findById(departmentId).orElse(null);
        }

        if (specializationRepository.findByNameIgnoreCaseAndDepartmentId(name.trim(), departmentId).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Specialization already exists in this department"));
        }

        Specialization spec = new Specialization(name.trim(), description, department);
        Specialization saved = specializationRepository.save(spec);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteSpecialization(@PathVariable Long id) {
        if (!specializationRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        specializationRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Specialization deleted successfully"));
    }

    @PostMapping("/import")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> importSpecializations(@RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
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

                org.apache.poi.ss.usermodel.Cell deptCodeCell = row.getCell(1);
                String deptCode = deptCodeCell != null ? deptCodeCell.getStringCellValue().trim().toUpperCase() : "";
                
                org.apache.poi.ss.usermodel.Cell descCell = row.getCell(2);
                String description = descCell != null ? descCell.getStringCellValue().trim() : "";

                Department department = null;
                if (!deptCode.isEmpty()) {
                    department = departmentRepository.findAll().stream()
                            .filter(d -> d.getCode().equalsIgnoreCase(deptCode))
                            .findFirst().orElse(null);
                }

                Long deptId = department != null ? department.getId() : null;

                boolean exists = specializationRepository.findByNameIgnoreCaseAndDepartmentId(name, deptId).isPresent();
                if (!exists) {
                    Specialization spec = new Specialization(name, description, department);
                    specializationRepository.save(spec);
                }
            }
            return ResponseEntity.ok(Map.of("message", "Specializations successfully imported"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Failed to import specializations: " + e.getMessage()));
        }
    }

    @GetMapping("/export")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportSpecializations() {
        try (org.apache.poi.ss.usermodel.Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
             java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
             
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Specializations");

            org.apache.poi.ss.usermodel.CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
            String[] columns = {"Name", "Department Code", "Description"};
            for (int i = 0; i < columns.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            List<Specialization> specializations = specializationRepository.findAll();
            int rowIdx = 1;
            for (Specialization spec : specializations) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(spec.getName());
                row.createCell(1).setCellValue(spec.getDepartment() != null ? spec.getDepartment().getCode() : "");
                row.createCell(2).setCellValue(spec.getDescription() != null ? spec.getDescription() : "");
            }

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            byte[] data = out.toByteArray();
            return ResponseEntity.ok()
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=specializations.xlsx")
                    .contentType(org.springframework.http.MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(data);
        } catch (Exception e) {
            throw new RuntimeException("Failed to export specializations", e);
        }
    }
}
