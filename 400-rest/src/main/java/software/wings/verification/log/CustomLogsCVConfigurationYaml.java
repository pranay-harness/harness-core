package software.wings.verification.log;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.sm.states.CustomLogVerificationState;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TargetModule(Module._870_CG_YAML_BEANS)
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonPropertyOrder({"type", "harnessApiVersion"})
public final class CustomLogsCVConfigurationYaml extends LogsCVConfigurationYaml {
  private CustomLogVerificationState.LogCollectionInfo logCollectionInfo;
}
