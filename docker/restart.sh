#!/usr/bin/env bash
docker-compose -f docker-compose.yml build
docker-compose -f docker-compose.yml stop

docker-compose -f docker-compose.yml up -d
