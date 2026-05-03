package uniresolver.driver.did.proof;

import com.fasterxml.jackson.databind.ObjectMapper;
import foundation.identity.did.DID;
import foundation.identity.did.DIDDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uniresolver.DereferencingException;
import uniresolver.ResolutionException;
import uniresolver.driver.Driver;
import uniresolver.result.DereferenceResult;
import uniresolver.result.ResolveResult;

import java.net.URLDecoder;
import java.util.*;
import java.util.regex.Pattern;

public class DidProofDriver implements Driver {

    private static final Logger log = LoggerFactory.getLogger(DidProofDriver.class);
    private static final Pattern PROOF_DID_ID_PATTERN = Pattern.compile("^z[A-Za-z0-9]{20,}$");
    private static final String PROOF_METHOD = "proof";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private Map<String, Object> defaultProofMetadata = new HashMap<>();

    public DidProofDriver() {
        // Initialize with some default metadata if needed
    }

    @Override
    public ResolveResult resolve(DID did, Map<String, Object> resolutionOptions) throws ResolutionException {

        if (did == null) throw new NullPointerException("DID must not be null");
        if (!PROOF_METHOD.equals(did.getMethodName())) {
            return null;
        }

        String methodSpecificId = this.getMethodSpecificId(did);
        if (methodSpecificId == null || !PROOF_DID_ID_PATTERN.matcher(methodSpecificId).matches()) {
            throw new ResolutionException(ResolutionException.ERROR_INVALID_DID,
                    "Invalid proof DID ID format: " + methodSpecificId);
        }

        String didString = did.getDidString();
        if (resolutionOptions != null && resolutionOptions.get("didUrl") instanceof String) {
            didString = (String) resolutionOptions.get("didUrl");
        }

        Map<String, Object> proofMetadata = this.parseQueryMetadata(didString);
        Map<String, Object> mergedMetadata = new HashMap<>(defaultProofMetadata);
        mergedMetadata.putAll(proofMetadata);

        DIDDocument didDocument = this.createProofDocument(didString, mergedMetadata);

        ResolveResult resolveResult = ResolveResult.build();
        Map<String, Object> resolutionMetadata = new LinkedHashMap<>();
        resolutionMetadata.put("contentType", "application/did+ld+json");
        resolveResult.setDidResolutionMetadata(resolutionMetadata);
        resolveResult.setDidDocument(didDocument);
        resolveResult.setDidDocumentMetadata(this.buildDidDocumentMetadata(mergedMetadata));

        return resolveResult;
    }

    @Override
    public DereferenceResult dereference(foundation.identity.did.DIDURL didUrl, Map<String, Object> dereferenceOptions) throws DereferencingException, ResolutionException {
        return null;
    }

    @Override
    public Map<String, Object> properties() throws ResolutionException {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("method", PROOF_METHOD);
        properties.put("accept", "application/did+ld+json");
        properties.put("supportsDereference", false);
        properties.put("supportsOptions", false);
        return properties;
    }

    @Override
    public List<String> testIdentifiers() throws ResolutionException {
        return Collections.singletonList("did:proof:z6MkfrQC9BjKJzKVh6eNkJQZmUZ6a4Qz8EJGwE7VzKdKjNQ");
    }

    @Override
    public Map<String, Object> traits() throws ResolutionException {
        Map<String, Object> traits = new LinkedHashMap<>();
        traits.put("deactivatable", false);
        traits.put("enumerable", false);
        return traits;
    }

    public void setDefaultProofMetadata(Map<String, Object> defaultProofMetadata) {
        this.defaultProofMetadata = defaultProofMetadata != null ? defaultProofMetadata : new HashMap<>();
    }

    private String getMethodSpecificId(DID did) {
        try {
            return did.getMethodSpecificId();
        } catch (Throwable ignored) {
            String didString = did.getDidString();
            if (didString == null) return null;
            String[] parts = didString.split(":");
            return parts.length >= 3 ? parts[2] : null;
        }
    }

    private Map<String, Object> parseQueryMetadata(String didString) {
        Map<String, Object> metadata = new LinkedHashMap<>();

        if (didString != null && didString.contains("?")) {
            String queryString = didString.substring(didString.indexOf("?") + 1);
            try {
                String decodedQuery = URLDecoder.decode(queryString, "UTF-8");
                String[] pairs = decodedQuery.split("&");

                for (String pair : pairs) {
                    if (pair.contains("=")) {
                        String[] keyValue = pair.split("=", 2);
                        String key = keyValue[0];
                        String value = keyValue[1];
                        if ("storageEndEpoch".equals(key)) {
                            try {
                                metadata.put(key, Long.parseLong(value));
                            } catch (NumberFormatException e) {
                                // ignore invalid numbers
                            }
                        } else {
                            metadata.put(key, value);
                        }
                    }
                }
            } catch (Exception e) {
                if (log.isDebugEnabled()) log.debug("Failed to parse query parameters for DID URL", e);
            }
        }

        return metadata;
    }

    private Map<String, Object> buildDidDocumentMetadata(Map<String, Object> proofMetadata) {
        Map<String, Object> metadata = new LinkedHashMap<>();

        String[] metadataFields = {
                "versionId", "publicationState", "canonicalArtifactHash",
                "canonicalArtifactHashAlgorithm", "canonicalArtifactLocator",
                "storageProvider", "storageEndEpoch", "previousVersion",
                "latestVersion"
        };

        for (String field : metadataFields) {
            if (proofMetadata.containsKey(field)) {
                metadata.put(field, proofMetadata.get(field));
            }
        }

        Object deactivated = proofMetadata.get("deactivated");
        if ("true".equals(deactivated)) {
            metadata.put("deactivated", true);
        } else if ("false".equals(deactivated)) {
            metadata.put("deactivated", false);
        }

        return metadata;
    }

    private DIDDocument createProofDocument(String didString, Map<String, Object> proofMetadata) {
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("@context", Arrays.asList(
                "https://www.w3.org/ns/did/v1",
                "https://w3id.org/proof/v1"
        ));
        document.put("id", didString);

        if (!proofMetadata.isEmpty()) {
            document.put("proofMetadata", proofMetadata);
        }

        String canonicalArtifactLocator = proofMetadata.get("canonicalArtifactLocator") instanceof String ? (String) proofMetadata.get("canonicalArtifactLocator") : null;
        if (canonicalArtifactLocator != null && !canonicalArtifactLocator.isEmpty()) {
            List<Map<String, Object>> services = new ArrayList<>();
            Map<String, Object> service = new LinkedHashMap<>();
            service.put("id", didString + "#artifact");
            service.put("type", "ArtifactRetrievalService");
            service.put("serviceEndpoint", canonicalArtifactLocator);
            services.add(service);
            document.put("service", services);
        }

        return objectMapper.convertValue(document, DIDDocument.class);
    }
}
