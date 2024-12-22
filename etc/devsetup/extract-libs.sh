#!/bin/bash

# Extract the libraries of the image that is used by the Custom Connector Node
# this is necessary to be able to manage the dependencies used in the code

# Validate ccnode directory exists
if [ ! -d "./ccnode" ]; then
    echo "Error: ccnode directory not found"
    exit 1
fi

# Validate .env file exists
if [ ! -f ./ccnode/.env ]; then
    echo "Error: ./ccnode/.env file not found - check env.sample"
    exit 1
fi


echo "Starting to create the libraries folder"

docker ps > /dev/null 2>&1
if [ $? -ne 0 ]; then
    echo "Docker daemon is not running. Please start Docker with one of these commands:"
    echo "  - systemctl start docker    # Linux with systemd"
    echo "  - sudo service docker start # Linux with sysvinit"
    echo "  - Start Docker Desktop      # Windows/Mac"
    exit 1
fi
# Check Docker version
DOCKER_VERSION=$(docker version --format '{{.Server.Version}}' | cut -d. -f1-2)
if (( $(echo "$DOCKER_VERSION < 27.3" | bc -l) )); then
    echo "Docker version 27.3 or higher is required. Current version: $DOCKER_VERSION"
    exit 1
fi
# Get the image version from the .env file
export IMAGE_VERSION=$(grep IMAGE ./ccnode/.env | cut -d= -f2)

echo "Extracting library from "  $IMAGE_VERSION
# Create a temporary container from the image defined in .env
# The image contains the Custom Connector Node libraries
docker create -q --platform linux/amd64 --name temp_container $IMAGE_VERSION


# Copy the lib directory from the temporary container to the local filesystem
# Target: /opt/customconnectornode/install/lib -> ./lib
docker cp temp_container:/opt/customconnectornode/install/lib ./


# Clean up by removing the temporary container
docker rm temp_container


echo "Libraries extracted from Custom Connector Node image version: ${IMAGE_VERSION}" > ./lib/README.md
echo "Extraction date: $(date)" >> ./lib/README.md
