# 📘 AI饮食识别系统 API 文档（v1）

## 🔹 基础说明

* 请求格式：JSON / multipart/form-data
* 返回统一格式：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

---

# 一、Recognition 模块（识别）

---

## 1. 多图识别

* **POST** `/recognition/recognize`

* 功能：多图上传识别食物营养数据，并入库，生成一个 session

### 请求参数（multipart/form-data）：

* files file[] 必填：图片文件，支持多张
* mealType string 可选：BREAKFAST / LUNCH / DINNER / SNACK / CUSTOM
* prompt string 可选：自定义提示词

---

### 返回格式：

```json
{
  "sessionId": 1,
  "totalCalories": 850,
  "protein": 45,
  "fat": 30,
  "carbs": 90,
  "images": [
    {
      "imageId": 101,
      "imageUrl": "https://xxx.jpg",
      "totalCalories": 400,
      "protein": 20,
      "fat": 10,
      "carbs": 50
    }
  ]
}
```

---

## 2. 二次识别

* **POST** `/recognition/refine`

* 功能：基于已有图片进行补充识别（无需重新上传图片）

### 请求参数（application/json）：

```json
{
  "sessionId": 1,
  "imageId": 101,
  "extraInfo": "鸡胸肉200g"
}
```

---

### 返回格式：

```json
{
  "imageId": 101,
  "imageUrl": "https://xxx.jpg",
  "totalCalories": 500,
  "protein": 40,
  "fat": 8,
  "carbs": 10
}
```

---

#  二、Session 模块（会话）

---

## 1. 获取会话列表

* **GET** `/session/list`

* 功能：分页获取用户识别记录

### 请求参数（query）：

* page int 可选：页码（默认1）
* pageSize int 可选：每页数量
* date string 可选：查询日期（yyyy-MM-dd）

---

### 返回格式：

```json
{
  "total": 20,
  "records": [
    {
      "id": 1,
      "mealType": "LUNCH",
      "totalCalories": 800,
      "createTime": "2026-04-03T12:00:00"
    }
  ]
}
```

---

## 2. 获取会话详情

* **GET** `/session/{id}`

* 功能：获取某次识别的详细数据

---

### 返回格式：

```json
{
  "id": 1,
  "mealType": "DINNER",
  "totalCalories": 900,
  "protein": 50,
  "fat": 35,
  "carbs": 80,
  "images": [
    {
      "imageId": 101,
      "imageUrl": "https://xxx.jpg",
      "totalCalories": 450
    }
  ]
}
```

---

## 3. 删除会话

* **DELETE** `/session/{id}`

* 功能：删除识别记录

---

### 返回格式：

```json
true
```

---

## 4. 修改餐别

* **PUT** `/session/{id}/mealType`

* 功能：修改该次识别属于哪一餐

### 请求参数：

```json
{
  "mealType": "DINNER"
}
```

---

### 返回格式：

```json
true
```

---

#  三、DietLog 模块（饮食日志）

---

## 1. 获取某日饮食

* **GET** `/diet-log`

* 功能：获取某一天的营养总摄入

### 请求参数：

* date string 必填：日期（yyyy-MM-dd）

---

### 返回格式：

```json
{
  "date": "2026-04-03",
  "totalCalories": 2000,
  "protein": 100,
  "fat": 70,
  "carbs": 220
}
```

---

## 2. 获取历史记录

* **GET** `/diet-log/history`

* 功能：获取饮食趋势数据

---

### 返回格式：

```json
[
  {
    "date": "2026-04-01",
    "totalCalories": 1800
  },
  {
    "date": "2026-04-02",
    "totalCalories": 2100
  }
]
```

---

#  四、User 模块（用户）

---

## 1. 获取用户信息（按ID）

* **GET** `/user/user/profile/{userId}`

* 功能：获取用户基本信息和身体数据

---

### 返回格式：

```json
{
  "nickname": "张三",
  "avatar": "https://xxx.jpg",
  "height": 175,
  "weight": 70,
  "bodyFat": 18,
  "bmi": 22.8,
  "bmr": 1600
}
```

---

## 2. 更新用户信息

* **PUT** `/user/user/profile`

* 功能：更新昵称和头像

### 请求参数：

```json
{
  "nickname": "新昵称",
  "avatar": "https://xxx.jpg"
}
```

---

### 返回格式：

```json
true
```

---

## 3. 获取用户身体数据（按ID）
 
* **GET** `/user/user/body/{userId}`
 
* 功能：获取用户身体指标
 
---
 
### 返回格式：
 
```json
{
  "height": 180,
  "weight": 75,
  "bodyFat": 15,
  "bmi": 23.1,
  "bmr": 1650
}
```
 
---
 
## 4. 更新身体数据

* **PUT** `/user/user/body`

* 功能：更新身体指标并自动计算BMI/BMR

### 请求参数：

```json
{
  "height": 180,
  "weight": 75,
  "bodyFat": 15,
  "bmi": 23.1,
  "bmr": 1650,
  "targetCalories": 2000
}
```

---

### 返回格式：

```json
true
```

---

#  五、Auth 模块（认证）

---

## 1. 登录

* **POST** `/user/login`

* 功能：支持密码登录或验证码登录

### 请求参数：

```json
{
  "loginType": "PASSWORD",
  "account": "test@example.com",
  "password": "123456"
}
```

或：

```json
{
  "loginType": "CODE",
  "account": "test@example.com",
  "code": "1234"
}
```

---

### 返回格式：

```json
{
  "id": 1,
  "token": "jwt-token"
}
```



#  六、Analytics 模块（分析）

---

## 1. 热量趋势

* **GET** `/analytics/calories`

* 功能：获取每日热量变化趋势

---

### 返回格式：

```json
[
  {
    "date": "2026-04-01",
    "calories": 1800
  }
]
```

---

## 2. 营养比例

* **GET** `/analytics/nutrition`

* 功能：获取蛋白质/脂肪/碳水比例

---

### 返回格式：

```json
{
  "proteinRatio": 20,
  "fatRatio": 30,
  "carbsRatio": 50
}
```

---

## 3. 饮食建议

* **GET** `/analytics/advice`

* 功能：基于历史数据生成饮食建议

---

### 返回格式：

```json
{
  "advice": "蛋白质摄入不足，建议增加鸡胸肉或鸡蛋摄入"
}
```

