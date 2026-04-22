package com.project.NewBank.model;

import com.project.NewBank.model.Enum.RoleName;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@Data
@Table(name="roles")
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(jakarta.persistence.EnumType.STRING)
    @Column(name = "role_name", length = 20)
    private RoleName name;


    public Role(RoleName name) {
        this.name = name;
    }

    public Role(Long id, RoleName name) {
        this.id = id;
        this.name = name;
    }

    public RoleName getName() {
        return name;
    }

    public void setName(RoleName name) {
        this.name = name;
    }
}
