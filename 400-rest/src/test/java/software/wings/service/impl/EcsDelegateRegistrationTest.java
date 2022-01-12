/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl;

import static io.harness.rule.OwnerRule.ADWAIT;

import static software.wings.beans.DelegateSequenceConfig.Builder.aDelegateSequenceBuilder;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.AdditionalAnswers.returnsSecondArg;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.Delegate;
import io.harness.ff.FeatureFlagService;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.app.MainConfiguration;
import software.wings.beans.DelegateSequenceConfig;
import software.wings.jre.JreConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

@Slf4j
@OwnedBy(HarnessTeam.DEL)
public class EcsDelegateRegistrationTest extends WingsBaseTest {
  DelegateServiceImpl delegateService;
  @Mock HPersistence persistence;
  @Mock Query query;
  @Mock UpdateOperations updateOperations;
  @Mock FeatureFlagService featureFlagService;
  @Mock MainConfiguration mainConfiguration;

  /**
   * Test keepAlivePath is taken when delegate.KeepAlivePacket = true
   */
  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testHandleEcsDelegateRequest_KeepAliveRequest() throws Exception {
    delegateService = spy(DelegateServiceImpl.class);
    delegateService.handleEcsDelegateRequest(Delegate.builder().keepAlivePacket(true).build());

    verify(delegateService).handleEcsDelegateKeepAlivePacket(any());
    verify(delegateService, times(0)).handleEcsDelegateRegistration(any());
  }

  /**
   * Test EcsDelegateRegistration path is taken, when delegate.KeepAlivePacket = false
   */
  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testHandleEcsDelegateRequest_EcsDelegateRegistration() throws Exception {
    delegateService = spy(DelegateServiceImpl.class);
    doReturn(Delegate.builder().hostName("delegate_1").build())
        .when(delegateService)
        .handleEcsDelegateRegistration(any());
    doReturn(aDelegateSequenceBuilder().withHostName("delegate").withSequenceNum(1).withDelegateToken("token").build())
        .when(delegateService)
        .getDelegateSequenceConfig(anyString(), anyString(), anyInt());
    on(delegateService).set("featureFlagService", featureFlagService);
    on(delegateService).set("mainConfiguration", mainConfiguration);
    doReturn(false).when(featureFlagService).isEnabled(any(), any());
    JreConfig oracleJreConfig = JreConfig.builder().version("1.8.0_191").build();
    HashMap<String, JreConfig> jreConfigMap = new HashMap<>();
    jreConfigMap.put("oracle8u191", oracleJreConfig);
    doReturn("oracle8u191").when(mainConfiguration).getCurrentJre();
    doReturn(jreConfigMap).when(mainConfiguration).getJreConfigs();

    delegateService.handleEcsDelegateRequest(Delegate.builder().keepAlivePacket(false).build());

    verify(delegateService, times(0)).handleEcsDelegateKeepAlivePacket(any());
    verify(delegateService).handleEcsDelegateRegistration(any());
  }

  /**
   * Test HandleEcsDelegateKeepAlivePacket flow
   */
  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testHandleEcsDelegateKeepAlivePacket() throws Exception {
    delegateService = spy(DelegateServiceImpl.class);
    doReturn(null)
        .doReturn(Delegate.builder().build())
        .doReturn(Delegate.builder().build())
        .when(delegateService)
        .getDelegateUsingSequenceNum(anyString(), anyString(), anyString());

    doReturn(aDelegateSequenceBuilder().withDelegateToken("aabbcc").build())
        .doReturn(aDelegateSequenceBuilder().withDelegateToken("aabbcc").build())
        .when(delegateService)
        .getDelegateSequenceConfig(anyString(), anyString(), anyInt());

    Delegate delegate = Delegate.builder().build();
    delegateService.handleEcsDelegateKeepAlivePacket(delegate);
    verify(delegateService, times(0)).getDelegateUsingSequenceNum(anyString(), anyString(), anyString());

    delegate.setHostName("host");
    delegateService.handleEcsDelegateKeepAlivePacket(delegate);
    verify(delegateService, times(0)).getDelegateUsingSequenceNum(anyString(), anyString(), anyString());

    delegate.setDelegateRandomToken("token");
    delegateService.handleEcsDelegateKeepAlivePacket(delegate);
    verify(delegateService, times(0)).getDelegateUsingSequenceNum(anyString(), anyString(), anyString());

    delegate.setUuid("id");
    delegateService.handleEcsDelegateKeepAlivePacket(delegate);
    verify(delegateService, times(0)).getDelegateUsingSequenceNum(anyString(), anyString(), anyString());

    delegate.setSequenceNum("1");
    delegateService.handleEcsDelegateKeepAlivePacket(delegate);
    verify(delegateService, times(1)).getDelegateUsingSequenceNum(anyString(), anyString(), anyString());

    delegateService.handleEcsDelegateKeepAlivePacket(delegate);
    verify(persistence, times(0)).createQuery(any());
    verify(persistence, times(0)).update(any(Query.class), any(UpdateOperations.class));

    mockWingsPersistanceForUpdateCall();

    delegate.setDelegateRandomToken("aabbcc");
    delegateService.handleEcsDelegateKeepAlivePacket(delegate);
    verify(persistence, times(1)).createQuery(any());
    verify(persistence, times(1)).update(any(Query.class), any(UpdateOperations.class));
  }

