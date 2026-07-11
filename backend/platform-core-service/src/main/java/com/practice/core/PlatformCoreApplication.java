package com.practice.core;
import org.springframework.boot.SpringApplication;import org.springframework.boot.autoconfigure.SpringBootApplication;import org.springframework.context.annotation.ComponentScan;import org.springframework.scheduling.annotation.EnableScheduling;
@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages={"com.practice.core","com.practice.common"})
public class PlatformCoreApplication { public static void main(String[] args){ SpringApplication.run(PlatformCoreApplication.class,args); } }
