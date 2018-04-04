package software.wings.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.Account.Builder.anAccount;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.USER_GROUP_ID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.wings.beans.Account;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.security.UserGroup;
import software.wings.dl.PageRequest;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.AuthService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.UserService;

public class UserGroupServiceImplTest {
  @Mock private WingsPersistence wingsPersistence;
  @Mock private UserService userService;
  @Mock private AccountService accountService;
  @Mock private AppService appService;
  @Mock private ServiceResourceService serviceResourceService;
  @Mock private AuthService authService;
  @InjectMocks private UserGroupServiceImpl userGroupService = spy(UserGroupServiceImpl.class);

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testCloneUserGroup() {
    final UserGroup storedGroupToClone = UserGroup.builder()
                                             .uuid(USER_GROUP_ID)
                                             .appId(APP_ID)
                                             .createdBy(null)
                                             .createdAt(0)
                                             .lastUpdatedBy(null)
                                             .lastUpdatedAt(0)
                                             .keywords(null)
                                             .entityYamlPath(null)
                                             .name("Stored")
                                             .description("Desc")
                                             .accountId(ACCOUNT_ID)
                                             .memberIds(null)
                                             .members(null)
                                             .appPermissions(null)
                                             .accountPermissions(null)
                                             .build();
    final PageRequest<UserGroup> req = aPageRequest()
                                           .addFilter("accountId", Operator.EQ, ACCOUNT_ID)
                                           .addFilter(ID_KEY, Operator.EQ, USER_GROUP_ID)
                                           .build();
    doReturn(storedGroupToClone).when(wingsPersistence).get(UserGroup.class, req);
    final Account account = anAccount().withUuid(ACCOUNT_ID).build();
    doReturn(account).when(accountService).get(ACCOUNT_ID);
    final String newName = "NewName";
    final UserGroup cloneExpected = UserGroup.builder()
                                        .uuid(USER_GROUP_ID)
                                        .appId(APP_ID)
                                        .createdBy(null)
                                        .createdAt(0)
                                        .lastUpdatedBy(null)
                                        .lastUpdatedAt(0)
                                        .keywords(null)
                                        .entityYamlPath(null)
                                        .name(newName)
                                        .description("Desc")
                                        .accountId(ACCOUNT_ID)
                                        .memberIds(null)
                                        .members(null)
                                        .appPermissions(null)
                                        .accountPermissions(null)
                                        .build();
    doNothing().when(authService).evictAccountUserPermissionInfoCache(anyString(), any());
    doReturn(cloneExpected).when(wingsPersistence).saveAndGet(eq(UserGroup.class), any());
    UserGroup cloneActual = userGroupService.cloneUserGroup(ACCOUNT_ID, USER_GROUP_ID, newName);
    assertThat(cloneActual.getName()).isEqualTo(newName);
    verify(wingsPersistence).saveAndGet(UserGroup.class, cloneExpected);
  }
}
