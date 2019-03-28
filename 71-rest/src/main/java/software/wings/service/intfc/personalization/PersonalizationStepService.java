package software.wings.service.intfc.personalization;

import software.wings.beans.peronalization.PersonalizationStep;
import software.wings.sm.StateType;

public interface PersonalizationStepService {
  PersonalizationStep get(String accountId, String userId);
  PersonalizationStep addFavoriteStep(StateType step, String accountId, String userId);
  PersonalizationStep removeFavoriteStep(StateType step, String accountId, String userId);
}
