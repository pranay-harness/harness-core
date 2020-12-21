package io.harness.ccm.serializer.morphia;

import io.harness.ccm.commons.entities.InstanceData;
import io.harness.ccm.commons.entities.LatestClusterInfo;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrarHelperPut;

import software.wings.graphql.datafetcher.ce.recommendation.entity.K8sWorkloadRecommendation;
import software.wings.graphql.datafetcher.ce.recommendation.entity.PartialRecommendationHistogram;

import java.util.Set;

public class CECommonsMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(InstanceData.class);
    set.add(LatestClusterInfo.class);
    set.add(K8sWorkloadRecommendation.class);
    set.add(PartialRecommendationHistogram.class);
  }
  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {
    // no classes to register
  }
}
