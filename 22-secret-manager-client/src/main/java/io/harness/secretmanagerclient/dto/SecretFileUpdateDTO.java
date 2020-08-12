package io.harness.secretmanagerclient.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import javax.validation.constraints.NotNull;

@Data
@Builder
public class SecretFileUpdateDTO {
  private String description;
  @NotNull private List<String> tags;
  @NotNull private String name;
}
