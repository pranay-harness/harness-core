package io.harness.delegate.exceptionhandler;

import static io.harness.rule.OwnerRule.MOHIT_GARG;

import io.harness.DelegateTestBase;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.DelegateResponseData;
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
  public void testMapAwsConfig() {
    AmazonCodeDeployException amazonServiceException = new AmazonCodeDeployException("Amazon Code deploy exception");
    DelegateResponseData delegateResponseData =
        delegateExceptionManager.getResponseData(amazonServiceException, null, true);
  }
}