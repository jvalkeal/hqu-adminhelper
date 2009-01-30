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
//import for EE classes
import com.hyperic.hq.bizapp.server.action.control.ScriptAction
import com.hyperic.hq.bizapp.shared.action.ScriptActionConfig
import com.hyperic.hq.bizapp.server.action.alert.SnmpAction
import org.hyperic.hq.bizapp.shared.action.SnmpActionConfig
import com.hyperic.hq.bizapp.shared.action.ControlActionConfig
import com.hyperic.hq.bizapp.server.action.alert.EnableAlertDefAction
import com.hyperic.hq.bizapp.shared.action.EnableAlertDefActionConfig


/**
 * This class is handling alert definition cloning operations. It should be used 
 * with one instance per one cloning operation.
 * 
 */
class Clone extends Manager {
	
	/**
	 * Constructor.
	 * @param user	User object which determines your rights.
	 */
	Clone(AuthzSubject user) {
		super(user)
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
	public void cloneAlertDefinition(	int alertDefId,
										AppdefEntityID aid,
										String name,
										String desc,
										String action_rid,
										String action_name) {
		// create new status where functions can report messages 
		try {
			def alertDef = AlertDefinitionManagerEJBImpl.one.getByIdAndCheck(user, alertDefId)
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
				reportItem.addMessage("Error in recovery alert", Report.ERROR)
			}
		} catch (Exception e) {
			reportItem.addMessage("General error in alert cloning. " + e, Report.ERROR)
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
	private AlertDefinitionValue doCloneAlertDefinition(AlertDefinition alertDef,
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
   				advT.addCondition(createCondition(c.getAlertConditionValue(),aid,rMeasurementId))
   			else
   				advT.addCondition(createCondition(c.getAlertConditionValue(),aid,-1))
   		}
   		
   		
   		// nasty way to get sessionid which is later
   		// needed by EventsBossEJBImpl to call 
   		// alert creation
   		int sessionId = SessionManager.instance.put(user)
   		def created = eb.createAlertDefinition(sessionId, advT)
   		
   		// actions from old alert
   		// createAction function in EB needs alertid, 
   		// so it has to be created before we can add actions
   		def actions = alertDef.getActions()
   		actions.each{ a ->
   			if (rMeasurementId > 0)
   				checkAndCreateAction(a,sessionId,created,rMeasurementId,action_rid, action_name)
   			else
   				checkAndCreateAction(a,sessionId,created,-1,action_rid, action_name)
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
	private void checkAndCreateAction(Action a,
									  int sessionId,
									  AlertDefinitionValue aDefValue,
									  int rMeasurementId,
									  String action_rid,
									  String action_name) {
		// classname from action. we use this to check 
		// what kind of action we're talking about
		def classname = a.getClassName() as String
		def aDef = aMan.getByIdAndCheck(user, aDefValue.id)
		// check what actions should be cloned
		if (classname.endsWith("EmailAction")) {
			handleEmailAction(aDef,a)
		// Open NMS integration
		} else if (classname.endsWith("OpenNMSAction")) {
			handleOpenNMSAction(aDef,a)
		// script action - EE feature
		} else if (classname.endsWith("ScriptAction")) {
			handleScriptAction(aDef,a)
		} else if (classname.endsWith("SnmpAction")) {
			handleSnmpAction(aDef,a)
		} else if (classname.endsWith("EnableAlertDefAction")) {
			handleEnableAlertDefAction(aDef,rMeasurementId)
		} else if (classname.endsWith("ControlAction")) {
			if (action_rid != null && 
				action_name != null &&
				action_rid != "" &&
				action_name != "none") {
				
				def appdefs = action_rid.split(":")
				def atid = appdefs[0].toInteger()
				def aid = appdefs[1].toInteger()
				
				handleControlAction(aDef,atid,aid,action_name)
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
	private AlertDefinitionValue createAlertDefinitionValue(AppdefEntityID aid,
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
		
		// check if escalation is enabled
		adv.setEscalationId(escalation)
		
		adv
	}
   	
   	/**
   	 * Create condition
   	 * 
   	 * @param c
   	 * @param aid
   	 * @param rMeasurementId
   	 */
	private AlertConditionValue createCondition(AlertConditionValue c,
												AppdefEntityID aid,
												int rMeasurementId) {
        AlertConditionValue cond = new AlertConditionValue()
        
        // set common variables
        cond.setType(c.type)
        cond.setRequired(c.required)
        cond.setName(c.name)
        
        // check condition type and handle correct settings
        if (c.type == EventConstants.TYPE_THRESHOLD) {
        	// treshold is comparing metric against
        	// absolute value
        	cond.setComparator(c.comparator)
        	cond.setThreshold(c.threshold)
        	
        	// we need to find new measurementid from the new
        	// resource where alert is to be cloned.
        	cond.setMeasurementId(findCompatibleMeasurementId(c.measurementId,aid))
        } else if (c.type == EventConstants.TYPE_BASELINE) {
        	cond.setOption(c.option) //min, max, mean
        	cond.setThreshold(c.threshold)
        	cond.setComparator(c.comparator)
        	// check if baselines are available in new resource.
        	// If no, notify warning.
        	def newMeaId = findCompatibleMeasurementId(c.measurementId,aid)
        	def measMan = MeasurementManagerEJBImpl.one
        	def newMea = measMan.getMeasurement(newMeaId)
        	def blines = newMea.baselines
        	if (blines == null) {
        		reportItem.addMessage("Baseline not set, alert may not fire", Report.WARN)
        	}
        	cond.setMeasurementId(newMeaId)
        } else if (c.type == EventConstants.TYPE_CONTROL) {
        	//nothing to do
        } else if (c.type == EventConstants.TYPE_CHANGE) {
        	cond.setMeasurementId(findCompatibleMeasurementId(c.measurementId,aid))        	
        } else if (c.type == EventConstants.TYPE_CUST_PROP) {
        	//nothing to do
        } else if (c.type == EventConstants.TYPE_ALERT) {
        	cond.setMeasurementId(rMeasurementId)
        } else if (c.type == EventConstants.TYPE_LOG) {
        	cond.setOption(c.getOption())
        } else if (c.type == EventConstants.TYPE_CFG_CHG) {        	
        	cond.setOption(c.getOption())
        } else {
        	// we should not be here, something wrong. return null
        	return null
        }
        
        cond
	}
   	
   	/**
   	 * This will try to find if alertdefinition has recovery alert setted.
   	 * If it's found, return it.
   	 * 
   	 * @param adv	AlertDefinition object which possible recovery we want to find 
   	 * @return 		Available recovery alert if found, return null otherwise.
   	 */
	private AlertDefinition findRecoveryAlertDefinition(AlertDefinition adv) {
		AlertDefinition found = null		
		
		//find resource where adv belongs
		AppdefEntityID eid = new AppdefEntityID(
				adv.getAppdefType() +
				":" +
				adv.getAppdefId())
		
   		List definitions = aMan.findAlertDefinitions(user, eid)
   		
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
