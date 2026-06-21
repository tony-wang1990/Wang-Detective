package com.tony.kingdetective.bean.params.rescue;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RescueSshActionParams {

    @NotBlank(message = "SSH host id cannot be blank")
    private String hostId;
}
