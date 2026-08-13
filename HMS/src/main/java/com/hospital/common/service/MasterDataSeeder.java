package com.hospital.common.service;

import com.hospital.appointments.entity.Appointment;
import com.hospital.appointments.repository.AppointmentRepository;
import com.hospital.common.enums.AppointmentStatus;
import com.hospital.common.enums.PatientVisitStatus;
import com.hospital.common.enums.PrescriptionStatus;
import com.hospital.common.enums.Role;
import com.hospital.departments.entity.Department;
import com.hospital.departments.repository.DepartmentRepository;
import com.hospital.doctors.entity.Doctor;
import com.hospital.doctors.repository.DoctorRepository;
import com.hospital.laboratory.entity.LaboratoryTest;
import com.hospital.laboratory.repository.LaboratoryTestRepository;
import com.hospital.medicalrecords.entity.MedicalRecord;
import com.hospital.medicalrecords.repository.MedicalRecordRepository;
import com.hospital.notifications.entity.Notification;
import com.hospital.notifications.repository.NotificationRepository;
import com.hospital.patients.entity.Patient;
import com.hospital.patients.repository.PatientRepository;
import com.hospital.pharmacy.entity.Medicine;
import com.hospital.pharmacy.entity.MedicineInventory;
import com.hospital.pharmacy.repository.MedicineInventoryRepository;
import com.hospital.pharmacy.repository.MedicineRepository;
import com.hospital.prescriptions.entity.Prescription;
import com.hospital.prescriptions.repository.PrescriptionRepository;
import com.hospital.users.entity.User;
import com.hospital.users.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@Order(1)
public class MasterDataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final DepartmentRepository departmentRepository;
    private final MedicineRepository medicineRepository;
    private final MedicineInventoryRepository medicineInventoryRepository;
    private final LaboratoryTestRepository laboratoryTestRepository;
    private final AppointmentRepository appointmentRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final NotificationRepository notificationRepository;
    private final PasswordEncoder passwordEncoder;

    public MasterDataSeeder(UserRepository userRepository,
                            DoctorRepository doctorRepository,
                            PatientRepository patientRepository,
                            DepartmentRepository departmentRepository,
                            MedicineRepository medicineRepository,
                            MedicineInventoryRepository medicineInventoryRepository,
                            LaboratoryTestRepository laboratoryTestRepository,
                            AppointmentRepository appointmentRepository,
                            MedicalRecordRepository medicalRecordRepository,
                            PrescriptionRepository prescriptionRepository,
                            NotificationRepository notificationRepository,
                            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.departmentRepository = departmentRepository;
        this.medicineRepository = medicineRepository;
        this.medicineInventoryRepository = medicineInventoryRepository;
        this.laboratoryTestRepository = laboratoryTestRepository;
        this.appointmentRepository = appointmentRepository;
        this.medicalRecordRepository = medicalRecordRepository;
        this.prescriptionRepository = prescriptionRepository;
        this.notificationRepository = notificationRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        seedUsersDoctorsAndPatients();
        seedMedicinesAndInventory();
        seedLaboratoryTests();
        seedAppointments();
        seedMedicalRecords();
        seedPrescriptions();
        seedNotifications();
    }

    private void seedUsersDoctorsAndPatients() {
        if (userRepository.count() >= 20) {
            return;
        }

        String encodedPassword = passwordEncoder.encode("Password123!");
        List<Department> departments = departmentRepository.findAll();
        Department defaultDept = departments.isEmpty() ? null : departments.get(0);

        // 1. Admins
        createOrGetAdmin("admin1", "admin1@hms.com", encodedPassword, "Super", "Admin");
        createOrGetAdmin("admin2", "admin2@hms.com", encodedPassword, "System", "Manager");
        createOrGetAdmin("admin3", "admin3@hms.com", encodedPassword, "Hospital", "Director");

        // 2. Doctors
        createOrGetDoctor("doctor_ravi", "ravi.k@hms.com", encodedPassword, "Ravi", "Kumar", "Cardiology", 12, defaultDept, "+91 98765 00001", "LIC-DOC-001", 800.0);
        createOrGetDoctor("doctor_kumar", "kumar.s@hms.com", encodedPassword, "Suresh", "Kumar", "General Medicine", 8, defaultDept, "+91 98765 00002", "LIC-DOC-002", 500.0);
        createOrGetDoctor("doctor_smith", "smith.j@hms.com", encodedPassword, "John", "Smith", "Orthopedics", 15, defaultDept, "+91 98765 00003", "LIC-DOC-003", 1000.0);
        createOrGetDoctor("doctor_mehta", "mehta.a@hms.com", encodedPassword, "Ananya", "Mehta", "Neurology", 10, defaultDept, "+91 98765 00004", "LIC-DOC-004", 950.0);
        createOrGetDoctor("doctor_sharma", "sharma.v@hms.com", encodedPassword, "Vikram", "Sharma", "Pediatrics", 7, defaultDept, "+91 98765 00005", "LIC-DOC-005", 600.0);

        // 3. Nurses
        createOrGetStaff("nurse_sarah", "sarah.n@hms.com", encodedPassword, Role.ROLE_NURSE, "Sarah", "Jenkins");
        createOrGetStaff("nurse_priya", "priya.n@hms.com", encodedPassword, Role.ROLE_NURSE, "Priya", "Nair");
        createOrGetStaff("nurse_emily", "emily.w@hms.com", encodedPassword, Role.ROLE_NURSE, "Emily", "Watson");
        createOrGetStaff("nurse_anita", "anita.r@hms.com", encodedPassword, Role.ROLE_NURSE, "Anita", "Roy");

        // 4. Pharmacists
        createOrGetStaff("pharma_john", "john.p@hms.com", encodedPassword, Role.ROLE_PHARMACIST, "John", "Doe");
        createOrGetStaff("pharma_lisa", "lisa.m@hms.com", encodedPassword, Role.ROLE_PHARMACIST, "Lisa", "Ray");
        createOrGetStaff("pharma_vikram", "vikram.p@hms.com", encodedPassword, Role.ROLE_PHARMACIST, "Vikram", "Patel");

        // 5. Lab Technicians
        createOrGetStaff("lab_alex", "alex.t@hms.com", encodedPassword, Role.ROLE_LAB_TECHNICIAN, "Alex", "Morgan");
        createOrGetStaff("lab_trent", "trent.l@hms.com", encodedPassword, Role.ROLE_LAB_TECHNICIAN, "Trent", "Alexander");
        createOrGetStaff("lab_sneha", "sneha.l@hms.com", encodedPassword, Role.ROLE_LAB_TECHNICIAN, "Sneha", "Gupta");

        // 6. Patients
        createOrGetPatient("patient_rahul", "rahul.k@example.com", encodedPassword, "Rahul", "Kumar", LocalDate.of(1992, 5, 14), "Male", "O+", "Bangalore, India", "INS-8801", "+91 99000 11111");
        createOrGetPatient("patient_priya", "priya.s@example.com", encodedPassword, "Priya", "Sharma", LocalDate.of(1998, 8, 22), "Female", "A+", "Delhi, India", "INS-8802", "+91 99000 22222");
        createOrGetPatient("patient_arun", "arun.r@example.com", encodedPassword, "Arun", "Rao", LocalDate.of(1980, 11, 30), "Male", "B+", "Hyderabad, India", "INS-8803", "+91 99000 33333");
        createOrGetPatient("patient_deepa", "deepa.v@example.com", encodedPassword, "Deepa", "Verma", LocalDate.of(1986, 3, 19), "Female", "AB-", "Chennai, India", "INS-8804", "+91 99000 44444");
        createOrGetPatient("patient_karan", "karan.p@example.com", encodedPassword, "Karan", "Patel", LocalDate.of(1974, 9, 5), "Male", "O-", "Mumbai, India", "INS-8805", "+91 99000 55555");
        createOrGetPatient("patient_sneha", "sneha.m@example.com", encodedPassword, "Sneha", "Mohan", LocalDate.of(1995, 1, 12), "Female", "B-", "Kolkata, India", "INS-8806", "+91 99000 66666");

        System.out.println("✅ Seeded 24 Users, Doctors, Nurses, Pharmacists, Technicians, and Patients!");
    }

    private void createOrGetAdmin(String username, String email, String password, String firstName, String lastName) {
        if (!userRepository.existsByUsername(username)) {
            User user = new User(username, email, password, Role.ROLE_ADMIN, firstName, lastName);
            userRepository.save(user);
        }
    }

    private void createOrGetStaff(String username, String email, String password, Role role, String firstName, String lastName) {
        if (!userRepository.existsByUsername(username)) {
            User user = new User(username, email, password, role, firstName, lastName);
            userRepository.save(user);
        }
    }

    private void createOrGetDoctor(String username, String email, String password, String firstName, String lastName,
                                  String specialization, int exp, Department dept, String phone, String license, double fee) {
        if (!userRepository.existsByUsername(username)) {
            User user = new User(username, email, password, Role.ROLE_DOCTOR, firstName, lastName);
            Doctor doctor = new Doctor(user, specialization, exp, dept, phone, license, fee);
            doctorRepository.save(doctor);
        }
    }

    private void createOrGetPatient(String username, String email, String password, String firstName, String lastName,
                                   LocalDate dob, String gender, String blood, String address, String insNo, String emergency) {
        if (!userRepository.existsByUsername(username)) {
            User user = new User(username, email, password, Role.ROLE_PATIENT, firstName, lastName);
            Patient patient = new Patient(user, dob, gender, blood, address, insNo, emergency);
            patientRepository.save(patient);
        }
    }

    private void seedMedicinesAndInventory() {
        if (medicineRepository.count() >= 20) {
            return;
        }

        List<Medicine> newMedicines = new ArrayList<>();
        String[][] medData = {
            {"Paracetamol 500mg", "MED-PAR-500", "Analgesics", "GlobalPharma", "Common pain reliever and fever reducer."},
            {"Amoxicillin 250mg", "MED-AMX-250", "Antibiotics", "BioLabs", "Penicillin antibiotic for bacterial infections."},
            {"Ibuprofen 400mg", "MED-IBU-400", "NSAID", "MediHealth", "Anti-inflammatory pain reliever."},
            {"Atorvastatin 10mg", "MED-ATO-10", "Statins", "Pfizer", "Cholesterol-lowering medication."},
            {"Metformin 500mg", "MED-MET-500", "Antidiabetic", "SunPharma", "Type 2 diabetes blood sugar control."},
            {"Amlodipine 5mg", "MED-AML-5", "Cardiovascular", "Cipla", "Blood pressure management."},
            {"Cetirizine 10mg", "MED-CET-10", "Antihistamine", "DrReddys", "Allergy and hives relief."},
            {"Azithromycin 500mg", "MED-AZI-500", "Antibiotics", "Lupin", "Broad-spectrum macrolide antibiotic."},
            {"Omeprazole 20mg", "MED-OME-20", "Antacid", "Torrent", "Acid reflux and ulcer treatment."},
            {"Losartan 50mg", "MED-LOS-50", "Cardiovascular", "Zydus", "Angiotensin receptor blocker for hypertension."},
            {"Pantoprazole 40mg", "MED-PAN-40", "Antacid", "Alkem", "Gastroesophageal reflux therapy."},
            {"Montelukast 10mg", "MED-MON-10", "Respiratory", "Mankind", "Asthma and allergy prevention."},
            {"Levothyroxine 50mcg", "MED-LEV-50", "Endocrine", "Abbott", "Thyroid hormone replacement."},
            {"Metoprolol 25mg", "MED-MPO-25", "Cardiovascular", "AstraZeneca", "Beta-blocker for blood pressure and angina."},
            {"Ciprofloxacin 500mg", "MED-CIP-500", "Antibiotics", "Bayer", "Fluoroquinolone antibiotic for infections."},
            {"Doxycycline 100mg", "MED-DOX-100", "Antibiotics", "Glenmark", "Tetracycline antibiotic for infections."},
            {"Clopidogrel 75mg", "MED-CLO-75", "Antiplatelet", "Sanofi", "Blood thinner for heart attack prevention."},
            {"Gabapentin 300mg", "MED-GAB-300", "Neurology", "Torrent", "Nerve pain and seizure management."},
            {"Ranitidine 150mg", "MED-RAN-150", "Antacid", "GSK", "Histamine H2 receptor blocker."},
            {"Vitamin D3 60K", "MED-VIT-60K", "Vitamins", "Cadila", "High-potency vitamin supplement."}
        };

        for (String[] m : medData) {
            if (!medicineRepository.existsByCode(m[1])) {
                Medicine med = new Medicine(m[0], m[1], m[2], m[3], m[4], true);
                newMedicines.add(med);
            }
        }

        if (!newMedicines.isEmpty()) {
            List<Medicine> saved = medicineRepository.saveAll(newMedicines);
            int batchIdx = 1;
            for (Medicine med : saved) {
                MedicineInventory inv = new MedicineInventory(
                    med,
                    "BATCH-" + (1000 + batchIdx),
                    LocalDate.now().plusYears(2),
                    50 + (batchIdx * 5),
                    10.0 + (batchIdx * 2.5),
                    "Prime Medical Distributors",
                    "+91 98000 " + String.format("%05d", batchIdx),
                    20
                );
                medicineInventoryRepository.save(inv);
                batchIdx++;
            }
            System.out.println("✅ Seeded 20 Medicines and Inventory batches into HMS database!");
        }
    }

    private void seedLaboratoryTests() {
        if (laboratoryTestRepository.count() >= 20) {
            return;
        }

        String[][] labData = {
            {"Complete Blood Count (CBC)", "LAB-CBC-01", "4.5-11.0 K/uL", "350.0"},
            {"Lipid Profile Panel", "LAB-LIP-02", "< 200 mg/dL Total Cholesterol", "600.0"},
            {"Chest X-Ray PA View", "LAB-XRAY-03", "Clear Lung Fields", "500.0"},
            {"Liver Function Test (LFT)", "LAB-LFT-04", "ALT: 7-56 U/L, AST: 10-40 U/L", "750.0"},
            {"Kidney Function Test (KFT)", "LAB-KFT-05", "Creatinine: 0.7-1.3 mg/dL", "700.0"},
            {"HbA1c Glycated Hemoglobin", "LAB-HBA1C-06", "< 5.7% Normal", "450.0"},
            {"Thyroid Panel (T3, T4, TSH)", "LAB-THY-07", "TSH: 0.4-4.0 mIU/L", "800.0"},
            {"Vitamin D3 Total", "LAB-VITD-08", "30-100 ng/mL", "1200.0"},
            {"Urinalysis Routine", "LAB-URI-09", "Normal Color/Clear", "250.0"},
            {"Fasting Blood Glucose", "LAB-FBG-10", "70-99 mg/dL", "200.0"},
            {"Electrocardiogram (ECG)", "LAB-ECG-11", "Normal Sinus Rhythm", "400.0"},
            {"C-Reactive Protein (CRP)", "LAB-CRP-12", "< 10.0 mg/L", "550.0"},
            {"Erythrocyte Sedimentation Rate", "LAB-ESR-13", "0-20 mm/hr", "300.0"},
            {"Serum Electrolytes (Na, K, Cl)", "LAB-ELE-14", "Na: 135-145 mEq/L", "480.0"},
            {"Serum Creatinine", "LAB-CREAT-15", "0.6-1.2 mg/dL", "320.0"},
            {"Platelet Count Test", "LAB-PLT-16", "150-450 K/uL", "280.0"},
            {"D-Dimer Quantitative", "LAB-DDIM-17", "< 0.50 ug/mL FEU", "1100.0"},
            {"Prothrombin Time (PT/INR)", "LAB-PTINR-18", "INR: 0.8-1.1", "420.0"},
            {"Stool Routine & Microscopy", "LAB-STL-19", "Normal Flora", "260.0"},
            {"Skin Allergy Panel", "LAB-ALL-20", "Negative Panel", "1500.0"}
        };

        for (String[] t : labData) {
            if (!laboratoryTestRepository.existsByTestCode(t[1])) {
                LaboratoryTest test = new LaboratoryTest(t[0], t[1], t[2], Double.parseDouble(t[3]), true);
                laboratoryTestRepository.save(test);
            }
        }
        System.out.println("✅ Seeded 20 Laboratory Test Catalog items into HMS database!");
    }

    private void seedAppointments() {
        if (appointmentRepository.count() >= 20) {
            return;
        }

        List<Patient> patients = patientRepository.findAll();
        List<Doctor> doctors = doctorRepository.findAll();

        if (patients.isEmpty() || doctors.isEmpty()) {
            return;
        }

        for (int i = 0; i < 20; i++) {
            Patient patient = patients.get(i % patients.size());
            Doctor doctor = doctors.get(i % doctors.size());
            LocalDateTime dateTime = LocalDateTime.now().plusDays((i % 5) - 2).withHour(9 + (i % 8)).withMinute(0);

            AppointmentStatus status = AppointmentStatus.SCHEDULED;
            PatientVisitStatus visitStatus = PatientVisitStatus.WAITING;

            if (i % 3 == 0) {
                status = AppointmentStatus.COMPLETED;
                visitStatus = PatientVisitStatus.CLOSED;
            } else if (i % 4 == 0) {
                status = AppointmentStatus.CONFIRMED;
                visitStatus = PatientVisitStatus.UNDER_CONSULTATION;
            }

            Appointment appointment = new Appointment(
                patient,
                doctor,
                dateTime,
                status,
                visitStatus,
                "Routine checkup and consultation #" + (i + 1),
                "Patient reported mild discomfort during follow-up."
            );
            appointmentRepository.save(appointment);
        }

        System.out.println("✅ Seeded 20 Appointment records into HMS database!");
    }

    private void seedMedicalRecords() {
        if (medicalRecordRepository.count() >= 20) {
            return;
        }

        List<Patient> patients = patientRepository.findAll();
        List<Doctor> doctors = doctorRepository.findAll();
        List<Appointment> appointments = appointmentRepository.findAll();

        if (patients.isEmpty() || doctors.isEmpty()) {
            return;
        }

        for (int i = 0; i < 20; i++) {
            Patient patient = patients.get(i % patients.size());
            Doctor doctor = doctors.get(i % doctors.size());
            Appointment appt = (appointments.size() > i) ? appointments.get(i) : null;

            MedicalRecord record = new MedicalRecord(
                patient,
                doctor,
                appt,
                "Clinical Diagnosis #" + (i + 1) + ": Mild hypertension and fatigue",
                "Symptoms reported: Headache, fatigue, mild dizziness",
                "Treatment Plan: Rest, low sodium diet, prescribed Amlodipine 5mg",
                "Penicillin, Dust Allergies",
                "No prior major surgeries",
                "Follow-up scheduled in 14 days",
                LocalDate.now().plusDays(14),
                "Hypertension",
                "COVID-19 Vaccinated, Hepatitis B",
                "None"
            );
            medicalRecordRepository.save(record);
        }
        System.out.println("✅ Seeded 20 MedicalRecord entries into HMS database!");
    }

    private void seedPrescriptions() {
        if (prescriptionRepository.count() >= 20) {
            return;
        }

        List<Patient> patients = patientRepository.findAll();
        List<Doctor> doctors = doctorRepository.findAll();
        List<MedicalRecord> records = medicalRecordRepository.findAll();

        if (patients.isEmpty() || doctors.isEmpty()) {
            return;
        }

        for (int i = 0; i < 20; i++) {
            Patient patient = patients.get(i % patients.size());
            Doctor doctor = doctors.get(i % doctors.size());
            MedicalRecord record = (records.size() > i) ? records.get(i) : null;
            PrescriptionStatus status = (i % 2 == 0) ? PrescriptionStatus.DISPENSED : PrescriptionStatus.PENDING;

            Prescription prescription = new Prescription(patient, doctor, record, status);
            prescriptionRepository.save(prescription);
        }
        System.out.println("✅ Seeded 20 Prescription entries into HMS database!");
    }

    private void seedNotifications() {
        if (notificationRepository.count() >= 20) {
            return;
        }

        List<User> users = userRepository.findAll();
        if (users.isEmpty()) return;

        for (int i = 0; i < 20; i++) {
            User user = users.get(i % users.size());
            Notification notification = new Notification(
                user,
                "System Notification #" + (i + 1),
                "Your appointment / clinical report has been updated successfully.",
                i % 2 == 0
            );
            notificationRepository.save(notification);
        }
        System.out.println("✅ Seeded 20 Notification entries into HMS database!");
    }
}
