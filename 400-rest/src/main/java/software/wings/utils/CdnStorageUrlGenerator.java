/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.utils;

import static software.wings.utils.SignedUrls.signUrl;

import software.wings.cdn.CdnConfig;

import com.google.inject.Singleton;
import java.net.URI;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Singleton
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CdnStorageUrlGenerator {
  public static final String FREE_CLUSTER_FOLDER_NAME = "freemium";
  public static final String PAID_CLUSTER_FOLDER_NAME = "premium";
  static final int SIGNED_URL_VALIDITY_DURATON_IN_SECONDS = 3600;
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

  final String getClusterTypeFolderName(boolean isFreeCluster) {
    return isFreeCluster ? FREE_CLUSTER_FOLDER_NAME : PAID_CLUSTER_FOLDER_NAME;
  }

  public String getDelegateJarUrl(String version) {
    String delegateUrl = cdnConfig.getUrl() + "/" + String.format(cdnConfig.getDelegateJarPath(), version);
    try {
      return signUrl(delegateUrl, key, cdnConfig.getKeyName(), getExpirationTime());
    } catch (InvalidKeyException | NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  private Date getExpirationTime() {
    Calendar cal = Calendar.getInstance();
    cal.setTime(new Date());
    cal.add(Calendar.SECOND, SIGNED_URL_VALIDITY_DURATON_IN_SECONDS);

    return cal.getTime();
  }

  public String getWatcherMetaDataFileUrl(String env) {
    String url =
        cdnConfig.getUrl() + "/" + String.format(cdnConfig.getWatcherMetaDataFilePath(), env, clusterTypeFolderName);
    return URI.create(url).normalize().toString();
  }

  public String getWatcherJarBaseUrl() {
    return cdnConfig.getUrl() + "/" + cdnConfig.getWatcherJarBasePath();
  }

  public String getWatcherJarUrl(String version) {
    String watcherJarUrl = cdnConfig.getUrl() + "/" + cdnConfig.getWatcherJarBasePath() + "/"
        + String.format(cdnConfig.getWatcherJarPath(), version);
    return URI.create(watcherJarUrl).normalize().toString();
  }
}
