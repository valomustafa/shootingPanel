#!/bin/bash
sleep 10
cd /home/pi/Developer
if [ -s ./app.properties ]
then
        . app.properties
else
        cd /home/pi/Documents
        . app.properties
fi

cd /home/pi/Developer

application=ShootingPanel-$currentVersion-jar-with-dependencies.jar
sudo chmod 777 $application
sudo java -jar $application