  /**
   * Delegate heartbeat with valid delegateId and token.
   * Expected : - get delegate from db with Id.
   *            - get DelegateSequenceConfig for that delegate
   *            - match seqNum and token sent by delegate with this config
   *            - just update this same existing delegate
   */
  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testHandleEcsDelegateRegistration_activeDelegateWithId() throws Exception {
    delegateService = spy(DelegateServiceImpl.class);
    mockWingsPersistanceForUpdateCall();

    DelegateSequenceConfig config = aDelegateSequenceBuilder().withDelegateToken("token").build();
    doReturn(config).when(delegateService).getDelegateSequenceConfig(anyString(), anyString(), anyInt());

    Delegate delegate =
        Delegate.builder().uuid("12345").delegateType("ECS").delegateRandomToken("token").sequenceNum("1").build();

    doReturn(delegate).when(query).get();

    doAnswer(returnsSecondArg())
        .when(delegateService)
        .upsertDelegateOperation(any(Delegate.class), any(Delegate.class));
    delegate = delegateService.handleEcsDelegateRegistration(delegate);
    assertThat(delegate).isNotNull();
    assertThat(delegate.getUuid()).isEqualTo("12345");

    verify(delegateService, times(1)).handleECSRegistrationUsingID(any(Delegate.class));
    verify(delegateService, times(0)).handleECSRegistrationUsingSeqNumAndToken(any(Delegate.class));
    verify(delegateService, times(0)).registerDelegateWithNewSequenceGeneration(any(Delegate.class));
  }

  /**
   * Delegate heartbeat with delegateId = null and delegateToken = null.
   * Expected - Should throw exception
   */
  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testHandleEcsDelegateRegistration_empty_UUid_token() throws Exception {
    delegateService = spy(DelegateServiceImpl.class);

    DelegateSequenceConfig config = aDelegateSequenceBuilder().withDelegateToken("token").build();
    doReturn(null).when(delegateService).getDelegateSequenceConfig(anyString(), anyString(), anyInt());

    Delegate delegate = Delegate.builder().delegateType("ECS").build();
    try {
      delegateService.handleEcsDelegateRegistration(delegate);
      assertThat(false).isTrue();
    } catch (Exception e) {
      assertThat(e.getMessage()).isEqualTo("Received invalid token from ECS delegate");
    }

    try {
      delegate.setUuid("12345");
      delegate.setSequenceNum("1");
      delegateService.handleEcsDelegateRegistration(delegate);
      assertThat(false).isTrue();
    } catch (Exception e) {
      assertThat(e.getMessage()).isEqualTo("Received invalid token from ECS delegate");
    }
  }

