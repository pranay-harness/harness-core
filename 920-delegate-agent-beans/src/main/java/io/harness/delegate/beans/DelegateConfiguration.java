package io.harness.delegate.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@JsonIgnoreProperties(ignoreUnknown = true)
@FieldNameConstants(innerTypeName = "DelegateConfigurationKeys")
@Value
@Builder
@OwnedBy(HarnessTeam.DEL)
public class DelegateConfiguration {
  private List<String> delegateVersions;
  private Action action;

  public enum Action { SELF_DESTRUCT }
}
