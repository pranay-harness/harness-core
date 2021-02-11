package io.harness.delegate.beans.azure;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ManagementGroupData {
  private String id;
  private String name;
  private String displayName;
}
