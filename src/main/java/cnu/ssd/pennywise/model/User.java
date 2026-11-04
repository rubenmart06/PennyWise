package cnu.ssd.pennywise.model;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString(onlyExplicitlyIncluded = true)
public class User {
    private Long id;
    @ToString.Include
    private String username;
    private String email;
}
