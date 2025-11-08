package com.yzx.chatdemo.service.imple;

import com.yzx.chatdemo.entity.Product;
import com.yzx.chatdemo.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @className: DataInitializer
 * @author: yzx
 * @date: 2025/9/22 19:46
 * @Version: 1.0
 * @description:
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
    private final ProductService productService;
    private final Random random = new Random();

    @Override
    public void run(String... args) throws Exception {
        log.info("开始初始化商品测试数据");

        // 如果已经存在数据，则不再初始化
        if (productService.count() > 0) {
            log.info("已存在商品数据，跳过初始化");
            return;
        }

        List<Product> products = new ArrayList<>();

        // 生成100个测试商品
        for (long i = 1; i <= 100; i++) {
            Product product = new Product();
            product.setId(i);
            product.setName(generateProductName(i));
            product.setCode("PROD" + String.format("%06d", i));
            product.setDescription(generateDescription(i));
            product.setPrice(new BigDecimal(50 + random.nextInt(950))); // 50-1000元
            product.setCategoryId((long) (1 + random.nextInt(5))); // 1-5分类
            product.setCategoryName(generateCategoryName(product.getCategoryId()));
            product.setTags(generateTags(i));
            product.setSales(random.nextInt(10000)); // 0-10000销量
            product.setScore(3 + random.nextFloat() * 2); // 3-5分
            product.setStock(random.nextInt(1000)); // 0-1000库存
            product.setIsOnSale(random.nextBoolean()); // 随机上架状态
            product.setCreateTime(LocalDateTime.now().minusDays(random.nextInt(365)));
            product.setUpdateTime(product.getCreateTime().plusDays(random.nextInt(30)));

            products.add(product);
        }

        // 批量保存
        productService.saveAll(products);
    }

    /**
     * 生成商品名称
     */
    private String generateProductName(long id) {
        String[] prefixes = {"高级", "智能", "新款", "经典", "豪华", "迷你", "便携", "专业"};
        String[] mainNames = {"手机", "电脑", "手表", "耳机", "音箱", "相机", "平板", "电视"};
        String[] suffixes = {"Pro", "Max", "Mini", "Plus", "Ultra", "", "", ""};

        return prefixes[random.nextInt(prefixes.length)] +
                mainNames[random.nextInt(mainNames.length)] +
                suffixes[random.nextInt(suffixes.length)] +
                (id % 10 == 0 ? " " + id : "");
    }

    /**
     * 生成商品描述
     */
    private String generateDescription(long id) {
        String[] descriptions = {
                "高性能产品，适合各种场景使用",
                "全新设计，时尚美观，功能强大",
                "性价比极高，用户评价良好",
                "专业级配置，满足高端需求",
                "轻便易携，随时随地使用",
                "长效续航，无需频繁充电",
                "高清显示，色彩还原真实",
                "快速响应，操作流畅"
        };

        return descriptions[random.nextInt(descriptions.length)] +
                "，商品ID: " + id + "，欢迎选购！";
    }

    /**
     * 生成分类名称
     */
    private String generateCategoryName(Long categoryId) {
        switch (categoryId.intValue()) {
            case 1: return "智能手机";
            case 2: return "笔记本电脑";
            case 3: return "智能穿戴";
            case 4: return "音频设备";
            case 5: return "家用电器";
            default: return "其他分类";
        }
    }

    /**
     * 生成商品标签
     */
    private List<String> generateTags(long id) {
        String[] allTags = {"新品", "热销", "促销", "限量", "爆款", "推荐", "优惠", "品质", "正品", "耐用"};
        List<String> tags = new ArrayList<>();

        // 随机选择2-4个标签
        int tagCount = 2 + random.nextInt(3);
        for (int i = 0; i < tagCount; i++) {
            int index = random.nextInt(allTags.length);
            if (!tags.contains(allTags[index])) {
                tags.add(allTags[index]);
            }
        }

        return tags;
    }


}
