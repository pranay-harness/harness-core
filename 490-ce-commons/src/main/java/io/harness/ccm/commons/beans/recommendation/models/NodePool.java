package io.harness.ccm.commons.beans.recommendation.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class NodePool {
  @SerializedName("role") private String role;

  @SerializedName("sumNodes") private Long sumNodes;

  @SerializedName("vm") private VirtualMachine vm;

  @SerializedName("vmClass") private String vmClass;
}
