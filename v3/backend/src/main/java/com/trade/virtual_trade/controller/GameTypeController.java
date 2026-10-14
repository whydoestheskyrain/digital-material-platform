package com.trade.virtual_trade.controller;

import com.trade.virtual_trade.common.Result;
import com.trade.virtual_trade.entity.GameType;
import com.trade.virtual_trade.service.GameTypeService;
import com.trade.virtual_trade.security.RequireAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/gameType")
public class GameTypeController {

    @Autowired
    private GameTypeService gameTypeService;

    @GetMapping("/list")
    @RequireAdmin
    public Result list() {
        // 管理端：返回全部分类（含禁用）
        List<GameType> list = gameTypeService.getAll();
        return Result.success(list);
    }

    @GetMapping("/enabled")
    public Result enabled() {
        // 前台：只返回启用的分类
        return Result.success(gameTypeService.getEnabled());
    }

    @PostMapping("/add")
    @RequireAdmin
    public Result add(@RequestBody GameType gameType) {
        // 👇 加固：防止 type_name 为空
        if (gameType.getTypeName() == null || gameType.getTypeName().trim().isEmpty()) {
            return Result.error("游戏类型名称不能为空");
        }
        gameTypeService.add(gameType);
        return Result.success();
    }

    @PutMapping("/update")
    @RequireAdmin
    public Result update(@RequestBody GameType gameType) {
        // 👇 加固：防止 type_name 为空
        if (gameType.getTypeName() == null || gameType.getTypeName().trim().isEmpty()) {
            return Result.error("游戏类型名称不能为空");
        }
        gameTypeService.update(gameType);
        return Result.success();
    }

    @DeleteMapping("/delete/{id}")
    @RequireAdmin
    public Result delete(@PathVariable Integer id) {
        gameTypeService.delete(id);
        return Result.success();
    }

    @GetMapping("/detail/{id}")
    public Result detail(@PathVariable Integer id) {
        return Result.success(gameTypeService.getById(id));
    }
}
