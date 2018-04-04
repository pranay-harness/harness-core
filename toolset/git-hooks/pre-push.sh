#!/bin/sh
#
#This pre - commit hook checks if any versions of clang - format
#are installed, and if so, uses the installed version to format
#the staged changes.

pushd `dirname $0` > /dev/null && cd ../.. && BASEDIR=$(pwd -L) && popd > /dev/null

CHECK_CONFLICTS=hook.pre-push.check-conflicts
if [ "`git config $CHECK_CONFLICTS`" == "false" ]
then
    echo '\033[0;31m' checking left conflicts is disabled - to enable: '\033[0;37m'git config --unset $CHECK_CONFLICTS '\033[0m'
else
    echo '\033[0;34m' checking left conflicts  ... to disable: '\033[0;37m'git config --add $CHECK_CONFLICTS false '\033[0m'

    . $BASEDIR/toolset/git-hooks/check_conflicts.sh
fi

CHECKSTYLE_PROPERTY=hook.pre-push.stylecheck
if [ "`git config $CHECKSTYLE_PROPERTY`" == "false" ]
then
    echo '\033[0;31m' checking style is disabled - to enable: '\033[0;37m'git config --unset $CHECKSTYLE_PROPERTY '\033[0m'
else
    echo '\033[0;34m' checking style  ... to disable: '\033[0;37m'git config --add $CHECKSTYLE_PROPERTY false '\033[0m'

    . $BASEDIR/toolset/git-hooks/checkstyle.sh
fi

CHECK_BEHIND_COMMITS=hook.pre-push.behindcommits
BEHIND_COMMITS=`git config $CHECK_BEHIND_COMMITS 2>/dev/null`
if [ "$BEHIND_COMMITS" == "" ]
then
    BEHIND_COMMITS=-1
fi

if [ $BEHIND_COMMITS == -1 ]
then
    echo '\033[0;31m' checking behind commits is disabled - to enable: '\033[0;37m'git config --add $CHECK_BEHIND_COMMITS 3 \# or any other number \>= 0 '\033[0m'
else
    echo '\033[0;34m' checking behind commits  ... to disable: '\033[0;37m'git config --unset $CHECK_BEHIND_COMMITS '\033[0m'

    CURRENT_BRANCH=`git rev-parse --abbrev-ref HEAD`
    TARGET_BRANCH=`echo $CURRENT_BRANCH | sed -e "s/^\([^@]*\)$/\1@master/" | sed -e "s/^.*@//"`

    BEHIND=`git rev-list --left-right --count ${TARGET_BRANCH}...${CURRENT_BRANCH} | awk '{ print $1}'`

    if [ $BEHIND -gt 3 ]
    then
        echo "You are $BEHIND commits behind ${TARGET_BRANCH}. Please merge before you push."
        exit 1
    fi
fi

