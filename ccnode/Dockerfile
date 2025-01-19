FROM idalko/customconnectornode:5.20.0-m59


COPY build/libs/discourse-api-groovy.jar /opt/customconnectornode/install/lib/discourse-api-groovy.jar
COPY etc/sources/annotations-24.0.1.jar /opt/customconnectornode/install/lib/annotations-24.0.1.jar
COPY ccnode/config/start-node.sh /opt/customconnectornode/install/bin/start-node.sh
COPY ccnode/config/logback.xml /opt/customconnectornode/install/conf/logback.xml


# Load environment variables from .env file
ENV CUSTOM_CONNECTOR_API_CLASS_NAME=customconnectornode.discourse.DiscourseApi
ENV CUSTOM_CONNECTOR_ALWAYS_RELOAD=true

