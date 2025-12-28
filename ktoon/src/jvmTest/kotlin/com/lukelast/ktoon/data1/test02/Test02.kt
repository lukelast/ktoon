package com.lukelast.ktoon.data1.test02

import com.lukelast.ktoon.data1.Runner
import kotlinx.serialization.Serializable

/** From https://toontools.vercel.app/tools/json-to-toon "E-commerce Data" */
class Test03 : Runner() {
    override fun run() = doTest(data)
}

val data =
    OrderRoot(
        order =
            Order(
                id = "ORD-2024-001",
                date = "2024-11-04T10:30:00Z",
                customer = Customer(id = 12345, name = "John Doe", email = "john@example.com"),
                items =
                    listOf(
                        OrderItem(
                            sku = "WIDGET-A1",
                            name = "Premium Widget",
                            qty = 2,
                            price = 29.99,
                        ),
                        OrderItem(
                            sku = "GADGET-B2",
                            name = "Smart Gadget",
                            qty = 1,
                            price = 149.99,
                        ),
                        OrderItem(sku = "TOOL-C3", name = "Pro Tool", qty = 3, price = 19.99),
                    ),
                shipping =
                    ShippingDetails(
                        address = "123 Main St",
                        city = "San Francisco",
                        state = "CA",
                        zip = "94102",
                        method = "express",
                    ),
                payment = PaymentDetails(method = "credit_card", last4 = "4242", amount = 269.94),
                status = "shipped",
            )
    )

@Serializable data class OrderRoot(val order: Order)

@Serializable
data class Order(
    val id: String,
    val date: String,
    val customer: Customer,
    val items: List<OrderItem>,
    val shipping: ShippingDetails,
    val payment: PaymentDetails,
    val status: String,
)

@Serializable data class Customer(val id: Int, val name: String, val email: String)

@Serializable
data class OrderItem(val sku: String, val name: String, val qty: Int, val price: Double)

@Serializable
data class ShippingDetails(
    val address: String,
    val city: String,
    val state: String,
    val zip: String,
    val method: String,
)

@Serializable data class PaymentDetails(val method: String, val last4: String, val amount: Double)
