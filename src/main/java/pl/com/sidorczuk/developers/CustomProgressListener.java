package pl.com.sidorczuk.developers;
import java.io.IOException;

import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener;

public class CustomProgressListener implements MediaHttpUploaderProgressListener {

	public void progressChanged(MediaHttpUploader uploader) throws IOException {
		switch (uploader.getUploadState()) {
		case NOT_STARTED:
			break;
		case INITIATION_STARTED:
			// System.out.println("Initiation has started!");
			break;
		case INITIATION_COMPLETE:
			// System.out.println("Initiation is complete!");
			break;
		case MEDIA_IN_PROGRESS:
			System.out.print("Uploading progress: ");
			System.out.printf("%.2f", uploader.getProgress() * 100);
			System.out.print("%\r");
			break;
		case MEDIA_COMPLETE:
			System.out.println("Upload is complete! 100.00%");
		}
	}
}
