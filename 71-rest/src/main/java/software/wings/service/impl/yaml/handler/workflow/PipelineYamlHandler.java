package software.wings.service.impl.yaml.handler.workflow;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.exception.WingsException.USER;
import static java.util.stream.Collectors.toList;
import static software.wings.beans.Pipeline.Yaml;
import static software.wings.utils.Validator.notNullCheck;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.HarnessException;
import io.harness.exception.WingsException;
import software.wings.beans.Application;
import software.wings.beans.Pipeline;
import software.wings.beans.PipelineStage;
import software.wings.beans.PipelineStage.PipelineStageElement;
import software.wings.beans.yaml.Change;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.YamlType;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.handler.YamlHandlerFactory;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.PipelineService;

import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    Yaml yaml = Yaml.builder()
                    .harnessApiVersion(getHarnessApiVersion())
                    .description(bean.getDescription())
                    .pipelineStages(pipelineStageYamlList)
                    .build();

    updateYamlWithAdditionalInfo(bean, appId, yaml);
    return yaml;
  }

  @Override
  public Pipeline upsertFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    Pipeline previous = get(changeContext.getChange().getAccountId(), changeContext.getChange().getFilePath());

    Pipeline current = toBean(changeContext, changeSetContext, previous);

    current.setSyncFromGit(changeContext.getChange().isSyncFromGit());
    if (previous != null) {
      current.setUuid(previous.getUuid());
      current = pipelineService.update(current);
    } else {
      current = pipelineService.save(current);
    }

    changeContext.setEntity(current);
    return current;
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
    Optional<Application> optionalApplication = yamlHelper.getApplicationIfPresent(accountId, filePath);
    if (!optionalApplication.isPresent()) {
      return;
    }

    Pipeline pipeline = yamlHelper.getPipelineByAppIdYamlPath(optionalApplication.get().getUuid(), filePath);
    if (pipeline != null) {
      pipelineService.deleteByYamlGit(
          pipeline.getAppId(), pipeline.getUuid(), changeContext.getChange().isSyncFromGit());
    }
  }
}
