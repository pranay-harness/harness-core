package io.harness.k8s.model.harnesscrds;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class LifecycleHook {
  private ExecNewPodHook execNewPod;
  private String failurePolicy;
  private List<TagImageHook> tagImages = new ArrayList();
}