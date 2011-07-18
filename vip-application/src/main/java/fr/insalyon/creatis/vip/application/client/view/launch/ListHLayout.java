/* Copyright CNRS-CREATIS
 *
 * Rafael Silva
 * rafael.silva@creatis.insa-lyon.fr
 * http://www.rafaelsilva.com
 *
 * This software is a grid-enabled data-driven workflow manager and editor.
 *
 * This software is governed by the CeCILL  license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and,  more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL license and that you accept its terms.
 */
package fr.insalyon.creatis.vip.application.client.view.launch;

import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;
import fr.insalyon.creatis.vip.common.client.view.FieldUtil;
import fr.insalyon.creatis.vip.datamanager.client.view.selection.PathSelectionWindow;

/**
 *
 * @author Rafael Silva
 */
public class ListHLayout extends HLayout {
    
    private ListHLayout instance;
    private TextItem listItem;
    private DynamicForm listItemForm;
    private IButton controlButton;
    private IButton browseButton;
    
    public ListHLayout(final VLayout parent, boolean master) {
        this(parent, master, "");
    }
    
    public ListHLayout(final VLayout parent, boolean master, String value) {
        
        this.instance = this;
        this.setMembersMargin(3);
                
        if (master) {
            controlButton = new IButton("+");
            controlButton.addClickHandler(new ClickHandler() {

                public void onClick(ClickEvent event) {
                    parent.addMember(new ListHLayout(parent, false));
                }
            });
            
        } else {
            controlButton = new IButton("-");
            controlButton.addClickHandler(new ClickHandler() {

                public void onClick(ClickEvent event) {
                    parent.removeMember(instance);
                }
            });
        }
        controlButton.setWidth(30);
        this.addMember(controlButton);
        
        listItem = FieldUtil.getTextItem(400, false, "", null);
        listItem.setValue(value);
        listItemForm = FieldUtil.getForm(listItem);
        this.addMember(listItemForm);
        
        browseButton = new IButton("Browse");
        browseButton.setWidth(60);
        browseButton.addClickHandler(new ClickHandler() {

            public void onClick(ClickEvent event) {
                new PathSelectionWindow(listItem).show();
            }
        });
        this.addMember(browseButton);
    }
    
    public boolean validate() {
        return listItem.validate();
    }
    
    public String getValue() {
        return listItem.getValueAsString();
    }
}