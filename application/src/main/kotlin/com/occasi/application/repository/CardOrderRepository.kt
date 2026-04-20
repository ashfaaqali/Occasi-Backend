package com.occasi.application.repository

import com.occasi.application.model.CardOrder
import com.occasi.application.model.OrderStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CardOrderRepository : JpaRepository<CardOrder, Long> {
    fun findByCustomerIdOrderByOrderDateDesc(customerId: Long): List<CardOrder>
    fun findByCardIdAndCustomerIdAndIsSampleAndStatusNot(
        cardId: Long, customerId: Long, isSample: Boolean, status: OrderStatus
    ): List<CardOrder>
}
