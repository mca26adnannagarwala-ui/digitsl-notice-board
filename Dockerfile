FROM eclipse-temurin:17-jdk-jammy

RUN apt-get update && apt-get install -y \
    xvfb x11vnc fluxbox novnc websockify x11-xserver-utils \
    && rm -rf /var/lib/apt/lists/* \
    && printf '<!DOCTYPE html><html><head><meta http-equiv="refresh" content="0;url=/vnc.html?autoconnect=true&resize=remote"></head></html>' > /usr/share/novnc/index.html

ENV DISPLAY=:1

WORKDIR /app
COPY . /app

RUN mkdir -p out && javac -cp ".:lib/*:*" -d out $(find . -name "*.java")

CMD bash -c "\
  mkdir -p /tmp/.X11-unix && chmod 1777 /tmp/.X11-unix && \
  rm -f /tmp/.X1-lock && \
  Xvfb :1 -screen 0 1280x800x24 -ac +extension GLX +render -noreset & \
  sleep 2 && \
  DISPLAY=:1 xsetroot -solid '#1e293b' && \
  DISPLAY=:1 fluxbox & \
  sleep 1 && \
  x11vnc -display :1 -nopw -listen 0.0.0.0 -xkb -forever -shared -noxdamage & \
  sleep 1 && \
  (while true; do \
    MAIN_CLASS=\$(find out -name 'LoginFrame.class' | head -n 1 | sed 's|^out/||;s|\.class\$||;s|/|.|g'); \
    DISPLAY=:1 java -Djava.awt.headless=false -Dsun.java2d.xrender=false -cp 'out:lib/*:*' \"\$MAIN_CLASS\"; \
    sleep 3; \
  done) & \
  exec websockify --web=/usr/share/novnc/ 0.0.0.0:${PORT:-10000} localhost:5900 \
"