/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.integration;

import static io.harness.rule.OwnerRule.ANUBHAW;

import io.harness.CategoryTest;
import io.harness.category.element.DeprecatedIntegrationTests;
import io.harness.rule.Owner;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.model.SetDesiredCapacityRequest;
import com.amazonaws.services.autoscaling.model.SetDesiredCapacityResult;
import com.amazonaws.services.autoscaling.model.UpdateAutoScalingGroupRequest;
import com.amazonaws.services.autoscaling.model.UpdateAutoScalingGroupResult;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.IamInstanceProfileSpecification;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ecs.AmazonECSClient;
import com.amazonaws.services.ecs.model.CreateClusterRequest;
import com.amazonaws.services.ecs.model.CreateClusterResult;
import com.amazonaws.services.ecs.model.CreateServiceRequest;
import com.amazonaws.services.ecs.model.CreateServiceResult;
import com.amazonaws.services.ecs.model.DescribeClustersRequest;
import com.amazonaws.services.ecs.model.DescribeServicesRequest;
import com.amazonaws.services.ecs.model.DescribeTasksRequest;
import com.amazonaws.services.ecs.model.DescribeTasksResult;
import com.amazonaws.services.ecs.model.ListContainerInstancesRequest;
import com.amazonaws.services.ecs.model.ListServicesRequest;
import com.amazonaws.services.ecs.model.ListTasksRequest;
import com.amazonaws.services.ecs.model.ListTasksResult;
import com.amazonaws.services.ecs.model.RegisterTaskDefinitionRequest;
import com.amazonaws.services.ecs.model.RegisterTaskDefinitionResult;
import com.amazonaws.services.ecs.model.Service;
import com.amazonaws.services.ecs.model.UpdateServiceRequest;
import com.amazonaws.services.ecs.model.UpdateServiceResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Created by anubhaw on 12/23/16.
 */

@Slf4j
public class EcsIntegrationTest extends CategoryTest {
  private AmazonECSClient ecsClient =
      new AmazonECSClient(new BasicAWSCredentials("AKIAJLEKM45P4PO5QUFQ", "nU8xaNacU65ZBdlNxfXvKM2Yjoda7pQnNP3fClVE"));
  private ObjectMapper mapper = new ObjectMapper();

