package io.harness.delegate.beans;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DelegateMetaInfo {
  private String id;
  private String hostName;
}