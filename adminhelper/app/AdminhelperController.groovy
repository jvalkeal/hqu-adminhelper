import org.hyperic.hq.hqu.rendit.BaseController

import java.text.DateFormat
import org.hyperic.hq.common.YesOrNo

import org.hyperic.hq.hqu.rendit.html.DojoUtil
import org.hyperic.hq.events.server.session.AlertDefinitionManagerEJBImpl
import org.hyperic.hq.authz.server.session.ResourceManagerEJBImpl
import org.hyperic.hq.appdef.server.session.AppdefManagerEJBImpl
import org.hyperic.hq.bizapp.server.session.AppdefBossEJBImpl
import org.hyperic.hq.appdef.server.session.ServerManagerEJBImpl
import org.hyperic.hq.appdef.server.session.ServiceManagerEJBImpl
import org.hyperic.hq.appdef.server.session.PlatformManagerEJBImpl
import org.hyperic.hq.events.AlertSeverity
import org.hyperic.hq.authz.server.session.ResourceSortField
import org.hyperic.hq.events.server.session.AlertDefSortField
import org.hyperic.hibernate.PageInfo
import org.hyperic.hq.appdef.shared.AppdefEntityID
import org.hyperic.hq.appdef.shared.AppdefEntityConstants
import org.hyperic.hq.events.shared.AlertDefinitionValue
import org.hyperic.hq.events.shared.AlertConditionValue
import org.hyperic.hq.events.EventConstants
import org.hyperic.hq.authz.server.session.Resource

import org.hyperic.hq.auth.shared.SessionManager
import org.hyperic.hq.bizapp.server.session.ControlBossEJBImpl
import org.hyperic.hq.bizapp.shared.ControlBossUtil
import org.hyperic.hq.bizapp.shared.ControlBossLocal

import org.hyperic.util.pager.PageControl

import org.json.JSONArray
import org.json.JSONObject

/**
 * Main logic for this plugin resides in this class.
 * 
 * 
 */
