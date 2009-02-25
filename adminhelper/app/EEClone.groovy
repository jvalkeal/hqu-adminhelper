import org.hyperic.hq.authz.server.session.AuthzSubject


class EEClone extends Clone {
	
	/**
	 * Constructor.
	 * @param user	User object which determines your rights.
	 */
	EEClone(AuthzSubject user) {
		super(user,new EEManager(user))
	}
}