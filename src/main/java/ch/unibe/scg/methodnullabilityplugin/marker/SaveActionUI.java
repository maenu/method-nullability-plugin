package ch.unibe.scg.methodnullabilityplugin.marker;


import java.io.File;

import org.eclipse.jdt.ui.cleanup.CleanUpOptions;
import org.eclipse.jdt.ui.cleanup.ICleanUpConfigurationUI;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Text;

import ch.unibe.scg.methodnullabilityplugin.Console;
import ch.unibe.scg.methodnullabilityplugin.eea.CsvToEeaConverter;

public class SaveActionUI implements ICleanUpConfigurationUI {

	@Override
	public void setOptions(CleanUpOptions options) {
		Console.msg("SaveActionUI.options: " + options.getKeys());
	}

	@Override
	public Composite createContents(Composite parent) {
		Console.msg("SaveActionUI.createContents: " + parent);
		
		
		GridLayout gridLayout = new GridLayout(3, false);
	    gridLayout.verticalSpacing = 8;

	    parent.setLayout(gridLayout);

	    Label csvLabel = new Label(parent, SWT.NULL);
	    csvLabel.setText("Path to CSV source data: ");

	    Text csvText = new Text(parent, SWT.SINGLE | SWT.BORDER);
	    GridData gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
	    gridData.horizontalSpan = 3;
	    csvText.setLayoutData(gridData);

	    Label eeaLabel = new Label(parent, SWT.NULL);
	    eeaLabel.setText("Path to EEA root: ");

	    Text eeaText = new Text(parent, SWT.SINGLE | SWT.BORDER);
	    gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
	    gridData.horizontalSpan = 3;
	    eeaText.setLayoutData(gridData);
	
	    Button enter = new Button(parent, SWT.PUSH);
	    enter.setText("Convert");
	    gridData = new GridData();
	    gridData.horizontalSpan = 4;
	    gridData.horizontalAlignment = GridData.END;
	    enter.setLayoutData(gridData);
		    
		  //register listener for the selection event
	    enter.addSelectionListener(new SelectionAdapter() {
	        @Override
	        public void widgetSelected(SelectionEvent e) {
	        	String csvPath = csvText.getText();
	        	String eeaPath = eeaText.getText();
				if (csvPath == null || csvPath.isEmpty() || eeaPath == null || eeaPath.isEmpty()) {
					MessageBox messageDialog = new MessageBox(parent.getShell(), SWT.ERROR);
	                messageDialog.setText("Error");
	                messageDialog.setMessage("Both CSV and EEA paths are required.");
	                messageDialog.open();
	                return;
				}
				File csv = new File(csvPath);
				if(!csv.exists() || csv.isDirectory()) { 
					MessageBox messageDialog = new MessageBox(parent.getShell(), SWT.ERROR);
	                messageDialog.setText("Error");
	                messageDialog.setMessage("The CSV file does not exist.");
	                messageDialog.open();
	                return;
				}
				
				try {
					new CsvToEeaConverter().convert(csvPath, eeaPath);
					MessageBox messageDialog = new MessageBox(parent.getShell(), SWT.OK);
	                messageDialog.setText("Converter success");
	                messageDialog.setMessage("The conversion was successful.");
	                messageDialog.open();
	                
				} catch (Exception e1) {
					e1.printStackTrace();
					MessageBox messageDialog = new MessageBox(parent.getShell(), SWT.ERROR);
	                messageDialog.setText("Converter failed");
	                messageDialog.setMessage("The conversion failed:\n" + e1.getMessage());
	                messageDialog.open();
	                return;
				}
				
	        }
	    });
		    
		
		return null;
	}

	@Override
	public int getCleanUpCount() {
		Console.msg("SaveActionUI.getCleanUpCount: ");
		return 1;
	}

	@Override
	public int getSelectedCleanUpCount() {
		Console.msg("SaveActionUI.getSelectedCleanUpCount: ");
		return 1;
	}

	@Override
	public String getPreview() {
		Console.msg("SaveActionUI.getPreview: ");
		return "Nullability Save Action Preview";
	}

}
