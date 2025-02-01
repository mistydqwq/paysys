package com.paysys.stock.adapters.outbound;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.paysys.stock.domain.valueobj.StockTransactionVO;
import org.apache.ibatis.annotations.Mapper;

/**
* @author Mitsui515
* @description 针对表【stock_transactions(库存交易记录表)】的数据库操作Mapper
* @createDate 2025-01-05 13:49:22
* @Entity com.paysys.stock.domain.valueobj.StockTransactionVO
*/
@Mapper
public interface StockTransactionMapper extends BaseMapper<StockTransactionVO> {

}




