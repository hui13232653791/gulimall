package com.atguigu.gulimall.product.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.gulimall.product.service.CategoryBrandRelationService;
import com.atguigu.gulimall.product.vo.Catelog2Vo;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.product.dao.CategoryDao;
import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.CategoryService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

    @Autowired
    private CategoryBrandRelationService categoryBrandRelationService;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private RedissonClient redisson;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 查出所有分类以及子分类，以树形结构组装起来
     *
     * @return
     */
    @Override
    public List<CategoryEntity> listWithTree() {
        //1、查出所有分类
        List<CategoryEntity> entities = baseMapper.selectList(null);
        //2、组装成父子的树形结构

        //2.1、找到所以的一级分类
        List<CategoryEntity> level1Menus = entities.stream().filter(categoryEntity ->
                categoryEntity.getParentCid() == 0
        ).map((menu) -> {
            menu.setChildren(getChildrens(menu, entities));
            return menu;
        }).sorted((menu1, menu2) -> {
            return (menu1.getSort() == null ? 0 : menu1.getSort()) - (menu2.getSort() == null ? 0 : menu2.getSort());
        }).collect(Collectors.toList());

        return level1Menus;
    }

    /**
     * 递归查找当前菜单的所有子菜单
     *
     * @param root 当前菜单
     * @param all  所有菜单
     * @return 当前菜单的子菜单
     */
    private List<CategoryEntity> getChildrens(CategoryEntity root, List<CategoryEntity> all) {
        List<CategoryEntity> children = all.stream().filter(categoryEntity -> {
            return categoryEntity.getParentCid() == root.getCatId();
        }).map(categoryEntity -> {
            //1、递归找到子菜单
            categoryEntity.setChildren(getChildrens(categoryEntity, all));
            return categoryEntity;
        }).sorted((menu1, menu2) -> {
            //2、菜单的排序
            return (menu1.getSort() == null ? 0 : menu1.getSort()) - (menu2.getSort() == null ? 0 : menu2.getSort());
        }).collect(Collectors.toList());
        return children;
    }

    /**
     * 删除菜单分类，检查当前删除的菜单，是否被别的地方引用
     *
     * @param asList
     */
    @Override
    public void removeMenuByIds(List<Long> asList) {
        //TODO 1、删除菜单分类，检查当前删除的菜单，是否被别的地方引用
        baseMapper.deleteBatchIds(asList);

    }

    /**
     * 找到catelogId完整所属分类路径：[父/子/孙]->[2/25/225]
     *
     * @param catelogId
     * @return
     */
    @Override
    public Long[] findCatelogPath(Long catelogId) {
        ArrayList<Long> paths = new ArrayList<>();
        List<Long> parentPath = findParentPath(catelogId, paths);

        Collections.reverse(parentPath);
        return (Long[]) parentPath.toArray(new Long[parentPath.size()]);
    }

    /**
     * 级联更新所有关联的数据
     *
     * @CacheEvict：缓存失效模式
     * @Caching：同时进行多种缓存操作
     *
     *  @CacheEvict:失效模式
     *  @CachePut:双写模式，需要有返回值
     *     1、同时进行多种缓存操作：@Caching
     *     2、指定删除某个分区下的所有数据 @CacheEvict(value = "category",allEntries = true)
     *     3、存储同一类型的数据，都可以指定为同一分区
     */
