import org.hyperic.hq.authz.server.session.AuthzSubject
import org.hyperic.hq.bizapp.server.action.integrate.OpenNMSAction
import org.hyperic.hq.events.server.session.AlertDefinition
import org.hyperic.hq.events.shared.AlertDefinitionValue
import org.hyperic.hq.events.shared.ActionValue
import org.hyperic.hq.events.server.session.Action
import org.hyperic.util.config.ConfigResponse
import org.hyperic.hq.auth.shared.SessionManager
import org.hyperic.hq.bizapp.server.session.EventsBossEJBImpl
import org.hyperic.hq.measurement.server.session.MeasurementManagerEJBImpl
import org.hyperic.hq.bizapp.shared.action.EmailActionConfig
import org.hyperic.hq.bizapp.server.action.email.EmailAction
import org.hyperic.hq.events.server.session.AlertDefinitionManagerEJBImpl
import org.hyperic.hq.events.shared.AlertConditionValue
import org.hyperic.hq.appdef.shared.AppdefEntityID
import org.hyperic.hq.events.EventConstants
//for EE
import com.hyperic.hq.bizapp.server.action.control.ScriptAction
import com.hyperic.hq.bizapp.shared.action.ScriptActionConfig
import com.hyperic.hq.bizapp.server.action.alert.SnmpAction
import org.hyperic.hq.bizapp.shared.action.SnmpActionConfig
import com.hyperic.hq.bizapp.server.action.control.ControlAction
import com.hyperic.hq.bizapp.shared.action.ControlActionConfig
import com.hyperic.hq.bizapp.server.action.alert.EnableAlertDefAction
import com.hyperic.hq.bizapp.shared.action.EnableAlertDefActionConfig

/**
 * 
 */
class Manager {
		
	/** Subject for user auth */
	protected AuthzSubject user
	
	/** Caching instance */
	protected EventsBossEJBImpl eb
	
	/** session Id */
	protected int sessionId
	
	protected ReportItem reportItem
	
	/** Alert Definition Manager*/
	def aMan

	/** Measurement manager */
	def measMan

	def log
	
	/**
	 * Constructor.
	 * @param user	User object which determines your rights.
	 */
	Manager(AuthzSubject user) {
		this.user = user
		this.eb = new EventsBossEJBImpl()
		this.sessionId = SessionManager.instance.put(user)
		this.aMan = AlertDefinitionManagerEJBImpl.one
		this.measMan = MeasurementManagerEJBImpl.one
		this.reportItem = new ReportItem()
	}

	/**
	 * Handles creation of email action. If action already
	 * exists, updates old one.
	 * 
	 * This handles notification of users, roles and other
	 * email addresses.
	 * 
	 * @param aDef Alert Definition to update
	 * @param action Original action to copy config
	 */
	def handleEmailAction(AlertDefinition aDef, Action action) {
		// check if we already have action
		ActionValue existing = null
		ConfigResponse res = ConfigResponse.decode(action.config)
		def typeToFind = res.getValue(EmailActionConfig.CFG_TYPE)
		aDef.actions.each{
			// ee has it's own EmailAction class... weird
			if(it.className.endsWith("EmailAction")) {
				// need to check from type the correct one
				def typeToComp = ConfigResponse.decode(it.config)
					.getValue(EmailActionConfig.CFG_TYPE)
				if(typeToFind.equals(typeToComp))
					existing = it.actionValue
			}
		}
		// We want to create new action everytime
		EmailActionConfig eac = new EmailActionConfig()
		eac.setType(res.getValue(EmailActionConfig.CFG_TYPE).toInteger())
		eac.setNames(res.getValue(EmailActionConfig.CFG_NAMES))
		// If we had old action, update it.
		// Else create new.
		if(existing != null) {
			existing.setClassname(eac.getImplementor())
			existing.setConfig(eac.getConfigResponse().encode())
			eb.updateAction(sessionId, existing)
		} else {
			eb.createAction(sessionId,
							aDef.id,
							eac.getImplementor(),
							eac.getConfigResponse())
		}
	}
	
	/**
	 * Handles creation of openNms action. If action already
	 * exists, updates old one.
	 * 
	 * @param aDef Alert Definition to update
	 * @param action Original action to copy config
	 */
	def handleOpenNMSAction(AlertDefinition aDef, Action action) {
		// check if we already have action
		ActionValue existing = null
		aDef.actions.each{
			if(it.className.equals(OpenNMSAction.class.name))
				existing = it.actionValue
		}
		// We want to create new action everytime
		OpenNMSAction oa = new OpenNMSAction()
		ConfigResponse res = ConfigResponse.decode(action.config)
		oa.setIp(res.getValue(OpenNMSAction.IP))
		oa.setServer(res.getValue(OpenNMSAction.SERVER))
		oa.setPort(res.getValue(OpenNMSAction.PORT))
		// If we had old action, update it.
		// Else create new.
		if(existing != null) {
			existing.setClassname(oa.getImplementor())
			existing.setConfig(oa.getConfigResponse().encode())
			eb.updateAction(sessionId, existing)
		} else {
			eb.createAction(sessionId,
							aDef.id,
							oa.getImplementor(),
							oa.getConfigResponse())
		}
	}
	
