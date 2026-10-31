# LibraryWeb2026 图书馆管理系统

Java Web 课程作业：基于 **Servlet + JDBC** 的图书馆管理系统，使用 Maven 构建、部署到 Tomcat 10.1，数据库为 MySQL 8.4（`librarydb`）。

> 技术约束：不使用 JSP、不使用 Spring / Spring Boot / MyBatis，不采用前后端分离；前端为 HTML + CSS + Vue 3 + Axios + jQuery，服务端全部由 Servlet 响应请求，数据访问使用改造后的 `JDBCTool`。

---

## 一、功能清单

### 1. 用户功能（必做）

| 功能 | 说明 |
| --- | --- |
| 用户注册 | 姓名、密码、确认密码、手机号（正则校验、唯一）、专业、角色；选择角色后动态显示该角色专属字段；姓名唯一 |
| 用户登录 | 姓名 + 密码 + 图形验证码；验证码不区分大小写、一次性使用；重置按钮清空；失败提示 |
| 首页 | 显示当前用户姓名、角色、专业、上次登录时间；按角色显示不同功能卡片与顶部菜单 |
| 用户信息查询 | 仅管理员；关键字模糊查询姓名/手机号/专业/角色；默认不显示数据；按创建时间倒序；只读；"查看"弹窗展示全部字段与角色专属字段 |
| 个人借阅查询 | 全部 / 未还 / 按借书日期范围；超期状态自动计算并显示超期天数 |

角色功能卡：

- **管理员**：用户信息查询、借阅记录查询、借阅数据统计、借书、还书、审阅推荐图书
- **操作员**：添加书籍、查看所有书籍、废弃图书、编辑图书、图书副本编辑、借书、还书、个人借阅查询
- **学生**：借书、还书、个人借阅查询
- **教师**：借书、还书、个人借阅查询、推荐图书

### 2. 图书功能（必做）

| 功能 | 说明 |
| --- | --- |
| 添加书籍 | 表单录入；按 `stock` 自动生成等量图书副本；添加后新书按创建时间倒序显示在第一行；可连续添加 |
| 查询书籍 | 书名/作者/类型/出版社关键字模糊查询；关键字为空查询全部 |
| 编辑图书 | 只修改 `title/author/type/publisher/days/price`，不修改 `stock`、副本、`status` |
| 废弃图书 | 存在未归还副本时拒绝；全部归还后 `status` 改为"已废弃"，不删除借阅记录 |
| 图书副本编辑 | 查看副本列表与借阅人；新增副本 `stock+1`；删除副本（借阅中或存在借阅历史时禁止）后 `stock-1` |
| 借书 | 展示可借图书与本人未还记录；选择图书后自动分配一个未借出副本；生成借阅记录、应还日期 = 借书时间 + `days`；库存-1 |
| 还书 | 展示本人未还记录；归还后记录从"未还"消失，库存+1；管理员/操作员可代还 |
| 推荐图书 | 教师提交书名/作者/出版社/价格；馆内已有相关书籍则返回馆藏信息，否则写入 `bookrecommend` 待审核 |
| 审阅图书 | 管理员查看待审核推荐；通过时补全类型/期限/库存/简介并加入图书列表；驳回必须填写理由 |

### 3. 查询与统计（选做，已实现）

- 借阅记录查询（管理员）：关键字（用户名/图书名模糊）、超期查询、借书日期范围查询
- 借阅数据统计（管理员）：按图书类型统计借阅次数（无借阅的类型显示 0）、按用户专业统计借阅次数
- 个人借阅查询：全部 / 未还 / 日期范围

---

## 二、技术栈

| 层次 | 技术 |
| --- | --- |
| 构建 | Maven（war 包），Java 17 编译（JDK 21 可编译运行） |
| Web 容器 | Tomcat 10.1（`jakarta.servlet` 6.0） |
| 服务端 | Servlet 6.0 + JDBC（原生 SQL，无 ORM 框架） |
| 数据库 | MySQL 8.4，库名 `librarydb` |
| JSON | Gson 2.11 |
| 前端 | HTML5 + CSS3 + Vue 3（本地 vendor）+ Axios + jQuery，无 JSP |

---

## 三、目录结构

```
LibraryWeb2026
├── pom.xml
├── README.md
├── docs
│   ├── 演示视频脚本.md
│   └── screenshots/                 # 运行截图
└── src/main
    ├── java/com/example
    │   ├── library
    │   │   ├── dao/                 # UserDao / BookDao / LoanDao / RecommendDao
    │   │   ├── entity/              # Base、Book、BookCopy、User 及子类、Loan、BookRecommend
    │   │   ├── filter/              # EncodingFilter、AuthFilter
    │   │   ├── servlet/             # BaseServlet、Auth/User/Book/Loan/Recommend/Captcha
    │   │   ├── util/Dates.java
    │   │   └── web/                 # Result、ApiException
    │   └── util/                    # DBUtil、JDBCTool（作业1 工具，已适配本库）
    ├── resources/db.properties
    └── webapp
        ├── login.html  register.html  index.html
        ├── books.html  borrow.html  return.html  my-loans.html
        ├── users.html  loan-query.html  stats.html
        ├── recommend.html  review.html
        ├── css/style.css
        ├── js/app.js
        └── vendor/                  # vue / axios / jquery 本地文件（离线可用）
```

