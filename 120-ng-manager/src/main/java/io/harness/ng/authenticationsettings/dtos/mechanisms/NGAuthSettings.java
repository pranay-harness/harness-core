package io.harness.ng.authenticationsettings.dtos.mechanisms;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
@OwnedBy(HarnessTeam.PL)
public abstract class NGAuthSettings {}
