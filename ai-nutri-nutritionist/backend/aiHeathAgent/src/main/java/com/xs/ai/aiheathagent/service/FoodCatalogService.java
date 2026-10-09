package com.xs.ai.aiheathagent.service;

import com.xs.ai.aiheathagent.common.BizEx;
import com.xs.ai.aiheathagent.common.ErrorCode;
import com.xs.ai.aiheathagent.dto.req.FoodPortionCalcReq;
import com.xs.ai.aiheathagent.dto.req.NutritionFoodPickReq;
import com.xs.ai.aiheathagent.dto.resp.FoodCatalogItemResp;
import com.xs.ai.aiheathagent.entity.FoodCatalog;
import com.xs.ai.aiheathagent.entity.NutritionFoodPickRecord;
import com.xs.ai.aiheathagent.entity.NutritionMealType;
import com.xs.ai.aiheathagent.repository.FoodCatalogRepository;
import com.xs.ai.aiheathagent.repository.NutritionFoodPickRecordRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class FoodCatalogService {
    private final FoodCatalogRepository foodCatalogRepository;
    private final NutritionFoodPickRecordRepository pickRecordRepository;

    public FoodCatalogService(FoodCatalogRepository foodCatalogRepository, NutritionFoodPickRecordRepository pickRecordRepository) {
        this.foodCatalogRepository = foodCatalogRepository;
        this.pickRecordRepository = pickRecordRepository;
    }

    @PostConstruct
    @Transactional
    public void seedBuiltinCatalogs() {
        List<FoodCatalog> builtins = new ArrayList<>();

        builtins.add(seed("米饭", "rice 米 白米", "lunch,dinner", 100.0, 116, 25.9, 2.6, 0.3, "https://images.unsplash.com/photo-1551218808-94e220e084d2?auto=format&fit=crop&w=300&q=80", 95));
        builtins.add(seed("面条", "noodle noodles 面", "lunch,dinner", 100.0, 138, 25.0, 4.5, 1.5, "https://images.unsplash.com/photo-1526318896980-cf78c088247c?auto=format&fit=crop&w=300&q=80", 86));
        builtins.add(seed("全麦面包", "bread 全麦 麦片", "breakfast", 60.0, 150, 26.0, 6.0, 2.0, "https://images.unsplash.com/photo-1549931319-a545dcf3bc73?auto=format&fit=crop&w=300&q=80", 85));
        builtins.add(seed("馒头", "mantou 馒头 面食", "breakfast,lunch", 80.0, 180, 37.0, 5.0, 1.0, "https://images.unsplash.com/photo-1549439602-43ebca2327af?auto=format&fit=crop&w=300&q=80", 70));
        builtins.add(seed("红薯", "sweet potato 地瓜", "breakfast,lunch,dinner", 150.0, 135, 31.0, 2.5, 0.2, "https://images.unsplash.com/photo-1604908812620-0c32d436a3d1?auto=format&fit=crop&w=300&q=80", 72));
        builtins.add(seed("土豆", "potato 土豆", "lunch,dinner", 150.0, 116, 26.0, 3.0, 0.2, "https://images.unsplash.com/photo-1518977676601-b53f82aba655?auto=format&fit=crop&w=300&q=80", 68));
        builtins.add(seed("玉米", "corn 玉米", "breakfast,lunch,dinner", 150.0, 145, 32.0, 5.0, 2.0, "https://images.unsplash.com/photo-1551754655-cd27e38d2076?auto=format&fit=crop&w=300&q=80", 65));
        builtins.add(seed("燕麦片", "oat oatmeal 燕麦", "breakfast", 50.0, 190, 33.0, 6.5, 3.5, "https://images.unsplash.com/photo-1517673400267-0251440c45dc?auto=format&fit=crop&w=300&q=80", 90));
        builtins.add(seed("粥", "congee 粥 大米", "breakfast", 300.0, 105, 23.0, 2.0, 0.3, "https://images.unsplash.com/photo-1604909053197-45c574b3c0c9?auto=format&fit=crop&w=300&q=80", 58));
        builtins.add(seed("糙米饭", "brown rice 糙米", "lunch,dinner", 100.0, 111, 23.0, 2.6, 0.9, "https://images.unsplash.com/photo-1551218808-94e220e084d2?auto=format&fit=crop&w=300&q=80", 78));
        builtins.add(seed("藜麦", "quinoa 藜麦", "lunch,dinner", 100.0, 120, 21.3, 4.4, 1.9, "https://images.unsplash.com/photo-1517673400267-0251440c45dc?auto=format&fit=crop&w=300&q=80", 62));
        builtins.add(seed("意大利面(熟)", "pasta 意面", "lunch,dinner", 150.0, 237, 46.0, 8.0, 1.3, "https://images.unsplash.com/photo-1526318896980-cf78c088247c?auto=format&fit=crop&w=300&q=80", 55));
        builtins.add(seed("荞麦面(熟)", "buckwheat soba 荞麦", "lunch,dinner", 150.0, 210, 43.0, 8.0, 1.0, "https://images.unsplash.com/photo-1526318896980-cf78c088247c?auto=format&fit=crop&w=300&q=80", 52));
        builtins.add(seed("饺子(素)", "dumpling 饺子", "lunch,dinner", 200.0, 420, 70.0, 14.0, 12.0, "https://images.unsplash.com/photo-1604908177074-4f28c1d09d7d?auto=format&fit=crop&w=300&q=80", 48));
        builtins.add(seed("饺子(猪肉)", "dumpling 饺子 猪肉", "lunch,dinner", 200.0, 520, 62.0, 20.0, 20.0, "https://images.unsplash.com/photo-1604908177074-4f28c1d09d7d?auto=format&fit=crop&w=300&q=80", 46));
        builtins.add(seed("包子(肉)", "baozi 包子", "breakfast,lunch", 200.0, 520, 72.0, 22.0, 18.0, "https://images.unsplash.com/photo-1549439602-43ebca2327af?auto=format&fit=crop&w=300&q=80", 44));
        builtins.add(seed("包子(素)", "baozi 包子 素菜", "breakfast,lunch", 200.0, 440, 78.0, 14.0, 10.0, "https://images.unsplash.com/photo-1549439602-43ebca2327af?auto=format&fit=crop&w=300&q=80", 42));
        builtins.add(seed("煎饼果子", "jianbing 煎饼", "breakfast", 250.0, 560, 66.0, 22.0, 24.0, "https://images.unsplash.com/photo-1549931319-a545dcf3bc73?auto=format&fit=crop&w=300&q=80", 40));
        builtins.add(seed("茶叶蛋", "egg 茶叶蛋", "breakfast", 50.0, 75, 0.5, 6.0, 5.0, "https://images.unsplash.com/photo-1565299624946-b28f40a0ae38?auto=format&fit=crop&w=300&q=80", 60));
        builtins.add(seed("油条", "youtiao 油条", "breakfast", 80.0, 320, 28.0, 6.0, 20.0, "https://images.unsplash.com/photo-1549931319-a545dcf3bc73?auto=format&fit=crop&w=300&q=80", 32));
        builtins.add(seed("小笼包", "xiaolongbao 小笼包", "breakfast,lunch", 200.0, 520, 60.0, 20.0, 22.0, "https://images.unsplash.com/photo-1549439602-43ebca2327af?auto=format&fit=crop&w=300&q=80", 30));
        builtins.add(seed("馄饨", "wonton 馄饨", "breakfast,lunch", 350.0, 460, 62.0, 18.0, 14.0, "https://images.unsplash.com/photo-1526318896980-cf78c088247c?auto=format&fit=crop&w=300&q=80", 28));
        builtins.add(seed("米粉(汤)", "rice noodle 米粉", "breakfast,lunch", 400.0, 420, 75.0, 12.0, 6.0, "https://images.unsplash.com/photo-1526318896980-cf78c088247c?auto=format&fit=crop&w=300&q=80", 26));
        builtins.add(seed("河粉(炒)", "ho fun 河粉", "lunch,dinner", 400.0, 620, 88.0, 16.0, 20.0, "https://images.unsplash.com/photo-1526318896980-cf78c088247c?auto=format&fit=crop&w=300&q=80", 24));
        builtins.add(seed("方便面(一包)", "instant noodle 方便面", "lunch,dinner", 120.0, 520, 60.0, 10.0, 24.0, "https://images.unsplash.com/photo-1526318896980-cf78c088247c?auto=format&fit=crop&w=300&q=80", 22));
        builtins.add(seed("炒饭", "fried rice 炒饭", "lunch,dinner", 350.0, 650, 90.0, 18.0, 18.0, "https://images.unsplash.com/photo-1551218808-94e220e084d2?auto=format&fit=crop&w=300&q=80", 21));
        builtins.add(seed("寿司(8个)", "sushi 寿司", "lunch,dinner", 250.0, 380, 55.0, 16.0, 8.0, "https://images.unsplash.com/photo-1553621042-f6e147245754?auto=format&fit=crop&w=300&q=80", 20));

        builtins.add(seed("水煮鸡蛋", "egg 鸡蛋", "breakfast", 50.0, 72, 0.4, 6.3, 4.8, "https://images.unsplash.com/photo-1565299624946-b28f40a0ae38?auto=format&fit=crop&w=300&q=80", 92));
        builtins.add(seed("鸡胸肉", "chicken breast 鸡胸", "lunch,dinner", 120.0, 198, 0.0, 37.0, 4.3, "https://images.unsplash.com/photo-1604909053197-45c574b3c0c9?auto=format&fit=crop&w=300&q=80", 94));
        builtins.add(seed("鸡腿肉", "chicken thigh 鸡腿", "lunch,dinner", 120.0, 252, 0.0, 30.0, 14.0, "https://images.unsplash.com/photo-1604909053197-45c574b3c0c9?auto=format&fit=crop&w=300&q=80", 70));
        builtins.add(seed("牛肉", "beef 牛肉 红肉", "lunch,dinner", 120.0, 250, 0.0, 26.0, 15.0, "https://images.unsplash.com/photo-1604908177074-4f28c1d09d7d?auto=format&fit=crop&w=300&q=80", 84));
        builtins.add(seed("猪里脊", "pork 猪肉 里脊", "lunch,dinner", 120.0, 215, 0.0, 28.0, 10.0, "https://images.unsplash.com/photo-1604908177074-4f28c1d09d7d?auto=format&fit=crop&w=300&q=80", 78));
        builtins.add(seed("鸡翅", "chicken wings 鸡翅", "lunch,dinner", 120.0, 290, 0.0, 23.0, 21.0, "https://images.unsplash.com/photo-1604909053197-45c574b3c0c9?auto=format&fit=crop&w=300&q=80", 52));
        builtins.add(seed("牛排", "steak 牛排", "lunch,dinner", 180.0, 430, 0.0, 45.0, 27.0, "https://images.unsplash.com/photo-1604908177074-4f28c1d09d7d?auto=format&fit=crop&w=300&q=80", 60));
        builtins.add(seed("鸡蛋白", "egg white 蛋白", "breakfast", 100.0, 52, 0.7, 11.0, 0.2, "https://images.unsplash.com/photo-1565299624946-b28f40a0ae38?auto=format&fit=crop&w=300&q=80", 55));
        builtins.add(seed("豆腐干", "tofu 豆腐干", "lunch,dinner", 100.0, 160, 6.0, 17.0, 9.0, "https://images.unsplash.com/photo-1604908177074-4f28c1d09d7d?auto=format&fit=crop&w=300&q=80", 54));
        builtins.add(seed("毛豆", "edamame 毛豆", "lunch,dinner", 150.0, 180, 13.0, 17.0, 8.0, "https://images.unsplash.com/photo-1524593119773-410f3a2d54e3?auto=format&fit=crop&w=300&q=80", 50));
        builtins.add(seed("豆皮", "tofu skin 豆皮", "lunch,dinner", 100.0, 200, 6.0, 20.0, 11.0, "https://images.unsplash.com/photo-1604908177074-4f28c1d09d7d?auto=format&fit=crop&w=300&q=80", 45));
        builtins.add(seed("鸡腿排(去皮)", "chicken 鸡腿排", "lunch,dinner", 150.0, 310, 0.0, 35.0, 18.0, "https://images.unsplash.com/photo-1604909053197-45c574b3c0c9?auto=format&fit=crop&w=300&q=80", 44));
        builtins.add(seed("牛肉丸", "beef ball 牛肉丸", "lunch,dinner", 150.0, 330, 12.0, 24.0, 20.0, "https://images.unsplash.com/photo-1543353071-087092ec393a?auto=format&fit=crop&w=300&q=80", 36));
        builtins.add(seed("虾仁", "shrimp 虾", "lunch,dinner", 120.0, 120, 1.0, 24.0, 1.5, "https://images.unsplash.com/photo-1604908554167-2ef39b4e2ff1?auto=format&fit=crop&w=300&q=80", 76));
        builtins.add(seed("鳕鱼", "cod 鳕鱼", "lunch,dinner", 150.0, 150, 0.0, 33.0, 1.0, "https://images.unsplash.com/photo-1543353071-087092ec393a?auto=format&fit=crop&w=300&q=80", 73));
        builtins.add(seed("三文鱼", "salmon 鱼", "lunch,dinner", 150.0, 312, 0.0, 30.0, 19.5, "https://images.unsplash.com/photo-1485921325833-c519f76c4927?auto=format&fit=crop&w=300&q=80", 74));
        builtins.add(seed("金枪鱼罐头(水浸)", "tuna 金枪鱼 罐头", "lunch,dinner", 100.0, 110, 0.0, 25.0, 1.0, "https://images.unsplash.com/photo-1529692236671-f1dc15e1d5a3?auto=format&fit=crop&w=300&q=80", 60));
        builtins.add(seed("豆腐", "tofu 豆腐 豆制品", "lunch,dinner", 150.0, 114, 3.0, 12.0, 6.0, "https://images.unsplash.com/photo-1604908177074-4f28c1d09d7d?auto=format&fit=crop&w=300&q=80", 72));
        builtins.add(seed("无糖豆浆", "soy milk 豆浆", "breakfast", 250.0, 90, 6.0, 8.0, 3.0, "https://images.unsplash.com/photo-1550583724-b2692b85b150?auto=format&fit=crop&w=300&q=80", 64));
        builtins.add(seed("希腊酸奶(无糖)", "yogurt 酸奶 希腊", "breakfast", 170.0, 100, 6.0, 17.0, 0.5, "https://images.unsplash.com/photo-1514996937319-344454492b37?auto=format&fit=crop&w=300&q=80", 82));
        builtins.add(seed("鸡腿便当(简)", "bento 便当", "lunch,dinner", 450.0, 720, 80.0, 35.0, 28.0, "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?auto=format&fit=crop&w=300&q=80", 18));
        builtins.add(seed("沙拉鸡胸卷", "wrap 鸡肉卷", "lunch", 300.0, 480, 45.0, 35.0, 18.0, "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?auto=format&fit=crop&w=300&q=80", 17));
        builtins.add(seed("烤红薯", "sweet potato 烤", "breakfast", 200.0, 180, 42.0, 3.5, 0.3, "https://images.unsplash.com/photo-1604908812620-0c32d436a3d1?auto=format&fit=crop&w=300&q=80", 16));
        builtins.add(seed("煮玉米", "corn 煮玉米", "breakfast", 200.0, 190, 43.0, 6.0, 2.5, "https://images.unsplash.com/photo-1551754655-cd27e38d2076?auto=format&fit=crop&w=300&q=80", 15));
        builtins.add(seed("鸡胸肉沙拉", "salad chicken 鸡胸沙拉", "lunch,dinner", 350.0, 420, 20.0, 45.0, 18.0, "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?auto=format&fit=crop&w=300&q=80", 14));
        builtins.add(seed("鸡蛋三明治", "sandwich 鸡蛋", "breakfast", 220.0, 420, 45.0, 20.0, 18.0, "https://images.unsplash.com/photo-1549931319-a545dcf3bc73?auto=format&fit=crop&w=300&q=80", 13));
        builtins.add(seed("牛肉汉堡", "burger 牛肉 汉堡", "lunch,dinner", 250.0, 550, 45.0, 28.0, 30.0, "https://images.unsplash.com/photo-1550547660-d9450f859349?auto=format&fit=crop&w=300&q=80", 12));
        builtins.add(seed("薯条", "fries 薯条", "lunch,dinner", 150.0, 470, 55.0, 5.0, 24.0, "https://images.unsplash.com/photo-1550547660-d9450f859349?auto=format&fit=crop&w=300&q=80", 11));
        builtins.add(seed("烤鸡腿", "roast chicken 烤鸡", "lunch,dinner", 200.0, 420, 0.0, 40.0, 28.0, "https://images.unsplash.com/photo-1604909053197-45c574b3c0c9?auto=format&fit=crop&w=300&q=80", 10));
        builtins.add(seed("牛肉拉面", "ramen 牛肉 拉面", "lunch,dinner", 450.0, 680, 78.0, 28.0, 26.0, "https://images.unsplash.com/photo-1526318896980-cf78c088247c?auto=format&fit=crop&w=300&q=80", 9));
        builtins.add(seed("鸡丝凉面", "noodle 鸡丝 凉面", "lunch,dinner", 450.0, 560, 70.0, 28.0, 18.0, "https://images.unsplash.com/photo-1526318896980-cf78c088247c?auto=format&fit=crop&w=300&q=80", 8));
        builtins.add(seed("牛肉炒面", "noodle 牛肉 炒面", "lunch,dinner", 450.0, 720, 85.0, 30.0, 28.0, "https://images.unsplash.com/photo-1526318896980-cf78c088247c?auto=format&fit=crop&w=300&q=80", 7));
        builtins.add(seed("咖喱鸡饭", "curry 咖喱 鸡", "lunch,dinner", 450.0, 750, 90.0, 30.0, 25.0, "https://images.unsplash.com/photo-1551218808-94e220e084d2?auto=format&fit=crop&w=300&q=80", 6));
        builtins.add(seed("麻婆豆腐", "tofu 麻婆 豆腐", "lunch,dinner", 350.0, 520, 18.0, 28.0, 36.0, "https://images.unsplash.com/photo-1604908177074-4f28c1d09d7d?auto=format&fit=crop&w=300&q=80", 5));
        builtins.add(seed("低脂酸奶", "yogurt 酸奶 低脂", "breakfast", 200.0, 120, 16.0, 10.0, 2.5, "https://images.unsplash.com/photo-1514996937319-344454492b37?auto=format&fit=crop&w=300&q=80", 66));
        builtins.add(seed("奶酪(切达)", "cheese 奶酪", "breakfast", 30.0, 120, 0.5, 7.0, 10.0, "https://images.unsplash.com/photo-1454944338482-a69bb95894af?auto=format&fit=crop&w=300&q=80", 35));
        builtins.add(seed("乳清蛋白粉", "whey protein 蛋白粉", "breakfast", 30.0, 120, 2.0, 24.0, 2.0, "https://images.unsplash.com/photo-1579722821271-0f6c0b3a1e8f?auto=format&fit=crop&w=300&q=80", 30));

        builtins.add(seed("纯牛奶", "milk 牛奶", "breakfast", 250.0, 150, 12.0, 8.0, 8.0, "https://images.unsplash.com/photo-1563636619-e9143da7973b?auto=format&fit=crop&w=300&q=80", 78));
        builtins.add(seed("无糖拿铁", "latte coffee 咖啡", "breakfast", 300.0, 90, 9.0, 6.0, 3.0, "https://images.unsplash.com/photo-1442512595331-e89e73853f31?auto=format&fit=crop&w=300&q=80", 52));

        builtins.add(seed("西兰花", "broccoli 西兰花", "lunch,dinner", 200.0, 70, 14.0, 6.0, 0.8, "https://images.unsplash.com/photo-1459411552884-841db9b3cc2a?auto=format&fit=crop&w=300&q=80", 70));
        builtins.add(seed("菠菜", "spinach 菠菜", "lunch,dinner", 200.0, 46, 7.0, 6.0, 0.8, "https://images.unsplash.com/photo-1524593119773-410f3a2d54e3?auto=format&fit=crop&w=300&q=80", 62));
        builtins.add(seed("生菜", "lettuce 生菜 沙拉", "lunch,dinner", 200.0, 30, 5.8, 2.0, 0.4, "https://images.unsplash.com/photo-1540420773420-3366772f4999?auto=format&fit=crop&w=300&q=80", 55));
        builtins.add(seed("番茄", "tomato 番茄 西红柿", "lunch,dinner", 200.0, 36, 7.8, 1.8, 0.4, "https://images.unsplash.com/photo-1546094096-0df4bcaaa337?auto=format&fit=crop&w=300&q=80", 58));
        builtins.add(seed("黄瓜", "cucumber 黄瓜", "lunch,dinner", 200.0, 32, 7.2, 1.6, 0.2, "https://images.unsplash.com/photo-1568584711271-46a3fb54a3e0?auto=format&fit=crop&w=300&q=80", 54));
        builtins.add(seed("胡萝卜", "carrot 胡萝卜", "lunch,dinner", 150.0, 60, 14.0, 1.2, 0.3, "https://images.unsplash.com/photo-1447175008436-054170c2e979?auto=format&fit=crop&w=300&q=80", 56));
        builtins.add(seed("蘑菇", "mushroom 蘑菇", "lunch,dinner", 200.0, 44, 6.6, 6.2, 0.6, "https://images.unsplash.com/photo-1546549030-974b62c7ad83?auto=format&fit=crop&w=300&q=80", 50));
        builtins.add(seed("蔬菜沙拉", "salad 蔬菜", "lunch,dinner", 200.0, 120, 10.0, 3.0, 6.0, "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?auto=format&fit=crop&w=300&q=80", 86));
        builtins.add(seed("花椰菜", "cauliflower 菜花", "lunch,dinner", 200.0, 50, 10.0, 4.0, 0.6, "https://images.unsplash.com/photo-1459411552884-841db9b3cc2a?auto=format&fit=crop&w=300&q=80", 42));
        builtins.add(seed("卷心菜", "cabbage 卷心菜", "lunch,dinner", 200.0, 50, 12.0, 3.0, 0.4, "https://images.unsplash.com/photo-1524593119773-410f3a2d54e3?auto=format&fit=crop&w=300&q=80", 40));
        builtins.add(seed("青椒", "pepper 青椒 彩椒", "lunch,dinner", 200.0, 60, 14.0, 2.0, 0.4, "https://images.unsplash.com/photo-1540420773420-3366772f4999?auto=format&fit=crop&w=300&q=80", 38));
        builtins.add(seed("洋葱", "onion 洋葱", "lunch,dinner", 150.0, 60, 14.0, 1.7, 0.2, "https://images.unsplash.com/photo-1518977676601-b53f82aba655?auto=format&fit=crop&w=300&q=80", 36));
        builtins.add(seed("南瓜", "pumpkin 南瓜", "lunch,dinner", 200.0, 80, 20.0, 2.0, 0.4, "https://images.unsplash.com/photo-1502741126161-b048400d2b6f?auto=format&fit=crop&w=300&q=80", 35));
        builtins.add(seed("茄子", "eggplant 茄子", "lunch,dinner", 200.0, 70, 16.0, 2.0, 0.4, "https://images.unsplash.com/photo-1546094096-0df4bcaaa337?auto=format&fit=crop&w=300&q=80", 34));

        builtins.add(seed("苹果", "apple 苹果 水果", "breakfast", 180.0, 95, 25.0, 0.5, 0.3, "https://images.unsplash.com/photo-1567306226416-28f0efdc88ce?auto=format&fit=crop&w=300&q=80", 68));
        builtins.add(seed("香蕉", "banana 香蕉 水果", "breakfast", 120.0, 105, 27.0, 1.3, 0.4, "https://images.unsplash.com/photo-1571771894821-ce9b6c11b08e?auto=format&fit=crop&w=300&q=80", 72));
        builtins.add(seed("橙子", "orange 橙子 水果", "breakfast", 200.0, 86, 21.0, 1.7, 0.2, "https://images.unsplash.com/photo-1547514701-42782101795e?auto=format&fit=crop&w=300&q=80", 60));
        builtins.add(seed("草莓", "strawberry 草莓", "breakfast", 200.0, 64, 15.0, 1.4, 0.6, "https://images.unsplash.com/photo-1464965911861-746a04b4bca6?auto=format&fit=crop&w=300&q=80", 62));
        builtins.add(seed("葡萄", "grape 葡萄", "breakfast", 200.0, 134, 36.0, 1.4, 0.3, "https://images.unsplash.com/photo-1515778767554-42d4b373f2b3?auto=format&fit=crop&w=300&q=80", 55));
        builtins.add(seed("梨", "pear 梨 水果", "breakfast", 200.0, 102, 27.0, 0.7, 0.2, "https://images.unsplash.com/photo-1567306226416-28f0efdc88ce?auto=format&fit=crop&w=300&q=80", 54));
        builtins.add(seed("桃子", "peach 桃子 水果", "breakfast", 200.0, 78, 19.0, 1.9, 0.4, "https://images.unsplash.com/photo-1502741126161-b048400d2b6f?auto=format&fit=crop&w=300&q=80", 53));
        builtins.add(seed("芒果", "mango 芒果 水果", "breakfast", 200.0, 120, 30.0, 1.6, 0.8, "https://images.unsplash.com/photo-1547514701-42782101795e?auto=format&fit=crop&w=300&q=80", 52));
        builtins.add(seed("蓝莓", "blueberry 蓝莓", "breakfast", 150.0, 85, 21.0, 1.1, 0.5, "https://images.unsplash.com/photo-1464965911861-746a04b4bca6?auto=format&fit=crop&w=300&q=80", 51));
        builtins.add(seed("猕猴桃", "kiwi 猕猴桃", "breakfast", 150.0, 92, 22.0, 1.7, 0.8, "https://images.unsplash.com/photo-1515778767554-42d4b373f2b3?auto=format&fit=crop&w=300&q=80", 50));
        builtins.add(seed("牛油果", "avocado 牛油果", "breakfast,lunch", 100.0, 160, 8.5, 2.0, 14.7, "https://images.unsplash.com/photo-1523049673857-eb18f1d7b578?auto=format&fit=crop&w=300&q=80", 58));


        builtins.add(seed("坚果(混合)", "nuts 坚果", "breakfast", 30.0, 180, 6.0, 5.0, 16.0, "https://images.unsplash.com/photo-1505253216365-6d42b065d9f0?auto=format&fit=crop&w=300&q=80", 50));
        builtins.add(seed("花生酱", "peanut butter 花生酱", "breakfast", 30.0, 180, 6.0, 7.0, 15.0, "https://images.unsplash.com/photo-1524593246832-39b3b330f3cf?auto=format&fit=crop&w=300&q=80", 45));
        builtins.add(seed("黑巧克力", "dark chocolate 巧克力", "breakfast", 20.0, 120, 9.0, 2.0, 8.0, "https://images.unsplash.com/photo-1549007994-cb92caebd54b?auto=format&fit=crop&w=300&q=80", 40));

        builtins.add(seed("豆腐脑(原味)", "doufunao 豆腐脑", "breakfast", 300.0, 160, 10.0, 14.0, 6.0, "https://images.unsplash.com/photo-1524593119773-410f3a2d54e3?auto=format&fit=crop&w=300&q=80", 44));
        builtins.add(seed("鸡蛋羹", "steamed egg 蛋羹", "breakfast", 150.0, 120, 2.0, 12.0, 7.0, "https://images.unsplash.com/photo-1565299624946-b28f40a0ae38?auto=format&fit=crop&w=300&q=80", 48));

        builtins.add(seed("白粥+咸菜(简)", "congee 腌菜", "breakfast", 350.0, 160, 34.0, 4.0, 1.0, "https://images.unsplash.com/photo-1604909053197-45c574b3c0c9?auto=format&fit=crop&w=300&q=80", 35));

        for (FoodCatalog f : builtins) {
            String name = String.valueOf(f.getName() == null ? "" : f.getName()).trim();
            if (name.isEmpty()) continue;
            boolean exists = foodCatalogRepository.findTopByNameIgnoreCase(name).isPresent();
            if (exists) continue;
            foodCatalogRepository.save(f);
        }
    }

    private FoodCatalog seed(String name, String keywords, String tags, double baseAmount, int calorie, double carb, double protein, double fat, String img, int sortOrder) {
        FoodCatalog f = new FoodCatalog();
        f.setName(name);
        f.setKeywords(String.valueOf(keywords == null ? "" : keywords));
        f.setMealTags(String.valueOf(tags == null ? "" : tags));
        f.setBaseUnit("g");
        f.setBaseAmount(Math.max(1.0, baseAmount));
        f.setBaseCalorie(Math.max(0, calorie));
        f.setBaseCarb(Math.max(0.0, carb));
        f.setBaseProtein(Math.max(0.0, protein));
        f.setBaseFat(Math.max(0.0, fat));
        f.setBaseFiber(0.0);
        f.setImgUrl(String.valueOf(img == null ? "" : img));
        f.setSortOrder(sortOrder);
        f.setIsActive(1);
        f.setCreatedAt(LocalDateTime.now());
        f.setUpdatedAt(LocalDateTime.now());
        return f;
    }

    @Transactional(readOnly = true)
    public List<FoodCatalogItemResp> recommend(String mealType, int limit) {
        int size = Math.min(Math.max(1, limit), 30);
        String tag = mealTag(mealType);
        List<FoodCatalog> list;
        if (!tag.isEmpty()) {
            list = foodCatalogRepository.recommendByMealTag(tag, PageRequest.of(0, size));
        } else {
            list = foodCatalogRepository.listActive(PageRequest.of(0, size));
        }
        return list.stream().map((f) -> toResp(f, 1.0, defaultUnitLabel(1.0))).toList();
    }

    @Transactional(readOnly = true)
    public List<FoodCatalogItemResp> search(String q, int limit) {
        String query = String.valueOf(q == null ? "" : q).trim();
        if (query.isEmpty()) return List.of();
        int size = Math.min(Math.max(1, limit), 50);
        List<FoodCatalog> list = foodCatalogRepository.search(query, PageRequest.of(0, size));
        return list.stream().map((f) -> toResp(f, 1.0, defaultUnitLabel(1.0))).toList();
    }

    @Transactional(readOnly = true)
    public FoodCatalogItemResp calcPortion(FoodPortionCalcReq req) {
        Long foodId = req == null ? null : req.getFoodId();
        if (foodId == null || foodId <= 0) throw new BizEx(ErrorCode.PARAM_INVALID, "foodId required");
        FoodCatalog f = foodCatalogRepository.findById(foodId).orElseThrow(() -> new BizEx(ErrorCode.PARAM_INVALID, "foodId 不存在"));

        double multiplier = req == null || req.getMultiplier() == null || Double.isNaN(req.getMultiplier()) ? 1.0 : req.getMultiplier();
        multiplier = clampMultiplier(multiplier);
        String unitLabel = String.valueOf(req == null ? "" : req.getUnitLabel()).trim();
        if (unitLabel.isEmpty()) unitLabel = defaultUnitLabel(multiplier);
        return toResp(f, multiplier, unitLabel);
    }

    @Transactional
    public void recordPick(Long userId, NutritionFoodPickReq req) {
        if (userId == null || userId <= 0) return;
        NutritionFoodPickReq r = req == null ? new NutritionFoodPickReq() : req;

        String name = String.valueOf(r.getName() == null ? "" : r.getName()).trim();
        if (name.isEmpty()) return;

        NutritionFoodPickRecord rec = new NutritionFoodPickRecord();
        rec.setUserId(userId);
        if (r.getMemberId() != null && r.getMemberId() > 0) rec.setMemberId(r.getMemberId());
        rec.setRecordDate(parseDateOrNull(r.getDate()));
        rec.setMealType(parseMealType(r.getMealType()));

        rec.setFoodName(name);
        rec.setUnit(String.valueOf(r.getUnit() == null ? "" : r.getUnit()).trim());
        rec.setNutrientText(String.valueOf(r.getNutrient() == null ? "" : r.getNutrient()).trim());
        rec.setImageUrl(String.valueOf(r.getImg() == null ? "" : r.getImg()).trim());
        rec.setNote(String.valueOf(r.getNote() == null ? "" : r.getNote()).trim());

        rec.setCalorie(r.getCalorie() == null ? 0 : Math.max(0, r.getCalorie()));
        rec.setCarb(safeMacro(r.getCarb()));
        rec.setProtein(safeMacro(r.getProtein()));
        rec.setFat(safeMacro(r.getFat()));
        rec.setFiber(safeMacro(r.getFiber()));

        rec.setFoodCatalogId(r.getFoodCatalogId() == null ? 0L : Math.max(0L, r.getFoodCatalogId()));
        double pm = r.getPortionMultiplier() == null || Double.isNaN(r.getPortionMultiplier()) ? 1.0 : r.getPortionMultiplier();
        rec.setPortionMultiplier(clampMultiplier(pm));
        rec.setCreatedAt(LocalDateTime.now());
        pickRecordRepository.save(rec);
    }

    private double safeMacro(Double v) {
        if (v == null || Double.isNaN(v)) return 0.0;
        return Math.max(0.0, v);
    }

    private NutritionMealType parseMealType(String mealType) {
        String s = String.valueOf(mealType == null ? "" : mealType).trim();
        if (s.isEmpty()) return null;
        try {
            return NutritionMealType.valueOf(s.toUpperCase(Locale.ROOT));
        } catch (Exception ignored) {
            return null;
        }
    }

    private LocalDate parseDateOrNull(String date) {
        String s = String.valueOf(date == null ? "" : date).trim();
        if (s.isEmpty()) return null;
        try {
            return LocalDate.parse(s);
        } catch (Exception ignored) {
            return null;
        }
    }

    private double clampMultiplier(double v) {
        if (v < 0.1) return 0.1;
        if (v > 20.0) return 20.0;
        return v;
    }

    private String mealTag(String mealType) {
        NutritionMealType t = parseMealType(mealType);
        if (t == NutritionMealType.BREAKFAST) return "breakfast";
        if (t == NutritionMealType.LUNCH) return "lunch";
        if (t == NutritionMealType.DINNER) return "dinner";
        return "";
    }

    private FoodCatalogItemResp toResp(FoodCatalog f, double multiplier, String unitLabel) {
        double ratio = multiplier;
        int calorie = roundInt(BigDecimal.valueOf(f.getBaseCalorie() == null ? 0 : Math.max(0, f.getBaseCalorie())).multiply(BigDecimal.valueOf(ratio)));
        double carb = round1(BigDecimal.valueOf(f.getBaseCarb() == null ? 0.0 : Math.max(0.0, f.getBaseCarb())).multiply(BigDecimal.valueOf(ratio)));
        double protein = round1(BigDecimal.valueOf(f.getBaseProtein() == null ? 0.0 : Math.max(0.0, f.getBaseProtein())).multiply(BigDecimal.valueOf(ratio)));
        double fat = round1(BigDecimal.valueOf(f.getBaseFat() == null ? 0.0 : Math.max(0.0, f.getBaseFat())).multiply(BigDecimal.valueOf(ratio)));
        double fiber = round1(BigDecimal.valueOf(f.getBaseFiber() == null ? 0.0 : Math.max(0.0, f.getBaseFiber())).multiply(BigDecimal.valueOf(ratio)));
        String nutrient = buildNutrient(carb, protein, fat);
        return new FoodCatalogItemResp(
                f.getId(),
                String.valueOf(f.getName() == null ? "" : f.getName()),
                String.valueOf(unitLabel == null ? "" : unitLabel),
                nutrient,
                calorie,
                carb,
                protein,
                fat,
                fiber,
                String.valueOf(f.getImgUrl() == null ? "" : f.getImgUrl()),
                "",
                String.valueOf(f.getBaseUnit() == null ? "" : f.getBaseUnit()),
                f.getBaseAmount() == null ? 0.0 : f.getBaseAmount(),
                f.getBaseFiber() == null ? 0.0 : f.getBaseFiber(),
                multiplier
        );
    }

    private String defaultUnitLabel(double multiplier) {
        String m = stripTrailingZero(multiplier);
        if ("1".equals(m)) return "1份";
        return m + "份";
    }

    private String stripTrailingZero(double v) {
        BigDecimal bd = BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros();
        return bd.toPlainString();
    }

    private String buildNutrient(double carb, double protein, double fat) {
        double max = Math.max(carb, Math.max(protein, fat));
        if (max <= 0) return "0克碳水";
        if (max == protein) return stripTrailingZero(protein) + "克蛋白";
        if (max == fat) return stripTrailingZero(fat) + "克脂肪";
        return stripTrailingZero(carb) + "克碳水";
    }

    private double round1(BigDecimal v) {
        return v.setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    private int roundInt(BigDecimal v) {
        return v.setScale(0, RoundingMode.HALF_UP).intValue();
    }
}
