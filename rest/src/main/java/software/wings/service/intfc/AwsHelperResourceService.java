package software.wings.service.intfc;

import software.wings.beans.NameValuePair;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by sgurubelli on 7/16/17.
 */

public interface AwsHelperResourceService {
  /**
   * The method will be removed after a while.
   * @return
   */
  @Deprecated Map<String, String> getRegions();

  List<NameValuePair> getAwsRegions();

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
