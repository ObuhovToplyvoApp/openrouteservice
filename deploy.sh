#!/usr/bin/env bash

wget https://download.geofabrik.de/europe/monaco-latest.osm.pbf -O docker/data/main.pbf;

cd docker; ./restart.sh ;


