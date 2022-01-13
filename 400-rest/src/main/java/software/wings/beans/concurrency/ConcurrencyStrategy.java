/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans.concurrency;

import static software.wings.common.InfrastructureConstants.INFRA_ID_EXPRESSION;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.distribution.constraint.Constraint.Strategy;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.WingsException;

import software.wings.sm.states.HoldingScope;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.Singular;
import lombok.experimental.FieldNameConstants;

@OwnedBy(HarnessTeam.CDC)
@TargetModule(HarnessModule._957_CG_BEANS)
@Data
@Builder
@FieldNameConstants(innerTypeName = "ConcurrencyStrategyKeys")
public class ConcurrencyStrategy {
  public enum UnitType { INFRA, CUSTOM, NONE }

  @NotNull @Default private UnitType unitType = UnitType.INFRA;
  @NotNull @Default private HoldingScope holdingScope = HoldingScope.WORKFLOW;
  @NotNull @Default private Strategy strategy = Strategy.FIFO;
  @NotNull @Default private String resourceUnit = INFRA_ID_EXPRESSION;
  @Singular private List<String> notificationGroups;
  private boolean notifyTriggeredByUser;

  @JsonIgnore
  public boolean isEnabled() {
    return unitType != UnitType.NONE;
  }

  public static ConcurrencyStrategy buildFromUnit(String unitTypeName) {
    try {
      UnitType unitTypeFromName = UnitType.valueOf(unitTypeName);
      return ConcurrencyStrategy.builder().unitType(unitTypeFromName).build();
    } catch (IllegalArgumentException ex) {
      throw new InvalidArgumentsException("Not a valid Concurrency Strategy", WingsException.USER);
    }
  }
}
