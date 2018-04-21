package software.wings.service.impl.yaml.handler.workflow;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static java.util.stream.Collectors.toList;
import static software.wings.beans.PipelineStage.Yaml;
import static software.wings.exception.WingsException.USER;
import static software.wings.utils.Validator.notNullCheck;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.beans.ErrorCode;
import software.wings.beans.NameValuePair;
import software.wings.beans.Pipeline;
import software.wings.beans.PipelineStage;
import software.wings.beans.PipelineStage.PipelineStageElement;
import software.wings.beans.PipelineStage.PipelineStageElement.PipelineStageElementBuilder;
import software.wings.beans.Workflow;
import software.wings.beans.yaml.Change;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.YamlType;
import software.wings.exception.HarnessException;
import software.wings.exception.WingsException;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.handler.NameValuePairYamlHandler;
import software.wings.service.impl.yaml.handler.YamlHandlerFactory;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.StateType;

import java.util.List;
import java.util.Map;

/**
 * @author rktummala on 11/2/17
 */
@Singleton
public class PipelineStageYamlHandler extends BaseYamlHandler<Yaml, PipelineStage> {
  @Inject YamlHelper yamlHelper;
  @Inject WorkflowService workflowService;
  @Inject YamlHandlerFactory yamlHandlerFactory;

  private PipelineStage toBean(ChangeContext<Yaml> context) throws HarnessException {
    Yaml yaml = context.getYaml();
    Change change = context.getChange();

    String appId = yamlHelper.getAppId(change.getAccountId(), change.getFilePath());
    notNullCheck("Could not retrieve valid app from path: " + change.getFilePath(), appId, USER);

    PipelineStage stage = PipelineStage.builder().build();
    stage.setName(yaml.getName());
    stage.setParallel(yaml.isParallel());

    Map<String, Object> properties = Maps.newHashMap();
    Map<String, String> workflowVariables = Maps.newHashMap();

    if (!yaml.getType().equals(StateType.APPROVAL.name())) {
      Workflow workflow = workflowService.readWorkflowByName(appId, yaml.getWorkflowName());
      notNullCheck("Invalid workflow with the given name:" + yaml.getWorkflowName(), workflow, USER);

      properties.put("envId", workflow.getEnvId());
      properties.put("workflowId", workflow.getUuid());
    }

    String stageElementId = null;
    Pipeline previous = yamlHelper.getPipeline(change.getAccountId(), change.getFilePath());
    if (previous != null) {
      Map<String, String> entityIdMap = context.getEntityIdMap();
      if (entityIdMap != null) {
        stageElementId = entityIdMap.remove(yaml.getName());
      }
    }

    if (yaml.getWorkflowVariables() != null) {
      yaml.getWorkflowVariables().stream().forEach(
          variable -> workflowVariables.put(variable.getName(), variable.getValue()));
    }

    PipelineStageElementBuilder builder = PipelineStageElement.builder();
    if (stageElementId != null) {
      builder.uuid(stageElementId);
    }

    PipelineStageElement pipelineStageElement = builder.name(yaml.getName())
                                                    .type(yaml.getType())
                                                    .properties(properties)
                                                    .workflowVariables(workflowVariables)
                                                    .build();

    stage.setPipelineStageElements(Lists.newArrayList(pipelineStageElement));

    return stage;
  }

  @Override
  public Yaml toYaml(PipelineStage bean, String appId) {
    if (isEmpty(bean.getPipelineStageElements())) {
      throw new WingsException(ErrorCode.INVALID_ARGUMENT)
          .addParam("args", "No stage elements present in the given phase stage: " + bean.getName());
    }

    PipelineStageElement stageElement = bean.getPipelineStageElements().get(0);
    notNullCheck("Pipeline stage element is null", stageElement, USER);

    String workflowName = null;
    List<NameValuePair.Yaml> nameValuePairYamlList = null;
    if (!StateType.APPROVAL.name().equals(stageElement.getType())) {
      Map<String, Object> properties = stageElement.getProperties();
      notNullCheck("Pipeline stage element is null", properties, USER);

      String workflowId = (String) properties.get("workflowId");
      notNullCheck("Workflow id is null in stage properties", workflowId, USER);

      Workflow workflow = workflowService.readWorkflow(appId, workflowId);
      notNullCheck("Workflow id is null in stage properties", workflowId, USER);

      workflowName = workflow.getName();

      Map<String, String> workflowVariables = stageElement.getWorkflowVariables();
      notNullCheck("Pipeline stage element is null", workflowVariables, USER);

      // properties
      NameValuePairYamlHandler nameValuePairYamlHandler = yamlHandlerFactory.getYamlHandler(YamlType.NAME_VALUE_PAIR);

      nameValuePairYamlList = workflowVariables.entrySet()
                                  .stream()
                                  .map(entry -> {
                                    NameValuePair nameValuePair =
                                        NameValuePair.builder().name(entry.getKey()).value(entry.getValue()).build();
                                    return nameValuePairYamlHandler.toYaml(nameValuePair, appId);
                                  })
                                  .collect(toList());
    }

    return Yaml.builder()
        .name(stageElement.getName())
        .parallel(bean.isParallel())
        .type(stageElement.getType())
        .workflowName(workflowName)
        .workflowVariables(nameValuePairYamlList)
        .build();
  }

  @Override
  public PipelineStage upsertFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    return toBean(changeContext);
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }

  @Override
  public PipelineStage get(String accountId, String yamlFilePath) {
    throw new WingsException(ErrorCode.UNSUPPORTED_OPERATION_EXCEPTION);
  }

  @Override
  public void delete(ChangeContext<Yaml> changeContext) throws HarnessException {
    // Do nothing
  }
}
