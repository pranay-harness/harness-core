package software.wings.beans.container;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.amazonaws.services.ecs.model.TaskDefinition;
import org.junit.Test;

import java.util.Arrays;

public class EcsContainerTaskTest {
  public static final String CONTAINER_NAME = "containerName";
  public static final String IMAGE_NAME = "imageName";
  public static final String EXEC_ROLE = "exec_role";

  @Test
  public void testcreateTaskDefinition() {
    EcsContainerTask ecsContainerTask = new EcsContainerTask();
    ecsContainerTask.setAdvancedConfig(null);
    PortMapping portMapping = PortMapping.builder().containerPort(80).build();
    ContainerDefinition containerDefinition =
        ContainerDefinition.builder()
            .cpu(256)
            .memory(1024)
            .portMappings(Arrays.asList(portMapping))
            .logConfiguration(LogConfiguration.builder().logDriver("awslog").build())
            .build();

    ecsContainerTask.setContainerDefinitions(Arrays.asList(containerDefinition));

    TaskDefinition taskDefinition = ecsContainerTask.createTaskDefinition(CONTAINER_NAME, IMAGE_NAME, EXEC_ROLE);

    assertNotNull(taskDefinition);
    assertEquals(1, taskDefinition.getContainerDefinitions().size());
    assertEquals("256", taskDefinition.getCpu());
    assertEquals("1024", taskDefinition.getMemory());
    assertEquals(EXEC_ROLE, taskDefinition.getExecutionRoleArn());

    com.amazonaws.services.ecs.model.ContainerDefinition containerDefinitionAws =
        taskDefinition.getContainerDefinitions().get(0);
    assertNotNull(containerDefinitionAws);
    assertEquals(CONTAINER_NAME, containerDefinitionAws.getName());
    assertEquals(IMAGE_NAME, containerDefinitionAws.getImage());
    assertEquals(256, containerDefinitionAws.getCpu().intValue());
    assertEquals(1024, containerDefinitionAws.getMemory().intValue());
    assertNotNull(containerDefinitionAws.getPortMappings());
    assertEquals(1, containerDefinitionAws.getPortMappings().size());
    assertEquals(80, containerDefinitionAws.getPortMappings().iterator().next().getContainerPort().intValue());

    taskDefinition = ecsContainerTask.createTaskDefinition(CONTAINER_NAME, IMAGE_NAME, null);

    assertNotNull(taskDefinition);
    assertEquals(1, taskDefinition.getContainerDefinitions().size());
    assertEquals("256", taskDefinition.getCpu());
    assertEquals("1024", taskDefinition.getMemory());
    assertEquals("null", taskDefinition.getExecutionRoleArn());

    containerDefinitionAws = taskDefinition.getContainerDefinitions().get(0);
    assertNotNull(containerDefinitionAws);
    assertEquals(CONTAINER_NAME, containerDefinitionAws.getName());
    assertEquals(IMAGE_NAME, containerDefinitionAws.getImage());
    assertEquals(256, containerDefinitionAws.getCpu().intValue());
    assertEquals(1024, containerDefinitionAws.getMemory().intValue());
    assertNotNull(containerDefinitionAws.getPortMappings());
    assertEquals(1, containerDefinitionAws.getPortMappings().size());
    assertEquals(80, containerDefinitionAws.getPortMappings().iterator().next().getContainerPort().intValue());

    containerDefinition = ContainerDefinition.builder()
                              .portMappings(Arrays.asList(portMapping))
                              .logConfiguration(LogConfiguration.builder().logDriver("awslog").build())
                              .build();

    ecsContainerTask.setContainerDefinitions(Arrays.asList(containerDefinition));
    taskDefinition = ecsContainerTask.createTaskDefinition(CONTAINER_NAME, IMAGE_NAME, EXEC_ROLE);
    assertNotNull(taskDefinition);
  }
}
