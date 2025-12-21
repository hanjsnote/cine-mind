# CineMind EC2 Setup / Recovery Guide

## 0) Quick Start (재가동 요약)
1. SSH 접속
    - `ssh -i <key.pem> ubuntu@<EC2_PUBLIC_IP>`
2. Redis Stack 확인/재기동
    - `docker ps | grep redis-stack || docker start redis-stack`
3. Nginx 확인
    - `sudo systemctl status nginx`
4. 백엔드 실행 확인
    - (systemd 사용 시) `sudo systemctl restart cinemind-backend`
    - (직접 실행 시) `java -jar /home/ubuntu/cine-mind-0.0.1-SNAPSHOT.jar`
5. 접속 확인
    - `curl -I https://cinemind.me`
    - `curl -I https://cinemind.me/api/health` (있으면)

---

## 1) Architecture
- Domain: `cinemind.me`, `www.cinemind.me`
- Nginx (443 SSL) -> Frontend static: `/var/www/cinemind-frontend`
- Nginx `/api/` -> Spring Boot: `http://127.0.0.1:8080/api/`
- Redis: Docker Redis Stack
    - port: 6379, 8001
- DB: AWS RDS PostgreSQL (+pgvector)

---

## 2) Server Spec / Versions
- OS: Ubuntu XX.XX LTS
- Java: `java -version` -> (기록)
- Nginx: `nginx -v` -> (기록)
- Docker: `docker -v` -> (기록)
- Redis Stack Image: `redis/redis-stack-server:latest` (또는 고정 태그로 변경 추천)

---

## 3) Required Ports / Security Group
- 22: SSH
- 80: HTTP (certbot/redirect)
- 443: HTTPS
- 8080: (외부 오픈 X, 내부만 사용 권장)

---

## 4) Install Steps (Fresh EC2)
### 4.1 기본 패키지
```bash
sudo apt update
sudo apt install -y nginx
# docker 설치 (공식 방식 or apt)

sudo docker run -d --name redis-stack \
  -p 6379:6379 -p 8001:8001 \
  redis/redis-stack-server:latest
sudo docker update --restart unless-stopped redis-stack