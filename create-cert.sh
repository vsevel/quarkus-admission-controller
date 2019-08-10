#!/bin/bash

mkdir -p target/cert
cp csr.json target/cert
pushd target/cert

# Create private key and CSR
cfssl genkey csr.json | cfssljson -bare quarkus-admission-controller

# Create CSR k8s object
cat <<EOF | kubectl create -f -
apiVersion: certificates.k8s.io/v1beta1
kind: CertificateSigningRequest
metadata:
  name: quarkus-admission-controller
spec:
  groups:
  - system:authenticated
  request: $(cat quarkus-admission-controller.csr | base64 | tr -d '\n')
  usages:
  - digital signature
  - key encipherment
  - server auth
EOF

# Approve certificate
kubectl certificate approve quarkus-admission-controller

sleep 5s

# Download public key
kubectl get csr quarkus-admission-controller -o jsonpath='{.status.certificate}' | base64 --decode > quarkus-admission-controller.crt

cp quarkus-admission-controller-key.pem tls.key
cp quarkus-admission-controller.crt tls.crt
kubectl create secret tls quarkus-admission-controller-tls -n admission --key ./tls.key --cert ./tls.crt

# Display public key content
openssl x509 -in tls.crt -text
  #Propriétaire : CN=quarkus-admission-controller.admission.svc
  #Emetteur : CN=kubernetes

popd