package software.wings.search.framework;

import lombok.Builder;
import lombok.Value;
import software.wings.search.entities.application.ApplicationView;
import software.wings.search.entities.deployment.DeploymentView;
import software.wings.search.entities.environment.EnvironmentView;
import software.wings.search.entities.pipeline.PipelineView;
import software.wings.search.entities.service.ServiceView;
import software.wings.search.entities.workflow.WorkflowView;

import java.util.List;

@Value
@Builder
public class SearchResponse {
  List<ApplicationView> applications;
  List<PipelineView> pipelines;
  List<WorkflowView> workflows;
  List<ServiceView> services;
  List<EnvironmentView> environments;
  List<DeploymentView> deployments;
}
