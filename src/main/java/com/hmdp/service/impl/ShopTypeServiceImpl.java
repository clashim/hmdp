package com.hmdp.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_TYPE_KEY;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Autowired
    private StringRedisTemplate stringredisTemplate;

    @Override
    public Result queryTypeList() {
        // public static final String CACHE_SHOP_TYPE_KEY = "cache:shopType:";
        String key = CACHE_SHOP_TYPE_KEY;

        // 1. 尝试从 Redis 获取（用 String 存整个列表更优）
        String json = stringredisTemplate.opsForValue().get(key);
        if (StrUtil.isNotEmpty(json)) {
            // 检查是否命中了“空结果”缓存
            if ("[]".equals(json)) {// 防止出现: 没有奶茶口味，却返回"有但空"的情况
                // 返回与数据库查询为空时一致的提示
                return Result.fail("商铺信息不存在！！");
            }

            // 正常反序列化并返回
            List<ShopType> shopTypes = JSONUtil.toList(json, ShopType.class);
            return Result.ok(shopTypes);//命中了
        }

        // 2. 没有命中，查询数据库
        List<ShopType> shopTypes = query().orderByAsc("sort").list();

        // 3. 没查到，fail
        if (CollectionUtil.isEmpty(shopTypes)) {
            // 缓存空结果，防穿透
            stringredisTemplate.opsForValue().set(key, "[]", 2, TimeUnit.MINUTES);
            return Result.fail("商铺信息不存在！！");
        }

        // 4. 写入 Redis（String + TTL）
        stringredisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shopTypes), 30, TimeUnit.MINUTES);
        return Result.ok(shopTypes);

//        // 1. 从Redis查询 商铺类型缓存 , end:-1 表示取全部数据 , 和py里面的数组有点像
//        List<String> shopTypeJson = stringredisTemplate.opsForList().range(key, 0, -1);
//        return Result.ok(shopType);
        //用string写才是简单又正确的
    }
}
