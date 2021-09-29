package io.harness.delegate.cf.apprenaming;

import lombok.Builder;
import lombok.Data;
import org.cloudfoundry.operations.applications.ApplicationSummary;

@Data
@Builder
public class AppRenamingData {
  String guid;
  String currentName;
  String newName;
  AppType appType;
  ApplicationSummary appSummary;
}
