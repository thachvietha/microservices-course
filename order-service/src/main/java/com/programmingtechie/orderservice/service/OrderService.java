package com.programmingtechie.orderservice.service;

import com.programmingtechie.orderservice.dto.InventoryResponse;
import com.programmingtechie.orderservice.dto.OrderLineItemsDto;
import com.programmingtechie.orderservice.dto.OrderRequest;
import com.programmingtechie.orderservice.model.Order;
import com.programmingtechie.orderservice.model.OrderLineItems;
import com.programmingtechie.orderservice.repository.OrderRepository;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {
    private final OrderRepository orderRepository;
    private final WebClient.Builder webClientBuilder;
private  final Tracer tracer;
    public String placeOrder(OrderRequest orderRequest) {
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());
        order.setOrderLineItemsList(orderRequest.getOrderLineItemsDtoList()
                .stream()
                .map(this::mapToOrderLineItems)
                .toList()
        );
        List<String> skuCodes = order.getOrderLineItemsList().stream()
                .map(OrderLineItems::getSkuCode)
                .toList();
        Span inventoryServiceLookup=tracer.nextSpan().name("InventoryServiceLookup");

        try (Tracer.SpanInScope spanInScope=  tracer.withSpan(inventoryServiceLookup.start())){
            InventoryResponse[] inventoryResponsesArray = webClientBuilder.build().get()
                    .uri("http://inventory-service/api/inventory",
                            uriBuilder -> uriBuilder.queryParam("skuCode", skuCodes).build())
                    .retrieve()
                    .bodyToMono(InventoryResponse[].class).block();
            boolean  allProductsInStock= Arrays.stream(inventoryResponsesArray).allMatch(InventoryResponse::getIsInStock);


            if( allProductsInStock){
                orderRepository.save(order);
                return "Order placed successfully";
            }
            else {
                throw new  IllegalArgumentException("Product is not in stock");
            }
        }
        finally {
            inventoryServiceLookup.end();
        }

    }

    private OrderLineItems mapToOrderLineItems(OrderLineItemsDto orderLineItems) {
        return OrderLineItems.builder()
                .skuCode(orderLineItems.getSkuCode())
                .quantity(orderLineItems.getQuantity())
                .price(orderLineItems.getPrice())
                .id(orderLineItems.getId())
                .build();
    }
}
