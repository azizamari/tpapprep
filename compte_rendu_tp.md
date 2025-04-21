# Compte Rendu du TP

Aziz Amari - GL 2/3

## Modernisation et Déploiement d'une Application Spring Boot

Ce document présente les modifications apportées à une application Spring Boot existante.

## Liens Importants

- **GitHub** : [https://github.com/azizamari/tpapprep](https://github.com/azizamari/tpapprep)
- **API Déployée**: [https://tpappreparti.lemonflower-41c9fa1a.westus2.azurecontainerapps.io/swagger-ui/index.html](https://tpappreparti.lemonflower-41c9fa1a.westus2.azurecontainerapps.io/swagger-ui/index.html)
- **Image Docker**: [https://hub.docker.com/r/azizamari/apprep](https://hub.docker.com/r/azizamari/apprep)

### 1. Intégration de Swagger UI pour la Documentation de l'API

La documentation de l'API a été améliorée grâce à l'intégration de Swagger UI, permettant une visualisation interactive et conviviale des endpoints REST:

- Ajout de la dépendance SpringDoc OpenAPI dans le fichier `pom.xml`:
  ```xml
  <dependency>
      <groupId>org.springdoc</groupId>
      <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
      <version>2.5.0</version>
  </dependency>
  ```

- L'interface Swagger UI est accessible via l'URL: `/swagger-ui/index.html`
- Cette intégration permet de documenter automatiquement les endpoints REST, les modèles de données et les paramètres requis

### 2. Migration de la Base de Données H2 vers PostgreSQL

L'application a été migrée d'une base de données H2 en mémoire vers une base de données PostgreSQL persistante:

- Ajout de la dépendance PostgreSQL dans le fichier `pom.xml`:
  ```xml
  <dependency>
      <groupId>org.postgresql</groupId>
      <artifactId>postgresql</artifactId>
      <scope>runtime</scope>
  </dependency>
  ```

- Modification de la configuration dans `application.properties`:
  ```properties
  spring.datasource.url=jdbc:postgresql://db:5432/personnel
  spring.datasource.username=user
  spring.datasource.password=password
  spring.datasource.driver-class-name=org.postgresql.Driver
  spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
  ```

- Cette migration permet une persistance des données au-delà du cycle de vie de l'application

### 3. Conteneurisation avec Docker

L'application a été conteneurisée pour faciliter son déploiement et garantir la cohérence entre les environnements:

- Création d'un `Dockerfile` multi-stage pour optimiser la taille de l'image:
  ```dockerfile
  # Étape de build avec Maven
  FROM maven:3.9.6-eclipse-temurin-17 AS build
  WORKDIR /app
  COPY pom.xml .
  COPY src ./src
  RUN mvn clean package -DskipTests

  # Étape finale avec JRE seulement
  FROM eclipse-temurin:17-jre
  WORKDIR /app
  COPY --from=build /app/target/Personnel-0.0.1-SNAPSHOT.jar app.jar
  EXPOSE 8080
  ENTRYPOINT ["java", "-jar", "app.jar"]
  ```

- Création d'un fichier `docker-compose.yml` pour orchestrer l'application et sa base de données:
  ```yaml
  version: '3.8'
  services:
    app:
      build: .
      ports:
        - "8080:8080"
      environment:
        - SPRING_PROFILES_ACTIVE=default
      depends_on:
        - db
    db:
      image: postgres:16-alpine
      environment:
        POSTGRES_DB: personnel
        POSTGRES_USER: user
        POSTGRES_PASSWORD: password
      ports:
        - "5432:5432"
      volumes:
        - pgdata:/var/lib/postgresql/data
  volumes:
    pgdata:
  ```

### 4. Mise en Place d'un Pipeline CI/CD avec GitHub Actions

Un pipeline d'intégration et de déploiement continus a été implémenté pour automatiser le processus de build et de déploiement:

- Création d'un workflow GitHub Actions (`.github/workflows/deploy.yml`):
  ```yaml
  name: Docker Build & Azure Container Apps Deployment

  on:
    push:
      branches: [ main ]
    pull_request:
      branches: [ main ]
    workflow_dispatch:

  jobs:
    build-and-deploy:
      runs-on: ubuntu-latest
      environment: deploy
      
      steps:
      - uses: actions/checkout@v4
      
      - name: Login to Docker Hub
        uses: docker/login-action@v3
        with:
          username: ${{ secrets.DOCKERHUB_USERNAME }}
          password: ${{ secrets.DOCKERHUB_TOKEN }}
      
      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3
      
      - name: Build and push
        uses: docker/build-push-action@v5
        with:
          context: .
          push: true
          tags: ${{ secrets.DOCKERHUB_USERNAME }}/apprep:latest
      
      - name: Azure login
        uses: azure/login@v1
        with:
          creds: ${{ secrets.AZURE_CREDENTIALS }}
      
      - name: Deploy to Azure Container App
        uses: azure/CLI@v1
        with:
          inlineScript: |
            az containerapp update \
              --name tpappreparti \
              --resource-group ${{ secrets.AZURE_RESOURCE_GROUP }} \
              --image ${{ secrets.DOCKERHUB_USERNAME }}/apprep:latest \
              --environment ${{ secrets.AZURE_CONTAINER_APP_ENVIRONMENT }} \
              --ingress external \
              --target-port 8080 \
              --env-vars "SPRING_PROFILES_ACTIVE=prod"
  ```

### 5. Sécurisation des Accès et des Identifiants

Pour garantir la sécurité du pipeline de déploiement, plusieurs mesures ont été mises en place:

- **Création d'un environnement GitHub sécurisé**:
  - Configuration d'un environnement GitHub nommé "deploy" avec des règles de protection
  - Restriction de l'exécution du workflow aux branches autorisées
  - Isolation des secrets sensibles dans l'environnement dédié

- **Gestion sécurisée des identifiants avec GitHub Secrets**:
  - Configuration des identifiants Docker Hub:
    - `DOCKERHUB_USERNAME`: Identifiant Docker Hub
    - `DOCKERHUB_TOKEN`: Token d'accès Docker Hub (évitant l'utilisation du mot de passe)
  
  - Configuration des identifiants Azure:
    - `AZURE_CREDENTIALS`: JSON contenant les informations d'authentification Azure
    - `AZURE_RESOURCE_GROUP`: Nom du groupe de ressources Azure
    - `AZURE_CONTAINER_APP_ENVIRONMENT`: Nom de l'environnement Container App

- **Configuration IAM dans Azure**:
  - Création d'une App Registration dans Azure Active Directory
  - Attribution du rôle "Contributor" limité au groupe de ressources nécessaire
  - Génération d'un Client Secret avec une durée de validité limitée
  - Principe du moindre privilège: accès minimal requis pour le déploiement

Cette approche de gestion des identifiants garantit:
  - Aucun identifiant en clair dans le code source
  - Rotation facilitée des secrets sans modification du code
  - Traçabilité des actions effectuées par le service principal Azure
  - Révocation possible des accès sans impacter le code source
