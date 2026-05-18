#!/bin/bash
#
# King-Detective v4.1.1 涓€閿崌绾ц剼鏈?
# 鍔熻兘锛氬畨鍏ㄥ崌绾у埌v4.1.1锛屼繚鐣欐墍鏈夐厤缃拰鏁版嵁
# 浣滆€咃細Antigravity AI
# 

set -e

# 棰滆壊瀹氫箟
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 鎵撳嵃甯﹂鑹茬殑娑堟伅
print_info() {
    echo -e "${BLUE}鈩癸笍  $1${NC}"
}

print_success() {
    echo -e "${GREEN}鉁?$1${NC}"
}

print_warning() {
    echo -e "${YELLOW}鈿狅笍  $1${NC}"
}

print_error() {
    echo -e "${RED}鉂?$1${NC}"
}

print_header() {
    echo ""
    echo -e "${BLUE}鈹佲攣鈹佲攣鈹佲攣鈹佲攣鈹佲攣鈹佲攣鈹佲攣鈹佲攣鈹佲攣鈹佲攣鈹佲攣鈹佲攣鈹佲攣鈹佲攣鈹佲攣鈹佲攣鈹佲攣鈹佲攣鈹佲攣鈹佲攣${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}鈹佲攣鈹佲攣鈹佲攣鈹佲攣鈹佲攣鈹佲攣鈹佲攣鈹佲攣鈹佲攣鈹佲攣鈹佲攣鈹佲攣鈹佲攣鈹佲攣鈹佲攣鈹佲攣鈹佲攣鈹佲攣鈹佲攣鈹佲攣${NC}"
    echo ""
}

# 妫€鏌ユ槸鍚﹀湪姝ｇ‘鐨勭洰褰?
check_directory() {
    if [ ! -f "docker-compose.yml" ] || [ ! -f "pom.xml" ]; then
        print_error "璇峰湪king-detective椤圭洰鏍圭洰褰曚笅鎵ц姝よ剼鏈紒"
        print_info "褰撳墠鐩綍: $(pwd)"
        exit 1
    fi
}

# 妫€鏌ocker compose鍛戒护
check_docker_compose() {
    if command -v docker &> /dev/null && docker compose version &> /dev/null; then
        COMPOSE_CMD="docker compose"
        print_success "妫€娴嬪埌 docker compose V2"
    elif command -v docker-compose &> /dev/null; then
        COMPOSE_CMD="docker-compose"
        print_warning "妫€娴嬪埌 docker-compose V1锛屽缓璁崌绾у埌V2"
    else
        print_error "鏈壘鍒癲ocker compose鍛戒护锛?
        exit 1
    fi
}

