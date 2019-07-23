package software.wings.service.impl.yaml.handler.variable;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.eraro.ErrorCode;
import io.harness.exception.HarnessException;
import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.AllowedValueYaml;
import software.wings.beans.ArtifactStreamAllowedValueYaml;
import software.wings.beans.FeatureName;
import software.wings.beans.SettingAttribute;
import software.wings.beans.Variable;
import software.wings.beans.Variable.VariableBuilder;
import software.wings.beans.Variable.Yaml;
import software.wings.beans.VariableType;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.yaml.ChangeContext;
import software.wings.service.impl.yaml.handler.ArtifactVariableYamlHelper;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.SettingsService;
import software.wings.utils.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author rktummala on 10/28/17
 */
@Singleton
@Slf4j
public class VariableYamlHandler extends BaseYamlHandler<Variable.Yaml, Variable> {
  @Inject FeatureFlagService featureFlagService;
  @Inject SettingsService settingsService;
  @Inject ArtifactStreamService artifactStreamService;
  @Inject AppService appService;
  @Inject ArtifactVariableYamlHelper artifactVariableYamlHelper;

  private Variable toBean(ChangeContext<Yaml> changeContext) throws HarnessException {
    Yaml yaml = changeContext.getYaml();
    VariableType variableType = Utils.getEnumFromString(VariableType.class, yaml.getType());
    String accountId = changeContext.getChange().getAccountId();
    if (variableType != null && variableType.equals(VariableType.ARTIFACT)) {
      if (featureFlagService.isEnabled(FeatureName.ARTIFACT_STREAM_REFACTOR, accountId)) {
        List<String> allowedList = artifactVariableYamlHelper.computeAllowedList(accountId, yaml.getAllowedList());
        return VariableBuilder.aVariable()
            .description(yaml.getDescription())
            .fixed(yaml.isFixed())
            .mandatory(yaml.isMandatory())
            .name(yaml.getName())
            .type(variableType)
            .value(yaml.getValue())
            .allowedValues(String.join(",", allowedList)) // convert to comma separated and set this
            .build();
      }
    }
    return VariableBuilder.aVariable()
        .description(yaml.getDescription())
        .fixed(yaml.isFixed())
        .mandatory(yaml.isMandatory())
        .name(yaml.getName())
        .type(variableType)
        .value(yaml.getValue())
        .allowedValues(yaml.getAllowedValues()) // convert to comma separated and set this
        .build();
  }

  @Override
  public Yaml toYaml(Variable bean, String appId) {
    VariableType type = bean.getType();
    String accountId = appService.getAccountIdByAppId(appId);
    if (type != null && type.equals(VariableType.ARTIFACT)) {
      if (featureFlagService.isEnabled(FeatureName.ARTIFACT_STREAM_REFACTOR, accountId)) {
        List<AllowedValueYaml> allowedValueYamlList = new ArrayList<>();
        if (isNotEmpty(bean.getAllowedList())) {
          for (String id : bean.getAllowedList()) {
            ArtifactStream as = artifactStreamService.get(id);
            if (as != null) {
              SettingAttribute settingAttribute = settingsService.get(as.getSettingId());
              allowedValueYamlList.add(ArtifactStreamAllowedValueYaml.builder()
                                           .artifactServerName(settingAttribute.getName())
                                           .artifactStreamName(as.getName())
                                           .artifactStreamType(as.getArtifactStreamType())
                                           .type("ARTIFACT")
                                           .build());
            } else {
              logger.warn("Artifact Stream with id {} not found, not converting it to yaml", id);
            }
          }
          return Yaml.builder()
              .description(bean.getDescription())
              .fixed(bean.isFixed())
              .mandatory(bean.isMandatory())
              .name(bean.getName())
              .type(bean.getType().name())
              .value(bean.getValue())
              .allowedList(allowedValueYamlList)
              .build();
        }
      } else {
        logger.warn("Variable type ARTIFACT not supported, skipping processing of variable {}", bean.getName());
      }
    }
    return Yaml.builder()
        .description(bean.getDescription())
        .fixed(bean.isFixed())
        .mandatory(bean.isMandatory())
        .name(bean.getName())
        .type(bean.getType().name())
        .value(bean.getValue())
        .allowedValues(bean.getAllowedValues())
        .build();
  }

  @Override
  public Variable upsertFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    return toBean(changeContext);
  }

  @Override
  public Class getYamlClass() {
    return Variable.Yaml.class;
  }

  @Override
  public Variable get(String accountId, String yamlFilePath) {
    throw new WingsException(ErrorCode.UNSUPPORTED_OPERATION_EXCEPTION);
  }

  @Override
  public void delete(ChangeContext<Yaml> changeContext) throws HarnessException {
    // Do nothing
  }
}
