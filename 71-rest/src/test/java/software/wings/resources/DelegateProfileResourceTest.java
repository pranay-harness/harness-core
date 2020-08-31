package software.wings.resources;

import static io.harness.rule.OwnerRule.VUK;
import static java.util.Arrays.asList;
import static javax.ws.rs.client.Entity.entity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.DelegateProfile;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.ArgumentCaptor;
import software.wings.exception.WingsExceptionMapper;
import software.wings.service.intfc.DelegateProfileService;
import software.wings.utils.ResourceTestRule;

import java.util.Arrays;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;

public class DelegateProfileResourceTest {
  private static HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
  private static DelegateProfileService delegateProfileService = mock(DelegateProfileService.class);

  @Parameters
  public static String[] data() {
    return new String[] {null, "https://testUrl"};
  }

  @ClassRule
  public static final ResourceTestRule RESOURCES =

      ResourceTestRule.builder()
          .instance(new DelegateProfileResource(delegateProfileService))
          .instance(new AbstractBinder() {
            @Override
            protected void configure() {
              bind(httpServletRequest).to(HttpServletRequest.class);
            }
          })
          .type(WingsExceptionMapper.class)
          .build();

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void shouldListDelegateProfiles() {
    PageResponse<DelegateProfile> pageResponse = new PageResponse<>();
    pageResponse.setResponse(asList(DelegateProfile.builder().build()));
    pageResponse.setTotal(1l);
    when(delegateProfileService.list(any(PageRequest.class))).thenReturn(pageResponse);
    RestResponse<PageResponse<DelegateProfile>> restResponse =
        RESOURCES.client()
            .target("/delegate-profiles?accountId=" + ACCOUNT_ID)
            .request()
            .get(new GenericType<RestResponse<PageResponse<DelegateProfile>>>() {});
    PageRequest<DelegateProfile> pageRequest = new PageRequest<>();
    pageRequest.setOffset("0");
    verify(delegateProfileService, atLeastOnce()).list(pageRequest);
    assertThat(restResponse.getResource().getResponse().size()).isEqualTo(1);
    assertThat(restResponse.getResource().getResponse().get(0)).isNotNull();
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void shouldDeleteDelegateProfile() {
    RESOURCES.client().target("/delegate-profiles/" + ID_KEY + "?accountId=" + ACCOUNT_ID).request().delete();

    verify(delegateProfileService, atLeastOnce()).delete(ACCOUNT_ID, ID_KEY);
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void shouldAddDelegateProfile() {
    DelegateProfile delegateProfile = DelegateProfile.builder().uuid(ID_KEY).accountId(ACCOUNT_ID).build();

    when(delegateProfileService.add(any(DelegateProfile.class))).thenReturn(delegateProfile);
    RestResponse<DelegateProfile> restResponse = RESOURCES.client()
                                                     .target("/delegate-profiles/?accountId=" + ACCOUNT_ID)
                                                     .request()
                                                     .post(entity(delegateProfile, MediaType.APPLICATION_JSON),
                                                         new GenericType<RestResponse<DelegateProfile>>() {});

    assertThat(restResponse.getResource()).isNotNull();
    verify(delegateProfileService, atLeastOnce()).add(delegateProfile);
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void shouldUpdateDelegateProfile() {
    DelegateProfile delegateProfile = DelegateProfile.builder().uuid(ID_KEY).accountId(ACCOUNT_ID).build();

    when(delegateProfileService.update(any(DelegateProfile.class))).thenReturn(delegateProfile);
    RestResponse<DelegateProfile> restResponse =
        RESOURCES.client()
            .target("/delegate-profiles/" + ID_KEY + "?accountId=" + ACCOUNT_ID)
            .request()
            .put(entity(delegateProfile, MediaType.APPLICATION_JSON),
                new GenericType<RestResponse<DelegateProfile>>() {});

    ArgumentCaptor<DelegateProfile> captor = ArgumentCaptor.forClass(DelegateProfile.class);
    verify(delegateProfileService, atLeastOnce()).update(captor.capture());
    assertThat(restResponse.getResource()).isNotNull();
    DelegateProfile captorValue = captor.getValue();
    assertThat(captorValue.getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(captorValue.getUuid()).isEqualTo(ID_KEY);
    DelegateProfile resource = restResponse.getResource();
    assertThat(resource.getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(resource.getUuid()).isEqualTo(ID_KEY);
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void shouldUpdateDelegateProfileSelectors() {
    List<String> profileSelectorsAdded = Arrays.asList("xxx");
    List<String> profileSelectorsUpdated = Arrays.asList("yyy");

    DelegateProfile delegateProfile =
        DelegateProfile.builder().uuid(ID_KEY).accountId(ACCOUNT_ID).selectors(profileSelectorsAdded).build();

    when(delegateProfileService.updateDelegateProfileSelectors(ID_KEY, ACCOUNT_ID, profileSelectorsAdded))
        .thenReturn(delegateProfile);

    RestResponse<DelegateProfile> restResponse =
        RESOURCES.client()
            .target("/delegate-profiles/" + ID_KEY + "/selectors?accountId=" + ACCOUNT_ID + "&selectors=yyy")
            .request()
            .put(entity(delegateProfile, MediaType.APPLICATION_JSON),
                new GenericType<RestResponse<DelegateProfile>>() {});
    verify(delegateProfileService, atLeastOnce())
        .updateDelegateProfileSelectors(ID_KEY, ACCOUNT_ID, profileSelectorsUpdated);
  }
}