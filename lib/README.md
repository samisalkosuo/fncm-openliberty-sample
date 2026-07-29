# FileNet JARs

Download the JARs from the FileNet Admin Console (ACCE) Client API download page and place them in this directory:

- `Jace.jar`
- `p8cel10n.jar`

## Local development

Before running `mvn package` locally, install the JARs into your local Maven repository:

```bash
mvn install:install-file -Dfile=lib/Jace.jar \
    -DgroupId=com.ibm.filenet -DartifactId=Jace \
    -Dversion=5.5 -Dpackaging=jar

mvn install:install-file -Dfile=lib/p8cel10n.jar \
    -DgroupId=com.ibm.filenet -DartifactId=p8cel10n \
    -Dversion=5.5 -Dpackaging=jar
```

The Dockerfile runs these commands automatically during the build stage.

