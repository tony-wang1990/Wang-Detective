package com.tony.kingdetective.bean.params;

import lombok.Data;

/**
 * @projectName: king-detective
 * @package: com.tony.kingdetective.bean.params
 * @className: BasicPageParams
 * @author: Tony Wang
 * @date: 2025/3/3 21:01
 */
@Data
public class BasicPageParams {

    private String keyword;
    private int currentPage;
    private int pageSize;

    public long getOffset() {
        return (long) (currentPage - 1) * pageSize;
    }
}
