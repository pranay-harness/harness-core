package software.wings.helpers.ext.ami;

import software.wings.beans.AwsConfig;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;
import java.util.Map;

/**
 * Created by sgurubelli on 12/14/17.
 */
public interface AmiService {
  /**
   * Get AMI Images
   * @param awsConfig
   * @param encryptionDetails
   * @param region
   * @param tags
   * @param maxNumberOfBuilds
   * @param platform
   * @return
   */
  List<BuildDetails> getBuilds(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region,
      Map<String, List<String>> tags, Map<String, String> filterMap, int maxNumberOfBuilds);
}
