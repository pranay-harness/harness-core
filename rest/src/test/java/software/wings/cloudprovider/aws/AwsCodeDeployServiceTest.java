package software.wings.cloudprovider.aws;

import static software.wings.beans.AwsConfig.Builder.anAwsConfig;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.codedeploy.model.CreateDeploymentRequest;
import com.amazonaws.services.codedeploy.model.RevisionLocation;
import com.amazonaws.services.codedeploy.model.S3Location;
import org.junit.Ignore;
import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.beans.SettingAttribute;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.cloudprovider.CodeDeployDeploymentInfo;
import software.wings.service.impl.AwsHelperService;

import javax.inject.Inject;

/**
 * Created by anubhaw on 6/22/17.
 */
@Ignore
public class AwsCodeDeployServiceTest extends WingsBaseTest {
  @Inject private AwsCodeDeployService awsCodeDeployService;
  @Inject private AwsHelperService awsHelperService;

  SettingAttribute cloudProvider =
      SettingAttribute.Builder.aSettingAttribute()
          .withValue(anAwsConfig()
                         .withAccessKey("AKIAJLEKM45P4PO5QUFQ")
                         .withSecretKey("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".toCharArray())
                         .build())
          .build();

  @Test
  public void shouldListApplication() {
    awsCodeDeployService.listApplications(Regions.US_EAST_1.getName(), cloudProvider).forEach(application -> {
      System.out.println(application.toString());
    });

    awsCodeDeployService.listDeploymentGroup(Regions.US_EAST_1.getName(), "todolistwar", cloudProvider).forEach(dg -> {
      System.out.println(dg.toString());
    });

    awsCodeDeployService.listDeploymentConfiguration(Regions.US_EAST_1.getName(), cloudProvider).forEach(dc -> {
      System.out.println(dc.toString());
    });

    CreateDeploymentRequest createDeploymentRequest =
        new CreateDeploymentRequest()
            .withApplicationName("todolistwar")
            .withDeploymentGroupName("todolistwarDG")
            .withDeploymentConfigName("CodeDeployDefault.OneAtATime")
            .withRevision(new RevisionLocation().withRevisionType("S3").withS3Location(
                new S3Location()
                    .withBucket("harnessapps")
                    .withBundleType("zip")
                    .withKey("todolist_war/19/codedeploysample.zip")));
    CodeDeployDeploymentInfo codeDeployDeploymentInfo = awsCodeDeployService.deployApplication(
        Regions.US_EAST_1.getName(), cloudProvider, createDeploymentRequest, new ExecutionLogCallback());
    System.out.println(codeDeployDeploymentInfo);
  }

  @Test
  public void shouldListApplicationRevisions() {
    System.out.println(awsCodeDeployService.getApplicationRevisionList(
        Regions.US_EAST_1.getName(), "todolistwar", "todolistwarDG", cloudProvider));
  }

  //    CreateDeploymentResult srinivasApplication = codeDeployClient
  //        .createDeployment(new
  //        CreateDeploymentRequest().withApplicationName("SrinivasApplication").withDeploymentGroupName("SrinivasDemoFleet"));
  //    System.out.println(srinivasApplication.toString());
  //  }
}
