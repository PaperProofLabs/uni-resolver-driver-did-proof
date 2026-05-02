# uni-resolver-driver-did-proof

This is a [Universal Resolver](https://github.com/decentralized-identity/universal-resolver) driver for **did:proof** identifiers.

## About did:proof

The `did:proof` method provides a way to create decentralized identifiers for digital artifacts, proofs, and attestations. It supports metadata about publication state, artifact types, storage information, and versioning.

## Driver Features

- **DID Resolution**: Resolves `did:proof` identifiers to DID documents
- **Query Parameters**: Supports rich metadata through URL query parameters
- **Dereferencing**: Supports DID URL dereferencing
- **Test Identifiers**: Provides sample test DIDs for validation

## DID Format

```
did:proof:<method-specific-id>[?<query-parameters>]
```

### Method-Specific ID

The method-specific ID uses Multibase encoding with base58btc (z-prefixed) format, following the pattern: `^z[A-Za-z0-9]{20,}$`

Examples:
- `did:proof:z6MkfrQC9BjKJzKVh6eNkJQZmUZ6a4Qz8EJGwE7VzKdKjNQ`
- `did:proof:zABC123def456ghi789jkl012mno345pqr678stu901vwx`

### Query Parameters

The driver supports the following query parameters for metadata:

- `subjectType`: Type of subject (e.g., "document", "artifact")
- `publicationState`: Publication state (e.g., "draft", "published")
- `artifactType`: Type of artifact
- `canonicalArtifactHash`: Hash of the canonical artifact
- `canonicalArtifactHashAlgorithm`: Hash algorithm used
- `canonicalArtifactLocator`: URL to retrieve the artifact
- `storageProvider`: Storage provider name
- `storageObjectId`: Object ID in storage
- `storageEndEpoch`: Storage expiration timestamp
- `versionId`: Version identifier
- `previousVersion`: Previous version DID
- `latestVersion`: Latest version DID
- `reservedAt`: Reservation timestamp
- `publishedAt`: Publication timestamp
- `governanceAuthority`: Governance authority identifier
- `verificationPolicy`: Verification policy
- `deactivated`: Whether the DID is deactivated ("true" or "false")

## Building and Testing

### Prerequisites

- Java 8 or higher
- Maven 3.6+

### Build

```bash
mvn clean compile
```

### Run Tests

```bash
mvn test
```

### Package

```bash
mvn package
```

## Usage Example

```java
import uniresolver.driver.did.proof.DidProofDriver;
import java.util.HashMap;
import java.util.Map;

public class Example {
    public static void main(String[] args) {
        DidProofDriver driver = new DidProofDriver();
        
        String did = "did:proof:z6MkfrQC9BjKJzKVh6eNkJQZmUZ6a4Qz8EJGwE7VzKdKjNQ";
        Map<String, Object> options = new HashMap<>();
        options.put("accept", "application/did+ld+json");
        
        try {
            ResolveResult result = driver.resolve(did, options);
            System.out.println("DID Document: " + result.getDidDocument());
        } catch (ResolutionException e) {
            System.err.println("Resolution failed: " + e.getMessage());
        }
    }
}
```

## Example Usage

```java
import uniresolver.driver.did.proof.DidProofDriver;
import foundation.identity.did.DID;
import uniresolver.result.ResolveResult;

DidProofDriver driver = new DidProofDriver();

String didString = "did:proof:z6MkfrQC9BjKJzKVh6eNkJQZmUZ6a4Qz8EJGwE7VzKdKjNQ?versionId=v1.0&publicationState=published";
DID did = DID.fromString(didString);

Map<String, Object> options = new HashMap<>();
options.put("accept", "application/did+ld+json");

ResolveResult result = driver.resolve(did, options);
```

## Configuration

### Universal Resolver Configuration

To add this driver to the Universal Resolver, add the following to your `application.yml`:

```yaml
uniresolver:
  drivers:
    - pattern: "^(did:proof:.+)$"
      url: ${uniresolver_web_driver_url_did_proof:http://driver-did-proof:8080/}
      testIdentifiers:
        - did:proof:z6MkfrQC9BjKJzKVh6eNkJQZmUZ6a4Qz8EJGwE7VzKdKjNQ
        - did:proof:sfABC123def456
      traits:
        deactivatable: true
        enumerable: false
        historyAvailable: true
        humanReadable: false
```

## Building

```bash
mvn clean install
```

## Testing

```bash
mvn test
```

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.