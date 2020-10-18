# quarkus-admission-controller

##prerequisites
creation of the certificate request is done with cfssl & cfssljson;
1. install go https://golang.org/doc/install
2. install plugins **cfssl and cfssljson** for go https://github.com/cloudflare/cfssl

minikube is utilized; don't push images into a registry,
tell docker to utilize minikube docker daemon
https://stackoverflow.com/a/42564211
1. start minikube
2. set docker environment to minikube docker
```
minikube start
eval $(minikube docker-env)
```

##installation

a quarkus application has been created with the following extensions
```
mvn io.quarkus:quarkus-maven-plugin:1.8.1.Final:create \
    -DprojectGroupId=eu.sevel.quarkus.admission \
    -DprojectArtifactId=quarkus-admission-controller \
    -DclassName="eu.sevel.quarkus.admission.ValidatingAdmissionController" \
    -Dpath="/validate" \
    -Dextensions="resteasy-jsonb,kubernetes-client,undertow"
```
unfortunately, this won't install 1.8.1.Final,
you need to tweak the versions manually in the pom to this version (last tested one)
1.9.0 RC has deserializing errors of AdmissionReview

```
    <quarkus-plugin.version>1.8.1.Final</quarkus-plugin.version>
    <quarkus.platform.version>1.8.1.Final</quarkus.platform.version>
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
chmod +x ./create-cert.sh
./create-cert.sh
```

deploy controller 
```
kubectl apply -f quarkus-admission-controller.yaml
kubectl wait --for=condition=available --timeout=100s deployment/quarkus-admission-controller
controller=$(kubectl get pods --selector=app=quarkus-admission-controller -o jsonpath='{.items[*].metadata.name}')
kubectl logs $controller -f
```

create test namespace and deploy test application
```
kubectl create ns test-admission
kubectl label ns test-admission admission=enabled
kubectl apply -f httpbin.yaml
kubectl wait --for=condition=available --timeout=100s deployment/httpbin
```

verify the controller service is working properly
```
httpbin=$(kubectl get pods -n test-admission --selector=app=httpbin -o jsonpath='{.items[*].metadata.name}')
kubectl exec $httpbin -n test-admission -- curl \
    --cacert /var/run/secrets/kubernetes.io/serviceaccount/ca.crt  \
    --request POST \
    -H "Content-Type: application/json" \
    --data '{"additionalProperties":{},"apiVersion":"admission.k8s.io/v1","kind":"AdmissionReview","request":{"additionalProperties":{},"kind":{"additionalProperties":{},"group":"extensions","kind":"Deployment","version":"v1"},"name":"httpbin","namespace":"test-admission","operation":"UPDATE","resource":{"additionalProperties":{},"group":"extensions","resource":"deployments","version":"v1"},"uid":"75a55056-bc03-11e9-82d4-025000000001","userInfo":{"additionalProperties":{},"groups":["system:masters","system:authenticated"],"username":"docker-for-desktop"}}}' \
    https://quarkus-admission-controller.admission.svc/validate
```

you should get
```
{"additionalProperties":{},"apiVersion":"admission.k8s.io/v1beta1","kind":"AdmissionReview","response":{"additionalProperties":{},"allowed":true,"auditAnnotations":{},"uid":"75a55056-bc03-11e9-82d4-025000000001"}}
```

get the base64 encoded ca bundle
``` 
cert=$(kubectl exec $controller -- cat /var/run/secrets/kubernetes.io/serviceaccount/ca.crt | base64 | tr -d '\n')
```

