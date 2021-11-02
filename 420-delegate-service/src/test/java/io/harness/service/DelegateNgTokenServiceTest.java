package io.harness.service;

import static io.harness.rule.OwnerRule.VLAD;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.MockitoAnnotations.initMocks;

import io.harness.DelegateServiceTestBase;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.DelegateTokenDetails;
import io.harness.outbox.api.OutboxService;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;
import io.harness.service.impl.DelegateNgTokenServiceImpl;

import com.google.inject.Inject;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;

@OwnedBy(HarnessTeam.DEL)
public class DelegateNgTokenServiceTest extends DelegateServiceTestBase {
  private static final String TEST_ACCOUNT_ID = "testAccountId";

  @Inject private HPersistence persistence;
  @Inject private OutboxService outboxService;
  @InjectMocks @Inject private DelegateNgTokenServiceImpl delegateNgTokenService;

  @Before
  public void setUp() {
    initMocks(this);
  }

  @Test
  @Owner(developers = VLAD)
  @Category(UnitTests.class)
  public void shouldCreateToken() {
    String tokenName = "token1";
    DelegateTokenDetails token = delegateNgTokenService.createToken(TEST_ACCOUNT_ID, null, tokenName);
    assertThat(token.getName()).isEqualTo(tokenName);
  }
}
