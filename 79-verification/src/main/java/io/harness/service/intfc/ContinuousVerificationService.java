package io.harness.service.intfc;

/**
 * Created by rsingh on 10/9/18.
 */
public interface ContinuousVerificationService {
  boolean triggerDataCollection(String accountId);

  /**
   * Creates tasks for Learning Engine
   * @param accountId
   */
  void triggerDataAnalysis(String accountId);
}
