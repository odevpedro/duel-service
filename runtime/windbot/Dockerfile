FROM mono:6.12.0.182

WORKDIR /opt/windbot
COPY .local-runtime/windbot-release/WindBot/ ./

EXPOSE 2399

ENTRYPOINT ["mono", "WindBot.exe"]
CMD ["ServerMode=True", "ServerPort=2399", "DbPath=/cards/cards.cdb", "Chat=False"]
