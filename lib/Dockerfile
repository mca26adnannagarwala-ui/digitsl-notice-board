FROM eclipse-temurin:17-jdk-jammy

ENV DEBIAN_FRONTEND=noninteractive
WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        xvfb \
        x11vnc \
        fluxbox \
        novnc \
        websockify \
        xauth \
    && rm -rf /var/lib/apt/lists/*

COPY src/ ./src/
COPY lib/mysql-connector-j-26.10.0.jar ./lib/mysql-connector-j-26.10.0.jar

RUN mkdir -p out \
    && javac -encoding UTF-8 \
        -cp lib/mysql-connector-j-26.10.0.jar \
        -d out \
        src/*.java

EXPOSE 10000

CMD ["bash", "-lc", "set -euo pipefail; : \"${VNC_PASSWORD:?Set VNC_PASSWORD (minimum 8 characters)}\"; if [ \"${#VNC_PASSWORD}\" -lt 8 ]; then echo 'VNC_PASSWORD must contain at least 8 characters.' >&2; exit 1; fi; x11vnc -storepasswd \"$VNC_PASSWORD\" /tmp/x11vnc.pass >/dev/null; unset VNC_PASSWORD; Xvfb :99 -screen 0 1280x800x24 -nolisten tcp >/tmp/xvfb.log 2>&1 & export DISPLAY=:99; fluxbox >/tmp/fluxbox.log 2>&1 & x11vnc -display :99 -localhost -forever -shared -rfbport 5900 -rfbauth /tmp/x11vnc.pass >/tmp/x11vnc.log 2>&1 & websockify --web=/usr/share/novnc \"${PORT:-10000}\" 127.0.0.1:5900 >/tmp/websockify.log 2>&1 & exec java -Djava.awt.headless=false -cp out:lib/mysql-connector-j-26.10.0.jar LoginFrame"]
