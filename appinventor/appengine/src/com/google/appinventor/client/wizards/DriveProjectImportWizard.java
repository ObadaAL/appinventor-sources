// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2019 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.client.wizards;

import static com.google.appinventor.client.Ode.MESSAGES;

import com.google.appinventor.client.Ode;
import com.google.appinventor.client.OdeAsyncCallback;
import com.google.appinventor.shared.rpc.BlocksTruncatedException;
import com.google.appinventor.client.explorer.project.Project;
import com.google.appinventor.shared.rpc.project.UserProject;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.cellview.client.CellList;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy.KeyboardSelectionPolicy;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.view.client.ListDataProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Wizard for importing projects from the user's Drive backup folder. 
 * 
 * @author obada@mit.edu (Obada Alkhatib)
 */
public class DriveProjectImportWizard extends Wizard {

	public DriveProjectImportWizard() {
		super(MESSAGES.driveProjectImportWizard(), true, false);
		
		final Map<String, String> namesToIds = new HashMap<>();
		final VerticalPanel panel = new VerticalPanel();
		panel.setVerticalAlignment(VerticalPanel.ALIGN_MIDDLE);
		final Label label = new Label("No project selected.");
		final Button upload = new Button("Upload", new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				Ode.getInstance().getDriveService().getDriveProjectNames(
						new OdeAsyncCallback<Map<String, String>>() {
							
							@Override
							public void onSuccess(Map<String, String> projectNamesToIds) {
								// Window.alert("hey there!");
								// do stuff
								
								namesToIds.putAll(projectNamesToIds);
								final List<String> names = new ArrayList<>(projectNamesToIds.keySet());
								
								CellList<String> cellList = new CellList<String>(new TextCell());
								cellList.setKeyboardSelectionPolicy(KeyboardSelectionPolicy.ENABLED);
								
								cellList.setRowCount(names.size(), true);

							    // Push the data into the widget.
							    cellList.setRowData(0, names);
								
								// Add a selection model to handle user selection.
							    final SingleSelectionModel<String> selectionModel = new SingleSelectionModel<String>();
							    cellList.setSelectionModel(selectionModel);
							    selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
							        public void onSelectionChange(SelectionChangeEvent event) {
							        	String selected = selectionModel.getSelectedObject();
							        	if (selected != null) {
							        		label.setText(selected);
							            }
							        }
							    });
							    final ScrollPanel scrollPanel = new ScrollPanel(cellList);
							    scrollPanel.setSize("400px", "100px");
							    
							    DialogBox db = cellListDialogBox(scrollPanel, "Select Project to Import...");
							    db.show();
							}
							
							@Override
				            public void onFailure(Throwable caught) {
				                Window.alert(caught.getMessage());
				            }
						});
			}
		});
		panel.add(upload);
		panel.add(label);
		final Set<String> selectedProjects = new HashSet<>();
		
		setStylePrimaryName("ode-DialogBox");
	    addPage(panel);
		
		initFinishCommand(new Command() {
			@Override
			public void execute() {
				String projectName = label.getText();
				if (projectName == null || ! projectName.endsWith(".aia")) {
					Window.alert(MESSAGES.driveNoSelectedProject());
					center();
				} else {
					final Ode ode = Ode.getInstance();
					String projectId = namesToIds.get(projectName);
					ode.getDriveService().importProjectFromDrive(projectName, projectId, 
                        new OdeAsyncCallback<UserProject>() {
						    @Override
						    public void onSuccess(UserProject userProject) {
						    	if (userProject == null) {
						    		Window.alert("Import failure (project potentially exists)");
						    	} else {
						    		Project uploadedProject = ode.getProjectManager().addProject(userProject);
						    		Window.alert("Successfully imported project!");
						    		ode.openYoungAndroidProjectInDesigner(uploadedProject);
						    	}
						    }
						    
						    @Override
						    public void onFailure(Throwable caught) {
						    	Window.alert("Import failure (project potentially exists)");
						    }
					    });
				}
			}
		});
	}
	
	/**
	 * Makes a dialog box for a CellList object.
	 * 
	 * @param scrollPanel ScrollPanel object
	 * @param title title of the dialog box
	 * @return dialog box object
	 */
	private static DialogBox cellListDialogBox(ScrollPanel scrollPanel, 
											   String title) {
		final DialogBox db = new DialogBox(false, true);
		db.setText(title);
	    db.setStyleName("ode-DialogBox");
	    db.setHeight("200px");
	    db.setWidth("450px");
	    db.setGlassEnabled(true);
	    db.setAnimationEnabled(true);
	    db.center();
	    
		VerticalPanel boxContents = new VerticalPanel();
		boxContents.setVerticalAlignment(VerticalPanel.ALIGN_MIDDLE);
		boxContents.add(scrollPanel);
		boxContents.add(new Button("Select", new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				db.hide();
			}
		}));
		db.setWidget(boxContents);
		
		return db;
	}
	
	@Override
	public void show() {
		super.show();
	    // Wizard size (having it resize between page changes is quite annoying)
	    int width = 320;
	    int height = 100;
	    this.center();

	    setPixelSize(width, height);
	    super.setPagePanelHeight(100);
	}
}
