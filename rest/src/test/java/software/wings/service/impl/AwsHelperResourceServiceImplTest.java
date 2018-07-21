package software.wings.service.impl;

import com.google.inject.Inject;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.service.intfc.AwsHelperResourceService;

import java.util.Map;

/**
 * Created by sgurubelli on 7/20/18.
 */
public class AwsHelperResourceServiceImplTest extends WingsBaseTest {
  @Inject private AwsHelperResourceService awsHelperResourceService;

  @Test
  public void shouldGetRegions() {
    Map<String, String> regions = awsHelperResourceService.getRegions();
    Assertions.assertThat(regions).isNotEmpty();
  }
}