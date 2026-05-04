package com.tony.kingdetective.bean.response.oci.traffic;

import com.tony.kingdetective.bean.dto.ValueLabelDTO;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * @projectName: king-detective
 * @package: com.tony.kingdetective.bean.response.oci.traffic
 * @className: GetConditionRsp
 * @author: Tony Wang
 * @date: 2025/3/7 21:33
 */
@Data
public class GetConditionRsp {

    private List<ValueLabelDTO> regionOptions;
    private Map<String, List<ValueLabelDTO>> instanceOptions;
}
