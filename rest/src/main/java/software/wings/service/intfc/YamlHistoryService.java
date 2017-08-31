package software.wings.service.intfc;

import org.hibernate.validator.constraints.NotEmpty;
import software.wings.yaml.YamlVersion;
import software.wings.yaml.YamlVersion.Type;

import java.util.List;

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

  /**
   * Find by entityId.
   *
   * @param entityId the entityId
   * @return the yaml version
   */
  List<YamlVersion> getList(String entityId, Type type);
}
