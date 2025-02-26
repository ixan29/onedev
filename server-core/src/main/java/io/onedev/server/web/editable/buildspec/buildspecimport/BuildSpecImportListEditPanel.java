package io.onedev.server.web.editable.buildspec.buildspecimport;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.HeadersToolbar;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.NoRecordsToolbar;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.convert.ConversionException;

import io.onedev.server.buildspec.BuildSpecImport;
import io.onedev.server.web.behavior.NoRecordsBehavior;
import io.onedev.server.web.behavior.sortable.SortBehavior;
import io.onedev.server.web.behavior.sortable.SortPosition;
import io.onedev.server.web.component.modal.ModalLink;
import io.onedev.server.web.component.modal.ModalPanel;
import io.onedev.server.web.component.svg.SpriteImage;
import io.onedev.server.web.editable.PropertyDescriptor;
import io.onedev.server.web.editable.PropertyEditor;
import io.onedev.server.web.editable.PropertyUpdating;

@SuppressWarnings("serial")
class BuildSpecImportListEditPanel extends PropertyEditor<List<Serializable>> {

	private final List<BuildSpecImport> imports;
	
	public BuildSpecImportListEditPanel(String id, PropertyDescriptor propertyDescriptor, IModel<List<Serializable>> model) {
		super(id, propertyDescriptor, model);
		
		imports = new ArrayList<>();
		for (Serializable each: model.getObject()) {
			imports.add((BuildSpecImport) each);
		}
	}
	
	@Override
	protected void onInitialize() {
		super.onInitialize();
		add(new ModalLink("addNew") {

			@Override
			protected Component newContent(String id, ModalPanel modal) {
				return new BuildSpecImportEditPanel(id, imports, -1) {

					@Override
					protected void onCancel(AjaxRequestTarget target) {
						modal.close();
					}

					@Override
					protected void onSave(AjaxRequestTarget target) {
						markFormDirty(target);
						modal.close();
						onPropertyUpdating(target);
						target.add(BuildSpecImportListEditPanel.this);
					}

				};
			}
			
		});
		
		List<IColumn<BuildSpecImport, Void>> columns = new ArrayList<>();
		
		columns.add(new AbstractColumn<BuildSpecImport, Void>(Model.of("")) {

			@Override
			public void populateItem(Item<ICellPopulator<BuildSpecImport>> cellItem, String componentId, 
					IModel<BuildSpecImport> rowModel) {
				cellItem.add(new SpriteImage(componentId, "grip") {

					@Override
					protected void onComponentTag(ComponentTag tag) {
						super.onComponentTag(tag);
						tag.setName("svg");
						tag.put("class", "icon drag-indicator");
					}
					
				});
			}
			
			@Override
			public String getCssClass() {
				return "minimum actions";
			}
			
		});		
		
		columns.add(new AbstractColumn<BuildSpecImport, Void>(Model.of("Project")) {

			@Override
			public void populateItem(Item<ICellPopulator<BuildSpecImport>> cellItem, String componentId, 
					IModel<BuildSpecImport> rowModel) {
				cellItem.add(new Label(componentId, rowModel.getObject().getProjectName()));
			}
		});		
		
		columns.add(new AbstractColumn<BuildSpecImport, Void>(Model.of("Tag")) {

			@Override
			public void populateItem(Item<ICellPopulator<BuildSpecImport>> cellItem, String componentId, 
					IModel<BuildSpecImport> rowModel) {
				cellItem.add(new Label(componentId, rowModel.getObject().getTag()));
			}
			
		});		
		
		columns.add(new AbstractColumn<BuildSpecImport, Void>(Model.of("")) {

			@Override
			public void populateItem(Item<ICellPopulator<BuildSpecImport>> cellItem, String componentId, 
					IModel<BuildSpecImport> rowModel) {
				Fragment fragment = new Fragment(componentId, "actionColumnFrag", BuildSpecImportListEditPanel.this);
				fragment.add(new ModalLink("edit") {

					@Override
					protected Component newContent(String id, ModalPanel modal) {
						return new BuildSpecImportEditPanel(id, imports, cellItem.findParent(Item.class).getIndex()) {

							@Override
							protected void onCancel(AjaxRequestTarget target) {
								modal.close();
							}

							@Override
							protected void onSave(AjaxRequestTarget target) {
								markFormDirty(target);
								modal.close();
								onPropertyUpdating(target);
								target.add(BuildSpecImportListEditPanel.this);
							}

						};
					}
					
				});
				fragment.add(new AjaxLink<Void>("delete") {

					@Override
					public void onClick(AjaxRequestTarget target) {
						markFormDirty(target);
						imports.remove(rowModel.getObject());
						onPropertyUpdating(target);
						target.add(BuildSpecImportListEditPanel.this);
					}
					
				});
				cellItem.add(fragment);
			}

			@Override
			public String getCssClass() {
				return "actions minimum";
			}
			
		});		
		
		IDataProvider<BuildSpecImport> dataProvider = new ListDataProvider<BuildSpecImport>() {

			@Override
			protected List<BuildSpecImport> getData() {
				return imports;			
			}

		};
		
		DataTable<BuildSpecImport, Void> dataTable;
		add(dataTable = new DataTable<BuildSpecImport, Void>("imports", columns, dataProvider, Integer.MAX_VALUE));
		dataTable.addTopToolbar(new HeadersToolbar<Void>(dataTable, null));
		dataTable.addBottomToolbar(new NoRecordsToolbar(dataTable, Model.of("Not defined")));
		dataTable.add(new NoRecordsBehavior());
		dataTable.add(new SortBehavior() {

			@Override
			protected void onSort(AjaxRequestTarget target, SortPosition from, SortPosition to) {
				int fromIndex = from.getItemIndex();
				int toIndex = to.getItemIndex();
				if (fromIndex < toIndex) {
					for (int i=0; i<toIndex-fromIndex; i++) 
						Collections.swap(imports, fromIndex+i, fromIndex+i+1);
				} else {
					for (int i=0; i<fromIndex-toIndex; i++) 
						Collections.swap(imports, fromIndex-i, fromIndex-i-1);
				}
				onPropertyUpdating(target);
				target.add(BuildSpecImportListEditPanel.this);
			}
			
		}.sortable("tbody"));
	}

	@Override
	public void onEvent(IEvent<?> event) {
		super.onEvent(event);
		
		if (event.getPayload() instanceof PropertyUpdating) {
			event.stop();
			onPropertyUpdating(((PropertyUpdating)event.getPayload()).getHandler());
		}		
	}

	@Override
	protected List<Serializable> convertInputToValue() throws ConversionException {
		List<Serializable> value = new ArrayList<>();
		for (BuildSpecImport each: imports)
			value.add(each);
		return value;
	}

}
