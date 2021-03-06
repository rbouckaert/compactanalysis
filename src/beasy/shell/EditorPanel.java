package beasy.shell;



import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javafx.application.Platform;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.JButton;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.fife.ui.autocomplete.AutoCompletion;
import org.fife.ui.autocomplete.BasicCompletion;
import org.fife.ui.autocomplete.CompletionProvider;
import org.fife.ui.autocomplete.DefaultCompletionProvider;
import org.fife.ui.autocomplete.ShorthandCompletion;
import org.fife.ui.rtextarea.*;
import org.fife.ui.rsyntaxtextarea.*;

import beast.app.beauti.BeautiDoc;
import beast.app.util.Utils;
import beast.core.util.Log;
import beast.util.BEASTClassLoader;
import beast.util.PackageManager;

public class EditorPanel extends JPanel implements ActionListener, KeyListener, DocumentListener {
	private static final long serialVersionUID = 1L;
	JTabbedPane tabbedPane;
	List<String> fileNames;
	JTextField searchField;
	Image image;
	private JCheckBox regexCB;
	private JCheckBox matchCaseCB;
	BeasyStudio studio;
	
	Set<RSyntaxTextArea> editorUnChanged = new  HashSet<>();
	
	String cwd = System.getProperty("user.dir");

	public EditorPanel(BeasyStudio studio) {
		this.studio = studio;
		fileNames = new ArrayList<String>();
		setLayout(new BorderLayout());
		JToolBar toolBar = new JToolBar();
		add(toolBar, BorderLayout.NORTH);

		JButton btnNewButton = new JButton("");
		btnNewButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				doNew();
			}

		});
		btnNewButton.setIcon(getIcon("/beasy/shell/icons/new.png", this));
		btnNewButton.setToolTipText("Start new editor");
		toolBar.add(btnNewButton);

		JButton btnNewButton_3 = new JButton("");
		btnNewButton_3.setIcon(getIcon("/beasy/shell/icons/open.png", this));
		btnNewButton_3.setToolTipText("Open file");
		btnNewButton_3.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				doOpen();
			}
		});
		toolBar.add(btnNewButton_3);

		JButton btnNewButton_1 = new JButton("");
		btnNewButton_1.setSelectedIcon(getIcon("/beasy/shell/icons/recent.png", this));
		btnNewButton_1.setToolTipText("Recently opened files");
		toolBar.add(btnNewButton_1);

		JButton btnNewButton_2 = new JButton("");
		btnNewButton_2.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				doSave();
			}
		});
		btnNewButton_2.setIcon(getIcon("/beasy/shell/icons/save.png", this));
		btnNewButton_2.setToolTipText("Save current editor");
		toolBar.add(btnNewButton_2);

		JButton btnNewButton_4 = new JButton("");
		btnNewButton_4.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				doSaveAll();
			}
		});
		btnNewButton_4.setIcon(getIcon("/beasy/shell/icons/saveall.png", this));
		btnNewButton_4.setToolTipText("Save all files");
		toolBar.add(btnNewButton_4);

		toolBar.addSeparator();

		// Create a toolbar with searching options.
		//image = Utils.getIcon("/beasy/shell/icons/search.png").getImage();
		ImageIcon icon = getIcon("/beasy/shell/icons/search.png", this);
		if (icon != null) {
			image = icon.getImage();
		}
		searchField = new JTextField(30) {
            @Override
			protected void paintComponent(Graphics g) {  
                super.paintComponent(g); 
                if (image != null) {
                	int y = (getHeight() - image.getHeight(null))/2;
                	int x = getWidth() - 17;
                	g.drawImage(image, x, y, this);
                } else {
                	int y = getHeight()/2;
                	int x = getWidth() - 17;
                	g.drawString("F", x, y);
                }
            }  
        };  
		toolBar.add(searchField);

		JButton prevButton = new JButton("");
		prevButton.setToolTipText("Find Previous");
		prevButton.setIcon(getIcon("/beasy/shell/icons/findup.png", this));
		prevButton.setActionCommand("FindPrev");
		prevButton.addActionListener(this);
		toolBar.add(prevButton);

		final JButton nextButton = new JButton("");
		nextButton.setIcon(getIcon("/beasy/shell/icons/finddown.png", this));
		nextButton.setToolTipText("Find Next");
		nextButton.setActionCommand("FindNext");
		nextButton.addActionListener(this);
		toolBar.add(nextButton);
		searchField.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				nextButton.doClick(0);
			}
		});

		regexCB = new JCheckBox("Regex");
		toolBar.add(regexCB);
		matchCaseCB = new JCheckBox("Match Case");
		toolBar.add(matchCaseCB);

		tabbedPane = new JTabbedPane(SwingConstants.TOP);
		tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		add(tabbedPane, BorderLayout.CENTER);
		
		restoreEditors();
	}

	
	static ImageIcon getIcon(String string, Object class_) {
		ImageIcon icon = Utils.getIcon(string);
		if (icon == null) {
			ClassLoader classLoader = class_.getClass().getClassLoader();
			URL url = classLoader.getResource(string);
			if (url != null) {
				icon = new ImageIcon(url);
				return icon;
			}
			BufferedImage bimage = null;
			InputStream is = class_.getClass().getResourceAsStream(string);
			if (is != null) {
				try {
					bimage = ImageIO.read(is);
				} catch (IOException e) {
					// ignore
				}
	        	if (bimage != null) {
					icon = new ImageIcon(bimage);
					return icon;
				}
			}
		}
		return null;
	}


	@Override
	public void actionPerformed(ActionEvent e) {

		// "FindNext" => search forward, "FindPrev" => search backward
		String command = e.getActionCommand();
		boolean forward = "FindNext".equals(command);

		// Create an object defining our search parameters.
		SearchContext context = new SearchContext();
		String text = searchField.getText();
		if (text.length() == 0) {
			return;
		}
		context.setSearchFor(text);
		context.setMatchCase(matchCaseCB.isSelected());
		context.setRegularExpression(regexCB.isSelected());
		context.setSearchForward(forward);
		context.setWholeWord(false);

		RSyntaxTextArea textPane = getCurrentTextPane();
		if (textPane == null) {
			JOptionPane.showMessageDialog(this, "No text to search in...");
			return;
		}
		SearchResult result = SearchEngine.find(textPane, context);
		if (!result.wasFound()) {
          SwingUtilities.invokeLater(new Runnable() {
	          @Override 
	          public void run() {
				searchField.setBackground(Color.RED);
				searchField.paintImmediately(0,  0, searchField.getWidth(), searchField.getHeight());
				try {
					Thread.sleep(200);
				} catch (InterruptedException e1) {
				}
				searchField.setBackground(Color.WHITE);
				searchField.paintImmediately(0,  0, searchField.getWidth(), searchField.getHeight());
	          }
	      });
		}

	}

	private RSyntaxTextArea getCurrentTextPane() {
		JScrollPane scrollPane = (JScrollPane) tabbedPane.getSelectedComponent();
		if (scrollPane == null) {
			return null;
		}
		for (Component c : scrollPane.getViewport().getComponents()) {
			if (c instanceof RSyntaxTextArea) {
				return (RSyntaxTextArea) c;
			}
		}
		return null;
	}

	public void doNew() {
		RSyntaxTextArea /* JTextArea */textPane;
		textPane = new RSyntaxTextArea();
		textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
		textPane.setCodeFoldingEnabled(true);
		textPane.setAntiAliasingEnabled(true);
		textPane.addKeyListener(this);
	    textPane.getDocument().addDocumentListener(this);
		
	    CompletionProvider provider = createCompletionProvider();
	    AutoCompletion ac = new AutoCompletion(provider);
	    ac.install(textPane);		

	    addTab("New file", textPane);
	    editorUnChanged.add(textPane);
		fileNames.add(null);
	}

		
		@Override
		public void keyTyped(java.awt.event.KeyEvent e) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void keyReleased(java.awt.event.KeyEvent e) {
			if (e.isControlDown() && e.getKeyCode() == 70) {
				searchField.requestFocus();
			}
			if (e.isControlDown() && e.getKeyCode() == 71) {
				actionPerformed(new ActionEvent(this, 1, "FindNext"));
			}
			if (e.isControlDown() && e.getKeyCode() == 72) {
				doSearchReplace();
			}
		}

		@Override
		public void keyPressed(java.awt.event.KeyEvent e) {
			// TODO Auto-generated method stub
			
		}
	
		void doSearchReplace() {
			SearchDialog dlg = new SearchDialog(this, getCurrentTextPane());
			dlg.setVisible(true);
			//dlg.dispose();
		}

		/**
	    * Create a simple provider that adds some Java-related completions.
	    * 
	    * @return The completion provider.
	    */
	   private CompletionProvider createCompletionProvider() {

	      // A DefaultCompletionProvider is the simplest concrete implementation
	      // of CompletionProvider. This provider has no understanding of
	      // language semantics. It simply checks the text entered up to the
	      // caret position for a match against known completions. This is all
	      // that is needed in the majority of cases.
	      DefaultCompletionProvider provider = new DefaultCompletionProvider();

	      // Add completions for all Java keywords. A BasicCompletion is just
	      // a straightforward word completion.
	      provider.addCompletion(new BasicCompletion(provider, "abstract"));
	      provider.addCompletion(new BasicCompletion(provider, "assert"));
	      provider.addCompletion(new BasicCompletion(provider, "break"));
	      provider.addCompletion(new BasicCompletion(provider, "case"));
	      provider.addCompletion(new BasicCompletion(provider, "catch"));
	      provider.addCompletion(new BasicCompletion(provider, "class"));
	      provider.addCompletion(new BasicCompletion(provider, "const"));
	      provider.addCompletion(new BasicCompletion(provider, "continue"));
	      provider.addCompletion(new BasicCompletion(provider, "default"));
	      provider.addCompletion(new BasicCompletion(provider, "do"));
	      provider.addCompletion(new BasicCompletion(provider, "else"));
	      provider.addCompletion(new BasicCompletion(provider, "enum"));
	      provider.addCompletion(new BasicCompletion(provider, "extends"));
	      provider.addCompletion(new BasicCompletion(provider, "final"));
	      provider.addCompletion(new BasicCompletion(provider, "finally"));
	      provider.addCompletion(new BasicCompletion(provider, "for"));
	      provider.addCompletion(new BasicCompletion(provider, "goto"));
	      provider.addCompletion(new BasicCompletion(provider, "if"));
	      provider.addCompletion(new BasicCompletion(provider, "implements"));
	      provider.addCompletion(new BasicCompletion(provider, "import"));
	      provider.addCompletion(new BasicCompletion(provider, "instanceof"));
	      provider.addCompletion(new BasicCompletion(provider, "interface"));
	      provider.addCompletion(new BasicCompletion(provider, "native"));
	      provider.addCompletion(new BasicCompletion(provider, "new"));
	      provider.addCompletion(new BasicCompletion(provider, "package"));
	      provider.addCompletion(new BasicCompletion(provider, "private"));
	      provider.addCompletion(new BasicCompletion(provider, "protected"));
	      provider.addCompletion(new BasicCompletion(provider, "public"));
	      provider.addCompletion(new BasicCompletion(provider, "return"));
	      provider.addCompletion(new BasicCompletion(provider, "static"));
	      provider.addCompletion(new BasicCompletion(provider, "strictfp"));
	      provider.addCompletion(new BasicCompletion(provider, "super"));
	      provider.addCompletion(new BasicCompletion(provider, "switch"));
	      provider.addCompletion(new BasicCompletion(provider, "synchronized"));
	      provider.addCompletion(new BasicCompletion(provider, "this"));
	      provider.addCompletion(new BasicCompletion(provider, "throw"));
	      provider.addCompletion(new BasicCompletion(provider, "throws"));
	      provider.addCompletion(new BasicCompletion(provider, "transient"));
	      provider.addCompletion(new BasicCompletion(provider, "try"));
	      provider.addCompletion(new BasicCompletion(provider, "void"));
	      provider.addCompletion(new BasicCompletion(provider, "volatile"));
	      provider.addCompletion(new BasicCompletion(provider, "while"));

	      // Add a couple of "shorthand" completions. These completions don't
	      // require the input text to be the same thing as the replacement text.
	      provider.addCompletion(new ShorthandCompletion(provider, "sysout",
	            "System.out.println(", "System.out.println("));
	      provider.addCompletion(new ShorthandCompletion(provider, "syserr",
	            "System.err.println(", "System.err.println("));

	      return provider;

	   }	
	
	public void doOpen() {
	    Platform.runLater(new Runnable() { 
	    	@Override
			public void run() { 
	    		File file = beasy.shell.Utils.getLoadFile("Open Beasy file", new File(cwd), "Beasy script files", "*.bea");
	    		doOpen(file);
	    	} 
	    });
	}
	
	private void doOpen(File file) {
		if (file != null) {
			String text = null;
			try {
				text = BeautiDoc.load(file);
			} catch (IOException e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(this, "Could not open file: " + e.getMessage());
				return;
			}
			RSyntaxTextArea /* JTextArea */textPane;
			textPane = new RSyntaxTextArea();
			textPane.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
			textPane.setCodeFoldingEnabled(true);
			textPane.setAntiAliasingEnabled(true);
			textPane.setText(text);
			textPane.addKeyListener(this);
		    editorUnChanged.add(textPane);
		    textPane.getDocument().addDocumentListener(this);
		    
		    CompletionProvider provider = createCompletionProvider();
		    AutoCompletion ac = new AutoCompletion(provider);
		    ac.install(textPane);		

		    addTab(file.getName(), textPane);
			fileNames.add(file.getAbsolutePath());
			cwd = file.getParent();			
		}
	}

	public void doSave() {
		int i = tabbedPane.getSelectedIndex();
		doSave(i);
		
		RSyntaxTextArea current = getCurrentTextPane();
		if (current != null && !editorUnChanged.contains(current)) {
			int currentTab = tabbedPane.getSelectedIndex();
			String label = tabbedPane.getTitleAt(currentTab);
			if (label.startsWith("*")) {
				label = label.substring(1);
			}
			tabbedPane.setTitleAt(currentTab, label);
			editorUnChanged.add(current);
		}
	}

	public void doSaveAll() {
		for (int i = 0; i < tabbedPane.getTabCount(); i++) {
			doSave(i);
		}
	}

	public void saveAs() {
		int i = tabbedPane.getSelectedIndex();
		File file = Utils.getSaveFile("Save Beasy file", new File(cwd), "Beasy script files", "bea");
		if (file != null) {
			cwd = file.getParent();
			fileNames.set(i, file.getAbsolutePath());
			doSave(i);
		}
	}
	
	private void doSave(int i) {
		File file = null;
		tabbedPane.setSelectedIndex(i);
		if (fileNames.get(i) == null) {
			saveAs();
		} else {
			file = new File(fileNames.get(i));
		}
		if (file != null) {
			RSyntaxTextArea textPane = getCurrentTextPane();
			String text = textPane.getText();
			try {
				FileWriter outfile = new FileWriter(file);
				outfile.write(text);
				outfile.close();
			} catch (IOException e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(this, "Something went wrong writing the file + " + file.getName() + ": " + e.getMessage());
				return;
			}
			tabbedPane.setTitleAt(tabbedPane.getSelectedIndex(), file.getName());
			fileNames.set(i, file.getAbsolutePath());
		}
	}

	void addTab(String title, final JComponent panel) {
	    RTextScrollPane sp = new RTextScrollPane(panel);
	    sp.setFoldIndicatorEnabled(true);
		//JScrollPane scrollPane = new JScrollPane(panel);
		tabbedPane.addTab(title, sp);
		int i = tabbedPane.indexOfComponent(sp);
		tabbedPane.setTabComponentAt(i, new ButtonTabComponent(this));
		tabbedPane.setSelectedIndex(i);
		String s;
	}

	final static String STATUS_FILE = "editors.dat";
	
	private void restoreEditors() {
		File backup = new File(PackageManager.getPackageUserDir() + BeasyStudio.PACKAGENAME + STATUS_FILE);
		if (backup.exists()) {
			try {
		        BufferedReader fin = new BufferedReader(new FileReader(backup));
		        String sStr = null;
		        while (fin.ready()) {
		            sStr = fin.readLine();
		            File file = new File(sStr);
		            doOpen(file);
		        }
		        fin.close();
			} catch (IOException e) {
				e.printStackTrace();
			}			
		}
	}
	
	public void saveStatus() {
		File backupdir = new File(PackageManager.getPackageUserDir() + BeasyStudio.PACKAGENAME);
		if (!backupdir.exists()) {
			backupdir.mkdirs();
		}
		File backup = new File(PackageManager.getPackageUserDir() + BeasyStudio.PACKAGENAME + STATUS_FILE);
		try {
	        FileWriter outfile = new FileWriter(backup);
	        for (String s : fileNames) {
	        	outfile.write(s);
	        	outfile.write('\n');
	        }
	        outfile.close();				
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
	
	public boolean hasEditors() {
		return tabbedPane.getTabCount() > 0;
	}


	@Override
	public void insertUpdate(DocumentEvent e) {
	}

	@Override
	public void removeUpdate(DocumentEvent e) {
	}

	@Override
	public void changedUpdate(DocumentEvent e) {
		RSyntaxTextArea current = getCurrentTextPane();
		if (current != null && editorUnChanged.contains(current)) {
			int currentTab = tabbedPane.getSelectedIndex();
			String label = "*" + tabbedPane.getTitleAt(currentTab);
			tabbedPane.setTitleAt(currentTab, label);
			editorUnChanged.remove(current);
		}
	}

	public void runCurrent() {
		String script = getCurrentTextPane().getText();
		if (script == null || script.trim().length() == 0) {
			JOptionPane.showMessageDialog(this, "No code found in editor");
			return;
		}
		studio.interpreter.run(script);
		Log.info.println("Done running " + tabbedPane.getTitleAt(tabbedPane.getSelectedIndex()));
	}

	public void runCurrentSelection() {
		String script = getCurrentTextPane().getSelectedText();
		if (script == null || script.trim().length() == 0) {
			JOptionPane.showMessageDialog(this, "Select some code first");
			return;
		}
		studio.interpreter.run(script);
		Log.info.println("Done running selection from " + tabbedPane.getTitleAt(tabbedPane.getSelectedIndex()));
	}
}
