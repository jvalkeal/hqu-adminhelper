import org.hyperic.hq.authz.server.session.AuthzSubject


class EESync extends Sync {
	
	/**
	 * Constructor.
	 * @param user	User object which determines your rights.
	 */
	EESync(AuthzSubject user) {
		super(user,new EEManager(user))
	}
}