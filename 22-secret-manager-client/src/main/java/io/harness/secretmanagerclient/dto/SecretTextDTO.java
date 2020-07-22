package io.harness.secretmanagerclient.dto;

import lombok.Data;

@Data
public class SecretTextDTO {
  private String accountIdentifier;
  private String orgIdentifier;
  private String projectIdentifier;
  private String identifier;
  private String name;
  private String value;
  private String path;
  private String secretManagerIdentifier;
}
