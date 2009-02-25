<%= dojoInclude(["dojo.event.*",
                 "dojo.collections.Store",
                 "dojo.widget.ContentPane",
                 "dojo.widget.TabContainer",
                 "dojo.widget.FilteringTable"]) %>

<link rel=stylesheet href="/hqu/public/hqu.css" type="text/css">

<style type="text/css">
#CompatibleAlertDefs {
    width: 100%;
    table-layout: fixed;
    min-width:660px;
    *margin-left:1px
}

#CompatibleAlertDefs td {
    overflow: hidden
}

.tundra .dijitTooltipContainer
{
border:1px solid gray;
max-width:450px;
}

.tundra .dijitTooltipAbove .dijitTooltipConnector { background:none; }
.tundra .dijitTooltipLeft .dijitTooltipConnector { background:none; }
.tundra .dijitTooltipBelow .dijitTooltipConnector { background:none; }
.tundra .dijitTooltipRight .dijitTooltipConnector { background:none; }
.dijitButtonNode { color: black; }

</style>


    
<style>
	@import "/js/dojo/1.1/dojox/grid/_grid/Grid.css";
	@import "/js/dojo/1.1/dojox/grid/_grid/tundraGrid.css";
	body {
		font-family: Tahoma, Arial, Helvetica, sans-serif;
		font-size: 11px;
	}
	.dojoxGrid-row-editing td {
		background-color: #F4FFF4;
	}
	.dojoxGrid input, .dojoxGrid select, .dojoxGrid textarea {
		margin: 0;
		padding: 0;
		border-style: none;
		width: 100%;
		font-size: 100%;
		font-family: inherit;
	}
	.dojoxGrid input {
	}
	.dojoxGrid select {
	}
	.dojoxGrid textarea {
	}
	
	.tundra .dojoxGrid-cell {
		border-bottom: 1px solid #DDDDDD;
		border-right: none;
		border-top: none;
		border-left: none;
	}
	.tundra .dojoxGrid-header .dojoxGrid-cell {
		background:transparent url(/hqu/public/images/ft-head.gif) no-repeat scroll left top;
		border-width:0 1px 1px 0;
		border-color:transparent #7BAFFF transparent transparent;
		border-style:solid;
		font-weight:bold;
	}
	.tundra .dojoxGrid-sort-up {
		background:transparent url(/hqu/public/images/ft-headup.gif) no-repeat scroll right top;
		margin:-3px;
		padding-bottom:3px;
		padding-left:3px;
		padding-top:3px;
	}
	.tundra .dojoxGrid-sort-down {
		background:transparent url(/hqu/public/images/ft-headdown.gif) no-repeat scroll right top;
		margin:-3px;
		padding-bottom:3px;
		padding-left:3px;
		padding-top:3px;
	}
	
	
	#grid {
		width: 1000px;
		height: 350px;
		border: 1px solid silver;
	}

	#alertsgrid {
		width: 998px;
		height: 350px !important;
		border-color:#7BAFFF silver silver;
		border-style:solid;
		border-width:1px;
	}
	
	.filterBox select {
		width:auto;
	}
	
	div.reportheader {
		font-size: 110%;
		font-weight: bold;
	}
	div.reportsection {
		text-indent: 10px;
	}
	div.reportstatus {
		margin-bottom: 10px;
	}
	
	.titlebox {
		margin: 30px 5px 15px 10px;
		border: #3c5a86 1px dashed;
		padding:5px;
		font-size: 12px;
		font-weight: normal;
		color: #000000;
		background-color: #d1e0ef;
		float: left; 
		width: 70%;
	}

	.titlebox H1 {
		margin : 0px 0px -12px 5px;
		position: relative;
		top : -12px;
		border: #3c5a86 1px solid;
		padding-top : 3px;
		padding-bottom: 3px;
		padding-left : 5px;
		padding-right : 5px;
		font-size : 18px;
		font-weight: bold;
		color : #000000;
		display: inline;
		background-color: #99bbdd;
	}
	
</style>


