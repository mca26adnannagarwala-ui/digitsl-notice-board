FROM eclipse-temurin:17-jdk-jammy

RUN apt-get update && apt-get install -y \
    xvfb x11vnc fluxbox novnc websockify \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY . /app

RUN mkdir -p out && javac -cp ".:lib/*:*" -d out $(find . -name "*.java")

CMD bash -c "\
  Xvfb :1 -screen 0 1280x800x16 & \
  export DISPLAY=:1 && \
  fluxbox & \
  x11vnc -display :1 -nopw -listen localhost -xkb -forever & \
  websockify --web=/usr/share/novnc/ ${PORT:-10000} localhost:5900 & \
  java -cp 'out:lib/*:*' LoginFrame \
"