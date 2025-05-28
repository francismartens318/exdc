The Dockerfile includes multi-stage builds to optimize the final image size and includes only the necessary runtime components.

# Development Guide

## Prerequisites

- Make sure Docker and Docker Compose are installed on your system
- The development environment requires Java 17 or higher, and gradle 8.3 or higher
- Access to the artifact.exalate.com to download some artifacts
- All configuration can be overridden using environment variables
- Define the exalate cloud prefix.  Something like `discoursenode`

## Setting Up Development Environment

1. Clone the repository
2. Configure environment variables by creating a `.env` file in the ccnode directory. A sample .env file is provided (env_sample)
3. Configure Gradle properties by creating/updating `gradle.properties` file in the root folder, a sample has been provided.
4. Run a gradle clean build to validate all is setup correctly.  The tests must succeed
5. Make sure that the ccnode container image is available.  It can be build by using
```
cd <rootdir>

docker buildx build --platform linux/amd64,linux/arm64 \
  -t ccnode:<sometag> \
  -f ccnode/Dockerfile --load ccnode

```

You should now have a fully functional development environment, allowing you to create testcases, make them green. Debug, autocomplete and all that jazz must be available


## Running the node

### Setup the application
- cd to the ccnode folder
- execute `docker compose up -d database` to start the database.
- access the database using `docker exec -it ccnode_database_1 psql -U exalate ccnode`
- update the lifecycle_info to skip the registration

``` 
update lifecycle_info set is_eula_accepted = 't', verified = 't' where id = 1;
```

### Running the application
- execute `docker compose up -d ccnode` to start the application
- access the application using `http://localhost:9002`
- The userid and password are set in the .env as 'TRACKER_USER' and 'TRACKER_PASSWORD'
- Logging can be found in the ccnode/persist/home/logs folder
- The application can be stopped using `docker compose stop ccnode'

### debugging the application
- The application can be debugged by setting up a remote debug configuration in your IDE.
- The application listens on port 5005 for debugging



## building the image for the node
- cd to the root folder
- check Dockerfile and adapt to your needs
- execute `docker build -t <prefix>:<version> .` to build the image
- publish it to your registry. 




