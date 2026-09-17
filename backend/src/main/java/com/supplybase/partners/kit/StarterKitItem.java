package com.supplybase.partners.kit;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "starter_kit_item")
@Getter
@Setter
@NoArgsConstructor
public class StarterKitItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "kit_id", nullable = false)
    private Long kitId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false)
    private int quantity = 1;
}
