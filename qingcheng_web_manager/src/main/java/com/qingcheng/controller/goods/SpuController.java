package com.qingcheng.controller.goods;

import com.alibaba.dubbo.config.annotation.Reference;
import com.qingcheng.entity.PageResult;
import com.qingcheng.entity.Result;
import com.qingcheng.pojo.goods.Goods;
import com.qingcheng.pojo.goods.Spu;
import com.qingcheng.service.goods.SpuService;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/spu")
public class SpuController {

    @Reference
    private SpuService spuService;

    @GetMapping("/findAll")
    public List<Spu> findAll() {
        return spuService.findAll();
    }

    @GetMapping("/findPage")
    public PageResult<Spu> findPage(int page, int size) {
        return spuService.findPage(page, size);
    }

    @PostMapping("/findList")
    public List<Spu> findList(@RequestBody Map<String, Object> searchMap) {
        return spuService.findList(searchMap);
    }

    @PostMapping("/findPage")
    public PageResult<Spu> findPage(@RequestBody Map<String, Object> searchMap, int page, int size) {
        return spuService.findPage(searchMap, page, size);
    }

    @GetMapping("/findById")
    public Spu findById(String id) {
        return spuService.findById(id);
    }


    @PostMapping("/add")
    public Result add(@RequestBody Spu spu) {
        spuService.add(spu);
        return new Result();
    }

    @PostMapping("/update")
    public Result update(@RequestBody Spu spu) {
        spuService.update(spu);
        return new Result();
    }

    @GetMapping("/delete")
    public Result delete(String id) {
        spuService.delete(id);
        return new Result();
    }

    @PostMapping("/save")
    public Result save(@RequestBody Goods goods) {
        spuService.saveGoods(goods);
        return new Result();
    }

    @GetMapping("/findGoodsById")
    public Goods findGoodsById(String id) {
        if ("null".equals(id) || "".equals(id)) {
            throw new RuntimeException("输入id");
        }
        return spuService.findGoodsById(id);
    }

    @PostMapping("/audit")
    public Result audit(String id, String status, String message) {
        if ("".equals(id) || "null".equals(id)) {
            throw new RuntimeException("未接收到ID");
        }
        spuService.audit(id, status, message);
        return new Result();
    }

    @GetMapping("/pull")
    public Result pull(String id) {
        if ("".equals(id) || "null".equals(id)) {
            throw new RuntimeException("未接收到ID");
        }
        spuService.pull(id);
        return new Result();
    }

    @GetMapping("/put")
    public Result put(String id) {
        if ("".equals(id) || "null".equals(id)) {
            throw new RuntimeException("未接收到ID");
        }
        spuService.put(id);
        return new Result();
    }

    @PostMapping("/putMany")
    public Result putMany(Long[] ids) {
        if (ids.length == 0) {
            throw new RuntimeException("未接收到ID");
        }
        spuService.putMany(ids);
        return new Result();
    }

    @PostMapping("/pullMany")
    public Result pullMany(Long[] ids) {
        if (ids.length == 0) {
            throw new RuntimeException("未接收到ID");
        }
        spuService.pullMany(ids);
        return new Result();
    }

    @GetMapping("/dele")
    public Result dele(String id) {
        if ("".equals(id) || "null".equals(id)) {
            throw new RuntimeException("未接收到ID");
        }
        spuService.dele(id);
        return new Result();
    }

    @GetMapping("/notDele")
    public Result notDele(String id) {
        if ("".equals(id) || "null".equals(id)) {
            throw new RuntimeException("未接收到ID");
        }
        spuService.notdele(id);
        return new Result();
    }
}
