/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.integration;

import io.harness.CategoryTest;

import software.wings.rules.Integration;

import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.extern.slf4j.Slf4j;

@Integration
@Slf4j
public abstract class KubernetesCertificateIntegrationTestBase extends CategoryTest {
  public static void main(String[] args) {
    String caCertData = "LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSUMxekNDQWIrZ0F3SUJBZ0lRUmpMcStZa3hP\n"
        + "WHhmYVB5TlBWK0MwakFOQmdrcWhraUc5dzBCQVFzRkFEQVYKTVJNd0VRWURWUVFERXdwcmRXSmxj\n"
        + "bTVsZEdWek1CNFhEVEUzTURjd05ESXhOVGd5TWxvWERUSTNNRGN3TkRJeApOVGd5TWxvd0ZURVRN\n"
        + "QkVHQTFVRUF4TUthM1ZpWlhKdVpYUmxjekNDQVNJd0RRWUpLb1pJaHZjTkFRRUJCUUFECmdnRVBB\n"
        + "RENDQVFvQ2dnRUJBTkI3aDZrZitaWWNkODNGUFc1bUNLSGdFcEU2S2FQbUpIay91QnNhUnhZcjZ5\n"
        + "b3oKdE5IV1dpM25nU1EzZkRxLzZxTGhhZFQvMGVsTzdsdEFBM1lwNm5oOGRnWVpYUXVJYStRNGVH\n"
        + "U0FaamVvdnpwYQpONVRYT1BlUmduVCthbzBiS3JBblJFTUpHVU5yak56MFpqeldlUHZ0TStuZ05Y\n"
        + "ZlpCYTlXQ0dnRWZnZXVKQzd2CnJZM3EwQjY4U1Mrd0J3SE5VY1pHQUM2eEZwYVJ5R05UUndvOHR1\n"
        + "a2w2WG5LWFdCeWEwVFA1dFZhUUc4VEJ5ZUoKa2pSU2l4d3QvT21oVGlSaXhJbTBBWUtraHBuWUYv\n"
        + "K09yRDQ3Skk1dHBqc01jVUUvcU5hS2lKK2R3dzl0WjgvaAo3MTAzbUd1cC84M3NGTDlodStsWWZK\n"
        + "Ync2YkIxelpYeW50ZjBPdWNDQXdFQUFhTWpNQ0V3RGdZRFZSMFBBUUgvCkJBUURBZ0VHTUE4R0Ex\n"
        + "VWRFd0VCL3dRRk1BTUJBZjh3RFFZSktvWklodmNOQVFFTEJRQURnZ0VCQUpFV25sL0oKdjhucHFp\n"
        + "T005TVpGNFFwZGh1Mzl0NXl4ZVBFV0ZSRnJ3aGJoZDFnd3pkbER2YXFLZHRLTkpzcUs1NUVSRzYx\n"
        + "dwpvbkh1MU1WNTVpV2k4V1NMYmEwRHIyRTYzV2wzM0hhdVFtYURhOVBZQ3BoZ3ZuMjRhNGZZNGVB\n"
        + "ckZOcnJ0RDBICks3OWJaR3laelRLTVE5b29MNkxrdjAxMGN4dEtibXp0QTFUQkVSaS9NcWQvYlRo\n"
        + "V2JvMm9CK3haSTYvK1FpUTgKTlhTVE9LdFpsU3NSTStRT09Uci9ZSitxajZnMlZXWW1sTkZuV1RW\n"
        + "bDhwNUsvNk1zckw3RVhnS1NhcGpsVHRzawpZVnFHampodko3MXpNSFMwaWg4dU1xSmtIQWdwYVpS\n"
        + "bmdtcmVJVTM1bUxEb3NBOWxxQUJGVVRmMHZKTzhHOWRqCkpqQ0pvS0VmOW9KYk56bz0KLS0tLS1F\n"
        + "TkQgQ0VSVElGSUNBVEUtLS0tLQo=";
    String clientCertData = "LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSURvekNDQW91Z0F3SUJBZ0lNRk03Y24zamN2\n"
        + "bWFORjlvOE1BMEdDU3FHU0liM0RRRUJDd1VBTUJVeEV6QVIKQmdOVkJBTVRDbXQxWW1WeWJtVjBa\n"
        + "WE13SGhjTk1UY3dOekEwTWpFMU9ESXpXaGNOTWpjd056QTBNakUxT0RJegpXakFjTVJvd0dBWURW\n"
        + "UVFERXhGcmRXSmxjbTVsZEdWekxXMWhjM1JsY2pDQ0FTSXdEUVlKS29aSWh2Y05BUUVCCkJRQURn\n"
        + "Z0VQQURDQ0FRb0NnZ0VCQU5Dd2RFTWZtcExqdnpZdm85QVp6eStFNkFnK0YwMFZ6VkxCVkQvdUdG\n"
        + "eTAKTlZMb05BWm9TQWdVUHZ1aGJ1czdsb1ZsM1NIVy82UGk1eTA2RDE2bCtrQzRVM1FEZ2VNVUUw\n"
        + "ZXAvNGxZUCtCRwpoYVlMWUVGcExMaHRTUFRXRmNxQ2QzWGdMbG4xY0lUSDRoMjhFMWg0WDlHWG9B\n"
        + "ajBRZGR0SGFQWnBQTzhrbmJVClo2MitLL0JjWnBCQU5xMXZpTE9tYkJENEVKRVlnaGNZUGI4eFhz\n"
        + "NDlRUzErSVl2b3daUnVLOWx4bWZ5dzVBcHMKeExqVlBsb2c1bnM4ZzMySWJqK05jQTUvRlg2a3BN\n"
        + "aWE2M0lUeFlvWHFYU1JOTm4rRHdPSEpjSjE5ejJYaXFSRApoMTJ3UGFYMUk0Y25lazhUY3VWejkw\n"
        + "ellDV25lUnkzQ3gxMnpVYlhQY0hFQ0F3RUFBYU9CNnpDQjZEQU9CZ05WCkhROEJBZjhFQkFNQ0Jh\n"
        + "QXdFd1lEVlIwbEJBd3dDZ1lJS3dZQkJRVUhBd0V3REFZRFZSMFRBUUgvQkFJd0FEQ0IKc2dZRFZS\n"
        + "MFJCSUdxTUlHbmdpRmhjR2t1YVc1MFpYSnVZV3d1ZDJWemRDNXJkV0psTG1oaGNtNWxjM011YVcr\n"
        + "QwpHR0Z3YVM1M1pYTjBMbXQxWW1VdWFHRnlibVZ6Y3k1cGI0SUthM1ZpWlhKdVpYUmxjNElTYTNW\n"
        + "aVpYSnVaWFJsCmN5NWtaV1poZFd4MGdoWnJkV0psY201bGRHVnpMbVJsWm1GMWJIUXVjM1pqZ2lS\n"
        + "cmRXSmxjbTVsZEdWekxtUmwKWm1GMWJIUXVjM1pqTG1Oc2RYTjBaWEl1Ykc5allXeUhCR1JBQUFH\n"
        + "SEJIOEFBQUV3RFFZSktvWklodmNOQVFFTApCUUFEZ2dFQkFKWW1SamE5TzByT1JtVDduSXNwMTV1\n"
        + "L0pwV0hCcUhDbGo5TlBWWXVJcE9HejlUVUMySkswamt5Cm9pV0Y3QnVVcFJINFg5MFAyM0ZwcWJ6\n"
        + "dml6SFBtMTluU3hBVGthWnl3YTk2WGEzbE5mSXVGUHNoek9iSkpXZ2UKb1dwbEZ4R3plTXF0RDds\n"
        + "ZjNTbFBETHErR2U0SzJ1aytrVHh3cmRvS25VZkdIb3N3dHRKVUhuQmRRVWs3ZDRUbgpBSzNWT1JV\n"
        + "aVlSUnNhbHoxdUlzTDRqeEtGMzJRUWkySlg1M2tJOHhBSUVrTG1pZE1aTHVENnMxUHIzQWFyREd6\n"
        + "CmVGZncwSVFINVFTbFAzNU9yanYxY09PZWt6eHpIWGpCRDNDZFEzNFhXRHRZUXpwVkg2TkFZZ2Fo\n"
        + "SDBBWXUvb0IKd0h5UzhGc1hKMWsrQ3ozcWcyYXcvTWxoclpIY2h0cz0KLS0tLS1FTkQgQ0VSVElG\n"
        + "SUNBVEUtLS0tLQo=";
    String clientKeyData = "LS0tLS1CRUdJTiBSU0EgUFJJVkFURSBLRVktLS0tLQpNSUlFcFFJQkFBS0NBUUVBMExCMFF4K2Fr\n"
        + "dU8vTmkrajBCblBMNFRvQ0Q0WFRSWE5Vc0ZVUCs0WVhMUTFVdWcwCkJtaElDQlErKzZGdTZ6dVdo\n"
        + "V1hkSWRiL28rTG5MVG9QWHFYNlFMaFRkQU9CNHhRVFI2bi9pVmcvNEVhRnBndGcKUVdrc3VHMUk5\n"
        + "TllWeW9KM2RlQXVXZlZ3aE1maUhid1RXSGhmMFplZ0NQUkIxMjBkbzltazg3eVNkdFJucmI0cgo4\n"
        + "Rnhta0VBMnJXK0lzNlpzRVBnUWtSaUNGeGc5dnpGZXpqMUJMWDRoaStqQmxHNHIyWEdaL0xEa0Nt\n"
        + "ekV1TlUrCldpRG1lenlEZllodVA0MXdEbjhWZnFTa3lKcnJjaFBGaWhlcGRKRTAyZjRQQTRjbHdu\n"
        + "WDNQWmVLcEVPSFhiQTkKcGZVamh5ZDZUeE55NVhQM1ROZ0phZDVITGNMSFhiTlJ0Yzl3Y1FJREFR\n"
        + "QUJBb0lCQVFDTEkwcXd3aTUrQnlpaQppUXJRSVpVWW1xVlZjWWg1bGJLcW5VbDl3bEl0NU55MHBT\n"
        + "cWlwdjUzbklBTnB5bGIzd1BSZHdRRG51UWNzWmp1CmQ0cHQwWDNUanRIZFJNVmgwQzIybHlwUHVT\n"
        + "NEZheCtLZ0lVeHQxemdyY1I1c1E4Z1VQSXkva0FKTEhKVHBTMG0KSkJKRjV5UURpdk5oWlBGTkgy\n"
        + "d21wV0tzT3lydjNDUExIb2xBbE4wckNjUHdNQkRhZTRhQS8yM1J0eDdDajFzTQpmMTdOT2FWVWk3\n"
        + "L092UzlkWjEzU1dRUUEyVzhmd3FBNlUwVXBzYTlaUTR6dnRkdTdMS3diMXBTYStjbGlGUUh3CmNX\n"
        + "TWltVzIzMEUwclIzdEQ5aEFnSHJwUWZvM3hIek1DTDNrbGlOZ0lBZ2pwS0ZVUG9TRW1Ec1piVmkw\n"
        + "bWI4N0YKRytyaHdoUVJBb0dCQU45YWhRZEtrRWsrdXgwLzBYWUE0UHpCOUdiN25uR1dmUXM5RDMy\n"
        + "YmRYVkM2dGM2Vkw1eApDSzdDRk1jTTljV2djZDNzNjlNaTRXU1FQU1p6d3ZnWVVDb285MzZrRzVT\n"
        + "VHZJOE53b2lHRVZWT1F0N05CbkwvCjhiNWNsbENVQnBwUVNDeGJVUndEMUl1ZlRIRGZ4eEwvMnJa\n"
        + "MkJpMldCK0ZpejZ4ZHA5UmFpRWhkQW9HQkFPOHgKT1FNWCtRcU5wS3dwZGt3bGZYUW9HMEJDYjBP\n"
        + "UVNpQW5VWTZKL0drL1N3VWZta3JTc1hEVEdrMFpWT2dNa2FYOApvVzlQc3AxdVlDM0hsSE04UTZK\n"
        + "UGF0M3E1WTlSMW9vTysycFQ1UFNGWHVTbitiS2syRkVScW9KY0o1blpLY01pCnBwYlNJK041S0wr\n"
        + "Vllvc0JqOEhwd3Y0VlRPK05pRHBuWDFXOXhUY2xBb0dBYXJGSTNxVEZxOWRsZHFGbm8yRGsKR0ps\n"
        + "bDhGTzk2akNpNXQxeUt1UTZCWnZEcHJCY2p1UmI2MjhXa2NjbEdCUitrQzUrc0VyM05CeWF1V1dP\n"
        + "K3doUwpNdkdDMkdINE1zOG53WVluS1NReDMzZ1ZCVzBXNlpSTm5FdUtHays3bjdjOTRzSjBTbGVp\n"
        + "RGxnNElhc2o0M1dJCnVxUkNhMEJCMzI1ZUdjNWJQMXRId3BrQ2dZRUF4dEtYbldhejhIM0V3QWJH\n"
        + "K295L04ycVBIRjhjdFlDZTNSTlIKeTdZUFJqMENVd1B0OFB3cDJxcmZWZThVa0w3QjRzT1lQVHAy\n"
        + "TWo2cFcrUm1GVk9tdEtobklJZzh3V0U1Z0JEcwpFM29nK3RCU0RLZEQrNmJpMktCaXAzR0t5V3Vt\n"
        + "TnpuNlY2dmRnUndaTHdjeG5uSngyTEpERDRrTkpxTHk5Zmt2Cldjb2d3TEVDZ1lFQXJqQ21KUHI0\n"
        + "MEw3N3dORmg4VUdpUnVLajVZbW1sUGh3b0c2KzNtSGFWOHFtR1ZEelk0Z0wKd0xGMzRGSk5LV0J6\n"
        + "NDNGTWdqdTVLcC9aSWVaRUlTYzZqYTFzSTNHSzh6Z0ZzNks0SVpDMkVZU2ZFZTJEM3BpQwp1RzhV\n"
        + "NDJQeWJZbVJtRk9CaVdyT0JRY0pvc3NZUzZ6K0l0OUgyWXRxZEEydER3YThaNkpkT1l3PQotLS0t\n"
        + "LUVORCBSU0EgUFJJVkFURSBLRVktLS0tLQo=\n";

    ConfigBuilder config = new ConfigBuilder()
                               .withMasterUrl("https://api.west.kube.harness.io")
                               .withCaCertData(caCertData)
                               .withClientCertData(clientCertData)
                               .withClientKeyData(clientKeyData);

    try (KubernetesClient client = new DefaultKubernetesClient(config.build())) {
      log.info("Version: " + client.getApiVersion());
      client.replicationControllers().list().getItems().forEach(
          replicationController -> log.info(replicationController.getMetadata().getName()));
    }
  }
}
