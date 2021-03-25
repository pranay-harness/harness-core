package io.harness.batch.processing.cloudevents.aws.ecs.service.support.impl;

import static io.harness.data.structure.HasPredicate.hasNone;

import static com.amazonaws.regions.Regions.GovCloud;
import static com.amazonaws.regions.Regions.US_GOV_EAST_1;

import io.harness.batch.processing.cloudevents.aws.ecs.service.support.intfc.AwsHelperResourceService;
import io.harness.batch.processing.config.BatchMainConfig;

import software.wings.beans.NameValuePair;
import software.wings.beans.NameValuePair.NameValuePairBuilder;

import com.amazonaws.regions.Regions;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AwsHelperResourceServiceImpl implements AwsHelperResourceService {
  @Autowired private BatchMainConfig batchMainConfig;

  @Override
  public List<NameValuePair> getAwsRegions() {
    Map<String, String> awsRegionIdToName = batchMainConfig.getAwsRegionIdToName();
    if (hasNone(awsRegionIdToName)) {
      awsRegionIdToName = new LinkedHashMap<>();
    }
    List<NameValuePair> awsRegions = new ArrayList<>();
    for (Regions region : Regions.values()) {
      String regionName = region.getName();
      if (!ImmutableSet.of(GovCloud.getName(), US_GOV_EAST_1.getName()).contains(regionName)) {
        NameValuePairBuilder regionNameValuePair = NameValuePair.builder().value(regionName);
        if (awsRegionIdToName.containsKey(regionName)) {
          regionNameValuePair.name(awsRegionIdToName.get(regionName));
        } else {
          regionNameValuePair.name(regionName);
        }
        awsRegions.add(regionNameValuePair.build());
      }
    }
    return awsRegions;
  }
}
