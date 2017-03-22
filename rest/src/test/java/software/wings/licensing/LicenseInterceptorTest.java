package software.wings.licensing;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import org.junit.Test;
import software.wings.WingsBaseTest;

/**
 * Created by peeyushaggarwal on 3/22/17.
 */
public class LicenseInterceptorTest extends WingsBaseTest {
  @Inject private LicenseManager licenseManager;

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
  public void shouldInterceptMethodCallExtendedWithLicensing() throws Exception {
    when(licenseManager.isAllowed(eq("ACCOUNT_ID"), eq("licensedMethod"))).thenReturn(true);
    methodLicensedObject.licensedMethod("ACCOUNT_ID");
    verify(licenseManager).isAllowed("ACCOUNT_ID", "licensedMethod");
  }

  @Test
  public void shouldNotInterceptMethodCallNotExtendedWithLicensing() throws Exception {
    methodLicensedObject.anotherMethod();
    verifyZeroInteractions(licenseManager);
  }

  @Test
  public void shouldNotInterceptMethodCallMissingLicenseKeyWithLicensing() throws Exception {
    methodLicensedObject.licensedMethodWithoutKey("ACCOUNT_ID");
    verifyZeroInteractions(licenseManager);
  }

  @Test
  public void shouldInterceptAllCallsFromClassAnnotatedWithLicenseAnnotation() throws Exception {
    when(licenseManager.isAllowed(eq("ACCOUNT_ID"), anyString())).thenReturn(true);

    classLicensedObject.method("ACCOUNT_ID");
    verify(licenseManager).isAllowed("ACCOUNT_ID", "method");

    classLicensedObject.anotherMethod("ACCOUNT_ID");
    verify(licenseManager).isAllowed("ACCOUNT_ID", "anotherMethod");
  }
}
