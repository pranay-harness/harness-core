/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.checks.buildpulse.dto;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TestFlakiness {
  @SerializedName("id") @Expose int id;
  @SerializedName("name") @Expose String name;
  @SerializedName("suite") @Expose String suite;
  @SerializedName("class") @Expose String clazz;
  @SerializedName("file") @Expose String file;
  @SerializedName("disruptiveness") @Expose double disruptiveness;
  @SerializedName("nondeterministic_negative_result_count") @Expose int nondeterministicNegativeResultCount;
  @SerializedName("nondeterminism_first_recorded_at") @Expose String nondeterminismFirstRecordedAt;
  @SerializedName("latest_nondeterministic_commit_oid") @Expose String latestNondeterministicCommitOid;
  @SerializedName("latest_nondeterministic_negative_result") @Expose TestResult latestNondeterministicNegativeResult;
  @SerializedName("latest_nondeterministic_positive_result") @Expose TestResult latestNondeterministicPositiveResult;

  public double getPassChance() {
    return 1 - disruptiveness;
  }

  public String getFqdn() {
    return getClazz() + "." + getName();
  }
}
