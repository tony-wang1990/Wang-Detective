package com.tony.kingdetective.bean.params.rescue;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class NetbootActionParams {

    @NotBlank(message = "SSH host id cannot be blank")
    private String hostId;

    @NotBlank(message = "Confirmation cannot be blank")
    private String confirmation;

    private Boolean reboot = false;
}
