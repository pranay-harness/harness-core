package io.harness.ccm.serializer.morphia;

import io.harness.ccm.budget.ApplicationBudgetScope;
import io.harness.ccm.budget.Budget;
import io.harness.ccm.budget.ClusterBudgetScope;
import io.harness.ccm.budget.PerspectiveBudgetScope;
import io.harness.ccm.commons.entities.CEDataCleanupRequest;
import io.harness.ccm.commons.entities.CEMetadataRecord;
import io.harness.ccm.commons.entities.InstanceData;
import io.harness.ccm.commons.entities.LatestClusterInfo;
import io.harness.ccm.commons.entities.recommendation.K8sNodeRecommendation;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrarHelperPut;

import software.wings.graphql.datafetcher.ce.recommendation.entity.K8sWorkloadRecommendation;
import software.wings.graphql.datafetcher.ce.recommendation.entity.PartialRecommendationHistogram;

import java.util.Set;

public class CECommonsMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(InstanceData.class);
    set.add(CEMetadataRecord.class);
    set.add(CEDataCleanupRequest.class);
    set.add(LatestClusterInfo.class);
    set.add(K8sWorkloadRecommendation.class);
    set.add(PartialRecommendationHistogram.class);
    set.add(Budget.class);
    set.add(K8sNodeRecommendation.class);
  }
  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {
    h.put("ccm.budget.ApplicationBudgetScope", ApplicationBudgetScope.class);
    h.put("ccm.budget.ClusterBudgetScope", ClusterBudgetScope.class);
    h.put("ccm.budget.PerspectiveBudgetScope", PerspectiveBudgetScope.class);
  }
}
