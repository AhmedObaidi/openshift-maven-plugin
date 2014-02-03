openshift-maven-plugin
======================

A maven plugin to deploy war file to Openshift

Add to your pom.xml

```xml
...
    <build>
        .....
        <plugins>
            <plugin>
                <groupId>openshift</groupId>
                <artifactId>openshift-maven-plugin</artifactId>
                <version>1.0-SNAPSHOT</version>
                <configuration>
                    <!-- this is example destination choose yours -->
                    <destination>app-root/data/wildfly-8.0.0.CR1/standalone/deployments/</destination>
                    <host>REMOTEHOST</host>
                    <!-- user name from the ssh  -->
                    <user>52ed3d44d0b3cd44a0000513</user>
                    <password></password>
                    <!-- path to your id_rsa file used by ssh to connect to openshift -->
                    <keyFilePath>/home/user/.ssh/id_rsa</keyFilePath>
                </configuration>
            </plugin>
....            
        
```
run 
<code>
mvn openshift:deploy
</code>
