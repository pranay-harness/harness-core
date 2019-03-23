package io.harness.generator;

import static io.harness.govern.Switch.unhandled;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.apache.commons.io.IOUtils;
import software.wings.beans.DelegateProfile;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.DelegateProfileService;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

@Singleton
public class DelegateProfileGenerator {
  @Inject DelegateProfileService delegateProfileService;
  @Inject private WingsPersistence wingsPersistence;

  public enum DelegateProfiles { TERRAFORM }

  public DelegateProfile ensurePredefined(DelegateProfileGenerator.DelegateProfiles profile) throws IOException {
    switch (profile) {
      case TERRAFORM:
        return ensureTerraform();
      default:
        unhandled(profile);
    }
    return null;
  }

  private DelegateProfile ensureTerraform() throws IOException {
    DelegateProfile delegateProfile = DelegateProfile.builder().startupScript(getScript()).name("terraform").build();
    return ensurePredefined(delegateProfile);
  }

  public DelegateProfile exists(DelegateProfile profile) {
    return wingsPersistence.createQuery(DelegateProfile.class).filter("name", profile.getName()).get();
  }

  public DelegateProfile ensurePredefined(DelegateProfile profile) {
    DelegateProfile existing = exists(profile);
    if (existing != null) {
      return existing;
    }

    return GeneratorUtils.suppressDuplicateException(() -> delegateProfileService.add(profile), () -> exists(profile));
  }

  private String getScript() throws IOException {
    InputStream inputStream = getClass().getResourceAsStream("/script.properties");
    return IOUtils.toString(inputStream, Charset.forName("UTF-8"));
  }
}
