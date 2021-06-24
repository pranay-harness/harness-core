package io.harness.beans.serializer;

import static io.harness.annotations.dev.HarnessTeam.CI;
import static io.harness.common.NGExpressionUtils.matchesInputSetPattern;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.pms.yaml.YamlNode.UUID_FIELD_NAME;

import static java.lang.String.format;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.yaml.extended.ArchiveFormat;
import io.harness.beans.yaml.extended.CIShellType;
import io.harness.beans.yaml.extended.ImagePullPolicy;
import io.harness.encryption.SecretRefData;
import io.harness.exception.ngexception.CIStageExecutionUserException;
import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.extended.ci.codebase.Build;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
@OwnedBy(CI)
public class RunTimeInputHandler {
  public static String UNRESOLVED_PARAMETER = "UNRESOLVED_PARAMETER";
  public boolean resolveGitClone(ParameterField<Boolean> cloneRepository) {
    if (cloneRepository == null || cloneRepository.isExpression() || cloneRepository.getValue() == null) {
      return true;
    } else {
      return (boolean) cloneRepository.fetchFinalValue();
    }
  }

  public Build resolveBuild(ParameterField<Build> buildDetails) {
    if (buildDetails == null || buildDetails.isExpression() || buildDetails.getValue() == null) {
      return null;
    } else {
      return buildDetails.getValue();
    }
  }

  public ArchiveFormat resolveArchiveFormat(ParameterField<ArchiveFormat> archiveFormat) {
    if (archiveFormat == null || archiveFormat.isExpression() || archiveFormat.getValue() == null) {
      return ArchiveFormat.TAR;
    } else {
      return ArchiveFormat.fromString(archiveFormat.fetchFinalValue().toString());
    }
  }

  public CIShellType resolveShellType(ParameterField<CIShellType> shellType) {
    if (shellType == null || shellType.isExpression() || shellType.getValue() == null) {
      return CIShellType.SH;
    } else {
      return CIShellType.fromString(shellType.fetchFinalValue().toString());
    }
  }

  public String resolveImagePullPolicy(ParameterField<ImagePullPolicy> pullPolicy) {
    if (pullPolicy == null || pullPolicy.isExpression() || pullPolicy.getValue() == null) {
      return null;
    } else {
      return ImagePullPolicy.fromString(pullPolicy.fetchFinalValue().toString()).getYamlName();
    }
  }

  public boolean resolveBooleanParameter(ParameterField<Boolean> booleanParameterField, Boolean defaultValue) {
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

  public Integer resolveIntegerParameter(ParameterField<Integer> parameterField, Integer defaultValue) {
    if (parameterField == null || parameterField.isExpression() || parameterField.getValue() == null) {
      return defaultValue;
    } else {
      return (Integer) parameterField.fetchFinalValue();
    }
  }

  public String resolveStringParameter(String fieldName, String stepType, String stepIdentifier,
      ParameterField<String> parameterField, boolean isMandatory) {
    if (parameterField == null) {
      if (isMandatory) {
        throw new CIStageExecutionUserException(
            format("Failed to resolve mandatory field %s in step type %s with identifier %s", fieldName, stepType,
                stepIdentifier));
      } else {
        return UNRESOLVED_PARAMETER;
      }
    }

    // It only checks input set pattern. Variable can be resolved on lite engine.
    if (parameterField.isExpression() && matchesInputSetPattern(parameterField.getExpressionValue())) {
      if (isMandatory) {
        throw new CIStageExecutionUserException(
            format("Failed to resolve mandatory field %s in step type %s with identifier %s", fieldName, stepType,
                stepIdentifier));
      } else {
        log.warn(format("Failed to resolve optional field %s in step type %s with identifier %s", fieldName, stepType,
            stepIdentifier));
        return UNRESOLVED_PARAMETER;
      }
    }

    return (String) parameterField.fetchFinalValue();
  }

  public SecretRefData resolveSecretRefWithDefaultValue(String fieldName, String stepType, String stepIdentifier,
      ParameterField<SecretRefData> parameterField, boolean isMandatory) {
    if (parameterField == null || parameterField.getValue() == null) {
      if (isMandatory) {
        throw new CIStageExecutionUserException(
            format("Failed to resolve mandatory field %s in step type %s with identifier %s", fieldName, stepType,
                stepIdentifier));
      } else {
        return null;
      }
    }

    return parameterField.getValue();
  }

  public String resolveStringParameterWithDefaultValue(String fieldName, String stepType, String stepIdentifier,
      ParameterField<String> parameterField, boolean isMandatory, String defaultValue) {
    if (parameterField == null) {
      if (isMandatory && isEmpty(defaultValue)) {
        throw new CIStageExecutionUserException(
            format("Failed to resolve mandatory field %s in step type %s with identifier %s", fieldName, stepType,
                stepIdentifier));
      } else {
        if (defaultValue != null) {
          return defaultValue;
        }
        return "";
      }
    }

    // It only checks input set pattern. Variable can be resolved on lite engine.
    if (parameterField.isExpression() && matchesInputSetPattern(parameterField.getExpressionValue())) {
      if (isMandatory && isEmpty(defaultValue)) {
        throw new CIStageExecutionUserException(
            format("Failed to resolve mandatory field %s in step type %s with identifier %s", fieldName, stepType,
                stepIdentifier));
      } else {
        log.warn(format("Failed to resolve optional field %s in step type %s with identifier %s", fieldName, stepType,
            stepIdentifier));
        return defaultValue;
      }
    }

    return (String) parameterField.fetchFinalValue();
  }

  public Map<String, String> resolveMapParameter(String fieldName, String stepType, String stepIdentifier,
      ParameterField<Map<String, String>> parameterField, boolean isMandatory) {
    if (parameterField == null || parameterField.getValue() == null) {
      if (isMandatory) {
        throw new CIStageExecutionUserException(
            format("Failed to resolve mandatory field %s in step type %s with identifier %s", fieldName, stepType,
                stepIdentifier));
      } else {
        return null;
      }
    }

    if (parameterField.isExpression()) {
      if (isMandatory) {
        throw new CIStageExecutionUserException(
            format("Failed to resolve mandatory field %s in step type %s with identifier %s", fieldName, stepType,
                stepIdentifier));
      } else {
        log.warn(format("Failed to resolve optional field %s in step type %s with identifier %s", fieldName, stepType,
            stepIdentifier));
        return null;
      }
    }

    Map<String, String> m = parameterField.getValue();
    if (isNotEmpty(m)) {
      m.remove(UUID_FIELD_NAME);
    }
    return m;
  }

  public List<String> resolveListParameter(String fieldName, String stepType, String stepIdentifier,
      ParameterField<List<String>> parameterField, boolean isMandatory) {
    if (parameterField == null || parameterField.getValue() == null) {
      if (isMandatory) {
        throw new CIStageExecutionUserException(
            format("Failed to resolve mandatory field %s in step type %s with identifier %s", fieldName, stepType,
                stepIdentifier));
      }
    }

    if (parameterField.isExpression()) {
      if (isMandatory) {
        throw new CIStageExecutionUserException(
            format("Failed to resolve mandatory field %s in step type %s with identifier %s", fieldName, stepType,
                stepIdentifier));
      } else {
        log.warn(format("Failed to resolve optional field %s in step type %s with identifier %s", fieldName, stepType,
            stepIdentifier));
        return new ArrayList<>();
      }
    }

    return parameterField.getValue();
  }
}
