# OCI 救援与 netboot.xyz 集成路线

## 结论

netboot.xyz 可以作为 W-探长后续“救援中心”的一部分集成，但不能直接做成无保护的一键重装按钮。

原因是 netboot.xyz 的核心能力是通过 iPXE/PXE/UEFI 引导进入网络安装或救援菜单；而 OCI 普通实例并没有一个稳定、通用、可在 Web 面板里直接切换到任意 iPXE 网络启动的公开入口。对已经失联的机器，官方更可靠的救援路径是实例控制台、停止实例、拆下 boot volume、挂载到同可用域的第二台 Linux 实例上修复，再挂回原实例。

参考资料：

- netboot.xyz 官方说明：<https://netboot.xyz/docs>
- netboot.xyz iPXE 链式引导：<https://netboot.xyz/docs/booting/ipxe>
- netboot.xyz Quick Start：<https://netboot.xyz/docs/quick-start/>
- OCI Linux boot volume 恢复：<https://docs.oracle.com/en-us/iaas/Content/Compute/Tasks/recoveringlinuxbootvolume.htm>
- OCI Instance Console Connection CLI：<https://docs.oracle.com/iaas/tools/oci-cli/latest/oci_cli_docs/cmdref/compute/instance-console-connection/create.html>

## 推荐产品形态

新增“救援中心”页面和 TGBOT 菜单，不直接叫“一键重装”，而是分三种模式：

1. **轻量自救**
   - 适用于实例还能 SSH 登录。
   - 自动执行连通性检查、磁盘空间检查、cloud-init/ssh/防火墙/网卡配置检查。
   - 可一键重启 SSH、修复 authorized_keys、清理异常防火墙规则、生成诊断包。

2. **boot volume 救援**
   - 适用于实例 SSH 失联、系统可能损坏。
   - 通过 OCI API 引导用户停止实例、备份 boot volume、拆卷、挂载到救援机、运行修复脚本、卸载并挂回。
   - 每一步都要显示风险提示和二次确认，保留操作审计。

3. **netboot.xyz 引导实验区**
   - 适用于用户明确知道自己要网络安装/救援，并且机器还能修改启动项或能使用控制台。
   - 首期只提供脚本生成和人工确认，不自动覆盖启动盘。
   - 后续根据 AMD/ARM、BIOS/UEFI、Oracle Linux/Ubuntu 的实测结果，再决定是否开放自动化。

## 安全边界

- 所有会停止实例、拆卷、重启、修改 bootloader、清盘或重装系统的动作必须二次确认。
- 默认先创建 boot volume 备份，再执行破坏性动作。
- 所有动作写入操作审计，记录配置名、实例 OCID、区域、操作者、动作、参数摘要和结果。
- TGBOT 侧只提供状态查看和带确认的救援入口，不直接裸奔执行高危重装。

## 后续实施顺序

1. 新增后端 `RescueController`，提供救援能力探测、实例控制台连接信息、boot volume 恢复流程状态接口。
2. 新增 Vue `/dashboard/rescue` 救援中心页面，按实例展示“轻量自救 / 拆卷救援 / netboot 实验区”。
3. 新增服务器端救援脚本模板，先覆盖 SSH、防火墙、cloud-init、磁盘满、日志爆盘等常见失联原因。
4. 新增 TGBOT “救援中心”菜单，支持诊断、最近异常、救援流程说明和 Web 快捷入口。
5. 在测试机上分 AMD/ARM、Ubuntu/Oracle Linux 验证 netboot.xyz 可行路径，再决定是否开放自动引导按钮。
