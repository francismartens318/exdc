FROM idalko/customconnectornode:004

COPY build/libs/exdc.jar /opt/customconnectornode/install/lib/external/cc.jar
COPY ccnode/config/start-node.sh /opt/customconnectornode/install/bin/start-node.sh
COPY ccnode/config/logback.xml /opt/customconnectornode/install/conf/logback.xml

# Load environment variables from ..env file
ENV CUSTOM_CONNECTOR_ALWAYS_RELOAD=true
