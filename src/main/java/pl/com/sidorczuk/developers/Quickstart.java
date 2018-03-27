package pl.com.sidorczuk.developers;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.*;
import com.google.api.services.drive.Drive;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.StringTokenizer;

public class Quickstart {
	/** Application name. */
	private static final String APPLICATION_NAME = "Drive API Java Quickstart";

	/** Directory to store user credentials for this application. */
	private static final java.io.File DATA_STORE_DIR = new java.io.File(System.getProperty("user.home"),
			".credentials/drive-java-quickstart");

	/** Global instance of the {@link FileDataStoreFactory}. */
	private static FileDataStoreFactory DATA_STORE_FACTORY;

	/** Global instance of the JSON factory. */
	private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

	/** Global instance of the HTTP transport. */
	private static HttpTransport HTTP_TRANSPORT;

	private static Drive service;

	private static String destin_path;
	private static HashMap<String, String> md5tables = new HashMap<>();
	private static HashMap<String, String> md5tables_to_read = new HashMap<>();
	private static HashMap<String, String> dateModifiedtables = new HashMap<>();
	private static String md5filename;
	private static String dateModifiedfilename;
	private static java.io.File md5file;
	private static java.io.File dateModifiedfile;
	private static java.io.File fileError = new java.io.File("errors.log");
	private static PrintStream errorStream;

	/**
	 * Global instance of the scopes required by this quickstart.
	 *
	 * If modifying these scopes, delete your previously saved credentials at
	 * ~/.credentials/drive-java-quickstart
	 */
	private static final List<String> SCOPES = Arrays.asList(DriveScopes.DRIVE);

	static {
		try {
			HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
			DATA_STORE_FACTORY = new FileDataStoreFactory(DATA_STORE_DIR);
		} catch (Throwable t) {
			t.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * Creates an authorized Credential object.
	 * 
	 * @return an authorized Credential object.
	 * @throws IOException
	 */
	public static Credential authorize() throws IOException {
		// Load client secrets.
		InputStream in = Quickstart.class.getResourceAsStream("/client_secret.json");
		GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

		// Build flow and trigger user authorization request.
		GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY,
				clientSecrets, SCOPES).setDataStoreFactory(DATA_STORE_FACTORY).setAccessType("offline").build();
		Credential credential = new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
		System.out.println("Credentials saved to " + DATA_STORE_DIR.getAbsolutePath());
		return credential;
	}

	/**
	 * Build and return an authorized Drive client service.
	 * 
	 * @return an authorized Drive client service
	 * @throws IOException
	 */
	public static Drive getDriveService() throws IOException {
		Credential credential = authorize();
		return new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential).setApplicationName(APPLICATION_NAME).build();
	}

