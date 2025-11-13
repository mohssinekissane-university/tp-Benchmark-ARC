Voici un fichier `README.md` complet pour ce projet, basé sur les informations fournies dans le document.

-----

# Benchmark de Performances des Web Services REST

Ce projet a pour objectif d'évaluer et de comparer les performances de trois stacks techniques Java différentes pour l'implémentation de services web REST.

[cite\_start]L'évaluation est réalisée sur un même domaine métier (Gestion d'Items et Catégories) et une même base de données (PostgreSQL), en se concentrant sur les impacts des choix d'implémentation[cite: 4].

## 🎯 Objectifs

Les métriques clés analysées pour chaque variante sont :

  * [cite\_start]**Performance API :** Latence (p50/p95/p99), Débit (requêtes/seconde) et Taux d'erreurs[cite: 5].
  * [cite\_start]**Consommation Ressources :** Empreinte CPU et RAM, activité du Garbage Collector (GC) et gestion des threads[cite: 6].
  * [cite\_start]**Coût d'Abstraction :** Comparaison entre un contrôleur écrit manuellement (Variantes A et C) et l'exposition automatique fournie par Spring Data REST (Variante D)[cite: 7].

## 🔧 Architecture & Variantes

[cite\_start]Le domaine métier est composé de deux entités : `Category` (1) et `Item` (N)[cite: 9].

Trois variantes de l'API ont été implémentées pour ce benchmark :

  * [cite\_start]**Variante A :** JAX-RS (Jersey) + JPA/Hibernate[cite: 46].
  * [cite\_start]**Variante C :** Spring Boot + Spring MVC (`@RestController`) + JPA/Hibernate[cite: 47].
  * [cite\_start]**Variante D :** Spring Boot + Spring Data REST (Exposition automatique des repositories)[cite: 48].

## 🛠️ Stack Technique & Prérequis

  * [cite\_start]Java 17 [cite: 80]
  * [cite\_start]PostgreSQL 14+ [cite: 80]
  * Maven (pour le build)
  * Docker & Docker Compose (recommandé pour la pile de monitoring)
  * [cite\_start]Apache JMeter (pour les tests de charge) [cite: 82]
  * [cite\_start]Prometheus (pour les métriques JVM) [cite: 81]
  * [cite\_start]InfluxDB v2 (pour les métriques JMeter) [cite: 82]
  * [cite\_start]Grafana (pour la visualisation) [cite: 81]

-----

## 🚀 Installation & Configuration

### 1\. Base de Données (PostgreSQL)

