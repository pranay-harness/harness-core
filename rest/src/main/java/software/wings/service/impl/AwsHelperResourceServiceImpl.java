package software.wings.service.impl;

import static java.util.stream.Collectors.toMap;

import com.google.common.collect.ImmutableMap;

import com.amazonaws.regions.Regions;
import software.wings.app.MainConfiguration;
import software.wings.service.intfc.AwsHelperResourceService;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by sgurubelli on 7/16/17.
 */
@Singleton
public class AwsHelperResourceServiceImpl implements AwsHelperResourceService {
  @Inject private MainConfiguration mainConfiguration;

  public Map<String, String> getRegions() {
    return Arrays.stream(Regions.values())
        .filter(regions -> regions != Regions.GovCloud)
        .collect(toMap(Regions::getName,
            regions
            -> Optional.ofNullable(mainConfiguration.getAwsRegionIdToName())
                   .orElse(ImmutableMap.of(regions.getName(), regions.getName()))
                   .get(regions.getName())));
  }
}