  /**
   * Delegate heartbeat with seqNum and delegateToken
   * Scenario:
   * - Delegate sent {seqNum = 1, delegateToken = "xyz", hostName = "hostname"} for ACCID=1
   * - DelegateSequenceConfig exists {seqNum = 1, delegateToken = "xyz", hostName = "hostname", ACCID=1}
   * - Delegate exists {ACCID=1, hostname = hostname_1}
   * This existing delegate should be updated
   */
  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testHandleEcsDelegateRegistration_with_valid_seqNum_token() throws Exception {
    delegateService = spy(DelegateServiceImpl.class);

    DelegateSequenceConfig config = aDelegateSequenceBuilder()
                                        .withDelegateToken("token")
                                        .withSequenceNum(Integer.valueOf(1))
                                        .withAccountId(ACCOUNT_ID)
                                        .withHostName("hostName")
                                        .build();

    doReturn(config).when(delegateService).getDelegateSequenceConfig(anyString(), anyString(), anyInt());

    Delegate delegate = Delegate.builder().delegateType("ECS").delegateRandomToken("token").sequenceNum("1").build();

    Delegate existingDelegate =
        Delegate.builder().delegateType("ECS").uuid("12345").delegateRandomToken("token").sequenceNum("1").build();

    doReturn(existingDelegate)
        .doReturn(delegate)
        .when(delegateService)
        .getDelegateUsingSequenceNum(anyString(), anyString(), anyString());

    // firstArg is existingDelegate fetched from db
    // secondArg is delegate arg passed.
    doAnswer(returnsFirstArg())
        .doAnswer(returnsSecondArg())
        .when(delegateService)
        .upsertDelegateOperation(any(Delegate.class), any(Delegate.class));

    delegate = delegateService.handleEcsDelegateRegistration(delegate);
    assertThat(delegate).isNotNull();
    assertThat(delegate.getUuid()).isEqualTo("12345");

    verify(delegateService, times(1)).handleECSRegistrationUsingSeqNumAndToken(any(Delegate.class));
    verify(delegateService, times(0)).handleECSRegistrationUsingID(any(Delegate.class));
    verify(delegateService, times(0)).registerDelegateWithNewSequenceGeneration(any(Delegate.class));
  }

  /**
   * Delegate heartbeat with seqNum and delegateToken
   * Scenario:
   * - Delegate sent {seqNum = 1, delegateToken = "xyz", hostName = "hostname"} for ACCID=1
   * - DelegateSequenceConfig exists {seqNum = 1, delegateToken = "xyz", hostName = "hostname", ACCID=1}
   * - No Delegate exists with {ACCID=1, hostname = hostname_1}
   * So new delegate record with hostname hostname_1 will be created.
   */
  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testHandleEcsDelegateRegistration_with_valid_seqNum_token_2() throws Exception {
    delegateService = spy(DelegateServiceImpl.class);

    DelegateSequenceConfig config = aDelegateSequenceBuilder()
                                        .withDelegateToken("token")
                                        .withSequenceNum(Integer.valueOf(1))
                                        .withAccountId(ACCOUNT_ID)
                                        .withHostName("hostName")
                                        .build();

    doReturn(config).when(delegateService).getDelegateSequenceConfig(anyString(), anyString(), anyInt());

    Delegate delegate = Delegate.builder().delegateType("ECS").delegateRandomToken("token").sequenceNum("1").build();

    doReturn(null).when(delegateService).getDelegateUsingSequenceNum(anyString(), anyString(), anyString());

    doNothing().when(delegateService).initDelegateWithConfigFromExistingDelegate(any(Delegate.class));

    // firstArg is existingDelegate fetched from db
    // secondArg is delegate arg passed.
    doAnswer(returnsSecondArg())
        .when(delegateService)
        .upsertDelegateOperation(any(Delegate.class), any(Delegate.class));

    delegate = delegateService.handleEcsDelegateRegistration(delegate);
    assertThat(delegate).isNotNull();
    assertThat(delegate.getUuid()).isEqualTo(null);
    assertThat(delegate.getHostName()).isEqualTo("hostName_1");

    // existing delegate should be null for upsertDelegateOperation(null, newDelegate)
    ArgumentCaptor<Delegate> captor = ArgumentCaptor.forClass(Delegate.class);
    verify(delegateService).upsertDelegateOperation(captor.capture(), any(Delegate.class));
    assertThat(captor.getValue()).isNull();

    verify(delegateService, times(1)).handleECSRegistrationUsingSeqNumAndToken(any(Delegate.class));
    verify(delegateService, times(0)).handleECSRegistrationUsingID(any(Delegate.class));
    verify(delegateService, times(0)).registerDelegateWithNewSequenceGeneration(any(Delegate.class));
  }

