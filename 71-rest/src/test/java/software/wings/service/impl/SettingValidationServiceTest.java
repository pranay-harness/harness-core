package software.wings.service.impl;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static software.wings.beans.HostConnectionAttributes.AuthenticationScheme.SSH_KEY;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;

import com.google.inject.Inject;

import io.harness.data.structure.UUIDGenerator;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mongodb.morphia.query.Query;
import software.wings.WingsBaseTest;
import software.wings.beans.ElkConfig;
import software.wings.beans.HostConnectionAttributes;
import software.wings.beans.HostConnectionAttributes.AccessType;
import software.wings.beans.HostConnectionAttributes.ConnectionType;
import software.wings.beans.SettingAttribute;
import software.wings.dl.WingsPersistence;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.analysis.ElkConnector;
import software.wings.service.intfc.elk.ElkAnalysisService;

import java.io.IOException;

/**
 * Created by Pranjal on 09/14/2018
 */
public class SettingValidationServiceTest extends WingsBaseTest {
  @Inject private SettingValidationService settingValidationService;
  @Mock private WingsPersistence wingsPersistence;
  @Mock private ElkAnalysisService elkAnalysisService;
  private Query<SettingAttribute> spyQuery;

  @Rule public ExpectedException thrown = ExpectedException.none();

  @Test
  public void testElkValidate() throws IOException {
    final String url = "https://ec2-34-207-78-53.compute-1.amazonaws.com:9200/";
    final String userName = "username";
    final String password = "password";

    when(wingsPersistence.createQuery(eq(SettingAttribute.class))).thenReturn(spyQuery);
    when(elkAnalysisService.getVersion(anyString(), any(ElkConfig.class), anyListOf(EncryptedDataDetail.class)))
        .thenThrow(IOException.class);

    ElkConfig elkConfig = new ElkConfig();
    elkConfig.setAccountId(ACCOUNT_ID);
    elkConfig.setElkConnector(ElkConnector.KIBANA_SERVER);
    elkConfig.setElkUrl(url);
    elkConfig.setUsername(userName);
    elkConfig.setPassword(password.toCharArray());

    SettingAttribute attribute = new SettingAttribute();
    attribute.setValue(elkConfig);
    thrown.expect(WingsException.class);
    settingValidationService.validate(attribute);
  }

  @Test
  public void testHostConnectionValidation() {
    HostConnectionAttributes.Builder hostConnectionAttributes =
        HostConnectionAttributes.Builder.aHostConnectionAttributes()
            .withAccessType(AccessType.KEY)
            .withAccountId(UUIDGenerator.generateUuid())
            .withConnectionType(ConnectionType.SSH)
            .withKeyless(false)
            .withUserName("TestUser")
            .withAuthenticationScheme(SSH_KEY);

    SettingAttribute attribute = new SettingAttribute();
    attribute.setValue(hostConnectionAttributes.build());

    thrown.expect(InvalidRequestException.class);
    settingValidationService.validate(attribute);

    hostConnectionAttributes.withKey("Test Private Key".toCharArray());
    attribute.setValue(hostConnectionAttributes.build());

    thrown = ExpectedException.none();
    settingValidationService.validate(attribute);
  }
}
