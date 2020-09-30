package io.harness.cvng;

import java.time.Duration;

public interface CVConstants {
  String SERVICE_BASE_URL = "/cv-nextgen";
  Duration VERIFICATION_JOB_INSTANCE_EXPIRY_DURATION = Duration.ofDays(30);
  double DEPLOYMENT_RISK_SCORE_FAILURE_THRESHOLD = 0.5;
}
