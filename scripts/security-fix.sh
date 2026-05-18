#!/bin/bash
# 瀹夊叏淇蹇€熸墽琛岃剼鏈?
# 鎵ц鍓嶈鍏堝湪BotFather鎾ら攢鏃oken骞惰幏鍙栨柊Token

set -e

echo "馃敀 King-Detective 瀹夊叏淇鑴氭湰"
echo "================================"
echo ""

# 妫€鏌ユ槸鍚﹀凡鎾ら攢Token
read -p "鉂?鏄惁宸插湪BotFather鎾ら攢鏃oken骞惰幏鍙栨柊Token? (y/n): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "鉂?璇峰厛鍦?@BotFather 鎾ら攢鏃oken锛?
    echo "   1. 鍙戦€?/mybots"
    echo "   2. 閫夋嫨Bot 鈫?API Token 鈫?Revoke current token"
    exit 1
fi

# 鑾峰彇鏂癟oken
echo ""
echo "馃摑 璇疯緭鍏ユ柊鐨凾elegram Bot Token:"
read -r NEW_TOKEN

if [ -z "$NEW_TOKEN" ]; then
    echo "鉂?Token涓嶈兘涓虹┖"
    exit 1
fi

# 鍒涘缓.env鏂囦欢
echo ""
echo "馃摑 鍒涘缓 .env 鏂囦欢..."
cat > .env << EOF
# Telegram Bot Configuration
TELEGRAM_BOT_TOKEN=$NEW_TOKEN

# Web Admin Credentials
ADMIN_USERNAME=${ADMIN_USERNAME:-${WEB_ACCOUNT:-admin}}
ADMIN_PASSWORD=${ADMIN_PASSWORD:-${WEB_PASSWORD:-admin123456}}

# OpenAI API (Optional)
OPENAI_API_KEY=${OPENAI_API_KEY:-}
EOF

echo "鉁?.env 鏂囦欢宸插垱寤?

# 楠岃瘉.gitignore
if ! grep -q ".env" .gitignore 2>/dev/null; then
    echo "鈿狅笍  娣诲姞 .env 鍒?.gitignore"
    echo ".env" >> .gitignore
fi

# 鎻愪氦鏇存敼
echo ""
read -p "鉂?鏄惁鎻愪氦鏇存敼鍒癎it? (y/n): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    git add .gitignore .env.example src/main/resources/application.yml
    git commit -m "security: use environment variables for sensitive data"
    echo "鉁?鏇存敼宸叉彁浜?
    
    read -p "鉂?鏄惁鎺ㄩ€佸埌杩滅▼浠撳簱? (y/n): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        git push
        echo "鉁?宸叉帹閫佸埌杩滅▼浠撳簱"
    fi
fi

# 閲嶅惎鏈嶅姟
echo ""
read -p "鉂?鏄惁閲嶅惎Docker鏈嶅姟? (y/n): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "馃攧 閲嶅惎鏈嶅姟..."
    docker-compose down
    docker-compose up -d
    echo "鉁?鏈嶅姟宸查噸鍚?
    
    echo ""
    echo "馃搵 鏌ョ湅鏃ュ織:"
    docker-compose logs -f --tail=50
fi

echo ""
echo "鉁?瀹夊叏淇瀹屾垚锛?
echo ""
echo "馃攳 涓嬩竴姝ワ細"
echo "1. 娴嬭瘯Bot鏄惁姝ｅ父 (鍙戦€?/start)"
echo "2. 鍦℅itHub鍏抽棴瀹夊叏璀︽姤"
echo "3. 妫€鏌ユ槸鍚﹂渶瑕佹竻鐞咷it鍘嗗彶"
