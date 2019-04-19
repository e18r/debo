#! /bin/bash

cp src/main/resources/db.properties.default src/main/resources/db.properties
cp src/main/resources/auth.properties.default src/main/resources/auth.properties
sudo pacman -S maven postgresql # install java 8
sudo -iu postgres
initdb --locale en_US.UTF-8 -D /var/lib/postgres/data
exit
sudo systemctl start postgresql.service
sudo -iu postgres
createuser --interactive # user debo
createdb -O debo debo
psql # \password debo
exit
psql -d debo -f database/2018100401-initial.sql -U debo
emacs src/main/resources/auth.properties # set a Google project
mvn compile
mvn test
mvn install
mvn exec:java
