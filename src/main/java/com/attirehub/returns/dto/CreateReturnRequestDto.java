package com.attirehub.returns.dto;

import com.attirehub.returns.enums.OrderReturnReason;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateReturnRequestDto {

    @NotBlank
    private String orderNumber;

    @NotBlank
    @Email
    private String email;

    @NotNull
    private OrderReturnReason reason;

    @NotEmpty
    private List<Long> variantIds;

    private String photoUrls;
}
