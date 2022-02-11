#!/bin/bash

# exit on any error
set -e

echo Installing java-sdk into local maven repo
mvn validate

echo Compiling java sources
mvn clean compile

echo Building React frontend
cd hanko-account && npm install && npm run build-keycloak
cd ..

echo Copy account-hanko.html
cp hanko-account/dist/index.html hanko-plugin-keycloak-ejb/target/classes/theme/keycloak-with-hanko/account/account-hanko.ftl
cp hanko-account/dist/index.html hanko-plugin-keycloak-ejb/target/classes/theme/kvsh/account/account-hanko.ftl

echo Copy new javascript files
cp hanko-account/dist/*.js hanko-plugin-keycloak-ejb/target/classes/theme/keycloak-with-hanko/account/resources/js/
mkdir -p hanko-plugin-keycloak-ejb/target/classes/theme/kvsh/account/resources/js
cp hanko-account/dist/*.js hanko-plugin-keycloak-ejb/target/classes/theme/kvsh/account/resources/js/

echo Copy main.css
cp hanko-account/dist/main.css hanko-plugin-keycloak-ejb/target/classes/theme/keycloak-with-hanko/account/resources/css/main.css

echo Copy kvsh.css
cp hanko-account/dist/kvsh.css hanko-plugin-keycloak-ejb/target/classes/theme/kvsh/account/resources/css/main.css

echo Packaging plugin ear
mvn package
