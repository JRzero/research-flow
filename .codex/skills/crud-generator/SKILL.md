---
name: crud-generator
description: Generate single-table CRUD code for this RuoYi/Spring Boot project from a database table structure, including backend domain, mapper, service, controller, MyBatis XML, frontend API, and Vue page files.
---

# 单表 CRUD 代码生成器

根据数据库表结构，快速生成前后端 CRUD 代码文件。

## 输入参数

用户提供以下信息：
- `tableName`: 数据库表名（如 `t_cn_code`）
- `functionName`: 功能名称（如 `CN代码`）
- `moduleName`: 模块名称（如 `cbam`）
- `businessName`: 业务名称（如 `cnCode`）

## 生成文件列表

| 文件类型 | 文件路径 | 说明 |
|---------|---------|------|
| Domain | `ruoyi-system/src/main/java/com/ruoyi/{moduleName}/domain/Biz{ClassName}.java` | 实体类 |
| Mapper | `ruoyi-system/src/main/java/com/ruoyi/{moduleName}/mapper/Biz{ClassName}Mapper.java` | Mapper接口 |
| Service | `ruoyi-system/src/main/java/com/ruoyi/{moduleName}/service/IBiz{ClassName}Service.java` | Service接口 |
| ServiceImpl | `ruoyi-system/src/main/java/com/ruoyi/{moduleName}/service/impl/Biz{ClassName}ServiceImpl.java` | Service实现 |
| Controller | `ruoyi-admin/src/main/java/com/ruoyi/web/controller/{moduleName}/Biz{ClassName}Controller.java` | Controller |
| Mapper.xml | `ruoyi-system/src/main/resources/mapper/{moduleName}/Biz{ClassName}Mapper.xml` | MyBatis映射 |
| api.js | `ruoyi-ui/src/api/{moduleName}/{businessName}.js` | 管理后台API |
| index.vue | `ruoyi-ui/src/views/{moduleName}/{businessName}/index.vue` | 管理后台页面 |

## 命名规则

- `tableName`: `t_cn_code` → `ClassName`: `CnCode` → 文件名: `BizCnCode`
- `tableName`: `t_order` → `ClassName`: `Order` → 文件名: `BizOrder`
- 去掉表前缀 `t_`，转换为驼峰命名，**后端文件统一加 `Biz` 前缀**
- 示例：
  - 表名 `t_cn_default_value` → Domain: `BizCnDefaultValue.java`
  - 表名 `t_cn_default_value` → Mapper: `BizCnDefaultValueMapper.java`
  - 表名 `t_cn_default_value` → Service: `IBizCnDefaultValueService.java`
  - 表名 `t_cn_default_value` → ServiceImpl: `BizCnDefaultValueServiceImpl.java`
  - 表名 `t_cn_default_value` → Controller: `BizCnDefaultValueController.java`

## 代码生成规则

### 1. Controller 引用规则
- **使用 Jakarta 命名空间**（Spring Boot 3.x）：
```java
import jakarta.servlet.http.HttpServletResponse;  // 正确
// 不是 javax.servlet.http.HttpServletResponse  // 错误
```

### 2. 路由命名规则
- **路由路径统一使用小写**：
```java
@RequestMapping("/cbam/cncode")  // 正确
// 不是 @RequestMapping("/cbam/cnCode")  // 错误
```

### 3. Mapper.xml 新增接口规则
- **不赋值字段**: `del_flag`, `update_by`, `update_time`（数据库有默认值）
- **需要赋值字段**: `create_by`, `create_time`

```xml
<insert id="insert{ClassName}">
    insert into {tableName}
    <trim prefix="(" suffix=")" suffixOverrides=",">
        <!-- 业务字段 -->
        <if test="field != null">field,</if>
        <!-- 不包含 del_flag, update_by, update_time -->
        <if test="createBy != null and createBy != ''">create_by,</if>
        <if test="createTime != null">create_time,</if>
    </trim>
    <trim prefix="values (" suffix=")" suffixOverrides=",">
        <if test="field != null">#{field},</if>
        <if test="createBy != null and createBy != ''">#{createBy},</if>
        <if test="createTime != null">#{createTime},</if>
    </trim>
</insert>
```

### 4. Mapper.xml 更新接口规则
- **不赋值字段**: `del_flag`, `create_by`, `create_time`, `update_time`
- **需要赋值字段**: `update_by`

```xml
<update id="update{ClassName}">
    update {tableName}
    <trim prefix="SET" suffixOverrides=",">
        <!-- 业务字段 -->
        <if test="field != null">field = #{field},</if>
        <!-- 不包含 del_flag, create_by, create_time, update_time -->
        <if test="updateBy != null and updateBy != ''">update_by = #{updateBy},</if>
    </trim>
    where id = #{id}
</update>
```

### 5. ServiceImpl 新增方法
```java
@Override
public int insert{ClassName}({ClassName} {className}) {
    {className}.setCreateTime(DateUtils.getNowDate());
    // createBy 由Controller设置
    return {className}Mapper.insert{ClassName}({className});
}
```

### 6. ServiceImpl 更新方法
```java
@Override
public int update{ClassName}({ClassName} {className}) {
    {className}.setUpdateTime(DateUtils.getNowDate());
    // updateBy 由Controller设置
    return {className}Mapper.update{ClassName}({className});
}
```

### 7. Controller 新增方法
```java
@PostMapping
public AjaxResult add(@RequestBody {ClassName} {className}) {
    {className}.setCreateBy(getUsername());
    return toAjax({className}Service.insert{ClassName}({className}));
}
```

### 8. Controller 更新方法
```java
@PutMapping
public AjaxResult edit(@RequestBody {ClassName} {className}) {
    {className}.setUpdateBy(getUsername());
    return toAjax({className}Service.update{ClassName}({className}));
}
```

### 9. 前端 API 路由规则
- **API 路径使用小写**：
```javascript
url: '/cbam/cncode/list'  // 正确
// 不是 url: '/cbam/cnCode/list'  // 错误
```

## 执行步骤

1. **获取表结构**: 查询数据库表结构和字段信息
2. **生成Domain**: 根据表字段生成实体类
3. **生成Mapper**: 生成Mapper接口和XML映射文件
4. **生成Service**: 生成Service接口和实现类
5. **生成Controller**: 生成RESTful接口
6. **生成前端API**: 生成管理后台API文件
7. **生成前端页面**: 生成管理后台列表页面

## 使用示例

```
用户: 生成 t_cn_default_value 表的CRUD代码，功能名称是默认值，模块是cbam
```

## 注意事项

1. 所有新建表必须指定 `DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci`
2. 表必须有 `id` 主键字段
3. 建议包含标准审计字段：`create_by`, `create_time`, `update_by`, `update_time`, `del_flag`, `remark`
4. 删除操作使用逻辑删除（设置 `del_flag = '1'`）
5. Controller 放在 `ruoyi-admin/src/main/java/com/ruoyi/web/controller/{moduleName}/` 目录下

