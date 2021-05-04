#!/usr/bin/env bash

cp -- /tmp/src/conf/ocp/logback.xml "$CONF_DIR/logback.xml"
# There are normally two configurations: core and environment
cp -- /tmp/src/conf/daner-face-search-*.yaml "$CONF_DIR/"
 
ln -s -- "$TOMCAT_APPS/daner-face-search.xml" "$DEPLOYMENT_DESC_DIR/daner-face-search.xml"
