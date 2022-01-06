/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans.marketplace;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@OwnedBy(PL)
@UtilityClass
public class MarketPlaceConstants {
  public static final String USERINVITE_ID_CLAIM_KEY = "userInviteID";
  public static final String MARKETPLACE_ID_CLAIM_KEY = "marketPlaceID";
  public static final String GCP_MARKETPLACE_TOKEN = "gcpMarketplaceToken";
  public static final String AWS_MARKETPLACE_50_INSTANCES = "instances_50";
  public static final String AWS_MARKETPLACE_200_INSTANCES = "instances_200";
  public static final String AWS_MARKETPLACE_500_INSTANCES = "instances_500";
  public static final String AWS_MARKETPLACE_750_INSTANCES = "instances_750";
  public static final String AWS_MARKETPLACE_1000_INSTANCES = "instances_1000";
  public static final String AWS_MARKETPLACE_1500_INSTANCES = "instances_1500";
  public static final String AWS_MARKETPLACE_2500_INSTANCES = "instances_2500";
}
