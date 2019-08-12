# quarkus-admission-controller

a quarkus application has been created with the following extensions
```
mvn io.quarkus:quarkus-maven-plugin:0.20.0:create \
    -DprojectGroupId=eu.sevel.quarkus.admission \
    -DprojectArtifactId=quarkus-admission-controller \
    -DclassName="eu.sevel.quarkus.admission.ValidatingAdmissionController" \
    -Dpath="/validate" \
    -Dextensions="resteasy-jsonb,kubernetes-client"
```

build the application
```
mvn package
docker build -f src/main/docker/Dockerfile.jvm -t quarkus/quarkus-admission-controller-jvm .
```

create admission namespace
```
kubectl create ns admission
kubectl config set-context $(kubectl config current-context) --namespace=admission
```

deploy a certificate for host quarkus-admission-controller.admission.svc
```
./create-cert.sh
```

deploy controller 
```
kubectl apply -f quarkus-admission-controller.yaml
sleep 10
controller=$(kubectl get pods --selector=app=quarkus-admission-controller -o jsonpath='{.items[*].metadata.name}')
kubectl logs $controller -f
```

create test namespace and deploy test application
```
kubectl create ns test-admission
kubectl label ns test-admission admission=enabled
kubectl apply -f httpbin.yaml
```

verify the controller service is working properly
```
httpbin=$(kubectl get pods -n test-admission --selector=name=httpbin -o jsonpath='{.items[*].metadata.name}')
kubectl exec $httpbin -n test-admission -- curl \
    --cacert /var/run/secrets/kubernetes.io/serviceaccount/ca.crt  \
    --request POST \
    -H "Content-Type: application/json" \
    --data '{"additionalProperties":{},"apiVersion":"admission.k8s.io/v1beta1","kind":"AdmissionReview","request":{"additionalProperties":{},"kind":{"additionalProperties":{},"group":"extensions","kind":"Deployment","version":"v1beta1"},"name":"httpbin","namespace":"test-admission","operation":"UPDATE","resource":{"additionalProperties":{},"group":"extensions","resource":"deployments","version":"v1beta1"},"uid":"75a55056-bc03-11e9-82d4-025000000001","userInfo":{"additionalProperties":{},"groups":["system:masters","system:authenticated"],"username":"docker-for-desktop"}}}' \
    https://quarkus-admission-controller.admission.svc/validate
```

you should get
```
{"additionalProperties":{},"apiVersion":"admission.k8s.io/v1beta1","kind":"AdmissionReview","response":{"additionalProperties":{},"allowed":true,"auditAnnotations":{},"uid":"75a55056-bc03-11e9-82d4-025000000001"}}
```

get the base64 encoded ca bundle
``` 
kubectl exec $controller -- cat /var/run/secrets/kubernetes.io/serviceaccount/ca.crt | base64 | tr -d '\n'
```

replace the value in field caBundle of file validating-webhook.yaml
```
caBundle: LS0...
```

deploy validating webhook
```
kubectl apply -f validating-webhook.yaml
```

delete the test application
```
kubectl delete -f httpbin.yaml
```

you should see a DELETE event in the controller logs
<details><summary>DELETE event</summary>
<p>

```
{
    "additionalProperties": {
    },
    "apiVersion": "admission.k8s.io/v1beta1",
    "kind": "AdmissionReview",
    "request": {
        "additionalProperties": {
        },
        "dryRun": false,
        "kind": {
            "additionalProperties": {
            },
            "group": "apps",
            "kind": "Deployment",
            "version": "v1beta1"
        },
        "name": "httpbin",
        "namespace": "test-admission",
        "operation": "DELETE",
        "resource": {
            "additionalProperties": {
            },
            "group": "apps",
            "resource": "deployments",
            "version": "v1beta1"
        },
        "uid": "5795d3fe-bc1a-11e9-b52a-025000000001",
        "userInfo": {
            "additionalProperties": {
            },
            "groups": [
                "system:masters",
                "system:authenticated"
            ],
            "username": "docker-for-desktop"
        }
    }
}
```

</p>
</details>

recreate the application
```
kubectl apply -f httpbin.yaml
```

you should now see a CREATE event with object
<details><summary>CREATE event</summary>
<p>

