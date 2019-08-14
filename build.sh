cd genstar.gamaplugin.parent &&
mvn clean install -U &&
mvn clean org.jacoco:jacoco-maven-plugin:prepare-agent install sonar:sonar -Dsonar.projectKey=anrgenstar_genstar.gamaplugin &&
cd -
