/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.sto.utils;

import static io.harness.common.NGExpressionUtils.matchesInputSetPattern;

import static java.lang.String.format;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.steps.stepinfo.security.BlackDuckStepInfo;
import io.harness.beans.steps.stepinfo.security.BurpStepInfo;
import io.harness.beans.steps.stepinfo.security.CheckmarxStepInfo;
import io.harness.beans.steps.stepinfo.security.FortifyOnDemandStepInfo;
import io.harness.beans.steps.stepinfo.security.PrismaCloudStepInfo;
import io.harness.beans.steps.stepinfo.security.SnykStepInfo;
import io.harness.beans.steps.stepinfo.security.SonarqubeStepInfo;
import io.harness.beans.steps.stepinfo.security.VeracodeStepInfo;
import io.harness.beans.steps.stepinfo.security.ZapStepInfo;
import io.harness.beans.steps.stepinfo.security.shared.STOGenericStepInfo;
import io.harness.beans.steps.stepinfo.security.shared.STOYamlAdvancedSettings;
import io.harness.beans.steps.stepinfo.security.shared.STOYamlArgs;
import io.harness.beans.steps.stepinfo.security.shared.STOYamlAuth;
import io.harness.beans.steps.stepinfo.security.shared.STOYamlBlackduckToolData;
import io.harness.beans.steps.stepinfo.security.shared.STOYamlCheckmarxToolData;
import io.harness.beans.steps.stepinfo.security.shared.STOYamlFODToolData;
import io.harness.beans.steps.stepinfo.security.shared.STOYamlImage;
import io.harness.beans.steps.stepinfo.security.shared.STOYamlIngestion;
import io.harness.beans.steps.stepinfo.security.shared.STOYamlInstance;
import io.harness.beans.steps.stepinfo.security.shared.STOYamlJavaParameters;
import io.harness.beans.steps.stepinfo.security.shared.STOYamlLog;
import io.harness.beans.steps.stepinfo.security.shared.STOYamlSonarqubeToolData;
import io.harness.beans.steps.stepinfo.security.shared.STOYamlTarget;
import io.harness.beans.steps.stepinfo.security.shared.STOYamlVeracodeToolData;
import io.harness.beans.steps.stepinfo.security.shared.STOYamlZapToolData;
import io.harness.exception.ngexception.CIStageExecutionUserException;
import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.sto.variables.STOYamlAuthType;
import io.harness.yaml.sto.variables.STOYamlGenericConfig;
import io.harness.yaml.sto.variables.STOYamlImageType;
import io.harness.yaml.sto.variables.STOYamlLogLevel;
import io.harness.yaml.sto.variables.STOYamlScanMode;
import io.harness.yaml.sto.variables.STOYamlTargetType;

import com.google.inject.Singleton;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.STO)
@Singleton
@Slf4j
public final class STOSettingsUtils {
  public static final String SECURITY_ENV_PREFIX = "SECURITY_";
  public static final String PRODUCT_PROJECT_VERSION = "product_project_version";
  public static final String PRODUCT_PROJECT_NAME = "product_project_name";
  public static final String TOOL_PROJECT_NAME = "tool.project_name";
  public static final Integer ZAP_DEFAULT_PORT = 8080;
  public static final Integer DEFAULT_INSTANCE_PORT = 80;

  private STOSettingsUtils() {
    throw new IllegalStateException("Utility class");
  }

  private static boolean resolveBooleanParameter(ParameterField<Boolean> booleanParameterField, Boolean defaultValue) {
    if (booleanParameterField == null || booleanParameterField.isExpression()
        || booleanParameterField.getValue() == null) {
      if (defaultValue != null) {
        return defaultValue;
      } else {
        return false;
      }
    } else {
      return (boolean) booleanParameterField.fetchFinalValue();
    }
  }

  private static Integer resolveIntegerParameter(ParameterField<Integer> parameterField, Integer defaultValue) {
    if (parameterField == null || parameterField.isExpression() || parameterField.getValue() == null) {
      return defaultValue;
    } else {
      try {
        return Integer.parseInt(parameterField.fetchFinalValue().toString());
      } catch (Exception exception) {
        log.info("Handling exception: {}", exception.getMessage());
        throw new CIStageExecutionUserException(
            format("Invalid value %s, Value should be number", parameterField.fetchFinalValue().toString()));
      }
    }
  }

