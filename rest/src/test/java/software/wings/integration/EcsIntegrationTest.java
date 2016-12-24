package software.wings.integration;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ecs.AmazonECSClient;
import com.amazonaws.services.ecs.model.CreateClusterRequest;
import com.amazonaws.services.ecs.model.CreateClusterResult;
import com.amazonaws.services.ecs.model.CreateServiceRequest;
import com.amazonaws.services.ecs.model.CreateServiceResult;
import com.amazonaws.services.ecs.model.DescribeClustersRequest;
import com.amazonaws.services.ecs.model.ListContainerInstancesRequest;
import com.amazonaws.services.ecs.model.RegisterTaskDefinitionRequest;
import com.amazonaws.services.ecs.model.RegisterTaskDefinitionResult;
import com.amazonaws.services.ecs.model.UpdateServiceRequest;
import com.amazonaws.services.ecs.model.UpdateServiceResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

/**
 * Created by anubhaw on 12/23/16.
 */

@Ignore
public class EcsIntegrationTest {
  private AmazonECSClient ecsClient =
      new AmazonECSClient(new BasicAWSCredentials("AKIAJLEKM45P4PO5QUFQ", "nU8xaNacU65ZBdlNxfXvKM2Yjoda7pQnNP3fClVE"));
  private ObjectMapper mapper = new ObjectMapper();

  @Test
  public void shouldDescribeCluster() {
    List<String> clusterArns = ecsClient.listClusters().getClusterArns();
    clusterArns.stream()
        .flatMap(arn
            -> ecsClient.describeClusters(new DescribeClustersRequest().withClusters(clusterArns))
                   .getClusters()
                   .stream())
        .forEach(cluster -> System.out.println("Cluster : " + cluster));
  }

  @Test
  public void shouldDescribeContainerInstances() {
    List<String> clusterArns = ecsClient.listClusters().getClusterArns();
    clusterArns.stream()
        .flatMap(arn
            -> ecsClient.listContainerInstances(new ListContainerInstancesRequest().withCluster(arn))
                   .getContainerInstanceArns()
                   .stream())
        .forEach(containInstance -> System.out.println("Container Instance : " + containInstance));
  }

  @Test
  public void shouldCreateCluster() {
    CreateClusterRequest demo2 = new CreateClusterRequest().withClusterName("Demo2");
    CreateClusterResult demo2Cluster = ecsClient.createCluster(demo2);
    System.out.println(demo2Cluster);
  }

  @Test
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
     */

    String josn =
        "{\"containerDefinitions\":[{\"name\":\"tomcat\",\"image\":\"tomcat\",\"cpu\":1,\"command\":[\"catalina.sh\",\"run\"],\"portMappings\":[{\"hostPort\":80,\"containerPort\":8080,\"protocol\":\"tcp\"}],\"memory\":100,\"essential\":true}],\"family\":\"tomcat\"}";
    RegisterTaskDefinitionRequest registerTaskDefinitionRequest =
        mapper.readValue(josn, RegisterTaskDefinitionRequest.class);
    RegisterTaskDefinitionResult registerTaskDefinitionResult =
        ecsClient.registerTaskDefinition(registerTaskDefinitionRequest);
    System.out.println(registerTaskDefinitionResult);
  }

  @Test
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
    System.out.println(service);
  }

  @Test
  public void shouldUpdateService() throws IOException {
    String serviceJson =
        "{\"cluster\":\"demo\",\"desiredCount\":1,\"service\":\"tomcat\",\"taskDefinition\":\"tomcat:7\"}";
    UpdateServiceRequest updateServiceRequest = mapper.readValue(serviceJson, UpdateServiceRequest.class);
    UpdateServiceResult updateServiceResult = ecsClient.updateService(updateServiceRequest);
    System.out.println(updateServiceResult);
  }

  @Test
  public void shouldScaleUpTheService() throws IOException {
    String serviceJson =
        "{\"cluster\":\"demo\",\"desiredCount\":10,\"service\":\"tomcat\",\"taskDefinition\":\"tomcat:6\"}";
    UpdateServiceRequest updateServiceRequest = mapper.readValue(serviceJson, UpdateServiceRequest.class);
    UpdateServiceResult updateServiceResult = ecsClient.updateService(updateServiceRequest);
    System.out.println(updateServiceResult);
  }

  @Test
  public void shouldScaleDownTheService() throws IOException {
    String serviceJson =
        "{\"cluster\":\"demo\",\"desiredCount\":0,\"service\":\"tomcat\",\"taskDefinition\":\"tomcat:6\"}";
    UpdateServiceRequest updateServiceRequest = mapper.readValue(serviceJson, UpdateServiceRequest.class);
    UpdateServiceResult updateServiceResult = ecsClient.updateService(updateServiceRequest);
    System.out.println(updateServiceResult);
  }
}
