java -Xms256m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -Dspring.profiles.active=dev -DSERVER_PORT=9101 -jar .\target\etl-service-0.0.1-SNAPSHOT.jar