  /**
   * Delegate heartbeat with seqNum and delegateToken and null UUID
   * Scenario:
   * - Delegate sent null UUID anf {seqNum = 1, delegateToken = "xyz", hostName = "hostname"} for ACCID=1
   * - It will take path "handleECSRegistrationUsingSeqNumAndToken", but it returns null
   *   (not able to register delegate, may be due to some exception)
   * - Test it takes path registerDelegateWithNewSequenceGeneration() in this case
   */
  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testHandleEcsDelegateRegistration_with_valid_seqNum_token_3() throws Exception {
    delegateService = spy(DelegateServiceImpl.class);

    DelegateSequenceConfig config = aDelegateSequenceBuilder()
                                        .withDelegateToken("token")
                                        .withSequenceNum(Integer.valueOf(1))
                                        .withAccountId(ACCOUNT_ID)
                                        .withHostName("hostName")
                                        .build();

    doReturn(config).when(delegateService).getDelegateSequenceConfig(anyString(), anyString(), anyInt());
    doReturn(false).when(delegateService).checkForValidTokenIfPresent(any(Delegate.class));
    Delegate delegate = Delegate.builder().delegateType("ECS").delegateRandomToken("token").sequenceNum("1").build();

    doReturn(null).when(delegateService).handleECSRegistrationUsingSeqNumAndToken(any(Delegate.class));
    doReturn(delegate).when(delegateService).registerDelegateWithNewSequenceGeneration(any(Delegate.class));

    delegateService.handleEcsDelegateRegistration(delegate);
    verify(delegateService, times(1)).handleECSRegistrationUsingSeqNumAndToken(any(Delegate.class));
    verify(delegateService, times(0)).handleECSRegistrationUsingID(any(Delegate.class));
    verify(delegateService, times(1)).registerDelegateWithNewSequenceGeneration(any(Delegate.class));
  }

  /**
   * Expected
   *  - One of the DelegateSequenceConfigs in DB is stale (not refreshed in last 100 secs)
   *  - Get that 1 and associate with new delegat being registered.
   *  - Delete any existing delegate associated with that config
   */
  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testGetInactiveDelegateSequenceConfigToReplace() throws Exception {
    delegateService = spy(DelegateServiceImpl.class);

    List<DelegateSequenceConfig> existingDelegateSequenceConfigs = getExistingDelegateSequenceConfigs();

    Delegate delegate = Delegate.builder().delegateType("ECS").hostName("hostname").build();

    doReturn(Delegate.builder().uuid("12345").tags(Arrays.asList("tag1", "tag2")).build())
        .doReturn(null)
        .when(delegateService)
        .getDelegateUsingSequenceNum(anyString(), anyString(), anyString());

    mockWingsPersistanceForUpdateCall();
    doNothing().when(delegateService).delete(anyString(), anyString());

    DelegateSequenceConfig config =
        delegateService.getInactiveDelegateSequenceConfigToReplace(delegate, existingDelegateSequenceConfigs);
    assertThat(config).isNotNull();
    assertThat(config.getSequenceNum().intValue()).isEqualTo(1);

    assertThat(delegate.getTags()).isNotNull();
    assertThat(delegate.getTags()).hasSize(2);
    assertThat(delegate.getTags().contains("tag1")).isTrue();
    assertThat(delegate.getTags().contains("tag2")).isTrue();
    assertThat(delegate.getHostName()).isEqualTo("hostname_1");

    // existing delegate assocaited to stale sequenceConfig is deleted
    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(delegateService).delete(anyString(), captor.capture());
    assertThat(captor.getValue()).isEqualTo("12345");
  }

  /**
   * SeqNum and token sent by delegate matches DelegateSequenceConfig
   * - Test for NPEs
   * @throws Exception
   */
  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testSeqNumAndTokenMatchesConfig() throws Exception {
    delegateService = spy(DelegateServiceImpl.class);

    Delegate delegate = Delegate.builder().delegateType("ECS").delegateRandomToken("token").sequenceNum("1").build();
    DelegateSequenceConfig config = aDelegateSequenceBuilder()
                                        .withDelegateToken("token")
                                        .withSequenceNum(Integer.valueOf(1))
                                        .withAccountId(ACCOUNT_ID)
                                        .withHostName("hostName")
                                        .build();
    assertThat(delegateService.seqNumAndTokenMatchesConfig(delegate, config)).isTrue();

    config.setDelegateToken("abc");
    assertThat(delegateService.seqNumAndTokenMatchesConfig(delegate, config)).isFalse();

    config.setDelegateToken(null);
    assertThat(delegateService.seqNumAndTokenMatchesConfig(delegate, config)).isFalse();

    config.setDelegateToken(StringUtils.EMPTY);
    assertThat(delegateService.seqNumAndTokenMatchesConfig(delegate, config)).isFalse();
  }

