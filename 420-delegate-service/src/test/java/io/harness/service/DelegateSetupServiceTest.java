package io.harness.service;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.delegate.beans.DelegateType.KUBERNETES;
import static io.harness.rule.OwnerRule.MARKO;
import static io.harness.rule.OwnerRule.NICOLAS;
import static io.harness.rule.OwnerRule.VUK;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import io.harness.DelegateServiceTestBase;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.Delegate;
import io.harness.delegate.beans.Delegate.DelegateBuilder;
import io.harness.delegate.beans.DelegateEntityOwner;
import io.harness.delegate.beans.DelegateGroup;
import io.harness.delegate.beans.DelegateGroupDetails;
import io.harness.delegate.beans.DelegateGroupListing;
import io.harness.delegate.beans.DelegateInsightsBarDetails;
import io.harness.delegate.beans.DelegateInsightsDetails;
import io.harness.delegate.beans.DelegateInstanceStatus;
import io.harness.delegate.beans.DelegateProfile;
import io.harness.delegate.beans.DelegateSizeDetails;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;
import io.harness.service.impl.DelegateSetupServiceImpl;
import io.harness.service.intfc.DelegateCache;
import io.harness.service.intfc.DelegateInsightsService;

import software.wings.beans.DelegateConnection;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import java.util.Arrays;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@OwnedBy(HarnessTeam.DEL)
public class DelegateSetupServiceTest extends DelegateServiceTestBase {
  private static final String VERSION = "1.0.0";
  private static final String GROUPED_HOSTNAME_SUFFIX = "-{n}";

  @Mock private DelegateCache delegateCache;
  @Mock private DelegateInsightsService delegateInsightsService;

  @InjectMocks @Inject private DelegateSetupServiceImpl delegateSetupService;
  @Inject private HPersistence persistence;

