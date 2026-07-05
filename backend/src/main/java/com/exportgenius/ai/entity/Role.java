package com.exportgenius.ai.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {
    public static final String EXPORTER = "EXPORTER";
    public static final String ADMIN = "ADMIN";
    public static final String IMPORTER = "IMPORTER";

    @Id
    private Integer id;

    @Column(unique = true, nullable = false, length = 50)
    private String name;
}
