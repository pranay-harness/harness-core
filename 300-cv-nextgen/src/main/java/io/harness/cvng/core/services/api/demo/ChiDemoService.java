package io.harness.cvng.core.services.api.demo;

import io.harness.cvng.beans.change.DemoChangeEventDTO;
import io.harness.cvng.core.beans.params.ProjectParams;

public interface ChiDemoService {
  void registerDemoChangeEvent(ProjectParams projectParams, DemoChangeEventDTO demoChangeEventDTO);
}
