Change Defaults
---------------

To change CPU, memory, portMappings, or hostName, edit the default values in harness-ecs-delegate.json.

You can also change any other JSON fields as needed.

Network Mode
------------

If you select "awsvpc" as network mode, the ECS console will request network configuration info when you run the Delegate task or service, including subnets, security groups, and public IP (for Fargate launch type).

Install and Run the ECS Delegate
--------------------------------

Install and run ECS Delegate in the AWS ECS Console:

 Register Task Defintion.
 - Register a task defintion using ecs-task-spec.json.
 - AWS CLI For Task Registration: "aws ecs register-task-definition --cli-input-json file://<$PATH>/ecs-task-spec.json"


 Run delegate task by one of the following ways.
 1. Create and run individual ECS tasks using above created task definition.

 2. Create ECS services. (Recommended. An ECS service will spin up a new ECS Delegate task if any ECS Delegate task goes down, thus maintaining a persistent ECS Delegate.)
    - Create a task defintion using harness-ecs-delegate.json.
    - Create ECS service using this task definition.
    - Run the service.
    - AWS CLI for creating service
        (Non-awsvpc mode): Execute "aws ecs create-service --service-name SERVICE_NAME --task-definition harness-delegate-task-spec --cluster CLUSTER_NAME --desired-count COUNT"

        (Awsvpc mode): Update file "service-spec-for-awsvpc-mode.json" with serviceName, cluster, desiredCount and NetworkConfiguration {subnet/security group}, and
                       Execute "aws ecs create-service --cli-input-json file://<$PATH>/service-spec-for-awsvpc-mode.json"




