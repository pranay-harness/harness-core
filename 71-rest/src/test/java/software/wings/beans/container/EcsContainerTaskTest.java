package software.wings.beans.container;

import static io.harness.rule.OwnerRule.ADWAIT;
import static io.harness.rule.OwnerRule.GEORGE;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import com.amazonaws.services.ecs.model.TaskDefinition;
import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class EcsContainerTaskTest extends CategoryTest {
  public static final String CONTAINER_NAME = "containerName";
  public static final String IMAGE_NAME = "imageName";
  public static final String EXEC_ROLE = "exec_role";
  public static final String DOMAIN_NAME = "domain.name.co";

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testcreateTaskDefinition() {
    EcsContainerTask ecsContainerTask = new EcsContainerTask();
    ecsContainerTask.setAdvancedConfig(null);
    PortMapping portMapping = PortMapping.builder().containerPort(80).build();
    ContainerDefinition containerDefinition =
        ContainerDefinition.builder()
            .cpu(256d)
            .memory(1024)
            .portMappings(asList(portMapping))
            .logConfiguration(LogConfiguration.builder().logDriver("awslog").build())
            .build();

    ecsContainerTask.setContainerDefinitions(asList(containerDefinition));

    TaskDefinition taskDefinition =
        ecsContainerTask.createTaskDefinition(CONTAINER_NAME, IMAGE_NAME, EXEC_ROLE, DOMAIN_NAME);

    assertThat(taskDefinition).isNotNull();
    assertThat(taskDefinition.getContainerDefinitions()).hasSize(1);
    assertThat(taskDefinition.getCpu()).isEqualTo("256.0");
    assertThat(taskDefinition.getMemory()).isEqualTo("1024");
    assertThat(taskDefinition.getExecutionRoleArn()).isEqualTo(EXEC_ROLE);

    com.amazonaws.services.ecs.model.ContainerDefinition containerDefinitionAws =
        taskDefinition.getContainerDefinitions().get(0);
    assertThat(containerDefinitionAws).isNotNull();
    assertThat(containerDefinitionAws.getName()).isEqualTo(CONTAINER_NAME);
    assertThat(containerDefinitionAws.getImage()).isEqualTo(DOMAIN_NAME + "/" + IMAGE_NAME);
    assertThat(containerDefinitionAws.getCpu().intValue()).isEqualTo(256);
    assertThat(containerDefinitionAws.getMemory().intValue()).isEqualTo(1024);
    assertThat(containerDefinitionAws.getPortMappings()).isNotNull();
    assertThat(containerDefinitionAws.getPortMappings()).hasSize(1);
    assertThat(containerDefinitionAws.getPortMappings().iterator().next().getContainerPort().intValue()).isEqualTo(80);

    taskDefinition = ecsContainerTask.createTaskDefinition(CONTAINER_NAME, IMAGE_NAME, null, DOMAIN_NAME);

    assertThat(taskDefinition).isNotNull();
    assertThat(taskDefinition.getContainerDefinitions()).hasSize(1);
    assertThat(taskDefinition.getCpu()).isEqualTo("256.0");
    assertThat(taskDefinition.getMemory()).isEqualTo("1024");
    assertThat(taskDefinition.getExecutionRoleArn()).isEqualTo("");

    containerDefinitionAws = taskDefinition.getContainerDefinitions().get(0);
    assertThat(containerDefinitionAws).isNotNull();
    assertThat(containerDefinitionAws.getName()).isEqualTo(CONTAINER_NAME);
    assertThat(containerDefinitionAws.getImage()).isEqualTo(DOMAIN_NAME + "/" + IMAGE_NAME);
    assertThat(containerDefinitionAws.getCpu().intValue()).isEqualTo(256);
    assertThat(containerDefinitionAws.getMemory().intValue()).isEqualTo(1024);
    assertThat(containerDefinitionAws.getPortMappings()).isNotNull();
    assertThat(containerDefinitionAws.getPortMappings()).hasSize(1);
    assertThat(containerDefinitionAws.getPortMappings().iterator().next().getContainerPort().intValue()).isEqualTo(80);

    containerDefinition = ContainerDefinition.builder()
                              .portMappings(asList(portMapping))
                              .logConfiguration(LogConfiguration.builder().logDriver("awslog").build())
                              .build();

    ecsContainerTask.setContainerDefinitions(asList(containerDefinition));
    taskDefinition = ecsContainerTask.createTaskDefinition(CONTAINER_NAME, IMAGE_NAME, EXEC_ROLE, DOMAIN_NAME);
    assertThat(taskDefinition).isNotNull();
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testCreateTaskDefinitionWithWhenNoMemoryProvided() {
    EcsContainerTask ecsContainerTask = new EcsContainerTask();
    ecsContainerTask.setAdvancedConfig(null);
    PortMapping portMapping = PortMapping.builder().containerPort(80).build();
    ContainerDefinition containerDefinition =
        ContainerDefinition.builder()
            .cpu(256d)
            .portMappings(asList(portMapping))
            .logConfiguration(LogConfiguration.builder().logDriver("awslog").build())
            .build();

    ecsContainerTask.setContainerDefinitions(asList(containerDefinition));

    TaskDefinition taskDefinition =
        ecsContainerTask.createTaskDefinition(CONTAINER_NAME, IMAGE_NAME, EXEC_ROLE, DOMAIN_NAME);

    assertThat(taskDefinition).isNotNull();
    assertThat(taskDefinition.getContainerDefinitions()).hasSize(1);
    assertThat(taskDefinition.getCpu()).isEqualTo("256.0");
    assertThat(taskDefinition.getExecutionRoleArn()).isEqualTo(EXEC_ROLE);

    com.amazonaws.services.ecs.model.ContainerDefinition containerDefinitionAws =
        taskDefinition.getContainerDefinitions().get(0);
    assertThat(containerDefinitionAws).isNotNull();
    assertThat(containerDefinitionAws.getName()).isEqualTo(CONTAINER_NAME);
    assertThat(containerDefinitionAws.getImage()).isEqualTo(DOMAIN_NAME + "/" + IMAGE_NAME);
    assertThat(containerDefinitionAws.getCpu().intValue()).isEqualTo(256);
    assertThat(containerDefinitionAws.getMemory().intValue()).isEqualTo(1024);
    assertThat(containerDefinitionAws.getPortMappings()).isNotNull();
    assertThat(containerDefinitionAws.getPortMappings()).hasSize(1);
    assertThat(containerDefinitionAws.getPortMappings().iterator().next().getContainerPort().intValue()).isEqualTo(80);

    taskDefinition = ecsContainerTask.createTaskDefinition(CONTAINER_NAME, IMAGE_NAME, null, DOMAIN_NAME);

    assertThat(taskDefinition).isNotNull();
    assertThat(taskDefinition.getContainerDefinitions()).hasSize(1);
    assertThat(taskDefinition.getCpu()).isEqualTo("256.0");
    assertThat(taskDefinition.getExecutionRoleArn()).isEqualTo("");

    containerDefinitionAws = taskDefinition.getContainerDefinitions().get(0);
    assertThat(containerDefinitionAws).isNotNull();
    assertThat(containerDefinitionAws.getName()).isEqualTo(CONTAINER_NAME);
    assertThat(containerDefinitionAws.getImage()).isEqualTo(DOMAIN_NAME + "/" + IMAGE_NAME);
    assertThat(containerDefinitionAws.getCpu().intValue()).isEqualTo(256);
    assertThat(containerDefinitionAws.getMemory().intValue()).isEqualTo(1024);
    assertThat(containerDefinitionAws.getPortMappings()).isNotNull();
    assertThat(containerDefinitionAws.getPortMappings()).hasSize(1);
    assertThat(containerDefinitionAws.getPortMappings().iterator().next().getContainerPort().intValue()).isEqualTo(80);

    containerDefinition = ContainerDefinition.builder()
                              .portMappings(asList(portMapping))
                              .logConfiguration(LogConfiguration.builder().logDriver("awslog").build())
                              .build();

    ecsContainerTask.setContainerDefinitions(asList(containerDefinition));
    taskDefinition = ecsContainerTask.createTaskDefinition(CONTAINER_NAME, IMAGE_NAME, EXEC_ROLE, DOMAIN_NAME);
    assertThat(taskDefinition).isNotNull();
  }

  @Test
  @Owner(developers = ADWAIT)
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
    assertThat(!str.contains("\"secrets\" : [ ],")).isTrue();

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
    assertThat(!str.contains("\"secrets\" : [ ],")).isTrue();

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
    assertThat(!str.contains("\"secrets\" : [ ],")).isTrue();

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
    assertThat(!str.contains("\"secrets\" : [ ],")).isTrue();
  }
}
