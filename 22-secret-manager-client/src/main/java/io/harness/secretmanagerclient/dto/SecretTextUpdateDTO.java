package io.harness.secretmanagerclient.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SecretTextUpdateDTO {
  private String name;
  private String value;
  private String path;
}
