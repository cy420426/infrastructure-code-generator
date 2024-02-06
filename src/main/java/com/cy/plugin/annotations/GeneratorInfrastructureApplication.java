package com.cy.plugin.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @classDesc: 生成基础设施层应用
 * @author: cyjer
 * @date: 2023/3/6 12:50
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface GeneratorInfrastructureApplication {
    String generatorJavaPackageLocation() default "com.generator";
    String generatorXmlPackageLocation() default "mapper";

    String author() default "cyjer generator";
}
