package software.wings.beans.container;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.amazonaws.services.ecs.model.TaskDefinition;
import org.junit.Test;

public class EcsContainerTaskTest {
  public static final String CONTAINER_NAME = "containerName";
  public static final String IMAGE_NAME = "imageName";
  public static final String EXEC_ROLE = "exec_role";
  public static final String DOMAIN_NAME = "domain.name.co";

  @Test
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
}
