package com.codeforwin.jdm;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

public class DownloadPool extends Thread implements Observer {
	private static DownloadPool pool;

	private List<DownloadManager> downloadManagers;
	private List<Thread> 		  downloadThreads;
	private List<DownloadManager> removedManagers;


	/**
	 * Only single instance of the class is possible through <code>pool</code> object.
	 */
	private DownloadPool() {
		downloadManagers = new ArrayList<>();
		removedManagers  = new ArrayList<>();
		downloadThreads  = new ArrayList<>();
	}


	/**
	 * Get the reference of download pool.
	 * @return
	 */
	public static DownloadPool getDownloadPool() {
		if(pool == null)
			pool = new DownloadPool();

		return pool;
	}


	/**
	 * Gets, a download manager if exists in the download pool.
	 * @param downloadId
	 * @return Returns reference of DownloadManager if exists in download pool, otherwise null.
	 */
	public DownloadManager get(long downloadId) {
		for(DownloadManager manager : downloadManagers) {
			if(manager.getDownloadId() == downloadId)
				return manager;
		}

		return null;
	}


	/**
	 * Add a DownloadManager to the download pool. Calling this automatically starts the DownloadManager.
	 * @param manager
	 */
	public synchronized void add(DownloadManager manager) {
		downloadManagers.add(manager);
		manager.addObserver(this);
		start(manager);
	}

	/**
	 * Starts the DownloadManager on a new download Thread.
	 * @param manager
	 */
	private void start(DownloadManager manager) {
		if(manager != null) {
			Thread t = new Thread(manager);
			t.setName(manager.getDownloadId() + "");

			downloadThreads.add(t);

			t.start();
		}
	}


	/**
	 * Removes the DownloadManager from the download pool. Calling this will automatically pause the active download.
	 * Calling this is similar to calling <code>remove(manager, true)</code>
	 * @param manager
	 * @see remove(DownloadManager manager, boolean stop)
	 */
	public synchronized void remove(DownloadManager manager) {
		remove(manager, true);
	}


	/**
	 * Removes the DownloadManager from the download pool.
	 * @param manager
	 * @param stop true if you want to pause the download if in progress, otherwise false.
	 */
	public synchronized void remove(DownloadManager manager, boolean stop) {
		if(manager != null) {
			if(stop)
				manager.pause();
			else {
				removedManagers.add(manager);
				downloadManagers.remove(manager);
			}
		}
	}

	/**
	 * Removes the DownloadManager from the download pool. Calling this will automatically pause the active download.
	 * Calling this is similar to calling <code>remove(manager, true)</code>
	 * @param manager
	 * @param downloadId
	 * @see remove(DownloadManager manager, boolean stop)
	 */
	public synchronized void remove(long downloadId) {
		DownloadManager manager = get(downloadId);
		remove(manager);
	}

	/**
	 * Pauses all active downloads and removes from the download pool.
	 */
	public synchronized void removeAll() {
		List<Long> ids = new ArrayList<>();

		downloadManagers.forEach((manager)->{
			ids.add(manager.getDownloadId());
		});

		ids.forEach((id)-> {
			remove(id);
		});
	}


	@SuppressWarnings("unused")
	private Thread getDownloadThread(long downloadId) {
		for(Thread t : downloadThreads) {
			if(t.getName().equals(downloadId + ""))
				return t;
		}

		return null;
	}

	/**
	 * @return Returns total active downloads at the current moment.
	 */
	public int activeDownloadCount() {
		return downloadManagers.size();
	}


	/**
	 * Removes the reference of a download manager if it completed download.
	 */
	@Override
	public void update(Observable o, Object arg) {
		if(o instanceof DownloadManager) {
			DownloadManager manager = (DownloadManager) o;

			DownloadStatus status = manager.getStatus();
			if(status == DownloadStatus.PAUSED || status == DownloadStatus.ERROR) {
				// If current download is paused or some error occurred
				// Then remove it from download pool

				remove(manager, false);
			}
		}
	}

	@Override
	protected void finalize() throws Throwable {
		removeAll();
		super.finalize();
	}
}
