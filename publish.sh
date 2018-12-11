#!/bin/bash
cd genstar.gamaplugin.p2updatesite &&
mvn -U -X clean install -P p2Repo --settings ../settings.xml && 
cd -
