# FinPass Cloud Deployment Guide

This guide provides comprehensive instructions for deploying FinPass on major cloud platforms (AWS, GCP, Azure) with production-ready configurations.

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Prerequisites](#prerequisites)
3. [AWS Deployment](#aws-deployment)
4. [Google Cloud Platform Deployment](#google-cloud-platform-deployment)
5. [Azure Deployment](#azure-deployment)
6. [Security Configuration](#security-configuration)
7. [Monitoring and Logging](#monitoring-and-logging)
8. [Backup and Disaster Recovery](#backup-and-disaster-recovery)
9. [Cost Optimization](#cost-optimization)
10. [Troubleshooting](#troubleshooting)

## Architecture Overview

### Production Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Load Balancer │    │   Load Balancer │    │   Load Balancer │
│   (HTTPS)       │    │   (Internal)    │    │   (Internal)    │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Frontend      │    │    Backend      │    │   PostgreSQL    │
│   (Nginx)       │    │   (Spring)      │    │   (Primary)     │
│   Auto Scaling  │    │   Auto Scaling  │    │   + Replicas    │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         │                       ▼                       │
         │              ┌─────────────────┐              │
         │              │     Redis       │              │
         │              │   (Cluster)     │              │
         └──────────────►│   + Sentinel    │◄─────────────┘
                        └─────────────────┘
```

### Infrastructure Components

- **Load Balancer**: SSL termination, traffic distribution
- **Frontend**: Nginx serving React app, static asset caching
- **Backend**: Spring Boot API, auto-scaling, health checks
- **Database**: PostgreSQL with read replicas
- **Cache**: Redis cluster with sentinel
- **Storage**: Object storage for files and backups
- **Monitoring**: Prometheus, Grafana, alerting
- **Logging**: Centralized log aggregation

## Prerequisites

### General Requirements
- **Cloud Account**: AWS, GCP, or Azure with admin permissions
- **Domain Name**: For SSL certificate configuration
- **SSL Certificate**: Wildcard certificate for your domain
- **CI/CD Pipeline**: GitHub Actions, GitLab CI, or similar
- **Monitoring**: Account with monitoring service (optional)

### Security Requirements
- **IAM Roles**: Properly configured service accounts
- **Network Security**: VPC, security groups, firewall rules
- **Secrets Management**: AWS Secrets Manager, GCP Secret Manager, or Azure Key Vault
- **Compliance**: GDPR, SOC2, HIPAA (if applicable)

### Tool Requirements
- **Docker**: Latest version
- **kubectl**: For Kubernetes deployments
- **Terraform**: For infrastructure as code (optional)
- **Helm**: For Kubernetes package management

## AWS Deployment

### 1. Infrastructure Setup

#### VPC Configuration
```bash
# Create VPC
aws ec2 create-vpc --cidr-block 10.0.0.0/16 --tag-specifications 'ResourceType=vpc,Tags=[{Key=Name,Value=finpass-vpc}]'

# Create subnets
aws ec2 create-subnet --vpc-id vpc-xxxxxxxxx --cidr-block 10.0.1.0/24 --availability-zone us-east-1a
aws ec2 create-subnet --vpc-id vpc-xxxxxxxxx --cidr-block 10.0.2.0/24 --availability-zone us-east-1b

# Create internet gateway
aws ec2 create-internet-gateway --tag-specifications 'ResourceType=internet-gateway,Tags=[{Key=Name,Value=finpass-igw}]'

# Attach gateway to VPC
aws ec2 attach-internet-gateway --vpc-id vpc-xxxxxxxxx --internet-gateway-id igw-xxxxxxxxx
```

#### EKS Cluster
```bash
# Create EKS cluster
aws eks create-cluster \
  --name finpass-cluster \
  --version 1.28 \
  --role-arn arn:aws:iam::ACCOUNT:role/EKSClusterRole \
  --resources-vpc-config subnetIds=subnet-xxxxxxxxx,subnet-yyyyyyyy \
  --logging clusterLogging={apiServer=true,audit=true}

# Update kubeconfig
aws eks update-kubeconfig --name finpass-cluster --region us-east-1
```

### 2. RDS Database Setup

#### PostgreSQL Configuration
```bash
# Create subnet group
aws rds create-db-subnet-group \
  --db-subnet-group-name finpass-subnet-group \
  --db-subnet-group-description "Subnet group for FinPass RDS" \
  --subnet-ids subnet-xxxxxxxxx subnet-yyyyyyyy

# Create primary database
aws rds create-db-instance \
  --db-instance-identifier finpass-db-primary \
  --db-instance-class db.m5.large \
  --engine postgres \
  --engine-version 15.4 \
  --master-username finpass \
  --master-user-password YOUR_SECURE_PASSWORD \
  --allocated-storage 100 \
  --storage-type gp2 \
  --vpc-security-group-ids sg-xxxxxxxxx \
  --db-subnet-group-name finpass-subnet-group \
  --backup-retention-period 7 \
  --multi-az \
  --storage-encrypted \
  --tags Key=Name,Value=finpass-db-primary

# Create read replica
aws rds create-db-instance-read-replica \
  --db-instance-identifier finpass-db-replica \
  --source-db-instance-identifier finpass-db-primary \
  --db-instance-class db.m5.large
```

### 3. ElastiCache Redis Setup

```bash
# Create Redis subnet group
aws elasticache create-cache-subnet-group \
  --cache-subnet-group-name finpass-redis-subnet-group \
  --cache-subnet-group-description "Subnet group for FinPass Redis" \
  --subnet-ids subnet-xxxxxxxxx subnet-yyyyyyyy

# Create Redis cluster
aws elasticache create-replication-group \
  --replication-group-id finpass-redis \
  --replication-group-description "FinPass Redis cluster" \
  --engine redis \
  --engine-version 7.0 \
  --cache-node-type cache.m5.large \
  --cache-parameter-group default.redis7 \
  --security-group-ids sg-xxxxxxxxx \
  --cache-subnet-group-name finpass-redis-subnet-group \
  --automatic-failover-enabled \
  --multi-az-enabled \
  --at-rest-encryption-enabled \
  --transit-encryption-enabled \
  --auth-token YOUR_REDIS_PASSWORD
```

### 4. Application Deployment

#### Kubernetes Manifests
```yaml
# namespace.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: finpass

---
# configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: finpass-config
  namespace: finpass
data:
  SPRING_PROFILES_ACTIVE: "prod"
  BLOCKCHAIN_NETWORK: "mainnet"
  FINPASS_ENVIRONMENT: "production"

---
# secret.yaml
apiVersion: v1
kind: Secret
metadata:
  name: finpass-secrets
  namespace: finpass
type: Opaque
data:
  POSTGRES_PASSWORD: <base64-encoded-password>
  JWT_SECRET: <base64-encoded-jwt-secret>
  REDIS_PASSWORD: <base64-encoded-redis-password>

---
# backend-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: finpass-backend
  namespace: finpass
spec:
  replicas: 3
  selector:
    matchLabels:
      app: finpass-backend
  template:
    metadata:
      labels:
        app: finpass-backend
    spec:
      containers:
      - name: backend
        image: finpass/backend:latest
        ports:
        - containerPort: 8080
        envFrom:
        - configMapRef:
            name: finpass-config
        - secretRef:
            name: finpass-secrets
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /health/live
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /health/ready
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 5

---
# backend-service.yaml
apiVersion: v1
kind: Service
metadata:
  name: finpass-backend-service
  namespace: finpass
spec:
  selector:
    app: finpass-backend
  ports:
  - protocol: TCP
    port: 8080
    targetPort: 8080
  type: ClusterIP

---
# frontend-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: finpass-frontend
  namespace: finpass
spec:
  replicas: 2
  selector:
    matchLabels:
      app: finpass-frontend
  template:
    metadata:
      labels:
        app: finpass-frontend
    spec:
      containers:
      - name: frontend
        image: finpass/frontend:latest
        ports:
        - containerPort: 80
        resources:
          requests:
            memory: "128Mi"
            cpu: "100m"
          limits:
            memory: "256Mi"
            cpu: "200m"

---
# ingress.yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: finpass-ingress
  namespace: finpass
  annotations:
    kubernetes.io/ingress.class: "nginx"
    cert-manager.io/cluster-issuer: "letsencrypt-prod"
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
spec:
  tls:
  - hosts:
    - finpass.io
    - api.finpass.io
    secretName: finpass-tls
  rules:
  - host: finpass.io
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: finpass-frontend-service
            port:
              number: 80
  - host: api.finpass.io
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: finpass-backend-service
            port:
              number: 8080
```

### 5. Load Balancer Configuration

#### Application Load Balancer
```bash
# Create target groups
aws elbv2 create-target-group \
  --name finpass-backend-tg \
  --protocol HTTP \
  --port 8080 \
  --target-type ip \
  --health-check-path /health \
  --health-check-interval-seconds 30 \
  --health-check-timeout-seconds 5 \
  --healthy-threshold-count 2 \
  --unhealthy-threshold-count 3

aws elbv2 create-target-group \
  --name finpass-frontend-tg \
  --protocol HTTP \
  --port 80 \
  --target-type ip \
  --health-check-path /health \
  --health-check-interval-seconds 30

# Create load balancer
aws elbv2 create-load-balancer \
  --name finpass-alb \
  --subnets subnet-xxxxxxxxx subnet-yyyyyyyy \
  --security-groups sg-xxxxxxxxx \
  --scheme internet-facing \
  --type application \
  --ip-address-type ipv4

# Create listeners
aws elbv2 create-listener \
  --load-balancer-arn arn:aws:elasticloadbalancing:... \
  --protocol HTTPS \
  --port 443 \
  --certificates CertificateArn=arn:aws:acm:... \
  --default-actions Type=forward,TargetGroupArn=arn:aws:elasticloadbalancing:...
```

## Google Cloud Platform Deployment

### 1. Project Setup

```bash
# Set project
gcloud config set project finpass-production

# Enable APIs
gcloud services enable container.googleapis.com
gcloud services enable sqladmin.googleapis.com
gcloud services enable redis.googleapis.com
gcloud services enable cloudbuild.googleapis.com
```

### 2. GKE Cluster Setup

```bash
# Create GKE cluster
gcloud container clusters create finpass-cluster \
  --zone us-central1-a \
  --num-nodes 2 \
  --machine-type e2-standard-2 \
  --enable-autoscaling \
  --min-nodes 1 \
  --max-nodes 10 \
  --enable-autorepair \
  --enable-autoupgrade \
  --enable-ip-alias \
  --enable-private-nodes \
  --master-authorized-networks 0.0.0.0/0

# Get credentials
gcloud container clusters get-credentials finpass-cluster --zone us-central1-a
```

### 3. Cloud SQL Setup

```bash
# Create Cloud SQL instance
gcloud sql instances create finpass-db \
  --database-version POSTGRES_15 \
  --tier db-custom-4-16384 \
  --region us-central1 \
  --storage-size 100GB \
  --storage-type SSD \
  --backup-start-time 02:00 \
  --enable-bin-log \
  --retained-backups-count 7 \
  --database-version POSTGRES_15

# Create database
gcloud sql databases create finpass --instance finpass-db

# Create read replica
gcloud sql instances create finpass-db-replica \
  --master-instance-name finpass-db \
  --tier db-custom-4-16384 \
  --region us-central1
```

### 4. Memorystore Redis Setup

```bash
# Create Redis instance
gcloud redis instances create finpass-redis \
  --region us-central1 \
  --tier standard \
  --size 4 \
  --redis-version redis_7_0 \
  --display-name "FinPass Redis" \
  --authorized-network 0.0.0.0/0
```

## Azure Deployment

### 1. Resource Group Setup

```bash
# Create resource group
az group create \
  --name finpass-rg \
  --location eastus

# Create AKS cluster
az aks create \
  --resource-group finpass-rg \
  --name finpass-cluster \
  --node-count 2 \
  --node-vm-size Standard_D2s_v3 \
  --enable-cluster-autoscaler \
  --min-count 1 \
  --max-count 10 \
  --enable-addons monitoring \
  --attach-acr finpass-acr \
  --generate-ssh-keys
```

### 2. Azure Database Setup

```bash
# Create PostgreSQL server
az postgres server create \
  --resource-group finpass-rg \
  --name finpass-db \
  --location eastus \
  --admin-user finpass \
  --admin-password YOUR_SECURE_PASSWORD \
  --sku-name GP_Gen5_4 \
  --version 15 \
  --storage-size 102400 \
  --backup-retention 7

# Configure firewall
az postgres server firewall-rule create \
  --resource-group finpass-rg \
  --server-name finpass-db \
  --name allow-azure-ip \
  --start-ip-address 0.0.0.0 \
  --end-ip-address 0.0.0.0

# Create database
az postgres db create \
  --resource-group finpass-rg \
  --server-name finpass-db \
  --name finpass
```

### 3. Azure Cache Setup

```bash
# Create Redis cache
az redis create \
  --resource-group finpass-rg \
  --name finpass-redis \
  --location eastus \
  --sku Premium \
  --vm-size P1 \
  --redis-version 6 \
  --enable-non-ssl-port false \
  --static-ip 10.0.0.5
```

## Security Configuration

### 1. SSL/TLS Configuration

#### AWS Certificate Manager
```bash
# Request certificate
aws acm request-certificate \
  --domain-name finpass.io \
  --subject-alternative-names api.finpass.io \
  --validation-method DNS

# Validate certificate (add CNAME records to DNS)
# Wait for certificate validation
```

#### Google Cloud Certificate Manager
```bash
# Create SSL certificate
gcloud compute ssl-certificates create finpass-ssl \
  --domains finpass.io,api.finpass.io \
  --certificate finpass.crt \
  --private-key finpass.key
```

### 2. Network Security

#### Security Groups (AWS)
```bash
# Create security group
aws ec2 create-security-group \
  --group-name finpass-sg \
  --description "Security group for FinPass" \
  --vpc-id vpc-xxxxxxxxx

# Allow HTTP traffic
aws ec2 authorize-security-group-ingress \
  --group-id sg-xxxxxxxxx \
  --protocol tcp \
  --port 80 \
  --cidr 0.0.0.0/0

# Allow HTTPS traffic
aws ec2 authorize-security-group-ingress \
  --group-id sg-xxxxxxxxx \
  --protocol tcp \
  --port 443 \
  --cidr 0.0.0.0/0

# Allow backend traffic (internal only)
aws ec2 authorize-security-group-ingress \
  --group-id sg-xxxxxxxxx \
  --protocol tcp \
  --port 8080 \
  --source-group sg-xxxxxxxxx
```

### 3. Secrets Management

#### AWS Secrets Manager
```bash
# Create secret for database credentials
aws secretsmanager create-secret \
  --name finpass/db-credentials \
  --description "Database credentials for FinPass" \
  --secret-string '{"username":"finpass","password":"YOUR_SECURE_PASSWORD"}'

# Create secret for JWT
aws secretsmanager create-secret \
  --name finpass/jwt-secret \
  --description "JWT secret for FinPass" \
  --secret-string 'YOUR_JWT_SECRET'
```

## Monitoring and Logging

### 1. CloudWatch Setup (AWS)

```bash
# Create log groups
aws logs create-log-group --log-group-name /finpass/backend
aws logs create-log-group --log-group-name /finpass/frontend

# Create metric filters
aws logs put-metric-filter \
  --log-group-name /finpass/backend \
  --filter-name ErrorCount \
  --filter-pattern "ERROR" \
  --metric-transformations metricName=ErrorCount,metricNamespace=FinPass,metricValue=1

# Create alarms
aws cloudwatch put-metric-alarm \
  --alarm-name FinPassHighErrorRate \
  --alarm-description "High error rate detected" \
  --metric-name ErrorCount \
  --namespace FinPass \
  --statistic Sum \
  --period 300 \
  --threshold 10 \
  --comparison-operator GreaterThanThreshold \
  --evaluation-periods 2
```

### 2. Prometheus and Grafana

#### Prometheus Configuration
```yaml
# prometheus.yaml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'finpass-backend'
    kubernetes_sd_configs:
    - role: pod
    relabel_configs:
    - source_labels: [__meta_kubernetes_pod_label_app]
      action: keep
      regex: finpass-backend
    - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_scrape]
      action: keep
      regex: true

  - job_name: 'finpass-frontend'
    kubernetes_sd_configs:
    - role: pod
    relabel_configs:
    - source_labels: [__meta_kubernetes_pod_label_app]
      action: keep
      regex: finpass-frontend
```

#### Grafana Dashboards
```json
{
  "dashboard": {
    "title": "FinPass Monitoring",
    "panels": [
      {
        "title": "Request Rate",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(http_requests_total[5m])",
            "legendFormat": "{{method}} {{status}}"
          }
        ]
      },
      {
        "title": "Error Rate",
        "type": "singlestat",
        "targets": [
          {
            "expr": "rate(http_requests_total{status=~\"5..\"}[5m])",
            "legendFormat": "5xx Errors"
          }
        ]
      }
    ]
  }
}
```

## Backup and Disaster Recovery

### 1. Database Backups

#### Automated Backups
```bash
# AWS RDS automated backups
aws rds modify-db-instance \
  --db-instance-identifier finpass-db-primary \
  --backup-retention-period 30 \
  --preferred-backup-window 02:00-03:00 \
  --preferred-maintenance-window sun:03:00-sun:04:00

# Manual backup
aws rds create-db-snapshot \
  --db-instance-identifier finpass-db-primary \
  --db-snapshot-identifier finpass-backup-$(date +%Y%m%d-%H%M%S)
```

#### Cross-Region Replication
```bash
# Create cross-region read replica
aws rds create-db-instance-read-replica \
  --db-instance-identifier finpass-db-dr \
  --source-db-instance-identifier finpass-db-primary \
  --availability-zone us-west-2a \
  --region us-west-2
```

### 2. Application Backups

#### Kubernetes Backup
```bash
# Install Velero
kubectl apply -f https://github.com/vmware-tanzu/velero/releases/download/v1.12.0/velero-v1.12.0.yaml

# Create backup
velero backup create finpass-backup --include-namespaces finpass

# Schedule daily backups
velero schedule create finpass-daily --schedule="0 2 * * *" --include-namespaces finpass
```

### 3. Disaster Recovery Plan

#### Recovery Procedures
```bash
# 1. Restore database from backup
aws rds restore-db-instance-from-db-snapshot \
  --db-instance-identifier finpass-db-restored \
  --db-snapshot-identifier finpass-backup-20231201-020000

# 2. Update application configuration
kubectl patch configmap finpass-config \
  --patch '{"data":{"SPRING_DATASOURCE_URL":"jdbc:postgresql://finpass-db-restored.xxx.us-east-1.rds.amazonaws.com:5432/finpass"}}'

# 3. Restart applications
kubectl rollout restart deployment/finpass-backend -n finpass
kubectl rollout restart deployment/finpass-frontend -n finpass
```

## Cost Optimization

### 1. Resource Optimization

#### Right-Sizing Instances
```bash
# Monitor resource usage
kubectl top nodes
kubectl top pods -n finpass

# Adjust resource requests and limits
# Based on actual usage patterns
```

#### Auto Scaling Configuration
```yaml
# horizontal-pod-autoscaler.yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: finpass-backend-hpa
  namespace: finpass
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: finpass-backend
  minReplicas: 2
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
```

### 2. Storage Optimization

#### Lifecycle Policies
```bash
# S3 lifecycle policy for backups
aws s3api put-bucket-lifecycle-configuration \
  --bucket finpass-backups \
  --lifecycle-configuration '{
    "Rules": [
      {
        "ID": "BackupLifecycle",
        "Status": "Enabled",
        "Transitions": [
          {
            "Days": 30,
            "StorageClass": "STANDARD_IA"
          },
          {
            "Days": 90,
            "StorageClass": "GLACIER"
          },
          {
            "Days": 365,
            "StorageClass": "DEEP_ARCHIVE"
          }
        ]
      }
    ]
  }'
```

## Troubleshooting

### Common Issues

#### Pod Crashes
```bash
# Check pod status
kubectl get pods -n finpass

# Check pod logs
kubectl logs -f deployment/finpass-backend -n finpass

# Describe pod for detailed information
kubectl describe pod <pod-name> -n finpass
```

#### Database Connection Issues
```bash
# Test database connectivity
kubectl exec -it deployment/finpass-backend -n finpass -- nc -zv finpass-db-primary.xxx.us-east-1.rds.amazonaws.com 5432

# Check database logs
aws rds describe-db-log-files --db-instance-identifier finpass-db-primary
```

#### Performance Issues
```bash
# Check resource utilization
kubectl top nodes
kubectl top pods -n finpass

# Check application metrics
curl http://api.finpass.io/health/detailed
```

### Emergency Procedures

#### Service Outage
```bash
# 1. Check service status
kubectl get services -n finpass
kubectl get ingress -n finpass

# 2. Check load balancer
aws elbv2 describe-target-groups --names finpass-backend-tg

# 3. Restart services if needed
kubectl rollout restart deployment/finpass-backend -n finpass
```

#### Database Issues
```bash
# 1. Check database status
aws rds describe-db-instances --db-instance-identifier finpass-db-primary

# 2. Failover to replica if needed
aws rds reboot-db-instance --db-instance-identifier finpass-db-primary --force-failover
```

## Support and Maintenance

### Monitoring Checklist
- [ ] Application health checks passing
- [ ] Database replication lag < 5 seconds
- [ ] Error rate < 1%
- [ ] Response time < 500ms (95th percentile)
- [ ] Disk usage < 80%
- [ ] Memory usage < 85%
- [ ] CPU usage < 75%

### Maintenance Tasks
- [ ] Weekly: Review logs and metrics
- [ ] Monthly: Apply security patches
- [ ] Quarterly: Review and update disaster recovery plan
- [ ] Annually: Capacity planning and optimization

### Emergency Contacts
- **Primary Support**: support@finpass.io
- **On-call Engineer**: +1-555-0123
- **Infrastructure Lead**: infrastructure@finpass.io

This deployment guide provides a comprehensive foundation for running FinPass in production environments. Adjust configurations based on your specific requirements and compliance needs.
