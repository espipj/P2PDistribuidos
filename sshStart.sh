#!/bin/bash

# ./sshStart i0901995 172.20.2.111 172.20.2.14 172.20.2.15

projectDir="/home/$1/Z/Distribuidos/P2PDistribuidos/"

warDir="P2P/P2P.war"
tomcatDir="tomcat"
tomcat=$projectDir$tomcatDir

# We clean OLD logs
#rm -r $tomcat/logs/*
# We copy the new War to the shared folder
cp $projectDir$warDir $tomcat/webapps/P2P.war
# We run tomcat in main machine

# We copy tomcat server to local machine, we shutdown the server and we run it again
mkdir -p /home/$1/tomcat && cp -a $tomcat/. /home/$1/tomcat/ && /home/$1/tomcat/bin/shutdown.sh && sleep 1 && rm -Rf /home/$1/tomcat/logs/* && /home/$1/tomcat/bin/startup.sh
ssh $1@$3 "mkdir -p /home/$1/tomcat && cp -a $tomcat/. /home/$1/tomcat/ && JRE_HOME=/opt/jdk1.8.0_60 /home/$1/tomcat/bin/shutdown.sh && sleep 1 && rm -Rf /home/$1/tomcat/logs/* && JRE_HOME=/opt/jdk1.8.0_60 /home/$1/tomcat/bin/startup.sh &"
ssh $1@$4 "mkdir -p /home/$1/tomcat && cp -a $tomcat/. /home/$1/tomcat/ && JRE_HOME=/opt/jdk1.8.0_60 /home/$1/tomcat/bin/shutdown.sh && sleep 1 && rm -Rf /home/$1/tomcat/logs/* && JRE_HOME=/opt/jdk1.8.0_60 /home/$1/tomcat/bin/startup.sh &"

# We launch our P2P network

echo -e "InicializaciOn de la red P2P...\n"
echo -e "###################################################\n"
sleep 1
curl -v http://localhost:8080/P2P/inicializar?ip=$2\&port=8080
curl -v http://$3:8080/P2P/inicializar?ip=$3\&port=8080
curl -v http://$4:8080/P2P/inicializar?ip=$4\&port=8080

echo -e "###################################################\n"
echo -e "MAquinas Inicializadas correctamente!\n"
echo -e "Inicializando equipos P2P...\n"
echo -e "###################################################\n"

curl http://localhost:8080/P2P/Peer?port=8080\&numPeer=0
sleep 1
curl http://localhost:8080/P2P/Peer?ip=$2\&port=8080\&toPeer=0\&numPeer=1
sleep 1
curl http://localhost:8080/P2P/Peer?ip=$2\&port=8080\&toPeer=0\&numPeer=2
sleep 1

curl http://$3:8080/P2P/Peer?ip=$2\&port=8080\&toPeer=0\&numPeer=0
sleep 1
curl http://$3:8080/P2P/Peer?ip=$2\&port=8080\&toPeer=0\&numPeer=1
sleep 1
curl http://$3:8080/P2P/Peer?ip=$2\&port=8080\&toPeer=0\&numPeer=2
sleep 1

curl http://$4:8080/P2P/Peer?ip=$2\&port=8080\&toPeer=0\&numPeer=0
sleep 1
curl http://$4:8080/P2P/Peer?ip=$2\&port=8080\&toPeer=0\&numPeer=1
sleep 1
curl http://$4:8080/P2P/Peer?ip=$2\&port=8080\&toPeer=0\&numPeer=2
sleep 1

echo -e "Equipos P2P Inicializados\n"
