package com.codeforwin.jdm;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

public class UIHome extends JFrame implements Observer {
	private static final long serialVersionUID = 1L;

	public final static SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("dd MMM yyyy hh:mm:ss aa");

	private UINewDownload uiNewDownload = null;
	private DownloadPool pool = DownloadPool.getDownloadPool();

	private JPanel contentPane;
	private JTable table;

	/**
	 * Create the frame.
	 */
	@SuppressWarnings({ "unused", "serial" })
	public UIHome() {
		final int COLUMN_DOWNLOADID = 0;
		final int COLUMN_FILENAME 	= 1;
		final int COLUMN_FILEPATH 	= 2;
		final int COLUMN_URL		= 3;
		final int COLUMN_SIZE		= 4;
		final int COLUMN_STATUS		= 5;
		final int COLUMN_DOWNLOADED	= 6;
		final int COLUMN_SPEED		= 7;
		final int COLUMN_TIMEREM	= 8;
		final int COLUMN_STARTTIME	= 9;
		final int COLUMN_ENDTIME	= 10;

		final UIHome uiRef = this;

		setTitle("Internet Download Manager");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 950, 570);
		setLocationByPlatform(true);
		addWindowListener(new WindowListener() {
			@Override
			public void windowOpened(WindowEvent e) {  }

			@Override
			public void windowIconified(WindowEvent e) { }

			@Override
			public void windowDeiconified(WindowEvent e) { }

			@Override
			public void windowDeactivated(WindowEvent e) { }

			@Override
			public void windowClosing(WindowEvent e) {
				pool.removeAll();

				setVisible(false);
				try { Thread.sleep(5000); }
				catch (InterruptedException ex) { }
			}

			@Override
			public void windowClosed(WindowEvent e) { }

			@Override
			public void windowActivated(WindowEvent arg0) { }
		});

		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);

		JMenu menuDownload = new JMenu("Download");
		menuDownload.setMnemonic('d');
		menuBar.add(menuDownload);

		JMenuItem menuNewFile = new JMenuItem("New File");
		menuNewFile.setMnemonic('n');
		menuNewFile.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				menuNewFileClick(arg0);
			}
		});
		menuDownload.add(menuNewFile);

		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		GridBagLayout gbl_contentPane = new GridBagLayout();
		gbl_contentPane.columnWidths = new int[] {200};
		gbl_contentPane.rowHeights = new int[]{363, 0, 0};
		gbl_contentPane.columnWeights = new double[]{1.0};
		gbl_contentPane.rowWeights = new double[]{1.0, 0.0, Double.MIN_VALUE};
		contentPane.setLayout(gbl_contentPane);

		JScrollPane scrollPane = new JScrollPane();

		JPopupMenu popupMenu = new JPopupMenu();

		JMenuItem popupPause = new JMenuItem("Pause");
		popupPause.addActionListener((e)->{
			int selectedRow = table.getSelectedRow();

			if(selectedRow >= 0) {
				Long downloadId = Long.parseLong((String)table.getValueAt(selectedRow, 0));
				pool.remove(downloadId);

				// Set the current status as downloading
				table.setValueAt(DownloadStatus.DOWNLOADING, selectedRow, COLUMN_STATUS);
			}
		});
		popupMenu.add(popupPause);

		JMenuItem popupResume = new JMenuItem("Resume");
		popupResume.addActionListener((e)->{
			int selectedRow = table.getSelectedRow();

			if(selectedRow >= 0) {
				Long downloadId = Long.parseLong((String)table.getValueAt(selectedRow, COLUMN_DOWNLOADID));

				addNewDownload(downloadId);
			}
		});
		popupMenu.add(popupResume);

		JMenuItem popupCancel = new JMenuItem("Cancel");
		popupCancel.addActionListener((e)->{
			int selectedRow = table.getSelectedRow();

			if(selectedRow >= 0) {
				Long downloadId = Long.parseLong((String)table.getValueAt(selectedRow, COLUMN_DOWNLOADID));

				pool.remove(downloadId);
			}
		});
		popupMenu.add(popupCancel);

		popupMenu.addSeparator();

		JMenuItem popupOpenFile = new JMenuItem("Open file");
		popupOpenFile.addActionListener((e)->{
			int selectedRow = table.getSelectedRow();

			if(selectedRow != -1) {
				String path = (String) table.getValueAt(selectedRow, COLUMN_FILEPATH);

				try {
					if(Desktop.isDesktopSupported())
						Desktop.getDesktop().open(new File(path));
				} catch (IOException ex) {
					JOptionPane.showMessageDialog(uiRef, "Unable to open the selected file.\n" + ex.getMessage(),
							"Error", JOptionPane.ERROR_MESSAGE);
				}
			}
		});
		popupMenu.add(popupOpenFile);

		JMenuItem popupOpenDir = new JMenuItem("Open directory");
		popupOpenDir.addActionListener((e)->{
			int selectedRow = table.getSelectedRow();

			if(selectedRow != -1) {
				String fname = (String) table.getValueAt(selectedRow, COLUMN_FILENAME);
				String fpath = (String) table.getValueAt(selectedRow, COLUMN_FILEPATH);

				String path  = fpath.substring(0, fpath.lastIndexOf(fname));

				try {
					if(Desktop.isDesktopSupported())
						Desktop.getDesktop().open(new File(path));
				} catch (IOException ex) {
					JOptionPane.showMessageDialog(uiRef, "Unable to open the selected file.\n" + ex.getMessage(),
							"Error", JOptionPane.ERROR_MESSAGE);
				}

			}
		});
		popupMenu.add(popupOpenDir);

		popupMenu.addSeparator();

		JMenuItem popupClear = new JMenuItem("Clear download list");
		popupClear.addActionListener((e)->{
			XMLFactory factory = XMLFactory.newXMLFactory();
			try {
				int activeDownloads = pool.activeDownloadCount();

				if(activeDownloads >= 1) {
					int choice = JOptionPane.showConfirmDialog(uiRef,
							"This will stop all active downloads. Sure to clear list?", "Confirm",
							JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);

					if (choice != JOptionPane.YES_OPTION)
						return;
				}

				// Clear all download list from XML
				factory.clearDownloadList();

				// Clear all temporary files
				Configuration.cleanTempFiles();

				// Remove the list from table
				DefaultTableModel model = (DefaultTableModel) table.getModel();
				model.getDataVector().clear();
				table.setModel(model);
				model.fireTableDataChanged();

			} catch (IOException ex) {
				JOptionPane.showMessageDialog(uiRef, "Unable to clear download list. \n" + ex.getMessage(), "Error",
						JOptionPane.ERROR_MESSAGE);
			}
		});
		popupMenu.add(popupClear);

		JMenuItem popupRemove = new JMenuItem("Remove from download list");
		popupRemove.addActionListener((e)->{
			// Get the download Id to remove
			int selectedRow = table.getSelectedRow();

			if (selectedRow != -1) {
				long downloadId = Long.parseLong((String)table.getValueAt(selectedRow, COLUMN_DOWNLOADID));

				XMLFactory factory = XMLFactory.newXMLFactory();

				try {
					factory.removeDownloadMetadata(downloadId);

					/*
					 * Remove the list from table.
					 */
					DefaultTableModel model = (DefaultTableModel) table.getModel();
					model.removeRow(selectedRow);
					table.setModel(model);
				} catch (IOException ex) {
					JOptionPane.showMessageDialog(uiRef,
							"Unable to remove selected from download list.\n" + ex.getMessage(), "Error",
							JOptionPane.ERROR_MESSAGE);
				}
			}
		});
		popupMenu.add(popupRemove);

		popupMenu.addPopupMenuListener(new PopupMenuListener() {

			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				int selectedRowCount = table.getSelectedRowCount();

				if(selectedRowCount == 0) {
					popupPause.setEnabled(false);
					popupResume.setEnabled(false);
					popupCancel.setEnabled(false);
					popupRemove.setEnabled(false);
					popupOpenDir.setEnabled(false);
					popupOpenFile.setEnabled(false);
				} else {
					int selectedRow = table.getSelectedRow();

					popupOpenFile.setEnabled(false);

					/*
					 * Disable pause, resume and cancel button based on
					 * downloading status.
					 */
					String value = table.getValueAt(selectedRow, 5) + "";
					DownloadStatus downStatus = DownloadStatus.valueOf(value);

					switch (downStatus) {
					case DOWNLOADING:
						popupResume.setEnabled(false);
						break;
					case ERROR:
						popupCancel.setEnabled(false);
						popupPause.setEnabled(false);
						break;
					case COMPLETED:
						popupPause.setEnabled(false);
						popupResume.setEnabled(false);
						popupCancel.setEnabled(false);
						popupOpenFile.setEnabled(true);
						break;
					case NEW:
						popupPause.setEnabled(false);
						popupResume.setEnabled(false);
						popupCancel.setEnabled(false);
						break;
					case READY:
						popupPause.setEnabled(false);
						popupResume.setEnabled(false);
						break;
					case PAUSED:
						popupPause.setEnabled(false);
						popupCancel.setEnabled(false);
						break;
					}
				}

			}

			@Override
			public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
				popupPause.setEnabled(true);
				popupResume.setEnabled(true);
				popupCancel.setEnabled(true);
				popupRemove.setEnabled(true);
				popupOpenDir.setEnabled(true);
				popupOpenFile.setEnabled(true);
			}

			@Override
			public void popupMenuCanceled(PopupMenuEvent e) {
				popupMenuWillBecomeInvisible(e);
			}
		});

		GridBagConstraints gbc_scrollPane = new GridBagConstraints();
		gbc_scrollPane.insets = new Insets(0, 0, 5, 0);
		gbc_scrollPane.fill = GridBagConstraints.BOTH;
		gbc_scrollPane.gridx = 0;
		gbc_scrollPane.gridy = 0;
		contentPane.add(scrollPane, gbc_scrollPane);

		table = new JTable();
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		scrollPane.setViewportView(table);
		table.setSurrendersFocusOnKeystroke(true);
		table.setModel(new DefaultTableModel(
			new Object[][] {
			},
			new String[] { "", "File name", "Path", "URL", "Size", "Status", "Downloaded", "Transfer rate",
						"Time Remaining", "Started on", "Completed on" }
			) {
			boolean[] columnEditables = new boolean[] {
				false, false, false, false, false, false, false, false, false, false, false
			};
			@Override
			public boolean isCellEditable(int row, int column) {
				return columnEditables[column];
			}
		});
		table.getColumnModel().getColumn(0).setResizable(false);
		table.getColumnModel().getColumn(0).setPreferredWidth(0);
		table.getColumnModel().getColumn(0).setMinWidth(0);
		table.getColumnModel().getColumn(0).setMaxWidth(0);
		table.getColumnModel().getColumn(1).setPreferredWidth(167);
		table.getColumnModel().getColumn(2).setPreferredWidth(265);
		table.getColumnModel().getColumn(5).setPreferredWidth(100);
		table.getColumnModel().getColumn(7).setPreferredWidth(90);
		table.getColumnModel().getColumn(8).setPreferredWidth(84);
		table.getColumnModel().getColumn(9).setPreferredWidth(150);
		table.getColumnModel().getColumn(10).setPreferredWidth(150);
		table.getTableHeader().setReorderingAllowed(false);
		table.setBackground(UIManager.getColor("inactiveCaptionBorder"));
		table.setComponentPopupMenu(popupMenu);
		table.setDefaultRenderer(Object.class, new CellRendered());
		table.setRowHeight(20);
		table.setAutoCreateRowSorter(true);

		JPanel panel = new JPanel();
		panel.setSize(new Dimension(0, 30));
		GridBagConstraints gbc_panel = new GridBagConstraints();
		gbc_panel.fill = GridBagConstraints.HORIZONTAL;
		gbc_panel.gridx = 0;
		gbc_panel.gridy = 1;
		contentPane.add(panel, gbc_panel);
		panel.setLayout(new BorderLayout(0, 0));

		JLabel lblcodeforwin = new JLabel("\u00A9 Codeforwin");
		lblcodeforwin.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseEntered(MouseEvent arg0) {
				lblcodeforwin.setForeground(new Color(-14848876));
			}

			@Override
			public void mouseExited(MouseEvent e) {
				lblcodeforwin.setForeground(Color.GRAY);
			}

			@Override
			public void mouseClicked(MouseEvent e) {
				if(Desktop.isDesktopSupported()) {
					try {
						Desktop.getDesktop().browse(new URI("http://www.codeforwin.in"));
					}catch (Exception ex) { }
				}
			}
		});
		lblcodeforwin.setForeground(Color.GRAY);
		lblcodeforwin.setCursor(new Cursor(Cursor.HAND_CURSOR));
		lblcodeforwin.setFont(new Font("Tahoma", Font.PLAIN, 9));
		panel.add(lblcodeforwin, BorderLayout.EAST);

		populateDownloadList();
	}


	/**
	 * On each download activity update the GUI accordingly
	 */
	@Override
	public void update(Observable o, Object arg) {
		final int COLUMN_STATUS			= 5;
		final int COLUMN_DOWNLOADED		= 6;
		final int COLUMN_TRANSFER_RATE	= 7;

		if(o instanceof DownloadManager) {
			DownloadManager manager = (DownloadManager) o;

			int rowToUpdate = getDownloadRow(manager.getDownloadId());

			String status	= manager.getStatus().name();
			String completed = toMB(manager.getDownloadCompleted());
			String transferRate	= toSpeed(manager.getDownloadSpeed());

			DefaultTableModel model = (DefaultTableModel) table.getModel();
			int totalRows = model.getRowCount();

			// If this is a new download
			if(arg != null) {
				// Add a new row
				DownloadMetadata meta = manager.getMetadata();

				String downloadId 	= meta.getId() + "";
				String fileName		= meta.getFileName() + "." + meta.getFileType();
				String filePath		= meta.getFilePath();
				String url			= meta.getUrl();
				String fileSize = toMB(meta.getFileSize());
				String timeRemaining= "";

				Date startTime 		= meta.getStartTime();
				String startedOn	= (startTime == null) ? "" : DATE_FORMATTER.format(startTime);

				Date endTime = meta.getEndTime();
				String completedOn 	= (endTime == null) ? "" : (meta.getStatus() == DownloadStatus.COMPLETED)
						? DATE_FORMATTER.format(endTime) : "-";

				Object[] rowData = new Object[] { downloadId, fileName, filePath, url, fileSize, status, completed,
						transferRate, timeRemaining, startedOn, completedOn };


				if(rowToUpdate == -1) {
					model.addRow(rowData);
					model.fireTableRowsInserted(totalRows, totalRows);
				} else {
					model.setValueAt(fileName, rowToUpdate, 1);
					model.setValueAt(filePath, rowToUpdate, 2);
					model.setValueAt(fileSize, rowToUpdate, 4);
					model.setValueAt(status, rowToUpdate, 5);
					model.setValueAt(completed, rowToUpdate, 6);
					model.setValueAt(transferRate, rowToUpdate, 7);
					model.setValueAt(timeRemaining, rowToUpdate, 8);
					model.setValueAt(startedOn, rowToUpdate, 9);
					model.setValueAt(completedOn, rowToUpdate, 10);

					model.fireTableRowsUpdated(rowToUpdate, rowToUpdate);
				}
			} else {
				// Update the column cells
				model.setValueAt(status, rowToUpdate, COLUMN_STATUS);
				model.setValueAt(completed, rowToUpdate, COLUMN_DOWNLOADED);
				model.setValueAt(transferRate, rowToUpdate, COLUMN_TRANSFER_RATE);

				model.fireTableCellUpdated(rowToUpdate, COLUMN_STATUS);
				model.fireTableCellUpdated(rowToUpdate, COLUMN_DOWNLOADED);
			}
		}
	}


	/**
	 * Populate the download list with existing downloads
	 */
	private void populateDownloadList() {
		XMLFactory factory = XMLFactory.newXMLFactory();

		List<DownloadMetadata> metadata = null;

		try {
			metadata = factory.getDownloadList();
		} catch (IOException e) {
			JOptionPane.showMessageDialog(this,
					"Oops! something went wrong while gathering download list.\n" + e.getMessage(), "Error loading",
					JOptionPane.ERROR_MESSAGE);
		}

		// If metadata is fetched successfully
		if(metadata != null) {
			DefaultTableModel model = (DefaultTableModel) table.getModel();

			metadata.forEach((meta)->{
				//"", "File name", "Path", "URL", "Size", "Status", "Downloaded", "Transfer rate", "Time Remaining", "Started on", "Completed on"
				String id		= meta.getId() + "";
				String fname	= meta.getFileName() + "." + meta.getFileType();
				String fpath	= meta.getFilePath();
				String url		= meta.getUrl();
				String size		= toMB(meta.getFileSize());
				String status	= meta.getStatus().name();
				String completed= toMB(meta.getCompleted());
				String transfer	= "";
				String timeRem	= "";

				Date startTime	= meta.getStartTime();
				String startedOn= (startTime == null) ? "" : DATE_FORMATTER.format(startTime);

				Date endTime 	= meta.getEndTime();
				String complOn	= (endTime == null) ? "" : DATE_FORMATTER.format(endTime);

				Object[] rowData = new Object[] { id, fname, fpath, url, size, status, completed, transfer, timeRem,
						startedOn, complOn };

				model.addRow(rowData);
			});

			table.setModel(model);
		}

	}


	/**
	 * Gets the row index of with particular download id.
	 * @param downloadId
	 * @return
	 */
	private int getDownloadRow(long downloadId) {
		DefaultTableModel model = (DefaultTableModel) table.getModel();

		for(int i=0; i<model.getRowCount(); i++) {
			String curId = (String) model.getValueAt(i, 0);

			if(curId.equals(downloadId + ""))
				return i;
		}

		return -1;
	}


	/**
	 * Converts bytes to megabytes MB.
	 * @param bytes
	 * @return
	 */
	private String toMB(long bytes) {
		if(bytes == 0) return "";

		float mb = bytes / (1024.0f * 1024.0f);

		return String.format("%.2f MB", mb);
	}

	/**
	 * Converts bytes per second to KBps or MBps
	 * @param rate
	 * @return
	 */
	private String toSpeed(float rate) {
		String[] SPEED = new String[] {"Bps", "KBps", "MBps", "GBps", "TBps" };

		if(rate == Float.POSITIVE_INFINITY || rate == Float.NEGATIVE_INFINITY)
			return String.format("0 %s", SPEED[0]);

		int i=0;

		while(i < SPEED.length && rate > 1024) {
			if(rate != 0)
				rate = rate / 1024.0f;
			i++;
		}

		return String.format("%.2f %s", rate, SPEED[i]);
	}


	/**
	 * Shows the new download UI.
	 * @param evt
	 */
	private void menuNewFileClick(ActionEvent evt) {
		if(uiNewDownload == null)
			uiNewDownload = new UINewDownload(this);

		if(uiNewDownload.isVisible())
			uiNewDownload.requestFocus();
		else
			uiNewDownload.setVisible(true);
	}


	/**
	 * Creates a new download and add it to download pool.
	 * @param url URL from where to download.
	 * @param filePath Path where to save the downloaded resource.
	 */
	public void addNewDownload(String url, String filePath) {
		DownloadManager downManager = new DownloadManager(url, filePath);
		downManager.addObserver(this);

		pool.add(downManager);

		update(downManager, true);
	}


	/**
	 * Resumes an existing download and adds it to the download pool.
	 * @param downloadId Unique download id to resume.
	 */
	public void addNewDownload(long downloadId) {
		XMLFactory factory = XMLFactory.newXMLFactory();

		try {
			DownloadMetadata meta = factory.getDownloadMetadata(downloadId);
			List<DownloadPartsMetadata> parts = factory.getDownloadPartsList(downloadId);

			DownloadManager manager = new DownloadManager(meta, parts);
			manager.addObserver(this);

			pool.add(manager);
		} catch (IOException ex) {
			JOptionPane.showMessageDialog(this, "Unable to resume download. \n" + ex.getMessage(),
					"Error resuming", JOptionPane.ERROR_MESSAGE);
		}
	}


	/**
	 * Custom cell for download table
	 * @author Pankaj Prakash
	 *
	 */
	private class CellRendered extends DefaultTableCellRenderer {
		private static final long serialVersionUID = 1L;

		private final Color COLOR_BACKGROUND	= new Color(-2691076); 	// Light blue
		private final Color COLOR_BACKGROUND_ALT= new Color(-331279); 	// Light red
		private final Color COLOR_SELECTED		= new Color(-13335895); // Blue

		private final int PADDING_HORIZONTAL = 5;
		private final int PADDING_VERTICAL	 = 10;

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) {
			Component cellComponent = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

			if(row % 2 == 0)
				cellComponent.setBackground(COLOR_BACKGROUND);
			else
				cellComponent.setBackground(COLOR_BACKGROUND_ALT);

			if(isSelected)
				cellComponent.setBackground(COLOR_SELECTED);

			setBorder(BorderFactory.createEmptyBorder(PADDING_VERTICAL, PADDING_HORIZONTAL, PADDING_VERTICAL, PADDING_HORIZONTAL));

			return cellComponent;
		}
	}
}
