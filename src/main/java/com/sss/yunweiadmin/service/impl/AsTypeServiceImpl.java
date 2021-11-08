package com.sss.yunweiadmin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sss.yunweiadmin.mapper.AsTypeMapper;
import com.sss.yunweiadmin.model.entity.AsType;
import com.sss.yunweiadmin.service.AsTypeService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 任勇林
 * @since 2021-10-08
 */
@Service
public class AsTypeServiceImpl extends ServiceImpl<AsTypeMapper, AsType> implements AsTypeService {

    @Override
    public AsType getAsType(Integer typeId) {
        AsType asType;
        List<AsType> list = this.list();
        Map<Integer, AsType> map = list.stream().collect(Collectors.toMap(AsType::getId, AsType -> AsType));
        while (true) {
            AsType asTypeTmp = map.get(typeId);
            if (asTypeTmp.getLevel() == 2) {
                asType = asTypeTmp;
                break;
            } else {
                typeId = asTypeTmp.getPid();
            }
        }
        return asType;
    }

    @Override
    public AsType getAsType(String typeName) {
        return this.getOne(new QueryWrapper<AsType>().eq("name", typeName));
    }
}
