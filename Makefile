COMPOSE := docker compose -f docker/docker-compose.yml

build:
	./mvnw clean package -DskipTests

build-native:
	./mvnw -Pnative spring-boot:build-image

## Run all tests (unit + integration)
test:
	./mvnw verify

## Check code formatting (Google Java Format via Spotless)
lint:
	./mvnw spotless:check

## Auto-fix code formatting
lint-fix:
	./mvnw spotless:apply

run:
	$(COMPOSE) up -d

stop:
	$(COMPOSE) down

clean:
	$(COMPOSE) down -v --rmi local

