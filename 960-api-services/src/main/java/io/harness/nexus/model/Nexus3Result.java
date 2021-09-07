package io.harness.nexus.model;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;

@lombok.Data
@JsonIgnoreProperties(ignoreUnknown = true)
@OwnedBy(HarnessTeam.CDC)
public class Nexus3Result {
  private boolean success;
  private List<Nexus3ResponseData> data = new ArrayList<>();
}
