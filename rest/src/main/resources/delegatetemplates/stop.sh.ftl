<#include "common.sh.ftl">
printenv
echo "Find delegate"
echo `ps -ef | grep delegate`
echo "=====Done==="
echo `pgrep -f "\-Ddelegatesourcedir"`
if `pgrep -f "\-Ddelegatesourcedir"> /dev/null`
then
  i=0
  while [ "$i" -le 30 ]
  do
    if `pgrep -f "\-Ddelegatesourcedir"> /dev/null`
    then
      pgrep -f "\-Ddelegatesourcedir" | xargs kill
      if [ "$i" -gt 0 ]
      then
        sleep 1
      fi
      i=$((i+1))
    else
      echo "Delegate stopped"
      exit 0
    fi
  done
  echo "Unable to stop delegate in 30 seconds."
  exit 1
else
  echo "Delegate not running"
  exit 0
fi