---

## 四、快速开始

### 1. 环境要求

- JDK 17 及以上
- Maven 3.6+
- Tomcat 10.1（必须是 10.x，使用 `jakarta.servlet`；Tomcat 9 的 `javax.servlet` 不兼容）
- MySQL 8.x

### 2. 还原数据库

使用课程提供的 `librarydb` 备份还原（解压后的 `.sql` 文件）：

```bash
mysql -uroot -p -e "CREATE DATABASE IF NOT EXISTS librarydb DEFAULT CHARACTER SET utf8mb4;"
mysql -uroot -p librarydb < librarydb.sql
```

也可以用 Navicat：新建连接 → 右键"运行 SQL 文件" → 选择备份 → 目标库 `librarydb`。

> 表结构使用驼峰列名（`createTime`、`bookId`、`isBorrowed` 等），请勿手工改成下划线命名。

### 3. 修改数据库连接

编辑 `src/main/resources/db.properties`：

```properties
jdbc.driver=com.mysql.cj.jdbc.Driver
jdbc.url=jdbc:mysql://localhost:3306/librarydb?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
jdbc.username=root
jdbc.password=你的密码
```

### 4. 构建

```bash
mvn clean package
```

产物：`target/LibraryWeb2026.war`

### 5. 部署到 Tomcat

把 `LibraryWeb2026.war` 复制到 `%CATALINA_HOME%\webapps\`，启动 Tomcat：

```bat
%CATALINA_HOME%\bin\startup.bat
```

浏览器访问：<http://localhost:8080/LibraryWeb2026/>

### 6. 测试账号

| 角色 | 姓名 | 密码 |
| --- | --- | --- |
| 管理员 | admin | admin123 |
| 管理员 | admin2 | admin456 |
| 操作员 | operator | operator123 |
| 学生 | student1 | student123 |
| 学生 | student2 | student123 |
| 教师 | teacher | teacher123 |

---

## 五、JDK 与 Tomcat 环境变量（Windows）

1. 新建系统变量 `JAVA_HOME` = `C:\Program Files\Java\jdk-21`
2. 新建系统变量 `CATALINA_HOME` = `D:\tomcat\apache-tomcat-10.1.61`
3. 编辑 `Path`，加入：
   - `%JAVA_HOME%\bin`
   - `%CATALINA_HOME%\bin`
4. 打开新的命令行验证：

```bat
java -version
javac -version
mvn -v
catalina version
```

`catalina version` 能输出版本号即说明 Tomcat 环境变量配置成功，本机即可作为 Web 服务器。

---

## 六、局域网访问

1. 查询本机 IPv4 地址：

```bat
ipconfig
```

找到"无线局域网适配器 WLAN"或"以太网"下的 `IPv4 地址`，例如 `192.168.1.100`。

2. 放行 8080 端口（管理员命令行执行一次）：

```bat
netsh advfirewall firewall add rule name="Tomcat 8080" dir=in action=allow protocol=TCP localport=8080
```

3. 同一局域网内的手机/电脑访问：

```
http://192.168.1.100:8080/LibraryWeb2026/
```

> 若仍无法访问：确认 Tomcat 监听 `0.0.0.0:8080`（`netstat -ano | findstr :8080`）、两台设备在同一网段、校园网/公司网未开启"AP 隔离"。

---

## 七、推送到 Gitee

```bash
git init
git add .
git commit -m "feat: LibraryWeb2026 图书馆管理系统"
git branch -M master
git remote add origin https://gitee.com/你的用户名/LibraryWeb2026.git
git push -u origin master
```

首次推送需要输入 Gitee 账号密码（或使用私人令牌）。仓库地址形如：
`https://gitee.com/你的用户名/LibraryWeb2026`

---

## 八、设计说明

### 1. 分层

```
浏览器（Vue + Axios）
   │  JSON / HTTP
Servlet（Auth/User/Book/Loan/Recommend/Captcha）
   │  参数校验、权限判断、事务边界
DAO（UserDao/BookDao/LoanDao/RecommendDao）
   │  PreparedStatement + JDBCTool
MySQL librarydb
```

