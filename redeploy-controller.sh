#!/bin/sh

mvn package
docker build -f src/main/docker/Dockerfile.jvm -t quarkus/quarkus-admission-controller-jvm .
kubectl delete -f quarkus-admission-controller.yaml
sleep 10
kubectl apply -f quarkus-admission-controller.yaml
sleep 10
controller=$(kubectl get pods --selector=app=quarkus-admission-controller -o jsonpath='{.items[*].metadata.name}')
kubectl logs $controller -f