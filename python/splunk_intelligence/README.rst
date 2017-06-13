To setup the python development environment

1.  Make sure python 2.7 is installed at /usr/bin/python2.7. We use 2,7 because splunk sdk does not support python 3 yet.
    On macs, by default, python 2.7 is installed here. In the future this can be made configurable through an environment
    variable.
2.  Make sure virtualenv is installed by running virtualenv -v.
    You can install virtualenv by running : sudo easy_install virtualenv
2.  Go to wings/python/splunk_intelligence/
3.  Run : make clean
4.  Run : make init
5.  To start the Jupyter notebook run : make jupyter
        1.  This should open the notebook in your browse.
        2.  Navigate to src/notebooks
        3.  Click on SplunkAnomaly.ipynb
        4.  Run notebook. If charts don't show up, copy the notebook content and create a new notebook and run it.
            There is a know issue with Jupyter notebook where saved notebooks don't show the charts when run again

Notes:
Everything under notebooks is for internal use and not for production. Treat this as work in progress.


Troubleshooting:

1.  "make init"
        Error: Setting up virtualenv
                /bin/bash: virtualenv: command not found
                make: *** [init] Error 127
        Cause: virtualenv is not installed        
        Solution: run sudo easy_install virtualenv (step 2 in setup)

2.  "make jupyter"
        Error: 0:97: execution error: "http://localhost:8888/tree?token=c250593d85c309b39b4c8da5eeea5145a16cbbda8382c653"                 
        doesn’t understand the “open location” message. (-1708)
        [W 10:50:57.462 NotebookApp] 404 GET /api/kernels/2fb90bee-ce80-4b54-8b4d-77ae475d60f8/channels?    
        session_id=3D2A2829C5724D099FFAAF3854A73353 (::1): Kernel does not exist: 2fb90bee-ce80-4b54-8b4d-77ae475d60f8
        
        Cause: Unable to open the notebooks viewer in your default browser
        
        Solution: Copy the url and paste it in your browser
