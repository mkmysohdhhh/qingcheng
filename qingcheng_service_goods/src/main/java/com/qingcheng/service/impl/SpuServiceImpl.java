package com.qingcheng.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.qingcheng.dao.CategoryBrandMapper;
import com.qingcheng.dao.CategoryMapper;
import com.qingcheng.dao.SkuMapper;
import com.qingcheng.dao.SpuMapper;
import com.qingcheng.entity.PageResult;
import com.qingcheng.pojo.goods.*;
import com.qingcheng.service.goods.SpuService;
import com.qingcheng.util.IdWorker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;

import java.util.*;

@Service(interfaceClass = SpuService.class)
public class SpuServiceImpl implements SpuService {

    @Autowired
    private SpuMapper spuMapper;

    @Autowired
    private SkuMapper skuMapper;

    @Autowired
    private IdWorker idWorker;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private CategoryBrandMapper categoryBrandMapper;

    /**
     * 返回全部记录
     *
     * @return
     */
    public List<Spu> findAll() {
        return spuMapper.selectAll();
    }

    /**
     * 分页查询
     *
     * @param page 页码
     * @param size 每页记录数
     * @return 分页结果
     */
    public PageResult<Spu> findPage(int page, int size) {
        PageHelper.startPage(page, size);
        Page<Spu> spus = (Page<Spu>) spuMapper.selectAll();
        return new PageResult<Spu>(spus.getTotal(), spus.getResult());
    }

    /**
     * 条件查询
     *
     * @param searchMap 查询条件
     * @return
     */
    public List<Spu> findList(Map<String, Object> searchMap) {
        Example example = createExample(searchMap);
        return spuMapper.selectByExample(example);
    }

    /**
     * 分页+条件查询
     *
     * @param searchMap
     * @param page
     * @param size
     * @return
     */
    public PageResult<Spu> findPage(Map<String, Object> searchMap, int page, int size) {
        PageHelper.startPage(page, size);
        Example example = createExample(searchMap);
        Page<Spu> spus = (Page<Spu>) spuMapper.selectByExample(example);
        return new PageResult<Spu>(spus.getTotal(), spus.getResult());
    }

    /**
     * 根据Id查询
     *
     * @param id
     * @return
     */
    public Spu findById(String id) {
        return spuMapper.selectByPrimaryKey(id);
    }

    /**
     * 新增
     *
     * @param spu
     */
    public void add(Spu spu) {
        spuMapper.insert(spu);
    }

    /**
     * 修改
     *
     * @param spu
     */
    public void update(Spu spu) {
        spuMapper.updateByPrimaryKeySelective(spu);
    }

    /**
     * 删除
     *
     * @param id
     */
    public void delete(String id) {
        spuMapper.deleteByPrimaryKey(id);
    }

    /**
     * 新增商品
     *
     * @param goods
     */
    @Transactional
    @Override
    public void saveGoods(Goods goods) {
        Spu spu = goods.getSpu();
        //判断是修改还是新增
        if (spu.getId() == null) {
            spu.setId(idWorker.nextId() + "");
            spuMapper.insert(spu);
        } else {
            Example example = new Example(Spu.class);
            Example.Criteria criteria = example.createCriteria();
            criteria.andEqualTo("spuId", spu.getId());
            spuMapper.deleteByExample(example);
            spuMapper.updateByPrimaryKeySelective(spu);
        }

        CategoryBrand categoryBrand = new CategoryBrand();
        categoryBrand.setBrandId(spu.getBrandId());
        categoryBrand.setCategoryId(spu.getCategory3Id());
        int count = categoryBrandMapper.selectCount(categoryBrand);
        if (count == 0) {
            categoryBrandMapper.insert(categoryBrand);
        }

        Date date = new Date();
        Category category = categoryMapper.selectByPrimaryKey(spu.getCategory3Id());
        List<Sku> skuList = goods.getSkuList();
        for (Sku sku : skuList) {
            if (sku.getSpec() == null || "".equals(sku.getSpec())) {
                sku.setSpec("{}");
            }
            if (sku.getId() == null) {
                sku.setId(idWorker.nextId() + "");
                sku.setSpuId(spu.getId());

                //拼接sku名
                StringBuilder sb_spuName = new StringBuilder(spu.getName());
                Map<String, String> specMap = JSON.parseObject(sku.getSpec(), Map.class);
                for (String value : specMap.values()) {
                    sb_spuName.append(" ").append(new StringBuilder(value));
                }

                //设置字段参数
                sku.setName(sb_spuName.toString());
                sku.setCreateTime(date);
                sku.setUpdateTime(date);//更新时间
                sku.setCategoryId(spu.getCategory3Id());
                sku.setCategoryName(category.getName());
                sku.setCommentNum(0);
                sku.setSaleNum(0);

                skuMapper.insert(sku);
            } else {
                //删除原有sku
                skuMapper.deleteByPrimaryKey(sku.getId());

                sku.setId(idWorker.nextId() + "");
                sku.setSpuId(spu.getId());

                //拼接sku名
                StringBuilder sb_spuName = new StringBuilder(spu.getName());
                Map<String, String> specMap = JSON.parseObject(sku.getSpec(), Map.class);
                for (String value : specMap.values()) {
                    sb_spuName.append(" ").append(new StringBuilder(value));
                }

                //设置字段参数
                sku.setName(sb_spuName.toString());
                sku.setCreateTime(date);
                sku.setUpdateTime(new Date());//更新时间
                sku.setCategoryId(spu.getCategory3Id());
                sku.setCategoryName(category.getName());
                sku.setCommentNum(0);
                sku.setSaleNum(0);

                skuMapper.insert(sku);
            }
        }

    }

