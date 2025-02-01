package generator.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import generator.domain.Stocks;
import generator.service.StocksService;
import generator.mapper.StocksMapper;
import org.springframework.stereotype.Service;

/**
* @author paysys
* @description 针对表【stocks(库存表)】的数据库操作Service实现
* @createDate 2025-01-02 12:10:26
*/
@Service
public class StocksServiceImpl extends ServiceImpl<StocksMapper, Stocks>
    implements StocksService{

}




