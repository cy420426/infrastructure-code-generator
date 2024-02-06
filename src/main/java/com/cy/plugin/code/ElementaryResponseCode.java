package com.cy.plugin.code;

/**
 * @classDesc: 功能描述:()
 * @author: cyjer
 * @date: 2022/11/21 15:30
 */
public enum ElementaryResponseCode implements BasicResponseCode {

    /**
     * 成功
     */
    SUCCESS("0", "success"),
    /**
     * 系统熔断
     */
    FALLBACK("system@9998", "系统繁忙,$1"),
    /**
     * 系统错误
     */
    SYSTEM_ERROR("system@9999", "系统错误,$1");


    private String code;
    private String msg;

    ElementaryResponseCode(String code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMsg() {
        return msg;
    }

    @Override
    public String prefix() {
        return "";
    }

}
