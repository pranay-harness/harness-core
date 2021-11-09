package io.harness.delegate.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Transient;

@JsonIgnoreProperties(ignoreUnknown = true)
@FieldNameConstants(innerTypeName = "DelegateConfigurationKeys")
@Value
@Builder
public class DelegateConfiguration {
  private List<String> delegateVersions;
  private Action action;
  @Transient private boolean accountVersion;

  public enum Action { SELF_DESTRUCT }
}
