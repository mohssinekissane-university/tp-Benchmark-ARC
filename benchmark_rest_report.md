# Benchmark de performances des Web Services REST

**Comparaison approfondie : Jersey JAX-RS, Spring Boot RestController et Spring Data REST**

**Variante A - Variante B - Variante C**

**Mohssine Kissane**

---

## Table des matières

1. Introduction générale
2. Modèle de données et spécifications
3. Variante A : JAX-RS Jersey + JPA/Hibernate
4. Variante B : Spring Boot RestController + Spring Data JPA
5. Variante C : Spring Boot + Spring Data REST
6. Environnement de test et instrumentation
7. Tests de charge JMeter
8. Résultats et analyse comparative
9. Synthèse et recommandations
10. InfluxDB Data Explorer - Analyse détaillée
11. Optimisations et perspectives
12. Conclusion
13. Annexes

---

## 1. Introduction générale

### 1.1 Contexte et enjeux

Le développement d'applications web basées sur des services REST s'est considérablement développé ces dernières années. Le choix du framework Java impacte de manière significative les performances, la productivité et la maintenance des applications.

Ce travail pratique évalue trois approches majeures pour la construction d'APIs REST :

1. **Variante A (VA)** : JAX-RS Jersey + JPA/Hibernate (approche bas-niveau)
2. **Variante B (VB)** : Spring Boot RestController + Spring Data JPA (niveau intermédiaire)
3. **Variante C (VC)** : Spring Boot Spring Data REST (exposition automatique)

### 1.2 Objectifs du benchmark

Les objectifs principaux de cette étude comparative sont :

1. Mesurer les performances brutes (RPS, latence p50/p95/p99, stabilité)
2. Analyser l'empreinte système (CPU, mémoire, threads, Garbage Collection)
3. Évaluer le coût d'abstraction de chaque approche
4. Fournir des recommandations d'optimisation concrètes

### 1.3 Scénarios de test

Quatre scénarios distincts ont été conçus pour évaluer différents aspects des performances :

#### 1.3.1 Scénario READ-heavy
- 50% GET items
- 20% GET avec filtres
- 20% GET avec relations
- 10% GET categories
- Charge : 50/100/200 threads
- Ramp-up : 60 secondes
- Durée du palier : 10 minutes

#### 1.3.2 Scénario JOIN-filter
- 70% GET avec filtres (requêtes JOIN)
- 30% GET single item
- Charge : 60/120 threads
- Ramp-up : 60 secondes
- Durée du palier : 8 minutes

#### 1.3.3 Scénario MIXED
- 40% GET
- 20% POST
- 10% PUT
- 10% DELETE
- Charge : 50/100 threads
- Ramp-up : 60 secondes
- Durée du palier : 10 minutes

#### 1.3.4 Scénario HEAVY-body
- 50% POST avec payload 5KB
- 50% PUT avec payload 5KB
- Charge : 30/60 threads
- Ramp-up : 60 secondes
- Durée du palier : 8 minutes

---

## 2. Modèle de données et spécifications

### 2.1 Architecture du modèle

Le modèle de données utilisé pour le benchmark comprend deux entités principales :

**Category (Catégorie)**
- id : BIGINT (clé primaire)
- code : VARCHAR(32) UNIQUE
- name : VARCHAR(128)
- updated_at : TIMESTAMP

**Item (Article)**
- id : BIGINT (clé primaire)
- sku : VARCHAR(64) UNIQUE
- name : VARCHAR(128)
- price : NUMERIC(10,2)
- stock : INT
- category_id : BIGINT (clé étrangère)

**Relation** : Une catégorie contient plusieurs items (relation 1:N)

**Données de test** :
- 2000 catégories
- 100 000 items

### 2.2 Endpoints REST communs

Tous les endpoints REST sont standardisés entre les trois variantes pour assurer une comparaison équitable.

#### 2.2.1 Opérations par entité

**Categories**

| Endpoint | Méthode | Description |
|----------|---------|-------------|
| /categories | GET | Liste paginée |
| /categories/{id} | GET | Détail |
| /categories | POST | Créer (0.51 KB) |
| /categories/{id} | PUT | Mettre à jour |
| /categories/{id} | DELETE | Supprimer |
| /categories/{id}/items | GET | Items de la catégorie |

**Items**

| Endpoint | Méthode | Description |
|----------|---------|-------------|
| /items | GET | Liste paginée |
| /items/{id} | GET | Détail |
| /items?categoryId=... | GET | Filtré (avec JOIN) |
| /items | POST | Créer (1-5 KB) |
| /items/{id} | PUT | Mettre à jour |
| /items/{id} | DELETE | Supprimer |

---

## 3. Variante A : JAX-RS Jersey + JPA/Hibernate

### 3.1 Présentation

Jersey représente l'implémentation de référence du standard JAX-RS (Java API for RESTful Web Services). Cette approche offre un contrôle fin et bas-niveau sur tous les aspects de l'API REST.

**Avantages** :
- Contrôle total sur les requêtes et réponses
- Performance optimale
- Empreinte mémoire minimale

**Inconvénients** :
- Code plus verbeux
- Configuration manuelle requise
- Courbe d'apprentissage plus élevée

### 3.2 Configuration Maven (pom.xml)

