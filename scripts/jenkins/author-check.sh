set +x
set -e

UNKNOWN_USERS=`\
git log -1000 --oneline --format='%aN <%aE>' | sort -u |\
    grep -iv "^CI Bot <bot@harness.io>$" |\
    grep -iv "^Aaditi Joag <aaditi.joag@harness.io>$" |\
    grep -iv "^Aaditya Kumar <aaditya.kumar@harness.io>$" |\
    grep -iv "^Abhijith V Mohan <abhijith.mohan@harness.io>$"|\
    grep -iv "^Abhijith V Mohan <abhijith.vmohan@gmail.com>$"|\
    grep -iv "^Adam Hancock <adam.hancock@harness.io>$"|\
    grep -iv "^Adwait Bhandare <adwait.bhandare@harness.io>$" |\
    grep -iv "^Aman <aman.singh@harness.io>$" |\
    grep -iv "^aman-iitj <aman.singh@harness.io>$" |\
    grep -iv "^Ankit Singhal <ankit.singhal@harness.io>$" |\
    grep -iv "^Anshul Anshul <anshul@harness.io>$" |\
    grep -iv "^Anubhaw Srivastava <anubhaw@harness.io>$" |\
    grep -iv "^Brett Zane <brett@harness.io>$" |\
    grep -iv "^Christopher Clark <christopher.clark@harness.io>$" |\
    grep -iv "^Deepak Patankar <deepak.patankar@harness.io>$"|\
    grep -iv "^Duc Nguyen <duc@harness.io>$" |\
    grep -iv "^Duc Nguyen <duc@wings.software>$" |\
    grep -iv "^Garvit Pahal <garvit.pahal@harness.io>$" |\
    grep -iv "^George Georgiev <george@harness.io>$" |\
    grep -iv "^hannah-tang <hannah.tang@harness.io>$"|\
    grep -iv "^Harsh Jain <harsh.jain@harness.io>$" |\
    grep -iv "^harshjain12 <49689464+harshjain12@users.noreply.github.com>$" |\
    grep -iv "^hitesharinga <51195474+hitesharinga@users.noreply.github.com>$"|\
    grep -iv "^hitesharinga <hitesh.aringa@harness.io>$"|\
    grep -iv "^Ishan Bhanuka <ishan.bhanuka@harness.io>$"|\
    grep -iv "^Jatin Shridhar <jatin@harness.io>$" |\
    grep -iv "^K Rohit Reddy <rohit.reddy@harness.io>$"|\
    grep -iv "^kjoshi12345 <kamal.joshi@harness.io>$"|\
    grep -iv "^Magdalene Lee <magdalene.lee@harness.io>$" |\
    grep -iv "^Mark Lu <mark.lu@harness.io>$" |\
    grep -iv "^Matt Hill <matt@harness.io>$" |\
    grep -iv "^Meenakshi Raikwar <meenakshi.raikwar@harness.io>$"|\
    grep -iv "^michael-katz <48811122+michael-katz@users.noreply.github.com>$"|\
    grep -iv "^Nataraja Maruthi <nataraja@harness.io>$" |\
    grep -iv "^Parnian Zargham <parnian@harness.io>$" |\
    grep -iv "^Pooja Singhal <pooja.singhal@harness.io>$" |\
    grep -iv "^poojaSinghal <pooja.singhal@harness.io>$" |\
    grep -iv "^Pranjal Kumar <pranjal@harness.io>$" |\
    grep -iv "^pranjal-harness <pranjal@harness.io>$" |\
    grep -iv "^Prashant Pal <50949177+ppal31@users.noreply.github.com>$"|\
    grep -iv "^Praveen Kambam Sugavanam <praveen.sugavanam@harness.io>$" |\
    grep -iv "^Puneet Saraswat <puneet.saraswat@harness.io>$" |\
    grep -iv "^Raghvendra Singh <raghu@harness.io>$" |\
    grep -iv "^Rama Tummala <rama@harness.io>$" |\
    grep -iv "^Rathnakara Malatesha <rathna@harness.io>$" |\
    grep -iv "^Rishi Singh <rishi@harness.io>$" |\
    grep -iv "^rohit kumar <rohit.kumar@harness.io>$"|\
    grep -iv "^Rushabh Shah <rushabh.shah@harness.io>$" |\
    grep -iv "^Sahithi Kolichala <sahithi@harness.io>$" |\
    grep -iv "^Satyam Shanker <satyam.shanker@harness.io>$" |\
    grep -iv "^Shaswat Deep <shaswat.deep@harness.io>$"|\
    grep -iv "^Shubhanshu Verma <shubhanshu.verma@harness.io>$"|\
    grep -iv "^Sowmya K <sowmya.k@harness.io>$" |\
    grep -iv "^Srinivasa Gurubelli <srinivas@harness.io>$" |\
    grep -iv "^Sriram Parthasarathy <sriram@harness.io>$" |\
    grep -iv "^Sunil Shetty <sunil@harness.io>$" |\
    grep -iv "^Swagat Konchada <swagat@harness.io>$" |\
    grep -iv "^Swamy Sambamurthy <swamy@harness.io>$" |\
    grep -iv "^swamy-harness <46276030+swamy-harness@users.noreply.github.com>$" |\
    grep -iv "^Swapnil Mahajan <swapnil@harness.io>$" |\
    grep -iv "^Tan Nhu <tan@harness.io>$" |\
    grep -iv "^Ujjawal Prasad <ujjawal.prasad@harness.io>$"|\
    grep -iv "^Utkarsh Gupta <utkarsh.gupta@harness.io>$"|\
    grep -iv "^Vaibhav Singhal <vaibhav.si@harness.io>$" |\
    grep -iv "^Vaibhav Tulsyan <vaibhav.tulsyan@harness.io>$" |\
    grep -iv "^Vaibhav Tulsyan <xennygrimmato@gmail.com>$"|\
    grep -iv "^vaibhavtulsyan <vaibhav.tulsyan@harness.io>$"|\
    grep -iv "^Venkatesh Kotrike <venkatesh.kotrike@harness.io>$" |\
    grep -iv "^Vikas Naiyar <vikas.naiyar@harness.io>$" |\
    grep -iv "^yogesh-chauhan <yogesh.chauhan@harness.io>$"` || echo "All clear"

    
