package com.hospital.departments.repository;

import com.hospital.departments.entity.Specialization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpecializationRepository extends JpaRepository<Specialization, Long> {
    List<Specialization> findByDepartmentId(Long departmentId);
    Optional<Specialization> findByNameIgnoreCaseAndDepartmentId(String name, Long departmentId);
    Optional<Specialization> findByNameIgnoreCase(String name);
}
