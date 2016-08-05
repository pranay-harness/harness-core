#!/bin/bash

# Start tail.
<#list tailPatterns as tailPattern>
touch "${tailPattern.filePath}"
tail -F -n0 "${tailPattern.filePath}" | grep --line-buffered --color=always -A10 -B10 "${tailPattern.pattern}" 2>&1 > ${executionStagingDir}/tailoutput${executionId}${tailPattern?index} &
pid${tailPattern?index}=$!
</#list>

WINGS_SCRIPT_NAME=$1
shift

# Run the script
$WINGS_SCRIPT_DIR/$WINGS_SCRIPT_NAME

echo "Verifing via tailing outputs"

#Wait for tail outputs.
TAIL_TIMEOUT=30
TAIL_COUNT=${tailPatterns?size}
while [ "$TAIL_TIMEOUT" -gt 0 -a "$TAIL_COUNT" -gt 0 ]
do
<#list tailPatterns as tailPattern>
  if [ $(wc -l ${executionStagingDir}/tailoutput${executionId}${tailPattern?index} | tr -s " " | cut -d " " -f2) -gt 0 ]
  then
    if $(kill -0 $pid${tailPattern?index} 2>/dev/null)
    then
      kill -9 $pid${tailPattern?index} || true
      TAIL_COUNT=$((TAIL_COUNT - 1))
    fi
  fi
</#list>
  sleep 1;
  TAIL_TIMEOUT=$((TAIL_TIMEOUT - 1))
done

#Kill remaining tails after timeout.
<#list tailPatterns as tailPattern>
if $(kill -0 $pid${tailPattern?index} 2>/dev/null)
then
  kill -9 $pid${tailPattern?index} || true
fi
</#list>

returnvalue=0
#Print success outputs.
<#list tailPatterns as tailPattern>
if [ $(wc -l ${executionStagingDir}/tailoutput${executionId}${tailPattern?index} | tr -s " " | cut -d " " -f2) -gt 0 ]
then
  echo "<===== Tail file '${tailPattern.filePath}' for pattern '${tailPattern.pattern}' =====> "
  cat ${executionStagingDir}/tailoutput${executionId}${tailPattern?index}
fi
</#list>

#Print failure outputs.
<#list tailPatterns as tailPattern>
if [ $(wc -l ${executionStagingDir}/tailoutput${executionId}${tailPattern?index} | tr -s " " | cut -d " " -f2) -eq 0 ]
then
  echo "<===== Unable to find pattern '${tailPattern.pattern}' in file '${tailPattern.filePath}' =====> "
  returnvalue=1
fi
</#list>

exit $returnvalue