<script type="text/javascript">
	dojo11.require("dojox.grid.Grid");
	dojo11.require("dijit.Tooltip");
	dojo11.require("dojo.parser");
	dojo11.require("dijit.Dialog");
	dojo11.require("dijit.form.Button");
</script>


<script type="text/javascript">

	// disable editing for some colums
	canEditTest = function(inCell, inRowIndex) {
		return (inCell.index < 2);
	};
	
	//
	//
	//
	function sendCode() {
		var content_object = {};
		var rows = model.data.toArray();
		for(var i = 0; i < rows.length; i++) {
    		var cols = rows[i].toArray();
    		for(var j = 0; j < cols.length; j++) {
        		content_object[i + ',' + j] = cols[j];
    		}
		}
		dojo.byId('timeStatus').innerHTML = '... executing';
		dojo.io.bind({
			url: 'hqu/adminhelper/adminhelper/execute.hqu',
			method: "post",
			mimetype: "text/json-comment-filtered",
			content: content_object,
			load: function(type, data, evt) {
				dojo.byId('timeStatus').innerHTML = data.timeStatus;
				dojo.byId('statusreportcontent').innerHTML = data.reportStatus;
    	},
			Error: function(type, data, evt) {
				alert('error! ' + data);
			}
		});
	}
	
	//
	//
	//
	function executeSynchronize() {
		var origAlertId = dojo11.byId('AlertSyncOrigAlertSelect').value;
		var content_object = {};
		var rows = dijit11.byId('alertsgrid').model.data.toArray();
		var rownewid = 0;
		for(var i = 0; i < rows.length; i++) {
    		if (rows[i][3]) {
	    		content_object[rownewid + ',0'] = origAlertId;
	    		content_object[rownewid + ',1'] = rows[i][4];
	    		rownewid++;
    		}
		}
		
		<% if (isEE) { %>
		cboxnames = ['syncName','syncDesc','syncPriority','syncEsc',
					 'syncConditions','syncWillRecover','syncNotifyFiltered',
					 'syncNotifyUsers','syncNotifyEmail','syncScript',
					 'syncSnmp','syncOpenNMS', 'syncNotifyRoles'
					];
		<% } else { %>
		cboxnames = ['syncName','syncDesc','syncPriority','syncEsc',
					 'syncConditions','syncNotifyUsers','syncNotifyEmail',
					 'syncWillRecover'
					];
		<% } %>
		
		for(var i = 0; i < cboxnames.length; i++) {
			content_object[cboxnames[i]] = dojo.byId(cboxnames[i]).checked;
		}

		dojo.byId('syncTimeStatus').innerHTML = '... executing';
		dojo.io.bind({
			url: 'hqu/adminhelper/adminhelper/executeSync.hqu',
			method: "post",
			mimetype: "text/json-comment-filtered",
			content: content_object,
			load: function(type, data, evt) {
				dojo.byId('syncTimeStatus').innerHTML = data.timeStatus;
				dojo.byId('syncstatusreportcontent').innerHTML = data.reportStatus;
    	},
			Error: function(type, data, evt) {
				alert('error! ' + data);
			}
		});
	}

	//
	//
	//
	function deleteGridSelectedRows() {
		var r = dijit11.byId('grid');
		r.removeSelectedRows();
	}

	//
	//
	//
	function deleteGridAllRows() {
		var r = dijit11.byId('grid');
		var c = r.model.count;
		var s = [];
		r.edit.apply();
		for(var i = 0; i < c; i++) {
			s.push(i);
		}
		if (s.length) {
			r.model.remove(s);
			r.selection.clear();
		}
	}

	//
	//
	//
	function getAlertDefData(s, rid, rtype) {
		var alertDefsSelect = dojo.byId('alertDefsSelect');
		var id = alertDefsSelect.options[alertDefsSelect.selectedIndex].value;
		dojo.io.bind({
			url: 'hqu/adminhelper/adminhelper/getAlertDefData.hqu',
			method: "post",
			mimetype: "text/json-comment-filtered",
			content: {
				id: id,
				rid: rid,
				rtype: rtype,
				name: s
			},
			load: function(type, data, evt) {
				var row = new Array();
				row.push(data.alertdefname);
				row.push(data.alertdefdesc);
				row.push(data.alertdefid);
				row.push(data.resourcetype + ':' + data.resourceid);
				row.push(data.resourcename);
				row.push(data.caname);
				row.push(data.carid);
				row.push(data.caaction);
				dijit11.byId('grid').addRow(row);
			},
			Error: function(type, data, evt) {
				alert('error! ' + data);
			}
		});
	}
    
  //
  //
  //    
	function fillResources(select) {
		dojo11.byId('resourceSelect').innerHTML = "";
		dojo11.byId('actionSelect').innerHTML = "";
		if (select.value != "-1") {
			dojo11.xhrGet( {
				preventCache: true,
				url:	'/alerts/EditControlActionResources.do' + 
							'?aetid=' + select.options[select.selectedIndex].value + 
							'&eid=3:10556',
				handleAs: "text",
				timeout: 5000,
				load: function(data, ioArgs) {
					dojo11.byId('resourceSelect').innerHTML = data;
					fillActions(dojo11.byId('resource'));
				},
				Error: function(data){
					console.debug("An error occurred fetching resources:");
					console.debug(data);
				}
			});
		}
	}

	//
	//
	//
	function fillActions(select) {
		dojo11.byId('actionSelect').innerHTML = "";
		if (select.value != "-1") {
			dojo11.xhrGet( {
				preventCache: true,
				url:	'/alerts/EditControlActionTypes.do' + 
							'?eid=' + select.options[select.selectedIndex].value +
							'&action=',
				handleAs: "text",
				timeout: 5000,
				load: function(data, ioArgs) {
					dojo11.byId('actionSelect').innerHTML = data;
				},
				Error: function(data){
					console.debug("An error occurred fetching resource actions:");
					console.debug(data);
				}
			});
		}
	}
  
	
	
	//
	//
	//	
	caOK = function(rowi, res, cont) {
		var ri = res.options.selectedIndex;
		var rn = res.options[ri].text;
		var rv = res.options[ri].value;
		
		var ci = cont.options.selectedIndex;
		var cn = cont.options[ci].text;
		var cv = cont.options[ci].value;
		
		model.data[parseInt(rowi)][5] = (cn == 'none' ? cn : rn + ' :: ' + cn);
		model.data[parseInt(rowi)][6] = rv;
		model.data[parseInt(rowi)][7] = cv;
		
		var r = dijit11.byId('grid');
		r.update();		
	}
	

      
	alertdata = [];
	model = new dojox11.grid.data.Table(null, alertdata);
	model.observer(this);
	
	alertgridLayout = [{
		type: 'dojox11.GridRowView', width: '0px'
	},{
		defaultCell: { width: 8, editor: dojox11.grid.editors.Input, styles: 'text-align: right;'  },
		rows: [[ 
			{ name: 'Name', field: 0, styles: '', width: '35%' },
			{ name: 'Description', field: 1, styles: '', width: '35%' },
			{ name: 'From', field: 2, styles: '', width: '7%' },
			{ name: 'To', field: 3, styles: '', width: '7%' },
			{ name: 'Control Action', field: 5, styles: '', width: '16%' }
		]]
	}];

	alertsyncdata = [];
	syncmodel = new dojox11.grid.data.Table(null, alertsyncdata);
	syncmodel.observer(this);
	
	alertsyncgridLayout = [{
		type: 'dojox11.GridRowView', width: '0px'
	},{
		defaultCell: { width: 8, editor: dojox11.grid.editors.Input, styles: 'text-align: right;'  },
		rows: [[ 
			{ name: 'From', field: 0, styles: '', width: '50%' },
			{ name: 'To', field: 1, styles: '', width: '50%' },
		]]
	}];

	
	//
	//
	//
	function AlertSyncGrid_UpdateComponentStatuses() {
		var allowOnlyCompAlerts = dojo.byId('allowOnlyCompAlerts').checked;
		dojo.byId('AlertSyncResourceTypeSelect').disabled = allowOnlyCompAlerts;
		
		if(allowOnlyCompAlerts) {
			dojo.byId('syncConditions').disabled = false;
		} else {
			dojo.byId('syncConditions').checked = false;
			dojo.byId('syncConditions').disabled = true;
		}
	
	}
		
	
	//
	//
	//
	function AlertSyncGrid_UpdateValues() {
		var origAlertId = dojo.byId('AlertSyncOrigAlertSelect').value;
		var resourceId = dojo.byId('AlertSyncResourceTypeSelect').value;
		var allowOnlyCompAlerts = dojo.byId('allowOnlyCompAlerts').checked;
		
		dojo.byId('loader').style.visibility = 'visible';
		
		
		dojo11.xhrGet({
			preventCache: true,
			url: '/hqu/adminhelper/adminhelper/getJsonCompatibleAlertSyncDefs.hqu?' +
				 'origAlertId=' + origAlertId +
				 '&resourceId=' + resourceId +
				 '&allowOnlyCompAlerts=' + allowOnlyCompAlerts,
			handleAs: "json-comment-filtered",
			timeout: 5000,
			load: function(response, ioArgs) {
		
				//var asstore = new dojo11.data.ItemFileReadStore({data:response});
				//var asmodel = new dojox11.grid.data.DojoData(null,asstore,{query: { name : '*' }});
				var asdata = new Array();
				for (var i = 0; i < response.items.length; i++) {
					var x = response.items[i];
					asdata.push(new Array(x['name'],x['desc'],x['rname'],false,x['id']));
				}
				asmodel = new dojox11.grid.data.Table(null, asdata);
				
				var modelObservers = {
					modelChange:function(){
						dojo.byId("rowCount").innerHTML = 'Row count: ' + 
						dijit11.byId('alertsgrid').model.count; 
   					}
				}
				
				asmodel.observer(modelObservers);			
				
				
				
				var g = dijit11.byId('alertsgrid');
				g.setModel(asmodel);
				//g.refresh();
				g.update();
				dojo.byId('loader').style.visibility = 'hidden';		
    		},
			Error: function(response, ioArgs) {
				alert('error! ' + data);
			}
		});
		
		AlertSyncGrid_UpdateComponentStatuses();
		
	}
	
	
		