  private static String resolveStringParameter(String fieldName, String stepType, String stepIdentifier,
      ParameterField<String> parameterField, boolean isMandatory) {
    if (parameterField == null) {
      if (isMandatory) {
        throw new CIStageExecutionUserException(
            format("Failed to resolve mandatory field %s in step type %s with identifier %s", fieldName, stepType,
                stepIdentifier));
      } else {
        return null;
      }
    }

    // It only checks input set pattern. Variable can be resolved on lite engine.
    if (parameterField.isExpression() && matchesInputSetPattern(parameterField.getExpressionValue())) {
      if (isMandatory) {
        throw new CIStageExecutionUserException(
            format("Failed to resolve mandatory field %s in step type %s with identifier %s", fieldName, stepType,
                stepIdentifier));
      } else {
        return null;
      }
    }

    return (String) parameterField.fetchFinalValue();
  }

  public static String getSTOKey(String value) {
    return SECURITY_ENV_PREFIX + value.toUpperCase(Locale.ROOT);
  }

  private static Map<String, String> processSTOAuthFields(STOYamlAuth authData, String stepType, String identifier) {
    Map<String, String> map = new HashMap<>();

    if (authData != null) {
      STOYamlAuthType authType = authData.getType();

      map.put(getSTOKey("product_auth_type"),
          authType != null ? authType.getYamlName() : STOYamlAuthType.API_KEY.getYamlName());
      map.put(getSTOKey("product_domain"),
          resolveStringParameter("auth.domain", stepType, identifier, authData.getDomain(), false));
      map.put(getSTOKey("product_api_version"),
          resolveStringParameter("auth.version", stepType, identifier, authData.getVersion(), false));
      map.put(getSTOKey("product_access_id"),
          resolveStringParameter("auth.accessId", stepType, identifier, authData.getAccessId(), false));
      map.put(getSTOKey("product_access_token"),
          resolveStringParameter("auth.accessToken", stepType, identifier, authData.getAccessToken(), false));
    }

    return map;
  }

  private static Map<String, String> processSTOImageFields(STOYamlImage imageData, String stepType, String identifier) {
    Map<String, String> map = new HashMap<>();

    if (imageData != null) {
      STOYamlImageType imageType = imageData.getType();

      map.put(getSTOKey("container_type"),
          imageType != null ? imageType.getYamlName() : STOYamlImageType.DOCKER_V2.getYamlName());
      map.put(getSTOKey("container_domain"),
          resolveStringParameter("image.domain", stepType, identifier, imageData.getDomain(), false));
      map.put(getSTOKey("container_region"),
          resolveStringParameter("image.region", stepType, identifier, imageData.getRegion(), false));
      map.put(getSTOKey("container_access_id"),
          resolveStringParameter("image.access_id", stepType, identifier, imageData.getAccessId(), false));
      map.put(getSTOKey("container_access_token"),
          resolveStringParameter("image.access_token", stepType, identifier, imageData.getAccessToken(), false));
      map.put(getSTOKey("container_image_name"),
          resolveStringParameter("image.name", stepType, identifier, imageData.getName(), false));
      map.put(getSTOKey("container_image_tag"),
          resolveStringParameter("image.tag", stepType, identifier, imageData.getTag(), false));
    }

    return map;
  }

  private static Map<String, String> processSTOInstanceFields(
      STOYamlInstance instanceData, String stepType, String identifier) {
    Map<String, String> map = new HashMap<>();

    if (instanceData != null) {
      map.put(getSTOKey("instance_domain"),
          resolveStringParameter("instance.domain", stepType, identifier, instanceData.getDomain(), false));
      map.put(getSTOKey("instance_path"),
          resolveStringParameter("instance.path", stepType, identifier, instanceData.getPath(), false));
      map.put(getSTOKey("instance_protocol"),
          resolveStringParameter("instance.protocol", stepType, identifier, instanceData.getProtocol(), false));
      map.put(getSTOKey("instance_port"),
          String.valueOf(resolveIntegerParameter(instanceData.getPort(), DEFAULT_INSTANCE_PORT)));
      map.put(getSTOKey("instance_access_id"),
          resolveStringParameter("instance.access_id", stepType, identifier, instanceData.getAccessId(), false));
      map.put(getSTOKey("instance_access_token"),
          resolveStringParameter("instance.access_token", stepType, identifier, instanceData.getAccessToken(), false));
    }

    return map;
  }

