<#include "common.sh.ftl">

if `pgrep -f "\-Dwatchersourcedir=$DIR"> /dev/null`; then
  i=0
  stopped=0
  while [ "$i" -le 30 ]; do
    if `pgrep -f "\-Dwatchersourcedir=$DIR"> /dev/null`; then
      pgrep -f "\-Dwatchersourcedir=$DIR" | xargs kill
      if [ "$i" -gt 0 ]; then
        sleep 1
      fi
      i=$((i+1))
    else
      echo "Watcher stopped"
      stopped=1
      break
    fi
  done
  if [ "$stopped" -eq 0 ]; then
    echo "Unable to stop watcher in 30 seconds. Trying to force it ..."
    pgrep -f "\-Dwatchersourcedir=$DIR" | xargs kill -9
    sleep 10
    if `pgrep -f "\-Dwatchersourcedir=$DIR"> /dev/null`; then
      echo "Unable to stop watcher in 40 seconds. See process details ..."
      echo
      pgrep -f "\-Dwatchersourcedir=$DIR" | xargs ps aux
      echo
    else
      echo "Watcher stopped"
    fi
  fi
else
  echo "Watcher not running"
fi

if `pgrep -f "\-Ddelegatesourcedir=$DIR"> /dev/null`; then
  i=0
  while [ "$i" -le 30 ]; do
    if `pgrep -f "\-Ddelegatesourcedir=$DIR"> /dev/null`; then
      pgrep -f "\-Ddelegatesourcedir=$DIR" | xargs kill
      if [ "$i" -gt 0 ]; then
        sleep 1
      fi
      i=$((i+1))
    else
      echo "Delegate stopped"
      rm -rf msg
      exit 0
    fi
  done
  echo "Unable to stop delegate in 30 seconds. Trying to force it ..."
  pgrep -f "\-Ddelegatesourcedir=$DIR" | xargs kill -9
  sleep 10
  if `pgrep -f "\-Ddelegatesourcedir=$DIR"> /dev/null`; then
    echo "Unable to stop delegate in 40 seconds. See process details ..."
    echo
    pgrep -f "\-Ddelegatesourcedir=$DIR" | xargs ps aux
    echo
    exit 1
  else
    echo "Delegate stopped"
    rm -rf msg
    exit 0
  fi
else
  echo "Delegate not running"
  rm -rf msg
  exit 0
fi ) 2>&1 | tee -a logs/stopscript.log