	/**
	 * Handles creation of script action. If action already
	 * exists, updates old one.
	 * 
	 * @param aDef Alert Definition to update
	 * @param action Original action to copy config
	 */
	def handleScriptAction(AlertDefinition aDef, Action action) {
		// check if we already have action
		ActionValue existing = null
		aDef.actions.each{
			if(it.className.equals(ScriptAction.class.name))
				existing = it.actionValue
		}
		// We want to create new action everytime
		ScriptActionConfig sa = new ScriptActionConfig()
		ConfigResponse res = ConfigResponse.decode(action.config)
		sa.setScript(res.getValue(ScriptActionConfig.CFG_SCRIPT))
		// If we had old action, update it.
		// Else create new.
		if(existing != null) {
			existing.setClassname(sa.getImplementor())
			existing.setConfig(sa.getConfigResponse().encode())
			eb.updateAction(sessionId, existing)
		} else {
			eb.createAction(sessionId,
							aDef.id,
							sa.getImplementor(),
							sa.getConfigResponse())
		}
	}

	/**
	 * Handles creation of snmp action. If action already
	 * exists, updates old one.
	 * 
	 * @param aDef Alert Definition to update
	 * @param action Original action to copy config
	 */
	def handleSnmpAction(AlertDefinition aDef, Action action) {
		// check if we already have action
		ActionValue existing = null
		aDef.actions.each{
			if(it.className.equals(SnmpAction.class.name))
				existing = it.actionValue
		}
		// We want to create new action everytime
		SnmpActionConfig sa = new SnmpActionConfig()
		ConfigResponse res = ConfigResponse.decode(action.config)
		sa.setOid(res.getValue(SnmpActionConfig.CFG_OID))
		sa.setAddress(res.getValue(SnmpActionConfig.CFG_ADDRESS))
		// If we had old action, update it.
		// Else create new.
		if(existing != null) {
			existing.setClassname(sa.getImplementor())
			existing.setConfig(sa.getConfigResponse().encode())
			eb.updateAction(sessionId, existing)
		} else {
			eb.createAction(sessionId,
							aDef.id,
							sa.getImplementor(),
							sa.getConfigResponse())
		}
	}

	/**
	 * Handles creation of enable alert definition action. If action already
	 * exists, updates old one.
	 * 
	 * @param aDef Alert Definition to update
	 * @param rMeasurementId Metric measurement id to use.
	 */
	def handleEnableAlertDefAction(AlertDefinition aDef, int rMeasurementId) {
		// check if we already have action
		ActionValue existing = null
		aDef.actions.each{
			if(it.className.equals(EnableAlertDefAction.class.name))
				existing = it.actionValue
		}
		// We want to create new action everytime
		EnableAlertDefActionConfig eaa = new EnableAlertDefActionConfig()
		eaa.setAlertDefId(rMeasurementId)
		// If we had old action, update it.
		// Else create new.
		if(existing != null) {
			existing.setClassname(eaa.getImplementor())
			existing.setConfig(eaa.getConfigResponse().encode())
			eb.updateAction(sessionId, existing)
		} else {
			eb.createAction(sessionId,
							aDef.id,
							eaa.getImplementor(),
							eaa.getConfigResponse())
		}
	}

	/**
	 * Handles creation of control action. If action already
	 * exists, updates old one.
	 * 
	 * @param aDef Alert Definition to update
	 * @param rMeasurementId Metric measurement id to use.
	 */
	def handleControlAction(AlertDefinition aDef, int atid, int aid, String aName) {
		// check if we already have action
		ActionValue existing = null
		aDef.actions.each{
			if(it.className.equals(ControlAction.class.name))
				existing = it.actionValue
		}
		// We want to create new action everytime
		ControlActionConfig ca = new ControlActionConfig()
		ca.setAppdefType(atid)
		ca.setAppdefId(aid)
		ca.setControlAction(aName)
		// If we had old action, update it.
		// Else create new.
		if(existing != null) {
			existing.setClassname(ca.getImplementor())
			existing.setConfig(ca.getConfigResponse().encode())
			eb.updateAction(sessionId, existing)
		} else {
			eb.createAction(sessionId,
							aDef.id,
							ca.getImplementor(),
							ca.getConfigResponse())
		}
	}

   	/**
   	 * Create condition
   	 * 
   	 * @param c
   	 * @param aid
   	 * @param rMeasurementId
   	 */
	def AlertConditionValue createCondition(AlertConditionValue c,
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
        	def newMea = measMan.getMeasurement(newMeaId)
        	def blines = newMea.baselines
        	if (blines == null) {
    			reportItem.addMessage("Baseline not set, alert may not fire",CloneStatus.WARN)        		
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
	 * Finding similar measurement from resource.
	 * 
	 * @param id Measurement id
	 * @param aid Application Definition Entity Id
	 */
	def findCompatibleMeasurementId(int id, AppdefEntityID aid) {
		def oldMea = measMan.getMeasurement(id)
		def newMea
		try {
			newMea = measMan.findMeasurement(user,oldMea.getTemplate().id,aid)
			
		} catch (Exception e) {
			reportItem.addMessage("Could not find new measurement. Check that similar metric is working in new resource.", Report.ERROR)
		}
		newMea.id
	}

	
}