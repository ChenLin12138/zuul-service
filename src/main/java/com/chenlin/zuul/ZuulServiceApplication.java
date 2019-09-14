package com.chenlin.zuul;

import java.util.Collections;
import java.util.List;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import com.chenlin.zuul.utils.UserContextInterceptor;

@SpringBootApplication
//使服务称为一个Zuul服务器
@EnableZuulProxy
@ServletComponentScan(basePackages = "com.chenlin.zuul.*")
public class ZuulServiceApplication {

	@LoadBalanced
	@Bean
	public RestTemplate getRestTemplate() {
		RestTemplate template = new RestTemplate();
		List<ClientHttpRequestInterceptor> interceptors = template.getInterceptors();
		if(interceptors==null) {
			template.setInterceptors(Collections.singletonList(new UserContextInterceptor()));
		}else {
			interceptors.add(new UserContextInterceptor());
		}
		return template;
	}
	
	public static void main(String[] args) {
		SpringApplication.run(ZuulServiceApplication.class, args);
	}

}
