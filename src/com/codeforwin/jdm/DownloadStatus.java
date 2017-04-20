/**
 * 
 */
package com.codeforwin.jdm;

/**
 * @author Pankaj Prakash
 *
 */
public enum DownloadStatus {
	/**
	 * The file download process has not initialized yet.
	 */
	NEW,
	
	/**
	 * The download is ready to begin. Can be set once.
	 */
	READY,
	
	/**
	 * The file download is in progress.
	 */
	DOWNLOADING,
	
	/**
	 * All block of the file has been downloaded and merged successfully.
	 */
	COMPLETED,
	
	/**
	 * Download is paused explicitly.
	 */
	PAUSED,
	
	/**
	 * Some error occurred while attempting to download file.
	 */
	ERROR
}
