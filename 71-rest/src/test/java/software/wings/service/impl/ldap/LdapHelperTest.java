package software.wings.service.impl.ldap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ldaptive.Connection;
import org.ldaptive.DefaultConnectionFactory;
import org.ldaptive.LdapEntry;
import org.ldaptive.LdapException;
import org.ldaptive.ResultCode;
import org.ldaptive.SearchResult;
import org.ldaptive.auth.AuthenticationResponse;
import org.ldaptive.auth.Authenticator;
import org.ldaptive.auth.SearchDnResolver;
import org.mockito.Matchers;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import software.wings.WingsBaseTest;
import software.wings.beans.sso.LdapConnectionSettings;
import software.wings.beans.sso.LdapGroupSettings;
import software.wings.beans.sso.LdapSettings;
import software.wings.beans.sso.LdapUserSettings;
import software.wings.helpers.ext.ldap.LdapResponse;
import software.wings.helpers.ext.ldap.LdapResponse.Status;
import software.wings.helpers.ext.ldap.LdapSearch;

import java.util.Collections;

/**
 * @author Swapnil
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({LdapSearch.class, LdapHelper.class})
public class LdapHelperTest extends WingsBaseTest {
  private LdapSettings ldapSettings;
  private LdapHelper helper;
  private SearchResult searchResult;
  private LdapSearch.Builder searchBuilder;
  private LdapSearch search;

  private void mockLdapSearchBuilder(LdapSearch.Builder searchBuilder, LdapSearch search) {
    PowerMockito.mockStatic(LdapSearch.class);
    when(LdapSearch.builder()).thenReturn(searchBuilder);
    when(searchBuilder.connectionFactory(any())).thenReturn(searchBuilder);
    when(searchBuilder.baseDN(any())).thenReturn(searchBuilder);
    when(searchBuilder.searchFilter(any())).thenReturn(searchBuilder);
    when(searchBuilder.limit(anyInt())).thenReturn(searchBuilder);
    when(searchBuilder.referralsEnabled(anyBoolean())).thenReturn(searchBuilder);
    when(searchBuilder.maxReferralHops(anyInt())).thenReturn(searchBuilder);
    when(searchBuilder.build()).thenReturn(search);
  }

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    LdapConnectionSettings connectionSettings = new LdapConnectionSettings();
    connectionSettings.setBindDN("testBindDN");
    connectionSettings.setBindPassword("testBindPassword");
    LdapUserSettings userSettings = new LdapUserSettings();
    userSettings.setBaseDN("testBaseDN");
    LdapGroupSettings groupSettings = new LdapGroupSettings();
    groupSettings.setBaseDN("testBaseDN");
    ldapSettings = new LdapSettings("testSettings", "testAccount", connectionSettings, userSettings, groupSettings);

    DefaultConnectionFactory cf = mock(DefaultConnectionFactory.class);
    Connection c = mock(Connection.class);
    when(cf.getConnection()).thenReturn(c);
    whenNew(DefaultConnectionFactory.class).withAnyArguments().thenReturn(cf);
    helper = new LdapHelper(ldapSettings.getConnectionSettings());

    searchResult = mock(SearchResult.class);
    searchBuilder = mock(LdapSearch.Builder.class);
    search = mock(LdapSearch.class);
  }

  @Test
  public void validateConnectionConfig() {
    assertThat(helper.validateConnectionConfig().getStatus()).isEqualTo(Status.SUCCESS);
  }

  @Test
  public void validateUserConfig() throws LdapException {
    mockLdapSearchBuilder(searchBuilder, search);
    when(search.execute()).thenReturn(searchResult);
    when(searchResult.size()).thenReturn(0);
    assertThat(helper.validateUserConfig(ldapSettings.getUserSettings()).getStatus()).isEqualTo(Status.FAILURE);

    when(searchResult.size()).thenReturn(1);
    assertThat(helper.validateUserConfig(ldapSettings.getUserSettings()).getStatus()).isEqualTo(Status.SUCCESS);

    LdapException ldapException = mock(LdapException.class);
    when(ldapException.getResultCode()).thenReturn(ResultCode.OPERATIONS_ERROR);
    when(search.execute()).thenThrow(ldapException);
    assertThat(helper.validateUserConfig(ldapSettings.getUserSettings()).getStatus()).isEqualTo(Status.FAILURE);
  }

  @Test
  public void validateGroupConfig() throws LdapException {
    mockLdapSearchBuilder(searchBuilder, search);
    when(search.execute(any())).thenReturn(searchResult);
    when(search.execute()).thenReturn(searchResult);
    when(searchResult.size()).thenReturn(0);
    assertThat(helper.validateGroupConfig(ldapSettings.getGroupSettings()).getStatus()).isEqualTo(Status.FAILURE);

    when(searchResult.size()).thenReturn(1);
    assertThat(helper.validateGroupConfig(ldapSettings.getGroupSettings()).getStatus()).isEqualTo(Status.SUCCESS);

    LdapException ldapException = mock(LdapException.class);
    when(ldapException.getResultCode()).thenReturn(ResultCode.OPERATIONS_ERROR);
    when(search.execute(Matchers.anyVararg())).thenThrow(ldapException);
    assertThat(helper.validateGroupConfig(ldapSettings.getGroupSettings()).getStatus()).isEqualTo(Status.FAILURE);
  }

  @Test
  public void populateGroupSize() throws LdapException {
    LdapEntry group = new LdapEntry("groupDN");
    SearchResult groups = new SearchResult(group);
    mockLdapSearchBuilder(searchBuilder, search);
    when(search.execute(Matchers.anyVararg())).thenReturn(searchResult);
    when(searchResult.getEntries()).thenReturn(Collections.singletonList(group));
    when(searchResult.size()).thenReturn(1);
    helper.populateGroupSize(groups, ldapSettings.getUserSettings());
    assertThat(group.getAttribute("groupSize").getStringValue()).isEqualTo("1");
  }

  @Test
  public void getGroupByDn() throws LdapException {
    mockLdapSearchBuilder(searchBuilder, search);
    when(search.execute(any())).thenReturn(searchResult);
    when(search.execute()).thenReturn(searchResult);
    when(searchResult.size()).thenReturn(1);
    String oldBaseDN = ldapSettings.getGroupSettings().getBaseDN();
    helper.getGroupByDn(ldapSettings.getGroupSettings(), "groupDN");
    assertThat(ldapSettings.getGroupSettings().getBaseDN()).isEqualTo(oldBaseDN);
  }

  @Test
  public void authenticate() throws Exception {
    mockLdapSearchBuilder(searchBuilder, search);
    when(search.execute(any())).thenReturn(searchResult);
    when(search.execute()).thenReturn(searchResult);
    when(searchResult.size()).thenReturn(1);
    SearchDnResolver searchDnResolver = mock(SearchDnResolver.class);
    when(search.getSearchDnResolver(anyString())).thenReturn(searchDnResolver);
    when(searchDnResolver.resolve(any())).thenReturn("");

    LdapResponse response = helper.authenticate(ldapSettings.getUserSettings(), "myemail@mycompany.com", "mypassword");
    assertThat(response.getStatus()).isEqualTo(Status.FAILURE);

    Authenticator authenticator = mock(Authenticator.class);
    whenNew(Authenticator.class).withAnyArguments().thenReturn(authenticator);
    when(searchDnResolver.resolve(any())).thenReturn("userDN");
    AuthenticationResponse authenticationResponse = mock(AuthenticationResponse.class);
    when(authenticationResponse.getResult()).thenReturn(true);
    when(authenticator.authenticate(any())).thenReturn(authenticationResponse);
    response = helper.authenticate(ldapSettings.getUserSettings(), "myemail@mycompany.com", "mypassword");
    assertThat(response.getStatus()).isEqualTo(Status.SUCCESS);

    when(authenticationResponse.getResult()).thenReturn(false);
    when(authenticationResponse.getResultCode()).thenReturn(ResultCode.INVALID_CREDENTIALS);
    response = helper.authenticate(ldapSettings.getUserSettings(), "myemail@mycompany.com", "mypassword");
    assertThat(response.getStatus()).isEqualTo(Status.FAILURE);

    LdapException ldapException = mock(LdapException.class);
    when(authenticator.authenticate(any())).thenThrow(ldapException);
    when(ldapException.getResultCode()).thenReturn(ResultCode.OPERATIONS_ERROR);
    response = helper.authenticate(ldapSettings.getUserSettings(), "myemail@mycompany.com", "mypassword");
    assertThat(response.getStatus()).isEqualTo(Status.FAILURE);
  }
}