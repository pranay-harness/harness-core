<#include "common.sh.ftl">

if `pgrep -f "\-Ddelegatesourcedir=$DIR"> /dev/null`
then
  i=0
  while [ "$i" -lt 30 ]
  do
    pgrep -f "\-Ddelegatesourcedir=$DIR" | xargs kill
    pgrep -f "\-Ddelegatesourcedir=$DIR"> /dev/null
    rc=$?
    if [ "$rc" -eq 0 ]
    then
      sleep 1
      i=$((i+1))
    else
      echo "Delegate stopped."
      exit 0
    fi
  done
  echo "Unable to stop delegate in 30 seconds."
  exit 1
else
  echo "Delegate not running."
  exit 1
fi
