package software.wings.sm.states;

import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.SearchFilter.Operator.EXISTS;
import static software.wings.beans.SearchFilter.Operator.NOT_EQ;
import static software.wings.dl.PageRequest.Builder.aPageRequest;

import com.google.inject.Inject;

import com.github.reinert.jjschema.SchemaIgnore;
import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.Activity;
import software.wings.beans.DeploymentExecutionContext;
import software.wings.beans.artifact.Artifact;
import software.wings.dl.PageResponse;
import software.wings.service.intfc.ArtifactService;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateType;

import java.util.List;

public class AwsLambdaRollback extends AwsLambdaState {
  @Inject @Transient protected transient ArtifactService artifactService;

  public AwsLambdaRollback(String name) {
    super(name, StateType.AWS_LAMBDA_ROLLBACK.name());
  }

  @Override
  protected Artifact getArtifact(String appId, String serviceId, String workflowExecutionId,
      DeploymentExecutionContext deploymentExecutionContext) {
    PageResponse<Activity> pageResponse =
        activityService.list(aPageRequest()
                                 .withLimit("1")
                                 .addFilter("appId", EQ, appId)
                                 .addFilter("serviceId", EQ, serviceId)
                                 .addFilter("status", EQ, ExecutionStatus.SUCCESS)
                                 .addFilter("workflowExecutionId", NOT_EQ, workflowExecutionId)
                                 .addFilter("artifactId", EXISTS)
                                 .build());
    if (pageResponse != null && !pageResponse.isEmpty()) {
      return artifactService.get(appId, pageResponse.getResponse().get(0).getArtifactId());
    }
    return null;
  }

  @SchemaIgnore
  @Override
  public List<String> getAliases() {
    return super.getAliases();
  }

  @SchemaIgnore
  @Override
  public String getCommandName() {
    return super.getCommandName();
  }
}
