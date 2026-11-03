package com.zacksimpson.verses

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ApiKeysScreenTest {

    @Test
    fun `401 and 403 count as a rejected key`() {
        assertTrue(isKeyRejection(IllegalStateException("YouVersion API HTTP 401: Invalid ApiKey")))
        assertTrue(isKeyRejection(IllegalStateException("ESV API HTTP 403: bad token")))
    }

    @Test
    fun `other failures are not a rejection`() {
        assertFalse(isKeyRejection(IllegalStateException("ESV API HTTP 500: oops")))
        assertFalse(isKeyRejection(java.net.UnknownHostException("api.esv.org")))
    }
}
