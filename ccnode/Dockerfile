FROM idalko/customconnectornode:5.23.0-m20

#COPY build/libs/exdc.jar /opt/customconnectornode/install/lib/external/exdc.jar
COPY ./config/start-node.sh /opt/customconnectornode/install/bin/start-node.sh
COPY ./config/logback.xml /opt/customconnectornode/install/conf/logback.xml

# Load environment variables from ..env file
ENV CUSTOM_CONNECTOR_ALWAYS_RELOAD=true
ENV CC_EXTERNAL_JAR_NAME="exdc.jar"
ENV CUSTOM_CONNECTOR_NODE_TYPE="Discourse"
ENV GENERAL_SETTINGS_ENABLED=true
ENV USER_SERVICE_FEATURE=true
ENV CC_DEBUG=true