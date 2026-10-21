# 不许涩涩的异世界

## GitHub Actions 构建

推送代码或创建 Pull Request 会自动构建。也可以在仓库的 **Actions → Build 不许涩涩的异世界 → Run workflow** 手动构建：

- `mod_version`：可选版本覆盖；留空使用 `gradle.properties` 中的 `mod_version`。
- `mod_description`：可选模组介绍覆盖；留空使用 `gradle.properties` 中的 `mod_description`。

构建完成后，artifact 名称和 JAR 文件名会自动使用实际构建版本。