  private static Map<String, String> processSTOTargetFields(STOYamlTarget target, String stepType, String identifier) {
    Map<String, String> map = new HashMap<>();

    if (target != null) {
      Boolean targetSsl = resolveBooleanParameter(target.getSsl(), Boolean.TRUE);

      map.put(getSTOKey("workspace"),
          resolveStringParameter("target.workspace", stepType, identifier, target.getWorkspace(), false));
      map.put(getSTOKey("bypass_ssl_check"), String.valueOf(!targetSsl));

      STOYamlTargetType targetType = target.getType();
      map.put(getSTOKey("scan_type"),
          targetType != null ? targetType.getYamlName() : STOYamlTargetType.REPOSITORY.getYamlName());

      String targetName = resolveStringParameter("target.name", stepType, identifier, target.getName(), true);
      String targetVariant = resolveStringParameter("target.variant", stepType, identifier, target.getVariant(), true);

      map.put(getSTOKey("target_name"), targetName);
      map.put(getSTOKey("target_variant"), targetVariant);

      switch (target.getType()) {
        case INSTANCE:
          map.put(getSTOKey("instance_identifier"), targetName);
          map.put(getSTOKey("instance_environment"), targetVariant);
          break;
        case REPOSITORY:
          map.put(getSTOKey("repository_project"), targetName);
          map.put(getSTOKey("repository_branch"), targetVariant);
          break;
        case CONTAINER:
          map.put(getSTOKey("container_project"), targetName);
          map.put(getSTOKey("container_tag"), targetVariant);
          break;
        case CONFIGURATION:
          map.put(getSTOKey("configuration_type"), targetName);
          map.put(getSTOKey("configuration_environment"), targetVariant);
          break;
        default:
          break;
      }
    }

    return map;
  }

  private static Map<String, String> processSTOAdvancedSettings(
      STOYamlAdvancedSettings advancedSettings, String stepType, String identifier) {
    Map<String, String> map = new HashMap<>();

    if (advancedSettings != null) {
      STOYamlLog logData = advancedSettings.getLog();
      if (logData != null) {
        STOYamlLogLevel logLevel = logData.getLevel();

        map.put(getSTOKey("log_level"), logLevel != null ? logLevel.getYamlName() : STOYamlLogLevel.INFO.getYamlName());
        map.put(getSTOKey("log_serializer"),
            resolveStringParameter("log.serializer", stepType, identifier, logData.getSerializer(), false));
      }

      STOYamlArgs argsData = advancedSettings.getArgs();
      if (argsData != null) {
        map.put(
            getSTOKey("tool_args"), resolveStringParameter("args.cli", stepType, identifier, argsData.getCli(), false));
        map.put(getSTOKey("tool_passthrough"),
            resolveStringParameter("args.passthrough", stepType, identifier, argsData.getPassthrough(), false));
      }

      map.put(getSTOKey("fail_on_severity"),
          String.valueOf(resolveIntegerParameter(advancedSettings.getFailOnSeverity(), 0)));
      map.put(getSTOKey("include_raw"),
          String.valueOf(resolveBooleanParameter(advancedSettings.getIncludeRaw(), Boolean.TRUE)));

      Boolean advancedSsl = resolveBooleanParameter(advancedSettings.getSsl(), Boolean.TRUE);
      map.put(getSTOKey("verify_ssl"), String.valueOf(advancedSsl));
    }

    return map;
  }

  private static Map<String, String> processSTOIngestionFields(
      STOYamlIngestion ingestion, String stepType, String identifier) {
    Map<String, String> map = new HashMap<>();

    if (ingestion != null) {
      map.put(getSTOKey("ingestion_file"),
          resolveStringParameter("ingestion.file", stepType, identifier, ingestion.getFile(), false));
    }

    return map;
  }

  private static Map<String, String> processSTOBlackDuckFields(
      BlackDuckStepInfo stepInfo, String stepType, String identifier) {
    Map<String, String> map = new HashMap<>();

    map.putAll(processSTOAuthFields(stepInfo.getAuth(), stepType, identifier));
    map.putAll(processSTOImageFields(stepInfo.getImage(), stepType, identifier));

    STOYamlBlackduckToolData toolData = stepInfo.getTool();

    if (toolData != null) {
      map.put(getSTOKey(PRODUCT_PROJECT_NAME),
          resolveStringParameter(TOOL_PROJECT_NAME, stepType, identifier, toolData.getProjectName(), false));
      map.put(getSTOKey(PRODUCT_PROJECT_VERSION),
          resolveStringParameter("tool.project_version", stepType, identifier, toolData.getProjectVersion(), false));
    }

    return map;
  }

  private static Map<String, String> processSTOBurpFields(BurpStepInfo stepInfo, String stepType, String identifier) {
    Map<String, String> map = new HashMap<>();

    map.putAll(processSTOInstanceFields(stepInfo.getInstance(), stepType, identifier));

    return map;
  }

