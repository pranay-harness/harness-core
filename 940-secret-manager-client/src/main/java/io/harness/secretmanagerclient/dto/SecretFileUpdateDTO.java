package io.harness.secretmanagerclient.dto;

import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SecretFileUpdateDTO {
  private String description;
  @NotNull private List<String> tags;
  @NotNull private String name;
}
