package com.trade.virtual_trade.service;

import com.trade.virtual_trade.entity.GameType;
import java.util.List;

public interface GameTypeService {
    List<GameType> getAll();

    // 仅启用的分类（前台展示用）
    List<GameType> getEnabled();

    void add(GameType gameType);

    void update(GameType gameType);

    void delete(Integer id);

    GameType getById(Integer id);
}