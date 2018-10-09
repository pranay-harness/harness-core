package software.wings.helpers.ext.docker;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import software.wings.WingsBaseTest;

public class DockerServiceTest extends WingsBaseTest {
  @Test
  public void shouldParseLink() {
    String link =
        "</v2/myAccount/myfirstrepo/tags/list?next_page=gAAAAABbuZsLNl9W6tAycol_oLvcYeti2w53XnoV3FYyFBkd-TQV3OBiWNJLqp2m8isy3SWusAqA4Y32dHJ7tGi0br18kXEt6nTW306QUFexaXrAGq8KeSc%3D&n=25>; rel=\"next\"";
    String parsedLink = DockerRegistryServiceImpl.parseLink(link);
    assertThat(parsedLink).isNotEmpty();
    assertThat(parsedLink)
        .isEqualTo(
            "v2/myAccount/myfirstrepo/tags/list?next_page=gAAAAABbuZsLNl9W6tAycol_oLvcYeti2w53XnoV3FYyFBkd-TQV3OBiWNJLqp2m8isy3SWusAqA4Y32dHJ7tGi0br18kXEt6nTW306QUFexaXrAGq8KeSc%3D&n=25");
  }
}