1.  Assurez-vous qu'une instance PostgreSQL 14+ est en cours d'exécution.
2.  [cite\_start]Créez les tables `category` et `item` en utilisant les scripts SQL fournis[cite: 11, 22]. [cite\_start]N'oubliez pas les index[cite: 42, 44].
3.  Peuplez la base de données avec le jeu de données requis :
      * [cite\_start]**2 000** catégories[cite: 74].
      * [cite\_start]**100 000** items (avec une distribution d'environ 50 items/catégorie)[cite: 75].

### 2\. Pile de Monitoring (Docker)

Le moyen le plus simple de lancer la pile de monitoring est via Docker Compose.

1.  Assurez-vous qu'un fichier `docker-compose.yml` est configuré pour lancer Prometheus, Grafana et InfluxDB v2.
2.  **InfluxDB :** Après le lancement, accédez à l'interface web (ex: `http://localhost:8086`) et créez :
      * [cite\_start]Une organisation (ex: `perf`)[cite: 117].
      * [cite\_start]Un bucket (ex: `jmeter`)[cite: 117].
      * Un token d'API (API Token) ayant les droits d'écriture sur le bucket `jmeter`.
3.  **Prometheus :** Configurez `prometheus.yml` pour "scraper" les métriques des applications Java (voir ci-dessous).
4.  **Grafana :** Connectez Grafana à vos deux *Data Sources* (Prometheus et InfluxDB).

### 3\. Lancement des Applications

[cite\_start]Vous devez lancer **une seule variante à la fois** pour isoler les mesures[cite: 129].

**Variantes C et D (Spring Boot) :**
[cite\_start]Ces variantes utilisent Spring Actuator et Micrometer pour exposer les métriques Prometheus[cite: 83].

```bash
# S'assurer que application.properties est configuré (DB, etc.)
mvn clean package
java -jar target/benchmark-variante-C.jar
```

**Variante A (JAX-RS) :**
[cite\_start]Cette variante nécessite l'agent Java JMX Exporter pour exposer ses métriques à Prometheus[cite: 81].

```bash
# Télécharger jmx_prometheus_javaagent.jar et un fichier config.yml
mvn clean package
java -javaagent:/path/to/jmx_prometheus_javaagent.jar=8081:config.yml -jar target/benchmark-variante-A.jar
```

-----

## ⏱️ Exécution des Scénarios de Charge

Les tests de charge sont définis dans des fichiers `.jmx` (Apache JMeter).

**Configuration JMeter (Bonnes pratiques) :**

  * [cite\_start]**`HTTP Request Defaults` :** À configurer pour pointer vers l'URL de la variante testée (ex: `http://localhost:8080`)[cite: 116].
  * [cite\_start]**`CSV Data Set Config` :** Les tests doivent utiliser des fichiers CSV pour les `id` existants (catégories, items) afin de simuler une charge réaliste[cite: 115].
  * [cite\_start]**`Backend Listener` :** Doit être configuré pour envoyer les résultats en temps réel vers InfluxDB v2 (en utilisant l'URL, l'organisation, le bucket et le token créés à l'étape 2)[cite: 117].
  * [cite\_start]**Listeners Lourds :** Tous les listeners graphiques (comme "View Results Tree") doivent être désactivés pendant les tirs de performance[cite: 118].

### Les 4 Scénarios de Test

Exécutez chaque scénario `.jmx` contre la variante d'application en cours d'exécution.

1.  **Scénario 1 : READ-heavy (relation incluse)**

      * [cite\_start]**Mix :** 50% `GET /items`, 20% `GET /items?categoryId=...`, 20% `GET /categories/{id}/items`, 10% `GET /categories` [cite: 89-92].
      * [cite\_start]**Charge :** Paliers de 50, 100 et 200 threads[cite: 93].

2.  **Scénario 2 : JOIN-filter ciblé**

      * [cite\_start]**Mix :** 70% `GET /items?categoryId=...`, 30% `GET /items/{id}`[cite: 96, 98].
      * [cite\_start]**Charge :** Paliers de 60 et 120 threads[cite: 99].

3.  **Scénario 3 : MIXED (écritures sur deux entités)**

      * [cite\_start]**Mix :** Mélange de GET, POST, PUT, DELETE sur les `items` et les `categories` (payloads de \~1 KB) [cite: 101-108].
      * [cite\_start]**Charge :** Paliers de 50 et 100 threads[cite: 109].

4.  **Scénario 4 : HEAVY-body (payload 5 KB)**

      * [cite\_start]**Mix :** 50% `POST /items` (5 KB), 50% `PUT /items/{id}` (5 KB) [cite: 111-112].
      * [cite\_start]**Charge :** Paliers de 30 et 60 threads[cite: 113].

## 📊 Analyse des Résultats

Pendant que les tests JMeter s'exécutent, observez les dashboards Grafana.

[cite\_start]L'objectif final est de collecter les données de performance (RPS, p95, Erreurs) et les métriques JVM (CPU, Heap, GC, Threads) pour chaque scénario et chaque variante afin de remplir les tableaux d'analyse (T2 à T7) [cite: 136, 142, 173] [cite\_start]et de formuler des r
[benchmark.pdf](https://github.com/user-attachments/files/23487503/benchmark.pdf)
ecommandations[cite: 174].


