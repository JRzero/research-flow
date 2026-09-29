# 003 Risk / Issue / Change

## Risk
score = probability * impact；等级由规则确定。Risk OCCURRED 可生成 Issue。

## Change
DRAFT -> SUBMITTED -> APPROVED/REJECTED -> APPLIED。
APPLY 在单事务中更新当前计划、预算或周期，生成新 Baseline，并更新 current_baseline_id。
