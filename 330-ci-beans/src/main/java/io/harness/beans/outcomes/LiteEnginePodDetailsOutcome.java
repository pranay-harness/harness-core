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
import io.harness.pms.sdk.core.data.Outcome;
import io.harness.validation.Update;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.SchemaIgnore;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import org.mongodb.morphia.annotations.Id;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("podDetailsOutcome")
@JsonTypeName("podDetailsOutcome")
@OwnedBy(CI)
@RecasterAlias("io.harness.beans.outcomes.LiteEnginePodDetailsOutcome")
public class LiteEnginePodDetailsOutcome implements Outcome {
  String ipAddress;
  public static final String POD_DETAILS_OUTCOME = "podDetailsOutcome";
  @Id @NotNull(groups = {Update.class}) @SchemaIgnore private String uuid;
}
