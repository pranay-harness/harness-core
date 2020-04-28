package software.wings.graphql.datafetcher.pipeline;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.validation.Validator.notNullCheck;

import com.google.inject.Inject;

import io.harness.logging.AutoLogContext;
import io.harness.persistence.AccountLogContext;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Pipeline;
import software.wings.beans.Variable;
import software.wings.graphql.datafetcher.AbstractArrayDataFetcher;
import software.wings.graphql.datafetcher.VariableController;
import software.wings.graphql.schema.query.QLPipelineVariableQueryParam;
import software.wings.graphql.schema.type.QLVariable;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.PipelineService;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class PipelineVariableConnectionDataFetcher
    extends AbstractArrayDataFetcher<QLVariable, QLPipelineVariableQueryParam> {
  @Inject PipelineService pipelineService;

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.WORKFLOW, action = PermissionAttribute.Action.READ)
  protected List<QLVariable> fetch(QLPipelineVariableQueryParam parameters, String accountId) {
    try (AutoLogContext ignore0 = new AccountLogContext(accountId, AutoLogContext.OverrideBehavior.OVERRIDE_ERROR)) {
      String pipelineId = parameters.getPipelineId();
      String appId = parameters.getApplicationId();
      Pipeline pipeline = pipelineService.readPipelineWithVariables(appId, pipelineId);
      notNullCheck("Pipeline " + pipelineId + " doesn't exist in the specified application " + appId, pipeline, USER);
      if (isEmpty(pipeline.getPipelineVariables())) {
        logger.info("No non-fixed variables present in pipeline: " + pipelineId);
        return new ArrayList<>();
      }
      List<Variable> userVariables = pipeline.getPipelineVariables();
      List<QLVariable> qlVariables = new ArrayList<>();
      VariableController.populateVariables(userVariables, qlVariables);
      return qlVariables;
    }
  }

  @Override
  protected QLVariable unusedReturnTypePassingDummyMethod() {
    return null;
  }
}
