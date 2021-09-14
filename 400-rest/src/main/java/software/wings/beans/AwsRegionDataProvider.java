/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans;

import static io.harness.annotations.dev.HarnessModule._957_CG_BEANS;
import static io.harness.annotations.dev.HarnessTeam.CDP;

import static java.util.stream.Collectors.toMap;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.app.MainConfiguration;
import software.wings.stencils.DataProvider;

import com.amazonaws.regions.Regions;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

@OwnedBy(CDP)
@TargetModule(_957_CG_BEANS)
public class AwsRegionDataProvider implements DataProvider {
  @Inject private MainConfiguration mainConfiguration;

  @Override
  public Map<String, String> getData(String appId, Map<String, String> params) {
    return Arrays.stream(Regions.values())
        .filter(regions -> regions != Regions.GovCloud)
        .collect(toMap(Regions::getName,
            regions
            -> Optional.ofNullable(mainConfiguration.getAwsRegionIdToName())
                   .orElse(ImmutableMap.of(regions.getName(), regions.getName()))
                   .get(regions.getName())));
  }
}
