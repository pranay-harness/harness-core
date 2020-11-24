package io.harness.k8s.model.statssummary;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class PVCRef {
  @SerializedName("name") private String name;
  @SerializedName("namespace") private String namespace;
}
