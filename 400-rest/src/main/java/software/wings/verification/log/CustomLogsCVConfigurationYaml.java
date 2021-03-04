package software.wings.verification.log;

import software.wings.sm.states.CustomLogVerificationState;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonPropertyOrder({"type", "harnessApiVersion"})
public final class CustomLogsCVConfigurationYaml extends LogsCVConfigurationYaml {
  private CustomLogVerificationState.LogCollectionInfo logCollectionInfo;
}
