package software.wings.delegatetasks.validation;

import static io.harness.network.Http.connectableHttpUrl;
import static java.util.Collections.singletonList;

import com.google.common.base.Preconditions;

import com.amazonaws.regions.Regions;
import io.harness.beans.DelegateTask;
import io.harness.security.encryption.EncryptionConfig;
import lombok.extern.slf4j.Slf4j;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Created by brett on 11/2/17
 */
@Slf4j
public abstract class AbstractSecretManagerValidation extends AbstractDelegateValidateTask {
  private static final Map<Regions, String> AWS_REGION_URL_MAP = new ConcurrentHashMap<>();
  static {
    // See AWS doc https://docs.aws.amazon.com/general/latest/gr/rande.html
    AWS_REGION_URL_MAP.put(Regions.US_EAST_1, "https://apigateway.us-east-1.amazonaws.com");
    AWS_REGION_URL_MAP.put(Regions.US_EAST_2, "https://apigateway.us-east-2.amazonaws.com");
    AWS_REGION_URL_MAP.put(Regions.US_WEST_1, "https://apigateway.us-west-1.amazonaws.com");
    AWS_REGION_URL_MAP.put(Regions.US_WEST_2, "https://apigateway.us-west-2.amazonaws.com");
    AWS_REGION_URL_MAP.put(Regions.AP_SOUTH_1, "https://apigateway.ap-south-1.amazonaws.com");
    AWS_REGION_URL_MAP.put(Regions.AP_NORTHEAST_1, "https://apigateway.ap-northeast-1.amazonaws.com");
    AWS_REGION_URL_MAP.put(Regions.AP_NORTHEAST_2, "https://apigateway.ap-northeast-2.amazonaws.com");
    AWS_REGION_URL_MAP.put(Regions.AP_SOUTHEAST_1, "https://apigateway.ap-southeast-1.amazonaws.com");
    AWS_REGION_URL_MAP.put(Regions.AP_SOUTHEAST_2, "https://apigateway.ap-southeast-2.amazonaws.com");
    AWS_REGION_URL_MAP.put(Regions.CA_CENTRAL_1, "https://apigateway.ca-central-1.amazonaws.com");
    AWS_REGION_URL_MAP.put(Regions.CN_NORTH_1, "https://apigateway.cn-north-1.amazonaws.com.cn");
    AWS_REGION_URL_MAP.put(Regions.CN_NORTHWEST_1, "https://apigateway.cn-northwest-1.amazonaws.com.cn");
    AWS_REGION_URL_MAP.put(Regions.EU_CENTRAL_1, "https://apigateway.eu-central-1.amazonaws.com");
    AWS_REGION_URL_MAP.put(Regions.EU_WEST_1, "https://apigateway.eu-west-1.amazonaws.com");
    AWS_REGION_URL_MAP.put(Regions.EU_WEST_2, "https://apigateway.eu-west-2.amazonaws.com");
    AWS_REGION_URL_MAP.put(Regions.EU_WEST_3, "https://apigateway.eu-west-3.amazonaws.com");
    AWS_REGION_URL_MAP.put(Regions.SA_EAST_1, "https://apigateway.sa-east-1.amazonaws.com");
    AWS_REGION_URL_MAP.put(Regions.US_GOV_EAST_1, "https://apigateway.us-gov-east-1.amazonaws.com");
    AWS_REGION_URL_MAP.put(Regions.GovCloud, "https://apigateway.us-gov-west-1.amazonaws.com");
  }

  AbstractSecretManagerValidation(
      String delegateId, DelegateTask delegateTask, Consumer<List<DelegateConnectionResult>> postExecute) {
    super(delegateId, delegateTask, postExecute);
  }

  public static String getAwsUrlFromRegion(String region) {
    Regions regions = Regions.US_EAST_1;
    if (region != null) {
      regions = Regions.fromName(region);
    }
    // If it's an unknown region, will default to US_EAST_1's URL.
    return AWS_REGION_URL_MAP.containsKey(regions) ? AWS_REGION_URL_MAP.get(regions)
                                                   : AWS_REGION_URL_MAP.get(Regions.US_EAST_1);
  }

  DelegateConnectionResult validateSecretManager() {
    EncryptionConfig encryptionConfig = getEncryptionConfig();
    Preconditions.checkNotNull(encryptionConfig);

    String secretManagerUrl = encryptionConfig.getEncryptionServiceUrl();
    return validateSecretManagerUrl(
        secretManagerUrl, encryptionConfig.getName(), encryptionConfig.getValidationCriteria());
  }

  @Override
  public List<DelegateConnectionResult> validate() {
    DelegateConnectionResult delegateConnectionResult = validateSecretManager();
    if (!delegateConnectionResult.isValidated()) {
      delegateConnectionResult.setCriteria(getCriteria().get(0));
      return singletonList(delegateConnectionResult);
    }
    return super.validate();
  }

  protected EncryptionConfig getEncryptionConfig() {
    for (Object parameter : getParameters()) {
      if (parameter instanceof EncryptionConfig) {
        return (EncryptionConfig) parameter;
      } else if (parameter instanceof EncryptedDataDetail) {
        return ((EncryptedDataDetail) parameter).getEncryptionConfig();
      } else if (parameter instanceof List) {
        List details = (List) parameter;
        for (Object detail : details) {
          if (detail instanceof EncryptedDataDetail) {
            return ((EncryptedDataDetail) detail).getEncryptionConfig();
          }
        }
      }
    }
    return null;
  }

  @Override
  public List<String> getCriteria() {
    EncryptionConfig encryptionConfig = getEncryptionConfig();
    return singletonList(encryptionConfig.getValidationCriteria());
  }

  private DelegateConnectionResult validateSecretManagerUrl(
      String secretManagerUrl, String secretManagerName, String validationCriteria) {
    // Secret manager URL will be null for LOCAL secret manager, consider it reachable always.
    boolean urlReachable = secretManagerUrl == null || connectableHttpUrl(secretManagerUrl);
    logger.info("Finished validating Vault config '{}' with URL {}.", secretManagerName, secretManagerUrl);
    return DelegateConnectionResult.builder().criteria(validationCriteria).validated(urlReachable).build();
  }
}
