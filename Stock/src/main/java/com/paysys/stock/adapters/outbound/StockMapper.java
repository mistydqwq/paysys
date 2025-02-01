package com.paysys.stock.adapters.outbound;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.paysys.stock.domain.valueobj.StockVO;
import org.apache.ibatis.annotations.Mapper;


/**
 * @author Mitsui515
 * @description 针对表【stocks(库存表)】的数据库操作Mapper
 * @createDate 2025-01-02 12:10:26
 * @Entity com.paysys.stock.domain.valueobj.StockVO
 */
@Mapper
public interface StockMapper extends BaseMapper<StockVO> {

}




