package uniresolver.driver.did.proof;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class DidProofDriverTest {

    private final DidProofDriver driver = new DidProofDriver();

    @Test
    public void testResolveValidDid() throws Exception {
        String didString = "did:proof:z6MkfrQC9BjKJzKVh6eNkJQZmUZ6a4Qz8EJGwE7VzKdKjNQ";
        Map<String, Object> options = new HashMap<>();
        options.put("accept", "application/did+ld+json");

        DidProofDriver.ResolveResult result = driver.resolve(didString, options);

        assertNotNull("Resolve result should not be null", result);
        assertNotNull("DID document should not be null", result.getDidDocument());
        assertEquals("DID should match", didString, result.getDidDocument().get("id"));
        assertTrue("Should have @context", result.getDidDocument().get("@context") != null);
    }

    @Test
    public void testResolveDidWithQueryParams() throws Exception {
        String didString = "did:proof:z6MkfrQC9BjKJzKVh6eNkJQZmUZ6a4Qz8EJGwE7VzKdKjNQ?versionId=v1.0&publicationState=published";
        Map<String, Object> options = new HashMap<>();
        options.put("accept", "application/did+ld+json");

        DidProofDriver.ResolveResult result = driver.resolve(didString, options);

        assertNotNull("Resolve result should not be null", result);
        assertNotNull("DID document should not be null", result.getDidDocument());
        assertNotNull("Document metadata should contain versionId", result.getDidDocumentMetadata().get("versionId"));
        assertEquals("versionId should be v1.0", "v1.0", result.getDidDocumentMetadata().get("versionId"));
    }

    @Test
    public void testResolveInvalidMethod() throws Exception {
        String didString = "did:example:123";
        Map<String, Object> options = new HashMap<>();
        options.put("accept", "application/did+ld+json");

        DidProofDriver.ResolveResult result = driver.resolve(didString, options);

        assertNull("Should return null for unsupported method", result);
    }

    @Test(expected = DidProofDriver.ResolutionException.class)
    public void testResolveInvalidDidFormat() throws Exception {
        String didString = "did:proof:invalid";
        Map<String, Object> options = new HashMap<>();
        options.put("accept", "application/did+ld+json");

        driver.resolve(didString, options);
    }
}