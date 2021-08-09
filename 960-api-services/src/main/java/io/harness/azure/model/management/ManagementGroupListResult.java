package io.harness.azure.model.management;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ManagementGroupListResult {
  private String nextLink;
  private List<ManagementGroupInfo> value;
}
