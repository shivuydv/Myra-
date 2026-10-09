package com.example

class MyraScreenStateTracker {
    private var oldPackage = ""
    private var oldClass = ""

    fun update(packageName: String, className: String) {
        oldPackage = packageName
        oldClass = className
    }

    fun hasChanged(packageName: String, className: String) =
        packageName != oldPackage || className != oldClass

    fun packageName() = oldPackage
    fun className() = oldClass
}