```
{
    "additionalProperties": {
    },
    "apiVersion": "admission.k8s.io/v1beta1",
    "kind": "AdmissionReview",
    "request": {
        "additionalProperties": {
        },
        "dryRun": false,
        "kind": {
            "additionalProperties": {
            },
            "group": "apps",
            "kind": "Deployment",
            "version": "v1beta1"
        },
        "namespace": "test-admission",
        "object": {
            "additionalProperties": {
            },
            "apiVersion": "apps/v1beta1",
            "kind": "Deployment",
            "metadata": {
                "additionalProperties": {
                },
                "annotations": {
                    "kubectl.kubernetes.io/last-applied-configuration": "{\"apiVersion\":\"apps/v1beta1\",\"kind\":\"Deployment\",\"metadata\":{\"annotations\":{},\"name\":\"httpbin\",\"namespace\":\"test-admission\"},\"spec\":{\"replicas\":1,\"template\":{\"metadata\":{\"labels\":{\"name\":\"httpbin\"}},\"spec\":{\"containers\":[{\"command\":[\"sleep\",\"3600\"],\"image\":\"scrapinghub/httpbin:latest\",\"name\":\"httpbin\",\"resources\":{\"requests\":{\"cpu\":0.1,\"memory\":200}}}]}}}}\n"
                },
                "creationTimestamp": "2019-08-11T09:27:13Z",
                "finalizers": [
                ],
                "generation": 1,
                "labels": {
                    "name": "httpbin"
                },
                "name": "httpbin",
                "namespace": "test-admission",
                "ownerReferences": [
                ],
                "uid": "33b61451-bc1a-11e9-b52a-025000000001"
            },
            "spec": {
                "additionalProperties": {
                },
                "progressDeadlineSeconds": 600,
                "replicas": 1,
                "revisionHistoryLimit": 2,
                "selector": {
                    "additionalProperties": {
                    },
                    "matchExpressions": [
                    ],
                    "matchLabels": {
                        "name": "httpbin"
                    }
                },
                "strategy": {
                    "additionalProperties": {
                    },
                    "rollingUpdate": {
                        "additionalProperties": {
                        },
                        "maxSurge": {
                            "additionalProperties": {
                            },
                            "strVal": "25%"
                        },
                        "maxUnavailable": {
                            "additionalProperties": {
                            },
                            "strVal": "25%"
                        }
                    },
                    "type": "RollingUpdate"
                },
                "template": {
                    "additionalProperties": {
                    },
                    "metadata": {
                        "additionalProperties": {
                        },
                        "finalizers": [
                        ],
                        "labels": {
                            "name": "httpbin"
                        },
                        "ownerReferences": [
                        ]
                    },
                    "spec": {
                        "additionalProperties": {
                        },
                        "containers": [
                            {
                                "additionalProperties": {
                                },
                                "args": [
                                ],
                                "command": [
                                    "sleep",
                                    "3600"
                                ],
                                "env": [
                                ],
                                "envFrom": [
                                ],
                                "image": "scrapinghub/httpbin:latest",
                                "imagePullPolicy": "Always",
                                "name": "httpbin",
                                "ports": [
                                ],
                                "resources": {
                                    "additionalProperties": {
                                    },
                                    "requests": {
                                        "cpu": {
                                            "additionalProperties": {
                                            },
                                            "amount": "100m"
                                        },
                                        "memory": {
                                            "additionalProperties": {
                                            },
                                            "amount": "200"
                                        }
                                    }
                                },
                                "terminationMessagePath": "/dev/termination-log",
                                "terminationMessagePolicy": "File",
                                "volumeDevices": [
                                ],
                                "volumeMounts": [
                                ]
                            }
                        ],
                        "dnsPolicy": "ClusterFirst",
                        "hostAliases": [
                        ],
                        "imagePullSecrets": [
                        ],
                        "initContainers": [
                        ],
                        "readinessGates": [
                        ],
                        "restartPolicy": "Always",
                        "schedulerName": "default-scheduler",
                        "securityContext": {
                            "additionalProperties": {
                            },
                            "supplementalGroups": [
                            ],
                            "sysctls": [
                            ]
                        },
                        "terminationGracePeriodSeconds": 30,
                        "tolerations": [
                        ],
                        "volumes": [
                        ]
                    }
                }
            },
            "status": {
                "additionalProperties": {
                },
                "conditions": [
                ]
            }
        },
        "operation": "CREATE",
        "resource": {
            "additionalProperties": {
            },
            "group": "apps",
            "resource": "deployments",
            "version": "v1beta1"
        },
        "uid": "33b62ca7-bc1a-11e9-b52a-025000000001",
        "userInfo": {
            "additionalProperties": {
            },
            "groups": [
                "system:masters",
                "system:authenticated"
            ],
            "username": "docker-for-desktop"
        }
    }
}
```

