package uniresolver.driver.did.proof;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

public class DidProofDriver {

    private static final Pattern PROOF_DID_ID_PATTERN = Pattern.compile("^z[A-Za-z0-9]{20,}$");
    private static final String PROOF_METHOD = "proof";

    // Default proof metadata
    private Map<String, Object> defaultProofMetadata = new HashMap<>();

    public DidProofDriver() {
        // Initialize with some default metadata if needed
    }

    public ResolveResult resolve(String didString, Map<String, Object> resolutionOptions) throws ResolutionException {

        // Parse DID
        if (!didString.startsWith("did:")) {
            return null;
        }

        // Remove query parameters if present
        String didWithoutQuery = didString.split("\\?")[0];

        String[] parts = didWithoutQuery.split(":");
        if (parts.length < 3 || !PROOF_METHOD.equals(parts[1])) {
            return null;
        }

        String methodSpecificId = parts[2];

        // Validate DID ID format
        if (!PROOF_DID_ID_PATTERN.matcher(methodSpecificId).matches()) {
            throw new ResolutionException(ResolutionException.ERROR_INVALID_DID,
                "Invalid proof DID ID format: " + methodSpecificId);
        }

        // Parse query parameters for metadata
        Map<String, Object> proofMetadata = parseQueryMetadata(didString);

        // Merge with default metadata
        Map<String, Object> mergedMetadata = new HashMap<>(defaultProofMetadata);
        mergedMetadata.putAll(proofMetadata);

        // Create DID document
        Map<String, Object> didDocument = createProofDocument(didString, mergedMetadata);

        // Build resolve result
        ResolveResult resolveResult = new ResolveResult();
        resolveResult.setDidDocument(didDocument);
        Map<String, Object> resolutionMetadata = new HashMap<>();
        resolutionMetadata.put("contentType", "application/did+ld+json");
        resolveResult.setDidResolutionMetadata(resolutionMetadata);
        resolveResult.setDidDocumentMetadata(buildDidDocumentMetadata(didString));

        return resolveResult;
    }

    private Map<String, Object> parseQueryMetadata(String didString) {
        Map<String, Object> metadata = new HashMap<>();

        if (didString.contains("?")) {
            String queryString = didString.substring(didString.indexOf("?") + 1);
            try {
                String decodedQuery = URLDecoder.decode(queryString, "UTF-8");
                String[] pairs = decodedQuery.split("&");

                for (String pair : pairs) {
                    if (pair.contains("=")) {
                        String[] keyValue = pair.split("=", 2);
                        String key = keyValue[0];
                        String value = keyValue[1];

                        // Handle numeric fields
                        if ("storageEndEpoch".equals(key)) {
                            try {
                                metadata.put(key, Long.parseLong(value));
                            } catch (NumberFormatException e) {
                                // Ignore invalid numbers
                            }
                        } else {
                            metadata.put(key, value);
                        }
                    }
                }
            } catch (Exception e) {
                // Ignore parsing errors
            }
        }

        return metadata;
    }

    private Map<String, Object> buildDidDocumentMetadata(String didString) {
        Map<String, Object> metadata = new HashMap<>();
        Map<String, Object> queryMetadata = parseQueryMetadata(didString);

        // Copy relevant fields to document metadata
        String[] metadataFields = {
            "versionId", "publicationState", "canonicalArtifactHash",
            "canonicalArtifactHashAlgorithm", "canonicalArtifactLocator",
            "storageProvider", "storageEndEpoch", "previousVersion",
            "latestVersion"
        };

        for (String field : metadataFields) {
            if (queryMetadata.containsKey(field)) {
                metadata.put(field, queryMetadata.get(field));
            }
        }

        // Handle deactivated field
        String deactivated = (String) queryMetadata.get("deactivated");
        if ("true".equals(deactivated)) {
            metadata.put("deactivated", true);
        } else if ("false".equals(deactivated)) {
            metadata.put("deactivated", false);
        }

        return metadata;
    }

    private Map<String, Object> createProofDocument(String did, Map<String, Object> proofMetadata) {
        Map<String, Object> document = new HashMap<>();
        document.put("@context", Arrays.asList(
            "https://www.w3.org/ns/did/v1",
            "https://w3id.org/proof/v1"
        ));
        document.put("id", did);

        // Add proof metadata if present
        if (!proofMetadata.isEmpty()) {
            document.put("proofMetadata", proofMetadata);
        }

        // Add service endpoint if canonicalArtifactLocator is present
        String canonicalArtifactLocator = (String) proofMetadata.get("canonicalArtifactLocator");
        if (canonicalArtifactLocator != null && !canonicalArtifactLocator.toString().isEmpty()) {
            List<Map<String, Object>> services = new ArrayList<>();
            Map<String, Object> service = new HashMap<>();
            service.put("id", did + "#artifact");
            service.put("type", "ArtifactRetrievalService");
            service.put("serviceEndpoint", canonicalArtifactLocator);
            services.add(service);
            document.put("service", services);
        }

        return document;
    }

    public void setDefaultProofMetadata(Map<String, Object> defaultProofMetadata) {
        this.defaultProofMetadata = defaultProofMetadata != null ? defaultProofMetadata : new HashMap<>();
    }

    // Simple result classes
    public static class ResolveResult {
        private Map<String, Object> didDocument;
        private Map<String, Object> didResolutionMetadata = new HashMap<>();
        private Map<String, Object> didDocumentMetadata = new HashMap<>();

        public Map<String, Object> getDidDocument() { return didDocument; }
        public void setDidDocument(Map<String, Object> didDocument) { this.didDocument = didDocument; }

        public Map<String, Object> getDidResolutionMetadata() { return didResolutionMetadata; }
        public void setDidResolutionMetadata(Map<String, Object> didResolutionMetadata) { this.didResolutionMetadata = didResolutionMetadata; }

        public Map<String, Object> getDidDocumentMetadata() { return didDocumentMetadata; }
        public void setDidDocumentMetadata(Map<String, Object> didDocumentMetadata) { this.didDocumentMetadata = didDocumentMetadata; }
    }

    public static class ResolutionException extends Exception {
        public static final int ERROR_INVALID_DID = 1;

        private final int errorCode;

        public ResolutionException(int errorCode, String message) {
            super(message);
            this.errorCode = errorCode;
        }

        public int getErrorCode() { return errorCode; }
    }
}