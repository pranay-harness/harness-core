/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.beans.sweepingoutputs;

import static io.harness.annotations.dev.HarnessTeam.CI;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.yaml.extended.infrastrucutre.Infrastructure;
import io.harness.pms.sdk.core.data.ExecutionSweepingOutput;
import io.harness.validation.Update;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.SchemaIgnore;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import org.mongodb.morphia.annotations.Id;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("podCleanupDetails")
@JsonTypeName("podCleanupDetails")
@OwnedBy(CI)
@RecasterAlias("io.harness.beans.sweepingoutputs.PodCleanupDetails")
public class PodCleanupDetails implements ExecutionSweepingOutput {
  List<String> cleanUpContainerNames;
  Infrastructure infrastructure;
  String podName;
  public static final String CLEANUP_DETAILS = "podCleanupDetails";
  @Id @NotNull(groups = {Update.class}) @SchemaIgnore private String uuid;
}
