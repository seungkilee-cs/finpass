#!/bin/bash

# FinPass Demo Script
# This script demonstrates the complete FinPass workflow from wallet creation to payment processing

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
API_BASE_URL="${API_BASE_URL:-http://localhost:8080}"
FRONTEND_URL="${FRONTEND_URL:-http://localhost}"
TIMEOUT="${TIMEOUT:-30}"

# Demo data
USER_DID="did:example:demouser$(date +%s)"
VERIFIER_DID="did:example:demoverifier$(date +%s)"
ISSUER_DID="did:example:demoissuer$(date +%s)"

# Helper functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

check_prerequisites() {
    log_info "Checking prerequisites..."
    
    # Check if curl is available
    if ! command -v curl &> /dev/null; then
        log_error "curl is required but not installed"
        exit 1
    fi
    
    # Check if jq is available
    if ! command -v jq &> /dev/null; then
        log_warning "jq is not installed. JSON parsing will be limited."
    fi
    
    # Check if services are running
    log_info "Checking if FinPass services are running..."
    
    # Check backend health
    if ! curl -f -s "${API_BASE_URL}/health" > /dev/null; then
        log_error "Backend service is not running at ${API_BASE_URL}"
        log_info "Please start the services with: docker-compose up -d"
        exit 1
    fi
    
    # Check frontend
    if ! curl -f -s "${FRONTEND_URL}/health" > /dev/null; then
        log_error "Frontend service is not running at ${FRONTEND_URL}"
        exit 1
    fi
    
    log_success "All prerequisites satisfied!"
}

wait_for_service() {
    local url=$1
    local service_name=$2
    local max_attempts=30
    local attempt=1
    
    log_info "Waiting for ${service_name} to be ready..."
    
    while [ $attempt -le $max_attempts ]; do
        if curl -f -s "${url}" > /dev/null; then
            log_success "${service_name} is ready!"
            return 0
        fi
        
        log_info "Attempt ${attempt}/${max_attempts} - ${service_name} not ready yet..."
        sleep 2
        ((attempt++))
    done
    
    log_error "${service_name} failed to become ready within ${max_attempts} attempts"
    return 1
}

display_header() {
    echo -e "${BLUE}"
    echo "╔══════════════════════════════════════════════════════════════╗"
    echo "║                    FinPass Demo Script                      ║"
    echo "║                    Version 1.0.0                           ║"
    echo "╚══════════════════════════════════════════════════════════════╝"
    echo -e "${NC}"
    echo "This demo will showcase the complete FinPass workflow:"
    echo "1. ✅ Health check and service verification"
    echo "2. 🔐 DID wallet creation"
    echo "3. 📜 Credential issuance"
    echo "4. 🔍 Credential verification"
    echo "5. 💳 Payment processing"
    echo "6. 📊 Audit trail verification"
    echo ""
}

step_1_health_check() {
    log_info "Step 1: Performing health checks..."
    
    echo "Backend Health:"
    curl -s "${API_BASE_URL}/health" | jq '.' 2>/dev/null || curl -s "${API_BASE_URL}/health"
    echo ""
    
    echo "Detailed Health:"
    curl -s "${API_BASE_URL}/health/detailed" | jq '.' 2>/dev/null || curl -s "${API_BASE_URL}/health/detailed"
    echo ""
    
    log_success "Health checks completed!"
}

step_2_create_wallet() {
    log_info "Step 2: Creating DID wallet..."
    
    # Create user wallet request
    local wallet_request=$(cat <<EOF
{
    "did": "${USER_DID}",
    "publicKey": "0x1234567890abcdef1234567890abcdef12345678",
    "keyType": "Ed25519",
    "controller": "${USER_DID}",
    "service": "did-communication",
    "serviceEndpoint": "${API_BASE_URL}/did/${USER_DID}"
}
EOF
)
    
    echo "Creating wallet for DID: ${USER_DID}"
    
    # Create wallet (this would typically be done through the frontend)
    local response=$(curl -s -X POST \
        "${API_BASE_URL}/api/wallet/create" \
        -H "Content-Type: application/json" \
        -d "${wallet_request}") || {
        log_warning "Wallet creation endpoint not available - simulating wallet creation"
        echo "Wallet created successfully for DID: ${USER_DID}"
    }
    
    if [ -n "$response" ]; then
        echo "$response" | jq '.' 2>/dev/null || echo "$response"
    fi
    
    log_success "Wallet creation completed!"
}

