package software.wings.service.impl.yaml.handler.workflow;

import static software.wings.beans.Pipeline.Yaml;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import software.wings.beans.ObjectType;
import software.wings.beans.Pipeline;
import software.wings.beans.Pipeline.Builder;
import software.wings.beans.PipelineStage;
import software.wings.beans.yaml.Change;
import software.wings.beans.yaml.Change.ChangeType;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.YamlType;
import software.wings.exception.HarnessException;
import software.wings.exception.WingsException;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.handler.YamlHandlerFactory;
import software.wings.service.impl.yaml.sync.YamlSyncHelper;
import software.wings.service.intfc.PipelineService;
import software.wings.utils.Validator;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author rktummala on 11/2/17
 */
public class PipelineYamlHandler extends BaseYamlHandler<Yaml, Pipeline> {
  @Inject private YamlSyncHelper yamlSyncHelper;
  @Inject private PipelineService pipelineService;
  @Inject private YamlHandlerFactory yamlHandlerFactory;

  @Override
  public Pipeline createFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    Pipeline pipeline = setWithYamlValues(changeContext, changeSetContext, Builder.aPipeline(), true);
    return pipelineService.createPipeline(pipeline);
  }

  private Pipeline setWithYamlValues(ChangeContext<Yaml> context, List<ChangeContext> changeSetContext,
      Pipeline.Builder pipeline, boolean isCreate) throws HarnessException {
    try {
      Yaml yaml = context.getYaml();
      Change change = context.getChange();

      String appId = yamlSyncHelper.getAppId(change.getAccountId(), change.getFilePath());
      Validator.notNullCheck("Could not retrieve valid app from path: " + change.getFilePath(), appId);

      List<PipelineStage> pipelineStages = Lists.newArrayList();
      if (yaml.getPipelineStages() != null) {
        BaseYamlHandler pipelineStageYamlHandler =
            yamlHandlerFactory.getYamlHandler(YamlType.PIPELINE_STAGE, ObjectType.PIPELINE_STAGE);

        // Pipeline stages
        pipelineStages = yaml.getPipelineStages()
                             .stream()
                             .map(stageYaml -> {
                               try {
                                 ChangeContext.Builder clonedContext = cloneFileChangeContext(context, stageYaml);
                                 return (PipelineStage) createOrUpdateFromYaml(
                                     isCreate, pipelineStageYamlHandler, clonedContext.build(), changeSetContext);
                               } catch (HarnessException e) {
                                 throw new WingsException(e);
                               }
                             })
                             .collect(Collectors.toList());
      }

      pipeline.withAppId(appId)
          .withDescription(yaml.getDescription())
          .withName(yaml.getName())
          .withPipelineStages(pipelineStages)
          .build();
      return pipeline.build();

    } catch (WingsException ex) {
      throw new HarnessException(ex);
    }
  }

  @Override
  public Yaml toYaml(Pipeline bean, String appId) {
    BaseYamlHandler pipelineStageYamlHandler =
        yamlHandlerFactory.getYamlHandler(YamlType.PIPELINE_STAGE, ObjectType.PIPELINE_STAGE);
    List<PipelineStage> pipelineStages = bean.getPipelineStages();
    List<PipelineStage.Yaml> pipelineStageYamlList =
        pipelineStages.stream()
            .map(pipelineStage -> (PipelineStage.Yaml) pipelineStageYamlHandler.toYaml(pipelineStage, bean.getAppId()))
            .collect(Collectors.toList());

    return Yaml.Builder.anYaml()
        .withName(bean.getName())
        .withDescription(bean.getDescription())
        .withPipelineStages(pipelineStageYamlList)
        .build();
  }

  @Override
  public Pipeline upsertFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    if (changeContext.getChange().getChangeType().equals(ChangeType.ADD)) {
      return createFromYaml(changeContext, changeSetContext);
    } else {
      return updateFromYaml(changeContext, changeSetContext);
    }
  }

  @Override
  public Pipeline updateFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    Pipeline previous =
        yamlSyncHelper.getPipeline(changeContext.getChange().getAccountId(), changeContext.getChange().getFilePath());
    Pipeline pipeline = setWithYamlValues(changeContext, changeSetContext, previous.toBuilder(), false);
    return pipelineService.updatePipeline(pipeline);
  }

  @Override
  public boolean validate(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    return true;
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }

  @Override
  public Pipeline get(String accountId, String yamlFilePath) {
    return yamlSyncHelper.getPipeline(accountId, yamlFilePath);
  }
}
