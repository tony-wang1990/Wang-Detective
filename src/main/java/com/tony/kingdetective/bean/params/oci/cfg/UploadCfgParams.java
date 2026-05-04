package com.tony.kingdetective.bean.params.oci.cfg;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * @projectName: king-detective
 * @package: com.tony.kingdetective.bean.params
 * @className: UploadCfgParams
 * @author: Tony Wang
 * @date: 2024/11/18 22:11
 */
@Data
public class UploadCfgParams {

    @NotEmpty(message = "文件列表不能为空")
    private List<MultipartFile> fileList;
}
