package software.wings.service.intfc;

import java.util.Map;
import java.util.Set;

/**
 * Created by sgurubelli on 7/16/17.
 */
public interface AwsHelperResourceService {
  /**
   *
   * @return
   */
  Map<String, String> getRegions();

  /**
   * List tags list.
   *
   * @param appId             the app id
   * @param computeProviderId the compute provider id
   * @param region            the region
   * @param resourceType
   * @return the list
   */
  Set<String> listTags(String appId, String computeProviderId, String region, String resourceType);
}
