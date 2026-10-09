package com.example

class MyraSelfCorrectionEngine {
    fun correctionFor(r: MyraActionResult): String? = when (r.status) {
        MyraActionStatus.SUCCESS -> null
        MyraActionStatus.RETRY -> "अलग strategy से कोशिश करो."
        MyraActionStatus.WAITING -> "User से missing detail पूछो."
        MyraActionStatus.NEED_CONFIRMATION -> "Confirmation मांगो."
        MyraActionStatus.FAILED -> "Failure reason देखकर plan सुधारो."
    }
}
