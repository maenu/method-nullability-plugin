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
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

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
		
		GridLayout gridLayout = new GridLayout(6, false);
	    gridLayout.verticalSpacing = 8;
	    newParent.setLayout(gridLayout);

	    Group group = new Group(newParent, SWT.NONE);
	    group.setText("Generate Eclipse External Annotations (EEA)");
	    group.setFont(newParent.getFont());
	    group.setLayout(gridLayout);
	    GridData gd= new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan= 6;
		gd.widthHint= SWT.DEFAULT;
		group.setLayoutData(gd);
	    
	    Label csvLabel = new Label(group, SWT.NULL);
	    csvLabel.setText("Path to CSV file: ");

	    Text csvText = new Text(group, SWT.SINGLE | SWT.BORDER);
	    GridData gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
	    gridData.horizontalSpan = 5;
	    gridData.grabExcessHorizontalSpace = true;
	    csvText.setLayoutData(gridData);

	    Label eeaLabel = new Label(group, SWT.NULL);
	    eeaLabel.setText("Path to EEA root: ");

	    Text eeaText = new Text(group, SWT.SINGLE | SWT.BORDER);
	    gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
	    gridData.horizontalSpan = 5;
	    gridData.grabExcessHorizontalSpace = true;
	    eeaText.setLayoutData(gridData);
	
	    Label maxNullabilityNonNullLabel = new Label(group, SWT.NULL);
	    maxNullabilityNonNullLabel.setText("Max. Nullability for @NonNull: ");

	    Text maxNullabilityNonNullText = new Text(group, SWT.SINGLE | SWT.BORDER);
	    maxNullabilityNonNullText.setText("0.1");
	    gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
	    gridData.horizontalSpan = 1;
	    gridData.grabExcessHorizontalSpace = false;
	    maxNullabilityNonNullText.setLayoutData(gridData);
	    addDecimalNumberVerifyListener(maxNullabilityNonNullText);
	    
	    Label maxNullabilityNonNullLabelFill = new Label(group, SWT.NULL);
	    gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
	    gridData.horizontalSpan = 4;
	    gridData.grabExcessHorizontalSpace = true;
	    maxNullabilityNonNullLabelFill.setLayoutData(gridData);
	    
	    Label minNullabilityNullableLabel = new Label(group, SWT.NULL);
	    minNullabilityNullableLabel.setText("Min. Nullability for @Nullable: ");

	    Text minNullabilityNullableText = new Text(group, SWT.SINGLE | SWT.BORDER);
	    minNullabilityNullableText.setText("0.2");
	    gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
	    gridData.horizontalSpan = 1;
	    gridData.grabExcessHorizontalSpace = false;
	    minNullabilityNullableText.setLayoutData(gridData);
	    addDecimalNumberVerifyListener(minNullabilityNullableText);
	    
	    Label minNullabilityNullableLabelFill = new Label(group, SWT.NULL);
	    gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
	    gridData.horizontalSpan = 4;
	    gridData.grabExcessHorizontalSpace = true;
	    minNullabilityNullableLabelFill.setLayoutData(gridData);
	    
	    Button enter = new Button(group, SWT.PUSH);
	    enter.setText("Generate");
	    gridData = new GridData();
	    gridData.horizontalSpan = 6;
	    gridData.horizontalAlignment = GridData.BEGINNING;
	    enter.setLayoutData(gridData);
		    
		  //register listener for the selection event
	    enter.addSelectionListener(new SelectionAdapter() {
	        @Override
	        public void widgetSelected(SelectionEvent e) {
	        	String csvPath = csvText.getText();
	        	String eeaPath = eeaText.getText();
				if (csvPath == null || csvPath.isEmpty() || eeaPath == null || eeaPath.isEmpty()) {
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
					
					CsvToEeaConverter converter = new CsvToEeaConverter(csvPath, eeaPath, maxNonNull, minNullable);
					BusyIndicator.showWhile(Display.getDefault(), new Runnable(){
					    @Override
						public void run(){
					    	try {
								converter.execute();
							} catch (Exception e) {
								throw new RuntimeException(e.getMessage(), e);
							}
					    }
					});
					
					MessageBox messageDialog = new MessageBox(newParent.getShell(), SWT.ICON_INFORMATION | SWT.OK);
	                messageDialog.setText("EEA Generation");
	                messageDialog.setMessage("EEA generation was successful.\n"
	                		+ "\nCSV records:\t" + converter.getTotalCsvRecords() 
	                		+ "\nEEA entries:\t" + converter.getProcessedCsvRecords()
	                		+ "\n@NonNull:\t" + converter.getNumNonNull()
	                		+ "\n@Nullable:\t" + converter.getNumNullable()
	                		+ "\nSkipped:\t\t" + converter.getSkippedCsvRecords());
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
