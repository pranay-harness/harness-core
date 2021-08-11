run_ui ()
{
    echo '<DOCKERHUBPASS>' | sudo docker login -u harnessdev -p XDF+=fdfkvkstxz;
    if [ ! -z "$1" ]; then
        tag=":$1";
    fi;
    sudo docker pull harness/ui$tag;
    sudo docker run -it -p 8000:8080 --rm -e API_URL=https://localhost:9090 harness/ui$tag
}  

alias runui='run_ui'
