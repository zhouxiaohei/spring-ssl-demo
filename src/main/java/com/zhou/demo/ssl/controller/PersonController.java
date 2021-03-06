package com.zhou.demo.ssl.controller;

import com.cloud.demo.common.WebResponse;
import com.cloud.demo.common.bean.Person;
import com.zhou.demo.ssl.service.PersonService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @ClassName PersonController
 * @Description
 * @Author JackZhou
 **/
@RestController
@RequestMapping("/demo/bootswagger/person")
@Slf4j
@Api(tags = "人员管理")
public class PersonController {

   @Autowired
   private PersonService personService;

    @ApiOperation(value = "根据id查询", notes = "备注")
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public WebResponse<Person> getById(@PathVariable("id") String id){
        log.info("根据id {} 进行查询", id);
        return WebResponse.<Person>builder().result(personService.get(id)).build();
    }

    @ApiOperation("保存")
    @RequestMapping(value = "/save", method = RequestMethod.POST)
    public WebResponse<Person> save(@RequestBody Person person){
        log.info("调用保存接口");
        return WebResponse.<Person>builder().result(personService.save(person)).build();
    }

    @ApiOperation("删除")
    //@ApiImplicitParams({@ApiImplicitParam(name = "id", value = "用户id")})
    @ApiImplicitParam(name = "id", value = "用户id")
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public WebResponse<Person> delete(@PathVariable("id") String id){
        log.info("调用删除接口");
        return WebResponse.<Person>builder().result(personService.delete(id)).build();
    }

    @ApiOperation("更新实体")
    @RequestMapping(value = "/update", method = RequestMethod.PUT)
    public WebResponse<Person> update(@ModelAttribute Person person){
        return WebResponse.<Person>builder().result(personService.update(person)).build();
    }

}
