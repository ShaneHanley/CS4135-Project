import os
from pathlib import Path

ROOT = Path(r"c:\Users\josep\cs4135-copy\pharmacy-platform")
ROOT.mkdir(parents=True, exist_ok=True)

SERVICES = [
    ("gateway-service", 8080, None),
    ("auth-service", 8081, "auth_svc"),
    ("prescription-service", 8082, "prescription_svc"),
    ("pharmacy-service", 8083, "pharmacy_svc"),
    ("patient-service", 8084, "patient_svc"),
    ("inventory-service", 8085, "inventory_svc"),
    ("billing-service", 8086, "billing_svc"),
    ("analytics-service", 8087, "analytics_svc"),
]


def w(path: Path, content: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content.strip() + "\n", encoding="utf-8")


w(
    ROOT / ".env.example",
    """
SUPABASE_DB_URL=
SUPABASE_MIGRATION_URL=
SUPABASE_DB_USER=
SUPABASE_DB_PASSWORD=
SUPABASE_URL=
SUPABASE_SERVICE_KEY=
JWT_SECRET=
REACT_APP_API_BASE_URL=http://localhost:8080
REACT_APP_ORIGIN=http://localhost:3000
""",
)

for name, port, schema in SERVICES:
    sdir = ROOT / name
    pkg = name.replace("-service", "").replace("-", "")
    w(
        sdir / "README.md",
        f"""
# {name}

Schema: `{schema or "none"}`
Port: `{port}`
""",
    )
    w(
        sdir / ".env.example",
        (ROOT / ".env.example").read_text(encoding="utf-8"),
    )
    w(
        sdir / "Dockerfile",
        f"""
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline
COPY src src
RUN mvn -q -DskipTests clean package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE {port}
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
""",
    )
    if name == "gateway-service":
        extra = """
    <dependency><groupId>org.springframework.cloud</groupId><artifactId>spring-cloud-starter-gateway-server-webmvc</artifactId></dependency>
"""
    elif name == "auth-service":
        extra = """
    <dependency><groupId>io.jsonwebtoken</groupId><artifactId>jjwt-api</artifactId><version>0.12.6</version></dependency>
    <dependency><groupId>io.jsonwebtoken</groupId><artifactId>jjwt-impl</artifactId><version>0.12.6</version><scope>runtime</scope></dependency>
    <dependency><groupId>io.jsonwebtoken</groupId><artifactId>jjwt-jackson</artifactId><version>0.12.6</version><scope>runtime</scope></dependency>
"""
    else:
        extra = ""
    w(
        sdir / "pom.xml",
        f"""
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.pharmacy</groupId>
  <artifactId>{name}</artifactId>
  <version>0.0.1-SNAPSHOT</version>
  <properties><java.version>17</java.version><spring-cloud.version>2023.0.3</spring-cloud.version></properties>
  <dependencyManagement><dependencies><dependency><groupId>org.springframework.cloud</groupId><artifactId>spring-cloud-dependencies</artifactId><version>${{spring-cloud.version}}</version><type>pom</type><scope>import</scope></dependency></dependencies></dependencyManagement>
  <dependencies>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-web</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-security</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-validation</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-data-jpa</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-jdbc</artifactId></dependency>
    <dependency><groupId>org.springdoc</groupId><artifactId>springdoc-openapi-starter-webmvc-ui</artifactId><version>2.6.0</version></dependency>
    <dependency><groupId>org.postgresql</groupId><artifactId>postgresql</artifactId><scope>runtime</scope></dependency>
    <dependency><groupId>org.flywaydb</groupId><artifactId>flyway-core</artifactId></dependency>
    <dependency><groupId>org.flywaydb</groupId><artifactId>flyway-database-postgresql</artifactId></dependency>
    {extra}
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-test</artifactId><scope>test</scope></dependency>
  </dependencies>
  <build><plugins><plugin><groupId>org.springframework.boot</groupId><artifactId>spring-boot-maven-plugin</artifactId></plugin></plugins></build>
</project>
""",
    )
    w(
        sdir / "src/main/resources/application.yml",
        f"""
server:
  port: {port}
spring:
  application:
    name: {name}
  datasource:
    url: ${{SUPABASE_DB_URL}}
    username: ${{SUPABASE_DB_USER}}
    password: ${{SUPABASE_DB_PASSWORD}}
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate.default_schema: {schema or "public"}
  flyway:
    enabled: {"false" if name == "gateway-service" else "true"}
    url: ${{SUPABASE_MIGRATION_URL}}
    user: ${{SUPABASE_DB_USER}}
    password: ${{SUPABASE_DB_PASSWORD}}
    schemas: {schema or "public"}
    default-schema: {schema or "public"}
    locations: classpath:db/migration
""",
    )
    w(
        sdir / f"src/main/java/com/pharmacy/{pkg}/Application.java",
        f"""
package com.pharmacy.{pkg};

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class Application {{
    public static void main(String[] args) {{
        SpringApplication.run(Application.class, args);
    }}
}}
""",
    )
    if name != "gateway-service":
        w(
            sdir / "src/main/resources/db/migration/V1__init_schema.sql",
            f"CREATE SCHEMA IF NOT EXISTS {schema};",
        )

