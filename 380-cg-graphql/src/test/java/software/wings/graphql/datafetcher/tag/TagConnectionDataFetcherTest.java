/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.tag;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.rule.OwnerRule.HINGER;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.beans.Environment;
import software.wings.beans.Service;
import software.wings.beans.User;
import software.wings.dl.WingsPersistence;
import software.wings.graphql.datafetcher.AbstractDataFetcherTestBase;
import software.wings.graphql.datafetcher.DataFetcherUtils;
import software.wings.graphql.schema.query.QLPageQueryParameterImpl;
import software.wings.graphql.schema.type.QLTagConnection;
import software.wings.graphql.schema.type.QLTagEntity;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.QLIdOperator;
import software.wings.graphql.schema.type.aggregation.tag.QLTagEntityFilter;
import software.wings.security.UserThreadLocal;

import com.google.inject.Inject;
import graphql.execution.MergedSelectionSet;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingFieldSelectionSet;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.SelectedField;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

@OwnedBy(CDC)
public class TagConnectionDataFetcherTest extends AbstractDataFetcherTestBase {
  public static final String SERVICE = "SERVICE";
  @Mock private DataFetchingEnvironment dataFetchingEnvironment;
  @Inject private WingsPersistence wingsPersistence;

  @Inject @InjectMocks private TagUsageConnectionDataFetcher tagUsageConnectionDataFetcher;
  @Inject @InjectMocks private TagConnectionDataFetcher tagConnectionDataFetcher;
  @InjectMocks @Spy private DataFetcherUtils dataFetcherUtils;

  private Service service;
  private Environment environment;

  private static final SelectedField selectedField = new SelectedField() {
    @Override
    public String getName() {
      return "total";
    }
    @Override
    public String getQualifiedName() {
      return null;
    }
    @Override
    public GraphQLFieldDefinition getFieldDefinition() {
      return null;
    }
    @Override
    public Map<String, Object> getArguments() {
      return null;
    }
    @Override
    public DataFetchingFieldSelectionSet getSelectionSet() {
      return null;
    }
  };

  private static final DataFetchingFieldSelectionSet mockSelectionSet = new DataFetchingFieldSelectionSet() {
    public MergedSelectionSet get() {
      return MergedSelectionSet.newMergedSelectionSet().build();
    }
    public Map<String, Map<String, Object>> getArguments() {
      return Collections.emptyMap();
    }
    public Map<String, GraphQLFieldDefinition> getDefinitions() {
      return Collections.emptyMap();
    }
    public boolean contains(String fieldGlobPattern) {
      return false;
    }
    public SelectedField getField(String fieldName) {
      return null;
    }
    public List<SelectedField> getFields() {
      return Collections.singletonList(selectedField);
    }
    public List<SelectedField> getFields(String fieldGlobPattern) {
      return Collections.emptyList();
    }
  };

  @Before
  public void setup() throws SQLException {
    User user = testUtils.createUser(testUtils.createAccount());
    UserThreadLocal.set(user);
    createAccount(ACCOUNT1_ID, getLicenseInfo());
    createApp(ACCOUNT1_ID, APP1_ID_ACCOUNT1, APP1_ID_ACCOUNT1, TAG_TEAM, TAG_VALUE_TEAM1);

    // Create 2 tags : TAG_TEAM and TAG_KEY
    service =
        createService(ACCOUNT1_ID, APP1_ID_ACCOUNT1, SERVICE1_ID_APP1_ACCOUNT1, SERVICE, TAG_KEY, TAG_VALUE_TEAM1);
    environment = createEnv(
        ACCOUNT1_ID, APP1_ID_ACCOUNT1, ENV1_ID_APP1_ACCOUNT1, ENV1_ID_APP1_ACCOUNT1, TAG_TEAM, TAG_VALUE_TEAM2);
  }

  @Test
  @Owner(developers = HINGER)
  @Category(UnitTests.class)
  public void testFetchConnectionWithTagNameFilters() {
    QLIdFilter idFilter = QLIdFilter.builder().operator(QLIdOperator.IN).values(new String[] {TAG_TEAM}).build();
    QLTagEntityFilter qlTagEntityFilter = QLTagEntityFilter.builder().tagName(idFilter).build();
    List<QLTagEntityFilter> tagEntityFilters = Arrays.asList(qlTagEntityFilter);
    QLPageQueryParameterImpl pageQueryParams =
        QLPageQueryParameterImpl.builder().limit(100).selectionSet(mockSelectionSet).build();

    QLTagConnection tagConnection = tagConnectionDataFetcher.fetchConnection(tagEntityFilters, pageQueryParams, null);

    assertThat(tagConnection).isNotNull();
    assertThat(tagConnection.getNodes()).hasSize(1);
    assertThat(tagConnection.getNodes().stream().map(QLTagEntity::getName)).containsExactlyInAnyOrder(TAG_TEAM);
  }

  @Test
  @Owner(developers = HINGER)
  @Category(UnitTests.class)
  public void testFetchConnectionWithEmptyFilters() {
    List<QLTagEntityFilter> tagEntityFilters = Collections.emptyList();
    QLPageQueryParameterImpl pageQueryParams =
        QLPageQueryParameterImpl.builder().limit(100).selectionSet(mockSelectionSet).build();

    QLTagConnection tagConnection = tagConnectionDataFetcher.fetchConnection(tagEntityFilters, pageQueryParams, null);

    assertThat(tagConnection).isNotNull();
    assertThat(tagConnection.getNodes()).hasSize(2);
    assertThat(tagConnection.getNodes().stream().map(QLTagEntity::getName))
        .containsExactlyInAnyOrder(TAG_TEAM, TAG_KEY);
  }
}
