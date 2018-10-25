package software.wings.resources;

import static io.harness.beans.PageResponse.PageResponseBuilder.aPageResponse;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static java.lang.String.format;
import static javax.ws.rs.client.Entity.entity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;

import com.google.common.collect.Lists;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import org.junit.ClassRule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import software.wings.WingsBaseTest;
import software.wings.beans.RestResponse;
import software.wings.beans.security.access.Whitelist;
import software.wings.beans.security.access.WhitelistStatus;
import software.wings.exception.WingsExceptionMapper;
import software.wings.service.intfc.WhitelistService;
import software.wings.utils.JsonUtils;
import software.wings.utils.ResourceTestRule;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;

/**
 * @author rktummala on 04/11/18
 */
public class WhitelistResourceTest extends WingsBaseTest {
  private static final WhitelistService WHITELIST_SERVICE = mock(WhitelistService.class);

  @Captor private ArgumentCaptor<PageRequest<Whitelist>> pageRequestArgumentCaptor;

  /**
   * The constant RESOURCES.
   */
  @ClassRule
  public static final ResourceTestRule RESOURCES = ResourceTestRule.builder()
                                                       .addResource(new WhitelistResource(WHITELIST_SERVICE))
                                                       .addProvider(WingsExceptionMapper.class)
                                                       .build();

  private static String WHITELIST_ID = "WHITELIST_ID";
  private static final Whitelist WHITELIST = Whitelist.builder()
                                                 .accountId(ACCOUNT_ID)
                                                 .uuid(WHITELIST_ID)
                                                 .filter("127.0.0.1")
                                                 .status(WhitelistStatus.ACTIVE)
                                                 .build();

  /**
   * Should create whitelist config.
   */
  @Test
  public void shouldCreateWhitelist() {
    Whitelist whitelist2 = Whitelist.builder()
                               .accountId(ACCOUNT_ID)
                               .uuid(generateUuid())
                               .filter("10.01.10.01")
                               .status(WhitelistStatus.ACTIVE)
                               .build();
    when(WHITELIST_SERVICE.save(WHITELIST)).thenReturn(whitelist2);

    RestResponse<Whitelist> restResponse =
        RESOURCES.client()
            .target(format("/whitelist?accountId=%s", ACCOUNT_ID))
            .request()
            .post(entity(WHITELIST, MediaType.APPLICATION_JSON), new GenericType<RestResponse<Whitelist>>() {});

    assertThat(restResponse).isNotNull().hasFieldOrPropertyWithValue("resource", whitelist2);
    verify(WHITELIST_SERVICE).save(WHITELIST);
  }

  /**
   * Should update whitelist config.
   */
  @Test
  public void shouldUpdateWhitelist() {
    Whitelist whitelist2 = Whitelist.builder()
                               .accountId(ACCOUNT_ID)
                               .uuid(generateUuid())
                               .filter("10.01.10.01")
                               .status(WhitelistStatus.ACTIVE)
                               .build();
    when(WHITELIST_SERVICE.update(WHITELIST)).thenReturn(whitelist2);

    RestResponse<Whitelist> restResponse =
        RESOURCES.client()
            .target(format("/whitelist/%s?accountId=%s", WHITELIST_ID, ACCOUNT_ID))
            .request()
            .put(entity(WHITELIST, MediaType.APPLICATION_JSON), new GenericType<RestResponse<Whitelist>>() {});

    assertThat(restResponse).isNotNull().hasFieldOrPropertyWithValue("resource", whitelist2);
    verify(WHITELIST_SERVICE).update(WHITELIST);
  }

  /**
   * Should update whitelist config.
   */
  @Test
  public void shouldDeleteWhitelist() {
    when(WHITELIST_SERVICE.delete(ACCOUNT_ID, WHITELIST_ID)).thenReturn(true);

    RestResponse<Boolean> restResponse = RESOURCES.client()
                                             .target(format("/whitelist/%s?accountId=%s", WHITELIST_ID, ACCOUNT_ID))
                                             .request()
                                             .delete(new GenericType<RestResponse<Boolean>>() {});

    assertThat(restResponse).isNotNull().hasFieldOrPropertyWithValue("resource", Boolean.TRUE);
    verify(WHITELIST_SERVICE).delete(ACCOUNT_ID, WHITELIST_ID);
  }

  /**
   * Should list whitelist config.
   */
  @Test
  public void shouldList() {
    PageResponse<Whitelist> pageResponse = aPageResponse().withResponse(Lists.newArrayList(WHITELIST)).build();
    when(WHITELIST_SERVICE.list(any(String.class), any(PageRequest.class))).thenReturn(pageResponse);

    RestResponse<PageResponse<Whitelist>> restResponse =
        RESOURCES.client()
            .target(format("/whitelist?accountId=%s", ACCOUNT_ID))
            .request()
            .get(new GenericType<RestResponse<PageResponse<Whitelist>>>() {});

    log().info(JsonUtils.asJson(restResponse));
    verify(WHITELIST_SERVICE).list(anyString(), pageRequestArgumentCaptor.capture());
    assertThat(pageRequestArgumentCaptor.getValue()).isNotNull();
    assertThat(restResponse).isNotNull().hasFieldOrPropertyWithValue("resource", pageResponse);
  }

  /**
   * Should read whitelist config.
   */
  @Test
  public void shouldRead() {
    when(WHITELIST_SERVICE.get(ACCOUNT_ID, WHITELIST_ID)).thenReturn(WHITELIST);

    RestResponse<Whitelist> restResponse = RESOURCES.client()
                                               .target(format("/whitelist/%s?accountId=%s", WHITELIST_ID, ACCOUNT_ID))
                                               .request()
                                               .get(new GenericType<RestResponse<Whitelist>>() {});

    assertThat(restResponse).isNotNull().hasFieldOrPropertyWithValue("resource", WHITELIST);
    verify(WHITELIST_SERVICE).get(ACCOUNT_ID, WHITELIST_ID);
  }

  /**
   * Should read whitelist config.
   */
  @Test
  public void isIpWhitelisted() {
    when(WHITELIST_SERVICE.isValidIPAddress(ACCOUNT_ID, "127.0.0.1")).thenReturn(true);

    RestResponse<Boolean> restResponse =
        RESOURCES.client()
            .target(format("/whitelist/ip-address-whitelisted?accountId=%s&ipAddress=%s", ACCOUNT_ID, "127.0.0.1"))
            .request()
            .get(new GenericType<RestResponse<Boolean>>() {});

    assertThat(restResponse.getResource()).isEqualTo(Boolean.TRUE);
    verify(WHITELIST_SERVICE).isValidIPAddress(ACCOUNT_ID, "127.0.0.1");
  }
}
