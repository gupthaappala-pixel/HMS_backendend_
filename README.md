# 🏥 Hospital Management System (HMS) — Spring Boot Backend API

![Spring Boot](https://img.shields.io/badge/Spring--Boot-3.x%20%2F%204.x-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Java](https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Supabase-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring--Security-JWT-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)
![AWS](https://img.shields.io/badge/AWS-Elastic_Beanstalk_%2F_EC2-232F3E?style=for-the-badge&logo=amazon-aws&logoColor=white)
![Build](https://img.shields.io/badge/Maven-Passing-000000?style=for-the-badge&logo=apachemaven&logoColor=white)

An enterprise-grade, RESTful backend API for the **Hospital Management System (HMS)** built with Java 17, Spring Boot, Spring Security (JWT authentication), Hibernate/JPA, and PostgreSQL/Supabase.

---

## 📋 Table of Contents
- [Architecture Overview](#-architecture-overview)
- [Key Features](#-key-features)
- [Technology Stack](#-technology-stack)
- [Database & Automated Data Seeder](#-database--automated-data-seeder)
- [Project Directory Structure](#-project-directory-structure)
- [Getting Started](#-getting-started)
  - [Prerequisites](#prerequisites)
  - [Environment Configuration](#environment-configuration)
  - [Local Development](#local-development)
  - [Running Unit Tests](#running-unit-tests)
- [AWS Cloud Deployment](#-aws-cloud-deployment)
- [API Endpoints Summary](#-api-endpoints-summary)
- [Security & Role-Based Access Control](#-security--role-based-access-control)

---

## 🏛️ Architecture Overview

The HMS Backend is engineered around domain-driven micro-packages with clean layer separation (Controllers, Services, Repositories, Entities, DTOs, and Enums).

```
                      Client Applications (React / Mobile / AI)
                                          │
                                 HTTPS / REST / WebSockets
                                          │
                                  ┌───────┴───────┐
                                  │ Security &    │
                                  │ JWT Filter    │
                                  └───────┬───────┘
                                          │
    ┌────────────────┬────────────────────┼───────────────────┬────────────────┐
    │                │                    │                   │                │
┌───▼────┐      ┌────▼───┐          ┌─────▼─────┐       ┌─────▼─────┐    ┌─────▼─────┐
│ Auth & │      │ EMR &  │          │ Clinical  │       │ Pharmacy  │    │ Billing & │
│ Users  │      │ Doctor │          │ Services  │       │ Inventory │    │ Analytics │
└───┬────┘      └────┬───┘          └─────┬─────┘       └─────┬─────┘    └─────┬─────┘
    │                │                    │                   │                │
    └────────────────┴────────────────────┼───────────────────┴────────────────┘
                                          │
                               Spring Data JPA / Hibernate
                                          │
                              PostgreSQL Database (Supabase)
```

---

## ✨ Key Features

- 🔐 **Role-Based Access Control (RBAC):** JWT authentication supporting 6 granular roles (`ROLE_ADMIN`, `ROLE_DOCTOR`, `ROLE_NURSE`, `ROLE_PHARMACIST`, `ROLE_LAB_TECHNICIAN`, `ROLE_PATIENT`).
- 👨‍⚕️ **Doctor & Staff Management:** Doctor schedules, specializations, department mappings, license verification, and consultation fees.
- 🏥 **Patient EMR & Clinical Records:** Complete patient electronic medical records, clinical diagnoses, reported symptoms, treatment plans, allergies, and step-by-step history.
- 📅 **Appointment Management:** Real-time scheduling, queue status (`WAITING`, `UNDER_CONSULTATION`, `CLOSED`), and doctor workload balancing.
- 💊 **Pharmacy & Inventory Control:** Medicine cataloging, batch tracking, stock quantity monitoring, low-stock reorder thresholds, and prescription dispensing.
- 🧪 **Laboratory Operations:** Diagnostic test catalog, reference ranges, specimen processing, report generation, and medical record linking.
- 🧾 **Billing & Financial Invoicing:** Itemized invoice generation, consultation charges, lab/pharmacy billing, tax calculations, and payment tracking.
- 🔔 **Notifications & Audit Logging:** Event notifications, audit trails for diagnostic updates, payments, and closed appointments.
- ⚡ **Auto Master Data Seeder:** Zero-setup data initializer seeding 24 accounts and 20+ records across all 16 system schemas.

---

## 🛠️ Technology Stack

| Layer | Technology |
| :--- | :--- |
| **Framework** | Spring Boot 3.x / 4.x |
| **Language** | Java 17 (Eclipse Temurin) |
| **Security** | Spring Security, JSON Web Tokens (JWT), BCrypt Hashing |
| **Persistence** | Spring Data JPA, Hibernate, Liquibase/Flyway |
| **Database** | PostgreSQL (Production/Supabase), H2 In-Memory (Test Profile) |
| **Build Tool** | Apache Maven |
| **Cloud Provider** | Amazon Web Services (AWS) |
| **Cloud Services** | AWS Elastic Beanstalk / AWS EC2, AWS RDS / Supabase |

---

## 🗄️ Database & Automated Data Seeder

Upon startup, the embedded [`MasterDataSeeder.java`](file:///d:/HMS/Backend/HMS/src/main/java/com/hospital/common/service/MasterDataSeeder.java) automatically populates the database schemas whenever tables are uninitialized.

### Seeded Datasets:
- **`users` (24 Accounts):** 3 Admins, 5 Doctors, 4 Nurses, 3 Pharmacists, 3 Lab Technicians, 6 Patients.
- **Default Universal Password:** `Password123!`
- **`doctors` (5 Profiles):** Cardiology, General Medicine, Orthopedics, Neurology, Pediatrics.
- **`patients` (6 Profiles):** Full demographic records, blood groups (`O+`, `A+`, `B+`, `AB-`, `O-`, `B-`), insurance numbers.
- **`appointments` (20 Records):** Pre-scheduled & completed appointments with visit statuses.
- **`medical_records` (20 Records):** Diagnoses, symptoms, treatment plans, allergies, and follow-up dates.
- **`laboratory_tests` (20 Catalog Items):** CBC, Lipid Panel, Chest X-Ray, LFT, KFT, HbA1c, Thyroid, Vit D, ECG, CRP, etc.
- **`medicines` & `medicine_inventory` (20 Items):** Paracetamol, Amoxicillin, Ibuprofen, Atorvastatin, Metformin, Amlodipine, Cetirizine, etc.

---

## 📁 Project Directory Structure

```
HMS/
├── pom.xml                           # Maven dependencies & build config
├── mvnw / mvnw.cmd                   # Maven wrapper executables
└── src/
    ├── main/
    │   ├── java/com/hospital/
    │   │   ├── HospitalApplication.java   # Spring Boot Main Entrypoint
    │   │   ├── appointments/          # Appointment entity, controller, service, repo
    │   │   ├── auth/                  # Login, Register, JWT authentication, tokens
    │   │   ├── billing/               # Invoices, Payments, Insurance claims
    │   │   ├── chatbot/               # AI Assistant session & messaging
    │   │   ├── common/                # Shared Enums, DTOs, MasterDataSeeder
    │   │   ├── config/                # SecurityConfig, CORS, PasswordEncoder
    │   │   ├── dashboard/             # Hospital admin KPI analytics
    │   │   ├── departments/           # Clinical departments & specializations
    │   │   ├── doctors/               # Doctor profiles & availability schedules
    │   │   ├── laboratory/            # Lab tests catalog & diagnostic reports
    │   │   ├── medicalrecords/        # Clinical EMR & consultation history
    │   │   ├── notifications/         # Real-time alert notifications
    │   │   ├── nurses/                # Nurse staff profiles
    │   │   ├── patients/              # Patient profiles & medical history
    │   │   ├── pharmacy/              # Medicines, Batch inventory, Dispensing
    │   │   ├── prescriptions/         # Prescriptions & itemized dosage
    │   │   ├── security/              # JwtFilter, UserDetailsService, TokenProvider
    │   │   └── users/                 # User entities, Audit logs, Refresh tokens
    │   └── resources/
    │       ├── application.yml        # Active Spring Boot environment config
    │       └── application-prod.yml   # Production Supabase PostgreSQL config
```

---

## 🚀 Getting Started

### Prerequisites
- **JDK 17** or higher installed ([Temurin JDK 17](https://adoptium.net/))
- **Apache Maven 3.8+** (or use the included `mvnw` wrapper)
- **PostgreSQL 14+** (or Supabase PostgreSQL instance)

### Environment Configuration

Create or modify `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:hms}
    username: ${DB_USER:postgres}
    password: ${DB_PASS:postgres}
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false

jwt:
  secret: 404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
  expiration-ms: 86400000
```

### Local Development

1. **Clone Repository:**
   ```bash
   git clone https://github.com/gupthaappala-pixel/HMS_backendend_.git
   cd HMS_backendend_/HMS
   ```

2. **Run Spring Boot Application:**
   ```bash
   # On Windows PowerShell
   .\mvnw.cmd spring-boot:run

   # On Linux / macOS
   ./mvnw spring-boot:run
   ```

3. **Verify API Status:**
   Open browser at `http://localhost:8080/actuator/health` or `http://localhost:8080/api/v1/auth/health`.

### Running Unit Tests

Backend unit tests run using an isolated H2 in-memory test profile:

```bash
.\mvnw test
```

---

## ☁️ AWS Cloud Deployment

The HMS Spring Boot backend is production-ready for deployment on **Amazon Web Services (AWS)** using **AWS Elastic Beanstalk** (managed PaaS) or **AWS EC2** (IaaS).

### 1. Packaging the Application

Build the production executable JAR package using Maven:

```bash
# On Windows PowerShell
.\mvnw.cmd clean package -DskipTests

# On Linux / macOS
./mvnw clean package -DskipTests
```
The compiled executable binary will be generated at `target/HMS-0.0.1-SNAPSHOT.jar`.

### 2. Option A: AWS Elastic Beanstalk Deployment (Recommended)

1. **Create Elastic Beanstalk Application:**
   - In AWS Console, go to **Elastic Beanstalk** → **Create Application**.
   - Platform: **Java** (Corretto 17).
2. **Upload Artifact:**
   - Upload `target/HMS-0.0.1-SNAPSHOT.jar`.
3. **Set Environment Properties:**
   Configure environment properties under **Configuration → Software**:
   ```env
   SERVER_PORT=8080
   DB_HOST=your-rds-or-supabase-host.amazonaws.com
   DB_PORT=5432
   DB_NAME=hms
   DB_USER=postgres
   DB_PASS=your-secure-password
   JWT_SECRET=your-256bit-jwt-secret-key
   ```
4. **Deploy & Health Check:** Launch environment and verify health status via `/actuator/health`.

### 3. Option B: AWS EC2 Standalone Deployment

1. **Launch EC2 Instance:**
   - Launch Ubuntu 22.04 LTS or Amazon Linux 2023 instance (`t3.medium` recommended).
   - In Security Groups, allow inbound traffic on port `8080`, `80` (HTTP), and `443` (HTTPS).
2. **Install Java 17:**
   ```bash
   sudo apt update && sudo apt install openjdk-17-jre-headless -y
   ```
3. **Execute Application:**
   ```bash
   nohup java -jar HMS-0.0.1-SNAPSHOT.jar \
     --spring.profiles.active=prod \
     --DB_HOST=your-database-host \
     --DB_USER=postgres \
     --DB_PASS=your-password > app.log 2>&1 &
   ```

---

## 🔑 REST API Endpoints Summary

### Authentication (`/api/v1/auth`)
| Method | Endpoint | Description | Access |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/auth/login` | Authenticate user & return JWT token | Public |
| `POST` | `/api/v1/auth/register` | Register new user account | Public |
| `GET` | `/api/v1/auth/me` | Fetch currently logged-in user profile | Authenticated |

### Clinical EMR & Doctors (`/api/v1`)
| Method | Endpoint | Description | Access |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/doctors` | List all doctors with specializations | Authenticated |
| `GET` | `/api/v1/patients` | List all patient EMR profiles | `ADMIN`, `DOCTOR`, `NURSE` |
| `GET` | `/api/v1/appointments` | Fetch appointments with status filters | Authenticated |
| `POST` | `/api/v1/appointments` | Book a new consultation appointment | `PATIENT`, `ADMIN` |
| `GET` | `/api/v1/medical-records` | Fetch patient medical history & diagnoses | `DOCTOR`, `NURSE`, `ADMIN` |

### Pharmacy & Lab (`/api/v1`)
| Method | Endpoint | Description | Access |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/pharmacy/medicines` | Retrieve medicine catalog | Authenticated |
| `GET` | `/api/v1/pharmacy/inventory` | Retrieve stock batches & low-stock alerts | `PHARMACIST`, `ADMIN` |
| `GET` | `/api/v1/lab/tests` | Fetch laboratory test catalog | Authenticated |
| `POST` | `/api/v1/billing/invoices` | Generate itemized billing receipt | `ADMIN`, `PHARMACIST` |

---

## 🛡️ Security & Role-Based Access Control

The backend enforces strict JWT authentication using Spring Security's `OncePerRequestFilter`:
- **Authorization Header:** `Bearer <JWT_TOKEN>`
- **Password Encryption:** Passwords are standardly hashed using `BCryptPasswordEncoder` with salt rounds.

---

## 📄 License
This project is licensed under the MIT License - see the `LICENSE` file for details.
