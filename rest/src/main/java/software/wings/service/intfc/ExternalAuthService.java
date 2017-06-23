package software.wings.service.intfc;

import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.ExternalServiceAuthToken;

/**
 * ExternalAuthService Service.
 *
 * @author raghu
 */
public interface ExternalAuthService {
  ExternalServiceAuthToken save(ExternalServiceAuthToken app);

  ExternalServiceAuthToken get(@NotEmpty String uuid);

  ExternalServiceAuthToken update(ExternalServiceAuthToken app);

  void delete(@NotEmpty String appId);
}
