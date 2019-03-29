package software.wings.service.intfc.personalization;

import software.wings.beans.peronalization.Personalization;
import software.wings.sm.StateType;

import java.util.List;

public interface PersonalizationService {
  int MAX_ALLOWED_RECENT = 10;

  Personalization fetch(String accountId, String userId, List<String> objects);

  Personalization addFavoriteStep(StateType step, String accountId, String userId);
  Personalization removeFavoriteStep(StateType step, String accountId, String userId);

  Personalization addRecentStep(StateType step, String accountId, String userId);
}
