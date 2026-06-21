# TVRemoteIME HA Edition (小盒精灵 HA 改造版)

基于 [newPersonKing/TVRemoteIME](https://github.com/newPersonKing/TVRemoteIME) 的 Home Assistant 适配版本。

## 改造内容

针对 Android 11+ 系统（包括雷鸟/TCL 新固件电视）做了以下适配：

1. **升级 Gradle 8.2 + Android Gradle Plugin 8.1.4**
2. **compileSdk 34 / targetSdk 34 / minSdk 21**（兼容 Android 5.0 ~ Android 14）
3. **AndroidX 全量迁移**（替换所有 `android.support.*`）
4. **jcenter → mavenCentral + 阿里云镜像**
5. **新增 `network_security_config.xml`**，允许局域网 HTTP 明文通信
6. **新增 `<queries>`**，让 `/apps` 能列出其他 APP（Android 11+）
7. **改为 Foreground Service**，通过常驻通知保活，避免被系统杀后台
8. **通知渠道 + POST_NOTIFICATIONS 权限**（Android 13+）
9. ** PendingIntent.FLAG_IMMUTABLE**（Android 12+）
10. **`foregroundServiceType="mediaPlayback"`**（Android 14+）

## 如何编译

### 方式一：GitHub Actions 自动编译（推荐，免本地环境）

1. Fork 本仓库到你的 GitHub 账号
2. 进入你 fork 后的仓库
3. 默认情况下，**任何 push 到 main/master 分支都会自动触发编译**
4. 也可以在仓库页面 → `Actions` 标签 → 选择 `Build TVRemoteIME HA Edition APK` 工作流 → 点击 `Run workflow` 手动触发
5. 等待编译完成（约 5-10 分钟）
6. 在 Actions 运行详情页底部 **Artifacts** 区域下载 APK：
   - `TVRemoteIME-Debug-APK` - Debug 版（带日志，推荐先用这个测试）
   - `TVRemoteIME-Release-APK` - Release 版（更小，正式用）

### 方式二：本地编译

需要 JDK 17 + Android SDK 34：

```bash
git clone https://github.com/<你的用户名>/TVRemoteIME-HA.git
cd TVRemoteIME-HA
chmod +x gradlew
./gradlew :IMEService:assembleDebug
# 产物：IMEService/build/outputs/apk/debug/IMEService-debug.apk
```

## 安装到电视

### 步骤 1：传输 APK 到电视

- **U 盘**：APK 拷到 U 盘根目录，插电视 USB，用文件管理器打开
- **多屏互动 APP**：手机装 TCL 多屏互动，用「应用推送」功能推送 APK
- **当贝市场 / 欢视助手**：电视端装好后从里面下载任意文件管理器再装 APK

### 步骤 2：允许"未知来源"

第一次安装会提示，按提示去**设置 → 安全 → 未知来源**打开即可。

### 步骤 3：打开 APP 并按提示操作

1. 点击"设置"激活启用输入法 → 系统输入法设置里勾选"小盒精灵"
2. 点击"设置"设为默认输入法 → 选择"小盒精灵"作为默认
3. 如果系统不允许设为默认，点"手动启动"启动服务

### 步骤 4：验证 HTTP 接口

在**同一局域网**的电脑/手机浏览器访问：

```
http://电视IP:9978/
```

看到控制面板网页就成功了。电视 IP 在 APP 主界面会显示。

## Home Assistant 接入

### configuration.yaml 示例

```yaml
rest_command:
  # 按键控制
  tv_key_home:
    url: "http://192.168.1.50:9978/key"
    method: POST
    content_type: "application/x-www-form-urlencoded"
    payload: "code=KEYCODE_HOME"
  tv_key_back:
    url: "http://192.168.1.50:9978/key"
    method: POST
    content_type: "application/x-www-form-urlencoded"
    payload: "code=KEYCODE_BACK"
  tv_key_power:
    url: "http://192.168.1.50:9978/key"
    method: POST
    content_type: "application/x-www-form-urlencoded"
    payload: "code=KEYCODE_POWER"
  tv_volume_up:
    url: "http://192.168.1.50:9978/key"
    method: POST
    content_type: "application/x-www-form-urlencoded"
    payload: "code=KEYCODE_VOLUME_UP"
  tv_volume_down:
    url: "http://192.168.1.50:9978/key"
    method: POST
    content_type: "application/x-www-form-urlencoded"
    payload: "code=KEYCODE_VOLUME_DOWN"
  tv_mute:
    url: "http://192.168.1.50:9978/key"
    method: POST
    content_type: "application/x-www-form-urlencoded"
    payload: "code=KEYCODE_VOLUME_MUTE"
  tv_dpad_up:
    url: "http://192.168.1.50:9978/key"
    method: POST
    content_type: "application/x-www-form-urlencoded"
    payload: "code=KEYCODE_DPAD_UP"
  tv_dpad_down:
    url: "http://192.168.1.50:9978/key"
    method: POST
    content_type: "application/x-www-form-urlencoded"
    payload: "code=KEYCODE_DPAD_DOWN"
  tv_dpad_left:
    url: "http://192.168.1.50:9978/key"
    method: POST
    content_type: "application/x-www-form-urlencoded"
    payload: "code=KEYCODE_DPAD_LEFT"
  tv_dpad_right:
    url: "http://192.168.1.50:9978/key"
    method: POST
    content_type: "application/x-www-form-urlencoded"
    payload: "code=KEYCODE_DPAD_RIGHT"
  tv_dpad_center:
    url: "http://192.168.1.50:9978/key"
    method: POST
    content_type: "application/x-www-form-urlencoded"
    payload: "code=KEYCODE_DPAD_CENTER"
  tv_menu:
    url: "http://192.168.1.50:9978/key"
    method: POST
    content_type: "application/x-www-form-urlencoded"
    payload: "code=KEYCODE_MENU"

  # 启动 APP
  tv_open_iqiyi:
    url: "http://192.168.1.50:9978/run"
    method: POST
    content_type: "application/x-www-form-urlencoded"
    payload: "packageName=com.qiyi.video"
  tv_open_youtube:
    url: "http://192.168.1.50:9978/run"
    method: POST
    content_type: "application/x-www-form-urlencoded"
    payload: "packageName=com.google.android.youtube.tv"
  tv_open_bilibili:
    url: "http://192.168.1.50:9978/run"
    method: POST
    content_type: "application/x-www-form-urlencoded"
    payload: "packageName=com.xiaodianshi.tv.ijk"

  # 输入文本
  tv_input_text:
    url: "http://192.168.1.50:9978/text"
    method: POST
    content_type: "application/x-www-form-urlencoded"
    payload: "text={{ text }}"

# 封装为开关
switch:
  - platform: template
    switches:
      tv_home:
        friendly_name: "电视 Home"
        value_template: "{{ false }}"
        turn_on:
          - service: rest_command.tv_key_home
        turn_off:
          - service: rest_command.tv_key_home
      tv_back:
        friendly_name: "电视 返回"
        value_template: "{{ false }}"
        turn_on:
          - service: rest_command.tv_key_back
        turn_off:
          - service: rest_command.tv_key_back
```

### 自动化示例

```yaml
automation:
  - alias: "回家自动打开电视"
    trigger:
      - platform: state
        entity_id: group.family
        to: "home"
    action:
      - service: rest_command.tv_key_power
      - delay: "00:00:05"
      - service: rest_command.tv_open_bilibili
```

## HTTP API 完整参考

| 方法 | 路径 | 参数 | 说明 |
|------|------|------|------|
| POST | `/key` | `code=KEYCODE_XXX` | 模拟按键（按下+抬起）|
| POST | `/keydown` | `code=KEYCODE_XXX` | 按键按下 |
| POST | `/keyup` | `code=KEYCODE_XXX` | 按键抬起 |
| POST | `/text` | `text=hello` | 输入文本 |
| POST | `/run` | `packageName=com.xxx` | 启动 APP |
| POST | `/runSystem` | `packageName=com.xxx` | 启动系统 APP |
| POST | `/uninstall` | `packageName=com.xxx` | 卸载 APP |
| POST | `/apps` | `system=true\|false` | 列出 APP（返回 JSON）|
| POST | `/clearCache` | - | 清除缓存 |
| GET  | `/` | - | 控制面板 Web UI |
| GET  | `/index.html` | - | 同上 |

## 常用 KEYCODE

| KEYCODE | 数字 | 说明 |
|---------|------|------|
| `KEYCODE_HOME` | 3 | Home 键 |
| `KEYCODE_BACK` | 4 | 返回 |
| `KEYCODE_MENU` | 82 | 菜单 |
| `KEYCODE_POWER` | 26 | 电源 |
| `KEYCODE_VOLUME_UP` | 24 | 音量+ |
| `KEYCODE_VOLUME_DOWN` | 25 | 音量- |
| `KEYCODE_VOLUME_MUTE` | 164 | 静音 |
| `KEYCODE_DPAD_UP` | 19 | 上 |
| `KEYCODE_DPAD_DOWN` | 20 | 下 |
| `KEYCODE_DPAD_LEFT` | 21 | 左 |
| `KEYCODE_DPAD_RIGHT` | 22 | 右 |
| `KEYCODE_DPAD_CENTER` | 23 | 确认 |
| `KEYCODE_MEDIA_PLAY_PAUSE` | 85 | 播放/暂停 |
| `KEYCODE_MEDIA_NEXT` | 87 | 下一首 |
| `KEYCODE_MEDIA_PREVIOUS` | 88 | 上一首 |

## 已知限制

1. **无法开机**：APP 没运行就接不到 HTTP 请求。建议配合「智能插座 + 通电自动开机」解决
2. **设为默认输入法是关键**：如果系统不允许设置，按键会退化为 ADB 模式（仍需要 ADB）
3. **雷鸟系统保活**：需在**设置 → 应用 → 小盒精灵 → 电池/自启动**里允许后台运行
4. ** targetType 34+ 限制**：Android 14+ 对前台服务类型有严格要求，已用 `mediaPlayback` 兜底

## 致谢

- 原作者 kingthy：[TVRemoteIME](https://github.com/kingthy/TVRemoteIME)
- Fork 来源：[newPersonKing/TVRemoteIME](https://github.com/newPersonKing/TVRemoteIME)
- 第三方库：NanoHTTPD、AdbLib、ijkplayer、Cling DLNA、Thunder SDK

## License

原项目 LICENSE 同步保留。
