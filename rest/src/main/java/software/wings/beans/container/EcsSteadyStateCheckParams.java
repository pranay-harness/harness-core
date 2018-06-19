package software.wings.beans.container;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import software.wings.beans.AwsConfig;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class EcsSteadyStateCheckParams {
  private String appId;
  private String region;
  private String accountId;
  private long timeoutInMs;
  private String activityId;
  private String commandName;
  private String clusterName;
  private String serviceName;
  private AwsConfig awsConfig;
  private List<EncryptedDataDetail> encryptionDetails;
}