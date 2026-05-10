package com.tony.kingdetective.bean.response.ops;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SftpListRsp {
    private String path;
    private List<Entry> entries;

    @Data
    @Builder
    public static class Entry {
        private String name;
        private String path;
        private Boolean directory;
        private Long size;
        private Integer permissions;
        private Long modifiedTime;
    }
}
