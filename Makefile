# FinPass Makefile
# Provides convenient commands for development, testing, and deployment

.PHONY: help build up down logs health test clean demo backup restore

# Default target
.DEFAULT_GOAL := help

# Variables
COMPOSE_FILE := docker-compose.yaml
ENV_FILE := .env
BACKUP_DIR := backups
TIMESTAMP := $(shell date +%Y%m%d-%H%M%S)

# Colors
BLUE := \033[36m
GREEN := \033[32m
YELLOW := \033[33m
RED := \033[31m
NC := \033[0m

help: ## Show this help message
	@echo "$(BLUE)FinPass Development Commands$(NC)"
	@echo ""
	@echo "$(GREEN)Development:$(NC)"
	@awk 'BEGIN {FS = ":.*?## "} /^[a-zA-Z_-]+:.*?## / {printf "  $(YELLOW)%-15s$(NC) %s\n", $$1, $$2}' $(MAKEFILE_LIST) | grep -E "Development|build|up|down|logs|health|test|clean"
	@echo ""
	@echo "$(GREEN)Deployment:$(NC)"
	@awk 'BEGIN {FS = ":.*?## "} /^[a-zA-Z_-]+:.*?## / {printf "  $(YELLOW)%-15s$(NC) %s\n", $$1, $$2}' $(MAKEFILE_LIST) | grep -E "Deployment|deploy|undeploy|status"
	@echo ""
	@echo "$(GREEN)Data Management:$(NC)"
	@awk 'BEGIN {FS = ":.*?## "} /^[a-zA-Z_-]+:.*?## / {printf "  $(YELLOW)%-15s$(NC) %s\n", $$1, $$2}' $(MAKEFILE_LIST) | grep -E "Data|backup|restore|migrate"
	@echo ""
	@echo "$(GREEN)Demo & Testing:$(NC)"
	@awk 'BEGIN {FS = ":.*?## "} /^[a-zA-Z_-]+:.*?## / {printf "  $(YELLOW)%-15s$(NC) %s\n", $$1, $$2}' $(MAKEFILE_LIST) | grep -E "Demo|demo|test|integration"

# Development Commands
check-env: ## Check if .env file exists
	@if [ ! -f $(ENV_FILE) ]; then \
		echo "$(RED)Error: $(ENV_FILE) not found$(NC)"; \
		echo "$(YELLOW)Please copy $(ENV_FILE).example to $(ENV_FILE) and configure it$(NC)"; \
		exit 1; \
	fi
	@echo "$(GREEN)✓ Environment file exists$(NC)"

build: check-env ## Build all Docker images
	@echo "$(BLUE)Building Docker images...$(NC)"
	docker-compose -f $(COMPOSE_FILE) build
	@echo "$(GREEN)✓ Build completed$(NC)"

up: check-env ## Start all services
	@echo "$(BLUE)Starting FinPass services...$(NC)"
	docker-compose -f $(COMPOSE_FILE) up -d
	@echo "$(GREEN)✓ Services started$(NC)"
	@echo "$(YELLOW)Waiting for services to be ready...$(NC)"
	sleep 10
	$(MAKE) health

down: ## Stop all services
	@echo "$(BLUE)Stopping FinPass services...$(NC)"
	docker-compose -f $(COMPOSE_FILE) down
	@echo "$(GREEN)✓ Services stopped$(NC)"

restart: down up ## Restart all services

logs: ## Show logs for all services
	docker-compose -f $(COMPOSE_FILE) logs -f

logs-backend: ## Show backend service logs
	docker-compose -f $(COMPOSE_FILE) logs -f backend

logs-frontend: ## Show frontend service logs
	docker-compose -f $(COMPOSE_FILE) logs -f frontend

logs-db: ## Show database logs
	docker-compose -f $(COMPOSE_FILE) logs -f postgres

health: ## Check health of all services
	@echo "$(BLUE)Checking service health...$(NC)"
	@echo ""
	@echo "$(GREEN)Frontend:$(NC)"
	@curl -s http://localhost/health || echo "$(RED)❌ Frontend not responding$(NC)"
	@echo ""
	@echo "$(GREEN)Backend:$(NC)"
	@curl -s http://localhost:8080/health | jq '.' 2>/dev/null || curl -s http://localhost:8080/health
	@echo ""
	@echo "$(GREEN)Database:$(NC)"
	@docker-compose -f $(COMPOSE_FILE) exec -T postgres pg_isready -U finpass || echo "$(RED)❌ Database not ready$(NC)"
	@echo ""
	@echo "$(GREEN)Redis:$(NC)"
	@docker-compose -f $(COMPOSE_FILE) exec -T redis redis-cli ping || echo "$(RED)❌ Redis not responding$(NC)"
	@echo ""
	@echo "$(GREEN)Service Status:$(NC)"
	@docker-compose -f $(COMPOSE_FILE) ps

status: ## Show detailed service status
	@echo "$(BLUE)FinPass Service Status$(NC)"
	@echo "====================="
	@docker-compose -f $(COMPOSE_FILE) ps
	@echo ""
	@echo "$(BLUE)Resource Usage:$(NC)"
	@docker stats --no-stream --format "table {{.Container}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.NetIO}}\t{{.BlockIO}}"

test: ## Run all tests
	@echo "$(BLUE)Running tests...$(NC)"
	$(MAKE) test-backend
	$(MAKE) test-frontend
	$(MAKE) test-integration

test-backend: ## Run backend tests
	@echo "$(BLUE)Running backend tests...$(NC)"
	docker-compose -f $(COMPOSE_FILE) exec backend ./mvnw test

test-frontend: ## Run frontend tests
	@echo "$(BLUE)Running frontend tests...$(NC)"
	docker-compose -f $(COMPOSE_FILE) exec frontend npm test -- --coverage --watchAll=false

test-integration: ## Run integration tests
	@echo "$(BLUE)Running integration tests...$(NC)"
	docker-compose -f $(COMPOSE_FILE) exec backend ./mvnw test -Dtest=*IntegrationTest

test-watch: ## Run tests in watch mode
	@echo "$(BLUE)Running tests in watch mode...$(NC)"
	docker-compose -f $(COMPOSE_FILE) exec backend ./mvnw test -Dspring.test.repeat=true

clean: ## Clean up Docker resources
	@echo "$(BLUE)Cleaning up Docker resources...$(NC)"
	docker-compose -f $(COMPOSE_FILE) down -v
	docker system prune -f
	docker volume prune -f
	@echo "$(GREEN)✓ Cleanup completed$(NC)"

# Deployment Commands
deploy-dev: ## Deploy to development environment
	@echo "$(BLUE)Deploying to development...$(NC)"
	$(MAKE) build
	$(MAKE) up
	@echo "$(GREEN)✓ Development deployment completed$(NC)"

deploy-staging: ## Deploy to staging environment
	@echo "$(BLUE)Deploying to staging...$(NC)"
	# Add staging-specific deployment logic here
	@echo "$(YELLOW)Staging deployment not yet implemented$(NC)"

deploy-prod: ## Deploy to production environment
	@echo "$(BLUE)Deploying to production...$(NC)"
	# Add production-specific deployment logic here
	@echo "$(YELLOW)Production deployment not yet implemented$(NC)"

undeploy: ## Undeploy all services
	@echo "$(BLUE)Undeploying services...$(NC)"
	$(MAKE) down
	@echo "$(GREEN)✓ Undeployment completed$(NC)"

# Data Management Commands
init-db: ## Initialize database with migrations
	@echo "$(BLUE)Initializing database...$(NC)"
	docker-compose -f $(COMPOSE_FILE) exec postgres psql -U finpass -d finpass -f /docker-entrypoint-initdb.d/init.sql
	@echo "$(GREEN)✓ Database initialized$(NC)"

migrate: ## Run database migrations
	@echo "$(BLUE)Running database migrations...$(NC)"
	docker-compose -f $(COMPOSE_FILE) exec backend ./mvnw flyway:migrate
	@echo "$(GREEN)✓ Migrations completed$(NC)"

backup: ## Backup all data
	@echo "$(BLUE)Creating backup...$(NC)"
	@mkdir -p $(BACKUP_DIR)
	@echo "$(YELLOW)Backing up database...$(NC)"
	docker-compose -f $(COMPOSE_FILE) exec postgres pg_dump -U finpass finpass > $(BACKUP_DIR)/database-$(TIMESTAMP).sql
	@echo "$(YELLOW)Backing up volumes...$(NC)"
	docker run --rm -v finpass_postgres_data:/data -v $(PWD)/$(BACKUP_DIR):/backup ubuntu tar czf /backup/postgres-data-$(TIMESTAMP).tar.gz -C /data .
	@echo "$(YELLOW)Backing up Redis...$(NC)"
	docker-compose -f $(COMPOSE_FILE) exec redis redis-cli BGSAVE
	@echo "$(GREEN)✓ Backup completed: $(BACKUP_DIR)/*$(TIMESTAMP)*$(NC)"

restore: ## Restore data from backup (usage: make restore BACKUP=20231201-120000)
	@if [ -z "$(BACKUP)" ]; then \
		echo "$(RED)Error: Please specify BACKUP timestamp$(NC)"; \
		echo "$(YELLOW)Usage: make restore BACKUP=20231201-120000$(NC)"; \
		exit 1; \
	fi
	@echo "$(BLUE)Restoring from backup: $(BACKUP)$(NC)"
	@echo "$(YELLOW)Restoring database...$(NC)"
	docker-compose -f $(COMPOSE_FILE) exec -T postgres psql -U finpass finpass < $(BACKUP_DIR)/database-$(BACKUP).sql
	@echo "$(GREEN)✓ Restore completed$(NC)"

reset-db: ## Reset database (WARNING: This deletes all data)
	@echo "$(RED)WARNING: This will delete all data!$(NC)"
	@read -p "Are you sure? [y/N] " -n 1 -r; \
	if [[ $$REPLY =~ ^[Yy]$$ ]]; then \
		echo ""; \
		echo "$(BLUE)Resetting database...$(NC)"; \
		docker-compose -f $(COMPOSE_FILE) down -v; \
		docker-compose -f $(COMPOSE_FILE) up -d postgres; \
		sleep 10; \
		$(MAKE) init-db; \
		echo "$(GREEN)✓ Database reset completed$(NC)"; \
	else \
		echo ""; \
		echo "$(YELLOW)Database reset cancelled$(NC)"; \
	fi

# Demo & Testing Commands
demo: ## Run the FinPass demo
	@echo "$(BLUE)Running FinPass demo...$(NC)"
	./scripts/demo.sh

demo-clean: ## Run demo with cleanup
	@echo "$(BLUE)Running FinPass demo with cleanup...$(NC)"
	./scripts/demo.sh --cleanup

benchmark: ## Run performance benchmarks
	@echo "$(BLUE)Running performance benchmarks...$(NC)"
	@echo "$(YELLOW)Backend API benchmark...$(NC)"
	@curl -s -o /dev/null -w "Response time: %{time_total}s\n" http://localhost:8080/health
	@echo "$(YELLOW)Frontend benchmark...$(NC)"
	@curl -s -o /dev/null -w "Response time: %{time_total}s\n" http://localhost/health
	@echo "$(GREEN)✓ Benchmark completed$(NC)"

load-test: ## Run load tests
	@echo "$(BLUE)Running load tests...$(NC)"
	@echo "$(YELLOW)Simulating concurrent requests...$(NC)"
	@for i in {1..10}; do curl -s http://localhost:8080/health > /dev/null & done; wait
	@echo "$(GREEN)✓ Load test completed$(NC)"

# Development Utilities
shell-backend: ## Open shell in backend container
	docker-compose -f $(COMPOSE_FILE) exec backend bash

shell-frontend: ## Open shell in frontend container
	docker-compose -f $(COMPOSE_FILE) exec frontend sh

shell-db: ## Open shell in database container
	docker-compose -f $(COMPOSE_FILE) exec postgres bash

db-client: ## Open database client
	docker-compose -f $(COMPOSE_FILE) exec postgres psql -U finpass -d finpass

redis-client: ## Open Redis client
	docker-compose -f $(COMPOSE_FILE) exec redis redis-cli

# Monitoring Commands
monitoring: ## Start monitoring stack
	@echo "$(BLUE)Starting monitoring services...$(NC)"
	docker-compose -f $(COMPOSE_FILE) --profile monitoring up -d
	@echo "$(GREEN)✓ Monitoring started$(NC)"
	@echo "$(YELLOW)Grafana: http://localhost:3000 (admin/admin123)$(NC)"
	@echo "$(YELLOW)Prometheus: http://localhost:9090$(NC)"

logs-monitoring: ## Show monitoring logs
	docker-compose -f $(COMPOSE_FILE) logs -f prometheus grafana

# Security Commands
security-scan: ## Run security vulnerability scan
	@echo "$(BLUE)Running security scan...$(NC)"
	@echo "$(YELLOW)Scanning backend dependencies...$(NC)"
	docker-compose -f $(COMPOSE_FILE) exec backend ./mvnw dependency-check:check
	@echo "$(YELLOW)Scanning frontend dependencies...$(NC)"
	docker-compose -f $(COMPOSE_FILE) exec frontend npm audit
	@echo "$(GREEN)✓ Security scan completed$(NC)"

# Documentation Commands
docs: ## Generate documentation
	@echo "$(BLUE)Generating documentation...$(NC)"
	@echo "$(YELLOW)API Documentation: http://localhost:8080/swagger-ui$(NC)"
	@echo "$(YELLOW)OpenAPI Spec: http://localhost:8080/api-docs$(NC)"
	@echo "$(GREEN)✓ Documentation available$(NC)"

docs-serve: ## Serve documentation locally
	@echo "$(BLUE)Serving documentation...$(NC)"
	@echo "$(YELLOW)Opening API documentation in browser...$(NC)"
	@command -v xdg-open > /dev/null && xdg-open http://localhost:8080/swagger-ui || \
	 command -v open > /dev/null && open http://localhost:8080/swagger-ui || \
	 echo "$(YELLOW)Please open http://localhost:8080/swagger-ui in your browser$(NC)"

# Utility Commands
version: ## Show version information
	@echo "$(BLUE)FinPass Version Information$(NC)"
	@echo "========================="
	@echo "$(GREEN)Application:$(NC) 1.0.0"
	@echo "$(GREEN)Docker:$(NC) $$(docker --version)"
	@echo "$(GREEN)Docker Compose:$(NC) $$(docker-compose --version)"
	@echo "$(GREEN)Java:$(NC) $$(docker-compose exec -T backend java -version 2>&1 | head -n 1)"
	@echo "$(GREEN)Node.js:$(NC) $$(docker-compose exec -T frontend node --version)"
	@echo "$(GREEN)PostgreSQL:$(NC) $$(docker-compose exec -T postgres psql -U finpass -d finpass -t -c "SELECT version();" 2>/dev/null || echo "Not available")"

update: ## Update dependencies
	@echo "$(BLUE)Updating dependencies...$(NC)"
	@echo "$(YELLOW)Updating backend dependencies...$(NC)"
	docker-compose -f $(COMPOSE_FILE) exec backend ./mvnw versions:display-dependency-updates
	@echo "$(YELLOW)Updating frontend dependencies...$(NC)"
	docker-compose -f $(COMPOSE_FILE) exec frontend npm outdated
	@echo "$(GREEN)✓ Dependency update check completed$(NC)"

# Quick Start Commands
quick-start: ## Quick start for new developers
	@echo "$(BLUE)FinPass Quick Start$(NC)"
	@echo "==================="
	@echo "$(YELLOW)1. Setting up environment...$(NC)"
	@if [ ! -f $(ENV_FILE) ]; then cp .env.example $(ENV_FILE); fi
	@echo "$(YELLOW)2. Building and starting services...$(NC)"
	$(MAKE) build
	$(MAKE) up
	@echo "$(YELLOW)3. Running health check...$(NC)"
	$(MAKE) health
	@echo "$(GREEN)✓ FinPass is now running!$(NC)"
	@echo ""
	@echo "$(BLUE)Access Points:$(NC)"
	@echo "🌐 Frontend: http://localhost"
	@echo "🔧 Backend API: http://localhost:8080"
	@echo "📚 API Docs: http://localhost:8080/swagger-ui"
	@echo "📊 Health: http://localhost:8080/health"
	@echo ""
	@echo "$(BLUE)Next Steps:$(NC)"
	@echo "• Run 'make demo' to see the demo"
	@echo "• Run 'make test' to run tests"
	@echo "• Run 'make logs' to view logs"
	@echo "• Run 'make help' to see all commands"

# CI/CD Commands
ci-test: ## Run tests for CI/CD
	@echo "$(BLUE)Running CI/CD tests...$(NC)"
	$(MAKE) build
	$(MAKE) up
	sleep 30
	$(MAKE) health
	$(MAKE) test
	$(MAKE) down
	@echo "$(GREEN)✓ CI/CD tests passed$(NC)"

ci-build: ## Build for CI/CD
	@echo "$(BLUE)Building for CI/CD...$(NC)"
	docker-compose -f $(COMPOSE_FILE) build --no-cache
	@echo "$(GREEN)✓ CI/CD build completed$(NC)"
