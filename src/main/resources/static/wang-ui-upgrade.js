(function () {
  const REFRESH_MS = 30000;

  function token() {
    return sessionStorage.getItem('token') || '';
  }

  function isDashboard() {
    return window.location.pathname.indexOf('/dashboard') === 0;
  }

  function isHomeDashboard() {
    const path = window.location.pathname;
    return path === '/dashboard' || path === '/dashboard/' || path === '/dashboard/home';
  }

  async function fetchJson(url) {
    const headers = {};
    const currentToken = token();
    if (currentToken) {
      headers.Authorization = 'Bearer ' + currentToken;
    }
    const response = await fetch(url, { headers });
    if (!response.ok) {
      throw new Error(url + ' ' + response.status);
    }
    return response.json();
  }

  function formatBytes(value) {
    const number = Number(value || 0);
    if (!number) {
      return '0 B';
    }
    const units = ['B', 'KB', 'MB', 'GB', 'TB'];
    let size = number;
    let index = 0;
    while (size >= 1024 && index < units.length - 1) {
      size /= 1024;
      index += 1;
    }
    return size.toFixed(index === 0 ? 0 : 1) + ' ' + units[index];
  }

  function formatUptime(seconds) {
    const value = Number(seconds || 0);
    if (value < 60) {
      return value + ' 秒';
    }
    const minutes = Math.floor(value / 60);
    if (minutes < 60) {
      return minutes + ' 分钟';
    }
    const hours = Math.floor(minutes / 60);
    if (hours < 24) {
      return hours + ' 小时';
    }
    return Math.floor(hours / 24) + ' 天';
  }

  function ensureLoginCopy() {
    if (window.location.pathname !== '/login') {
      return;
    }
    document.title = 'W-探长 登录';
    const button = document.querySelector('.login-button');
    if (button && button.textContent.trim() !== '登录控制台') {
      button.textContent = '登录控制台';
    }
    const title = document.querySelector('.login-title');
    if (title && title.textContent.trim() !== '登录控制台') {
      title.textContent = '登录控制台';
    }
    const subtitle = document.querySelector('.login-subtitle');
    if (subtitle && subtitle.textContent.trim() !== 'OCI 资源与运维管理') {
      subtitle.textContent = 'OCI 资源与运维管理';
    }
  }

  function ensureLoginRedirect() {
    const hasToken = Boolean(token());
    const loginVisible = Boolean(document.querySelector('.login-container'));
    const dashboardVisible = Boolean(document.querySelector('.dashboard-container'));

    if (hasToken && loginVisible && !dashboardVisible) {
      const target = '/dashboard/home';
      if (window.location.pathname !== target) {
        window.location.replace(target);
      } else if (!sessionStorage.getItem('wang_dashboard_reload_once')) {
        sessionStorage.setItem('wang_dashboard_reload_once', '1');
        window.location.reload();
      }
    }
  }

  function ensureTopbar() {
    if (!isDashboard()) {
      return;
    }
    const header = document.querySelector('.dashboard-container .header');
    if (!header || header.querySelector('.wang-topbar-left')) {
      return;
    }

    const left = document.createElement('div');
    left.className = 'wang-topbar-left';
    left.innerHTML = [
      '<button type="button" class="wang-menu-button" aria-label="menu">☰</button>',
      '<label class="wang-search-box">',
      '<span aria-hidden="true">⌕</span>',
      '<input readonly value="" placeholder="搜索资源、任务、日志等...">',
      '<kbd>⌘K</kbd>',
      '</label>',
      '<div class="wang-health-pill"><span></span><b>系统健康</b><em id="wangTopHealth">检测中</em></div>',
      '<div class="wang-version-pill"><b>版本</b><em id="wangTopVersion">main</em></div>'
    ].join('');

    header.insertBefore(left, header.firstChild);
    const menuButton = left.querySelector('.wang-menu-button');
    menuButton.addEventListener('click', function () {
      const toggle = document.querySelector('.dashboard-container .toggle-button');
      if (toggle) {
        toggle.click();
      }
    });
    refreshTopbar();
  }

  async function refreshTopbar() {
    if (!isDashboard()) {
      return;
    }
    const healthEl = document.getElementById('wangTopHealth');
    const versionEl = document.getElementById('wangTopVersion');
    try {
      const health = await fetchJson('/actuator/health');
      if (healthEl) {
        healthEl.textContent = health.status === 'UP' ? '正常' : '异常';
        healthEl.className = health.status === 'UP' ? 'ok' : 'warn';
      }
      if (versionEl) {
        versionEl.textContent = health.version || localStorage.getItem('currentVersion') || 'main';
      }
    } catch (error) {
      if (healthEl) {
        healthEl.textContent = '未知';
        healthEl.className = 'warn';
      }
    }
  }

  function ensureSidebarInfo() {
    if (!isDashboard()) {
      return;
    }
    const sidebar = document.querySelector('.dashboard-container .sidebar');
    if (!sidebar || sidebar.querySelector('.wang-sidebar-meta')) {
      return;
    }

    const card = document.createElement('div');
    card.className = 'wang-sidebar-meta';
    card.innerHTML = [
      '<div class="wang-meta-label">API 网关地址</div>',
      '<div class="wang-meta-host">',
      window.location.host,
      '<button type="button" title="复制地址">⧉</button>',
      '</div>',
      '<div class="wang-meta-row"><span>环境</span><b>生产环境</b></div>',
      '<div class="wang-meta-row"><span>面板</span><b>W-探长</b></div>'
    ].join('');

    const button = card.querySelector('button');
    button.addEventListener('click', function () {
      navigator.clipboard && navigator.clipboard.writeText(window.location.origin);
    });
    sidebar.appendChild(card);
  }

  function makeDiagRow(title, detail, state) {
    const row = document.createElement('div');
    row.className = 'wang-diag-row ' + (state || 'ok');
    row.innerHTML = [
      '<span class="wang-diag-icon"></span>',
      '<div><b>',
      title,
      '</b><small>',
      detail || '',
      '</small></div>',
      '<em>',
      state === 'warn' ? '警告' : state === 'error' ? '异常' : '正常',
      '</em>'
    ].join('');
    return row;
  }

  function ensureDashboardGrid() {
    if (!isHomeDashboard()) {
      return;
    }
    const map = document.getElementById('map');
    if (!map || document.querySelector('.wang-map-grid')) {
      return;
    }

    const grid = document.createElement('section');
    grid.className = 'wang-map-grid';

    const mapCard = document.createElement('div');
    mapCard.className = 'wang-map-card';
    const mapHead = document.createElement('div');
    mapHead.className = 'wang-card-head';
    mapHead.innerHTML = '<h2>资源分布地图</h2><div><button type="button">全屏查看</button><button type="button" id="wangMapResize">刷新</button></div>';

    const diagCard = document.createElement('aside');
    diagCard.className = 'wang-diagnostics-card';
    diagCard.innerHTML = [
      '<div class="wang-card-head">',
      '<h2>系统诊断</h2>',
      '<span id="wangDiagTime">刷新中</span>',
      '</div>',
      '<div id="wangDiagRows" class="wang-diag-rows"></div>',
      '<a href="/wang-features.html?embedded=1" class="wang-diag-link">查看完整诊断报告 ›</a>'
    ].join('');

    map.parentNode.insertBefore(grid, map);
    mapCard.appendChild(mapHead);
    mapCard.appendChild(map);
    grid.appendChild(mapCard);
    grid.appendChild(diagCard);

    const resize = function () {
      window.dispatchEvent(new Event('resize'));
    };
    const resizeButton = document.getElementById('wangMapResize');
    if (resizeButton) {
      resizeButton.addEventListener('click', resize);
    }
    setTimeout(resize, 300);
    refreshDiagnostics();
  }

  async function refreshDiagnostics() {
    const rows = document.getElementById('wangDiagRows');
    const time = document.getElementById('wangDiagTime');
    if (!rows) {
      return;
    }

    try {
      const health = await fetchJson('/actuator/health');
      rows.innerHTML = '';
      rows.appendChild(makeDiagRow('API 网关连通性', window.location.origin + '/actuator/health', health.status === 'UP' ? 'ok' : 'error'));
      rows.appendChild(makeDiagRow('数据库连接', health.databaseConnectivity ? '健康检查通过' : '连接状态未知', health.databaseConnectivity ? 'ok' : 'warn'));
      rows.appendChild(makeDiagRow('内存使用', formatBytes(health.usedMemoryBytes) + ' / ' + formatBytes(health.maxMemoryBytes), health.memoryStatus ? 'ok' : 'warn'));
      rows.appendChild(makeDiagRow('运行版本', health.version || localStorage.getItem('currentVersion') || 'main', 'ok'));
      rows.appendChild(makeDiagRow('系统运行', formatUptime(health.uptimeSeconds), 'ok'));
      if (time) {
        time.textContent = new Date().toLocaleTimeString() + ' 刷新';
      }
    } catch (error) {
      rows.innerHTML = '';
      rows.appendChild(makeDiagRow('系统诊断', '暂时无法读取健康检查', 'warn'));
      if (time) {
        time.textContent = '读取失败';
      }
    }
  }

  function ensureChartTitle() {
    if (!isHomeDashboard()) {
      return;
    }
    const chartContainer = document.querySelector('.chart-container');
    if (!chartContainer || document.querySelector('.wang-chart-block')) {
      return;
    }
    const block = document.createElement('section');
    block.className = 'wang-chart-block';
    const head = document.createElement('div');
    head.className = 'wang-card-head';
    head.innerHTML = '<h2>资源使用情况</h2><span>数据每 30 秒自动刷新</span>';
    chartContainer.parentNode.insertBefore(block, chartContainer);
    block.appendChild(head);
    block.appendChild(chartContainer);
    setTimeout(function () {
      window.dispatchEvent(new Event('resize'));
    }, 300);
  }

  function apply() {
    ensureLoginRedirect();
    ensureLoginCopy();
    ensureTopbar();
    ensureSidebarInfo();
    ensureDashboardGrid();
    ensureChartTitle();
  }

  let scheduled = false;
  function scheduleApply() {
    if (scheduled) {
      return;
    }
    scheduled = true;
    window.requestAnimationFrame(function () {
      scheduled = false;
      apply();
    });
  }

  const observer = new MutationObserver(scheduleApply);
  observer.observe(document.documentElement, { childList: true, subtree: true });
  window.addEventListener('load', scheduleApply);
  document.addEventListener('DOMContentLoaded', scheduleApply);
  window.addEventListener('popstate', scheduleApply);
  setInterval(function () {
    refreshTopbar();
    refreshDiagnostics();
    scheduleApply();
  }, REFRESH_MS);
  setTimeout(scheduleApply, 500);
  setTimeout(scheduleApply, 1500);
})();
