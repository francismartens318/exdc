#!/bin/bash

HOME_DIR=/opt/customconnectornode
DATA_DIR=$HOME_DIR/data
APP_DIR=$HOME_DIR/install
SYSCONFIG=/etc/sysconfig/customconnectornode



# ***********************************************

# stop if no configuration available
if [ ! -f "$SYSCONFIG" ]; then printf "no config present ... exiting\n"; exit 1; fi
source $SYSCONFIG

#logging adjustments
sed -i 's:<logger name="application.controllers" level="TRACE"/>:<logger name="application.controllers" level="DEBUG"/>:g' $APP_DIR/conf/logback.xml
if [ "z${TRACE_CONTROLLER}" = "zTRUE" ]; then
        sed -i 's:<logger name="application.controllers" level="DEBUG"/>:<logger name="application.controllers" level="TRACE"/>:g' $APP_DIR/conf/logback.xml
fi

sed -i 's:<logger name="slick.jdbc.JdbcBackend.statement" level="DEBUG"/>:<logger name="slick.jdbc.JdbcBackend.statement" level="ERROR"/>:g' $APP_DIR/conf/logback.xml
if [ "z${SQL_CONTROLLER}" = "zTRUE" ]; then
        sed -i 's:<logger name="slick.jdbc.JdbcBackend.statement" level="ERROR"/>:<logger name="slick.jdbc.JdbcBackend.statement" level="DEBUG"/>:g' $APP_DIR/conf/logback.xml
fi




PORT=$CUSTOMCONNECTORNODE_PORT

#
# Set NODE_XMX to 2048m when it is not set, or blank
#
: ${CUSTOMCONNECTORNODE_XMX:=2048m}

#
# SNOWNODE-254 - set the appropriate email details, keep it backward compatible
#

echo "SMTP Parameters are"
echo "=========================="
echo "SMTP_HOST_NAME = '${SMTP_HOST_NAME}'"
echo "SMTP_PORT      = '${SMTP_PORT}'"
echo "SMTP_FROM      = '${SMTP_FROM}'"
echo "SMTP_LOGIN     = '${SMTP_LOGIN}'"
echo "SMTP_PASS      = 'XXX'"
echo "SMTP_TLS       = '${SMTP_TLS}'"

#
# SNOWNODE-202 - add support for proxy
#
if [[ ! -z "${PROXY_HTTP_HOST}" && ! -z "${PROXY_HTTP_PORT}" ]]; then
   echo "Setting up PROXY HTTP on ${PROXY_HTTP_HOST}:${PROXY_HTTP_PORT}"

   PROXYHTTP="-Dhttp.proxyHost=${PROXY_HTTP_HOST} -Dhttp.proxyPort=${PROXY_HTTP_PORT}"
   PROXYENABLE="-Dplay.ws.useProxyProperties=true"
fi

if [[ ! -z "${PROXY_HTTPS_HOST}" && ! -z "${PROXY_HTTPS_PORT}" ]]; then
   echo "Setting up PROXY HTTPS on ${PROXY_HTTPS_HOST}:${PROXY_HTTPS_PORT}" 

   PROXYHTTPS="-Dhttps.proxyHost=${PROXY_HTTPS_HOST} -Dhttps.proxyPort=${PROXY_HTTPS_PORT}"
   PROXYENABLE="-Dplay.ws.useProxyProperties=true"
fi


export CUSTOM_CONNECTOR_ALWAYS_RELOAD=true


echo "starting Exalate for Custom Connector node"
echo "-----------------------------------------"
echo "Scripts directory"
ls -lR /opt/customconnectornode/data/scripts/customconnectornode
echo "Environment"
printenv
echo "-----------------------------------------"




echo "/opt/customconnectornode/install/bin/customconnectornode -J-Xms128M -J-Xmx${CUSTOMCONNECTORNODE_XMX} -Dhttp.port=${PORT} -Dlogger.file=/opt/customconnectornode/install/conf/logback.xml -Dslick.dbs.default.db.url=jdbc:postgresql://${CUSTOMCONNECTORNODE_PG_HOST}/${CUSTOMCONNECTORNODE_PG_DB} -Dslick.dbs.default.db.user=${CUSTOMCONNECTORNODE_PG_USER} -Dslick.dbs.default.db.password=XXXX -DapplyEvolutions.default=true ${PROXYHTTP} ${PROXYHTTPS} ${PROXYENABLE}"

/opt/customconnectornode/install/bin/customconnectornode -J-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 -J-Xms128M -J-Xmx${CUSTOMCONNECTORNODE_XMX} -Dhttp.port=${PORT} -Dlogger.file=/opt/customconnectornode/install/conf/logback.xml -Dslick.dbs.default.db.url=jdbc:postgresql://${CUSTOMCONNECTORNODE_PG_HOST}/${CUSTOMCONNECTORNODE_PG_DB} -Dslick.dbs.default.db.user=${CUSTOMCONNECTORNODE_PG_USER} -Dslick.dbs.default.db.password=${CUSTOMCONNECTORNODE_PG_PWD} -DapplyEvolutions.default=true ${PROXYHTTP} ${PROXYHTTPS} ${PROXYENABLE}

