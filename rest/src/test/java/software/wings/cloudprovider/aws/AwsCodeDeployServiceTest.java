package software.wings.cloudprovider.aws;

import static software.wings.beans.AwsConfig.Builder.anAwsConfig;

import com.amazonaws.regions.Regions;
import org.junit.Ignore;
import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.beans.SettingAttribute;

import javax.inject.Inject;

/**
 * Created by anubhaw on 6/22/17.
 */
@Ignore
public class AwsCodeDeployServiceTest extends WingsBaseTest {
  @Inject private AwsCodeDeployService awsCodeDeployService;

  @Test
  public void shouldListApplication() {
    SettingAttribute cloudProvider =
        SettingAttribute.Builder.aSettingAttribute()
            .withValue(anAwsConfig()
                           .withAccessKey("AKIAJLEKM45P4PO5QUFQ")
                           .withSecretKey("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".toCharArray())
                           .build())
            .build();
    awsCodeDeployService.listApplications(Regions.US_EAST_1.getName(), cloudProvider).forEach(application -> {
      System.out.println(application.toString());
    });

    awsCodeDeployService.listDeploymentGroup(Regions.US_EAST_1.getName(), "SrinivasApplication", cloudProvider)
        .forEach(dg -> { System.out.println(dg.toString()); });

    awsCodeDeployService.listDeploymentConfiguration(Regions.US_EAST_1.getName(), cloudProvider).forEach(dc -> {
      System.out.println(dc.toString());
    });
  }

  //    CreateDeploymentResult srinivasApplication = codeDeployClient
  //        .createDeployment(new
  //        CreateDeploymentRequest().withApplicationName("SrinivasApplication").withDeploymentGroupName("SrinivasDemoFleet"));
  //    System.out.println(srinivasApplication.toString());
  //  }
}
