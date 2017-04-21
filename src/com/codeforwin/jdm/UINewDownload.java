package com.codeforwin.jdm;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

@SuppressWarnings("serial")
public class UINewDownload extends JFrame{

	private UIHome uiHome = null;


	private JPanel contentPane;
	private JTextField textUrl;
	private JTextField textSaveLocation;
	private JButton btnBrowse;
	private JButton btnDownload;
	private JButton btnCancel;

	/**
	 * Create the frame.
	 */
	public UINewDownload(UIHome uiHome) {
		this.uiHome = uiHome;

		setType(Type.POPUP);
		setTitle("New Download");
		setResizable(false);
		setAlwaysOnTop(true);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setBounds(100, 100, 511, 155);
		setLocationRelativeTo(uiHome);
		contentPane = new JPanel();
		contentPane.setBorder(new TitledBorder(null, "Download information", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		setContentPane(contentPane);
		GridBagLayout gbl_contentPane = new GridBagLayout();
		gbl_contentPane.columnWidths = new int[]{0, 0, 0, 0};
		gbl_contentPane.rowHeights = new int[]{0, 0, 0, 0};
		gbl_contentPane.columnWeights = new double[]{0.0, 1.0, 1.0, Double.MIN_VALUE};
		gbl_contentPane.rowWeights = new double[]{0.0, 0.0, 0.0, Double.MIN_VALUE};
		contentPane.setLayout(gbl_contentPane);

		JLabel lblUrlToDownload = new JLabel("URL to download");
		lblUrlToDownload.setFont(new Font("Tahoma", Font.PLAIN, 11));
		GridBagConstraints gbc_lblUrlToDownload = new GridBagConstraints();
		gbc_lblUrlToDownload.fill = GridBagConstraints.HORIZONTAL;
		gbc_lblUrlToDownload.insets = new Insets(5, 10, 5, 10);
		gbc_lblUrlToDownload.gridx = 0;
		gbc_lblUrlToDownload.gridy = 0;
		contentPane.add(lblUrlToDownload, gbc_lblUrlToDownload);

		textUrl = new JTextField();
		textUrl.addKeyListener(new KeyAdapter() {
			@Override
			public void keyTyped(KeyEvent arg0) {
				textUrlKeyTyped(arg0);
			}
		});
		textUrl.setFont(new Font("Tahoma", Font.PLAIN, 11));
		GridBagConstraints gbc_textUrl = new GridBagConstraints();
		gbc_textUrl.gridwidth = 2;
		gbc_textUrl.fill = GridBagConstraints.HORIZONTAL;
		gbc_textUrl.insets = new Insets(5, 0, 5, 5);
		gbc_textUrl.gridx = 1;
		gbc_textUrl.gridy = 0;
		contentPane.add(textUrl, gbc_textUrl);
		textUrl.setColumns(10);

		JLabel lblSaveLocation = new JLabel("Save location");
		lblSaveLocation.setFont(new Font("Tahoma", Font.PLAIN, 11));
		GridBagConstraints gbc_lblSaveLocation = new GridBagConstraints();
		gbc_lblSaveLocation.fill = GridBagConstraints.HORIZONTAL;
		gbc_lblSaveLocation.insets = new Insets(5, 10, 5, 10);
		gbc_lblSaveLocation.gridx = 0;
		gbc_lblSaveLocation.gridy = 1;
		contentPane.add(lblSaveLocation, gbc_lblSaveLocation);

		textSaveLocation = new JTextField();
		textSaveLocation.setFont(new Font("Tahoma", Font.PLAIN, 11));
		textSaveLocation.setColumns(10);
		textSaveLocation.setText(Configuration.DEFAULT_DOWNLOAD_PATH);
		textSaveLocation.setEditable(false);
		textSaveLocation.setFocusable(false);
		GridBagConstraints gbc_textSaveLocation = new GridBagConstraints();
		gbc_textSaveLocation.weightx = 10.0;
		gbc_textSaveLocation.insets = new Insets(5, 0, 5, 5);
		gbc_textSaveLocation.fill = GridBagConstraints.HORIZONTAL;
		gbc_textSaveLocation.gridx = 1;
		gbc_textSaveLocation.gridy = 1;
		contentPane.add(textSaveLocation, gbc_textSaveLocation);

		btnBrowse = new JButton("Browse");
		btnBrowse.setMnemonic('b');
		btnBrowse.addActionListener((e)-> { btnBrowseClick(e); });
		GridBagConstraints gbc_btnBrowse = new GridBagConstraints();
		gbc_btnBrowse.fill = GridBagConstraints.HORIZONTAL;
		gbc_btnBrowse.insets = new Insets(5, 0, 5, 5);
		gbc_btnBrowse.gridx = 2;
		gbc_btnBrowse.gridy = 1;
		contentPane.add(btnBrowse, gbc_btnBrowse);

		btnDownload = new JButton("Download");
		btnDownload.setEnabled(false);
		btnDownload.setMnemonic('d');
		btnDownload.addActionListener((e)-> { btnDownloadClick(e); });
		GridBagConstraints gbc_btnDownload = new GridBagConstraints();
		gbc_btnDownload.anchor = GridBagConstraints.EAST;
		gbc_btnDownload.insets = new Insets(5, 5, 5, 5);
		gbc_btnDownload.gridx = 1;
		gbc_btnDownload.gridy = 2;
		contentPane.add(btnDownload, gbc_btnDownload);

		btnCancel = new JButton("Cancel");
		btnCancel.setMnemonic('c');
		btnCancel.addActionListener((e)-> { btnCancelClick(e); });
		GridBagConstraints gbc_btnCancel = new GridBagConstraints();
		gbc_btnCancel.fill = GridBagConstraints.HORIZONTAL;
		gbc_btnCancel.insets = new Insets(5, 0, 5, 5);
		gbc_btnCancel.gridx = 2;
		gbc_btnCancel.gridy = 2;
		contentPane.add(btnCancel, gbc_btnCancel);
	}


	/**
	 * Displays the file browser dialog to choose file save location.
	 * @param evt
	 */
	private void btnBrowseClick(ActionEvent evt) {
		JFileChooser fileChooser = new JFileChooser(Configuration.DEFAULT_DOWNLOAD_PATH);
		fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY | JFileChooser.CANCEL_OPTION);
		fileChooser.setDialogTitle("Select download directory");

		int option = fileChooser.showDialog(this, "Choose Directory");

		if(option == JFileChooser.APPROVE_OPTION) {
			String filePath = fileChooser.getSelectedFile().getPath() + "\\";

			textSaveLocation.setText(filePath);

			/*
			 * Ask to set it as default directory
			 */
			option = JOptionPane.showConfirmDialog(this, "Do you want to set it as your default directory?", "Set default directory", JOptionPane.YES_NO_OPTION);

			// Set it as default directory
			if(option == JOptionPane.YES_OPTION) {
				Configuration.setProperty("DEFAULT_DOWNLOAD_PATH", filePath);
			}
		}
	}

	/**
	 * Closes the new download window
	 * @param evt
	 */
	private void btnCancelClick(ActionEvent evt) {
		setVisible(false);
		uiHome.requestFocus();
	}

	/**
	 * On every key typed on URL text box. If URL is not empty then enable download button.
	 * @param evt
	 */
	private void textUrlKeyTyped(KeyEvent evt) {
		String url = textUrl.getText().trim();

		if(url.length() > 0) {
			btnDownload.setEnabled(true);

			if(evt.getKeyChar() == KeyEvent.VK_ENTER)
				btnDownloadClick(null);
		}
		else
			btnDownload.setEnabled(false);
	}

	/**
	 * Validates all information and sends the downloads request to UIHome object.
	 * @param evt
	 */
	private void btnDownloadClick(ActionEvent evt) {
		String url 		= textUrl.getText();
		String filePath = textSaveLocation.getText();

		if (filePath == null || filePath.isEmpty()) {
			// If file path is empty ask user to select a file path
			JOptionPane.showMessageDialog(this, "Please select a download location.", "Select download location",
					JOptionPane.WARNING_MESSAGE);

			btnBrowseClick(evt);
			return;
		} else {
			// Check if the location exists
			File file = new File(filePath);

			// If file does not exists prompt user with a message to choose another file path
			if (!file.exists()) {
				JOptionPane.showMessageDialog(this,
						"Invalid download location. Please choose another download location.", "Invalid save location",
						JOptionPane.WARNING_MESSAGE);

				btnBrowseClick(evt);
				return;
			}
		}

		uiHome.addNewDownload(url, filePath);
		setVisible(false);
	}
}
