package io.harness.cvng.state;

import static io.harness.annotations.dev.HarnessTeam.CV;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CV)
public interface CVNGVerificationTaskService {
  String create(CVNGVerificationTask cvngVerificationTask);
  void markDone(String cvngVerificationTaskId);
  void markTimedOut(String cvngVerificationTaskId);

  CVNGVerificationTask get(String cvngVerificationTaskId);

  CVNGVerificationTask getByActivityId(String activityId);
}
