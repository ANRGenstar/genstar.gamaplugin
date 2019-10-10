[![GitHub license](https://img.shields.io/github/license/gama-platform/gama-platform.github.io)](https://github.com/gama-platform/gama-platform.github.io/blob/sources/LICENSE)
[![Language](http://img.shields.io/badge/language-java-brightgreen.svg)](https://www.java.com/)
[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=ANRGenstar_genstar.gamaplugin&metric=ncloc)](https://sonarcloud.io/dashboard?id=ANRGenstar_genstar.gamaplugin)

[![Build Status](https://travis-ci.org/ANRGenstar/genstar.gamaplugin.svg?branch=master)](https://travis-ci.org/ANRGenstar/genstar.gamaplugin)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=ANRGenstar_genstar.gamaplugin&metric=bugs)](https://sonarcloud.io/dashboard?id=ANRGenstar_genstar.gamaplugin)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=ANRGenstar_genstar.gamaplugin&metric=vulnerabilities)](https://sonarcloud.io/dashboard?id=ANRGenstar_genstar.gamaplugin)
 

# Temporary install procedure in GAMA

This install procedure has been tested the 19th of April 2018. It has many drawbacks (copy-paste of jars, incompatible with the continuous built, use of very heavy jars for genstar, with duplicated libraries, Eclipse-dependent...) but has the main advantage to integrate very well in a classical IDE for GAMA. So it is only temporary!

To use and develop the Genstar Plugin in GAMA, you need:
- GAMA in its development version
- the genstar library
- the genstar GAMA plugin


# 1. Install Eclipse and GAMA source code
cf. as presented here: https://github.com/gama-platform/gama/wiki/InstallingGitVersion

# 2. Get the source code of the genstar library and genstar plugin.

Clone the following GitHub repository Github: 
 * the genstar library:  https://github.com/ANRGenstar/genstar.git
 * [Optional] the genstar templates (i.e. examples): https://github.com/ANRGenstar/template.git
 * the gama plugin: https://github.com/ANRGenstar/gamaplugin.git

With Eclipse: 
* Open the Git perspective
* In the View Git Repositories, Click on the icon "Clone a Git Repository and add the clone to this view"

In the View Git Repositories, you should have 4 repositories: gama, gamaplugin, genstar and template.

# 3. Import the genstar library as Eclipse projects.
 * File > Import ...
 * [Window: Import] Select: Git / Projects from Git (Click on Next button)
 * [Window: Import Project from Git] Select: Existing local repository (Next)
 * [Window: Import Projects from Git] Select: genstar (Next)
 * [Window: Import Projects from Git] "Import existing Eclipse projects" should be checked and "Working Tree" selected (Next)
 * [Window: Import Projects from Git] Check "Search for nested projects", select the 5 projects (genstar-core, genstar-gospl, genstar-spin, genstar-spll and parent) (Finish)


# 4. Import the genstar plugin as Eclipse projects.
 * File > Import ...
 * [Window: Import] Select: Git / Projects from Git (Click on Next button)
 * [Window: Import Project from Git] Select: Existing local repository (Next)
 * [Window: Import Projects from Git] Select: gamaplugin (Next)
 * [Window: Import Projects from Git] "Import existing Eclipse projects" should be checked and "Working Tree" selected (Next)
 * [Window: Import Projects from Git] Check "Search for nested projects", select the last project (genstar.plugin.bundle-all, the last one, the deepest one, among the 2 available) (Finish)

# 5. Import the template as Eclipse projects. (similarly)

# 6. Associate genstar library with the GAMA plugin.
The GAMA Plugin comes with all the necessary jar libraries and in particular the genstar library.
  

If you want to modify the genstar library, after a modifications you need to build again the genstar libraries:
  * right-click on the modified plugin > Run As > Maven install
  * in the genstar project, in the target folder, copy the genstar*.jar and paste it in the plugin lib_genstar folder.

In the case where a ClassNotFoundException appears in GAMA, when running a model using Genstar operators, it could be due to a missing .jar in the gamaplugin plugin. You should thus need to add the missing library in the lib folder (you also have to add it in the plugin.xml, in the classpath pane).

A sure case to avoid any missing library, you can follow the following procedure, to prouce the genstar library with all the needed libraries:
  * Right-click on the `parent` plugin > Run As >  Maven install
  * in the 4 genstar projects (genstar-core, -gospl, -spll, -spin), in the target folder, take the genstar-*-jar-with-dependencies.jar and paste them in the plugin `lib_genstar` folder.
  * add these 4 libraries to the classpath of the gamaplugin plugin and to the classpath in the plugin.xml.

# 7. Ask GAMA to call the plugin at start
  * In ummisco.gama.feature.core.extensions plugin, feature.xml, add the genstar plugin to the Included Plug-ins



