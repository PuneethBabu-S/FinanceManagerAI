Here’s a **download‑ready README.md** file for your project. You can copy this into a file named `README.md` in your repo root and commit it.

---

````markdown
# 🧾 AI-Driven Personal Finance Manager

A modern microservices-based personal finance platform that tracks expenses, provides analytics, and integrates AI for categorization and natural language queries.  
This project demonstrates backend mastery, system design, cloud deployment (later), security, and AI integration.

---

## 📑 Project Overview

- Track and categorize expenses automatically.
- Generate financial insights and summaries.
- Provide natural language queries via AI assistant.
- Real-time notifications for overspending or budget alerts.
- Secure authentication and role-based access.

---

## 🛠 Tech Stack

**Backend**

- Java 17, Spring Boot (microservices)
- Python (FastAPI for AI services)

**Databases & Storage**

- PostgreSQL, MongoDB, Redis
- Elasticsearch (semantic search)
- AWS S3 (later for receipts/reports)

**Messaging & Event Streaming**

- Apache Kafka + Zookeeper

**Deployment**

- Docker, Kubernetes (local first, cloud later)

**Security**

- OAuth2, JWT (Keycloak)
- Role-based access control (RBAC)

**Monitoring**

- Prometheus + Grafana
- ELK Stack (Elasticsearch, Logstash, Kibana)

**AI**

- Hugging Face / OpenAI APIs
- Custom ML models for expense categorization

---

## 🏗 Architecture Diagram

![Architecture Diagram](./docs/architecture.png)

---

## ⚙️ Setup Instructions

1. **Clone the repository**
   ```bash
   git clone https://github.com/<your-username>/FinanceManagerAI.git
   cd FinanceManagerAI
   ```
````

2. **Start environment with Docker Compose**

   ```bash
   docker-compose up -d
   ```

3. **Access services locally**
   - Keycloak → `http://localhost:8080` (admin/admin)
   - PostgreSQL → `localhost:5432` (user: postgres, password: pass)
   - MongoDB → `localhost:27017`
   - Redis → `localhost:6379`
   - Kafka → `localhost:9092`
   - Elasticsearch → `http://localhost:9200`

4. **Authentication Setup**
   
   See [KEYCLOAK_QUICK_REF.md](./KEYCLOAK_QUICK_REF.md) for quick start or [KEYCLOAK_AUTH_SETUP.md](./KEYCLOAK_AUTH_SETUP.md) for complete guide.
   
   Quick test:
   ```powershell
   .\test-keycloak-auth.ps1
   ```

5. **Microservices folders**
   ```
   /user-service
   /expense-service
   /analytics-service
   /ai-categorization-service
   /ai-assistant-service
   /notification-service
   /file-storage-service
   /monitoring-service
   /docs
   ```

---

## ✅ Roadmap

- Phase 1: Local development (Dockerized services, no cloud).
- Phase 2: Add AI categorization + assistant.
- Phase 3: Monitoring + logging integration.
- Phase 4: Cloud migration (AWS/GCP with Kubernetes).

---

## 📌 License

MIT License — free to use and modify.

```

```
