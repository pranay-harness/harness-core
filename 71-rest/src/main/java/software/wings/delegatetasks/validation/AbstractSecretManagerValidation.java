package software.wings.delegatetasks.validation;

import static io.harness.network.Http.connectableHttpUrl;
import static java.util.Collections.singletonList;

import com.google.common.base.Preconditions;

import com.amazonaws.regions.Regions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.beans.KmsConfig;
import software.wings.beans.VaultConfig;
import software.wings.security.EncryptionType;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.security.EncryptionConfig;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Created by brett on 11/2/17
 */
public abstract class AbstractSecretManagerValidation extends AbstractDelegateValidateTask {
  private static final Logger logger = LoggerFactory.getLogger(AbstractDelegateValidateTask.class);
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

  protected DelegateConnectionResult validateSecretManager() {
    EncryptionConfig encryptionConfig = getEncryptionConfig();
    // local encryption
    if (encryptionConfig == null) {
      return DelegateConnectionResult.builder()
          .criteria("encryption type: " + EncryptionType.LOCAL)
          .validated(true)
          .build();
    }

    Preconditions.checkNotNull(encryptionConfig);
    if (encryptionConfig instanceof KmsConfig) {
      KmsConfig kmsConfig = (KmsConfig) encryptionConfig;
      Regions regions = Regions.US_EAST_1;
      if (kmsConfig.getRegion() != null) {
        regions = Regions.fromName(kmsConfig.getRegion());
      }
      // If it's an unknown region, will default to US_EAST_1's URL.
      String kmsUrl = AWS_REGION_URL_MAP.containsKey(regions) ? AWS_REGION_URL_MAP.get(regions)
                                                              : AWS_REGION_URL_MAP.get(Regions.US_EAST_1);
      return validateSecretManagerUrl(kmsUrl, kmsConfig.getName(), kmsConfig.getValidationCriteria());
    } else if (encryptionConfig instanceof VaultConfig) {
      VaultConfig vaultConfig = (VaultConfig) encryptionConfig;
      return validateSecretManagerUrl(
          vaultConfig.getVaultUrl(), vaultConfig.getName(), vaultConfig.getValidationCriteria());
    } else {
      throw new IllegalStateException("Invalid encryptionConfig " + encryptionConfig);
    }
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
      if (parameter instanceof KmsConfig) {
        return (KmsConfig) parameter;
      } else if (parameter instanceof VaultConfig) {
        return (VaultConfig) parameter;
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
    if (encryptionConfig instanceof KmsConfig) {
      KmsConfig kmsConfig = (KmsConfig) encryptionConfig;
      return singletonList(kmsConfig.getValidationCriteria());
    } else if (encryptionConfig instanceof VaultConfig) {
      VaultConfig vaultConfig = (VaultConfig) encryptionConfig;
      return singletonList(vaultConfig.getValidationCriteria());
    } else {
      throw new IllegalStateException("Unsupported encryption config: " + encryptionConfig);
    }
  }

  private DelegateConnectionResult validateSecretManagerUrl(
      String secretManagerUrl, String secretManagerName, String validationCriteria) {
    boolean urlReachable = connectableHttpUrl(secretManagerUrl);
    logger.info("Finished validating Vault config '{}' with URL {}.", secretManagerName, secretManagerUrl);
    return DelegateConnectionResult.builder().criteria(validationCriteria).validated(urlReachable).build();
  }
}
