/**
 * Licensed to JumpMind Inc under one or more contributor
 * license agreements.  See the NOTICE file distributed
 * with this work for additional information regarding
 * copyright ownership.  JumpMind Inc licenses this file
 * to you under the GNU General Public License, version 3.0 (GPLv3)
 * (the "License"); you may not use this file except in compliance
 * with the License.
 *
 * You should have received a copy of the GNU General Public License,
 * version 3.0 (GPLv3) along with this library; if not, see
 * <http://www.gnu.org/licenses/>.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jumpmind.metl.ui.views.design;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.jumpmind.metl.core.model.ComponentAttributeSetting;
import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.model.ModelAttribute;
import org.jumpmind.metl.core.model.ModelEntity;
import org.jumpmind.metl.core.runtime.component.ModelAttributeScriptHelper;
import org.jumpmind.metl.core.runtime.component.Transformer;
import org.jumpmind.metl.ui.common.ButtonBar;
import org.jumpmind.metl.ui.common.UiUtils;

import com.vaadin.data.Container;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.event.FieldEvents.TextChangeEvent;
import com.vaadin.event.FieldEvents.TextChangeListener;
import com.vaadin.ui.AbstractSelect;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Field;
import com.vaadin.ui.Table;
import com.vaadin.ui.Table.ColumnGenerator;
import com.vaadin.ui.TableFieldFactory;
import com.vaadin.ui.TextField;

@SuppressWarnings("serial")
public class EditTransformerPanel extends AbstractComponentEditPanel {

    Table table = new Table();

    TextField filterField;

    AbstractSelect filterPopField;

    List<ComponentAttributeSetting> componentAttributes;

    BeanItemContainer<ComponentAttributeSetting> container = new BeanItemContainer<ComponentAttributeSetting>(
            ComponentAttributeSetting.class);

    static final String SHOW_ALL = "Show All";
    static final String SHOW_POPULATED_ENTITIES = "Show Entities with Transforms";
    static final String SHOW_POPULATED_ATTRIBUTES = "Show Attributes with Transforms";

    protected void buildUI() {
        ButtonBar buttonBar = new ButtonBar();
        addComponent(buttonBar);

        filterPopField = new ComboBox();
        filterPopField.addItem(SHOW_ALL);
        filterPopField.addItem(SHOW_POPULATED_ENTITIES);
        filterPopField.addItem(SHOW_POPULATED_ATTRIBUTES);
        filterPopField.setNullSelectionAllowed(false);
        filterPopField.setImmediate(true);
        filterPopField.setWidth(20, Unit.EM);
        filterPopField.setValue(SHOW_ALL);
        filterPopField.addValueChangeListener(new ValueChangeListener() {
            public void valueChange(ValueChangeEvent event) {
                updateTable(filterField.getValue());
            }
        });
        buttonBar.addLeft(filterPopField);

        filterField = buttonBar.addFilter();
        filterField.addTextChangeListener(new TextChangeListener() {

            @Override
            public void textChange(TextChangeEvent event) {
                filterField.setValue(event.getText());
                updateTable(event.getText());
            }
        });

        addComponent(buttonBar);

        table.setContainerDataSource(container);

        table.setSelectable(true);
        table.setSortEnabled(false);
        table.setImmediate(true);
        table.setSortEnabled(true);
        table.setSizeFull();
        table.addGeneratedColumn("entityName", new ColumnGenerator() {

            @Override
            public Object generateCell(Table source, Object itemId, Object columnId) {
                ComponentAttributeSetting setting = (ComponentAttributeSetting) itemId;
                Model model = component.getInputModel();
                ModelAttribute attribute = model.getAttributeById(setting.getAttributeId());
                ModelEntity entity = model.getEntityById(attribute.getEntityId());
                return UiUtils.getName(filterField.getValue(), entity.getName());
            }
        });
        table.addGeneratedColumn("attributeName", new ColumnGenerator() {

            @Override
            public Object generateCell(Table source, Object itemId, Object columnId) {
                ComponentAttributeSetting setting = (ComponentAttributeSetting) itemId;
                Model model = component.getInputModel();
                ModelAttribute attribute = model.getAttributeById(setting.getAttributeId());
                return UiUtils.getName(filterField.getValue(), attribute.getName());
            }
        });
        table.setVisibleColumns(new Object[] { "entityName", "attributeName", "value" });
        table.setColumnWidth("entityName", 250);
        table.setColumnWidth("attributeName", 250);
        table.setColumnHeaders(new String[] { "Entity Name", "Attribute Name", "Transform" });
        table.setColumnExpandRatio("value", 1);
        table.setTableFieldFactory(new EditFieldFactory());
        table.setEditable(true);
        addComponent(table);
        setExpandRatio(table, 1.0f);

        if (component.getInputModel() != null) {

            componentAttributes = component.getAttributeSettings();
            
            List<ComponentAttributeSetting> toRemove = new ArrayList<ComponentAttributeSetting>();
            for (ComponentAttributeSetting componentAttribute : componentAttributes) {
                Model model = component.getInputModel();
                ModelAttribute attribute1 = model.getAttributeById(componentAttribute.getAttributeId());
                if (attribute1 == null) {
                    /* invalid attribute.  model must have changed.  lets remove it */
                    toRemove.add(componentAttribute);
                }
            }
            
            for (ComponentAttributeSetting componentAttributeSetting : toRemove) {
                componentAttributes.remove(componentAttributeSetting);
                context.getConfigurationService().delete(componentAttributeSetting);
            }

            for (ModelEntity entity : component.getInputModel().getModelEntities()) {
                for (ModelAttribute attr : entity.getModelAttributes()) {
                    boolean found = false;
                    for (ComponentAttributeSetting componentAttribute : componentAttributes) {
                        if (componentAttribute.getAttributeId().equals(attr.getId())
                                && componentAttribute.getName().equals(
                                        Transformer.TRANSFORM_EXPRESSION)) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        componentAttributes.add(new ComponentAttributeSetting(attr.getId(),
                                component.getId(), Transformer.TRANSFORM_EXPRESSION, null));
                    }
                }
            }

            Collections.sort(componentAttributes, new Comparator<ComponentAttributeSetting>() {
                @Override
                public int compare(ComponentAttributeSetting o1, ComponentAttributeSetting o2) {
                    Model model = component.getInputModel();
                    ModelAttribute attribute1 = model.getAttributeById(o1.getAttributeId());
                    ModelEntity entity1 = model.getEntityById(attribute1.getEntityId());

                    ModelAttribute attribute2 = model.getAttributeById(o2.getAttributeId());
                    ModelEntity entity2 = model.getEntityById(attribute2.getEntityId());

                    int compare = entity1.getName().compareTo(entity2.getName());
                    if (compare == 0) {
                        compare = attribute1.getName().compareTo(attribute2.getName());
                    }
                    return compare;
                }
            });
        }

        updateTable(null);

    }

    protected void updateTable(String filter) {
        boolean showPopulatedEntities = filterPopField.getValue().equals(SHOW_POPULATED_ENTITIES);
        boolean showPopulatedAttributes = filterPopField.getValue().equals(SHOW_POPULATED_ATTRIBUTES);
    
        if (componentAttributes != null) {
            Model model = component.getInputModel();
            Collection<String> entityNames = new ArrayList<>();

            filter = filter != null ? filter.toLowerCase() : null;
            if (model != null) {
            	table.removeAllItems();
            	// loop through the attributes with transforms to get a list of entities
	            for (ComponentAttributeSetting componentAttribute : componentAttributes) {
	                ModelAttribute attribute = model.getAttributeById(componentAttribute.getAttributeId());
	                ModelEntity entity = model.getEntityById(attribute.getEntityId());
	                if (isNotBlank(componentAttribute.getValue())
	                		&& !entityNames.contains(entity.getName())) {
	                	entityNames.add(entity.getName());
	                }
	            }

            	for (ComponentAttributeSetting componentAttribute : componentAttributes) {
	                ModelAttribute attribute = model.getAttributeById(componentAttribute.getAttributeId());
	                ModelEntity entity = model.getEntityById(attribute.getEntityId());
	                
	                boolean populated = (showPopulatedEntities && entityNames.contains(entity.getName())) ||
	                (showPopulatedAttributes && isNotBlank(componentAttribute.getValue())) || 
	                (!showPopulatedAttributes && !showPopulatedEntities); 
	                if (isBlank(filter) || entity.getName().toLowerCase().contains(filter)
	                        || attribute.getName().toLowerCase().contains(filter)) {
	                	if (populated) {
	                		table.addItem(componentAttribute);
	                	}
	                }
	            }
            }
        }
    }

    class EditFieldFactory implements TableFieldFactory {
        public Field<?> createField(final Container dataContainer, final Object itemId,
                final Object propertyId, com.vaadin.ui.Component uiContext) {
            final ComponentAttributeSetting setting = (ComponentAttributeSetting) itemId;
            Field<?> field = null;

            if (propertyId.equals("value")) {
                final ComboBox combo = new ComboBox();
                combo.setWidth(100, Unit.PERCENTAGE);
                String[] functions = ModelAttributeScriptHelper.getSignatures();
                for (String function : functions) {
                    combo.addItem(function);
                }
                combo.setPageLength(functions.length > 20 ? 20 : functions.length);
                if (setting.getValue() != null && !combo.getItemIds().contains(setting.getValue())) {
                    combo.addItem(setting.getValue());
                }
                combo.setImmediate(true);
                combo.setNewItemsAllowed(true);
                combo.addValueChangeListener(new ValueChangeListener() {
                    public void valueChange(ValueChangeEvent event) {
                        setting.setValue((String) combo.getValue());
                        context.getConfigurationService().save(setting);
                    }
                });
                field = combo;
            }
            return field;
        }
    }

}
