package software.wings.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import com.amazonaws.regions.Regions;
import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.beans.NameValuePair;
import software.wings.service.intfc.AwsHelperResourceService;

import java.util.List;

/**
 * Created by sgurubelli on 7/20/18.
 */
public class AwsHelperResourceServiceImplTest extends WingsBaseTest {
  @Inject private AwsHelperResourceService awsHelperResourceService;

  @Test
  public void shouldGetRegions() {
    List<NameValuePair> regions = awsHelperResourceService.getAwsRegions();
    assertThat(regions).isNotEmpty().extracting(NameValuePair::getName).contains(Regions.US_EAST_1.getName());
    assertThat(regions).extracting(NameValuePair::getName).doesNotContain(Regions.GovCloud.getName());
  }
}