package methods;


import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.PrintStream;
import java.util.*;

import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.text.StyledDocument;

import beast.app.beauti.BeautiConfig;
import beast.app.beauti.BeautiDoc;
import beast.app.beauti.BeautiSubTemplate;
import beast.app.beauti.InputFilter;
import beast.app.draw.InputEditor;
import beast.core.BEASTInterface;
import beast.core.Description;
import beast.core.Distribution;
import beast.core.Input;
import beast.core.StateNode;
import beast.core.util.CompoundDistribution;
import beast.core.util.Log;
import beast.evolution.likelihood.GenericTreeLikelihood;
import beast.evolution.operators.DeltaExchangeOperator;
import beast.evolution.tree.TreeInterface;
import beast.util.XMLParser;
import beast.util.XMLProducer;
import beast.core.MCMC;
import beast.core.Operator;

@Description("Convert MCMC analysis in XML file to a methods section")
public class XML2TextPane extends JTextPane implements ActionListener {
	
	BeautiDoc beautiDoc;
	public XML2TextPane(String [] args) throws Exception {
		beautiDoc = new BeautiDoc();
		File file = new File(args[0]);
		beautiDoc.setFileName(file.getAbsolutePath());
		beautiDoc.beautiConfig = new BeautiConfig();
		beautiDoc.beautiConfig.initAndValidate();		
		String xml = beautiDoc.load(file);
		int i = xml.indexOf("beautitemplate=");
		if (i > 0) {
			i += 15;
			char c = xml.charAt(i);
			i++;
			int start = i;
			while (xml.charAt(i) != c) {
				i++;
			}
			String template = xml.substring(start, i);
			if (!template.endsWith("xml")) {
				template = template + ".xml";
			}
			beautiDoc.loadNewTemplate(template);
		} else {
			beautiDoc.loadNewTemplate("Standard.xml");
		}
		
		XMLParser parser = new XMLParser();
		MCMC mcmc = (MCMC) parser.parseFile(file);
		beautiDoc.mcmc.setValue(mcmc, beautiDoc);
		for (BEASTInterface o : InputFilter.getDocumentObjects(beautiDoc.mcmc.get())) {
			beautiDoc.registerPlugin(o);
		}
		
		MethodsText.initNameMap();
		initialise((MCMC) beautiDoc.mcmc.get());
	}
	
	
	private void refreshText()  throws Exception {
//		XMLProducer p = new XMLProducer();
//		String xml = p.toXML(beautiDoc.mcmc.get());
//		PrintStream ps = new PrintStream(new File("/tmp/beast-raw.xml"));
//		ps.println(xml);
//		ps.close();
//		
//		beautiDoc.save("/tmp/beast.xml");		
		beautiDoc.determinePartitions();
		beautiDoc.scrubAll(false, false);

		StyledDocument doc = getStyledDocument();
		doc.remove(0, doc.getLength());
		MethodsText.done.clear();
		initialise((MCMC) beautiDoc.mcmc.get());
	}
	
