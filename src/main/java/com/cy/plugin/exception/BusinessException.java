package com.cy.plugin.exception;

import com.cy.plugin.code.BasicResponseCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;

/**
 * @classDesc: 功能描述:(业务异常)
 * @author: cyjer
 * @date: 2022/11/21 15:30
 */
public class BusinessException extends RuntimeException {

    /**
     * 业务code
     */
    private String code;

    /**
     * 错误前缀
     */
    private String prefix;

    private static final Logger log = LoggerFactory.getLogger(BusinessException.class);

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    private static final String REPLACE_STR = "$";

    /**
     * 根据错误code、错误信息构造业务异常
     *
     * @param errorCode
     */
    public static BusinessException build(BasicResponseCode errorCode, String... params) {
        return new BusinessException(errorCode.prefix(), errorCode.getCode(), executeMsg(errorCode.getMsg(), params), params);
    }

    @Override
    public Throwable fillInStackTrace() {
        return this;
    }

    /**
     * 构造业务异常
     *
     * @param prefix  异常前缀
     * @param code    异常编码
     * @param message 异常信息
     * @param params  异常参数
     * @return
     */
    public static BusinessException build(String prefix, String code, String message, String... params) {
        return new BusinessException(prefix, code, message, params);
    }

    /**
     * 处理消息
     *
     * @param msg
     * @param params
     * @return
     */
    private static String executeMsg(String msg, String... params) {
        try {
            if (REPLACE_STR.equals(msg) && params != null && params.length > 0) {
                return params[0];
            }
            if (msg.contains(REPLACE_STR) && params != null && params.length > 0) {
                for (int i = 0; i < params.length; i++) {
                    String param = params[i];
                    if (param != null) {
                        msg = msg.replaceAll("\\$" + (i + 1), Matcher.quoteReplacement(param));
                    }
                }
            }
            return msg;
        } catch (Exception e) {
            log.error("invoke executeMsg error", e);
            return msg;
        }
    }

    /**
     * 根据错误code、错误信息构造业务异常
     *
     * @param code    业务code
     * @param message 错误信息
     */
    protected BusinessException(String prefix, String code, String message, String[] params) {
        super(executeMsg(message, null));
        this.prefix = prefix;
        this.code = code;
    }


    /**
     * @param errorCode
     */
    protected BusinessException(BasicResponseCode errorCode) {
        super(executeMsg(errorCode.getMsg(), null));
        this.prefix = errorCode.prefix();
        this.code = errorCode.getCode();
    }

    /**
     * @param errorCode
     * @param params
     */
    protected BusinessException(BasicResponseCode errorCode, String... params) {
        super(executeMsg(errorCode.getMsg(), params));
        this.prefix = errorCode.prefix();
        this.code = errorCode.getCode();
    }

    public static void main(String[] args) {

        String msg = "test,$1";

        String abc = executeMsg(msg, Matcher.quoteReplacement(""));

        System.out.println(abc);

    }

    @Override
    public String toString() {
        return "BusinessException(code=" + this.getCode() + ", prefix=" + this.getPrefix() + ", msg=" + super.getMessage() + ")";
    }

}
