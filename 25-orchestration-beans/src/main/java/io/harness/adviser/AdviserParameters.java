package io.harness.adviser;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDC)
@Redesign
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "class")
public interface AdviserParameters {}
