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
