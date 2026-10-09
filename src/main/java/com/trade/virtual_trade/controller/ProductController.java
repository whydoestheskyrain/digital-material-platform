package com.trade.virtual_trade.controller;

import com.trade.virtual_trade.common.Result;
import com.trade.virtual_trade.entity.Product;
import com.trade.virtual_trade.dto.ProductPublicResponse;
import com.trade.virtual_trade.service.ProductService;
import com.trade.virtual_trade.security.CurrentUser;
import com.trade.virtual_trade.security.RequireAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/product")
public class ProductController {

    // 1. 把 @Autowired 放到最上面（必须改）
    @Autowired
    private ProductService productService;

    // 管理员审核商品
    @PutMapping("/audit/{id}/{status}")
    @RequireAdmin
    public Result audit(
            @PathVariable Integer id,
            @PathVariable Integer status
    ) {
        productService.audit(id, status);
        return Result.success();
    }

    // 根据状态查询商品
    @GetMapping("/listByStatus/{status}")
    public Result listByStatus(@PathVariable Integer status, HttpServletRequest request) {
        // 前台只能浏览已上架商品；待审核、已拒绝、已下架列表仅管理员可见。
        if (status != 1 && !CurrentUser.isAdmin(request)) {
            return Result.error("需要管理员权限");
        }
        if (status == 1) {
            return Result.success(toPublicList(productService.listByStatus(status)));
        }
        return Result.success(productService.listByStatus(status));
    }

    // 查询我的商品
    @GetMapping("/myList/{sellerId}")
    public Result myList(@PathVariable Integer sellerId, HttpServletRequest request) {
        if (!isOwnerOrAdmin(sellerId, request)) {
            return Result.error("无权查看其他用户的商品管理列表");
        }
        return Result.success(productService.myList(sellerId));
    }

    // 全部列表
    @GetMapping("/list")
    public Result<List<ProductPublicResponse>> list() {
        return Result.success(toPublicList(productService.listByStatus(1)));
    }

    // 分页
    @GetMapping("/page")
    @RequireAdmin
    public Result<List<Product>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(productService.findPage(page, size));
    }

    // 根据ID查询
    @GetMapping("/get/{id}")
    public Result<Product> get(@PathVariable Integer id, HttpServletRequest request) {
        Product product = productService.findById(id);
        if (product == null) {
            return Result.error("商品不存在");
        }
        if (!isOwnerOrAdmin(product.getSellerId(), request)) {
            return Result.error("无权查看此商品的敏感资料");
        }
        return Result.success(product);
    }

    // 商品详情查询（公开接口）
    @GetMapping("/detail/{id}")
    public Result<ProductPublicResponse> detail(@PathVariable Integer id) {
        return Result.success(ProductPublicResponse.from(productService.findById(id)));
    }

    // 新增
    @PostMapping("/add")
    public Result add(@RequestBody Product product) {
        // 1. 如果前端没有传图片，设置默认图片
        // 后端 ProductController 里
        if (product.getImageUrl() == null || product.getImageUrl().trim().isEmpty()) {
            // 相对路径：开发环境由前端 devServer 提供，生产环境由 Nginx 提供
            product.setImageUrl("/images/game-default.png");
        }
        // 2. 设置初始状态（待审核）
        product.setStatus(0);

        // 3. 设置创建时间（使用 LocalDateTime.now()）
        product.setCreateTime(LocalDateTime.now());

        // 4. 保存商品
        productService.add(product);

        // 5. 返回成功
        return Result.success();
    }

    // 修改
    @PostMapping("/update")
    public Result update(@RequestBody Product product, HttpServletRequest request) {
        Product existing = productService.findById(product.getId());
        if (existing == null) {
            return Result.error("商品不存在");
        }
        if (!isOwnerOrAdmin(existing.getSellerId(), request)) {
            return Result.error("无权修改此商品");
        }
        productService.update(product);
        return Result.success();
    }

    // 2. 删除必须用 @DeleteMapping（规范且安全）
    @DeleteMapping("/delete/{id}")
    public Result delete(@PathVariable Integer id, HttpServletRequest request) {
        Product existing = productService.findById(id);
        if (existing == null) {
            return Result.error("商品不存在");
        }
        if (!isOwnerOrAdmin(existing.getSellerId(), request)) {
            return Result.error("无权删除此商品");
        }
        productService.delete(id);
        return Result.success();
    }

    private boolean isOwnerOrAdmin(Integer ownerId, HttpServletRequest request) {
        return ownerId != null && (ownerId.equals(CurrentUser.getUserId(request)) || CurrentUser.isAdmin(request));
    }

    private List<ProductPublicResponse> toPublicList(List<Product> products) {
        return products.stream().map(ProductPublicResponse::from).collect(Collectors.toList());
    }
}
