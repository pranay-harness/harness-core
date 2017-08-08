package software.wings.service.impl.expression;

import static com.google.common.truth.Truth.assertThat;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.APP_NAME;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;

import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import software.wings.WingsBaseTest;
import software.wings.beans.Application;
import software.wings.beans.EntityType;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.expression.ExpressionBuilderService;

import java.util.List;
import javax.inject.Inject;

/**
 * Created by sgurubelli on 8/8/17.
 */
public class ExpressionBuilderServiceTest extends WingsBaseTest {
  @Mock private AppService appService;

  @Inject @InjectMocks private ExpressionBuilderService builderService;

  @Test
  public void shouldGetServiceExpressions() {
    Mockito.when(appService.get(APP_ID)).thenReturn(Application.Builder.anApplication().withName(APP_NAME).build());
    List<String> expressions = builderService.listExpressions(APP_ID, SERVICE_ID, EntityType.SERVICE);
    assertThat(expressions).isNotNull();
    assertThat(expressions.contains("service.name"));
  }
}
