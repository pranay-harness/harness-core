/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.task.utils;

import com.amazonaws.regions.Regions;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.experimental.UtilityClass;

/**
 * @author marklu on 2019-07-22
 */
@UtilityClass
public class KmsUtils {
  private static final Map<Regions, String> KMS_REGION_URL_MAP = new ConcurrentHashMap<>();
  static {
    // See AWS doc https://docs.aws.amazon.com/general/latest/gr/rande.html
    KMS_REGION_URL_MAP.put(Regions.US_EAST_1, "https://kms.us-east-1.amazonaws.com");
    KMS_REGION_URL_MAP.put(Regions.US_EAST_2, "https://kms.us-east-2.amazonaws.com");
    KMS_REGION_URL_MAP.put(Regions.US_WEST_1, "https://kms.us-west-1.amazonaws.com");
    KMS_REGION_URL_MAP.put(Regions.US_WEST_2, "https://kms.us-west-2.amazonaws.com");
    KMS_REGION_URL_MAP.put(Regions.AP_SOUTH_1, "https://kms.ap-south-1.amazonaws.com");
    KMS_REGION_URL_MAP.put(Regions.AP_NORTHEAST_1, "https://kms.ap-northeast-1.amazonaws.com");
    KMS_REGION_URL_MAP.put(Regions.AP_NORTHEAST_2, "https://kms.ap-northeast-2.amazonaws.com");
    KMS_REGION_URL_MAP.put(Regions.AP_SOUTHEAST_1, "https://kms.ap-southeast-1.amazonaws.com");
    KMS_REGION_URL_MAP.put(Regions.AP_SOUTHEAST_2, "https://kms.ap-southeast-2.amazonaws.com");
    KMS_REGION_URL_MAP.put(Regions.CA_CENTRAL_1, "https://kms.ca-central-1.amazonaws.com");
    KMS_REGION_URL_MAP.put(Regions.CN_NORTH_1, "https://kms.cn-north-1.amazonaws.com.cn");
    KMS_REGION_URL_MAP.put(Regions.CN_NORTHWEST_1, "https://kms.cn-northwest-1.amazonaws.com.cn");
    KMS_REGION_URL_MAP.put(Regions.EU_CENTRAL_1, "https://kms.eu-central-1.amazonaws.com");
    KMS_REGION_URL_MAP.put(Regions.EU_WEST_1, "https://kms.eu-west-1.amazonaws.com");
    KMS_REGION_URL_MAP.put(Regions.EU_WEST_2, "https://kms.eu-west-2.amazonaws.com");
    KMS_REGION_URL_MAP.put(Regions.EU_WEST_3, "https://kms.eu-west-3.amazonaws.com");
    KMS_REGION_URL_MAP.put(Regions.SA_EAST_1, "https://kms.sa-east-1.amazonaws.com");
    KMS_REGION_URL_MAP.put(Regions.US_GOV_EAST_1, "https://kms.us-gov-east-1.amazonaws.com");
    KMS_REGION_URL_MAP.put(Regions.GovCloud, "https://kms.us-gov-west-1.amazonaws.com");
  }

  public static String generateKmsUrl(String region) {
    Regions regions = Regions.US_EAST_1;
    if (region != null) {
      regions = Regions.fromName(region);
    }
    // If it's an unknown region, will default to US_EAST_1's URL.
    return KMS_REGION_URL_MAP.containsKey(regions) ? KMS_REGION_URL_MAP.get(regions)
                                                   : KMS_REGION_URL_MAP.get(Regions.US_EAST_1);
  }
}
