package com.sftpapplication.config;

import java.io.File;

//import org.springframework.batch.poller.Poller;
import org.springframework.integration.annotation.Poller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.InboundChannelAdapter;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.file.FileNameGenerator;
import org.springframework.integration.file.filters.AcceptOnceFileListFilter;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.sftp.filters.SftpSimplePatternFileListFilter;
import org.springframework.integration.sftp.inbound.SftpInboundFileSynchronizer;
import org.springframework.integration.sftp.inbound.SftpInboundFileSynchronizingMessageSource;
import org.springframework.integration.sftp.outbound.SftpMessageHandler;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;

import com.jcraft.jsch.ChannelSftp.LsEntry;

@Configuration
public class SftpConfig {

	// For Uploading the file on remote server, we need to create a Messaging
	// Gateway
	@Autowired
	private UploadGateway gateway;

	// Properties of Remote Host

	@Value("${sftp.host.ip}")
	private String sftpHostIp;

	@Value("${sftp.host.port}")
	private int sftphostPort;

	@Value("${sftp.host.user}")
	private String sftpHostUser;

	// Further Addition on private key and private key paraphrase in case needed

	/*
	 * @Value("${sftp.privateKey:#{null}}") private Resource sftpPrivateKey;
	 * 
	 * @Value("${sftp.privateKeyPassphrase:}") private String
	 * sftpPrivateKeyPassphrase;
	 */

	@Value("${sftp.host.password}")
	private String sftpHostPassword;

	@Value("${sftp.host.remote.directory.download}")
	private String sftpRemoteDirectoryDownloadHost;

	// @Value("${sftp.local.directory.download:${java.io.tmpdir}/localDownload}")

	// Local Directory for Download
	@Value("${sftp.local.directory.download}")
	private String sftpLocalDirectoryDownload;

	// @Value("${sftp.remote.directory.download.filter:*.*}")
	@Value("${sftp.host.remote.directory.download.filter}")
	private String sftpRemoteDirectoryDownloadFilter;

	// Properties of Remote Destination

	@Value("${sftp.dest.ip}")
	private String sftpDestIp;

	@Value("${sftp.dest.port}")
	private int sftpDestPort;

	@Value("${sftp.dest.user}")
	private String sftpDestUser;

	@Value("${sftp.dest.password}")
	private String sftpDestPassword;

	@Value("${sftp.dest.remote.directory}")
	private String sftpRemoteDestDirectory;

	/*
	 * The SftpSessionFactory creates the sftp sessions.  
	 * This is where you define the host , user and key information for your sftp server.
	 */
	// Creating session for Remote Destination SFTP server Folder
	@Bean
	public SessionFactory<LsEntry> sftpSessionFactoryDestination() {
		DefaultSftpSessionFactory factory = new DefaultSftpSessionFactory(true);
		/*
		 * factory.setHost("192.168.190.1"); factory.setPort(25);
		 * factory.setUser("tester"); factory.setPassword("password");
		 * factory.setAllowUnknownKeys(true);
		 */
		factory.setHost(sftpDestIp);
		factory.setPort(sftpDestPort);
		factory.setUser(sftpDestUser);
		factory.setPassword(sftpDestPassword);
		factory.setAllowUnknownKeys(true);
		return new CachingSessionFactory<LsEntry>(factory);
	}

	// Creating session for Source SFTP server Folder
	@Bean
	public SessionFactory<LsEntry> sftpSessionFactory() {
		DefaultSftpSessionFactory factory = new DefaultSftpSessionFactory(true);
		/*
		 * factory.setHost("192.168.56.1"); factory.setPort(22);
		 * factory.setUser("tester"); factory.setPassword("password");
		 * factory.setAllowUnknownKeys(true);
		 */

		factory.setHost(sftpHostIp);
		factory.setPort(sftphostPort);
		factory.setUser(sftpHostUser);
		factory.setPassword(sftpHostPassword);
		factory.setAllowUnknownKeys(true);
		return new CachingSessionFactory<LsEntry>(factory);
	}

	
	/*
	 * The SftpInboundFileSynchronizer uses the session factory that we defined above. 
	 * Here we set information about the remote directory to fetch files from.
	 * We could also set filters here to control which files get downloaded
	 */
	@Bean
	public SftpInboundFileSynchronizer sftpInboundFileSynchronizer() {
		SftpInboundFileSynchronizer fileSynchronizer = new SftpInboundFileSynchronizer(sftpSessionFactory());
		fileSynchronizer.setDeleteRemoteFiles(false);
		fileSynchronizer.setRemoteDirectory(sftpRemoteDirectoryDownloadHost);
		fileSynchronizer.setFilter(new SftpSimplePatternFileListFilter(sftpRemoteDirectoryDownloadFilter));
		return fileSynchronizer;
	}

	/*
	 * The Message source bean uses the @InboundChannelAdapter annotation.
	 * This message source connects the synchronizer we defined above to a message queue (sftpChannel). 
	 * The adapter will take files from the sftp server and place them in the message queue as messages
	 */
	@Bean
	@InboundChannelAdapter(channel = "sftpChannel", poller = @Poller(fixedDelay = "5000"))
	public MessageSource<File> sftpMessageSource() {
		SftpInboundFileSynchronizingMessageSource source = new SftpInboundFileSynchronizingMessageSource(
				sftpInboundFileSynchronizer());
		//source.setLocalDirectory(new File("sftp-inbound"));
		source.setLocalDirectory(new File(sftpLocalDirectoryDownload));
		source.setAutoCreateLocalDirectory(true);
		source.setLocalFilter(new AcceptOnceFileListFilter<File>());
		return source;
	}

	/*
	 * The message consumer is where you get to process the files that are downloaded. 
	 * Here we have sent it using message gateway through the upload method to remote SFTP folder
	 */
	@Bean
	@ServiceActivator(inputChannel = "sftpChannel")
	public MessageHandler handler() {
		return new MessageHandler() {

			@Override
			public void handleMessage(Message<?> message) throws MessagingException {

				System.out.println("now sending the payload: " + message.getPayload());
				gateway.upload((File) message.getPayload());
				System.out.println("sent the payload");

			}

		};
	}

	/*
	 * Message handler for Outbound Adapter
	 * so that we can send it to remote destination directory on SFTP server
	 */
	@Bean
	@ServiceActivator(inputChannel = "toSftpChannel")
	public MessageHandler handlerSend() {
		SftpMessageHandler handler = new SftpMessageHandler(sftpSessionFactoryDestination());
		handler.setRemoteDirectoryExpression(new LiteralExpression(sftpRemoteDestDirectory));
		handler.setFileNameGenerator(new FileNameGenerator() {
			@Override
			public String generateFileName(Message<?> message) {
				if (message.getPayload() instanceof File) {
					System.out.println("message payload sending now " + message.getPayload());
					return ((File) message.getPayload()).getName();
				} else {
					throw new IllegalArgumentException("File expected as payload.");
				}
			}
		});
		return handler;
	}

	/*
	 * Gateway
	 */
	@MessagingGateway
	public interface UploadGateway {

		@Gateway(requestChannel = "toSftpChannel")
		void upload(File file);

	}

}
