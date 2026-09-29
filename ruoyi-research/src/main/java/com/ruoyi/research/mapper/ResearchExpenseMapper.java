package com.ruoyi.research.mapper;

import java.math.BigDecimal;
import java.util.List;
import com.ruoyi.research.domain.ResearchExpense;

public interface ResearchExpenseMapper {
    List<ResearchExpense> selectByProjectId(Long projectId);
    BigDecimal sumByProjectId(Long projectId);
    int insertExpense(ResearchExpense expense);
}
