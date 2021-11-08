package com.sss.yunweiadmin.model.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class SysUserExcel {
    @ExcelProperty("登录账号")
    private String loginName;
    @ExcelProperty("显示姓名")
    private String displayName;
}
