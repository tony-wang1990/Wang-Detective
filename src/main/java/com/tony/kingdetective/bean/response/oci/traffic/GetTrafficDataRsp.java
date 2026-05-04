package com.tony.kingdetective.bean.response.oci.traffic;

import lombok.Data;

import java.util.List;

/**
 * @projectName: king-detective
 * @package: com.tony.kingdetective.bean.response.oci.traffic
 * @className: GetTrafficDataRsp
 * @author: Tony Wang
 * @date: 2025/3/7 21:26
 */
@Data
public class GetTrafficDataRsp {

    List<String> time;
    List<String> inbound;
    List<String> outbound;
}
