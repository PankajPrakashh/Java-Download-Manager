package com.codeforwin.jdm;

/**
 * Describes metadata of each part of the download.
 * @author Pankaj Prakash
 *
 */
public class DownloadPartsMetadata  {
	private long  start;

	private final long 		downloadId;
	private final int 		id;
	private final long 		end;
	private final String	path;

	public DownloadPartsMetadata(long downloadId, int id, long start, long end, String path) {
		this.downloadId	= downloadId;
		this.id			= id;
		this.start		= start;
		this.end		= end;
		this.path		= path;
	}


	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}

	/**
	 * @return the start
	 */
	public long getStart() {
		return start;
	}

	/**
	 * @return the end
	 */
	public long getEnd() {
		return end;
	}

	/**
	 * @return the path
	 */
	public String getPath() {
		return path;
	}


	/**
	 * @return the downloadId
	 */
	public long getDownloadId() {
		return downloadId;
	}


	/**
	 * @param start the start to set
	 */
	public void setStart(long start) {
		this.start = start;
	}
}