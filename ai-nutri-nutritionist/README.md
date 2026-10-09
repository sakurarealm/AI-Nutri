```markdown
# AI Health Agent 项目

## 项目结构
## 前端已改为小程序前端，本文档对前端描述作废
```

aiHealthAgent/
 │
 ├── backend/              # 后端代码
 │   ├── src/              # Spring Boot 源代码
 │   └── pom.xml           # Maven 配置文件
 │
 ├── frontend/             # 前端代码
 │   ├── src/              # uni-app 源代码
 │   └── package.json      # npm 配置文件
 └── README.md             # 项目文档

```
## 前端部分（uni-app）

### 1. 安装依赖

首先在 `frontend` 目录中安装所需依赖：

```bash
cd frontend
npm install
```

### 2. 开发环境配置

- 安装 HBuilder 并配置 uni-app 开发环境。
- 配置 Vite 插件支持 PWA（渐进式Web应用）。

### 3. 运行项目

启动开发服务器：

```bash
hbuilderX直接运行即可
```

### 4. 构建项目

构建项目：

```bash
hbuilderX中h5发行
```

### 5. 主要依赖

- `@dcloudio/uni-ui`: uni-app UI 组件库。
- `@huggingface/transformers`: 自然语言处理（NLP）模型。
- `axios`: 用于发送 HTTP 请求。
- `onnxruntime-web`: 在 Web 环境中运行 ONNX 模型。
- `phonemizer`: 将文本转换为拼音。
- `pinia-plugin-unistorage`: 持久化 Pinia 状态。
- `pinyin`: 汉字拼音转换。
- `segmentit`: 中文文本分词。
- `three`: 3D 渲染库。

### 6. 功能说明

- **UI组件**: 使用 `@dcloudio/uni-ui` 提供的 UI 组件构建用户界面。
- **数据交互**: 使用 `axios` 与后端 API 进行通信。
- **自然语言处理**: 使用 `@huggingface/transformers` 和 `onnxruntime-web` 实现文本处理。

------

## 后端部分（Spring Boot + MySQL）

### 1. 环境配置

- 安装 **JDK 17** 并配置环境变量。
- 使用 **Maven** 构建项目：

```bash
mvn clean install
```

### 2. 数据库配置

- 在 MySQL 中创建数据库并导入 `schema.sql` 文件以初始化数据库。
- 配置 `application.properties` 文件中的数据库连接：

```properties
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/ai_nutritionist?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Taipei&useSSL=false&allowPublicKeyRetrieval=true&allowMultiQueries=true
    username: ai_nutritionist
    password: SecurePass2025!
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      maximum-pool-size: 8
      minimum-idle: 5
      idle-timeout: 30000
      connection-timeout: 30000
      max-lifetime: 1800000
      auto-commit: true
      pool-name: HikariPool-AI
      shutdown-timeout: 5000
```

### 3. 运行项目

启动 Spring Boot 应用：

```bash
mvn spring-boot:run
```

### 4. 主要依赖

- `spring-boot-starter-web`: 提供 Web 功能支持。
- `spring-boot-starter-data-jpa`: 提供 JPA 数据库操作支持。
- `spring-boot-starter-security`: 提供安全管理功能，如认证与授权。
- `mysql-connector-j`: MySQL JDBC 连接器。
- `aliyun-java-sdk-core`: 与阿里云 API 进行交互。
- `jjwt`: 用于生成和验证 JWT（JSON Web Token）。
- `fastjson`: 用于处理 JSON 数据。
- `httpclient`: 用于发送 HTTP 请求。

### 5. API 文档

提供后端 API 接口文档，使用 Swagger 或其他工具生成接口文档，前端可以根据接口文档进行数据交互。

------

## 认证与授权

- 后端使用 **JWT**（JSON Web Token）进行用户认证和授权。
- 前端在请求时，需要在请求头中添加 `Authorization` 字段，携带有效的 JWT。

------

## 数据交互

- 前后端数据采用 **JSON** 格式进行传输。
- 具体接口可以参考后端 API 文档。

------

## 开发工具

- **前端开发**: HBuilder + uni-app + Vite
- **后端开发**: IntelliJ IDEA（版本 17.0.14） + Spring Boot
- **数据库管理**: Navicat 16
- **JDK 版本**: 17

------

## 构建与部署

1. **前端**:
   - 使用 Vite 进行构建，支持多平台（如微信小程序、Web、APP）部署。
2. **后端**:
   - 使用 Maven 构建和部署 Spring Boot 项目，支持本地和云环境部署。

------

## 补充说明

- **开发过程中**，请确保安装 JDK 17，并正确配置开发工具。
- **前端和后端**：前后端分离，使用 RESTful API 进行数据交互。

```

```