replace the value in field caBundle of file validating-webhook.yaml
```
sed -i.bak -E "s/caBundle:.*?/caBundle: $cert/" validating-webhook.yaml
sed -i.bak -E "s/caBundle:.*?/caBundle: $cert/" mutating-webhook.yaml
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
                "version": "v1"
            },
            "name": "httpbin",
            "namespace": "test-admission",
            "oldObject": {
                "additionalProperties": {
                },
                "apiVersion": "apps/v1",
                "kind": "Deployment",
                "metadata": {
                    "additionalProperties": {
                    },
                    "annotations": {
                        "deployment.kubernetes.io/revision": "1",
                        "kubectl.kubernetes.io/last-applied-configuration": "{\"apiVersion\":\"apps/v1\",\"kind\":\"Deployment\",\"metadata\":{\"annotations\":{},\"labels\":{\"app\":\"httpbin\"},\"name\":\"httpbin\",\"namespace\":\"test-admission\"},\"spec\":{\"replicas\":1,\"selector\":{\"matchLabels\":{\"app\":\"httpbin\"}},\"template\":{\"metadata\":{\"labels\":{\"app\":\"httpbin\"}},\"spec\":{\"containers\":[{\"command\":[\"sleep\",\"3600\"],\"image\":\"scrapinghub/httpbin:latest\",\"name\":\"httpbin\",\"resources\":{\"requests\":{\"cpu\":0.1,\"memory\":200}}}]}}}}\n"
                    },
                    "creationTimestamp": "2020-10-18T16:26:44Z",
                    "finalizers": [
                    ],
                    "generation": 1,
                    "labels": {
                        "app": "httpbin"
                    },
                    "managedFields": [
                        {
                            "additionalProperties": {
                            },
                            "apiVersion": "apps/v1",
                            "fieldsType": "FieldsV1",
                            "fieldsV1": {
                                "additionalProperties": {
                                    "f:metadata": {
                                        "f:annotations": {
                                            ".": {
                                            },
                                            "f:kubectl.kubernetes.io/last-applied-configuration": {
                                            }
                                        },
                                        "f:labels": {
                                            ".": {
                                            },
                                            "f:app": {
                                            }
                                        }
                                    },
                                    "f:spec": {
                                        "f:progressDeadlineSeconds": {
                                        },
                                        "f:replicas": {
                                        },
                                        "f:revisionHistoryLimit": {
                                        },
                                        "f:selector": {
                                            "f:matchLabels": {
                                                ".": {
                                                },
                                                "f:app": {
                                                }
                                            }
                                        },
                                        "f:strategy": {
                                            "f:rollingUpdate": {
                                                ".": {
                                                },
                                                "f:maxSurge": {
                                                },
                                                "f:maxUnavailable": {
                                                }
                                            },
                                            "f:type": {
                                            }
                                        },
                                        "f:template": {
                                            "f:metadata": {
                                                "f:labels": {
                                                    ".": {
                                                    },
                                                    "f:app": {
                                                    }
                                                }
                                            },
                                            "f:spec": {
                                                "f:containers": {
                                                    "k:{\"name\":\"httpbin\"}": {
                                                        ".": {
                                                        },
                                                        "f:command": {
                                                        },
                                                        "f:image": {
                                                        },
                                                        "f:imagePullPolicy": {
                                                        },
                                                        "f:name": {
                                                        },
                                                        "f:resources": {
                                                            ".": {
                                                            },
                                                            "f:requests": {
                                                                ".": {
                                                                },
                                                                "f:cpu": {
                                                                },
                                                                "f:memory": {
                                                                }
                                                            }
                                                        },
                                                        "f:terminationMessagePath": {
                                                        },
                                                        "f:terminationMessagePolicy": {
                                                        }
                                                    }
                                                },
                                                "f:dnsPolicy": {
                                                },
                                                "f:restartPolicy": {
                                                },
                                                "f:schedulerName": {
                                                },
                                                "f:securityContext": {
                                                },
                                                "f:terminationGracePeriodSeconds": {
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            "manager": "kubectl-client-side-apply",
                            "operation": "Update",
                            "time": "2020-10-18T16:26:44Z"
                        },
                        {
                            "additionalProperties": {
                            },
                            "apiVersion": "apps/v1",
                            "fieldsType": "FieldsV1",
                            "fieldsV1": {
                                "additionalProperties": {
                                    "f:metadata": {
                                        "f:annotations": {
                                            "f:deployment.kubernetes.io/revision": {
                                            }
                                        }
                                    },
                                    "f:status": {
                                        "f:availableReplicas": {
                                        },
                                        "f:conditions": {
                                            ".": {
                                            },
                                            "k:{\"type\":\"Available\"}": {
                                                ".": {
                                                },
                                                "f:lastTransitionTime": {
                                                },
                                                "f:lastUpdateTime": {
                                                },
                                                "f:message": {
                                                },
                                                "f:reason": {
                                                },
                                                "f:status": {
                                                },
                                                "f:type": {
                                                }
                                            },
                                            "k:{\"type\":\"Progressing\"}": {
                                                ".": {
                                                },
                                                "f:lastTransitionTime": {
                                                },
                                                "f:lastUpdateTime": {
                                                },
                                                "f:message": {
                                                },
                                                "f:reason": {
                                                },
                                                "f:status": {
                                                },
                                                "f:type": {
                                                }
                                            }
                                        },
                                        "f:observedGeneration": {
                                        },
                                        "f:readyReplicas": {
                                        },
                                        "f:replicas": {
                                        },
                                        "f:updatedReplicas": {
                                        }
                                    }
                                }
                            },
                            "manager": "kube-controller-manager",
                            "operation": "Update",
                            "time": "2020-10-18T16:26:48Z"
                        }
                    ],
                    "name": "httpbin",
                    "namespace": "test-admission",
                    "ownerReferences": [
                    ],
                    "resourceVersion": "6556",
                    "uid": "66a165ff-98a3-4289-b29b-cb1f49013c23"
                },
                "spec": {
                    "additionalProperties": {
                    },
                    "progressDeadlineSeconds": 600,
                    "replicas": 1,
                    "revisionHistoryLimit": 10,
                    "selector": {
                        "additionalProperties": {
                        },
                        "matchExpressions": [
                        ],
                        "matchLabels": {
                            "app": "httpbin"
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
                                "kind": 1,
                                "strVal": "25%"
                            },
                            "maxUnavailable": {
                                "additionalProperties": {
                                },
                                "kind": 1,
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
                                "app": "httpbin"
                            },
                            "managedFields": [
                            ],
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
                                                "amount": "100",
                                                "format": "m"
                                            },
                                            "memory": {
                                                "additionalProperties": {
                                                },
                                                "amount": "200",
                                                "format": ""
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
                            "ephemeralContainers": [
                            ],
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
                            "topologySpreadConstraints": [
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
                            "lastTransitionTime": "2020-10-18T16:26:48Z",
                            "lastUpdateTime": "2020-10-18T16:26:48Z",
                            "message": "Deployment has minimum availability.",
                            "reason": "MinimumReplicasAvailable",
                            "status": "True",
                            "type": "Available"
                        },
                        {
                            "additionalProperties": {
                            },
                            "lastTransitionTime": "2020-10-18T16:26:44Z",
                            "lastUpdateTime": "2020-10-18T16:26:48Z",
                            "message": "ReplicaSet \"httpbin-5644b4777\" has successfully progressed.",
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
            "operation": "DELETE",
            "options": {
                "additionalProperties": {
                },
                "apiVersion": "meta.k8s.io/v1",
                "dryRun": [
                ],
                "kind": "DeleteOptions",
                "propagationPolicy": "Background"
            },
            "requestKind": {
                "additionalProperties": {
                },
                "group": "apps",
                "kind": "Deployment",
                "version": "v1"
            },
            "requestResource": {
                "additionalProperties": {
                },
                "group": "apps",
                "resource": "deployments",
                "version": "v1"
            },
            "resource": {
                "additionalProperties": {
                },
                "group": "apps",
                "resource": "deployments",
                "version": "v1"
            },
            "uid": "bbd83bf5-673d-4d2b-8be9-1beb2e817457",
            "userInfo": {
                "additionalProperties": {
                },
                "groups": [
                    "system:masters",
                    "system:authenticated"
                ],
                "username": "minikube-user"
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
            "group": "apps",
            "kind": "Deployment",
            "version": "v1"
        },
        "name": "httpbin",
        "namespace": "test-admission",
        "object": {
            "additionalProperties": {
            },
            "apiVersion": "apps/v1",
            "kind": "Deployment",
            "metadata": {
                "additionalProperties": {
                },
                "annotations": {
                    "deployment.kubernetes.io/revision": "1",
                    "kubectl.kubernetes.io/last-applied-configuration": "{\"apiVersion\":\"apps/v1\",\"kind\":\"Deployment\",\"metadata\":{\"annotations\":{},\"labels\":{\"app\":\"httpbin\"},\"name\":\"httpbin\",\"namespace\":\"test-admission\"},\"spec\":{\"replicas\":1,\"selector\":{\"matchLabels\":{\"app\":\"httpbin\"}},\"template\":{\"metadata\":{\"labels\":{\"app\":\"httpbin\"}},\"spec\":{\"containers\":[{\"command\":[\"sleep\",\"3600\"],\"image\":\"scrapinghub/httpbin:latest\",\"name\":\"httpbin\",\"resources\":{\"requests\":{\"cpu\":0.1,\"memory\":200}}}]}}}}\n"
                },
                "creationTimestamp": "2020-10-18T16:28:43Z",
                "finalizers": [
                ],
                "generation": 1,
                "labels": {
                    "app": "httpbin",
                    "foo": "bar"
                },
                "managedFields": [
                    {
                        "additionalProperties": {
                        },
                        "apiVersion": "apps/v1",
                        "fieldsType": "FieldsV1",
                        "fieldsV1": {
                            "additionalProperties": {
                                "f:metadata": {
                                    "f:annotations": {
                                        ".": {
                                        },
                                        "f:kubectl.kubernetes.io/last-applied-configuration": {
                                        }
                                    },
                                    "f:labels": {
                                        ".": {
                                        },
                                        "f:app": {
                                        }
                                    }
                                },
                                "f:spec": {
                                    "f:progressDeadlineSeconds": {
                                    },
                                    "f:replicas": {
                                    },
                                    "f:revisionHistoryLimit": {
                                    },
                                    "f:selector": {
                                        "f:matchLabels": {
                                            ".": {
                                            },
                                            "f:app": {
                                            }
                                        }
                                    },
                                    "f:strategy": {
                                        "f:rollingUpdate": {
                                            ".": {
                                            },
                                            "f:maxSurge": {
                                            },
                                            "f:maxUnavailable": {
                                            }
                                        },
                                        "f:type": {
                                        }
                                    },
                                    "f:template": {
                                        "f:metadata": {
                                            "f:labels": {
                                                ".": {
                                                },
                                                "f:app": {
                                                }
                                            }
                                        },
                                        "f:spec": {
                                            "f:containers": {
                                                "k:{\"name\":\"httpbin\"}": {
                                                    ".": {
                                                    },
                                                    "f:command": {
                                                    },
                                                    "f:image": {
                                                    },
                                                    "f:imagePullPolicy": {
                                                    },
                                                    "f:name": {
                                                    },
                                                    "f:resources": {
                                                        ".": {
                                                        },
                                                        "f:requests": {
                                                            ".": {
                                                            },
                                                            "f:cpu": {
                                                            },
                                                            "f:memory": {
                                                            }
                                                        }
                                                    },
                                                    "f:terminationMessagePath": {
                                                    },
                                                    "f:terminationMessagePolicy": {
                                                    }
                                                }
                                            },
                                            "f:dnsPolicy": {
                                            },
                                            "f:restartPolicy": {
                                            },
                                            "f:schedulerName": {
                                            },
                                            "f:securityContext": {
                                            },
                                            "f:terminationGracePeriodSeconds": {
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        "manager": "kubectl-client-side-apply",
                        "operation": "Update",
                        "time": "2020-10-18T16:28:43Z"
                    },
                    {
                        "additionalProperties": {
                        },
                        "apiVersion": "apps/v1",
                        "fieldsType": "FieldsV1",
                        "fieldsV1": {
                            "additionalProperties": {
                                "f:metadata": {
                                    "f:annotations": {
                                        "f:deployment.kubernetes.io/revision": {
                                        }
                                    }
                                },
                                "f:status": {
                                    "f:availableReplicas": {
                                    },
                                    "f:conditions": {
                                        ".": {
                                        },
                                        "k:{\"type\":\"Available\"}": {
                                            ".": {
                                            },
                                            "f:lastTransitionTime": {
                                            },
                                            "f:lastUpdateTime": {
                                            },
                                            "f:message": {
                                            },
                                            "f:reason": {
                                            },
                                            "f:status": {
                                            },
                                            "f:type": {
                                            }
                                        },
                                        "k:{\"type\":\"Progressing\"}": {
                                            ".": {
                                            },
                                            "f:lastTransitionTime": {
                                            },
                                            "f:lastUpdateTime": {
                                            },
                                            "f:message": {
                                            },
                                            "f:reason": {
                                            },
                                            "f:status": {
                                            },
                                            "f:type": {
                                            }
                                        }
                                    },
                                    "f:observedGeneration": {
                                    },
                                    "f:readyReplicas": {
                                    },
                                    "f:replicas": {
                                    },
                                    "f:updatedReplicas": {
                                    }
                                }
                            }
                        },
                        "manager": "kube-controller-manager",
                        "operation": "Update",
                        "time": "2020-10-18T16:28:46Z"
                    },
                    {
                        "additionalProperties": {
                        },
                        "apiVersion": "apps/v1",
                        "fieldsType": "FieldsV1",
                        "fieldsV1": {
                            "additionalProperties": {
                                "f:metadata": {
                                    "f:labels": {
                                        "f:foo": {
                                        }
                                    }
                                }
                            }
                        },
                        "manager": "kubectl-label",
                        "operation": "Update",
                        "time": "2020-10-18T16:28:54Z"
                    }
                ],
                "name": "httpbin",
                "namespace": "test-admission",
                "ownerReferences": [
                ],
                "resourceVersion": "6671",
                "uid": "21c2d418-01c5-431d-bef1-19db4c8d6b7f"
            },
            "spec": {
                "additionalProperties": {
                },
                "progressDeadlineSeconds": 600,
                "replicas": 1,
                "revisionHistoryLimit": 10,
                "selector": {
                    "additionalProperties": {
                    },
                    "matchExpressions": [
                    ],
                    "matchLabels": {
                        "app": "httpbin"
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
                            "kind": 1,
                            "strVal": "25%"
                        },
                        "maxUnavailable": {
                            "additionalProperties": {
                            },
                            "kind": 1,
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
                            "app": "httpbin"
                        },
                        "managedFields": [
                        ],
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
                                            "amount": "100",
                                            "format": "m"
                                        },
                                        "memory": {
                                            "additionalProperties": {
                                            },
                                            "amount": "200",
                                            "format": ""
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
                        "ephemeralContainers": [
                        ],
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
                        "topologySpreadConstraints": [
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
                        "lastTransitionTime": "2020-10-18T16:28:46Z",
                        "lastUpdateTime": "2020-10-18T16:28:46Z",
                        "message": "Deployment has minimum availability.",
                        "reason": "MinimumReplicasAvailable",
                        "status": "True",
                        "type": "Available"
                    },
                    {
                        "additionalProperties": {
                        },
                        "lastTransitionTime": "2020-10-18T16:28:43Z",
                        "lastUpdateTime": "2020-10-18T16:28:46Z",
                        "message": "ReplicaSet \"httpbin-5644b4777\" has successfully progressed.",
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
            "apiVersion": "apps/v1",
            "kind": "Deployment",
            "metadata": {
                "additionalProperties": {
                },
                "annotations": {
                    "deployment.kubernetes.io/revision": "1",
                    "kubectl.kubernetes.io/last-applied-configuration": "{\"apiVersion\":\"apps/v1\",\"kind\":\"Deployment\",\"metadata\":{\"annotations\":{},\"labels\":{\"app\":\"httpbin\"},\"name\":\"httpbin\",\"namespace\":\"test-admission\"},\"spec\":{\"replicas\":1,\"selector\":{\"matchLabels\":{\"app\":\"httpbin\"}},\"template\":{\"metadata\":{\"labels\":{\"app\":\"httpbin\"}},\"spec\":{\"containers\":[{\"command\":[\"sleep\",\"3600\"],\"image\":\"scrapinghub/httpbin:latest\",\"name\":\"httpbin\",\"resources\":{\"requests\":{\"cpu\":0.1,\"memory\":200}}}]}}}}\n"
                },
                "creationTimestamp": "2020-10-18T16:28:43Z",
                "finalizers": [
                ],
                "generation": 1,
                "labels": {
                    "app": "httpbin"
                },
                "managedFields": [
                ],
                "name": "httpbin",
                "namespace": "test-admission",
                "ownerReferences": [
                ],
                "resourceVersion": "6671",
                "uid": "21c2d418-01c5-431d-bef1-19db4c8d6b7f"
            },
            "spec": {
                "additionalProperties": {
                },
                "progressDeadlineSeconds": 600,
                "replicas": 1,
                "revisionHistoryLimit": 10,
                "selector": {
                    "additionalProperties": {
                    },
                    "matchExpressions": [
                    ],
                    "matchLabels": {
                        "app": "httpbin"
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
                            "kind": 1,
                            "strVal": "25%"
                        },
                        "maxUnavailable": {
                            "additionalProperties": {
                            },
                            "kind": 1,
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
                            "app": "httpbin"
                        },
                        "managedFields": [
                        ],
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
                                            "amount": "100",
                                            "format": "m"
                                        },
                                        "memory": {
                                            "additionalProperties": {
                                            },
                                            "amount": "200",
                                            "format": ""
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
                        "ephemeralContainers": [
                        ],
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
                        "topologySpreadConstraints": [
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
                        "lastTransitionTime": "2020-10-18T16:28:46Z",
                        "lastUpdateTime": "2020-10-18T16:28:46Z",
                        "message": "Deployment has minimum availability.",
                        "reason": "MinimumReplicasAvailable",
                        "status": "True",
                        "type": "Available"
                    },
                    {
                        "additionalProperties": {
                        },
                        "lastTransitionTime": "2020-10-18T16:28:43Z",
                        "lastUpdateTime": "2020-10-18T16:28:46Z",
                        "message": "ReplicaSet \"httpbin-5644b4777\" has successfully progressed.",
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
        "options": {
            "additionalProperties": {
            },
            "apiVersion": "meta.k8s.io/v1",
            "dryRun": [
            ],
            "fieldManager": "kubectl-label",
            "kind": "UpdateOptions"
        },
        "requestKind": {
            "additionalProperties": {
            },
            "group": "apps",
            "kind": "Deployment",
            "version": "v1"
        },
        "requestResource": {
            "additionalProperties": {
            },
            "group": "apps",
            "resource": "deployments",
            "version": "v1"
        },
        "resource": {
            "additionalProperties": {
            },
            "group": "apps",
            "resource": "deployments",
            "version": "v1"
        },
        "uid": "fffffd16-ed50-4f7e-bce0-301d8fb93938",
        "userInfo": {
            "additionalProperties": {
            },
            "groups": [
                "system:masters",
                "system:authenticated"
            ],
            "username": "minikube-user"
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
