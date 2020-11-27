package io.harness.cvng.state;

public interface CVNGVerificationTaskService {
  String create(CVNGVerificationTask cvngVerificationTask);
  void markDone(String cvngVerificationTaskId);
  void markTimedOut(String cvngVerificationTaskId);

  CVNGVerificationTask get(String cvngVerificationTaskId);
}
