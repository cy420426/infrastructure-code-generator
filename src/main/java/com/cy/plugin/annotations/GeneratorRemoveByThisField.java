package com.cy.plugin.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @classDesc: 生成字段删除注解
 * @author: cyjer
 * @date: 2023/3/6 12:50
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
public @interface GeneratorRemoveByThisField {
    boolean batch() default false;
}
