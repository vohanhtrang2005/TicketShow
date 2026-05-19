package com.waterpark.tickershow.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.waterpark.tickershow.enums.ShowTypeName;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "show_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShowType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 50)
    private ShowTypeName name;

    @Column(columnDefinition = "TEXT")
    private String description;

    // Back-reference: tránh JSON recursion
    @JsonIgnore
    @OneToMany(mappedBy = "showType")
    @Builder.Default
    private List<Show> shows = new ArrayList<>();
}
