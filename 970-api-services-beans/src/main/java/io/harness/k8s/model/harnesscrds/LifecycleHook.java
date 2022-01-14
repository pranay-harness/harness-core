package io.harness.k8s.model.harnesscrds;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class LifecycleHook {
  private ExecNewPodHook execNewPod;
  private String failurePolicy;
  private List<TagImageHook> tagImages = new ArrayList();
}
