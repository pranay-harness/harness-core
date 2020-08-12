package io.harness.secretmanagerclient.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.secretmanagerclient.ValueType;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import javax.validation.constraints.NotNull;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
public class SecretTextUpdateDTO {
  @NotNull private String value;
  @NotNull private ValueType valueType;
  @JsonIgnore private String path;
  private String description;
  @NotNull private String name;
  private List<String> tags;
  @JsonIgnore private boolean draft;
}
