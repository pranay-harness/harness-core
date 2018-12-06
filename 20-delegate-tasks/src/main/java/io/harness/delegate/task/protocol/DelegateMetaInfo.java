package io.harness.delegate.task.protocol;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class DelegateMetaInfo {
  private String id;
  private String hostName;
}