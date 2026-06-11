# Banking Management System 

This project has been completely overhauled into a modern full-stack web application!

## Backend (Spring Boot 3 + Java 17 + MySQL)
The old core Java JDBC application has been migrated to a robust Spring Boot backend.
- **REST API endpoints** under `/api/customers`, `/api/accounts`, `/api/transactions`
- **Spring Data JPA** handling all database operations
- **Lombok** used for clean and concise entity classes
- Removed boilerplate SQL logic and replaced it with clean Java repository patterns

### Running the backend
1. Ensure your MySQL database is running and `banking_db` exists.
2. The `src/main/resources/application.properties` defines connection params (`root`/`password`).
3. Run the Spring Boot application using your IDE or `mvn spring-boot:run`.

## Frontend (React + Vite + TypeScript)
A brand-new, responsive, dark-mode inspired glassmorphism interactive frontend!
- View all customers dynamically
- See account balances mapped beautifully
- Real-time simulation of `DEPOSIT` and `WITHDRAWAL` operations

### Running the frontend
1. Open a terminal and navigate to the `frontend` folder
2. Run `npm run dev`
3. Visit the provided localhost link (typically `http://localhost:5173`)
