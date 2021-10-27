package io.harness.delegate.filter;

import static io.harness.annotations.dev.HarnessTeam.DEL;
import static io.harness.filter.FilterConstants.DELEGATE_FILTER;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateInstanceStatus;
import io.harness.filter.FilterType;
import io.harness.filter.dto.FilterPropertiesDTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(DELEGATE_FILTER)
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@ApiModel("DelegateFilterProperties")
@Schema(name = "DelegateFilterProperties", description = "Properties of a Delegate Filter.")
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@OwnedBy(DEL)
public class DelegateFilterPropertiesDTO extends FilterPropertiesDTO {
  private DelegateInstanceStatus status;
  private String description;
  private String hostName;
  private String delegateName;
  private String delegateType;
  private String delegateGroupIdentifier;

  @Override
  public FilterType getFilterType() {
    return FilterType.DELEGATE;
  }
}