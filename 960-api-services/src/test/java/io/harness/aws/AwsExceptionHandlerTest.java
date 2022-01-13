/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.aws;

import static io.harness.rule.OwnerRule.ABHINAV;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.codedeploy.model.AmazonCodeDeployException;
import com.amazonaws.services.ec2.model.AmazonEC2Exception;
import com.amazonaws.services.ecs.model.AmazonECSException;
import com.amazonaws.services.ecs.model.ClusterNotFoundException;
import com.amazonaws.services.ecs.model.ServiceNotFoundException;
import com.amazonaws.services.lambda.model.AWSLambdaException;
import com.google.inject.Inject;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.MockitoAnnotations;

public class AwsExceptionHandlerTest extends CategoryTest {
  @Inject AwsExceptionHandler delegateBase;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testHandleAmazonClientException() {
    AmazonClientException exception = new AmazonClientException("Error Message");
    assertThatThrownBy(() -> delegateBase.handleAmazonClientException(exception))
        .isInstanceOf(InvalidRequestException.class);
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testHandleAmazonServiceException() {
    AmazonServiceException exception1 = new AmazonCodeDeployException("Error Message");
    assertThatThrownBy(() -> delegateBase.handleAmazonServiceException(exception1))
        .isInstanceOf(InvalidRequestException.class);
    AmazonServiceException exception2 = new AmazonEC2Exception("Error Message");
    assertThatThrownBy(() -> delegateBase.handleAmazonServiceException(exception2))
        .isInstanceOf(InvalidRequestException.class);
    AmazonServiceException exception3 = new ClusterNotFoundException("Error Message");
    assertThatThrownBy(() -> delegateBase.handleAmazonServiceException(exception3))
        .isInstanceOf(InvalidRequestException.class);
    AmazonServiceException exception4 = new ServiceNotFoundException("Error Message");
    assertThatThrownBy(() -> delegateBase.handleAmazonServiceException(exception4))
        .isInstanceOf(InvalidRequestException.class);
    AmazonServiceException exception5 = new AmazonECSException("Error Message");
    assertThatThrownBy(() -> delegateBase.handleAmazonServiceException(exception5))
        .isInstanceOf(InvalidRequestException.class);
    AmazonServiceException exception6 = new AWSLambdaException("Error Message");
    assertThatThrownBy(() -> delegateBase.handleAmazonServiceException(exception6))
        .isInstanceOf(InvalidRequestException.class);
  }
}
