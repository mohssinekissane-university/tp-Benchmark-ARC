# REST API Performance Benchmark

This project compares performance across different Java REST implementations using the same business domain (Items and Categories management) with PostgreSQL.

## Overview

We measure and analyze three key metrics for each implementation variant:
- **API Performance**: Response latency (p50/p95/p99), throughput (requests/sec), and error rates
- **Resource Usage**: CPU and memory footprint, garbage collection activity, thread management
- **Abstraction Cost**: Manual controllers vs automated REST exposure

## Implementation Variants

The data model consists of two entities: `Category` (1) and `Item` (N). Three API variants are tested:
- **Variant A**: JAX-RS (Jersey) with JPA/Hibernate
- **Variant C**: Spring Boot with Spring MVC (`@RestController`) and JPA/Hibernate  
- **Variant D**: Spring Boot with Spring Data REST (automatic repository exposure)

## Tech Stack

Java 17, PostgreSQL 14+, Maven, Docker, Apache JMeter, Prometheus, InfluxDB v2, Grafana

## Quick Start

1. Setup PostgreSQL with `category` and `item` tables (2,000 categories, 100,000 items)
2. Launch monitoring stack: `docker-compose up -d` (Prometheus, Grafana, InfluxDB)
3. Build and run one variant at a time: `mvn clean package && java -jar target/app.jar`
4. Execute JMeter test scenarios and monitor Grafana dashboards for performance analysis


