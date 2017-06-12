To setup the python development environment

1.  Make sure python 2.7 is installed at /usr/bin/python2.7. We use 2,7 because splunk sdk does not support python 3 yet.
    On macs, by default, python 2.7 is installed here. In the future this can be made configurable through an environment
    variable.
2.  Go to wings/python/splunk_intelligence/
3.  Run : make clean
4.  Run : make init
5.  To start the Jupyter notebook run : make jupyter
    a.  This should open the notebook in your browse.
    b.  Navigate to src/notebooks
    c.  Click on SplunkAnomaly.ipynb
    d.  Run notebook. If charts don't show up, copy the notebook content and create a new notebook and run it.
        There is a know issue with Jupyter notebook where saved notebooks don't show the charts when run again

Notes:
Everything under notebooks is for internal use and not for production. Treat this as work in progress.
