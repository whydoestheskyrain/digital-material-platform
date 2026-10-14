# 数字商品交易平台

本仓库统一管理 v2 和 v3 两个版本的前端与后端代码。新开发建议基于 v3；v2 保留作为历史版本，便于对照与复现。

## 目录

```text
v2/
  backend/    Spring Boot 后端
  frontend/   Vue 3 前端
v3/
  backend/    Spring Boot 后端
  frontend/   Vue 3 前端
```

| 版本 | 后端来源 | 前端来源 |
| --- | --- | --- |
| v2 | [`-backend`](https://github.com/whydoestheskyrain/-backend)，`main` | [`-frontend`](https://github.com/whydoestheskyrain/-frontend)，`main` |
| v3 | [`digital-material-platform-backend`](https://github.com/whydoestheskyrain/digital-material-platform-backend)，`release/v3` | [`digital-material-platform-frontend`](https://github.com/whydoestheskyrain/digital-material-platform-frontend)，`release/v3` |

四个来源仓库的提交历史通过 Git 合并保留。原仓库保持原状，迁移后的开发和版本管理统一在本仓库进行。

## 本地运行

两版都需要 Java、Node.js、MySQL 和 Redis；请分别在所选版本的 `backend` 与 `frontend` 目录执行命令，不要混用两个版本的前后端。

### v3

1. 在 `v3/backend` 中，将 `src/main/resources/application.yml.example` 复制为同目录下的 `application.yml`，配置本地数据库、Redis 和服务密钥，并把 `server.port` 改为 `8081`。实际配置文件已被该目录的 `.gitignore` 忽略。
2. 在 `v3/backend` 中运行 `./mvnw spring-boot:run`（Windows 用 `mvnw.cmd spring-boot:run`）。
3. 在 `v3/frontend` 中运行 `npm ci`，再运行 `npm run serve`。

v3 前端开发服务默认使用 `http://localhost:8082`，其代理配置指向 `http://localhost:8081`。若使用支付宝回跳或跨域请求，还需在本地后端配置中将前端地址设为 `http://localhost:8082`、后端地址设为 `http://localhost:8081`。

### v2

1. 在 `v2/backend` 中，参考 `src/main/resources/application-local.yml.example` 设置本地配置，并准备数据库。
2. 在 `v2/backend` 中运行 `./mvnw spring-boot:run`（Windows 用 `mvnw.cmd spring-boot:run`）。
3. 在 `v2/frontend` 中运行 `npm ci`，再运行 `npm run serve`。

v2 默认使用后端 8081、前端 8082 端口。两版共用默认端口，切换版本时需先停止另一版。

## 版本维护

日常开发直接修改 `v3/`。需要修复旧版时修改 `v2/`，并在提交信息中注明版本。迁移时保留了原始目录和代码；各版本可以独立构建与运行。
