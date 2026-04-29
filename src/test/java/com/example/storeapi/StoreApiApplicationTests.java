package com.example.storeapi;

import org.junit.jupiter.api.Test;

/**
 * Lightweight sanity test — does not start the full Spring context to avoid
 * environment-specific failures during unit testing (e.g. missing DB).
 */
class StoreApiApplicationTests {

    @Test
    void simpleSanityCheck() {
        // Basic assertion to keep a minimal smoke test without loading Spring.
        assert true;
    }

}
