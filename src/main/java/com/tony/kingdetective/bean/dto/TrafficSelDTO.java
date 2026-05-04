package com.tony.kingdetective.bean.dto;

import lombok.Data;

import java.util.List;

/**
 * @projectName: king-detective
 * @package: com.tony.kingdetective.bean.dto
 * @className: TrafficSelDTO
 * @author: Tony Wang
 * @date: 2025/3/7 23:59
 */
@Data
public class TrafficSelDTO {

    private String key;
    private List<ValueLabelDTO> list;
}
