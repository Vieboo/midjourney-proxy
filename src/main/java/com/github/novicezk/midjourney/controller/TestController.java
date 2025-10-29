package com.github.novicezk.midjourney.controller;

import cn.hutool.core.comparator.CompareUtil;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.github.novicezk.midjourney.VeoProperties;
import com.github.novicezk.midjourney.support.Task;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@Api(tags = "test")
@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class TestController {

    @Autowired
    private VeoProperties veoProperties;

    @ApiOperation(value = "查询所有任务")
    @GetMapping("/config")
    public VeoProperties config() {
        return veoProperties;
    }

}