  @Test
  @Owner(developers = ANUBHAW)
  @Category(DeprecatedIntegrationTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void shouldDescribeCluster() {
    List<String> clusterArns = ecsClient.listClusters().getClusterArns();
    clusterArns.stream()
        .flatMap(arn
            -> ecsClient.describeClusters(new DescribeClustersRequest().withClusters(clusterArns))
                   .getClusters()
                   .stream())
        .forEach(cluster -> log.info("Cluster : " + cluster));
  }

  @Test
  @Owner(developers = ANUBHAW)
  @Category(DeprecatedIntegrationTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void shouldDescribeContainerInstances() {
    List<String> clusterArns = ecsClient.listClusters().getClusterArns();
    clusterArns.stream()
        .flatMap(arn
            -> ecsClient.listContainerInstances(new ListContainerInstancesRequest().withCluster(arn))
                   .getContainerInstanceArns()
                   .stream())
        .forEach(containInstance -> log.info("Container Instance : " + containInstance));
  }

  @Test
  @Owner(developers = ANUBHAW)
  @Category(DeprecatedIntegrationTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void shouldCreateCluster() {
    CreateClusterRequest demo2 = new CreateClusterRequest().withClusterName("Demo2");
    CreateClusterResult demo2Cluster = ecsClient.createCluster(demo2);
    log.info(demo2Cluster.toString());
  }

  @Test
  @Owner(developers = ANUBHAW)
  @Category(DeprecatedIntegrationTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void shouldFetchContainerInstanceForService() {
    ListTasksResult listTasksResult =
        ecsClient.listTasks(new ListTasksRequest().withCluster("test2").withServiceName("Nix__docker__Development__6"));
    List<String> taskArns = listTasksResult.getTaskArns();
    DescribeTasksResult describeTasksResult = ecsClient.describeTasks(new DescribeTasksRequest().withTasks(taskArns));
    log.info(taskArns.toString());
  }

  @Test
  @Owner(developers = ANUBHAW)
  @Category(DeprecatedIntegrationTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void shouldCreateTaskDefinition() throws IOException {
    /*
    {
    "containerDefinitions": [{
        "name": "tomcat",
        "image": "tomcat",
        "cpu": 1,
        "command": ["catalina.sh", "run"],
        "portMappings": [{
            "hostPort": 80,
            "containerPort": 8080,
            "protocol": "tcp"
        }],
        "memory": 1000,
        "essential": true
    }],
    "family": "tomcat"
    }
}
     */

    String josn =
        "{\"containerDefinitions\":[{\"name\":\"tomcat\",\"image\":\"tomcat\",\"cpu\":1,\"command\":[\"catalina.sh\",\"run\"],\"portMappings\":[{\"hostPort\":80,\"containerPort\":8080,\"protocol\":\"tcp\"}],\"memory\":100,\"essential\":true}],\"family\":\"tomcat\"}";
    RegisterTaskDefinitionRequest registerTaskDefinitionRequest =
        mapper.readValue(josn, RegisterTaskDefinitionRequest.class);
    RegisterTaskDefinitionResult registerTaskDefinitionResult =
        ecsClient.registerTaskDefinition(registerTaskDefinitionRequest);
    log.info(registerTaskDefinitionResult.toString());
  }

  @Test
  @Owner(developers = ANUBHAW)
  @Category(DeprecatedIntegrationTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void shouldCreateService() throws IOException {
    /*
    {
    "cluster": "demo",
    "desiredCount": 1,
    "serviceName": "tomcat",
    "taskDefinition": "tomcat:2"
     */

    String serviceJson =
        "{\"cluster\":\"demo\",\"desiredCount\":1,\"serviceName\":\"tomcat\",\"taskDefinition\":\"tomcat:6\"}";
    CreateServiceRequest createServiceRequest = mapper.readValue(serviceJson, CreateServiceRequest.class);

    CreateServiceResult service = ecsClient.createService(createServiceRequest);
    log.info(service.toString());
  }

  @Test
  @Owner(developers = ANUBHAW)
  @Category(DeprecatedIntegrationTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void shouldUpdateService() throws IOException {
    String serviceJson =
        "{\"cluster\":\"demo\",\"desiredCount\":1,\"service\":\"tomcat\",\"taskDefinition\":\"tomcat:7\"}";
    UpdateServiceRequest updateServiceRequest = mapper.readValue(serviceJson, UpdateServiceRequest.class);
    UpdateServiceResult updateServiceResult = ecsClient.updateService(updateServiceRequest);
    log.info(updateServiceResult.toString());
  }

  @Test
  @Owner(developers = ANUBHAW)
  @Category(DeprecatedIntegrationTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void shouldScaleUpTheService() throws IOException {
    String serviceJson =
        "{\"cluster\":\"demo\",\"desiredCount\":10,\"service\":\"tomcat\",\"taskDefinition\":\"tomcat:6\"}";
    UpdateServiceRequest updateServiceRequest = mapper.readValue(serviceJson, UpdateServiceRequest.class);
    UpdateServiceResult updateServiceResult = ecsClient.updateService(updateServiceRequest);
    log.info(updateServiceResult.toString());
  }

  @Test
  @Owner(developers = ANUBHAW)
  @Category(DeprecatedIntegrationTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void shouldScaleDownTheService() throws IOException {
    String serviceJson =
        "{\"cluster\":\"demo\",\"desiredCount\":0,\"service\":\"tomcat\",\"taskDefinition\":\"tomcat:6\"}";
    UpdateServiceRequest updateServiceRequest = mapper.readValue(serviceJson, UpdateServiceRequest.class);
    UpdateServiceResult updateServiceResult = ecsClient.updateService(updateServiceRequest);
    log.info(updateServiceResult.toString());
  }

  @Test
  @Owner(developers = ANUBHAW)
  @Category(DeprecatedIntegrationTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void shouldAddContainerInstance() {
    AmazonEC2Client amazonEC2Client = new AmazonEC2Client(
        new BasicAWSCredentials("AKIAJLEKM45P4PO5QUFQ", "nU8xaNacU65ZBdlNxfXvKM2Yjoda7pQnNP3fClVE"));
    amazonEC2Client.setRegion(Region.getRegion(Regions.US_EAST_1));

    RunInstancesRequest runInstancesRequest =
        new RunInstancesRequest()
            .withInstanceType("t2.small")
            .withImageId("ami-6df8fe7a")
            .withMinCount(1)
            .withMaxCount(1)
            .withIamInstanceProfile(new IamInstanceProfileSpecification().withArn(
                "arn:aws:iam::830767422336:instance-profile/ecsInstanceRole"))
            .withUserData(
                Base64.encodeBase64String("#!/bin/bash\necho ECS_CLUSTER=demo >> /etc/ecs/ecs.config".getBytes()));

    RunInstancesResult runInstancesResult = amazonEC2Client.runInstances(runInstancesRequest);
    log.info(runInstancesRequest.toString());
  }

  @Test
  @Owner(developers = ANUBHAW)
  @Category(DeprecatedIntegrationTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void shouldScaleContainerInstanceCluster() {
    AmazonAutoScalingClient amazonAutoScalingClient = new AmazonAutoScalingClient(
        new BasicAWSCredentials("AKIAJLEKM45P4PO5QUFQ", "nU8xaNacU65ZBdlNxfXvKM2Yjoda7pQnNP3fClVE"));
    UpdateAutoScalingGroupResult updateAutoScalingGroupResult = amazonAutoScalingClient.updateAutoScalingGroup(
        new UpdateAutoScalingGroupRequest()
            .withAutoScalingGroupName("EC2ContainerService-demo-EcsInstanceAsg-1TUMY9AGURFZC")
            .withMaxSize(10));
    log.info(updateAutoScalingGroupResult.toString());
    SetDesiredCapacityResult setDesiredCapacityResult = amazonAutoScalingClient.setDesiredCapacity(
        new SetDesiredCapacityRequest()
            .withAutoScalingGroupName("EC2ContainerService-demo-EcsInstanceAsg-1TUMY9AGURFZC")
            .withDesiredCapacity(5));
    log.info(setDesiredCapacityResult.toString());
  }

  @Test
  @Owner(developers = ANUBHAW)
  @Category(DeprecatedIntegrationTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void shouldWaitTillAllInstanceaAreReady() {
    //    AmazonAutoScalingClient amazonAutoScalingClient =
    //        new AmazonAutoScalingClient(new BasicAWSCredentials("AKIAJLEKM45P4PO5QUFQ",
    //        "nU8xaNacU65ZBdlNxfXvKM2Yjoda7pQnNP3fClVE"));

    log.info(
        "curl --data \"name=wings&password=wings123&password2=wings123\" http://localhost:${PORT}/todolist/register\ncurl -b cookies.txt -c cookies.txt --data \"name=wings&password=wings123\" http://localhost:${PORT}/todolist/requestLogin\ncurl -IL -b cookies.txt -c cookies.txt http://localhost:${PORT}/todolist/inside/display\nfor i in $(seq 10)\ndo\n    curl -IL -b cookies.txt -c cookies.txt \"http://localhost:${PORT}/todolist/inside/addTask?priority=1&task=task\"$i\n    sleep 0.1\ndone\nrm cookies.txt\n");
  }

  @Test
  @Owner(developers = ANUBHAW)
  @Category(DeprecatedIntegrationTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void shouldReturnServices() {
    AmazonECSClient amazonECSClient = new AmazonECSClient(
        new BasicAWSCredentials("AKIAJLEKM45P4PO5QUFQ", "nU8xaNacU65ZBdlNxfXvKM2Yjoda7pQnNP3fClVE"));

    List<String> serviceArns =
        amazonECSClient.listServices(new ListServicesRequest().withCluster("test2")).getServiceArns();
    List<Service> test2 =
        amazonECSClient.describeServices(new DescribeServicesRequest().withCluster("test2").withServices(serviceArns))
            .getServices();
  }
}
