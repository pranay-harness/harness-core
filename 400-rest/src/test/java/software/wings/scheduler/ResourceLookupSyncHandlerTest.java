/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.scheduler;

import static io.harness.rule.OwnerRule.HINGER;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.EntityType;
import software.wings.beans.HarnessTagLink;
import software.wings.beans.HarnessTagType;
import software.wings.beans.ResourceLookup;
import software.wings.beans.ResourceLookup.ResourceLookupKeys;

import com.google.inject.Inject;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

public class ResourceLookupSyncHandlerTest extends WingsBaseTest {
  @InjectMocks @Inject ResourceLookupSyncHandler resourceLookupSyncHandler;
  @Inject private HPersistence persistence;
  Account account;

  @Before
  public void Setup() {
    MockitoAnnotations.initMocks(this);
    account = getAccount("PAID");
  }

  @Test
  @Owner(developers = HINGER)
  @Category(UnitTests.class)
  public void testResourceLookupCreation() {
    HarnessTagLink harnessTagLink = HarnessTagLink.builder()
                                        .accountId(account.getUuid())
                                        .appId("appId")
                                        .entityId("entityId")
                                        .entityType(EntityType.SERVICE)
                                        .key("testKey")
                                        .value("testValue")
                                        .tagType(HarnessTagType.USER)
                                        .build();

    persistence.save(harnessTagLink);

    resourceLookupSyncHandler.syncTagLinkResourceLookup(account);

    List<ResourceLookup> resourceLookups =
        persistence.createQuery(ResourceLookup.class).filter(ResourceLookupKeys.accountId, account.getUuid()).asList();

    assertThat(resourceLookups.size()).isEqualTo(1);
    assertThat(resourceLookups.get(0).getTags().size()).isEqualTo(1);
  }
}
