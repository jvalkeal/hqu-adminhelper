import org.hyperic.hq.hqu.rendit.HQUPlugin

import AdminhelperController

class Plugin extends HQUPlugin {
	Plugin() {
		addAdminView(true, '/adminhelper/index.hqu', 'Admin Helper')
    }         
}

