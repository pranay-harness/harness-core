/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ccm.setup.graphql;

import static io.harness.annotations.dev.HarnessTeam.CE;
import static io.harness.rule.OwnerRule.HITESH;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.beans.AwsCrossAccountAttributes;
import software.wings.beans.AwsS3BucketDetails;
import software.wings.beans.User;
import software.wings.beans.ce.CEAwsConfig;
import software.wings.graphql.datafetcher.AbstractDataFetcherTestBase;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.QLIdOperator;
import software.wings.security.UserThreadLocal;
import software.wings.settings.SettingValue;

import com.google.inject.Inject;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@TargetModule(HarnessModule._375_CE_GRAPHQL)
@OwnedBy(CE)
public class CeConnectorDataFetcherTest extends AbstractDataFetcherTestBase {
  @Inject CeConnectorDataFetcher ceConnectorDataFetcher;

  private static final String CUR_REPORT_NAME = "curReportName";
  private static final String BUCKET_NAME = "bucketName";
  private static final String CONNECTOR_NAME = "connectorName";
  private static final String UUID = "NBeViCRzTpOpVVf22M4EZA";
  private static final String ROLE_ARN = "arn:aws:iam::830767422336:role/harnessCERole";
  private static final String EXTERNAL_ID = "harness:108817434118:zEaa-FLS425IEO7OLzMUg";

  @Before
  public void setup() throws SQLException {
    User user = testUtils.createUser(testUtils.createAccount());
    UserThreadLocal.set(user);

    createAccount(ACCOUNT1_ID, getLicenseInfo());
    AwsCrossAccountAttributes awsCrossAccountAttributes =
        AwsCrossAccountAttributes.builder().crossAccountRoleArn(ROLE_ARN).externalId(EXTERNAL_ID).build();
    AwsS3BucketDetails awsS3BucketDetails = AwsS3BucketDetails.builder().s3BucketName(BUCKET_NAME).build();
    SettingValue settingValue = CEAwsConfig.builder()
                                    .curReportName(CUR_REPORT_NAME)
                                    .awsCrossAccountAttributes(awsCrossAccountAttributes)
                                    .s3BucketDetails(awsS3BucketDetails)
                                    .build();
    createCEConnector(UUID, ACCOUNT1_ID, CONNECTOR_NAME, settingValue);
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testFetchConnection() {
    List<QLCESetupFilter> filters = Arrays.asList(getSettingIdFilter());
    QLCEConnectorData ceConnectorData = ceConnectorDataFetcher.fetchConnection(filters, null, null);
    QLCEConnector ceConnector = ceConnectorData.getCeConnectors().get(0);
    assertThat(ceConnectorData.getCeConnectors()).hasSize(1);
    assertThat(ceConnector.getS3BucketName()).isEqualTo(BUCKET_NAME);
    assertThat(ceConnector.getAccountName()).isEqualTo(CONNECTOR_NAME);
    assertThat(ceConnector.getCurReportName()).isEqualTo(CUR_REPORT_NAME);
    assertThat(ceConnector.getCrossAccountRoleArn()).isEqualTo(ROLE_ARN);
    assertThat(ceConnector.getSettingId()).isEqualTo(UUID);
  }

  private QLCESetupFilter getSettingIdFilter() {
    return QLCESetupFilter.builder()
        .settingId(QLIdFilter.builder().operator(QLIdOperator.EQUALS).values(new String[] {UUID}).build())
        .build();
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testGenerateFilter() {
    QLCESetupFilter filter = ceConnectorDataFetcher.generateFilter(null, null, null);
    assertThat(filter).isNull();
  }
}