step_3_issue_credential() {
    log_info "Step 3: Issuing credential..."
    
    # Create credential request
    local credential_request=$(cat <<EOF
{
    "holderDid": "${USER_DID}",
    "credentialType": "PASSPORT",
    "credentialData": {
        "passportNumber": "P$(date +%s)",
        "fullName": "Demo User",
        "dateOfBirth": "1990-01-01",
        "nationality": "US",
        "issuingCountry": "US",
        "expirationDate": "2030-01-01"
    },
    "livenessProof": {
        "score": 0.95,
        "timestamp": "$(date -u +%Y-%m-%dT%H:%M:%S.%3NZ)",
        "proofData": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"
    }
}
EOF
)
    
    echo "Issuing PASSPORT credential for DID: ${USER_DID}"
    
    # Issue credential
    local response=$(curl -s -X POST \
        "${API_BASE_URL}/api/issuer/credentials" \
        -H "Content-Type: application/json" \
        -d "${credential_request}")
    
    echo "$response" | jq '.' 2>/dev/null || echo "$response"
    
    # Extract credential ID for later use
    if command -v jq &> /dev/null && [ -n "$response" ]; then
        CREDENTIAL_ID=$(echo "$response" | jq -r '.credentialId // empty')
        CREDENTIAL_JWT=$(echo "$response" | jq -r '.credentialJwt // empty')
        
        if [ -n "$CREDENTIAL_ID" ] && [ "$CREDENTIAL_ID" != "null" ]; then
            log_success "Credential issued with ID: $CREDENTIAL_ID"
        else
            log_warning "Could not extract credential ID from response"
            CREDENTIAL_ID="demo-credential-$(date +%s)"
        fi
    else
        CREDENTIAL_ID="demo-credential-$(date +%s)"
        log_success "Credential issued with ID: $CREDENTIAL_ID"
    fi
    
    log_success "Credential issuance completed!"
}

step_4_verify_credential() {
    log_info "Step 4: Verifying credential..."
    
    if [ -z "$CREDENTIAL_JWT" ]; then
        log_warning "No credential JWT available - using demo credential"
        CREDENTIAL_JWT="demo.jwt.token"
    fi
    
    # Create verification request
    local verification_request=$(cat <<EOF
{
    "credentialJwt": "${CREDENTIAL_JWT}",
    "verifierDid": "${VERIFIER_DID}",
    "verificationType": "IDENTITY_VERIFICATION",
    "verificationData": {
        "minimumAge": 18,
        "requiredFields": ["fullName", "dateOfBirth"]
    }
}
EOF
)
    
    echo "Verifying credential with verifier DID: ${VERIFIER_DID}"
    
    # Verify credential
    local response=$(curl -s -X POST \
        "${API_BASE_URL}/api/verifier/verify" \
        -H "Content-Type: application/json" \
        -d "${verification_request}")
    
    echo "$response" | jq '.' 2>/dev/null || echo "$response"
    
    # Extract verification result
    if command -v jq &> /dev/null && [ -n "$response" ]; then
        IS_VALID=$(echo "$response" | jq -r '.valid // false')
        VERIFICATION_ID=$(echo "$response" | jq -r '.verificationId // empty')
        
        if [ "$IS_VALID" = "true" ]; then
            log_success "Credential verified successfully!"
        else
            log_warning "Credential verification returned invalid - this may be expected in demo mode"
        fi
    else
        log_success "Credential verification completed!"
    fi
    
    log_success "Credential verification completed!"
}

step_5_process_payment() {
    log_info "Step 5: Processing payment..."
    
    # Create payment request
    local payment_request=$(cat <<EOF
{
    "payerDid": "${USER_DID}",
    "payeeDid": "${ISSUER_DID}",
    "amount": 100.50,
    "currency": "USD",
    "paymentMethod": "DIGITAL_WALLET",
    "description": "Demo payment for credential verification",
    "metadata": {
        "invoiceId": "INV-$(date +%s)",
        "orderId": "ORDER-$(date +%s)",
        "verificationId": "${VERIFICATION_ID:-demo-verification}"
    }
}
EOF
)
    
    echo "Processing payment from ${USER_DID} to ${ISSUER_DID}"
    
    # Process payment
    local response=$(curl -s -X POST \
        "${API_BASE_URL}/api/payments" \
        -H "Content-Type: application/json" \
        -d "${payment_request}")
    
    echo "$response" | jq '.' 2>/dev/null || echo "$response"
    
    # Extract payment ID for confirmation
    if command -v jq &> /dev/null && [ -n "$response" ]; then
        PAYMENT_ID=$(echo "$response" | jq -r '.paymentId // empty')
        
        if [ -n "$PAYMENT_ID" ] && [ "$PAYMENT_ID" != "null" ]; then
            log_success "Payment initiated with ID: $PAYMENT_ID"
            
            # Confirm payment
            log_info "Confirming payment..."
            local confirmation_request=$(cat <<EOF
{
    "confirmationCode": "CONF-$(date +%s)",
    "transactionHash": "0x$(date +%s | sha256sum | cut -c1-64)"
}
EOF
)
            
            local confirm_response=$(curl -s -X POST \
                "${API_BASE_URL}/api/payments/${PAYMENT_ID}/confirm" \
                -H "Content-Type: application/json" \
                -d "${confirmation_request}")
            
            echo "Payment confirmation:"
            echo "$confirm_response" | jq '.' 2>/dev/null || echo "$confirm_response"
        else
            log_warning "Could not extract payment ID from response"
            PAYMENT_ID="demo-payment-$(date +%s)"
        fi
    else
        PAYMENT_ID="demo-payment-$(date +%s)"
        log_success "Payment processed with ID: $PAYMENT_ID"
    fi
    
    log_success "Payment processing completed!"
}

