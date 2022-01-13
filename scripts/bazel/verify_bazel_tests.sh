# Copyright 2021 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

directory=$1
exitCode=$2
echo "Directory: $directory";
echo "exitCode: $exitCode";

newExitCode=0 
if [ $exitCode -ne 0 ]; then
  if [ -d $directory ]; then
    find $directory -name test.xml | cpio -pmd testOutput
    if [ -d "testOutput" ]; then
      for file in `find "testOutput" -name "test.xml"`; do
        let itemsCount=$(xmllint --xpath 'count(//testsuite/testcase)' $file)

        for (( i=1; i <= $itemsCount; i++ )); do 
            description="$(xmllint --xpath '//testsuite/testcase['$i']' $file)"
            failure="$(xmllint --xpath 'boolean(//testsuite/testcase['$i']/failure)' $file)"
            failure_message=""
            if "$failure"; then 
                failure_message="$(xmllint --xpath '//testsuite/testcase['$i']/failure/@message' $file)"
                echo "Failure found for test $description message: $failure_message";
                newExitCode=1
                break
            fi
        done
      
        if [ $newExitCode -eq 1 ]; then
          break
        fi
      done 
    fi  
  fi 
fi

rm -rf testOutput
echo "newExitCode: $newExitCode"
exit $newExitCode;
