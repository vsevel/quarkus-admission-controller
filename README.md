# quarkus-admission-controller

kubectl create ns admission
kubectl config set-context $(kubectl config current-context) --namespace=admission

kubectl apply -f quarkus-admission-controller.yaml
kubectl apply -f httpbin.yaml

kubectl exec -it httpbin-XXX sh
curl --cacert /var/run/secrets/kubernetes.io/serviceaccount/ca.crt  https://quarkus-admission-controller.admission.svc