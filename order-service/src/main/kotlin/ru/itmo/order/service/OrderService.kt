package ru.itmo.order.service

import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.itmo.order.exception.BadRequestException
import ru.itmo.order.exception.ResourceNotFoundException
import ru.itmo.order.model.dto.response.OrderResponse
import ru.itmo.order.model.dto.response.OrderItemResponse
import ru.itmo.order.model.dto.response.PaginatedResponse
import ru.itmo.order.model.entity.Order
import ru.itmo.order.model.entity.OrderItem
import ru.itmo.order.model.enums.OrderStatus
import ru.itmo.order.repository.OrderRepository
import ru.itmo.order.repository.OrderItemRepository
import ru.itmo.order.service.client.ProductServiceClient
import java.math.BigDecimal

@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository,
    private val productServiceClient: ProductServiceClient
) {

    fun getCart(userId: Long): OrderResponse {
        val cart = orderRepository.findByUserIdAndStatus(userId, OrderStatus.CART)
            .orElseGet {
                val newCart = Order(
                    userId = userId,
                    status = OrderStatus.CART,
                    totalPrice = BigDecimal.ZERO
                )
                orderRepository.save(newCart)
            }
        return cart.toResponse()
    }

    @Transactional
    fun addToCart(userId: Long, productId: Long, quantity: Int): OrderResponse {
        val cart = orderRepository.findByUserIdAndStatus(userId, OrderStatus.CART)
            .orElseGet {
                val newCart = Order(
                    userId = userId,
                    status = OrderStatus.CART,
                    totalPrice = BigDecimal.ZERO
                )
                orderRepository.save(newCart)
            }

        
        val product = productServiceClient.getProductById(productId)

        val existingItem = orderItemRepository.findByOrderIdAndProductId(cart.id, productId)
        
        if (existingItem.isPresent) {
            val item = existingItem.get()
            val updatedItem = item.copy(quantity = item.quantity + quantity)
            orderItemRepository.save(updatedItem)
        } else {
            val newItem = OrderItem(
                orderId = cart.id,
                productId = productId,
                quantity = quantity,
                price = product.price
            )
            orderItemRepository.save(newItem)
        }

        val items = orderItemRepository.findAllByOrderId(cart.id)
        val newTotalPrice = items.fold(BigDecimal.ZERO) { acc, orderItem ->
            acc + orderItem.price.multiply(BigDecimal(orderItem.quantity))
        }

        val updatedCart = cart.copy(totalPrice = newTotalPrice)
        val savedCart = orderRepository.save(updatedCart)
        return savedCart.toResponse()
    }

    @Transactional
    fun updateCartItemQuantity(userId: Long, itemId: Long, quantity: Int): OrderResponse {
        val cart = orderRepository.findByUserIdAndStatus(userId, OrderStatus.CART)
            .orElseThrow { ResourceNotFoundException("No items in cart") }

        val item = orderItemRepository.findByOrderIdAndProductId(cart.id, itemId)
            .orElseThrow { ResourceNotFoundException("No such item in cart") }

        if (quantity <= 0) {
            orderItemRepository.deleteById(item.id)
        } else {
            val updatedItem = item.copy(quantity = quantity)
            orderItemRepository.save(updatedItem)
        }

        val items = orderItemRepository.findAllByOrderId(cart.id)
        val newTotalPrice = items.fold(BigDecimal.ZERO) { acc, orderItem ->
            acc + orderItem.price.multiply(BigDecimal(orderItem.quantity))
        }

        val updatedCart = cart.copy(totalPrice = newTotalPrice)
        val savedCart = orderRepository.save(updatedCart)
        return savedCart.toResponse()
    }

    @Transactional
    fun removeFromCart(userId: Long, itemId: Long): OrderResponse {
        val cart = orderRepository.findByUserIdAndStatus(userId, OrderStatus.CART)
            .orElseThrow { ResourceNotFoundException("No items in cart") }

        val item = orderItemRepository.findByOrderIdAndProductId(cart.id, itemId)
            .orElseThrow { ResourceNotFoundException("No such item in cart") }

        orderItemRepository.deleteById(item.id)


        val items = orderItemRepository.findAllByOrderId(cart.id)
        val newTotalPrice = items.fold(BigDecimal.ZERO) { acc, orderItem ->
            acc + orderItem.price.multiply(BigDecimal(orderItem.quantity))
        }

        val updatedCart = cart.copy(totalPrice = newTotalPrice)
        val savedCart = orderRepository.save(updatedCart)
        return savedCart.toResponse()
    }

    @Transactional
    fun clearCart(userId: Long) {
        val cart = orderRepository.findByUserIdAndStatus(userId, OrderStatus.CART)
            .orElseThrow { ResourceNotFoundException("Корзина не найдена") }

        orderItemRepository.deleteByOrderId(cart.id)
        val updatedCart = cart.copy(totalPrice = BigDecimal.ZERO)
        orderRepository.save(updatedCart)
    }

    @Transactional
    fun createOrder(userId: Long, deliveryAddress: String): OrderResponse {
        val cart = orderRepository.findByUserIdAndStatus(userId, OrderStatus.CART)
            .orElseThrow { BadRequestException("Корзина не найдена") }

        val items = orderItemRepository.findAllByOrderId(cart.id)
        if (items.isEmpty()) {
            throw BadRequestException("Корзина пуста. Добавьте товары перед оформлением заказа")
        }

        val pendingOrder = cart.copy(
            status = OrderStatus.PENDING,
            deliveryAddress = deliveryAddress
        )
        val savedOrder = orderRepository.save(pendingOrder)

        val newCart = Order(
            userId = userId,
            status = OrderStatus.CART,
            totalPrice = BigDecimal.ZERO
        )
        orderRepository.save(newCart)

        return savedOrder.toResponse()
    }

    fun getUserOrders(userId: Long, page: Int, pageSize: Int): PaginatedResponse<OrderResponse> {
        val pageable = PageRequest.of(page - 1, pageSize)
        val orderPage = orderRepository.findAllByUserIdAndStatusNot(userId, OrderStatus.CART, pageable)
        return PaginatedResponse(
            data = orderPage.content.map { it.toResponse() },
            page = page,
            pageSize = pageSize,
            totalElements = orderPage.totalElements,
            totalPages = orderPage.totalPages
        )
    }

    fun getOrderById(orderId: Long, userId: Long): OrderResponse {
        val order = orderRepository.findByIdAndUserId(orderId, userId)
            .orElseThrow { ResourceNotFoundException("Заказ с ID $orderId не найден") }
        return order.toResponse()
    }

    private fun Order.toResponse(): OrderResponse {
        val items = orderItemRepository.findAllByOrderId(this.id)
        val itemResponses = items.map { item ->
            val productDto = productServiceClient.getProductById(item.productId)

            OrderItemResponse(
                id = item.id,
                product = productDto,
                quantity = item.quantity,
                price = item.price,
                subtotal = item.price.multiply(BigDecimal(item.quantity)),
                createdAt = item.createdAt
            )
        }

        return OrderResponse(
            id = this.id,
            userId = this.userId,
            items = itemResponses,
            totalPrice = this.totalPrice,
            status = this.status.name,
            deliveryAddress = this.deliveryAddress,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
        )
    }
}