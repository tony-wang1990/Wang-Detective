package com.tony.kingdetective.bean.response.oci.traffic;

import com.tony.kingdetective.bean.dto.ValueLabelDTO;
import lombok.Data;

import java.util.List;

/**
 * @projectName: king-detective
 * @package: com.tony.kingdetective.bean.response.oci.traffic
 * @className: FetchInstancesRsp
 * @author: Tony Wang
 * @date: 2025/3/7 22:32
 */
@Data
public class FetchInstancesRsp {

//    private List<ValueLabelDTO> instanceOptions;
    private String inboundTraffic;
    private String outboundTraffic;
    private Integer instanceCount;
}
