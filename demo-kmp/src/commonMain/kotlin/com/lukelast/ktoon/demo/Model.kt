package com.lukelast.ktoon.demo

import kotlinx.serialization.Serializable

@Serializable
data class Company(val name: String, val employees: List<User>)

@Serializable
data class User(val id: Int, val name: String, val role: String)
