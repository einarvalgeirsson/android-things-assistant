package com.jayway.androidthingsassistant.util

val Any.TAG: String
    get() {
        val klass = this.javaClass

        return if(klass.isAnonymousClass) {
            klass.enclosingClass.simpleName
        } else {
            klass.simpleName
        }
    }