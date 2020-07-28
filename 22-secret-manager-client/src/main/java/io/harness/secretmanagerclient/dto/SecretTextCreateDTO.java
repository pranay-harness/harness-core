package io.harness.secretmanagerclient.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SecretTextCreateDTO {
  private String accountIdentifier;
  private String orgIdentifier;
  private String projectIdentifier;
  private String identifier;
  private String secretManagerIdentifier;
  private String secretManagerName;
  private String name;
  private String value;
  private String path;
  private List<String> tags;
  private String description;
}
