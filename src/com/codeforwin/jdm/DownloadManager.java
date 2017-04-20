package com.codeforwin.jdm;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Observable;

import javax.swing.JOptionPane;

/**
 * Manages download of single file.
 *
 * @author Pankaj Prakash
 *
 */
public class DownloadManager extends Observable implements Runnable {

	private final static int MAX_WORKER	= 8;
	private final static int MIN_WORKER_DOWNLOAD_SIZE = 1024 * 1024; // 1MB

	private long downloadId;

	private DownloadWorker[] workers		= null;
	private Thread[] 		 workerThreads	= null;
	private int 			 workersAtWork	= 0;

	private String 	downloadURL			= null;
	private String 	filePath			= "";

	private long 	downloadSize		= 0l;
	private Long 	downloadCompleted	= 0l;
	private float	downloadSpeed		= 0.0f;

	private DownloadStatus 	 status		= null;
	private DownloadMetadata metadata	= null;
	private List<DownloadPartsMetadata>	partsMetaList = null;


	/**
	 * Use this constructor for new download with default download path.
	 * @param downloadURL URL to download.
	 */
	public DownloadManager(String downloadURL) throws IOException {
		this(downloadURL, Configuration.DEFAULT_DOWNLOAD_PATH + "");
	}


	/**
	 * Use this constructor for new download.
	 * @param downloadURL URL to download.
	 */
	public DownloadManager(String downloadURL, String filePath) {
		workers 		= new DownloadWorker[MAX_WORKER];
		workerThreads 	= new Thread[MAX_WORKER];

		this.downloadURL	= downloadURL;
		status			= DownloadStatus.NEW;
		this.filePath		= filePath;

		metadata = new DownloadMetadata(downloadURL);

		downloadId = metadata.getId();
	}


	/**
	 * Use this constructor for saved downloads.
	 * @param downloadId Unique download id.
	 */
	public DownloadManager(DownloadMetadata metadata, List<DownloadPartsMetadata> partsMeta) {
		workers		= new DownloadWorker[MAX_WORKER];
		workerThreads	= new Thread[MAX_WORKER];

		this.metadata		= metadata;
		downloadURL	= metadata.getUrl();

		partsMetaList = new ArrayList<>(partsMeta);

		downloadId 	= metadata.getId();
		downloadSize	= metadata.getFileSize();
		status 		= metadata.getStatus();

		workersAtWork	= partsMeta.size();

		downloadCompleted	= metadata.getCompleted();
	}


	/**
	 * Start a new download
	 */
	@Override
	public void run() {
		try {
			if (status != DownloadStatus.PAUSED) {
				downloadFileMetaInformation();
				updateDownloadStatus();
			}

			employWorkers();

		} catch (IOException e) {
			System.out.println("[ERROR] " + e.getMessage());

			status = DownloadStatus.ERROR;
			updateDownloadStatus();

			return;
		}

		status = DownloadStatus.DOWNLOADING;

		// Run a new thread to compute download speed
		initDownloadSpeedCalculator();

		// Start all worker download threads
		startAllWorkers();

		// Wait for workers to end
		waitForWorkers();

		// Finally update the GUI
		updateDownloadGUI();

		// Merge the file if download completed
		if(downloadCompleted == downloadSize) {
			status = DownloadStatus.COMPLETED;

			mergeDownloadFile();
		}

		updateDownloadStatus();
	}


	/**
	 * Pause the download.
	 */
	public void pause() {
		status = DownloadStatus.PAUSED;
	}


	/**
	 * Returns the current download status.
	 * @return
	 */
	public DownloadStatus getStatus() {
		return status;
	}

	/**
	 * Returns the download metadata
	 * @return
	 */
	public DownloadMetadata getMetadata() {
		return metadata;
	}

	/**
	 * Returns the total number of bytes downloaded
	 * @return
	 */
	public long getDownloadCompleted() {
		return downloadCompleted;
	}

	/**
	 * @return the unique download Id.
	 */
	public long getDownloadId() {
		return downloadId;
	}

	/**
	 * @return the current download speed.
	 */
	public float getDownloadSpeed() {
		return downloadSpeed;
	}

