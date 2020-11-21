package io.harness.ccm.views.service.impl;

import static io.harness.rule.OwnerRule.HITESH;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.doReturn;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ccm.views.dao.CEViewDao;
import io.harness.ccm.views.entities.CEView;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class CEViewServiceImplTest extends CategoryTest {
  @InjectMocks @Inject private CEViewServiceImpl ceViewService;
  @Mock private CEViewDao ceViewDao;

  private static final String ACCOUNT_ID = "account_id";
  private static final String VIEW_NAME = "view_name";
  private static final String VIEW_FIELD = "projectId";

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void shouldThrowExceptionWhileSavingCustomField() {
    doReturn(ceView()).when(ceViewDao).findByName(ACCOUNT_ID, VIEW_NAME);
    assertThatExceptionOfType(InvalidRequestException.class).isThrownBy(() -> ceViewService.save(ceView()));
  }

  private CEView ceView() {
    return CEView.builder().name(VIEW_NAME).accountId(ACCOUNT_ID).build();
  }
}
