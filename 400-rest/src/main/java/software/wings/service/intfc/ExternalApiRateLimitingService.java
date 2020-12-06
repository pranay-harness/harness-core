package software.wings.service.intfc;

import org.hibernate.validator.constraints.NotEmpty;

public interface ExternalApiRateLimitingService {
  boolean rateLimitRequest(@NotEmpty String key);
  double getMaxQPMPerManager();
}
