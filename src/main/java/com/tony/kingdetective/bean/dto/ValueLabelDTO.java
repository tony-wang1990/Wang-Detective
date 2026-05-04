package com.tony.kingdetective.bean.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @projectName: king-detective
 * @package: com.tony.kingdetective.bean.dto
 * @className: ValueLabelDTO
 * @author: Tony Wang
 * @date: 2025/3/7 21:16
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ValueLabelDTO {

    private String label;
    private String value;
}
