package com.lin.zuul;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;

@SpringBootApplication
//使服务称为一个Zuul服务器
@EnableZuulProxy
public class ZuulServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ZuulServiceApplication.class, args);
	}

}
