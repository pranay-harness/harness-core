package io.harness.batch.processing.cloudevents.aws.ecs.service.support.impl;

import static io.harness.rule.OwnerRule.HITESH;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import com.amazonaws.regions.Regions;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.beans.NameValuePair;
import software.wings.service.intfc.AwsHelperResourceService;

import java.util.List;

public class AwsHelperResourceServiceImplTest extends WingsBaseTest {
  @Inject private AwsHelperResourceService awsHelperResourceService;

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void shouldGetRegions() {
    List<NameValuePair> regions = awsHelperResourceService.getAwsRegions();
    assertThat(regions).isNotEmpty().extracting(NameValuePair::getName).contains(Regions.US_EAST_1.getName());
    assertThat(regions).extracting(NameValuePair::getName).doesNotContain(Regions.GovCloud.getName());
  }
}