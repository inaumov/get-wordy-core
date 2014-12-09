GetWordy-core
========

Core module for GetWordy App.

Integration tests are run on MySql database.

Build instructions:

1. The following profile should be added to maven settings.xml:

<settings>
    [...]
    <profiles>
        [...]
        <profile>
            <id>get_wordy_dev</id>
            <activation>
                <activeByDefault>true</activeByDefault>
            </activation>
            <properties>
                <get.wordy.jdbc.driver>com.mysql.jdbc.Driver</get.wordy.jdbc.driver>
                <get.wordy.jdbc.url>jdbc:mysql://localhost:3306</get.wordy.jdbc.url>
                <get.wordy.jdbc.database>get_wordy_test</get.wordy.jdbc.database>
                <get.wordy.jdbc.username>user</get.wordy.jdbc.username>
                <get.wordy.jdbc.password>password</get.wordy.jdbc.password>
            </properties>
        </profile>
        [...]
    </profiles>
    [...]
</settings>

2. Profiles can be activated also in the Maven settings, via the <activeProfiles> section:

<settings>
    [...]
    <activeProfiles>
        <activeProfile>get_wordy_dev</activeProfile>
    </activeProfiles>
    [...]
</settings>

3. Add username and password in your settings.xml. Just ensure that you configure settingsKey in your POM:

<settings>
    [...]
    <servers>
        <server>
            <id>sensibleKey</id>
            <username>user</username>
            <password>password</password>
        </server>
        [...]
    </servers>
    [...]
</settings>