  private static Map<String, String> processSTOCheckmarxFields(
      CheckmarxStepInfo stepInfo, String stepType, String identifier) {
    Map<String, String> map = new HashMap<>();

    map.putAll(processSTOAuthFields(stepInfo.getAuth(), stepType, identifier));
    map.putAll(processSTOImageFields(stepInfo.getImage(), stepType, identifier));

    STOYamlCheckmarxToolData toolData = stepInfo.getTool();

    if (toolData != null) {
      map.put(getSTOKey("product_team_name"),
          resolveStringParameter("tool.team_name", stepType, identifier, toolData.getTeamName(), false));
      map.put(getSTOKey(PRODUCT_PROJECT_NAME),
          resolveStringParameter(TOOL_PROJECT_NAME, stepType, identifier, toolData.getProjectName(), false));
    }

    return map;
  }

  private static Map<String, String> processSTOFODFields(
      FortifyOnDemandStepInfo stepInfo, String stepType, String identifier) {
    Map<String, String> map = new HashMap<>();

    map.putAll(processSTOAuthFields(stepInfo.getAuth(), stepType, identifier));
    map.putAll(processSTOImageFields(stepInfo.getImage(), stepType, identifier));

    STOYamlFODToolData toolData = stepInfo.getTool();

    if (toolData != null) {
      map.put(getSTOKey("product_app_name"),
          resolveStringParameter("tool.app_name", stepType, identifier, toolData.getAppName(), false));
      map.put(getSTOKey("product_audit_type"),
          resolveStringParameter("tool.audit_type", stepType, identifier, toolData.getAuditType(), false));
      map.put(getSTOKey("product_data_center"),
          resolveStringParameter("tool.data_center", stepType, identifier, toolData.getDataCenter(), false));
      map.put(getSTOKey("product_lookup_type"),
          resolveStringParameter("tool.lookup_type", stepType, identifier, toolData.getLoookupType(), false));
      map.put(getSTOKey("product_release_name"),
          resolveStringParameter("tool.release_name", stepType, identifier, toolData.getReleaseName(), false));
      map.put(getSTOKey("product_entitlement"),
          resolveStringParameter("tool.entitlement", stepType, identifier, toolData.getEntitlement(), false));
      map.put(getSTOKey("product_owner_id"),
          resolveStringParameter("tool.owner_id", stepType, identifier, toolData.getOwnerId(), false));
      map.put(getSTOKey("product_scan_settings"),
          resolveStringParameter("tool.scan_settings", stepType, identifier, toolData.getScanSettings(), false));
      map.put(getSTOKey("product_scan_type"),
          resolveStringParameter("tool.scan_type", stepType, identifier, toolData.getScanType(), false));
      map.put(getSTOKey("product_target_language"),
          resolveStringParameter("tool.target_language", stepType, identifier, toolData.getTargetLanguage(), false));
      map.put(getSTOKey("product_target_language_version"),
          resolveStringParameter(
              "tool.target_language_version", stepType, identifier, toolData.getTargetLanguageVersion(), false));
    }

    return map;
  }

  private static Map<String, String> processSTOPrismaCloudFields(
      PrismaCloudStepInfo stepInfo, String stepType, String identifier) {
    Map<String, String> map = new HashMap<>();

    map.putAll(processSTOAuthFields(stepInfo.getAuth(), stepType, identifier));
    map.putAll(processSTOImageFields(stepInfo.getImage(), stepType, identifier));

    return map;
  }

  private static Map<String, String> processSTOSonarqubeFields(
      SonarqubeStepInfo stepInfo, String stepType, String identifier) {
    Map<String, String> map = new HashMap<>();

    map.putAll(processSTOAuthFields(stepInfo.getAuth(), stepType, identifier));

    STOYamlSonarqubeToolData toolData = stepInfo.getTool();

    if (toolData != null) {
      map.put(getSTOKey("product_exclude"),
          resolveStringParameter("tool.exclude", stepType, identifier, toolData.getExclude(), false));
      map.put(getSTOKey("product_include"),
          resolveStringParameter("tool.include", stepType, identifier, toolData.getInclude(), false));

      STOYamlJavaParameters javaParameters = toolData.getJava();

      if (javaParameters != null) {
        map.put(getSTOKey("product_java_binaries"),
            resolveStringParameter("tool.java.binaries", stepType, identifier, javaParameters.getBinaries(), false));
        map.put(getSTOKey("product_java_libraries"),
            resolveStringParameter("tool.java.libraries", stepType, identifier, javaParameters.getLibraries(), false));
      }
    }

    return map;
  }

