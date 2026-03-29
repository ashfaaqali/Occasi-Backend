package com.occasi.application.service

import com.razorpay.Order
import com.razorpay.RazorpayClient
import com.razorpay.RazorpayException
import org.json.JSONObject
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Service
class RazorpayService(
    @Value("\${razorpay.key-id}") private val keyId: String,
    @Value("\${razorpay.key-secret}") private val keySecret: String
) {
    private val razorpayClient: RazorpayClient by lazy {
        RazorpayClient(keyId, keySecret)
    }

    fun createOrder(amountInPaise: Int, bookingId: Long): String {
        try {
            val orderRequest = JSONObject()
            orderRequest.put("amount", amountInPaise)
            orderRequest.put("currency", "INR")
            orderRequest.put("receipt", "booking_$bookingId")

            val order: Order = razorpayClient.orders.create(orderRequest)
            return order.get("id")
        } catch (e: RazorpayException) {
            throw RuntimeException("Failed to create Razorpay order: ${e.message}", e)
        }
    }

    fun verifySignature(orderId: String, paymentId: String, signature: String): Boolean {
        val data = "$orderId|$paymentId"
        val generatedSignature = hmacSha256(data, keySecret)
        return generatedSignature == signature
    }

    fun initiateRefund(paymentId: String, amountInPaise: Int): String {
        try {
            val refundRequest = JSONObject()
            refundRequest.put("amount", amountInPaise)

            val refund = razorpayClient.payments.refund(paymentId, refundRequest)
            return refund.get("id")
        } catch (e: RazorpayException) {
            throw RuntimeException("Failed to initiate refund: ${e.message}", e)
        }
    }

    private fun hmacSha256(data: String, secret: String): String {
        val algorithm = "HmacSHA256"
        val mac = Mac.getInstance(algorithm)
        val keySpec = SecretKeySpec(secret.toByteArray(), algorithm)
        mac.init(keySpec)
        val hash = mac.doFinal(data.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }
}