  @Before
  public void setUp() {
    initMocks(this);
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void shouldListAccountDelegateGroups() {
    String accountId = generateUuid();
    String delegateProfileId = generateUuid();

    when(delegateCache.getDelegateProfile(accountId, delegateProfileId))
        .thenReturn(DelegateProfile.builder().name("profile").selectors(ImmutableList.of("s1", "s2")).build());

    DelegateGroup delegateGroup1 = DelegateGroup.builder().name("grp1").accountId(accountId).build();
    persistence.save(delegateGroup1);
    DelegateGroup delegateGroup2 = DelegateGroup.builder().name("grp2").accountId(accountId).build();
    persistence.save(delegateGroup2);

    // Insights
    DelegateInsightsDetails delegateInsightsDetails =
        DelegateInsightsDetails.builder()
            .insights(ImmutableList.of(
                DelegateInsightsBarDetails.builder().build(), DelegateInsightsBarDetails.builder().build()))
            .build();
    when(
        delegateInsightsService.retrieveDelegateInsightsDetails(eq(accountId), eq(delegateGroup1.getUuid()), anyLong()))
        .thenReturn(delegateInsightsDetails);

    // these three delegates should be returned for group 1
    Delegate delegate1 = createDelegateBuilder()
                             .accountId(accountId)
                             .ng(true)
                             .delegateType(KUBERNETES)
                             .delegateName("grp1")
                             .hostName("kube-0")
                             .sizeDetails(DelegateSizeDetails.builder().replicas(2).build())
                             .delegateGroupId(delegateGroup1.getUuid())
                             .delegateProfileId(delegateProfileId)
                             .build();

    Delegate delegate2 = createDelegateBuilder()
                             .accountId(accountId)
                             .ng(true)
                             .delegateType(KUBERNETES)
                             .delegateName("grp1")
                             .hostName("kube-1")
                             .sizeDetails(DelegateSizeDetails.builder().replicas(2).build())
                             .delegateGroupId(delegateGroup1.getUuid())
                             .delegateProfileId(delegateProfileId)
                             .lastHeartBeat(System.currentTimeMillis() - 60000)
                             .build();

    // this delegate should cause an empty group to be returned
    Delegate delegate3 = createDelegateBuilder()
                             .accountId(accountId)
                             .ng(true)
                             .delegateName("grp2")
                             .sizeDetails(DelegateSizeDetails.builder().replicas(1).build())
                             .delegateGroupId(delegateGroup2.getUuid())
                             .build();

    Delegate deletedDelegate =
        createDelegateBuilder().accountId(accountId).status(DelegateInstanceStatus.DELETED).build();

    Delegate orgDelegate = createDelegateBuilder()
                               .accountId(accountId)
                               .owner(DelegateEntityOwner.builder().identifier(generateUuid()).build())
                               .build();

    persistence.save(Arrays.asList(orgDelegate, deletedDelegate, delegate1, delegate2, delegate3));

    DelegateConnection delegateConnection1 = DelegateConnection.builder()
                                                 .accountId(accountId)
                                                 .delegateId(delegate1.getUuid())
                                                 .lastHeartbeat(System.currentTimeMillis())
                                                 .disconnected(false)
                                                 .version(VERSION)
                                                 .build();
    DelegateConnection delegateConnection2 = DelegateConnection.builder()
                                                 .accountId(accountId)
                                                 .delegateId(delegate2.getUuid())
                                                 .lastHeartbeat(System.currentTimeMillis())
                                                 .disconnected(false)
                                                 .version(VERSION)
                                                 .build();
    persistence.save(delegateConnection1);
    persistence.save(delegateConnection2);

    DelegateGroupListing delegateGroupListing = delegateSetupService.listDelegateGroupDetails(accountId, null, null);

    assertThat(delegateGroupListing.getDelegateGroupDetails()).hasSize(2);
    assertThat(delegateGroupListing.getDelegateGroupDetails())
        .extracting(DelegateGroupDetails::getGroupName)
        .containsOnly("grp1", "grp2");

    for (DelegateGroupDetails group : delegateGroupListing.getDelegateGroupDetails()) {
      if (group.getGroupName().equals("grp1")) {
        assertThat(group.getDelegateInstanceDetails()).hasSize(2);
        assertThat(group.getDelegateType()).isEqualTo(KUBERNETES);
        assertThat(group.getGroupHostName()).isEqualTo("kube-{n}");
        assertThat(group.getGroupImplicitSelectors()).isNotNull();
        assertThat(group.getGroupImplicitSelectors().containsKey("grp1")).isTrue();
        assertThat(group.getGroupImplicitSelectors().containsKey("kube-0")).isFalse();
        assertThat(group.getGroupImplicitSelectors().containsKey("kube-1")).isFalse();
        assertThat(group.getGroupImplicitSelectors().containsKey("profile")).isTrue();
        assertThat(group.getGroupImplicitSelectors().containsKey("s1")).isTrue();
        assertThat(group.getGroupImplicitSelectors().containsKey("s2")).isTrue();
        assertThat(group.getLastHeartBeat()).isEqualTo(delegate1.getLastHeartBeat());
        assertThat(group.isActivelyConnected()).isTrue();
        assertThat(group.getDelegateReplicas()).isEqualTo(2);
        assertThat(group.getDelegateInstanceDetails())
            .extracting(DelegateGroupListing.DelegateInner::getUuid)
            .containsOnly(delegate1.getUuid(), delegate2.getUuid());
        assertThat(group.getDelegateInsightsDetails()).isNotNull();
        assertThat(group.getDelegateInsightsDetails().getInsights()).hasSize(2);
      } else if (group.getGroupName().equals("grp2")) {
        assertThat(group.getDelegateInstanceDetails()).isEmpty();
        assertThat(group.isActivelyConnected()).isFalse();
        assertThat(group.getDelegateReplicas()).isEqualTo(1);
      }
    }
  }

  private DelegateBuilder createDelegateBuilder() {
    return Delegate.builder()
        .ip("127.0.0.1")
        .hostName("localhost")
        .delegateName("testDelegateName")
        .delegateType(KUBERNETES)
        .version(VERSION)
        .status(DelegateInstanceStatus.ENABLED)
        .lastHeartBeat(System.currentTimeMillis());
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void shouldRetrieveGroupedHostnameNullValue() {
    String hostNameForGroupedDelegate = delegateSetupService.getHostNameForGroupedDelegate(null);

    assertThat(hostNameForGroupedDelegate).isNull();
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void shouldRetrieveGroupedHostnameEmptyValue() {
    String hostNameForGroupedDelegate = delegateSetupService.getHostNameForGroupedDelegate("");

    assertThat(hostNameForGroupedDelegate).isEmpty();
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void shouldRetrieveGroupedHostnameValidValue() {
    String hostNameForGroupedDelegate = delegateSetupService.getHostNameForGroupedDelegate("test-hostname-1");

    assertThat(hostNameForGroupedDelegate).isEqualTo("test-hostname" + GROUPED_HOSTNAME_SUFFIX);
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void shouldRetrieveDelegatesImplicitSelectors() {
    String accountId = generateUuid();
    String delegateProfileId = generateUuid();

    DelegateProfile delegateProfile = DelegateProfile.builder()
                                          .uuid(delegateProfileId)
                                          .accountId(accountId)
                                          .name(generateUuid())
                                          .selectors(ImmutableList.of("jkl", "fgh"))
                                          .build();

    when(delegateCache.getDelegateProfile(accountId, delegateProfileId)).thenReturn(delegateProfile);

    Delegate delegate = Delegate.builder()
                            .accountId(accountId)
                            .ip("127.0.0.1")
                            .hostName("host")
                            .delegateName("test")
                            .version(VERSION)
                            .status(DelegateInstanceStatus.ENABLED)
                            .lastHeartBeat(System.currentTimeMillis())
                            .delegateProfileId(delegateProfile.getUuid())
                            .build();
    persistence.save(delegate);

    Set<String> tags = delegateSetupService.retrieveDelegateImplicitSelectors(delegate).keySet();
    assertThat(tags.size()).isEqualTo(5);
    assertThat(tags).containsExactlyInAnyOrder(delegateProfile.getName().toLowerCase(), "test", "jkl", "fgh", "host");
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void shouldRetrieveDelegateImplicitSelectorsWithDelegateProfileSelectorsOnly() {
    String accountId = generateUuid();
    String delegateProfileId = generateUuid();

    DelegateProfile delegateProfile = DelegateProfile.builder()
                                          .uuid(delegateProfileId)
                                          .accountId(accountId)
                                          .selectors(ImmutableList.of("jkl", "fgh"))
                                          .build();

    when(delegateCache.getDelegateProfile(accountId, delegateProfileId)).thenReturn(delegateProfile);

    Delegate delegate = Delegate.builder()
                            .accountId(accountId)
                            .delegateProfileId(delegateProfile.getUuid())
                            .ip("127.0.0.1")
                            .version(VERSION)
                            .status(DelegateInstanceStatus.ENABLED)
                            .lastHeartBeat(System.currentTimeMillis())
                            .build();
    persistence.save(delegate);

    Set<String> selectors = delegateSetupService.retrieveDelegateImplicitSelectors(delegate).keySet();
    assertThat(selectors.size()).isEqualTo(2);
    assertThat(selectors).containsExactly("fgh", "jkl");
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void shouldRetrieveDelegateImplicitSelectorsWithHostName() {
    String accountId = generateUuid();

    Delegate delegate = Delegate.builder()
                            .accountId(accountId)
                            .ip("127.0.0.1")
                            .hostName("a.b.c")
                            .version(VERSION)
                            .status(DelegateInstanceStatus.ENABLED)
                            .lastHeartBeat(System.currentTimeMillis())
                            .build();
    persistence.save(delegate);

    Set<String> tags = delegateSetupService.retrieveDelegateImplicitSelectors(delegate).keySet();
    assertThat(tags.size()).isEqualTo(1);
  }

  @Test
  @Owner(developers = NICOLAS)
  @Category(UnitTests.class)
  public void shouldRetrieveDelegateImplicitSelectorsWithGroupName() {
    String accountId = generateUuid();

    DelegateGroup delegateGroup =
        DelegateGroup.builder().uuid(generateUuid()).accountId(accountId).name("group").build();
    when(delegateCache.getDelegateGroup(accountId, delegateGroup.getUuid())).thenReturn(delegateGroup);

    Delegate delegate = Delegate.builder()
                            .accountId(accountId)
                            .version(VERSION)
                            .hostName("host")
                            .status(DelegateInstanceStatus.ENABLED)
                            .lastHeartBeat(System.currentTimeMillis())
                            .delegateGroupId(delegateGroup.getUuid())
                            .build();
    persistence.save(delegate);

    Set<String> tags = delegateSetupService.retrieveDelegateImplicitSelectors(delegate).keySet();
    assertThat(tags.size()).isEqualTo(1);
    assertThat(tags).containsExactlyInAnyOrder("group");
  }
}
