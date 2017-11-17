<#include "common.sh.ftl">

if `pgrep -f "\-Dwatchersourcedir=$DIR"> /dev/null`
then
  i=0
  while [ "$i" -le 30 ]
  do
    if `pgrep -f "\-Dwatchersourcedir=$DIR"> /dev/null`
    then
      pgrep -f "\-Dwatchersourcedir=$DIR" | xargs kill
      if [ "$i" -gt 0 ]
      then
        sleep 1
      fi
      i=$((i+1))
    else
      echo "Watcher stopped"
      break
    fi
  done
  echo "Unable to stop watcher in 30 seconds."
  exit 1
else
  echo "Watcher not running"
fi

if `pgrep -f "\-Ddelegatesourcedir=$DIR"> /dev/null`
then
  i=0
  while [ "$i" -le 30 ]
  do
    if `pgrep -f "\-Ddelegatesourcedir=$DIR"> /dev/null`
    then
      pgrep -f "\-Ddelegatesourcedir=$DIR" | xargs kill
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
