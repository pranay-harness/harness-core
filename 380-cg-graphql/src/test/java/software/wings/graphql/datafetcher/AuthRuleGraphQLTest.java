/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher;

import static io.harness.rule.OwnerRule.ROHIT_KUMAR;

import static software.wings.graphql.utils.GraphQLConstants.CREATE_APPLICATION_API;
import static software.wings.graphql.utils.GraphQLConstants.DELETE_APPLICATION_API;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;

import software.wings.security.AuthRuleFilter;
import software.wings.service.impl.security.auth.AuthHandler;
import software.wings.service.intfc.AuthService;

import graphql.language.Field;
import graphql.schema.DataFetchingEnvironment;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

public class AuthRuleGraphQLTest extends CategoryTest {
  @Mock AuthRuleFilter authRuleFilter;
  @Mock AuthHandler authHandler;
  @Mock AuthService authService;
  @Mock HPersistence persistence;
  @Spy
  @InjectMocks
  AuthRuleGraphQL authRuleGraphQL = new AuthRuleGraphQL(authRuleFilter, authHandler, authService, persistence);

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void test_handlePostMutation() {
    final DataFetchingEnvironment dataFetchingEnvironment = Mockito.mock(DataFetchingEnvironment.class);
    doReturn(new Field(CREATE_APPLICATION_API)).when(dataFetchingEnvironment).getField();
    doNothing().when(authService).evictUserPermissionAndRestrictionCacheForAccount("accountid", true, true);
    final MutationContext mutationContext =
        MutationContext.builder().accountId("accountid").dataFetchingEnvironment(dataFetchingEnvironment).build();
    authRuleGraphQL.handlePostMutation(mutationContext, new Object(), new Object());
    verify(authService, times(1)).evictUserPermissionAndRestrictionCacheForAccount("accountid", true, true);
    doReturn(new Field(DELETE_APPLICATION_API)).when(dataFetchingEnvironment).getField();
    authRuleGraphQL.handlePostMutation(mutationContext, new Object(), new Object());
    verify(authService, times(2)).evictUserPermissionAndRestrictionCacheForAccount("accountid", true, true);
  }
}
