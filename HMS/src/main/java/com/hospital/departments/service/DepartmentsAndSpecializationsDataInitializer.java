package com.hospital.departments.service;

import com.hospital.departments.entity.Department;
import com.hospital.departments.entity.Specialization;
import com.hospital.departments.repository.DepartmentRepository;
import com.hospital.departments.repository.SpecializationRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class DepartmentsAndSpecializationsDataInitializer implements CommandLineRunner {

    private final DepartmentRepository departmentRepository;
    private final SpecializationRepository specializationRepository;

    public DepartmentsAndSpecializationsDataInitializer(DepartmentRepository departmentRepository,
                                                         SpecializationRepository specializationRepository) {
        this.departmentRepository = departmentRepository;
        this.specializationRepository = specializationRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if (departmentRepository.count() == 0) {
            System.out.println("Seeding default Departments and Specializations...");

            // Create departments
            Department cardiology = new Department("Cardiology", "CARDIOLOGY", "Cardiovascular system diseases treatment and diagnosis");
            Department pediatrics = new Department("Pediatrics", "PEDIATRICS", "Medical care for infants, children, and adolescents");
            Department orthopedics = new Department("Orthopedics", "ORTHOPEDICS", "Skeletal and muscular system correction and treatment");
            Department neurology = new Department("Neurology", "NEUROLOGY", "Nervous system disorders treatment and diagnosis");
            Department medicine = new Department("General Medicine", "GENERAL_MEDICINE", "Primary healthcare and general clinical medicine");
            Department dermatology = new Department("Dermatology", "DERMATOLOGY", "Skin, hair, and nail health treatments");

            List<Department> depts = departmentRepository.saveAll(Arrays.asList(
                    cardiology, pediatrics, orthopedics, neurology, medicine, dermatology
            ));

            Department cardDept = depts.get(0);
            Department pedDept = depts.get(1);
            Department orthDept = depts.get(2);
            Department neurDept = depts.get(3);
            Department medDept = depts.get(4);
            Department dermDept = depts.get(5);

            // Create specializations
            List<Specialization> specs = Arrays.asList(
                    // Cardiology
                    new Specialization("General Cardiology", "General cardiology consultation", cardDept),
                    new Specialization("Interventional Cardiology", "Catheter-based therapies", cardDept),
                    new Specialization("Electrophysiology", "Heart rhythm disorders therapy", cardDept),

                    // Pediatrics
                    new Specialization("General Pediatrics", "General pediatric consultation", pedDept),
                    new Specialization("Pediatric Cardiology", "Children's heart diseases specialist", pedDept),
                    new Specialization("Neonatology", "Newborn infant medical care", pedDept),

                    // Orthopedics
                    new Specialization("Orthopedic Surgery", "Surgical care of musculoskeletal disorders", orthDept),
                    new Specialization("Sports Medicine", "Treatment of athletic injuries", orthDept),

                    // Neurology
                    new Specialization("General Neurology", "Neurological disorders consultation", neurDept),
                    new Specialization("Neurosurgery", "Brain and spinal cord surgery", neurDept),

                    // General Medicine
                    new Specialization("General Practice", "Family medicine and primary care", medDept),
                    new Specialization("Internal Medicine", "Diagnosis and medical treatment of adults", medDept),

                    // Dermatology
                    new Specialization("General Dermatology", "Skin disorders treatment", dermDept),
                    new Specialization("Cosmetic Dermatology", "Aesthetic dermatology procedures", dermDept)
            );

            specializationRepository.saveAll(specs);
            System.out.println("Default Departments and Specializations seeded successfully!");
        }
    }
}
