package com.trade.virtual_trade.dto;

import com.trade.virtual_trade.entity.Product;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 前台商品展示字段。游戏凭据和绑定资料绝不能出现在此对象中。 */
@Data
public class ProductPublicResponse {
    private Integer id;
    private Integer sellerId;
    private Integer gameTypeId;
    private String gameType;
    private String gameTypeName;
    private String title;
    private String gameServer;
    private Integer roleLevel;
    private BigDecimal price;
    private String description;
    private String detailDesc;
    private String detailImages;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String imageUrl;

    public static ProductPublicResponse from(Product product) {
        if (product == null) return null;
        ProductPublicResponse response = new ProductPublicResponse();
        response.setId(product.getId());
        response.setSellerId(product.getSellerId());
        response.setGameTypeId(product.getGameTypeId());
        response.setGameType(product.getGameType());
        response.setGameTypeName(product.getGameTypeName());
        response.setTitle(product.getTitle());
        response.setGameServer(product.getGameServer());
        response.setRoleLevel(product.getRoleLevel());
        response.setPrice(product.getPrice());
        response.setDescription(product.getDescription());
        response.setDetailDesc(product.getDetailDesc());
        response.setDetailImages(product.getDetailImages());
        response.setStatus(product.getStatus());
        response.setCreateTime(product.getCreateTime());
        response.setUpdateTime(product.getUpdateTime());
        response.setImageUrl(product.getImageUrl());
        return response;
    }
}
