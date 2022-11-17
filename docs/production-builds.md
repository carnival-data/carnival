# Production Builds

## Publishing to Maven Local

Carnival artifacts can be published to your local Maven repository usually found on your file system in the directory `~/.m2` via the following command. 

```Shell
./gradlew :carnival-gradle:publishToMavenLocal 
```

## Publishing to Maven

Carnival artifacts are published to [Maven](https://search.maven.org/search?q=io.github.carnival-data) via the [Nexus Repository Manager](https://s01.oss.sonatype.org).

### Configuration
Copy `.env-template` to `.env` and update the file with your maven central credentials and private key information. The signing file should be **.gpg** (not **.asc**) format.  `SIGNING_KEY_ID` is usually the last 8 digits of the fingerprint. More detail about the signing plugin available [here](https://docs.gradle.org/7.4.1/userguide/signing_plugin.html#sec:signatory_credentials).


### Publish to Snapshot Repository
When the carnivalVersion specified in `app/gradle.properties` ends with "-SNAPSHOT", the package will be published to the snapshot repository. Previous releases with the same version can be overwritten.

#### Docker

```Shell
docker-compose -f docker-compose-publish-maven.yml up
```

#### Gradle

From the `app/` directory:

```Shell
source ../.env
./gradlew :carnival-gradle:publishAllPublicationsToCentralRepository 
```

### Publish Release Versions
If the version number does not end with "-SNAPSHOT", the package will be published to the staging repository and must be manually approved.

#### Docker

Run the following to publish to the staging repository:
```
docker-compose -f docker-compose-publish-maven.yml up
```

#### Gradle

From the `app/` directory:


```Shell
source ../.env
```

```Shell
./gradlew publishAllPublicationsToCentralRepository -Psigning.secretKeyRingFile=${SIGNING_PRIVATE_DIR}/${SIGNING_PRIVATE_FILE} -Psigning.password=${SIGNING_PRIVATE_KEY_PASSWORD} -Psigning.keyId=${SIGNING_KEY_ID} -Pcentral.user=${CENTRAL_USER} -Pcentral.password=${CENTRAL_PASSWORD} --no-daemon --console=plain
```

#### Publish The Release

1. Log into the [Maven Nexus Repository Manager](https://s01.oss.sonatype.org/#welcome)

1. Click "Staging Repositories" on the left. The repository that was just published should be visible.

1. Review the repository files. If it looks correct, click "close" to close the staging repository and start the validation process.

1. If the validation is successfull, click "Release" to publish the release.



## Publishing Production Builds to Github (Deprecated)

Production images are published to Github packages. In order to publish an image, you will need to create a Github personal access token with appropriate permissions to manage github packages (see Github Packages documentation for details). Then create local environment variables **GITHUB_USER** and **GITHUB_TOKEN** with your github user and personal access token.
Once authorization has been set up, the procedure to publish production builds is:

-   Stage and test any changes in the master branch
-   Update the app version number in `app/build.gradle` using semantic versioning conventions
-   Merge changes into the production branch
-   Build and publish changes to Github with the command gradle publish . The build.grade file has been configured to use the authentication information in the environment variables **GITHUB_USER** and **GITHUB_TOKEN** when attempting to publish.
-   Check that the packages with the updated version number are listed in Carnival Packages
-   Go back to the master branch, and `app/build.gradle` increment the version number and add the `-SNAPSHOT` suffix (i.e. `0.2.9-SNAPSHOT`)

For further details, see Configuring Gradle for use with GitHub Packages.