	/**
	 * Initializes the download speed calculator. It continuously monitors the download speed.
	 */
	private void initDownloadSpeedCalculator() {
		Thread t = new Thread(()->{
			final Date startTime = new Date();
			Date endTime;

			while(status == DownloadStatus.DOWNLOADING) {
				endTime = new Date();
				if(downloadCompleted != 0)
					downloadSpeed = downloadCompleted / ((endTime.getTime() - startTime.getTime()) / 1000.0f) ;

				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					break;
				}
			}
		});
		t.setName(downloadId + "Speed");
		t.start();
	}


	/**
	 * Fetches the basic file metadata information from the download server.
	 * @throws IOException
	 */
	private void downloadFileMetaInformation() throws IOException {
		metadata.getLoadFileMetadata();

		if(metadata == null)
			throw new IOException("Unable to get download file information.");

		status	= DownloadStatus.READY;
		downloadCompleted	= 0l;
		downloadSize		= metadata.getFileSize();
		downloadId	= metadata.getId();

		filePath += metadata.getFileName() + "." + metadata.getFileType();

		metadata.setFilePath(filePath);
		metadata.setStatus(status);
		metadata.setStartTime(new Date());
		metadata.setEndTime(new Date());
	}


	/**
	 * Performs download split task. It initializes all worker and its individual threads.
	 * @throws IOException
	 */
	private void employWorkers() throws IOException {
		// If it is a new download
		if (partsMetaList == null) {
			// Create new part meta list
			partsMetaList = new ArrayList<>();

			// Calculate number of parts to be done
			int parts = (int) (downloadSize / MIN_WORKER_DOWNLOAD_SIZE);

			int totalParts = (parts > MAX_WORKER) ? MAX_WORKER : parts;

			long partDownloadSize = downloadSize / totalParts;
			long startRange = 0l;
			long endRange 	= 0l;
			for (int i = 0; i < totalParts; i++) {
				endRange = (i == totalParts - 1) ? downloadSize : startRange + partDownloadSize;

				// Generate a temporary part file path
				String tempPartFile = Configuration.TEMP_DIRECTORY + metadata.getId() + ".part" + i;

				// Create a download partition metadata
				DownloadPartsMetadata partMeta = new DownloadPartsMetadata(metadata.getId(), i, startRange, endRange, tempPartFile);

				// Add the new part meta to list
				partsMetaList.add(partMeta);

				// Employ the worker
				workers[i] = new DownloadWorker(partMeta);

				startRange += partDownloadSize + 1;
			}

			workersAtWork = totalParts;

		} else {
			// Initialize all workers with previous part data
			int i = 0;
			for (DownloadPartsMetadata partMeta : partsMetaList) {
				workers[i] = new DownloadWorker(partMeta);
				i++;
			}

			workersAtWork = partsMetaList.size();
		}

		/*
		 * Initialize all worker threads.
		 */
		for(int i=0; i<workersAtWork; i++) {
			Thread t = new Thread(workers[i]);
			t.setName(metadata.getId() + "" + (i+1));

			workerThreads[i] = t;
		}
	}

	/**
	 * Starts all worker threads.
	 */
	private void startAllWorkers()  {
		System.out.println("[INFO] Starting download threads.");

		for(int i=0; i<workersAtWork; i++)
			workerThreads[i].start();
	}


	/**
	 * Joins all workers threads and wait until they complete.
	 */
	private void waitForWorkers() {
		for(int i=0; i<workersAtWork; i++) {

			try {
				workerThreads[i].join();
			} catch (InterruptedException e) {
				System.out.println("[WARN] Download worker thread was interrupted.");
			}
		}
	}


	/**
	 * Updates the download status to XML.
	 */
	private void updateDownloadStatus() {
		XMLFactory factory = XMLFactory.newXMLFactory();

		try {
			metadata.setEndTime(new Date());
			metadata.setStatus(status);
			metadata.setCompleted(downloadCompleted);
			factory.updateDownloadList(metadata);
		} catch (IOException e) {
			System.out.println("[ERROR] Unable to update download metadata to file. " + e.getMessage());
		}

		setChanged();
		notifyObservers(true);
	}


	/**
	 * Updates the download part file status to XML.
	 * @param downWorker
	 */
	private synchronized void updatePartDownloadStatus(DownloadWorker downWorker) {
		XMLFactory factory = XMLFactory.newXMLFactory();

		DownloadPartsMetadata meta = downWorker.getPartMeta();

		long startRange		= meta.getStart();
		long totalDownloaded= downWorker.getCompleted();
		long downloadSize 	= downWorker.getDownloadSize();

		boolean completed = (totalDownloaded == downloadSize);

		// Update the start range of current part
		meta.setStart(startRange + totalDownloaded);

		try {
			if (completed)
				factory.removeSavedDownloadParts(meta);
			else
				factory.updateSavedDownloadParts(meta);
		} catch (IOException e) {
			System.out.println("[ERROR] Unable to update the download part XML. " + e.getMessage());
		}

		setChanged();
		notifyObservers();
	}

	/**
	 * Updates the GUI with new download status
	 */
	private synchronized void updateDownloadGUI() {
		setChanged();
		notifyObservers();
	}


	/**
	 * Merges the final part file in single usable file.
	 */
	private void mergeDownloadFile() {
		String filePath = metadata.getFilePath();

		System.out.println("[WAIT] Merging files please wait.");

		// Sort all parts before merging
		Collections.sort(partsMetaList, new Comparator<DownloadPartsMetadata>(){
			@Override
			public int compare(DownloadPartsMetadata o1, DownloadPartsMetadata o2) {
				if(o1.getId() > o2.getId())
					return 1;
				else if(o1.getId() == o2.getId())
					return 0;
				else
					return -1;
			}
		});


		/*
		 * Merge all parts to single file.
		 */
		try(BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath))) {
			for(DownloadPartsMetadata partMeta : partsMetaList) {
				String partPath = partMeta.getPath();

				try (FileInputStream fis = new FileInputStream(partPath)) {
					final int BUFFER_SIZE = 1024;

					byte[] buff = new byte[BUFFER_SIZE];
					int len;

					while((len = fis.read(buff)) != -1) {
						bos.write(buff, 0, len);
						bos.flush();
					}
				}
			}

			System.out.println("[SUCCESS] File merged successfully at - " + filePath);
		} catch (IOException e) {
			System.out.println("[ERROR] Unable to merge file at the given location.");
		}

		/*
		 * Remove the temporary part files
		 */
		partsMetaList.forEach((e)->{
			File file = new File(e.getPath());

			if(file.exists())
				file.delete();
		});
	}



	/**
	 * Manages individual block of a file.
	 * @author Pankaj Prakash
	 *
	 */
	private class DownloadWorker implements Runnable {
		private final static int BUFFER_SIZE = 1024 * 250; // 250 KB

		private final String TEMP_PATH;

		private DownloadPartsMetadata partMeta;

		@SuppressWarnings("unused")
		private int part;
		private long startRange, endRange;
		private long downloadSize;
		private long completed;

		private byte[] buffer;

		/**
		 * Create new instance of Download worker with file part metadata information.
		 * @param partMeta
		 * @throws IOException
		 */
		public DownloadWorker(DownloadPartsMetadata partMeta) throws IOException {
			this.partMeta	= partMeta;

			startRange = partMeta.getStart();
			endRange	= partMeta.getEnd();
			downloadSize=endRange - startRange;

			completed	= 0l;
			buffer 	= new byte[BUFFER_SIZE];
			part		= partMeta.getId();

			TEMP_PATH = partMeta.getPath();

			/*
			 * If temporary download file does not exists then create it
			 */
			File tempFile = new File(TEMP_PATH);
			if(!tempFile.exists()) {
				try {
					tempFile.createNewFile();
				} catch(IOException e) {
					throw new IOException("Unable to create temporary download part file.", e);
				}
			}
		}


		/**
		 * Starts the download worker. It starts downloading all parts of the file parallely.
		 */
		@Override
		public void run() {
			// Update the changes to XML file
			updatePartDownloadStatus(this);

			try {
				/* Create an open an HTTP connection */
				URL link = new URL(downloadURL);
				HttpURLConnection conn = (HttpURLConnection) link.openConnection();

				/* Set connection properties */
				conn.setRequestProperty("User-Agent", "Mozilla/5.0");
				conn.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

				if (metadata.isRangeAllowed())
					conn.setRequestProperty("Range", "bytes=" + startRange + "-" + endRange);

				final int responseCode = conn.getResponseCode();

				/* If connected to server then start the download process. */
				if (responseCode == HttpURLConnection.HTTP_OK ||
					responseCode == HttpURLConnection.HTTP_PARTIAL) {

					downloadFilePart(conn.getInputStream());

				} else {
					throw new IOException("Could not connect with server. " + responseCode);
				}

			} catch (MalformedURLException e) {
				System.out.println("[ERROR] Invalid download URL. " + e.getMessage());

				JOptionPane.showMessageDialog(null, "Invalid download URL. " + e.getMessage(), "Error downloading", JOptionPane.ERROR_MESSAGE);

			} catch (IOException e) {
				status = DownloadStatus.ERROR;

				System.out.println("[ERROR] Unable to download part. " + e.getMessage());

				JOptionPane.showMessageDialog(null, e.getMessage(), "Error downloading", JOptionPane.ERROR_MESSAGE);
			}

			/*
			 * Update the download part file XML
			 */
			updatePartDownloadStatus(this);
		}


		/**
		 * Download bytes of the individual file block.
		 */
		private void downloadFilePart(InputStream stream) throws IOException {
			int len;

			try (FileOutputStream writer = new FileOutputStream(TEMP_PATH, true)) {
				while ((len = stream.read(buffer)) != -1) {

					// write contents to file and flush the buffer
					writer.write(buffer, 0, len);
					writer.flush();

					completed += len;
					synchronized (downloadCompleted) {
						downloadCompleted += len;
					}

					updateDownloadGUI();

					// Stop the download if download manager is paused
					if(status == DownloadStatus.PAUSED || status == DownloadStatus.ERROR)
						break;
				}
			} catch (IOException e) {
				status = DownloadStatus.ERROR;

				System.out.println("[ERROR] Unable to read file contents. " + e.getMessage());

				updatePartDownloadStatus(this);

				throw e;
			} finally {
				stream.close();
			}
		}


		/**
		 * @return the bytes downloaded
		 */
		public long getCompleted() {
			return completed;
		}

		/**
		 * @return the file part metadata information
		 */
		public DownloadPartsMetadata getPartMeta() {
			return partMeta;
		}

		/**
		 * @return the download size
		 */
		public long getDownloadSize() {
			return downloadSize;
		}
	}
}