# 涓诲嚱鏁?
main() {
    print_header "馃殌 King-Detective v4.1.1 鍗囩骇鑴氭湰"
    
    print_info "姝よ剼鏈皢瀹夊叏鍗囩骇鍒皏4.1.1鐗堟湰"
    print_info "鎵€鏈夐厤缃拰鏁版嵁灏嗚淇濈暀锛?
    echo ""
    
    # 纭缁х画
    read -p "鏄惁缁х画鍗囩骇锛?y/n): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        print_warning "鍗囩骇宸插彇娑?
        exit 0
    fi
    
    # Step 0: 鐜妫€鏌?
    print_header "[0/8] 鐜妫€鏌?
    check_directory
    check_docker_compose
    
    # Step 1: 澶囦唤閰嶇疆鏂囦欢
    print_header "[1/8] 澶囦唤閰嶇疆鏂囦欢"
    BACKUP_SUFFIX=$(date +%Y%m%d_%H%M%S)
    
    if [ -f "docker-compose.yml" ]; then
        cp docker-compose.yml docker-compose.yml.backup.$BACKUP_SUFFIX
        print_success "宸插浠?docker-compose.yml"
    fi
    
    if [ -f "application.yml" ]; then
        cp application.yml application.yml.backup.$BACKUP_SUFFIX
        print_success "宸插浠?application.yml"
    fi
    
    if [ -f "king-detective.db" ]; then
        cp king-detective.db king-detective.db.backup.$BACKUP_SUFFIX
        print_success "宸插浠?king-detective.db"
    fi
    
    print_info "澶囦唤鏂囦欢鍚庣紑: $BACKUP_SUFFIX"
    
    # Step 2: 鎷夊彇鏈€鏂颁唬鐮?
    print_header "[2/8] 鎷夊彇v4.1.1浠ｇ爜"
    
    # 淇濆瓨鏈湴淇敼
    if ! git diff-index --quiet HEAD --; then
        print_warning "妫€娴嬪埌鏈湴淇敼锛屾鍦ㄦ殏瀛?.."
        git stash
        STASHED=true
    else
        STASHED=false
    fi
    
    # 鎷夊彇浠ｇ爜
    git fetch origin
    git checkout main
    git pull origin main
    
    print_success "浠ｇ爜宸叉洿鏂板埌鏈€鏂扮増鏈?
    
    # Step 3: 妫€鏌ラ厤缃枃浠?
    print_header "[3/8] 妫€鏌ラ厤缃枃浠?
    
    if [ ! -f "application.yml" ]; then
        print_warning "application.yml涓嶅瓨鍦紝浠庡浠芥仮澶?.."
        if [ -f "application.yml.backup.$BACKUP_SUFFIX" ]; then
            cp application.yml.backup.$BACKUP_SUFFIX application.yml
            print_success "宸叉仮澶?application.yml"
        else
            print_error "鏈壘鍒板浠芥枃浠讹紒璇锋墜鍔ㄦ鏌ラ厤缃?
        fi
    else
        print_success "application.yml 瀛樺湪"
    fi
    
    # 楠岃瘉閰嶇疆鍐呭
    if grep -q "account:" application.yml && grep -q "password:" application.yml; then
        print_success "閰嶇疆鏂囦欢鍖呭惈璐︽埛淇℃伅"
    else
        print_warning "閰嶇疆鏂囦欢鍙兘涓嶅畬鏁达紝璇锋鏌ワ紒"
    fi
    
    # Step 4: 淇trigger鏂囦欢
    print_header "[4/8] 淇trigger鏂囦欢"
    
    if [ -d "update_version_trigger.flag" ]; then
        print_warning "trigger鏄洰褰曪紝姝ｅ湪淇..."
        rm -rf update_version_trigger.flag
    fi
    
    touch update_version_trigger.flag
    chmod 666 update_version_trigger.flag
    print_success "trigger鏂囦欢宸蹭慨澶?
    
    # Step 5: 鍋滄鏃atcher
    print_header "[5/8] 鍋滄鏃atcher瀹瑰櫒"
    
    $COMPOSE_CMD stop watcher 2>/dev/null || true
    $COMPOSE_CMD rm -f watcher 2>/dev/null || true
    
    # 鍒犻櫎鏃ч暅鍍?
    OLD_IMAGE=$(docker images | grep "oci-helper-watcher" | awk '{print $3}' | head -n 1)
    if [ -n "$OLD_IMAGE" ]; then
        print_info "鍒犻櫎鏃atcher闀滃儚..."
        docker rmi -f $OLD_IMAGE 2>/dev/null || true
    fi
    
    print_success "鏃atcher宸插仠姝?
    
    # Step 6: 鎷夊彇鏂伴暅鍍?
    print_header "[6/8] 鎷夊彇鏈€鏂癉ocker闀滃儚"
    
    print_info "姝ｅ湪鎷夊彇 ghcr.io/tony-wang1990/king-detective:main..."
    $COMPOSE_CMD pull king-detective
    
    print_info "姝ｅ湪鎷夊彇 alpine:latest (鐢ㄤ簬watcher)..."
    docker pull alpine:latest
    
    print_success "闀滃儚鎷夊彇瀹屾垚"
    
    # Step 7: 閲嶅惎鏈嶅姟
    print_header "[7/8] 閲嶅惎鎵€鏈夋湇鍔?
    
    $COMPOSE_CMD up -d
    
    print_info "绛夊緟瀹瑰櫒鍚姩..."
    sleep 8
    
    # Step 8: 楠岃瘉閮ㄧ讲
    print_header "[8/8] 楠岃瘉閮ㄧ讲"
    
    echo ""
    print_info "瀹瑰櫒鐘舵€侊細"
    $COMPOSE_CMD ps
    
    echo ""
    print_info "Watcher鏃ュ織锛堟渶杩?琛岋級锛?
    docker logs king-detective-watcher --tail 5 2>&1 || print_warning "Watcher鏃ュ織鏆傛椂鏃犳硶鑾峰彇"
    
    # 鏈€缁堟€荤粨
    print_header "鉁?鍗囩骇瀹屾垚锛?
    
    echo ""
    print_success "King-Detective宸插崌绾у埌v4.1.1"
    echo ""
    
    print_info "馃搵 楠岃瘉娓呭崟锛?
    echo "  1. 鍦═elegram Bot涓婂彂閫?/start 鏌ョ湅鏂扮殑4鍒楀竷灞€"
    echo "  2. 鐧诲綍Web闈㈡澘楠岃瘉閰嶇疆鏈涪澶?
    echo "  3. 鐐瑰嚮'鐗堟湰淇℃伅'鏌ョ湅褰撳墠鐗堟湰"
    echo "  4. 娴嬭瘯'鏇存柊鍒版渶鏂扮増鏈?鎸夐挳锛堣嚜鍔ㄦ洿鏂板姛鑳斤級"
    echo ""
    
    print_info "馃搨 澶囦唤鏂囦欢浣嶇疆锛?
    echo "  - docker-compose.yml.backup.$BACKUP_SUFFIX"
    echo "  - application.yml.backup.$BACKUP_SUFFIX"
    echo "  - king-detective.db.backup.$BACKUP_SUFFIX"
    echo ""
    
    print_info "馃摉 鏌ョ湅鏃ュ織锛?
    echo "  docker logs -f king-detective-watcher  # Watcher鏃ュ織"
    echo "  docker logs -f king-detective          # 涓绘湇鍔℃棩蹇?
    echo ""
    
    print_info "馃攧 浠ュ悗鐨勬洿鏂帮細"
    echo "  浠巚4.1.1寮€濮嬶紝鍙互鐩存帴鍦˙ot涓婁竴閿洿鏂帮紝鏃犻渶鎵嬪姩鎿嶄綔锛?
    echo ""
    
    # 鎭㈠鏆傚瓨鐨勪慨鏀?
    if [ "$STASHED" = true ]; then
        print_warning "妫€娴嬪埌涔嬪墠鏆傚瓨鐨勪慨鏀癸紝鏄惁鎭㈠锛?y/n)"
        read -p "> " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            git stash pop
            print_success "宸叉仮澶嶆湰鍦颁慨鏀?
        fi
    fi
    
    print_success "鍗囩骇鑴氭湰鎵ц瀹屾瘯锛?
}

# 鎵ц涓诲嚱鏁?
main

exit 0
