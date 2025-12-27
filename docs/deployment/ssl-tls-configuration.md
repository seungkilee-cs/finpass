# FinPass SSL/TLS Configuration Guide

This guide provides comprehensive instructions for configuring SSL/TLS certificates for FinPass deployments, including local development, staging, and production environments.

## Table of Contents

1. [Overview](#overview)
2. [Local Development](#local-development)
3. [Self-Signed Certificates](#self-signed-certificates)
4. [Let's Encrypt](#lets-encrypt)
5. [Commercial Certificates](#commercial-certificates)
6. [Cloud Provider SSL](#cloud-provider-ssl)
7. [Configuration](#configuration)
8. [Testing](#testing)
9. [Troubleshooting](#troubleshooting)

## Overview

FinPass supports SSL/TLS encryption for all network communications:

- **Frontend**: HTTPS termination at load balancer/nginx
- **Backend**: HTTPS for API endpoints
- **Database**: TLS encryption for connections
- **Redis**: TLS encryption for cache connections
- **Inter-service**: Mutual TLS for service-to-service communication

### Security Benefits

- **Encryption**: All data in transit is encrypted
- **Authentication**: Certificates verify server identity
- **Integrity**: Prevents man-in-the-middle attacks
- **Compliance**: Meets security standards (GDPR, SOC2, HIPAA)

## Local Development

### Using mkcert (Recommended)

mkcert creates locally-trusted development certificates with a single command:

#### Installation

```bash
# macOS
brew install mkcert nss
brew install --cask firefox

# Linux
sudo apt install libnss3-tools
wget -O - https://dl.filippo.io/mkcert/latest?os=linux | tar zxvf - && sudo mv mkcert-v*-linux-amd64 /usr/local/bin/mkcert

# Windows
choco install mkcert
```

#### Setup and Certificate Creation

```bash
# Create local CA
mkcert -install

# Create certificates for FinPass
mkcert finpass.io localhost 127.0.0.1 ::1

# Move certificates to project directory
mkdir -p docker/nginx/ssl
mv finpass.io+4.pem docker/nginx/ssl/cert.pem
mv finpass.io+4-key.pem docker/nginx/ssl/key.pem
```

#### Docker Compose Configuration

```yaml
# docker-compose.override.yaml
version: '3.8'

services:
  frontend:
    volumes:
      - ./docker/nginx/ssl:/etc/nginx/ssl:ro
    ports:
      - "443:443"
    environment:
      - SSL_ENABLED=true

  backend:
    environment:
      - SERVER_SSL_ENABLED=true
      - SERVER_SSL_KEY_STORE=classpath:keystore.p12
      - SERVER_SSL_KEY_STORE_PASSWORD=changeit
```

### Using OpenSSL (Alternative)

```bash
# Create directory for certificates
mkdir -p docker/nginx/ssl
cd docker/nginx/ssl

# Generate private key
openssl genrsa -out finpass.key 2048

# Create certificate signing request
openssl req -new -key finpass.key -out finpass.csr \
  -subj "/C=US/ST=State/L=City/O=FinPass/CN=localhost"

# Generate self-signed certificate
openssl x509 -req -days 365 -in finpass.csr -signkey finpass.key -out finpass.crt

# Create DH parameters for perfect forward secrecy
openssl dhparam -out dhparam.pem 2048
```

## Self-Signed Certificates

For staging or internal environments, you can use self-signed certificates:

### Certificate Generation Script

```bash
#!/bin/bash
# generate-certs.sh

DOMAIN=${1:-finpass.io}
VALIDITY_DAYS=${2:-365}

echo "Generating certificates for $DOMAIN"

# Create directory structure
mkdir -p certs/{private,csr,certs}

# Generate CA private key
openssl genrsa -out certs/private/ca.key 4096

# Generate CA certificate
openssl req -new -x509 -days $VALIDITY_DAYS -key certs/private/ca.key \
  -out certs/certs/ca.crt \
  -subj "/C=US/ST=State/L=City/O=FinPass CA/CN=FinPass CA"

# Generate server private key
openssl genrsa -out certs/private/$DOMAIN.key 2048

# Generate server CSR
openssl req -new -key certs/private/$DOMAIN.key -out certs/csr/$DOMAIN.csr \
  -subj "/C=US/ST=State/L=City/O=FinPass/CN=$DOMAIN"

# Create server certificate configuration
cat > certs/$DOMAIN.conf << EOF
authorityKeyIdentifier=keyid,issuer
basicConstraints=CA:FALSE
keyUsage = digitalSignature, nonRepudiation, keyEncipherment, dataEncipherment
subjectAltName = @alt_names

[alt_names]
DNS.1 = $DOMAIN
DNS.2 = *.$DOMAIN
DNS.3 = localhost
IP.1 = 127.0.0.1
IP.2 = ::1
EOF

# Generate server certificate
openssl x509 -req -in certs/csr/$DOMAIN.csr \
  -CA certs/certs/ca.crt -CAkey certs/private/ca.key -CAcreateserial \
  -out certs/certs/$DOMAIN.crt -days $VALIDITY_DAYS \
  -extensions v3_req -extfile certs/$DOMAIN.conf

echo "Certificates generated successfully!"
echo "Private key: certs/private/$DOMAIN.key"
echo "Certificate: certs/certs/$DOMAIN.crt"
echo "CA certificate: certs/certs/ca.crt"
```

### Usage

```bash
# Generate certificates
chmod +x generate-certs.sh
./generate-certs.sh finpass.io 365

# Copy to nginx directory
cp certs/certs/finpass.io.crt docker/nginx/ssl/cert.pem
cp certs/private/finpass.io.key docker/nginx/ssl/key.pem
```

## Let's Encrypt

Let's Encrypt provides free, trusted SSL certificates for production deployments.

### Using Certbot

#### Installation

```bash
# Ubuntu/Debian
sudo apt update
sudo apt install certbot python3-certbot-nginx

# CentOS/RHEL
sudo yum install certbot python3-certbot-nginx

# Docker
docker run -it --rm --name certbot \
  -v "/etc/letsencrypt:/etc/letsencrypt" \
  -v "/var/lib/letsencrypt:/var/lib/letsencrypt" \
  -p 80:80 \
  certbot/certbot certonly --standalone
```

#### Certificate Generation

```bash
# Single domain
sudo certbot --nginx -d finpass.io

# Multiple domains
sudo certbot --nginx -d finpass.io -d api.finpass.io -d www.finpass.io

# Wildcard certificate
sudo certbot certonly --manual --preferred-challenges dns \
  -d finpass.io -d *.finpass.io
```

#### Automatic Renewal

```bash
# Test renewal
sudo certbot renew --dry-run

# Setup automatic renewal (cron)
echo "0 12 * * * /usr/bin/certbot renew --quiet" | sudo crontab -

# Or use systemd timer
sudo systemctl enable certbot.timer
sudo systemctl start certbot.timer
```

### Using Traefik (Recommended for Docker)

Traefik automatically handles Let's Encrypt certificates:

```yaml
# docker-compose.traefik.yaml
version: '3.8'

services:
  traefik:
    image: traefik:v3.0
    command:
      - "--api.dashboard=true"
      - "--providers.docker=true"
      - "--providers.docker.exposedbydefault=false"
      - "--entrypoints.web.address=:80"
      - "--entrypoints.websecure.address=:443"
      - "--certificatesresolvers.letsencrypt.acme.tlschallenge=true"
      - "--certificatesresolvers.letsencrypt.acme.email=admin@finpass.io"
      - "--certificatesresolvers.letsencrypt.acme.storage=/letsencrypt/acme.json"
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock:ro
      - traefik_letsencrypt:/letsencrypt
    labels:
      - "traefik.enable=true"
      - "traefik.http.routers.traefik.rule=Host(`traefik.finpass.io`)"
      - "traefik.http.routers.traefik.entrypoints=websecure"
      - "traefik.http.routers.traefik.tls.certresolver=letsencrypt"

volumes:
  traefik_letsencrypt:
```

## Commercial Certificates

For enterprise deployments, you might use commercial certificate authorities:

### Certificate Authorities

- **DigiCert**: Enterprise certificates with extended validation
- **GlobalSign**: Business and enterprise SSL certificates
- **Sectigo**: Wide range of certificate types
- **Entrust**: High-assurance certificates

### Certificate Types

#### Domain Validation (DV)
- Quick verification process
- Basic encryption and authentication
- Suitable for most applications

#### Organization Validation (OV)
- Business identity verification
- Enhanced trust indicators
- Good for corporate sites

#### Extended Validation (EV)
- Strictest verification process
- Green address bar in browsers
- Maximum user trust

### Installation Process

```bash
# 1. Generate CSR
openssl req -new -newkey rsa:2048 -nodes -keyout finpass.key -out finpass.csr \
  -subj "/C=US/ST=State/L=City/O=FinPass Inc/CN=finpass.io"

# 2. Submit CSR to certificate authority
# 3. Download certificates (usually .crt and .ca-bundle files)

# 4. Combine certificates
cat finpass.crt ca-bundle.crt > finpass-fullchain.crt

# 5. Install in nginx
cp finpass.key /etc/nginx/ssl/
cp finpass-fullchain.crt /etc/nginx/ssl/
```

## Cloud Provider SSL

### AWS Certificate Manager (ACM)

```bash
# Request certificate
aws acm request-certificate \
  --domain-name finpass.io \
  --subject-alternative-names api.finpass.io *.finpass.io \
  --validation-method DNS

# Validate certificate (add CNAME records to DNS)
aws acm describe-certificate \
  --certificate-arn arn:aws:acm:... \
  --query 'Certificate.DomainValidationOptions[*].{Name:ResourceRecord,Value:Value}'

# Use with Load Balancer
aws elbv2 create-listener \
  --load-balancer-arn arn:aws:elasticloadbalancing:... \
  --protocol HTTPS \
  --port 443 \
  --certificates CertificateArn=arn:aws:acm:...
```

### Google Cloud Certificate Manager

```bash
# Create SSL certificate
gcloud compute ssl-certificates create finpass-ssl \
  --domains finpass.io,api.finpass.io \
  --certificate finpass.crt \
  --private-key finpass.key

# Use with load balancer
gcloud compute target-https-proxies create finpass-https-proxy \
  --ssl-certificates finpass-ssl \
  --url-map finpass-url-map
```

### Azure Application Gateway

```bash
# Create certificate
az network application-gateway ssl-cert create \
  --resource-group finpass-rg \
  --gateway-name finpass-ag \
  --name finpass-ssl-cert \
  --cert-file finpass.crt \
  --key-file finpass.key
```

## Configuration

### Nginx Configuration

```nginx
# /etc/nginx/conf.d/ssl.conf
server {
    listen 443 ssl http2;
    server_name finpass.io api.finpass.io;

    # SSL certificates
    ssl_certificate /etc/nginx/ssl/cert.pem;
    ssl_certificate_key /etc/nginx/ssl/key.pem;

    # SSL configuration
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-RSA-AES256-GCM-SHA512:DHE-RSA-AES256-GCM-SHA512:ECDHE-RSA-AES256-GCM-SHA384:DHE-RSA-AES256-GCM-SHA384;
    ssl_prefer_server_ciphers off;
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 10m;

    # Security headers
    add_header Strict-Transport-Security "max-age=63072000; includeSubDomains; preload" always;
    add_header X-Frame-Options DENY always;
    add_header X-Content-Type-Options nosniff always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header Referrer-Policy "strict-origin-when-cross-origin" always;

    # Application configuration
    location / {
        proxy_pass http://frontend:80;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /api/ {
        proxy_pass http://backend:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}

# HTTP to HTTPS redirect
server {
    listen 80;
    server_name finpass.io api.finpass.io;
    return 301 https://$host$request_uri;
}
```

### Spring Boot Configuration

```yaml
# application-ssl.yml
server:
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${SSL_KEYSTORE_PASSWORD:changeit}
    key-store-type: PKCS12
    key-alias: finpass
  port: 8443

# Force HTTPS
security:
  require-ssl: true

# Database SSL
spring:
  datasource:
    url: jdbc:postgresql://postgres:5432/finpass?ssl=true&sslmode=verify-full
    ssl:
      mode: verify-full
      factory: org.postgresql.ssl.DefaultJavaSSLFactory

# Redis SSL
spring:
  redis:
    ssl: true
    ssl-bundle: bean:redis-ssl-bundle

# SSL Bundle Configuration
spring:
  ssl:
    bundle:
      jks:
        redis-ssl-bundle:
          keystore:
            location: classpath:redis-keystore.p12
            password: ${REDIS_SSL_PASSWORD:changeit}
```

### Java Keystore Creation

```bash
# Convert PEM to PKCS12
openssl pkcs12 -export -in finpass.crt -inkey finpass.key \
  -out finpass.p12 -name finpass -CAfile ca.crt -caname root

# Import to Java keystore
keytool -importkeystore -srckeystore finpass.p12 -srcstoretype PKCS12 \
  -destkeystore keystore.p12 -deststoretype PKCS12 \
  -srcstorepass changeit -deststorepass changeit

# Verify keystore
keytool -list -v -keystore keystore.p12
```

## Testing

### SSL Certificate Testing

```bash
# Test certificate chain
openssl s_client -connect finpass.io:443 -servername finpass.io

# Check certificate details
openssl x509 -in finpass.crt -text -noout

# Verify certificate against CA
openssl verify -CAfile ca.crt finpass.crt

# Test SSL configuration
nmap --script ssl-enum-ciphers -p 443 finpass.io

# Check SSL rating
curl -I https://finpass.io
```

### Online Testing Tools

- **SSL Labs**: https://www.ssllabs.com/ssltest/
- **Qualys SSL Test**: Comprehensive SSL assessment
- **KeyCDN TLS Test**: https://tools.keycdn.com/ssl
- **ImmuniWeb**: https://www.immuniweb.com/ssl/

### Automated Testing Script

```bash
#!/bin/bash
# ssl-test.sh

DOMAIN=${1:-finpass.io}
PORT=${2:-443}

echo "Testing SSL configuration for $DOMAIN:$PORT"

# Test certificate chain
echo "1. Testing certificate chain..."
openssl s_client -connect $DOMAIN:$PORT -servername $DOMAIN </dev/null 2>/dev/null | openssl x509 -noout -dates

# Test SSL protocols
echo "2. Testing SSL protocols..."
for protocol in ssl2 ssl3 tls1 tls1_1 tls1_2 tls1_3; do
    echo -n "$protocol: "
    openssl s_client -connect $DOMAIN:$PORT -$protocol </dev/null 2>/dev/null && echo "✓" || echo "✗"
done

# Test cipher suites
echo "3. Testing cipher suites..."
nmap --script ssl-enum-ciphers -p $PORT $DOMAIN 2>/dev/null | grep -A 20 "SSLv3:"

# Check certificate expiration
echo "4. Checking certificate expiration..."
exp_date=$(echo | openssl s_client -connect $DOMAIN:$PORT -servername $DOMAIN 2>/dev/null | openssl x509 -noout -enddate | cut -d= -f2)
echo "Certificate expires: $exp_date"

# Calculate days until expiration
exp_epoch=$(date -d "$exp_date" +%s)
current_epoch=$(date +%s)
days_until=$((($exp_epoch - $current_epoch) / 86400))

if [ $days_until -lt 30 ]; then
    echo "⚠️  Certificate expires in $days_until days!"
else
    echo "✓ Certificate valid for $days_until days"
fi
```

## Troubleshooting

### Common Issues

#### Certificate Chain Errors

```bash
# Check certificate chain
openssl s_client -connect finpass.io:443 -showcerts

# Fix: Include intermediate certificates
cat finpass.crt intermediate.crt > finpass-fullchain.crt
```

#### Hostname Mismatch

```bash
# Check certificate CN and SAN
openssl x509 -in finpass.crt -noout -text | grep -A 1 "Subject Alternative Name"

# Fix: Generate certificate with correct SANs
```

#### Certificate Expiration

```bash
# Check expiration date
openssl x509 -in finpass.crt -noout -enddate

# Fix: Renew certificate before expiration
```

#### Browser Trust Issues

```bash
# For self-signed certificates, import CA certificate
# Firefox: Settings -> Certificates -> Authorities -> Import
# Chrome: Settings -> Privacy and security -> Manage certificates -> Authorities
```

### Debug Commands

```bash
# Test specific SSL version
openssl s_client -connect finpass.io:443 -tls1_2

# Test with specific cipher
openssl s_client -connect finpass.io:443 -cipher ECDHE-RSA-AES256-GCM-SHA384

# Verify certificate chain
openssl verify -CAfile ca.crt -untrusted intermediate.crt finpass.crt

# Check OCSP status
openssl ocsp -issuer ca.crt -cert finpass.crt -url http://ocsp.example.com
```

### Performance Optimization

```bash
# Test SSL handshake time
time echo | openssl s_client -connect finpass.io:443 2>/dev/null

# Optimize session cache
nginx -t -c /etc/nginx/nginx.conf

# Monitor SSL sessions
openssl s_client -connect finpass.io:443 -reconnect
```

### Security Hardening

```bash
# Disable weak protocols
ssl_protocols TLSv1.2 TLSv1.3;

# Use strong ciphers
ssl_ciphers ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384;

# Enable HSTS
add_header Strict-Transport-Security "max-age=63072000; includeSubDomains; preload";

# Enable OCSP Stapling
ssl_stapling on;
ssl_stapling_verify on;
resolver 8.8.8.8 8.8.4.4 valid=300s;
```

## Best Practices

1. **Use TLS 1.2+**: Disable older SSL/TLS versions
2. **Strong Ciphers**: Use modern cipher suites
3. **Certificate Monitoring**: Monitor expiration dates
4. **Automated Renewal**: Setup automatic certificate renewal
5. **Security Headers**: Implement HSTS and other security headers
6. **Perfect Forward Secrecy**: Use ECDHE key exchange
7. **Regular Testing**: Test SSL configuration regularly
8. **Backup Certificates**: Keep secure backups of certificates

## Compliance

### GDPR Compliance
- Encrypt all data in transit
- Implement proper certificate management
- Maintain audit logs of certificate changes

### SOC2 Compliance
- Use certificates from trusted CAs
- Implement proper key management
- Regular security assessments

### PCI DSS Compliance
- Strong encryption (TLS 1.2+)
- Regular certificate rotation
- Security monitoring and testing

This SSL/TLS configuration guide ensures that FinPass deployments maintain strong security standards across all environments.
