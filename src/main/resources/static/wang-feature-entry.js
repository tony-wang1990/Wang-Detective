(function () {
  const entries = [
    {
      id: 'wang-feature-center-entry',
      label: '新版功能',
      icon: '◆',
      href: '/wang-features.html'
    },
    {
      id: 'wang-ops-terminal-entry',
      label: '运维终端',
      icon: '⌁',
      href: '/ops-terminal.html'
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
    item.addEventListener('click', function () {
      window.location.href = entry.href;
    });
    return item;
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
        '.sidebar.collapsed .wang-extra-menu-item .menu-text{display:none;}'
      ].join('');
      document.head.appendChild(style);
    }
  }

  const observer = new MutationObserver(injectFeatureEntries);
  observer.observe(document.documentElement, { childList: true, subtree: true });
  window.addEventListener('load', injectFeatureEntries);
  document.addEventListener('DOMContentLoaded', injectFeatureEntries);
  setTimeout(injectFeatureEntries, 1000);
})();
