package com.tony.kingdetective.bean.response.oci.vcn;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @projectName: king-detective
 * @package: com.tony.kingdetective.bean.response.oci.vcn
 * @className: VcnPageRsp
 * @author: Tony Wang
 * @date: 2025/3/3 20:22
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class VcnPageRsp<T> extends Page<T> {

    @Data
    public static class VcnInfo{
        private String id;
        private String displayName;
        private String status;
        private Boolean visibility;
        private String createTime;
    }

    public static <T> Page<T> buildPage(List<T> entities, long size, long current, long total) {
        VcnPageRsp<T> page = new VcnPageRsp<>();
        page.setRecords(entities);
        page.setSize(size);
        page.setCurrent(current);
        page.setTotal(total);
        page.setPages((long) (Math.ceil((double) total / size)));
        return page;
    }
}
