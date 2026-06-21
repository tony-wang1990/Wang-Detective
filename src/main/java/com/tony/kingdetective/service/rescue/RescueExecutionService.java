package com.tony.kingdetective.service.rescue;

import com.tony.kingdetective.bean.params.ops.SshCommandParams;
import com.tony.kingdetective.bean.response.ops.SshCommandRsp;
import com.tony.kingdetective.exception.OciException;
import com.tony.kingdetective.service.ops.SshHostService;
import com.tony.kingdetective.service.ops.WebSshService;
import org.springframework.stereotype.Service;

@Service
public class RescueExecutionService {

    private final WebSshService webSshService;
    private final SshHostService sshHostService;

    public RescueExecutionService(WebSshService webSshService, SshHostService sshHostService) {
        this.webSshService = webSshService;
        this.sshHostService = sshHostService;
    }

    public SshCommandRsp runLightRescue(String hostId) {
        return execute(hostId, lightRescueCommand(), 120);
    }

    public SshCommandRsp preflightNetboot(String hostId) {
        return execute(hostId, netbootPreflightCommand(), 45);
    }

    public SshCommandRsp prepareNetboot(String hostId, String confirmation, boolean reboot) {
        String expected = reboot ? "NETBOOT-REBOOT" : "NETBOOT";
        if (!expected.equals(confirmation)) {
            throw new OciException(-1, "请输入确认词 " + expected);
        }
        return execute(hostId, netbootPrepareCommand(reboot), 180);
    }

    private SshCommandRsp execute(String hostId, String command, int timeoutSeconds) {
        SshCommandParams params = new SshCommandParams();
        params.setCredential(sshHostService.credentialForHost(hostId));
        params.setCommand(command);
        params.setTimeoutSeconds(timeoutSeconds);
        SshCommandRsp result = webSshService.execute(params);
        if (Boolean.TRUE.equals(result.getTimedOut())) {
            throw new OciException(-1, "远程执行超时，请到运维终端检查目标主机");
        }
        return result;
    }

    private String lightRescueCommand() {
        return """
                bash -s <<'WANG_RESCUE'
                set -u
                if [ "$(id -u)" -eq 0 ]; then
                  SUDO=""
                elif sudo -n true >/dev/null 2>&1; then
                  SUDO="sudo -n"
                else
                  echo "ERROR: 当前 SSH 用户不是 root，且无法免密 sudo。"
                  exit 20
                fi

                echo "=== W-探长一键轻量自救 ==="
                date
                echo "[1/6] 检查磁盘和 inode"
                df -h /
                df -ih /

                echo "[2/6] 修复 SSH 运行目录与服务"
                $SUDO mkdir -p /run/sshd
                if systemctl list-unit-files ssh.service >/dev/null 2>&1; then
                  $SUDO systemctl enable ssh >/dev/null 2>&1 || true
                  $SUDO systemctl restart ssh
                elif systemctl list-unit-files sshd.service >/dev/null 2>&1; then
                  $SUDO systemctl enable sshd >/dev/null 2>&1 || true
                  $SUDO systemctl restart sshd
                else
                  echo "WARN: 未发现 ssh/sshd systemd 服务"
                fi

                echo "[3/6] 修复当前用户 SSH 权限"
                HOME_DIR="$(getent passwd "$(id -un)" | cut -d: -f6)"
                if [ -n "$HOME_DIR" ] && [ -d "$HOME_DIR/.ssh" ]; then
                  chmod 700 "$HOME_DIR/.ssh" || true
                  [ -f "$HOME_DIR/.ssh/authorized_keys" ] && chmod 600 "$HOME_DIR/.ssh/authorized_keys" || true
                fi

                echo "[4/6] 检查防火墙"
                if command -v ufw >/dev/null 2>&1 && $SUDO ufw status 2>/dev/null | grep -q '^Status: active'; then
                  $SUDO ufw allow 22/tcp >/dev/null
                  echo "UFW: 已确保 22/tcp 放行"
                else
                  echo "UFW: 未启用或未安装，不修改"
                fi

                echo "[5/6] 收敛异常日志占用"
                $SUDO journalctl --vacuum-size=200M >/dev/null 2>&1 || true

                echo "[6/6] 验证"
                systemctl is-active ssh 2>/dev/null || systemctl is-active sshd 2>/dev/null || true
                ss -lnt 2>/dev/null | grep -E ':22\\b' || true
                ip route || true
                echo "=== 轻量自救执行完成 ==="
                WANG_RESCUE
                """;
    }

