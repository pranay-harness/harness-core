package software.wings.utils;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Singleton;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import software.wings.cdn.CdnConfig;

import java.net.URI;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@Singleton
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CdnStorageUrlGenerator {
  public static final String FREE_CLUSTER_FOLDER_NAME = "freemium";
  public static final String PAID_CLUSTER_FOLDER_NAME = "premium";
  static final String algorithm = "HmacSHA1";
  static final int SIGNED_URL_VALIDITY_DURATON_IN_SECONDS = 3600;
  static final String DEFAULT_ERROR_STRING = "ERROR_GETTING_DATA";
  private String clusterTypeFolderName;
  byte[] key;
  CdnConfig cdnConfig;

  public CdnStorageUrlGenerator(CdnConfig cdnConfig, boolean isFreeCluster) {
    this.cdnConfig = cdnConfig;
    if (cdnConfig != null && StringUtils.isNotBlank(cdnConfig.getKeySecret())) {
      key = Base64.getUrlDecoder().decode(cdnConfig.getKeySecret());
    } else {
      key = null;
    }
    clusterTypeFolderName = getClusterTypeFolderName(isFreeCluster);
  }

  @VisibleForTesting
  final String getClusterTypeFolderName(boolean isFreeCluster) {
    return isFreeCluster ? FREE_CLUSTER_FOLDER_NAME : PAID_CLUSTER_FOLDER_NAME;
  }

  public String getDelegateJarUrl(String version) {
    String delegateUrl = cdnConfig.getUrl() + "/" + String.format(cdnConfig.getDelegateJarPath(), version);
    return getSignedUrl(delegateUrl);
  }

  public String getWatcherJarUrl(String version) {
    return cdnConfig.getUrl() + "/" + String.format(cdnConfig.getWatcherJarPath(), version);
  }

  public String getWatcherMetaDataFileUrl(String env) {
    String url = cdnConfig.getUrl() + "/"
        + String.format(
              cdnConfig.getWatcherMetaDataFilePath(), getWatcherMetaDataEnvFolderName(env), clusterTypeFolderName);
    return URI.create(url).normalize().toString();
  }

  private String getWatcherMetaDataEnvFolderName(String env) {
    switch (env) {
      case "qa":
        return "qa";
      case "pr":
        return "pr";
      case "dev":
        return "dev";
      default:
        return "";
    }
  }

  private String getSignedUrl(String url) {
    Calendar cal = Calendar.getInstance();
    cal.setTime(new Date());
    cal.add(Calendar.SECOND, SIGNED_URL_VALIDITY_DURATON_IN_SECONDS);

    final long expiryTime = cal.getTime().getTime() / 1000;
    String urlToSign =
        url + (url.contains("?") ? "&" : "?") + "Expires=" + expiryTime + "&KeyName=" + cdnConfig.getKeyName();
    String encoded = getSignature(key, urlToSign);
    return encoded != null ? (urlToSign + "&Signature=" + encoded) : DEFAULT_ERROR_STRING;
  }

  private String getSignature(byte[] privateKey, String input) {
    final int offset = 0;
    try {
      Key secretKeySpec = new SecretKeySpec(privateKey, offset, privateKey.length, algorithm);
      Mac mac = Mac.getInstance(algorithm);
      mac.init(secretKeySpec);
      return Base64.getUrlEncoder().encodeToString(mac.doFinal(input.getBytes(UTF_8)));
    } catch (InvalidKeyException ike) {
      logger.error("InvalidKeyException occurred while creating delegate url");
    } catch (NoSuchAlgorithmException nse) {
      logger.error("NoSuchAlgorithmException occurred while creating delegate url");
    }
    return null;
  }
}
