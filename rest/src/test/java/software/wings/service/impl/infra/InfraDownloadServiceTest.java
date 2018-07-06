package software.wings.service.impl.infra;

import static junit.framework.TestCase.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.MockitoAnnotations.initMocks;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.Spy;
import software.wings.WingsBaseTest;

public class InfraDownloadServiceTest extends WingsBaseTest {
  @Spy @InjectMocks InfraDownloadServiceImpl infraDownloadService;

  @Before
  public void setUp() {
    initMocks(this);
  }

  @Test
  public void testInfraDownloadFailForEnvWhenNoServiceAccDefined() {
    try {
      String url = infraDownloadService.getDownloadUrlForDelegate("4333");
      Assertions.assertThat(url).isEqualTo(InfraDownloadServiceImpl.DEFAULT_ERROR_STRING);
    } catch (Exception e) {
      fail();
    }
  }

  @Test
  public void testDelegateDownload() {
    /***
     * THESE ARE FAKE KEYS
     */
    String prodTestKey = "{\n"
        + "  \"type\": \"service_account\",\n"
        + "  \"project_id\": \"prod-testaccount-fake-key-1234\",\n"
        + "  \"private_key_id\": \"4321\",\n"
        + "  \"private_key\": \"-----BEGIN PRIVATE KEY-----\\nMIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDFStBcZTmZPUmy\\nAceX3XQnoKBEQ8sK6PkUAyPvecnjl1wdrO5gtHKUlYOvgUB9ajt3pRtu7zjxj5Ey\\nPzQ3xGlE8Jefk3QFgWqhLgzcYK6Y58h+OfcnhE2lCvezCy+K4CF+bbMvkR+tx10Q\\nv7EZkZMSZWQ2q13sU45ppg4H5cncGIK2ScQC6ePmOM7UWnnnRajstQ8lT++1EUsy\\nGNhJmmYXFf28xMAburu/i1tk2LzETk3FCrVgJiZobL8wVhfIQIwwjGa6vRUVr40M\\nYFAmh9TW6lP7BvJ67wVSBmEfjRAD9WXK0C4U7NchZ7JMcBOHHykGFQAorRtIOKaA\\nP2ajVtgBAgMBAAECggEADyAvvk2yb0mwGzogs8dyLtGVR/aMnA8FfZ/KAwOq00yY\\n7tuBwR9Eg5i86lk1+QeKfJ9HbadgjcCMiLYoyjfAPF1Xuo/C/uMPqkDOEp1E9Ysb\\np3XJOZnenA8ts2fhoruRRdCiQrzVGW0l+PFBYizzgh8M4lYcEtDLI9Vr0PYwljOW\\nZ0l7BQ8stPgpekgTNolMjVh03mnA0BmxVYIu+tbIAcccuEt9ZFmwwv9yUtLsueQC\\n62vFrBnG2uF1H90ENxghYa5K07f1tfRqJndWR3FXsWjpJ+u8hbsPOHh/ft3yJVGn\\ntHHNL3nZfgJvVXt8k181qjurCbeoTdb2M+F7YcYmKwKBgQDjxKGdoq2h5pJd3fyF\\ncpt7gPBMgnMeoO/7/RRCYoYKnI1iT+c09lBEA21jr9zHFn54QooobTVhoB6b+D51\\nbyqmNkADW36wHL6OOyHiXGuck3vU66v/9m3BU6LlLbnKd6FRpwoFyXefCMCQRIoD\\n3Kk2UnSgQYi7WoDTGyY6XPVxawKBgQDdvyU+Ku5xbRKcjaLKAic7BHDcESnxqzkb\\nR0hgJh4FMPSLqS4PoAyWA+U+VfmC8rxN4ibSgK/cNinpX8aEaLWuCzsDgifTt6Ri\\n3UtVEHdp+mOW8FT9MYCuoZjOL3PkWM1j5b3fEKJJTJm3Kq5yKPs7djBYswKK58TQ\\n4YJ5UwO7QwKBgBbyucHTISFdxMN2WTnsySORYySiRA0C9Ar0fbUOjijFiy0rlcTt\\nSDsCCWRw1JKufoGJWrgmAKncYxkd0tUsJSTn9F0iO8psqvpTlN9kpmb5KRHTyNma\\nL4a58YCbsctncDZ02PqqSMYaMKSYczteYSIa/qsizy7pOG+MnbZlpUkPAoGBAIPk\\nvr00gfEbZFeT80DeeQEkihCLcPxa0LPF+WmfInoJ/VrYvRn0I6hTFta2Apv9zz0w\\nB9FrU67S1KkIG6cENRQZf/d9Qj0u3OslHzqweaailPvhZVvYRucYHTB+jxtCKqCB\\ntSbp2O1qT3/gNjSW5aAfk3AewaNnaeyoRZfuZajLAoGBAOKqY3DWCSQgd0qjszf4\\njXBWePyT/B0VtoYYeJByL/78jajweapigrnvq9cDq0t4EAIzF3SaUyEM2HIzvB1O\\nULw6bYkuqc+1YgI7CqNFdcWYkXkgeNnGMquL6gIwXY4EDvvoNqHXWbncqZtqV+cO\\nUR4xQtsfixTkAP50MLMHnHPU\\n-----END PRIVATE KEY-----\\n\",\n"
        + "  \"client_email\": \"testserviceaccount@prod-testaccount-fake-key-1234.iam.gserviceaccount.com\",\n"
        + "  \"client_id\": \"106843626166628319689\",\n"
        + "  \"auth_uri\": \"https://accounts.google.com/o/oauth2/auth\",\n"
        + "  \"token_uri\": \"https://accounts.google.com/o/oauth2/token\",\n"
        + "  \"auth_provider_x509_cert_url\": \"https://www.googleapis.com/oauth2/v1/certs\",\n"
        + "  \"client_x509_cert_url\": \"https://www.googleapis.com/robot/v1/metadata/x509/testserviceaccount%40prod-testaccount-fake-key-1234.iam.gserviceaccount.com\"\n"
        + "}\n";

    /***
     * THESE ARE FAKE KEYS
     */
    String qaTestKey = "{\n"
        + "  \"type\": \"service_account\",\n"
        + "  \"project_id\": \"qa-testaccount-fake-key-1234\",\n"
        + "  \"private_key_id\": \"4321\",\n"
        + "  \"private_key\": \"-----BEGIN PRIVATE KEY-----\\nMIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDFStBcZTmZPUmy\\nAceX3XQnoKBEQ8sK6PkUAyPvecnjl1wdrO5gtHKUlYOvgUB9ajt3pRtu7zjxj5Ey\\nPzQ3xGlE8Jefk3QFgWqhLgzcYK6Y58h+OfcnhE2lCvezCy+K4CF+bbMvkR+tx10Q\\nv7EZkZMSZWQ2q13sU45ppg4H5cncGIK2ScQC6ePmOM7UWnnnRajstQ8lT++1EUsy\\nGNhJmmYXFf28xMAburu/i1tk2LzETk3FCrVgJiZobL8wVhfIQIwwjGa6vRUVr40M\\nYFAmh9TW6lP7BvJ67wVSBmEfjRAD9WXK0C4U7NchZ7JMcBOHHykGFQAorRtIOKaA\\nP2ajVtgBAgMBAAECggEADyAvvk2yb0mwGzogs8dyLtGVR/aMnA8FfZ/KAwOq00yY\\n7tuBwR9Eg5i86lk1+QeKfJ9HbadgjcCMiLYoyjfAPF1Xuo/C/uMPqkDOEp1E9Ysb\\np3XJOZnenA8ts2fhoruRRdCiQrzVGW0l+PFBYizzgh8M4lYcEtDLI9Vr0PYwljOW\\nZ0l7BQ8stPgpekgTNolMjVh03mnA0BmxVYIu+tbIAcccuEt9ZFmwwv9yUtLsueQC\\n62vFrBnG2uF1H90ENxghYa5K07f1tfRqJndWR3FXsWjpJ+u8hbsPOHh/ft3yJVGn\\ntHHNL3nZfgJvVXt8k181qjurCbeoTdb2M+F7YcYmKwKBgQDjxKGdoq2h5pJd3fyF\\ncpt7gPBMgnMeoO/7/RRCYoYKnI1iT+c09lBEA21jr9zHFn54QooobTVhoB6b+D51\\nbyqmNkADW36wHL6OOyHiXGuck3vU66v/9m3BU6LlLbnKd6FRpwoFyXefCMCQRIoD\\n3Kk2UnSgQYi7WoDTGyY6XPVxawKBgQDdvyU+Ku5xbRKcjaLKAic7BHDcESnxqzkb\\nR0hgJh4FMPSLqS4PoAyWA+U+VfmC8rxN4ibSgK/cNinpX8aEaLWuCzsDgifTt6Ri\\n3UtVEHdp+mOW8FT9MYCuoZjOL3PkWM1j5b3fEKJJTJm3Kq5yKPs7djBYswKK58TQ\\n4YJ5UwO7QwKBgBbyucHTISFdxMN2WTnsySORYySiRA0C9Ar0fbUOjijFiy0rlcTt\\nSDsCCWRw1JKufoGJWrgmAKncYxkd0tUsJSTn9F0iO8psqvpTlN9kpmb5KRHTyNma\\nL4a58YCbsctncDZ02PqqSMYaMKSYczteYSIa/qsizy7pOG+MnbZlpUkPAoGBAIPk\\nvr00gfEbZFeT80DeeQEkihCLcPxa0LPF+WmfInoJ/VrYvRn0I6hTFta2Apv9zz0w\\nB9FrU67S1KkIG6cENRQZf/d9Qj0u3OslHzqweaailPvhZVvYRucYHTB+jxtCKqCB\\ntSbp2O1qT3/gNjSW5aAfk3AewaNnaeyoRZfuZajLAoGBAOKqY3DWCSQgd0qjszf4\\njXBWePyT/B0VtoYYeJByL/78jajweapigrnvq9cDq0t4EAIzF3SaUyEM2HIzvB1O\\nULw6bYkuqc+1YgI7CqNFdcWYkXkgeNnGMquL6gIwXY4EDvvoNqHXWbncqZtqV+cO\\nUR4xQtsfixTkAP50MLMHnHPU\\n-----END PRIVATE KEY-----\\n\",\n"
        + "  \"client_email\": \"testserviceaccount@qa-testaccount-fake-key-1234.iam.gserviceaccount.com\",\n"
        + "  \"client_id\": \"106843626166628319689\",\n"
        + "  \"auth_uri\": \"https://accounts.google.com/o/oauth2/auth\",\n"
        + "  \"token_uri\": \"https://accounts.google.com/o/oauth2/token\",\n"
        + "  \"auth_provider_x509_cert_url\": \"https://www.googleapis.com/oauth2/v1/certs\",\n"
        + "  \"client_x509_cert_url\": \"https://www.googleapis.com/robot/v1/metadata/x509/testserviceaccount%40qa-testaccount-fake-key-1234.iam.gserviceaccount.com\"\n"
        + "}\n";

    try {
      doReturn(prodTestKey).when(infraDownloadService).getServiceAccountJson(Mockito.anyString());
      doReturn(HarnessEnv.PROD).when(infraDownloadService).getEnv();
      checkProdBucket();
      doReturn(HarnessEnv.ON_PREM).when(infraDownloadService).getEnv();
      checkProdBucket();
      doReturn(HarnessEnv.STAGE).when(infraDownloadService).getEnv();
      checkProdBucket();

      doReturn(HarnessEnv.QA).when(infraDownloadService).getEnv();
      checkQABucket();
      doReturn(HarnessEnv.CI).when(infraDownloadService).getEnv();
      checkQABucket();

    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  private void checkProdBucket() {
    String url = infraDownloadService.getDownloadUrlForDelegate("5139");
    Assertions.assertThat(url.contains("prod"));
    Assertions.assertThat(!url.contains("qa"));
    url = infraDownloadService.getDownloadUrlForWatcher("5139");
    Assertions.assertThat(url.contains("prod"));
    Assertions.assertThat(!url.contains("qa"));
  }

  private void checkQABucket() {
    String url = infraDownloadService.getDownloadUrlForDelegate("5139");
    Assertions.assertThat(!url.contains("prod"));
    Assertions.assertThat(url.contains("qa"));
    url = infraDownloadService.getDownloadUrlForWatcher("5139");
    Assertions.assertThat(!url.contains("prod"));
    Assertions.assertThat(url.contains("qa"));
  }
}
