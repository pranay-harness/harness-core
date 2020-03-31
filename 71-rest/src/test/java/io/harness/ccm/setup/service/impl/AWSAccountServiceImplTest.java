package io.harness.ccm.setup.service.impl;

import static io.harness.rule.OwnerRule.HITESH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

import com.google.common.collect.ImmutableList;

import com.amazonaws.services.organizations.model.Account;
import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ccm.setup.service.support.intfc.AWSOrganizationHelperService;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import software.wings.beans.AwsCrossAccountAttributes;
import software.wings.beans.ce.CEAwsConfig;
import software.wings.beans.ce.CECloudAccount;

import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class AWSAccountServiceImplTest extends CategoryTest {
  private AWSAccountServiceImpl awsAccountService;
  @Mock private AWSOrganizationHelperService awsOrganizationHelperService;

  private final String ACCOUNT_ID = "accountId";
  private final String SETTING_ID = "settingId";
  private final String AWS_ACCOUNT_ID = "424324243";
  private final String AWS_MASTER_ACCOUNT_ID = "awsMasterAccountId";

  @Before
  public void setUp() throws Exception {
    awsAccountService = new AWSAccountServiceImpl(awsOrganizationHelperService);
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testListAWSAccounts() {
    CEAwsConfig ceAwsConfig =
        CEAwsConfig.builder()
            .awsAccountId(AWS_ACCOUNT_ID)
            .awsMasterAccountId(AWS_MASTER_ACCOUNT_ID)
            .awsCrossAccountAttributes(AwsCrossAccountAttributes.builder()
                                           .crossAccountRoleArn("arn:aws:iam::454324243:role/harness_master_account")
                                           .externalId("externalId")
                                           .build())
            .build();
    doReturn(ImmutableList.of(new Account()
                                  .withName("account_name_1")
                                  .withArn("arn:aws:organizations::454324243:account/o-tbm3caqef8/424324243"),
                 new Account()
                     .withName("account_name_2")
                     .withArn("arn:aws:organizations::454324243:account/o-tbm3caqef8/454324243")))
        .when(awsOrganizationHelperService)
        .listAwsAccounts(ceAwsConfig.getAwsCrossAccountAttributes());

    List<CECloudAccount> awsAccounts = awsAccountService.getAWSAccounts(ACCOUNT_ID, SETTING_ID, ceAwsConfig);
    assertThat(awsAccounts).hasSize(1);
    assertThat(awsAccounts.get(0))
        .isEqualTo(CECloudAccount.builder()
                       .accountId(ACCOUNT_ID)
                       .accountArn("arn:aws:organizations::454324243:account/o-tbm3caqef8/424324243")
                       .accountName("account_name_1")
                       .infraAccountId("424324243")
                       .infraMasterAccountId(AWS_MASTER_ACCOUNT_ID)
                       .masterAccountSettingId(SETTING_ID)
                       .build());
  }
}