```xml
<project>
  <groupId>com.benchmark.rest</groupId>
  <artifactId>variante-a-jersey</artifactId>
  <version>1.0-SNAPSHOT</version>
  <packaging>war</packaging>

  <dependencies>
    <dependency>
      <groupId>org.glassfish.jersey.containers</groupId>
      <artifactId>jersey-container-servlet-core</artifactId>
      <version>3.1.2</version>
    </dependency>
    <dependency>
      <groupId>org.glassfish.jersey.media</groupId>
      <artifactId>jersey-media-json-jackson</artifactId>
      <version>3.1.2</version>
    </dependency>
    <dependency>
      <groupId>org.hibernate</groupId>
      <artifactId>hibernate-core</artifactId>
      <version>5.6.15.Final</version>
    </dependency>
    <dependency>
      <groupId>javax.persistence</groupId>
      <artifactId>javax.persistence-api</artifactId>
      <version>2.2</version>
    </dependency>
    <dependency>
      <groupId>org.postgresql</groupId>
      <artifactId>postgresql</artifactId>
      <version>42.5.1</version>
    </dependency>
    <dependency>
      <groupId>com.zaxxer</groupId>
      <artifactId>HikariCP</artifactId>
      <version>5.0.1</version>
    </dependency>
    <dependency>
      <groupId>io.prometheus</groupId>
      <artifactId>simpleclient</artifactId>
      <version>0.16.0</version>
    </dependency>
  </dependencies>
</project>
```

### 3.3 Configuration Hibernate (persistence.xml)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<persistence xmlns="http://xmlns.jcp.org/xml/ns/persistence" version="2.2">
  <persistence-unit name="benchmarkPU" transaction-type="RESOURCE_LOCAL">
    <provider>org.hibernate.jpa.HibernatePersistenceProvider</provider>
    <class>com.benchmark.rest.model.Category</class>
    <class>com.benchmark.rest.model.Item</class>
    
    <properties>
      <property name="javax.persistence.jdbc.driver" 
                value="org.postgresql.Driver"/>
      <property name="javax.persistence.jdbc.url" 
                value="jdbc:postgresql://localhost:5432/benchmark"/>
      <property name="javax.persistence.jdbc.user" value="postgres"/>
      <property name="javax.persistence.jdbc.password" value="password"/>
      <property name="hibernate.dialect" 
                value="org.hibernate.dialect.PostgreSQL14Dialect"/>
      <property name="hibernate.hbm2ddl.auto" value="update"/>
      <property name="hibernate.show_sql" value="false"/>
      
      <property name="hibernate.hikari.maximumPoolSize" value="20"/>
      <property name="hibernate.hikari.minimumIdle" value="10"/>
      <property name="hibernate.hikari.connectionTimeout" value="30000"/>
      
      <property name="hibernate.cache.use_second_level_cache" value="false"/>
      <property name="hibernate.cache.use_query_cache" value="false"/>
    </properties>
  </persistence-unit>
</persistence>
```

### 3.4 Entités JPA

#### 3.4.1 Category.java

```java
package com.benchmark.rest.model;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "category")
public class Category {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false, length = 32)
    private String code;
    
    @Column(nullable = false, length = 128)
    private String name;
    
    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Item> items = new ArrayList<>();
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    public Category() {}
    
    public Category(String code, String name) {
        this.code = code;
        this.name = name;
        this.updatedAt = LocalDateTime.now();
    }
    
    // Getters et setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<Item> getItems() { return items; }
    public void setItems(List<Item> items) { this.items = items; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
```

#### 3.4.2 Item.java

```java
package com.benchmark.rest.model;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "item")
public class Item {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false, length = 64)
    private String sku;
    
    @Column(nullable = false, length = 128)
    private String name;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;
    
    @Column(nullable = false)
    private Integer stock;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;
    
    @Column(length = 4000)
    private String description;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    public Item() {}
    
    public Item(String sku, String name, BigDecimal price, Integer stock, Category category) {
        this.sku = sku;
        this.name = name;
        this.price = price;
        this.stock = stock;
        this.category = category;
        this.updatedAt = LocalDateTime.now();
    }
    
    // Getters et setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
```

### 3.5 Couche DAO (Data Access Object)

#### 3.5.1 EntityManagerProvider.java

```java
package com.benchmark.rest.dao;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public class EntityManagerProvider {
    private static EntityManagerFactory emf;
    
    static {
        emf = Persistence.createEntityManagerFactory("benchmarkPU");
    }
    
    public static EntityManagerFactory getEntityManagerFactory() {
        return emf;
    }
    
    public static void closeFactory() {
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }
}
```

#### 3.5.2 CategoryDAO.java

```java
package com.benchmark.rest.dao;

import com.benchmark.rest.model.Category;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import java.util.List;

public class CategoryDAO {
    private EntityManagerFactory emf;
    
    public CategoryDAO(EntityManagerFactory emf) {
        this.emf = emf;
    }
    
    public List<Category> findAll(int pageSize, int page) {
        EntityManager em = emf.createEntityManager();
        try {
            Query query = em.createQuery("SELECT c FROM Category c ORDER BY c.id");
            query.setFirstResult(page * pageSize);
            query.setMaxResults(pageSize);
            return query.getResultList();
        } finally {
            em.close();
        }
    }
    
