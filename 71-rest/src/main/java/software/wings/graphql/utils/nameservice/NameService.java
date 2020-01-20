package software.wings.graphql.utils.nameservice;

import java.util.Set;
import javax.validation.constraints.NotNull;

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
  String connector = "Connector";
  String deployment = "Deployment";
  String instance = "Instance";
  String trigger = "Trigger";
  String pipelineExecution = "PipelineExecution";
  String user = "User";

  NameResult getNames(@NotNull Set<String> ids, @NotNull String type);
}