if [ ! -z "$UNKNOWN_USERS" ]
then
    echo "Unknown user name and/or email"
    echo "$UNKNOWN_USERS"
    exit 1
fi

EXCEPTIONS="Abhijith V Mohan <abhijith.vmohan@gmail.com>"
EXCEPTIONS="$EXCEPTIONS\|Aman <aman.singh@harness.io>"
EXCEPTIONS="$EXCEPTIONS\|aman-iitj <aman.singh@harness.io>"
EXCEPTIONS="$EXCEPTIONS\|Duc Nguyen <duc@wings.software>"
EXCEPTIONS="$EXCEPTIONS\|hannah-tang <hannah.tang@harness.io>"
EXCEPTIONS="$EXCEPTIONS\|harshjain12 <49689464+harshjain12@users.noreply.github.com>"
EXCEPTIONS="$EXCEPTIONS\|hitesharinga <51195474+hitesharinga@users.noreply.github.com>"
EXCEPTIONS="$EXCEPTIONS\|hitesharinga <hitesh.aringa@harness.io>"
EXCEPTIONS="$EXCEPTIONS\|K Rohit Reddy <rohit.reddy@harness.io>"
EXCEPTIONS="$EXCEPTIONS\|kjoshi12345 <kamal.joshi@harness.io>"
EXCEPTIONS="$EXCEPTIONS\|michael-katz <48811122+michael-katz@users.noreply.github.com>"
EXCEPTIONS="$EXCEPTIONS\|poojaSinghal <pooja.singhal@harness.io>"
EXCEPTIONS="$EXCEPTIONS\|pranjal-harness <pranjal@harness.io>"
EXCEPTIONS="$EXCEPTIONS\|Prashant Pal <50949177+ppal31@users.noreply.github.com>"
EXCEPTIONS="$EXCEPTIONS\|rohit kumar <rohit.kumar@harness.io>"
EXCEPTIONS="$EXCEPTIONS\|swamy-harness <46276030+swamy-harness@users.noreply.github.com>"
EXCEPTIONS="$EXCEPTIONS\|Vaibhav Tulsyan <xennygrimmato@gmail.com>"
EXCEPTIONS="$EXCEPTIONS\|vaibhavtulsyan <vaibhav.tulsyan@harness.io>"
EXCEPTIONS="$EXCEPTIONS\|yogesh-chauhan <yogesh.chauhan@harness.io>"

EXECPTION_COMMITS=`git log --oneline --format='%aN <%aE>' | grep -i "^$EXCEPTIONS$" | wc -l`

if [ $EXECPTION_COMMITS -gt 203 ]
then
    echo "You bringing commit with excepted author that is no longer allowed"
    exit 1
fi