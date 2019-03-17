package software.wings.service.impl;

import static org.joor.Reflect.on;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
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
import static software.wings.beans.Delegate.Builder.aDelegate;
import static software.wings.beans.DelegateSequenceConfig.Builder.aDelegateSequenceBuilder;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;

import io.harness.category.element.UnitTests;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.WingsBaseTest;
import software.wings.beans.Delegate;
import software.wings.beans.DelegateSequenceConfig;
import software.wings.dl.WingsPersistence;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class EcsDelegateRegistrationTest extends WingsBaseTest {
  private static final Logger logger = LoggerFactory.getLogger(EcsDelegateRegistrationTest.class);

  DelegateServiceImpl delegateService;
  @Mock WingsPersistence wingsPersistence;
  @Mock Query query;
  @Mock UpdateOperations updateOperations;

  /**
   * Test keepAlivePath is taken when delegate.KeepAlivePacket = true
   */
  @Test
  @Category(UnitTests.class)
  public void testHandleEcsDelegateRequest_KeepAliveRequest() throws Exception {
    delegateService = spy(DelegateServiceImpl.class);
    doReturn(null).when(delegateService).handleEcsDelegateKeepAlivePacket(any());

    delegateService.handleEcsDelegateRequest(aDelegate().withKeepAlivePacket(true).build());

    verify(delegateService).handleEcsDelegateKeepAlivePacket(any());
    verify(delegateService, times(0)).handleEcsDelegateRegistration(any());
  }

  /**
   * Test EcsDelegateRegistration path is taken, when delegate.KeepAlivePacket = false
   */
  @Test
  @Category(UnitTests.class)
  public void testHandleEcsDelegateRequest_EcsDelegateRegistration() throws Exception {
    delegateService = spy(DelegateServiceImpl.class);
    doReturn(aDelegate().withHostName("delegate_1").build()).when(delegateService).handleEcsDelegateRegistration(any());
    doReturn(aDelegateSequenceBuilder().withHostName("delegate").withSequenceNum(1).withDelegateToken("token").build())
        .when(delegateService)
        .getDelegateSequenceConfig(anyString(), anyString(), anyInt());
    delegateService.handleEcsDelegateRequest(aDelegate().withKeepAlivePacket(false).build());

    verify(delegateService, times(0)).handleEcsDelegateKeepAlivePacket(any());
    verify(delegateService).handleEcsDelegateRegistration(any());
  }

  /**
   * Test HandleEcsDelegateKeepAlivePacket flow
   */
  @Test
  @Category(UnitTests.class)
  public void testHandleEcsDelegateKeepAlivePacket() throws Exception {
    delegateService = spy(DelegateServiceImpl.class);
    doReturn(null)
        .doReturn(aDelegate().build())
        .doReturn(aDelegate().build())
        .when(delegateService)
        .getDelegateUsingSequenceNum(anyString(), anyString(), anyString());

    doReturn(aDelegateSequenceBuilder().withDelegateToken("aabbcc").build())
        .doReturn(aDelegateSequenceBuilder().withDelegateToken("aabbcc").build())
        .when(delegateService)
        .getDelegateSequenceConfig(anyString(), anyString(), anyInt());

    Delegate delegate = aDelegate().build();
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
    verify(wingsPersistence, times(0)).createQuery(any());
    verify(wingsPersistence, times(0)).update(any(Query.class), any(UpdateOperations.class));

    mockWingsPersistanceForUpdateCall();

    delegate.setDelegateRandomToken("aabbcc");
    delegateService.handleEcsDelegateKeepAlivePacket(delegate);
    verify(wingsPersistence, times(1)).createQuery(any());
    verify(wingsPersistence, times(1)).update(any(Query.class), any(UpdateOperations.class));
  }

  /**
   * Delegate heartbeat with valid delegateId and token.
   * Expected : - get delegate from db with Id.
   *            - get DelegateSequenceConfig for that delegate
   *            - match seqNum and token sent by delegate with this config
   *            - just update this same existing delegate
   */
  @Test
  @Category(UnitTests.class)
  public void testHandleEcsDelegateRegistration_activeDelegateWithId() throws Exception {
    delegateService = spy(DelegateServiceImpl.class);
    mockWingsPersistanceForUpdateCall();

    DelegateSequenceConfig config = aDelegateSequenceBuilder().withDelegateToken("token").build();
    doReturn(config).when(delegateService).getDelegateSequenceConfig(anyString(), anyString(), anyInt());

    Delegate delegate = aDelegate()
                            .withUuid("12345")
                            .withDelegateType("ECS")
                            .withDelegateRandomToken("token")
                            .withSequenceNum("1")
                            .build();

    doReturn(delegate).when(query).get();

    doAnswer(returnsSecondArg())
        .when(delegateService)
        .upsertDelegateOperation(any(Delegate.class), any(Delegate.class));
    delegate = delegateService.handleEcsDelegateRegistration(delegate);
    assertNotNull(delegate);
    assertEquals("12345", delegate.getUuid());

    verify(delegateService, times(1)).handleECSRegistrationUsingID(any(Delegate.class));
    verify(delegateService, times(0)).handleECSRegistrationUsingSeqNumAndToken(any(Delegate.class));
    verify(delegateService, times(0)).registerDelegateWithNewSequenceGeneration(any(Delegate.class));
  }

  /**
   * Delegate heartbeat with delegateId = null and delegateToken = null.
   * Expected - Should throw exception
   */
  @Test
  @Category(UnitTests.class)
  public void testHandleEcsDelegateRegistration_empty_UUid_token() throws Exception {
    delegateService = spy(DelegateServiceImpl.class);

    DelegateSequenceConfig config = aDelegateSequenceBuilder().withDelegateToken("token").build();
    doReturn(null).when(delegateService).getDelegateSequenceConfig(anyString(), anyString(), anyInt());

    Delegate delegate = aDelegate().withDelegateType("ECS").build();
    try {
      delegateService.handleEcsDelegateRegistration(delegate);
      assertTrue(false);
    } catch (Exception e) {
      assertEquals("Received invalid token from ECS delegate", e.getMessage());
    }

    try {
      delegate.setUuid("12345");
      delegate.setSequenceNum("1");
      delegateService.handleEcsDelegateRegistration(delegate);
      assertTrue(false);
    } catch (Exception e) {
      assertEquals("Received invalid token from ECS delegate", e.getMessage());
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

    Delegate delegate =
        aDelegate().withDelegateType("ECS").withDelegateRandomToken("token").withSequenceNum("1").build();

    Delegate existingDelegate = aDelegate()
                                    .withDelegateType("ECS")
                                    .withUuid("12345")
                                    .withDelegateRandomToken("token")
                                    .withSequenceNum("1")
                                    .build();

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
    assertNotNull(delegate);
    assertEquals("12345", delegate.getUuid());

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

    Delegate delegate =
        aDelegate().withDelegateType("ECS").withDelegateRandomToken("token").withSequenceNum("1").build();

    doReturn(null).when(delegateService).getDelegateUsingSequenceNum(anyString(), anyString(), anyString());

    doNothing().when(delegateService).initDelegateWithConfigFromExistingDelegate(any(Delegate.class));

    // firstArg is existingDelegate fetched from db
    // secondArg is delegate arg passed.
    doAnswer(returnsSecondArg())
        .when(delegateService)
        .upsertDelegateOperation(any(Delegate.class), any(Delegate.class));

    delegate = delegateService.handleEcsDelegateRegistration(delegate);
    assertNotNull(delegate);
    assertEquals(null, delegate.getUuid());
    assertEquals("hostName_1", delegate.getHostName());

    // existing delegate should be null for upsertDelegateOperation(null, newDelegate)
    ArgumentCaptor<Delegate> captor = ArgumentCaptor.forClass(Delegate.class);
    verify(delegateService).upsertDelegateOperation(captor.capture(), any(Delegate.class));
    assertNull(captor.getValue());

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
    Delegate delegate =
        aDelegate().withDelegateType("ECS").withDelegateRandomToken("token").withSequenceNum("1").build();

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
  @Category(UnitTests.class)
  public void testGetInactiveDelegateSequenceConfigToReplace() throws Exception {
    delegateService = spy(DelegateServiceImpl.class);

    List<DelegateSequenceConfig> existingDelegateSequenceConfigs = getExistingDelegateSequenceConfigs();

    Delegate delegate = aDelegate().withDelegateType("ECS").withHostName("hostname").build();

    doReturn(aDelegate().withUuid("12345").withTags(Arrays.asList("tag1", "tag2")).build())
        .doReturn(null)
        .when(delegateService)
        .getDelegateUsingSequenceNum(anyString(), anyString(), anyString());

    mockWingsPersistanceForUpdateCall();
    doNothing().when(delegateService).delete(anyString(), anyString());

    DelegateSequenceConfig config =
        delegateService.getInactiveDelegateSequenceConfigToReplace(delegate, existingDelegateSequenceConfigs);
    assertNotNull(config);
    assertEquals(1, config.getSequenceNum().intValue());

    assertNotNull(delegate.getTags());
    assertEquals(2, delegate.getTags().size());
    assertTrue(delegate.getTags().contains("tag1"));
    assertTrue(delegate.getTags().contains("tag2"));
    assertEquals("hostname_1", delegate.getHostName());

    // existing delegate assocaited to stale sequenceConfig is deleted
    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(delegateService).delete(anyString(), captor.capture());
    assertEquals("12345", captor.getValue());
  }

  /**
   * SeqNum and token sent by delegate matches DelegateSequenceConfig
   * - Test for NPEs
   * @throws Exception
   */
  @Test
  @Category(UnitTests.class)
  public void testSeqNumAndTokenMatchesConfig() throws Exception {
    delegateService = spy(DelegateServiceImpl.class);

    Delegate delegate =
        aDelegate().withDelegateType("ECS").withDelegateRandomToken("token").withSequenceNum("1").build();
    DelegateSequenceConfig config = aDelegateSequenceBuilder()
                                        .withDelegateToken("token")
                                        .withSequenceNum(Integer.valueOf(1))
                                        .withAccountId(ACCOUNT_ID)
                                        .withHostName("hostName")
                                        .build();
    assertTrue(delegateService.seqNumAndTokenMatchesConfig(delegate, config));

    config.setDelegateToken("abc");
    assertFalse(delegateService.seqNumAndTokenMatchesConfig(delegate, config));

    config.setDelegateToken(null);
    assertFalse(delegateService.seqNumAndTokenMatchesConfig(delegate, config));

    config.setDelegateToken(StringUtils.EMPTY);
    assertFalse(delegateService.seqNumAndTokenMatchesConfig(delegate, config));
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
  @Category(UnitTests.class)
  public void testAddNewDelegateSequenceConfigRecord() {
    delegateService = spy(DelegateServiceImpl.class);
    mockWingsPersistanceForUpdateCall();

    List<DelegateSequenceConfig> existingDelegateSequenceConfigs = getExistingDelegateSequenceConfigs();
    doAnswer(returnsSecondArg()).when(wingsPersistence).saveAndGet(any(Class.class), any(DelegateSequenceConfig.class));
    doReturn(existingDelegateSequenceConfigs).when(query).asList();

    Delegate delegate =
        aDelegate().withHostName("hostname").withAccountId(ACCOUNT_ID).withDelegateRandomToken("token").build();

    // existing sequenceConfigs are {.. seqNum = 0 / 1 / 2}, so 3 should picked as new
    DelegateSequenceConfig config = delegateService.addNewDelegateSequenceConfigRecord(delegate);
    assertNotNull(config);
    assertEquals(3, config.getSequenceNum().intValue());
    assertEquals("hostname", config.getHostName());
    assertEquals(ACCOUNT_ID, config.getAccountId());
    assertEquals("3", delegate.getSequenceNum());

    // existing sequenceConfigs are {.. seqNum = 0 / 1 / 3}, so 2 should picked as new
    existingDelegateSequenceConfigs.get(2).setSequenceNum(Integer.valueOf(3));
    delegate.setSequenceNum(null);
    config = delegateService.addNewDelegateSequenceConfigRecord(delegate);
    assertNotNull(config);
    assertEquals(2, config.getSequenceNum().intValue());
    assertEquals("hostname", config.getHostName());
    assertEquals(ACCOUNT_ID, config.getAccountId());
    assertEquals("2", delegate.getSequenceNum());
  }

  /**
   *  Test 2 scenarios
   *  - Stale DelegateSequenceConfig exists, that  is taken
   *  and we do not create new DelegateSequenceconfig record
   *  - No stale config, we take path to create a new record for DelegateSequenceConfig
   */
  @Test
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

    Delegate delegate = aDelegate().withAccountId(ACCOUNT_ID).withHostName("hostname").build();
    delegate = delegateService.registerDelegateWithNewSequenceGeneration(delegate);
    assertNotNull(delegate);
    verify(delegateService, times(0)).addNewDelegateSequenceConfigRecord(any(Delegate.class));

    // Case 2: creating new DelegateSequenceConfig record
    doReturn(getExistingDelegateSequenceConfigs())
        .when(delegateService)
        .getDelegateSequenceConfigs(any(Delegate.class));

    doAnswer(returnsSecondArg())
        .when(delegateService)
        .upsertDelegateOperation(any(Delegate.class), any(Delegate.class));
    delegate = delegateService.registerDelegateWithNewSequenceGeneration(delegate);
    assertEquals("hostname_5", delegate.getHostName());
  }

  private void mockWingsPersistanceForUpdateCall() {
    on(delegateService).set("wingsPersistence", wingsPersistence);
    doReturn(query).when(wingsPersistence).createQuery(any(Class.class));
    doReturn(query).when(query).filter(anyString(), any());
    doReturn(query).when(query).project(anyString(), anyBoolean());
    doReturn(null).when(wingsPersistence).update(any(Query.class), any(UpdateOperations.class));
    doReturn(updateOperations).when(wingsPersistence).createUpdateOperations(any(Class.class));
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
  @Category(UnitTests.class)
  public void testGetDelegateHostNameByRemovingSeqNum() throws Exception {
    delegateService = spy(DelegateServiceImpl.class);
    assertEquals("hostname_harness__delegate",
        delegateService.getDelegateHostNameByRemovingSeqNum(
            aDelegate().withHostName("hostname_harness__delegate_1").build()));
  }

  @Test
  @Category(UnitTests.class)
  public void testGetDelegateSeqNumFromHostName() throws Exception {
    delegateService = spy(DelegateServiceImpl.class);
    assertEquals("1",
        delegateService.getDelegateSeqNumFromHostName(
            aDelegate().withHostName("hostname_harness__delegate_1").build()));
  }

  @Test
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

    Delegate delegate = aDelegate().withHostName("hostname_harness__delegate_1").build();
    delegateService.updateExistingDelegateWithSequenceConfigData(delegate);
    assertEquals("1", delegate.getSequenceNum());
    assertEquals("token", delegate.getDelegateRandomToken());

    ArgumentCaptor<Integer> captorSeqNum = ArgumentCaptor.forClass(Integer.class);
    ArgumentCaptor<String> captorHostName = ArgumentCaptor.forClass(String.class);
    verify(delegateService).getDelegateSequenceConfig(anyString(), captorHostName.capture(), captorSeqNum.capture());
    assertEquals("hostname_harness__delegate", captorHostName.getValue());
    assertEquals(1, captorSeqNum.getValue().intValue());
  }
}
