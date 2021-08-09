package io.harness.azure.model.management;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ManagementGroupInfo {
  private String id;
  private String name;
  private String type;
  private ManagementGroupInfoProperty properties;
}