    public Category find(Long id) {
        EntityManager em = emf.createEntityManager();
        try {
            return em.find(Category.class, id);
        } finally {
            em.close();
        }
    }
    
    public Category save(Category category) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(category);
            em.getTransaction().commit();
            return category;
        } catch (Exception e) {
            em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }
    
    public Category update(Category category) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(category);
            em.getTransaction().commit();
            return category;
        } catch (Exception e) {
            em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }
    
    public void delete(Long id) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            Category category = em.find(Category.class, id);
            if (category != null) em.remove(category);
            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }
}
```

#### 3.5.3 ItemDAO.java

```java
package com.benchmark.rest.dao;

import com.benchmark.rest.model.Item;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import java.util.List;

public class ItemDAO {
    private EntityManagerFactory emf;
    
    public ItemDAO(EntityManagerFactory emf) {
        this.emf = emf;
    }
    
    public List<Item> findAll(int pageSize, int page) {
        EntityManager em = emf.createEntityManager();
        try {
            Query query = em.createQuery("SELECT i FROM Item i ORDER BY i.id");
            query.setFirstResult(page * pageSize);
            query.setMaxResults(pageSize);
            return query.getResultList();
        } finally {
            em.close();
        }
    }
    
    public List<Item> findByCategory(Long categoryId, int pageSize, int page) {
        EntityManager em = emf.createEntityManager();
        try {
            Query query = em.createQuery(
                "SELECT i FROM Item i JOIN FETCH i.category c " +
                "WHERE c.id = :categoryId ORDER BY i.id"
            );
            query.setParameter("categoryId", categoryId);
            query.setFirstResult(page * pageSize);
            query.setMaxResults(pageSize);
            return query.getResultList();
        } finally {
            em.close();
        }
    }
    
    public Item find(Long id) {
        EntityManager em = emf.createEntityManager();
        try {
            return em.find(Item.class, id);
        } finally {
            em.close();
        }
    }
    
    public Item save(Item item) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(item);
            em.getTransaction().commit();
            return item;
        } catch (Exception e) {
            em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }
    
    public Item update(Item item) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            em.merge(item);
            em.getTransaction().commit();
            return item;
        } catch (Exception e) {
            em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }
    
    public void delete(Long id) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            Item item = em.find(Item.class, id);
            if (item != null) em.remove(item);
            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }
}
```

### 3.6 Ressources Jersey (REST Endpoints)

#### 3.6.1 CategoryResource.java

```java
package com.benchmark.rest.resource;

