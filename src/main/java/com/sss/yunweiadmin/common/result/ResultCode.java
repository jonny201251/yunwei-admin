package com.sss.yunweiadmin.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

//状态码和信息描述
@Getter
@AllArgsConstructor
public enum ResultCode {
    //状态码=0,java代码抛出异常
    //成功
    SUCCESS(200, "操作成功"),
    //操作失败
    FAIL(2, "操作失败"),
    //下拉树、级联菜单，非叶子节点不能删除
    NOT_LEAF_NODE(3, "请先删除该节点下的所有节点"),

    //参数错误，101-199
    PARAM_INVALID(101, "参数无效"),
    PARAM_BLANK(102, "参数为空"),
    PARAM_TYPE_ERROR(103, "参数类型错误"),
    PARAM_MISS(104, "缺少参数"),
    //用户错误，201-299
    USER_NOT_LOGIN(201, "用户未登录"),
    USER_LOGIN_ERROR(202, "账号不存在或密码错误"),
    USER_ACCOUNT_INVALID(203, "账号无效，已被禁用"),
    USER_NOT_EXIST(204, "用户不存在"),
    USER_HAS_EXIST(205, "用户已存在");


    //状态码
    private Integer code;
    //信息描述
    private String msg;
}
