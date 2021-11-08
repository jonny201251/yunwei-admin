package com.sss.yunweiadmin.model.vo;

import lombok.Data;

import java.util.List;

@Data
public class TreeSelectVO {
    private String title;
    private Object value;
    private Object key;
    private List<TreeSelectVO> children;
}
