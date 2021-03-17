package io.harness.delegate.beans;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@TargetModule(Module._955_DELEGATE_BEANS)
public class DelegateMetaInfo {
  private String id;
  private String hostName;
}
