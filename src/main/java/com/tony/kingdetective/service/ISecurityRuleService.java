package com.tony.kingdetective.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tony.kingdetective.bean.params.oci.securityrule.AddEgressSecurityRuleParams;
import com.tony.kingdetective.bean.params.oci.securityrule.AddIngressSecurityRuleParams;
import com.tony.kingdetective.bean.params.oci.securityrule.GetSecurityRuleListPageParams;
import com.tony.kingdetective.bean.params.oci.securityrule.RemoveSecurityRuleParams;
import com.tony.kingdetective.bean.response.oci.securityrule.SecurityRuleListRsp;

public interface ISecurityRuleService {

    Page<SecurityRuleListRsp.SecurityRuleInfo> page(GetSecurityRuleListPageParams params);

    void addIngress(AddIngressSecurityRuleParams params);

    void addEgress(AddEgressSecurityRuleParams params);

    void remove(RemoveSecurityRuleParams params);
}
