/**
 *
 */
package com.codeforwin.jdm;

import java.awt.EventQueue;

import javax.swing.UIManager;

/**
 * @author Pankaj Prakash
 *
 */
public class Main {

	/**
	 * Main driver class. It invokes the primary UI.
	 * @param args
	 */
	public static void main(String[] args) {
		// Set look and feel of the application
		try {
			for(UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
				if(info.getClassName().contains("Nimbus"))
					UIManager.setLookAndFeel(info.getClassName());
			}
		} catch (Exception e) { }


		// Run the GUI
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					UIHome frame = new UIHome();
					frame.setVisible(true);
				} catch (Exception e) {
					System.err.print("[ERROR] Unable to start application. " + e.getMessage());
					System.exit(-1);
				}
			}
		});
	}

}