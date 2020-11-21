package io.harness.notification.remote.bos;

import io.harness.Team;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TemplateDTO {
  private String identifier;
  private Team team;
  private long createdAt;
  private long lastModifiedAt;
  private byte[] file;
}
