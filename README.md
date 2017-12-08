This is a simple example on how to build and deploy your first vert.x application.

- REST API
- SockJS API

## Running locally 

- Download Infinispan server and unzip
- Start Infinispan Standalone Server `./infinispan-server/bin/standalone.sh` 

### REST API


### SockJS bridge R

## Running the Node application

- `cd cute-react`
- run `npm install`
- run `npm start`
- Go to `http://localhost:9000/`


## Deploying the solution on Openshift

- Docker version 1.13.1. Above versions are not guaranteed to be fully working on openshift
- OpenShift Client 3.6
- Kubernetes 1.6
- Kubetail 1.2.1

Docker daemon has to be running !

- Start Openshift cluster with the service catalog `./bin/start-openshift.sh`
- Start Infinispan cluster `./bin/start-infinispan.sh`
- Deploy the verticles `mvn fabric8:deploy`