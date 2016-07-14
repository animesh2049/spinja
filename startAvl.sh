#!/bin/bash
# Simple bash shell script to launch SpinJa

show_usage=
promela_file=
spinja_options=

if [ $# -eq 0 ] ; then
    echo "usage: spinja [options] promela_file" ;
    echo "options will be passed to the SpinJa model checker"
    exit 1
fi


while [ $# -gt 1 ] ; do
    spinja_options="$spinja_options $1"
    shift
done
promela_file=$1
mongod --port 7777 >/dev/null 2>&1
java  -cp spinja.jar   spinja.Compile $promela_file
javac -cp spinja.jar:. spinja/PanModel.java 
java  -cp "spinja.jar:lib/mongo-java-driver-3.2.2.jar:lib/Sizeof.jar:." -javaagent:lib/Sizeof.jar spinja.PanModel -a -DAVLSQLITE
