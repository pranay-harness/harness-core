package software.wings.integration.migration.legacy;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;
import static software.wings.dl.PageRequest.UNLIMITED;

import com.google.inject.Inject;

import org.junit.Ignore;
import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.beans.Application;
import software.wings.beans.AwsInfrastructureMapping;
import software.wings.beans.AwsInstanceFilter.Tag;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.ConfigFile;
import software.wings.beans.GraphNode;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.PhaseStep;
import software.wings.beans.SearchFilter;
import software.wings.beans.ServiceVariable;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowPhase;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.CommandUnitType;
import software.wings.beans.command.ExecCommandUnit;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.rules.Integration;
import software.wings.service.intfc.WorkflowService;

import java.util.List;
import java.util.Map;

/**
 * Migration script to fix corrupted expressions caused by a UI bug
 * @author brett on 10/13/17
 */
@Integration
@Ignore
public class FixCorruptedExpressionsMigrationUtil extends WingsBaseTest {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private WorkflowService workflowService;

  @Test
  public void fixCorruptedExpressions() {
    System.out.println("Checking for corrupted expressions.");
    checkWorkflows();
    checkServiceVariables();
    checkCommands();
    checkAwsInfraMappings();
    checkConfigFiles();
    System.out.println("Finished checking for corrupted expressions.");
  }

  private void checkConfigFiles() {
    List<ConfigFile> configFiles = wingsPersistence.list(ConfigFile.class);
    for (ConfigFile configFile : configFiles) {
      String value = configFile.getRelativeFilePath();
      if (isNotEmpty(value) && (value.contains("style1|") || value.contains("style2|"))) {
        System.out.println(configFile.getName() + ":" + value);
        configFile.setRelativeFilePath(value.replaceAll("style1\\|", "").replaceAll("style2\\|", ""));
        wingsPersistence.updateField(ConfigFile.class, configFile.getUuid(), "relativeFilePath", value);
      }
    }
  }

  private void checkAwsInfraMappings() {
    List<InfrastructureMapping> awsInfraMappings = wingsPersistence.createQuery(InfrastructureMapping.class)
                                                       .field("infraMappingType")
                                                       .equal(InfrastructureMappingType.AWS_SSH.name())
                                                       .asList();
    for (InfrastructureMapping infraMapping : awsInfraMappings) {
      AwsInfrastructureMapping awsInfrastructureMapping = (AwsInfrastructureMapping) infraMapping;
      if (!awsInfrastructureMapping.isProvisionInstances() && awsInfrastructureMapping.getAwsInstanceFilter() != null) {
        boolean modified = false;
        List<Tag> tags = awsInfrastructureMapping.getAwsInstanceFilter().getTags();
        for (Tag tag : tags) {
          String value = tag.getValue();
          if (isNotEmpty(value) && (value.contains("style1|") || value.contains("style2|"))) {
            System.out.println(tag.getKey() + ":" + value);
            tag.setValue(value.replaceAll("style1\\|", "").replaceAll("style2\\|", ""));
            modified = true;
          }
        }
        if (modified) {
          wingsPersistence.save(awsInfrastructureMapping);
        }
      }
    }
  }

  private void checkCommands() {
    List<Command> commands = wingsPersistence.list(Command.class);
    for (Command command : commands) {
      boolean modified = false;
      for (CommandUnit commandUnit : command.getCommandUnits()) {
        if (commandUnit.getCommandUnitType() != CommandUnitType.EXEC || !(commandUnit instanceof ExecCommandUnit)
            || ((ExecCommandUnit) commandUnit).getCommandString() == null) {
          continue;
        }
        String value = ((ExecCommandUnit) commandUnit).getCommandString();
        if (isNotEmpty(value) && (value.contains("style1|") || value.contains("style2|"))) {
          System.out.println(command.getName() + ":" + value);
          ((ExecCommandUnit) commandUnit)
              .setCommandString(value.replaceAll("style1\\|", "").replaceAll("style2\\|", ""));
          modified = true;
        }
        if (modified) {
          wingsPersistence.save(command);
        }
      }
    }
  }

  private void checkServiceVariables() {
    List<ServiceVariable> serviceVariables = wingsPersistence.list(ServiceVariable.class);
    for (ServiceVariable serviceVariable : serviceVariables) {
      String value = new String(serviceVariable.getValue());
      if (isNotEmpty(value) && (value.contains("style1|") || value.contains("style2|"))) {
        System.out.println(serviceVariable.getName() + ":" + value);
        serviceVariable.setValue(value.replaceAll("style1\\|", "").replaceAll("style2\\|", "").toCharArray());
        wingsPersistence.updateField(ServiceVariable.class, serviceVariable.getUuid(), "value", value);
      }
    }
  }

  private void checkWorkflows() {
    PageRequest<Application> pageRequest = aPageRequest().withLimit(UNLIMITED).build();
    System.out.println("Retrieving applications");
    PageResponse<Application> pageResponse = wingsPersistence.query(Application.class, pageRequest);

    List<Application> apps = pageResponse.getResponse();
    if (pageResponse.isEmpty() || isEmpty(apps)) {
      System.out.println("No applications found");
      return;
    }
    System.out.println("Updating " + apps.size() + " applications.");
    StringBuilder result = new StringBuilder();
    for (Application app : apps) {
      List<Workflow> workflows = workflowService
                                     .listWorkflows(aPageRequest()
                                                        .withLimit(UNLIMITED)
                                                        .addFilter("appId", SearchFilter.Operator.EQ, app.getUuid())
                                                        .build())
                                     .getResponse();
      int updateCount = 0;
      for (Workflow workflow : workflows) {
        boolean workflowModified = false;
        if (workflow.getOrchestrationWorkflow() instanceof CanaryOrchestrationWorkflow) {
          CanaryOrchestrationWorkflow coWorkflow = (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();
          for (WorkflowPhase workflowPhase : coWorkflow.getWorkflowPhases()) {
            for (PhaseStep phaseStep : workflowPhase.getPhaseSteps()) {
              for (GraphNode node : phaseStep.getSteps()) {
                boolean phaseStepFound = false;
                Map<String, Object> properties = node.getProperties();
                for (String key : properties.keySet()) {
                  Object obj = properties.get(key);
                  if (obj instanceof String) {
                    String value = (String) obj;
                    if (isNotEmpty(value) && (value.contains("style1|") || value.contains("style2|"))) {
                      if (!phaseStepFound) {
                        System.out.println("\n" + app.getName() + ":" + workflow.getName() + ":"
                            + workflowPhase.getName() + ":" + phaseStep.getName() + ":" + node.getName());
                      }
                      System.out.println(key + ":" + value);
                      properties.put(key, value.replaceAll("style1\\|", "").replaceAll("style2\\|", ""));
                      phaseStepFound = true;
                      workflowModified = true;
                    }
                  }
                }
              }
            }
          }
        }
        if (workflowModified) {
          try {
            workflowService.updateWorkflow(workflow);
            Thread.sleep(100);
          } catch (Exception e) {
            e.printStackTrace();
          }

          updateCount++;
        }
      }
      if (updateCount > 0) {
        result.append("Application migrated: ")
            .append(app.getName())
            .append(". Updated ")
            .append(updateCount)
            .append(" workflows.\n");
      }
    }
    System.out.println(result.toString());
  }
}
