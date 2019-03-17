package software.wings.beans.container;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.amazonaws.services.ecs.model.TaskDefinition;
import io.harness.category.element.UnitTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class EcsContainerTaskTest {
  public static final String CONTAINER_NAME = "containerName";
  public static final String IMAGE_NAME = "imageName";
  public static final String EXEC_ROLE = "exec_role";
  public static final String DOMAIN_NAME = "domain.name.co";

  @Test
  @Category(UnitTests.class)
  public void testcreateTaskDefinition() {
    EcsContainerTask ecsContainerTask = new EcsContainerTask();
    ecsContainerTask.setAdvancedConfig(null);
    PortMapping portMapping = PortMapping.builder().containerPort(80).build();
    ContainerDefinition containerDefinition =
        ContainerDefinition.builder()
            .cpu(256)
            .memory(1024)
            .portMappings(asList(portMapping))
            .logConfiguration(LogConfiguration.builder().logDriver("awslog").build())
            .build();

    ecsContainerTask.setContainerDefinitions(asList(containerDefinition));

    TaskDefinition taskDefinition =
        ecsContainerTask.createTaskDefinition(CONTAINER_NAME, IMAGE_NAME, EXEC_ROLE, DOMAIN_NAME);

    assertNotNull(taskDefinition);
    assertEquals(1, taskDefinition.getContainerDefinitions().size());
    assertEquals("256", taskDefinition.getCpu());
    assertEquals("1024", taskDefinition.getMemory());
    assertEquals(EXEC_ROLE, taskDefinition.getExecutionRoleArn());

    com.amazonaws.services.ecs.model.ContainerDefinition containerDefinitionAws =
        taskDefinition.getContainerDefinitions().get(0);
    assertNotNull(containerDefinitionAws);
    assertEquals(CONTAINER_NAME, containerDefinitionAws.getName());
    assertEquals(DOMAIN_NAME + "/" + IMAGE_NAME, containerDefinitionAws.getImage());
    assertEquals(256, containerDefinitionAws.getCpu().intValue());
    assertEquals(1024, containerDefinitionAws.getMemory().intValue());
    assertNotNull(containerDefinitionAws.getPortMappings());
    assertEquals(1, containerDefinitionAws.getPortMappings().size());
    assertEquals(80, containerDefinitionAws.getPortMappings().iterator().next().getContainerPort().intValue());

    taskDefinition = ecsContainerTask.createTaskDefinition(CONTAINER_NAME, IMAGE_NAME, null, DOMAIN_NAME);

    assertNotNull(taskDefinition);
    assertEquals(1, taskDefinition.getContainerDefinitions().size());
    assertEquals("256", taskDefinition.getCpu());
    assertEquals("1024", taskDefinition.getMemory());
    assertEquals("", taskDefinition.getExecutionRoleArn());

    containerDefinitionAws = taskDefinition.getContainerDefinitions().get(0);
    assertNotNull(containerDefinitionAws);
    assertEquals(CONTAINER_NAME, containerDefinitionAws.getName());
    assertEquals(DOMAIN_NAME + "/" + IMAGE_NAME, containerDefinitionAws.getImage());
    assertEquals(256, containerDefinitionAws.getCpu().intValue());
    assertEquals(1024, containerDefinitionAws.getMemory().intValue());
    assertNotNull(containerDefinitionAws.getPortMappings());
    assertEquals(1, containerDefinitionAws.getPortMappings().size());
    assertEquals(80, containerDefinitionAws.getPortMappings().iterator().next().getContainerPort().intValue());

    containerDefinition = ContainerDefinition.builder()
                              .portMappings(asList(portMapping))
                              .logConfiguration(LogConfiguration.builder().logDriver("awslog").build())
                              .build();

    ecsContainerTask.setContainerDefinitions(asList(containerDefinition));
    taskDefinition = ecsContainerTask.createTaskDefinition(CONTAINER_NAME, IMAGE_NAME, EXEC_ROLE, DOMAIN_NAME);
    assertNotNull(taskDefinition);
  }

  @Test
  @Category(UnitTests.class)
  public void testCreateTaskDefinitionWithWhenNoMemoryProvided() {
    EcsContainerTask ecsContainerTask = new EcsContainerTask();
    ecsContainerTask.setAdvancedConfig(null);
    PortMapping portMapping = PortMapping.builder().containerPort(80).build();
    ContainerDefinition containerDefinition =
        ContainerDefinition.builder()
            .cpu(256)
            .portMappings(asList(portMapping))
            .logConfiguration(LogConfiguration.builder().logDriver("awslog").build())
            .build();

    ecsContainerTask.setContainerDefinitions(asList(containerDefinition));

    TaskDefinition taskDefinition =
        ecsContainerTask.createTaskDefinition(CONTAINER_NAME, IMAGE_NAME, EXEC_ROLE, DOMAIN_NAME);

    assertNotNull(taskDefinition);
    assertEquals(1, taskDefinition.getContainerDefinitions().size());
    assertEquals("256", taskDefinition.getCpu());
    assertEquals(EXEC_ROLE, taskDefinition.getExecutionRoleArn());

    com.amazonaws.services.ecs.model.ContainerDefinition containerDefinitionAws =
        taskDefinition.getContainerDefinitions().get(0);
    assertNotNull(containerDefinitionAws);
    assertEquals(CONTAINER_NAME, containerDefinitionAws.getName());
    assertEquals(DOMAIN_NAME + "/" + IMAGE_NAME, containerDefinitionAws.getImage());
    assertEquals(256, containerDefinitionAws.getCpu().intValue());
    assertEquals(1024, containerDefinitionAws.getMemory().intValue());
    assertNotNull(containerDefinitionAws.getPortMappings());
    assertEquals(1, containerDefinitionAws.getPortMappings().size());
    assertEquals(80, containerDefinitionAws.getPortMappings().iterator().next().getContainerPort().intValue());

    taskDefinition = ecsContainerTask.createTaskDefinition(CONTAINER_NAME, IMAGE_NAME, null, DOMAIN_NAME);

    assertNotNull(taskDefinition);
    assertEquals(1, taskDefinition.getContainerDefinitions().size());
    assertEquals("256", taskDefinition.getCpu());
    assertEquals("", taskDefinition.getExecutionRoleArn());

    containerDefinitionAws = taskDefinition.getContainerDefinitions().get(0);
    assertNotNull(containerDefinitionAws);
    assertEquals(CONTAINER_NAME, containerDefinitionAws.getName());
    assertEquals(DOMAIN_NAME + "/" + IMAGE_NAME, containerDefinitionAws.getImage());
    assertEquals(256, containerDefinitionAws.getCpu().intValue());
    assertEquals(1024, containerDefinitionAws.getMemory().intValue());
    assertNotNull(containerDefinitionAws.getPortMappings());
    assertEquals(1, containerDefinitionAws.getPortMappings().size());
    assertEquals(80, containerDefinitionAws.getPortMappings().iterator().next().getContainerPort().intValue());

    containerDefinition = ContainerDefinition.builder()
                              .portMappings(asList(portMapping))
                              .logConfiguration(LogConfiguration.builder().logDriver("awslog").build())
                              .build();

    ecsContainerTask.setContainerDefinitions(asList(containerDefinition));
    taskDefinition = ecsContainerTask.createTaskDefinition(CONTAINER_NAME, IMAGE_NAME, EXEC_ROLE, DOMAIN_NAME);
    assertNotNull(taskDefinition);
  }

  @Test
  @Category(UnitTests.class)
  public void testRemoveEmptySecretsContainerDefinitionString() throws Exception {
    String str = "{\n"
        + "  \"containerDefinitions\" : [ {\n"
        + "    \"name\" : \"${CONTAINER_NAME}\",\n"
        + "    \"image\" : \"${DOCKER_IMAGE_NAME}\",\n"
        + "    \"cpu\" : 1,\n"
        + "    \"memory\" : 512,\n"
        + "    \"links\" : [ ],\n"
        + "    \"portMappings\" : [ {\n"
        + "      \"containerPort\" : 8080,\n"
        + "      \"protocol\" : \"tcp\"\n"
        + "    } ],\n"
        + "    \"entryPoint\" : [ ],\n"
        + "    \"command\" : [ ],\n"
        + "    \"environment\" : [ ],\n"
        + "    \"mountPoints\" : [ ],\n"
        + "    \"volumesFrom\" : [ ],\n"
        + "    \"secrets\" : [ ],\n"
        + "    \"dnsServers\" : [ ],\n"
        + "    \"dnsSearchDomains\" : [ ],\n"
        + "    \"extraHosts\" : [ ],\n"
        + "    \"dockerSecurityOptions\" : [ ],\n"
        + "    \"ulimits\" : [ ],\n"
        + "    \"systemControls\" : [ ]\n"
        + "  } ],\n"
        + "  \"executionRoleArn\" : \"${EXECUTION_ROLE}\",\n"
        + "  \"volumes\" : [ ],\n"
        + "  \"requiresAttributes\" : [ ],\n"
        + "  \"placementConstraints\" : [ ],\n"
        + "  \"compatibilities\" : [ ],\n"
        + "  \"requiresCompatibilities\" : [ ],\n"
        + "  \"cpu\" : \"1\",\n"
        + "  \"memory\" : \"512\"\n"
        + "}";

    EcsContainerTask ecsContainerTask = new EcsContainerTask();
    str = ecsContainerTask.removeEmptySecretsContainerDefinitionString(str);
    assertTrue(!str.contains("\"secrets\" : [ ],"));

    str = "{\n"
        + "  \"containerDefinitions\" : [ {\n"
        + "    \"name\" : \"${CONTAINER_NAME}\",\n"
        + "    \"image\" : \"${DOCKER_IMAGE_NAME}\",\n"
        + "    \"cpu\" : 1,\n"
        + "    \"memory\" : 512,\n"
        + "    \"links\" : [ ],\n"
        + "    \"portMappings\" : [ {\n"
        + "      \"containerPort\" : 8080,\n"
        + "      \"protocol\" : \"tcp\"\n"
        + "    } ],\n"
        + "    \"entryPoint\" : [ ],\n"
        + "    \"command\" : [ ],\n"
        + "    \"environment\" : [ ],\n"
        + "    \"mountPoints\" : [ ],\n"
        + "    \"volumesFrom\" : [ ],\n"
        + "    \"secrets\" : [],\n"
        + "    \"dnsServers\" : [ ],\n"
        + "    \"dnsSearchDomains\" : [ ],\n"
        + "    \"extraHosts\" : [ ],\n"
        + "    \"dockerSecurityOptions\" : [ ],\n"
        + "    \"ulimits\" : [ ],\n"
        + "    \"systemControls\" : [ ]\n"
        + "  } ],\n"
        + "  \"executionRoleArn\" : \"${EXECUTION_ROLE}\",\n"
        + "  \"volumes\" : [ ],\n"
        + "  \"requiresAttributes\" : [ ],\n"
        + "  \"placementConstraints\" : [ ],\n"
        + "  \"compatibilities\" : [ ],\n"
        + "  \"requiresCompatibilities\" : [ ],\n"
        + "  \"cpu\" : \"1\",\n"
        + "  \"memory\" : \"512\"\n"
        + "}";

    str = ecsContainerTask.removeEmptySecretsContainerDefinitionString(str);
    assertTrue(!str.contains("\"secrets\" : [ ],"));

    str = "{\n"
        + "  \"containerDefinitions\" : [ {\n"
        + "    \"name\" : \"${CONTAINER_NAME}\",\n"
        + "    \"image\" : \"${DOCKER_IMAGE_NAME}\",\n"
        + "    \"cpu\" : 1,\n"
        + "    \"memory\" : 512,\n"
        + "    \"links\" : [ ],\n"
        + "    \"portMappings\" : [ {\n"
        + "      \"containerPort\" : 8080,\n"
        + "      \"protocol\" : \"tcp\"\n"
        + "    } ],\n"
        + "    \"entryPoint\" : [ ],\n"
        + "    \"command\" : [ ],\n"
        + "    \"environment\" : [ ],\n"
        + "    \"mountPoints\" : [ ],\n"
        + "    \"volumesFrom\" : [ ],\n"
        + "    \"secrets\":[ ],\n"
        + "    \"dnsServers\" : [ ],\n"
        + "    \"dnsSearchDomains\" : [ ],\n"
        + "    \"extraHosts\" : [ ],\n"
        + "    \"dockerSecurityOptions\" : [ ],\n"
        + "    \"ulimits\" : [ ],\n"
        + "    \"systemControls\" : [ ]\n"
        + "  } ],\n"
        + "  \"executionRoleArn\" : \"${EXECUTION_ROLE}\",\n"
        + "  \"volumes\" : [ ],\n"
        + "  \"requiresAttributes\" : [ ],\n"
        + "  \"placementConstraints\" : [ ],\n"
        + "  \"compatibilities\" : [ ],\n"
        + "  \"requiresCompatibilities\" : [ ],\n"
        + "  \"cpu\" : \"1\",\n"
        + "  \"memory\" : \"512\"\n"
        + "}";

    str = ecsContainerTask.removeEmptySecretsContainerDefinitionString(str);
    assertTrue(!str.contains("\"secrets\" : [ ],"));

    str = "{\n"
        + "  \"containerDefinitions\" : [ {\n"
        + "    \"name\" : \"${CONTAINER_NAME}\",\n"
        + "    \"image\" : \"${DOCKER_IMAGE_NAME}\",\n"
        + "    \"cpu\" : 1,\n"
        + "    \"memory\" : 512,\n"
        + "    \"links\" : [ ],\n"
        + "    \"portMappings\" : [ {\n"
        + "      \"containerPort\" : 8080,\n"
        + "      \"protocol\" : \"tcp\"\n"
        + "    } ],\n"
        + "    \"entryPoint\" : [ ],\n"
        + "    \"command\" : [ ],\n"
        + "    \"environment\" : [ ],\n"
        + "    \"mountPoints\" : [ ],\n"
        + "    \"volumesFrom\" : [ ],\n"
        + "    \"dnsServers\" : [ ],\n"
        + "    \"dnsSearchDomains\" : [ ],\n"
        + "    \"extraHosts\" : [ ],\n"
        + "    \"dockerSecurityOptions\" : [ ],\n"
        + "    \"ulimits\" : [ ],\n"
        + "    \"systemControls\" : [ ]\n"
        + "  } ],\n"
        + "  \"executionRoleArn\" : \"${EXECUTION_ROLE}\",\n"
        + "  \"volumes\" : [ ],\n"
        + "  \"requiresAttributes\" : [ ],\n"
        + "  \"placementConstraints\" : [ ],\n"
        + "  \"compatibilities\" : [ ],\n"
        + "  \"requiresCompatibilities\" : [ ],\n"
        + "  \"cpu\" : \"1\",\n"
        + "  \"memory\" : \"512\"\n"
        + "}";

    str = ecsContainerTask.removeEmptySecretsContainerDefinitionString(str);
    assertTrue(!str.contains("\"secrets\" : [ ],"));
  }
}
