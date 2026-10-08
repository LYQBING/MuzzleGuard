# Muzzle Guard

## 自定义口球贴图

将工作区根目录的 `123.png` 放入 `src/main/resources/assets/muzzle_guard/textures/entity/equipment/humanoid/muzzle.png` 并提交，重新运行 GitHub Actions 构建并下载 `muzzle-guard` 构建产物，即可使用该口球贴图。资源包也可通过 `assets/muzzle_guard/textures/entity/equipment/humanoid/muzzle.png` 覆盖该贴图。

## GitHub Actions 构建

推送代码或创建 Pull Request 会自动构建。也可以在仓库的 **Actions → Build Muzzle Guard → Run workflow** 手动构建；完成后从 `muzzle-guard` artifact 下载 JAR。
