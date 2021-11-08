package com.sss.yunweiadmin.common.result;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;
import org.apache.poi.ss.formula.functions.T;

import java.io.Serializable;

@Data
public class ResponseResult implements Serializable {
    private static final long serialVersionUID = 5876920254332715285L;
    private Integer code;
    private String msg;
    private Object data;

    public static ResponseResult success() {
        ResultCode code = ResultCode.SUCCESS;
        ResponseResult responseResult = new ResponseResult();
        responseResult.setCode(code.getCode());
        responseResult.setMsg(code.getMsg());
        responseResult.setData(code.getMsg());
        return responseResult;
    }

    public static ResponseResult success(Object data) {
        ResponseResult responseResult = success();
        if (data instanceof IPage) {
            IPage<T> page = (IPage<T>) data;
            int pageSize = (int) page.getSize();
            int total = (int) page.getTotal();
            //计算总页数
            int totalPage = total / pageSize + ((total % pageSize == 0) ? 0 : 1);
            PaginationData<T> paginationVO = new PaginationData<>((int) page.getCurrent(), pageSize, total, totalPage, page.getRecords());
            responseResult.setData(paginationVO);
        } else {
            responseResult.setData(data);
        }

        return responseResult;
    }

    public static ResponseResult fail() {
        ResultCode code = ResultCode.FAIL;
        ResponseResult responseResult = new ResponseResult();
        responseResult.setCode(code.getCode());
        responseResult.setMsg(code.getMsg());
        return responseResult;
    }

    public static ResponseResult fail(String msg) {
        ResponseResult responseResult = new ResponseResult();
        responseResult.setCode(0);
        responseResult.setMsg(msg);
        return responseResult;
    }

    public static ResponseResult fail(ResultCode code) {
        ResponseResult responseResult = new ResponseResult();
        responseResult.setCode(code.getCode());
        responseResult.setMsg(code.getMsg());
        return responseResult;
    }
}
