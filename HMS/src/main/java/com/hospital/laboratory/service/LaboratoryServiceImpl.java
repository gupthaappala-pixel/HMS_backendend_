package com.hospital.laboratory.service;

import com.hospital.common.enums.LaboratoryReportStatus;
import com.hospital.doctors.entity.Doctor;
import com.hospital.doctors.repository.DoctorRepository;
import com.hospital.laboratory.dto.*;
import com.hospital.laboratory.entity.LaboratoryReport;
import com.hospital.laboratory.entity.LaboratoryTest;
import com.hospital.laboratory.repository.LaboratoryReportRepository;
import com.hospital.laboratory.repository.LaboratoryTestRepository;
import com.hospital.notifications.service.NotificationService;
import com.hospital.patients.entity.Patient;
import com.hospital.patients.repository.PatientRepository;
import com.hospital.users.entity.User;
import com.hospital.users.repository.UserRepository;
import com.hospital.users.service.AuditLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LaboratoryServiceImpl implements LaboratoryService {

    private final LaboratoryTestRepository laboratoryTestRepository;
    private final LaboratoryReportRepository laboratoryReportRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;
    private final com.hospital.chatbot.service.ReportZoneAnalyzer reportZoneAnalyzer;

    public LaboratoryServiceImpl(LaboratoryTestRepository laboratoryTestRepository,
                                 LaboratoryReportRepository laboratoryReportRepository,
                                 PatientRepository patientRepository,
                                 DoctorRepository doctorRepository,
                                 UserRepository userRepository,
                                 NotificationService notificationService,
                                 AuditLogService auditLogService,
                                 com.hospital.chatbot.service.ReportZoneAnalyzer reportZoneAnalyzer) {
        this.laboratoryTestRepository = laboratoryTestRepository;
        this.laboratoryReportRepository = laboratoryReportRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
        this.reportZoneAnalyzer = reportZoneAnalyzer;
    }

    @Override
    @Transactional
    public LaboratoryTestResponse createLaboratoryTest(LaboratoryTestRequest request) {
        if (laboratoryTestRepository.existsByTestCode(request.getTestCode())) {
            throw new RuntimeException("Laboratory test code already exists: " + request.getTestCode());
        }

        LaboratoryTest test = new LaboratoryTest(
                request.getTestName(),
                request.getTestCode(),
                request.getReferenceRange(),
                request.getCost(),
                request.isActive()
        );

        LaboratoryTest saved = laboratoryTestRepository.save(test);
        return mapToTestResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LaboratoryTestResponse> getActiveTests() {
        return laboratoryTestRepository.findByIsActive(true).stream()
                .map(this::mapToTestResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<LaboratoryTestResponse> getAllTests() {
        return laboratoryTestRepository.findAll().stream()
                .map(this::mapToTestResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public LaboratoryReportResponse requestLaboratoryReport(LaboratoryReportRequest request) {
        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new RuntimeException("Patient not found: " + request.getPatientId()));

        Doctor doctor = doctorRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new RuntimeException("Doctor not found: " + request.getDoctorId()));

        LaboratoryTest test = laboratoryTestRepository.findById(request.getLabTestId())
                .orElseThrow(() -> new RuntimeException("Laboratory test not found: " + request.getLabTestId()));

        LaboratoryReport report = new LaboratoryReport(
                patient,
                doctor,
                test,
                null, // result value is filled by tech
                null, // comments are filled by tech
                LaboratoryReportStatus.PENDING,
                null // doctor remarks handled by consultation
        );

        LaboratoryReport saved = laboratoryReportRepository.save(report);

        // Notify Laboratory staff that a new test has been requested
        triggerLabTechNotification("New Lab Request", String.format("A new laboratory test '%s' has been requested for patient %s (ID: %d)",
                test.getTestName(), patient.getUser() != null ? patient.getUser().getLastName() : "Unknown", patient.getId()));

        return mapToReportResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LaboratoryReportResponse> getPendingReports() {
        return laboratoryReportRepository.findByStatusOrderByTestDateDesc(LaboratoryReportStatus.PENDING).stream()
                .map(this::mapToReportResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<LaboratoryReportResponse> getPatientReports(Long patientId) {
        return laboratoryReportRepository.findByPatientIdOrderByTestDateDesc(patientId).stream()
                .map(this::mapToReportResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<LaboratoryReportResponse> getMyReports(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        List<LaboratoryReport> list;
        if (user.getRole().name().equals("ROLE_DOCTOR")) {
            list = laboratoryReportRepository.findByDoctorUserIdOrderByTestDateDesc(user.getId());
        } else if (user.getRole().name().equals("ROLE_PATIENT")) {
            list = laboratoryReportRepository.findByPatientUserUsernameOrderByTestDateDesc(username);
        } else {
            list = new ArrayList<>();
        }
        return list.stream().map(this::mapToReportResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public LaboratoryReportResponse updateStatus(Long reportId, com.hospital.common.enums.LaboratoryReportStatus status) {
        LaboratoryReport report = laboratoryReportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Laboratory report not found: " + reportId));
        
        report.setStatus(status);
        LaboratoryReport saved = laboratoryReportRepository.save(report);
        
        // Notify patient
        if (report.getPatient().getUser() != null) {
            String message = "";
            if (status == LaboratoryReportStatus.SAMPLE_COLLECTED) {
                message = "Sample has been collected for your laboratory test: " + report.getLabTest().getTestName();
            } else if (status == LaboratoryReportStatus.PROCESSING) {
                message = "Your laboratory test " + report.getLabTest().getTestName() + " is now processing.";
            }
            if (!message.isEmpty()) {
                notificationService.createSystemNotification(
                        report.getPatient().getUser().getUsername(),
                        "Laboratory Test Status Update",
                        message
                );
            }
        }
        
        return mapToReportResponse(saved);
    }
    
    @Override
    @Transactional
    public LaboratoryReportResponse uploadReport(Long reportId, org.springframework.web.multipart.MultipartFile file, String techRemarks, String resultValue) {
        LaboratoryReport report = laboratoryReportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Laboratory report not found: " + reportId));

        if (report.getStatus() == LaboratoryReportStatus.COMPLETED) {
            throw new RuntimeException("Laboratory report results are already completed.");
        }

        report.setResultValue(resultValue);
        report.setTechRemarks(techRemarks);
        report.setStatus(LaboratoryReportStatus.COMPLETED);
        
        if (file != null && !file.isEmpty()) {
            report.setReportFileUrl("/uploads/lab_reports/" + file.getOriginalFilename());
        }

        // Run AI Clinical Zone Analysis
        com.hospital.chatbot.service.ReportZoneAnalyzer.AnalysisResult analysis = reportZoneAnalyzer.analyze(
                report.getLabTest().getTestName(),
                resultValue,
                report.getLabTest().getReferenceRange(),
                techRemarks
        );
        report.setZoneStatus(analysis.getZoneStatus());
        report.setAiSummary(analysis.getAiSummary());
        report.setHealthMetricsJson(analysis.getHealthMetricsJson());

        LaboratoryReport saved = laboratoryReportRepository.save(report);

        // Audit Log
        String patientName = report.getPatient().getUser() != null ?
                report.getPatient().getUser().getFirstName() + " " + report.getPatient().getUser().getLastName() : "Unknown";
        auditLogService.log(
                "SYSTEM",
                "LAB_RESULT_RECORDED",
                String.format("Uploaded result '%s' [%s ZONE] for test: %s, Patient: %s", resultValue, analysis.getZoneStatus(), report.getLabTest().getTestName(), patientName),
                "0.0.0.0"
        );

        // Notify patient with AI summary and Zone Status
        if (report.getPatient().getUser() != null) {
            String zoneEmoji = "GREEN".equals(analysis.getZoneStatus()) ? "🟢" : ("YELLOW".equals(analysis.getZoneStatus()) ? "🟡" : "🔴");
            notificationService.createSystemNotification(
                    report.getPatient().getUser().getUsername(),
                    "Laboratory Results Completed " + zoneEmoji,
                    String.format("%s. %s", zoneEmoji + " " + analysis.getZoneStatus() + " ZONE: " + report.getLabTest().getTestName(), analysis.getAiSummary())
            );
        }

        // Notify doctor with AI summary and Zone Status
        if (report.getDoctor().getUser() != null) {
            String zoneEmoji = "GREEN".equals(analysis.getZoneStatus()) ? "🟢" : ("YELLOW".equals(analysis.getZoneStatus()) ? "🟡" : "🔴");
            notificationService.createSystemNotification(
                    report.getDoctor().getUser().getUsername(),
                    "Laboratory Results Completed " + zoneEmoji,
                    String.format("Patient %s — %s (%s). %s", patientName, report.getLabTest().getTestName(), zoneEmoji + " " + analysis.getZoneStatus(), analysis.getAiSummary())
            );
        }

        return mapToReportResponse(saved);
    }

    @Override
    @Transactional
    public LaboratoryReportResponse recordResult(Long reportId, LaboratoryResultRequest request) {
        LaboratoryReport report = laboratoryReportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Laboratory report not found: " + reportId));

        if (report.getStatus() != LaboratoryReportStatus.PENDING) {
            throw new RuntimeException("Laboratory report results are already completed.");
        }

        report.setResultValue(request.getResultValue());
        report.setComments(request.getComments());
        report.setStatus(LaboratoryReportStatus.COMPLETED);

        // Run AI Clinical Zone Analysis
        com.hospital.chatbot.service.ReportZoneAnalyzer.AnalysisResult analysis = reportZoneAnalyzer.analyze(
                report.getLabTest().getTestName(),
                request.getResultValue(),
                report.getLabTest().getReferenceRange(),
                request.getComments()
        );
        report.setZoneStatus(analysis.getZoneStatus());
        report.setAiSummary(analysis.getAiSummary());
        report.setHealthMetricsJson(analysis.getHealthMetricsJson());

        LaboratoryReport saved = laboratoryReportRepository.save(report);

        // Audit Log
        String patientName = report.getPatient().getUser() != null ?
                report.getPatient().getUser().getFirstName() + " " + report.getPatient().getUser().getLastName() : "Unknown";
        auditLogService.log(
                "SYSTEM",
                "LAB_RESULT_RECORDED",
                String.format("Recorded result '%s' [%s ZONE] for test: %s, Patient: %s", request.getResultValue(), analysis.getZoneStatus(), report.getLabTest().getTestName(), patientName),
                "0.0.0.0"
        );

        // Notify patient with AI summary and Zone Status
        if (report.getPatient().getUser() != null) {
            String zoneEmoji = "GREEN".equals(analysis.getZoneStatus()) ? "🟢" : ("YELLOW".equals(analysis.getZoneStatus()) ? "🟡" : "🔴");
            notificationService.createSystemNotification(
                    report.getPatient().getUser().getUsername(),
                    "Laboratory Results Completed " + zoneEmoji,
                    String.format("%s %s ZONE: %s. %s", zoneEmoji, analysis.getZoneStatus(), report.getLabTest().getTestName(), analysis.getAiSummary())
            );
        }

        // Notify doctor with AI summary and Zone Status
        if (report.getDoctor().getUser() != null) {
            String zoneEmoji = "GREEN".equals(analysis.getZoneStatus()) ? "🟢" : ("YELLOW".equals(analysis.getZoneStatus()) ? "🟡" : "🔴");
            notificationService.createSystemNotification(
                    report.getDoctor().getUser().getUsername(),
                    "Laboratory Results Completed " + zoneEmoji,
                    String.format("Patient %s — %s (%s). %s", patientName, report.getLabTest().getTestName(), zoneEmoji + " " + analysis.getZoneStatus(), analysis.getAiSummary())
            );
        }

        return mapToReportResponse(saved);
    }

    private void triggerLabTechNotification(String title, String message) {
        List<User> techs = userRepository.findAll().stream()
                .filter(u -> u.getRole().name().equals("ROLE_LAB_TECHNICIAN") || u.getRole().name().equals("ROLE_ADMIN"))
                .collect(Collectors.toList());

        for (User u : techs) {
            notificationService.createSystemNotification(u.getUsername(), title, message);
        }
    }

    private LaboratoryTestResponse mapToTestResponse(LaboratoryTest t) {
        return new LaboratoryTestResponse(
                t.getId(),
                t.getTestName(),
                t.getTestCode(),
                t.getReferenceRange(),
                t.getCost(),
                t.isActive()
        );
    }

    private LaboratoryReportResponse mapToReportResponse(LaboratoryReport r) {
        String patientName = r.getPatient().getUser() != null ?
                r.getPatient().getUser().getFirstName() + " " + r.getPatient().getUser().getLastName() : "Unknown";
        String doctorName = r.getDoctor().getUser() != null ?
                "Dr. " + r.getDoctor().getUser().getFirstName() + " " + r.getDoctor().getUser().getLastName() : "Dr. Unknown";

        return new LaboratoryReportResponse(
                r.getId(),
                r.getPatient().getId(),
                patientName,
                r.getDoctor().getId(),
                doctorName,
                r.getLabTest().getId(),
                r.getLabTest().getTestName(),
                r.getLabTest().getTestCode(),
                r.getLabTest().getReferenceRange(),
                r.getLabTest().getCost(),
                r.getTestDate(),
                r.getResultValue(),
                r.getComments(),
                r.getStatus(),
                r.getDoctorRemarks(),
                r.getTechRemarks(),
                r.getReportFileUrl(),
                r.getZoneStatus(),
                r.getAiSummary(),
                r.getHealthMetricsJson()
        );
    }
}
