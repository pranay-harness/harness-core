{
  "ipcMode": null,
  "executionRoleArn": null,
  "containerDefinitions": [
    {
      "dnsSearchDomains": [],
      "entryPoint": [],
      "portMappings": [
        {
          "hostPort": 8080,
          "protocol": "tcp",
          "containerPort": 8080
        }
      ],
      "command": [],
      "linuxParameters": null,
      "cpu": 1,
      "environment": [
        {
          "name": "ACCOUNT_ID",
          "value": "${accountId}"
        },
        {
          "name": "ACCOUNT_SECRET",
          "value": "${accountSecret}"
        },
        {
          "name": "DELEGATE_CHECK_LOCATION",
          "value": "${delegateCheckLocation}"
        },
        {
          "name": "DELEGATE_STORAGE_URL",
          "value": "${delegateStorageUrl}"
        },
        {
          "name": "DELEGATE_TYPE",
          "value": "ECS"
        },
        {
          "name": "DELEGATE_GROUP_NAME",
          "value": "${delegateGroupName}"
        },

        {
          "name": "DEPLOY_MODE",
          "value": "${deployMode}"
        },
        {
          "name": "MANAGER_HOST_AND_PORT",
          "value": "${managerHostAndPort}"
        },
        {
          "name": "POLL_FOR_TASKS",
          "value": "false"
        },
        {
          "name": "WATCHER_CHECK_LOCATION",
          "value": "${watcherCheckLocation}"
        },
        {
          "name": "WATCHER_STORAGE_URL",
          "value": "${watcherStorageUrl}"
        }
      ],
      "ulimits": null,
      "dnsServers": [],
      "mountPoints": [],
      "workingDirectory": null,
      "secrets": null,
      "dockerSecurityOptions": [],
      "memory": 6144,
      "memoryReservation": null,
      "volumesFrom": [],
      "image": "${delegateDockerImage}",
      "disableNetworking": null,
      "interactive": null,
      "healthCheck": null,
      "essential": true,
      "links": [],
      "hostname": ${hostnameForDelegate},
      "extraHosts": null,
      "pseudoTerminal": null,
      "user": null,
      "readonlyRootFilesystem": null,
      "dockerLabels": null,
      "systemControls": null,
      "privileged": null,
      "name": "ecs-delegate"
    }
  ],
  "placementConstraints": [],
  "memory": "6144",
  "taskRoleArn": null,
  "pidMode": null,
  "requiresCompatibilities": [
    "EC2"
  ],
  "networkMode": ${networkModeForTask},
  "cpu": "1024",
  "volumes": [],
  "family": "harness-delegate-task-spec"
}