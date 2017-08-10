package software.wings.service.impl.expression;

import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sgurubelli on 8/7/17.
 */
public abstract class ExpressionBuilder {
  protected static final String APP_NAME = "app.name";
  protected static final String APP_DESCRIPTION = "app.description";

  protected static final String ARTIFACT_NAME = "artifact.name";
  protected static final String ARTIFACT_BUILDNO = "artifact.buildNo";
  protected static final String ARTIFACT_REVISION = "artifact.revision";

  protected static final String ENV_NAME = "env.name";
  protected static final String ENV_DESCRIPTION = "env.description";

  protected static final String WORKFLOW_NAME = "workflow.name";
  protected static final String WORKFLOW_DESCRIPTION = "workflow.description";

  protected static final String INSTANCE_NAME = "instance.name";
  protected static final String INSTANCE_HOSTNAME = "instance.hostName";
  protected static final String INSTANCE_HOST_PUBLICDNS = "instance.host.publicDns";

  protected static final String HOST_NAME = "host.name";
  protected static final String HOST_HOSTNAME = "host.hostName";
  protected static final String HOST_PUBLICDNS = "host.publicDns";
  protected static final String HOST_INSTANCE_ID = "host.ec2Instance.instanceId";
  protected static final String HOST_INSTANCE_TYPE = "host.ec2Instance.instanceType";
  protected static final String HOST_INSTANCE_IMAGEID = "host.ec2Instance.imageId";
  protected static final String HOST_INSTANCE_ARCH = "host.ec2Instance.architecture";
  protected static final String HOST_INSTANCE_KERNELID = "host.ec2Instance.kernelId";
  protected static final String HOST_INSTANCE_KEY_NAME = "host.ec2Instance.keyName";
  protected static final String HOST_INSTANCE_PVTDNS = "host.ec2Instance.privateDnsName";
  protected static final String HOST_INSTANCE_PRIVATEIP = "host.ec2Instance.privateIpAddress";
  protected static final String HOST_INSTANCE_PUBLICDNS = "host.ec2Instance.publicDnsName";
  protected static final String HOST_INSTANCE_PUBLICIP = "host.ec2Instance.publicIpAddress";
  protected static final String HOST_INSTANCE_SUBNETID = "host.ec2Instance.subnetId";
  protected static final String HOST_INSTANCE_VPCID = "host.ec2Instance.vpcId";

  public List<String> getExpressions(String appId, String serviceId, String entityId) {
    return getExpressions(appId, entityId);
  }

  public abstract List<String> getExpressions(String appId, String entityId);

  public abstract List<String> getDynamicExpressions(String appId, String entityId);

  List<String> getStaticExpressions() {
    List<String> expressions = new ArrayList<>();
    expressions.addAll(asList(APP_NAME, APP_DESCRIPTION));
    expressions.addAll(asList(ARTIFACT_NAME, ARTIFACT_BUILDNO, ARTIFACT_REVISION));
    expressions.addAll(asList(ENV_NAME, ENV_DESCRIPTION));
    expressions.addAll(asList(WORKFLOW_NAME, WORKFLOW_DESCRIPTION));

    expressions.addAll(asList(HOST_NAME, HOST_HOSTNAME, HOST_PUBLICDNS, HOST_INSTANCE_ID, HOST_INSTANCE_TYPE,
        HOST_INSTANCE_IMAGEID, HOST_INSTANCE_ARCH, HOST_INSTANCE_KERNELID, HOST_INSTANCE_KEY_NAME, HOST_INSTANCE_PVTDNS,
        HOST_INSTANCE_PRIVATEIP, HOST_INSTANCE_PUBLICDNS, HOST_INSTANCE_PUBLICIP, HOST_INSTANCE_SUBNETID,
        HOST_INSTANCE_VPCID));

    return expressions;
  }
}
