package com.constructor.client;

import org.apache.commons.net.ftp.FTPSClient;
import org.apache.commons.net.util.TrustManagerUtils;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.apache.commons.net.PrintCommandListener;
import org.apache.commons.net.ftp.FTP;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

//@Component
public class ConstructorFTPSUploader implements CommandLineRunner {

	//
	// to create ftp cred
	// https://app.constructor.io/dashboard/integration/ftp_credentials
	//
	//

	private static final String SERVER = "ftp.cnstrc.com";
	private static final int PORT = 21;
	private static final String USER = "jimmy_kongoli@epam.com(Partner Sandbox: EPAM)";
//	private static final String USER = "jimmy_kongoli@epam.com";
	private static final String PASSWORD = "Test123456789$";

	private static final String ITEM_EN = "target/output/index_en/item.jsonl";
	private static final String VARIANT_EN = "target/output/index_en/variations.jsonl";

	private static final String ARCHIVE_FOLDER = "target/constructor_upload/";
	private static final String ARCHIVE_PREFIX = "key_on1j1t2BjFymbXpC_Products_sync_";

	private static final String PATH_FR = "target/output/index_fr/item.jsonl";

	@Override
	public void run(String... args) throws Exception {
	    String archiveFileName = generateArchiveFileName();
	    File outputTarGz = new File(ARCHIVE_FOLDER + archiveFileName);
	    outputTarGz.getParentFile().mkdirs();

	    List<File> filesToArchive = Arrays.asList(new File(ITEM_EN), new File(VARIANT_EN));

	    System.out.println("📦 Creating archive: " + outputTarGz.getName());
	    TarGzUtils.createTarGzArchive(filesToArchive, outputTarGz);
	    System.out.println("✅ Archive created: " + outputTarGz.getAbsolutePath());

	    if (!outputTarGz.exists() || outputTarGz.length() == 0) {
	        throw new IllegalStateException("Archive file is missing or empty: " + outputTarGz.getAbsolutePath());
	    }

	    System.out.println("🔄 Preparing to upload: " + outputTarGz.getName() + " (" + outputTarGz.length() + " bytes)");

	    FTPSClient ftps = new FTPSClient("TLS", false); // explicit FTPS (correct) // true = implicit mode

	    try {
	        ftps.addProtocolCommandListener(new PrintCommandListener(System.out));
	        ftps.setTrustManager(TrustManagerUtils.getAcceptAllTrustManager());
	        ftps.setRemoteVerificationEnabled(false);

	        ftps.connect(SERVER, PORT);
	        System.out.println("🔐 Connected to " + SERVER);

	        if (!ftps.login(USER, PASSWORD)) {
	            throw new IOException("Login failed");
	        }

	        ftps.execPBSZ(0);
	        ftps.execPROT("P");

	        ftps.enterLocalPassiveMode();
	        ftps.setFileType(FTP.BINARY_FILE_TYPE);

	        try (
	            FileInputStream input = new FileInputStream(outputTarGz);
	            OutputStream output = ftps.storeFileStream(outputTarGz.getName())
	        ) {
	            if (output == null) throw new IOException("FTPS stream was null");

	            byte[] buffer = new byte[4096];
	            int bytesRead;
	            while ((bytesRead = input.read(buffer)) != -1) {
	                output.write(buffer, 0, bytesRead);
	            }

	            output.flush();
	        }

	        boolean completed = ftps.completePendingCommand();
	        if (completed) {
	            System.out.println("✅ File uploaded to Constructor as: " + outputTarGz.getName());
	        } else {
	            throw new IOException("Upload did not complete successfully");
	        }

	        ftps.logout();
	    } catch (IOException e) {
	        System.err.println("❌ Error: " + e.getMessage());
	        e.printStackTrace();
	    } finally {
	        if (ftps.isConnected()) {
	            try {
	                ftps.disconnect();
	            } catch (IOException ex) {
	                ex.printStackTrace();
	            }
	        }
	    }

	    System.out.println("🏁 Upload process complete.");
	    System.exit(0);
	}

	private static String generateArchiveFileName() {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");
		String timestamp = LocalDateTime.now().format(formatter);
		return ARCHIVE_PREFIX + timestamp + ".tar.gz";
	}

}