class AdminhelperController 
	extends BaseController
{
	 private final DateFormat df = 
	        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
	
	 /** Alert Definition Manager*/
	 def aMan
       
	 /** Server Manager */
	 def serverMan

	 /** Service Manager */
	 def serviceMan

	 /** Platform Manager */
	 def platformMan

	/**
	 * Introducing which functions are used as json queries
	 * within plugin page components.
	 */
    def AdminhelperController() {
        setJSONMethods(['getCompatibleAlertDefs', 'execute', 'executeSync', 'getAlertDefData', 'getCompatibleAlertSyncDefs'])
		this.aMan = AlertDefinitionManagerEJBImpl.one
		this.serverMan = ServerManagerEJBImpl.one
		this.serviceMan = ServiceManagerEJBImpl.one
		this.platformMan = PlatformManagerEJBImpl.one
    }
 
    // Methods under ResourceManagerEJBImpl.findResourcesOfPrototype
    // doesn't support sorting. So this table can't be sorted by name.
    // Leaving it as sortable in case this feature is added.
    // Sorting doesn't work in recources page either.
    private final COMPATIBLE_RESOURCES_SCHEMA = [
        getData: {pageInfo, params ->
            def alertDefId = params.getOne('alertdef').toInteger()

       		def alertDef = aMan.getByIdAndCheck(user, alertDefId)       		
       		
            def proto = alertDef.resource.prototype

            resourceHelper.findResourcesOfType(proto, pageInfo)
            
        },
        defaultSort: ResourceSortField.NAME,
        defaultSortOrder: 0,
        columns: [
            [field:ResourceSortField.NAME, width:'92%',
             label:{it.name}],
             [field: [getValue: {''},
                      description:'addButton',
                      sortable:false], 
              width: '8%',
              label: {"""<a href="#" class="buttonGreen" onclick="getAlertDefData('${it.name}','${it.instanceId}','${it.resourceType.appdefType}');return false;"><span>Add</span></a>"""}] 
        ]
    ]
    
    private final DEF_TABLE_SCHEMA = [
	    getData: {pageInfo, params -> 
            alertHelper.findDefinitions(AlertSeverity.LOW, 
                                        true,
                                        true, pageInfo) 
            },
            defaultSort: AlertDefSortField.CTIME,
            defaultSortOrder: 0,  // descending
            columns: [
                [field:AlertDefSortField.NAME, width:'13%',
                 label:{linkTo(it.name, [resource:it]) }],
                [field:AlertDefSortField.CTIME, width:'13%',
                 label:{df.format(it.ctime)}],
                [field:AlertDefSortField.MTIME, width:'13%',
                 label:{df.format(it.mtime)}],
                [field:AlertDefSortField.ACTIVE, width:'5%',
                 label:{YesOrNo.valueFor(it.enabled).value.capitalize()}],
                [field:AlertDefSortField.LAST_FIRED, width:'13%',
                 label:{
                     if (it.lastFired)
                         return linkTo(df.format(it.lastFired),
                [resource:it, resourceContext:'listAlerts'])
                     else
                         return ''
                }],
                [field:AlertDefSortField.RESOURCE, width:'36%',
                 label:{linkTo(it.resource.name,
                [resource:it.resource])}],
                [field: [getValue: {''},
                         description:'addButton',
                         sortable:false], 
                 width: '8%',
                 label: {"""<a href="#" class="buttonGreen" onclick="moveToSyncGrid('${it.id}');return false;"><span>Add</span></a>"""}] 
                ]
            ]

    
    /**
     * Returning all alert definition which are not resource type
     * alerts, are active and is not recovery alert.
     */
    private getAllAlertDefs() {
    	def res = []
    	def definitions  = alertHelper.findDefinitions(AlertSeverity.LOW, true, false)
    	def rMan = ResourceManagerEJBImpl.one

    	definitions.each { adef ->
    		if (!isRecoveryAlert(adef.alertDefinitionValue)){
    			def resource = rMan.findResource(adef.appdefEntityId)
    			res << [code: adef.id, value: adef.name + ":" + resource.name]
    		}
    	}
    	res
    }
    
    /**
     * Frontend method to create new cloner and ask it to do actual cloning.
     * 
     * @param alertDefId Alert Definition id to clone
     * @param eid Application Definition Entity where alert is to be cloned.
	 * @param name Name of the alert to be created.
	 * @param desc Description of the alert to be created.
	 * @param action_id Resource id from cloned alert where contron action is done
	 * @param action_name Action name where action_id is done. stop, start, restart, etc.
     */
    private cloneAlertDefinition(alertDefId, eid, name, desc, action_rid, action_name, report) {
    	def cloner = new Clone(user)
   		cloner.cloneAlertDefinition(alertDefId, eid, name, desc, action_rid, action_name)
   		report.addReport(cloner.reportItem)    		
    }
    
    
    /**
     * Process json request from compatible resources table.
     */
    def getCompatibleAlertDefs(params) {
    	DojoUtil.processTableRequest(COMPATIBLE_RESOURCES_SCHEMA, params)
    }
    
    /**
     * Process json request from sync alert grid.
     */
    def getJsonCompatibleAlertSyncDefs(params) {
    	log.info "Params in getJsonCompatibleAlertSyncDefs ${params}"
    	def origAlertId = params.getOne('origAlertId')
    	def resourceId = params.getOne('resourceId')
    	def allowOnlyCompAlerts = params.getOne('allowOnlyCompAlerts')
    	// if allowOnlyCompAlerts is true
    	// find only alerts which are related to 
    	// same prototype as origAlertId
    	//
    	// else find alerts which
    	// are related to type defined in
    	// resourcId

    	def definitions = alertHelper.findDefinitions(AlertSeverity.LOW, null, true) 
    	JSONArray jsonData = new JSONArray()
		def origAlertDef = aMan.getByIdAndCheck(user, origAlertId.toInteger())
    	
    	if(allowOnlyCompAlerts == 'true') {
    		def origProto = origAlertDef.resource.prototype
        	definitions.each {
    			// Don't add comparing alert to list.
    			// Only add alert if its resource has same prototype = compatible alert
    			def proto = it.resource.prototype
        		if(origProto == proto && it.id != origAlertDef.id) {
            		def row = [:]
            		row.put('name',it.name)
           			row.put('id',it.id)
           			row.put('desc',it.description)
           			row.put('rname',it.resource.name)
           			jsonData.put(row)
        		}
        	}
    	} else {
           	definitions.each{
           		def protoEid = it.appdefType + ':' + it.resource.prototype.valueObject.instanceId
   				if(protoEid == resourceId && it.id != origAlertDef.id) {
   					def row = [:]
   					row.put('name',it.name)
   					row.put('id',it.id)
   					row.put('desc',it.description)
        			row.put('rname',it.resource.name)
   					jsonData.put(row)
   				}    		
   			}
    	}
    	
    	
    	
    	
    	def json = [items  : jsonData, 
         label  : 'name',
         identifier  : 'name'] as JSONObject
         
         render(inline:"/* ${json} */", contentType:'text/json-comment-filtered')
    }

    /**
     * Process request to execute alert sync.
     */
    def executeSync(params) {
    	def gridData = getGridDataFromParams(params)
    	def syncData = ensureSyncValues(params)
    	log.info "Params is ${params}"
    	def nSync = 0
    	def report = new Report(Report.INSTANCE_SYNC)
    	long start = now()
    	
    	gridData.each{row ->
    		syncAlertDefinition(row[0].toInteger(),
    							row[1].toInteger(),
    							syncData,
    							report)
        	nSync++
    	}
       	long end = now()
       	def reportStr = report.getStatusAsString()
       	def timeStr = "${nSync} synchronation(s) executed in ${end - start} ms. "
       	timeStr += (report.isOK() ? 'No errors.': 'Errors occured.')
        [timeStatus: timeStr,
         reportStatus: "<div>${reportStr}</div>"]
    }
    
    private syncAlertDefinition(fromId,toId,syncData,report) {
    	def sync = new Sync(user)
    	sync.log = log
   		sync.syncAlertDefinition(fromId, toId, syncData)
   		report.addReport(sync.reportItem)
    }
    
    /**
     * Iterates through params and
     * adds params which start with
     * word 'sync' to array
     */
    def ensureSyncValues(params) {
    	def sync = [:]
    	def expect = ['syncName','syncDesc','syncPriority','syncActive','syncEsc','syncCA',
    	              'syncConditions','syncWillRecover','syncNotifyFiltered',
    	              'syncNotifyRoles','syncNotifyUsers','syncNotifyEmail',
    	              'syncScript','syncSnmp','syncOpenNMS']
    	expect.each{
    		def v = params.getOne(it)
    		if(v == 'true')
    			sync.put(it,'true')
    		else
    			sync.put(it,'false')
    	}
    	sync
    }
    
    /**
     * Grid data are coming in form "0,1":["10116"]]
     * We iterate params and build table type array
     * which contains grid values.
     * 
     * @param params Request parameters
     */
    def getGridDataFromParams(params) {
        def table = []
        def i = 0
        while(true) {
        	// if we dont find rows first column, 
        	// assume end of data
        	def test = params.getOne(i + ',0')
        	if (test == null) break
        	
        	def row = []
        	def j = 0
        	while(true) {
            	def cell = params.getOne(i + ',' +j)
            	//exit from row in end
            	if (cell == null) break
            	row << cell
            	j++
        	}
        	table << row
        	i++
        }
        table
    }
    
    /**
     * Process request to execute alert cloning.
     */
    def execute(params) {
        log.info "Params is ${params}"
        //params are coming from dojogrid as map values
        //row,col(0,0): 0,0=text
        //other params are in format param_something=text
        
        //from table map values we create multidimensional array
        def table = []
        def i = 0
        while(true) {
        	// if we dont find rows first column, 
        	// assume end of data
        	def test = params.getOne(i + ',0')
        	if (test == null) break
        	
        	def row = []
        	def j = 0
        	while(true) {
            	def cell = params.getOne(i + ',' +j)
            	//exit from row in end
            	if (cell == null) break
            	row << cell
            	j++
        	}
        	table << row
        	i++
        }
        
        //log.info "Table from params ${table}"
        executeCode(table)
    }
    
    /**
     * Method to do actual alert cloning.
     */
    private Map executeCode(table) {
    	def nCreated = 0
    	def report = new Report(Report.INSTANCE_CLONE)
    	long start = now()
    	
    	table.each{row ->
    		def eid = new AppdefEntityID(row[3])
        	cloneAlertDefinition(row[2].toInteger(), eid, row[0], row[1], row[6], row[7], report)
        	nCreated++
    	}
       	long end = now()
       	def reportStr = report.getStatusAsString()
       	def timeStr = "${nCreated} cloning(s) executed in ${end - start} ms. "
       	timeStr += (report.isOK() ? 'No errors.': 'Errors occured.')
        [timeStatus: timeStr,
         reportStatus: "<div>${reportStr}</div>"]
    }
    
    /**
     * This method is called when user is pressing add button to
     * copy alert to be cloned. We need some spesific information
     * which is then stored to grid component on plugin page.
     */
    def getAlertDefData(params) {
    	executeGetAlertDefData(
    			params.getOne('id').toInteger(),
    			params.getOne('name'),
    			params.getOne('rid').toInteger(),
    			params.getOne('rtype').toInteger())
    }

    /**
     * Actual execution of getAlertDefData method.
     */
    private Map executeGetAlertDefData(id, name, rid, rtype) {

   		def aDef = aMan.getByIdAndCheck(user, id)       		
    	
   		// check if original alert has control action
   		// in that case we se new caname as unavailable
   		// which tells grid not to popup dialog
   		def caname = "unavailable"
   		aDef.actions.each{
   			def classname = it.getClassName() as String
   			if(classname.endsWith("ControlAction"))
   				caname = "none"
   		}
   		
        [alertdefid: id,
         alertdefname: aDef.name,
         alertdefdesc: aDef.description,
         resourceid: rid,
         resourcetype: rtype,
         resourcename: name,
         caname: caname,
         carid: '',
         caaction: 'none'
         ]    	
    }
    
    /**
     * Main page.
     */
    def index(params) {
    	render(locals:
    		[compatibleAlertDefsSchema: COMPATIBLE_RESOURCES_SCHEMA,
    		 compatibleAlertSyncDefsSchema: DEF_TABLE_SCHEMA,
    		 allAlertDefs: getAllAlertDefs(),
    		 controllableTypes: CONTROLLABLE_TYPES
    	    ])  
    }
    
    /**
     * 
     */
     private final CONTROLLABLE_TYPES = [
        getServers: {
    		serverMan.getViewableServerTypes(user, PageControl.PAGE_ALL)
    	},
        getServices: {
    		serviceMan.getViewableServiceTypes(user, PageControl.PAGE_ALL)
    	},
        getPlatforms: {
    		platformMan.getViewablePlatformTypes(user, PageControl.PAGE_ALL)
    	}
     ]
       
    
    /**
     * This method is needed by addcadialog.gsp template. Template
     * is used when 'add control action' dialog is used to
     * select CA to new alert definition.
     * TODO: switch to AppdefManagerEJBImpl
     */
    def addcadialog(params) {
    	int sessionId = SessionManager.instance.put(user)
    	ControlBossLocal cbl = ControlBossUtil.getLocalHome().create()
    	
    	def services = cbl.findControllableServiceTypes(sessionId)
    	def servers = cbl.findControllableServerTypes(sessionId)
    	
    	def s = ""
    	
    	render(locals:
    		[servers: servers,
    		 services: services
    		])
    }
    
    /**
     * Returns platform where resource belongs to.
     */
    private AppdefEntityID findMyPlatform(AppdefEntityID eid) {
    	def rMan = ResourceManagerEJBImpl.one
    	
    	// if I'm platform return myself
    	if(eid.getType == AppdefEntityConstants.APPDEF_TYPE_PLATFORM)
    		return eid
    		
    	def r = rMan.findResource(eid)
    	// resource edges contain relation to other resources.
    	// parent - platform is in the list with correct resource type.
    	def edges = r.toEdges
    	Resource pRoot = edges[edges.length - 1]
    	new AppdefEntityID(AppdefEntityConstants.APPDEF_TYPE_PLATFORM +
    						":" +
    						pRoot.instanceId)
    }
    
    /**
     * Checking if alert definition is someone's recovery alert.
     * 
     * TODO: There may be more reliable method on hq's backend 
     *       to check this info.
     */
	private boolean isRecoveryAlert(AlertDefinitionValue adv) {
		AlertConditionValue[] conditions = adv.getConditions()
		if (conditions != null) {
			for (int i = 0; i < conditions.length; i++) {
				if (conditions[i].type == EventConstants.TYPE_ALERT)
					return true
			}
		}
		return false		
	}

}
