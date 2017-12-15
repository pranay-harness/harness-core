package software.wings.helpers.ext.ami;

import software.wings.beans.AwsConfig;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;

/**
 * Created by sgurubelli on 12/14/17.
 */
public interface AmiService {
  /**
   * Get Image details
   * @param awsConfig
   * @param encryptionDetails
   * @param region
   * @param tag
   * @param maxNumberOfBuilds
   * @return
   */
  List<BuildDetails> getBuilds(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region,
      String tag, int maxNumberOfBuilds);

  /**
   * Lists aws regions
   *
   * @param awsConfig aws config
   * @return
   */
  List<String> listRegions(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails);
}