w(
    ROOT / "docker-compose.yml",
    """
services:
  gateway-service:
    build: ./gateway-service
    ports: ["8080:8080"]
    env_file: .env
  auth-service: { build: ./auth-service, ports: ["8081:8081"], env_file: .env }
  prescription-service: { build: ./prescription-service, ports: ["8082:8082"], env_file: .env }
  pharmacy-service: { build: ./pharmacy-service, ports: ["8083:8083"], env_file: .env }
  patient-service: { build: ./patient-service, ports: ["8084:8084"], env_file: .env }
  inventory-service: { build: ./inventory-service, ports: ["8085:8085"], env_file: .env }
  billing-service: { build: ./billing-service, ports: ["8086:8086"], env_file: .env }
  analytics-service: { build: ./analytics-service, ports: ["8087:8087"], env_file: .env }
  web-frontend:
    build: ./web-frontend
    ports: ["3000:3000"]
    env_file: .env
""",
)

wf = ROOT / "web-frontend"
w(
    wf / "package.json",
    """
{
  "name": "web-frontend",
  "private": true,
  "version": "0.1.0",
  "type": "module",
  "scripts": {
    "dev": "vite --host 0.0.0.0 --port 3000",
    "build": "vite build",
    "preview": "vite preview --host 0.0.0.0 --port 3000"
  },
  "dependencies": {
    "@hookform/resolvers": "^3.9.0",
    "@reduxjs/toolkit": "^2.2.7",
    "axios": "^1.7.5",
    "react": "^18.3.1",
    "react-dom": "^18.3.1",
    "react-hook-form": "^7.53.0",
    "react-redux": "^9.1.2",
    "react-router-dom": "^6.26.1",
    "recharts": "^2.12.7",
    "yup": "^1.4.0"
  },
  "devDependencies": {
    "@vitejs/plugin-react": "^4.3.1",
    "vite": "^5.4.2"
  }
}
""",
)
w(wf / ".env.example", "REACT_APP_API_BASE_URL=http://localhost:8080\nREACT_APP_ORIGIN=http://localhost:3000\n")
w(wf / "vite.config.js", "import { defineConfig } from 'vite';\nimport react from '@vitejs/plugin-react';\nexport default defineConfig({ plugins: [react()] });")
w(
    wf / "index.html",
    "<!doctype html><html><head><meta charset='UTF-8'/><meta name='viewport' content='width=device-width, initial-scale=1.0'/><title>Pharmacy Platform</title></head><body><div id='root'></div><script type='module' src='/src/main.jsx'></script></body></html>",
)
w(
    wf / "src/main.jsx",
    "import React from 'react'; import { createRoot } from 'react-dom/client'; import { BrowserRouter } from 'react-router-dom'; import { Provider } from 'react-redux'; import { store } from './app/store'; import App from './App'; createRoot(document.getElementById('root')).render(<Provider store={store}><BrowserRouter><App /></BrowserRouter></Provider>);",
)
w(
    wf / "src/app/store.js",
    "import { configureStore } from '@reduxjs/toolkit'; import authReducer from '../features/authSlice'; export const store = configureStore({ reducer: { auth: authReducer } });",
)
w(
    wf / "src/features/authSlice.js",
    "import { createSlice } from '@reduxjs/toolkit'; const stored = localStorage.getItem('auth'); const initial = stored ? JSON.parse(stored) : {accessToken:null,refreshToken:null,user:null}; const slice = createSlice({name:'auth', initialState: initial, reducers:{ setAuth(_s,a){ localStorage.setItem('auth', JSON.stringify(a.payload)); return a.payload; }, clearAuth(){ localStorage.removeItem('auth'); return {accessToken:null,refreshToken:null,user:null}; } }}); export const { setAuth, clearAuth } = slice.actions; export default slice.reducer;",
)
w(
    wf / "src/App.jsx",
    "import { Routes, Route, Navigate } from 'react-router-dom'; import { useSelector } from 'react-redux'; function Protected({children}){ return useSelector(s=>s.auth.accessToken) ? children : <Navigate to='/login' replace/>; } export default function App(){ return <Routes><Route path='/login' element={<div>Login page scaffold</div>} /><Route path='/' element={<Protected><div>Dashboard scaffold</div></Protected>} /><Route path='*' element={<Navigate to='/' replace/>} /></Routes>; }",
)

w(
    ROOT / "README.md",
    """
# Pharmacy Management Platform Scaffold

Includes:
- 8 Spring Boot microservices (`gateway`, `auth`, `prescription`, `pharmacy`, `patient`, `inventory`, `billing`, `analytics`)
- React frontend (`web-frontend`)
- Supabase-first environment placeholders and docker-compose
""",
)

print("Scaffold generated in", ROOT)
