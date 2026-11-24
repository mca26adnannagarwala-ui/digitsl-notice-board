FROM eclipse-temurin:17-jdk-jammy

RUN apt-get update && apt-get install -y \
    xvfb x11vnc fluxbox novnc websockify \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY . /app

RUN mkdir -p out && javac -cp ".:lib/*:*" -d out $(find . -name "*.java")

CMD bash -c "\
  mkdir -p /tmp/.X11-unix && chmod 1777 /tmp/.X11-unix && \
  rm -f /tmp/.X1-lock && \
  Xvfb :1 -screen 0 1280x800x24 -ac +extension GLX +render -noreset & \
  sleep 2 && \
  export DISPLAY=:1 && \
  fluxbox & \
  sleep 1 && \
  x11vnc -display :1 -nopw -listen 0.0.0.0 -xkb -forever -shared & \
  sleep 1 && \
  websockify --web=/usr/share/novnc/ ${PORT:-10000} localhost:5900 & \
  java -cp 'out:lib/*:*' LoginFrame \
"