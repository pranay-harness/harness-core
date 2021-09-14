/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.beans.outcomes;

import static io.harness.annotations.dev.HarnessTeam.CI;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.dependencies.ServiceDependency;
import io.harness.pms.sdk.core.data.Outcome;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@TypeAlias("dependencyOutcome")
@JsonTypeName("dependencyOutcome")
@OwnedBy(CI)
@RecasterAlias("io.harness.beans.outcomes.DependencyOutcome")
public class DependencyOutcome implements Outcome {
  List<ServiceDependency> serviceDependencyList;
}
