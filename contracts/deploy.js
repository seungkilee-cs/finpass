const { ethers } = require("hardhat");

async function main() {
  console.log("Deploying DIDRegistry and TrustRegistry to Polygon Mumbai...");
  
  // Get the deployer account
  const [deployer] = await ethers.getSigners();
  console.log("Deploying contracts with the account:", deployer.address);
  
  // Check account balance
  const balance = await deployer.getBalance();
  console.log("Account balance:", ethers.utils.formatEther(balance));
  
  // Deploy DIDRegistry
  console.log("Deploying DIDRegistry...");
  const DIDRegistry = await ethers.getContractFactory("DIDRegistry");
  const didRegistry = await DIDRegistry.deploy();
  
  await didRegistry.deployed();
  console.log("DIDRegistry deployed to:", didRegistry.address);
  console.log("DIDRegistry transaction hash:", didRegistry.deployTransaction.hash);
  
  // Deploy TrustRegistry
  console.log("Deploying TrustRegistry...");
  const TrustRegistry = await ethers.getContractFactory("TrustRegistry");
  const trustRegistry = await TrustRegistry.deploy();
  
  await trustRegistry.deployed();
  console.log("TrustRegistry deployed to:", trustRegistry.address);
  console.log("TrustRegistry transaction hash:", trustRegistry.deployTransaction.hash);
  
  // Wait for confirmations
  console.log("Waiting for confirmations...");
  await didRegistry.deployTransaction.wait(2);
  await trustRegistry.deployTransaction.wait(2);
  
  console.log("Deployments confirmed!");
  
  // Example DID Registry registration
  console.log("Registering test DID in DIDRegistry...");
  const testDID = "did:example:123456789abcdefghi";
  const testPublicKey = JSON.stringify({
    kty: "OKP",
    crv: "Ed25519",
    x: "1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef"
  });
  
  const didHash = ethers.utils.keccak256(ethers.utils.toUtf8Bytes(testDID));
  const didTx = await didRegistry.registerDID(didHash, testDID, testPublicKey);
  await didTx.wait();
  console.log("Test DID registered in DIDRegistry!");
  
  // Example Trust Registry operations
  console.log("Adding test issuer to TrustRegistry...");
  const issuerDID = "did:example:trusted-issuer";
  const assuranceLevel = 2; // MEDIUM
  const metadata = JSON.stringify({
    type: "government",
    country: "US",
    jurisdiction: "federal"
  });
  
  const trustTx = await trustRegistry.addIssuer(issuerDID, assuranceLevel, metadata);
  await trustTx.wait();
  console.log("Test issuer added to TrustRegistry!");
  
  // Verify operations
  const isDidRegistered = await didRegistry.verifyDIDAt(didHash, Math.floor(Date.now() / 1000));
  console.log("DID Registry verification:", isDidRegistered);
  
  const isTrusted = await trustRegistry.isTrustedIssuer(issuerDID);
  console.log("Trust Registry verification:", isTrusted);
  
  const issuerInfo = await trustRegistry.getIssuerInfo(issuerDID);
  console.log("Issuer info:", {
    assuranceLevel: issuerInfo.assuranceLevel,
    metadata: issuerInfo.metadata,
    addedAt: new Date(issuerInfo.addedAt * 1000).toISOString(),
    active: issuerInfo.active
  });
  
  // Output deployment summary
  console.log("\n=== Deployment Summary ===");
  console.log("DIDRegistry:", didRegistry.address);
  console.log("TrustRegistry:", trustRegistry.address);
  console.log("Deployer:", deployer.address);
  console.log("Network:", (await ethers.provider.getNetwork()).name);
  console.log("Gas used DIDRegistry:", (await didRegistry.deployTransaction.wait()).gasUsed.toString());
  console.log("Gas used TrustRegistry:", (await trustRegistry.deployTransaction.wait()).gasUsed.toString());
  
  console.log("\n=== Configuration for Applications ===");
  console.log("Add these to your application.properties:");
  console.log(`blockchain.didRegistry.address=${didRegistry.address}`);
  console.log(`blockchain.trustRegistry.address=${trustRegistry.address}`);
}

main()
  .then(() => process.exit(0))
  .catch((error) => {
    console.error(error);
    process.exit(1);
  });
