package uniresolver.driver.did.proof;

import foundation.identity.did.DID;
import org.junit.Test;
import uniresolver.result.ResolveResult;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class DidProofDriverTest {

    private final DidProofDriver driver = new DidProofDriver();

    @Test
    public void testResolveValidDid() throws Exception {
        String didString = "did:proof:z6MkfrQC9BjKJzKVh6eNkJQZmUZ6a4Qz8EJGwE7VzKdKjNQ";
        DID did = DID.fromString(didString);

        Map<String, Object> options = new HashMap<>();
        options.put("accept", "application/did+ld+json");

        ResolveResult result = driver.resolve(did, options);

        assertNotNull("Resolve result should not be null", result);
        assertNotNull("DID document should not be null", result.getDidDocument());
        assertEquals("DID should match", didString, result.getDidDocument().getId().toString());
        assertTrue("Should have @context", result.getDidDocument().getContexts() != null);
    }

    @Test
    public void testResolveDidWithQueryParams() throws Exception {
        String didUrl = "did:proof:z6MkfrQC9BjKJzKVh6eNkJQZmUZ6a4Qz8EJGwE7VzKdKjNQ?versionId=v1.0&publicationState=published";
        DID did = DID.fromString("did:proof:z6MkfrQC9BjKJzKVh6eNkJQZmUZ6a4Qz8EJGwE7VzKdKjNQ");

        Map<String, Object> options = new HashMap<>();
        options.put("accept", "application/did+ld+json");
        options.put("didUrl", didUrl);

        ResolveResult result = driver.resolve(did, options);

        assertNotNull("Resolve result should not be null", result);
        assertNotNull("DID document should not be null", result.getDidDocument());
        assertNotNull("Document metadata should contain versionId", result.getDidDocumentMetadata().get("versionId"));
        assertEquals("versionId should be v1.0", "v1.0", result.getDidDocumentMetadata().get("versionId"));
    }

    @Test
    public void testResolveInvalidMethod() throws Exception {
        DID did = DID.fromString("did:example:123");
        Map<String, Object> options = new HashMap<>();
        options.put("accept", "application/did+ld+json");

        ResolveResult result = driver.resolve(did, options);

        assertNull("Should return null for unsupported method", result);
    }

    @Test(expected = uniresolver.ResolutionException.class)
    public void testResolveInvalidDidFormat() throws Exception {
        DID did = DID.fromString("did:proof:invalid");
        Map<String, Object> options = new HashMap<>();
        options.put("accept", "application/did+ld+json");

        driver.resolve(did, options);
    }
}
