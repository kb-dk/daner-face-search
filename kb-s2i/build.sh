#!/usr/bin/env bash

cd /tmp/src

cp -rp -- /tmp/src/target/daner-face-search-*.war "$TOMCAT_APPS/daner-face-search.war"
cp -- /tmp/src/conf/ocp/daner-face-search.xml "$TOMCAT_APPS/daner-face-search.xml"

export WAR_FILE=$(readlink -f "$TOMCAT_APPS/daner-face-search.war")
