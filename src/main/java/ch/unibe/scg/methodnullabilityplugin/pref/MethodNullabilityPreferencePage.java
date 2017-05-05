package ch.unibe.scg.methodnullabilityplugin.pref;

import java.io.File;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
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
				
				try {
					CsvToEeaConverter converter = new CsvToEeaConverter();
					converter.execute(csvPath, eeaPath);
					MessageBox messageDialog = new MessageBox(newParent.getShell(), SWT.ICON_INFORMATION | SWT.OK);
	                messageDialog.setText("EEA generation");
	                messageDialog.setMessage("The EEA generation was successful.\n\nNumber of CSV records: " 
	                		+ converter.getTotalCsvRecords() + "\nNumber of EEA entries: " + converter.getProcessedCsvRecords());
	                messageDialog.open();
	                
				} catch (Exception e1) {
					e1.printStackTrace();
					MessageBox messageDialog = new MessageBox(newParent.getShell(), SWT.ERROR);
	                messageDialog.setText("Converter failed");
	                messageDialog.setMessage("The conversion failed:\n" + e1.getMessage());
	                messageDialog.open();
	                return;
				}
				
	        }
	    });
		
		return newParent;
	}

}
