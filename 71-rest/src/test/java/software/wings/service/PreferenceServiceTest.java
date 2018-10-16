package software.wings.service;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.Base.ACCOUNT_ID_KEY;
import static software.wings.service.impl.PreferenceServiceImpl.USER_ID_KEY;

import com.google.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mongodb.morphia.mapping.Mapper;
import software.wings.WingsBaseTest;
import software.wings.beans.Base;
import software.wings.beans.DeploymentPreference;
import software.wings.beans.Preference;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.PreferenceService;
import software.wings.utils.WingsTestConstants;

public class PreferenceServiceTest extends WingsBaseTest {
  private static final String TEST_USER_ID = "123";
  private static final String TEST_PREFERENCE_ID = "AEtq6ZDIQMyH2JInYeifWQ";

  private static Preference preference = new DeploymentPreference();

  @Inject private WingsPersistence wingsPersistence;
  @InjectMocks @Inject private PreferenceService preferenceService;

  @Before
  public void setUp() {
    preference.setAccountId(WingsTestConstants.INTEGRATION_TEST_ACCOUNT_ID);
    preference.setUserId(TEST_USER_ID);
    preference.setUuid(TEST_PREFERENCE_ID);
    preference.setAppId(Base.GLOBAL_APP_ID);
  }

  @Test
  public void shouldGet() {
    Preference savedPreference = wingsPersistence.saveAndGet(Preference.class, preference);
    assertThat(
        preferenceService.get(WingsTestConstants.INTEGRATION_TEST_ACCOUNT_ID, TEST_USER_ID, savedPreference.getUuid()))
        .isEqualTo(savedPreference);
  }

  @Test
  public void shouldCreate() {
    Preference createdPreference =
        preferenceService.save(WingsTestConstants.INTEGRATION_TEST_ACCOUNT_ID, TEST_USER_ID, preference);
    assertThat(wingsPersistence.get(Preference.class, createdPreference.getUuid())).isEqualTo(preference);
  }

  @Test
  public void shouldList() {
    preference.setAppId(Base.GLOBAL_APP_ID);
    preference.setAccountId(WingsTestConstants.INTEGRATION_TEST_ACCOUNT_ID);
    preference.setUserId(TEST_USER_ID);
    Preference savedPreference = wingsPersistence.saveAndGet(Preference.class, preference);
    assertThat(preferenceService.list(
                   aPageRequest().addFilter("accountId", EQ, WingsTestConstants.INTEGRATION_TEST_ACCOUNT_ID).build(),
                   TEST_USER_ID))
        .hasSize(1)
        .containsExactly(savedPreference);
  }

  @Test
  public void shouldUpdate() {
    Preference savedPreference = wingsPersistence.saveAndGet(Preference.class, preference);
    savedPreference.setName("NEW NAME");
    preferenceService.update(
        WingsTestConstants.INTEGRATION_TEST_ACCOUNT_ID, TEST_USER_ID, savedPreference.getUuid(), savedPreference);
    assertThat(wingsPersistence.get(Preference.class, savedPreference.getUuid())).isEqualTo(preference);
  }

  @Test
  public void shouldDelete() {
    wingsPersistence.saveAndGet(Preference.class, preference);
    preferenceService.delete(WingsTestConstants.INTEGRATION_TEST_ACCOUNT_ID, TEST_USER_ID, TEST_PREFERENCE_ID);
    assertThat(wingsPersistence.createQuery(Preference.class)
                   .filter(ACCOUNT_ID_KEY, WingsTestConstants.INTEGRATION_TEST_ACCOUNT_ID)
                   .filter(USER_ID_KEY, TEST_USER_ID)
                   .filter(Mapper.ID_KEY, TEST_PREFERENCE_ID)
                   .get())
        .isNull();
  }
}
