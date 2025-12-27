// SPDX-License-Identifier: MIT
pragma solidity ^0.8.19;

/**
 * @title TrustRegistry
 * @dev On-chain registry for trusted issuers with assurance levels
 * Allows admin management of trusted issuers and time-based verification
 */
contract TrustRegistry {
    
    struct IssuerInfo {
        uint8 assuranceLevel;  // 1=LOW, 2=MEDIUM, 3=HIGH
        string metadata;       // Additional metadata (JSON string)
        uint256 addedAt;       // When issuer was added
        bool active;           // Whether issuer is currently trusted
    }
    
    // Admin address (can be transferred)
    address private admin;
    
    // Mapping from issuer DID to issuer info
    mapping(string => IssuerInfo) private issuers;
    
    // Array of all registered issuer DIDs for enumeration
    string[] private issuerList;
    
    // Events
    event AdminTransferred(address indexed oldAdmin, address indexed newAdmin);
    event IssuerAdded(string indexed issuerDID, uint8 assuranceLevel, string metadata, uint256 timestamp);
    event IssuerRemoved(string indexed issuerDID, uint256 timestamp);
    event IssuerUpdated(string indexed issuerDID, uint8 newAssuranceLevel, string newMetadata, uint256 timestamp);
    
    // Errors
    error OnlyAdminAllowed();
    error IssuerAlreadyExists(string issuerDID);
    error IssuerNotFound(string issuerDID);
    error InvalidAssuranceLevel(uint8 level);
    error EmptyMetadata(string metadata);
    
    // Constants for assurance levels
    uint8 public constant ASSURANCE_LOW = 1;
    uint8 public constant ASSURANCE_MEDIUM = 2;
    uint8 public constant ASSURANCE_HIGH = 3;
    
    constructor() {
        admin = msg.sender;
        emit AdminTransferred(address(0), admin);
    }
    
    /**
     * @dev Modifier to restrict access to admin only
     */
    modifier onlyAdmin() {
        if (msg.sender != admin) {
            revert OnlyAdminAllowed();
        }
        _;
    }
    
    /**
     * @dev Transfer admin rights to a new address
     * @param newAdmin The new admin address
     */
    function transferAdmin(address newAdmin) external onlyAdmin {
        if (newAdmin == address(0)) {
            revert OnlyAdminAllowed();
        }
        
        address oldAdmin = admin;
        admin = newAdmin;
        emit AdminTransferred(oldAdmin, newAdmin);
    }
    
    /**
     * @dev Get current admin address
     * @return Current admin address
     */
    function getAdmin() external view returns (address) {
        return admin;
    }
    
    /**
     * @dev Add a new trusted issuer
     * @param issuerDID The DID of the issuer to add
     * @param assuranceLevel The assurance level (1-3)
     * @param metadata Additional metadata as JSON string
     */
    function addIssuer(
        string calldata issuerDID,
        uint8 assuranceLevel,
        string calldata metadata
    ) external onlyAdmin {
        // Validate inputs
        if (issuers[issuerDID].active) {
            revert IssuerAlreadyExists(issuerDID);
        }
        
        if (assuranceLevel < ASSURANCE_LOW || assuranceLevel > ASSURANCE_HIGH) {
            revert InvalidAssuranceLevel(assuranceLevel);
        }
        
        if (bytes(metadata).length == 0) {
            revert EmptyMetadata(metadata);
        }
        
        // Add issuer
        issuers[issuerDID] = IssuerInfo({
            assuranceLevel: assuranceLevel,
            metadata: metadata,
            addedAt: block.timestamp,
            active: true
        });
        
        issuerList.push(issuerDID);
        
        emit IssuerAdded(issuerDID, assuranceLevel, metadata, block.timestamp);
    }
    
    /**
     * @dev Remove a trusted issuer
     * @param issuerDID The DID of the issuer to remove
     */
    function removeIssuer(string calldata issuerDID) external onlyAdmin {
        if (!issuers[issuerDID].active) {
            revert IssuerNotFound(issuerDID);
        }
        
        // Deactivate issuer (keep historical data)
        issuers[issuerDID].active = false;
        
        emit IssuerRemoved(issuerDID, block.timestamp);
    }
    
    /**
     * @dev Update an existing issuer's assurance level and metadata
     * @param issuerDID The DID of the issuer to update
     * @param newAssuranceLevel The new assurance level (1-3)
     * @param newMetadata The new metadata as JSON string
     */
    function updateIssuer(
        string calldata issuerDID,
        uint8 newAssuranceLevel,
        string calldata newMetadata
    ) external onlyAdmin {
        if (!issuers[issuerDID].active) {
            revert IssuerNotFound(issuerDID);
        }
        
        if (newAssuranceLevel < ASSURANCE_LOW || newAssuranceLevel > ASSURANCE_HIGH) {
            revert InvalidAssuranceLevel(newAssuranceLevel);
        }
        
        if (bytes(newMetadata).length == 0) {
            revert EmptyMetadata(newMetadata);
        }
        
        // Update issuer
        issuers[issuerDID].assuranceLevel = newAssuranceLevel;
        issuers[issuerDID].metadata = newMetadata;
        
        emit IssuerUpdated(issuerDID, newAssuranceLevel, newMetadata, block.timestamp);
    }
    
    /**
     * @dev Check if an issuer is trusted at a specific timestamp
     * @param issuerDID The DID of the issuer to check
     * @param timestamp The timestamp to check against
     * @return True if issuer was trusted at the given timestamp
     */
    function isTrusted(string calldata issuerDID, uint256 timestamp) external view returns (bool) {
        IssuerInfo memory issuer = issuers[issuerDID];
        
        // Check if issuer exists and was added before the timestamp
        return issuer.active && issuer.addedAt <= timestamp;
    }
    
    /**
     * @dev Check if an issuer is currently trusted
     * @param issuerDID The DID of the issuer to check
     * @return True if issuer is currently trusted
     */
    function isTrustedIssuer(string calldata issuerDID) external view returns (bool) {
        return issuers[issuerDID].active;
    }
    
    /**
     * @dev Get information about a trusted issuer
     * @param issuerDID The DID of the issuer
     * @return assuranceLevel The assurance level
     * @return metadata The metadata
     * @return addedAt When the issuer was added
     * @return active Whether the issuer is active
     */
    function getIssuerInfo(string calldata issuerDID) external view returns (
        uint8 assuranceLevel,
        string memory metadata,
        uint256 addedAt,
        bool active
    ) {
        IssuerInfo memory issuer = issuers[issuerDID];
        return (issuer.assuranceLevel, issuer.metadata, issuer.addedAt, issuer.active);
    }
    
    /**
     * @dev Get all trusted issuer DIDs
     * @return Array of all issuer DIDs
     */
    function getAllIssuers() external view returns (string[] memory) {
        return issuerList;
    }
    
    /**
     * @dev Get the number of trusted issuers
     * @return Count of trusted issuers
     */
    function getIssuerCount() external view returns (uint256) {
        return issuerList.length;
    }
    
    /**
     * @dev Get issuers by assurance level
     * @param level The assurance level to filter by
     * @return Array of issuer DIDs with the specified assurance level
     */
    function getIssuersByLevel(uint8 level) external view returns (string[] memory) {
        uint256 count = 0;
        
        // First pass: count matching issuers
        for (uint256 i = 0; i < issuerList.length; i++) {
            if (issuers[issuerList[i]].active && issuers[issuerList[i]].assuranceLevel == level) {
                count++;
            }
        }
        
        // Second pass: populate result array
        string[] memory result = new string[](count);
        uint256 index = 0;
        
        for (uint256 i = 0; i < issuerList.length; i++) {
            if (issuers[issuerList[i]].active && issuers[issuerList[i]].assuranceLevel == level) {
                result[index] = issuerList[i];
                index++;
            }
        }
        
        return result;
    }
}