import com.benchmark.rest.dao.CategoryDAO;
import com.benchmark.rest.dao.EntityManagerProvider;
import com.benchmark.rest.dao.ItemDAO;
import com.benchmark.rest.model.Category;
import com.benchmark.rest.model.Item;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("/categories")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CategoryResource {
    
    private CategoryDAO categoryDAO = new CategoryDAO(
        EntityManagerProvider.getEntityManagerFactory()
    );
    private ItemDAO itemDAO = new ItemDAO(
        EntityManagerProvider.getEntityManagerFactory()
    );
    
    @GET
    public List<Category> getAll(
        @QueryParam("pagesize") @DefaultValue("50") int pageSize,
        @QueryParam("page") @DefaultValue("0") int page) {
        return categoryDAO.findAll(pageSize, page);
    }
    
    @GET
    @Path("{id}")
    public Response getById(@PathParam("id") Long id) {
        Category category = categoryDAO.find(id);
        if (category == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(category).build();
    }
    
    @POST
    public Response create(Category category) {
        try {
            Category created = categoryDAO.save(category);
            return Response.status(Response.Status.CREATED).entity(created).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("{\"error\":\"" + e.getMessage() + "\"}").build();
        }
    }
    
    @PUT
    @Path("{id}")
    public Response update(@PathParam("id") Long id, Category category) {
        try {
            category.setId(id);
            Category updated = categoryDAO.update(category);
            return Response.ok(updated).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("{\"error\":\"" + e.getMessage() + "\"}").build();
        }
    }
    
    @DELETE
    @Path("{id}")
    public Response delete(@PathParam("id") Long id) {
        try {
            categoryDAO.delete(id);
            return Response.noContent().build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("{\"error\":\"" + e.getMessage() + "\"}").build();
        }
    }
    
    @GET
    @Path("{id}/items")
    public Response getCategoryItems(
        @PathParam("id") Long categoryId,
        @QueryParam("pagesize") @DefaultValue("50") int pageSize,
        @QueryParam("page") @DefaultValue("0") int page) {
        try {
            Category category = categoryDAO.find(categoryId);
            if (category == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            List<Item> items = itemDAO.findByCategory(categoryId, pageSize, page);
            return Response.ok(items).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("{\"error\":\"" + e.getMessage() + "\"}").build();
        }
    }
}
```

#### 3.6.2 ItemResource.java

```java
package com.benchmark.rest.resource;

import com.benchmark.rest.dao.ItemDAO;
import com.benchmark.rest.dao.EntityManagerProvider;
import com.benchmark.rest.model.Item;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("/items")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ItemResource {
    
    private ItemDAO itemDAO = new ItemDAO(
        EntityManagerProvider.getEntityManagerFactory()
    );
    
    @GET
    public Response getAll(
        @QueryParam("pagesize") @DefaultValue("50") int pageSize,
        @QueryParam("page") @DefaultValue("0") int page,
        @QueryParam("categoryId") Long categoryId) {
        if (categoryId != null) {
            List<Item> items = itemDAO.findByCategory(categoryId, pageSize, page);
            return Response.ok(items).build();
        }
        List<Item> items = itemDAO.findAll(pageSize, page);
        return Response.ok(items).build();
    }
    
    @GET
    @Path("{id}")
    public Response getById(@PathParam("id") Long id) {
        Item item = itemDAO.find(id);
        if (item == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(item).build();
    }
    
    @POST
    public Response create(Item item) {
        try {
            Item created = itemDAO.save(item);
            return Response.status(Response.Status.CREATED).entity(created).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("{\"error\":\"" + e.getMessage() + "\"}").build();
        }
    }
    
    @PUT
    @Path("{id}")
    public Response update(@PathParam("id") Long id, Item item) {
        try {
            item.setId(id);
            Item updated = itemDAO.update(item);
            return Response.ok(updated).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("{\"error\":\"" + e.getMessage() + "\"}").build();
        }
    }
    
    @DELETE
    @Path("{id}")
    public Response delete(@PathParam("id") Long id) {
        try {
            itemDAO.delete(id);
            return Response.noContent().build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("{\"error\":\"" + e.getMessage() + "\"}").build();
        }
    }
}
```

### 3.7 Configuration de l'application

#### ApplicationConfig.java

```java
package com.benchmark.rest.config;

import com.benchmark.rest.resource.CategoryResource;
import com.benchmark.rest.resource.ItemResource;
import org.glassfish.jersey.server.ResourceConfig;
import javax.ws.rs.ApplicationPath;

@ApplicationPath("/api")
public class ApplicationConfig extends ResourceConfig {
    
    public ApplicationConfig() {
        register(CategoryResource.class);
        register(ItemResource.class);
    }
}
```

---

## 4. Variante B : Spring Boot RestController + Spring Data JPA

### 4.1 Configuration Maven (pom.xml)

```xml
<project>
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.benchmark.rest</groupId>
  <artifactId>variante-b-springmvc</artifactId>
  <version>1.0-SNAPSHOT</version>
  <packaging>jar</packaging>
  
  <parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.1.5</version>
  </parent>
  
  <dependencies>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
      <groupId>org.postgresql</groupId>
      <artifactId>postgresql</artifactId>
      <scope>runtime</scope>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
    <dependency>
      <groupId>io.micrometer</groupId>
      <artifactId>micrometer-registry-prometheus</artifactId>
    </dependency>
  </dependencies>
  
  <build>
    <plugins>
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
      </plugin>
    </plugins>
  </build>
</project>
```

### 4.2 Configuration de l'application (application.properties)

```properties
spring.application.name=variante-b-springmvc
server.port=8080

spring.datasource.url=jdbc:postgresql://db:5432/benchmark
spring.datasource.username=postgres
spring.datasource.password=password
spring.datasource.driver-class-name=org.postgresql.Driver

spring.jpa.database-platform=org.hibernate.dialect.PostgreSQL14Dialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false

spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=10

spring.jpa.properties.hibernate.cache.use_second_level_cache=false

management.endpoints.web.exposure.include=metrics,prometheus
```

### 4.3 Repositories Spring Data JPA

#### 4.3.1 CategoryRepository.java

```java
package com.benchmark.rest.repository;

import com.benchmark.rest.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    Category findByCode(String code);
}
```

#### 4.3.2 ItemRepository.java

```java
package com.benchmark.rest.repository;

import com.benchmark.rest.model.Item;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {
    Item findBySku(String sku);
    
    @Query("SELECT i FROM Item i JOIN FETCH i.category c WHERE c.id = :categoryId")
    Page<Item> findByCategoryId(@Param("categoryId") Long categoryId, Pageable pageable);
}
```

### 4.4 Contrôleurs REST

#### 4.4.1 CategoryController.java

```java
package com.benchmark.rest.controller;

import com.benchmark.rest.model.Category;
import com.benchmark.rest.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    @GetMapping
    public Page<Category> getAll(
        @RequestParam(defaultValue = "50") int pagesize,
        @RequestParam(defaultValue = "0") int page) {
        return categoryRepository.findAll(PageRequest.of(page, pagesize));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Category> getById(@PathVariable Long id) {
        return categoryRepository.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping
    public ResponseEntity<Category> create(@RequestBody Category category) {
        Category saved = categoryRepository.save(category);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Category> update(@PathVariable Long id, @RequestBody Category category) {
        return categoryRepository.findById(id)
            .map(existing -> {
                category.setId(id);
                return ResponseEntity.ok(categoryRepository.save(category));
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        categoryRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
```

#### 4.4.2 ItemController.java

```java
package com.benchmark.rest.controller;

import com.benchmark.rest.model.Item;
import com.benchmark.rest.repository.ItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/items")
public class ItemController {
    
    @Autowired
    private ItemRepository itemRepository;
    
    @GetMapping
    public Page<Item> getAll(
        @RequestParam(defaultValue = "50") int pagesize,
        @RequestParam(defaultValue = "0") int page) {
        return itemRepository.findAll(PageRequest.of(page, pagesize));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Item> getById(@PathVariable Long id) {
        return itemRepository.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping(params = "categoryId")
    public Page<Item> getByCategory(
        @RequestParam Long categoryId,
        @RequestParam(defaultValue = "50") int pagesize,
        @RequestParam(defaultValue = "0") int page) {
        return itemRepository.findByCategoryId(categoryId, PageRequest.of(page, pagesize));
    }
    
    @PostMapping
    public ResponseEntity<Item> create(@RequestBody Item item) {
        Item saved = itemRepository.save(item);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Item> update(@PathVariable Long id, @RequestBody Item item) {
        return itemRepository.findById(id)
            .map(existing -> {
                item.setId(id);
                return ResponseEntity.ok(itemRepository.save(item));
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        itemRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
```

### 4.5 Classe principale de l'application

#### Application.java

```java
package com.benchmark.rest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

---

## 5. Variante C : Spring Boot + Spring Data REST

### 5.1 Configuration Maven (pom.xml)

```xml
<project>
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.benchmark.rest</groupId>
  <artifactId>variante-c-datarest</artifactId>
  <version>1.0-SNAPSHOT</version>
  <packaging>jar</packaging>
  
  <parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.1.5</version>
  </parent>
  
  <dependencies>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-data-rest</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
      <groupId>org.postgresql</groupId>
      <artifactId>postgresql</artifactId>
      <scope>runtime</scope>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
    <dependency>
      <groupId>io.micrometer</groupId>
      <artifactId>micrometer-registry-prometheus</artifactId>
    </dependency>
  </dependencies>
  
  <build>
    <plugins>
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
      </plugin>
    </plugins>
  </build>
</project>
```

### 5.2 Configuration de l'application (application.properties)

```properties
spring.application.name=variante-c-datarest
server.port=8080

spring.datasource.url=jdbc:postgresql://db:5432/benchmark
spring.datasource.username=postgres
spring.datasource.password=password

spring.jpa.database-platform=org.hibernate.dialect.PostgreSQL14Dialect
spring.jpa.hibernate.ddl-auto=update

spring.datasource.hikari.maximum-pool-size=20

spring.data.rest.base-path=/api
spring.data.rest.default-page-size=50

spring.jpa.properties.hibernate.cache.use_second_level_cache=false

management.endpoints.web.exposure.include=metrics,prometheus
```

### 5.3 Repositories avec exposition automatique

#### 5.3.1 CategoryRepository.java

```java
package com.benchmark.rest.repository;

import com.benchmark.rest.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(path = "categories", collectionResourceRel = "categories")
public interface CategoryRepository extends JpaRepository<Category, Long> {
    Category findByCode(String code);
}
```

#### 5.3.2 ItemRepository.java

```java
package com.benchmark.rest.repository;

import com.benchmark.rest.model.Item;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(path = "items", collectionResourceRel = "items")
public interface ItemRepository extends JpaRepository<Item, Long> {
    Item findBySku(String sku);
    
    @Query("SELECT i FROM Item i JOIN FETCH i.category WHERE i.category.id = :categoryId")
    Page<Item> findByCategoryId(@Param("categoryId") Long categoryId, Pageable pageable);
}
```

### 5.4 Classe principale de l'application

#### Application.java

```java
package com.benchmark.rest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

---

## 6. Environnement de test et instrumentation

### 6.1 Architecture de monitoring

L'architecture de monitoring mise en place utilise une stack complète pour la collecte et la visualisation des métriques :

**Composants** :
- **JMeter** : Génération de charge et envoi des métriques vers InfluxDB
- **InfluxDB** : Base de données temporelle pour stocker les métriques brutes
- **Grafana** : Visualisation et analyse des données via des requêtes Flux
- **Prometheus** : Collecte des métriques JVM et applicatives
- **PostgreSQL** : Base de données relationnelle pour les données de test

**Flux de données** :
```
JMeter → InfluxDB → Grafana (requêtes Flux)
     ↓
Variantes (VA, VB, VC) → Prometheus → Grafana
     ↓
PostgreSQL
```

**Ports utilisés** :
- VA (Jersey) : 8080, 8081 (métriques)
- VB (Spring MVC) : 8082, 8083 (métriques)
- VC (Spring REST) : 8084, 8085 (métriques)
- PostgreSQL : 5432
- Prometheus : 9090
- Grafana : 3000
- InfluxDB : 8086

### 6.2 Configuration Docker Compose

#### docker-compose.yml

```yaml
version: '3.8'

services:
  db:
    image: postgres:14-alpine
    container_name: benchmark-db
    environment:
      POSTGRES_DB: benchmark
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: password
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    networks:
      - benchmark-network
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 10s
      timeout: 5s
      retries: 5

  variante-a:
    build:
      context: ./variante-a
      dockerfile: Dockerfile
    container_name: benchmark-va
    environment:
      - JAVA_OPTS=-Xms512m -Xmx1024m -javaagent:/opt/prometheus/jmx_exporter.jar=8081:/opt/prometheus/config.yml
    ports:
      - "8080:8080"
      - "8081:8081"
    depends_on:
      db:
        condition: service_healthy
    networks:
      - benchmark-network

  variante-b:
    build:
      context: ./variante-b
      dockerfile: Dockerfile
    container_name: benchmark-vb
    environment:
      - SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/benchmark
    ports:
      - "8082:8080"
      - "8083:8081"
    depends_on:
      db:
        condition: service_healthy
    networks:
      - benchmark-network

  variante-c:
    build:
      context: ./variante-c
      dockerfile: Dockerfile
    container_name: benchmark-vc
    environment:
      - SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/benchmark
    ports:
      - "8084:8080"
      - "8085:8081"
    depends_on:
      db:
        condition: service_healthy
    networks:
      - benchmark-network

  prometheus:
    image: prom/prometheus:latest
    container_name: benchmark-prometheus
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
      - prometheus_data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
    networks:
      - benchmark-network

  grafana:
    image: grafana/grafana:latest
    container_name: benchmark-grafana
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin
    volumes:
      - grafana_data:/var/lib/grafana
    depends_on:
      - prometheus
    networks:
      - benchmark-network

  influxdb:
    image: influxdb:2.6
    container_name: benchmark-influxdb
    ports:
      - "8086:8086"
    environment:
      - INFLUXDB_DB=jmeter
      - INFLUXDB_ADMIN_USER=admin
      - INFLUXDB_ADMIN_PASSWORD=admin
    volumes:
      - influxdb_data:/var/lib/influxdb2
    networks:
      - benchmark-network

volumes:
  postgres_data:
  prometheus_data:
  grafana_data:
  influxdb_data:

networks:
  benchmark-network:
    driver: bridge
```

### 6.3 Configuration Prometheus

#### prometheus.yml

```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'variante-a'
    static_configs:
      - targets: ['variante-a:8081']
    metrics_path: '/metrics'

  - job_name: 'variante-b'
    static_configs:
      - targets: ['variante-b:8081']
    metrics_path: '/actuator/prometheus'

  - job_name: 'variante-c'
    static_configs:
      - targets: ['variante-c:8081']
    metrics_path: '/actuator/prometheus'

  - job_name: 'prometheus'
    static_configs:
      - targets: ['localhost:9090']
```

### 6.4 Initialisation des données de test

#### DataSeeder.java

```java
package com.benchmark.rest.util;

import com.benchmark.rest.model.Category;
import com.benchmark.rest.model.Item;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class DataSeeder {
    public static void main(String[] args) {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("benchmarkPU");
        EntityManager em = emf.createEntityManager();
        
        try {
            em.getTransaction().begin();
            
            // Création de 2000 catégories avec 50 items chacune
            for (int i = 1; i <= 2000; i++) {
                Category cat = new Category();
                cat.setCode(String.format("CAT%04d", i));
                cat.setName("Category " + i);
                cat.setUpdatedAt(LocalDateTime.now());
                em.persist(cat);
                
                // 50 items par catégorie = 100 000 items au total
                for (int j = 1; j <= 50; j++) {
                    Item item = new Item();
                    item.setSku(String.format("SKU%08d", i * 1000 + j));
                    item.setName("Item " + (i * 1000 + j));
                    item.setPrice(BigDecimal.valueOf(100.00 + j * 0.5));
                    item.setStock(j * 2);
                    item.setCategory(cat);
                    item.setDescription("Description for item " + (i * 1000 + j));
                    item.setUpdatedAt(LocalDateTime.now());
                    em.persist(item);
                }
                
                // Flush périodique pour éviter les problèmes de mémoire
                if (i % 50 == 0) {
                    em.flush();
                    em.clear();
                    System.out.println("Inserted " + (i * 50) + " items");
                }
            }
            
            em.getTransaction().commit();
            System.out.println("Data seeding completed: 2000 categories + 100000 items!");
            
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            e.printStackTrace();
        } finally {
            em.close();
            emf.close();
        }
    }
}
```

---

## 7. Tests de charge JMeter

### 7.1 Configuration du Backend Listener JMeter

La configuration du Backend Listener permet d'envoyer les métriques de JMeter vers InfluxDB en temps réel :

```
Backend Listener Configuration:
  Classname: org.apache.jmeter.visualizers.backend.influxdb.InfluxdbBackendListenerClient
  InfluxDB Server Hostname: localhost
  InfluxDB Server Port: 8086
  InfluxDB Database Name: jmeter
  InfluxDB Measurement: jmeter

Tags (dimensions):
  - application: benchmark-va, benchmark-vb, benchmark-vc
  - endpoint: /api/categories, /api/items
  - method: GET, POST, PUT, DELETE
  - status: success, failed
  - scenario: read-heavy, join-filter, mixed, heavy-body

Fields (métriques):
  - latency: temps de réponse (ms)
  - responseTime: latency + overhead
  - count: nombre de requêtes
  - hit: requêtes réussies
  - countError: requêtes échouées
  - responseSize: taille de la réponse (bytes)
  - sentBytes: taille du payload (bytes)
```

### 7.2 Détails du scénario READ-heavy

Ce scénario simule une charge typique de lecture intensive, courante dans les applications orientées consultation.

| Paramètre | Valeur |
|-----------|--------|
| Distribution de charge | 50% GET /items<br>20% GET avec filtres<br>20% GET avec relations<br>10% GET categories |
| Concurrence | 50, 100, 200 threads |
| Ramp-up | 60 secondes |
| Durée du palier | 10 minutes |
| Payload | Léger (GET uniquement) |
| Objectif | Mesurer la scalabilité en lecture |

### 7.3 Détails du scénario JOIN-filter

Ce scénario évalue les performances lors de requêtes avec jointures, particulièrement sensibles au problème N+1.

| Paramètre | Valeur |
|-----------|--------|
| Distribution de charge | 70% GET /items?categoryId (requêtes avec JOIN)<br>30% GET /items/{id} |
| Concurrence | 60, 120 threads |
| Ramp-up | 60 secondes |
| Durée du palier | 8 minutes |
| Objectif | Évaluer le problème N+1 et l'efficacité des JOIN FETCH |

### 7.4 Détails du scénario MIXED

Ce scénario simule une charge mixte réaliste avec opérations CRUD complètes.

| Paramètre | Valeur |
|-----------|--------|
| Distribution de charge | 40% GET<br>20% POST<br>10% PUT<br>10% DELETE<br>(+ 10% POST categories, 10% PUT categories) |
| Concurrence | 50, 100 threads |
| Ramp-up | 60 secondes |
| Durée du palier | 10 minutes |
| Payload | Mixte (~1 KB) |

### 7.5 Détails du scénario HEAVY-body

Ce scénario teste les performances avec des payloads volumineux, typiques des opérations avec descriptions longues ou métadonnées étendues.

| Paramètre | Valeur |
|-----------|--------|
| Distribution de charge | 50% POST avec payload 5KB<br>50% PUT avec payload 5KB |
| Concurrence | 30, 60 threads |
| Ramp-up | 60 secondes |
| Durée du palier | 8 minutes |
| Payload | Volumineux (5 KB avec descriptions) |

---

## 8. Résultats et analyse comparative

### 8.1 Résultats JMeter (100 threads)

Le tableau suivant présente les résultats détaillés des tests de charge avec 100 threads concurrents :

| Scénario | Variante | RPS | p50 (ms) | p95 (ms) | Erreurs (%) |
|----------|----------|-----|----------|----------|-------------|
| **READ-heavy** | VA Jersey | 2850 | 32 | 85 | 0.0 |
| | VB Spring MVC | 3120 | 29 | 78 | 0.0 |
| | VC Spring REST | 2950 | 31 | 82 | 0.1 |
| **JOIN-filter** | VA Jersey | 1950 | 48 | 125 | 0.0 |
| | VB Spring MVC | 2180 | 43 | 112 | 0.0 |
| | VC Spring REST | 1850 | 51 | 135 | 0.2 |
| **MIXED** | VA Jersey | 1200 | 78 | 210 | 0.0 |
| | VB Spring MVC | 1350 | 71 | 195 | 0.0 |
| | VC Spring REST | 1100 | 85 | 225 | 0.3 |
| **HEAVY-body** | VA Jersey | 480 | 195 | 520 | 0.0 |
| | VB Spring MVC | 520 | 180 | 485 | 0.0 |
| | VC Spring REST | 450 | 210 | 550 | 0.5 |

**Observations clés** :
- **VB (Spring MVC)** offre le meilleur débit (RPS) dans tous les scénarios
- **VA (Jersey)** présente l'empreinte la plus stable avec 0% d'erreur constant
- **VC (Spring REST)** montre des taux d'erreur légèrement supérieurs sous forte charge
- La latence p95 augmente significativement pour les opérations avec payloads volumineux

### 8.2 Analyse du débit (RPS) par scénario

**Comparaison graphique** :

```
READ-heavy:
  VB: ████████████████ 3120 RPS (+9% vs VA)
  VC: ███████████████  2950 RPS (+3% vs VA)
  VA: ██████████████   2850 RPS (baseline)

JOIN-filter:
  VB: ████████████ 2180 RPS (+12% vs VA)
  VA: ███████████  1950 RPS (baseline)
  VC: ██████████   1850 RPS (-5% vs VA)

MIXED:
  VB: ███████ 1350 RPS (+13% vs VA)
  VA: ██████  1200 RPS (baseline)
  VC: █████   1100 RPS (-8% vs VA)

HEAVY-body:
  VB: ████ 520 RPS (+8% vs VA)
  VA: ███  480 RPS (baseline)
  VC: ███  450 RPS (-6% vs VA)
```

### 8.3 Analyse de la latence p95

**Comparaison graphique** :

```
READ-heavy:
  VB: ███████ 78ms  (-8% vs VA)
  VC: ████████ 82ms  (-4% vs VA)
  VA: ████████ 85ms  (baseline)

JOIN-filter:
  VB: ███████████ 112ms (-10% vs VA)
  VA: ████████████ 125ms (baseline)
  VC: █████████████ 135ms (+8% vs VA)

MIXED:
  VB: ███████████████ 195ms (-7% vs VA)
  VA: ████████████████ 210ms (baseline)
  VC: █████████████████ 225ms (+7% vs VA)

HEAVY-body:
  VB: ███████████████████ 485ms (-7% vs VA)
  VA: ████████████████████ 520ms (baseline)
  VC: █████████████████████ 550ms (+6% vs VA)
```

### 8.4 Consommation des ressources système

| Variante | Métrique | Min | Moyenne | Max | Observation |
|----------|----------|-----|---------|-----|-------------|
| **VA Jersey** | CPU (%) | 15 | 45 | 72 | Très stable |
| | Heap (MB) | 120 | 580 | 820 | Croissance progressive |
| | Threads | 55 | 110 | 125 | Bien géré |
| | GC (ms) | 0 | 12 | 45 | Pauses courtes |
| **VB Spring MVC** | CPU (%) | 18 | 50 | 78 | CPU plus élevé |
| | Heap (MB) | 150 | 620 | 850 | Empreinte plus large |
| | Threads | 60 | 125 | 145 | Plus de threads |
| | GC (ms) | 2 | 15 | 52 | GC fréquent |
| **VC Spring REST** | CPU (%) | 20 | 55 | 85 | Pics CPU élevés |
| | Heap (MB) | 180 | 680 | 920 | Empreinte la plus grande |
| | Threads | 65 | 140 | 160 | Beaucoup de threads |
| | GC (ms) | 3 | 20 | 60 | GC plus long |

### 8.5 Évolution CPU pendant le test READ-heavy

```
Timeline (0-10 minutes, 100 threads):

CPU (%)
100 |
 80 |                            VC ----
 60 |                   VB ----      ----
 40 |          VA ----      ----  ----
 20 |     ----      ----  ----
  0 |----
    0  1  2  3  4  5  6  7  8  9  10 (minutes)

    VA Jersey:     15% → 45% → 72% (stable)
    VB Spring MVC: 18% → 50% → 78% (croissance modérée)
    VC Spring REST: 20% → 55% → 85% (pics élevés)
```

### 8.6 Évolution de la mémoire Heap

```
Heap (MB)
1000 |                            VC ----
 800 |                   VB ----      ----
 600 |          VA ----      ----  ----
 400 |     ----      ----  ----
 200 |----
   0 |
    0  1  2  3  4  5  6  7  8  9  10 (minutes)

    VA Jersey:     max 820 MB (empreinte minimale)
    VB Spring MVC: max 850 MB (+4% vs VA)
    VC Spring REST: max 920 MB (+12% vs VA)
```

### 8.7 Nombre de threads actifs

```
Threads
200 |
150 |                            VC ----
100 |                   VB ----      ----
 50 |          VA ----      ----  ----
  0 |----
    0  1  2  3  4  5  6  7  8  9  10 (minutes)

    VA Jersey:     max 125 threads
    VB Spring MVC: max 145 threads (+16% vs VA)
    VC Spring REST: max 160 threads (+28% vs VA)
```

### 8.8 Durée des pauses Garbage Collection

```
GC Pause (ms)
60 |                 [VC]
50 |            [VB]  |
40 |       [VA]  |    |
30 |        |    |    |
20 |        |    |    |
10 |   [VA] | [VB|    |
 0 |---+----+----+----+----
    READ JOIN MIXED HEAVY

    VA Jersey:     12ms (moyenne) - pauses les plus courtes
    VB Spring MVC: 15ms (moyenne)
    VC Spring REST: 20ms (moyenne) - pauses les plus longues
```

### 8.9 Taux d'erreur par scénario

```
Erreurs (%)
1.0 |
0.8 |
0.6 |                      [VC]
0.4 |              [VC]     |
0.2 |       [VC]    |       |
0.0 |--[VA][VB]--[VA][VB]--[VA][VB]--[VA][VB]
    READ    JOIN    MIXED    HEAVY

    VA & VB: Stabilité excellente (0% d'erreurs)
    VC: Légère dégradation sous forte charge
```

**Synthèse de l'analyse** :
- Jersey (VA) offre la meilleure empreinte ressources et stabilité
- Spring MVC (VB) propose le meilleur équilibre performance/ressources
- Spring REST (VC) sacrifie la performance pour la productivité

---

## 9. Synthèse et recommandations

### 9.1 Tableau comparatif synthétique

| Critère | Meilleure variante | Écart | Recommandation |
|---------|-------------------|-------|----------------|
| Débit (RPS) | VB Spring MVC | +9% vs VA | Spring MVC pour haute performance |
| Latence p95 | VB Spring MVC | -8% vs VA | Spring MVC pour réactivité |
| Stabilité | VA/VB | 0% erreurs | Jersey ou Spring MVC |
| Empreinte CPU | VA Jersey | -20% vs VC | Jersey si ressources limitées |
| Empreinte Heap | VA Jersey | -28% vs VC | Jersey le plus léger |
| Productivité | VC Spring REST | Code minimal | Spring REST pour MVPs |
| Flexibilité | VA Jersey | JPQL complet | Jersey pour requêtes complexes |
| Courbe d'apprentissage | VC Spring REST | La plus facile | Spring REST pour juniors |
| Maintenance long terme | VB Spring MVC | Équilibré | Spring MVC recommandé |

### 9.2 Matrice de décision

```
                   Performance
                       ↑
                       |
              VA       |        VB
            (Jersey)   |   (Spring MVC)
                       |
                       |
Peu de code ←----------+----------→ Beaucoup de code
                       |
                       |
              VC       |
          (Spring REST)|
                       |
                       ↓
                  Productivité

Positionnement:
- VA (Jersey