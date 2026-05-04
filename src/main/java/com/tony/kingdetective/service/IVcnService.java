package com.tony.kingdetective.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tony.kingdetective.bean.params.oci.vcn.RemoveVcnParams;
import com.tony.kingdetective.bean.params.oci.vcn.VcnPageParams;
import com.tony.kingdetective.bean.response.oci.vcn.VcnPageRsp;

public interface IVcnService {

    Page<VcnPageRsp.VcnInfo> page(VcnPageParams params);

    void remove(RemoveVcnParams params);
}