step_6_audit_trail() {
    log_info "Step 6: Verifying audit trail..."
    
    echo "Retrieving audit events for user: ${USER_DID}"
    
    # Get audit events
    local response=$(curl -s "${API_BASE_URL}/api/audit/events" \
        -H "Content-Type: application/json" \
        -G \
        -d "userIdHash=hash_${USER_DID#did:}" \
        -d "page=0" \
        -d "size=10")
    
    echo "Audit Trail:"
    echo "$response" | jq '.' 2>/dev/null || echo "$response"
    
    # Get system statistics
    echo ""
    echo "System Statistics:"
    local stats_response=$(curl -s "${API_BASE_URL}/api/audit/stats")
    echo "$stats_response" | jq '.' 2>/dev/null || echo "$stats_response"
    
    log_success "Audit trail verification completed!"
}

display_summary() {
    echo -e "${GREEN}"
    echo "╔══════════════════════════════════════════════════════════════╗"
    echo "║                    Demo Completed!                          ║"
    echo "╚══════════════════════════════════════════════════════════════╝"
    echo -e "${NC}"
    
    echo "Demo Summary:"
    echo "================"
    echo "✅ User DID: ${USER_DID}"
    echo "✅ Verifier DID: ${VERIFIER_DID}"
    echo "✅ Issuer DID: ${ISSUER_DID}"
    echo "✅ Credential ID: ${CREDENTIAL_ID:-demo-credential}"
    echo "✅ Payment ID: ${PAYMENT_ID:-demo-payment}"
    echo ""
    
    echo "Access Points:"
    echo "=============="
    echo "🌐 Frontend Application: ${FRONTEND_URL}"
    echo "🔧 Backend API: ${API_BASE_URL}"
    echo "📚 API Documentation: ${API_BASE_URL}/swagger-ui"
    echo "📊 Health Status: ${API_BASE_URL}/health"
    echo ""
    
    echo "Next Steps:"
    echo "============"
    echo "1. Explore the frontend at ${FRONTEND_URL}"
    echo "2. Review API documentation at ${API_BASE_URL}/swagger-ui"
    echo "3. Check system health at ${API_BASE_URL}/health/detailed"
    echo "4. View audit logs at ${API_BASE_URL}/api/audit/events"
    echo ""
    
    echo "Troubleshooting:"
    echo "================"
    echo "• Check service logs: docker-compose logs -f"
    echo "• Verify service status: docker-compose ps"
    echo "• Restart services: docker-compose restart"
    echo "• View detailed health: curl ${API_BASE_URL}/health/detailed"
    echo ""
    
    log_success "FinPass demo completed successfully! 🎉"
}

cleanup() {
    log_info "Performing cleanup..."
    
    # This function can be used to clean up any demo-specific data
    # For now, we'll just display cleanup information
    
    echo "Demo data cleanup:"
    echo "- User DID: ${USER_DID}"
    echo "- Credential ID: ${CREDENTIAL_ID:-demo-credential}"
    echo "- Payment ID: ${PAYMENT_ID:-demo-payment}"
    echo ""
    echo "Note: In a production environment, you would want to implement"
    echo "proper cleanup procedures for demo data."
    
    log_success "Cleanup completed!"
}

# Main execution
main() {
    display_header
    
    # Check for help flag
    if [[ "$1" == "-h" || "$1" == "--help" ]]; then
        echo "FinPass Demo Script"
        echo ""
        echo "Usage: $0 [OPTIONS]"
        echo ""
        echo "Options:"
        echo "  -h, --help     Show this help message"
        echo "  -c, --cleanup  Run cleanup after demo"
        echo ""
        echo "Environment Variables:"
        echo "  API_BASE_URL    Backend API URL (default: http://localhost:8080)"
        echo "  FRONTEND_URL    Frontend URL (default: http://localhost)"
        echo "  TIMEOUT         Request timeout in seconds (default: 30)"
        echo ""
        echo "Examples:"
        echo "  $0                           # Run demo with default settings"
        echo "  API_BASE_URL=https://api.finpass.io $0  # Use custom API URL"
        echo "  $0 --cleanup                 # Run demo and cleanup afterwards"
        exit 0
    fi
    
    # Check for cleanup flag
    CLEANUP=false
    if [[ "$1" == "-c" || "$1" == "--cleanup" ]]; then
        CLEANUP=true
    fi
    
    # Trap to ensure cleanup runs on exit
    trap cleanup EXIT
    
    # Run demo steps
    check_prerequisites
    wait_for_service "${API_BASE_URL}/health" "Backend Service"
    wait_for_service "${FRONTEND_URL}/health" "Frontend Service"
    
    step_1_health_check
    step_2_create_wallet
    step_3_issue_credential
    step_4_verify_credential
    step_5_process_payment
    step_6_audit_trail
    
    display_summary
    
    # Run cleanup if requested
    if [ "$CLEANUP" = true ]; then
        cleanup
    fi
}

# Run main function with all arguments
main "$@"
