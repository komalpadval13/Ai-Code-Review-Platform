# AI Code Review Platform (CodeLens AI)

A production-grade, full-stack SaaS platform for automated code review powered by AI, static analysis, and plagiarism detection.

## Architecture

```
┌─────────────────────────────────────────────────────┐
│                    Nginx (Port 80)                   │
│          Reverse Proxy + Load Balancer               │
├──────────────────────┬──────────────────────────────┤
│   Frontend (:5173)   │    Backend (:8080)            │
│   React + Vite       │    Spring Boot 3              │
│   TypeScript         │    Java 21                    │
│   TailwindCSS        │    Spring Security + JWT      │
│   Zustand            │    Spring Data JPA            │
│   Monaco Editor      │    WebSocket + STOMP          │
│   Framer Motion      │    JavaParser AST             │
│                      │    AI Provider Abstraction     │
├──────────────────────┴──────────────────────────────┤
│                  MySQL 8 Database                    │
└─────────────────────────────────────────────────────┘
```

## Features

- **Authentication**: JWT access/refresh tokens, BCrypt hashing, role-based access (USER/ADMIN)
- **Project Management**: CRUD with visibility controls and tagging
- **Code Upload**: Drag-and-drop, multi-language support (.java, .py, .js, .ts, .cpp, .c, etc.)
- **Static Analysis**: AST-based analysis with JavaParser (long methods, empty catches, deep nesting, magic numbers, security risks)
- **Code Metrics**: LOC, cyclomatic complexity, maintainability index, comment ratio
- **AI Review**: Real OpenAI/Claude/Gemini API integration with structured JSON responses
- **Plagiarism Detection**: Winnowing fingerprint algorithm with Jaccard similarity
- **Real-Time Progress**: WebSocket + STOMP for live processing updates
- **PDF Reports**: Downloadable reports with findings, metrics, and plagiarism results
- **Admin Panel**: User management, audit logs, system analytics
- **Premium UI**: Glassmorphism dark theme, animated transitions, Monaco code editor

## Quick Start

### Prerequisites
- Java 21+
- Node.js 20+
- MySQL 8+
- Maven 3.9+

### 1. Database Setup
```bash
mysql -u root -p -e "CREATE DATABASE code_review_db;"
```

### 2. Backend
```bash
cd backend
mvn spring-boot:run
```

### 3. Frontend
```bash
cd frontend
npm install
npm run dev
```

### 4. Docker (Full Stack)
```bash
cp .env.example .env
# Edit .env with your API keys
docker compose up -d
```

## Configuration

| Variable | Description | Default |
|----------|-------------|---------|
| `DB_URL` | MySQL JDBC URL | `jdbc:mysql://localhost:3306/code_review_db` |
| `DB_USERNAME` | Database username | `root` |
| `DB_PASSWORD` | Database password | `root` |
| `JWT_SECRET` | JWT signing key (Base64) | Generated |
| `AI_PROVIDER` | AI provider (openai/anthropic/google) | `openai` |
| `AI_API_KEY` | AI provider API key | Required for AI reviews |
| `AI_MODEL` | AI model name | `gpt-4o` |

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/register` | Register new user |
| POST | `/api/auth/login` | Login |
| POST | `/api/auth/refresh` | Refresh token |
| GET | `/api/projects` | List user projects |
| POST | `/api/projects` | Create project |
| POST | `/api/submissions/upload/{projectId}` | Upload code file |
| GET | `/api/submissions/{id}` | Get submission details |
| GET | `/api/submissions/{id}/report` | Download PDF report |
| GET | `/api/analytics/dashboard` | Dashboard statistics |
| GET | `/api/admin/stats` | Admin statistics |

## Default Admin Credentials
- Username: `admin`
- Password: `admin123`

## License
MIT
