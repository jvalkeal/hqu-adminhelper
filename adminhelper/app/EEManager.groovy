import org.hyperic.hq.authz.server.session.AuthzSubject
import org.hyperic.hq.bizapp.shared.action.SnmpActionConfig
import org.hyperic.hq.bizapp.server.action.integrate.OpenNMSAction

import org.hyperic.hq.events.server.session.AlertDefinition
import org.hyperic.hq.events.shared.AlertDefinitionValue
import org.hyperic.hq.events.shared.ActionValue
import org.hyperic.hq.events.server.session.Action
import org.hyperic.util.config.ConfigResponse
import org.hyperic.hq.auth.shared.SessionManager
import org.hyperic.hq.appdef.shared.AppdefEntityID
import org.hyperic.hq.events.EventConstants


import com.hyperic.hq.bizapp.server.action.control.ScriptAction
import com.hyperic.hq.bizapp.shared.action.ScriptActionConfig
import com.hyperic.hq.bizapp.server.action.alert.SnmpAction
import com.hyperic.hq.bizapp.server.action.control.ControlAction
import com.hyperic.hq.bizapp.shared.action.ControlActionConfig
import com.hyperic.hq.bizapp.server.action.alert.EnableAlertDefAction
import com.hyperic.hq.bizapp.shared.action.EnableAlertDefActionConfig

/**
 * 
 */
class EEManager extends Manager {

	/**
	 * Constructor.
	 * @param user	User object which determines your rights.
	 */
	EEManager(AuthzSubject user) {
		super(user)
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

	
}