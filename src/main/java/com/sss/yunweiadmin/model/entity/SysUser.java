package com.sss.yunweiadmin.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 用户表
 * </p>
 *
 * @author 任勇林
 * @since 2021-04-09
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class SysUser implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 登录名称
     */
    private String loginName;

    /**
     * 显示名称
     */
    private String displayName;

    /**
     * 登录密码
     */
    private String password;

    /**
     * 密级程度,值为机密，秘密，普通商密，核心商密，非密
     */
    private String secretDegree;

    /**
     * 手机号码
     */
    private String mobile;

    /**
     * 座机
     */
    private String telephone;

    /**
     * 位置
     */
    private String location;

    /**
     * 性别
     */
    private String gender;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 使用状态，正常和禁用
     */
    private String status;

    /**
     * 根据该字段进行排序显示
     */
    private Double sort;

    /**
     * 创建时间
     */
    private LocalDateTime createDatetime;

    /**
     * 备注
     */
    private String remark;

    /**
     * 部门id
     */
    private Integer deptId;
}
