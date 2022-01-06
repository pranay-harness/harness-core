/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service;

import static io.harness.rule.OwnerRule.ANUBHAW;
import static io.harness.rule.OwnerRule.UNKNOWN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import io.harness.category.element.UnitTests;
import io.harness.exception.WingsException;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.service.intfc.DownloadTokenService;

import com.google.inject.Inject;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Created by peeyushaggarwal on 12/13/16.
 */
public class DownloadTokenServiceTest extends WingsBaseTest {
  @Inject private DownloadTokenService downloadTokenService;

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void shouldCreateToken() {
    String token = downloadTokenService.createDownloadToken("resource");
    assertThat(token).isNotEmpty();
  }

  @Test
  @Owner(developers = ANUBHAW)
  @Category(UnitTests.class)
  public void shouldValidateToken() {
    String token = downloadTokenService.createDownloadToken("resource");
    downloadTokenService.validateDownloadToken("resource", token);
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void shouldThrowExceptionWhenNoTokenFoundOnValidation() {
    assertThatExceptionOfType(WingsException.class)
        .isThrownBy(() -> downloadTokenService.validateDownloadToken("resource", "token"));
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void shouldThrowExceptionWhenResourceDoesntMatchOnValiation() {
    String token = downloadTokenService.createDownloadToken("resource");
    assertThatExceptionOfType(WingsException.class)
        .isThrownBy(() -> downloadTokenService.validateDownloadToken("resource1", "token"));
  }
}
