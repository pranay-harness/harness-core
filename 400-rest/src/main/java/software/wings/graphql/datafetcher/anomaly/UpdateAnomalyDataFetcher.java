/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.anomaly;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.ccm.anomaly.entities.AnomalyEntity;
import io.harness.ccm.anomaly.mappers.QlAnomalyMapper;
import io.harness.ccm.anomaly.service.itfc.AnomalyService;

import software.wings.graphql.datafetcher.BaseMutatorDataFetcher;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.graphql.schema.type.aggregation.anomaly.QLAnomalyInput;
import software.wings.graphql.schema.type.aggregation.anomaly.QLUpdateAnomalyPayLoad;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@TargetModule(HarnessModule._375_CE_GRAPHQL)
@OwnedBy(CE)
public class UpdateAnomalyDataFetcher extends BaseMutatorDataFetcher<QLAnomalyInput, QLUpdateAnomalyPayLoad> {
  @Inject @Autowired private AnomalyService anomalyService;

  public UpdateAnomalyDataFetcher() {
    super(QLAnomalyInput.class, QLUpdateAnomalyPayLoad.class);
  }

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected QLUpdateAnomalyPayLoad mutateAndFetch(QLAnomalyInput input, MutationContext mutationContext) {
    AnomalyEntity updateAnomaly = AnomalyEntity.builder().id(input.getAnomalyId()).note(input.getNote()).build();
    AnomalyEntity updatedAnomaly = anomalyService.update(updateAnomaly);

    return QLUpdateAnomalyPayLoad.builder()
        .clientMutationId(null)
        .anomaly(QlAnomalyMapper.toDto(updatedAnomaly))
        .build();
  }
}
