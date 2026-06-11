package com.springboot.POS.modal;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.springboot.POS.domain.UserRole;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @Column(nullable = false)
    private String fullName;
    @Column(nullable = false, unique = true)
    @Email(message = "Email should be valid")
    private String email;

    @ManyToOne
    @JoinColumn(name = "store_id")
    @JsonIgnore
    private Store store;


    @ManyToOne
    @JoinColumn(name = "branch_id")
    @JsonIgnore
    private Branch branch;

    private String phone;

    @Column(nullable = false)
    private UserRole role;

    @Column(nullable = false)
    private String password;

    @Column(name = "created_at")
    @org.hibernate.annotations.CreationTimestamp
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    @org.hibernate.annotations.UpdateTimestamp
    private LocalDateTime updatedAt;
    
    private LocalDateTime lastLogin;

    private Boolean deleted = false;

    @Column(nullable = false)
    private String status = "active";

    // Expose only IDs in JSON response
    public Long getStoreId() {
        return store != null ? store.getId() : null;
    }

    public Long getBranchId() {
        return branch != null ? branch.getId() : null;
    }

}
