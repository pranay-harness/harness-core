/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.azure.utility;

import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import java.util.Arrays;
import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
public class AzureUtils {
  public final List<String> AZURE_GOV_REGIONS_NAMES =
      Arrays.asList(Region.GOV_US_VIRGINIA.name(), Region.GOV_US_IOWA.name(), Region.GOV_US_ARIZONA.name(),
          Region.GOV_US_TEXAS.name(), Region.GOV_US_DOD_EAST.name(), Region.GOV_US_DOD_CENTRAL.name());
}