</script>



<!-- TABS -->
<div dojoType="TabContainer" id="mainTabContainer" 
	style="width: 100%; height:1050px; position: relative; z-index: 1;">

	<!-- ALERTDEF CLONING TAB -->
	<div dojoType="ContentPane" label="Alert Cloning">
	
		<!-- FILTER PANE -->
		<div style="margin-top:10px;margin-left:10px;margin-bottom:5px;padding-right:10px;">
			<div style="float:left;width:1000px;">
				<div class="filters">
					<div class="BlockTitle">${l.alertdef}</div>
					<div class="filterBox">
					<%
					out.write(selectList(allAlertDefs, 
     	                       [id:'alertDefsSelect',
                               onchange:'CompatibleAlertDefs_refreshTable(); ']))
					%>
					</div>
				</div>
			</div>
		</div>
		<!-- TABLE PANE -->
		<div style="width: 1000px; margin-left:10px; margin-right:10px;margin-top:10px;float:left;display:block;height: 445px;overflow-x: hidden; overflow-y: auto;" id="alertsCont">
			<div id="alertsTable">
				<%
				out.write(dojoTable(id:'CompatibleAlertDefs', title:l.compAlertDefs,
                                refresh:6000, url:urlFor(action:'getCompatibleAlertDefs'),
                                schema:compatibleAlertDefsSchema, numRows:12))
                %>
			</div>
		</div>
		<div style="float:left;width:1002px;margin-left:10px;display:inline;overflow-x: hidden; overflow-y: auto;" id="alertDefGrid">
			<div class="pageCont">
    			<div class="tableTitleWrapper">
        			<div style="display: inline; width: 75px;">${l.gridtitle}</div>    
    			</div>
			</div>
			<div id="gridContainer">
			<script type="text/javascript">
	dojo.addOnLoad(function() {
		window["grid"] = dijit11.byId("grid");
		
		var tgrid = new dojox11.Grid({
					"id": "grid",
					"model": model,
					"structure": alertgridLayout,
					"canEdit": canEditTest
				});
				dojo.byId("gridContainer").appendChild(tgrid.domNode);
				tgrid.render();
		
				
		var showTooltip = function(e) {
					var msg = "";
					var show = false;
					if (e.rowIndex < 0) {
						show = true;
						if (e.cellIndex == 0)
							msg = "Name of New Alert Definition";
						else if (e.cellIndex == 1)
							msg = "Description of New Alert Definition";
						else if (e.cellIndex == 2)
							msg = "Original Alert Definition Id";
						else if (e.cellIndex == 3)
							msg = "Resource where to Clone";
					else if (e.cellIndex == 4)
							msg = "New Control Action to Use";
					} else if (e.cellIndex == 3) {
						show = true;
						msg = model.data[e.rowIndex][4];
					}
					if (show)
						dijit11.showTooltip(msg, e.cellNode);
			},
			hideTooltip = function(e) {
				dijit11.hideTooltip(e.cellNode);
				dijit11._masterTT._onDeck=null;
			},
			showCaDialog = function(e) {
				if (e.cellIndex == 4) {
					var test = model.data[e.rowIndex][5];
					if (test != 'unavailable') {
						dojo11.byId('rowid').value = e.rowIndex;
						dijit11.byId('dialog1').show();			
					}
				}
			}
		
		// cell tooltip
		dojo11.connect(tgrid, "onCellMouseOver", showTooltip);
		dojo11.connect(tgrid, "onCellMouseOut", hideTooltip);
		// header cell tooltip
		dojo11.connect(tgrid, "onHeaderCellMouseOver", showTooltip);
		dojo11.connect(tgrid, "onHeaderCellMouseOut", hideTooltip);
		// dialog
		dojo11.connect(tgrid, "onCellClick", showCaDialog);
	});
			</script>
			</div>
			
			<br/>
			<div>
				<a class="buttonGreen" onclick="deleteGridSelectedRows()" href="javascript:void(0)"><span>${l.buttonremoveselected}</span></a>
			</div>
			<div>
				<a class="buttonGreen" onclick="deleteGridAllRows()" href="javascript:void(0)"><span>${l.buttonremoveall}</span></a>
			</div>
			<div>
				<a class="buttonGreen" onclick="sendCode()" href="javascript:void(0)"><span>Execute</span></a>
			</div>
			
			<div id='timeStatus' style="margin-top: 40px; margin-bottom: 5px;">Status:  Idle</div>
			<input id="rowid" name="rowid" dojo11Type="dijit11.form.TextBox" type="hidden" value="hidden">
			<div dojo11Type="dijit11.form.DropDownButton">
				<span>Show Report</span>
				<div dojo11Type="dijit11.TooltipDialog" id="statusreportdialog">
					<div id="statusreportcontent">
						<div class="reportstatus">No cloning executed.</div>
					</div>
				</div>
			</div>
		</div>
		<div style="clear:both;height:1px;"></div>
	</div>

	<!-- ALERTDEF SYNC TAB -->
	<div dojoType="ContentPane" label="Alert Synchronization" onShow="AlertSyncGrid_UpdateValues()">
		<div style="margin-top:10px;margin-left:10px;margin-bottom:5px;padding-right:10px;">
			<div style="float:left;width:1000px;margin-bottom:10px;">
				<div class="filters">
					<div class="BlockTitle">${l.alertdef}</div>
					<div class="filterBox">
					<div style="display: inline; font-weight: bold; float: left; width: 50px;">Alert:</div>
					<%
					out.write(selectList(allAlertDefs, 
     	                       [id:'AlertSyncOrigAlertSelect',
                               onchange:'AlertSyncGrid_UpdateValues(); ']))
					%>
					</div>
					<div class="filterBox">
					<div style="display: inline; font-weight: bold; float: left; width: 50px;">Type:</div>
					<select id="AlertSyncResourceTypeSelect" disabled="disabled" onChange="AlertSyncGrid_UpdateValues();">
					<option value="-1" selected="selected" disabled="disabled">Select Type...</option>
					<option value="-1" disabled="disabled" class="boldText">Platform Type</option>
					<%
					controllableTypes.getPlatforms().each{
						out.write('<option value="1:' + it.id + '">' + it.name + '</option>')
					}
					%>					
					<option value="-1" disabled="disabled" class="boldText">Server Type</option>
					<%
					controllableTypes.getServers().each{
						out.write('<option value="2:' + it.id + '">' + it.name + '</option>')
					}
					%>
					<option value="-1" disabled="disabled" class="boldText">Service Type</option>
					<%
					controllableTypes.getServices().each{
						out.write('<option value="3:' + it.id + '">' + it.name + '</option>')
					}
					%>
					</select>
					</div>
					<div class="filterbox">
						<input id="allowOnlyCompAlerts" type="checkbox" name="allowOnlyCompAlerts" checked="true" onChange="AlertSyncGrid_UpdateValues();"/>
						<label for="allowOnlyCompAlerts">Handle only compatible alerts. (Enables condition set checkbox)</label>
					</div>
					
				</div>
			</div>
		</div>
		<!-- COMP ALERT TABLE PANE -->
		<!-- JSON GRID START -->
		<div style="float:left;width:1000px;margin-bottom:10px;margin-left:10px;margin-right:10px;display:inline;overflow-x: hidden; overflow-y: auto;">
			<div class="pageCont">
    			<div class="tableTitleWrapper">
        			<div style="display: inline; width: 75px;">Compatible Alerts</div>    
    			</div>
				<div style="float: right; padding-right: 15px;">
					<div id="loader" style="display: inline; padding-right: 5px; visibility: hidden;"><img src="/hqu/public/images/ajax-loader-blue.gif"/></div>
					<div id="refresh" style="display: inline; padding-right: 5px;">
						<img src="/hqu/public/images/arrow_refresh.gif" onClick="AlertSyncGrid_UpdateValues();"/>
					</div>
					<div style="display: inline;" id="rowCount">Items: 123</div>
				</div>
			</div>
			<div class="tundra" id="alertsgridContainer">
			<script type="text/javascript">
				dojo.addOnLoad(function() {
			
				var layout = [
					{ type: 'dojox11.GridRowView', width: '0px' },
					{ rows: [[
						{name:"Name", editor: dojox11.grid.editors.Input, field: 0, width: '33%'},
						{name:"Desc", field: 1, width: '30%'},
						{name:"Resource", field: 2, width: '30%'},
						{name:'Select', field: 3, width: '7%', styles: 'text-align: center;', editor: dojox11.grid.editors.Bool },
						]]}
				];
			
				var talertsgrid = new dojox11.Grid({
						id: "alertsgrid",
						structure: layout
					});
					dojo.byId("alertsgridContainer").appendChild(talertsgrid.domNode);
					
					talertsgrid.startup();
					talertsgrid.render();
			
				});
			</script>
			</div>
		</div>
		<!-- JSON GRID END -->


		
		<div style="float:left;width:1000px;margin-left:10px;margin-right:10px;display:inline;overflow-x: hidden; overflow-y: auto;" id="syncSelectors">
			
			<div style="float: left; width: 48%; margin-bottom: 15px;">
				<div class="filters">
					<div class="BlockTitle">General</div>
					<div class="filterBox">	
					<div><input id="syncName" type="checkbox" name="syncName" value="false"/>
					<label for="syncName">${l.lSyncName}</label></div>
					<div><input id="syncDesc" type="checkbox" name="syncDesc" value="false"/>
					<label for="syncDesc">${l.lSyncDesc}</label></div>
					<div><input id="syncPriority" type="checkbox" name="syncPriority" value="false"/>
					<label for="syncPriority">${l.lSyncPriority}</label></div>
					</div>
				</div>
			</div>
			

			<div style="float: right; width: 48%; margin-bottom: 15px;">
				<div class="filters">
					<div class="BlockTitle">Conditions</div>
					<div class="filterBox">
					<div><input id="syncConditions" type="checkbox" name="syncConditions" value="false"/>
					<label for="syncConditions">${l.lSyncConditions}</label></div>
					<div><input id="syncWillRecover" type="checkbox" name="syncWillRecover" value="false"/>
					<label for="syncWillRecover">${l.lSyncWillRecover}</label></div>
					<% if (isEE) { %>
					<div><input id="syncNotifyFiltered" type="checkbox" name="syncNotifyFiltered" value="false"/>
					<label for="syncNotifyFiltered">${l.lSyncNotifyFiltered}</label></div>
					<% } %>
					</div>
				</div>
			</div>
			
			<div style="float: left; width: 100%;">
				<div class="filters">
					<div class="BlockTitle">Actions</div>
					<div class="filterBox">
					<div><input id="syncEsc" type="checkbox" name="syncEsc" value="false"/>
					<label for="syncEsc">${l.lSyncEsc}</label></div>
					<% if (isEE) { %>
					<div><input id="syncNotifyRoles" type="checkbox" name="syncNotifyRoles" value="false"/>
					<label for="syncNotifyRoles">${l.lSyncNotifyRoles}</label></div>
					<% } %>
					<div><input id="syncNotifyUsers" type="checkbox" name="syncNotifyUsers" value="false"/>
					<label for="syncNotifyUsers">${l.lSyncNotifyUsers}</label></div>
					<div><input id="syncNotifyEmail" type="checkbox" name="syncNotifyEmail" value="false"/>
					<label for="syncNotifyEmail">${l.lSyncNotifyEmail}</label></div>
					<% if (isEE) { %>
					<div><input id="syncScript" type="checkbox" name="syncScript" value="false"/>
					<label for="syncScript">${l.lSyncScript}</label></div>
					<div><input id="syncSnmp" type="checkbox" name="syncSnmp" value="false"/>
					<label for="syncSnmp">${l.lSyncSnmp}</label></div>
					<div><input id="syncOpenNMS" type="checkbox" name="syncOpenNMS" value="false"/>
					<label for="syncOpenNMS">${l.lSyncOpenNMS}</label></div>
					<% } %>
					</div>
				</div>
			</div>


		</div>
		<div style="float:left;width:1000px;margin-left:10px;margin-right:10px;margin-top:10px;display:inline;overflow-x: hidden; overflow-y: auto;" id="synccontrols">
			<div>
				<a class="buttonGreen" onclick="executeSynchronize()" href="javascript:void(0)"><span>Synchronize</span></a>
			</div>
			<div id='syncTimeStatus' style="margin-top: 40px; margin-bottom: 5px;">Status:  Idle</div>
			
			<div dojo11Type="dijit11.form.DropDownButton">
				<span>Show Report</span>
				<div dojo11Type="dijit11.TooltipDialog" id="syncstatusreportdialog">
					<div id="syncstatusreportcontent">
						<div class="reportstatus">No synchronizing executed.</div>
					</div>
				</div>
			</div>
		</div>
		
		
		<div style="clear:both;height:1px;"></div>
	</div>

	
	
	
</div>

<div dojo11Type="dijit11.Dialog" jsId="dialog1" id="dialog1" title="${l.cadialoglheader}"
	 execute="caOK(dojo11.byId('rowid').value,dojo11.byId('resource'),dojo11.byId('controlAction'));"
	 href="/hqu/adminhelper/adminhelper/addcadialog.hqu">
	 </div>		
	



<script type="text/javascript">
    function getAlertDefsUrlMap(id) {
        var res = {};
        var alertDefsSelect = dojo.byId('alertDefsSelect');
        res['alertdef'] = alertDefsSelect.options[alertDefsSelect.selectedIndex].value;
        
        return res;
    }
    
    CompatibleAlertDefs_addUrlXtraCallback(getAlertDefsUrlMap);
</script>
