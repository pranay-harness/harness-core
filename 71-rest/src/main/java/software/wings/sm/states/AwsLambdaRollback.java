package software.wings.sm.states;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.beans.SearchFilter.Operator.EXISTS;
import static io.harness.beans.SearchFilter.Operator.NOT_EQ;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;
import static software.wings.api.AwsLambdaContextElement.AWS_LAMBDA_REQUEST_PARAM;

import com.google.inject.Inject;

import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.PageResponse;
import io.harness.context.ContextElementType;
import org.mongodb.morphia.annotations.Transient;
import software.wings.api.AwsLambdaContextElement;
import software.wings.beans.Activity;
import software.wings.beans.DeploymentExecutionContext;
import software.wings.beans.Tag;
import software.wings.beans.artifact.Artifact;
import software.wings.service.intfc.ArtifactService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.StateType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    if (isNotEmpty(pageResponse)) {
      return artifactService.get(pageResponse.getResponse().get(0).getArtifactId());
    }
    return null;
  }

  @Override
  protected List<String> getEvaluatedAliases(ExecutionContext context) {
    AwsLambdaContextElement awsLambdaContextElement =
        context.getContextElement(ContextElementType.PARAM, AWS_LAMBDA_REQUEST_PARAM);
    if (awsLambdaContextElement != null) {
      List<String> aliases = awsLambdaContextElement.getAliases();
      if (isNotEmpty(aliases)) {
        return aliases.stream().map(context::renderExpression).collect(toList());
      }
    }
    return emptyList();
  }

  @Override
  protected Map<String, String> getFunctionTags(ExecutionContext context) {
    AwsLambdaContextElement awsLambdaContextElement =
        context.getContextElement(ContextElementType.PARAM, AWS_LAMBDA_REQUEST_PARAM);
    if (awsLambdaContextElement != null) {
      List<Tag> tags = awsLambdaContextElement.getTags();
      if (isNotEmpty(tags)) {
        Map<String, String> functionTags = new HashMap<>();
        tags.forEach(tag -> { functionTags.put(tag.getKey(), context.renderExpression(tag.getValue())); });
        return functionTags;
      }
    }
    return emptyMap();
  }

  @Override
  @SchemaIgnore
  public List<String> getAliases() {
    return super.getAliases();
  }

  @Override
  @SchemaIgnore
  public String getCommandName() {
    return super.getCommandName();
  }

  @Override
  @SchemaIgnore
  public List<Tag> getTags() {
    return super.getTags();
  }
}