//    @Caching(evict = {
//            @CacheEvict(value = {"category"}, key = "'getLevel1Catagories'"),
//            @CacheEvict(value = {"category"}, key = "'getCatelogJson'")
//    })
    @CacheEvict(value = {"category"}, allEntries = true)
    @Transactional
    @Override
    public void updateCascade(CategoryEntity category) {
        this.updateById(category);
        categoryBrandRelationService.updateCategory(category.getCatId(), category.getName());

    }


    /**
     * 每一个需要缓存的数据我们都来指定要放到那个名字的缓存。【缓存的分区(按照业务类型分)】
     * 代表当前方法的结果需要缓存，如果缓存中有，方法都不用调用，如果缓存中没有，会调用方法。最后将方法的结果放入缓存
     * 默认行为
     * 如果缓存中有，方法不再调用
     * key是默认生成的:缓存的名字::SimpleKey::[](自动生成key值)
     * 缓存的value值，默认使用jdk序列化机制，将序列化的数据存到redis中
     * 默认时间是 -1：
     * <p>
     * 自定义操作：key的生成
     * 指定生成缓存的key：key属性指定，接收一个Spel
     * 指定缓存的数据的存活时间:配置文档中修改存活时间
     * 将数据保存为json格式
     * <p>
     * 4、Spring-Cache的不足之处：
     * 1）、读模式
     * 缓存穿透：查询一个null数据。解决方案：缓存空数据
     * 缓存击穿：大量并发进来同时查询一个正好过期的数据。解决方案：加锁 ? 默认是无加锁的;使用sync = true来解决击穿问题
     * 缓存雪崩：大量的key同时过期。解决：加随机时间。加上过期时间
     * 2)、写模式：（缓存与数据库一致）
     * 1）、读写加锁。
     * 2）、引入Canal,感知到MySQL的更新去更新Redis
     * 3）、读多写多，直接去数据库查询就行
     * <p>
     * 总结：
     * 常规数据（读多写少，即时性，一致性要求不高的数据，完全可以使用Spring-Cache）：写模式(只要缓存的数据有过期时间就足够了)
     * 特殊数据：特殊设计
     * <p>
     * 原理：
     * CacheManager(RedisCacheManager)->Cache(RedisCache)->Cache负责缓存的读写
     *
     * @return 取所有的1级分类
     */
    @Cacheable(value = {"category"}, key = "#root.method.name",sync = true)
    //代表该方法的结果需要缓存，如果缓存中有，方法不调用。如果缓存中没有，则会调用方法，最后将方法的结果放入缓存
    @Override
    public List<CategoryEntity> getLevel1Catagories() {
        List<CategoryEntity> categoryEntities = baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", 0));

        return categoryEntities;
    }

    @Cacheable(value = {"category"}, key = "#root.method.name")
    @Override
    public Map<String, List<Catelog2Vo>> getCatelogJson() {
        System.out.println("。。查询了数据库。。");

        /**
         * 将数据库的多次查询变为一次
         */
        List<CategoryEntity> selectList = this.baseMapper.selectList(null);

        // 查询所有1级分类
        List<CategoryEntity> level1 = getCategoryByParentCid(selectList, 0L);
        //封装数据
        Map<String, List<Catelog2Vo>> parent_cid = level1.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
            // 拿到每一个1级分类 然后查询他们的2级分类
            //List<CategoryEntity> entities = baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", v.getCatId()));
            List<CategoryEntity> entities = getCategoryByParentCid(selectList, v.getCatId());
            List<Catelog2Vo> catelog2Vos = null;
            if (entities != null) {
                catelog2Vos = entities.stream().map(l2 -> {
                    Catelog2Vo catelog2Vo = new Catelog2Vo(v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName());
                    // 找当前二级分类的3级分类
                    //List<CategoryEntity> level3 = baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", l2.getCatId()));
                    List<CategoryEntity> level3 = getCategoryByParentCid(selectList, l2.getCatId());
                    // 三级分类有数据的情况下
                    if (level3 != null) {
                        List<Catelog2Vo.Catelog3Vo> catalog3Vos = level3.stream().map(l3 -> {
                            Catelog2Vo.Catelog3Vo catelog3Vo = new Catelog2Vo.Catelog3Vo(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());
                            return catelog3Vo;
                        }).collect(Collectors.toList());
                        catelog2Vo.setCatelog3List(catalog3Vos);
                    }
                    return catelog2Vo;
                }).collect(Collectors.toList());
            }
            return catelog2Vos;
        }));
        return parent_cid;
    }

    //TODO lettuce堆外内存溢出bug。
    // 1、当进行压力测试时后期后出现堆外内存溢出OutOfDirectMemoryError
    // 2、产生原因：lettuce和jedis是操作redis的底层客户端，RedisTemplate是再次封装
    //  1)、springboot2.0以后默认使用lettuce作为操作redis的客户端，它使用netty进行网络通信
    //  2)、lettuce的bug导致netty堆外内存溢出。netty如果没有指定堆外内存，默认使用Xms的值，可以使用-Dio.netty.maxDirectMemory进行设置
    // 3、解决方案：由于是lettuce的bug造成，不要直接使用-Dio.netty.maxDirectMemory去调大虚拟机堆外内存，治标不治本。
    //  1)、升级lettuce客户端。但是没有解决的
    //  2)、切换使用jedis

    /**
     * 渲染三级分类菜单：缓存
     *
     * @return
     */
