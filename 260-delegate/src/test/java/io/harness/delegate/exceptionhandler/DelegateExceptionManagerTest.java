package io.harness.delegate.exceptionhandler;

import static io.harness.rule.OwnerRule.MOHIT_GARG;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.DelegateTestBase;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.rule.Owner;

import com.amazonaws.services.codedeploy.model.AmazonCodeDeployException;
import com.google.inject.Inject;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.MockitoAnnotations;

public class DelegateExceptionManagerTest extends DelegateTestBase {
  @Inject DelegateExceptionManager delegateExceptionManager;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = MOHIT_GARG)
  @Category(UnitTests.class)
  public void testBasicSanitCaseForSingleException() {
    AmazonCodeDeployException amazonServiceException = new AmazonCodeDeployException("Amazon Code deploy exception");
    DelegateResponseData delegateResponseData =
        delegateExceptionManager.getResponseData(amazonServiceException, null, true);
    assertThat(delegateResponseData).isNotNull();

    ErrorNotifyResponseData errorNotifyResponseData = (ErrorNotifyResponseData) delegateResponseData;
    assertThat(errorNotifyResponseData.getException()).isNotNull();
  }
}