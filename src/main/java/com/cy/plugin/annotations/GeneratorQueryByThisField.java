package com.cy.plugin.annotations;

import com.cy.plugin.constant.ResultType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @classDesc: 生成字段查询注解
 * @author: cyjer
 * @date: 2023/3/6 12:50
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
public @interface GeneratorQueryByThisField {
    /**
     * 结果类型
     */
    ResultType resultType() default ResultType.ENTITY;
    boolean batch() default false;
}