  /**
   * if existing delegateSequenceConfig records are,
   * {accId=1, seqNum=0, token="abc", hostname="host"},
   * {accId=1, seqNum=1, token="abc", hostname="host"}
   * {accId=1, seqNum=2, token="abc", hostname="host"}
   * New record should be {accId=1, seqNum=3, token="abc", hostname="host"}
   *
   * if existing delegateSequenceConfig records are,
   * {accId=1, seqNum=0, token="abc", hostname="host"},
   * {accId=1, seqNum=1, token="abc", hostname="host"}
   * {accId=1, seqNum=3, token="abc", hostname="host"}
   * New record should be {accId=1, seqNum=2, token="abc", hostname="host"}
   */
  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testAddNewDelegateSequenceConfigRecord() {
    delegateService = spy(DelegateServiceImpl.class);
    mockWingsPersistanceForUpdateCall();

    List<DelegateSequenceConfig> existingDelegateSequenceConfigs = getExistingDelegateSequenceConfigs();
    doReturn(existingDelegateSequenceConfigs).when(query).asList();

    Delegate delegate =
        Delegate.builder().hostName("hostname").accountId(ACCOUNT_ID).delegateRandomToken("token").build();

    // existing sequenceConfigs are {.. seqNum = 0 / 1 / 2}, so 3 should picked as new
    DelegateSequenceConfig config = delegateService.addNewDelegateSequenceConfigRecord(delegate);
    assertThat(config).isNotNull();
    assertThat(config.getSequenceNum().intValue()).isEqualTo(3);
    assertThat(config.getHostName()).isEqualTo("hostname");
    assertThat(config.getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(delegate.getSequenceNum()).isEqualTo("3");

    // existing sequenceConfigs are {.. seqNum = 0 / 1 / 3}, so 2 should picked as new
    existingDelegateSequenceConfigs.get(2).setSequenceNum(Integer.valueOf(3));
    delegate.setSequenceNum(null);
    config = delegateService.addNewDelegateSequenceConfigRecord(delegate);
    assertThat(config).isNotNull();
    assertThat(config.getSequenceNum().intValue()).isEqualTo(2);
    assertThat(config.getHostName()).isEqualTo("hostname");
    assertThat(config.getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(delegate.getSequenceNum()).isEqualTo("2");
  }

  /**
   *  Test 2 scenarios
   *  - Stale DelegateSequenceConfig exists, that  is taken
   *  and we do not create new DelegateSequenceconfig record
   *  - No stale config, we take path to create a new record for DelegateSequenceConfig
   */
  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testRegisterDelegateWithNewSequenceGeneration() throws Exception {
    delegateService = spy(DelegateServiceImpl.class);
    mockWingsPersistanceForUpdateCall();

    // Case 1: using seqNum from stale DelegateSequenceConfig
    doReturn(getExistingDelegateSequenceConfigs())
        .when(delegateService)
        .getDelegateSequenceConfigs(any(Delegate.class));

    doReturn(aDelegateSequenceBuilder().build())
        .doReturn(null)
        .when(delegateService)
        .getInactiveDelegateSequenceConfigToReplace(any(Delegate.class), anyList());

    doAnswer(returnsSecondArg())
        .when(delegateService)
        .upsertDelegateOperation(any(Delegate.class), any(Delegate.class));

    doReturn(aDelegateSequenceBuilder().withSequenceNum(Integer.valueOf(5)).build())
        .when(delegateService)
        .addNewDelegateSequenceConfigRecord(any(Delegate.class));

    doNothing().when(delegateService).initDelegateWithConfigFromExistingDelegate(any(Delegate.class));

    Delegate delegate = Delegate.builder().accountId(ACCOUNT_ID).hostName("hostname").build();
    delegate = delegateService.registerDelegateWithNewSequenceGeneration(delegate);
    assertThat(delegate).isNotNull();
    verify(delegateService, times(0)).addNewDelegateSequenceConfigRecord(any(Delegate.class));

    // Case 2: creating new DelegateSequenceConfig record
    doReturn(getExistingDelegateSequenceConfigs())
        .when(delegateService)
        .getDelegateSequenceConfigs(any(Delegate.class));

    doAnswer(returnsSecondArg())
        .when(delegateService)
        .upsertDelegateOperation(any(Delegate.class), any(Delegate.class));
    delegate = delegateService.registerDelegateWithNewSequenceGeneration(delegate);
    assertThat(delegate.getHostName()).isEqualTo("hostname_5");
  }

  private void mockWingsPersistanceForUpdateCall() {
    on(delegateService).set("persistence", persistence);
    doReturn(query).when(persistence).createQuery(any(Class.class));
    doReturn(query).when(query).filter(anyString(), any());
    doReturn(query).when(query).project(anyString(), anyBoolean());
    doReturn(null).when(persistence).update(any(Query.class), any(UpdateOperations.class));
    doReturn(updateOperations).when(persistence).createUpdateOperations(any(Class.class));
    doReturn(updateOperations).when(updateOperations).set(anyString(), any());
  }

  private List<DelegateSequenceConfig> getExistingDelegateSequenceConfigs() {
    List<DelegateSequenceConfig> existingDelegateSequenceConfigs = new ArrayList<>();

    existingDelegateSequenceConfigs.add(aDelegateSequenceBuilder()
                                            .withDelegateToken("abc")
                                            .withHostName("hostname")
                                            .withSequenceNum(0)
                                            .withAccountId(ACCOUNT_ID)
                                            .withLastUpdatedAt(System.currentTimeMillis())
                                            .build());

    existingDelegateSequenceConfigs.add(
        aDelegateSequenceBuilder()
            .withDelegateToken("abc")
            .withHostName("hostname")
            .withSequenceNum(1)
            .withAccountId(ACCOUNT_ID)
            .withLastUpdatedAt(System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(200))
            .build());

    existingDelegateSequenceConfigs.add(aDelegateSequenceBuilder()
                                            .withDelegateToken("abc")
                                            .withHostName("hostname")
                                            .withSequenceNum(2)
                                            .withAccountId(ACCOUNT_ID)
                                            .withLastUpdatedAt(System.currentTimeMillis())
                                            .build());
    return existingDelegateSequenceConfigs;
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testGetDelegateHostNameByRemovingSeqNum() throws Exception {
    delegateService = spy(DelegateServiceImpl.class);
    assertThat(delegateService.getDelegateHostNameByRemovingSeqNum(
                   Delegate.builder().hostName("hostname_harness__delegate_1").build()))
        .isEqualTo("hostname_harness__delegate");
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testGetDelegateSeqNumFromHostName() throws Exception {
    delegateService = spy(DelegateServiceImpl.class);
    assertThat(delegateService.getDelegateSeqNumFromHostName(
                   Delegate.builder().hostName("hostname_harness__delegate_1").build()))
        .isEqualTo("1");
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testUpdateExistingDelegateWithSequenceConfigData() throws Exception {
    delegateService = spy(DelegateServiceImpl.class);
    doReturn(aDelegateSequenceBuilder()
                 .withSequenceNum(1)
                 .withHostName("hostname_harness__delegate")
                 .withDelegateToken("token")
                 .build())
        .when(delegateService)
        .getDelegateSequenceConfig(anyString(), anyString(), anyInt());

    Delegate delegate = Delegate.builder().hostName("hostname_harness__delegate_1").build();
    delegateService.updateExistingDelegateWithSequenceConfigData(delegate);
    assertThat(delegate.getSequenceNum()).isEqualTo("1");
    assertThat(delegate.getDelegateRandomToken()).isEqualTo("token");

    ArgumentCaptor<Integer> captorSeqNum = ArgumentCaptor.forClass(Integer.class);
    ArgumentCaptor<String> captorHostName = ArgumentCaptor.forClass(String.class);
    verify(delegateService).getDelegateSequenceConfig(anyString(), captorHostName.capture(), captorSeqNum.capture());
    assertThat(captorHostName.getValue()).isEqualTo("hostname_harness__delegate");
    assertThat(captorSeqNum.getValue().intValue()).isEqualTo(1);
  }
}
