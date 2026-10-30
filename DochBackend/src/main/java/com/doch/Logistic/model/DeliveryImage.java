package com.doch.Logistic.model;

import com.doch.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

@Setter
@Getter
@Entity
@Table(name = "delivery_image")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryImage extends BaseEntity {
    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.RANDOM)
    @Column(columnDefinition = "VARCHAR(36)")
    private String id;

    private String imageUrl; // Minio key
    private String contentType;
}