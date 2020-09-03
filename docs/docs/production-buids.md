---
# Feel free to add content and custom Front Matter to this file.
# To modify the layout, see https://jekyllrb.com/docs/themes/#overriding-theme-defaults

layout: default
title: Production Builds
nav_order: 2
has_children: false
parent: Home
---

# Production Builds

## Publishing Production Builds to Github

Production images are published to Github packages. In order to publish an image, you will need to create a Github personal access token with appropriate permissions to manage github packages (see Github Packages documentation for details). Then create local environment variables **GITHUB_USER** and **GITHUB_TOKEN** with your github user and personal access token.
Once authorization has been set up, the procedure to publish production builds is:

-   Stage and test any changes in the master branch
-   Update the app version number in `app/build.gradle` using semantic versioning conventions
-   Merge changes into the production branch
-   Build and publish changes to Github with the command gradle publish . The build.grade file has been configured to use the authentication information in the environment variables **GITHUB_USER** and **GITHUB_TOKEN** when attempting to publish.
-   Check that the packages with the updated version number are listed in Carnival Packages
-   Go back to the master branch, and `app/build.gradle` increment the version number and add the `-SNAPSHOT` suffix (i.e. `0.2.9-SNAPSHOT`)

For further details, see Configuring Gradle for use with GitHub Packages.
