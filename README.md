Distributed Noracle Backend
===================

[![Build Status](https://jenkins.dbis.rwth-aachen.de/buildStatus/icon?job=Distributed-Noracle-Backend)](https://jenkins.dbis.rwth-aachen.de/job/Distributed-Noracle-Backend/)

This suite of microservices forms the backend of the Distributed Noracle Project.
Please try out our app at: [dbis.rwth-aachen.de/noracle/](http://dbis.rwth-aachen.de/noracle/)

---------------

## Java

las2peer uses **Java 14**.

## Setup
1. Build the project using `ant all`
1. Copy *launcher-configuration.ini* to *etc/*
1. Start using `./start-local.sh` for starting in the same shell (useful for testing), or `./start-node.sh` for starting Noracle in a screen (useful for production)

