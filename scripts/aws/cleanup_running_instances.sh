# Copyright 2018 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

REGIONS=`aws ec2 describe-regions | grep RegionName | awk '{ print $2 }' | sed 's/.$//' | sed 's/^.//'`

for REGION in $REGIONS
do
    echo $REGION
    aws ec2 describe-instances --filter "Name=instance-state-name,Values=running" --region $REGION |\
      grep InstanceId | awk '{ print $2 }' | sed 's/.\{2\}$//' | sed 's/^.//' |\
      while read INSTANCE
      do
        echo "  $INSTANCE"
        # aws ec2 modify-instance-attribute --region $REGION --no-disable-api-termination --instance-id=$INSTANCE
        aws ec2 terminate-instances --region $REGION --instance-id=$INSTANCE
      done
done
