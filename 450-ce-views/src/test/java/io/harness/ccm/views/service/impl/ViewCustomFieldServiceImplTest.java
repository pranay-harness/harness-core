package io.harness.ccm.views.service.impl;

import static io.harness.rule.OwnerRule.HITESH;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.doReturn;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ccm.views.dao.ViewCustomFieldDao;
import io.harness.ccm.views.entities.ViewCustomField;
import io.harness.ccm.views.entities.ViewField;
import io.harness.ccm.views.entities.ViewFieldIdentifier;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;

import com.google.cloud.bigquery.BigQuery;
import com.google.inject.Inject;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ViewCustomFieldServiceImplTest extends CategoryTest {
  @InjectMocks @Inject private ViewCustomFieldServiceImpl viewCustomFieldService;
  @Mock private ViewCustomFieldDao viewCustomFieldDao;
  @Mock BigQuery bigQuery;

  private static final String ACCOUNT_ID = "account_id";
  private static final String VIEW_ID = "view_id";
  private static final String CUSTOM_FIELD_NAME = "custom_field";
  private static final String VIEW_FIELD = "projectId";
  private static final String UUID = "uuid";

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void shouldThrowExceptionWhileSavingCustomField() {
    doReturn(viewCustomField()).when(viewCustomFieldDao).findByName(ACCOUNT_ID, VIEW_ID, CUSTOM_FIELD_NAME);
    assertThatExceptionOfType(InvalidRequestException.class)
        .isThrownBy(() -> viewCustomFieldService.save(viewCustomField(), bigQuery, "tableName"));
  }

  private ViewCustomField viewCustomField() {
    return ViewCustomField.builder()
        .uuid(UUID)
        .accountId(ACCOUNT_ID)
        .viewId(VIEW_ID)
        .name(CUSTOM_FIELD_NAME)
        .viewFields(singletonList(
            ViewField.builder().fieldId(VIEW_FIELD).fieldName(VIEW_FIELD).identifier(ViewFieldIdentifier.GCP).build()))
        .build();
  }
}
