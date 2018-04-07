package software.wings.integration.migration.legacy;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.stream.Collectors.toList;
import static software.wings.beans.Graph.Builder.aGraph;
import static software.wings.beans.GraphNode.GraphNodeBuilder.aGraphNode;
import static software.wings.beans.command.Command.Builder.aCommand;
import static software.wings.beans.command.CommandUnitType.ECS_SETUP;
import static software.wings.beans.command.CommandUnitType.KUBERNETES_SETUP;
import static software.wings.beans.command.ServiceCommand.Builder.aServiceCommand;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;
import static software.wings.dl.PageRequest.UNLIMITED;

import com.google.inject.Inject;

import io.harness.data.structure.UUIDGenerator;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.WingsBaseTest;
import software.wings.beans.Application;
import software.wings.beans.SearchFilter;
import software.wings.beans.Service;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandType;
import software.wings.beans.command.ServiceCommand;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.rules.Integration;
import software.wings.service.intfc.ServiceResourceService;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Migration script to make node select counts cumulative
 * @author brett on 10/3/17
 */
@Integration
@Ignore
public class ContainerSetupCommandsMigrationUtil extends WingsBaseTest {
  private static final Logger logger = LoggerFactory.getLogger(ContainerSetupCommandsMigrationUtil.class);

  @Inject private WingsPersistence wingsPersistence;
  @Inject private ServiceResourceService serviceResourceService;

  @Test
  public void addCommandsToServices() {
    PageRequest<Application> pageRequest = aPageRequest().withLimit(UNLIMITED).build();
    logger.info("Retrieving applications");
    PageResponse<Application> pageResponse = wingsPersistence.query(Application.class, pageRequest);

    List<Application> apps = pageResponse.getResponse();
    if (pageResponse.isEmpty() || isEmpty(apps)) {
      logger.info("No applications found");
      return;
    }
    logger.info("Updating " + apps.size() + " applications.");
    for (Application app : apps) {
      PageRequest<Service> svcPageRequest =
          aPageRequest().addFilter("appId", SearchFilter.Operator.EQ, app.getUuid()).build();

      List<Service> services = serviceResourceService.list(svcPageRequest, false, true).getResponse();
      Set<Service> updatedServices = new HashSet<>();
      for (Service service : services) {
        logger.info("\nservice = " + service.getName());
        List<ServiceCommand> commands = service.getServiceCommands();
        boolean containsEcsSetup =
            commands.stream().anyMatch(serviceCommand -> "Setup Service Cluster".equals(serviceCommand.getName()));
        boolean containsKubeSetup = commands.stream().anyMatch(
            serviceCommand -> "Setup Replication Controller".equals(serviceCommand.getName()));
        for (ServiceCommand serviceCommand : commands) {
          logger.info("command = " + serviceCommand.getName());
          if (!containsEcsSetup && "Resize Service Cluster".equals(serviceCommand.getName())) {
            Command command = aCommand()
                                  .withCommandType(CommandType.SETUP)
                                  .withGraph(aGraph()
                                                 .withGraphName("Setup Service Cluster")
                                                 .addNodes(aGraphNode()
                                                               .withOrigin(true)
                                                               .withId(UUIDGenerator.graphIdGenerator("node"))
                                                               .withName("Setup ECS Service")
                                                               .withType(ECS_SETUP.name())
                                                               .build())
                                                 .buildPipeline())
                                  .build();

            updatedServices.add(serviceResourceService.addCommand(service.getAppId(), service.getUuid(),
                aServiceCommand()
                    .withTargetToAllEnv(true)
                    .withCommand(command)
                    .withName("Setup Service Cluster")
                    .build(),
                true));
          }
          if (!containsKubeSetup && "Resize Replication Controller".equals(serviceCommand.getName())) {
            Command command = aCommand()
                                  .withCommandType(CommandType.SETUP)
                                  .withGraph(aGraph()
                                                 .withGraphName("Setup Replication Controller")
                                                 .addNodes(aGraphNode()
                                                               .withOrigin(true)
                                                               .withId(UUIDGenerator.graphIdGenerator("node"))
                                                               .withName("Setup Kubernetes Replication Controller")
                                                               .withType(KUBERNETES_SETUP.name())
                                                               .build())
                                                 .buildPipeline())
                                  .build();

            updatedServices.add(serviceResourceService.addCommand(service.getAppId(), service.getUuid(),
                aServiceCommand()
                    .withTargetToAllEnv(true)
                    .withCommand(command)
                    .withName("Setup Replication Controller")
                    .build(),
                true));
          }
        }
      }
      if (isNotEmpty(updatedServices)) {
        logger.info("Updated services in app " + app.getName() + ": "
            + updatedServices.stream().map(Service::getName).collect(toList()));
      }
    }
  }
}
