Change Defaults
---------------

To change CPU, memory, portMappings, or hostName, edit the default values in ecs-task-spec.json.

You can also change any other JSON fields as needed.

Network Mode
------------

If you select "awsvpc" as network mode, the ECS console will request network configuration info when you run the Delegate task or service, including subnets, security groups, and public IP (for Fargate launch type).


FARGATE Launch type
-------------------

To run the ECS Delegate task with a FARGATE launch type, select "Use AWS VPC Mode" when downloading the Delegate.

Update the ECS task spec with following changes:
- Set "requiresCompatibilities" to "FARGATE" instead of "EC2"
- Add "executionRoleArn" to the ECS task spec. This is needed by FARGATE launch types.

Note: If you change the default CPU or memory settings, follow the steps in this document to avoid any invalid CPU
or memory errors: https://docs.aws.amazon.com/AmazonECS/latest/userguide/task-cpu-memory-error.html.

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




