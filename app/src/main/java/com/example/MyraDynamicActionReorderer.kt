package com.example

class MyraDynamicActionReorderer {
    fun reorder(actions: List<MyraAction>, currentPackage: String) =
        actions.toList() // No package metadata in MyraAction yet.
}