	public void initialise(MCMC mcmc) throws Exception {
        CompoundDistribution posterior = (CompoundDistribution) mcmc.posteriorInput.get();
		StringBuilder b = new StringBuilder();


		List<Phrase> m = new ArrayList<>();

        for (Distribution distr : posterior.pDistributions.get()) {
            if (distr.getID().equals("likelihood")) {
            	String partitionDescription = getPartitionDescription((CompoundDistribution) distr);
            	b.append(partitionDescription);
            	b.append("\n");
            }
        }
		
        
        
        // collect model descriptions of all partitions
        List<String> partitionIDs = new ArrayList<>();
        List<List<Phrase>> partitionModels = new ArrayList<>();
        
        for (Distribution distr : posterior.pDistributions.get()) {
            if (distr.getID().equals("likelihood")) {
                for (Distribution likelihood : ((CompoundDistribution) distr).pDistributions.get()) {
                    if (likelihood instanceof GenericTreeLikelihood) {
                        GenericTreeLikelihood treeLikelihood = (GenericTreeLikelihood) likelihood;
                    	partitionIDs.add(treeLikelihood.dataInput.get().getID());
                    	List<Phrase> modelDescription = getModelDescription(treeLikelihood);
                    	partitionModels.add(modelDescription);
                    }
                }
            }
        }
        
        // amalgamate partitions
        for (int i = 0; i < partitionIDs.size(); i++) {
        	if (partitionModels.get(i) != null) {
                List<String> currentPartitionIDs = new ArrayList<>();
                currentPartitionIDs.add(partitionIDs.get(i));
                String model = Phrase.toSimpleString(partitionModels.get(i));

                List<List<Phrase>> selected = new ArrayList<>();
                selected.add(partitionModels.get(i));
                for (int j = i + 1; j < partitionIDs.size(); j++) {
                	if (Phrase.toSimpleString(partitionModels.get(j)).equals(model)) {
                        selected.add(partitionModels.get(j));
                		partitionModels.set(j, null);
                		currentPartitionIDs.add(partitionIDs.get(j));
                	}
                }
                // translate to text
                
                model = Phrase.toString(selected.toArray(new List[]{}));
            	m.clear();
                if (currentPartitionIDs.size() > 1) {
                	StringBuilder b2 = new StringBuilder();
                	b2.append("Partitions ");
                	b2.append(XML2Text.printParitions(currentPartitionIDs));
                	b.append(b2.toString());
                	m.add(new Phrase(b2.toString()));
                	b2.append(model + "\n");

                } else {
                	m.add(new Phrase("Partitions " + currentPartitionIDs.get(0)));
                	b.append("Partitions " + currentPartitionIDs.get(0) + " " + model + "\n");                	
                }
                
                if (model.trim().length() > 0) {
                	List<Phrase> [] phrases = new List[1];
                	phrases[0] = m;
                	Phrase.addTextToDocument(getStyledDocument(), this, beautiDoc, phrases);
                }

                Phrase.addTextToDocument(getStyledDocument(), this, beautiDoc, selected.toArray(new List[]{}));
        	}
        }

        List<Phrase> [] phrases = new List[1];
    	m.add(new Phrase(b.toString()));
    	phrases[0] = m;
//        Phrase.addTextToDocument(getStyledDocument(), this, beautiDoc, phrases);
        
        // tree priors        
        Set<TreeInterface> trees = new LinkedHashSet<>();
        for (Distribution distr : posterior.pDistributions.get()) {
            if (distr.getID().equals("likelihood")) {
                for (Distribution likelihood : ((CompoundDistribution) distr).pDistributions.get()) {
                    if (likelihood instanceof GenericTreeLikelihood) {
                        GenericTreeLikelihood treeLikelihood = (GenericTreeLikelihood) likelihood;
                    	trees.add(treeLikelihood.treeInput.get());
                    }
                }
            }
        }
        
        for (TreeInterface tree : trees) {
        	m = MethodsTextFactory.getModelDescription(tree);
        	m.set(0, new Phrase("Tree prior: "));
        	b.append(Phrase.toString(m));
        	phrases = new List[1];
        	phrases[0] = m;
            Phrase.addTextToDocument(getStyledDocument(), this, beautiDoc, phrases);
        }
		Log.warning(b.toString());
        b = new StringBuilder();
        b.append("\n\n");
        
        // has FixMeanMutationRatesOperator?
        for (Operator op : mcmc.operatorsInput.get()) {
        	if (op.getID().equals("FixMeanMutationRatesOperator")) {
        		b.append("Relative substitution rates among partitions ");
                partitionIDs = new ArrayList<>();
                for (StateNode s : ((DeltaExchangeOperator)op).parameterInput.get()) {
                	partitionIDs.add(BeautiDoc.parsePartition(s.getID()));
                }
                b.append(XML2Text.printParitions(partitionIDs));
        		b.append("are estimated.\n");
        		m.clear();
        		m.add(new Phrase(b.toString()));
        		phrases[0] = m;
                Phrase.addTextToDocument(getStyledDocument(), this, beautiDoc, phrases);
        	}
        }

		Log.warning(b.toString());
		Log.warning("Done!");
	}
	

