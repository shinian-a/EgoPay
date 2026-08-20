# EgoPay

基于 Spring Boot 的个人收款与订单管理系统，支持微信、支付宝收款码，并兼容常用的易支付接口。

> 本项目仅适合个人开发者学习、调试和测试。请遵守当地法律法规，不要用于未经授权的商业支付或其他非法用途。

## 功能特性

- 微信、支付宝收款码管理
- 创建订单、查询订单、关闭订单
- 订单超时和金额区分，避免相同金额订单冲突
- 异步通知和同步跳转
- 监控端心跳、收款推送和状态查询
- 二维码生成与识别
- 管理后台：系统配置、收款码、订单和运行状态
- 兼容易支付接口：
  - `submit.php`：页面跳转支付
  - `mapi.php`：API 支付
  - `query.php`：订单查询

## 技术栈

| 项目 | 版本或说明 |
| --- | --- |
| Java | 17 |
| Spring Boot | 2.7.5 |
| Spring MVC | `spring-boot-starter-web` |
| Spring Data JPA | `spring-boot-starter-data-jpa` |
| 数据库 | H2 |
| 构建工具 | Maven |
| 发布格式 | WAR |

## 环境要求

- JDK 17 或更高版本
- Maven 3.8 或更高版本（仅源码构建时需要）
- 可被公网访问的服务器（接收异步通知时需要）
- 微信或支付宝收款码
- 如需自动识别收款通知，还需要单独部署并配置监控端

## 快速开始

### 1. 从源码运行

```bash
mvn spring-boot:run
```

### 2. 打包运行

```bash
mvn clean package
java -jar target/mq-0.0.1-SNAPSHOT.war
```

启动后访问：

```text
http://localhost:8081/
```

也可以通过启动参数修改端口：

```bash
java -jar target/mq-0.0.1-SNAPSHOT.war --server.port=9090
```

## 默认配置

首次启动时，系统会自动初始化基础配置：

| 配置项 | 默认值 |
| --- | --- |
| 管理员账号 | `admin` |
| 管理员密码 | `admin` |
| 易支付商户 ID | `1000` |
| 订单有效期 | `5` 分钟 |
| 价格区分方式 | 递增 `0.01` |
| 服务端口 | `8081` |

首次登录后，请立即修改管理员密码、商户密钥、通知地址和收款码配置。

管理后台：

```text
http://localhost:8081/admin.html
```

API 文档页面：

```text
http://localhost:8081/api.html
```

## 数据库与配置

默认配置文件：

```text
src/main/resources/application.properties
```

默认使用 H2 文件数据库：

```text
~/vmq.mv.db
```

Windows 通常位于：

```text
C:\Users\<用户名>\vmq.mv.db
```

Linux 通常位于：

```text
/home/<用户名>/vmq.mv.db
```

当前默认配置：

```properties
server.port=8081
spring.datasource.url=jdbc:h2:~/vmq
spring.datasource.username=vmq
spring.jpa.hibernate.ddl-auto=update
```

迁移数据时，请先停止服务，再复制 `vmq.mv.db` 及相关 H2 文件到新环境。生产环境建议定期备份数据库，并根据实际情况调整数据库、连接池和日志配置。

## 核心 API

### 创建订单

```text
POST /createOrder
```

主要参数：

| 参数 | 说明 |
| --- | --- |
| `payId` | 商户订单号，必须唯一 |
| `param` | 自定义参数，可选 |
| `type` | `1` 微信，`2` 支付宝 |
| `price` | 订单金额 |
| `sign` | `md5(payId + param + type + price + 通讯密钥)` |
| `isHtml` | `0` 返回 JSON，`1` 跳转收银台 |

### 查询订单

```text
GET /getOrder?orderId=<订单编号>
```

### 查询支付状态

```text
GET /checkOrder?orderId=<订单编号>
```

### 关闭订单

```text
POST /closeOrder
```

签名规则：

```text
md5(orderId + 通讯密钥)
```

### 支付回调

系统兼容以下回调地址：

```text
/returnNotify
/pay/notify
/pay/return
```

回调成功时应返回：

```text
success
```

完整参数和返回格式请以项目内的 `api.html` 为准。

## 易支付兼容接口

易支付接口使用后台配置的商户 ID `pid` 和通讯密钥进行 MD5 签名校验：

```text
POST /submit.php
POST /mapi.php
GET  /query.php
```

支持的支付类型：

```text
wxpay
alipay
```

异步通知和同步跳转会根据订单配置回调地址，并返回易支付兼容参数。

## 监控端对接

如果需要自动根据微信或支付宝通知完成订单匹配，监控端需要调用：

```text
POST /appHeart
POST /appPush
GET  /getState
```

监控端必须持续运行，并开启系统要求的通知读取或辅助功能权限。手机系统的后台限制、权限策略和省电设置可能导致收款通知无法及时上报。

## 安全建议

- 首次登录后立即修改默认管理员密码。
- 不要将通讯密钥、数据库文件和生产配置提交到公开仓库。
- 异步通知地址应使用 HTTPS，并校验来源和签名。
- 生产环境不要开启 H2 控制台。
- 建议将管理后台限制在可信网络或增加反向代理认证。
- 定期备份 `vmq.mv.db`，升级前先备份数据。

## 项目结构

```text
src/main/java/                 Java 源码
src/main/resources/            配置文件
src/main/webapp/               管理后台、收银台和 API 页面
src/test/                      测试代码
pom.xml                        Maven 配置
```

编译产物目录 `target/` 和 `out/` 不参与版本控制。

## 许可证

本项目遵循 MIT License。使用、修改和分发时请保留相应版权及许可证声明。
