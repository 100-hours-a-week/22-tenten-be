#!/bin/bash
docker stop backend-app || true
docker rm backend-app || true