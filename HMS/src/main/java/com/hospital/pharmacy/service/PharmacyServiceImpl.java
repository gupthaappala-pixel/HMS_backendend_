package com.hospital.pharmacy.service;

import com.hospital.notifications.service.NotificationService;
import com.hospital.pharmacy.dto.MedicineInventoryRequest;
import com.hospital.pharmacy.dto.MedicineInventoryResponse;
import com.hospital.pharmacy.dto.MedicineRequest;
import com.hospital.pharmacy.dto.MedicineResponse;
import com.hospital.pharmacy.dto.PharmacyInvoiceResponse;
import com.hospital.pharmacy.dto.PharmacyInvoiceItemResponse;
import com.hospital.common.enums.PharmacyInvoicePaymentStatus;
import com.hospital.pharmacy.entity.Medicine;
import com.hospital.pharmacy.entity.MedicineInventory;
import com.hospital.pharmacy.entity.PharmacyInvoice;
import com.hospital.pharmacy.entity.PharmacyInvoiceItem;
import com.hospital.pharmacy.repository.MedicineInventoryRepository;
import com.hospital.pharmacy.repository.MedicineRepository;
import com.hospital.pharmacy.repository.PharmacyInvoiceRepository;
import com.hospital.pharmacy.repository.PharmacyInvoiceItemRepository;
import com.hospital.patients.entity.Patient;
import com.hospital.patients.repository.PatientRepository;
import com.hospital.users.entity.User;
import com.hospital.users.repository.UserRepository;
import com.hospital.doctors.entity.Doctor;
import com.hospital.doctors.repository.DoctorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.apache.poi.ss.usermodel.*;
import java.io.InputStream;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PharmacyServiceImpl implements PharmacyService {

    private final MedicineRepository medicineRepository;
    private final MedicineInventoryRepository medicineInventoryRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final PharmacyInvoiceRepository pharmacyInvoiceRepository;
    private final PharmacyInvoiceItemRepository pharmacyInvoiceItemRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;

    public PharmacyServiceImpl(MedicineRepository medicineRepository,
                               MedicineInventoryRepository medicineInventoryRepository,
                               NotificationService notificationService,
                               UserRepository userRepository,
                               PharmacyInvoiceRepository pharmacyInvoiceRepository,
                               PharmacyInvoiceItemRepository pharmacyInvoiceItemRepository,
                               PatientRepository patientRepository,
                               DoctorRepository doctorRepository) {
        this.medicineRepository = medicineRepository;
        this.medicineInventoryRepository = medicineInventoryRepository;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
        this.pharmacyInvoiceRepository = pharmacyInvoiceRepository;
        this.pharmacyInvoiceItemRepository = pharmacyInvoiceItemRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
    }

    @Override
    @Transactional
    public MedicineResponse createMedicine(MedicineRequest request) {
        if (medicineRepository.existsByCode(request.getCode())) {
            throw new RuntimeException("Medicine code already exists: " + request.getCode());
        }

        Medicine medicine = new Medicine(
                request.getName(),
                request.getCode(),
                request.getCategory(),
                request.getManufacturer(),
                request.getDescription(),
                request.isActive()
        );

        Medicine saved = medicineRepository.save(medicine);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MedicineResponse> getAllMedicines() {
        List<Medicine> allMedicines = medicineRepository.findAll();
        
        // Check if a Doctor is logged in
        org.springframework.security.core.Authentication authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            User user = userRepository.findByUsername(username).orElse(null);
            if (user != null && "ROLE_DOCTOR".equals(user.getRole().name())) {
                Doctor doctor = doctorRepository.findByUserUsername(username).orElse(null);
                if (doctor != null) {
                    String spec = doctor.getSpecialization();
                    String dept = doctor.getDepartment() != null ? doctor.getDepartment().getName() : "";
                    
                    // Filter matching medicines
                    List<Medicine> filtered = allMedicines.stream()
                            .filter(m -> isMedicineRelated(m, spec, dept))
                            .collect(Collectors.toList());
                    
                    // Check if matched medicines contain an Antibiotic
                    boolean containsAntibiotic = filtered.stream().anyMatch(m ->
                            m.getCategory().toLowerCase().contains("antibiotic") ||
                            (m.getDescription() != null && m.getDescription().toLowerCase().contains("antibiotic"))
                    );
                    
                    if (containsAntibiotic) {
                        // Dynamically append Antacids/Gas/PPI medicines
                        List<Medicine> gasTablets = allMedicines.stream()
                                .filter(m -> m.getCategory().toLowerCase().contains("antacid") ||
                                             m.getCategory().toLowerCase().contains("gas") ||
                                             m.getCategory().toLowerCase().contains("gastr") ||
                                             m.getCategory().toLowerCase().contains("ppi") ||
                                             (m.getDescription() != null && (
                                                 m.getDescription().toLowerCase().contains("acidity") ||
                                                 m.getDescription().toLowerCase().contains("antacid") ||
                                                 m.getDescription().toLowerCase().contains("gas relief") ||
                                                 m.getDescription().toLowerCase().contains("gastric") ||
                                                 m.getDescription().toLowerCase().contains("proton pump")
                                             )) ||
                                             m.getName().toLowerCase().contains("pantoprazole") ||
                                             m.getName().toLowerCase().contains("omeprazole") ||
                                             m.getName().toLowerCase().contains("pantocid")
                                )
                                .filter(m -> filtered.stream().noneMatch(fm -> fm.getId().equals(m.getId())))
                                .collect(Collectors.toList());
                        filtered.addAll(gasTablets);
                    }
                    
                    return filtered.stream()
                            .map(this::mapToResponse)
                            .collect(Collectors.toList());
                }
            }
        }
        
        return allMedicines.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private boolean isMedicineRelated(Medicine medicine, String spec, String dept) {
        String s = (spec != null ? spec : "").toLowerCase();
        String d = (dept != null ? dept : "").toLowerCase();
        String name = medicine.getName().toLowerCase();
        String category = medicine.getCategory().toLowerCase();
        String desc = medicine.getDescription() != null ? medicine.getDescription().toLowerCase() : "";

        // 1. General Medicine / General practitioners see everything
        if (s.contains("general") || d.contains("general")) {
            return true;
        }

        // 2. Common/General drug categories that any doctor can prescribe (like fever, pain relief, standard antibiotics)
        if (category.contains("analgesic") || category.contains("antibiotic") || category.contains("nsaid") ||
            desc.contains("fever") || desc.contains("pain relief") || desc.contains("common cold")) {
            return true;
        }

        // 3. Direct string matching with Doctor's specialization or department
        if ((!s.isEmpty() && (name.contains(s) || category.contains(s) || desc.contains(s))) ||
            (!d.isEmpty() && (name.contains(d) || category.contains(d) || desc.contains(d)))) {
            return true;
        }

        // 4. Clinical keyword semantic extensions
        // Cardiology
        if (s.contains("cardio") || d.contains("cardio") || s.contains("heart") || d.contains("heart")) {
            return category.contains("statin") || category.contains("cholesterol") || category.contains("blood pressure") ||
                   desc.contains("cholesterol") || desc.contains("cardiovascular") || desc.contains("blood pressure") ||
                   name.contains("atorvastatin") || name.contains("aspirin") || name.contains("clopidogrel");
        }
        // Pediatrics
        if (s.contains("pediatr") || d.contains("pediatr") || s.contains("child") || d.contains("child")) {
            return desc.contains("child") || desc.contains("infant") || desc.contains("pediatric") ||
                   name.contains("suspension") || name.contains("syrup") || name.contains("pediatric");
        }
        // Neurology
        if (s.contains("neuro") || d.contains("neuro") || s.contains("brain") || d.contains("brain")) {
            return category.contains("statin") || category.contains("seizure") || category.contains("epilepsy") ||
                   desc.contains("brain") || desc.contains("nerve") || desc.contains("seizure");
        }
        // Orthopedics
        if (s.contains("ortho") || d.contains("ortho") || s.contains("bone") || d.contains("bone") || s.contains("joint") || d.contains("joint")) {
            return category.contains("nsaid") || category.contains("pain") || category.contains("analgesic") ||
                   desc.contains("bone") || desc.contains("joint") || desc.contains("muscle") || desc.contains("pain") || desc.contains("inflammation");
        }
        // Dermatology
        if (s.contains("derma") || d.contains("derma") || s.contains("skin") || d.contains("skin")) {
            return category.contains("ointment") || category.contains("cream") || category.contains("topical") ||
                   desc.contains("skin") || desc.contains("rash") || desc.contains("acne") || desc.contains("eczema");
        }

        return false;
    }

    @Override
    @Transactional
    public MedicineResponse updateMedicine(Long id, MedicineRequest request) {
        Medicine medicine = medicineRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Medicine not found with id: " + id));
        
        if (!medicine.getCode().equals(request.getCode()) && medicineRepository.existsByCode(request.getCode())) {
            throw new RuntimeException("Medicine code already exists: " + request.getCode());
        }

        medicine.setName(request.getName());
        medicine.setCode(request.getCode());
        medicine.setCategory(request.getCategory());
        medicine.setManufacturer(request.getManufacturer());
        medicine.setDescription(request.getDescription());
        medicine.setActive(request.isActive());

        Medicine saved = medicineRepository.save(medicine);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public void importMedicinesFromExcel(MultipartFile file) {
        try (InputStream is = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(is)) {
             
            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                // Read Name (Col 0)
                Cell nameCell = row.getCell(0);
                if (nameCell == null) continue;
                String name = nameCell.getStringCellValue().trim();
                if (name.isEmpty()) continue;

                // Read Code (Col 1)
                Cell codeCell = row.getCell(1);
                String code = codeCell != null ? getCellValueAsString(codeCell).trim() : "";
                if (code.isEmpty()) continue;

                // Read Category (Col 2)
                Cell catCell = row.getCell(2);
                String category = catCell != null ? catCell.getStringCellValue().trim() : "General";

                // Read Manufacturer (Col 3)
                Cell manCell = row.getCell(3);
                String manufacturer = manCell != null ? manCell.getStringCellValue().trim() : "Unknown";

                // Read Description (Col 4)
                Cell descCell = row.getCell(4);
                String description = descCell != null ? descCell.getStringCellValue().trim() : "";

                // Find or Create Medicine
                Medicine medicine = medicineRepository.findByCode(code).orElse(null);
                if (medicine == null) {
                    medicine = new Medicine(name, code, category, manufacturer, description, true);
                } else {
                    medicine.setName(name);
                    medicine.setCategory(category);
                    medicine.setManufacturer(manufacturer);
                    medicine.setDescription(description);
                }
                Medicine savedMedicine = medicineRepository.save(medicine);

                // Read Stock Quantity (Col 5)
                Cell stockCell = row.getCell(5);
                double stockQty = stockCell != null ? getNumericCellValue(stockCell) : 0;

                // Read Price Per Unit (Col 6)
                Cell priceCell = row.getCell(6);
                double price = priceCell != null ? getNumericCellValue(priceCell) : 0;

                if (stockQty > 0) {
                    String batchNo = "B-IMPORT-" + code.toUpperCase();
                    boolean batchExists = medicineInventoryRepository.existsByBatchNumber(batchNo);
                    if (!batchExists) {
                        MedicineInventory inventory = new MedicineInventory(
                            savedMedicine,
                            batchNo,
                            LocalDate.now().plusYears(3),
                            (int) stockQty,
                            price > 0 ? price : 10.0,
                            "Excel Import Services",
                            "+1-800-555-IMPORT",
                            20
                        );
                        medicineInventoryRepository.save(inventory);
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to import medicines from Excel: " + e.getMessage(), e);
        }
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        if (cell.getCellType() == CellType.STRING) {
            return cell.getStringCellValue();
        } else if (cell.getCellType() == CellType.NUMERIC) {
            return String.valueOf((int) cell.getNumericCellValue());
        }
        return cell.toString();
    }

    private double getNumericCellValue(Cell cell) {
        if (cell == null) return 0;
        if (cell.getCellType() == CellType.NUMERIC) {
            return cell.getNumericCellValue();
        } else if (cell.getCellType() == CellType.STRING) {
            try {
                return Double.parseDouble(cell.getStringCellValue().trim());
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    @Override
    @Transactional
    public MedicineInventoryResponse addInventory(MedicineInventoryRequest request) {
        Medicine medicine = medicineRepository.findById(request.getMedicineId())
                .orElseThrow(() -> new RuntimeException("Medicine not found: " + request.getMedicineId()));

        MedicineInventory inventory = new MedicineInventory(
                medicine,
                request.getBatchNumber(),
                request.getExpiryDate(),
                request.getStockQuantity(),
                request.getPricePerUnit()
        );

        MedicineInventory saved = medicineInventoryRepository.save(inventory);
        
        // Instant check if newly added batch creates alerts
        if (saved.getStockQuantity() < 20) {
            triggerAlertToStaff("Low Stock Warning", String.format("New batch %s of medicine '%s' is registered with low stock: %d units", 
                    saved.getBatchNumber(), saved.getMedicine().getName(), saved.getStockQuantity()));
        }
        if (saved.getExpiryDate().isBefore(LocalDate.now())) {
            triggerAlertToStaff("Expired Batch Added", String.format("Batch %s of medicine '%s' is expired (Expiry date: %s)", 
                    saved.getBatchNumber(), saved.getMedicine().getName(), saved.getExpiryDate()));
        }

        return mapToInventoryResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MedicineInventoryResponse> getFullInventory() {
        return medicineInventoryRepository.findAll().stream()
                .map(this::mapToInventoryResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MedicineInventoryResponse> getLowStockItems() {
        return medicineInventoryRepository.findLowStock(20).stream()
                .map(this::mapToInventoryResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MedicineInventoryResponse> getExpiredItems() {
        return medicineInventoryRepository.findExpiredBefore(LocalDate.now()).stream()
                .map(this::mapToInventoryResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void checkExpiryAndLowStock() {
        LocalDate today = LocalDate.now();
        List<MedicineInventory> expired = medicineInventoryRepository.findExpiredBefore(today);
        List<MedicineInventory> lowStock = medicineInventoryRepository.findLowStock(20);

        for (MedicineInventory i : expired) {
            String title = "Stock Expiry Warning";
            String message = String.format("Batch %s of drug '%s' expired on %s! Remove from inventory immediately.",
                    i.getBatchNumber(), i.getMedicine().getName(), i.getExpiryDate());
            triggerAlertToStaff(title, message);
        }

        for (MedicineInventory i : lowStock) {
            String title = "Low Stock Alert";
            String message = String.format("Drug '%s' (Batch: %s) stock level is critical. Remaining: %d units.",
                    i.getMedicine().getName(), i.getBatchNumber(), i.getStockQuantity());
            triggerAlertToStaff(title, message);
        }
    }

    private void triggerAlertToStaff(String title, String message) {
        List<User> staff = userRepository.findAll().stream()
                .filter(u -> u.getRole().name().equals("ROLE_PHARMACIST") || u.getRole().name().equals("ROLE_INVENTORY_MANAGER") || u.getRole().name().equals("ROLE_ADMIN"))
                .collect(Collectors.toList());

        for (User u : staff) {
            notificationService.createSystemNotification(u.getUsername(), title, message);
        }
    }

    private MedicineResponse mapToResponse(Medicine m) {
        return new MedicineResponse(
                m.getId(),
                m.getName(),
                m.getCode(),
                m.getCategory(),
                m.getManufacturer(),
                m.getDescription(),
                m.isActive()
        );
    }

    private MedicineInventoryResponse mapToInventoryResponse(MedicineInventory i) {
        return new MedicineInventoryResponse(
                i.getId(),
                i.getMedicine().getId(),
                i.getMedicine().getName(),
                i.getMedicine().getCode(),
                i.getMedicine().getCategory(),
                i.getBatchNumber(),
                i.getExpiryDate(),
                i.getStockQuantity(),
                i.getPricePerUnit()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PharmacyInvoiceResponse getInvoiceByPrescription(Long prescriptionId) {
        PharmacyInvoice invoice = pharmacyInvoiceRepository.findByPrescriptionId(prescriptionId)
                .orElseThrow(() -> new RuntimeException("No invoice found for prescription: " + prescriptionId));
        return mapToInvoiceResponse(invoice);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PharmacyInvoiceResponse> getPatientInvoices(Long patientId) {
        return pharmacyInvoiceRepository.findByPatientIdOrderByCreatedAtDesc(patientId).stream()
                .map(this::mapToInvoiceResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PharmacyInvoiceResponse> getMyInvoices(String username) {
        Patient patient = patientRepository.findByUserUsername(username)
                .orElseThrow(() -> new RuntimeException("Patient profile not found for user: " + username));
        return pharmacyInvoiceRepository.findByPatientIdOrderByCreatedAtDesc(patient.getId()).stream()
                .map(this::mapToInvoiceResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PharmacyInvoiceResponse> getAllInvoices() {
        return pharmacyInvoiceRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::mapToInvoiceResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PharmacyInvoiceResponse updatePaymentStatus(Long invoiceId, PharmacyInvoicePaymentStatus status) {
        PharmacyInvoice invoice = pharmacyInvoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found: " + invoiceId));
        invoice.setPaymentStatus(status);
        PharmacyInvoice saved = pharmacyInvoiceRepository.save(invoice);

        if (status == PharmacyInvoicePaymentStatus.PAID && invoice.getPatient().getUser() != null) {
            notificationService.createSystemNotification(
                    invoice.getPatient().getUser().getUsername(),
                    "Pharmacy Payment Confirmed",
                    String.format("Your pharmacy invoice %s (₹%.2f) has been marked as paid.", invoice.getInvoiceNumber(), invoice.getGrandTotal())
            );
        }

        return mapToInvoiceResponse(saved);
    }

    private PharmacyInvoiceResponse mapToInvoiceResponse(PharmacyInvoice inv) {
        PharmacyInvoiceResponse res = new PharmacyInvoiceResponse();
        res.setId(inv.getId());
        res.setInvoiceNumber(inv.getInvoiceNumber());
        res.setPrescriptionId(inv.getPrescription().getId());
        res.setPatientId(inv.getPatient().getId());
        String patientName = inv.getPatient().getUser() != null ?
                inv.getPatient().getUser().getFirstName() + " " + inv.getPatient().getUser().getLastName() : "Unknown";
        res.setPatientName(patientName);
        res.setSubtotal(inv.getSubtotal());
        res.setDiscountPercent(inv.getDiscountPercent());
        res.setDiscountAmount(inv.getDiscountAmount());
        res.setGstPercent(inv.getGstPercent());
        res.setGstAmount(inv.getGstAmount());
        res.setGrandTotal(inv.getGrandTotal());
        res.setPaymentStatus(inv.getPaymentStatus());
        res.setCreatedAt(inv.getCreatedAt());

        List<PharmacyInvoiceItem> items = pharmacyInvoiceItemRepository.findByPharmacyInvoiceId(inv.getId());
        res.setItems(items.stream().map(item -> new PharmacyInvoiceItemResponse(
                item.getId(),
                item.getMedicineName(),
                item.getMedicineCode(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getTotalPrice()
        )).collect(Collectors.toList()));

        return res;
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportInventoryToExcel() {
        try (Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
             java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
             
            Sheet sheet = workbook.createSheet("Inventory");

            // Header Style
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            // Header Row
            Row headerRow = sheet.createRow(0);
            String[] columns = {"Name", "Code", "Category", "Manufacturer", "Description", "Batch Number", "Stock Quantity", "Price Per Unit", "Expiry Date"};
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data Rows
            List<MedicineInventory> inventory = medicineInventoryRepository.findAll();
            int rowIdx = 1;
            for (MedicineInventory item : inventory) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(item.getMedicine().getName());
                row.createCell(1).setCellValue(item.getMedicine().getCode());
                row.createCell(2).setCellValue(item.getMedicine().getCategory());
                row.createCell(3).setCellValue(item.getMedicine().getManufacturer() != null ? item.getMedicine().getManufacturer() : "");
                row.createCell(4).setCellValue(item.getMedicine().getDescription() != null ? item.getMedicine().getDescription() : "");
                row.createCell(5).setCellValue(item.getBatchNumber());
                row.createCell(6).setCellValue(item.getStockQuantity());
                row.createCell(7).setCellValue(item.getPricePerUnit());
                row.createCell(8).setCellValue(item.getExpiryDate() != null ? item.getExpiryDate().toString() : "");
            }

            // Auto-size columns
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to export inventory to Excel", e);
        }
    }
}
