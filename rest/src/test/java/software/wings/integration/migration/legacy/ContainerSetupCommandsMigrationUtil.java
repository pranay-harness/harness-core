package software.wings.integration.migration.legacy;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static software.wings.beans.Graph.Builder.aGraph;
import static software.wings.beans.GraphNode.GraphNodeBuilder.aGraphNode;
import static software.wings.beans.SearchFilter.Builder.aSearchFilter;
import static software.wings.beans.command.Command.Builder.aCommand;
import static software.wings.beans.command.CommandUnitType.ECS_SETUP;
import static software.wings.beans.command.CommandUnitType.KUBERNETES_SETUP;
import static software.wings.beans.command.ServiceCommand.Builder.aServiceCommand;
import static software.wings.dl.PageRequest.Builder.aPageRequest;
import static software.wings.dl.PageRequest.UNLIMITED;

import com.google.inject.Inject;

import org.junit.Ignore;
import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.beans.Application;
import software.wings.beans.SearchFilter;
import software.wings.beans.Service;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandType;
import software.wings.beans.command.ServiceCommand;
import software.wings.common.UUIDGenerator;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.rules.Integration;
import software.wings.service.intfc.ServiceResourceService;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Migration script to make node select counts cumulative
 * @author brett on 10/3/17
 */
@Integration
@Ignore
public class ContainerSetupCommandsMigrationUtil extends WingsBaseTest {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private ServiceResourceService serviceResourceService;

  @Test
  public void addCommandsToServices() {
    PageRequest<Application> pageRequest = aPageRequest().withLimit(UNLIMITED).build();
    System.out.println("Retrieving applications");
    PageResponse<Application> pageResponse = wingsPersistence.query(Application.class, pageRequest);

    List<Application> apps = pageResponse.getResponse();
    if (pageResponse.isEmpty() || isEmpty(apps)) {
      System.out.println("No applications found");
      return;
    }
    System.out.println("Updating " + apps.size() + " applications.");
    for (Application app : apps) {
      PageRequest<Service> svcPageRequest =
          aPageRequest()
              .addFilter(aSearchFilter().withField("appId", SearchFilter.Operator.EQ, app.getUuid()).build())
              .build();

      List<Service> services = serviceResourceService.list(svcPageRequest, false, true).getResponse();
      Set<Service> updatedServices = new HashSet<>();
      for (Service service : services) {
        System.out.println("\nservice = " + service.getName());
        List<ServiceCommand> commands = service.getServiceCommands();
        boolean containsEcsSetup =
            commands.stream().anyMatch(serviceCommand -> "Setup Service Cluster".equals(serviceCommand.getName()));
        boolean containsKubeSetup = commands.stream().anyMatch(
            serviceCommand -> "Setup Replication Controller".equals(serviceCommand.getName()));
        for (ServiceCommand serviceCommand : commands) {
          System.out.println("command = " + serviceCommand.getName());
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
        System.out.println("Updated services in app " + app.getName() + ": "
            + updatedServices.stream().map(Service::getName).collect(Collectors.toList()));
      }
    }
  }
}
