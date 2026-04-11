# LogiAI - Intelligent Log Analyzer & Ticket Creator

LogiAI is an AI-powered log analysis engine built for modern incident response. Leveraging Spring Boot, MongoDB, and RabbitMQ, it uses intelligent semantic analysis to catch anomalies in real-time and automates Jira ticketing to streamline your DevOps workflow.

## 🚀 Key Features

1.  **Real-time AI Log Analysis**: 
    - Hybrid detection using **RegexRuleEngine** for known patterns and **LlmRuleEngine** for semantic anomaly detection.
    - Distributed ingestion via **RabbitMQ** to handle high-volume log streams from multiple microservices.
    
2.  **Deep Diagnostic Tracing & Root Cause Analysis**:
    - Automatic correlation of logs using `correlationId` to visualize execution traces.
    - AI-driven suggested solutions for every detected anomaly, helping engineers resolve issues faster.
    
3.  **Automated Incident Orchestration**:
    - Seamless integration with **Jira** for automated ticket creation.
    - Real-time alerts via **Email/Slack** ensuring high-priority issues are never missed.

---

## 🏗️ System Architecture & Flow

The following diagram illustrates the end-to-end data flow from log ingestion to incident resolution:

![LogiAI Architecture](drawings/flow%20diagram.png)

### Data Workflow:
1.  **Ingestion**: Logs are sent via REST API or File Upload to the `IngestionController`.
2.  **Buffering**: Log events are pushed to RabbitMQ (`ingestion.queue`) for asynchronous processing.
3.  **Analysis**: The `AnalysisService` orchestrates evaluations through Regex and LLM engines. Traces are buffered in **Redis** for context.
4.  **Persistence**: Detected anomalies and their suggested solutions are stored in **MongoDB**.
5.  **Dispatch**: High-severity anomalies trigger a task in RabbitMQ (`anomaly.exchange`).
6.  **Action**: The `DispatchListener` creates Jira tickets and sends notifications.

---

## 📂 Project Structure

```text
java-log-analyzer/
├── src/main/java/com/loganalyzer/
│   ├── analysis/       # Rule engines, AI analysis, and anomaly controllers
│   ├── auth/           # Identity and Access Management (Tenant models)
│   ├── core/           # Shared configs, DTOs, and base entities
│   ├── dispatch/       # Notification and Ticket services (Jira/Email)
│   └── ingestion/      # Log parsing and ingestion listeners
├── src/main/resources/
│   ├── static/         # Modern Dashboard UI (HTML, CSS, JS)
│   └── templates/      # Dashboard templates
├── drawings/           # Architecture diagrams
├── docs/               # Technical documentation
├── docker-compose.yml  # Infrastructure setup (Mongo, Redis, RabbitMQ, Ollama)
└── pom.xml             # Build and dependency configuration
```

---

## 🛠️ Setup & Installation

### Prerequisites
- **Java 17** or higher
- **Maven 3.8+**
- **Docker Desktop**

### 1. Spin up Infrastructure
Launch the database, cache, message broker, and local LLM (Ollama):
```bash
docker-compose up -d
```

### 2. Configure Environment
Update `src/main/resources/application.properties` with your credentials:
- **Jira API Tokens**
- **LLM Settings** (Default is Ollama at localhost:11434)
- **Database URIs**

### 3. Build & Run
```bash
mvn clean install
mvn spring-boot:run
```

Access the dashboard at: `http://localhost:8080`

---

## 📊 Dashboard UI
The dashboard provides a real-time feed of anomalies divided into **Open** and **Resolved** issues, with a direct view into execution traces and AI-suggested solutions.

> [!TIP]
> Use the **Manual Analysis** tab to upload and analyze historical log files instantly using the AI engine.
