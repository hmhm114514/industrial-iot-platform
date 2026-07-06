package com.practice.visual;
import org.springframework.boot.SpringApplication;import org.springframework.boot.autoconfigure.SpringBootApplication;import org.springframework.context.annotation.ComponentScan;
@SpringBootApplication
@ComponentScan(basePackages={"com.practice.visual","com.practice.common"})
public class VisualVideoApplication { public static void main(String[] args){ SpringApplication.run(VisualVideoApplication.class,args); } }
