package com.sky.controller.admin;

import com.sky.constant.MessageConstant;
import com.sky.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/admin/common")
@Slf4j
@Api(tags = "通用接口")
public class CommonController {

    private static final String FILE_PATH = "SpringbootFileCache";

    /**
     * 文件上传
     * @param file
     * @return
     */

    @PostMapping("/upload")
    @ApiOperation("文件上传")
    public Result<String> upload(MultipartFile file){
        //没有阿里云改成本地
        log.info("文件上传{}", file);
        //原文件名获得
        String originalFilename = file.getOriginalFilename();
        //截取文件后缀
        String substring = originalFilename.substring(originalFilename.lastIndexOf("."));
        //新文件名称
        String newFileName = UUID.randomUUID().toString()+ originalFilename + substring;
        if (file.isEmpty()){
            return Result.error("文件为空");
        }

        File newFile = new File("C:"+File.separator+FILE_PATH+File.separator+newFileName);
        try {
            //保存到目标位置
            file.transferTo(newFile);
            log.info("文件上传成功");
            return Result.success();
        } catch (IOException e) {
            return Result.error(MessageConstant.UPLOAD_FAILED);
        }



    }
}
