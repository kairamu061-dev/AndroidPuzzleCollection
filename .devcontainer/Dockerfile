FROM debian:bookworm-slim

ENV LANG=C.UTF-8
ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
ENV ANDROID_HOME=/opt/android-sdk
ENV PATH=${PATH}:${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools

# slim strips /usr/share/man; the JDK's postinst fails without this directory
RUN mkdir -p /usr/share/man/man1 \
    && apt-get update && apt-get install -y \
    git \
    curl \
    wget \
    procps \
    sudo \
    unzip \
    zip \
    openjdk-17-jdk-headless \
    && rm -rf /var/lib/apt/lists/*

RUN curl -fsSL https://deb.nodesource.com/setup_lts.x | bash - \
    && apt-get install -y nodejs \
    && rm -rf /var/lib/apt/lists/*

# gh is required by the verify-request skill
RUN curl -fsSL https://cli.github.com/packages/githubcli-archive-keyring.gpg \
        -o /usr/share/keyrings/githubcli-archive-keyring.gpg \
    && echo "deb [arch=amd64 signed-by=/usr/share/keyrings/githubcli-archive-keyring.gpg] https://cli.github.com/packages stable main" \
        > /etc/apt/sources.list.d/github-cli.list \
    && apt-get update && apt-get install -y gh \
    && rm -rf /var/lib/apt/lists/*

RUN npm install -g @anthropic-ai/claude-code

RUN useradd -m -s /bin/bash claudeuser \
    && echo "claudeuser ALL=(ALL) NOPASSWD:ALL" >> /etc/sudoers

# Only the SDK manager and adb live here. Platform and build-tools versions belong to the
# Gradle build, which installs them on demand: this needs the licenses accepted and
# ANDROID_HOME writable by claudeuser. Gradle itself comes from the wrapper.
ARG CMDLINE_TOOLS_ZIP=commandlinetools-linux-15859902_latest.zip
ARG CMDLINE_TOOLS_SHA256=4e4c464f145a7512b57d088ac6c278c03c9eea610886b35a5e0804e74eedf583
RUN curl -fsSL "https://dl.google.com/android/repository/${CMDLINE_TOOLS_ZIP}" -o /tmp/tools.zip \
    && echo "${CMDLINE_TOOLS_SHA256}  /tmp/tools.zip" | sha256sum -c - \
    && mkdir -p "${ANDROID_HOME}/cmdline-tools" \
    && unzip -q /tmp/tools.zip -d "${ANDROID_HOME}/cmdline-tools" \
    && mv "${ANDROID_HOME}/cmdline-tools/cmdline-tools" "${ANDROID_HOME}/cmdline-tools/latest" \
    && rm /tmp/tools.zip \
    && yes | sdkmanager --licenses > /dev/null \
    && sdkmanager "platform-tools" > /dev/null \
    && chown -R claudeuser:claudeuser "${ANDROID_HOME}"

# A named volume inherits ownership from these directories; without them it mounts as root
RUN mkdir -p /home/claudeuser/.gradle /home/claudeuser/.config/gh \
    && chown -R claudeuser:claudeuser /home/claudeuser/.gradle /home/claudeuser/.config
