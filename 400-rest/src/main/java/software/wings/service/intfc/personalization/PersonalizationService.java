/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.intfc.personalization;

import software.wings.beans.peronalization.Personalization;
import software.wings.sm.StateType;

import java.util.List;
import java.util.Set;

public interface PersonalizationService {
  int MAX_ALLOWED_RECENT = 10;

  Personalization fetch(String accountId, String userId, List<String> objects);

  Personalization addFavoriteStep(StateType step, String accountId, String userId);
  Personalization removeFavoriteStep(StateType step, String accountId, String userId);

  Personalization addRecentStep(StateType step, String accountId, String userId);

  Personalization addFavoriteTemplate(String templateId, String accountId, String userId);
  Personalization removeFavoriteTemplate(String templateId, String accountId, String userId);
  Set<String> fetchFavoriteTemplates(String accountId, String userId);
}
