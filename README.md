# 不许涩涩的异世界

## 自定义口球贴图

玩家佩戴口球后的面部模型使用 `src/main/resources/assets/no_lewd_otherworld/textures/item/ball_gag/ball_gag_face.png`。该贴图来自工作区根目录的 `123.png`，与背包口球图标 `textures/item/ball_gag/ball_gag.png` 分开。资源包覆盖路径为 `assets/no_lewd_otherworld/textures/item/ball_gag/ball_gag_face.png`。

## GitHub Actions 构建

推送代码或创建 Pull Request 会自动构建。也可以在仓库的 **Actions → Build 不许涩涩的异世界 → Run workflow** 手动构建：

- `mod_version`：可选版本覆盖；留空使用 `gradle.properties` 中的 `mod_version`。
- `mod_description`：可选模组介绍覆盖；留空使用 `gradle.properties` 中的 `mod_description`。

构建完成后，artifact 名称和 JAR 文件名会自动使用实际构建版本。
