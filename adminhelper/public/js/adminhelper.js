
function AlertDeleteGrid_UpdateValues() {
		
	dojo.byId('deleteloader').style.visibility = 'visible';
	var adg_1 = dojo.byId('Adg_DisabledAlerts').checked;
	var adg_2 = dojo.byId('Adg_RecoveryAlerts').checked;
	
	var adg_3 = dojo.byId('Adg_ResourceSelect').value;
	var adg_4 = dojo.byId('Adg_GroupSelect').value;
	var adg_5 = dojo.byId('Adg_FilterText').value;
		
	var adg_6 = "type";
	if(dojo.byId('Adg_FilterGroup').checked)
		adg_6 = "group"
	else if(dojo.byId('Adg_FilterAll').checked)
		adg_6 = "all"
		
	dojo11.xhrGet({
		preventCache: true,
		url: '/hqu/adminhelper/adminhelper/getJsonAlertDefs.hqu?' +
			 'Adg_DisabledAlerts=' + adg_1 +
			 '&Adg_RecoveryAlerts=' + adg_2 +
			 '&Adg_ResourceSelect=' + adg_3 +
			 '&Adg_GroupSelect=' + adg_4 +
			 '&Adg_FilterText=' + adg_5 +
			 '&Adg_FilterType=' + adg_6,
		handleAs: "json-comment-filtered",
		timeout: 5000,
		load: function(response, ioArgs) {
		
			var asdata = new Array();
			for (var i = 0; i < response.items.length; i++) {
				var x = response.items[i];
				asdata.push(new Array(x['name'],x['desc'],x['rname'],false,x['id']));
			}
			asmodel = new dojox11.grid.data.Table(null, asdata);
			
			var modelObservers = {
				modelChange:function(){
					dojo.byId("adg_rowCount").innerHTML = 'Row count: ' + 
					dijit11.byId('alertsdeletegrid').model.count; 
  					}
			}
				
			asmodel.observer(modelObservers);			
				
				
				
			var g = dijit11.byId('alertsdeletegrid');
			g.setModel(asmodel);
			g.update();
			dojo.byId('deleteloader').style.visibility = 'hidden';		
   		},
		Error: function(response, ioArgs) {
			alert('error! ' + data);
		}
	});
	
	
}

function AlertDelete_RequestDelete() {
	var jsonData = dijit11.byId('alertsdeletegrid').model.data.toJSON();
	
	var contentObject = {};
	contentObject['jsonData'] = jsonData;
	
	dojo11.xhrPost({
		url: '/hqu/adminhelper/adminhelper/executeDelete.hqu',
		handleAs: "json-comment-filtered",
		timeout: 10000,
		content: contentObject,
		load: function(response, ioArgs) {
			dojo.byId('deleteTimeStatus').innerHTML = response.timeStatus;
			dojo.byId('deletestatusreportcontent').innerHTML = response.reportStatus;
			AlertDeleteGrid_UpdateValues();
			return response;
		},
		error: function(response, ioArgs) {
			return response;
		}
	});
};

function AlertDelete_ToggleHighlighted() {
	var g = dijit11.byId('alertsdeletegrid');
	var data = g.model.data;
	var s = g.selection.getSelected();
	
	for(var i = 0; i < s.length; i++) {
		data[s[i]][3] = !data[s[i]][3];
	}
	g.update();
};
