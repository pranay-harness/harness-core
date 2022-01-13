/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.perpetualtask.k8s.utils;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@TargetModule(HarnessModule._420_DELEGATE_AGENT)
@OwnedBy(HarnessTeam.CE)
public interface DebugConstants {
  // These clusterIds belongs to Relativity
  Set<String> RELATIVITY_CLUSTER_IDS = new HashSet<>(Arrays.asList("5e287dc91e057d4bc64a7829",
      "5e28906d1e057d4bc60ebe03", "5e28bef31e057d4bc6b5e6be", "5e28dcc31e057d4bc69c8720", "5e28dd851e057d4bc6afa17a",
      "5e29c4f01e057d4bc66cd56b", "5e29d5961e057d4bc60ae768", "5e2b69ab1e057d4bc6966f02", "5e2b69b91e057d4bc6979066",
      "5e2f29e41e057d4bc666b4b0", "5e2f329e1e057d4bc6594cb0", "5e331a411e057d4bc6c7508e", "5e3451421e057d4bc65f1ca2",
      "5e3481ec1e057d4bc630a2b0", "5e34a56e1e057d4bc6df11b1", "5e3855d51e057d4bc6606420", "5e4dbb73ffe515507c437589",
      "5e544f6affe515507c7b717d", "5e62ba3c44f443447ff88677", "5e739391b9f80f47e177f33b", "5e751125b9f80f47e1cc4d15",
      "5e7a4abbb9f80f47e14b6cb0", "5e823d74b9f80f47e1010912", "5eb45b11d2a72e4d76f5a6d0", "5eb57ab3d2a72e4d76754617",
      "5eb57ad9d2a72e4d767bae5a", "5eb57b01d2a72e4d768152ab", "5eb57b1dd2a72e4d76848331", "5eb59e4bd2a72e4d76072e02",
      "5eb59e72d2a72e4d760cb07a", "5ec61274e9954c40180a6790", "5ece950be3084c95a0fc2222", "5ecebe58e3084c95a025117a",
      "5ed90fe0e3084c95a066292d", "5edfc5182aa4186d1c3c4d92", "5edfc54b2aa4186d1c44b544", "5ee0ee912aa4186d1c185979",
      "5ee0eeaa2aa4186d1c1b01cd", "5ee13d452aa4186d1cfce5a0", "5ee13d652aa4186d1c02a627", "5ee14ced2aa4186d1c643845",
      "5ee14d032aa4186d1c67ce2e", "5ee151302aa4186d1c02fbf0", "5ee1514c2aa4186d1c084740", "5ee1546d2aa4186d1c7f6c2d",
      "5ee154832aa4186d1c83b81b", "5ee157962aa4186d1cf6c5fe", "5ee158392aa4186d1c13e6b0", "5ee1584f2aa4186d1c1852de",
      "5ee158b22aa4186d1c2e927e", "5ee15b2c2aa4186d1c965877", "5ee15b482aa4186d1c9c1ef6", "5ee787462aa4186d1ced60f9",
      "5ee78e1a2aa4186d1cd8308e", "5ee78e322aa4186d1cdaabd0", "5ee78ec42aa4186d1cec7998", "5ef439dd2aa4186d1c0170d7",
      "5ef43a222aa4186d1c0a6fab", "5f0624865ca519dda84e69b2", "5f0624a35ca519dda8511570", "5f076fb85ca519dda89adc66",
      "5f076fd65ca519dda89ec55b", "5f15b7a55ca519dda8436fa8", "5f18b19c5ca519dda87dadf7", "5f3d8b0378440ec88aa6ac41",
      "5f49585078440ec88a2d5b2d", "5f4eadc78160d0a7d7968aee", "5f4eadf08160d0a7d79ea87c", "5f4eae088160d0a7d7a4d34a",
      "5f4eae1f8160d0a7d7aa45c4", "5f4eae338160d0a7d7aee690", "5f4eae5a8160d0a7d7b6ddf2", "5f4eae6f8160d0a7d7ba9ca7",
      "5f4eae858160d0a7d7bffd36", "5f4eae988160d0a7d7c35007", "5f4eaeb08160d0a7d7cad372", "5f4eaec78160d0a7d7d1e9ed",
      "5f57f56f8160d0a7d7b2c7eb", "5f93294bf3d07567e2bd4d8a", "6010524ae5ed70b9ea9a17ec", "60105273e5ed70b9eaa3e3a7",
      "602e90f7e4d9f4fa897b8f31", "60369743e4d9f4fa8990a1a3", "60369fafe4d9f4fa8924dfc7", "60369fdfe4d9f4fa8933b125",
      "60a5380edb0e21dee46ae585", "60b23477db0e21dee4401f5d", "60b28629db0e21dee41922db", "60c8b51adb0e21dee4e26161",
      "60cd0bf2db0e21dee49a4efd", "60dca5874ca464612ca7616e", "60dddc1b4ca464612c3dcf3f", "60ddf4614ca464612c5cfee7",
      "60f768a75cbe3ebdf9010475", "61007f7e5cbe3ebdf90ca38b", "61007ff05cbe3ebdf9294c79", "610081b95cbe3ebdf9a5c5b4",
      "6103188b5cbe3ebdf9908b67", "610329525cbe3ebdf956376c", "6104074d5cbe3ebdf90a5cb4", "61040c675cbe3ebdf9858fea",
      "61040ed75cbe3ebdf9310b39", "61083933d01ad32dc362c0c7", "610aa216d01ad32dc3402353", "610aa21ad01ad32dc340f1fd",
      "610aa237d01ad32dc34816cd", "610aa2d2d01ad32dc37472e8", "610abfdfd01ad32dc3cba4e2", "610ac05ad01ad32dc3ed81d0",
      "610ac6b0d01ad32dc3ba0d2b", "610bed9fd01ad32dc38ac130", "61115bf7d01ad32dc336682f", "61123b9ad01ad32dc3dd34c9",
      "61123b9ad01ad32dc3dd552b", "6113e143d01ad32dc3ea3992", "6114eebed01ad32dc34625b8", "61153699d01ad32dc3aa7471",
      "61153dd9d01ad32dc3b5c8d2", "611541f5d01ad32dc3da1181", "61157ef2d01ad32dc3ac4fa9", "611627d4d01ad32dc3e9269e",
      "611629dbd01ad32dc36f2f15", "61166c81d01ad32dc3920d5d", "611687bad01ad32dc37a28f1", "61168d88d01ad32dc3f5a73a",
      "611a807c05ef6bf31c34c243", "611b6fe805ef6bf31c02b6c0", "611b6fe905ef6bf31c02d406"));
}
