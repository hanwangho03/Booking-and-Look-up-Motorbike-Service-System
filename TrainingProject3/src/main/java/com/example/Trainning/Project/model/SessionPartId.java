package com.example.Trainning.Project.model;

import jakarta.persistence.Embeddable;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.io.Serializable;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionPartId implements Serializable {
    private Long serviceSessionId;
    private Long partId;
}