    /**
     * 根据ID查询商品
     *
     * @param id
     * @return
     */
    @Override
    public Goods findGoodsById(String id) {
        Spu spu = spuMapper.selectByPrimaryKey(id);

        Example example = new Example(Sku.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("spuId", id);
        List<Sku> skuList = skuMapper.selectByExample(example);
        Goods goods = new Goods();
        goods.setSpu(spu);
        goods.setSkuList(skuList);
        return goods;
    }

    /**
     * 商品审核
     *
     * @param id
     * @param status
     * @param message
     */
    @Transactional
    @Override
    public void audit(String id, String status, String message) {
        Spu spu = new Spu();
        spu.setId(id);
        spu.setStatus(status);
        if ("1".equals(status)) {
            spu.setIsMarketable("1");
        }
        spuMapper.updateByPrimaryKeySelective(spu);

        //TODO 记录审核记录

        //TODO 记录商品日志
    }

    /**
     * 下架商品
     *
     * @param id
     */
    @Override
    public void pull(String id) {
        Spu spu = spuMapper.selectByPrimaryKey(id);
        spu.setIsMarketable("0");
        spuMapper.updateByPrimaryKeySelective(spu);
    }

    /**
     * 上架商品
     *
     * @param id
     */
    @Override
    public void put(String id) {
        Spu spu;
        try {
            spu = spuMapper.selectByPrimaryKey(id);
        } catch (RuntimeException e) {
            throw new RuntimeException("无效的ID");
        }
        if (!"1".equals(spu.getStatus())) {
            throw new RuntimeException("该商品未通过审核");
        }
        spu.setIsMarketable("1");
        spuMapper.updateByPrimaryKeySelective(spu);

        //TODO 记录上架日志
    }

    /**
     * 批量上架
     *
     * @param ids
     */
    @Override
    public void putMany(Long[] ids) {
        Spu spu = new Spu();
        spu.setIsMarketable("1");
        Example example = new Example(Spu.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("isMarketable", 0);
        criteria.andEqualTo("status", 1);
        criteria.andEqualTo("isDelete", "0");
        criteria.andIn("id", Arrays.asList(ids));
        spuMapper.updateByExampleSelective(spu, example);
    }

    /**
     * 批量下架
     *
     * @param ids
     */
    @Override
    public void pullMany(Long[] ids) {
        Spu spu = new Spu();
        spu.setIsMarketable("0");
        Example example = new Example(Spu.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("isMarketable", 1);
        criteria.andEqualTo("status", 1);
        criteria.andEqualTo("isDelete", "0");
        criteria.andIn("id", Arrays.asList(ids));
        spuMapper.updateByExampleSelective(spu, example);
    }

    /**
     * 删除商品
     *
     * @param id
     */
    @Transactional
    @Override
    public void dele(String id) {
        Spu spu = null;
        try {
            spu = spuMapper.selectByPrimaryKey(id);
        } catch (Exception e) {
            throw new RuntimeException("无效的ID");
        }
        spu.setIsDelete("1");
        spuMapper.updateByPrimaryKeySelective(spu);

        Example example = new Example(Sku.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("spuId", id);
        List<Sku> skuList = skuMapper.selectByExample(example);
        if (skuList.size() != 0) {
            for (Sku sku : skuList) {
                sku.setStatus("3");
                skuMapper.updateByPrimaryKeySelective(sku);
            }
        }

    }

    /**
     * 还原商品
     * @param id
     */
    @Transactional
    @Override
    public void notdele(String id) {
        Spu spu = null;
        try {
            spu = spuMapper.selectByPrimaryKey(id);
        } catch (Exception e) {
            throw new RuntimeException("无效的ID");
        }
        spu.setIsDelete("0");
        spuMapper.updateByPrimaryKeySelective(spu);

        Example example = new Example(Sku.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("spuId", id);
        List<Sku> skuList = skuMapper.selectByExample(example);
        if (skuList.size() != 0) {
            for (Sku sku : skuList) {
                sku.setStatus("1");
                skuMapper.updateByPrimaryKeySelective(sku);
            }
        }
    }

    /**
     * 构建查询条件
     *
     * @param searchMap
     * @return
     */
    private Example createExample(Map<String, Object> searchMap) {
        Example example = new Example(Spu.class);
        Example.Criteria criteria = example.createCriteria();
        if (searchMap != null) {
            // 主键
            if (searchMap.get("id") != null && !"".equals(searchMap.get("id"))) {
                criteria.andLike("id", "%" + searchMap.get("id") + "%");
            }
            // 货号
            if (searchMap.get("sn") != null && !"".equals(searchMap.get("sn"))) {
                criteria.andLike("sn", "%" + searchMap.get("sn") + "%");
            }
            // SPU名
            if (searchMap.get("name") != null && !"".equals(searchMap.get("name"))) {
                criteria.andLike("name", "%" + searchMap.get("name") + "%");
            }
            // 副标题
            if (searchMap.get("caption") != null && !"".equals(searchMap.get("caption"))) {
                criteria.andLike("caption", "%" + searchMap.get("caption") + "%");
            }
            // 图片
            if (searchMap.get("image") != null && !"".equals(searchMap.get("image"))) {
                criteria.andLike("image", "%" + searchMap.get("image") + "%");
            }
            // 图片列表
            if (searchMap.get("images") != null && !"".equals(searchMap.get("images"))) {
                criteria.andLike("images", "%" + searchMap.get("images") + "%");
            }
            // 售后服务
            if (searchMap.get("saleService") != null && !"".equals(searchMap.get("saleService"))) {
                criteria.andLike("saleService", "%" + searchMap.get("saleService") + "%");
            }
            // 介绍
            if (searchMap.get("introduction") != null && !"".equals(searchMap.get("introduction"))) {
                criteria.andLike("introduction", "%" + searchMap.get("introduction") + "%");
            }
            // 规格列表
            if (searchMap.get("specItems") != null && !"".equals(searchMap.get("specItems"))) {
                criteria.andLike("specItems", "%" + searchMap.get("specItems") + "%");
            }
            // 参数列表
            if (searchMap.get("paraItems") != null && !"".equals(searchMap.get("paraItems"))) {
                criteria.andLike("paraItems", "%" + searchMap.get("paraItems") + "%");
            }
            // 是否上架
            if (searchMap.get("isMarketable") != null && !"".equals(searchMap.get("isMarketable"))) {
                criteria.andLike("isMarketable", "%" + searchMap.get("isMarketable") + "%");
            }
            // 是否启用规格
            if (searchMap.get("isEnableSpec") != null && !"".equals(searchMap.get("isEnableSpec"))) {
                criteria.andLike("isEnableSpec", "%" + searchMap.get("isEnableSpec") + "%");
            }
            // 是否删除
            if (searchMap.get("isDelete") != null && !"".equals(searchMap.get("isDelete"))) {
                criteria.andLike("isDelete", "%" + searchMap.get("isDelete") + "%");
            }
            // 审核状态
            if (searchMap.get("status") != null && !"".equals(searchMap.get("status"))) {
                criteria.andLike("status", "%" + searchMap.get("status") + "%");
            }

            // 品牌ID
            if (searchMap.get("brandId") != null) {
                criteria.andEqualTo("brandId", searchMap.get("brandId"));
            }
            // 一级分类
            if (searchMap.get("category1Id") != null) {
                criteria.andEqualTo("category1Id", searchMap.get("category1Id"));
            }
            // 二级分类
            if (searchMap.get("category2Id") != null) {
                criteria.andEqualTo("category2Id", searchMap.get("category2Id"));
            }
            // 三级分类
            if (searchMap.get("category3Id") != null) {
                criteria.andEqualTo("category3Id", searchMap.get("category3Id"));
            }
            // 模板ID
            if (searchMap.get("templateId") != null) {
                criteria.andEqualTo("templateId", searchMap.get("templateId"));
            }
            // 运费模板id
            if (searchMap.get("freightId") != null) {
                criteria.andEqualTo("freightId", searchMap.get("freightId"));
            }
            // 销量
            if (searchMap.get("saleNum") != null) {
                criteria.andEqualTo("saleNum", searchMap.get("saleNum"));
            }
            // 评论数
            if (searchMap.get("commentNum") != null) {
                criteria.andEqualTo("commentNum", searchMap.get("commentNum"));
            }

        }
        return example;
    }

}
