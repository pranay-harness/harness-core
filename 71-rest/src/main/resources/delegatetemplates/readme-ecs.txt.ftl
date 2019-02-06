Change Defaults
---------------

To change CPU, memory, portMappings, or hostName, edit the default values in harness-ecs-delegate.json.

You can also change any other JSON fields as needed.

Network Mode
------------

If you select "awsvpc" as network mode, the ECS console will request network configuration info when you run the Delegate task or service, including subnets, security groups, and public IP (for Fargate launch type).

Install and Run the ECS Delegate
--------------------------------

Ways to install and run ECS Delegate in the AWS ECS Console:

1. Create ECS tasks.
- Create a task defintion using harness-ecs-delegate.json.
- Create and run tasks using this task defintion.

2. Create ECS services. (Recommended. An ECS service will spin up a new ECS Delegate task if any ECS Delegate task goes down, thus maintaining a persistent ECS Delegate.)
- Create a task defintion using harness-ecs-delegate.json.
- Create ECS service using this task definition.
- Run the service.