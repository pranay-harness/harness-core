package software.wings.graphql.utils.nameservice;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import java.util.Set;
import javax.validation.constraints.NotNull;

@TargetModule(HarnessModule._380_CG_GRAPHQL)
public interface NameService {
  String application = "Application";
  String service = "Service";
  String environment = "Environment";
  String environmentType = "EnvironmentType";
  String Type = "Type";
  String cloudProvider = "CloudProvider";
  String status = "Status";
  String triggeredBy = "TriggeredBy";
  String triggerId = "TriggerId";
  String workflow = "Workflow";
  String pipeline = "Pipeline";
  String instanceType = "InstanceType";
  String artifactType = "ArtifactType";
  String artifactSource = "ArtifactSource";
  String connector = "Connector";
  String deployment = "Deployment";
  String instance = "Instance";
  String trigger = "Trigger";
  String pipelineExecution = "PipelineExecution";
  String user = "User";

  String infrastructureDefinition = "InfrastructureDefinition";

  NameResult getNames(@NotNull Set<String> ids, @NotNull String type);
}