  private static Map<String, String> processSTOSnykFields(SnykStepInfo stepInfo, String stepType, String identifier) {
    Map<String, String> map = new HashMap<>();

    map.putAll(processSTOAuthFields(stepInfo.getAuth(), stepType, identifier));
    map.putAll(processSTOImageFields(stepInfo.getImage(), stepType, identifier));

    return map;
  }

  private static Map<String, String> processSTOVeracodeFields(
      VeracodeStepInfo stepInfo, String stepType, String identifier) {
    Map<String, String> map = new HashMap<>();

    map.putAll(processSTOAuthFields(stepInfo.getAuth(), stepType, identifier));

    STOYamlVeracodeToolData toolData = stepInfo.getTool();

    if (toolData != null) {
      map.put(getSTOKey("product_app_id"),
          resolveStringParameter("tool.app_id", stepType, identifier, toolData.getAppId(), false));
      map.put(getSTOKey(PRODUCT_PROJECT_NAME),
          resolveStringParameter(TOOL_PROJECT_NAME, stepType, identifier, toolData.getProjectName(), false));
    }

    return map;
  }

  private static Map<String, String> processSTOZapFields(ZapStepInfo stepInfo, String stepType, String identifier) {
    Map<String, String> map = new HashMap<>();

    map.putAll(processSTOInstanceFields(stepInfo.getInstance(), stepType, identifier));

    STOYamlZapToolData toolData = stepInfo.getTool();

    if (toolData != null) {
      map.put(getSTOKey("product_context"),
          resolveStringParameter("tool.context", stepType, identifier, toolData.getContext(), false));
      map.put(
          getSTOKey("zap_custom_port"), String.valueOf(resolveIntegerParameter(toolData.getPort(), ZAP_DEFAULT_PORT)));
    }

    return map;
  }

  private static String getProductConfigName(STOYamlGenericConfig config) {
    if (config != null) {
      config.getYamlName();
    }

    return STOYamlGenericConfig.DEFAULT.getYamlName();
  }

  private static String getPolicyType(STOYamlScanMode scanMode) {
    if (scanMode != null) {
      return scanMode.getPluginName();
    }
    return STOYamlScanMode.ORCHESTRATION.getPluginName();
  }

  public static Map<String, String> getSTOPluginEnvVariables(STOGenericStepInfo stepInfo, String identifier) {
    Map<String, String> map = new HashMap<>();
    String stepType = stepInfo.getStepType().getType();

    STOYamlGenericConfig config = stepInfo.getConfig();
    STOYamlScanMode scanMode = stepInfo.getMode();

    map.put(getSTOKey("product_name"), stepInfo.getProductName());
    map.put(getSTOKey("product_config_name"), getProductConfigName(config));
    map.put(getSTOKey("policy_type"), getPolicyType(scanMode));

    map.putAll(processSTOTargetFields(stepInfo.getTarget(), stepType, identifier));
    map.putAll(processSTOAdvancedSettings(stepInfo.getAdvanced(), stepType, identifier));
    map.putAll(processSTOIngestionFields(stepInfo.getIngestion(), stepType, identifier));

    switch (stepInfo.getSTOStepType()) {
      case BLACKDUCK:
        map.putAll(processSTOBlackDuckFields((BlackDuckStepInfo) stepInfo, stepType, identifier));
        break;
      case BURP:
        map.putAll(processSTOBurpFields((BurpStepInfo) stepInfo, stepType, identifier));
        break;
      case CHECKMARX:
        map.putAll(processSTOCheckmarxFields((CheckmarxStepInfo) stepInfo, stepType, identifier));
        break;
      case FORTIFY_ON_DEMAND:
        map.putAll(processSTOFODFields((FortifyOnDemandStepInfo) stepInfo, stepType, identifier));
        break;
      case PRISMA_CLOUD:
        map.putAll(processSTOPrismaCloudFields((PrismaCloudStepInfo) stepInfo, stepType, identifier));
        break;
      case SONARQUBE:
        map.putAll(processSTOSonarqubeFields((SonarqubeStepInfo) stepInfo, stepType, identifier));
        break;
      case SNYK:
        map.putAll(processSTOSnykFields((SnykStepInfo) stepInfo, stepType, identifier));
        break;
      case VERACODE:
        map.putAll(processSTOVeracodeFields((VeracodeStepInfo) stepInfo, stepType, identifier));
        break;
      case ZAP:
        map.putAll(processSTOZapFields((ZapStepInfo) stepInfo, stepType, identifier));
        break;
      default:
        break;
    }
    map.values().removeAll(Collections.singleton(null));

    return map;
  }
}
