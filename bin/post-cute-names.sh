#!/usr/bin/env bash
curl -X POST -H "Content-Type: application/json" -d '{"name":"pepe"}' "http://localhost:8081/api/cutenames"
