# spring-integration-sftp-spring-boot
Showcases transfer of files from one remote SFTP server to another SFTP server using Spring Integration Capabilities

The application using Spring Boot 1.5.13 and maven dependencies relating to spring-integration-sftp

In order to use the application locally, we need to set up two remote SFTP server which can be easily downloaded using below link<br />
https://www.sftp.net/public-online-sftp-servers

The below properties need to modified in application.properties<br />

Remote host SFTP server properties

sftp.host.ip = 192.168.56.1 <br />
sftp.host.port = 22 <br />
sftp.host.user = tester<br />
sftp.host.password = password<br />
sftp.host.remote.directory.download = /<br />
sftp.local.directory.download = sftp-inbound<br />
sftp.host.remote.directory.download.filter = *.*<br />

Remote SFTP server dest properties<br />

sftp.dest.ip = 192.168.190.1<br />
sftp.dest.port = 25<br />
sftp.dest.user = tester<br />
sftp.dest.password = password<br />
sftp.dest.remote.directory = /<br />

Note**<br />
No private key is being used however few parts of the code have been commented so as to use private key and paraphrase and just properties need to be added in application properties.
