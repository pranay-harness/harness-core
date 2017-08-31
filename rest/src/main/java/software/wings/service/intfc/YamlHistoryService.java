package software.wings.service.intfc;

import org.hibernate.validator.constraints.NotEmpty;
import software.wings.yaml.YamlVersion;

/**
 * Yaml History Service.
 *
 * @author bsollish
 */
public interface YamlHistoryService {
  /**
   * Save.
   *
   * @param yv the yaml version
   * @return the yaml version
   */
  YamlVersion save(YamlVersion yv);

  /**
   * Find by uuid.
   *
   * @param uuid the uuid
   * @return the yaml version
   */
  YamlVersion get(@NotEmpty String uuid);
}
