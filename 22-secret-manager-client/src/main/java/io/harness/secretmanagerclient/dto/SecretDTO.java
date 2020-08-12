package io.harness.secretmanagerclient.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public abstract class SecretDTO {
  String account; // TODO{phoenikx} remove this and get from context
  String org;
  String project;
  String identifier;
  String secretManager;
  String name;
  List<String> tags;
  String description;

  public SecretDTO(String account, String org, String project, String identifier, String secretManager, String name,
      List<String> tags, String description) {
    this.account = account;
    this.org = org;
    this.project = project;
    this.identifier = identifier;
    this.secretManager = secretManager;
    this.name = name;
    this.tags = tags;
    this.description = description;
  }
}
