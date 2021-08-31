package io.harness.cvng.core.services.api;

import static io.harness.annotations.dev.HarnessTeam.CV;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.beans.pagerduty.PagerDutyServiceDetail;
import io.harness.cvng.core.beans.params.ProjectParams;

import java.util.List;

@OwnedBy(CV)
public interface PagerDutyService {
  List<PagerDutyServiceDetail> getPagerDutyServices(
      ProjectParams projectParams, String connectorIdentifier, String requestGuid);
}