//    @Override
    public Map<String, List<Catelog2Vo>> getCatelogJson2() {
        /**
         * 1、解决缓存穿透：空结果返回
         * 2、解决缓存雪崩：设置过期时间（加随机值）
         * 3、解决缓存击穿：加锁
         */
        //1、加入缓存逻辑，缓存中存的数据是json字符串
        //JSON好处：跨语言，跨平台兼容
        String catalogJSON = redisTemplate.opsForValue().get("catalogJSON");
        if (StringUtils.isEmpty(catalogJSON)) {
            //2、缓存中没有，查询数据库
            System.out.println("缓存不命中，查询数据库。。");
            Map<String, List<Catelog2Vo>> catelogJsonFromDb = getCatalogJsonDbWithRedisLock();

            return catelogJsonFromDb;
        }
        System.out.println("缓存命中，直接返回。。");
        //给缓存中放json字符串，拿出的json字符串；还能逆转为能用的对象类型；【序列化和反序列化】
        Map<String, List<Catelog2Vo>> result = JSON.parseObject(catalogJSON, new TypeReference<Map<String, List<Catelog2Vo>>>() {
        });
        return result;
    }


    /**
     * 渲染三级分类菜单：redisson分布式锁
     * <p>
     * 缓存里的数据如何和数据库的数据保持一致？？
     * 缓存数据一致性
     * 1)、双写模式
     * 2)、失效模式
     *
     * @return
     */
    public Map<String, List<Catelog2Vo>> getCatalogJsonDbWithRedissonLock() {
        //1、占分布式锁。去redis占坑
        //（锁的粒度，越细越快:具体缓存的是某个数据，11号商品） product-11-lock
        RLock lock = redisson.getLock("catalogJson-lock");
        lock.lock();

        Map<String, List<Catelog2Vo>> dataFromDb = null;
        try {
            dataFromDb = getDataFromDb();

        } finally {
            lock.unlock();
        }
        return dataFromDb;
    }


    /**
     * 渲染三级分类菜单：redis分布式锁
     *
     * @return
     */
    public Map<String, List<Catelog2Vo>> getCatalogJsonDbWithRedisLock() {
        String uuid = UUID.randomUUID().toString();
        //占分布式锁，设置过期时间（原子性）
        Boolean lock = redisTemplate.opsForValue().setIfAbsent("lock", uuid, 300, TimeUnit.SECONDS);
        if (lock) {
            Map<String, List<Catelog2Vo>> dataFromDb = null;
            System.out.println("获取分布式锁成功。。");
            try {
                dataFromDb = getDataFromDb();
            } finally {
                // get值和delete锁（原子操作）
                String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
                //删除锁
                Long execute = redisTemplate.execute(new DefaultRedisScript<Long>(script, Long.class), Arrays.asList("lock"), uuid);
            }
            return dataFromDb;
        } else {
            System.out.println("获取分布式锁失败。。等待重试");
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // 睡眠0.1s后，重新调用 //自旋
            return getCatalogJsonDbWithRedisLock();
        }
    }

    private Map<String, List<Catelog2Vo>> getDataFromDb() {
        //得到锁以后，我们应该再去缓存中确定一次，如果没有才需要继续查询
        String catalogJson = redisTemplate.opsForValue().get("catalogJSON");
        if (!StringUtils.isEmpty(catalogJson)) {
            //缓存不为空直接返回
            Map<String, List<Catelog2Vo>> result = JSON.parseObject(catalogJson, new TypeReference<Map<String, List<Catelog2Vo>>>() {
            });
            return result;
        }
        System.out.println("。。查询了数据库。。");

        /**
         * 将数据库的多次查询变为一次
         */
        List<CategoryEntity> selectList = this.baseMapper.selectList(null);

        // 查询所有1级分类
        List<CategoryEntity> level1 = getCategoryByParentCid(selectList, 0L);
        //封装数据
        Map<String, List<Catelog2Vo>> parent_cid = level1.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
            // 拿到每一个1级分类 然后查询他们的2级分类
            //List<CategoryEntity> entities = baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", v.getCatId()));
            List<CategoryEntity> entities = getCategoryByParentCid(selectList, v.getCatId());
            List<Catelog2Vo> catelog2Vos = null;
            if (entities != null) {
                catelog2Vos = entities.stream().map(l2 -> {
                    Catelog2Vo catelog2Vo = new Catelog2Vo(v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName());
                    // 找当前二级分类的3级分类
                    //List<CategoryEntity> level3 = baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", l2.getCatId()));
                    List<CategoryEntity> level3 = getCategoryByParentCid(selectList, l2.getCatId());
                    // 三级分类有数据的情况下
                    if (level3 != null) {
                        List<Catelog2Vo.Catelog3Vo> catalog3Vos = level3.stream().map(l3 -> {
                            Catelog2Vo.Catelog3Vo catelog3Vo = new Catelog2Vo.Catelog3Vo(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());
                            return catelog3Vo;
                        }).collect(Collectors.toList());
                        catelog2Vo.setCatelog3List(catalog3Vos);
                    }
                    return catelog2Vo;
                }).collect(Collectors.toList());
            }
            return catelog2Vos;
        }));
        //3、查到的数据在放入缓存,将对象转为json放在缓存中
        String s = JSON.toJSONString(parent_cid);
        redisTemplate.opsForValue().set("catalogJSON", s);

        return parent_cid;
    }

    /**
     * 渲染三级分类菜单：从数据库查询并封装数据
     *
     * @return
     */
    public Map<String, List<Catelog2Vo>> getCatelogJsonFromDb() {
        /**
         * 1、优化业务逻辑，仅查询一次数据库
         */
        List<CategoryEntity> selectList = baseMapper.selectList(null);

        // 查询所有1级分类
        List<CategoryEntity> level1 = getCategoryByParentCid(selectList, 0L);
        //封装数据
        Map<String, List<Catelog2Vo>> parent_cid = level1.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
            // 拿到每一个1级分类 然后查询他们的2级分类
            //List<CategoryEntity> entities = baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", v.getCatId()));
            List<CategoryEntity> entities = getCategoryByParentCid(selectList, v.getCatId());
            List<Catelog2Vo> catelog2Vos = null;
            if (entities != null) {
                catelog2Vos = entities.stream().map(l2 -> {
                    Catelog2Vo catelog2Vo = new Catelog2Vo(v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName());
                    // 找当前二级分类的3级分类
                    //List<CategoryEntity> level3 = baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", l2.getCatId()));
                    List<CategoryEntity> level3 = getCategoryByParentCid(selectList, l2.getCatId());
                    // 三级分类有数据的情况下
                    if (level3 != null) {
                        List<Catelog2Vo.Catelog3Vo> catalog3Vos = level3.stream().map(l3 -> {
                            Catelog2Vo.Catelog3Vo catelog3Vo = new Catelog2Vo.Catelog3Vo(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());
                            return catelog3Vo;
                        }).collect(Collectors.toList());
                        catelog2Vo.setCatelog3List(catalog3Vos);
                    }
                    return catelog2Vo;
                }).collect(Collectors.toList());
            }
            return catelog2Vos;
        }));
        return parent_cid;
    }

    /**
     * 渲染三级分类菜单
     *
     * @param selectList 分类集合
     * @param parent_id  父id
     * @return 该父id的子分类
     */
    private List<CategoryEntity> getCategoryByParentCid(List<CategoryEntity> selectList, Long parent_id) {
        List<CategoryEntity> collect = selectList.stream().filter(item -> item.getParentCid() == parent_id).collect(Collectors.toList());

        return collect;
    }


    /**
     * 递归收集所有父节点->[225/25/2]
     *
     * @param catelogId
     * @param paths
     * @return
     */
    private List<Long> findParentPath(Long catelogId, List<Long> paths) {
        //1、收集当前节点id
        paths.add(catelogId);
        CategoryEntity byId = this.getById(catelogId);
        if (byId.getParentCid() != 0) {
            findParentPath(byId.getParentCid(), paths);
        }
        return paths;
    }

}