package io.harness.delegate.task.protocol;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DelegateMetaInfo {
  private String id;
  private String hostName;
}