    private String netbootPreflightCommand() {
        return """
                bash -s <<'WANG_NETBOOT_CHECK'
                set -u
                echo "=== netboot.xyz 一次性引导预检 ==="
                echo "ARCH=$(uname -m)"
                [ -d /sys/firmware/efi ] && echo "UEFI=yes" || echo "UEFI=no"
                echo "ESP_SOURCE=$(findmnt -n -o SOURCE /boot/efi 2>/dev/null || true)"
                command -v efibootmgr >/dev/null 2>&1 && echo "EFIBOOTMGR=yes" || echo "EFIBOOTMGR=no"
                command -v curl >/dev/null 2>&1 && echo "CURL=yes" || echo "CURL=no"
                command -v wget >/dev/null 2>&1 && echo "WGET=yes" || echo "WGET=no"
                df -h /boot/efi 2>/dev/null || true
                efibootmgr 2>/dev/null | head -30 || true
                echo "要求：UEFI=yes、ESP_SOURCE 非空、efibootmgr 与 curl/wget 可用。"
                WANG_NETBOOT_CHECK
                """;
    }

    private String netbootPrepareCommand(boolean reboot) {
        String rebootCommand = reboot
                ? "echo '将在 5 秒后重启并尝试进入 netboot.xyz'; sleep 5; $SUDO systemctl reboot"
                : "echo '已设置 BootNext。未自动重启，请确认控制台可用后手动重启。'";
        return """
                bash -s <<'WANG_NETBOOT'
                set -Eeuo pipefail
                if [ "$(id -u)" -eq 0 ]; then
                  SUDO=""
                elif sudo -n true >/dev/null 2>&1; then
                  SUDO="sudo -n"
                else
                  echo "ERROR: 当前 SSH 用户不是 root，且无法免密 sudo。"
                  exit 20
                fi

                [ -d /sys/firmware/efi ] || { echo "ERROR: 仅支持 UEFI 实例"; exit 21; }
                ESP_SOURCE="$(findmnt -n -o SOURCE /boot/efi)"
                [ -n "$ESP_SOURCE" ] || { echo "ERROR: /boot/efi 未挂载"; exit 22; }
                command -v efibootmgr >/dev/null 2>&1 || { echo "ERROR: 缺少 efibootmgr"; exit 23; }

                ARCH="$(uname -m)"
                case "$ARCH" in
                  x86_64|amd64)
                    URL="https://boot.netboot.xyz/ipxe/netboot.xyz-snp.efi"
                    FILE="netboot.xyz-snp.efi"
                    ;;
                  aarch64|arm64)
                    URL="https://boot.netboot.xyz/ipxe/netboot.xyz-arm64.efi"
                    FILE="netboot.xyz-arm64.efi"
                    ;;
                  *)
                    echo "ERROR: 不支持的架构 $ARCH"
                    exit 24
                    ;;
                esac

                if command -v curl >/dev/null 2>&1; then
                  $SUDO curl -fL --retry 3 "$URL" -o "/boot/efi/$FILE"
                elif command -v wget >/dev/null 2>&1; then
                  $SUDO wget -O "/boot/efi/$FILE" "$URL"
                else
                  echo "ERROR: 缺少 curl/wget"
                  exit 25
                fi

                PARTITION="$(lsblk -no PARTN "$ESP_SOURCE" | head -1)"
                PARENT="$(lsblk -no PKNAME "$ESP_SOURCE" | head -1)"
                [ -n "$PARTITION" ] && [ -n "$PARENT" ] || { echo "ERROR: 无法识别 EFI 分区"; exit 26; }

                BEFORE="$($SUDO efibootmgr | awk '/Boot[0-9A-Fa-f]{4}/ {print substr($1,5,4)}' | sort)"
                $SUDO efibootmgr --create --disk "/dev/$PARENT" --part "$PARTITION" \
                  --label "netboot.xyz (W-探长)" --loader "\\\\$FILE"
                AFTER="$($SUDO efibootmgr | awk '/Boot[0-9A-Fa-f]{4}/ {print substr($1,5,4)}' | sort)"
                BOOTNUM="$(comm -13 <(printf '%s\\n' "$BEFORE") <(printf '%s\\n' "$AFTER") | tail -1)"
                if [ -z "$BOOTNUM" ]; then
                  BOOTNUM="$($SUDO efibootmgr | sed -n 's/^Boot\\([0-9A-Fa-f]\\{4\\}\\).*netboot.xyz.*/\\1/p' | tail -1)"
                fi
                [ -n "$BOOTNUM" ] || { echo "ERROR: 创建 UEFI 启动项后未找到编号"; exit 27; }
                $SUDO efibootmgr --bootnext "$BOOTNUM"
                echo "BootNext=$BOOTNUM"
                echo "netboot.xyz 文件=/boot/efi/$FILE"
                __REBOOT_COMMAND__
                WANG_NETBOOT
                """.replace("__REBOOT_COMMAND__", rebootCommand);
    }
}
