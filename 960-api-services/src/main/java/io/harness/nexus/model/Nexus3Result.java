package io.harness.nexus.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;

@lombok.Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Nexus3Result {
  private boolean success;
  private List<Nexus3ResponseData> data = new ArrayList<>();
}
