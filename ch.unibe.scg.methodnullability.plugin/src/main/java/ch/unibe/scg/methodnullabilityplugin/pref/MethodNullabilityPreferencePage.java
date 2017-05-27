package ch.unibe.scg.methodnullabilityplugin.pref;

import java.io.File;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;

import ch.unibe.scg.methodnullabilityplugin.Console;
import ch.unibe.scg.methodnullabilityplugin.eea.CsvToEeaConverter;

public class MethodNullabilityPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	public MethodNullabilityPreferencePage(){
		super("Method Nullability");
	}
	
	@Override
	public void init(IWorkbench workbench) {
	}

	@Override
	protected Control createContents(Composite parent) {
		noDefaultAndApplyButton();
		
		Composite newParent = new Composite(parent, SWT.NONE);
		
		GridLayout gridLayout1 = new GridLayout(6, false);
	    gridLayout1.verticalSpacing = 8;
	    newParent.setLayout(gridLayout1);
		
		Group group1 = new Group(newParent, SWT.NONE);
	    group1.setText("Configuration");
	    group1.setFont(newParent.getFont());
	    group1.setLayout(gridLayout1);
	    GridData gd1= new GridData(GridData.FILL_HORIZONTAL);
		gd1.horizontalSpan= 6;
		gd1.widthHint= SWT.DEFAULT;
		group1.setLayoutData(gd1);
		
		Link link = new Link(group1, SWT.NONE);
		link.setText("Enable annotation-based null analysis <A>here</A>.\n");
		link.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				IWorkbenchPreferenceContainer container= (IWorkbenchPreferenceContainer) getContainer();
				container.openPage("org.eclipse.jdt.ui.preferences.ProblemSeveritiesPreferencePage", null);
			}
		});
		
		GridLayout gridLayout2 = new GridLayout(6, false);
	    gridLayout2.verticalSpacing = 8;
	    newParent.setLayout(gridLayout2);

	    Group group2 = new Group(newParent, SWT.NONE);
	    group2.setText("Generate Javadoc and Eclipse External Annotations (EEA)");
	    group2.setFont(newParent.getFont());
	    group2.setLayout(gridLayout2);
	    GridData gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan= 6;
		gd.widthHint= SWT.DEFAULT;
		group2.setLayoutData(gd);
	    
	    Label csvLabel = new Label(group2, SWT.NULL);
	    csvLabel.setText("Path to CSV file: ");

	    Text csvText = new Text(group2, SWT.SINGLE | SWT.BORDER);
	    GridData gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
	    gridData.horizontalSpan = 4;
	    gridData.grabExcessHorizontalSpace = true;
	    csvText.setLayoutData(gridData);
	    
	    Button fileDialogButton = new Button(group2, SWT.PUSH);
	    fileDialogButton.setText("...");
	    fileDialogButton.addSelectionListener(new SelectionAdapter() {
        	@Override
        	public void widgetSelected(SelectionEvent e) {
        		FileDialog fileDialog = new FileDialog(group2.getShell(), SWT.OPEN);
            	fileDialog.setFilterExtensions(new String[] {"*.csv"});
            	String name = fileDialog.open();
            	if (name != null && !name.isEmpty()) {
            		csvText.setText(name);
            	}
        	}
		});

	    new Label(group2, SWT.NULL); // dummy
	    
	    Label eeaCbLabel = new Label(group2, SWT.NULL);
	    eeaCbLabel.setText("EEA: ");
	    
	    Button eeaCb = new Button(group2, SWT.CHECK);
	    eeaCb.setSelection(true);
	    gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
	    gridData.horizontalSpan = 1;
	    gridData.grabExcessHorizontalSpace = false;	    
	    eeaCb.setLayoutData(gridData);
	    
	    Label javadocCbLabel = new Label(group2, SWT.NULL);
	    javadocCbLabel.setText("Javadoc: ");
	    
	    Button javadocCb = new Button(group2, SWT.CHECK);
	    javadocCb.setSelection(true);
	    gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
	    gridData.horizontalSpan = 2;
	    gridData.grabExcessHorizontalSpace = true;
	    javadocCb.setLayoutData(gridData);
	    
	    
	    Label eeaLabel = new Label(group2, SWT.NULL);
	    eeaLabel.setText("Path to EEA root: ");

	    Text eeaText = new Text(group2, SWT.SINGLE | SWT.BORDER);
	    gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
	    gridData.horizontalSpan = 5;
	    gridData.grabExcessHorizontalSpace = true;
	    eeaText.setLayoutData(gridData);
	    
	    Label aIdLabel = new Label(group2, SWT.NULL);
	    aIdLabel.setText("List of artifactId: ");

	    Text aiText = new Text(group2, SWT.SINGLE | SWT.BORDER);
	    gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
	    gridData.horizontalSpan = 5;
	    gridData.grabExcessHorizontalSpace = true;
	    aiText.setLayoutData(gridData);
	
	    Label maxNullabilityNonNullLabel = new Label(group2, SWT.NULL);
	    maxNullabilityNonNullLabel.setText("Max. Nullability for @NonNull: ");

	    Text maxNullabilityNonNullText = new Text(group2, SWT.SINGLE | SWT.BORDER);
	    maxNullabilityNonNullText.setText("0.1");
	    gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
	    gridData.horizontalSpan = 1;
	    gridData.grabExcessHorizontalSpace = false;
	    maxNullabilityNonNullText.setLayoutData(gridData);
	    addDecimalNumberVerifyListener(maxNullabilityNonNullText);
	    
	    Label maxNullabilityNonNullLabelFill = new Label(group2, SWT.NULL);
	    gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
	    gridData.horizontalSpan = 4;
	    gridData.grabExcessHorizontalSpace = true;
	    maxNullabilityNonNullLabelFill.setLayoutData(gridData);
	    
	    Label minNullabilityNullableLabel = new Label(group2, SWT.NULL);
	    minNullabilityNullableLabel.setText("Min. Nullability for @Nullable: ");

	    Text minNullabilityNullableText = new Text(group2, SWT.SINGLE | SWT.BORDER);
	    minNullabilityNullableText.setText("0.2");
	    gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
	    gridData.horizontalSpan = 1;
	    gridData.grabExcessHorizontalSpace = false;
	    minNullabilityNullableText.setLayoutData(gridData);
	    addDecimalNumberVerifyListener(minNullabilityNullableText);
	    
	    Label minNullabilityNullableLabelFill = new Label(group2, SWT.NULL);
	    gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
	    gridData.horizontalSpan = 4;
	    gridData.grabExcessHorizontalSpace = true;
	    minNullabilityNullableLabelFill.setLayoutData(gridData);
	    
	    Button enter = new Button(group2, SWT.PUSH);
	    enter.setText("Generate");
	    gridData = new GridData();
	    gridData.horizontalSpan = 6;
	    gridData.horizontalAlignment = GridData.BEGINNING;
	    enter.setLayoutData(gridData);
	    
	    eeaCb.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				boolean newSelection = ((Button) e.getSource()).getSelection();
				eeaText.setEnabled(newSelection);
				if (!javadocCb.getSelection() && !newSelection) {
					enter.setEnabled(false);
				} else {
					enter.setEnabled(true);
				}
			}
		});
	    javadocCb.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				boolean newSelection = ((Button) e.getSource()).getSelection();
				if (!eeaCb.getSelection() && !newSelection) {
					enter.setEnabled(false);
				} else {
					enter.setEnabled(true);
				}
			}
		});
	    
	    
		//register listener for the selection event
	    enter.addSelectionListener(new SelectionAdapter() {
	    	
	        @Override
	        public void widgetSelected(SelectionEvent e) {
	        	enter.setEnabled(false);
	        	try {
	        		generate();
	        	} finally {
	        		enter.setEnabled(true);
	        	}
	        }
	        
	        private void generate() {
	        	String csvPath = csvText.getText();
	        	String eeaPath = eeaText.getText();
				if (csvPath == null || csvPath.isEmpty() || (eeaCb.getSelection() && (eeaPath == null || eeaPath.isEmpty()))) {
					MessageBox messageDialog = new MessageBox(newParent.getShell(), SWT.ERROR);
	                messageDialog.setText("Error");
	                messageDialog.setMessage("Both CSV and EEA paths are required.");
	                messageDialog.open();
	                return;
				}
				File csv = new File(csvPath);
				if(!csv.exists() || csv.isDirectory()) { 
					MessageBox messageDialog = new MessageBox(newParent.getShell(), SWT.ERROR);
	                messageDialog.setText("Error");
	                messageDialog.setMessage("The CSV file does not exist.");
	                messageDialog.open();
	                return;
				}
				
				double minNullable, maxNonNull;
				try {
					String minStr = minNullabilityNullableText.getText().trim();
					minNullable = Double.parseDouble(minStr);
					String maxStr = maxNullabilityNonNullText.getText().trim();
					maxNonNull = Double.parseDouble(maxStr);
					if (maxNonNull > minNullable) {
						MessageBox messageDialog = new MessageBox(newParent.getShell(), SWT.ERROR);
		                messageDialog.setText("Error");
		                messageDialog.setMessage("Max. Nullability must be less than Min. Nullability.");
		                messageDialog.open();
		                return;
					}
					if (maxNonNull == 0 && minNullable == 0) {
						MessageBox messageDialog = new MessageBox(newParent.getShell(), SWT.ERROR);
		                messageDialog.setText("Error");
		                messageDialog.setMessage("Max. and Min. Nullability must not both be 0.");
		                messageDialog.open();
		                return;
					}
				} catch (RuntimeException re) {
					MessageBox messageDialog = new MessageBox(newParent.getShell(), SWT.ERROR);
	                messageDialog.setText("Error");
	                messageDialog.setMessage("Max. and Min. Nullability must be specified.");
	                messageDialog.open();
	                return;
				}
				
				try {
					
					CsvToEeaConverter converter = new CsvToEeaConverter(csvPath, 
							javadocCb.getSelection(), eeaCb.getSelection(), 
							eeaPath, aiText.getText(), maxNonNull, minNullable);
					BusyIndicator.showWhile(Display.getDefault(), new Runnable(){
					    @Override
						public void run(){
					    	try {
								converter.execute();
							} catch (Exception e) {
								Console.err(e);
								throw new RuntimeException(e.getMessage(), e);
							}
					    }
					});
					
					MessageBox messageDialog = new MessageBox(newParent.getShell(), SWT.ICON_INFORMATION | SWT.OK);
	                messageDialog.setText("Javadoc database and EEA Generation");
	                String message = "";
	                if (javadocCb.getSelection()) {
	                	message = "Generation of Javadoc database was successful.\n";
	                	message += "\nRecords created:\t" + converter.getNumRecordsJavadocDatabase();
	                }
	                if (eeaCb.getSelection()) {
	                	message = message.isEmpty() ? message : message + "\n\n";
	                	message += "Generation of EEA was successful.\n"
		                		+ "\nCSV records:\t" + converter.getTotalCsvRecords() 
		                		+ "\nEEA entries:\t" + converter.getProcessedCsvRecords()
		                		+ "\n@NonNull:\t" + converter.getNumNonNull()
		                		+ "\n@Nullable:\t" + converter.getNumNullable()
		                		+ "\nSkipped:\t\t" + converter.getSkippedCsvRecords();
	                }
	                
	                messageDialog.setMessage(message);
	                messageDialog.open();
	                
				} catch (Exception e1) {
					MessageBox messageDialog = new MessageBox(newParent.getShell(), SWT.ERROR);
	                messageDialog.setText("EEA Generation");
	                messageDialog.setMessage("Failure:\n" + e1.getMessage());
	                messageDialog.open();
	                return;
				}
	        }
	    });
		
		return newParent;
	}

	private void addDecimalNumberVerifyListener(Text minNullabilityNullableText) {
		minNullabilityNullableText.addVerifyListener(new VerifyListener() {

	        @Override
	        public void verifyText(VerifyEvent e) {

	            Text text = (Text)e.getSource();

	            // get old text and create new text by using the VerifyEvent.text
	            final String oldS = text.getText();
	            String newS = oldS.substring(0, e.start) + e.text + oldS.substring(e.end);

	            boolean isValid = true;
	            try
	            {
	                double parseDouble = Double.parseDouble(newS);
	                if (parseDouble < 0 || parseDouble > 1) {
	                	isValid = false;
	                }
	            }
	            catch(NumberFormatException ex)
	            {
	                isValid = false;
	            }

	            if(!isValid)
	                e.doit = false;
	        }
	    });
	}

}
