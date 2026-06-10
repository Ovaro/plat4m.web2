docker stop plat4m
docker rm plat4m
docker image rm plat4m
cd ..
./gradlew -Pprod bootJar jibDockerBuild
cd scripts
./localdocker.sh
