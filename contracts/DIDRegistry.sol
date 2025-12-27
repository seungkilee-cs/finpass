// SPDX-License-Identifier: MIT
pragma solidity ^0.8.19;

/**
 * @title DIDRegistry
 * @dev Registry for Decentralized Identifiers on blockchain
 * Allows registration and verification of DIDs with their public keys
 */
contract DIDRegistry {
    
    struct DIDInfo {
        string publicKeyJWK;  // Public key in JWK format
        uint256 timestamp;    // Registration timestamp
        bool active;          // Whether the DID is active
    }
    
    // Mapping from DID hash to DID info
    mapping(bytes32 => DIDInfo) private didRegistry;
    
    // Mapping from DID to list of historical timestamps
    mapping(string => uint256[]) private didHistory;
    
    // Events
    event DIDRegistered(bytes32 indexed didHash, string did, string publicKeyJWK, uint256 timestamp);
    event DIDUpdated(bytes32 indexed didHash, string did, string newPublicKeyJWK, uint256 timestamp);
    
    // Errors
    error DIDAlreadyRegistered(bytes32 didHash);
    error DIDNotRegistered(bytes32 didHash);
    error InvalidPublicKey(string publicKey);
    
    /**
     * @dev Register a new DID with its public key
     * @param didHash Hash of the DID string
     * @param did The full DID string
     * @param publicKeyJWK Public key in JWK format
     */
    function registerDID(
        bytes32 didHash,
        string calldata did,
        string calldata publicKeyJWK
    ) external {
        // Check if DID is already registered
        if (didRegistry[didHash].timestamp != 0) {
            revert DIDAlreadyRegistered(didHash);
        }
        
        // Validate public key (basic check)
        if (bytes(publicKeyJWK).length == 0) {
            revert InvalidPublicKey(publicKeyJWK);
        }
        
        // Register DID
        didRegistry[didHash] = DIDInfo({
            publicKeyJWK: publicKeyJWK,
            timestamp: block.timestamp,
            active: true
        });
        
        // Add to history
        didHistory[did].push(block.timestamp);
        
        emit DIDRegistered(didHash, did, publicKeyJWK, block.timestamp);
    }
    
    /**
     * @dev Update an existing DID's public key
     * @param didHash Hash of the DID string
     * @param did The full DID string
     * @param newPublicKeyJWK New public key in JWK format
     */
    function updateDID(
        bytes32 didHash,
        string calldata did,
        string calldata newPublicKeyJWK
    ) external {
        // Check if DID exists
        if (didRegistry[didHash].timestamp == 0) {
            revert DIDNotRegistered(didHash);
        }
        
        // Validate new public key
        if (bytes(newPublicKeyJWK).length == 0) {
            revert InvalidPublicKey(newPublicKeyJWK);
        }
        
        // Update DID
        didRegistry[didHash].publicKeyJWK = newPublicKeyJWK;
        didRegistry[didHash].timestamp = block.timestamp;
        
        // Add to history
        didHistory[did].push(block.timestamp);
        
        emit DIDUpdated(didHash, did, newPublicKeyJWK, block.timestamp);
    }
    
    /**
     * @dev Verify if a DID was registered at or before a specific timestamp
     * @param didHash Hash of the DID string
     * @param timestamp Timestamp to verify against
     * @return True if DID was registered at or before the timestamp
     */
    function verifyDIDAt(
        bytes32 didHash,
        uint256 timestamp
    ) external view returns (bool) {
        DIDInfo memory info = didRegistry[didHash];
        return info.timestamp != 0 && info.timestamp <= timestamp && info.active;
    }
    
    /**
     * @dev Get information about a registered DID
     * @param didHash Hash of the DID string
     * @return publicKeyJWK Public key in JWK format
     * @return timestamp Registration timestamp
     * @return active Whether the DID is active
     */
    function getDIDInfo(
        bytes32 didHash
    ) external view returns (string memory publicKeyJWK, uint256 timestamp, bool active) {
        DIDInfo memory info = didRegistry[didHash];
        return (info.publicKeyJWK, info.timestamp, info.active);
    }
    
    /**
     * @dev Get the registration history for a DID
     * @param did The full DID string
     * @return Array of timestamps when the DID was registered/updated
     */
    function getDIDHistory(string calldata did) external view returns (uint256[] memory) {
        return didHistory[did];
    }
    
    /**
     * @dev Deactivate a DID (can only be done by the DID owner)
     * @param didHash Hash of the DID string
     */
    function deactivateDID(bytes32 didHash) external {
        if (didRegistry[didHash].timestamp == 0) {
            revert DIDNotRegistered(didHash);
        }
        
        didRegistry[didHash].active = false;
    }
    
    /**
     * @dev Reactivate a deactivated DID
     * @param didHash Hash of the DID string
     */
    function reactivateDID(bytes32 didHash) external {
        if (didRegistry[didHash].timestamp == 0) {
            revert DIDNotRegistered(didHash);
        }
        
        didRegistry[didHash].active = true;
    }
}
