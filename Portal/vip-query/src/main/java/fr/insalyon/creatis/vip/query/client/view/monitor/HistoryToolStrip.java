/*
 * Copyright and authors: see LICENSE.txt in base repository.
 *
 * This software is a web portal for pipeline execution on distributed systems.
 *
 * This software is governed by the CeCILL-B license under French law and
 * abiding by the rules of distribution of free software.  You can  use, 
 * modify and/ or redistribute the software under the terms of the CeCILL-B
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
 * knowledge of the CeCILL-B license and that you accept its terms.
 */
package fr.insalyon.creatis.vip.query.client.view.monitor;

//import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.util.BooleanCallback;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.toolbar.ToolStrip;
import fr.insalyon.creatis.vip.core.client.CoreModule;
import fr.insalyon.creatis.vip.core.client.view.CoreConstants;
import fr.insalyon.creatis.vip.core.client.view.ModalWindow;
import fr.insalyon.creatis.vip.core.client.view.layout.Layout;
import fr.insalyon.creatis.vip.core.client.view.util.WidgetUtil;
import fr.insalyon.creatis.vip.query.client.rpc.QueryService;
import fr.insalyon.creatis.vip.query.client.view.QueryConstants;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Nouha Boujelben
 */
public class HistoryToolStrip extends ToolStrip {

    private ModalWindow modal;

    public HistoryToolStrip(ModalWindow modal) {

        this.modal = modal;
        this.setWidth100();

        // Refresh Button
        this.addButton(WidgetUtil.getToolStripButton("Refresh",
                CoreConstants.ICON_REFRESH, null, new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                getQueryHistoryTab().loadData();
            }
        }));

        // Search Button
        this.addButton(WidgetUtil.getToolStripButton("Search Query",
                QueryConstants.ICON_SEARCH, null, new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                getQueryHistoryTab().setFilter();
            }
        }));


        //purge query
        this.addButton(WidgetUtil.getToolStripButton("Purge Query",
                CoreConstants.ICON_CLEAR, null, new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {


                SC.ask("Do you really want to purge the selected  Query Execution?", new BooleanCallback() {
                    @Override
                    public void execute(Boolean value) {
                        if (value) {
                            purgeQuery();
                        }
                    }
                });

            }
        }));

    }

    private void purgeQuery() {
        ListGridRecord[] records = getQueryHistoryTab().getGridSelection();

        for (ListGridRecord record : records) {

            Long id = record.getAttributeAsLong("queryExecutionID");

            final AsyncCallback<Void> callback = new AsyncCallback<Void>() {
                @Override
                public void onFailure(Throwable caught) {

                    Layout.getInstance().setWarningMessage("Unable to purge simulations:<br />" + caught.getMessage());
                }

                @Override
                public void onSuccess(Void result) {
                    getQueryHistoryTab().loadData();
                }
            };
            QueryService.Util.getInstance().removeQueryExecution(id, callback);
        }
    }

    private QueryHistoryTab getQueryHistoryTab() {
        return (QueryHistoryTab) Layout.getInstance().getTab(QueryConstants.TAB_QUERYHISTORY);
    }
}