	public static void main(String[] args) {
		try {
			errorStream = new PrintStream(new FileOutputStream(fileError, true));
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		// Build a new authorized API client service.
		try {
			service = getDriveService();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logErrors(e);
		}
		String id_backup_folder;
		try {
			id_backup_folder = args[0];
			String source_path = args[1];
			destin_path = args[1];
			if (args.length == 3) {
				switch (args[2]) {
				case "generatechecksums":
					initChecskums(args[1]);
					break;
				case "synchronize":
					initSynchronize(id_backup_folder, source_path);
					break;
				case "clean":
					cleanlists();
					break;
				case "verify":
					defaultAction(id_backup_folder, source_path);
					// after process is finished let's make synchronization one more time to make sure that every file was uplaoded
					initChecskums(args[1]);
					initSynchronize(id_backup_folder, source_path);
					break;
				default:
					System.out.println("Wrong 3rd argument");
					System.exit(1);
				}
			} else {
				defaultAction(id_backup_folder, source_path);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logErrors(e);
		}
		errorStream.close();
	}
	
	public static void defaultAction(String id_backup_folder, String source_path) throws IOException {
		Path path_source_path = Paths.get(source_path);
		DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmm");
		Date date = new Date();
		String current_time = dateFormat.format(date);
		String first_folder_name = "backup_" + current_time + "_" + path_source_path.getFileName();
		String return_id = uploadItem(source_path, id_backup_folder, first_folder_name);
		if (return_id != null) {
			proceedFolder(source_path, return_id);
		}
	}

	public static void initSynchronize(String id_backup_folder, String source_path) throws IOException {
		// TODO Auto-generated method stub
		String pageToken = null;
		ArrayList<Long> listFolders = new ArrayList<>();
		java.io.File file_source = new java.io.File(source_path);
		String file_source_name = file_source.getName();
		do {
			FileList result = service.files().list()
					.setQ("'" + id_backup_folder + "'" + " in parents and trashed = false").setSpaces("drive")
					.setFields("nextPageToken, files(id, name, parents, trashed)").setPageToken(pageToken).execute();
			for (File file : result.getFiles()) {
				if (file.getName().contains(file_source_name)) {
					String date = file.getName().replace("backup_", "").replace("_" + file_source_name, "");
					try {
						long date_as_int = Long.parseLong(date);
						listFolders.add(date_as_int);
					} catch (NumberFormatException error) {
						error.printStackTrace();
						logErrors(error);
					}
				}
			}
			pageToken = result.getNextPageToken();
		} while (pageToken != null);
		Collections.sort(listFolders);
		if (listFolders.size() == 0) {
			System.out.println("Synchroniza folder was not found. Exiting...");
			System.exit(1);
		}
		String folder_name = "backup_" + listFolders.get(listFolders.size() - 1) + "_" + file_source_name;
		String pageToken2 = null;
		ArrayList<String> listOfDestinFolders = new ArrayList<>();
		do {
			FileList result = service.files().list()
					.setQ("'" + id_backup_folder + "'" + " in parents and trashed = false").setSpaces("drive")
					.setFields("nextPageToken, files(id, name, parents, trashed)").setPageToken(pageToken2).execute();
			for (File file : result.getFiles()) {
				if (folder_name.equals(file.getName())) {
					listOfDestinFolders.add(file.getId());
				}
			}
			pageToken2 = result.getNextPageToken();
		} while (pageToken2 != null);
		if (listOfDestinFolders.size() == 0) {
			System.out.println("Synchroniza folder was not found. Exiting...");
			System.exit(1);
		}
		if (listOfDestinFolders.size() > 1) {
			System.out.println("There is more than one folder to synchronize in backup folder. Exiting...");
			System.exit(1);
		}
		String destinID = listOfDestinFolders.get(0);
		md5filename = "md5_checksums_" + new java.io.File(destin_path).getName() + ".txt";
		md5file = new java.io.File(md5filename);
		Scanner scanner = new Scanner(md5file);
		scanner.useDelimiter(";|\\n");
		while (scanner.hasNext()) {
			md5tables_to_read.put(scanner.next(), scanner.next());
		}
		scanner.close();
		synchronize(destinID, source_path);
	}

	private static void logErrors(Exception error) {
		// TODO Auto-generated method stub
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date currDate = new Date();
		errorStream.println(dateFormat.format(currDate));
		error.printStackTrace(errorStream);
		errorStream.println("----------");
	}

	private static void logErrors(Exception error, String path) {
		// TODO Auto-generated method stub
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date currDate = new Date();
		errorStream.println(dateFormat.format(currDate));
		error.printStackTrace(errorStream);
		errorStream.println("File: " + path);
		errorStream.println("----------");
	}

	public static void synchronize(String destinID, String source_path) throws IOException {
		// TODO Auto-generated method stub
		java.io.File f = new java.io.File(source_path);
		ArrayList<java.io.File> files = new ArrayList<java.io.File>(Arrays.asList(f.listFiles()));
		List<File> google_drive_file_list = new ArrayList<File>();
		List<File> google_drive_directory_list = new ArrayList<File>();
		String pageToken = null;
		String query = "'" + destinID + "'" + " in parents"
				+ " and mimeType != 'application/vnd.google-apps.folder' and trashed = false";
		do {
			FileList result = service.files().list().setQ(query).setSpaces("drive")
					.setFields("nextPageToken, files(id, name, parents, trashed, md5Checksum)").setPageToken(pageToken)
					.execute();
			List<File> filesInGdrive = result.getFiles();
			if (!filesInGdrive.isEmpty()) {
				for (int i = 0; i < filesInGdrive.size(); i++) {
					google_drive_file_list.add(filesInGdrive.get(i));
				}
			}
			pageToken = result.getNextPageToken();
		} while (pageToken != null);
		pageToken = null;
		query = "'" + destinID + "'" + " in parents"
				+ " and mimeType = 'application/vnd.google-apps.folder' and trashed = false";
		do {
			FileList result = service.files().list().setQ(query).setSpaces("drive")
					.setFields("nextPageToken, files(id, name, parents, trashed, md5Checksum)").setPageToken(pageToken)
					.execute();
			List<File> filesInGdrive = result.getFiles();
			if (!filesInGdrive.isEmpty()) {
				for (int i = 0; i < filesInGdrive.size(); i++) {
					google_drive_directory_list.add(filesInGdrive.get(i));
				}
			}
			pageToken = result.getNextPageToken();
		} while (pageToken != null);
		outerloop: for (int i = 0; i < files.size(); i++) {
			String file_name = files.get(i).getName();
			List<File> list = null;
			boolean is_directory = files.get(i).isDirectory();
			if (is_directory) {
				list = google_drive_directory_list;
			} else {
				list = google_drive_file_list;
			}
			for (int j = 0; j < list.size(); j++) {
				String google_drive_file_name = list.get(j).getName();
				if (file_name.equals(google_drive_file_name)) {
					if (is_directory) {
						String google_drive_directory_id = list.get(j).getId();
						String newPath = files.get(i).getPath();
						synchronize(google_drive_directory_id, newPath);
						continue outerloop;
					} else {
						String google_drive_md5 = list.get(j).getMd5Checksum().trim();
						;
						String localChecksum = md5tables_to_read.get(files.get(i).getPath()).trim();
						if (!(localChecksum.equals(google_drive_md5))) {
							String google_drive_file_id = list.get(j).getId();
							service.files().delete(google_drive_file_id).execute();
							uploadItem(files.get(i).getPath(), destinID);
						}
						continue outerloop;
					}
				}
			}
			if (is_directory) {
				String newPath = files.get(i).getPath();
				String id = uploadItem(newPath, destinID);
				synchronize(id, newPath);
			} else {
				uploadItem(files.get(i).getPath(), destinID);
			}
		}
	}

	public static String uploadItem(String currentPath, String folder_id, String first_run_name) throws IOException {
		java.io.File currentFile = new java.io.File(currentPath);
		String id = null;
		File fileMetadata = new File();
		fileMetadata.setName(first_run_name);
		boolean is_directory = currentFile.isDirectory();
		ArrayList<String> parent = new ArrayList<String>();
		parent.add(folder_id);
		fileMetadata.setParents(parent);
		System.out.println("Uploading: " + currentPath);
		if (is_directory) {
			fileMetadata.setMimeType("application/vnd.google-apps.folder");
			File file = service.files().create(fileMetadata).setFields("id").execute();
			id = file.getId();
		} else {
			InputStreamContent mediaContent = new InputStreamContent("",
					new BufferedInputStream(new FileInputStream(currentFile)));
			mediaContent.setLength(currentFile.length());
			Drive.Files.Create req = service.files().create(fileMetadata, mediaContent);
			req.getMediaHttpUploader().setProgressListener(new CustomProgressListener());
			req.execute();
		}

		return id;
	}

	public static String uploadItem(String currentPath, String folder_id) throws IOException {
		java.io.File currentFile = new java.io.File(currentPath);
		String id = null;
		File fileMetadata = new File();
		String fileName = Paths.get(currentPath).getFileName().toString();
		fileMetadata.setName(fileName);
		boolean is_directory = currentFile.isDirectory();
		ArrayList<String> parent = new ArrayList<String>();
		parent.add(folder_id);
		fileMetadata.setParents(parent);
		System.out.println("Uploading: " + currentPath);
		if (is_directory) {
			fileMetadata.setMimeType("application/vnd.google-apps.folder");
			File file = service.files().create(fileMetadata).setFields("id").execute();
			id = file.getId();
		} else {
			InputStreamContent mediaContent = new InputStreamContent("",
					new BufferedInputStream(new FileInputStream(currentFile)));
			mediaContent.setLength(currentFile.length());
			Drive.Files.Create req = service.files().create(fileMetadata, mediaContent);
			req.getMediaHttpUploader().setProgressListener(new CustomProgressListener()).setChunkSize(1 * 1024 * 1024);
			try {
				req.execute();
			} catch (java.io.IOException e) {
				// stop program for 30 sec
				try {
					Thread.sleep(30000);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
					logErrors(e1, currentPath);
				}
				logErrors(e, currentPath);
				// check if file exists - if not, upload file again
				String pageToken = null;
				List<File> google_drive_file_list = new ArrayList<File>();
				do {
					FileList result = service.files().list()
							.setQ("'" + folder_id + "'"
									+ " in parents and mimeType != 'application/vnd.google-apps.folder' and trashed = false")
							.setSpaces("drive")
							.setFields("nextPageToken, files(id, name, parents, trashed, md5Checksum)")
							.setPageToken(pageToken).execute();
					List<File> filesInGdrive = result.getFiles();
					for (int i = 0; i < filesInGdrive.size(); i++) {
						if (fileName.equals(filesInGdrive.get(i).getName())) {
							google_drive_file_list.add(filesInGdrive.get(i));
						}
					}
					pageToken = result.getNextPageToken();
				} while (pageToken != null);
				if (google_drive_file_list.isEmpty()) {
					uploadItem(currentPath, folder_id);
				} else {
					String checksumFromGoogleDrive = google_drive_file_list.get(0).getMd5Checksum().trim();
					MessageDigest md;
					try {
						md = MessageDigest.getInstance("MD5");
						String localChecksum = getFileChecksum(md, new java.io.File(currentFile.getPath()));
						if ((localChecksum != null) && (!(checksumFromGoogleDrive.equals(localChecksum)))) {
							String idOfFile = google_drive_file_list.get(0).getId();
							service.files().delete(idOfFile).execute();
							uploadItem(currentPath, folder_id);
						}
					} catch (NoSuchAlgorithmException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
						logErrors(e1, currentPath);
					}
				}

			}
		}
		return id;
	}

	public static void proceedFolder(String local_path, String folder_id) {
		java.io.File f = new java.io.File(local_path);
		ArrayList<java.io.File> files = new ArrayList<java.io.File>(Arrays.asList(f.listFiles()));
		String current_id = null;
		for (int i = 0; i < files.size(); i++) {
			try {
				String current_path = files.get(i).getPath();
				current_id = uploadItem(current_path, folder_id);
				if (current_id != null) {
					proceedFolder(current_path, current_id);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				logErrors(e, files.get(i).getPath());
			}
		}
	}

	public static void generatechecksums(String local_path) throws FileNotFoundException, IOException {
		java.io.File f = new java.io.File(local_path);
		ArrayList<java.io.File> files = new ArrayList<java.io.File>(Arrays.asList(f.listFiles()));
		for (int i = 0; i < files.size(); i++) {
			String current_path = files.get(i).getPath();
			if (files.get(i).isDirectory()) {
				generatechecksums(current_path);
			} else {
				try {
					if (!dateModifiedtables.containsKey(current_path)) {
						long moddate = files.get(i).lastModified();
						dateModifiedtables.put(current_path, Long.toString(moddate));
						MessageDigest md = MessageDigest.getInstance("MD5");
						String checksum = getFileChecksum(md, new java.io.File(current_path));
						if (md5tables.containsKey(current_path)) {
							md5tables.replace(current_path, checksum);
						} else {
							md5tables.put(current_path, checksum);
						}
					}
					long currentModDate = files.get(i).lastModified();
					long tableModDate = Long.parseLong(dateModifiedtables.get(current_path).trim());
					if (currentModDate > tableModDate) {
						MessageDigest md = MessageDigest.getInstance("MD5");
						String checksum = getFileChecksum(md, new java.io.File(current_path));
						if (md5tables.containsKey(current_path)) {
							md5tables.replace(current_path, checksum);
						} else {
							md5tables.put(current_path, checksum);
						}
						dateModifiedtables.replace(current_path, Long.toString(currentModDate));
					}
				} catch (NoSuchAlgorithmException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					logErrors(e, current_path);
				}
			}
		}
	}

	public static void initChecskums(String local_path) throws IOException {
		md5filename = "md5_checksums_" + new java.io.File(destin_path).getName() + ".txt";
		md5file = new java.io.File(md5filename);
		dateModifiedfilename = "datemodified_" + new java.io.File(destin_path).getName() + ".txt";
		dateModifiedfile = new java.io.File(dateModifiedfilename);
		if (!md5file.exists()) {
			java.io.File file = new java.io.File(md5filename);
			file.createNewFile();
		}
		if (!dateModifiedfile.exists()) {
			java.io.File file = new java.io.File(dateModifiedfilename);
			file.createNewFile();
		}
		Scanner scanner = new Scanner(md5file);
		scanner.useDelimiter(";|\\n");
		while (scanner.hasNext()) {
			md5tables.put(scanner.next(), scanner.next());
		}
		scanner.close();
		Scanner scanner2 = new Scanner(dateModifiedfile);
		scanner2.useDelimiter(";|\\n");
		while (scanner2.hasNext()) {
			dateModifiedtables.put(scanner2.next(), scanner2.next());
		}
		scanner2.close();
		generatechecksums(local_path);
		savechecksums();
	}

	public static void savechecksums() throws IOException {
		// TODO Auto-generated method stub
		FileWriter fw = new FileWriter(md5filename);
		BufferedWriter bw = new BufferedWriter(fw);
		for (String path : md5tables.keySet()) {
			String lineToWrite = path + ";" + md5tables.get(path);
			bw.write(lineToWrite);
			bw.newLine();
		}
		bw.close();
		FileWriter fw2 = new FileWriter(dateModifiedfilename);
		BufferedWriter bw2 = new BufferedWriter(fw2);
		for (String path : dateModifiedtables.keySet()) {
			String lineToWrite = path + ";" + dateModifiedtables.get(path);
			bw2.write(lineToWrite);
			bw2.newLine();
		}
		bw2.close();
	}

	public static String getFileChecksum(MessageDigest digest, java.io.File file) throws IOException {
		// Get file input stream for reading the file content
		FileInputStream fis = new FileInputStream(file);

		// Create byte array to read data in chunks
		byte[] byteArray = new byte[1024];
		int bytesCount = 0;

		// Read file data and update in message digest
		while ((bytesCount = fis.read(byteArray)) != -1) {
			digest.update(byteArray, 0, bytesCount);
		}
		;

		// close the stream; We don't need it now.
		fis.close();

		// Get the hash's bytes
		byte[] bytes = digest.digest();

		// This bytes[] has bytes in decimal format;
		// Convert it to hexadecimal format
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < bytes.length; i++) {
			sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
		}

		// return complete hash
		return sb.toString();
	}
	
	public static void cleanlists() throws IOException {
		md5filename = "md5_checksums_" + new java.io.File(destin_path).getName() + ".txt";
		md5file = new java.io.File(md5filename);
		dateModifiedfilename = "datemodified_" + new java.io.File(destin_path).getName() + ".txt";
		dateModifiedfile = new java.io.File(dateModifiedfilename);
		if(dateModifiedfile.exists()) {
			Scanner scan = new Scanner(dateModifiedfile);
			ArrayList<String> file_time_list = new ArrayList<>();
			while(scan.hasNextLine()) {
				file_time_list.add(scan.nextLine());
			}
			scan.close();
			for(int i = 0; i < file_time_list.size(); i++) {
				StringTokenizer token = new StringTokenizer(file_time_list.get(i), ";");
				java.io.File current_file;
				if(token.hasMoreTokens()) {
					current_file = new java.io.File(token.nextToken());
					if(!current_file.exists()) {
						file_time_list.remove(i);
					}
				}
			}
			FileWriter fw = new FileWriter(dateModifiedfilename);
			BufferedWriter bw = new BufferedWriter(fw);
			for (String el : file_time_list) {
				bw.write(el);
				bw.newLine();
			}
			bw.close();
		}
		if(md5file.exists()) {
			Scanner scan = new Scanner(md5file);
			ArrayList<String> file_checksum_list = new ArrayList<>();
			while(scan.hasNextLine()) {
				file_checksum_list.add(scan.nextLine());
			}
			scan.close();
			for(int i = 0; i < file_checksum_list.size(); i++) {
				StringTokenizer token = new StringTokenizer(file_checksum_list.get(i), ";");
				java.io.File current_file;
				if(token.hasMoreTokens()) {
					current_file = new java.io.File(token.nextToken());
					if(!current_file.exists()) {
						file_checksum_list.remove(i);
					}
				}
			}
			FileWriter fw = new FileWriter(md5filename);
			BufferedWriter bw = new BufferedWriter(fw);
			for (String el : file_checksum_list) {
				bw.write(el);
				bw.newLine();
			}
			bw.close();
		}		
	}
}