</p>
</details>

make a modification
```
kubectl label deployment httpbin -n test-admission foo=bar
```

you should see an UPDATE event with object and oldObject
<details><summary>UPDATE event</summary>
<p>

```
{
    "additionalProperties": {
    },
    "apiVersion": "admission.k8s.io/v1beta1",
    "kind": "AdmissionReview",
    "request": {
        "additionalProperties": {
        },
        "dryRun": false,
        "kind": {
            "additionalProperties": {
            },
            "group": "extensions",
            "kind": "Deployment",
            "version": "v1beta1"
        },
        "name": "httpbin",
        "namespace": "test-admission",
        "object": {
            "additionalProperties": {
            },
            "apiVersion": "extensions/v1beta1",
            "kind": "Deployment",
            "metadata": {
                "additionalProperties": {
                },
                "annotations": {
                    "deployment.kubernetes.io/revision": "1",
                    "kubectl.kubernetes.io/last-applied-configuration": "{\"apiVersion\":\"apps/v1beta1\",\"kind\":\"Deployment\",\"metadata\":{\"annotations\":{},\"name\":\"httpbin\",\"namespace\":\"test-admission\"},\"spec\":{\"replicas\":1,\"template\":{\"metadata\":{\"labels\":{\"name\":\"httpbin\"}},\"spec\":{\"containers\":[{\"command\":[\"sleep\",\"3600\"],\"image\":\"scrapinghub/httpbin:latest\",\"name\":\"httpbin\",\"resources\":{\"requests\":{\"cpu\":0.1,\"memory\":200}}}]}}}}\n"
                },
                "creationTimestamp": "2019-08-11T09:29:09Z",
                "finalizers": [
                ],
                "generation": 1,
                "labels": {
                    "foo": "bar",
                    "name": "httpbin"
                },
                "name": "httpbin",
                "namespace": "test-admission",
                "ownerReferences": [
                ],
                "resourceVersion": "4080",
                "uid": "78d5dadb-bc1a-11e9-b52a-025000000001"
            },
            "spec": {
                "additionalProperties": {
                },
                "progressDeadlineSeconds": 600,
                "replicas": 1,
                "revisionHistoryLimit": 2,
                "selector": {
                    "additionalProperties": {
                    },
                    "matchExpressions": [
                    ],
                    "matchLabels": {
                        "name": "httpbin"
                    }
                },
                "strategy": {
                    "additionalProperties": {
                    },
                    "rollingUpdate": {
                        "additionalProperties": {
                        },
                        "maxSurge": {
                            "additionalProperties": {
                            },
                            "strVal": "25%"
                        },
                        "maxUnavailable": {
                            "additionalProperties": {
                            },
                            "strVal": "25%"
                        }
                    },
                    "type": "RollingUpdate"
                },
                "template": {
                    "additionalProperties": {
                    },
                    "metadata": {
                        "additionalProperties": {
                        },
                        "finalizers": [
                        ],
                        "labels": {
                            "name": "httpbin"
                        },
                        "ownerReferences": [
                        ]
                    },
                    "spec": {
                        "additionalProperties": {
                        },
                        "containers": [
                            {
                                "additionalProperties": {
                                },
                                "args": [
                                ],
                                "command": [
                                    "sleep",
                                    "3600"
                                ],
                                "env": [
                                ],
                                "envFrom": [
                                ],
                                "image": "scrapinghub/httpbin:latest",
                                "imagePullPolicy": "Always",
                                "name": "httpbin",
                                "ports": [
                                ],
                                "resources": {
                                    "additionalProperties": {
                                    },
                                    "requests": {
                                        "cpu": {
                                            "additionalProperties": {
                                            },
                                            "amount": "100m"
                                        },
                                        "memory": {
                                            "additionalProperties": {
                                            },
                                            "amount": "200"
                                        }
                                    }
                                },
                                "terminationMessagePath": "/dev/termination-log",
                                "terminationMessagePolicy": "File",
                                "volumeDevices": [
                                ],
                                "volumeMounts": [
                                ]
                            }
                        ],
                        "dnsPolicy": "ClusterFirst",
                        "hostAliases": [
                        ],
                        "imagePullSecrets": [
                        ],
                        "initContainers": [
                        ],
                        "readinessGates": [
                        ],
                        "restartPolicy": "Always",
                        "schedulerName": "default-scheduler",
                        "securityContext": {
                            "additionalProperties": {
                            },
                            "supplementalGroups": [
                            ],
                            "sysctls": [
                            ]
                        },
                        "terminationGracePeriodSeconds": 30,
                        "tolerations": [
                        ],
                        "volumes": [
                        ]
                    }
                }
            },
            "status": {
                "additionalProperties": {
                },
                "availableReplicas": 1,
                "conditions": [
                    {
                        "additionalProperties": {
                        },
                        "lastTransitionTime": "2019-08-11T09:29:14Z",
                        "lastUpdateTime": "2019-08-11T09:29:14Z",
                        "message": "Deployment has minimum availability.",
                        "reason": "MinimumReplicasAvailable",
                        "status": "True",
                        "type": "Available"
                    },
                    {
                        "additionalProperties": {
                        },
                        "lastTransitionTime": "2019-08-11T09:29:09Z",
                        "lastUpdateTime": "2019-08-11T09:29:14Z",
                        "message": "ReplicaSet \"httpbin-7fc88d9dd9\" has successfully progressed.",
                        "reason": "NewReplicaSetAvailable",
                        "status": "True",
                        "type": "Progressing"
                    }
                ],
                "observedGeneration": 1,
                "readyReplicas": 1,
                "replicas": 1,
                "updatedReplicas": 1
            }
        },
        "oldObject": {
            "additionalProperties": {
            },
            "apiVersion": "extensions/v1beta1",
            "kind": "Deployment",
            "metadata": {
                "additionalProperties": {
                },
                "annotations": {
                    "deployment.kubernetes.io/revision": "1",
                    "kubectl.kubernetes.io/last-applied-configuration": "{\"apiVersion\":\"apps/v1beta1\",\"kind\":\"Deployment\",\"metadata\":{\"annotations\":{},\"name\":\"httpbin\",\"namespace\":\"test-admission\"},\"spec\":{\"replicas\":1,\"template\":{\"metadata\":{\"labels\":{\"name\":\"httpbin\"}},\"spec\":{\"containers\":[{\"command\":[\"sleep\",\"3600\"],\"image\":\"scrapinghub/httpbin:latest\",\"name\":\"httpbin\",\"resources\":{\"requests\":{\"cpu\":0.1,\"memory\":200}}}]}}}}\n"
                },
                "creationTimestamp": "2019-08-11T09:29:09Z",
                "finalizers": [
                ],
                "generation": 1,
                "labels": {
                    "name": "httpbin"
                },
                "name": "httpbin",
                "namespace": "test-admission",
                "ownerReferences": [
                ],
                "resourceVersion": "4080",
                "uid": "78d5dadb-bc1a-11e9-b52a-025000000001"
            },
            "spec": {
                "additionalProperties": {
                },
                "progressDeadlineSeconds": 600,
                "replicas": 1,
                "revisionHistoryLimit": 2,
                "selector": {
                    "additionalProperties": {
                    },
                    "matchExpressions": [
                    ],
                    "matchLabels": {
                        "name": "httpbin"
                    }
                },
                "strategy": {
                    "additionalProperties": {
                    },
                    "rollingUpdate": {
                        "additionalProperties": {
                        },
                        "maxSurge": {
                            "additionalProperties": {
                            },
                            "strVal": "25%"
                        },
                        "maxUnavailable": {
                            "additionalProperties": {
                            },
                            "strVal": "25%"
                        }
                    },
                    "type": "RollingUpdate"
                },
                "template": {
                    "additionalProperties": {
                    },
                    "metadata": {
                        "additionalProperties": {
                        },
                        "finalizers": [
                        ],
                        "labels": {
                            "name": "httpbin"
                        },
                        "ownerReferences": [
                        ]
                    },
                    "spec": {
                        "additionalProperties": {
                        },
                        "containers": [
                            {
                                "additionalProperties": {
                                },
                                "args": [
                                ],
                                "command": [
                                    "sleep",
                                    "3600"
                                ],
                                "env": [
                                ],
                                "envFrom": [
                                ],
                                "image": "scrapinghub/httpbin:latest",
                                "imagePullPolicy": "Always",
                                "name": "httpbin",
                                "ports": [
                                ],
                                "resources": {
                                    "additionalProperties": {
                                    },
                                    "requests": {
                                        "cpu": {
                                            "additionalProperties": {
                                            },
                                            "amount": "100m"
                                        },
                                        "memory": {
                                            "additionalProperties": {
                                            },
                                            "amount": "200"
                                        }
                                    }
                                },
                                "terminationMessagePath": "/dev/termination-log",
                                "terminationMessagePolicy": "File",
                                "volumeDevices": [
                                ],
                                "volumeMounts": [
                                ]
                            }
                        ],
                        "dnsPolicy": "ClusterFirst",
                        "hostAliases": [
                        ],
                        "imagePullSecrets": [
                        ],
                        "initContainers": [
                        ],
                        "readinessGates": [
                        ],
                        "restartPolicy": "Always",
                        "schedulerName": "default-scheduler",
                        "securityContext": {
                            "additionalProperties": {
                            },
                            "supplementalGroups": [
                            ],
                            "sysctls": [
                            ]
                        },
                        "terminationGracePeriodSeconds": 30,
                        "tolerations": [
                        ],
                        "volumes": [
                        ]
                    }
                }
            },
            "status": {
                "additionalProperties": {
                },
                "availableReplicas": 1,
                "conditions": [
                    {
                        "additionalProperties": {
                        },
                        "lastTransitionTime": "2019-08-11T09:29:14Z",
                        "lastUpdateTime": "2019-08-11T09:29:14Z",
                        "message": "Deployment has minimum availability.",
                        "reason": "MinimumReplicasAvailable",
                        "status": "True",
                        "type": "Available"
                    },
                    {
                        "additionalProperties": {
                        },
                        "lastTransitionTime": "2019-08-11T09:29:09Z",
                        "lastUpdateTime": "2019-08-11T09:29:14Z",
                        "message": "ReplicaSet \"httpbin-7fc88d9dd9\" has successfully progressed.",
                        "reason": "NewReplicaSetAvailable",
                        "status": "True",
                        "type": "Progressing"
                    }
                ],
                "observedGeneration": 1,
                "readyReplicas": 1,
                "replicas": 1,
                "updatedReplicas": 1
            }
        },
        "operation": "UPDATE",
        "resource": {
            "additionalProperties": {
            },
            "group": "extensions",
            "resource": "deployments",
            "version": "v1beta1"
        },
        "uid": "7c39bed0-bc1a-11e9-b52a-025000000001",
        "userInfo": {
            "additionalProperties": {
            },
            "groups": [
                "system:masters",
                "system:authenticated"
            ],
            "username": "docker-for-desktop"
        }
    }
}
```
</p>
</details>

deploy the mutating webhook
```
kubectl apply -f mutating-webhook.yaml
```

redeploy the application 
```
kubectl delete -f httpbin.yaml
kubectl apply -f httpbin.yaml
```

you should see
```
patching with [{"op":"add","path":"/metadata/labels/foo","value":"bar"}]
```

check that label has been applied to the deployment
```
kubectl get deploy -n test-admission httpbin -o yaml
```

you should see
```
apiVersion: extensions/v1beta1
kind: Deployment
metadata:
...
  labels:
    foo: bar
    name: httpbin
  name: httpbin
  namespace: test-admission

```

cleanup
```
kubectl delete ns admission
kubectl delete ns test-admission
kubectl delete csr quarkus-admission-controller
kubectl delete ValidatingWebhookConfiguration validating-quarkus-admission-controller
kubectl delete MutatingWebhookConfiguration mutating-quarkus-admission-controller
```
