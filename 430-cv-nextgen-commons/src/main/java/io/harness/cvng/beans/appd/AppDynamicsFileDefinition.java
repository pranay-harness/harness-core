package io.harness.cvng.beans.appd;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Value;
import org.jetbrains.annotations.NotNull;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class AppDynamicsFileDefinition implements Comparable<AppDynamicsFileDefinition> {
  String name;
  FileType type;

  @Override
  public int compareTo(@NotNull AppDynamicsFileDefinition o) {
    return name.compareTo(o.name);
  }

  public enum FileType {
    @JsonProperty("leaf") @SerializedName("leaf") LEAF,
    @JsonProperty("folder") @SerializedName("folder") FOLDER
  }
}
