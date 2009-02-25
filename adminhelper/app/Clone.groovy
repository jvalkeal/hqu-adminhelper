import org.hyperic.hq.events.server.session.AlertDefinitionManagerEJBImpl
import org.hyperic.hq.measurement.server.session.MeasurementManagerEJBImpl
import org.hyperic.hq.events.server.session.RegisteredTriggerManagerEJBImpl
import org.hyperic.hq.bizapp.server.session.EventsBossEJBImpl
import org.hyperic.hq.authz.server.session.AuthzSubject
import org.hyperic.hq.appdef.shared.AppdefEntityID
import org.hyperic.hq.events.shared.AlertConditionValue
import org.hyperic.hq.events.shared.AlertDefinitionValue
import org.hyperic.hq.events.server.session.AlertDefinition
import org.hyperic.hq.events.EventConstants
import org.hyperic.hq.bizapp.server.action.integrate.OpenNMSAction
import org.hyperic.hq.bizapp.shared.action.EmailActionConfig
import org.hyperic.hq.events.server.session.Action
import org.hyperic.util.config.ConfigResponse
import org.hyperic.hq.auth.shared.SessionManager
import org.hyperic.hq.authz.server.session.AuthzSubject


/**
 * This class is handling alert definition cloning operations. It should be used 
 * with one instance per one cloning operation.
 * 
 */
class Clone {
	
	/** handler to manager */
	def manager
	
	/** Subject for user auth */
	def AuthzSubject user

	
	/**
	 * Constructor.
	 * @param user	User object which determines your rights.
	 */
	Clone(AuthzSubject user) {
		this.manager = new Manager(user)
		this.user = user
	}
	
	/**
	 * Constructor.
	 * @param user	User object which determines your rights.
	 * @param manager	Manager object
	 */
	Clone(AuthzSubject user,manager) {
		this.manager = manager
		this.user = user
	}


	/**
	 * This is the main function used by alert cloning. 
	 * 
	 * @param alertDefId Alert definition id to clone.
	 * @param aid Application Definition Entity where alert is to be cloned.
	 * @param name Name of the alert to be created.
	 * @param desc Description of the alert to be created.
	 * @param action_id Resource id from cloned alert where contron action is done
	 * @param action_name Action name where action_id is done. stop, start, restart, etc.
	 */
	def void cloneAlertDefinition(	int alertDefId,
										AppdefEntityID aid,
										String name,
										String desc,
										String action_rid,
										String action_name) {
		// create new status where functions can report messages 
		try {
			def alertDef = manager.aMan.getByIdAndCheck(user, alertDefId)
			def created = doCloneAlertDefinition(alertDef,aid,name,desc,-1, action_rid, action_name)

			// if alert cloning failed, don't even
			// think about to check wheter it
			// had active recovery alert.
			if (created != null) {
		   		def rAdef = findRecoveryAlertDefinition(alertDef)
		   		// ok, if we found recovery alert,
		   		// clone it also.
		   		if (rAdef != null) {
		   			doCloneAlertDefinition(	rAdef,
		   									aid,
		   									"Recovery: " + name,
		   									"Recovery for id " + created.id,
		   									created.id,
		   									null,null)
		   		}				
			} else {
				manager.reportItem.addMessage("Error in recovery alert", Report.ERROR)
			}
		} catch (Exception e) {
			manager.reportItem.addMessage("General error in alert cloning. " + e, Report.ERROR)
		}
	}
	
	/**
	 * Inner method which does the actual job. It calls alert definition
	 * creation, actions creation and condition creation.
	 * 
	 * 
	 * 
	 * @param alertDefId Alert definition id to clone.
	 * @param aid Application Definition Entity where alert is to be cloned.
	 * @param name Name of the alert to be created.
	 * @param desc Description of the alert to be created.
	 * @param 
	 * @param action_id Resource id from cloned alert where contron action is done
	 * @param action_name Action name where action_id is done. stop, start, restart, etc.
	 */
	def AlertDefinitionValue doCloneAlertDefinition(AlertDefinition alertDef,
											AppdefEntityID aid,
											String name,
											String desc,
											int rMeasurementId,
											String action_rid,
											String action_name) {
   		

   		def adv = alertDef.getAlertDefinitionValue()
   		
   		// template used to create new alert
   		def advT = createAlertDefinitionValue(aid, name, desc, adv.priority,
   												adv.getNotifyFiltered(),
   												adv.getWillRecover(),
   												adv.getFrequencyType(),
   												adv.getRange(),
   												adv.getCount(),
   												adv.getEscalationId())
   		
   		// conditions from old alert
   		def conditions = alertDef.getConditions()
   		conditions.each{c -> 
   			if (rMeasurementId > 0)
   				advT.addCondition(manager.createCondition(c.getAlertConditionValue(),aid,rMeasurementId))
   			else
   				advT.addCondition(manager.createCondition(c.getAlertConditionValue(),aid,-1))
   		}
   		
   		def created = manager.eb.createAlertDefinition(manager.sessionId, advT)
   		
   		// actions from old alert
   		def actions = alertDef.getActions()
   		actions.each{ a ->
   			if (rMeasurementId > 0)
   				checkAndCreateAction(a,manager.sessionId,created,rMeasurementId,action_rid, action_name)
   			else
   				checkAndCreateAction(a,manager.sessionId,created,-1,action_rid, action_name)
   		}
   		
   		// escalation
   		if(adv.getEscalationId() != null) {
   			manager.setEscalation(created.id, adv.getEscalationId())
   		}
   		
   		// return created alert definition value
   		created
	}
	
