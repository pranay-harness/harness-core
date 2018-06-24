package software.wings.service.impl.yaml.handler.workflow;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static java.util.stream.Collectors.toList;
import static software.wings.beans.Pipeline.Yaml;
import static software.wings.exception.WingsException.USER;
import static software.wings.utils.Validator.notNullCheck;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.beans.Pipeline;
import software.wings.beans.PipelineStage;
import software.wings.beans.PipelineStage.PipelineStageElement;
import software.wings.beans.yaml.Change;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.YamlType;
import software.wings.exception.HarnessException;
import software.wings.exception.WingsException;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.handler.YamlHandlerFactory;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.PipelineService;

import java.util.List;
import java.util.Map;

/**
 * @author rktummala on 11/2/17
 */
@Singleton
public class PipelineYamlHandler extends BaseYamlHandler<Yaml, Pipeline> {
  @Inject private YamlHelper yamlHelper;
  @Inject private PipelineService pipelineService;
  @Inject private YamlHandlerFactory yamlHandlerFactory;

  private Pipeline toBean(ChangeContext<Yaml> context, List<ChangeContext> changeSetContext, Pipeline previous)
      throws HarnessException {
    try {
      Yaml yaml = context.getYaml();
      Change change = context.getChange();

      String appId = yamlHelper.getAppId(change.getAccountId(), change.getFilePath());
      notNullCheck("Could not retrieve valid app from path: " + change.getFilePath(), appId, USER);

      context.setEntityIdMap(getPreviousStageElementMap(previous));

      List<PipelineStage> pipelineStages = Lists.newArrayList();
      if (yaml.getPipelineStages() != null) {
        PipelineStageYamlHandler pipelineStageYamlHandler = yamlHandlerFactory.getYamlHandler(YamlType.PIPELINE_STAGE);

        // Pipeline stages
        pipelineStages =
            yaml.getPipelineStages()
                .stream()
                .map(stageYaml -> {
                  try {
                    ChangeContext.Builder clonedContext = cloneFileChangeContext(context, stageYaml);
                    return pipelineStageYamlHandler.upsertFromYaml(clonedContext.build(), changeSetContext);
                  } catch (HarnessException e) {
                    throw new WingsException(e);
                  }
                })
                .collect(toList());
      }

      String name = yamlHelper.getNameFromYamlFilePath(context.getChange().getFilePath());

      return Pipeline.builder()
          .appId(appId)
          .description(yaml.getDescription())
          .name(name)
          .pipelineStages(pipelineStages)
          .build();

    } catch (WingsException ex) {
      throw new HarnessException(ex);
    }
  }

  private Map<String, String> getPreviousStageElementMap(Pipeline previous) {
    Map<String, String> entityIdMap = Maps.newHashMap();

    if (previous == null) {
      return entityIdMap;
    }

    List<PipelineStage> pipelineStages = previous.getPipelineStages();
    if (isEmpty(pipelineStages)) {
      return entityIdMap;
    }

    pipelineStages.forEach(pipelineStage -> {
      List<PipelineStageElement> pipelineStageElements = pipelineStage.getPipelineStageElements();
      if (isEmpty(pipelineStageElements)) {
        return;
      }

      pipelineStageElements.forEach(
          stageElement -> entityIdMap.putIfAbsent(stageElement.getName(), stageElement.getUuid()));
    });

    return entityIdMap;
  }

  @Override
  public Yaml toYaml(Pipeline bean, String appId) {
    PipelineStageYamlHandler pipelineStageYamlHandler = yamlHandlerFactory.getYamlHandler(YamlType.PIPELINE_STAGE);
    List<PipelineStage> pipelineStages = bean.getPipelineStages();
    List<PipelineStage.Yaml> pipelineStageYamlList =
        pipelineStages.stream()
            .map(pipelineStage -> pipelineStageYamlHandler.toYaml(pipelineStage, bean.getAppId()))
            .collect(toList());

    return Yaml.builder()
        .harnessApiVersion(getHarnessApiVersion())
        .description(bean.getDescription())
        .pipelineStages(pipelineStageYamlList)
        .build();
  }

  @Override
  public Pipeline upsertFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    Pipeline previous = get(changeContext.getChange().getAccountId(), changeContext.getChange().getFilePath());

    Pipeline current = toBean(changeContext, changeSetContext, previous);

    if (previous != null) {
      current.setUuid(previous.getUuid());
      return pipelineService.update(current);
    } else {
      return pipelineService.save(current);
    }
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }

  @Override
  public Pipeline get(String accountId, String yamlFilePath) {
    return yamlHelper.getPipeline(accountId, yamlFilePath);
  }

  @Override
  public void delete(ChangeContext<Yaml> changeContext) throws HarnessException {
    String accountId = changeContext.getChange().getAccountId();
    String filePath = changeContext.getChange().getFilePath();
    Pipeline pipeline = get(accountId, filePath);
    if (pipeline != null) {
      pipelineService.deletePipeline(pipeline.getAppId(), pipeline.getUuid());
    }
  }
}
