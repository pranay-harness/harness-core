package io.harness.delegate.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@AllArgsConstructor
@JsonTypeName("DELEGATE")
@JsonIgnoreProperties(ignoreUnknown = true)
@TypeAlias("delegate")
public class DelegateSetupDetails {
  private String sessionIdentifier;
  @NotNull private String name;
  private String description;
  @NotNull private DelegateSize size;
  @NotNull private String delegateConfigurationId;
}
