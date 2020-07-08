package com.zhou.demo.ssl.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableSwagger2
public class SslConfig {

    @Value("${spring.swagger.package:com.cloud.demo}")
    private String swaggerPackage;

    @Bean
    public Docket createRestApi() {
        return new Docket(DocumentationType.SWAGGER_2).apiInfo(apiInfo())
                .select()
                .apis(RequestHandlerSelectors.basePackage(swaggerPackage))
                .paths(PathSelectors.any())
                .build();
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder().title("spring-boot-swagger-demo")
                .description("这是一个简单的 Swagger API 演示")
                .version("1.0.0-SNAPSHOT")
                .build();
    }

    //下面是2.0的配置，1.x请搜索对应的设置
//    public ServletWebServerFactory servletContainer(){
//        TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory();
//        tomcat.addAdditionalTomcatConnectors(createHTTPConnector());
//        return tomcat;
//    }
//
//
//    private Connector createHTTPConnector(){
//        Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
//        //同时启用http（8080）、https（8443）两个端口
//        connector.setScheme("http");
//        connector.setSecure(false);
//        connector.setPort(11001);
//        connector.setRedirectPort(18443);
//        return connector;
//    }
}