package io.harness.ng.projectsummary;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProjectAggregation {
  String org_identifier;
  int totaldeployment;
  int totalbuild;
  int totalfeature;
}
