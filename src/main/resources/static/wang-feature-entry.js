(function () {
  const entries = [
    {
      id: 'wang-feature-center-entry',
      label: '新版功能',
      icon: '◆',
      href: '/wang-features.html',
      title: '新版功能'
    },
    {
      id: 'wang-ops-terminal-entry',
      label: '运维终端',
      icon: '⌁',
      href: '/ops-terminal.html',
      title: '运维终端'
    }
  ];

  function buildEntry(entry) {
    const item = document.createElement('li');
    item.id = entry.id;
    item.className = 'el-menu-item wang-extra-menu-item';
    item.setAttribute('role', 'menuitem');
    item.style.cssText = [
      'height:56px',
      'line-height:56px',
      'display:flex',
      'align-items:center',
      'gap:14px',
      'padding:0 20px',
      'cursor:pointer'
    ].join(';');
    item.innerHTML = [
      '<span class="wang-extra-icon" aria-hidden="true">',
      entry.icon,
      '</span>',
      '<span class="menu-text">',
      entry.label,
      '</span>'
    ].join('');
    item.addEventListener('click', function (event) {
      event.preventDefault();
      renderEmbeddedPage(entry);
    });
    return item;
  }

  function embeddedUrl(href) {
    return href + (href.includes('?') ? '&' : '?') + 'embedded=1';
  }

  function setMenuActive(activeId) {
    document.querySelectorAll('.sidebar-menu .el-menu-item').forEach(function (item) {
      item.classList.toggle('is-active', item.id === activeId);
    });
  }

  function restoreMainContent() {
    const main = document.querySelector('.el-main');
    if (!main) {
      return;
    }
    const panel = document.getElementById('wang-embedded-panel');
    if (panel) {
      panel.remove();
    }
    Array.from(main.children).forEach(function (child) {
      child.style.display = '';
    });
  }

  function renderEmbeddedPage(entry) {
    const main = document.querySelector('.el-main');
    if (!main) {
      window.location.href = entry.href;
      return;
    }

    Array.from(main.children).forEach(function (child) {
      if (child.id !== 'wang-embedded-panel') {
        child.style.display = 'none';
      }
    });

    let panel = document.getElementById('wang-embedded-panel');
    if (!panel) {
      panel = document.createElement('div');
      panel.id = 'wang-embedded-panel';
      main.appendChild(panel);
    }

    panel.style.cssText = [
      'display:block',
      'height:calc(100vh - 112px)',
      'min-height:680px',
      'background:#f6f8fb',
      'border-radius:8px',
      'overflow:hidden'
    ].join(';');

    panel.innerHTML = [
      '<iframe id="wang-embedded-frame" title="',
      entry.title,
      '" src="',
      embeddedUrl(entry.href),
      '" style="width:100%;height:100%;border:0;display:block;background:#f6f8fb"></iframe>'
    ].join('');

    const iframe = document.getElementById('wang-embedded-frame');
    iframe.addEventListener('load', function () {
      try {
        const token = sessionStorage.getItem('token');
        if (token) {
          iframe.contentWindow.sessionStorage.setItem('token', token);
        }
      } catch (error) {
        console.warn('Failed to sync embedded session:', error.message);
      }
    });

    setMenuActive(entry.id);
  }

  function injectFeatureEntries() {
    const menu = document.querySelector('.sidebar-menu');
    if (!menu || document.getElementById(entries[0].id)) {
      return;
    }

    entries.forEach(function (entry) {
      menu.appendChild(buildEntry(entry));
    });

    if (!document.getElementById('wang-feature-entry-style')) {
      const style = document.createElement('style');
      style.id = 'wang-feature-entry-style';
      style.textContent = [
        '.wang-extra-menu-item{color:#e8eefc!important;}',
        '.wang-extra-menu-item:hover{background:#315fc8!important;color:#fff!important;}',
        '.wang-extra-icon{width:18px;text-align:center;font-weight:700;color:#7dd3fc;}',
        '.sidebar.collapsed .wang-extra-menu-item{justify-content:center;padding:0!important;}',
        '.sidebar.collapsed .wang-extra-menu-item .menu-text{display:none;}',
        '.wang-extra-menu-item.is-active{background:#2f69dc!important;color:#fff!important;}'
      ].join('');
      document.head.appendChild(style);
    }

    menu.addEventListener('click', function (event) {
      if (!event.target.closest('.wang-extra-menu-item')) {
        restoreMainContent();
      }
    }, true);
  }

  function applyVersionInfo(versionInfo) {
    const currentVersion = versionInfo.currentVersion || 'dev';
    const latestVersion = versionInfo.latestVersion || currentVersion;
    localStorage.setItem('currentVersion', currentVersion);
    localStorage.setItem('latestVersion', latestVersion);

    document.querySelectorAll('button').forEach(function (button) {
      const text = button.textContent || '';
      if (!text.includes('新版本:')) {
        return;
      }
      if (currentVersion === latestVersion) {
        button.style.display = 'none';
      } else {
        button.style.display = '';
        button.textContent = '🔔 新版本:' + latestVersion;
      }
    });

    document.querySelectorAll('footer a').forEach(function (link) {
      if ((link.textContent || '').includes('Tony Wang')) {
        link.textContent = '© Tony Wang All Rights Reserved ' + currentVersion;
      }
    });
  }

  async function refreshVersionInfo() {
    const token = sessionStorage.getItem('token');
    if (!token) {
      return;
    }
    try {
      const response = await fetch('/api/v1/system/version-info', {
        headers: { Authorization: 'Bearer ' + token }
      });
      const json = await response.json();
      if (json && json.data) {
        applyVersionInfo(json.data);
      }
    } catch (error) {
      console.warn('Failed to refresh Wang-Detective version info:', error.message);
    }
  }

  const observer = new MutationObserver(injectFeatureEntries);
  observer.observe(document.documentElement, { childList: true, subtree: true });
  window.addEventListener('load', injectFeatureEntries);
  window.addEventListener('load', refreshVersionInfo);
  document.addEventListener('DOMContentLoaded', injectFeatureEntries);
  document.addEventListener('DOMContentLoaded', refreshVersionInfo);
  setTimeout(injectFeatureEntries, 1000);
  setTimeout(refreshVersionInfo, 1500);
})();
