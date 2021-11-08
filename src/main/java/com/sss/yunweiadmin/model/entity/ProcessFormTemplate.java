package com.sss.yunweiadmin.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * <p>
 * 自定义表单模板
 * </p>
 *
 * @author 任勇林
 * @since 2021-07-29
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class ProcessFormTemplate implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * label显示名称
     */
    private String label;

    /**
     * 对应基本组件中的name，比如input中的name,基本类型name=base+主键，表字段类型=表名+字段名称
     */
    private String name;

    /**
     * 数据类型，来自process_form_type，process_form_custorm,字段组
     */
    private String type;

    /**
     * 基本类型(字符串，数字)，自定义表类型，字段变更类型，字段组类型
     */
    private String flag;

    /**
     * 对应sys_dic中的自定义表单布局，用于字段组里的布局
     */
    private Integer groupLayout;

    /**
     * 用于子分组，关联父分组的label
     */
    private String groupParentLabel;

    /**
     * 发起流程时，是否允许进行字段组选择
     */
    private String haveGroupSelect;

    /**
     * 是否必填，值为是和否
     */
    private String required;

    /**
     * 提示
     */
    private String tooltip;

    /**
     * 下拉或者点击的单选或者复选的值，使用英文逗号隔开
     */
    private String value;

    /**
     * 默认值
     */
    private String defaultValue;

    /**
     * 关联process_definition.id
     */
    private Integer processDefinitionId;


}
