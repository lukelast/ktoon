package com.lukelast.ktoon.demo

import kotlinx.serialization.Serializable

@Serializable
data class ParentCompany(val name: String, val leader: Ceo, val organizations: List<Company>)

@Serializable data class Ceo(val name: String)

@Serializable data class Company(val name: String, val employees: List<User>)

@Serializable data class User(val id: Int, val name: String, val role: String)
