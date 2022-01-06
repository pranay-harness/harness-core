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
import static org.assertj.core.api.Assertions.fail;

import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;

import software.wings.beans.User;
import software.wings.graphql.datafetcher.AbstractDataFetcherTestBase;
import software.wings.graphql.schema.query.QLTagQueryParameters;
import software.wings.graphql.schema.type.QLTagEntity;
import software.wings.security.UserThreadLocal;

import com.google.inject.Inject;
import java.sql.SQLException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(CDC)
public class TagDataFetcherTest extends AbstractDataFetcherTestBase {
  @Inject TagDataFetcher tagDataFetcher;
  @Before
  public void setup() throws SQLException {
    User user = testUtils.createUser(testUtils.createAccount());
    UserThreadLocal.set(user);
    createAccount(ACCOUNT1_ID, getLicenseInfo());
  }

  @Test
  @Owner(developers = HINGER)
  @Category(UnitTests.class)
  public void testTagDataFetcher() {
    createTag(ACCOUNT1_ID, TAG1_ID_ACCOUNT1, TAG_KEY);
    QLTagEntity qlTagEntityByName =
        tagDataFetcher.fetch(QLTagQueryParameters.builder().name(TAG_KEY).build(), ACCOUNT1_ID);
    QLTagEntity qlTagEntityById =
        tagDataFetcher.fetch(QLTagQueryParameters.builder().tagId(TAG1_ID_ACCOUNT1).build(), ACCOUNT1_ID);

    assertThat(qlTagEntityByName).isNotNull();
    assertThat(qlTagEntityByName.getId()).isEqualTo(TAG1_ID_ACCOUNT1);

    assertThat(qlTagEntityById).isNotNull();
    assertThat(qlTagEntityById.getName()).isEqualTo(TAG_KEY);

    try {
      // fetch non existent tag
      QLTagEntity qlTagEntity_notExist =
          tagDataFetcher.fetch(QLTagQueryParameters.builder().tagId(TAG2_ID_ACCOUNT1).build(), ACCOUNT1_ID);
      fail("InvalidRequestException expected here");
    } catch (Exception e) {
      assertThat(e).isInstanceOf(InvalidRequestException.class);
      assertThat(e).hasMessage("Tag does not exist");
    }
  }
}