	/**
	 * Every action are checked here whether we want
	 * to clone it or not.
	 * 
	 * @param sessionId HQ internal session id used within EJB implementations.
	 * 
	 * @param action_id Resource id from cloned alert where contron action is done
	 * @param action_name Action name where action_id is done. stop, start, restart, etc.
	 */
	def void checkAndCreateAction(Action a,
									  int sessionId,
									  AlertDefinitionValue aDefValue,
									  int rMeasurementId,
									  String action_rid,
									  String action_name) {
		// classname from action. we use this to check 
		// what kind of action we're talking about
		def classname = a.getClassName() as String
		def aDef = manager.aMan.getByIdAndCheck(user, aDefValue.id)
		// check what actions should be cloned
		if (classname.endsWith("EmailAction")) {
			manager.handleEmailAction(aDef,a)
		// Open NMS integration
		} else if (classname.endsWith("OpenNMSAction")) {
			manager.handleOpenNMSAction(aDef,a)
		// script action - EE feature
		} else if (classname.endsWith("ScriptAction")) {
			manager.handleScriptAction(aDef,a)
		} else if (classname.endsWith("SnmpAction")) {
			manager.handleSnmpAction(aDef,a)
		} else if (classname.endsWith("EnableAlertDefAction")) {
			manager.handleEnableAlertDefAction(aDef,rMeasurementId)
		} else if (classname.endsWith("ControlAction")) {
			if (action_rid != null && 
				action_name != null &&
				action_rid != "" &&
				action_name != "none") {
				
				def appdefs = action_rid.split(":")
				def atid = appdefs[0].toInteger()
				def aid = appdefs[1].toInteger()
				
				manager.handleControlAction(aDef,atid,aid,action_name)
			}
		}
	}
	
	
   	/**
   	 * Creating alert template
   	 * 
	 * @param aid Application Definition Entity where alert is to be cloned.
	 * @param name Name of the alert to be created.
	 * @param desc Description of the alert to be created.
   	 * @param priority Alert prioroty
   	 * @param notifyFiltered Filter notification actions that are defined for related alerts. 
   	 * @param willRecover Generate one alert and then disable alert definition until fixed.
   	 * @param frequency  
   	 * @param crange
   	 * @param ccount
   	 * @param escalation Escalation id
   	 */
	def AlertDefinitionValue createAlertDefinitionValue(AppdefEntityID aid,
															String name,
															String desc,
															int priority,
															boolean notifyFiltered,
															boolean willRecover,
															int frequency,
															long crange,
															long ccount,
															Integer escalation) {
		AlertDefinitionValue adv = new AlertDefinitionValue()
		
		// name and description
		adv.setName(name)
		adv.setDescription(desc)
		
		// should we actually allow null for aid
		if (aid != null) {
            adv.setAppdefType(aid.getType())
            adv.setAppdefId(aid.getID())
		}
		
		adv.setPriority(priority)
		
		// we don't clone definitions which are not enabled or
		// activated. Just enable, no need to copy these from old one
		adv.setEnabled(true)
		adv.setActive(true)
		
		// Form - Frequency settings
		adv.setFrequencyType(frequency)
		adv.setRange(crange)
		adv.setCount(ccount)

		// Form - Filter notification actions that are defined for related alerts. 
		adv.setNotifyFiltered(notifyFiltered)
		
		// Form - Generate one alert and then disable alert definition until fixed 
		adv.setWillRecover(willRecover)
				
		adv
	}
   	
   	
   	/**
   	 * This will try to find if alertdefinition has recovery alert setted.
   	 * If it's found, return it.
   	 * 
   	 * @param adv	AlertDefinition object which possible recovery we want to find 
   	 * @return 		Available recovery alert if found, return null otherwise.
   	 */
	def AlertDefinition findRecoveryAlertDefinition(AlertDefinition adv) {
		AlertDefinition found = null		
		
		//find resource where adv belongs
		AppdefEntityID eid = new AppdefEntityID(
				adv.getAppdefType() +
				":" +
				adv.getAppdefId())
		
   		List definitions = AlertDefinitionManagerEJBImpl.one.findAlertDefinitions(user, eid)
   		
   		// TODO: what if you have multiple recovery alert definitions.
   		definitions.each{aDef ->
			def condition = aDef.conditions
			condition.each{
				if(it.type == EventConstants.TYPE_ALERT &&
					it.measurementId == adv.id) {
					found = aDef
				}
				if (found != null)
					return found
			}
		}
   		
		return found
	}

}
