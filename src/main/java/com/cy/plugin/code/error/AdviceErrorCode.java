package com.cy.plugin.code.error;

import com.cy.plugin.code.BasicResponseCode;

/**
 * @classDesc: 功能描述:()
 * @author: cyjer
 * @date: 2022/11/21 15:30
 */
public enum AdviceErrorCode implements BasicResponseCode {


    /**
     * 数据绑定异常
     */
    BIND_EX("0001", "数据绑定异常,$1"),
    /**
     * 参数校验失败错误
     */
    PARAM_NOT_VALID_EX("0002", "参数校验失败错误,$1"),
    /**
     * 数据验证异常
     */
    CONSTRAINT_EX("0003", "数据验证异常,$1"),
    /**
     * 系统异常
     */
    OTHER_EX("0004", "系统异常,$1"),

    /**
     * 客户端错误
     */
    CLIENT_ERROR("0005", "客户端错误,$1"),
    /**
     * 自定义错误
     */
    BASIC_ERROR("0006", "$1");

    private String code;
    private String msg;

    AdviceErrorCode(String code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public String getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }

    @Override
    public String prefix() {
        return "advice";
    }
}