- `BaseServlet`：统一 `doGet/doPost` 分发、JSON 读写、参数校验、异常转 `{code,msg,data}`。
- `EncodingFilter`：统一 UTF-8，避免中文乱码。
- `AuthFilter`：未登录 API 返回 401、页面跳登录页；按页面配置角色白名单，越权跳首页。
- 所有写操作使用 `Connection` 事务 + `commit/rollback`，借书/还书对 `book`、`bookcopy` 行加 `FOR UPDATE` 锁，防止并发超借。

### 2. JDBCTool 的适配（作业1 工具）

原始 `JDBCTool` 按"驼峰转下划线"拼列名、只读取子类字段，直接用于本库会失败。改造点：

1. 列名直接使用字段名（本库列名就是驼峰）；
2. 沿继承链收集字段，支持 `Base` 中的 `id/createTime/updateTime`；
3. 跳过 `static`、`transient`、集合/Map 及非标量字段（如 `Book.bookCopies`、`BookCopy.book`）；
4. `bit(1)` → `boolean`、`DATETIME` → `LocalDateTime`、`DATE` → `LocalDate`；
5. 增加 `Admin/Operator/Student/Teacher → user` 的表名映射（单表存多角色）。

`Book`、`BookCopy`、`Loan` 的简单增删改查使用 `JDBCTool`；带 JOIN 的查询与多表事务使用 DAO 中的原生 SQL。

### 3. 库存（stock）语义

本库 `stock` 表示**当前可借副本数**（样例数据可验证：某书 4 个副本、1 个已借出时 `stock=3`）。

- 借书：`stock-1`；还书：`stock+1`
- 新增副本：`stock+1`；删除副本：`stock-1`
- 新增图书：按表单 `stock` 生成等量副本

### 4. 借阅状态

数据库 `loan.status` 只存"未还/已还"；查询时若 `status='未还'` 且 `dueDate < now()`，派生显示为"超期"并计算超期天数。还书后状态改为"已还"，历史记录保留。

### 5. 权限控制

三层控制：`AuthFilter` 页面白名单 → 页面菜单按角色渲染 → Servlet `requireRole` 接口校验。前端隐藏入口只是体验优化，真正拦截在服务端。

---

## 九、作业要求对照

| 要求 | 实现 |
| --- | --- |
| Maven 创建 Java Web 项目，名 LibraryWeb2026 | `pom.xml`，`artifactId=LibraryWeb2026`，war 打包 |
| Gitee 建仓并 push，每完成一个功能 push | 本地已按功能分多次提交，推送命令见第七节 |
| 完成用户功能、图书功能，查询选做 | 全部实现（见功能清单） |
| JDK/Tomcat 环境变量，本机作 Web 服务器 | 见第五节，本机已验证 `http://localhost:8080/LibraryWeb2026/` |
| 打 war 跑在本机 Tomcat，局域网访问并录视频 | `mvn package` 产物部署；局域网步骤见第六节；录制脚本见 `docs/演示视频脚本.md` |
| 前端 html/css/jQuery/Axios/Vue，不能用 JSP | 全部 HTML + Vue3 + Axios + jQuery，无 JSP |
| 服务端 Servlet + JDBC，不用 SpringBoot/MyBatis | 纯 Servlet 6 + JDBC，无 Spring/MyBatis |
| MySQL + 作业1 的 JDBCTool | MySQL 8.4；`com.example.util.JDBCTool`（已适配） |
| 不前后端分离 | 前端静态页与 Servlet 同一个 war、同源部署 |
| 用给定数据库备份，不新建库 | 直接连接 `librarydb`，未新增业务表 |
| 实体类字段 | 与题目一致（`Base/Book/BookCopy/User/Admin/Operator/Student/Teacher/Loan`） |

---

## 十、常见问题

| 现象 | 处理 |
| --- | --- |
| 启动报 `ClassNotFoundException: com.mysql.cj.jdbc.Driver` | 确认 `mysql-connector-j` 已打进 `WEB-INF/lib`，重新 `mvn package` |
| 页面 404 | 访问路径要带项目名：`/LibraryWeb2026/`；确认 war 已解压到 `webapps` |
| 中文乱码 | 数据库/表/连接串均为 `utf8mb4`；`EncodingFilter` 已统一 UTF-8 |
| 登录提示验证码错误 | 验证码一次性使用，刷新页面或点击图片重新获取 |
| 端口被占用 | 修改 `conf/server.xml` 中 `8080`，或结束占用进程 `netstat -ano | findstr :8080` |
| 时间相差 8 小时 | 连接串必须带 `serverTimezone=Asia/Shanghai` |
| 废弃/删除副本被拒绝 | 属于业务规则：有未还副本不能废弃；有借阅历史的副本不能删除 |

---

## 十一、运行截图

![登录](docs/screenshots/login.png)

![首页](docs/screenshots/index.png)

![图书管理](docs/screenshots/books.png)

![借书](docs/screenshots/borrow.png)
