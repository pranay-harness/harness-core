package io.harness.cvng.cdng.services.api;

import io.harness.cvng.cdng.entities.CVNGStepTask;

public interface CVNGStepTaskService {
  void create(CVNGStepTask cvngStepTask);
  void notifyCVNGStep(CVNGStepTask entity);
  CVNGStepTask getByCallBackId(String callBackId);
}
