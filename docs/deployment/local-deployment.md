# FinPass Local Deployment Guide

This guide provides step-by-step instructions for deploying FinPass locally using Docker and Docker Compose.

## Prerequisites

### Required Software
- **Docker** (version 20.10 or higher)
- **Docker Compose** (version 2.0 or higher)
- **Git** (for cloning the repository)
- **Make** (optional, for using the provided Makefile)

### System Requirements
- **RAM**: Minimum 4GB, Recommended 8GB
- **Storage**: Minimum 10GB free disk space
- **CPU**: 2+ cores recommended

## Quick Start

### 1. Clone the Repository
```bash
git clone https://github.com/finpass/finpass.git
cd finpass
```

### 2. Configure Environment
```bash
# Copy the environment template
cp .env.example .env

# Edit the environment file with your preferences
nano .env
```

### 3. Start the Services
```bash
# Start all services
docker-compose up -d

# Or use the Makefile
make up
```

### 4. Verify the Deployment
```bash
# Check service status
docker-compose ps

# Check logs
docker-compose logs -f

# Run health checks
make health
```

## Detailed Setup

### Environment Configuration

The `.env` file contains all configuration options. Key settings:

#### Database Configuration
```bash
POSTGRES_DB=finpass
POSTGRES_USER=finpass
POSTGRES_PASSWORD=your-secure-password
```

#### Security Configuration
```bash
JWT_SECRET=your-256-bit-jwt-secret-key
BLOCKCHAIN_PRIVATE_KEY=your-blockchain-private-key
```

#### Service Ports
```bash
FRONTEND_PORT=80          # Frontend application
BACKEND_PORT=8080         # Backend API
POSTGRES_PORT=5432        # PostgreSQL database
REDIS_PORT=6379           # Redis cache
```

### Service Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Frontend      │    │    Backend      │    │   PostgreSQL    │
│   (nginx:80)    │◄──►│   (Spring:8080)│◄──►│   (5432)        │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         │                       ▼                       │
         │              ┌─────────────────┐              │
         │              │     Redis       │              │
         └──────────────►│   (6379)        │◄─────────────┘
                        └─────────────────┘
