package com.paysys.stock;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.paysys.stock.adapters.inbound.grpc.StockServiceApi;
import com.paysys.stock.adapters.outbound.StockMapper;
import com.paysys.stock.common.BaseResponse;
import com.paysys.stock.domain.entities.Stock;
import com.paysys.stock.domain.valueobj.OrderItem;
import com.paysys.stock.domain.valueobj.StockVO;
import com.paysys.stock.ports.outbound.StockRepositoryPort;
import org.apache.dubbo.config.annotation.DubboReference;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {"dubbo.protocol.port=-1"}) // 使用随机端口
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class StockServiceApiTests {

    @DubboReference(version = "1.0.0", check = false)
    private StockServiceApi stockServiceApi;

    @Autowired
    private StockRepositoryPort stockRepositoryPort;

    @Autowired
    private StockMapper stockMapper;

    @BeforeEach
    public void setup() {
        cleanup();

        // 准备测试数据
        StockVO stockVO = new StockVO();
        stockVO.setProductId("14");
        stockVO.setProductName("Test Product");
        stockVO.setQuantity(10L);
        stockVO.setReservedQuantity(0L);
        stockVO.setPrice(new BigDecimal("100.00"));

        // 直接使用 Mapper 插入数据
        int result = stockMapper.insert(stockVO);
        assertTrue(result > 0, "Stock should be inserted successfully");
    }

    @AfterEach
    public void cleanup() {
        // 使用原生SQL进行物理删除
        stockMapper.delete(new QueryWrapper<StockVO>()
                .eq("product_id", "14")
                .last("and is_deleted=1"));  // 强制物理删除

        // 确保数据被删除
        StockVO stock = stockMapper.selectOne(
                new QueryWrapper<StockVO>()
                        .eq("product_id", "14")
                        .last("and is_deleted=1")); // 包括已删除的数据也要查
        assertNull(stock, "Stock should be physically deleted");
    }

    @Test
    @Order(2)
    public void testReserveStock() {
        // 准备测试数据
        String orderId = "test-order-1";
        List<OrderItem> items = new ArrayList<>();
        OrderItem item = new OrderItem(
                "14",           // productId
                "Test Product", // productName
                1L,            // quantity
                new BigDecimal("100.00") // price
        );
        items.add(item);

        // 执行预留并验证返回值
        BaseResponse<Boolean> response = stockServiceApi.reserveStock(orderId, items);
        assertNotNull(response, "Response should not be null");
        assertNotNull(response.getData(), "Response data should not be null");
        assertTrue(response.getData(), "Stock reservation should succeed");
        assertEquals(0, response.getCode(), "Response code should be success (0)");

        // 验证库存状态
        Stock stock = stockRepositoryPort.findById("14").orElseThrow();
        assertEquals(1L, stock.getReservedQuantity(), "Reserved quantity should be 1");
        assertEquals(9L, stock.getAvailableQuantity(), "Available quantity should be 9");
    }

//    @Test
//    @Order(2)
//    public void testReserveStockWithInsufficientQuantity() {
//        // 准备测试数据
//        String orderId = "test-order-2";
//        List<OrderItem> items = new ArrayList<>();
//        items.add(createOrderItem("14", 100L)); // 大于库存数量
//
//        // 执行预留
//        BaseResponse<Boolean> response = stockServiceApi.reserveStock(orderId, items);
//
//        // 验证结果
//        assertNotNull(response, "Response should not be null");
//        assertNotNull(response.getData(), "Response data should not be null");
//        assertFalse(response.getData(), "Stock reservation should fail");
//
//        // 验证库存状态未变
//        Stock stock = stockRepositoryPort.findById("14").orElseThrow();
//        assertEquals(0L, stock.getReservedQuantity(), "Reserved quantity should remain 0");
//        assertEquals(10L, stock.getAvailableQuantity(), "Available quantity should remain 10");
//    }

    @Test
    @Order(1)
    public void testReleaseStock() {
        // 先预留库存
        testReserveStock();

        // 准备释放数据
        String orderId = "test-order-1";
        List<OrderItem> items = new ArrayList<>();
        items.add(createOrderItem("14", 1L));

        // 执行释放
        Boolean result = stockServiceApi.releaseStock(orderId, items);

        // 验证结果
        assertTrue(result, "Stock release should succeed");

        // 验证库存状态
        Stock stock = stockRepositoryPort.findById("14").orElseThrow();
        assertEquals(0L, stock.getReservedQuantity(), "Reserved quantity should be 0");
        assertEquals(10L, stock.getAvailableQuantity(), "Available quantity should be 10");
    }

    private OrderItem createOrderItem(String productId, Long quantity) {
        return new OrderItem(
                productId,
                "Test Product",
                quantity,
                new BigDecimal("100.00")
        );
    }
}