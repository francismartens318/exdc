FROM idalko/customconnectornode:5.24.0-m34


COPY ./build/libs/exdc.jar /opt/customconnectornode/install/lib/external/exdc.jar

# Load environment variables from ..env file
ENV CUSTOM_CONNECTOR_ALWAYS_RELOAD=false
ENV CC_EXTERNAL_JAR_NAME="exdc.jar"
ENV CUSTOM_CONNECTOR_NODE_TYPE="Discourse"
ENV GENERAL_SETTINGS_ENABLED=true
ENV USER_SERVICE_FEATURE=true
ENV CC_DEBUG=false