	@Override
    public void actionPerformed(ActionEvent e) {
    	if (e.getSource() instanceof JComboBox) {
    		JComboBox<String> b = (JComboBox<String>) e.getSource();
    		String cmd = e.getActionCommand();
    		int k = cmd.lastIndexOf(' ');
    		String pid = cmd.substring(0, k);
    		String inputName = cmd.substring(k + 1);
    		System.out.println("You selected " + b.getSelectedItem() + " for " + e.getActionCommand());
    		BEASTInterface m_beastObject = beautiDoc.pluginmap.get(pid);
    		Input<?> input = m_beastObject.getInput(inputName);
    		
            BeautiSubTemplate selected = (BeautiSubTemplate) b.getSelectedItem();
            BEASTInterface beastObject = (BEASTInterface) input.get();
            String id = beastObject.getID();
            String partition = id.indexOf('.') >= 0 ? 
            		id.substring(id.indexOf('.') + 1) : "";
            if (partition.indexOf(':') >= 0) {
            	partition = id.substring(id.indexOf(':') + 1);
            }
            if (selected.equals(InputEditor.NO_VALUE)) {
                beastObject = null;
            } else {
                try {
                    beastObject = selected.createSubNet(beautiDoc.getContextFor(beastObject), m_beastObject, input, true);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null, "Could not select beastObject: " +
                            ex.getClass().getName() + " " +
                            ex.getMessage()
                    );
                }
            }


            try {
                if (beastObject == null) {
                    b.setSelectedItem(InputEditor.NO_VALUE);
                } else {
                    if (!input.canSetValue(beastObject, m_beastObject)) {
                        throw new IllegalArgumentException("Cannot set input to this value");
                    }
                }

                input.setValue(beastObject, m_beastObject);

                refreshText();
            } catch (Exception ex) {
                id = ((BEASTInterface) input.get()).getID();
                b.setSelectedItem(id);
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null, "Could not change beastObject: " +
                        ex.getClass().getName() + " " +
                        ex.getMessage() 
                );
            }
    		
    		
    	} else if (e.getSource() instanceof JTextField) {
    		JTextField b = (JTextField) e.getSource();
    		String cmd = e.getActionCommand();
    		int k = cmd.lastIndexOf(' ');
    		String id = cmd.substring(0, k);
    		String inputName = cmd.substring(k + 1);
    		String value = b.getText();
    		System.out.println("You selected " + b.getText() + " for " + cmd);
    		BEASTInterface o = beautiDoc.pluginmap.get(id);
    		Input<?> input = o.getInput(inputName);
    		if (input.canSetValue(value, o)) {
    			try {
    				input.setValue(value, o);
    			} catch (RuntimeException ex) {
    				// could not set the value after all...
    			}
    		}
    		System.out.println(id + "." + input.getName() + " set to " + input.get().toString());
    		
    	}
    }

    private String getPartitionDescription(CompoundDistribution distr) {
		// TODO Auto-generated method stub
		return null;
	}

	private List<Phrase> getModelDescription(GenericTreeLikelihood treeLikelihood) {
		return MethodsTextFactory.getModelDescription(treeLikelihood);
	}

	public static void main(String[] args) throws Exception {
        XML2TextPane textPane = new XML2TextPane(args);
        
        JScrollPane paneScrollPane = new JScrollPane(textPane);
        paneScrollPane.setVerticalScrollBarPolicy(
                        JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        paneScrollPane.setPreferredSize(new Dimension(650, 455));
        paneScrollPane.setMinimumSize(new Dimension(10, 10));

        //Create and set up the window.
        JFrame frame = new JFrame("XML2TextPane");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.add(paneScrollPane);

        frame.pack();
        frame.setVisible(true);
        
	}
}