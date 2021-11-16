package io.harness.cvng.servicelevelobjective.services;

import io.harness.cvng.core.beans.params.ProjectParams;
import io.harness.cvng.servicelevelobjective.beans.UserJourneyDTO;
import io.harness.cvng.servicelevelobjective.beans.UserJourneyResponse;
import io.harness.ng.beans.PageResponse;

public interface UserJourneyService {
  UserJourneyResponse create(ProjectParams projectParams, UserJourneyDTO userJourneyDTO);

  PageResponse<UserJourneyResponse> getUserJourneys(ProjectParams projectParams, Integer offset, Integer pageSize);
}
