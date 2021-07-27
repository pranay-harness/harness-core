package io.harness.nexus.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.ArrayList;
import java.util.List;

@lombok.Data
@OwnedBy(HarnessTeam.CDC)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Nexus3Result {
  private boolean success;
  private List<Nexus3ResponseData> data = new ArrayList<>();
}
