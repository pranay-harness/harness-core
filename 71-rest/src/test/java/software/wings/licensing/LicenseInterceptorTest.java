package software.wings.licensing;

import static io.harness.rule.OwnerRule.UNKNOWN;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;

/**
 * Created by peeyushaggarwal on 3/22/17.
 */
public class LicenseInterceptorTest extends WingsBaseTest {
  @Mock private LicenseService licenseManager;

  @InjectMocks @Inject private LicenseInterceptor licenseInterceptor;

  @Licensed
  public static class ClassLicensedObject {
    public void method(@LicenseKey String accountId) {}

    public void anotherMethod(@LicenseKey String accountId) {}
  }

  public static class MethodLicensedObject {
    @Licensed
    public void licensedMethod(@LicenseKey String accountId) {}

    @Licensed
    public void licensedMethodWithoutKey(String accountId) {}

    public void anotherMethod() {}
  }

  @Inject private ClassLicensedObject classLicensedObject;

  @Inject private MethodLicensedObject methodLicensedObject;

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void shouldInterceptMethodCallExtendedWithLicensing() throws Exception {
    methodLicensedObject.licensedMethod("ACCOUNT_ID");
    verify(licenseManager).validateLicense("ACCOUNT_ID", "licensedMethod");
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void shouldNotInterceptMethodCallNotExtendedWithLicensing() throws Exception {
    methodLicensedObject.anotherMethod();
    verifyZeroInteractions(licenseManager);
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void shouldNotInterceptMethodCallMissingLicenseKeyWithLicensing() throws Exception {
    methodLicensedObject.licensedMethodWithoutKey("ACCOUNT_ID");
    verifyZeroInteractions(licenseManager);
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void shouldInterceptAllCallsFromClassAnnotatedWithLicenseAnnotation() throws Exception {
    classLicensedObject.method("ACCOUNT_ID");
    verify(licenseManager).validateLicense("ACCOUNT_ID", "method");

    classLicensedObject.anotherMethod("ACCOUNT_ID");
    verify(licenseManager).validateLicense("ACCOUNT_ID", "anotherMethod");
  }
}
