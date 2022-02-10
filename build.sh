#!/bin/bash

# exit on any error
set -e

echo Clean dist directory
rm -rf dist/
mkdir dist

echo Buildung React frontend
cd hanko-account && npm install && npm run build-keycloak
cd ..

echo Copy account-hanko.html
cp hanko-account/dist/index.html hanko-plugin-keycloak-ejb/src/main/resources/theme/keycloak-with-hanko/account/account-hanko.ftl

echo Copy new javascript files
rm -f hanko-plugin-keycloak-ejb/src/main/resources/theme/keycloak-with-hanko/account/resources/js/*.js
cp hanko-account/dist/*.js hanko-plugin-keycloak-ejb/src/main/resources/theme/keycloak-with-hanko/account/resources/js/

echo Copy main.css
cp hanko-account/dist/main.css hanko-plugin-keycloak-ejb/src/main/resources/theme/keycloak-with-hanko/account/resources/css/main.css


echo Building ejb package
mvn validate
mvn package