```

## Service Access

### Frontend Application
- **URL**: http://localhost
- **Description**: React TypeScript application
- **Features**: DID management, credential wallet, payment interface

### Backend API
- **URL**: http://localhost:8080
- **Description**: Spring Boot REST API
- **Endpoints**: 
  - `/api/issuer/*` - Credential management
  - `/api/verifier/*` - Verification services
  - `/api/payments/*` - Payment processing
  - `/api/audit/*` - Audit logging

### API Documentation
- **Swagger UI**: http://localhost/swagger-ui
- **OpenAPI Spec**: http://localhost/api-docs

### Database Access
- **PostgreSQL**: localhost:5432
- **Redis**: localhost:6379
- **Admin Tools**: Use your preferred database client

## Development Workflow

### Making Changes

#### Frontend Changes
```bash
# Rebuild frontend
docker-compose build frontend

# Restart frontend service
docker-compose restart frontend

# View frontend logs
docker-compose logs -f frontend
```

#### Backend Changes
```bash
# Rebuild backend
docker-compose build backend

# Restart backend service
docker-compose restart backend

# View backend logs
docker-compose logs -f backend
```

#### Database Changes
```bash
# Access database shell
docker-compose exec postgres psql -U finpass -d finpass

# Run database migrations
docker-compose exec backend ./mvnw flyway:migrate

# Reset database
docker-compose down -v
docker-compose up -d postgres
```

### Testing

#### Run Tests
```bash
# Run backend tests
docker-compose exec backend ./mvnw test

# Run frontend tests
docker-compose exec frontend npm test

# Run integration tests
docker-compose exec backend ./mvnw test -Dtest=*IntegrationTest
```

#### Health Checks
```bash
# Basic health check
curl http://localhost:8080/health

# Detailed health check
curl http://localhost:8080/health/detailed

# Readiness probe
curl http://localhost:8080/health/ready

# Liveness probe
curl http://localhost:8080/health/live
```

## Troubleshooting

### Common Issues

#### Port Conflicts
```bash
# Check what's using the ports
netstat -tulpn | grep :80
netstat -tulpn | grep :8080

# Change ports in .env file
FRONTEND_PORT=8081
BACKEND_PORT=8081
```

#### Memory Issues
```bash
# Check Docker resource usage
docker stats

# Increase Docker memory allocation
# In Docker Desktop: Settings > Resources > Memory
```

#### Database Connection Issues
```bash
# Check database logs
docker-compose logs postgres

# Test database connection
docker-compose exec postgres pg_isready -U finpass

# Reset database
docker-compose down -v
docker-compose up -d
```

#### Service Startup Issues
```bash
# Check service status
docker-compose ps

# View service logs
docker-compose logs backend
docker-compose logs frontend
docker-compose logs postgres

# Restart specific service
docker-compose restart backend
```

### Debug Mode

#### Enable Debug Logging
```bash
# Add to .env
LOG_LEVEL=DEBUG
JAVA_OPTS="-Xmx1g -Xms512m -Dlogging.level.com.finpass=DEBUG"

# Restart services
docker-compose restart backend
```

#### Access Service Shell
```bash
# Backend shell
docker-compose exec backend bash

# Frontend shell
docker-compose exec frontend sh

# Database shell
docker-compose exec postgres bash
```

## Data Management

### Backups
```bash
# Create database backup
docker-compose exec postgres pg_dump -U finpass finpass > backup.sql

# Create volume backup
docker run --rm -v finpass_postgres_data:/data -v $(pwd):/backup ubuntu tar czf /backup/postgres_backup.tar.gz -C /data .

# Restore database backup
docker-compose exec -T postgres psql -U finpass finpass < backup.sql
```

### Data Persistence
- **Database data**: Stored in `postgres_data` volume
- **Redis data**: Stored in `redis_data` volume
- **Application logs**: Stored in `backend_logs` volume
- **SSL certificates**: Stored in `traefik_letsencrypt` volume

## Performance Optimization

### Resource Allocation
```bash
# Optimize Docker memory limits
# In docker-compose.yaml:
services:
  backend:
    deploy:
      resources:
        limits:
          memory: 1G
        reservations:
          memory: 512M
```

### Database Optimization
```bash
# Connection pool settings in .env
DATABASE_POOL_SIZE=20
DATABASE_CONNECTION_TIMEOUT=30000

# PostgreSQL configuration
# Add to docker/postgres/postgresql.conf
shared_buffers = 256MB
effective_cache_size = 1GB
```

### Caching
```bash
# Enable Redis caching
REDIS_ENABLED=true
REDIS_TTL=3600
```

## Security Considerations

### Default Credentials
⚠️ **Important**: Change all default passwords before production use

#### Database
```bash
POSTGRES_PASSWORD=your-secure-password
```

#### JWT
```bash
JWT_SECRET=your-256-bit-jwt-secret-key
```

#### Blockchain
```bash
BLOCKCHAIN_PRIVATE_KEY=your-secure-private-key
```

### Network Security
```bash
# Use custom networks
docker network create --driver bridge finpass-network

# Limit exposed ports
# Comment out port mappings in docker-compose.yaml for production
```

### SSL/TLS
```bash
# Enable HTTPS with Traefik
# Set ACME email for Let's Encrypt
ACME_EMAIL=admin@yourdomain.com

# Configure domain names
FRONTEND_HOST=yourdomain.com
BACKEND_HOST=api.yourdomain.com
```

## Monitoring and Logging

### View Logs
```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f backend

# Last 100 lines
docker-compose logs --tail=100 backend
```

### Monitoring Stack
```bash
# Start monitoring services
docker-compose --profile monitoring up -d

# Access Grafana
http://localhost:3000
# Username: admin
# Password: admin123

# Access Prometheus
http://localhost:9090
```

## Cleanup

### Remove All Services
```bash
# Stop and remove containers
docker-compose down

# Remove volumes (WARNING: This deletes all data)
docker-compose down -v

# Remove images
docker-compose down --rmi all
```

### Clean Docker System
```bash
# Remove unused containers, networks, images
docker system prune -a

# Remove unused volumes
docker volume prune
```

## Makefile Commands

The project includes a Makefile for common operations:

```bash
# Start services
make up

# Stop services
make down

# View logs
make logs

# Run tests
make test

# Health check
make health

# Backup data
make backup

# Clean up
make clean
```

## Next Steps

After successful local deployment:

1. **Review the API documentation** at http://localhost/swagger-ui
2. **Run the demo script** to test functionality
3. **Explore the frontend** at http://localhost
4. **Check monitoring dashboards** if enabled
5. **Review logs** for any issues
6. **Customize configuration** for your needs

## Support

For issues and questions:

1. Check the [troubleshooting section](#troubleshooting)
2. Review service logs: `docker-compose logs`
3. Check the [GitHub Issues](https://github.com/finpass/finpass/issues)
4. Contact the FinPass team

## Production Deployment

For production deployment, see the [Cloud Deployment Guide](./cloud-deployment.md).
