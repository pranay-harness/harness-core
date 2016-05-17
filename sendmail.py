import smtplib
import sys
import os

def send_email(user, pwd, recipient, subject, body):
    gmail_user = user
    gmail_pwd = pwd
    FROM = user
    TO = recipient if type(recipient) is list else [recipient]
    SUBJECT = subject
    TEXT = body

    # Prepare actual message
    message = """\From: %s\nTo: %s\nSubject: %s\n\n%s
    """ % (FROM, ", ".join(TO), SUBJECT, TEXT)
    #print message
    try:
        server = smtplib.SMTP("smtp.gmail.com", 587)
        server.ehlo()
        server.starttls()
        server.login(gmail_user, gmail_pwd)
        server.sendmail(FROM, TO, message)
        server.close()
        print 'successfully sent the mail'
    except:
        print "failed to send mail", sys.exc_info()[0]


build_num=os.environ['CIRCLE_BUILD_NUM']
branch=os.environ['CIRCLE_BRANCH']
recipient=os.environ['CIRCLE_RECIPIENT']
sender=os.environ['CIRCLE_SENDER_EMAIL']
password=os.environ['CIRCLE_SENDER_PASSWORD']
subject='CI Test Report for branch %s build %s' % (branch,build_num)
body="""Report Link(Maynot get generated in case of code coverage failure): https://circleci.com/api/v1/project/wings-software/wings/%s/artifacts/0/$CIRCLE_ARTIFACTS/site/project-reports.html
Test Coverage: https://circleci.com/api/v1/project/wings-software/wings/%s/artifacts/0/$CIRCLE_ARTIFACTS/site/jacoco/index.html
""" % (build_num,build_num)

send_email(send_email, password, recipient, subject, body)
