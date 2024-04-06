# get-wordy-core

## Core module for Get Wordy App

Since version 1.1.x the Postgres database is used as a data storage instead of mysql.
Integration tests are executed against real database.

[flyway-maven-plugin](https://documentation.red-gate.com/flyway/flyway-cli-and-api/usage/maven-goal) is configured to
run schema upgrades automatically on [test] phase

### Build instructions:

1. The following profile [with-integration-tests] should be enabled during maven build or added to settings.xml with
   your own credentials, for example:
   ```
   <settings>
       ...
       <profiles>
           ...
           <profile>
               <id>with-integration-tests</id>
               <activation>
                   <activeByDefault>true</activeByDefault>
               </activation>
               <properties>
                   <get.wordy.jdbc.url>jdbc:postgresql://localhost:5432/get_wordy_test?schema=public</get.wordy.jdbc.url>
                   <get.wordy.jdbc.user>postgres</get.wordy.jdbc.user>
                   <get.wordy.jdbc.password>postgres</get.wordy.jdbc.password>
               </properties>
           </profile>
           ...
       </profiles>
       ...
   </settings>
   ```
2. Profiles can be activated also in the Maven settings, via the <activeProfiles> section:
   ```
   <settings>
       ...
       <activeProfiles>
           <activeProfile>with-integration-tests</activeProfile>
       </activeProfiles>
       ...
   </settings>
   ```
3. Add username and password in your settings.xml if needed. Just ensure that settingsKey is configured in your POM:
   ```
   <settings>
       ...
       <servers>
           <server>
               <id>sensibleKey</id>
               <username>user</username>
               <password>password</password>
           </server>
       </servers>
       ...
   </settings>
   ```
