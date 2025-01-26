FROM idalko/customconnectornode:5.20.0-m59


#
# copy some adaptations necessary (should be in the customconnectornode repo
#

COPY etc/sources/annotations-24.0.1.jar /opt/customconnectornode/install/lib/annotations-24.0.1.jar
COPY ccnode/config/start-node.sh /opt/customconnectornode/install/bin/start-node.sh
COPY ccnode/config/logback.xml /opt/customconnectornode/install/conf/logback.xml
COPY ccnode/config/customconnectornode /opt/customconnectornode/install/bin/customconnectornode

#
# Make sure that the resulting jar (from the gradle build) is copied to the correct location in the image (/opt/customconnectornode/install/lib)
#

COPY build/libs/exdc.jar /opt/customconnectornode/install/lib/exdc.jar

#
#

ENV CUSTOM_CONNECTOR_API_CLASS_NAME=customconnectornode.discourse.DiscourseApi

#
# Allow for hot reload
#

ENV CUSTOM_CONNECTOR_ALWAYS_RELOAD=true


# Make the image ready for deployment on Exalate Cloud
#
# The template which will allow to create nodes of this type will have a prefix like 'discoursenode'
# make sure that the prefix for the custom connector is available as a log file
# like <prefix>.log

ENV LOG_FILE_NAME=discoursenode.log

#
# make sure that the path to the data directory is also available as a symmbolic link
# like /opt/<prefix> -> /opt/customconnectornode
#

RUN ln -s /opt/customconnectornode /opt/discoursenode
