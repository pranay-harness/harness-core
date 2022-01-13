/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.cluster;

import static io.harness.annotations.dev.HarnessTeam.CE;
import static io.harness.ccm.cluster.entities.ClusterType.AWS_ECS;
import static io.harness.rule.OwnerRule.SHUBHANSHU;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.category.element.UnitTests;
import io.harness.ccm.cluster.ClusterRecordService;
import io.harness.rule.Owner;

import software.wings.beans.User;
import software.wings.graphql.datafetcher.AbstractDataFetcherTestBase;
import software.wings.graphql.schema.query.QLClustersQueryParameters;
import software.wings.graphql.schema.type.QLClusterConnection;
import software.wings.security.UserThreadLocal;

import com.google.inject.Inject;
import graphql.execution.MergedSelectionSet;
import graphql.schema.DataFetchingFieldSelectionSet;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.SelectedField;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

@TargetModule(HarnessModule._375_CE_GRAPHQL)
@OwnedBy(CE)
public class ClusterConnectionDataFetcherTest extends AbstractDataFetcherTestBase {
  @Mock ClusterRecordService clusterRecordService;
  @Inject ClusterConnectionDataFetcher clusterConnectionDataFetcher;

  private static final DataFetchingFieldSelectionSet testSelectionSet = new DataFetchingFieldSelectionSet() {
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
      return Collections.emptyList();
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
    createClusterRecord(ACCOUNT1_ID, CLUSTER1_NAME, CLUSTER2_ID, CLOUD_PROVIDER1_ID_ACCOUNT1, REGION1);
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testClusterConnectionDataFetcherEcs() {
    QLClusterConnection qlClusterConnection = clusterConnectionDataFetcher.fetchConnection(null,
        QLClustersQueryParameters.builder()
            .limit(1)
            .offset(0)
            .accountId(ACCOUNT1_ID)
            .selectionSet(testSelectionSet)
            .build(),
        null);

    assertThat(qlClusterConnection.getNodes().get(0).getName()).isEqualTo(CLUSTER1_NAME);
    assertThat(qlClusterConnection.getNodes().get(0).getCloudProviderId()).isEqualTo(CLOUD_PROVIDER1_ID_ACCOUNT1);
    assertThat(qlClusterConnection.getNodes().get(0).getClusterType()).isEqualTo(AWS_ECS);
  }
}
