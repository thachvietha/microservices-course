package com.programmingtechie.product_service.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@Document(value = "product")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Product {
    @Id
    String id;
    String name;
    String description;
    BigDecimal price;
}
