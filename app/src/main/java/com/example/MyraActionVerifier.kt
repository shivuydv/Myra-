package com.example

class MyraActionVerifier {
    suspend fun verify(
        expectedElement: String = "",
        expectedPackage: String = "",
        timeout: Long = 4000L
    ): MyraVerificationResult {
        val v = MyraSmartVerifier().verify(
            expectedElement, expectedPackage, timeout
        )
        return MyraVerificationResult(v.success, v.message)
    }
}
