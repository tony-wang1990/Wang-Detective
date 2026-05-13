# Wang-Detective Frontend

This directory is the new maintainable Vue frontend source for the UI rebuild.

Current scope:

- Native Vue login page.
- Native dashboard shell with sidebar, topbar, theme switch, and route outlet.
- Native home page skeleton that reads the existing `/api/sys/glance` and `/actuator/health` data.
- Route placeholders for the old pages so migration can continue one page at a time.

It intentionally builds to `src/main/resources/dist-next` first. The current production UI still serves `src/main/resources/dist` plus the incremental theme scripts, so this source tree can be improved and verified before it replaces the legacy bundle.

Commands:

```bash
cd frontend
npm install
npm run dev
npm run build
```

Migration plan:

1. Finish the native login, dashboard layout, and home route.
2. Move "新版功能" and "运维终端" from iframe/static HTML into Vue routes.
3. Rebuild configuration, task, log, system, and AI pages in the same shell.
4. Switch the Maven/Docker build to copy `dist-next` as the served frontend after route parity is verified.
