package software.wings.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.google.inject.Inject;

import io.harness.exception.WingsException;
import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.service.intfc.DownloadTokenService;

/**
 * Created by peeyushaggarwal on 12/13/16.
 */
public class DownloadTokenServiceTest extends WingsBaseTest {
  @Inject private DownloadTokenService downloadTokenService;

  @Test
  public void shouldCreateToken() {
    String token = downloadTokenService.createDownloadToken("resource");
    assertThat(token).isNotEmpty();
  }

  @Test
  public void shouldValidateToken() {
    String token = downloadTokenService.createDownloadToken("resource");
    downloadTokenService.validateDownloadToken("resource", token);
  }

  @Test
  public void shouldThrowExceptionWhenNoTokenFoundOnValidation() {
    assertThatExceptionOfType(WingsException.class)
        .isThrownBy(() -> downloadTokenService.validateDownloadToken("resource", "token"));
  }

  @Test
  public void shouldThrowExceptionWhenResourceDoesntMatchOnValiation() {
    String token = downloadTokenService.createDownloadToken("resource");
    assertThatExceptionOfType(WingsException.class)
        .isThrownBy(() -> downloadTokenService.validateDownloadToken("resource1", "token"));
  }
}
