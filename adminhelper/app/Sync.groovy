import org.hyperic.hq.authz.server.session.AuthzSubject
import org.hyperic.hq.events.server.session.AlertDefinitionManagerEJBImpl
import org.hyperic.hq.bizapp.server.session.EventsBossEJBImpl
import org.hyperic.hq.auth.shared.SessionManager
import org.hyperic.util.config.ConfigResponse
import org.hyperic.hq.bizapp.shared.action.EmailActionConfig
import org.hyperic.hq.events.server.session.AlertDefinition
import org.hyperic.hq.events.shared.AlertDefinitionValue
import org.hyperic.hq.events.server.session.Action

/**
 * This class is handling alert definition synchronize operations. It should be used 
 * with one instance per one sync operation.
 * 
 */
class Sync {

	/** handler to manager */
	def manager
	
	/** Subject for user auth */
	def AuthzSubject user
	
	/**
	 * Constructor.
	 * @param user	User object which determines your rights.
	 */
	Sync(AuthzSubject user) {
		this.manager = new Manager(user)
		this.user = user
	}
	
	/**
	 * Constructor.
	 * @param user	User object which determines your rights.
	 * @param manager	Manager object
	 */
	Sync(AuthzSubject user,manager) {
		this.manager = manager
		this.user = user
	}
	
	/**
	 * Main method to sync alert definitions.
	 * 
	 * @param fromId Id of the alert from which parameters are synched
	 * @param toId Id of the alert where parameters are synched
	 * @param syncData Mapdata containing sync instructions
	 */
	def syncAlertDefinition(int fromId, int toId, Map syncData) {
		try {
			def fromDef = manager.aMan.getByIdAndCheck(user, fromId)
			def toDef = manager.aMan.getByIdAndCheck(user, toId)
			def fromADefValue = fromDef.alertDefinitionValue
			def toADefValue = toDef.alertDefinitionValue
		
			updateAlerfDefinitionValue(fromADefValue,toADefValue,syncData)
		
			// update condition structure
			// pass also AlertDefinitionValue since
			// it's already used. Can't ask it 
			// from alertdef.
			if(syncData.get('syncConditions').equals('true'))
				updateConditions(fromDef,toDef,toADefValue)
		
			manager.eb.updateAlertDefinition(manager.sessionId,toADefValue)
				
			// update actions
			updateActions(fromDef, toDef, syncData)
			
			// escalation
			if(syncData.get('syncEsc').equals('true')){ 
				if(fromDef.escalation != null) {
					manager.setEscalation(toId, fromDef.escalation.id)
				} else {
					manager.setEscalation(toId, 0)
				}
			}

			
		} catch (Exception e) {
			manager.reportItem.addMessage("Unable to update alert definition. " + e, Report.ERROR)
		}
	}
	
	/**
	 * Updating aleft definition conditions.
	 * 
	 * @param from Alert definition from where to sync values.
	 * @param from Alert definition to sync values.
	 * @param from Alert definition value to sync values.
	 */
	def updateConditions(AlertDefinition from, 
						 AlertDefinition to,
						 AlertDefinitionValue toADef) {
		toADef.removeAllConditions()
		
   		def conditions = from.getConditions()
   		conditions.each{c -> 
   			toADef.addCondition(manager.createCondition(c.getAlertConditionValue(),to.appdefEntityId,-1))
   		}
	}
	
	/**
	 * Updating actions.
	 * 
	 * @param from Alert definition from where to sync values.
	 * @param from Alert definition to sync values.
	 * @param syncData Map containing sync instructions.
	 */
	def updateActions(AlertDefinition from, AlertDefinition to, Map syncData) {
		
		String.metaClass.canSync = {String action, String param, Map data ->
			delegate.endsWith(action) && data.get(param).equals('true')
		}

		String.metaClass.canSyncEmail = {String action, String param, Map data,
										 Action a, int type ->
			if(!delegate.endsWith(action)) {
				return false
			} else {
				def t = ConfigResponse.decode(a.config).getValue(EmailActionConfig.CFG_TYPE).toInteger()
				return data.get(param).equals('true') && t == type 
			}
		}

		
		from.actions.each{
			def classname = it.getClassName() as String
			if (classname.canSync("OpenNMSAction","syncOpenNMS",syncData)) {
				manager.handleOpenNMSAction(to,it)
			} else if (classname.canSyncEmail("EmailAction","syncNotifyUsers",syncData,it,EmailActionConfig.TYPE_USERS)) {	
				manager.handleEmailAction(to,it)
			} else if (classname.canSyncEmail("EmailAction","syncNotifyRoles",syncData,it,EmailActionConfig.TYPE_ROLES)) {	
				manager.handleEmailAction(to,it)
			} else if (classname.canSyncEmail("EmailAction","syncNotifyEmail",syncData,it,EmailActionConfig.TYPE_EMAILS)) {	
				manager.handleEmailAction(to,it)
			} else if (classname.canSync("ScriptAction","syncScript",syncData)) {
				manager.handleScriptAction(to,it)				
			} else if (classname.canSync("SnmpAction","syncSnmp",syncData)) {
				manager.handleSnmpAction(to,it)				
			} 
		}		
	}
		
	/**
	 * @param from Source AlertDefinitioValue for sync values. 
	 * @param to Destination AlertDefinitioValue for sync.
	 * @param syncData List containing info for sync.
	 */
	def updateAlerfDefinitionValue(AlertDefinitionValue from,
								   AlertDefinitionValue to,
								   Map syncData) {
		 
		if(syncData.get('syncName').equals('true')) 
			to.setName(from.name)

		if(syncData.get('syncDesc').equals('true')) 
			to.setDescription(from.description)
			
		if(syncData.get('syncPriority').equals('true')) 
			to.setPriority(from.priority)
			
		if(syncData.get('syncActive').equals('true')) 
			to.setActive(from.active)
			
		if(syncData.get('syncWillRecover').equals('true')) 
			to.setWillRecover(from.willRecover)
		
		if(syncData.get('syncNotifyFiltered').equals('true')) 
			to.setNotifyFiltered(from.notifyFiltered)

		if(syncData.get('syncConditions').equals('true')) {
			to.setFrequencyType(from.frequencyType)			
			to.setRange(from.range)			
			to.setCount(from.getCount())			
		}
			
	}
	

}