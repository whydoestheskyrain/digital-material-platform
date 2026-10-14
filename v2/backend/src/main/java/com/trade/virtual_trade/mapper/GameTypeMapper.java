package com.trade.virtual_trade.mapper;
import com.trade.virtual_trade.entity.GameType;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface GameTypeMapper  {

    // 查询所有游戏类型（管理端用，含禁用，按排序号升序）
    List<GameType> findAll();

    // 查询所有启用的游戏类型（前台用）
    List<GameType> findAllEnabled();

    // 新增
    int add(GameType gameType);

    // 修改
    int update(GameType gameType);

    // 删除
    int delete(Integer id);

    // 根据ID查询
    GameType findById(Integer id);
}
