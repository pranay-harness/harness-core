/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
