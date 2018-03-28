package software.wings.service.impl.yaml.handler.workflow;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static software.wings.beans.PipelineStage.Yaml;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.beans.ErrorCode;
import software.wings.beans.NameValuePair;
import software.wings.beans.PipelineStage;
import software.wings.beans.PipelineStage.PipelineStageElement;
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
import software.wings.utils.Validator;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    Validator.notNullCheck("Could not retrieve valid app from path: " + change.getFilePath(), appId);

    PipelineStage stage = new PipelineStage();
    stage.setName(yaml.getName());
    stage.setParallel(yaml.isParallel());

    Map<String, Object> properties = Maps.newHashMap();
    Map<String, String> workflowVariables = Maps.newHashMap();

    if (!yaml.getType().equals(StateType.APPROVAL.name())) {
      Workflow workflow = workflowService.readWorkflowByName(appId, yaml.getWorkflowName());
      Validator.notNullCheck("Invalid workflow with the given name:" + yaml.getWorkflowName(), workflow);

      properties.put("envId", workflow.getEnvId());
      properties.put("workflowId", workflow.getUuid());
    }

    if (yaml.getWorkflowVariables() != null) {
      yaml.getWorkflowVariables().stream().forEach(
          variable -> workflowVariables.put(variable.getName(), variable.getValue()));
    }
    PipelineStageElement pipelineStageElement = PipelineStageElement.builder()
                                                    .uuid(yaml.getUuid())
                                                    .name(yaml.getName())
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
    Validator.notNullCheck("Pipeline stage element is null", stageElement);

    String workflowName = null;
    List<NameValuePair.Yaml> nameValuePairYamlList = null;
    if (!StateType.APPROVAL.name().equals(stageElement.getType())) {
      Map<String, Object> properties = stageElement.getProperties();
      Validator.notNullCheck("Pipeline stage element is null", properties);

      String workflowId = (String) properties.get("workflowId");
      Validator.notNullCheck("Workflow id is null in stage properties", workflowId);

      Workflow workflow = workflowService.readWorkflow(appId, workflowId);
      Validator.notNullCheck("Workflow id is null in stage properties", workflowId);

      workflowName = workflow.getName();

      Map<String, String> workflowVariables = stageElement.getWorkflowVariables();
      Validator.notNullCheck("Pipeline stage element is null", workflowVariables);

      // properties
      NameValuePairYamlHandler nameValuePairYamlHandler = yamlHandlerFactory.getYamlHandler(YamlType.NAME_VALUE_PAIR);

      nameValuePairYamlList = workflowVariables.entrySet()
                                  .stream()
                                  .map(entry -> {
                                    NameValuePair nameValuePair =
                                        NameValuePair.builder().name(entry.getKey()).value(entry.getValue()).build();
                                    return nameValuePairYamlHandler.toYaml(nameValuePair, appId);
                                  })
                                  .collect(Collectors.toList());
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
